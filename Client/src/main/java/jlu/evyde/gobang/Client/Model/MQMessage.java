package jlu.evyde.gobang.Client.Model;

import com.google.gson.Gson;
import jlu.evyde.gobang.Client.Controller.Utils;

import java.io.Serializable;

public class MQMessage implements Serializable {

    public String status;
    public int code;

    public String method;
    public SystemConfiguration.MQ_Source from;
    public int x;
    public int y;

    private static final Gson parser = new Gson();

    public static String constructConsumeMessage(SystemConfiguration.MQ_Source mqs) {
        MQMessage m = new MQMessage();
        m.from = mqs;
        return SystemConfiguration.getMQConsumeHead() + "\n" +
                parser.toJson(m, MQMessage.class) + "\n" +
                SystemConfiguration.getMQMsgEnd() + "\n";
    }

    public static String constructRegisterMessage(SystemConfiguration.MQ_Source mqs) {
        MQMessage m = new MQMessage();
        m.from = mqs;
        return SystemConfiguration.getMqRegisterHead() + "\n" +
                parser.toJson(m, MQMessage.class) + "\n" +
                SystemConfiguration.getMQMsgEnd() + "\n";
    }

    public static String generateProduceMessage(SystemConfiguration.MQ_Source mqs) {
        MQMessage m = new MQMessage();
        m.from = mqs;
        m.status = Utils.generateRandomString(2);
        m.code = Utils.generateRandomInt(200, 300);
        m.x = Utils.generateRandomInt(0, 300);
        m.y = Utils.generateRandomInt(0, 300);
        m.method = "put";
        return SystemConfiguration.getMQProduceHead() + "\n" +
                parser.toJson(m, MQMessage.class) + "\n" +
                SystemConfiguration.getMQMsgEnd() + "\n";
    }

    public static SystemConfiguration.MQ_Source parseMQConsumeMessage(String json) {
        return parser.fromJson(json, MQMessage.class).from;
    }

    public static MQMessage fromJson(String message) {
        return parser.fromJson(message, MQMessage.class);
    }

    public String toJson() {
        return toString();
    }

    @Override
    public String toString() {
        return parser.toJson(this);
    }
}
