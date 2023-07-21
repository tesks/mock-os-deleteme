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
package jpl.gds.db.impl.types.cfdp;

import jpl.gds.cfdp.data.api.*;
import jpl.gds.db.api.types.cfdp.IDbCfdpIndicationUpdater;
import jpl.gds.db.impl.types.AbstractDatabaseItem;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.FastDateFormat;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.TimeUtility;
import org.springframework.context.ApplicationContext;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class provides a data object for the binary packet and various
 * attributes that the CfdpIndication Database knows explicitly.
 */
public class DatabaseCfdpIndication extends AbstractDatabaseItem implements IDbCfdpIndicationUpdater {

    private static final String CSV_COL_HDR = DQ + "CfdpIndication";

    private IAccurateDateTime indicationTime = null;
    private String cfdpProcessorInstanceId = null;
    private ECfdpIndicationType type = null;
    private ICfdpCondition condition = null;
    private ECfdpTransactionDirection transactionDirection = null;
    private long sourceEntityId = -1;
    private long transactionSequenceNumber = -1;
    private byte serviceClass = -1;
    private long destinationEntityId = -1;
    private boolean involvesFileTransfer = false;
    private long totalBytesSentOrReceived = -1;
    private ECfdpTriggeredByType triggeringType = null;
    private String pduId = null;
    private FixedPduHeader triggeringPduFixedHeader = new FixedPduHeader();

    private static final List<String> csvSkip = new ArrayList<>(0);

    public DatabaseCfdpIndication(final ApplicationContext appContext) {
        super(appContext);
    }

    @Override
    public IAccurateDateTime getIndicationTime() {
        return indicationTime;
    }

    @Override
    public String getCfdpProcessorInstanceId() {
        return cfdpProcessorInstanceId;
    }

    @Override
    public ECfdpIndicationType getType() {
        return type;
    }

    @Override
    public ICfdpCondition getCondition() {
        return condition;
    }

    @Override
    public ECfdpTransactionDirection getTransactionDirection() {
        return transactionDirection;
    }

    @Override
    public long getSourceEntityId() {
        return sourceEntityId;
    }

    @Override
    public long getTransactionSequenceNumber() {
        return transactionSequenceNumber;
    }

    @Override
    public byte getServiceClass() {
        return serviceClass;
    }

    @Override
    public long getDestinationEntityId() {
        return destinationEntityId;
    }

    @Override
    public boolean getInvolvesFileTransfer() {
        return involvesFileTransfer;
    }

    @Override
    public long getTotalBytesSentOrReceived() {
        return totalBytesSentOrReceived;
    }

    @Override
    public ECfdpTriggeredByType getTriggeringType() {
        return triggeringType;
    }

    @Override
    public String getPduId() {
        return pduId;
    }

    @Override
    public FixedPduHeader getTriggeringPduFixedHeader() {
        return triggeringPduFixedHeader;
    }

    @Override
    public void setIndicationTime(final IAccurateDateTime indicationTime) {
        this.indicationTime = indicationTime;
    }

    @Override
    public void setCfdpProcessorInstanceId(final String cfdpProcessorInstanceId) {
        this.cfdpProcessorInstanceId = cfdpProcessorInstanceId;
    }

    @Override
    public void setType(final ECfdpIndicationType type) {
        this.type = type;
    }

    @Override
    public void setCondition(final ICfdpCondition condition) {
        this.condition = condition;
    }

    @Override
    public void setTransactionDirection(final ECfdpTransactionDirection transactionDirection) {
        this.transactionDirection = transactionDirection;
    }

    @Override
    public void setSourceEntityId(final long sourceEntityId) {
        this.sourceEntityId = sourceEntityId;
    }

    @Override
    public void setTransactionSequenceNumber(final long transactionSequenceNumber) {
        this.transactionSequenceNumber = transactionSequenceNumber;
    }

    @Override
    public void setServiceClass(final byte serviceClass) {
        this.serviceClass = serviceClass;
    }

    @Override
    public void setDestinationEntityId(final long destinationEntityId) {
        this.destinationEntityId = destinationEntityId;
    }

    @Override
    public void setInvolvesFileTransfer(final boolean involvesFileTransfer) {
        this.involvesFileTransfer = involvesFileTransfer;
    }

    @Override
    public void setTotalBytesSentOrReceived(final long totalBytesSentOrReceived) {
        this.totalBytesSentOrReceived = totalBytesSentOrReceived;
    }

