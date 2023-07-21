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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.types.IDbLog1553Updater;
import jpl.gds.shared.holders.SessionFragmentHolder;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.SclkFmt.SclkFormatter;
import jpl.gds.shared.time.TimeProperties;
import jpl.gds.shared.time.TimeUtility;


/**
 * Used for 1553 log data.  Used by chill_get_everything.  Note, that this data 
 * is not retrieved from the database, but is parsed from the output of chill_decode_1553.
 *
 */
public class Log1553 extends AbstractDatabaseItem implements IDbLog1553Updater
{
    private static final List<String> csvSkip = new ArrayList<String>(0);

	/**
	 * The system time and date
	 */
	private IAccurateDateTime sysTime;
	
	/**
	 * The spacecraft clock in microseconds
	 */
	private ISclk sclk;
	
	/**
	 * The bus the message was on
	 */
	private String bus;
	
	/**
	 * The address of the receiving remote terminal
	 */
	private String remoteTerminal;
	
	/**
	 * The sub-address that the command is targeting
	 */
	private String subAddress;
	
	/**
	 * The actual data being exchanged
	 */
	private String data;
	
	/**
	 * The transmit/receive status from the perspective of the remote terminal
	 */
	private String transmitReceiveStatus;
	
    /** MPCS-6808 String constants pushed up */
    private static final String CSV_COL_HDR = DQ + "1553 Log";

	private static final StringBuilder csv = new StringBuilder(1024);
	
    private final SclkFormatter sclkFmt;

	/**
     * @param appContext
     *            the Spring Aplication Context
     */
	public Log1553(final ApplicationContext appContext) {
		super(appContext);
		sclkFmt = TimeProperties.getInstance().getSclkFormatter();
	}


