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
import java.util.Iterator;
import java.util.List;

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
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.alarm.AlarmLevel;
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
import jpl.gds.monitor.guiapp.common.ChannelFlushListener;
import jpl.gds.monitor.guiapp.common.GeneralMessageDistributor;
import jpl.gds.monitor.guiapp.common.GeneralMessageListener;
import jpl.gds.monitor.guiapp.common.gui.DisplayConstants;
import jpl.gds.monitor.guiapp.gui.views.preferences.ChannelListPreferencesShell;
import jpl.gds.monitor.guiapp.gui.views.support.ChannelHistoryShell;
import jpl.gds.monitor.guiapp.gui.views.support.ChannelSetConsumer;
import jpl.gds.monitor.guiapp.gui.views.support.ChannelSetEditorShell;
import jpl.gds.monitor.guiapp.gui.views.support.ChannelSetUpdateListener;
import jpl.gds.monitor.perspective.view.ChannelListViewConfiguration;
import jpl.gds.monitor.perspective.view.RealtimeRecordedSupport;
import jpl.gds.monitor.perspective.view.StationSupport;
import jpl.gds.monitor.perspective.view.channel.ChannelDisplayFormat;
import jpl.gds.monitor.perspective.view.channel.ChannelSet;
import jpl.gds.monitor.perspective.view.channel.MonitorChannelLad;
import jpl.gds.monitor.perspective.view.channel.MonitorChannelSample;
import jpl.gds.perspective.ChillTable;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.perspective.view.View;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.swt.ProgressBarShell;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.LocalSolarTimeFactory;

/**
 * This class manages a shell with a list of channels updated with LAD (Latest
 * Available Data). Channels can be selected using the ChannelSetEditorShell
 * class or else specified directly in the configuration using a ChannelSet
 * file. The class also allows the editing of the channel title and otherwise
 * can be saved as part of an overall display configuration.
 * 
 */
@SuppressWarnings("PMD.ImmutableField")
public class ChannelListPageComposite implements View, ChannelSetConsumer, GeneralMessageListener, ChannelFlushListener
{
    private final MonitorConfigValues globalConfig;

	/**
	 * Channel list composite title
	 */
	public static final String TITLE = "Channel List";


	private long maxChannelSelectionSize;

	private Composite parent;
	private Composite mainShell;
	private ChannelTableComposite channelTableComposite;
	private ChannelSet channelSet;
	private final ChannelListViewConfiguration viewConfig;
	private ChannelListPreferencesShell prefShell;
	private ChillTable tableDef;
	private final MonitorChannelLad lad;
	private long lastClearTime;
	private final ApplicationContext appContext;


