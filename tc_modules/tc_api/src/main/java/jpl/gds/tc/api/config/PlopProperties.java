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
package jpl.gds.tc.api.config;

import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.util.BinOctHexUtility;

/**
 * This class facilitates the validation and retrieval of Physical Layer Operations Procedure (PLOP) related properties.
 * 
 *
 * MPCS-8822  05/23/17 - Changed to extend GdsHierarchicalProperties, completely overhauled to new design
 * 
 */
public final class PlopProperties extends GdsHierarchicalProperties
{

	/** Default property file name */
	public static final String PROPERTY_FILE = "plop.properties";
	
	private static final String PROPERTY_PREFIX = "plop.";
	
	private static final String SEQUENCE_BLOCK = PROPERTY_PREFIX + "sequence.";
	private static final String ACQ_SEQ_BLOCK = SEQUENCE_BLOCK + "acquisition.";
	private static final String IDLE_SEQ_BLOCK = SEQUENCE_BLOCK + "idle.";
	
	private static final String HEX_PROPERTY = "hex";
	private static final String LENGTH_PROPERTY = "length";
	
	private static final String ACQ_SEQ_LOCATION_PROPERTY = SEQUENCE_BLOCK + "acquisition";
	private static final String ACQ_SEQ_BYTE_HEX_PROPERTY = ACQ_SEQ_BLOCK + HEX_PROPERTY;	
	private static final String ACQ_SEQ_LENGTH_PROPERTY = ACQ_SEQ_BLOCK + LENGTH_PROPERTY;	
	
	private static final String IDLE_SEQ_LOCATION_PROPERTY = SEQUENCE_BLOCK + "idle";
	private static final String IDLE_SEQ_BYTE_HEX_PROPERTY = IDLE_SEQ_BLOCK + HEX_PROPERTY;
	private static final String IDLE_SEQ_LENGTH_PROPERTY = IDLE_SEQ_BLOCK + LENGTH_PROPERTY;
	
	private static final String TYPE_PROPERTY = PROPERTY_PREFIX + "type";
	
	private int acquisitionSequenceBitLength = 176;
	private SessionLocationType acquisitionSequenceLocation = SessionLocationType.FIRST;
	private int idleSequenceBitLength = 64;
	private SessionLocationType idleSequenceLocation = SessionLocationType.LAST;
	private PlopType plopType;
	
    
    /**
     * Default constructor. Retrieves properties from "plop.properties" file.
     * The standard AMPCS hierarchical property retrieval and declaration will
     * be utilized
     * 
     * @param sseFlag
     *            The SSE context flag
     */
    public PlopProperties(final SseContextFlag sseFlag) {
        super(PROPERTY_FILE, sseFlag);
    	resetConfiguration();
    }
    
	/**
	 * Return any configurable property values to their original value
	 */
    public void resetConfiguration(){
    	this.acquisitionSequenceBitLength = this.getIntProperty(ACQ_SEQ_LENGTH_PROPERTY, 176);
    	this.acquisitionSequenceLocation = SessionLocationType.valueOf(this.getProperty(ACQ_SEQ_LOCATION_PROPERTY,SessionLocationType.FIRST.toString()).toUpperCase());
    	this.idleSequenceBitLength = this.getIntProperty(IDLE_SEQ_LENGTH_PROPERTY,64);
    	this.idleSequenceLocation = SessionLocationType.valueOf(this.getProperty(IDLE_SEQ_LOCATION_PROPERTY,SessionLocationType.LAST.toString()).toUpperCase());
    	
    	
    	final String plopString = this.getProperty(TYPE_PROPERTY, PlopType.PLOP_2.toString());
    	
    	try
    	{
    		final int plopInt = Integer.parseInt(plopString);
    	    this.plopType = new PlopType(plopInt);
    	}
    	    catch(final NumberFormatException nfe)
    	{
    	    this.plopType = new PlopType(plopString);
        }
    }

	/**
	 * Returns the beginCommandLoad.
	 * 
	 * @return Returns the beginCommandLoad.
	 */
	public synchronized SessionLocationType getAcquisitionSequenceLocation()
	{
		return this.acquisitionSequenceLocation;
	}
	
