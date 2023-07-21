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
package jpl.gds.shared.time;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.file.FileUtility;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.BeanUtil;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.time.SclkFmt.SclkFormatter;


/**
 * This is a lower level class that will do a SCLK/SCET conversion for a particular spacecraft. For each spacecraft (for each
 * sclk/scet correlation file) there will be an associated instance of this class.
 *
 * Developers should use the SclkScetUtility class to do conversions.
 *
 * NOTE: Assuming SCLK Rate has units of seconds per tick
 *
 * Some spots as marked with WRONG so we don't miss them when we remove double later.
 *
 *
 */
@SuppressWarnings("serial")
public class SclkScetConverter implements Serializable {
    /** Spacecraft name */
    public final String            SPACECRAFT_NAME            = "SPACECRAFT_NAME";
    /** Mission name */
    public final String            MISSION_NAME            = "MISSION_NAME";
    /** Data Set ID */
    public final String            DATA_SET_ID                = "DATA_SET_ID";
    /** File name */
    public final String            FILE_NAME                = "FILE_NAME";
    /** Product Creation Time */
    public final String            PRODUCT_CREATION_TIME    = "PRODUCT_CREATION_TIME";
    /** Product Version ID */
    public final String            PRODUCT_VERSION_ID        = "PRODUCT_VERSION_ID";
    /** Producer ID */
    public final String            PRODUCER_ID                = "PRODUCER_ID";
    /** Applicable start time */
    public final String            APPLICABLE_START_TIME    = "APPLICABLE_START_TIME";
    /** Applicable end time */
    public final String            APPLICABLE_STOP_TIME    = "APPLICABLE_STOP_TIME";
    /** Mission ID */
    public final String            MISSION_ID                = "MISSION_ID";
    /** Spacecraft ID */
    public final String            SPACECRAFT_ID            = "SPACECRAFT_ID";

    private static final BigDecimal  MILLION_D = BigDecimal.valueOf(1_000_000L);
    private static final BigInteger  MILLION_I = BigInteger.valueOf(1_000_000L);
    private static final BigDecimal  HALF      = BigDecimal.valueOf(0.5D);
    private static final MathContext CONTEXT   = MathContext.DECIMAL128;

    /**
     * Map containing SCLK/SCET File header data (version info, mission, etc.).
     */
    private final Map<String, String>    metadata;

    /** The table of entries for this sclk/scet correlation */
    private SclkScetEntry[]        table;

    private String                filename;

    private final SclkFormatter sclkFmt = TimeProperties.getInstance().getSclkFormatter();

    private static final boolean SCET_PRECISION_EXTENDED = TimeProperties.getInstance().getScetPrecision() > 3;
    private static final boolean USE_EXTENDED_SCET       = TimeProperties.getInstance().useExtendedScet();


    /**
     * Creates an instance of SclkScetConverter.
     */
    private SclkScetConverter()
    {
        this.table = null;
        this.filename = null;
        this.metadata = new HashMap<>(11);
    }

    /**
     * Static method to create a SCLK/SCET converter for a particular
     * spacecraft. This is the only way to create an instance of this class.
     *
     * @param scid
     *            The spacecraft ID whose SCLK/SCET correlation is wanted
     * @param log
     *            The application Tracer
     *
     * @return The sclk/scet converter for the specified spacecraft or null if
     *         the correlation file cannot be found or there is an error parsing
     *         the correlation file.
     */
    public static SclkScetConverter createConverter(final int scid, final Tracer log)
    {

        String filename = getSclkScetFilePath(scid);
        if (filename == null) {
            return (null);
        }

        final SclkScetConverter converter = new SclkScetConverter();
        try {
            if (!converter.parseCorrelationFile(filename, log)) {
                return (null);
            }
        }
        catch (final Exception e) {
            log.error("Unexpected extention parsing sclk/scet file ", ExceptionTools.getMessage(e), e);
        }

        // Validate the expected and actual spacecraft ID values. Warn the user if they're different.
        if(scid != Integer.parseInt(converter.metadata.get("SPACECRAFT_ID"))){
            log.warn(Markers.TIME_CORR,
                    "Given Spacecraft ID and value in SCLK/SCET file do not match: Expected: " + scid
                    + " in file: " + converter.metadata.get("SPACECRAFT_ID"));
        }

        return (converter);
    }

