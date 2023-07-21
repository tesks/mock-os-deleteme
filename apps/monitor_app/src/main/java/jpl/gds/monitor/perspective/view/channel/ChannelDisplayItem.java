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
package jpl.gds.monitor.perspective.view.channel;

import jpl.gds.shared.formatting.SprintfFormat;

/**
 * 
 * ChannelDisplayItem is used to store values from channel samples when they require
 * their own format string.
 */
public class ChannelDisplayItem
{
    private Object value;
    private String format;
    private String formattedValue;
     
    /**
     * Constructor. Note these items are pooled. Use the pool to get new objects rather
     * than this constructor. That's why it is protected.
     * 
     * @see ChannelDisplayItemPool
     */
    public ChannelDisplayItem() {}
    
    /**
     * Gets the default formatter for this value.
     * 
     * @return default format string (for sprintf or date format usage)
     */
    public String getFormat() {
		return format;
	}
    
    /**
     * Sets the default formatter for this value.
     * 
     * @param format default format string (for sprintf or date format usage) to set
     */
	public void setFormat(final String format) {
		this.format = format;
		formattedValue = null;
	}

    /**
     * 
     * Gets the value of this channel item as an unformatted string.
     * 
     * @return string value of this channel item.
     */
    public String getStringValue()
    {   
    	if (value == null) {
    		return "";
    	} else {
    		return value.toString();
    	}
    }
    
    /**
     * Gets the value of this channel item as an Object.
     * 
     * @return the Object value of this channel item
     */
    public Object getValue() {
        return value;
    }
    
    /**
     * Sets the value of this channel item as an Object.
     * 
     * @param value The value to set.
     */
    public void setValue(final Object value)
    {
        this.value = value;
        formattedValue = null;
    }
    
    /**
     * Returns the formatted object value, using the default formatter in this object.
     * This value is cached so that everyone does not have to pay the price for the formatting.
     * 
     * @return the formatted String value of this channel item for display
     */
    public String getFormattedValue(SprintfFormat formatUtil) {
    	if (format == null) {
    		return getStringValue();
    	} else if (value == null) {
    		return "";
    	} else if (formattedValue != null) { 
    		return formattedValue;
    	} else {
    	    try {
    	        formattedValue = formatUtil.anCsprintf(format, value);
    	        return formattedValue;
    	    } catch (Exception e) {
    	        e.printStackTrace();
    	        return getStringValue();
    	    }
    	}
    }
    
    /**
     * Returns the formatted object value, using the input formatter if non-null, else 
     * the default formatter in this object.
     * 
     * @param overrideFormat the string formatter to use before using the one in this object
     * @return the formatted String value of this channel item for display
     */
    public String getFormattedValue(SprintfFormat formatUtil, String overrideFormat) {
        if (overrideFormat == null) {
            overrideFormat = format;
        }
        if (overrideFormat == null || overrideFormat.trim().length() == 0) {
            return getStringValue();
        } else if (value == null) {
            return "";
        } else {
            try {
                return formatUtil.anCsprintf(overrideFormat, value);
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        }
    }
    
    /**
     * Clears all object members.
     */
    public void reset() {
    	value = null;
    	format = null;
    	formattedValue = null;
    }
}