    @Override
    public void setTriggeringType(final ECfdpTriggeredByType triggeringType) {
        this.triggeringType = triggeringType;
    }

    @Override
    public void setPduId(final String pduId) {
        this.pduId = pduId;
    }

    @Override
    public void setTriggeringPduFixedHeader(final FixedPduHeader triggeringPduFixedHeader) {
        this.triggeringPduFixedHeader = triggeringPduFixedHeader;
    }

    @Override
    public Map<String, String> getFileData(String NO_DATA) {
        return null;
    }

    @Override
    public String toCsv(List<String> csvColumns) {
        final StringBuilder csv = new StringBuilder(1024);

        final DateFormat df = TimeUtility.getFormatterFromPool();

        csv.append(CSV_COL_HDR);

        for (final String cce : csvColumns) {
            final String upcce = cce.toUpperCase();

            csv.append(CSV_COL_SEP);

            switch (upcce) {

                case "SESSIONID":
                    if (sessionId != null) {
                        csv.append(sessionId);
                    }
                    break;

                case "CONTEXTID":
                    if (contextId != null) {
                        csv.append(contextId);
                    }
                    break;

                case "SESSIONHOST":
                    if (sessionHost != null) {
                        csv.append(sessionHost);
                    }
                    break;

                case "CONTEXTHOST":
                    if (contextHost != null) {
                        csv.append(contextHost);
                    }
                    break;

                case "INDICATIONTIME":
                case "EVENTTIME":
                case "RCT":
                    if (indicationTime != null) {
                        csv.append(df.format(indicationTime));
                    }
                    break;

                case "CFDPPROCESSORINSTANCEID":
                    if (cfdpProcessorInstanceId != null) {
                        csv.append(cfdpProcessorInstanceId);
                    }
                    break;

                case "TYPE":
                case "STATUS":
                    if (type != null) {
                        csv.append(type);
                    }
                    break;

                case "FAULTCONDITION":
                case "FAILREASON":
                    if (condition != null) {
                        csv.append(condition);
                    }
                    break;

                case "FINAL":
                    if(type!= null ) {
                        csv.append(type.isFinal());
                    }
                    break;

                case "FINALIZED":
                    if(type != null) {
                        csv.append((type.isFinal() && !type.equals(ECfdpIndicationType.ABANDONED)));
                    }
                    break;

                case "TRANSACTIONDIRECTION":
                    if (transactionDirection != null) {
                        csv.append(transactionDirection);
                    }
                    break;

                case "SOURCEENTITYID":
                    csv.append(Long.toUnsignedString(sourceEntityId));
                    break;

                case "TRANSACTIONSEQUENCENUMBER":
                case "REQUESTID":
                    csv.append(Long.toUnsignedString(transactionSequenceNumber));
                    break;

                case "SERVICECLASS":
                    csv.append(serviceClass);
                    break;

                case "DESTINATIONENTITYID":
                case "DSSID":
                    csv.append(Long.toUnsignedString(destinationEntityId));
                    break;

                case "INVOLVESFILETRANSFER":
                    csv.append(involvesFileTransfer);
                    break;

                case "TOTALBYTESSENTORRECEIVED":
                    csv.append(Long.toUnsignedString(totalBytesSentOrReceived));
                    break;

                case "TRIGGERINGTYPE":
                    if (triggeringType != null) {
                        csv.append(triggeringType);
                    }
                    break;

                case "PDUID":
                    if (pduId != null) {
                        csv.append(pduId);
                    }
                    break;

                case "PDUHEADERVERSION":
                    csv.append(triggeringPduFixedHeader.getVersion());
                    break;

                case "PDUHEADERTYPE":
                    if (triggeringPduFixedHeader.getType() != null) {
                        csv.append(triggeringPduFixedHeader.getType());
                    }
                    break;

                case "PDUHEADERDIRECTION":
                    if (triggeringPduFixedHeader.getDirection() != null) {
                        csv.append(triggeringPduFixedHeader.getDirection());
                    }
                    break;

                case "PDUHEADERTRANSMISSIONMODE":
                    if (triggeringPduFixedHeader.getTransmissionMode() != null) {
                        csv.append(triggeringPduFixedHeader.getTransmissionMode());
                    }
                    break;

                case "PDUHEADERCRCFLAGPRESENT":
                    csv.append(triggeringPduFixedHeader.isCrcFlagPresent());
                    break;

                case "PDUHEADERDATAFIELDLENGTH":
                    csv.append(triggeringPduFixedHeader.getDataFieldLength());
                    break;

                case "PDUHEADERENTITYIDLENGTH":
                    csv.append(triggeringPduFixedHeader.getEntityIdLength());
                    break;

                case "PDUHEADERTRANSACTIONSEQUENCENUMBERLENGTH":
                    csv.append(triggeringPduFixedHeader.getTransactionSequenceNumberLength());
                    break;

                case "PDUHEADERSOURCEENTITYID":
                    csv.append(Long.toUnsignedString(triggeringPduFixedHeader.getSourceEntityId()));
                    break;

                case "PDUHEADERTRANSACTIONSEQUENCENUMBER":
                    csv.append(Long.toUnsignedString(triggeringPduFixedHeader.getTransactionSequenceNumber()));
                    break;

                case "PDUHEADERDESTINATIONENTITYID":
                    csv.append(Long.toUnsignedString(triggeringPduFixedHeader.getDestinationEntityId()));
                    break;

                /*
                 * MPCS-10869 - All of the following were added for supporting indication objects
                 * being displayed in chill_get_commands
                 */
                case "SCMFFILE":
                case "ORGINALFILE":
                case "CHECKSUM":
                case "TOTALCLTUS":
                case "BIT1RADTIME":
                case "LASTBITRADTIME":
                case "COMMANDSTRING":
                case "ORIGINALFILE":
                case "VCIDNAME":
                    break;

                default:

                    if (!csvSkip.contains(upcce)) {
                        log.warn("Column " + cce + " is not supported, skipped");

                        csvSkip.add(upcce);
                    }

                    break;
            }
        }

        csv.append(CSV_COL_TRL);

        TimeUtility.releaseFormatterToPool(df);

        return csv.toString();
    }

