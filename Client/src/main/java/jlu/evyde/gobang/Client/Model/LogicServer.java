package jlu.evyde.gobang.Client.Model;

import jlu.evyde.gobang.Client.Controller.Communicator;
import jlu.evyde.gobang.Client.Controller.CommunicatorFactory;
import jlu.evyde.gobang.Client.Controller.GobangException;
import jlu.evyde.gobang.Client.Controller.LogicCommunicatorReceiveListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Thread.sleep;

public class LogicServer {
    private final Communicator communicator = CommunicatorFactory.getWebSocketCommunicator();
    private final Logger logger = LoggerFactory.getLogger(LogicServer.class);
    private final Map<MQProtocol.Chess.Color, UUID> gamer = new ConcurrentHashMap<>();
    private final Stack<MQProtocol.Chess> steps = new Stack<>();
    private final EnumMap<MQProtocol.Chess.Color, Integer> score = new EnumMap<>(MQProtocol.Chess.Color.class);
    private final MQProtocol.Chess.Color[][] board = new MQProtocol.Chess.Color
            [SystemConfiguration.getBoardHeight()][SystemConfiguration.getBoardWidth()];
    private Integer chessNums = 0;
    private MQProtocol.Chess.Color nowMove = SystemConfiguration.getFIRST().equals(MQProtocol.Chess.Color.WHITE) ?
            MQProtocol.Chess.Color.WHITE : MQProtocol.Chess.Color.BLACK;

