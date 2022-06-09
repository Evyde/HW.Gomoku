package jlu.evyde.gobang.Client.SwingView;
import jlu.evyde.gobang.Client.Controller.Callback;
import jlu.evyde.gobang.Client.Controller.Communicator;
import jlu.evyde.gobang.Client.Model.MQProtocol;
import jlu.evyde.gobang.Client.Model.SystemConfiguration;
import jlu.evyde.gobang.Client.SwingController.GUIPrintStream;
import jlu.evyde.gobang.Client.View.GameFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.formdev.flatlaf.extras.FlatSVGIcon;


import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import static java.lang.Thread.sleep;

public class MainFrame extends GameFrame {
    private JTextPane logTextPane;
    private final ResourceBundle bundle;
    private final Callback exit;
    private final PrintStream stdout;
    private final PrintStream stderr;
    private Integer choice;
    private JPanel game;
    private Deque<MQProtocol.Chess> steps = new ConcurrentLinkedDeque<>();
    private HashSet<Point> relativePositions = new HashSet<>();
    private HashSet<Point> absolutePositions = new HashSet<>();
    private final Logger logger = LoggerFactory.getLogger(MainFrame.class);
    private final Map<MQProtocol.Chess.Color, Communicator> communicatorMap;
    private final Map<MQProtocol.Chess.Color, FlatSVGIcon> chessImage = new ConcurrentHashMap<>();
    public MainFrame(Callback disposeListener, Map<MQProtocol.Chess.Color, Communicator> uiCommunicatorMap) {
        super(uiCommunicatorMap);

        this.communicatorMap = uiCommunicatorMap;
        // let one of communicator to be sent only because it may cause win twice
        this.communicatorMap.get(MQProtocol.Chess.Color.WHITE).setSendOnly(true);
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
                    chessImage.put(MQProtocol.Chess.Color.WHITE, new FlatSVGIcon(new File("Client/src/main/resources" +
                            "/circle-light.svg")));
                    chessImage.put(MQProtocol.Chess.Color.BLACK, new FlatSVGIcon(new File("Client/src/main/resources" +
                            "/circle-dark.svg")));
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
                Point origin = getVirtualOrigin();

                g.setColor(new Color(233,233,233, 170));
                g.fillRect((int) (origin.x- getCellWidth() / 2), (int) (origin.y - getCellHeight() / 2),
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

                for (MQProtocol.Chess chess: steps) {
                    if (chess != null && chess.getPosition() != null) {
                        Point p = toAbsolutePosition(chess.getPosition());
                        p.setLocation(p.x - getCellWidth() / 2, p.y - getCellHeight() / 2);
                        chessImage.get(chess.getColor()).derive((int) Math.floor(getCellWidth()),
                                (int) Math.floor(getCellHeight())).paintIcon(this, g,
                                p.x,
                                p.y);
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
        showWinDialog(c);
    }

    @Override
    public void updateScore(Map<MQProtocol.Chess.Color, Integer> score) {

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
                (int) Math.floor(point.y /getCellHeight() - 1));
    }

    @Override
    public Point toAbsolutePosition(Point point) {
        Point origin = getVirtualOrigin();
        return new Point((int) (origin.x + point.x * getCellWidth()), (int) (origin.y + point.y * getCellHeight()));
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
        Window window = SwingUtilities.windowForComponent(this);

        MessageFormat formatter = new MessageFormat(bundle.getString("WINS"), SystemConfiguration.LOCALE);

        // 0 means YES, 1 means NO, 2 means CANCEL
        choice = JOptionPane.showOptionDialog(
                window,
                formatter.format(new Object[]{color}),
                "提示",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                null,
                JOptionPane.YES_OPTION);
        if (choice.equals(JOptionPane.CANCEL_OPTION)) {
            // do nothing and do not exit game
        } else if (choice.equals(JOptionPane.NO_OPTION)) {
            // exit game
            dispose();
        } else {
            // start next game
        }
    }
}
