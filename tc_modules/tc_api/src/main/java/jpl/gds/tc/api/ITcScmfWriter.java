/*
 * Copyright 2006-2019. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */
package jpl.gds.tc.api;

import jpl.gds.tc.api.exception.ScmfWrapUnwrapException;

import java.util.List;

/**
 * {@code ITcScmfWriter} is a builder that accepts one or more SCMF data records (a.k.a. spacecraft messages) and
 * ultimately writes the SCMF to the file system.
 *
 * @since 8.2.0
 */
public interface ITcScmfWriter {

    /**
     * Add the provided hexadecimal string as a new data record (a.k.a. spacecraft message) entry in the SCMF to
     * be generated.
     *
     * @param dataHex hexadecimal string as a new data record
     * @return this object (builder pattern)
     */
    ITcScmfWriter addDataRecord(String dataHex);

    /**
     * Set the provided text as the comment text for the current data record. Note: Must call {@code #addDataRecord
     * (java.lang.String)} first.
     *
     * @param dataRecordComment comment text for the current data record
     * @return this object (builder pattern)
     * @throws ScmfWrapUnwrapException thrown when there is no data record to apply this comment text to
     */
    ITcScmfWriter setCurrentDataRecordComment(String dataRecordComment) throws ScmfWrapUnwrapException;

    /**
     * Retrieve the current list of data records to be written to the SCMF. Data is represented in hexadecimal
     * strings. Do not modify this list! It should be used for examination only.
     *
     * @return current list of data record hexadecimal strings
     */
    List<String> getScmfDataRecordDataList();

    /**
     * Retrieve the current list of comment texts to be written to the SCMF. Do not modify this list! It should be
     * used for examination only. The list order matches that of {@code #getScmfDataRecordDataList()}, i.e. comment
     * text #5 goes with the data record (hex string) #5.
     *
     * @return current list of data record comment texts
     */
    List<String> getScmfDataRecordCommentList();

    /**
     * Get the SCMF header preparer name currently configured for this writer.
     *
     * @return SCMF header preparer name
     */
    String getScmfHeaderPreparerName();

    /**
     * Set the SCMF header preparer name for this writer.
     *
     * @param scmfHeaderPreparerName SCMF header preparer name to set this writer with
     * @return this object (builder pattern)
     */
    ITcScmfWriter setScmfHeaderPreparerName(String scmfHeaderPreparerName);

    /**
     * Get the SCMF header bit one radiation time currently configured for this writer.
     *
     * @return SCMF header bit one radiation time
     */
    double getScmfHeaderBitOneRadiationTime();

    /**
     * Set the SCMF header bit one radiation time for this writer.
     *
     * @param scmfHeaderBitOneRadiationTime SCMF header bit one radiation time, in seconds since UNIX epoch.
     * @return this object (builder pattern)
     */
    ITcScmfWriter setScmfHeaderBitOneRadiationTime(double scmfHeaderBitOneRadiationTime);

    /**
     * Get the SCMF message radiation window open time for this writer.
     *
     * @return SCMF message radiation window open time, in seconds since UNIX epoch
     */
    double getScmfMessageRadiationWindowOpenTime();

    /**
     * Set the SCMF message radiation window open time for this writer.
     *
     * @param scmfMessageRadiationWindowOpenTime SCMF message radiation window open time, in seconds since UNIX epoch.
     * @return this object (builder pattern)
     */
    ITcScmfWriter setScmfMessageRadiationWindowOpenTime(double scmfMessageRadiationWindowOpenTime);

    /**
     * Get the SCMF message radiation window close time for this writer.
     *
     * @return SCMF message radiation window close time, in seconds since UNIX epoch.
     */
    double getScmfMessageRadiationWindowCloseTime();

    /**
     * Set the SCMF message radiation window close time for this writer.
     *
     * @param scmfMessageRadiationWindowCloseTime SCMF message radiation window close time, in seconds since UNIX epoch.
     * @return this object (builder pattern)
     */
    ITcScmfWriter setScmfMessageRadiationWindowCloseTime(double scmfMessageRadiationWindowCloseTime);

