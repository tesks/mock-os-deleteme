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

package jpl.gds.tc.impl.output.adapter;

import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.types.UplinkConnectionType;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.IGeneralContextInformation;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.file.FileUtility;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.metadata.InvalidMetadataException;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.SclkScetConverter;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.CommandStatusType;
import jpl.gds.tc.api.DisplayUtility;
import jpl.gds.tc.api.ICommandFileLoad;
import jpl.gds.tc.api.IScmf;
import jpl.gds.tc.api.IScmfFactory;
import jpl.gds.tc.api.ITcTransferFrame;
import jpl.gds.tc.api.ITewUtility;
import jpl.gds.tc.api.IUplinkResponse;
import jpl.gds.tc.api.TcApiBeans;
import jpl.gds.tc.api.UplinkFailureReason;
import jpl.gds.tc.api.cltu.ICltu;
import jpl.gds.tc.api.cltu.ICltuFactory;
import jpl.gds.tc.api.command.ICommand;
import jpl.gds.tc.api.command.IFlightCommand;
import jpl.gds.tc.api.command.ISseCommand;
import jpl.gds.tc.api.config.CltuProperties;
import jpl.gds.tc.api.config.CommandFrameProperties;
import jpl.gds.tc.api.config.CommandProperties;
import jpl.gds.tc.api.config.PlopProperties;
import jpl.gds.tc.api.config.ScmfProperties;
import jpl.gds.tc.api.config.VirtualChannelType;
import jpl.gds.tc.api.exception.BlockException;
import jpl.gds.tc.api.exception.CltuEndecException;
import jpl.gds.tc.api.exception.CommandFileParseException;
import jpl.gds.tc.api.exception.CommandParseException;
import jpl.gds.tc.api.exception.RawOutputException;
import jpl.gds.tc.api.exception.ScmfParseException;
import jpl.gds.tc.api.exception.ScmfWrapUnwrapException;
import jpl.gds.tc.api.exception.UplinkException;
import jpl.gds.tc.api.frame.ITcTransferFrameFactory;
import jpl.gds.tc.api.message.IScmfCommandMessage;
import jpl.gds.tc.api.output.ICommandMessageUtility;
import jpl.gds.tc.api.output.IRawOutputAdapter;
import jpl.gds.tc.api.output.ISseCommandSocket;
import jpl.gds.tc.api.output.OutputFileNameFactory;
import jpl.gds.tc.api.plop.ICommandLoadBuilder;
import jpl.gds.tc.api.session.ISessionBuilder;
import jpl.gds.tc.api.through.ITcThroughBuilder;
import jpl.gds.tc.api.through.ThroughTewException;
import jpl.gds.tc.impl.SseCommand;
import jpl.gds.tc.impl.plop.CommandLoadBuilder;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static jpl.gds.tc.api.ITcTransferFrame.BITMASK_EXECUTION_STRING;
import static jpl.gds.tc.api.ITcTransferFrame.EXECUTION_STRING_BIT_OFFSET;


/**
 * Output adapter abstract class.
 *
 */
public abstract class AbstractOutputAdapter implements IRawOutputAdapter {
    /** Tracer */
	protected final Tracer trace;
    /** Buffer size */
	protected static final int BUFFER_SIZE = 800;
    /** Command message utility object */
	protected ICommandMessageUtility messageUtil;
    /** True means send internal message with SCMF */
	protected boolean sendInternalMessagesWithScmf;
    /** Uplink connection type */
	protected UplinkConnectionType connType;
	/** the ApplicaitonContext this instance is running in */
	protected ApplicationContext appContext;
	/** SCMF configuration */
    protected final ScmfProperties           scmfConfig;
    /** TC Session builder */
    protected final ISessionBuilder sessionBuilder;
    /** CLTU Builder for output adapters */
    protected final ICltuFactory cltuBuilder;
    /** Telecommand Frame factory */
    protected final ITcTransferFrameFactory frameFactory;
    /** MissionProperties config */
    protected final MissionProperties        missionConfig;
    /** Command Frame configuration */
    protected final CommandFrameProperties frameConfig;
    /** PLOP configuration */
    protected final PlopProperties         plopConfig;
    /** CLTU configuration */
    protected final CltuProperties         cltuConfig;

