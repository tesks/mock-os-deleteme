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
package jpl.gds.tc.mps.impl.scmf.parsers;

import gov.nasa.jpl.tcsession.TcSession;
import gov.nasa.jpl.uplinkutils.SWIGTYPE_p_unsigned_char;
import gov.nasa.jpl.uplinkutils.UplinkUtils;
import gov.nasa.jpl.uplinkutils.scmf_dataRec;
import gov.nasa.jpl.uplinkutils.scmf_headerRec;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.IScmf;
import jpl.gds.tc.api.IScmfInternalMessageFactory;
import jpl.gds.tc.api.cltu.ICltu;
import jpl.gds.tc.api.config.CltuProperties;
import jpl.gds.tc.api.config.CommandFrameProperties;
import jpl.gds.tc.api.config.ScmfProperties;
import jpl.gds.tc.api.exception.CltuEndecException;
import jpl.gds.tc.api.exception.ScmfParseException;
import jpl.gds.tc.api.scmf.IScmfBuilder;
import jpl.gds.tc.impl.scmf.Scmf;
import jpl.gds.tc.impl.scmf.ScmfCommandMessage;
import jpl.gds.tc.impl.scmf.ScmfSfduHeader;
import jpl.gds.tc.mps.impl.cltu.parsers.MpsCltuParser;
import jpl.gds.tc.mps.impl.session.MpsSession;

import static jpl.gds.tc.impl.scmf.ScmfDateUtils.toScmfDate;

/**
 * The MpsScmfBuilder builds SCMFs utilizing the MPS "gold standard" translation.
 *
 * MPCS-11285 - 09/24/19 - added CltuProperties as a required object. Use CltuProperties to get start and
 * tail CLTU sequences
 */
public class MpsScmfBuilder implements IScmfBuilder {
    private       IScmfInternalMessageFactory scmfInternalMessageFactory;
    private       String                      filePath;
    private final Tracer                      tracer     = TraceManager.getTracer(Loggers.UPLINK);
    private       MissionProperties           missionProperties;
    private       ScmfProperties              scmfProperties;
    private       CltuProperties              cltuProperties;
    private       boolean                     validation = true;
    private       CommandFrameProperties      frameProperties;

    /**
     * Set the file path of an SCMF
     *
     * @param filePath the SCMF file path
     * @return this builder
     */
    public IScmfBuilder setFilePath(final String filePath) {
        this.filePath = filePath;
        return this;
    }

    /**
     * Set the internal message factory for SCMFs
     *
     * @param scmfInternalMessageFactory SCMF internal message factory
     * @return this builder
     */
    public IScmfBuilder setInternalMessageFactory(final IScmfInternalMessageFactory scmfInternalMessageFactory) {
        this.scmfInternalMessageFactory = scmfInternalMessageFactory;
        return this;
    }

    /**
     * Set the Mission Properties for this builder
     *
     * @param missionProperties mission properties
     * @return this builder
     */
    public IScmfBuilder setMissionProperties(final MissionProperties missionProperties) {
        this.missionProperties = missionProperties;
        return this;
    }

    public IScmfBuilder setScmfProperties(final ScmfProperties scmfProperties) {
        this.scmfProperties = scmfProperties;
        return this;
    }

    public IScmfBuilder setCltuProperties( final CltuProperties cltuProperties) {
        this.cltuProperties = cltuProperties;
        return this;
    }

    public IScmfBuilder setFrameProperties(final CommandFrameProperties frameProperties) {
        this.frameProperties = frameProperties;
        return this;
    }

    public IScmf build() throws ScmfParseException {
        checkPreconditions();

        final IScmf scmf = new Scmf(scmfInternalMessageFactory, scmfProperties);

        final int scmfHandle = getScmfHandle(filePath);

        final scmf_headerRec inHr = getCtsScmfHeader(scmfHandle);

        generateScmfHeader(filePath, scmf, inHr);

        try (final MpsSession session = new MpsSession(inHr.getHrScid())) {
            String startSeq = cltuProperties.getStartSequenceHex();
            String tailSeq = cltuProperties.getTailSequenceHex();

            if(startSeq != null && !startSeq.isEmpty()) {
                session.setCltuStartSequence(startSeq);
            }
            if(tailSeq != null && !tailSeq.isEmpty()) {
                session.setCltuTailSequence(cltuProperties.getTailSequenceHex());
            }
            
            scmf_dataRec dataRecord;

            final MpsCltuParser             cltuParser  = new MpsCltuParser(inHr.getHrScid());
            if (frameProperties.getFecfLength() != 2) {
                cltuParser.setFecfByteLength(frameProperties.getFecfLength());
            }

            while ((dataRecord = UplinkUtils.scmf_read_next_dataRec(scmfHandle)) != null) {
                final byte[] cltuBytes = getCltuBytesFromDataRecord(dataRecord);
                ICltu        cltu;
                try {

                    final TcSession.cltuitem cltuItem = session.getCltuItem(dataRecord, validation);
                    if (cltuItem.nerrors > 0) {
                        final String errMsg = "Error decoding CLTUs in SCMF: " + cltuItem.errmsg;
                        tracer.error(errMsg);
                        throw new ScmfParseException(errMsg);
                    }

                    cltu = cltuParser.parse(cltuItem);

                } catch (final CltuEndecException e) {
                    cltu = null;
                }

                generateScmfCommandMessage(scmf, dataRecord, cltuBytes, cltu);
            }

            checkScmfReadError();

            UplinkUtils.scmf_close(scmfHandle);

            return scmf;
        } catch (final RuntimeException e) {
            throw new ScmfParseException(e);
        }
    }

