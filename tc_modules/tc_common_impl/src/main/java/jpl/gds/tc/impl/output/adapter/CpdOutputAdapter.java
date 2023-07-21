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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import gov.nasa.jpl.icmd.schema.InsertResponseType;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.metadata.InvalidMetadataException;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.tc.api.CommandStatusType;
import jpl.gds.tc.api.DisplayUtility;
import jpl.gds.tc.api.IScmf;
import jpl.gds.tc.api.IScmfFactory;
import jpl.gds.tc.api.ITcTransferFrame;
import jpl.gds.tc.api.ITelecommandPacket;
import jpl.gds.tc.api.ITewUtility;
import jpl.gds.tc.api.IUplinkResponse;
import jpl.gds.tc.api.TcApiBeans;
import jpl.gds.tc.api.UplinkFailureReason;
import jpl.gds.tc.api.cltu.ICltu;
import jpl.gds.tc.api.command.ICommand;
import jpl.gds.tc.api.command.IFlightCommand;
import jpl.gds.tc.api.exception.CltuEndecException;
import jpl.gds.tc.api.exception.FrameWrapUnwrapException;
import jpl.gds.tc.api.exception.RawOutputException;
import jpl.gds.tc.api.exception.ScmfWrapUnwrapException;
import jpl.gds.tc.api.exception.UplinkException;
import jpl.gds.tc.api.icmd.ICpdClient;
import jpl.gds.tc.api.icmd.exception.AuthenticationException;
import jpl.gds.tc.api.icmd.exception.AuthorizationException;
import jpl.gds.tc.api.icmd.exception.CpdConnectionException;
import jpl.gds.tc.api.icmd.exception.ICmdException;
import jpl.gds.tc.api.output.OutputFileNameFactory;
import jpl.gds.tc.api.packet.ITelecommandPacketFactory;
import jpl.gds.tc.api.through.ITcThroughBuilder;
import jpl.gds.tc.impl.SseCommand;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This is an output adapter that is used when uplinking data to CPD
 *
 * @since AMPCS R3
 */
public class CpdOutputAdapter extends AbstractOutputAdapter {
	private static final long TENTH_SECOND = 100L;
	private static final long ONE_SECOND = 1000L;
	private static final long PING_TIMEOUT = 5L * ONE_SECOND;

	/**
	 * Default constructor.
	 * @param appContext the ApplicationContext in which this object is being used
	 * @throws InvalidMetadataException 
	 * @throws BeansException 
	 */
	public CpdOutputAdapter(final ApplicationContext appContext) {
		super(appContext);
		sendInternalMessagesWithScmf = false;
	}

	/** The CPD client used to talk to CPD */
	private ICpdClient client;

	/**
	 * Initialize by creating client and trying to ping ICMD. The ping is
	 * necessary in some cases to make security services work properly.
	 *
	 * @throws RawOutputException If unable to initialize and ping
	 */
	@Override
	public void init() throws RawOutputException {
		super.init();
		try {
            client = appContext.getBean(ICpdClient.class);

			long delta = PING_TIMEOUT;
			boolean ok = false;
			final long timeout = System.currentTimeMillis() + delta;

			while (delta > 0L) {
				ok = client.pingCommandService();

				if (ok) {
					break;
				}

				SleepUtilities.checkedSleep(Math.min(delta, TENTH_SECOND));

				delta = timeout - System.currentTimeMillis();
			}

			if (!ok) {
				throw new RawOutputException(
						"Unable to initialize CPD Client: Ping Timeout. Please check the accuracy of the configured/provided FSW host/port.");
			}
		} catch(final ICmdException e) {
			/** MPCS-7767 - 03/16/16: Added specific catch.  The MTAK/AUTO python interface for errors
			 * depends on preserving the original exception type; the catch-all that wraps exceptions in an ICmdException
			 * causes information loss.
			 */
			throw e;
		} catch (final Exception e) {
            throw new ICmdException("Unable to initialize CPD Client: " + ExceptionTools.getMessage(e));
		}
	}

