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
package jpl.gds.tc.api.command.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.OpcodeUtil;
import jpl.gds.dictionary.api.command.CommandArgumentType;
import jpl.gds.dictionary.api.command.ICommandArgumentDefinition;
import jpl.gds.dictionary.api.command.ICommandDefinition;
import jpl.gds.dictionary.api.command.ICommandDefinitionProvider;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.ICommandFileLoad;
import jpl.gds.tc.api.ICommandObjectFactory;
import jpl.gds.tc.api.IFileLoadInfo;
import jpl.gds.tc.api.command.ICommand;
import jpl.gds.tc.api.command.IFlightCommand;
import jpl.gds.tc.api.command.IFlightCommandTranslator;
import jpl.gds.tc.api.command.ISseCommand;
import jpl.gds.tc.api.config.CommandProperties;
import jpl.gds.tc.api.config.FileLoadParseException;
import jpl.gds.tc.api.exception.CommandFileParseException;
import jpl.gds.tc.api.exception.CommandParseException;


/**
 * The UplinkInputParser class is responsible for parsing all the various types of uplink user input and
 * serializing it into the objects used by the uplink processing.
 *
 * MPCS-10473 - 03/21/19 - Moved some functionality to FlightCommand. Updated remaining
 * functionality to utilize FlightCommand and index to argument instead of handling argument
 * directly.
 * <p>
 * Moved the command string splitting logic in parseCommandString to a separate function.
 */
public final class UplinkInputParser {
    /**
     * The string used to separate arguments in a command string
     */
    public static final String SEPARATOR_STRING = ",";
    /**
     * A regular expression for defining the format of what a command string should look like.  It's essentially:
     * <p>
     * 0 or more non-comma characters followed by a comma followed, repeat that pattern, ending with a string of non-comma characters
     * <p>
     * It essentially matches:
     * <p>
     * adfsdfsaasfdafds,sadffsdafdsa,asdsfdaa,adfssdfadfsa,sdafasasfd
     */
    public static final String COMMAND_ARG_STRING_REGEXP = "([^" + SEPARATOR_STRING + "]{0,}" + SEPARATOR_STRING + "{1}){0,}[^" + SEPARATOR_STRING + "]{0,}";

    /**
     * The comment character for a command list file
     */
    public static final String FILE_COMMENT_PREFIX = "//";

    /**
     * Remove the comments from a line that was read from a command list file.  Also strips leading
     * and trailing whitespace.
     *
     * @param input The input line from a command list file whose comments should be removed
     * @return The input string with comments removed.  If the input string contained no comment, the
     * string is returned unchanged.  If the entire string was a comment, the empty string is returned.
     */
    public static String removeComments(final String input) {
        String tempInput = input;

        //strip leading/trailing whitespace
        tempInput = tempInput.trim();

        //find the comment location
        final int commentLoc = tempInput.indexOf(FILE_COMMENT_PREFIX);

        //whole line was a comment
        if (commentLoc == 0) {
            tempInput = "";
        }
        //strip out the comment
        else if (commentLoc > 0) {
            tempInput = tempInput.substring(0, commentLoc).trim();
        }

        return (tempInput);
    }

    /**
     * Given a command list file, which has one command (FSW or SSE) per line, read in all the various commands,
     * construct an object for each command and return the list (in the same order as the file!) of all the commands
     * that were in the file.
     *
     * @param appContext the ApplicationContext in which this object is being used
     * @param cmdFile    The command list file containing all of the commands to parse
     * @return A list of all the commands read from the file (both FSW and SSE commands).
     * @throws CommandFileParseException If there's an issue parsing any part of the input file
     */
    public static List<ICommand> parseCommandListFromFile(final ApplicationContext appContext, final File cmdFile) throws CommandFileParseException {
        if (cmdFile == null) {
            throw new IllegalArgumentException("Null input filename");
        } else if (!cmdFile.exists()) {
            throw new CommandFileParseException("Input file " + cmdFile.getName() + " does not exist.");
        }

        final List<ICommand> commandList = new ArrayList<>(64);

        try (BufferedReader reader = new BufferedReader(new FileReader(cmdFile))) {

            //read one line at a time
            String line = reader.readLine();
            while (line != null) {
                final String commandString = removeComments(line);

                // skip lines that are commented and blank lines
                if (!commandString.isEmpty()) {
                    //parse the command and add it to the list
                    commandList.add(parseCommandString(appContext, commandString));
                }

                line = reader.readLine();
            }

        } catch (final Exception e) {
            throw new CommandFileParseException("Error parsing command list from file: " + e.getMessage(), e);
        }

        return (commandList);
    }

