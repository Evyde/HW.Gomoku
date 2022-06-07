package jlu.evyde.gobang.Client.Model;

import com.google.gson.Gson;
import jlu.evyde.gobang.Client.Controller.Callback;
import jlu.evyde.gobang.Client.Controller.GobangException;
import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

// TODO: Every Produce Message from UI show push to logic server first, then let logic server broadcast to all UIs,
//      to prevent wrong put action to a same position (Also should judge by UI).
//      Simple check to prevent fake PRODUCE request from WATCHER (check PUTTERs' token).
public class WebSocketMQServer implements MQBrokerServer {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private WebSocketServer wss;

    // TODO: Change this to multiple kind of messages to cover MQSource for better performance.

    /**
     * Start message queue broker server in MQServerAddress.
     *
     * @param msa           Message queue server address.
     * @param startComplete Callback method when server started successfully.
     * @param startError    Callback method when server start failed.
     * @throws GobangException.MQServerStartFailedException
     */
    @Override
    public void startMQBrokerServer(MQServerAddress msa, Callback startComplete, Callback startError)
            throws GobangException.MQServerStartFailedException {
        try {
            wss = new RealWebSocketMQServer(msa, startComplete);
            wss.start();
        } catch (Exception e) {
            logger.error(e.toString());
            startError.run();
            throw new GobangException.MQServerStartFailedException();
        }
        logger.info("Message queue server configured.");
    }

    /**
     * Close message queue broker server immediately.
     *
     * @param beforeClose Callback method before server closed.
     * @param afterClose  Callback method when server closed successfully.
     */
    @Override
    public void closeMQBrokerServer(Callback beforeClose, Callback afterClose) {
        beforeClose.run();
        try {
            wss.stop();
        } catch (InterruptedException ie) {
            logger.error(ie.toString());
            throw new GobangException.MQServerClosedFailedException();
        }
        afterClose.run();
    }

    private class RealWebSocketMQServer extends WebSocketServer {
        private static final Gson jsonParser = new Gson();
        private final Callback startComplete;
        // Obviously FIFO, use linked list as double ends queue.
        private final Deque<MQMessage> normalMessages = new LinkedList<>();
        private final Deque<MQMessage> authMessages = new LinkedList<>();
        private final Deque<MQMessage> persistenceMessages = new LinkedList<>();
        private final Map<MQProtocol.Group, List<WebSocket>> clients = new ConcurrentHashMap<>();
        private final Map<WebSocket, MQProtocol.Group> clientGroupMap = new ConcurrentHashMap<>();
        private final Map<MQProtocol.Group, UUID> tokens = new ConcurrentHashMap<>();
        private final transient ReentrantLock broadcastLock = new ReentrantLock();

        public RealWebSocketMQServer(MQServerAddress msa, Callback startComplete) {
            super(msa.getIsa());
            this.startComplete = startComplete;
            for (MQProtocol.Group g: MQProtocol.Group.getAllGroup()) {
                clients.put(g, new CopyOnWriteArrayList<>());
                tokens.put(g, UUID.randomUUID());
            }
        }

        @Deprecated
        private static String constructRegisterSuccessfulResponse(UUID token) {
            MQMessage m = new MQMessage();
            m.status = MQProtocol.Status.SUCCESS;
            m.code = MQProtocol.Code.UPDATE_TOKEN.getCode();
            m.msg = "Update token";
            m.group = MQProtocol.Group.LOGIC_SERVER;
            m.token = token;
            return m.toJson();
        }

        @Deprecated
        private static String constructRegisterFailedResponse(String msg) {
            MQMessage m = new MQMessage();
            m.msg = msg;
            m.group = MQProtocol.Group.LOGIC_SERVER;
            m.status = MQProtocol.Status.FAILED;
            return m.toJson();
        }

        private static String constructFailedResponse(String msg) {
            MQMessage m = new MQMessage();
            m.msg = msg;
            m.status = MQProtocol.Status.FAILED;
            m.code = MQProtocol.Code.NO_OPERATION.getCode();
            m.group = MQProtocol.Group.LOGIC_SERVER;
            return m.toJson();
        }

