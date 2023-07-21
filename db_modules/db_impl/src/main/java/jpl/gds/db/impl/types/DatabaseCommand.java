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
import jpl.gds.db.api.types.CommandType;
import jpl.gds.db.api.types.IDbCommandUpdater;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.FastDateFormat;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.tc.api.CommandStatusType;

import static jpl.gds.shared.util.BinOctHexUtility.HEX_STRING_PREFIX1;
import static jpl.gds.shared.util.BinOctHexUtility.HEX_STRING_PREFIX2;

/**
 * Holder for data queried from Command table.
 */
public class DatabaseCommand extends AbstractDatabaseItem implements IDbCommandUpdater
{
    private static final List<String> csvSkip =
        new ArrayList<String>(0);

    private static final Calendar      calendar    =
                                           FastDateFormat.getStandardCalendar();
	private static final StringBuilder csv         = new StringBuilder(1024);
	private static final StringBuilder csv2        = new StringBuilder();

    /** MPCS-6808 Most of these are inherited */
    private static final String CSV_COL_HDR = DQ + "Command";

	private CommandType type;
	private String commandString;
    private IAccurateDateTime eventTime;
    private IAccurateDateTime rct;
	private String originalFile;
	private String scmfFile;
	private String failReason;
	private String commandedSide = "";

    private String requestId = null;

    private CommandStatusType status = CommandStatusType.UNKNOWN;

    private boolean finalized = false;

    private boolean finl = false;

    private Long checksum = null;

    private Long totalCltus = null;

    /*
     * MPCS-6349 : DSS ID not set properly
     * Removed dssId. Parent class has been updated with 
     * protected fields sessionDssId and recordDssId with get/set 
     * methods for both.
     */

    private IAccurateDateTime bit1RadTime = null;

    private IAccurateDateTime lastBitRadTime = null;


    /**
     * Constructor.
     * 
     * @param appContext
     *            the Spring Application Context
     */
	public DatabaseCommand(final ApplicationContext appContext)
	{
		super(appContext);
		
		this.type = null;
		this.commandString = null;
		this.eventTime = null;
		this.rct = null;
		this.originalFile = null;
		this.scmfFile = null;
        this.failReason = null;
        this.requestId = null;
        this.status = CommandStatusType.UNKNOWN;
        this.finalized = false;
        this.finl = false;
        this.checksum = null;
        this.totalCltus = null;
        /* 
         * MPCS-6349 : DSS ID not set properly
         * Removed dssId. Parent class has been updated with 
         * protected fields sessionDssId and recordDssId with get/set 
         * methods for both.
         */        
        this.recordDssId = 0;
        this.bit1RadTime = null;
        this.lastBitRadTime = null;
	}


