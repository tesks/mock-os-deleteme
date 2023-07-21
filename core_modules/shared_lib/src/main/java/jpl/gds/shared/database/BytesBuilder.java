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
package jpl.gds.shared.database;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;

import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.sys.SystemUtilities;
import jpl.gds.shared.time.DbTimeUtility;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.TimeTooLargeException;
import jpl.gds.shared.types.UnsignedInteger;
import jpl.gds.shared.types.UnsignedLong;


/**
 * Works like a StringBuilder, but stores bytes instead of chars. Used to build
 * lines of data for use with LDI store classes. The lines are in bytes, not
 * characters because we are using UTF-8.
 *
 * The result can be considered as a "string" of eight-bit characters, bit
 * seven is always zero. Characters above seven bits are converted to a
 * replacement character. We also replace control characters and deletes.
 *
 * To speed things up a bit, we bypass UTF-8 conversion for strings known to
 * contain only safe characters, such as numeric conversions.
 *
 * The LDI code requires that certain characters be escaped. The result is
 * essentially a comma-separated-value string, so we must escape commas and
 * the escapes themselves, as well as line-ends. NUL characters are kind of a
 * special case, and must also be escaped; this is most likely because MySQL
 * is written in C.
 *
 * NULL (not NUL) values have a escape sequence as well. I mean NULL as in a
 * SQL NULL, not a zero character.
 *
 * Instances are reusable; the internal storage area grows as necessary.
 *
 * Timestamps format out to nanoseconds, which must be truncated to make MySQL
 * happy.
 *
 * The class is final in case that helps the compiler to optimize.
 *
 * Remember that an ANSI character is already in UTF-8, so by forcing all
 * characters to seven-bits we encode them as UTF-8 automatically.
 *
 * Summary of escaping:
 *
 * Text columns will always be forced to seven-bit ANSI. Escaping is done by
 * just adding the escape.
 *
 * Blob columns can be eight-bit and must also do some escaping. But in some
 * cases the byte escaped is replaced with a different byte. That's what the
 * substitution array is for.
 *
 * Arrays are used because they seem faster, but also so that changes need be
 * made only to the arrays and not the method logic. Arrays are always indexed
 * as int and not as byte since bytes are signed.
 *
 */
public final class BytesBuilder extends Object
{
    private static final int    TIMESTAMP_SIZE  = 19;
    private static final int    DEFAULT_SIZE    = 2000;
    private static final byte   ESCAPE_BYTE     = (byte) '\\';
    private static final byte   SEPARATOR_BYTE  = (byte) ',';
    private static final byte   TERMINATOR_BYTE = (byte) '\n';
    private static final byte   NUL_BYTE        = (byte) '\0'; // ANSI NUL
    private static final byte   ZERO_BYTE       = (byte) '0';
    private static final byte   N_BYTE          = (byte) 'N';
    private static final byte[] NULL_SEQUENCE   = {ESCAPE_BYTE, N_BYTE};
    private static final String ZERO_TIMESTAMP  =
        new Timestamp(0L).toString().substring(0, TIMESTAMP_SIZE);

    private static final char MIN_CHAR         = StringUtil.SQL_MIN_CHAR;
    private static final char MAX_CHAR         = StringUtil.SQL_MAX_CHAR;
    private static final byte REPLACEMENT_BYTE = (byte) ' ';
    private static final int  BYTE8_ARRAY_SIZE = 256;
    private static final int  BYTE8_MASK       = 255;
    private static final int  BYTE7_ARRAY_SIZE = 128;
    private static final int  BYTE7_MASK       = 127;

    // Information used in escaping; see static initializer
    private static final boolean[] _escape_text     = new boolean[BYTE7_ARRAY_SIZE];
    private static final boolean[] _escape_blob     = new boolean[BYTE8_ARRAY_SIZE];
    private static final byte[]    _substitute_blob = new byte[BYTE8_ARRAY_SIZE];

    // Storage area for bytes; expands as required.
    private byte[] _bytes = new byte[DEFAULT_SIZE];
    private int    _size  = 0; // Bytes in use