        /**
         * Broadcast message to UI or LogicServer automatically.
         *
         * @param m Message to broadcast.
         * @return true if this message has consumer.
         */
        private boolean autoBroadcast(MQMessage m) {
            boolean broadcastStatus = false;
            while (broadcastLock.isLocked()) {
                Thread.onSpinWait();
            }
            logger.info("Broadcast to all available consumers.");
            broadcastLock.lock();
            for (MQProtocol.Group g: m.group.pushMyMessageTo()) {
                if (!clients.get(g).isEmpty()) {
                    for (WebSocket client: clients.get(g)) {
                        if (client != null && !client.isClosed()) {
                            client.send(m.toJson());
                            broadcastStatus = true;
                        } else {
                            clients.get(g).remove(client);
                        }
                    }
                }
            }
            broadcastLock.unlock();
            if (!broadcastStatus) {
                logger.warn("Broadcast called but no body to talk.");
            }
            return broadcastStatus;
        }

        @Override
        public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
            logger.info("New websocket open: {}",
                    webSocket.getRemoteSocketAddress()
            );
            if (clients.get(MQProtocol.Group.GUEST).size() < MQProtocol.Group.GUEST.getCapacityLimit()) {
                clientGroupMap.put(webSocket, MQProtocol.Group.GUEST);
                clients.get(MQProtocol.Group.GUEST).add(webSocket);
            } else {
                logger.warn("Guest queue is full, may be logic server error.");
                webSocket.close(CloseFrame.ABNORMAL_CLOSE,
                        constructFailedResponse("Sorry, the queue of guest is full, try connect later."));
            }
        }

        @Override
        public void onClose(WebSocket webSocket, int i, String s, boolean b) {
            if (clientGroupMap.get(webSocket) != null) {
                clients.get(clientGroupMap.get(webSocket)).remove(webSocket);
            }
            logger.info("Websocket connection closed: {}", webSocket.getRemoteSocketAddress());
        }

