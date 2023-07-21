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
import jpl.gds.monitor.perspective.view.SingleWindowViewConfiguration;
import jpl.gds.perspective.view.*;
import jpl.gds.session.message.SessionMessageType;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.swt.ChillColorCreator;
import jpl.gds.shared.swt.ChillFontCreator;
import jpl.gds.shared.swt.types.ChillLocation;
import jpl.gds.shared.swt.types.ChillSize;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.springframework.context.ApplicationContext;

import java.util.List;

/**
 * 
 * SingleViewShell is a special view type that implements a standalone window that contains 
 * another single view. Instances of this class are managed as top-level windows by the
 * monitor.
 */
public class SingleViewShell implements ViewSetManager,
ViewConfigurationListener, ViewShell, GeneralMessageListener {

	/**
	 * Number of milliseconds in a second constant
	 */
	public static final long MILLISECONDS_PER_SECOND = 1000;
	
	/**
	 * Default height for the single view shell in pixels
	 */
	public static final int DEFAULT_HEIGHT = 400;
	
	/**
	 * Default width for the single view shell
	 */
	public static final int DEFAULT_WIDTH = 660;
	
	/**
	 * Single view shell title
	 */
	public static final String TITLE = "Monitor";

	private final SingleWindowViewConfiguration viewConfig;
	private MenuItemManager menuItems;
	private MonitorPerspectiveListener perspectiveListener;
	private View childView;

	/**
	 * Main display
	 */
	protected Display mainDisplay;
	
	/**
	 * Main shell
	 */
	protected Shell mainShell;
	
	/**
	 * Single view shell header
	 */
	protected ViewShellHeader header;
	
	/**
	 * Test configuration
	 */
	protected ISimpleContextConfiguration testConfig;

	private final WindowManager windowManager;
	private String realWindowName;
	
	/**
	 * Optional view title that is enabled through the preferences menu
	 */
	private StyledText viewLabel;
	private long oldKey;
	private Font dataFont;
    private Color background;
    private Color foreground;

	private final ApplicationContext appContext;

	/**
	 * 
	 * Creates an instance of SingleViewShell.
	 * 
	 * @param appContext the current application context
	 * @param display the parent Display object
	 * @param config the current TestConfiguration object
	 * @param viewConfig the SingleWindowViewConfiguration containing settings for
	 *            this window
	 * @param winMgr the WindowManager for this view shell
	 */
	public SingleViewShell(final ApplicationContext appContext, final Display display,
			final IContextConfiguration config,
			final SingleWindowViewConfiguration viewConfig,
			final WindowManager winMgr) {

		this.appContext = appContext;
		mainDisplay = display;
		mainShell = new Shell(mainDisplay, SWT.SHELL_TRIM);
		if (viewConfig.isIconified()) {
			mainShell.setMinimized(true);
		}
		testConfig = config;
		windowManager = winMgr;
		this.viewConfig = viewConfig;  
		appContext.getBean(GeneralMessageDistributor.class).addDataListener(this, SessionMessageType.StartOfSession);
		appContext.getBean(GeneralMessageDistributor.class).addDataListener(this, SessionMessageType.SessionHeartbeat);
	}

	/**
	 * Creates the controls and composites in the main shell.
	 */
	public void createControls() {

	    final GridLayout shellLayout = new GridLayout();
	    shellLayout.marginHeight = 5;
        shellLayout.marginWidth = 5;
		mainShell.setLayout(shellLayout);
		// Add remove listener to shell
		mainShell.addShellListener(new ShellListener() {

			/**
		     * {@inheritDoc}
			 * @see org.eclipse.swt.events.ShellListener#shellClosed(org.eclipse.swt.events.ShellEvent)
			 */
			@Override
			public void shellClosed(final ShellEvent arg0) {
				final boolean closeShell = windowManager
				.removeView(viewConfig);
				if (closeShell) {
					arg0.doit = true;
					appContext.getBean(GeneralMessageDistributor.class).removeDataListener(SingleViewShell.this);
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
				// not used
			}

			/**
		     * {@inheritDoc}
			 * @see org.eclipse.swt.events.ShellListener#shellDeactivated(org.eclipse.swt.events.ShellEvent)
			 */
			@Override
			public void shellDeactivated(final ShellEvent arg0) {
				//not used
			}

			/**
		     * {@inheritDoc}
			 * @see org.eclipse.swt.events.ShellListener#shellDeiconified(org.eclipse.swt.events.ShellEvent)
			 */
			@Override
			public void shellDeiconified(final ShellEvent arg0) {
				viewConfig.setIsIconified(false);
			}

			/**
		     * {@inheritDoc}
			 * @see org.eclipse.swt.events.ShellListener#shellIconified(org.eclipse.swt.events.ShellEvent)
			 */
			@Override
			public void shellIconified(final ShellEvent arg0) {
				viewConfig.setIsIconified(true);
			}
		});

		ChillSize size = null;
		ChillLocation location = null;
		if (viewConfig != null) {
			size = viewConfig.getSize();
			location = viewConfig.getLocation();
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

		setWindowTitle();

		final Menu menuBar = new Menu(mainShell, SWT.BAR);
		mainShell.setMenuBar(menuBar);

		menuItems = new MenuItemManager(appContext, windowManager);

		menuItems.createMonitorMenuItems(menuBar, mainShell,
				testConfig, perspectiveListener,
				null );
		
		final boolean shouldShowHeader = windowManager.getDisplayConfiguration().shouldShowHeader();
		header = new ViewShellHeader(appContext, mainShell, viewConfig, shouldShowHeader);
		
		createInitialViews();

		setColorsAndFonts();
	}
	
	/**
	 * Sets the foregound color, background color and data font
	 */
	protected void setColorsAndFonts() {
		if (this.dataFont != null && !this.dataFont.isDisposed()) {
			this.dataFont.dispose();
			this.dataFont = null;
		}
		dataFont = ChillFontCreator.getFont(viewConfig.getDataFont());
		this.mainShell.setFont(dataFont);
		
		if (this.foreground != null && !this.foreground.isDisposed()) {
			this.foreground.dispose();
			this.foreground = null;
		}
		this.foreground = ChillColorCreator.getColor(viewConfig.getForegroundColor());
		this.mainShell.setForeground(this.foreground);
		
		if (this.background != null && !this.background.isDisposed()) {
			this.background.dispose();
			this.background = null;
		}
		this.background = ChillColorCreator.getColor(viewConfig.getBackgroundColor());
		this.mainShell.setBackground(this.background);
	}

	private void setWindowTitle() {

		realWindowName = viewConfig.getViewName();
		final Long oldKeyObj = testConfig.getContextId().getNumber();
		final String hostName = testConfig.getContextId().getHost();
		oldKey = oldKeyObj == null ? 0 : oldKeyObj.longValue(); 
		mainShell.setText(viewConfig.getWindowTitle(testConfig.getContextId().getName(), String.valueOf(oldKey) + "/" + hostName));
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.guiapp.common.GeneralMessageListener#messageReceived(jpl.gds.shared.message.IMessage[])
	 */
	@Override
	public void messageReceived(final IMessage[] m) {
		if (mainShell.isDisposed()) {
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
						return "SingleViewShell.messageReceived.Runnable";
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

		viewConfig.setSize(csize);
		viewConfig.setLocation(clocation);
		viewConfig.setViewName(realWindowName);

		if (childView != null) {
			childView.updateViewConfig();
		}
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.perspective.view.ViewConfigurationListener#configurationChanged(jpl.gds.perspective.view.ViewConfiguration)
	 */
	@Override
	@SuppressWarnings("CompareObjectsWithEquals")
	public void configurationChanged(final IViewConfiguration config) {
		
		setColorsAndFonts();
		/* Suppress PMD suggestion to use
		 * equals().  This needs to check if it's the same object. */
		if (config == viewConfig) {
			setWindowTitle();
		} else {
			if (childView.getViewConfig().isDisplayViewTitle()) {
			    if (viewLabel != null && !viewLabel.isDisposed()) {
                  viewLabel.dispose();
			    }
			    viewLabel = new StyledText(mainShell, SWT.NONE);
			    viewLabel.setFont(dataFont);
			    viewLabel.setBackground(background);
			    viewLabel.setForeground(foreground);
			    viewLabel.setText(childView.getViewConfig().getViewName());
			    viewLabel.setMargins(5, 5, 0, 0);
			    final StyleRange style1 = new StyleRange();
			    style1.start = 0;
			    style1.length = viewLabel.getText().length();
			    style1.fontStyle = SWT.BOLD;
			    viewLabel.setStyleRange(style1);
			    
			} else if (viewLabel != null) {
				if (!viewLabel.isDisposed()) {
					viewLabel.dispose();
				}
				viewLabel = null;
			}
			if (viewLabel != null) {
			    
			    //move viewLabel to the top
			    viewLabel.moveAbove(null);
	            
			    //if there are 3 children, then header is present and view 
			    //title should be moved below it. i realize this is not 
			    //stellar code but not sure how else to determine if header is 
			    //present and dynamically move view title below it.
			    final Control[] children = mainShell.getChildren();
                if(children.length == 3) {
                    viewLabel.moveBelow(children[1]);
                }
			}
			mainShell.layout();
		}
	}

	/**
	 * Create the initial view pane from the supplied configuration.
	 * 
	 */
	private void createInitialViews() {
		final List<IViewConfiguration> configs = viewConfig.getViews();
		if (configs != null && !configs.isEmpty()) {
			final IViewConfiguration vc = configs.get(0);
			addNewView(vc);
		}
	}

	/**
	 * Add a new view to the monitor shell
	 * 
	 * @param vc the configuration for the view to add
	 */
	private void addNewView(final IViewConfiguration vc) {
		final View v = ViewFactory.createView(appContext, vc, false);

		v.init(mainShell);
		childView = v;

		if (childView.getViewConfig().isDisplayViewTitle()) {
		    viewLabel = new StyledText(mainShell, SWT.NONE);
            viewLabel.setFont(dataFont);
            viewLabel.setBackground(background);
            viewLabel.setForeground(foreground);
            viewLabel.setText(childView.getViewConfig().getViewName());
            viewLabel.setMargins(5, 5, 0, 0);
            final StyleRange style1 = new StyleRange();
            style1.start = 0;
            style1.length = viewLabel.getText().length();
            style1.fontStyle = SWT.BOLD;
            viewLabel.setStyleRange(style1);
		}

		final GridData viewFormData = new GridData();

		viewFormData.horizontalAlignment = SWT.FILL;
		viewFormData.grabExcessHorizontalSpace = true;
		viewFormData.verticalAlignment = SWT.FILL;
		viewFormData.grabExcessVerticalSpace = true;
		if (viewLabel != null) {
		    viewLabel.moveAbove(null);
		    mainShell.layout();
		}
		v.getMainControl().setLayoutData(viewFormData);
		childView.getViewConfig().addConfigurationListener(this);
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void addView(final IViewConfiguration config) {
		addNewView(config);
		viewConfig.addViewConfiguration(config);
	}

	/**
     * {@inheritDoc}
	 */
	@Override
	public void removeView(final IViewConfiguration config) {
		// cannot remove the one child view from this window
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
		return SingleViewShell.TITLE;
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.monitor.guiapp.gui.ViewShell#getViewConfig()
	 */
	@Override
	public SingleWindowViewConfiguration getViewConfig() {
		return viewConfig;
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.perspective.view.ViewSetManager#clearAllViews()
	 */
	@Override
	public void clearAllViews() {
		if (childView != null) {
			childView.clearView();
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
