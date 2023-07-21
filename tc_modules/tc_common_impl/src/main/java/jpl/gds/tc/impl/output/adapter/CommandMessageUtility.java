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

import java.io.File;
import java.util.List;

import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.command.CommandDefinitionType;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.IPublishableLogMessage;
import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.CommonMessageType;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.tc.api.CommandStatusType;
import jpl.gds.tc.api.ICommandFileLoad;
import jpl.gds.tc.api.IScmf;
import jpl.gds.tc.api.IUplinkResponse;
import jpl.gds.tc.api.UplinkFailureReason;
import jpl.gds.tc.api.command.ICommand;
import jpl.gds.tc.api.command.IFlightCommand;
import jpl.gds.tc.api.command.ISseCommand;
import jpl.gds.tc.api.exception.ScmfParseException;
import jpl.gds.tc.api.message.ICommandMessage;
import jpl.gds.tc.api.message.ICommandMessageFactory;
import jpl.gds.tc.api.message.ICpdUplinkMessage;
import jpl.gds.tc.api.message.IFileLoadMessage;
import jpl.gds.tc.api.message.IRawUplinkDataMessage;
import jpl.gds.tc.api.message.ITransmittableCommandMessage;
import jpl.gds.tc.api.message.IUplinkMessage;
import jpl.gds.tc.api.output.ICommandMessageUtility;
import jpl.gds.tc.impl.FlightCommand;
import jpl.gds.tc.impl.SseCommand;
import jpl.gds.tc.impl.message.FlightSoftwareCommandMessage;
import jpl.gds.tc.impl.message.HardwareCommandMessage;
import jpl.gds.tc.impl.message.SequenceDirectiveMessage;


/**
 * Utility methods for command messaging.
 * This utility is used by uplink output classes to quickly and easily publish 
 * messages for commands and their subsequent statuses.
 *
 *
 * MPCS-10813 - 04/09/19 - Now implements ICommandMessageUtility
 */
public class CommandMessageUtility implements ICommandMessageUtility {

	/** Tracer */
	protected final Tracer trace; 


	/** Message context */
	protected final IMessagePublicationBus context;

    private final IStatusMessageFactory statusMessageFactory;
    private final ICommandMessageFactory cmdMessageFactory;

	/**
	 * Constructor.
	 */
	public CommandMessageUtility(final ApplicationContext appContext) {
        trace = TraceManager.getTracer(appContext, Loggers.UPLINK);
	    context = appContext.getBean(IMessagePublicationBus.class);
        statusMessageFactory = appContext.getBean(IStatusMessageFactory.class);
        cmdMessageFactory = appContext.getBean(ICommandMessageFactory.class);
	}

    /**
     * Private helper method for publishing log messages
     * 
     * @param level
     *            The message severity
     * @param msg
     *            the IMessage to log
     */
    private void publishLog(final TraceSeverity level, final IMessage msg) {
        final IPublishableLogMessage lm = statusMessageFactory.createPublishableLogMessage(level, msg.toString(),
                                                                                           LogMessageType.UPLINK,
                                                                                           CommonMessageType.Log);
        trace.log(lm);
        context.publish(msg);
    }

    /**
     * Private helper method for publishing log messages
     * 
     * @param level
     *            The message severity
     * @param msg
     *            the message to log
     */
    private void publishLog(final TraceSeverity level, final String msg) {
        final IPublishableLogMessage lm = statusMessageFactory.createPublishableLogMessage(level, msg,
                                                                                           LogMessageType.UPLINK,
                                                                                           CommonMessageType.Log);
        trace.log(lm);
        context.publish(lm);
    }


	@Override
	public void sendCommandMessages(
			final List<? extends ICommand> commands,
			final IUplinkResponse uplinkResponse, final int id) {
		if (commands == null) {
			throw new IllegalArgumentException("Null input command list");
		}

		for (final ICommand command : commands) {
			sendCommandMessage(command, uplinkResponse, id);
		}
	}


