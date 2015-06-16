package de.artcom.callbacks.advanced;

import com.dcsquare.hivemq.spi.message.RetainedMessage;
import com.dcsquare.hivemq.spi.services.RetainedMessageStore;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * This is a scheduled callback, which will be called in the specified interval. The callback is independent
 * of MQTT messages and other stuff. The purpose is to do maintenance stuff or publish stuff regularly.
 *
 * In this example the callback is used to clear all retained messages every 30 seconds.
 *
 * @author Christian Goetz
 */
public class ScheduledClearRetainedCallback implements com.dcsquare.hivemq.spi.callback.schedule.ScheduledCallback {

    private static final Logger log = LoggerFactory.getLogger(ScheduledClearRetainedCallback.class);
    private final RetainedMessageStore retainedMessageStore;

    @Inject
    public ScheduledClearRetainedCallback(final RetainedMessageStore retainedMessageStore) {
        this.retainedMessageStore = retainedMessageStore;
    }

    @Override
    public void execute() {
        final Set<RetainedMessage> retainedMessages = retainedMessageStore.getRetainedMessages();
        for (RetainedMessage retainedMessage : retainedMessages) {
            retainedMessageStore.remove(retainedMessage);
        }
        log.info("All retained messages have been cleared!");
    }

    /**
     * This method returns the quartz-like cron expression for the callback.
     * <p/>
     * Note that this method only gets called once when adding the callback to the
     * {@link com.dcsquare.hivemq.spi.callback.registry.CallbackRegistry}. If you have dynamic
     * cron expressions in this method, you must manually call the
     * {@link com.dcsquare.hivemq.spi.callback.registry.CallbackRegistry#reloadScheduledCallbackExpression(ScheduledCallback)}
     * method in order to reload the expression
     *
     * @return a String which contains the quartz-like cron expressions.
     * @see <a href="http://quartz-scheduler.org/api/2.2.0/org/quartz/CronExpression.html">
     *      Documentation for quartz cron expressions</a>
     */
    @Override
    public String cronExpression() {
        // Every 30 seconds
        return "0/30 * * * * ?";
    }
}
