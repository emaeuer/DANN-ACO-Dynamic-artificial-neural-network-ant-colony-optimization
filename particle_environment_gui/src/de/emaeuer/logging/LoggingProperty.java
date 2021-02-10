package de.emaeuer.logging;

import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Plugin(
        name = "LoggingProperty",
        category = "Core",
        elementType = "appender"
)
public class LoggingProperty extends AbstractAppender {

    private static final StringProperty LOG_TEXT = new SimpleStringProperty("");

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();

    protected LoggingProperty(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions, Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);
    }

    public static void bindToLog(StringProperty property) {
        property.bind(LOG_TEXT);
    }

    public static void addListener(InvalidationListener listener) {
        LOG_TEXT.addListener(listener);
    }

    @Override
    public void append(LogEvent event) {
        readLock.lock();
        try {
            String log = new String(getLayout().toByteArray(event));
            System.out.println(log);
            LOG_TEXT.setValue(LOG_TEXT.getValue() + log);
        } catch (Exception ex) {
            if (!ignoreExceptions()) {
                throw new AppenderLoggingException(ex);
            }
        } finally {
            readLock.unlock();
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