    /**
     * Parse a single command string into a command object.
     *
     * @param appContext     the ApplicationContext in which this object is being used
     * @param inputArgString The command string with stem and arguments as input by the user.
     * @return The command object corresponding to the user input command string.
     * @throws CommandParseException If there is an error parsing the input command string.
     */
    public static ICommand parseCommandString(final ApplicationContext appContext, final String inputArgString) throws CommandParseException {
        String argString = inputArgString;

        if (argString == null) {
            throw new IllegalArgumentException("Null input String!");
        } else if (!argString.matches(COMMAND_ARG_STRING_REGEXP)) {
            throw new CommandParseException("Improperly formatted command: " + argString);
        }

        // remove quotes from around the entire command (if they exist)
        argString = StringUtil.removeQuotes(argString.trim());

        //Check if this is an SSE command...if so nothing else needs to be done
        if (ISseCommand.isSseCommand(appContext.getBean(CommandProperties.class), argString)) {
            // MPCS-7163 04/22/15 Use factory and not constructor
            final ISseCommand sseCommand = appContext.getBean(ICommandObjectFactory.class).createSseCommand(argString);

            return (sseCommand);
        }
        //If this is an SSE chill up, this must be an SSE command...add the prefix
        //TODO: this seems like it would double-prepend the prefix if the prefix is already there (brn)
        else if (appContext.getBean(SseContextFlag.class).isApplicationSse()) {
            // MPCS-7163 04/22/15 Use factory and not constructor
            final ISseCommand sseCommand = appContext.getBean(ICommandObjectFactory.class).createSseCommand(
                    appContext.getBean(CommandProperties.class).getSseCommandPrefix() + argString);
            return (sseCommand);
        }

        final String[] args = splitCommandString(argString, SEPARATOR_STRING);

        if (args.length == 0) {
            throw new CommandParseException("Improperly formatted command: " + argString);
        }
        // the command string has been split into proper pieces, create the command object
        return (parseFlightCommandStringPieces(appContext, args));
    }

    //pulled this out - not only so we can use it elsewhere in UplinkInputParser, but we can make this better at some point
    //TODO: Use a regex instead of all of this work???
    public static String[] splitCommandString(final String argString, final String separatorString) throws CommandParseException {
        // split the input into a stem/opcode and command arg values
        final String[] tempArgs = argString.split(separatorString);
        final List<String> argPieces = new ArrayList<>(tempArgs.length);

        // we need to loop through and do some checking to make sure that the
        // SEPARATOR_STRING was not part of the value for a string argument and
        // if it was then we have to reconstruct it (if there was a SEPARATOR_STRING
        // entered between double quotes as part of the value for a string argument, then
        // the argString.split(...) command split on it too even when it shouldn't)
        //
        // For example, say your SEPARATOR_STRING is a comma, then imagine what happens to
        // the string value "ab,cd" when it is parsed in
        for (int z = 0; z < tempArgs.length; z++) {
            final StringBuilder tempValue = new StringBuilder(tempArgs[z].trim());

            // if this value starts with a quote, then we need to keep reading until the
            // end quote and put it in a single entry for processing later
            if (tempValue.toString().startsWith("\"")) {
                boolean insideQuote = true;
                do {
                    // if we've found the end quote
                    // (the length > 1 makes sure that this string isn't just a single quote mark)
                    // (the count%2 checks that the string contains an even # of quotation marks)
                    if (tempValue.toString().endsWith("\"") && tempValue.length() > 1 && StringUtil.count(tempValue.toString(), '\"') % 2 == 0) {
                        insideQuote = false;
                    }
                    // we haven't reached the end of the quoted text yet
                    else {
                        // get the next value out of the input array and append it to the current
                        // value (it's all part of the quoted text)...note that we also have to add
                        // back in the separator string that was removed by the String.split operation
                        // done earlier
                        z++;
                        if (z == tempArgs.length) {
                            throw new CommandParseException("Could not find a closing \" mark for this part of the command line input: " + tempValue);
                        }
                        tempValue.append(SEPARATOR_STRING + tempArgs[z]);
                    }
                } while (insideQuote);
            }

            argPieces.add(tempValue.toString());
        }

        // get an array containing the stem/opcode and the full values of each argument
        return argPieces.toArray(new String[argPieces.size()]);
    }