        @Override
        public void onMessage(WebSocket webSocket, String s) {
            clearNormalMessageQueue();
            logger.trace(s);
            String[] splitMessage = s.split("\n");

            // parse message content
            StringBuilder message = new StringBuilder();
            for (String i : splitMessage) {
                if (MQProtocol.Head.END.name().equals(i)
                        || MQProtocol.Head.PRODUCE.name().equals(i)
                        || MQProtocol.Head.CONSUME.name().equals(i)
                        || MQProtocol.Head.REGISTER.name().equals(i)
                ) {
                    continue;
                }
                message.append(i);
            }
            logger.debug(message.toString());
            MQMessage incomingMQMessage;
            try {
                incomingMQMessage = jsonParser.fromJson(message.toString(), MQMessage.class);
            } catch (Exception e) {
                logger.warn("Invalid json.");
                logger.debug(message.toString());
                return;
            }

            logger.info("Incoming {} message from {}.", splitMessage[0], webSocket.getRemoteSocketAddress());
            if (MQProtocol.Head.REDIRECT.name().equals(splitMessage[0])) {
                // auth from logic server
                if (
                        incomingMQMessage == null
                                || !tokens.get(MQProtocol.Group.LOGIC_SERVER).equals(incomingMQMessage.token)
                                || incomingMQMessage.group == null
                ) {
                    webSocket.close(CloseFrame.ABNORMAL_CLOSE,
                            constructFailedResponse("Unauthorized AUTH behaviour!"));
                    logger.warn("Unauthorized AUTH behaviour!");
                } else {
                    if (clients.get(MQProtocol.Group.GUEST).contains(incomingMQMessage.session)) {
                        clients.get(incomingMQMessage.group).add(incomingMQMessage.session);
                        clients.get(MQProtocol.Group.GUEST).remove(incomingMQMessage.session);
                        clientGroupMap.put(incomingMQMessage.session, incomingMQMessage.group);
                        // replace token
                        incomingMQMessage.token = tokens.get(incomingMQMessage.group);
                        // replace op code
                        incomingMQMessage.code = MQProtocol.Code.UPDATE_TOKEN.getCode();
                        incomingMQMessage.session.send(incomingMQMessage.toJson());
                        logger.info("Register for {} in Group {} successfully."
                                , incomingMQMessage.session.getRemoteSocketAddress()
                                , incomingMQMessage.group
                        );
                    } else {
                        logger.warn("No specified session found, maybe closed.");
                    }
                }
            } else if (MQProtocol.Head.PRODUCE.name().equals(splitMessage[0])) {
                // produce
                if (
                        incomingMQMessage == null
                                || incomingMQMessage.group == null
                        || !tokens.get(incomingMQMessage.group).equals(incomingMQMessage.token)
                        || incomingMQMessage.code == null
                        || !incomingMQMessage.group.hasPrivilege(MQProtocol.Code.fromInteger(incomingMQMessage.code))
                ) {
                    // no privilege
                    logger.warn("Detect access denied behaviour.");
                    webSocket.close(CloseFrame.ABNORMAL_CLOSE, constructFailedResponse("Access denied!"));
                    return;
                } else {
                    // call auto broadcast
                    if (!autoBroadcast(incomingMQMessage)) {
                        // broadcast failed, save message
                        normalMessages.addLast(incomingMQMessage);
                    } else {
                        // success, persistence
                        persistenceMessages.addLast(incomingMQMessage);
                    }
                }
            } else if (MQProtocol.Head.CONSUME.name().equals(splitMessage[0])) {
                // consume for this guy (debug and legacy tests only)
                logger.warn("Deprecated method.");
            } else if (MQProtocol.Head.REGISTER.name().equals(splitMessage[0])) {
                // Register
                // TODO: Could let token has expired time.
                if (
                        incomingMQMessage == null
                                || !SystemConfiguration.getInitializedUuid().equals(incomingMQMessage.token)
                                || incomingMQMessage.group == null
                                || (
                                incomingMQMessage.group == MQProtocol.Group.LOGIC_SERVER &&
                                        (
                                                (clients.get(MQProtocol.Group.LOGIC_SERVER).size() >=
                                                        MQProtocol.Group.LOGIC_SERVER.getCapacityLimit()) &&
                                                        !allLogicServerDown()
                                        )
                        )
                                || clients.get(incomingMQMessage.group).size() >= incomingMQMessage.group.getCapacityLimit()
                                || incomingMQMessage.group == MQProtocol.Group.GUEST
                ) {
                    // invalid register message
                    webSocket.close(CloseFrame.ABNORMAL_CLOSE, constructFailedResponse("Invalid register message."));
                    logger.warn("Receive invalid register message.");
                    logger.debug(s);
                } else {
                    if (incomingMQMessage.group == MQProtocol.Group.LOGIC_SERVER) {
                        clients.get(MQProtocol.Group.LOGIC_SERVER).add(webSocket);
                        clientGroupMap.put(webSocket, MQProtocol.Group.LOGIC_SERVER);
                        incomingMQMessage.token = tokens.get(MQProtocol.Group.LOGIC_SERVER);
                        incomingMQMessage.code = MQProtocol.Code.UPDATE_TOKEN.getCode();
                        incomingMQMessage.status = MQProtocol.Status.SUCCESS;
                        webSocket.send(incomingMQMessage.toJson());
                        clearRegisterMessageQueue();
                        return;
                    }

                    incomingMQMessage.session = webSocket;
                    // replace token with random token
                    incomingMQMessage.token = UUID.randomUUID();
                    // replace op code to AUTH
                    incomingMQMessage.code = MQProtocol.Code.AUTH.getCode();

                    if (allLogicServerDown()) {
                        logger.warn("All logic server down, no body can process auth message, saved.");
                        authMessages.addLast(incomingMQMessage);
                    }
                    // send to logic server with no response and add it to guest

                    clientGroupMap.put(webSocket, MQProtocol.Group.GUEST);
                    clients.get(MQProtocol.Group.GUEST).add(webSocket);
                    autoBroadcast(incomingMQMessage);
                }
            }
        }

        private boolean allLogicServerDown() {
            for (WebSocket w: clients.get(MQProtocol.Group.LOGIC_SERVER)) {
                if (w != null && !w.isClosed()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void onError(WebSocket webSocket, Exception e) {
            logger.error(
                    "Websocket {} raise error: {}",
                    webSocket.getRemoteSocketAddress(),
                    e.toString()
            );
            System.exit(5);
        }

        @Override
        public void onStart() {
            startComplete.run();
        }

        private void clearNormalMessageQueue() {
            if (!allLogicServerDown() && !normalMessages.isEmpty()) {
                // clear message queue
                logger.debug("Clearing message queue.");
                while (!normalMessages.isEmpty() && autoBroadcast(normalMessages.peekFirst())) {
                    persistenceMessages.addLast(normalMessages.removeFirst());
                }
            }
        }

        private void clearRegisterMessageQueue() {
            if (!allLogicServerDown() && !authMessages.isEmpty()) {
                logger.debug("Clearing register queue.");
                while (!authMessages.isEmpty() && autoBroadcast(authMessages.peekFirst()));
            }
        }
    }
}