    public static String getSclkScetFilePath(final int scid) {
        try {
            return GdsSystemProperties.getMostLocalPath("sclkscet." + scid,
                    BeanUtil.getBean(SseContextFlag.class).isApplicationSse());
        }
        catch (final Exception e) {
            return GdsSystemProperties.getMostLocalPath("sclkscet." + scid);
        }
    }

    /**
     * Static method to create a SCLK/SCET converter for a particular
     * spacecraft.
     *
     * WARNING: THIS METHOD IS PRIMARILY MEANT TO BE USED BY TEST FUNCTIONS ONLY
     * It CANNOT validate that the spacecraft specified by the filename is valid
     * or matches the spacecraft specified in the file.
     *
     * @param filename
     *            The filename, including any full or relative path, of a
     *            desired SCLK/SCET correlation file.
     *
     * @return The sclk/scet converter for the specified file or null if the
     *         correlation file cannot be found or there is an error parsing the
     *         correlation file.
     */
    public static SclkScetConverter createConverter(final String filename) {
        if (filename == null) {
            return (null);
        }

        final SclkScetConverter converter = new SclkScetConverter();
        if (!converter.parseCorrelationFile(filename, TraceManager.getDefaultTracer())) {
            return (null);
        }

        return (converter);
    }


    /**
     * Convert the given SCET time to a SCLK time.
     *
     * NB: We do not care about the SCET extended precision mode here.
     * We just take the nanoseconds as is.
     *
     * NB: Be careful with rounding! That is why below we use
     * divideAndRemainder instead of breaking the integer and fractional
     * parts out independently. Also, after the fine is converted to a
     * count from a fraction we check for overflow and adjust the coarse.
     *
     * @param scet
     *            The SCET time to convert
     *
     * @param ert
     *            The ERT to use in the conversion...if SCLK resets are not a concern, this parameter may be null
     *
     * @return The SCLK time corresponding to the SCET input
     *
     */
    public ISclk to_sclk(final IAccurateDateTime scet,
                        final IAccurateDateTime ert)
    {
        if (scet == null)
        {
            throw new IllegalArgumentException("Null input SCET");
        }

        if ((table == null) || (table.length == 0))
        {
            // TODO check if this is backwards compatible
            return new Sclk(true);
        }

        // assumes no sclk resets
        // if sclk resets can occur must use ert to
        // find appropriate entry

        int ind = 0;

        for (ind = 0; ind < table.length; ++ind)
        {
            if (scet.compareTo(table[ind].getScet()) < 0)
            {
                break;
            }
        }

        // At this point, "ind" is pointing to the entry that is directly past where the
        // SCET falls in the table...meaning the input SCET is between table[ind-1] and table[ind]
        // Except that ind may point past the table.

        ISclk sclk = null;

        final SclkScetEntry sse0 = ((ind < table.length) ? table[ind] : null);

        if (ind == 0)
        {
            //input SCET is before first table entry

            //Assume that SCET0 represents the first row of the table
            //Assume that SCLK_RATE has units of seconds/sclk_ticks
            //Assume that SCET is the user input SCET value and SCLK is the value we're trying to find

            //SCET0-SCET (divide by 1000 because getTime() returns milliseconds)
            // double scet_secs_before = (sse0.getScet().getTime() - scet.getTime()) / 1000.0;
            final BigDecimal scet_secs_before =
                sse0.getScet().asFractionalSeconds().subtract(scet.asFractionalSeconds(), CONTEXT);

            //(SCET0-SCET) * (1/SCLK_RATE)
            // double time_to_subtract = scet_secs_before / sse0.getSclkRate();
            final BigDecimal time_to_subtract =
                scet_secs_before.divide(BigDecimal.valueOf(sse0.getSclkRate()), CONTEXT);

            //create a SCLK from the first table entry and then decrement it by the
            //amount of seconds it will be before that entry
            sclk = new Sclk(sse0.getSclk());

            final long FULP1 = sclk.getFineUpperLimit() + 1L;

            //the coarse is equal to the part of the result before the decimal point
            // long coarse_to_subtract = (long) Math.floor(time_to_subtract);

            //the fine is equal to the part of the result after the decimal point
            //translated onto the fine scale (fines are usually 1/256 secs or 1/65536 secs)
            // double secs_to_subtract = time_to_subtract % 1;

            // int fine_to_subtract = (int) Math.round(secs_to_subtract * FULP1);

            final BigDecimal[] split = time_to_subtract.divideAndRemainder(BigDecimal.ONE, CONTEXT);

            final long coarse_to_subtract = split[0].longValue();
            final long fine_to_subtract   =
                split[1].multiply(BigDecimal.valueOf(FULP1), CONTEXT).add(HALF, CONTEXT).longValue();

            // Check for overflow from fine
            if (fine_to_subtract < FULP1)
            {
                sclk = sclk.decrement(coarse_to_subtract, fine_to_subtract);
            }
            else
            {
                sclk = sclk.decrement(coarse_to_subtract + 1L, fine_to_subtract - FULP1);
            }
        }
        else
        {
            final SclkScetEntry sse1 = table[ind - 1];

            BigDecimal time_to_add = BigDecimal.ZERO;

            if (ind == table.length)
            {
                //input SCET is past the last table entry

                //Assume that SCET0 represents the last row of the table
                //Assume that SCLK_RATE has units of seconds/sclk_ticks
                //Assume that SCET is the user input SCET value and SCLK is the value we're trying to find

                //SCET-SCET0 (divide by 1000 because getTime() returns milliseconds)
                // double scet_secs_past = (scet.getTime() - sse1.getScet().getTime()) / 1000.0;
                final BigDecimal scet_secs_past =
                    scet.asFractionalSeconds().subtract(sse1.getScet().asFractionalSeconds(), CONTEXT);

                //(SCET-SCET0) * (1/SCLK_RATE)
                // time_to_add = scet_secs_past / sse1.getSclkRate();
                time_to_add = scet_secs_past.divide(BigDecimal.valueOf(sse1.getSclkRate()), CONTEXT);
            }
            else
            {
                //input SCET is between two table entries

                //Assume that SCLK0 and SCET0 represent the lower bound row
                //Assume that SCLK1 and SCET1 represent the upper bound row
                //Assume that SCET is the user input SCET value and SCLK is the value we're trying to find

                //SCET1 - SCET0
                // double scet_bin_width = sse0.getScet().getTime() - sse1.getScet().getTime();
                final BigDecimal scet_bin_width =
                    sse0.getScet().asFractionalSeconds().subtract(sse1.getScet().asFractionalSeconds(), CONTEXT);

                //SCET - SCET0
                // double scet_difference = scet.getTime() - sse1.getScet().getTime();
                final BigDecimal scet_difference =
                    scet.asFractionalSeconds().subtract(sse1.getScet().asFractionalSeconds(), CONTEXT);

                //(SCET - SCET0)/(SCET1-SCET0)
                // double ratio_percentage = scet_difference / scet_bin_width;
                final BigDecimal ratio_percentage = scet_difference.divide(scet_bin_width, CONTEXT);

                //SCLK1 - SCLK0
                // double sclk_bin_width = sse0.getSclk().getFloatingPointTime() - sse1.getSclk().getFloatingPointTime();
                final BigDecimal sclk_bin_width =
                    BigDecimal.valueOf(sse0.getSclk().getFloatingPointTime() - sse1.getSclk().getFloatingPointTime());

                //SCLK = ((SCET-SCET0)/(SCET1-SCET0)) * (SCLK1-SCLK0)
                // time_to_add = (ratio_percentage * sclk_bin_width);
                time_to_add = ratio_percentage.multiply(sclk_bin_width, CONTEXT);
            }

            //create a SCLK from the last table entry that occurred before the input SCET
            sclk = new Sclk(sse1.getSclk());

            final long FULP1 = sclk.getFineUpperLimit() + 1L;

            //the coarse is equal to the part of the result before the decimal point
            // long coarse_to_add = (long) Math.floor(time_to_add);

            //the fine is equal to the part of the result after the decimal point
            //translated onto the fine scale (fines are usually 1/256 secs or 1/65536 secs)
            // double msecs_to_add = time_to_add % 1;

            // int fine_to_add = (int) Math.round(msecs_to_add * (sclk.getFineUpperLimit() + 1));

            final BigDecimal[] split = time_to_add.divideAndRemainder(BigDecimal.ONE, CONTEXT);

            final long coarse_to_add = split[0].longValue();
            final long fine_to_add   =
                split[1].multiply(BigDecimal.valueOf(FULP1), CONTEXT).add(HALF, CONTEXT).longValue();

            // Check for overflow from fine
            if (fine_to_add < FULP1)
            {
                sclk = sclk.increment(coarse_to_add, fine_to_add);
            }
            else
            {
                sclk = sclk.increment(coarse_to_add + 1L, fine_to_add - FULP1);
            }
        }

        return sclk;
    }


