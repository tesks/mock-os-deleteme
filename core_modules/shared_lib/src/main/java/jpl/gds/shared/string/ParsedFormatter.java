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
/**
 * 
 */
package jpl.gds.shared.string;

import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import jpl.gds.shared.types.AlignmentType;
import jpl.gds.shared.types.FormatType;
import jpl.gds.shared.types.IntegerBaseType;
import jpl.gds.shared.types.PadCharacterType;


/**
 * The ParsedFormatter class is an object representation of a C printf formatter.

 *
 */
public class ParsedFormatter {

	private static final String PAD_REGEXP = "[\\-+ 0#]+";
	private static final String WIDTH_REGEXP = "[0123456789]+";
	private static final String PRECISION_REGEXP = "[0123456789]+";
	private static final String PRECISION_SEP_REGEXP = "[.]";

	private static final String INT_REGEXP = "[%][\\-+ 0#]*([1-9][0-9]*)?l?[diuxXo].*";
	private static final String INT_LENGTH_REGEXP = "[l]";
	private static final String INT_TYPE_REGEXP = "[diuxXo]";

	private static final String UNSIGNED_INT_REGEXP = "[%][\\-+ 0#]*([1-9][0-9]*)?l?[diuxXo].*";
	private static final String UNSIGNED_INT_LENGTH_REGEXP = "[l]";
	private static final String UNSIGNED_INT_TYPE_REGEXP = "[diuxXo]";

	private static final String FLOAT_REGEXP = "[%][\\-+ 0]*([1-9][0-9]*)?([.][0-9]+)?[fegFEG].*";
	private static final String FLOAT_TYPE_REGEXP = "[fegFEG]";
	private static final String FLOAT_DELIM_REGEXP = "[.fegFEG]";

	private static final String STRING_REGEXP = "[%][\\- 0]*([1-9][0-9]*)?([.][0-9]+)?[cs].*";
	private static final String STRING_PAD_REGEXP = "[\\- 0]+";
	private static final String STRING_TYPE_REGEXP = "[cs]";
	private static final String STRING_DELIM_REGEXP = "[.cs]";

	private int width = 10;
	private AlignmentType alignment = AlignmentType.RIGHT;
	private PadCharacterType padChar = PadCharacterType.NONE;
	private FormatType type = FormatType.SIGNED_INT;
	private IntegerBaseType base = IntegerBaseType.DECIMAL;
	private int precision = 0;
	private boolean useSign = false;
	private boolean signIsSpaceWhenPadding = false;
	private boolean useUpperCaseHex = false;
	private boolean displayAsCharacters = false;
	private String prefix;
	private String suffix;
	private boolean displayTypePrefix = false;


    /**
     * Constructor.
     */
	public ParsedFormatter()
    {
		// do nothing
    }


    /**
     * Constructor.
     *
     * @param typeChar  Type character
     * @param width     Width
     * @param padSpec   Pad specification  
     * @param precision Precision
     * @param prefixStr Prefix string
     * @param suffixStr Suffix string
     */
	public ParsedFormatter(char typeChar, int width, String padSpec, int precision, String prefixStr, String suffixStr) {
		this.width = width;
		this.precision = precision;
		prefix = prefixStr;
		suffix = suffixStr;
		type = FormatType.createFromTypeChar(typeChar);

		displayAsCharacters = type == FormatType.CHARACTER;

		if (typeChar == 'x') {
			base = IntegerBaseType.HEX;
		} else if (typeChar == 'X') {
			base = IntegerBaseType.HEX;
			useUpperCaseHex = true;
		} else if (typeChar == 'o') {
			base = IntegerBaseType.OCTAL;
		} else {
			base = IntegerBaseType.DECIMAL;
		}

		if (padSpec == null) {
			alignment = AlignmentType.RIGHT;
			padChar = PadCharacterType.NONE;
			useSign = false;
			signIsSpaceWhenPadding = false;
			displayTypePrefix = false;

		} else {
			displayTypePrefix = ((base == IntegerBaseType.HEX || base == IntegerBaseType.OCTAL) &&
					padSpec.indexOf('#') != -1);
			useSign = padSpec.indexOf('+') != -1;
			if (padSpec.indexOf('-') != -1) {
				alignment = AlignmentType.LEFT;
			} else {
				alignment = AlignmentType.RIGHT;
			}
			if (padSpec.indexOf(' ') != -1) {
				padChar = PadCharacterType.SPACE;
			}
			if (padSpec.indexOf('0') != -1) {
				if (padChar == PadCharacterType.SPACE && type.isSigned()) {
					signIsSpaceWhenPadding = true;
				} else {
					signIsSpaceWhenPadding = false;
				}
				padChar = PadCharacterType.ZERO;
			}
		}
	}

