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
package jpl.gds.product.api;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jpl.gds.shared.holders.SessionFragmentHolder;
import jpl.gds.shared.interfaces.ICsvSupport;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ICoarseFineTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.xml.stax.StaxSerializable;

/**
 * The read-only interface to be implemented by product metadata classes.
 * 
 *
 * @since R8
 */
public interface IProductMetadataProvider extends ICsvSupport, StaxSerializable {

    /**
     * Value that represents an unknown product type/APID name.
     */
    String UNKNOWN_PRODUCT_TYPE = "UNKNOWN";

    /**
     * Gets the virtual channel ID on which this product was received.
     * 
     * @return the virtual channel number
     */
    Integer getVcid();

    /**
     * Retrieves the spacecraft ID for this product.
     * 
     * @return the scid number
     */
    int getScid();

    /**
     * Retrieves the session ID for this data product. Set only upon query.
     * 
     * @return the session key as a long
     */
    Long getSessionId();

    /**
     * Get session fragment.
     *
     * @return Session fragment
     */
    SessionFragmentHolder getSessionFragment();

    /**
     * Return DSS (station) ID from the session that generated this product. Set only upon query.
     *
     * @return the session DSS ID, or null if none set
     */
    Integer getSessionDssId();

    /**
     * Returns the virtual channel ID from the session that generated this product. Set
     * only upon query.
     *
     * @return VCID from the Session, or null if none set
     */
    Integer getSessionVcid();

    /**
     * Retrieves the application process ID for this data product or part.
     * 
     * @return the APID 
     */
    int getApid();

    /**
     * Gets the Spacecraft Event Time for this data product or part.
     * 
     * @return the SCET time, or null if none set
     */
    IAccurateDateTime getScet();

    /**
     * Gets the local solar time for this data product or part.
     * 
     * @return the LST time, or null if none set
     */
    ILocalSolarTime getSol();

    /**
     * Gets the SCET exact (integer) time.
     * 
     * @return the SCET exact time as a long
     */
    long getScetExact();

    /**
     * Gets the LST exact (integer) time.
     * 
     * @return the LST exact time as a long
     */
    long getSolExact();

    /**
     * Gets the Spacecraft Event Time as an ISO-formatted string.
     * 
     * @return the SCET string, or the empty string if SCET not set
     */
    String getScetStr();

    /**
     * Gets the Record Creation Time as an ISO-formatted string.
     * 
     * @return the RCT string, or the empty string if RCT not set
     *
     */
    String getRctStr();

    /**
     * Gets the local solar time as an ISO-formatted string.
     * 
     * @return the LST string, or the empty string if LST not set
     */
    String getSolStr();

    /**
     * Gets the product ground creation time as an ISO-formatted string.
     * 
     * @return the time string, or the empty string if not time is set
     */
    String getProductCreationTimeStr();

    /**
     * Gets the product or part ERT as an ISO-formatted string.
     * 
     * @return the ERT time string, or the empty string if no ERT is set
     */
    String getErtStr();

    /**
     * Gets the ERT exact (integer) time of this product or part.
     * 
     * @return ERT exact time, or 0 if no ERT is set
     */
    long getErtExact();

    /**
     * Gets the ERT exact (integer) fine time of this product or part.
     * 
     * @return ERT exact fine time, or 0 if no ERT is set
     */
    long getErtExactFine();

    /**
     * Gets the Spacecraft Clock time of this product or part.
     * 
     * @return the SCLK time, or null if none set
     */
    ICoarseFineTime getSclk();

    /**
     * Gets the coarse Spacecraft Clock time of this product or part.
     * 
     * @return the SCLK coarse time or 0 if no SCLK is set
     */
    long getSclkCoarse();

    /**
     * Gets the fine Spacecraft Clock time of this product or part.
     * 
     * @return the SCLK fine time or 0 if no SCLK is set
     */
    long getSclkFine();

