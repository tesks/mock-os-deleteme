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

/**
 * This class facilitates retrieval of properties for
 * SCMF-related tasks. This class is responsible
 * for handling and validating these options.
 * 
 *
 * MPCS-8822 - 05/23/19 - Changed to extend GdsHierarchicalProperties, completely overhauled to new design
 */
public final class ScmfProperties extends GdsHierarchicalProperties
{
    
	/** Default property file name */
	public static final String PROPERTY_FILE = "scmf.properties";
    
	private static final String PROPERTY_PREFIX = "scmf.";
	
	private static final String BIT_RATE_BLOCK = PROPERTY_PREFIX + "bitRate.";
	private static final String CHECKSUMS_BLOCK = PROPERTY_PREFIX + "checksums.";
	private static final String DICTIONARY_BLOCK = PROPERTY_PREFIX + "dictionary.";
	private static final String MACRO_BLOCK = PROPERTY_PREFIX + "macro.";
	private static final String RADIATION_BLOCK = PROPERTY_PREFIX + "radiation.";
	private static final String SEQTRAN_BLOCK = PROPERTY_PREFIX + "seqTran.";
	private static final String WRITE_BLOCK = PROPERTY_PREFIX + "write.";
	
	private static final String RADIATION_BIT_ONE_BLOCK = RADIATION_BLOCK + "bitOne.";
	private static final String RADIATION_START_BLOCK = RADIATION_BLOCK + "start.";
	private static final String RADIATION_WINDOW_BLOCK = RADIATION_BLOCK + "window.";
	private static final String RADIATION_WINDOW_CLOSE_BLOCK = RADIATION_WINDOW_BLOCK + "close."; 
	private static final String RADIATION_WINDOW_OPEN_BLOCK = RADIATION_WINDOW_BLOCK + "open.";
	
	private static final String TIME_PROPERTY = "time";
	private static final String VERSION_PROPERTY = "version";
	
	private static final String BIT_RATE_INDEX_PROPERTY = BIT_RATE_BLOCK + "index";
	private static final String BIT_RATE_STRICT_PROPERTY = BIT_RATE_BLOCK + "strict";
	
	private static final String DISABLE_CHECKSUMS_PROPERTY = CHECKSUMS_BLOCK + "disable";
	
	private static final String COMMENT_PROPERTY = PROPERTY_PREFIX + "comment";
	
	private static final String DICTIONARY_VALIDATE_PROPERTY = DICTIONARY_BLOCK + "validate";
	
	private static final String MACRO_VERSION_PROPERTY = MACRO_BLOCK + VERSION_PROPERTY;
	
	private static final String MESSAGE_COMMENT_PROPERTY = PROPERTY_PREFIX + "messageComment";
	private static final String SCMF_NAME_PROPERTY = PROPERTY_PREFIX + "name";
	private static final String PREPARER_PROPERTY = PROPERTY_PREFIX + "preparer";
	
	private static final String RADIATION_BIT_ONE_TIME_PROPERTY = RADIATION_BIT_ONE_BLOCK + TIME_PROPERTY;
	private static final String RADIATION_UNTIMED_PROPERTY = RADIATION_BLOCK + "untimed";
	private static final String RADIATION_START_TIME_PROPERTY = RADIATION_START_BLOCK + TIME_PROPERTY;
	private static final String RADIATION_WINDOW_CLOSE_TIME_PROPERTY = RADIATION_WINDOW_CLOSE_BLOCK + TIME_PROPERTY;
	private static final String RADIATION_WINDOW_OPEN_TIME_PROPERTY = RADIATION_WINDOW_OPEN_BLOCK + TIME_PROPERTY;
	
	private static final String REFERENCE_NUMBER_PROPERTY = PROPERTY_PREFIX + "referenceNumber";
	
	private static final String SEQTRAN_VERSION_PROPERTY = SEQTRAN_BLOCK + VERSION_PROPERTY;
	
	private static final String TITLE_PROPERTY = PROPERTY_PREFIX + "title";
	
