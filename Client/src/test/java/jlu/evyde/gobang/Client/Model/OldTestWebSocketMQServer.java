package jlu.evyde.gobang.Client.Model;

import jlu.evyde.gobang.Client.Controller.Utils;
import org.java_websocket.client.WebSocketClient;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;

import static jlu.evyde.gobang.Client.Controller.Utils.*;
import static org.junit.Assert.*;
import static oshi.util.Util.sleep;

@Deprecated
public class OldTestWebSocketMQServer {


    @Test
    public void normalAddTest() {
        int port = Utils.generateRandomInt(8000, 65535);
        WebSocketMQServer mbs = (WebSocketMQServer) startServer(port);
        WebSocketClient client = createClient(port, new ArrayList<>());
        assertNotNull(client);

        try {
            sleep(1000);
        } catch (Exception e) {

        }

        client.connect();

        try {
            sleep(1000);
        } catch (Exception e) {

        }

        client.send("PRODUCE\n" +
                "{\"status\": \"ok\", \"code\": 200}\n" +
                "END");

        client.send("PRODUCE\n" +
                "{\"status\": \"ok\", \"code\": 200}\n" +
                "END");

        try {
            sleep(1000);
        } catch (Exception e) {

        }

        client.close();
        closeServer(mbs);
    }

    @Test
    public void normalConsumeTest() {
        int port = Utils.generateRandomInt(8000, 65535);
        ArrayList<String> expected = new ArrayList<>();
        ArrayList<String> actual = new ArrayList<>();
        MQBrokerServer mbs = startServer(port);
        WebSocketClient client = createClient(port, actual);
        assertNotNull(client);

        try {
            sleep(1000);
        } catch (Exception e) {

        }

        client.connect();

        try {
            sleep(1000);
        } catch (Exception e) {

        }

        client.send("PRODUCE\n" +
                "{\"status\": \"ok\", \"code\": 200,from:\"UI\"}\n" +
                "END");

        client.send("PRODUCE\n" +
                "{\"status\": \"ok\", \"code\": 200,from:\"LOGIC\"}\n" +
                "END");

        try {
            sleep(1000);
        } catch (Exception e) {

        }


        System.out.println();

        client.send(MQMessage.constructConsumeMessage(SystemConfiguration.MQ_Source.UI));
        client.send(MQMessage.constructConsumeMessage(SystemConfiguration.MQ_Source.LOGIC));

        try {
            sleep(1000);
        } catch (Exception e) {

        }

        expected.add("{\"status\":\"ok\",\"code\":200,\"from\":\"" + SystemConfiguration.MQ_Source.LOGIC.name() + "\"," +
                "\"x\":0," +
                "\"y\":0}");
        expected.add("{\"status\":\"ok\",\"code\":200,\"from\":\"" + SystemConfiguration.MQ_Source.UI.name() +
                "\",\"x\":0,\"y\":0}");
        assertArrayEquals(expected.toArray(), actual.toArray());

        client.close();
        closeServer(mbs);
    }

    @Test
    public void errorTest() {
        int port = Utils.generateRandomInt(8000, 65535);
        WebSocketMQServer mbs = (WebSocketMQServer) startServer(port);
        WebSocketClient client = createClient(port, new ArrayList<>());
        assertNotNull(client);

        try {
            sleep(1000);
        } catch (Exception e) {

        }

        client.connect();

        try {
            sleep(1000);
        } catch (Exception e) {

        }

        client.send("PRODUCE\n" +
                "{\"status\": \"ok\", \"code\": 200\n" +
                "END");

        client.send("END" +
                "{\"status\": \"ok\", \"code\": 200}\n" +
                "END");

        try {
            sleep(1000);
        } catch (Exception e) {

        }

        client.close();
        closeServer(mbs);
    }

    @Test
    public void randomlyTest() {
        int port = Utils.generateRandomInt(8000, 65535);
        Deque<String> expected = new LinkedList<>();
        LinkedList<String> actual = new LinkedList<>();
        MQBrokerServer mbs = startServer(port);
        WebSocketClient client = createClient(port, actual);
        assertNotNull(client);

        try {
            sleep(1000);
        } catch (Exception e) {

        }

        client.connect();

        try {
            sleep(1000);
        } catch (Exception e) {

        }

        int times = 100;

        while (times-- > 0) {
            int choice = Utils.generateRandomInt(0, 3);
            if (choice == 0) {
                // add
                MQMessage mqMessage = new MQMessage();

                mqMessage.x = Utils.generateRandomInt(0, 100);
                mqMessage.y = Utils.generateRandomInt(0, 100);
                mqMessage.method = Utils.generateRandomString(8);
                mqMessage.code = Utils.generateRandomInt(100, 500);
                mqMessage.status = Utils.generateRandomString(7);
                mqMessage.from = SystemConfiguration.MQ_Source.UI;

                client.send(SystemConfiguration.getMQProduceHead() +
                        "\n" + mqMessage.toJson() + "\n" + SystemConfiguration.getMQMsgEnd() + "\n");
                expected.add(mqMessage.toJson());
            } else if (choice == 1) {
                if (!expected.isEmpty()) {
                    // consume
                    try {
                        sleep(100);
                    } catch (Exception e) {

                    }
                    client.send(MQMessage.constructConsumeMessage(SystemConfiguration.MQ_Source.LOGIC));
                    try {
                        sleep(100);
                    } catch (Exception e) {

                    }
                    assertEquals(expected.removeFirst(), actual.removeFirst());
                }
            } else if (choice == 2) {
                // add something strange
                client.send(SystemConfiguration.getMQProduceHead()
                        + "\n" + ">\n" + SystemConfiguration.getMQMsgEnd() + "\n");
                client.send("L\n" + ">\n" + SystemConfiguration.getMQMsgEnd() + "\n");
            }
        }

        client.close();
        closeServer(mbs);
    }
}
