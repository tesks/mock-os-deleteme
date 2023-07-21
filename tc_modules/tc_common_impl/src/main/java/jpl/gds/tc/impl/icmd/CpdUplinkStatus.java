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

package jpl.gds.tc.impl.icmd;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import gov.nasa.jpl.icmd.schema.ICmdRadiationStatusType;
import gov.nasa.jpl.icmd.schema.ICmdUplinkStateType;
import gov.nasa.jpl.icmd.schema.UplinkRequest;
import jpl.gds.common.config.mission.StationMapper;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.tc.api.CommandStatusType;
import jpl.gds.tc.api.ICpdUplinkStatus;
import jpl.gds.tc.api.IUplinkMetadata;

/**
 * This class represents a command status request to ICMD.
 *
 * @since AMPCS R3
 */
public class CpdUplinkStatus extends Object implements
		Comparable<CpdUplinkStatus>, ICpdUplinkStatus {
    private static final Tracer     log            = TraceManager.getTracer(Loggers.CPD_UPLINK);


	/*
	 * 8/22/13 - MPCS-5205. Added mapping of CPD station string to AMPCS ID.
	 */
	/** Station Map */
	private final StationMapper stationMapper;

	/** ICMD request ID */
	private String id;

	/** Status reported to us by ICMD */
	private CommandStatusType status;

	/** Timestamp of the status from ICMD */
	private IAccurateDateTime timestamp;

	/** String representation of the timestamp */
	private String timestampStr;

	/** Station id */
	private Integer dssId = null;

	/** Radiation start time */
    private IAccurateDateTime bit1RadTime    = null;

	/** String representation of the bit 1 radiation time */
	private String bit1RadTimeStr;

	/** Radiation end time */
    private IAccurateDateTime lastBitRadTime = null;

	/** String representation of the last bit radiation time */
	private String lastBitRadTimeStr;

	/** SCMF checksum */
	private String checksum = null;

	/** Total CLTUs in request */
	private int totalCltus = 0;

	/** SCMF creation time */
	private String scmfCreationTime;

	/** Command dictionary version */
	private String commandDictVer;

	/** Uplink metadata */
	private IUplinkMetadata metadata = null;

	/**
	 * Other properties from the CPD's uplink request object that is needed for
	 * GUI
	 */
	private String filename;
	private final Map<Float, Float> bitrateRadDurationMap;
	private final String userId;
	private final String roleId;
	private final String submitTimeStr;
	private final String includedInExeListStr;


	public CpdUplinkStatus(final StationMapper stationMap) {
		this(stationMap, "", CommandStatusType.UNKNOWN, new AccurateDateTime(), "",
				new ArrayList<Float>(), new ArrayList<Float>(), "", "", "", "",
				"", "", -1, null, null);
	}

	/**
	 * Constructs a new CpdUplinkStatus from an UplinkRequest object.
	 *
	 * @param ur UplinkRequest object to populate values from
	 */
	public CpdUplinkStatus(final StationMapper stationMap, final UplinkRequest ur) {
		
		this.stationMapper = stationMap;
		
		this.id = ur.getREQUESTID();

		// TODO according to William, we're assuming the status list
		// contains at most 1 element, and if not, we just use the
		// last element as the latest status. verify this!
		final List<ICmdRadiationStatusType> autoradStatusList = ur.getSTATUSLIST()
				.getSTATUS();
		final ICmdRadiationStatusType radStatus = autoradStatusList
				.get(autoradStatusList.size() - 1);
		final ICmdUplinkStateType status = radStatus.getSTATE();

		// Convert blanks and dashes to underscores to match the enum values
		this.status = CommandStatusType.valueOfIgnoreCase(status.toString()
				.replaceAll("[- ]", "_"));

		final String updateTimeStr = radStatus.getUPDATETIME();
		final String bit1RadTimeStr = radStatus.getBIT1RADTIME();
		final String lastBitRadTimeStr = radStatus.getLASTBITRADTIME();
		final DateFormat df = TimeUtility.getDoyFormatterFromPool();

		try {
            this.timestamp = new AccurateDateTime(df.parse(updateTimeStr));
		} catch (final ParseException e) {
			log.error("Unable to convert status update timestamp ("
					+ updateTimeStr
					+ ") from UplinkRequest object; setting to current time");
			this.timestamp = new AccurateDateTime();
		}

		if (bit1RadTimeStr != null) {
			try {
                this.bit1RadTime = new AccurateDateTime(df.parse(bit1RadTimeStr));
			} catch (final ParseException e) {
				log.warn("Unable to convert bit 1 radiation timestamp ("
						+ bit1RadTimeStr + ") from UplinkRequest object");
			}
		}

		if (lastBitRadTimeStr != null) {
			try {
                this.lastBitRadTime = new AccurateDateTime(df.parse(lastBitRadTimeStr));
			} catch (final ParseException e) {
				log.warn("Unable to convert last bit radiation timestamp ("
						+ lastBitRadTimeStr + ") from UplinkRequest object");
			}
		}

		TimeUtility.releaseDoyFormatterToPool(df);

		if (ur.getFILEINFO() != null) {
			this.filename = ur.getFILEINFO().getFILENAME();

			this.checksum = ur.getFILEINFO().getCHECKSUM();

			if (this.checksum != null) {
				this.checksum = "0x" + this.checksum;
			}

			this.totalCltus = ur.getFILEINFO().getTOTALCLTUS() != null ? ur
					.getFILEINFO().getTOTALCLTUS().intValue() : -1;

			this.scmfCreationTime = ur.getFILEINFO().getCREATIONTIME();
		}

		this.commandDictVer = ur.getXLATEDICTIONARYVERSION();

		final List<Float> bitRatesList = ur.getBITRATERANGE() != null ? ur
				.getBITRATERANGE().getBITRATE() : null;
		final List<Float> radDurationsList = ur.getRADIATIONDURATIONRANGE() != null ? ur
				.getRADIATIONDURATIONRANGE().getDURATION() : null;

		this.bitrateRadDurationMap = CpdUplinkStatus.getBitRateRadDurationMap(
				bitRatesList, radDurationsList);

		this.userId = ur.getUSERID();
		this.roleId = ur.getROLEID();
		this.submitTimeStr = ur.getSUBMITTIME();
		this.includedInExeListStr = ur.getINCLUDEDINEXELIST();

		if (ur.getSENDERSOURCE() != null) {
			this.metadata = new UplinkMetadata(ur.getSENDERSOURCE()
					.getSENDERID());
		} else {
			log.warn("Did not receive AMPCS metadata from CPD poll results");
			this.metadata = new UplinkMetadata(-1, -1, "UNKNOWN", -1);
		}

		/*
		 * 8/22/13 - MPCS-5205. Added mapping of CPD station string to AMPCS ID.
		 */
		final String stationStr = radStatus.getSTATIONID();
		if (stationStr == null || stationStr.isEmpty()) {
			this.dssId = StationIdHolder.UNSPECIFIED_VALUE;
		} else {
			this.dssId = stationMapper.getStationId(stationStr);
		}
	}

	/*
	 * MPCS-7355 - 6/1/2015: Copy constructor created to be able to
	 * generate faux CpdUplinkStatus objects.
	 */
	/**
	 * Copy constructor. Performs a shallow copy.
	 *
	 * @param cus original CpdUplinkStatus to copy from
	 */
	public CpdUplinkStatus(final StationMapper stationMap, final ICpdUplinkStatus cus) {
		this(stationMap, cus.getId(), cus.getStatus(), cus.getTimestamp(), cus
				.getFilename(), cus.getBitrates(), cus.getEstRadDurations(),
				cus.getUserId(), cus.getRoleId(), cus.getSubmitTime(), cus
						.getIncludedInExeList(), cus.getUplinkMetadata()
						.toMetadataString(), cus.getChecksum(), cus
						.getTotalCltus(), cus.getBit1RadTime(), cus
						.getLastBitRadTime());
	}

	/**
	 * Constructs a new CpdUplinkStatus with values populated.
	 *
	 * @param id request ID of the uplink request to CPD
	 * @param status status of the request
	 * @param timestamp timestamp of the status change
	 * @param filename filename from uplink request status's file info property
	 * @param bitrates list of bitrates from uplink request status's bit rate
	 *            range property
	 * @param estRadDurations list of estimated radiation durations
	 *            corresponding to each bit rate in bitrates list, respectively
	 * @param userId user ID property from uplink request status
	 * @param roleId role ID property from uplink request status
	 * @param submitTimeStr uplink request status's submit time property
	 * @param includedInExeListStr uplink request statu's includedinexelist
	 *            property
	 * @param uplinkMetadataString the metadata string generated by an
	 *            UplinkMetadata instance
	 * @param checksum the SCMF checksum, in hex
	 * @param totalCltus the total CLTUs
	 * @param bit1RadTime the start radiation time
	 * @param lastBitRadTime the end radiation time
	 */
	public CpdUplinkStatus(final StationMapper stationMap, 
			final String id, final CommandStatusType status,
            final IAccurateDateTime timestamp, final String filename,
			final List<Float> bitrates, final List<Float> estRadDurations,
			final String userId, final String roleId,
			final String submitTimeStr, final String includedInExeListStr,
			final String uplinkMetadataString, final String checksum,
            final int totalCltus, final IAccurateDateTime bit1RadTime, final IAccurateDateTime lastBitRadTime) {
		super();
		
		this.stationMapper = stationMap;

		this.id = id;
		this.status = status;
        this.timestamp = new AccurateDateTime(timestamp.getTime());
		this.filename = filename;

		final List<Float> bitRatesList = bitrates != null ? new ArrayList<Float>(
				bitrates) : null;
		final List<Float> radDurationsList = estRadDurations != null ? new ArrayList<Float>(
				estRadDurations) : null;

		this.bitrateRadDurationMap = CpdUplinkStatus.getBitRateRadDurationMap(
				bitRatesList, radDurationsList);

		this.userId = userId;
		this.roleId = roleId;
		this.submitTimeStr = submitTimeStr;
		this.includedInExeListStr = includedInExeListStr;
		this.metadata = new UplinkMetadata(uplinkMetadataString);
		this.checksum = checksum;
		this.totalCltus = totalCltus;
		this.bit1RadTime = bit1RadTime;
		this.lastBitRadTime = lastBitRadTime;
	}

	private static Map<Float, Float> getBitRateRadDurationMap(
			final List<Float> bitRates, final List<Float> estRadDurations) {
		final Map<Float, Float> resultingMap = new LinkedHashMap<Float, Float>();

		if (bitRates == null || estRadDurations == null) {
			return resultingMap;
		}

		// go backwards because if radiation duration could not be calculated,
		// it is usually a result of a high bit rate
		int i = estRadDurations.size() - 1;
		int j = bitRates.size() - 1;

		while (i >= 0) {
			resultingMap.put(bitRates.get(j), estRadDurations.get(i));

			i--;
			j--;
		}

		return resultingMap;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICpdUplinkStatus#getId()
     */
	@Override
    public String getId() {
		return id;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICpdUplinkStatus#getStatus()
     */
	@Override
    public CommandStatusType getStatus() {
		return status;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICpdUplinkStatus#getTimestamp()
     */
	@Override
    public IAccurateDateTime getTimestamp() {
		return timestamp;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICpdUplinkStatus#getTimestampString()
     */
	@Override
    public String getTimestampString() {
		if (timestampStr == null && timestamp != null) {
			timestampStr = getTimeString(timestamp);
		}

		return timestampStr;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICpdUplinkStatus#getBit1RadTimeString()
     */
	@Override
    public String getBit1RadTimeString() {
		if (bit1RadTimeStr == null && bit1RadTime != null) {
			bit1RadTimeStr = getTimeString(bit1RadTime);
		}

		return bit1RadTimeStr;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICpdUplinkStatus#getLastBitRadTimeString()
     */
	@Override
    public String getLastBitRadTimeString() {
		if (lastBitRadTimeStr == null && lastBitRadTime != null) {
			lastBitRadTimeStr = getTimeString(lastBitRadTime);
		}

		return lastBitRadTimeStr;
	}

    private String getTimeString(final IAccurateDateTime time) {
		final DateFormat df = TimeUtility.getFormatterFromPool();
		final String timeStr = df.format(time);
		TimeUtility.releaseFormatterToPool(df);

		return timeStr;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICpdUplinkStatus#setTimestamp(java.util.Date)
     */
	@Override
    public void setTimestamp(final IAccurateDateTime timestamp) {
		this.timestamp = timestamp;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICpdUplinkStatus#getFilename()
     */
	@Override
    public String getFilename() {
		return filename;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICpdUplinkStatus#getBitrates()
     */
	@Override
    public List<Float> getBitrates() {
		return new ArrayList<Float>(this.bitrateRadDurationMap.keySet());
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICpdUplinkStatus#getEstRadDurations()
     */
	@Override
    public List<Float> getEstRadDurations() {
		return new ArrayList<Float>(this.bitrateRadDurationMap.values());
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICpdUplinkStatus#getUserId()
     */
	@Override
    public String getUserId() {
		return userId;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICpdUplinkStatus#getRoleId()
     */
	@Override
    public String getRoleId() {
		return roleId;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICpdUplinkStatus#getSubmitTime()
     */
	@Override
    public String getSubmitTime() {
		return submitTimeStr;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICpdUplinkStatus#getIncludedInExeList()
     */
	@Override
    public String getIncludedInExeList() {
		return includedInExeListStr;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICpdUplinkStatus#getDssId()
     */
	@Override
    public Integer getDssId() {
		return dssId;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICpdUplinkStatus#getBit1RadTime()
     */
	@Override
    public IAccurateDateTime getBit1RadTime() {
        return ((bit1RadTime != null) ? new AccurateDateTime(bit1RadTime.getTime()) : null);
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICpdUplinkStatus#getLastBitRadTime()
     */
	@Override
    public IAccurateDateTime getLastBitRadTime() {
        return ((lastBitRadTime != null) ? new AccurateDateTime(lastBitRadTime.getTime()) : null);
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICpdUplinkStatus#getChecksum()
     */
	@Override
    public String getChecksum() {
		return checksum;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICpdUplinkStatus#getTotalCltus()
     */
	@Override
    public int getTotalCltus() {
		return totalCltus;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICpdUplinkStatus#setId(java.lang.String)
     */
	@Override
    public void setId(final String id) {
		this.id = id;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICpdUplinkStatus#setStatus(jpl.gds.tc.api.CommandStatusType)
     */
	@Override
    public void setStatus(final CommandStatusType status) {
		this.status = status;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICpdUplinkStatus#setFilename(java.lang.String)
     */
	@Override
    public void setFilename(final String filename) {
		this.filename = filename;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICpdUplinkStatus#getUplinkMetadata()
     */
	@Override
    public IUplinkMetadata getUplinkMetadata() {
		return this.metadata;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICpdUplinkStatus#setUplinkMetadata(jpl.gds.tc.impl.icmd.UplinkMetadata)
     */
	@Override
    public void setUplinkMetadata(final IUplinkMetadata um) {
		this.metadata = um;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICpdUplinkStatus#getRadiationDurationString(float)
     */
	@Override
    public String getRadiationDurationString(final float bitRate) {
		if (this.bitrateRadDurationMap.containsKey(bitRate)) {
			final float radDurationSecs = this.bitrateRadDurationMap.get(bitRate);
			final long radDurationMillis = (long) (radDurationSecs * 1000);

			final Calendar cal = new GregorianCalendar();

			cal.setTimeInMillis(radDurationMillis);

			final StringBuilder sb = new StringBuilder();

			final int hours = (cal.get(Calendar.DAY_OF_YEAR) - 1) * 24;

			sb.append(String.format("%02d", hours + cal.get(Calendar.HOUR_OF_DAY)));
			sb.append(":");

			sb.append(String.format("%02d", cal.get(Calendar.MINUTE)));
			sb.append(":");

			sb.append(String.format("%02d", cal.get(Calendar.SECOND)));
			sb.append(".");

			int milliSecs = cal.get(Calendar.MILLISECOND);
			if (milliSecs == 0) {
				milliSecs++;
			}

			final String milliSecStr = String.format("%03d", milliSecs);
			sb.append(milliSecStr);

			return sb.toString();
		} else {
			return null;
		}
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICpdUplinkStatus#getScmfCreationTime()
     */
	@Override
    public String getScmfCreationTime() {
		return scmfCreationTime;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICpdUplinkStatus#setScmfCreationTime(java.lang.String)
     */
	@Override
    public void setScmfCreationTime(final String scmfCreationTime) {
		this.scmfCreationTime = scmfCreationTime;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICpdUplinkStatus#getCommandDictVer()
     */
	@Override
    public String getCommandDictVer() {
		return commandDictVer;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICpdUplinkStatus#setCommandDictVer(java.lang.String)
     */
	@Override
    public void setCommandDictVer(final String commandDictVer) {
		this.commandDictVer = commandDictVer;
	}

	/**
	 * Comparison operator.
	 *
	 * @param other Object to compare against
	 *
	 * @return Compare status
	 */
    @Override
    public int compareTo(final CpdUplinkStatus other) {
		final int result = getId().compareTo(other.getId());

		if (result != 0) {
			return result;
		}

		// No timestamp

		return getStatus().toString().compareTo(other.getStatus().toString());
	}

	/**
	 * Equals operator.
	 *
	 * @param other Object to compare against
	 *
	 * @return True if equal
	 */
    @Override
    public boolean equals(final Object other) {
		if (this == other) {
			return true;
		}

		if (!(other instanceof CpdUplinkStatus)) {
			return false;
		}

		final ICpdUplinkStatus o = (ICpdUplinkStatus) other;

		if (!getId().equals(o.getId())) {
			return false;
		}

		if (this.getEstRadDurations() != null
				&& !this.getEstRadDurations().equals(o.getEstRadDurations())) {
			return false;
		}

		if (this.getBit1RadTime() != null
				&& !this.getBit1RadTime().equals(o.getBit1RadTime())) {
			return false;
		}

		if (this.getTotalCltus() != o.getTotalCltus()) {
			return false;
		}

		// Timestamp is not really part of the useful value

		return getStatus().equals(o.getStatus());
	}

	/**
	 * Hashcode operator.
	 *
	 * @return Calculated hash code
	 */
    @Override
    public int hashCode() {
		return (getId().hashCode() + getTimestamp().hashCode() + getStatus()
				.hashCode());
	}
}
