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
package jpl.gds.station.impl.dsn.chdo;

import static jpl.gds.station.api.dsn.chdo.ChdoConstants.CHARSET;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.holders.HeaderHolder;
import jpl.gds.shared.holders.HolderException;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.sfdu.SfduException;
import jpl.gds.shared.sfdu.SfduId;
import jpl.gds.shared.sfdu.SfduLabel;
import jpl.gds.shared.sfdu.SfduVersionException;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.station.api.dsn.chdo.ChdoPropertyException;
import jpl.gds.station.api.dsn.chdo.IChdo;
import jpl.gds.station.api.dsn.chdo.IChdoCondition;
import jpl.gds.station.api.dsn.chdo.IChdoConfiguration;
import jpl.gds.station.api.dsn.chdo.IChdoDefinition;
import jpl.gds.station.api.dsn.chdo.IChdoFieldDefinition;
import jpl.gds.station.api.dsn.chdo.IChdoProperty;
import jpl.gds.station.api.dsn.chdo.IChdoSfdu;
import jpl.gds.station.api.dsn.chdo.IEqualityCondition;

/**
 * This class represents a Compressed Header Data Object (CHDO) structured CCSDS
 * Standard Formatted Data Unit (SFDU). CHDO These SFDUs are self-identifying,
 * self-delimiting data structures used to encapsulate data acquired (telemetry
 * via radio frequency or hardline) or produced by the DSMS for delivery to a
 * mission. In short, CHDO SFDUs arrive as headers applied to telemetry frames
 * or packets by the various receiving and ground systems which have processed
 * them.
 * 
 * See JPL D-16745, DSMS 820-013 module 0172-Telecomm-CHDO
 * "DSMS-Created CHDO Structures" for an overview of CHDO design, and the
 * various CHDO descriptions for further detail.
 * 
 */

// The basic CHDO structure for a version 2 SFDU is as follows.
// The primary CHDO is always CHDO 2 for DSN and AMMOS generated
// data. The other CHDOs differ from mission to mission and data
// format to data format
//
// +-------------------------------------------+
// | CCSDS SFDU Label |
// | "NJPL2I00xxxx" |
// |-------------------------------------------|
// | Length of SFDU = L1 |
// + +-------------------------------------------+
// | | CHDO aggregation chdo_type code |
// | |-------------------------------------------|
// | | Length of aggregated CHDOs = L2 |
// | +-------------------------------------------+ +
// | | Primary chdo_type code | |
// | |-------------------------------------------| |
// | | Length of primary CHDO = L3 | |
// | +|-------------------------------------------| |
// | || | |
// | L3| Primary CHDO data | |
// | || | |
// | ++-------------------------------------------+ |
// | | Secondary CHDO_type code | |
// | |-------------------------------------------| |
// | | Length of secondary CHDO = L4 | |
// | +|-------------------------------------------| |
// | || | |
// | L4| Secondary CHDO data | L2
// | || | |
// | ++-------------------------------------------+ |
// | | Tertiary CHDO_type code | |
// L1 |-------------------------------------------| |
// | | Length of tertiary CHDO = L5 | |
// | +|-------------------------------------------| |
// | || | |
// | L5| Tertiary CHDO data | |
// | || | |
// | ++-------------------------------------------+ |
// | | Quaternary CHDO_type code | |
// | |-------------------------------------------| |
// | | Length of quaternary CHDO = L6 | |
// | +|-------------------------------------------| |
// | || | |
// | L6| Quaternary CHDO data | |
// | || | |
// | ++-------------------------------------------+ +
// | | Data CHDO type code |
// | |-------------------------------------------|
// | | Length of data CHDO = L7 |
// | +|-------------------------------------------|
// | || |
// | L7| Data CHDO data |
// | || |
// + ++-------------------------------------------+

public class ChdoSfdu implements IChdoSfdu {
    private static final Tracer      trace                       = TraceManager.getDefaultTracer();

    private static final Tracer      debugTrace                  = TraceManager
            .getTracer(Loggers.TLM_EHA);


	// Store control authority IDs to look for in a config file (CCSD, NJPL,
	// ...)
	private final String[] controlAuthorityIds;

	/** Maximum CHDO SFDU size (straight out of the 0172-Telecomm) */
	public static final int MAX_SFDU_SIZE = 131096; 

	private SfduLabel sfduLabel;
	private byte[] sfduBuffer;
	private SfduId sfduId;

