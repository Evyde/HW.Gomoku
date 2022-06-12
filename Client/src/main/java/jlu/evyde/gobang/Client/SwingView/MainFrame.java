package jlu.evyde.gobang.Client.SwingView;

import com.formdev.flatlaf.extras.FlatSVGIcon;
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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;

public class MainFrame extends GameFrame {
    private final ResourceBundle bundle;
    private final Callback exit;
    private final PrintStream stdout;
    private final PrintStream stderr;
    private JPanel nowPlayerPanel;
    private final Logger logger = LoggerFactory.getLogger(MainFrame.class);
    private JTextArea outputArea;
    private final Map<MQProtocol.Chess.Color, Communicator> communicatorMap;
    private final Map<MQProtocol.Chess.Color, FlatSVGIcon> chessImage = new ConcurrentHashMap<>();
    private final Map<MQProtocol.Chess.Color, FlatSVGIcon> winChessImage = new ConcurrentHashMap<>();
    private JTextPane logTextPane;
    private JPanel game;
    private final Deque<MQProtocol.Chess> steps = new ConcurrentLinkedDeque<>();
    private final HashSet<Point> relativePositions = new HashSet<>();
    private final HashSet<Point> absolutePositions = new HashSet<>();
    private MQProtocol.Chess winningChess = null;
    private JLabel blackScore;
    private JLabel whiteScore;
    public JPanel controlButtonPanel;

