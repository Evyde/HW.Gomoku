package jlu.evyde.gobang.Client.Controller;

import jlu.evyde.gobang.Client.Model.*;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

public class Utils {
    public static String toUTF8String(String s) {
        String ss = "";
        try {
             ss = new String(s.getBytes("ISO_8859_1"), "GBK");
        } catch (UnsupportedEncodingException uee) {
            return "";
        }
        System.out.println(ss);
        return ss;
    }

    public static int generateRandomInt(int min, int max) {
        return min + (int)(Math.random() * ((max - min) + 1));
    }

    public static String generateRandomString(int length) {
        byte[] array = new byte[length];
        new Random().nextBytes(array);
        return new String(array, StandardCharsets.UTF_8);
    }

    public static MQBrokerServer startTestServer(int port) throws GobangException.MQServerStartFailedException {
        MQServerAddress msa = new MQServerAddress();
        msa.setIsa(new InetSocketAddress(port));
        MQBrokerServer mbs = MQBrokerServerFactory.getWebSocketMQServer();
        mbs.startMQBrokerServer(
                msa,
                () -> {

                },
                () -> {

                }
        );
        return mbs;
    }

    public static void closeServer(MQBrokerServer mbs) {
        try {
            mbs.closeMQBrokerServer(() -> {}, () -> {});
        } catch (Exception e) {

        }
    }

    public static class TestWebSocketClient extends WebSocketClient {
        private UUID clientToken;
        private List<MQMessage> receive;

        public TestWebSocketClient(URI serverUri, List<MQMessage> receive) {
            super(serverUri);
            this.receive = receive;
        }

        @Override
        public void onOpen(ServerHandshake serverHandshake) {

        }

        @Override
        public void onMessage(String s) {
            try {
                MQMessage m = MQMessage.fromJson(s);
                if ((clientToken == null || MQProtocol.Code.UPDATE_TOKEN.getCode().equals(m.code))
                        && m.group == MQProtocol.Group.LOGIC_SERVER) {
                    if (m.token != null) {
                        clientToken = m.token;
                    } else {
                        System.err.println("Server returns wrong token!");
                    }
                } else {
                    receive.add(m);
                }
            } catch (Exception e) {

            }
        }

        @Override
        public void onClose(int i, String s, boolean b) {

        }

        @Override
        public void onError(Exception e) {

        }

        public UUID getClientToken() {
            return clientToken;
        }

        public void send(MQMessage m, MQProtocol.Head p) {
            m.token = clientToken;
            send(MQProtocol.Head.constructRequest(p, m));
        }
    }

    public static TestWebSocketClient createTestClient(int port, List<MQMessage> receive) {

        MQServerAddress msa = new MQServerAddress();
        try {
            msa.setUri(new URI("ws://localhost:" + port + "/"));
            return new TestWebSocketClient(msa.getUri(), receive) ;
        } catch (Exception e) {
            return null;
        }
    }

    public static <T> void assertListEquals(List<T> expectedUIMessageList, List<T> m) {
        if (expectedUIMessageList.size() != m.size()) {
            System.err.println("预期长度: " + expectedUIMessageList.size());
            System.err.println("实际长度: " + m.size());
            throw new AssertionError();
        }
        if (m.size() == 0) {
            return;
        }
        if (!expectedUIMessageList.containsAll(m)) {
            System.err.println("预期: " + expectedUIMessageList);
            System.err.println("实际: " + m);
            throw new AssertionError();
        }
    }
}
