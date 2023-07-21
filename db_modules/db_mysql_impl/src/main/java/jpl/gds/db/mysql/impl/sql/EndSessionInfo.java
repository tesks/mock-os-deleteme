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
/**
 * 
 */
package jpl.gds.db.mysql.impl.sql;

import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ICoarseFineTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.Sclk;

/**
 * Object that holds some of the information that will be populated in the EndSession table.  
 * Specifically, it keeps track of the ISclk start/end times, Ert start/end times, Scet start/end times,
 * and counters for evr and chanvals.
 * 
 * Uses a singleton design pattern.
 * 
 * Note, that parsing the log message is not needed b/c the SessionSummary class has all this info.
 *
 */
public class EndSessionInfo
{
    private static final ISclk             ZERO_SCLK = new Sclk(0L);
    private static final IAccurateDateTime ZERO_ADT  = new AccurateDateTime(0L);
	
//	private BaseMessageHandler handler;
//	private MessageContext msgContext;
	
	private static EndSessionInfo instance;
	
	/**
	 * Objects to keep track of the lastest/earliest times
	 */
    private ICoarseFineTime  latestSclk      = null;
	private IAccurateDateTime latestErt    = null;
	private IAccurateDateTime latestScet   = null;
	private ICoarseFineTime  earliestSclk = null;
	private IAccurateDateTime earliestErt  = null;		
	private IAccurateDateTime earliestScet = null;
	
	private ISclk             latestSseSclk   = null;
	private IAccurateDateTime latestSseErt    = null;		
	private IAccurateDateTime latestSseScet   = null;
	private ISclk             earliestSseSclk = null;
	private IAccurateDateTime earliestSseErt  = null;	
	private IAccurateDateTime earliestSseScet = null;
	
	/**
	 * Counters for evrs and chanvals
	 */
	private int evrCount;
	private int chanValCount;
	
//	/**
//	 * Mapping of data for the EndSession table.  Maps the EndSession field name to the data to be stored.
//	 */
//	private Map<String,Object> dataMap;
	
	/**
	 * Returns the global, shared instance of EndSessionInfo
	 *
	 * @return the static instance of EndSessionInfo
	 */
	public static EndSessionInfo getInstance() {
		if (instance == null) {
			instance = new EndSessionInfo();
		}
		return instance;
	}
	
	/**
	 * Constructor
	 */
	protected EndSessionInfo() {
		
//		dataMap = new HashMap<String,Object>();
//		
//	    this.handler = new BaseMessageHandler()
//        {
//            @Override
//            public void handleMessage(final Message m)
//            {
//                handleLogMessage((LogMessage) m);
//            }
//        };
//
//		// Subscribe to Log Messages on the internal bus
//		msgContext = MessageContext.getInstance();
//		msgContext.subscribe(LogMessage.TYPE, handler);
					
	}
	
	/**
	 * Update the Ert times
     *
     * @param ert Latest ERT
	 */
	public void updateErt(final IAccurateDateTime ert) {
		
		if (latestErt == null || latestErt.compareTo(ert) <= 0) {
			latestErt = ert;
		}
		if (earliestErt == null || earliestErt.compareTo(ert) >= 0) {
			earliestErt = ert;
		}
	}
	
	/**
	 * Update the Scet times
     *
     * @param scet Latest SCET
	 */
	public void updateScet(final IAccurateDateTime scet) {
		
		if (latestScet == null || latestScet.compareTo(scet) <= 0) {
			latestScet = scet;
		}
		if (earliestScet == null || earliestScet.compareTo(scet) >= 0) {
			earliestScet = scet;
		}
	}
	
	/**
	 * Update the ISclk times
     *
     * @param sclk Latest SCLK
	 */
    public void updateSclk(final ICoarseFineTime sclk) {
		
		if (latestSclk == null || latestSclk.compareTo(sclk) <= 0) {
			latestSclk = sclk;
		}
		if (earliestSclk == null || earliestSclk.compareTo(sclk) >= 0) {
			earliestSclk = sclk;
		}
	}
	
