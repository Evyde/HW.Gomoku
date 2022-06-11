package jlu.evyde.gobang.Client.Controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jlu.evyde.gobang.Client.Model.*;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

import static java.lang.Thread.sleep;

public class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);
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

    public static String readLineFromFile(String filename) {
        try {
            BufferedReader file = new BufferedReader(new FileReader(getFullFilePath(filename)));
            return file.readLine();
        } catch (Exception e) {
            return "{}";
        }
    }

    public static boolean isFileExists(String filename) {
        return new File(filename).exists();
    }

    public static String getFullFilePath(String filename) {
        String cwd = System.getProperty("user.dir");
        return cwd + "/" + filename;
    }

    public static EnumMap<MQProtocol.Chess.Color, Integer> generateEmptyScoreMap() {
        EnumMap<MQProtocol.Chess.Color, Integer> score = new EnumMap<>(MQProtocol.Chess.Color.class);
        for (MQProtocol.Chess.Color c: MQProtocol.Chess.Color.values()) {
            score.put(c, 0);
        }
        return score;
    }

    public static void createScoreFileWithDefaultValues(String filename) {
        MQMessage m = new MQMessage();

        m.score = generateEmptyScoreMap();
        try {
            Writer writer = new FileWriter(filename);
            new GsonBuilder().enableComplexMapKeySerialization().create().toJson(m, writer);
            writer.close();
        } catch (IOException e) {
            logger.error(e.toString());
            e.printStackTrace();
        }
    }

    public static void saveScoreToFile(String filename, EnumMap<MQProtocol.Chess.Color, Integer> score) {
        if (!isFileExists(getFullFilePath(filename))) {
            logger.warn("Score file not found, create.");
            createScoreFileWithDefaultValues(getFullFilePath(filename));
        }
        try {
            MQMessage m = new MQMessage();
            m.score = score;
            Writer writer = new FileWriter(filename);
            new GsonBuilder().enableComplexMapKeySerialization().create().toJson(m, writer);
            writer.close();
        } catch (IOException e) {
            logger.error("Saving score to file error: {}.", e.toString());
            e.printStackTrace();
        }
    }

    public static EnumMap<MQProtocol.Chess.Color, Integer> readScoreFromJson(String json) {
        Gson parser = new GsonBuilder().enableComplexMapKeySerialization().create();
        return parser.fromJson(json, MQMessage.class).score;
    }

    public static EnumMap<MQProtocol.Chess.Color, Integer> readScoreFromFile(String filename) {
        Gson parser = new GsonBuilder().enableComplexMapKeySerialization().create();
        try {
            return parser.fromJson(new FileReader(getFullFilePath(filename)), MQMessage.class).score;
        } catch (Exception e) {
            return generateEmptyScoreMap();
        }
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
        private MQProtocol.Group role;

        public TestWebSocketClient(URI serverUri, List<MQMessage> receive, MQProtocol.Group role) {
            super(serverUri);
            this.receive = receive;
            this.role = role;
        }

        @Override
        public void onOpen(ServerHandshake serverHandshake) {
            if (MQProtocol.Group.GAMER.equals(role)) {
                MQMessage m = new MQMessage();
                m.token = MQProtocol.Group.GAMER.getInitializedUUID();
                m.group = role;
                m.chess = new MQProtocol.Chess(new Point(), MQProtocol.Chess.Color.WHITE);
                this.send(MQMessage.constructRegisterMessage(m));
                try {
                    sleep(500);
                } catch (Exception e) {

                }
                m.chess = new MQProtocol.Chess(new Point(), MQProtocol.Chess.Color.BLACK);
                this.send(MQMessage.constructRegisterMessage(m));
            } else {
                this.send(MQMessage.constructRegisterMessage(role));
            }
        }

        @Override
        public void onMessage(String s) {
            MQMessage m = MQMessage.fromJson(s);
            if ((clientToken == null && MQProtocol.Code.UPDATE_TOKEN.getCode().equals(m.code))
                    && m.group == MQProtocol.Group.LOGIC_SERVER) {
                if (m.token != null) {
                    clientToken = m.token;
                    receive.add(m);
                } else {
                    System.err.println("Server returns wrong token!");
                    throw new GobangException.CommunicatorInitFailedException();
                }
            } else if (MQProtocol.Code.REGISTER_FAILED.getCode().equals(m.code)) {
                // do nothing
            } else if (MQProtocol.Code.NO_OPERATION.getCode().equals(m.code)) {
                // also do nothing
            } else {
                receive.add(m);
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
            if (clientToken != null) {
                m.token = clientToken;
            }
            send(MQProtocol.Head.constructRequest(p, m));
        }
    }

    public static TestWebSocketClient createTestClient(int port, List<MQMessage> receive, MQProtocol.Group id) {

        MQServerAddress msa = new MQServerAddress();
        try {
            msa.setUri(new URI("ws://localhost:" + port + "/"));
            return new TestWebSocketClient(msa.getUri(), receive, id);
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
