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
package jpl.gds.telem.down;

import jpl.gds.common.config.TimeComparisonStrategy;
import jpl.gds.eha.api.service.channel.ISuspectChannelService;
import jpl.gds.session.config.gui.SessionConfigShell;
import jpl.gds.telem.common.app.mc.IRestfulTelemetryApp;
import jpl.gds.telem.down.gui.AbstractDownShell;

/**
 * An interface to be implemented by the downlink application.
 * 
 * @since R8
 */
public interface IDownlinkApp extends IRestfulTelemetryApp, Runnable {

    /**
     * Gets the autostart flag, indicating if processing should start automatically
     * or await user start cue.
     * 
     * @return true if autostart is set, false if not
     */
    public boolean isAutoStart();

    /**
     * Gets the autorun flag, indicating if processing should start automatically
     * or await user start cue.
     * 
     * @return true if autorun is set, false if not
     */
    public boolean isAutoRun();



    /**
     * Method to launch the application in GUI Mode. Subclass should implement
     * this and call launchGuiApp(boolean, boolean) with appropriate arguments.
     */
    public void launchGuiApp();

    /**
     * Executes the configured telemetry processing session.
     * 
     */
    // @Override
    // public void run();

    /**
     * Set the TimeComparisonStrategy for this downlink
     * 
     * @param strategy
     *            the TimeComparisonStrategy to set
     */
    public void setTimeComparisonStrategy(TimeComparisonStrategy strategy);

    /**
     * Retrieves the current TimeComparisonStrategy
     * 
     * @return the current TimeComparisonStrategy
     */
    public TimeComparisonStrategy getTimeComparisonStrategy();

    /**
     * Returns the GUI enabled flag.
     * @return true if GUI display is enabled, false otherwise.
     */
    public boolean useGui();

    /**
     * Gets a DownConfiguration object reflecting the current settings,
     * including the test configuration.
     * @return a DownConfiguration object
     */
    public DownConfiguration getDownConfiguration();


    /**
     * Gets the SuspectChannelTable object from the downlink context manager object
     * @return SuspectChannelTable, or null if none has been initialized
     */
    public ISuspectChannelService getSuspectChannelService();

    /**
     * Get the current contents of the downlink-local LAD in a VERY LARGE string
     * 
     * @return a string containing the contents of the lad as CSV
     */
    public String getLadContentsAsString();

    /**
     * Save the current contents of the downlink-local LAD to a file.
     * 
     * @param filename
     *            the file path
     * @return true if successful, false if not
     */
    public boolean saveLadToFile(String filename);

    // R8 Refactor TODO - This was all being done by the channel LAD, but it
    // can no longer access either AlarmHistory or AlarmNotifierService.  This may
    // result in strange behavior, because really clearing all of these needs to
    // be a synchronized set of actions.  On the other hand, anyone clearing EHA
    // processing state while actively flowing data is a fool. Perhaps we should 
    // just not worry about it.
    /**
     * Clears the downlink-local channel state (LAD, alarm history)
     */
    public void clearChannelState();
    
    /**
     * @return the SWT Shell for the Downlink App (FSW or SSE).
     */
    public AbstractDownShell getDownlinkShell();

    /**
     * @return the SWT Shell for the Session Configuration dialog for Downlink App (FSW or SSE)
     */
    public SessionConfigShell getConfigShell();

}