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
package jpl.gds.dictionary.api.frame;

import java.util.Optional;

import jpl.gds.dictionary.api.IAttributesSupport;
import jpl.gds.dictionary.api.config.IFrameFormatDefinition;
import jpl.gds.serialization.frame.Proto3TransferFrameDefinition;

/**
 * The ITransferFrameDefinition interface is the dictionary interface that must
 * be implemented by all transfer frame definition classes. <br>
 * <br>
 * <b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br>
 * <br>
 * 
 * An ITransferFrameDefinition object is the multi-mission representation of one
 * defined type of downlinked transfer frame CADU. It describes the format, ASM,
 * and encoding of the frame. ITransferFrameDefinition defines methods needed to
 * interact with Transfer Frame Definition objects as required by the Transfer
 * Frame Dictionary interface. It is primarily used by transfer frame file
 * parser implementations in conjunction with the
 * TransferFrameDefinitionFactory, which is used to create actual multi-mission
 * ITransferFrameDefinition objects in the parsers. Dictionary and other objects
 * should interact with Transfer Frame Definition objects only through the
 * Factory and the ITransferFrameDefinition interfaces. Interaction with the
 * actual Transfer Frame Definition implementation classes in an
 * ITransferFrameDictionary implementation is contrary to multi-mission
 * development standards.
 * 
 *
 *
 * @see ITransferFrameDictionary
 * @see TransferFrameDefinitionFactory
 */
public interface ITransferFrameDefinition extends IAttributesSupport {

    /**
     * Gets the turbo encoding rate for this frame type as a string value: 1/2,
     * 1/4, 1/3, 1/6.
     * 
     * @return Returns the turbo rate string, or null if the frame encoding type
     *         is not turbo.
     */
    public String getTurboRate();

    /**
     * Gets the ASM (Asynchronous Marker or "PN" code) size in bytes. The ASM
     * byte size will be returned even if the frame typically arrives at AMPCS
     * without an ASM.
     * 
     * @return ASM size in bytes
     */
    public int getASMSizeBytes();

    /**
     * Sets the ASM (Asynchronous Marker or "PN" code) size in bits. The ASM bit
     * size should be set even if the frame typically arrives at AMPCS without
     * an ASM.
     *
     * @param size
     *            the ASM size in bits to set
     */
    public void setASMSizeBits(int size);

    /**
     * Gets the ASM bytes for this transfer frame definition. The ASM will be
     * returned even if the frame typically arrives at AMPCS without an ASM.
     * 
     * @return ASM byte array
     */
    public byte[] getASM();

    /**
     * Sets the ASM (as bytes) for this transfer frame definition. This is the
     * applicable ASM even if the frame arrives at AMPCS without an ASM.
     * 
     * @param buff
     *            the byte array representing the ASM value, which will be
     *            copied and may not be null
     */
    public void setASM(byte[] buff);

    /**
     * Gets the UNENCODED frame Channel Access Data Unit (CADU) size in bytes,
     * which includes the sync mark (ASM), primary and secondary header, PDU
     * header, frame data area, frame operational control field (OCF), frame
     * error correction (FECF or CRC) field, and encoding bit size. In short, it
     * is the entire size of the unencoded frame <b>as received by AMPCS</b>. If
     * the frame arrives without ASM, the ASM size is not included in the CADU
     * size. If the CADU bit size is not divisible by 8, the return value will
     * be rounded up to the next number that is divisible by 8.
     * 
     * @return CADU in bytes
     */
    public int getCADUSizeBytes();

    /**
     * Gets the UNENCODED frame Channel Access Data Unit (CADU) size in bits,
     * which includes the sync mark (ASM), primary and secondary header, PDU
     * header, frame data area, frame operational control field (OCF) frame
     * error correction (FECF or CRC) field, and encoding bit size. In short, it
     * is the entire size of the unencoded frame <b>as received by AMPCS</b>. If
     * the frame arrives at AMPCS without ASM, the ASM size is not included in
     * the CADU size. The CADU size in bits may not be divisible by 8.
     * 
     * @return CADU size in bits
     */
    public abstract int getCADUSizeBits();

    /**
     * Sets the unencoded frame Channel Access Data Unit (CADU) size in bits,
     * which includes the sync mark (ASM), primary and secondary header, PDU
     * header, frame data area, frame operational control field (OCF), frame
     * error correction (FECF or CRC) field, and encoding bit size. In short, it
     * is the entire size of the unencoded frame as received by AMPCS. If the
     * frame arrives at AMPCS without ASM, the ASM size is not included in the
     * CADU size. The CADU size in bits may or may not be divisible by 8.
     * 
     * @param bits
     *            CADU size to set in bits; must be a multiple of 8
     */
    public abstract void setCADUSizeBits(int bits);