    /**
     * Convert the given SCLK time to a SCET time.
     *
     * WRONG! Cannot use double!
     *
     * @param sclk
     *            The SCLK time to convert
     *
     * @param ert
     *            The ERT to use in the conversion...if SCLK resets are not a concern, this parameter may be null
     *
     * @return The SCET time corresponding to the SCLK input
     *
     */
    public IAccurateDateTime to_scet(final ICoarseFineTime sclk,
                                    final IAccurateDateTime ert)
    {
        if (sclk == null)
        {
            throw new IllegalArgumentException("Null input SCLK");
        }

        if ((table == null) || (table.length == 0))
        {
            return new AccurateDateTime(sclk.getCoarse());
        }

        // this method assumes no sclk resets
        // if sclk resets can occur must use ert to
        // find appropriate entry

        // find the table entry just past the input time

        int ind = 0;

        for (ind = 0; ind < table.length; ++ind)
        {
            if (sclk.compareTo(table[ind].getSclk()) < 0)
            {
                break;
            }
        }

        // At this point, "ind" is pointing to the entry that is directly past where the
        // SCLK falls in the table...meaning the input SCLK is between table[ind-1] and table[ind]
        // except may point past the table.

        IAccurateDateTime scet = null;

        final SclkScetEntry sse0 = ((ind < table.length) ? table[ind] : null);

        if (ind == 0)
        {
            //input SCLK is before the first table entry

            //Assume that SCLK0 represents the first row of the table
            //Assume that SCLK_RATE has units of seconds/sclk_ticks
            //Assume that SCLK is the user input SCLK value and SCET is the value we're trying to find

            //SCLK0 - SCLK
            final double sclk_secs_before = sse0.getSclk().getFloatingPointTime() - sclk.getFloatingPointTime();

            //((SCLK0-SCLK)*SCLK_RATE)*1000 (multiply by 1000 to get milliseconds)
            final double msecs_to_subtract = (sclk_secs_before * sse0.getSclkRate()) * 1000.0D;

            //create a SCET from the first table entry and then decrement it by the
            //amount of seconds it will be before that entry
            scet = interpolatedScet(sse0.getScet(), - msecs_to_subtract);
        }
        else
        {
            double msecs_to_add = 0.0D;

            final SclkScetEntry sse1 = table[ind - 1];

            if (ind == table.length)
            {
                //input SCLK is past the last table entry

                //Assume that SCLK0 represents the last row of the table
                //Assume that SCLK_RATE has units of seconds/sclk_ticks
                //Assume that SCLK is the user input SCLK value and SCET is the value we're trying to find

                //SCLK-SCLK0
                final double sclk_secs_past = sclk.getFloatingPointTime() - sse1.getSclk().getFloatingPointTime();

                //((SCLK-SCLK0) * SCLK_RATE) * 1000 (we multiply by 1000 to turn seconds into milliseconds)
                msecs_to_add = (sclk_secs_past * sse1.getSclkRate()) * 1000.0D;
            }
            else
            {
                //input SCLK is between two table entries

                //Assume that SCLK0 and SCET0 represent the lower bound row
                //Assume that SCLK1 and SCET1 represent the upper bound row
                //Assume that SCLK is the user input SCLK value and SCET is the value we're trying to find

                //SCLK1 - SCLK0
                final double sclk_bin_width = sse0.getSclk().getFloatingPointTime() - sse1.getSclk().getFloatingPointTime();

                //SCLK - SCLK0
                final double sclk_difference = sclk.getFloatingPointTime() - sse1.getSclk().getFloatingPointTime();

                //(SCLK-SCLK0)/(SCLK1-SCLK0)
                final double ratio = sclk_difference / sclk_bin_width;

                //SCET1 - SCET0
                final double scet_bin_width = sse0.getScet().getTime() - sse1.getScet().getTime();

                //((SCLK-SCLK0)/(SCLK1-SCLK0))*(SCET1-SCET0)
                msecs_to_add = ratio * scet_bin_width;
            }

            //create a SCET from the beginning of the bin and add the milliseconds based on the ratio to it
            scet = interpolatedScet(sse1.getScet(), msecs_to_add);
        }

        return scet;
    }


