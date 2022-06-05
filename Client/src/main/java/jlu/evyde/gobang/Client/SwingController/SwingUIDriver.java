package jlu.evyde.gobang.Client.SwingController;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;
import com.jthemedetecor.OsThemeDetector;
import jlu.evyde.gobang.Client.Controller.Callback;
import jlu.evyde.gobang.Client.Controller.GobangException;
import jlu.evyde.gobang.Client.Controller.UIDriver;
import jlu.evyde.gobang.Client.Model.SystemConfiguration;
import jlu.evyde.gobang.Client.SwingView.MainFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;



public class SwingUIDriver implements UIDriver {
    private OsThemeDetector detector = null;
    private JFrame mainFrame;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

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
        mainFrame = new MainFrame(disposeListener);
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
        mainFrame.setVisible(true);
        // don't forget to callback
        complete.run();
    }

    /**
     * Initialize the communicator for UI, should persistence it properly.
     *
     * @param complete Callback function when successfully initialized.
     * @throws GobangException.UICommunicatorInitFailedException
     */
    @Override
    public void initUICommunicator(Callback complete) throws GobangException.UICommunicatorInitFailedException {

    }


    /**
     * Put chess in the UI.
     *
     * @param x     Relative axis of chess.
     * @param y     Relative axis of chess.
     * @param chess Kind of chess.
     */
    @Override
    public void put(int x, int y, SystemConfiguration.Chess chess) {

    }

    /**
     * Tell UI we win! :)
     */
    @Override
    public void win() {

    }

    /**
     * Tell UI we lose. :(
     */
    @Override
    public void lose() {

    }
}
