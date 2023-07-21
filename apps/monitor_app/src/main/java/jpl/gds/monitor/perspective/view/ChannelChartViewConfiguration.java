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
package jpl.gds.monitor.perspective.view;

import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.monitor.perspective.view.channel.ChannelSet;
import jpl.gds.perspective.config.PerspectiveProperties;
import jpl.gds.perspective.config.ViewType;
import jpl.gds.perspective.view.ViewConfiguration;
import jpl.gds.shared.annotation.ToDo;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.swt.types.ChillFont;

/**
 * ChannelChartViewConfiguration encapsulates the configuration for the channel
 * chart (plot) view.
 */
public class ChannelChartViewConfiguration extends ViewConfiguration implements StationSupport, RealtimeRecordedSupport {
    private static final String DOMAIN_CHANNEL_SET_CONFIG = "domainChannelSet";
    private static final String RANGE_CHANNEL_SET_CONFIG = "channelSet";
    private static final String PLOT_TITLE_CONFIG = "plotTitle";
    private static final String PLOT_SHOW_TITLE_CONFIG = "showTitle";
    private static final String PLOT_TITLE_FONT_CONFIG = "titleFont";
    private static final String PLOT_TITLE_COLOR_CONFIG = "titleColor";
    private static final String DOMAIN_LABEL_CONFIG = "domainLabel";
    private static final String DOMAIN_LABEL_FONT_CONFIG = "domainLabelFont";
    private static final String DOMAIN_COLOR_CONFIG = "domainColor";
    private static final String DOMAIN_SHOW_TICK_LABELS_CONFIG = "domainShowTickLabels";
    private static final String DOMAIN_TICK_LABEL_FONT_CONFIG = "domainTickLabelFont";
    private static final String DOMAIN_SHOW_TICKS_CONFIG = "domainShowTicks";
    private static final String DOMAIN_VERTICAL_TICK_LABELS_CONFIG = "domainVerticalTicks";
    private static final String RANGE_LABEL_CONFIG = "rangeLabel";
    private static final String RANGE_LABEL_FONT_CONFIG = "rangeLabelFont";
    private static final String RANGE_COLOR_CONFIG = "rangeColor";
    private static final String RANGE_SHOW_TICK_LABELS_CONFIG = "rangeShowTickLabels";
    private static final String RANGE_TICK_LABEL_FONT_CONFIG = "rangeTickLabelFont";
    private static final String RANGE_SHOW_TICKS_CONFIG = "rangeShowTicks";
    private static final String RANGE_AUTO_ADJUST_CONFIG = "rangeAutoAdjust";
    private static final String RANGE_MIN_RANGE_CONFIG = "rangeMin";
    private static final String RANGE_MAX_RANGE_CONFIG = "rangeMax";
    private static final String PLOT_OUTLINE_STROKE_CONFIG = "plotOutlineStroke";
    private static final String PLOT_OUTLINE_COLOR_CONFIG = "plotOutlineColor";
    private static final String PLOT_BACKGROUND_COLOR_CONFIG = "plotBackgroundColor";
    private static final String PLOT_VERTICAL_ORIENT_CONFIG = "plotVertical";
    private static final String CHART_DRAW_ANTI_ALIASED_CONFIG = "chartAntiAliased";
    private static final String CHART_BACKGROUND_COLOR_CONFIG = "chartBackgroundColor";
    private static final String LEGEND_BACKGROUND_COLOR_CONFIG = "legendBackgroundColor";
    private static final String LEGEND_FONT_CONFIG = "legendFont";
    private static final String DOMAIN_AXIS_VALUE = "domainValueSelector";
    private static final String RANGE_AXIS_VALUE = "rangeValueSelector";
    private static final String RENDERER_LINE_STYLE = "rendererLineStyle";
    private static final String RECORDED_DATA_CONFIG = "displayIsRecordedData";
    private static final String USE_CHANNEL_NAME_CONFIG = "labelDomainWithChannelName";
    private static final String DOMAIN_TIME_FORMAT_CONFIG = "domainTimeFormat";
    private static final String RETAIN_HOURS_CONFIG = "dataRetentionHours";
    private static final String RETAIN_MINUTES_CONFIG = "dataRetentionMins";
    private static final String RETAIN_SECONDS_CONFIG = "dataRetentionSeconds";
    private static final String RETAIN_TIME_CONFIG = "dataRetentionTimeType";
    private static final String STEP_PLOT_CONFIG = "useStepPlot";
    private static final String STATION_CONFIG = "stationId";

    /**
     * Data plotted on X axis
     *
     */
    public enum XAxisChoice {
        /**
         * Earth Receive Time
         */
        ERT, 

        /**
         * SpaceCraft Event Time
         */
        SCET, 