    /**
     * Given an array of command stem/opcode and argument values, generate the corresponding command object.
     *
     * @param appContext the ApplicationContext in which this object is being use
     * @param args       The array of stem/arguments to convert to a command object
     * @return The command object corresponding to the input array values
     * @throws CommandParseException If there's an issue parsing the input values
     */
    private static IFlightCommand parseFlightCommandStringPieces(final ApplicationContext appContext, final String[] args) throws CommandParseException {
        // make sure we have a dictionary to work with
        ICommandDefinition tempCommand = null;
        ICommandDefinitionProvider dictionary = null;
        try {
            dictionary = appContext.getBean(ICommandDefinitionProvider.class);
        } catch (final Exception e) {
            throw new CommandParseException("Error retrieving command dictionary: " + e.getMessage(), e);
        }

        // we got an opcode
        boolean hexOrBinaryInput = false;
        final String stemOrOpcodeValue = StringUtil.removeQuotes(args[0]);

        /** MPCS-7725 01/21/16 Use OpcodeUtil */
        if (OpcodeUtil.hasHexPrefix(stemOrOpcodeValue)) {
            // look up the command by opcode
            tempCommand = dictionary.getCommandDefinitionForOpcode(stemOrOpcodeValue);
            hexOrBinaryInput = true;
        } else if (OpcodeUtil.hasBinaryPrefix(stemOrOpcodeValue)) {
            tempCommand = dictionary.getCommandDefinitionForOpcode(OpcodeUtil.toHexFromBin(stemOrOpcodeValue));
            hexOrBinaryInput = true;
        }
        // we got a stem
        else {
            // lookup the command by stem
            tempCommand = dictionary.getCommandDefinitionForStem(stemOrOpcodeValue);
        }

        if (tempCommand == null) {
            if (hexOrBinaryInput) {
                throw new CommandParseException("Unrecognized command opcode \"" + stemOrOpcodeValue + "\"!");
            } else {
                throw new CommandParseException("Unrecognized command stem \"" + stemOrOpcodeValue + "\"!");
            }
        }

        // make a copy so we don't mess up the command object stored in the dictionary
        final IFlightCommand command = appContext.getBean(ICommandObjectFactory.class).createFlightCommand(tempCommand);
        parseArgumentsForCommand(appContext, command, args);

        return (command);
    }

    /**
     * Given the dictionary definitions of all the arguments for a command,
     * parse all their associated values in the argument objects.
     *
     * @param appContext the ApplicationContext in which this object is being use
     * @param cmd        The IFlightCommand to be populated with argument values
     * @param args       The associated values for each argument in the command
     * @throws CommandParseException If there's a problem parsing the argument values
     *
     * 11/8/13 - MPCS-5521. Changed type of "arguments" argument to use ICommandArgumentDefinition interface
     */
    private static void parseArgumentsForCommand(final ApplicationContext appContext, final IFlightCommand cmd, final String[] args) throws CommandParseException {
        // loop through all the arguments in the command & parse them
        int argIndex = 1;
        for (int cmdIndex = 0; cmdIndex < cmd.getArgumentCount(); cmdIndex++) {
            if (argIndex >= args.length) {
                /*
                 * 11/14/13 - MPCS-5521. Removed dependence on XML root element name here.
                 * There is no reason we need to log errors using XML root element names.
                 */
                throw new CommandParseException("Parser expected a value for the " + cmd.getArgumentType(cmdIndex) + " argument " +
                        " with dictionary name \"" + cmd.getArgumentDefinition(cmdIndex).getDictionaryName()
                        + "\", but there are no more argument values on the command line");
            }

            argIndex = parseAndSetArgument(appContext, cmd, cmdIndex, args, argIndex);
        }

        //there were more arg values than arguments
        if (argIndex != args.length) {
            final StringBuilder errorString = new StringBuilder(1024);
            errorString.append("Parser was finished, but there are still extra command argument values on the command line: \n");
            for (int a = 1; argIndex < args.length; argIndex++, a++) {
                errorString.append("Extra Argument Value #");
                errorString.append(a);
                errorString.append(" = ");
                errorString.append(args[argIndex]);
                errorString.append("\n");
            }
            throw new CommandParseException(errorString.toString());
        }
    }

    /**
     * Given a command argument object, an array of argument values and the index into the array where the value is located, parse
     * that value as the value of the input command argument.
     *
     * @param appContext the ApplicationContext in which this object is being use
     * @param cmd        the IFlightCommand holding the argument to have its values set
     * @param argIndex   the index to the argument to be set in the IFlightCommand
     * @param args       The array of user input command argument values
     * @param j          The index into the array of where the current command argument value is located
     * @return The location of the next argument value in the array
     * @throws CommandParseException If there's an error parsing the command argument value
     */
    public static int parseAndSetArgument(final ApplicationContext appContext, final IFlightCommand cmd, final int argIndex, final String[] args, final int j) throws CommandParseException {
        if (cmd.getArgumentType(argIndex).isRepeat()) {
            return (parseAndSetRepeatArgument(appContext, cmd, argIndex, args, j));
        }

        parseAndSetCommandArgument(appContext, cmd, argIndex, args[j]);
        return (j + 1);
    }

