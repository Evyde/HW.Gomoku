package jlu.evyde.gobang.Client.Controller;

public class CommunicatorFactory {
    public static Communicator getWebSocketCommunicator() {
        return new WebSocketCommunicator();
    }

    public static Communicator getRawSocketCommunicator() {
        return null;
    }
}