        /**
         * Spacecraft CLocK 
         */
        SCLK, 

        /**
         * Local Solar Time
         */
        LST, 

        /**
         * Channel
         */
        CHANNEL
    };

    /**
     * Data plotted on Y axis
     *
     */
    public enum YAxisChoice {
        /**
         * DN (data in its rawest form)
         */
        Raw, 

        /**
         * Value of the raw data. Depending on the channel DN, EU or Status 
         * map to this.
         */
        Value
    };

    /**
     * Type of graph
     *
     */
    public enum RenderingStyle {
        /**
         * Each pair of consecutive x axis points are connected with a line. 
         * Line is essentially the slope between the points. End result looks 
         * like peaks.
         */
        LINE, 

        /**
         * x,y data points just have a shape over them, so graph looks like 
         * floating points
         */
        SHAPE, 

        /**
         * Data points are connected with lines and have a shape at each x 
         * axis point where the lines meet
         */
        SHAPE_AND_LINE, 

        /**
         * A specialized line graph.  Instead of the slope between 2 points, 
         * the line travels along the x-axis and then along the y-axis so the 
         * end result looks like steps.
         */
        STEP
    };

    /**
     * Enumeration of time types used to determine if plotted data is expired
     *
     */
    public enum RetentionTimeType {
        /**
         * Time when the ChannelChartPageComposite in MPCS receives the data
         */
        ReceiveTime, 

        /**
         * Earth Receive Time
         */
        ERT
    };

    private ChannelSet domainChannel, rangeChannel;

    /**
     * Creates an instance of ChannelChartViewConfiguration.
     * @param appContext the current application context
     */
    public ChannelChartViewConfiguration(final ApplicationContext appContext) {
        super(appContext);
        initToDefaults();
    }

    /**
     * Sets the flag indicating whether the plot is a step plot.
     * 
     * @param step
     *            true to use step plot; false to not use step plot
     */
    public void setUseStepPlot(final boolean step) {
        this.setConfigItem(STEP_PLOT_CONFIG, String.valueOf(step));
    }

    /**
     * Gets the flag indicating whether the plot is a step plot.
     * 
     * @return true if chart is a step plot, false otherwise
     */
    public boolean isUseStepPlot() {
        final String showStr = this.getConfigItem(STEP_PLOT_CONFIG);
        if (showStr == null) {
            return false;
        } else {
            return Boolean.parseBoolean(showStr);
        }
    }

    /**
     * Gets the rendering style of the plot line (shapes only, shapes and line,
     * line only, step)
     * 
     * @return the RenderingStyle
     */
    public RenderingStyle getRenderingStyle() {
        final String styleStr = getConfigItem(RENDERER_LINE_STYLE);
        if (styleStr == null) {
            setRenderingStyle(RenderingStyle.LINE);
            return RenderingStyle.LINE;
        } else {
            return RenderingStyle.valueOf(styleStr);
        }
    }

    /**
     * Sets the rendering style of the plot line (shapes only, shapes and line,
     * line only, step)
     * 
     * @param style
     *            the RenderingStyle to set
     */
    public void setRenderingStyle(final RenderingStyle style) {
        setConfigItem(RENDERER_LINE_STYLE, style.toString());
    }

    /**
     * Gets the stroke width (in pixels) of the plot line.
     * 
     * @return the stroke width
     */
    public int getPlotOutlineStroke() {
        final String intStr = getConfigItem(PLOT_OUTLINE_STROKE_CONFIG);
        if (intStr == null) {
            return 1;
        }
        return Integer.parseInt(intStr);
    }

    /**
     * Gets the stroke width (in pixels) of the plot line.
     * 
     * @param strokeWidth
     *            the stroke width to set
     */
    public void setPlotOutlineStroke(final int strokeWidth) {
        setConfigItem(PLOT_OUTLINE_STROKE_CONFIG, String.valueOf(strokeWidth));
    }

    /**
     * Gets the minimum value of the range axis.
     * 
     * @return the range axis lower bound
     */
    public double getRangeMin() {
        final String doubleStr = getConfigItem(RANGE_MIN_RANGE_CONFIG);
        if (doubleStr == null) {
            return Double.MIN_VALUE;
        }
        return Double.parseDouble(doubleStr);
    }

    /**
     * Sets the minimum value of the range axis.
     * 
     * @param range
     *            the range axis lower bound to set
     */
    public void setRangeMin(final double range) {
        setConfigItem(RANGE_MIN_RANGE_CONFIG, String.valueOf(range));
    }

