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
package jpl.gds.shared.formatting;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.LocalSolarTime;
import jpl.gds.shared.time.Sclk;
import jpl.gds.shared.time.SclkScetUtility;


/**
 * Formats data values in a manner as close to that of a C printf(format,object...) as possible,
 * though I don't think we have proven it yet.
 *
 * ALL changes to this module shall be tested for performance impact.
 * This is a performance critical components.
 *
 */
public class SprintfFormat
{
    private static final long       HIGH_BIT_MASK   = 1L << (Long.SIZE - 1);
    private static final BigInteger HIGH_BIT        =
        BigInteger.ONE.shiftLeft(Long.SIZE - 1);
    private static final String     HIGH_BIT_STRING = HIGH_BIT.toString();
    private static final String     NSF             = "NSF";


    /**
     * Defines the types of time formatters we support.
     *
     */
    private enum TimeType {
        UNDEFINED,
        SCET,
        SCLK,
        ERT,
        LST
    };

    // Constants
    private final static char FINAL_TOKEN = '%';
    private final static String WIDTH_SEPARATOR_TOKEN = ".";
    private final static int DEFAULT_PRECISION = 6;
    private final static int BYTE_INT_MASK = 0x000000FF;
    private final static int SHORT_INT_MASK = 0x0000FFFF;
    private final static long INT_MASK = 0xFFFFFFFFL;
    private final static int PREFAB_STRING_LENGTH = 40;
    private final static String STRING_OF_BLANKS = "                                        ";
    private final static String STRING_OF_ZEROS =  "0000000000000000000000000000000000000000";

    private static final String[] vectorOfTranslations = {
        "%SCET", "%SCLK", "%ERT", "%LST", "%ENUM", "%scet", "%sclk", "%ert", "%lst", "%enum"
    };

    private static final String[] translatedTimeVector = {
        "%0t", "%1t", "%2t", "%3t", "%b",   "%0t", "%1t", "%2t", "%3t", "%b"
    };

    // Instance variables
    private static final Pattern formatterPattern = Pattern.compile("[bciUudEefGgnopstXx]");
    private final List<Integer> percentPositions = new ArrayList<Integer>();
    private int nextLiteralPos = 0;
    private final FormatterData formatterData = new FormatterData();
    private final Integer scid;

    private static Tracer         trace;

    /**
     * Constructor. Assumes spacecraft ID is 0.
     * 
     */
    public SprintfFormat() {
        this(0);
    }
    
    /**
     * Constructor.
     * 
     * @param scid
     *            spacecraft ID, used for time conversions
     */
    public SprintfFormat(final Integer scid) {
        this.scid = scid;
        trace = TraceManager.getDefaultTracer();
    }
    
    /**
     * This routine emulates the sprintf function from the programming language
     * C
     *
     * @param formatString
     *            Format string
     * @param formatValues
     *            an Object array with the values to insert in the format string
     * @return StringBuilder of the formated string.
     */
    public synchronized String sprintf(final String formatString,
            final Object formatValues[]) {
        int i = 0;
        String aFormatString = null; //formatString;
        StringBuilder formattedOutput = null;

        try {
            int endLiteral = 0;

            // Replace non-C format specifiers from the dictionary with formatters we can easily identify
            aFormatString = translateNonCFormats(formatString);

            // Count the number and location of the percent formatters and the literals
            // in the format string.
            final int numOfPercents = findPercents(aFormatString);

            // If no conversions necessary, so just replace double percents and return
            if (numOfPercents == 0) {
                return replaceDoublePercents(aFormatString);
            } else {
                formattedOutput = new StringBuilder("");
            }

            // If too few formatters for value list, throw an exception
            if (numOfPercents < formatValues.length) {
                trace.error(
                        "Too few format values in: "
                        + formatString
                        + " to output the number of Objects ("
                        + formatValues.length + ")");
                trace
                .error(
                "This error is fatal and returns a 'NSF' (No Such Format) as the return value.");
                return NSF;
            }
            // If too few values for formatter list, throw an exception
            if (numOfPercents > formatValues.length) {
                trace.error(
                        "Too few values for: "
                        + formatString
                        + " to output the number of Objects ("
                        + formatValues.length + ")");
                trace
                .error(
                "This error is fatal and returns a 'NSF' (No Such Format) as the return value.");
                return NSF;
            }

            nextLiteralPos = 0;
            final int formatStrLen = aFormatString.length();

            // Start by appending the first literal portion of the format string to the output text
            formattedOutput.append(replaceDoublePercents(aFormatString.substring(0,
                    percentPositions.get(0))));

            // Loop through the percent/formatter token positions. Format each object and append it. Then append
            // in literal text between the current formatter and the next formatter, etc, until all the
            // objects have been formatted and the string is complete.

            for (i = 0; i < numOfPercents; ++i) {

                if (i == numOfPercents - 1) {
                    endLiteral = formatStrLen;
                } else {
                    endLiteral = percentPositions.get(i + 1);
                }

                final String nxtToken = findFormatterToken(aFormatString, percentPositions.get(i));
                if (nxtToken.equals(NSF)) {
                    return nxtToken;
                }

                if (formatValues[i] == null) {
                    return NSF;
                }

                // Get the cType of the value to format
                final SprintfDataType type = SprintfDataType.getTypeForObject(formatValues[i]);

                // Format the value and append it to the output string
                final StringBuilder tempBuff = formatValueForOutput(nxtToken,formatValues, type, i);
                formattedOutput.append(tempBuff);

                // Append the next chunk of literal string
                if (nextLiteralPos < endLiteral) {
                    formattedOutput.append(replaceDoublePercents(aFormatString.substring(nextLiteralPos, endLiteral)));
                }
            } // end for

            // Append any trailing chunk of literal stream
            if (endLiteral < formatStrLen) {
                formattedOutput.append(replaceDoublePercents(aFormatString.substring(endLiteral)));
            }

            return formattedOutput.toString();

        } catch (final Exception e) {
            e.printStackTrace();
            trace.error("Unexpected exception in SprintfFormat processing "
                    + " format string " + formatString);
            return NSF;

        }

    } // end member function Csprintf