	/**
	 * Update the SSE Ert times
     *
     * @param ert Latest ERT
	 */
	public void updateSseErt(final IAccurateDateTime ert) {
		
		if (latestSseErt == null || latestSseErt.compareTo(ert) <= 0) {
			latestSseErt = ert;
		}
		if (earliestSseErt == null || earliestSseErt.compareTo(ert) >= 0) {
			earliestSseErt = ert;
		}
	}
	
	/**
	 * Update the SSE Scet times
     *
     * @param scet Latest SET
	 */
	public void updateSseScet(final IAccurateDateTime scet) {
		
		if (latestSseScet == null || latestSseScet.compareTo(scet) <= 0) {
			latestSseScet = scet;
		}
		if (earliestSseScet == null || earliestSseScet.compareTo(scet) >= 0) {
			earliestSseScet = scet;
		}
	}
	

	/**
	 * Update the SSE ISclk times
     *
     * @param sclk Latest SCLK
	 */
	public void updateSseSclk(final ISclk sclk) {
		
		if (latestSseSclk == null || latestSseSclk.compareTo(sclk) <= 0) {
			latestSseSclk = sclk;
		}
		if (earliestSseSclk == null || earliestSseSclk.compareTo(sclk) >= 0) {
			earliestSseSclk = sclk;
		}
	}
	
	/**
	 * Updates all of times that are non-null
	 * @param ert  ERT
	 * @param scet SCET
	 * @param sclk SCLK
	 */
    public void updateTimes(final IAccurateDateTime ert, final IAccurateDateTime scet, final ICoarseFineTime sclk) {
		
		if (ert != null) {
			updateErt(ert);
		}
		if (scet != null) {
			updateScet(scet);
		}
		if (sclk != null) {
			updateSclk(sclk);
		}
	}
	
	/**
	 * Updates all of SSE times that are non-null
	 * @param ert  ERT
	 * @param scet SCET
	 * @param sclk SCLK
	 */
	public void updateTimesSse(final IAccurateDateTime ert, final IAccurateDateTime scet, final ISclk sclk) {
		
		if (ert != null) {
			updateSseErt(ert);
		}
		if (scet != null) {
			updateSseScet(scet);
		}
		if (sclk != null) {
			updateSseSclk(sclk);
		}
	}
	

    /**
     * Get EVR count.
     *
     * @return Count
     */	
	public int getEvrCnt() {
		return evrCount;
	}
	

    /**
     * Get channel count.
     *
     * @return Count
     */	
	public int getChanCnt() {
		 return chanValCount;
	}
	

    /**
     * Update EVR count.
     */	
	public void updateEvrCnt() {
		evrCount++;
	}
	

    /**
     * Update channel count.
     */	
	public void updateChanCnt() {
		chanValCount++;
	}
	

    /**
     * Get latest ERT.
     *
     * @return ERT
     */	
	public IAccurateDateTime getLatestErt()
    {
        if (latestErt == null)
        {
            return ZERO_ADT;
        }

		return latestErt;
	}
	

    /**
     * Get earliest ERT.
     *
     * @return ERT
     */	
	public IAccurateDateTime getEarliestErt()
    {
        if (earliestErt == null)
        {
            return ZERO_ADT;
        }

		return earliestErt;
	}
	

    /**
     * Get latest SCET
     *
     * @return SCET
     */	
	public IAccurateDateTime getLatestScet()
    {
        if (latestScet == null)
        {
            return ZERO_ADT;
        }

		return latestScet;
	}
	

    /**
     * Get earliest SCET.
     *
     * @return SCET
     */	
	public IAccurateDateTime getEarliestScet()
    {
        if (earliestScet == null)
        {
            return ZERO_ADT;
        }

		return earliestScet;
	}
	

    /**
     * Get latest SCLK.
     *
     * @return SCLK
     */	
	public ICoarseFineTime getLatestSclk()
    {
        if (latestSclk == null)
        {
            return ZERO_SCLK;
        }

		return latestSclk;
	}
	

