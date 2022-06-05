package jlu.evyde.gobang.Client.Controller;

public class GobangException {
    public static class FrameInitFailedException extends RuntimeException {}
    public static class UICommunicatorInitFailedException extends RuntimeException {}
    public static class MQServerStartFailedException extends RuntimeException {}
    public static class MQServerClosedFailedException extends RuntimeException {}
}
