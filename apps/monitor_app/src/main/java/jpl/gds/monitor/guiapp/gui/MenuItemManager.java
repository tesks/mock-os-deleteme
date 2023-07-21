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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jpl.gds.context.api.ISimpleContextConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.GeneralProperties;
import jpl.gds.common.config.TimeComparisonStrategy;
import jpl.gds.common.swt.AboutUtility;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.TimeComparisonStrategyContextFlag;
import jpl.gds.message.api.util.MessageCaptureHandler;
import jpl.gds.message.api.util.MessageCaptureHandler.CaptureType;
import jpl.gds.monitor.config.GlobalPerspectiveParameter;
import jpl.gds.monitor.config.MonitorConfigValues;
import jpl.gds.monitor.config.MonitorConfigValues.SclkFormat;
import jpl.gds.monitor.config.MonitorGuiProperties;
import jpl.gds.monitor.perspective.view.SingleWindowViewConfiguration;
import jpl.gds.monitor.perspective.view.TabularViewConfiguration;
import jpl.gds.monitor.perspective.view.channel.MonitorChannelLad;
import jpl.gds.monitor.perspective.view.fixed.IFixedLayoutViewConfiguration;
import jpl.gds.perspective.PerspectiveConfiguration;
import jpl.gds.perspective.config.PerspectiveProperties;
import jpl.gds.perspective.config.ViewProperties;
import jpl.gds.perspective.config.ViewType;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.perspective.view.View;
import jpl.gds.perspective.view.ViewFactory;
import jpl.gds.perspective.view.ViewReference;
import jpl.gds.perspective.view.ViewScanner;
import jpl.gds.session.config.gui.SessionConfigViewShell;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.swt.NumberEntryShell;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.TextEntryShell;

/**
 * MenuItemManager is a utility class used to create all of the menu items for
 * each standalone window in the monitor application. This class is required due
 * to a limitation in the SWT library where you cannot share menus across
 * windows and functions as a menu item factory.
 */
public class MenuItemManager
{
	/**
	 * Start file capture menu item
	 */
	protected MenuItem startCapMenuItem;

	/**
	 * Show view header menu item.
	 */
	protected MenuItem displayHeaderItem;

	/**
	 * Stop file capture menu item
	 */
	protected MenuItem stopCapMenuItem;
	
	/**
	 * Save perspective menu item
	 */
	protected MenuItem saveMenuItem;
	
	/**
	 * Save perspective as menu item
	 */
	protected MenuItem saveAsMenuItem;
	
	/**
	 * Exit perspective menu item
	 */
	protected MenuItem exitPerMenuItem;
	
	/**
	 * Edit perspective menu item
	 */
	protected MenuItem editMenuItem;
	
	/**
	 * Lock perspective menu item
	 */
	protected MenuItem lockMenuItem;
	
	/**
	 * Unlock perspective menu item
	 */
	protected MenuItem unlockMenuItem;

	/**
	 * View drop down menu
	 */
	protected Menu viewMenu;
	
	/**
	 * Utilities drop down menu
	 */
	protected Menu utilityMenu;

	/**
	 * Advanced drop down menu
	 */
	protected Menu advancedMenu;
	
	/**
	 * File drop down menu
	 */
	protected Menu fileMenu;

	/**
	 * Preferences drop down menu.
	 */
	protected Menu prefMenu;
	

	private final WindowManager tabManager;

	private static ViewBrowserShell viewBrowserShell = null;
	
	private final SWTUtilities utils = new SWTUtilities();
	
	private final ApplicationContext appContext;
	private final MonitorChannelLad lad;

	/**
	 * Constructs a new MenuItemManager.
	 * @param winMgr the WindowManager object associated with this menu item manager.
	 */
	public MenuItemManager(final ApplicationContext appContext, final WindowManager winMgr) {
		this.appContext = appContext;
		this.lad = appContext.getBean(MonitorChannelLad.class);
		tabManager = winMgr;
	}

