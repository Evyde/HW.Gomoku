package jlu.evyde.gobang.Client.Controller;

/**
 * @author evyde
 */

public interface UIDriver {
    /**
     * Returns if system is dark.
     * @return true if system is dark.
     */
    boolean dark();

    /**
     * Initialize the main frame.
     * @param callback Callback function when successfully initialized.
     * @throws GobangException.FrameInitFailedException
     */
    void initMainFrame(Callback callback) throws GobangException.FrameInitFailedException;

    void initUICommunicator(Callback callback) throws GobangException.UICommunicatorInitFailedException;
}
