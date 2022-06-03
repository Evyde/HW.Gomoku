package jlu.evyde.gobang.Client.SwingController;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.jthemedetecor.OsThemeDetector;
import jlu.evyde.gobang.Client.Controller.Callback;
import jlu.evyde.gobang.Client.Controller.GobangException;
import jlu.evyde.gobang.Client.Controller.UIDriver;
import jlu.evyde.gobang.Client.SwingView.MainFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;


public class SwingUIDriver implements UIDriver {
    private final OsThemeDetector detector = OsThemeDetector.getDetector();
    private final JFrame mainFrame = new MainFrame();
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

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
     * @param callback Callback function when successfully initialized.
     * @throws GobangException.FrameInitFailedException
     */
    @Override
    public void initMainFrame(Callback callback) throws GobangException.FrameInitFailedException {
        detector.registerListener(isDark -> {
            if (isDark) {
                //The OS switched to a dark theme
                try {
                    logger.warn("Switch theme to dark.");
                    UIManager.setLookAndFeel(new FlatDarculaLaf());
                } catch(Exception ex) {
                    logger.error("Failed to initialize LaF: {}.", ex.toString());
                }
            } else {
                //The OS switched to a light theme
                try {
                    logger.warn("Switch theme to light.");
                    UIManager.setLookAndFeel(new FlatIntelliJLaf());
                } catch(Exception ex) {
                    logger.error("Failed to initialize LaF: {}.", ex.toString());
                }
            }
            SwingUtilities.updateComponentTreeUI(mainFrame);
        });

        if (detector.isDark()) {
            //The OS switched to a dark theme
            try {
                logger.warn("Using theme dark.");
                UIManager.setLookAndFeel(new FlatDarculaLaf());
            } catch(Exception ex) {
                logger.error("Failed to initialize LaF: {}.", ex.toString());
                throw new GobangException.FrameInitFailedException();
            }
        } else {
            //The OS switched to a light theme
            try {
                logger.warn("Using theme light.");
                UIManager.setLookAndFeel(new FlatIntelliJLaf());
            } catch(Exception ex) {
                logger.error("Failed to initialize LaF: {}.", ex.toString());
                throw new GobangException.FrameInitFailedException();
            }
        }
        SwingUtilities.updateComponentTreeUI(mainFrame);

        javax.swing.SwingUtilities.invokeLater(() -> mainFrame.setVisible(true));
        // don't forget to callback
        callback.run();
    }

    @Override
    public void initUICommunicator(Callback callback) throws GobangException.UICommunicatorInitFailedException {

    }
}
