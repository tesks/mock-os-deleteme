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
package jpl.gds.shared.io;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

/**
 * Utility class to handle indenting. Everything is static for an unknown
 * reason.
 *
 */
public class Indent
{
    private static int level = 0;
    private static final int INDENT = 4;

    private static List<String> indentLookup = new LinkedList<>();
    private static StringBuilder builder = new StringBuilder(10 * INDENT);

    private static final String INDENT_STRING;


    static {

        StringBuilder temp = new StringBuilder();
        for(int x = 0;x < INDENT;x++) {
            temp.append(" ");
        }
        INDENT_STRING = temp.toString();

        indentLookup.add("");
        for(int i = 0;i < 10;i++) {
            addIndentLevel();
        }
    }

    private static void addIndentLevel() {
        builder.append(INDENT_STRING);
        indentLookup.add(builder.toString());
    }

    /**
     * Perform indenting.
     *
     * @param out PrintStream
     */
    static public void print(final PrintStream out) {
        while(indentLookup.size() <= level) {
            addIndentLevel();
        }
        out.print(indentLookup.get(level));
    }


    /**
     * Perform indenting.
     *
     * @param out PrintWriter
     */
    static public void print(final PrintWriter out) {
        while(indentLookup.size() <= level) {
            addIndentLevel();
        }
        out.print(indentLookup.get(level));
    }

    /**
     * Increment level..
     */
    static public void incr() {
        level++;
    }

    /**
     * Decrement level..
     */
    static public void decr() {
        level = Math.max(0, --level);
    }


    /**
     * Reset level to zero.
     */
    static public void reset() {
        level = 0;
    }
}