	@Override
	public void sendCommandMessage(final ICommand command,
			final IUplinkResponse uplinkResponse, final int id) {
		if (command == null) {
			throw new IllegalArgumentException("Null input command");
		}

		if (command instanceof ISseCommand) {
			sendSseCommandMessage((SseCommand) command, id, uplinkResponse.isSuccessful());
		} else {
			sendFlightCommandMessage((FlightCommand)command, uplinkResponse, id);
		}
	}


	@Override
	public void sendFileLoadMessages(final List<ICommandFileLoad> fileLoads,
			final IUplinkResponse uplinkResponse, final int id) {
		if (fileLoads == null) {
			throw new IllegalArgumentException("Null input file load list");
		}

		for (final ICommandFileLoad fileLoad : fileLoads) {
			sendFileLoadMessage(fileLoad, uplinkResponse, id);

			//if is an error, just send 1 message
			if(!uplinkResponse.isSuccessful()) {
				return;
			}
		}
	}


	@Override
	public void sendFileLoadMessage(final ICommandFileLoad fileLoad,
			IUplinkResponse uplinkResponse, final int id) {
		if (fileLoad == null) {
			throw new IllegalArgumentException("Null input file load");
		}

		if (uplinkResponse == null) {
			uplinkResponse = new GenericUplinkResponse("",
					CommandStatusType.UNKNOWN, UplinkFailureReason.UNKNOWN,
					"Did not receive an uplink response", null, null, new AccurateDateTime());
		}

		final IFileLoadMessage message = cmdMessageFactory.createFileLoadMessage(fileLoad);

		message.setICmdRequestId(uplinkResponse.getRequestId());
		message.setICmdRequestStatus(uplinkResponse.getStatus());
		message.setICmdRequestFailureReason(uplinkResponse.getFailureReason());
		message.setTransmitEventId(id);
		message.setChecksum(uplinkResponse.getScmfChecksum());
		message.setTotalCltus(uplinkResponse.getTotalCltus());
		message.setEventTime(uplinkResponse.getStatusChangeTime());

        publishLog(TraceSeverity.INFO, message);
	}