    /**
     * Sets the frame Channel Access Data Unit (CADU) encoded size in bits,
     * which includes the sync mark (ASM), primary and secondary header, PDU
     * header, frame data area, frame operational control field (OCF), frame
     * error correction (FECF or CRC) field, and encoding bit size. In short, it
     * is the size of the CADU as received at a station. If the frame arrives at
     * AMPCS without ASM, the ASM size is not included in the encoded CADU size
     * TODO - Double check this fact.) The CADU size in bits
     * may or may not be divisible by 8.<br>
     * <br>
     * AMPCS does not decode RS or TURBO frames. The encoded size is used only
     * in rare situations for bitrate and ERT time calculations.
     * 
     * @param size
     *            The frame encoded CADU size in bits
     */
    public abstract void setEncodedCADUSizeBits(int size);
    
    /**
     * Gets the ENCODED frame Channel Access Data Unit (CADU) size in bytes,
     * which includes the sync mark (ASM), primary and secondary header, PDU
     * header, frame data area, frame operational control field (OCF), frame
     * error correction (FECF or CRC) field, and encoding bit size. In short, it
     * is the entire size of the unencoded frame <b>as received by a station</b>
     * . If the frame arrives without ASM, the ASM size is not included in the
     * encoded CADU size. If the CADU size in bits is not divisible by 8, the
     * return value will be fractional.<br>
     * <br>
     * AMPCS does not decode RS or TURBO frames. The encoded size is used only
     * in rare situations for bitrate and ERT time calculations.
     * 
     * @return the frame encoded size in bytes. Note that this may not be a
     *         whole byte value -- it may be fractional.
     */
    public abstract double getEncodedCADUSizeBytes();

    /**
     * Sets the bit size of the frame data area, also known as the packet or PDU
     * store size. This is the area of the frame in which packet or PDU data
     * will be found, and thus includes none of the frame header or trailing
     * blocks. Note that the size here does NOT include any PDU header in the
     * data area. This means that this size differs slightly from the CCSDS
     * definition.
     * 
     * @param size
     *            bit size to set
     */
    public void setDataAreaSizeBits(int size);

    /**
     * Gets the byte size of the frame data area, also known as the packet or
     * PDU store size. If the data area bit size is not divisible by 8, the
     * return value will be rounded up to the next number divisible by 8. This
     * is the area of the frame in which packet or PDU data will be found, and
     * thus includes none of the frame header or trailing blocks. Note that the
     * size here does NOT include any PDU header in the data area. This means
     * that this size differs slightly from the CCSDS definition.
     * 
     * @return data area byte size
     */
    public int getDataAreaSizeBytes();

    /**
     * Gets the encoding type for this frame definition (RS, TURBO, etc). <br>
     * <br>
     * AMPCS does not decode frames. Encoding type is used to map received
     * frames to the proper entry in the transfer frame dictionary, and in rare
     * cases for bitrate and ERT calculation.
     * 
     * @return the encoding type
     */
    public abstract EncodingType getEncoding();

    /**
     * Sets the encoding type for this frame definition. (RS, TURBO, etc). <br>
     * <br>
     * AMPCS does not decode frames. Encoding type is used to map received
     * frames to the proper entry in the transfer frame dictionary, and in rare
     * cases for bitrate and ERT calculation.
     *
     * @param encoding
     *            The encoding to set.
     */
    public abstract void setEncoding(EncodingType encoding);

    /**
     * Gets the encoding byte size for this frame definition. Encoding byte size
     * is the encoding bit size rounded up to the next value divisible by 8. At
     * this time, AMPCS does nothing with this field other than skip over it.
     * 
     * @return the encoding byte size
     */
    public abstract int getEncodingSizeBytes();

    /**
     * Gets the encoding bit size for this frame definition. Encoding bit size
     * should include any Reed-Solomon check size, TURBO trellis size, or any
     * other trailing bits related to the EncodingType of the frame. Note that
     * the bit length of the encoding data should be set based upon what is
     * attached to the frame at the time it arrives at AMPCS, not at the time it
     * arrives at the station. Encoding bit size may or may not be divisible by
     * 8. At this time, AMPCS does nothing with this field other than skip over
     * it.
     * 
     * @return the encoding bit size
     */
    public abstract int getEncodingSizeBits();