    public MainFrame(Callback disposeListener, Map<MQProtocol.Chess.Color, Communicator> uiCommunicatorMap) {
        super(uiCommunicatorMap);

        this.communicatorMap = uiCommunicatorMap;
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
        this.setTitle(bundle.getString("TITLE"));
        this.setSize(new Dimension(1024, 768));
        this.setPreferredSize(new Dimension(1024, 768));

        JFrame.setDefaultLookAndFeelDecorated(true);
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        GridBagLayout mainGridBagLayout = new GridBagLayout();

        mainGridBagLayout.columnWidths = new int[]{0};
        mainGridBagLayout.rowHeights = new int[]{0};
        mainGridBagLayout.columnWeights = new double[]{1.0};
        mainGridBagLayout.rowWeights = new double[]{1.0};

        this.setLayout(mainGridBagLayout);

        GridBagConstraints s = new GridBagConstraints();

        JPanel controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setBorder(BorderFactory.createTitledBorder(bundle.getString("CONTROL_PANEL")));

        logTextPane = new JTextPane();

        JScrollPane logScrollPane = new JScrollPane(
                logTextPane,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );

        logTextPane.setPreferredSize(new Dimension(30, 30));
        logScrollPane.setPreferredSize(new Dimension(30, 30));

        logScrollPane.setBorder(BorderFactory.createTitledBorder(bundle.getString("LOG")));
        s.gridx = 0;
        s.gridy = 4;
        s.gridheight = 4;
        s.gridwidth = 2;
        s.fill = GridBagConstraints.BOTH;
        s.weightx = 1;
        s.weighty = 1;
        controlPanel.add(logScrollPane, s);

        nowPlayerPanel = new JPanel(new GridBagLayout()) {
            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                int radius = Math.min(this.getWidth(), this.getHeight()) / 3;
                chessImage.get(MQProtocol.Chess.Color.WHITE)
                        .derive(radius, radius)
                        .paintIcon(this, g, 20, 20);

                chessImage.get(MQProtocol.Chess.Color.BLACK)
                        .derive(radius, radius)
                        .paintIcon(this, g, this.getWidth() - radius - 15,
                                this.getHeight() - radius - 15);
                if (nowPlayer.equals(MQProtocol.Chess.Color.WHITE)) {
                    g.drawOval(20, 20, radius + 1,
                            radius + 1);
                } else {
                    g.drawOval(this.getWidth() - radius - 16,
                            this.getHeight() - radius - 16, radius + 1,
                            radius + 1);
                }
                g.drawString(MQProtocol.Chess.Color.WHITE.toString(), 20 + radius + 6,
                        20 + radius / 2 + 3);
                g.drawString(MQProtocol.Chess.Color.BLACK.toString(),
                        this.getWidth() - radius - 16 - 6 - 30,
                        this.getHeight() - radius / 2 - 16 + 6);
            }
        };
        nowPlayerPanel.setBorder(BorderFactory.createTitledBorder(bundle.getString("NOW_PLAYER_PANEL")));
        s = new GridBagConstraints();
        s.gridx = 0;
        s.gridy = 0;
        s.gridheight = 1;
        s.fill = GridBagConstraints.BOTH;
        s.weightx = 1;
        s.weighty = 1;
        controlPanel.add(nowPlayerPanel, s);

        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBorder(BorderFactory.createTitledBorder(bundle.getString("CHAT_PANEL")));
        JPanel subChatPanel = new JPanel(new BorderLayout());
        JTextField inputArea = new JTextField();
        outputArea = new JTextArea();
        outputArea.setDragEnabled(true);
        JScrollPane outputPane = new JScrollPane(outputArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JButton sendButton = new JButton(bundle.getString("SEND"));
        sendButton.addActionListener(e -> {
            if (!inputArea.getText().isEmpty()) {
                communicatorMap.get(nowPlayer).talk(inputArea.getText());
                inputArea.setText("");
            }
        });
        inputArea.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {

            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (!inputArea.getText().isEmpty()) {
                        communicatorMap.get(nowPlayer).talk(inputArea.getText().replaceAll("\n", ""));
                        inputArea.setText("");
                    }
                }
            }
        });
        subChatPanel.add(BorderLayout.CENTER, inputArea);
        subChatPanel.add(BorderLayout.LINE_END, sendButton);
        chatPanel.add(BorderLayout.SOUTH, subChatPanel);
        chatPanel.add(BorderLayout.CENTER, outputPane);
        outputPane.setAutoscrolls(true);
        // subChatPanel.setPreferredSize(new Dimension(2, 2));
        chatPanel.setPreferredSize(new Dimension(2, 2));
        // inputArea.setPreferredSize(new Dimension(2, 2));
        // outputArea.setPreferredSize(new Dimension(2, 2));
        outputArea.setEditable(false);
        // sendButton.setPreferredSize(new Dimension(2, 2));

        s = new GridBagConstraints();
        s.gridx = 1;
        s.gridy = 0;
        s.gridheight = 4;
        s.fill = GridBagConstraints.BOTH;
        s.weightx = 1;
        s.weighty = 1;
        controlPanel.add(chatPanel, s);

        JPanel scorePanel = new JPanel(new GridBagLayout());
        scorePanel.setBorder(BorderFactory.createTitledBorder(bundle.getString("SCORE_PANEL")));
        s = new GridBagConstraints();
        s.gridx = 0;
        s.gridy = 1;
        s.gridheight = 1;
        s.fill = GridBagConstraints.BOTH;
        s.weightx = 1;
        s.weighty = 1;
        controlPanel.add(scorePanel, s);

        whiteScore = new JLabel("0", SwingConstants.CENTER);
        whiteScore.putClientProperty("FlatLaf.styleClass", "h00");
        s = new GridBagConstraints();
        s.gridx = 0;
        s.gridy = 0;
        s.gridheight = 1;
        s.fill = GridBagConstraints.BOTH;
        s.weightx = 1;
        s.weighty = 1;
        scorePanel.add(whiteScore, s);

        blackScore = new JLabel("0", SwingConstants.CENTER);
        blackScore.putClientProperty("FlatLaf.styleClass", "h00");
        s = new GridBagConstraints();
        s.gridx = 3;
        s.gridy = 0;
        s.gridheight = 1;
        s.fill = GridBagConstraints.BOTH;
        s.weightx = 1;
        s.weighty = 1;
        scorePanel.add(blackScore, s);

        JLabel versus = new JLabel(":", SwingConstants.CENTER);
        versus.putClientProperty("FlatLaf.styleClass", "h00");
        s = new GridBagConstraints();
        s.gridx = 2;
        s.gridy = 0;
        s.gridheight = 1;
        s.fill = GridBagConstraints.BOTH;
        s.weightx = 1;
        s.weighty = 1;
        scorePanel.add(versus, s);

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

        JButton recallButton = new JButton(bundle.getString("RECALL"));
        recallButton.addActionListener(e -> communicatorMap.get(SystemConfiguration.getFIRST()).recall());

        JButton resetScoreButton = new JButton(bundle.getString("RESET_SCORE"));
        resetScoreButton.addActionListener(e -> communicatorMap.get(SystemConfiguration.getFIRST()).clearScore());

        JButton resetGameButton = new JButton(bundle.getString("RESTART_GAME"));
        resetGameButton.addActionListener(e -> communicatorMap.get(SystemConfiguration.getFIRST()).restartGame());

        JButton endGameButton = new JButton(bundle.getString("END_GAME"));
        endGameButton.addActionListener(e -> communicatorMap.get(SystemConfiguration.getFIRST()).endGame());

        JButton exitGameButton = new JButton(bundle.getString("EXIT_GAME"));
        exitGameButton.addActionListener(e -> {
            communicatorMap.get(SystemConfiguration.getFIRST()).endGame();
            dispose();
        });

        List<JButton> buttonList = new CopyOnWriteArrayList<>();

        buttonList.add(recallButton);
        buttonList.add(resetScoreButton);
        buttonList.add(resetGameButton);
        buttonList.add(endGameButton);
        buttonList.add(exitGameButton);
        controlButtonPanel = new JPanel(new GridBagLayout()) {
            private Dimension size;
            private final Integer buttons = buttonList.size();
            private final Integer rows = 2;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (size != this.getSize()) {
                    size = this.getSize();
                    int i = 0;
                    for (JButton button: buttonList) {
                        this.remove(button);
                        GridBagConstraints s = new GridBagConstraints();
                        s.gridx = (int) (i % Math.ceil((double) buttons / (double) rows));
                        s.gridy = (int) (i / Math.ceil((double) buttons / (double) rows));
                        s.gridheight = 1;
                        s.gridwidth = 1;
                        s.insets = getCellInsets();
                        s.weightx = 1;
                        s.weighty = 1;
                        s.fill = GridBagConstraints.BOTH;
                        //setPreferredSize(new Dimension(2, 2));
                        //button.setPreferredSize(new Dimension(2, 2));
                        button.setEnabled(true);
                        this.add(button, s);
                        i++;
                    }
                }
            }

            private Dimension getCellSize() {
                return new Dimension(this.getWidth() / (buttons - (buttons % rows)) / rows, this.getHeight() / rows);
            }

            private Insets getCellInsets() {
                Dimension cellSize = getCellSize();
                return new Insets(cellSize.height / 6, cellSize.width / 8, cellSize.height / 6, cellSize.width / 8);
            }
        };
        int i = 0, buttons = buttonList.size(), rows = 2;
        for (JButton b: buttonList) {
            s = new GridBagConstraints();
            s.gridx = (int) (i % Math.ceil((double) buttons / (double) rows));
            s.gridy = (int) (i / Math.ceil((double) buttons / (double) rows));
            s.gridheight = 1;
            s.gridwidth = 1;
            s.weightx = 1;
            s.weighty = 1;
            s.fill = GridBagConstraints.BOTH;
            // setPreferredSize(new Dimension(2, 2));
            // b.setPreferredSize(new Dimension(2, 2));
            b.setEnabled(true);
            controlButtonPanel.add(b, s);
        }

        controlButtonPanel.setBorder(BorderFactory.createTitledBorder(bundle.getString("CONTROL_BUTTON_PANEL")));
        s = new GridBagConstraints();
        s.gridx = 0;
        s.gridy = 2;
        s.gridheight = 2;
        s.fill = GridBagConstraints.BOTH;
        s.weightx = 1;
        s.weighty = 1;
        controlPanel.add(controlButtonPanel, s);

        JPanel gamePanel = new JPanel(new GridBagLayout());
        gamePanel.setBorder(BorderFactory.createTitledBorder(bundle.getString("GAME_PANEL")));
        game = new JPanel() {
            private BufferedImage backgroundImage;

            {
                try {
                    backgroundImage = ImageIO.read(SystemConfiguration.getBackgroundImage());
                    chessImage.put(MQProtocol.Chess.Color.WHITE, new FlatSVGIcon(SystemConfiguration.getWhiteChess()));
                    chessImage.put(MQProtocol.Chess.Color.BLACK, new FlatSVGIcon(SystemConfiguration.getBlackChess()));
                    winChessImage.put(MQProtocol.Chess.Color.WHITE, new FlatSVGIcon(SystemConfiguration.getWhiteWinnerChess()));
                    winChessImage.put(MQProtocol.Chess.Color.BLACK, new FlatSVGIcon(SystemConfiguration.getBlackWinnerChess()));
                } catch (IOException ioe) {
                    logger.error("Open image failed.");
                }
            }

            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                nowPlayerPanel.repaint();
                g.drawImage(backgroundImage,
                        (getWidth() - backgroundImage.getWidth()) / 2,
                        (getHeight() - backgroundImage.getHeight()) / 2,
                        this
                );
                Point origin = getVirtualOrigin();

                g.setColor(new Color(233, 233, 233, 170));
                g.fillRect((int) (origin.x - getCellWidth() / 2), (int) (origin.y - getCellHeight() / 2),
                        (int) (getCellWidth() * SystemConfiguration.getBoardWidth()),
                        (int) (getCellHeight() * SystemConfiguration.getBoardHeight()));
                g.setColor(Color.BLACK);

                for (int i = 0; i < SystemConfiguration.getBoardHeight(); i++) {
                    g.drawLine(origin.x, (int) (origin.y + i * getCellHeight()),
                            (int) (origin.x + (SystemConfiguration.getBoardWidth() - 1) * getCellWidth()),
                            (int) (origin.y + i * getCellHeight()));
                }

                for (int i = 0; i < SystemConfiguration.getBoardWidth(); i++) {
                    g.drawLine((int) (origin.x + i * getCellWidth()), origin.y,
                            (int) (origin.x + i * getCellWidth()),
                            (int) (origin.y + (SystemConfiguration.getBoardHeight() - 1) * getCellHeight()));
                }
                g.fillOval((int) (origin.x + (SystemConfiguration.getBoardWidth() / 2 - 2) * getCellWidth() - 4),
                        (int) (origin.y + (SystemConfiguration.getBoardWidth() / 2 + 2) * getCellHeight() - 4)
                        , 8, 8);
                g.fillOval((int) (origin.x + (SystemConfiguration.getBoardWidth() / 2 + 2) * getCellWidth() - 4),
                        (int) (origin.y + (SystemConfiguration.getBoardWidth() / 2 - 2) * getCellHeight() - 4)
                        , 8, 8);
                g.fillOval((int) (origin.x + (SystemConfiguration.getBoardWidth() / 2) * getCellWidth() - 5),
                        (int) (origin.y + (SystemConfiguration.getBoardWidth() / 2) * getCellHeight() - 5)
                        , 10, 10);
                g.fillOval((int) (origin.x + (SystemConfiguration.getBoardWidth() / 2 - 2) * getCellWidth() - 4),
                        (int) (origin.y + (SystemConfiguration.getBoardWidth() / 2 - 2) * getCellHeight() - 4)
                        , 8, 8);
                g.fillOval((int) (origin.x + (SystemConfiguration.getBoardWidth() / 2 + 2) * getCellWidth() - 4),
                        (int) (origin.y + (SystemConfiguration.getBoardWidth() / 2 + 2) * getCellHeight() - 4)
                        , 8, 8);

                for (MQProtocol.Chess chess : steps) {
                    if (chess != null && chess.getPosition() != null) {
                        Point p = toAbsolutePosition(chess.getPosition());
                        p.setLocation(p.x - getCellWidth() / 2, p.y - getCellHeight() / 2);
                        chessImage.get(chess.getColor()).derive((int) Math.floor(getCellWidth()),
                                (int) Math.floor(getCellHeight())).paintIcon(this, g,
                                p.x,
                                p.y);
                    }
                }

                if (winningChess != null) {

                    // check if 4 directions (8 sub directions) has 5 combo chess
                    // top -> down
                    int continuousNum = 0;
                    MQProtocol.Chess chess = winningChess;
                    for (int y = chess.getPosition().y; y >= 0; y--) {
                        if (chess.getColor().equals(getChessAt(chess.getPosition().x, y))) {
                            continuousNum++;
                        } else {
                            break;
                        }
                    }
                    for (int y = chess.getPosition().y; y < SystemConfiguration.getBoardHeight(); y++) {
                        if (chess.getColor().equals(getChessAt(chess.getPosition().x, y))) {
                            continuousNum++;
                        } else {
                            break;
                        }
                    }
                    continuousNum -= 1;
                    if (continuousNum >= SystemConfiguration.getWinContinuousNum()) {
                        for (int y = chess.getPosition().y; y >= 0; y--) {
                            if (chess.getColor().equals(getChessAt(chess.getPosition().x, y))) {
                                Point p = toAbsolutePosition(new Point(chess.getPosition().x, y));
                                p.setLocation(p.x - getCellWidth() / 2, p.y - getCellHeight() / 2);
                                winChessImage.get(chess.getColor()).derive((int) Math.floor(getCellWidth()),
                                        (int) Math.floor(getCellHeight())).paintIcon(this, g,
                                        p.x,
                                        p.y);
                            } else {
                                break;
                            }
                        }
                        for (int y = chess.getPosition().y; y < SystemConfiguration.getBoardHeight(); y++) {
                            if (chess.getColor().equals(getChessAt(chess.getPosition().x, y))) {
                                Point p = toAbsolutePosition(new Point(chess.getPosition().x, y));
                                p.setLocation(p.x - getCellWidth() / 2, p.y - getCellHeight() / 2);
                                winChessImage.get(chess.getColor()).derive((int) Math.floor(getCellWidth()),
                                        (int) Math.floor(getCellHeight())).paintIcon(this, g,
                                        p.x,
                                        p.y);
                            } else {
                                break;
                            }
                        }
                        return;
                    }

                    // left -> right
                    continuousNum = 0;
                    for (int x = chess.getPosition().x; x >= 0; x--) {
                        if (chess.getColor().equals(getChessAt(x, chess.getPosition().y))) {
                            continuousNum++;
                        } else {
                            break;
                        }
                    }
                    for (int x = chess.getPosition().x; x < SystemConfiguration.getBoardWidth(); x++) {
                        if (chess.getColor().equals(getChessAt(x, chess.getPosition().y))) {
                            continuousNum++;
                        } else {
                            break;
                        }
                    }
                    continuousNum -= 1;
                    if (continuousNum >= SystemConfiguration.getWinContinuousNum()) {
                        for (int x = chess.getPosition().x; x >= 0; x--) {
                            if (chess.getColor().equals(getChessAt(x, chess.getPosition().y))) {
                                Point p = toAbsolutePosition(new Point(x, chess.getPosition().y));
                                p.setLocation(p.x - getCellWidth() / 2, p.y - getCellHeight() / 2);
                                winChessImage.get(chess.getColor()).derive((int) Math.floor(getCellWidth()),
                                        (int) Math.floor(getCellHeight())).paintIcon(this, g,
                                        p.x,
                                        p.y);
                            } else {
                                break;
                            }
                        }
                        for (int x = chess.getPosition().x; x < SystemConfiguration.getBoardWidth(); x++) {
                            if (chess.getColor().equals(getChessAt(x, chess.getPosition().y))) {
                                Point p = toAbsolutePosition(new Point(x, chess.getPosition().y));
                                p.setLocation(p.x - getCellWidth() / 2, p.y - getCellHeight() / 2);
                                winChessImage.get(chess.getColor()).derive((int) Math.floor(getCellWidth()),
                                        (int) Math.floor(getCellHeight())).paintIcon(this, g,
                                        p.x,
                                        p.y);
                            } else {
                                break;
                            }
                        }
                        return;
                    }

                    // left top -> right down
                    continuousNum = 0;
                    for (int x = chess.getPosition().x, y = chess.getPosition().y; x >= 0 && y >= 0; x--, y--) {
                        if (chess.getColor().equals(getChessAt(x, y))) {
                            continuousNum++;
                        } else {
                            break;
                        }
                    }
                    for (int x = chess.getPosition().x, y = chess.getPosition().y;
                         x < SystemConfiguration.getBoardWidth() && y < SystemConfiguration.getBoardWidth(); x++, y++) {
                        if (chess.getColor().equals(getChessAt(x, y))) {
                            continuousNum++;
                        } else {
                            break;
                        }
                    }
                    continuousNum -= 1;
                    if (continuousNum >= SystemConfiguration.getWinContinuousNum()) {
                        for (int x = chess.getPosition().x, y = chess.getPosition().y; x >= 0 && y >= 0; x--, y--) {
                            if (chess.getColor().equals(getChessAt(x, y))) {
                                Point p = toAbsolutePosition(new Point(x, y));
                                p.setLocation(p.x - getCellWidth() / 2, p.y - getCellHeight() / 2);
                                winChessImage.get(chess.getColor()).derive((int) Math.floor(getCellWidth()),
                                        (int) Math.floor(getCellHeight())).paintIcon(this, g,
                                        p.x,
                                        p.y);
                            } else {
                                break;
                            }
                        }
                        for (int x = chess.getPosition().x, y = chess.getPosition().y;
                             x < SystemConfiguration.getBoardWidth() && y < SystemConfiguration.getBoardWidth(); x++, y++) {
                            if (chess.getColor().equals(getChessAt(x, y))) {
                                Point p = toAbsolutePosition(new Point(x, y));
                                p.setLocation(p.x - getCellWidth() / 2, p.y - getCellHeight() / 2);
                                winChessImage.get(chess.getColor()).derive((int) Math.floor(getCellWidth()),
                                        (int) Math.floor(getCellHeight())).paintIcon(this, g,
                                        p.x,
                                        p.y);
                            } else {
                                break;
                            }
                        }
                        return;
                    }

                    // right top -> left down
                    continuousNum = 0;
                    for (int x = chess.getPosition().x, y = chess.getPosition().y;
                         x < SystemConfiguration.getBoardWidth() && y >= 0; x++, y--) {
                        if (chess.getColor().equals(getChessAt(x, y))) {
                            continuousNum++;
                        } else {
                            break;
                        }
                    }
                    for (int x = chess.getPosition().x, y = chess.getPosition().y;
                         x >= 0 && y < SystemConfiguration.getBoardWidth(); x--, y++) {
                        if (chess.getColor().equals(getChessAt(x, y))) {
                            continuousNum++;
                        } else {
                            break;
                        }
                    }
                    continuousNum -= 1;
                    if (continuousNum >= SystemConfiguration.getWinContinuousNum()) {
                        for (int x = chess.getPosition().x, y = chess.getPosition().y;
                             x < SystemConfiguration.getBoardWidth() && y >= 0; x++, y--) {
                            if (chess.getColor().equals(getChessAt(x, y))) {
                                Point p = toAbsolutePosition(new Point(x, y));
                                p.setLocation(p.x - getCellWidth() / 2, p.y - getCellHeight() / 2);
                                winChessImage.get(chess.getColor()).derive((int) Math.floor(getCellWidth()),
                                        (int) Math.floor(getCellHeight())).paintIcon(this, g,
                                        p.x,
                                        p.y);
                            } else {
                                break;
                            }
                        }
                        for (int x = chess.getPosition().x, y = chess.getPosition().y;
                             x >= 0 && y < SystemConfiguration.getBoardWidth(); x--, y++) {
                            if (chess.getColor().equals(getChessAt(x, y))) {
                                Point p = toAbsolutePosition(new Point(x, y));
                                p.setLocation(p.x - getCellWidth() / 2, p.y - getCellHeight() / 2);
                                winChessImage.get(chess.getColor()).derive((int) Math.floor(getCellWidth()),
                                        (int) Math.floor(getCellHeight())).paintIcon(this, g,
                                        p.x,
                                        p.y);
                            } else {
                                break;
                            }
                        }
                        return;
                    }
                }
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
        s.weightx = 1;
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
            private Point lastMousePositionCell;

            @Override
            public void mouseMoved(MouseEvent e) {
                Point relativePosition = toRelativePosition(e.getPoint());
                if (relativePosition == null || relativePositions.contains(relativePosition)
                        || relativePosition.x >= SystemConfiguration.getBoardWidth() || relativePosition.y >= SystemConfiguration.getBoardHeight()) {
                    game.repaint();
                    return;
                }
                Point absolutePosition = toAbsolutePosition(toRelativePosition(e.getPoint()));
                if (!relativePosition.equals(lastMousePositionCell)) {
                    game.repaint();
                    lastMousePositionCell = relativePosition;
                }
                chessImage.get(nowPlayer).derive((int) Math.floor(getCellWidth() / 5),
                        (int) Math.floor(getCellHeight() / 5)).paintIcon(game, game.getGraphics(),
                        absolutePosition.x - (int) Math.floor(getCellWidth() / 5 / 2),
                        absolutePosition.y - (int) Math.floor(getCellWidth() / 5 / 2));
            }
        });
        // Blow's stupid things from stackoverflow to solve grid bag layout width equals
        // See https://stackoverflow.com/questions/41923709/cant-get-equal-width-columns-with-gridbaglayout-layout-manager
        logScrollPane.setPreferredSize(new Dimension(2, 2));
        logTextPane.setPreferredSize(new Dimension(2, 2));
        controlButtonPanel.setPreferredSize(new Dimension(2, 2));
        controlPanel.setPreferredSize(new Dimension(2, 2));
        game.setPreferredSize(new Dimension(2, 2));
        gamePanel.setPreferredSize(new Dimension(2, 2));
        scorePanel.setPreferredSize(new Dimension(2, 2));
        nowPlayerPanel.setPreferredSize(new Dimension(2, 2));
        this.setPreferredSize(new Dimension(2, 2));
        controlButtonPanel.repaint();
        this.pack();

    }

    private MQProtocol.Chess.Color getChessAt(int x, int y) {
        for (MQProtocol.Chess c : steps) {
            if (c.getPosition().equals(new Point(x, y))) {
                return c.getColor();
            }
        }
        return null;
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
            changePlayer();
            logger.warn("Put " + c);
            game.repaint();
        }
    }

    @Override
    public void recall() {
        if (!steps.isEmpty()) {
            logger.warn("Recall");
            Point tempPosition = steps.pop().getPosition();
            this.relativePositions.remove(tempPosition);
            this.absolutePositions.remove(toAbsolutePosition(tempPosition));
            changePlayer();
            game.repaint();
        }
    }

    @Override
    public void win(MQProtocol.Chess c) {
        logger.warn("{} wins!", c.getColor().toString());
        winningChess = c;
        game.repaint();
        setNowPlayLock(true);
        showWinDialog(c.getColor());
    }

    @Override
    public void updateScore(Map<MQProtocol.Chess.Color, Integer> score) {
        this.whiteScore.setText(score.get(MQProtocol.Chess.Color.WHITE).toString());
        this.blackScore.setText(score.get(MQProtocol.Chess.Color.BLACK).toString());
        blackScore.repaint();
        whiteScore.repaint();
    }

    @Override
    public void draw() {
        showDrawDialog();
        setNowPlayLock(true);
    }

    @Override
    public Point toRelativePosition(Point point) {
        Point origin = getVirtualOrigin();
        origin.setLocation(origin.x - getCellWidth() / 2, origin.y - getCellHeight() / 2);
        if (point.x < origin.x || point.y < origin.y) {
            return null;
        }
        if (point.x > origin.x + getCellWidth() * SystemConfiguration.getBoardWidth()
                || point.y > origin.y + getCellHeight() * SystemConfiguration.getBoardHeight()) {
            return null;
        }

        return new Point((int) Math.floor(point.x / getCellWidth() - 1),
                (int) Math.floor(point.y / getCellHeight() - 1));
    }

    @Override
    public Point toAbsolutePosition(Point point) {
        Point origin = getVirtualOrigin();
        return new Point((int) (origin.x + point.x * getCellWidth()), (int) (origin.y + point.y * getCellHeight()));
    }

    @Override
    public void reset() {
        steps.clear();
        relativePositions.clear();
        absolutePositions.clear();
        winningChess = null;
        nowPlayer = SystemConfiguration.getFIRST();
        setNowPlayLock(false);
        //communicatorMap.get(SystemConfiguration.getFIRST()).setSendOnly(true);
        //communicatorMap.get(SystemConfiguration.getFIRST()).setReadOnly(false);
        //communicatorMap.get(SystemConfiguration.getNextColor()).setReadOnly(false);
    }

    private Point getVirtualOrigin() {
        // Get how many heights can have
        Double heights = getActualBoardHeight() / getCellHeight(SystemConfiguration.getBoardScale());
        Double widths = getActualBoardWidth() / getCellWidth(SystemConfiguration.getBoardScale());
        Double x, y;
        if (heights >= SystemConfiguration.getBoardHeight()) {
            y =
                    (heights - SystemConfiguration.getBoardHeight() + 1) / 2 * getCellHeight(SystemConfiguration.getBoardScale());
        } else {
            y = 0d;
        }
        if (widths >= SystemConfiguration.getBoardWidth()) {
            x =
                    (widths - SystemConfiguration.getBoardWidth() + 1) / 2 * getCellWidth(SystemConfiguration.getBoardScale());
        } else {
            x = 0d;
        }
        return new Point(x.intValue(), y.intValue());
    }

    private Double getActualBoardHeight() {
        return game.getSize().getHeight();
    }

    private Double getActualBoardWidth() {
        return game.getSize().getWidth();
    }

    private Double getCellHeight(Double scale) {
        return getActualBoardHeight() * scale / (SystemConfiguration.getBoardHeight() - 1);
    }

    private Double getCellWidth(Double scale) {
        return getActualBoardWidth() * scale / (SystemConfiguration.getBoardWidth() - 1);
    }

    private Double getCellHeight() {
        return getCellHeight(SystemConfiguration.getBoardScale());
    }

    private Double getCellWidth() {
        return getCellWidth(SystemConfiguration.getBoardScale());
    }

    private void showWinDialog(MQProtocol.Chess.Color color) {
        MessageFormat formatter = new MessageFormat(bundle.getString("WINS"), SystemConfiguration.LOCALE);

        showDialog(formatter.format(new Object[]{color}));
    }

    private void showDrawDialog() {
        showDialog(bundle.getString("DRAW"));
    }

    private void showDialog(String message) {
        Window window = SwingUtilities.windowForComponent(this);

        // 0 means YES, 1 means NO, 2 means CANCEL
        Integer choice = JOptionPane.showOptionDialog(
                window,
                message,
                bundle.getString("ALERT"),
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                null,
                JOptionPane.YES_OPTION);

        if (choice.equals(JOptionPane.CANCEL_OPTION)) {
            // do nothing and do not exit game
            communicatorMap.get(SystemConfiguration.getFIRST()).setReadOnly(true);
            communicatorMap.get(SystemConfiguration.getNextColor()).setReadOnly(true);
        } else if (choice.equals(JOptionPane.NO_OPTION)) {
            // exit game
            dispose();
        } else {
            // start next game
            setNowPlayLock(false);
            communicatorMap.get(SystemConfiguration.getFIRST()).restartGame();
            // communicatorMap.get(SystemConfiguration.getFIRST()).setSendOnly(true);

        }
    }

    public void talk(String message) {
        if (!message.isEmpty()) {
            this.outputArea.append(message + "\n");
        }
    }
}
