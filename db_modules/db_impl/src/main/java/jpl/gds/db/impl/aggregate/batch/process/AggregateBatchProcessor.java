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
package jpl.gds.db.impl.aggregate.batch.process;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

import org.apache.velocity.VelocityContext;
import org.springframework.context.ApplicationContext;

import com.google.protobuf.InvalidProtocolBufferException;

import jpl.gds.alarm.serialization.AlarmValue.Proto3AlarmValue;
import jpl.gds.alarm.serialization.Proto3AlarmValueSet;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.db.api.sql.fetch.IDbSessionPreFetch;
import jpl.gds.db.api.sql.fetch.aggregate.AggregateFetchException;
import jpl.gds.db.api.sql.fetch.aggregate.AggregateFetchMarkers;
import jpl.gds.db.api.sql.fetch.aggregate.ComparableIndexItem;
import jpl.gds.db.api.sql.fetch.aggregate.IEhaAggregateDbRecord;
import jpl.gds.db.api.sql.fetch.aggregate.IEhaAggregateQueryCoordinator;
import jpl.gds.db.api.sql.fetch.aggregate.PacketInfo;
import jpl.gds.db.api.sql.fetch.aggregate.ProcessedBatchInfo;
import jpl.gds.db.api.sql.fetch.aggregate.RecordBatchContainer;
import jpl.gds.db.api.sql.order.IChannelAggregateOrderByType;
import jpl.gds.db.api.types.IDbChannelSampleUpdater;
import jpl.gds.db.api.types.IDbQueryable;
import jpl.gds.db.impl.aggregate.AggregateUtils;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.eha.api.channel.aggregation.serialization.Proto3EhaAggregatedGroup;
import jpl.gds.eha.api.channel.aggregation.serialization.Proto3EhaGroupDiscriminator;
import jpl.gds.eha.api.channel.aggregation.serialization.Proto3EhaGroupMember;
import jpl.gds.eha.api.channel.aggregation.serialization.Proto3EhaGroupMember.HasAlarmValueSetCase;
import jpl.gds.eha.api.channel.aggregation.serialization.Proto3EhaGroupMember.HasEuCase;
import jpl.gds.eha.api.channel.serialization.Proto3Dn;
import jpl.gds.eha.api.channel.serialization.Proto3Dn.DnCase;
import jpl.gds.serialization.primitives.alarm.Proto3AlarmLevel;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.sys.SystemUtilities;
import jpl.gds.shared.template.NullTool;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.DatabaseTimeType;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.LocalSolarTimeFactory;
import jpl.gds.shared.time.Sclk;
import jpl.gds.shared.time.TimeProperties;

/**
 * This is the EHA Aggregate batch processor which does most of the processing
 * work
 *
 */
public abstract class AggregateBatchProcessor extends BatchProcessor<IEhaAggregateDbRecord> {

    protected int batchCnt;
    protected long totalCsvConversionTime;
    protected long totalTemplateMergeTime;
    protected long totalInvlateTime;
    protected long totalDeserializtionTime;
    protected long totalChanSampleUpdaterTime;
    protected long totalTimeFormatAndRangeCheck;
    protected long totalBatchSortTime;
    protected long totalBatchFileWriteTime;
    protected long totalBatchChanIdFilterTime;

    private Proto3Dn proto3Dn;
    private ChannelType channelType = ChannelType.UNKNOWN;
    private Object val = null;
    private long chanSampleUpdaterStart;
    protected int recordIndex;

    protected ProcessedBatchInfo batchInfo;
    protected List<String> recordList;
    protected List<ComparableIndexItem<String>> compList;

    protected String channelId;
    protected String module;
    protected IAccurateDateTime ert;
    protected IAccurateDateTime rct;
    protected IAccurateDateTime scet;
    protected ILocalSolarTime sol;
    protected Sclk sclk;

    private final boolean isUsingAlarmFilter;
    private final boolean isUsingChannelFilter;
    private final boolean isUsingModuleFilter;
    private final boolean isIncludePacketInfo;
    private String modulePattern;
    private final SprintfFormat formatter;
    private String velocityOutput;
    private final StringWriter writer;
    private String modulePatternRegex;
    private Map<Long, PacketInfo> packetInfoMap;

    private Boolean isRealtime;
    private Boolean isSSE;
    private boolean isMonitor;
    private boolean isHeader;

