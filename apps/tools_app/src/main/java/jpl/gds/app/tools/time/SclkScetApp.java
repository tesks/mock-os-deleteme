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

import java.io.File;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Date;

import org.apache.commons.cli.ParseException;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.options.SpacecraftIdOption;
import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.cli.options.time.AccurateDateTimeFormatOption;
import jpl.gds.shared.cli.options.time.SclkFineFormatOption;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.DateTimeFormat;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.SclkFineFormat;
import jpl.gds.shared.time.SclkFmt.SclkFormatter;
import jpl.gds.shared.time.SclkScetConverter;
import jpl.gds.shared.time.SclkScetUtility;
import jpl.gds.shared.time.TimeProperties;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.shared.types.UnsignedInteger;

/**
 * This application class will perform a SCLK to SCET or SCET to SCLK conversion
 * based upon command line arguments.
 */
public class SclkScetApp extends AbstractCommandLineApp {
	private static final String FILE_SHORT_VALUE = "f";
	private static final String FILE_LONG_VALUE = "sclkScetFile";

	private final Tracer trace;
	private int scid;
	private File conversionFile;
	private String inputValue;
	private SclkScetConverter converter;
	private SclkFineFormat sclkFineFormat;
	private DateTimeFormat scetFormat;
	
	private final SclkFormatter sclkFmt;
	
	private final AccurateDateTimeFormatOption scetFormatOption;
	private final SclkFineFormatOption sclkFormatOption;
	private final FileOption sclkScetFileOption;

	/**
	 * Constructor.
	 */
	public SclkScetApp() {

		final TimeProperties tc = new TimeProperties();
		sclkFmt = tc.getSclkFormatter();
        trace = TraceManager.getDefaultTracer();
		
		scetFormatOption = new AccurateDateTimeFormatOption(tc, false, "SCET");
		sclkFormatOption = new SclkFineFormatOption(tc, false);
		sclkScetFileOption = new FileOption(FILE_SHORT_VALUE, FILE_LONG_VALUE, "file", "SCLK/SCET conversion file to use", false, true);

		this.scid = MissionProperties.UNKNOWN_ID;
		this.conversionFile = null;
		this.inputValue = null;
		this.converter = null;
		this.sclkFineFormat = SclkFineFormat.SUBSECONDS;
		this.scetFormat = DateTimeFormat.DOY;
	}

	@Override
    @SuppressWarnings("DM_EXIT")
	public void configure(final ICommandLine commandLine) throws ParseException {
	    
	    super.configure(commandLine);

	    final BaseCommandOptions options = createOptions();
	    
	    scid = ((UnsignedInteger) options.getOption(SpacecraftIdOption.LONG_OPTION).parseWithDefault(commandLine, false, true)).intValue();

		final String fileValue = sclkScetFileOption.parse(commandLine);
		
		if (fileValue != null) {
			this.conversionFile = new File(fileValue.trim());
		}

		this.scetFormat = scetFormatOption.parseWithDefault(commandLine, false, true);
		this.sclkFineFormat = sclkFormatOption.parseWithDefault(commandLine, false, true);

	
		if (this.conversionFile == null) {
			this.converter = SclkScetUtility
					.getConverterFromSpacecraftId(this.scid);
		} else {
			this.converter = SclkScetUtility.getConverterFromFile(this.scid,
					this.conversionFile.getAbsolutePath());
		}

		if (this.converter == null) {
			throw new ParseException(
					"Could not interpret SCLK/SCET conversion file. No conversions will be done.");
		}

        trace.info(Markers.TIME_CORR, "Parsed SCLK/SCET correlation file from "
				+ this.converter.getFilename());

		final String[] leftoverArgs = commandLine.getTrailingArguments();
		if (leftoverArgs.length == 0) {
			throw new ParseException(
					"You must supply an input time on the command line.");
		} else if (leftoverArgs.length > 1) {
			throw new ParseException(
					"You can only supply one input time on the command line.");
		}

		this.inputValue = leftoverArgs[0];
	}

	/**
	 * Perform the main application logic.
	 */
	private void convert() {

		try {
			runAsCommandLine();
		} catch (final Exception e) {
            trace.error(Markers.TIME_CORR, "Conversion failed: " + e.getMessage());
		}

	}