    static
    {
        // For fast escaping, we use arrays indexed by byte. The bytes are
        // treated as seven or eight-bit unsigned integers.
        // The escape arrays are used to detect the need for escaping, and the
        // substitution array supplies the byte value (in addition to the
        // backslash.)
        //
        // For blob, we need to escape NULs, but they do NOT escape as backslash
        // NUL, but as backslash zero character.
        //
        // For text, bad stuff like NUL has been turned to blanks, so there is no
        // need to escape control characters, and no substitution needed.

        Arrays.fill(_escape_text, false);
        Arrays.fill(_escape_blob, false);

        _escape_text[SEPARATOR_BYTE]  = true;
        _escape_text[ESCAPE_BYTE]     = true;

        _escape_blob[NUL_BYTE]        = true;
        _escape_blob[TERMINATOR_BYTE] = true;
        _escape_blob[SEPARATOR_BYTE]  = true;
        _escape_blob[ESCAPE_BYTE]     = true;

        for (int i = 0; i < BYTE8_ARRAY_SIZE; ++i)
        {
            _substitute_blob[i] = (byte) i;
        }

        _substitute_blob[NUL_BYTE] = ZERO_BYTE;
    }


    /**
     * Creates an instance of BytesBuilder.
     */
    public BytesBuilder()
    {
        super();
    }


    /**
     * Reset to empty
     */
    public void clear()
    {
        _size = 0;
    }


    /**
     * Return true if empty.
     *
     * @return True if empty
     */
    public boolean isEmpty()
    {
        return (_size == 0);
    }


    /**
     * Write bytes to output stream and clear
     *
     * @param fos Stream to write to
     *
     * @throws IOException I/O exception
     */
    public void write(final FileOutputStream fos) throws IOException
    {
        if ((fos != null) && (_size > 0))
        {
            fos.write(_bytes, 0, _size);
        }

        clear();
    }


    /**
     * Insert byte without escaping. The internal array is doubled in size if it
     * is full.
     *
     * @param b Byte to insert
     */
    public void insertRaw(final byte b)
    {
        if (_size == _bytes.length)
        {
            final byte[] old = _bytes;

            _bytes = new byte[2 * old.length];

            System.arraycopy(old, 0, _bytes, 0, _size);
        }

        _bytes[_size] = b;

        ++_size;
    }


    /**
     * Insert bytes without escaping. If the internal array does not have enough
     * room it is increased in size such that it is twice as large as required.
     *
     * @param b Bytes to insert
     */
    public void insertRaw(final byte[] b)
    {
        if (b.length > (_bytes.length - _size))
        {
            final byte[] old = _bytes;

            _bytes = new byte[2 * (_size + b.length)];

            System.arraycopy(old, 0, _bytes, 0, _size);
        }

        System.arraycopy(b, 0, _bytes, _size, b.length);

        _size += b.length;
    }


    /**
     * Insert string, repairing as needed. Characters above ANSI are turned
     * to blanks, as are control characters and deletes.
     *
     * This method is used for columns that are like "messages", i.e., that
     * may contain arbitrary text. We get rid of the junk.
     *
     * @param s String to insert
     *
     * @throws SQLException SQL exception
     */
    public void insertTextAllowReplace(final String s) throws SQLException
    {

        SystemUtilities.ignoreStatus(innerInsertText(s));
    }


    /**
     * Insert string, which may contain characters above ANSI
     * and may need escaping. Characters above ANSI are turned
     * to blanks, as are control characters and deletes.
     *
     * This method is used for columns that are have no reason to ever contain
     * junk. Throw if any replacements were necessary.
     *
     * @param s String to insert
     *
     * @throws SQLException SQL exception
     */
    public void insertTextComplainReplace(final String s) throws SQLException
    {

        if (! innerInsertText(s))
        {
            throw new SQLException("SQL character(s) out of bounds");
        }
    }


    /**
     * Insert string, which may contain characters above ANSI
     * and may need escaping. Characters above ANSI are turned
     * to blanks, as are control characters and deletes.
     *
     * @param s String to insert
     *
     * @return True if no characters were replaced
     *
     * @throws SQLException SQL exception
     */
    private boolean innerInsertText(final String s) throws SQLException
    {

        if (s == null)
        {
            return true;
        }

        final int len    = s.length();
        boolean   status = true;

        for (int i = 0; i < len; ++i)
        {
            final char c = s.charAt(i);

            if ((c >= MIN_CHAR) && (c <= MAX_CHAR))
            {
                // Mask is logically needed so we do not depend upon the
                // character range.
                final int ci = c & BYTE7_MASK;

                if (_escape_text[ci])
                {
                    insertRaw(ESCAPE_BYTE);
                }

                // No replacement for text, just escaping

                insertRaw((byte) ci);
            }
            else
            {
                // Clobber the bad character.
                // We assume that the replacement
                // does not need escaping

                insertRaw(REPLACEMENT_BYTE);

                status = false;
            }
        }

        return status;
    }




