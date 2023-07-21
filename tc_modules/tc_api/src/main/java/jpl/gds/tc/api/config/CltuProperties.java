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
 * This class facilitates retrieval of uplink Command Link Transmission Unit (CLTU) telecommand frame related values
 * 
 */
public class CltuProperties extends GdsHierarchicalProperties {
	
	/** Default property file name */
	public static final String PROPERTY_FILE = "cltu.properties";
	
	private static final String PROPERTY_PREFIX = "cltu.";
	
	private static final String CODEBLOCK_BLOCK = PROPERTY_PREFIX + "codeBlock.";
	private static final String SEQUENCE_BLOCK = PROPERTY_PREFIX + "sequence.";
	private static final String START_SEQUENCE_BLOCK = SEQUENCE_BLOCK  + "start.";
	private static final String TAIL_SEQUENCE_BLOCK = SEQUENCE_BLOCK + "tail.";
	
	private static final String LENGTH_PROPERTY = "length";
	private static final String HEX_PROPERTY = "hex";
	
	private static final String CODEBLOCK_DATA_BIT_LENGTH_PROPERTY = CODEBLOCK_BLOCK + "data." + LENGTH_PROPERTY;
	private static final String CODEBLOCK_EDAC_BIT_LENGTH_PROPERTY = CODEBLOCK_BLOCK + "edac." + LENGTH_PROPERTY;
	private static final String CODEBLOCK_FILL_BIT_LENGTH_PROPERTY = CODEBLOCK_BLOCK + "fill." + LENGTH_PROPERTY;
	private static final String CODEBLOCK_TOTAL_BIT_LENGTH_PROPERTY = CODEBLOCK_BLOCK + "total." + LENGTH_PROPERTY;
	private static final String FILL_BYTE_HEX_PROPERTY = PROPERTY_PREFIX + "fill." + HEX_PROPERTY;
	private static final String FRAMES_PER_CLTU_PROPERTY = PROPERTY_PREFIX + "framesPer";
	private static final String MAX_BYTE_LENGTH_PROPERTY = PROPERTY_PREFIX + LENGTH_PROPERTY + ".max";
	private static final String START_SEQ_LONG_HEX_PROPERTY = START_SEQUENCE_BLOCK + HEX_PROPERTY + ".long"; 
	private static final String START_SEQ_SHORT_HEX_PROPERTY = START_SEQUENCE_BLOCK + HEX_PROPERTY + ".short";
	private static final String USE_LONG_START_SEQUENCE_PROPERTY = START_SEQUENCE_BLOCK + "useLong";
	private static final String TAIL_SEQ_HEX_PROPERTY = TAIL_SEQUENCE_BLOCK + HEX_PROPERTY;
	
	
	private boolean useLongStart = true;
	
	/**
     * Retrieves properties from "cltu.properties" file.
     * The standard AMPCS hierarchical property retrieval and declaration will
     * be utilized
     * 
     * @param sseFlag
     *            The SSE context flag
     */
    public CltuProperties(final SseContextFlag sseFlag) {
        super(PROPERTY_FILE, sseFlag);
		resetConfiguration();
	}
	
	/**
	 * Return any configurable property values to their original value
	 */
	public void resetConfiguration(){
		this.useLongStart = this.getBooleanProperty(USE_LONG_START_SEQUENCE_PROPERTY, true);
	}
	
	/**
	 * Returns the length, in bits, of the data section of a CLTU codeblock.
	 * <br> This value should be "byte divisible" (evenly divisible by 8)
	 * @return The length, in bits, of the data section of a CLTU codeblock.
	 */
	public synchronized int getCodeblockDataBitLength() {
		return this.getIntProperty(CODEBLOCK_DATA_BIT_LENGTH_PROPERTY,56);
	}

	/**
	 * Returns the length, in bytes, of the data section of a CLTU codeblock.
	 * @return The length, in bytes, of the data section of a CLTU codeblock.
	 */
	public synchronized  int getCodeBlockDataByteLength() {
		return (getCodeblockDataBitLength() / 8);
	}
	
	/**
	 * Returns the length, in bits, of the EDAC section of a CLTU codeblock.
	 * @return The length, in bits, of the EDAC section of a CLTU codeblock.
	 */
	public synchronized int getCodeblockEdacBitLength() {
		return this.getIntProperty(CODEBLOCK_EDAC_BIT_LENGTH_PROPERTY,7);
	}
	