    /**
     * Get the SCMF message radiation start time for this writer.
     *
     * @return SCMF message radiation start time, in seconds since UNIX epoch.
     */
    double getScmfMessageRadiationStartTime();

    /**
     * Set the SCMF message radiation start time for this writer.
     *
     * @param scmfMessageRadiationStartTime SCMF message radiation start time, in seconds since UNIX epoch
     * @return this object (builder pattern)
     */
    ITcScmfWriter setScmfMessageRadiationStartTime(double scmfMessageRadiationStartTime);

    /**
     * Get the SCMF header bit rate index currently configured for this writer.
     *
     * @return SCMF header bit rate index
     */
    int getScmfHeaderBitRateIndex();

    /**
     * Set the SCMF header bit rate index for this writer.
     *
     * @param scmfHeaderBitRateIndex SCMF header bit rate index to set this writer with
     * @return this object (builder pattern)
     */
    ITcScmfWriter setScmfHeaderBitRateIndex(int scmfHeaderBitRateIndex);

    /**
     * Get the SCMF header comment text currently configured for this writer.
     *
     * @return SCMF header comment text
     */
    String getScmfHeaderComment();

    /**
     * Set the SCMF header comment text for this writer.
     *
     * @param scmfHeaderComment SCMF header comment text to set this writer with
     * @return this object (builder pattern)
     */
    ITcScmfWriter setScmfHeaderComment(String scmfHeaderComment);

    /**
     * Get the SCMF header title text currently configured for this writer.
     *
     * @return SCMF header title text
     */
    String getScmfHeaderTitle();

    /**
     * Set the SCMF header title text for this writer.
     *
     * @param scmfHeaderTitle SCMF header title text to set this writer with
     * @return this object (builder pattern)
     */
    ITcScmfWriter setScmfHeaderTitle(String scmfHeaderTitle);

    /**
     * Get the SCMF header untimed value currently configured for this writer.
     *
     * @return SCMF header untimed value
     */
    int getScmfHeaderUntimed();

    /**
     * Set the SCMF header untimed value for this writer.
     *
     * @param scmfHeaderUntimed SCMF header untimed value to set this writer with
     * @return this object (builder pattern)
     */
    ITcScmfWriter setScmfHeaderUntimed(int scmfHeaderUntimed);

    /**
     * Get the SCMF default data record comment text currently configured for this writer.
     *
     * @return SCMF default data record comment text
     */
    String getScmfDefaultDataRecordComment();

    /**
     * Get the output SCMF file name currently configured for this writer.
     *
     * @return output SCMF file name
     */
    String getOutScmfFile();

    /**
     * Set the output SCMF file name for this writer
     *
     * @param outScmfFile output SCMF file name to set this writer with
     * @return this object (builder pattern)
     */
    ITcScmfWriter setOutScmfFile(String outScmfFile);

    /**
     * Set the SCID to use.
     * @param scid spacecraft ID
     * @return this object (builder pattern)
     */
    ITcScmfWriter setScid(int scid);

    /**
     * Get the SCID currently configured for this writer.
     *
     * @return the spacecraft ID
     */
    int getScid();

    /**
     * Get the mission ID currently configured for this writer.
     *
     * @return the mission ID
     */
    int getMissionId();

    /**
     * Set the mission ID to use.
     *
     * @param missionId mission ID
     * @return this object (builder pattern)
     */
    ITcScmfWriter setMissionId(int missionId);

    /**
     * Write the SCMF to the configured output file name.
     *
     * @throws ScmfWrapUnwrapException thrown when any precondition is not met (such as required parameters not yet
     *                                 set) or when the MPSA UplinkUtils library throws any sort of exception
     */
    void writeScmf() throws ScmfWrapUnwrapException;

}