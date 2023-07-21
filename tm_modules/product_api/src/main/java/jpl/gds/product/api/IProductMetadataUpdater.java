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

import java.util.Map;

import org.xml.sax.SAXException;

import jpl.gds.shared.holders.SessionFragmentHolder;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ICoarseFineTime;
import jpl.gds.shared.time.ILocalSolarTime;

/**
 * The write interface to be implemented by product metadata classes.
 * 
 *
 * @since R8
 */
public interface IProductMetadataUpdater extends IProductMetadataProvider {

    /**
     * Sets the virtual channel ID on which this product was received.
     * 
     * @param vcid The vcid to set.
     */
    void setVcid(Integer vcid);

    /**
     * Sets the spacecraft ID for this product.
     * 
     * @param scid the ID to set
     */
    void setScid(int scid);

    /**
     * Sets the session ID for this data product.
     * 
     * @param testIdKey the session key as a long
     */
    void setSessionId(Long testIdKey);

    /**
     * Set session fragment.
     *
     * @param fragment Session fragment
     */
    void setSessionFragment(SessionFragmentHolder fragment);

    /**
     * Sets DSS (station) ID from the session that generated this product. 
     *
     * @param dssId station ID from the session
     */
    void setSessionDssId(Integer dssId);

    /**
     * Set the virtual channel ID from the session that generated this product.
     *
     * @param vcid the virtual channel ID from the session
     */
    void setSessionVcid(Integer vcid);

    /**
     * Sets the application process ID for this data product or part.
     * 
     * @param apid the ID to set
     */
    void setApid(int apid);

    /**
     * Sets the Spacecraft Event Time for this product or part.
     * 
     * @param scet the SCET time to set
     */
    void setScet(IAccurateDateTime scet);

    /**
     * Sets the spacecraft event time for this product or part from an ISO-formatted
     * date/time string.
     * 
     * @param scetStr the ISO-formatted date/time string
     * @throws SAXException if parsing of the SCET fails
     */
    void setScet(String scetStr) throws SAXException;

    /**
     * Sets the local solar time for this product part.
     * @param sol the ILocalSolarTime to set
     */
    void setSol(ILocalSolarTime sol);

    /**
     * Sets the Spacecraft Clock Time of this product or part.
     * 
     * @param sclk the SCLK to set
     */
    void setSclk(ICoarseFineTime sclk);

    /**
     * Sets the name of the directory in which the product or part is stored.
     * 
     * @param storageDirectory the directory name
     */
    void setStorageDirectory(String storageDirectory);

    /**
     * Sets the full path to the product file, minus the file extension.
     * 
     * @param filenameNoSuffix the file path without extension (suffix)
     */
    void setFullPath(String filenameNoSuffix);

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.template.Templatable#setTemplateContext(Map<String,Object>)
     */
    /**
     * @param map
     */
    void setTemplateContext(Map<String, Object> map);

    /**
     * Parses the value of a single field with the given XML element name from
     * the provided text.
     * 
     * @param elemName The name of the XML element
     * @param text the value of the XML element's text node
     * @return true if the value of the element with the given name could be parsed from
     * the given text
     */
    boolean parseFromElement(String elemName, String text);

    /**
     * Sets the sequence ID identifying the onboard sequence that generated the
     * product. Interpretation of the sequence ID is mission-specific.
     * 
     * @param seq the sequence ID to set
     */
    void setSequenceId(int seq);

    /**
     * Sets the sequence version number that identifies which version of the
     * onboard sequence that generated the product.
     * 
     * @param seqVersion the sequence version to set
     */
    void setSequenceVersion(int seqVersion);

    /**
     * Sets the command number identifying the FSW command that generated the
     * product.
     * 
     * @param commandNum the command number to set
     */
    void setCommandNumber(int commandNum);

    /**
     * Sets the XML product definition version, which is used to identify which
     * definition in the DP dictionary should be used when interpreting the
     * product.
     * 
     * @param xmlVersion the version to set
     */
    void setXmlVersion(int xmlVersion);

    /**
     * Sets the coarse data validity time in seconds.
     * 
     * @param dvtCoarse the coarse time
     */
    void setDvtCoarse(long dvtCoarse);