    @Override
    public void parseCsv(String csvStr, List<String> csvColumns) {

        // The following removes the start/end quotes w/ the substring
        // and splits based on ",". It leaves the trailing empty string in the case that
        // csvStr ends with "". The empty strings server as place holders.
        final String[] dataArray = csvStr.substring(1, csvStr.length() - 1).split("\",\"", -1);

        if ((csvColumns.size() + 1) != dataArray.length) {
            throw new IllegalArgumentException("CSV column length mismatch, received " + dataArray.length
                    + " but expected " + (csvColumns.size() + 1));
        }

        // Clear everything we might process, in case empty column or not in list

        sessionId = null;
        contextId = null;
        sessionHost = null;
        contextHost = null;
        indicationTime = null;
        cfdpProcessorInstanceId = null;
        type = null;
        condition = null;
        transactionDirection = null;
        sourceEntityId = -1;
        transactionSequenceNumber = -1;
        serviceClass = -1;
        destinationEntityId = -1;
        involvesFileTransfer = false;
        totalBytesSentOrReceived = -1;
        triggeringType = null;
        pduId = null;
        /*
        MPCS-10321 Rather than nulling, instantiate anew so that we can populate its contents without
        running into NPE.
         */
        triggeringPduFixedHeader = new FixedPduHeader();

        int next = 1; // Skip recordType
        String token;

        for (final String cce : csvColumns) {
            token = dataArray[next].trim();

            ++next;

            if (token.isEmpty()) {
                continue;
            }

            final String upcce = cce.toUpperCase();

            try {
                switch (upcce) {
                    case "SESSIONID":
                        sessionId = Long.valueOf(token);
                        break;

                    case "CONTEXTID":
                        contextId = Long.valueOf(token);
                        break;

                    case "SESSIONHOST":
                        sessionHost = token;
                        break;

                    case "CONTEXTHOST":
                        contextHost = token;
                        break;

                    case "INDICATIONTIME":
                        indicationTime = new AccurateDateTime(token);
                        break;

                    case "CFDPPROCESSORINSTANCEID":
                        cfdpProcessorInstanceId = token;
                        break;

                    case "TYPE":
                        type = ECfdpIndicationType.valueOf(token);
                        break;

                    case "FAULTCONDITION":

                        try {
                            condition = ECfdpFaultCondition.valueOf(token);
                        } catch (IllegalArgumentException iae) {
                            condition = ECfdpNonFaultCondition.valueOf(token);
                        }

                        break;

                    case "TRANSACTIONDIRECTION":
                        transactionDirection = ECfdpTransactionDirection.valueOf(token);
                        break;

                    case "SOURCEENTITYID":
                        sourceEntityId = Long.parseUnsignedLong(token);
                        break;

                    case "TRANSACTIONSEQUENCENUMBER":
                        transactionSequenceNumber = Long.parseUnsignedLong(token);
                        break;

                    case "SERVICECLASS":
                        serviceClass = Byte.valueOf(token);
                        break;

                    case "DESTINATIONENTITYID":
                        destinationEntityId = Long.parseUnsignedLong(token);
                        break;

                    case "INVOLVESFILETRANSFER":
                        involvesFileTransfer = Boolean.valueOf(token);
                        break;

                    case "TOTALBYTESSENTORRECEIVED":
                        totalBytesSentOrReceived = Long.parseUnsignedLong(token);
                        break;

                    case "TRIGGERINGTYPE":
                        triggeringType = ECfdpTriggeredByType.valueOf(token);
                        break;

                    case "PDUID":
                        pduId = token;
                        break;

                    case "PDUHEADERVERSION":
                        triggeringPduFixedHeader.setVersion(Byte.valueOf(token));
                        break;

                    case "PDUHEADERTYPE":
                        triggeringPduFixedHeader.setType(ECfdpPduType.valueOf(token));
                        break;

                    case "PDUHEADERDIRECTION":
                        triggeringPduFixedHeader.setDirection(ECfdpPduDirection.valueOf(token));
                        break;

                    case "PDUHEADERTRANSMISSIONMODE":
                        triggeringPduFixedHeader.setTransmissionMode(ECfdpTransmissionMode.valueOf(token));
                        break;

                    case "PDUHEADERCRCFLAGPRESENT":
                        triggeringPduFixedHeader.setCrcFlagPresent(Boolean.valueOf(token));
                        break;

                    case "PDUHEADERDATAFIELDLENGTH":
                        triggeringPduFixedHeader.setDataFieldLength(Short.valueOf(token));
                        break;

                    case "PDUHEADERENTITYIDLENGTH":
                        triggeringPduFixedHeader.setEntityIdLength(Byte.valueOf(token));
                        break;

                    case "PDUHEADERTRANSACTIONSEQUENCENUMBERLENGTH":
                        triggeringPduFixedHeader.setTransactionSequenceNumberLength(Byte.valueOf(token));
                        break;

                    case "PDUHEADERSOURCEENTITYID":
                        triggeringPduFixedHeader.setSourceEntityId(Long.parseUnsignedLong(token));
                        break;

                    case "PDUHEADERTRANSACTIONSEQUENCENUMBER":
                        triggeringPduFixedHeader.setTransactionSequenceNumber(Long.parseUnsignedLong(token));
                        break;

                    case "PDUHEADERDESTINATIONENTITYID":
                        triggeringPduFixedHeader.setDestinationEntityId(Long.parseUnsignedLong(token));
                        break;

                    default:

                        if (!csvSkip.contains(upcce)) {
                            log.warn("Column " + cce + " is not supported, skipped");

                            csvSkip.add(upcce);
                        }

                        break;
                }
            } catch (final RuntimeException re) {
                re.printStackTrace();

                throw re;
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTemplateContext(final Map<String,Object> map) {
        /*
         * MPCS-10869 - Currently this only supports the
         *  fields that have values when shown in chill_get_commands.
         * This will need to be expanded if templates support is brough to chill_get_cfdp
         */
        super.setTemplateContext(map);

        StringBuilder sb = new StringBuilder();
        sb.append("CFDP Indication");
        if(type != null) {
            sb.append(" - ");
            sb.append(type);
        }

        map.put("commandType", sb.toString());

        map.put("eventTime", indicationTime != null ?
                FastDateFormat.format(this.indicationTime, null, null) : "");
        map.put("rct", indicationTime != null ?
                FastDateFormat.format(this.indicationTime, null, null) : "");
        map.put("rctExact", indicationTime != null ?
                indicationTime.getTime() : "");

        map.put("requestId", Long.toUnsignedString(transactionSequenceNumber));

        map.put("status", type != null ? type : "");

        map.put("failReason", condition != null ? condition : "");

        boolean fin = false;
        boolean finalized = false;

        if(type != null) {
            fin = type.isFinal();
            finalized = fin && !type.equals(ECfdpIndicationType.ABANDONED);
        }

        map.put("finalized", finalized);
        map.put("final", fin);

        map.put("dssId", Long.toUnsignedString(destinationEntityId));
    }

}
