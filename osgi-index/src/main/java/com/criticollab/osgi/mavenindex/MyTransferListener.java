package com.criticollab.osgi.mavenindex;/**
 * Created by ses on 2/21/17.
 */

import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.observers.AbstractTransferListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MyTransferListener extends AbstractTransferListener {
    private static Logger logger = LoggerFactory.getLogger("Transfer:");
    int t = 0;

    public void transferStarted(TransferEvent transferEvent) {
        logger.info("  Downloading " + transferEvent.getResource().getName());
        t = 0;
    }

    public void transferProgress(TransferEvent transferEvent, byte[] buffer, int length) {
        t += length;
        if ((t % 1024 * 1024) == 0) logger.info("Fetching {} : {} ", transferEvent.getResource().getName(),
                                                String.format("%,d", t));
    }

    public void transferCompleted(TransferEvent transferEvent) {
        logger.info(" {} - Done", transferEvent.getResource());
    }
}
