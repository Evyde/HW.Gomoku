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

import static java.lang.Thread.sleep;

// TODO: Fix cannot remove from Guest error.
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
            wss.setReuseAddr(true);
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
            wss.stop(SystemConfiguration.getSleepTime());
            sleep((long) SystemConfiguration.getSleepTime() * SystemConfiguration.getMaxRetryTime());
        } catch (InterruptedException ie) {
            logger.error(ie.toString());
            System.exit(5);
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

        private final Map<MQProtocol.Group, Map<MQClient, UUID>> clients = new ConcurrentHashMap<>();
        private final transient ReentrantLock broadcastLock = new ReentrantLock();

        public RealWebSocketMQServer(MQServerAddress msa, Callback startComplete) {
            super(msa.getIsa());
            this.startComplete = startComplete;
            for (MQProtocol.Group g: MQProtocol.Group.getAllGroup()) {
                clients.put(g, new ConcurrentHashMap<>());
            }
        }

        private static MQMessage constructDisconnectMessage(UUID token, MQProtocol.Group group) {
            MQMessage m = new MQMessage();
            m.status = MQProtocol.Status.SUCCESS;
            m.code = MQProtocol.Code.DISCONNECT.getCode();
            m.msg = token.toString();
            m.group = group;
            return m;
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

        private static String constructRegisterFailedResponse(String msg) {
            MQMessage m = new MQMessage();
            m.msg = msg;
            m.group = MQProtocol.Group.LOGIC_SERVER;
            m.status = MQProtocol.Status.FAILED;
            m.code = MQProtocol.Code.REGISTER_FAILED.getCode();
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
            if (!MQProtocol.Code.AUTH.getCode().equals(m.code)) {
                m.token = null;
            }
            boolean broadcastStatus = false;
            while (broadcastLock.isLocked()) {
                Thread.onSpinWait();
            }
            logger.info("Broadcast to all available consumers.");
            broadcastLock.lock();
            for (MQProtocol.Group g: m.group.pushMyMessageTo()) {
                for (MQClient client: clients.get(g).keySet()) {
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
        }

        @Override
        public void onClose(WebSocket webSocket, int i, String s, boolean b) {
            logger.info("Websocket connection closed: {}", webSocket.getRemoteSocketAddress());
            for (MQProtocol.Group group: MQProtocol.Group.getAllGroup()) {
                for (MQClient c: clients.get(group).keySet()) {
                    if (webSocket.equals(c.getWebSocket())) {
                        autoBroadcast(constructDisconnectMessage(c.getToken(), c.getGroup()));
                        clients.get(group).remove(c);
                    }
                }
            }
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
                if (
                        incomingMQMessage == null ||
                                incomingMQMessage.token == null ||
                                incomingMQMessage.group == null
                ) {
                    logger.warn("Invalid json.");
                    logger.debug(message.toString());
                    return;
                }
                client.setToken(incomingMQMessage.token);
                client.setGroup(incomingMQMessage.group);
            } catch (Exception e) {
                e.printStackTrace();
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
                        if (!clients.get(MQProtocol.Group.LOGIC_SERVER).containsKey(client)) {
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

                                    for (MQClient c: clients.get(MQProtocol.Group.GUEST).keySet()) {
                                        if (client.equals(c)) {
                                            c.send(incomingMQMessage);
                                            clients.get(MQProtocol.Group.GUEST).remove(c);
                                        }
                                    }
                                    return;
                                } catch (Exception e) {
                                    logger.warn(e.toString());
                                    return;
                                }
                            }
                            client.setToken(targetUUID);
                            if (clients.get(MQProtocol.Group.GUEST).containsKey(client)) {
                                // move client from guest group to requested group
                                for (MQClient c: clients.get(MQProtocol.Group.GUEST).keySet()) {
                                    if (client.equals(c)) {
                                        client.setWebSocket(c.getWebSocket());
                                    }
                                }
                                clients.get(incomingMQMessage.group).put(client, client.getToken());
                                clients.get(MQProtocol.Group.GUEST).remove(client);
                                // replace token
                                // incomingMQMessage.token = tokens.get(incomingMQMessage.group);
                                // replace op code
                                incomingMQMessage.code = MQProtocol.Code.UPDATE_TOKEN.getCode();
                                incomingMQMessage.token = targetUUID;
                                incomingMQMessage.msg = "Access approved.";
                                logger.info("Register for {} to Group {} successfully."
                                        , client.getToken()
                                        , incomingMQMessage.group
                                );
                                incomingMQMessage.group = MQProtocol.Group.LOGIC_SERVER;
                                client.send(incomingMQMessage);

                                for (MQMessage m: persistenceMessages) {
                                    client.send(m);
                                }
                                clearNormalMessageQueue();
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
                        !clients.get(incomingMQMessage.group).containsKey(client)
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
                        if (MQProtocol.Group.LOGIC_SERVER.equals(incomingMQMessage.group)) {
                            persistenceMessages.addLast(incomingMQMessage);
                        }
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
                    client.close(constructRegisterFailedResponse("Invalid register message."));
                    for (MQClient c: clients.get(MQProtocol.Group.GUEST).keySet()) {
                        if (c == null || c.isClosed()) {
                            clients.get(MQProtocol.Group.GUEST).remove(c);
                        }
                    }
                    logger.warn("Receive invalid register message.");
                    logger.debug(s);
                } else {
                    if (incomingMQMessage.group == MQProtocol.Group.LOGIC_SERVER) {
                        // register immediately
                        client.setToken(UUID.randomUUID());
                        // remove from guest
                        clients.get(MQProtocol.Group.GUEST).remove(client);
                        clients.get(MQProtocol.Group.LOGIC_SERVER).put(client, client.getToken());
                        incomingMQMessage.token = client.getToken();
                        incomingMQMessage.code = MQProtocol.Code.UPDATE_TOKEN.getCode();
                        incomingMQMessage.status = MQProtocol.Status.SUCCESS;
                        client.send(incomingMQMessage);
                        logger.warn("REGISTER for logic server {} approved!", client.getWebSocket().getRemoteSocketAddress());
                        clearRegisterMessageQueue();
                        return;
                    } else if (incomingMQMessage.group == MQProtocol.Group.WATCHER) {
                        // register immediately
                        client.setToken(UUID.randomUUID());
                        // remove from guest
                        clients.get(MQProtocol.Group.GUEST).remove(client);
                        clients.get(MQProtocol.Group.WATCHER).put(client, client.getToken());
                        incomingMQMessage.token = client.getToken();
                        incomingMQMessage.code = MQProtocol.Code.UPDATE_TOKEN.getCode();
                        incomingMQMessage.status = MQProtocol.Status.SUCCESS;
                        client.send(incomingMQMessage);
                        for (MQMessage m: persistenceMessages) {
                            client.send(m);
                        }
                        return;
                    }
                    // check if this guy try to register twice
                    for (MQProtocol.Group g: MQProtocol.Group.getAllGroup()) {
                        for (MQClient c: clients.get(g).keySet()) {
                            if (client.getWebSocket().equals(c.getWebSocket())) {
                                logger.warn("Duplicate register message.");
                                c.send(constructRegisterFailedResponse("Duplicate register message."));
                                // clients.get(g).remove(c);
                                autoBroadcast(constructDisconnectMessage(c.getToken(), g));
                                return;
                            }
                        }
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
                    // check if group has enough room
                    if (clients.get(MQProtocol.Group.GUEST).size() < MQProtocol.Group.GUEST.getCapacityLimit()) {
                        // update this four data structure
                        client.setGroup(MQProtocol.Group.GUEST);
                        clients.get(MQProtocol.Group.GUEST).put(client, client.getToken());
                    } else {
                        logger.warn("Guest queue is full, may be logic server error.");
                        client.close(constructFailedResponse("Sorry, the queue of guest is full, try connect later."));
                    }
                    autoBroadcast(incomingMQMessage);
                }
            }
        }

        private boolean allLogicServerDown() {
            for (MQClient w: clients.get(MQProtocol.Group.LOGIC_SERVER).keySet()) {
                if (w != null && !w.isClosed()) {
                    return false;
                } else {
                    clients.get(MQProtocol.Group.LOGIC_SERVER).remove(w);
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
            e.printStackTrace();
            try {
                this.stop();
            } catch (InterruptedException ex) {
                e.printStackTrace();
            }
            // System.exit(5);
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