	/**
     * {@inheritDoc}
	 */
    @Override
	public Map<String, String> getFileData(final String NO_DATA) {

		final DateFormat df = TimeUtility.getFormatterFromPool();
		final Map<String,String> map = new HashMap<String,String>();
		
		map.put("sysTime", sysTime != null ? df.format(sysTime) : NO_DATA);
		map.put("sclk", sclk != null ? sclk.toString() : NO_DATA);
		map.put("bus", bus != null ? bus : NO_DATA);
		map.put("remoteTerminal", remoteTerminal != null ? remoteTerminal : NO_DATA);
		map.put("subAddress", subAddress != null ? subAddress : NO_DATA);
		map.put("transmitReceiveStatus", transmitReceiveStatus != null ? transmitReceiveStatus : NO_DATA);
		map.put("data", data != null ? data : NO_DATA);

		TimeUtility.releaseFormatterToPool(df);
		
		return map;
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
        final DateFormat df = TimeUtility.getFormatterFromPool();

        csv.setLength(0);

		csv.append(CSV_COL_HDR);

        for (final String cce : csvColumns)
        {
            final String upcce = cce.toUpperCase();

            csv.append(CSV_COL_SEP);

            switch (upcce)
            {
                case "SESSIONID":
                    // Skip session id
                    break;

                case "SESSIONHOST":
                    // Skip session host
                    break;

                case "SYSTIME":
                   if (sysTime != null)
                   {
                       csv.append(df.format(sysTime));
                   }
                   break;

                case "SCLK":
                   if (sclk != null)
                   {
                       csv.append(sclk);
                   }
                   break;

                case "BUS":
                   if (bus != null)
                   {
                       csv.append(bus);
                   }
                   break;

                case "REMOTETERMINAL":
                   if (remoteTerminal != null)
                   {
                       csv.append(remoteTerminal);
                   }
                   break;

                case "SUBADDRESS":
                   if (subAddress != null)
                   {
                       csv.append(subAddress);
                   }
                   break;

                case "TRANSMITRECEIVESTATUS":
                   if (transmitReceiveStatus != null)
                   {
                       csv.append(transmitReceiveStatus);
                   }
                   break;

                case "DATA":
                   if (data != null)
                   {
                       csv.append(data);
                   }
                   break;

                default:

                    if (! csvSkip.contains(upcce))
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
		
		TimeUtility.releaseFormatterToPool(df);

		return csv.toString();
	}

	
	/**
     * {@inheritDoc}
     *
	 */
    @Override
	public void parseCsv(final String       csvStr,
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

        sessionId             = null;
        sessionHost           = null;
        sysTime               = null;
        sclk                  = null;
        bus                   = null;
        remoteTerminal        = null;
        subAddress            = null;
        transmitReceiveStatus = null;
        data                  = null;

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
                        // No action
                        break;

                    case "SESSIONHOST":
                        // No action
                        break; 

                    case "SYSTIME":
                        sysTime = new AccurateDateTime(token);
                        break; 

                    case "SCLK":
                        sclk = sclkFmt.valueOf(token);
                        break; 

                    case "BUS":
                        bus = token;
                        break; 

                    case "REMOTETERMINAL":
                        remoteTerminal = token;
                        break; 

                    case "SUBADDRESS":
                        subAddress = token;
                        break; 

                    case "TRANSMITRECEIVESTATUS":
                        transmitReceiveStatus = token;
                        break; 

                    case "DATA":
                        data = token;
                        break;

                    default:
                        if (! csvSkip.contains(upcce))
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
    public void parseCsvCD1553(final String csvStr) {
		
		// The following removes the start/end quotes w/ the substring
		// and splits based on ",".  It leaves the trailing empty string in the case that 
		// csvStr ends with "".  The empty strings server as place holders.
		final String[] dataArray = csvStr.substring(1, csvStr.length()-1).split("\",\"",-1);
		
		try {
			if (dataArray[0].length() != 0) {
				sysTime = new AccurateDateTime(dataArray[0]);
			}
			if (dataArray[1].length() != 0) {
				sclk = sclkFmt.valueOf(dataArray[1]);
			}
			if (dataArray[2].length() != 0) {
				bus = dataArray[2];
			}
			if (dataArray[3].length() != 0) {
				remoteTerminal = dataArray[3];
			}
			if (dataArray[4].length() != 0) {
				subAddress = dataArray[4];
			}
			if (dataArray[5].length() != 0) {
				transmitReceiveStatus = dataArray[5];
			}
	    	final StringBuilder sb = new StringBuilder();
	    	final int length = dataArray.length;
	    	for (int k = 6; k < length; k++) {
	    		final String str = dataArray[k];
	    		if (k == length-1 && str.length() != 0) {
	    			sb.append(str);
	    		} else if (str.length() != 0) {
	    			sb.append(str + " ");
	    		}
	    	}
	    	data = sb.toString();

		}
		catch (final java.text.ParseException e) {
			//e.printStackTrace();
		}
	}
	
	/**
     * {@inheritDoc}
     */
	@Override
    public IAccurateDateTime getSystemTime() {
		return sysTime;
	}


	/**
     * {@inheritDoc}
	 * @see jpl.gds.db.api.types.IDbQueryable#getRecordBytes()
	 */
    @Override
	public byte[] getRecordBytes() {
		return null;
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.db.api.types.IDbQueryable#getRecordOffset()
	 */
    @Override
	public Long getRecordOffset() {
		return null;
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.db.api.types.IDbQueryable#getSessionHost()
	 */
    @Override
	public String getSessionHost() {
		return null;
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.db.api.types.IDbQueryable#getSessionId()
	 */
    @Override
	public Long getSessionId() {
		return null;
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.db.api.types.IDbQueryable#setRecordBytes(byte[])
	 */
    @Override
	public void setRecordBytes(final byte[] bytes) {
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.db.api.types.IDbQueryable#setRecordOffset(java.lang.Long)
	 */
    @Override
	public void setRecordOffset(final Long offset) {
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.db.api.types.IDbQueryable#setSessionHost(java.lang.String)
	 */
    @Override
	public void setSessionHost(final String sessionHost) {
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.db.api.types.IDbQueryable#setSessionId(java.lang.Long)
	 */
    @Override
	public void setSessionId(final Long sessionId) {
	}

	/**
     * {@inheritDoc}
	 */
    @Override
	public void setTemplateContext(final Map<String,Object> map) {
	}


    /**
     * Get session fragment.
     *
     * @return Session fragment
     */	
    @Override
	public SessionFragmentHolder getSessionFragment()
    {
        return SessionFragmentHolder.MINIMUM;
    }


    /**
     * Set session fragment.
     *
     * @param fragment Session fragment
     */
    @Override
	public void setSessionFragment(final SessionFragmentHolder fragment)
    {
        // Do nothing
    }
}
