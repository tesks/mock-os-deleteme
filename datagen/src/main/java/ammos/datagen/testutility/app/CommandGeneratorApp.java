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
package ammos.datagen.testutility.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Random;

import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;

import ammos.datagen.cmdline.DatagenOptions;
import ammos.datagen.config.TraversalType;
import ammos.datagen.generators.StringGenerator;
import ammos.datagen.generators.seeds.StringGeneratorSeed;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.command.CommandArgumentType;
import jpl.gds.dictionary.api.command.CommandEnumerationDefinition;
import jpl.gds.dictionary.api.command.ICommandArgumentDefinition;
import jpl.gds.dictionary.api.command.ICommandDefinition;
import jpl.gds.dictionary.api.command.ICommandDictionary;
import jpl.gds.dictionary.api.command.ICommandDictionaryFactory;
import jpl.gds.dictionary.api.command.ICommandEnumerationValue;
import jpl.gds.dictionary.api.command.IRepeatCommandArgumentDefinition;
import jpl.gds.dictionary.api.command.IValidationRange;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.cmdline.OptionSet;
import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.sys.SystemUtilities;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.Sclk;
import jpl.gds.shared.time.SclkFmt.SclkFormatter;
import jpl.gds.shared.time.TimeProperties;
import nl.flotsam.xeger.Xeger;

/**
 * This is an application that reads an command dictionary for the current AMPCS
 * mission and writes an ASCII file of valid command/argument permutations,
 * based on acceptance criteria in MM Command Dictionary Support implementation
 * plan.
 * 
 * This implementation is based on CommandDictionaryConverterApp.java class
 * located in ammos.datagen.dictionary.app
 * 
 * Initial Implementation
 * Cleanup and expansion to cover more cases (MPCS-6303)
 * MPCS-7750 - 10/23/15. Changed to use new BaseCommandOptions
 *          and new command line option strategy throughout.
 * 
 *
 */
public class CommandGeneratorApp extends AbstractCommandLineApp {

    /**
     * Modified character set for command strings. Note that comma and semicolon
     * are excluded because they complicate test result comparison with
     * chill_get_commands.
     */
    public static final String commandCharacterSet = new String(new byte[] {
            (byte) 0x20, (byte) 0x21, (byte) 0x23, (byte) 0x24, (byte) 0x25,
            (byte) 0x26, (byte) 0x27, (byte) 0x28, (byte) 0x29, (byte) 0x2A,
            (byte) 0x2B, (byte) 0x2E, (byte) 0x2F, (byte) 0x30, (byte) 0x31,
            (byte) 0x32, (byte) 0x33, (byte) 0x34, (byte) 0x35, (byte) 0x36,
            (byte) 0x37, (byte) 0x38, (byte) 0x39, (byte) 0x3A, (byte) 0x3C,
            (byte) 0x3E, (byte) 0x3F, (byte) 0x40, (byte) 0x41, (byte) 0x42,
            (byte) 0x43, (byte) 0x44, (byte) 0x45, (byte) 0x46, (byte) 0x47,
            (byte) 0x48, (byte) 0x49, (byte) 0x4A, (byte) 0x4B, (byte) 0x4C,
            (byte) 0x4D, (byte) 0x4E, (byte) 0x4F, (byte) 0x50, (byte) 0x51,
            (byte) 0x52, (byte) 0x53, (byte) 0x54, (byte) 0x55, (byte) 0x56,
            (byte) 0x57, (byte) 0x58, (byte) 0x59, (byte) 0x5A, (byte) 0x5B,
            (byte) 0x5C, (byte) 0x5E, (byte) 0x5F, (byte) 0x60, (byte) 0x61,
            (byte) 0x62, (byte) 0x63, (byte) 0x64, (byte) 0x65, (byte) 0x66,
            (byte) 0x67, (byte) 0x68, (byte) 0x69, (byte) 0x6A, (byte) 0x6B,
            (byte) 0x6C, (byte) 0x6E, (byte) 0x6F, (byte) 0x70, (byte) 0x71,
            (byte) 0x72, (byte) 0x73, (byte) 0x74, (byte) 0x75, (byte) 0x76,
            (byte) 0x77, (byte) 0x78, (byte) 0x79, (byte) 0x7A, (byte) 0x7B,
            (byte) 0x7C, (byte) 0x7E });

