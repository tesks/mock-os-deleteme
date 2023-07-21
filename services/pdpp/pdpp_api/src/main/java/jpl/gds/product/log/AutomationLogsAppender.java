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
package jpl.gds.product.log;

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
import org.springframework.stereotype.Component;

import jpl.gds.product.automation.hibernate.dao.ProductAutomationLogsDAO;
import jpl.gds.shared.log.AmpcsLog4jMessage;

/**
 * Appender for product automation.  Adds messages to the Logs table of the automation database.
 *
 * MPCS-8180 - 07/20/2016 - Imported to and updated for AMPCS M20 adaptation
 */
@Plugin(name = "AutomationLogsAppender", category = "Core", elementType = "appender", printObject = true)
@Component
public class AutomationLogsAppender extends AbstractAppender {
    /**
     * @param name
     * @param filter
     * @param layout
     * @param ignoreExceptions
     */
	protected AutomationLogsAppender(final String name, final Filter filter, final Layout<? extends Serializable> layout, final boolean ignoreExceptions) {
		super(name, filter, layout, ignoreExceptions);
	}
	
    /**
     * @param name
     * @param filter
     * @param layout
     * @param ignoreExceptions
     * @return
     */
	@PluginFactory
	public static AutomationLogsAppender createAppender(
			@PluginAttribute("name") final String name, 
			@PluginElement("Filters") final Filter filter,
			@PluginElement("Layout") Layout<? extends Serializable> layout,
			@PluginAttribute("ignoreExceptions") final boolean ignoreExceptions )
	{	
		if (layout == null) {
			layout = PatternLayout.createDefaultLayout();
		}

		return new AutomationLogsAppender(name, filter, layout, ignoreExceptions);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.apache.logging.log4j.core.Appender#append(org.apache.logging.log4j.core.LogEvent)
	 */
	@Override
	public void append(final LogEvent event) {
        final Message m = event.getMessage();
        final AmpcsLog4jMessage msg = (AmpcsLog4jMessage) m.getParameters()[0];
        if (null != msg.getApplicationContext()) {
            try {
                msg.getApplicationContext().getBean(ProductAutomationLogsDAO.class).addLogMessage(msg);
            } catch (final Exception e) {

            } // silently fail
        }
	}
}