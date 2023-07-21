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

package jpl.gds.tc.impl.scmf;

import jpl.gds.tc.api.*;
import jpl.gds.tc.api.cltu.ICltu;
import jpl.gds.tc.api.config.ScmfProperties;
import jpl.gds.tc.api.exception.CltuEndecException;
import jpl.gds.tc.api.exception.UnblockException;
import jpl.gds.tc.api.message.IScmfCommandMessage;
import jpl.gds.tc.api.message.IUplinkMessage;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SCMF object for MPS/CTS.
 *
 */
public class Scmf implements IScmf {

    private static final String FILE_NAME = "File Name";
    private static final String PREPARER = "Preparer";
    private static final String FILE_SIZE = "File Size";
    private static final String FILE_HEADER_SIZE = "File Header Size";
    private static final String MISSION_ID = "Mission ID";
    private static final String SPACECRAFT_ID = "Spacecraft ID";
    private static final String REFERENCE_NUMBER = "Reference Number";
    private static final String BIT_ONE_RAD_TIME = "Bit 1 Radiation Time";
    private static final String BIT_RATE_INDEX = "Bit Rate Index";
    private static final String BIT_RATE_VALUE = "Bit Rate Value";
    private static final String COMMENT = "Comment Field";
    private static final String CREATION_TIME = "Creation Time";
    private static final String TITLE = "Title";
    private static final String SEQTRAN_VERSION = "Seqtran Version";
    private static final String MACRO_VERSION = "Macro Version";
    private static final String FILE_CHECKSUM = "File Checksum";

    private List<IScmfCommandMessage>   commandMessages = new ArrayList<>();
    private String                      originalFile;
    private long                        bitRateIndex;
    private String                      commentField;
    private String                      macroVersion;
    private String                      preparer;
    private long                        referenceNumber;
    private String                      seqtranVersion;
    private String                      title;
    private long                        fileByteSize;
    private int                         fileChecksum;
    private long                        fileHeaderByteSize;
    private long                        missionId;
    private long                        spacecraftId;
    private String                      filename;
    private String                      bitOneRadiationTime;
    private String                      creationTime;
    private IScmfSfduHeader             sfduHeader;
    private byte[]                      bytes;
    private IScmfInternalMessageFactory scmfInternalMessageFactory;
    private ScmfProperties              scmfConfig;


    /**
     * Constructor
     *
     * @param scmfInternalMessageFactory internal message factory for SCMF
     */
    public Scmf(final IScmfInternalMessageFactory scmfInternalMessageFactory, final ScmfProperties scmfConfig) {
        this.scmfInternalMessageFactory = scmfInternalMessageFactory;
        this.scmfConfig = scmfConfig;
    }

    @Override
    public byte[] getBytes() {
        return this.bytes;
    }

    @Override
    public void addCommandMessage(final IScmfCommandMessage cm) {
        this.commandMessages.add(cm);
    }

    @Override
    public void removeAllCommandMessages() {
        this.commandMessages.clear();
    }

    @Override
    public List<IScmfCommandMessage> getCommandMessages() {
        return this.commandMessages;
    }

    @Override
    public void setCommandMessages(final List<IScmfCommandMessage> commandMessages) {
        this.commandMessages = commandMessages;
    }

    @Override
    public String getBitOneRadiationTime() {
        return this.bitOneRadiationTime;
    }

    @Override
    public void setBitOneRadiationTime(final String bitOneRadiationTime) {
        this.bitOneRadiationTime = bitOneRadiationTime;
    }

    @Override
    public String getCreationTime() {
        return this.creationTime;
    }

    @Override
    public void setCreationTime(final String creationTime) {
        this.creationTime = creationTime;
    }

    @Override
    public long getBitRateIndex() {
        return bitRateIndex;
    }

    @Override
    public void setBitRateIndex(final int bitRateIndex) {
        this.bitRateIndex = bitRateIndex;
    }

    @Override
    public String getCommentField() {
        return this.commentField;
    }

    @Override
    public void setCommentField(final String commentField) {
        this.commentField = commentField;
    }

    @Override
    public String getMacroVersion() {
        return this.macroVersion;
    }

    @Override
    public void setMacroVersion(final String macroVersion) {
        this.macroVersion = macroVersion;
    }

    @Override
    public String getPreparer() {
        return this.preparer;
    }

    @Override
    public void setPreparer(final String preparer) {
        this.preparer = preparer;
    }

    @Override
    public long getReferenceNumber() {
        return this.referenceNumber;
    }

