package jlu.evyde.gobang.Client.Model;

import com.google.gson.Gson;
import jlu.evyde.gobang.Client.Controller.Callback;
import jlu.evyde.gobang.Client.Controller.GobangException;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

// TODO: Every Produce Message from UI show push to logic server first, then let logic server broadcast to all UIs,
//      to prevent wrong put action to a same position (Also should judge by UI).
//      Simple check to prevent fake PRODUCE request from WATCHER (check PUTTERs' token).
public class WebSocketMQServer implements MQBrokerServer {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private WebSocketServer wss;

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
        private final Deque<MQMessage> normalMessages = new ConcurrentLinkedDeque<>();
        private final Deque<MQMessage> persistenceMessages = new ConcurrentLinkedDeque<>();
        private final Deque<MQMessage> authMessages = new ConcurrentLinkedDeque<>();

        private final Map<MQProtocol.Group, List<MQClient>> clients = new ConcurrentHashMap<>();
        private final transient ReentrantLock broadcastLock = new ReentrantLock();

        public RealWebSocketMQServer(MQServerAddress msa, Callback startComplete) {
            super(msa.getIsa());
            this.startComplete = startComplete;
            for (MQProtocol.Group g: MQProtocol.Group.getAllGroup()) {
                clients.put(g, new CopyOnWriteArrayList<>());
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
                for (MQClient client: clients.get(g)) {
                    if (client.send(m)) {
                        broadcastStatus = true;
                    } else {
                        clients.get(g).remove(client);
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
            MQClient client = new MQClient(webSocket);
            // generate uuid and put this client to GUEST group
            // check if group has enough room
            if (clients.get(MQProtocol.Group.GUEST).size() < MQProtocol.Group.GUEST.getCapacityLimit()) {
                UUID generatedUUID = UUID.randomUUID();
                // update this four data structure
                client.setGroup(MQProtocol.Group.GUEST);
                client.setToken(generatedUUID);
                clients.get(MQProtocol.Group.GUEST).add(client);
            } else {
                logger.warn("Guest queue is full, may be logic server error.");
                client.close(constructFailedResponse("Sorry, the queue of guest is full, try connect later."));
            }
        }

        @Override
        public void onClose(WebSocket webSocket, int i, String s, boolean b) {
            logger.info("Websocket connection closed: {}", webSocket.getRemoteSocketAddress());
        }

        @Override
        public void onMessage(WebSocket webSocket, String s) {
            clearNormalMessageQueue();
            MQClient client = new MQClient(webSocket);
            logger.trace(s);
            String[] splitMessage = s.split("\n");

            // parse message content
            StringBuilder message = new StringBuilder();
            for (String i : splitMessage) {
                if (MQProtocol.Head.END.name().equals(i)
                        || MQProtocol.Head.PRODUCE.name().equals(i)
                        || MQProtocol.Head.CONSUME.name().equals(i)
                        || MQProtocol.Head.REGISTER.name().equals(i)
                        || MQProtocol.Head.REDIRECT.name().equals(i)
                ) {
                    continue;
                }
                message.append(i);
            }
            logger.debug(message.toString());
            MQMessage incomingMQMessage;
            try {
                incomingMQMessage = jsonParser.fromJson(message.toString(), MQMessage.class);
                assert incomingMQMessage != null;
                assert incomingMQMessage.token != null;
                assert incomingMQMessage.group != null;
                client.setToken(incomingMQMessage.token);
                client.setGroup(incomingMQMessage.group);
            } catch (Exception e) {
                logger.warn("Invalid json.");
                logger.debug(message.toString());
                return;
            }

            logger.info("Incoming {} message from {}.", splitMessage[0], webSocket.getRemoteSocketAddress());
            if (MQProtocol.Head.REDIRECT.name().equals(splitMessage[0])) {
                // auth from logic server
                if (incomingMQMessage.msg != null) {
                    UUID targetUUID = UUID.fromString(incomingMQMessage.msg);
                    if (targetUUID.toString().equals(incomingMQMessage.msg)) {
                        if (!clients.get(MQProtocol.Group.LOGIC_SERVER).contains(client)) {
                            client.close(constructFailedResponse("Unauthorized AUTH behaviour!"));
                            logger.debug(client.toString());
                            logger.warn("Unauthorized AUTH behaviour!");
                        } else {
                            if (!MQProtocol.Status.SUCCESS.equals(incomingMQMessage.status)) {
                                try {
                                    client.setToken(UUID.fromString(incomingMQMessage.msg));
                                    incomingMQMessage.token = null;
                                    incomingMQMessage.code = MQProtocol.Code.REGISTER_FAILED.getCode();
                                    incomingMQMessage.msg = "Register failed.";

                                    clients.get(MQProtocol.Group.GUEST)
                                            .get(clients.get(MQProtocol.Group.GUEST)
                                                    .indexOf(client))
                                            .send(incomingMQMessage);
                                    return;
                                } catch (Exception e) {
                                    logger.warn(e.toString());
                                    return;
                                }
                            }
                            client.setToken(targetUUID);
                            if (clients.get(MQProtocol.Group.GUEST).contains(client)) {
                                // move client from guest group to requested group
                                client.setWebSocket(
                                        clients.get(MQProtocol.Group.GUEST)
                                                .get(clients.get(MQProtocol.Group.GUEST)
                                                        .indexOf(client))
                                                .getWebSocket());
                                clients.get(incomingMQMessage.group).add(client);
                                clients.get(MQProtocol.Group.GUEST).remove(client);
                                // replace token
                                // incomingMQMessage.token = tokens.get(incomingMQMessage.group);
                                // replace op code
                                incomingMQMessage.code = MQProtocol.Code.UPDATE_TOKEN.getCode();
                                incomingMQMessage.token = targetUUID;
                                incomingMQMessage.msg = "Access approved.";
                                client.send(incomingMQMessage);
                                logger.info("Register for {} to Group {} successfully."
                                        , client.getToken()
                                        , incomingMQMessage.group
                                );
                            } else {
                                logger.warn("No specified session found, maybe closed.");
                            }
                        }
                    } else {
                        logger.warn("Invalid client UUID.");
                        client.close(constructFailedResponse("Invalid UUID in message."));
                    }
                } else {
                    logger.warn("Invalid client UUID.");
                    client.send(constructFailedResponse("Invalid UUID in message."));
                }

            } else if (MQProtocol.Head.PRODUCE.name().equals(splitMessage[0])) {
                // produce
                if (
                        !clients.get(incomingMQMessage.group).contains(client)
                                || incomingMQMessage.code == null
                                || !incomingMQMessage.group.hasPrivilegeToDo(MQProtocol.Code.fromInteger(incomingMQMessage.code))
                ) {
                    // no privilege
                    logger.warn("Detect access denied behaviour: {}.",
                            MQProtocol.Code.fromInteger(incomingMQMessage.code));
                    client.send(constructFailedResponse("Access denied!"));
                    return;
                } else {
                    // call auto broadcast
                    if (!autoBroadcast(incomingMQMessage)) {
                        // broadcast failed, save message
                        // there's no AUTH message from PRODUCE method
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
                        !incomingMQMessage.group.getInitializedUUID().equals(incomingMQMessage.token)
                                || (
                                        MQProtocol.Group.LOGIC_SERVER.equals(incomingMQMessage.group)
                                        && clients.get(MQProtocol.Group.LOGIC_SERVER).size() >=
                                        MQProtocol.Group.LOGIC_SERVER.getCapacityLimit() && !allLogicServerDown()
                                    )
                                || clients.get(incomingMQMessage.group).size() >= incomingMQMessage.group.getCapacityLimit()
                                || MQProtocol.Group.GUEST.equals(incomingMQMessage.group)
                ) {
                    // invalid register message
                    client.close(constructFailedResponse("Invalid register message."));
                    logger.warn("Receive invalid register message.");
                    logger.debug(s);
                } else {
                    if (incomingMQMessage.group == MQProtocol.Group.LOGIC_SERVER) {
                        // register immediately
                        client.setToken(UUID.randomUUID());
                        clients.get(MQProtocol.Group.LOGIC_SERVER).add(client);
                        incomingMQMessage.token = client.getToken();
                        incomingMQMessage.code = MQProtocol.Code.UPDATE_TOKEN.getCode();
                        incomingMQMessage.status = MQProtocol.Status.SUCCESS;
                        client.send(incomingMQMessage);
                        clearRegisterMessageQueue();
                        return;
                    }
                    // replace token with random token
                    incomingMQMessage.token = UUID.randomUUID();
                    // replace op code to AUTH
                    incomingMQMessage.code = MQProtocol.Code.AUTH.getCode();
                    client.setToken(incomingMQMessage.token);

                    if (allLogicServerDown()) {
                        logger.warn("All logic server down, no body can process auth message.");
                        authMessages.addLast(incomingMQMessage);
                    }
                    // send to logic server with no response and add it to guest
                    clients.get(MQProtocol.Group.GUEST).add(client);
                    autoBroadcast(incomingMQMessage);
                }
            }
        }

        private boolean allLogicServerDown() {
            for (MQClient w: clients.get(MQProtocol.Group.LOGIC_SERVER)) {
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
