package jlu.evyde.gobang.Client.Model;

import jlu.evyde.gobang.Client.Controller.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Thread.sleep;

public class LogicServer {
    private final UIDriver uiDriver;
    private Communicator communicator = CommunicatorFactory.getWebSocketCommunicator();
    private Logger logger = LoggerFactory.getLogger(LogicServer.class);

    public LogicServer(UIDriver uid) {
        uiDriver = uid;
        try {
            communicator.addReceiveListener(new LogicCommunicatorReceiveListener() {
                @Override
                public void beforeReceive() {
                    logger.info("Receiving message.");
                }

                @Override
                public void afterReceive() {
                    logger.info("Receive complete.");
                }
            });
            communicator.connect(new MQServerAddress());
            Integer counter = SystemConfiguration.getMaxRetryTime();
            while (!communicator.connected()) {
                if (counter-- <= 0) {
                    logger.error("Could not start logic server.");
                }
                sleep(SystemConfiguration.getSleepTime());
            }
            communicator.register(MQProtocol.MQSource.LOGIC,
                    () -> { logger.info("Logic server started."); },
                    () -> {
                        logger.error("Register to MQ failed.");
                        throw new GobangException.UICommunicatorInitFailedException();
                    });
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Logic server may start failed.");
            throw new GobangException.LogicCommunicatorInitFailedException();
        }
    }
}
