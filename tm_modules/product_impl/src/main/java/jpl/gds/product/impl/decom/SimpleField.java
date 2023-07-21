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
package jpl.gds.product.impl.decom;

import java.io.IOException;
import java.io.PrintStream;

import jpl.gds.common.eu.IEUCalculationFactory;
import jpl.gds.dictionary.api.EnumerationDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.dictionary.api.eu.EUGenerationException;
import jpl.gds.dictionary.api.eu.IEUCalculation;
import jpl.gds.dictionary.api.eu.IEUDefinition;
import jpl.gds.eha.api.channel.IChannelValueFactory;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.product.api.decom.BaseDecomDataType;
import jpl.gds.product.api.decom.DecomDataType;
import jpl.gds.product.api.decom.IProductDecomFieldFactory;
import jpl.gds.product.api.decom.ISimpleField;
import jpl.gds.product.api.decom.ProductDecomFieldType;
import jpl.gds.product.api.decom.ProductDecomTimestampType;
import jpl.gds.product.api.decom.formatter.IDecomOutputFormatter;
import jpl.gds.product.impl.message.ProductChannelTimeMessage;
import jpl.gds.product.impl.message.ProductChannelValueMessage;
import jpl.gds.shared.channel.ChannelIdException;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.sys.SystemUtilities;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.Sclk;
import jpl.gds.shared.time.TimeUnit;
import jpl.gds.shared.types.ByteArraySlice;
import jpl.gds.shared.types.ByteStream;
import jpl.gds.shared.util.ByteOffset;

/**
 * DefinitionField represents a decom field definition that describes a value
 * that is of a primitive data type and is byte aligned.
 * 
 */
public class SimpleField extends AbstractDecomField implements ISimpleField {

    private static Tracer log = TraceManager.getDefaultTracer();


    private final DecomDataType dataType;
    private String unit;
    private EnumerationDefinition lookup;
    private IEUDefinition dnToEu;
    private IEUCalculation eu;
    private String euUnits;
    private String euFormat;
    private int prefixLength = -1;
    private ISimpleField lengthField;
    private final boolean shouldDoChannels;
    private ProductDecomTimestampType timeType;
    private String channelId;
    private boolean isAChannel;
    private Number lengthValue;
    private final IChannelDefinitionProvider chanTable;


    /**
     * Creates an instance of SimpleField.
     * 
     * @param name the name of the field
     * @param type the dictionary type of the field
     * @param chanTable the channel definition provider
     * @param shouldDoChannels true if channels should be extracted during decom
     */
    @Deprecated
    public SimpleField(final String name, final DecomDataType type, final IChannelDefinitionProvider chanTable, final boolean shouldDoChannels) {
        super(ProductDecomFieldType.SIMPLE_FIELD);
        setName(name);
        dataType = type;
        this.chanTable = chanTable;
        this.shouldDoChannels = shouldDoChannels;
        
    }

    /**
     * Creates an instance of SimpleField with an implicit length prefix.
     * 
     * @param name the name of the field
     * @param dataType the dictionary type of the field
     * @param lengthLength the byte length of the prefix
     * @param chanTable the channel definition provider
     * @param shouldDoChannels true if channels should be extracted during decom
     * @param fieldFactory factory for making product decom field objects
     */
    @Deprecated
    public SimpleField(final String name, final DecomDataType dataType, final IChannelDefinitionProvider chanTable, final boolean shouldDoChannels, final int lengthLength, final IProductDecomFieldFactory fieldFactory) {
        this(name, dataType, chanTable, shouldDoChannels);

        prefixLength = lengthLength;
        if (prefixLength != -1) {
            lengthField = fieldFactory.createSimpleField("lengthPrefix", new DecomDataType(BaseDecomDataType.UNSIGNED_INT, prefixLength * 8), null, false);
        }
    }

