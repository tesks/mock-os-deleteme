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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.GeneralProperties;
import jpl.gds.common.config.TimeComparisonStrategy;
import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.connection.IDownlinkConnection;
import jpl.gds.common.swt.AboutUtility;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.TimeComparisonStrategyContextFlag;
import jpl.gds.db.api.adaptation.IMySqlAdaptationProperties;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.client.FlightDictionaryLoadingStrategy;
import jpl.gds.dictionary.api.client.SseDictionaryLoadingStrategy;
import jpl.gds.dictionary.api.client.channel.IChannelUtilityDictionaryManager;
import jpl.gds.eha.api.message.IEhaMessageFactory;
import jpl.gds.eha.api.message.ISuspectChannelTable;
import jpl.gds.eha.api.message.ISuspectChannelsMessage;
import jpl.gds.eha.api.service.channel.ISuspectChannelService;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.perspective.DisplayConfiguration;
import jpl.gds.perspective.PerspectiveConfiguration;
import jpl.gds.perspective.config.ViewType;
import jpl.gds.perspective.gui.PerspectiveListener;
import jpl.gds.perspective.gui.PerspectiveShell;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.session.config.SessionConfigurationListener;
import jpl.gds.session.config.gui.SessionConfigViewShell;
import jpl.gds.session.message.EndOfSessionMessage;
import jpl.gds.session.message.SessionMessageType;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.config.PerformanceProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.GuiNotifier;
import jpl.gds.shared.log.ILogMessage;
import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.BaseMessageHandler;
import jpl.gds.shared.message.CommonMessageType;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.swt.ChillColorCreator;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.swt.types.ChillLocation;
import jpl.gds.shared.swt.types.ChillSize;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.telem.common.app.mc.DownlinkProcessingState;
import jpl.gds.telem.down.IDownlinkApp;
import jpl.gds.telem.down.perspective.view.DownMessageViewConfiguration;
import jpl.gds.telem.input.api.TmInputMessageType;
import jpl.gds.telem.input.api.config.TelemetryInputProperties;
import jpl.gds.telem.input.api.message.ITelemetrySummaryMessage;
import jpl.gds.tm.service.api.TmServiceMessageType;

/**
 * AbstractDownShell is the main SWT GUI class for the downlink controller.
 * It has an associated IDownlinkApp application that 
 * actually processes the telemetry.
 * 
 * @see IDownlinkApp
 */
public abstract class AbstractDownShell implements PerspectiveShell, SessionConfigurationListener {
	/**
	 * Default title for this shell.
	 */
	protected static final String TITLE = "Downlink Control";
	
	/** Text for confirmations */
	protected static final String ARE_YOU_SURE = "Are you sure?";

	/**
	 * Default pixel width of the downlink window.
	 */
	public static final int DEFAULT_WIDTH = 680;
	/**
	 * Default pixel height of the downlink window.
	 */
	public static final int DEFAULT_HEIGHT = 460;

	/**
	 * The SWT display on which this shell is drawn.
	 */
	protected final Display mainDisplay;

	/**
	 * The Downlink application object.
	 * Protected, so they can be used by the SSE subclass.
	 */
	protected final IDownlinkApp downApp;
	/**
	 * The current ContextConfiguration object.
	 */
	protected IContextConfiguration testConfig;
	/**
	 * The SWT object that displays downlink messages.
	 */
	protected MessageTable table;
	/**
	 * Flag indicating whether the downlink application thread was ever started.
	 */
	protected boolean neverStarted = true;
	
	/**
	 * Shared tracer instance.
	 */
	protected Tracer log;
	
    //  R8 Refactor - Commenting out everything related to session restart
//	/**
//	 * Flag indicating a session reset attempt failed 
//	 */
//	protected boolean resetFailed;

	private Color redColor;
	private Color yellowColor;
	private Color greenColor;
	private Color whiteColor;
	private Color blackColor;
	private Shell mainShell;
	private MenuItem startMenuItem;
	private MenuItem stopMenuItem;
	private MenuItem pauseMenuItem;
	private MenuItem exitPerMenuItem;
	private MenuItem clearLadAllItem;
	private MenuItem clearBufferItem;
	private ToolItem startToolItem;
	private ToolItem stopToolItem;
	private ToolItem pauseToolItem;
	private ToolBar toolBar;
	private Thread appThread;
	private Label statusLabel;
	private boolean started = false;
	private boolean paused = false;
	private boolean stopped = false;
	private Group statusGroup;
	private boolean firstRun = true;
	private Group connectedGroup;
	private Label connectedLabel;
	private Group syncGroup;
	private Label syncLabel;
	private Group flowGroup;
	private Label flowLabel;
	private Group processGroup;
	private Label processLabel;
	private final boolean standAlone;
	private final Shell parentShell;
	private boolean subscribed;
	private DisplayConfiguration displayConfig;
	private PerspectiveListener perspectiveListener;
	private MenuItem saveMenuItem;
	private MenuItem saveAsMenuItem;
	//private boolean awaitingStop = true;
	
	/** The current application context */
	protected ApplicationContext appContext;
	/** The internal message publication bus */
	protected IMessagePublicationBus bus;
	/** The current time comparison strategy */
	protected TimeComparisonStrategyContextFlag timeStrategy;

	private final IEhaMessageFactory ehaMessageFactory;

    private DownStatisticsComposite             statisticsComp;

    private final SseContextFlag                      sseFlag;

	/**
	 * Creates an instance of AbstractDownShell with the given parent Display,
	 * associated application class, and test configuration.
	 *
	 * On Linux we do not get min/max decorations if we have a parent shell,
	 * so do not define one.
	 *
	 * @param context the current application context
	 * @param display the parent Display
	 * @param app the associated instance of ChillRunnerApp
	 * @param tc the IContextConfiguration object
	 * @param dispConfig the DisplayConfiguration for this window
	 */
	public AbstractDownShell(final ApplicationContext context, final Display display, final IDownlinkApp app, final IContextConfiguration tc, final DisplayConfiguration dispConfig)
	{
		appContext = context;
		bus = appContext.getBean(IMessagePublicationBus.class);
        log = TraceManager.getTracer(context, Loggers.DOWNLINK);
		timeStrategy = appContext.getBean(TimeComparisonStrategyContextFlag.class);
		this.ehaMessageFactory = appContext.getBean(IEhaMessageFactory.class);
        this.sseFlag = context.getBean(SseContextFlag.class);
		
		downApp = app;
		testConfig = tc;
		standAlone = true;
		mainDisplay = display;
		parentShell = null;
		this.setDisplayConfiguration(dispConfig);
		createColors();
		createControls();
	}

	/**
	 * Creates an instance of AbstractDownShell with the given parent Shell,
	 * associated application class, and test configuration.
	 * @param context the current application context
	 * @param parent the parent Shell
	 * @param app the associated instance of the downlink application
	 * @param config the IContextConfiguration object
	 */
	public AbstractDownShell(final ApplicationContext context, final Shell parent, final IDownlinkApp app, final IContextConfiguration config) {

	    bus = appContext.getBean(IMessagePublicationBus.class);
        log = TraceManager.getTracer(context, Loggers.DOWNLINK);
        timeStrategy = appContext.getBean(TimeComparisonStrategyContextFlag.class);
        this.ehaMessageFactory = appContext.getBean(IEhaMessageFactory.class);
        this.sseFlag = appContext.getBean(SseContextFlag.class);
    
		appContext = context;
		downApp = app;
		testConfig = config;
		mainDisplay = parent.getDisplay();
		parentShell = parent;
		standAlone = false;
		createColors();
		createControls();       
	}