    /** Keep track of the offset of the last CHDO, the data CHDO. */
    private int lastChdoOffset = 0;


	private final IChdoConfiguration dictionary;
	private final Map<Integer, Chdo> typeToChdoMap;
	
	/** Label that indicates the start of a TDS response */
	public static final String TDS_RESPONSE_START_LABEL = "CCSD3ZS00001TDSQDATA";
	/** Label that indicates the end of a TDS response */
	public static final String TDS_RESPONSE_END_LABEL = "CCSD3RE00000TDSQDATA";
	/** Label from TDS that indicates the start of the query parameters object */
	public static final String TDS_QUERY_PARAM_START_LABEL = "NJPL3KS0L009TDSQUERY";
	/** Label from TDS that indicates the end of the query parameters object */
	public static final String TDS_QUERY_PARAM_END_LABEL = "CCSD3RE00000TDSQUERY";
	/** Label that indicates the start of a status/error message */
	public static final String STAT_ERR_MSG_START_LABEL = "NJPL3KS0L009STAT/ERR";
	/** Label that indicates the end of a status/error message */
	public static final String STAT_ERR_MSG_END_LABEL = "CCSD3RE00000STAT/ERR";
	
	
	/**
	 * Creates a blank instance of ChdoSfdu.
     * @param chdoConfig the SHDU SFDU configuration
	 */
	public ChdoSfdu(final IChdoConfiguration chdoConfig)  {
		this.typeToChdoMap = new HashMap<Integer, Chdo>(128);
		this.dictionary = chdoConfig;
		this.controlAuthorityIds = this.dictionary.getControlAuthorityIds();

		clear();
	}

	/**
	 * Clears the current CHDO definition and all CHDO/SFDU related fields.
	 */
	@Override
    public void clear() {
		this.sfduBuffer = new byte[MAX_SFDU_SIZE + SfduLabel.LABEL_LENGTH];
		this.sfduLabel = null;
		this.sfduId = null;
		this.typeToChdoMap.clear();
	}
	
	@Override
    public void loadSdfuHeaderOnly(final byte[] buffer, final int offset, final int length) throws IOException, SfduException {
	    clear();
	    
	    DataInputStream bis = null;
	    
	    try {
            if (length < SfduLabel.LABEL_LENGTH) {
                throw new IllegalArgumentException("Requested read of CHDO SFDU but length of buffer is shorter than the SFDU label length");
            }
            
            bis = new DataInputStream(new ByteArrayInputStream(buffer, offset, length));

            // read in the SFDU label
            this.sfduLabel = readSfduLabel(bis);
            if (this.sfduLabel == null) {
                throw new IllegalStateException("Got \"null\" when trying to read the SFDU label");
            }

            debugTrace.debug("========================================================================");
            debugTrace.debug(this.sfduLabel);
            debugTrace.debug("========================================================================\n");

            // get the full SFDU length
            switch (this.sfduLabel.getVersionId()) {
            // in case 1 and 2, we get a numeric length back that's the # of bytes
            // in the SFDU
            case 1:
            case 2:

                readSfduByLength(bis, length - SfduLabel.LABEL_LENGTH);
                break;

            // if the version is something else, we just reject it
            default:

                debugTrace.debug("Version #"
                        + this.sfduLabel.getVersionId()
                        + " SFDU labels are not currently supported by this application. The offending SFDU label is: "
                        + this.sfduLabel.toString());
                throw new SfduVersionException("Version #"
                        + this.sfduLabel.getVersionId()
                        + " SFDU labels are not currently supported by this application. The offending SFDU label is: "
                        + this.sfduLabel.toString(), this.sfduLabel.getVersionId(), this.sfduLabel.toString());
            }

            readChdos(true);
            createSfduId();
        } finally {
            bis.close();
            
        }
    }
	

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoSfdu#readSfdu(java.io.DataInputStream)
     */
	@Override
    public void readSfdu(final DataInputStream dis) throws IOException,
	        EOFException, SfduException {
		if (dis == null) {
			throw new EOFException("Null input data stream.");
		}

		clear();

		// read in the SFDU label
		this.sfduLabel = readSfduLabel(dis);
		if (this.sfduLabel == null) {
			throw new IllegalStateException("Got \"null\" when trying to read the SFDU label");
		}

		debugTrace.debug("========================================================================");
		debugTrace.debug(this.sfduLabel);
		debugTrace.debug("========================================================================\n");

		// get the full SFDU length
		switch (this.sfduLabel.getVersionId()) {
		// in case 1 and 2, we get a numeric length back that's the # of bytes
		// in the SFDU
		case 1:
		case 2:

			readSfduByLength(dis);
			break;

		case 3:
			readMessage(dis);
			//no chdos or additional data to be read
			return;
		// if the version is something else, we just reject it
		default:

			final String errMsg = "Version #" + this.sfduLabel.getVersionId()
			        + " SFDU labels are not currently supported by this application. The offending SFDU label is: "
			        + this.sfduLabel.toString();
			debugTrace.debug(errMsg);
			throw new SfduVersionException(errMsg, this.sfduLabel.getVersionId(), this.sfduLabel.toString());
		}

		readChdos(false);
		createSfduId();
	}

