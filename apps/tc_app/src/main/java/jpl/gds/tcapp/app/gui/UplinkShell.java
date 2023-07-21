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
package jpl.gds.tcapp.app.gui;

import jpl.gds.common.config.GeneralProperties;
import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.security.AccessControlParameters;
import jpl.gds.common.config.security.SecurityProperties;
import jpl.gds.common.config.types.CommandUserRole;
import jpl.gds.common.config.types.UplinkConnectionType;
import jpl.gds.common.swt.AboutUtility;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.IVenueConfiguration;
import jpl.gds.dictionary.api.command.ICommandDefinitionProvider;
import jpl.gds.perspective.DisplayConfiguration;
import jpl.gds.perspective.DisplayType;
import jpl.gds.perspective.PerspectiveConfiguration;
import jpl.gds.perspective.gui.PerspectiveListener;
import jpl.gds.perspective.gui.PerspectiveShell;
import jpl.gds.security.cam.AccessControl;
import jpl.gds.security.cam.AccessControlException;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.session.config.SessionConfigurationListener;
import jpl.gds.session.config.gui.SessionConfigViewShell;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.config.ReleaseProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.shared.metadata.InvalidMetadataException;
import jpl.gds.shared.process.LineHandler;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.swt.types.ChillLocation;
import jpl.gds.shared.swt.types.ChillSize;
import jpl.gds.shared.types.Pair;
import jpl.gds.tc.api.ISendCompositeState;
import jpl.gds.tc.api.config.CommandFrameProperties;
import jpl.gds.tc.api.config.CommandProperties;
import jpl.gds.tc.api.config.ExecutionStringType;
import jpl.gds.tc.api.message.CommandMessageType;
import jpl.gds.tc.api.message.ITransmittableCommandMessage;
import jpl.gds.tcapp.app.AbstractUplinkApp;
import jpl.gds.tcapp.app.SendCommandApp;
import jpl.gds.tcapp.app.gui.AbstractSendComposite.TransmitListener;
import jpl.gds.tcapp.app.gui.external.ExternalApplication;
import jpl.gds.tcapp.app.gui.external.ExternalApplicationFactory;
import jpl.gds.tcapp.app.gui.factory.IUplinkTabFactory;
import jpl.gds.tcapp.app.gui.fault.CommandBuilderComposite;
import jpl.gds.tcapp.app.gui.icmd.CpdControlPanel;
import jpl.gds.tcapp.app.gui.icmd.CpdUplinkRateView;
import jpl.gds.tcapp.app.gui.icmd.HeaderManager;
import jpl.gds.tcapp.spring.bootstrap.TcAppSpringBootstrap;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static jpl.gds.shared.exceptions.ExceptionTools.rollUpMessages;

/**
 * The main shell for chill_up
 * 
 */
