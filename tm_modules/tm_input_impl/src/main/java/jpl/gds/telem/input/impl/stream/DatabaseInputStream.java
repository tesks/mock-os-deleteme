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
package jpl.gds.telem.input.impl.stream;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.types.DatabaseConnectionKey;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.fetch.IEndSessionFetch;
import jpl.gds.db.api.sql.fetch.IFrameFetch;
import jpl.gds.db.api.sql.fetch.IFrameQueryOptionsFactory;
import jpl.gds.db.api.sql.fetch.IFrameQueryOptionsProvider;
import jpl.gds.db.api.sql.fetch.IPacketFetch;
import jpl.gds.db.api.sql.order.IFrameOrderByType;
import jpl.gds.db.api.sql.order.IOrderByTypeFactory;
import jpl.gds.db.api.sql.order.IPacketOrderByType;
import jpl.gds.db.api.sql.order.OrderByType;
import jpl.gds.db.api.types.IDbEndSessionProvider;
import jpl.gds.db.api.types.IDbRawData;
import jpl.gds.db.api.types.IDbSessionInfoFactory;
import jpl.gds.db.api.types.IDbSessionInfoProvider;
import jpl.gds.db.api.types.IDbSessionInfoUpdater;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.holders.HolderException;
import jpl.gds.shared.holders.PacketIdHolder;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.telem.input.api.config.RawDataFormat;
import jpl.gds.telem.input.api.stream.IRawInputStream;


/**
 * This class is an <code>IRawInputStream</code> that can be processed by a
 * <code>IRawStreamProcessor</code> It is a stream of data queried from the MPCS
 * database
 * 
 *
 */
