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
package jpl.gds.db.mysql.impl.sql.store;

import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.sql.store.ICommandUpdateStore;
import jpl.gds.db.api.sql.store.IContextConfigStore;
import jpl.gds.db.api.sql.store.IDbSqlStoreFactory;
import jpl.gds.db.api.sql.store.IEndSessionStore;
import jpl.gds.db.api.sql.store.IHostStore;
import jpl.gds.db.api.sql.store.ISessionStore;
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
import jpl.gds.db.mysql.impl.sql.store.ldi.ChannelValueLDIStore;
import jpl.gds.db.mysql.impl.sql.store.ldi.CommandMessageLDIStore;
import jpl.gds.db.mysql.impl.sql.store.ldi.EvrLDIStore;
import jpl.gds.db.mysql.impl.sql.store.ldi.FrameLDIStore;
import jpl.gds.db.mysql.impl.sql.store.ldi.HeaderChannelValueLDIStore;
import jpl.gds.db.mysql.impl.sql.store.ldi.LogMessageLDIStore;
import jpl.gds.db.mysql.impl.sql.store.ldi.MonitorChannelValueLDIStore;
import jpl.gds.db.mysql.impl.sql.store.ldi.PacketLDIStore;
import jpl.gds.db.mysql.impl.sql.store.ldi.ReferenceProductLDIStore;
import jpl.gds.db.mysql.impl.sql.store.ldi.SseChannelValueLDIStore;
import jpl.gds.db.mysql.impl.sql.store.ldi.SseEvrLDIStore;
import jpl.gds.db.mysql.impl.sql.store.ldi.SsePacketLDIStore;
import jpl.gds.db.mysql.impl.sql.store.ldi.aggregate.ChannelAggregateLDIStore;
import jpl.gds.db.mysql.impl.sql.store.ldi.aggregate.HeaderChannelAggregateLDIStore;
import jpl.gds.db.mysql.impl.sql.store.ldi.aggregate.MonitorChannelAggregateLDIStore;
import jpl.gds.db.mysql.impl.sql.store.ldi.aggregate.SseChannelAggregateLDIStore;
import jpl.gds.db.mysql.impl.sql.store.ldi.cfdp.CfdpFileGenerationLDIStore;
import jpl.gds.db.mysql.impl.sql.store.ldi.cfdp.CfdpFileUplinkFinishedLDIStore;
import jpl.gds.db.mysql.impl.sql.store.ldi.cfdp.CfdpIndicationLDIStore;
import jpl.gds.db.mysql.impl.sql.store.ldi.cfdp.CfdpPduReceivedLDIStore;
import jpl.gds.db.mysql.impl.sql.store.ldi.cfdp.CfdpPduSentLDIStore;
import jpl.gds.db.mysql.impl.sql.store.ldi.cfdp.CfdpRequestReceivedLDIStore;
import jpl.gds.db.mysql.impl.sql.store.ldi.cfdp.CfdpRequestResultLDIStore;


public class MultimissionStoreFactory implements IDbSqlStoreFactory {
	/** The Spring Application Context */
	protected final ApplicationContext appContext;

	/**
	 * @param appContext
	 *            the Spring Application Context
	 */
	public MultimissionStoreFactory(final ApplicationContext appContext) {
		super();
		this.appContext = appContext;
	}

	/**
	 * @return the sessionStore
	 */
	@Override
	public ISessionStore getSessionStore() {
		return new SessionStore(appContext);
	}

	/**
	 * @return the endSessionStore
	 */
	@Override
	public IEndSessionStore getEndSessionStore() {
		return new EndSessionStore(appContext);
	}

	/**
	 * @return the hostStore
	 */
	@Override
	public IHostStore getHostStore() {
		return new HostStore(appContext);
	}

	/**
	 * @return the channelValueStore
	 */
	@Override
	public IChannelValueLDIStore getChannelValueStore() {
		return new ChannelValueLDIStore(appContext);
	}

	/**
	 * @return the commandMessageStore
	 */
	@Override
	public ICommandMessageLDIStore getCommandMessageStore() {
		return new CommandMessageLDIStore(appContext);
	}

