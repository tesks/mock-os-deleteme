package jpl.gds.db.mysql.impl.sql.store.ldi.aggregate;

import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.store.ldi.aggregate.IMonitorChannelAggregateLDIStore;
import jpl.gds.eha.api.message.IEhaGroupedChannelValueMessage;
import jpl.gds.eha.api.message.aggregation.AggregateMessageType;


/**
 * This is the database write/storage interface to the ChannelAggregate table in
 * the AMPCS database. This class will receive an input channel aggregate and write
 * it to the ChannelAggregate table in the database. This is done via LDI.
 *
 * ChannelLink is also written, but only the first time any channel id is seen.
 *
 * startResource and stopResource also make table-specific calls to enable or
 * disable processing.
 *
 * Note that there may be duplicate channel ids but we insert a given channel id but once.
 * And there may be identical hash codes even for different channel ids, but we only
 * store the unique ones.
 *
 * The ids are done by channel type in order to avoid problems like FSW versus SSE caused by
 * running multiple inserting processes witb the same master key. So long as the processes
 * insert distinct channel types, the ids will not clash.
 *
 * I am skipping the serialization queue business because it will just increase the latency.
 * That can be added in later if deemed necessary.
 *
 */
public class MonitorChannelAggregateLDIStore extends AbstractAggregateLDIStore implements IMonitorChannelAggregateLDIStore
{
    /**
     * Creates an instance of ChannelAggregateLDIStore.
     *
     * @param appContext Spring Application Context
     */
    public MonitorChannelAggregateLDIStore(final ApplicationContext appContext) {
        super(appContext, 
        	IMonitorChannelAggregateLDIStore.STORE_IDENTIFIER, 
        	AggregateMessageType.AggregateMonitorChannel);
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

        buildIdListsAndStoreChannalData(iegcvm, false, false);

        synchronized (this) {

            insertCommonBeginFields(contextConfig.getContextId());
            
            insertIdAndChannelType(iegcvm, false);
            
            insertRct(iegcvm, IMonitorChannelAggregateLDIStore.STORE_IDENTIFIER);            
            insertErt(iegcvm, IMonitorChannelAggregateLDIStore.STORE_IDENTIFIER);
            insertDssId(iegcvm);
            
            insertCommonEndFields(iegcvm, chanIdString, distinctCnt);
        }
    }
}
