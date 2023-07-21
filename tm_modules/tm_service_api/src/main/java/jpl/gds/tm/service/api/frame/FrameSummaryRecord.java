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
package jpl.gds.tm.service.api.frame;

import jpl.gds.shared.time.IAccurateDateTime;

/**
 * The FrameSummaryRecord is the record that stores the summary of the frames
 * processed.
 * 
 */
public class FrameSummaryRecord {
    private String theFrameType = "";
    private long    theSequenceCount;
    private IAccurateDateTime theErtTime;
    private long   theCount;
    private long   theVcidValue;

    /**
     * This creates an empty instance of a FrameSummaryRecord.
     */
    public FrameSummaryRecord() {
    }
    
    /**
     * This creates an instance of a FrameSummaryRecord and sets basic variables.
     * @param type The type to set.
     * @param vcfc The vcfc to set.
     * @param ert The ert to set.
     * @param vcid The vcid to set.
     * @param count The count to set.
     */
    public FrameSummaryRecord(final String type, final long vcfc, final IAccurateDateTime ert,
    		final long vcid, final long count) {
    	this.theFrameType = type;
    	this.theSequenceCount = vcfc;
    	this.theErtTime = ert;
    	this.theCount = count;
    	this.theVcidValue = vcid;
    }
   
    /**
     * This gets the current count.
     * @return The current count.
     */
    public long getCount() {
        return this.theCount;
    }

    /**
     * This sets the current count.
     * @param count The count to set.
     */
    public void setCount(long count) {
    	this.theCount = count;
    }
    /**
     * This returns the current sequence count.
     * @return The current sequence count.
     */
    public long getSequenceCount() {
        return this.theSequenceCount;
    }
     
    /**
     * This returns the vcid
     * @return The vcid value.
     */
    public long getVcid() {
        return this.theVcidValue;
    }

    /**
     * This returns the frame type.
     * @return The frame type.
     */
    public String getFrameType() {
        return this.theFrameType;
    }

    /**
     * This returns the last ert.
     * @return The last ert.
     */
    public IAccurateDateTime getLastErt() {
        return this.theErtTime;
    }

    /**
     * This returns the last ert as a string.
     * @return The last ert as a string.
     */
    public String getLastErtStr() {
    	return this.theErtTime.getFormattedErt(true);
    }
    
    /**
     * Sets the sequence count.
     * @param aSequenceCount The sequence count to set.
     */
    public void setSequenceCount ( final long aSequenceCount ) {
        this.theSequenceCount = aSequenceCount;
    }

    /**
     * Sets the last ert.
     * @param ert The last ert time to set.
     */
    public void setLastErt ( final IAccurateDateTime ert ) {
        this.theErtTime = ert;
    }

    /**
     * Sets the frame type.
     * @param newFrameType The new frame type to set.
     */
    public void setFrameType ( final String newFrameType ) {
        this.theFrameType = newFrameType;
    }

    /**
     * Sets the new vcid value.
     * @param newVcidValue The new vcid value to set.
     */
    public void setVcid ( final long newVcidValue ) {
        this.theVcidValue = newVcidValue;
    }
}