    /**
     * Insert string, repairing as needed. Characters above ANSI are turned
     * to blanks, as are control characters and deletes.
     *
     * Insert as a NULL if null or empty.
     *
     * @param s String to insert
     *
     * @throws SQLException SQL exception
     */
    public void insertTextOrNullAllowReplace(final String s)
        throws SQLException
    {

        SystemUtilities.ignoreStatus(innerInsertTextOrNull(s));
    }


    /**
     * Insert string, which may contain characters above ANSI
     * and may need escaping. Characters above ANSI are turned
     * to blanks, as are control characters and deletes.
     *
     * This method is used for columns that are have no reason to ever contain
     * junk. Throw if any replacements were necessary.
     *
     * Insert as a NULL if null or empty.
     *
     * Throw if any replacements were necessary.
     *
     * @param s String to insert
     *
     * @throws SQLException SQL exception or replacement performed
     */
    public void insertTextOrNullComplainReplace(final String s)
        throws SQLException
    {

        if (! innerInsertTextOrNull(s))
        {
            throw new SQLException("SQL character(s) out of bounds");
        }
    }


    /**
     * Insert String, which may contain characters above ANSI or junk
     * and may need escaping. Insert as a NULL if null or empty.
     *
     * @param s String to insert
     *
     * @return True if no characters were replaced
     *
     * @throws SQLException SQL exception
     */
    private boolean innerInsertTextOrNull(final String s) throws SQLException
    {
        /** Redo UTF-8 conversion */

        final String ss = StringUtil.emptyAsNull(s);

        if (ss != null)
        {
            return innerInsertText(ss);
        }

        insertNULL();

        return true;
    }


    /**
     * Insert blob, which may need escaping.
     * Note that blobs can take control characters.
     *
     * @param blob Bytes to insert
     */
    public void insertBlob(final byte[] blob)
    {
        if (blob == null)
        {
            return;
        }

        for (final byte b : blob)
        {
            final int next = b & BYTE8_MASK;

            if (_escape_blob[next])
            {
                insertRaw(ESCAPE_BYTE);
            }

            insertRaw(_substitute_blob[next]);
        }
    }


    /**
     * Insert characters which do not need escaping.
     *
     * @param s String to insert
     */
    public void insertSafe(final String s)
    {
        if (s != null)
        {
            insertRaw(s.getBytes());
        }
    }


    /**
     * Insert bytes which do not need escaping.
     *
     * @param bytes Bytes to insert
     */
    public void insertSafe(final byte[] bytes)
    {
        if (bytes != null)
        {
            insertRaw(bytes);
        }
    }


    /**
     * Insert numeric types.
     *
     * @param n Number to insert
     */
    public void insert(final double n)
    {
        insertSafe(Double.toString(n));
    }


    /**
     * Insert numeric types
     *
     * @param n Number to insert
     */
    public void insert(final float n)
    {
        insertSafe(Float.toString(n));
    }


    /**
     * Insert Long as number or NULL.
     *
     * @param n Number to insert or null
     */
    public void insertLongOrNull(final Long n)
    {
        if (n == null)
        {
            insertNULL();
        }
        else
        {
            insert(n.longValue());
        }
    }


    /**
     * Insert Integer as number or NULL.
     *
     * @param n Number to insert or null
     */
    public void insertIntegerOrNull(final Integer n)
    {
        if (n == null)
        {
            insertNULL();
        }
        else
        {
            insert(n.intValue());
        }
    }


    /**
     * Insert numeric types
     *
     * @param n Number to insert
     */
    public void insert(final long n)
    {
        insertSafe(Long.toString(n));
    }


    /**
     * Insert unsigned long.
     *
     * @param n Number to insert
     */
    public void insert(final UnsignedLong n)
    {
        insertSafe(n.toString());
    }


    /**
     * Insert unsigned integer.
     *
     * @param n Number to insert
     *
     */
    public void insert(final UnsignedInteger n)
    {
        insertSafe(n.toString());
    }


    /**
     * Insert big integer.
     *
     * @param n Number to insert
     */
    public void insert(final BigInteger n)
    {
        insertSafe(n.toString());
    }


