package jlu.evyde.gobang.Client.Controller;

import jlu.evyde.gobang.Client.Model.MQProtocol;

import java.util.Map;

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
     * @throws GobangException.CommunicatorInitFailedException
     */
    void initCommunicator(Callback complete) throws GobangException.CommunicatorInitFailedException;

    /**
     * Put chess in the UI.
     * @param chess Chess (with position and kind).
     */
    void put(MQProtocol.Chess chess);

    /**
     * Tell UI which color of chess wins.
     * @param color Color of chess who win.
     */
    void win(MQProtocol.Chess.Color color);

    /**
     * Let UI update score of gamer.
     * @param score Score map that should be updated.
     */
    void updateScore(Map<MQProtocol.Chess.Color, Integer> score);

    /**
     * Recall last step.
     */
    void recall();
}
