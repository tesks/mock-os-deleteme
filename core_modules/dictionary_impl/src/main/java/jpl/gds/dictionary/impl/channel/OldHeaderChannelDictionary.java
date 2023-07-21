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
package jpl.gds.dictionary.impl.channel;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.channel.DerivationType;
import jpl.gds.dictionary.api.channel.FrameHeaderFieldName;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDictionary;
import jpl.gds.dictionary.api.channel.IHeaderChannelDefinition;
import jpl.gds.dictionary.api.channel.PacketHeaderFieldName;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.shared.xml.XmlUtility;

/**
 * OldHeaderChannelDictionary implements the IChannelDictionary interface for the
 * multi-mission ground generated header channels. It parses the information in
 * a header channel definition file (header_channel.xml). This parser is for the
 * old, AMPCS proprietary header channel schema. That's why it's called "old".
 * 
 *
 * @see IChannelDictionary
 *
 */
public class OldHeaderChannelDictionary extends AbstractChannelDictionary {
    
    /* Get schema version from config */
    private static final String MM_SCHEMA_VERSION = 
            DictionaryProperties.getMultimissionDictionaryVersion(DictionaryType.HEADER);

    private static final String CHANNEL_ID = "channel_id";
    private static final String IO_FORMAT = "io_format";
    private static final String NAME = "name";
    private static final String COMMENT = "comment";
    private static final String OPS_CAT = "ops_cat";
    private static final String SUBSYSTEM = "subsystem";
    private static final String CHANNEL_NAME = "channel_name";
    private static final String TYPE = "type";
    private static final String ACTIVE = "active";

    private String currentArgName = null;
    