public class DatabaseInputStream implements IRawInputStream,
        Iterable<IDbRawData>
{
    private static final long SHORT_SLEEP     = 1L * 1000L; // 1 second
    private static final long LONG_SLEEP      = 6L * 1000L; // 6 seconds
    private static final int  AFTER_END_COUNT = 10;

	private static final int  BATCH_SIZE       = 100;

	private static final Tracer logger = TraceManager.getDefaultTracer();

	private static final String ME = "DatabaseInputStream ";
	private final DatabaseConnectionKey connectionKey;
	private final IDbSessionInfoProvider dsi;
	private List<? extends IDbRawData> buffer;
	private IFrameFetch ff;
	private IPacketFetch pf;
	private IEndSessionFetch esf;

	private Long           nextFrameId  = null;
	private PacketIdHolder nextPacketId = null;

    private boolean remote = false;

    /** True if we have seen an EndSession */
    private boolean haveEnd = false;

    /** Set true when stopping */
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    private final boolean                    isSse;

    private final boolean isFrame;

    private final IFrameQueryOptionsFactory  frameQueryOptionsFactory;

    private final IFrameQueryOptionsProvider qo;
    private final IOrderByTypeFactory        orderByTypeFactory;
    
    /**
     * Constructor
     * 
     * @param appContext
     *            the Spring Application Context
     * @param bus
     *            the message publication bus to use
     * @param ff
     *            the <code>FrameFetch</code> object that will be used to fetch
     *            frames
     * @param esf
     *            the <code>EndSessionFetch</code> object that will be used to
     *            determine the end of a session in remote mode
     * @param key
     *            the <code>DatabaseConnectionKey</code> object containing
     *            information regarding the session to be reprocessed
     * @param format
     *            the raw data format of the data to be reprocessed
     * 
     * MPCS-7689 - 9/24/15. Change to take DatabaseConnectionKey
     *          argument rather than DatabaseSessionInfo argument
     */
    public DatabaseInputStream(final ApplicationContext appContext, final IMessagePublicationBus bus,
            final IFrameFetch ff, final IEndSessionFetch esf,
            final DatabaseConnectionKey key, final RawDataFormat format) {
        this(appContext, bus, key, format);
        this.ff = ff;
        this.esf = esf;
    }

    /**
     * Constructor
     * 
     * @param appContext
     *            the Spring Application Context
     * @param bus
     *            the message publication bus to use
     * @param pf
     *            the <code>PacketFetch</code> object that will be used to fetch
     *            packets
     * @param esf
     *            the <code>EndSessionFetch</code> object that will be used to
     *            determine the end of a session in remote mode
     * @param key
     *            the <code>DatabaseConnectionKey</code> object containing
     *            information regarding the session to be reprocessed
     * @param format
     *            the raw data format of the data to be reprocessed
     * 
     * MPCS-7689 - 9/24/15. Change to take DatabaseConnectionKey
     *          argument rather than DatabaseSessionInfo argument
     */
    public DatabaseInputStream(final ApplicationContext appContext, final IMessagePublicationBus bus,
            final IPacketFetch pf, final IEndSessionFetch esf,
            final DatabaseConnectionKey key, final RawDataFormat format) {
        this(appContext, bus, key, format);
        this.pf = pf;
        this.esf = esf;
    }

    private DatabaseInputStream(final ApplicationContext appContext, final IMessagePublicationBus bus,
            final DatabaseConnectionKey key, final RawDataFormat format) {
    	this.connectionKey = key;
        this.frameQueryOptionsFactory = appContext.getBean(IFrameQueryOptionsFactory.class);
        this.orderByTypeFactory = appContext.getBean(IOrderByTypeFactory.class);
        this.qo = frameQueryOptionsFactory.createFrameQuereyOptions();
        this.isSse = appContext.getBean(SseContextFlag.class).isApplicationSse();
        logger.setAppContext(appContext);

        /* MPCS-7689 - 9/24/15. Populate DatabaseSessionInfo from
         * DatabaseConnectionKey,
         */
        final IDbSessionInfoFactory dbSessionInfoFactory = appContext.getBean(IDbSessionInfoFactory.class);
        this.dsi = dbSessionInfoFactory.createQueryableProvider();
        final IDbSessionInfoUpdater dsiUpdater = dbSessionInfoFactory.convertProviderToUpdater(dsi);
        dsiUpdater.setHostPatternList(connectionKey.getHostPatternList());
        dsiUpdater.setSessionKeyList(connectionKey.getSessionKeyList());

        this.isFrame = (format != null) &&
                format.equals(RawDataFormat.TRANSFER_FRAME);
    }

	

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.telem.input.api.stream.IRawInputStream#getDataInputStream()
	 */
	@Override
	public DataInputStream getDataInputStream() {
		return null;
	}


    /**
     * Get next batch, handling delays. We do not exit unless we
     * have some data or we are terminating.
     *
     * @param initial If true, start a new query
     *
     * next*Id will be null if we have never queried any rows.
     */
    @SuppressWarnings("unchecked")
	private void queryNextBatch(final boolean initial)
    {
        boolean newQuery      = initial;
        int     afterEndCount = 0;

        try
        {
            while (true)
            {
                if (stopped.get())
                {
                    buffer = null;
                    break;
                }

                if (newQuery && isFrame)
                {
                    frameQueryOptionsFactory.convertProviderToUpdater(qo).setFrameId(nextFrameId);

                    /* MPCS-7339 - 3//30/16.  Changed the following frame fetch to NOT
                     * reattach ASMs to frames.
                     */
                    buffer = (List<? extends IDbRawData>) ff.get(dsi, null, BATCH_SIZE, qo, false,
                                                                 orderByTypeFactory.getOrderByType(OrderByType.FRAME_ORDER_BY,
                                                                                                   IFrameOrderByType.ID_TYPE),
                                                                 null, true);
                }
                else if (isFrame)
                {
                    buffer = (List<? extends IDbRawData>) ff.getNextResultBatch();
                }
                else if (newQuery)
                {
                    buffer = (List<? extends IDbRawData>) pf.get(dsi, null, BATCH_SIZE, null, null, null,
                                                                 isSse, nextPacketId,
                                                                 orderByTypeFactory.getOrderByType(OrderByType.PACKET_ORDER_BY,
                                                                                                   IPacketOrderByType.ID_TYPE),
                                    true);
                }
                else
                {
                    buffer = (List<? extends IDbRawData>) pf.getNextResultBatch();
                }

                if (! buffer.isEmpty())
                {
                    // Process the data
                    break;
                }

                // No data

                if (! remote || stopped.get())
                {
                    buffer = null;
                    break;
                }

                // We're remote with no data.
                // We must start over with a new query.

                newQuery = true;

                // We're either still waiting for the first buffer
                // or we are waiting for data after a pause.
                // Sleep and try again, unless we see the end session.

                if (! haveEnd)
                {
                    // See if it's there now

                    haveEnd = readEndSession();

                    if (! haveEnd)
                    {
                        // Still no end session, so sleep and try again

                        SleepUtilities.checkedSleep(SHORT_SLEEP);
                        continue;
                    }
                }

                // Have end session. But have we really seen all the data?

                if (afterEndCount >= AFTER_END_COUNT)
                {
                    // Waited long enough, assume done
                    buffer = null;
                    break;
                }

                ++afterEndCount;

                SleepUtilities.checkedSleep(LONG_SLEEP);
            }
        }
        catch (final DatabaseException e)
        {
            logger.error("Unable to query next batch from database", e);

            buffer = null;
        }
    }


	/**
	 * Look for the endSession entry.
	 * @throws DatabaseException 
	 */
	@SuppressWarnings("unchecked")
	private boolean readEndSession() throws DatabaseException
    {
        if (! remote)
        {
            return false;
        }

        final List<? extends IDbEndSessionProvider> ldes = (List<? extends IDbEndSessionProvider>) esf.get(dsi, null,
                                                                                                           2);

		if (ldes.isEmpty()) {
			return false;
		}

		if (ldes.size() > 1) {
			logger.warn(ME + "Multiple endsession messages found");
		}

		/* 
		 * R8 Refactor - Cannot tell what this affects or causes to happen.
		 * Do we need a local end of session message for the remote session? 
		 * At this time, we do not want the dependency on the session module here,
		 * so commenting this out.
		 */
//        final IDbEndSessionProvider endSession = ldes.get(0);
//
//		// Update test session with end time by sending end message
//
//		final Date endTime = endSession.getEndTime();
//
//		final ContextIdentification tc = new SessionIdentification();
//
//		final List<String> hosts = dsi.getHostPatternList();
//		final List<Long> sessions = dsi.getSessionKeyList();
//		final String host = hosts.get(0);
//		final long session = sessions.get(0);
//
//		tc.setHost(host);
//		tc.setNumber(session);
//		tc.setStartTime(DUMMY_START_TIME);
//		tc.setEndTime(endTime);
//		tc.setName("REMOTE_" + host + "_" + session);
//
//		final EndOfSessionMessage etm =
//          new ProtectedEndOfSessionMessage(tc);
//
//		bus.publish(etm);
//
//      final DateFormat format = TimeUtility.getFormatterFromPool();
//
//		logger.debug(ME                               +
//                     "Sent end-of-test-message for '" +
//                     host                             +
//                     "' session "                     +
//                     session                          +
//                     " end-time "                     +
//                     format.format(endTime));
//
//        TimeUtility.releaseFormatterToPool(format);

		esf.abortQuery();

		return true;
	}


	/**
	 * {@inheritDoc}
	 * 
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<IDbRawData> iterator() {
		return new DatabaseInputStreamIterator();
	}


	/**
	 * This is an <code>Iterator</code> class that will iterate through the
	 * queried database data
	 * 
	 */
	private class DatabaseInputStreamIterator implements
	        Iterator<IDbRawData> {
		private int pointer;

		/**
		 * Constructor
		 */
		public DatabaseInputStreamIterator() {
			this.pointer = 0;
		}


		/**
		 * {@inheritDoc}
		 * 
		 * @see java.util.Iterator#hasNext()
		 */
		@Override
		public boolean hasNext()
        {
            if (stopped.get())
            {
                closeConnections();
                return false;
            }

			// Initially, we have no data
			if (buffer == null)
            {
				queryNextBatch(true);

				if (stopped.get()    ||
                    (buffer == null) ||
                    buffer.isEmpty())
                {
                    closeConnections();
					return false;
				}

                this.pointer = 0;
                return true;
            }

            // We reached the end of our buffer, get more rows
            if (pointer == buffer.size())
            {
				queryNextBatch(false);

				// If the next batch is empty, there are no more rows
				if (stopped.get()    ||
                    (buffer == null) ||
                    buffer.isEmpty())
                {
                    closeConnections();
					return false;
				}

                this.pointer = 0;
                return true;
            }

            if (pointer < buffer.size())
            {
				return true;
			}

            // Should never happen
            closeConnections();
            return false;
		}


		/**
		 * {@inheritDoc}
		 * 
		 * @see java.util.Iterator#next()
		 */
		@Override
		public IDbRawData next()
        {
			if (hasNext())
            {
                final IDbRawData drd =
                    DatabaseInputStream.this.buffer.get(pointer);

                if (isFrame)
                {
                    nextFrameId = drd.getFrameId() + 1L;
                }
                else
                {
                    try
                    {
                        nextPacketId = drd.getPacketId().incrementValue();
                    }
                    catch (final HolderException he)
                    {
                        // Highly unlikely; turn to a runtime exception

                        ExceptionTools.addCauseAndThrow(
                            new NoSuchElementException(), he);
                    }
                }

				pointer++;
				return drd;
			}

            throw new NoSuchElementException();
		}


		/**
		 * {@inheritDoc}
		 * 
		 * @see java.util.Iterator#remove()
		 */
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
    /* 
     * R8 Refactor - Cannot tell what this affects or causes to happen.
     * Do we need a local end of session message for the remote session? 
     * At this time, we do not want the dependency on the session module here,
     * so commenting this out.
     */

//	/**
//	 * Class to prevent MPCS from overriding the special test configuration.
//	 */
//	private static class ProtectedEndOfSessionMessage extends EndOfSessionMessage {
//		/**
//		 * Constructor.
//		 * 
//		 * @param id the context identification for this end message
//		 */
//		public ProtectedEndOfSessionMessage(final ContextIdentification id) {
//			super(id);
//		}
//
//		/**
//		 * Only want the context id to be set once in the constructor.
//		 * Prevent it from being set here
//		 * 
//		 * @param id the context identification for this end message
//		 */
//		@Override
//		public void setSessionId(final ContextIdentification id) {
//			return;
//		}
//
//		/**
//		 * Flag that we represent a remote session.
//		 * 
//		 * @return True
//		 */
//		@Override
//		public boolean isRemote() {
//			return true;
//		}
//	}


	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.telem.input.api.stream.IRawInputStream#getData()
	 */
	@Override
	public byte[] getData()
    {
		return null;
	}


    /**
     * Close all connections.
     */
    private void closeConnections()
    {
        if (ff != null)
        {
            try
            {
                ff.abortQuery();
            }
            finally
            {
                ff.close();
            }
        }

        if (pf != null)
        {
            try
            {
                pf.abortQuery();
            }
            finally
            {
                pf.close();
            }
        }

        if (esf != null)
        {
            try
            {
                esf.abortQuery();
            }
            finally
            {
                esf.close();
            }
        }
    }


    /**
     * {@inheritDoc}
     *
     * We cannot close the connections at this point because they may be in
     * use. Instead, we tell the processing to stop, and the connections are
     * closed when no longer needed.
     * 
     * @see jpl.gds.telem.input.api.stream.IRawInputStream#close()
     */
    @Override
    public void close() throws IOException
    {
        stopped.set(true);
    }


    /**
     * Tell stream to go to remote mode.
     */
    public void setRemote()
    {
        remote = true;
    }

    // MPCS-7832  01/12/16 - Added function due to change in IRawInputStream
	/**
	 * Clear buffer if present, otherwise an error is thrown. Currently this
	 * InputStream does not utilize a buffer.
	 * 
	 * @throws UnsupportedOperationException
	 *             The buffer does not exist
	 */
	@Override
	public void clearInputStreamBuffer() throws UnsupportedOperationException {
		throw new UnsupportedOperationException("DatabaseInputStream does not utilize a buffer");
	}
}