    /**
     * Insert long as unsigned.
     *
     * @param n Number to insert
     */
    public void insertLongAsUnsigned(final long n)
    {
        insertSafe(UnsignedLong.valueOfLongAsUnsigned(n).toString());
    }


    /**
     * Insert numeric types
     *
     * @param n Number to insert
     */
    public void insert(final int n)
    {
        insertSafe(Integer.toString(n));
    }


    /**
     * Insert numeric types
     *
     * @param n Number to insert
     */
    public void insert(final short n)
    {
        insertSafe(Short.toString(n));
    }


    /**
     * Insert boolean types
     *
     * @param b Boolean to insert
     */
    public void insert(final boolean b)
    {
        insertSafe(Boolean.toString(b));
    }


    /**
     * Insert Date
     *
     * @param d Date to insert
     */
    public void insert(final IAccurateDateTime d)
    {
        insert((d != null) ? new Timestamp(d.getTime()) : (Timestamp) null);
    }


    /**
     * Insert Timestamp
     *
     * @param ts Timestamp to insert
     */
    public void insert(final Timestamp ts)
    {
        if (ts != null)
        {
            // Ignore nanoseconds

             insertSafe(ts.toString().substring(0, TIMESTAMP_SIZE));
        }
        else
        {
            insertSafe(ZERO_TIMESTAMP);
        }
    }


    /**
     * Insert Date as a long
     *
     * @param d Date to insert
     */
    public void insertAsLong(final IAccurateDateTime d)
    {
        insert((d != null) ? d.getTime() : 0L);
    }


    /**
     * Insert ISclk
     *
     * @param s SCLK to insert
     */
    public void insert(final ISclk s)
    {
        if (s != null)
        {
            insertSafe(s.toString());
        }
    }


    /**
     * Insert ISclk as a long
     *
     * @param s SCLK to insert
     */
    public void insertAsLong(final ISclk s)
    {
        if (s != null)
        {
            insert(s.getBinaryGdrLong());
        }
        else
        {
            insert(0L);
        }
    }


    /**
     * Insert ISclk as coarse and fine and separate.
     *
     * @param s SCLK to insert
     */
    public void insertSclkAsCoarseFineSeparate(final ISclk s)
    {
        if (s != null)
        {
            insertSclkAsCoarseFineSeparate(s.getCoarse(), s.getFine());
        }
        else
        {
            insertSclkAsCoarseFineSeparate(0L, 0L);
        }
    }


    /**
     * Insert ISclk as coarse and fine and separate.
     *
     * @param c SCLK coarse
     * @param f SCLK fine
     */
    public void insertSclkAsCoarseFineSeparate(final long c,
                                               final long f)
    {
        insert(c);
        insertSeparator();
        insert(f);
        insertSeparator();
    }


    /**
     * Insert ERT as coarse and fine and separate. Note that we only throw after
     * finishing our work.
     *
     * @param ert ERT to insert
     *
     * @throws TimeTooLargeException Time cannot be represented in database
     */
    public void insertErtAsCoarseFineSeparate(final IAccurateDateTime ert)
        throws TimeTooLargeException
    {
        long exact = 0L;
        int  nano  = 0;

        if (ert != null)
        {
            exact = ert.getTime();
            nano  = (int) ert.getNanoseconds();
        }

        long                  coarse = 0L;
        int                   fine   = 0;
        TimeTooLargeException error  = null;

        try
        {
            coarse = DbTimeUtility.coarseFromExact(exact);
            fine   = DbTimeUtility.ertFineFromExact(exact, nano);
        }
        catch (final TimeTooLargeException ttle)
        {
            coarse = DbTimeUtility.MAX_COARSE;
            fine   = DbTimeUtility.MAX_ERT_FINE;
            error  = ttle;
        }

        insert(coarse);
        insertSeparator();
        insert(fine);
        insertSeparator();

        if (error != null)
        {
            // Rethrow the error, after we are done
            throw error;
        }
    }


