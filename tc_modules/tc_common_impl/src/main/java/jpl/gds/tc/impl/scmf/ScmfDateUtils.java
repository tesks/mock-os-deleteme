/*
 * Copyright 2006-2019. California Institute of Technology.
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

package jpl.gds.tc.impl.scmf;

import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.tc.api.config.ScmfProperties;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SCMF Date utility class
 *
 */
public class ScmfDateUtils {

    private static final DateTimeFormatter SCMF_DATE_FORMATTER = DateTimeFormatter.ofPattern("yy-DDD/HH:mm:ss.SSS");
    private static final Tracer            log                 = TraceManager.getTracer(Loggers.UPLINK);

    private ScmfDateUtils() {}

    /**
     * Convert a String that is a date/time value in the internal SCMF format to a LocalDateTime equivalent numeric
     * value
     *
     * @param dateStr a String that is a date/time value in the internal SCMF format
     * @return the long value from the equivalent LocalDateTime value
     */
    public static long parseScmfDate(final String dateStr) {
        if (dateStr == null) {
            throw new IllegalArgumentException("The input date string cannot be null");
        }
        final Pattern pattern = Pattern.compile("\\d{2}-\\d{3}/\\d{2}:\\d{2}:\\d{2}\\.\\d{3}");
        final Matcher matcher = pattern.matcher(dateStr);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "The SCMF date string does not match the format \"YY-DDD/HH:MM:SS.fff\"");
        }

        final LocalDateTime date = LocalDateTime.parse(dateStr, SCMF_DATE_FORMATTER);

        return date.toEpochSecond(ZoneOffset.UTC);
    }

    /**
     * Convert a LocalDateTime compatible long value to an internal Scmf formatted String of the date/time
     *
     * @param dateDouble a LocalDateTime compatible long value
     * @return the internal Scmf formatted String of the date/time
     */
    public static String toScmfDate(final long dateDouble) {
        if (dateDouble == -1.0) {
            return "";
        }
        final LocalDateTime ldt = LocalDateTime.ofEpochSecond(dateDouble, 0, ZoneOffset.UTC);
        return ScmfDateUtils.SCMF_DATE_FORMATTER.format(ldt);
    }

    /**
     * Get the transmission start time
     *
     * @param scmfProperties SCMF properties
     * @return -1 if unset, else time in seconds since epoch
     */
    public static long getTransmissionStartTime(final ScmfProperties scmfProperties) {
        return parseAndGetTransmissionTime("transmission start time", scmfProperties.getTransmissionStartTime());
    }

    /**
     * Get the transmission window open time
     *
     * @param scmfProperties SCMF properties
     * @return -1 if unset, else time in seconds since epoch
     */
    public static long getTransmissionWindowOpenTime(final ScmfProperties scmfProperties) {
        return parseAndGetTransmissionTime("transmission window open time", scmfProperties.getOpenWindow());
    }

    /**
     * Get the transmission window close time
     *
     * @param scmfProperties SCMF properties
     * @return -1 if unset, else time in seconds since epoch
     */
    public static long getTransmissionWindowCloseTime(final ScmfProperties scmfProperties) {
        return parseAndGetTransmissionTime("transmission window close time", scmfProperties.getCloseWindow());
    }

    private static long parseAndGetTransmissionTime(final String transmissionType, final String transmissionTime) {
        if (!transmissionTime.isEmpty()) {
            try {
                return parseScmfDate(transmissionTime);
            } catch (final IllegalArgumentException e) {
                log.warn("SCMF configuration value for " + transmissionType + " is invalid. Using untimed default. ",
                        e.getMessage());
            }
        }
        return -1;
    }
}