    /**
     * Parse the given sclk/scet correlation file to populate the internal value table of this class. SCLK/SCET conversions cannot
     * be done properly by this class until a correlation file has successfully been read.
     *
     * @param correlationFilePath
     *            The path to the sclk/scet correlation file
     *
     * @return True if the correlation was parsed successfully, false otherwise (if false is returned, this object cannot be counted
     *         on to do conversions properly)
     */
    private boolean parseCorrelationFile(final String correlationFilePath, final Tracer log) {
        if (correlationFilePath == null) {
            throw new IllegalArgumentException("Null input correlation file path");
        }

        final ArrayList<SclkScetEntry> entry = new ArrayList<SclkScetEntry>(64);
        ISclk sclk = null;
        IAccurateDateTime scet = null;
        double dup = 60.0;
        double rate = 1.0;
        LineNumberReader lnr = null;

        try {
            lnr = new LineNumberReader(new FileReader(new File(correlationFilePath)));
        }
        catch (final FileNotFoundException e) {
            log.error(Markers.TIME_CORR, "Can't open SCLK/SCET file " + correlationFilePath);
            return (false);
        }

        while (true) {
            String ln;
            try {
                ln = lnr.readLine();
            }
            catch (final IOException e) {
                break;
            }
            if (ln == null)
                break;
            if (ln.startsWith(" ")) {
                final StringTokenizer st = new StringTokenizer(ln);
                int i = 0;
                while (st.hasMoreTokens()) {
                    final String elm = st.nextToken();
                    if (i == 0) {
                        try {
                            sclk = sclkFmt.valueOf(elm);
                        }
                        catch (final NumberFormatException e) {
                            e.printStackTrace();
                            try {
                                lnr.close();
                            }
                            catch (final IOException ex) {
                                ex.printStackTrace();
                            }
                            return (false);
                        }
                    }
                    else if (i == 1) {
                        try {
                            scet = new AccurateDateTime(elm);
                        }
                        catch (final ParseException e) {
                            e.printStackTrace();
                            try {
                                lnr.close();
                            }
                            catch (final IOException e1) {
                                e1.printStackTrace();
                            }
                            return (false);
                        }
                    }
                    else if (i == 2) {
                        try {
                            dup = Double.parseDouble(elm);
                        }
                        catch (final NumberFormatException e) {
                            dup = 65.184;
                        }
                    }
                    else if (i == 3) {
                        try {
                            rate = Double.parseDouble(elm);
                        }
                        catch (final NumberFormatException e) {
                            rate = 1.0;
                        }
                    }
                    ++i;
                }

                final SclkScetEntry sce = new SclkScetEntry();

                sce.setSclk(sclk);
                sce.setScet(scet);
                sce.setDut(dup);
                sce.setSclkRate(rate);

                entry.add(sce);
            }

            /*
             * Collect metadata at top of SCLK/SCET file.
             */
            else if (ln.contains("=") && ln.endsWith(";")) {
                final String[] keyValuePair = ln.split("[=;]");
                if (2 == keyValuePair.length) {
                    metadata.put(keyValuePair[0], keyValuePair[1]);
                }
            }
        }

        try {
            lnr.close();
        }
        catch (final Exception e) {
            e.printStackTrace();
        }

        //populate the local entry table
        table = new SclkScetEntry[entry.size()];
        for (int i = 0; i < entry.size(); i++) {
            table[i] = entry.get(i);
        }

        this.filename = correlationFilePath;

        // Add log statement for SCLK/SCET file loading
        log.info(Markers.TIME_CORR, "Successfully loaded " + this);

        return (true);
    }


