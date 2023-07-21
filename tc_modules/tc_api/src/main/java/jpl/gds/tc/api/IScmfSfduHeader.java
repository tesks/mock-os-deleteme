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
package jpl.gds.tc.api;

import java.text.SimpleDateFormat;

import jpl.gds.tc.api.exception.ScmfWrapUnwrapException;

public interface IScmfSfduHeader {
    
    /** The time format for the product creation time */
    String           PRODUCT_CREATION_TIME_FORMAT = "yyyy-DDD'T'hh:mm:ss.SSS";
    
    SimpleDateFormat creationTimeFormatter        = new SimpleDateFormat(PRODUCT_CREATION_TIME_FORMAT);

    String           FILE_NAME_LABEL              = "FILE_NAME";

    String           MISSION_NAME_LABEL           = "MISSION_NAME";

    String           SPACECRAFT_ID_LABEL          = "SPACECRAFT_ID";

    String           PRODUCT_CREATION_TIME_LABEL  = "PRODUCT_CREATION_TIME";

    String           PRODUCT_VERSION_LABEL        = "PRODUCT_VERSION";

    String           DATA_SET_ID_LABEL            = "DATA_SET_ID";

    String           MISSION_ID_LABEL             = "MISSION_ID";

    String           SPACECRAFT_NAME_LABEL        = "SPACECRAFT_NAME";

    /**
     * Get the string representation of this header (without the SFDU label)
     * 
     * @return The string representation of this header
     */
    String getHeaderString();

    /**
     * Accessor for the file name
     * 
     * @return Returns the fileName.
     */
    String getFileName();

    /**
     * Sets the fileName
     * 
     * @param fileName
     *            The fileName to set.
     */
    void setFileName(String fileName);

    /**
     * Accessor for the mission ID
     * 
     * @return Returns the missionId.
     */
    String getMissionId();

    /**
     * Sets the missionId
     * 
     * @param missionId
     *            The missionId to set.
     */
    void setMissionId(String missionId);

    /**
     * Accessor for the mission name
     * 
     * @return Returns the missionName.
     */
    String getMissionName();

    /**
     * Sets the missionName
     * 
     * @param missionName
     *            The missionName to set.
     */
    void setMissionName(String missionName);

    /**
     * Accessor for the spacecraft ID
     * 
     * @return Returns the spacecraftId.
     */
    String getSpacecraftId();

    /**
     * Sets the spacecraftId
     * 
     * @param spacecraftId
     *            The spacecraftId to set.
     */
    void setSpacecraftId(String spacecraftId);

    /**
     * Accessor for the spacecraft name
     * 
     * @return Returns the spacecraftName.
     */
    String getSpacecraftName();

    /**
     * Sets the spacecraftName
     * 
     * @param spacecraftName
     *            The spacecraftName to set.
     */
    void setSpacecraftName(String spacecraftName);

    /**
     * Accessor for the product creation time
     * 
     * @return Returns the product creation time
     */
    String getProductCreationTime();

    /**
     * Sets the product creation time
     * 
     * @param time
     *            The product creation time to set
     */
    void setProductCreationTime(String time);

    /**
     * @return Returns the productVersion.
     */
    String getProductVersion();

    /**
     * Sets the productVersion
     * 
     * @param productVersion
     *            The productVersion to set.
     */
    void setProductVersion(String productVersion);

}