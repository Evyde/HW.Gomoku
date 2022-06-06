package jlu.evyde.gobang.Client.Controller;

public class GobangException {
    public static class FrameInitFailedException extends RuntimeException {}
    public static class CommunicatorInitFailedException extends RuntimeException {}
    public static class UICommunicatorInitFailedException extends CommunicatorInitFailedException {}
    public static class LogicCommunicatorInitFailedException extends CommunicatorInitFailedException {}
    public static class MQServerStartFailedException extends RuntimeException {}
    public static class MQServerClosedFailedException extends RuntimeException {}
}
