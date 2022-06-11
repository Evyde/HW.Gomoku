package jlu.evyde.gobang.Client.Controller;

import jlu.evyde.gobang.Client.Model.MQMessage;
import jlu.evyde.gobang.Client.Model.MQProtocol;
import jlu.evyde.gobang.Client.Model.MQServerAddress;
import jlu.evyde.gobang.Client.Model.SystemConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.Map;
import java.util.Stack;

import static java.lang.Thread.sleep;

public class AIClient implements UIDriver {
    private Communicator aiCommunicator;
    private Callback disposeListener;
    private static final Logger logger = LoggerFactory.getLogger(AIClient.class);
    private MQProtocol.Chess.Color[][] board = new MQProtocol.Chess.Color
            [SystemConfiguration.getBoardHeight()][SystemConfiguration.getBoardWidth()];
    private Stack<MQProtocol.Chess> steps = new Stack<>();
    private final MQProtocol.Chess.Color myColor;
    private boolean recallLock = false;

    public AIClient() {
        myColor = SystemConfiguration.getNextColor();
    }

    public AIClient(MQProtocol.Chess.Color color) {
        myColor = color;
    }

    /**
     * Returns if system is dark.
     *
     * @return true if system is dark.
     */
    @Override
    public boolean dark() {
        return false;
    }

    /**
     * Initialize the main frame.
     *
     * @param complete        Callback function when successfully initialized.
     * @param disposeListener Callback function when frame closed.
     * @throws GobangException.FrameInitFailedException
     */
    @Override
    public void initMainFrame(Callback complete, Callback disposeListener) throws GobangException.FrameInitFailedException {
        complete.run();
        this.disposeListener = disposeListener;
    }

    /**
     * Initialize the communicator for UI, should persistence it properly.
     *
     * @param complete Callback function when successfully initialized.
     * @throws GobangException.CommunicatorInitFailedException
     */
    @Override
    public void initCommunicator(Callback complete) throws GobangException.CommunicatorInitFailedException {
        try {
            Communicator communicator = CommunicatorFactory.getWebSocketCommunicator();
            communicator.addReceiveListener(new UICommunicatorReceiveListener(this) {
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
                    logger.error("Could not connect.");
                }
                sleep(SystemConfiguration.getSleepTime());
            }
            MQMessage registerMessage = new MQMessage();
            registerMessage.token = MQProtocol.Group.GAMER.getInitializedUUID();
            registerMessage.group = MQProtocol.Group.GAMER;
            registerMessage.chess = new MQProtocol.Chess(null, myColor);
            communicator.register(registerMessage, complete , () -> {
                logger.error("Register {} to MQ failed.", myColor);
                throw new GobangException.UICommunicatorInitFailedException();
            });
            this.aiCommunicator = communicator;
        } catch (Exception e) {
            logger.error("Failed to initialize {} communicator.", myColor);
            e.printStackTrace();
            throw new GobangException.UICommunicatorInitFailedException();
        }
    }

    /**
     * Put chess in the UI.
     *
     * @param chess Chess (with position and kind).
     */
    @Override
    public void put(MQProtocol.Chess chess) {
        steps.push(chess);
        setChessAt(chess);
        if (!myColor.equals(chess.getColor())) {
            this.recallLock = false;
        }
        Point randomPosition = new Point();
        while (getChessAt(randomPosition.x, randomPosition.y) != null) {
            randomPosition.setLocation(new Point(Utils.generateRandomInt(0, 14), Utils.generateRandomInt(0,
                    14)));
        }
        aiCommunicator.put(new MQProtocol.Chess(randomPosition, myColor));
    }

    /**
     * Tell UI which color of chess wins.
     *
     * @param chess Chess who win.
     */
    @Override
    public void win(MQProtocol.Chess chess) {
        logger.warn("{} wins.", chess.getColor().toString());
        aiCommunicator.setReadOnly(true);
    }

    /**
     * Let UI update score of gamer.
     *
     * @param score Score map that should be updated.
     */
    @Override
    public void updateScore(Map<MQProtocol.Chess.Color, Integer> score) {

    }

    /**
     * Recall last step.
     */
    @Override
    public void recall() {
        clearChessAt(steps.pop());
        if (!this.recallLock) {
            this.recallLock = true;
            aiCommunicator.recall();
        }
    }

    /**
     * Tell UI draw.
     */
    @Override
    public void draw() {
        logger.warn("Draw.");
    }

    /**
     * Let UI resets.
     */
    @Override
    public void reset() {
        steps.clear();
        board = new MQProtocol.Chess.Color[SystemConfiguration.getBoardHeight()][SystemConfiguration.getBoardWidth()];
    }

    /**
     * Exit UI (Called by END_GAME).
     */
    @Override
    public void exit() {
        aiCommunicator.close();
        this.disposeListener.run();
    }

    /**
     * Let UI display incoming chat message.
     *
     * @param message Incoming chat message.
     */
    @Override
    public void talk(String message) {

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

    private void clearChessAt(MQProtocol.Chess chess) {
        this.board[chess.getPosition().y][chess.getPosition().x] = null;
    }
}
