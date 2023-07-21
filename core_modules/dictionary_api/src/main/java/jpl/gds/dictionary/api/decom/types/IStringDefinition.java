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
package jpl.gds.dictionary.api.decom.types;


/**
 * Represents string data in a generic decom map.  String data is
 * a sequence of characters or code points that should be represented textually.
 * 
 * Strings in decom data can be encoding in several ways; see {@link StringEncoding} for
 * supported encodings. 
 * 
 * Strings can be either sized statically or null terminated. 
 *
 */
public interface IStringDefinition extends IStorableDataDefinition {

	/**
	 * Enum defining supported types of string encodings.
	 *
	 */
	public enum StringEncoding {
		/**
		 * Each byte in string data is 1 byte long.
		 */
		ASCII,
		/**
		 * UTF-8 unicode. Each element in a string is a variable
		 * length unicode code point.
		 */
		UTF8
	}
	/**
	 * Get the encoding to use to represent the string data.
	 * @return the string encoding
	 */
	public StringEncoding getEncoding();
	
	/**
	 * Get the length of the string in bytes.
	 * Use {@link #isNullTerminated()} to determine whether the length
	 * is absolute or a maximum limit. 
	 * @return the maximum length of the string if the string is null
	 * 		   terminated, or the absolute length if the string is not null
	 * 		   terminated.
	 */
	public int getLength();
	
	/**
	 * Determine whether the string is null terminated. Null terminated
	 * strings may be variable length, but need to define a maximum length.
	 * If a string is null terminated, the string should be extracted until encountering
	 * a null (0x00) character.
	 * @return true if the string is null terminated, otherwise false
	 */
	public boolean isNullTerminated();

}
