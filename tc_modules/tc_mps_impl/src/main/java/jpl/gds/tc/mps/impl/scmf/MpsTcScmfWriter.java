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
package jpl.gds.tc.mps.impl.scmf;

import gov.nasa.jpl.tcsession.TcSession;
import gov.nasa.jpl.uplinkutils.UplinkUtils;
import gov.nasa.jpl.uplinkutils.scmf_dataRec;
import gov.nasa.jpl.uplinkutils.scmf_headerRec;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.tc.api.ITcScmfWriter;
import jpl.gds.tc.api.config.ScmfProperties;
import jpl.gds.tc.api.exception.ScmfWrapUnwrapException;
import jpl.gds.tc.mps.impl.session.MpsSession;
import org.springframework.context.ApplicationContext;

import java.util.LinkedList;
import java.util.List;

/**
 * <p>{@code MpsTcScmfWriter} is an SCMF writer that uses the MPSA UplinkUtils to build one.</p>
 *
 * <p>This writer assumes nothing about the data records fed into it. Whatever data record the user supplies is what
 * gets added as a spacecraft message. This means that this writer does not generate the typical acquisition sequence
 * and tail sequence on behalf of the user. The user needs to add data records that contain them, in their respective
 * positions in the data record list. Also, the writer does not automatically insert marker or delimiter CLTUs. Again,
 * it's the user's responsibility to insert them in their proper locations. This writer simply builds an SCMF with
 * whatever the user provides.</p>
 *
 * <p>This class is not thread-safe.</p>
 *
 * @since 8.2.0
 */
public class MpsTcScmfWriter implements ITcScmfWriter {

    private final Tracer       log;
    private final List<String> scmfDataRecordDataHexList;
    private final List<String> scmfDataRecordCommentList;
    private       String       scmfHeaderPreparerName;
    private       double       scmfHeaderBitOneRadiationTime;
    private       int          scmfHeaderBitRateIndex;
    private       String       scmfHeaderComment;
    private       String       scmfHeaderTitle;
    private       int          scmfHeaderUntimed;
    private final String       scmfDefaultDataRecordComment;
    private       String       outScmfFile;
    private       int          missionId = -1;
    private       int          scid      = -1;
    private       double       scmfMessageStartTime = -1;
    private       double       scmfMessageWindowOpenTime = -1;
    private       double       scmfMessageWindowCloseTime = -1;

    /**
     * Default constructor
     *
     * @param appContext current application context
     */
    public MpsTcScmfWriter(final ApplicationContext appContext) {
        this(TraceManager.getTracer(appContext, Loggers.UPLINK), appContext.getBean(ScmfProperties.class));
    }

    /**
     * Default constructor
     *
     * @param tracer          log tracer
     * @param scmfProperties  SCMF Properties
     */

    public MpsTcScmfWriter(final Tracer tracer, final ScmfProperties scmfProperties) {
        this.log = tracer;
        scmfDataRecordDataHexList = new LinkedList<>();
        scmfDataRecordCommentList = new LinkedList<>();
        scmfHeaderPreparerName = scmfProperties.getPreparer();
        scmfHeaderBitOneRadiationTime = scmfProperties.getBitOneRadiationTime();
        scmfHeaderBitRateIndex = scmfProperties.getBitRateIndex();
        scmfHeaderComment = scmfProperties.getComment();
        scmfHeaderTitle = scmfProperties.getTitle();
        scmfHeaderUntimed = scmfProperties.getUntimed();
        scmfDefaultDataRecordComment = scmfProperties.getMessageComment();
    }

    @Override
    public ITcScmfWriter setScmfHeaderPreparerName(final String scmfHeaderPreparerName) {
        this.scmfHeaderPreparerName = scmfHeaderPreparerName;
        return this;
    }

    @Override
    public ITcScmfWriter setScmfHeaderBitOneRadiationTime(final double scmfHeaderBitOneRadiationTime) {
        this.scmfHeaderBitOneRadiationTime = scmfHeaderBitOneRadiationTime;
        return this;
    }

    @Override
    public double getScmfMessageRadiationWindowOpenTime() {
        return scmfMessageWindowOpenTime;
    }

    @Override
    public ITcScmfWriter setScmfMessageRadiationWindowOpenTime(double scmfMessageRadiationWindowOpenTime) {
        this.scmfMessageWindowOpenTime = scmfMessageRadiationWindowOpenTime;
        return this;
    }

    @Override
    public double getScmfMessageRadiationWindowCloseTime() {
        return scmfMessageWindowCloseTime;
    }

