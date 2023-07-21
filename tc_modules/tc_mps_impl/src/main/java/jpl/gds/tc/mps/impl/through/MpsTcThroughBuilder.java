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
package jpl.gds.tc.mps.impl.through;

import gov.nasa.jpl.tcsession.TcSession;
import gov.nasa.jpl.uplinkutils.UplinkUtils;
import jpl.gds.dictionary.api.command.CommandDefinitionType;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.ITcScmfWriter;
import jpl.gds.tc.api.config.ScmfProperties;
import jpl.gds.tc.api.exception.CltuEndecException;
import jpl.gds.tc.api.exception.CommandParseException;
import jpl.gds.tc.api.exception.FrameWrapUnwrapException;
import jpl.gds.tc.api.exception.ScmfWrapUnwrapException;
import jpl.gds.tc.api.frame.FrameErrorControlFieldAlgorithm;
import jpl.gds.tc.api.through.ITcThroughBuilder;
import jpl.gds.tc.api.through.ThroughTewException;
import jpl.gds.tc.impl.scmf.ScmfDateUtils;
import jpl.gds.tc.mps.impl.ctt.CommandTranslationTable;
import jpl.gds.tc.mps.impl.properties.MpsTcProperties;
import jpl.gds.tc.mps.impl.session.AMpsSession;
import jpl.gds.tc.mps.impl.session.MpsSession;
import jpl.gds.tc.mps.impl.session.TranslatingMpsSession;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static gov.nasa.jpl.uplinkutils.UplinkUtils.bintoasciihex;
import static gov.nasa.jpl.uplinkutils.UplinkUtilsConstants.*;
import static jpl.gds.tc.api.frame.FrameErrorControlFieldAlgorithm.EACSUM55AA;

/**
 * <p>{@code MpsTcThroughBuilder} implements the {@code ITcThroughBuilder} bean. It is an AMPCS-facing wrapper for
 * performing "gold standard" translation, encoding, and/or wrapping, entirely using the MPSA UplinkUtils library
 * underneath as the pipeline.</p>
 *
 * <p>This builder will not free up the memory held by the {@code CommandTranslationTable} passed to it. User is
 * responsible for doing that, if there's no longer a need for the CTT.</p>
 *
 * <h1>Steps to Use this Builder</h1>
 *
 * <ol>
 * <li>Instantiate the builder bean via Spring</li>
 * <li>At minimum, set the required parameters:
 * <ul>
 * <li>{@see #setCommandTranslationTable(jpl.gds.tc.mps.impl.ctt.CommandTranslationTable)}</li>
 * <li>{@see #setScid(int)}</li>
 * <li>{@see #setSclkScetFile(java.lang.String)}</li>
 * <li>{@see #setHardwareCommandsVcid(int)}</li>
 * <li>{@see #setImmediateFswCommandsVcid(int)}</li>
 * <li>{@see #setMarkerVcid(int)}</li>
 * <li>{@see #setFirstMarkerFrameDataHex(java.lang.String)}</li>
 * <li>{@see #setMiddleMarkerFrameDataHex(java.lang.String)}</li>
 * <li>{@see #setLastMarkerFrameDataHex(java.lang.String)}</li>
 * <li>{@see #setAcqSeq(java.lang.String)}</li>
 * <li>{@see #setTailSeq(java.lang.String)}</li>
 * <li>{@see #setOutScmfFile(java.lang.String)}</li>
 * </ul>
 * </li>
 * <li>Feel free to call any of the other "set" methods to customize the builder parameters to your liking.</li>
 * <li>Feed the builder with hardware and immediate FSW commands. Use {@code #addCommandMnemonic(java.lang.String,
 * jpl.gds.tc.api.through.ITcThroughBuilder.InternalCommandMnemonicType)} and {@code #addCommandBytes(java.lang.String,
 * jpl.gds.tc.api.through.ITcThroughBuilder.InternalCommandMnemonicType)} calls.</li>
 * <li>Finally, call {@see #buildScmf()} on the builder to retrieve the SCMF generated from the commands fed in</li>
 * </ol>
 *
 * <p>Hardware and immediate FSW commands are sequenced separately within the builder. By default, hardware commands
 * have the frame sequence number always set to 0. Also by default, immediate FSW commands have frame sequence numbers
 * that start from zero and increment by 1 with each added command. The user may override these default FSNs,
 * however.</p>
 *
 * <h1>SCMF Generation Defaults</h1>
 *
 * <p>Preparer name field in SCMF header will be defaulted to current logged in user via system properties, unless
 * overridden.</p>
 *
 * <p>All other SCMF header and data record entries will be defaulted to that configured in AMPCS properties. See
 * the "command.mps" block in AMPCS properties.</p>
 *
 * MPCS-11308 - 2019-09-25 - refactored to include support for adding hex bytes directly to the list
 *                                               the list of commands to wrap.
 * MPCS-11285 - 09/24/19 - changed tailSeq to idleSeq to match AMPCS terminology in other classes. added
 * get and set for startSeq and tailSeq.
 * @since 8.2.0
 */
public class MpsTcThroughBuilder implements ITcThroughBuilder {

    private final Tracer                                            log;
    private final List<MpsTcThroughBuilder.InternalCommandWrapper>  commandWrappers;
    private       int                                               hardwareCommandsVcid;
    private       int                                               immediateFswCommandsVcid;
    private       int                                               sequenceCommandsVcid;
    private       int                                               markerVcid;
    private       String                                            firstMarkerFrameDataHex;
    private       String                                            middleMarkerFrameDataHex;
    private       String                                            lastMarkerFrameDataHex;
    private       CommandTranslationTable                           commandTranslationTable;
    private       int                                               scid = -1;
    private       String                                            sclkScetFile;
    private       String                                            acqSeq;
    private       String                                            idleSeq;
    private       String                                            startSeq = null;
    private       String                                            tailSeq = null;

    private String outScmfFile;

    private boolean                         hardwareCommandsFrameErrorControlFieldAlgorithmSet;
    private FrameErrorControlFieldAlgorithm hardwareCommandsFrameErrorControlFieldAlgorithm;
    private boolean                         hardwareCommandsFrameErrorControlFieldSetToNone;
    private boolean                         immediateFswCommandsFrameErrorControlFieldAlgorithmSet;
    private FrameErrorControlFieldAlgorithm immediateFswCommandsFrameErrorControlFieldAlgorithm;
    private boolean                         immediateFswCommandsFrameErrorControlFieldSetToNone;
    private boolean                         sequenceCommandsFrameErrorControlFieldAlgorithmSet;
    private FrameErrorControlFieldAlgorithm sequenceCommandsFrameErrorControlFieldAlgorithm;
    private boolean                         sequenceCommandsFrameErrorControlFieldSetToNone;
    private boolean                         delimiterFrameErrorControlFieldAlgorithmSet;
    private FrameErrorControlFieldAlgorithm delimiterFrameErrorControlFieldAlgorithm;
    private boolean                         delimiterFrameErrorControlFieldSetToNone;

    private int     hardwareCommandsFrameSequenceNumber;
    private boolean hardwareCommandsFrameSequenceNumberSet;
    private int     immediateFswCommandsFrameSequenceNumber;
    private boolean immediateFswCommandsFrameSequenceNumberSet;
    private int     sequenceCommandsFrameSequenceNumber;
    private boolean sequenceCommandsFrameSequenceNumberSet;

    private boolean hardwareCommandsBypassFlagSet;
    private boolean hardwareCommandsBypassFlag;
    private boolean immediateFswCommandsBypassFlagSet;
    private boolean immediateFswCommandsBypassFlag;
    private boolean sequenceCommandsBypassFlagSet;
    private boolean sequenceCommandsBypassFlag;

    private boolean hardwareCommandsControlCommandFlagSet;
    private boolean hardwareCommandsControlCommandFlag;
    private boolean immediateFswCommandsControlCommandFlagSet;
    private boolean immediateFswCommandsControlCommandFlag;
    private boolean sequenceCommandsControlCommandFlagSet;
    private boolean sequenceCommandsControlCommandFlag;

