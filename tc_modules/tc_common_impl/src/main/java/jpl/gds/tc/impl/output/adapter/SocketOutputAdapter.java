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
import jpl.gds.common.config.connection.IUplinkConnection;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.file.FileUtility;
import jpl.gds.shared.metadata.InvalidMetadataException;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.tc.api.*;
import jpl.gds.tc.api.cltu.ICltu;
import jpl.gds.tc.api.exception.CltuEndecException;
import jpl.gds.tc.api.exception.FrameWrapUnwrapException;
import jpl.gds.tc.api.exception.UplinkException;
import jpl.gds.tc.api.message.IScmfCommandMessage;
import jpl.gds.tc.api.packet.ITelecommandPacketFactory;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Class SocketOutputAdapter
 */
public class SocketOutputAdapter extends AbstractOutputAdapter {
	
    /**
     * Constructor for SocketOutputAdapter
     * 
     * @param appContext
     *            The current application context
     */
	public SocketOutputAdapter(final ApplicationContext appContext) {
		super(appContext);
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

		Socket socket = null;
		OutputStream os = null;
		try {
			final IUplinkConnection cc = appContext.getBean(IConnectionMap.class).getFswUplinkConnection();
			final String host = cc.getHost();
            final int port = cc.getPort();

			socket = new Socket(host, port);
			os = socket.getOutputStream();

			for (final ICltu cltu : plopCltus) {
				os.write(cltu.getPlopBytes());
			}
			os.flush();
		} catch (final Exception e) {
            final String diagMessage = ExceptionTools.getMessage(e);
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

			if (socket != null) {
				try {
					socket.close();
				} catch (final IOException e) {
					// don't care
				}
			}
		}
		response.setFailureReason(null);
		response.setStatus(CommandStatusType.Radiated);
		return response;
	}

	@Override
    public IUplinkResponse transmitScmf(final IScmf scmf) throws UplinkException {
		Long scmfChecksum = null;
		Long totalCltus = null;

		try {
			scmfChecksum = Long.valueOf(scmf.getFileChecksum());
			totalCltus = Long.valueOf(scmf.getCltuCount());
		} catch (final Exception e) {
		}

		/*
		 * MPCS-7417 - 9/4/2015: Initial status should be
		 * Send_Failure, not Radiated, because if an exception is thrown during
		 * the below try block, the send is abandoned and the status will remain
		 * in this initial state. So it's best to initialize with a fail. Right
		 * before this method's return statement (if we get there, it means
		 * everything went fine), the status is set to Radiated.
		 */
		final IUplinkResponse response = new GenericUplinkResponse("",
				CommandStatusType.Send_Failure, null, "", scmfChecksum, totalCltus, new AccurateDateTime());
		/*
		 * MPCS-7417 - 9/4/2015: Removed extra, redundant call of
		 * setFailureReason.
		 */
		/** MPCS-6472 - 8/20/2014: Setting failure reason by default.  Not setting this can cause
		 * issues with MTAK output because it expect a failure reason.
		 */
		response.setFailureReason(UplinkFailureReason.AMPCS_SEND_FAILURE);
		
		final IUplinkConnection cc = appContext.getBean(IConnectionMap.class).getFswUplinkConnection();
		final String host = cc.getHost();
		final int port = cc.getPort();

		Socket socket = null;
		OutputStream os = null;
		try {
			if (scmf == null) {
				throw new IllegalArgumentException("Null input SCMF");
			}

			final List<IScmfCommandMessage> messages = scmf.getCommandMessages();
			socket = new Socket(host, port);
			os = socket.getOutputStream();

			for (final IScmfCommandMessage msg : messages) {
				os.write(msg.getData());
			}
			os.flush();
		} catch (final Exception e) {
			final String diagMessage = e.getMessage() == null ? e.toString() : e.getMessage();
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

			if (socket != null) {
				try {
					socket.close();
				} catch (final IOException e) {
					// don't care
				}
			}
		}

		response.setFailureReason(null);
		response.setStatus(CommandStatusType.Radiated);
		return response;
	}