    /**
     * Given a command argument definition and an argument value, parse then value according to the definition.
     *
     * @param cmd      the IFlightCommand holding the argument to have its values set
     * @param argIndex the index to the argument to be set in the IFlightCommand
     * @param value    The associated value for the command argument
     * @throws CommandParseException If there's an error parsing the argument value for the command argument
     */
    public static void parseArgument(final ApplicationContext appContext, final IFlightCommand cmd, final int argIndex, final String value) throws CommandParseException {
        if (cmd == null || value == null || argIndex < 0) {
            throw new IllegalArgumentException("parseArgument was supplied an invalid value");
        }
        if (cmd.getArgumentType(argIndex).isRepeat()) {
            final String[] values = splitCommandString(value, cmd.getRepeatArgumentSeparator(argIndex));
            parseAndSetRepeatArgument(appContext, cmd, argIndex, values, 0);
        } else {
            parseAndSetCommandArgument(appContext, cmd, argIndex, value);
        }
    }

    /**
     * Utility function for parsing arguments input by the user via the Command Builder
     *
     * @param appContext the ApplicationContext in which this object is being used
     * @param cmd        the IFlightCommand holding the argument to have its values set
     * @param argIndex   the index to the argument to be set in the IFlightCommand
     * @param value      The user input command argument value
     * @throws CommandParseException If there's an error parsing the argument value
     */
    public static void parseAndSetGuiArgument(final ApplicationContext appContext, final IFlightCommand cmd, final int argIndex, final String value) throws CommandParseException {
        if (cmd.getArgumentType(argIndex).isRepeat()) {

            final String[] valuesArray = splitCommandString(value, cmd.getRepeatArgumentSeparator(argIndex));


            final String userArgValue = valuesArray[0];

            parseAndSetArgument(appContext, cmd, argIndex, valuesArray, 0);
            cmd.setArgumentValue(argIndex, userArgValue);
        } else {
            parseArgument(appContext, cmd, argIndex, value);
        }
    }

    /**
     * Parse a repeat argument according to the given definition and values.
     *
     * @param appContext the ApplicationContext in which this object is being used
     * @param cmd        The command dictionary definition of the repeat argument
     * @param args       The argument values input by the user
     * @param loc        The location in the array where the repeat arguments begin
     * @return The location of the next argument in the args array after the repeat argument
     * @throws CommandParseException If there's an error parsing the repeat argument
     *
     * MPCS-10473 - 04/05/19 - significantly updated since the command must hold onto the arguments.
     */
    private static int parseAndSetRepeatArgument(final ApplicationContext appContext, final IFlightCommand cmd, final int argIndex, final String[] args, final int loc) throws CommandParseException {

        final CommandProperties cmdProps = appContext.getBean(CommandProperties.class);

        int j = loc;

        /*
         * Must disable validation before we set the numeric portion of the repeat. Before we would just set this value and move on, but now we validate anything we're setting into an argument
         * from UplinkInputParser.
         * When we set this value the argument value in a repeat, we're setting the number of repeats that should be present and this is most likely NOT the number of repeat sets that are
         * present.
         * Once the number of repeats is set we can reenable validation.
         */
        final boolean oldValidate = cmdProps.getValidateCommands();
        cmdProps.setValidateCommands(false);
        try {
            //parse the numeric portion of the repeat argument
            parseAndSetCommandArgument(appContext, cmd, argIndex, args[j]);
        } catch (final CommandParseException e) {
            //ignore this for now, not only does this value get set again, but things might be in a wonky state
        } finally {
            cmdProps.setValidateCommands(oldValidate);
        }
        j++;

        //figure out how many argument values to read from the array
        int numArgBlocks = 0;
        try {
            //the numeric portion of the repeat argument is the number of repeat blocks
            numArgBlocks = Integer.parseInt(cmd.getArgumentValue(argIndex));
        } catch (final NumberFormatException e) {
            throw new CommandParseException("Could not interpret value of " +
                    cmd.getArgumentValue(argIndex) + " for argument with dictionary name " +
                    cmd.getArgumentDefinition(argIndex).getDictionaryName());
        }
        if (numArgBlocks < 0) {
            throw new CommandParseException("The command line parser detected that the repeat argument (dictionary name = " + cmd.getArgumentDefinition(argIndex).getDictionaryName() + ", fsw name = " +
                    cmd.getArgumentDefinition(argIndex).getFswName() + ") has been supplied a negative repeat count value.  The repeat argument cannot be parsed due to this invalid value.");
        }

        //the total number of arguments is the number of arguments times the number or repeats
        final int argsNeeded = numArgBlocks * cmd.getRepeatArgumentDefinition(argIndex).getDictionaryArgumentCount(false); // have to ignore fill arguments

        //if we don't have enough arguments...
        if (j + argsNeeded > args.length) {
            throw new CommandParseException("Parser expected more argument values for the RepeatArgument with dictionary name \"" + cmd.getArgumentDefinition(argIndex).getDictionaryName()
                    + "\", but there are no more argument values on the command line");
        }

        //split off the arugments we need for easier processing.
        final String[] repeatArgs = Arrays.copyOfRange(args, j, j + argsNeeded);

        j += argsNeeded;

        parseAndSetRepeatArgumentValuesFromArray(appContext, cmd, argIndex, repeatArgs);

        /*
         * At the time we set the argument value, the number of repeats may not have matched this value, hence why validation was turned off.
         * Now that the value has been set and the repeats have all been created and set, let's validate it.
         */
        validTransmittableCheck(appContext, cmd, argIndex, cmd.getArgumentValue(argIndex));

        return (j);
    }

