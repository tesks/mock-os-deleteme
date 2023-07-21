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

import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.ISimpleContextConfiguration;
import jpl.gds.context.api.message.IContextMessage;
import jpl.gds.monitor.guiapp.common.GeneralMessageDistributor;
import jpl.gds.monitor.guiapp.common.GeneralMessageListener;
import jpl.gds.monitor.perspective.view.TabularViewConfiguration;
import jpl.gds.perspective.PromptSettings;
import jpl.gds.perspective.config.ViewType;
import jpl.gds.perspective.view.*;
import jpl.gds.session.message.SessionMessageType;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.swt.ChillColorCreator;
import jpl.gds.shared.swt.ChillFontCreator;
import jpl.gds.shared.swt.ConfirmationShell;
import jpl.gds.shared.swt.types.ChillLocation;
import jpl.gds.shared.swt.types.ChillSize;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 
 * TabularViewShell is a special view type that is a window containing a tab pane, where the 
 * tabs are other views.  Instances of this class are managed as windows by the monitor.
 */
public class TabularViewShell implements ViewSetManager,
ViewConfigurationListener, ViewShell, GeneralMessageListener {

	/**
	 * Constant that stores number of milliseconds in a second
	 */
	public static final long MILLISECONDS_PER_SECOND = 1000;
	
	/**
	 * Default height for a tabular view shell in pixels
	 */
	public static final int DEFAULT_HEIGHT = 400;
	
	/**
	 * Default width for a tabular view shell in pixels
	 */
	public static final int DEFAULT_WIDTH = 660;

	/**
	 * Tabular view shell title
	 */
	public static final String TITLE = "Monitor";

	private TabularViewConfiguration tabConfig;
	private MenuItemManager menuItems;
	
	/**
	 * Listener for discerning changes to the perspective
	 */
	protected MonitorPerspectiveListener perspectiveListener;
	
	/**
	 * List of views contained within this tabular view shell
	 */
	protected List<View> tabViews = new ArrayList<>();

	/**
	 * Main display
	 */
	protected Display mainDisplay;
	
	/**
	 * Main shell
	 */
	protected Shell mainShell;
	
	/**
	 * Shell header that contains the VCID, DSS ID and message topic
	 */
	protected ViewShellHeader header;
	
	/**
	 * Tab folder widget
	 */
	protected CTabFolder tabs;
	
	/**
	 * Test configuration
	 */
	protected ISimpleContextConfiguration testConfig;
	
	private final WindowManager windowManager;
	private String realWindowName;
	private final PromptSettings settings = new PromptSettings();
	private long oldKey;
	private Font dataFont;
	private Color foreground;
	private Color background;

	private final ApplicationContext appContext;

	/**
	 * 
	 * Creates an instance of TabularViewShell.
	 * 
	 * @param appContext the current application context
	 * @param display the parent Display object
	 * @param testConfig2 the current TestConfiguration object
	 * @param viewConfig the TabularViewConfiguration containing settings for
	 *            this window
	 * @param winMgr the WindowManager object for this view shell           
	 */
	public TabularViewShell(final ApplicationContext appContext, final Display display,
			final IContextConfiguration testConfig2,
			final TabularViewConfiguration viewConfig,
			final WindowManager winMgr) {

		this.appContext = appContext;
		mainDisplay = display;
		mainShell = new Shell(mainDisplay, SWT.SHELL_TRIM);
		if (viewConfig.isIconified()) {
			mainShell.setMinimized(true);
		}
		testConfig = testConfig2;
		windowManager = winMgr;
		this.setTabConfig(viewConfig);
		appContext.getBean(GeneralMessageDistributor.class).addDataListener(this, SessionMessageType.StartOfSession);
		appContext.getBean(GeneralMessageDistributor.class).addDataListener(this, SessionMessageType.SessionHeartbeat);
	}

	/**
	 * Retrieves the list of views contained on the tabs in this window.
	 * @return a List of View objects
	 */
	public List<View> getViews() {
		return tabViews;
	}

	/**
	 * Retrieves the SWT Tab Folder object used by this view.
	 * @return the CTabFolder object
	 */
	public CTabFolder getTabs() {
		return tabs;
	}

	/**
	 * Retrieves the TableViewConfiguration object used to configure this view
	 * @param viewConfig TabularViewConfigurationObject
	 */
	private void setTabConfig(final TabularViewConfiguration viewConfig) {
		tabConfig = viewConfig;
	}

	/**
	 * Creates the controls and composites in the main shell.
	 */
	public void createControls() {

		// Add remove listener to shell
		mainShell.addShellListener(new ShellListener() {

			/**
		     * {@inheritDoc}
			 * @see org.eclipse.swt.events.ShellListener#shellClosed(org.eclipse.swt.events.ShellEvent)
			 */
			@Override
			public void shellClosed(final ShellEvent arg0) {
				final boolean closeShell = windowManager
				.removeView(tabConfig);
				if (closeShell) {
					arg0.doit = true;
				} else {
					arg0.doit = false;
				}
			}

			/**
		     * {@inheritDoc}
			 * @see org.eclipse.swt.events.ShellListener#shellActivated(org.eclipse.swt.events.ShellEvent)
			 */
			@Override
			public void shellActivated(final ShellEvent arg0) {
				// Not used
			}

			/**
		     * {@inheritDoc}
			 * @see org.eclipse.swt.events.ShellListener#shellDeactivated(org.eclipse.swt.events.ShellEvent)
			 */
			@Override
			public void shellDeactivated(final ShellEvent arg0) {
				// Not used
			}

			/**
		     * {@inheritDoc}
			 * @see org.eclipse.swt.events.ShellListener#shellDeiconified(org.eclipse.swt.events.ShellEvent)
			 */
			@Override
			public void shellDeiconified(final ShellEvent arg0) {
				tabConfig.setIsIconified(false);
			}

			/**
		     * {@inheritDoc}
			 * @see org.eclipse.swt.events.ShellListener#shellIconified(org.eclipse.swt.events.ShellEvent)
			 */
			@Override
			public void shellIconified(final ShellEvent arg0) {
				tabConfig.setIsIconified(true);
			}
		});

		ChillSize size = null;
		ChillLocation location = null;
		if (tabConfig != null) {
			size = tabConfig.getSize();
			location = tabConfig.getLocation();
		}
		if (size != null) {
			mainShell.setSize(size.getXWidth(), size.getYHeight());
		} else {
			mainShell.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
		}
		if (location != null) {
			mainShell.setLocation(location.getXPosition(), location
					.getYPosition());
		}

		final GridLayout shellLayout = new GridLayout();
		shellLayout.marginHeight = 5;
		shellLayout.marginWidth = 5;
		mainShell.setLayout(shellLayout);

		setWindowTitle();

		final Menu menuBar = new Menu(mainShell, SWT.BAR);
		mainShell.setMenuBar(menuBar);

		menuItems = new MenuItemManager(appContext, windowManager);

		menuItems.createMonitorMenuItems(menuBar, mainShell,
				testConfig, perspectiveListener, this );
		
		final boolean shouldShowHeader = windowManager.getDisplayConfiguration().shouldShowHeader();
		header = new ViewShellHeader(appContext, mainShell, tabConfig, shouldShowHeader);
	
		tabs = new CTabFolder(mainShell, SWT.TOP | SWT.CLOSE
				| SWT.BORDER);
		tabs.setLayoutData(new GridData(GridData.FILL_BOTH));
		tabs.setBackground(background);
		tabs.setFont(dataFont);

		createInitialViews();

		setColorsAndFonts();

		// Initial colorizing of last viewed tab
		
		final List<IViewConfiguration> views = tabConfig
				.getViews();

		if (tabs.getItemCount() != 0) {
			final IViewConfiguration childView = views.get(tabConfig.getTopTabIndex());

			tabs
			.setSelectionForeground(ChillColorCreator
					.getColor(childView.getForegroundColor()));

			tabs
			.setSelectionBackground(ChillColorCreator
					.getColor(childView.getBackgroundColor()));
		}

		tabs.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(final Event event) {
			    if (tabs == null) {
			        return;
			    }
				final CTabItem item = (CTabItem) event.item;
				final CTabItem[] listOfItems = tabs.getItems();
				int index = 0;
				for (; index < listOfItems.length; index++) {
					if (listOfItems[index] == item) {
						break;
					}
				}
				final List<IViewConfiguration> views = tabConfig
				.getViews();
				final IViewConfiguration childView = views.get(index);

				Color old = tabs.getSelectionForeground();
				if (old != null && !old.isDisposed()) {
					old.dispose();
				}
				old = tabs.getSelectionBackground();
				if (old != null && !old.isDisposed()) {
					old.dispose();
				}
				tabs
				.setSelectionForeground(ChillColorCreator
						.getColor(childView.getForegroundColor()));

				tabs
				.setSelectionBackground(ChillColorCreator
						.getColor(childView.getBackgroundColor()));
			}
		});

		tabs.addCTabFolder2Listener(new CTabFolder2Adapter() {

			@Override
			public void close(final CTabFolderEvent event) {
				final CTabItem whichTab = (CTabItem) event.item;
				if (settings
						.showMonitorTabCloseConfirmation()) {
					final ConfirmationShell confirm = new ConfirmationShell(
							mainShell,
							"If you close the "
							+ whichTab.getText()
							+ " tab you will not be able to re-open it.\nAre you sure?",
                                                                            false);
					confirm.open();
					while (!confirm.getShell().isDisposed()) {
						if (!confirm.getShell().getDisplay().readAndDispatch()) {
							confirm.getShell().getDisplay().sleep();
						}
					}
					final boolean exit = !confirm.wasCanceled();
					if (!exit) {
						event.doit = false;
						return;
					}
					final boolean promptAgain = confirm.getPromptAgain();
					if (!promptAgain) {
						settings
						.setMonitorTabCloseConfirmation(false);
						settings.save();
					}
				}
				for (int i = 0; i < tabViews.size(); i++) {
					final View vt = tabViews.get(i);
					if (vt instanceof ViewTab) {
						final CTabItem ct = ((ViewTab) vt).getTabItem();
						if (ct.equals(whichTab)) {
							tabViews.remove(i);

							final List<IViewConfiguration> viewConfigs = tabConfig
							.getViews();

							if (viewConfigs != null) {
								MonitorViewReferences.getInstance().removeViewReferences(vt.getViewConfig().getAllViewReferences());
								vt.getViewConfig().clearViewReferences();
								viewConfigs.remove(vt.getViewConfig());
							}

						}
					}
				}
				whichTab.dispose();
			}
		});

		if (tabs.getItemCount() != 0) {
			tabs.setSelection(this.getViewConfig().getTopTabIndex());
		}
	}

	/**
	 * Sets the background color, foreground color and data font
	 */
	protected void setColorsAndFonts() {
		if (dataFont != null && !dataFont.isDisposed()) {
			dataFont.dispose();
			dataFont = null;
		}
		dataFont = ChillFontCreator.getFont(tabConfig.getDataFont());
		mainShell.setFont(dataFont);
		tabs.setFont(dataFont);
		
		if (foreground != null && !foreground.isDisposed()) {
			foreground.dispose();
			foreground = null;
		}
		foreground = ChillColorCreator.getColor(tabConfig.getForegroundColor());
		mainShell.setForeground(foreground);
		tabs.setForeground(foreground);
		
		if (background != null && !background.isDisposed()) {
			background.dispose();
			background = null;
		}
		background = ChillColorCreator.getColor(tabConfig.getBackgroundColor());
		mainShell.setBackground(background);
		tabs.setBackground(background);
	}

	private void setWindowTitle() {
		realWindowName = tabConfig.getViewName();
		final Long oldKeyObj = testConfig.getContextId().getNumber();
		final String hostName = testConfig.getContextId().getHost();
		oldKey = oldKeyObj == null ? 0 : oldKeyObj.longValue(); 
		mainShell.setText(tabConfig.getWindowTitle(testConfig.getContextId().getName(), String.valueOf(oldKey) + "/" + hostName));
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.guiapp.common.GeneralMessageListener#messageReceived(jpl.gds.shared.message.IMessage[])
	 */
	@Override
	public void messageReceived(final IMessage[] m) {
		if (mainShell.isDisposed() || mainShell.getDisplay().isDisposed()) {
			return;
		}
		ISimpleContextConfiguration msgTestConfig = null;
		if (m == null || m.length == 0) {
			return;
		}
		if (m[0] instanceof IContextMessage) {
			msgTestConfig = ((IContextMessage)m[0]).getContextConfiguration();

			final Long newKey = msgTestConfig.getContextId().getNumber();
			final long newK = newKey == null ? 0 : newKey.longValue();
			if ((oldKey != newK && !m[0].isFromSse()) || oldKey == 0 ) {
				testConfig = msgTestConfig;
				mainShell.getDisplay().asyncExec(new Runnable() {

					@Override
					public String toString() {
						return "TabularViewShell.messageReceived.Runnable";
					}

					@Override
					public void run() {
						try {
							if (mainShell.isDisposed()) {
								return;
							}
							setWindowTitle();
						} catch (final Exception e) {
							TraceManager.getDefaultTracer().error("Unexpected exception setting window title: " +
						       ExceptionTools.getMessage(e), e);
						}
					}
				});
			}
		}
	}

	/**
	 * Create the default view configurations for the case that the application
	 * is started and a perspective has not yet been saved.
	 * 
	 * @param appContext the current application context
	 * 
	 * @return ArrayList of all views to add to the monitor window
	 */
	public static TabularViewConfiguration createDefaultViewConfigs(final ApplicationContext appContext) {

		final TabularViewConfiguration viewTab = new TabularViewConfiguration(appContext);

		IViewConfiguration view = ViewFactory.createViewConfigForTab(appContext, ViewType.STATUS);
		viewTab.addViewConfiguration(view);
		view = ViewFactory.createViewConfigForTab(appContext, ViewType.PRODUCT);
		viewTab.addViewConfiguration(view);
		view = ViewFactory.createViewConfigForTab(appContext, ViewType.EVR);
		viewTab.addViewConfiguration(view);
		view = ViewFactory.createViewConfigForTab(appContext, ViewType.COMMAND);
		viewTab.addViewConfiguration(view);
		view = ViewFactory.createViewConfigForTab(appContext, ViewType.CHANNEL_LIST);
		viewTab.addViewConfiguration(view);

		return viewTab;
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.monitor.guiapp.gui.ViewShell#updateViewConfig()
	 */
	@Override
	public void updateViewConfig() {
		// Update all views
		final Point size = mainShell.getSize();
		final ChillSize csize = new ChillSize(size.x, size.y);
		final Point location = mainShell.getLocation();
		final ChillLocation clocation = new ChillLocation(location.x, location.y);

		tabConfig.setSize(csize);
		tabConfig.setLocation(clocation);
		tabConfig.setViewName(realWindowName);
		int tabindex = tabs.getSelectionIndex();
		if (tabindex == -1) {
			tabindex = 0;
		}
		tabConfig.setTopTabIndex(tabindex);

		for (int index = 0; index < tabViews.size(); index++) {
			final View view = tabViews.get(index);
			view.updateViewConfig();
		}
	}

	/**
     * {@inheritDoc}
	 */
	@Override
	public void configurationChanged(final IViewConfiguration config) {
		if (config.equals(tabConfig)) {
			setColorsAndFonts();
			setWindowTitle();
		}
	}

	/**
	 * Create the initial set of view panes from the supplied configuration.
	 * 
	 * @param viewConfigs List of configurations defining views to create
	 */
	private void createInitialViews() {
		final List<IViewConfiguration> configs = tabConfig.getViews();

		final Iterator<IViewConfiguration> it = configs.iterator();
		while (it.hasNext()) {
			final IViewConfiguration vc = it.next();
			if (vc != null) {
				addNewView(vc);
			}
		}
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void addView(final IViewConfiguration config) {

		addNewView(config);
		tabConfig.addViewConfiguration(config);
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void removeView(final IViewConfiguration config) {
		final Iterator<View> it = tabViews.iterator();
		int index = 0;
		int found = -1;
		while (it.hasNext()) {
			if (it.next().getViewConfig() == config) {
				found = index;
			}
			index++;
		}
		if (found != -1) {
			tabViews.remove(found);
		}
	}

	/**
	 * Add a new view to the monitor shell
	 * 
	 * @param vc the configuration for the view to add
	 */
	private void addNewView(final IViewConfiguration vc) {
		final View v = ViewFactory.createView(appContext, vc, true);

		if (v instanceof ViewTab) {
			v.init(tabs);
		} else {
			v.init(mainShell);
		}
		tabViews.add(v);
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.guiapp.gui.ViewShell#updatePerspectiveListener(jpl.gds.monitor.guiapp.gui.MonitorPerspectiveListener)
	 */
	@Override
	public void updatePerspectiveListener(final MonitorPerspectiveListener listener) {
		perspectiveListener = listener;

		if ((perspectiveListener != null) && (menuItems != null)) {
			menuItems.updatePerspective();
		}
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.shared.swt.ChillShell#open()
	 */
	@Override
	public void open() {
		mainShell.open();
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.shared.swt.ChillShell#getShell()
	 */
	@Override
	public Shell getShell() {
		return mainShell;
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.shared.swt.ChillShell#wasCanceled()
	 */
	@Override
	public boolean wasCanceled() {
		return false;
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.shared.swt.ChillShell#getTitle()
	 */
	@Override
	public String getTitle() {
		return TabularViewShell.TITLE;
	}

	/**
	 * Gets the view configuration object for this view tab shell.
	 * 
	 * @return TabularViewConfiguration
	 */
	@Override
	public TabularViewConfiguration getViewConfig() {
		return tabConfig;
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.perspective.view.ViewSetManager#clearAllViews()
	 */
	@Override
	public void clearAllViews() {
		for (int index = 0; index < tabViews.size(); index++) {
			final View view = tabViews.get(index);
			view.clearView();
		}
	}

	/**
     * {@inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.ViewShell#getHeader()
     */
    @Override
	public ViewShellHeader getHeader() {
        return header;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.ViewShell#getMenuItemManager()
     */
    @Override
	public MenuItemManager getMenuItemManager() {
        return menuItems;
    }
}