	/**
	 * If the SFDU label has a version of 1 or 2, we know that it has a length
	 * in bytes of how much more data should be read from the input stream. This
	 * functions assumes that a val
	 * 
	 * @param dis
	 * @throws IOException
	 * @throws EOFException
	 * @throws SfduException
	 */
	private void readSfduByLength(final DataInputStream dis)
	        throws IOException, EOFException, SfduException {
		if (dis == null) {
			throw new EOFException("Null input data stream.");
		} else if (this.sfduLabel == null) {
			throw new IllegalStateException("Tried to read in a full SFDU, but the internal SFDU label is null.");
		}

		// get the length in bytes to read
		final Integer sfduLengthObj = this.sfduLabel.getBlockLength();
		if (sfduLengthObj == null) {
			throw new SfduException("Obtained a null length from the SFDU label.  Make sure the SFDU label version is supported.");
		}

		final int sfduLength = sfduLengthObj.intValue();
		debugTrace.debug("SFDU length is " + sfduLength);

		if (sfduLength > MAX_SFDU_SIZE || sfduLength < 0) {
			throw new SfduException("The length value of " + sfduLength
			        + " bytes found in the " + " version #"
			        + this.sfduLabel.getVersionId()
			        + " SFDU label is outside the allowable range " + " of "
			        + 0 + " to " + MAX_SFDU_SIZE + " bytes.");
		}

		// read the bytes
		this.sfduBuffer = new byte[SfduLabel.LABEL_LENGTH + sfduLength];
		System.arraycopy(this.sfduLabel.getBytes(), 0, this.sfduBuffer, 0, SfduLabel.LABEL_LENGTH);
		dis.readFully(this.sfduBuffer, SfduLabel.LABEL_LENGTH, sfduLength);
	}
	
	private void readSfduByLength(final DataInputStream dis, final int length)
            throws IOException, EOFException, SfduException {
        if (dis == null) {
            throw new EOFException("Null input data stream.");
        }

        final int sfduLength = length;
        debugTrace.debug("SFDU length is " + sfduLength);

        if (sfduLength > MAX_SFDU_SIZE || sfduLength < 0) {
            throw new SfduException("The length value of " + sfduLength
                    + " bytes found in the " + " version #"
                    + this.sfduLabel.getVersionId()
                    + " SFDU label is outside the allowable range " + " of "
                    + 0 + " to " + MAX_SFDU_SIZE + " bytes.");
        }

        // read the bytes
        this.sfduBuffer = new byte[SfduLabel.LABEL_LENGTH + sfduLength];
        System.arraycopy(this.sfduLabel.getBytes(), 0, this.sfduBuffer, 0, SfduLabel.LABEL_LENGTH);
        dis.readFully(this.sfduBuffer, SfduLabel.LABEL_LENGTH, sfduLength);
    }

