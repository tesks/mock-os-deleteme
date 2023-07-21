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
package jpl.gds.app.tools.time;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.cli.ParseException;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.StringOption;
import jpl.gds.shared.cli.options.numeric.UnsignedLongOption;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.types.UnsignedLong;

/**
 * TimeMapperApp is a utility to map between string times and numeric (long) 
 * times. If the user supplies a number, it is mapped to string format. If
 * the user supplies a string, it is parsed to a long number that is
 * milliseconds past Jan 1, 1970.
 *
 */
public class TimeMapperApp extends AbstractCommandLineApp {

    /** Success status */
    public static final int SUCCESS = 0;

    /** Invalid argument status */
    public static final int INVALID_ARG = 1;

    /** Failure status */
    public static final int FAILURE = -1;
    
    private String charTimeStr = null;
    private long numTime = -1;
    
    private final StringOption timeStringOption = new StringOption("s", "string", "time-string", 
            "A time string to convert to integer.", false);
    private final UnsignedLongOption numberOption = new UnsignedLongOption("n", "number", "integer", 
            "Integer to convert to time string.", false);

    /**
     * App entry point.
     *
     * @param args Command-line arguments
     */
    public static void main(final String[] args) {
  
        try {
            final TimeMapperApp app = new TimeMapperApp();
            final ICommandLine commandLine = app.createOptions().parseCommandLine(args, true);
                app.configure(commandLine);
            app.run();
        } catch (final ParseException e) {
            TraceManager.getDefaultTracer().error(e.getMessage());
            System.exit(INVALID_ARG);
        } catch (final Exception e) {
            TraceManager.getDefaultTracer().error("Unexpected exception: " + e.toString(),e);
            System.exit(FAILURE);
        }
        System.exit(SUCCESS);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public BaseCommandOptions createOptions() {
        if (optionsCreated.get()) {
            return options;
        }
        
        super.createOptions();
        
        options.getHelpOption().addAlias("m");
        options.getHelpOption().addAlias("man");
        options.addOption(timeStringOption);
        options.addOption(numberOption);
        
        return options;
    }   
    
    private static void mapNumericToString(final long numTime) {
        final Date d = new AccurateDateTime(numTime);
        SimpleDateFormat format = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss'.'SSS");
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        System.out.println("YMD = " + format.format(d));
        format = new SimpleDateFormat("yyyy'-'DDD'T'HH':'mm':'ss'.'SSS"); 
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        System.out.println("DOY = " + format.format(d));
    }
    
    private static void mapStringToNumeric(final String timeStr) {
        try {
            final SimpleDateFormat format = new SimpleDateFormat("yyy'-'MM'-'dd'T'HH':'mm':'ss'.'SSS");
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            System.out.println("Milliseconds = " + format.parse(timeStr).getTime());
        } catch (final java.text.ParseException e) {
            try {
                final SimpleDateFormat format = new SimpleDateFormat("yyyy'-'DDD'T'HH':'mm':'ss'.'SSS");
                format.setTimeZone(TimeZone.getTimeZone("GMT"));
                System.out.println("Milliseconds = " + format.parse(timeStr).getTime());
            } catch (final java.text.ParseException ex) {
                System.err.println("Input time string does not have a recognized format");
                System.exit(FAILURE);
            }
        }
     }

    
    /**
     * Prints utility help text.
     *
     */
    public static void usage() {
        final String result = "Usage:\n"
                + "      "
                + "time_mapper -s <time-string> | -n <numeric-time>\n\n"
                + "Where:\n"
                + "      -s <time-string>  A time as an ASCII string to map to a numeric time.\n"
                + "                        Can be of two forms:\n" 
                + "                             YYYY-DOYThh:mm:ss.ttt or\n" 
                + "                             YYYY-MM-DDThh:mm:ss.ttt\n" 
                + "                        Only one of -s or -n can be specified.\n"
                + "                        Result is in milliseconds past Jan 1, 1970 00:00:00.\n" 
                + "      -n <numeric-time> A numeric time as milliseconds past Jan 1, 1970.\n" 
                + "                        Result will be two ASCII time strings, one in\n" 
                + "                        year-month-day format and the other in year-day-of-year\n"
                + "                        format. Only one of -s or -n can be specified.\n"
                + "\n Other flags are:\n"
                + "        -h              Display this help message.\n"
                + "        -m              Display man page.\n"
                + "        -v              Display the version number of this application.\n";
         System.out.println(result);
    }
    
    /**
     * Parses the command line arguments, set appropriate flags, and creates the
     * shared data class that subscribes to message service messages.
     * 
     * @param commandLine a CommandLine object initialized using the current command line
     * arguments.
     * @throws ParseException if there is a command line parsing error

     */
    @Override
    @SuppressWarnings({"DM_EXIT"})
    public void configure(final ICommandLine commandLine) throws ParseException {
        
        super.configure(commandLine);
           
        final UnsignedLong time = numberOption.parse(commandLine);
        charTimeStr = timeStringOption.parse(commandLine);
        
        if (time != null) {
            numTime = time.longValue();
        }
    
        if (time == null && charTimeStr == null) {
            throw new ParseException("You must specify either a numeric time (-n) or ASCII time string (-s)");
        }
        
        if (time != null && charTimeStr != null) {
            throw new ParseException("You cannot specify both the -n and -s options together");
        }
    }


    /**
     * Run application.
     */    
    public void run() {
        
        if (numTime != -1) {
            mapNumericToString(numTime);
        } else {
            mapStringToNumeric(charTimeStr);
        }
    }
}