	/**
	 * Send SCMF to CPD
	 *
	 * @param scmf the SCMF to send to CPD
	 * @return an uplink response detailing the results of the uplink
	 * @throws UplinkException if CPD responds with an error
	 * @throws ICmdException if the uplink fails to get a response from CPD or
	 *             uplink fails
	 */
	private IUplinkResponse sendScmfToCpd(final IScmf scmf) throws UplinkException,
			ICmdException {
		CpdUplinkResponse resp = null;
		InsertResponseType cpdResp;
		try {
			cpdResp = client.sendScmf(scmf);
			resp = new CpdUplinkResponse(cpdResp, scmf);
			if (!resp.isSuccessful()) {
				throw new UplinkException(resp.getDiagnosticMessage(), resp);
			}
		} catch (final CpdConnectionException e) {
			final String errorMessage = e.getMessage() == null ? e.toString() : e
					.getMessage();
			resp = new CpdUplinkResponse("", CommandStatusType.Send_Failure,
					false, scmf);
			resp.setFailureReason(UplinkFailureReason.COMMAND_SERVICE_CONNECTION_ERROR);
			resp.setDiagnosticMessage(errorMessage);
			resp.setSuccessful(false);
			throw new UplinkException("Unable to contact CPD server: "
					+ errorMessage, e, resp);
		} catch (final AuthenticationException e) {
			final String errorMessage = e.getMessage() == null ? e.toString() : e
					.getMessage();
			resp = new CpdUplinkResponse("", CommandStatusType.Send_Failure,
					false, scmf);
			resp.setFailureReason(UplinkFailureReason.AUTHENTICATION_ERROR);
			resp.setDiagnosticMessage(errorMessage);
			resp.setSuccessful(false);
			throw new UplinkException("Authentication Error: " + errorMessage,
					e, resp);
		} catch (final AuthorizationException e) {
			final String errorMessage = e.getMessage() == null ? e.toString() : e
					.getMessage();
			resp = new CpdUplinkResponse("", CommandStatusType.Send_Failure,
					false, scmf);
			resp.setFailureReason(UplinkFailureReason.AUTHORIZATION_ERROR);
			resp.setDiagnosticMessage(errorMessage);
			resp.setSuccessful(false);
			throw new UplinkException("Authorization Error: " + errorMessage,
					e, resp);
		} catch (final ScmfWrapUnwrapException e) {
			final String errorMessage = e.getMessage() == null ? e.toString() : e
					.getMessage();
			resp = new CpdUplinkResponse("", CommandStatusType.Send_Failure,
					false, scmf);
			resp.setFailureReason(UplinkFailureReason.AMPCS_SEND_FAILURE);
			resp.setDiagnosticMessage(errorMessage);
			resp.setSuccessful(false);
			throw new UplinkException(errorMessage, e, resp);
		}

		return resp;

	}

	@Override
	protected IUplinkResponse transmitPlopCltus(final List<ICltu> plopCltus, final IScmf scmf)
			throws UplinkException {
		IUplinkResponse resp = null;
		try {
			resp = sendScmfToCpd(scmf);
		} catch (final ICmdException e) {
			final IUplinkResponse response = new CpdUplinkResponse("",
					CommandStatusType.Send_Failure, false, scmf);
			throw new UplinkException("Failed to transmit command to CPD: "
					+ e.getMessage(), e, response);
		}

		return resp;
	}

	@Override
	protected IUplinkResponse transmitScmf(final IScmf scmf) throws UplinkException {
		IUplinkResponse resp = null;
		try {
			resp = sendScmfToCpd(scmf);
		} catch (final ICmdException e) {
			final IUplinkResponse response = new CpdUplinkResponse("",
					CommandStatusType.Send_Failure, false, scmf);
			throw new UplinkException("Failed to transmit SCMF to CPD: "
					+ e.getMessage(), e, response);
		}

		return resp;
	}

