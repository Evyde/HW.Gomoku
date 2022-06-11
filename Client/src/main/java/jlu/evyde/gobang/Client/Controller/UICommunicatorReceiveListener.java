package jlu.evyde.gobang.Client.Controller;

import jlu.evyde.gobang.Client.Model.MQMessage;
import jlu.evyde.gobang.Client.Model.MQProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public abstract class UICommunicatorReceiveListener implements CommunicatorReceiveListener {
    public final MQProtocol.Group group;
    public final UIDriver uiDriver;
    private static final Logger logger = LoggerFactory.getLogger(UICommunicatorReceiveListener.class);

    public UICommunicatorReceiveListener (UIDriver uid) {
        this.group = MQProtocol.Group.GAMER;
        this.uiDriver = uid;
    }

    @Override
    public void doReceive(MQMessage msg) {
        if (msg != null) {
            if (msg.code != null) {
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
                        uiDriver.win(msg.chess);
                    }
                } else if (MQProtocol.Code.DRAW.getCode().equals(msg.code)) {
                    uiDriver.draw();
                } else if (MQProtocol.Code.RESET.getCode().equals(msg.code)) {
                    uiDriver.reset();
                } else if (MQProtocol.Code.CLEAR_SCORE.getCode().equals(msg.code)) {
                    Map<MQProtocol.Chess.Color, Integer> clearScoreMap =
                            new EnumMap<MQProtocol.Chess.Color, Integer>(MQProtocol.Chess.Color.class);
                    for (MQProtocol.Chess.Color c: MQProtocol.Chess.Color.values()) {
                        clearScoreMap.put(c, 0);
                    }
                    uiDriver.updateScore(clearScoreMap);
                } else if (MQProtocol.Code.END_GAME.getCode().equals(msg.code)) {
                    uiDriver.exit();
                } else if (MQProtocol.Code.TALK.getCode().equals(msg.code)) {
                    uiDriver.talk(msg.msg);
                } else if (MQProtocol.Code.UPDATE_SCORE.getCode().equals(msg.code)) {
                    if (msg.score != null) {
                        uiDriver.updateScore(msg.score);
                    }
                }
            }
            if (MQProtocol.Status.FAILED.equals(msg.status)) {
                logger.warn(msg.toJson());
            }
        }
    }
}
