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
package jpl.gds.tm.service.api.packet;

import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;

/**
 * PacketSummaryObject is used by the packet metering and packet extract summary message 
 * for tracking packets received by APID and virtual channel.
 * 
 */
public class PacketSummaryRecord {
    private String theApidName   = "";
    private IAccurateDateTime theScetTime;
    private ISclk theSclkTime;
    private IAccurateDateTime theErtTime;
    private ILocalSolarTime theSolTime;
    private long theCount;
    private long theApidId;
    private long theVcidValue;
    private long theSeqValue;
    /* MPCS-7376 - 6/2/15. Cache the sol flag */

    /**
     * Crates an empty PacketSummaryObject.
     */
    public PacketSummaryRecord() {}
    
    /**
     * Creates a populated PacketSummaryObject.
     * @param apid packet APID number
     * @param apidName packet APID name
     * @param scetTime packet SCET
     * @param sclkTime packet SCLK
     * @param ertTime packet ERT
     * @param solTime packet LST
     * @param vcidValue packet VCID
     * @param seqValue last packet sequence counter
     * @param count number of packets received
     */
    public PacketSummaryRecord(final long apid, final 
    		String apidName, final IAccurateDateTime scetTime, final ISclk sclkTime, final IAccurateDateTime ertTime, 
    		final ILocalSolarTime solTime, final long vcidValue, final long seqValue, final long count ) {
    	
        theApidId     = apid;
        theApidName   = apidName;
        theScetTime   = scetTime;
        theSclkTime   = sclkTime;
        theErtTime    = ertTime;
        theSolTime    = solTime;
        theVcidValue  = vcidValue;
        theSeqValue   = seqValue;
        theCount      = count;
    }
    
    /**
     * Updates this object with the given values.
     * 
     * @param byCount new count of packets received
     * @param sclk new packet SCLK
     * @param scet new packet SCET
     * @param ert new packet ERT
     * @param sol new packet LST
     * @param lastSeq last packet sequence number
     */
    public void increment(final long byCount, final ISclk sclk, final IAccurateDateTime scet, final IAccurateDateTime ert, final ILocalSolarTime sol, final long lastSeq) {
    	theCount += byCount;
    	setLastSclk(sclk);
    	setLastScet(scet);
    	setLastErt(ert);
        setLastLst(sol);
    	setSeqValue(lastSeq);
    }
    
    /**
     * Gets the count of packets received with the APID/VCID combination defined in 
     * this object.
     * 
     * @return packet count
     */
    public long getInstanceCount() {
        return theCount;
    }
    
    /**
     * Gets the count of packets received with the APID/VCID combination defined in 
     * this object.
     * 
     * @return packet count
     * 
     * @deprecated Use getInstanceCount()
     */
    @Deprecated
    public long getCount() {
        return theCount;
    }
    
    /**
     * Gets the count of packets received with the APID/VCID combination defined in 
     * this object as a string.
     * 
     * @return packet count as a string
     */
    public String getInstanceCountAsString() {
        return String.valueOf(theCount);
    }
    
    /**
     * Gets the count of packets received with the APID/VCID combination defined in 
     * this object as a string.
     * 
     * @return packet count as a string
     * 
     * @deprecated Use getInstanceCountAsString()
     */
    @Deprecated
    public String getCountAsString() {
        return String.valueOf(theCount);
    }
    
    /**
     * Gets the packet APID number.
     * 
     * @return apid number
     */
    public long getApid() {
        return theApidId;
    }
    
    /**
     * Gets the packet APID number as a string.
     * 
     * @return apid number as a string
     */
    public String getApidAsString() {
        return String.valueOf(theApidId);
    }
    
    /**
     * Gets the packet virtual channel ID.
     * 
     * @return the packet VCID
     */
    public long getVcid() {
        return theVcidValue;
    }
    
    /**
     * Gets the packet virtual channel ID as a string.
     * 
     * @return packet vcid as a string
     */
    public String getVcidAsString() {
        return String.valueOf(theVcidValue);
    }
    
    /**
     * Gets the last recorded packet sequence counter.
     * 
     * @return sequence count
     */
    public long getSeqCount() {
        return theSeqValue;
    }
    
    /**
     * Gets the last recorded packet sequence counter as a string.
     * 
     * @return sequence count as a string
     */
    public String getSeqCountAsString() {
        return String.valueOf(theSeqValue);
    }
    