    protected IEhaAggregateQueryCoordinator ehaAggregateQueryQoordinator;
    private final IDbSessionPreFetch sessionPreFetch;

    private final boolean useLst;

    /**
     * Constructor
     * 
     * @param appContext     the Spring Application Context
     * @param batchContainer the batch container
     */
    public AggregateBatchProcessor(final ApplicationContext appContext,
            final RecordBatchContainer<IEhaAggregateDbRecord> batchContainer) {
        super(appContext, batchContainer);

        this.ehaAggregateQueryQoordinator = appContext.getBean(IEhaAggregateQueryCoordinator.class);

        isUsingAlarmFilter = config.isUsingAlarmFilter();
        isUsingChannelFilter = config.isUsingChannelFilter();
        isUsingModuleFilter = config.isUsingModuleFilter();
        isIncludePacketInfo = config.isIncludePacketInfo();

        if (isUsingModuleFilter) {
            modulePatternRegex = config.getModulePatternRegex();
            modulePattern = config.getModulePattern().trim();
            modulePattern = modulePattern.replaceAll(modulePatternRegex, "");
        }

        sessionPreFetch = config.getSessionPreFetch();
        formatter = new SprintfFormat();
        writer = new StringWriter();
        this.useLst = TimeProperties.getInstance().usesLst();

    }

    @Override
    public void processBatch() {
        totalCsvConversionTime = 0;
        totalTemplateMergeTime = 0;
        totalInvlateTime = 0;
        totalDeserializtionTime = 0;
        totalChanSampleUpdaterTime = 0;
        totalTimeFormatAndRangeCheck = 0;
        totalBatchSortTime = 0;
        totalBatchFileWriteTime = 0;
        totalBatchChanIdFilterTime = 0;

        batchId = batchContainer.getBatchId();
        batchCnt++;
        trace.debug(AggregateFetchMarkers.AGGREGATE_BATCH_PROCESSOR, "BatchProcessor # ", batchProcessorNumber,
                " received Batch: ", batchId);

        try {
            processRecordBatch(batchContainer);
        } catch(IOException e) {
            trace.debug(ExceptionTools.getMessage(e));
        }
        catch (DataFormatException | AggregateFetchException e) {
            trace.warn(ExceptionTools.getMessage(e), ": See application log file for more details");
            trace.error(Markers.SUPPRESS, ExceptionTools.getMessage(e), e);
        }
        trace.debug(AggregateFetchMarkers.AGGREGATE_BATCH_PROCESSOR, "BatchProcessor # ", batchProcessorNumber,
                " finished Batch: ", batchId);

    }

    protected void preAggregateProcess() {
        SystemUtilities.doNothing();
    }

    protected void sortRecordIndexList() {
        final long sortStart = System.nanoTime();
        trace.debug(AggregateFetchMarkers.AGGREGATE_BATCH_PROCESSOR, "BatchProcessor # ", batchProcessorNumber,
                " started sort for: ", batchId);

        Collections.sort(compList, ComparableIndexItem.NATURAL_ORDER);

        trace.debug(AggregateFetchMarkers.AGGREGATE_BATCH_PROCESSOR, "BatchProcessor # ", batchProcessorNumber,
                " finished sort for: ", batchId);
        totalBatchSortTime = System.nanoTime() - sortStart;
        trace.debug(AggregateFetchMarkers.AGGREGATE_BATCH_PROCESSOR, "BatchProcessor # ", batchProcessorNumber,
                " for: ", batchId, " Batch Sort time = ", (totalBatchSortTime / 1000000.0), " msecs");
    }

    protected void perAggregateProcess() {
        String orderByField;

        switch (orderByType) {
        case IChannelAggregateOrderByType.ERT_TYPE:
            orderByField = ert.getFormattedErt(true);
            break;
        case IChannelAggregateOrderByType.RCT_TYPE:
            orderByField = rct.getFormattedErt(true);
            break;
        case IChannelAggregateOrderByType.SCET_TYPE:
            orderByField = scet.getFormattedScet(true);
            break;
        case IChannelAggregateOrderByType.SCLK_TYPE:
            orderByField = sclk.toString();
            break;
        case IChannelAggregateOrderByType.CHANNEL_ID_TYPE:
            orderByField = channelId.toString();
            break;
        case IChannelAggregateOrderByType.MODULE_TYPE:
            orderByField = (module == null ? "" : module);
            break;
        default:
            orderByField = ert.getFormattedErt(true);
            break;
        }

        compList.add(new ComparableIndexItem<String>(batchId, recordIndex, orderByField));
    }