    /**
     * Sets the encoding bit size for this frame definition. Encoding bit size
     * should include Reed-Solomon check size, TURBO trellis size, or any other
     * trailing bits related to the EncodingType of the frame. Note that the bit
     * length of the encoding data should be set based upon what is attached to
     * the frame at the time it arrives at AMPCS, not at the time it arrives at
     * the station. Encoding bit size may or may not be divisible by 8. At this
     * time, AMPCS does nothing with this field other than skip over it.
     *
     * @param encSize
     *            The encoding bit size
     */
    public abstract void setEncodingSizeBits(int encSize);

    /**
     * Gets the frame definition name. This is the unique identifier for a
     * specific frame definition within the dictionary.
     * 
     * @return the name of this frame definition.
     */
    public abstract String getName();

    /**
     * Sets the name of this frame definition. This is the unique identifier for
     * a specific frame definition within the dictionary.
     *
     * @param name
     *            The name to set; may not be null.
     */
    public abstract void setName(String name);

    /**
     * Sets the bit size of the optional Operational Control Field (OCF). If
     * this length is non-zero, the frame has an OCF following the frame data
     * area.
     * 
     * @param size
     *            the size of the OCF in bits; 0 implies there is no OCF
     */
    public void setOperationalControlSizeBits(int size);

    /**
     * Gets the byte size of the optional Operational Control Field (OCF). If
     * this length is non-zero, the frame has an OCF following the frame data
     * area. If the bit size of the OCF is not divisible by 8, the return value
     * will be rounded up to the next number divisible by 8.
     * 
     * @return the size of the OCF in bytes; 0 implies there is no OCF
     */
    public int getOperationalControlSizeBytes();

    /**
     * Indicates whether the frame has an Operational Control Field (OCF).
     * 
     * @return true if the frame has an OCF, false if not
     */
    public boolean hasOperationalControl();

    /**
     * Sets the bit size of the transfer frame primary header, which is
     * mandatory on all frames and immediately follows the frame ASM. If there
     * is an optional Frame header Error Control Field (FHECF), that length
     * should NOT be included in the primary header length.
     * 
     * @param size
     *            the size of the primary header in bits
     */
    public void setPrimaryHeaderSizeBits(int size);

    /**
     * Gets the byte size of the transfer frame primary header, which is
     * mandatory on all frames. If the bit size of the primary header is not
     * divisible by 8, the return value will be rounded up to the next number
     * divisible by 8. If there is an optional Frame header Error Control Field
     * (FHECF), that length should NOT be included in the primary header length.
     * 
     * @return the size of the primary header in bytes
     */
    public int getPrimaryHeaderSizeBytes();

    /**
     * Sets the bit size of optional the transfer frame secondary header or
     * insert zone, which, if present, immediately follows the primary header of
     * FHECF.
     * 
     * @param size
     *            the size of the secondary header in bits; 0 implies there is
     *            no secondary header
     */
    public void setSecondaryHeaderSizeBits(int size);

    /**
     * Gets the byte size of the optional transfer frame secondary header or
     * insert zone. If the bit size of the secondary header is not divisible by
     * 8, the return value will be rounded up to the next number divisible by 8.
     * 
     * @return the size of the secondary header in bytes; 0 implies there is no
     *         secondary header
     */
    public int getSecondaryHeaderSizeBytes();

    /**
     * Indicates whether the frame has a secondary header or insert zone.
     * 
     * @return true if the frame has a secondary header, false if not
     */
    public boolean hasSecondaryHeader();

    /**
     * Sets the bit size of the optional Frame Header Error Control Field
     * (FHECF), which, if present, is assumed to follow the frame primary
     * header. At this time, AMPCS does nothing with this field other than skip
     * over it.
     *
     * @param size
     *            the size of the FHECF in bits; 0 implies there is no FHECF
     */
    public void setHeaderErrorControlSizeBits(int size);

    /**
     * Gets the byte size of the optional Frame Header Error Control Field
     * (FHECF), which, if present, is assumed to follow the frame primary
     * header. At this time, AMPCS does nothing with this field other than skip
     * over it. If the bit size of the FHECF is not divisible by 8, the return
     * value will be rounded up to the next number divisible by 8.
     *
     * @return the size of the FHECF in bytes; 0 implies there is no FHECF
     */
    public int getHeaderErrorControlSizeBytes();

    /**
     * Indicates whether the frame has a frame Header Error Control Field
     * (FHECF).
     * 
     * @return true if the frame has an FHECF, false if not
     */
    public boolean hasHeaderErrorControl();

