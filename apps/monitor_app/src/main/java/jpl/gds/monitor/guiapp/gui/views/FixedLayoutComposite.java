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
package jpl.gds.monitor.guiapp.gui.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.channel.ChannelDefinitionType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.eha.api.message.EhaMessageType;
import jpl.gds.eha.api.message.ISuspectChannelsMessage;
import jpl.gds.monitor.canvas.ButtonElement;
import jpl.gds.monitor.canvas.CanvasElement;
import jpl.gds.monitor.canvas.CanvasElementFactory;
import jpl.gds.monitor.canvas.ChannelElement;
import jpl.gds.monitor.canvas.FixedCanvas;
import jpl.gds.monitor.canvas.HeaderElement;
import jpl.gds.monitor.canvas.TimeElement;
import jpl.gds.monitor.canvas.support.StaleSupport;
import jpl.gds.monitor.config.GlobalPerspectiveParameter;
import jpl.gds.monitor.config.IMonitorConfigChangeListener;
import jpl.gds.monitor.config.MonitorConfigValues;
import jpl.gds.monitor.guiapp.MonitorTimers;
import jpl.gds.monitor.guiapp.channel.ChannelMessageDistributor;
import jpl.gds.monitor.guiapp.channel.ChannelSampleListener;
import jpl.gds.monitor.guiapp.common.ChannelFlushListener;
import jpl.gds.monitor.guiapp.common.GeneralMessageDistributor;
import jpl.gds.monitor.guiapp.common.GeneralMessageListener;
import jpl.gds.monitor.guiapp.gui.ViewUtility;
import jpl.gds.monitor.guiapp.gui.views.preferences.FixedLayoutPreferencesShell;
import jpl.gds.monitor.guiapp.gui.views.support.ChannelHistoryShell;
import jpl.gds.monitor.perspective.view.ChannelListViewConfiguration;
import jpl.gds.monitor.perspective.view.RealtimeRecordedFilterType;
import jpl.gds.monitor.perspective.view.channel.ChannelDisplayFormat;
import jpl.gds.monitor.perspective.view.channel.LatestChannelTimes;
import jpl.gds.monitor.perspective.view.channel.MonitorChannelLad;
import jpl.gds.monitor.perspective.view.channel.MonitorChannelSample;
import jpl.gds.monitor.perspective.view.fixed.FixedLayoutViewConfiguration;
import jpl.gds.monitor.perspective.view.fixed.IFixedFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.IFixedLayoutViewConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.TimeFieldConfiguration.SourceTimeType;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.perspective.view.View;
import jpl.gds.perspective.view.ViewConfigurationListener;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.swt.ChillColorCreator;
import jpl.gds.shared.swt.ChillFontCreator;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.TextViewShell;
import jpl.gds.shared.swt.types.CoordinateSystemType;
import jpl.gds.shared.types.Pair;

/**
 * FixedLayoutComposite is the GUI widget responsible for displaying a fixed channel display.
 * It is considered a monitor view.
 */
public class FixedLayoutComposite implements ChannelSampleListener, View, GeneralMessageListener, ViewConfigurationListener, ChannelFlushListener, IMonitorConfigChangeListener {

	/**
	 * Fixed Layout composite title
	 */
	public static final String TITLE = "Fixed Layout";

	private boolean paused;
	private final IFixedLayoutViewConfiguration viewConfig;
	private final List<String> channelIds;
	private FixedCanvas canvas;
	private Composite parent;
	private ScrolledComposite mainComposite;
	private Timer staleTimer;
	private final MonitorChannelLad lad;
	private long lastClearTime;
	private final SWTUtilities util = new SWTUtilities();
	private final ChannelListViewConfiguration channelTableConfig;
	private long latestTimesTimestamp;
	private final Map<Pair<String, Integer>, MonitorChannelSample> lastChans = new HashMap<Pair<String, Integer>, MonitorChannelSample>();

	private final HashSet<String> fswSuspectChannels = new HashSet<String>();
	private final HashSet<String> sseSuspectChannels = new HashSet<String>();
	private Font plainFont;
	private Font italicsFont;
	private FixedLayoutPreferencesShell prefShell;
	private boolean forceMonitorDataUpdate;
	private boolean forceAllDataUpdate;

	private final ApplicationContext appContext;
	/** the channel definition provider */
	private final IChannelDefinitionProvider defProv;
	

