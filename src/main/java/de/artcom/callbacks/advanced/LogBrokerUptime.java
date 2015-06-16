package de.artcom.callbacks.advanced;

import com.dcsquare.hivemq.spi.services.SYSTopicService;
import com.dcsquare.hivemq.spi.topic.sys.SYSTopicEntry;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a scheduled callback, which will be called in the specified interval. The callback is independent
 * of MQTT messages and other stuff. The purpose is to do maintenance stuff or publish stuff regularly.
 *
 * In this example the callback is used to log the uptime of the broker.
 *
 * @author Christian Goetz
 */
public class LogBrokerUptime implements com.dcsquare.hivemq.spi.callback.schedule.ScheduledCallback {

    private static final Logger log = LoggerFactory.getLogger(LogBrokerUptime.class);
    private final SYSTopicService sysTopicService;

    @Inject
    public LogBrokerUptime(final SYSTopicService sysTopicService) {
        this.sysTopicService = sysTopicService;
    }

    /**
     * This method is called regularly depending on the given interval.
     */
    @Override
    public void execute() {
        final Optional<SYSTopicEntry> entry = sysTopicService.getEntry("$SYS/broker/uptime");

        if (entry.isPresent()) {
            log.info("Uptime is {}", new String(entry.get().payload().get(), Charsets.UTF_8));
        }

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
        // Every 50 seconds
        return "50 * * * * ?";
    }
}
