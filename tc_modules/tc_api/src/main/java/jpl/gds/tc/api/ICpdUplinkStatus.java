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

import java.util.List;

import jpl.gds.shared.time.IAccurateDateTime;

public interface ICpdUplinkStatus {

    /**
     * Returns the ICMD request ID.
     *
     * @return ICMD request ID.
     */
    public String getId();

    /**
     * Returns the request status reported by ICMD.
     *
     * @return ICMD request status.
     */
    public CommandStatusType getStatus();

    /**
     * Returns the timestamp of the status, as reported by ICMD.
     *
     * @return status timestamp
     */
    public IAccurateDateTime getTimestamp();

    /**
     * Returns a string representation of the timestamp. Representation format
     * depends on the Time Configuration setting.
     *
     * @return Timestamp in string.
     */
    public String getTimestampString();

    /**
     * Returns a string representation of the bit 1 radiation time.
     * Representation format depends on the Time Configuration setting.
     *
     * @return bit 1 radiation time in string.
     */
    public String getBit1RadTimeString();

    /**
     * Returns a string representation of the last bit radiation time.
     * Representation format depends on the Time Configuration setting.
     *
     * @return last bit radiation time in string.
     */
    public String getLastBitRadTimeString();

    /**
     * Sets the timestamp.
     *
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(IAccurateDateTime timestamp);

    /**
     * Returns the filename property (from CPD's uplink request object)
     *
     * @return the filename
     */
    public String getFilename();

    /**
     * Returns the bitrate range list property (from CPD's uplink request
     * object)
     *
     * @return the bitrate range list
     */
    public List<Float> getBitrates();

    /**
     * @return the Estimated Rad Duration
     */
    public List<Float> getEstRadDurations();

    /**
     * Returns the user ID property (from CPD's uplink request object)
     *
     * @return the userId
     */
    public String getUserId();

    /**
     * Returns the role ID property (from CPD's uplink request object)
     *
     * @return the roleId
     */
    public String getRoleId();

    /**
     * Returns the submit time property (from CPD's uplink request object)
     *
     * @return the submit time
     */
    public String getSubmitTime();

    /**
     * Returns the includedinexelist property (from CPD's uplink request object)
     *
     * @return the includedinexelist property
     */
    public String getIncludedInExeList();

    /**
     * Get the station id.
     *
     * @return Station id
     */
    public Integer getDssId();

    /**
     * Get the radiation start time.
     *
     * @return Start time
     */
    public IAccurateDateTime getBit1RadTime();

    /**
     * Get the radiation end time.
     *
     * @return End time
     */
    public IAccurateDateTime getLastBitRadTime();

    /**
     * Get the SCMF checksum
     *
     * @return the SCMF checksum
     */
    public String getChecksum();

    /**
     * Get the total CLTUs in the SCMF
     *
     * @return the total CLTUs in the SCMF
     */
    public int getTotalCltus();

    /**
     * Set the CPD Request ID
     *
     * @param id the CPD request ID
     */
    public void setId(String id);

    /**
     * Set the CPD request status
     *
     * @param status the CPD request status
     */
    public void setStatus(CommandStatusType status);

    /**
     * Set the filename property (from CPD's uplink request object)
     *
     * @param filename the filename property (from CPD's uplink request object)
     */
    public void setFilename(String filename);

    /**
     * Get the uplink metadata
     *
     * @return the uplink metadata object
     */
    public IUplinkMetadata getUplinkMetadata();

    /**
     * Set the uplink metadata
     *
     * @param um the uplink metadata object
     */
    public void setUplinkMetadata(IUplinkMetadata um);

    public String getRadiationDurationString(float bitRate);

    /**
     * Get the SCMF creation time
     *
     * @return the the SCMF creation time
     */
    public String getScmfCreationTime();

    /**
     * Set the SCMF creation time
     *
     * @param scmfCreationTime the SCMF creation time
     */
    public void setScmfCreationTime(String scmfCreationTime);

    /**
     * Get the command dictionary version
     *
     * @return the the command dictionary version
     */
    public String getCommandDictVer();

    /**
     * Set the command dictionary version
     *
     * @param commandDictVer the command dictionary version
     */
    public void setCommandDictVer(String commandDictVer);
}