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
package jpl.gds.db.mysql.impl.sql.store.ldi;

import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.IDbTableNames;
import jpl.gds.db.api.sql.store.ldi.ISsePacketLDIStore;
import jpl.gds.shared.holders.HeaderHolder;
import jpl.gds.shared.holders.PacketIdHolder;
import jpl.gds.shared.holders.TrailerHolder;
import jpl.gds.tm.service.api.packet.ITelemetryPacketInfo;
import jpl.gds.tm.service.api.packet.ITelemetryPacketMessage;


/**
 * This is the database write/storage interface to the SsePacket table in the
 * MPCS database. This class will receive an input packet and write it and
 * its body to the SsePacket table in the database. This is done through
 * LOAD DATA INFILE.
 *
 * This class is very similar to the equivalent non-LDI class. The fields are
 * dumped into a BytesBuilder instead of a PreparedStatement, and written by
 * means of a table-specific call to the superclass.
 *
 * startResource and stopResource also make table-specific calls to enable or
 * disable processing.
 *
 * Id values now obtained from PacketIdHolder.
 *
 */
public class SsePacketLDIStore extends PacketLDIStore implements ISsePacketLDIStore
{
    /**
     * Creates an instance of ISsePacketLDIStore.
     *
     * @param appContext
     *            The test configuration for the current test session
     */
    public SsePacketLDIStore(final ApplicationContext appContext)
    {
    	/* 
    	 * MPCS-7135 - Add second argument to indicate this
    	 * store does not operate asynchronously.
    	 */
        super(appContext, ISsePacketLDIStore.STORE_IDENTIFIER, false);
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.store.ldi.ISsePacketLDIStore#insertSsePacket(jpl.gds.tm.service.api.packet.ITelemetryPacketInfo, byte[], jpl.gds.shared.holders.PacketIdHolder, jpl.gds.shared.holders.HeaderHolder, jpl.gds.shared.holders.TrailerHolder)
     */
    @Override
    public void insertSsePacket(final ITelemetryPacketInfo    pi,
                                final byte[]         packetData,
                                final PacketIdHolder packetId,
                                final HeaderHolder   hdr,
                                final TrailerHolder  tr)
        throws DatabaseException
    {
        if (!dbProperties.getUseDatabase() || isStoreStopped.get())
        {
            return;
        }

        final HeaderHolder  header  = HeaderHolder.getSafeHolder(hdr);
        final TrailerHolder trailer = TrailerHolder.getSafeHolder(tr);

        try
        {
            synchronized (this)
            {
                if (!archiveController.isUp())
                {
                    throw new IllegalStateException(
                                  "This connection has already been closed");
                }

                // Format common portion and terminate
                formatPacketCommon(contextConfig,
                                                  _bb,
                                                  pi,
                                                  packetData,
                                                  IDbTableNames.DB_SSE_PACKET_DATA_TABLE_NAME,
                                                  packetId,
                                                  header,
                                                  trailer,
                                                  true);

                // Do the body
                formatPacketBody(contextConfig,
                                                _bbBody,
                                                packetData,
                                                packetId,
                                                header,
                                                trailer);

                // Add the line to the LDI batch
                writeToStream(_bb, _bbBody);
            }
        }
        catch (final RuntimeException re)
        {
            throw re;
        }
        catch (final Exception e)
        {
            throw new DatabaseException(
                          "Error inserting SsePacket record into database: " +
                          e);
        }
    }

    /**
     * Consume a packet message from the internal message bus. If the packet
     * is not SSE, then throw it away, otherwise insert it and its body into
     * the database
     *
     * @param pm The packet message to consume
     */
    @Override
    protected void handlePacketMessage(final ITelemetryPacketMessage pm)
    {
        if (pm == null)
        {
            throw new IllegalArgumentException("Null input packet message");
        }

        final ITelemetryPacketInfo pi = pm.getPacketInfo();

        if (! pi.isFromSse())
        {
            // Not for us
            return;
        }

        try
        {
            insertSsePacket(pi,
                            pm.getPacket(),
                            pm.getPacketId(),
                            pm.getHeader(),
                            pm.getTrailer());
        }
        catch (final DatabaseException de)
        {
            trace.error("LDI SsePacket store failed: ", de);
        }
    }
}
