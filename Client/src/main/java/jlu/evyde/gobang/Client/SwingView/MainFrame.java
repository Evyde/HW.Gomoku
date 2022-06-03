package jlu.evyde.gobang.Client.SwingView;
import jlu.evyde.gobang.Client.Controller.Utils;

import javax.swing.*;
import java.awt.GridLayout;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class MainFrame extends JFrame {
    private JLabel userName;
    private final ResourceBundle bundle = ResourceBundle.getBundle("MainFrame", new Locale("zh_CN"));
    public MainFrame() {
        super();
        this.setTitle(Utils.toUTF8String(bundle.getString("title")));
        Locale locale = Locale.getDefault();

        JFrame.setDefaultLookAndFeelDecorated(true);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLayout(new GridLayout(1, 3));
        userName = new JLabel();

        this.add(userName);
        this.add(new JTextField());
        this.add(new JLabel());
        this.add(new JPasswordField());
        this.add(new JButton(Utils.toUTF8String(bundle.getString("login"))));
        this.pack();
    }

}