    private final ITcScmfWriter scmfWriter;

    private String scmfHeaderPreparerName;
    private double scmfHeaderBitOneRadiationTime;
    private int    scmfHeaderBitRateIndex;
    private String scmfHeaderComment;
    private String scmfHeaderTitle;
    private int    scmfHeaderUntimed;

    private String scmfDataFirstRecordComment;
    private String scmfDataMarkerRecordComment;
    private String scmfDataActualCommandRecordComment;
    private String scmfDataLastRecordComment;

    private long scmfMessageRadiationStartTime;
    private long scmfRadiationWindowOpenTime;
    private long scmfRadiationWindowCloseTime;
    private boolean fecfByteLengthSet;
    private int     fecfByteLength;

    /**
     * @param appContext current application context
     */
    public MpsTcThroughBuilder(final ApplicationContext appContext) {
        this(appContext.getBean(ScmfProperties.class), appContext.getBean(MpsTcProperties.class),
                appContext.getBean(ITcScmfWriter.class),
                TraceManager.getTracer(appContext, Loggers.UPLINK));
    }

    public MpsTcThroughBuilder(final ScmfProperties scmfProperties, final MpsTcProperties mpsTcProperties,
                               final ITcScmfWriter scmfWriter, final Tracer tracer) {
        this.scmfWriter = scmfWriter;

        if (tracer == null) {
            this.log = TraceManager.getTracer(Loggers.UPLINK);
        } else {
            this.log = tracer;
        }

        commandWrappers = new ArrayList<>();
        hardwareCommandsVcid = -1;
        immediateFswCommandsVcid = -1;
        sequenceCommandsVcid = -1;
        markerVcid = -1;
        scmfHeaderPreparerName = GdsSystemProperties.getSystemUserName();
        scmfHeaderBitOneRadiationTime = scmfProperties.getBitOneRadiationTime();
        scmfHeaderBitRateIndex = scmfProperties.getBitRateIndex();
        scmfHeaderComment = scmfProperties.getComment();
        scmfHeaderTitle = scmfProperties.getTitle();
        scmfHeaderUntimed = scmfProperties.getUntimed();

        scmfMessageRadiationStartTime = ScmfDateUtils.getTransmissionStartTime(scmfProperties);
        scmfRadiationWindowOpenTime = ScmfDateUtils.getTransmissionWindowOpenTime(scmfProperties);
        scmfRadiationWindowCloseTime = ScmfDateUtils.getTransmissionWindowCloseTime(scmfProperties);

        scmfDataFirstRecordComment = mpsTcProperties.getScmfDataRecordDefaultFirstRecordCommentProperty();
        scmfDataMarkerRecordComment = mpsTcProperties.getScmfDataRecordDefaultMarkerCommentProperty();
        scmfDataActualCommandRecordComment = mpsTcProperties.getScmfDataRecordDefaultCommandCommentProperty();
        scmfDataLastRecordComment = mpsTcProperties.getScmfDataRecordDefaultLastRecordCommentProperty();
    }

    @Override
    public String getOutScmfFile() {
        return outScmfFile;
    }

    @Override
    public ITcThroughBuilder setOutScmfFile(final String outScmfFile) {
        this.outScmfFile = outScmfFile;
        return this;
    }

    public CommandTranslationTable getCommandTranslationTable() {
        return commandTranslationTable;
    }

    public ITcThroughBuilder setCommandTranslationTable(final CommandTranslationTable commandTranslationTable) {
        this.commandTranslationTable = commandTranslationTable;
        return this;
    }

    @Override
    public int getScid() {
        return scid;
    }

    @Override
    public ITcThroughBuilder setScid(final int scid) {
        this.scid = scid;
        return this;
    }

    @Override
    public String getSclkScetFile() {
        return sclkScetFile;
    }

    @Override
    public ITcThroughBuilder setSclkScetFile(final String sclkScetFile) {
        this.sclkScetFile = sclkScetFile;
        return this;
    }

    @Override
    public String getAcqSeq() {
        return acqSeq;
    }

    @Override
    public ITcThroughBuilder setAcqSeq(final String acqSeq) {
        this.acqSeq = acqSeq;
        return this;
    }

    @Override
    public String getIdleSeq() {
        return idleSeq;
    }

    @Override
    public ITcThroughBuilder setIdleSeq(final String idleSeq) {
        this.idleSeq = idleSeq;
        return this;
    }

    @Override
    public String getStartSeq() {
        return this.startSeq;
    }

    @Override
    public ITcThroughBuilder setStartSeq(String startSeq) {
        if (!BinOctHexUtility.isValidHex(startSeq)) {
            throw new IllegalArgumentException("Start sequence must be a valid hex string.");
        }

        this.startSeq = startSeq;
        return this;
    }

    @Override
    public String getTailSeq() {
        return this.tailSeq;
    }

    @Override
    public ITcThroughBuilder setTailSeq(String tailSeq) {
        if (!BinOctHexUtility.isValidHex(tailSeq)) {
            throw new IllegalArgumentException("Tail sequence must be a valid hex string.");
        }

        this.tailSeq = tailSeq;
        return this;
    }

    @Override
    public ITcThroughBuilder addCommandMnemonic(final String commandMnemonic,
                                                final ITcThroughBuilder.InternalCommandMnemonicType commandType) {
        commandWrappers.add(
                InternalCommandWrapper.addCommandMnemonic(new InternalCommandMnemonic(commandMnemonic, commandType)));
        return this;
    }

    @Override
    public ITcThroughBuilder addCommandMnemonic(final String commandMnemonic,
                                                final CommandDefinitionType type) {
        return addCommandMnemonic(commandMnemonic, InternalCommandMnemonicType.convertFromCommandDefinitionType(type));
    }

    @Override
    public ITcThroughBuilder addCommandBytes(final String hexBytes,
                                             final CommandDefinitionType type) {
        return addCommandBytes(hexBytes, InternalCommandMnemonicType.convertFromCommandDefinitionType(type));
    }

    @Override
    public ITcThroughBuilder addCommandBytes(final String hexBytes,
                                             final ITcThroughBuilder.InternalCommandMnemonicType commandType) {
        if (!BinOctHexUtility.isValidHex(hexBytes)) {
            throw new IllegalArgumentException("Hex string is invalid: " + hexBytes);
        }
        commandWrappers.add(
                InternalCommandWrapper.addCommandBytes(new InternalCommandBytes(hexBytes, commandType)));
        return this;
    }

    @Override
    public int getHardwareCommandsVcid() {
        return hardwareCommandsVcid;
    }

    @Override
    public ITcThroughBuilder setHardwareCommandsVcid(final int hardwareCommandsVcid) {
        this.hardwareCommandsVcid = hardwareCommandsVcid;
        return this;
    }

    @Override
    public int getImmediateFswCommandsVcid() {
        return immediateFswCommandsVcid;
    }

    @Override
    public ITcThroughBuilder setImmediateFswCommandsVcid(final int immediateFswCommandsVcid) {
        this.immediateFswCommandsVcid = immediateFswCommandsVcid;
        return this;
    }

    @Override
    public int getSequenceCommandsVcid() {
        return sequenceCommandsVcid;
    }

    @Override
    public ITcThroughBuilder setSequenceCommandsVcid(final int sequenceCommandsVcid) {
        this.sequenceCommandsVcid = sequenceCommandsVcid;
        return this;
    }

    @Override
    public int getMarkerVcid() {
        return markerVcid;
    }

    @Override
    public ITcThroughBuilder setMarkerVcid(final int markerVcid) {
        this.markerVcid = markerVcid;
        return this;
    }

    @Override
    public String getFirstMarkerFrameDataHex() {
        return this.firstMarkerFrameDataHex;
    }

    @Override
    public ITcThroughBuilder setFirstMarkerFrameDataHex(final String firstMarkerFrameDataHex) {
        this.firstMarkerFrameDataHex = firstMarkerFrameDataHex;
        return this;
    }