    /**
     * Gets the Spacecraft Clock time of this product or part as a GDR long
     * 
     * @return the SCLK GDR representation, or 0 if no SCLK is set
     */
    long getSclkExact();

    /**
     * Gets the Spacecraft Clock Time of this product or part as a formatted string.
     * 
     * @return the SCLK string, or the empty string if no SCLK is set
     */
    String getSclkStr();

    /**
     * Gets the name of the directory in which the product or part is stored
     * 
     * @return the storage directory
     */
    String getStorageDirectory();

    /**
     * Gets the full path to the product file without extension (suffix). If the
     * full path has been set via setFullPath() then that string is returned.
     * Otherwise, the full path is computed.
     * 
     * @return the file path, without extension
     */
    String getFullPath();

    /**
     * A Map of mission specific properties.  This will show up in the velocity
     * template context as an ArrayList called "names" for the keys and an ArrayList
     * called "values" for their corresponding values.  This way we can pass mission-specific
     * data to the templates in a mission-independent fashion. 
     *
     * @return A Map where the keys are property names and the values are property values
     */
    Map<String, String> getMissionProperties();

    /**
     * Gets the absolute data file name for the product, given its pre MPCS 5.4 database relative path.
     * 
     * @return absolute data file path
     */
    String getAbsoluteDataFile();

    /**
     * Gets the sequence ID identifying the onboard sequence that generated the
     * product. Interpretation of the sequence ID is mission-specific.
     * 
     * @return the sequence ID
     */
    int getSequenceId();

    /**
     * Gets the sequence version number that identifies which version of the
     * onboard sequence that generated the product.
     * 
     * @return the sequence version
     */
    int getSequenceVersion();

    /**
     * Retrieves the command number identifying the FSW command that generated
     * the product.
     * 
     * @return the command number
     */
    int getCommandNumber();

    /**
     * Gets the XML product definition version, which is used to identify which
     * definition in the DP dictionary should be used when interpreting the
     * product.
     * 
     * @return the version
     */
    int getXmlVersion();

    /**
     * Gets the coarse data validity in seconds.
     * 
     * @return the coarse DVT
     */
    long getDvtCoarse();

    /**
     * Gets the fine data validity time in subticks.
     * 
     * @return the fine DVT
     */
    long getDvtFine();

    /**
     * Gets the formatted data validity time, including both coarse and fine
     * portions.
     * 
     * @return the formatted DVT String
     */
    String getDvtString();

    /**
     * Gets the total number of parts in the product. This is the total expected
     * number of data parts.
     * 
     * @return the number of parts
     */
    int getTotalParts();

    /**
     * Returns the Product Part object for the last part added to this product
     * metadata.
     * 
     * @return the most recently added ProductPart
     */
    IProductPartProvider getLastPart();

    /**
     * Returns an iterator for the part list.
     * 
     * @return the Iterator for a list of IProductPartProvider objects
     */
    Iterator<IProductPartProvider> partIterator();

    /**
     * Returns the part list.
     * 
     * @return the Iterator for a list of IProductPartProvider objects
     */
    List<IProductPartProvider> getPartList();

    /**
     * Gets the time at which the product was created on the ground.
     * 
     * @return the product creation time, or null if none set
     */
    IAccurateDateTime getProductCreationTime();

    /**
     * Gets the time at which the record was created.
     * 
     * @return the RCT, or null if none set
     */
    IAccurateDateTime getRct();

    /**
     * Returns the partial product flag.
     * @return true if the data product is partial, false if complete
     */
    boolean isPartial();

    /**
     * Gets the name of the host running the session on which this product was generated.
     * Set only upon query.
     * 
     * @return session host name, or null if none set
     */
    String getSessionHost();

    /**
     * Gets the host ID
     *
     * @return session host ID
     */
    Integer getSessionHostId();

    /**
     * Retrieves the product filename with the product type directory prefix.
     * 
     * @return the filename
     */
    String getFilenameWithPrefix();

