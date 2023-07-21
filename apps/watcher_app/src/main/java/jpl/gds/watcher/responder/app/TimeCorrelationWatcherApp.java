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
package jpl.gds.watcher.responder.app;

import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.session.message.SessionMessageType;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.ExitWithSessionOption;
import jpl.gds.time.api.message.TimeCorrelationMessageType;
import jpl.gds.watcher.IResponderAppHelper;
import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;

/**
 * This is the application helper class for a message responder that watches
 * for time correlation messages from FSW and SSE and calculates their
 * differences, displaying onto the terminal.
 */
public class TimeCorrelationWatcherApp implements IResponderAppHelper {
    private static final String APP_NAME = ApplicationConfiguration.getApplicationName(ResponderAppName.TIME_CORRELATION_WATCHER_APP_NAME.getAppName());

    private boolean exitWithSession = false;
	private final ApplicationContext appContext;
	private final ExitWithSessionOption exitOption = new ExitWithSessionOption();

	/**
	 * Constructor.
	 * 
	 * @param appContext the current application context
	 */
	public TimeCorrelationWatcherApp(final ApplicationContext appContext) {
		this.appContext = appContext;
	}
	

    @Override
    public void addAppOptions(final BaseCommandOptions opts) {
        opts.addOption(exitOption);
    }

 
    @Override
    public void configure(final ICommandLine commandLine)
            throws ParseException {

        // auto exit with context
        this.exitWithSession = exitOption.parse(commandLine);

    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.watcher.IResponderAppHelper#getAdditionalHelpText()
     */
    @Override
    public String getAdditionalHelpText() {
        return "\n";
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.watcher.IResponderAppHelper#getOverrideTypes()
     */
    @Override
    public String[] getOverrideTypes() {
        return new String[] { 
                TimeCorrelationMessageType.FswTimeCorrelation.getSubscriptionTag(),
                TimeCorrelationMessageType.SseTimeCorrelation.getSubscriptionTag(), 
                SessionMessageType.StartOfSession.getSubscriptionTag(),
                SessionMessageType.SessionHeartbeat.getSubscriptionTag(),
                SessionMessageType.EndOfSession.getSubscriptionTag() };
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.watcher.IResponderAppHelper#getContextConfiguration()
     */
    @Override
    public IContextConfiguration getContextConfiguration() {
        return null;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.watcher.IResponderAppHelper#getUsageText()
     */
    @Override
    public String getUsageText() {
        final StringBuilder sb = new StringBuilder("Usage: " + APP_NAME + " [session options] [jms options] [database options]\n");
        sb.append("                                      [--printLog --exitWithSession]\n");
        sb.append("      " + APP_NAME + " --topics <topic-list> [jms options] [database options]\n");
        sb.append("                                     [--printLog --exitWithSession]\n");
    
        return sb.toString();
     }

    /**
     * Gets the flag indicating whether the application should exit when the
     * session ends.
     * @return true if the application should exit when an end of session
     *         message is received
     */
    public boolean isExitSession() {
        return this.exitWithSession;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.watcher.IResponderAppHelper#setContextConfiguration(IContextConfiguration)
     */
    @Override
    public void setContextConfiguration(final IContextConfiguration context) {
        // do nothing
    }
    

	@Override
	public ApplicationContext getApplicationContext() {
		return this.appContext;
	}

}