	private static final String WRITE_SCMF_PROPERTY = PROPERTY_PREFIX + "write";
	private static final String WRITE_SCMF_ONLY_PROPERTY = WRITE_BLOCK + "only";
	
	
	private boolean dictionaryValidation = false;
	private boolean disableChecksums = false;
	private boolean onlyWriteScmf = false;
	private String scmfName;
	private boolean writeScmf = true;


    /**
     * Default constructor. Retrieves properties from "scmf.properties" file.
     * The standard AMPCS hierarchical property retrieval and declaration will
     * be utilized.
     * 
     * @param sseFlag
     *            The SSE context flag
     */
    public ScmfProperties(final SseContextFlag sseFlag) {
        super(PROPERTY_FILE, sseFlag);
    	resetConfiguration();
    }
    
	/**
	 * Return any configurable property values to their original value
	 */
    public void resetConfiguration(){
    	this.dictionaryValidation = this.getBooleanProperty(DICTIONARY_VALIDATE_PROPERTY, false);
    	this.disableChecksums = this.getBooleanProperty(DISABLE_CHECKSUMS_PROPERTY,false);
    	this.onlyWriteScmf = this.getBooleanProperty(WRITE_SCMF_ONLY_PROPERTY,false);
    	this.scmfName = this.getProperty(SCMF_NAME_PROPERTY);
    	this.writeScmf = this.getBooleanProperty(WRITE_SCMF_PROPERTY, true);
    }
	
    /**
	 * Gets the configured value for the SCMG "Bit Rate Index" field
	 * @return The configured value for the SCMF "Bit Rate Index" field
	 */
	public short getBitRateIndex(){
		return (short)this.getIntProperty(BIT_RATE_INDEX_PROPERTY, 1);
	}
	
	/**
	 * Gets the configured value for enforcement of the SCMF bit rate.
	 * 
	 * @return TRUE if the bit rate is enforced, FALSE if not
	 */
	public boolean isBitRateStrict(){
		return this.getBooleanProperty(BIT_RATE_STRICT_PROPERTY, true);
	}
	
	/**
	 * Returns the disableChecksums.
	 * @return Returns the disableChecksums.
	 */
	public boolean isDisableChecksums()
	{
		return this.disableChecksums;
	}
	
	
	/**
	 * Set the disable checksums property
	 * @param disableChecksums TRUE if checksums are to be disables, FALSE if not
	 */
	public void setDisableChecksums(final boolean disableChecksums){
		this.disableChecksums = disableChecksums;
	}
    
	/**
	 * Gets the configured value for the SCMF "Comment" field
	 * @return The configured value for the SCMF "Comment" field
	 */
	public String getComment(){
		return this.getProperty(COMMENT_PROPERTY, "MPCS generated SCMF file");
	}
	
	/**
	 * if validation is turned on, validate the command dictionary version from
	 * the SCMF file (the command dictionary that was used to generate the SCMF)
	 * against the current command dictionary version
	 * 
	 * @return TRUE if validation is to be performed, FALSE if not
	 */
	public boolean isDictionaryValidation(){
		return this.dictionaryValidation;
	}
	
	/**
	 * Enable or disable dictionary validation against the dictionary specified
	 * in SCMF files
	 * 
	 * @param dictionaryValidation
	 *            the new validation state
	 */
	public void setDictionaryValidation(final boolean dictionaryValidation){
		this.dictionaryValidation = dictionaryValidation;
	}
	
	/**
	 * Get the current macro version
	 * 
	 * @return the String representation of the current macro version
	 */
	public String getMacroVersion(){
		return this.getProperty(MACRO_VERSION_PROPERTY, "1.0");
	}
	

	/**
	 * Gets the configured value for the SCMF "Comment" field for individual messages
	 * @return The configured value for the SCMF "Comment" field for individual messages
	 */
	public String getMessageComment(){
		return this.getProperty(MESSAGE_COMMENT_PROPERTY, "MPCS Command Message");
	}
	