    /**
     * Insert SCET as coarse and fine and separate. Note that we only throw after
     * finishing our work.
     *
     * @param scet SCET to insert
     *
     * @throws TimeTooLargeException Time cannot be represented in database
     *
     */
    public void insertScetAsCoarseFineSeparate(final IAccurateDateTime scet)
        throws TimeTooLargeException
    {
        long exact = 0L;
        int  nano  = 0;

        if (scet != null)
        {
            exact = scet.getTime();
            nano  = (int) scet.getNanoseconds();
        }

        long                  coarse = 0L;
        int                   fine   = 0;
        TimeTooLargeException error  = null;

        try
        {
            coarse = DbTimeUtility.coarseFromExact(exact);
            fine   = DbTimeUtility.scetFineFromExact(exact, nano);
        }
        catch (final TimeTooLargeException ttle)
        {
            coarse = DbTimeUtility.MAX_COARSE;
            fine   = DbTimeUtility.MAX_SCET_FINE;
            error  = ttle;
        }

        insert(coarse);
        insertSeparator();
        insert(fine);
        insertSeparator();

        if (error != null)
        {
            // Rethrow the error, after we are done
            throw error;
        }
    }
    
	public void insertDateAsCoarse(final IAccurateDateTime date) throws TimeTooLargeException {
		long exact = 0L;

		if (date != null) {
			exact = date.getTime();
		}

		long coarse = 0L;
		TimeTooLargeException error = null;

		try {
			coarse = DbTimeUtility.coarseFromExact(exact);
		} catch (final TimeTooLargeException ttle) {
			coarse = DbTimeUtility.MAX_COARSE;
			error = ttle;
		}

		insert(coarse);
		insertSeparator();

		if (error != null) {
			// Rethrow the error, after we are done
			throw error;
		}
	}

    /**
     * Insert SCET as coarse and fine and separate. Note that we only throw after
     * finishing our work. Truncate fine to old range.
     *
     * @param scet SCET to insert
     *
     * @throws TimeTooLargeException Time cannot be represented in database
     *
     */
    public void insertScetAsCoarseFineSeparateShort(final IAccurateDateTime scet)
        throws TimeTooLargeException
    {
        long exact = 0L;
        int  nano  = 0;

        if (scet != null)
        {
            exact = scet.getTime();
            nano  = (int) scet.getNanoseconds();
        }

        long                  coarse = 0L;
        int                   fine   = 0;
        TimeTooLargeException error  = null;

        try
        {
            coarse = DbTimeUtility.coarseFromExact(exact);
            fine   = DbTimeUtility.scetFineFromExactShort(exact, nano);
        }
        catch (final TimeTooLargeException ttle)
        {
            coarse = DbTimeUtility.MAX_COARSE;
            fine   = DbTimeUtility.MAX_SCET_FINE_SHORT;
            error  = ttle;
        }

        insert(coarse);
        insertSeparator();
        insert(fine);
        insertSeparator();

        if (error != null)
        {
            // Rethrow the error, after we are done
            throw error;
        }
    }


    /**
     * Insert other time as coarse and fine and separate. (RCT, etc.) Note that
     * we only throw after finishing our work.
     *
     * @param date Date to insert
     *
     * @throws TimeTooLargeException Time cannot be represented in database
     */
    public void insertDateAsCoarseFineSeparate(final IAccurateDateTime date)
        throws TimeTooLargeException
    {
        final long            exact  = ((date != null) ? date.getTime() : 0L);
        long                  coarse = 0L;
        int                   fine   = 0;
        TimeTooLargeException error  = null;

        try
        {
            coarse = DbTimeUtility.coarseFromExact(exact);
            fine   = DbTimeUtility.fineFromExact(exact);
        }
        catch (final TimeTooLargeException ttle)
        {
            coarse = DbTimeUtility.MAX_COARSE;
            fine   = DbTimeUtility.MAX_FINE;
            error  = ttle;
        }

        insert(coarse);
        insertSeparator();
        insert(fine);
        insertSeparator();

        if (error != null)
        {
            throw error;
        }
    }


    /**
     * Insert NULL indicator
     */
    public void insertNULL()
    {
        insertRaw(NULL_SEQUENCE);
    }


    /**
     * Insert separator
     */
    public void insertSeparator()
    {
        insertRaw(SEPARATOR_BYTE);
    }


    /**
     * Insert terminator
     */
    public void insertTerminator()
    {
        insertRaw(TERMINATOR_BYTE);
    }


    /**
     * Get current contents.
     *
     * @return Contents
     */
    public byte[] getBytes()
    {
        final byte[] result = new byte[_size];

        System.arraycopy(_bytes, 0, result, 0, _size);

        return result;
    }
    
    @Override
    public String toString() {
        return new String(_bytes);
    }
}
