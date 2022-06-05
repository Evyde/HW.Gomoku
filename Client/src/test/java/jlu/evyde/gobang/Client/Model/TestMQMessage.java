package jlu.evyde.gobang.Client.Model;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestMQMessage {
    @Test
    public void normalJsonTest() {
        MQMessage mm = new MQMessage();
        mm.status = "success";
        mm.code = 200;
        mm.method = "plot";
        mm.x = 3;
        mm.y = 4;
        mm.from = SystemConfiguration.MQ_Source.UI;

        assertEquals("Normal JSON Test",
                "{\"status\":\"success\",\"code\":200,\"method\":\"plot\",\"from\":\"UI\",\"x\":3,\"y\":4}",
                mm.toString()
        );
    }

    @Test
    public void allNullJsonTest() {
        MQMessage mm = new MQMessage();

        assertEquals("All null JSON test", "{\"code\":0,\"x\":0,\"y\":0}", mm.toString());
    }

    @Test
    public void toJsonTest() {
        MQMessage mm;

        mm = MQMessage.fromJson(
                "{\"status\":\"success\",\"code\":200,\"method\":\"plot\",\"x\":3,\"y\":4}"
        );

        assertNotNull(mm);
        assertEquals("success", mm.status);
        assertEquals("plot", mm.method);
        assertEquals(200, mm.code);
        assertEquals(3, mm.x);
        assertEquals(4, mm.y);
    }
}
