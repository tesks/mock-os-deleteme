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

import jpl.gds.db.api.types.cfdp.IDbCfdpRequestReceivedUpdater;
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
 * attributes that the CfdpRequestReceived Database knows explicitly.
 */
public class DatabaseCfdpRequestReceived extends AbstractDatabaseItem implements IDbCfdpRequestReceivedUpdater {

    private static final String CSV_COL_HDR = DQ + "CfdpRequestReceived";

    private IAccurateDateTime eventTime = null;
    private String cfdpProcessorInstanceId = null;
    private String requestId = null;
    private String requesterId = null;
    private String httpUser = null;
    private String httpHost = null;
    private String requestContent = null;

    private static final List<String> csvSkip = new ArrayList<String>(0);

    public DatabaseCfdpRequestReceived(final ApplicationContext appContext) {
        super(appContext);
    }

    @Override
    public IAccurateDateTime getEventTime() {
        return eventTime;
    }

    @Override
    public String getCfdpProcessorInstanceId() {
        return cfdpProcessorInstanceId;
    }

    @Override
    public String getRequestId() {
        return requestId;
    }

    @Override
    public String getRequesterId() {
        return requesterId;
    }

    @Override
    public String getHttpUser() {
        return httpUser;
    }

    @Override
    public String getHttpHost() {
        return httpHost;
    }

    @Override
    public String getRequestContent() {
        return requestContent;
    }

    @Override
    public void setEventTime(final IAccurateDateTime eventTime) {
        this.eventTime = eventTime;
    }

    @Override
    public void setCfdpProcessorInstanceId(final String cfdpProcessorInstanceId) {
        this.cfdpProcessorInstanceId = cfdpProcessorInstanceId;
    }

    @Override
    public void setRequestId(final String requestId) {
        this.requestId = requestId;
    }

    @Override
    public void setRequesterId(final String requesterId) {
        this.requesterId = requesterId;
    }

    @Override
    public void setHttpUser(final String httpUser) {
        this.httpUser = httpUser;
    }

    @Override
    public void setHttpHost(final String httpHost) {
        this.httpHost = httpHost;
    }

    @Override
    public void setRequestContent(final String requestContent) {
        this.requestContent = requestContent;
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

                case "EVENTTIME":
                    if (eventTime != null) {
                        csv.append(df.format(eventTime));
                    }
                    break;

                case "CFDPPROCESSORINSTANCEID":
                    if (cfdpProcessorInstanceId != null) {
                        csv.append(cfdpProcessorInstanceId);
                    }
                    break;

                case "REQUESTID":
                    if (requestId != null) {
                        csv.append(requestId);
                    }
                    break;

                case "REQUESTERID":
                    if (requesterId != null) {
                        csv.append(requesterId);
                    }
                    break;

                case "HTTPUSER":
                    if (httpUser != null) {
                        csv.append(httpUser);
                    }
                    break;

                case "HTTPHOST":
                    if (httpHost != null) {
                        csv.append(httpHost);
                    }
                    break;

                case "REQUESTCONTENT":
                    if (requestContent != null) {
                        csv.append(requestContent);
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
        eventTime = null;
        cfdpProcessorInstanceId = null;
        requestId = null;
        requesterId = null;
        httpUser = null;
        httpHost = null;
        requestContent = null;

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

                    case "EVENTTIME":
                        eventTime = new AccurateDateTime(token);
                        break;

                    case "CFDPPROCESSORINSTANCEID":
                        cfdpProcessorInstanceId = token;
                        break;

                    case "REQUESTID":
                        requestId = token;
                        break;

                    case "REQUESTERID":
                        requesterId = token;
                        break;

                    case "HTTPUSER":
                        httpUser = token;
                        break;

                    case "HTTPHOST":
                        httpHost = token;
                        break;

                    case "REQUESTCONTENT":
                        requestContent = token;
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
