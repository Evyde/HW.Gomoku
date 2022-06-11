package jlu.evyde.gobang.Client.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;

public class SystemConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(SystemConfiguration.class);
    public static final Locale LOCALE = Locale.getDefault();
    // TODO: Read and set these properties from file.
    private static final int MQ_SERVER_PORT = 8887;// Utils.generateRandomInt(8000, 65535);
    private static final String MQ_SERVER_HOST = "localhost";
    private static final UUID INITIALIZED_UUID = UUID.nameUUIDFromBytes("Evyde HF 2022-06".getBytes());
    private static final Integer MAX_RETRY_TIME = 5;
    private static final Integer SLEEP_TIME = 200;
    private static final Integer BOARD_WIDTH = 15;
    private static final Integer BOARD_HEIGHT = 15;

    private static final Double BOARD_SCALE = 0.8;
    private static final MQProtocol.Chess.Color FIRST = MQProtocol.Chess.Color.WHITE;
    private static final String CWD = System.getProperty("user.dir") + "/";
    private static final Integer WIN_CONTINUOUS_NUM = 5;
    private static final String SCORE_FILE_NAME = "gobang-score.json";
    private static final String BACKGROUND_IMAGE = "/background.png";
    private static final String WHITE_CHESS = "/circle-light.svg";
    private static final String BLACK_CHESS = "/circle-dark.svg";
    private static final String WHITE_WINNER_CHESS = "/circle-light-winner.svg";
    private static final String BLACK_WINNER_CHESS = "/circle-dark-winner.svg";

    public static int getMQServerPort() {
        return MQ_SERVER_PORT;
    }

    public static String getMQServerHost() {
        return MQ_SERVER_HOST;
    }

    @Deprecated
    public static UUID getInitializedUuid() {
        return INITIALIZED_UUID;
    }

    public static Integer getMaxRetryTime() {
        return MAX_RETRY_TIME;
    }

    public static Integer getSleepTime() {
        return SLEEP_TIME;
    }

    public static MQProtocol.Chess.Color getFIRST() {
        return FIRST;
    }

    public static Integer getBoardWidth() {
        return BOARD_WIDTH;
    }

    public static Integer getBoardHeight() {
        return BOARD_HEIGHT;
    }

    public static Double getBoardScale() {
        return BOARD_SCALE;
    }

    public static Integer getWinContinuousNum() {
        return WIN_CONTINUOUS_NUM;
    }

    public static String getScoreFileName() {
        return SCORE_FILE_NAME;
    }

    public static InputStream getBackgroundImage() {
        return SystemConfiguration.class.getResourceAsStream(BACKGROUND_IMAGE);
    }

    public static InputStream getBlackChess() {
        return SystemConfiguration.class.getResourceAsStream(BLACK_CHESS);
    }

    public static InputStream getBlackWinnerChess() {
        return SystemConfiguration.class.getResourceAsStream(BLACK_WINNER_CHESS);
    }

    public static InputStream getWhiteChess() {
        return SystemConfiguration.class.getResourceAsStream(WHITE_CHESS);
    }

    public static InputStream getWhiteWinnerChess() {
        return SystemConfiguration.class.getResourceAsStream(WHITE_WINNER_CHESS);
    }

    public static MQProtocol.Chess.Color getNextColor() {
        return getFIRST() == MQProtocol.Chess.Color.WHITE? MQProtocol.Chess.Color.BLACK: MQProtocol.Chess.Color.WHITE;
    }
}
