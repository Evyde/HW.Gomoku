package jlu.evyde.gobang.Client.Controller;

import jlu.evyde.gobang.Client.Model.MQMessage;

public interface CommunicatorReceiveListener {
    void beforeReceive();
    void doReceive(MQMessage msg);
    void afterReceive();
}