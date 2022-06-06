package jlu.evyde.gobang.Client.View;

import jlu.evyde.gobang.Client.Model.MQProtocol;

import javax.swing.*;

public abstract class GameFrame extends JFrame {
    public abstract void put(MQProtocol.Chess c);
    public abstract void recall();
    public abstract void win(MQProtocol.Chess.Color c);
}