	/**
	 * Set the acquisitionSequenceLocation
	 * 
	 * @param acquisitionSequenceLocation
	 *            the SessionLocationType corresponding to the new acquisition
	 *            sequence location
	 */
	public synchronized void setAcquisitionSequenceLocation(final SessionLocationType acquisitionSequenceLocation){
		this.acquisitionSequenceLocation = acquisitionSequenceLocation;
	}
	
	/**
	 * Accessor for one byte of the acquisition sequence.
	 * 
	 * @return The representation of one byte of the acquisition sequence as a hex string.
	 */
	public synchronized String getAcquisitionSequenceByteHex()
	{
		return this.getProperty(ACQ_SEQ_BYTE_HEX_PROPERTY,"55");
	}
	
	/**
	 * Returns the acquisitionSequenceBitLength.
	 * 
	 * @return Returns the acquisitionSequenceBitLength.
	 */
	public synchronized int getAcquisitionSequenceBitLength()
	{
		return this.acquisitionSequenceBitLength;
	}
	
	/**
	 * Set the acquisitionSequenceBitLength
	 * 
	 * @param acquisitionSequenceBitLength
	 *            the number of bits for the acquisition bit length
	 */
	public synchronized void setAcquisitionSequenceBitLength(final int acquisitionSequenceBitLength){
		this.acquisitionSequenceBitLength = acquisitionSequenceBitLength;
	}
    
	/**
	 * Returns the idleSequenceLocation.
	 * 
	 * @return Returns the idleSequenceLocation.
	 */
	public synchronized SessionLocationType getIdleSequenceLocation()
	{
		return this.idleSequenceLocation;
	}
	
	/**
	 * Set the idleSequenceLocaiton
	 * 
	 * @param idleSequenceLocation
	 */
	public synchronized void setIdleSequenceLocation(final SessionLocationType idleSequenceLocation){
		this.idleSequenceLocation = idleSequenceLocation;
	}
    
	/**
	 * Accessor for one byte of the idle sequence.
	 * 
	 * @return The representation of one byte of the idle sequence as a hex string.
	 */
	public synchronized String getIdleSequenceByteHex()
	{
		return this.getProperty(IDLE_SEQ_BYTE_HEX_PROPERTY,"55");
	}

	/**
	 * Returns the idleSequenceBitLength.
	 * 
	 * @return Returns the idleSequenceBitLength.
	 */
	public synchronized int getIdleSequenceBitLength()
	{
		return this.idleSequenceBitLength;
	}
	
	/**
	 * Sets the idleSequenceBitLength
	 * @param idleSequenceBitLength
	 */
	public synchronized void setIdleSequenceBitLength(final int idleSequenceBitLength){
		this.idleSequenceBitLength = idleSequenceBitLength;
	}

	/**
	 * Returns the type.
	 * @return Returns the type.
	 */
	public synchronized PlopType getType()
	{
		return this.plopType;
	}
	
	/**
	 * Set the plop type
	 * @param plopType the new plop type
	 */
	public synchronized void setType(final PlopType plopType){
		this.plopType = plopType;
	}
	
	/**
     * Get the configured acquisition sequence as a byte array.
     * 
     * @return A byte[] containing a single acquisition sequence.
     */
	public synchronized byte[] getAcquisitionSequence()
	{
		final int acqBitLength = getAcquisitionSequenceBitLength();
		final int acqByteLength = acqBitLength / 8;
        
		final byte acqSeqByte = BinOctHexUtility.toBytesFromHex(getAcquisitionSequenceByteHex())[0];
		final byte[] acqBytes = new byte[acqByteLength];
        for (int index = 0; index < acqBytes.length; index++)
        {
            acqBytes[index] = acqSeqByte;
        }
        
        return(acqBytes);
	}
	
	/**
     * Get the configured idle sequence as a byte array.
     * 
     * @return A byte[] containing a single idle sequence.
     */
    public synchronized byte[] getIdleSequence()
	{
    	final int idleBitLength = getIdleSequenceBitLength();
    	final int idleByteLength = idleBitLength / 8;
        
		final byte idleByte = BinOctHexUtility.toBytesFromHex(getIdleSequenceByteHex())[0];
		final byte[] idleBytes = new byte[idleByteLength];
        for (int index = 0; index < idleBytes.length; index++)
        {
            idleBytes[index] = idleByte;
        }
        
        return(idleBytes);
	}
    
    @Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }
}