	/**
	 * Creates standard colors used in this window.
	 *
	 */
	private void createColors() {
		redColor = ChillColorCreator.getColor(new ChillColor(ChillColor.ColorName.RED));
		greenColor = ChillColorCreator.getColor(new ChillColor(ChillColor.ColorName.GREEN));
		yellowColor = ChillColorCreator.getColor(new ChillColor(ChillColor.ColorName.YELLOW));
		whiteColor = ChillColorCreator.getColor(new ChillColor(ChillColor.ColorName.WHITE));
		blackColor = ChillColorCreator.getColor(new ChillColor(ChillColor.ColorName.BLACK));
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public Shell getShell() {
		return mainShell;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void open() {
		if (displayConfig != null) {
			// Setup values out of config
			ChillLocation location = displayConfig.getLocation();
			if (location == null) {
				location = new ChillLocation(100, 100);
			}
			mainShell.setLocation(location.getXPosition(),location.getYPosition());
		}
		mainShell.open();
	}

    /**
     * Stops the GUI window log listener
     */
    public void stopGuiListeners() {
        this.table.stopGuiListener();
    }

	/**
	 * Creates the controls and composites in the main shell.
	 */
	private void createControls() {
		mainShell = new Shell(parentShell, SWT.SHELL_TRIM);

        mainShell.addListener(SWT.Close,
				new Listener()
		{
			@Override
			public void handleEvent(final Event event)
			{
			    // Close gui using downApp instead of internal methods
			    downApp.getDownlinkShell().exitShell();
				System.exit(0); // invoke shutdown hook
			}
		});

		mainShell.setText(getTitle());

		final FormLayout shellLayout = new FormLayout();
		shellLayout.marginHeight = 5;
		shellLayout.marginWidth = 5;
		mainShell.setLayout(shellLayout);
		final Menu menuBar = new Menu(mainShell, SWT.BAR);
		if (standAlone) {
			mainShell.setMenuBar(menuBar);
		}

		final MenuItem fileMenuItem = new MenuItem(menuBar, SWT.CASCADE);
		fileMenuItem.setText("File");
		final MenuItem controlMenuItem = new MenuItem(menuBar, SWT.CASCADE);
		controlMenuItem.setText("Control");
		/* Add Advanced option to menu bar */
		final MenuItem advancedMenuItem = new MenuItem(menuBar, SWT.CASCADE);
		advancedMenuItem.setText("Advanced");
		final MenuItem helpMenuItem = new MenuItem(menuBar, SWT.CASCADE);
		helpMenuItem.setText("Help");

		final Menu fileMenu = new Menu(mainShell, SWT.DROP_DOWN);
		fileMenuItem.setMenu(fileMenu);
		final Menu controlMenu = new Menu(mainShell, SWT.DROP_DOWN);
		controlMenuItem.setMenu(controlMenu);
		/* Add Advanced option to menu bar */
		final Menu advancedMenu = new Menu(mainShell, SWT.DROP_DOWN);
		advancedMenuItem.setMenu(advancedMenu);
		final Menu helpMenu = new Menu(mainShell, SWT.DROP_DOWN);
		helpMenuItem.setMenu(helpMenu);

		saveMenuItem = new MenuItem(fileMenu, SWT.PUSH);
		saveMenuItem.setText("Save Perspective");
		saveMenuItem.setEnabled(perspectiveListener != null);
		saveMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				try
				{
					if (perspectiveListener != null) {
						perspectiveListener.saveCalled();
					}
				} catch (final Exception e1)
				{
                    
					e1.printStackTrace();
				}
			}
		});