	@Override
	public void sendFlightCommandMessage(final IFlightCommand command,
			IUplinkResponse uplinkResponse, final int transmitEventId) {
		if (command == null) {
			throw new IllegalArgumentException("Null input command");
		}

		if (uplinkResponse == null) {
			uplinkResponse = new GenericUplinkResponse("",
					CommandStatusType.UNKNOWN, UplinkFailureReason.UNKNOWN,
					"Did not receive an uplink response", null, null, new AccurateDateTime());
		}

		/* @TODO This code was updated for MPCS-6328 to make sequence directives work
		 *       in the database and the chill_up GUI as they did for MSL. However, if
		 *       it becomes necessary to support sequence directives through CPD, this
		 *       handling is inadequate. Sequence directives will have to be handled like
		 *       FSW and hardware commands. Jira MPCS-6620 has been filed to
		 *       track this issue. - 9/15/14.
		 */       
		IMessage message = null;
		if (command.getDefinition().getType() == CommandDefinitionType.HARDWARE) {
			final HardwareCommandMessage hwMessage = new HardwareCommandMessage(command);
			hwMessage.setICmdRequestId(uplinkResponse.getRequestId());
			hwMessage.setICmdRequestStatus(uplinkResponse.getStatus());
			hwMessage.setICmdRequestFailureReason(uplinkResponse
					.getFailureReason());
			hwMessage.setTransmitEventId(transmitEventId);
			hwMessage.setChecksum(uplinkResponse.getScmfChecksum());
			hwMessage.setTotalCltus(uplinkResponse.getTotalCltus());
			hwMessage.setEventTime(uplinkResponse.getStatusChangeTime());

			message = hwMessage;
		} else if (command.getDefinition().getType() == CommandDefinitionType.FLIGHT) {
			final FlightSoftwareCommandMessage fswMessage = new FlightSoftwareCommandMessage(command.getDatabaseString());
			fswMessage.setICmdRequestId(uplinkResponse.getRequestId());
			fswMessage.setICmdRequestStatus(uplinkResponse.getStatus());
			fswMessage.setICmdRequestFailureReason(uplinkResponse
					.getFailureReason());
			fswMessage.setTransmitEventId(transmitEventId);
			fswMessage.setChecksum(uplinkResponse.getScmfChecksum());
			fswMessage.setTotalCltus(uplinkResponse.getTotalCltus());
			fswMessage.setEventTime(uplinkResponse.getStatusChangeTime());

			message = fswMessage;

		} else if (command.getDefinition().getType()  == CommandDefinitionType.SEQUENCE_DIRECTIVE) {
			/* MPCS-6328 - 9/15/14. Fix failure sending sequence directives. */
		    /* MPCS-9142 - 10/02/17 SequenceDirectiveMessages can now be sent via CPD.
             *       Updated to reflect these changes. */
            final SequenceDirectiveMessage seqMessage = new SequenceDirectiveMessage(command.getDatabaseString());
            
            seqMessage.setICmdRequestId(uplinkResponse.getRequestId());
            seqMessage.setICmdRequestStatus(uplinkResponse.getStatus());
            seqMessage.setICmdRequestFailureReason(uplinkResponse
                    .getFailureReason());
            seqMessage.setTransmitEventId(transmitEventId);
            seqMessage.setChecksum(uplinkResponse.getScmfChecksum());
            seqMessage.setTotalCltus(uplinkResponse.getTotalCltus());
            seqMessage.setEventTime(uplinkResponse.getStatusChangeTime());
            
            message = seqMessage;

		} else {
			final String errorMessage = "Unknown command type " + command.getDefinition().getType()
					+ " in CommandMessageUtility.sendCommandMessage";
            publishLog(TraceSeverity.ERROR, errorMessage);
			return;
		}
        publishLog(TraceSeverity.INFO, message);
	}


	@Override
	public void sendSseCommandMessage(final ISseCommand command, final int id, final boolean isSuccessful) {
		if (command == null) {
			throw new IllegalArgumentException("Null input command");
		}

		final ITransmittableCommandMessage message = cmdMessageFactory.createSseCommandMessage(command.getDatabaseString());
		message.setTransmitEventId(id);
		message.setSuccessful(isSuccessful);

        publishLog(TraceSeverity.INFO, message);
	}


	// MPCS-9142 - 10/02/17 - Removed sendSequenceDirectiveMessage function, no longer used

	@Override
	public void sendScmfMessage(final File scmf,
			IUplinkResponse uplinkResponse, final int id) {
		if (scmf == null) {
			throw new IllegalArgumentException("Null input SCMF file");
		}

		if (uplinkResponse == null) {
			uplinkResponse = new GenericUplinkResponse("",
					CommandStatusType.UNKNOWN, UplinkFailureReason.UNKNOWN,
					"Did not receive an uplink response", null, null, new AccurateDateTime());
		}

		final ICpdUplinkMessage message = cmdMessageFactory.createScmfMessage(scmf.getAbsolutePath());
		message.setOriginalFilename(scmf.getAbsolutePath());
		message.setICmdRequestId(uplinkResponse.getRequestId());
		message.setICmdRequestStatus(uplinkResponse.getStatus());
		message.setICmdRequestFailureReason(uplinkResponse.getFailureReason());
		message.setTransmitEventId(id);
		message.setChecksum(uplinkResponse.getScmfChecksum());
		message.setTotalCltus(uplinkResponse.getTotalCltus());
		message.setEventTime(uplinkResponse.getStatusChangeTime());

        publishLog(TraceSeverity.INFO, message);
	}


