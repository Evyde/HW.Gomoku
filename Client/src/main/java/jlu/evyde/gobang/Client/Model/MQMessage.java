package jlu.evyde.gobang.Client.Model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jlu.evyde.gobang.Client.Controller.Utils;

import java.awt.*;
import java.io.Serializable;
import java.util.EnumMap;
import java.util.Objects;
import java.util.UUID;

public class MQMessage implements Serializable {

    public MQProtocol.Status status;
    public Integer code;

    public String method;
    public MQProtocol.Chess chess;
    public MQProtocol.Group group;
    public UUID token;
    public String msg;
    public EnumMap<MQProtocol.Chess.Color, Integer> score;

    private static final Gson parser = new GsonBuilder().enableComplexMapKeySerialization().create();

    @Deprecated
    public static String constructConsumeMessage(MQProtocol.Group group) {
        MQMessage m = new MQMessage();
        m.group = group;
        return MQProtocol.Head.constructRequest(MQProtocol.Head.CONSUME, m);
    }

    public static String constructRegisterMessage(MQProtocol.Group group) {
        MQMessage m = new MQMessage();
        m.group = group;
        m.token = group.getInitializedUUID();
        return constructRegisterMessage(m);
    }

    public static String constructRegisterMessage(MQMessage message) {
        return MQProtocol.Head.constructRequest(MQProtocol.Head.REGISTER, message);
    }

    public static String generateProduceMessage(MQProtocol.Group group) {
        MQMessage m = generateRandomMQMessage(group);
        m.token = UUID.randomUUID();
        return generateProduceMessage(m);
    }

    public static String generateProduceMessage(MQProtocol.Group group, UUID token) {
        MQMessage m = generateRandomMQMessage(group);
        m.token = token;
        return generateProduceMessage(m);
    }

    public static MQMessage generateRandomMQMessage(MQProtocol.Group group) {
        return generateRandomMQMessage(group, MQProtocol.Chess.Color.BLACK);
    }

    public static MQMessage generateRandomMQMessage(MQProtocol.Group group, MQProtocol.Chess.Color color) {
        MQMessage m = new MQMessage();
        m.group = group;
        m.status = MQProtocol.Status.SUCCESS;
        m.code = Utils.generateRandomInt(200, 300);
        m.chess = new MQProtocol.Chess(
                new Point(
                        Utils.generateRandomInt(0, 300),
                        Utils.generateRandomInt(0, 300)
                ),
                color
        );
        m.method = "put";
        return m;
    }

    public static String generateProduceMessage(MQMessage m) {
        return MQProtocol.Head.constructRequest(MQProtocol.Head.PRODUCE, m);
    }

    public static MQProtocol.Group parseMQConsumeMessage(String json) {
        return parser.fromJson(json, MQMessage.class).group;
    }

    public static MQMessage fromJson(String message) {
        return parser.fromJson(message, MQMessage.class);
    }

    public String toJson() {
        parser.newBuilder().enableComplexMapKeySerialization();
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
            return Objects.equals(this.group, ((MQMessage) obj).group)
                    && Objects.equals(this.msg, ((MQMessage) obj).msg)
                    && Objects.equals(this.code, ((MQMessage) obj).code)
                    // && Objects.equals(this.token, ((MQMessage) obj).token) // Obviously equals
                    && Objects.equals(this.status, ((MQMessage) obj).status)
                    && Objects.equals(this.chess, ((MQMessage) obj).chess)
                    && Objects.equals(this.method, ((MQMessage) obj).method);
        }
        return false;
    }
}
