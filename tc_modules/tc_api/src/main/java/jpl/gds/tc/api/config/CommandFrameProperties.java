/*
 * Copyright 2006-2019. California Institute of Technology.
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
import jpl.gds.tc.api.frame.FrameErrorControlFieldAlgorithm;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * This class facilities validation and retrieval of 
 * telecommand frame related properties.
 * 
 *
 * MPCS-8822 - 05/23/17 - Changed to extend GdsHierarchicalProperties, completely overhauled to new design
 * MPCS-10928  - 07/17/19 - Added default FECF type and type per virtual channel number. Added validation to
 *     validation for FECF byte length
 * MPCS-11285 - 09/24/19 - added getStringIdVcidValue() - uses currently configured string ID
 */
public final class CommandFrameProperties extends GdsHierarchicalProperties
{
	
	/** Default property file name */
	public static final String PROPERTY_FILE = "command_frame.properties";
	
	private static final String PROPERTY_PREFIX = "commandFrame.";
	
	private static final String FRAME_DATA_BLOCK = PROPERTY_PREFIX + "data.";

	private static final String FRAME_DELIMITER_BLOCK = FRAME_DATA_BLOCK + "Delimiter.";
	private static final String EXECUTION_STRING_BLOCK = PROPERTY_PREFIX + "executionString.";
	private static final String RANDOMIZATION_BLOCK = PROPERTY_PREFIX + "randomization.";
	private static final String FECF_BLOCK = PROPERTY_PREFIX + "fecf.";

	private static final String TYPE_PROPERTY = ".type";
	
	private static final String FRAME_DELIMITER_BEGIN_HEX_PROPERTY = FRAME_DELIMITER_BLOCK + "beginHex";
	private static final String FRAME_DELIMITER_END_HEX_PROPERTY = FRAME_DELIMITER_BLOCK + "endHex";
	private static final String MAX_FILE_LOAD_BYTE_SIZE_PROPERTY = FRAME_DATA_BLOCK + "maxFileLoadByteSize";
	private static final String FRAME_EXECUTION_STRING_ID_PROPERTY = EXECUTION_STRING_BLOCK + "stringId";
	private static final String FRAME_FECF_LENGTH_PROPERTY = FECF_BLOCK + "bytes";
	private static final String FRAME_FECF_DEFAULT_TYPE_PROPERTY = FECF_BLOCK + "default" + TYPE_PROPERTY;
	private static final String FRAME_MAX_BYTE_LENGTH_PROPERTY = PROPERTY_PREFIX + "maxBytes";
	private static final String FRAME_MAX_DATA_BYTE_LENGTH_PROPERTY = PROPERTY_PREFIX + "maxDataBytes";
	private static final String FRAME_RANDOMIZATION_ALGORITHM_PROPERTY = RANDOMIZATION_BLOCK + "algorithm";
	private static final String FRAME_USE_RANDOMIZATION_PROPERTY = RANDOMIZATION_BLOCK + "use";
	private static final String FRAME_SESSION_REPEAT_COUNT_PROPERTY = PROPERTY_PREFIX + "sessionRepeat";
	private static final String FRAME_VERSION_PROPERTY = PROPERTY_PREFIX + "version";
	
	private static final String BEGIN_SESSION_PROPERTY = ".beginSession";
	private static final String END_SESSION_PROPERTY = ".endSession";
	private static final String HAS_FECF_PROPERTY = ".hasFECF";
	private static final String HAS_SEQUENCE_COUNTER_PROPERTY = ".hasSequenceCounter";
	private static final String VIRTUAL_CHANNEL_NUMBER_PROPERTY = ".virtualChannelNumber";
	
	/** The minimum allowable value for the virtual channel number */
    public static final long VIRTUAL_CHANNEL_NUMBER_MIN_VALUE = 0;
    /** The maximum allowable value for the virtual channel number */
    public static final long VIRTUAL_CHANNEL_NUMBER_MAX_VALUE = 7;
	
	
	private IPseudoRandomizerAlgorithm randomizer;
	private Byte                       overrideVirtualChannelNumber = null;
	private SessionLocationType        overrideBeginType;
	private SessionLocationType        overrideEndType;
	private int                        repeatCount                  = -1;
	private String                     DEFAULT_STRING_ID_VALUE      = "A";
	private String                     stringId                     = DEFAULT_STRING_ID_VALUE;
	
