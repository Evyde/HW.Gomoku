package jlu.evyde.gobang.Client;

import jlu.evyde.gobang.Client.Controller.Callback;
import jlu.evyde.gobang.Client.Controller.GobangException;
import jlu.evyde.gobang.Client.Controller.UIDriverFactory;
import jlu.evyde.gobang.Client.Model.MQBrokerServer;
import jlu.evyde.gobang.Client.Model.MQBrokerServerFactory;
import jlu.evyde.gobang.Client.Model.MQServerAddress;
import jlu.evyde.gobang.Client.Model.SystemConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

import static java.lang.Thread.sleep;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static MQBrokerServer mqBrokerServer;

    private static void init() {
        try {
            UIDriverFactory.getUIDriver().initMainFrame(
                    () -> {
                        logger.info("MainFrame started.");
                    },
                    new dispose()
            );
        } catch (GobangException.FrameInitFailedException ffe) {
            logger.error("MainFrame started failed: {}", ffe.toString());
            System.exit(1);
        }



        try {
            MQServerAddress msa = new MQServerAddress();
            msa.setIsa(new InetSocketAddress(SystemConfiguration.getMQServerHost(),
                    SystemConfiguration.getMQServerPort()));
            mqBrokerServer = MQBrokerServerFactory.getWebSocketMQServer();
            mqBrokerServer.startMQBrokerServer(
                    msa,
                    () -> {
                        logger.info("Message queue server started.");
                    },
                    () -> {

                    }
            );
        } catch (GobangException.MQServerStartFailedException e) {
            logger.error("Message queue server started or connected failed, exit.");
            System.exit(2);
        }
    }

    private static class dispose implements Callback {
        volatile boolean locked = true;

        @Override
        public void run() {
            logger.warn("Exiting.");

            mqBrokerServer.closeMQBrokerServer(
                    () -> { logger.info("Closing MQ server."); },
                    () -> {
                        logger.info("MQ server closed.");
                        locked = false;
                    }
            );
            while (locked) {
                Thread.onSpinWait();
            }

            System.exit(0);
        }
    }

    public static void main(String[] args) {
        logger.warn("Starting client.");
        init();
    }
}