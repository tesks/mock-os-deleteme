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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.connection.IDatabaseConnectionSupport;
import jpl.gds.common.config.connection.IDownlinkConnection;
import jpl.gds.common.config.types.DatabaseConnectionKey;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.api.sql.fetch.IEndSessionFetch;
import jpl.gds.db.api.sql.fetch.IFrameFetch;
import jpl.gds.db.api.sql.fetch.IPacketFetch;
import jpl.gds.telem.input.api.RawInputException;
import jpl.gds.telem.input.api.config.RawDataFormat;
import jpl.gds.telem.input.api.connection.IRemoteConnectionSupport;
import jpl.gds.telem.input.api.stream.IRawInputStream;
import jpl.gds.telem.input.impl.stream.DatabaseInputStream;

/**
 * This class is responsible for establishing a connection to the database to
 * fetch data for re-processing.
 * 
 *
 */
public class DatabaseInputConnection extends AbstractRawInputConnection implements IRemoteConnectionSupport {
	private static final String ME = "DatabaseInputConnection ";

	private DatabaseConnectionKey dsi;
	private String host;
	private long session;
	private IFrameFetch frameFetch;
	private IPacketFetch packetFetch;
	private IEndSessionFetch endSessionFetch;
	private DatabaseInputStream dis;
	private RawDataFormat rdf;
	private boolean remoteMode;
	
	/**
	 * Database fetch instance factory.
	 * 
	 * MPCS-9572 - 4/5/18 - Use fetch factory rather than store controller.
	 */
	protected final IDbSqlFetchFactory fetchFactory;
	
    /**
     * Constructor.
     * 
     * @param serveContext the current application context
     */
	public DatabaseInputConnection(final ApplicationContext serveContext) {
	    super(serveContext);
	    
		/*
		 * Initialize fetch factory from application context.
		 */
		this.fetchFactory = appContext.getBean(IDbSqlFetchFactory.class);

		final IDownlinkConnection dc = serveContext.getBean(IConnectionMap.class).getDownlinkConnection();				
	    this.dsi = ((IDatabaseConnectionSupport)dc).getDatabaseConnectionKey();
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.connection.IRawInputConnection#openConnection()
	 */
	@Override
	public boolean openConnection() throws RawInputException {
		host = "";
		session = 0L;

		if (dsi != null) {
			final List<String> hosts = dsi.getHostPatternList();
			final List<Long> sessions = dsi.getSessionKeyList();

			if ((hosts.size() == 1) && (sessions.size() == 1)) {
				host = hosts.get(0);
				session = sessions.get(0);
			} else {
				final String errMsg = "There must be one and only one database host and session key";
				logger.error(errMsg);
				throw new RawInputException(errMsg);
			}
		} else {
			final String errMsg = "Missing database host and session key";
			logger.error(errMsg);
			throw new RawInputException(errMsg);
		}

		if (host == null) {
			throw new RawInputException(ME + "Null host");
		}

		if (session < 0L) {
			throw new RawInputException(ME + "Negative session");
		}

		logger.debug(ME + "Reading from database, host '" + host + "' session "
		        + session);

		dsi = new DatabaseConnectionKey();

		dsi.addHostPattern(host);
		dsi.addSessionKey(session);

		connectionString = this.rawInputType + ": " + host + "/"
		        + session;

		if (this.remoteMode) {
			endSessionFetch = fetchFactory.getEndSessionFetch(0);
		}

		rdf = RawDataFormat.getRawDataFormat(this.rawInputType);

		if (rdf.equals(RawDataFormat.TRANSFER_FRAME)) {
			frameFetch = fetchFactory.getFrameFetch();
			if (!frameFetch.isConnected()) {
				logger.error(ME + "Unable to connect FrameFetch to database");
				return false;
			}

            dis = new DatabaseInputStream(appContext, bus, frameFetch, endSessionFetch, dsi, rdf);
		} else if (rdf.equals(RawDataFormat.PACKET)) {
			packetFetch = fetchFactory.getPacketFetch();
			if (!packetFetch.isConnected()) {
				logger.error(ME + "Unable to connect PacketFetch to database");
				return false;
			}

            dis = new DatabaseInputStream(appContext, bus, packetFetch, endSessionFetch, dsi, rdf);
		}

		logger.debug(ME + ": connection established");
		return true;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.connection.IRawInputConnection#loadData(java.util.concurrent.atomic.AtomicBoolean)
	 */
	@Override
	public boolean loadData(final AtomicBoolean handlerStopping) throws RawInputException, IOException {
        // MPCS-5131 08/01/13 Add atomic
		// the initial (and only) DatabaseInputStream is loaded in
		// getRawInputStream
		if (this.dis != null) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.connection.IRawInputConnection#getRawInputStream()
	 */
	@Override
	public IRawInputStream getRawInputStream() {
		// return the current database input stream and nullify it to signal
		// that
		// there are not additional input streams
		final DatabaseInputStream tempDis = this.dis;
		this.dis = null;

		return tempDis;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.connection.IRawInputConnection#getConnectionString()
	 */
	@Override
	public String getConnectionString() {
		return this.connectionString;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.impl.connection.AbstractRawInputConnection#closeConnection()
	 */
	@Override
	public void closeConnection() throws IOException {
		super.closeConnection();
		if (this.dis != null) {
			this.dis.close();
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.connection.IRawInputConnection#isConnected()
	 */
	@Override
	public boolean isConnected() {
		if (rdf.equals(RawDataFormat.TRANSFER_FRAME) && frameFetch != null) {
			return frameFetch.isConnected();
		} else if (rdf.equals(RawDataFormat.PACKET) && packetFetch != null) {
			return packetFetch.isConnected();
		} else {
			return false;
		}
	}


	/**
	 * {@inheritDoc}
	 * 
	 * @see
	 * jpl.gds.telem.input.api.connection.IRawInputConnection#isDataStream()
	 */
	@Override
	public boolean isDataStream()
    {
		return false;
	}

    /**
     * {@inheritDoc}
     * @see jpl.gds.telem.input.api.connection.IRemoteConnectionSupport#setRemoteMode(boolean)
     */
    @Override
    public void setRemoteMode(final boolean enable) {
        this.remoteMode = enable;
        
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.telem.input.api.connection.IRemoteConnectionSupport#getRemoteMode()
     */
    @Override
    public boolean getRemoteMode() {
        return this.remoteMode;
    }
}
