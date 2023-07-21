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
package jpl.gds.telem.down.gui;


import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.perspective.DisplayConfiguration;
import jpl.gds.shared.config.ReleaseProperties;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.telem.down.IDownlinkApp;

/**
 * This is the downlink controller window specifically for flight software telemetry processing.
 *
 */
public class FSWDownShell extends AbstractDownShell {
    /**
     * Creates an instance of FSWDownShell with the given parent Display,
     * associated application class, and test configuration.
     * @param appContext the current application context
     * @param display the parent Display
     * @param app the associated instance of IDownlinkApp
     * @param tc the IContextConfiguration object
     * @param config the DisplayConfiguration object for the window
     */
    public FSWDownShell(final ApplicationContext appContext, final Display display, final IDownlinkApp app, final IContextConfiguration tc, 
            final DisplayConfiguration config) {
        super(appContext, display, app, tc, config);
    }
    
    /**
     * Creates an instance of FSWDownShell with the given parent Shell,
     * associated application class, and test configuration.
     * @param appContext the current application context
     * @param parent the parent Shell
     * @param app the associated instance of IDownlinkApp
     * @param config the IContextConfiguration object
     */
    public FSWDownShell(final ApplicationContext appContext, final Shell parent, final IDownlinkApp app, final IContextConfiguration config) {
        super(appContext, parent, app, config);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#getTitle()
     */
    @Override
	public String getTitle() {
        return ReleaseProperties.getProductLine() + " FSW " + TITLE;
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.telem.down.gui.AbstractDownShell#getBrandingImage()
     */
    @Override
    protected Image getBrandingImage() {
        return SWTUtilities.createImage(this.mainDisplay, "jpl/gds/down/gui/FSWLogo.gif");
    }
    
    // R8 Refactor - Commenting out everything related to session restart
//	/**
//	 * {@inheritDoc}
//	 * @see jpl.gds.telem.down.gui.AbstractDownShell#sessionChanged(jpl.gds.config.SessionConfiguration)
//	 */
//	@Override
//	public void sessionChanged(SessionConfiguration config) {
//		// Write out the new session config so it has the updated session number
//		AbstractDownlinkApp.writeOutSessionConfig(appContext, config, true);
//	}

	// R8 Refactor - Commenting out everything related to session restart
//	/**
//	 * {@inheritDoc}
//	 * @see jpl.gds.telem.down.gui.AbstractDownShell#canRestartSession()
//	 */
//	@Override
//	public boolean canRestartSession() {
//		return true;
//	}
}
