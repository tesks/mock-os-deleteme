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

import jpl.gds.common.config.GeneralProperties;
import jpl.gds.common.config.connection.ConnectionProperties;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.types.DownlinkStreamType;
import jpl.gds.common.swt.AboutUtility;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.swt.ChillShell;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.xml.validation.XmlValidationException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static jpl.gds.shared.exceptions.ExceptionTools.rollUpMessages;

/**
 * The class is the Session Configuration GUI window, in which the user can
 * enter/modify parameters in the session configuration. This window must handle
 * every type of session: integrated with or without SSE, stand-alone flight
 * downlink, stand-alone SSE downlink, or stand-alone uplink with or without
 * SSE. In addition, many of the session configuration parameters are
 * inter-related, with the set of allowed values for one parameter being
 * dependent upon the value of another parameter. Finally, the configurations
 * supported and the set of allowed values for many parameters is
 * mission-specific. These facts make the implementation of this GUI very
 * complex. <br>
 * 
 * The first GUI Tab displays the session information. The GUI display presented
 * here also allows the user to view/modify message service and database-related parameters.
 * These are not part of the session configuration (can be classified as
 * application configuration) but they are presented by this class using a
 * second GUI Tab. <br>
 * 
 * Each Tab contains an ExpandBar. The ExpandBars are populated with GUI
 * sub-panels that enclose a set of related GUI fields. The sub-panels include a
 * general session information panel, a venue panel, a flight software panel, an
 * SSE panel, a message service panel, and a database panel. These panels are implemented as
 * SWT Groups in separate GUI classes. <br>
 * 
 * The basic startup flow is: Create GUI -> Initialize Fields to Defaults ->
 * Populate from Session Configuration Object. While the user is modifying the
 * GUI display, NOTHING is written to the Session Configuration Object. When the
 * user elects to wither save his configuration or run the session, the flow is:
 * Validate Fields -> Populate Session Configuration Object -> Save Session
 * Configuration/Return Session Configuration to Caller. It is very important
 * that these flows NOT be violated by updating the Session Configuration Object
 * in GUI event handlers or modifying GUI fields in methods meant only to update
 * the Session Configuration Object. <br>
 * 
 * This code was produced by performing a complete refactor of GUI code written
 * by a variety of developers. It used to support both read-write and read-only
 * views. It does no longer. This is the modifiable view of the session
 * configuration. The read-only view is now implemented by the
 * SessionConfigViewShell class. <br>
 * 
 * This code is effective in AMPCS R6.
 * 
 */
public class SessionConfigShell extends Object implements ChillShell {

	private final Tracer trace; 


	// GUI shell width
	private static final int SHELL_WIDTH = 850;

	// GDS Schema location
	private static final String GDS_SCHEMA_LOC = "/schema/SessionConfigFile.rnc";
	
	private static final String CONFIG_MISMATCH = "Configuration Mismatch";

	// Holds the SWT display device for the shell.
	private final Display parentDisplay;

	// The main shell object for the whole GUI.
	private Shell configShell;

	// Tabs
	private Composite firstTab;
	private Composite secondTab;
	private TabFolder tabFolder;

	// Expand Bars
	private ExpandBar jmsGroupBar;
	private ExpandBar sessionGroupBar;

	// Sub-panels - the session panel does not need to be a member
	// but these do. The list is to store all sub-panel objects
	// so they can be looped through.
	private SessionConfigFswComposite fswComposite;
	private SessionConfigSseComposite sseComposite;
	private SessionConfigMessageServiceDbComposite jmsDbComposite;
	private SessionConfigVenueComposite venueComposite;
	private final List<ISessionConfigPanel> subPanels = new LinkedList<>();

	// Button and menu controls
	private Composite buttonComposite;
	private MenuItem saveMenuItem = null;
	private MenuItem loadMenuItem = null;
	private MenuItem exitMenuItem = null;
	private MenuItem runMenuItem = null;
	private MenuItem aboutMenuItem = null;
	private Button runSessionButton;
	private Button exitButton;

	// Flags to track window exit state
	private boolean cancelClicked;
	private boolean runClicked;

	// The title of this window
	private String title;

	// The internal Session Configuration representation
	private SessionConfiguration sessionConfig;

	// Handlers for GUI events
	private final MenuItemAndBottomButtonSelectionHandler menuHandler = new MenuItemAndBottomButtonSelectionHandler();

	// When this is false, don't show uplink fields
	private final boolean showUplink;

	// When this is false, don't show downlink fields
	private final boolean showDownlink;

	// The configuration file where this session configuration is saved
	private String configFile;

	// Flag indicating whether Message Service/DB settings can be edited.
	private final boolean allowJmsDbEdits;

	// Flag to suppress the resize/layout event storm while the GUI
	// is still being created or when loading a new session configuration.
	private boolean suppressPacks = true;

	// Flag to suppress the reversion of GUI fields to default values.
	private boolean suppressDefaulting = false;
	
	private final ApplicationContext appContext;
	private final MissionProperties missionProps;
    private final SseContextFlag                          sseFlag;


