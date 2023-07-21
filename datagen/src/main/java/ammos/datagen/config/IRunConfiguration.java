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
package ammos.datagen.config;

/**
 * This interface is to be implemented by all Run Configuration classes. It
 * exists to provide common configuration methods that apply to all Run
 * Configuration classes, and to provide constants for all common mission
 * configuration property names.
 * 
 */
public interface IRunConfiguration extends IXmlConfiguration {
	/**
	 * Configuration property name for the 1 byte integer seed table file path
	 * (string)
	 */
	public static final String INTEGER_8_SEED_TABLE = "Integer8SeedTable";
	/**
	 * Configuration property name for the 2 byte integer seed table file path
	 * (string)
	 */
	public static final String INTEGER_16_SEED_TABLE = "Integer16SeedTable";
	/**
	 * Configuration property name for the 4 byte integer seed table file path
	 * (string)
	 */
	public static final String INTEGER_32_SEED_TABLE = "Integer32SeedTable";
	/**
	 * Configuration property name for the 8 byte integer seed table file path
	 * (string)
	 */
	public static final String INTEGER_64_SEED_TABLE = "Integer64SeedTable";
	/**
	 * Configuration property name for the integer traversal type, indicating
	 * whether to generate integer values sequentially or randomly
	 * (TraversalType).
	 */
	public static final String INTEGER_TRAVERSAL_TYPE = "IntegerTraversalType";
	/**
	 * Configuration property name for the 1 byte unsigned seed table file path
	 * (string)
	 */
	public static final String UNSIGNED_8_SEED_TABLE = "Unsigned8SeedTable";
	/**
	 * Configuration property name for the 2 byte unsigned seed table file path
	 * (string)
	 */
	public static final String UNSIGNED_16_SEED_TABLE = "Unsigned16SeedTable";
	/**
	 * Configuration property name for the 4 byte float seed table file path
	 * (string)
	 */
	public static final String FLOAT_32_SEED_TABLE = "Float32SeedTable";
	/**
	 * Configuration property name for the 8 byte float seed table file path
	 * (string)
	 */
	public static final String FLOAT_64_SEED_TABLE = "Float64SeedTable";
	/**
	 * Configuration property name for the float traversal type, indicating
	 * whether to generate unsigned values sequentially or randomly
	 * (TraversalType).
	 */
	public static final String FLOAT_TRAVERSAL_TYPE = "FloatTraversalType";
	/**
	 * Configuration property name for the configuration flag indicating whether
	 * to add NaN and Infinite values among values generated for floating point
	 * fields (boolean).
	 */
	public static final String INCLUDE_NAN_INFINITE_FLOATS = "IncludeNaNInfiniteFloats";
	/**
	 * Configuration property name for the 4 byte unsigned seed table file path
	 * (string)
	 */
	public static final String UNSIGNED_32_SEED_TABLE = "Unsigned32SeedTable";
	/**
	 * Configuration property name for the 8 byte unsigned seed table file path
	 * (string)
	 */
	public static final String UNSIGNED_64_SEED_TABLE = "Unsigned64SeedTable";
	/**
	 * Configuration property name for the unsigned traversal type, indicating
	 * whether to generate unsigned values sequentially or randomly
	 * (TraversalType).
	 */
	public static final String UNSIGNED_TRAVERSAL_TYPE = "UnsignedTraversalType";
	/**
	 * Configuration property name for initial SCLK Coarse value (long)
	 */
	public static final String INITIAL_SCLK_COARSE = "InitialSclkCoarse";
	/**
	 * Configuration property name for the SCLK Fine value (long)
	 */
	public static final String INITIAL_SCLK_FINE = "InitialSclkFine";
	/**
	 * Configuration property name for the SCLK Coarse value delta (long)
	 */
	public static final String SCLK_COARSE_DELTA = "SclkCoarseDelta";
	/**
	 * Configuration property name for the SCLK Fine value delta (long)
	 */
	public static final String SCLK_FINE_DELTA = "SclkFineDelta";
	/**
	 * Configuration property name for the SCLK seed table (string).
	 */
	public static final String SCLK_SEED_TABLE = "SclkSeedTable";
	/**
	 * Configuration property name for the flag indicating whether to stop
	 * generating packets when values in the SCLK seed table are exhausted
	 * (boolean).
	 */
	public static final String SCLK_STOP_WHEN_EXHAUSTED = "StopWhenExhausted";
	/**
	 * Configuration property name for the flag indicating whether to include a
	 * single non-zero or one value when generating boolean values (boolean).
	 */
	public static final String INCLUDE_NON_ZERO_ONE_BOOL = "IncludeNonZeroOrOne";
	/**
	 * Configuration property name for the boolean traversal type, indicating
	 * whether to generate boolean values sequentially or randomly
	 * (TraversalType).
	 */
	public static final String BOOL_TRAVERSAL_TYPE = "BooleanTraversalType";
	/**
	 * Configuration property name for the flag indicating whether to include
	 * invalid enumeration values in generated enum fields (boolean).
	 */
	public static final String INCLUDE_INVALID_ENUMS = "IncludeInvalidEnums";
	/**
	 * Configuration property name for approximate percentage of invalid enums
	 * to generate (float).
	 */
	public static final String INVALID_ENUM_PERCENT = "InvalidEnumPercent";
	/**
	 * Configuration property name for the enum traversal type, indicating
	 * whether to generate enum values sequentially or randomly (TraversalType).
	 */
	public static final String ENUM_TRAVERSAL_TYPE = "EnumTraversalType";
	/**
	 * Configuration property name for the flag indicating whether to include
	 * empty strings in generated string fields (boolean).
	 */
	public static final String INCLUDE_EMPTY_STRINGS = "IncludeEmptyStrings";
	/**
	 * Configuration property name for the flag indicating whether to include
	 * null characters in generated string fields (boolean).
	 */
	public static final String INCLUDE_NULL_CHAR = "IncludeNullCharacter";
	/**
	 * Configuration property name for the character set used for generating
	 * string fields (string).
	 */
	public static final String STRING_CHAR_SET = "StringCharacterSet";
	/**
	 * Configuration property name for the maximum string length to use for
	 * generated string fields (integer).
	 */
	public static final String STRING_MAX_LEN = "StringMaxLength";
	/**
	 * Configuration property name for the string traversal type, indicating
	 * whether to generate string values sequentially or randomly
	 * (TraversalType).
	 */
	public static final String STRING_TRAVERSAL_TYPE = "StringTraversalType";
	/**
	 * Configuration property name for desired size of the packet output file
	 * (long).
	 */
	public static final String DESIRED_FILE_SIZE = "DesiredFileSize";
	
    /* MPCS-9375 - 1/3/18 - Support generation of multiple files. Add constant. */
	/**
     * Configuration property name for desired size of the packet output file
     * (integer).
     */
    public static final String DESIRED_NUM_FILES = "DesiredNumFiles";

	/**
	 * Configuration property name for desired progress report interval.
	 * (integer).
	 */
	public static final String DESIRED_REPORT_INTERVAL = "DesiredReportInterval";
	/**
	 * Configuration property name for desired fill packet percentage. (float).
	 */
	public static final String DESIRED_FILL_PERCENT = "DesiredFillPercent";
	/**
	 * Configuration property name for desired number of packets. (integer).
	 * 
	 * MPCS-6864 - 11/21/14. Added constant.
	 */
	public static final String DESIRED_NUM_PACKETS = "DesiredNumPackets";

}