    /**
     * Create signed integer formatter from format string.
     *
     * @param inFormatString Format string
     *
     * @return Formatter
     */
	public static ParsedFormatter parseIntFormatter(String inFormatString) {
		String formatString = inFormatString;
		int width = 0;
		String typeStr = null;
		String padStr = "";
		int index = formatString.indexOf('%');
		String prefixStr = null;
		String suffixStr = null;

		if (index != -1 && index != 0) {
			prefixStr = formatString.substring(0, index);
			formatString = formatString.substring(index);
		}

		if (formatString.matches(INT_REGEXP)) {

			// Start the scanner after the %, which we now know is there due to the regexp match
			Scanner scanner = new Scanner(formatString);
			String tempTypeStr = scanner.findInLine(INT_TYPE_REGEXP);
			if (tempTypeStr != null) {
				MatchResult mr = scanner.match();
				suffixStr = formatString.substring(mr.end());
				formatString = formatString.substring(0, mr.end());
			}
			scanner.close();
			
			scanner = new Scanner(formatString.substring(1));

			// See if the next character is one of the pad "flags"
			String tempPadStr = scanner.findWithinHorizon(PAD_REGEXP, 1);
			while (tempPadStr != null) {
				padStr = padStr + tempPadStr;
				tempPadStr = scanner.findWithinHorizon(PAD_REGEXP, 1);
			}
			if (padStr.equals("")) {
				padStr = null;
			}

			// See if there is a width next
			String widthStr = scanner.findInLine(WIDTH_REGEXP);
			if (widthStr != null) {
				width = Integer.parseInt(widthStr);
			}
			// See if there is a length modifier next and skip over it
			scanner.findInLine(INT_LENGTH_REGEXP);

			//Now for the type indicator
			typeStr = scanner.findInLine(INT_TYPE_REGEXP);
			
			scanner.close();

			return new ParsedFormatter(typeStr.charAt(0), width, padStr, 0, prefixStr, suffixStr);
		} else {
			return null;
		}
	}