    @Override
    public String getMiddleMarkerFrameDataHex() {
        return this.middleMarkerFrameDataHex;
    }

    @Override
    public ITcThroughBuilder setMiddleMarkerFrameDataHex(final String middleMarkerFrameDataHex) {
        this.middleMarkerFrameDataHex = middleMarkerFrameDataHex;
        return this;
    }

    @Override
    public String getLastMarkerFrameDataHex() {
        return this.lastMarkerFrameDataHex;
    }

    @Override
    public ITcThroughBuilder setLastMarkerFrameDataHex(final String lastMarkerFrameDataHex) {
        this.lastMarkerFrameDataHex = lastMarkerFrameDataHex;
        return this;
    }

    @Override
    public FrameErrorControlFieldAlgorithm getHardwareCommandsFrameErrorControlFieldAlgorithm() {
        return hardwareCommandsFrameErrorControlFieldAlgorithm;
    }

    @Override
    public ITcThroughBuilder setHardwareCommandsFrameErrorControlFieldAlgorithm(
            final FrameErrorControlFieldAlgorithm hardwareCommandsFrameErrorControlFieldAlgorithm) {
        this.hardwareCommandsFrameErrorControlFieldAlgorithm = hardwareCommandsFrameErrorControlFieldAlgorithm;
        hardwareCommandsFrameErrorControlFieldAlgorithmSet = true;
        hardwareCommandsFrameErrorControlFieldSetToNone = false;
        return this;
    }

    @Override
    public boolean isHardwareCommandsFrameErrorControlFieldSetToNone() {
        return hardwareCommandsFrameErrorControlFieldSetToNone;
    }

    @Override
    public ITcThroughBuilder noHardwareCommandsFrameErrorControlField() {
        hardwareCommandsFrameErrorControlFieldSetToNone = true;
        hardwareCommandsFrameErrorControlFieldAlgorithmSet = false;
        return this;
    }

    @Override
    public FrameErrorControlFieldAlgorithm getImmediateFswCommandsFrameErrorControlFieldAlgorithm() {
        return immediateFswCommandsFrameErrorControlFieldAlgorithm;
    }

    @Override
    public ITcThroughBuilder setImmediateFswCommandsFrameErrorControlFieldAlgorithm(
            final FrameErrorControlFieldAlgorithm immediateFswCommandsFrameErrorControlFieldAlgorithm) {
        this.immediateFswCommandsFrameErrorControlFieldAlgorithm = immediateFswCommandsFrameErrorControlFieldAlgorithm;
        immediateFswCommandsFrameErrorControlFieldAlgorithmSet = true;
        immediateFswCommandsFrameErrorControlFieldSetToNone = false;
        return this;
    }

    @Override
    public boolean isImmediateFswCommandsFrameErrorControlFieldSetToNone() {
        return immediateFswCommandsFrameErrorControlFieldSetToNone;
    }

    @Override
    public ITcThroughBuilder noImmediateFswCommandsFrameErrorControlField() {
        immediateFswCommandsFrameErrorControlFieldSetToNone = true;
        immediateFswCommandsFrameErrorControlFieldAlgorithmSet = false;
        return this;
    }

    @Override
    public FrameErrorControlFieldAlgorithm getSequenceCommandsFrameErrorControlFieldAlgorithm() {
        return sequenceCommandsFrameErrorControlFieldAlgorithm;
    }

    @Override
    public ITcThroughBuilder setSequenceCommandsFrameErrorControlFieldAlgorithm(
            final FrameErrorControlFieldAlgorithm sequenceCommandsFrameErrorControlFieldAlgorithm) {
        this.sequenceCommandsFrameErrorControlFieldAlgorithm = sequenceCommandsFrameErrorControlFieldAlgorithm;
        sequenceCommandsFrameErrorControlFieldAlgorithmSet = true;
        sequenceCommandsFrameErrorControlFieldSetToNone = false;
        return this;
    }

    @Override
    public boolean isSequenceCommandsFrameErrorControlFieldSetToNone() {
        return sequenceCommandsFrameErrorControlFieldSetToNone;
    }

    @Override
    public ITcThroughBuilder noSequenceCommandsFrameErrorControlField() {
        sequenceCommandsFrameErrorControlFieldSetToNone = true;
        sequenceCommandsFrameErrorControlFieldAlgorithmSet = false;
        return this;
    }

    @Override
    public FrameErrorControlFieldAlgorithm getDelimiterFrameErrorControlFieldAlgorithm() {
        return delimiterFrameErrorControlFieldAlgorithm;
    }

    @Override
    public ITcThroughBuilder setDelimiterFrameErrorControlFieldAlgorithm(
            final FrameErrorControlFieldAlgorithm delimiterFrameErrorControlFieldAlgorithm) {
        this.delimiterFrameErrorControlFieldAlgorithm = delimiterFrameErrorControlFieldAlgorithm;
        delimiterFrameErrorControlFieldAlgorithmSet = true;
        delimiterFrameErrorControlFieldSetToNone = false;
        return this;
    }

    @Override
    public boolean isDelimiterFrameErrorControlFieldSetToNone() {
        return delimiterFrameErrorControlFieldSetToNone;
    }

    @Override
    public ITcThroughBuilder noDelimiterFrameErrorControlField() {
        delimiterFrameErrorControlFieldSetToNone = true;
        delimiterFrameErrorControlFieldAlgorithmSet = false;
        return this;
    }

    @Override
    public ITcThroughBuilder setFecfByteLength(int byteLength) {
        this.fecfByteLength = byteLength;
        this.fecfByteLengthSet = true;
        return this;
    }

    @Override
    public int getHardwareCommandsFrameSequenceNumber() {
        return hardwareCommandsFrameSequenceNumber;
    }

    @Override
    public ITcThroughBuilder setHardwareCommandsFrameSequenceNumber(final int hardwareCommandsFrameSequenceNumber) {
        this.hardwareCommandsFrameSequenceNumber = hardwareCommandsFrameSequenceNumber;
        hardwareCommandsFrameSequenceNumberSet = true;
        return this;
    }

    @Override
    public int getImmediateFswCommandsFrameSequenceNumber() {
        return immediateFswCommandsFrameSequenceNumber;
    }

    @Override
    public ITcThroughBuilder setImmediateFswCommandsFrameSequenceNumber(
            final int immediateFswCommandsFrameSequenceNumber) {
        this.immediateFswCommandsFrameSequenceNumber = immediateFswCommandsFrameSequenceNumber;
        immediateFswCommandsFrameSequenceNumberSet = true;
        return this;
    }

    @Override
    public int getSequenceCommandsFrameSequenceNumber() {
        return sequenceCommandsFrameSequenceNumber;
    }

    @Override
    public ITcThroughBuilder setSequenceCommandsFrameSequenceNumber(
            final int sequenceCommandsFrameSequenceNumber) {
        this.sequenceCommandsFrameSequenceNumber = sequenceCommandsFrameSequenceNumber;
        sequenceCommandsFrameSequenceNumberSet = true;
        return this;
    }

    @Override
    public boolean getHardwareCommandsBypassFlag() {
        return hardwareCommandsBypassFlag;
    }

    @Override
    public ITcThroughBuilder setHardwareCommandsBypassFlagOn() {
        hardwareCommandsBypassFlag = true;
        hardwareCommandsBypassFlagSet = true;
        return this;
    }

    @Override
    public ITcThroughBuilder setHardwareCommandsBypassFlagOff() {
        hardwareCommandsBypassFlag = false;
        hardwareCommandsBypassFlagSet = true;
        return this;
    }

    @Override
    public boolean getImmediateFswCommandsBypassFlag() {
        return immediateFswCommandsBypassFlag;
    }

    @Override
    public ITcThroughBuilder setImmediateFswCommandsBypassFlagOn() {
        immediateFswCommandsBypassFlag = true;
        immediateFswCommandsBypassFlagSet = true;
        return this;
    }

