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
import jpl.gds.dictionary.api.config.IFrameFormatDefinition;
import jpl.gds.dictionary.api.frame.EncodingType;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinition;
import jpl.gds.dictionary.api.frame.ITransferFrameDictionary;
import jpl.gds.dictionary.api.frame.TransferFrameDefinitionFactory;
import jpl.gds.dictionary.impl.AbstractBaseDictionary;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.xml.XmlUtility;

/**
 * TransferFrameDictionary parses and serves up the content of a telemetry
 * transfer frame dictionary file, which contains definitions of the transfer
 * frame Control and Display Units (CADUs) for a specific mission. These define
 * aspects of the transfer frames such as length, ASM, and encoding type. Note
 * that the same class is used regardless of mission at this time. Transfer
 * frame dictionary files are currently delivered as AMPCS configuration files
 * by AMPCS, and not as dictionary files from the mission. Do not create an
 * instance directly. Go through TransferFrameDictionaryFactory.
 *
 * 
 * @deprecated Do not use for new missions or implementations. The current
 *             implementation is now in MultimissionTransferFrameDictionary.
 */
@Deprecated
public class OldTransferFrameDictionary extends AbstractBaseDictionary implements ITransferFrameDictionary {
    
    /**
     *  Hardcode to old schema version. This should never change again.
     */
    private static final String MM_SCHEMA_VERSION = "6.0";


    private int maxFrameSize;
    private ITransferFrameDefinition[] tff = new ITransferFrameDefinition[0];
    /*  Add hashmap of format objects keyed by frame type */
    private final Map<String, ITransferFrameDefinition> formatMap = new HashMap<String, ITransferFrameDefinition>();
    
    /* Parser fields */
    private ITransferFrameDefinition currentTf;
    private final List<ITransferFrameDefinition> parsedTfList = new ArrayList<ITransferFrameDefinition>();
    private int currentAsmSize = 0;
    private int currentAsmActualSize = 0;
    
    /**
     * Package protected constructor.
     * 
     */
    OldTransferFrameDictionary() {
        super(DictionaryType.FRAME, MM_SCHEMA_VERSION);
    }
    

    @Override
    public void clear() {
        tff = new ITransferFrameDefinition[0];
        formatMap.clear();
        maxFrameSize = 0;
        super.clear();
    }
  
    @Override
    public void parse(final String filename, final DictionaryProperties config)
            throws DictionaryException {
        parse(filename, config, tracer);
    }

