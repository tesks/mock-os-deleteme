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
package jpl.gds.dictionary.impl.frame;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.dictionary.api.config.FrameErrorControlType;
import jpl.gds.dictionary.api.config.IFrameFormatDefinition;
import jpl.gds.dictionary.api.frame.EncodingType;
import jpl.gds.dictionary.api.frame.IFrameTimeFieldDefinition;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinition;
import jpl.gds.dictionary.api.frame.ITransferFrameDictionary;
import jpl.gds.dictionary.api.frame.TransferFrameDefinitionFactory;
import jpl.gds.dictionary.impl.AbstractBaseDictionary;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.time.CustomSclkExtractor;
import jpl.gds.shared.xml.XmlUtility;

/**
 * MultimissionTransferFrameDictionary parses and serves up the content of a
 * telemetry transfer frame dictionary file, which contains definitions of the
 * transfer frame Channel Access Data Unit (CADUs) for a specific mission. These
 * define aspects of the transfer frames such as length, ASM, and encoding type.
 * Note that the same class is used regardless of mission at this time. Do not
 * create an instance directly. Go through TransferFrameDictionaryFactory.
 * 
 * @since AMPCS R7.4
 * 
 *
 * @see TransferFrameDictionaryFactory
 */
public class MultimissionTransferFrameDictionary extends AbstractBaseDictionary
        implements ITransferFrameDictionary {

    private static final String MM_SCHEMA_VERSION = DictionaryProperties
            .getMultimissionDictionaryVersion(DictionaryType.FRAME);
    private static final String ROOT_ELEMENT_NAME = "frame_dictionary";
    private static final String HEADER_ELEMENT_NAME = "header";

    private int maxFrameSize;
    private ITransferFrameDefinition[] tff = new ITransferFrameDefinition[0];
    private final Map<String, ITransferFrameDefinition> formatMap = new HashMap<String, ITransferFrameDefinition>();
    private ITransferFrameDefinition currentTf;
    private final List<ITransferFrameDefinition> parsedTfList = new ArrayList<ITransferFrameDefinition>();
    private int timeOffset;
    private int timeSize;

    /**
     * Package protected constructor.
     * 
     */
    /* package */MultimissionTransferFrameDictionary() {
        super(DictionaryType.FRAME, MM_SCHEMA_VERSION);
    }


    @Override
    public void clear() {
        tff = new ITransferFrameDefinition[0];
        parsedTfList.clear();
        formatMap.clear();
        maxFrameSize = 0;
        super.clear();
    }

    /**
     * @{inheritDoc
     * @see org.xml.sax.helpers.DefaultHandler#startDocument()
     */
    @Override
    public void startDocument() throws SAXException {
        super.startDocument();
        setRequiredElements(
                "Multimission",
                Arrays.asList(new String[] { ROOT_ELEMENT_NAME,
                        HEADER_ELEMENT_NAME }));
    }

    @Override
    public void parse(final String filename, final DictionaryProperties config)
            throws DictionaryException {
        parse(filename, config, tracer);
    }

    @Override
    public void parse(final String filename, final DictionaryProperties config, final Tracer log)
            throws DictionaryException {
        super.parse(filename, config, log);

        /*
         * Transfer frame definitions have to be sorted, first by ASM length
         * (descending) and within that, by frame length (ascending). This
         * ensures that we always match the longest ASM possible when ASMs have
         * a common prefix, and that we always match the shortest possible frame
         * (with the already matched ASM) we can find in the data before we
         * attempt to match longer frames..
         */
        final TransferFrameFormatCompare tfCompare = new TransferFrameFormatCompare();
        Arrays.sort(tff, tfCompare);

        // Get the maximum frame size from the included frames and put them in
        // the final Map.
        for (int i = 0; i < this.tff.length; ++i) {
            if (this.formatMap.get(tff[i].getName()) != null) {
                throw new DictionaryException(
                        "Duplicate definition of transfer frame type "
                                + tff[i].getName()
                                + " found in the transfer frame dictionary. Frame type names must be unique.");
            }
            this.formatMap.put(tff[i].getName(), tff[i]);
            if (this.tff[i].getCADUSizeBytes() > this.maxFrameSize) {
                this.maxFrameSize = this.tff[i].getCADUSizeBytes();
            }
        }
    }


    @Override
    public int getMaxFrameSize() {
        return this.maxFrameSize;
    }


    @Override
    public List<ITransferFrameDefinition> getFrameDefinitions() {
        final List<ITransferFrameDefinition> returnList = new ArrayList<ITransferFrameDefinition>();
        returnList.addAll(Arrays.asList(this.tff));
        return returnList;
    }


    @Override
    public ITransferFrameDefinition findFrameDefinition(final String type) {
        return this.formatMap.get(type);
    }
    
    /**
     * Added method, used to duplicate structure of
     * previous findFrameDefinition
     * 
     * Gets the TransferFrameFormat object that matches the given CADU bit size.
     * Calls the other version of this function. Because null is being passed as
     * the string argument, it will return a ITransferFrameDefinition that
     * matches in size to the supplied argument.
     * 
     * @param sizeBits
     *            the CADU size to match in bits (as supplied by CHDO #69,
     *            field: number_bits).
     * @return the matching ITransferFrameDefinition object, or null if no match
     *         found
     */
    @Override
    public ITransferFrameDefinition findFrameDefinition(final int sizeBits) {
        return findFrameDefinition(sizeBits, null);
    }

    /**
     *
     * Gets the TransferFrameFormat object that matches the given CADU bit size.
     * 
     * @param sizeBits
     *            the CADU size to match in bits
     * @param turboRate
     *            the turbo rate for TURBO formats, or null if not specified.
     * @return the matching ITransferFrameDefinition object, or null if no match
     *         found
     */
    @Override
    public ITransferFrameDefinition findFrameDefinition(final int sizeBits,
            final String turboRate) {
        for (int i = 0; i < this.tff.length; ++i) {
            if (this.tff[i].getCADUSizeBits() == sizeBits) {
                if ((turboRate != null) && (this.tff[i].getTurboRate() != null)) {
                    if (turboRate.equalsIgnoreCase(this.tff[i].getTurboRate())) {
                        return this.tff[i];
                    }
                } else if (turboRate != null) {
                    continue;
                } else {
                    return this.tff[i];
                }
            }
        }
        tracer.warn("Could not find appropriate frame using sizeBits");
        return null;
    }

    /**
     * This comparator class allows sort of TransferFrameDefinition objects such
     * that frames with longest ASMs are first, and within frame types with the
     * same ASM size, shortest length frames are first. The reason for this is
     * 1) some ASMs are actually prefixes of longer ASMs. We want to make the
     * longest possible match to ASM, so longer ASM frame types should be looked
     * for first when syncing frames. 2) Once ASM is matched, we want to look
     * for the shortest frame with that ASM first, so we will not end up
     * accidentally finding a longer frame and not noticing that it actually
     * consisted of multiple short frames. The latter situation would be
     * uncommon, but is not impossible.
     * 
     */
    @SuppressWarnings("serial")
    public static class TransferFrameFormatCompare implements Serializable,
            Comparator<ITransferFrameDefinition> {
        /**
         * Compares two transfer frame definitions.
         * 
         * @param tff0
         *            The first TFF to compare
         * @param tff1
         *            The second TFF to compare
         * @return An integer, 0 if equal, 1 if tff0 is smaller, -1 if tff0 is
         *         larger.
         */
        @Override
        public int compare(final ITransferFrameDefinition tff0,
                final ITransferFrameDefinition tff1) {

            final int tff0AsmSize = tff0.getASMSizeBytes();
            final int tff1AsmSize = tff1.getASMSizeBytes();

            if (tff0AsmSize < tff1AsmSize) {
                return 1;
            } else if (tff0AsmSize > tff1AsmSize) {
                return -1;
            }
            if (tff0.getCADUSizeBits() > tff1.getCADUSizeBits()) {
                return 1;
            } else if (tff0.getCADUSizeBits() < tff1.getCADUSizeBits()) {
                return -1;
            }
            return 0;
        }
    };

    /**
     * Gets an array of parsed transfer frame definitions, or an empty list if
     * none found or yet parsed.
     * 
     * @return an array of TransferFrameDefinition objects, or an empty array if
     *         none exist
     */
    private ITransferFrameDefinition[] getTransferFrameFormatArray() {
        final ITransferFrameDefinition[] tff = new ITransferFrameDefinition[parsedTfList
                .size()];
        return parsedTfList.toArray(tff);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
     *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    @Override
    public void startElement(final String uri, final String localName,
            final String qname, final Attributes attr) throws SAXException {
        text = new StringBuilder();
        
        /* @TODO -Validate lengths of bit fields that need to be
         * divisible by 8. 
         */ 

        /*
         *  Use common method to parser and report
         * header info.
         */
        parseMultimissionHeader(localName, attr);

        if (qname.equalsIgnoreCase("telemetry_frame_definition")) {
            currentTf = TransferFrameDefinitionFactory.createTransferFrame();
            currentTf.setEncoding(EncodingType.UNENCODED);
            currentTf.setName(getRequiredAttribute("name", qname, attr));
            currentTf.setCADUSizeBits(getRequiredIntAttr("cadu_size", qname,
                    attr));
            final int encodedSize = getOptionalIntAttr("encoded_cadu_size", qname,
                    attr);
            currentTf.setEncodedCADUSizeBits(encodedSize == 0 ? currentTf
                    .getCADUSizeBits() : encodedSize);

        } else if (qname.equalsIgnoreCase("asm_definition")) {
            currentTf.setArrivesWithASM(getRequiredBooleanAttr(
                    "arrives_as_attached", qname, attr));

        } else if (qname.equalsIgnoreCase("reed_solomon_encoding")) {
            currentTf.setEncoding(EncodingType.REED_SOLOMON);
            currentTf.setEncodingSizeBits(getRequiredIntAttr("bit_size", qname,
                    attr));

        } else if (qname.equalsIgnoreCase("turbo_encoding")) {
            final String rateStr = getRequiredAttribute("turbo_rate", qname, attr);
            try {
                currentTf.setEncoding(EncodingType.valueOf(rateStr));
            } catch (final IllegalArgumentException e) {
                tracer.warn("Unrecogized turbo rate " + rateStr
                        + " found in transfer frame definition "
                        + currentTf.getName() + ". Rate will be undefined.");
                currentTf.setEncoding(EncodingType.ANY_TURBO);
            }
            currentTf.setEncodingSizeBits(getRequiredIntAttr("bit_size", qname,
                    attr));

        } else if (qname.equalsIgnoreCase("ccsds_tm_1_frame")) {

            final String typeStr = getRequiredAttribute("frame_format_type", qname,
                    attr);
            try {
                final IFrameFormatDefinition.TypeName type = IFrameFormatDefinition.TypeName.valueOf(typeStr);
                if (type != IFrameFormatDefinition.TypeName.CCSDS_TM_1) {
                    error("Invalid value for frame_format_type attribute ("
                            + typeStr + ") in ccsds_tm_1_frame element");
                }
                currentTf.setFormat(new FrameFormatDefinition(type));
            } catch (final IllegalArgumentException e) {
                error("Unrecognized value for frame_format_type attribute ("
                        + typeStr + ") in ccsds_tm_1_frame element");
            }

            currentTf.setPrimaryHeaderSizeBits(48);
            currentTf.setPduHeaderSizeBits(0);
            currentTf.setDataAreaSizeBits(getRequiredIntAttr(
                    "data_area_length", qname, attr));
            currentTf.setSecondaryHeaderSizeBits(getOptionalIntAttr(
                    "secondary_header_length", qname, attr));
            final boolean hasOcf = getRequiredBooleanAttr("has_ocf", qname, attr);
            if (hasOcf) {
                currentTf.setOperationalControlSizeBits(32);
            }
            final boolean hasFecf = getRequiredBooleanAttr("has_fecf", qname, attr);
            if (hasFecf) {
                currentTf.setFrameErrorControlSizeBits(16);
            }

        } else if (qname.equalsIgnoreCase("ccsds_aos_2_frame")) {

            final String typeStr = getRequiredAttribute("frame_format_type", qname,
                    attr);
            try {
                final IFrameFormatDefinition.TypeName type = IFrameFormatDefinition.TypeName.valueOf(typeStr);
                currentTf.setFormat(new FrameFormatDefinition(type));
            } catch (final IllegalArgumentException e) {
                error("Unrecognized value for frame_format_type attribute ("
                        + typeStr + ") in ccsds_aos_2_frame element");
            }

            switch (currentTf.getFormat().getType()) {
            case CCSDS_AOS_2_BPDU:
            case CCSDS_AOS_2_MPDU:
                currentTf.setPduHeaderSizeBits(16);
                break;
            case CCSDS_TM_1:
            case CUSTOM_CLASS:
            default:
                error("Invalid value for frame_format_type attribute ("
                        + typeStr + ") in ccsds_aos_2_frame element");
            }

            currentTf.setPrimaryHeaderSizeBits(48);
            currentTf.setDataAreaSizeBits(getRequiredIntAttr(
                    "data_area_length", qname, attr));
            currentTf.setSecondaryHeaderSizeBits(getOptionalIntAttr(
                    "insert_zone_length", qname, attr));

            final boolean hasFhecf = getRequiredBooleanAttr("has_fhecf", qname, attr);
            if (hasFhecf) {
                currentTf.setHeaderErrorControlSizeBits(16);
            }

            final boolean hasOcf = getRequiredBooleanAttr("has_ocf", qname, attr);
            if (hasOcf) {
                currentTf.setOperationalControlSizeBits(32);
            }
            final boolean hasFecf = getRequiredBooleanAttr("has_fecf", qname, attr);
            if (hasFecf) {
                currentTf.setFrameErrorControlSizeBits(16);
            }
        } else if (qname.equalsIgnoreCase("custom_frame_format")) {

            final String typeStr = getRequiredAttribute("frame_format_type", qname,
                    attr);
            IFrameFormatDefinition.TypeName type = null;

            try {
                type = IFrameFormatDefinition.TypeName.valueOf(typeStr);
            } catch (final IllegalArgumentException e) {
                error("Unrecognized value for frame_format_type attribute ("
                        + typeStr + ") in custom_frame_format element");
            }
            if (type != IFrameFormatDefinition.TypeName.CUSTOM_CLASS) {
                error("Invalid value for frame_format_type attribute ("
                        + typeStr + ") in custom_frame_format element");
            }
            currentTf.setFormat(new FrameFormatDefinition(type));

            currentTf.setPrimaryHeaderSizeBits(getRequiredIntAttr(
                    "primary_header_length", qname, attr));
            currentTf.setDataAreaSizeBits(getRequiredIntAttr(
                    "data_area_length", qname, attr));
            currentTf.setSecondaryHeaderSizeBits(getOptionalIntAttr(
                    "secondary_header_length", qname, attr));
            currentTf.setHeaderErrorControlSizeBits(getOptionalIntAttr(
                    "fhecf_length", qname, attr));
            currentTf.setOperationalControlSizeBits(getOptionalIntAttr(
                    "ocf_length", qname, attr));
            currentTf.setFrameErrorControlSizeBits(getOptionalIntAttr(
                    "fecf_length", qname, attr));
            currentTf.setPduHeaderSizeBits(getOptionalIntAttr(
                    "pdu_header_length", qname, attr));

            final String headerClass =  getRequiredAttribute("frame_header_class", qname, attr);
            
            try {
                type = IFrameFormatDefinition.TypeName.valueOf(headerClass);
                currentTf.getFormat().setFrameHeaderClass(type.getDefaultFrameHeaderClass());
                currentTf.getFormat().setFrameErrorControlClass(
                        type.getDefaultFrameErrorControlClass());

            } catch (final IllegalArgumentException e) {
                currentTf.getFormat().setFrameHeaderClass(headerClass);
                
            }
            final String errorClass = attr.getValue("frame_error_control_class");
            try {
                final FrameErrorControlType errorType = FrameErrorControlType
                        .valueOf(errorClass);
                currentTf.getFormat().setFrameErrorControlClass(
                        errorType.getDefaultFrameErrorClass());
            } catch (final IllegalArgumentException e) {
                currentTf.getFormat().setFrameErrorControlClass(errorClass);
            }
        } else if (qname.equalsIgnoreCase("time_field")) {
            timeOffset = getRequiredIntAttr("bit_offset", qname, attr);
            timeSize = getRequiredIntAttr("bit_size", qname, attr);
            
        } else if (qname.equalsIgnoreCase("project_sclk_time")) {
            final IFrameTimeFieldDefinition timeField = new FrameTimeFieldDefinition(IFrameTimeFieldDefinition.TimecodeType.PROJECT_SCLK, timeOffset, timeSize);
            currentTf.setTimeField(timeField);
            
        } else if (qname.equalsIgnoreCase("custom_sclk_time")) {
            final IFrameTimeFieldDefinition timeField = new FrameTimeFieldDefinition(IFrameTimeFieldDefinition.TimecodeType.CUSTOM_SCLK, timeOffset, timeSize);
            currentTf.setTimeField(timeField);
            final Map<String, Object> params = new HashMap<String, Object>();
            // shortened coarse_bit_len and fine_bit_len to match names in TransferFrameDictionary.rnc
            params.put(CustomSclkExtractor.COARSE_LENGTH_PARAM, getRequiredIntAttr("coarse_bit_len", qname, attr));
            params.put(CustomSclkExtractor.FINE_LENGTH_PARAM, getRequiredIntAttr("fine_bit_len", qname, attr));
            params.put(CustomSclkExtractor.FINE_LIMIT_PARAM, getRequiredIntAttr("fine_upper_limit", qname, attr));
            timeField.setParameterMap(params);
            
        } else if (qname.equalsIgnoreCase("java_time_class")) {
            final IFrameTimeFieldDefinition timeField = new FrameTimeFieldDefinition(IFrameTimeFieldDefinition.TimecodeType.CUSTOM_CLASS, timeOffset, timeSize);
            timeField.setExtractorClass(getRequiredAttribute("class_name", qname, attr));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    @Override
    public void endElement(final String uri, final String localName,
            final String qname) throws SAXException {
        super.endElement(uri, localName, qname);
        final String inText = XmlUtility.normalizeWhitespace(text.toString().trim());

        if (qname.equalsIgnoreCase("frame_dictionary")) {
            /*
             * Copy parsed definitions to
             * the member array in the enclosing class.
             */
            tff = getTransferFrameFormatArray();
        }
        if (qname.equalsIgnoreCase("telemetry_frame_definition")) {

            validateFrame(currentTf);
            parsedTfList.add(currentTf);
            currentTf = null;
            
        } else if (qname.equalsIgnoreCase("description")) {
            currentTf.setDescription(inText);

        } else if (qname.equalsIgnoreCase("asm_definition")) {

            if (inText.length() < 2 || inText.length() % 2 != 0) {
                error("ASM for transfer frame definition "
                        + currentTf.getName() + " is too short or "
                        + "does not contain an even number of hex digits: "
                        + inText);
            }

            final int numDigits = inText.length() / 2;
            int off = 0;
            final byte[] foo = new byte[numDigits];
            for (int i = 0; i < inText.length(); i += 2) {
                try {
                    foo[off++] = (byte) GDR.parse_int("0x"
                            + inText.substring(i, i + 2));
                } catch (final NumberFormatException e) {
                    error("ASM for transfer frame definition "
                            + currentTf.getName() + " is not a valid "
                            + "hex string: " + inText);
                }
            }

            currentTf.setASM(foo);
            currentTf.setASMSizeBits(foo.length * 8);
        }
    }

    private int getRequiredIntAttr(final String attrName, final String elementName,
            final Attributes attr) throws SAXParseException {
        final String strVal = getRequiredAttribute(attrName, elementName, attr);
        try {
            return GDR.parse_int(strVal);
        } catch (final NumberFormatException e) {
            error("Value of the " + attrName + " attribute for element "
                    + elementName + " must be an integer. Found: " + strVal);
        }
        return 0;
    }
    
    @SuppressWarnings("unused")
	private long getRequiredLongAttr(final String attrName, final String elementName,
            final Attributes attr) throws SAXParseException {
        final String strVal = getRequiredAttribute(attrName, elementName, attr);
        try {
            return GDR.parse_long(strVal);
        } catch (final NumberFormatException e) {
            error("Value of the " + attrName + " attribute for element "
                    + elementName + " must be an integer. Found: " + strVal);
        }
        return 0;
    }

    private boolean getRequiredBooleanAttr(final String attrName, final String elementName,
            final Attributes attr) throws SAXParseException {
        final String strVal = getRequiredAttribute(attrName, elementName, attr);
        try {
            return GDR.parse_boolean(strVal);
        } catch (final NumberFormatException e) {
            error("Value of the " + attrName + " attribute for element "
                    + elementName + " must be true or false. Found: " + strVal);
        }
        return false;
    }

    private int getOptionalIntAttr(final String attrName, final String elementName,
            final Attributes attr) throws SAXParseException {
        final String strVal = attr.getValue(attrName);
        if (strVal == null) {
            return 0;
        }
        try {
            return GDR.parse_int(strVal);
        } catch (final NumberFormatException e) {
            error("Value of the " + attrName + " attribute for element "
                    + elementName + " must be an integer. Found: " + strVal);
        }
        return 0;
    }

    private void validateFrame(final ITransferFrameDefinition def)
            throws SAXParseException {
        final int totalSize = (def.arrivesWithASM() ? def.getASMSizeBytes() : 0)
                + def.getTotalHeaderSizeBytes() + def.getDataAreaSizeBytes()
                + def.getFrameErrorControlSizeBytes()
                + def.getOperationalControlSizeBytes()
                + def.getEncodingSizeBytes();
        if (def.getCADUSizeBytes() != totalSize) {
            error("Transfer frame definition for frame "
                    + def.getName()
                    + " is inconsistent. Byte size of the various frame fields ("
                    + totalSize + ") does not" + " add up to the CADU size ("
                    + def.getCADUSizeBytes() + ")");
        }
    }
}
