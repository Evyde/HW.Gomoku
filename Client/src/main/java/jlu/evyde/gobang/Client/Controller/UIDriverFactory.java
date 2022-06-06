package jlu.evyde.gobang.Client.Controller;

import jlu.evyde.gobang.Client.SwingController.SwingUIDriver;

public class UIDriverFactory {
    public static UIDriver getSwingUIDriver() {
        return new SwingUIDriver();
    }

    public static UIDriver getHeadlessDriver() {
        return null;
    }
}