	/**
	 * Creates a new FixedLayoutComposite with the given configuration.
	 * @param appContext the current application context
	 * 
	 * @param config the FixedLayoutViewConfiguration object that defines settings
	 * for this view
	 */
	public FixedLayoutComposite(final ApplicationContext appContext, final IViewConfiguration config) {
		this.appContext = appContext;
		channelTableConfig = new ChannelListViewConfiguration(appContext);
		lad = appContext.getBean(MonitorChannelLad.class);
		viewConfig = (IFixedLayoutViewConfiguration)config;
		channelIds = viewConfig.getDynamicReferencedChannelIds();
		plainFont = new Font(null, viewConfig.getDataFont().getFace(), viewConfig.getDataFont().getSize(), SWT.NONE);
		italicsFont = new Font(null, viewConfig.getDataFont().getFace(), viewConfig.getDataFont().getSize(), SWT.ITALIC);
        /*
         * Initialize view staleness from global
         * staleness. Doing this in the first place would have eliminated the
         * need for additional interfaces and handling that were added in many
         * places to try to adjust to the fact that this simply was not being
         * done here.
         */
        int staleness = viewConfig.getStalenessInterval();

        /*
         * Only set global staleness if there
         * is no local staleness for this composite defined.
         */
        if (staleness == FixedLayoutViewConfiguration.DEFAULT_STALENESS_INTERVAL) {
            staleness = ((Long)appContext.getBean(MonitorConfigValues.class)
                                  .getValue(GlobalPerspectiveParameter.FIXED_VIEW_STALENESS_INTERVAL)).intValue();
        }
        viewConfig.setStalenessInterval(staleness);
		
		defProv = appContext.getBean(IChannelDefinitionProvider.class);

	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.perspective.view.View#init(org.eclipse.swt.widgets.Composite)
	 */
	@Override
    public void init(final Composite parent) {
		this.parent = parent;
		mainComposite = new ScrolledComposite(this.parent, SWT.V_SCROLL | SWT.H_SCROLL);
		mainComposite.setLayout(new FormLayout());

		final FormData fd = new FormData();
		fd.left = new FormAttachment(0);
		fd.top = new FormAttachment(0);
		fd.bottom = new FormAttachment(100);
		fd.right = new FormAttachment(100);
		mainComposite.setLayoutData(fd);

		canvas = new FixedCanvas(mainComposite);

		final FormData fd2 = new FormData();
		fd2.left = new FormAttachment(0, 3);
		fd2.top = new FormAttachment(0, 3);
		fd2.bottom = new FormAttachment(100, -3);
		fd2.right = new FormAttachment(100, -3);
		canvas.getCanvas().setLayoutData(fd2);

		if (viewConfig.getBackgroundColor() != null) {
			canvas.setDefaultBackgroundColor(ChillColorCreator.getColor(viewConfig.getBackgroundColor()));
		}
		if (viewConfig.getForegroundColor() != null) {
			canvas.setDefaultForegroundColor(ChillColorCreator.getColor(viewConfig.getForegroundColor()));
		}	
		if (viewConfig.getDataFont() != null) {
			canvas.setDefaultFont(ChillFontCreator.getFont(viewConfig.getDataFont()));
		}
	
		if (viewConfig.getCoordinateSystem().equals(CoordinateSystemType.CHARACTER)) {
			canvas.setCharacterLayout(true);
		} 

		createCanvasElements();

		mainComposite.setContent(canvas.getCanvas());
		final int preferredHeight = viewConfig.getPreferredHeight();
		final int preferredWidth = viewConfig.getPreferredWidth();
		if (viewConfig.getCoordinateSystem().equals(CoordinateSystemType.CHARACTER)) {
			canvas.resizeInCharacters(preferredWidth, preferredHeight);
		} else {
			canvas.resizeInPixels(preferredWidth, preferredHeight);
		}

		final Menu popup = new Menu(mainComposite);

		final MenuItem prefMenuItem = new MenuItem(popup, SWT.PUSH);
		prefMenuItem.setText("Preferences");

		prefMenuItem.addSelectionListener(new SelectionAdapter() {
		    @Override
		    public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
		        try {

		            if (prefShell == null) {

		                prefShell = new FixedLayoutPreferencesShell(appContext, mainComposite.getShell());
		                prefShell.setValuesFromViewConfiguration(viewConfig);
		                prefShell.getShell().addDisposeListener(new DisposeListener() {
		                    @Override
                            public void widgetDisposed(final DisposeEvent event) {
		                        try {
		                            if (!prefShell.wasCanceled()) {

		                            	final RealtimeRecordedFilterType oldRealtimeRecordedFilter = viewConfig.getRealtimeRecordedFilterType();
		                            	final int oldStationFilter = viewConfig.getStationId();
		                            	
                                        /**
                                         * Store original staleness interval to check if
                                         * it changed
                                         **/
                                        final int oldStalenessInterval = viewConfig.getStalenessInterval();

		                                prefShell.getValuesIntoViewConfiguration(viewConfig);
		                                setConfigValuesInChannelListViewConfiguration();

		                                lad
		                                .registerStation(
		                                		FixedLayoutComposite.this,
		                                		FixedLayoutComposite.this.viewConfig
		                                		.getStationId());
		                                
		                                if (oldRealtimeRecordedFilter != viewConfig.getRealtimeRecordedFilterType()) {
		                                	clearData(false);
		                                	forceAllDataUpdate = true;
		                                }
		                                else if (oldStationFilter != viewConfig.getStationId()) {
		                                	clearData(true);
		                                	forceMonitorDataUpdate = true;
		                                }
                                        if (oldStalenessInterval != viewConfig.getStalenessInterval()) {
                                            startStaleTimer();
                                        }
		                                
		                                flushTimerFired();
		                            }

		                        } catch (final Exception e) {
		                            e.printStackTrace();
                                    TraceManager.getDefaultTracer()
                                            .error("Unable to handle exit from Preferences window " + e.toString());
		                        } finally {
		                            prefShell = null;
		                            prefMenuItem.setEnabled(true);
		                        }
		                    }
		                });
		                prefMenuItem.setEnabled(false);
		                prefShell.open();
		            }
		        } catch (final Exception e1) {
		            e1.printStackTrace();
                    TraceManager.getDefaultTracer().error("Unable to handle Preferences menu item " + e1.toString());
		        }
		    }
		});

		new MenuItem(popup, SWT.SEPARATOR);
	    
		final MenuItem pauseItem = new MenuItem(popup, SWT.PUSH);
		pauseItem.setText("Pause");

		final MenuItem resumeItem = new MenuItem(popup, SWT.PUSH);
		resumeItem.setText("Resume");
		resumeItem.setEnabled(false);

		pauseItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {
					pause();
					resumeItem.setEnabled(true);
					pauseItem.setEnabled(false);
				} catch (final Exception e1) {
					e1.printStackTrace();
                    TraceManager.getDefaultTracer().error("Unable to handle Pause menu item " + e1.toString());
				}
			}
		});

		resumeItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {
					resume();
					resumeItem.setEnabled(false);
					pauseItem.setEnabled(true);
				} catch (final Exception e1) {
					e1.printStackTrace();
                    TraceManager.getDefaultTracer().error("Unable to handle Resume menu item " + e1.toString());
				}
			}
		});

		final MenuItem clearItem = new MenuItem(popup, SWT.PUSH);
		clearItem.setText("Clear Data");

		clearItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {
					clearView();
				} catch (final Exception e1) {
					e1.printStackTrace();
                    TraceManager.getDefaultTracer().error("Unable to handle Clear menu item " + e1.toString());
				}
			}
		});

		new MenuItem(popup, SWT.SEPARATOR);

		final MenuItem saveImageItem = new MenuItem(popup, SWT.CASCADE);
		saveImageItem.setText("Save Snapshot as Image");

		final Menu imageSubMenu = new Menu(mainComposite.getShell(), SWT.DROP_DOWN);
		saveImageItem.setMenu(imageSubMenu);

		final MenuItem jpgMenuItem = new MenuItem(imageSubMenu, SWT.PUSH);
		jpgMenuItem.setText("As JPG...");
		addPrintImageListener(jpgMenuItem, SWT.IMAGE_JPEG);

		final MenuItem pngMenuItem = new MenuItem(imageSubMenu, SWT.PUSH);
		pngMenuItem.setText("As PNG...");
		addPrintImageListener(pngMenuItem, SWT.IMAGE_PNG);

		final MenuItem bmpMenuItem = new MenuItem(imageSubMenu, SWT.PUSH);
		bmpMenuItem.setText("As BMP...");
		addPrintImageListener(bmpMenuItem, SWT.IMAGE_BMP);

		final MenuItem showDefinitionItem = new MenuItem(popup, SWT.PUSH);
		showDefinitionItem.setText("Show Channel Information...");

		final MenuItem showHistoryItem = new MenuItem(popup, SWT.PUSH);
		showHistoryItem.setText("Show History...");

		showDefinitionItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				final CanvasElement elem = canvas.getElementAtCursor();
				if (elem == null) {
					return;
				}

                final TextViewShell tvs = new TextViewShell(mainComposite.getShell(), TraceManager.getDefaultTracer());
				String text = "";
				if (elem instanceof ChannelElement) {
				    final ChannelElement ce = (ChannelElement)elem;
					text = MonitorChannelSample.getChanDefText(appContext, ce.getChannelId(), ce.getData());
					tvs.getShell().setSize(400,500);
				} else if (elem instanceof TimeElement) {
					text = ((TimeElement)elem).getTimeText();
					tvs.getShell().setSize(400,100);
				} else {
					return;
				}

				tvs.setText(text);
				tvs.open();
			}  	
		});

		showHistoryItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {
					final Point p = canvas.getLastMousePosition();
					final CanvasElement elem = canvas.getElementAtCursor();
					if (elem == null) {
						return;
					}
					if (!(elem instanceof ChannelElement)) {
						return;
					}

					final ChannelElement chanElem = (ChannelElement)elem;
					final String id = chanElem.getChannelId();
					final ChannelHistoryShell shell = new ChannelHistoryShell(appContext, mainComposite.getShell(), p, channelTableConfig);
					final ChannelDisplayFormat displayStuff = new ChannelDisplayFormat(chanElem.getChannelDefinition(), id, false);
					/*
				     * Recorded/realtime filter is now an enum rather than
				     * a boolean and station ID is needed to access the LAD.
				     */
					final List<MonitorChannelSample> latestData = lad.getValueHistory(id, 
							viewConfig.getRealtimeRecordedFilterType(), viewConfig.getStationId());
					
					shell.setChannelValues(latestData, displayStuff, true);
					shell.open();

				} catch (final Exception e1) {
					e1.printStackTrace();
                    TraceManager.getDefaultTracer().error("Unable to handle Clear menu item " + e1.toString());
				}
			}
		});

		canvas.setMenu(popup);		

		mainComposite.addDisposeListener(new DisposeListener() {
			@Override
            public void widgetDisposed(final DisposeEvent event) {
				try {
					final ChannelMessageDistributor dist = appContext.getBean(ChannelMessageDistributor.class);

					for (final String chanId : channelIds) {
						dist.removeGlobalLadListener(chanId, FixedLayoutComposite.this);
					}
					stopStaleTimer();
					appContext.getBean(MonitorTimers.class).removeChannelFlushListener(FixedLayoutComposite.this);

					//dispose of fonts when composite is disposed of
					if(italicsFont != null)
					{
						italicsFont.dispose();
						italicsFont = null;
					}
					if(plainFont != null)
					{
						plainFont.dispose();
						plainFont = null;
					}

					appContext.getBean(MonitorConfigValues.class).removeListener(FixedLayoutComposite.this);

					lad.unregisterStation(
							FixedLayoutComposite.this);

				} catch (final Exception ex) {
					ex.printStackTrace();
                    TraceManager.getDefaultTracer().error("Error disposing of canvas " + ex.toString());
				}
			}
		});

		final ChannelMessageDistributor dist = appContext.getBean(ChannelMessageDistributor.class);

		for (final String chanId : channelIds) {
			dist.addGlobalLadListener(chanId, this);
		}
		canvas.redraw();

		setConfigValuesInChannelListViewConfiguration();

		startStaleTimer();

		appContext.getBean(GeneralMessageDistributor.class).addDataListener(this, EhaMessageType.SuspectChannels);
		appContext.getBean(MonitorTimers.class).addChannelFlushListener(this);
		appContext.getBean(MonitorConfigValues.class).addListener(this);

		lad.registerStation(this, viewConfig.getStationId());
	}

	private void addPrintImageListener(final MenuItem item, final int imageType) {
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				String filename = viewConfig.getViewName().replace(' ', '_');
				switch (imageType) {
				case SWT.IMAGE_JPEG: 
					filename += ".jpg";
					break;
				case SWT.IMAGE_PNG: 
					filename += ".png";
					break;
				case SWT.IMAGE_BMP: 
					filename += ".bmp";
					break;
				case SWT.IMAGE_GIF: 
					filename += ".gif";
					break;
				}
				util.displayStickyFileSaver(mainComposite.getShell(), "Save Image", null, filename);
				canvas.saveImage(filename, imageType);
			}
		});
	}


	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.guiapp.common.ChannelFlushListener#flushTimerFired()
	 */
	@Override
    public void flushTimerFired() {

		try
		{
			if (mainComposite.isDisposed())
			{
				return;
			}

			if (!mainComposite.isVisible()) {
				return;
			}

			if (paused) {
				return;
			}

			/*
			 * Realtime recorded filter in the perspective and
			 * LAD is now enum rather than boolean, and station ID is required for LAD
			 * access.
			 */
			final List<MonitorChannelSample> latestData = lad.getLatestValues(channelIds, 
					viewConfig.getRealtimeRecordedFilterType(), viewConfig.getStationId());

			boolean updated = false;
			if (latestData != null) {
				/* Add station to map
				 * key, update elements when there have been changes to RT/Rec 
				 * or station filtering */
				for (final MonitorChannelSample data: latestData) {
					if(data != null) {
						
						final Pair<String, Integer> pairId = new Pair<String, Integer>
						(data.getChanId(), data.getDssId());
						/*
						 * Check for recorded/realtime flag match here seemed
						 * unnecessary. The LAD will only return recorded/realtime samples based upon the
						 * filter in the view configuration.
						 */
						if ((lastChans.get(pairId) != data && data.getTimestamp() > lastClearTime) ||
								((data.getChanDef().getDefinitionType() == ChannelDefinitionType.M) && forceMonitorDataUpdate) || 
								(forceAllDataUpdate)){
							updated = updateElementsForChannel(data) || updated;
							lastChans.put(pairId, data);
						}
					}
				}
				forceMonitorDataUpdate = false;
				forceAllDataUpdate = false;
			}
			/*
			 * Recorded/realtime flag is now an enum
			 * instead of a boolean. The latest times fetched from the LAD must
			 * reflect this.
			 */
			LatestChannelTimes times = null;
			switch(viewConfig.getRealtimeRecordedFilterType()) {
			case RECORDED:
				times =  lad.getLatestRecordedTimes();
				break;
			case REALTIME:
				times =  lad.getLatestRealtimeTimes();
				break;
			case BOTH:
				times =  lad.getLatestTimes();
				break;
			}

			/*
			 * Updating time and header elements based upon
			 * latest timestamps in the LAD. In the first case, a sample has actually changed
			 * in the LAD since the last time we updated, and since the last time the user
			 * cleared the display.  So all the time elements (ERT, SCET, UTC, etc) must be
			 * updated. In the second case, no new samples are in the LAD but data flow has been
			 * seen at a new UTC. Only UTC time elements are updated.
			 */
			if (times.getTimestamp() > latestTimesTimestamp && times.getTimestamp() > lastClearTime) {
				updated = updateElementsForTimes(times, false) || updated;
				latestTimesTimestamp = times.getTimestamp();
			} else {
				updated = updateElementsForTimes(times, true) || updated;
			}
			if (updated) {
				canvas.redraw();
			}
		}

		catch (final Exception e)
		{
			e.printStackTrace();
			return;
		}
	}

	private void startStaleTimer() {
		if (staleTimer != null) {
			staleTimer.cancel();
			staleTimer = null;
		}

		final int staleInterval = viewConfig.getStalenessInterval();
		if (staleInterval <= 0) {
			return;
		}
		staleTimer = new Timer();
		staleTimer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run()
			{
				SWTUtilities.safeAsyncExec(parent.getDisplay(),
						"fixed layout view stale timer",
						new Runnable()
				{
					@Override
					public String toString() {
						return "FixedLayoutComposite.startStaleTimer.Runnable";
					}
					
					@Override
                    public void run()
					{
						try
						{
							if (mainComposite.isDisposed())
							{
								return;
							}

							if (!mainComposite.isVisible()) {
								return;
							}

							final boolean staled = checkStaleElements(staleInterval);
							if (staled) {
								canvas.redraw();
							}
						}

						catch (final Exception e)
						{
							e.printStackTrace();
							return;
						}
					}
				});
			}

		}, 0, 15000);
	}

	private void stopStaleTimer() {
		if (staleTimer != null) {
			staleTimer.cancel();
		}
	}

	private void createCanvasElements() {
		final List<IFixedFieldConfiguration> fields = (viewConfig).getFieldConfigs();
		final ArrayList<CanvasElement> results = new ArrayList<CanvasElement>();

		for (final IFixedFieldConfiguration config : fields) {
			final CanvasElement elem = CanvasElementFactory.create(config, canvas.getCanvas());
			results.add(elem);	
			if (elem instanceof ButtonElement) {
			    ((ButtonElement)elem).setViewLaunchManager(new ViewUtility(appContext));
			}
		}
		canvas.setCanvasElements(results);
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.guiapp.channel.ChannelSampleListener#receive(jpl.gds.monitor.perspective.view.channel.MonitorChannelSample)
	 */
	@Override
    public void receive(final MonitorChannelSample data) {
		if (mainComposite.isDisposed()) {
			return;
		}
		if (data.isRealtime() && viewConfig.getRealtimeRecordedFilterType() == RealtimeRecordedFilterType.RECORDED) {
			return;
		} else if (!data.isRealtime() && viewConfig.getRealtimeRecordedFilterType() == RealtimeRecordedFilterType.REALTIME) {
			return;
		}

		/* Filter out monitor channels
		 * that don't match station filter
		 *
		 */
		if((data.getChanDef().getDefinitionType() == ChannelDefinitionType.M) && 
				data.getDssId() != viewConfig.getStationId()) {
			return;
		}

		final boolean updated = updateElementsForChannel(data);

		//TODO  before we used the "updated" flag to know when to redraw canvas.
		//With conditionals are we always going to update?
		//Or do we need to keep track of changes?
		if (updated) {
			canvas.redraw();
		}

	}

	/**
	 * Checks all the elements in the canvas to see 
	 * Checks if a channel element or parts of a header element need to be updated
	 * 
	 * @param data is the channel that is being checked for updates
	 * @return true if canvas needs to be redrawn, false otherwise
	 */
	private boolean updateElementsForChannel(final MonitorChannelSample data) {
		boolean updated = false;
		for (final CanvasElement elem : canvas.getCanvasElements()) {
			if (elem instanceof ChannelElement) {
				if (updateChannelElement((ChannelElement)elem, data)) {
					updated = true;
				}
			} else if (elem instanceof HeaderElement) {
				final List<CanvasElement> childElems = ((HeaderElement)elem).getChildElements();
				if (childElems != null) {
					for (final CanvasElement childElem: childElems) {
						if (childElem instanceof ChannelElement) {
							if (updateChannelElement((ChannelElement)childElem, data)) {
								updated = true;
							}
						}
					}
				}
			}
			
			//evaluate element if it has an associated condition set
			if(elem.getFieldConfiguration().getCondition() != null) {
			    //TODO check if contains stale comparison
			    elem.evaluate(appContext, viewConfig.getStalenessInterval());
			    //updated = elem.hasConditionChanged();
			    updated = true;
			}
		}

		return updated;
	}

	private boolean checkStaleElements(final int staleInterval) {
		boolean stale = false;
		for (final CanvasElement elem : canvas.getCanvasElements()) {
			if (elem instanceof StaleSupport) {
				if (((StaleSupport)elem).checkStale(staleInterval)) {
					stale = true;
				}
			} else if (elem instanceof HeaderElement) {
				final List<CanvasElement> childElems = ((HeaderElement)elem).getChildElements();
				if (childElems != null) {
					for (final CanvasElement childElem: childElems) {
						if (childElem instanceof StaleSupport) {
							if (((StaleSupport)childElem).checkStale(staleInterval)) {
								stale = true;
							}
						}
					}
				}
			}
			//evaluate element if it has an associated condition set
            if(elem.getFieldConfiguration().getCondition() != null) {
                elem.evaluate(appContext, viewConfig.getStalenessInterval());
                //updated = elem.hasConditionChanged();
                stale = true;
            }
		}

		return stale;
	}

	/**
	 * Checks for channel changes (dynamic data, alarm highlights, suspect status)
	 * 
	 * @param elem is the channel element
	 * @param data 
	 * @return true if the canvas needs to be redrawn, false otherwise
	 */
	private boolean updateChannelElement(final ChannelElement elem, final MonitorChannelSample data) {
		boolean updated = false;
		final String lookId = data.getChanId();
		final ChannelElement chanElem = elem;

		//update dynamic elements
		if (!elem.isStatic()) {
			final String id = chanElem.getChannelId();
			if (id.equalsIgnoreCase(lookId)) {
				chanElem.setTextFromChannelValue(data);
				updated = true;
			}
		}
		//if channel is static but requires alarm highlighting, we need to update it dynamically
		else if(chanElem.isAlarmHighlight())
		{
			final String id = (elem).getChannelId();
			if (id.equalsIgnoreCase(lookId)) {
				chanElem.setAlarmColors(data);
				updated = true;
			}
		}

		//update elements whose suspect status has changed
		synchronized(fswSuspectChannels) {
			final boolean isSuspect = fswSuspectChannels.contains(elem.getChannelId()) ||
			sseSuspectChannels.contains(elem.getChannelId());
			if(chanElem.isSuspect() != isSuspect)
			{
				chanElem.setSuspect(isSuspect);
				updated = true;
			}
		}

		return updated;
	}

	private boolean updateElementsForTimes(final LatestChannelTimes times, final boolean utcOnly) {
		boolean updated = false;
		for (final CanvasElement elem : canvas.getCanvasElements()) {
			if (elem instanceof TimeElement) {
				final TimeElement timeElem = (TimeElement)elem;
				if (!utcOnly || timeElem.getTimeType().equals(SourceTimeType.UTC)) {
					((TimeElement) elem).setTextFromLatestTimes(times);
					updated = true;
				}
			} else if (elem instanceof HeaderElement) {
				final List<CanvasElement> childElems = ((HeaderElement)elem).getChildElements();
				if (childElems != null) {
					for (final CanvasElement childElem: childElems) {
						if (childElem instanceof TimeElement) {
							final TimeElement timeElem = (TimeElement)childElem;
							if (!utcOnly || timeElem.getTimeType().equals(SourceTimeType.UTC)) {
								timeElem.setTextFromLatestTimes(times);
								updated = true;
							}
						}
					}
				}
			}
		}
		return updated;
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.perspective.view.View#clearView()
	 */
	@Override
    public void clearView() {
		
		clearData(false);
		
		lastClearTime = System.currentTimeMillis();
	}
	
	/* Factor this out into a separate method */
	/**
	 * Helper function to clear fixed page data
	 * @param monitorOnly flag is true if only monitor data should be cleared, 
	 * false if all data should be cleared.
	 */
	private void clearData(final boolean monitorOnly) {
		for (final CanvasElement elem : canvas.getCanvasElements()) {
			if (elem instanceof ChannelElement) {
				final IChannelDefinition chanDef = ((ChannelElement)elem).getChannelDefinition();
				if (null == chanDef) {
					continue;	// skip null channel definition
				}

				final boolean isMonitor = chanDef.getDefinitionType() == ChannelDefinitionType.M;
				if(!monitorOnly || (monitorOnly && isMonitor)) {
					if (!elem.isStatic()) {
						((ChannelElement)elem).setText(((ChannelElement)elem).getNoDataIndicator());
					}
					((ChannelElement)elem).resetAlarmState();
				}
			} else if (elem instanceof TimeElement) {
				((TimeElement)elem).setText(((TimeElement)elem).getNoDataIndicator());

			} else if (elem instanceof HeaderElement) {
				final List<CanvasElement> childElems = ((HeaderElement)elem).getChildElements();
				if (childElems != null) {

					for (final CanvasElement childElem: childElems) {
						if (childElem instanceof ChannelElement) {
							final IChannelDefinition childDef = ((ChannelElement)elem).getChannelDefinition();
							if (null == childDef) {
                                continue;
                            }

							final boolean isMonitor = childDef.getDefinitionType() == ChannelDefinitionType.M;
							if(!monitorOnly || (monitorOnly && isMonitor)) {
								if (!childElem.isStatic()) {
									((ChannelElement)childElem).setText(((ChannelElement)childElem).getNoDataIndicator());
								}
								((ChannelElement)childElem).resetAlarmState();
							}
						} else if (childElem instanceof TimeElement) {
							((TimeElement)childElem).setText(((TimeElement)childElem).getNoDataIndicator());
						}
						if (childElem instanceof StaleSupport) {
							((StaleSupport)childElem).clearStale();
						}
					}
				}
			}
			if (elem instanceof StaleSupport) {
				((StaleSupport)elem).clearStale();
			}
		}
		canvas.redraw();
		
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.perspective.view.View#getDefaultName()
	 */
	@Override
    public String getDefaultName() {
		return TITLE;
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.perspective.view.View#getMainControl()
	 */
	@Override
    public Control getMainControl() {
		return mainComposite;
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.perspective.view.View#getViewConfig()
	 */
	@Override
    public IViewConfiguration getViewConfig() {
		return viewConfig;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.config.IMonitorConfigChangeListener#globalConfigurationChange(jpl.gds.monitor.config.GlobalPerspectiveParameter, java.lang.Object)
	 */
	@Override
	public void globalConfigurationChange(final GlobalPerspectiveParameter param,
	        final Object newValue) {

	    if (param == GlobalPerspectiveParameter.FIXED_VIEW_STALENESS_INTERVAL) {
	        viewConfig.setStalenessInterval(((Long)newValue).intValue());
	    }
	    startStaleTimer();

	    /* Force next refresh to do all channels,
	     * because SCLK format may have changed.
	     */
	    forceAllDataUpdate = true;
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.perspective.view.View#updateViewConfig()
	 */
	@Override
    public void updateViewConfig() {
		// Nothing to do here - no dynamic updates to config allowed for this view
	}

	private void setConfigValuesInChannelListViewConfiguration() {
		/*
	     * Recorded/realtime filter is now an enum rather than
	     * a boolean and station ID is needed to access the LAD.
	     */
		channelTableConfig.setRealtimeRecordedFilterType(viewConfig.getRealtimeRecordedFilterType());
		channelTableConfig.setStationId(viewConfig.getStationId());
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.guiapp.common.GeneralMessageListener#messageReceived(jpl.gds.shared.message.IMessage[])
	 */
	@Override
    public void messageReceived(final IMessage[] m) {
		if (m.length == 0) {
			return;
		}
		for (int i = 0; i < m.length; i++) {
			displayMessage(m[i]);
		}
	}

	private void pause() {
		paused = true;
	}

	private void resume() {
		paused = false;
	}

	/**
	 * Process messages that are of type SuspectChannelMessage
	 * 
	 * @param msg is the current message that is being processed
	 */
	public void displayMessage(final IMessage msg) {
		if (parent.isDisposed()) {
			return;
		}
		parent.getDisplay().asyncExec(new Runnable () {
			
			@Override
			public String toString() {
				return "FixedLayoutComposite.displayMessage.Runnable";
			}
			
			@Override
            public void run () {
				try {
					if (mainComposite.isDisposed()) {
						return;
					}
                    TraceManager.getDefaultTracer().trace("fixed layout view is processing message");

					if (msg.isType(EhaMessageType.SuspectChannels)) {
						handleSuspectChannelsMessage((ISuspectChannelsMessage)msg);
					}
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Update suspectChannels list based on the information in the SuspectChannelsMsg
	 * 
	 * @param msg this object stores a table with the current suspect channels
	 */
	public void handleSuspectChannelsMessage(final ISuspectChannelsMessage msg)
	{
		synchronized(fswSuspectChannels) {
			// get latest channel data for given ids
			final List<String> suspectIds = msg.getSuspectTable().getAllSuspectChannelIds();

			HashSet<String> useSet = fswSuspectChannels;
			if (msg.isFromSse()) {
				useSet = sseSuspectChannels;
			}
			useSet.clear();

			//latestSuspectData is null if no channels are marked suspect
			if(suspectIds != null)
			{	
				// Add suspect channels to list
				for (final String id : suspectIds)
				{
					useSet.add(id);
				}
			}
		}

		boolean updated = false;
		for (final CanvasElement elem : canvas.getCanvasElements()) {
			if (elem instanceof ChannelElement) {
				final String id = ((ChannelElement)elem).getChannelId();
				final boolean isSuspect = ((ChannelElement)elem).isSuspect();
				final boolean newSuspect = fswSuspectChannels.contains(id) || sseSuspectChannels.contains(id);
				if (isSuspect != newSuspect) {
					((ChannelElement)elem).setSuspect(newSuspect);
					updated = true;
				}
			} else if (elem instanceof HeaderElement) {
				final List<CanvasElement> childElems = ((HeaderElement)elem).getChildElements();
				if (childElems != null) {
					for (final CanvasElement childElem: childElems) {
						if (childElem instanceof ChannelElement) {
							final String id = ((ChannelElement)childElem).getChannelId();
							final boolean isSuspect = ((ChannelElement)childElem).isSuspect();
							final boolean newSuspect = fswSuspectChannels.contains(id) || sseSuspectChannels.contains(id);
							if (isSuspect != newSuspect) {
								((ChannelElement)childElem).setSuspect(newSuspect);
								updated = true;
							}
						}
					}
				}
			}
		}
		if (updated) {
			canvas.redraw();
		}
	}

	/**
     * {@inheritDoc}
	 */
	@Override
    public void configurationChanged(final IViewConfiguration config) {
		plainFont = new Font(null, viewConfig.getDataFont().getFace(), viewConfig.getDataFont().getSize(), SWT.NONE);
		italicsFont = new Font(null, viewConfig.getDataFont().getFace(), viewConfig.getDataFont().getSize(), SWT.ITALIC);
	}
	
}