	@Override
	protected IUplinkResponse transmitRawUplinkData(final File transmitFile,
			final boolean isHex, final IScmf scmf) throws UplinkException {
		IUplinkResponse resp = null;
		try {
			resp = sendScmfToCpd(scmf);
		} catch (final ICmdException e) {
			final IUplinkResponse response = new CpdUplinkResponse("",
					CommandStatusType.Send_Failure, false, scmf);
			throw new UplinkException("Failed to transmit raw data to CPD: "
					+ e.getMessage(), e, response);
		}

		return resp;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see jpl.gds.tc.impl.output.adapter.AbstractOutputAdapter#sendCommands(List, int)
	 */
	@Override
	public void sendCommands(final List<ICommand> commands, final int id) throws UplinkException {
		final ScheduledExecutorService es = Executors.newSingleThreadScheduledExecutor(
				new ThreadFactoryBuilder().setNameFormat("cpd-output-updater-%d").build());
		final AtomicInteger commandCounter = new AtomicInteger(0);
		final int total = commands.size();
		es.scheduleAtFixedRate(() -> {
			final int number = commandCounter.get();
			final int percentage = (int) Math.floor((number * 100.0 / total));
			DisplayUtility.printMessage(appContext, "Forwarded " + number + "/" + total + " (" + percentage + "%)\n");
		}, 0, 10, TimeUnit.SECONDS);
		try {
			for (final ICommand command : commands) {
				final String outputLocation = OutputFileNameFactory.createNameForCommand(appContext, command);
				scmfConfig.setScmfName(outputLocation);

				if (command instanceof SseCommand) {
					sendSseCommand((SseCommand) command, id);
				} else {
					IFlightCommand cmd = (IFlightCommand) command;
					sendViaThroughBuilder(Collections.singletonList(cmd), id);
				}

				DisplayUtility.printMessage(appContext, "Wrote SCMF to path: " + outputLocation + "\n");

				commandCounter.incrementAndGet();
			}

			if(scmfConfig.getOnlyWriteScmf()) {
				return;
			}
		} catch (final UplinkException e) {
			this.messageUtil.sendCommandMessage(commands.get(commandCounter.get()),
					e.getUplinkResponse(), id);
			throw e;
		} catch (final Exception e) {
			final String errorMessage = e.getMessage() == null ? "AMPCS encountered an error: "
					+ e.toString()
					: e.getMessage();

			final IUplinkResponse resp = new GenericUplinkResponse("",
					CommandStatusType.Send_Failure,
					UplinkFailureReason.AMPCS_SEND_FAILURE, errorMessage, null,
					null, new AccurateDateTime());
			this.messageUtil.sendCommandMessage(commands.get(commandCounter.get()),
					resp, id);

			throw new UplinkException(errorMessage, e, resp);

		} finally {
			scmfConfig.setScmfName(null);
			es.shutdownNow();
		}

		// Print at debug level
		DisplayUtility.printMessage(appContext, "Forwarded command(s) to CPD.\n\n", true);
	}

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void sendPdus(final byte[] pdu, final int vcid, final int scid, final int apid)
            throws UplinkException {
        trace.debug(this.getClass().getName(), " received pdus to send...");
        trace.debug("pduSize=", pdu.length, " vcid=", vcid, " scid=", scid);

        final List<ITcTransferFrame> frames = new ArrayList<>(1);

        try {
			final ITewUtility tewUtility = appContext.getBean(TcApiBeans.MPS_TEW_UTILITY, ITewUtility.class);

			if (missionConfig.isUsingTcPackets()) {
				// TODO: Do we need to pass in sequence counters for the packet? Where do we hold the count
				final ITelecommandPacketFactory pktFactory = appContext.getBean(ITelecommandPacketFactory.class);
				final ITelecommandPacket pkt = pktFactory.buildPacketFromPdu(pdu, apid);
				final ITcTransferFrame frame = tewUtility.wrapBytesToFrame(pkt.getBytes(), vcid, getPduFrameSequenceCounter());
				frames.add(frame);
			} else {
				final ITcTransferFrame frame = tewUtility.wrapBytesToFrame(pdu, vcid, getPduFrameSequenceCounter());
				frames.add(frame);
			}
        } catch (FrameWrapUnwrapException e) {
			throw new UplinkException(e);
		}

        List<ICltu> cltus = new ArrayList<>();

        IUplinkResponse response;
        IScmf scmf;
        try {
			cltus = cltuBuilder.createPlopCltusFromFrames(frames);
			trace.debug("Successfully created ", cltus.size(), " cltu's");

            if (scmfConfig.getScmfName() == null) {
                scmfConfig.setScmfName(OutputFileNameFactory.createNameForPduFile(appContext));
            }

            trace.debug("Creating scmf from cltu...");
            // for CPD, we need to write SCMF's to disk
            scmf = this.appContext.getBean(IScmfFactory.class).toScmf(cltus, true);
            trace.debug("Created SCMF ", scmf.toString());

            trace.debug("Transmitting CLTU length=", cltus.get(0).getHexDisplayString());
            response = transmitPlopCltus(cltus, scmf);
        }
        catch (CltuEndecException | ScmfWrapUnwrapException | IOException e) {
            response = new GenericUplinkResponse();
            response.setDiagnosticMessage(ExceptionTools.getMessage(e));
            response.setFailureReason(UplinkFailureReason.AMPCS_SEND_FAILURE);
            response.setStatus(CommandStatusType.Failed);
	        response.setSuccessful(false);
            trace.error("Error creating SCMF" + ExceptionTools.getMessage(e));
        }
        catch (final UplinkException e) {
            response = e.getUplinkResponse();
            trace.error("Error transmitting data " + ExceptionTools.getMessage(e));
        }

        trace.debug("Uplink Response: ", response.getStatus());
        DisplayUtility.writeCltusToDisplay(appContext, cltus);
        if (response.isSuccessful()) {
            DisplayUtility.printMessage(appContext, this.getClass().getName() + " Successfully radiated CLTU");
        }
        else {
            DisplayUtility.printMessage(appContext, "Failure radiating\n" + response.getDiagnosticMessage() + "\n");
        }

        // TODO: use CommandMessageUtility to write a command message? Do we need to respond to cpd/cmdsink?

    }
}
