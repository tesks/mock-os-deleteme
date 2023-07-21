package jpl.gds.shared.log;

import java.io.Serializable;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.Message;

import jpl.gds.shared.message.IMessagePublicationBus;

/**
 * Custom Log4j2 appender used to forward log messages to the
 * IMessagePublicationBus implementation in the current application context.
 * 
 * Messages can not be forwarded to the bus if the Tracer being used to log with
 * does not contain the application context
 * 
 *
 */
@Plugin(name = "MessageBusAppender", category = "Core", elementType = "appender", printObject = true)
public class MessageBusAppender extends AbstractAppender {

    /**
     * @param name
     *            The name to use
     * @param filter
     *            The filter to use
     * @param layout
     *            The layout to use
     * @param ignoreExceptions
     *            Whether or not to ignore exceptions
     */
    protected MessageBusAppender(final String name, final Filter filter, final Layout<? extends Serializable> layout,
            final boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions);
    }

    /**
     * @param name
     *            The appender name from configuration
     * @param layout
     *            The appender layout from configuration
     * @param filter
     *            The appender filter from configuration
     * @return The AMPCS MessageBusAppender
     */
    @PluginFactory
    public static MessageBusAppender createAppender(@PluginAttribute("name") final String name,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter) {
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        return new MessageBusAppender(name, filter, layout, true);
    }

    @Override
    public void append(final LogEvent event) {
        final Message m = event.getMessage();
        try {
            final AmpcsLog4jMessage msg = (AmpcsLog4jMessage) m.getParameters()[0];
            if (msg.getApplicationContext() != null && event.getLevel().isMoreSpecificThan(Level.INFO)) {
                final IMessagePublicationBus bus;
                bus = msg.getApplicationContext().getBean(IMessagePublicationBus.class);
                bus.publish(msg);
            }
        } catch (final Exception e) {

        } // silently fail
    }

}
