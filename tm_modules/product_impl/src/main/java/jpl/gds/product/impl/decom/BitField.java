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
import jpl.gds.dictionary.api.eu.EUGenerationException;
import jpl.gds.dictionary.api.eu.IEUCalculation;
import jpl.gds.dictionary.api.eu.IEUDefinition;
import jpl.gds.product.api.decom.BaseDecomDataType;
import jpl.gds.product.api.decom.DecomDataType;
import jpl.gds.product.api.decom.IBitField;
import jpl.gds.product.api.decom.ProductDecomFieldType;
import jpl.gds.product.api.decom.formatter.IDecomOutputFormatter;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.sys.SystemUtilities;
import jpl.gds.shared.types.ByteArraySlice;
import jpl.gds.shared.types.ByteStream;

/**
 * BitField represents a decom definition that describes a decom field that is a
 * series of bits.
 *
 */
public class BitField extends AbstractDecomField implements IBitField {

    private String unit;
    private EnumerationDefinition lookup;
    private IEUDefinition dnToEu;
    private IEUCalculation eu;
    private final int bitlength;
    private String euUnit;
    private String euFormat;
    

    /**
     * Creates an instance of BitField.
     * 
     * @param name the name of the bit field
     * @param bitlength the length of the bit field
     */
    @Deprecated
    public BitField(final String name, final int bitlength) {
        super(ProductDecomFieldType.BIT_FIELD);
        setName(name);
        this.bitlength = bitlength;
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
    public int printValue(final ByteStream stream, final IDecomOutputFormatter out, final int depth)
                                                                            throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public int printValue(final ByteArraySlice data, final int bitoffset,
            final IDecomOutputFormatter out, final int depth) throws IOException {
        if (name.equalsIgnoreCase("fill")) {
            if (bitlength == -1) {
                return (data.length * 8) - bitoffset;
            } else {
                return bitlength;
            }
        }
        final Number value = getBitValue(bitoffset, bitlength, data);

        final Object nv = getResolvedValue(value, out);

        out.nameValue(name, nv.toString(), unit);

        return bitlength;
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
	        } else {
	            nv = value.toString();
	        }
        } catch (final EUGenerationException e) {
        	TraceManager.getDefaultTracer().warn("DN to EU conversion failed during decom: " + e.toString());

        	nv = value;
        }
        return nv;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.product.impl.decom.AbstractDecomField#printType(java.io.PrintStream, int)
     */
    @Override
    public void printType(final PrintStream out, final int depth) throws IOException {
        out.println("(BitField: " + bitlength + ") " + name);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.product.impl.decom.AbstractDecomField#getValueSize()
     */
    @Override
    public int getValueSize() {
        if (bitlength == -1) {
            return -1;
        }
        return bitlength / 8;
    }

    /**
     * Get the number of bits in this bit field
     * 
     * @return the field length in bits
     */
    public int getBitLength() {
        return bitlength;
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
    public void setDnToEu(final IEUDefinition dnToEu) {
        this.dnToEu = dnToEu;

    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.eu.IEUSupport#setHasEu(boolean)
     * 
     *  5/26/14. Added to meet interface. Does nothing.
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
     * @see jpl.gds.dictionary.impl.impl.api.eu.IEUSupport#getEuUnits()
     */
    @Override
	public String getEuUnits() {
        return euUnit;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.eu.IEUSupport#setEuUnits(java.lang.String)
     */
    @Override
	public void setEuUnits(final String unitStr) {
        euUnit = unitStr;        
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
	public void setEuFormat(final String formatStr) {
        euFormat = formatStr;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public DecomDataType getDataType() {
        return new DecomDataType(BaseDecomDataType.UNSIGNED_INT, bitlength);
    }
}