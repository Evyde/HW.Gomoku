package jlu.evyde.gobang.Client.Model;

import jlu.evyde.gobang.Client.Controller.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.lang.Thread.sleep;

public class LogicServer {
    private Communicator communicator = CommunicatorFactory.getWebSocketCommunicator();
    private Logger logger = LoggerFactory.getLogger(LogicServer.class);
    private List<MQProtocol.Chess.Color> gamer = new CopyOnWriteArrayList<>();
    private MQProtocol.Chess.Color[][] board = new MQProtocol.Chess.Color
            [SystemConfiguration.getBoardHeight()][SystemConfiguration.getBoardWidth()];
    private MQProtocol.Chess.Color nowMove = SystemConfiguration.getFIRST().equals(MQProtocol.Chess.Color.WHITE)?
            MQProtocol.Chess.Color.WHITE: MQProtocol.Chess.Color.BLACK;

    public LogicServer() {
        try {
            communicator.addReceiveListener(new LogicCommunicatorReceiveListener() {
                @Override
                public void doReceive(MQMessage msg) {
                    if (MQProtocol.Code.AUTH.getCode().equals(msg.code)) {
                        if (MQProtocol.Group.GAMER.equals(msg.group)) {
                            if (msg.chess != null && msg.chess.getColor() != null) {
                                if (!gamer.contains(msg.chess.getColor())) {
                                    registerSuccess(msg);
                                } else {
                                    registerFailed(msg);
                                }
                            } else {
                                registerFailed(msg);
                            }
                        } else if (MQProtocol.Group.WATCHER.equals(msg.group)) {
                            registerSuccess(msg);
                        } else {
                            registerFailed(msg);
                        }
                    } else if (MQProtocol.Code.PUT_CHESS.getCode().equals(msg.code)) {
                        if (msg.chess != null && msg.chess.getColor() != null) {
                            if (nowMove.equals(msg.chess.getColor())) {
                                if (setChessAt(msg.chess)) {
                                    nowMove = nowMove.equals(MQProtocol.Chess.Color.WHITE)?
                                            MQProtocol.Chess.Color.BLACK: MQProtocol.Chess.Color.WHITE;
                                    communicator.put(msg.chess);
                                    // isWin(msg.chess);
                                }
                            }
                        }
                    }
                }

                @Override
                public void beforeReceive() {
                    logger.debug("Receiving message.");
                }


                @Override
                public void afterReceive() {
                    logger.debug("Receive complete.");
                }
            });
            communicator.connect(new MQServerAddress());
            Integer counter = SystemConfiguration.getMaxRetryTime();
            while (!communicator.connected()) {
                if (counter-- <= 0) {
                    logger.error("Could not start logic server.");
                    System.exit(6);
                }
                sleep(SystemConfiguration.getSleepTime());
            }
            MQMessage registerMessage = new MQMessage();
            registerMessage.group = MQProtocol.Group.LOGIC_SERVER;
            registerMessage.token = MQProtocol.Group.LOGIC_SERVER.getInitializedUUID();
            communicator.register(registerMessage,
                    () -> { logger.info("Logic server started."); },
                    () -> {
                        logger.error("Register to MQ failed.");
                        throw new GobangException.LogicCommunicatorInitFailedException();
                    });
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Logic server may start failed.");
            throw new GobangException.LogicCommunicatorInitFailedException();
        }
    }

    private void registerSuccess(MQMessage msg) {
        msg.msg = msg.token.toString();
        msg.status = MQProtocol.Status.SUCCESS;
        communicator.redirect(msg, () -> {
            logger.warn("Register for {} to Group {} approved.", msg.msg, msg.group);
        }, () -> {
            logger.error("Send auth success message failed.");
        });
    }

    private void registerFailed(MQMessage msg) {
        msg.msg = msg.token.toString();
        msg.status = MQProtocol.Status.FAILED;
        msg.chess = null;
        communicator.redirect(msg, () -> {
            logger.warn("Register for {} to Group {} denied.", msg.msg, msg.group);
        }, () -> {
            logger.error("Send auth failed message failed.");
        });
    }

    public void closeServer() {
        communicator.close();
    }

    private MQProtocol.Chess.Color getChessAt(MQProtocol.Chess chess) {
        if (chess != null && chess.getPosition() != null) {
            return getChessAt((int) Math.floor(chess.getPosition().getX()), (int) Math.floor(chess.getPosition().getY()));
        }
        return null;
    }

    private MQProtocol.Chess.Color getChessAt(Integer x, Integer y) {
        return this.board[y][x];
    }

    private boolean setChessAt(MQProtocol.Chess chess) {
        if (chess != null && chess.getPosition() != null && chess.getColor() != null) {
            return setChessAt((int) Math.floor(chess.getPosition().getX()), (int) Math.floor(chess.getPosition().getY()),
                    chess.getColor());
        }
        return false;
    }

    private boolean setChessAt(Integer x, Integer y, MQProtocol.Chess.Color color) {
        if (this.getChessAt(x, y) == null) {
            this.board[y][x] = color;
            return true;
        }
        return false;
    }

    private void isWin(MQProtocol.Chess chess) {
        try {
            sleep(SystemConfiguration.getSleepTime());
        } catch (Exception e) {

        }
        // judge if this chess wins

        communicator.win(chess.getColor());
    }
}