	/**
	 * Creates a Session Configuration shell.
	 * 
	 * @param appContext  the current ApplicationContext object
	 * 
	 * @param display
	 *            SWT display to create the shell to
	 * @param showUplink
	 *            whether to show the uplink portion of the GUI
	 * @param showDownlink
	 *            whether to show the downlink portion of the GUI
	 * @param allowJmsDbMods
	 *            whether to allow Message Service/DB fields to be modified
     * @param sc
     *            Session configuration (may be null)
	 */
    public SessionConfigShell(final ApplicationContext   appContext,
    		                  final Display              display,
                              final boolean              showUplink,
                              final boolean              showDownlink,
                              final boolean              allowJmsDbMods,
                              final SessionConfiguration sc)
    {

		super();
		
		this.appContext = appContext;
        trace = TraceManager.getDefaultTracer(appContext);
		this.missionProps = this.appContext.getBean(MissionProperties.class);
        this.sseFlag = appContext.getBean(SseContextFlag.class);

		this.allowJmsDbEdits = allowJmsDbMods;
		this.parentDisplay = display;
		this.cancelClicked = false;
		this.runClicked = false;
		this.showDownlink = showDownlink;
		this.showUplink = showUplink;

		this.title = "New Session Configuration for "
                + GdsSystemProperties.getSystemMissionIncludingSse(sseFlag.isApplicationSse()).toUpperCase();

        this.sessionConfig = sc;
        
		try {
			createGui();
		} catch (final Exception e) {
			trace.error("Error creating components for the configuration GUI", e);
		}
		
		setSessionConfiguration(this.sessionConfig);
	}


	/**
	 * Creates a Session Configuration shell. Message service/DB fields will not be
	 * modifiable if this constructor is used.
	 * 
	 * @param appContext  the current ApplicationContext object
	 * 
	 * @param display
	 *            SWT display to create the shell to
	 * @param showUplink
	 *            whether to show the uplink portion of the GUI
	 * @param showDownlink
	 *            whether to show the downlink portion of the GUI
     * @param sc
     *            Session configuration (may be null)
	 */
    public SessionConfigShell(final ApplicationContext   appContext,
    		                  final Display              display,
                              final boolean              showUplink,
                              final boolean              showDownlink,
                              final SessionConfiguration sc)
    {

		this(appContext, display, showUplink, showDownlink, false, sc);
	}


	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.swt.ChillShell#getShell()
	 */
	@Override
	public Shell getShell() {

		return (this.configShell);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.swt.ChillShell#getTitle()
	 */
	@Override
	public String getTitle() {

		return (this.title);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.swt.ChillShell#open()
	 */
	@Override
	public void open() {

		this.runClicked = false;
		this.cancelClicked = false;
		this.configShell.open();
	}

    /**
     * Returns whether or not the "Run Session" button was clicked
     * 
     * @return true if Run Session button was selected
     */
    public boolean wasRunClicked() {
        return (this.runClicked);
    }

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.swt.ChillShell#wasCanceled()
	 */
	@Override
	public boolean wasCanceled() {

		return (this.cancelClicked);
	}

    /**
     * @param b
     *            true to indicate Session Config shell was cancelled, false to indicate that it was accepted.
     */
    public void setCancelled(final boolean b) {
        this.cancelClicked = b;
    }

	/**
	 * Indicates whether this session configuration GUI was created by a
	 * stand-alone flight software downlink application.
	 * 
	 * @return true if stand-alone FSW chill_down; false if not
	 */
	public boolean isStandaloneChillDown() {
        return !this.showUplink && !sseFlag.isApplicationSse() && !GdsSystemProperties.isIntegratedGui();
	}

	/**
	 * Sets data in the current session configuration object from the current
	 * GUI fields and returns the session configuration.
	 * 
	 * @return current session configuration, or creates a new one if none is
	 *         currently assigned to this object
	 */
	public SessionConfiguration getSessionConfiguration() {

		return (this.sessionConfig);
	}

	/**
	 * Sets the current session configuration object and configures the GUI
	 * based on the new configuration.
	 * 
	 * @param tc
	 *            the session configuration to assign
	 */
	private void setSessionConfiguration(final SessionConfiguration tc) {

		this.sessionConfig = tc;

		// Set the session configuration into all the sub-panels
		for (final ISessionConfigPanel panel : this.subPanels) {
			panel.setSessionConfiguration(this.sessionConfig);
		}

		// Because we will be changing all the combo boxes and things at once,
		// setting the GUI fields from the new session configuration results
		// in a storm of window redraws that looks really disturbing under
		// GTK/RedHat Linux. So suppress all the resize events while setting
		// fields and re-enable them when done, then manually trigger the
		// redraw once.
		this.suppressPacks = true;
		setFieldsFromData();
		this.suppressPacks = false;

		resizeAndPackLater();

	}

	/**
	 * Resizes and packs the window "now", i.e., without posting an asynchronous
	 * event.
	 */
	public void resizeAndPackNow() {
		this.configShell.layout(true);
		this.configShell.setMinimumSize(SHELL_WIDTH, 0);
		this.configShell.pack();
		this.configShell.setMinimumSize(this.configShell.getSize());
	}

	/**
	 * Lays out GUI fields, packs them, and resizes the whole window to reflect
	 * new content. Does nothing if the "suppressPacks" flag is currently
	 * enabled. Note that this is implemented as an SWT async exec, so this
	 * method will return before the operation completes. In short, the request
	 * will be posted to the event loop.
	 */
	public void resizeAndPackLater() {

		if (this.suppressPacks) {
			return;
		}

		if (this.configShell.isDisposed()) {
			return;
		}
		
		// An event is posted to the SWT event loop to do this because
		// it must happen after all other GUI events have
		// been completed. Otherwise, the resize may come too early
		// and additional changes may mess up the window again
		this.configShell.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {

				if(getFirstTabBar().isDisposed()) {
					return;
				}
				
				for (final ExpandItem item : getFirstTabBar().getItems()) {
					item.setHeight(item.getControl().computeSize(SWT.DEFAULT,
							SWT.DEFAULT, true).y);
				}
				/*
				 * Pack will call computeSize();
				 */
				getFirstTab().pack(true);
				getTabFolder().pack(true);
				SessionConfigShell.this.buttonComposite.pack(true);
				resizeAndPackNow();
			}
		});
	}