    protected abstract void postAggregateProcess() throws AggregateFetchException, IOException;

    protected final void processRecordBatch(final RecordBatchContainer<IEhaAggregateDbRecord> batchContainer)
            throws InvalidProtocolBufferException, DataFormatException, AggregateFetchException, IOException {

        final List<IEhaAggregateDbRecord> aggregateRecords = batchContainer.getBatchList();

        recordList = new ArrayList<>();
        compList = new LinkedList<>();

        preAggregateProcess();

        for (final IEhaAggregateDbRecord aggregateRecord : aggregateRecords) {

            if (isUsingChannelFilter && !aggregateMatchesFilter(aggregateRecord)) {
                continue;
            }
            /*
             * final IAccurateDateTime rct =
             * DbTimeUtility.dateFromCoarseFine(aggregateRecord.getRctCoarse(),
             * aggregateRecord.getRctFine());
             */
            final Proto3EhaAggregatedGroup p3eag = retrieveAggregateGroup(aggregateRecord.getContents());

            final Proto3EhaGroupDiscriminator groupDisc = p3eag.getDiscriminatorKey();

            isMonitor = false;
            isSSE = false;
            isRealtime = false;
            isHeader = false;

            switch (groupDisc.getChanType()) {
            case CHAN_DEF_TYPE_M:
                isMonitor = true;
                break;
            case CHAN_DEF_TYPE_H:
                isHeader = true;
                break;
            default:
                break;
            }
            isRealtime = groupDisc.getIsRealtime();
            isSSE = groupDisc.getIsFromSSE();

            for (final Proto3EhaGroupMember member : p3eag.getValuesList()) {

                channelId = member.getChannelId().toUpperCase();

                if (isUsingChannelFilter && !channleIdMatchesFilter(channelId)) {
                    continue;
                }

                if (isUsingModuleFilter && !moduleMatchesFilter(member.getModule())) {
                    continue;
                }

                final long timeFormatStart = System.nanoTime();
                sol = null;
                if (member.hasScet()) {
                    scet = new AccurateDateTime(member.getScet().getMilliseconds(), member.getScet().getNanoseconds());

                    /**
                     * MPCS-10408: Only execute below logic when LST is enabled for
                     * performance
                     */
                    if (useLst) {
                        // Set local solar time for channels that have a real SCET
                        final VenueType vt = sessionPreFetch.lookupVenue(aggregateRecord.getHostId(),
                                aggregateRecord.getSessionId());

                        // TODO: Optimize missionProps.getVenueUsesSol() lookup, it uses expensive call
                        // to getListProperty
                        if (missionProps != null && missionProps.getVenueUsesSol(vt)) {
                            sol = LocalSolarTimeFactory.getNewLst(scet, aggregateRecord.getSpacecraftId());
                        }
                    }

                } else {
                    // Set as a dummy (certainly not real)
                    scet = new AccurateDateTime(true);
                }

                if (member.hasSclk()) {
                    sclk = new Sclk(member.getSclk().getSeconds(), member.getSclk().getNanos());
                } else {
                    sclk = new Sclk(true);
                }

                ert = new AccurateDateTime(member.getErt().getMilliseconds(), member.getErt().getNanoseconds());

                rct = new AccurateDateTime(member.getRct().getMilliseconds(), member.getRct().getNanoseconds());

                if (isUsingTimeRange && !recordWithinTimeRange()) {
                    continue;
                }

                totalTimeFormatAndRangeCheck += System.nanoTime() - timeFormatStart;

                IDbChannelSampleUpdater ref = constructRecordObject(aggregateRecord, rct, member, ert);

                if (isIncludePacketInfo) {
                    packetInfoMap = aggregateRecord.getPacketInfoMap();
                    final PacketInfo packetInfo = packetInfoMap.get(member.getPacketId());
                    ref.setPacketInfo(packetInfo.getApid(), packetInfo.getApidName(), packetInfo.getSpsc(),
                            packetInfo.getRct(), packetInfo.getVcfc());
                }

                if (isUsingAlarmFilter && !alarmMatchesFilter(ref)) {
                    continue;
                }

                perAggregateProcess();

                final long csvStart = System.nanoTime();
                recordList.add(getOutputRecordString(ref));
                totalCsvConversionTime += (System.nanoTime() - csvStart);

                ref = null;
                recordIndex++;
            }
        }

        logDebugStats();

        // MPCS-12309: chill_get_chanvals hangs when there are no records
        // in first aggregate batch, when sorting not used
        // MPCS-12309 is a clone of MPCS-12211
        if (recordList.isEmpty()) {
            trace.debug(AggregateFetchMarkers.AGGREGATE_BATCH_PROCESSOR, "RecordList is empty for batch: ", batchId);
            ehaAggregateQueryQoordinator.removeBatchIdFromBatchIdList(batchId);
            return;
        }

        postAggregateProcess();

        ehaAggregateQueryQoordinator.addBatchToCacheMap(batchId, batchInfo);
        trace.debug(AggregateFetchMarkers.AGGREGATE_BATCH_PROCESSOR, "Putting Batch: ", batchId,
                " into recordCacheMap");
    }