    /**
     * Given an array of argument values, populate the valued arguments of this
     * repeat argument.
     *
     * @param appContext the ApplicationContext in which this object is being used
     * @param cmd        the IFlightCommand holding the argument to have its values set
     * @param argIndex   the index to the argument to be set in the IFlightCommand
     * @param values     The array of repeat argument values to add
     * @throws CommandParseException If the input argument values can't be parsed properly
     *
     * MPCS-10473 - 04/05/19 - renamed from "setArgumentValuesFromArray". Only was used to set REPEAT arguments
     */
    public static void parseAndSetRepeatArgumentValuesFromArray(final ApplicationContext appContext, final IFlightCommand cmd, final int argIndex, final String[] values)
            throws CommandParseException {

        if (values == null) {
            throw new IllegalArgumentException("Null input array!");
        } else if (values.length == 0) {
            return;
        }

        final String argValue = cmd.getArgumentValue(argIndex);
        // Check how many times this argument is supposed to repeat
        Integer argumentIntValue = null;
        try {
            argumentIntValue = Integer.valueOf(argValue);
        } catch (final NumberFormatException e) {
            throw new CommandParseException(
                    "Could not parse value for number of repeats for argument "
                            + cmd.getArgumentDefinition(argIndex).getDictionaryName() + ": "
                            + e.getMessage());
        }

        // make sure we receive the correct amount of input
        final int numArgs = cmd.getRepeatArgumentDefinition(argIndex).getDictionaryArgumentCount(false);
        if ((values.length % numArgs) != 0) {
            throw new CommandParseException(
                    "The "
                            + values.length
                            + " values given RepeatArgument with dictionary name \""
                            + cmd.getArgumentDefinition(argIndex).getDictionaryName()
                            + "\" is not a multiple of the "
                            + numArgs
                            + " arguments specified in the dictionary (excluding filler arguments)");
        } else if ((values.length / numArgs) != argumentIntValue.intValue()) {
            throw new CommandParseException(
                    "The given # of repeats does not match the number of argument values given for the repeat argument"
                            + " with dictionary name "
                            + cmd.getArgumentDefinition(argIndex).getDictionaryName());
        }

        //now that we've verified everything is ready to go, let's clear the old repeat and set up again.
        cmd.clearArgumentValue(argIndex);
        cmd.setArgumentValue(argIndex, argValue);
        for (int i = 0; i < argumentIntValue; i++) {
            cmd.addRepeatArguments(argIndex, 0);
        }

        for (int subIndex = 0; subIndex < values.length; subIndex++) {
            parseAndSetCommandArgument(appContext, cmd, argIndex, subIndex, values[subIndex]);
        }
    }

    /**
     * Given a command, an argument index, and an argument value, parse the value according to the definition.
     *
     * @param appContext the ApplicationContext in which this object is being used
     * @param cmd        the IFlightCommand holding the argument to have its values set
     * @param argIndex   the index to the argument to be set in the IFlightCommand
     * @param value      The associated user input argument value
     * @throws CommandParseException If there is an error parsing the input argument value
     * MPCS-10473 - 04/05/19 - renamed from "parseCommandArgument" since it also sets the argument value
     */
    public static void parseAndSetCommandArgument(final ApplicationContext appContext, final IFlightCommand cmd, final int argIndex, final String value) throws CommandParseException {
        parseAndSetCommandArgument(appContext, cmd, argIndex, -1, value);
    }