	/**
	 * @return the evrStore
	 */
	@Override
	public IEvrLDIStore getEvrStore() {
		return new EvrLDIStore(appContext);
	}

	/**
	 * @return the frameStore
	 */
	@Override
	public IFrameLDIStore getFrameStore() {
		return new FrameLDIStore(appContext);
	}

	/**
	 * @return the headerChannelValueStore
	 */
	@Override
	public IHeaderChannelValueLDIStore getHeaderChannelValueStore() {
		return new HeaderChannelValueLDIStore(appContext);
	}

	/**
	 * @return the logMessageStore
	 */
	@Override
	public ILogMessageLDIStore getLogMessageStore() {
		return new LogMessageLDIStore(appContext);
	}

	/**
	 * @return the monitorChannelValueStore
	 */
	@Override
	public IMonitorChannelValueLDIStore getMonitorChannelValueStore() {
		return new MonitorChannelValueLDIStore(appContext);
	}

	/**
	 * @return the packetStore
	 */
	@Override
	public IPacketLDIStore getPacketStore() {
		return new PacketLDIStore(appContext);
	}

	/**
	 * @return the productStore
	 */
	@Override
	public IProductLDIStore getProductStore() {
		return new ReferenceProductLDIStore(appContext);
	}

	/**
	 * @return the sseChannelValueStore
	 */
	@Override
	public ISseChannelValueLDIStore getSseChannelValueStore() {
		return new SseChannelValueLDIStore(appContext);
	}

	/**
	 * @return the sseEvrStore
	 */
	@Override
	public ISseEvrLDIStore getSseEvrStore() {
		return new SseEvrLDIStore(appContext);
	}

	/**
	 * @return the ssePacketStore
	 */
	@Override
	public ISsePacketLDIStore getSsePacketStore() {
		return new SsePacketLDIStore(appContext);
	}

	/**
	 * @return the CommandUpdateStore
	 */
	@Override
	public ICommandUpdateStore getCommandUpdateStore() {
		return new CommandUpdateStore(appContext);
	}

	@Override
	public ICfdpIndicationLDIStore getCfdpIndicationStore() {
		return new CfdpIndicationLDIStore(appContext);
	}

	@Override
	public ICfdpFileGenerationLDIStore getCfdpFileGenerationStore() {
		return new CfdpFileGenerationLDIStore(appContext);
	}

	@Override
	public ICfdpFileUplinkFinishedLDIStore getCfdpFileUplinkFinishedStore() {
		return new CfdpFileUplinkFinishedLDIStore(appContext);
	}

	@Override
	public ICfdpRequestReceivedLDIStore getCfdpRequestReceivedStore() {
		return new CfdpRequestReceivedLDIStore(appContext);
	}

	@Override
	public ICfdpRequestResultLDIStore getCfdpRequestResultStore() {
		return new CfdpRequestResultLDIStore(appContext);
	}

	@Override
	public ICfdpPduReceivedLDIStore getCfdpPduReceivedStore() {
		return new CfdpPduReceivedLDIStore(appContext);
	}

	@Override
	public ICfdpPduSentLDIStore getCfdpPduSentStore() {
		return new CfdpPduSentLDIStore(appContext);
	}

	@Override
	public IContextConfigStore getContextConfigStore() {
		return new ContextConfigStore(appContext);
	}

    /**
     * @return the channel aggregate store
     */
    @Override
    public IChannelAggregateLDIStore getChannelAggregateStore() {
        return new ChannelAggregateLDIStore(appContext);
    }

    /**
     * @return the header channel aggregate store
     */
	@Override
	public IHeaderChannelAggregateLDIStore getHeaderChannelAggregateStore() {
		return new HeaderChannelAggregateLDIStore(appContext);
	}
	
    /**
     * @return the sse channel aggregate store
     */
	@Override
	public ISseChannelAggregateLDIStore getSseChannelAggregateStore() {
		return new SseChannelAggregateLDIStore(appContext);
	}
	
    /**
     * @return the monitor channel aggregate store
     */
	@Override
	public IMonitorChannelAggregateLDIStore getMonitorChannelAggregateStore() {
		return new MonitorChannelAggregateLDIStore(appContext);
	}
}
