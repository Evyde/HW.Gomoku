package jlu.evyde.gobang.Client;

import jlu.evyde.gobang.Client.Controller.*;
import jlu.evyde.gobang.Client.Model.*;
import jlu.evyde.gobang.Client.SwingController.SwingUIDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
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
                        stageLock = false;
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
        ls = new LogicServer();
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
                        System.exit(0);
                        sleep(SystemConfiguration.getSleepTime());
                    }
                    Thread.onSpinWait();
                }
                System.exit(0);
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


        logger.warn("Stage 2: Initialize message queue server.");
        initMQServer();
        while (stageLock) {
            Thread.onSpinWait();
        }
        logger.warn("Stage 3: Initialize logic server.");
        initLogicServer();

        logger.warn("Stage 4: Initialize UI server.");
        initUIServer();
        
        logger.warn("Stage 5: Initialize AI client.");
//        initAIClient(MQProtocol.Chess.Color.WHITE);
//        try {
//            sleep(5000);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
        initAIClient(MQProtocol.Chess.Color.BLACK);
        logger.warn("Stage 1: Initialize main frame.");
        initMainFrame();
        while (stageLock) {
            Thread.onSpinWait();
        }
        logger.warn("Initialized successfully.");
    }

    private static void initAIClient(MQProtocol.Chess.Color color) {
        UIDriver ai = UIDriverFactory.getAIDriver(color);
        ai.initMainFrame(() -> {}, () -> {});
        ai.initCommunicator(() -> {logger.warn("AI started.");});
        if (color.equals(SystemConfiguration.getFIRST())) {
            try {
                sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            ai.put(new MQProtocol.Chess(new Point(), color));
        }
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