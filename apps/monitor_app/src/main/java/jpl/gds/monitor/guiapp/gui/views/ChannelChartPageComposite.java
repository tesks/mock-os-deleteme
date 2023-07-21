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

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYStepRenderer;
import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.channel.ChannelDefinitionType;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.dictionary.api.client.channel.IChannelUtilityDictionaryManager;
import jpl.gds.monitor.config.MonitorGuiProperties;
import jpl.gds.monitor.guiapp.MonitorTimers;
import jpl.gds.monitor.guiapp.channel.ChannelMessageDistributor;
import jpl.gds.monitor.guiapp.channel.ChannelSampleListener;
import jpl.gds.monitor.guiapp.gui.views.preferences.ChannelChartPreferencesShell;
import jpl.gds.monitor.guiapp.gui.views.support.ChannelSetConsumer;
import jpl.gds.monitor.guiapp.gui.views.support.ChannelSetEditorShell;
import jpl.gds.monitor.guiapp.gui.views.support.ChannelSetUpdateListener;
import jpl.gds.monitor.guiapp.plot.ChartDataPoint;
import jpl.gds.monitor.guiapp.plot.freechart.ChartComposite;
import jpl.gds.monitor.guiapp.plot.freechart.ChillChart;
import jpl.gds.monitor.guiapp.plot.freechart.ChillDataset;
import jpl.gds.monitor.perspective.view.ChannelChartViewConfiguration;
import jpl.gds.monitor.perspective.view.ChannelChartViewConfiguration.RetentionTimeType;
import jpl.gds.monitor.perspective.view.ChannelChartViewConfiguration.XAxisChoice;
import jpl.gds.monitor.perspective.view.RealtimeRecordedFilterType;
import jpl.gds.monitor.perspective.view.channel.ChannelSet;
import jpl.gds.monitor.perspective.view.channel.MonitorChannelLad;
import jpl.gds.monitor.perspective.view.channel.MonitorChannelSample;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.perspective.view.View;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.sys.SystemUtilities;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;



/**
 * This class manages a composite that plots channel values for a 
 * list of channels.  Channels can be selected using the 
 * ChannelSetEditorShell class or else specified directly in the 
 * configuration using a ChannelSet file.  The class also allows the
 * editing of the channel plot title and otherwise can be saved as part of
 * an overall display configuration.
 *
 */
public class ChannelChartPageComposite implements View, ChannelSetConsumer
{
	/**
	 * Channel plot composite title
	 */
	public static final String TITLE = "Channel Plot";

	private static int chartId = 1;

	private ChannelChartViewConfiguration.XAxisChoice xAxisChoice =  ChannelChartViewConfiguration.XAxisChoice.ERT; // Domain
	private ChannelChartViewConfiguration.YAxisChoice yAxisChoice =  ChannelChartViewConfiguration.YAxisChoice.Raw;  // Range

	/**
	 * Maximum numbers of channels that may be added to a plot
	 */
	protected long maxChannelSelectionSize;

	private Composite parent;
	private Composite mainShell;
	private final ChannelChartViewConfiguration viewConfig;
	private ChillDataset dataset = null;
	private final ChannelSet domainChannelSet;
	private final ChannelSet rangeChannelSet;
	private final Map<String,Integer> channelMap = new HashMap<String,Integer>();
	private ChillChart theChart = null;
	private final List<ChannelListener> listeners = new ArrayList<ChannelListener>();
	private ChannelChartPreferencesShell prefShell;
	private boolean autoAdjustRange;
	private BrainDeadChartComposite chartComp;

	////// Testing
	private TimeData<Double, Long> lastValue;
	private final Map<Integer, TimeData<ChartDataPoint, long[]>> lastValues = new HashMap<Integer, TimeData<ChartDataPoint, long[]>>();

	private final ApplicationContext appContext;
	////

