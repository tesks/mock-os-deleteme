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
package jpl.gds.dictionary.impl.command;

import jpl.gds.dictionary.api.KeyValueAttributes;
import jpl.gds.dictionary.api.command.*;
import jpl.gds.shared.time.CoarseFineEncoding;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.Sclk;
import jpl.gds.shared.time.TimeProperties;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the implementation object for the command dictionary
 * ICommandArgumentDefinition interface. This class holds all of the dictionary
 * configuration for a command argument.
 * <p>
 * Instances of this class should never be created directly. Use
 * CommandArgumentDefinitionFactory.
 * 
 *
 * @see CommandArgumentDefinitionFactory
 * 
 */
public class CommandArgumentDefinition implements ICommandArgumentDefinition {

    /** 
     * The length of argument in bits 
     */
    private int bitLength;

    /** 
     * The dictionary name of this argument 
     */
    private String dictionaryName;

    /** 
     * The FSW name of this argument 
     */
    private String fswName;

    /**
     * The default value of this argument (if the user doesn't supply a value)
     * or null if this argument has no default value.
     */
    private String defaultValue;

    /**
     * Data type of this argument. Will be overridden by subclasses upon object
     * instantiation.
     */
    private CommandArgumentType argType = CommandArgumentType.UNDEFINED;

    /**
     * Description of this argument. May be null.
     */
    private String description;

    /**
     * The set of allowable ranges for this argument's value
     */
    private List<IValidationRange> ranges = new ArrayList<IValidationRange>();

	/**
	 *  Key-value attribute map to hold the keyword name and value of any project-
	 *  specific information.
	 *  
	 */	
	private KeyValueAttributes keyValueAttr = new KeyValueAttributes();

    /** 
     * The table of enumeration values for this argument 
     */
    private CommandEnumerationDefinition enumeration;

    /**
     * Units for this argument, May be null.
     */
    private String units;

    /**
     * Validation pattern for this argument. May be null.
     */
    private String valueRegexp;

    /**
     * Length of argument value prefix. Used for variable length arguments that
     * are prefixed by length in the encoded command.
     */
    private int prefixBitLength;
    
    private static final CoarseFineEncoding sclkEncoding = TimeProperties.getInstance().getCanonicalEncoding();

