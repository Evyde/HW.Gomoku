package jlu.evyde.gobang.Client.Controller;

import jlu.evyde.gobang.Client.SWingController.SWingUIDriver;

public class UIDriverFactory {
    public static UIDriver getUIDriver() {
        return new SWingUIDriver();
    }
}