    private boolean moduleMatchesFilter(final String module) {
        return module.contains(modulePattern);
    }

    private boolean alarmMatchesFilter(final IDbChannelSampleUpdater ref) {
        return alarmControl.match(ref.getDnAlarmState(), ref.getEuAlarmState());
    }

    private boolean channleIdMatchesFilter(final String channelId) {
        return channelMap.contains(channelId);
    }

    private boolean aggregateMatchesFilter(final IEhaAggregateDbRecord aggregateRecord) {
        boolean match = false;
        final long chanFilterStart = System.nanoTime();
        final String chanIdList[] = aggregateRecord.getChannelIdsString().split(":");
        for (int i = 0; i < chanIdList.length; i++) {
            if (channelMap.contains(chanIdList[i])) {
                match = true;
                break;
            }
        }
        totalBatchChanIdFilterTime += System.nanoTime() - chanFilterStart;

        return match;
    }

    private void logDebugStats() {
        trace.debug(AggregateFetchMarkers.AGGREGATE_BATCH_PROCESSOR, "BatchProcessor # ", batchProcessorNumber,
                " for: ", batchId, " ChanId Filter time = ", (totalBatchChanIdFilterTime / 1000000.0), " msecs");
        trace.debug(AggregateFetchMarkers.AGGREGATE_BATCH_PROCESSOR, "BatchProcessor # ", batchProcessorNumber,
                " for: ", batchId, " Format And Range Check time = ", (totalTimeFormatAndRangeCheck / 1000000.0),
                " msecs");
        trace.debug(AggregateFetchMarkers.AGGREGATE_BATCH_PROCESSOR, "BatchProcessor # ", batchProcessorNumber,
                " for: ", batchId, " ChannelSampleUpdater time = ", (totalChanSampleUpdaterTime / 1000000.0), " msecs");
        trace.debug(AggregateFetchMarkers.AGGREGATE_BATCH_PROCESSOR, "BatchProcessor # ", batchProcessorNumber,
                " for: ", batchId, " Inflate time = ", (totalInvlateTime / 1000000.0), " msecs");
        trace.debug(AggregateFetchMarkers.AGGREGATE_BATCH_PROCESSOR, "BatchProcessor # ", batchProcessorNumber,
                " for: ", batchId, " Template Merge time = ", (totalTemplateMergeTime / 1000000.0), " msecs");
        trace.debug(AggregateFetchMarkers.AGGREGATE_BATCH_PROCESSOR, "BatchProcessor # ", batchProcessorNumber,
                " for: ", batchId, " Per record Template Merge avg = ",
                ((totalTemplateMergeTime / 1000000.0) / recordList.size()), " msecs/record");
        trace.debug(AggregateFetchMarkers.AGGREGATE_BATCH_PROCESSOR, "BatchProcessor # ", batchProcessorNumber,
                " for: ", batchId, " CSV conversion time = ", (totalCsvConversionTime / 1000000.0), " msecs");
        trace.debug(AggregateFetchMarkers.AGGREGATE_BATCH_PROCESSOR, "BatchProcessor # ", batchProcessorNumber,
                " for: ", batchId, " Deserializtion time = ", (totalDeserializtionTime / 1000000.0), " msecs");
    }