	/**
	 * Sets the scmfName.
	 * @return Returns the scmfName.
	 */
	public String getScmfName(){
		return this.scmfName;
	}
	
	/**
	 * Gets the scmfName
	 * @param scmfName the new scmfName
	 */
	public void setScmfName(final String scmfName){
		this.scmfName = scmfName;
	}
	
	/**
	 * Gets the configured value for the SCMF "Preparer" field.
	 * @return The configured value for the SCMF "Preparer" field
	 */
	public String getPreparer(){
		return this.getProperty(PREPARER_PROPERTY, "MPCS");
	}
	
	/**
	 * Gets the configured value for the SCMF "Bit One Radiation Time" field
	 * @return The configured value for the SCMF "Bit One Radiation Time" field
	 */
	public double getBitOneRadiationTime(){
		return this.getDoubleProperty(RADIATION_BIT_ONE_TIME_PROPERTY, -1.0);
	}

	/**
	 * Gets the configured value for the SCMF "untimed" field
	 * @return The configured value for the SCMF "untimed" field
	 */
	public int getUntimed() {
		return this.getIntProperty(RADIATION_UNTIMED_PROPERTY, 1);
	}
	
	/**
	 * Gets the configured value for the SCMF "Transmission Start Time" field
	 * @return The configured value for the SCMF "Transmission Start Time" field
	 */
	public String getTransmissionStartTime(){
		return this.getProperty(RADIATION_START_TIME_PROPERTY, "");
	}
	
	/**
	 * Gets the configured value for the SCMF "Close Window" field
	 * @return The configured value for the SCMF "Close Window" field
	 */
	public String getCloseWindow(){
		return this.getProperty(RADIATION_WINDOW_CLOSE_TIME_PROPERTY, "");
	}
	
	/**
	 * Gets the configured value for the SCMF "Open Window" field
	 * @return The configured value for the SCMF "Open Window" field
	 */
	public String getOpenWindow(){
		return this.getProperty(RADIATION_WINDOW_OPEN_TIME_PROPERTY, "");
	}
	
	/**
	 * Gets the configured value for the SCMF "Reference Number" field
	 * @return The configured value for the SCMF "Reference Number" field
	 */
	public long getReferenceNumber(){
		return this.getLongProperty(REFERENCE_NUMBER_PROPERTY, 12345);
	}
	
	/**
	 * gets the configured value for the SCMF "Seqtran Version" field
	 * @return The configured value for the SCMF "Seqtran Version" field
	 */
	public String getSeqtranVersion(){
		return this.getProperty(SEQTRAN_VERSION_PROPERTY, "1.0");
	}
	
	/**
	 * Gets the configured value for the SCMF "Title" field
	 * @return The configured value for the SCMF "Title" field
	 */
	public String getTitle(){
		return this.getProperty(TITLE_PROPERTY, "MPCS SCMF");
	}
	
	/**
	 * Gets the writeScmf.
	 * @return Returns the writeScmf.
	 */
	public boolean getWriteScmf(){
		return this.writeScmf;
	}
	
	/**
	 * Set the write SCMF property. If true, SCMFs will be written to disk
	 * 
	 * @param writeScmf
	 *            TRUE if SCMFs are to be written to disk, FALSE if not
	 */
	public void setWriteScmf(final boolean writeScmf){
		this.writeScmf = writeScmf;
	}
	
	/**
	 * Gets the onlyWriteScmf.
	 * @return Returns the onlyWriteScmf.
	 */
	public boolean getOnlyWriteScmf(){
		return this.onlyWriteScmf;
	}
	
	/**
	 * Set if SCMF files are to be written and radiation is not attempted
	 * 
	 * @param onlyWriteScmf
	 *            TRUE if only write SCMFs, false if radiation may be attempted
	 */
	public void setOnlyWriteScmf(final boolean onlyWriteScmf){
		this.onlyWriteScmf = onlyWriteScmf;
	}
	
	@Override
    public String getPropertyPrefix() {
	    return PROPERTY_PREFIX;
	}
}