    @Override
    public void parse(final String filename, final DictionaryProperties config, final Tracer log)
            throws DictionaryException {
        /*  Use parse logic in the superclass. */
        super.parse(filename, config, log);
        
        /*
         *  Transfer frame definitions have to be
         * sorted, first by ASM length (descending) and within that, by frame
         * length (ascending). This ensures that we always match the longest ASM
         * possible when ASMs have a common prefix, and that we always match the
         * shortest possible frame (with the already matched ASM) we can find in
         * the data before we attempt to match longer frames.
         *
         *  No longer need to get the format array
         * from the parser. The parser copies it into the array member.
         */
        final TransferFrameFormatCompare tfCompare = new TransferFrameFormatCompare();
        Arrays.sort(tff, tfCompare);

        // Get the maximum frame size from the included frames.
        for (int i = 0; i < this.tff.length; ++i) {
            /*  Add format object to hashmap */
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
     *  Added method, used to duplicate structure of previous findFrameDefinition
     * 
     * Gets the TransferFrameFormat object that matches the given CADU bit
     * size. Calls the other version of this function. Because null is being passed as the string argument,
     * it will return a ITransferFrameDefintion that matches in size to the supplied argument.
     * 
     * @param sizeBits the CADU size to match in bits (as supplied by CHDO #69, field: number_bits).
     * @return the matching TransferFrameFormat object, or null if no match
     *         found
     */
    @Override
    public ITransferFrameDefinition findFrameDefinition(final int sizeBits){
    	return findFrameDefinition(sizeBits, null);
    }
    
    /**
     *
     * Gets the TransferFrameFormat object that matches the given CADU bit
     * size.
     * 
     * @param sizeBits the CADU size to match in bits (as supplied by CHDO #69, field: number_bits.
     * @param turboRate the turbo rate for TURBO formats, or null if not specified.
     * @return the matching TransferFrameFormat object, or null if no match
     *         found
     */
	@Override
    public ITransferFrameDefinition findFrameDefinition(final int sizeBits, final String turboRate) {
		for (int i = 0; i < this.tff.length; ++i) {
			if (this.tff[i].getCADUSizeBits() == sizeBits) {
				if ((turboRate != null) && (this.tff[i].getTurboRate() != null)) {
					if (turboRate.equalsIgnoreCase(this.tff[i].getTurboRate())) {
						return this.tff[i];
					}
				}
				else if (turboRate != null) {
					continue;
				}
				else {
					return this.tff[i];
				}
			}
		}
		tracer.warn("Could not find appropriate frame using sizeBits");
		return null;
	}

    /**
     * This comparator class allows sort of TransferFrameDefinition objects
     * such that frames with longest ASMs are first, and within 
     * frame types with the same ASM size, shortest length frames are first.
     * The reason for this is 1) some ASMs are actually prefixes of 
     * longer ASMs. We want to make the longest possible match to ASM,
     * so longer ASM frame types should be looked for first when syncing
     * frames. 2) Once ASM is matched, we want to look for the shortest
     * frame with that ASM first, so we will not end up accidentally 
     * finding a longer frame and not noticing that it actually consisted of
     * multiple short frames. The latter situation would be uncommon,
     * but is not impossible.
     * 
     */
    @SuppressWarnings("serial")
    public static class TransferFrameFormatCompare implements Serializable, Comparator<ITransferFrameDefinition> {
        /**
         * Compares two transfer frame definitions.
         * @param tff0 The first TFF to compare
         * @param tff1 The second TFF to compare
         * @return An integer, 0 if equal, 1 if tff0 is smaller, -1 if tff0 is larger.
         */
        @Override
        public int compare ( final ITransferFrameDefinition tff0, final ITransferFrameDefinition tff1 ) {

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
     * Gets an array of parsed transfer frame definitions, or an empty 
     * list if none found or yet parsed.
     * @return an array of TransferFrameDefinition objects, or an empty array
     * if none exist
     */
    private ITransferFrameDefinition[] getTransferFrameFormatArray() {
        final ITransferFrameDefinition[] tff = new ITransferFrameDefinition[parsedTfList.size()];
        return parsedTfList.toArray(tff);
    }

    /**
     * {@inheritDoc}
     * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    @Override
    public void startElement(final String uri, final String localName, final String qname,
            final Attributes attr) throws SAXException {
        text = new StringBuilder();

        /* Use common method to parser and report header info. */
        parseMultimissionHeader(localName, attr);

        if (qname.equals("Frame")) {
            currentTf = TransferFrameDefinitionFactory.createTransferFrame();
            currentTf.setName(attr.getValue("name"));
            /* 
             *  Parse both old and new values for RS encoding. Modified
             * to use temporary variable for efficiency and check for missing
             * encoding.
             */
            final String encoding = attr.getValue("encoding");
            if (encoding == null) {
                throw new SAXException("Frame encoding type is missing");
            }

            if (encoding.equals("reed-solomon") || encoding.equals("reed-soloman")) {
                currentTf.setEncoding(EncodingType.REED_SOLOMON);
            } else if (encoding.equals("checksum")) {
                
                /* There is no longer
                 * a CHECKSUM encoding type, which was never an encoding
                 * in the first place.  All CHECKSUM meant was that
                 * the frame was unencoded and had a 16-bit FECF.
                 */
                currentTf.setEncoding(EncodingType.UNENCODED);
                currentTf.setFrameErrorControlSizeBits(16);
            } else if (encoding.equals("none")) {
                currentTf.setEncoding(EncodingType.UNENCODED);
            } else if (encoding.equals("turbo")) {
                final String rateStr = attr.getValue("rate");
                final String[] pieces = rateStr.split("/");
                final int rate = Integer.parseInt(pieces[1]);
                switch(rate) {
                case 2:   
                    currentTf.setEncoding(EncodingType.TURBO_1_2);
                    break;
                case 3:   
                    currentTf.setEncoding(EncodingType.TURBO_1_3);
                    break;
                case 4:
                    /*  Added handling for 1/4 turbo rate.
                     * 
                     * corrected from TURBO_1_3 to TURBO_1_4
                     */
                    currentTf.setEncoding(EncodingType.TURBO_1_4);
                    break;
                case 6:   
                    currentTf.setEncoding(EncodingType.TURBO_1_6);
                    break;

                }

            } else {
                throw new SAXException("Illegal frame encoding type: " + encoding);
            }
            currentTf.setCADUSizeBits(XmlUtility.getIntFromAttr(attr, "size")) ;

            if (attr.getValue("encodedSize") != null) {
                currentTf.setEncodedCADUSizeBits(XmlUtility.getIntFromAttr(attr, "encodedSize"));
            }
            else {
                tracer.debug("Transfer frame definition " + currentTf.getName() + " doesn't specify encodedSize: " +
                        "Using unencoded CADU size value of " + currentTf.getCADUSizeBits());
                currentTf.setEncodedCADUSizeBits(currentTf.getCADUSizeBits());
            }

        }
        if (qname.equals("asm")) {
            try {
                currentAsmSize = XmlUtility.getIntFromAttr(attr, "bit_size");
            } catch(final NumberFormatException e) {
                throw new SAXException("illegal asm size");
            }
            try {
                currentAsmActualSize = XmlUtility.getIntFromAttr(attr, "actual_size");
            } catch(final NumberFormatException e) {
                throw new SAXException("illegal asm actual size.");
            }
        }
    }

    /**
     * {@inheritDoc}
     * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void endElement(final String uri, final String localName, final String qname)
            throws SAXException {

        final String inText = XmlUtility.normalizeWhitespace(text.toString().trim());

        if (qname.equals("transfer_frame")) {
            /*  End of XML. Copy parsed definitions to
             * the member array in the enclosing class.
             */
            tff = getTransferFrameFormatArray();
        }
        if (qname.equals("Frame")) {
            /*
             * The XML attributes'elements referenced
             * here do not appear to be used by any transfer_frame.xml file we have. 
             */

            /* Removed setting of TF version. */

            /*  Extensive changes below to account for new interfaces.
             * If using this older implementation, the assumed frame format must be 
             * configured in the dictionary properties.
             */
            currentTf.setHeaderErrorControlSizeBits(0);
            currentTf.setOperationalControlSizeBits(0);
            currentTf.setSecondaryHeaderSizeBits(0);
            final IFrameFormatDefinition.TypeName assumedType = getDictionaryConfiguration().getAssumedFrameFormat();
            switch (assumedType) {
            case CCSDS_AOS_2_MPDU:
                currentTf.setPrimaryHeaderSizeBits(48);
                currentTf.setPduHeaderSizeBits(16);
                break;
            case CCSDS_TM_1:
                currentTf.setPrimaryHeaderSizeBits(48);
                currentTf.setPduHeaderSizeBits(0);
                break;
            case CUSTOM_CLASS:
            case CCSDS_AOS_2_BPDU:
            case UNKNOWN:
            default:
                error("Assumed frame format " + assumedType + " is not supported by this version of the frame dictionary");
                break;
            }
            final IFrameFormatDefinition format = new FrameFormatDefinition(assumedType);
            currentTf.setFormat(format);
            currentTf.setDataAreaSizeBits(Byte.SIZE * (currentTf.getCADUSizeBytes() - currentTf.getASMSizeBytes() 
                    - currentTf.getPrimaryHeaderSizeBytes() - currentTf.getPduHeaderSizeBytes() 
                    - currentTf.getEncodingSizeBytes() - currentTf.getFrameErrorControlSizeBytes()));
            
            validateFrame(currentTf);
            parsedTfList.add(currentTf);
            currentTf = null;
        }
        if (qname.equals("enc_size")) {
            int value = 0;
            try {
                value = XmlUtility.getIntFromText(inText);
            } catch (final NumberFormatException e) {
                throw new SAXException("Expected enc_size"
                        + " to be an integer, was " + text);
            }
            if (value >= 16) {
                switch (currentTf.getEncoding()) {
                case ANY_TURBO:
                case TURBO_1_2:
                case TURBO_1_3:
                case TURBO_1_4:
                case TURBO_1_6:
                    /* 3/29/16. Encoding size no longer covers the FECF in the
                     * new interface.  If there was a previous enc_size for turbo frames,
                     * we assume that 16 bits of it was FECF and the rest are likely trellis 
                     * bits.
                     */
                    currentTf.setFrameErrorControlSizeBits(16);
                    currentTf.setEncodingSizeBits(value - 16);
                    break;
                case REED_SOLOMON:
                    currentTf.setEncodingSizeBits(value);
                    break;
                case BYPASS:
                case UNENCODED:
                default:
                    break;

                }
            }
        }
        if (qname.equals("turbo_trellis_bits")) {
            int ttb = 0;
            try {
                ttb = XmlUtility.getIntFromText(inText);
            } catch(final NumberFormatException e) {
                throw new SAXException("Expected Turbo Trellis Bits to be a number, was " + inText);
            }
            currentTf.setEncodingSizeBits(ttb);
        }
        if (qname.equals("asm")) {
            
            /*  Note that in the new interfaces, there
             * is no "actual ASM size" vs ASM bit size. There is only a flag indicating
             * of the frame arrives with ASM. Logic below has been adjusted accordingly.
             */
            final StringBuilder tmp = new StringBuilder();
            int off = 0;
            final byte[] foo = new byte[(currentAsmActualSize/8)];
            for (int i = 0; i < (currentAsmActualSize/8); i += 1) {
                tmp.append("0x");
                tmp.append(inText.substring(2 * i, (2 * i) + 2));
                try {
                    foo[off++] = (byte )GDR.parse_int(tmp.toString());
                } catch(final NumberFormatException e) {
                    throw new SAXException("Expected hex string for asm, was " + inText);
                }
                tmp.delete(0, tmp.length());                
            }
            if ( 0 != currentAsmSize ) {
                if ( currentAsmSize != currentAsmActualSize ) {
                    throw new SAXException("The ASM actual_size attribute must equal the ASM bit_size attribute unless the ASM bit_size attribute has a value of zero.");
                }
            }
            currentTf.setASM(foo);
            if (currentAsmSize == 0) {
                currentTf.setArrivesWithASM(false);
            }
            currentTf.setASMSizeBits(currentAsmActualSize);
        }
    }

    /**
     * Validates a frame definition.
     * 
     * @param def the definition to validate
     * @throws SAXParseException if there is a validation error
     * 
     */
    private void validateFrame(final ITransferFrameDefinition def) throws SAXParseException {
        final int totalSize = def.getASMSizeBytes() + def.getTotalHeaderSizeBytes() + def.getDataAreaSizeBytes() +
                def.getFrameErrorControlSizeBytes() + def.getOperationalControlSizeBytes() +
                def.getEncodingSizeBytes();
        if (def.getCADUSizeBytes() != totalSize) {
            error("Transfer frame definition for frame " + def.getName() + " is inconsistent. Byte size of the various frame fields (" +
                totalSize + ") does not" +
                " add up to the CADU size (" + def.getCADUSizeBytes() + ")");
        }
    }

}