	/**
	 * Read in an SFDU label from the data stream. If the next byte of the data
	 * stream isn't the first byte of an SFDU label, this function will throw
	 * away intermediate data until it finds the control authority ID (usually
	 * "NJPL" or "CCSD") of an SFDU label that it recognizes.
	 * 
	 * @param dis
	 * 
	 * @return
	 * 
	 * @throws IOException
	 * @throws EOFException
	 */
	private SfduLabel readSfduLabel(final DataInputStream dis)
	        throws IOException, EOFException {
		if (dis == null) {
			throw new EOFException("Null input data stream.");
		} else if (this.sfduBuffer == null) {
			throw new IllegalStateException("Tried to read in a full SFDU, but the internal SFDU buffer is null.");
		}

		// Loop until we find a control authority ID that we recognize
		// (to start, read in the length of an entire control authority ID)
		boolean found = false;
		final byte[] sfduLabelBuffer = new byte[SfduLabel.LABEL_LENGTH];
		dis.readFully(sfduLabelBuffer, 0, SfduLabel.CONTROL_AUTHORITY_ID_LENGTH);
		while (found == false) {
			// Look to see if the current bytes in the buffer match any of
			// the defined control authority IDs we're looking for
			// (this is done by a char-by-char comparison for each control
			// authority ID that we're looking for)
			for (int i = 0; i < this.controlAuthorityIds.length; i++) {
				// loop through to see if the current control authority ID
				// matches what's in the buffer
				int j = 0;
				for (; j < SfduLabel.CONTROL_AUTHORITY_ID_LENGTH; j++) {
					if (sfduLabelBuffer[j] != this.controlAuthorityIds[i].charAt(j)) {
						break;
					}
				}

				// if the inner for loop above ran all the way through, then
				// it means we found a match on a control authority ID
				if (j == SfduLabel.CONTROL_AUTHORITY_ID_LENGTH) {
					found = true;
					break;
				}
			}

			// skip this step if we found a control authority ID in the current
			// position
			if (found == false) {
				// drop the first byte in the buffer, shift everything left by 1
				// spot,
				// read in one more byte, and then loop around to compare again
				int k = 1;
				for (; k < SfduLabel.CONTROL_AUTHORITY_ID_LENGTH; k++) {
					sfduLabelBuffer[k - 1] = sfduLabelBuffer[k];
				}
				dis.readFully(sfduLabelBuffer, k - 1, 1);
			}
		}

		// At this point, sfduBuffer[0] is pointing to the start of a control
		// authority ID we recognize, so
		// starting after that control authority ID, read in the rest of the
		// SFDU label
		dis.readFully(sfduLabelBuffer, SfduLabel.CONTROL_AUTHORITY_ID_LENGTH, SfduLabel.LABEL_LENGTH
		        - SfduLabel.CONTROL_AUTHORITY_ID_LENGTH);

		// parse the SFDU label that we read in
		final SfduLabel label = new SfduLabel(sfduLabelBuffer, 0);
		return (label);
	}


	/**
	 * Read sequential chdos in this sfdu
     *
     * @throws SfduException If problem with data CHDO
	 */
	private void readChdos(final boolean headerOnly) throws SfduException
    {
		int  offset = SfduLabel.LABEL_LENGTH;
        Chdo chdo   = null;

		while (offset < this.sfduBuffer.length)
        {
            // Keep track of last one, which better be the data CHDO
            lastChdoOffset = offset;

			final int chdoType = GDR.get_u16(this.sfduBuffer, offset);

			final int chdoLength = GDR.get_u16(this.sfduBuffer, offset+IChdoDefinition.CHDO_TYPE_SIZE);
			
			final IChdoDefinition definition = this.dictionary.getDefinitionByType(chdoType);
			
			 /* Check for null definition also */
			if (headerOnly && (definition == null || definition.getClassification().equals("data"))) {
			    break;
			}

			chdo = readChdo(offset, chdoType, chdoLength);
			offset += IChdoDefinition.CHDO_TYPE_SIZE
					+ IChdoDefinition.CHDO_LENGTH_SIZE;
			if (chdo == null) {
				// this will work as long as the unidentified chdo is not an aggregation type
				offset += chdoLength;
			} else {
				this.typeToChdoMap.put(Integer.valueOf(chdoType), chdo);

				if (chdo.getDefinition().getClassification().equals("aggregation") == false) {
					offset += chdo.getLength();
				}
			}
		}

        if (!headerOnly && ((chdo == null) || ! chdo.getDefinition().getClassification().equals("data")))
        {
            throw new SfduException("Data CHDO not found at end of SFDU");
        }
	}


