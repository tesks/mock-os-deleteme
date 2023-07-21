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
package jpl.gds.tc.api.through;

import jpl.gds.dictionary.api.command.CommandDefinitionType;
import jpl.gds.tc.api.frame.FrameErrorControlFieldAlgorithm;

/**
 * {@code ITcThroughBuilder} is used to construct translated, encoded, and wrapped (TEW) data (SCMF) for telecommanding
 * . It is modeled after the "gold standard" MPSA UplinkUtils telecommand construction. The name includes the word
 * "through" in order to differentiate this from the typical stepwise telecommand construction. The through builder
 * consumes command mnemonics and performs all TEW behind the scenes, in the MPSA UplinkUtils pipeline.
 *
 * @since 8.2.0
 *
 * MPCS-11080 - 09/18/19  - updated to add support for sequence directive commands
 * MPCS-11285 - 09/24/19  - changed tailSeq to idleSeq to match AMPCS terminology in other classes.
 *                                        added get and set for startSeq and tailSeq.
 */
public interface ITcThroughBuilder {

    /**
     * Retrieve the output SCMF file name. (This must be set first.)
     *
     * @return output SCMF file name
     */
    String getOutScmfFile();

    /**
     * Set the output SCMF file name.
     *
     * @param outScmfFile desired outpupt SCMF file name
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setOutScmfFile(String outScmfFile);

    /**
     * Retrieve the spacecraft ID number. (This must be set first.)
     *
     * @return spacecraft ID number
     */
    int getScid();

    /**
     * Set the spacecraft ID number.
     *
     * @param scid spacecraft ID number to set
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setScid(int scid);

    /**
     * Retrieve the SCLK/SCET file. (This must be set first.)
     *
     * @return SCLK/SCET file
     */
    String getSclkScetFile();

    /**
     * Set the SCLK/SCET file.
     *
     * @param sclkScetFile SCLK/SCET file to set
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setSclkScetFile(String sclkScetFile);

    /**
     * Retrieve the acquisition sequence hexadecimal string. (This must be set first.)
     * These are the bytes that are prepended to the FIRST PLOP CLTU
     *
     * @return acquisition sequence hexadecimal string
     */
    String getAcqSeq();

    /**
     * Set the acquisition sequence hexadecimal string.
     * These are the bytes that are prepended to the FIRST PLOP CLTU
     *
     * @param acqSeq acquisition sequence hexadecimal string to set
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setAcqSeq(String acqSeq);

    /**
     * Retrieve the idle sequence hexadecimal string. (This must be set first.)
     * These are the bytes that are appended to the FINAL PLOP CLTU
     *
     * @return idle sequence hexadecimal string
     */
    String getIdleSeq();

    /**
     * Set the tail sequence hexadecimal string.
     * These are the bytes that are appended to the FINAL PLOP CLTU
     *
     * @param idleSeq idle sequence hexadecimal string to set
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setIdleSeq(String idleSeq);

    /**
     * Retrieve the start sequence hexadecimal string. (This must be set first.)
     * These are the bytes that are prepended to every CLTU
     *
     * @return start sequence hexadecimal string
     */
    String getStartSeq();

    /**
     * Set the start sequence hexadecimal string.
     * These are the bytes that are prepended to every CLTU
     *
     * @param startSeq idle sequence hexadecimal string to set
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setStartSeq(String startSeq);

    /**
     * Retrieve the tail sequence hexadecimal string. (This must be set first.)
     * These are the bytes that are appended to every CLTU
     *
     * @return tail sequence hexadecimal string
     */
    String getTailSeq();

