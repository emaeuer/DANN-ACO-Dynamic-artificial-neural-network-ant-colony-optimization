package de.emaeuer.logging;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;

import java.nio.charset.Charset;

@Plugin(name = "SampleLayout",
        category = "Core",
        elementType = "layout",
        printObject = true)
public class GuiLoggingLayout extends AbstractStringLayout {

    protected GuiLoggingLayout(Charset charset) {
        super(charset);
    }

    @Override
    public String toSerializable(LogEvent event) {
        return event.getMessage().toString();
    }

    @PluginFactory
    public static GuiLoggingLayout createLayout(@PluginAttribute(value = "charset", defaultString = "UTF-8") Charset charset) {
        return new GuiLoggingLayout(charset);
    }
}