	/**
	 * Constructor
	 *
	 * Create a ChannelListPageComposite with the given view configuration.
	 * @param appContext the current application context
	 *
	 * @param config the ChannelListViewConfiguration containing view settings
	 */
	public ChannelListPageComposite(final ApplicationContext appContext, final IViewConfiguration config)
	{
		this.appContext = appContext;
		globalConfig = appContext.getBean(MonitorConfigValues.class);
		lad = appContext.getBean(MonitorChannelLad.class);
		
		this.viewConfig = (ChannelListViewConfiguration)config;
		this.tableDef = this.viewConfig.getTable(ChannelListViewConfiguration.CHANNEL_TABLE_NAME);
		this.channelSet = this.viewConfig.getChannels(appContext.getBean(IChannelDefinitionProvider.class));
		this.maxChannelSelectionSize = appContext.getBean(MonitorGuiProperties.class).getListMaxChannels();

	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.perspective.view.View#getMainControl()
	 */
	@Override
	public Control getMainControl() {
		return this.mainShell;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.perspective.view.View#init(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void init(final Composite parent) {
		this.parent = parent;
		createGui();
		appContext.getBean(MonitorTimers.class).addChannelFlushListener(this);
		appContext.getBean(GeneralMessageDistributor.class).addDataListener(this, EhaMessageType.SuspectChannels);

		lad.registerStation(this, viewConfig.getStationId());
	}

	/**
	 * Creates the GUI controls for this composite.
	 */
	private void createGui()
	{
		this.mainShell   = new Composite(this.parent, SWT.NONE);
		final FormLayout layout = new FormLayout();
		this.mainShell.setLayout(layout);

		this.channelTableComposite = new ChannelTableComposite(appContext, this.mainShell, this.viewConfig);
		final FormData data = new FormData();
		data.top = new FormAttachment(0);
		data.right = new FormAttachment(100);
		data.left = new FormAttachment(0);
		data.bottom = new FormAttachment(100);
		this.channelTableComposite.setLayoutData(data);

		this.channelTableComposite.rebuildChannelSetComposite();
	}

	/**
	 * Processes an incoming channel sample and updates the display.
	 */
	private void receive(final MonitorChannelSample data)
	{
		if (this.mainShell.isDisposed()) {
			return;
		}

		/*
		 * Removed data filtering no longer necessary. This method is now only called
		 * by flushTimerFired(), which filters what it gets from the LAD.
		 */

		final IChannelDefinition def = appContext.getBean(IChannelDefinitionProvider.class).getDefinitionFromChannelId(data.getChanId());
		if (def != null) {
			this.channelTableComposite.updateChannelWithData(def, data);
		}
	}

	private void selectChannels() {
		final ChannelSetEditorShell chanSetShell = new ChannelSetEditorShell(appContext.getBean(IChannelUtilityDictionaryManager.class), 
                appContext.getBean(SprintfFormat.class), this);
		chanSetShell.popupShell(this.mainShell, new SetUpdateListener(), this.channelSet, true);
	}

	/**
	 * Creates the right-click menu, adds channels to the table and updates 
	 * the channel values in the table
	 *
	 */
	@SuppressWarnings("PMD.IdempotentOperations")
	private class ChannelTableComposite extends BasicChannelTableComposite {

		private final MenuItem historyMenuItem;

		/**
		 * Constructor.
		 * @param appContext the current application context
		 * 
		 * @param parent parent composite to this one
		 * @param config the view configuration for this channel list view
		 */
		public ChannelTableComposite(final ApplicationContext appContext, final Composite parent, final ChannelListViewConfiguration config) {
			super (appContext, parent, config);

			final Menu viewMenu = this.table.getMenu();
			new MenuItem(viewMenu, SWT.SEPARATOR);
			final MenuItem prefMenuItem = new MenuItem(viewMenu, SWT.PUSH);
			prefMenuItem.setText("Preferences...");
			final MenuItem editMenuItem = new MenuItem(viewMenu, SWT.PUSH);
			editMenuItem.setText("Select Channels...");
			new MenuItem(viewMenu, SWT.SEPARATOR);
			final MenuItem clearMenuItem =  new MenuItem(viewMenu, SWT.PUSH);
			clearMenuItem.setText("Clear Data");
			clearMenuItem.setEnabled(true);

			this.historyMenuItem =  new MenuItem(viewMenu, SWT.PUSH);
			this.historyMenuItem.setText("Show History...");
			this.historyMenuItem.setEnabled(false);

			this.addDisposeListener(new DisposeListener() {

				/**
				 * {@inheritDoc}
				 * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
				 */
				@Override
				public void widgetDisposed(final DisposeEvent event) {
					try {
						appContext.getBean(MonitorTimers.class).removeChannelFlushListener(ChannelListPageComposite.this);

						/*
						 * Removed de-registration of ChannelSampleListeners with the ChannelMessageDistributor.
						 * This composite class longer contains ChannelSampleListeners. It gets all
						 * data from the LAD.
						 */

						if (ChannelListPageComposite.this.prefShell != null) {
							ChannelListPageComposite.this.prefShell.getShell().dispose();
							ChannelListPageComposite.this.prefShell = null;
						}

						// Station unregistration when the view is disposed
						lad.unregisterStation(ChannelListPageComposite.this);

					} catch (final Exception e) {
						TraceManager.getDefaultTracer().error("Error handling channel list main shell disposal " + e.toString());

						e.printStackTrace();
					}
				}
			});

			clearMenuItem.addSelectionListener(new SelectionAdapter() {
				/**
				 * {@inheritDoc}
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

			this.historyMenuItem.addSelectionListener(new SelectionAdapter() {
				/**
				 * {@inheritDoc}
				 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
				 */
				@Override
				public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
					try {
						updateViewConfig();
						final Point p = ChannelListPageComposite.this.channelTableComposite.getSelectionLocation();
						final ChannelHistoryShell shell = new ChannelHistoryShell(appContext, ChannelListPageComposite.this.mainShell.getShell(), p, (ChannelListViewConfiguration)ChannelTableComposite.this.viewConfig);
						final ChannelDisplayFormat displayStuff = ChannelListPageComposite.this.channelTableComposite.getSelectionCharacteristics();
						/*
						 * Realtime recorded filter in the perspective and
						 * LAD is now enum rather than boolean, and station ID is required for LAD
						 * access.
						 */
						final List<MonitorChannelSample> latestData = ChannelListPageComposite.this.lad.getValueHistory(ChannelListPageComposite.this.channelTableComposite.getSelectionId(), 
								((RealtimeRecordedSupport)ChannelTableComposite.this.viewConfig).getRealtimeRecordedFilterType(), 
								((StationSupport)ChannelTableComposite.this.viewConfig).getStationId());
						shell.setChannelValues(latestData, displayStuff, true);
						shell.open();
					} catch (final Exception ex) {
						ex.printStackTrace();
						trace.error("Error in history menu item handling " + ex.toString());
					}
				}
			});

			prefMenuItem.addSelectionListener(new SelectionListener()  {

				/**
				 * {@inheritDoc}
				 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
				 */
				@Override
				public void widgetSelected(final SelectionEvent arg0)
				{
					try {
						// This kludge works around an SWT bug on Linux
						// in which column sizes are not remembered
						final TableColumn[] cols = ChannelTableComposite.this.table.getColumns();
						for (int i = 0; i < cols.length; i++) {
							cols[i].setWidth(cols[i].getWidth());
						}
						ChannelListPageComposite.this.prefShell = new ChannelListPreferencesShell(appContext, ChannelListPageComposite.this.mainShell.getShell());
						ChannelListPageComposite.this.prefShell.setValuesFromViewConfiguration(ChannelListPageComposite.this.viewConfig);
						ChannelListPageComposite.this.prefShell.getShell().addDisposeListener(new DisposeListener() {
							/**
							 * {@inheritDoc}
							 * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
							 */
							@Override
							public void widgetDisposed(final DisposeEvent event) {
								try {
									if (!ChannelListPageComposite.this.prefShell.wasCanceled()) {

										/*
										 * Clear the table. Preferences
										 * may have changed.
										 */
										ChannelListPageComposite.this.channelTableComposite.clearTable();

										cancelOldSortColumn();

										ChannelListPageComposite.this.prefShell.getValuesIntoViewConfiguration(ChannelListPageComposite.this.viewConfig);

										// Added station unregistration and registration when preferences change. Only need to call registration because registering with the identical object will replace the old staiton value.
										lad
										.registerStation(
												ChannelListPageComposite.this,
												ChannelListPageComposite.this.viewConfig
												.getStationId());

										updateDataFontAndColors();
										updateSeparatorFonts();
										/*
										 * PMD idempotent warning suppressed here on assignment of tableDef.
										 */
										ChannelListPageComposite.this.tableDef = ChannelListPageComposite.this.viewConfig.getTable(ChannelListViewConfiguration.CHANNEL_TABLE_NAME);
										ChannelListPageComposite.this.channelTableComposite.setTableDef(ChannelListPageComposite.this.tableDef);
										if (ChannelListPageComposite.this.prefShell.needColumnChange()) {
											updateTableColumns();
											updateChannelSetComposite();
											ChannelTableComposite.this.tableDef.setColumnOrder(ChannelListPageComposite.this.channelTableComposite.getTable().getColumnOrder());
										}
										ChannelTableComposite.this.table.setHeaderVisible(ChannelListPageComposite.this.tableDef.isShowColumnHeader());
										if (ChannelTableComposite.this.tableDef.isSortAllowed() && ChannelTableComposite.this.tableDef.getSortColumn() != null) {
											ChannelTableComposite.this.table.setSortDirection(ChannelTableComposite.this.tableDef.isSortAscending() ? SWT.UP : SWT.DOWN);
										} else {
											ChannelTableComposite.this.table.setSortDirection(SWT.NONE);
										}

										final int newSortColumn = getSortColumnIndex();
										if (newSortColumn != -1) {
											if (!ChannelTableComposite.this.tableDef.isSortAllowed()) {
												ChannelTableComposite.this.tableColumns[newSortColumn].setImage(null);
											} else {
												ChannelTableComposite.this.tableColumns[newSortColumn].setImage(ChannelTableComposite.this.tableDef.isSortAscending() ? upImage : downImage);
											}
										}
										if (ChannelListPageComposite.this.tableDef.isSortAllowed() && ChannelTableComposite.this.table.getSortColumn() != null) {
											final int index = ChannelListPageComposite.this.tableDef.getColumnIndex(ChannelListPageComposite.this.tableDef.getSortColumn());
											sortTableItems(index);
										}
										flushTimerFired();
									}
								} catch (final Exception ex) {
									ex.printStackTrace();
									trace.error("Unable to handle exit from Preferences window " + ex.toString());
								} finally {
									ChannelListPageComposite.this.prefShell = null;
									prefMenuItem.setEnabled(true);
								}
							}
						});
						prefMenuItem.setEnabled(false);
						ChannelListPageComposite.this.prefShell.open();
					} catch (final Exception ex) {
						ex.printStackTrace();
						trace.error("Error in preference menu item handling " + ex.toString());
					}
				}

				@Override
				public void widgetDefaultSelected(final SelectionEvent arg0)
				{
					// do nothing
				}

			});

			editMenuItem.addSelectionListener(new SelectionListener() {

				@Override
				public void widgetSelected(final SelectionEvent arg0)
				{
					try {
						editMenuItem.setEnabled(false);
						selectChannels();
						editMenuItem.setEnabled(true);
					} catch (final Exception ex) {
						ex.printStackTrace();
						trace.error("Error in edit menu item handling " + ex.toString());
					}
				}

				@Override
				public void widgetDefaultSelected(final SelectionEvent arg0)
				{
					// do nothing
				}
			});
		}

		@Override
		protected void enableMenuItems() {
			super.enableMenuItems();
			try {
				final int[] i = this.table.getSelectionIndices();

				if (i != null && i.length != 0) {
					this.historyMenuItem.setEnabled(true);
				} else {
					this.historyMenuItem.setEnabled(false);
				}
				if (i != null && i.length != 0) {

					final ChannelDisplayFormat displayStuff = ChannelListPageComposite.this.channelTableComposite.getSelectionCharacteristics();
					if (displayStuff.isSeparator()) {
						this.historyMenuItem.setEnabled(false);
					}
				}
			} catch (final Exception ex) {
				ex.printStackTrace();
				trace.error("Error in table selection handler " + ex.toString());
			}
		}

		public void updateTable(final int index, final MonitorChannelSample data, final TableItem item,  final IChannelDefinition def) {

			/*
			 * Removed logic that was checking the display SCLKs out of order flag. This
			 * handling is no longer required because the LAD accepts nothing 
			 * out of order.
			 */

		    final String time = this.timeFormat.format(data.getErt());
		    /* Handle global SCLK format flag. */
		    final String sclk = globalConfig.getValue(GlobalPerspectiveParameter.SCLK_FORMAT).equals(SclkFormat.DECIMAL) ?
		            data.getSclk().toDecimalString() : data.getSclk().toTicksString();
		            final IAccurateDateTime scetDate = data.getScet();
		            final ILocalSolarTime solDate = data.getSol();
			/*
			 * Populate DSS ID and Recorded columns.
			 */
			final String recorded = String.valueOf(!data.isRealtime());
			final String dssId = data.getDssId() != StationIdHolder.UNSPECIFIED_VALUE ? String.valueOf(data.getDssId()) :
				DisplayConstants.UNSPECIFIED_STATION;

			final ChannelDisplayFormat displayStuff = (ChannelDisplayFormat)item.getData("characteristics");

			final String dn = data.getDnValue().getFormattedValue(formatUtil, displayStuff.getRawFormat());
			String eu = "";
			if (data.getEuValue() != null) {
				eu = data.getEuValue().getFormattedValue(formatUtil, displayStuff.getValueFormat());
			}

			//Set font to be italics if channel is suspect, 
			ChannelListPageComposite.this.channelTableComposite.setSuspectFont(item, data.getChanId());

			setColumn(item, ChannelListPageComposite.this.tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_DN_COLUMN), dn);
			setColumn(item, ChannelListPageComposite.this.tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_EU_COLUMN), eu);
			setColumn(item, ChannelListPageComposite.this.tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_ERT_COLUMN), time);
			setColumn(item, ChannelListPageComposite.this.tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_SCLK_COLUMN), sclk);
			item.setData("sclk",  data.getSclk());
			if (scetDate != null && ChannelListPageComposite.this.tableDef.isColumnEnabled(ChannelListPageComposite.this.tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_SCET_COLUMN))) {
				final String scetStr = scetDate.getFormattedScet(true);
				setColumn(item, ChannelListPageComposite.this.tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_SCET_COLUMN), scetStr);
			}
			if (useSolTimes) {
				if (solDate != null && ChannelListPageComposite.this.tableDef.isColumnEnabled(ChannelListPageComposite.this.tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_LST_COLUMN))) {
					final String solStr = solDate.getFormattedSol(true);
					setColumn(item, ChannelListPageComposite.this.tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_LST_COLUMN), solStr);
				} else if (solDate == null && ChannelListPageComposite.this.tableDef.isColumnEnabled(ChannelListPageComposite.this.tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_LST_COLUMN))) {
					setColumn(item, ChannelListPageComposite.this.tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_LST_COLUMN), LocalSolarTimeFactory.getNewLst(0).getFormattedSol(true));				
				}
			}
			final String state = ChannelListPageComposite.this.channelTableComposite.getAlarmState(data);

			final AlarmLevel dnlevel = data.getDnAlarmLevel();
			int dataIndex = ChannelListPageComposite.this.tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_DN_COLUMN);
			setAlarmColors(item, dnlevel, dataIndex);

			final AlarmLevel eulevel = data.getEuAlarmLevel();
			dataIndex = ChannelListPageComposite.this.tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_EU_COLUMN);
			setAlarmColors(item, eulevel, dataIndex);

			if (eulevel == AlarmLevel.NONE && dnlevel == AlarmLevel.NONE) {
				item.setData("alarmLevel", null);
				setAlarmColors(item, AlarmLevel.NONE, this.tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_ALARM_COLUMN));
			} else {
				final AlarmLevel worst = eulevel.ordinal() > dnlevel.ordinal() ? eulevel : dnlevel;
				item.setData("alarmLevel", worst);
				setAlarmColors(item, worst, this.tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_ALARM_COLUMN));
			}

			setColumn(item, ChannelListPageComposite.this.tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_ALARM_COLUMN), state);

			/*
			 * Populate DSS ID and Recorded columns.
			 */
			setColumn(item, ChannelListPageComposite.this.tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_DSS_COLUMN), dssId);
			setColumn(item, ChannelListPageComposite.this.tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_RECORDED_COLUMN), recorded);
		}

		private void rebuildChannelSetComposite()
		{
			if (ChannelListPageComposite.this.channelSet == null) {
				return;
			}

			/*
			 * Removed de-registration of ChannelSampleListeners with the ChannelMessageDistributor.
			 * This composite class longer contains ChannelSampleListeners. It gets all
			 * data from the LAD.
			 */

			ChannelListPageComposite.this.channelTableComposite.clearRows();

			final ChannelDisplayFormat[] ids = ChannelListPageComposite.this.channelSet.getDisplayCharacteristics();

			final ChannelSetEditorShell chanSetShell = new ChannelSetEditorShell(appContext.getBean(IChannelUtilityDictionaryManager.class), 
	                appContext.getBean(SprintfFormat.class), ChannelListPageComposite.this);
			ProgressBarShell shell = null;
			if (ids.length > 50) {
				shell = new ProgressBarShell(chanSetShell.getShell());

				shell.getShell().setText("Import Channels");
				shell.getProgressLabel().setText("Importing Channel Selections: ");
				shell.getProgressBar().setMinimum(0);
				shell.getProgressBar().setMaximum(ids.length + 1);
				shell.getProgressBar().setSelection(0);
				shell.getShell().setSize(500,50);

				shell.open();
			}

			for (int index = 0; index < ids.length; index++)
			{
				if (!ids[index].isSeparator())  {          
					final String chan = ids[index].getChanId();

					/*
					 * Removed registration of ChannelSampleListener with the ChannelMessageDistributor.
					 * This composite class longer contains ChannelSampleListeners. It gets all
					 * data from the LAD.
					 */

					final IChannelDefinition def = appContext.getBean(IChannelDefinitionProvider.class).getDefinitionFromChannelId(chan);

					if (def == null) {
						trace.warn("Channel in list page not found in dictionary: " + chan);
						continue;
					}

					final String title = def.getTitle();
					final String fswName = def.getName();
					final String module = def.getModule();
					final String dnUnits = def.getDnUnits();
					final String euUnits = def.hasEu() ? def.getEuUnits() : def.getDnUnits();
					final TableItem item = new TableItem(this.table, SWT.NONE);
					ArrayList<TableItem> existingItems = this.tableItems.get(chan);
					if (existingItems == null) {
						existingItems = new ArrayList<TableItem>(1);
						this.tableItems.put(chan, existingItems);
					}
					existingItems.add(item);
					item.setData("definition", def);
					item.setData("characteristics", ids[index]);
					setColumn(item, this.tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_ID_COLUMN), chan);
					setColumn(item, this.tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_TITLE_COLUMN), title);
					setColumn(item, this.tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_FSW_COLUMN), fswName == null ? "" : fswName);
					setColumn(item, this.tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_DN_UNITS_COLUMN), dnUnits);
					setColumn(item, this.tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_EU_UNITS_COLUMN), euUnits);
					setColumn(item, this.tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_MODULE_COLUMN), module == null ? "" : module);

					if (shell != null) {
						shell.getProgressBar().setSelection(index);
					}
				} else {
					final TableItem item = new TableItem(this.table, SWT.NONE);
					item.setData("characteristics", ids[index]);
					if (ids[index].isLine()) {
						fillWithLine(ids[index].getSeparatorString(), item);
					} else {
						item.setText(0, ids[index].getSeparatorString());
					}
					item.setFont(this.dataFont);
				}
			}

			if (this.tableDef.isSortAllowed() && this.table.getSortColumn() != null) {
				final int index = this.tableDef.getColumnIndex(this.tableDef.getSortColumn());
				sortTableItems(this.tableDef.getActualIndex(index));
			}

			if (shell != null) {
				shell.getProgressBar().setSelection(shell.getProgressBar().getMaximum());
				shell.dispose();
				shell = null;
			}
		}

		private void updateChannelSetComposite()
		{
			final int count = this.table.getItemCount();

			for (int index = 0; index < count; index++)
			{
				final TableItem item = this.table.getItem(index);
				final IChannelDefinition def = (IChannelDefinition)item.getData("definition");
				final ChannelDisplayFormat characteristics = (ChannelDisplayFormat)item.getData("characteristics");

				if (def == null && !characteristics.isSeparator()) {
					continue;
				}

				if (!characteristics.isSeparator()) {
					final String title = def.getTitle();
					final String fswName = def.getName();
					final String module = def.getModule();
					final String dnUnits = def.getDnUnits();
					final String euUnits = def.getEuUnits();
					final String chan = def.getId();
					setColumn(item, this.tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_ID_COLUMN), chan);
					setColumn(item, this.tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_TITLE_COLUMN), title);
					setColumn(item, this.tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_FSW_COLUMN), fswName == null ? "" : fswName);
					setColumn(item, this.tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_DN_UNITS_COLUMN), dnUnits);
					setColumn(item, this.tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_EU_UNITS_COLUMN), euUnits);
					setColumn(item, this.tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_MODULE_COLUMN), module == null ? "" : module);
				} else if (characteristics.isLine()) {
					fillWithLine(characteristics.getSeparatorString(), item);
				}
			}
		}

		@Override
		protected void sortTableItems(final int index) {
			if (this.tableDef.isSortAllowed()) {
				super.sortTableItems(index);
				ChannelListPageComposite.this.channelSet.sort(this.tableDef.isSortAscending());
			}
		}

		protected void updateChannelWithData(final IChannelDefinition def, final MonitorChannelSample data) {
			synchronized(this.table) {
				final ArrayList<TableItem> items = this.tableItems.get(data.getChanId());
				if (items != null) {
					final Iterator<TableItem> it = items.iterator();
					while (it.hasNext()) {
						try {
							final TableItem item = it.next();
							final int index = this.table.indexOf(item);
							updateTable(index, data, item, def);
						} catch (final Exception e) {
							e.printStackTrace();
						}                  
					}
				}
			}
		}
	}

	/**
	 * Listener for updating channel set
	 *
	 */
	private class SetUpdateListener implements ChannelSetUpdateListener
	{

		/**
		 * @{inheritDoc}
		 * @see jpl.gds.monitor.guiapp.gui.views.support.ChannelSetUpdateListener#updateSet(jpl.gds.monitor.perspective.view.channel.ChannelSet)
		 */
		@Override
		public boolean updateSet(final ChannelSet set)
		{
			if (ChannelListPageComposite.this.tableDef.isSortAllowed()) {
				final boolean result = SWTUtilities.showConfirmDialog(ChannelListPageComposite.this.mainShell.getShell(), "Sort Confirmation",
						"Sorting is enabled, which may change your selected channel ordering.\n Do you want to disable sorting?");
				if (result) {
					ChannelListPageComposite.this.tableDef.setSortAllowed(false);
				}

			}
			if(set.size() > ChannelListPageComposite.this.maxChannelSelectionSize)
			{
				SWTUtilities.showErrorDialog(ChannelListPageComposite.this.mainShell.getShell(),"Too Many Channels Selected",
						"Only " + ChannelListPageComposite.this.maxChannelSelectionSize + " channels are allowed to be placed on a single channel list page.");
				return false;
			}

			ChannelListPageComposite.this.channelSet = set;
			ChannelListPageComposite.this.channelTableComposite.rebuildChannelSetComposite();
			flushTimerFired();
			return true;
		}
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.perspective.view.View#updateViewConfig()
	 */
	@Override
	public void updateViewConfig()
	{
		boolean columnWidthError = false;
		this.viewConfig.setChannels(this.channelSet);
		final TableColumn[] tableColumns = this.channelTableComposite.getColumns();
		if (tableColumns != null) {
			for (int i = 0; i < tableColumns.length; i++) {
				if (tableColumns[i] != null) {
					if (tableColumns[i].getWidth() == 0) {
						columnWidthError = true;
						break;  
					}
					this.tableDef.setColumnWidth(i, tableColumns[i].getWidth());
				}
			}
		}
		if (this.channelTableComposite.getTable().getSortColumn() != null) {
			this.tableDef.setSortColumn(this.channelTableComposite.getTable().getSortColumn().getText());
		}

		this.tableDef.setColumnOrder(this.channelTableComposite.getTable().getColumnOrder());

		if (columnWidthError && !WarningUtil.getWidthWarningShown()) {
			SWTUtilities.showWarningDialog(this.mainShell.getShell(), 
					"Save Warning for View " + this.viewConfig.getViewName(),
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
		return this.viewConfig;
	}

	/**
	 * Gets the maximum permitted number of channels that can be added to a channel table
	 * 
	 * @return Returns the maxChannelSelectionSize.
	 */
	@Override
	public long getMaxChannelSelectionSize()
	{
		return this.maxChannelSelectionSize;
	}

	/**
	 * Sets the maxChannelSelectionSize
	 *
	 * @param maxChannelSelectionSize The maxChannelSelectionSize to set.
	 */
	@Override
	public void setMaxChannelSelectionSize(final long maxChannelSelectionSize)
	{
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
		this.lastClearTime = System.currentTimeMillis();
		this.channelTableComposite.clearTable();
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

	/**
	 * Handles suspect channel messages if the table composite is not disposed
	 * 
	 * @param msg suspect channel message that indicates which channel IDs are 
	 * to be marked as suspect
	 */
	public void displayMessage(final IMessage msg) {
		if (this.parent.isDisposed()) {
			return;
		}
		this.parent.getDisplay().asyncExec(new Runnable () {

			/**
			 * {@inheritDoc}
			 * @see java.lang.Object#toString()
			 */
			@Override
			public String toString() {
				return "ChannelListPageComposite.displayMessage.Runnable";
			}

			/**
			 * {@inheritDoc}
			 * @see java.lang.Runnable#run()
			 */
			@Override
			public void run () {
				try {
					if (ChannelListPageComposite.this.channelTableComposite.isDisposed()) {
						return;
					}
					TraceManager.getDefaultTracer().trace("channel list view is processing message"); 


					if (msg.isType(EhaMessageType.SuspectChannels)) {
						ChannelListPageComposite.this.channelTableComposite.handleSuspectChannelsMessage((ISuspectChannelsMessage)msg);
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

		if (this.parent.isDisposed() || this.parent.getDisplay().isDisposed()) {
			return;
		}

		try
		{
			if (this.channelTableComposite.isDisposed())
			{
				return;
			}

			if (!this.channelTableComposite.isVisible()) {
				return;
			}
			final List<String> listenerIds = this.channelTableComposite.getChannelIds();
			/*
			 * Get the list of samples needed for this view from the LAD, supplying
			 * the list of specific channels we are interested in, the realtime/recorded
			 * filter, and the station filter configured in this view.
			 * 
			 * Realtime recorded filter in the perspective and
			 * LAD is now enum rather than boolean, and station ID is required for LAD
			 * access.
			 */
			final List<MonitorChannelSample>  latestData = this.lad.getLatestValues(listenerIds, 
					((RealtimeRecordedSupport)this.viewConfig).getRealtimeRecordedFilterType(), 
					((StationSupport)this.viewConfig).getStationId());
			/*
			 * If there are any samples from the LAD that match the selection criteria...
			 */
			if (latestData != null) {
				/*
				 * Display each, if it has a received timestamp later than the last time we
				 * cleared this view.
				 */
				for (final MonitorChannelSample data: latestData) {
					if (data != null && data.getTimestamp() > this.lastClearTime) {
						receive(data);
					}
				}
			}
			this.channelTableComposite.redraw();
		}

		catch (final Exception e)
		{
			e.printStackTrace();
			return;
		}
	}
}