    /**
     * Set the tail sequence hexadecimal string.
     * These are the bytes that are appended to every CLTU
     *
     * @param tailSeq idle sequence hexadecimal string to set
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setTailSeq(String tailSeq);

    /**
     * Add a command mnemonic string to the queue to be translated, encoded, and wrapped (TEW). Also designate
     * whether the command is either a hardware or immediate flight software command type.
     *
     * @param commandMnemonic command mnemonic string to add to the sequence
     * @param commandType     type of command
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder addCommandMnemonic(String commandMnemonic,
                                           InternalCommandMnemonicType commandType);

    /**
     * Add a command mnemonic string to the queue to be translated, encoded, and wrapped (TEW). Also designate
     * the type of command with the AMPCS CommandDefinitionType.
     *
     * @param commandMnemonic command mnemonic string to add to the sequence
     * @param type     type of command as per AMPCS
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder addCommandMnemonic(final String commandMnemonic,
                                                final CommandDefinitionType type);

    /**
     * Add a command bytes hex string to the queue to be wrapped. Also designate the type of command with the AMPCS
     * CommandDefinitiontype.
     *
     * @param hexBytes Command hex bytes to add to the sequence
     * @param type     type of command as per AMPCS
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder addCommandBytes(final String hexBytes,
                                      final CommandDefinitionType type);

    /**
     * Add a command bytes hex string to the queue to be wrapped.  Also designate whether the command is either a
     * hardware or immediate flight software command type.
     *
     * @param hexBytes    Command hex bytes to add to the sequence
     * @param commandType command type
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder addCommandBytes(final String hexBytes,
                                      InternalCommandMnemonicType commandType);

    /**
     * Retrieve the VCID configured in this builder for hardware commands (may be ORed with execution string ID, if
     * any).
     *
     * @return VCID configured in this builder for hardware commands (may be ORed with execution string ID, if any)
     */
    int getHardwareCommandsVcid();

    /**
     * Set the VCID in this builder for hardware commands (may be ORed with execution string ID, if any).
     *
     * @param hardwareCommandsVcid VCID to set in this builder for hardware commands (may be ORed with execution
     *                             string ID, if any)
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setHardwareCommandsVcid(int hardwareCommandsVcid);

    /**
     * Retrieve the VCID configured in this builder for immediate flight software commands (may be ORed with
     * execution string ID, if any).
     *
     * @return VCID configured in this builder for immediate flight software commands (may be ORed with execution
     * string ID, if any)
     */
    int getImmediateFswCommandsVcid();

    /**
     * Set the VCID in this builder for immediate flight software commands (may be ORed with execution string ID, if
     * any).
     *
     * @param immediateFswCommandsVcid VCID to set in this builder for immediate flight software commands (may be
     *                                 ORed with execution string ID, if any)
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setImmediateFswCommandsVcid(int immediateFswCommandsVcid);

    /**
     * Retrieve the VCID configured in this builder for sequence commands (may be ORed with
     * execution string ID, if any).
     *
     * @return VCID configured in this builder for sequence commands (may be ORed with execution
     * string ID, if any)
     */
    int getSequenceCommandsVcid();

    /**
     * Set the VCID in this builder for sequence commands (may be ORed with execution string ID, if
     * any).
     *
     * @param sequenceCommandsVcid VCID to set in this builder for sequence commands (may be
     *                                 ORed with execution string ID, if any)
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setSequenceCommandsVcid(int sequenceCommandsVcid);

    /**
     * Retrieve the VCID configured in this builder for marker frames (may be ORed with execution string ID, if any).
     *
     * @return VCID configured in this builder for marker frames (may be ORed with execution string ID, if any)
     */
    int getMarkerVcid();

    /**
     * Set the VCID in this builder for marker frames (may be ORed with execution string ID, if any).
     *
     * @param markerVcid VCID to set in this builder for marker frames (may be ORed with execution string ID, if any)
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setMarkerVcid(int markerVcid);

    /**
     * Retrieve the transfer frame data configured in this builder as a hexadecimal string, for the first marker
     * frame of a session.
     *
     * @return transfer frame data value configured in this builder for the first marker frame in a session, as
     * hexadecimal string
     */
    String getFirstMarkerFrameDataHex();