    /**
     * Gets the maximum value of the range axis.
     * 
     * @return the range axis upper bound
     */
    public double getRangeMax() {
        final String doubleStr = getConfigItem(RANGE_MAX_RANGE_CONFIG);
        if (doubleStr == null) {
            return Double.MAX_VALUE;
        }
        return Double.parseDouble(doubleStr);
    }

    /**
     * Sets the maximum value of the range axis.
     * 
     * @param range
     *            the range axis upper bound
     */
    public void setRangeMax(final double range) {
        setConfigItem(RANGE_MAX_RANGE_CONFIG, String.valueOf(range));
    }

    /**
     * setDomainValueSelector Sets the default Domain (x-axis) value to plot
     * against (currently ERT, SCET, SCLK)
     * 
     * @param selector is the domain type selector
     */
    public void setDomainValueSelector(final XAxisChoice selector) {
        if (selector == null) {
            this.removeConfigItem(DOMAIN_AXIS_VALUE);
        } else {
            this.setConfigItem(DOMAIN_AXIS_VALUE, selector.name());
        }
    }

    /**
     * setRangeValueSelector Sets the default Range (y-axis) value to plot
     * against (currently Dn or eu)
     * 
     * @param selector is the range type selector
     */
    public void setRangeValueSelector(final YAxisChoice selector) {
        /*
         * Implementation was stupid.
         * Relied upon throw and catch of NPE.
         */
        if (selector == null) {
            this.removeConfigItem(RANGE_AXIS_VALUE);
        } else {
            this.setConfigItem(RANGE_AXIS_VALUE, selector.name());
        } 
    }

    /**
     * Gets what type of data will be plotted on the range
     * 
     * @return choice for what to plot on the y axis
     */
    public YAxisChoice getRangeValueSelector() {
        final String val = this.getConfigItem(RANGE_AXIS_VALUE);
        try {
            return Enum.valueOf(YAxisChoice.class, val);
        } catch (final Exception e) {
            return YAxisChoice.Raw;
        }
    }

    /**
     * Gets what type of data will be plotted on the domain
     * 
     * @return choice for what to plot on the axis
     */
    public XAxisChoice getDomainValueSelector() {
        final String val = this.getConfigItem(DOMAIN_AXIS_VALUE);
        try {
            return Enum.valueOf(XAxisChoice.class, val);
        } catch (final Exception e) {
            return XAxisChoice.ERT;
        }
    }

    /**
     * Sets the plot title text.
     * 
     * @param title
     *            the title text
     */
    public void setPlotTitle(final String title) {
        if (title != null) {
            this.setConfigItem(PLOT_TITLE_CONFIG, title);
        } else {
            this.removeConfigItem(PLOT_TITLE_CONFIG);
        }
    }

    /**
     * Gets the plot title text.
     * 
     * @return the title text
     */
    public String getPlotTitle() {
        return this.getConfigItem(PLOT_TITLE_CONFIG);
    }

    /**
     * Sets the plot title font.
     * 
     * @param font
     *            the ChillFont to set
     */
    public void setPlotTitleFont(final ChillFont font) {
        setFont(PLOT_TITLE_FONT_CONFIG, font);
    }

    /**
     * Gets the plot title font.
     * 
     * @return the ChillFont
     */
    public ChillFont getPlotTitleFont() {
        return getFont(PLOT_TITLE_FONT_CONFIG);
    }

    private void setFont(final String configName, final ChillFont font) {
        if (font != null) {
            final String fontStr = font.getFace() + "," + font.getSize() + ","
                    + font.getStyle();
            setConfigItem(configName, fontStr);
        } else {
            removeConfigItem(configName);
        }
    }

    private ChillFont getFont(final String configName) {
        final String fontStr = getConfigItem(configName);
        if (fontStr == null) {
            return null;
        }
        final String[] pieces = fontStr.split(",");
        final ChillFont font = new ChillFont();
        font.setFace(pieces[0]);
        try {
            font.setSize(Integer.parseInt(pieces[1]));
        } catch (final NumberFormatException e) {
            font.setSize(Enum.valueOf(ChillFont.FontSize.class, pieces[1]));
        }
        font.setStyle(Integer.parseInt(pieces[2]));
        return font;
    }

    /**
     * Sets the label text for the domain axis.
     * 
     * @param label
     *            the label text to set
     */
    public void setDomainLabel(final String label) {
        if (label != null) {
            this.setConfigItem(DOMAIN_LABEL_CONFIG, label);
        } else {
            this.removeConfigItem(DOMAIN_LABEL_CONFIG);
        }
    }

    /**
     * Gets the label text for the domain axis.
     * 
     * @return the label text to set
     */
    public String getDomainLabel() {
        return this.getConfigItem(DOMAIN_LABEL_CONFIG);
    }

