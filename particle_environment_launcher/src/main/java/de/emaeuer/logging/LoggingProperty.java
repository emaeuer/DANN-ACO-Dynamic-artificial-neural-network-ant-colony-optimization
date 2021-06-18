package de.emaeuer.logging;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Plugin(
        name = "LoggingProperty",
        category = "Core",
        elementType = "appender"
)
public class LoggingProperty extends AbstractAppender {

    private static final Lock LOCK = new ReentrantLock();

    private static final Queue<String> NEW_LOG_ENTRIES = new LinkedBlockingQueue<>();

    protected LoggingProperty(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions, Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);
    }

    public static Collection<String> retrieveNewLogEntries() {
        LOCK.lock();
        try {
            List<String> newLogEntries = new ArrayList<>(NEW_LOG_ENTRIES);
            NEW_LOG_ENTRIES.clear();
            return newLogEntries;
        } finally {
            LOCK.unlock();
        }
    }

    @Override
    public void append(LogEvent event) {
        LOCK.lock();
        try {
            NEW_LOG_ENTRIES.add(new String(getLayout().toByteArray(event)));
        } catch (Exception ex) {
            if (!ignoreExceptions()) {
                throw new AppenderLoggingException(ex);
            }
        } finally {
            LOCK.unlock();
        }
    }

    @PluginFactory
    public static LoggingProperty createAppender(
            @PluginAttribute("name") String name,
            @PluginAttribute("ignoreExceptions") boolean ignoreExceptions,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filters") Filter filter,
            @PluginElement("Properties") Property[] properties) {

        if (name == null) {
            LOGGER.error("No name provided for TextAreaAppender");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        return new LoggingProperty(name, filter, layout, true, properties);
    }

}