    /**
     * Set the data value for the transfer frame that serves as the first marker frame of a session, represented as a
     * hexadecimal string.
     *
     * @param firstMarkerFrameDataHex hexadecimal string of the transfer frame data that serves as the first marker
     *                                frame of a session
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setFirstMarkerFrameDataHex(String firstMarkerFrameDataHex);

    /**
     * Retrieve the transfer frame data configured in this builder as a hexadecimal string, for the middle (or
     * in-between commands) marker frame of a session.
     *
     * @return transfer frame data value configured in this builder for the middle marker frame in a session, as
     * hexadecimal string
     */
    String getMiddleMarkerFrameDataHex();

    /**
     * Set the data value for the transfer frame that serves as the middle (or in-between commands) marker frame of a
     * session, represented as a hexadecimal string.
     *
     * @param middleMarkerFrameDataHex hexadecimal string of the transfer frame data that serves as the middle marker
     *                                 frame of a session
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setMiddleMarkerFrameDataHex(String middleMarkerFrameDataHex);

    /**
     * Retrieve the transfer frame data configured in this builder as a hexadecimal string, for the last marker
     * frame of a session.
     *
     * @return transfer frame data value configured in this builder for the last marker frame in a session, as
     * hexadecimal string
     */
    String getLastMarkerFrameDataHex();

    /**
     * Set the data value for the transfer frame that serves as the last marker frame of a session, represented as a
     * hexadecimal string.
     *
     * @param lastMarkerFrameDataHex hexadecimal string of the transfer frame data that serves as the last marker
     *                               frame of a session
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setLastMarkerFrameDataHex(final String lastMarkerFrameDataHex);

    /**
     * Retrieve the transfer frame Frame Error Control Field algorithm configured in this builder for hardware
     * commands. (Result is only valid when manually set first.)
     *
     * @return FECF algorithm configured in this builder for hardware commands (result is valid only when set first
     * by user)
     */
    FrameErrorControlFieldAlgorithm getHardwareCommandsFrameErrorControlFieldAlgorithm();

    /**
     * Set the transfer frame Frame Error Control Field algorithm in this builder for hardware commands.
     *
     * @param hardwareCommandsFrameErrorControlFieldAlgorithm FECF algorithm to use for hardware commands
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setHardwareCommandsFrameErrorControlFieldAlgorithm(FrameErrorControlFieldAlgorithm hardwareCommandsFrameErrorControlFieldAlgorithm);

    /**
     * Check whether or not the FECF is disabled in this builder for hardware commands. (Result is only valid when
     * manually enabled/disabled first.)
     *
     * @return true if FECF has been manually disabled for hardware commands, false otherwise
     */
    boolean isHardwareCommandsFrameErrorControlFieldSetToNone();

    /**
     * Manually disable FECF in this builder for hardware commands.
     *
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder noHardwareCommandsFrameErrorControlField();

    /**
     * Retrieve the transfer frame Frame Error Control Field algorithm configured in this builder for immediate
     * flight software commands. (Result is only valid when manually set first.)
     *
     * @return FECF algorithm configured in this builder for immediate flight software commands (result is valid only
     * when set first by user)
     */
    FrameErrorControlFieldAlgorithm getImmediateFswCommandsFrameErrorControlFieldAlgorithm();

    /**
     * Set the transfer frame Frame Error Control Field algorithm in this builder for immediate flight softawre
     * commands.
     *
     * @param immediateFswCommandsFrameErrorControlFieldAlgorithm FECF algorithm to use for immediate flight software
     *                                                            commands
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setImmediateFswCommandsFrameErrorControlFieldAlgorithm(FrameErrorControlFieldAlgorithm immediateFswCommandsFrameErrorControlFieldAlgorithm);

    /**
     * Check whether or not the FECF is disabled in this builder for immediate flight software commands. (Result is
     * only valid when manually enabled/disabled first.)
     *
     * @return true if FECF has been manually disabled for immediate flight software commmands, false otherwise
     */
    boolean isImmediateFswCommandsFrameErrorControlFieldSetToNone();