    @Override
    public ITcScmfWriter setScmfMessageRadiationWindowCloseTime(double scmfMessageRadiationWindowCloseTime) {
        this.scmfMessageWindowCloseTime = scmfMessageRadiationWindowCloseTime;
        return this;
    }

    @Override
    public double getScmfMessageRadiationStartTime() {
        return scmfMessageStartTime;
    }

    @Override
    public ITcScmfWriter setScmfMessageRadiationStartTime(double scmfMessageRadiationStartTime) {
        this.scmfMessageStartTime = scmfMessageRadiationStartTime;
        return this;
    }

    @Override
    public ITcScmfWriter setScmfHeaderBitRateIndex(final int scmfHeaderBitRateIndex) {
        this.scmfHeaderBitRateIndex = scmfHeaderBitRateIndex;
        return this;
    }

    @Override
    public ITcScmfWriter setScmfHeaderComment(final String scmfHeaderComment) {
        this.scmfHeaderComment = scmfHeaderComment;
        return this;
    }

    @Override
    public ITcScmfWriter setScmfHeaderTitle(final String scmfHeaderTitle) {
        this.scmfHeaderTitle = scmfHeaderTitle;
        return this;
    }

    @Override
    public ITcScmfWriter setScmfHeaderUntimed(final int scmfHeaderUntimed) {
        this.scmfHeaderUntimed = scmfHeaderUntimed;
        return this;
    }

    @Override
    public ITcScmfWriter addDataRecord(final String dataHex) {
        scmfDataRecordDataHexList.add(dataHex);
        scmfDataRecordCommentList.add(scmfDefaultDataRecordComment);
        return this;
    }

    @Override
    public ITcScmfWriter setCurrentDataRecordComment(final String dataRecordComment) throws ScmfWrapUnwrapException {

        if (scmfDataRecordCommentList.size() < 1) {
            throw new ScmfWrapUnwrapException("Must add a data record first");
        }

        scmfDataRecordCommentList.set(scmfDataRecordCommentList.size() - 1, dataRecordComment);
        return this;
    }

    @Override
    public ITcScmfWriter setOutScmfFile(final String outScmfFile) {
        this.outScmfFile = outScmfFile;
        return this;
    }

    @Override
    public ITcScmfWriter setScid(final int scid) {
        this.scid = scid;
        return this;
    }

    @Override
    public int getScid() {
        return this.scid;
    }

    @Override
    public int getMissionId() {
        return this.missionId;
    }

    @Override
    public ITcScmfWriter setMissionId(final int missionId) {
        this.missionId = missionId;
        return this;
    }

    @Override
    public List<String> getScmfDataRecordDataList() {
        return this.scmfDataRecordDataHexList;
    }

    @Override
    public List<String> getScmfDataRecordCommentList() {
        return this.scmfDataRecordCommentList;
    }

    @Override
    public String getScmfHeaderPreparerName() {
        return this.scmfHeaderPreparerName;
    }

    @Override
    public double getScmfHeaderBitOneRadiationTime() {
        return this.scmfHeaderBitOneRadiationTime;
    }

    @Override
    public int getScmfHeaderBitRateIndex() {
        return this.scmfHeaderBitRateIndex;
    }

    @Override
    public String getScmfHeaderComment() {
        return this.scmfHeaderComment;
    }

    @Override
    public String getScmfHeaderTitle() {
        return this.scmfHeaderTitle;
    }

    @Override
    public int getScmfHeaderUntimed() {
        return this.scmfHeaderUntimed;
    }

    @Override
    public String getScmfDefaultDataRecordComment() {
        return this.scmfDefaultDataRecordComment;
    }

    @Override
    public String getOutScmfFile() {
        return this.outScmfFile;
    }

    @Override
    public void writeScmf() throws ScmfWrapUnwrapException {

        checkPreconditions();

        try (final MpsSession tcSession = new MpsSession(scid)) {

            final int scmfHandle = getScmfHandle();

            final scmf_headerRec scmfHeader = generateHeaderRecord();
            writeHeaderRecord(scmfHandle, scmfHeader);

            // Set SCMF data one by one
            // Count how many data records have been written
            int i = 0;

            for (final String dataHex : scmfDataRecordDataHexList) {
                final String            dataComment    = scmfDataRecordCommentList.get(i);
                final TcSession.bufitem dataBufferItem = tcSession.hexStringToBufferItem(dataHex);
                final scmf_dataRec      scmfData       = generateDataRecord(dataBufferItem, dataComment);

                writeDataRecord(scmfHandle, i, dataHex, scmfData);
                i++;
            }

            log.debug("Wrote total of ", i, " data records to SCMF ", outScmfFile);
            UplinkUtils.scmf_close(scmfHandle);

        } catch (final Exception e) {
            // UplinkUtils might throw unexpected exception, so catch and rethrow as checked exception
            throw new ScmfWrapUnwrapException(e);
        }

    }

