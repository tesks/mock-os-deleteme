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
 * ExternalAppender is a Tracer Appender implementation that forwards non-debug
 * messages to ExternalTraceNotifier. It is used to forward messages that should
 * go to the outside world (database, message service, etc) to listeners that
 * can do that forwarding.
 * 
 *
 */
@Plugin(name="ExternalAppender", category="core", elementType="appender", printObject=true)
public class ExternalAppender extends AbstractAppender {

    private static ExternalAppender instance = null;

    /**
     * @param name
     *            Appender name
     * @param filter
     *            Appender filter
     * @param layout
     *            Appender layout
     * @param ignoreExceptions
     *            If the appender should ignore exceptions
     */
    protected ExternalAppender(final String name, final Filter filter, final Layout<? extends Serializable> layout, final boolean ignoreExceptions) {
		super(name, filter, layout, ignoreExceptions);
	}
    
    /**
     * @param name
     *            The appender name from log4j2 configuration
     * @param filter
     *            The appender filter from log4j2 configuration
     * @param layout
     *            The appender layout from log4j2 configuration
     * @param ignoreExceptions
     *            Whether or not the appender should ignore exceptions
     * @return ExternalAppender
     */
    @PluginFactory
	protected static ExternalAppender createAppender(
			@PluginAttribute("name") final String name, 
			@PluginElement("Filters") final Filter filter,
			@PluginElement("Layout") Layout<? extends Serializable> layout,
			@PluginAttribute("ignoreExceptions") final boolean ignoreExceptions )
	{
    	if (layout == null) {
			layout = PatternLayout.createDefaultLayout();
		}

		instance = new ExternalAppender(name, filter, layout, ignoreExceptions);
	    return instance;
	}
    
    /**
     * @return an instance of the custom External Appender
     */
    public static ExternalAppender getInstance() {
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
        if (null != msg.getApplicationContext()) {
            ExternalTraceNotifier notifier;
            try {
                notifier = msg.getApplicationContext().getBean(ExternalTraceNotifier.class);
                synchronized (notifier) {
                    notifier.notifyTraceListeners(msg);
                }
            } catch (final Exception e) {

            }
        }
	}
	
}
