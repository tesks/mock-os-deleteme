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
/* ===========================================================
 * JFreeChart : a free chart library for the Java(tm) platform
 * ===========================================================
 *
 * (C) Copyright 2000-2006, by Object Refinery Limited and Contributors.
 *
 * Project Info:  http://www.jfree.org/jfreechart/index.html
 *
 * This library is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by 
 * the Free Software Foundation; either version 2.1 of the License, or 
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, 
 * USA.  
 *
 * [Java is a trademark or registered trademark of Sun Microsystems, Inc. 
 * in the United States and other countries.]
 *
 * -------------------------
 * SWTMultipleAxisDemo1.java
 * -------------------------
 * (C) Copyright 2006, by Object Refinery Limited and Contributors.
 *
 * Contributor(s):   Henry Proudhon;
 *
 * Changes
 * -------
 * 
 */

package jpl.gds.monitor.guiapp.plot.freechart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleInsets;

import jpl.gds.monitor.perspective.view.ChannelChartViewConfiguration;
import jpl.gds.shared.swt.ChillColorCreator;
import jpl.gds.shared.swt.ChillFontCreator;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.swt.types.ChillFont;
import jpl.gds.shared.time.TimeUtility;

/**
 * ChillChart contains static methods for creating JFreeChart plots.
 */
public class ChillChart {

    /**
     * List of permissible charts
     */
    public enum ChartType {
        /**
         * Chart type based on a series of times
         */
        TIME_SERIES, 
        /**
         * Chart type based on Spacecraft CLocK
         */
        SCLK;
    }

    private JFreeChart chart;
    private ChartType type = null;

    /**
     * Gets the current chill chart
     * 
     * @return JFreeChart object
     */
    public JFreeChart getChart() {
        return this.chart;
    }

    /**
     * Sends a default ChartChangeEvent to all registered listeners.
     */
    public void fireChartChanged() {
        if (this.chart != null) {
            this.chart.fireChartChanged();
        }
    }

    /**
     * Sets the type of renderer (i.e. step, line, shape, etc.)
     * 
     * @param renderer
     *            defines the way the chart will be drawn
     */
    public void setRenderer(final XYItemRenderer renderer) {
        if (this.chart != null) {
            this.chart.getXYPlot().setRenderer(renderer);
        }
    }

    /**
     * Gets the label placed on the domain side of the chart
     * 
     * @return the label to be used on the x-axis
     */
    public String getDomainLabel() {
        if (this.chart != null) {
            return this.chart.getXYPlot().getDomainAxis().getLabel();
        }
        return null;
    }

    /**
     * Sets the label that will be placed on the domain side of the chart
     * 
     * @param label
     *            is used on the x-axis
     */
    public void setDomainLabel(final String label) {
        if (this.chart != null) {
            this.chart.getXYPlot().getDomainAxis().setLabel(label);
        }
    }