    /**
     * Get earliest SCLK.
     *
     * @return SCLK
     */	
	public ICoarseFineTime getEarliestSclk()
    {
        if (earliestSclk == null)
        {
            return ZERO_SCLK;
        }

		return earliestSclk;
	}
	

    /**
     * Get latest SSE ERT.
     *
     * @return ERT
     */	
	public IAccurateDateTime getLatestSseErt()
    {
        if (latestSseErt == null)
        {
            return ZERO_ADT;
        }

		return latestSseErt;
	}
	

    /**
     * Get earliest SSE ERT.
     *
     * @return ERT
     */	
	public IAccurateDateTime getEarliestSseErt()
    {
        if (earliestSseErt == null)
        {
            return ZERO_ADT;
        }

		return earliestSseErt;
	}
	

    /**
     * Get latest SSE SCET.
     *
     * @return SCET
     */	
	public IAccurateDateTime getLatestSseScet()
    {
        if (latestSseScet == null)
        {
            return ZERO_ADT;
        }

		return latestSseScet;
	}
	

    /**
     * Get earliest SSE SCET.
     *
     * @return SCET
     */	
	public IAccurateDateTime getEarliestSseScet()
    {
        if (earliestSseScet == null)
        {
            return ZERO_ADT;
        }

		return earliestSseScet;
	}
	


    /**
     * Get latest SSE SCLK.
     *
     * @return SCLK
     */	
	public ISclk getLatestSseSclk()
    {
        if (latestSseSclk == null)
        {
            return ZERO_SCLK;
        }

		return latestSseSclk;
	}


    /**
     * Get earliest SSE SCLK.
     *
     * @return SCLK
     */	
	public ISclk getEarliestSseSclk()
    {
        if (earliestSseSclk == null)
        {
            return ZERO_SCLK;
        }

		return earliestSseSclk;
	}

	// Not needed, SessionSummary class handles this:
//	/**
//	 * Add to the dataMap all the data that is found in the summary field of
//	 * the log messages.
//	 * @param message
//	 */
//	public void handleLogMessage(LogMessage message) {
//		// Only log messsages for now?
//
//		String msg = message.getMessage();
//
//		String[] dataTypes =  {"validFrames","outOfSyncData","outOfSyncCount","frameGaps","frameRegressions","frameRepeats","idleFrames",
//                				"deadFrames","invalidFrames",
//                				"validPackets","evrPackets","ehaPackets","productPackets","invalidPackets","fillPackets","partialProducts",
//                				"completeProducts","productDataBytes"};
//
//		String[] substrCheck = {"In Sync Frames Processed","Out Of Sync Bytes","Out Of Sync Count","Frame Gaps","Frame Regressions",
//                				"Frame Repeats","Idle Frames","Dead Frames","Bad Frames","Valid Packets Processed","EVR Packets",
//                				"EHA Packets","Product Packets","Bad Packets","Fill Packets","Partial Products Generated",
//                				"Complete Products Generated","Product Data Bytes"};
//
//		//String testString = "FSW::SESSION SUMMARY: In Sync Frames Processed: 0, Out Of Sync Bytes: 1, Out Of Sync Count: 2, Frame Gaps: 0, Frame Regressions: 0, Frame Repeats: 0, Idle Frames: 0, Dead Frames: 0, Bad Frames: 0, Valid Packets Processed: 0, EVR Packets: 0, EHA Packets: 0, Product Packets: 0, Bad Packets: 0, Fill Packets: 0, Partial Products Generated: 0, Complete Products Generated: 0, Product Data Bytes: 0";
//
//		if (msg.indexOf("SUMMARY") != -1) {
//
//			String[] dataArray = msg.split(",");
//
//			for(int i = 0; i < dataTypes.length; i++) {
//
//				String data = dataArray[i];
//
//				// Checks if the desired substring is in the message:
//				String substr = substrCheck[i];
//				int index = data.indexOf(substr);
//				if (index != -1) {
//					int length = substr.length();
//					// Add the value to data map after converting it to a long:
//					dataMap.put(dataTypes[i], Long.valueOf(data.substring(index+length+2)));	// +2 b/c of ": " in each string
//				}
//			}
//
//		}
//
//	}


}
