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
package jpl.gds.session.validation;


/**
 * Constraints on parameter values. It holds various stuff that may or may not
 * pertain to any specific value type. Minimum and maximum is used both for
 * long values and string lengths.
 *
 */
public final class Constraints extends AbstractParameter
{
    /** Does a file have to exist or not ? */
    public static enum FileExistEnum
    {
        /** A file must exist */
        MUST_EXIST,

        /** A file must not exist */
        MUST_NOT_EXIST,

        /** A file may exist */
        MAY_EXIST;
    }


    private static final String ME                 = "Constraints: ";
    private static final long   MIN_DEFAULT_LONG   = 0L;
    private static final long   MAX_DEFAULT_LONG   = Long.MAX_VALUE;
    private static final double MIN_DEFAULT_DOUBLE = 0.0D;
    private static final double MAX_DEFAULT_DOUBLE = Double.POSITIVE_INFINITY;

    private static final UppercaseBool DEFAULT_UPPERCASE =
        UppercaseBool.DO_NOT_UPPERCASE;

    private static final FileExistEnum DEFAULT_FILEEXIST =
        FileExistEnum.MAY_EXIST;

    private static final DirectoryFileBool DEFAULT_DIRECTORY =
        DirectoryFileBool.REGULAR_FILE;

    private final long              _minimum;
    private final long              _maximum;
    private final double            _dMinimum;
    private final double            _dMaximum;
    private final UppercaseBool     _uppercase;
    private final FileExistEnum     _mustExist;
    private final DirectoryFileBool _directory;


    /**
     * Full constructor.
     *
     * @param minimum   Minimum
     * @param maximum   Maximum
     * @param dMinimum  Double minimum
     * @param dMaximum  Double maximum
     * @param uppercase Uppercase the string value?
     * @param mustExist Must the file exist?
     * @param directory Must the file be a directory?
     *
     * @throws ParameterException On empty range
     */
    public Constraints(final long              minimum,
                       final long              maximum,
                       final double            dMinimum,
                       final double            dMaximum,
                       final UppercaseBool     uppercase,
                       final FileExistEnum     mustExist,
                       final DirectoryFileBool directory)
        throws ParameterException
    {
        super();

        _minimum   = minimum;
        _maximum   = maximum;
        _dMinimum  = dMinimum;
        _dMaximum  = dMaximum;
        _uppercase = checkNull(ME, uppercase, "Uppercase");
        _mustExist = checkNull(ME, mustExist, "Must exist");
        _directory = checkNull(ME, directory, "Directory");

        if (_minimum > _maximum)
        {
            throw new ParameterException(ME + "Long range cannot be empty");
        }

        // Picks up NaN's as well
        if (! (_dMinimum <= _dMaximum))
        {
            throw new ParameterException(ME + "Double range cannot be empty");
        }
    }


    /**
     * Constructor.
     *
     * @param minimum   Minimum
     * @param maximum   Maximum
     * @param uppercase Uppercase the string value?
     * @param mustExist Must the file exist?
     * @param directory Must the file be a directory?
     *
     * @throws ParameterException On empty range
     */
    public Constraints(final long              minimum,
                       final long              maximum,
                       final UppercaseBool     uppercase,
                       final FileExistEnum     mustExist,
                       final DirectoryFileBool directory)
        throws ParameterException
    {
        this(minimum,
             maximum,
             MIN_DEFAULT_DOUBLE,
             MAX_DEFAULT_DOUBLE,
             uppercase,
             mustExist,
             directory);
    }


    /**
     * Constructor.
     *
     * @param minimum   Minimum
     * @param maximum   Maximum
     * @param uppercase Uppercase the string value?
     *
     * @throws ParameterException On empty range
     */
    public Constraints(final long          minimum,
                       final long          maximum,
                       final UppercaseBool uppercase)
        throws ParameterException
    {
        this(minimum,
             maximum,
             MIN_DEFAULT_DOUBLE,
             MAX_DEFAULT_DOUBLE,
             uppercase,
             DEFAULT_FILEEXIST,
             DEFAULT_DIRECTORY);
    }


    /**
     * Constructor.
     *
     * @param minimum Minimum
     * @param maximum Maximum
     *
     * @throws ParameterException On empty range
     */
    public Constraints(final long minimum,
                       final long maximum)
        throws ParameterException
    {
        this(minimum,
             maximum,
             MIN_DEFAULT_DOUBLE,
             MAX_DEFAULT_DOUBLE,
             DEFAULT_UPPERCASE,
             DEFAULT_FILEEXIST,
             DEFAULT_DIRECTORY);
    }


    /**
     * Constructor.
     *
     * @param minimum Minimum
     * @param maximum Maximum
     *
     * @throws ParameterException On empty range
     */
    public Constraints(final double minimum,
                       final double maximum)
        throws ParameterException
    {
        this(MIN_DEFAULT_LONG,
             MAX_DEFAULT_LONG,
             minimum,
             maximum,
             DEFAULT_UPPERCASE,
             DEFAULT_FILEEXIST,
             DEFAULT_DIRECTORY);
    }


    /**
     * Constructor. Essentially a dummy for when no constraints are needed.
     * We cannot use the main constructor because we do not want to allow an
     * exception to be thrown.
     */
    public Constraints()
    {
        super();

        _minimum   = MIN_DEFAULT_LONG;
        _maximum   = MAX_DEFAULT_LONG;
        _dMinimum  = MIN_DEFAULT_DOUBLE;
        _dMaximum  = MAX_DEFAULT_DOUBLE;
        _uppercase = DEFAULT_UPPERCASE;
        _mustExist = DEFAULT_FILEEXIST;
        _directory = DEFAULT_DIRECTORY;
    }


    /**
     * Getter for minimum.
     *
     * @return Minimum
     */
    public long getMinimum()
    {
        return _minimum;
    }


    /**
     * Getter for maximum.
     *
     * @return Maximum
     */
    public long getMaximum()
    {
        return _maximum;
    }


    /**
     * Getter for double minimum.
     *
     * @return Minimum
     */
    public double getDoubleMinimum()
    {
        return _dMinimum;
    }


    /**
     * Getter for double maximum.
     *
     * @return Maximum
     */
    public double getDoubleMaximum()
    {
        return _dMaximum;
    }


    /**
     * Getter for uppercase.
     *
     * @return Uppercase state
     */
    public UppercaseBool getUppercase()
    {
        return _uppercase;
    }


    /**
     * Getter for must-exist.
     *
     * @return Must-exist state
     */
    public FileExistEnum getMustExist()
    {
        return _mustExist;
    }


    /**
     * Getter for directory.
     *
     * @return Directory state
     */
    public DirectoryFileBool getDirectory()
    {
        return _directory;
    }


    /**
     * Check in-bounds.
     *
     * @param value Long value to check
     *
     * @return boolean
     */
    public boolean checkBounds(final long value)
    {
        return ((value >= _minimum) && (value <= _maximum));
    }


    /**
     * Check in-bounds.
     *
     * @param value Double value to check
     *
     * @return boolean
     */
    public boolean checkBounds(final double value)
    {
        return ((value >= _dMinimum) && (value <= _dMaximum));
    }


    /**
     * Check in-bounds.
     *
     * @param value String value to check
     *
     * @return boolean
     */
    public boolean checkBounds(final String value)
    {
        return ((value != null) && checkBounds(value.length()));
    }
}
