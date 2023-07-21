/*
 * Copyright 2006-2021. California Institute of Technology.
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

package jpl.gds.db.mysql.impl.sql.store.ldi.aggregate;

import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.store.ldi.aggregate.IHeaderChannelAggregateLDIStore;
import jpl.gds.eha.api.channel.ChannelCategoryEnum;
import jpl.gds.eha.api.channel.aggregation.serialization.Proto3EhaGroupMember;
import jpl.gds.eha.api.message.IEhaGroupedChannelValueMessage;
import jpl.gds.eha.api.message.aggregation.AggregateMessageType;
import jpl.gds.shared.database.ChannelTypeEnum;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Header channel aggregate LDI Store
 */
public class HeaderChannelAggregateLDIStore extends AbstractAggregateLDIStore implements IHeaderChannelAggregateLDIStore
{
    /**
     * Creates an instance of ChannelAggregateLDIStore.
     *
     * @param appContext Spring Application Context
     */
    public HeaderChannelAggregateLDIStore(final ApplicationContext appContext) {
        super(appContext, 
        	IHeaderChannelAggregateLDIStore.STORE_IDENTIFIER,
        	AggregateMessageType.AggregateHeaderChannel);
         
    }

    /**
     * Insert a channel aggregate into the database.
     *
     * @param iegcvm Channel aggregate message to insert
     *
     * @throws DatabaseException Throws exception on error
     */
    @Override
    public void insertChannelAggregate(final IEhaGroupedChannelValueMessage iegcvm)
        throws DatabaseException {

        //MPCS-12061 - Don't insert this if LOST_HEADER
        if(iegcvm.getChannelCategory() == ChannelCategoryEnum.LOST_HEADER){
            throw new DatabaseException("Unsupported channel category: " + iegcvm.getChannelCategory() +
                    " for channel IDs " + getChannelIdSetAsString(iegcvm));
        }
        
        final ChannelTypeEnum channelType = extendedChannelEnum(iegcvm.getChannelCategory(),
                iegcvm.isRealtime());
        
        final boolean fromSse = (channelType == ChannelTypeEnum.SSE_HEADER);
        
        buildIdListsAndStoreChannalData(iegcvm, false, fromSse);

        synchronized (this) {

            insertCommonBeginFields(contextConfig.getContextId());

            insertIdAndChannelType(iegcvm, true);
            
            insertRct(iegcvm, IHeaderChannelAggregateLDIStore.STORE_IDENTIFIER);            
            insertErt(iegcvm, IHeaderChannelAggregateLDIStore.STORE_IDENTIFIER);
            
            insertApid(iegcvm);
            insertDssId(iegcvm);
            insertVcid(iegcvm);

            insertCommonEndFields(iegcvm, chanIdString, distinctCnt);
        }
    }

    /**
     * Get a set of channel IDs, filtering out duplicates
     * @param iegcvm IEhaGroupedChannelValueMessage object
     * @return Set of unique channel IDs as string
     */
    private String getChannelIdSetAsString(final IEhaGroupedChannelValueMessage iegcvm){
        final List<Proto3EhaGroupMember> list = iegcvm.getEhaAggregatedGroup().getValuesList();
        final Set<String> channelIds = new HashSet<>();
        for(Proto3EhaGroupMember member : list){
            channelIds.add(member.getChannelId());
        }
       return channelIds.toString();
    }
}

