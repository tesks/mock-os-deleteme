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

package jpl.gds.monitor.guiapp.plot;

/**
 * Stores a single plot data point for monitor channel plots.
 *
 */
public class ChartDataPoint {
    private double yValue;
    private double xValue;
    private long   postTime;
       
    /**
     * Sets the timestamp on this chart data point, indicating the time at which it was posted.
     * 
     * @param millis timestamp as milliseconds
     */
    public void setPostTime(long millis) {
    	postTime = millis;
    }
    
    /**
     * Gets the timestamp on this chart data point, indicating the time at which it was posted.
     * 
     * @return timestamp as milliseconds
     */
    public long getPostTime() {
    	return postTime;
    }
    
    /**
     * Gets the X (domain) value for this data point.
     * 
     * @return the X value
     */
    public double getXValue()
    {
        return xValue;
    }
    
    /** 
     * Sets the X (domain) value for this data point.
     * 
     * @param val the X value to set
     */
    public void setXValue(double val)
    {
        this.xValue = val;
    }
    
    /**
     * Sets the Y (range) value for this data point.
     * @param value the value to set
     */
    public void setYValue(double value)
    {
    	this.yValue = value;
    }

    /**
     * Gets the Y (range) value for this data point.
     * 
     * @return the Y value
     */
    public double getYValue()
    {
        return yValue;
    }
    
    /**
     * Zeros out the members of this data point.
     */
    public void reset() {
    	xValue = 0.0;
    	yValue = 0.0;
    	postTime = 0;
    }
}