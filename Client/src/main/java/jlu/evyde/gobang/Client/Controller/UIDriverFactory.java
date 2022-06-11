package jlu.evyde.gobang.Client.Controller;

import jlu.evyde.gobang.Client.Model.MQProtocol;
import jlu.evyde.gobang.Client.SwingController.SwingUIDriver;

public class UIDriverFactory {
    public static UIDriver getSwingUIDriver() {
        return new SwingUIDriver();
    }

    public static UIDriver getAIDriver() {
        return new AIClient();
    }

    public static UIDriver getAIDriver(MQProtocol.Chess.Color color) {
        return new AIClient(color);
    }
}
