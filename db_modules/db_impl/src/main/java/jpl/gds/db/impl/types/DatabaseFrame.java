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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.db.api.types.IDbFrameProvider;
import jpl.gds.db.api.types.IDbFrameUpdater;
import jpl.gds.shared.holders.PacketIdHolder;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.FastDateFormat;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.station.api.InvalidFrameCode;

/**
 * A representation of a frame as it is stored in the database
 */
public class DatabaseFrame extends AbstractDatabaseItem implements IDbFrameUpdater, IDbFrameProvider {
	private static final Calendar calendar = FastDateFormat.getStandardCalendar();

	private static final StringBuilder csv = new StringBuilder(1024);
	private static final StringBuilder csv2 = new StringBuilder(1024);

	/** MPCS-6808 String constants pushed up */
	private static final String CSV_COL_HDR = DQ + "Frame";

	/**
	 * The DB id of this frame.
	 */
	private Long id;

	/**
	 * The frame type
	 */
	private String type;

	/**
	 * The record creation time
	 */
	private IAccurateDateTime rct = null;

	/**
	 * True if a fill frame
	 */
	private boolean fillFrame = false;

	/**
	 * The earth receive time
	 */
	private IAccurateDateTime ert;

	/**
	 * The relay spacecraft ID
	 */
	private Integer relaySpacecraftId;

	/**
	 * The virtual channel ID
	 */
	private Integer vcid;

	/**
	 * The virtual channel frame counter
	 */
	private Integer vcfc;

	/** SLE header metadata */
	private String sleMetadata;

	/*
	 * MPCS-6349 : DSS ID not set properly Removed dssId.
	 * Parent class has been updated with protected fields sessionDssId and
	 * recordDssId with get/set methods for both.
	 */

	/**
	 * The bit rate when this frame was received by MPCS
	 */
	private Double bitRate;

	/**
	 * The reason the frame is bad.
	 */
	private InvalidFrameCode badReason;

	/** MPCS-6808 Added */
	private static final List<String> csvSkip = new ArrayList<String>(0);

	/**
	 * Constructor.
	 * 
	 * @param appContext
	 *            Spring Application Context
	 */
	public DatabaseFrame(final ApplicationContext appContext) {
		super(appContext);

		id = null;
		type = null;
		ert = null;
		relaySpacecraftId = null;
		vcid = null;
		vcfc = null;
		sleMetadata = null;
		/*
		 * MPCS-6349 : DSS ID not set properly Removed dssId.
		 * Parent class has been updated with protected fields sessionDssId and
		 * recordDssId with get/set methods for both.
		 */
		recordDssId = 0;
		bitRate = null;
		badReason = null;
	}

