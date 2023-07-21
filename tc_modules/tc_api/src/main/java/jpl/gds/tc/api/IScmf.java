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
package jpl.gds.tc.api;

import jpl.gds.tc.api.cltu.ICltu;
import jpl.gds.tc.api.exception.CltuEndecException;
import jpl.gds.tc.api.exception.ScmfWrapUnwrapException;
import jpl.gds.tc.api.exception.UnblockException;
import jpl.gds.tc.api.message.IScmfCommandMessage;
import jpl.gds.tc.api.message.IUplinkMessage;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface IScmf {

    /** The max allowable file size for an SCMF */
    public long   FILE_SIZE_MAX_VALUE       = 99999999;

    /**
     * Get the complete byte representation of the SCMF file.
     *
     * @return The byte array representation of this SCMF object
     * @throws ScmfWrapUnwrapException if the SCMF exceeds the maximum file size
     */
    byte[] getBytes() throws ScmfWrapUnwrapException;

    /**
     * Add a command message to this SCMF
     *
     * @param cm The command message to add
     */
    void addCommandMessage(IScmfCommandMessage cm);

    /**
     * Remove all command messages from this SCMF
     *
     */
    void removeAllCommandMessages();

    /**
     * Get the list of command messages in this SCMF
     *
     * @return The list of command messages in this SCMF
     */
    List<IScmfCommandMessage> getCommandMessages();

    /**
     * Sets the commandMessages
     *
     * @param commandMessages The commandMessages to set.
     */
    void setCommandMessages(List<IScmfCommandMessage> commandMessages);

    /**
     * Accessor for the bit one radiation time
     *
     * @return Returns the bitOneRadiationTime.
     */
    String getBitOneRadiationTime();

    /**
     * Sets the bitOneRadiationTime
     *
     * @param bitOneRadiationTime The bitOneRadiationTime to set.
     */
    void setBitOneRadiationTime(String bitOneRadiationTime);

    /**
     * @return Returns the creationTime.
     */
    String getCreationTime();

    /**
     * Sets the creationTime
     *
     * @param creationTime The creationTime to set.
     */
    void setCreationTime(String creationTime);

    /**
     * @return Returns the bitRateIndex.
     */
    long getBitRateIndex();

    /**
     * Sets the bitRate
     *
     * @param bitRateIndex The bitRate to set.
     */
    void setBitRateIndex(int bitRateIndex);

    /**
     * Accessor for the comment field
     *
     * @return Returns the commentField.
     */
    String getCommentField();

    /**
     * Sets the commentField
     *
     * @param commentField The commentField to set.
     */
    void setCommentField(String commentField);

    /**
     * Accessor for the macro version
     *
     * @return Returns the macroVersion.
     */
    String getMacroVersion();

    /**
     * Sets the macroVersion
     *
     * @param macroVersion The macroVersion to set.
     */
    void setMacroVersion(String macroVersion);

    /**
     * Accessor for the preparer
     *
     * @return Returns the preparer.
     */
    String getPreparer();

    /**
     * Sets the preparer
     *
     * @param preparer The preparer to set.
     */
    void setPreparer(String preparer);

    /**
     * Accessor for the reference number
     *
     * @return Returns the referenceNumber.
     */
    long getReferenceNumber();

    /**
     * Sets the referenceNumber
     *
     * @param referenceNumber The referenceNumber to set.
     */
    void setReferenceNumber(long referenceNumber);

    /**
     * Accessor for the seqtran version
     *
     * @return Returns the seqtranVersion.
     */
    String getSeqtranVersion();

    /**
     * Sets the seqtranVersion
     *
     * @param seqtranVersion The seqtranVersion to set.
     */
    void setSeqtranVersion(String seqtranVersion);

    /**
     * Accessor for the SFDU header
     *
     * @return Returns the sfduHeader.
     */
    IScmfSfduHeader getSfduHeader();

    /**
     * Sets the sfduHeader
     *
     * @param sfduHeader The sfduHeader to set.
     */
    void setSfduHeader(IScmfSfduHeader sfduHeader);

    /**
     * Accessor for the title
     *
     * @return Returns the title.
     */
    String getTitle();

    /**
     * Sets the title
     *
     * @param title The title to set.
     */
    void setTitle(String title);

    /**
     * Parse an SCMF from an input stream
     *
     * @param in The input stream to read the SCMF from
     * @param scmfLength The length of the SCMF being read
     *
     * @throws IOException
     * @throws ScmfWrapUnwrapException
     */
    void parseFromInputStream(InputStream in, int scmfLength) throws IOException, ScmfWrapUnwrapException;

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#toString()
     */
    @Override
    String toString();

    /**
     * Get a string representation of the SCMF header
     *
     * @return A string representing the SCMF header
     */
    String getHeaderString();

    /**
     * @return Returns the fileByteSize.
     */
    long getFileByteSize();

    /**
     * @return Returns the fileChecksum.
     */
    int getFileChecksum();

    /**
     * @return Returns the fileHeaderByteSize.
     */
    long getFileHeaderByteSize();

    /**
     * @return Returns the filename.
     */
    String getFileName();

    /**
     * @return Returns the missionId.
     */
    long getMissionId();

    /**
     * @return Returns the spacecraftId.
     */
    long getSpacecraftId();

    /**
     * Sets the fileByteSize
     *
     * @param fileByteSize The fileByteSize to set.
     */
    void setFileByteSize(long fileByteSize);

    /**
     * Sets the fileChecksum
     *
     * @param fileChecksum The fileChecksum to set.
     */
    void setFileChecksum(int fileChecksum);

    /**
     * Sets the filename
     *
     * @param filename The filename to set.
     */
    void setFileName(String filename);

    /**
     * Sets the missionId
     *
     * @param missionId The missionId to set.
     */
    void setMissionId(long missionId);

    /**
     * Sets the spacecraftId
     *
     * @param spacecraftId The spacecraftId to set.
     */
    void setSpacecraftId(long spacecraftId);

    /**
     * Retrieve all the CLTUs out of this SCMF
     *
     * @return A list of all the CLTUs contained in this SCMF
     *
     * @throws CltuEndecException
     */
    List<ICltu> getCltusFromScmf() throws CltuEndecException;

    /**
     * Return the number of CLTUs in this SCMF
     *
     * @return the number of CLTUs in this SCMF
     */
    int getCltuCount();

    /**
     * Retrieve all the telecommand frames from within the CTLUs in this SCMF
     *
     * @return A list of all the telecommand frames buried in this SCMF
     *
     * @throws CltuEndecException
     */
    List<ITcTransferFrame> getFramesFromScmf() throws CltuEndecException;

    /**
     * When transmitting an SCMF, we need to be able to pull out all of the
     * actual contents so they can be sent to the message bus and recorded in
     * the database. This method reverses the contents of the SCMF all the way
     * back to their original form (e.g. as FSW commands or file loads).
     *
     * @return A list of all the uplink items contained in this SCMF as messages
     *         that can be sent on the internal bus
     *
     * @throws CltuEndecException
     * @throws UnblockException
     */
    List<IUplinkMessage> getInternalMessagesFromScmf() throws CltuEndecException, UnblockException;

    /**
     * Accessor for the original file.
     *
     * @return The name of the original user input SCMF file
     */
    String getOriginalFile();

    /**
     * Set the file path of this SCMF
     *
     * @param filePath the file path
     */
    void setFilePath(final String filePath);

    /**
     * Set file header byte size
     *
     * @param fileHeaderByteSize size of file header in bytes
     */
    void setFileHeaderByteSize(final int fileHeaderByteSize);
}