		saveAsMenuItem = new MenuItem(fileMenu, SWT.PUSH);
		saveAsMenuItem.setText("Save Perspective as...");
		saveAsMenuItem.setEnabled(perspectiveListener != null);
		saveAsMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				if (perspectiveListener != null) {
					perspectiveListener.saveAsCalled(mainShell);
				} 
			}
		});

		final MenuItem editConfigMenuItem = new MenuItem(fileMenu, SWT.PUSH);
		final MenuItem showTestConfigMenuItem = new MenuItem(fileMenu, SWT.PUSH);
		final MenuItem exitMenuItem = new MenuItem(fileMenu, SWT.PUSH);
		exitPerMenuItem = new MenuItem(fileMenu, SWT.PUSH);
		new MenuItem(fileMenu, SWT.SEPARATOR);
		final MenuItem abortMenuItem = new MenuItem(fileMenu, SWT.PUSH);
		abortMenuItem.setText("Abort Downlink...");
		abortMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				final boolean abort = SWTUtilities.showConfirmDialog(mainShell, "Abort Downlink Confirmation", "Aborting the downlink will cause this process to exit immediately.\n\n" +
						"It will not complete processing of telemetry in the pipeline.\nIt will not wait until database or message queues are empty.\n" +
						"This option should be used only if this process cannot be exited in another fashion.\n\n" +
						"Are you SURE you want to abort this process?");
				if (abort) {
					downApp.abort();
					System.exit(1);
				}
			}
		});
		final MenuItem aboutMenuItem = new MenuItem(helpMenu, SWT.PUSH);
		startMenuItem = new MenuItem(controlMenu, SWT.PUSH);
		stopMenuItem = new MenuItem(controlMenu, SWT.PUSH);
		pauseMenuItem = new MenuItem(controlMenu, SWT.PUSH);
		new MenuItem(controlMenu, SWT.SEPARATOR);
		final MenuItem logMenuItem = new MenuItem(controlMenu, SWT.PUSH);   
		new MenuItem(controlMenu, SWT.SEPARATOR);
		clearLadAllItem = new MenuItem(controlMenu, SWT.PUSH);
		clearLadAllItem.setEnabled(false);
		final MenuItem markSuspectChannelItem = new MenuItem(controlMenu, SWT.PUSH);
		final MenuItem unmarkSuspectChannelItem = new MenuItem(controlMenu, SWT.PUSH);
		// added constructor and set enabled for clearBufferItem
		clearBufferItem = new MenuItem(controlMenu, SWT.PUSH);
		clearBufferItem.setEnabled(false);

		/* Add Time System option to Advanced menu */
		final MenuItem timeSystemMenuItem = new MenuItem(advancedMenu, SWT.CASCADE);
		timeSystemMenuItem.setText("Time System");

		final Menu timeSystemSubMenu = new Menu(mainShell, SWT.DROP_DOWN);
		timeSystemMenuItem.setMenu(timeSystemSubMenu);

		final MenuItem latestTimeSystemMenuItem = new MenuItem(
				timeSystemSubMenu, SWT.RADIO);
		latestTimeSystemMenuItem.setText("Latest Received Data");
		
		/**
		 * Change to time system.  Adding all of the options and setting the
		 * default value from the config.
		 */
		final MenuItem ertTimeSystemMenuItem = new MenuItem(
				timeSystemSubMenu, SWT.RADIO);
		ertTimeSystemMenuItem.setText("ERT");

		final MenuItem scetTimeSystemMenuItem = new MenuItem(
				timeSystemSubMenu, SWT.RADIO);
		scetTimeSystemMenuItem.setText("SCET");

		final MenuItem sclkTimeSystemMenuItem = new MenuItem(
				timeSystemSubMenu, SWT.RADIO);
		sclkTimeSystemMenuItem.setText("SCLK");

		MenuItem selectedItem;
		switch(timeStrategy.getTimeComparisonStrategy()) {
		case ERT:
			selectedItem = ertTimeSystemMenuItem;
			break;
		case SCLK:
			selectedItem = sclkTimeSystemMenuItem;
			break;
		case SCET:
			selectedItem = scetTimeSystemMenuItem;
			break;
		case LAST_RECEIVED:
		default:
			/**
			 * Default is always last received.
			 */
			selectedItem = latestTimeSystemMenuItem;
			break;
		
		}
		
		selectedItem.setSelection(true);
		
		/* Add Time System option to Advanced menu */

		/* Add advanced menu item for saving LAD to a file */
		final MenuItem dumpLadMenuItem = new MenuItem(advancedMenu, SWT.CASCADE);
		dumpLadMenuItem.setText("Save LAD to File");

		exitMenuItem.setText("Exit Application");
		exitPerMenuItem.setText("Exit Perspective");
		aboutMenuItem.setText("About");

		editConfigMenuItem.setText("Configure...");
		showTestConfigMenuItem.setText("Show Session Configuration...");

		startMenuItem.setText("Start/Resume");
		stopMenuItem.setText("Stop");
		pauseMenuItem.setText("Pause");
		logMenuItem.setText("Make Log Entry...");
		clearLadAllItem.setText("Clear Channel LAD...");
		markSuspectChannelItem.setText("Mark Suspect Channels...");
		unmarkSuspectChannelItem.setText("Unmark Suspect Channels...");
		clearBufferItem.setText("Clear Buffered Data...");

		createToolbar();

		statusGroup = new Group(mainShell, SWT.NONE);
		final FormData sfd = new FormData();
		sfd.left = new FormAttachment(0);
		sfd.top = new FormAttachment(toolBar);
		sfd.height = 30;
		sfd.width= 80;
		statusGroup.setLayoutData(sfd);
		statusGroup.setLayout(new GridLayout(1, true));
		statusLabel = new Label(statusGroup, SWT.PUSH |SWT.CENTER);
		GridData gd = new GridData(GridData.FILL_BOTH);
		statusLabel.setLayoutData(gd);
		setStatusLabel();

		connectedGroup = new Group(mainShell, SWT.NONE);
		final FormData cfd = new FormData();
		cfd.left = new FormAttachment(statusGroup, 5);
		cfd.top = new FormAttachment(toolBar);
		cfd.height=30;
		cfd.width=100;
		connectedGroup.setLayoutData(cfd);
		connectedGroup.setLayout(new GridLayout(1, true));
		connectedLabel = new Label(connectedGroup, SWT.PUSH);
		gd = new GridData(GridData.FILL_BOTH);
		connectedLabel.setLayoutData(gd);
		setConnectedLabel(false);

		flowGroup = new Group(mainShell, SWT.NONE);
		final FormData fyfd = new FormData();
		fyfd.left = new FormAttachment(connectedGroup, 5);
		fyfd.top = new FormAttachment(toolBar);
		fyfd.height = 30;
		fyfd.width=80;
		flowGroup.setLayoutData(fyfd);
		flowGroup.setLayout(new GridLayout(1, true));
		flowLabel = new Label(flowGroup, SWT.PUSH);
		gd = new GridData(GridData.FILL_BOTH);
		flowLabel.setLayoutData(gd);
		setFlowLabel(false);

		if (appContext.getBean(IConnectionMap.class).getDownlinkConnection().getInputType().needsFrameSync()) {
			syncGroup = new Group(mainShell, SWT.NONE);
			final FormData syfd = new FormData();
			syfd.left = new FormAttachment(flowGroup, 5);
			syfd.top = new FormAttachment(toolBar);
			syfd.height = 30;
			syfd.width=80;
			syncGroup.setLayoutData(syfd);
			syncGroup.setLayout(new GridLayout(1, true));
			syncLabel = new Label(syncGroup, SWT.PUSH);
			gd = new GridData(GridData.FILL_BOTH);
			syncLabel.setLayoutData(gd);
			setSyncLabel(false);
		}

		processGroup = new Group(mainShell, SWT.NONE);
		final FormData prfd = new FormData();
		prfd.left = new FormAttachment(syncGroup == null ? flowGroup : syncGroup, 5);
		prfd.top = new FormAttachment(toolBar);
		prfd.height = 30;
		prfd.width=85;
		processGroup.setLayoutData(prfd);
		processGroup.setLayout(new GridLayout(1, true));
		processLabel = new Label(processGroup, SWT.PUSH);
		gd = new GridData(GridData.FILL_BOTH);
		processLabel.setLayoutData(gd);
		setProcessLabel(true,false);

		final Composite brandGroup = new Composite(mainShell, SWT.NONE);
		final FormData brfd = new FormData();
		brfd.right = new FormAttachment(100);
		brfd.top = new FormAttachment(0);
		brfd.height = 85;
		brfd.width= 95;
		brandGroup.setLayoutData(brfd);
		brandGroup.setLayout(new GridLayout(1, true));
		final Label brandLabel = new Label(brandGroup, SWT.PUSH);
		gd = new GridData(GridData.FILL_BOTH);
		brandLabel.setLayoutData(gd);
		final Image brandImage = getBrandingImage();        
		brandLabel.setImage(brandImage);

		DownMessageViewConfiguration viewConfig = null;
		if (displayConfig != null) {
			viewConfig = (DownMessageViewConfiguration)displayConfig.
					getViewConfig(ViewType.DOWN_MESSAGE);
		}
		if (viewConfig == null) {
			viewConfig = new DownMessageViewConfiguration(appContext);
			if (displayConfig != null) {
				displayConfig.addViewConfiguration(viewConfig);
			}
		}
		downApp.getDownConfiguration().setMessageViewConfig(viewConfig);

        table = new MessageTable(viewConfig, appContext.getBean(IContextKey.class), bus, sseFlag,
                                 appContext.getBean(GuiNotifier.class));
		table.init(mainShell);
		final FormData tfd = new FormData();
		tfd.left = new FormAttachment(0);
		tfd.top = new FormAttachment(statusGroup, 10);
		tfd.bottom = new FormAttachment(80);
		tfd.right = new FormAttachment(100);
		table.getMainControl().setLayoutData(tfd);

        statisticsComp = new DownStatisticsComposite(bus, appContext.getBean(PerformanceProperties.class), mainShell);
		final FormData stfd = new FormData();
		stfd.left = new FormAttachment(0);
		stfd.top = new FormAttachment(table.getMainControl(), 10);
		stfd.right = new FormAttachment(100);
		statisticsComp.setLayoutData(stfd);

		exitMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {

				stopApp(false);

				mainShell.close();
				System.exit(0);
			}
		});

		exitPerMenuItem.setEnabled(perspectiveListener != null);
		exitPerMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				if (perspectiveListener != null) {
					perspectiveListener.exitCalled();
				}
			}
		});

		aboutMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				displayAboutDialog();
			}
		});

		editConfigMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				displayConfigurationDialog();
			}
		});

		showTestConfigMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				displayTestConfigDialog();
			}
		});

		startMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				if (paused) {
					resumeApp();
				} else {
					startApp();
				}
			}
		});

		pauseMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				pauseApp();
			}
		});

		stopMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				stopApp(true);
			}
		});

		logMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				displayLogDialog();
			}
		});

		clearLadAllItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				clearAllLad();
			}
		});

		markSuspectChannelItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {
				    markSuspectChannel();
				} catch (final Exception ex) {
				    log.error("Unexpected error marking suspect channels: " 
				            + ExceptionTools.getMessage(ex), ex);
				}
			}
		});

		unmarkSuspectChannelItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
			    try {
                    unmarkSuspectChannel();
                } catch (final Exception ex) {
                    log.error("Unexpected error marking suspect channels: " 
                            + ExceptionTools.getMessage(ex), ex);
                }
			}
		});

		/**
		 * Add / change the menu items.
		 */
		latestTimeSystemMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				setTimeSystem(e, TimeComparisonStrategy.LAST_RECEIVED);
			}
		});
		
		ertTimeSystemMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				setTimeSystem(e, TimeComparisonStrategy.ERT);
			}
		});

		sclkTimeSystemMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				setTimeSystem(e, TimeComparisonStrategy.SCLK);
			}
		});
		
		scetTimeSystemMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				setTimeSystem(e, TimeComparisonStrategy.SCET);
			}
		});

		/* Add listener to dump lad menu item */
		dumpLadMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				dumpLadToFile();
			}
		});
		
		clearBufferItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e){
				clearInputStreamBuffer();
			}
		});

		enablePerspectiveMenuItems();

		populateFromConfig();
		mainShell.pack();
		
		ChillSize size = null;
		if (displayConfig != null) {
		    size = displayConfig.getSize();
		}

		if (size != null) {
		    mainShell.setSize(size.getXWidth(), size.getYHeight());
		} else {
		    mainShell.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
		}

		if (downApp.isAutoStart()) {
			startApp();
		}
	}

	/* (non-Javadoc)
	 * @see jpl.gds.perspective.gui.PerspectiveShell#perspectiveChanged()
	 */
	@Override
	public void perspectiveChanged() {
		enablePerspectiveMenuItems();
	}


	private void enablePerspectiveMenuItems() {
		final PerspectiveConfiguration pc = appContext.getBean(PerspectiveConfiguration.class);
		
		saveMenuItem.setEnabled(perspectiveListener != null &&
				pc.isWriteable() &&
				!pc.isLocked());
	}

	private void markSuspectChannel() {

		final ISuspectChannelService service = downApp.getSuspectChannelService();
		ISuspectChannelTable suspectTable;
		String filepath = null;
		List<String> suspectChannels = new ArrayList<>();
		if (service != null) {
		    suspectTable = service.getTable();
		} else {
		    suspectTable = ehaMessageFactory.createSuspectChannelsMessage().getSuspectTable();
		    suspectTable.init(appContext);
		}
		filepath = suspectTable.locateSuspectChannelFile();
		suspectChannels = suspectTable.getAllSuspectChannelIds();
		
		final IChannelUtilityDictionaryManager chanTable = downApp.getAppContext().getBean(IChannelUtilityDictionaryManager.class);
		if (chanTable.getChanIds().isEmpty()) {
		    try {
                if (sseFlag.isApplicationSse()) {
		            appContext.getBean(SseDictionaryLoadingStrategy.class)
                    .setChannel(true);
                    chanTable.loadSse(true);
		        } else {
		            appContext.getBean(FlightDictionaryLoadingStrategy.class)
	                .setChannel(true);
		            chanTable.loadFsw(true);
		        }
		    } catch (final DictionaryException e) {
		        SWTUtilities.showErrorDialog(mainShell, "Dictionary Load Failure", "Cannot load channel dictionary");
		        return;
		    }
		    
		}
		
		log.info("Suspect channel file is located in " + filepath);

		final MarkSuspectChannelShell scs = new MarkSuspectChannelShell(mainShell, filepath, suspectChannels, chanTable);
		scs.open();

		scs.getShell().addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent event) {
				try {
					if (!scs.wasCanceled()) {
						final String[] chanIds = scs.getSelectedChannels();
						final boolean isDn = scs.isDn();
						final boolean isEu = scs.isEu();
						final boolean isAlarm = scs.isAlarm();

						if (service == null) {

							//if user saved the file as something new we want to add the suspects from the old file to the new file
							for(int i=0; i<scs.suspectChans.length;i++)
							{
							    suspectTable.addSuspectDN(scs.suspectChans[i]);
							    suspectTable.addSuspectEU(scs.suspectChans[i]);
							    suspectTable.addSuspectAlarm(scs.suspectChans[i]);
							}

							suspectTable.setFileLocation(scs.getFilePath());
							suspectTable.parse(scs.getFilePath());
                            suspectTable.setDefaultFileLocation(scs.getFilePath());

						} else {
							service.getTable().setFileLocation(scs.getFilePath());
						}

						//add recently marked suspect channels
						if (chanIds != null) {
							for (final String chanId : chanIds) {
								if (isDn) {
								    suspectTable.addSuspectDN(chanId);
								}
								if (isEu) {
								    suspectTable.addSuspectEU(chanId);
								}
								if (isAlarm) {
								    suspectTable.addSuspectAlarm(chanId);
								}
							}
						}
						final ISuspectChannelsMessage scm = ehaMessageFactory.createSuspectChannelsMessage(suspectTable);
					    bus.publish(scm);
					    suspectTable.save();
					}
				} catch (final Exception ex) {
				    log.error("Unexpected exception processing suspect channels: " + ExceptionTools.getMessage(ex), ex);
				}
			}
		});
	}


	private void unmarkSuspectChannel() {
		final ISuspectChannelService service = downApp.getSuspectChannelService();
		ISuspectChannelTable suspectTable;
		String filepath = null;
		List<String> suspectChannels = new ArrayList<>();
		if (service != null) {
		    suspectTable = service.getTable();
		} else {
		    suspectTable =  ehaMessageFactory.createSuspectChannelsMessage().getSuspectTable();
		    suspectTable.init(appContext);
		}
	    filepath = suspectTable.locateSuspectChannelFile();
        suspectChannels = suspectTable.getAllSuspectChannelIds();
        
        final IChannelUtilityDictionaryManager chanTable = downApp.getAppContext().getBean(IChannelUtilityDictionaryManager.class);
        if (chanTable.getChanIds().isEmpty()) {
            try {
                if (sseFlag.isApplicationSse()) {
                    appContext.getBean(SseDictionaryLoadingStrategy.class)
                    .setChannel(true);
                    chanTable.loadSse(true);
                } else {
                    appContext.getBean(FlightDictionaryLoadingStrategy.class)
                    .setChannel(true);
                    chanTable.loadFsw(true);
                }
                chanTable.loadAll(false);
            } catch (final DictionaryException e) {
                SWTUtilities.showErrorDialog(mainShell, "Dictionary Load Failure", "Cannot load channel dictionary");
                return;
            }
            
        }
        
        log.info("Suspect channel file is located in " + filepath);

		final UnmarkSuspectChannelShell scs = new UnmarkSuspectChannelShell(mainShell, filepath, suspectChannels);
		scs.open();

		scs.getShell().addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent event) {
				try {
					if (!scs.wasCanceled()) {
						final String[] chanIds = scs.getSelectedChannels();


						if (service == null) {

							//if user saved the file as something new we want to add the suspects from the old file to the new file
							for(int i=0; i<scs.suspectChans.length;i++)
							{
							    suspectTable.addSuspectDN(scs.suspectChans[i]);
							    suspectTable.addSuspectEU(scs.suspectChans[i]);
							    suspectTable.addSuspectAlarm(scs.suspectChans[i]);
							}

							suspectTable.setFileLocation(scs.getFilePath());

							suspectTable.parse(scs.getFilePath());
                            suspectTable.setDefaultFileLocation(scs.getFilePath());

						} else {
							service.getTable().setFileLocation(scs.getFilePath());
						}
						if (chanIds != null) {
							for (final String chanId: chanIds) {
							    suspectTable.removeSuspectDN(chanId);
							    suspectTable.removeSuspectEU(chanId);
							    suspectTable.removeSuspectAlarm(chanId);
							}
						}
						final ISuspectChannelsMessage scm = ehaMessageFactory.createSuspectChannelsMessage(suspectTable);
						bus.publish(scm);
						suspectTable.save();
					}
				} catch (final Exception ex) {				    
				    log.error("Unexpected exception processing suspect channels: " + ExceptionTools.getMessage(ex), ex);
				}
			}
		});
	}

    private void clearAllLad() {

		/* Replaced ConfirmationShell with SWTUtilies dialog box. Added alarm warning.
		 */
		final boolean ok = SWTUtilities.showConfirmDialog(mainShell, 
				"Confirm Clear Channel LAD",
				"This operation will clear all values from the downlink " +
						"local LAD for this session.\n\n" +
						"If telemetry is flowing, results of alarm calculations may " +
						"be unpredictable.\n\n" +
				ARE_YOU_SURE);

		if (!ok) {
			return;
		}
		try {

			// note that clearing the ChannelLad returns a "clear time". Can be
			// used to sync up clearing of other LAD tables.
			/* Pass AlarmNotifierService to clearAll so
			 * it may be cleared as well */
		    downApp.clearChannelState();		  

			// We no longer want to clear the Global LAD when the chill_down LAD is cleared
			SWTUtilities.showMessageDialog(mainShell, "LAD Cleared",
					"The downlink local LAD has been successfully cleared.");
		} catch (final Exception e) {
			SWTUtilities.showErrorDialog(
					mainShell,
					"Error clearing LAD",
					"There was an error attempting to clear the downlink local LAD: "
							+ e.toString());
		}
	}

	/* Add private method for taking care of time  system button listeners */
	private void setTimeSystem(final SelectionEvent e, final TimeComparisonStrategy timeComparisonStrategy) {
		final MenuItem item = (MenuItem) e.widget;

		if(item.getSelection() && item.getText().equals(timeComparisonStrategy.getDisplayName())) {
			// user clicked option that was already selected
		    /*  Use TimeComparisonStrategy rather than ChannelUtility. */
			if(timeStrategy.getTimeComparisonStrategy() == timeComparisonStrategy) {
				return;
			}

			final boolean ok = SWTUtilities.showConfirmDialog(mainShell, 
					"Confirm Change Time System", 
					"This action will change the current time system to " + 
							timeComparisonStrategy.getDisplayName() + ".\n\n" +
							"If telemetry is flowing, results of alarm " +
							"calculations and derived channel computation may be " +
							"unpredictable.\n\n" +
					ARE_YOU_SURE);

			if (!ok) {
				item.setSelection(false);
				final MenuItem[] items = item.getParent().getItems();
				for(final MenuItem menuItem : items) {
					if(menuItem.getText().equals(timeStrategy.getTimeComparisonStrategy().getDisplayName())) {
						menuItem.setSelection(true);
					}
				}

				return;
			}

            downApp.setTimeComparisonStrategy(timeComparisonStrategy);
		}
	}

	/* Save LAD to file */
	private void dumpLadToFile() {

		final String directory = GdsSystemProperties.getUserConfigDir();

		final IAccurateDateTime timestamp = new AccurateDateTime(System.currentTimeMillis());
		String time = timestamp.getFormattedErt(true);
		/* Replace slashes and colons with dashes */
		time = time.replace('/', '-').replace(':', '-');
		final String hostname = testConfig.getContextId().getHost();
		if(testConfig.getContextId() == null ||  testConfig.getContextId().getNumber() == null) {
			SWTUtilities.showErrorDialog(mainShell, 
					"Error Saving LAD to File", 
					"There was an error saving the local LAD. The session has not started.");
			return;
		}
		final String sessionId = testConfig.getContextId().getNumber().toString();
		final String filename = directory + "/ChillDownLadDump_" + time + "_" + hostname + "_" + sessionId + ".csv";

		if (downApp.saveLadToFile(filename)) {
		
		    
		    SWTUtilities.showMessageDialog(mainShell, 
		            "LAD Saved to File", 
		            "The contents of the local LAD have been saved to " + filename);
		} else {
			SWTUtilities.showErrorDialog(mainShell, 
					"Error Saving LAD to File", 
					"There was an error saving the local LAD to " + filename);
			return;
		}
	}
	
	/**
	 * Function called when the menu option to clear the input buffer is
	 * selected
	 */
	private void clearInputStreamBuffer(){
		final boolean ok = SWTUtilities.showConfirmDialog(mainShell, 
				"Confirm Clear Buffered Raw Input Data ", 
				"This action will remove all buffered data." + 
						"All removed buffered data has not been processed" +
						" and will be lost.\n\n" +
				ARE_YOU_SURE);
		if(ok){
			try{
				downApp.clearInputStreamBuffer();
				log.info("Input buffer cleared by user.");

			}
			catch(final Exception e){
				log.warn(e.getMessage());

				//  updated pop-up message title
				SWTUtilities.showMessageDialog(mainShell, "Clear Buffered Data", e.getMessage());
			}
		}
	}

	/**
	 * Loads the branding image.
	 * @return the Image object
	 */
	protected abstract Image getBrandingImage();

	/**
	 * Start the downlink data processing.
	 *
	 */
    public void startApp() {
		/*  Name the thread. */
		appThread = new Thread(downApp, "GUI Downlink Runner");
		//  R8 Refactor - Commenting out everything related to session restart
//		if (stopped) {
//			final boolean resetSessionNumber = !GdsSystemProperties.applicationIsSse() ||
//					(GdsSystemProperties.applicationIsSse() && !GdsSystemProperties.isIntegratedGui());
//			/* Handle the case where the session reset fails */
//			resetFailed = !downApp.resetSession(resetSessionNumber);
//			hasSessionChanged = canRestartSession() && !resetFailed;
//			if (resetFailed) {
//				enableControls();
//				return;
//			}
//			statisticsComp.resetCounters();
//		}
		subscribe();
		appThread.start();
		stopped = false;
		paused = false;
		started = true;
		firstRun = false;
		clearLadAllItem.setEnabled(true);
		neverStarted = false;
        /** call enableControls() after setting everything else */
        enableControls();
	}

	/**
     * Stops downlink data processing.
     *
     * @param prompt
     *            indicating whether to prompt for confirmation before stopping the session.
     * @return true if the processing was stopped, false if not (user opted out)
     * 
     *  made stopApp() public so that it can be accessed from RESTful interface
     */
    public boolean stopApp(final boolean prompt) {
		if (!prompt || SWTUtilities.showConfirmDialog(mainShell, "Stop Confirmation", 
				"Are you sure you wish to stop this session?")) {
			try { 
                log.debug("About to call stop()");

				downApp.stop();
                log.debug("stop() returned");

			} catch (final IllegalStateException e) {
				/* cannot ensure everything was
				 * stopped, don't update the user display and let the user
				 * know the reason. No longer print stack trace.
				 */
				final IMessage isem = appContext.getBean(IStatusMessageFactory.class).createPublishableLogMessage(TraceSeverity.WARN,"Cannot stop downlink application at this time. Reason: " + e.getMessage());
				bus.publish(isem);
                log.warn(isem);
				return false;
			}
			stopped = true;

            // Added interrupt for statistics updater thread
            if (statisticsComp != null) {
                statisticsComp.stopUpdateThread();
            }
			appThread = null;
			enableControls();
            resetAll(true);
			return true;
		}
		return false;
	}

	/**
	 * Pauses downlink data processing.
	 *
	 */
    public void pauseApp() {
		try {
			downApp.pause();
		} catch (final IllegalStateException e) {
			e.printStackTrace();
		}
		paused = true;
		enableControls();
	}

	/**
	 * Resets the application for another session run.
	 * @param newRun true if the session can be started again, false if not
	 */
	protected void resetAll(final boolean newRun) {
		if (newRun) {
			started = false;
			paused = false;

		}
		if (!mainShell.isDisposed()) {
			setSyncLabel(false);
			setConnectedLabel(false);
			setFlowLabel(false);
			setStatusLabel();
			enableControls();
		}
	}

	/**
	 * Resumes downlink data processing.
	 *
	 */
    public void resumeApp() {
		try { 
			downApp.resume();
		} catch (final IllegalStateException e) {
		    log.error("Unexpected exception resuming processing: " + ExceptionTools.getMessage(e), e);
		}
		paused = false;
		enableControls();
	}

	/**
	 * Sets the application status indicator based upon current state.
	 *
	 */
	protected void setStatusLabel() {
		if (statusLabel.isDisposed()) {
			return;
		}
		if (firstRun) {
			statusLabel.setText("  Ready");
			statusLabel.setBackground(greenColor);
			statusGroup.setBackground(greenColor);
			statusLabel.setForeground(blackColor);
		} else if (started && !paused) {
			statusLabel.setText(" Running");
			statusLabel.setBackground(greenColor);
			statusGroup.setBackground(greenColor);
			statusLabel.setForeground(blackColor);
		} else if (started && paused) {
			statusLabel.setText("  Paused");
			statusLabel.setBackground(yellowColor);
			statusGroup.setBackground(yellowColor);
			statusLabel.setForeground(blackColor);
		} else if (!started) {
			statusLabel.setText("  Stopped");
			statusLabel.setBackground(redColor);
			statusGroup.setBackground(redColor);
			statusLabel.setForeground(whiteColor);
		}
	}

	/**
	 * Sets the connection status indicator based upon current state.
	 * @param connected true if data source is currently connected; false
	 * otherwise
	 */
	protected void setConnectedLabel(final boolean connected) {
		if (connectedLabel.isDisposed()) {
			return;
		}
		if (connected) {
			connectedLabel.setText("  Connected");
			connectedLabel.setBackground(greenColor);
			connectedGroup.setBackground(greenColor);
			connectedLabel.setForeground(blackColor);           
		} else {
			connectedLabel.setText("  No Connect");
			connectedLabel.setBackground(redColor);
			connectedGroup.setBackground(redColor);
			connectedLabel.setForeground(whiteColor);
		}
	}


	/**
	 * Sets the in sync status indicator based upon current state.
	 * @param inSync true if the data stream is currently in sync; false
	 * otherwise
	 */
	protected void setSyncLabel(final boolean inSync) {
		if (syncGroup == null || syncLabel.isDisposed()) {
			return;
		}
		if (inSync) {
			syncLabel.setText("   In Sync");
			syncLabel.setBackground(greenColor);
			syncGroup.setBackground(greenColor);
			syncLabel.setForeground(blackColor);
		} else {
			syncLabel.setText("   No Sync");
			syncLabel.setBackground(redColor);
			syncGroup.setBackground(redColor);
			syncLabel.setForeground(whiteColor);
		}
	}

	/**
	 * Sets the data flow status indicator based upon current state.
	 * @param gotFlow true if the data stream is currently flowing; false
	 * otherwise
	 */ 
	protected void setFlowLabel(final boolean gotFlow) {
		if (flowLabel.isDisposed()) {
			return;
		}
		if (gotFlow) {
			flowLabel.setText("  Flowing");
			flowLabel.setBackground(greenColor);
			flowGroup.setBackground(greenColor);
			flowLabel.setForeground(blackColor);
		} else {
			flowLabel.setText("  No Flow");
			flowLabel.setBackground(redColor);
			flowGroup.setBackground(redColor);
			flowLabel.setForeground(whiteColor);
		}
	}

	/**
	 * Sets the data processing status indicator based upon current state.
	 * @param waiting true if no processing has happened yet
	 * @param processing true if processing is happening
	 */ 
	protected void setProcessLabel(final boolean waiting, final boolean processing) {
		if (processLabel.isDisposed()) {
			return;
		}
		if (waiting) {
			processLabel.setText("  Waiting");
			processLabel.setBackground(yellowColor);
			processGroup.setBackground(yellowColor);
			processLabel.setForeground(blackColor);
		} else {
			if (processing) {
				processLabel.setText("Processing");
				processLabel.setBackground(greenColor);
				processGroup.setBackground(greenColor);
				processLabel.setForeground(blackColor);
			} else {
				processLabel.setText("  Done");
				processLabel.setBackground(redColor);
				processGroup.setBackground(redColor);
				processLabel.setForeground(whiteColor);
			}
		}
	}

	/**
	 * Creates the toolbar.
	 *
	 */
	private void createToolbar() {
		toolBar = new ToolBar(mainShell, SWT.HORIZONTAL | SWT.SHADOW_OUT);
		final ToolItem editConfigToolItem = new ToolItem(toolBar, SWT.PUSH | SWT.BORDER);
		final ToolItem testConfigToolItem = new ToolItem(toolBar, SWT.PUSH | SWT.BORDER);
		final ToolItem clearToolItem = new ToolItem(toolBar, SWT.PUSH | SWT.BORDER);
		new ToolItem(toolBar, SWT.SEPARATOR);
		startToolItem = new ToolItem(toolBar, SWT.PUSH | SWT.BORDER);
		stopToolItem = new ToolItem(toolBar, SWT.PUSH | SWT.BORDER);
		pauseToolItem = new ToolItem(toolBar, SWT.PUSH | SWT.BORDER);
		new ToolItem(toolBar, SWT.SEPARATOR);
		final ToolItem logToolItem = new ToolItem(toolBar, SWT.PUSH | SWT.BORDER);
		final Image editImage = SWTUtilities.createImage(mainDisplay, "jpl/gds/down/gui/EditConfig1.gif");
		editConfigToolItem.setImage(editImage);
		editConfigToolItem.setToolTipText("Edit Downlink Configuration");
		final Image testImage = SWTUtilities.createImage(mainDisplay, "jpl/gds/down/gui/voyager.gif");
		testConfigToolItem.setImage(testImage);
		testConfigToolItem.setToolTipText("Show Session Configuration");
		final Image clearImage = SWTUtilities.createImage(mainDisplay, "jpl/gds/down/gui/Clear.gif");
		clearToolItem.setImage(clearImage);
		clearToolItem.setToolTipText("Clear Messages");
		final Image startImage = SWTUtilities.createImage(mainDisplay, "jpl/gds/down/gui/Play.gif");
		startToolItem.setImage(startImage);
		startToolItem.setToolTipText("Start Processing");
		final Image stopImage = SWTUtilities.createImage(mainDisplay, "jpl/gds/down/gui/Stop3.gif");
		stopToolItem.setImage(stopImage);
		stopToolItem.setToolTipText("Stop Processing");
		final Image pauseImage = SWTUtilities.createImage(mainDisplay, "jpl/gds/down/gui/Pause.gif");
		pauseToolItem.setImage(pauseImage);
		pauseToolItem.setToolTipText("Pause Processing");    
		final Image logImage = SWTUtilities.createImage(mainDisplay, "jpl/gds/down/gui/Pencil.gif");
		logToolItem.setImage(logImage);
		logToolItem.setToolTipText("Make Log Entry"); 

		editConfigToolItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				displayConfigurationDialog();
			}
		});

		testConfigToolItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				displayTestConfigDialog();
			}
		});

		clearToolItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				table.clearView();
			}
		});

		startToolItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				if (paused) {
					resumeApp();
				} else {
					startApp();
				}
			}
		});

		pauseToolItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				pauseApp();
			}
		});

		stopToolItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				stopApp(true);
			}
		});

		logToolItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				displayLogDialog();
			}
		});
	}

	/**
	 * Enables/disables controls based upon current state.
	 */
	protected void enableControls() {
		/* Add checks for reset failure state */
	    //  R8 Refactor - Commenting out everything related to session restart
		//startMenuItem.setEnabled(!resetFailed && ((neverStarted && !started) || (!started && canRestartSession()) || paused));
	    /* 
	     * Now that session restart is disabled, we only want the
	     * start menu item enabled if the session was never started and was not autostarted,
	     * or if it is paused.
	     */
		startMenuItem.setEnabled((!downApp.isAutoStart() && neverStarted) || paused);
		stopMenuItem.setEnabled(!stopped && started);
		pauseMenuItem.setEnabled(!stopped && started && !paused);
		// R8 Refactor - Commenting out everything related to session restart
		//startToolItem.setEnabled(!resetFailed && ((neverStarted && !started) || (!started && canRestartSession()) || paused));
		 /* 
         * Now that session restart is disabled, we only want the
         * start tool button enabled if the session was never started and was not autostarted,
         * or if it is paused.
         */
		startToolItem.setEnabled((!downApp.isAutoStart() && neverStarted) || paused);
		stopToolItem.setEnabled(!stopped && started);
		pauseToolItem.setEnabled(!stopped && started && !paused);
		setStatusLabel();
		
		// added to control enabling of buffer menu option
		final IDownlinkConnection dc = appContext.getBean(IConnectionMap.class).getDownlinkConnection();
		clearBufferItem.setEnabled(appContext.getBean(TelemetryInputProperties.class).
                                             isBufferedInputAllowed(dc.getDownlinkConnectionType(), sseFlag)
                && !stopped && started);
	}

	/**
	 * Convinces the GUI the session has stopped without it receiving an
	 * EndOfSession message. This puts the GUI back into a state in which the
	 * session can be restarted.
	 * 
	 */
	public void forceSessionStop() {
		if (!mainShell.isDisposed()) {
			mainShell.getDisplay().asyncExec(new Runnable () {
				@Override
				public void run () {
					try {
					    //  R8 Refactor - Commenting out everything related to session restart
						//resetAll(true);
						resetAll(false);
						//awaitingStop = false;
					} catch (final Exception ex) {
				        log.error("Unexpected exception restarting session: " + ExceptionTools.getMessage(ex), ex);
					}
				}
			});
		}
	}

	/**
	 * Creates subscriptions for internal messages generated by the downlink 
	 * application.
	 *
	 */
	protected void subscribe() {
		if (subscribed || downApp == null) {
			return;
		}
		final IMessagePublicationBus context = bus;

		context.subscribe(CommonMessageType.Log, new BaseMessageHandler() {
			@Override
			public void handleMessage(final IMessage m) {
                if (mainShell.isDisposed()) {
                    return;
                }
                try {
                    mainShell.getDisplay().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (((ILogMessage) m).getLogType().equals(LogMessageType.CONNECT)) {
                                    setConnectedLabel(true);
                                }
                                else if (((ILogMessage) m).getLogType().equals(LogMessageType.DISCONNECT)) {
                                    setConnectedLabel(false);
                                    setFlowLabel(false);
                                    setSyncLabel(false);
                                    setProcessLabel(true, false);
                                }
                            }
                            catch (final Exception ex) {
                                log.error("Unexpected exception handling log message: " + ExceptionTools.getMessage(ex), ex); 
                            }
                        }
                    });
                }
                catch (final SWTException e) {
                    // ignore
                }
			}
		});

        context.subscribe(TmServiceMessageType.InSync, new BaseMessageHandler() {
			@Override
			public void handleMessage(final IMessage m) {
				if (mainShell.isDisposed()) {
					return;
				}
				mainShell.getDisplay().asyncExec(new Runnable () {
					@Override
					public void run () {
						try {
							setSyncLabel(true);
						} catch (final Exception ex) {
					        log.error("Unexpected exception handling insync message: " + ExceptionTools.getMessage(ex), ex);
						}
					}
				});
			}
		});

        context.subscribe(TmServiceMessageType.LossOfSync, new BaseMessageHandler() {
			@Override
			public void handleMessage(final IMessage m) {
				if (mainShell.isDisposed()) {
					return;
				}
				mainShell.getDisplay().asyncExec(new Runnable () {
					@Override
					public void run () {
						try {
							setSyncLabel(false);
						} catch (final Exception ex) {
	                        log.error("Unexpected exception handling loss of sync message: " + ExceptionTools.getMessage(ex), ex);
						}
					}
				});
			}
		});

		context.subscribe(CommonMessageType.StartOfData, new BaseMessageHandler() {
			@Override
			public void handleMessage(final IMessage m) {
				if (mainShell.isDisposed()) {
					return;
				}
				mainShell.getDisplay().asyncExec(new Runnable () {
					@Override
					public void run () {
						try {
							setFlowLabel(true);
							setProcessLabel(false,true);
						} catch (final Exception ex) {
	                        log.error("Unexpected exception handling start of data message: " + ExceptionTools.getMessage(ex), ex);
						}
					}
				});
			}
		});

        context.subscribe(SessionMessageType.StartOfSession, new BaseMessageHandler() {
			@Override
			public void handleMessage(final IMessage m) {
				if (mainShell.isDisposed()) {
					return;
				}
				mainShell.getDisplay().asyncExec(new Runnable () {
					@Override
					public void run () {
						try {
							populateFromConfig();
							//  R8 Refactor - Commenting out everything related to session restart
//							if (hasSessionChanged && perspectiveListener != null) {
//								perspectiveListener.newSessionStarted();
//							}
//							hasSessionChanged = false;
							setProcessLabel(true,false);
						} catch (final Exception ex) {
	                        log.error("Unexpected exception handling start of session message: " + ExceptionTools.getMessage(ex), ex);
						}
					}
				});
			}
		});

        context.subscribe(TmInputMessageType.TelemetryInputSummary, new BaseMessageHandler() {
			@Override
			public void handleMessage(final IMessage m) {
				if (mainShell.isDisposed()) {
					return;
				}
				final ITelemetrySummaryMessage msg = (ITelemetrySummaryMessage)m;
				final boolean connected = msg.isConnected();
				final boolean flowing = msg.isFlowing();
				mainShell.getDisplay().asyncExec(new Runnable () {
					@Override
					public void run () {
						try {
							setFlowLabel(flowing);
							setConnectedLabel(connected);
						} catch (final Exception ex) {
	                        log.error("Unexpected exception handling telemetry input summary message: " + ExceptionTools.getMessage(ex), ex);
						}
					}
				});
			}
		});

        context.subscribe(SessionMessageType.EndOfSession, new BaseMessageHandler() {
			@Override
			public void handleMessage(final IMessage m)
			{
				if ((m instanceof EndOfSessionMessage) &&
						((EndOfSessionMessage) m).isRemote())
				{
					// Ignore if remote
					return;
				}

				if (!mainShell.isDisposed()) {
					mainShell.getDisplay().asyncExec(new Runnable () {
						@Override
						public void run () {
							try {
								resetAll(true);
								final TelemetrySummaryShell summary = new TelemetrySummaryShell(downApp.getSessionSummary(), mainShell,
                                                                                                appContext.getBean(IMySqlAdaptationProperties.class).getDatabaseName(),
                                                                                                sseFlag);
								summary.open();
								//awaitingStop = false;
							} catch (final Exception ex) {
		                        log.error("Unexpected exception handling end of session message: " + ExceptionTools.getMessage(ex), ex);
							}
						}
					});
				}
			}
		});


		context.subscribe(CommonMessageType.EndOfData, new BaseMessageHandler() {
			@Override
			public void handleMessage(final IMessage m) {
				if (mainShell.isDisposed()) {
					return;
				}
				mainShell.getDisplay().asyncExec(new Runnable () {
					@Override
					public void run () {
						try {
							setFlowLabel(false);
							setSyncLabel(false);
							stopped = true;
							setStatusLabel();
						} catch (final Exception ex) {
	                        log.error("Unexpected exception handling end of data message: " + ExceptionTools.getMessage(ex), ex);
						}
					}
				});
			}
		});

        context.subscribe(SessionMessageType.EndOfSession, new BaseMessageHandler() {
			@Override
			public void handleMessage(final IMessage m) {
				if (mainShell.isDisposed()) {
					return;
				}
				mainShell.getDisplay().asyncExec(new Runnable () {
					@Override
					public void run () {
						try {
							setConnectedLabel(false);
							setFlowLabel(false);
							setSyncLabel(false);
							stopped = true;
							setStatusLabel();
							setProcessLabel(false,false);
						} catch (final Exception ex) {
	                        log.error("Unexpected exception handling end of session message: " + ExceptionTools.getMessage(ex), ex);
						}
					}
				});
			}
		});

		subscribed = true;

	}

	//  R8 Refactor - Commenting out everything related to session restart
