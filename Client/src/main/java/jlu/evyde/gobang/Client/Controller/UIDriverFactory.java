package jlu.evyde.gobang.Client.Controller;

import jlu.evyde.gobang.Client.SwingController.SwingUIDriver;

public class UIDriverFactory {
    public static UIDriver getUIDriver() {
        return new SwingUIDriver();
    }
}