    /**
     * Sets the font for the domain label text.
     * 
     * @param font
     *            the ChillFont to set
     */
    public void setDomainLabelFont(final ChillFont font) {
        setFont(DOMAIN_LABEL_FONT_CONFIG, font);
    }

    /**
     * Gets the font for the domain label text.
     * 
     * @return the ChillFont
     */
    public ChillFont getDomainLabelFont() {
        return getFont(DOMAIN_LABEL_FONT_CONFIG);
    }

    /**
     * Sets the font for the domain tick text.
     * 
     * @param font the ChillFont to set
     */
    public void setDomainTickFont(final ChillFont font) {
        setFont(DOMAIN_TICK_LABEL_FONT_CONFIG, font);
    }

    /**
     * Gets the font for the domain tick text.
     * 
     * @return the ChillFont
     */
    public ChillFont getDomainTickFont() {
        return getFont(DOMAIN_TICK_LABEL_FONT_CONFIG);
    }

    /**
     * Sets the label text on the range axis.
     * 
     * @param label
     *            the text to set
     */
    public void setRangeLabel(final String label) {
        if (label != null) {
            this.setConfigItem(RANGE_LABEL_CONFIG, label);
        } else {
            this.removeConfigItem(RANGE_LABEL_CONFIG);
        }
    }

    /**
     * Gets the label text on the range axis.
     * 
     * @return the label text
     */
    public String getRangeLabel() {
        return this.getConfigItem(RANGE_LABEL_CONFIG);
    }

    /**
     * Sets the font for the range axis label.
     * 
     * @param font
     *            the ChillFont to set
     */
    public void setRangeLabelFont(final ChillFont font) {
        setFont(RANGE_LABEL_FONT_CONFIG, font);
    }

    /**
     * Gets the font for the range axis label.
     * 
     * @return the ChillFont
     */
    public ChillFont getRangeLabelFont() {
        return getFont(RANGE_LABEL_FONT_CONFIG);
    }

    /**
     * Sets the font for the range tick labels.
     * 
     * @param font
     *            the ChillFont to set
     */
    public void setRangeTickFont(final ChillFont font) {
        setFont(RANGE_TICK_LABEL_FONT_CONFIG, font);
    }

    /**
     * Gets the font for the range tick labels.
     * 
     * @return the ChillFont
     */
    public ChillFont getRangeTickFont() {
        return getFont(RANGE_TICK_LABEL_FONT_CONFIG);
    }

    /**
     * Sets the flag indicating whether the plot if vertically or horizontally
     * oriented.
     * 
     * @param vertical
     *            true to orient the plot vertically; false to orient
     *            horizontally
     */
    public void setVerticalPlotOrientation(final boolean vertical) {
        this.setConfigItem(PLOT_VERTICAL_ORIENT_CONFIG, String
                .valueOf(vertical));
    }

    /**
     * Gets the flag indicating whether the plot if vertically or horizontally
     * oriented.
     * 
     * @return true if the plot is oriented vertically; false for horizontally
     */
    public boolean isVerticalPlotOrientation() {
        final String showStr = this.getConfigItem(PLOT_VERTICAL_ORIENT_CONFIG);
        if (showStr == null) {
            return true;
        } else {
            return Boolean.parseBoolean(showStr);
        }
    }

    /**
     * Gets the color of the plot title.
     * 
     * @return the ChillColor
     */
    public ChillColor getPlotTitleColor() {
        return getColor(PLOT_TITLE_COLOR_CONFIG);
    }

    /**
     * Sets the color of the plot title.
     * 
     * @param color
     *            the ChillColor to set
     */
    public void setPlotTitleColor(final ChillColor color) {
        setColor(PLOT_TITLE_COLOR_CONFIG, color);
    }

    /**
     * Gets the color of the domain axis.
     * 
     * @return the ChillColor
     */
    public ChillColor getDomainColor() {
        return getColor(DOMAIN_COLOR_CONFIG);
    }

    /**
     * Sets the color of the domain axis.
     * 
     * @param color
     *            the ChillColor to set
     */
    public void setDomainColor(final ChillColor color) {
        setColor(DOMAIN_COLOR_CONFIG, color);
    }

    /**
     * Gets the color of the range axis.
     * 
     * @return the ChillColor
     */
    public ChillColor getRangeColor() {
        return getColor(RANGE_COLOR_CONFIG);
    }

    /**
     * Sets the color of the range axis.
     * 
     * @param color
     *            the ChillColor to set
     */
    public void setRangeColor(final ChillColor color) {
        setColor(RANGE_COLOR_CONFIG, color);
    }

    /**
     * Gets the color of the plot outline.
     * 
     * @return the ChillColor
     */
    public ChillColor getPlotOutlineColor() {
        return getColor(PLOT_OUTLINE_COLOR_CONFIG);
    }

