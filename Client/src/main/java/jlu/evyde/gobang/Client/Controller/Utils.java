package jlu.evyde.gobang.Client.Controller;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class Utils {
    public static String toUTF8String(String s) {
        String ss = "";
        try {
             ss = new String(s.getBytes("ISO_8859_1"), "GBK");
        } catch (UnsupportedEncodingException uee) {
            return "";
        }
        System.out.println(ss);
        return ss;
    }
}
