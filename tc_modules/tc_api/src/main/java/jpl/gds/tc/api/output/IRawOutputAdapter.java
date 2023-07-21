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
package jpl.gds.tc.api.output;

import java.io.File;
import java.io.IOException;
import java.util.List;

import jpl.gds.tc.api.ICommandFileLoad;
import jpl.gds.tc.api.IScmf;
import jpl.gds.tc.api.command.ICommand;
import jpl.gds.tc.api.command.ISseCommand;
import jpl.gds.tc.api.exception.RawOutputException;
import jpl.gds.tc.api.exception.UplinkException;

/**
 * This interface that defines the methods required for output (uplink) adapters
 * 
 * @since AMPCS R3
 */
public interface IRawOutputAdapter {
	/**
	 * Initialize the output adapter
	 * 
	 * @throws RawOutputException if initialization fails
	 */
	public void init() throws RawOutputException;

	/**
	 * Send an SSE command
	 * 
	 * @param command the SSE command to send
	 * @param id an ID to associate with this uplink
	 * @throws IOException if the uplink fails
	 */
	public void sendSseCommand(final ISseCommand command, int id) throws IOException;

	/**
	 * Send an SCMF
	 * 
	 * @param scmf the SCMF to send
	 * @param id an ID to associate with this uplink
	 * @throws UplinkException if the uplink fails
	 */
	public void sendScmf(final IScmf scmf, int id) throws UplinkException;

	/**
	 * Send raw data
	 * 
	 * @param fileToSend the file containing the raw data
	 * @param isHex whether or not the raw data is in hex
	 * @param isFaultInjected whether or not this raw data came from fault
	 *            injector
	 * @param id an ID to associate with this uplink
	 * @throws UplinkException if the uplink fails
	 */
	public void sendRawUplinkData(final File fileToSend, final boolean isHex,
			boolean isFaultInjected, int id) throws UplinkException;

	/**
	 * Send a FSW file load
	 * 
	 * @param fileLoads the file load(s)
	 * @param id an ID to associate with this uplink
	 * @throws UplinkException if the uplink fails
	 */
	public void sendFileLoads(final List<ICommandFileLoad> fileLoads, int id)
			throws UplinkException;

	/**
	 * Send a command
	 * @param commands the command(s) to send
	 * @param id an ID associated with this uplink
	 * @throws UplinkException if the uplink fails
	 */
	public void sendCommands(final List<ICommand> commands, int id)
			throws UplinkException;

    /**
     * Send a PDU
     * 
     * @param pdu
     *            pdu data to send
     * @param vcid
     *            vcid to send to
     * @param scid
     *            scid to send to
     * @param apid
     *            apid to use in command packet
     * @throws UplinkException
     *             if the uplink fails
     * 
     */
    public void sendPdus(final byte[] pdu, int vcid, int scid, int apid) throws UplinkException;
}