    private void checkPreconditions() {
        if (this.filePath == null) {
            throw new IllegalStateException("The file path must be set.");
        }

        if (this.scmfInternalMessageFactory == null) {
            throw new IllegalStateException("The internal message factory must be set.");
        }

        if (this.missionProperties == null) {
            throw new IllegalStateException("The mission properties must set.");
        }

        if (this.scmfProperties == null) {
            throw new IllegalStateException("The SCMF properties must be set.");
        }

        if (this.cltuProperties == null) {
            throw new IllegalStateException("The CLTU properties must be set.");
        }

        if (this.frameProperties == null) {
            throw new IllegalStateException("The TC frame properties must be set.");
        }
    }

    private void generateScmfCommandMessage(final IScmf scmf, final scmf_dataRec dataRecord,
                                            final byte[] cltuBytes, final ICltu cltu) {
        final ScmfCommandMessage message = new ScmfCommandMessage();
        scmf.addCommandMessage(message);
        message.setData(cltuBytes);
        message.setCltuFromData(cltu);
        message.setMessageChecksum(dataRecord.getDrChecksum());
        message.setCloseWindow(toScmfDate((long) dataRecord.getDrCloseTime()));
        message.setOpenWindow(toScmfDate((long) dataRecord.getDrOpenTime()));
        message.setMessageComment(dataRecord.getDrComment());
        message.setMessageNumber(dataRecord.getDrMsgNum());
        message.setTransmissionStartTime(toScmfDate((long) dataRecord.getDrStartTime()));
    }

    private byte[] getCltuBytesFromDataRecord(final scmf_dataRec dataRecord) {
        final SWIGTYPE_p_unsigned_char drData    = dataRecord.getDrData();
        final long                     drNumBits = dataRecord.getDrNumBits();

        final String cltuHexString = UplinkUtils.bintoasciihex(drData,
                (int) drNumBits, 0);
        return BinOctHexUtility.toBytesFromHex("0x" + cltuHexString);
    }

    private void checkScmfReadError() throws ScmfParseException {
        final int errorNumber = UplinkUtils.getScmf_errno();
        if (errorNumber != UplinkUtils.SCMF_ENORMEOF) {
            final String errorMessage = "Failed to read SCMF data record (scmf_errno = " + errorNumber + ")";
            tracer.error(errorMessage);
            throw new ScmfParseException(errorMessage);
        }
    }

    private void generateScmfHeader(final String filePath, final IScmf scmf, final scmf_headerRec inHr) {
        final ScmfSfduHeader header = new ScmfSfduHeader();

        header.setMissionId(String.valueOf(inHr.getHrPrjid()));
        header.setSpacecraftId(String.valueOf(inHr.getHrScid()));
        header.setProductCreationTime(inHr.getHrCreationTime());
        header.setFileName(inHr.getHrSfduFileName());
        header.setMissionName(missionProperties.getMissionLongName());
        header.setSpacecraftName(missionProperties.mapScidToName(inHr.getHrScid()));
        header.setProductVersion("1");
        scmf.setFileHeaderByteSize((int) inHr.getHrFileHdrSize());
        scmf.setSfduHeader(header);

        scmf.setFilePath(filePath);
        final double bitOneRadiationTime = inHr.getHrBitOneRadTime();
        if (bitOneRadiationTime < 0) {
            scmf.setBitOneRadiationTime("UNTIMED");
        } else {
            scmf.setBitOneRadiationTime(toScmfDate((long) inHr.getHrBitOneRadTime()));
        }
        scmf.setFileName(inHr.getHrFileName());
        scmf.setSpacecraftId(inHr.getHrScid());
        scmf.setMissionId(inHr.getHrPrjid());
        scmf.setFileChecksum(inHr.getHrChecksum());
        scmf.setFileByteSize(inHr.getHrFileSize());
        scmf.setTitle(inHr.getHrTitle());
        scmf.setCommentField(inHr.getHrComment());
        scmf.setCreationTime(inHr.getHrCreationTime());
        scmf.setMacroVersion(inHr.getHrMacroVersion());
        scmf.setSeqtranVersion(inHr.getHrSeqtranVersion());
        scmf.setBitRateIndex(inHr.getHrBitRateIndex());
        scmf.setPreparer(inHr.getHrPreparerName());
    }

    private scmf_headerRec getCtsScmfHeader(final int scmfHandle) throws ScmfParseException {
        final scmf_headerRec inHr = UplinkUtils.scmf_read_headerRec(scmfHandle);
        if (inHr == null) {
            final int    errNum = UplinkUtils.getScmf_errno();
            final String errMsg = "Failed to read SCMF header record (scmf_errno = " + errNum + ")";
            tracer.error(errMsg);
            throw new ScmfParseException(errMsg);
        }
        return inHr;
    }

    private int getScmfHandle(final String filePath) throws ScmfParseException {
        final int in = UplinkUtils.scmf_open_read_w(filePath);
        if (in < 0) {
            final int    errNum    = UplinkUtils.getScmf_errno();
            final String ctsErrMsg = UplinkUtils.getErrMsg(UplinkUtils.getScmf_errlist(), errNum);
            final String errMsg    = "Failed to open SCMF '" + filePath + "' for reading (scmf_errno = " + errNum + "): " + ctsErrMsg;
            tracer.error(errMsg);
            throw new ScmfParseException(errMsg);
        }
        return in;
    }

}