    /**
     * Gets the value of the field if it is used as the length for another field.
     * @return field value as an int
     */
    @Override
    public int getLengthValue() {
        if (lengthValue == null) {
            return 0;
        }
        return lengthValue.intValue();
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.eu.IEUSupport#getEuUnits()
     */
    @Override
    public String getEuUnits() {
        return euUnits;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.eu.IEUSupport#setEuUnits(java.lang.String)
     */
    @Override
    public void setEuUnits(final String euUnit) {
        euUnits = euUnit;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.eu.IEUSupport#getEuFormat()
     */
    @Override
    public String getEuFormat() {
        return euFormat;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.eu.IEUSupport#setEuFormat(java.lang.String)
     */
    @Override
    public void setEuFormat(final String euFormat) {
        this.euFormat = euFormat;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.eu.IEUSupport#getDnToEu()
     */
    @Override
    public IEUDefinition getDnToEu() {
        return dnToEu;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.eu.IEUSupport#setDnToEu(IEUDefinition)
     */
    @Override
    public void setDnToEu(final IEUDefinition dnToEuCalc) {
        dnToEu = dnToEuCalc;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.eu.IEUSupport#setHasEu(boolean)
     * 
     * 5/26/14. Added to meet interface. Does nothing.
     * Call setDnToEu() to set an EU conversion on this object.
     */
    @Override
    public void setHasEu(final boolean yes) {
        SystemUtilities.doNothing();
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.eu.IEUSupport#hasEu()
     */
    @Override
    public boolean hasEu() {
        return dnToEu != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isChannelTimestamp() {
        return timeType != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProductDecomTimestampType getChannelTimeType() {
        return timeType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setChannelTimeType(final ProductDecomTimestampType type) {
        timeType = type;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setIsChannel(final boolean chan) {
        isAChannel = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isChannel() {
        return isAChannel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getChannelId() {
        return channelId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setChannelId(final String channelId) {
        this.channelId = channelId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DecomDataType getDataType() {
        return dataType;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.ILookupSupport#setLookupTable(jpl.gds.dictionary.impl.impl.api.EnumerationDefinition)
     */
    @Override
    public void setLookupTable(final EnumerationDefinition table) {
        lookup = table;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUnit(final String unit) {
        if ((unit != null) && (unit.length() == 0)) {
            this.unit = null;
        } else {
            this.unit = unit;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUnit() {
        return unit;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getResolvedValue(final Object value, final IDecomOutputFormatter out) {
        Object nv = null;
        try {
            if (lookup != null && value instanceof Number) {
                Object lookupValue = lookup.getValue(((Number) value).intValue());
                if (lookupValue == null) {
                    lookupValue = value.toString();
                }
                nv = lookupValue;
            } else if (dnToEu != null && value instanceof Number) {
                if (eu == null) {
                    eu = out.getApplicationContext().getBean(IEUCalculationFactory.class).createEuCalculator(dnToEu);
                }
                nv = eu.eu(((Number) value).doubleValue());
                if (printFormat != null) {
                	nv = out.getPrintFormatter().anCsprintf(printFormat, nv);
                } 
            } else if (printFormat != null) {
            	nv = out.getPrintFormatter().anCsprintf(printFormat, value);
            }
        } catch (final EUGenerationException e) {
            log.warn("DN to EU conversion failed during decom: " + e.toString());
        }
        return nv;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int printValue(final ByteStream stream, final IDecomOutputFormatter out, final int depth)
            throws IOException {
        if (name.equalsIgnoreCase("fill")
                || dataType.equals(new DecomDataType(BaseDecomDataType.FILL))) {
            long len = getValueSize();
            if (len == -1) {
                len = stream.remainingBytes();
            }
            stream.skip(len);
            return (int) len;
        }

        if (getValueSize() > stream.remainingBytes()) {
            log.warn("DefinitionField is past end of data");
            final long len = (int) stream.remainingBytes();
            stream.skip(len);
            return (int) len;
        }

        final long origOffset = stream.getOffset();
        final DecomDataType tempType = new DecomDataType(dataType.getBaseType(), dataType.getBitLength());

        if (lengthField != null) {
            if (lengthField.getValueSize() > stream.remainingBytes()) {
                log.warn("DefinitionField prefix is past end of data");
                final long len = (int) stream.remainingBytes();
                stream.skip(len);
                return (int) len;
            }
            final Number length = (Number) this.getValue(lengthField
                    .getDataType(), stream);
            tempType.setByteLength(length.intValue());

        } else if (dataType.getByteLength() == -1) {
            tempType.setByteLength((int) stream.remainingBytes());
        }

        if (tempType.getByteLength() > stream.remainingBytes()) {
            log.warn("DefinitionField is past end of data");
            final long len = (int) stream.remainingBytes();
            stream.skip(len);
            return (int) len;
        }

        Object value = null;

        value = getValue(tempType, stream);

        final long length = stream.getOffset() - origOffset;

        Object nv = getResolvedValue(value, out);
        if (nv == null) {
            nv = value;
        }

        if (unit != null) {
            out.nameValue(name, nv.toString(), unit);
        } else {
            out.nameValue(name, nv.toString());
        }

        // If the field is a channel, generate a channel message
        if (shouldDoChannels) {
        	final IMessagePublicationBus bus = out.getMessagePublicationBus();

            if (this.getChannelTimeType() != null) {

                final ProductChannelTimeMessage pcm = new ProductChannelTimeMessage();

                if (dataType.getBaseType().equals(BaseDecomDataType.TIME)) {
                    pcm.setBitSize(this.getValueSize() * 8);
                    pcm.setTimeType(timeType);
                    if (unit != null) {
                        try {
                            final TimeUnit unitEnum = Enum.valueOf(TimeUnit.class, unit);
                            pcm.setUnit(unitEnum);
                            pcm.setIsSclk(unitEnum.isSclkBased());
                            if (unitEnum.isSclkBased()) {
                                pcm.setIsSclk(true);
                                pcm.setSclkTime((ISclk)value);
                            } else {
                                pcm.setIsSclk(false);
                                pcm.setIsoTime(new AccurateDateTime((Long)value));
                            }
                        } catch (final IllegalArgumentException e) {
                            pcm.setUnit(TimeUnit.SCLK);
                            pcm.setIsSclk(true);
                            pcm.setSclkTime((ISclk)value);
                        }
                    } else {
                        pcm.setUnit(TimeUnit.SCLK);
                        pcm.setIsSclk(true);
                        pcm.setSclkTime((ISclk)value);
                    }
                    bus.publish(pcm);

                } else if (dataType.equals(DecomDataType.FLOAT_64)) {
                    pcm.setBitSize(this.getValueSize() * 8);
                    pcm.setTimeType(timeType);
                    pcm.setUnit(TimeUnit.SCLK);
                    pcm.setIsSclk(true);
                    value = new Sclk((Double)value);
                    pcm.setSclkTime((ISclk)value);
                    bus.publish(pcm);
                } else {
                    log.warn("Channel time type is set on non-time field");
                }
            } else {
                if (channelId != null) {
                    try {
                        log.debug("Processing DPO field for channel ID " + channelId);
                        
                        final IChannelDefinition chanDef = chanTable.getDefinitionFromChannelId(channelId);
                        
                        if (chanDef != null) {
                            final IServiceChannelValue chanVal = out.getApplicationContext().getBean(IChannelValueFactory.class).createServiceChannelValue(chanDef);
                            chanVal.setDn(value);

                            /*
                             * No longer compute EU here. EU computation is now done
                             * by the EhaPublisherUtility.
                             */
                  
                            // publish product channel message
                            final ProductChannelValueMessage pcm = new ProductChannelValueMessage(chanVal);
                            bus.publish(pcm);

                        } else {
                            log.debug("NOT processing product field for channel ID " + channelId + ": no definition");
                            // apparently field is not a channel
                        }
                    } catch (final ChannelIdException e) {
                        log.debug("NOT processing product field for channel name " + channelId + ": not a valid channel ID");
                    }
                }
            }
        }

        return (int) length;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.product.impl.decom.AbstractDecomField#printType(java.io.PrintStream, int)
     */
    @Override
    public void printType(final PrintStream out, final int depth) throws IOException {
        printIndent(out, depth);
        final StringBuilder output = new StringBuilder(64);
        output.append("(" + dataType + ") " + name);
        if (unit != null) {
            output.append(" (" + unit + ")");
        }
        if (timeType != null) {
            output.append(" [this is a " + timeType.toString() + " channel timestamp]");
        }
        if (channelId != null) {
            output.append(" [this is a channel (" + channelId + ")]");
        }
        out.println(output);
    }


    /**
     * {@inheritDoc}
     * @see jpl.gds.product.impl.decom.AbstractDecomField#getValueSize()
     */
    @Override
    public int getValueSize() {
        if (prefixLength != -1) {
            return prefixLength;
        } else if (dataType.getByteLength() == -1) {
            return 0;
        } else {
            return dataType.getByteLength();
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.ILookupSupport#getLookupTable()
     */
    @Override
    public EnumerationDefinition getLookupTable() {
        return lookup;
    }

    /**
     * Gets the field object for the prefix field for this field.
     * 
     * @return the length field object for this field
     */
    @Override
    public ISimpleField getLengthField() {
        return lengthField;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getValue(final DecomDataType dataType, final ByteStream stream) {
        final Object val = super.getValue(dataType, stream);
        if (val != null && val instanceof Number) {
            lengthValue = (Number)val;
        }
        return val;
    }

    /**
     * Gets the value of a numeric field in the given data slice.
     * 
     * @param dataType the dictionary definition type of the data
     * @param data the ByteArraySlice to read the value from
     * @param newOffset a ByteOffset object used to track position in the
     *            ByteArraySlice; will be updated to reflect the number of bytes
     *            read
     * @return the field value as a Number object; the type will vary based upon
     *         the field type
     */
    @Override
    protected Object getValue(final DecomDataType dataType, final ByteArraySlice data,
            final ByteOffset newOffset) {
        if (dataType.getBaseType().equals(BaseDecomDataType.TIME)) {
            newOffset.inc(dataType.getByteLength());
            switch (dataType.getBitLength()) {
            case 32:
                if (unit != null && unit.equals("MS")) {
                    return GDR.get_u32(data.array, data.offset);
                } else {
                    return new Sclk(GDR.get_u32(data.array, data.offset), 0);
                }
            case 64:
                if (unit != null && unit.equals("MS")) {
                    return GDR.get_u64(data.array, data.offset);
                } else {
                    long fine = GDR.get_u32(data.array, data.offset + 4);
                    fine = (fine >> 16) & 0x000000000000FFFF;
                    return new Sclk(GDR.get_u32(data.array, data.offset),fine);
                }
            default:
                throw new IllegalStateException(
                        "Unsupported time bit length in field definition: "
                                + dataType.getBitLength());
            }
        } else {
            return super.getValue(dataType, data, newOffset);
        }
    }
}
