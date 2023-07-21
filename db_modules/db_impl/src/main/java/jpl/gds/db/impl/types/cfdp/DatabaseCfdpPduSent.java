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

import jpl.gds.db.api.types.cfdp.IDbCfdpPduSentUpdater;
import jpl.gds.db.impl.types.AbstractDatabaseItem;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.FastDateFormat;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.TimeUtility;

import org.springframework.context.ApplicationContext;

import java.text.DateFormat;
import java.util.*;

import com.google.common.base.Splitter;

/**
 * This class provides a data object for the binary packet and various
 * attributes that the CfdpPduSent Database knows explicitly.
 *
 * MPCS-11215: Improvements in memory usage to avoid OOM
 */
public class DatabaseCfdpPduSent extends AbstractDatabaseItem implements IDbCfdpPduSentUpdater {

    private static final String CSV_COL_HDR = DQ + "CfdpPduSent";

    private IAccurateDateTime pduTime = null;
    private String cfdpProcessorInstanceId = null;
    private String pduId = null;

    private Map<String,String> metadataMap = new LinkedHashMap<>();

    private static final Set<String> csvSkip = new HashSet<>(0);

    private static final Splitter COMMA_SPLITER = Splitter.on(',').trimResults().omitEmptyStrings();

    /**
     * Constructor
     * @param appContext Spring Application Context
     */
    public DatabaseCfdpPduSent(final ApplicationContext appContext) {
        super(appContext);
    }

    @Override
    public IAccurateDateTime getPduTime() {
        return pduTime;
    }

    @Override
    public String getCfdpProcessorInstanceId() {
        return cfdpProcessorInstanceId;
    }

    @Override
    public String getPduId() {
        return pduId;
    }

    @Override
    public String getMetadata() {
        return metadataToCsv();
    }

    @Override
    public void setPduTime(final IAccurateDateTime pduTime) {
        this.pduTime = pduTime;
    }

    @Override
    public void setCfdpProcessorInstanceId(final String cfdpProcessorInstanceId) {
        this.cfdpProcessorInstanceId = cfdpProcessorInstanceId;
    }

    @Override
    public void setPduId(final String pduId) {
        this.pduId = pduId;
    }

    @Override
    public void setMetadata(final String metadata) {
        this.parseMetadata(metadata);
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

                case "PDUTIME":
                case "EVENTTIME":
                case "RCT":
                    if (pduTime != null) {
                        csv.append(df.format(pduTime));
                    }
                    break;

                case "CFDPPROCESSORINSTANCEID":
                    if (cfdpProcessorInstanceId != null) {
                        csv.append(cfdpProcessorInstanceId);
                    }
                    break;

                case "PDUID":
                case "REQUESTID":
                    if (pduId != null) {
                        csv.append(pduId);
                    }
                    break;

                case "METADATA":
                    csv.append(metadataToCsv());
                    break;

                case "TYPE":
                    if(!metadataMap.isEmpty()) {
                        csv.append(getMetadata("pduType"));
                    }
                    break;

                case "FINAL":
                    if(!metadataMap.isEmpty()) {
                        String directiveType = getMetadata("fileDirective");
                        csv.append(!directiveType.isEmpty() && directiveType.equalsIgnoreCase("EOF"));
                    }
                    break;

                case "ORIGINALFILE":
                    if(!metadataMap.isEmpty()) {
                        csv.append(getMetadata("sourceFileName"));
                    }
                    break;

                case "SCMFFILE":
                    if(!metadataMap.isEmpty()) {
                        csv.append(getMetadata("destinationFileName"));
                    }
                    break;

                case "CHECKSUM":
                    if(!metadataMap.isEmpty()) {
                        csv.append(getMetadata("crcValue"));
                    }
                    break;

                case "BIT1RADTIME":
                    if(!metadataMap.isEmpty()) {
                        csv.append(getMetadata("sendTime"));
                    }
                    break;

                case "STATUS":
                    if(!metadataMap.isEmpty()) {
                        csv.append(getMetadata("fileDirective"));
                    }
                    break;

                /*
                 * MPCS-10869 - All of the following were added for supporting PDU sent objects
                 * being displayed in chill_get_commands
                 */
                case "COMMANDSTRING":
                case "FAILREASON":
                case "TOTALCLTUS":
                case "DSSID":
                case "LASTBITRADTIME":
                case "FINALIZED":
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

    public String getMetadata(final String key) {
        String value = metadataMap.get(key);

        return value != null ? value : "";
    }

    String metadataToCsv(){
        final StringBuilder sb = new StringBuilder();
        int size = metadataMap.size();
        int cnt = 0;
        for (Map.Entry<String,String> entry : metadataMap.entrySet()){
            cnt++;
            sb.append(entry.getKey());
            sb.append("=");
            sb.append(entry.getValue());
            if(cnt < size) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    private void parseMetadata(String metadata) {
        metadataMap.clear();

        if(metadata == null) {
            return;
        }

        for(String metaEntry : COMMA_SPLITER.split(metadata)) {
            // in case the value is a string and has a = in it
            String[] metaPair = metaEntry.split("=", 2);
            // currently there shouldn't be any value in the metadata that aren't key/value, but just in case
            metadataMap.put(metaPair[0], metaPair.length == 2 ? metaPair[1] : metaPair[0]);
        }
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
        pduTime = null;
        cfdpProcessorInstanceId = null;
        pduId = null;
        setMetadata(null);

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

                    case "PDUTIME":
                        pduTime = new AccurateDateTime(token);
                        break;

                    case "CFDPPROCESSORINSTANCEID":
                        cfdpProcessorInstanceId = token;
                        break;

                    case "PDUID":
                        pduId = token;
                        break;

                    case "METADATA":
                        setMetadata(token);
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
    public void setTemplateContext(final Map<String, Object> map) {
        /*
         * MPCS-10869 - Currently this only supports the
         *  fields that have values when shown in chill_get_commands.
         * This will need to be expanded if templates support is brough to chill_get_cfdp
         */
        super.setTemplateContext(map);

        StringBuilder sb = new StringBuilder();
        sb.append("CFDP PDU Sent");
        if(!getMetadata("pduType").isEmpty()) {
            sb.append(" - ");
            sb.append(getMetadata("pduType"));
        }

        map.put("commandType", sb.toString());

        map.put("eventTime", this.pduTime != null ?
                        FastDateFormat.format(this.pduTime, null, null) : "");
        map.put("rct", this.pduTime != null ?
                        FastDateFormat.format(this.pduTime, null, null) : "");
        map.put("rctExact", this.pduTime != null ?
                this.pduTime.getTime() : "");

        map.put("pduId", pduId != null ? pduId : "");
        map.put("requestId", pduId != null ? pduId : "");

        if(!metadataMap.isEmpty()) {
            map.put("scmfFile", getMetadata("destinationFileName"));
            map.put("originalFile", getMetadata("sourceFileName"));
            map.put("status", getMetadata("fileDirective"));
            map.put("final", !getMetadata("fileDirective").isEmpty()
                    && getMetadata("fileDirective").equalsIgnoreCase("EOF"));
            map.put("checksum", getMetadata("crcValue"));
            map.put("bit1RadTime", getMetadata("sendTime"));
        }
    }

}