    @Override
    public ITcThroughBuilder setImmediateFswCommandsBypassFlagOff() {
        immediateFswCommandsBypassFlag = false;
        immediateFswCommandsBypassFlagSet = true;
        return this;
    }

    @Override
    public boolean getSequenceCommandsBypassFlag() {
        return sequenceCommandsBypassFlag;
    }

    @Override
    public ITcThroughBuilder setSequenceCommandsBypassFlagOn() {
        sequenceCommandsBypassFlag = true;
        sequenceCommandsBypassFlagSet = true;
        return this;
    }

    @Override
    public ITcThroughBuilder setSequenceCommandsBypassFlagOff() {
        sequenceCommandsBypassFlag = false;
        sequenceCommandsBypassFlagSet = true;
        return this;
    }

    @Override
    public boolean getHardwareCommandsControlCommandFlag() {
        return hardwareCommandsControlCommandFlag;
    }

    @Override
    public ITcThroughBuilder setHardwareCommandsControlCommandFlagOn() {
        hardwareCommandsControlCommandFlag = true;
        hardwareCommandsControlCommandFlagSet = true;
        return this;
    }

    @Override
    public ITcThroughBuilder setHardwareCommandsControlCommandFlagOff() {
        hardwareCommandsControlCommandFlag = false;
        hardwareCommandsControlCommandFlagSet = true;
        return this;
    }

    @Override
    public boolean getImmediateFswCommandsControlCommandFlag() {
        return immediateFswCommandsControlCommandFlag;
    }

    @Override
    public ITcThroughBuilder setImmediateFswCommandsControlCommandFlagOn() {
        immediateFswCommandsControlCommandFlag = true;
        immediateFswCommandsControlCommandFlagSet = true;
        return this;
    }

    @Override
    public ITcThroughBuilder setImmediateFswCommandsControlCommandFlagOff() {
        immediateFswCommandsControlCommandFlag = false;
        immediateFswCommandsControlCommandFlagSet = true;
        return this;
    }

    @Override
    public boolean getSequenceCommandsControlCommandFlag() {
        return sequenceCommandsControlCommandFlag;
    }

    @Override
    public ITcThroughBuilder setSequenceCommandsControlCommandFlagOn() {
        sequenceCommandsControlCommandFlag = true;
        sequenceCommandsControlCommandFlagSet = true;
        return this;
    }

    @Override
    public ITcThroughBuilder setSequenceCommandsControlCommandFlagOff() {
        sequenceCommandsControlCommandFlag = false;
        sequenceCommandsControlCommandFlagSet = true;
        return this;
    }

    @Override
    public String getScmfHeaderPreparerName() {
        return scmfHeaderPreparerName;
    }

    @Override
    public ITcThroughBuilder setScmfHeaderPreparerName(final String scmfHeaderPreparerName) {
        this.scmfHeaderPreparerName = scmfHeaderPreparerName;
        return this;
    }

    @Override
    public double getScmfHeaderBitOneRadiationTime() {
        return scmfHeaderBitOneRadiationTime;
    }

    @Override
    public ITcThroughBuilder setScmfHeaderBitOneRadiationTime(final double scmfHeaderBitOneRadiationTime) {
        this.scmfHeaderBitOneRadiationTime = scmfHeaderBitOneRadiationTime;
        return this;
    }

    @Override
    public int getScmfHeaderBitRateIndex() {
        return scmfHeaderBitRateIndex;
    }

    @Override
    public ITcThroughBuilder setScmfHeaderBitRateIndex(final int scmfHeaderBitRateIndex) {
        this.scmfHeaderBitRateIndex = scmfHeaderBitRateIndex;
        return this;
    }

    @Override
    public String getScmfHeaderComment() {
        return scmfHeaderComment;
    }

    @Override
    public ITcThroughBuilder setScmfHeaderComment(final String scmfHeaderComment) {
        this.scmfHeaderComment = scmfHeaderComment;
        return this;
    }

    @Override
    public String getScmfHeaderTitle() {
        return scmfHeaderTitle;
    }

    @Override
    public ITcThroughBuilder setScmfHeaderTitle(final String scmfHeaderTitle) {
        this.scmfHeaderTitle = scmfHeaderTitle;
        return this;
    }

    @Override
    public int getScmfHeaderUntimed() {
        return scmfHeaderUntimed;
    }

    @Override
    public ITcThroughBuilder setScmfHeaderUntimed(final int scmfHeaderUntimed) {
        this.scmfHeaderUntimed = scmfHeaderUntimed;
        return this;
    }

    @Override
    public ITcThroughBuilder setScmfMessageRadiationWindowOpenTime(final long scmfMessageRadiationWindowOpenTime) {
        this.scmfRadiationWindowOpenTime = scmfMessageRadiationWindowOpenTime;
        return this;
    }

    @Override
    public long getScmfMessageRadiationWindowOpenTime() {
        return scmfRadiationWindowOpenTime;
    }

    @Override
    public ITcThroughBuilder setScmfMessageRadiationWindowCloseTime(final long scmfMessageRadiationWindowCloseTime) {
        this.scmfRadiationWindowCloseTime = scmfMessageRadiationWindowCloseTime;
        return this;
    }

    @Override
    public long getScmfMessageRadiationWindowCloseTime() {
        return scmfRadiationWindowCloseTime;
    }

    @Override
    public ITcThroughBuilder setScmfMessageRadiationStartTime(final long scmfMessageRadiationStartTime) {
        this.scmfMessageRadiationStartTime = scmfMessageRadiationStartTime;
        return this;
    }

    @Override
    public long getScmfMessageRadiationStartTime() {
        return scmfMessageRadiationStartTime;
    }

    @Override
    public String getScmfDataFirstRecordComment() {
        return scmfDataFirstRecordComment;
    }

    @Override
    public ITcThroughBuilder setScmfDataFirstRecordComment(final String scmfDataFirstRecordComment) {
        this.scmfDataFirstRecordComment = scmfDataFirstRecordComment;
        return this;
    }

    @Override
    public String getScmfDataMarkerRecordComment() {
        return scmfDataMarkerRecordComment;
    }

    @Override
    public ITcThroughBuilder setScmfDataMarkerRecordComment(final String scmfDataMarkerRecordComment) {
        this.scmfDataMarkerRecordComment = scmfDataMarkerRecordComment;
        return this;
    }

    @Override
    public String getScmfDataActualCommandRecordComment() {
        return scmfDataActualCommandRecordComment;
    }

    @Override
    public ITcThroughBuilder setScmfDataActualCommandRecordComment(final String scmfDataActualCommandRecordComment) {
        this.scmfDataActualCommandRecordComment = scmfDataActualCommandRecordComment;
        return this;
    }

    @Override
    public String getScmfDataLastRecordComment() {
        return scmfDataLastRecordComment;
    }

    @Override
    public ITcThroughBuilder setScmfDataLastRecordComment(final String scmfDataLastRecordComment) {
        this.scmfDataLastRecordComment = scmfDataLastRecordComment;
        return this;
    }

