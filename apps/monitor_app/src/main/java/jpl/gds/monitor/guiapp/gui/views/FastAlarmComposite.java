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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.jfree.util.Log;
import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.EnableLstContextFlag;
import jpl.gds.dictionary.api.alarm.AlarmLevel;
import jpl.gds.dictionary.api.channel.ChannelDefinitionType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.dictionary.api.client.channel.IChannelUtilityDictionaryManager;
import jpl.gds.eha.api.message.EhaMessageType;
import jpl.gds.eha.api.message.ISuspectChannelsMessage;
import jpl.gds.monitor.config.GlobalPerspectiveParameter;
import jpl.gds.monitor.config.MonitorConfigValues;
import jpl.gds.monitor.config.MonitorConfigValues.SclkFormat;
import jpl.gds.monitor.config.MonitorGuiProperties;
import jpl.gds.monitor.guiapp.MonitorTimers;
import jpl.gds.monitor.guiapp.channel.ChannelMessageDistributor;
import jpl.gds.monitor.guiapp.channel.ChannelSampleListener;
import jpl.gds.monitor.guiapp.common.ChannelFlushListener;
import jpl.gds.monitor.guiapp.common.GeneralMessageDistributor;
import jpl.gds.monitor.guiapp.common.GeneralMessageListener;
import jpl.gds.monitor.guiapp.common.gui.DisplayConstants;
import jpl.gds.monitor.guiapp.gui.views.preferences.FastAlarmPreferencesShell;
import jpl.gds.monitor.guiapp.gui.views.support.ChannelSetConsumer;
import jpl.gds.monitor.guiapp.gui.views.support.ChannelSetEditorShell;
import jpl.gds.monitor.guiapp.gui.views.support.ChannelSetUpdateListener;
import jpl.gds.monitor.guiapp.gui.views.support.FastAlarmHistoryShell;
import jpl.gds.monitor.perspective.view.FastAlarmViewConfiguration;
import jpl.gds.monitor.perspective.view.RealtimeRecordedFilterType;
import jpl.gds.monitor.perspective.view.RealtimeRecordedSupport;
import jpl.gds.monitor.perspective.view.StationSupport;
import jpl.gds.monitor.perspective.view.channel.AlarmFilter;
import jpl.gds.monitor.perspective.view.channel.ChannelDisplayFormat;
import jpl.gds.monitor.perspective.view.channel.ChannelSet;
import jpl.gds.monitor.perspective.view.channel.MonitorChannelLad;
import jpl.gds.monitor.perspective.view.channel.MonitorChannelSample;
import jpl.gds.perspective.ChillTable;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.perspective.view.View;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.sys.SystemUtilities;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.LocalSolarTimeFactory;
import jpl.gds.shared.time.TimeUtility;


/**
 * FastAlarmComposite is the new view class for the Alarm view. It is faster
 * than AlarmComposite, which was the old view. Alarm views show only channels in alarm. 
 * The channels and alarms shown may be filtered by user preferences.
 */
public class FastAlarmComposite implements View, ChannelSetConsumer, GeneralMessageListener, ChannelFlushListener {

    private final MonitorConfigValues globalConfig;
    
	/**
	 * Alarm composite title
	 */
	public static final String TITLE = "Alarm";

	private static final String NO_FILTER_TEXT = "View is not filtered";
	private static final String YES_FILTER_TEXT = "***View is filtered";
	private long maxChannelSelectionSize;

	private Composite parent;
	private Composite mainShell;
	private Timer alarmFlushTimer;
	private FastAlarmTableComposite fastAlarmTableComposite;
	private ChannelSet channelSet;
	private final FastAlarmViewConfiguration viewConfig;
	private FastAlarmPreferencesShell prefShell;
	private ChillTable tableDef;

	private final List<String> channelsInAlarm = new ArrayList<String>();
	private final List<String> channelsAwaitingExpiration = new ArrayList<String>();
	private final MonitorChannelLad lad;
	private final FastAlarmDataListener dataListener;
	private final Semaphore syncFlag = new Semaphore(1);
	private final DateFormat timeFormat = TimeUtility.getIsoFormatter();
	private final List<Integer> volatileIndices = new ArrayList<Integer>();
	private AlarmFilter alarmFilter;
	/*
	 * Realtime recorded filter in the perspective and
	 * LAD is now enum rather than boolean. 
	 */
	private RealtimeRecordedFilterType realtimeRecFilter;
	private int station;

	private long lastClearTime;
	private Label filterLabel;

	private final ApplicationContext appContext;

	private final boolean useSolTimes;
	private final SprintfFormat formatUtil;
    private final Tracer                     log;