    /**
     * Sets the color of the plot outline.
     * 
     * @param color
     *            the ChillColor to set
     */
    public void setPlotOutlineColor(final ChillColor color) {
        setColor(PLOT_OUTLINE_COLOR_CONFIG, color);
    }

    /**
     * Gets the color of the plot background.
     * 
     * @return the ChillColor
     */
    public ChillColor getPlotBackgroundColor() {
        return getColor(PLOT_BACKGROUND_COLOR_CONFIG);
    }

    /**
     * Sets the color of the plot background.
     * 
     * @param color
     *            the ChillColor to set
     */
    public void setPlotBackgroundColor(final ChillColor color) {
        setColor(PLOT_BACKGROUND_COLOR_CONFIG, color);
    }

    /**
     * Gets the color of the chart background.
     * 
     * @return the ChillColor
     */
    public ChillColor getChartBackgroundColor() {
        return getColor(CHART_BACKGROUND_COLOR_CONFIG);
    }

    /**
     * Sets the color of the chart background.
     * 
     * @param color
     *            the ChillColor to set
     */
    public void setChartBackgroundColor(final ChillColor color) {
        setColor(CHART_BACKGROUND_COLOR_CONFIG, color);
    }

    /**
     * Gets the color of the legend background.
     * 
     * @return the ChillColor
     */
    public ChillColor getLegendBackgroundColor() {
        return getColor(LEGEND_BACKGROUND_COLOR_CONFIG);
    }

    /**
     * Sets the color of the legend background.
     * 
     * @param color
     *            the ChillColor to set
     */
    public void setLegendBackgroundColor(final ChillColor color) {
        setColor(LEGEND_BACKGROUND_COLOR_CONFIG, color);
    }

    /**
     * Gets the font for the chart legend.
     * 
     * @return the ChillFont
     */
    public ChillFont getLegendFont() {
        return getFont(LEGEND_FONT_CONFIG);
    }

    /**
     * Sets the font for the chart legend.
     * 
     * @param font
     *            the ChillFont to set
     */
    public void setLegendFont(final ChillFont font) {
        setFont(LEGEND_FONT_CONFIG, font);
    }

    /**
     * Sets the flag indicating whether the default legend label is the channel
     * name, as opposed to the channel ID. oriented.
     * 
     * @param useName
     *            true to use channel name, false to use ID
     */
    public void setUseChannelNameForLegend(final boolean useName) {
        this.setConfigItem(USE_CHANNEL_NAME_CONFIG, String.valueOf(useName));
    }

    /**
     * Gets the flag indicating whether the default legend label is the channel
     * name as opposed to the channel ID.
     * 
     * @return true if the channel name should be used; false for channel ID
     */
    public boolean isUseChannelNameForLegend() {
        final String showStr = this.getConfigItem(USE_CHANNEL_NAME_CONFIG);
        if (showStr == null) {
            return false;
        } else {
            return Boolean.parseBoolean(showStr);
        }
    }

    private void setColor(final String configName, final ChillColor color) {
        if (color != null) {
            final String colorStr = color.getRed() + "," + color.getGreen()
                    + "," + color.getBlue();
            setConfigItem(configName, colorStr);
        } else {
            removeConfigItem(configName);
        }
    }

    private ChillColor getColor(final String configName) {
        final String colorStr = getConfigItem(configName);
        if (colorStr == null) {
            return null;
        }
        final String[] pieces = colorStr.split(",");
        final ChillColor color = new ChillColor();
        color.setRed(Integer.parseInt(pieces[0]));
        color.setGreen(Integer.parseInt(pieces[1]));
        color.setBlue(Integer.parseInt(pieces[2]));
        return color;
    }

    /**
     * Sets the flag indicating whether the domain tick labels are vertically
     * oriented.
     * 
     * @param vertical
     *            true if orientation should be vertical; false for horizontal
     */
    public void setVerticalDomainTickLabels(final boolean vertical) {
        this.setConfigItem(DOMAIN_VERTICAL_TICK_LABELS_CONFIG, String
                .valueOf(vertical));
    }

    /**
     * Gets the flag indicating whether the domain tick labels are vertically
     * oriented.
     * 
     * @return true if orientation should be vertical; false for horizontal
     */
    public boolean isVerticalDomainTickLabels() {
        final String showStr = this
                .getConfigItem(DOMAIN_VERTICAL_TICK_LABELS_CONFIG);
        if (showStr == null) {
            return false;
        } else {
            return Boolean.parseBoolean(showStr);
        }
    }

