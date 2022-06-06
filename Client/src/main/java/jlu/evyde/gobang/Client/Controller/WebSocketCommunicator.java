package jlu.evyde.gobang.Client.Controller;

import jlu.evyde.gobang.Client.Model.MQMessage;
import jlu.evyde.gobang.Client.Model.MQProtocol;
import jlu.evyde.gobang.Client.Model.MQServerAddress;
import jlu.evyde.gobang.Client.Model.SystemConfiguration;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.UUID;

import static java.lang.Thread.sleep;

public class WebSocketCommunicator implements Communicator {
    private RealWebSocketCommunicator wsc;
    private MQProtocol.MQSource id;
    private CommunicatorReceiveListener listener;
    private final Logger logger = LoggerFactory.getLogger(WebSocketCommunicator.class);
    private UUID communicatorToken = null;
    private Integer triedTimes = 0;

    /**
     * Connect to message queue server.
     *
     * @param msa Message queue server address.
     * @throws GobangException.CommunicatorInitFailedException Init failed.
     */
    @Override
    public void connect(MQServerAddress msa) throws GobangException.CommunicatorInitFailedException {
        try {
            wsc = new RealWebSocketCommunicator(msa);
            wsc.connect();
        } catch (Exception e) {

        }
    }

    /**
     * Returns connection status.
     *
     * @return true if is connected.
     */
    @Override
    public boolean connected() {
        return wsc.isOpen();
    }

    /**
     * Register this communicator to MQ server.
     *
     * @param id      Message queue source.
     * @param success Callback function when registered successfully.
     * @param failed  Callback function when registered failed.
     */
    @Override
    public void register(MQProtocol.MQSource id, Callback success, Callback failed) {
        this.id = id;
        if (SystemConfiguration.getMaxRetryTime().equals(this.triedTimes)) {
            failed.run();
            logger.error("Register reached in max retry time, abandon");
        }
        try {
            wsc.send(MQMessage.constructRegisterMessage(id));
            Integer counter = SystemConfiguration.getMaxRetryTime();
            while (this.communicatorToken != null) {
                if (counter-- <= 0) {
                    this.triedTimes++;
                    register(id, success, failed);
                }
                try {
                    sleep(SystemConfiguration.getSleepTime());
                } catch (Exception e) {
                    continue;
                }
            }
            this.triedTimes = 0;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Register error, retry {} times.", SystemConfiguration.getMaxRetryTime());
            this.triedTimes++;
            register(id, success, failed);
        }
        if (this.triedTimes == 0) {
            success.run();
        }
        this.triedTimes = 0;
    }


    /**
     * Send produce request to MQ server.
     *
     * @param message      Message in json format.
     * @param sendComplete Callback method when send complete.
     * @param sendError    Callback method when send error.
     */
    @Override
    public void produce(MQMessage message, Callback sendComplete, Callback sendError) {
        try {
            wsc.send(message, MQProtocol.Head.PRODUCE);
        } catch (Exception e) {
            logger.error("Produce error.");
            e.printStackTrace();
            sendError.run();
        }
        sendComplete.run();
    }

    /**
     * Put chess to MQ server.
     *
     * @param chess Chess object that put.
     */
    @Override
    public void put(MQProtocol.Chess chess) {
        MQMessage put = new MQMessage();
        put.from = id;
        put.chess = chess;
        put.status = MQProtocol.Status.SUCCESS;
        put.code = MQProtocol.Code.PUT_CHESS.getCode();
        produce(put, () -> { logger.info("Put {}.", chess.toString()); }, () -> { logger.warn("Put {} failed.",
                chess.toString()); });
    }

    /**
     * Push recall last step message to MQ server.
     */
    @Override
    public void recall() {
        MQMessage recall = new MQMessage();
        recall.code = MQProtocol.Code.RECALL.getCode();
        recall.status = MQProtocol.Status.SUCCESS;
        recall.from = id;
        recall.chess = new MQProtocol.Chess(new Point(), MQProtocol.Chess.Color.WHITE);
        produce(recall, () -> { logger.info("Recall."); }, () -> { logger.warn("Recall failed."); });
    }

    /**
     * Let the specific color of chess to win.
     *
     * @param color Chess color to win.
     */
    @Override
    public void win(MQProtocol.Chess.Color color) {
        MQMessage win = new MQMessage();
        win.from = id;
        win.chess = new MQProtocol.Chess(new Point(), color);
        win.status = MQProtocol.Status.SUCCESS;
        win.code = color == MQProtocol.Chess.Color.WHITE?
                MQProtocol.Code.WHITE_WIN.getCode(): MQProtocol.Code.BLACK_WIN.getCode();
        produce(win, () -> { logger.info("{} wins.", color.toString()); },
                () -> { logger.warn("{} wins failed.", color.toString()); });
    }

    /**
     * Add receive listener to this communicator.
     * When message comes in, call this listener.
     *
     * @param crl CommunicatorReceiveListener class.
     */
    @Override
    public void addReceiveListener(CommunicatorReceiveListener crl) {
        assert crl != null;
        listener = crl;
    }

    private class RealWebSocketCommunicator extends WebSocketClient {

        public RealWebSocketCommunicator(MQServerAddress msa) {
            super(msa.getUri());
        }

        @Override
        public void onOpen(ServerHandshake serverHandshake) {

        }

        @Override
        public void onMessage(String s) {
            listener.beforeReceive();
            MQMessage m;
            try {
                m = MQMessage.fromJson(s);
            } catch (Exception e) {
                logger.warn("Received invalid message {}.", s);
                return;
            }
            assert m != null;
            if ((communicatorToken == null || MQProtocol.Code.UPDATE_TOKEN.getCode().equals(m.code))
                    && m.from == MQProtocol.MQSource.SERVER) {
                if (m.token != null) {
                    communicatorToken = m.token;
                } else {
                    System.err.println("Server returns wrong token!");
                }
            } else {
                listener.doReceive(m);
            }
            listener.afterReceive();
        }

        @Override
        public void onClose(int i, String s, boolean b) {

        }

        @Override
        public void onError(Exception e) {

        }

        public void send(MQMessage m, MQProtocol.Head p) {
            m.token = communicatorToken;
            m.from = id;
            send(MQProtocol.Head.constructRequest(p, m));
        }
    }
}
