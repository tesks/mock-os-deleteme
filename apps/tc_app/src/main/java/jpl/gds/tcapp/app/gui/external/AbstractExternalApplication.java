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
package jpl.gds.tcapp.app.gui.external;

import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.springframework.context.ApplicationContext;

import jpl.gds.shared.process.DoNothingLineHandler;
import jpl.gds.shared.process.ProcessLauncher;

/**
 * Abstract class which launches an external application.
 * 
 */
public abstract class AbstractExternalApplication implements
        ExternalApplication {
    /**
	 * Process launcher which launches the external application.
	 */
    protected ProcessLauncher process;
    /**
	 * If the process is running.
	 */
    protected boolean running;
    /**
	 * Holds the menu item for the application.
	 */
    protected MenuItem menuItem;
    /**
	 * Indicates if the application is enabled
	 */
    protected boolean enabled;
    /**
	 * Indicates whether to wait for the external application to exit before proceeding
	 */
    protected boolean waitForExit;
    /**
	 * Name of the script to execute to run the application
	 */
    protected String scriptName;
	protected ApplicationContext appContext;

    /**
	 * Constructor.
	 */
    public AbstractExternalApplication(ApplicationContext appContext) {
    	this.appContext = appContext;
        this.process = null;
        this.menuItem = null;
        this.enabled = false;
        this.waitForExit = false;
        this.scriptName = null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.tcapp.app.gui.external.ExternalApplication#isRunning()
     */
    @Override
	public boolean isRunning() {
        return (this.process != null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.tcapp.app.gui.external.ExternalApplication#createMenuItem(org.eclipse.swt.widgets.Menu,
     *      org.eclipse.swt.events.SelectionListener)
     */
    @Override
	public MenuItem createMenuItem(final Menu menu,
            final SelectionListener handler) {
        if (this.menuItem != null) {
            return (this.menuItem);
        }

        this.menuItem = new MenuItem(menu, SWT.PUSH);
        this.menuItem.setText("Launch " + getName() + "...");
        this.menuItem.addSelectionListener(handler);
        return (this.menuItem);
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.tcapp.app.gui.external.ExternalApplication#getMenuItem()
     */
    @Override
	public MenuItem getMenuItem() {
        return (this.menuItem);
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.tcapp.app.gui.external.ExternalApplication#launch()
     */
    @Override
	public void launch() throws IOException {
        launch(new DoNothingLineHandler(), new DoNothingLineHandler());
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.tcapp.app.gui.external.ExternalApplication#shutdown()
     */
    @Override
	public void shutdown() {
        if (this.process == null) {
            return;
        }

        this.process.destroy();
        this.process = null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.tcapp.app.gui.external.ExternalApplication#isEnabled()
     */
    @Override
	public boolean isEnabled() {
        return enabled;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.tcapp.app.gui.external.ExternalApplication#isWaitForExit()
     */
    @Override
	public boolean isWaitForExit() {
        return waitForExit;
    }

    /**
     * Gets the script to be executed for the external application.
     * @return script name
     */
    public String getScriptName() {
        return scriptName;
    }
}