public class UplinkShell implements PerspectiveShell,
SessionConfigurationListener, TransmitListener, HeaderManager, MessageSubscriber {
	private static Tracer log;

	/** Default window width */
	public static final int DEFAULT_WIDTH = 930;
	/** Default window height */
	public static final int DEFAULT_HEIGHT = 1000;

	/** Access through get/setStaticShell methods */
	private static Shell mainShell = null;

	/** Allowable roles */
	private final String[] roles;

	/** Tabs */
	private final java.util.List<AbstractUplinkComposite> tabIndex = new ArrayList<AbstractUplinkComposite>();
	private TabFolder tabFolder = null;

	private TabFolder logTabFolder;
	private LocalRequestHistoryComposite localRequestHistoryTab;

	private DisplayConfiguration config;
	private static Text outputText;

	/** Title */
	private Composite titleComposite;

	private Composite stringIdComposite;
	private Button sendButton;
	private IContextConfiguration testConfig;
	private ChillSize size;
	private ChillLocation location;
	private ChillColor configBackgroundColor;
	private PerspectiveListener perspectiveListener;
	private final SWTUtilities swtUtil = new SWTUtilities();
	private MenuItem exitPerMenuItem;
	private MenuItem saveMenuItem;
	private MenuItem saveAsMenuItem;
	private final EventHandler handler;
	private MenuItem configureMenuItem = null;
	private MenuItem testConfigMenuItem = null;
	private MenuItem exitMenuItem = null;
	private Menu roleMenu = null;
	private MenuItem roleMenuItem = null;
	private MenuItem[] roleMenuItems;
	private MenuItem windowColorMenuItem = null;
	private MenuItem commandFormatMenuItem = null;
	private MenuItem aboutMenuItem = null;

	private Color backgroundColor;

	/** the uplink application running and being used by this shell */
	private final AbstractUplinkApp uplinkApp;

	private java.util.List<ExternalApplication> externalApps = null;

	private final UplinkConnectionType uplinkConnType;
	private CpdControlPanel cpdCtrlPanel;
	private SashForm sashForm;
  
    private boolean guiStarted = false;
    
    private String title = null;
    /* Keep track if key has been added to title */
    private boolean titleHasSessionKey = false;
    
    private final ApplicationContext appContext;
    private final IMessagePublicationBus bus;
    private final SseContextFlag                          sseFlag;

	/**
	 * Constructor
	 * 
	 * @param appContext
	 *            the ApplicationContext in which this object is being used
	 * @param dispConfig
	 *            the display configuration
	 * @param display
	 *            the display
	 * @param configuration
	 *            the session configuration
	 * @param uplinkApp
	 *            the uplink app opening this shell
	 * @param startGui
	 *            If true start GUI now
	 * @throws InvalidMetadataException
	 * @throws BeansException
	 */
    public UplinkShell(final ApplicationContext appContext,
    	               final DisplayConfiguration dispConfig,
                       final Display              display,
                       final IContextConfiguration configuration,
			           final AbstractUplinkApp    uplinkApp,
                       final boolean              startGui) throws BeansException, InvalidMetadataException
    {
    	this.appContext = appContext;
    	this.bus = appContext.getBean(IMessagePublicationBus.class);
        this.log = TraceManager.getDefaultTracer(appContext);
        this.sseFlag = appContext.getBean(SseContextFlag.class);
    	this.testConfig = configuration;
    	
    	final Set<CommandUserRole> roleSet = appContext.getBean(SecurityProperties.class).getRoles();

		roles = new String[roleSet.size()];

		int next = 0;

		for (final CommandUserRole r : roleSet) {
			roles[next++] = r.toString();
		}

		this.uplinkApp = uplinkApp;
		this.uplinkConnType = appContext.getBean(IConnectionMap.class).getFswUplinkConnection().getUplinkConnectionType();

		handler = new EventHandler(this);

		final Shell tempShell = new Shell(display, SWT.TITLE | SWT.MIN
				| SWT.MAX | SWT.RESIZE);

		tempShell.addShellListener(handler);

		setStaticShell(tempShell);

		size = new ChillSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);

        if (sseFlag.isApplicationSse())
		{
			title = getTagLine("SSE Uplink Control");
		}
		else if (appContext.getBean(MissionProperties.class).missionHasSse() &&
				! appContext.getBean(IVenueConfiguration.class).getVenueType().isOpsVenue())
		{
			title = getTagLine("FSW/SSE Uplink Control");
		}
		else
		{
			title = getTagLine("FSW Uplink Control");
		}


        getStaticShell().setText(title);

		if (dispConfig != null) {
			config = dispConfig;
			size = config.getSize();
			location = config.getLocation();
			configBackgroundColor = config.getBackgroundColor();

			// the default background color is white, but we don't want that for
			// chill_up. We detect that white was not explicitly check by seeing
			// if all RGB values are valid. The PerspectiveConfiguration for the
			// uplink app overrides the default background color to -1,-1,-1
			// (RGB), to denote that no color was explicitly set.
			if (configBackgroundColor != null && (configBackgroundColor.getRed() < 0
					|| configBackgroundColor.getGreen() < 0
					|| configBackgroundColor.getBlue() < 0)) {
				configBackgroundColor = null;

			}
		} else {
			config = new DisplayConfiguration();
			location = new ChillLocation(100, 100);
			config.setLocation(location);
			config.setSize(size);
			config.setType(DisplayType.UPLINK);
		}

		getStaticShell().setBackgroundMode(SWT.INHERIT_DEFAULT);

        if (startGui)
        {
            createGui();

            guiStarted = true;
        }
	}


    /**
     * Start up the GUI.
     * @throws InvalidMetadataException 
     * @throws BeansException 
     */
    public void startGui() throws BeansException, InvalidMetadataException
    {

        if (! guiStarted)
        {
            createGui();
            
            bus.subscribe(CommandMessageType.UplinkGuiLog, this);
            bus.subscribe(CommandMessageType.ClearUplinkGuiLog, this);

            guiStarted = true;
        }
    }

	@Override
	public void updateConfiguration() {
		final Point size = getStaticShell().getSize();
		final ChillSize csize = new ChillSize(size.x, size.y);
		final Point location = getStaticShell().getLocation();
		final ChillLocation clocation = new ChillLocation(location.x, location.y);
		final String title = getStaticShell().getText();

		final int red = getStaticShell().getBackground().getRed();
		final int green = getStaticShell().getBackground().getGreen();
		final int blue = getStaticShell().getBackground().getBlue();

		final ChillColor color = new ChillColor(red, green, blue);

		config.setSize(csize);
		config.setName(title);
		config.setLocation(clocation);
		config.setBackgroundColor(color);
	}

	private void createGui() throws BeansException, InvalidMetadataException {
		if (configBackgroundColor != null) {
			final RGB rbg = new RGB(configBackgroundColor.getRed(),
					configBackgroundColor.getGreen(),
					configBackgroundColor.getBlue());
			final Color configColor = new Color(getStaticShell().getDisplay(), rbg);

			getStaticShell().setBackground(configColor);
		}

		getStaticShell().setSize(size.getXWidth(), size.getYHeight());
		getStaticShell().setLocation(location.getXPosition(),
				location.getYPosition());

		final Menu menuBar = new Menu(getStaticShell(), SWT.BAR);

		final MenuItem fileMenuItem = new MenuItem(menuBar, SWT.CASCADE);
		fileMenuItem.setText("File");
		final Menu fileMenu = new Menu(getStaticShell(), SWT.DROP_DOWN);
		fileMenuItem.setMenu(fileMenu);

		configureMenuItem = new MenuItem(fileMenu, SWT.PUSH);
		configureMenuItem.setText("Configure...");
		configureMenuItem.addSelectionListener(handler);

		testConfigMenuItem = new MenuItem(fileMenu, SWT.PUSH);
		testConfigMenuItem.setText("Show Session Configuration...");
		testConfigMenuItem.addSelectionListener(handler);

		saveMenuItem = new MenuItem(fileMenu, SWT.PUSH);
		saveMenuItem.setText("Save Perspective");
		saveMenuItem.addSelectionListener(handler);

		saveAsMenuItem = new MenuItem(fileMenu, SWT.PUSH);
		saveAsMenuItem.setText("Save Perspective as...");
		saveAsMenuItem.addSelectionListener(handler);

		if (!GdsSystemProperties.isIntegratedGui()) {
			saveMenuItem.setEnabled(false);
			saveAsMenuItem.setEnabled(false);
		}

		exitMenuItem = new MenuItem(fileMenu, SWT.PUSH);
		exitMenuItem.setText("Exit Application");
		exitMenuItem.addSelectionListener(handler);

		exitPerMenuItem = new MenuItem(fileMenu, SWT.PUSH);
		exitPerMenuItem.setText("Exit Perspective");
		exitPerMenuItem.setEnabled(perspectiveListener != null);
		exitPerMenuItem.addSelectionListener(handler);

		final MenuItem sendMenuItem = new MenuItem(menuBar, SWT.CASCADE);
		sendMenuItem.setText("Send");
		final Menu sendMenu = new Menu(getStaticShell(), SWT.DROP_DOWN);
		sendMenuItem.setMenu(sendMenu);

		externalApps = ExternalApplicationFactory.getUplinkApplications(appContext);
		for (final ExternalApplication app : externalApps) {
			app.createMenuItem(sendMenu, handler);
		}

		if (sendMenu.getItemCount() < 1) {
			sendMenu.dispose();
			sendMenuItem.dispose();
		}

		if (this.uplinkConnType.equals(UplinkConnectionType.COMMAND_SERVICE)) {
			roleMenu = new Menu(getStaticShell(), SWT.DROP_DOWN);
			roleMenuItem = new MenuItem(menuBar, SWT.CASCADE);
			roleMenuItem.setText("Role");
			roleMenuItem.setMenu(roleMenu);

			final AccessControl ac = AccessControl.getInstance();
			final CommandUserRole cur = (ac != null) ? ac.getUserRole()
					: CommandUserRole.VIEWER;
			final String role = cur.toString();

			if (ac != null) {
				ac.setShell(getStaticShell());
			}

			this.appContext.getBean(AccessControlParameters.class).setUserRole(cur);

			roleMenuItems = new MenuItem[roles.length];

			for (int i = 0; i < roleMenuItems.length; i++) {
				roleMenuItems[i] = new MenuItem(roleMenu, SWT.RADIO);
				roleMenuItems[i].setText(roles[i]);
				roleMenuItems[i].addSelectionListener(handler);

				if (role.equals(roleMenuItems[i].getText())) {
					roleMenuItems[i].setSelection(true);
				}
			}
		}

		final Menu windowMenu = new Menu(getStaticShell(), SWT.DROP_DOWN);
		final MenuItem windowMenuItem = new MenuItem(menuBar, SWT.CASCADE);
		windowMenuItem.setText("Window");
		windowMenuItem.setMenu(windowMenu);

		windowColorMenuItem = new MenuItem(windowMenu, SWT.PUSH);
		windowColorMenuItem.setText("Background color...");
		windowColorMenuItem.addSelectionListener(handler);

		final MenuItem helpMenuItem = new MenuItem(menuBar, SWT.CASCADE);
		helpMenuItem.setText("Help");
		final Menu helpMenu = new Menu(getStaticShell(), SWT.DROP_DOWN);
		helpMenuItem.setMenu(helpMenu);

		commandFormatMenuItem = new MenuItem(helpMenu, SWT.PUSH);
		commandFormatMenuItem.setText("Command Format...");
		commandFormatMenuItem.addSelectionListener(handler);

		aboutMenuItem = new MenuItem(helpMenu, SWT.PUSH);
		aboutMenuItem.setText("About...");
		aboutMenuItem.addSelectionListener(handler);

		getStaticShell().setMenuBar(menuBar);

		getStaticShell().setLocation(config.getLocation().getXPosition(),
				config.getLocation().getYPosition());

		final FormLayout layout = new FormLayout();

		layout.spacing = 5;
		layout.marginWidth = 5;
		layout.marginHeight = 5;

		getStaticShell().setLayout(layout);

		getStaticShell().setSize(config.getSize().getXWidth(), config.getSize().getYHeight());

		configureTitleComposite();

		sashForm = new SashForm(getStaticShell(), SWT.VERTICAL);

		final FormData fd1 = new FormData();

		fd1.top = new FormAttachment(this.titleComposite, 0);
		fd1.left = new FormAttachment(0);
		fd1.right = new FormAttachment(100);
		fd1.bottom = new FormAttachment(100);

		sashForm.setLayoutData(fd1);

		configureTopPane();

		this.logTabFolder = new TabFolder(sashForm, SWT.NONE);

		configureLocalRequestHistoryTab();
		configureConsoleTab();

		sashForm.setWeights(new int[] { 75, 25 });
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.swt.ChillShell#open()
	 */
	@Override
	public void open() {
		getStaticShell().open();

		try {
            if (!sseFlag.isApplicationSse()) {
				appContext.getBean(ICommandDefinitionProvider.class);
			}
		} catch (final Exception e) {
            log.error(ExceptionTools.getMessage(e));
			SWTUtilities.showErrorDialog(getStaticShell(),
					"Command Dictionary Parsing Error", e.getMessage());
		}
		
//      // R8 Refactor - TODO. Decouple from the product classes.  Garrett Sohl
//    // doesn't think this was ever used. Resolve some other way if needed.
//		if (MissionProperties.getGlobalInstance().missionHasSse()
//				&& (vt.equals(VenueType.ATLO) || vt.equals(VenueType.TESTBED))) {
//			try {
//				sendSseProductDir();
//			} catch (Exception e) {
//				IPublishableLogMessage lm = StatusMessageFactory.createPublishableLogMessage(
//                        TraceSeverity.WARNING, "Could not create the SSE product directory: "
//								+ e.getMessage());
//				SessionBasedMessageContext.getInstance().publish(lm);
//			}
//
//			outputText.setText("");
//			this.localRequestHistoryTab.removeAll();
//		}

		if (this.uplinkConnType.equals(UplinkConnectionType.COMMAND_SERVICE)) {
			this.cpdCtrlPanel = new CpdControlPanel(appContext, getStaticShell());
			this.cpdCtrlPanel.open();
			this.cpdCtrlPanel.setHeaderManager(this);
		}

	}
	
//  // R8 REFACTOR - TODO. Decouple from the product classes.  Garrett Sohl
//  // doesn't think this was every used. Resolve some other way if needed.

//	/**
//	 * Send an SSE cmd to provide the SSE product file path for the current MPCS
//	 * session
//	 * 
//	 * @throws ProductStorageException
//	 *             if it cannot create the product directory for SSE
//	 * @throws BlockException
//	 *             if there is an error translating user input command
//	 *             information into a binary representation
//	 * @throws IOException
//	 * @throws SessionOverflowException
//	 *             if the number of telecommand frames in the uplink session to
//	 *             exceed the maximum allowable number of frames in an uplink
//	 *             session
//	 * @throws RawOutputException
//	 *             If the proper raw output adapter can't be created
//	 * @throws UplinkException
//	 *             If the command(s) cannot be sent over the network
//	 */
//	public static void sendSseProductDir() throws ProductStorageException,
//	BlockException, SessionOverflowException, RawOutputException,
//	UplinkException {
//	    
//	    // REFACTOR - TODO. Decouple from the product classes.  Garrett Sohl
//	    // doesn't think this was every used. Resolve some other way if needed.
//		SessionConfiguration testConfig = SessionConfiguration.getGlobalInstance();
//		
//		File productDir = new File(ProductConfigFactory.getStaticInstance().getStorageDir());
//		if (productDir.exists() == false && productDir.mkdirs() == false) {
//			throw new ProductStorageException(
//					"Couldn't create product directory for SSE: "
//							+ productDir.toString());
//		}
//
//		String commandString = CommandProperties.getInstance()
//				.getSseCommandPrefix()
//				+ CommandProperties.getInstance().getSseProductDirCommand()
//				+ ","
//				+ testConfig.getSessionNumber()
//				+ ","
//				+ productDir.getAbsolutePath();
//
//		java.util.List<ICommand> commands = new ArrayList<ICommand>(
//				1);
//
//        // Use factory and not constructor
//		commands.add(CommandObjectFactory.createSseCommand(commandString));
//
//		SendCommandApp.sendCommands(commands, -1);
//	}

	/**
	 * Set the shell for static access
	 * 
	 * @param shell
	 *            the shell for static access
	 */
	public static synchronized void setStaticShell(final Shell shell) {
		mainShell = shell;
	}

	/**
	 * Get the static shell
	 * 
	 * @return the static shell
	 */
	public static synchronized Shell getStaticShell() {
		return mainShell;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.swt.ChillShell#getShell()
	 */
	@Override
	public Shell getShell() {
		return getStaticShell();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.swt.ChillShell#wasCanceled()
	 */
	@Override
	public boolean wasCanceled() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.swt.ChillShell#getTitle()
	 */
	@Override
	public String getTitle() {
		return title;
	}

	/**
	 * Set the display configuration
	 * 
	 * @param config
	 *            the display configuration
	 */
	@Override
	public void setDisplayConfiguration(final DisplayConfiguration config) {
		this.config = config;
	}

	/**
	 * Print a message on the console
	 * 
	 * @param s
	 *            message to print on console
	 */
	public static void printMessage(final String s) {
		if (getStaticShell() != null && !getStaticShell().isDisposed()) {
			SWTUtilities.safeAsyncExec(getStaticShell().getDisplay(), log,
					"printMessage", new Runnable() {
				@Override
				public void run() {
					if (!outputText.isDisposed()) {
						outputText.append(s);
					}
				}
			});
		}
	}

	/**
	 * Clear messages on the console
	 */
	public static void clearMessages() {
		if (getStaticShell() != null) {
			SWTUtilities.safeAsyncExec(getStaticShell().getDisplay(), log,
					"clearMessages", new Runnable() {
				@Override
				public void run() {
					if (!outputText.isDisposed()) {
						outputText.setText("");
					}
				}
			});
		}
	}


	/**
	 * Initialize the perspective listener
	 * 
	 * @param listener Perspective listener
	 */
	public void initPerspectiveListener(final PerspectiveListener listener)
    {
		perspectiveListener = listener;
	}


	/**
	 * Set the perspective listener
	 * 
	 * @param listener
	 *            the perspective listener
	 */
	@Override
	public void setPerspectiveListener(final PerspectiveListener listener) {
		perspectiveListener = listener;
		exitPerMenuItem.setEnabled(perspectiveListener != null);
		saveAsMenuItem.setEnabled(perspectiveListener != null);
		perspectiveChanged();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.perspective.gui.PerspectiveShell#perspectiveChanged()
	 */
	@Override
	public void perspectiveChanged() {
		final PerspectiveConfiguration pc = this.appContext.getBean(PerspectiveConfiguration.class);

		saveMenuItem.setEnabled(perspectiveListener != null
				&& !pc.isLocked()
				&& pc.isWriteable());
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see @see jpl.gds.perspective.gui.PerspectiveShell#getDisplayConfiguration()
	 */
	@Override
	public DisplayConfiguration getDisplayConfiguration() {
		return config;
	}

	/**
	 * Exit, but beware of recursion. What happens is that the user exits, which
	 * causes a shell-closed event, which calls exitShell. We handle
	 * shell-closed in order to field the direct close through the window close
	 * button.
	 */
	@Override
	public void exitShell() {
		getStaticShell().close();
	}

	/**
	 * Shows a standard "About" dialog.
	 * 
	 * @param cmdConfig
	 *            the command configuration for this instance
	 * @param parent
	 *            the parent Shell
	 */
	public static void showCommandFormatDialog(final CommandProperties cmdConfig, final Shell parent) {
		final MessageBox msgDialog = new MessageBox(parent, SWT.ICON_INFORMATION
				| SWT.OK);
		msgDialog.setText("Command Format");
		msgDialog.setMessage(SendCommandApp.getCommandFormatUsage(cmdConfig));
		msgDialog.open();
	}

	private void resetBackgroundColor() {
		final Display display = getStaticShell().getDisplay();
		final Color defaultColor = display
				.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
		getStaticShell().setBackground(defaultColor);
	}

	private RGB showColorChooser() {
		final ColorDialog dlg = new ColorDialog(getStaticShell());

		// Change the title bar text
		dlg.setText("Choose a background color");

		// Open the dialog and retrieve the selected color
		return dlg.open();
	}

	/**
	 * Event handler for user actions
	 * 
	 */
	private class EventHandler implements SelectionListener, KeyListener,
	ShellListener {
		private final UplinkShell shell;

		public EventHandler(final UplinkShell shell) {
			this.shell = shell;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
		 */
		@Override
		public void widgetDefaultSelected(final SelectionEvent arg0) {
			// do nothing
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse
		 * .swt.events.SelectionEvent)
		 */
		@Override
		public void widgetSelected(final SelectionEvent arg0) {
			try {
				safeWidgetSelected(arg0);
			} catch (final Exception e) {
				log.error("widgetSelected error: " + rollUpMessages(e));

				e.printStackTrace();
			}
		}

		private void safeWidgetSelected(final SelectionEvent arg0) {
			if (arg0.getSource() == shell.configureMenuItem) {
				final UplinkConfigShell configShell = new UplinkConfigShell(
						appContext, getStaticShell(), swtUtil);

				configShell.open();
			} else if (arg0.getSource() == shell.testConfigMenuItem) {
				final SessionConfigViewShell configShell = new SessionConfigViewShell(appContext,
						getStaticShell());
				configShell.setContextConfiguration(testConfig);
				configShell.open();
			} else if (arg0.getSource() == shell.saveMenuItem) {
				if (perspectiveListener != null) {
					perspectiveListener.saveCalled();
				}
			} else if (arg0.getSource() == shell.saveAsMenuItem) {
				if (perspectiveListener != null) {
					perspectiveListener.saveAsCalled(getStaticShell());
				}
			} else if (arg0.getSource() == shell.exitMenuItem) {
				exitShell();
			} else if (arg0.getSource() == shell.exitPerMenuItem) {
				if (perspectiveListener != null) {
					perspectiveListener.exitCalled();
				}
			} else if (arg0.getSource() == shell.commandFormatMenuItem) {
				showCommandFormatDialog(appContext.getBean(CommandProperties.class), getStaticShell());
			} else if (arg0.getSource() == shell.windowColorMenuItem) {
				final RGB selectedColor = showColorChooser();

				if (selectedColor != null) {
					final Color newColor = new Color(getStaticShell().getDisplay(),
							selectedColor);

					if (backgroundColor != null) {
						resetBackgroundColor();
						backgroundColor.dispose();
					}

					if (newColor != null) {
						backgroundColor = newColor;
						UplinkShell.getStaticShell().setBackground(newColor);
					}
				}

			} else if (arg0.getSource() == shell.aboutMenuItem) {
			    AboutUtility.showStandardAboutDialog(getStaticShell(), 
			    		appContext.getBean(GeneralProperties.class));
			} else if(roleMenuItems != null) {
					for (int i = 0; i < roleMenuItems.length; i++) {
						if (arg0.getSource() == roleMenuItems[i]) {
							// Set role in session config and access control and in
							// the CPD panel header
							final String newRole = roleMenuItems[i].getText();

						final CommandUserRole cur = CommandUserRole.valueOf(newRole);

						appContext.getBean(AccessControlParameters.class).setUserRole(cur);

						try {
							final AccessControl ac = AccessControl
									.getInstance();

							if (ac != null) {
								ac.setUserRole(appContext.getBean(SecurityProperties.class), cur);
							}
						} catch (final AccessControlException ace) {
							SWTUtilities.showErrorDialog(
									getShell(),
									"Internal error",
									"Could not set user role: "
											+ ace.getMessage());
						}

						cpdCtrlPanel.getHeaderManager()
						.sendRoleConfigurationChange(newRole);
						break;
					}
				}
			}

			for (final ExternalApplication app : externalApps) {
				if (arg0.getSource() == app.getMenuItem()) {
					try {
						UplinkShell.clearMessages();

						// intentionally using the same object so we get stream
						// ordering correct
						final LineHandler lh = new UplinkShellLineHandler();
						app.launch(lh, lh);
					} catch (final IOException e) {
						SWTUtilities
						.showErrorDialog(
								getShell(),
								"External Application Error",
								"Could not launch the external application "
										+ app.getName() + ": "
										+ e.getMessage());
					}
				}
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.swt.events.KeyListener#keyReleased(org.eclipse.swt.events
		 * .KeyEvent)
		 */
		@Override
		public void keyReleased(final KeyEvent arg0) {
			// do nothing
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.swt.events.ShellListener#shellClosed(org.eclipse.swt.
		 * events.ShellEvent)
		 */
		@Override
		public void shellClosed(final ShellEvent arg0) {
			try {
				if(actuallyCloseShell(arg0)) {
					for (final ExternalApplication app : externalApps) {
						if (app.isRunning()) {
							log.info("Shutting down external application " + app.getName() + "...");
							app.shutdown();
						}
					}
				}
			} catch (final Exception e) {
				log.error("shellClosed error: " + rollUpMessages(e));

				e.printStackTrace();
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.swt.events.ShellListener#shellActivated(org.eclipse.swt
		 * .events.ShellEvent)
		 */
		@Override
		public void shellActivated(final ShellEvent arg0) {
			// do nothing
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
			// do nothing
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
			// do nothing
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
			// do nothing
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.swt.events.KeyListener#keyPressed(org.eclipse.swt.events
		 * .KeyEvent)
		 */
		@Override
		public void keyPressed(final KeyEvent arg0) {
			// do nothing
		}
	}

	/**
	 * Uplink shell line handler
	 * 
	 */
	private static class UplinkShellLineHandler implements LineHandler {
		public UplinkShellLineHandler() {
			super();
		}

		@Override
		public void handleLine(String line) throws IOException {
			if (line.endsWith("\n") == false) {
				line += "\n";
			}

			UplinkShell.printMessage(line);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * jpl.gds.config.SessionConfigurationListener#configurationChanged
	 * (jpl.gds.config.SessionConfiguration)
	 */
	@Override
	public void configurationChanged(final SessionConfiguration config) {
		testConfig = config;
		if (getStaticShell().isDisposed()) {
			return;
		}

		SWTUtilities.safeAsyncExec(getStaticShell().getDisplay(), log,
				"updateTitle", new Runnable() {
			@Override
			public void run() {
				try
                {

					final StringBuilder title =
                        new StringBuilder(getTitle());

                     title.append(": ");
                     title.append(testConfig.getContextId().getName());

					if (testConfig.getContextId().getNumber() != null)
                    {
						title.append(" (");
                        title.append(testConfig.getContextId().getNumber());

                        title.append(')');

					}

					mainShell.setText(title.toString());
				}
                catch (final Exception e)
                {

                    if ((outputText != null) && ! outputText.isDisposed())
                    {
                        String message = e.getMessage();

                        if (message == null)
                        {
                            message = e.toString();
                        }

                        outputText.setText("Exception encountered while sending uplink: " +
                                           message                                        +
                                           "\n\n");
                    }
				}
			}
		});

	}

	//  R8 Refactor - Commenting out everything related to session restart
//	/*
//	 * (non-Javadoc)
//	 * 
//	 * @see
//	 * jpl.gds.perspective.gui.PerspectiveShell#sessionChanged(jpl.gds.
//	 * core.config.SessionConfiguration)
//	 */
//	@Override
//	public void sessionChanged(SessionConfiguration newSession) {
//		UplinkShell
//		.printMessage("Session was restarted by partner application. Stopping session "
//				+ testConfig.
//				getContextId().getNumber() + ".\n");
//		this.uplinkApp.stopExternalInterfaces();
//		// 3/15/13 The add/remove configuration listener
//		// may not be required. Session Config listeners were
//		// removed as part of session config refactoring for 
//		// But I am not positive session restart and
//		// session config display will work properly so I am
//		// leaving these here in commented state for now.  If these 
//		// 2 lines are still commented out in a year, you can remove
//		// them.
//		//testConfig.removeConfigurationListener(this);
//		this.testConfig = newSession;
//		//testConfig.addConfigurationListener(this);
//		SessionConfiguration.setGlobalInstance(newSession);
//		this.uplinkApp.resetSession();
//		UplinkShell.printMessage("Automatically starting session "
//				+ testConfig.getContextId().
//				getNumber() + ".\n");
//		try {
//			this.uplinkApp.startExternalInterfaces();
//		} catch (final AuthenticationException e) {
//			log.warn("Authentication failed", e);
//		} catch (final InvalidMetadataException e) {
//		    log.error("Failed to restart external interfaces upon session change", e);
//        }
//		configurationChanged(this.testConfig);
//	}

	private void configureTitleComposite() {
		this.titleComposite = new Composite(getStaticShell(), SWT.NONE);

		final FormLayout fl = new FormLayout();

		fl.spacing = 5;
		fl.marginHeight = 5;
		fl.marginWidth = 5;

		this.titleComposite.setLayout(fl);

		final Composite dummyComposite = new Composite(getStaticShell(), SWT.NONE);

		FormData fd = new FormData();

		fd.top = new FormAttachment(0);
		fd.bottom = new FormAttachment(0);
		fd.left = new FormAttachment(0);
		fd.right = new FormAttachment(100);

		dummyComposite.setLayoutData(fd);

		fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.left = new FormAttachment(dummyComposite, 0, SWT.CENTER);

		this.titleComposite.setLayoutData(fd);

		// make labels
		final Label venueLabel = new Label(titleComposite, SWT.NONE);
		final Label missionLabel = new Label(titleComposite, SWT.NONE);
		final Label scidLabel = new Label(titleComposite, SWT.NONE);

		venueLabel.setText("Venue:");
		missionLabel.setText("Mission:");
		scidLabel.setText("Spacecraft ID:");

		// make text fields
		final String mission = appContext.getBean(MissionProperties.class).getMissionLongName();
		final String scid = Integer.toString(appContext.getBean(IContextIdentification.class).getSpacecraftId());
		final String venue = appContext.getBean(IVenueConfiguration.class).getVenueType().toString();

		final Label venueText = new Label(titleComposite, SWT.NONE);
		final FontData titleFontData = venueText.getFont().getFontData()[0];
		final Font titleFont = new Font(getStaticShell().getDisplay(), new FontData(
				titleFontData.getName(), titleFontData.getHeight(), SWT.BOLD));

		venueText.setText(venue);
		venueText.setFont(titleFont);

		final Label missionText = new Label(titleComposite, SWT.NONE);
		missionText.setText(mission);
		missionText.setFont(titleFont);

		final Label scidText = new Label(titleComposite, SWT.NONE);
		scidText.setText(scid);
		scidText.setFont(titleFont);

		if (appContext.getBean(CommandProperties.class).showStringIdSelector()) {
			this.stringIdComposite = new StringIdComposite(this.titleComposite);
		}

		// lay everything out
		final FormData venueLabelFd = new FormData();
		venueLabelFd.top = new FormAttachment(0);
		venueLabelFd.left = new FormAttachment(0);
		venueLabel.setLayoutData(venueLabelFd);

		final FormData venueTextFd = new FormData();
		venueTextFd.top = new FormAttachment(0);
		venueTextFd.left = new FormAttachment(venueLabel, 0);
		venueText.setLayoutData(venueTextFd);

		final FormData missionLabelFd = new FormData();
		missionLabelFd.top = new FormAttachment(0);
		missionLabelFd.left = new FormAttachment(venueText, 0);
		missionLabel.setLayoutData(missionLabelFd);

		final FormData missionTextFd = new FormData();
		missionTextFd.top = new FormAttachment(0);
		missionTextFd.left = new FormAttachment(missionLabel, 0);
		missionText.setLayoutData(missionTextFd);

		final FormData scidLabelFd = new FormData();
		scidLabelFd.top = new FormAttachment(0);
		scidLabelFd.left = new FormAttachment(missionText, 0);
		scidLabel.setLayoutData(scidLabelFd);

		final FormData scidTextFd = new FormData();
		scidTextFd.top = new FormAttachment(0);
		scidTextFd.left = new FormAttachment(scidLabel, 0);
		scidText.setLayoutData(scidTextFd);

		if (appContext.getBean(CommandProperties.class).showStringIdSelector()) {
			final FormData stringIdFd = new FormData();
			stringIdFd.top = new FormAttachment(scidText, 0, SWT.CENTER);
			stringIdFd.left = new FormAttachment(scidText, 0);
			this.stringIdComposite.setLayoutData(stringIdFd);
		}
	}


	private void configureConsoleTab() {
		final TabItem tabItem = new TabItem(this.logTabFolder, SWT.NONE);

		tabItem.setText("Console");

		outputText = new Text(this.logTabFolder, SWT.MULTI | SWT.V_SCROLL
				| SWT.H_SCROLL);
		outputText.setEditable(false);

		final FormData data = new FormData();
		data.top = new FormAttachment(0);
		data.left = new FormAttachment(0);
		data.right = new FormAttachment(100);
		data.bottom = new FormAttachment(100);

		outputText.setLayoutData(data);
		tabItem.setControl(outputText);
	}

	private void configureLocalRequestHistoryTab() {
		final TabItem tabItem = new TabItem(this.logTabFolder, SWT.NONE);

		tabItem.setText("Local Request History");

		this.localRequestHistoryTab = new LocalRequestHistoryComposite(
				appContext, this.logTabFolder);
		this.localRequestHistoryTab.addHistoryMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(final MouseEvent e) {
				final int selected = localRequestHistoryTab.getSelectionIndex();
				if (selected >= 0
						&& selected < localRequestHistoryTab.getHistoryCount()) {
					selectAndPopulateTabWithHistory(localRequestHistoryTab
							.getSelectedHistory());
				}
			}
		});
		
		/**
		 * Add selection event for displaying
		 * immediate commands in composite upon single click/selection.
		 */		
		this.localRequestHistoryTab.addHistorySelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(final SelectionEvent e) {
				final int selected = localRequestHistoryTab.getSelectionIndex();
				if (selected >= 0
						&& selected < localRequestHistoryTab.getHistoryCount()) {
					populateImmediateCommandTab(localRequestHistoryTab
							.getSelectedHistory());
				}				
			}

			@Override
			public void widgetDefaultSelected(final SelectionEvent e) {
				// do nothing				
			}
			
		});

		final FormLayout fl = new FormLayout();

		fl.spacing = 10;
		fl.marginHeight = 5;
		fl.marginWidth = 5;

		this.localRequestHistoryTab.setLayout(fl);

		final FormData data = new FormData();

		data.left = new FormAttachment(0);
		data.right = new FormAttachment(100);

		this.localRequestHistoryTab.setLayoutData(data);

		tabItem.setControl(this.localRequestHistoryTab);
	}

	private void configureTopPane() throws BeansException, InvalidMetadataException {
		final Composite topPane = new Composite(sashForm, SWT.NONE);
		GridLayout gl = new GridLayout();
		topPane.setLayout(gl);

		tabFolder = new TabFolder(topPane, SWT.NONE);

		tabFolder.setBackgroundMode(SWT.INHERIT_DEFAULT);
		tabFolder.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(final SelectionEvent arg0) {
				// do nothing
			}

			@Override
			public void widgetSelected(final SelectionEvent arg0) {
				updateSendButton();
			}

		});
		
		final IUplinkTabFactory tabFactory = (IUplinkTabFactory) appContext.getBean(TcAppSpringBootstrap.UPLINK_TAB_FACTORY);
		
		final List<Pair<TabItem, AbstractUplinkComposite>> tabs = tabFactory.createUplinkTabs(appContext, this, tabFolder);
		
		for(final Pair<TabItem, AbstractUplinkComposite> tab : tabs) {
			tab.getTwo().addTransmitListener(this);
			tabIndex.add(tabFolder.indexOf(tab.getOne()), tab.getTwo());
		}

		GridData gd = new GridData(GridData.FILL_BOTH);
		tabFolder.setLayoutData(gd);

		final Composite transmitBar = new Composite(topPane, SWT.NONE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		transmitBar.setLayoutData(gd);

		gl = new GridLayout();
		gl.numColumns = 4;

		transmitBar.setLayout(gl);

		final CpdUplinkRateView uplinkRateView = new CpdUplinkRateView(appContext, transmitBar,
				SWT.NONE);

		gd = new GridData(GridData.FILL_HORIZONTAL);
		uplinkRateView.getControl().setLayoutData(gd);

		this.sendButton = new Button(transmitBar, SWT.NONE);
		this.sendButton.setText("Send");

		this.sendButton.addSelectionListener(new SelectionListener() {
			private boolean doit = true;
			@Override
			public void widgetDefaultSelected(final SelectionEvent arg0) {
				// do nothing
			}

			// Prevented multiple initiateSend invocation
			// from multiple button clicks.
			@Override
			public void widgetSelected(final SelectionEvent arg0) {

                            if (doit) {
                				doit = false;
                            	initiateSend();
                            	Display.getDefault().timerExec(Display.getDefault().getDoubleClickTime(),
                    					new Runnable() {
                    						@Override
                                            public void run() {
                    							doit = true;
                    						}
                    					});
                            }
                        
                    
			}
		});
	}
	
	/**
	 * Special handling for immediate command window
	 * Separated this into two functions to utilize the text extraction elsewhere
	 */
	private void populateImmediateCommandTab(final TableItem item) {
		
		// Check if immediate command tab is open
		if(this.tabFolder.getSelection()[0].getText().equals(LocalRequestHistoryComposite.IMMEDIATE_TRANSMITTER_NAME)) {
			
			final ImmediateCommandComposite immediateCmdComp = (ImmediateCommandComposite)this.tabIndex.get(0);

			String commandText = getImmediateCommandFromTableItem(item);

			immediateCmdComp.setCommandText(commandText);
		}
	}

	private String getImmediateCommandFromTableItem(TableItem item) {
			String commandText = "";
			String source = item.getText(0);

			// Check if command source is Immediate Command
			if(source.equals(LocalRequestHistoryComposite.IMMEDIATE_TRANSMITTER_NAME)) {
				// Get immediate command from column data
				commandText = item.getText(2);
			}
			return commandText;
	}

	private void selectAndPopulateTabWithHistory(final TableItem item) {
		final TransmitEvent event = localRequestHistoryTab.getTransmitEvent(Integer
				.valueOf(item.getData("transmitId").toString()));

		if (event != null) {
			final int index = this.tabIndex.indexOf(event.getTransmitter());

			if (index >= 0 || index < this.tabIndex.size()) {
				changeTabs(index);

				//fix search criteria in stem list
				final int selectedIndex = this.tabFolder.getSelectionIndex();
				final TabItem selectedTab = this.tabFolder.getItem(selectedIndex);
				final Control selectedCtrl = selectedTab.getControl();
				if (selectedCtrl instanceof CommandBuilderComposite) {
					final CommandBuilderComposite selectedComp = (CommandBuilderComposite) selectedCtrl;
					String stem = "";
					if(item.getText(2).indexOf(",") != -1) {
						stem = item.getText(2).substring(0, item.getText(2).indexOf(","));
					}else {
						stem = item.getText(2);
					}
					selectedComp.clearStemSearchCriteria(stem);
				}
				else if (selectedCtrl instanceof ImmediateCommandComposite) {
					/*
					 *  Update to only repopulate a
					 * file list history with the single command.
					 * When a command list file is transmitted, only ONE
					 * event is stored for all of the commands because all of the commands
					 * are stored in one SCMF.
					 * This works for both regular immediate commands and SSE commands
					 */
					ISendCompositeState state = event.getTransmitState();

					if (state instanceof ImmediateCommandComposite.ImmediateCommandState) {
						ImmediateCommandComposite.ImmediateCommandState myState =
								(ImmediateCommandComposite.ImmediateCommandState) event.getTransmitState();
						myState.setCommand(getImmediateCommandFromTableItem(item));
					}
				}

				event.getTransmitter().setFieldsFromTransmitHistory(event);
			} else {
				SWTUtilities.showErrorDialog(getShell(),
						"Invalid History Item",
						"The selected history item does not map to any tab.");
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * jpl.gds.tcapp.app.gui.AbstractSendComposite.TransmitListener#onTransmit
	 * (jpl.gds.tcapp.app.gui.TransmitEvent)
	 */
	@Override
	public void onTransmit(final TransmitEvent event) {
		this.localRequestHistoryTab.onTransmit(event);
	}

	@Override
	public void onTransmit(ITransmittableCommandMessage msg) {
		this.localRequestHistoryTab.onTransmit(msg);
	}

	private void initiateSend() {
		final int selectedIndex = this.tabFolder.getSelectionIndex();
		final TabItem selectedTab = this.tabFolder.getItem(selectedIndex);

		final Control selectedCtrl = selectedTab.getControl();

		if (selectedCtrl instanceof AbstractUplinkComposite) {
			final AbstractUplinkComposite selectedComp = (AbstractUplinkComposite) selectedCtrl;
			try {
				selectedComp.initiateSend();
			} catch (final Exception e) {
				SWTUtilities.showErrorDialog(getStaticShell(),
						"Execution Error", e.getMessage());
			}
		}
	}

	/**
	 * A widget to select the string ID to uplink to
	 * 
	 */
	private class StringIdComposite extends Composite {
		private final Combo stringIdCombo;

		public StringIdComposite(final Composite parent) {
			super(parent, SWT.NONE);

			final FormLayout layout = new FormLayout();
			setLayout(layout);

			stringIdCombo = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);
			for (final ExecutionStringType est : ExecutionStringType.values()) {
				if (!est.toString().equalsIgnoreCase("UNKNOWN")) {
					stringIdCombo.add(est.toString());
				}
			}
			stringIdCombo.setText(appContext.getBean(CommandFrameProperties.class)
					.getStringId() == null ? ExecutionStringType.A.toString() 
							: appContext.getBean(CommandFrameProperties.class).getStringId());
			stringIdCombo.addSelectionListener(handler);

			final FormData comboData = new FormData();
			comboData.right = new FormAttachment(100);
			comboData.top = new FormAttachment(0);
			stringIdCombo.setLayoutData(comboData);

			final Label stringIdLabel = new Label(this, SWT.NONE);
			stringIdLabel.setText("String ID:");
			final FormData fd = new FormData();
			fd.right = new FormAttachment(stringIdCombo);
			fd.top = new FormAttachment(stringIdCombo, 0, SWT.CENTER);
			stringIdLabel.setLayoutData(fd);

			stringIdCombo.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetDefaultSelected(final SelectionEvent arg0) {
					// do nothing
				}

				@Override
				public void widgetSelected(final SelectionEvent arg0) {
					try {
						appContext.getBean(CommandFrameProperties.class)
						.setStringId(
								stringIdCombo.getText());
					} catch (final Exception e) {
						log.error("widgetSelected error: " + rollUpMessages(e));

						e.printStackTrace();
					}

				}
			});
		}
	}

	private void changeTabs(final int tabIndex) {
		this.tabFolder.setSelection(tabIndex);
		updateSendButton();
	}

	private void updateSendButton() {
	        /*Fix index out of bounds
                   exception because with the latest version of SWT, this
                   handler gets called before anything is in tabIndex.
                   So return if tabIndex is empty.
		*/
		final int selectedIndex = tabFolder.getSelectionIndex();
                if (selectedIndex < 0 || selectedIndex >= tabIndex.size()) {
		    return;
	        }
 
		final AbstractUplinkComposite comp = UplinkShell.this.tabIndex
				.get(selectedIndex);
		if (comp.needSendButton()) {
			UplinkShell.this.sendButton.setVisible(true);
		} else {
			UplinkShell.this.sendButton.setVisible(false);
		}
	}
	
	@Override
    public void handleMessage(final IMessage m) {
	    if (m.isType(CommandMessageType.UplinkGuiLog)) {
	        printMessage(m.getOneLineSummary());
	    } else if (m.isType(CommandMessageType.ClearUplinkGuiLog)) {
	        clearMessages();
	    }
	}
	

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * jpl.gds.tcapp.icmd.gui.HeaderManager#sendRoleConfigurationChange(java
	 * .lang.String)
	 */
	@Override
	public void sendRoleConfigurationChange(final String role) {
		cpdCtrlPanel.getHeader().sendRoleConfigurationChange(role);
	}
	
    /**
     * session key is created after UplinkShell is initialized
     * this method updates the title string by attaching the new session key in the title string
     */
	public void updateTitle() {
		
		/* If -K option is used, the session key
		 * will already have been added */
		if(!titleHasSessionKey) {
		
			final StringBuilder title = new StringBuilder(getTitle());
			
			if (testConfig.getContextId().getNumber() != null)
            {
				title.append(" (");
                title.append(testConfig.getContextId().getNumber());
                title.append(')');
            }
			getStaticShell().setText(title.toString());
		}
	}

    /**
     * Construct tag line with session, etc.
     *
     * @param tailor Tailoring text
     *
     * @return Resulting string
     */
    private String getTagLine(final String tailor)
    {
        final StringBuilder sb = new StringBuilder();

        sb.append(ReleaseProperties.getProductLine());
        sb.append(' ').append(tailor).append(": ");
        sb.append(testConfig.getContextId().getName());

        final Long sessionId = testConfig.getContextId().getNumber();

        if ((sessionId != null) && (sessionId.longValue() > 0L))
        {
        	sb.append(" (");
            sb.append(sessionId);
            sb.append(')');
            
            /* Keep track of whether the session
             * key has been added to the title */
            titleHasSessionKey = true;
        }
        return sb.toString();
    }
}