    private boolean recordWithinTimeRange() {
        if (databaseTimeRange.getTimeType().equals(DatabaseTimeType.ERT)){
            return inTimeRange(DatabaseTimeType.ERT, ert);
        }
        else if (databaseTimeRange.getTimeType().equals(DatabaseTimeType.SCET)){
            return inTimeRange(DatabaseTimeType.SCET, scet);
        }
        else if (databaseTimeRange.getTimeType().equals(DatabaseTimeType.SCLK)
                && ((databaseTimeRange.getStartSclk() != null && sclk.compareTo(databaseTimeRange.getStartSclk()) == -1)
                        || (databaseTimeRange.getStopSclk() != null
                        && sclk.compareTo(databaseTimeRange.getStopSclk()) == 1))) {

            trace.debug(AggregateFetchMarkers.AGGREGATE_BATCH_PROCESSOR, "Record with SCLK time: ", sclk.toString(),
                    " skipped, outside time range: ", databaseTimeRange.getStartSclk(), " - ",
                    databaseTimeRange.getStopSclk());

            return false;
        }

        return true;
    }

    //MPCS-11621 - handles ERT and SCET, checking for null
    boolean inTimeRange(final DatabaseTimeType type, final IAccurateDateTime time){
        final IAccurateDateTime startTime = databaseTimeRange.getStartTime();
        final IAccurateDateTime stopTime = databaseTimeRange.getStopTime();

         if (startTime != null && time.before(startTime) || (stopTime != null && time.after(stopTime))) {
             //start or stop times can be null
             trace.debug(AggregateFetchMarkers.AGGREGATE_BATCH_PROCESSOR, "Record with " + type.toString() + " time: ",
                         time.getFormattedErt(true), " skipped, outside time range: ",
                         startTime != null ? startTime.getFormattedErt(true): "null" ," - ",
                         stopTime != null ? stopTime.getFormattedErt(true) : "null");
             return false;
         }
         return true;
    }

    private String getOutputRecordString(final IDbChannelSampleUpdater ref) {

        if (template != null) {

            Map<String, Object> context = new HashMap<String, Object>();
            context.putAll(templateGlobalContext);
            context.put("body", true);
            context.put("formatter", formatter);
            ((IDbQueryable) ref).setTemplateContext(context);

            // Velocity doesn't have a built-in null checker, so using a
            // third-party "tool".
            context.put("nullTool", NullTool.getInstance());

            final long templateMergeStart = System.nanoTime();
            try {
                template.merge(new VelocityContext(context), writer);
            } catch (final Exception e) {
                e.printStackTrace();
                return "";
            }
            totalTemplateMergeTime += (System.nanoTime() - templateMergeStart);
            velocityOutput = writer.getBuffer().toString();
            writer.getBuffer().setLength(0);
            context.clear();
            context = null;

            return velocityOutput;
        } else {
            return ((IDbQueryable) ref).toCsv(csvColumns);
        }

    }