    /**
     * Gets the packet APID name.
     * 
     * @return apid name text
     */
    public String getApidName() {
        return theApidName;
    }

    /**
     * Gets the last recorded packet SCET.
     * 
     * @return SCET value
     */
    public IAccurateDateTime getLastScet() {
        return theScetTime;
    }
    
    /**
     * Gets the last recorded packet SCET as a formatted string.
     * 
     * @return SCET string
     */
    public String getLastScetStr() {
    	final String result = theScetTime.getFormattedScet(true);
    	return result;
    }
    
    /**
     * Gets the last recorded packet LST.
     * 
     * @return LST value
     */
    public ILocalSolarTime getLastLst() {
    	return theSolTime;
    }
    
    /**
     * Gets the last recorded packet LST as a formatted string.
     * 
     * @return LST string
     */
    public String getLastLstStr() {
    	if (theSolTime == null) {
    		return null;
    	} else {
    		return theSolTime.getFormattedSol(true);
    	}
    }
    
    /**
     * Gets the last recorded packet SCLK.
     * 
     * @return SCLK value
     */
	public ISclk getLastSclk() {
        return theSclkTime;
    }
    
	 /**
     * Gets the last recorded packet SCLK as a formatted string.
     * 
     * @return LST string
     */
    public String getLastSclkStr() {
    	return theSclkTime.toString();
    }

    /**
     * Gets the last recorded packet ERT.
     * 
     * @return ERT value
     */
    public IAccurateDateTime getLastErt() {
        return theErtTime;
    }

    /**
     * Gets the last recorded packet ERT as a formatted string.
     * 
     * @return ERT string
     */
    public String getLastErtStr() {
    	return theErtTime.getFormattedErt(true);
    }
    
    /**
     * Sets the last recorded packet SCLK.
     * 
     * @param sclkTime SCLK value
     */
    public void setLastSclk ( final ISclk sclkTime ) {
        theSclkTime = sclkTime;
    }
    
    /**
     * Sets the last recorded packet SCET.
     * 
     * @param scetTime SCET value
     */
    public void setLastScet ( final IAccurateDateTime scetTime ) {
        theScetTime =  scetTime;
    }

    /**
     * Sets the last recorded packet ERT.
     * 
     * @param ertTime ERT value
     */
    public void setLastErt ( final IAccurateDateTime ertTime) {
        theErtTime = ertTime;
    }
    
    /**
     * Sets the last recorded packet LST.
     * 
     * @param solTime LST value
     */
    public void setLastLst ( final ILocalSolarTime solTime ) {
    	theSolTime = solTime;
    }
   
    /**
     * Sets the APID name.
     * 
     * @param newApidName the name to set
     */
    public void setApidName ( final String newApidName ) {
        theApidName = newApidName;
    }

    /**
     * Sets the APID number.
     * 
     * @param newApidId APID number to set
     */
    public void setApid ( final long newApidId ) {
        theApidId = newApidId;
    }

    /**
     * Sets the virtual channel ID
     * 
     * @param newVcidValue new VCID to set
     */
    public void setVcidValue ( final long newVcidValue ) {
        theVcidValue = newVcidValue;
    }
    
    /**
     * Sets the last recorded packet sequence number.
     * 
     * @param newSeqValue the new sequence count
     */
    public void setSeqValue ( final long newSeqValue ) {
        theSeqValue = newSeqValue;
    }
    
    /**
     * Sets the packet receive count.
     * 
     * @param newCount packet count to set
     */
    public void setInstanceCount(final long newCount) {
    	theCount = newCount;	
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj)
    {
    	if(!(obj instanceof PacketSummaryRecord)) {
            throw new IllegalArgumentException();
        } else
    	{
    		final PacketSummaryRecord arg = (PacketSummaryRecord) obj;
    
    		return arg.getApid() == theApidId &&
    			   arg.getApidName().equalsIgnoreCase(theApidName) &&
    			   arg.getLastScetStr().equalsIgnoreCase(this.getLastScetStr()) &&
    			   arg.getLastSclkStr().equalsIgnoreCase(this.getLastSclkStr()) &&
    			   arg.getLastErtStr().equalsIgnoreCase(this.getLastErtStr()) &&
    			   arg.getVcid() == theVcidValue &&
    			   arg.getSeqCount() == theSeqValue &&
    			   arg.getInstanceCount() == this.getInstanceCount();
    	}
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
    	return String.valueOf(getApid() + "/" + getVcid()).hashCode();
    }
}
