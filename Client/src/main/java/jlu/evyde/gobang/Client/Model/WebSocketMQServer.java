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
        private final Deque<MQMessage> messages = new LinkedList<>();
        private final Set<WebSocket> uis = Collections.synchronizedSet(new HashSet<>());
        private final Deque<MQMessage> persistenceMessages = new LinkedList<>();
        private final Map<WebSocket, UUID> uiUUIDTokens = Collections.synchronizedMap(new HashMap<>());
        private WebSocket logicServer;
        private UUID logicServerUUIDToken;
        private boolean broadcastLock = false;

        public RealWebSocketMQServer(MQServerAddress msa, Callback startComplete) {
            super(msa.getIsa());
            this.startComplete = startComplete;
        }

        private static String constructRegisterSuccessfulResponse(UUID token) {
            MQMessage m = new MQMessage();
            m.status = MQProtocol.Status.SUCCESS;
            m.code = MQProtocol.Code.UPDATE_TOKEN.getCode();
            m.msg = "Update token";
            m.from = MQProtocol.MQSource.SERVER;
            m.token = token;
            return m.toJson();
        }

        private static String constructRegisterFailedResponse(String msg) {
            MQMessage m = new MQMessage();
            m.msg = msg;
            m.from = MQProtocol.MQSource.SERVER;
            m.status = MQProtocol.Status.FAILED;
            return m.toJson();
        }

        /**
         * Broadcast message to UI or LogicServer automatically.
         *
         * @param m Message to broadcast.
         * @return true if this message has consumer.
         */
        private boolean autoBroadcast(MQMessage m) {
            if (broadcastLocked()) {
                logger.warn("Broadcast locked.");
                return false;
            }
            logger.info("Broadcast to all available consumers.");
            boolean broadcast = false;
            synchronized (uis) {
                synchronized (uiUUIDTokens) {
                    if (MQProtocol.MQSource.UI.canConsume(m.from) && !uis.isEmpty()) {
                        for (WebSocket ws : uis) {
                            if (ws == null || ws.isClosed()) {
                                uis.remove(ws);
                                uiUUIDTokens.remove(ws);
                            } else {
                                try {
                                    ws.send(m.toJson());
                                    broadcast = true;
                                } catch (RuntimeException re) {
                                    logger.warn("Unexpected client close.");
                                    re.printStackTrace();
                                }
                            }
                        }
                        if (broadcast) {
                            logger.debug("Broadcast to all UI successfully.");
                        }
                    }
                    if (MQProtocol.MQSource.LOGIC.canConsume(m.from)
                            && (logicServer != null)) {
                        // broadcast to logic
                        if (logicServer.isClosed()) {
                            logicServer = null;
                        } else {
                            logicServer.send(m.toJson());
                            logger.debug("Broadcast to logic server successfully.");
                            broadcast = true;
                        }
                    }
                    if (!broadcast) {
                        logger.warn("Broadcast called but nobody can talk.");
                    }
                    return broadcast;
                }
            }
        }

        @Override
        public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
            logger.info("New websocket open: {}/{}",
                    clientHandshake.getResourceDescriptor(),
                    webSocket.getRemoteSocketAddress()
            );
        }

        @Override
        public void onClose(WebSocket webSocket, int i, String s, boolean b) {
            if (webSocket == logicServer) {
                logger.warn("Logic server down.");
                logicServer = null;
            } else {
                synchronized (uis) {
                    uis.remove(webSocket);
                }
                synchronized (uiUUIDTokens) {
                    uiUUIDTokens.remove(webSocket);
                }
            }
            logger.info("Websocket connection closed: {}", webSocket.getRemoteSocketAddress());
        }

        @Override
        public void onMessage(WebSocket webSocket, String s) {
            clearMessageQueue();
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
            if (MQProtocol.Head.PRODUCE.name().equals(splitMessage[0])) {
                // produce
                synchronized (uiUUIDTokens) {
                    if (
                            Objects.equals(uiUUIDTokens.get(webSocket), incomingMQMessage.token)
                                    || Objects.equals(logicServerUUIDToken, incomingMQMessage.token)
                    ) {
                        if (!autoBroadcast(incomingMQMessage)) {
                            if (incomingMQMessage.from != null) {
                                messages.addLast(incomingMQMessage);
                            }
                        } else {
                            persistenceMessages.addLast(incomingMQMessage);
                        }
                    } else {
                        logger.warn("Unauthorized behaviour!");
                    }
                }
            } else if (MQProtocol.Head.CONSUME.name().equals(splitMessage[0])) {
                // consume for this guy (debug and legacy tests only)
                if (!messages.isEmpty()) {
                    try {
                        for (MQMessage m : messages) {
                            if (m.from.canConsume(incomingMQMessage.from)) {
                                webSocket.send(m.toJson());
                                messages.remove(m);
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("Invalid consume content.");
                    }
                }
            } else if (MQProtocol.Head.REGISTER.name().equals(splitMessage[0])) {
                // Register
                // TODO: Could let token has expired time.
                acquireBroadcastLock();
                try {
                    // Check initialized token
                    if (incomingMQMessage.token == null
                            || !incomingMQMessage.token.equals(SystemConfiguration.getInitializedUuid())) {
                        logger.warn("Unauthorized register!");
                        webSocket.send(constructRegisterFailedResponse("Unauthorized register!"));
                    } else {
                        // generate random token
                        UUID randomToken = UUID.randomUUID();
                        if (incomingMQMessage.from != null) {
                            if (incomingMQMessage.from == MQProtocol.MQSource.UI) {
                                synchronized (uis) {
                                    uis.add(webSocket);
                                }
                                synchronized (uiUUIDTokens) {
                                    uiUUIDTokens.put(webSocket, randomToken);
                                }
                                // TODO: Change this to flexible way
                                webSocket.send(constructRegisterSuccessfulResponse(randomToken));

                                // send all history for reconnect
                                if (!persistenceMessages.isEmpty()) {
                                    for (MQMessage m : persistenceMessages) {
                                        webSocket.send(m.toJson());
                                    }
                                }
                            } else if (incomingMQMessage.from == MQProtocol.MQSource.LOGIC) {
                                if (logicServer == null || logicServer.isClosed()) {
                                    logicServer = webSocket;
                                    logicServerUUIDToken = randomToken;
                                    webSocket.send(constructRegisterSuccessfulResponse(randomToken));

                                    // send all history for reconnection
                                    if (!persistenceMessages.isEmpty()) {
                                        for (MQMessage m : persistenceMessages) {
                                            if (m.from != MQProtocol.MQSource.LOGIC) {
                                                webSocket.send(m.toJson());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    releaseBroadcastLock();
                } catch (Exception e) {
                    logger.warn("Register for {} websocket {} failed.",
                            incomingMQMessage.from,
                            webSocket.getRemoteSocketAddress());
                    releaseBroadcastLock();
                }
                releaseBroadcastLock();
            }
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

        private void acquireBroadcastLock() {
            broadcastLock = true;
        }

        private void releaseBroadcastLock() {
            broadcastLock = false;
            clearMessageQueue();
        }

        private boolean broadcastLocked() {
            return broadcastLock;
        }

        private void clearMessageQueue() {
            if ((logicServer != null || !uis.isEmpty()) && !messages.isEmpty()) {
                // clear message queue
                logger.debug("Clearing message queue.");
                while (!messages.isEmpty() && autoBroadcast(messages.peekFirst())) {
                    persistenceMessages.addLast(messages.removeFirst());
                }
            }
        }
    }
}
