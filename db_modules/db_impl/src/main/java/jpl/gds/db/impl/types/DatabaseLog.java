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
package jpl.gds.db.impl.types;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.db.api.types.IDbLogUpdater;
import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.FastDateFormat;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.TimeUtility;


/**
 * Holder for data from database LogMessage table.
 */
public class DatabaseLog extends AbstractDatabaseItem implements IDbLogUpdater
{
    private static final Calendar      calendar    =
                                           FastDateFormat.getStandardCalendar();
	private static final StringBuilder csv         = new StringBuilder(1024);
	private static final StringBuilder csv2        = new StringBuilder();

    /** MPCS-6808 String constants pushed up */
    private static final String CSV_COL_HDR = DQ + "Log";

	private IAccurateDateTime eventTime;
	private IAccurateDateTime rct;
	private String classification;
	private String message;
	private LogMessageType type;
	private TraceSeverity severity;

    private static final List<String> csvSkip =
        new ArrayList<String>(0);
	

    /**
     * Constructor.
     * 
     * @param appContext
     *            the Spring Application Context
     */
	public DatabaseLog(final ApplicationContext appContext)
	{
		super(appContext);
		
		this.eventTime = null;
		this.rct       = null;
		this.classification = null;
		this.message = null;
		this.type = null;
		this.severity = null;
	}


	/**
     * {@inheritDoc}
	 * 
	 * NOTE: This method is NOT threadsafe!!!! It utilizes unsynchronized
	 * static variables to do its work for performance reasons.
     *
     * @version MPCS-6808 Massive rewrite
	 */
    @Override
	public String toCsv(final List<String> csvColumns)
	{
        csv.setLength(0);

		csv.append(CSV_COL_HDR);

        for (final String cce : csvColumns)
        {
            final String upcce = cce.toUpperCase();

            csv.append(CSV_COL_SEP);

            switch (upcce)
            {
                case "SESSIONID":
                    if (sessionId != null)
                    {
                        csv.append(sessionId);
                    }
                    break;
                case "CONTEXTID":
                    if (contextId != null)
                    {
                        csv.append(contextId);
                    }
                    break;

                case "SESSIONHOST":
                    if (sessionHost != null)
                    {
                        csv.append(sessionHost);
                    }
                    break;

                case "EVENTTIME":
                   if (eventTime != null)
                   {
                       csv.append(FastDateFormat.format(
                                      eventTime, calendar, csv2));
                   }
                   break;

                case "SEVERITY":
                    if (severity != null)
                    {
                        csv.append(severity);
                    }
                    break;

                case "TYPE":
                    if (type != null)
                    {
                        csv.append(type);
                    }
                    break;

                case "MESSAGE":
                    if (message != null)
                    {
                        // Replacing embedded quotes with single quotes
                        csv.append(message.replaceAll(DQ, SQ));
                    }
                    break;

                case "RCT":
                   if (rct != null)
                   {
                       csv.append(FastDateFormat.format(rct, calendar, csv2));
                   }
                   break;

                // MPCS-8011 - Added to prevent error message from being generated
                case "SESSIONDSSID":
                	csv.append(getSessionDssId());
                	break;

                // MPCS-8021 - updated to reflect default value
                //vcid is not present in logs 
                case "VCIDNAME":

                	csv.append("");
                	break;

                case "SESSIONFRAGMENT":
                    csv.append(getSessionFragment());
                    break;

                default:

                	// MPCS-8021 - added due to the configurable nature of the column name
                	if(missionProperties.getVcidColumnName().toUpperCase().equals(upcce))
                	{
                		//vcid is not present
                		csv.append("");
                	}
                	else if (! csvSkip.contains(upcce))
                	{
                		log.warn("Column " + 
                				cce       +
                				" is not supported, skipped");

                		csvSkip.add(upcce);
                	}

                	break;
            }
        }

		csv.append(CSV_COL_TRL);

		return csv.toString();
	}
	