    /**
     * Create unsigned int formatter from format string.
     *
     * @param inFormatString Format string
     *
     * @return Formatter
     */
	public static ParsedFormatter parseUnsignedIntFormatter(String inFormatString) {
		String formatString = inFormatString;
		int width = 0;
		String typeStr = null;
		String padStr = "";

		int index = formatString.indexOf('%');
		String prefixStr = null;
		String suffixStr = null;

		if (index != -1 && index != 0) {
			prefixStr = formatString.substring(0, index);
			formatString = formatString.substring(index);
		}

		if (formatString.matches(UNSIGNED_INT_REGEXP)) {
			// Start the scanner after the %, which we now know is there due to the regexp match
			Scanner scanner = new Scanner(formatString);

			String tempTypeStr = scanner.findInLine(UNSIGNED_INT_TYPE_REGEXP);
			if (tempTypeStr != null) {
				MatchResult mr = scanner.match();
				suffixStr = formatString.substring(mr.end());
				formatString = formatString.substring(0, mr.end());
			}
			scanner.close();
			
			scanner = new Scanner(formatString.substring(1));

			// See if the next character is one of the pad "flags"
			String tempPadStr = scanner.findWithinHorizon(PAD_REGEXP, 1);
			while (tempPadStr != null) {
				padStr = padStr + tempPadStr;
				tempPadStr = scanner.findWithinHorizon(PAD_REGEXP, 1);
			}
			if (padStr.equals("")) {
				padStr = null;
			}

			// See if there is a width next
			scanner.useDelimiter(Pattern.compile("[.]"));
			String widthStr = scanner.findInLine(WIDTH_REGEXP);
			if (widthStr != null) {
				width = Integer.parseInt(widthStr);
			}
			// See if there is a length modifier next and skip over it
			scanner.findInLine(UNSIGNED_INT_LENGTH_REGEXP);

			//Now for the type indicator
			typeStr = scanner.findInLine(UNSIGNED_INT_TYPE_REGEXP);
			
			scanner.close();

			return new ParsedFormatter(typeStr.charAt(0), width, padStr, 0, prefixStr, suffixStr);
		} else {
			return null;
		}
	}

    /**
     * Create float formatter from format string.
     *
     * @param inFormatString Format string
     *
     * @return Formatter
     */
	public static ParsedFormatter parseFloatFormatter(String inFormatString) {
		String formatString = inFormatString;
		int width = 0;
		String typeStr = null;
		String padStr = "";
		int precision = 0;

		int index = formatString.indexOf('%');
		String prefixStr = null;
		String suffixStr = null;

		if (index != -1 && index != 0) {
			prefixStr = formatString.substring(0, index);
			formatString = formatString.substring(index);
		}

		if (formatString.matches(FLOAT_REGEXP)) {
			// Start the scanner after the %, which we now know is there due to the regexp match
			Scanner scanner = new Scanner(formatString);

			String tempTypeStr = scanner.findInLine(FLOAT_TYPE_REGEXP);
			if (tempTypeStr != null) {
				MatchResult mr = scanner.match();
				suffixStr = formatString.substring(mr.end());
				formatString = formatString.substring(0, mr.end());
			}
			scanner.close();
			
			scanner = new Scanner(formatString.substring(1));

			// See if the next character is one of the pad "flags"
			String tempPadStr = scanner.findWithinHorizon(PAD_REGEXP, 1);
			while (tempPadStr != null) {
				padStr = padStr + tempPadStr;
				tempPadStr = scanner.findWithinHorizon(PAD_REGEXP, 1);
			}
			if (padStr.equals("")) {
				padStr = null;
			}

			String dotStr = scanner.findWithinHorizon(PRECISION_SEP_REGEXP, 1);
			if (dotStr == null) {
				// See if there is a width next
				scanner.useDelimiter(FLOAT_DELIM_REGEXP);
				if (scanner.hasNextInt()) {
					width = scanner.nextInt();
				}
			}

			// Look for precision
			scanner.findWithinHorizon(PRECISION_SEP_REGEXP, 1);
			String precisionStr = scanner.findInLine(PRECISION_REGEXP);
			if (precisionStr != null) {
				precision = Integer.parseInt(precisionStr);
			}

			//Now for the type indicator
			typeStr = scanner.findInLine(FLOAT_TYPE_REGEXP);

			scanner.close();
			
			return new ParsedFormatter(typeStr.charAt(0), width, padStr, precision, prefixStr, suffixStr);
		} else {
			return null;
		}
	}