    @Override
    public void setReferenceNumber(final long referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    @Override
    public String getSeqtranVersion() {
        return this.seqtranVersion;
    }

    @Override
    public void setSeqtranVersion(final String seqtranVersion) {
        this.seqtranVersion = seqtranVersion;
    }

    @Override
    public IScmfSfduHeader getSfduHeader() {
        return this.sfduHeader;
    }

    @Override
    public void setSfduHeader(final IScmfSfduHeader sfduHeader) {
        this.sfduHeader = sfduHeader;
    }

    @Override
    public String getTitle() {
        return this.title;
    }

    @Override
    public void setTitle(final String title) {
        this.title = title;
    }

    @Override
    public void parseFromInputStream(final InputStream in, final int scmfLength) {
        throw new UnsupportedOperationException("Operation is not yet supported.");
    }

    @Override
    public String toString() {
        try {
            return IOUtils.toString(new ByteArrayInputStream(bytes), StandardCharsets.US_ASCII.name());
        } catch (final IOException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    @Override
    public String getHeaderString() {
        IScmfSfduHeader header = getSfduHeader();

        StringBuilder sb = new StringBuilder(1024);
        sb.append("\n");

        long br = getBitRateIndex();
        String brv;

        try {
            brv = "" + BitRateTable.getBitRateFromIndex(scmfConfig, (int) this.bitRateIndex);
        } catch (IllegalArgumentException e) {
            brv = "Out of Bounds - " + scmfConfig.getProperty("scmf.bitRate.index.validValues");
        }


        addHeaderLine(sb, FILE_NAME, header.getFileName().trim());
        addHeaderLine(sb, PREPARER, getPreparer());
        addHeaderLine(sb, FILE_SIZE, String.valueOf(getFileByteSize()));
        addHeaderLine(sb, FILE_HEADER_SIZE, String.valueOf(getFileHeaderByteSize()));
        addHeaderLine(sb, MISSION_ID, String.valueOf(getMissionId()));
        addHeaderLine(sb, SPACECRAFT_ID, header.getSpacecraftId());
        addHeaderLine(sb, REFERENCE_NUMBER, String.valueOf(getReferenceNumber()));
        addHeaderLine(sb, BIT_ONE_RAD_TIME, getBitOneRadiationTime());
        addHeaderLine(sb, BIT_RATE_INDEX, String.valueOf(br));
        addHeaderLine(sb, BIT_RATE_VALUE, brv);
        addHeaderLine(sb, COMMENT, getCommentField());
        addHeaderLine(sb, CREATION_TIME, header.getProductCreationTime());
        addHeaderLine(sb, TITLE, getTitle());
        addHeaderLine(sb, SEQTRAN_VERSION, getSeqtranVersion());
        addHeaderLine(sb, MACRO_VERSION, getMacroVersion());
        addHeaderLine(sb, FILE_CHECKSUM, "0x" + getFileChecksum());

        return sb.toString();
    }

    private void addHeaderLine(final StringBuilder sb, final String label, final String value) {
        sb.append(label).append(" = ").append(value).append(";\n");
    }

    @Override
    public long getFileByteSize() {
        return this.fileByteSize;
    }

    @Override
    public void setFileByteSize(final long fileByteSize) {
        this.fileByteSize = fileByteSize;
    }

    @Override
    public int getFileChecksum() {
        return this.fileChecksum;
    }

    @Override
    public void setFileChecksum(final int fileChecksum) {
        this.fileChecksum = fileChecksum;
    }

    @Override
    public long getFileHeaderByteSize() {
        return this.fileHeaderByteSize;
    }

    @Override
    public String getFileName() {
        return this.filename;
    }

    @Override
    public void setFileName(final String filename) {
        this.filename = filename;
    }

    @Override
    public long getMissionId() {
        return this.missionId;
    }

    @Override
    public void setMissionId(final long missionId) {
        this.missionId = missionId;
    }

    @Override
    public long getSpacecraftId() {
        return this.spacecraftId;
    }

    @Override
    public void setSpacecraftId(final long spacecraftId) {
        this.spacecraftId = spacecraftId;
    }

    @Override
    public List<ICltu> getCltusFromScmf() throws CltuEndecException {
        final List<ICltu> list = new ArrayList<>();
        for (final IScmfCommandMessage commandMessage : commandMessages) {
            final ICltu cltuFromData = commandMessage.getCltuFromData();
            list.add(cltuFromData);
        }
        return list;
    }

    @Override
    public int getCltuCount() {
        return commandMessages.size();
    }

    @Override
    public List<ITcTransferFrame> getFramesFromScmf() throws CltuEndecException {
        return getCltusFromScmf().stream().flatMap(cltu -> cltu.getFrames().stream()).collect(Collectors.toList());
    }

    @Override
    public List<IUplinkMessage> getInternalMessagesFromScmf() throws UnblockException, CltuEndecException {
        if (scmfInternalMessageFactory == null) {
            throw new IllegalStateException("SCMF internal message factory is null.");
        }
        return scmfInternalMessageFactory.createInternalUplinkMessages(this);
    }

    @Override
    public String getOriginalFile() {
        return originalFile;
    }

    @Override
    public void setFilePath(final String filePath) {
        this.originalFile = filePath;
        try {
            this.bytes = IOUtils.toByteArray(new FileInputStream(filePath));
        } catch (final IOException e) {
            throw new IllegalArgumentException("File path could not be read to bytes. Is the file path correct?");
        }
        setFileName(filePath.substring(filePath.lastIndexOf('/') + 1));
    }

    @Override
    public void setFileHeaderByteSize(final int fileHeaderByteSize) {
        this.fileHeaderByteSize = fileHeaderByteSize;
    }

    /**
     * Set the internal message factory
     *
     * @param scmfInternalMessageFactory
     */
    public void setScmfInternalMessageFactory(final IScmfInternalMessageFactory scmfInternalMessageFactory) {
        this.scmfInternalMessageFactory = scmfInternalMessageFactory;
    }
}