	/**
     * {@inheritDoc}
     *
     * @version MPCS-6808  Massive rewrite
	 */
    @Override
	public void parseCsv(final String              csvStr,
                         final List<String> csvColumns)
    {
		// The following removes the start/end quotes w/ the substring
		// and splits based on ",".  It leaves the trailing empty string in the case that 
		// csvStr ends with "".  The empty strings server as place holders.
        final String[] dataArray = csvStr.substring(1, csvStr.length()-1).split("\",\"",-1);

        if ((csvColumns.size() + 1) != dataArray.length)
        {
            throw new IllegalArgumentException("CSV column length mismatch, received " +
                                               dataArray.length                        +
                                               " but expected "                        +
                                               (csvColumns.size() + 1));
        }

        // Clear everything we might process, in case empty column or not in list

        sessionId   = null;
        sessionHost = null;
        eventTime   = null;
        severity    = null;
        type        = null;
        message     = null;
        rct         = null;

        int    next  = 1; // Skip recordType
        String token = null;

        for (final String cce : csvColumns)
        {
            token = dataArray[next].trim();

            ++next;

            if (token.isEmpty())
            {
                continue;
            }

            final String upcce = cce.toUpperCase();

            try
            {
                switch (upcce)
                {
                    case "SESSIONID":
                        sessionId = Long.valueOf(token);
                        break;

                    case "SESSIONHOST":
                        sessionHost = token;
                        break; 

                    case "EVENTTIME":
                        eventTime = new AccurateDateTime(token);
                        break;

                    case "SEVERITY":
                        severity = TraceSeverity.fromStringValue(token);
                        break;

                    case "TYPE":
                        type = LogMessageType.fromStringValue(token);
                        break;

                    case "MESSAGE":
                        message = token;
                        break;

                    case "RCT":
                        rct = new AccurateDateTime(token);
                        break;
                        
                    //MPCS-8021 added to handle named vcid column in parseCsv
                    case "VCIDNAME":
                    	//vcid name is mapped, not stored. do nothing
                    	break;

                    default:
                    	//MPCS-8021 added to handle named vcid column in parseCsv. Added here as well due to configurable nature of the column name
                    	if(missionProperties.getVcidColumnName().toUpperCase().equals(upcce))
                    	{
                    		//vcid name is mapped, not stored. do nothing
                    	}
                    	else if (! csvSkip.contains(upcce))
                    	{
                    		log.warn("Column " + 
                    				cce       +
                    				" is not supported, skipped");

                    		csvSkip.add(upcce);
                    	}

                    	break;
                }
             }
             catch (final RuntimeException re)
             {
                 re.printStackTrace();

                 throw re;
		     }
             catch (final Exception e)
             {
                 e.printStackTrace();
             }
        }
	}


	/**
     * {@inheritDoc}
	 */
    @Override
	public void setTemplateContext(final Map<String,Object> map)
	{
		super.setTemplateContext(map);
		
    	map.put("eventTime",
                (this.eventTime != null) 
                    ? FastDateFormat.format(this.eventTime, null, null)
                    : "");

        map.put("severity",
                (this.severity != null) ? this.severity.toString() : "");

        map.put("message", (this.message != null) ? this.message : "");

        map.put("type", (this.type != null) ? this.type.toString() : "");

        /** MPCS-6808  Add RCT */

        if (this.rct != null)
        {
            map.put("rct", FastDateFormat.format(this.rct, null, null));
            map.put("rctExact", this.rct.getTime());
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("EI_EXPOSE_REP")
    public IAccurateDateTime getEventTime()
    {
		return eventTime;
	}

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("EI_EXPOSE_REP2")
    public void setEventTime(final IAccurateDateTime eventTime)
    {
		this.eventTime = eventTime;
	}


    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("EI_EXPOSE_REP")
    public IAccurateDateTime getRct()
    {
		return rct;
	}

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("EI_EXPOSE_REP2")
    public void setRct(final IAccurateDateTime rct)
    {
		this.rct = rct;
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public String getClassification() {
		return classification;
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public void setClassification(final String classification) {
		this.classification = classification;
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMessage() {
		return message;
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMessage(final String message) {
		this.message = message;
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public LogMessageType getType() {
		return type;
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public void setType(final LogMessageType type) {
		this.type = type;
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public TraceSeverity getSeverity() {
		return severity;
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSeverity(final TraceSeverity severity) {
		this.severity = severity;
	}
	
	/**
     * {@inheritDoc}
	 */
    @Override
	public Map<String,String> getFileData(final String NO_DATA) {
		
		final DateFormat df = TimeUtility.getFormatterFromPool();
		final Map<String,String> map = new HashMap<String,String>();
		
		map.put("ert", eventTime != null ? df.format(eventTime) : NO_DATA);
		map.put("scet",NO_DATA);
		map.put("sclk",NO_DATA);
		final String level_st = severity != null ? severity.toString().toUpperCase() : NO_DATA;
		map.put("level",level_st);	
		map.put("type","LOG " + level_st);	// For session report csv format
		map.put("source","MPCS");	// For session report csv format
		map.put("id",type != null ? type.toString() : NO_DATA);
		// TODO Eventually we should not have to remove the newlines:
		String message_clean = message != null ? message.replaceAll("[\r\n]+", " ") : NO_DATA; // removes the newlines from message
		map.put("data", message_clean);	

		message_clean = message_clean.replaceAll("\"", ""); // Removing embedded quotes
		message_clean = "\"" + message_clean + "\""; // Adding starting/ending quotes
		map.put("data_sr_csv",message_clean);	// for session report csv format
		
		TimeUtility.releaseFormatterToPool(df);
		
		return map;

	}
}
