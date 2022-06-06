package jlu.evyde.gobang.Client.Controller;

import jlu.evyde.gobang.Client.Model.MQProtocol;
import jlu.evyde.gobang.Client.Model.SystemConfiguration;

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
     * @param complete Callback function when successfully initialized.
     * @param disposeListener Callback function when frame closed.
     * @throws GobangException.FrameInitFailedException
     */
    void initMainFrame(Callback complete, Callback disposeListener) throws GobangException.FrameInitFailedException;

    /**
     * Initialize the communicator for UI, should persistence it properly.
     * @param complete Callback function when successfully initialized.
     * @throws GobangException.UICommunicatorInitFailedException
     */
    void initUICommunicator(Callback complete) throws GobangException.UICommunicatorInitFailedException;

    /**
     * Put chess in the UI.
     * @param x Relative axis of chess.
     * @param y Relative axis of chess.
     * @param chess Kind of chess.
     */
    void put(int x, int y, MQProtocol.Chess chess);

    /**
     * Tell UI we win! :)
     */
    void win();

    /**
     * Tell UI we lose. :(
     */
    void lose();
}
