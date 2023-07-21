package jpl.gds.db.log;

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

import jpl.gds.db.api.sql.IDbSqlArchiveController;
import jpl.gds.shared.log.AmpcsLog4jMessage;

/**
 * Custom Log4j2 appender used to forward log messages to the MySQL
 * implementation in the current application context.
 *
 */
@Plugin(name = "MySQLAppender", category = "Core", elementType = "appender", printObject = true)
public class MySQLAppender extends AbstractAppender {
    /**
     * @param name
     *            The appender name
     * @param filter
     *            The appender filter
     * @param layout
     *            The appender layout
     * @param ignoreExceptions
     *            If the appender should ignore exceptions
     */
	protected MySQLAppender(final String name, final Filter filter, final Layout<? extends Serializable> layout,
			final boolean ignoreExceptions) {
		super(name, filter, layout, ignoreExceptions);
	}

    /**
     * @param name
     *            The appender name to use from config
     * @param layout
     *            The appender layout to use from config
     * @param filter
     *            The appender filter to use from config
     * @return MySQLAppender
     */
	@PluginFactory
	public static MySQLAppender createAppender(@PluginAttribute("name") final String name,
			@PluginElement("Layout") Layout<? extends Serializable> layout,
			@PluginElement("Filter") final Filter filter) {
		if (layout == null) {
			layout = PatternLayout.createDefaultLayout();
		}
		return new MySQLAppender(name, filter, layout, true);
	}

	@Override
	public void append(final LogEvent event) {
        final Message m = event.getMessage();
        try {
            final AmpcsLog4jMessage msg = (AmpcsLog4jMessage) m.getParameters()[0];
            if (msg.getApplicationContext() != null && event.getLevel().isMoreSpecificThan(Level.INFO)) {

                final IDbSqlArchiveController dbController = msg.getApplicationContext().getBean(IDbSqlArchiveController.class);

                if (dbController.isUp()) {
                    dbController.getLogMessageStore().handleLogMessage(msg);
                }
            }
        } catch (final Exception e) {

        } // silently fail
	}

}
