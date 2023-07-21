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
package jpl.gds.monitor.guiapp.gui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.message.api.external.CheckMessageService;
import jpl.gds.monitor.config.GlobalPerspectiveParameter;
import jpl.gds.monitor.config.IMonitorConfigChangeListener;
import jpl.gds.monitor.config.MonitorConfigValues;
import jpl.gds.monitor.config.MonitorConfigValues.SclkFormat;
import jpl.gds.monitor.guiapp.MonitorTimers;
import jpl.gds.monitor.guiapp.gui.views.WarningUtil;
import jpl.gds.monitor.perspective.view.SingleWindowViewConfiguration;
import jpl.gds.monitor.perspective.view.TabularViewConfiguration;
import jpl.gds.monitor.perspective.view.channel.MonitorChannelLad;
import jpl.gds.perspective.DisplayConfiguration;
import jpl.gds.perspective.PerspectiveCounters;
import jpl.gds.perspective.PromptSettings;
import jpl.gds.perspective.gui.PerspectiveListener;
import jpl.gds.perspective.gui.PerspectiveShell;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.perspective.view.ViewReference;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.swt.ConfirmationShell;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.types.ChillLocation;
import jpl.gds.shared.swt.types.ChillSize;


/**
 * MonitorShell is the main SWT GUI class for the message monitor. It is responsible for 
 * instantiating the perspective and all the top level monitor windows. It then serves as 
 * window manager for the running monitor, and handles perspective messages.
 *
 */
public class MonitorShell implements PerspectiveShell, WindowManager, IMonitorConfigChangeListener {

	/**
	 * Default monitor shell height in pixels
	 */
	public static final int DEFAULT_HEIGHT = 400;
	
	/**
	 * Default monitor shell width in pixels
	 */
	public static final int DEFAULT_WIDTH = 660;
	private static final String TITLE = "Monitor";

	private final Display mainDisplay;
	private final Shell defaultShell;
	private final MonitorMessageController data;
	private final MonitorTimers timers;

	private final List<ViewShell> tabViews = new ArrayList<>();

	private DisplayConfiguration displayConfiguration;
	private MonitorPerspectiveListener perspectiveListener;
	private final IContextConfiguration testConfig;
	private final PromptSettings settings = new PromptSettings();
	private boolean restart = false;
	
	private final ApplicationContext appContext;
    private final Tracer                   trace;

	/**
	 * Creates an instance of MonitorShell using a Display as parent.
	 * @param appContext the current application context
	 * @param display the SWT Display
	 * @param config the SessionConfiguration object for the current application
	 * @param dispConfig the DisplayConfiguration object for this window, from the user perspective 
	 */
	public MonitorShell(final ApplicationContext appContext, final Display display, final IContextConfiguration config,
			final DisplayConfiguration dispConfig) {

		this.appContext = appContext;
		mainDisplay = display;
		defaultShell = new Shell(mainDisplay, SWT.SHELL_TRIM);
		data = appContext.getBean(MonitorMessageController.class);
		timers = appContext.getBean(MonitorTimers.class);
		testConfig = config;
        trace = TraceManager.getTracer(appContext, Loggers.DEFAULT);
		setDisplayConfiguration(dispConfig);
		MonitorViewReferences.getInstance().setWindowManager(this);
		appContext.getBean(MonitorConfigValues.class).addListener(this);
	}

