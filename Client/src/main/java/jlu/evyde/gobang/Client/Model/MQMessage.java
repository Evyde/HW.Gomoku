package jlu.evyde.gobang.Client.Model;

import com.google.gson.Gson;
import jlu.evyde.gobang.Client.Controller.Utils;

import java.awt.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class MQMessage implements Serializable {

    public MQProtocol.Status status;
    public Integer code;

    public String method;
    public MQProtocol.MQSource from;
    public MQProtocol.Chess chess;
    public UUID token;
    public String msg;

    private static final Gson parser = new Gson();

    @Deprecated
    public static String constructConsumeMessage(MQProtocol.MQSource mqs) {
        MQMessage m = new MQMessage();
        m.from = mqs;
        return MQProtocol.Head.constructRequest(MQProtocol.Head.CONSUME, m);
    }

    public static String constructRegisterMessage(MQProtocol.MQSource mqs) {
        MQMessage m = new MQMessage();
        m.from = mqs;
        m.token = SystemConfiguration.getInitializedUuid();
        return MQProtocol.Head.constructRequest(MQProtocol.Head.REGISTER, m);
    }

    public static String generateProduceMessage(MQProtocol.MQSource mqs) {
        MQMessage m = generateRandomMQMessage(mqs);
        m.token = UUID.randomUUID();
        return generateProduceMessage(m);
    }

    public static String generateProduceMessage(MQProtocol.MQSource mqs, UUID token) {
        MQMessage m = generateRandomMQMessage(mqs);
        m.token = token;
        return generateProduceMessage(m);
    }

    public static MQMessage generateRandomMQMessage(MQProtocol.MQSource mqs) {
        MQMessage m = new MQMessage();
        m.from = mqs;
        m.status = MQProtocol.Status.SUCCESS;
        m.code = Utils.generateRandomInt(200, 300);
        m.chess = new MQProtocol.Chess(
                new Point(
                        Utils.generateRandomInt(0, 300),
                        Utils.generateRandomInt(0, 300)
                ),
                MQProtocol.Chess.Color.BLACK
        );
        m.method = "put";
        return m;
    }

    public static String generateProduceMessage(MQMessage m) {
        return MQProtocol.Head.constructRequest(MQProtocol.Head.PRODUCE, m);
    }

    public static MQProtocol.MQSource parseMQConsumeMessage(String json) {
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

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MQMessage) {
            // f**king compiler
            return Objects.equals(this.from, ((MQMessage) obj).from)
                    && Objects.equals(this.msg, ((MQMessage) obj).msg)
                    && Objects.equals(this.code, ((MQMessage) obj).code)
                    && Objects.equals(this.token, ((MQMessage) obj).token)
                    && Objects.equals(this.status, ((MQMessage) obj).status)
                    && Objects.equals(this.chess, ((MQMessage) obj).chess)
                    && Objects.equals(this.method, ((MQMessage) obj).method);
        }
        return false;
    }
}
