package jlu.evyde.gobang.Client.Model;

import jlu.evyde.gobang.Client.Controller.Callback;
import jlu.evyde.gobang.Client.Controller.GobangException;

public class RawSocketMQBrokerServer implements MQBrokerServer {
    /**
     * Start message queue broker server in MQServerAddress.
     *
     * @param msa           Message queue server address.
     * @param startComplete Callback method when server started successfully.
     * @param startError    Callback method when server start failed.
     * @throws GobangException.MQServerStartFailedException
     */
    @Override
    public void startMQBrokerServer(MQServerAddress msa, Callback startComplete, Callback startError) throws GobangException.MQServerStartFailedException {

    }

    /**
     * Close message queue broker server immediately.
     *
     * @param beforeClose Callback method before server closed.
     * @param afterClose  Callback method when server closed successfully.
     */
    @Override
    public void closeMQBrokerServer(Callback beforeClose, Callback afterClose) {

    }
}