    /**
     * Manually disable FECF in this builder for immediate flight software commands.
     *
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder noImmediateFswCommandsFrameErrorControlField();

    /**
     * Retrieve the transfer frame Frame Error Control Field algorithm configured in this builder for sequence
     * commands. (Result is only valid when manually set first.)
     *
     * @return FECF algorithm configured in this builder for sequence commands (result is valid only
     * when set first by user)
     */
    FrameErrorControlFieldAlgorithm getSequenceCommandsFrameErrorControlFieldAlgorithm();

    /**
     * Set the transfer frame Frame Error Control Field algorithm in this builder for sequence
     * commands.
     *
     * @param sequenceCommandsFrameErrorControlFieldAlgorithm FECF algorithm to use for sequence
     *                                                            commands
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setSequenceCommandsFrameErrorControlFieldAlgorithm(FrameErrorControlFieldAlgorithm sequenceCommandsFrameErrorControlFieldAlgorithm);

    /**
     * Check whether or not the FECF is disabled in this builder for sequence commands. (Result is
     * only valid when manually enabled/disabled first.)
     *
     * @return true if FECF has been manually disabled for sequence commmands, false otherwise
     */
    boolean isSequenceCommandsFrameErrorControlFieldSetToNone();

    /**
     * Manually disable FECF in this builder for sequence commands.
     *
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder noSequenceCommandsFrameErrorControlField();

    /**
     * Retrieve the transfer frame Frame Error Control Field algorithm configured in this builder for delimiter frames.
     * (Result is only valid when manually set first.)
     *
     * @return FECF algorithm configured in this builder for delimiter frames (result is valid only
     * when set first by user)
     */
    FrameErrorControlFieldAlgorithm getDelimiterFrameErrorControlFieldAlgorithm();

    /**
     * Set the transfer frame Frame Error Control Field algorithm in this builder for delimiter frames.
     *
     * @param delimiterFrameErrorControlFieldAlgorithm FECF algorithm to use for delimiter frames
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setDelimiterFrameErrorControlFieldAlgorithm(FrameErrorControlFieldAlgorithm delimiterFrameErrorControlFieldAlgorithm);

    /**
     * Check whether or not the FECF is disabled in this builder for delimiter frames. (Result is
     * only valid when manually enabled/disabled first.)
     *
     * @return true if FECF has been manually disabled for delimiter frames, false otherwise
     */
    boolean isDelimiterFrameErrorControlFieldSetToNone();

    /**
     * Manually disable FECF in this builder for delimiter frames.
     *
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder noDelimiterFrameErrorControlField();

    /**
     * Set the byte length of the FEC field
     *
     * @param byteLength byte length
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setFecfByteLength(int byteLength);

    /**
     * Retrieve the frame sequence number configured in this builder for hardware commands. (Result is only valid
     * when manually set first.)
     *
     * @return frame sequence number configured in this builder for hardware commands (result is valid only when set
     * first by user)
     */
    int getHardwareCommandsFrameSequenceNumber();

    /**
     * Set the frame sequence number in this builder to use for hardware commands.
     *
     * @param hardwareCommandsFrameSequenceNumber frame sequence number to use for hardware commands
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setHardwareCommandsFrameSequenceNumber(int hardwareCommandsFrameSequenceNumber);

    /**
     * Retrieve the frame sequence number configured in this builder for immediate flight software commands. (Result is
     * only valid when manually set first.)
     *
     * @return frame sequence number configured in this builder for immediate flight software commands (result is
     * valid only when set first by user)
     */
    int getImmediateFswCommandsFrameSequenceNumber();

