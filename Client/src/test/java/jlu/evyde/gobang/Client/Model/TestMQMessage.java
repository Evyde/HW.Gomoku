package jlu.evyde.gobang.Client.Model;

import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.*;

public class TestMQMessage {
    @Test
    public void normalJsonTest() {
        MQMessage mm = new MQMessage();
        mm.status = MQProtocol.Status.SUCCESS;
        mm.code = 200;
        mm.method = "plot";
        mm.chess = new MQProtocol.Chess(
                new Point(3, 4),
                MQProtocol.Chess.Color.BLACK
        );
        mm.group = MQProtocol.Group.GAMER;

        assertEquals("Normal JSON Test",
                "{\"status\":\"SUCCESS\",\"code\":200,\"method\":\"plot\"," +
                        "\"chess\":{\"position\":{\"x\":3,\"y\":4},\"color\":\"BLACK\"},\"group\":\"GAMER\"}",
                mm.toString()
        );
    }

    @Test
    public void allNullJsonTest() {
        MQMessage mm = new MQMessage();

        assertEquals("All null JSON test", "{}", mm.toString());
    }

    @Test
    public void toJsonTest() {
        MQMessage mm;

        mm = MQMessage.fromJson(
                "{\"status\":\"SUCCESS\",\"code\":200,\"method\":\"plot\",\"from\":\"UI\",\"chess\":{\"position\":{\"x\":3,\"y\":4},\"color\":\"BLACK\"}}"
        );

        assertNotNull(mm);
        assertEquals(MQProtocol.Status.SUCCESS, mm.status);
        assertEquals("plot", mm.method);
        assertEquals(Integer.valueOf(200), mm.code);
        assertEquals(new MQProtocol.Chess(new Point(3, 4), MQProtocol.Chess.Color.BLACK), mm.chess);
    }
}
