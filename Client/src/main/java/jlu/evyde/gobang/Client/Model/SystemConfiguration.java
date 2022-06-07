package jlu.evyde.gobang.Client.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.UUID;

public class SystemConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(SystemConfiguration.class);
    public static final Locale LOCALE = Locale.getDefault();
    private static final ResourceBundle bundle = ResourceBundle.getBundle("SystemConfiguration", LOCALE);
    // TODO: Read and set these properties from file.
    private static final int MQ_SERVER_PORT = 8887;
    private static final String MQ_SERVER_HOST = "localhost";
    private static final UUID INITIALIZED_UUID = UUID.nameUUIDFromBytes("Evyde HF 2022-06".getBytes());
    private static final Integer MAX_RETRY_TIME = 5;
    private static final Integer SLEEP_TIME = 200;
    private static final MQProtocol.Chess.Color FIRST = MQProtocol.Chess.Color.WHITE;

    public static int getMQServerPort() {
        return MQ_SERVER_PORT;
    }

    public static String getMQServerHost() {
        return MQ_SERVER_HOST;
    }

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
}
