package jlu.evyde.gobang.Client.SwingView;
import jlu.evyde.gobang.Client.Controller.Callback;
import jlu.evyde.gobang.Client.Controller.Communicator;
import jlu.evyde.gobang.Client.Model.MQProtocol;
import jlu.evyde.gobang.Client.Model.SystemConfiguration;
import jlu.evyde.gobang.Client.SwingController.GUIPrintStream;
import jlu.evyde.gobang.Client.View.GameFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

import static java.lang.Thread.sleep;

public class MainFrame extends GameFrame {
    private JTextPane logTextPane;
    private final ResourceBundle bundle;
    private final Callback exit;
    private final PrintStream stdout;
    private final PrintStream stderr;
    private JPanel game;
    private Stack<MQProtocol.Chess> steps = new Stack<>();
    private HashSet<Point> relativePositions = new HashSet<>();
    private HashSet<Point> absolutePositions = new HashSet<>();
    private final Logger logger = LoggerFactory.getLogger(MainFrame.class);
    private Communicator communicator;
    public MainFrame(Callback disposeListener, Communicator uiCommunicator) {
        super(uiCommunicator);

        this.communicator = uiCommunicator;
        // save source print stream
        stdout = System.out;
        stderr = System.err;

        exit = disposeListener;

        bundle = ResourceBundle.getBundle("MainFrame", SystemConfiguration.LOCALE);

        initComponents();

        PrintStream out = new GUIPrintStream(System.out, logTextPane);
        System.setOut(out);
        PrintStream err = new GUIPrintStream(System.err, logTextPane);
        System.setErr(err);
    }

    private void initComponents() {
        this.setTitle(bundle.getString("title"));
        this.setSize(new Dimension(1024, 768));
        this.setPreferredSize(new Dimension(1024, 768));

        JFrame.setDefaultLookAndFeelDecorated(true);
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
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

        logTextPane.setPreferredSize(new Dimension(30, 30));
        logScrollPane.setPreferredSize(new Dimension(30, 30));

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


        JPanel gamePanel = new JPanel(new GridBagLayout());
        gamePanel.setBorder(BorderFactory.createTitledBorder(bundle.getString("gamePanel")));
        game = new JPanel() {
            private BufferedImage backgroundImage;
            {
                try {
                    backgroundImage = ImageIO.read(new File("Client/src/main/resources/background.png"));
                } catch (IOException ioe) {
                    logger.error("Open image failed.");
                }
            }
            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(backgroundImage,
                        (getWidth() - backgroundImage.getWidth()) / 2,
                        (getHeight() - backgroundImage.getHeight()) / 2,
                        this
                );
            }
        };

        s = new GridBagConstraints();
        s.gridx = 0;
        s.gridy = 0;
        s.fill = GridBagConstraints.BOTH;
        s.weightx = 1;
        s.weighty = 1;
        s.ipadx = -10;
        s.ipady = -10;
        gamePanel.add(game, s);

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
        game.addMouseListener(new PutChessListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!absolutePositions.contains(e.getPoint())) {
                    super.mouseClicked(e);
                }
            }
        });
        game.addMouseMotionListener(new HoverChessListener() {
            @Override
            public void mouseMoved(MouseEvent e) {

            }
        });
        this.pack();
    }

    public void dispose(Callback e) {
        System.setOut(stdout);
        System.setErr(stderr);

        super.dispose();

        e.run();
    }

    @Override
    public void dispose() {
        System.setOut(stdout);
        System.setErr(stderr);

        super.dispose();
        exit.run();
    }

    @Override
    public void put(MQProtocol.Chess c) {
        if (!relativePositions.contains(c.getPosition())) {
            this.relativePositions.add(c.getPosition());
            this.absolutePositions.add(toAbsolutePosition(c.getPosition()));
            this.steps.push(c);
            logger.warn("Put " + c);
            game.repaint();
        }
    }

    @Override
    public void recall() {
        if (!steps.isEmpty()) {
            logger.warn("Recall");
            this.relativePositions.remove(steps.pop().getPosition());
            this.absolutePositions.remove(toAbsolutePosition(steps.pop().getPosition()));
            game.repaint();
        }
    }

    @Override
    public void win(MQProtocol.Chess.Color c) {
        logger.warn("{} wins!", c.toString());
    }

    @Override
    public Point toRelativePosition(Point point) {
        return point;
    }

    @Override
    public Point toAbsolutePosition(Point point) {
        return null;
    }
}
