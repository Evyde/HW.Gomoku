package jlu.evyde.gobang.Client.SwingController;

import java.awt.*;
import java.io.OutputStream;
import java.io.PrintStream;
import javax.swing.*;
import javax.swing.text.*;

/*
  These codes are from Internet, sources are in JavaDocs below
 */

/**
 * 输出到文本组件的流。
 *
 * @author Chen Wei
 * &#064;website   www.chenwei.mobi
 * &#064;email   chenweionline@hotmail.com
 */
public class GUIPrintStream extends PrintStream {

    private final JTextPane component;

    private final Color c;
    private static volatile boolean locked;

    public GUIPrintStream(OutputStream out, JTextPane component) {
        super(out);
        float[] hsbCC = new float[3];
        if (out == System.err) {
            Color.RGBtoHSB(255, 85, 84, hsbCC);
        } else {
            Color.RGBtoHSB(47, 200, 100, hsbCC);
        }
        c = Color.getHSBColor(hsbCC[0], hsbCC[1], hsbCC[2]);
        // c = Color.GREEN;
        this.component = component;
    }

    /**
     * &#064;source  <a href="https://stackoverflow.com/questions/9650992/how-to-change-text-color-in-the-jtextarea">...</a>
     */
    private void appendToPane(JTextPane tp, String msg, Color c) {
        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c);

        int len = tp.getDocument().getLength();
        tp.setCaretPosition(len);
        tp.setCharacterAttributes(aset, false);
        tp.replaceSelection(msg);
    }

    /**
     * &#064;source  <a href="https://stackoverflow.com/questions/9650992/how-to-change-text-color-in-the-jtextarea">...</a>
     */
    private void print(JTextPane tp, String msg, Color c) {
        AttributeSet attributes = StyleContext.getDefaultStyleContext()
                .addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c);

        try {
            tp.getStyledDocument().insertString(tp.getDocument().getLength(), msg, attributes);
        } catch (BadLocationException ignored) { }
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        final String message = new String(buf, off, len);

        while (locked) {
            Thread.onSpinWait();
        }
        locked = true;
        component.setEditable(true);
        if (component.getText().split("\n").length >= 100) {
            component.setText("");
        }
        print(component, message, c);
        component.setEditable(false);
        locked = false;
    }
}

