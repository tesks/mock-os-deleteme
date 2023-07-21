package jpl.gds.db.mysql.impl.sql.store.ldi.aggregate;

import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.store.ldi.aggregate.ISseChannelAggregateLDIStore;
import jpl.gds.eha.api.message.IEhaGroupedChannelValueMessage;
import jpl.gds.eha.api.message.aggregation.AggregateMessageType;

public class SseChannelAggregateLDIStore extends AbstractAggregateLDIStore implements ISseChannelAggregateLDIStore
{
    /**
     * Creates an instance of ChannelAggregateLDIStore.
     *
     * @param appContext Spring Application Context
     */
    public SseChannelAggregateLDIStore(final ApplicationContext appContext) {
        super(appContext, 
        	ISseChannelAggregateLDIStore.STORE_IDENTIFIER, 
        	AggregateMessageType.AggregateSseChannel);
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
        
        buildIdListsAndStoreChannalData(iegcvm, true, true);
        
        synchronized (this) {

            insertCommonBeginFields(contextConfig.getContextId());

            insertIdAndChannelType(iegcvm, false);
            insertPacketIds(packetIds);
            
            insertRct(iegcvm, ISseChannelAggregateLDIStore.STORE_IDENTIFIER);            
            insertErt(iegcvm, ISseChannelAggregateLDIStore.STORE_IDENTIFIER);
            insertSclk(iegcvm);
            insertScet(iegcvm, ISseChannelAggregateLDIStore.STORE_IDENTIFIER);

            insertCommonEndFields(iegcvm, chanIdString, distinctCnt);
        }
    }
}