    /**
     * Set the frame sequence number in this builder to use for immediate flight software commands.
     *
     * @param immediateFswCommandsFrameSequenceNumber frame sequence number to use for immediate flight software
     *                                                commands
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setImmediateFswCommandsFrameSequenceNumber(int immediateFswCommandsFrameSequenceNumber);

    /**
     * Retrieve the frame sequence number configured in this builder for sequence commands. (Result is
     * only valid when manually set first.)
     *
     * @return frame sequence number configured in this builder for sequence commands (result is
     * valid only when set first by user)
     */
    int getSequenceCommandsFrameSequenceNumber();

    /**
     * Set the frame sequence number in this builder to use for sequence commands.
     *
     * @param sequenceCommandsFrameSequenceNumber frame sequence number to use for sequence
     *                                                commands
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setSequenceCommandsFrameSequenceNumber(int sequenceCommandsFrameSequenceNumber);

    /**
     * Retrieve the bypass flag set in this builder for hardware commands. (Result is only valid when manually set
     * first.)
     *
     * @return bypass flag set in this builder for hardware commands (result is valid only when set first by user)
     */
    boolean getHardwareCommandsBypassFlag();

    /**
     * Set the bypass flag on in this builder for hardware commands.
     *
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setHardwareCommandsBypassFlagOn();

    /**
     * Set the bypass flag off in this builder for hardware commands.
     *
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setHardwareCommandsBypassFlagOff();

    /**
     * Retrieve the bypass flag set in this builder for immediate flight softaware commands. (Result is only valid when
     * manually set first.)
     *
     * @return bypass flag set in this builder for immediate flight software commands (result is valid only when set
     * first by user)
     */
    boolean getImmediateFswCommandsBypassFlag();

    /**
     * Set the bypass flag on in this builder for immediate flight software commands.
     *
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setImmediateFswCommandsBypassFlagOn();

    /**
     * Set the bypass flag off in this builder for immediate flight software commands.
     *
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setImmediateFswCommandsBypassFlagOff();

    /**
     * Retrieve the bypass flag set in this builder for sequence commands. (Result is only valid when
     * manually set first.)
     *
     * @return bypass flag set in this builder for sequence commands (result is valid only when set
     * first by user)
     */
    boolean getSequenceCommandsBypassFlag();

    /**
     * Set the bypass flag on in this builder for sequence commands.
     *
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setSequenceCommandsBypassFlagOn();

    /**
     * Set the bypass flag off in this builder for sequence commands.
     *
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setSequenceCommandsBypassFlagOff();

    /**
     * Retrieve the control command flag set in this builder for hardware commands. (Result is only valid when manually
     * set first.)
     *
     * @return control command flag set in this builder for hardware commands (result is valid only when set first by
     * user)
     */
    boolean getHardwareCommandsControlCommandFlag();

    /**
     * Set the control command flag on in this builder for hardware commands.
     *
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setHardwareCommandsControlCommandFlagOn();

    /**
     * Set the control command flag off in this builder for hardware commands.
     *
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setHardwareCommandsControlCommandFlagOff();

    /**
     * Retrieve the control command flag set in this builder for immediate flight software commands. (Result is only
     * valid when manually set first.)
     *
     * @return control command flag set in this builder for immediate flight software commands (result is valid only
     * when set first by user)
     */
    boolean getImmediateFswCommandsControlCommandFlag();

    /**
     * Set the control command flag on in this builder for immediate flight software commands.
     *
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setImmediateFswCommandsControlCommandFlagOn();

    /**
     * Set the control command flag off in this builder for immediate flight software commands.
     *
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setImmediateFswCommandsControlCommandFlagOff();

    /**
     * Retrieve the control command flag set in this builder for sequence commands. (Result is only
     * valid when manually set first.)
     *
     * @return control command flag set in this builder for sequence commands (result is valid only
     * when set first by user)
     */
    boolean getSequenceCommandsControlCommandFlag();