	/**
	 * Populates the given menu bar with the standard monitor menu items.
	 * 
	 * @param menuBar the SWT menu bar widget to populate.
	 * @param mainShell the main SWT Shell control from the object that owns the give menu bar. 
	 * @param testConfig the current SessionConfiguration object
	 * @param perspectiveListener an initialized PerspectiveListener object.
	 * current monitor instance
	 * @param tabShell the TabularViewShell object that contains the given menu bar, or null if the menu bar
	 * is not on a TabularViewShell
	 *
	 */
	public void createMonitorMenuItems(final Menu menuBar,
			final Shell mainShell,
			final ISimpleContextConfiguration testConfig,
			final MonitorPerspectiveListener perspectiveListener,
			final TabularViewShell tabShell) {

		final MenuItem fileMenuItem = new MenuItem(menuBar, SWT.CASCADE);
		fileMenuItem.setText("File");

		final MenuItem viewMenuItem = new MenuItem(menuBar, SWT.CASCADE);
		viewMenuItem.setText("View");

		final MenuItem utilityMenuItem = new MenuItem(menuBar, SWT.CASCADE);
		utilityMenuItem.setText("Utilities");

		final MenuItem prefMenuItem = new MenuItem(menuBar, SWT.CASCADE);
		prefMenuItem.setText("Preferences");

		final MenuItem advancedMenuItem = new MenuItem(menuBar, SWT.CASCADE);
		advancedMenuItem.setText("Advanced");

		final MenuItem helpMenuItem = new MenuItem(menuBar, SWT.CASCADE);
		helpMenuItem.setText("Help");

		fileMenu = new Menu(mainShell, SWT.DROP_DOWN);
		fileMenuItem.setMenu(fileMenu);

		viewMenu = new Menu(mainShell, SWT.DROP_DOWN);
		viewMenuItem.setMenu(viewMenu);
		createViewMenuItems(mainShell, tabShell);

		utilityMenu = new Menu(mainShell, SWT.DROP_DOWN);
		utilityMenuItem.setMenu(utilityMenu);

		prefMenu = new Menu(mainShell, SWT.DROP_DOWN);
		prefMenuItem.setMenu(prefMenu);

		advancedMenu = new Menu(mainShell, SWT.DROP_DOWN);
		advancedMenuItem.setMenu(advancedMenu);

		final Menu helpMenu = new Menu(mainShell, SWT.DROP_DOWN);
		helpMenuItem.setMenu(helpMenu);

		final MenuItem aboutMenuItem = new MenuItem(helpMenu, SWT.PUSH);
		aboutMenuItem.setText("About");

		aboutMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				displayAboutDialog();
			}