	/**
	 * Performs SCLK to SCET conversion and display.
	 * @return SCET string
	 */
	String displayScetFromSclk() {

        final ISclk sclk = sclkFmt.valueOf(this.inputValue);
		final IAccurateDateTime scet = this.converter.to_scet(sclk, new AccurateDateTime());
		final Date now = new Date(System.currentTimeMillis());

		String nowString = "";
		String scetString = "";
		DateFormat df = null;
		// Use formatter from AccurateDateTime
		if (this.scetFormat.equals(DateTimeFormat.ISO)) {
			df = TimeUtility.getISOFormatterFromPool();
			nowString = df.format(now);
			scetString = scet.getFormattedScet(false);
			TimeUtility.releaseISOFormatterToPool(df);
		} else if (this.scetFormat.equals(DateTimeFormat.DOY)) {
			df = TimeUtility.getDoyFormatterFromPool();
			nowString = df.format(now);
			scetString = scet.getFormattedScet(false);
			TimeUtility.releaseDoyFormatterToPool(df);
		}

		System.out.println("NOW  = " + nowString);
		System.out.println("SCET = " + scetString);

		return scetString;
	}

	/**
	 * Performs SCET to SCLK conversion and display.
	 * @return SCLK string
	 */
	String displaySclkFromScet() throws java.text.ParseException {

		final IAccurateDateTime scet = new AccurateDateTime(this.inputValue);
		final ISclk sclk = this.converter.to_sclk(scet, new AccurateDateTime());

		String sclkString = "";
		if (this.sclkFineFormat.equals(SclkFineFormat.TICKS)) {
			sclkString = sclkFmt.toTicksString(sclk);
		} else if (this.sclkFineFormat.equals(SclkFineFormat.SUBSECONDS)) {
			sclkString = sclkFmt.toDecimalString(sclk);
		}

		System.out.println("SCLK = " + sclkString);

		return sclkString;
	}

	/**
	 * Perform the actual time conversion and display.
	 * 
	 * @throws java.text.ParseException
	 */
	private void runAsCommandLine() throws java.text.ParseException {

		if (sclkFmt.matches(this.inputValue)) {
			displayScetFromSclk();
		} else {
			displaySclkFromScet();
		}
	}

	/**
	 * Main application method.
	 * 
	 * @param args
	 *            command line arguments.
	 */
	public static void main(final String[] args) {

		final SclkScetApp app = new SclkScetApp();
		try {
			app.configure(app.createOptions().parseCommandLine(args, true));
		} catch (final ParseException e) {
            TraceManager.getDefaultTracer().error("Command line parsing error: " + e.getMessage());
			return;
		}
		app.convert();
	}

	/**
	 * Creates an Options object containing possible command line
	 * arguments/options.
	 * 
	 * @return the Options object
	 */
	@Override
    public BaseCommandOptions createOptions() {
	    
	    if (optionsCreated.get()) {
            return options;
        }
        

		super.createOptions();
		
		options.addOption(new SpacecraftIdOption(new MissionProperties(), false));
		
		options.addOption(scetFormatOption);
		scetFormatOption.setDescription(scetFormatOption.getDescription() + 
		        ". All SCET formats will be accepted as input regardless" +
                " of the value of this option");
        
		options.addOption(sclkFormatOption);
        sclkFormatOption.setDescription(sclkFormatOption.getDescription() +
                ". All SCLK formats will be accepted as input regardless" +
                " of the value of this option");
        
        options.addOption(sclkScetFileOption);
        
		return options;
	}

	/**
	 * Get the usage for this application.
	 * 
	 */
	@Override
    public void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

	    final PrintWriter pw = new PrintWriter(System.out);

	    pw.println("Usage: " + ApplicationConfiguration.getApplicationName() + " [options] <input_time>\n\n");

	    createOptions().getOptions().printOptions(pw);
	        
		pw.println("\n<input_time> is a SCLK or SCET time to be converted.");
		pw.println("A SCET time should be input in ISO (YYYY-MM-DDTHH:mm:ss.SSS) or DOY (YYYY-dddTHH:mm:ss.SSS) format with or without milliseconds. ");
		pw.println("A SCLK time should be input using either coarse-subticks (CCCCCCCCCC-FFFFF) or coarse.subsecs (CCCCCCCCCC.SSSSS).");

		pw.flush();
	}

}