	/**
     * Retrieves properties from "frame.properties" file.
     * The standard AMPCS hierarchical property retrieval and declaration will
     * be utilized
     * 
     * @param sseFlag
     *            The SSE context flag
     * 
     */
    public CommandFrameProperties(final SseContextFlag sseFlag) {
        super(PROPERTY_FILE, sseFlag);
    	resetConfiguration();
    }
    
    /**
	 * Return any configurable property values to their original value
	 */
    public void resetConfiguration(){
    	this.overrideBeginType = null;
    	this.overrideEndType = null;
    	this.overrideVirtualChannelNumber = null;
    	this.repeatCount = this.getIntProperty(FRAME_SESSION_REPEAT_COUNT_PROPERTY,0);
    	this.stringId = this.getProperty(FRAME_EXECUTION_STRING_ID_PROPERTY, DEFAULT_STRING_ID_VALUE);
    }

	/**
	 * Get the hex data to be placed in the leading delimiter frame
	 * 
	 * @return String of the hex to be placed in the leading delimiter frame
	 */
    public String getBeginDataHex() {
    	return this.getProperty(FRAME_DELIMITER_BEGIN_HEX_PROPERTY, "0000");
    }
    
	/**
	 * Get the hex data to be placed in the trailing delimiter frame
	 * 
	 * @return String of the hex to be placed in the trailing delimiter frame
	 */
    public String getEndDataHex() {
    	return this.getProperty(FRAME_DELIMITER_END_HEX_PROPERTY, "0000");
    }
    
    
	/**
	 * Gets max file load bite size.
	 * 
	 * @return the max file load bite size.
	 */
	public synchronized int getMaxFileLoadByteSize()
	{
		return this.getIntProperty(MAX_FILE_LOAD_BYTE_SIZE_PROPERTY,260220);
	}
    
	/**
	 * Get when leading delimiter frames should be inserted for the specified
	 * virtual channel type.
	 * 
	 * @param vct
	 *            the VirtualChannelType being queried
	 * @return The SessionLocaitonType enumeration corresponding to the
	 *         placement of leading delimiter frames
	 */
    public SessionLocationType getBeginSession(final VirtualChannelType vct){
    	return SessionLocationType.valueOf(this.getProperty(FRAME_DATA_BLOCK + vct.toString() + BEGIN_SESSION_PROPERTY, "ALL").toUpperCase());
    }
    
	/**
	 * Get when trailing delimiter frames should be inserted for the specified
	 * virtual channel type.
	 * 
	 * @param vct
	 *            the VirtualChannelType being queried
	 * @return The SessionLocaitonType enumeration corresponding to the
	 *         placement of trailing delimiter frames
	 */
    public SessionLocationType getEndSession(final VirtualChannelType vct){
    	return SessionLocationType.valueOf(this.getProperty(FRAME_DATA_BLOCK + vct.toString() + END_SESSION_PROPERTY, "ALL").toUpperCase());
    }
    
	/**
	 * Get if the specified virtual channel type frames contain a forward error
	 * correction field
	 * 
	 * @param vct
	 *            the VirtualChannelType being queried
	 * @return TRUE if an FECF is to be included, FALSE if not
	 */
    public boolean hasFecf(final VirtualChannelType vct){
    	return this.getBooleanProperty(FRAME_DATA_BLOCK + vct.toString() + HAS_FECF_PROPERTY, false);
    }
    
    /**
	 * Get if the specified virtual channel type frames use a sequence counter
	 * 
	 * @param vct
	 *            the VirtualChannelType being queried
	 * @return TRUE if a sequence counter is to be used, FALSE if not
	 */
    public boolean hasSequenceCounter(final VirtualChannelType vct){
    	return this.getBooleanProperty(FRAME_DATA_BLOCK + vct.toString() + HAS_SEQUENCE_COUNTER_PROPERTY, true);
    }
	
	/**
	 * Get the virtual channel number for a Virtual ChannelType
	 * 
	 * @param vct
	 *            the VirtualChannelType being queried
	 * @return a byte containing the numeric channel number
	 */
	public byte getVirtualChannelNumber(final VirtualChannelType vct){
		return (byte)this.getIntProperty(FRAME_DATA_BLOCK + vct.toString() + VIRTUAL_CHANNEL_NUMBER_PROPERTY, 5);
	}

	/**
	 * Get the type of algorithm that should be utilized to calculate the TC frame FECF for a given VirtualChannelType
	 * @param vct the VirtualChannelType that needs to know what type of FECF algorithm it will use
	 * @return the enumeration indicating which algorithm should be used
	 */
	public FrameErrorControlFieldAlgorithm getChecksumCalcluator(final VirtualChannelType vct) {
		return getChecksumCalcluator(getVirtualChannelNumber(vct));
	}