    /**
     * Sets the flag indicating whether than range axis adjusts automatically
     * for new values.
     * 
     * @param enabled
     *            true to enable auto-range
     */
    public void setAutoAdjustRange(final boolean enabled) {
        this.setConfigItem(RANGE_AUTO_ADJUST_CONFIG, String.valueOf(enabled));
    }

    /**
     * Gets the flag indicating whether than range axis adjusts automatically
     * for new values.
     * 
     * @return true if auto-range is enabled, false otherwise
     */
    public boolean isAutoAdjustRange() {
        final String showStr = this.getConfigItem(RANGE_AUTO_ADJUST_CONFIG);
        if (showStr == null) {
            return true;
        } else {
            return Boolean.parseBoolean(showStr);
        }
    }

    /**
     * Sets the flag indicating whether the chart is anti-aliased.
     * 
     * @param enabled
     *            true if anti-aliasing is enabled
     */
    public void setDrawAntiAliased(final boolean enabled) {
        this.setConfigItem(CHART_DRAW_ANTI_ALIASED_CONFIG, String
                .valueOf(enabled));
    }

    /**
     * Gets the flag indicating whether the chart is anti-aliased.
     * 
     * @return true if anti-aliasing is enabled
     */
    public boolean isDrawAntiAliased() {
        final String showStr = this
                .getConfigItem(CHART_DRAW_ANTI_ALIASED_CONFIG);
        if (showStr == null) {
            return false;
        } else {
            return Boolean.parseBoolean(showStr);
        }
    }

    /**
     * Gets the flag indicating whether the plot title is visible.
     * 
     * @return true if the title is visible; false if not
     */
    public boolean isShowPlotTitle() {
        final String showStr = this.getConfigItem(PLOT_SHOW_TITLE_CONFIG);
        if (showStr == null) {
            return false;
        } else {
            return Boolean.parseBoolean(showStr);
        }
    }

    /**
     * Sets the flag indicating whether the plot title is visible.
     * 
     * @param show
     *            true if the title should be visible; false if not
     */
    public void setShowPlotTitle(final boolean show) {
        this.setConfigItem(PLOT_SHOW_TITLE_CONFIG, String.valueOf(show));
    }

    /**
     * Gets the flag indicating whether the domain tick labels are visible.
     * 
     * @return true if the labels are visible; false if not
     */
    public boolean isShowDomainTickLabels() {
        final String showStr = this
                .getConfigItem(DOMAIN_SHOW_TICK_LABELS_CONFIG);
        if (showStr == null) {
            return true;
        } else {
            return Boolean.parseBoolean(showStr);
        }
    }

    /**
     * Sets the flag indicating whether the domain tick labels are visible.
     * 
     * @param show
     *            true if the labels should be visible; false if not
     */
    public void setShowDomainTickLabels(final boolean show) {
        this
        .setConfigItem(DOMAIN_SHOW_TICK_LABELS_CONFIG, String
                .valueOf(show));
    }

    /**
     * Gets the flag indicating whether the domain ticks are visible.
     * 
     * @return true if the ticks should be visible; false if not
     */
    public boolean isShowDomainTicks() {
        final String showStr = this.getConfigItem(DOMAIN_SHOW_TICKS_CONFIG);
        if (showStr == null) {
            return true;
        } else {
            return Boolean.parseBoolean(showStr);
        }
    }

    /**
     * Sets the flag indicating whether the domain ticks are visible.
     * 
     * @param show
     *            true if the ticks should be visible; false if not
     */
    public void setShowDomainTicks(final boolean show) {
        this.setConfigItem(DOMAIN_SHOW_TICKS_CONFIG, String.valueOf(show));
    }

    /**
     * Gets the flag indicating whether the range tick labels are visible.
     * 
     * @return true if the tick labels should be visible; false if not
     */
    public boolean isShowRangeTickLabels() {
        final String showStr = this
                .getConfigItem(RANGE_SHOW_TICK_LABELS_CONFIG);
        if (showStr == null) {
            return true;
        } else {
            return Boolean.parseBoolean(showStr);
        }
    }

    /**
     * Sets the flag indicating whether the range tick labels are visible.
     * 
     * @param show
     *            true if the tick labels should be visible; false if not
     */
    public void setShowRangeTickLabels(final boolean show) {
        this.setConfigItem(RANGE_SHOW_TICK_LABELS_CONFIG, String.valueOf(show));
    }

    /**
     * Gets the flag indicating whether the range ticks are visible.
     * 
     * @return true if the ticks should be visible; false if not
     */
    public boolean isShowRangeTicks() {
        final String showStr = this.getConfigItem(RANGE_SHOW_TICKS_CONFIG);
        if (showStr == null) {
            return true;
        } else {
            return Boolean.parseBoolean(showStr);
        }
    }

