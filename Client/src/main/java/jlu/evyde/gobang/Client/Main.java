package jlu.evyde.gobang.Client;

import jlu.evyde.gobang.Client.Controller.Callback;
import jlu.evyde.gobang.Client.Controller.GobangException;
import jlu.evyde.gobang.Client.Controller.UIDriver;
import jlu.evyde.gobang.Client.Controller.UIDriverFactory;
import jlu.evyde.gobang.Client.Model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

import static java.lang.Thread.sleep;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static MQBrokerServer mqBrokerServer;
    private static final UIDriver uid = UIDriverFactory.getSwingUIDriver();
    private static LogicServer ls;
    private static volatile boolean stageLock = true;

    private static void initMainFrame() {
        try {
            uid.initMainFrame(
                    () -> {
                        logger.info("MainFrame started.");
                    },
                    new dispose()
            );
        } catch (GobangException.FrameInitFailedException ffe) {
            logger.error("MainFrame started failed: {}", ffe.toString());
            System.exit(1);
        }
    }

    private static void initMQServer() {
        try {
            MQServerAddress msa = new MQServerAddress();
            msa.setIsa(new InetSocketAddress(SystemConfiguration.getMQServerHost(),
                    SystemConfiguration.getMQServerPort()));
            mqBrokerServer = MQBrokerServerFactory.getWebSocketMQServer();
            mqBrokerServer.startMQBrokerServer(
                    msa,
                    () -> {
                        logger.info("Message queue server started.");
                        stageLock = false;
                    },
                    () -> {
                        logger.error("Message queue server start failed, exit.");
                        System.exit(2);
                    }
            );
        } catch (GobangException.MQServerStartFailedException e) {
            logger.error("Message queue server started or connected failed, exit.");
            System.exit(3);
        }
    }

    private static void initLogicServer() {
        ls = new LogicServer(uid);
    }

    private static class dispose implements Callback {
        volatile boolean locked = true;

        @Override
        public void run() {
            logger.warn("Exiting.");

            try {

                mqBrokerServer.closeMQBrokerServer(
                        () -> {
                            logger.info("Closing MQ server.");
                        },
                        () -> {
                            logger.info("MQ server closed.");
                            locked = false;
                        }
                );
                Integer counter = SystemConfiguration.getMaxRetryTime();
                while (locked) {
                    if (counter-- <= 0) {
                        locked = false;
                        sleep(SystemConfiguration.getSleepTime());
                    }
                    Thread.onSpinWait();
                }
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Failed to exit, exit anyway.");
                System.exit(4);
            }

            System.exit(0);
        }
    }

    public static void main(String[] args) {
        logger.info("Starting client.");

        logger.warn("Stage 1: Initialize message queue server.");
        initMQServer();
        while (stageLock) {
            Thread.onSpinWait();
        }
        logger.warn("Stage 2: Initialize logic server.");
        initLogicServer();
        logger.warn("Stage 3: Initialize UI server.");
        initUIServer();
        // logger.warn("Stage 4: Initialize main frame.");
        // initMainFrame();
        logger.warn("Initialized successfully.");
    }

    private static void initUIServer() {
        stageLock = true;
        uid.initCommunicator(() -> {
            stageLock = false;
            logger.info("UI server started.");
        });
        try {
            Integer counter = SystemConfiguration.getMaxRetryTime();
            while (stageLock) {
                if (counter-- <= 0) {
                    logger.error("Could not start UI server.");
                }
                sleep(SystemConfiguration.getSleepTime());
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.warn("UI server may start failed.");
        }
    }
}