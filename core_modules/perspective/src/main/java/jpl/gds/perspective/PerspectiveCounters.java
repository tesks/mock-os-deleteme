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
package jpl.gds.perspective;

import jpl.gds.perspective.config.PerspectiveProperties;
import jpl.gds.perspective.config.ViewType;
import jpl.gds.shared.log.TraceManager;


/**
 * PerspectiveCounters is a singleton object that tracks numbers of objects in
 * the current user perspective and can check those counts against configured
 * limits.
 * 
 *
 */
public class PerspectiveCounters {

    /**
     * The single instance of this class
     */
    //private static PerspectiveCounters instance;
    private final PerspectiveProperties perspectiveProps;

    private final int MAX_PLOT_VIEWS;
    private final int MAX_PLOT_TRACES;
    private final int MAX_CHANNEL_VIEWS;
    private final int MAX_EVR_VIEWS;
    private final int MAX_PRODUCT_VIEWS;
    private final int MAX_FIXED_VIEWS;
    private final int MAX_ALARM_VIEWS;
    private final int MAX_VIEWS;
    private final boolean VIEW_CHECK_ENABLED;


    private int numPlotViews;
    private int numTraces;
    private int numEvrViews;
    private int numChannelViews;
    private int numProductViews;
    private int numFixedViews;
    private int numAlarmViews;
    private int numViews;
    private int numOtherViews;

    /**
     * Constructor. Private for singleton pattern.
     */
    public PerspectiveCounters(PerspectiveProperties props) {
    	perspectiveProps = props;

    	MAX_PLOT_VIEWS = perspectiveProps.getMaxPlotViews();
    	MAX_PLOT_TRACES = perspectiveProps.getMaxPlotTraces();
    	MAX_CHANNEL_VIEWS = perspectiveProps.getMaxChannelViews();
    	MAX_EVR_VIEWS = perspectiveProps.getMaxEvrViews();
    	MAX_PRODUCT_VIEWS = perspectiveProps.getMaxProductViews();
    	MAX_FIXED_VIEWS = perspectiveProps.getMaxFixedViews();
    	MAX_ALARM_VIEWS = perspectiveProps.getMaxAlarmViews();
    	MAX_VIEWS = perspectiveProps.getMaxViews();
    	VIEW_CHECK_ENABLED = perspectiveProps.isViewCountCheckEnabled();
    }

//    /**
//     * Retrieves the single instance of this class.
//     * 
//     * @return PerspectiveCounters
//     */
//    public static PerspectiveCounters getInstance() {
//        if (instance == null) {
//            instance = new PerspectiveCounters();
//        }
//        return instance;
//    }

//    /**
//     * This method is part of a proper singleton class. It prevents using
//     * cloning as a hack around the singleton.
//     * 
//     * @return It never returns
//     * @throws CloneNotSupportedException
//     *             This function always throws this exception
//     */
//    @Override
//    public Object clone() throws CloneNotSupportedException {
//        throw new CloneNotSupportedException();
//    }

    /**
     * Updates counters for the given view type.
     * 
     * @param type
     *            the ViewType to increment counters for
     */
    public void incrementViewCount(final ViewType type) {
        this.numViews++;
        switch (type.getValueAsInt()) {
        case ViewType.CHANNEL_CHART_VIEW:
            this.numPlotViews++;
            break;
        case ViewType.CHANNEL_LIST_VIEW:
            this.numChannelViews++;
            break;
        case ViewType.FAST_ALARM_VIEW:
            this.numAlarmViews++;
            break;
        case ViewType.EVR_MESSAGE_VIEW:
            this.numEvrViews++;
            break;
        case ViewType.FIXED_LAYOUT_VIEW:
            this.numFixedViews++;
            break;
        case ViewType.PRODUCT_STATUS_VIEW:
            this.numProductViews++;
            break;
        default:
            this.numOtherViews++;
        }
    }

    /**
     * Adds the given number of plot traces to the current plot trace counter.
     * 
     * @param byTraces
     *            number of traces to add
     */
    public void incrementTraceCount(final int byTraces) {
        this.numTraces += byTraces;
    }