    /**
     * Set the control command flag on in this builder for sequence commands.
     *
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setSequenceCommandsControlCommandFlagOn();

    /**
     * Set the control command flag off in this builder for sequence commands.
     *
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setSequenceCommandsControlCommandFlagOff();

    /**
     * Retrieve the SCMF header preparer name value configured in this builder.
     *
     * @return SCMF header preparer name value configured in this builder
     */
    String getScmfHeaderPreparerName();

    /**
     * Set the SCMF header preparer name value for this builder
     *
     * @param scmfHeaderPreparerName SCMF header preparer name value to set in this builder
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setScmfHeaderPreparerName(String scmfHeaderPreparerName);

    /**
     * Retrieve the SCMF header bit one radiation time value configured in this builder.
     *
     * @return SCMF header bit one radiation time value configured in this builder
     */
    double getScmfHeaderBitOneRadiationTime();

    /**
     * Set the SCMF header bit one radiation time value for this builder
     *
     * @param scmfHeaderBitOneRadiationTime SCMF header bit one radiation time value to set in this builder
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setScmfHeaderBitOneRadiationTime(double scmfHeaderBitOneRadiationTime);

    /**
     * Retrieve the SCMF header bit rate index value configured in this builder.
     *
     * @return SCMF header bit rate index value configured in this builder
     */
    int getScmfHeaderBitRateIndex();

    /**
     * Set the SCMF header bit rate index value for this builder
     *
     * @param scmfHeaderBitRateIndex SCMF header bit rate index value to set in this builder
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setScmfHeaderBitRateIndex(int scmfHeaderBitRateIndex);

    /**
     * Retrieve the SCMF header comment value configured in this builder.
     *
     * @return SCMF header comment value configured in this builder
     */
    String getScmfHeaderComment();

    /**
     * Set the SCMF header comment value for this builder
     *
     * @param scmfHeaderComment SCMF header comment value to set in this builder
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setScmfHeaderComment(String scmfHeaderComment);

    /**
     * Retrieve the SCMF header title value configured in this builder.
     *
     * @return SCMF header title value configured in this builder
     */
    String getScmfHeaderTitle();

    /**
     * Set the SCMF header title value for this builder
     *
     * @param scmfHeaderTitle SCMF header comment value to set in this builder
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setScmfHeaderTitle(String scmfHeaderTitle);

    /**
     * Retrieve the SCMF header untimed field value configured in this builder.
     *
     * @return SCMF header untimed field value configured in this builder
     */
    int getScmfHeaderUntimed();

    /**
     * Set the SCMF header untimed value to set in this builder
     *
     * @param scmfHeaderUntimed SCMF header untimed value to set in this builder
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setScmfHeaderUntimed(int scmfHeaderUntimed);

    /**
     * Set the SCMF message radiation window open time
     *
     * @param scmfMessageRadiationWindowOpenTime SCMF message radiation window open time, in seconds since UNIX epoch.
     * @return this object (builder pattern)
     */
    ITcThroughBuilder setScmfMessageRadiationWindowOpenTime(long scmfMessageRadiationWindowOpenTime);

    /**
     * Get the SCMF message radiation window open time
     *
     * @return SCMF message radiation window open time, in seconds since UNIX epoch.
     */
    long getScmfMessageRadiationWindowOpenTime();

    /**
     * Set the SCMF message radiation window close time
     *
     * @param scmfMessageRadiationWindowCloseTime SCMF message radiation window close time, in seconds since UNIX epoch.
     * @return this object (builder pattern)
     */
    ITcThroughBuilder setScmfMessageRadiationWindowCloseTime(long scmfMessageRadiationWindowCloseTime);

    /**
     * Get the SCMF message radiation window close time
     *
     * @return SCMF message radiation window close time, in seconds since UNIX epoch.
     */
    long getScmfMessageRadiationWindowCloseTime();

