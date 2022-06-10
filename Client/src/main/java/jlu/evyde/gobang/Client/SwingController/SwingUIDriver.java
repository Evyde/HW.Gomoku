package jlu.evyde.gobang.Client.SwingController;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;
import com.jthemedetecor.OsThemeDetector;
import jlu.evyde.gobang.Client.Controller.*;
import jlu.evyde.gobang.Client.Model.MQMessage;
import jlu.evyde.gobang.Client.Model.MQProtocol;
import jlu.evyde.gobang.Client.Model.MQServerAddress;
import jlu.evyde.gobang.Client.Model.SystemConfiguration;
import jlu.evyde.gobang.Client.SwingView.MainFrame;
import jlu.evyde.gobang.Client.View.GameFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Thread.sleep;


public class SwingUIDriver implements UIDriver {
    private OsThemeDetector detector = null;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private Map<MQProtocol.Chess.Color, Communicator> communicatorMap = new ConcurrentHashMap<>();
    private GameFrame gameFrame;
    private MQProtocol.Chess.Color gamerColor = SystemConfiguration.getFIRST();

    public SwingUIDriver() {
        try {
            detector = OsThemeDetector.getDetector();
        } catch (Exception e) {
            logger.error("Disable auto dark mode.");
            detector = (OsThemeDetector) (Object) new FakeDetector();
        }
    }

    /**
     * Returns if system is dark.
     *
     * @return true if system is dark.
     */
    @Override
    public boolean dark() {
        return detector.isDark();
    }

    /**
     * Initialize the main frame.
     *
     * @param complete Callback function when successfully initialized.
     * @throws GobangException.FrameInitFailedException
     */
    @Override
    public void initMainFrame(Callback complete, Callback disposeListener) throws GobangException.FrameInitFailedException {
        gameFrame = new MainFrame(disposeListener, this.communicatorMap);
        detector.registerListener(isDark -> {
            SwingUtilities.invokeLater(() -> {
                if (isDark) {
                    //The OS switched to a dark theme
                    try {
                        logger.info("Switch theme to dark.");
                        UIManager.setLookAndFeel(new FlatDarculaLaf());
                    } catch (Exception ex) {
                        logger.error("Failed to initialize LaF: {}.", ex.toString());
                    }
                } else {
                    //The OS switched to a light theme
                    try {
                        logger.info("Switch theme to light.");
                        UIManager.setLookAndFeel(new FlatIntelliJLaf());
                    } catch (Exception ex) {
                        logger.error("Failed to initialize LaF: {}.", ex.toString());
                    }
                }
                FlatLaf.updateUI();

                // SwingUtilities.updateComponentTreeUI(mainFrame);
            });
        });

        SwingUtilities.invokeLater(() -> {
            if (detector.isDark()) {
                //The OS switched to a dark theme
                try {
                    logger.info("Using theme dark.");
                    UIManager.setLookAndFeel(new FlatDarculaLaf());
                } catch (Exception ex) {
                    logger.error("Failed to initialize LaF: {}.", ex.toString());
                    throw new GobangException.FrameInitFailedException();
                }
            } else {
                //The OS switched to a light theme
                try {
                    logger.info("Using theme light.");
                    UIManager.setLookAndFeel(new FlatIntelliJLaf());
                } catch (Exception ex) {
                    logger.error("Failed to initialize LaF: {}.", ex.toString());
                    throw new GobangException.FrameInitFailedException();
                }
            }
            FlatLaf.updateUI();
        });

        // SwingUtilities.updateComponentTreeUI(mainFrame);
        gameFrame.setVisible(true);
        // don't forget to callback
        complete.run();
    }

    /**
     * Initialize the communicator for UI, should persistence it properly.
     *
     * @param complete Callback function when successfully initialized.
     * @throws GobangException.CommunicatorInitFailedException
     */
    @Override
    public void initCommunicator(Callback complete) throws GobangException.CommunicatorInitFailedException {
        this.communicatorMap.put(MQProtocol.Chess.Color.WHITE, initColoredCommunicator(MQProtocol.Chess.Color.WHITE,
                complete));
        this.communicatorMap.put(MQProtocol.Chess.Color.BLACK, initColoredCommunicator(MQProtocol.Chess.Color.BLACK,
                complete));

    }

    private Communicator initColoredCommunicator(MQProtocol.Chess.Color color, Callback complete) {
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
            registerMessage.chess = new MQProtocol.Chess(null, color);
            communicator.register(registerMessage, complete , () -> {
                logger.error("Register {} to MQ failed.", color);
                throw new GobangException.UICommunicatorInitFailedException();
            });
            return communicator;
        } catch (Exception e) {
            logger.error("Failed to initialize {} communicator.", color);
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
        gameFrame.put(chess);
    }

    /**
     * Tell UI which color of chess wins.
     *
     * @param chess Chess who win.
     */
    @Override
    public void win(MQProtocol.Chess chess) {
        gameFrame.win(chess);
    }

    /**
     * Let UI update score of gamer.
     *
     * @param score Score map that should be updated.
     */
    @Override
    public void updateScore(Map<MQProtocol.Chess.Color, Integer> score) {
        gameFrame.updateScore(score);
    }

    /**
     * Recall last step.
     */
    @Override
    public void recall() {
        gameFrame.recall();
    }

    /**
     * Tell UI draw.
     */
    @Override
    public void draw() {
        gameFrame.draw();
    }

    /**
     * Let UI resets.
     */
    @Override
    public void reset() {
        gameFrame.reset();
        gameFrame.repaint();
    }

    /**
     * Exit UI (Called by END_GAME).
     */
    @Override
    public void exit() {
        gameFrame.dispose();
        System.exit(0);
    }

    /**
     * Let UI display incoming chat message.
     *
     * @param message Incoming chat message.
     */
    @Override
    public void talk(String message) {
        gameFrame.talk(message);
    }

    public Communicator getBlackCommunicator() {
        return communicatorMap.get(MQProtocol.Chess.Color.BLACK);
    }

    public Communicator getWhiteCommunicator() {
        return communicatorMap.get(MQProtocol.Chess.Color.WHITE);
    }

    public Map<MQProtocol.Chess.Color, Communicator> getCommunicatorMap() {
        return communicatorMap;
    }
}
