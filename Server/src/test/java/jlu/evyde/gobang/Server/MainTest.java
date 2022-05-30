package jlu.evyde.gobang.Server;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.*;

public class MainTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @Before
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @After
    public void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    public void emptyHelloTest() {
        Main.hello("");
        assertEquals(" hello!\n", outContent.toString());
    }

    @Test
    public void normalHelloTest() {
        Main.hello("CQ");
        assertEquals("CQ hello!\n", outContent.toString());
    }

    @Test
    public void nullHelloTest() {
        Main.hello(null);
        assertEquals("null hello!\n", outContent.toString());
    }
}