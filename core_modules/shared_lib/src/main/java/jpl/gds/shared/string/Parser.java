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

import java.util.StringTokenizer;


/**
 * Public Class Parser                                        
 *                                                           
 * Take a string and parse it into an array of strings using
 * the given delimiters.                                   
 *                                                         
 */

public class Parser
{
    private String[] parsedString = null;


    /**
     * Constructor.
     */
    public Parser()
    {
    }

    /**
     * Get parsed strings.
     *
     * @param stringToParse Initial string
     * @param delimiters    Demarcating characters
     *  
     * @return Array of parsed strings
     */
    public String[] getParsedStrings(String stringToParse, String delimiters) {

        StringTokenizer st = new StringTokenizer(stringToParse, delimiters);
        int numTokens = st.countTokens();
        parsedString = new String[numTokens];
        for(int x=0; x<numTokens; x++) {
            parsedString[x] = st.nextToken();
            parsedString[x] = parsedString[x].trim();
        }

        return parsedString;
    }


    /**
     * Test.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {

        Parser p = new Parser();
        String[] ps = p.getParsedStrings("this,is,  the    , string     ,    to  ,  parse.",",");
        System.out.println("parsed strings");
        for(int x=0; x<ps.length; x++)
              System.out.println(ps[x]);
    }
}
