package jlu.evyde.gobang.Client.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.ResourceBundle;

public class SystemConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(SystemConfiguration.class);
    public static final Locale LOCALE = Locale.getDefault();
    private static final ResourceBundle bundle = ResourceBundle.getBundle("SystemConfiguration", LOCALE);
    private static final int MQ_SERVER_PORT = 8887;
    private static final String MQ_SERVER_HOST = "localhost";
    private static final String MQ_PRODUCE_HEAD = "PRODUCE";
    private static final String MQ_CONSUME_HEAD = "CONSUME";
    private static final String MQ_MSG_END = "END";
    private static final String MQ_GROUP_HEAD = "GROUP";

    public enum MQ_Source {
        UI {
            @Override
            public boolean consume(MQ_Source ms) {
                return ms == MQ_Source.LOGIC;
            }
        },
        CLIENT {
            @Override
            public boolean consume(MQ_Source ms) {
                return ms == MQ_Source.SERVER;
            }
        },
        SERVER {
            @Override
            public boolean consume(MQ_Source ms) {
                return ms == MQ_Source.CLIENT;
            }
        },
        LOGIC {
            @Override
            public boolean consume(MQ_Source ms) {
                return ms == MQ_Source.UI;
            }
        },
        ;
        public abstract boolean consume(MQ_Source ms);

        @Override
        public String toString() {
            return bundle.getString(this.name());
        }
    }

    public enum Chess {
        WHITE,
        BLACK,;
        @Override
        public String toString() {
            return bundle.getString(this.name());
        }
    }


    public static int getMQServerPort() {
        return MQ_SERVER_PORT;
    }

    public static String getMQServerHost() {
        return MQ_SERVER_HOST;
    }

    public static String getMQConsumeHead() {
        return MQ_CONSUME_HEAD;
    }

    public static String getMQMsgEnd() {
        return MQ_MSG_END;
    }

    public static String getMQProduceHead() {
        return MQ_PRODUCE_HEAD;
    }

    public static String getMQGroupHead() {
        return MQ_GROUP_HEAD;
    }
}