    @Override
    public void buildScmf() throws ThroughTewException {
        // First, check preconditions

        checkPreconditions();

        // Next, start the TcSession
        try (final TranslatingMpsSession tcSession = new TranslatingMpsSession(commandTranslationTable)) {

            if(startSeq != null) {
                tcSession.setCltuStartSequence(startSeq);
            }
            if(tailSeq != null) {
                tcSession.setCltuTailSequence(tailSeq);
            }

            final TcSession.bufitem acqSeqBufitem = tcSession.hexStringToBufferItem(acqSeq);
            log.debug("Acquisition sequence: ",
                    bintoasciihex(acqSeqBufitem.buf, acqSeqBufitem.nbits, 1).replaceAll("\\s", ""));
            final TcSession.bufitem idleSeqBufitem = tcSession.hexStringToBufferItem(idleSeq);
            log.debug("Tail sequence: ",
                    bintoasciihex(idleSeqBufitem.buf, idleSeqBufitem.nbits, 1).replaceAll("\\s", ""));

            /*
            From @timr:

            Create the "hardware command" and "immediate command" session level Tcwrap groups, using the
            create_tcwrap_group_global() method.

            The purpose of a Tcwrap group is to act as a container for certain Telecommand wrapping settings. Below, we
            are creating the groups "tcwg_hardware" and "tcwg_immed". The arguments to the create_tcwrap_group_global()
            method are virtual channel (VC) number, telecommand transfer frame error code algorithm index, also known as
            frame error code (FEC), and frame sequence number (FSN). -1 for FEC indicates that FEC will be fetched from a
            system table for this VC. -1 for FSN indicates that the FSN is to be generated automatically on this VC,
            starting at zero and incrementing with each new frame.

            There are three signatures for this method: (int), (int, int) and (int, int, int). In the first of these, just
            the VC is passed, FEC is defaulted to -1, and FSN is defaulted to 0. In the second, VC and FEC are passed,
            and FSN is defaulted to 0. In third, all three are passed.

            All wrapping settings can be modified with "setter" methods. These are set_vc(), set_fec(), set_fsn(), set_bp
            () and set_cc(). The first three have been explained. set_bp() sets the value of the "bypass" flag, which
            defaults to 1. Frames with the "bypass" flag set bypass the Frame Accountability Reporting (FAR) mechanism
            on board the spacecraft, meaning that the FSN is not examined. set_cc() sets the value of the control command
            flag, which defaults to 0. This flag indicates whether or not this frame contains a "control" command.
             */

            final TcSession.TcwrapGroup hardwareCommandsGroup = initializeHardwareCommandsGroup(tcSession);

            final TcSession.TcwrapGroup immediateFswCommandsGroup = initializeFlightSoftwareCommandsGroup(tcSession);

            final TcSession.TcwrapGroup sequenceCommandsGroup = initializeSequenceCommandsGroup(tcSession);

            // Now override any wrapping parameters
            overrideHardwareWrappingParameters(hardwareCommandsGroup);
            overrideImmediateWrappingParameters(immediateFswCommandsGroup);
            overrideSequenceWrappingParameters(sequenceCommandsGroup);

            // Create the first marker frame and corresponding CLTU
            final TcSession.bufitem firstMarkerBufferItem = createMarkerBufferItem(tcSession, firstMarkerFrameDataHex);
            log.trace("First marker CLTU: " + bintoasciihex(firstMarkerBufferItem.buf,
                    firstMarkerBufferItem.nbits, 0));

            // Create the middle marker frame and corresponding CLTU
            final TcSession.bufitem middleMarkerBufferItem = createMarkerBufferItem(tcSession,
                    middleMarkerFrameDataHex);

            log.trace("Middle marker CLTU: " + bintoasciihex(middleMarkerBufferItem.buf,
                    middleMarkerBufferItem.nbits, 0));

            // Create the last marker frame and corresponding CLTU
            final TcSession.bufitem lastMarkerBufferItem = createMarkerBufferItem(tcSession, lastMarkerFrameDataHex);

            log.trace("Last marker CLTU: " + bintoasciihex(lastMarkerBufferItem.buf,
                    lastMarkerBufferItem.nbits, 0));

            // Loop through all mnemonic strings, performing TEW

            for (final InternalCommandWrapper wrapper : commandWrappers) {
                switch (wrapper.getWrapperType()) {
                    case COMMAND_BYTES:
                        addCommandBytesToWrapGroup(hardwareCommandsGroup, immediateFswCommandsGroup,
                                sequenceCommandsGroup, wrapper);
                        log.trace("HexstrFrmTcwrapToCltu ", wrapper.getCommandContents(), " to ",
                                wrapper.getCommandType(), " TcwrapGroup");
                        break;
                    case COMMAND_MNEMONIC:
                        addCommandMnemonicToWrapGroup(hardwareCommandsGroup, immediateFswCommandsGroup,
                                sequenceCommandsGroup, wrapper);
                        log.trace("MneFrmTcwrapToCltu ", wrapper.getCommandContents(), " to ", wrapper.getCommandType(),
                                " TcwrapGroup");
                        break;
                    default:
                }
            }

            // check for command translation errors
            checkCommandBufferErrors(tcSession.getCommandBufferList());


            // Generate the SCMF
            final int missionId = tcSession.getMissionId();
            final int scid      = tcSession.getScid();
            log.debug("scmf_open_write_w(", outScmfFile, ", ", missionId, ", ", scid, ")");

            scmfWriter.setScid(scid);
            scmfWriter.setMissionId(missionId);
            scmfWriter.setOutScmfFile(outScmfFile);
            // Set SCMF's header
            scmfWriter.setScmfHeaderPreparerName(scmfHeaderPreparerName);
            scmfWriter.setScmfHeaderBitOneRadiationTime(scmfHeaderBitOneRadiationTime);
            scmfWriter.setScmfHeaderBitRateIndex(scmfHeaderBitRateIndex);
            scmfWriter.setScmfHeaderComment(scmfHeaderComment);
            scmfWriter.setScmfHeaderTitle(scmfHeaderTitle);
            scmfWriter.setScmfHeaderUntimed(scmfHeaderUntimed);
            scmfWriter.setScmfMessageRadiationStartTime(scmfMessageRadiationStartTime);
            scmfWriter.setScmfMessageRadiationWindowOpenTime(scmfRadiationWindowOpenTime);
            scmfWriter.setScmfMessageRadiationWindowCloseTime(scmfRadiationWindowCloseTime);
            log.debug("scmfHeaderPreparerName=", scmfHeaderPreparerName);
            log.debug("scmfHeaderBitOneRadiationTime=", scmfHeaderBitOneRadiationTime);
            log.debug("scmfHeaderBitRateIndex=", scmfHeaderBitRateIndex);
            log.debug("scmfHeaderComment=", scmfHeaderComment);
            log.debug("scmfHeaderTitle=", scmfHeaderTitle);
            log.debug("scmfHeaderUntimed=", scmfHeaderUntimed);
            log.debug("scmfMessageRadiationStartTime=", scmfMessageRadiationStartTime);
            log.debug("scmfMessageRadiationWindowOpenTime=", scmfRadiationWindowOpenTime);
            log.debug("scmfMessageRadiationWindowCloseTime=", scmfRadiationWindowCloseTime);

            boolean first = true;

            // Get the global CLTU list
            List<TcSession.bufitem> cltus = tcSession.getLinearCltuBufferList();

            if (fecfByteLengthSet) {
                cltus = overrideFecfLength(cltus);
            }

            // Before looping, prepare first and last segments
            firstMarkerBufferItem.prepend(acqSeqBufitem);
            lastMarkerBufferItem.append(idleSeqBufitem);

            // Count how many data records have been written
            int counter = 0;

            for (final TcSession.bufitem bufferItem : cltus) {

                if (first) {
                    scmfWriter.addDataRecord(
                            UplinkUtils.bintoasciihex(firstMarkerBufferItem.buf, firstMarkerBufferItem.nbits, 0));
                    scmfWriter.setCurrentDataRecordComment(scmfDataFirstRecordComment);
                    first = false;
                    log.trace("Setting first data record: ", scmfDataFirstRecordComment);
                } else {
                    scmfWriter.addDataRecord(
                            UplinkUtils.bintoasciihex(middleMarkerBufferItem.buf, middleMarkerBufferItem.nbits, 0));
                    scmfWriter.setCurrentDataRecordComment(scmfDataMarkerRecordComment);
                    log.trace("Setting marker data record: ", scmfDataMarkerRecordComment);
                }

                counter++;
                log.trace("Wrote set data record #", counter, " to SCMF");

                scmfWriter.addDataRecord(UplinkUtils.bintoasciihex(bufferItem.buf, bufferItem.nbits, 0));
                scmfWriter.setCurrentDataRecordComment(scmfDataActualCommandRecordComment);
                log.trace("Setting actual command data record: ", scmfDataActualCommandRecordComment);

                counter++;
                log.trace("Wrote set data record #", counter, " to SCMF");
            }

            scmfWriter
                    .addDataRecord(UplinkUtils.bintoasciihex(lastMarkerBufferItem.buf, lastMarkerBufferItem.nbits, 0));
            scmfWriter.setCurrentDataRecordComment(scmfDataLastRecordComment);
            log.trace("Setting last data record: ", scmfDataLastRecordComment);

            counter++;
            log.trace("Wrote set data record #", counter, " (last) to SCMF");

            scmfWriter.writeScmf();

        } catch (final RuntimeException re) {
            throw new ThroughTewException("UplinkUtils threw exception: " + re.getMessage(), re);
        } catch (final ScmfWrapUnwrapException | CommandParseException | FrameWrapUnwrapException | CltuEndecException e) {
            throw new ThroughTewException(e);
        }
    }

