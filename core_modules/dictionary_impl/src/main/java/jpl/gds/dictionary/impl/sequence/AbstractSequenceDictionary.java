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
package jpl.gds.dictionary.impl.sequence;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.dictionary.api.sequence.ISequenceDictionary;
import jpl.gds.dictionary.impl.AbstractBaseDictionary;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.TraceSeverity;

/**
 * Base class for implementation of sequence dictionary parsers. This class
 * should be extended by mission-specific implementations. <br>
 * At this time, this class parses XML corresponding to the MSL/M2020 sequence
 * category file schema. There is no multimission schema.
 * 
 *
 *
 */
public abstract class AbstractSequenceDictionary extends AbstractBaseDictionary implements ISequenceDictionary {

    /**
     * Character length of the sequence number in the sequence display string.
     */
    protected static final int SEQUENCE_NUMBER_DISPLAY_LENGTH = 5;
    
    /**
     * Bit length of the category ID in the numeric sequence ID.
     */
    protected static final int CATEGORY_ID_LENGTH = 6;
    
    /**
     * Bit length of the sequence number in the numeric sequence ID.
     */
    protected static final int SEQUENCE_NUMBER_LENGTH = 14;
    
    /**
     * Bit length of left-pad in the numeric sequence ID.
     */
    protected static final int EXTRA_SPACE_LENGTH = 12;

    /**
     * Mapping of sequence category number to category name.
     */
    protected Map<Integer, String> sequenceMapping = new HashMap<Integer, String>();
    
    /**
     * Package protected constructor.
     * 
     * @param maxSchemaVersion the maximum schema version supported
     * 
     */
    AbstractSequenceDictionary(String maxSchemaVersion) {
        super(DictionaryType.SEQUENCE, maxSchemaVersion);
    }


    /**
     * {@inheritDoc}
     * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    @Override
    public void startElement(final String namespaceURI, final String qname,
            final String rawName, final Attributes atts)
                    throws SAXException
    {
        super.startElement(namespaceURI, qname, rawName, atts);
        
        if(qname.equals("sequenceCategories"))
        {
            return;
        }

        if(qname.equals("categories")) {
            /*
             * chill_get_products query output begins with INFO msg on parsing sequenceCategories.xml
             * Do not need to publish an External Message to this effect, simply log it.
             * Use log severity level of DEBUG to prevent it from being displayed in data output.
             */
            setGdsVersionId(atts.getValue("version"));
            reportHeaderInfo(TraceSeverity.DEBUG); // Honor change from MBI above
           
        }

        if(qname.equals("sequenceCategory")) {
            String categoryName = atts.getValue("categoryName");
            int categoryNumber = GDR.parse_int(atts.getValue("categoryNumber"));

            sequenceMapping.put(Integer.valueOf(categoryNumber), categoryName);
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.IBaseDictionary#clear()
     */
    @Override
    public void clear() {
        sequenceMapping.clear();
        super.clear();
    }


    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.sequence.ISequenceDictionary#getCategoryMap()
     */
    @Override
    public Map<Integer, String> getCategoryMap() {
        return sequenceMapping;
    }


    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.sequence.ISequenceDictionary#getCategoryNameByCategoryId(int)
     */
    @Override
    public String getCategoryNameByCategoryId(final int catId) {
        return sequenceMapping.get(catId);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.sequence.ISequenceDictionary#getCategoryIdByCategoryName(java.lang.String)
     */
    @Override
    public Integer getCategoryIdByCategoryName(final String category) {
        for(Entry<Integer, String> entry: sequenceMapping.entrySet()) {
            if (entry.getValue().equals(category)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.sequence.ISequenceDictionary#getCategoryIdFromSeqId(int)
     */
    @Override
    public int getCategoryIdFromSeqId(final int seqid) {
        final byte[] bytes = ByteBuffer.allocate(4).putInt(seqid).array();
        return GDR.get_u8(bytes, 1, 4, CATEGORY_ID_LENGTH);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.sequence.ISequenceDictionary#getSequenceNumberFromSeqId(int)
     */
    @Override
    public int getSequenceNumberFromSeqId(final int seqid) {
        final byte[] bytes = ByteBuffer.allocate(4).putInt(seqid).array();
        return GDR.get_u16(bytes, 2, 2, SEQUENCE_NUMBER_LENGTH);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.sequence.ISequenceDictionary#getSeqNameFromSeqIdBytes(byte[])
     */
    @Override
    public String getSeqNameFromSeqIdBytes(byte[] parameter) {
        //32-bit value
        byte[] byteArray = parameter;
        int offsetByte = 0;

        //should be a 32-bit number 12-bits are 0s, 6-bit category, 14-bit sequence id
        //12-bit sequence version
        int extraDigits = GDR.get_u16(byteArray, offsetByte, 0, EXTRA_SPACE_LENGTH);

        if(extraDigits != 0) {
            return "";
        }

        //6-bit category id (0 to 63). this number needs to be mapped to category name
        offsetByte = 1;
        int categoryId = GDR.get_u8(byteArray, offsetByte, 4, CATEGORY_ID_LENGTH);

        //14-bit sequence id - still in 1st byte
        offsetByte = 2;
        int sequenceId = GDR.get_u16(byteArray, offsetByte, 2, SEQUENCE_NUMBER_LENGTH);

        //map category id to category name
        String categoryName = sequenceMapping.get(categoryId);
        
        if (categoryName == null) {
            categoryName = "Unknown";
        }

        // remove extra space at front of string for proper formatting
        /*  Removed extra space at front of string. */
        return "(" + categoryName + padNumberWithZeros(String.valueOf(sequenceId), SEQUENCE_NUMBER_DISPLAY_LENGTH) + ")";
    }

    private String padNumberWithZeros(String number, int paddedLength) {

        for(int i=0; i<paddedLength; i++) {
            if(number.length() >= paddedLength) {
                break;
            }	
            number = "0" + number;
        }
        return number;
    }
}
