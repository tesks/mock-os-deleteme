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
package jpl.gds.db.api.sql.store;

import jpl.gds.db.api.sql.store.ldi.IChannelValueLDIStore;
import jpl.gds.db.api.sql.store.ldi.ICommandMessageLDIStore;
import jpl.gds.db.api.sql.store.ldi.IEvrLDIStore;
import jpl.gds.db.api.sql.store.ldi.IFrameLDIStore;
import jpl.gds.db.api.sql.store.ldi.IHeaderChannelValueLDIStore;
import jpl.gds.db.api.sql.store.ldi.ILogMessageLDIStore;
import jpl.gds.db.api.sql.store.ldi.IMonitorChannelValueLDIStore;
import jpl.gds.db.api.sql.store.ldi.IPacketLDIStore;
import jpl.gds.db.api.sql.store.ldi.IProductLDIStore;
import jpl.gds.db.api.sql.store.ldi.ISseChannelValueLDIStore;
import jpl.gds.db.api.sql.store.ldi.ISseEvrLDIStore;
import jpl.gds.db.api.sql.store.ldi.ISsePacketLDIStore;
import jpl.gds.db.api.sql.store.ldi.aggregate.IChannelAggregateLDIStore;
import jpl.gds.db.api.sql.store.ldi.aggregate.IHeaderChannelAggregateLDIStore;
import jpl.gds.db.api.sql.store.ldi.aggregate.IMonitorChannelAggregateLDIStore;
import jpl.gds.db.api.sql.store.ldi.aggregate.ISseChannelAggregateLDIStore;
import jpl.gds.db.api.sql.store.ldi.cfdp.ICfdpFileGenerationLDIStore;
import jpl.gds.db.api.sql.store.ldi.cfdp.ICfdpFileUplinkFinishedLDIStore;
import jpl.gds.db.api.sql.store.ldi.cfdp.ICfdpIndicationLDIStore;
import jpl.gds.db.api.sql.store.ldi.cfdp.ICfdpPduReceivedLDIStore;
import jpl.gds.db.api.sql.store.ldi.cfdp.ICfdpPduSentLDIStore;
import jpl.gds.db.api.sql.store.ldi.cfdp.ICfdpRequestReceivedLDIStore;
import jpl.gds.db.api.sql.store.ldi.cfdp.ICfdpRequestResultLDIStore;

public interface IDbSqlStoreFactory {

    /**
     * @return the sessionStore
     */
    public ISessionStore getSessionStore();

    /**
     * @return the endSessionStore
     */
    public IEndSessionStore getEndSessionStore() ;

    /**
     * @return the hostStore
     */
    public IHostStore getHostStore();

    /**
     * @return the channelValueStore
     */
    public IChannelValueLDIStore getChannelValueStore();
    
    /**
     * @return the commandMessageStore
     */
    public ICommandMessageLDIStore getCommandMessageStore();

    /**
     * @return the evrStore
     */
    public IEvrLDIStore getEvrStore();

    /**
     * @return the frameStore
     */
    public IFrameLDIStore getFrameStore();

    /**
     * @return the headerChannelValueStore
     */
    public IHeaderChannelValueLDIStore getHeaderChannelValueStore();

    /**
     * @return the logMessageStore
     */
    public ILogMessageLDIStore getLogMessageStore();

    /**
     * @return the monitorChannelValueStore
     */
    public IMonitorChannelValueLDIStore getMonitorChannelValueStore();

    /**
     * @return the packetStore
     */
    public IPacketLDIStore getPacketStore();

    /**
     * @return the productStore
     */
    public IProductLDIStore getProductStore();

    /**
     * @return the sseChannelValueStore
     */
    public ISseChannelValueLDIStore getSseChannelValueStore();

    /**
     * @return the sseEvrStore
     */
    public ISseEvrLDIStore getSseEvrStore();

    /**
     * @return the ssePacketStore
     */
    public ISsePacketLDIStore getSsePacketStore();

    /**
     * @return
     */
    public ICommandUpdateStore getCommandUpdateStore();
    
    public ICfdpIndicationLDIStore getCfdpIndicationStore();

    public ICfdpFileGenerationLDIStore getCfdpFileGenerationStore();

    public ICfdpFileUplinkFinishedLDIStore getCfdpFileUplinkFinishedStore();

    public ICfdpRequestReceivedLDIStore getCfdpRequestReceivedStore();

    public ICfdpRequestResultLDIStore getCfdpRequestResultStore();

    public ICfdpPduReceivedLDIStore getCfdpPduReceivedStore();

    public ICfdpPduSentLDIStore getCfdpPduSentStore();

    /**
     * @return the context config store
     */
    public IContextConfigStore getContextConfigStore();
    
    /**
     * @return the channel aggregate store
     */
    public IChannelAggregateLDIStore getChannelAggregateStore();

    /**
     * @return the header channel aggregate store
     */
    public IHeaderChannelAggregateLDIStore getHeaderChannelAggregateStore();
    
    /**
     * @return the sse channel aggregate store
     */
    public ISseChannelAggregateLDIStore getSseChannelAggregateStore();
    
    /**
     * @return the monitor channel aggregate store
     */
    public IMonitorChannelAggregateLDIStore getMonitorChannelAggregateStore();
    

}
