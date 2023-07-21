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
import jpl.gds.db.api.adaptation.IMySqlAdaptationProperties;
import jpl.gds.db.api.sql.store.StoreIdentifier;
import jpl.gds.db.api.sql.store.ldi.IHeaderChannelValueLDIStore;
import jpl.gds.db.api.types.ParentTypeEnum;
import jpl.gds.dictionary.api.channel.ChannelDefinitionType;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.eha.api.channel.ChannelCategoryEnum;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.message.IAlarmedChannelValueMessage;
import jpl.gds.shared.database.BytesBuilder;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.types.Pair;

/**
 * See IChannelValueLDIStore for description. This is for HeaderChannelValue.
 *
 */
public class HeaderChannelValueLDIStore extends ChannelValueLDIStore implements IHeaderChannelValueLDIStore {
    
    private final boolean archiveFrames;
    
    /**
     * Creates an instance of HeaderChannelValueLDIStore.
     *
     * @param contextConfig
     *            The information about the current test session
     */
    public HeaderChannelValueLDIStore(final ApplicationContext appContext) {
        /*
         * MPCS-7135 - Add second argument to indicate this store
         * can operate asynchronously.
         */
        super(appContext, IHeaderChannelValueLDIStore.STORE_IDENTIFIER);
        
        archiveFrames = archiveController.getUseArchive(StoreIdentifier.Frame);
    }

    /**
     * Format frame id portion of HeaderChannelValue for LDI.
     *
     * @param bb
     *            BytesBuilder to populate
     * @param val
     *            The channel value to insert
     *
     * @throws DatabaseException
     *             Throws exception on error
     */
    protected void formatChannelValueFrameId(final IMySqlAdaptationProperties dbProperties, final BytesBuilder bb,
                                             final IServiceChannelValue val)
            throws DatabaseException {
        final Long frameId = val.getFrameId();

        /** MPCS-6809 As unsigned */
        /** MPCS-7681  Avoid dead link to Frame */
        if ((frameId != null) && archiveFrames) {
            bb.insertLongAsUnsigned(frameId);
        }
        else {
            bb.insertNULL();
        }

        bb.insertSeparator();
    }

    /**
     * Remove all entries for which this store is responsible
     */
    @Override
    protected void clearChannelIds() {
        archiveController.clearFswIds(headerChannelPrefix);
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
         * Filter all channels that are not HEADER channels both Definition and
         * Category. If either indicates that they are HEADER channels, then
         * they will be processed here.
         */
        final IServiceChannelValue cv = (IServiceChannelValue) message.getChannelValue();
        if (ChannelDefinitionType.H != cv.getChannelDefinition().getDefinitionType()) {
            return;
        }

        writeChannelValueToLDIFile(cv);
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.api.sql.store.ldi.IHeaderChannelValueLDIStore#
     * insertHeaderChannelValue(jpl.gds.eha.api.channel.
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
        final boolean channelFromSse = ((cd.getDefinitionType() == ChannelDefinitionType.SSE) || (ChannelCategoryEnum.SSEPACKET_HEADER == cce));
        if (channelFromSse != applicationIsSse) {
            trace.warn(Markers.DB, "Channel \"" + ci + "\" is not consistent with application context: channelFromSse="
                    + channelFromSse + ", applicationIsSse="
                    + applicationIsSse);
            /*
             * FALL THROUGH -- DO NOT RETURN HERE: HeaderChannelValues can be either FSW or SSE!
             * JUST ISSUE THE WARNING AND CONTINUE PROCESSING.
             */
        }

        final Pair<Long, Boolean> idPair = archiveController.getAssociatedId(ci, channelFromSse);
        final long id = idPair.getOne();
        final boolean doData = idPair.getTwo();

        try {
            synchronized (this) {
                if (!archiveController.isUp()) {
                    throw new IllegalStateException("This connection has already been closed");
                }

                formatChannelValueCommon(appContext, contextConfig, bb, val, DB_HEADER_CHANNEL_VALUE_TABLE_NAME, id);
                switch (cce) {
                    case FRAME_HEADER:
                        formatChannelValueFrameId(dbProperties, bb, val);

                        /** MPCS-5153 */
                        bb.insertTextComplainReplace(ParentTypeEnum.FRAME.toString());
                        break;

                    case PACKET_HEADER:
                        formatChannelValuePacketId(appContext, dbProperties, bb, val, false);
                        /** MPCS-5153 */
                        bb.insertTextComplainReplace(ParentTypeEnum.PACKET.toString());
                        break;

                    case SSEPACKET_HEADER:
                        /** MPCS-5008  */
                        formatChannelValuePacketId(appContext, dbProperties, bb, val, false);
                        bb.insertTextComplainReplace(ParentTypeEnum.SSEPACKET.toString());
                        break;

                    default:
                        // Should not happen, checked earlier
                        trace.error(this.getClass().getSimpleName() + ": Input channel  " + cd + ", " + cce
                                + " is incorrect for messagePublicationBus");
                }

                bb.insertSeparator();

                /** MPCS-5008 */
                bb.insert(channelFromSse ? 1 : 0);
                bb.insertSeparator();

                formatChannelValueErt(appContext, bb, val, DB_HEADER_CHANNEL_VALUE_TABLE_NAME, "ert");
                formatChannelValueDssId(appContext, bb, val, DB_HEADER_CHANNEL_VALUE_TABLE_NAME,
                        sessionDss == null ? StationIdHolder.MIN_VALUE : sessionDss, false);
                formatChannelValueVcid(appContext, bb, val, DB_HEADER_CHANNEL_VALUE_TABLE_NAME, sessionVcid, true);
                if (doData) {
                    /** MPCS-5008*/
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
            throw new DatabaseException("Error inserting HeaderChannelValue record into database", e);
        }
    }
}