    /**
     * Retrieves the filename used for product or part storage, without product
     * type directory prefix.
     * 
     * @return the filename
     */
    String getFilename();

    /**
     * Retrieves the relative name of the sub-directory used for product or part
     * storage
     * 
     * @return the filename
     */
    String getDirectoryName();

    /**
     * Retrieves the command ID. The construction and interpretation of this
     * value is mission-specific; it is generally a composite of sequence id,
     * sequence version, and command number.
     * 
     * @return the command ID
     */
    long getCommandId();

    /**
     * Gets the ERT of the data product or part.
     * 
     * @return the ERT time, or null if none set.
     */
    IAccurateDateTime getErt();

    /**
     * Gets the ground status of the data product.
     * 
     * @return the ground status as an enum
     */
    ProductStatusType getGroundStatus();

    /**
     * Getter for the sequence category.
     *
     * @return sequence category text; may be null.
     */
    String getSequenceCategory();

    /**
     * Getter for sequence number.
     *
     * @return sequence number; may be null
     */
    Long getSequenceNumber();

    /**
     * Retrieves the product type text, or APID name.
     * 
     * @return the product type text; may be "UNKNOWN"
     */
    String getProductType();

    /**
     * Checks that the current product type is valid
     * 
     * @return true if the product type is not UNKNOWN_PRODUCT_TYPE, false if it is
     */
    boolean productTypeIsValid();

    /**
     * Retrieves the Dictionary Directory name found in the product EMD file. Not populated
     * during product generation, only during product decom.
     * @return the directory name
     */
    String getEmdDictionaryDir();

    /**
     * Retrieves the Dictionary Version found in the product EMD file. Not populated
     * during product generation, only during product decom.
     * @return the version string
     */
    String getEmdDictionaryVersion();
    
    /**
     * Retrieve the fsw build id field from the dictioanry.
     * @return the build id.
     */
    default long getFswVersion() {
    	return -1;
    };

    /**
     * Returns the flight software dictionary directory
     * @return dictionary directory as a string
     */
    String getFswDictionaryDir();

    /**
     * Returns the flight software dictionary version.
     * @return version as a string
     */
    String getFswDictionaryVersion();

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.xml.stax.StaxSerializable#toXml()
     */
    @Override
    String toXml();

    /**
     * Used by GetEverythingApp to get additional mission specific product data
     * @return a map from the name of the data to the data
     */
    Map<String, Object> getAdditionalMissionData();

    /**
     * Gets the product version string.
     * 
     * @return the ground product version, or null if none set  
     */
    String getProductVersion();

    /**
     * Convert Session.vcid to proper output form.
     *
     * @return Session.vcid in proper output form.
     */
    String getTransformedVcid();

    /**
     * Convert Session.vcid string to its integer value.
     *
     * @param str virtual channel string
     * @return Session.vcid in proper integer form.
     */
    // TODO Why is this dependency here? Use the VirtualChannelMapper directly!
    int getTransformedStringId(String str);

    /**
     * Returns the expected  product checksum, as sent in the flight data.
     * 
     * @return the checksum.
     */
    long getChecksum();

    /**
     * Returns the expected file size, in bytes, as sent in the flight data.
     * @return the size in bytes.
     */
    long getFileSize();

    /**
     * Returns the actual product checksum, as computed on the ground.
     * 
     * @return the checksum.
     */
    long getActualChecksum();

    /**
     * Returns the actual file size, in bytes, as measured on the ground.
     * @return the size in bytes.
     */
    long getActualFileSize();

	/**
	 * Used by GetEverythingApp. Returns a map of data to be displayed to
	 * various output files.
	 * 
	 * @param NO_DATA
	 *			is the string to be used to represent no data (ie " " or
	 *			"---").
	 * @return keyword/value map of metadata values
	 * @TODO R8 Refactor TODO - I cannot find anything using this!
	 */
	public Map<String, String> getFileData(final String NO_DATA);
}