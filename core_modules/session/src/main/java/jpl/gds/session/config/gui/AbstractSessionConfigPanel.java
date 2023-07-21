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
package jpl.gds.session.config.gui;

import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Shell;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.connection.ConnectionProperties;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * This is an Abstract class for extension by the various sub-panels
 * (composites) that contain the content of the session configuration window.
 * 
 */

public abstract class AbstractSessionConfigPanel implements ISessionConfigPanel {

	private final SessionConfigShell configShell;
	private final Shell parent;
	private SessionConfiguration sessionConfig;
    private ExpandItem expandItem;
	
	/**
	 * The current ApplicationContext object.
	 */
	protected ApplicationContext appContext;

	/**
	 * The current ConnectionProperties object, extracted from the context
	 */
	protected ConnectionProperties connectProps;
	/**
	 * The current MissionProperties object, extracted from the context
	 */
	protected MissionProperties missionProps;

    protected final SseContextFlag   sseFlag;

	/**
	 * Constructor. Note that this will not initialize the actual GUI
	 * components. To do that, call enableBar().
	 * 
	 * @param appContext the current ApplicationContext object
	 * 
	 * @param parentWindow
	 *            The parent session configuration GUI object
	 * @param session
	 *            The SessionConfiguration object containing initial data for
	 *            display
	 */
	public AbstractSessionConfigPanel(final ApplicationContext appContext,
			final SessionConfigShell parentWindow,
			final SessionConfiguration session) {

		this.appContext = appContext;
		this.missionProps = appContext.getBean(MissionProperties.class);
		this.connectProps = appContext.getBean(ConnectionProperties.class);
		this.configShell = parentWindow;
		this.parent = parentWindow.getShell();
		this.sessionConfig = session;
        this.sseFlag = appContext.getBean(SseContextFlag.class);
	}

    /**
     * {@inheritDoc}
     * @see jpl.gds.session.config.gui.ISessionConfigPanel#setSessionConfiguration(jpl.gds.session.config.SessionConfiguration)
     */
    @Override
	public void setSessionConfiguration(final SessionConfiguration sc) {

		this.setSessionConfig(sc);
	}

	/**
	 * Gets the parent SessionConfigShell.
	 * 
	 * @return SessionConfigShell that owns this panel
	 */
	protected SessionConfigShell getConfigShell() {

		return this.configShell;
	}

	/**
	 * Gets the parent Shell object.
	 * 
	 * @return parent Shell
	 */
	protected Shell getParent() {

		return this.parent;
	}

	/**
	 * Gets the current session configuration object.
	 * 
	 * @return session configuration
	 */
	protected SessionConfiguration getSessionConfig() {

		return this.sessionConfig;
	}

	/**
	 * Sets the current session configuration object.
	 * 
	 * @param sessionConfig
	 *            the session configuration to set
	 */
	protected void setSessionConfig(final SessionConfiguration sessionConfig) {

		this.sessionConfig = sessionConfig;
	}


	/**
	 * Gets the ExpandItem that contains the panel Composite.
	 * 
	 * @return expandItem
	 */
	protected ExpandItem getExpandItem() {

		return expandItem;
	}

	/**
	 * Sets the ExpandItem that contains the panel Composite.
	 * 
	 * @param expandItem ExpandItem to set
	 */
	protected void setExpandItem(final ExpandItem expandItem) {

		this.expandItem = expandItem;
	}


}