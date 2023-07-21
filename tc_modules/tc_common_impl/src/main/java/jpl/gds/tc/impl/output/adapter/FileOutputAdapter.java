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
package jpl.gds.tc.impl.output.adapter;

import jpl.gds.shared.file.FileUtility;
import jpl.gds.shared.metadata.InvalidMetadataException;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.tc.api.*;
import jpl.gds.tc.api.cltu.ICltu;
import jpl.gds.tc.api.cltu.ICltuFactory;
import jpl.gds.tc.api.command.ICommand;
import jpl.gds.tc.api.command.IFlightCommand;
import jpl.gds.tc.api.command.ISseCommand;
import jpl.gds.tc.api.config.PlopProperties;
import jpl.gds.tc.api.exception.CltuEndecException;
import jpl.gds.tc.api.exception.FrameWrapUnwrapException;
import jpl.gds.tc.api.exception.SessionOverflowException;
import jpl.gds.tc.api.exception.UplinkException;
import jpl.gds.tc.api.message.IScmfCommandMessage;
import jpl.gds.tc.api.plop.ICommandLoadBuilder;
import jpl.gds.tc.api.through.ITcThroughBuilder;
import jpl.gds.tc.impl.SseCommand;
import jpl.gds.tc.impl.plop.CommandLoadBuilder;
import org.springframework.context.ApplicationContext;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileOutputAdapter extends AbstractOutputAdapter {
	/**
	 * Whether or not the output file has already been opened once by this Java
	 * process. This is our cue whether to append to the file or start empty.
	 */
	protected static boolean hasBeenOpened = false;

	protected File outputFile;

	public FileOutputAdapter(final ApplicationContext appContext) throws InvalidMetadataException {
		super(appContext);

		this.outputFile = null;
	}

	public FileOutputAdapter(final ApplicationContext appContext, final String filename) throws InvalidMetadataException {
		super(appContext);

		this.outputFile = new File(filename);
	}

	@Override
	protected IUplinkResponse transmitPlopCltus(final List<ICltu> plopCltus,
			final IScmf scmf) throws UplinkException {
		Long scmfChecksum = null;
		Long totalCltus = null;

		try {
			scmfChecksum = Long.valueOf(scmf.getFileChecksum());
			totalCltus = Long.valueOf(scmf.getCltuCount());
		} catch (final Exception e) {
		}

		final IUplinkResponse response = new GenericUplinkResponse("",
				CommandStatusType.Send_Failure, null, "", scmfChecksum,
				totalCltus, new AccurateDateTime());
		response.setFailureReason(UplinkFailureReason.AMPCS_SEND_FAILURE);

		if (this.outputFile == null) {
			final String diagMessage = "Output file location has not been set for FileOutputAdapter.";
			response.setDiagnosticMessage(diagMessage);
			response.setSuccessful(false);

			throw new UplinkException(diagMessage, response);
		}

		OutputStream os = null;
		try {
			os = new FileOutputStream(this.outputFile, hasBeenOpened);
			hasBeenOpened = true;

			for (final ICltu cltu : plopCltus) {
				os.write(cltu.getPlopBytes());
			}
			os.flush();
		} catch (final IOException e) {
			final String diagMessage = "Failed to write CLTUs to output stream: "
					+ e.getMessage() == null ? e.toString() : e.getMessage();
			response.setDiagnosticMessage(diagMessage);
			response.setSuccessful(false);

			throw new UplinkException(diagMessage, e, response);
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (final IOException e) {
					// don't care
				}
			}
		}

		trace.info("Wrote " + plopCltus.size() + " CLTUs to the output stream.");
		response.setFailureReason(null);
		response.setStatus(CommandStatusType.Radiated);
		return response;
	}

	@Override
	protected IUplinkResponse transmitScmf(final IScmf scmf)
			throws UplinkException {
		Long scmfChecksum = null;
		Long totalCltus = null;

		try {
			scmfChecksum = Long.valueOf(scmf.getFileChecksum());
			totalCltus = Long.valueOf(scmf.getCltuCount());
		} catch (final Exception e) {
		}

		final IUplinkResponse response = new GenericUplinkResponse("",
				CommandStatusType.Send_Failure, null, "", scmfChecksum,
				totalCltus, new AccurateDateTime());
		response.setFailureReason(UplinkFailureReason.AMPCS_SEND_FAILURE);

		if (scmf == null) {
			throw new IllegalArgumentException("Null input SCMF");
		} else if (this.outputFile == null) {
			final String diagMessage = "Output file location has not been set for FileOutputAdapter.";
			response.setDiagnosticMessage(diagMessage);
			response.setSuccessful(false);

			throw new UplinkException(diagMessage, response);
		}

		final List<IScmfCommandMessage> messages = scmf.getCommandMessages();
		if (messages.size() == 0) {
			final String errMsg = "No CLTUs available in the input SCMF.\n";
			DisplayUtility
					.printMessage(appContext, "No CLTUs available in the input SCMF.\n");
			response.setDiagnosticMessage(errMsg);
			response.setFailureReason(UplinkFailureReason.AMPCS_SEND_FAILURE);
			response.setStatus(CommandStatusType.Send_Failure);
			response.setSuccessful(false);
			return response;
		}

		OutputStream os = null;
		try {
			os = new FileOutputStream(this.outputFile, hasBeenOpened);
			hasBeenOpened = true;

			for (final IScmfCommandMessage msg : messages) {
				os.write(msg.getData());
			}
			os.flush();
		} catch (final Exception e) {
			final String diagMessage = "Failed to write CLTUs to output stream: "
					+ e.getMessage() == null ? e.toString() : e.getMessage();
			response.setDiagnosticMessage(diagMessage);
			response.setSuccessful(false);

			throw new UplinkException(diagMessage, e, response);
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (final IOException e) {
					// don't care
				}
			}
		}

		trace.info("Wrote " + messages.size() + " CLTUs to the output stream.");
		response.setFailureReason(null);
		response.setStatus(CommandStatusType.Radiated);
		return response;
	}

	@Override
	protected IUplinkResponse transmitRawUplinkData(final File inputFile,
			final boolean isHex, final IScmf scmf) throws UplinkException {
		Long scmfChecksum = null;
		Long totalCltus = null;

		try {
			scmfChecksum = Long.valueOf(scmf.getFileChecksum());
			totalCltus = Long.valueOf(scmf.getCltuCount());
		} catch (final Exception e) {
		}

		final IUplinkResponse response = new GenericUplinkResponse("",
				CommandStatusType.Send_Failure, null, "", scmfChecksum,
				totalCltus, new AccurateDateTime());
		response.setFailureReason(UplinkFailureReason.AMPCS_SEND_FAILURE);

		if (inputFile == null) {
			throw new IllegalArgumentException("Input file was null");
		} else if (inputFile.exists() == false) {
			throw new IllegalArgumentException("The input file "
					+ inputFile.getName() + " does not exist.");
		} else if (this.outputFile == null) {
			final String diagMessage = "Output file location has not been set for FileOutputAdapter.";
			response.setDiagnosticMessage(diagMessage);

			throw new UplinkException(diagMessage, response);
		}

		OutputStream os = null;
		try {
			os = new FileOutputStream(this.outputFile, hasBeenOpened);
			hasBeenOpened = true;

			FileUtility.writeFileToOutputStream(os, inputFile, isHex);
		} catch (final Exception e) {
			final String diagMessage = "Failed to write raw data to output stream: "
					+ e.getMessage() == null ? e.toString() : e.getMessage();
			response.setDiagnosticMessage(diagMessage);
			response.setSuccessful(false);

			throw new UplinkException(diagMessage, e, response);
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (final IOException e) {
					// don't care
				}
			}
		}

		response.setFailureReason(null);
		response.setStatus(CommandStatusType.Radiated);
		return response;
	}

	private void transmitSseCommand(final String commandString)
			throws IOException {
		if (commandString == null) {
			throw new IllegalArgumentException("Input SSE command was null");
		}

		PrintWriter pw = null;
		OutputStream os = null;
		try {
			os = new FileOutputStream(this.outputFile, hasBeenOpened);
			hasBeenOpened = true;

			pw = new PrintWriter(os);

			pw.write(commandString);
			pw.flush();
		} finally {
			if (pw != null) {
				pw.close();
			}

			if (os != null) {
				os.close();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * jpl.gds.tc.impl.output.adapter.AbstractOutputAdapter#sendCommands(java
	 * .util.List, int)
	 * 
	 * MPCS-7400 - 8/27/2015: Added UplinkException throws
	 * declaration, because upon error, the exception will be thrown to the
	 * caller rather than a simple return.
	 */
	@Override
	public void sendCommands(final List<ICommand> commands, final int id) throws UplinkException {

		List<IFlightCommand> queuedCommands = new ArrayList<>();

		try {
			for (final ICommand command : commands) {
				if (command instanceof ISseCommand) {
					if(queuedCommands.size() > 0) {
						setScmfConfigFilenameCommands(queuedCommands);

						sendViaThroughBuilder(queuedCommands, id);
						queuedCommands.clear();
					}
					sendSseCommand((SseCommand) command, id);
				} else {
					IFlightCommand cmd = (IFlightCommand)command;
					queuedCommands.add(cmd);
				}
			}

			if(!queuedCommands.isEmpty()) {
				setScmfConfigFilenameCommands(queuedCommands);

				sendViaThroughBuilder(queuedCommands, id);
			}
		} catch (final Exception e) {
			final String errorMessage = e.getMessage() == null ? e.toString() : e
					.getMessage();

			final IUplinkResponse response = new GenericUplinkResponse("",
					CommandStatusType.Send_Failure,
					UplinkFailureReason.AMPCS_SEND_FAILURE,
					"Could not write binary output file to "
							+ this.outputFile.getAbsolutePath() + ": "
							+ errorMessage, null, null, new AccurateDateTime());

			response.setDiagnosticMessage(errorMessage);
			response.setFailureReason(UplinkFailureReason.AMPCS_SEND_FAILURE);
			response.setStatus(CommandStatusType.Send_Failure);
			response.setSuccessful(false);

			messageUtil.sendCommandMessages(commands,
					response, -1);

			/*
			 * MPCS-7400 - 8/27/2015: The superclass declaration of
			 * this method already has the UplinkException throws declaration,
			 * which means the callers already expect this type of exception to
			 * be thrown. For some reason, it was never thrown in this specific
			 * method, only returned. To fix MPCS-7400, the throwing of the
			 * exception here seems very fitting.
			 */
			if (e instanceof UplinkException) {
				throw (UplinkException) e;
			}

			return;
		}

		trace.info("Wrote binary output file to "
				+ this.outputFile.getAbsolutePath());
	}

	@Override
	public void sendFileLoads(final List<ICommandFileLoad> fileLoads, final int id) {
		List<ICltu> plopCltus = null;
		try {
			/*
			 * If multiple file loads are sent at once, each one is supposed to
			 * go into its own uplink session, but they should all go into the
			 * same command load
			 */

			final ICommandLoadBuilder commandLoad = new CommandLoadBuilder();

			// MPCS-11459: need to use a legacy CLTU builder to account for frame randomization
			ICltuFactory legacyCltuBuilder = appContext.getBean(TcApiBeans.LEGACY_CLTU_FACTORY, ICltuFactory.class);

			for (final ICommandFileLoad load : fileLoads) {
				final File inputFile = new File(load.getInputFileName());
				load.setInputFileName(inputFile.getAbsolutePath());
                sessionBuilder.addFrames(frameFactory.createFileLoadFrames(load));
                final List<ICltu> cltuList = legacyCltuBuilder.createCltusFromFrames(sessionBuilder.getSessionFrames());
                // MPCS-11509 - Must clear session builder after each pass
                sessionBuilder.clear();
				commandLoad.addCltus(cltuList);
			}

			plopCltus = commandLoad.getPlopCltus(appContext.getBean(PlopProperties.class));
		} catch (final FrameWrapUnwrapException | CltuEndecException | SessionOverflowException e) {
			final String errorMessage = e.getMessage() == null ? e.toString() : e
					.getMessage();

			final IUplinkResponse response = new GenericUplinkResponse("",
					CommandStatusType.Send_Failure,
					UplinkFailureReason.AMPCS_SEND_FAILURE,
					"Uplink session overflow encountered while translating file loads: "
							+ errorMessage, null, null, new AccurateDateTime());

			response.setDiagnosticMessage(errorMessage);
			response.setFailureReason(UplinkFailureReason.AMPCS_SEND_FAILURE);
			response.setStatus(CommandStatusType.Send_Failure);
			response.setSuccessful(false);

			messageUtil.sendFileLoadMessages(fileLoads,
					response, -1);
			return;
		}

		try {
			transmitPlopCltus(plopCltus, null);
		} catch (final Exception e) {
			final String errorMessage = e.getMessage() == null ? e.toString() : e
					.getMessage();

			final IUplinkResponse response = new GenericUplinkResponse("",
					CommandStatusType.Send_Failure,
					UplinkFailureReason.AMPCS_SEND_FAILURE,
					"Could not write binary output file to "
							+ this.outputFile.getAbsolutePath() + ": "
							+ errorMessage, null, null, new AccurateDateTime());

			response.setDiagnosticMessage(errorMessage);
			response.setFailureReason(UplinkFailureReason.AMPCS_SEND_FAILURE);
			response.setStatus(CommandStatusType.Send_Failure);
			response.setSuccessful(false);

			messageUtil.sendFileLoadMessages(fileLoads,
					response, -1);
			return;
		}

		trace.info("Wrote binary output file to "
				+ this.outputFile.getAbsolutePath());
	}

	@Override
	public void sendSseCommand(final ISseCommand command, final int id) throws IOException {
		if (command == null) {
			throw new IllegalArgumentException("Input SSE command was null");
		} else if (this.outputFile == null) {
			throw new IOException(
					"Output file location has not been set for FileOutputAdapter.");
		}

		String commandString = command.getCommandString(false);
		if (commandString.endsWith("\n") == false) {
			commandString += "\n";
		}

		transmitSseCommand(commandString);

		trace.info("Wrote binary output file to "
				+ this.outputFile.getAbsolutePath());
	}

	@Override
	public void sendScmf(final IScmf scmf, final int id) {
		if (scmf == null) {
			throw new IllegalArgumentException("Null input SCMF");
		}

		final List<IScmfCommandMessage> messages = scmf.getCommandMessages();
		if (messages.size() == 0) {
			final String errorMessage = "No CLTUs found in input SCMF.";
			Long scmfChecksum = null;
			Long totalCltus = null;

			try {
				scmfChecksum = Long.valueOf(scmf.getFileChecksum());
				totalCltus = Long.valueOf(scmf.getCltuCount());
			} catch (final Exception e) {
			}

			final IUplinkResponse response = new GenericUplinkResponse("",
					CommandStatusType.Send_Failure,
					UplinkFailureReason.AMPCS_SEND_FAILURE, errorMessage,
					scmfChecksum, totalCltus, new AccurateDateTime());

			response.setDiagnosticMessage(errorMessage);
			response.setFailureReason(UplinkFailureReason.AMPCS_SEND_FAILURE);
			response.setStatus(CommandStatusType.Send_Failure);
			response.setSuccessful(false);

			messageUtil.sendScmfMessage(
					new File(scmf.getOriginalFile()), response, -1);
			return;
		}

		try {
			transmitScmf(scmf);
		} catch (final Exception e) {
			final String errorMessage = e.getMessage() == null ? e.toString() : e
					.getMessage();

			final IUplinkResponse response = new GenericUplinkResponse("",
					CommandStatusType.Send_Failure,
					UplinkFailureReason.AMPCS_SEND_FAILURE,
					"Could not write binary output file to "
							+ this.outputFile.getAbsolutePath() + ": "
							+ errorMessage, null, null, new AccurateDateTime());

			response.setDiagnosticMessage(errorMessage);
			response.setFailureReason(UplinkFailureReason.AMPCS_SEND_FAILURE);
			response.setStatus(CommandStatusType.Send_Failure);
			response.setSuccessful(false);

			messageUtil.sendScmfMessage(
					new File(scmf.getOriginalFile()), response, -1);
			return;
		}

		trace.info("Wrote binary output file to "
				+ this.outputFile.getAbsolutePath());
	}

	@Override
	public void sendRawUplinkData(final File inputFile, final boolean isHex,
			final boolean isFaultInjected, final int id) {
		if (inputFile == null) {
			throw new IllegalArgumentException("Input file was null");
		} else if (inputFile.exists() == false) {
			throw new IllegalArgumentException("The input file "
					+ inputFile.getName() + " does not exist.");
		}

		try {
			transmitRawUplinkData(inputFile, isHex, null);
		} catch (final Exception e) {
			final String errorMessage = e.getMessage() == null ? e.toString() : e
					.getMessage();

			final IUplinkResponse response = new GenericUplinkResponse("",
					CommandStatusType.Send_Failure,
					UplinkFailureReason.AMPCS_SEND_FAILURE,
					"Could not write binary output file to "
							+ this.outputFile.getAbsolutePath() + ": "
							+ errorMessage, null, null, new AccurateDateTime());

			response.setDiagnosticMessage(errorMessage);
			response.setFailureReason(UplinkFailureReason.AMPCS_SEND_FAILURE);
			response.setStatus(CommandStatusType.Send_Failure);
			response.setSuccessful(false);

			messageUtil.sendRawUplinkDataMessage(
					inputFile, response, isFaultInjected, -1);
			return;
		}

		trace.info("Wrote binary output file to "
				+ this.outputFile.getAbsolutePath());
	}

}
