package jlu.evyde.gobang.Client.SwingController;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;
import com.jthemedetecor.OsThemeDetector;
import jlu.evyde.gobang.Client.Controller.*;
import jlu.evyde.gobang.Client.Model.MQProtocol;
import jlu.evyde.gobang.Client.Model.MQServerAddress;
import jlu.evyde.gobang.Client.Model.SystemConfiguration;
import jlu.evyde.gobang.Client.SwingView.MainFrame;
import jlu.evyde.gobang.Client.View.GameFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

import static java.lang.Thread.sleep;


public class SwingUIDriver implements UIDriver {
    private OsThemeDetector detector = null;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private Communicator communicator;
    private GameFrame gameFrame;

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
        gameFrame = new MainFrame(disposeListener, this.communicator);
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
        try {
            communicator = CommunicatorFactory.getWebSocketCommunicator();
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
            communicator.register(MQProtocol.MQSource.UI, complete, () -> {
                logger.error("Register to MQ failed.");
                throw new GobangException.UICommunicatorInitFailedException();
            });
        } catch (Exception e) {
            logger.error("Failed to initialize communicator.");
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
     * @param color Color of chess who win.
     */
    @Override
    public void win(MQProtocol.Chess.Color color) {
        gameFrame.win(color);
    }

    /**
     * Recall last step.
     */
    @Override
    public void recall() {
        gameFrame.recall();
    }

}