	/**
	 * Enables or disables downlink-related fields in the venue panel. Provided
	 * by this class so that not all sub-panels have to have direct access to
	 * the venue panel.
	 * 
	 * @param enable
	 *            true to enabled downlink venue fields, false to disable
	 */
	public void enableFlightDownlinkFields(final boolean enable) {

		this.venueComposite.enableFlightDownlinkFields(enable);
	}

	
	/**
	 * Master method for creating all the GUI controls and sub-panels.
	 */
	private void createGui() {

		// Configure the main shell
		this.configShell = new Shell(this.parentDisplay, SWT.DIALOG_TRIM
				| SWT.MIN | SWT.APPLICATION_MODAL);

		this.configShell.setText(getTitle());
		this.configShell.setLocation(0, 25);
		this.configShell.setMinimumSize(850, 0);
		final FormLayout shellLayout = new FormLayout();
		shellLayout.spacing = 10;
		shellLayout.marginHeight = 5;
		shellLayout.marginWidth = 5;
		this.configShell.setLayout(shellLayout);
		this.configShell.addShellListener(new ShellEventHandler());

		// Add menu items
		createMenus();

		// Create the first tab, which shows session, venue, FSW, and SSE info
		getFirstTab();

		// Add the sub-panels to the first tab

		// Session
		final SessionConfigInfoComposite sessionComposite = new SessionConfigInfoComposite(
				this.appContext, this, this.sessionConfig);
		sessionComposite.createGui(getFirstTabBar());
		this.subPanels.add(sessionComposite);

		// Venue
		this.venueComposite = new SessionConfigVenueComposite(this.appContext,
				this,
				this.sessionConfig, this.showDownlink, this.showUplink);
		this.venueComposite.createGui(getFirstTabBar());
		this.subPanels.add(this.venueComposite);

		// FSW - Not enabled if this session configuration is for a standalone
		// SSE downlink instance
		this.fswComposite = new SessionConfigFswComposite(this.appContext, this,
				this.sessionConfig, this.showDownlink, this.showUplink);
        if (!sseFlag.isApplicationSse()) {
			this.fswComposite.enable(true, getFirstTabBar());
		}
		this.subPanels.add(this.fswComposite);
		this.venueComposite.setFswPanel(this.fswComposite);

		// SSE - If this mission supports SSE. Not enabled if this session
		// configuration is for a standalone FSW downlink instance or if
        // SSE may not be run integrated.
        // SSE is suppressed when the mission supports SSE, but not in integrated
        // chill.

		if (missionProps.missionHasSse() && ! missionProps.disallowIntegratedSse())
        {
			this.sseComposite = new SessionConfigSseComposite(this.appContext, this,
					this.sessionConfig, this.showDownlink, this.showUplink);
			this.sseComposite.enable(!this.isStandaloneChillDown(),
					getFirstTabBar());
			this.subPanels.add(this.sseComposite);
			this.venueComposite.setSsePanel(this.sseComposite);
		}

		// Create the buttons at the bottom of the window
		createButtons();

		// Create second tab and its sub-panel, which shows the JMS and database
		// settings
		getSecondTab();

		this.jmsDbComposite = new SessionConfigMessageServiceDbComposite(this.appContext, this,
				this.sessionConfig, this.allowJmsDbEdits);
		this.jmsDbComposite.createGui(getSecondTabBar());
		this.subPanels.add(this.jmsDbComposite);
		this.venueComposite.setMessageServiceDbPanel(this.jmsDbComposite);

		// Set default values into all GUI fields
		setDefaultFieldValues();

		// First set hosts/ports from the global HostConfiguration
		restoreNetworkSettings();

		// Update default hosts/ports for FSW based upon venue settings, unless
		// this is a standalone SSE downlink instance
        if (!sseFlag.isApplicationSse()) {
			this.venueComposite.restoreFswDefaultsForVenueAndConnection();
		}

		// Update default hosts/ports for SSE based upon venue settings, if the
		// mission supports SSE but SSE is not suppressed.
        // SSE is suppressed when the mission supports SSE, but not in integrated
        // chill.

		if (sessionConfig.getMissionProperties().missionHasSse() && ! sessionConfig.getMissionProperties().disallowIntegratedSse())
        {
			this.venueComposite.restoreSseDefaultsForVenueAndConnection();
		}


		// Now that the initial GUI is drawn, enable resize/pack, which has been
		// disabled since construction
		this.suppressPacks = false;
	}

	/**
	 * Restores host/port settings in the FSW and SSE panels to the defaults in
	 * the global HostConfiguration.
	 */
	private void restoreNetworkSettings() {

		this.fswComposite.restoreNetworkSettings();
		if (this.sseComposite != null) {
			this.sseComposite.restoreNetworkSettings();
		}
	}

