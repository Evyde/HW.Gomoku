package jlu.evyde.gobang.Client.Controller;

import jlu.evyde.gobang.Client.Model.MQMessage;

public abstract class LogicCommunicatorReceiveListener implements CommunicatorReceiveListener {

    @Override
    public abstract void beforeReceive();

    @Override
    public abstract void doReceive(MQMessage msg);

    @Override
    public abstract void afterReceive();
}