    /**
     * Set the SCMF message radiation start time
     *
     * @param scmfMessageRadiationStartTime SCMF message radiation start time, in seconds since UNIX epoch.
     * @return this object (builder pattern)
     */
    ITcThroughBuilder setScmfMessageRadiationStartTime(long scmfMessageRadiationStartTime);

    /**
     * Get the SCMF message radiation start time
     *
     * @return SCMF message radiation start time, in seconds since UNIX epoch.
     */
    long getScmfMessageRadiationStartTime();

    /**
     * Retrieve the first SCMF data record's comment value configured in this builder.
     *
     * @return first SCMF data record's comment value configured in this builder
     */
    String getScmfDataFirstRecordComment();

    /**
     * Set the first SCMF data record's comment value in this builder
     *
     * @param scmfDataFirstRecordComment first SCMF data record's comment value to set in this builder
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setScmfDataFirstRecordComment(String scmfDataFirstRecordComment);

    /**
     * Retrieve the marker SCMF data record's comment value configured in this builder.
     *
     * @return marker SCMF data record's comment value configured in this builder
     */
    String getScmfDataMarkerRecordComment();

    /**
     * Set the marker SCMF data record's comment value in this builder
     *
     * @param scmfDataMarkerRecordComment marker SCMF data record's comment value to set in this builder
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setScmfDataMarkerRecordComment(String scmfDataMarkerRecordComment);

    /**
     * Retrieve the actual command bits SCMF data record's comment value configured in this builder.
     *
     * @return actual command bits SCMF data record's comment value configured in this builder.
     */
    String getScmfDataActualCommandRecordComment();

    /**
     * Set the actual command bits SCMF data record's comment value in this builder
     *
     * @param scmfDataActualCommandRecordComment actual command bits SCMF data record's comment value to set in this
     *                                           builder
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setScmfDataActualCommandRecordComment(String scmfDataActualCommandRecordComment);

    /**
     * Retrieve the last SCMF data record's comment value configured in this builder.
     *
     * @return last SCMF data record's comment value configured in this builder
     */
    String getScmfDataLastRecordComment();

    /**
     * Set the last SCMF data record's comment value in this builder
     *
     * @param scmfDataLastRecordComment last SCMF data record's comment value to set in this builder
     * @return same builder object (builder pattern)
     */
    ITcThroughBuilder setScmfDataLastRecordComment(String scmfDataLastRecordComment);

    /**
     * Generate the SCMF from command mnemonics provided. The SCMF will be generated to the output SCMF file set via
     * {@code #setOutScmfFile(java.lang.String)}.
     *
     * @throws ThroughTewException thrown when any precondition is not met (such as required parameters not yet set)
     *                             or when the MPSA UplinkUtils library throws any sort of exception
     */
    void buildScmf() throws ThroughTewException;

    /**
     * Different types of command categories.
     */
    enum InternalCommandMnemonicType {

        /**
         * Value for hardware commands.
         */
        HARDWARE,

        /**
         * Value for immediate flight software commands.
         */
        IMMEDIATE_FSW,

        /**
         * Value for sequence commands
         */
        SEQUENCE;

        /**
         * Convert an AMPCS CommandDefinitionType to an InternalCommandMnemonicType that can be utilized by ITcThroughBuilder
         * @param type the AMPCS CommandDefinitionType for a command to be converted by ITcThroughBuilder
         * @return the IntercnalCommandMnemonicType that can be utilized by ITcThroughBuilder
         */
        public static InternalCommandMnemonicType convertFromCommandDefinitionType(CommandDefinitionType type) {
            switch(type) {
                case HARDWARE:
                    return HARDWARE;
                case FLIGHT:
                    return IMMEDIATE_FSW;
                case SEQUENCE_DIRECTIVE:
                    return SEQUENCE;
                case SSE:
                case UNDEFINED:
                default:
                    throw new IllegalArgumentException("The through builder does not support " + type + " commands");
            }
        }
    }

}