    public LogicServer() {
        score.put(MQProtocol.Chess.Color.WHITE, 0);
        score.put(MQProtocol.Chess.Color.BLACK, 0);

        // check from file if score is already exists


        try {
            communicator.addReceiveListener(new LogicCommunicatorReceiveListener() {
                @Override
                public void doReceive(MQMessage msg) {
                    if (MQProtocol.Code.AUTH.getCode().equals(msg.code)) {
                        if (MQProtocol.Group.GAMER.equals(msg.group)) {
                            if (msg.chess != null && msg.chess.getColor() != null) {
                                if (!gamer.containsKey(msg.chess.getColor())) {
                                    gamer.put(msg.chess.getColor(), msg.token);
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
                                    steps.push(msg.chess);
                                    nowMove = nowMove.equals(MQProtocol.Chess.Color.WHITE) ?
                                            MQProtocol.Chess.Color.BLACK : MQProtocol.Chess.Color.WHITE;
                                    communicator.put(msg.chess);
                                    win(msg.chess);
                                    draw();
                                }
                            }
                        }
                    } else if (MQProtocol.Code.DISCONNECT.getCode().equals(msg.code)) {
                        for (MQProtocol.Chess.Color color : gamer.keySet()) {
                            if (gamer.get(color).equals(UUID.fromString(msg.msg))) {
                                gamer.remove(color);
                            }
                        }
                    } else if (MQProtocol.Code.RESTART_GAME.getCode().equals(msg.code)) {
                        // should judge if win or draw
                        // for (...)
                        resetVariables();
                        communicator.reset();
                    } else if (MQProtocol.Code.CLEAR_SCORE.getCode().equals(msg.code)) {
                        communicator.clearScore();
                    } else if (MQProtocol.Code.END_GAME.getCode().equals(msg.code)) {
                        communicator.endGame();
                    } else if (MQProtocol.Code.TALK.getCode().equals(msg.code)) {
                        communicator.talk(msg.msg);
                    } else if (MQProtocol.Code.RECALL.getCode().equals(msg.code)) {
                        if (!boardEmpty()) {
                            clearChessAt(steps.pop());
                            nowMove = nowMove.equals(MQProtocol.Chess.Color.WHITE) ?
                                    MQProtocol.Chess.Color.BLACK : MQProtocol.Chess.Color.WHITE;
                            communicator.recall();
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
                    () -> {
                        logger.info("Logic server started.");
                    },
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
            chessNums++;
            return true;
        }
        return false;
    }

    private void clearChessAt(MQProtocol.Chess chess) {
        clearChessAt(chess.getPosition().x, chess.getPosition().y);
    }

    private void clearChessAt(Integer x, Integer y) {
        this.board[y][x] = null;
        chessNums--;
    }

    private boolean isDraw() {
        return chessNums >= SystemConfiguration.getBoardHeight() * SystemConfiguration.getBoardWidth();
    }

    private boolean isWin(MQProtocol.Chess chess) {
        try {
            sleep(SystemConfiguration.getSleepTime());
        } catch (Exception e) {
            e.printStackTrace();
        }
        // judge if this chess wins

        {
            // check if 4 directions (8 sub directions) has 5 combo chess
            // top -> down
            int continuousNum = 0;
            for (int y = chess.getPosition().y; y >= 0; y--) {
                if (chess.getColor().equals(getChessAt(chess.getPosition().x, y))) {
                    continuousNum++;
                } else {
                    break;
                }
            }
            for (int y = chess.getPosition().y; y < SystemConfiguration.getBoardHeight(); y++) {
                if (chess.getColor().equals(getChessAt(chess.getPosition().x, y))) {
                    continuousNum++;
                } else {
                    break;
                }
            }
            continuousNum -= 1;
            if (continuousNum >= SystemConfiguration.getWinContinuousNum()) {
                return true;
            }

            // left -> right
            continuousNum = 0;
            for (int x = chess.getPosition().x; x >= 0; x--) {
                if (chess.getColor().equals(getChessAt(x, chess.getPosition().y))) {
                    continuousNum++;
                } else {
                    break;
                }
            }
            for (int x = chess.getPosition().x; x < SystemConfiguration.getBoardWidth(); x++) {
                if (chess.getColor().equals(getChessAt(x, chess.getPosition().y))) {
                    continuousNum++;
                } else {
                    break;
                }
            }
            continuousNum -= 1;
            if (continuousNum >= SystemConfiguration.getWinContinuousNum()) {
                return true;
            }

            // left top -> right down
            continuousNum = 0;
            for (int x = chess.getPosition().x, y = chess.getPosition().y; x >= 0 && y >= 0; x--, y--) {
                if (chess.getColor().equals(getChessAt(x, y))) {
                    continuousNum++;
                } else {
                    break;
                }
            }
            for (int x = chess.getPosition().x, y = chess.getPosition().y;
                 x < SystemConfiguration.getBoardWidth() && y < SystemConfiguration.getBoardWidth(); x++, y++) {
                if (chess.getColor().equals(getChessAt(x, y))) {
                    continuousNum++;
                } else {
                    break;
                }
            }
            continuousNum -= 1;
            if (continuousNum >= SystemConfiguration.getWinContinuousNum()) {
                return true;
            }

            // right top -> left down
            continuousNum = 0;
            for (int x = chess.getPosition().x, y = chess.getPosition().y;
                 x < SystemConfiguration.getBoardWidth() && y >= 0; x++, y--) {
                if (chess.getColor().equals(getChessAt(x, y))) {
                    continuousNum++;
                } else {
                    break;
                }
            }
            for (int x = chess.getPosition().x, y = chess.getPosition().y;
                 x >= 0 && y < SystemConfiguration.getBoardWidth(); x--, y++) {
                if (chess.getColor().equals(getChessAt(x, y))) {
                    continuousNum++;
                } else {
                    break;
                }
            }
            continuousNum -= 1;
            return continuousNum >= SystemConfiguration.getWinContinuousNum();
        }
    }

    private void win(MQProtocol.Chess chess) {
        // if win
        if (isWin(chess)) {
            // update score to color + 1
            MQProtocol.Chess.Color winColor = chess.getColor();
            score.put(winColor, score.getOrDefault(winColor, 0) + 1);
            communicator.win(chess);
            // communicator.updateScore(score);
        }
    }

    private void draw() {
        if (isDraw()) {
            communicator.draw();
        }
    }

    private void resetVariables() {
        gamer.clear();
        // score.clear();
        for (int y = 0; y < SystemConfiguration.getBoardHeight(); y++) {
            Arrays.fill(board[y], null);
        }
        nowMove = SystemConfiguration.getFIRST().equals(MQProtocol.Chess.Color.WHITE) ?
                MQProtocol.Chess.Color.WHITE : MQProtocol.Chess.Color.BLACK;
        chessNums = 0;
    }

    private boolean boardEmpty() {
        return steps.isEmpty();
    }
}