	/**
	 * Creates GUI components and starts the message service subscriber.
	 *
	 */
	public void init() {
	    
		restart = false;
		List <IViewConfiguration> viewConfigs = displayConfiguration.getViewConfigs();

		if ((viewConfigs == null) || viewConfigs.isEmpty()) {

			// Try to load the default perspective as stated in the configuration file.
			final DefaultPerspectiveConfig config = new DefaultPerspectiveConfig(appContext);
			viewConfigs = config.getViewConfigs();
			if (viewConfigs != null) {
				for (final IViewConfiguration view: viewConfigs) {
					displayConfiguration.addViewConfiguration(view);
				}
			} else {
				// Then create one new view tab configuration with default views
				final TabularViewConfiguration viewTabConfig = createNewTabConfig();
				displayConfiguration.addViewConfiguration(viewTabConfig);
			}
			viewConfigs = displayConfiguration.getViewConfigs();
		}

		// Load and instantiate all the views. 
		
		for (int index = 0; index < viewConfigs.size(); index++) {
			final IViewConfiguration viewConfig = viewConfigs.get(index);
			new ViewUtility(appContext).normalizeCoordinates(viewConfig);
			
			// First do tabbed windows, then single view windows. These contain
			// all the other views, which are created recursively.
			if (viewConfig instanceof TabularViewConfiguration) {

				final TabularViewShell shell = new TabularViewShell(appContext, mainDisplay, testConfig,
						(TabularViewConfiguration) viewConfig, this);
				shell.updatePerspectiveListener(perspectiveListener);
				shell.createControls();

				tabViews.add(shell);

			} else if (viewConfig instanceof SingleWindowViewConfiguration){

				final SingleViewShell shell = new SingleViewShell(appContext, mainDisplay, testConfig,
						(SingleWindowViewConfiguration)viewConfig, this);
				shell.updatePerspectiveListener(perspectiveListener);
				shell.createControls();

				tabViews.add(shell);
			} else {
                trace.warn("Standard view found in perspective, expecting tab view: " +
						viewConfig.getViewName());
			}
			MonitorViewReferences.getInstance().addViewReferences(viewConfig.getAllViewReferences());
		}

		loadGlobalConfig();

		// Fire up the message interface and start message flow to views
		startTopic();
	}
	
	/**
	 * Loads global config changes from the perspective into MonitorConfigValues.
	 */
	private void loadGlobalConfig() {
	    // Read the global configuration parameters from the perspective. These include all 
	    // the configured refresh rates. 
	    /* No longer notify other objects from here. Use the singleton
	     * config values and let it notify listeners.
	     */
	    final MonitorConfigValues globalConfig = appContext.getBean(MonitorConfigValues.class);

	    String temp = displayConfiguration.getConfigItem(GlobalPerspectiveParameter.CHANNEL_ALARM_UPDATE_RATE.getXmlTag());
	    if (temp != null) {
	        globalConfig.addValue(GlobalPerspectiveParameter.CHANNEL_ALARM_UPDATE_RATE, Long.valueOf(temp));
	    }	
	    temp = displayConfiguration.getConfigItem(GlobalPerspectiveParameter.CHANNEL_LIST_UPDATE_RATE.getXmlTag());
	    if (temp != null) {
	        globalConfig.addValue(GlobalPerspectiveParameter.CHANNEL_LIST_UPDATE_RATE, Long.valueOf(temp));
	    }
	    temp = displayConfiguration.getConfigItem(GlobalPerspectiveParameter.CHANNEL_PLOT_UPDATE_RATE.getXmlTag());
	    if (temp != null) {
	        globalConfig.addValue(GlobalPerspectiveParameter.CHANNEL_PLOT_UPDATE_RATE, Long.valueOf(temp));
	    }
	    temp = displayConfiguration.getConfigItem(GlobalPerspectiveParameter.FIXED_VIEW_STALENESS_INTERVAL.getXmlTag());
	    if (temp != null) {
	        globalConfig.addValue(GlobalPerspectiveParameter.FIXED_VIEW_STALENESS_INTERVAL, Long.valueOf(temp));
	    }
	    temp = displayConfiguration.getConfigItem(GlobalPerspectiveParameter.SCLK_FORMAT.getXmlTag());
	    if (temp != null) {
	        globalConfig.addValue(GlobalPerspectiveParameter.SCLK_FORMAT, Enum.valueOf(SclkFormat.class, temp));
	    }

	}