	/** Command configuration */
	private final CommandProperties cmdConfig;

    /** MPCS-11666 6/5/2020
     * Frame sequence number wraps after 255 */
    private final int FRAME_SEQUENCE_WRAP_MAX = 255;
    private AtomicInteger pduFrameSequenceCounter = new AtomicInteger(0);


    /**
     * Constructor.
     * @param appContext the ApplicationContext in which this object is being usedß
     */
	protected AbstractOutputAdapter(final ApplicationContext appContext) {
		this.appContext = appContext;
        this.trace = TraceManager.getTracer(appContext, Loggers.UPLINK);
		this.messageUtil = new CommandMessageUtility(appContext);
		this.sendInternalMessagesWithScmf = true;
		this.connType = appContext.getBean(IConnectionMap.class).getFswUplinkConnection().
		        getUplinkConnectionType();

		this.cmdConfig = appContext.getBean(CommandProperties.class);
		this.scmfConfig = appContext.getBean(ScmfProperties.class);
		this.sessionBuilder = appContext.getBean(ISessionBuilder.class);
        this.cltuBuilder = appContext.getBean(ICltuFactory.class);
        this.frameFactory = appContext.getBean(ITcTransferFrameFactory.class);
        this.missionConfig = appContext.getBean(MissionProperties.class);
        this.frameConfig = appContext.getBean(CommandFrameProperties.class);
        this.plopConfig = appContext.getBean(PlopProperties.class);
        this.cltuConfig = appContext.getBean(CltuProperties.class);
	}