    /**
     * Create string formatter from format string.
     *
     * @param inFormatString Format string
     *
     * @return Formatter
     */
	public static ParsedFormatter parseStringFormatter(String inFormatString) {
		String formatString = inFormatString;
		int width = 0;
		String typeStr = null;
		String padStr = "";
		int precision = 0;
		int index = formatString.indexOf('%');
		String prefixStr = null;
		String suffixStr = null;

		if (index != -1 && index != 0) {
			prefixStr = formatString.substring(0, index);
			formatString = formatString.substring(index);
		}
		if (formatString.matches(STRING_REGEXP)) {

			// Start the scanner after the %, which we now know is there due to the regexp match
			Scanner scanner = new Scanner(formatString);
			String tempTypeStr = scanner.findInLine(STRING_TYPE_REGEXP);
			if (tempTypeStr != null) {
				MatchResult mr = scanner.match();
				suffixStr = formatString.substring(mr.end());
				formatString = formatString.substring(0, mr.end());
			}
			
			scanner.close();
			
			// Start the scanner after the %, which we now know is there due to the regexp match
			scanner = new Scanner(formatString.substring(1));

			// See if the next character is one of the pad "flags"
			String tempPadStr = scanner.findWithinHorizon(STRING_PAD_REGEXP, 1);
			while (tempPadStr != null) {
				padStr = padStr + tempPadStr;
				tempPadStr = scanner.findWithinHorizon(STRING_PAD_REGEXP, 1);
			}
			if (padStr.equals("")) {
				padStr = null;
			}

			String dotStr = scanner.findWithinHorizon(PRECISION_SEP_REGEXP, 1);
			if (dotStr == null) {
				// See if there is a width next
				scanner.useDelimiter(STRING_DELIM_REGEXP);
				if (scanner.hasNextInt()) {
					width = scanner.nextInt();
				}
			}

			// Look for precision
			scanner.findWithinHorizon(PRECISION_SEP_REGEXP, 1);
			String precisionStr = scanner.findInLine(PRECISION_REGEXP);
			if (precisionStr != null) {
				precision = Integer.parseInt(precisionStr);
			}

			//Now for the type indicator
			typeStr = scanner.findInLine(STRING_TYPE_REGEXP);

			scanner.close();
			
			return new ParsedFormatter(typeStr.charAt(0), width, padStr, precision, prefixStr, suffixStr);
		} else {
			return null;
		}
	}


    /**
     * Get format string.
     *
     * @return Format as string
     */
	public String getFormatStringOnly() {
		StringBuffer result = new StringBuffer("%");

		if (displayTypePrefix) {
			result.append('#');
		}

		if (useSign) {
			result.append('+');
		}
		if (alignment.equals(AlignmentType.LEFT)) {
			result.append('-');
		}

		if (!padChar.equals(PadCharacterType.NONE)) {
			result.append(padChar.getPadCharacter());   
		}
		if (signIsSpaceWhenPadding) {
			result.append(' ');
		}
		if (width != 0) {
			result.append(width);
		}
		if (precision != 0) {
			result.append('.');
			result.append(precision);
		}
		switch(type) {
		case SIGNED_INT:
			result.append('d');
			break;
		case UNSIGNED_INT:
			switch(base) {
			case DECIMAL: result.append('u');
			break;
			case OCTAL: result.append('o');
			break;
			case HEX: result.append(useUpperCaseHex ? 'X' : 'x');
			}
			break;
		case FLOAT:
			result.append('f');
			break;
		case EXPONENTIAL:
			result.append('e');
			break;
		case FLOAT_OR_EXPONENTIAL:
			result.append('g');
			break;
		case CHARACTER:
			result.append('c');
			break;
		case STRING:
			result.append('s');
			break;
		}

		return result.toString();
	}

	/**
	 * Retrieves the alignment.
	 * @return the alignment
	 */
	public AlignmentType getAlignment() {
		return alignment;
	}


	/**
	 * Sets the alignment.
	 * @param alignment the alignment to set
	 */
	public void setAlignment(AlignmentType alignment) {
		this.alignment = alignment;
	}


	/**
	 * Retrieves the padChar.
	 * @return the padChar
	 */
	public PadCharacterType getPadChar() {
		return padChar;
	}


	/**
	 * Sets the padChar.
	 * @param padChar the padChar to set
	 */
	public void setPadChar(PadCharacterType padChar) {
		this.padChar = padChar;
	}


