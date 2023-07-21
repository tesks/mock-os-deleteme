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
package jpl.gds.db.api.sql.fetch;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.ParseException;

/**
 * Keeps track of desired alarm states and implements useful methods.
 *
 */
public class AlarmControl extends Object implements IAlarmControl {
    private static final String  RED       = "RED";
    private static final String  YELLOW    = "YELLOW";
    private static final String  DN_COLUMN = "dnAlarmState";
    private static final String  EU_COLUMN = "euAlarmState";

    private final Set<AlarmType> _alarms   = new HashSet<AlarmType>(4);

    /**
     * Possible alarm states
     */
    public enum AlarmType {
        /** DN red alarm */
        DN_RED,

        /** DN yellow alarm */
        DN_YELLOW,

        /** EU red alarm */
        EU_RED,

        /** EU yellow alarm */
        EU_YELLOW;
    }

    /**
     * Constructor AlarmControl.
     */
    public AlarmControl() {
        super();
    }

    /**
     * Add an alarm to the set.
     *
     * @param alarm
     *            Type of alarm
     */
    public void addAlarm(final AlarmType alarm) {
        if (alarm != null) {
            _alarms.add(alarm);
        }
    }

    /**
     * Process option arguments.
     *
     * @param alarmsStr
     *            Option value
     * @param option
     *            Option name
     *
     * @throws MissingOptionException
     *             Missing option exception
     * @throws ParseException
     *             Parse exception
     */
    public void processOptions(final String alarmsStr, final String option)
            throws MissingOptionException, ParseException {
        final String s = (alarmsStr != null) ? alarmsStr.trim() : "";

        if (s.length() == 0) {
            throw new MissingOptionException("The argument -" + option + " requires a value");
        }

        for (final String next : s.split(",", -1)) {
            final String value = next.trim();
            final String test = value.toUpperCase();

            if (test.equals("ANY")) {
                addAlarm(AlarmType.DN_RED);
                addAlarm(AlarmType.DN_YELLOW);
                addAlarm(AlarmType.EU_RED);
                addAlarm(AlarmType.EU_YELLOW);
            } else if (test.equals("DN") || test.equals("DN-RY") || test.equals("DN-YR")) {
                addAlarm(AlarmType.DN_RED);
                addAlarm(AlarmType.DN_YELLOW);
            } else if (test.equals("DN-R")) {
                addAlarm(AlarmType.DN_RED);
            } else if (test.equals("DN-Y")) {
                addAlarm(AlarmType.DN_YELLOW);
            } else if (test.equals("EU") || test.equals("EU-RY") || test.equals("EU-YR")) {
                addAlarm(AlarmType.EU_RED);
                addAlarm(AlarmType.EU_YELLOW);
            } else if (test.equals("EU-R")) {
                addAlarm(AlarmType.EU_RED);
            } else if (test.equals("EU-Y")) {
                addAlarm(AlarmType.EU_YELLOW);
            } else if (test.equals("RED") || test.equals("R")) {
                addAlarm(AlarmType.DN_RED);
                addAlarm(AlarmType.EU_RED);
            } else if (test.equals("YELLOW") || test.equals("Y")) {
                addAlarm(AlarmType.DN_YELLOW);
                addAlarm(AlarmType.EU_YELLOW);
            } else {
                throw new ParseException("Alarm is not valid: '" + value + "'");
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return _alarms.isEmpty();
    }

    /**
     * Append where clause for single alarm.
     *
     * @param sb
     * @param tableAbbrev
     * @param column
     * @param like
     */
    private static void addWhere(final StringBuilder sb, final String tableAbbrev, final String column,
            final String like) {
        sb.append("(");
        sb.append(tableAbbrev).append(".").append(column);
        sb.append(" LIKE '%").append(like).append("%'");
        sb.append(")");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String whereClause(final String tableAbbrev) {
        if (isEmpty()) {
            return "";
        }

        final StringBuilder sb = new StringBuilder("(");
        boolean first = true;

        if (haveAlarm(AlarmType.DN_RED)) {
            first = false;

            addWhere(sb, tableAbbrev, DN_COLUMN, RED);
        }

        if (haveAlarm(AlarmType.DN_YELLOW)) {
            if (first) {
                first = false;
            } else {
                sb.append(" OR ");
            }

            addWhere(sb, tableAbbrev, DN_COLUMN, YELLOW);
        }

        if (haveAlarm(AlarmType.EU_RED)) {
            if (first) {
                first = false;
            } else {
                sb.append(" OR ");
            }

            addWhere(sb, tableAbbrev, EU_COLUMN, RED);
        }

        if (haveAlarm(AlarmType.EU_YELLOW)) {
            if (first) {
                first = false;
            } else {
                sb.append(" OR ");
            }

            addWhere(sb, tableAbbrev, EU_COLUMN, YELLOW);
        }

        sb.append(")");

        return sb.toString();
    }

    /**
     * Check for alarm match from string values.
     *
     * @param dnAlarm
     *            DN alarm
     * @param euAlarm
     *            EU alarm
     *
     * @return True if any match
     */
    public boolean match(final String dnAlarm, final String euAlarm) {
        if (isEmpty()) {
            return false;
        }

        return ((haveAlarm(AlarmType.DN_RED) && contains(dnAlarm, RED))
                || (haveAlarm(AlarmType.DN_YELLOW) && contains(dnAlarm, YELLOW))
                || (haveAlarm(AlarmType.EU_RED) && contains(euAlarm, RED))
                || (haveAlarm(AlarmType.EU_YELLOW) && contains(euAlarm, YELLOW)));
    }

    /**
     * Case-blind "contains" that does not create intermediate string.
     *
     * @param s
     * @param target
     *
     * @return True if s contains target
     */
    private boolean contains(final String s, final String target) {
        if ((s == null) || (target == null)) {
            return false;
        }

        final int slen = s.length();
        final int tlen = target.length();

        if (slen < tlen) {
            return false;
        }

        final int overlap = slen - tlen;

        for (int i = 0; i <= overlap; ++i) {
            boolean found = true;

            for (int j = 0; j < tlen; ++j) {
                if (Character.toUpperCase(s.charAt(i + j)) != Character.toUpperCase(target.charAt(j))) {
                    found = false;
                    break;
                }
            }

            if (found) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check for alarm in set.
     *
     * @param alarm
     *            Type of alarm
     *
     * @return True if contained
     */
    private boolean haveAlarm(final AlarmType alarm) {
        return _alarms.contains(alarm);
    }
}