	/**
	 * Sets content of all GUI fields from the current session configuration
	 * object. Never do anything in this method or those it calls that modifies
	 * the current session configuration object.
	 */
	private void setFieldsFromData() {

		/*
		 * Set flag to disable reset of hosts and
		 * ports to defaults during the load stage. The incoming configuration
		 * has priority.
		 */
		suppressDefaulting = true;

		// Set GUI fields from data in all sub-panels.
		for (final ISessionConfigPanel panel : this.subPanels) {
			panel.setFieldsFromData();
		}

		// Set the window title from the configuration file
		// name, if one is present
		this.configFile = this.sessionConfig.getConfigFile();
		if (this.configFile != null) {
			this.title = this.configFile;
			this.configShell.setText(this.configFile);
		}

		/*
		 * Post event to enable reset of hosts and
		 * ports to defaults during editing.
		 */
		asyncEnableDefaulting();
	}

	/**
	 * Posts an event to enable defaulting of hosts and ports.
	 * 
	 */
	private void asyncEnableDefaulting() {
		// An event is posted to the SWT event loop to do this because
		// it must happen after all other GUI events have
		// been completed. Otherwise, the event may come too early
		// and additional changes may mess up the window again
		this.configShell.getDisplay().asyncExec(new Runnable() {

			@Override
			public void run() {
				suppressDefaulting = false;
			}
		});

	}

	/**
	 * Indicates whether defaulting of host and port fields is temporarily
	 * suppressed.
	 * 
	 * @return true if defaulting of hosts and ports is suppressed, false if
	 *         not.
	 * 
	 */
	public boolean isSuppressDefaulting() {
		return this.suppressDefaulting;
	}

	/**
	 * Clears/resets to default all fields in the current session configuration
	 * object that are written in the XML output and may not otherwise be
	 * properly reset by setDataFromFields() because they are associated with
	 * dynamic GUI elements that may or may not currently exist.
	 */
	private void clearDynamicAttributes(final SessionConfiguration tc) {

		/*
		 * Input file, subtopic, and topic should be
		 * set to null, not the empty string.
		 */
		tc.getVenueConfiguration().setTestbedName(null);

		tc.getVenueConfiguration().setDownlinkStreamId(DownlinkStreamType.NOT_APPLICABLE);

		tc.getGeneralInfo().setSubtopic(null);
		tc.getFilterInformation().setDssId(null);
		tc.getFilterInformation().setVcid(null);

		//  Clear time-related fields from previous sessions.
		tc.clearFieldsForNewConfiguration();
	}

	/**
	 * Sets all GUI fields to default values.
	 */
	private void setDefaultFieldValues() {

		// Set GUI fields to defaults in all sub-panels
		for (final ISessionConfigPanel panel : this.subPanels) {
			panel.setDefaultFieldValues();
		}

	}



	/**
	 * Creates or returns the first ExpandBar, which is associated with the
	 * first GUI tab (the session configuration tab). If the first expand bar
	 * has not been created, this method creates it. Otherwise, it just returns
	 * the existing object
	 * 
	 * @return the session ExpandBar
	 */
	private ExpandBar getFirstTabBar() {

		if (this.sessionGroupBar == null) {
			this.sessionGroupBar = new ExpandBar(getFirstTab(), SWT.NONE);

			final FontData groupFontData = new FontData(
					SessionConfigShellUtil.DEFAULT_FACE,
					SessionConfigShellUtil.DEFAULT_FONT_SIZE, SWT.BOLD);
			final Font groupFont = new Font(this.configShell.getDisplay(),
					groupFontData);

			this.sessionGroupBar.setFont(groupFont);

			final FormData fd2 = new FormData();
			fd2.top = new FormAttachment(0);
			fd2.left = new FormAttachment(0);
			fd2.right = new FormAttachment(100);
			fd2.bottom = new FormAttachment(100);
			this.sessionGroupBar.setLayoutData(fd2);

			this.sessionGroupBar.addExpandListener(new PanelExpandHandler(
					this.sessionGroupBar));
		}
		return this.sessionGroupBar;
	}

	/**
	 * Creates or gets the TabFolder that holds both tabs that make up this GUI
	 * window. If the tab folder has not been created, this method creates it.
	 * Otherwise, it just returns the object is has already created.
	 * 
	 * @return the one and only tab folder
	 */
	private TabFolder getTabFolder() {

		if (this.tabFolder == null) {
			this.tabFolder = new TabFolder(this.configShell, SWT.NONE);

			final FormLayout fl = new FormLayout();
			fl.spacing = 10;
			fl.marginHeight = 5;
			fl.marginWidth = 5;
			this.tabFolder.setLayout(fl);
			final FormData fd1 = new FormData();
			fd1.left = new FormAttachment(0);
			fd1.right = new FormAttachment(100);
			fd1.top = new FormAttachment(0);
			this.tabFolder.setLayoutData(fd1);
		}
		return this.tabFolder;
	}