    /**
     * Given a command, a repeat argument index, an index to the argument within, and an argument value, parse the value according to the definition.
     *
     * @param appContext  the ApplicationContext in which this object is being used
     * @param cmd         the IFlightCommand holding the argument to have its values set
     * @param argIndex    the index to the repeat argument containing the argument to be set in the IFlightCommand
     * @param argSubIndex the index to the argument to be set in the IFlightCommand
     * @param value       The associated user input argument value
     * @throws CommandParseException If there is an error parsing the input argument value
     *
     * MPCS-10473 - 04/05/19 - new function - Because of how the old UplinkInputParser worked, we only needed one function for that, but now that we don't have direct access
     * to the arguments, we have to address it. In order to make it work with both regular and repeat arguments, -1 is passed in if the desired argument is NOT a repeat argument.
     *
     * MPCS-10745 - 06/30/19 - replaced calls to cmd.parseAndSetArgumentValueFromBitString to get an IFlightCommandTranslator and have that parse the argument value
     *
     * TODO: probably move more of this to be within the command translator
     */
    public static void parseAndSetCommandArgument(final ApplicationContext appContext, final IFlightCommand cmd, final int argIndex, final int argSubIndex, final String value) throws CommandParseException {

        final ICommandArgumentDefinition argDef = argSubIndex == -1 ? cmd.getArgumentDefinition(argIndex) : cmd.getArgumentDefinition(argIndex, argSubIndex);
        final CommandArgumentType argType = argSubIndex == -1 ? cmd.getArgumentType(argIndex) : cmd.getArgumentType(argIndex, argSubIndex);


        //see if the string is quoted and strip the quotes if need be
        final boolean quoted = StringUtil.isQuoted(value);
        final String unquotedValue = StringUtil.removeQuotes(value);

        //see if the user wants the default value of the argument
        if (unquotedValue.equals(appContext.getBean(CommandProperties.class).getDefaultValueString())) {

            final String defaultValue = argDef.getDefaultValue();
            if (defaultValue == null) {
                throw new CommandParseException("The command line parser was given the default value string \"" + appContext.getBean(CommandProperties.class).getDefaultValueString() + "\" for the argument with dictionary name "
                        + argDef.getDictionaryName() + ", but that argument does not have a default value defined in the command dictionary");
            }

            if (argSubIndex == -1) {
                cmd.setArgumentValue(argIndex, defaultValue);
            } else {
                cmd.setArgumentValue(argIndex, argSubIndex, defaultValue);
            }
            return;
        }

        //this has to go first so we can send strings like "0x0070" as an ASCII string and
        //not have it be interpreted as a hex value by MPCS (some instrument commands do this type of thing)
        if (quoted && argType.isString()) {
            if (argSubIndex == -1) {
                cmd.setArgumentValue(argIndex, unquotedValue);
            } else {
                cmd.setArgumentValue(argIndex, argSubIndex, unquotedValue);
            }
        }
        //see if the user input a hex (0x) or binary (0b) value
        else if (BinOctHexUtility.hasHexPrefix(unquotedValue) || BinOctHexUtility.hasBinaryPrefix(unquotedValue)) {
            //translate the user input to a bit string
            String argBits = null;
            if (BinOctHexUtility.hasHexPrefix(unquotedValue)) {
                argBits = BinOctHexUtility.toBinFromHex(BinOctHexUtility.stripHexPrefix(unquotedValue));
            } else {
                argBits = BinOctHexUtility.stripBinaryPrefix(unquotedValue);
            }

            if (argBits.length() > argDef.getBitLength() && !argDef.isVariableLength()) {
                throw new CommandParseException("The input value " + value + " for argument (dictionary name = \""
                        + argDef.getDictionaryName() + "\", fsw name = \"" + argDef.getFswName() + "\") has a bit length of " + argBits.length()
                        + " which is greater than the expected length of " + argDef.getBitLength() + " bits.");
            }

            //parse in the argument from a bit string
            try {
                // MPCS-7415 Lavin Zhang
                // We only want the enumerated integer not enumerated value
                // When ca is of BaseEnumeratedArgument, parseFromBitString
                // will return the enumerated value
                // This is to comply with GdsConfig option bitValueFormat
                // which BaseEnumeratedArgument.parseFromBitString will catch
                if (argType.isEnumeration()) {
                    if (argSubIndex == -1) {
                        cmd.setArgumentValue(argIndex, argBits);
                    } else {
                        cmd.setArgumentValue(argIndex, argSubIndex, argBits);
                    }
                } else {
                    if (argSubIndex == -1) {
                        appContext.getBean(IFlightCommandTranslator.class, appContext).setCommand(cmd).parseAndSetArgumentValueFromBitString(argIndex, argBits, 0);
                    } else {
                        appContext.getBean(IFlightCommandTranslator.class, appContext).setCommand(cmd).parseAndSetArgumentValueFromBitString(argIndex, argSubIndex, argBits, 0);
                    }
                }
            } catch (final Exception e) {
                throw new CommandParseException("Error interpreting hex value \"" + unquotedValue
                        + "\" for argument with dictionary name = \"" + argDef.getDictionaryName() + "\": " + e.getMessage(), e);
            }
        } else {
            if (argSubIndex == -1) {
                cmd.setArgumentValue(argIndex, unquotedValue);
            } else {
                cmd.setArgumentValue(argIndex, argSubIndex, unquotedValue);
            }
        }

        validTransmittableCheck(appContext, cmd, argIndex, argSubIndex, value);
    }