	/**
	 * Constructor
	 * 
	 * Create a FastAlarmComposite with the given view configuration.
	 * @param appContext the current application context
	 * 
	 * @param config the FastAlarmViewConfiguration containing view settings
	 */
	public FastAlarmComposite(final ApplicationContext appContext, final IViewConfiguration config) {
		this.appContext = appContext;
		globalConfig = appContext.getBean(MonitorConfigValues.class);
		useSolTimes = appContext.getBean(EnableLstContextFlag.class).isLstEnabled();
		formatUtil = appContext.getBean(SprintfFormat.class);
		lad = appContext.getBean(MonitorChannelLad.class);
		viewConfig = (FastAlarmViewConfiguration) config;
		tableDef = viewConfig.getTable(FastAlarmViewConfiguration.ALARM_TABLE_NAME);
		channelSet = viewConfig.getChannels(appContext.getBean(IChannelDefinitionProvider.class));
		dataListener = new FastAlarmDataListener();
		alarmFilter = viewConfig.getAlarmFilter(appContext.getBean(IChannelDefinitionProvider.class));
		final ChannelMessageDistributor cdm = appContext.getBean(ChannelMessageDistributor.class);
		cdm.addGlobalLadListener(dataListener);
		cdm.addGlobalAlarmListener(dataListener);
		maxChannelSelectionSize = appContext.getBean(MonitorGuiProperties.class).getAlarmMaxChannels();
        this.log = TraceManager.getDefaultTracer(appContext);

		/*
		 * Realtime recorded filter in the perspective and
		 * LAD is now enum rather than boolean. 
		 */
		realtimeRecFilter = viewConfig.getRealtimeRecordedFilterType();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.perspective.view.View#getMainControl()
	 */
	@Override
	public Control getMainControl() {
		return mainShell;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.perspective.view.View#init(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void init(final Composite parent) {
		this.parent = parent;
		createGui();
		appContext.getBean(MonitorTimers.class).addAlarmFlushListener(this);
		startAlarmFlushTimer();
		updateFilter();
		appContext.getBean(GeneralMessageDistributor.class).addDataListener(this, EhaMessageType.SuspectChannels);

		lad.registerStation(this, viewConfig.getStationId());
	}

	private void startAlarmFlushTimer() {
		if (alarmFlushTimer != null) {
			alarmFlushTimer.cancel();
		}
		final long interval = viewConfig.getResetFlushInterval() * 60000L;
		if (interval == 0) {
			alarmFlushTimer = null;
			return;
		}
		alarmFlushTimer = new Timer();
		alarmFlushTimer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public String toString() {
				return "FastAlarmComposite.startAlarmFlushTimer.Runnable";
			}

			@Override
			public void run() {
				try {
					parent.getDisplay().asyncExec(
							new Runnable() {

								@Override
								public void run() {
									try {
										if (mainShell
												.isDisposed()) {
											return;
										}
										syncFlag.acquire();
										flushClearedAlarms(false);
										syncFlag.release();
									} catch (final Exception e) {
										e.printStackTrace();
										return;
									} finally {
										if (syncFlag
												.availablePermits() == 0) {
											syncFlag
											.release();
										}
									}
								}
							});
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		}, interval, interval);
	}

	private boolean isVolatileIndex(final int index) {
		synchronized(volatileIndices) {
			if (volatileIndices.isEmpty()) {
				volatileIndices.add(tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_ALARM_COLUMN));
				volatileIndices.add(tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_DN_COLUMN));
				volatileIndices.add(tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_EU_COLUMN));
				volatileIndices.add(tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_ERT_COLUMN));
				volatileIndices.add(tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_IN_ERT_COLUMN));
				volatileIndices.add(tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_OUT_ERT_COLUMN));
				volatileIndices.add(tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_SCET_COLUMN));
				volatileIndices.add(tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_SCLK_COLUMN));
				volatileIndices.add(tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_LST_COLUMN));
				volatileIndices.add(tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_DSS_COLUMN));
				volatileIndices.add(tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_RECORDED_COLUMN));
			}
			return volatileIndices.contains(index);
		}
	}

	private void stopAlarmFlushTimer() { 
		if (alarmFlushTimer != null) {
			alarmFlushTimer.cancel();
			alarmFlushTimer = null;
		}
	}

	private void createGui() {
		mainShell = new Composite(parent, SWT.NONE);
		final FormLayout layout = new FormLayout();
		mainShell.setLayout(layout);

		fastAlarmTableComposite = new FastAlarmTableComposite(appContext, mainShell, viewConfig);
		final FormData data = new FormData();
		data.top = new FormAttachment(0);
		data.right = new FormAttachment(100);
		data.left = new FormAttachment(0);
		data.bottom = new FormAttachment(93);
		fastAlarmTableComposite.setLayoutData(data);

		filterLabel = new Label(mainShell, SWT.NONE);
		final FormData flfd = new FormData();
		flfd.top = new FormAttachment(fastAlarmTableComposite);
		flfd.left = new FormAttachment(0, 3);
		flfd.right = new FormAttachment(100);
		flfd.bottom = new FormAttachment(100);
		filterLabel.setLayoutData(flfd);
		setFilterLabel();
	}

	private void setFilterLabel() {
		alarmFilter = viewConfig.getAlarmFilter(appContext.getBean(IChannelDefinitionProvider.class));
		if (alarmFilter.isEmpty()) {
			filterLabel.setText(NO_FILTER_TEXT);
		} else {
			filterLabel.setText(YES_FILTER_TEXT);
		}	
		filterLabel.setBackground(fastAlarmTableComposite.background);
		filterLabel.setForeground(fastAlarmTableComposite.foreground);
	}

	/**
	 * Processes new alarms and updates exisiting ones
	 *
	 */
	private class FastAlarmDataListener implements ChannelSampleListener {

		/**
		 * @{inheritDoc}
		 * @see jpl.gds.monitor.guiapp.channel.ChannelSampleListener#receive(jpl.gds.monitor.perspective.view.channel.MonitorChannelSample)
		 */
		@Override
		public void receive(final MonitorChannelSample data) {
			final String chanId = data.getChanId();

			/*
			 * Realtime recorded filter in the perspective and
			 * LAD is now enum rather than boolean. Adjusted the logic here to account for 
			 * this. If data is realtime and we only want recorded, or vice versa, discard
			 * this data.
			 */
			if (data.isRealtime() && realtimeRecFilter == RealtimeRecordedFilterType.RECORDED) {
				return;
			} else if (!data.isRealtime() && realtimeRecFilter == RealtimeRecordedFilterType.REALTIME) {
				return;
			}


			if(data.getChanDef().getDefinitionType().equals(
					ChannelDefinitionType.M) && data.getDssId() != station) {
				return;
			}

			// Do not process this sample if filtered out by user preferences.
			if (!alarmFilter.accept(data)) {
				return;
			}

			final TableItem item = fastAlarmTableComposite.tableItems.get(chanId);
			// This code serves only to pick up very transient alarms, in which the channel
			// goes in and out of alarm in between the LAD refresh interval.
			// So only process this channel if it is not already on the display. 
			// The periodic LAD fetch will get the updates to channels already on the display.
			// If the channel is not in alarm, this operation is a NO-OP.
			if (item == null) {
				try {
					syncFlag.acquire();
					dataListener.processNewAlarmChannel(chanId, data);

				} catch (final InterruptedException e) {
					Log.error("Unable to aquire sync lock in Alarm view");
					e.printStackTrace();
				} finally {
					syncFlag.release();
				}
			}
		}

		private boolean processNewAlarmChannel(final String id, final MonitorChannelSample data) {
			final boolean newValueInAlarm = !(data.getDnAlarmLevel().equals(AlarmLevel.NONE) && data.getEuAlarmLevel().equals(AlarmLevel.NONE));

			// no alarm on a new channel or a channel with no definition means do nothing.
			if (!newValueInAlarm) {
				return false;
			}

			final IChannelDefinition def = appContext.getBean(IChannelDefinitionProvider.class).getDefinitionFromChannelId(id);
			if (def == null) {
				return false;
			}

			// create a new table item. This will set values into all the columns from the channel data object
			final TableItem item = createTableItem(def, data);

			// All that's left is to do the alarm highlighting and suspect font marking
			fastAlarmTableComposite.highlightAlarmColumns(item, data);
			fastAlarmTableComposite.setSuspectFont(item, id);

			return true;
		}

		public void processUpdatedAlarmChannel(final String id,
				final MonitorChannelSample data, final TableItem item) {

			final ISclk oldSclk = (ISclk) item.getData("sclk");

	
			// Do not display the value if it is from the LAD and SCLK is less
			// than old SCLK
			if (oldSclk != null && data.getSclk().compareTo(oldSclk) < 0
					&& data.isFromLad()) {
                log.debug("LAD Alarm for ", data.getChanId(), " discarded because SCLK is older than current");
				return;
			}

			final boolean newValueInAlarm = !(data.getDnAlarmLevel().equals(AlarmLevel.NONE) && data.getEuAlarmLevel().equals(AlarmLevel.NONE));
			final boolean oldValueInAlarm = channelsInAlarm.contains(id);


			// Case 1: Old value in alarm. New value in alarm.
			if (oldValueInAlarm && newValueInAlarm) {
				final AlarmLevel euLevel = data.getEuAlarmLevel();
				final AlarmLevel dnLevel = data.getDnAlarmLevel();
				item.setData("alarmLevel", euLevel.ordinal() > dnLevel.ordinal() ? euLevel : dnLevel);
			} 

			// Case 2: Old value in alarm. New value not in alarm.
			else if (oldValueInAlarm && !newValueInAlarm) {
				item.setData("outOfAlarm", true);
				channelsInAlarm.remove(id);
				channelsAwaitingExpiration.add(id);
				item.setData("clearTime", System.currentTimeMillis());
				item.setData("alarmLevel", null);
			}

			// Case 3: Old value not in alarm. New value in alarm
			else if (!oldValueInAlarm && newValueInAlarm) {
				channelsAwaitingExpiration.remove(id);
				channelsInAlarm.add(id);
				item.setData("clearTime", null);
				final AlarmLevel euLevel = data.getEuAlarmLevel();
				final AlarmLevel dnLevel = data.getDnAlarmLevel();
				item.setData("alarmLevel", euLevel.ordinal() > dnLevel.ordinal() ? euLevel : dnLevel);
			}

			// Case 4: Old value not in alarm. New value not in alarm
			else {
				SystemUtilities.doNothing();
			}

			// Update the existing table columns with new values
			final String time = timeFormat.format(data.getErt());
			final String sclk = data.getSclk().toString();
			final IAccurateDateTime scetDate = data.getScet();
			String sol = "";
			if (useSolTimes) {
			    if (data.getSol() != null) {
			        sol = data.getSol().getFormattedSol(true);
			    } else {
			        sol = LocalSolarTimeFactory.getNewLst(0).getFormattedSol(true);
			    }
			}

			final String dn = getFormattedDn(data);
			final String eu = getFormattedEu(data);
			final String fullState = fastAlarmTableComposite.getAlarmState(data);
			final String station = data.getDssId() != 
					StationIdHolder.UNSPECIFIED_VALUE ? 
							String.valueOf(data.getDssId()) : 
								DisplayConstants.UNSPECIFIED_STATION;
							final String recorded = String.valueOf(!data.isRealtime());

							fastAlarmTableComposite.setColumn(item, tableDef.getColumnIndex(
									FastAlarmViewConfiguration.ALARM_DN_COLUMN), dn);

							fastAlarmTableComposite.setColumn(item, tableDef.getColumnIndex(
									FastAlarmViewConfiguration.ALARM_EU_COLUMN), eu);

							fastAlarmTableComposite.setColumn(item, tableDef.getColumnIndex(
									FastAlarmViewConfiguration.ALARM_ERT_COLUMN),time);

							fastAlarmTableComposite.setColumn(item, tableDef.getColumnIndex(
									FastAlarmViewConfiguration.ALARM_SCLK_COLUMN),sclk);
							item.setData("sclk", data.getSclk());

							fastAlarmTableComposite.setColumn(item, tableDef.getColumnIndex(
									FastAlarmViewConfiguration.ALARM_LST_COLUMN), sol);
							item.setData("sol", data.getSol());

							if (scetDate != null && 
									tableDef.isColumnEnabled(tableDef.getColumnIndex(
											FastAlarmViewConfiguration.ALARM_SCET_COLUMN))) {
								final String scetStr = scetDate.getFormattedScet(true);
								fastAlarmTableComposite.setColumn(item, tableDef.getColumnIndex(
										FastAlarmViewConfiguration.ALARM_SCET_COLUMN), scetStr);
							}

							final int stateIndex = tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_ALARM_COLUMN);
							fastAlarmTableComposite.setColumn(item, stateIndex, fullState);

							fastAlarmTableComposite.setColumn(item, tableDef.getColumnIndex(
									FastAlarmViewConfiguration.ALARM_DSS_COLUMN), station);

							fastAlarmTableComposite.setColumn(item, tableDef.getColumnIndex(
									FastAlarmViewConfiguration.ALARM_RECORDED_COLUMN), recorded);

							// mark suspect channels
							fastAlarmTableComposite.setSuspectFont(item, id);

							// highlight alarm columns
							fastAlarmTableComposite.highlightAlarmColumns(item, data);
		}
	}

	private void selectChannels() {
		final ChannelSetEditorShell chanSetShell = new ChannelSetEditorShell(appContext.getBean(IChannelUtilityDictionaryManager.class), 
                appContext.getBean(SprintfFormat.class), this);
		chanSetShell.popupShell(mainShell, new SetUpdateListener(), channelSet, false);
	}

	/**
	 * This class extends the basic alarm table to add extra menus and options.
	 *
	 */
	private class FastAlarmTableComposite extends BasicFastAlarmTableComposite {

		private final MenuItem historyMenuItem;

		/**
		 * Constructor.
		 * @param appContext the current application context
		 *
		 * @param parent parent Composite object for this table
		 * @param config FastAlarmViewConfiguration for the parent view
		 */
		public FastAlarmTableComposite(final ApplicationContext appContext, final Composite parent, final FastAlarmViewConfiguration config) {
			super(appContext, parent, config);

			final Menu viewMenu = table.getMenu();
			new MenuItem(viewMenu, SWT.SEPARATOR);
			final MenuItem prefMenuItem = new MenuItem(viewMenu, SWT.PUSH);
			prefMenuItem.setText("Preferences...");
			final MenuItem editMenuItem = new MenuItem(viewMenu, SWT.PUSH);
			editMenuItem.setText("Select Channels...");
			new MenuItem(viewMenu, SWT.SEPARATOR);
			final MenuItem clearMenuItem = new MenuItem(viewMenu, SWT.PUSH);
			clearMenuItem.setText("Clear Data");
			clearMenuItem.setEnabled(true);
			final MenuItem flushMenuItem = new MenuItem(viewMenu, SWT.PUSH);
			flushMenuItem.setText("Flush Cleared Alarms");

			historyMenuItem = new MenuItem(viewMenu, SWT.PUSH);
			historyMenuItem.setText("Show History...");
			historyMenuItem.setEnabled(false);

			this.addDisposeListener(new DisposeListener() {

				/**
				 * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
				 */
				@Override
				public void widgetDisposed(final DisposeEvent event) {
					try {
						appContext.getBean(MonitorTimers.class).removeAlarmFlushListener(FastAlarmComposite.this);
						stopAlarmFlushTimer();
						final ChannelMessageDistributor cdm = appContext.getBean(ChannelMessageDistributor.class);
						cdm.removeGlobalLadListener(dataListener);
						cdm.removeGlobalAlarmListener(dataListener);

						if (prefShell != null) {
							prefShell.getShell().dispose();
							prefShell = null;
						}

						lad.unregisterStation(FastAlarmComposite.this);

					} catch (final Exception e) {
                        log.error("Error handling channel list main shell disposal ", ExceptionTools.getMessage(e));

						e.printStackTrace();
					}
				}
			});

			clearMenuItem.addSelectionListener(new SelectionAdapter() {
				/**
				 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
				 */
				@Override
				public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
					try {
						clearView();
					} catch (final Exception ex) {
						ex.printStackTrace();
						trace.error("Error in clear menu item handling " + ex.toString());
					}
				}
			});

			flushMenuItem.addSelectionListener(new SelectionAdapter() {
				/**
				 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
				 */
				@Override
				public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
					try {
						syncFlag.acquire();
						flushClearedAlarms(true);
					} catch (final Exception ex) {
						ex.printStackTrace();
						trace.error("Error in clear menu item handling " + ex.toString());
					} finally {
						syncFlag.release();
					}
				}
			});


			historyMenuItem.addSelectionListener(new SelectionAdapter() {
				/**
				 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
				 */
				@Override
				public void widgetSelected(
						final org.eclipse.swt.events.SelectionEvent e) {
					try {
						updateViewConfig();
						final Point p = fastAlarmTableComposite.getSelectionLocation();
						final FastAlarmHistoryShell shell = new FastAlarmHistoryShell(appContext, mainShell.getShell(), p, (FastAlarmViewConfiguration)FastAlarmTableComposite.this.viewConfig);
						final ChannelDisplayFormat displayStuff = fastAlarmTableComposite.getSelectionCharacteristics();
						/*
						 * Realtime recorded filter in the perspective and
						 * LAD is now enum rather than boolean, and station ID is required for LAD
						 * access.
						 */
						final List<MonitorChannelSample> latestData = lad.getValueHistory(fastAlarmTableComposite.getSelectionId(), 
								realtimeRecFilter, 
								((StationSupport)FastAlarmTableComposite.this.viewConfig).getStationId());
						shell.setChannelValues(latestData, displayStuff, true);
						shell.open();
					} catch (final Exception ex) {
						ex.printStackTrace();
						trace.error("Error in history menu item handling " + ex.toString());
					}
				}
			});

			prefMenuItem.addSelectionListener(new SelectionListener() {

				/**
				 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
				 */
				@Override
				public void widgetSelected(final SelectionEvent arg0) {
					try {
						// This kludge works around an SWT bug on Linux
						// in which column sizes are not remembered
						final TableColumn[] cols = FastAlarmTableComposite.this.table
								.getColumns();
						for (int i = 0; i < cols.length; i++) {
							cols[i].setWidth(cols[i].getWidth());
						}
						prefShell = new FastAlarmPreferencesShell(
								appContext, mainShell.getShell());
						prefShell
						.setValuesFromViewConfiguration(FastAlarmComposite.this.viewConfig);
						prefShell.getShell()
						.addDisposeListener(new DisposeListener() {
							@Override
							public void widgetDisposed(
									final DisposeEvent event) {
								try {
									if (!prefShell
											.wasCanceled()) {

										cancelOldSortColumn();
										prefShell
										.getValuesIntoViewConfiguration(FastAlarmComposite.this.viewConfig);


										// Added station unregistration and registration when preferences change.
										// Only need to call registration because registering with the identical object will replace the old staiton value.
										lad
										.registerStation(
												FastAlarmComposite.this,
												FastAlarmComposite.this.viewConfig
												.getStationId());

										updateDataFontAndColors();
										FastAlarmComposite.this.tableDef = FastAlarmComposite.this.viewConfig
												.getTable(FastAlarmViewConfiguration.ALARM_TABLE_NAME);
										syncFlag
										.acquire();
										fastAlarmTableComposite
										.setTableDef(FastAlarmComposite.this.tableDef);
										if (prefShell
												.needColumnChange()) {
											updateTableColumns();
											updateChannelSetComposite();
											FastAlarmTableComposite.this.tableDef
											.setColumnOrder(fastAlarmTableComposite
													.getTable()
													.getColumnOrder());
										}

										/*
										 * Realtime recorded filter in the perspective and
										 * LAD is now enum rather than boolean.
										 */
										final RealtimeRecordedFilterType oldFilter = realtimeRecFilter;
										realtimeRecFilter = ((RealtimeRecordedSupport)viewConfig).getRealtimeRecordedFilterType();
										if(oldFilter != realtimeRecFilter) {
											fastAlarmTableComposite.clearRows();
										}
										updateFilter();
										stopAlarmFlushTimer();
										flushClearedAlarms(false);
										FastAlarmTableComposite.this.table
										.setHeaderVisible(FastAlarmComposite.this.tableDef
												.isShowColumnHeader());
										setSortColumn();
										if (tableDef.isSortAllowed()) {
											table
											.setSortDirection(tableDef
													.isSortAscending() ? SWT.UP
															: SWT.DOWN);
										} else {
											table
											.setSortDirection(SWT.NONE);
										}
										synchronized(volatileIndices) {
											volatileIndices.clear();
										}
										final int newSortColumn = getSortColumnIndex();
										if (newSortColumn != -1) {
											if (!tableDef.isSortAllowed()) {
												tableColumns[newSortColumn].setImage(null);
											} else {
												tableColumns[newSortColumn].setImage(tableDef.isSortAscending() ? upImage : downImage);
											}
										}
										if (FastAlarmComposite.this.tableDef
												.isSortAllowed()
												&& FastAlarmTableComposite.this.table
												.getSortColumn() != null) {
											final int index = FastAlarmComposite.this.tableDef
													.getColumnIndex(FastAlarmComposite.this.tableDef
															.getSortColumn());
											sortTableItems(FastAlarmComposite.this.tableDef
													.getActualIndex(index));
										}
										startAlarmFlushTimer();
										syncFlag
										.release();
										flushTimerFired();
									}
								} catch (final Exception ex) {
									ex.printStackTrace();
									trace
									.error("Unable to handle exit from Preferences window "
											+ ex.toString());
								} finally {
									prefShell = null;
									prefMenuItem.setEnabled(true);
								}
							}
						});
						prefMenuItem.setEnabled(false);
						prefShell.open();
					} catch (final Exception ex) {
						ex.printStackTrace();
						trace.error("Error in preference menu item handling "
								+ ex.toString());
					}
				}

				/**
				 * {@inheritDoc}
				 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
				 */
				@Override
				public void widgetDefaultSelected(final SelectionEvent arg0) {
				}

			});

			editMenuItem.addSelectionListener(new SelectionListener() {

				/**
				 * {@inheritDoc}
				 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
				 */
				@Override
				public void widgetSelected(final SelectionEvent arg0) {
					try {
						editMenuItem.setEnabled(false);
						selectChannels();
						editMenuItem.setEnabled(true);
					} catch (final Exception ex) {
						ex.printStackTrace();
						trace.error("Error in edit menu item handling "
								+ ex.toString());
					}
				}

				/**
				 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
				 */
				@Override
				public void widgetDefaultSelected(final SelectionEvent arg0) {
				}
			});
		}

		/**
		 * {@inheritDoc}
		 * @see jpl.gds.monitor.guiapp.gui.views.AbstractBasicTableComposite#enableMenuItems()
		 */
		@Override
		protected void enableMenuItems() {
			super.enableMenuItems();
			try {
				final int[] i = table.getSelectionIndices();

				if (i != null && i.length != 0) {
					historyMenuItem.setEnabled(true);
				} else {
					historyMenuItem.setEnabled(false);
				}

			} catch (final Exception ex) {
				ex.printStackTrace();
				trace
				.error("Error in table selection handler "
						+ ex.toString());
			}
		}

		// Called when the channel list is updated using the channel selector
		private void updateChannelSetComposite() {
			if (channelSet == null) {
				return;
			}

			final int count = table.getItemCount();

			for (int index = 0; index < count; index++) {
				final TableItem item = table.getItem(index);
				final IChannelDefinition def = (IChannelDefinition) item
						.getData("definition");
				final ChannelDisplayFormat characteristics = (ChannelDisplayFormat) item
						.getData("characteristics");
				if (!characteristics.isSeparator()) {
					final String title = def.getTitle();
					final String fswName = def.getName();
                    final String module = def.getCategory(IChannelDefinition.MODULE);
					final String dnUnits = def.getDnUnits();
					final String euUnits = def.getEuUnits();
					final String chan = def.getId();
					setColumn(item, tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_ID_COLUMN),chan);
					setColumn(item, tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_TITLE_COLUMN),title);
					setColumn(item,tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_FSW_COLUMN),fswName == null ? "" : fswName);
					setColumn(item,tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_DN_UNITS_COLUMN),dnUnits);
					setColumn(item,tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_EU_UNITS_COLUMN),euUnits);
					setColumn(item,tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_MODULE_COLUMN),module == null ? "" : module);
				} else if (characteristics.isLine()) {
					fillWithLine(characteristics.getSeparatorString(), item);
				}
			}
		}

		/**
		 * {@inheritDoc}
		 * @see jpl.gds.monitor.guiapp.gui.views.AbstractBasicTableComposite#sortTableItems(int)
		 */
		@Override
		protected void sortTableItems(final int index) {
			if (tableDef.isSortAllowed() && tableDef.getSortColumn() != null) {
				super.sortTableItems(index);
				channelSet.sort(tableDef
						.isSortAscending());
			}
		}
	}

	private void flushClearedAlarms(final boolean force) {

		final long interval = viewConfig.getResetFlushInterval() * 60000L;
		if (interval == 0 && !force) {
			return;
		}
		final long curTime = new AccurateDateTime().getTime();
		final ArrayList<String> removeItems = new ArrayList<String>();
		for (final String id: channelsAwaitingExpiration) {
			final TableItem item = fastAlarmTableComposite.tableItems.get(id);
			//if item has been cleared from table previously, we don't want to do anything
			if(item == null) {
				continue;
			}
			final Long clearTime = (Long) item.getData("clearTime");
			if (clearTime == null) {
				continue;
			}
			if (force || curTime - clearTime > interval) {
				removeItems.add(id);
				fastAlarmTableComposite.tableItems.remove(id);
				item.dispose();
			}
		}
		final Iterator<String> it = removeItems.iterator();
		while (it.hasNext()) {
			channelsAwaitingExpiration.remove(it.next());
		}
	}

	/**
	 * Add and remove channels from table according to current filter.
	 */
	private void updateFilter() {
		setFilterLabel();

		//Remove channels
		final TableItem[] items = fastAlarmTableComposite.getTable().getItems();
		for (int i = items.length - 1; i >= 0; i--) {
			final IChannelDefinition def = (IChannelDefinition) items[i].getData("definition");

			// get latest channel data for given id
			/*
			 * Realtime recorded filter in the perspective and
			 * LAD is now enum rather than boolean and station ID is required for LAD access.
			 */
			/* Get chanId up front since it's
			 * used a lot in the following code */
			final String chanId = def.getId();
			final MonitorChannelSample data = lad.getMostRecentValue(
					chanId, realtimeRecFilter, viewConfig.getStationId());

			// To remain on the display, the channel must first of all match everything 
			// but the level filter.  If it does not match the level filter, AND is not 
			// awaiting expiration, it should be removed. If it does not match the level
			// filter, but is awaiting expiration, remove it.
			if (data == null || !alarmFilter.acceptNoLevel(data) ||
					(!alarmFilter.acceptLevel(data.getDnAlarmLevel(), data.getEuAlarmLevel()) && !channelsAwaitingExpiration.contains(chanId)))
			{
				fastAlarmTableComposite.tableItems.remove(chanId);
				channelsAwaitingExpiration.remove(chanId);
				items[i].dispose();
				items[i] = null;
				channelsInAlarm.remove(chanId);
			}
		}

		//Add channels
		/*
		 * Realtime recorded filter in the perspective and
		 * LAD is now enum rather than boolean and station ID is required for LAD access.
		 */
		final List<MonitorChannelSample> latestData = lad.getLatestValues(alarmFilter, realtimeRecFilter, viewConfig.getStationId(), null);
		if (latestData != null) {
			// for each channel we want in the list...
			for (final MonitorChannelSample data : latestData) {
				if (data != null) {
					final String chanId = data.getChanId();
					if(!fastAlarmTableComposite.tableItems.containsKey(chanId)) {

						/* Get chanDef from
						 * ChannelSample. Surround createTableItem with try/catch 
						 * in case mysterious NPE happens again */
						final IChannelDefinition chanDef = data.getChanDef();

						try {
							final TableItem item = createTableItem(chanDef, data);
							fastAlarmTableComposite.highlightAlarmColumns(item, data);
						} catch (final NullPointerException e) {
                            log.error("Error creating table item " + e.toString());

							e.printStackTrace();
						}
					}
				}
			}   
		}
	}

	private TableItem createTableItem(final IChannelDefinition def, final MonitorChannelSample data) {

		final String title = def.getTitle();
		final String fswName = def.getName();
		final String module = def.getModule();
		final String dnUnits = def.getDnUnits();
		final String euUnits = def.hasEu() ? def.getEuUnits() : def.getDnUnits();

		if (data == null) {
			throw new IllegalArgumentException("data argument cannot be null");
		}

		final String alarmTime = timeFormat.format(data.getErt());
		final String ert = alarmTime;
		/* Handle global SCLK format flag. */
		final String sclk = globalConfig.getValue(GlobalPerspectiveParameter.SCLK_FORMAT).equals(SclkFormat.DECIMAL) ?
		        data.getSclk().toDecimalString() : data.getSclk().toTicksString();
		final String scet = timeFormat.format(data.getScet());
		String sol = "";
		if (useSolTimes) {
		    if (data.getSol() != null) {
		        sol = data.getSol().getFormattedSol(true);
		    } else {
		        sol = LocalSolarTimeFactory.getNewLst(0).getFormattedSol(true);
		    }
		}
		final String state = fastAlarmTableComposite.getAlarmState(data);
		final String dn = getFormattedDn(data);
		final String eu = getFormattedEu(data);

		/* Set station and recorded values for
		 * the table item */
		final String station = data.getDssId() != 
				StationIdHolder.UNSPECIFIED_VALUE ? 
						String.valueOf(data.getDssId()) : 
							DisplayConstants.UNSPECIFIED_STATION;
						final String recorded = String.valueOf(!data.isRealtime());

						final TableItem item = fastAlarmTableComposite.createTableItem(new String[] {
								def.getId(), title, module, fswName, dn, dnUnits,
								eu, euUnits, ert, sclk, scet, "---", "---", state, sol, 
								station, recorded });

						channelsInAlarm.add(def.getId());
						final AlarmLevel dnLevel = data.getDnAlarmLevel();
						final AlarmLevel euLevel = data.getEuAlarmLevel();
						item.setData("alarmLevel", euLevel.ordinal() > dnLevel.ordinal() ? euLevel : dnLevel);

						item.setData("definition", def);
						item.setData("clearTime", null);
						final IChannelDefinitionProvider defProv = appContext.getBean(IChannelDefinitionProvider.class);
						if (viewConfig.getChannels(defProv) != null && !viewConfig.getChannels(defProv).isEmpty()) {
							final ChannelDisplayFormat display = viewConfig.getChannels(defProv).getDisplayCharacteristics(def.getId());
							if (display != null) {
								item.setData("characteristics", display);
							}
						} else {
							final ChannelDisplayFormat display = new ChannelDisplayFormat(def, def.getId(), false);
							item.setData("characteristics", display);
						}
						fastAlarmTableComposite.tableItems.put(def.getId(), item);

						return item;
	}

	private String getFormattedDn(final MonitorChannelSample data) {
		ChannelDisplayFormat displayStuff = null;

		final IChannelDefinitionProvider defProv = appContext.getBean(IChannelDefinitionProvider.class);
		
		if (viewConfig.getChannels(defProv) != null) {
			displayStuff = viewConfig.getChannels(defProv).getDisplayCharacteristics(data.getChanId());
		}

		if (displayStuff != null) {
			return data.getDnValue().getFormattedValue(formatUtil, displayStuff.getRawFormat());
		} else {
			return data.getDnValue().getFormattedValue(formatUtil);
		}
	}

	private String getFormattedEu(final MonitorChannelSample data) {
		ChannelDisplayFormat displayStuff = null;
		if (data.getEuValue() == null) {
			return "";
		}

		final IChannelDefinitionProvider defProv = appContext.getBean(IChannelDefinitionProvider.class);
		if (viewConfig.getChannels(defProv) != null) {
			displayStuff = viewConfig.getChannels(defProv).getDisplayCharacteristics(data.getChanId());
		}

		if (displayStuff != null) {
			return data.getEuValue().getFormattedValue(formatUtil, displayStuff.getValueFormat());
		} else {
			return data.getEuValue().getFormattedValue(formatUtil);
		}
	}

	/**
	 * This class is the listener for the channel set editor window. It's called when that window
	 * is exited.
	 *
	 */
	private class SetUpdateListener implements ChannelSetUpdateListener {
		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * jpl.gds.swt.common.channelViews.ChannelSetUpdateListener#updateSet(jpl.gds.channel.ChannelSet)
		 */
		@Override
		public boolean updateSet(final ChannelSet set) {
			if (tableDef.isSortAllowed()) {
				final boolean result = SWTUtilities
						.showConfirmDialog(
								mainShell.getShell(),
								"Sort Confirmation",
								"Sorting is enabled, which may change your selected channel ordering.\n Do you want to disable sorting?");
				if (result) {
					tableDef.setSortAllowed(false);
				}

			}

			viewConfig.setChannels(set);
			channelSet = set;

			updateFilter();
			return true;

		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.perspective.view.View#updateViewConfig()
	 */
	@Override
	public void updateViewConfig() {
		boolean columnWidthError = false;
		viewConfig.setChannels(channelSet);
		final TableColumn[] tableColumns = fastAlarmTableComposite.getColumns();
		if (tableColumns != null) {
			for (int i = 0; i < tableColumns.length; i++) {
				if (tableColumns[i] != null) {
					if (tableColumns[i].getWidth() == 0) {
						columnWidthError = true;
						break;
					}
					tableDef.setColumnWidth(i, tableColumns[i].getWidth());
				}
			}
		}
		if (tableDef.isSortAllowed() && fastAlarmTableComposite.getTable().getSortColumn() != null) {
			tableDef.setSortColumn(fastAlarmTableComposite.getTable().getSortColumn().getText());
		}

		tableDef.setColumnOrder(fastAlarmTableComposite.getTable().getColumnOrder());
		if (columnWidthError && !WarningUtil.getWidthWarningShown()) {
			SWTUtilities.showWarningDialog(mainShell.getShell(), 
					"Save Warning for View " + viewConfig.getViewName(),
					"Table column widths with 0 values were found during the save of the perspective. " +
							"This is a known bug with the windowing toolkit. The widths of these table " +
							"columns will not be saved (will be left as the previous value). If you " +
							"wish to remove a column, please use the Preferences window rather than " +
							"the mouse to remove the column. All other changes in your perspective " +
					"will be saved.");
			WarningUtil.setWidthWarningShown(true);
		}
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.perspective.view.View#getViewConfig()
	 */
	@Override
	public IViewConfiguration getViewConfig() {
		return viewConfig;
	}

	/**
	 * {@inheritDoc}
	 * @return Returns the maxChannelSelectionSize.
	 */
	@Override
	public long getMaxChannelSelectionSize()
	{
		return maxChannelSelectionSize;
	}

	/**
	 * Sets the maxChannelSelectionSize
	 * 
	 * @param maxChannelSelectionSize
	 *            The maxChannelSelectionSize to set.
	 */
	@Override
	public void setMaxChannelSelectionSize(final long maxChannelSelectionSize) {
		this.maxChannelSelectionSize = maxChannelSelectionSize;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.perspective.view.View#getDefaultName()
	 */
	@Override
	public String getDefaultName() {
		return TITLE;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.perspective.view.View#clearView()
	 */
	@Override
	public void clearView() {
		fastAlarmTableComposite.clearRows();
		try {
			syncFlag.acquire();
			lastClearTime = System.currentTimeMillis();
			fastAlarmTableComposite.clearRows();
			channelsInAlarm.clear();
			channelsAwaitingExpiration.clear();
		} catch (final Exception ex) {
			ex.printStackTrace();
            log.error("Error clearing alarm view " + ex.toString());

		} finally {
			if (syncFlag.availablePermits() == 0) {
				syncFlag.release();
			}
		}
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

	private void displayMessage(final IMessage msg) {
		if (parent.isDisposed()) {
			return;
		}
		parent.getDisplay().asyncExec(new Runnable() {

			/**
			 * @see java.lang.Object#toString()
			 */
			@Override
			public String toString() {
				return "FastAlarmComposite.displayMessage.Runnable";
			}

			/**
			 * @see java.lang.Runnable#run()
			 */
			@Override
			public void run() {
				try {
					if (fastAlarmTableComposite.isDisposed()) {
						return;
					}
                    log.trace("fast alarm view is processing message ", msg.getType());


					if (msg.isType(EhaMessageType.SuspectChannels)) {
						fastAlarmTableComposite.handleSuspectChannelsMessage((ISuspectChannelsMessage) msg);
					}
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.guiapp.common.ChannelFlushListener#flushTimerFired()
	 */
	@Override
	public void flushTimerFired() {
		if (parent.isDisposed() || parent.getDisplay().isDisposed()) {
			return;
		}

		try {
			if (fastAlarmTableComposite.isDisposed()) {
				return;
			}

			if (!fastAlarmTableComposite.isVisible()) {
				return;
			}

			syncFlag.acquire();


			// merge the channels we want filtered in with the channelsInAlarm list
			final List<String> channelsOnDisplay = new ArrayList<String>(channelsInAlarm);
			channelsOnDisplay.addAll(channelsAwaitingExpiration);

			/*
			 * Realtime recorded filter in the perspective and
			 * LAD is now enum rather than boolean and station ID is required for LAD access.
			 */
			final List<MonitorChannelSample> latestData = lad.getLatestValues(alarmFilter, realtimeRecFilter, 
					this.viewConfig.getStationId(), channelsOnDisplay);

			if (latestData != null) {
				// for each channel we want in the list...
				for (final MonitorChannelSample data : latestData) {
					if (data != null && data.getTimestamp() > lastClearTime) {
						final String chanId = data.getChanId();
						final TableItem item = fastAlarmTableComposite.tableItems.get(chanId);
						if (item != null) {
							dataListener.processUpdatedAlarmChannel(chanId, data, item);
						} else {
							dataListener.processNewAlarmChannel(chanId, data);
						}
					}
				}

				flushClearedAlarms(false);

				// sort the table if needed.
				if (fastAlarmTableComposite.getTable().getSortColumn() != null && tableDef.isSortAllowed())
				{
					final int index = tableDef.getColumnIndex(tableDef.getSortColumn());
					// no need to sort if sort column is not one that changes
					if (isVolatileIndex(index)) {
						fastAlarmTableComposite.sortTableItems(index);
					}
				} 
				fastAlarmTableComposite.redraw();									

				updateFilter();
			}
			syncFlag.release();
		}

		catch (final Exception e) {
			e.printStackTrace();
			return;
		}
	}
}