	/**
	 * Retrieves the base.
	 * @return the base
	 */
	public IntegerBaseType getBase() {
		return base;
	}


	/**
	 * Sets the base.
	 * @param base the base to set
	 */
	public void setBase(IntegerBaseType base) {
		this.base = base;
	}


	/**
	 * Retrieves the precision.
	 * @return the precision
	 */
	public int getPrecision() {
		return precision;
	}


	/**
	 * Sets the precision.
	 * @param precision the precision to set
	 */
	public void setPrecision(int precision) {
		this.precision = precision;
	}


	/**
	 * Retrieves the useSign.
	 * @return the useSign
	 */
	public boolean isUseSign() {
		return useSign;
	}


	/**
	 * Sets the useSign.
	 * @param useSign the useSign to set
	 */
	public void setUseSign(boolean useSign) {
		this.useSign = useSign;
	}

	/**
	 * Retrieves the signIsSpaceWhenPadding.
	 * @return the signIsSpaceWhenPadding
	 */
	public boolean isSignIsSpaceWhenPadding() {
		return signIsSpaceWhenPadding;
	}


	/**
	 * Sets the signIsSpaceWhenPadding.
	 * @param signIsSpaceWhenPadding the signIsSpaceWhenPadding to set
	 */
	public void setSignIsSpaceWhenPadding(boolean signIsSpaceWhenPadding) {
		this.signIsSpaceWhenPadding = signIsSpaceWhenPadding;
	}

	/**
	 * Retrieves the useUpperCaseHex.
	 * @return the useUpperCaseHex
	 */
	public boolean isUseUpperCaseHex() {
		return useUpperCaseHex;
	}


	/**
	 * Sets the useUpperCaseHex.
	 * @param useUpperCaseHex the useUpperCaseHex to set
	 */
	public void setUseUpperCaseHex(boolean useUpperCaseHex) {
		this.useUpperCaseHex = useUpperCaseHex;
	}


	/**
	 * Retrieves the width.
	 * @return the width
	 */
	public int getWidth() {
		return width;
	}


	/**
	 * Retrieves the type.
	 * @return the type
	 */
	public FormatType getType() {
		return type;
	}


	/**
	 * Sets the width.
	 * @param width the width to set
	 */
	public void setWidth(int width) {
		this.width = width;
	}


	/**
	 * Sets the type.
	 * @param type the type to set
	 */
	public void setType(FormatType type) {
		this.type = type;
	}


	/**
	 * Retrieves the displayAsCharacters.
	 * @return the displayAsCharacters
	 */
	public boolean isDisplayAsCharacters() {
		return displayAsCharacters;
	}


	/**
	 * Sets the displayAsCharacters.
	 * @param displayAsCharacters the displayAsCharacters to set
	 */
	public void setDisplayAsCharacters(boolean displayAsCharacters) {
		this.displayAsCharacters = displayAsCharacters;
	}


	/**
	 * Retrieves the prefix.
	 * @return the prefix
	 */
	public String getPrefix() {
		return prefix;
	}


	/**
	 * Sets the prefix.
	 * @param prefix the prefix to set
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}


	/**
	 * Retrieves the suffix.
	 * @return the suffix
	 */
	public String getSuffix() {
		return suffix;
	}


	/**
	 * Sets the suffix.
	 * @param suffix the suffix to set
	 */
	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	/**
	 * Gets the flag indicating where to prepend 0 or 0x to octal/hex numbers
	 * @return the displayTypePrefix flag
	 */
	public boolean isDisplayTypePrefix() {
		return displayTypePrefix;
	}

	/**
	 * Sets the flag indicating where to prepend 0 or 0x to octal/hex numbers
	 * @param display true to display type prefix, false to not
	 */
	public void setDisplayTypePrefix(boolean display) {
		displayTypePrefix = display;
	}
}