	/**
	 * Constructor
	 * 
	 * Create a ChannelListPageShell with the given test configuration, display configuration
	 * and parent shell.
	 * @param appContext the current application context
	 * 
	 * @param config		Parent display configuration
	 */
	public ChannelChartPageComposite(final ApplicationContext appContext, final IViewConfiguration config)
	{
		this.appContext = appContext;
		viewConfig = (ChannelChartViewConfiguration)config;
		domainChannelSet = viewConfig.getDomainChannels(appContext.getBean(IChannelDefinitionProvider.class));
		rangeChannelSet = viewConfig.getRangeChannels(appContext.getBean(IChannelDefinitionProvider.class));
		maxChannelSelectionSize = appContext.getBean(MonitorGuiProperties.class).getPlotMaxChannels();
		xAxisChoice = viewConfig.getDomainValueSelector();
		yAxisChoice = viewConfig.getRangeValueSelector();
		autoAdjustRange = viewConfig.isAutoAdjustRange();
	}


	/**
	 * {@inheritDoc}
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

		// Added station registration when
		// the view is first drawn
		appContext.getBean(MonitorChannelLad.class).registerStation(this, viewConfig.getStationId());
	}

	private void createGui()
	{
		mainShell   = new Composite(parent, SWT.NONE);  
		final FormLayout layout = new FormLayout();
		mainShell.setLayout(layout);

		dataset = new ChillDataset(appContext.getBean(MonitorGuiProperties.class), appContext.getBean(MonitorTimers.class));

		theChart = new ChillChart();

		switch (viewConfig.getDomainValueSelector()) {
		case ERT:
		case SCET:
		case LST:	
			theChart.createTimeSeriesChart(dataset, viewConfig);
			break;
		case SCLK:
		case CHANNEL:
			theChart.createXYLineChart(dataset, viewConfig);
			break;
		default:
			throw new IllegalStateException("Unrecognized X Axis Value Type");
		}

		// Set the default Domain Axis Label
		if (theChart.getDomainLabel() == null) {
			theChart.setDomainLabel( xAxisChoice.name());
		}
		dataset.setChartComposite(mainShell);

		updateChartRenderers();
		final Composite chartBound = new Composite(mainShell, SWT.NONE);

		final FormLayout layout3 = new FormLayout();
		chartBound.setLayout(layout3);

		FormData data = new FormData();
		data.top = new FormAttachment(0);
		data.bottom = new FormAttachment(100);
		data.left = new FormAttachment(0);
		data.right = new FormAttachment(100);
		chartBound.setLayoutData(data);

		chartComp = new BrainDeadChartComposite( chartBound, SWT.NONE, null);

		// by using setChart, the chartComposite is made to listen to chart changes
		chartComp.setChart( theChart.getChart() );

		data = new FormData();
		data.top = new FormAttachment(0);
		data.bottom = new FormAttachment(100);
		data.left = new FormAttachment(0);
		data.right = new FormAttachment(100);
		chartComp.setLayoutData(data);

		mainShell.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent event) {
				try {
					if (listeners != null) {
						// First remove old channel listeners and dataset series
						for (int index = 0; index < listeners.size(); index++) {
							final ChannelMessageDistributor dist = appContext.getBean(ChannelMessageDistributor.class);
							final ChannelListener listener = listeners.get(index);

							final String channel = listener.getId();
							dist.removePlotListener(channel, listener);
						}
					}

					// Added station
					// unregistration when the view is disposed
					appContext.getBean(MonitorChannelLad.class).unregisterStation(
							ChannelChartPageComposite.this);

				} catch (final Exception ex) {
					ex.printStackTrace();
					TraceManager.getDefaultTracer().error("Error disposiing of channel plot main shell " + ex.toString());

				}
			}
		});

		resetChartSources();
		dataset.startUpdates();
	}


	private void updateChartRenderers()
	{
		final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		final boolean shapesVisible = viewConfig.getRenderingStyle() == ChannelChartViewConfiguration.RenderingStyle.SHAPE ||
				viewConfig.getRenderingStyle() == ChannelChartViewConfiguration.RenderingStyle.SHAPE_AND_LINE;
		final boolean lineVisible = viewConfig.getRenderingStyle() == ChannelChartViewConfiguration.RenderingStyle.LINE ||
				viewConfig.getRenderingStyle() == ChannelChartViewConfiguration.RenderingStyle.SHAPE_AND_LINE;
		dataset.setRenderingStyle(viewConfig.getRenderingStyle());

		for (int index = 0; index < maxChannelSelectionSize; index++) {

			renderer.setSeriesShapesVisible(index, shapesVisible);
			renderer.setSeriesLinesVisible(index, lineVisible);
			renderer.setSeriesShapesFilled(index, false);

			switch (index) {
			case 0:
				renderer.setSeriesPaint(index, Color.blue);
				break;
			case 1:
				renderer.setSeriesPaint(index, Color.red);
				break;
			case 2:
				renderer.setSeriesPaint(index, Color.green);
				break;
			case 3:
				renderer.setSeriesPaint(index, Color.gray);
				break;
			case 4:
				renderer.setSeriesPaint(index, Color.black);
				break;
			case 5:
				renderer.setSeriesPaint(index, Color.magenta);
				break;
			case 6:
				renderer.setSeriesPaint(index, Color.orange);
				break;
			default:
				renderer.setSeriesPaint(index, Color.black);
				break;

			}
		}

		if(!shapesVisible && !lineVisible)
		{
			final XYItemRenderer stepRenderer = new XYStepRenderer();
			//setSeriesPaint and other stuff done above for line/shape
			for (int index = 0; index < maxChannelSelectionSize; index++) {

				switch (index) {
				case 0:
					stepRenderer.setSeriesPaint(index, Color.blue);
					break;
				case 1:
					stepRenderer.setSeriesPaint(index, Color.red);
					break;
				case 2:
					stepRenderer.setSeriesPaint(index, Color.green);
					break;
				case 3:
					stepRenderer.setSeriesPaint(index, Color.gray);
					break;
				case 4:
					stepRenderer.setSeriesPaint(index, Color.black);
					break;
				case 5:
					stepRenderer.setSeriesPaint(index, Color.magenta);
					break;
				case 6:
					stepRenderer.setSeriesPaint(index, Color.orange);
					break;
				default:
					stepRenderer.setSeriesPaint(index, Color.black);
					break;

				}
			}
			//set step plot
			theChart.setRenderer(stepRenderer);
			return;
		}

		//set line, shape or line_and_shape plot
		theChart.setRenderer(renderer);
	}


	private void resetChartSources() {
		if (rangeChannelSet == null) {
			System.out.println("Null channel set in free chart returning");
			return;
		} 

		// First remove old channel listeners and dataset series
		for (int index = 0; index < listeners.size(); index++) {
			final ChannelMessageDistributor dist = appContext.getBean(ChannelMessageDistributor.class);
			final ChannelListener listener = listeners.get(index);

			final String channel = listener.getId().toString();
			dist.removePlotListener(channel, listener);
		}
		dataset.clearData();
		listeners.clear();
		channelMap.clear();

		final String[] domainIds = domainChannelSet.getIds();

		if (domainIds.length > 0){

			final ChannelListener listener = new ChannelListener(){

				@Override
				public void receive(final MonitorChannelSample data) {

					if (xAxisChoice == XAxisChoice.CHANNEL){

						// do not plot from the LAD
						if (data.isFromLad()) {
							return;
						}

						/*
						 * RT/Recorded flag is now an enum
						 * rather than a boolean. Updated this logic appropriately.
						 * If the data is realtime but we only want recorded, or vice
						 * versa, discard the data.
						 */
						if (data.isRealtime() && viewConfig.getRealtimeRecordedFilterType() == RealtimeRecordedFilterType.RECORDED) {
							return;
						} else if (!data.isRealtime() && viewConfig.getRealtimeRecordedFilterType() == RealtimeRecordedFilterType.REALTIME) {
							return;
						}

