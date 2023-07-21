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
import jpl.gds.db.api.sql.store.ldi.IMonitorChannelValueLDIStore;
import jpl.gds.dictionary.api.channel.ChannelDefinitionType;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.eha.api.channel.ChannelCategoryEnum;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.message.IAlarmedChannelValueMessage;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.types.Pair;

/**
 * See IChannelValueLDIStore for description. This is for MonitorChannelValue.
 *
 */
public class MonitorChannelValueLDIStore extends ChannelValueLDIStore implements IMonitorChannelValueLDIStore {
    /**
     * Creates an instance of MonitorChannelValueLDIStore.
     *
     * @param contextConfig
     *            The information about the current test session
     */
    public MonitorChannelValueLDIStore(final ApplicationContext appContext) {
        /*
         * MPCS-7135 - Add second argument to indicate this store
         * can operate asynchronously.
         */
        super(appContext, IMonitorChannelValueLDIStore.STORE_IDENTIFIER);
    }
    
    /**
     * Remove all entries for which this store is responsible

     */
    @Override
    protected void clearChannelIds() {
        archiveController.clearFswIds(monitorChannelPrefix);
    }

    /**
     * Receive an EHA Channel message from the internal message bus, pull out
     * the pertinent information, and insert the information into the database
     *
     * @param message
     *            The EHA channel message received on the internal bus
     */
    @Override
    protected void handleEhaChannelMessage(final IAlarmedChannelValueMessage message) {
        /*
         * Filter all channels that are SSE channels (By Message)
         */
        if (message.isFromSse()) {
            return;
        }

        /*
         * Filter all channels that are not MONITOR channels both Definition and
         * Category. If either indicates that they are MONITOR channels, then
         * they will be processed here.
         */
        final IServiceChannelValue cv = (IServiceChannelValue) message.getChannelValue();
        if (ChannelDefinitionType.M != cv.getChannelDefinition().getDefinitionType()) {
            return;
        }
        writeChannelValueToLDIFile(cv);
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.api.sql.store.ldi.IMonitorChannelValueLDIStore#
     * insertMonitorChannelValue(jpl.gds.eha.api.channel.
     * IInternalChannelValue)
     */
    @Override
    public void insertChannelValue(final IServiceChannelValue val) throws DatabaseException {
        if (val == null) {
            throw new IllegalArgumentException("Null input channel value");
        }

        if (!dbProperties.getUseDatabase() || isStoreStopped.get()) {
            return;
        }

        if (val.getChanId() == null) {
            throw new IllegalArgumentException("Input channel value has a null channel id");
        }
        final String ci = val.getChanId().toUpperCase();

        final ChannelCategoryEnum cce = val.getChannelCategory();
        if (cce == null) {
            throw new IllegalArgumentException("Null channel category");
        }

        final IChannelDefinition cd = val.getChannelDefinition();
        if (cd == null) {
            throw new IllegalArgumentException("Input channel value has a null channel definition");
        }

        final ChannelType ct = val.getChannelType();
        if (ct == null) {
            throw new IllegalArgumentException("Input channel value has a null channel type in its channel definition");
        }

        final boolean applicationIsSse = archiveController.isApplicationIsSse();
        final boolean channelFromSse = (cd.getDefinitionType() == ChannelDefinitionType.SSE);
        if (channelFromSse != applicationIsSse) {
            trace.warn(Markers.DB, "Channel \"" , ci , "\" is not consistent with application context: channelFromSse="
                    , channelFromSse , ", applicationIsSse="
                    , applicationIsSse);
            return;
        }

        final Pair<Long, Boolean> idPair = archiveController.getAssociatedId(ci, channelFromSse);
        final long id = idPair.getOne();
        final boolean doData = idPair.getTwo();

        try {
            synchronized (this) {
                if (!archiveController.isUp()) {
                    throw new IllegalStateException("This connection has already been closed");
                }

                formatChannelValueCommon(appContext, contextConfig, bb, val, DB_MONITOR_CHANNEL_VALUE_TABLE_NAME, id);
                formatChannelValueErt(appContext, bb, val, DB_MONITOR_CHANNEL_VALUE_TABLE_NAME, "mst");
                formatChannelValueDssId(appContext, bb, val, DB_MONITOR_CHANNEL_VALUE_TABLE_NAME,
                        sessionDss == null ? StationIdHolder.MIN_VALUE : sessionDss, true);

                if (doData) {
                    prepareChannelData(appContext, contextConfig, ci, channelFromSse, // SSE
                            ct, cd, id, bbcd);
                    // Add the lines to the LDI batch
                    writeToStream(bb, bbcd);
                }
                else {
                    // Add the line to the LDI batch
                    writeToStream(bb);
                }
            }
        }
        catch (final RuntimeException re) {
            throw re;
        }
        catch (final Exception e) {
            throw new DatabaseException("Error inserting MonitorChannelValue record into database", e);
        }
    }
}
