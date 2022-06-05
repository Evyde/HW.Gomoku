package jlu.evyde.gobang.Client.Model;

public class MQBrokerServerFactory {
    public static MQBrokerServer getWebSocketMQServer() {
        return new WebSocketMQServer();
    }

    public static MQBrokerServer getRawSocketMQServer() {
        return new RawSocketMQBrokerServer();
    }
}
