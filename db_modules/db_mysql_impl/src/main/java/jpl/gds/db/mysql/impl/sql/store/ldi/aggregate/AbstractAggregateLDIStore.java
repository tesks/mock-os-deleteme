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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.Deflater;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.store.StoreIdentifier;
import jpl.gds.db.impl.aggregate.AggregateUtils;
import jpl.gds.db.mysql.impl.sql.store.ldi.AbstractLDIStore;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.eha.api.channel.ChannelCategoryEnum;
import jpl.gds.eha.api.channel.aggregation.serialization.Proto3EhaAggregatedGroup;
import jpl.gds.eha.api.channel.aggregation.serialization.Proto3EhaGroupMember;
import jpl.gds.eha.api.message.IEhaGroupedChannelValueMessage;
import jpl.gds.eha.api.message.aggregation.AggregateMessageType;
import jpl.gds.shared.database.BytesBuilder;
import jpl.gds.shared.database.ChannelTypeEnum;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.message.BaseMessageHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.TimeTooLargeException;
import jpl.gds.shared.types.Pair;

/**
 * Abstract parent class for aggregate LDI stores
 */
public abstract class AbstractAggregateLDIStore extends AbstractLDIStore {
	
    protected static final boolean COMPRESS = true;
    
    /** String column lengths */
    protected static final int     CID_LENGTH    = 9;
    protected static final int     MODULE_LENGTH = 32;
    protected static final int     NAME_LENGTH   = 64;
    protected static final int     FORMAT_LENGTH = 16;
    
    protected final Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
    protected final byte[]   buffer   = new byte[10000];
        
    /**
     * Byte Builders for ChannelLink
     */
    protected final List<BytesBuilder> bbcl = new ArrayList<>();

    /**
     * Ids for this master key, by channel type. Atomic is used for
     * convenience as a holder of an integer with useful methods.
     */
    protected final EnumMap<ChannelTypeEnum, AtomicLong> ids =
        new EnumMap<>(ChannelTypeEnum.class);

    /**
     * Byte Builder for ChannelAggregate
     */
    protected final BytesBuilder bb = new BytesBuilder();
	
    
    protected AggregateMessageType aggregateMessageType;
    
    protected List<String> cids = new ArrayList<>();
    protected String chanIdString = "";
    protected Set<String> packetIds = new TreeSet<>();
    protected int distinctCnt = 0;
    
    /**
     * Supports subclass initialization.
     *
     * @param appContext Spring Application Context
     * @param si         Store Identifier for this store
     * @param aggregateMessageType Aggregate Message type
     */
	protected AbstractAggregateLDIStore(final ApplicationContext appContext, final StoreIdentifier si, final AggregateMessageType aggregateMessageType) {
		// Will use serialization queue
		this(appContext, si, true, aggregateMessageType);
	}
    
	private AbstractAggregateLDIStore(final ApplicationContext appContext, final StoreIdentifier si, final boolean supportsAsync, final AggregateMessageType aggregateMessageType) {
		super(appContext, si, supportsAsync);
		
		this.aggregateMessageType = aggregateMessageType;
        
        // Pre-populate map to save time later
        for (final ChannelTypeEnum cte : ChannelTypeEnum.values()) {
            ids.put(cte, new AtomicLong(1L));
        }
	}

    @Override
    protected void startResource(){
        super.startResource();
       
        handler = new BaseMessageHandler() {
            @Override
            public synchronized void handleMessage(final IMessage m)
            {
                handleEhaGroupMessage((IEhaGroupedChannelValueMessage) m);
            }
        };
        
        // Subscribe to channel messages on the internal bus
        if (messagePublicationBus != null) {
            messagePublicationBus.subscribe(aggregateMessageType, handler);
        }
    }

    @Override
    protected void stopResource() {
    	
    	super.stopResource();
    	
        // Unsubscribe from channel messages on the internal bus
        if (messagePublicationBus != null) {
            messagePublicationBus.unsubscribe(aggregateMessageType, handler);
        }
    }
    
    /**
     * Receive a grouped EHA Channel message from the internal message bus, pull out
     * the pertinent information, and insert the information into the database.
     *
     * @param message EHA aggregate message received on the internal bus
     */
	protected void handleEhaGroupMessage(final IEhaGroupedChannelValueMessage message) {		
        if (message == null) {
            throw new IllegalArgumentException("Null input Channel Aggregate");
        }
    
        if (!dbProperties.getUseDatabase() || isStoreStopped.get()) {
            return;
        }
    	
    	writeChannelAggregateToLDIFile(message);
    }
    
    
    /**
     * @param message
     *            the Channel Aggregate to write
     */
    protected void writeChannelAggregateToLDIFile(final IEhaGroupedChannelValueMessage message) {

        if (message == null) {
            throw new IllegalArgumentException("Null input channel aggregate");
        }

        if (! dbProperties.getUseDatabase() || isStoreStopped.get()) {
            return;
        }
        
        try {
            if (this.doAsyncSerialization) {
                queueForSerialization(message);
            }
            else {
                serializeToLDIFile(message);
            }
        }
        catch (final Exception e) {
            trace.error(Markers.DB, "LDI ChannelAggregate storage failed for aggregate ",
            		ExceptionTools.getMessage(e));
        }
    }    