    /**
     * Validates the current counters against limits in the PerspectiveProperties.
     * Constructs a message indicating which limits are exceeded. Returns null
     * if perspective checking is disabled.
     * 
     * @return message indicating exceeded limits, or null if no limits exceeded
     */
    public String checkViewLimits() {

        if (!VIEW_CHECK_ENABLED) {
            return null;
        }
        final StringBuilder result = new StringBuilder();

        if (this.numViews > MAX_VIEWS) {
            result.append("Total number of views (" + this.numViews
                    + ") exceeds " + MAX_VIEWS + "\n");
        }
        if (this.numPlotViews > MAX_PLOT_VIEWS) {
            result.append("Total number of Channel Plot Views ("
                    + this.numPlotViews + ") exceeds " + MAX_PLOT_VIEWS + "\n");
        }
        if (this.numChannelViews > MAX_CHANNEL_VIEWS) {
            result.append("Total number of Channel List Views ("
                    + this.numChannelViews + ") exceeds " + MAX_CHANNEL_VIEWS
                    + "\n");
        }
        if (this.numEvrViews > MAX_EVR_VIEWS) {
            result.append("Total number of EVR Views (" + this.numEvrViews
                    + ") exceeds " + MAX_EVR_VIEWS + "\n");
        }
        if (this.numProductViews > MAX_PRODUCT_VIEWS) {
            result.append("Total number of Product Views ("
                    + this.numProductViews + ") exceeds " + MAX_PRODUCT_VIEWS
                    + "\n");
        }
        if (this.numFixedViews > MAX_FIXED_VIEWS) {
            result.append("Total number of Fixed Views (" + this.numFixedViews
                    + ") exceeds " + MAX_FIXED_VIEWS + "\n");
        }
        if (this.numAlarmViews > MAX_ALARM_VIEWS) {
            result.append("Total number of Alarm Views (" + this.numFixedViews
                    + ") exceeds " + MAX_ALARM_VIEWS + "\n");
        }
        if (this.numTraces > MAX_PLOT_TRACES) {
            result.append("Total number of channels plotted (" + this.numTraces
                    + ") exceeds " + MAX_PLOT_TRACES + "\n");
        }

        if (result.length() == 0) {
            return null;
        }
        return result.toString();
    }

    /**
     * Logs an information message about the current counters to the default
     * tracer.
     */
    public void logCounters() {
        /*  Can no longer log number of channels plotted. */
        TraceManager.getDefaultTracer().info(

                "Perspective contains " + this.numViews + " total views, "
                        + this.numPlotViews + " Channel Plots, "
                        + this.numChannelViews + " Channel Lists, "
                        + this.numFixedViews + " Fixed Views, "
                        + this.numEvrViews + " EVR Views, "
                        + this.numProductViews + " Product Status Views, "
                        + this.numAlarmViews + " Alarm Views, "
                        + this.numOtherViews + " Other types of views");
    }

    /**
     * Returns a copy of this object.
     * 
     * @return copy of the Singleton instance with all counters the same
     */
    public PerspectiveCounters getCopy() {
        final PerspectiveCounters result = new PerspectiveCounters(this.perspectiveProps);
        result.numAlarmViews = this.numAlarmViews;
        result.numChannelViews = this.numChannelViews;
        result.numEvrViews = this.numEvrViews;
        result.numFixedViews = this.numFixedViews;
        result.numPlotViews = this.numPlotViews;
        result.numProductViews = this.numProductViews;
        result.numTraces = this.numTraces;
        result.numViews = this.numViews;
        result.numOtherViews = this.numOtherViews;

        return result;
    }

    /**
     * Adds the counter values in the given PerspectiveCounters object to the
     * counters in this one.
     * 
     * @param newCounts
     *            the PerspectiveCpunters object containing counts to add
     */
    public void addCopy(final PerspectiveCounters newCounts) {
        this.numAlarmViews += newCounts.numAlarmViews;
        this.numChannelViews += newCounts.numChannelViews;
        this.numEvrViews += newCounts.numEvrViews;
        this.numFixedViews += newCounts.numFixedViews;
        this.numPlotViews += newCounts.numPlotViews;
        this.numProductViews += newCounts.numProductViews;
        this.numTraces += newCounts.numTraces;
        this.numViews += newCounts.numViews;
        this.numOtherViews += newCounts.numOtherViews;
    }

    /**
     * Resets all counters to 0.
     */
    public void reset() {
        this.numViews = 0;
        this.numPlotViews = 0;
        this.numChannelViews = 0;
        this.numFixedViews = 0;
        this.numEvrViews = 0;
        this.numProductViews = 0;
        this.numAlarmViews = 0;
        this.numTraces = 0;
        this.numOtherViews = 0;
    }
}