    /**
     * Checks a format string to see if it is valid and returns true if it is
     * and false if it is not.
     *
     * @param theFormatString Format string to check
     * @return True if valid
     */
    public boolean isValidFormatString(final String theFormatString) {
        return validFormatString(theFormatString);
    }

    /**
     * Checks to see if theFormatString is a valid c format string
     *
     * @param theFormatString
     *
     * @return Boolean true if theFormatString is valid otherwise boolean false.
     *
     */
    private synchronized boolean validFormatString(final String theFormatString) {
        boolean statusReturnValue = false;
        if (theFormatString == null || theFormatString.length() == 0) {
            return false;
        }

        formatterData.clear();
        final String formatString = theFormatString;

        try {
            int i = 0;

            // Count the number and and store the location of the percent formatters
            final int numOfPercents = this.findPercents(theFormatString);

            if (numOfPercents == 0) {
                return true;
            } // end else

            // check each formatter for validity
            for (i = 0; i < numOfPercents; ++i) {

                final String nxtToken = findFormatterToken(formatString,
                        percentPositions.get(i));
                if (nxtToken.equals(NSF)) {
                    statusReturnValue = false;
                    break;
                }

                statusReturnValue = checkFormatter(nxtToken);
                if (!statusReturnValue) {
                    break;
                }

            } // end for
        } catch (final ArrayIndexOutOfBoundsException aiobE) {
            statusReturnValue = false;
        } catch (final NumberFormatException nfeE) {
            statusReturnValue = false;
        }
        return statusReturnValue;
    }


    private String translateNonCFormats(final String formatString) {

        StringBuilder afterTranslation = null;
        int i = 0;
        int k = 0;

        try {
            final int len = formatString.length();
            ArrayList<Integer> replacePositions = null;

            while (i != -1 && i < len) {
                i = formatString.indexOf(FINAL_TOKEN, i);
                if (i == -1) {
                    break;
                } else if (i == len - 1) {
                    break;
                } else if (formatString.charAt(i+ 1) == FINAL_TOKEN) {
                    i+=2;
                    continue;
                }
                for (k = 0; k < vectorOfTranslations.length; k++) {
                    final String checkStr = vectorOfTranslations[k];
                    if (formatString.indexOf(checkStr, i) == i) {
                        if (replacePositions == null) {
                            replacePositions = new ArrayList<Integer>(1);
                        }
                        replacePositions.add(i);
                        i += checkStr.length();
                        break;
                    }
                }
                i++;
            }
            if (replacePositions == null) {
                return formatString;
            }

            afterTranslation = new StringBuilder(formatString);

            int posAdjust = 0;
            for (final Integer pos : replacePositions) {
                for (k = 0; k < vectorOfTranslations.length; k++) {
                    final String checkStr = vectorOfTranslations[k];
                    if (afterTranslation.substring(pos + posAdjust).startsWith(checkStr)) {
                        final int checkLen = checkStr.length();
                        afterTranslation.replace(pos + posAdjust, pos + posAdjust + checkLen,translatedTimeVector[k]);
                        posAdjust +=  translatedTimeVector[k].length() - checkLen;
                        break;
                    }
                }
            }


        } catch (final IndexOutOfBoundsException iobE) {
            trace.error(
            "Invalid format supplied. IndexOutOfBounds at:");
            iobE.printStackTrace();
        }

        return afterTranslation.toString();
    }

    /**
     * Count and create a position array locating every
     * %format value in the format string passed to the function.
     * @param formatString a C-style scanf format string
     * @return int count of the % formatters in the string; escaped %% are not counted.
     */

    private int findPercents(final String formatString) {
        final int finalPos = formatString.length();
        int nxtPos = formatString.indexOf(FINAL_TOKEN);
        percentPositions.clear();
        nextLiteralPos = 0;
        int totalPercents = 0;

        try {
            while (nxtPos != -1 && nxtPos < finalPos) {
                if (nxtPos == finalPos - 1) {
                    break;
                } else if (formatString.charAt(nxtPos + 1) == FINAL_TOKEN) {
                    nxtPos += 2;
                } else {
                    percentPositions.add(nxtPos);
                    totalPercents++;
                    nxtPos++;
                }
                nxtPos = formatString.indexOf(FINAL_TOKEN, nxtPos);
            } // end while
        } catch (final ArrayIndexOutOfBoundsException e) {
            trace.error(
                    "Method CountPercents in CSprintf exceeded a vector boundary with value of totalPercents: "
                    + totalPercents);
            trace.error(
                    "Vector percentPositions length was: "
                    + percentPositions.size(), e);
        }

        return totalPercents;
    }

    /**
     * Verifies whether the given print formatter indicates a supported
     * data type.
     * @param aToken A C-style formatter token as a String
     *
     * @return true if format string is valid; otherwise false.
     *
     */
    private static boolean checkFormatter(final String aToken) {
        final boolean statusReturnValue = false;

        if (aToken == null || aToken.length() == 0) {
            return statusReturnValue;
        }

        if (!aToken.matches(".*[bciUudEefGgnopstXx]")) {
            trace.error(
                    "Invalid format character: "
                    + aToken.charAt(aToken.length() - 1));
            return false;
        }

        return true;
    }