    @Override
    protected boolean serializeToLDIFile(final Object toInsert) {
		final IEhaGroupedChannelValueMessage aggregateObj = (IEhaGroupedChannelValueMessage) toInsert;

        try {
            bb.clear();
            insertChannelAggregate(aggregateObj);
            trace.trace("Writing aggregated ", bb, " bytes to stream.. ", bb.getBytes().length);
            writeToStream(bb, bbcl.toArray(new BytesBuilder[bbcl.size()]), aggregateObj.getValuesCount());
            return true;
        }
        catch (final DatabaseException de) {
        	trace.error(Markers.DB, this.getClass().getSimpleName(), " failed for aggregate ", aggregateObj,
        			ExceptionTools.getMessage(de), de);
            return false;
        }
    }

    abstract void insertChannelAggregate(final IEhaGroupedChannelValueMessage iegcvm) throws DatabaseException;
    
    /**
     * Convert from Java category to database category.
     *
     * @param cce      Java category
     * @param realTime True if real-time
     *
     * @return Database enum
     */
    protected static ChannelTypeEnum extendedChannelEnum(final ChannelCategoryEnum cce, final boolean realTime)
        throws DatabaseException
    {
        if (cce == null)
        {
            throw new DatabaseException("Null channel enum");
        }

        if (! realTime && (cce != ChannelCategoryEnum.FSW))
        {
            throw new DatabaseException("Bad channel enum real-time state");
        }

        switch (cce)
        {
            case FSW:
                return (realTime ? ChannelTypeEnum.FSW_RT : ChannelTypeEnum.FSW_REC);

            case SSE:
                return ChannelTypeEnum.SSE;

            case MONITOR:
                return ChannelTypeEnum.MONITOR;

            case FRAME_HEADER:
                return ChannelTypeEnum.FRAME_HEADER;

            case PACKET_HEADER:
                return ChannelTypeEnum.PACKET_HEADER;

            case SSEPACKET_HEADER:
                return ChannelTypeEnum.SSE_HEADER;

            default:
                //MPCS-12061 - Prevent chill_down hang if this is thrown
                throw new DatabaseException("Bad channel enum: " + cce);
        }
    }
    
    /**
     * Prepare a ChannelData for insertion into the database.
     * 
     * @param appContext
     *            the Spring Application Context
     * @param contextConfig
     *            Session configuration
     * @param channelId
     *            Channel id
     * @param member
     *        Proto3EhaGroupMember
     * @param fromSse
     *            From SSE (true) or FSW (false)
     * @param id
     *            Index to match with *ChannelValue.
     *
     * @throws DatabaseException
     *             SQL error
     * @return BytesBuilder object
     */
    protected BytesBuilder prepareChannelData(
    		final ApplicationContext    appContext,
            final IContextConfiguration contextConfig,
            final String                channelId,
            final boolean               fromSse,
            final Proto3EhaGroupMember  member,
            final long                  id)
                    throws DatabaseException
    {
    	final BytesBuilder bb = new BytesBuilder();
        final ChannelType chanType = ChannelType.valueOf(member.getDn().getType().name().substring("DN_TYPE_".length()));

        try {
            //bb.clear();

            bb.insert(contextConfig.getContextId().getNumber());
            bb.insertSeparator();

            bb.insert(contextConfig.getContextId().getHostId());
            bb.insertSeparator();

            bb.insert(contextConfig.getContextId().getFragment());
            bb.insertSeparator();

            bb.insert(id);
            bb.insertSeparator();

            /** MPCS-5153 */
            bb.insertTextComplainReplace(checkLength("ChannelData.channelId", CID_LENGTH, channelId.toUpperCase()));

            bb.insertSeparator();

            bb.insert(fromSse ? 1 : 0);
            bb.insertSeparator();

            /** MPCS-5153 */
            bb.insertTextComplainReplace(chanType.toString());
            bb.insertSeparator();

            final int index = member.getChannelIndex();

            if (index > 0) {
                bb.insert(index);
            }
            else {
                bb.insertNULL();
            }

            bb.insertSeparator();

            final String module = checkLength("ChannelData.module", MODULE_LENGTH,
                    StringUtil.emptyAsNull(member.getModule())); 
            
            if (module != null) {
                /** MPCS-5153 */
                bb.insertTextComplainReplace(module.toUpperCase());
            }
            else {
                bb.insertNULL();
            }

            bb.insertSeparator();

            /** MPCS-6624 Prefer name to title */
            /**
             * MPCS-6624  Reverse previous decision. Prefer title to name
             * but leave the new null check.
             */
            //String name = StringUtil.emptyAsNull(cd.getTitle());
            String name = StringUtil.emptyAsNull(member.getName());
            if (name == null) {
                //name = StringUtil.emptyAsNull(cd.getName());
            	name = StringUtil.emptyAsNull(member.getName());
            }

            /** MPCS-5153  */
            bb.insertTextOrNullComplainReplace(checkLength("ChannelData.name", NAME_LENGTH, name));

            bb.insertSeparator();

            /** MPCS-5153  */
            bb.insertTextOrNullComplainReplace(checkLength("ChannelData.dnFormat", FORMAT_LENGTH, member.getDnFormat()));

            bb.insertSeparator();

            /** MPCS-5153 */
            bb.insertTextOrNullComplainReplace(checkLength("ChannelData.euFormat", FORMAT_LENGTH, member.getEuFormat()));

            bb.insertTerminator();
        }
        catch (final SQLException se) {
            throw new DatabaseException(se);
        }
        
		return bb;
    }
    