	/**
	 * Creates or gets the first tab's composite in the window, which is the
	 * session configuration tab. If the composite has not been created, this
	 * method creates it. Otherwise, it just returns the object is has already
	 * created.
	 * 
	 * @return the session composite
	 */
	private Composite getFirstTab() {

		if (this.firstTab == null) {
			final TabItem tabItem = new TabItem(getTabFolder(), SWT.NONE);
			tabItem.setText("Session Config");

			this.firstTab = new Composite(getTabFolder(), SWT.NONE);
			final FormLayout fl = new FormLayout();
			fl.spacing = 10;
			fl.marginHeight = 5;
			fl.marginWidth = 5;
			this.firstTab.setLayout(fl);
			final FormData fd1 = new FormData();
			fd1.left = new FormAttachment(0);
			fd1.right = new FormAttachment(100);

			this.firstTab.setLayoutData(fd1);

			tabItem.setControl(this.firstTab);
		}
		return this.firstTab;
	}

	/**
	 * Creates or gets the second tab's composite in the window, which is the
	 * message service/DB configuration tab. If the composite has not been created, this
	 * method creates it. Otherwise, it just returns the object is has already
	 * created.
	 * 
	 * @return the message service/DB composite
	 */
	private Composite getSecondTab() {

		if (this.secondTab == null) {
			final TabItem tabItem = new TabItem(getTabFolder(), SWT.NONE);
			tabItem.setText("Database/Message Service Config");

			this.secondTab = new Composite(getTabFolder(), SWT.NONE);
			final FormLayout fl = new FormLayout();
			fl.spacing = 10;
			fl.marginHeight = 5;
			fl.marginWidth = 5;
			this.secondTab.setLayout(fl);
			final FormData fd1 = new FormData();
			fd1.top = new FormAttachment(0);
			fd1.left = new FormAttachment(0);
			fd1.right = new FormAttachment(100);
			fd1.bottom = new FormAttachment(100);

			this.secondTab.setLayoutData(fd1);

			tabItem.setControl(this.secondTab);
		}
		return this.secondTab;
	}

	/**
	 * Creates or returns the second ExpandBar, which is associated with the
	 * second GUI tab (the Message service/DB configuration tab). If the first expand bar
	 * has not been created, this method creates it. Otherwise, it just returns
	 * the existing object
	 * 
	 * @return the message service/DB ExpandBar
	 */
	private ExpandBar getSecondTabBar() {

		if (this.jmsGroupBar == null) {
			this.jmsGroupBar = new ExpandBar(getSecondTab(), SWT.NONE);

			final FontData groupFontData = new FontData(
					SessionConfigShellUtil.DEFAULT_FACE,
					SessionConfigShellUtil.DEFAULT_FONT_SIZE, SWT.BOLD);
			final Font groupFont = new Font(this.configShell.getDisplay(),
					groupFontData);

			this.jmsGroupBar.setFont(groupFont);

			final FormData fd2 = new FormData();
			fd2.left = new FormAttachment(0);
			fd2.right = new FormAttachment(100);
			this.jmsGroupBar.setLayoutData(fd2);

			this.jmsGroupBar.addExpandListener(new PanelExpandHandler(
					this.jmsGroupBar));
		}
		return this.jmsGroupBar;
	}

	/**
	 * Creates the window menu and menu items (at the top of the window).
	 */
	private void createMenus() {

		final Menu menuBar = new Menu(this.configShell, SWT.BAR);

		final Menu fileMenu = new Menu(this.configShell, SWT.DROP_DOWN);
		final Menu helpMenu = new Menu(this.configShell, SWT.DROP_DOWN);

		final MenuItem fileMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
		fileMenuHeader.setText("File");
		fileMenuHeader.setMenu(fileMenu);

		this.saveMenuItem = new MenuItem(fileMenu, SWT.PUSH);
		this.saveMenuItem.setText("Save Config...");
		this.saveMenuItem.addSelectionListener(this.menuHandler);

		this.loadMenuItem = new MenuItem(fileMenu, SWT.PUSH);
		this.loadMenuItem.setText("Load Config...");
		this.loadMenuItem.addSelectionListener(this.menuHandler);

		new MenuItem(fileMenu, SWT.SEPARATOR);

		this.runMenuItem = new MenuItem(fileMenu, SWT.PUSH);
		this.runMenuItem.setText("Run Session");
		this.runMenuItem.addSelectionListener(this.menuHandler);

		this.exitMenuItem = new MenuItem(fileMenu, SWT.PUSH);
		this.exitMenuItem.setText("Exit");

		this.exitMenuItem.addSelectionListener(this.menuHandler);

		final MenuItem helpMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
		helpMenuHeader.setText("Help");
		helpMenuHeader.setMenu(helpMenu);

		this.aboutMenuItem = new MenuItem(helpMenu, SWT.PUSH);
		this.aboutMenuItem.setText("About...");
		this.aboutMenuItem.addSelectionListener(this.menuHandler);

		this.configShell.setMenuBar(menuBar);
	}

	/**
	 * Creates the window buttons (at the bottom of the window).
	 */
	private void createButtons() {

		this.buttonComposite = new Composite(this.configShell, SWT.NONE);
		final FormLayout fl = new FormLayout();
		fl.spacing = 5;
		fl.marginHeight = 0;
		fl.marginWidth = 0;
		this.buttonComposite.setLayout(fl);
		final FormData fd1 = new FormData();
		fd1.left = new FormAttachment(0);
		fd1.right = new FormAttachment(100);
		fd1.bottom = new FormAttachment(100);
		fd1.top = new FormAttachment(getTabFolder(), 10);
		this.buttonComposite.setLayoutData(fd1);

		this.runSessionButton = new Button(this.buttonComposite, SWT.PUSH);
		this.runSessionButton.setText("Run Session");
		final FormData fd5 = new FormData();
		fd5.top = new FormAttachment(0);
		fd5.left = new FormAttachment(15);
		fd5.right = new FormAttachment(40);
		this.runSessionButton.setLayoutData(fd5);
		this.runSessionButton.addSelectionListener(this.menuHandler);

		this.exitButton = new Button(this.buttonComposite, SWT.PUSH);
		this.exitButton.setText("Exit");

		final FormData fd6 = new FormData();
		fd6.top = new FormAttachment(0);
		fd6.left = new FormAttachment(60);
		fd6.right = new FormAttachment(85);
		this.exitButton.setLayoutData(fd6);
		this.exitButton.addSelectionListener(this.menuHandler);
	}