    /**
     * Sets the flag indicating whether the range ticks are visible.
     * 
     * @param show
     *            true if the ticks should be visible; false if not
     */
    public void setShowRangeTicks(final boolean show) {
        this.setConfigItem(RANGE_SHOW_TICKS_CONFIG, String.valueOf(show));
    }

    /**
     * Gets the domain channel set for this chart view.
     * @param defProv the channel definition provider
     * 
     * @return Returns the domain channel set for this chart view.
     */
    public ChannelSet getDomainChannels(final IChannelDefinitionProvider defProv) {
        final String chans = this.getConfigItem(DOMAIN_CHANNEL_SET_CONFIG);
        if (chans != null) {
            this.domainChannel = new ChannelSet();
            this.domainChannel.loadFromString(defProv, chans);
        }
        return this.domainChannel;
    }

    /**
     * Sets the Domain channel set for this chart view.
     * 
     * @param channels
     *            The domain channels to set.
     */
    public void setDomainChannels(final ChannelSet channels) {
        this.domainChannel = channels;
        if (this.domainChannel != null) {
            this.setConfigItem(DOMAIN_CHANNEL_SET_CONFIG, this.domainChannel
                    .toString());
        } else {
            this.removeConfigItem(DOMAIN_CHANNEL_SET_CONFIG);
        }
    }

    /**
     * Gets the range channel set for this chart view.
     * 
     * @return Returns the range channel set for this chart view.
     */
    public ChannelSet getRangeChannels(final IChannelDefinitionProvider defProv) {
        final String chans = this.getConfigItem(RANGE_CHANNEL_SET_CONFIG);
        if (chans != null) {
            this.rangeChannel = new ChannelSet();
            this.rangeChannel.loadFromString(defProv, chans);
        }
        return this.rangeChannel;
    }

