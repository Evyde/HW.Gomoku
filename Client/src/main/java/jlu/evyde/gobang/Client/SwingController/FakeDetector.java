package jlu.evyde.gobang.Client.SwingController;

import com.jthemedetecor.OsThemeDetector;

import java.util.function.Consumer;

public class FakeDetector {

    FakeDetector() {

    }

    public boolean isDark() {
        return false;
    }

    public void registerListener(Consumer<Boolean> consumer) {

    }

    public void removeListener(Consumer<Boolean> consumer) {

    }
}