//	/**
//	 * {@inheritDoc}
//	 * @see jpl.gds.perspective.gui.PerspectiveShell#sessionChanged(jpl.gds.config.SessionConfiguration)
//	 */
//	@Override
//	public void sessionChanged(final SessionConfiguration config) {
//		if (started) {
//			IPublishableLogMessage lm = StatusMessageFactory.createPublishableLogMessage(TraceSeverity.INFO,
//                    "Session was restarted by partner application. Stopping session " + 
//							SessionConfiguration.getGlobalInstance().getContextId().getNumber() + ".");
//			bus.publish(lm);
//			awaitingStop = true;
//			final boolean ok = stopApp(false);
//			if (!ok) {
//				lm = StatusMessageFactory.createPublishableLogMessage(
//                        TraceSeverity.FATAL, "Session could not be stopped");
//				bus.publish(lm);
//			} else {
//
//				final Timer restartTimer = new Timer("Session Stop Wait Timer");
//				restartTimer.schedule(new TimerTask() {
//
//					@Override
//					public void run()
//					{
//
//						while(awaitingStop) {
//							try {
//								Thread.sleep(500);
//							} catch (final InterruptedException e) {}
//						}
//						restartWithNewConfig(config);
//					}
//
//				}, 1000);
//			}
//		} else {
//			restartWithNewConfig(config);
//		}
//	}

	//  R8 Refactor - Commenting out everything related to session restart
