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

import java.util.ArrayList;

/**
 * ChartDataSeries holds the entire list of data points for a single plot in chill_monitor.
 *
 */
public class ChartDataSeries {
    
    private String name;
    private final ArrayList<ChartDataPoint> seriesData = new ArrayList<ChartDataPoint>(1000);
    private long timeToLive    = Long.MAX_VALUE;
    private long maxDataPoints = Long.MAX_VALUE;

    
    /**
     * Gets the maximum data points kept by this data series.
     * 
     * @return maximum number of data points
     */
    public synchronized long getMaxDataPoints() {
		return maxDataPoints;
	}

    /**
     * Sets the maximum data points kept by this data series.
     * 
     * @param maxDataPoints number of data points
     */
	public synchronized void setMaxDataPoints(long maxDataPoints) {
		this.maxDataPoints = maxDataPoints;
		expireData();
	}

	/**
	 * Sets a new time to live (expiration time) for plot points in this data series.
	 * 
	 * @param millis new expiration time in milliseconds
	 */
	public synchronized void setTimeToLive(long millis) {
    	this.timeToLive = millis;
    	expireData();
    }
    
	/**
	 * Retrieves the number of points currently in the data series.
	 * 
	 * @return number of data points
	 */
    public synchronized int numItems() {
        return seriesData.size();
    }


    /**
     * Adds a new data point to the series and then expires old data
     * if required.
     * 
     * @param point the new ChartDataPoint to add 
     */
    public synchronized void addDataPoint(ChartDataPoint point) {
        
        this.seriesData.add(point);

        if ((this.seriesData.size() > this.maxDataPoints) ||
            (this.timeToLive < Long.MAX_VALUE))
        {
        	expireData();
        }
    }


    /**
     * Clears all the data points in this series.
     */
    public synchronized  void clearDataPoints() {
        this.seriesData.clear();
    }
    
    /**
     * Gets the data point at the specified index.
     * 
     * @param num the index of the data point to get (starting at 0)
     * @return the ChartDataPoint, or null if none found at the given index
     */
    public synchronized ChartDataPoint getDataPoint(int num) {
    	if (this.seriesData.size() == 0 || this.seriesData.size() <= num) {
    		return null;
    	}
        ChartDataPoint point = this.seriesData.get(num);
        return point;
    }
    
    /**
     * Gets the latest data point added to this series.
     * 
     * @return latest data point, or null if no data points exist
     */
    public synchronized ChartDataPoint getLatestDataPoint() {
    	if (this.seriesData.isEmpty()) {
    		return null;
    	} else {
    		return this.seriesData.get(this.seriesData.size() - 1);
    	}
    }
    
    /**
     * Expires data from the data series. Expiration is based upon two factors: maximum
     * data points allowed, and the user-specified retention time. If either is exceeded,
     * oldest data points are removed.
     */
    private synchronized void expireData() {
        if (this.seriesData.size() == 0) {
            return;
        }
        
        // If too many data points, remove the oldest 25%
        if (this.seriesData.size() > this.maxDataPoints) {
        	while (this.seriesData.size() > ((this.maxDataPoints / 4.0) * 3.0)) {
        		seriesData.remove(0);
        	}
        }
        
        // Remove data points that have exceeded the configured retention time
        /*Don't attempt to remove points if
         * seriesData is empty */
        if(seriesData.size() > 0) {
	        long latest = this.seriesData.get(this.seriesData.size() - 1).getPostTime();        
	        ArrayList<ChartDataPoint> removeList = new ArrayList<ChartDataPoint>();
	        
	        for (ChartDataPoint point : this.seriesData) {
	        	long pointTime = point.getPostTime();
	        	long elapsed = latest - pointTime;
	        	if (elapsed > timeToLive) {
	               removeList.add(point);		
	        	} else {
	        		break;
	        	}
	        }
	        
	        for (ChartDataPoint point : removeList) {
	        	seriesData.remove(point);
	        }
        }
    }

    /**
     * Sets the name of this data series. Needed by JFreechart.
     * 
     * @param name name to set
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Gets the name of this data series.
     * 
     * @return name string
     */
    public String getName()
    {  
        return this.name;
    }
    
    /**
     * Gets the Y (range) value of the data series at the given index (starting with 0).
     * 
     * @param index index of the data point to look for
     * 
     * @return data point Y value at the given index, or 0.0 if none found.
     */
    public synchronized double getYValue(int index)
    {
        ChartDataPoint point = getDataPoint(index);
        if (point == null) {
        	return 0.0;
        }
         return point.getYValue();
    }

    /**
     * Gets the X (domain) value of the data series at the given index (starting with 0).
     * 
     * @param index index of the data point to look for
     * 
     * @return data point X value at the given index, or 0.0 if none found.
     */
    public synchronized double getXValue(int item)
    {
        ChartDataPoint point = getDataPoint(item);
        if (point == null) {
        	return 0.0;
        }
        return point.getXValue();
    }
}
