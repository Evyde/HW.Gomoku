package jlu.evyde.gobang.Client.Model;

import com.google.gson.Gson;
import jlu.evyde.gobang.Client.Controller.Callback;
import jlu.evyde.gobang.Client.Controller.GobangException;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;
import java.util.LinkedList;

public class WebSocketMQServer implements MQBrokerServer {
    private WebSocketServer wss;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // TODO: Change this to multiple kind of messages to cover MQ_Source for better performance.
    // Obviously FIFO, use linked list as double ends queue.
    private final Deque<MQMessage> messages = new LinkedList<>();

    public Deque<MQMessage> getMessages() {
        return messages;
    }

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
        public RealWebSocketMQServer(@NotNull MQServerAddress msa, @NotNull Callback startComplete) {
            super(msa.getIsa());
            this.startComplete = startComplete;
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
            logger.info("Websocket connection closed: {}", webSocket.getRemoteSocketAddress());
        }

        @Override
        public void onMessage(WebSocket webSocket, String s) {
            logger.debug(s);
            String[] splitMessage = s.split("\n");

            StringBuilder message = new StringBuilder();
            for (String i : splitMessage) {
                if (SystemConfiguration.getMQMsgEnd().equals(i)
                        || SystemConfiguration.getMQProduceHead().equals(i)
                        || SystemConfiguration.getMQConsumeHead().equals(i)) {
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
            }

            logger.info("Incoming {} message from {}.", splitMessage[0], webSocket.getRemoteSocketAddress());
            if (SystemConfiguration.getMQProduceHead().equals(splitMessage[0])) {

                if (j != null) {
                    messages.addLast(j);
                }
            } else if (SystemConfiguration.getMQConsumeHead().equals(splitMessage[0])) {
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
            } else if (SystemConfiguration.getMQGroupHead().equals(splitMessage[0])) {
                try {
                    for (MQMessage m: messages) {
                        if (m.from.consume(j.from)) {
                            webSocket.send(jsonParser.toJson(m));
                            messages.remove(m);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Invalid group content.");
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