    /**
     *
     * @return an array of Strings representing all metadata tags found in SCLK/SCET file.
     */
    public String[] getMetaTags() {
        return metadata.keySet().toArray(new String[metadata.size()]);
    }

    /**
     *
     * @param tag
     *            the metadata tag whose value is to be returned.
     * @return the value of the metadata tag specified, or null if tag does not exist.
     */
    public String getMetaValue(final String tag) {
        return metadata.get(tag);
    }

    @Override
    public String toString() {
        return "SCLK/SCET file: " + FileUtility.createFilePathLogMessage(this.filename) + " (Version: " + metadata.get(PRODUCT_VERSION_ID) + ": MISSION: " + metadata.get(MISSION_NAME) + ", SCID=" + metadata.get(SPACECRAFT_ID)
                + " [VALID TIME RANGE: " + metadata.get(APPLICABLE_START_TIME) + " -- " + metadata.get(APPLICABLE_STOP_TIME) + "])";
    }

    /**
     * @return Returns the filename.
     */
    public String getFilename() {
        return this.filename;
    }

    public double getDut(final ISclk iSclk) {

        if (iSclk == null) {
            throw new IllegalArgumentException("Null input SCLK");
        }

        int ind = 0;
        double dut = 0.0;

        if ((table == null) || (table.length == 0)) {
            return (dut);
        }

        // this method assume no sclk resets
        // if sclk resets can occur must use ert to
        // find appropriate entry

        //find the table entry just past the input time
        for (ind = 0; ind < table.length; ++ind) {
            if (iSclk.compareTo(table[ind].getSclk()) < 0) {
                break;
            }
        }

        //At this point, "ind" is pointing to the entry that is directly past where the
        //SCLK falls in the table...meaning the input SCLK is between table[ind-1] and table[ind]

        //input SCLK is before the first table entry
        if (ind == 0) {
            dut = table[0].getDup();
        }
        //input SCLK is past the last table entry or between two table entries
        else {
            dut = table[ind - 1].getDup();
        }

        return dut;
    }