    /**
     * This function determines the next field's width and sets the
     * options for the field.
     *
     * @param aToken A C-style string formatter
     * @param aValue An Object class instance containing the value to format and
     *            insert into the string buffer.
     * @param theType Data type
     * @param nxtValue Nextvalue to process
     *
     * @return StringBuilder containing the formated value.
     */
    private StringBuilder formatValueForOutput(final String aToken,
            final Object[] aValue, final SprintfDataType theType, final int nxtValue)
    throws ArrayIndexOutOfBoundsException {
        StringBuilder formatedString = new StringBuilder("");
        //double characteristic = 0.0;
        //double mantissa = 0.0;
        final int tokenLen = aToken.length();

        if (tokenLen == 0) {
            return formatedString;
        }

        SprintfDataType theDesiredType = SprintfDataType.getDesiredType(aToken.charAt(tokenLen - 1), theType);

        setOptions(aToken, theType); // Important work is done here!

        if (SprintfDataType.INT == theDesiredType && formatterData.longValue) {
            theDesiredType = SprintfDataType.LONG;
        }

        if (! isTypeValid(theDesiredType, theType, aValue[nxtValue]))
        {
            formatedString.append(aValue[nxtValue].toString());
            return formatedString;
        }

        try {
            // Okay...we know what we want, let's go and actually build the
            // output string.
            switch (theDesiredType) {
            case ENUM:
                break;  // NOTHING?
            case TIME:
                formatedString = processValueToTime(aValue[nxtValue]);
                break;
            case BYTE:
                formatedString = processValueToByte(aValue[nxtValue]);
                break;
            case STRING:
                formatedString = processValueToString(aValue[nxtValue]);
                break;
            case SHORT:
                formatedString = processValueToShort(aValue[nxtValue]);
                break;
            case INT:
            case LONG:
                if (formatterData.charOnly) {
                    char theCharValue = (char) (((Number) aValue[nxtValue])).byteValue();
                    theCharValue = getPrintable(theCharValue);
                    formatedString.append(theCharValue);
                    break;
                }

                if (formatterData.forceScientificNotation) {
                        trace.error(
                                "Attempting to format an Integer type with %e. Object is not of base type DOUBLE for supplied format string.");

                    formatedString.append(aValue[nxtValue].toString());
                    break;
                }

                long tempValue = 0L;

                final boolean doNotContinueWithIntTypeSearch = !(formatterData.truncateToInt
                        && (theType.isFloatingPoint() ) );
                if (doNotContinueWithIntTypeSearch && ((SprintfDataType.CHAR != theType) && (SprintfDataType.INT != theType)
                        && (SprintfDataType.BYTE != theType)
                        && (SprintfDataType.STRING != theType)
                        && (SprintfDataType.LONG != theType)
                        && (SprintfDataType.SHORT != theType))) {
                        trace.error("Object is not of base type INTEGER for supplied format string.");

                    formatedString.append (aValue[nxtValue].toString());
                    break;
                }

                if (SprintfDataType.CHAR == theType)
                {
                    tempValue = (Character) aValue[nxtValue];
                }
                else if (SprintfDataType.STRING == theType)
                {
                    final String s = (String) aValue[nxtValue];

                    tempValue = (s.length() > 0) ? (long) s.charAt(0) : 0L;
                }
                else if (SprintfDataType.BYTE == theType)
                {
                    final byte b = (Byte) aValue[nxtValue];

                    tempValue = formatterData.unsignedValue ? (b & 255L) : (long) b;
                }
                else if (SprintfDataType.SHORT == theType)
                {
                    final short s = (Short) aValue[nxtValue];

                    tempValue = formatterData.unsignedValue ? (s & 65535L) : (long) s;
                }
                else if (SprintfDataType.DOUBLE == theType && formatterData.truncateToInt) {
                    // Here we completely violate the C standard by trying to
                    // print a double as an integer -- we round, truncate
                    // and convert
                    tempValue = convertDoubleToInt((Double) aValue[nxtValue]);
                }
                else if (SprintfDataType.FLOAT == theType && formatterData.truncateToInt) {
                    // Here we completely violate the C standard by trying to
                    // print a float as an integer -- we round, truncate
                    // and convert
                    tempValue = convertDoubleToInt(((Float) aValue[nxtValue]).doubleValue());
                }
                else if (SprintfDataType.INT == theType)
                {
                    final int i = (Integer) aValue[nxtValue];

                    tempValue = formatterData.unsignedValue ? (i & INT_MASK) : (long) i;
                }
                else if (SprintfDataType.LONG == theType)
                {
                    tempValue = (Long) aValue[nxtValue];
                }

                if (formatterData.unsignedValue)
                {
                    formatedString.append(formatUnsignedDecimal(tempValue));
                }
                else
                {
                    formatedString.append(formatSignedDecimal(tempValue));
                }

                break;

            case FLOAT:
            case DOUBLE:

                double theValue = 0.0D;

                if (SprintfDataType.FLOAT == theType)
                {
                    theValue = ((Float) aValue[nxtValue]);
                }
                else
                {
                    theValue = ((Double) aValue[nxtValue]);
                }

                formatedString.append(formatDouble(theValue,
                        formatterData.fieldWidth,
                        formatterData.decimalWidth,
                        formatterData.formatChar,
                        formatterData.signRequired,
                        formatterData.spaceSignRequired,
                        (formatterData.leadingChar == '0'),
                        formatterData.justification,
                        formatterData.alternate));
                break;

            case HEXADECIMAL:
            case OCTAL:
                if (formatterData.truncateToInt)
                {
                    final double doubleTempValue = ((Number) aValue[nxtValue]).doubleValue();

                    formatedString.append(formatNonDecimal(StrictMath.round(doubleTempValue)));
                    break;
                }

                if (aValue[nxtValue] instanceof Character)
                {
                    formatedString.append(formatHexOrOctal((int) ((Character) aValue[nxtValue]).charValue()));
                }
                else
                {
                    formatedString.append(formatHexOrOctal(expandUnsignedToLong((Number) aValue[nxtValue])));
                }

                break;

            default:
                break;
            } // end switch
        } catch (final ArrayIndexOutOfBoundsException e) {
            trace.error(
                    "Method ParsePercent in class CSprintf exceeded an array bounds at nxtValue: "
                    + nxtValue);
            trace.error(
                    "aValue string length: " + aValue.length);
            formatedString.append (aValue[nxtValue].toString());
        }

        return formatedString;
    }

    /**
     * Locate formatter token in string.
     *
     * @param searchString
     * @param StartPos
     * @return String
     */
    private String findFormatterToken(final String searchString, final int startPos)
    throws ArrayIndexOutOfBoundsException {
        final Matcher m = formatterPattern.matcher(searchString);

        if (m.find(startPos)) {
            final int start = m.start();
            nextLiteralPos = start + 1;
            return(searchString.substring(startPos, start + 1));
        }

        return NSF;
    } // end member function FindTokenEnd