	/**
	 * Get the type of checksum to be used on a particular virtual channel number
	 * @param vc the virtual channel number that will have a checksum calculated
	 * @return the UplinkChecksumType
	 */
	public FrameErrorControlFieldAlgorithm getChecksumCalcluator(final byte vc) {
		String val = this.getProperty(FECF_BLOCK + vc + TYPE_PROPERTY);

		try {
			return FrameErrorControlFieldAlgorithm.getAlgorithmFromPropertyValue(val);
		} catch (IllegalArgumentException | NullPointerException e) {
			return getDefaultChecksumCalculator();
		}
	}

	/**
	 * Get the default type of checksum to be used on all virtual channels if no other type
	 * is specified
	 * @return the UplinkChecksumType
	 */
	public FrameErrorControlFieldAlgorithm getDefaultChecksumCalculator() {
		String val = this.getProperty(FRAME_FECF_DEFAULT_TYPE_PROPERTY, FrameErrorControlFieldAlgorithm.EACSUM55AA.getSimpleName());

		try {
			return FrameErrorControlFieldAlgorithm.getAlgorithmFromPropertyValue(val);
		} catch (IllegalArgumentException e) {
			return FrameErrorControlFieldAlgorithm.EACSUM55AA;
		}
	}

	/**
	 * Accessor for the String ID
	 *
	 * @return The String ID
	 */
	public synchronized String getStringId() {
		return this.stringId;
	}
    
	/**
	 * Get the String ID
	 * 
	 * @param stringId
	 *            the String ID
	 */
    public synchronized void setStringId(final String stringId){
    	this.stringId = stringId;
    }
    
    /**
     * Get the execution string value for a specified string ID
     * @param stringId the string ID in question
     * @return the byte numeric execution string value of the supplied string ID
     */
    public byte getStringIdVcidValue(final String stringId){
    	return (byte)this.getIntProperty(EXECUTION_STRING_BLOCK + stringId, 0);
    }

	/**
	 * Get the execution string value for the current string ID
	 *
	 * @return the byte numeric execution string value for the current string ID
	 */
	public byte getStringIdVcidValue() {
    	return getStringIdVcidValue(getStringId());
	}
	
	/**
	 * Returns the length of the FECF in a TC transfer frame.
	 * @return Returns the FECF length.
	 */
	public synchronized int getFecfLength()
	{
		int fecf = this.getIntProperty(FRAME_FECF_LENGTH_PROPERTY,2);

		if(fecf > 4) {
			fecf = 8;
		} else if (fecf > 2) {
			fecf = 4;
		} else {
			fecf = 2;
		}

		return fecf;
	}
	
	/**
	 * Returns the maxByteLength.
	 * @return the maxByteLength.
	 */
	public synchronized int getMaxByteLength()
	{
		return this.getIntProperty(FRAME_MAX_BYTE_LENGTH_PROPERTY,1024);
	}

	/**
	 * Returns the maxByteLength.
	 * @return Returns the maxByteLength.
	 */
	public synchronized int getMaxDataByteLength()
	{
		return this.getIntProperty(FRAME_MAX_DATA_BYTE_LENGTH_PROPERTY,1024);
	}
	
	/**
	 * Get the max frame data length in bytes for the virtual channel in question
	 * @param virtualChannelType the VirtualChannelType being queried
	 * @return the integer number of bytes that each frame of the specified virtual channel can contain
	 */
	public int getMaxFrameDataLength(final VirtualChannelType virtualChannelType){
		if(hasFecf(virtualChannelType)){
			return (getMaxDataByteLength() - getFecfLength());
		}
		
		return getMaxDataByteLength();
	}