			/**
			 * Displays the "about" window for the application.
			 */
			private void displayAboutDialog()
			{
			    AboutUtility.showStandardAboutDialog(mainShell, appContext.getBean(GeneralProperties.class));
			} 
		});

		createFileMenuItems(mainShell, perspectiveListener, testConfig);
		createUtilityMenuItems(mainShell);

		createPreferencesMenuItems(mainShell);

		createAdvancedMenuItems(mainShell);
		enableFileMenuItems();

		if (perspectiveListener != null) {
			this.updatePerspective();
		}
	}

	private void createAdvancedMenuItems(final Shell mainShell) {
		final MenuItem timeSystemMenuItem = new MenuItem(advancedMenu, SWT.CASCADE);
		timeSystemMenuItem.setText("Time System");
		
		final Menu timeSystemSubMenu = new Menu(mainShell, SWT.DROP_DOWN | SWT.RADIO);
		timeSystemMenuItem.setMenu(timeSystemSubMenu);
		
		final MenuItem latestTimeSystemMenuItem = new MenuItem(
				timeSystemSubMenu, SWT.RADIO);
		latestTimeSystemMenuItem.setText("Latest Received Data");

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
		switch(appContext.getBean(TimeComparisonStrategyContextFlag.class).getTimeComparisonStrategy()) {
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

		latestTimeSystemMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				setTimeSystem(e, TimeComparisonStrategy.LAST_RECEIVED, mainShell);
			}
		});
		
		ertTimeSystemMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				setTimeSystem(e, TimeComparisonStrategy.ERT, mainShell);
			}
		});

		sclkTimeSystemMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				setTimeSystem(e, TimeComparisonStrategy.SCLK, mainShell);
			}
		});

		scetTimeSystemMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				setTimeSystem(e, TimeComparisonStrategy.SCET, mainShell);
			}
		});
	}

	private void setTimeSystem(final SelectionEvent e, final TimeComparisonStrategy timeComparisonStrategy, final Shell mainShell) {
		
		final TimeComparisonStrategyContextFlag timeStrategy = appContext.getBean(TimeComparisonStrategyContextFlag.class);
		
    	final MenuItem item = (MenuItem) e.widget;
		
		if(item.getSelection() && item.getText().equals(timeComparisonStrategy.getDisplayName())) {
			//user clicked option that was already selected
			if(timeStrategy.getTimeComparisonStrategy() == timeComparisonStrategy) {
				return;
			}
			
			final boolean ok = SWTUtilities.showConfirmDialog(mainShell, 
					"Confirm Change Time System", 
					"This action will change the current time system to " + 
					timeComparisonStrategy.getDisplayName() + 
					". Are you sure?");

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
			timeStrategy.setTimeComparisonStrategy(timeComparisonStrategy);
			// Clear the chill_monitor LAD
			lad.clear();
		}
    }

    private void createUtilityMenuItems(final Shell mainShell) {

        startCapMenuItem = new MenuItem(utilityMenu, SWT.PUSH | SWT.BORDER);
        stopCapMenuItem = new MenuItem(utilityMenu, SWT.PUSH | SWT.BORDER);
        startCapMenuItem.setText("Start File Capture...");
        stopCapMenuItem.setText("Stop File Capture");

        final MenuItem getLadMenuItem = new MenuItem(utilityMenu, SWT.PUSH
                | SWT.BORDER);
        getLadMenuItem.setText("Fetch Channel LAD");

        final MenuItem clearMenuItem = new MenuItem(utilityMenu, SWT.PUSH
                | SWT.BORDER);
        clearMenuItem.setText("Clear All Displays...");

        final MenuItem messageStatsMenuItem = new MenuItem(utilityMenu, SWT.PUSH
                | SWT.BORDER);
        messageStatsMenuItem.setText("Messaging Status...");

        messageStatsMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    final MessageStatusShell shell = new MessageStatusShell(appContext.getBean(MonitorMessageController.class).getSubscribers(), mainShell);
                    shell.open();
                } catch (final Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        startCapMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                displayCaptureDialog();
            }

            /**
             * Displays the "start message capture" window, which prompts the
             * user for the parameters needed to start writing messages to a
             * directory or file.
             */
            private void displayCaptureDialog() {
                final CapturePreferencesShell prefShell = new CapturePreferencesShell(
                        mainShell, appContext.getBean(MessageCaptureHandler.class));
                prefShell.open();
                prefShell.getShell().addDisposeListener(new DisposeListener() {
                    @Override
                    public void widgetDisposed(final DisposeEvent event) {
                        enableFileMenuItems();
                    }
                });
            }
        });

        stopCapMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                stopCapture();
            }

            /**
             * Stops any ongoing capture of messages to files.
             */
            private void stopCapture() {
            	appContext.getBean(MessageCaptureHandler.class).setWriteMode(
                        CaptureType.WRITE_NONE);
            	appContext.getBean(MessageCaptureHandler.class).setMessageOutput(null);
                SWTUtilities.showMessageDialog(mainShell, "Captured Stopped",
                        "Message capture stopped.");
                enableFileMenuItems();
            }
        });

        getLadMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                if (appContext.getBean(IContextIdentification.class).getNumber() == null
                        || appContext.getBean(IContextIdentification.class).getNumber() == 0) {
                    SWTUtilities
                            .showMessageDialog(
                                    mainShell,
                                    "LAD Request Queued",
                                    "The session number is still unknown. Your LAD request will be completed as soon as the session is known.");
                }

                lad.triggerLadFetch();
            }
        });

        clearMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                final boolean ok = SWTUtilities
                        .showConfirmDialog(
                                mainShell,
                                "Confirm Clear Displays",
                                "This action will clear all data from your displays and all channel values from your local LAD. Are you sure?");

                if (!ok) {
                    return;
                }
                tabManager.clearAllViews();
            }
        });

    }

    /**
     * Prompt the user to change a global perspective parameter that takes a
     * long value.
     * 
     * @param mainShell parent shell
     * @param displayName display name of the parameter
     * @param unit units for the parameter
     * @param param GlobalPerspectiveParameter enum value of the parameter
     * @param lowerBound optional lower bound on value (inclusive); -1 to disable
     * @param upperBound optional upper bound on value (inclusive); -1 to disable
     *
     */
    private void askForLongParameter(final Shell mainShell, final String displayName, 
            final String unit, final GlobalPerspectiveParameter param,
            final int lowerBound, final int upperBound) {

        final MonitorConfigValues defaultValues = appContext.getBean(MonitorConfigValues.class);

        final NumberEntryShell numEntryShell = new NumberEntryShell(mainShell, displayName,
                                                                    displayName + " (" + unit + ")", false, false);
        numEntryShell.setValue (
                (Long)defaultValues.getValue(param));
        if (lowerBound != -1) {
            numEntryShell.setLowerBound(lowerBound);
        }
        if (upperBound != -1) {
            numEntryShell.setUpperBound(upperBound);
        }

        numEntryShell.open();

        numEntryShell.getShell().addDisposeListener(new DisposeListener() {
            /**
             * {@inheritDoc}
             * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
             */
            @Override
            public void widgetDisposed(final DisposeEvent event) {
                if (numEntryShell.wasCanceled()) {
                    return;
                }
                defaultValues.addValue (param, Long.valueOf(numEntryShell.getValue()));
            }
        });

    }

    /**
     * Creates the preferences menu items and their menu item handlers.
     * 
     * @param mainShell paren shell
     *.
     */
    private void createPreferencesMenuItems(final Shell mainShell) {
        final MonitorConfigValues defaultValues = appContext.getBean(MonitorConfigValues.class);

        final MenuItem refreshChannelsMenuItem = new MenuItem(prefMenu, SWT.PUSH | SWT.BORDER);
        refreshChannelsMenuItem.setText("Channel List Update Interval...");

        final MenuItem refreshAlarmsMenuItem = new MenuItem(prefMenu, SWT.PUSH | SWT.BORDER);
        refreshAlarmsMenuItem.setText("Alarm List Update Interval...");

        final MenuItem refreshPlotsMenuItem = new MenuItem(prefMenu, SWT.PUSH | SWT.BORDER);
        refreshPlotsMenuItem.setText("Plot Update Interval...");
        final MenuItem stalenessMenuItem = new MenuItem(prefMenu, SWT.PUSH | 
                SWT.BORDER);
        stalenessMenuItem.setText("Global Staleness Interval...");

        final MenuItem sclkFormatMenuItem = new MenuItem(prefMenu, SWT.PUSH | 
                SWT.BORDER);
        final SclkFormat currentFormat = (SclkFormat)defaultValues.getValue(GlobalPerspectiveParameter.SCLK_FORMAT);
        sclkFormatMenuItem.setText(currentFormat == SclkFormat.SUBTICK ? 
                "Show Fine SCLK in Decimal" : "Show Fine SCLK in Sub-ticks");;
        
        sclkFormatMenuItem.setSelection(true);

        refreshChannelsMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                askForLongParameter(mainShell, "Channel Update Rate", 
                        "Seconds", 
                        GlobalPerspectiveParameter.CHANNEL_LIST_UPDATE_RATE,
                        1, -1);            
            }
        });

        refreshAlarmsMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                askForLongParameter(mainShell, "Alarm Update Rate", 
                        "Seconds", 
                        GlobalPerspectiveParameter.CHANNEL_ALARM_UPDATE_RATE,
                        1,
                        -1);
            }
        });


        refreshPlotsMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                askForLongParameter(mainShell, "Plot Update Rate", 
                        "Seconds", 
                        GlobalPerspectiveParameter.CHANNEL_PLOT_UPDATE_RATE,
                        2,
                        -1);
            }
        });

        stalenessMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                final NumberEntryShell numEntryShell = new NumberEntryShell(mainShell, 
                        "Fixed View Staleness Interval", 
                        "Global Staleness Interval (Seconds)", 
                        false, true,
                        "Enable Global Staleness");
                numEntryShell.setLowerBound(30);

                Long defaultStaleness = (Long) defaultValues.getValue(GlobalPerspectiveParameter.FIXED_VIEW_STALENESS_INTERVAL);
                //if -1, global staleness is not enabled
                if(defaultStaleness == -1) {
                    defaultStaleness = Long.valueOf(IFixedLayoutViewConfiguration.DEFAULT_STALENESS_INTERVAL);
                    numEntryShell.disable();
                }
                numEntryShell.setValue(defaultStaleness);

                numEntryShell.open();
                numEntryShell.getShell().addDisposeListener(
                        new DisposeListener() {
                            @Override
                            public void widgetDisposed(final DisposeEvent event) {
                                if (numEntryShell.wasCanceled()) {
                                    return;
                                }

                                // Get values in shell
                                final long interval = numEntryShell.getValue();
                                final boolean isEnabled = numEntryShell.isEnabled();

                                // Disable global staleness
                                if(!isEnabled) {
                                    final boolean ok = SWTUtilities.showConfirmDialog(
                                            mainShell,
                                            "Confirm Disable Global Staleness Interval", 
                                            "This action will overwrite the individual staleness " +
                                                    "interval for all of your fixed view displays " +
                                            "Are you sure?");

                                    if(ok) {
                                        //disable global staleness
                                        defaultValues.addValue(
                                                GlobalPerspectiveParameter.
                                                                                                           FIXED_VIEW_STALENESS_INTERVAL,
                                                                                                   Long.valueOf(-1));

                                    }
                                    else {
                                        return;
                                    }
                                }
                                // valid staleness, need confirmation
                                else {
                                    final boolean ok = SWTUtilities.showConfirmDialog(
                                            mainShell, 
                                            "Confirm Update Global Staleness Interval", 
                                            "This action will update the staleness " +
                                                    "interval for all of your fixed page " +
                                            "displays.  Are you sure?");

                                    if (ok) {
                                        defaultValues.addValue(
                                                GlobalPerspectiveParameter.
                                                FIXED_VIEW_STALENESS_INTERVAL,
                                                interval);

                                    }
                                    else {
                                        return;
                                    }
                                }
                            }
                        });
            }
        });

        sclkFormatMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                
                final SclkFormat currentVal = (SclkFormat)defaultValues.getValue(GlobalPerspectiveParameter.SCLK_FORMAT);
                final SclkFormat newVal = currentVal == SclkFormat.DECIMAL ? SclkFormat.SUBTICK : SclkFormat.DECIMAL;
                
                defaultValues.addValue(GlobalPerspectiveParameter.SCLK_FORMAT, newVal);
                
                sclkFormatMenuItem.setText(currentVal == SclkFormat.SUBTICK ? 
                        "Show Fine SCLK in Sub-ticks" : "Show Fine SCLK in Decimal");
                            
            }
        });
    }


	private void createFileMenuItems(final Shell mainShell, final MonitorPerspectiveListener perspectiveListener,
			final ISimpleContextConfiguration testConfig) {

    	if (testConfig instanceof IContextConfiguration) {

			final MenuItem testConfigMenuItem = new MenuItem(fileMenu, SWT.PUSH);
			testConfigMenuItem.setText("Show Session Configuration...");
			testConfigMenuItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					final SessionConfigViewShell configShell = new SessionConfigViewShell(appContext, mainShell);
					configShell.setContextConfiguration((IContextConfiguration) testConfig);
					configShell.open();
				}
			});
		}

		new MenuItem(fileMenu, SWT.SEPARATOR);

		saveMenuItem = new MenuItem(fileMenu, SWT.PUSH);
		saveMenuItem.setText("Save Perspective");
		saveMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				if (perspectiveListener != null) {
					perspectiveListener.saveCalled();
				}
			}
		});

		saveAsMenuItem = new MenuItem(fileMenu, SWT.PUSH);
		saveAsMenuItem.setText("Save Perspective as...");
		saveAsMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				if (perspectiveListener != null) {
					perspectiveListener.saveAsCalled(mainShell);
					enableFileMenuItems();
				} 
			}
		});

		editMenuItem = new MenuItem(fileMenu, SWT.PUSH);
		editMenuItem.setText("Edit Perspective...");
		editMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				if (perspectiveListener != null) {
					perspectiveListener.getPerspectiveShell().updateConfiguration();
					final boolean reload = perspectiveListener.editCalled(mainShell);
					if (reload) {
						tabManager.removeAllViewsAndExit(true);
					}
				} 
			}
		});

		final List<String> perspectiveList = getConfiguredPerspectiveList();

		MenuItem loadMenuItem = null;

		if (perspectiveList == null || perspectiveList.isEmpty()) {
			loadMenuItem = new MenuItem(fileMenu, SWT.PUSH);
			loadMenuItem.setText("Replace Perspective...");

			loadMenuItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					if (perspectiveListener != null) {
						final boolean reload = perspectiveListener.loadCalled(mainShell);
						if (reload) {
							tabManager.removeAllViewsAndExit(true);
						}
					} 
				}
			});

		} else {
			loadMenuItem = new MenuItem(fileMenu, SWT.CASCADE);
			loadMenuItem.setText("Replace Perspective...");
			final Menu perspectiveMenu = new Menu(mainShell, SWT.DROP_DOWN);
			loadMenuItem.setMenu(perspectiveMenu);
			final MenuItem browseItem = new MenuItem(perspectiveMenu, SWT.PUSH);
			browseItem.setText("Browse...");
			new MenuItem(perspectiveMenu, SWT.SEPARATOR);
			browseItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					if (perspectiveListener != null) {
						final boolean reload = perspectiveListener.loadCalled(mainShell);
						if (reload) {
							tabManager.removeAllViewsAndExit(true);
						}
					} 
				}
			});

			for (final String dir: perspectiveList) {
				final MenuItem dirItem = new MenuItem(perspectiveMenu, SWT.PUSH);
				final File f = new File(dir);
				dirItem.setText(f.getName());
				dirItem.setData(dir);
				dirItem.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						if (perspectiveListener != null) {
							final boolean reload = perspectiveListener.loadCalled(mainShell, (String)((MenuItem)e.getSource()).getData());
							if (reload) {
								tabManager.removeAllViewsAndExit(true);
							}
						} 
					}
				});
			}
		}

		MenuItem mergeMenuItem = null;

		if (perspectiveList == null || perspectiveList.isEmpty()) {
			mergeMenuItem = new MenuItem(fileMenu, SWT.PUSH);
			mergeMenuItem.setText("Merge In Perspective...");
			mergeMenuItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					if (perspectiveListener != null) {
						final boolean merge = perspectiveListener.mergeCalled(mainShell);
						if (merge) {
							tabManager.addMergedViews();
							

							lad.triggerLadFetch();
						}
					} 
				}
			});

		} else {
			mergeMenuItem = new MenuItem(fileMenu, SWT.CASCADE);
			mergeMenuItem.setText("Merge In Perspective...");
			final Menu perspectiveMenu = new Menu(mainShell, SWT.DROP_DOWN);
			mergeMenuItem.setMenu(perspectiveMenu);
			final MenuItem browseItem = new MenuItem(perspectiveMenu, SWT.PUSH);
			browseItem.setText("Browse...");
			new MenuItem(perspectiveMenu, SWT.SEPARATOR);
			browseItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					if (perspectiveListener != null) {
						final boolean merge = perspectiveListener.mergeCalled(mainShell);
						if (merge) {
							tabManager.addMergedViews();

							lad.triggerLadFetch();
						}
					} 
				}
			});

			for (final String dir: perspectiveList) {
				final MenuItem dirItem = new MenuItem(perspectiveMenu, SWT.PUSH);
				final File f = new File(dir);
				dirItem.setText(f.getName());
				dirItem.setData(dir);
				dirItem.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						if (perspectiveListener != null) {
							final boolean merge = perspectiveListener.mergeCalled(mainShell, (String)((MenuItem)e.getSource()).getData());
							if (merge) {
								tabManager.addMergedViews();
								

								lad.triggerLadFetch();
							}
						}
					}
				});
			}
		}

		lockMenuItem = new MenuItem(fileMenu, SWT.PUSH);
		lockMenuItem.setText("Lock Perspective");

		lockMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				if (perspectiveListener != null) {
					perspectiveListener.getPerspectiveShell().updateConfiguration();
					perspectiveListener.setPerspectiveLock(true);
					perspectiveListener.saveCalled();
					enableFileMenuItems();
				}
			}
		});

		unlockMenuItem = new MenuItem(fileMenu, SWT.PUSH);
		unlockMenuItem.setText("Unlock Perspective");

		unlockMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				if (perspectiveListener != null) {
					perspectiveListener.getPerspectiveShell().updateConfiguration();
					perspectiveListener.setPerspectiveLock(false);
					perspectiveListener.saveCalled();
					enableFileMenuItems();
				}
			}
		});

		exitPerMenuItem = new MenuItem(fileMenu, SWT.PUSH);
		exitPerMenuItem.setText("Exit Perspective");

		new MenuItem(fileMenu, SWT.SEPARATOR);

		final MenuItem exitMenuItem = new MenuItem(fileMenu, SWT.PUSH);        
		exitMenuItem.setText("Exit Application");

		exitMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				tabManager.removeAllViewsAndExit(false);
			}
		});

		exitPerMenuItem.setEnabled(perspectiveListener != null &&  GdsSystemProperties.isIntegratedGui());
		exitPerMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				if (perspectiveListener != null) {
					perspectiveListener.exitCalled();
				}
			}
		});        
	}

	/**
	 * Enables/disables items on the file menu and tool bar based upon current state and
	 * settings.
	 */
	private void enableFileMenuItems()
	{
		startCapMenuItem.setEnabled(appContext.getBean(MessageCaptureHandler.class).getWriteMode().equals(CaptureType.WRITE_NONE));
		stopCapMenuItem.setEnabled(!appContext.getBean(MessageCaptureHandler.class).getWriteMode().equals(CaptureType.WRITE_NONE));

		final boolean canWrite = appContext.getBean(PerspectiveConfiguration.class).isWriteable();
		final boolean locked = appContext.getBean(PerspectiveConfiguration.class).isLocked();
		saveMenuItem.setEnabled(canWrite && !locked);
		editMenuItem.setEnabled(canWrite && !locked);
		lockMenuItem.setEnabled(canWrite && !locked);
		unlockMenuItem.setEnabled(canWrite && locked);
	}

	/**
	 * Gets the View drop down menu
	 * 
	 * @return Returns the viewMenu.
	 */
	public Menu getViewMenu()
	{
		return viewMenu;
	}

	/**
	 * Sets the viewMenu
	 *
	 * @param viewMenu The viewMenu to set.
	 */
	public void setViewMenu(final Menu viewMenu)
	{
		this.viewMenu = viewMenu;
	}

	/**
	 * Enables the perspective menu items.
	 */
	public void updatePerspective()
	{
		if (saveMenuItem != null) {
			saveMenuItem.setEnabled(true);
			saveAsMenuItem.setEnabled(true);
			if (GdsSystemProperties.isIntegratedGui()) {
				exitPerMenuItem.setEnabled(true);
			}
		}
	}

	/**
	 * Create all of the view menu items as specified in the configuration
	 * 
	 */
	private void createViewMenuItems(final Shell mainShell, final TabularViewShell tabShell) {

		MenuItem addNewViewMenuItem = null;

		final List<String> types = appContext.getBean(MonitorGuiProperties.class).getAllowedViewTypes();

		final MenuItem addNewTabWindowMenuItem = new MenuItem(viewMenu, SWT.PUSH);
		addNewTabWindowMenuItem.setText("Add New Tabbed Window...");

		addNewTabWindowMenuItem.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(
					final org.eclipse.swt.events.SelectionEvent e) {
				final TabularViewConfiguration myTabConfig = new TabularViewConfiguration(appContext);
				myTabConfig.initToDefaults();

				final TextEntryShell titleShell = new TextEntryShell(
						mainShell,
						"New Window Setup", "Enter New Window Title",
                        myTabConfig.getViewName(), false, TraceManager.getDefaultTracer());
				titleShell.open();

				while (!titleShell.getShell().isDisposed()) {
					if (!mainShell.getDisplay().readAndDispatch()) {
						mainShell.getDisplay().sleep();
					}
				}

				if (titleShell.wasCanceled()) {
					return;
				}

				myTabConfig.setViewName(titleShell.getValue());
				tabManager.addViewTab(myTabConfig);
			}
		});

		final MenuItem addNewWindowMenuItem = new MenuItem(viewMenu, SWT.CASCADE);
		addNewWindowMenuItem.setText("Add New Simple Window...");

		final Menu newWindowSubMenu = new Menu(mainShell, SWT.DROP_DOWN);
		addNewWindowMenuItem.setMenu(newWindowSubMenu);

		for (final String type : types) {
			final MenuItem mi = new MenuItem(newWindowSubMenu, SWT.PUSH);
			setUpNewWindowMenuItem(mi, type, mainShell);
		}

		// If the current window is a tabbed window, need the menu items
		// for adding a view as a new tab

		if (tabShell != null) {
			addNewViewMenuItem = new MenuItem(viewMenu, SWT.CASCADE);
			addNewViewMenuItem.setText("Create New View as Tab...");

			final Menu addNewViewSubMenu = new Menu(mainShell, SWT.DROP_DOWN);
			addNewViewMenuItem.setMenu(addNewViewSubMenu);

			for (final String type: types) {
				final MenuItem mi = new MenuItem(addNewViewSubMenu, SWT.DROP_DOWN);
				setUpNewTabMenuItem(mi, type, mainShell, tabShell);
			}
		}


        final ViewScanner vs = new ViewScanner(appContext.getBean(PerspectiveProperties.class),
                                               appContext.getBean(SseContextFlag.class));
		vs.scanViews();

		final List<ViewReference> viewList = vs.getViewList();

		if (viewList != null && !viewList.isEmpty()) {
			new MenuItem(viewMenu, SWT.SEPARATOR);
			final MenuItem openViewBrowserMenuItem = new MenuItem(viewMenu, SWT.PUSH);
			openViewBrowserMenuItem.setText("Open View Launcher...");

			openViewBrowserMenuItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					if (viewBrowserShell == null) {
						viewBrowserShell = new ViewBrowserShell(appContext, mainShell.getDisplay());
						openViewBrowserMenuItem.setEnabled(false);
						viewBrowserShell.getShell().addDisposeListener(new DisposeListener() {

							@Override
                            public void widgetDisposed(final DisposeEvent event) {
								try {
									viewBrowserShell = null;
									if (!openViewBrowserMenuItem.isDisposed()) {
										openViewBrowserMenuItem.setEnabled(true);
									}
								} catch (final Exception ex) {
									ex.printStackTrace();
									TraceManager.getDefaultTracer().error("Error handling browser shell dispose event "

											+ ex.toString());
								}
							}
						});
						viewBrowserShell.open();
					} else {
						viewBrowserShell.getShell().setActive();
					}
				}
			});
		}

		if (tabShell != null && viewList != null && !viewList.isEmpty()) {
			final MenuItem addViewMenuItem = new MenuItem(viewMenu, SWT.PUSH);
			addViewMenuItem.setText("Add Pre-Defined Views as Tabs...");

			addViewMenuItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					final ViewBrowserShell browser = new ViewBrowserShell(appContext, mainShell, tabShell);
					browser.open();
				}    		
			});
			
		}

		new MenuItem(viewMenu, SWT.SEPARATOR);

		final MenuItem loadViewMenuItem = new MenuItem(viewMenu, SWT.PUSH);
		loadViewMenuItem.setText("Open New Window From File...");

		loadViewMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				final String file = utils.displayStickyFileChooser(false, mainShell, "MenuItemManager", new String[] {"*.xml", "*"});
				if (file == null) {
					return;
				}
				new ViewUtility(appContext).loadView(file, mainShell);
			}
		});

		if (tabShell != null) {

			final MenuItem loadViewIntoTabMenuItem = new MenuItem(viewMenu, SWT.PUSH);
			loadViewIntoTabMenuItem.setText("Add New Tab from File...");

			loadViewIntoTabMenuItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					final String file = utils.displayStickyFileChooser(false, mainShell, "MenuItemManager", new String[] {"*.xml", "*"});
					if (file == null) {
						return;
					}
					new ViewUtility(appContext).loadViewAsTab(file, mainShell, tabShell);
				}
			});
		}
		
		new MenuItem(viewMenu, SWT.SEPARATOR);

        displayHeaderItem = new MenuItem(viewMenu, SWT.CHECK);
        displayHeaderItem.setSelection(tabManager.getDisplayConfiguration().shouldShowHeader());
        displayHeaderItem.setText("Header");
        
        displayHeaderItem.addListener(SWT.Selection, new Listener() {
            
            @Override
            public void handleEvent(final Event arg0) {
                tabManager.sendGlobalHeaderConfigurationChange(GlobalPerspectiveParameter.SHOW_HEADER, displayHeaderItem.getSelection());
            }
        });
        

	}

	/**
	 * Sets the current selection of the header menu item
	 * @param showHeader is true if the menu item should be checked, false otherwise
	 */
	public void setHeaderSelection(final boolean showHeader) {
	    displayHeaderItem.setSelection(showHeader);
	}

	private void setUpNewTabMenuItem(final MenuItem mi, final String type, final Shell mainShell,
			final TabularViewShell tabShell) {
	    final ViewType vt = new ViewType(type);
        final ViewProperties vp = appContext.getBean(PerspectiveProperties.class).getViewProperties(vt);
        
        mi.setText(vp.getStringDefault(IViewConfiguration.DEFAULT_NAME_PROPERTY));
        mi.setData("viewType", vt);
		mi.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(
					final org.eclipse.swt.events.SelectionEvent e) {
				final TabularViewConfiguration myTabConfig = tabShell.getViewConfig();

				final MenuItem item = (MenuItem) e.getSource();
				final ViewType vt = (ViewType)item.getData("viewType");

				final View v = ViewFactory.createView( appContext, vt, true);
				tabShell.getViews().add(v);
				final IViewConfiguration vc = v.getViewConfig();
				myTabConfig.addViewConfiguration(vc);

				if (v instanceof ViewTab) {
					v.init(tabShell.getTabs());
				} else {
					v.init(mainShell);
				}
			}
		});
	}

	private void setUpNewWindowMenuItem(final MenuItem mi, final String type, final Shell mainShell) {

	    final ViewType vt = new ViewType(type);
	    final ViewProperties vp =  appContext.getBean(PerspectiveProperties.class).getViewProperties(vt);
	    
		mi.setText(vp.getStringDefault(IViewConfiguration.DEFAULT_NAME_PROPERTY));
		mi.setData("viewType", vt);
		mi.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(
					final org.eclipse.swt.events.SelectionEvent e) {
				final SingleWindowViewConfiguration myWinConfig = new SingleWindowViewConfiguration(appContext);
				myWinConfig.initToDefaults();

				final MenuItem item = (MenuItem) e.getSource();
				final ViewType vt = (ViewType)item.getData("viewType");

				final IViewConfiguration vc = ViewFactory.createViewConfig(appContext, vt);
				vc.setDisplayViewTitle(false);

				final TextEntryShell titleShell = new TextEntryShell(
						mainShell,
						"New Window Setup", "Enter New Window Title",
                        myWinConfig.getViewName(), false, TraceManager.getDefaultTracer());
				titleShell.open();

				while (!titleShell.getShell().isDisposed()) {
					if (!mainShell.getDisplay().readAndDispatch()) {
						mainShell.getDisplay().sleep();
					}
				}

				if (titleShell.wasCanceled()) {
					return;
				}

				myWinConfig.setViewName(titleShell.getValue());
				myWinConfig.addViewConfiguration(vc);
				tabManager.addView(myWinConfig);		
			}
		});
	}

	private List<String> getConfiguredPerspectiveList() {
		final List<String> dirs = appContext.getBean(PerspectiveProperties.class).getPerspectiveDirectories();
		if (dirs == null || dirs.isEmpty()) {
			return null;
		}
		final ArrayList<String> result = new ArrayList<String>();
		for (final String dir: dirs) {
			final File dirFile = new File(dir);
			final String[] allFiles = dirFile.list();
			if (allFiles == null) {
				continue;
			}
			for (final String name: allFiles) {
				if (new File(dir + File.separator + name).isDirectory() && 
						new File(dir + File.separator + name + File.separator + appContext.getBean(PerspectiveProperties.class).getDefaultPerspectiveFile()).exists()) {
					result.add(dir + File.separator + name + File.separator);					
				}
			}
		}
		return result;
	}

}