    private List<TcSession.bufitem> overrideFecfLength(List<TcSession.bufitem> cltus) throws
                                                                                      FrameWrapUnwrapException,
                                                                                      CltuEndecException,
                                                                                      ThroughTewException {
        List<TcSession.bufitem> overrideCltuList = new ArrayList<>(cltus.size());

        for (TcSession.bufitem cltuBufferItem : cltus) {
            try (MpsSession session = new MpsSession(scid)) {
                TcSession.cltuitem    cltuItem          = session.getCltuItem(cltuBufferItem);
                TcSession.frmitem     originalFrameItem = session.getFrameItem(cltuItem);
                if (originalFrameItem.frmfecdata == null && originalFrameItem.frmfeclen == 0) {
                    overrideCltuList.add(cltuBufferItem);
                } else {
                    // get a configured wrapping group
                    int                   vcid       = originalFrameItem.frmvc;
                    TcSession.TcwrapGroup localGroup = null;
                    if (vcid == hardwareCommandsVcid) {
                        localGroup = initializeHardwareCommandsGroup(session);
                    } else if (vcid == immediateFswCommandsVcid) {
                        localGroup = initializeFlightSoftwareCommandsGroup(session);
                    } else if (vcid == sequenceCommandsVcid) {
                        localGroup = initializeSequenceCommandsGroup(session);
                    }

                    // take the data from the original frame
                    String dataHex = UplinkUtils
                            .bintoasciihex(originalFrameItem.data, originalFrameItem.datalen << 3, 0);

                    // correct the frame length
                    int originalFrameLength = originalFrameItem.frmlen;
                    // real frame length should be (original length) - (default FECF length) + (new FECF length)
                    int correctedFrameLength = originalFrameLength - 2 + fecfByteLength;

                    // override frame length, and generate a new frame
                    localGroup.set_flen_override(correctedFrameLength);
                    localGroup.HexstrFrmTcwrapToFrm(dataHex);
                    TcSession.bufitem correctedLengthBuffer = localGroup.getTcwrapBuffer(0);
                    String correctedFrameHex = UplinkUtils
                            .bintoasciihex(correctedLengthBuffer.buf, correctedLengthBuffer.nbits, 0);

                    // Get the last 2 bytes, the adjusted FEC
                    String fecHex = correctedFrameHex.substring(correctedFrameHex.length() - 4);

                    // pad the FEC string to fill bytes, override the FEC on the wrap group, generate a new frame
                    String paddedFecHex = org.apache.commons.lang3.StringUtils.leftPad(fecHex, fecfByteLength * 2, '0');
                    localGroup.set_fec_override(paddedFecHex);
                    localGroup.HexstrFrmTcwrapToFrm(dataHex);

                    TcSession.bufitem correctedFrameBuffer = localGroup.getTcwrapBuffer(1);

                    String frameHex = UplinkUtils
                            .bintoasciihex(correctedFrameBuffer.buf, correctedFrameBuffer.nbits, 0);

                    // generate CLTU from frame hex
                    TcSession.cltuitem overrideCltu = session
                            .wrapFrameToCltu(BinOctHexUtility.toBytesFromHex("0x" + frameHex), vcid);
                    TcSession.bufitem overrideCltuBufitem = session.encodeCltu(overrideCltu);
                    overrideCltuList.add(overrideCltuBufitem);
                }
            }
        }

        return overrideCltuList;
    }

    /**
     * Add command bytes to a wrap group
     *
     * @param hardwareCommandsGroup     hardware tc wrap group
     * @param immediateFswCommandsGroup immediate fsw tc wrap group
     * @param sequenceCommandsGroup     sequence tc wrap group
     * @param wrapper                   internal command wrapper
     */
    private void addCommandBytesToWrapGroup(TcSession.TcwrapGroup hardwareCommandsGroup,
                                            TcSession.TcwrapGroup immediateFswCommandsGroup,
                                            TcSession.TcwrapGroup sequenceCommandsGroup,
                                            InternalCommandWrapper wrapper) {
        switch (wrapper.getCommandType()) {
            case HARDWARE:
                hardwareCommandsGroup.HexstrFrmTcwrapToCltu(wrapper.getCommandContents());
                break;
            case IMMEDIATE_FSW:
                immediateFswCommandsGroup.HexstrFrmTcwrapToCltu(wrapper.getCommandContents());
                break;
            case SEQUENCE:
                sequenceCommandsGroup.HexstrFrmTcwrapToCltu(wrapper.getCommandContents());
                break;
            default:
        }
    }

    /**
     * Add command mnemonics to a tc wrap group
     *
     * @param hardwareCommandsGroup     hardware tc wrap group
     * @param immediateFswCommandsGroup immediate fsw tc wrap group
     * @param sequenceCommandsGroup     sequence tc wrap group
     * @param wrapper                   internal command wrapper
     */
    private void addCommandMnemonicToWrapGroup(TcSession.TcwrapGroup hardwareCommandsGroup,
                                               TcSession.TcwrapGroup immediateFswCommandsGroup,
                                               TcSession.TcwrapGroup sequenceCommandsGroup,
                                               InternalCommandWrapper wrapper) {
        switch (wrapper.getCommandType()) {
            case HARDWARE:
                hardwareCommandsGroup.MneFrmTcwrapToCltu(wrapper.getCommandContents(), true);
                break;
            case IMMEDIATE_FSW:
                immediateFswCommandsGroup.MneFrmTcwrapToCltu(wrapper.getCommandContents(), true);
                break;
            case SEQUENCE:
                sequenceCommandsGroup.MneFrmTcwrapToCltu(wrapper.getCommandContents(), true);
                break;
            default:
        }
    }

    private void checkCommandBufferErrors(final List<TcSession.bufitem> commandBufferList) throws
                                                                                           CommandParseException {
        final List<String> errorMessages = new ArrayList<>();
        for (final TcSession.bufitem buffer : commandBufferList) {
            if (buffer.nerrors > 0) {
                errorMessages.add(buffer.errmsg);
            }
        }
        if (!errorMessages.isEmpty()) {
            throw new CommandParseException(StringUtils.join(errorMessages, " "));
        }
    }

    private TcSession.bufitem createMarkerBufferItem(final TranslatingMpsSession tcSession,
                                                     final String markerFrameDataHex) throws
                                                                                      ThroughTewException {
        TcSession.TcwrapGroup markerGroup;

        if(delimiterFrameErrorControlFieldAlgorithmSet) {
            final int fecType = getDelimiterFecType();
            markerGroup = tcSession.createLocalWrapGroup(markerVcid, fecType);
        } else {
            markerGroup = tcSession.createLocalWrapGroup(markerVcid);
        }
        markerGroup.HexstrFrmTcwrapToCltu(BinOctHexUtility.stripHexPrefix(markerFrameDataHex));
        final TcSession.bufitem markerBufferItem = markerGroup.getTcwrapBuffer(0);

        if (markerBufferItem.nerrors > 0) {
            throw new ThroughTewException(markerBufferItem.nerrors + " errors from HexstrFrmTcwrapToCltu(\""
                    + BinOctHexUtility.stripHexPrefix(
                    markerFrameDataHex) + "\"): nbits=" + markerBufferItem.nbits + ", errmsg=" + markerBufferItem.errmsg);
        }

        return markerBufferItem;
    }