    /**
     * Set options from token.
     *
     * @param theToken Format token
     * @param theType  Data type
     */
    private void setOptions(final String theToken, final SprintfDataType theType) {

        final int theTokenLen = theToken.length();
        formatterData.clear();

        final char option = theToken.charAt(theTokenLen - 1);

        formatterData.upperCase = Character.isUpperCase(option);

        // First look at the actual format character, which must be the last
        // one in the formatter token, and set the options it calls for

        formatterData.formatChar = option;

        switch (option)
        {
        case 'c':
            formatterData.charOnly = true;
            break;
        case 'i':
        case 'd':
            if (theType.isFloatingPoint()) {
                formatterData.truncateToInt = true;
            }
            break;
        case 'U':
        case 'u':

            if (theType.isFloatingPoint()) {
                formatterData.truncateToInt = true;
            }
            formatterData.unsignedValue = true;
            break;
        case 'E':
        case 'e':
            formatterData.forceScientificNotation = true;
            break;
        case 'G':
        case 'g':
            break;
        case 'n': // processing number of characters output. Not implemented
            return; // no post processing of values
        case 'o':
            formatterData.octal = true;
            if (theType.isFloatingPoint()) {
                formatterData.truncateToInt = true;
            }
            break;
        case 't': // processing time values.
            formatterData.timeType = TimeType.UNDEFINED;
            break;
        case 'X':
        case 'x':
            formatterData.hexadecimal = true;
            if (theType.isFloatingPoint()) {
                formatterData.truncateToInt = true;
            }
            break;
        default:
            break;
        } // end switch

        boolean doingFieldWidth = true;

        if (theToken.indexOf(WIDTH_SEPARATOR_TOKEN) > 0)  {
            formatterData.decimalWidth = 0;
            doingFieldWidth = false;
        }

        int index = theTokenLen - 2;
        StringBuilder fieldDigits = null;
        StringBuilder decimalDigits = null;
        boolean firstNumZero = false;
        char nxtChar = '\0';

        do {
            nxtChar = theToken.charAt(index);
            switch (nxtChar) {
            case 'h':
            case 'H':
                break;
            case 'l': // processing long values
            case 'L': // processing Long values
                formatterData.longValue = true;
                break;
            case '-':
                formatterData.justification = true;
                break;
            case '+':
                formatterData.signRequired = true;
                break;
            case ' ':
                formatterData.spaceSignRequired = true;
                break;
            case '#':
                formatterData.alternate = true;
                break;
            case '0':
                firstNumZero = true;
                if (theType == SprintfDataType.TIME || theType == SprintfDataType.DOUBLE ||
                        theType == SprintfDataType.FLOAT) {
                    formatterData.timeType = TimeType.SCET;
                }
                if (doingFieldWidth) {
                    if (fieldDigits == null) {
                        fieldDigits = new StringBuilder(2);
                    }
                    fieldDigits.insert(0, nxtChar);
                } else {
                    if (decimalDigits == null) {
                        decimalDigits = new StringBuilder(2);
                    }
                    decimalDigits.insert(0, nxtChar);
                }
                break;
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                if ((theType == SprintfDataType.TIME || theType == SprintfDataType.DOUBLE ||
                        theType == SprintfDataType.FLOAT) && (formatterData.timeType == TimeType.UNDEFINED))
                {
                    switch (nxtChar)
                    {
                    case '1':
                        formatterData.timeType = TimeType.SCLK;
                        break;
                    case '2':
                        formatterData.timeType = TimeType.ERT;
                        break;
                    case '3':
                        formatterData.timeType = TimeType.LST;
                        break;
                    default:
                        break;
                    }
                }

                firstNumZero = false;
                if (doingFieldWidth) {
                    if (fieldDigits == null) {
                        fieldDigits = new StringBuilder(2);
                    }
                    fieldDigits.insert(0, nxtChar);
                } else {
                    if (decimalDigits == null) {
                        decimalDigits = new StringBuilder(2);
                    }
                    decimalDigits.insert(0, nxtChar);
                }
                break;
            case '.':
                doingFieldWidth = true;

                formatterData.hasDecimalPoint = true;
                break;
            case '*':
                break;
            default:
                break;
            } // end switch

        } while (--index >= 0); // end do-while

        if (fieldDigits != null) {
            formatterData.fieldWidth = Integer.valueOf(fieldDigits.toString());
        }

        if (decimalDigits != null) {
            if (decimalDigits.length() == 0) {
                formatterData.decimalWidth = 0;
            } else {
                formatterData.decimalWidth = Integer.valueOf(decimalDigits.toString());
            }
        }

        if (firstNumZero) {
            formatterData.leadingChar = '0';
        }
    }

    private static String makeStringOf(final char theChar2Make, final int lenOfString) {
        if (lenOfString <= 0) {
            return "";
        }

        if (lenOfString < PREFAB_STRING_LENGTH) {
            if (theChar2Make == ' ') {
                return STRING_OF_BLANKS.substring(0, lenOfString);
            } else if (theChar2Make == '0') {
                return STRING_OF_ZEROS.substring(0, lenOfString);
            }
        }

        final StringBuilder aString = new StringBuilder(lenOfString);
        for (int i = 0; i < lenOfString; ++i) {
            aString.append(theChar2Make);
        }
        return aString.toString();
    }


    /**
     * Purpose: Removes the escaped '%%' char, changing it to a '%', in the input string
     * All other escape characters are identical in Java and C or C++ so their
     * conversion happens on either input or output.

     * @param value2Format
     * @return String following formatting
     *
     */
    private static String replaceDoublePercents(final String value2Format) {
        if (value2Format == null || value2Format.length() == 0) {
            return value2Format;
        }

        final String returnValue = value2Format.replaceAll("%%", "%");

        return returnValue;
    }


    /**
     * Format a string (%s).
     *
     * @param valueToFormat
     *
     * @return Formatted value
     */
    private String formatString(final String valueToFormat)
    {
        final StringBuilder sb       = new StringBuilder();
        final String        original = (valueToFormat != null) ? valueToFormat : "";
        final int           oLength  = original.length();
        final int           length   = (formatterData.decimalWidth >= 0)
        ? Math.min(oLength, formatterData.decimalWidth)
                : oLength;
        final int           pad      = (formatterData.fieldWidth >= 0)
        ? Math.max(formatterData.fieldWidth - length, 0)
                : 0;
        if (formatterData.justification)
        {
            // Left justify

            sb.append(original, 0, length);

            if (pad > 0)
            {
                pad(sb, ' ', pad);
            }
        }
        else
        {
            // Right justify

            if (pad > 0)
            {
                pad(sb, ' ', pad);
            }

            sb.append(original, 0, length);
        }

        return sb.toString();
    }

