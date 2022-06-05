package jlu.evyde.gobang.Client.Controller;

import com.google.gson.Gson;

public interface CommunicatorReceiveListener {
    void beforeReceive();
    void afterReceive(Gson receivedJson);
}
