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
package jpl.gds.monitor.perspective.view.channel;

import java.util.Date;

import jpl.gds.dictionary.api.channel.ChannelDefinitionType;
import jpl.gds.shared.sys.SystemUtilities;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;

/**
 * This class tracks latest channelized telemetry timestamps in the monitor. It is used 
 * by the MonitorChannelLad.
 *
 */
public class LatestChannelTimes {
	
	private ISclk latestSclk = null;
	private IAccurateDateTime latestErt = null;
	private Date latestRct = null;
	private IAccurateDateTime latestScet = null;
	private ILocalSolarTime latestSol = null;
	private IAccurateDateTime latestMst = null;
	private long timestamp = 0;

	/**
	 * Basic constructor.
	 */
	public LatestChannelTimes() {
	    SystemUtilities.doNothing();
	}
	
	/**
	 * Copy constructor. Uses shallow copy.
	 * @param toCopy the LatestChannelTimes object to copy
	 */
	public LatestChannelTimes(final LatestChannelTimes toCopy) {
	    this.latestErt = toCopy.latestErt;
	    this.latestMst = toCopy.latestMst;
	    this.latestRct = toCopy.latestRct;
	    this.latestScet = toCopy.latestScet;
	    this.latestSclk = toCopy.latestSclk;
	    this.latestSol = toCopy.latestSol;
	    this.timestamp = toCopy.timestamp;    
	}
	
	/**
	 * Update the times from a received telemetry value.
	 * 
	 * @param data ChannelSample object contained the telemetry value
	 * @param type ChannelDefinitionType of the received channel
	 */
	public void updateFromChannelValue(final MonitorChannelSample data, final ChannelDefinitionType type) {
		final IAccurateDateTime ert = data.getErt();
		if (latestErt == null || latestErt.compareTo(ert) <= 0) {
			latestErt = ert;
			latestRct = data.getRct();
			if (type == null || type.equals(ChannelDefinitionType.FSW) || type.equals(ChannelDefinitionType.SSE)) {
				latestSclk = data.getSclk();
				latestScet = data.getScet();
				latestSol = data.getSol();
			} else if (type.equals(ChannelDefinitionType.M)) {
				latestMst = ert;
			}
		}
		timestamp = getLatestUtc();
	}

	/**
	 * Just updates the latest timestamp on this object.
	 */
	public void updateTimestamp() {
		timestamp = getLatestUtc();
	}

	/**
	 * Clears all times.
	 */
	public void clear() {
		latestSclk = null;
		latestErt = null;
		latestRct = null;
		latestScet = null;
		latestSol = null;
		latestMst = null;
		timestamp = 0;
	}

	/**
	 * Gets the timestamp indicating when this object was last updated.
	 * 
	 * @return timestamp as milliseconds
	 */
	public long getTimestamp() {
		return timestamp;
	}

	/**
	 * Gets the latest SCLK value.
	 * 
	 * @return ISclk value, or null if none yet received.
	 */
    public ISclk getLatestSclk() {
		return latestSclk;
	}

	/**
	 * Gets the latest ERT value.
	 * 
	 * @return Date value, or null if no ERT yet received.
	 */
	public IAccurateDateTime getLatestErt() {
		return latestErt;
	}

	/**
	 * Gets the latest RCT value.
	 * 
	 * @return Date value, or null if no RCT yet received.
	 */
	public Date getLatestRct() {
	    if (latestRct == null) {
	        return null;
	    }
		return new Date(latestRct.getTime());
	}

	/**
	 * Gets the latest SCET value.
	 * 
	 * @return Date value, or null if no SCET yet received.
	 */
	public IAccurateDateTime getLatestScet() {
		return latestScet;
	}

	/**
	 * Gets the latest LST value.
	 * @return ILocalSolarTime value, or null if no SOL yet received.
	 */
	public ILocalSolarTime getLatestSol() {
		return latestSol;
	}
	/**
	 * Gets the latest MST value.
	 * 
	 * @return Date value, or null if no MST yet received.
	 */
	public IAccurateDateTime getLatestMst() {
		return latestMst;
	}

	/**
	 * Gets the latest UTC value as milliseconds, which is basically just the system time.
	 * 
	 * @return long value in milliseconds
	 */
	public long getLatestUtc() {
		return System.currentTimeMillis();
	}

	/**
	 * Gets the latest UTC value.
	 * 
	 * @return Date value
	 */
	public Date getLatestUtcAsDate() {
		return new Date(getLatestUtc());
	}
}