	/**
	 * Returns the length, in bits, of the FILL section of a CLTU codeblock.
	 * @return The length, in bits, of the FILL section of a CLTU codeblock.
	 */
	public synchronized int getCodeblockFillBitLength() {
		return this.getIntProperty(CODEBLOCK_FILL_BIT_LENGTH_PROPERTY,1);
	}
	
	/**
	 * Returns the configured length, in bits, of a CTLU codeblock.
	 * <br> This value should be "byte divisible" (evenly divisible by 8)
	 * @return The configured length, in bits, of a CTLU codeblock. 
	 */
	public synchronized int getCodeblockBitLength() {
		return getCodeblockDataBitLength() + getCodeblockEdacBitLength() + getCodeblockFillBitLength();
	}

	/**
	 * Returns the configured length, in bytes, of a CLTU codeblock.
	 * @return The configured length, in bytes, of a CLTU codeblock.
	 */
	public synchronized  int getCodeblockByteLength() {
		return (getCodeblockBitLength() / 8);
	}
	
	/**
	 * Returns the fillByteHex.
	 * @return the fillByteHex.
	 */
	public synchronized String getFillByteHex()
	{
		return this.getProperty(FILL_BYTE_HEX_PROPERTY,"55");
	}

	/**
	 * Returns the framesPerCltu.
	 * @return the framesPerCltu.
	 */
	public synchronized short getFramesPerCltu()
	{
		return (short)this.getIntProperty(FRAMES_PER_CLTU_PROPERTY,1);
	}
	
	/**
	 * Returns the maxByteLength.
	 * @return the maxByteLength.
	 */
	public synchronized long getMaxByteLength()
	{
		return this.getLongProperty(MAX_BYTE_LENGTH_PROPERTY,1188);
	}
	
	/**
	 * Returns the startSequenceHex.
	 * @return the startSequenceHex.
	 */
	public synchronized String getStartSequenceHex()
	{
		if(this.getUseLongStartSequence())
    	{
    		return this.getProperty(START_SEQ_LONG_HEX_PROPERTY,"5555EB90");
    	}
    	else
    	{
    		return this.getProperty(START_SEQ_SHORT_HEX_PROPERTY,"EB90");
    	}
	}
	
	/**
	 * Returns the configured CLTU start sequence as a byte array.
	 * @return The configured CLTU start sequence as a byte array.
	 */
	public synchronized byte[] getStartSequence()
	{
		return(BinOctHexUtility.toBytesFromHex(getStartSequenceHex()));
	}
	
	/**
	 * Returns the useLongStartSequence. True is CLTUs should have long start
	 * sequences (usually 0x5555EB90), false otherwise (0xEB90).
	 * 
	 * TODO: This is an MSL-ism which I hate. 0x5555EB90 is not a "long start
	 * sequence". According to CCSDS, the start sequence is ALWAYS 0xEB90. What
	 * MSL is doing is actually using the PLOP specifications that allow the
	 * insertion of an idle sequence (0x5555 in this case) in between
	 * consecutive CLTUs. So this is really an idle sequence followed by a start
	 * sequence, not a long start sequence. *nerd rage* (brn)
	 * 
	 * @return the useLongStartSequence.
	 */
	public synchronized boolean getUseLongStartSequence()
	{
		return this.useLongStart;
	}
	
	/**
	 * gets the useLongStartSequence. True if CLTUs should have long start
	 * sequences (eg: 0x5555EB90), false if short start sequence (eg: 0xEB90).
	 * 
	 * @param useLongStart
	 *            the new useLongStart value
	 */
	public synchronized void setUseLongStartSequence(final boolean useLongStart){
		this.useLongStart = useLongStart;
	}
	
	/**
	 * Returns the tailSequenceHex.
	 * @return the tailSequenceHex.
	 */
	public synchronized String getTailSequenceHex()
	{
		return this.getProperty(TAIL_SEQ_HEX_PROPERTY,"C5C5C5C5C5C5C579");
	}
	
	/**
	 * Returns the configured CLTU tail sequence as a byte array.
	 * @return The configured CLTU tail sequence as a byte array.
	 */
	public synchronized byte[] getTailSequence()
	{
		return(BinOctHexUtility.toBytesFromHex(getTailSequenceHex()));
	}
	
	@Override
    public String getPropertyPrefix() {
	    return PROPERTY_PREFIX;
	}
}