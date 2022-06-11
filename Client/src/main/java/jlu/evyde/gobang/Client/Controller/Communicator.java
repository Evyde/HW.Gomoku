package jlu.evyde.gobang.Client.Controller;

import jlu.evyde.gobang.Client.Model.MQMessage;
import jlu.evyde.gobang.Client.Model.MQProtocol;
import jlu.evyde.gobang.Client.Model.MQServerAddress;

import java.util.EnumMap;

public interface Communicator {

    /**
     * Connect to message queue server.
     * @param msa Message queue server address.
     * @throws GobangException.UICommunicatorInitFailedException
     */
    void connect(MQServerAddress msa) throws GobangException.UICommunicatorInitFailedException;

    /**
     * Returns connection status.
     * @return true if is connected.
     */
    boolean connected();

    /**
     * Register this communicator to MQ server.
     * @param message Message to register.
     * @param success Callback function when registered successfully.
     * @param failed Callback function when registered failed.
     */
    void register(MQMessage message, Callback success, Callback failed);

    /**
     * Send produce request to MQ server.
     * @param message Message in json format.
     * @param sendComplete Callback method when send complete.
     * @param sendError Callback method when send error.
     */
    void produce(MQMessage message, Callback sendComplete, Callback sendError);

    /**
     * Put chess to MQ server.
     * @param chess Chess object that put.
     */
    void put(MQProtocol.Chess chess);

    /**
     * Push recall last step message to MQ server.
     */
    void recall();

    /**
     * Let the specific color of chess to win.
     * @param chess Chess to win.
     */
    void win(MQProtocol.Chess chess);

    /**
     * Clear score in server.
     */
    void clearScore();

    /**
     * Let UI update score of gamer.
     * @param score Score map that should be updated.
     */
    void updateScore(EnumMap<MQProtocol.Chess.Color, Integer> score);

    /**
     * Let logic server send redirect message to MQ server for authentication.
     * Should be called by Logic Server ONLY.
     * @param message Message to redirect.
     * @param sendComplete Callback method when send complete.
     * @param sendError    Callback method when send error.
     */
    void redirect(MQMessage message, Callback sendComplete, Callback sendError);

    /**
     * Add receive listener to this communicator.
     * When message comes in, call this listener.
     * @param crl CommunicatorReceiveListener class.
     */
    void addReceiveListener(CommunicatorReceiveListener crl);

    /**
     * Destroy this communicator.
     */
    void close();

    /**
     * Let communicator send only to prevent from multiple win message.
     * @param sendOnly
     */
    void setSendOnly(boolean sendOnly);

    /**
     * Let communicator read only to prevent from duplicated operation.
     * @param readOnly
     */
    void setReadOnly(boolean readOnly);

    /**
     * Let this game draw.
     */
    void draw();

    /**
     * Restart this game.
     */
    void restartGame();

    /**
     * Reset all things except for score.
     */
    void reset();

    /**
     * Tell logic server to end game immediately.
     */
    void endGame();

    /**
     * Say something to every one.
     * @param message Thing to talk.
     */
    void talk(String message);
}