    /**
     * Compute modified SCET by adding offset.
     *
     * WRONG! msecsOffset should not be a double.
     *
     * NB: Note that we round only once, which takes care of any
     * fractional nanoseconds. Once we have the whole thing as
     * an integer count of nanoseconds we can split the parts out
     * without fear of any more rounding.
     *
     * NB: Remember that IAccurateDateTime nanoseconds are NOT a
     * full count of nanoseconds; they do not hold the milliseconds.
     *
     * @param scet        Current SCET
     * @param msecsOffset Fractional offset in milliseconds
     *
     * @return Modified SCET
     *
     */
    private static IAccurateDateTime interpolatedScet(final IAccurateDateTime scet,
                                                     final double           msecsOffset)
    {
        if (! SCET_PRECISION_EXTENDED || ! USE_EXTENDED_SCET)
        {
            // WRONG! Cannot fit in double!
            return new AccurateDateTime(StrictMath.round(scet.getTime() + msecsOffset));
        }

        // Compute result as an integer in nanoseconds, then split without more rounding.

        // Form SCET as a count in nanoseconds.
        final BigInteger bigScet =
            BigInteger.valueOf(scet.getTime()).multiply(MILLION_I).add(BigInteger.valueOf(scet.getNanoseconds()));

        // Form offset as fractional nanoseconds.
        final BigDecimal bigOffset = BigDecimal.valueOf(msecsOffset).multiply(MILLION_D, CONTEXT);

        // The new SCET is the sum.
        final BigDecimal bigResult = bigOffset.add(new BigDecimal(bigScet), CONTEXT);

        // Extract as an integer count of nanoseconds, rounding.
        final BigInteger bigNanoseconds = bigResult.add(HALF, CONTEXT).toBigInteger();

        final BigInteger msecs = bigNanoseconds.divide(MILLION_I);
        final BigInteger nanos = bigNanoseconds.mod(MILLION_I);

        return new AccurateDateTime(msecs.longValue(), nanos.longValue());
    }