    /**
     * Sets the Range channel set for this chart view.
     * 
     * @param channels
     *            The range channels to set.
     */
    public void setRangeChannels(final ChannelSet channels) {
        this.rangeChannel = channels;
        if (this.rangeChannel != null) {
            this.setConfigItem(RANGE_CHANNEL_SET_CONFIG, this.rangeChannel
                    .toString());
        } else {
            this.removeConfigItem(RANGE_CHANNEL_SET_CONFIG);
        }
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.perspective.view.StationSupport#getStationId()
     */
    @Override
    @ToDo("Figure out how to implement in a common location")
    public int getStationId() {
        final String stationStr = this.getConfigItem(STATION_CONFIG);
        if (stationStr == null) {
            return StationIdHolder.UNSPECIFIED_VALUE;
        }
        try {
            return Integer.valueOf(stationStr);
        } catch (final NumberFormatException e) {
            TraceManager.getDefaultTracer().warn("Non-integer station ID " + stationStr + " found in Channel Plot View Configuration");

            return StationIdHolder.UNSPECIFIED_VALUE;
        }
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.perspective.view.StationSupport#setStationId(int)
     */
    @Override
    @ToDo("Figure out how to implement in a common location")
    public void setStationId(final int station) {
        this.setConfigItem(STATION_CONFIG, String.valueOf(station));
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.perspective.view.RealtimeRecordedSupport#setRealtimeRecordedFilterType(jpl.gds.monitor.perspective.view.RealtimeRecordedFilterType)
     */
    @Override
    @ToDo("Figure out how to implement in a common location")
    public void setRealtimeRecordedFilterType(final RealtimeRecordedFilterType filterType) {
        this.setConfigItem(RECORDED_DATA_CONFIG, String.valueOf(filterType));
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.perspective.view.RealtimeRecordedSupport#getRealtimeRecordedFilterType()
     */
    @Override
    @ToDo("Figure out how to implement in a common location")
    public RealtimeRecordedFilterType getRealtimeRecordedFilterType() {
        final String str = this.getConfigItem(RECORDED_DATA_CONFIG);
        if (str == null) {
            return RealtimeRecordedFilterType.REALTIME;
        }
        RealtimeRecordedFilterType result = null;
        /*
         * Old perspective will have
         * a true/false value for this config item rather than
         * the new enum value. Detect this and convert here.
         */
        try {
            result = RealtimeRecordedFilterType.valueOf(str);
        } catch (final IllegalArgumentException e) {
            if (str.equals(Boolean.TRUE.toString())) {
                result = RealtimeRecordedFilterType.RECORDED;
            } else {
                result = RealtimeRecordedFilterType.REALTIME;
            } 
            // Set the converted value back into the config so it
            // will be saved properly.
            this.setConfigItem(RECORDED_DATA_CONFIG, result.toString());
        }

        return result;
    }

    /**
     * Sets the format specifier for the domain time axis labels.
     * 
     * @param format
     *            A java DateFormat string
     */
    public void setDomainTimeFormat(final String format) {
        this.setConfigItem(DOMAIN_TIME_FORMAT_CONFIG, format);
    }

    /**
     * Gets the format specifier for the domain time axis labels.
     * 
     * @return A java DateFormat string
     */
    public String getDomainTimeFormat() {
        return this.getConfigItem(DOMAIN_TIME_FORMAT_CONFIG);
    }

    /**
     * Sets the plot data retention hours.
     * 
     * @param hours
     *            number of hours to retain plot points
     */
    public void setDataRetentionHours(final int hours) {
        this.setConfigItem(RETAIN_HOURS_CONFIG, String.valueOf(hours));
    }

    /**
     * Gets the plot data retention hours.
     * 
     * @return number of hours to retain plot points
     */
    public int getDataRetentionHours() {
        final String hourStr = this.getConfigItem(RETAIN_HOURS_CONFIG);
        if (hourStr != null) {
            return Integer.valueOf(hourStr);
        } else {
            return 1;
        }
    }

    /**
     * Sets the plot data retention minutes.
     * 
     * @param minutes
     *            number of minutes to retain plot points
     */
    public void setDataRetentionMinutes(final int minutes) {
        this.setConfigItem(RETAIN_MINUTES_CONFIG, String.valueOf(minutes));
    }

    /**
     * Gets the plot data retention minutes.
     * 
     * @return number of minutes to retain plot points
     */
    public int getDataRetentionMinutes() {
        final String minStr = this.getConfigItem(RETAIN_MINUTES_CONFIG);
        if (minStr != null) {
            return Integer.valueOf(minStr);
        } else {
            return 0;
        }
    }

    /**
     * Sets the plot data retention seconds.
     * 
     * @param ticks
     *            number of seconds to retain plot points
     */
    public void setDataRetentionSeconds(final int ticks) {
        this.setConfigItem(RETAIN_SECONDS_CONFIG, String.valueOf(ticks));
    }

    /**
     * Gets the plot data retention seconds.
     * 
     * @return number of seconds to retain plot points
     */
    public int getDataRetentionSeconds() {
        final String tickStr = this.getConfigItem(RETAIN_SECONDS_CONFIG);
        if (tickStr != null) {
            return Integer.valueOf(tickStr);
        } else {
            return 3600;
        }
    }

    /**
     * Gets the time type used for data retention check.
     * 
     * @return RetentionTimeType
     */
    public RetentionTimeType getDataRetentionTimeType() {
        final String typeStr = this.getConfigItem(RETAIN_TIME_CONFIG);
        if (typeStr == null) {
            return RetentionTimeType.ReceiveTime;
        }
        final RetentionTimeType result = Enum.valueOf(RetentionTimeType.class,
                typeStr);
        return result;
    }

    /**
     * Sets the time type used for data retention check.
     * 
     * @param type
     *            the RetentionTimeType to set
     */
    public void setDataRetentionTimeType(final RetentionTimeType type) {
        this.setConfigItem(RETAIN_TIME_CONFIG, type.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initToDefaults() {
        super.initToDefaults(appContext.getBean(PerspectiveProperties.class).getViewProperties(ViewType.CHART),
                "jpl.gds.monitor.guiapp.gui.views.ChannelChartPageComposite",
                "jpl.gds.monitor.guiapp.gui.views.tab.ChartTabItem",
                "jpl.gds.monitor.guiapp.gui.views.preferences.ChannelChartPreferencesShell");
                
        setDataFont(new ChillFont(ChillFont.DEFAULT_FACE,
                ChillFont.FontSize.SMALL, ChillFont.DEFAULT_STYLE));
        setLegendFont(getDataFont());
        setLegendBackgroundColor(getBackgroundColor());
        setDomainChannels(new ChannelSet());
        setRangeChannels(new ChannelSet());
        final String format = viewProperties.getStringDefault(DOMAIN_TIME_FORMAT_CONFIG);
        setDomainTimeFormat(format);
        final String hourStr = viewProperties.getStringDefault(RETAIN_HOURS_CONFIG);
        if (hourStr != null) {
            setDataRetentionHours(Integer.valueOf(hourStr));
        }
        final String minStr =viewProperties.getStringDefault(RETAIN_MINUTES_CONFIG);
        if (minStr != null) {
            setDataRetentionMinutes(Integer.valueOf(minStr));
        }
        final String tickStr =viewProperties.getStringDefault(RETAIN_SECONDS_CONFIG);
        if (tickStr != null) {
            setDataRetentionSeconds(Integer.valueOf(tickStr));
        }

        setRealtimeRecordedFilterType(RealtimeRecordedFilterType.REALTIME);
    }
}