    private String formatNonDecimal(final Number value) {

        int leadTrailCharCount = 0;
        String firstGuess = null;
        if (value instanceof Long) {
            firstGuess = makeStringFromLong((Long)value);
        } else {
            final int val = value.intValue();
            firstGuess = makeStringFromInteger(val);
        }

        final int firstGuessLen = firstGuess.length();

        final StringBuilder theFormatedValue = new StringBuilder(256);

        leadTrailCharCount = formatterData.fieldWidth - firstGuessLen;

        if (0 > leadTrailCharCount) {
            leadTrailCharCount = 0;
        }

        if (0 >= formatterData.fieldWidth) {
            formatterData.fieldWidth = firstGuessLen;
        }
        if (!formatterData.justification) { // left justified
            if (firstGuessLen < formatterData.fieldWidth) {
                theFormatedValue.append(makeStringOf(formatterData.leadingChar,
                        leadTrailCharCount) + firstGuess).substring(0, formatterData.fieldWidth);
            } else {
                theFormatedValue.append(makeStringOf(formatterData.leadingChar, leadTrailCharCount));
                theFormatedValue.append(firstGuess);
            }
        } else { // right justified
            if (firstGuessLen < formatterData.fieldWidth) {
                theFormatedValue.append(firstGuess
                        + makeStringOf(formatterData.trailingChar, leadTrailCharCount))
                        .substring(0, formatterData.fieldWidth);
            } else {
                theFormatedValue.append(firstGuess);
                theFormatedValue.append(makeStringOf(formatterData.trailingChar, leadTrailCharCount));
            }
        }
        return theFormatedValue.toString();
    }


    private String makeStringFromLong(final long aLongValue) {

        if (formatterData.hexadecimal) {
            return (formatterData.upperCase ?
                    Long.toHexString(aLongValue).toUpperCase() : Long.toHexString(aLongValue));

        } else if (formatterData.octal) {
            return Long.toOctalString(aLongValue);
        } else {
            return String.valueOf(aLongValue);
        }
    }

    /**
     * Convert int to string.
     *
     * @param anIntegerValue Integer value
     *
     * @return String
     */
    private String makeStringFromInteger(final int anIntegerValue) {

        if (formatterData.hexadecimal) {
            return (formatterData.upperCase ?
                    Integer.toHexString(anIntegerValue).toUpperCase() : Integer.toHexString(anIntegerValue));

        } else if (formatterData.octal) {
            return Integer.toOctalString(anIntegerValue);
        } else {
            return String.valueOf(anIntegerValue);
        }
    }

    /**
     * Replaces unprintable characters by their escaped (or unicode escaped)
     * equivalents in the given string
     */
    private static final char getPrintable(final char c) {
        char retval = c;

        if (c < 0x20 || c > 0x7e) {
            retval = '.';
        }
        return retval;
    }

 
    private StringBuilder processValueToTime(final Object aValue)
    {
        final StringBuilder formattedString = new StringBuilder(60);
        boolean deprecatedFeature = false;

        if (scid == null) {
            formattedString.append ( aValue.toString() );
            return formattedString;
        }

        switch (formatterData.timeType) {
        case SCET: // SCET
            IAccurateDateTime scet = null;
            if (aValue instanceof Sclk) {
                // Got a SCLK, want SCET
                    deprecatedFeature = true;

                scet = SclkScetUtility.getScet((ISclk) aValue, null, scid.intValue());
                if (scet == null)
                {
                    scet = new AccurateDateTime(0L, 0L);
                }
            } else if (aValue instanceof IAccurateDateTime) {
                // Got Date, want SCET
                scet = (IAccurateDateTime)aValue;
            } else if (aValue instanceof Float || aValue instanceof Double) {
                // WRONG Why use double here?
                final ISclk tempSclk = new Sclk((Double)aValue);
                scet = SclkScetUtility.getScet(tempSclk, null, scid.intValue());
                if (scet == null) {
                    scet = new AccurateDateTime(0L, 0L);
                }
            }
            if (deprecatedFeature) {
                formattedString.append("UNSUPPORTED");
            }
            else if (scet != null) {
                formattedString.append(scet.getFormattedScet(true));
            } else {
                formattedString.append("No SCET");
            }
            break;
        case SCLK: // SCLK
            ISclk sclk = null;
            if (aValue instanceof IAccurateDateTime) {
                // Got a Scet, want SCLK
                sclk = SclkScetUtility.getSclk(
                           (IAccurateDateTime) aValue, null, scid.intValue());
                if (null == sclk) {
                        trace.error("Cannot format value as SCLK because the SCLK/SCET correlation could not be found");
                    formattedString.append (aValue);
                    break;
                }
            } else if (aValue instanceof Sclk) {
                // Got SCLK, want SCLK, we're cool
                sclk = (ISclk) aValue;
            } else if (aValue instanceof Float || aValue instanceof Double) {
                sclk = new Sclk((Double)aValue);
            }
            else if (aValue != null)
            {
                    trace.error("Cannot convert value to SCLK: " + aValue.getClass().getName());

                formattedString.append(aValue);
                break;
            }
            else
            {
                    trace.error("Cannot convert null to SCLK");
                break;
            }

            formattedString.append(sclk.toString());
            break;
        case ERT: // ERT
            final IAccurateDateTime ertDate  = (IAccurateDateTime) aValue;
            formattedString.append(ertDate.getFormattedErt(true));
            break;
        case LST: // LMST
            ILocalSolarTime sol = null;
            if (aValue instanceof Sclk) {
                // Got a SCLK, want LST
                    deprecatedFeature = true;

                    sol = new LocalSolarTime(scid.intValue(), (ISclk) aValue);

            } else if (aValue instanceof ILocalSolarTime) {
                // Got LST, want LST, we're cool
                    sol = (ILocalSolarTime) aValue;
            } else if (aValue instanceof IAccurateDateTime) {
                // Got Date, want LST
                    sol = new LocalSolarTime(scid.intValue(), (IAccurateDateTime) aValue);
            }
            if (deprecatedFeature) { 
                formattedString.append("UNSUPPORTED");
            }
            else if (sol != null) {
                formattedString.append(sol.getFormattedSol(true));
            } else {
                formattedString.append("No LST");
            }
            break;

        default:
            break;
        } // end switch

        return formattedString;
    }


