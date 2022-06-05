package jlu.evyde.gobang.Client.SwingView;
import jlu.evyde.gobang.Client.SwingController.GUIPrintStream;

import javax.swing.*;
import java.awt.*;
import java.io.PrintStream;
import java.util.Locale;
import java.util.ResourceBundle;

public class MainFrame extends JFrame {
    private JTextPane logTextPane;
    private final ResourceBundle bundle;
    public MainFrame() {
        super();

        Locale locale = Locale.getDefault();
        bundle = ResourceBundle.getBundle("MainFrame", locale);

        initComponents();

        PrintStream stdout = new GUIPrintStream(System.out, logTextPane);
        System.setOut(stdout);
        PrintStream stderr = new GUIPrintStream(System.err, logTextPane);
        System.setErr(stderr);
    }

    private void initComponents() {
        this.setTitle(bundle.getString("title"));
        this.setSize(new Dimension(1024, 768));
        this.setPreferredSize(new Dimension(1024, 768));

        JFrame.setDefaultLookAndFeelDecorated(true);
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        GridBagLayout mainGridBagLayout = new GridBagLayout();

        mainGridBagLayout.columnWidths = new int[] { 0 };
        mainGridBagLayout.rowHeights = new int[] { 0 };
        mainGridBagLayout.columnWeights = new double[] { 1.0 };
        mainGridBagLayout.rowWeights = new double[] { 1.0 };

        this.setLayout(mainGridBagLayout);

        GridBagConstraints s = new GridBagConstraints();

        JPanel controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setBorder(BorderFactory.createTitledBorder(bundle.getString("controlPanel")));

        logTextPane = new JTextPane();

        JScrollPane logScrollPane = new JScrollPane(
                logTextPane,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );

        logTextPane.setPreferredSize(new Dimension(50, 50));
        logTextPane.setSize(new Dimension(50, 50));
        logScrollPane.setPreferredSize(new Dimension(50, 50));
        logScrollPane.setSize(new Dimension(50, 50));

        logScrollPane.setBorder(BorderFactory.createTitledBorder(bundle.getString("log")));
        s.gridx = 0;
        s.gridy = 3;
        s.gridheight = 4;
        s.fill = GridBagConstraints.BOTH;
        s.weightx = 1;
        s.weighty = 4;
        controlPanel.add(logScrollPane, s);

        s = new GridBagConstraints();
        s.gridx = 0;
        s.gridy = 0;
        s.gridheight = 1;
        s.gridwidth = 1;
        s.ipadx = -50;
        s.ipady = -50;
        s.weightx = 1;
        s.weighty = 1;
        s.fill = GridBagConstraints.BOTH;
        this.add(controlPanel, s);


        JPanel gamePanel = new JPanel();
        gamePanel.setBorder(BorderFactory.createTitledBorder(bundle.getString("gamePanel")));

        s = new GridBagConstraints();
        s.gridx = 1;
        s.gridy = 0;
        s.gridwidth = 2;
        s.fill = GridBagConstraints.BOTH;
        s.ipadx = -50;
        s.ipady = -50;
        s.weightx = 2;
        s.weighty = 1;
        this.add(gamePanel, s);




        this.pack();
    }

}
