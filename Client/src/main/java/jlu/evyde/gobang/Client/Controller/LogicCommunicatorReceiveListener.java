package jlu.evyde.gobang.Client.Controller;

import jlu.evyde.gobang.Client.Model.MQMessage;
import jlu.evyde.gobang.Client.Model.MQProtocol;

public abstract class LogicCommunicatorReceiveListener implements CommunicatorReceiveListener {

    @Override
    public abstract void beforeReceive();

    @Override
    public void doReceive(MQMessage msg) {
        if (MQProtocol.Code.AUTH.getCode().equals(msg.code)) {
            // TODO: Valid registration check.

        }
    }

    @Override
    public abstract void afterReceive();
}
