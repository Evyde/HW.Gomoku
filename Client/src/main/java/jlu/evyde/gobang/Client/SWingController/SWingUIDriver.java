package jlu.evyde.gobang.Client.SWingController;

import jlu.evyde.gobang.Client.Controller.Callback;
import jlu.evyde.gobang.Client.Controller.GobangException;
import jlu.evyde.gobang.Client.Controller.UIDriver;

public class SWingUIDriver implements UIDriver {

    /**
     * Returns if system is dark.
     *
     * @return true if system is dark.
     */
    @Override
    public boolean dark() {
        return false;
    }

    /**
     * Initialize the main frame.
     *
     * @param callback Callback function when successfully initialized.
     * @throws GobangException.FrameInitFailedException
     */
    @Override
    public void initMainFrame(Callback callback) throws GobangException.FrameInitFailedException {

    }

    @Override
    public void initUICommunicator(Callback callback) throws GobangException.UICommunicatorInitFailedException {

    }
}
