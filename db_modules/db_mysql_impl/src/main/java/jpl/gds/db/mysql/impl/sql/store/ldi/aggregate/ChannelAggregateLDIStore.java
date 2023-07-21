package jpl.gds.db.mysql.impl.sql.store.ldi.aggregate;

import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.store.ldi.aggregate.IChannelAggregateLDIStore;
import jpl.gds.eha.api.message.IEhaGroupedChannelValueMessage;
import jpl.gds.eha.api.message.aggregation.AggregateMessageType;

/**
 * This is the database write/storage interface to the ChannelAggregate table in
 * the AMPCS database. This class will receive an input channel aggregate and write
 * it to the ChannelAggregate table in the database. This is done via LDI.
 *
 * startResource and stopResource also make table-specific calls to enable or
 * disable processing.
 *
 * Note that there may be duplicate channel ids but we insert a given channel id but once.
 *
 * The ids are done by channel type in order to avoid problems like FSW versus SSE caused by
 * running multiple inserting processes with the same master key. So long as the processes
 * insert distinct channel types, the ids will not clash.
 *
 */
public class ChannelAggregateLDIStore extends AbstractAggregateLDIStore implements IChannelAggregateLDIStore {

	/**
     * Creates an instance of ChannelAggregateLDIStore.
     *
     * @param appContext Spring Application Context
     */
    public ChannelAggregateLDIStore(final ApplicationContext appContext) {
        super(appContext, IChannelAggregateLDIStore.STORE_IDENTIFIER, AggregateMessageType.AggregateAlarmedEhaChannel);
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

        buildIdListsAndStoreChannalData(iegcvm, true, false);

        synchronized (this) {
          
            insertCommonBeginFields(contextConfig.getContextId());

            insertIdAndChannelType(iegcvm, true);
            
            insertPacketIds(packetIds);

            insertRct(iegcvm, IChannelAggregateLDIStore.STORE_IDENTIFIER);            
            insertErt(iegcvm, IChannelAggregateLDIStore.STORE_IDENTIFIER);
            insertSclk(iegcvm);
            insertScet(iegcvm, IChannelAggregateLDIStore.STORE_IDENTIFIER);
            
            insertDssId(iegcvm);            

            insertVcid(iegcvm);

            insertCommonEndFields(iegcvm, chanIdString, distinctCnt);
        }
    }
}