	/**
	 * Returns the randomizer.
	 * @return the randomizer.
	 */
	public IPseudoRandomizerAlgorithm getRandomizer()
	{
		if(this.randomizer != null)
		{
			return this.randomizer;
		}

		String randomizerClassStr = null;
		boolean doRandomization = false;
        try
        {
        	doRandomization = isDoRandomization();
        	randomizerClassStr = this.getProperty(FRAME_RANDOMIZATION_ALGORITHM_PROPERTY,"jpl.gds.command.frame.CcsdsPseudoRandomizerAlgorithm");
            final Class<?> randomizerClass = Class.forName(randomizerClassStr);
            final Constructor<?> constructor = randomizerClass.getConstructor(new Class<?>[] {});
            this.randomizer = (IPseudoRandomizerAlgorithm)constructor.newInstance(new Object[] {});
        }
		catch(final ClassNotFoundException e)
		{
			if(doRandomization == true)
			{
				throw new IllegalStateException("Could not find configured uplink pseudo randomizer class \"" + randomizerClassStr + "\": " + e.getMessage());
			}
		}
		catch(final NoSuchMethodException e)
		{
			if(doRandomization == true)
			{
				throw new IllegalStateException("Could not find no-argument constructor for configured uplink pseudo randomizer class \"" + randomizerClassStr + "\": " + e.getMessage());
			}
		}
        catch(SecurityException | IllegalArgumentException | InstantiationException | IllegalAccessException | InvocationTargetException e)
		{
			if(doRandomization == true)
			{
				throw new IllegalStateException("Could not instantiate configured uplink pseudo randomizer class \"" + randomizerClassStr + "\": " + e.getMessage());
			}
		}

		catch(final ClassCastException e)
		{
			if(doRandomization == true)
			{
				throw new IllegalStateException("Configured uplink pseudo randomizer class \"" + randomizerClassStr +
						"\" does not implement the require interface \"" + IPseudoRandomizerAlgorithm.class.getName() + "\"");
			}
		}

		return this.randomizer ;
	}
	
	/**
	 * Returns the doRandomization.
	 * @return Returns the doRandomization.
	 */
	public boolean isDoRandomization()
	{
		return this.getBooleanProperty(FRAME_USE_RANDOMIZATION_PROPERTY, false);
	}

	/**
	 * Returns the sessionRepeatCount.
	 * @return the sessionRepeatCount.
	 */
	public int getSessionRepeatCount()
	{
		return this.repeatCount;
	}
	
	/**
	 * Set the sessionRepeatCount
	 * @param repeatCount the integer number of times to repeat the session
	 */
	public void setSessionRepeatCount(final int repeatCount){
		this.repeatCount = repeatCount;
	}
	
	/**
	 * Returns the versionNumber.
	 * @return the versionNumber.
	 */
	public synchronized byte getVersionNumber()
	{
		return (byte)this.getIntProperty(FRAME_VERSION_PROPERTY,0x00);
	}
	
	/**
	 * Get the overridden value for the VC number (if there is one).
	 * 
	 * @return Get the overridden virtual channel number or null if it
	 * hasn't been set.
	 */
	public synchronized Byte getOverrideVirtualChannelNumber()
	{
		return this.overrideVirtualChannelNumber;
	}

	/**
	 * Set the override virtual channel number for fault injection.
	 * 
	 * @param overrideVirtualChannelNumber The virtual channel number frames should use instead of their normal one
	 */
	public synchronized void setOverrideVirtualChannelNumber(final Byte overrideVirtualChannelNumber)
	{
		if(overrideVirtualChannelNumber != null &&
		   (overrideVirtualChannelNumber.byteValue() < VIRTUAL_CHANNEL_NUMBER_MIN_VALUE ||
		   overrideVirtualChannelNumber.byteValue() > VIRTUAL_CHANNEL_NUMBER_MAX_VALUE))
		{
			throw new IllegalArgumentException("Invalid virtual channel number \"" + overrideVirtualChannelNumber.byteValue() + "\". The virtual channel number" +
					" must be between " + VIRTUAL_CHANNEL_NUMBER_MIN_VALUE + " and " + VIRTUAL_CHANNEL_NUMBER_MAX_VALUE + ".");
		}
		
		this.overrideVirtualChannelNumber = overrideVirtualChannelNumber;
	}
	
	/**
	 * Returns the overrideBeginType.
	 * @return the overrideBeginType.
	 */
	public SessionLocationType getOverrideBeginType()
	{
		return this.overrideBeginType;
	}

	/**
	 * Sets the overrideBeginType
	 *
	 * @param overrideBeginType The overrideBeginType to set.
	 */
	public void setOverrideBeginType(final SessionLocationType overrideBeginType)
	{
		this.overrideBeginType = overrideBeginType;
	}

	/**
	 * Returns the overrideEndType.
	 * @return the overrideEndType.
	 */
	public SessionLocationType getOverrideEndType()
	{
		return this.overrideEndType;
	}

	/**
	 * Sets the overrideEndType
	 *
	 * @param overrideEndType The overrideEndType to set.
	 */
	public void setOverrideEndType(final SessionLocationType overrideEndType)
	{
		this.overrideEndType = overrideEndType;
	}
	
	@Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }	
}
