package jlu.evyde.gobang.Client;

import javax.swing.*;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.jthemedetecor.OsThemeDetector;

public class Main {
    /**{
     * 创建并显示GUI。出于线程安全的考虑，
     * 这个方法在事件调用线程中调用。
     */
    private static JFrame frame;
    private static OsThemeDetector detector;
    private static void createAndShowGUI() {
        // 确保一个漂亮的外观风格

        // 创建及设置窗口
        frame = new MainTest();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(100, 100);

        // 添加 "Hello World" 标签
        JLabel label = new JLabel("Hello World");
        frame.getContentPane().add(label);

        // 显示窗口
        // frame.pack();
        frame.setSize(100, 100);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        System.out.println("Hello world!");
        detector = OsThemeDetector.getDetector();
        detector.registerListener(isDark -> {
            if (isDark) {
                //The OS switched to a dark theme
                try {
                    System.out.println("Dark");
                    UIManager.setLookAndFeel( new FlatDarculaLaf() );
                } catch( Exception ex ) {
                    System.err.println( "Failed to initialize LaF" );
                }
            } else {
                //The OS switched to a light theme
                try {
                    System.out.println("Light");
                    UIManager.setLookAndFeel( new FlatIntelliJLaf() );
                } catch( Exception ex ) {
                    System.err.println( "Failed to initialize LaF" );
                }
            }
            SwingUtilities.updateComponentTreeUI(frame);
            // frame.pack();
        });
        if (detector.isDark()) {
            //The OS switched to a dark theme
            try {
                System.out.println("Dark");
                UIManager.setLookAndFeel( new FlatDarculaLaf() );
            } catch( Exception ex ) {
                System.err.println( "Failed to initialize LaF" );
            }
        } else {
            //The OS switched to a light theme
            try {
                System.out.println("Light");
                UIManager.setLookAndFeel( new FlatIntelliJLaf() );
            } catch( Exception ex ) {
                System.err.println( "Failed to initialize LaF" );
            }
        }
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}