    protected void buildIdListsAndStoreChannalData(final IEhaGroupedChannelValueMessage iegcvm, final boolean hasPacketIds, final boolean fromSse) throws DatabaseException {
        cids = new ArrayList<>();
        chanIdString = "";
        packetIds = new TreeSet<>();
        distinctCnt = 0;
        
        final Proto3EhaAggregatedGroup proto3Eag = iegcvm.getEhaAggregatedGroup();
        final List<Proto3EhaGroupMember> members = proto3Eag.getValuesList();
        for (final Proto3EhaGroupMember member : members) {
            final String cidu = member.getChannelId().toUpperCase();
            if (! cids.contains(cidu)) {
                cids.add(cidu);
                chanIdString += cidu + ":";
                distinctCnt++;
            }
            
            if (hasPacketIds) {
                packetIds.add(String.valueOf(member.getPacketId()));
            }
            
            final Pair<Long, Boolean> idPair = archiveController.getAssociatedId(cidu, fromSse);
            final long id = idPair.getOne();
            final boolean doData = idPair.getTwo();

            if (doData) {
                bbcl.add(prepareChannelData(appContext, contextConfig, cidu, fromSse, // SSE
                        member, id));
            }
        }
    }    
    
    protected void insertIdAndChannelType(final IEhaGroupedChannelValueMessage iegcvm, final boolean insertChannelType)
        throws DatabaseException{
        final ChannelTypeEnum channelType = extendedChannelEnum(iegcvm.getChannelCategory(),
                                                                iegcvm.isRealtime());

        bb.insert(ids.get(channelType).getAndIncrement());
        bb.insertSeparator();
        
        if (insertChannelType) {
            bb.insertSafe(channelType.toString());
            bb.insertSeparator();
        }
    }    
    
    protected void insertPacketIds(final Set<String> packetIds) {
        bb.insertSafe(StringUtils.join(packetIds, ':'));
        bb.insertSeparator();
    }    
    
    protected void insertRct(final IEhaGroupedChannelValueMessage iegcvm, final StoreIdentifier storeIdentifier) {
        if (iegcvm.hasRctRange()) {
            final IAccurateDateTime beginRctCoarse = new AccurateDateTime(iegcvm.getRctMinimumRange());
            try
            {
                bb.insertDateAsCoarse(beginRctCoarse);
            }
            catch (final TimeTooLargeException ttle)
            {
                trace.warn(dateExceedsWarning(storeIdentifier + ".rct",
                                             "aggregate",
                                             beginRctCoarse));
            }
            
            final IAccurateDateTime endScetCoarse = new AccurateDateTime(iegcvm.getRctMaximumRange());
            try
            {
                bb.insertDateAsCoarse(endScetCoarse);
            }
            catch (final TimeTooLargeException ttle)
            {
                trace.warn(dateExceedsWarning(storeIdentifier + ".rct",
                                             "aggregate",
                                             endScetCoarse));
            }
        } else {
            bb.insertNULL();
            bb.insertSeparator();

            bb.insertNULL();
            bb.insertSeparator();
        }
    }
    
