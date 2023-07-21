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
package jpl.gds.dictionary.api.command;



/**
 * This is a generic representation of a valid range for a numeric command
 * argument. Other values may change, but a range should always have a max and
 * min. Other range classes may extend this common class for mission-specific 
 * implementations.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * 
 *
 */
public class CommonValidationRange implements IValidationRange
{

    /** The minimum value for this range */
    protected String minimum;

    /** The maximum value for this range */
    protected String maximum;

    /**
     * 
     * Creates an instance of Common Validation Range.
     */
    public CommonValidationRange()
    {
        this.minimum = null;
        this.maximum = null;
    }

    /**
     * Creates an instance of CommonValidationRange that encompasses a single
     * value (ranges are inclusive).
     * 
     * @param value for min and max
     */
    public CommonValidationRange(final String value)
    {
        setMinimum(value);
        setMaximum(value);
    }

    /**
     * 
     * Creates an instance of AbstractRange.
     * 
     * @param mi
     *            The initial minimum
     * @param ma
     *            The initial maximum
     */
    public CommonValidationRange(final String mi, final String ma)
    {
        setMinimum(mi);
        setMaximum(ma);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.api.command.IValidationRange#getMinimum()
     */
    @Override
    public String getMinimum() {
        return (this.minimum);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.api.command.IValidationRange#getMaximum()
     */
    @Override
    public String getMaximum() {
        return (this.maximum);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.api.command.IValidationRange#setMinimum(java.lang.String)
     */
    @Override
    public final void setMinimum(final String mi) {

        if(mi == null)
        {
            throw new IllegalArgumentException("Null input minimum value");
        }

        this.minimum = mi.trim();
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.api.command.IValidationRange#setMaximum(java.lang.String)
     */
    @Override
    public final void setMaximum(final String ma) {

        if(ma == null)
        {
            throw new IllegalArgumentException("Null input maximum value");
        }

        this.maximum = ma.trim();
    }

    /**
     * Create a deep copy of this range object
     * 
     * @return A deep copy of this object
     */
    public CommonValidationRange copy()
    {
        CommonValidationRange range = new CommonValidationRange();
        setSharedValues(range);
        return(range);
    }

    /**
     * Copy the values from this range into the input range.
     * 
     * @param range The range whose values should be set to the values
     * of this range.
     */
    protected void setSharedValues(final CommonValidationRange range)
    {
        range.minimum = this.minimum;
        range.maximum = this.maximum;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return(this.minimum + " <= ? <= " + this.maximum);
    }
}