    private StringBuilder processValueToString(final Object aValue)
    {
        final StringBuilder formattedString = new StringBuilder("");

        if (formatterData.charOnly) {

            final String tempString = (String)(aValue);

            int tempStrLen = tempString.length();
            // Get the minimum between the string length and the
            // field width
            if (formatterData.fieldWidth < tempStrLen) {
                tempStrLen = formatterData.fieldWidth;
            }

            // If no field with was defined, make it 1 (%c)
            if (tempStrLen < 0) {
                tempStrLen = 1;
            }

            formattedString.append(tempString.substring(0, tempStrLen));

        } else if (formatterData.unsignedValue) {
            final String tempString = (String)(aValue);
            final int tempUnicodeChar = tempString.charAt(0);
            formattedString.append(tempUnicodeChar);

        }
        else
        {
            // Actually format the string value

            formattedString.append(formatString((String) aValue));
        }

        return formattedString;
    }


    private static StringBuilder processValueToByte(final Object aValue)
    {
        final StringBuilder formattedString = new StringBuilder("");

        final char theCharValue = (char)(((Byte)aValue).byteValue());
        formattedString.append(theCharValue);

        return formattedString;
    }


    private StringBuilder processValueToShort(final Object aValue)
    {
        final StringBuilder formattedString = new StringBuilder("");
        if (formatterData.charOnly) {
            final char theCharValue = (char) (((Short)aValue)).byteValue();
            formattedString.append(theCharValue);
        }
        return formattedString;
    }


    private boolean isTypeValid(final SprintfDataType desiredType, final SprintfDataType currentType, final Object aValue) {
        switch(desiredType) {
        case BYTE:
            if ((SprintfDataType.STRING != currentType) && (SprintfDataType.BYTE != currentType)) {
                    trace.error("Object is not of type BYTE or STRING for %c");
                return false;
            }
            break;
        case STRING:
            if (SprintfDataType.STRING != currentType) {
                    trace.error("Object is not of type STRING for %s");
                return false;
            }
            break;
        case TIME:
            switch (formatterData.timeType) {
            case SCET: // SCET
            case SCLK: // SCLK
                if (!(aValue instanceof ISclk || aValue instanceof Date || aValue instanceof Double || aValue instanceof Float)) {
                            trace.error("Invalid object passed as SCET or SCLK. NSF returned for %SCET or %SCLK");
                    return false;
                }
                break;
            default:
                if (formatterData.timeType == TimeType.UNDEFINED) {
                            trace.error("Invalid Time Type specified!");
                    return false;
                }
            } // end switch
            break;
        case SHORT:
            if (formatterData.charOnly                  &&
                (SprintfDataType.STRING != currentType) &&
                (SprintfDataType.SHORT  != currentType))
            {
                    trace.error("Object is not of type SHORT, or STRING for %c");

                return false;
            }
            break;
        case INT:
            if (formatterData.charOnly                  &&
                (SprintfDataType.STRING != currentType) &&
                (SprintfDataType.INT    != currentType))
            {
                    trace.error("Object is not of type INT, SHORT, or STRING for %c");

                return false;
            }
            break;

        case LONG:
            if (SprintfDataType.LONG   != currentType &&
                    SprintfDataType.INT    != currentType &&
                    SprintfDataType.SHORT  != currentType &&
                    SprintfDataType.BYTE   != currentType &&
                    SprintfDataType.DOUBLE != currentType &&
                    SprintfDataType.FLOAT  != currentType &&
                    SprintfDataType.CHAR   != currentType)
            {
                    trace.error("Object is not of base type LONG/INT/SHORT/BYTE/CHAR for supplied format string.");
                return false;
            }

            break;

        case FLOAT:
        case DOUBLE:
            if (!currentType.isFloatingPoint()) {
                    trace.error("Object is not of base type DOUBLE or FLOAT for the supplied format.");
                return false;
            }
            break;

        case HEXADECIMAL:
            final boolean doNotContinueWithHexTypeSearch = !(formatterData.truncateToInt
                    && (currentType.isFloatingPoint()));
            if (doNotContinueWithHexTypeSearch && (SprintfDataType.LONG != currentType) &&
                    (SprintfDataType.INT != currentType)
                    && (SprintfDataType.SHORT != currentType)
                    && (SprintfDataType.CHAR  != currentType)
                    && (SprintfDataType.POINTER != currentType)
                    && (SprintfDataType.BYTE != currentType)) {
                    trace.error("Object is not of base type INTEGER for %x format.");
                return false;
            }
            break;

        case OCTAL:
            final boolean doNotContinueWithOctalTypeSearch = !(formatterData.truncateToInt
                    && currentType.isFloatingPoint());
            if (doNotContinueWithOctalTypeSearch
                    && ((SprintfDataType.LONG != currentType) && (SprintfDataType.INT != currentType)
                            && (SprintfDataType.SHORT != currentType)
                            && (SprintfDataType.CHAR  != currentType)
                            && (SprintfDataType.POINTER != currentType)
                            && (SprintfDataType.BYTE != currentType)) ) {
                // Is the base type valid
                    trace.error("Object is not of base type INTEGER for %o format.");
                return false;
            }
            break;

        default:
            break;
        }
        return true;
    }


    private static long convertDoubleToInt(final double doubleVal)
    {
        return StrictMath.round(doubleVal);
    }