	/**
	 * Create a new default tab configuration. This will define the views in the default perspective.
	 *
	 * @return TabularViewConfiguration object representing the default tabbed window
	 */
	private TabularViewConfiguration createNewTabConfig()
	{
		return TabularViewShell.createDefaultViewConfigs(appContext);
	}

	/**
	 * Performs steps necessary for message topic startup after a topic name has been
	 * set. This starts message flow to views. Also starts central display timers.
	 */
	protected void startTopic()
	{
		CheckMessageService.checkMessageServiceRunning(
				appContext.getBean(MessageServiceConfiguration.class),
				null,
				0,
                                                       trace,
				false);

		timers.start();
		data.createSubscribers();
		data.startMessageReceipt();
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.shared.swt.ChillShell#open()
	 */
	@Override
	public void open() {

	    for (int index = 0; index < tabViews.size(); index++) {
	        final ViewShell tabShell = tabViews.get(index);
	        tabShell.open();
	    }
		showPerspectiveSizeWarning();
	}

	private void showPerspectiveSizeWarning() {
		final String message =  appContext.getBean(PerspectiveCounters.class).checkViewLimits();
		if (message != null) {
			SWTUtilities.showWarningDialog(this.getShell(), "Perspective Too Large", "Your perspective exceeds some MPCS-recommended limits:\n\n" + 
					message + "\nBehavior of this monitor may be unpredictable.");
		}
	}
	
	/**
     * {@inheritDoc}
	 * @see jpl.gds.perspective.gui.PerspectiveShell#setDisplayConfiguration(jpl.gds.perspective.DisplayConfiguration)
	 */
	@Override
	public void setDisplayConfiguration(final DisplayConfiguration config)
	{
		displayConfiguration = config;
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.perspective.gui.PerspectiveShell#updateConfiguration()
	 */
	@Override
	public void updateConfiguration()
	{
		WarningUtil.setWidthWarningShown(false);
		
		if (defaultShell.isDisposed()) {
			return;
		}
		final Point size = defaultShell.getSize();
		final ChillSize csize = new ChillSize(size.x, size.y);
		final Point location = defaultShell.getLocation();
		final ChillLocation clocation = new ChillLocation(location.x, location.y);
		final String title = defaultShell.getText();

		// Set current main window size into the monitor Display Configuration
		displayConfiguration.setSize(csize);
		displayConfiguration.setName(title);
		displayConfiguration.setLocation(clocation);
		
		// Instruct all the windows to update configuration
		final Iterator<ViewShell> it = tabViews.iterator();
		while (it.hasNext()) {
			it.next().updateViewConfig();
		}
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.perspective.gui.PerspectiveShell#setPerspectiveListener(jpl.gds.perspective.gui.PerspectiveListener)
	 */
	@Override
	public void setPerspectiveListener(final PerspectiveListener listener)
	{
		perspectiveListener = (MonitorPerspectiveListener)listener;

		// For each view tab, set the perspective
		for (int index = 0; index < tabViews.size(); index++) {
			final ViewShell tabView = tabViews.get(index);
			tabView.updatePerspectiveListener(perspectiveListener);
		}
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.perspective.gui.PerspectiveShell#getDisplayConfiguration()
	 */
	@Override
	public DisplayConfiguration getDisplayConfiguration()
	{
		return displayConfiguration;
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.perspective.gui.PerspectiveShell#exitShell()
	 */
	@Override
	public void exitShell() {
		timers.stop();
		/* Trying to get rid of Invalid Thread Access exceptions on 
		 * shutdown. Even though the timers are stopped and purged above,
		 * they still seem to fire and attempt to use the display. Trying
		 * here to delay just enough so the Display won't be disposed 
		 * immediately.
		 */
		try {
		    Thread.sleep(500);
		} catch (final InterruptedException e) {
		    // do nothing
		}
		if (!defaultShell.isDisposed()) {
			defaultShell.close();
		}
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.shared.swt.ChillShell#getShell()
	 */
	@Override
	public Shell getShell()
	{
		return defaultShell;
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.shared.swt.ChillShell#getTitle()
	 */
	@Override
	public String getTitle()
	{
		return MonitorShell.TITLE;
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.shared.swt.ChillShell#wasCanceled()
	 */
	@Override
	public boolean wasCanceled()
	{
		return false;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.guiapp.gui.WindowManager#addMergedViews()
	 */
	@Override
	public void addMergedViews() {
		final List<IViewConfiguration> views = new ArrayList<>(displayConfiguration.getViewConfigs());
		for (final IViewConfiguration vc : views) {
			if (vc instanceof TabularViewConfiguration && vc.isMerged()) {
				final TabularViewShell shell = new TabularViewShell(appContext, mainDisplay, testConfig,
						(TabularViewConfiguration)vc, this);
				shell.updatePerspectiveListener(perspectiveListener);
				shell.createControls();

				tabViews.add(shell);
				shell.open();
				vc.setMerged(false);
				MonitorViewReferences.getInstance().addViewReferences(vc.getAllViewReferences());
			}	
			else if(vc instanceof SingleWindowViewConfiguration && vc.isMerged()){
				final SingleViewShell shell = new SingleViewShell(appContext, mainDisplay, testConfig,
						(SingleWindowViewConfiguration)vc, this);
				shell.updatePerspectiveListener(perspectiveListener);
				shell.createControls();

				tabViews.add(shell);
				shell.open();
				vc.setMerged(false);
				MonitorViewReferences.getInstance().addViewReferences(vc.getAllViewReferences());
			}
		}
		showPerspectiveSizeWarning();
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.guiapp.gui.WindowManager#addViewTab(jpl.gds.monitor.perspective.view.TabularViewConfiguration)
	 */
	@Override
	public boolean addViewTab(final TabularViewConfiguration config)
	{
		final TabularViewShell shell = new TabularViewShell(appContext, mainDisplay, testConfig,
				config, this);
		shell.updatePerspectiveListener(perspectiveListener);
		shell.createControls();

		tabViews.add(shell);
		shell.open();
		displayConfiguration.addViewConfiguration(config);
		MonitorViewReferences.getInstance().addViewReferences(config.getAllViewReferences());

		return true;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.guiapp.gui.WindowManager#addView(jpl.gds.monitor.perspective.view.SingleWindowViewConfiguration)
	 */
	@Override
	public boolean addView(final SingleWindowViewConfiguration config)
	{
		final SingleViewShell shell = new SingleViewShell(appContext, mainDisplay, testConfig,
				config, this);
		shell.updatePerspectiveListener(perspectiveListener);
		shell.createControls();

		tabViews.add(shell);
		shell.open();
		displayConfiguration.addViewConfiguration(config);
		MonitorViewReferences.getInstance().addViewReferences(config.getAllViewReferences());

		return true;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.guiapp.gui.WindowManager#removeAllViewsAndExit(boolean)
	 */
	@Override
	public void removeAllViewsAndExit(final boolean restart) {
		this.restart = restart;
		if (this.restart) {
			final Iterator<ViewShell> it = tabViews.iterator();
			while (it.hasNext()) {
				final ViewShell tabShell = it.next();
				tabShell.getShell().close();
			}
		}
		exitShell();
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.guiapp.gui.WindowManager#clearAllViews()
	 */
	@Override
	public void clearAllViews() {
		final Iterator<ViewShell> it = tabViews.iterator();
		while (it.hasNext()) {
			final ViewShell tabShell = it.next();
			tabShell.clearAllViews();
		}
		appContext.getBean(MonitorChannelLad.class).clear();
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.config.IMonitorConfigChangeListener#globalConfigurationChange(jpl.gds.monitor.config.GlobalPerspectiveParameter, java.lang.Object)
	 */
	@Override
	public void globalConfigurationChange(final GlobalPerspectiveParameter param, final Object newValue) {
		displayConfiguration.setConfigItem(param.getXmlTag(), newValue.toString());
	}
	
	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.guiapp.gui.WindowManager#sendGlobalHeaderConfigurationChange(jpl.gds.monitor.config.GlobalPerspectiveParameter, java.lang.Boolean)
	 */
	@Override
	public void sendGlobalHeaderConfigurationChange(
			final GlobalPerspectiveParameter param, final Boolean newValue) {
		for(final ViewShell view : tabViews) {
			view.getHeader().showHeader(newValue);
			view.getMenuItemManager().setHeaderSelection(newValue);
		}
		
		displayConfiguration.setConfigItem(param.getXmlTag(), String.valueOf(newValue));
		displayConfiguration.setShowHeader(newValue);
		
	}

	/**
	 * Indicates whether the current perspective is marked for restart.
	 * @return true if restart has been requested, false if not
	 */
	public boolean isRestart() {
		return restart;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.guiapp.gui.WindowManager#removeView(jpl.gds.perspective.view.ViewConfiguration)
	 */
	@Override
	public boolean removeView(final IViewConfiguration config)
	{
		if (restart) {
			return true;
		}
		final Iterator<ViewShell> it = tabViews.iterator();
		int index = 0;
		int found = -1;
		while (it.hasNext()) {
			final ViewShell tabShell = it.next();
			if (tabShell.getViewConfig() == config) {
				found = index;
			}
			index++;
		}

		// If we're on the last window, check to see if the user wants to close the app
		// Otherwise just return, and don't allow the user to close
		if ((found != -1) && tabViews.size() == 1 && !restart) {
			if (settings.showMonitorExitConfirmation()) {
				final ConfirmationShell cs = new ConfirmationShell(tabViews.get(0).getShell(),
						"Closing the last monitor window will exit the application.\n" +
                                "Continue and exit monitor?",
                                                                   false);
				cs.open();
				while (!cs.getShell().isDisposed()) {
					if (!cs.getShell().getDisplay().readAndDispatch())
					{
						cs.getShell().getDisplay().sleep();
					}
				}
				final boolean exit = !cs.wasCanceled();
				if (!exit) {
					return false;
				}
				final boolean promptAgain = cs.getPromptAgain();
				if (!promptAgain) {
					settings.setMonitorExitConfirmation(false);
					settings.save();
				}
				exitShell();
			} else {
				exitShell();
			}
		}

		// remove the requested view
		if (found != -1 && displayConfiguration != null) {
			final List<ViewReference> references = config.getAllViewReferences();
			MonitorViewReferences.getInstance().removeViewReferences(references);
			config.clearViewReferences();

			displayConfiguration.removeViewConfiguration(config);

			int foundShellIndex = -1;
			for (int index2 = 0; index2 < tabViews.size(); index2++) {
				final ViewShell tab = tabViews.get(index2);
				final IViewConfiguration tabConfig = tab.getViewConfig();
				if (config.equals(tabConfig)) {
					foundShellIndex = index2;
				}
			}
			if (foundShellIndex != -1) {
				tabViews.remove(foundShellIndex);
			}
		}
		return true;
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.perspective.gui.PerspectiveShell#perspectiveChanged()
	 */
	@Override
	public void perspectiveChanged() {		
		// do nothing - perspective changes received by this monitor
		// were initiated by this monitor and require no further action
	}

//	/**
//     * {@inheritDoc}
//	 * @see jpl.gds.perspective.gui.PerspectiveShell#sessionChanged(jpl.gds.config.SessionConfiguration)
//	 */
//	@Override
//	public void sessionChanged(SessionConfiguration newSession) {
//	    // do nothing - session changes already handled by GeneralrMessageDistributor
//	}

}
