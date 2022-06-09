package jlu.evyde.gobang.Client.View;

import jlu.evyde.gobang.Client.Controller.Communicator;
import jlu.evyde.gobang.Client.Model.MQProtocol;
import jlu.evyde.gobang.Client.Model.SystemConfiguration;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Map;

public abstract class GameFrame extends JFrame {
    public Map<MQProtocol.Chess.Color, Communicator> communicatorMap;
    public MQProtocol.Chess.Color nowPlayer = SystemConfiguration.getFIRST();
    public GameFrame(Map<MQProtocol.Chess.Color, Communicator> communicatorMap) {
        super();
        this.communicatorMap = communicatorMap;
    }
    public abstract void put(MQProtocol.Chess c);
    public abstract void recall();
    public abstract void win(MQProtocol.Chess.Color c);
    public abstract void updateScore(Map<MQProtocol.Chess.Color, Integer> score);
    public class PutChessListener implements MouseListener {
        @Override
        public void mouseClicked(MouseEvent e) {
            communicatorMap.get(nowPlayer).put(new MQProtocol.Chess(toRelativePosition(e.getPoint()), getNowPlayer()));
            changePlayer();
        }

        @Override
        public void mousePressed(MouseEvent e) {}

        @Override
        public void mouseReleased(MouseEvent e) {}

        @Override
        public void mouseEntered(MouseEvent e) {}

        @Override
        public void mouseExited(MouseEvent e) {}
    }
    public abstract static class HoverChessListener implements MouseMotionListener {
        @Override
        public void mouseDragged(MouseEvent e) {}
    }
    public abstract Point toRelativePosition(Point point);
    public abstract Point toAbsolutePosition(Point point);
    public void changePlayer() {
        if (MQProtocol.Chess.Color.BLACK == getNowPlayer()) {
            nowPlayer = MQProtocol.Chess.Color.WHITE;
        } else if (MQProtocol.Chess.Color.WHITE == getNowPlayer()) {
            nowPlayer = MQProtocol.Chess.Color.BLACK;
        }
    }

    public MQProtocol.Chess.Color getNowPlayer() {
        return nowPlayer;
    }
}