    // MPCS-10473 - 04/05/19 - new function. This was

    /**
     * Verifies that a specific argument within the command is both valid and transmittable
     *
     * @param appContext the application context that the command is being used in
     * @param cmd        the command in question
     * @param argIndex   the index of the argument in question
     * @param value      the base (untranslated) command argument
     *
     * @throws CommandParseException
     */
    public static void validTransmittableCheck(final ApplicationContext appContext, final IFlightCommand cmd, final int argIndex, final String value) throws CommandParseException {
        validTransmittableCheck(appContext, cmd, argIndex, -1, value);
    }

    /**
     * Verifies that a specific argument within the command is both valid and transmittable
     *
     * @param appContext  the application context that the command is being used in
     * @param cmd         the command in question
     * @param argIndex    the index of the argument in question
     * @param argSubIndex the sub index of the argument in question
     * @param value       the base (untranslated) command argument
     *
     * @throws CommandParseException
     */
    public static void validTransmittableCheck(final ApplicationContext appContext, final IFlightCommand cmd, final int argIndex, final int argSubIndex, final String value) throws CommandParseException {

        final ICommandArgumentDefinition argDef = argSubIndex == -1 ? cmd.getArgumentDefinition(argIndex) : cmd.getArgumentDefinition(argIndex, argSubIndex);

        final boolean isValid = argSubIndex == -1 ? cmd.isArgumentValueValid(argIndex) : cmd.isArgumentValueValid(argIndex, argSubIndex);
        final boolean isTransmittable = argSubIndex == -1 ? cmd.isArgumentValueTransmittable(argIndex) : cmd.isArgumentValueTransmittable(argIndex, argSubIndex);

        if (!isValid && appContext.getBean(CommandProperties.class).getValidateCommands()) {
            throw new CommandParseException("Illegal/Invalid argument value \"" + value + "\" supplied to argument (dictionary name = \""
                    + argDef.getDictionaryName() + "\", fsw name = \"" + argDef.getFswName() + "\"). Make sure that the value you supplied fits in the " +
                    argDef.getBitLength() + " bits allotted to this argument and that the value is valid according to any restrictions given in the command dictionary.");
        } else if (!isTransmittable) {
            throw new CommandParseException("Untransmittable argument value \"" + value + "\" supplied to argument (dictionary name = \""
                    + argDef.getDictionaryName() + "\", fsw name = \"" + argDef.getFswName() + "\"). Make sure that the value you supplied fits in the " +
                    argDef.getBitLength() + " bits allotted to this argument.");
        }
    }

    /**
     * Given a file load info object, create the associated list of file loads that correspond to it (it's not
     * 1 to 1 because the file might need to be split into multiple file loads).
     *
     * @param appContext the ApplicationContext in which this object is being used
     * @param info       The file load info object defining a user input file load
     * @return A list of command file loads based on the user input
     * @throws FileLoadParseException If there's an issue interpreting the user input file load info
     */
    public static List<ICommandFileLoad> createFileLoadsFromInfo(final ApplicationContext appContext, final IFileLoadInfo info) throws FileLoadParseException {
        final List<IFileLoadInfo> infoList = new ArrayList<>(1);
        infoList.add(info);
        return (createFileLoadsFromInfo(appContext, infoList));
    }

    /**
     * Parse the user input file load details into a set of file load objects.
     *
     * @param appContext    the ApplicationContext in which this object is being used
     * @param args          The user input values from the command line
     * @param overwriteFlag The overwrite flag for a file load as input by the user
     * @return A list of command file loads based on the user input
     * @throws FileLoadParseException If there's an issue interpreting the user input file load info
     */
    public static List<ICommandFileLoad> createFileLoadsFromCommandLine(final ApplicationContext appContext, final String[] args, final boolean overwriteFlag) throws FileLoadParseException {
        final List<IFileLoadInfo> inputTriples = readFileInputTriples(appContext, args, overwriteFlag);
        return (createFileLoadsFromInfo(appContext, inputTriples));
    }

