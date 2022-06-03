package jlu.evyde.gobang.Client;

import javax.swing.*;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.jthemedetecor.OsThemeDetector;
import jlu.evyde.gobang.Client.Controller.GobangException;
import jlu.evyde.gobang.Client.Controller.UIDriverFactory;
import jlu.evyde.gobang.Client.Model.SystemConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    /**{
     * 创建并显示GUI。出于线程安全的考虑，
     * 这个方法在事件调用线程中调用。
     */
    private static final Logger logger = LoggerFactory.getLogger(SystemConfiguration.class);

    public static void main(String[] args) {
        logger.info("Starting client");
        try {
            UIDriverFactory.getUIDriver().initMainFrame(() -> {
                logger.info("MainFrame started.");
            });
        } catch (GobangException.FrameInitFailedException ffe) {
            logger.error("MainFrame started failed: {}", ffe.toString());
            System.exit(1);
        }

    }
}