	/**
	 * Constructor.
	 * 
	 * @param appContext
	 *            Spring Application Context
	 * @param id
	 *            Id
	 * @param type
	 *            Type
	 * @param ert
	 *            ERT
	 * @param relaySpacecraftId
	 *            Relay S/C id
	 * @param vcid
	 *            VCID
	 * @param vcfc
	 *            VCFC
	 * @param dssId
	 *            DSS id
	 * @param bitRate
	 *            Bit rate
	 * @param body
	 *            Body bytes
	 * @param badReason
	 *            Bad reason
	 * @param testSessionId
	 *            Session id
	 * @param sessionHost
	 *            Session host
	 * @param fileByteOffset
	 *            File byte offset
	 * @param fillFrame
	 *            True if fill frame
	 */
	@SuppressWarnings({ "EI_EXPOSE_REP2", "PMD.ExcessiveParameterList" })
	public DatabaseFrame(final ApplicationContext appContext, final Long id, final String type,
			final IAccurateDateTime ert, final Integer relaySpacecraftId, final Integer vcid, final Integer vcfc,
			final Integer dssId, final Double bitRate, final byte[] body, final InvalidFrameCode badReason,
			final Long testSessionId, final String sessionHost, final Long fileByteOffset, final boolean fillFrame,
	        final String sleMetadata) {
		super(appContext, testSessionId, sessionHost);

		this.id = id;
		this.type = type;
		this.ert = ert;
		this.relaySpacecraftId = relaySpacecraftId;
		this.vcid = vcid;
		this.vcfc = vcfc;
		this.sleMetadata = sleMetadata;
		/*
		 * MPCS-6349 : DSS ID not set properly Removed dssId.
		 * Parent class has been updated with protected fields sessionDssId and
		 * recordDssId with get/set methods for both.
		 */
		this.recordDssId = dssId;
		this.bitRate = bitRate;

		setRecordBytes(body);

		this.badReason = badReason;
		recordOffset = fileByteOffset;
		this.fillFrame = fillFrame;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof DatabaseFrame)) {
			return (false);
		}

		final DatabaseFrame frame = (DatabaseFrame) obj;

		/*
		 * MPCS-6349 : DSS ID not set properly Removed dssId.
		 * Parent class has been updated with protected fields sessionDssId and
		 * recordDssId with get/set methods for both.
		 */
		if (this.getBitRate().doubleValue() != frame.getBitRate().doubleValue()
				|| this.getRecordDssId() != frame.getRecordDssId() || this.getRecordLength() != frame.getRecordLength()
				|| this.getRelaySpacecraftId().intValue() != frame.getRelaySpacecraftId().intValue()
				|| this.getVcfc().intValue() != frame.getVcfc().intValue()
				|| this.getVcid().intValue() != frame.getVcid().intValue()) {
			return (false);
		}

		if (this.getErt() == null) {
			if (frame.getErt() != null) {
				return (false);
			}
		} else {
			if (frame.getErt() == null) {
				return (false);
			} else if (!this.getErt().equals(frame.getErt())) {
				return (false);
			}
		}
		if (this.getType() == null) {
			if (frame.getType() != null) {
				return (false);
			}
		} else {
			if (frame.getType() == null) {
				return (false);
			} else if (!this.getType().equals(frame.getType())) {
				return (false);
			}
		}

		// MPCS-5189
		// If both the bodies and the lengths are equal, we are equal.
		// We have to check both because if there are no bodies, the bodies
		// will be zero-length and the lengths may not be.

		if (!Arrays.equals(getRecordBytes(), frame.getRecordBytes())) {
			return false;
		}

		if (getRecordLength() != frame.getRecordLength()) {
			return false;
		}

		return (badReason != null || frame.getBadReason() == null) && (badReason == null || badReason.equals(
				frame.getBadReason()));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		assert false : "hashCode not designed";
		return 42; // any arbitrary constant will do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("  ");

		sb.append(type).append(' ');
		sb.append(ert.getFormattedErt(true)).append(' ');
		sb.append(vcid).append(' ');
		sb.append(vcfc).append(' ');
				/*
				 * MPCS-6349 : DSS ID not set properly Removed dssId.
				 * Parent class has been updated with protected fields sessionDssId and
				 * recordDssId with get/set methods for both.
		 */
		sb.append(recordDssId).append(' ');
		sb.append(relaySpacecraftId).append(' ');
		sb.append(bitRate).append(' ');

		sb.append(getRecordLength());

		return sb.toString();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @version MPCS-6808 Massive rewrite
	 * @version MPCS-7587 Parse named VCID column.
	 */
	@Override
	public String toCsv(final List<String> csvColumns) {
		/*
		 * MPCS-6349 : DSS ID not set properly Removed dssId.
		 * Parent class has been updated with protected fields sessionDssId and
		 * recordDssId with get/set methods for both.
		 */

		csv.setLength(0);

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

			case "SESSIONHOST":
				if (sessionHost != null) {
					csv.append(sessionHost);
				}
				break;

			case "TYPE":
				if (type != null) {
					csv.append(type);
				}
				break;

			case "ERT":
				if (ert != null) {
					csv.append(ert.getFormattedErt(true));
				}
				break;

			case "RELAYSPACECRAFTID":
				if (relaySpacecraftId != null) {
					csv.append(relaySpacecraftId);
				}
				break;

			case "VCID":
				if (vcid != null) {
					csv.append(vcid);
				}
				break;

			case "SOURCEVCFC":
				if (vcfc != null) {
					csv.append(vcfc);
				}
				break;
				
		    case "VCFC":
                if (vcfc != null) {
                    csv.append(vcfc);
                }
                break;

			case "DSSID":
				csv.append(recordDssId); // int
				break;

			case "BITRATE":
				if (bitRate != null) {
					csv.append(bitRate);
				}
				break;

			case "ISBAD":
				csv.append((badReason != null) ? "true" : "false");
				break;

			case "BADREASON":
				if (badReason != null) {
					csv.append(badReason);
				}
				break;

			case "LENGTH":
				csv.append(getRecordLength());
				break;

			case "FILEBYTEOFFSET":
				if (recordOffset != null) {
					csv.append(recordOffset);
				}
				break;

			case "RCT":
				if (rct != null) {
					csv.append(FastDateFormat.format(rct, calendar, csv2));
				}
				break;

			// MPCS-7587  Add named VCID column to csv.
			case "VCIDNAME":
				// MPCS-8021 - updated for better parsing
				if (missionProperties.shouldMapQueryOutputVcid() && vcid != null) {
					csv.append(missionProperties.mapDownlinkVcidToName(this.vcid));
				} else {
					csv.append("");
				}

				break;

			case "SLEMETADATA":
				if (sleMetadata != null) {
					csv.append(sleMetadata);
				}
				break;

			default:

				// MPCS-7587 Add named VCID column to csv.
				// MPCS-8021 - updated for better parsing
				// Put here due to the configurable nature of the column name
				if (missionProperties.getVcidColumnName().toUpperCase().equals(upcce)) {
					if (missionProperties.shouldMapQueryOutputVcid() && vcid != null) {
						csv.append(missionProperties.mapDownlinkVcidToName(this.vcid));
					} else {
						csv.append("");
					}
				} else if (!csvSkip.contains(upcce)) {
					log.warn("Column " + cce + " is not supported, skipped");

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
	 * @version MPCS-6808 Massive rewrite
	 */
	@Override
	public void parseCsv(final String csvStr, final List<String> csvColumns) {
		/*
		 * MPCS-6349 : DSS ID not set properly Removed dssId.
		 * Parent class has been updated with protected fields sessionDssId and
		 * recordDssId with get/set methods for both.
		 */

		// The following removes the start/end quotes w/ the substring
		// and splits based on ",". It leaves the trailing empty string in the
		// case that
		// csvStr ends with "". The empty strings server as place holders.
		final String[] dataArray = csvStr.substring(1, csvStr.length() - 1).split("\",\"", -1);

		if ((csvColumns.size() + 1) != dataArray.length) {
			throw new IllegalArgumentException("CSV column length mismatch, received " + dataArray.length
					+ " but expected " + (csvColumns.size() + 1));
		}

		// Clear everything we might process, in case empty column or not in
		// list

		sessionId = null;
		sessionHost = null;
		type = null;
		ert = null;
		relaySpacecraftId = null;
		vcid = null;
		vcfc = null;
		recordDssId = StationIdHolder.UNSPECIFIED_VALUE;
		bitRate = null;
		badReason = null;
		recordOffset = null;
		rct = null;
		sleMetadata = null;

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

				case "SESSIONHOST":
					sessionHost = token;
					break;

				case "TYPE":
					type = token;
					break;

				case "ERT":
					ert = new AccurateDateTime(token);
					break;

				case "RELAYSPACECRAFTID":
					relaySpacecraftId = Integer.valueOf(token);
					break;

				case "VCID":
					vcid = Integer.valueOf(token);
					break;

				case "SOURCEVCFC":
					vcfc = Integer.valueOf(token);
					break;
					
                case "VCFC":
                    vcfc = Integer.valueOf(token);
                    break;

				case "DSSID":
					recordDssId = Integer.parseInt(token);
					break;

				case "BITRATE":
					bitRate = Double.valueOf(token);
					break;

				case "ISBAD":
					// Nothing to do
					break;

				case "BADREASON":
					badReason = InvalidFrameCode.valueOf(token);
					break;

				case "LENGTH":
					// Nothing to do
					break;

				case "FILEBYTEOFFSET":
					recordOffset = Long.valueOf(token);
					break;

				case "RCT":
					rct = new AccurateDateTime(token);
					break;

				// MPCS-8021 added to handle named vcid column in
				// parseCsv
				case "VCIDNAME":
					// vcid name is mapped, not stored. do nothing
					break;

				case "SLEMETADATA":
					sleMetadata = token;
					break;

				default:
					// MPCS-8021 added to handle named vcid column
					// in parseCsv. Added here as well due to configurable
					// nature of the column name
					if (missionProperties.getVcidColumnName().toUpperCase().equals(upcce)) {
						// vcid name is mapped, not stored. do nothing
					} else if (!csvSkip.contains(upcce)) {
						log.warn("Column " + cce + " is not supported, skipped");

						csvSkip.add(upcce);
					}
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
	 * 
	 * MPCS-7587 Add named VCID column
	 */
	@Override
	public void setTemplateContext(final Map<String, Object> map) {
		super.setTemplateContext(map);

		if (type != null) {
			map.put("frameType", type);
		}

        // MPCS-9461 - updated to include the proper velocity template name
		if (relaySpacecraftId != null) {
			map.put("relayScid", relaySpacecraftId);
            map.put("relayScftId", relaySpacecraftId); // deprecated for R8
            map.put("relaySpacecraftId", relaySpacecraftId); // deprecated for R8
		}

		if (ert != null) {
			map.put("ert", ert.getFormattedErt(true));
		}

		if (vcid != null) {
			map.put("vcid", vcid);
		}

		// MPCS-7587 - add mapping of VCID name
		// MPCS-8021 - updated for efficiency
		if (missionProperties.shouldMapQueryOutputVcid() && this.vcid != null) {
			map.put(missionProperties.getVcidColumnName(), missionProperties.mapDownlinkVcidToName(this.vcid));
		}

		if (vcfc != null) {
			map.put("vcfc", vcfc);
		}

		if (sleMetadata != null) {
			map.put("sleMetadata", sleMetadata);
		}

		/*
		 * MPCS-6349 : DSS ID not set properly Removed dssId.
		 * Parent class has been updated with protected fields sessionDssId and
		 * recordDssId with get/set methods for both.
		 */
		map.put("dssId", recordDssId);

		if (bitRate != null) {
			map.put("bitRate", bitRate);
		}

        // MPCS-9461 - updated to include the proper velocity template name
		if (badReason != null) {
			map.put("bad", Boolean.TRUE);
			map.put("badReason", badReason);
            // deprecated
            map.put("isBad", Boolean.TRUE);
		} else {
			map.put("bad", Boolean.FALSE);
            // deprecated
            map.put("isBad", Boolean.FALSE);
		}

		/** MPCS-6808 Add RCT */

		if (this.rct != null) {
			map.put("rct", FastDateFormat.format(this.rct, null, null));
			map.put("rctExact", this.rct.getTime());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getType() {
		return type;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IAccurateDateTime getErt() {
		return ert;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Integer getVcfc() {
		return vcfc;
	}

	/*
	 * MPCS-6349 : DSS ID not set properly Removed dssId.
	 * Parent class has been updated with protected fields sessionDssId and
	 * recordDssId with get/set methods for both.
	 */

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Double getBitRate() {
		return bitRate;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean getIsBad() {
		return (badReason != null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public InvalidFrameCode getBadReason() {
		return badReason;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Long getFrameId() {
		return id;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PacketIdHolder getPacketId() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, String> getFileData(final String NO_DATA) {
		return null;
	}

	/*
	 * MPCS-6349 : DSS ID not set properly Removed dssId.
	 * Parent class has been updated with protected fields sessionDssId and
	 * recordDssId with get/set methods for both.
	 */
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Integer getRecordDssIdAsInt() {
		return recordDssId;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Integer getVcid() {
		return vcid;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setType(final String type) {
		this.type = type;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setErt(final IAccurateDateTime ert) {
		this.ert = ert;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public IAccurateDateTime getRct() {
		return rct;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public void setRct(final IAccurateDateTime rct) {
		this.rct = rct;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean getFillFrame() {
		return fillFrame;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setFillFrame(final boolean fillFrame) {
		this.fillFrame = fillFrame;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Integer getRelaySpacecraftId() {
		return relaySpacecraftId;
	}

	@Override
	public String getSleMetadata() {
		return sleMetadata;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setRelaySpacecraftId(final Integer relaySpacecraftId) {
		this.relaySpacecraftId = relaySpacecraftId;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setVcid(final Integer vcid) {
		this.vcid = vcid;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setVcfc(final Integer vcfc) {
		this.vcfc = vcfc;
	}

	/*
	 * MPCS-6349 : DSS ID not set properly Removed dssId.
	 * Parent class has been updated with protected fields sessionDssId and
	 * recordDssId with get/set methods for both.
	 */

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setBitRate(final Double bitRate) {
		this.bitRate = bitRate;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setBadReason(final InvalidFrameCode badReason) {
		this.badReason = badReason;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setId(final Long id) {
		this.id = id;
	}

	@Override
	public void setSleMetadata(final String  sleMetadata) {
		this.sleMetadata = sleMetadata;
	}
}