    /**
     * Simulate pushing the EXIT button (for remote control).
     * 
     * @return true if button pressed successfully, false if not
     */
    public boolean pushExitButton() {
        final AtomicBoolean ok = new AtomicBoolean(false);
        this.configShell.getDisplay().syncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    SessionConfigShell.this.exitButton.notifyListeners(SWT.Selection, new Event());
                    ok.set(true);
                }
                catch (final Exception e) {
                    // ignore
                }
            }
        });
        return ok.get();
    }

    /**
     * Simulate pushing the EXIT button (for remote control).
     * 
     * @return true if button pressed successfully, false if not
     */
    public boolean pushRunSessionButton() {
        final AtomicBoolean ok = new AtomicBoolean(false);
        this.configShell.getDisplay().syncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    SessionConfigShell.this.runSessionButton.notifyListeners(SWT.Selection, new Event());
                    ok.set(true);
                }
                catch (final Exception e) {
                    // ignore
                }
            }
        });
        return ok.get();
    }

	/**
	 * This class implements an event handler for menu item and button events.
	 * They are combined because the menu items mirror the capability of some of
	 * the buttons.
	 * 
	 */
	private class MenuItemAndBottomButtonSelectionHandler implements
			SelectionListener {

		private final SWTUtilities util = new SWTUtilities();

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
		 */
		@Override
		public void widgetDefaultSelected(final SelectionEvent event) {

			// Empty method for interface only
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
		 */
		@Override
		public void widgetSelected(final SelectionEvent event) {

			try {
				internalWidgetSelected(event);
			} catch (final Exception e) {
				TraceManager.getDefaultTracer().error(

						"Error in SessionConfigShell.widgetSelected handling: "
								+ rollUpMessages(e), e);

			}
		}

		/**
		 * Actual selection event processing.
		 * 
		 * @param event
		 *            the SWT SelectionEvent that triggered this call
		 */
		private void internalWidgetSelected(final SelectionEvent event) {

			if (event.getSource() == SessionConfigShell.this.loadMenuItem) {

				// User elected to load a session configuration file
				// Get a file path to load from
				final String filename = this.util.displayStickyFileChooser(
						false, SessionConfigShell.this.configShell,
						"loadMenuItem");

				if (filename != null) {

					//  validate schema before load
					final File userFile = new File(filename);
					if (!userFile.exists() ) {
					       SWTUtilities.showErrorDialog(SessionConfigShell.this.configShell, "File Not Found",
                                   "The specified file " + filename +   "does not exist.");                                              
                                                                                                     
				           return;
					}
					// validate user supplied config file against schema
					final String gdsConfigDir = GdsSystemProperties.getGdsDirectory();
					final File schemaFile = new File(gdsConfigDir + GDS_SCHEMA_LOC);
					if (!schemaFile.exists()) {
					       SWTUtilities.showErrorDialog(SessionConfigShell.this.configShell, " Session Schema File Not Found",
                                   "Unable to locate the session configuration schema file " + gdsConfigDir + GDS_SCHEMA_LOC );    
					       return;
					}
					try {
						final boolean pass = IContextConfiguration.schemaVsConfigFileCheck(
								schemaFile, userFile);
						if (!pass) {
							SWTUtilities.showErrorDialog(SessionConfigShell.this.configShell, " Invalid Session Configuration File",
					         filename + " does not match the schema definition");
						     return;
						}
					} catch (final XmlValidationException ve) {
						SWTUtilities.showErrorDialog(SessionConfigShell.this.configShell, " Schema Validation Error",
						         filename + " is not a valid xml file");
						return;
					}
					// load the current session configuration object by parsing
					// the XML
					/*
					 * Load into a temporary object and
					 * determine whether the loaded session configuration is 
					 * appropriate for the current configuration before using it.
					 */
					final SessionConfiguration tempConfig = new SessionConfiguration(appContext.getBean(MissionProperties.class),
					        appContext.getBean(ConnectionProperties.class), true);

					final boolean ok = tempConfig.load(filename);
					
                    if (ok) {
                    	if (showUplink && tempConfig.isDownlinkOnly()) {
                     		SWTUtilities.showErrorDialog(SessionConfigShell.this.configShell, CONFIG_MISMATCH,
                       			 "Supplied session configuration file is for a\n" +
                  	                 "downlink-only configuration and cannot\nbe used by this application");
                       		return;
                       		
                    	} else if (showDownlink && tempConfig.isUplinkOnly()) {
                    		SWTUtilities.showErrorDialog(SessionConfigShell.this.configShell, CONFIG_MISMATCH,
                    			 "Supplied session configuration file is for\n" +
               	                 "an uplink-only configuration and cannot\nbe used by this application");
                    		return;
                    		
                        }
                        else if (!sseFlag.isApplicationSse() && tempConfig.isSseDownlinkOnly()) {
                    		SWTUtilities.showErrorDialog(SessionConfigShell.this.configShell, CONFIG_MISMATCH,
                       			 "Supplied session configuration file is for\n" +
                  	                 "an SSE-only configuration and cannot\nbe used by this application");
                    		return;
                    	}
                    
					
					    sessionConfig.copyValuesFrom(tempConfig);
					    
						// Set the session configuration into all the sub-panels
						for (final ISessionConfigPanel panel : subPanels) {
							panel.setSessionConfiguration(sessionConfig);
						}

						/**
						 * Packs cannot be suppressed
						 * here. Although the screen spasms horribly as a
						 * result, there is no way to get the screen to resize
						 * properly in all situations without letting the pack
						 * execute over and over. I hate SWT.
						 */
						// set values for new session
						sessionConfig.clearFieldsForNewConfiguration();

						setFieldsFromData();
						resizeAndPackLater();

						// Reset the window title
						SessionConfigShell.this.title = filename;
						SessionConfigShell.this.configShell.setText(filename);


					} else {

						// Could not load the file. Note we do nothing to say
						// why. Need to fix this
						// in the load method somehow.
						SWTUtilities.showErrorDialog(
								SessionConfigShell.this.configShell,
								"Load Failed",
								"Could not load configuration file.");
					}
				}

			} else if (event.getSource() == SessionConfigShell.this.saveMenuItem) {

				// User elected to save a session configuration file
				// Get a file path to save to
				String filename = SessionConfigShell.this.sessionConfig.getConfigFile();

				// This gets rid of a funky file browser bug under RedHat
				// linux
				if (filename != null && !filename.equals("")) {
					final int index = filename.lastIndexOf(File.separator);
					if (index != -1) {
						filename = filename.substring(index + 1);
					}
				} else {
					filename = null;
				}

				filename = this.util.displayStickyFileSaver(
						SessionConfigShell.this.configShell,
						"SessionConfigShell", null, filename);

				if (filename != null) {

					SessionConfigShell.this.configFile = filename;

					// Validate the content first. No sense saving a bad file.
					if (!validateInputFields()) {
						return;
					}
					// Update the current session configuration object from GUI
					// fields.
					setDataFromFields();

					try {
						// Save session configuration to file as XML
						SessionConfigShell.this.sessionConfig.save();

						// Update the window title to reflect current file name
						SessionConfigShell.this.title = filename;
						SessionConfigShell.this.configShell
								.setText(SessionConfigShell.this.title);

					} catch (final IOException ex) {
						SWTUtilities
								.showErrorDialog(
										SessionConfigShell.this.configShell,
										"Save Error",
										"Could not save configuration due to IO error.");
					}
				}
			} else if (event.getSource() == SessionConfigShell.this.runSessionButton
					|| event.getSource() == SessionConfigShell.this.runMenuItem) {

				// User elected to run the session

				SessionConfigShell.this.runSessionButton.setFocus();
				SessionConfigShell.this.runClicked = true;

				// Validate GUI inputs before proceeding
				if (!validateInputFields()) {
					SessionConfigShell.this.runClicked = false;
					return;
				}
				// Updated the current session configuration object, and global
				// message service and DB configuration, from the GUI fields
				setDataFromFields();
				SessionConfigShell.this.jmsDbComposite
						.setJmsConfigurationFields();
				SessionConfigShell.this.jmsDbComposite
						.setDbConfigurationFields();
			    SessionConfigShell.this.jmsDbComposite
                    .setLadConfigurationFields();

				// Exit the window
				SessionConfigShell.this.configShell.close();

			} else if ((event.getSource() == SessionConfigShell.this.exitButton
					|| event.getSource() == SessionConfigShell.this.exitMenuItem) && actuallyCloseShell(event)) {

				// User elected to exit without running the session
				SessionConfigShell.this.cancelClicked = true;

				// Exit the window
				SessionConfigShell.this.configShell.close();

			} else if (event.getSource() == SessionConfigShell.this.aboutMenuItem) {

				// Show the "About" dialog
			    AboutUtility.showStandardAboutDialog(getShell(), appContext.getBean(GeneralProperties.class));
			}
		}
		
		  /**
	     * Validates the content of all GUI fields. If any are found to be in error,
	     * an error dialog is displayed to the user and focus will be returned to
	     * the offending field.
	     * 
	     * @return true if all field values are valid; false if not
	     */
	    private boolean validateInputFields() {

	        // Validate fields in each sub-panel
	        for (final ISessionConfigPanel panel : subPanels) {
	            if (!panel.validateInputFields()) {
	                return false;
	            }
	        }

	        return (true);
	    }
	    
	    /**
	     * Sets fields in the current session configuration object from the values
	     * currently in the GUI fields.
	     */
	    private void setDataFromFields() {

	        // Set filename in session configuration
	        sessionConfig.setConfigFile(configFile);

	        // Clear dynamic session fields that may no longer be relevant
	        // on the GUI
	        clearDynamicAttributes(sessionConfig);

	        // Set data from GUI fields in all sub-panels
	        for (final ISessionConfigPanel panel : subPanels) {
	            panel.setDataFromFields();
	        }

	        // Set the message service topic in the session configuration
	        //  If SSE only, set SSE topic rather than
	        // flight topic
//	      String topic = null;
//	      if (GdsSystemProperties.applicationIsSse()) {
//	          topic = ContextTopicNameFactory.getSseSessionTopic(appContext);
//	      } else {
//	          topic = ContextTopicNameFactory.getMissionSessionTopic(appContext);
//	      }
//	      this.sessionConfig.getGeneralInfo().setRootPublicationTopic(topic);
	    }
	}

	/**
	 * This is the event handler for events on the window Shell. It's really
	 * only here to handle close events.
	 * 
	 *
	 */
	private class ShellEventHandler implements ShellListener {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.swt.events.ShellListener#shellActivated(org.eclipse.swt
		 * .events.ShellEvent)
		 */
		@Override
		public void shellActivated(final ShellEvent arg0) {

			// Empty method for interface only
		}

		/**
		 * If we get here and neither cancel nor run has been clicked, it means
		 * the window has been closed from the close button. We then close the
		 * shell, but first pretend it's a cancel to avoid the recursive call to
		 * ourselves.
		 */
		@Override
		public void shellClosed(final ShellEvent arg0) {

			try {
				if (!(SessionConfigShell.this.cancelClicked
						|| SessionConfigShell.this.runClicked) && actuallyCloseShell(arg0)) {
					SessionConfigShell.this.cancelClicked = true;

					SessionConfigShell.this.configShell.close();
				}
			} catch (final Exception e) {
				trace.error("Error in SessionConfigShell.shellClosed handling: "
						+ rollUpMessages(e), e);
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.swt.events.ShellListener#shellDeactivated(org.eclipse
		 * .swt.events.ShellEvent)
		 */
		@Override
		public void shellDeactivated(final ShellEvent arg0) {

			// Empty method for interface only
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.swt.events.ShellListener#shellDeiconified(org.eclipse
		 * .swt.events.ShellEvent)
		 */
		@Override
		public void shellDeiconified(final ShellEvent arg0) {

			// Empty method for interface only
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.swt.events.ShellListener#shellIconified(org.eclipse.swt
		 * .events.ShellEvent)
		 */
		@Override
		public void shellIconified(final ShellEvent arg0) {

			// Empty method for interface only
		}

	}

	/**
	 * This is the event handler for expand/collapse events on the ExpandItems
	 * in the two ExpandBars. It is necessary for proper resize of the window
	 * when items are expanded/collapsed.
	 * 
	 * I wish I could explain this in more detail but I did not write it
	 * 
	 *
	 */
	private static class PanelExpandHandler implements ExpandListener {

		private final ExpandBar bar;

		public PanelExpandHandler(final ExpandBar toExpand) {

			this.bar = toExpand;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.swt.events.ExpandListener#itemCollapsed(org.eclipse.swt.events.ExpandEvent)
		 */
		@Override
		public void itemCollapsed(final ExpandEvent event) {

			resizeForExpandCollapse();
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.swt.events.ExpandListener#itemExpanded(org.eclipse.swt.events.ExpandEvent)
		 */
		@Override
		public void itemExpanded(final ExpandEvent event) {

			resizeForExpandCollapse();
		}

		/**
		 * Method that performs a resize of the current window.
		 */
		private void resizeForExpandCollapse() {

			final Display display = Display.getCurrent();

			// This whole job has to be done asynchronously because of the order
			// in which expand events are posted versus the state of the actual
			// expand item's size
			new Thread(new Runnable() {
				/**
				 * {@inheritDoc}
				 * 
				 * @see java.lang.Runnable#run()
				 */
				@Override
				public void run() {

					final int[] orgSize = new int[1];
					final int[] currentSize = new int[1];

					final Object lock = new Object();

					if (display.isDisposed()
							|| PanelExpandHandler.this.bar.isDisposed()) {
						return;
					}

					display.syncExec(new Runnable() {
						@Override
						public void run() {

							if (PanelExpandHandler.this.bar.isDisposed()
									|| PanelExpandHandler.this.bar.getShell()
											.isDisposed()) {
								return;
							}

							synchronized (lock) {
								PanelExpandHandler.this.bar.getShell().pack(
										true);
								orgSize[0] = PanelExpandHandler.this.bar
										.getShell().getSize().y;
								currentSize[0] = orgSize[0];
							}
						}
					});

					while (currentSize[0] == orgSize[0]) {
						if (display.isDisposed()
								|| PanelExpandHandler.this.bar.isDisposed()) {
							return;
						}

						display.syncExec(new Runnable() {
							/**
							 * {@inheritDoc}
							 * 
							 * @see java.lang.Runnable#run()
							 */
							@Override
							public void run() {

								synchronized (lock) {
									if (PanelExpandHandler.this.bar
											.isDisposed()
											|| PanelExpandHandler.this.bar
													.getShell().isDisposed()) {
										return;
									}

									currentSize[0] = PanelExpandHandler.this.bar
											.getShell().getSize().y;

									if (currentSize[0] != orgSize[0]) {
										return;
									} else {
										PanelExpandHandler.this.bar.getShell()
												.layout(true);
										PanelExpandHandler.this.bar.getShell()
												.setMinimumSize(SHELL_WIDTH, 0);
										PanelExpandHandler.this.bar.getShell()
												.pack(true);
										PanelExpandHandler.this.bar
												.getShell()
												.setMinimumSize(
														PanelExpandHandler.this.bar
																.getShell()
																.getSize());
									}
								}
							}
						});
					}
				}
			}).start();
		}
	}
}
