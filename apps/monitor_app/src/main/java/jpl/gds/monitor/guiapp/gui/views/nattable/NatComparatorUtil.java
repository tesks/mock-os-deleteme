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
package jpl.gds.monitor.guiapp.gui.views.nattable;

import java.util.Comparator;

/**
 * A static utility class for creating custom comparators for NAT table
 * columns.
 */
public class NatComparatorUtil {
    /**
     * Creates a custom comparator for long values. Used when
     * sorting by long column.
     * 
     * @return Comparator<Long>
     */
    public static Comparator<Long> getCustomLongComparator() {
        return new Comparator<Long>() {
            @Override
            public int compare(Long o1, Long o2) {
                return o1.compareTo(o2);
            }
        };
    }

    /**
     * Creates a custom comparator for double values. Used when
     * sorting by double column.
     * 
     * @return Comparator<Double>
     */
    public static Comparator<Double> getCustomDoubleComparator() {
        return new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                return o1.compareTo(o2);
            }
        };
    }


    /**
     * Creates a custom comparator for string values.  Used when
     * sorting by string column.
     * 
     * @return Comparator<String>
     */
    public static Comparator<String> getCustomStringComparator() {
        return new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareToIgnoreCase(o2);
            }
        };
    }

}
