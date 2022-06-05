package jlu.evyde.gobang.Client.Model;

import com.google.gson.Gson;
import jlu.evyde.gobang.Client.Controller.Callback;
import jlu.evyde.gobang.Client.Controller.GobangException;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public class WebSocketMQServer implements MQBrokerServer {
    private WebSocketServer wss;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // TODO: Change this to multiple kind of messages to cover MQ_Source for better performance.


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
        private WebSocket logicServer;
        private HashSet<WebSocket> uis = new HashSet<>();
        // Obviously FIFO, use linked list as double ends queue.
        private final Deque<MQMessage> messages = new LinkedList<>();
        private Deque<MQMessage> persistenceMessages = new LinkedList<>();

        public RealWebSocketMQServer(MQServerAddress msa, Callback startComplete) {
            super(msa.getIsa());
            this.startComplete = startComplete;
        }

        /**
         * Broadcast message to UI or LogicServer automatically.
         * @param m Message to broadcast.
         * @return true if this message has consumer.
         */
        private boolean autoBroadcast(MQMessage m) {
            if (SystemConfiguration.MQ_Source.UI.consume(m.from) && !uis.isEmpty()) {
                boolean uiBroadcast = false;
                for (WebSocket ws: uis) {
                    if (ws.isClosed()) {
                        uis.remove(ws);
                    } else {
                        ws.send(m.toJson());
                        uiBroadcast = true;
                    }
                }
                return uiBroadcast;
            } else if (SystemConfiguration.MQ_Source.LOGIC.consume(m.from)
                    && (logicServer != null || !uis.isEmpty())) {
                // broadcast to all
                boolean broadcast = false;
                for (WebSocket ws: uis) {
                    if (ws.isClosed()) {
                        uis.remove(ws);
                    } else {
                        ws.send(m.toJson());
                        broadcast = true;
                    }
                }
                if (logicServer == null || logicServer.isClosed()) {
                    logicServer = null;
                } else {
                    logicServer.send(m.toJson());
                    broadcast = true;
                }
                return broadcast;
            }
            return false;
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
                uis.remove(webSocket);
            }
            logger.info("Websocket connection closed: {}", webSocket.getRemoteSocketAddress());
        }

        @Override
        public void onMessage(WebSocket webSocket, String s) {
            if ((logicServer != null || !uis.isEmpty()) && !messages.isEmpty()) {
                // clear message queue
                logger.debug("Clearing message queue.");
                while (!messages.isEmpty() && autoBroadcast(messages.peekFirst())) {
                    messages.removeFirst();
                }
            }
            logger.debug(s);
            String[] splitMessage = s.split("\n");

            // parse message content
            StringBuilder message = new StringBuilder();
            for (String i : splitMessage) {
                if (SystemConfiguration.getMQMsgEnd().equals(i)
                        || SystemConfiguration.getMQProduceHead().equals(i)
                        || SystemConfiguration.getMQConsumeHead().equals(i)
                        || SystemConfiguration.getMqRegisterHead().equals(i)
                ) {
                    continue;
                }
                message.append(i);
            }
            logger.debug(message.toString());
            MQMessage j = null;
            try {
                j = jsonParser.fromJson(message.toString(), MQMessage.class);
            } catch (Exception e) {
                logger.warn("Invalid json.");
                logger.debug(message.toString());
                return;
            }

            logger.info("Incoming {} message from {}.", splitMessage[0], webSocket.getRemoteSocketAddress());
            if (SystemConfiguration.getMQProduceHead().equals(splitMessage[0])) {

                if (j != null) {
                    if (!autoBroadcast(j)) {
                        if (j.from != null) {
                            messages.addLast(j);
                        }
                    }
                }

            } else if (SystemConfiguration.getMQConsumeHead().equals(splitMessage[0])) {
                // consume for this guy (debug and legacy tests only)
                if (!messages.isEmpty()) {
                    try {
                        for (MQMessage m: messages) {
                            if (m.from.consume(j.from)) {
                                webSocket.send(jsonParser.toJson(m));
                                messages.remove(m);
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("Invalid consume content.");
                    }
                }
            } else if (SystemConfiguration.getMqRegisterHead().equals(splitMessage[0])) {
                // Register
                try {
                    if (j.from != null) {
                        if (j.from == SystemConfiguration.MQ_Source.UI) {
                            uis.add(webSocket);
                        } else if (j.from == SystemConfiguration.MQ_Source.LOGIC) {
                            if (logicServer == null || logicServer.isClosed()) {
                                logicServer = webSocket;
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Register for {} websocket {} failed.",
                            j.from,
                            webSocket.getRemoteSocketAddress());
                }
            }
        }

        @Override
        public void onError(WebSocket webSocket, Exception e) {
            logger.error(
                    "Websocket {} raise error: {}",
                    webSocket.getRemoteSocketAddress(),
                    e.toString()
            );
        }

        @Override
        public void onStart() {
            startComplete.run();
        }
    }
}