	/**
     * {@inheritDoc}
	 * 
	 * NOTE: This method is NOT threadsafe!!!! It utilizes unsynchronized
	 * static variables to do its work for performance reasons.
     *
	 */
    @Override
	public String toCsv(final List<String> csvColumns)
	{
        /* 
         * MPCS-6349 : DSS ID not set properly
         * Removed dssId. Parent class has been updated with 
         * protected fields sessionDssId and recordDssId with get/set 
         * methods for both.
         */        

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

                case "SESSIONHOST":
                    if (sessionHost != null)
                    {
                        csv.append(sessionHost);
                    }
                    break;

                case "EVENTTIME":
                   if (eventTime != null)
                   {
                       csv.append(FastDateFormat.format(eventTime, calendar, csv2));
                   }
                   break;

                case "REQUESTID":
                   if (requestId != null)
                   {
                       csv.append(requestId);
                   }
                   break;

                case "TYPE":
                   if (type != null)
                   {
                       csv.append(type);
                   }
                   break;

                case "STATUS":
                   if (status != null)
                   {
                       csv.append(status);
                   }
                   break;

                case "COMMANDSTRING":
                   if (commandString != null)
                   {
                       // Replacing embedded quotes w/ single quotes

                       csv.append(commandString.replace(DQ, SQ));
                   }
                   break;

                case "SCMFFILE":
                   if (scmfFile != null)
                   {
                       csv.append(scmfFile);
                   }
                   break;

                case "ORIGINALFILE":
                   if (originalFile != null)
                   {
                       csv.append(originalFile);
                   }
                   break;

                case "FAILREASON":
                   if (failReason != null)
                   {
                       csv.append(failReason);
                   }
                   break;

                case "FINAL":
                   csv.append(finl);
                   break;

                case "FINALIZED":
                   csv.append(finalized);
                   break;

                case "CHECKSUM":
                   if (checksum != null)
                   {
                       csv.append(formatChecksum(checksum));
                   }
                   break;

                case "TOTALCLTUS":
                   if (totalCltus != null)
                   {
                       csv.append(totalCltus);
                   }
                   break;

                case "DSSID":
                   csv.append(recordDssId);
                   break;

                case "BIT1RADTIME":
                   if (bit1RadTime != null)
                   {
                       csv.append(FastDateFormat.format(bit1RadTime, calendar, csv2));
                   }
                   break;

                case "LASTBITRADTIME":
                   if (lastBitRadTime != null)
                   {
                       csv.append(FastDateFormat.format(lastBitRadTime, calendar, csv2));
                   }
                   break;

                case "RCT":
                   if (rct != null)
                   {
                       csv.append(FastDateFormat.format(rct, calendar, csv2));
                   }
                   break;

                // MPCS-8021 - updated to reflect default value
                //vcid is not present in commands 
                case "VCIDNAME":
                	break;

                default:

                	// MPCS-8021  - added due to the configurable nature of the column name
                	if(missionProperties.getVcidColumnName().toUpperCase().equals(upcce))
                	{
                		//vcid is not present
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
     * commandString may contain commas; but we do not split on just comma, but on a comma
     * surrounded by double quotes. commandString does not contain double quotes because they
     * are removed in toCsv. Therefore, nothing special needs to be done to avoid erroneously
     * splitting the commandString.
     *
     * @version MPCS-6808 Massive rewrite
	 */
    @Override
	public void parseCsv(final String              csvStr,
                         final List<String> csvColumns)
    {	
        /* 
         * MPCS-6349 : DSS ID not set properly
         * Removed dssId. Parent class has been updated with 
         * protected fields sessionDssId and recordDssId with get/set 
         * methods for both.
         */                

		// The following removes the start/end quotes w/ the substring
		// and splits based on ",". It leaves the trailing empty string in the case that 
		// csvStr ends with "". The empty strings serve as place holders.
        final String[] dataArray =
            csvStr.substring(1, csvStr.length() - 1).split(CSV_COL_SEP, -1);

		//dataArray[0] is recordType

        if ((csvColumns.size() + 1) != dataArray.length)
        {
            throw new IllegalArgumentException("CSV column length mismatch, received " +
                                               dataArray.length                        +
                                               " but expected "                        +
                                               (csvColumns.size() + 1));
        }

        // Clear everything we might process, in case empty column or not in list

        sessionId      = null;
        sessionHost    = null;
        eventTime      = null;
        requestId      = null;
        type           = null;
        status         = null;
        commandString  = null;
        scmfFile       = null;
        originalFile   = null;
        failReason     = null;
        finl           = false;
        finalized      = false;
        checksum       = null;
        totalCltus     = null;
        recordDssId    = StationIdHolder.UNSPECIFIED_VALUE;
        bit1RadTime    = null;
        lastBitRadTime = null;
        rct            = null;

        int      next  = 1; // Skip recordType
        String   token;

        for (final String cce : csvColumns)
        {
            if (next >= dataArray.length)
            {
                // Skip extras
                break;
            }

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

                    case "REQUESTID":
                        requestId = token;
                        break;

                    case "TYPE":
                        type = new CommandType(token);
                        break;

                    case "STATUS":
                        status = CommandStatusType.valueOf(token);
                        break;

                    case "COMMANDSTRING":
                        commandString = token;
                        break;

                    case "SCMFFILE":
                        scmfFile = token;
                        break;

                    case "ORIGINALFILE":
                        originalFile = token;
                        break;

                    case "FAILREASON":
                        failReason = token;
                        break;

                    case "FINAL":
                        finl = Boolean.valueOf(token);
                        break;

                    case "FINALIZED":
                        finalized = Boolean.valueOf(token);
                        break;

                    case "CHECKSUM":
                        //MPCS-12020: Parse hex correctly
                        //add hex prefix if not present
                        if(!token.startsWith(HEX_STRING_PREFIX1) && !token.startsWith(HEX_STRING_PREFIX2)){
                            token = "0x" + token;
                        }
                        checksum = Long.decode(token);
                        break;

                    case "TOTALCLTUS":
                        totalCltus = Long.valueOf(token);
                        break;

                    case "DSSID":
                        recordDssId = Integer.valueOf(token);
                        break;

                    case "BIT1RADTIME":
                        bit1RadTime = new AccurateDateTime(token);
                        break;

                    case "LASTBITRADTIME":
                        lastBitRadTime = new AccurateDateTime(token);
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
		
		// "Command","$testSessionId","$sessionHost","$commandType",
        // "$commandString","$eventTime","$scmfFile","$failReason"

		map.put("commandType", this.type != null ? this.type.toString() : "");

        final String cs = StringUtil.emptyAsNull(commandString);

        if (cs != null)
        {
            map.put("commandString", cs);
        }

		map.put("eventTime",
                this.eventTime != null
                    ? FastDateFormat.format(this.eventTime, null, null)
                    : "");

        if (this.scmfFile != null)
        {
            map.put("scmfFile", this.scmfFile);
        }

        if (this.originalFile != null)
        {
            map.put("originalFile", this.originalFile);
        }

		map.put("requestId",
                this.requestId != null ? this.requestId : "");

		map.put("status",
                this.status != null ? this.status : "");

        if (this.failReason != null)
        {
            map.put("failReason", this.failReason);
        }

		map.put("finalized",
                this.finalized ? "true" : "false");

		map.put("final",
                this.finl ? "true" : "false");

        if (checksum != null)
        {
            map.put("checksum", formatChecksum(checksum));
        }

        if (totalCltus != null)
        {
            map.put("totalCltus", totalCltus);
        }
        
        /* 
         * MPCS-6349 : DSS ID not set properly
         * Removed dssId. Parent class has been updated with 
         * protected fields sessionDssId and recordDssId with get/set 
         * methods for both.
         */
        map.put("dssId", recordDssId);

        if (bit1RadTime != null)
        {
            map.put("bit1RadTime",
                    FastDateFormat.format(bit1RadTime, null, null));
        }

        if (lastBitRadTime != null)
        {
            map.put("lastBitRadTime",
                    FastDateFormat.format(lastBitRadTime, null, null));
        }

        // Make special entries for sr_csv, sr_text, excel templates

        final String   iddata = StringUtil.safeTrim(commandString).replace("\"", "");
        final String[] split  = iddata.split(",", 2);

        String id   = null;
        String data = null;

        if (split.length > 1)
        {
            id   = split[0];
            data = split[1];
        }
        else
        {
            id   = split[0];
            data = "";
        }

        map.put("id", id);

        if (originalFile == null)
        {
            final String extra = "Status=" + status + ",Command=";

            map.put("data",   extra + data);
            map.put("iddata", extra + iddata);
        }
        else
        {
            final String total = "Status=" + status + ",File=" + originalFile;

            map.put("data",   total);
            map.put("iddata", total);
        }


        if (this.rct != null)
        {
            map.put("rct", FastDateFormat.format(this.rct, null, null));
            map.put("rctExact", this.rct.getTime());
        }
        
        map.put("commandedSide", commandedSide);
    }


    /**
     * {@inheritDoc}
     */
	@Override
    public CommandType getType() {
		return type;
	}

    /**
     * {@inheritDoc}
     */
	@Override
    public void setType(final CommandType type) {
		this.type = type;
	}

    /**
     * {@inheritDoc}
     */
	@Override
    public String getCommandString() {
		return commandString;
	}

    /**
     * {@inheritDoc}
     */
	@Override
    public void setCommandString(final String commandString) {
		this.commandString = commandString;
	}


    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("EI_EXPOSE_REP")
	public IAccurateDateTime getEventTime() {
		return eventTime;
	}


    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("EI_EXPOSE_REP2")
    public void setEventTime(final IAccurateDateTime eventTime) {
		this.eventTime = eventTime;
	}


    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("EI_EXPOSE_REP")
    public IAccurateDateTime getRct() {
		return rct;
	}


    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("EI_EXPOSE_REP2")
    public void setRct(final IAccurateDateTime rct) {
		this.rct = rct;
	}


    /**
     * {@inheritDoc}
     */
	@Override
    public String getOriginalFile() {
		return originalFile;
	}

    /**
     * {@inheritDoc}
     */
	@Override
    public void setOriginalFile(final String originalFile) {
		this.originalFile = originalFile;
	}

    /**
     * {@inheritDoc}
     */
	@Override
    public String getScmfFile() {
		return scmfFile;
	}

    /**
     * {@inheritDoc}
     */
	@Override
    public void setScmfFile(final String scmfFile) {
		this.scmfFile = scmfFile;
	}

    /**
     * {@inheritDoc}
     */
	@Override
    public String getFailReason() {
		return failReason;
	}

    /**
     * {@inheritDoc}
     */
	@Override
    public void setFailReason(final String failReason) {
		this.failReason = failReason;
	}


    /**
     * {@inheritDoc}
     */
    @Override
    public String getCommandedSide()
    {
        return commandedSide;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setCommandedSide(final String cs)
    {
        commandedSide = StringUtil.emptyAsNull(cs);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getRequestId()
    {
        return requestId;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setRequestId(final String id)
    {
        requestId = id;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public CommandStatusType getStatus()
    {
        return status;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setStatus(final CommandStatusType cst)
    {
        status = ((cst != null) ? cst : CommandStatusType.UNKNOWN);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getFinalized()
    {
        return finalized;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setFinalized(final boolean fin)
    {
        finalized = fin;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getFinal()
    {
        return finl;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setFinal(final boolean fin)
    {
        finl = fin;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Long getChecksum()
    {
        return checksum;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setChecksum(final Long value)
    {
        checksum = value;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Long getTotalCltus()
    {
        return totalCltus;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setTotalCltus(final Long value)
    {
        totalCltus = value;
    }


    /* 
     * MPCS-6349 : DSS ID not set properly
     * Removed dssId. Parent class has been updated with 
     * protected fields sessionDssId and recordDssId with get/set 
     * methods for both.
     */
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public IAccurateDateTime getBit1RadTime()
    {
        return ((bit1RadTime != null)
                ? new AccurateDateTime(bit1RadTime.getTime()) : null);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setBit1RadTime(final IAccurateDateTime value)
    {
        bit1RadTime = ((value != null) ? new AccurateDateTime(value.getTime()) : null);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public IAccurateDateTime getLastBitRadTime()
    {
        return ((lastBitRadTime != null)
                ? new AccurateDateTime(lastBitRadTime.getTime()) : null);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setLastBitRadTime(final IAccurateDateTime value)
    {
        lastBitRadTime = ((value != null) ? new AccurateDateTime(value.getTime()) : null);
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
		
		if (type != null) {
            if (type.equals(CommandType.SSE_COMMAND)) {
				map.put("source","SSE CMD");
			}
			else {
				map.put("source","FSW CMD");
			}
		}
		else {
			map.put("source", NO_DATA);
		}
		
		if (commandString != null) {
			String message_clean = commandString;
			map.put("data",message_clean);

			message_clean = message_clean.replaceAll("\"", ""); // Removing embedded quotes
			final String[] data_a = message_clean.split(",",2);
			map.put("id",data_a[0]);		// For session report csv format
			map.put("data_sr_csv", "\"" + (data_a.length > 1 ? data_a[1] : "") + "\"");	// For session report csv format
		}
		else {
			map.put("data",NO_DATA);
			map.put("id",NO_DATA);		// For session report csv format
			map.put("data_sr_csv","\"\"");	// For session report csv format
		}
			
		TimeUtility.releaseFormatterToPool(df);
		
		return map;
	}


    /**
     * Format a checksum for presentation.
     *
     * @param checksum Value as unsigned integer
     *
     * @return Formatted string
     */
    private static String formatChecksum(final long checksum)
    {
        final int           digits = Integer.SIZE / 4;
        final String        hex    = Long.toHexString(
                                         checksum &
                                         ((1L << Integer.SIZE) - 1L));

        final StringBuilder sb  = new StringBuilder(digits);

        for (int i = hex.length(); i < digits; ++i)
        {
            sb.append('0');
        }

        sb.append(hex);

        return sb.toString();
    }
}