    /**
     * This class represents a single correlation entry in the stored SCLK/SCET correlation table.
     *
     *
     */
    private class SclkScetEntry implements Serializable {
        /** The SCLK time */
        private ISclk               sclk;

        /** The SCET time */
        private IAccurateDateTime    scet;

        /**
         * The Delta-UT value. A value in atomic seconds usually formatted ss.sss. Delta UT is the difference between UTC and
         * Ephemeris Time (ET) at SCET0.
         */
        private double                dut;

        /**
         * The SCLK rate in seconds per sclk tick
         */
        private double                sclkRate;

        /**
         *
         * Creates an instance of SclkScetEntry.
         */
        public SclkScetEntry() {
            this.sclk = null;
            this.scet = null;
            this.dut = 0.0;
            this.sclkRate = 0.0;
        }

        /**
         *
         * Creates an instance of SclkScetEntry.
         *
         * @param sclk
         *            The SCLK value
         * @param scet
         *            The SCET value
         * @param dup
         *            The dup value
         * @param sclkRate
         *            The SCLK rate value
         */
        @SuppressWarnings("unused")
        public SclkScetEntry(final ISclk sclk, final IAccurateDateTime scet, final double dup, final double sclkRate) {
            this();

            this.sclk = sclk;
            this.scet = scet;
            this.dut = dup;
            this.sclkRate = sclkRate;
        }

        /**
         * Accessor for the dup
         *
         * @return Returns the dup.
         */
        public double getDup() {
            return this.dut;
        }

        /**
         * Mutator for the dup
         *
         * @param dup
         *            The dup to set.
         */
        public void setDut(final double dup) {
            this.dut = dup;
        }

        /**
         * Accessor for the SCET
         *
         * @return Returns the scet.
         */
        public IAccurateDateTime getScet() {
            return this.scet;
        }

        /**
         * Mutator for the scet
         *
         * @param scet
         *            The scet to set.
         */
        public void setScet(final IAccurateDateTime scet) {
            this.scet = scet;
        }

        /**
         * Accessor for the SCLK
         *
         * @return Returns the sclk.
         */
        public ISclk getSclk() {
            return this.sclk;
        }

        /**
         * Mutator for the sclk
         *
         * @param sclk
         *            The sclk to set.
         */
        public void setSclk(final ISclk sclk) {
            this.sclk = sclk;
        }

        /**
         * Accessor for the SCLK rate
         *
         * @return Returns the sclkRate.
         */
        public double getSclkRate() {
            return this.sclkRate;
        }

        /**
         * Mutator for the sclkRate
         *
         * @param sclkRate
         *            The sclkRate to set.
         */
        public void setSclkRate(final double sclkRate) {
            this.sclkRate = sclkRate;
        }
    }
}