    private void overrideHardwareWrappingParameters(final TcSession.TcwrapGroup hardwareCommandsGroup) {
        if (hardwareCommandsBypassFlagSet) {

            if (hardwareCommandsBypassFlag) {
                hardwareCommandsGroup.set_bp();
            } else {
                hardwareCommandsGroup.reset_bp();
            }

        }

        if (hardwareCommandsControlCommandFlagSet) {

            if (hardwareCommandsControlCommandFlag) {
                hardwareCommandsGroup.set_cc();
            } else {
                hardwareCommandsGroup.reset_cc();
            }

        }
    }

    private void overrideImmediateWrappingParameters(final TcSession.TcwrapGroup immediateFswCommandsGroup) {
        if (immediateFswCommandsBypassFlagSet) {

            if (immediateFswCommandsBypassFlag) {
                immediateFswCommandsGroup.set_bp();
            } else {
                immediateFswCommandsGroup.reset_bp();
            }

        }

        if (immediateFswCommandsControlCommandFlagSet) {

            if (immediateFswCommandsControlCommandFlag) {
                immediateFswCommandsGroup.set_cc();
            } else {
                immediateFswCommandsGroup.reset_cc();
            }

        }
    }

    private void overrideSequenceWrappingParameters(final TcSession.TcwrapGroup sequenceCommandsGroup) {
        if (sequenceCommandsBypassFlagSet) {

            if (sequenceCommandsBypassFlag) {
                sequenceCommandsGroup.set_bp();
            } else {
                sequenceCommandsGroup.reset_bp();
            }

        }

        if (sequenceCommandsControlCommandFlagSet) {

            if (sequenceCommandsControlCommandFlag) {
                sequenceCommandsGroup.set_cc();
            } else {
                sequenceCommandsGroup.reset_cc();
            }

        }
    }

    private boolean isAutoconfigureHardwareWrapGroup() {
        return !hardwareCommandsFrameErrorControlFieldAlgorithmSet
                && !hardwareCommandsFrameErrorControlFieldSetToNone
                && !hardwareCommandsFrameSequenceNumberSet;
    }

    private boolean isAutoconfigureImmediateWrapGroup() {
        return !immediateFswCommandsFrameErrorControlFieldAlgorithmSet
                && !immediateFswCommandsFrameErrorControlFieldSetToNone
                && !immediateFswCommandsFrameSequenceNumberSet;
    }

    private boolean isAutoconfigureSequenceWrapGroup() {
        return !sequenceCommandsFrameErrorControlFieldAlgorithmSet
                && !sequenceCommandsFrameErrorControlFieldSetToNone
                && !sequenceCommandsFrameSequenceNumberSet;
    }

    private TcSession.TcwrapGroup initializeHardwareCommandsGroup(final AMpsSession tcSession) throws
                                                                                                         ThroughTewException {
        final TcSession.TcwrapGroup hardwareCommandsGroup;
        final String                beginLogMessage = "Created hardware commands TcwrapGroup with VCID " + hardwareCommandsVcid;

        if (isAutoconfigureHardwareWrapGroup()) {
            hardwareCommandsGroup = tcSession.createGlobalWrapGroup(hardwareCommandsVcid);
            log.trace(beginLogMessage);
        } else {
            final int    fecType   = getHardwareFecType();
            final String fecString = getHardwareFecAlgorithmLogString(fecType);

            if (!hardwareCommandsFrameSequenceNumberSet) {
                hardwareCommandsGroup = tcSession.createGlobalWrapGroup(hardwareCommandsVcid, fecType);
                log.trace(beginLogMessage, " and ", fecString);
            } else {
                hardwareCommandsGroup = tcSession
                        .createGlobalWrapGroup(hardwareCommandsVcid, fecType, hardwareCommandsFrameSequenceNumber);
                log.trace(beginLogMessage, ", ", fecString, ", and frame sequence number ",
                        hardwareCommandsFrameSequenceNumber);
            }
        }

        if (hardwareCommandsGroup == null) {
            throw new ThroughTewException(
                    "Unexpected state - cannot determine how to create hardware commands TcwrapGroup");
        }

        return hardwareCommandsGroup;
    }

    private TcSession.TcwrapGroup initializeFlightSoftwareCommandsGroup(final AMpsSession tcSession) throws
            ThroughTewException {
        final TcSession.TcwrapGroup immediateFswCommandsGroup;
        final String                beginLogMessage = "Created immediate FSW commands TcwrapGroup with VCID " + immediateFswCommandsVcid;
        if (isAutoconfigureImmediateWrapGroup()) {
            immediateFswCommandsGroup = tcSession.createGlobalWrapGroup(immediateFswCommandsVcid, -1, -1);
            log.trace(beginLogMessage);
        } else {
            final int    fecType   = getImmediateFswFecType();
            final String fecString = getImmediateFswFecAlgorithmLogString(fecType);

            if (!immediateFswCommandsFrameSequenceNumberSet) {
                immediateFswCommandsGroup = tcSession.createGlobalWrapGroup(immediateFswCommandsVcid, fecType);
                log.trace(beginLogMessage, fecString);
            } else {
                immediateFswCommandsGroup = tcSession.createGlobalWrapGroup(immediateFswCommandsVcid, fecType,
                        immediateFswCommandsFrameSequenceNumber);
                log.trace(beginLogMessage, ", ", fecString, ", and frame sequence number ",
                        immediateFswCommandsFrameSequenceNumber);
            }
        }

        if (immediateFswCommandsGroup == null) {
            throw new ThroughTewException(
                    "Unexpected state - cannot determine how to create immediate FSW commands TcwrapGroup");
        }
        return immediateFswCommandsGroup;
    }

    private TcSession.TcwrapGroup initializeSequenceCommandsGroup(final AMpsSession tcSession) throws
            ThroughTewException {
        final TcSession.TcwrapGroup sequenceCommandsGroup;
        final String                beginLogMessage = "Created sequence commands TcwrapGroup with VCID " + sequenceCommandsVcid;
        if (isAutoconfigureSequenceWrapGroup()) {
            sequenceCommandsGroup = tcSession.createGlobalWrapGroup(sequenceCommandsVcid, -1, -1);
            log.trace(beginLogMessage);
        } else {
            final int    fecType   = getSequenceFecType();
            final String fecString = getSequenceFecAlgorithmLogString(fecType);

            if (!sequenceCommandsFrameSequenceNumberSet) {
                sequenceCommandsGroup = tcSession.createGlobalWrapGroup(sequenceCommandsVcid, fecType);
                log.trace(beginLogMessage, fecString);
            } else {
                sequenceCommandsGroup = tcSession.createGlobalWrapGroup(sequenceCommandsVcid, fecType,
                        sequenceCommandsFrameSequenceNumber);
                log.trace(beginLogMessage, ", ", fecString, ", and frame sequence number ",
                        sequenceCommandsFrameSequenceNumber);
            }
        }

        if (sequenceCommandsGroup == null) {
            throw new ThroughTewException(
                    "Unexpected state - cannot determine how to create immediate FSW commands TcwrapGroup");
        }
        return sequenceCommandsGroup;
    }

    private String getHardwareFecAlgorithmLogString(final int fecType) {
        return getFecAlgorithmLogString(fecType, hardwareCommandsFrameErrorControlFieldAlgorithmSet,
                hardwareCommandsFrameErrorControlFieldAlgorithm);
    }

    private String getImmediateFswFecAlgorithmLogString(final int fecType) {
        return getFecAlgorithmLogString(fecType, immediateFswCommandsFrameErrorControlFieldAlgorithmSet,
                immediateFswCommandsFrameErrorControlFieldAlgorithm);
    }

