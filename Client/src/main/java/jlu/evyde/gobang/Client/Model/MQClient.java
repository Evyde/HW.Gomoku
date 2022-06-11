package jlu.evyde.gobang.Client.Model;

import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class MQClient implements Serializable {
    private WebSocket webSocket;
    private UUID token;
    private MQProtocol.Group group;

    public MQClient(WebSocket webSocket) {
        this(webSocket, UUID.randomUUID(), MQProtocol.Group.GUEST);
    }

    public MQClient(UUID token) {
        this(null, token, null);
    }

    public MQClient(WebSocket webSocket, UUID token, MQProtocol.Group group) {
        this.webSocket = webSocket;
        this.token = token;
        this.group = group;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MQClient) {
            if (this.token != null && ((MQClient) obj).token != null) {
                return Objects.equals(this.token, ((MQClient) obj).token);
            } else {
                return Objects.equals(this.getWebSocket(), ((MQClient) obj).getWebSocket());
            }
        } else if (obj instanceof UUID) {
            return Objects.equals(this.token, obj);
        } else if (obj instanceof WebSocket) {
            return Objects.equals(webSocket, obj);
        }
        return false;
    }

    public boolean verify(UUID token) {
        return Objects.equals(this.token, token);
    }

    public boolean verify(MQMessage message) {
        return Objects.equals(this.token, message.token);
    }

    public void setGroup(MQProtocol.Group group) {
        this.group = group;
    }

    public void setToken(UUID token) {
        this.token = token;
    }

    public void setWebSocket(WebSocket webSocket) {
        this.webSocket = webSocket;
    }

    public MQProtocol.Group getGroup() {
        return group;
    }

    public UUID getToken() {
        return token;
    }

    public boolean isClosed() {
        return webSocket == null || webSocket.isClosed();
    }

    public WebSocket getWebSocket() {
        return webSocket;
    }

    public boolean send(String message) {
        if (webSocket != null && !webSocket.isClosed()) {
            webSocket.send(message);
            return true;
        }
        return false;
    }

    public boolean send(MQMessage message) {
        if (webSocket != null && !webSocket.isClosed() && message != null) {
            webSocket.send(message.toJson());
            return true;
        }
        return false;
    }

    public void close(String message) {
        if (webSocket != null && !webSocket.isClosed()) {
            webSocket.close(CloseFrame.ABNORMAL_CLOSE, message);
        }
    }

    public void close(MQMessage message) {
        if (webSocket != null && !webSocket.isClosed() && message != null) {
            webSocket.close(CloseFrame.ABNORMAL_CLOSE, message.toJson());
        }
    }

    public void close() {
        if (webSocket != null) {
            webSocket.close();
        }
    }

    @Override
    public int hashCode() {
        if (getToken() != null) {
            return getToken().hashCode();
        } else {
            return getWebSocket().hashCode();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("MQClient [");
        if (webSocket != null) {
            sb.append(webSocket.getRemoteSocketAddress());
            sb.append(", ");
        }
        if (token != null) {
            sb.append(token);
            sb.append(", ");
        }
        if (group != null) {
            sb.append(group);
        }
        sb.append("]");
        return sb.toString();
    }
}
