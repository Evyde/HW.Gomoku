package jlu.evyde.gobang.Client.Model;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class MQBrokerServerFactory {
    @Contract(" -> new")
    public static @NotNull MQBrokerServer getWebSocketMQServer() {
        return new WebSocketMQServer();
    }

    @Contract(value = " -> new", pure = true)
    public static @NotNull MQBrokerServer getRawSocketMQServer() {
        return new RawSocketMQBrokerServer();
    }
}