	/**
	 * Extract the next chdo at the given offset and return it as a Chdo object, or return
	 * null if no definition is available for the chdo type indicated in the type field in
	 * the given byte stream. Thus, any chdo type not defined in the chdo.xml config file
	 * will be skipped quietly (a debug-level log message will be recorded whenever a chdo
	 * is skipped this way). 
	 * note: previous version that threw an exception on unidentified chdo had the side-effect
	 * of short-circuiting the read loop in readChdos so that any subsequent chdos in the same
	 * sfdu would not even be examined.
	 * 
	 * @param offset
	 * @return extracted Chdo object or null if type is not defined
	 */
	private Chdo readChdo(final int offset, final int chdoType, final int chdoLength) {
		final int chdoOffset = offset;

		final IChdoDefinition definition = this.dictionary.getDefinitionByType(chdoType);
		if (definition == null) {
			debugTrace.warn("Unrecognized CHDO Type \"" + chdoType
			        + "\" encountered. Skipping.");
			return null;
		}

		int actualLength = chdoLength;
		
		if (definition.getClassification().equals("aggregation")) {
		    actualLength = 0;
        }
		
		final byte[] chdoBytes = new byte[IChdoDefinition.CHDO_TYPE_SIZE
		        + IChdoDefinition.CHDO_LENGTH_SIZE + actualLength];
		System.arraycopy(this.sfduBuffer, chdoOffset, chdoBytes, 0, chdoBytes.length);
		if (debugTrace.isDebugEnabled()) {
			final StringBuilder chdoText = new StringBuilder(2048);
			chdoText.append(definition.getClassification());
			chdoText.append(" CHDO Type ");
			chdoText.append(chdoType);
			chdoText.append(" (Length ");
			chdoText.append(chdoLength);
			chdoText.append(") Hex: \n\n");
			chdoText.append(BinOctHexUtility.toHexFromBytes(chdoBytes));
			chdoText.append("\n\n");
			debugTrace.debug(chdoText.toString());
		}

		final Chdo chdo = new Chdo(definition, chdoBytes);
		chdo.setLength(chdoLength);

		return (chdo);
	}

