package jlu.evyde.gobang.Client.Controller;

import com.google.gson.Gson;
import jlu.evyde.gobang.Client.Model.MQMessage;
import jlu.evyde.gobang.Client.Model.MQProtocol;
import jlu.evyde.gobang.Client.Model.MQServerAddress;

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
     * @param id Message queue source.
     * @param success Callback function when registered successfully.
     * @param failed Callback function when registered failed.
     */
    void register(MQProtocol.MQSource id, Callback success, Callback failed);

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
     * @param color Chess color to win.
     */
    void win(MQProtocol.Chess.Color color);

    /**
     * Add receive listener to this communicator.
     * When message comes in, call this listener.
     * @param crl CommunicatorReceiveListener class.
     */
    void addReceiveListener(CommunicatorReceiveListener crl);
}