						/*
						 * Filter out monitor channels that do not have the station
						 * configured in the preferences.
						 */
						if (data.getChanDef().getDefinitionType() == ChannelDefinitionType.M && viewConfig.getStationId() != data.getDssId()) {
							return;
						}

						String value;

						final IChannelDefinition def = appContext.getBean(IChannelDefinitionProvider.class).getDefinitionFromChannelId(data.getChanId());
						if (def == null) {
							return;
						}
						boolean hasEu = false;

						hasEu = def.hasEu();

						if ( yAxisChoice == ChannelChartViewConfiguration.YAxisChoice.Value && hasEu) {
							value = data.getEuValue().getStringValue();
						} else {
							value = data.getDnValue().getStringValue();
						}

						if (value != null){
							try {
								lastValue = new TimeData<Double, Long>(Double.valueOf(value), data.getErt().getTime());
							} catch (final NumberFormatException e) {
								TraceManager.getDefaultTracer().warn("Problem converting plot data for channel " + data.getChanId() + " to floating point: " + e.getMessage() + ". Perhaps the channel is not numeric?");

								return;
							}
							final Set<Entry<Integer,TimeData<ChartDataPoint, long[]>>> series = lastValues.entrySet();

							final HashMap<Integer, TimeData<ChartDataPoint, long[]>> newPoints = new HashMap<Integer, TimeData<ChartDataPoint, long[]>>();

							for(final Entry<Integer,TimeData<ChartDataPoint, long[]>> entry : series){

								final ChartDataPoint point = entry.getValue().getData();
								// time[timestamp of the x-axis data used, timestamp of the y-axis data used]

								// Timestamps are used to determine if a point should be created or if the value of an existing point should be changed.
								// These cases occur because the X-Axis channel and Y-Axis channels come in at different times, rates, or the same time.

								// For example if both a X-Axis value and Y-Axis value come in at the same time, but the Y-Axis comes first.  It does
								// not know that it should use the new X-Axis value, since it has not been processed yet.  Instead it will create the point
								// with the currently known last X-Axis value.

								// Once the X-Axis is processed it will look at the timestamps of the last known datapoints per channel where it's data was 
								// referenced and change the Point's value if old data was used.
								final long[] time = entry.getValue().getTime();

								// if the timestamp on the y data is newer, update the x data value and timestamp.
								if (time.length >= 2 && lastValue.getTime() <= time[1]){
									point.setXValue(lastValue.getData());
									time[0] = lastValue.getTime();
								}
								// otherwise create a new datapoint using the existing y data value.
								else{
									final ChartDataPoint newPoint = new ChartDataPoint();
									newPoint.setXValue(lastValue.getData());
									newPoint.setYValue(point.getYValue());

									final long[] timestamp = new long[]{lastValue.getTime(), time[1]};

									newPoints.put(entry.getKey(), new TimeData<ChartDataPoint,long[]>(newPoint, timestamp));

									dataset.addDataPoint(entry.getKey(), newPoint);
								}
							}

							final Set<Entry<Integer,TimeData<ChartDataPoint, long[]>>> insertPoints = newPoints.entrySet();

							// add the new points back to the lastvalues hashmap
							for (final Entry<Integer,TimeData<ChartDataPoint, long[]>> entry: insertPoints){
								lastValues.put(entry.getKey(), new TimeData<ChartDataPoint,long[]>(entry.getValue().getData(), entry.getValue().getTime()));
							}
						}
					}
				}
			};

			listeners.add(listener);
			listener.setId(domainIds[0]);

			final ChannelMessageDistributor dist = appContext.getBean(ChannelMessageDistributor.class);

			dist.addPlotListener(domainIds[0], listener);

		}

		// For each channel in set, add to dataset
		final String[] ids = rangeChannelSet.getIds();
		int seriesIndex = 0;
		for (int index = 0; index < ids.length; index++) {

			final ChannelListener listener = new ChannelListener();
			final IChannelDefinition def = appContext.getBean(IChannelDefinitionProvider.class).getDefinitionFromChannelId(ids[index]);
			if (def != null) {
				final String name = def.getTitle();
				listener.setChannelName(name);
			}else {
				continue;
			}
			listeners.add(listener);
			listener.setId(ids[index]);
			final String channel = ids[index];

			final ChannelMessageDistributor dist = appContext.getBean(ChannelMessageDistributor.class);

			dist.addPlotListener(ids[index], listener);
			if (viewConfig.isUseChannelNameForLegend() && def != null && def.getTitle() != null) {
				dataset.addDataSeries(seriesIndex, def.getTitle());
			} else {
				dataset.addDataSeries(seriesIndex, ids[index]);
			}

			try {
				channelMap.put(channel, seriesIndex++);

			} catch (final Exception e) {
				e.printStackTrace();
			} 
		}

		dataset.setRenderingStyle(viewConfig.getRenderingStyle());
		dataset.setTimeToLive(viewConfig.getDataRetentionHours(), viewConfig.getDataRetentionMinutes(), viewConfig.getDataRetentionSeconds());

		chartComp.layout();
		this.mainShell.layout();
	}

	/**
	 * Uses the ChannelSetEditorShell to select channels for the passed in channelSet object.
	 * @param channelSet ChannelSet object to assigned selected values to.
	 */
	private void selectChannels(final ChannelSet channelSet) {
		final ChannelSetEditorShell chanSetEdit = new ChannelSetEditorShell(appContext.getBean(IChannelUtilityDictionaryManager.class), 
		        appContext.getBean(SprintfFormat.class), this);
		chanSetEdit.popupShell(mainShell.getShell(), new SetUpdateListener(channelSet), channelSet, false);
	}

	/**
	 * Generic inner class used to store data and the timestamp of the data.
	 *
	 * @param <Data> Some object representing the data.
	 * @param <Time> Some object representing the time
	 */
	private static class TimeData<Data, Time>{

		private final Time time;
		private final Data data;

		/**
		 * Constructor which sets the data and time.
		 * @param data Data to hold within the object.
		 * @param time Time to hold within the object.
		 */
		public TimeData(final Data data, final Time time){
			this.data = data;
			this.time = time;
		}

		/**
		 * Returns the Time held within the object.
		 * @return Time value.
		 */
		public Time getTime(){
			return time;
		}

		/**
		 * Returns the Data held within the object.
		 * @return Data value.
		 */
		public Data getData(){
			return data;
		}
	}

	/**
	 * ChannelListener waits for updates to channel values for a specific channel.
	 *
	 */
	private class ChannelListener implements ChannelSampleListener {
		private String id = null;
		private String channelName = null;

		@Override
		public void receive(final MonitorChannelSample data)
		{
			// do not plot from the LAD
			if (data.isFromLad()) {
				return;
			}

			/*
			 * RT/Recorded flag is now an enum
			 * rather than a boolean. Updated this logic appropriately.
			 * If the data is realtime but we only want recorded, or vice
			 * versa, discard the data.
			 */
			if (data.isRealtime() && viewConfig.getRealtimeRecordedFilterType() == RealtimeRecordedFilterType.RECORDED) {
				return;
			} else if (!data.isRealtime() && viewConfig.getRealtimeRecordedFilterType() == RealtimeRecordedFilterType.REALTIME) {
				return;
			}

			/*
			 * Filter out monitor channels that do not have the station
			 * configured in the preferences.
			 */
			if (data.getChanDef().getDefinitionType() == ChannelDefinitionType.M && viewConfig.getStationId() != data.getDssId()) {
				return;
			}

			String value = "";
			final String channel = data.getChanId();
			final IChannelDefinition def = appContext.getBean(IChannelDefinitionProvider.class).getDefinitionFromChannelId(data.getChanId());
			if (def == null) {
				return;
			}
			final Integer channelIndex = channelMap.get(channel);
			boolean hasEu = false;

			hasEu = def.hasEu();

			if (channelIndex != null) {
				ChartDataPoint point = new ChartDataPoint();
				if ( yAxisChoice == ChannelChartViewConfiguration.YAxisChoice.Value && hasEu) {
					value = data.getEuValue().getStringValue();
				} else {
					value = data.getDnValue().getStringValue();
				}

				try {
					final Double val = Double.valueOf((value != null) ? value : "");
					point.setYValue(val.doubleValue());
                    IAccurateDateTime date = data.getErt();

					// Here we make a distinction about displaying the x-axis.
					switch ( xAxisChoice )
					{
					case ERT :
						date = data.getErt();
						point.setXValue(date.getTime());
						break;
					case SCET :
						date = data.getScet();
						point.setXValue(date.getTime());
						break;
					case LST :
						date = data.getSol();
						point.setXValue(date.getTime());
						break;
					case SCLK :
						final ISclk sclk = data.getSclk();
						point.setXValue(sclk.getFloatingPointTime());
						break;
					case CHANNEL :
						if (lastValue != null){
							final TimeData<ChartDataPoint, long[]> currentEntry = lastValues.get(channelIndex);

							// look to see if updating data is required.
							if (currentEntry != null && data.getErt().getTime() <= currentEntry.getTime()[0]){
								// timestamp[x,y]
								final long[] timestamp = currentEntry.getTime();
								final ChartDataPoint currentPoint = currentEntry.getData();

								currentPoint.setYValue(point.getYValue());
								timestamp[1] = data.getErt().getTime();

								point = null;
							}
							else{
								point.setXValue(lastValue.getData());
								final long[] newTimestamps = new long[]{lastValue.getTime(), data.getErt().getTime()};

								lastValues.put(channelIndex, new TimeData<ChartDataPoint,long[]>(point, newTimestamps));
							}
						}
						else{
							point = null;
						}
						break;
					default :
						date = data.getErt();
						point.setXValue(date.getTime());
						break;
					} // end select

					if (point != null) {

						if (viewConfig.getDataRetentionTimeType() == RetentionTimeType.ReceiveTime) {
							point.setPostTime(System.currentTimeMillis());
						} else if (viewConfig.getDataRetentionTimeType() == RetentionTimeType.ERT) {
							point.setPostTime(data.getErt().getTime());
						}

						dataset.addDataPoint(channelIndex, point);
					}

				} catch (final Exception e) {
					TraceManager.getDefaultTracer().warn("Problem converting plot data for channel " + channel + " to floating point: " + e.getMessage() + ". Perhaps the channel is not numeric?");

					//e.printStackTrace();
				}
			}
		}


		/**
		 * Gets the channel ID that is being listened to
		 * 
		 * @return Returns the channel id.
		 */
		public String getId()
		{
			return id;
		}        

		/**
		 * Set the channel ID
		 * 
		 * @param id The ChannelId to set.
		 */
		public void setId(final String id)
		{
			this.id = id;
		}

		/**
		 * Retrieves the channelName.
		 * @return the channelName
		 */
		public String getChannelName() {
			return channelName;
		}

		/**
		 * Sets the channelName.
		 * @param channelName the channelName to set
		 */
		public void setChannelName(final String channelName) {
			this.channelName = channelName;
		}
	}

	/**
	 * Updates channels in the chart when user selects them through the 
	 * channel selector
	 *
	 */
	private class SetUpdateListener implements ChannelSetUpdateListener {

		private final ChannelSet channelSet;

		public SetUpdateListener(final ChannelSet set){
			channelSet = set;
		}

		@Override
		public boolean updateSet(final ChannelSet set)
		{
			if(set.size() > getMaxChannelSelectionSize())
			{
				SWTUtilities.showErrorDialog(mainShell.getShell(),"Too Many Channels Selected",
						"Only " + getMaxChannelSelectionSize() + " channels are allowed to be placed on a single channel plot.");
				return false;
			}

			channelSet.clearChannels();
			channelSet.loadFromString(appContext.getBean(IChannelDefinitionProvider.class), set.toString());

			for (final String id: set.getIds()) {
				final IChannelDefinition def = appContext.getBean(IChannelDefinitionProvider.class).getDefinitionFromChannelId(id);
				if (def.getChannelType().equals(ChannelType.ASCII)) {
					SWTUtilities.showErrorDialog(mainShell.getShell(), "Bad Range Channel", "You cannot use channel " + id + " on the range (Y) axis because it has a string value.");
					channelSet.removeChannel(id);
				} else if (yAxisChoice.equals(ChannelChartViewConfiguration.YAxisChoice.Value) && (def.getChannelType().equals(ChannelType.STATUS) || def.getChannelType().equals(ChannelType.BOOLEAN))) {
					SWTUtilities.showErrorDialog(mainShell.getShell(), "Bad Range Channel", "You cannot use channel " + id + "'s value for the range (Y) axis because it has a string value.");
					channelSet.removeChannel(id);
				}
			}
			resetChartSources();
			return true;
		}

	} // end ChannelSetUpdateListener

	/**
	 * Sets the unique identifier for this chart view.
	 * @param id the unique ID to set
	 */
	public void setChartId(final String id) {
		dataset.startUpdates();
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.swt.ChillShell#saveConfiguration()
	 */
	@Override
	public void updateViewConfig()
	{
		viewConfig.setDomainChannels(domainChannelSet);
		viewConfig.setRangeChannels(rangeChannelSet);
		theChart.getChartConfiguration(viewConfig);
		viewConfig.setAutoAdjustRange(autoAdjustRange);
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
	 * @{inheritDoc}
	 * @see jpl.gds.perspective.view.View#getDefaultName()
	 */
	@Override
	public String getDefaultName() {
		return TITLE;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.monitor.guiapp.gui.views.support.ChannelSetConsumer#getMaxChannelSelectionSize()
	 */
	@Override
	public long getMaxChannelSelectionSize() {
		return maxChannelSelectionSize;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.monitor.guiapp.gui.views.support.ChannelSetConsumer#setMaxChannelSelectionSize(long)
	 */
	@Override
	public void setMaxChannelSelectionSize(final long maxChannelSelectionSize) {
		// TODO - make num channels configurable, do nothing at the moment	
	} 

	/**
	 * Generates a Unique Id for this chart
	 * @return a unique ID for this chart
	 */
	public static String getNextChartId() {
		return "MonitorChannelPlot " + chartId++;
	}

	/**
	 * 
	 * BrainDeadChartComposite is an extension of the SWT experimental JFreeChart
	 * composite to work around its bugs. It disables the print option, which 
	 * hangs, and the auto-range options, which throw a null pointer exception.
	 * It also adds a few menu items of our own.
	 *
	 */
	protected class BrainDeadChartComposite extends ChartComposite {

		BrainDeadChartComposite(final Composite parent, final int style, final JFreeChart chart) {
			super(parent, style, chart, true, true, false, true, true);
		}
		/**
		 * Displays a dialog that allows the user to edit the properties for the
		 * current chart.
		 */
		@Override
		protected void attemptEditChartProperties() {
			final boolean beforeAutoAdjustRange = viewConfig.isAutoAdjustRange();
			super.attemptEditChartProperties();
			theChart.getChartConfiguration(viewConfig);
			final boolean afterAutoAdjustRange = viewConfig.isAutoAdjustRange();
			if (beforeAutoAdjustRange != afterAutoAdjustRange) {
				autoAdjustRange = afterAutoAdjustRange;
			}
			forceRedraw();     	           
		}

		/**
		 * Creates a popup menu for the canvas.
		 *
		 * @param properties  include a menu item for the chart property editor.
		 * @param save  include a menu item for saving the chart.
		 * @param print  include a menu item for printing the chart.
		 * @param zoom  include menu items for zooming.
		 *
		 * @return The popup menu.
		 */
		@Override
		protected Menu createPopupMenu(final boolean properties, final boolean save, 
				final boolean print,      final boolean zoom) {

			final Menu jfreeMenu = super.createPopupMenu(properties, true, print, zoom);

			new MenuItem(jfreeMenu, SWT.SEPARATOR);
			final MenuItem prefMenuItem = new MenuItem(jfreeMenu, SWT.PUSH);
			prefMenuItem.setText("Preferences...");
			final MenuItem editRangeChannelMenuItem = new MenuItem(jfreeMenu, SWT.PUSH);
			editRangeChannelMenuItem.setText("Select Range (Y Axis) Channels...");
			new MenuItem(jfreeMenu, SWT.SEPARATOR);
			final MenuItem clearMenuItem = new MenuItem(jfreeMenu, SWT.PUSH);
			clearMenuItem.setText("Clear Data");

			clearMenuItem.addSelectionListener(new SelectionListener() {

				@Override
				public void widgetDefaultSelected(final SelectionEvent e)
				{
					SystemUtilities.doNothing();
				}

				@Override
				public void widgetSelected(final SelectionEvent e)
				{
					try {
						clearView();
					} catch (final Exception ex) {
						ex.printStackTrace();
						TraceManager.getDefaultTracer().error("Error handling Clear Plot menu item " + ex.toString());

					}
				}
			});

			editRangeChannelMenuItem.addSelectionListener(new SelectionListener() {

				@Override
				public void widgetSelected(final SelectionEvent arg0) {
					selectChannels(rangeChannelSet);
				}

				@Override
				public void widgetDefaultSelected(final SelectionEvent arg0) {
					SystemUtilities.doNothing();
				}
			});


			prefMenuItem.addSelectionListener(new SelectionListener() {

				@Override
				public void widgetDefaultSelected(final SelectionEvent e)
				{
					SystemUtilities.doNothing();
				}

				@Override
				public void widgetSelected(final SelectionEvent e)
				{
					try {
						prefShell = new ChannelChartPreferencesShell(appContext, mainShell.getShell());
						theChart.getChartConfiguration(viewConfig);
						final boolean beforeAutoAdjustRange = viewConfig.isAutoAdjustRange();
						final ChannelChartViewConfiguration.XAxisChoice beforeDomainSelector = viewConfig.getDomainValueSelector();
						final ChannelChartViewConfiguration.YAxisChoice beforeRangeSelector = viewConfig.getRangeValueSelector();

						prefShell.setValuesFromViewConfiguration(viewConfig);
						prefShell.getShell().addDisposeListener(new DisposeListener() {
							@Override
							public void widgetDisposed(final DisposeEvent event) {
								try {
									if (!prefShell.wasCanceled()) {
										prefShell.getValuesIntoViewConfiguration(viewConfig);

										// Added station unregistration and registration when preferences change. Only need to call registration because registering with the identical object will replace the old staiton value.
										appContext.getBean(MonitorChannelLad.class)
										.registerStation(
												ChannelChartPageComposite.this,
												ChannelChartPageComposite.this.viewConfig
												.getStationId());

										if (listeners != null) {
											int numSeries = 0;

											// ignores a possible domain channel being in the list of listeners
											for (int i = 0; i < rangeChannelSet.size() && i < listeners.size(); i++){
												final ChannelListener l = listeners.get(i);
												dataset.setName(numSeries++, viewConfig.isUseChannelNameForLegend() ? l.getChannelName() : l.getId());
											}
										}
										xAxisChoice = viewConfig.getDomainValueSelector();
										yAxisChoice = viewConfig.getRangeValueSelector();

										theChart.setChartConfiguration(viewConfig);
										final boolean afterAutoAdjustRange = viewConfig.isAutoAdjustRange();
										if (beforeAutoAdjustRange != afterAutoAdjustRange) {
											autoAdjustRange = afterAutoAdjustRange;
										}
										updateChartRenderers();
										dataset.setRenderingStyle(viewConfig.getRenderingStyle());
										dataset.setTimeToLive(viewConfig.getDataRetentionHours(), viewConfig.getDataRetentionMinutes(), viewConfig.getDataRetentionSeconds());
										final ChannelChartViewConfiguration.XAxisChoice afterDomainSelector = viewConfig.getDomainValueSelector();
										final ChannelChartViewConfiguration.YAxisChoice afterRangeSelector = viewConfig.getRangeValueSelector();

                                        /**
                                         * Removed check for
                                         * domain channel set size since it gets checked in
                                         * ChannelChartPreferencesShell.
                                         */

										theChart.setDomainLabel(viewConfig.getDomainLabel());

										if (!beforeDomainSelector.equals(afterDomainSelector)) {
											switch(afterDomainSelector) {
											case ERT:
											case SCET:
											case LST:
												if (beforeDomainSelector == ChannelChartViewConfiguration.XAxisChoice.SCLK) { 
													clearView();
													theChart.createTimeSeriesChart(dataset, viewConfig);
												}
												break;
											case SCLK:
											case CHANNEL:
												clearView();
												theChart.createXYLineChart(dataset, viewConfig);
												break;
											default:
												break;
											}

											chartComp.setChart( theChart.getChart() );
										}
										resetChartSources();

										if (!beforeRangeSelector.equals(afterRangeSelector)) {
											clearView();
										}

										forceRedraw();
									}
								} catch (final Exception ex) {
									ex.printStackTrace();
									TraceManager.getDefaultTracer().error("Error handling exit from Preferences window " + ex.toString());

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
						TraceManager.getDefaultTracer().error("Error handling Preferences menu item " + ex.toString());

					}
				}
			});
			return jfreeMenu;
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.perspective.view.View#clearView()
	 */
	@Override
	public void clearView() {
		dataset.clearSeries();
		theChart.fireChartChanged();   
	}

}