    private String getSequenceFecAlgorithmLogString(final int fecType) {
        return getFecAlgorithmLogString(fecType, sequenceCommandsFrameErrorControlFieldAlgorithmSet,
                sequenceCommandsFrameErrorControlFieldAlgorithm);
    }

    private String getFecAlgorithmLogString(final int fecType, final boolean fecFieldAlgorithmSet,
                                            final FrameErrorControlFieldAlgorithm algorithm) {
        final String fecString;
        if (fecType == TC_FEC_NONE) {
            fecString = "no";
        } else if (fecFieldAlgorithmSet) {
            fecString = algorithm.name();
        } else {
            fecString = "default";
        }
        return fecString + " FECF algorithm";
    }

    private int getHardwareFecType() {
        return determineFecType(hardwareCommandsFrameErrorControlFieldSetToNone,
                hardwareCommandsFrameErrorControlFieldAlgorithmSet,
                hardwareCommandsFrameErrorControlFieldAlgorithm);
    }

    private int getImmediateFswFecType() {
        return determineFecType(immediateFswCommandsFrameErrorControlFieldSetToNone,
                immediateFswCommandsFrameErrorControlFieldAlgorithmSet,
                immediateFswCommandsFrameErrorControlFieldAlgorithm);
    }

    private int getSequenceFecType() {
        return determineFecType(sequenceCommandsFrameErrorControlFieldSetToNone,
                sequenceCommandsFrameErrorControlFieldAlgorithmSet,
                sequenceCommandsFrameErrorControlFieldAlgorithm);
    }

    private int getDelimiterFecType() {
        return determineFecType(delimiterFrameErrorControlFieldSetToNone,
                delimiterFrameErrorControlFieldAlgorithmSet,
                delimiterFrameErrorControlFieldAlgorithm);
    }

    private int determineFecType(final boolean fecFieldSetToNone, final boolean fecFieldAlgorithmSet,
                                 final FrameErrorControlFieldAlgorithm algorithm) {
        int fecType = -1; // default
        if (fecFieldSetToNone) {
            fecType = TC_FEC_NONE;
        } else if (fecFieldAlgorithmSet) {
            fecType = algorithm == EACSUM55AA ? TC_FEC_EACSUM55AA : TC_FEC_SDLC;
        }
        return fecType;
    }

    private void checkPreconditions() throws ThroughTewException {
        if (commandTranslationTable == null) {
            throw new ThroughTewException("Command translation table must be set first");
        }

        if (commandTranslationTable.getCommandTranslationPointer() == null) {
            throw new ThroughTewException("Command translation table's pointer cannot be null");
        }

        if (scid < 0) {
            throw new ThroughTewException("Spacecraft ID must be set first");
        }

        if (hardwareCommandsVcid < 0) {
            throw new ThroughTewException("Hardware command VCID must be set first");
        }

        if (immediateFswCommandsVcid < 0) {
            throw new ThroughTewException("Immediate FSW command VCID must be set first");
        }

        if (sequenceCommandsVcid < 0) {
            throw new ThroughTewException("Sequence command VCID must be set first");
        }

        if (markerVcid < 0) {
            throw new ThroughTewException("Marker VCID must be set first");
        }

        if (firstMarkerFrameDataHex == null) {
            throw new ThroughTewException("First marker frame data hex string must be set first");
        }

        if (middleMarkerFrameDataHex == null) {
            throw new ThroughTewException("Middle marker frame data hex string must be set first");
        }

        if (lastMarkerFrameDataHex == null) {
            throw new ThroughTewException("Last marker frame data hex string must be set first");
        }

        if (sclkScetFile == null) {
            throw new ThroughTewException("SCLK/SCET file path must be set first");
        }

        if (acqSeq == null) {
            throw new ThroughTewException("Acquisition sequence must be set first");
        }

        if (idleSeq == null) {
            throw new ThroughTewException("Idle sequence must be set first");
        }

        if (outScmfFile == null) {
            throw new ThroughTewException("Output SCMF file name must be set first");
        }
    }

    /**
     * Internal abstract class for containing internal command objects
     */
    private abstract static class AInternalCommand {
        protected final InternalCommandMnemonicType commandType;

        AInternalCommand(final InternalCommandMnemonicType commandType) {
            this.commandType = commandType;
        }

        /**
         * Get the command type (ie, hardware, immediate flight software, or sequence)
         *
         * @return command type
         */
        public InternalCommandMnemonicType getCommandType() {
            return commandType;
        }

        /**
         * Get the contents of this internal command object (hex bytes or mnemonic string, depending on subclass)
         *
         * @return command object contents
         */
        public abstract String getCommandContents();
    }

    /**
     * Internal class for containing command mnemonics.
     */
    private static class InternalCommandMnemonic extends AInternalCommand {
        private final String                                          mnemonic;

        InternalCommandMnemonic(final String mnemonic,
                                final InternalCommandMnemonicType commandType) {
            super(commandType);
            this.mnemonic = mnemonic;
        }

        public String getMnemonic() {
            return mnemonic;
        }

        @Override
        public String getCommandContents() {
            return getMnemonic();
        }

    }

    /**
     * Internal class for containing command bytes.
     */
    private static class InternalCommandBytes extends AInternalCommand {
        private final String hexBytes;

        /**
         * Constructor
         *
         * @param hexBytes hex command bytes
         * @param commandType command type
         */
        InternalCommandBytes(final String hexBytes,
                             final InternalCommandMnemonicType commandType) {
            super(commandType);
            this.hexBytes = hexBytes;
        }

        /**
         * Get the command hex bytes
         *
         * @return command hex bytes
         */
        String getHexBytes() {
            return hexBytes;
        }

        @Override
        public String getCommandContents() {
            return getHexBytes();
        }
    }

    /**
     * Wrapper class for internal command mnemonics. Allows us to deal with command mnemonics or command bytes in a
     * polymorphic way. Please use the static factory methods to generate new instances.
     *
     * @param <T> Subclass of AInternalCommand
     * @since R8.2
     */
    private static class InternalCommandWrapper<T extends AInternalCommand> {

        /**
         * Wrapper type
         */
        enum WrapperType {
            COMMAND_BYTES,
            COMMAND_MNEMONIC
        }

        private final WrapperType wrapperType;
        private final T           commandProxy;

        /**
         * Private constructor. Please use the static factory methods.
         *
         * @param wrapperType wrapper type
         * @param commandProxy command proxy object
         */
        private InternalCommandWrapper(final WrapperType wrapperType, final T commandProxy) {
            this.wrapperType = wrapperType;
            this.commandProxy = commandProxy;
        }

        /**
         * Retrieve the proxied command object
         *
         * @return the proxied command object
         */
        T getCommandProxy() {
            return this.commandProxy;
        }

        /**
         * Static factory method
         *
         * @param commandMnemonic internal command mnemonic
         * @return command wrapper
         */
        static InternalCommandWrapper addCommandMnemonic(final InternalCommandMnemonic commandMnemonic) {
            return new InternalCommandWrapper<>(WrapperType.COMMAND_MNEMONIC, commandMnemonic);
        }

        /**
         * Static factory method
         *
         * @param commandBytes internal command bytes
         * @return command wrapper
         */
        static InternalCommandWrapper addCommandBytes(final InternalCommandBytes commandBytes) {

            return new InternalCommandWrapper<>(WrapperType.COMMAND_BYTES, commandBytes);
        }

        /**
         * Get the wrapper type enum
         *
         * @return wrapper type
         */
        WrapperType getWrapperType() {
            return this.wrapperType;
        }

        /**
         * Get the command contents (ie, bytes or mnemonic string)
         *
         * @return command bytes in hex, or the mnemonic string
         */
        String getCommandContents() {
            return this.commandProxy.getCommandContents();
        }

        /**
         * Get the command type (ie, hardware, immediate fsw, or sequence
         *
         * @return command type
         */
        InternalCommandMnemonicType getCommandType() {
            return this.commandProxy.getCommandType();
        }
    }

}
