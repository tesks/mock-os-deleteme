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

import java.util.LinkedList;
import java.util.List;

import jpl.gds.shared.types.Triplet;

/**
 * Contains static methods used for manipulating printf format strings.
 *  
 * @since AMPCS 6.1
 */
public final class SprintfUtil {
	
    private static final String allowedIntegerFormatPrefixes = "hHlL";
   
    private static final String[] unsupportedFormats = {
        "%scet", "%sclk", "%ert", "%lst", "%enum"
    };
    
    private static final String formatCharacters = "uodxiXfgGeEsc";
    
    private static final String lengthWidthChars = ".+- 01234567890#";

	/**
	 * Reads a print format string and extracts the "letters" associated
	 * with each % formatter found in the string. Does not accept the AMPCS
	 * custom formatters %SCET, %SCLK, %ERT, and %LST. If these are found in the
	 * format string, this method will throw. If other format characters are found
	 * that also are not supported by the AMPCS SprintfFormat class, an exception
	 * is thrown.
	 * 
	 * @param format
	 *            the print format string
	 * @return the list of format "letters" found in the format string
	 * @throws SprintfUtilException if illegal formatters are found in the format string
	 * 
	 */
	public static List<String> getFormatLetters(final String format) throws SprintfUtilException {

		List<String> parameterFormats = new LinkedList<String>();
        List<Triplet<Integer,Integer, String>> triples = getFormatLettersAndPositions(format);
        for (Triplet<Integer,Integer, String> triple: triples) {
        	parameterFormats.add(triple.getThree());
        }
	
        return parameterFormats;
	}
	
	/**
	 * Reads a print format string and extracts the "letters" associated
	 * with each % formatter found in the string. Does not accept the AMPCS
	 * custom formatters %SCET, %SCLK, %ERT, and %LST. If these are found in the
	 * format string, this method will throw. If other format characters are found
	 * that also are not supported by the AMPCS SprintfFormat class, an exception
	 * is thrown.
	 * 
	 * @param format
	 *            the print format string
	 * @return the list of format "letters" found in the format string
	 * @throws SprintfUtilException if illegal formatters are found in the format string
	 * 
	 */
	public static List<Triplet<Integer,Integer,String>> getFormatLettersAndPositions(final String format) throws SprintfUtilException {

		List<Triplet<Integer,Integer,String>> parameterPairs = new LinkedList<Triplet<Integer,Integer,String>>();

		// Return an empty list if no formatters found
		if ((format == null) || (format.length() == 0) || (format.indexOf("%") == -1)) {
			return parameterPairs;
		}
		
		// Search for AMPCS custom formatters we do not support in this method
		// and throw if any found.
		for (String unsupported: unsupportedFormats) {
			if (format.indexOf(unsupported) != -1) {
				throw new SprintfUtilException("Formatter " + unsupported + " is not supported in this context");
			}
		}
		
		int len = format.length();

		// Loop through the format string always looking for the next %
		int nextPercent = format.indexOf('%');
		while (nextPercent != -1) {

			String prefix = format.substring(nextPercent);

			// A double percent is not a formatter. Skip it.
			if (prefix.startsWith("%%")) {
				nextPercent = format.indexOf('%', nextPercent + 2);
				continue;
			}

			// Starting at the current %, we must find the actual format character
			// by looking forward one character at a time.
			int index = nextPercent;
			while (++index < len) {
				
				char c = format.charAt(index);

				// Another %. There is no formatter associated with the last %. Break out
				// of this loop and start over with the current %.
				if (c == '%') {
					break;
				}
				
				// Skip over the length specifiers allowed on integers (hHlL), but check for doubles
				// hh, ll and throw if found. We do not support those.
				if (allowedIntegerFormatPrefixes.indexOf(c) != -1) {
					if (index < len && format.charAt(index + 1) == c) {
						throw new SprintfUtilException(c + c  + " is not supported in printf formatters");
					}
					continue;
				}
				
				// If in the allowed characters for length, width, or precision, skip over it.
				if (lengthWidthChars.indexOf(c) != -1)  {
					continue;
				}

				// If we got here, we must have a formatter. Double check and throw if not.				
				if (formatCharacters.indexOf(c) == -1) {
					throw new SprintfUtilException(c  + " is not supported in printf formatters");
				}
				
				// Special case: if this was a %x formatter and there was a 0x before the %, the 0x
				// is considered part of the formatter, so the position must be adjusted.
				int startIndex = nextPercent;
				if (c == 'x' || c == 'X') {
					if (nextPercent - 2 >= 0) {
						String zerox = format.substring(nextPercent - 2, nextPercent);
						if (zerox.equalsIgnoreCase("0x")) {
							startIndex -= 2;
						}
					}
				}
				
				// Add the formatter to the list of formatters. Break out of this loop and look
				// for the next %.
				parameterPairs.add(new Triplet<Integer,Integer, String>(startIndex, index, Character.toString(c)));
				break;
			}

			nextPercent = format.indexOf("%", nextPercent + 1);
		}

		return parameterPairs;
	}
}