	@Override
	protected IUplinkResponse transmitRawUplinkData(final File transmitFile,
			final boolean isHex, final IScmf scmf) throws UplinkException {
		Long scmfChecksum = null;
		Long totalCltus = null;

		try {
			scmfChecksum = Long.valueOf(scmf.getFileChecksum());
			totalCltus = Long.valueOf(scmf.getCltuCount());
		} catch (final Exception e) {
		}

		final IUplinkResponse response = new GenericUplinkResponse("",
				CommandStatusType.Send_Failure, null, "", scmfChecksum, totalCltus, new AccurateDateTime());
		/** MPCS-6472 - 8/20/2014: Setting failure reason by default.  Not setting this can cause
		 * issues with MTAK output because it expect a failure reason.
		 */
		response.setFailureReason(UplinkFailureReason.AMPCS_SEND_FAILURE);

		if (transmitFile == null) {
			throw new IllegalArgumentException("Input file was null");
		} else if (transmitFile.exists() == false) {
			throw new IllegalArgumentException("The input file "
					+ transmitFile.getName() + " does not exist.");
		}

		final IUplinkConnection cc = appContext.getBean(IConnectionMap.class).getFswUplinkConnection();
		final String host = cc.getHost();
		final int port = cc.getPort();

		Socket socket = null;
		try {
			socket = new Socket(host, port);
			FileUtility.writeFileToOutputStream(socket.getOutputStream(),
					transmitFile, isHex);
		} catch (final Exception e) {
			final String diagMessage = e.getMessage() == null ? e.toString() : e.getMessage();
			response.setDiagnosticMessage(diagMessage);
			response.setSuccessful(false);

			throw new UplinkException(diagMessage, e, response);
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (final IOException e) {
					// don't care
				}
			}
		}

		response.setFailureReason(null);
		response.setStatus(CommandStatusType.Radiated);
		return response;
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
			ITewUtility tewUtility = appContext.getBean(ITewUtility.class);
			if (missionConfig.isUsingTcPackets()) {
				// TODO: Do we need to pass in sequence counters for the packet? Where do we hold the count
				final ITelecommandPacketFactory pktFactory = appContext.getBean(ITelecommandPacketFactory.class);
				final ITelecommandPacket        pkt        = pktFactory.buildPacketFromPdu(pdu, apid);
				final ITcTransferFrame frame = tewUtility.wrapBytesToFrame(pkt.getBytes(), vcid, getPduFrameSequenceCounter());
				frames.add(frame);
			} else {
				final ITcTransferFrame frame = tewUtility.wrapBytesToFrame(pdu, vcid, getPduFrameSequenceCounter());
				frames.add(frame);
			}
		} catch (FrameWrapUnwrapException e) {
			throw new UplinkException(e);
		}

		final List<ICltu> cltus = new ArrayList<>();

        IUplinkResponse response = null;
        try {
			cltus.addAll(cltuBuilder.createPlopCltusFromFrames(frames));

			trace.debug("Successfully created ", cltus.size(), " cltu's");
			trace.debug("Transmitting CLTU length=", cltus.get(0).getHexDisplayString());

			response = transmitCltus(cltus);
        } catch (final CltuEndecException e) {
			response = new GenericUplinkResponse();
			response.setDiagnosticMessage(ExceptionTools.getMessage(e));
			response.setFailureReason(UplinkFailureReason.AMPCS_SEND_FAILURE);
			response.setStatus(CommandStatusType.Failed);
			response.setSuccessful(false);
			trace.error("Error creating SCMF" + ExceptionTools.getMessage(e));
		} catch (final UplinkException e) {
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

    /**
     * Initial implementation for CLTU transmission without SCMF's. 
     * Not ready to expose this as interface method yet
     * @return IUplinkResponse
     */
    private IUplinkResponse transmitCltus(final List<ICltu> cltus) throws UplinkException{
        final IUplinkResponse response = new GenericUplinkResponse("", CommandStatusType.Send_Failure, null, "", null,
                                                                   Long.valueOf(cltus.size()), new AccurateDateTime());
        response.setFailureReason(UplinkFailureReason.AMPCS_SEND_FAILURE);

        Socket socket = null;
        OutputStream os = null;
        try {
            final IUplinkConnection cc = appContext.getBean(IConnectionMap.class).getFswUplinkConnection();
            final String host = cc.getHost();
            final int port = cc.getPort();

            socket = new Socket(host, port);
            os = socket.getOutputStream();

            for (final ICltu cltu : cltus) {
                os.write(cltu.getPlopBytes());
            }
            os.flush();
        }
        catch (final Exception e) {
            final String diagMessage = ExceptionTools.getMessage(e);
            response.setDiagnosticMessage(diagMessage);
	        response.setSuccessful(false);

            throw new UplinkException(diagMessage, e, response);
        }
        finally {
            if (os != null) {
                try {
                    os.close();
                }
                catch (final IOException e) {
                    // don't care
                }
            }

            if (socket != null) {
                try {
                    socket.close();
                }
                catch (final IOException e) {
                    // don't care
                }
            }
        }
        response.setFailureReason(null);
        response.setStatus(CommandStatusType.Radiated);
        return response;
    }
}
