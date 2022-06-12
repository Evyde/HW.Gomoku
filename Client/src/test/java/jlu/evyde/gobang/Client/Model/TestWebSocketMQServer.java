package jlu.evyde.gobang.Client.Model;

import jlu.evyde.gobang.Client.Controller.Utils;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.util.*;

import static java.lang.Thread.sleep;
import static jlu.evyde.gobang.Client.Controller.Utils.*;
import static org.junit.Assert.*;

public class TestWebSocketMQServer {
    @Test
    public void normalTest() {
        // construct 2 ui client, 1 logic server
        int port = Utils.generateRandomInt(8000, 65535);

        MQBrokerServer server = startTestServer(port);
        List<MQMessage> ui1List = new ArrayList<>();
        List<MQMessage> ui2List = new ArrayList<>();
        List<MQMessage> logicList = new ArrayList<>();

        try {
            sleep(50);
        } catch (Exception e) {

        }

        TestWebSocketClient ui1 = createTestClient(port, ui1List, MQProtocol.Group.GAMER);
        TestWebSocketClient ui2 = createTestClient(port, ui2List, MQProtocol.Group.GAMER);
        LogicServer logic = new LogicServer(logicList, port);

        try {
            sleep(50);
        } catch (Exception e) {

        }
        ui1.connect();
        ui2.connect();
        try {
            sleep(1000);
        } catch (Exception e) {

        }
        ui1.send("");
        ui2.send("");
        try {
            sleep(1000);
        } catch (Exception e) {

        }
        assertEquals(ui1List.get(1), ui2List.get(1));
        if (ui1List.get(0).chess.getColor().equals(SystemConfiguration.getFIRST())) {
            // ui1 send, all will receive.
            ui1.send(MQMessage.generateRandomMQMessage(MQProtocol.Group.GAMER, ui1List.get(0).chess.getColor()), MQProtocol.Head.PRODUCE);
            try {
                sleep(500);
            } catch (Exception e) {

            }
            assertEquals(ui1List.get(2), ui2List.get(2));
            assertEquals(ui1List.get(2).chess, logicList.get(1).chess);
            assertEquals(ui1List.get(2).code, logicList.get(1).code);
            assertEquals(ui1List.get(2).status, logicList.get(1).status);
            assertNotEquals(ui1List.get(2), logicList.get(1));
        } else {
            // ui2 send, all will receive.
            ui2.send(MQMessage.generateRandomMQMessage(MQProtocol.Group.GAMER, ui2List.get(0).chess.getColor()),
                    MQProtocol.Head.PRODUCE);
            try {
                sleep(500);
            } catch (Exception e) {

            }
            assertEquals(ui1List.get(2), ui2List.get(2));
            assertEquals(ui2List.get(2).chess, logicList.get(1).chess);
            assertEquals(ui2List.get(2).code, logicList.get(1).code);
            assertEquals(ui2List.get(2).status, logicList.get(1).status);
            assertNotEquals(ui2List.get(2), logicList.get(1));
        }

        // logic send, except for logic, all will receive
        logic.send(MQMessage.generateRandomMQMessage(MQProtocol.Group.LOGIC_SERVER), MQProtocol.Head.PRODUCE);
        try {
            sleep(50);
        } catch (Exception e) {

        }
        assertEquals(ui1List.get(1), ui2List.get(1));
        assertEquals(2, logicList.size());
        closeServer(server);
    }

    @Test
    public void duplicateLogicServer() {
        int port = Utils.generateRandomInt(8000, 65535);
        MQBrokerServer server = startTestServer(port);
        List<MQMessage> ui1List = new ArrayList<>();
        List<MQMessage> ui2List = new ArrayList<>();
        List<MQMessage> logicList = new ArrayList<>();

        TestWebSocketClient ui1 = createTestClient(port, ui1List, MQProtocol.Group.GAMER);
        TestWebSocketClient ui2 = createTestClient(port, ui2List, MQProtocol.Group.GAMER);
        LogicServer logic = new LogicServer(new MQServerAddress(port));
        ui1.connect();
        ui2.connect();
        try {
            sleep(50);
        } catch (Exception e) {

        }
        ui1.send("");
        ui2.send("");

        new LogicServer(logicList, port);

        try {
            sleep(500);
        } catch (Exception e) {

        }

        new LogicServer(logicList, port);

        try {
            sleep(500);
        } catch (Exception e) {

        }

        for (MQMessage m: logicList) {
            System.err.println(m);
        }

        closeServer(server);

    }

}