	private void createSfduId() {
		this.sfduId = new SfduId();

		this.sfduId.setDdpId(this.sfduLabel.getDataDescriptionPackageId());

		final Long majorType = getFieldValueAsUnsignedInt("major");
		if (majorType == null) {
			throw new IllegalStateException("Could not find a \"major\" in the input SFDU (should be in the primary CHDO).");
		}
		this.sfduId.setMajorDataType(majorType.intValue());
		debugTrace.debug("Major Type = " + majorType);

		final Long minorType = getFieldValueAsUnsignedInt("minor");
		if (minorType == null) {
			throw new IllegalStateException("Could not find a \"minor\" in the input SFDU (should be in the primary CHDO).");
		}
		this.sfduId.setMinorDataType(minorType.intValue());
		debugTrace.debug("Minor Type = " + minorType);

		final Long missionId = getFieldValueAsUnsignedInt("mission_id");
		if (missionId == null) {
			throw new IllegalStateException("Could not find a \"mission_id\" in the input SFDU (should be in the primary CHDO).");
		}
		this.sfduId.setMissionId(missionId.intValue());
		debugTrace.debug("Mission ID = " + missionId);

		final Long formatId = getFieldValueAsUnsignedInt("format");
		if (formatId == null) {
			throw new IllegalStateException("Could not find a \"format\" in the input SFDU (should be in the primary CHDO).");
		}
		this.sfduId.setFormatId(formatId.intValue());
		debugTrace.debug("Format ID = " + formatId + "\n\n");
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoSfdu#getFieldValueAsByteArray(java.lang.String)
     */
	@Override
    public byte[] getFieldValueAsByteArray(final String fieldName) {
		final IChdo chdo = getChdoForFieldName(fieldName);
		if (chdo == null) {
			return (null);
		}

		return (IChdoSfdu.getFieldValueAsByteArray(chdo, fieldName));
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoSfdu#getFieldValueAsUnsignedInt(java.lang.String)
     */
	@Override
    public Long getFieldValueAsUnsignedInt(final String fieldName) {
		final IChdo chdo = getChdoForFieldName(fieldName);
		if (chdo == null) {
			return (null);
		}

		return (IChdoSfdu.getFieldValueAsUnsignedInt(chdo, fieldName));
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoSfdu#getFieldValueAsSignedInt(java.lang.String)
     */
	@Override
    public Long getFieldValueAsSignedInt(final String fieldName) {
		final IChdo chdo = getChdoForFieldName(fieldName);
		if (chdo == null) {
			return (null);
		}

		return (IChdoSfdu.getFieldValueAsSignedInt(chdo, fieldName));
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoSfdu#getFieldValueAsFloatingPoint(java.lang.String)
     */
	@Override
    public Double getFieldValueAsFloatingPoint(final String fieldName) {
		final IChdo chdo = getChdoForFieldName(fieldName);
		if (chdo == null) {
			return (null);
		}

		return (IChdoSfdu.getFieldValueAsFloatingPoint(chdo, fieldName));
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoSfdu#getFieldValueAsDate(java.lang.String)
     */
	@Override
    public IAccurateDateTime getFieldValueAsDate(final String fieldName) {
		final IChdo chdo = getChdoForFieldName(fieldName);
		if (chdo == null) {
			return (null);
		}

		return (IChdoSfdu.getFieldValueAsDate(chdo, fieldName));
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoSfdu#getFieldValueAsSclk(java.lang.String)
     */
	@Override
    public ISclk getFieldValueAsSclk(final String fieldName) {
		final IChdo chdo = getChdoForFieldName(fieldName);
		if (chdo == null) {
			return (null);
		}

		return (IChdoSfdu.getFieldValueAsSclk(chdo, fieldName));
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoSfdu#getFieldValueAsBoolean(java.lang.String)
     */
	@Override
    public Boolean getFieldValueAsBoolean(final String fieldName) {
		final Long value = getFieldValueAsUnsignedInt(fieldName);
		if (value == null) {
			trace.error("Boolean field " + fieldName
			        + " does not exist. Returning false by default.");
			return (Boolean.FALSE);
		}

		return (value.longValue() != 0);
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoSfdu#getFieldValueAsString(java.lang.String)
     */
	@Override
    public String getFieldValueAsString(final String fieldName)
	        throws UnsupportedEncodingException {
		final byte[] byteValue = getFieldValueAsByteArray(fieldName);
		if (byteValue == null) {
			return (null);
		}

		final String value = new String(byteValue, CHARSET);
		return (value);
	}

	/**
	 * Gets the Chdo object containing the indicated CHDO field.
	 * 
	 * @param fieldName name of the CHDO field to look for
	 * @return Chdo object containing the field, or null if not found
	 */
	private IChdo getChdoForFieldName(final String fieldName) {
		final Collection<Chdo> chdoCollection = this.typeToChdoMap.values();
		for (final IChdo chdo : chdoCollection) {
			if (chdo.getDefinition().getFieldDefinitionByName(fieldName) != null) {
				return (chdo);
			}
		}

		return (null);
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoSfdu#getChdoFieldDefinitionForFieldName(java.lang.String)
     */
	@Override
    public IChdoFieldDefinition getChdoFieldDefinitionForFieldName(
	        final String fieldName) {
		final IChdo chdo = getChdoForFieldName(fieldName);
		if (chdo == null) {
			return null;
		}
		final IChdoFieldDefinition fieldDef = chdo.getDefinition().getFieldDefinitionByName(fieldName);
		return fieldDef;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoSfdu#getDataChdo()
     */
	@Override
    public IChdo getDataChdo() {
		final Collection<Chdo> chdoCollection = this.typeToChdoMap.values();
		for (final IChdo chdo : chdoCollection) {
			if (chdo.getDefinition().getClassification().equals("data") == true) {
				return (chdo);
			}
		}

		return (null);
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoSfdu#isGifFrame()
     */
	@Override
    public Boolean isGifFrame() throws ChdoPropertyException {
		return (getPropertyValue("isGifFrame"));
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoSfdu#isFrame()
     */
	@Override
    public Boolean isFrame() throws ChdoPropertyException {
		return (getPropertyValue("isFrame"));
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoSfdu#isPacket()
     */
	@Override
    public Boolean isPacket() throws ChdoPropertyException {
		return (getPropertyValue("isPacket"));
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoSfdu#isOutOfSync()
     */
	@Override
    public Boolean isOutOfSync() throws ChdoPropertyException {
		return (getPropertyValue("isOutOfSync"));
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoSfdu#isIdle()
     */
	@Override
    public Boolean isIdle() throws ChdoPropertyException {
		return (getPropertyValue("isIdle"));
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoSfdu#isInvalid()
     */
	@Override
    public Boolean isInvalid() throws ChdoPropertyException {
		return (getPropertyValue("isInvalid"));
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoSfdu#isCdr()
     */
	@Override
    public Boolean isCdr() throws ChdoPropertyException {
		return (getPropertyValue("isCdr"));
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoSfdu#isEcdr()
     */
	@Override
    public Boolean isEcdr() throws ChdoPropertyException {
		return (getPropertyValue("isEcdr"));
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoSfdu#isQqc()
     */
	@Override
    public Boolean isQqc() throws ChdoPropertyException {
		return (getPropertyValue("isQqcData"));
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoSfdu#isMonitor()
     */
	@Override
    public Boolean isMonitor() throws ChdoPropertyException {
		return (getPropertyValue("isMonitorData"));
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoSfdu#isAnomaly()
     */
	@Override
    public Boolean isAnomaly() throws ChdoPropertyException {
		return (getPropertyValue("isAnomaly"));
	}

	/**
	 * isPadded has been returned for SFDU packets but not frames.
	 * Is Padded() has been returned, CHDO for SDFU packets does contain isDataPadded
	 */
	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoSfdu#isPadded()
     */
	@Override
    public Boolean isPadded() throws ChdoPropertyException {
		return (getPropertyValue("isDataPadded"));
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoSfdu#isPacketFull()
     */
	@Override
    public Boolean isPacketFull() throws ChdoPropertyException {
		return (getPropertyValue("isPacketFull"));
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoSfdu#isTurbo()
     */
	@Override
    public Boolean isTurbo() throws ChdoPropertyException {
		return (getPropertyValue("isTurbo"));
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoSfdu#getTurboRate()
     */
	@Override
    public String getTurboRate() {
		final long denom = getFieldValueAsUnsignedInt("turbo_rate_denominator");
		return "1/" + denom;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoSfdu#getNumberOfDataBits()
     */
	@Override
    public int getNumberOfDataBits() throws ChdoPropertyException {
		final Long numberOfDataBitsObj = getFieldValueAsUnsignedInt("number_bits");
		final long numberOfDataBits = (null == numberOfDataBitsObj) ? 0 : numberOfDataBitsObj.longValue();
		if ((numberOfDataBits > Integer.MAX_VALUE) || (numberOfDataBits < 0)) {
			throw new ChdoPropertyException("Number of data bits will not result in a positive Integer: " + numberOfDataBits);
		}
		return (int)numberOfDataBits;
	}
	
	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoSfdu#getPropertyValue(java.lang.String)
     */
	@Override
    public Boolean getPropertyValue(final String propertyName)
	        throws ChdoPropertyException {
		final IChdoProperty property = this.dictionary.getPropertyByName(propertyName);
		if (property == null) {
			throw new ChdoPropertyException("The CHDO property \""
			        + propertyName + "\" is not defined.", propertyName);
		}

		boolean propertyResult = false;
		final List<IChdoCondition> chdoConditions = property.getChdoConditions();
		for (int i = 0; i < chdoConditions.size(); i++) {
			final IChdoCondition chdoCondition = chdoConditions.get(i);
			final IChdo chdo = this.typeToChdoMap.get(Integer.valueOf(chdoCondition.getChdoType()));
			if (chdo == null) {
				// this is in here for algorithm clarity...I'm aware it does
				// nothing
				// propertyResult = propertyResult || false;
				continue;
			}

			boolean conditionResult = true;
			final List<IEqualityCondition> equalityConditions = chdoCondition.getEqualityConditions();
			for (int j = 0; j < equalityConditions.size(); j++) {
				boolean equalityStatementResult = false;

				final IEqualityCondition ec = equalityConditions.get(j);
				final String chdoFieldName = ec.getName();

				final IChdoFieldDefinition fieldDef = chdo.getDefinition().getFieldDefinitionByName(chdoFieldName);
				if (fieldDef == null) {
					throw new ChdoPropertyException("The CHDO property \""
					        + propertyName + "\" references "
					        + "an unknown CHDO field \"" + chdoFieldName + "\"");
				}

				final boolean areEqual = fieldDef.getFieldFormat().resolveComparison(ec.getValue(), chdo, chdoFieldName);
				equalityStatementResult = areEqual == ec.getEqualityValue();

				// all equality statements within a given condition are ANDed
				// together
				if (equalityStatementResult == false) {
					conditionResult = false;
					break;
				}
				// conditionResult = conditionResult && equalityStatementResult;
			}

			// all condition statements within a property are ORed together
			if (conditionResult == true) {
				propertyResult = true;
				break;
			}
			// propertyResult = propertyResult || conditionResult;
		}

		return (propertyResult);
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoSfdu#getBytes()
     */
	@Override
    public byte[] getBytes() {
		final byte[] bytes = new byte[this.sfduBuffer.length];
		System.arraycopy(this.sfduBuffer, 0, bytes, 0, bytes.length);
		return (bytes);
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoSfdu#getSfduId()
     */
	@Override
    public SfduId getSfduId() {
		return this.sfduId;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoSfdu#getSfduLabel()
     */
	@Override
    public SfduLabel getSfduLabel() {
		return this.sfduLabel;
	}


    /**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoSfdu#getEntireHeader()
     */
    @Override
    public HeaderHolder getEntireHeader() throws SfduException
    {
        try
        {
            return HeaderHolder.valueOf(
                       sfduBuffer,
                       0,
                       lastChdoOffset + getDataChdo().getHeaderLength());
        }
        catch (final HolderException he)
        {
           throw new SfduException("ChdoSfdu.getEntireHeader", he);
        }
    }
	/**
	 * Handles reading SFDU messages that come in. All messages are labeled
	 * version 3 If the "message" is a header, an appropriate message is given.
	 * If there is additional information, it is displayed
	 * 
	 * @param dis
	 *            the DataInputStream containing the incoming data
	 * @throws IOException
	 *             If an error is encountered while reading from the input
	 *             stream
	 * @throws SfduVersionException
	 *             if the SFDU label cannot be handled
	 * 
	 */
    private void readMessage(final DataInputStream dis) throws IOException, SfduVersionException{
    	switch (this.sfduLabel.toString().toUpperCase()){
		case ChdoSfdu.TDS_RESPONSE_START_LABEL:
			trace.info("vvvvvvvvvvvv TDS RESPONSE BEGIN vvvvvvvvvvvv ");
			break;
		case ChdoSfdu.TDS_RESPONSE_END_LABEL:
			trace.info("^^^^^^^^^^^^  TDS RESPONSE END  ^^^^^^^^^^^^");
			break;
		case ChdoSfdu.STAT_ERR_MSG_START_LABEL:
			trace.info(">>>>>>>>>>> SFDU Status Message <<<<<<<<<<<");
			displayMessage(dis);
			break; 
		case ChdoSfdu.TDS_QUERY_PARAM_START_LABEL:
			trace.info("----------- TDS Query Parameters -----------");
			displayMessage(dis);
			break;
		case ChdoSfdu.STAT_ERR_MSG_END_LABEL:
		case ChdoSfdu.TDS_QUERY_PARAM_END_LABEL:
			//these close out their messages, no need to report anything
			break;
		default:
			final String errMsg = "Received an unknown SFDU message label. The offending SFDU label is: "
					+ this.sfduLabel.toString(); 
			debugTrace.debug(errMsg);
			throw new SfduVersionException(errMsg, this.sfduLabel.getVersionId(), this.sfduLabel.toString());
		}
    	
    }
    
	/**
	 * Displays to the user additional message information for query parameter
	 * and status messages received
	 * 
	 * @param dis
	 *            the DataInputStream containing the incoming data
	 * @throws IOException
	 *             If an error is encountered while reading from the input
	 * 
	 */
    private void displayMessage(final DataInputStream dis) throws IOException{
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte newChar;
		
		while (!baos.toString().startsWith("END_OBJECT")){
			
			baos.reset();
			
			do{
				newChar = dis.readByte();
				
				/*
				 * A properly formatted TDS query parameters response will have
				 * a newline character after each property. This will be the
				 * first character read on all properties but the first one.
				 * 
				 * TDS status/error messages do not have a new-line character
				 * after each parameter in the message. However, the MESSAGE
				 * property does have a newline after the text, but before its
				 * terminating semicolon.
				 * 
				 * In either case this newline character can be discarded.
				 */
				if (newChar != (byte)0x0A){
					baos.write(newChar);
				}
			} while(newChar != ';');
			
			trace.info("    " + baos.toString());
		}
	}

    @Override
    public Integer getScid() {
        Long scidObj = getFieldValueAsUnsignedInt("scft_id");
		//old deprecared field in Monitor-0158
        if (scidObj == null) {
            scidObj = getFieldValueAsUnsignedInt("8b_scft_id");
        }
		//other secondary headers
	    if (scidObj == null) {
		    scidObj = getFieldValueAsUnsignedInt("spacecraft_id");
	    }
        return scidObj == null ? null : Integer.valueOf(scidObj.intValue());
    }

    @Override
    public Integer getVcid() {
        final Long vcidObj = getFieldValueAsUnsignedInt("virtual_channel_id");
        return vcidObj == null ? null : Integer.valueOf(vcidObj.intValue());
    }

    @Override
    public Integer getDssId() {
        final Long dssIdObj = getFieldValueAsUnsignedInt("data_source");
        return dssIdObj == null ? null : Integer.valueOf(dssIdObj.intValue());
    }
}