    /**
     * Package protected constructor.
     * 
     */
    OldHeaderChannelDictionary() {
        super(DictionaryType.HEADER, MM_SCHEMA_VERSION);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.impl.channel.AbstractChannelDictionary#clear()
     */
    @Override
    public void clear() {
        super.clear();
        currentArgName = null;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.impl.channel.AbstractChannelDictionary#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    @Override
    public void startElement(final String uri, final String localName, final String qname,
            final Attributes attr) throws SAXException {
        super.startElement(uri, localName, qname, attr);
        
        /*  Use common method to parse and report header info */
        parseMultimissionHeader(localName, attr);

        if (qname.equalsIgnoreCase("header_channel")) {
            final String active = attr.getValue(ACTIVE);
            if (active == null) {
                throw new SAXException("header_channel element is missing active attribute");
            } 

            if ("NO".equalsIgnoreCase(active)) {
                return;
            }
            else if (!"YES".equalsIgnoreCase(active)){
                throw new SAXException("header_channel element's active attribute must be \"YES\" or \"NO\" (parsed \"" + active + "\")");
            } 

            final String id = attr.getValue(CHANNEL_ID);
            if (id == null) {
                throw new SAXException("header_channel element is missing channel_id attribute");
            }

            int index = 0;
            try {
                index = XmlUtility.getIntFromAttr(attr, "measurement_id");
            } catch (final NumberFormatException e) {
                throw new SAXException("header_channel measurement_id attribute must be an integer");
            }

            final String opsCat = attr.getValue(OPS_CAT);
            final String subs = attr.getValue(SUBSYSTEM);
            final String name = attr.getValue(CHANNEL_NAME);
            final String ctype = attr.getValue(TYPE);

            if (ctype == null) {
                throw new SAXException("header_channel type attribute must be supplied");
            }

            String units = attr.getValue("units");
            if (units != null && units.equalsIgnoreCase("UNDEFINED")) {
                units = null;
            }

            final String format = attr.getValue(IO_FORMAT);

            startHeaderChannelDefinition(id, Enum.valueOf(ChannelType.class,ctype));

            if (opsCat != null) {
                currentChannel.setCategory(IChannelDefinition.OPS_CAT, opsCat);
            }
            if (subs != null) {
                currentChannel.setCategory(IChannelDefinition.SUBSYSTEM, subs);
            }
            currentChannel.setIndex(index);
            currentChannel.setTitle(name);
            currentChannel.setName(name); /*  Make channel name available */
            currentChannel.setDnUnits(units);
            currentChannel.setDnFormat(format);

        } else if (qname.equalsIgnoreCase("states") && inChannelDefinition()) {
            startStates();
            /* Enums must be named for multimission conversion. */
            currentEnumDef.setName("Enum_" + currentChannel.getId());

        } else if (qname.equalsIgnoreCase("enum") && inStates()) {
            int dn = 0;
            try {
                dn = XmlUtility.getIntFromAttr(attr, "dn");
            } catch (final NumberFormatException e) {
                throw new SAXException("dn value in state enumeration must be an integer");
            }
            setCurrentEnumIndex(dn);


        } else if (qname.equalsIgnoreCase("header_derivation_by_bit_unpacking")) {
            final String id = attr.getValue(CHANNEL_ID);
            if (id == null) {
                throw new SAXException("header_derivation_by_java element is missing channel_id attribute");
            }

            int index = 0;
            try {
                index = XmlUtility.getIntFromAttr(attr, "measurement_id");
            } catch (final NumberFormatException e) {
                throw new SAXException("header_derivation_by_java measurement_id attribute must be an integer");
            }

            final String opsCat = attr.getValue(OPS_CAT);
            final String subs = attr.getValue(SUBSYSTEM);
            final String name = attr.getValue(CHANNEL_NAME);
            final String ctype = attr.getValue(TYPE);

            if (ctype == null) {
                throw new SAXException("header_channel type attribute must be supplied");
            }

            String units = attr.getValue("units");
            if (units != null && units.equalsIgnoreCase("UNDEFINED")) {
                units = null;
            }

            final String format = attr.getValue(IO_FORMAT);

            startHeaderChannelDefinition(id, Enum.valueOf(ChannelType.class,ctype));
            if (opsCat != null) {
                currentChannel.setCategory(IChannelDefinition.OPS_CAT, opsCat);
            }
            if (subs != null) {
                currentChannel.setCategory(IChannelDefinition.SUBSYSTEM, subs);
            }
            currentChannel.setIndex(index);
            currentChannel.setTitle(name);
            currentChannel.setDnUnits(units);
            currentChannel.setDnFormat(format);

            currentChannel.setDerived(true);
            currentChannel.setDerivationType(DerivationType.BIT_UNPACK);

        } else if (qname.equalsIgnoreCase("header_derivation_by_java")) {
            final String id = attr.getValue(CHANNEL_ID);
            if (id == null) {
                throw new SAXException("header_derivation_by_java element is missing channel_id attribute");
            }

            int index = 0;
            try {
                index = XmlUtility.getIntFromAttr(attr, "measurement_id");
            } catch (final NumberFormatException e) {
                throw new SAXException("header_derivation_by_java measurement_id attribute must be an integer");
            }

            final String opsCat = attr.getValue(OPS_CAT);
            final String subs = attr.getValue(SUBSYSTEM);
            final String name = attr.getValue(CHANNEL_NAME);
            final String ctype = attr.getValue(TYPE);

            if (ctype == null) {
                throw new SAXException("header_channel type attribute must be supplied");
            }

            String units = attr.getValue("units");
            if (units != null && units.equalsIgnoreCase("UNDEFINED")) {
                units = null;
            }

            final String format = attr.getValue(IO_FORMAT);

            startHeaderChannelDefinition(id, Enum.valueOf(ChannelType.class,ctype));
            if (opsCat != null) {
                currentChannel.setCategory(IChannelDefinition.OPS_CAT, opsCat);
            }
            if (subs != null) {
                currentChannel.setCategory(IChannelDefinition.SUBSYSTEM, subs);
            }
            currentChannel.setIndex(index);
            currentChannel.setTitle(name);
            currentChannel.setDnUnits(units);
            currentChannel.setDnFormat(format);

            currentChannel.setDerived(true);
            currentChannel.setDerivationType(DerivationType.ALGORITHMIC);

        } else if (qname.equalsIgnoreCase("dn_to_eu") && inChannelDefinition()) {
            currentChannel.setEuUnits(attr.getValue("eu_units"));
            if (currentChannel.getEuUnits() != null && currentChannel.getEuUnits().equals("")) {
                currentChannel.setEuUnits(null);
            }
            currentChannel.setEuFormat(attr.getValue("eu_io_format"));
        }
        else if (qname.equalsIgnoreCase("dn_eu_table") && inChannelDefinition()) {
            startDnToEu();
        }
        else if (qname.equalsIgnoreCase("val") && inDnToEu()) {
            try {
                final double dn = XmlUtility.getDoubleFromAttr(attr, "dn");
                setTableDn(dn);
            } catch (final NumberFormatException e) {
                throw new SAXException("dn value in dn to eu table must be an integer or floating point number");
            }
            try {
                final double eu = XmlUtility.getDoubleFromAttr(attr, "eu");
                setTableEu(eu);
            } catch (final NumberFormatException e) {
                throw new SAXException("eu value in dn to eu table must be an integer or floating point number");
            }
        }

        else if (qname.equalsIgnoreCase("dn_eu_poly") && inChannelDefinition()) {
            startDnToEu();
        }
        else if (qname.equalsIgnoreCase("dn_eu_java") && inChannelDefinition()) {
            startDnToEu();
            final String name = attr.getValue("java_class");
            if (name == null) {
                throw new SAXException("dn_eu_java must have a java_class attribute");
            }
            setAlgorithmName(name);
        }

        else if (qname.equalsIgnoreCase("coeff") && inDnToEu()) {
            try {
                final int offset = XmlUtility.getIntFromAttr(attr, "index");
                setDnToEuPolyIndex(offset);
            } catch (final NumberFormatException e) {
                throw new SAXException("poly index attribute must be an integer");
            }
            try {
                final double val = XmlUtility.getDoubleFromAttr(attr, "val");
                setDnToEuPolyCoefficient(val);
            } catch (final NumberFormatException e) {
                throw new SAXException("poly val attribute must be a floating point number");
            }
        }
        else if (qname.equalsIgnoreCase("bits") && isInBitUnpackDerivation()) {
            int start = 0;
            try {
                if (attr.getValue("start_bit") == null) {
                    throw new SAXException("bits element must have a start_bit attribute");
                }
                start = XmlUtility.getIntFromAttr(attr, "start_bit");
            } catch (final NumberFormatException e) {
                throw new SAXException("start_bits attribute must be an integer");
            }
            int len = 0;
            try {
                if (attr.getValue("num_bits") == null) {
                    throw new SAXException("bits element must have a num_bits attribute");
                }
                len = XmlUtility.getIntFromAttr(attr, "num_bits");
            } catch (final NumberFormatException e) {
                throw new SAXException("start_bits attribute must be an integer");
            }
            addBitUnpackRange(start, len);
        }

        else if (qname.equalsIgnoreCase("derivation_bit") && inChannelDefinition()) {
            final String parent = attr.getValue("parent");
            if (parent == null) {
                throw new SAXException("derivation_bit element must have a parent attribute");
            }
            if (!parent.equals("NONE")) {
                startBitUnpackDerivation(parent);
                addDerivationChild(currentChannel.getId());
            }
        }

        else if (qname.equalsIgnoreCase("derivation_java") && inChannelDefinition())
        {
            final String id = attr.getValue("derivation_id");

            if (id == null)
            {
                throw new SAXException("derivation_java element must have a " +
                        "derivation_id attribute");
            }

            final String javaClass = attr.getValue("java_class");

            if (javaClass == null)
            {
                throw new SAXException("derivation_java element must have a " +
                        "java_class attribute");
            }

            startAlgorithmicDerivation(id);
            setAlgorithmName(javaClass);
            addDerivationChild(currentChannel.getId());
        }
        else if (qname.equalsIgnoreCase("argument") && inAlgorithmDerivation()) {
            final String name = attr.getValue(NAME);

            if (name == null)
            {
                throw new SAXException("argument element must have a " +
                        "name attribute");
            }

            currentArgName = name;
        }
        else if (qname.equalsIgnoreCase("parent") && inAlgorithmDerivation()) {
            final String id = attr.getValue(CHANNEL_ID);
            if (id == null) {
                throw new SAXException("parent element must have channel_id attribute");
            }
            addDerivationParent(id);
        }
    }

    /**
     * {@inheritDoc}
     * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void endElement(final String uri, final String localName, final String qName)
            throws SAXException {
        super.endElement(uri, localName, qName);
        final String ntext = XmlUtility.normalizeWhitespace(text);

        if (inChannelDefinition() &&
                (qName.equalsIgnoreCase("header_channel") ||
                        qName.equalsIgnoreCase("header_derivation_by_java") ||
                        qName.equalsIgnoreCase("header_derivation_by_bit_unpacking"))) {

            endChannelDefinition();

        } else if (qName.equalsIgnoreCase("packet_source") && inChannelDefinition()) {
            try {
                final PacketHeaderFieldName currentPktHdrFieldName = PacketHeaderFieldName.valueOf(ntext);
                ((IHeaderChannelDefinition)currentChannel).setPacketHeaderField(currentPktHdrFieldName);
            } catch (final IllegalArgumentException e) {
                throw new SAXException(qName + " (" + currentChannel.getId() + 
                        ") specifies packet_source but source string does not match a defined packet header field");
            }

        } else if (qName.equalsIgnoreCase("frame_source") && inChannelDefinition()) {
            try {
                final FrameHeaderFieldName currentFrmHdrFieldName = FrameHeaderFieldName.valueOf(ntext);
                ((IHeaderChannelDefinition)currentChannel).setFrameHeaderField(currentFrmHdrFieldName);
            } catch (final IllegalArgumentException e) {
                throw new SAXException(qName + " (" + currentChannel.getId() + 
                        ") specifies frame_source but source string does not match a defined frame header field");
            }

        } else if (qName.equalsIgnoreCase("sfdu_source") && inChannelDefinition()) {
            final String currentSfduHdrFieldId = ntext;
            if (ntext.trim().isEmpty()) {
                throw new SAXException(qName + " (" + currentChannel.getId() + 
                        ") specifies sfdu_source but source string invalid");
            }
            ((IHeaderChannelDefinition)currentChannel).setSfduHeaderField(currentSfduHdrFieldId);

        } else if (qName.equalsIgnoreCase("enum") && inStates()) {
            setCurrentEnumValue(ntext);

        } else if (qName.equalsIgnoreCase("states") && inChannelDefinition()) {
            endStates();

        } else if (qName.equalsIgnoreCase(COMMENT) && inChannelDefinition()) {
            currentChannel.setDescription(ntext);
        }

        else if (qName.equals("dn_eu_table") && inChannelDefinition()) {
            this.endDnToEuTable();
        }

        else if (qName.equalsIgnoreCase("dn_eu_poly") && inChannelDefinition()) {
            endDnToEuPoly();
        }
        else if (qName.equalsIgnoreCase("dn_eu_java") && inChannelDefinition()) {
            endDnToEuAlgorithmic();
        }

        else if (qName.equalsIgnoreCase("derivation_bit") && inChannelDefinition()) {
            endBitUnpackDerivation();
        }

        else if (qName.equalsIgnoreCase("derivation_java") && inChannelDefinition()) {
            endAlgorithmicDerivation();
        }
        else if("argument".equals(qName) && inAlgorithmDerivation())
        {
            addNamedParameter(currentArgName, text.toString());
            currentArgName = null;
        }
    }
}