    /**
     * Constructor.
     * 
     * @param type
     *            the data type enumeration value for this argument.
     * 
     */
    CommandArgumentDefinition(final CommandArgumentType type) {

        if (type == null || type == CommandArgumentType.UNDEFINED) {
            throw new IllegalArgumentException("Command argument type may not be null or UNDEFINED");
        }

        this.argType = type;

        /*
         * Define an enumeration only for enumerated arguments. Will be null for
         * all other types.
         */
        if (this.argType.isEnumeration()) {
            enumeration = new CommandEnumerationDefinition("Default");
        } else {
            enumeration = null;
        }

        /*
         * Boolean arguments are actually enumerations. Create the default
         * enumeration table.
         */
        if (this.argType.equals(CommandArgumentType.BOOLEAN)) {
            CommandParserUtil.setBooleanEnumValues(this,
                    CommandParserUtil.TRUE_STRING,
                    CommandParserUtil.FALSE_STRING);
        }

        /*
         * These are the defaults for variable length string maximum bit length
         * and prefix length.
         */
        if (this.argType.equals(CommandArgumentType.VAR_STRING)) {
            this.bitLength = 1024 * 8;
            this.prefixBitLength = 16;
        }

    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.command.ICommandArgumentDefinition#getType()
     */
    @Override
    public CommandArgumentType getType() {

        return this.argType;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.command.ICommandArgumentDefinition#setType(jpl.gds.dictionary.impl.impl.api.command.CommandArgumentType)
     */
    @Override
    public void setType(final CommandArgumentType type) {

        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        this.argType = type;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.command.ICommandArgumentDefinition#getRanges()
     */
    @Override
    public List<IValidationRange> getRanges() {

        return this.ranges;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.command.ICommandArgumentDefinition#addRange(jpl.gds.dictionary.impl.impl.api.command.IValidationRange)
     */
    @Override
    public void addRange(final IValidationRange r) {

        if (r == null) {
            throw new IllegalArgumentException("Null input range");
        }

        if (this.ranges.contains(r) == false) {
            if (MINIMUM_STRING.equalsIgnoreCase(r.getMinimum())) {
                r.setMinimum(String.valueOf(getMinimumValue()));
            }

            if (MAXIMUM_STRING.equalsIgnoreCase(r.getMaximum())) {
                r.setMaximum(String.valueOf(getMaximumValue()));
            }

            this.ranges.add(r);
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.command.ICommandArgumentDefinition#clearRanges()
     */
    @Override
    public void clearRanges() {

        this.ranges = new ArrayList<IValidationRange>();
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.command.ICommandArgumentDefinition#getRangeString(boolean)
     */
    @Override
    public String getRangeString(final boolean includeUnits) {

        StringBuilder rangeValue = new StringBuilder(128);


        String units = getUnits();
        if (units != null && includeUnits == true) {
            rangeValue.append(units);
            rangeValue.append(" (");
        }

        java.util.List<IValidationRange> ranges = getRanges();
        if (!ranges.isEmpty()) {
            for (int i = 0; i < ranges.size(); i++) {
                IValidationRange range = ranges.get(i);
                rangeValue.append(range.getMinimum());
                rangeValue.append(" to ");
                rangeValue.append(range.getMaximum());
                if (i != (ranges.size() - 1)) {
                    rangeValue.append(", ");
                }
            }
        } else if (this.argType.equals(CommandArgumentType.TIME)) {
            if (getBitLength() == sclkEncoding.getCoarseBits()) {
                rangeValue.append(0);
                rangeValue.append(" to ");
                rangeValue.append(sclkEncoding.getMaxCoarse());
            } else if (getBitLength() == sclkEncoding.getBitLength()) {
                rangeValue.append(Sclk.MIN_SCLK);
                rangeValue.append(" to ");
                rangeValue.append(new Sclk(sclkEncoding.getMaxCoarse(), sclkEncoding.getMaxFine(), sclkEncoding));
            }

        } else {
            rangeValue.append(getMinimumValue());
            rangeValue.append(" to ");
            rangeValue.append(getMaximumValue());
        }

        if (units != null && includeUnits == true) {
            rangeValue.append(")");
        }

        return (rangeValue.toString());
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.command.ICommandArgumentDefinition#getDescription()
     */
    @Override
    public String getDescription() {

        return this.description;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.command.ICommandArgumentDefinition#setDescription(java.lang.String)
     */
    @Override
    public void setDescription(final String desc) {

        this.description = desc;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.command.ICommandArgumentDefinition#getDefaultValue()
     */
    @Override
    public String getDefaultValue() {

        return this.defaultValue;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.command.ICommandArgumentDefinition#setDefaultValue(java.lang.String)
     */
    @Override
    public void setDefaultValue(final String value) {

        this.defaultValue = value;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.command.ICommandArgumentDefinition#setDictionaryName(java.lang.String)
     */
    @Override
    public void setDictionaryName(final String dictionaryName) {

        this.dictionaryName = dictionaryName;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.command.ICommandArgumentDefinition#getDictionaryName()
     */
    @Override
    public String getDictionaryName() {

        return this.dictionaryName;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.command.ICommandArgumentDefinition#getFswName()
     */
    @Override
    public String getFswName() {

        return this.fswName;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.command.ICommandArgumentDefinition#setFswName(java.lang.String)
     */
    @Override
    public void setFswName(final String fswName) {

        this.fswName = fswName;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.command.ICommandArgumentDefinition#getBitLength()
     */
    @Override
    public int getBitLength() {

        return (this.bitLength);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.command.ICommandArgumentDefinition#setBitLength(int)
     */
    @Override
    public void setBitLength(final int l) {

        if (l < 0) {
            throw new IllegalArgumentException("Can't have a negative length!");
        }

        this.bitLength = l;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.command.ICommandArgumentDefinition#isVariableLength()
     */
    @Override
    public boolean isVariableLength() {

        return (this.argType.isVariableLength());
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.command.ICommandArgumentDefinition#setTypeName(java.lang.String)
     */
    @Override
    public void setTypeName(String type) {

        this.enumeration.setName(type);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.command.ICommandArgumentDefinition#getTypeName()
     */
    @Override
    public String getTypeName() {

        return this.enumeration.getName();
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.command.ICommandArgumentDefinition#getEnumeration()
     */
    @Override
    public CommandEnumerationDefinition getEnumeration() {

        return this.enumeration;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.command.ICommandArgumentDefinition#setEnumeration(jpl.gds.dictionary.impl.impl.api.command.CommandEnumerationDefinition)
     */
    @Override
    public void setEnumeration(final CommandEnumerationDefinition def) {

        this.enumeration = def;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.command.ICommandArgumentDefinition#getPrefixBitLength()
     */
    @Override
    public int getPrefixBitLength() {

        return this.prefixBitLength;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.command.ICommandArgumentDefinition#setPrefixBitLength(int)
     */
    @Override
    public void setPrefixBitLength(final int prefixBitLength) {

        this.prefixBitLength = prefixBitLength;
    }


    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.command.ICommandArgumentDefinition#setUnits(java.lang.String)
     */
    @Override
    public void setUnits(final String units) {

        this.units = units;
    }

    /**
     * {@inheritDoc}
     * 
     * This implementation filters out the various ways the "no unit" may be
     * specified in the dictionary (none, n/a, etc) and returns null for these
     * cases.
     * 
     * @see jpl.gds.dictionary.impl.impl.api.command.ICommandArgumentDefinition#getUnits()
     * 
     */
    @Override
    public String getUnits() {

        if (this.units != null) {
            this.units = this.units.trim();
            if (this.units.length() > 0
                    && "none".equalsIgnoreCase(this.units) == false
                    && "n/a".equalsIgnoreCase(this.units) == false
                    && "undefined".equalsIgnoreCase(this.units) == false
                    && "null".equalsIgnoreCase(this.units) == false) {
                return (this.units);
            }
        }

        return (null);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.command.ICommandArgumentDefinition#getMinimumValue()
     */
    @Override
    public Object getMinimumValue() {

        if (this.argType.isEnumeration()) {
            /* 
             * The minimum value for an enumerated argument is the 
             * minimum bit value in the enumeration. 
             */
            return this.enumeration.getMinimumValue();

        } else if (this.argType.equals(CommandArgumentType.FLOAT)) {
            /* 
             * The minimum value of a floating point is NEGATIVE_INFINITY.
             * The reason we do not use MAX_VALUE instead is because the range
             * is displayed in the GUI, and the resulting range is really ugly
             * if we use MIN/MAX value for floats. 
             */
            switch (this.bitLength) {
            case Float.SIZE:
                return (Float.NEGATIVE_INFINITY);

            case Double.SIZE:
                return (Double.NEGATIVE_INFINITY);

            default:
                break;
            }

            /*
             * Should never get here, but return NaN if we do.
             */
            return (Double.NaN);

        } else if (this.argType.equals(CommandArgumentType.INTEGER)) {

            /*
             * Minimum value of a signed integer is the largest negative number
             * that fits in the bit size of the value.
             */
            return (Long.valueOf((long) (Math.pow(-2, getBitLength() - 1))));

        } else if (this.argType.isTime()) {

            /*
             * Minimum value of a floating point time is the float equivalent
             * of the minimum SCLK value.
             */
            if (this.argType.isFloatTime()) {
                return (Sclk.MIN_SCLK.getFloatingPointTime());
            }

            /*
             * Minimum value of a SCLK time, if the value is large enough to hold
             * the entire SCLK, is the GDR long equivalent of the whole minimim SCLK.
             */
            if (this.bitLength == sclkEncoding.getBitLength()) {
                return (Sclk.MIN_SCLK.getBinaryGdrLong());
            }

            /*
             * Minimum value of a SCLK time, if the value is only large enough
             * to hold coarse SCLK, is the minimum coarse SCLK.
             */
            if (this.bitLength == sclkEncoding.getCoarseBits()) {
                ISclk minSclkNoFine = new Sclk(Sclk.MIN_SCLK.getCoarse(),0);
                return(minSclkNoFine.getBinaryGdrLong());
            }

            /*
             * This should never happen, but if we fall through to here, we return
             * the long equivalent of the whole minimum SCLK.
             */
            return (Sclk.MIN_SCLK.getBinaryGdrLong());

        } else if (this.argType.equals(CommandArgumentType.UNSIGNED)
                || this.argType.equals(CommandArgumentType.REPEAT)) {

            /* 
             * The minimum value for unsigned arguments, and for repeat arguments, is 0.
             * In the case of repeat arguments, this is the minimum number of repeats.
             */
            return 0L;
        }

        return null;

    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.command.ICommandArgumentDefinition#getMaximumValue()
     */
    @Override
    public Object getMaximumValue() {

        if (this.argType.isEnumeration()) {
            /* 
             * The maximum value for an enumerated argument is the 
             * maximum value in the enumeration. 
             */
            return this.enumeration.getMaximumValue();

        } else if (this.argType.equals(CommandArgumentType.FLOAT)) {
            switch (this.bitLength) {            
            /* 
             * The maximum value of a floating point is POSITIVE_INFINITY.
             * The reason we do not use MAX_VALUE instead is because the range
             * is displayed in the GUI, and the resulting range is really ugly
             * if we use MIN/MAX value for floats. 
             */
            case Float.SIZE:
                return (Float.POSITIVE_INFINITY); 

            case Double.SIZE:
                return (Double.POSITIVE_INFINITY);

            default:
                break;
            }

            /*
             * Should never get here, but return NaN if we do.
             */
            return (Double.NaN);

        } else if (this.argType.equals(CommandArgumentType.INTEGER)) {

            /*
             * Maximum value of a signed integer is the largest signed positive number
             * that fits in the bit size of the value.
             */
            return (Long.valueOf((long) (Math.pow(2, getBitLength() - 1) - 1)));

        } else if (this.argType.isTime()) {

            /*
             * Maximum value of a floating point time is the float equivalent
             * of the maximum SCLK value.
             */
            if (this.argType.isFloatTime()) {
                return (TimeProperties.getInstance().getMaxCanonicalSclk().getFloatingPointTime());
            }

            /*
             * Maximum value of a SCLK time, if the value is large enough to hold
             * the entire SCLK, is the GDR long equivalent of the whole maximum SCLK.
             */
            if (this.bitLength == sclkEncoding.getBitLength()) {
                return (TimeProperties.getInstance().getMaxCanonicalSclk().getBinaryGdrLong());
            }

            /*
             * Maximum value of a SCLK time, if the value is only large enough
             * to hold coarse SCLK, is the maximum coarse SCLK.
             */
            if (this.bitLength == sclkEncoding.getCoarseBits()) {
                ISclk maxSclkNoFine = new Sclk(TimeProperties.getInstance().getMaxCanonicalSclk().getCoarse(), 0);
                return(maxSclkNoFine.getBinaryGdrLong());
            }

            /*
             * This should never happen, but if we fall through to here, we return
             * the long equivalent of the whole maximum SCLK.
             */           
            return TimeProperties.getInstance().getMaxCanonicalSclk().getBinaryGdrLong();

        } if (this.argType.equals(CommandArgumentType.UNSIGNED)) {

            /* 
             * JFWagner - 5/28/2020 - For unsigned values we need to be careful to pick types that won't cap the
             * larger values we may need to use. The "long" primitive caps out at (2^64 - 1), but unsigned 64-bit numbers
             * can exceed that limit. In order to accommodate these bigger numbers, we need to use
             * a combination of BigDecimal and BigInteger.
             */
            return (new BigDecimal(Math.pow(2, getBitLength()))).subtract(new BigDecimal(1)).toBigInteger();

        } else if (this.argType.equals(CommandArgumentType.REPEAT)) {

            /*
             * The maximum value for unsigned arguments, and for repeat arguments, is
             * the largest unsigned number than can fit in the bit length.
             * In the case of repeat arguments, this is the maximum number of repeats.
             */
            return (Long.valueOf((long) Math.pow(2, getBitLength()) - 1));
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.command.ICommandArgumentDefinition#getValueRegexp()
     */
    @Override
    public String getValueRegexp() {

        return this.valueRegexp;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.command.ICommandArgumentDefinition#setValueRegexp(java.lang.String)
     */
    @Override
    public void setValueRegexp(final String valueRegexp) {

        if (valueRegexp != null && valueRegexp.length() == 0) {
            this.valueRegexp = null;
            return;
        }

        this.valueRegexp = valueRegexp;
    }

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.IAttributesSupport#setKeyValueAttribute(java.lang.String, java.lang.String)
	 */
	@Override	
	public void setKeyValueAttribute(String key, String value) {
		keyValueAttr.setKeyValue(key, value);
	}
	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.IAttributesSupport#getKeyValueAttribute(java.lang.String)
	 */
	@Override
	
	public String getKeyValueAttribute(String key) {
		return keyValueAttr.getValueForKey(key);
	}
	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.IAttributesSupport#getKeyValueAttributes()
	 */
	@Override	
	public KeyValueAttributes getKeyValueAttributes() {
		return keyValueAttr;
	}
	
	/**
	 *  {@inheritDoc}
	 *  @see jpl.gds.dictionary.impl.impl.api.IAttributesSupport#setKeyValueAttribute(java.lang.String, java.lang.String)
	 * 
	 */
	@Override	
	public void setKeyValueAttributes(KeyValueAttributes kvAttr) {
		keyValueAttr.copyFrom(kvAttr);
	}

	/** {@inheritDoc}
	 *
	 * @see jpl.gds.dictionary.impl.impl.api.IAttributesSupport#clearKeyValue()
	 */
	@Override
	public void clearKeyValue() {
		keyValueAttr.clearKeyValue();
	}


}