//	/**
//	 * Configures the GUI window for a session restart.
//	 * @param config the new SessionConfiguration
//	 * 
//	 */
//	protected void restartWithNewConfig(final SessionConfiguration config) {
//		SessionConfiguration.setGlobalInstance(config);
//		SWTUtilities.safeAsyncExec(
//				mainShell.getDisplay(),
//				Log4jTracer.getDefaultTracer(),

//				"Session Restart",
//				new Runnable ()
//				{
//					@Override
//					public void run()
//					{
//
//						try {
//							final IPublishableLogMessage lm = StatusMessageFactory.createPublishableLogMessage(
//                                    TraceSeverity.INFO, "Automatically starting session " +  config.getContextId().getNumber() + ".");
//							bus.publish(lm);
//
//							downApp.setSessionConfiguration(config, false);
//							// Write out session configuration.
//							// Add flag to call to control whether config is written
//							AbstractDownlinkApp.writeOutSessionConfig(appContext, config, true);
//							configurationChanged(config);
//							if (!neverStarted) {
//								startApp();
//							}
//
//						} catch (final Exception e) {
//							e.printStackTrace();
//						}
//					}
//				});
//	}


	/**
	 * Sets GUI fields based upon current application configuration.
	 */
	protected void populateFromConfig()
	{
		final StringBuilder sb = new StringBuilder(getTitle());

		sb.append(": ").append(appContext.getBean(IContextIdentification.class).getName());

		if (appContext.getBean(IContextIdentification.class).getNumber() != null)
		{
			sb.append(" (").append(appContext.getBean(IContextIdentification.class).getNumber());
			sb.append(')');

		}

		mainShell.setText(sb.toString());

		enableControls();
	}


	/**
	 * Displays the "about" window for the application.
	 */
	protected void displayAboutDialog() {
		AboutUtility.showStandardAboutDialog(mainShell, appContext.getBean(GeneralProperties.class));
	}

	/**
	 * Displays the edit downlink configuration window.
	 *
	 */
	protected void displayConfigurationDialog() {
		final DownConfigShell configShell = new DownConfigShell(mainShell, downApp.getDownConfiguration());
		configShell.getShell().addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent event) {
				try {
					if (!configShell.wasCanceled()) {
						table.setViewConfig(downApp.getDownConfiguration().getMessageViewConfig());
					}
				} catch (final Exception ex) {
                    log.error("Unexpected exception displaying downlink config window: " + ExceptionTools.getMessage(ex), ex);
				}
			}
		});
		configShell.open();
	}

	/**
	 * Displays the test configuration window.
	 *
	 */
    public void displayTestConfigDialog() {
		final SessionConfigViewShell configShell = new SessionConfigViewShell(appContext, mainShell);
		configShell.setContextConfiguration(testConfig);
		configShell.open();
	}

	/**
	 * Displays the log entry window.
	 */
	protected void displayLogDialog() {
		final LogEntryShell logShell = new LogEntryShell(mainShell, appContext);
		logShell.open();
		while (!logShell.getShell().isDisposed()) {
			if (!mainDisplay.readAndDispatch()) {
				mainDisplay.sleep();
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean wasCanceled() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setDisplayConfiguration(final DisplayConfiguration config)
	{
		displayConfig = config;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void updateConfiguration() 
	{
		final Point size = mainShell.getSize();
		final ChillSize csize = new ChillSize(size.x, size.y);
		final Point location = mainShell.getLocation();
		final ChillLocation clocation = new ChillLocation(location.x, location.y);
		final String title = mainShell.getText();

		displayConfig.setSize(csize);
		displayConfig.setName(title);
		displayConfig.setLocation(clocation);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setPerspectiveListener(final PerspectiveListener listener)
	{
		perspectiveListener = listener;
		final PerspectiveConfiguration pc = appContext.getBean(PerspectiveConfiguration.class);
		if (perspectiveListener != null && saveAsMenuItem != null) {
			saveAsMenuItem.setEnabled(true);
			saveMenuItem.setEnabled(pc.isWriteable() && 
					!pc.isLocked());
			exitPerMenuItem.setEnabled(true);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DisplayConfiguration getDisplayConfiguration()
	{
		return displayConfig;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void exitShell() {
		if (!stopped) {
			final boolean stop = this.stopApp(false);
			if (stop) {
				mainShell.dispose();
			}
		} else {
			mainShell.dispose();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void configurationChanged(final SessionConfiguration config)
	{
		testConfig = config;

		final StringBuilder title = new StringBuilder(getTitle() + ": " + testConfig.getContextId().getName());
		new Thread ()
		{
			@Override
			public void run() {

				if (testConfig.getContextId().getNumber() != null)
				{

					title.append(" (").append(testConfig.getContextId().getNumber());
					title.append(')');

				}

				if (mainDisplay.isDisposed())
				{
					return;
				}

				mainDisplay.syncExec(new Runnable() {
					@Override
					public void run() {
						try {
							if (mainShell.isDisposed()) {
								return;
							}

							if (!mainShell.isVisible()) {
								return;
							}

							mainShell.setText(title.toString());
						}
						catch(final Exception e)
						{
							e.printStackTrace();
							return;
						}
					}
				});
			}
		}.start();  
	}

    /**
     * @return true if app has never been started, false if it hass
     */
    public boolean isNeverStarted() {
        return neverStarted;
    }

    /**
     * @return true if app is started (processing), false if not
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * @return true if app is paused, false if not
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * @return true if app is stopped (not processing), false if not
     */
    public boolean isStopped() {
        return stopped;
    }

    /**
     * @return returns the Downlink Processing State based upon the GUI's status
     */
    public DownlinkProcessingState getProcessingState() {
        DownlinkProcessingState state = DownlinkProcessingState.UNKNOWN;
        if (neverStarted) {
            state = DownlinkProcessingState.UNBOUND;
        }
        else if (stopped) {
            state = DownlinkProcessingState.BOUND;
        }
        else if (paused) {
            state = DownlinkProcessingState.PAUSED;
        }
        else if (started) {
            state = DownlinkProcessingState.STARTED;
        }
        return state;
    }

	//  R8 Refactor - Commenting out everything related to session restart
//	/** 
//	 * Indicates whether the user can restart a session from this GUI.
//	 * @return true if restart allowed, false if not
//	 */
//	public abstract boolean canRestartSession();

}