    private IDbChannelSampleUpdater constructRecordObject(final IEhaAggregateDbRecord aggregateRecord,
            final IAccurateDateTime rct, final Proto3EhaGroupMember member, final IAccurateDateTime ert) {

        proto3Dn = member.getDn();
        channelType = ChannelType.valueOf(proto3Dn.getType().name().substring("DN_TYPE_".length()));
        String status = null;
        switch (proto3Dn.getType()) {
        case DN_TYPE_STATUS:
            status = member.getStatus();
        case DN_TYPE_SIGNED_INT:
            if (proto3Dn.getDnCase().equals(DnCase._INT)) {
                val = proto3Dn.getInt();
            } else {
                val = proto3Dn.getLong();
            }
            break;
        case DN_TYPE_UNSIGNED_INT:
        case DN_TYPE_DIGITAL:
        case DN_TYPE_TIME:
            if (proto3Dn.getDnCase().equals(DnCase._UINT)) {
                val = proto3Dn.getUint();
            } else {
                val = proto3Dn.getUlong();
            }
            break;
        case DN_TYPE_FLOAT:
            if (proto3Dn.getDnCase().equals(DnCase._FLOAT)) {
                val = proto3Dn.getFloat();

                val = Float.valueOf((float) val).doubleValue();
            } else {
                val = proto3Dn.getDouble();
            }
            break;
        case DN_TYPE_ASCII:
            val = proto3Dn.getString();
            break;
        case DN_TYPE_BOOLEAN:
            if (proto3Dn.getBool()) {
                val = 1;
                status = member.getStatus();
            } else {
                val = 0;
                status = member.getStatus();
            }
            break;
        default:
            channelType = ChannelType.UNKNOWN;
            break;
        }

        Double eu = null;
        if (member.getHasEuCase().equals(HasEuCase.EU)) {
            eu = member.getEu();
        }

        Proto3AlarmLevel worstDnLevel = Proto3AlarmLevel.NONE;
        Proto3AlarmLevel worstEuLevel = Proto3AlarmLevel.NONE;

        if (member.getHasAlarmValueSetCase().equals(HasAlarmValueSetCase.ALARMVALUESET)) {
            final Proto3AlarmValueSet proto3AlarmValueSet = member.getAlarmValueSet();
            final List<Proto3AlarmValue> alarmList = proto3AlarmValueSet.getAlarmsList();
            for (final Proto3AlarmValue alarmValue : alarmList) {
                final Proto3AlarmLevel testLevel = alarmValue.getLevel();
                if (alarmValue.getIsInAlarm() && alarmValue.getIsOnEu()) {
                    if (testLevel.compareTo(worstEuLevel) > 0) {
                        worstEuLevel = testLevel;
                    }
                } else {
                    if (testLevel.compareTo(worstDnLevel) > 0) {
                        worstDnLevel = testLevel;
                    }
                }
            }
        }

        if (member.getModule().isEmpty()) {
            module = null;
        } else {
            module = member.getModule();
        }

        Integer vcid = aggregateRecord.getVcid();

        // For Monitor channels these fields should be null
        if (isMonitor) {
            sclk = null;
            scet = null;
        }

        // Set module to null for Monitor and Header channels
        if (isMonitor || isHeader) {
            module = null;
        }

        // Set vcid to null for Monitor and SSE channels
        if (isMonitor || isSSE) {
            vcid = null;
        }

        chanSampleUpdaterStart = System.nanoTime();
        final IDbChannelSampleUpdater ref = dbChannelSampleFactory.createQueryableAggregateProvider(
                aggregateRecord.getSessionId(), isSSE, // fromSse
                isRealtime, // isRealtime
                sclk, // sclk
                ert, // scet
                scet, // scet
                sol, // sol
                val, // val
                channelType, // channelType
                channelId, // channelId
                null, // channelIndex
                module, // module
                aggregateRecord.getHost(), // host
                eu, // eu
                worstDnLevel.getValueDescriptor().getName(), // dnAlarm
                worstEuLevel.getValueDescriptor().getName(), // euAlarm
                status, // status
                aggregateRecord.getSpacecraftId(), // scid
                member.getName(), // name
                aggregateRecord.getDssId(), // dssId
                vcid, // vcid
                rct, null, // packetId
                null); // frameId

        ref.setDnFormat(member.getDnFormat().isEmpty() ? null : member.getDnFormat());
        ref.setEuFormat(member.getEuFormat().isEmpty() ? null : member.getEuFormat());
        ref.setSessionFragment(aggregateRecord.getSessionFragmentHolder());
        totalChanSampleUpdaterTime += System.nanoTime() - chanSampleUpdaterStart;

        return ref;
    }

    protected Proto3EhaAggregatedGroup retrieveAggregateGroup(final byte[] bytes)
            throws InvalidProtocolBufferException, DataFormatException {
        return deserializeAggregate(inflateAggregate(bytes));
    }

    protected Proto3EhaAggregatedGroup deserializeAggregate(final byte[] bytes) throws InvalidProtocolBufferException {
        final long protoDesStart = System.nanoTime();
        final Proto3EhaAggregatedGroup ehaAggGroup = AggregateUtils.deserializeAggregate(bytes);
        totalDeserializtionTime += System.nanoTime() - protoDesStart;
        return ehaAggGroup;
    }

    protected byte[] inflateAggregate(final byte[] compressedBytes) throws DataFormatException {
        final long inflateStart = System.nanoTime();
        final byte[] docompressedBytes = AggregateUtils.decompress(compressedBytes);
        totalInvlateTime += System.nanoTime() - inflateStart;
        return docompressedBytes;
    }
}