	@Override
	public void sendRawUplinkDataMessage(final File dataFile,
			IUplinkResponse uplinkResponse, final boolean isFaultInjected, final int id) {
		if (dataFile == null) {
			throw new IllegalArgumentException("Null input raw data file");
		}

		if (uplinkResponse == null) {
			uplinkResponse = new GenericUplinkResponse("",
					CommandStatusType.UNKNOWN, UplinkFailureReason.UNKNOWN,
					"Did not receive an uplink response", null, null, new AccurateDateTime());
		}

		final IRawUplinkDataMessage message = cmdMessageFactory.createRawUplinkDataMessage(dataFile.getAbsolutePath());
		message.setICmdRequestId(uplinkResponse.getRequestId());
		message.setICmdRequestStatus(uplinkResponse.getStatus());
		message.setICmdRequestFailureReason(uplinkResponse.getFailureReason());
		message.setFaultInjected(isFaultInjected);
		message.setTransmitEventId(id);
		message.setChecksum(uplinkResponse.getScmfChecksum());
		message.setTotalCltus(uplinkResponse.getTotalCltus());
		message.setEventTime(uplinkResponse.getStatusChangeTime());

        publishLog(TraceSeverity.INFO, message);
	}


	@Override
	public void sendScmfInternalMessages(final IScmf scmf,
			IUplinkResponse uplinkResponse, final int id) throws ScmfParseException {
		try {
			if (uplinkResponse == null) {
				uplinkResponse = new GenericUplinkResponse("",
						CommandStatusType.UNKNOWN, UplinkFailureReason.UNKNOWN,
						"Did not receive an uplink response", null, null, new AccurateDateTime());
			}

			final List<IUplinkMessage> scmfContents = scmf
					.getInternalMessagesFromScmf();

			for (final IUplinkMessage msg : scmfContents) {
				if (msg instanceof ICpdUplinkMessage) {
				    final ICpdUplinkMessage icmsg = (ICpdUplinkMessage)msg;
					icmsg.setICmdRequestId(uplinkResponse
							.getRequestId());
					icmsg.setICmdRequestStatus(uplinkResponse.getStatus());
					icmsg.setICmdRequestFailureReason(uplinkResponse
							.getFailureReason());
					icmsg.setChecksum(uplinkResponse
							.getScmfChecksum());
					icmsg.setTotalCltus(uplinkResponse
							.getTotalCltus());
					icmsg.setTransmitEventId(id);
				}

                publishLog(TraceSeverity.INFO, msg);
			}
		} catch (final Exception e) {
            final String message = "Could not parse out contents of SCMF file (No SCMF contents will be logged in the database or sent on the message bus): "
                    + ExceptionTools.getMessage(e);
            publishLog(TraceSeverity.WARN, message);
		}
	}


	@Override
	public void logScmfInternals(final IScmf scmf,
			final IUplinkResponse uplinkResponse) {
		final StringBuilder sb = new StringBuilder(1024);

		try {
			final List<IUplinkMessage> scmfContents = scmf
					.getInternalMessagesFromScmf();

			/*
			 * We're not going to publish the messages. Just log them.
			 */
			for (final IUplinkMessage msg : scmfContents) {
				sb.append((msg instanceof ICommandMessage) ? ((ICommandMessage) msg)
						.getCommandString() : msg.getType());
				sb.append(" transmitted (contained in SCMF ");
				sb.append(scmf.getOriginalFile());
				if (uplinkResponse != null) {
					sb.append(", CPD req ID=");
					sb.append(uplinkResponse.getRequestId());
				}
				sb.append(")");
                trace.log(statusMessageFactory.createPublishableLogMessage(TraceSeverity.INFO, sb.toString(),
                                                                           LogMessageType.UPLINK, msg.getType()));
				sb.setLength(0);
			}

		} catch (final Exception e) {
			final String message = "Could not parse out contents of SCMF file (No SCMF contents will be logged in the database or sent on the message bus): "
                    + ExceptionTools.getMessage(e);
            publishLog(TraceSeverity.WARN, message);
		}

	}

}
