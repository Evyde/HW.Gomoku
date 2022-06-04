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
 * @website  www.chenwei.mobi
 * @email  chenweionline@hotmail.com
 */
public class GUIPrintStream extends PrintStream {

    private final JTextPane component;

    private final Color c;

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
     * @source <a href="https://stackoverflow.com/questions/9650992/how-to-change-text-color-in-the-jtextarea">...</a>
     */
    private void appendToPane(JTextPane tp, String msg, Color c) {
        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c);

        aset = sc.addAttribute(aset, StyleConstants.FontFamily, "Lucida Console");
        aset = sc.addAttribute(aset, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED);

        int len = tp.getDocument().getLength();
        tp.setCaretPosition(len);
        tp.setCharacterAttributes(aset, false);
        tp.replaceSelection(msg);
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        final String message = new String(buf, off, len);

        if (component.getText().split("\n").length >= 100) {
            component.setText("");
            component.setSize(new Dimension(100, 10));
        }
        component.setEditable(true);
        appendToPane(component, message, c);
        component.setEditable(false);
    }
}