    /**
     * Creates a TimeSeries chart using default settings.
     * 
     * @param dataset1
     *            data that will be plotted in the chart
     * @param chartTitle
     *            optional title that is placed above the chart
     * @param chartDomainValueTitle
     *            label placed along the X axis
     */
    public void createTimeSeriesChart(final XYDataset dataset1,
            final String chartTitle, final String chartDomainValueTitle) {

        this.type = ChartType.TIME_SERIES;

        this.chart = ChartFactory.createTimeSeriesChart(chartTitle == null ? ""
                : chartTitle, chartDomainValueTitle == null ? "ERT"
                : chartDomainValueTitle, "", dataset1, true, true, false);
        this.chart.setBackgroundPaint(Color.white);
        this.chart.setBorderVisible(true);
        this.chart.setBorderPaint(Color.BLACK);
        this.chart.setAntiAlias(false);
        final XYPlot plot = this.chart.getXYPlot();
        plot.setOrientation(PlotOrientation.VERTICAL);
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.lightGray);
        plot.setRangeGridlinePaint(Color.lightGray);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.getRangeAxis().setFixedDimension(15.0);
        ((NumberAxis) plot.getRangeAxis()).setAutoRangeStickyZero(false);
        plot.setRangeGridlinesVisible(true);
        plot.setDomainGridlinesVisible(true);
        plot.setNoDataMessage("No Data");
        plot.setOutlineVisible(false);
        plot.getRangeAxis().setLowerMargin(0.02);
        plot.getDomainAxis().setLowerMargin(0.02);
        plot.getRangeAxis().setUpperMargin(0.02);
        plot.getDomainAxis().setUpperMargin(0.02);
        final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesLinesVisible(0, true);
        renderer.setSeriesShapesVisible(0, false);
        plot.setRenderer(renderer);
        ValueAxis axis = plot.getDomainAxis();
        axis.setVerticalTickLabels(true);
        ((DateAxis) axis).setTimeZone(TimeZone.getTimeZone("GMT"));
        ((DateAxis) axis).setDateFormatOverride(TimeUtility.getIsoFormatter());
        Font font = axis.getTickLabelFont();
        axis.setTickLabelFont(font.deriveFont(6));
        axis = plot.getRangeAxis();
        font = axis.getTickLabelFont();
        axis.setTickLabelFont(font.deriveFont(6));
    }

    /**
     * Creates an XY line chart using default settings.
     * 
     * @param dataset1
     *            data that will be plotted in the chart
     * @param chartTitle
     *            optional title that is placed above the chart
     * @param chartDomainValueTitle
     *            label placed along the X axis
     */
    public void createXYLineChart(final XYDataset dataset1,
            final String chartTitle, final String chartDomainValueTitle) {

        this.type = ChartType.SCLK;

        this.chart = ChartFactory.createXYLineChart(chartTitle == null ? ""
                : chartTitle, chartDomainValueTitle == null ? "SCLK"
                : chartDomainValueTitle, "", dataset1,
                PlotOrientation.VERTICAL, true, true, false);

        this.chart.setBackgroundPaint(Color.white);
        this.chart.setBorderVisible(true);
        this.chart.setBorderPaint(Color.BLACK);
        this.chart.setAntiAlias(false);
        final XYPlot plot = this.chart.getXYPlot();
        plot.setOrientation(PlotOrientation.VERTICAL);
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.lightGray);
        plot.setRangeGridlinePaint(Color.lightGray);
        plot.setOutlineVisible(false);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.getRangeAxis().setFixedDimension(15.0);
        plot.getRangeAxis().setLowerMargin(0.02);
        plot.getDomainAxis().setLowerMargin(0.02);
        plot.getRangeAxis().setUpperMargin(0.02);
        plot.getDomainAxis().setUpperMargin(0.02);
        ((NumberAxis) plot.getRangeAxis()).setAutoRangeStickyZero(false);
        plot.setRangeGridlinesVisible(true);
        plot.setDomainGridlinesVisible(true);
        plot.setNoDataMessage("No Data");
        final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesLinesVisible(0, true);
        renderer.setSeriesShapesVisible(0, false);
        plot.setRenderer(renderer);
        ValueAxis axis = plot.getDomainAxis();
        axis.setVerticalTickLabels(true);
        Font font = axis.getTickLabelFont();
        axis.setTickLabelFont(font.deriveFont(6));
        axis = plot.getRangeAxis();
        font = axis.getTickLabelFont();
        axis.setTickLabelFont(font.deriveFont(6));
    }

    /**
     * Creates a TimeSeries chart using the properties in the given view
     * configuration.
     * 
     * @param dataset1
     *            data that will be plotted in the chart
     * @param config
     *            the ChannelChartViewConfiguration object containing chart
     *            properties
     */
    public void createTimeSeriesChart(final XYDataset dataset1,
            final ChannelChartViewConfiguration config) {

        final String domainValueTitle = config.getDomainLabel();
        final String rangeValueTitle = config.getRangeLabel(); // replaces the
        // null in the
        // createChart
        // below when
        // ready
        createTimeSeriesChart(dataset1, rangeValueTitle, domainValueTitle);
        setChartConfiguration(config);
    }

    /**
     * Creates an XY Line chart using the properties in the given view
     * configuration.
     * 
     * @param dataset1
     *            data that will be plotted in the chart
     * @param config
     *            the ChannelChartViewConfiguration object containing chart
     *            properties
     */
    public void createXYLineChart(final XYDataset dataset1,
            final ChannelChartViewConfiguration config) {

        final String domainValueTitle = config.getDomainLabel();
        final String rangeValueTitle = config.getRangeLabel(); // replaces the
        // null in the
        // createChart
        // below when
        // ready
        createXYLineChart(dataset1, rangeValueTitle, domainValueTitle);
        setChartConfiguration(config);
    }

    /**
     * Updates chart properties from the given view configuration object.
     * 
     * @param config
     *            the ChannelChartViewConfiguration object containing chart
     *            properties
     */
    public void setChartConfiguration(final ChannelChartViewConfiguration config) {
        final XYPlot plot = this.chart.getXYPlot();
        final ValueAxis domainAxis = plot.getDomainAxis();
        final ValueAxis rangeAxis = plot.getRangeAxis();

        if (config.getPlotTitle() != null) {
            final TextTitle title = this.chart.getTitle();
            title.setText(config.getPlotTitle());
            if (config.getPlotTitleColor() != null) {
                this.chart.getTitle().setPaint(
                        ChillColorCreator.getAwtColor(config
                                .getPlotTitleColor()));
            }
            if (config.getPlotTitleFont() != null) {
                this.chart.getTitle().setFont(
                        ChillFontCreator.getAwtFont(config.getPlotTitleFont()));
            }
        } else {
            this.chart.setTitle((TextTitle) null);
        }

        this.chart.setAntiAlias(config.isDrawAntiAliased());

        final LegendTitle legend = this.chart.getLegend();

        if (legend != null) {
            if (config.getLegendBackgroundColor() != null) {
                legend.setBackgroundPaint(ChillColorCreator.getAwtColor(config
                        .getLegendBackgroundColor()));
            }
            if (config.getLegendFont() != null) {
                legend.setItemFont(ChillFontCreator.getAwtFont(config
                        .getLegendFont()));
            }
            legend.setItemLabelPadding(new RectangleInsets(5, 5, 0, 10));
            legend
                    .setLegendItemGraphicPadding(new RectangleInsets(5, 10, 5,
                            5));
        }
        if (config.getPlotBackgroundColor() != null) {
            plot.setBackgroundPaint(ChillColorCreator.getAwtColor(config
                    .getPlotBackgroundColor()));
        }
        if (config.getPlotOutlineColor() != null) {
            plot.setOutlinePaint(ChillColorCreator.getAwtColor(config
                    .getPlotOutlineColor()));
        }
        plot.setOutlineStroke(new BasicStroke(config.getPlotOutlineStroke()));
        if (config.isVerticalPlotOrientation()) {
            plot.setOrientation(PlotOrientation.VERTICAL);
        } else {
            plot.setOrientation(PlotOrientation.HORIZONTAL);
        }

        if (config.getChartBackgroundColor() != null) {
            this.chart.setBackgroundPaint(ChillColorCreator.getAwtColor(config
                    .getChartBackgroundColor()));
        }
        if (config.getDomainColor() != null) {
            domainAxis.setLabelPaint(ChillColorCreator.getAwtColor(config
                    .getDomainColor()));
        }
        if (config.getDomainLabel() != null) {
            domainAxis.setLabel(config.getDomainLabel());
        }
        if (config.getDomainLabelFont() != null) {
            domainAxis.setLabelFont(ChillFontCreator.getAwtFont(config
                    .getDomainLabelFont()));
        }
        if (config.getDomainTickFont() != null) {
            domainAxis.setTickLabelFont(ChillFontCreator.getAwtFont(config
                    .getDomainTickFont()));
        }
        domainAxis.setTickMarksVisible(config.isShowDomainTicks());
        domainAxis.setTickLabelsVisible(config.isShowDomainTickLabels());
        domainAxis.setVerticalTickLabels(config.isVerticalDomainTickLabels());

        if (this.type == ChartType.TIME_SERIES) {
            ((DateAxis) domainAxis).setDateFormatOverride(new SimpleDateFormat(
                    config.getDomainTimeFormat()));
        }

        if (config.getRangeColor() != null) {
            rangeAxis.setLabelPaint(ChillColorCreator.getAwtColor(config
                    .getRangeColor()));
        }
        if (config.getRangeLabel() != null) {
            rangeAxis.setLabel(config.getRangeLabel());
        }
        if (config.getRangeLabelFont() != null) {
            rangeAxis.setLabelFont(ChillFontCreator.getAwtFont(config
                    .getRangeLabelFont()));
        }
        if (config.getRangeTickFont() != null) {
            rangeAxis.setTickLabelFont(ChillFontCreator.getAwtFont(config
                    .getRangeTickFont()));
        }
        rangeAxis.setAutoRange(config.isAutoAdjustRange());
        rangeAxis.setTickMarksVisible(config.isShowRangeTicks());
        rangeAxis.setTickLabelsVisible(config.isShowRangeTickLabels());
        rangeAxis.setAutoRange(config.isAutoAdjustRange());
        if (!config.isAutoAdjustRange()) {
            final Range r = new Range(config.getRangeMin(), config
                    .getRangeMax());
            rangeAxis.setRange(r);
        }
    }

    /**
     * Updates the given view configuration object from the chart object.
     * 
     * @param config
     *            the ChannelChartViewConfiguration object in which chart
     *            properties will be updated
     */
    public void getChartConfiguration(final ChannelChartViewConfiguration config) {
        final XYPlot plot = this.chart.getXYPlot();
        final ValueAxis domainAxis = plot.getDomainAxis();
        final ValueAxis rangeAxis = plot.getRangeAxis();

        final TextTitle title = this.chart.getTitle();
        if (title == null || this.chart.getTitle().getText().equals("")) {
            config.setShowPlotTitle(false);
            config.setPlotTitle(null);
        } else {
            config.setShowPlotTitle(true);
            config.setPlotTitle(this.chart.getTitle().getText());
        }

        config.setDrawAntiAliased(this.chart.getAntiAlias());

        Color color = (java.awt.Color) plot.getBackgroundPaint();
        ChillColor setColor = new ChillColor(color.getRed(), color.getGreen(),
                color.getBlue());
        config.setPlotBackgroundColor(setColor);

        if (title != null) {
            color = (java.awt.Color) title.getPaint();
            setColor = new ChillColor(color.getRed(), color.getGreen(), color
                    .getBlue());
            config.setPlotTitleColor(setColor);
            final Font font = title.getFont();
            final ChillFont chillFont = new ChillFont(font.getFontName(), font
                    .getSize(), ChillFontCreator.mapAwtStyleToSwtStyle(font
                    .getStyle()));
            config.setPlotTitleFont(chillFont);
        }
        color = (java.awt.Color) plot.getOutlinePaint();
        setColor = new ChillColor(color.getRed(), color.getGreen(), color
                .getBlue());
        config.setPlotOutlineColor(setColor);

        final LegendTitle legend = this.chart.getLegend();
        if (legend != null) {
            final Font font = legend.getItemFont();
            final ChillFont chillFont = new ChillFont(font.getFontName(), font
                    .getSize(), ChillFontCreator.mapAwtStyleToSwtStyle(font
                    .getStyle()));
            config.setLegendFont(chillFont);
            color = (java.awt.Color) legend.getBackgroundPaint();
            setColor = new ChillColor(color.getRed(), color.getGreen(), color
                    .getBlue());
            config.setLegendBackgroundColor(setColor);
        }

        final BasicStroke stroke = (BasicStroke) plot.getOutlineStroke();
        config.setPlotOutlineStroke((int) stroke.getLineWidth());
        ;

        color = (java.awt.Color) this.chart.getBackgroundPaint();
        setColor = new ChillColor(color.getRed(), color.getGreen(), color
                .getBlue());
        config.setChartBackgroundColor(setColor);

        color = (java.awt.Color) domainAxis.getLabelPaint();
        setColor = new ChillColor(color.getRed(), color.getGreen(), color
                .getBlue());
        config.setDomainColor(setColor);

        Font font = domainAxis.getLabelFont();
        ChillFont chillFont = new ChillFont(font.getFontName(), font.getSize(),
                ChillFontCreator.mapAwtStyleToSwtStyle(font.getStyle()));
        config.setDomainLabelFont(chillFont);

        config.setDomainLabel(domainAxis.getLabel());

        font = domainAxis.getTickLabelFont();
        chillFont = new ChillFont(font.getFontName(), font.getSize(),
                ChillFontCreator.mapAwtStyleToSwtStyle(font.getStyle()));
        config.setDomainTickFont(chillFont);

        config.setShowDomainTickLabels(domainAxis.isTickLabelsVisible());
        config.setShowDomainTicks(domainAxis.isTickMarksVisible());
        config.setVerticalDomainTickLabels(domainAxis.isVerticalTickLabels());

        color = (java.awt.Color) rangeAxis.getLabelPaint();
        setColor = new ChillColor(color.getRed(), color.getGreen(), color
                .getBlue());
        config.setRangeColor(setColor);

        font = rangeAxis.getLabelFont();
        chillFont = new ChillFont(font.getFontName(), font.getSize(),
                ChillFontCreator.mapAwtStyleToSwtStyle(font.getStyle()));
        config.setRangeLabelFont(chillFont);

        config.setRangeLabel(rangeAxis.getLabel());

        font = rangeAxis.getTickLabelFont();
        chillFont = new ChillFont(font.getFontName(), font.getSize(),
                ChillFontCreator.mapAwtStyleToSwtStyle(font.getStyle()));
        config.setRangeTickFont(chillFont);

        config.setShowRangeTickLabels(rangeAxis.isTickLabelsVisible());
        config.setShowRangeTicks(rangeAxis.isTickMarksVisible());

        config.setAutoAdjustRange(rangeAxis.isAutoRange());

        if (!config.isAutoAdjustRange()) {
            config.setRangeMin(rangeAxis.getRange().getLowerBound());
            config.setRangeMax(rangeAxis.getRange().getUpperBound());
        }

        config
                .setVerticalPlotOrientation(plot.getOrientation() == PlotOrientation.VERTICAL);
    }
}