	/*protected*/ ITcThroughBuilder getNewThroughBuilder() {
		final int              scid             = appContext.getBean(IContextIdentification.class).getSpacecraftId();
		final int              executionString  = (frameConfig.getStringIdVcidValue() & BITMASK_EXECUTION_STRING) << EXECUTION_STRING_BIT_OFFSET;
		final int              hardwareCommands = frameConfig.getVirtualChannelNumber(VirtualChannelType.HARDWARE_COMMAND) & ITcTransferFrame.BITMASK_VC_NUMBER;
		final int              fswCommands      = frameConfig.getVirtualChannelNumber(VirtualChannelType.FLIGHT_SOFTWARE_COMMAND) & ITcTransferFrame.BITMASK_VC_NUMBER;
		final int              sequenceCommands = frameConfig.getVirtualChannelNumber(VirtualChannelType.SEQUENCE_DIRECTIVE) & ITcTransferFrame.BITMASK_VC_NUMBER;
		final int              marker           = frameConfig.getVirtualChannelNumber(VirtualChannelType.DELIMITER) & ITcTransferFrame.BITMASK_VC_NUMBER;



		final ITcThroughBuilder throughBuilder = appContext.getBean(ITcThroughBuilder.class);

		throughBuilder.setScid(scid)
				.setSclkScetFile(SclkScetConverter.getSclkScetFilePath(scid))
				.setHardwareCommandsVcid(executionString + hardwareCommands) // ORed with exec string
				.setImmediateFswCommandsVcid(executionString + fswCommands) // ORed with exec string
				.setSequenceCommandsVcid(executionString + sequenceCommands) // ORed with exec string
				.setMarkerVcid(executionString + marker) // ORed with exec string
				.setFirstMarkerFrameDataHex(frameConfig.getBeginDataHex())
				.setMiddleMarkerFrameDataHex("0000") //TODO: Make a config for this
				.setLastMarkerFrameDataHex(frameConfig.getEndDataHex())
				.setAcqSeq(BinOctHexUtility.toHexFromBytes(plopConfig.getAcquisitionSequence()))
				.setIdleSeq(BinOctHexUtility.toHexFromBytes(plopConfig.getIdleSequence()))
				.setStartSeq(cltuConfig.getStartSequenceHex())
				.setTailSeq(cltuConfig.getTailSequenceHex())
				.setScmfHeaderPreparerName(scmfConfig.getPreparer());
		if(frameConfig.hasFecf(VirtualChannelType.HARDWARE_COMMAND)) {
			throughBuilder.setHardwareCommandsFrameErrorControlFieldAlgorithm(
					frameConfig.getChecksumCalcluator(VirtualChannelType.HARDWARE_COMMAND));
		} else {
			throughBuilder.noHardwareCommandsFrameErrorControlField();
		}

		if(frameConfig.hasFecf(VirtualChannelType.FLIGHT_SOFTWARE_COMMAND)) {
			throughBuilder.setImmediateFswCommandsFrameErrorControlFieldAlgorithm(
					frameConfig.getChecksumCalcluator(VirtualChannelType.FLIGHT_SOFTWARE_COMMAND));
		} else {
			throughBuilder.noImmediateFswCommandsFrameErrorControlField();
		}

		if(frameConfig.hasFecf(VirtualChannelType.SEQUENCE_DIRECTIVE)) {
			throughBuilder.setSequenceCommandsFrameErrorControlFieldAlgorithm(
					frameConfig.getChecksumCalcluator(VirtualChannelType.SEQUENCE_DIRECTIVE));
		} else {
			throughBuilder.noSequenceCommandsFrameErrorControlField();
		}

		if(frameConfig.hasFecf(VirtualChannelType.DELIMITER)) {
			throughBuilder.setDelimiterFrameErrorControlFieldAlgorithm(
					frameConfig.getChecksumCalcluator(VirtualChannelType.DELIMITER));
		} else {
			throughBuilder.noDelimiterFrameErrorControlField();
		}

		if (frameConfig.getFecfLength() != 2) {
			throughBuilder.setFecfByteLength(frameConfig.getFecfLength());
		}

		return throughBuilder;
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public void init() throws RawOutputException {
	}

	/**
	 * Send commands.
	 *
	 * @param commands List of commands
	 * @param id       a unique ID to tag this command to allow retrieval of the
	 *                 Command Messages generated from this uplink
	 *
	 * @throws UplinkException On uplink error
	 */
	@Override
	public void sendCommands(final List<ICommand> commands, final int id) throws UplinkException {

		final List<IFlightCommand> queuedCommands = new ArrayList<>();
		IUplinkResponse            resp;
		try {
			for(final ICommand command : commands) {
				if(command instanceof ISseCommand) {
					if(!queuedCommands.isEmpty()) {
						setScmfConfigFilenameCommands(queuedCommands);
						resp = sendViaThroughBuilder(queuedCommands, id);

						queuedCommands.clear();
					}
					sendSseCommand((SseCommand)command, id);
				} else {
					final IFlightCommand cmd          = (IFlightCommand)command;
					queuedCommands.add(cmd);
				}
			}

			if(!queuedCommands.isEmpty()) {
				setScmfConfigFilenameCommands(queuedCommands);
				resp = sendViaThroughBuilder(queuedCommands, id);
			}
			if (scmfConfig.getOnlyWriteScmf()) {
				return;
			}

		} catch (final UplinkException e) {
			this.messageUtil.sendCommandMessages(queuedCommands,
					e.getUplinkResponse(), id);
			throw e;
		} catch (final Exception e) {
			final String errorMessage = e.getMessage() == null ? "AMPCS encountered an error: "
					+ e.toString()
					: e.getMessage();

			resp = new GenericUplinkResponse("",
					CommandStatusType.Send_Failure,
					UplinkFailureReason.AMPCS_SEND_FAILURE, errorMessage, -1L,
					-1L, new AccurateDateTime());
			this.messageUtil.sendCommandMessages(queuedCommands,
					resp, id);

			throw new UplinkException(errorMessage, e, resp);
		} finally {
			scmfConfig.setScmfName(null);
		}

		String commandSinkStr = "";

		if (this.connType != null) {
			commandSinkStr = " to " + this.connType.toString();
		}

		DisplayUtility.printMessage(appContext, "Forwarded transmitted command(s)"
				+ commandSinkStr + ".\n\n");
	}

	/**
	 * Get command bytes from a command object. This will attempt command translation through MPS TEW gold standard, and
	 * fall back upon AMPCS legacy translation.
	 *
	 * @param cmd command object
	 * @return command bytes
	 * @throws BlockException
	 * @throws CommandFileParseException
	 * @throws CommandParseException
	 */
	private byte[] getCommandBytes(IFlightCommand cmd) throws
													   BlockException,
													   CommandFileParseException,
													   CommandParseException {
		byte[]      commandBytes;
		ITewUtility tewUtility = appContext.getBean(TcApiBeans.MPS_TEW_UTILITY, ITewUtility.class);

		try {
			commandBytes = tewUtility.validateAndTranslateCommand(cmd);
		} catch (final CommandParseException e) {
			if (appContext.getBean(CommandProperties.class).getValidateCommands()) {
				throw e;
			}
			trace.warn(e.getMessage(), " Attempting to reverse command bytes using legacy AMPCS utilities.");
			// Legacy translation attempt. This will throw an exception if command validation is on.
			try {
				ITewUtility legacyTewUtility = appContext.getBean(TcApiBeans.LEGACY_TEW_UTILITY, ITewUtility.class);
				commandBytes = legacyTewUtility.validateAndTranslateCommand(cmd);
			} catch (final CommandParseException ignore) {
				// if legacy command translation fails, throw the original exception from MPS
				trace.warn("Legacy command translation failed: ", e.getMessage());
				throw e;
			}
		}

		return commandBytes;
	}

	protected void setScmfConfigFilenameCommands(final List<IFlightCommand> commands) {
		String scmfName;

		if (scmfConfig.getScmfName() != null) {
			return;
		}

		if (commands.size() == 1) {
			scmfName = OutputFileNameFactory.createNameForCommand(appContext, commands.get(0));
		} else {
			scmfName = OutputFileNameFactory.createNameForCommandListFile(appContext);
		}

		if(!scmfConfig.getWriteScmf()) {
			String scmfDir;
			try {
				scmfDir = appContext.getBean(IGeneralContextInformation.class).getOutputDir();
			} catch (final IllegalStateException ise) {
				scmfDir = ".";
			}

			final String tmpDir = GdsSystemProperties.getSystemProperty("java.io.tmpdir", "/tmp");

			scmfName = scmfName.replace(scmfDir,tmpDir);
		}

		scmfConfig.setScmfName(scmfName);
	}


	protected IUplinkResponse sendViaThroughBuilder(List<IFlightCommand> commands, final int id)
			throws BlockException, CommandParseException, CommandFileParseException, ThroughTewException, UplinkException, CltuEndecException, IOException, ScmfWrapUnwrapException, ScmfParseException {
		ITcThroughBuilder throughBuilder = getNewThroughBuilder();

		throughBuilder.setOutScmfFile(scmfConfig.getScmfName());

		for (IFlightCommand cmd : commands ) {
			byte[]               commandBytes = getCommandBytes(cmd);
			throughBuilder.addCommandBytes(BinOctHexUtility.toHexFromBytes(commandBytes), cmd.getDefinition().getType());
		}

		IUplinkResponse resp = sendThroughBuilder(throughBuilder);

		if (resp != null) {
			this.messageUtil.sendCommandMessages(commands, resp, id);
		}

		return resp;
	}

	private IUplinkResponse sendThroughBuilder(final ITcThroughBuilder throughBuilder) throws ThroughTewException, UplinkException, CltuEndecException, IOException, ScmfWrapUnwrapException, ScmfParseException {
		if(throughBuilder == null) {
			return null;
		}
		IUplinkResponse response = null;

		throughBuilder.buildScmf();

		if(!scmfConfig.getOnlyWriteScmf()) {
			final IScmf scmf = appContext.getBean(ITewUtility.class).reverseScmf(throughBuilder.getOutScmfFile());
			response = transmitScmf(scmf);

			if(cmdConfig.isDebug()) {
				DisplayUtility.writeScmfToDisplay(appContext, scmf);
			}
		}

		if (scmfConfig.getWriteScmf()) {
			trace.info("Wrote SCMF to " + throughBuilder.getOutScmfFile());
		} else {
			final File scmfFile = new File(throughBuilder.getOutScmfFile());
			if (scmfFile.exists()) {
				scmfFile.delete();
			}
		}

		return response;
	}


    /**
     * Send file loads.
     *
     * @param fileLoads File loads
     * @param id        Id
     *
     * @throws UplinkException On error
     */
    @Override
    public void sendFileLoads(final List<ICommandFileLoad> fileLoads, final int id)
			throws UplinkException {
		if (scmfConfig.getScmfName() == null) {
			scmfConfig.setScmfName(OutputFileNameFactory
					.createNameForFileLoad(appContext));
		}

		final boolean onlyWriteScmf = scmfConfig.getOnlyWriteScmf();

		IUplinkResponse response = new GenericUplinkResponse();
		response.setRequestId("");

		final List<ICltu> plopCltus;
		final IScmf       scmf;
		try {
			/*
			 * If multiple file loads are sent at once, each one is supposed to
			 * go into its own uplink session, but they should all go into the
			 * same command load
			 */

			final ICommandLoadBuilder commandLoad = new CommandLoadBuilder();
			for (final ICommandFileLoad load : fileLoads) {
				final File inputFile = new File(load.getInputFileName());

				if (!onlyWriteScmf) {
					try {
						FileUtility.copyFileToSessionDirectory(
								appContext.getBean(IGeneralContextInformation.class)
										.getOutputDir(), inputFile);
					} catch (final IOException ioe) {
						trace.warn("Could not copy input file "
								+ inputFile.getAbsolutePath()
								+ " to session output directory.");
					}
				}

				load.setInputFileName(inputFile.getAbsolutePath());
                sessionBuilder.addFrames(frameFactory.createFileLoadFrames(load));

                // MPCS-11459: need to use a legacy CLTU builder to account for frame randomization
				ICltuFactory legacyCltuBuilder = appContext.getBean(TcApiBeans.LEGACY_CLTU_FACTORY, ICltuFactory.class);

                final List<ICltu> cltuList = legacyCltuBuilder.createCltusFromFrames(sessionBuilder.getSessionFrames());
                sessionBuilder.clear();
				commandLoad.addCltus(cltuList);
			}

			plopCltus = commandLoad.getPlopCltus(appContext.getBean(PlopProperties.class));
			scmf = getScmf(appContext, plopCltus);
			if (onlyWriteScmf) {
				return;
			}

		} catch (final Exception e) {
			final String errorMessage = e.getMessage() == null ? "AMPCS encountered an error: "
					+ e.toString()
					: e.getMessage();

			response.setFailureReason(UplinkFailureReason.AMPCS_SEND_FAILURE);
			response.setStatus(CommandStatusType.Send_Failure);
			response.setDiagnosticMessage(errorMessage);
			response.setSuccessful(false);
			this.messageUtil.sendFileLoadMessages(fileLoads, response, id);

			throw new UplinkException(errorMessage, e, response);
		}

		try {
			response = transmitPlopCltus(plopCltus, scmf);
		} catch (final UplinkException e) {
			this.messageUtil.sendFileLoadMessages(fileLoads,
					e.getUplinkResponse(), id);
			throw e;
		}

        if (cmdConfig.isDebug()) {
            DisplayUtility.writeCltusToDisplay(appContext, plopCltus);
        }

		String commandSinkStr = "";

		if (this.connType != null) {
			commandSinkStr = " to " + this.connType.toString();
		}

		if (response.isSuccessful()) {
			DisplayUtility.printMessage(appContext, "Successfully transmitted file load(s)"
					+ commandSinkStr + ".\n\n");
		} else {
			DisplayUtility.printMessage(appContext, response.getFailureReason()
					.getMessage()
					+ ": "
					+ response.getDiagnosticMessage()
					+ "\n");
		}

		this.messageUtil.sendFileLoadMessages(fileLoads, response, id);
	}


    /**
     * Send SSE command.
     *
     * @param command SSE command
     * @param id      Id
     *
     * @throws IOException On error
     */
    @Override
    public void sendSseCommand(final ISseCommand command, final int id)
			throws IOException {
		if (command == null) {
			throw new IllegalArgumentException("Input SSE command was null");
		}

		String commandString = command.getCommandString(false);
		if (!commandString.endsWith("\n")) {
			commandString += "\n";
		}

		try
        {
			appContext.getBean(ISseCommandSocket.class).transmitSseCommand(commandString);
		}
        catch (final IOException e)
        {
			this.messageUtil.sendSseCommandMessage(command, id, false);
			throw e;
		}

		DisplayUtility.printMessage(appContext, "SSE command forwarded to socket.\n\n");
		this.messageUtil.sendSseCommandMessage(command, id, true);
	}


	/**
	 * Send SCMF.
	 *
	 * @param scmf
	 *            SCMF
	 * @param id
	 *            Id
	 *
	 * @throws UplinkException
	 *             On error
	 */
	@Override
	public void sendScmf(final IScmf scmf, final int id) throws UplinkException {
		if (scmf == null) {
			throw new IllegalArgumentException("Null input SCMF");
		}

		final File inputFile = new File(scmf.getOriginalFile());
		File transmitFile = inputFile;
		try {
			transmitFile = FileUtility.copyFileToSessionDirectory(
					appContext.getBean(IGeneralContextInformation.class).getOutputDir(), inputFile);
		} catch (final IOException ioe) {
			trace.warn("Could not copy input file " + scmf.getOriginalFile() + " to session output directory.");
		}

		IUplinkResponse response = new GenericUplinkResponse();
		response.setRequestId("");

		final List<IScmfCommandMessage> messages = scmf.getCommandMessages();
        if (messages.isEmpty()) {
			final String errorMessage = "No CLTUs available to be sent in the uplink session.\n";
			DisplayUtility.printMessage(appContext, errorMessage);
			response.setFailureReason(UplinkFailureReason.AMPCS_SEND_FAILURE);
			response.setStatus(CommandStatusType.Send_Failure);
			response.setDiagnosticMessage(errorMessage);
	        response.setSuccessful(false);
			this.messageUtil.sendScmfMessage(transmitFile, response, id);

			throw new UplinkException(errorMessage, response);
		}

		try {
			response = transmitScmf(scmf);

			if (cmdConfig.isDebug() || cmdConfig.getShowGui()) {
				DisplayUtility.writeSpacecraftMessagesToDisplay(appContext, messages);
			}

			String commandSinkStr = "";

			if (this.connType != null) {
				commandSinkStr = " to " + this.connType.toString();
			}

			if (response.isSuccessful()) {
				DisplayUtility.printMessage(appContext, "Successfully transmitted SCMF" + commandSinkStr + ".\n");
			} else {
				DisplayUtility.printMessage(appContext,
						response.getFailureReason().getMessage() + ": " + response.getDiagnosticMessage() + "\n");
			}

			// TODO add hashcode here
			this.messageUtil.sendScmfMessage(transmitFile, response, id);

			if (sendInternalMessagesWithScmf) { // JIRA MPCS-3109
				this.messageUtil.sendScmfInternalMessages(scmf, response, id);
			} else {
				this.messageUtil.logScmfInternals(scmf, response);
			}

		} catch (final UplinkException e) {
			this.messageUtil.sendScmfMessage(transmitFile, e.getUplinkResponse(), id);
			throw e;
		} catch (final ScmfParseException e) {
			final String errorMessage = e.getMessage() == null ? "AMPCS encountered an error: " + e.toString()
					: e.getMessage();

			response.setFailureReason(UplinkFailureReason.AMPCS_SEND_FAILURE);
			response.setStatus(CommandStatusType.Send_Failure);
			response.setDiagnosticMessage(errorMessage);
			response.setSuccessful(false);
			this.messageUtil.sendScmfMessage(transmitFile, response, id);

			throw new UplinkException(errorMessage, e, response);
		}
	}


	/**
	 * Send raw uplink data.
	 *
	 * @param inputFile
	 *            Input file
	 * @param isHex
	 *            True if hex file
	 * @param isFaultInjected
	 *            True if a fault
	 * @param id
	 *            Id
	 *
	 * @throws UplinkException
	 *             On error
	 */
	@Override
	public void sendRawUplinkData(final File inputFile, final boolean isHex, final boolean isFaultInjected, final int id)
			throws UplinkException {
		if (inputFile == null) {
			throw new IllegalArgumentException("Input file was null");
		} else if (!inputFile.exists()) {
			throw new IllegalArgumentException("The input file " + inputFile.getName() + " does not exist.");
		}

		if (scmfConfig.getScmfName() == null) {
			scmfConfig.setScmfName(OutputFileNameFactory.createNameForRawUplinkData(appContext));
		}

		final boolean onlyWriteScmf = scmfConfig.getOnlyWriteScmf();
		// if (onlyWriteScmf == false) {
		// SecurityChecker.checkTestbedFswScenario();
		// }

		File transmitFile = inputFile;
		if (!onlyWriteScmf) {
			try {
				transmitFile = FileUtility.copyFileToSessionDirectory(
						appContext.getBean(IGeneralContextInformation.class).getOutputDir(), inputFile);
			} catch (final Exception ioe) {
				ioe.printStackTrace();
				trace.warn(
						"Could not copy input file " + inputFile.getAbsolutePath() + " to session output directory.");
			}
		}

		IUplinkResponse response = new GenericUplinkResponse();
		response.setRequestId("");

		final IScmf scmf;
		try {
			scmf = getScmf(appContext, transmitFile, isHex);

			if (onlyWriteScmf) {
				return;
			}
		} catch (final Exception e) {
			final String errorMessage = e.getMessage() == null ? "AMPCS encountered an error: " + e.toString()
					: e.getMessage();

			response.setFailureReason(UplinkFailureReason.AMPCS_SEND_FAILURE);
			response.setStatus(CommandStatusType.Send_Failure);
			response.setDiagnosticMessage(errorMessage);
			response.setSuccessful(false);

			this.messageUtil.sendRawUplinkDataMessage(transmitFile, response, isFaultInjected, id);

			throw new UplinkException(errorMessage, e, response);
		}

		try {
			response = transmitRawUplinkData(transmitFile, isHex, scmf);
			if (cmdConfig.isDebug() || cmdConfig.getShowGui()) {
				DisplayUtility.writeRawUplinkDataToDisplay(appContext, transmitFile, isHex);
			}
		} catch (final UplinkException e) {
			this.messageUtil.sendRawUplinkDataMessage(transmitFile, e.getUplinkResponse(), isFaultInjected, id);

			throw e;
		}

		String commandSinkStr = "";

		if (this.connType != null) {
			commandSinkStr = " to " + this.connType.toString();
		}

		if (response.isSuccessful()) {
			DisplayUtility.printMessage(appContext, "Successfully transmitted raw data file" + commandSinkStr + ".\n");
		} else {
			DisplayUtility.printMessage(appContext,
					response.getFailureReason().getMessage() + ": " + response.getDiagnosticMessage() + "\n");
		}

		this.messageUtil.sendRawUplinkDataMessage(inputFile, response, isFaultInjected, id);
	}


    /**
     * Transmit PLOP CLTUs.
     *
     * @param plopCltus CLTUs
     * @param scmf      SCMF
     *
     * @return Response
     *
     * @throws UplinkException On error
     */
	protected abstract IUplinkResponse transmitPlopCltus(
			final List<ICltu> plopCltus, final IScmf scmf) throws UplinkException;


    /**
     * Transmit SCMF.
     *
     * @param scmf SCMF
     *
     * @return Response
     *
     * @throws UplinkException On error
     */
	protected abstract IUplinkResponse transmitScmf(final IScmf scmf)
			throws UplinkException;


    /**
     * Transmit raw uplink data.
     *
     * @param transmitFile Transmit file
     * @param isHex        True if hex file
     * @param scmf         SCMF
     *
     * @return Response
     *
     * @throws UplinkException On error
     */
	protected abstract IUplinkResponse transmitRawUplinkData(
			final File transmitFile, final boolean isHex, final IScmf scmf)
			throws UplinkException;

    /**
     * Get SCMF.
     *
     * @param appContext the ApplicationContext in which this object is being used
     * @param dataFile  Data file
     * @param isHexFile True if hex file
     *
     * @return SCMF
     *
     * @throws IOException         On I/O error
     * @throws ScmfWrapUnwrapException On SCMF error
	 */
	protected static IScmf getScmf(final ApplicationContext appContext, final File dataFile, final boolean isHexFile)
			throws IOException, ScmfWrapUnwrapException {
		final boolean writeScmf = appContext.getBean(ScmfProperties.class).getWriteScmf();
		final IScmf scmf = appContext.getBean(IScmfFactory.class).toScmf(dataFile, isHexFile, writeScmf);

		if (writeScmf) {
			DisplayUtility.printMessage(appContext, "Wrote SCMF to "
					+ scmf.getOriginalFile() + "\n\n");
		}

		return (scmf);
	}


    /**
     * Get SCMF.
     *
     * @param appContext the ApplicationContext in which this object is being used
     * @param plopCltus List of CLTUs
     *
     * @return SCMF
     *
     * @throws IOException         On I/O error
     * @throws ScmfWrapUnwrapException On SCMF error
     */
	protected static IScmf getScmf(final ApplicationContext appContext, final List<ICltu> plopCltus)
			throws IOException, ScmfWrapUnwrapException, CltuEndecException {
		final boolean writeScmf = appContext.getBean(ScmfProperties.class).getWriteScmf();

		final IScmf scmf = appContext.getBean(IScmfFactory.class).toScmf(plopCltus, writeScmf);

		if (writeScmf) {
			DisplayUtility.printMessage(appContext, "Wrote SCMF to "
					+ scmf.getOriginalFile() + "\n\n");
		}

		return (scmf);
	}

    @Override
    public void sendPdus(final byte[] pdu, final int vcid, final int scid, final int apid)
            throws UplinkException {
        throw new UnsupportedOperationException("This adapter cannot send PDU's");
    }

	/**
	 * Used to track and set FSN for the CFDP-AUTO uplink path
	 * MPCS-11666 add method
	 *
	 * @return the current frame sequence number to set
	 */
	protected int getPduFrameSequenceCounter() {
		// wrap after 255
	    return pduFrameSequenceCounter.get() == FRAME_SEQUENCE_WRAP_MAX ?
			    pduFrameSequenceCounter.getAndSet(0) :
			    pduFrameSequenceCounter.getAndIncrement();
    }

}