    /**
     * Sets the fine data validity time in subticks.
     * 
     * @param dvtFine the fine DVT
     */
    void setDvtFine(long dvtFine);

    /**
     * Sets the total number of data parts in the product. This is the total
     * expected number, not the number received.
     * 
     * @param totalParts the number of parts to set
     */
    void setTotalParts(int totalParts);

    /**
     * Sets the time at which the product was created on the ground.
     * 
     * @param productCreationTime The time to set.
     */
    void setProductCreationTime(IAccurateDateTime productCreationTime);

    /**
     * Sets the time at which the record was created.
     * 
     * @param rct The time to set.
     */
    void setRct(IAccurateDateTime rct);

    /**
     * Sets the partial product flag.
     * 
     * @param partial true if product is partial, false if not
     */
    void setPartial(boolean partial);

    /**
     * Sets the name of the host running the session on which this product was generated.
     * 
     * @param testHost The host name to set.
     */
    void setSessionHost(String testHost);

    /**
     * Sets the host ID.
     *
     * @param hostId The host ID.
     */
    void setSessionHostId(Integer hostId);

    /**
     * Adds a part to this product metadata.
     * 
     * @param part the new part
     */
    void addPart(IProductPartProvider part);

    /**
     * Sets the ERT of the data product or part,
     * 
     * @param ert The ERT to set.
     */
    void setErt(IAccurateDateTime ert);

    /**
     * Sets the ground status of the data product.
     * 
     * @param groundStatus The status to set.
     */
    void setGroundStatus(ProductStatusType groundStatus);

    /**
     * Sets the sequence category to a new value (null OK).
     * 
     * @param sc New sequenceCategory value
     */
    void setSequenceCategory(String sc);

    /**
     * Sets the sequence number to a new value (null OK).
     * 
     * @param sn New sequenceNumber value
     */
    void setSequenceNumber(Long sn);

    /**
     * Sets the spacecraft clock time from a formatted SCLK string.
     * 
     * @param sclkStr the SCLK string to parse
     * @throws SAXException if the SCLK parsing fails
     */
    void setSclk(String sclkStr) throws SAXException;

    /**
     * Sets the product type test, or APID name.
     * 
     * @param productType the productType to set
     */
    void setProductType(String productType);

    /**
     * Sets the Dictionary Directory name found in the product EMD file. Not populated
     * during product generation, only during product decom.
     * @param emdDictionaryDir the directory to set
     */
    void setEmdDictionaryDir(String emdDictionaryDir);

    /**
     * Sets the Dictionary Version found in the product EMD file. Not populated
     * during product generation, only during product decom.
     * @param emdDictionaryVersion the version string
     */
    void setEmdDictionaryVersion(String emdDictionaryVersion);

    /**
     * Sets the product version string.
     * 
     * @param productVersion the ground product version, or null if none set  
     */
    void setProductVersion(String productVersion);

    /**
     * Sets the flight software version.
     *
     * @param fswVersion
     */
    void setFswVersion(long fswVersion);

    /**
     * Sets the flight software dictionary.
     *
     * @param fswDictionaryDir
     */
    void setFswDictionaryDir(String fswDictionaryDir);

    /**
     * Sets the flight software dictionary version
     *
     * @param fswDictionaryVersion
     */
    void setFswDictionaryVersion(String fswDictionaryVersion);

    /**
     * Sets the expected checksum.
     * 
     * @param checksum The checksum to set.
     */
    void setChecksum(long checksum);

    /**
     * Sets the expected file size in bytes.
     * 
     * @param fileSize The fileSize to set.
     */
    void setFileSize(long fileSize);

    /**
     * Sets the actual checksum.
     * 
     * @param checksum The checksum to set.
     */
    void setActualChecksum(long checksum);

    /**
     * Sets the actual file size in bytes.
     * 
     * @param fileSize The fileSize to set.
     */
    void setActualFileSize(long fileSize);

    /**
     * Sets the product filename.
     * 
     * @param filename the name to set, without directory path.
     */
    void setFilename(String filename);

    /**
     * Loads product metadata from the XML product metadata file. Used during
     * decom only, not for message parsing.
     * 
     * @param filename the full path to the file to load
     * @throws ProductException if parsing or IO error
     */
    void loadFile(String filename) throws ProductException;
}