    protected void insertErt(final IEhaGroupedChannelValueMessage iegcvm, final StoreIdentifier storeIdentifier) {
        final IAccurateDateTime beginErt = new AccurateDateTime(iegcvm.getErtMinimumRange());

        try
        {
            bb.insertErtAsCoarseFineSeparate(beginErt);
        }
        catch (final TimeTooLargeException ttle)
        {
            trace.warn(ertExceedsWarning(storeIdentifier + ".ert",
                                         "aggregate",
                                         beginErt));
        }
        
        final IAccurateDateTime endErt = new AccurateDateTime(iegcvm.getErtMaximumRange());

        try
        {
            bb.insertErtAsCoarseFineSeparate(endErt);
        }
        catch (final TimeTooLargeException ttle)
        {
            trace.warn(ertExceedsWarning(storeIdentifier + ".ert",
                                         "aggregate",
                                         endErt));
        }
    }    
    
    protected void insertScet(final IEhaGroupedChannelValueMessage iegcvm, final StoreIdentifier storeIdentifier) {
        if (iegcvm.hasScetRange()) {
            final IAccurateDateTime beginScetCoarse = new AccurateDateTime(iegcvm.getScetMinimumRange());
            try
            {
                bb.insertDateAsCoarse(beginScetCoarse);
            }
            catch (final TimeTooLargeException ttle)
            {
                trace.warn(dateExceedsWarning(storeIdentifier + ".scet",
                                             "aggregate",
                                             beginScetCoarse));
            }
            
            final IAccurateDateTime endScetCoarse = new AccurateDateTime(iegcvm.getScetMaximumRange());
            try
            {
                bb.insertDateAsCoarse(endScetCoarse);
            }
            catch (final TimeTooLargeException ttle)
            {
                trace.warn(dateExceedsWarning(storeIdentifier + ".scet",
                                             "aggregate",
                                             endScetCoarse));
            }
        } else {
            bb.insertNULL();
            bb.insertSeparator();

            bb.insertNULL();
            bb.insertSeparator();
        }
    }    

    protected void insertSclk(final IEhaGroupedChannelValueMessage iegcvm) {
        
        if (iegcvm.hasSclkRange())
        {
            bb.insert(iegcvm.getSclkMinimumRange());
            bb.insertSeparator();
            
            // Allow for non-zero fine time
            bb.insert(iegcvm.getSclkMaximumRange() + 1L);
            bb.insertSeparator();
        }
        else
        {
            bb.insertNULL();
            bb.insertSeparator();

            bb.insertNULL();
            bb.insertSeparator();
        }
    }    
    
    protected void insertApid(final IEhaGroupedChannelValueMessage iegcvm) {
        final Integer apid = iegcvm.getApid();

        if (apid != null)
        {
            bb.insert(Integer.toUnsignedLong(apid));
        }
        else
        {
            bb.insertNULL();
        }

        bb.insertSeparator();
    }    
    
    protected void insertVcid(final IEhaGroupedChannelValueMessage iegcvm) {
        final Integer vcid = iegcvm.getVcid();

        if (vcid != null)
        {
            bb.insert(Integer.toUnsignedLong(vcid));
        }
        else
        {
            bb.insertNULL();
        }

        bb.insertSeparator();
    }    
    
    protected void insertDssId(final IEhaGroupedChannelValueMessage iegcvm) {
        final Integer dssId = iegcvm.getDssId();

        if (dssId != null)
        {
            bb.insert(Integer.toUnsignedLong(dssId));
        }
        else
        {
            bb.insertNULL();
        }

        bb.insertSeparator();
    }    
    
    protected void insertCommonBeginFields(final IContextIdentification contextId) {
        bb.insert(contextId.getHostId());
        bb.insertSeparator();

        bb.insert(contextId.getNumber().longValue());
        bb.insertSeparator();

        bb.insert(contextId.getFragment());
        bb.insertSeparator();
    } 
    
    protected void insertCommonEndFields(final IEhaGroupedChannelValueMessage iegcvm, final String chanIdString,
            final int distinctCnt) {
        bb.insert(iegcvm.getValuesCount());
        bb.insertSeparator();
                 
        bb.insert(distinctCnt);
        bb.insertSeparator();
        
        byte[] blob = iegcvm.toBinaryWithoutContextHeaders();
        
        if (COMPRESS)
        {
            final byte[] cblob = AggregateUtils.compress(blob);

            trace.trace("Compressed aggregate blob ",
                        blob.length,
                        ":",
                        cblob.length);

            blob = cblob;
        }

        bb.insertBlob(blob);
        bb.insertSeparator();
        
        bb.insertSafe(StringUtils.chop(chanIdString));
        bb.insertTerminator();
    }
}
