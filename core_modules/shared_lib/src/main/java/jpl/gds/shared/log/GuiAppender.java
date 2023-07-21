/*
 * Copyright 2006-2018. California Institute of Technology.
 * ALL RIGHTS RESERVED.
 * U.S. Government sponsorship acknowledged.
 *
 * This software is subject to U. S. export control laws and
 * regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 * extent that the software is subject to U.S. export control laws
 * and regulations, the recipient has the responsibility to obtain
 * export licenses or other export authority as may be required
 * before exporting such information to foreign countries or
 * providing access to foreign nationals.
 */
package jpl.gds.shared.log;

import java.io.Serializable;

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
/**
 * 
 * GuiAppender is a Tracer Appender implementation that forwards non-debug messages to
 * the GuiNotifier.
 *
 *
 */
@Plugin(name = "GuiAppender", category = "Core", elementType = "appender", printObject = true)
public class GuiAppender extends AbstractAppender {
    private static GuiAppender instance = null;

    /**
     * @param name
     *            appender name
     * @param filter
     *            appender filter
     * @param layout
     *            appender layout
     * @param ignoreExceptions
     *            if the appender should ignore exceptions
     */
    protected GuiAppender(final String name, final Filter filter, final Layout<? extends Serializable> layout, final boolean ignoreExceptions) {
 		super(name, filter, layout, ignoreExceptions);
 	}
    
    /**
     * @param name
     *            appener name
     * @param filter
     *            appender filter
     * @param layout
     *            appender layout
     * @param ignoreExceptions
     *            if the appender should ignore exceptions
     * @return custom gui appender
     */
    @PluginFactory
	protected static GuiAppender createAppender(
			@PluginAttribute("name") final String name, 
			@PluginElement("Filters") final Filter filter,
			@PluginElement("Layout") Layout<? extends Serializable> layout,
			@PluginAttribute("ignoreExceptions") final boolean ignoreExceptions )
	{
    	if (layout == null) {
			layout = PatternLayout.createDefaultLayout();
		}
		instance = new GuiAppender(name, filter, layout, ignoreExceptions);
	    return instance;
	}

    /**
     * @return The custom GuiAppender
     */
    public static GuiAppender getInstance() { 
    	return instance;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.logging.log4j.core.Appender#append(org.apache.logging.log4j.core.LogEvent)
     */
	@Override
    public synchronized void append(final LogEvent event) {
        final Message m = event.getMessage();
        final AmpcsLog4jMessage msg = (AmpcsLog4jMessage) m.getParameters()[0];
        if (msg.getApplicationContext() != null) {
            GuiNotifier notifier;
            try {
                notifier = msg.getApplicationContext().getBean(GuiNotifier.class);
                synchronized (notifier) {
                    notifier.notifyTraceListeners(msg);
                }
            } catch (final Exception e) {

            }
        }
    }

	
}
