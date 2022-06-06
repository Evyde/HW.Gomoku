package jlu.evyde.gobang.Client.Controller;

import jlu.evyde.gobang.Client.Model.MQMessage;
import jlu.evyde.gobang.Client.Model.MQProtocol;

public abstract class UICommunicatorReceiveListener implements CommunicatorReceiveListener {
    public final MQProtocol.MQSource mqSource;
    public final UIDriver uiDriver;

    public UICommunicatorReceiveListener (UIDriver uid) {
        this.mqSource = MQProtocol.MQSource.UI;
        this.uiDriver = uid;
    }

    public void doReceive(MQMessage msg) {
        beforeReceive();
        if (msg != null) {
            if (msg.from != null) {
                if (mqSource.canConsume(msg.from)) {
                    if (MQProtocol.Code.PUT_CHESS.getCode().equals(msg.code)) {
                        // Put chess
                        if (msg.chess != null) {
                            uiDriver.put(msg.chess);
                        }
                    } else if (MQProtocol.Code.RECALL.getCode().equals(msg.code)) {
                        uiDriver.recall();
                    } else if (MQProtocol.Code.WHITE_WIN.getCode().equals(msg.code) || MQProtocol.Code.BLACK_WIN.getCode().equals(msg.code)) {
                        // Win!
                        if (msg.chess != null && msg.chess.getColor() != null) {
                            uiDriver.win(msg.chess.getColor());
                        }
                    }
                }
            }
        }
        afterReceive();
    }

    public abstract void beforeReceive();

    public abstract void afterReceive();
}
