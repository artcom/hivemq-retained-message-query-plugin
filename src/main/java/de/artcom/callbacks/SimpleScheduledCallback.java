package de.artcom.callbacks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christian Goetz
 */
public class SimpleScheduledCallback implements com.dcsquare.hivemq.spi.callback.schedule.ScheduledCallback {

    private static final Logger log = LoggerFactory.getLogger(SimpleScheduledCallback.class);

    @Override
    public void execute() {
        log.info("Scheduled Callback is doing maintenance!");
    }

    @Override
    public String cronExpression() {
        // Every 5 seconds
        return "0/5 * * * * ?";
    }
}
