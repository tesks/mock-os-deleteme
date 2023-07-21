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
package jpl.gds.telem.input.impl.connection;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.connection.IDownlinkConnection;
import jpl.gds.common.config.connection.IFileConnectionSupport;
import jpl.gds.telem.input.api.RawInputException;
import jpl.gds.telem.input.api.stream.IRawInputStream;
import jpl.gds.telem.input.impl.stream.RawInputStream;

/**
 * This class reads telemetry data from a file for processing.
 * 
 */
public class FileInputConnection extends AbstractRawInputConnection {
	private DataInputStream dis;
	private final String filename;

	/**
     * Constructor.
     * 
     * @param serveContext the current application context
     */
	public FileInputConnection(ApplicationContext serveContext) {
	    super(serveContext);
	    final IDownlinkConnection dc = serveContext.getBean(IConnectionMap.class).getDownlinkConnection();
		this.filename =  ((IFileConnectionSupport)dc).getFile();	    
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.connection.IRawInputConnection#openConnection()
	 */
	@Override
	public boolean openConnection() throws RawInputException {

		if (dis == null) {

			logger.debug("Filename: " + filename);

			final File f = new File(filename);
			FileInputStream fis;
			try {
				fis = new FileInputStream(f);
			} catch (final FileNotFoundException e) {
				throw new RawInputException("Could not find input file.", e);
			}

			dis = new DataInputStream(fis);
		}

		return dis != null;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see jpl.gds.telem.input.api.connection.IRawInputConnection#getConnectionString()
	 */
	@Override
	public String getConnectionString() {
		if (dis != null) {
			connectionString = this.rawInputType
			        + ":" + filename;

			return connectionString;
		}

		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.impl.connection.AbstractRawInputConnection#reconnect()
	 */
	@Override
	public void reconnect() throws RawInputException {
		super.reconnect();

		// it does not make sense to "reconnect" to a file. reconnecting to a
		// file would start from the beginning, so it would be misleading to
		// implement this method for file-based connections
		this.dis = null;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.connection.IRawInputConnection#getRawInputStream()
	 */
	@Override
	public IRawInputStream getRawInputStream() {
		if (this.dis == null) {
			return null;
		}
		// return the current data input stream and nullify it to signal that
		// there are not additional data input streams
		final DataInputStream tempDis = this.dis;
		this.dis = null;

		return new RawInputStream(tempDis);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.connection.IRawInputConnection#loadData(java.util.concurrent.atomic.AtomicBoolean)
	 */
	@Override
	public boolean loadData(final AtomicBoolean handlerStopping)
        throws IOException
    {
        // MPCS-5131 08/01/13 Add atomic
		// the initial (and only) DataInputStream is loaded in openConnection
		if (this.dis != null) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.connection.IRawInputConnection#isConnected()
	 */
	@Override
	public boolean isConnected() {
		return dis != null;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.connection.IRawInputConnection#isDataStream()
	 */
	@Override
	public boolean isDataStream() {
		// files are finite data streams
		return false;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.impl.connection.AbstractRawInputConnection#closeConnection()
	 */
	@Override
	public void closeConnection() throws IOException {
		super.closeConnection();
		
		if(dis != null) {
			dis.close();
		}
	}
}
