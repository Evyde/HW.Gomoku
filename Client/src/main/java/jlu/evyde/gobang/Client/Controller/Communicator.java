package jlu.evyde.gobang.Client.Controller;

import com.google.gson.Gson;
import jlu.evyde.gobang.Client.Model.MQServerAddress;

public interface Communicator {

    /**
     * Connect to message queue server.
     * @param msa Message queue server address.
     * @throws GobangException.UICommunicatorInitFailedException
     */
    void connect(MQServerAddress msa) throws GobangException.UICommunicatorInitFailedException;

    /**
     * Send produce request to MQ server.
     * @param message Message in json format.
     * @param sendComplete Callback method when send complete.
     * @param sendError Callback method when send error.
     */
    void produce(Gson message, Callback sendComplete, Callback sendError);

    /**
     * Send consume request to MQ server.
     * @param sendComplete Callback method when send complete.
     * @param sendError Callback method when send error.
     */
    void consume(Callback sendComplete, Callback sendError);

    /**
     * Add receive listener to this communicator.
     * When message comes in, call this listener.
     * @param crl CommunicatorReceiveListener class.
     */
    void addReceiveListener(CommunicatorReceiveListener crl);
}
