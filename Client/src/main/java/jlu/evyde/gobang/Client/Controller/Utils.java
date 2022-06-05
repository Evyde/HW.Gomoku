package jlu.evyde.gobang.Client.Controller;

import jlu.evyde.gobang.Client.Model.MQBrokerServer;
import jlu.evyde.gobang.Client.Model.MQBrokerServerFactory;
import jlu.evyde.gobang.Client.Model.MQServerAddress;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;

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

    public static MQBrokerServer startServer(int port) throws GobangException.MQServerStartFailedException {
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

    public static WebSocketClient createClient(int port, List<String> receive) {
        MQServerAddress msa = new MQServerAddress();
        try {
            msa.setUri(new URI("ws://localhost:" + port + "/"));
            return new WebSocketClient(msa.getUri()) {
                @Override
                public void onOpen(ServerHandshake serverHandshake) {

                }

                @Override
                public void onMessage(String s) {
                    receive.add(s);
                }

                @Override
                public void onClose(int i, String s, boolean b) {

                }

                @Override
                public void onError(Exception e) {

                }
            };
        } catch (Exception e) {
            return null;
        }
    }
}