    private void writeDataRecord(final int scmfHandle, final int index, final String dataHex,
                                 final scmf_dataRec scmfData) throws
                                                              ScmfWrapUnwrapException {
        final int scmfDataWriteRetVal = UplinkUtils.scmf_write_dataRec(scmfHandle, scmfData);

        if (scmfDataWriteRetVal < 0) {
            throw new ScmfWrapUnwrapException(
                    "Failed to write data record #" + (index + 1) + " to SCMF file " + outScmfFile
                            + ". scmf_errno = " + UplinkUtils.getScmf_errno());
        }

        log.trace("Wrote data record: ", dataHex, " Comment: ", scmfDataRecordCommentList.get(index));
    }

    private scmf_dataRec generateDataRecord(final TcSession.bufitem bufferItem, final String comment) {
        final scmf_dataRec scmfData = new scmf_dataRec();
        scmfData.setDrNumBits(bufferItem.nbits);
        scmfData.setDrData(bufferItem.buf);
        scmfData.setDrComment(comment);
        if (scmfHeaderUntimed == 1) {
            scmfData.setDrCloseTime(-1);
            scmfData.setDrOpenTime(-1);
            scmfData.setDrStartTime(-1);
        } else {
            scmfData.setDrCloseTime(scmfMessageWindowCloseTime);
            scmfData.setDrOpenTime(scmfMessageWindowOpenTime);
            scmfData.setDrStartTime(scmfMessageStartTime);
        }

        return scmfData;
    }

    private void writeHeaderRecord(final int scmfHandle, final scmf_headerRec scmfHeader) throws
                                                                                          ScmfWrapUnwrapException {
        final int scmfHeaderWriteRetVal = UplinkUtils.scmf_write_headerRec(scmfHandle, scmfHeader);

        if (scmfHeaderWriteRetVal < 0) {
            throw new ScmfWrapUnwrapException("Failed to write header to SCMF file " + outScmfFile
                    + ". scmf_errno = " + UplinkUtils.getScmf_errno());
        }
    }

    private scmf_headerRec generateHeaderRecord() {
        // Set SCMF's header
        final scmf_headerRec scmfHeader = new scmf_headerRec();
        scmfHeader.setHrPreparerName(scmfHeaderPreparerName);
        scmfHeader.setHrBitOneRadTime(scmfHeaderBitOneRadiationTime);
        scmfHeader.setHrBitRateIndex(scmfHeaderBitRateIndex);
        scmfHeader.setHrComment(scmfHeaderComment);
        scmfHeader.setHrTitle(scmfHeaderTitle);
        scmfHeader.setHrUntimed(scmfHeaderUntimed);
        log.debug("scmfHeaderPreparerName=", scmfHeaderPreparerName);
        log.debug("scmfHeaderBitOneRadiationTime=", scmfHeaderBitOneRadiationTime);
        log.debug("scmfHeaderBitRateIndex=", scmfHeaderBitRateIndex);
        log.debug("scmfHeaderComment=", scmfHeaderComment);
        log.debug("scmfHeaderTitle=", scmfHeaderTitle);
        log.debug("scmfHeaderUntimed=", scmfHeaderUntimed);
        return scmfHeader;
    }

    private int getScmfHandle() throws ScmfWrapUnwrapException {
        log.debug("scmf_open_write_w(", outScmfFile, ", ", missionId, ", ", scid, ")");
        final int scmfHandle = UplinkUtils.scmf_open_write_w(outScmfFile, missionId,
                scid);

        if (scmfHandle < 0) {
            throw new ScmfWrapUnwrapException(
                    "Failed to open SCMF file " + outScmfFile + " for output. scmf_errno = " + UplinkUtils
                            .getScmf_errno());
        }
        return scmfHandle;
    }

    private void checkPreconditions() throws ScmfWrapUnwrapException {

        if (outScmfFile == null) {
            throw new ScmfWrapUnwrapException("Output SCMF file name must be set first");
        }

        if (scid < 0 || missionId < 0) {
            throw new IllegalStateException("The SCID and Mission ID are required to write an SCMF.");
        }
    }

}