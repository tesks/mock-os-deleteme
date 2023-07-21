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

import jpl.gds.db.api.types.cfdp.IDbCfdpPduReceivedUpdater;
import jpl.gds.db.impl.types.AbstractDatabaseItem;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.TimeUtility;
import org.springframework.context.ApplicationContext;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class provides a data object for the binary packet and various
 * attributes that the CfdpPduReceived Database knows explicitly.
 */
public class DatabaseCfdpPduReceived extends AbstractDatabaseItem implements IDbCfdpPduReceivedUpdater {

    private static final String CSV_COL_HDR = DQ + "CfdpPduReceived";

    private IAccurateDateTime pduTime = null;
    private String cfdpProcessorInstanceId = null;
    private String pduId = null;
    private String metadata = null;

    private static final List<String> csvSkip = new ArrayList<String>(0);

    public DatabaseCfdpPduReceived(final ApplicationContext appContext) {
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
        return metadata;
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
        this.metadata = metadata;
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
                    if (pduId != null) {
                        csv.append(pduId);
                    }
                    break;

                case "METADATA":
                    if (metadata != null) {
                        csv.append(metadata);
                    }
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
        pduTime = null;
        cfdpProcessorInstanceId = null;
        pduId = null;
        metadata = null;

        int next = 1; // Skip recordType
        String token = null;

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
                        metadata = token;
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

}