    /**
     * This is just a private bucket into which we dump all of the variables the determine
     * the behavior of the format operation currently taking place.  Do not privatize the
     * members; that has a huge effect on performance because this class is used so often.
     *
     * Leave the unread fields alone until we can do more rewriting.
     *
     */
    @SuppressWarnings("URF_UNREAD_FIELD")
    private static class FormatterData extends Object
    {
        public char    formatChar;
        public boolean justification; // false == left
        public boolean unsignedValue;
        public boolean longValue;
        public boolean forceScientificNotation;
        public char leadingChar = ' ';
        public char trailingChar = ' ';
        public int fieldWidth = -1;
        public int decimalWidth = -1;
        public boolean signRequired;
        public boolean upperCase;
        public boolean spaceSignRequired;
        public TimeType timeType = TimeType.UNDEFINED;
        public boolean hexadecimal;
        public boolean octal;
        public boolean charOnly;
        public boolean truncateToInt;
        public boolean hasDecimalPoint = false;
        public boolean alternate = false;

        @SuppressWarnings("URF_UNREAD_FIELD")
        private void clear()
        {
            formatChar    = ' ';
            justification = false; // false == left
            unsignedValue = false;
            longValue = false;
            forceScientificNotation = false;
            leadingChar = ' ';
            trailingChar = ' ';
            fieldWidth = -1;
            decimalWidth = -1;
            signRequired = false;
            upperCase = false;
            spaceSignRequired = false;
            timeType = TimeType.UNDEFINED;
            hexadecimal = false;
            octal = false;
            truncateToInt = false;
            charOnly = false;
            hasDecimalPoint = false;
            alternate = false;
        }
    }


    /**
     * Append a bunch of pad characters.
     *
     * @param sb
     * @param c
     * @param count
     *
     * @return The sb
     */
    private static StringBuilder pad(final StringBuilder sb,
            final char          c,
            final int           count)
    {
        for (int i = 0; i < count; ++i)
        {
            sb.append(c);
        }

        return sb;
    }


    /**
     * Format with %f, %e, %E, %g, or %G and fix up.
     */
    private static String formatDouble(final double  value,
            final int     rawFieldSize,
            final int     rawPrecisionSize,
            final char    formatChar,
            final boolean hasPlus,
            final boolean hasBlank,
            final boolean hasZero,
            final boolean hasMinus,
            final boolean hasOctothorpe)
    {
        final int           fieldSize     = Math.max(rawFieldSize, 0);
        final boolean       isE           = ((formatChar == 'e') ||
                (formatChar == 'E'));
        final boolean       isF           = (formatChar == 'f');
        final boolean       isG           = ((formatChar == 'g') ||
                (formatChar == 'G'));
        final StringBuilder sb            = new StringBuilder(100);
        final int           precisionSize = (rawPrecisionSize >= 0)
        ? rawPrecisionSize
                : DEFAULT_PRECISION;
        String              temp          = null;

        if (isG && (value == 0.0D) && (precisionSize <= 1))
        {
            // Work around oddball bug in Java
            temp = "0";
        }
        else
        {
            // Let Java do the initial formatting

            sb.setLength(0);

            sb.append("%.").append(precisionSize).append(formatChar);

            temp = String.format(sb.toString(), Math.abs(value));
        }

        boolean useZero = hasZero;

        if (temp.equals("NaN")      ||
            temp.equals("NAN")      ||
            temp.equals("Infinity") ||
            temp.equals("INFINITY"))
        {
            useZero = false;
        }
        else if (isF)
        {
            if (hasOctothorpe && ! temp.contains("."))
            {
                sb.setLength(0);

                sb.append(temp).append('.');

                temp = sb.toString();
            }
        }
        else if ((isE || isG) && hasOctothorpe)
        {
            if (! temp.contains("."))
            {
                sb.setLength(0);

                sb.append(temp);

                final int e = temp.toLowerCase().indexOf('e');

                sb.insert((e >= 0) ? e : sb.length(), '.');

                temp = sb.toString();
            }
        }
        else if (isG && ! hasOctothorpe)
        {
            // Fixup for g/G format trailing zero suppression

            int start = temp.indexOf('.');

            if (start >= 0)
            {
                final int length = temp.length();
                int       end    = start + 1;
                boolean   delete = true;

                for (int i = start + 1; i < length; ++i)
                {
                    final char c = temp.charAt(i);

                    if ((c < '0') || (c > '9'))
                    {
                        // Not a digit, so exit with or without delete
                        break;
                    }

                    if (c == '0')
                    {
                        if (delete)
                        {
                            ++end;
                        }
                        else
                        {
                            delete = true;
                            start  = i;
                            end    = start + 1;
                        }
                    }
                    else
                    {
                        delete = false;
                    }
                }

                if (delete)
                {
                    sb.setLength(0);

                    sb.append(temp);

                    sb.delete(start, end);

                    temp = sb.toString();
                }
            }
        }

        // Finish by adding signs and padding

        sb.setLength(0);

        String sign = "";

        if ((value < 0.0D) || ((value == 0.0D) && ((1.0D / value) < 0.0D)))
        {
            sign = "-";
        }
        else if (hasPlus)
        {
            sign = "+";
        }
        else if (hasBlank)
        {
            sign = " ";
        }

        final int unpaddedSize = temp.length() + sign.length();
        final int padSize      = Math.max(fieldSize - unpaddedSize, 0);

        if (hasMinus)
        {
            // Left justified

            sb.append(sign).append(temp);

            pad(sb, ' ', padSize);
        }
        else if (useZero)
        {
            // Right justified with zero fill

            sb.append(sign);

            pad(sb, '0', padSize);

            sb.append(temp);
        }
        else
        {
            // Right justified with blank fill

            pad(sb, ' ', padSize);

            sb.append(sign).append(temp);
        }

        return sb.toString();
    }


    private static long expandUnsignedToLong(final Number n)
    {
        long l = 0L;

        if (n instanceof Long)
        {
            l = (Long) n;
        }
        else if (n instanceof Integer)
        {
            l = ((Integer) n) & INT_MASK;
        }
        else if (n instanceof Short)
        {
            l = ((Short) n) & SHORT_INT_MASK;
        }
        else if (n instanceof Byte)
        {
            l = ((Byte) n) & BYTE_INT_MASK;
        }
        else if (n instanceof Double)
        {
            l = StrictMath.round((Double) n);
        }
        else if (n instanceof Float)
        {
            l = StrictMath.round((Float) n);
        }

        return l;
    }


