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
package jpl.gds.monitor.guiapp.plot.freechart;

import java.util.ArrayList;

import org.eclipse.swt.widgets.Composite;
import org.jfree.data.xy.AbstractXYDataset;

import jpl.gds.monitor.config.MonitorGuiProperties;
import jpl.gds.monitor.guiapp.MonitorTimers;
import jpl.gds.monitor.guiapp.common.ChartUpdateListener;
import jpl.gds.monitor.guiapp.plot.ChartDataPoint;
import jpl.gds.monitor.guiapp.plot.ChartDataSeries;
import jpl.gds.monitor.perspective.view.ChannelChartViewConfiguration;
import jpl.gds.shared.swt.SWTUtilities;

/**
 * Contains info that describes how plot should be rendered
 *
 */
@SuppressWarnings("serial")
public class ChillDataset extends AbstractXYDataset implements
        ChartUpdateListener {

    /**
     * The maximum number of data points that can be plotted on a single trace
     * if point shapes are drawn. Oldest data points will be discarded when this
     * limit is reached
     */
    public final long maxShapeValues;

    /**
     * The maximum number of data points that can be plotted on a single trace
     * if line only is drawn. Oldest data points will be discarded when this
     * limit is reached.
     */
    public final long maxLineValues;

    /**
     * Flag for determining whether plots should actually be drawn on channel
     * chart pages (temporary config item for solving monitor disappearance
     * problem)
     */
    public final boolean renderPlots;

    private final ArrayList<ChartDataSeries> list = new ArrayList<ChartDataSeries>();
    private boolean datasetChanged = false;
    private long timeToLive;
    private ChannelChartViewConfiguration.RenderingStyle renderingStyle;
    private long maxPointsPerTrace;
    private Composite chartComposite;
	private final MonitorTimers timers;
    
    /**
     * Constructor.
     * 
     * @param guiProps current GUI properties
     * @param timers current monitor timers object
     */
    public ChillDataset(MonitorGuiProperties guiProps, MonitorTimers timers) {
    	this.timers = timers;

    	maxShapeValues = guiProps.getPlotMaxShapePoints();
    	maxLineValues =  guiProps.getPlotMaxLinePoints();
    	renderPlots = guiProps.isPlottingEnabled();
    }

    /**
     * Gets the enumeration value that describes how plot should be drawn
     * 
     * @return the type of plot that will be drawn (line, shape, line and shape,
     *         step)
     */
    public ChannelChartViewConfiguration.RenderingStyle getRenderingStyle() {
        return this.renderingStyle;
    }

    /**
     * Sets the chart composite
     * 
     * @param chart
     *            GUI component that will contain the plot
     */
    public void setChartComposite(final Composite chart) {
        this.chartComposite = chart;
    }

    /**
     * Sets the rendering stype enumeration value
     * 
     * @param renderingStyle
     *            type of graph that will be drawn (line, shape, line and shape,
     *            step)
     */
    public void setRenderingStyle(
            final ChannelChartViewConfiguration.RenderingStyle renderingStyle) {
        this.renderingStyle = renderingStyle;
        if (this.renderingStyle == ChannelChartViewConfiguration.RenderingStyle.LINE) {
            this.maxPointsPerTrace = maxLineValues;
        } else {
            this.maxPointsPerTrace = maxShapeValues;
        }
        for (final ChartDataSeries series : this.list) {
            series.setMaxDataPoints(this.maxPointsPerTrace);
        }
    }

    /**
     * Sets a new time to live (expiration time) for plot points in this data
     * series.
     * 
     * @param hours
     *            new expiration time hours
     * @param minutes
     *            expiration time minutes
     * @param seconds
     *            expiration time seconds
     */
    public synchronized void setTimeToLive(final int hours, final int minutes,
            final int seconds) {
        this.timeToLive = (hours * 3600 + minutes * 60 + seconds) * 1000;
        for (final ChartDataSeries series : this.list) {
            series.setTimeToLive(this.timeToLive);
        }
    }

    /**
     * Sets up the list that will contain all the data points and sets its
     * properties (i.e. title, ttl)
     * 
     * @param numSeries
     *            number this data series will occupy in the sequence
     * @param name
     *            name of this data series
     */
    public synchronized void addDataSeries(final int numSeries,
            final String name) {
        final ChartDataSeries series = new ChartDataSeries();
        series.setName(name);
        series.setTimeToLive(this.timeToLive);

        this.list.add(numSeries, series);
        SWTUtilities.runInDisplayThread(new Runnable() {

            @Override
            public String toString() {
                return "ChillDataSet.addDataSeries.Runnable";
            }

            @Override
            public void run() {
                if (renderPlots) {
                    fireDatasetChanged();
                }
            }
        });
    }

    /**
     * Adds a data point to the series specified by the series number parameter
     * 
     * @param series
     *            number of the data series in the sequence
     * @param point
     *            x,y data point
     */
    public synchronized void addDataPoint(final int series,
            final ChartDataPoint point) {
        final ChartDataSeries ser = this.list.get(series);
        ser.addDataPoint(point);
        this.datasetChanged = true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jfree.data.general.AbstractSeriesDataset#getSeriesCount()
     */
    @Override
    public int getSeriesCount() {
        return this.list.size();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jfree.data.general.AbstractSeriesDataset#getSeriesKey(int)
     */
    @Override
    public Comparable<String> getSeriesKey(final int series) {
        final ChartDataSeries ser = this.list.get(series);
        final String name = ser.getName();
        return name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jfree.data.xy.XYDataset#getItemCount(int)
     */
    @Override
    public int getItemCount(final int series) {
        final ChartDataSeries ser = this.list.get(series);
        return ser.numItems();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jfree.data.xy.XYDataset#getX(int, int)
     */
    @Override
    public Number getX(final int series, final int item) {
        final ChartDataSeries ser = this.list.get(series);
        final double value = ser.getXValue(item);
        return Double.valueOf(value);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jfree.data.xy.XYDataset#getY(int, int)
     */
    @Override
    public Number getY(final int series, final int item) {
        final ChartDataSeries ser = this.list.get(series);
        final double value = ser.getYValue(item);
        return Double.valueOf(value);
    }

    /**
     * Removes all data points from each series
     */
    public synchronized void clearSeries() {
        for (final ChartDataSeries ser : this.list) {
            ser.clearDataPoints();
        }
    }

    /**
     * Clears the entire list of series. Updates the drawn plot accordingly.
     */
    public synchronized void clearData() {
        this.list.clear();

        SWTUtilities.runInDisplayThread(new Runnable() {
            @Override
            public String toString() {
                return "ChillDataSet.clearData.Runnable";
            }

            @Override
            public void run() {
                if (renderPlots) {
                    fireDatasetChanged();
                }
            }
        });
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.guiapp.common.ChartUpdateListener#update()
     */
    @Override
    public void update() {

        if (!ChillDataset.this.datasetChanged) {
            return;
        }

        if (ChillDataset.this.chartComposite.isDisposed()) {
            return;
        }
        if (!ChillDataset.this.chartComposite.getVisible()) {
            return;
        }
        ChillDataset.this.datasetChanged = false;

        if (renderPlots) {
            fireDatasetChanged();
        }
    }

    /**
     * Adds a plot update listener for this data series
     */
    public void startUpdates() {
        timers.addPlotListener(this);
    }

    /**
     * Updates the name for this data series
     * 
     * @param numSeries
     *            number this data series occupies in the list
     * @param name
     *            new name for the data series
     */
    public void setName(final int numSeries, final String name) {
        final ChartDataSeries ser = this.list.get(numSeries);
        if (ser != null) {
            ser.setName(name);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable {
        timers.removePlotListener(this);
    }

}
