package jlu.evyde.gobang.Client.Model;

import jlu.evyde.gobang.Client.Controller.Utils;
import org.java_websocket.client.WebSocketClient;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Thread.sleep;
import static jlu.evyde.gobang.Client.Controller.Utils.createClient;
import static jlu.evyde.gobang.Client.Controller.Utils.startServer;
import static org.junit.Assert.*;

public class TestWebSocketMQServer {
    @Test
    public void normalTest() {
        // construct 2 ui client, 1 logic server
        int port = Utils.generateRandomInt(8000, 65535);
        MQBrokerServer server = startServer(port);
        List<String> ui1List = new ArrayList<>();
        List<String> ui2List = new ArrayList<>();
        List<String> logicList = new ArrayList<>();

        try {
            sleep(500);
        } catch (Exception e) {

        }

        WebSocketClient ui1 = createClient(port, ui1List);
        WebSocketClient ui2 = createClient(port, ui2List);
        WebSocketClient logic = createClient(port, logicList);
        ui1.connect();
        ui2.connect();
        logic.connect();

        try {
            sleep(500);
        } catch (Exception e) {

        }

        ui1.send(MQMessage.constructRegisterMessage(SystemConfiguration.MQ_Source.UI));
        ui2.send(MQMessage.constructRegisterMessage(SystemConfiguration.MQ_Source.UI));
        logic.send(MQMessage.constructRegisterMessage(SystemConfiguration.MQ_Source.LOGIC));

        try {
            sleep(50);
        } catch (Exception e) {

        }

        // ui1 send, all will receive.
        ui1.send(MQMessage.generateProduceMessage(SystemConfiguration.MQ_Source.LOGIC));
        try {
            sleep(50);
        } catch (Exception e) {

        }
        assertEquals(ui1List.get(0), ui2List.get(0));
        assertEquals(ui2List.get(0), logicList.get(0));

        // ui2 send, all will receive.
        ui2.send(MQMessage.generateProduceMessage(SystemConfiguration.MQ_Source.UI));
        try {
            sleep(50);
        } catch (Exception e) {

        }
        assertEquals(ui1List.get(1), ui2List.get(1));
        assertEquals(ui2List.get(1), logicList.get(1));

        // logic send, except for logic, all will receive
        logic.send(MQMessage.generateProduceMessage(SystemConfiguration.MQ_Source.UI));
        try {
            sleep(50);
        } catch (Exception e) {

        }
        assertEquals(ui1List.get(1), ui2List.get(1));
        assertEquals(2, logicList.size());
    }
}