    private String formatHexOrOctal(final Number value)
    {
        final long longValue = value.longValue();
        String     basic     = null;

        if ((longValue == 0L)             &&
                formatterData.hasDecimalPoint &&
                (formatterData.decimalWidth <= 0))
        {
            if (formatterData.alternate && formatterData.octal)
            {
                basic = "0";
            }
            else
            {
                basic = "";
            }
        }
        else
        {
            basic = makeStringFromLong(longValue);
        }

        final int lengthBasic = basic.length();
        final int digitsPad   =
            Math.max(formatterData.decimalWidth - lengthBasic, 0);
        String    header      = "";

        if (formatterData.alternate && (longValue != 0L))
        {
            // Alternate form requires a header if non-zero

            if (formatterData.hexadecimal)
            {
                header = (formatterData.upperCase ? "0X" : "0x");
            }
            else if (formatterData.octal && (digitsPad == 0))
            {
                header = "0";
            }
        }

        final int withoutPad = lengthBasic + digitsPad + header.length();

        // Calculate the pad, and assign it to a character and location

        final int pad = Math.max(formatterData.fieldWidth - withoutPad, 0);

        int leftZeroPad   = 0;
        int leftBlankPad  = 0;
        int rightBlankPad = 0;

        if (formatterData.justification)
        {
            rightBlankPad = pad;
        }
        else if ((formatterData.leadingChar == '0') &&
                ! formatterData.hasDecimalPoint)
        {
            leftZeroPad = pad;
        }
        else
        {
            leftBlankPad = pad;
        }

        // Build the result

        final StringBuilder sb = new StringBuilder(withoutPad + pad);

        pad(sb, ' ', leftBlankPad);

        sb.append(header);

        pad(sb, '0', leftZeroPad + digitsPad);

        sb.append(basic);

        pad(sb, ' ', rightBlankPad);

        return sb.toString();
    }


    private static String rawFormatUnsignedLong(final long l)
    {
        if (l >= 0L)
        {
            return String.valueOf(l);
        }

        return BigInteger.valueOf(l ^ HIGH_BIT_MASK).add(HIGH_BIT).toString();
    }


    private String formatUnsignedDecimal(final Number value)
    {
        final long longValue = value.longValue();
        String     basic     = null;

        if ((longValue == 0L)             &&
                formatterData.hasDecimalPoint &&
                (formatterData.decimalWidth <= 0))
        {
            basic = "";
        }
        else
        {
            basic = rawFormatUnsignedLong(longValue);
        }

        final int lengthBasic = basic.length();
        final int digitsPad   =
            Math.max(formatterData.decimalWidth - lengthBasic, 0);
        final int withoutPad  = lengthBasic + digitsPad;

        // Calculate the pad, and assign it to a character and location

        final int pad = Math.max(formatterData.fieldWidth - withoutPad, 0);

        int leftZeroPad   = 0;
        int leftBlankPad  = 0;
        int rightBlankPad = 0;

        if (formatterData.justification)
        {
            rightBlankPad = pad;
        }
        else if ((formatterData.leadingChar == '0') &&
                ! formatterData.hasDecimalPoint)
        {
            leftZeroPad = pad;
        }
        else
        {
            leftBlankPad = pad;
        }

        // Build the result

        final StringBuilder sb = new StringBuilder(withoutPad + pad);

        pad(sb, ' ', leftBlankPad);

        pad(sb, '0', leftZeroPad + digitsPad);

        sb.append(basic);

        pad(sb, ' ', rightBlankPad);

        return sb.toString();
    }


    /**
     * Print without a sign.
     */
    private static String rawFormatSignedLong(final long l)
    {
        if (l >= 0L)
        {
            return String.valueOf(l);
        }

        if (l == HIGH_BIT_MASK)
        {
            // Cannot negate this one
            return HIGH_BIT_STRING;
        }

        return String.valueOf(- l);
    }


    private String formatSignedDecimal(final Number value)
    {
        final long longValue = value.longValue();
        String     basic     = null;

        if ((longValue == 0L)             &&
                formatterData.hasDecimalPoint &&
                (formatterData.decimalWidth <= 0))
        {
            basic = "";
        }
        else
        {
            basic = rawFormatSignedLong(longValue);
        }

        final int lengthBasic = basic.length();
        final int digitsPad   =
            Math.max(formatterData.decimalWidth - lengthBasic, 0);
        String    sign        = "";

        if (longValue < 0L)
        {
            sign = "-";
        }
        else if (formatterData.signRequired)
        {
            sign = "+";
        }
        else if (formatterData.spaceSignRequired)
        {
            sign = " ";
        }

        final int withoutPad = lengthBasic + digitsPad + sign.length();

        // Calculate the pad, and assign it to a character and location

        final int pad = Math.max(formatterData.fieldWidth - withoutPad, 0);

        int leftZeroPad   = 0;
        int leftBlankPad  = 0;
        int rightBlankPad = 0;

        if (formatterData.justification)
        {
            rightBlankPad = pad;
        }
        else if ((formatterData.leadingChar == '0') &&
                 ! formatterData.hasDecimalPoint)
        {
            leftZeroPad = pad;
        }
        else
        {
            leftBlankPad = pad;
        }

        // Build the result

        final StringBuilder sb = new StringBuilder(withoutPad + pad);

        pad(sb, ' ', leftBlankPad);

        sb.append(sign);

        pad(sb, '0', leftZeroPad + digitsPad);

        sb.append(basic);

        pad(sb, ' ', rightBlankPad);

        return sb.toString();
    }
    
    /**
     * Format a single value.
     *
     * @param formatString Format string
     * @param formatValue  Value to format
     * @return Formatted string
     */
    public String anCsprintf(final String formatString,
            final Object formatValue) {
      
        String aReturnValue = "";
    
        if (formatValue != null) {
            if (formatString != null && formatString.length() > 0) { // then we have something to process
                aReturnValue = sprintf(
                        formatString, new Object[] { formatValue });
            } else {
                trace.error(
                "A value was supplied without a format string!");
                aReturnValue = SprintfFormat.NSF; // value supplied but no format string
            }
        } else {
            if (formatValue == null) {
                trace.error(
                "No value was supplied for the formatting!");
                aReturnValue = SprintfFormat.NSF;
            }
        }
    
        return aReturnValue;
    } // end member function doCSprintf (static reference)
}
