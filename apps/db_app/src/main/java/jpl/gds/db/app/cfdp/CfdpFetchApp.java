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
package jpl.gds.db.app.cfdp;

import static jpl.gds.shared.cli.options.ICommandLineOption.COMMAND_LINE_ERROR;

import java.io.PrintWriter;
import java.util.Arrays;

import org.apache.commons.cli.ParseException;

import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.cmdline.OptionSet;

/**
 * The CfdpFetchApp is a class that handles loading and executing the appropriate
 * CFDP fetch class.
 */
public class CfdpFetchApp extends AbstractCommandLineApp {

	public enum AvailableCommandsForHelp {
		INDICATION,
		FILEGEN,
		FILEUPLINK,
		REQRECV,
		REQRESULT,
		PDURECV,
		PDUSENT;

		@Override
		public String toString() {
			return super.toString().toLowerCase();
		}
	}

	public static void main(final String[] args) {

		try {

			if (args.length == 0) {
				CfdpFetchApp app = new CfdpFetchApp();
				app.configure(app.createOptions().parseCommandLine(new String[] { "--help" }, true));
			}

			// From the user-supplied command, get the proper sub fetch app to run

			ACfdpSubFetchApp subApp = null;

			try{
				AvailableCommandsForHelp command = AvailableCommandsForHelp.valueOf(args[0].trim().toUpperCase());
				subApp = getSubFetchAppFromCommand(command);
			} catch (IllegalArgumentException e) {
				//do nothing, this will be exiting after showing the user the appropriate info
			}

			if (subApp == null) {

				/*
				 * If due to incorrect subcommand, point it out. */
				if (!args[0].trim().startsWith("-")) {
					System.err.print("'" + args[0].trim() + "' is not a recognized subcommand");
				} else {
					CfdpFetchApp app = new CfdpFetchApp();
					app.configure(app.createOptions().parseCommandLine(args, true));

					// If reached this point, the arguments are invalid
					System.err.print("Invalid arguments:");
					String delim = " ";

					for (String a : args) {
						System.err.print(delim);
						System.err.print(a);
					}

				}

				System.err.println();
				System.exit(COMMAND_LINE_ERROR);

			} else {
				subApp.runMain(Arrays.copyOfRange(args, 1, args.length));
			}

		} catch (ParseException pe) {
			System.err.println(pe.getMessage());
			System.exit(COMMAND_LINE_ERROR);
		}

	}

	private static ACfdpSubFetchApp getSubFetchAppFromCommand(AvailableCommandsForHelp command) {

		if(command != null) {

			switch(command) {
				case INDICATION:
					return new CfdpIndicationFetchApp();
				case FILEGEN:
					return new CfdpFileGenerationFetchApp();
				case FILEUPLINK:
					return new CfdpFileUplinkFinishedFetchApp();
				case REQRECV:
					return new CfdpRequestReceivedFetchApp();
				case REQRESULT:
					return new CfdpRequestResultFetchApp();
				case PDURECV:
					return new CfdpPduReceivedFetchApp();
				case PDUSENT:
					return new CfdpPduSentFetchApp();
				default:
			}
		}

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
		pw.println("commands:");

		for (AvailableCommandsForHelp command : AvailableCommandsForHelp.values()) {
			pw.print("   ");
			pw.println(command.toString());
		}
		pw.println();
		pw.println("options:");
		options.printOptions(pw);
		pw.flush();
	}

}
