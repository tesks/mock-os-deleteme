/*
 * Copyright 2006-2019. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */
package jpl.gds.cfdp.clt;

import jpl.gds.cfdp.clt.action.abandon.AbandonClt;
import jpl.gds.cfdp.clt.action.cancel.CancelClt;
import jpl.gds.cfdp.clt.action.clear.ClearClt;
import jpl.gds.cfdp.clt.action.forcegen.ForceGenClt;
import jpl.gds.cfdp.clt.action.ingest.IngestClt;
import jpl.gds.cfdp.clt.action.pausetimer.PauseTimerClt;
import jpl.gds.cfdp.clt.action.put.PutClt;
import jpl.gds.cfdp.clt.action.report.ReportClt;
import jpl.gds.cfdp.clt.action.resetstat.ResetStatClt;
import jpl.gds.cfdp.clt.action.resume.ResumeClt;
import jpl.gds.cfdp.clt.action.resumetimer.ResumeTimerClt;
import jpl.gds.cfdp.clt.action.savestate.SaveStateClt;
import jpl.gds.cfdp.clt.action.suspend.SuspendClt;
import jpl.gds.cfdp.clt.config.ConfigClt;
import jpl.gds.cfdp.clt.entitymap.EntityMapClt;
import jpl.gds.cfdp.clt.mib.MibClt;
import jpl.gds.cfdp.clt.mtu.MtuMapClt;
import jpl.gds.cfdp.clt.root.RootClt;
import jpl.gds.cfdp.clt.shutdown.ShutdownClt;
import jpl.gds.cfdp.clt.status.StatusClt;
import jpl.gds.cfdp.stat.StatClt;
import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.cmdline.OptionSet;
import jpl.gds.shared.exceptions.ExceptionTools;
import org.apache.commons.cli.ParseException;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static jpl.gds.cfdp.clt.ENonActionCommandType.*;
import static jpl.gds.cfdp.common.action.EActionCommandType.*;
import static jpl.gds.shared.cli.options.ICommandLineOption.COMMAND_LINE_ERROR;

/**
 * {@code CfdpCltApp} is the entry-point class for chill_cfdp app. It serves as a dispatcher, by looking at the
 * user-supplied subcommand and running the appropriate subcommand app accordingly.
 *
 * @since 8.0.1
 */
public class CfdpCltApp extends AbstractCommandLineApp {

    // Using LinkedHashMap allows preserving order when iterated. Useful for controlling 'help' output presentation.
    private final static Map<String, Class> subCommandClasses = new LinkedHashMap<>();

    public static void main(final String[] args) {

        try {

			/*
			MPCS-10654 4/4/19 Make available commands and help listing always sync by using a single map for
			 both
			 */
            subCommandClasses.put(PUT.getCltCommandStr(), PutClt.class);
            subCommandClasses.put(CANCEL.getCltCommandStr(), CancelClt.class);
            subCommandClasses.put(ABANDON.getCltCommandStr(), AbandonClt.class);
            subCommandClasses.put(SUSPEND.getCltCommandStr(), SuspendClt.class);
            subCommandClasses.put(RESUME.getCltCommandStr(), ResumeClt.class);
            subCommandClasses.put(REPORT.getCltCommandStr(), ReportClt.class);
            subCommandClasses.put(RESET_STAT.getCltCommandStr(),
                    ResetStatClt.class);
            subCommandClasses.put(SAVE_STATE.getCltCommandStr(), SaveStateClt.class);
            subCommandClasses.put(FORCE_GEN.getCltCommandStr(), ForceGenClt.class);
            subCommandClasses.put(PAUSE_TIMER.getCltCommandStr(), PauseTimerClt.class);
            subCommandClasses.put(RESUME_TIMER.getCltCommandStr(), ResumeTimerClt.class);
            subCommandClasses.put(INGEST.getCltCommandStr(), IngestClt.class);
            subCommandClasses.put(MIB.getCltCommandStr(), MibClt.class);
            subCommandClasses.put(CONFIG.getCltCommandStr(), ConfigClt.class);
            subCommandClasses.put(STAT.getCltCommandStr(), StatClt.class);
            subCommandClasses.put(ENTITY_MAP.getCltCommandStr(), EntityMapClt.class);
            subCommandClasses.put(MTU_MAP.getCltCommandStr(), MtuMapClt.class);
            subCommandClasses.put(ROOT.getCltCommandStr(), RootClt.class);
            subCommandClasses.put(STATUS.getCltCommandStr(), StatusClt.class);
            subCommandClasses.put(CLEAR.getCltCommandStr(), ClearClt.class);
            subCommandClasses.put(SHUTDOWN.getCltCommandStr(), ShutdownClt.class);

            // Now that the above map is set up, help output will include the commands

            if (args.length == 0) {
                final CfdpCltApp app = new CfdpCltApp();
                app.configure(app.createOptions().parseCommandLine(new String[]{"--help"}, true));
            }

            // From the user-supplied command, get the proper CLT to run

            final String command = args[0].trim().toLowerCase();
            final ACfdpClt clt = getCltFromCommand(command);

            if (clt == null) {

                /* MPCS-11212 - 9/6/2019
                * If due to incorrect subcommand, point it out. */
                if (!command.startsWith("-")) {
                    System.err.print("'" + command + "' is not a recognized subcommand");
                } else {
                    final CfdpCltApp app = new CfdpCltApp();
                    app.configure(app.createOptions().parseCommandLine(args, true));

                    // If reached this point, the arguments are invalid in ways other than expected
                    System.err.print("Invalid arguments:");
                    final String delim = " ";

                    for (final String a : args) {
                        System.err.print(delim);
                        System.err.print(a);
                    }

                }

                System.err.println();
                System.exit(COMMAND_LINE_ERROR);

            } else {
                clt.configure(clt.createOptions().parseCommandLine(Arrays.copyOfRange(args, 1, args.length), true));
                clt.run();
            }

        } catch (final ParseException pe) {
            System.err.println(pe.getMessage());
            System.exit(COMMAND_LINE_ERROR);
        }

    }

    private static ACfdpClt getCltFromCommand(final String command) {
        final Class<?> subCommandClass = subCommandClasses.get(command);


        if (subCommandClass != null) {
            try {
                return (ACfdpClt) subCommandClass.newInstance();
            } catch (final Exception e) {
                System.err.println("Could not instantiate handler for subcommand " + command + ": " + ExceptionTools.getMessage(e));
                return null;
            }

        }

        /*
        Reaching here means that user entered a subcommand that isn't in the list of acceptable subcommands. Return
        null and allow the calling method to inform the user as such.
         */
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#showHelp()
     */
    @Override
    public void showHelp() {

        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        final OptionSet options = createOptions().getOptions();
        final PrintWriter pw = new PrintWriter(System.out);
        pw.println(
                "Usage: " + ApplicationConfiguration.getApplicationName() + " [--help] [--version] <command> [<args>]");
        pw.println();
        pw.println("For help on a specific command, type: '" + ApplicationConfiguration.getApplicationName() + " <command> --help'");
        pw.println("commands:");
        for (final String command : subCommandClasses.keySet()) {
            pw.print("   ");
            pw.println(command);
        }
        pw.println();
        pw.println("options:");
        options.printOptions(pw);
        pw.flush();
    }

}