    /**
     * Given a set of file load info triples (input file, output file, file type) input by the user, create the associated
     * file load objects.
     *
     * @param appContext   the ApplicationContext in which this object is being used
     * @param inputTriples The user input file load information
     * @return A list of all the file load objects associated with the user input
     * @throws FileLoadParseException If there's an issue interpreting the user input file load info
     */
    public static List<ICommandFileLoad> createFileLoadsFromInfo(final ApplicationContext appContext, final List<IFileLoadInfo> inputTriples) throws FileLoadParseException {
        final List<ICommandFileLoad> fileLoads = new ArrayList<>(32);
        for (int i = 0; i < inputTriples.size(); i++) {
            final IFileLoadInfo info = inputTriples.get(i);
            final byte[] inputBytes = new byte[appContext.getBean(CommandProperties.class).getChunkSize()];

            try (FileInputStream in = new FileInputStream(new File(info.getInputFilePath()))) {
                //read in each of the files and split big files into multiple
                //pieces if need be
                int partNumber = 1;
                int bytesRead = in.read(inputBytes);
                while (bytesRead != -1) {

                    final ICommandFileLoad load = appContext.getBean(ICommandObjectFactory.class).createCommandFileLoad();
                    load.setOverwriteFlag(info.isOverwrite());
                    load.setFileType(info.getFileType());
                    load.setInputFileName(info.getInputFilePath());
                    load.setFileName(info.getTargetFilePath());
                    load.setPartNumber(partNumber);
                    load.setFileLoadData(inputBytes, 0, bytesRead);

                    fileLoads.add(load);

                    bytesRead = in.read(inputBytes);
                    if (bytesRead != -1) {
                        partNumber++;
                    }
                    TraceManager.getDefaultTracer(appContext).info("Chunking command load file to " + load.getFileByteLength() + " bytes. ");
                }
            } catch (final IOException ioe) {
                throw new FileLoadParseException("IO error encountered while creating command file loads: " + ioe.getMessage(), ioe);
            }
        }

        //change the names of the target files on the spacecraft file system to be numbered to indicate
        //what part of the file they are (e.g. if "some_file.txt" was too big, it might get uploaded as
        //"some_file.txt1" and "some_file.txt2")
        ICommandFileLoad currentLoad = null;
        ICommandFileLoad nextLoad = null;
        for (int j = 0; j < fileLoads.size(); j++) {
            currentLoad = fileLoads.get(j);
            nextLoad = ((j + 1) >= fileLoads.size()) ? null : fileLoads.get(j + 1);

            if (currentLoad.getPartNumber() > 1 || (nextLoad != null && nextLoad.getPartNumber() > 1)) {
                final int partNumber = currentLoad.getPartNumber();
                currentLoad.setPartialFileLoad(true);
                String targetFile = currentLoad.getFileName();
                if (targetFile.length() == ICommandFileLoad.MAX_FILE_NAME_BYTE_SIZE) {
                    targetFile = targetFile.substring(0, targetFile.length() - 2);
                }

                currentLoad.setFileName(targetFile + partNumber);
            }
        }

        return (fileLoads);
    }

    /**
     * Parse the file input triples (type,input,output) from the command line. Note that the input may either contain
     * input/output file (2 parts) or input/output/type (3 parts).
     *
     * @param appContext    the ApplicationContext in which this object is being used
     * @param fileTriples   An array of String where each string is a single command line triple in the form (type,input,output)
     * @param overwriteFlag true if file should overwrite any previous file onboard, false if not
     * @throws FileLoadParseException If there's an issue interpreting the user input file load info
     * @returns a List of FileLoadInfo objects that represent the data in the provided fileTriples
     */
    public static List<IFileLoadInfo> readFileInputTriples(final ApplicationContext appContext, final String[] fileTriples, final boolean overwriteFlag) throws FileLoadParseException {
        if (fileTriples == null) {
            throw new IllegalArgumentException("Null input array");
        } else if (fileTriples.length == 0) {
            throw new IllegalArgumentException("No input files were given");
        }

        final List<IFileLoadInfo> inputTriples = new ArrayList<IFileLoadInfo>();

        for (int i = 0; i < fileTriples.length; i++) {
            byte fileType = IFileLoadInfo.DEFAULT_FILE_TYPE;
            String inFile = null;
            String outFile = null;

            final String[] inputParts = fileTriples[i].split(",{1}");
            if (inputParts.length == 2) {
                inFile = inputParts[0];
                outFile = inputParts[1];
            } else if (inputParts.length == 3) {
                fileType = Byte.parseByte(inputParts[0]);
                inFile = inputParts[1];
                outFile = inputParts[2];
            } else {
                throw new FileLoadParseException("Input segment " + fileTriples[i] + " should either have 2 or 3 comma-separated inputs");
            }

            final IFileLoadInfo info = appContext.getBean(ICommandObjectFactory.class).createFileLoadInfo(fileType, inFile, outFile, overwriteFlag);
            inputTriples.add(info);
        }

        return (inputTriples);
    }
}