    private ICommandDictionary missionDictionary;
    private String outputPath;
    private String dictionaryPath;
    private final Random random = new Random();
    private final SclkFormatter sclkFmt = TimeProperties.getInstance().getSclkFormatter();

    private final ApplicationContext appContext;
    private final SseContextFlag     sseFlag;

    /**
     * Constructor.
     */
    public CommandGeneratorApp() {
        this.appContext = SpringContextFactory.getSpringContext(true);
        sseFlag = appContext.getBean(SseContextFlag.class);
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.cli.app.cmd.AbstractCommandLineApp#createOptions()
     */
    @Override
    public DatagenOptions createOptions() {

        final DatagenOptions options = new DatagenOptions(this);
        options.addOption(DatagenOptions.DICTIONARY);
        options.addOption(DatagenOptions.OUTPUT_FILE);
        /* 10/6/14 - MPCS-6698. Added source schema option. */
        options.addOption(DatagenOptions.SOURCE_SCHEMA);
        return options;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#configure(jpl.gds.shared.cli.cmdline.ICommandLine)
     */
    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {

        super.configure(commandLine);

        this.dictionaryPath = DatagenOptions.DICTIONARY
                .parse(commandLine, true);
        this.outputPath = DatagenOptions.OUTPUT_FILE.parse(commandLine, true);

        /* 10/6/14 - MPCS-6698. Added source schema option */
        /* 9/15/15 - MPCS-7679. Added 'mm' as default */
        String source = DatagenOptions.SOURCE_SCHEMA.parse(commandLine, false);
        if (source == null) {
            source = "mm";
        }
        GdsSystemProperties.setSystemMission(source);
        validateSourceSchema(source);
    }

    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#showHelp()
     *
     * 9/18/14 - MPCS-6641. Overrode method.
     */
    @Override
    public void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        final OptionSet options = createOptions().getOptions();

        final PrintWriter pw = new PrintWriter(System.out);
        pw.println("Usage: "
                + ApplicationConfiguration.getApplicationName()
                + " --dictionary <file path> --outputFile <file path> [--sourceSchema <schema-name>]");
        pw.println("                   ");

        options.printOptions(pw);
        pw.println("This is a test data generator application that will create an ascii file of");
        pw.println("commands.  It requires an input command dictionary and a name of an output");
        pw.println("command file. The source schema defaults to 'mm' (for multimission).  If the");
        pw.println("command dictionary is not in multimission format, the source schema must be");
        pw.println("specified using the command line option. The source schema option ");
        pw.println("may specify one of 'mm', 'smap', or 'msl'");
        pw.close();
    }

    /**
     * Validates the source schema designator.
     * 
     * @param schemaName
     *            the input schema name from the command line
     * @throws ParseException
     *             if the entry is not valid
     * 
     * 10/6/14 - MPCS-6698. Added method.
     */
    protected void validateSourceSchema(final String schemaName)
            throws ParseException {
        if (GdsSystemProperties.getSystemProperty("datagen.test") != null) {
            return;
        }
        final String configPath = GdsSystemProperties
                .getSystemConfigDir()
                + File.separator
                + schemaName
                + File.separator + GdsHierarchicalProperties.CONSOLIDATED_PROPERTY_FILE_NAME;
        if (!new File(configPath).exists()) {
            throw new ParseException("There is no "
                    + GdsHierarchicalProperties.CONSOLIDATED_PROPERTY_FILE_NAME
                    + " for the specified source schema " + schemaName);

        }
        if (schemaName.equalsIgnoreCase("monitor")) {
            throw new ParseException(
                    "Data cannot be generated for the 'monitor' mission or schema");
        }
        if (sseFlag.isApplicationSse()) {
            throw new ParseException("There is no SSE command schema");
        }
    }

    /**
     * Parses the mission-specific command dictionary.
     * 
     * @throws DictionaryException
     *             if there is a problem reading the dictionary
     */
    private void readMissionDictionary() throws DictionaryException {

        this.missionDictionary = appContext.getBean(ICommandDictionaryFactory.class)
                .getNewInstance(appContext.getBean(DictionaryProperties.class),
                        this.dictionaryPath);
    }

    /**
     * Writes valid command/argument permutations for every command in the
     * dictionary to the configured output file.
     * 
     * @throws IOException
     *             if there is a problem writing to the file
     */
    private void writeCommands() throws IOException {

        final String toSave = this.generateAsciiCmdOutput();

        final FileOutputStream fos = new FileOutputStream(this.outputPath);
        final PrintStream ps = new PrintStream(fos);
        ps.println(toSave);
        ps.close();
        TraceManager.getDefaultTracer().info(

                "Commands written to " + this.outputPath);
    }

    /**
     * Generates output commands, one per line, for the entire dictionary, and
     * returns the entire command list as a string.
     * 
     * @return command list, as one big string
     */
    public String generateAsciiCmdOutput() {

        final List<ICommandDefinition> defs = this.missionDictionary
                .getCommandDefinitions();

        final StringBuilder cmdResults = new StringBuilder();

        for (final ICommandDefinition cmd : defs) {

            /*
             * MPCS-6677 - 9/29/14 Do not output commands with null
             * opcode. Phooey to MSL!
             */
            if (cmd.getOpcode().equalsIgnoreCase("null")) {
                continue;
            }

            cmdResults.append(cmd.getStem());

            if (!cmd.getArguments().isEmpty()) {
                cmdResults.append(",");
                writeArguments(cmdResults, cmd.getArguments());
            }

            cmdResults.append("\n");
        }

        return cmdResults.toString();
    }

    /**
     * Writes command arguments to the supplied StringBuilder.
     * 
     * @param writer
     *            the StringBuilder to write output to
     * @param args
     *            the list of ICommandArgumentDefinitions for the command
     *            arguments to write
     */
    private void writeArguments(final StringBuilder writer,
            final List<ICommandArgumentDefinition> args) {

        if (args != null && !args.isEmpty()) {
            String separator = "";
            for (final ICommandArgumentDefinition arg : args) {
                writer.append(separator);
                switch (arg.getType()) {
                case REPEAT:
                    writeRepeatArgument(writer,
                            (IRepeatCommandArgumentDefinition) arg);
                    break;
                case UNSIGNED:
                case INTEGER:
                    writeIntegerArgument(writer, arg);
                    break;
                case FLOAT:
                    writeFloatArgument(writer, arg);
                    break;
                case VAR_STRING:
                case FIXED_STRING:
                    writeStringArgument(writer, arg);
                    break;
                case TIME:
                case FLOAT_TIME:
                    writeTimeArgument(writer, arg);
                    break;
                case SIGNED_ENUMERATION:
                case UNSIGNED_ENUMERATION:
                    writeEnumArgument(writer, arg);
                    break;
                case BOOLEAN:
                    writeBooleanArgument(writer, arg);
                    break;
                case FILL:
                    break;

                default:
                    TraceManager.getDefaultTracer().error(

                            "Unrecognized command argument type "
                                    + arg.getType());
                    break;
                }
                separator = ",";
            }
        }

    }

    /**
     * Writes an integer command argument.
     * 
     * @param writer
     *            StringBuilder to write to
     * @param arg
     *            the definition of the integer argument
     */
    private void writeIntegerArgument(final StringBuilder writer,
            final ICommandArgumentDefinition arg) {

        long min = 0;
        long max = 0;

        /*
         * If there are valid ranges, we want the argument to be in one of them.
         */
        final List<IValidationRange> ranges = arg.getRanges();
        if (!ranges.isEmpty()) {
            int rangeIndex = 0;
            /*
             * If multiple ranges, randomize which one we use.
             */
            if (ranges.size() > 1) {
                rangeIndex = randInt(0, ranges.size() - 1);
            }
            min = Long.valueOf(ranges.get(rangeIndex).getMinimum());
            max = Long.valueOf(ranges.get(rangeIndex).getMaximum());
        } else {
            /* Otherwise just use min/max based upon argument length. */
            min = ((Number) arg.getMinimumValue()).longValue();
            max = ((Number) arg.getMaximumValue()).longValue();
        }
        writer.append(randLong(min, max));
    }

    /**
     * Writes a float command argument.
     * 
     * @param writer
     *            StringBuilder to write to
     * @param arg
     *            the definition of the float argument
     */
    private void writeFloatArgument(final StringBuilder writer,
            final ICommandArgumentDefinition arg) {

        /*
         * If there are valid ranges, we want the argument to be in one of them.
         */
        final List<IValidationRange> ranges = arg.getRanges();
        if (!ranges.isEmpty()) {
            int rangeIndex = 0;
            /*
             * If multiple ranges, randomize which one we use.
             */
            if (ranges.size() > 1) {
                rangeIndex = randInt(0, ranges.size() - 1);
            }
            
            /* 
             * Edit min/max by a 'fudge value' of .0001
             * Ensure that the inherent inaccuracy of floats does not cause the value to exceed the range on either end
             */
            final double min = Double.valueOf(ranges.get(rangeIndex).getMinimum()) + .0001d;
            final double max = Double.valueOf(ranges.get(rangeIndex).getMaximum()) - .0001d;
            
            /*
             * The stated min/max range values are closer together than the 'fudge' factor
             * Max cannot be less than min
             */
            if (max < min) { 
            	TraceManager.getDefaultTracer().error("Min/max float values for command " + arg.getDictionaryName() + 

            			" range definition too small for the generator to be accurate");
            } else { 
            	final double rangeval = randFloat(min, max);
            	writer.append(String.format("%.3g", rangeval));
            }
        } else {
            /*
             * No defined ranges. Argument size determines range of available
             * values. Do not use getMinimum() and getMaximum() on the argument
             * here, because those just return +/- INFINITY for floats.
             */
            double min = Double.MIN_VALUE;
            double max = Double.MAX_VALUE;

            if (arg.getBitLength() == Float.SIZE) {
                min = Float.MIN_VALUE;
            }

            if (arg.getBitLength() == Float.SIZE) {
                max = Float.MAX_VALUE;
            }

            final double val = randFloat(min, max);
            writer.append(String.format("%.3g", val));
        }
    }

    /**
     * Writes a string command argument.
     * 
     * @param writer
     *            StringBuilder to write to
     * @param arg
     *            the definition of the string argument
     */
    private void writeStringArgument(final StringBuilder writer,
            final ICommandArgumentDefinition arg) {

        /*
         * If there is a valid regular expression for the argument, we want to
         * generate a value that matches it.
         */
        if (arg.getValueRegexp() != null) {
            final String regex = this.stripSpecialConstructsFromRegex(arg
                    .getValueRegexp());
            Xeger xegergen = null;

            /*
             * MPCS-6677 - 9/29/14. Revamp logic below to catch illegal
             * argument exeption from the Xeger constructor for certain regular
             * expressions.
             */
            boolean ok = false;
            String xegergenstr = null;
            try {
                xegergen = new Xeger(regex);
                int retries = 0;

                /*
                 * xeger simply cannot do the job for some of the complex
                 * regular expressions. Loop generating strings from the regular
                 * expression until we get a match, or decide to give up.
                 */
                while (!ok && retries < 10) {
                    xegergenstr = xegergen.generate();
                    ok = xegergenstr.matches(regex);
                    retries++;
                }
            } catch (final IllegalArgumentException e) {
                SystemUtilities.doNothing();
            }

            if (!ok) {
                writer.append("\"CANNOT GENERATE REGEX MATCH\"");
            } else {
                writer.append("\"" + xegergenstr + "\"");
            }

        } else {
            /*
             * Otherwise just generate a string of random length, up to the
             * maximum length of the argument.
             */
            final StringGeneratorSeed seed = new StringGeneratorSeed();
            // use default character set
            seed.setCharSet(CommandGeneratorApp.commandCharacterSet);
            final int maxChars = Math.min(64, arg.getBitLength() / 8);
            seed.setMaxStringLength(maxChars);
            seed.setTraversalType(TraversalType.RANDOM);
            final StringGenerator gen = new StringGenerator();
            gen.setSeedData(seed);
            writer.append("\"" + gen.getRandom() + "\"");
        }

    }

    /**
     * Writes a time command argument.
     * 
     * @param writer
     *            StringBuilder to write to
     * @param arg
     *            the definition of the time argument
     */
    private void writeTimeArgument(final StringBuilder writer,
            final ICommandArgumentDefinition arg) {

        if (arg.getType() == CommandArgumentType.FLOAT_TIME) {
            /* Time is a float */
            final Double min = (Double) arg.getMinimumValue();
            final Double max = (Double) arg.getMaximumValue();
            final double val = randFloat(min, max);
            final ISclk sclk = new Sclk(val, TimeProperties.getInstance().getCanonicalEncoding());
            writer.append(sclkFmt.toDecimalString(sclk));
        } else {
            /* Time is an int */
            final Long min = (Long) arg.getMinimumValue();
            final Long max = (Long) arg.getMaximumValue();
            final long val = randLong(min, max);
            final ISclk sclk;
            final byte[] buff = new byte[Long.SIZE/8];

            GDR.set_i64(buff, 0, val);
            sclk = TimeProperties.getInstance().getCanonicalExtractor().getValueFromBytes(buff, 0);
            writer.append(sclk.toString());
        }

    }

    /**
     * Writes a repeat command argument.
     * 
     * @param writer
     *            StringBuilder to write to
     * @param arg
     *            the definition of the repeat argument
     */
    private void writeRepeatArgument(final StringBuilder writer,
            final IRepeatCommandArgumentDefinition arg) {

        int min = 0;
        int max = 0;

        /*
         * First decide how many times to repeat the argument block.
         */
        final List<IValidationRange> ranges = arg.getRanges();
        if (ranges.isEmpty()) {
            min = Math.max(((Long) arg.getMinimumValue()).intValue(), 1);
            /*
             * The maximum value in this case will be huge. Just set to 3
             * repeats.
             */
            max = 3;
        } else {
            min = Math.max(Integer.valueOf(ranges.get(0).getMinimum()), 1);
            max = Integer.valueOf(ranges.get(0).getMaximum());
        }

        /*
         * Write the number of repeats.
         */
        final int numRepeats = randInt(min, max);
        writer.append(numRepeats);

        /*
         * Now write the argument block the proper number of times.
         */
        if (numRepeats > 0) {
            for (int i = 1; i <= numRepeats; i++) {
                writer.append(',');
                final List<ICommandArgumentDefinition> subArgs = arg
                        .getDictionaryArguments();
                writeArguments(writer, subArgs);
            }
        }

    }

    /**
     * Writes an enumerated command argument.
     * 
     * @param writer
     *            StringBuilder to write to
     * @param arg
     *            the definition of the enumerated argument
     */
    private void writeEnumArgument(final StringBuilder writer,
            final ICommandArgumentDefinition arg) {

        final CommandEnumerationDefinition enumDef = arg.getEnumeration();
        final List<ICommandEnumerationValue> enumList = enumDef
                .getEnumerationValues();

        /*
         * If there are valid ranges, we want the argument to be in one of them.
         */
        final List<IValidationRange> ranges = arg.getRanges();
        if (!ranges.isEmpty()) {
            int rangeIndex = 0;
            /*
             * If multiple ranges, randomize which one we use.
             */
            if (ranges.size() > 1) {
                rangeIndex = randInt(0, ranges.size() - 1);
            }
            /*
             * There is no way to randomize within the range. Choose either the
             * minimum or the maximum, randomly.
             */
            final int choice = randInt(0, 1);
            if (choice == 0) {
                String minVal = ranges.get(rangeIndex).getMinimum();

                /*
                 * This nonsense exists because of the MSL
                 * XML format. XML command enumeration values have three
                 * values: FSW value, dictionary value, and bit value. We don;t
                 * accept FSW value on the command line, only the dictionary
                 * value. But MSL defined all of its enum ranges by FSW value.
                 */
                if (enumDef.lookupByDictionaryValue(minVal) == null) {
                    if (enumDef.lookupByFswValue(minVal) != null) {
                        minVal = enumDef.lookupByFswValue(minVal)
                                .getDictionaryValue();
                    }
                }
                writer.append(minVal);
            } else {
                String maxVal = ranges.get(rangeIndex).getMaximum();
                /*
                 * See note in the previous case about the MSL nonsense
                 * being handled here.
                 */
                if (enumDef.lookupByDictionaryValue(maxVal) == null) {
                    if (enumDef.lookupByFswValue(maxVal) != null) {
                        maxVal = enumDef.lookupByFswValue(maxVal)
                                .getDictionaryValue();
                    }
                }
                writer.append(maxVal);
            }

        } else {
            /*
             * Otherwise pick a random enumerated value from the values in the
             * table.
             */
            final int randVal = randInt(0, enumList.size() - 1);
            writer.append(enumList.get(randVal).getDictionaryValue());
        }
    }

    /**
     * Writes a boolean command argument.
     * 
     * @param writer
     *            StringBuilder to write to
     * @param arg
     *            the definition of the boolean argument
     */
    private void writeBooleanArgument(final StringBuilder writer,
            final ICommandArgumentDefinition arg) {

        final int randVal = randInt(0, 1);
        final CommandEnumerationDefinition enumDef = arg.getEnumeration();
        if (randVal == 0) {
            final ICommandEnumerationValue val = enumDef
                    .lookupByFswValue("TRUE");
            writer.append(val.getDictionaryValue());
        } else {
            final ICommandEnumerationValue val = enumDef
                    .lookupByFswValue("FALSE");
            writer.append(val.getDictionaryValue());
        }

    }

    /**
     * Generates a random integer between min and max (inclusive).
     * 
     * @param min
     *            the minimum desired value
     * @param max
     *            the maximum desired value
     * @return random value between min and max
     */
    private int randInt(final int min, final int max) {

        // Calculate distance between min and max
        int distance = Math.abs(max - min);

        // If not already at integer max, add one to make the value inclusive of
        // max
        if (distance != Integer.MAX_VALUE) {
            distance++;
        }

        // Choose random number from 0 to distance
        final int randomNum;
        if (distance > 0) {
            randomNum = this.random.nextInt(distance);
        } else {
            randomNum = 0;
        }

        // Add randomNum to min to get the final random integer
        return randomNum + min;
    }

    /**
     * Generates a random long between min and max (inclusive).
     * 
     * @param min
     *            the minimum desired value
     * @param max
     *            the maximum desired value
     * @return random value between min and max
     */
    private long randLong(final long min, final long max) {

        return min + ((long) (this.random.nextDouble() * (max - min)));
    }

    /**
     * Generates a random double between min and max (inclusive).
     * 
     * @param min
     *            the minimum desired value
     * @param max
     *            the maximum desired value
     * @return random value between min and max
     */
    private double randFloat(final double min, final double max) {

        final double range = Math.abs(max - min);
        final double scaled = this.random.nextDouble() * range;
        return scaled + min;
    }

    /**
     * Fixes up a regular expression fromt he dicitonary such that xeger handles
     * it better.
     * 
     * @param regex
     *            the input regular expression
     * @return doctored regular expression
     */
    private String stripSpecialConstructsFromRegex(String regex) {

        // Replace special constructs that xeger cannot handle

        regex = regex.replaceAll("\\(\\?[a-z]\\)", "");

        // Strip ^ and $ from beginning and end, as xeger will put these on the
        // output string */
        if (regex.charAt(0) == '^') {
            regex = regex.substring(1);
        }
        if (regex.endsWith("$")) {
            regex = regex.substring(0, regex.length() - 1);
        }
        return regex;
    }

    /**
     * Main method.
     * 
     * @param args
     *            command line arguments
     */
    public static void main(final String[] args) {

        final CommandGeneratorApp theApp = new CommandGeneratorApp();

        try {
            final DatagenOptions ro = theApp.createOptions();
            final ICommandLine commandLine = ro.parseCommandLine(args, true);
            theApp.configure(commandLine);

            theApp.readMissionDictionary();
            theApp.writeCommands();

        } catch (final DictionaryException e) {
            TraceManager.getDefaultTracer().error(e.getMessage());

            System.exit(1);

        } catch (final ParseException e) {
            if (e.getMessage() == null) {
                TraceManager.getDefaultTracer().error(e.toString());

            } else {
                TraceManager.getDefaultTracer().error(e.getMessage());

            }
            System.exit(1);
        } catch (final Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.exit(0);
    }
}
