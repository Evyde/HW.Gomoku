package jlu.evyde.gobang.Client.Model;

import jlu.evyde.gobang.Client.Controller.Callback;
import jlu.evyde.gobang.Client.Controller.GobangException;

/**
 * Class who implement this interface must start another thread to execute its own logic.
 * Whatever the method (Socket/Websocket) the implementation uses,
 * client's connection requests should be processed properly.
 */
public interface MQBrokerServer {
    /**
     * Start message queue broker server in MQServerAddress.
     * @param msa Message queue server address.
     * @param startComplete Callback method when server started successfully.
     * @param startError Callback method when server start failed.
     * @throws GobangException.MQServerStartFailedException
     */
    void startMQBrokerServer(MQServerAddress msa, Callback startComplete, Callback startError) throws GobangException.MQServerStartFailedException;

    /**
     * Close message queue broker server immediately.
     *
     * @param beforeClose Callback method before server closed.
     * @param afterClose Callback method when server closed successfully.
     */
    void closeMQBrokerServer(Callback beforeClose, Callback afterClose);
}