    /**
     * Sets the bit size of the optional Frame Error Control Field (FECF),
     * which, if present, is assumed to follow the frame data area, or the OCF,
     * if present. The only supported bit sizes are 16, 24, 32, and 64.
     *
     * @param size
     *            the size of the FECF in bits; 0 implies there is no FECF
     */
    public void setFrameErrorControlSizeBits(int size);

    /**
     * Gets the byte size of the optional Frame Error Control Field (FECF),
     * which, if present, is assumed to follow the frame data area, or the OCF,
     * if present. The only supported byte sizes are 2, 3, 4, and 8.
     *
     * @return the size of the FECF in bytes; 0 implies there is no FECF
     */
    public int getFrameErrorControlSizeBytes();

    /**
     * Indicates whether the frame has a frame Error Control Field (FECF).
     * 
     * @return true if the frame has an FECF, false if not
     */
    public boolean hasFrameErrorControl();

    /**
     * Sets the optional description of this frame definition.
     * 
     * @param description
     *            the descriptive text for this frame type
     */
    public void setDescription(String description);

    /**
     * Gets the optional description of this frame definition.
     * 
     * @return Optional of String containing the descriptive text for this frame type,
     *         or an empty Optional if no description defined
     */
    public Optional<String> getDescription();

    /**
     * Sets the flag indicating whether this frame arrives at the AMPCS downlink
     * processor with an ASM.
     * 
     * @param enable
     *            true if the ASM will be attached to the frame when it arrives,
     *            false if not
     */
    public void setArrivesWithASM(boolean enable);

    /**
     * Gets the flag indicating whether this frame arrives at the AMPCS downlink
     * processor with an ASM.
     * 
     * @return true if the ASM will be attached to the frame when it arrives,
     *         false if not
     */
    public boolean arrivesWithASM();

    /**
     * Sets the size of the data area PDU header in bits. The data area PDU
     * header is assumed to follow the frame primary header, optional FHECF, and
     * optional secondary header. The only PDU header types supported are the
     * CCSDS M_PDU and the CCSDS B_PDU.
     * 
     * @param size
     *            bit size of the PDU header; 0 implies there is no PDU header
     */
    public void setPduHeaderSizeBits(int size);

    /**
     * Gets the size of the data area PDU header in bytes. The data area PDU
     * header is assumed to follow the frame primary header, optional FHECF, and
     * optional secondary header.
     * 
     * @return byte size of the PDU header; 0 implies there is no PDU header
     */
    public int getPduHeaderSizeBytes();

    /**
     * Indicates whether the frame has a PDU header.
     * 
     * @return true if there is a PDU header, false if not
     */
    public boolean hasPduHeader();

    /**
     * Gets the total bytes size of the frame header, including primary header,
     * FHECF, secondary header or insert zone, and data area PDU header.
     * 
     * @return total byte size of the header
     */
    public int getTotalHeaderSizeBytes();

    /**
     * Sets the frame format object for this frame definition. The frame format
     * defines the frame as either a supported CCSDS type or a custom type, and
     * provides Java class names for the classes that parse the frame header and
     * compute the FECF.
     * 
     * @param type
     *            FrameFormat object to set
     */
    public void setFormat(IFrameFormatDefinition type);

    /**
     * Gets the frame format object for this frame definition. The frame format
     * defines the frame as either a supported CCSDS type or a custom type, and
     * provides Java class names for the classes that parse the frame header,
     * compute the FECF, and extract timecode.
     * 
     * @return FrameFormat object; once the frame definition is established,
     *         should never be null
     */
    public IFrameFormatDefinition getFormat();
    
    /**
     * Sets any associated IFrameTimeFieldDefinition, which is used to extract
     * spacecraft/hardware timecode from the frame header.
     * 
     * @param timeField IFrameTimeFieldDefinition to set
     */
    public void setTimeField(IFrameTimeFieldDefinition timeField);
    
    /**
     * Gets any associated IFrameTimeFieldDefinition, which is used to extract
     * spacecraft/hardware timecode from the frame header.
     * 
     * @return Optional of IFrameTimeFieldDefinition, or an empty Optional if none
     *         defined.
     */
    public Optional<IFrameTimeFieldDefinition> getTimeField();

    /**
     * Build and return the protobuf representation of a Transfer Frame Definition
     * 
     * @return the protobuf transfer frame representation.
     */
    public Proto3TransferFrameDefinition build();
    
    /**
     * Load from a protobuf message
     * @param msg
     */
    public void load(Proto3TransferFrameDefinition msg);

}