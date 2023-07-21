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
package jpl.gds.shared.string;


/**
 * Library of static string utilities. Final because all static.
 * 
 */
public final class StringUtil
{
    /** SQL text bounds
     *  Pure ANSI 7-bit w/o control characters or delete
     */
	/** Minimum SQL character code - ANSI 7-bit */
    public static final char SQL_MIN_CHAR = (char)  32; // Blank

	/** Minimum SQL character code - ANSI 7-bit */
    public static final char SQL_MAX_CHAR = (char) 126; // Tilde

	/** Unicode NULL */
	private static final String UNICODE_NULL = "\u0000";

	/**
	 * Constructor.
	 * 
	 * Never instantiated.
	 */
	private StringUtil()
    {
		super();
	}


	/**
	 * Remove quotes from string if quoted.
	 * 
	 * @param testString
	 *            String to examine
	 * 
	 * @return Resulting string
	 */
	public static String removeQuotes(final String testString) {
		if (!isQuoted(testString)) {
			return (testString);
		}

		return (testString.substring(1, testString.length() - 1));
	}

	/**
	 * Get whether string is quoted.
	 * 
	 * @param testString
	 *            String to examine
	 * 
	 * @return True if quoted
	 */
	public static boolean isQuoted(final String testString) {
		if (testString == null || testString.length() <= 1) {
			return (false);
		}

		final char firstChar = testString.charAt(0);
		final char lastChar = testString.charAt(testString.length() - 1);

		return ((firstChar == '\'' && lastChar == '\'') || (firstChar == '"' && lastChar == '"'));
	}

	/**
	 * Count the number of occurences of character 'x' in the string.
	 * 
	 * @param testString
	 *            Where to look
	 * @param x
	 *            Character to look for
	 * 
	 * @return The number of times that 'x' occurs in the string 'testString'
	 */
	public static int count(final String testString, final char x) {
		int count = 0;
		for (int i = 0; i < testString.length(); i++) {
			if (testString.charAt(i) == x) {
				count++;
			}
		}

		return (count);
	}


	/**
	 * Trim string if not null. Preserve null.
	 * 
	 * @param s String
	 * 
	 * @return Trimmed String or null if originally null
	 */
	public static String trimPreserveNull(final String s)
    {
        return ((s != null) ? s.trim() : null);
	}


	/**
	 * Trim string, and if null, return as empty.
	 * 
	 * @param s
	 *            String
	 * 
	 * @return Non-null trimmed String
	 */
	public static String safeTrim(final String s) {
		return ((s != null) ? s.trim() : "");
	}


	/**
	 * Trim string, and if empty, return as null.
	 * 
	 * @param s
	 *            String
	 * 
	 * @return Trimmed String or null
	 */
	public static String emptyAsNull(final String s) {
		if (s == null) {
			return null;
		}

		final String ss = s.trim();

		if (ss.length() == 0) {
			return null;
		}

		return ss;
	}

	/**
	 * Trim and uppercase string, and if empty, return as empty.
	 * 
	 * @param s
	 *            String
	 * 
	 * @return Cleaned string
	 */
	public static String safeTrimAndUppercase(final String s) {
		return ((s != null) ? s.trim().toUpperCase() : "");
	}

	/**
	 * Remove whitespace and uppercase string, and if empty, return as null.
	 * 
	 * @param s String
	 * 
	 * @return Cleaned string
	 */
	public static String safeCompressAndUppercase(final String s) {
		return ((s != null) ? s.replaceAll("[\\s]+", "").toUpperCase() : "");
	}


    /**
     * Test for a string as null, or empty.
     *
     * @param s String to test
     *
     * @return True if meets condition
     */
    public static boolean isNullOrEmpty(final String s)
    {
        return ((s == null) || s.isEmpty());
    }


    /**
     * Test for a string as null, or empty when trimmed.
     *
     * @param s String to test
     *
     * @return True if meets condition
     */
    public static boolean isNullOrEmptyTrimmed(final String s)
    {
        return ((s == null) || s.trim().isEmpty());
    }

    /**
     * Binary null-safe equalsIgnoreCase.
     *
     * @param left  One string
     * @param right Other string
     *
     * @return True if equal
     */
    public static boolean equalsIgnoreCase(final String left,
                                           final String right)
    {
	    if (left == null || right == null) {
		    return false;
	    }

        if (left.equals(right)) {
            return true;
        }

        return left.equalsIgnoreCase(right);
    }


    /**
     * Check SQL text for characters out of bounds. This method is used for
     * non-LDI checking of VARCHAR insertion. Also see BytesBuilder.
     *
     * @param s String to check
     *
     * @return Original string
     */
    public static String checkSqlText(final String s)
    {

        if (s == null)
        {
            return s;
        }

        final int len = s.length();

        for (int i = 0; i < len; ++i)
        {
            final char c = s.charAt(i);

            if ((c < SQL_MIN_CHAR) || (c > SQL_MAX_CHAR))
            {
                throw new IllegalArgumentException("Characters must be pure ANSI 7-bit w/o control characters or delete");
            }
        }
        return s;
    }

	/** Clean up string - for now just remove unicode NULLs
	 * @param str String to be cleaned
	 */
	public static String cleanUpString(final String str){
		String cleaned = str;
		if(str.contains(UNICODE_NULL)) {
			//clean up null characters, only if present
			cleaned = str.replace(UNICODE_NULL, "");
		}
		return cleaned;
	}
}
