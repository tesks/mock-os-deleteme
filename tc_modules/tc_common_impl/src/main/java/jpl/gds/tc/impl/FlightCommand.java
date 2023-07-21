
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
package jpl.gds.tc.impl;

import jpl.gds.dictionary.api.OpcodeUtil;
import jpl.gds.dictionary.api.command.*;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.tc.api.command.IFlightCommand;
import jpl.gds.tc.api.command.args.ICommandArgument;
import jpl.gds.tc.api.command.args.IEnumeratedCommandArgument;
import jpl.gds.tc.api.command.args.IRepeatCommandArgument;
import jpl.gds.tc.api.command.args.ITimeArgument;
import jpl.gds.tc.api.exception.CommandParseException;
import jpl.gds.tc.impl.args.CommandArgumentFactory;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * This class is used to represent a spacecraft command for runtime processing.
 * Dictionary attributes of the command are kept in a separate
 * ICommandDefinition object which is accessible through this class. This class
 * can be used for three types of commands: flight software commands, hardware
 * commands, and sequence directives, all three of which are considered flight
 * commands.
 * 
 *
 * 11/5/13 - MPCS-5521. Correct static analysis and javadoc
 *          issues.
 * 6/22/14 - MPCS-6304. No longer implements ICommandDefinition.
 *          Dictionary-related members throughout removed to the
 *          CommandDefinition class. This class has been stripped down to
 *          support runtime commanding capabilities only. Also, no longer
 *          abstract.
 * 03/06/19 - MPCS-10587. Major refactor. Remove ability to get
 *          anything that extends ICommandArgument. All argument definitions,
 *          values, and properties are retrieved and set through the 
 *          FlightCommand that owns it.
 *          Removed all parseAndSet functions,
 *          except parseAndSetArgumentValueFromBitString (necessary as part
 *          of BinaryRepresentable interface). Replaced isValueValid with
 *          isArguemntValueValid and isTransmittable with
 *          isArguemntValueTransmittable. These new functions do not take
 *          a value as an argument. They evaluate the current argument's
 *          value.
 * 07/02/19 - MPCS-10745 - removed functions to get or set arguments as bit string, removed
 *          getOpcodeBits and toBitString
 *
 *          
 */
public class FlightCommand implements IFlightCommand {

    /**
     * The command's arguments. The length of this list should generally match
     * the length of the list of command argument definitions attached to the
     * ICommandDefinition object.
     */
    private final List<ICommandArgument> arguments;

    /**
     * The dictionary definition of the command.
     */
    private ICommandDefinition definition;

    /**
     * The actual opcode supplied by the user. This will nearly always match the
     * opcode in the definition object, but since we allow faults to be injected
     * into the opcode, we have to keep a separate copy here.
     */
    private String enteredOpcode;
    
    private final ApplicationContext appContext;
    private final DictionaryProperties dictConfig;

    /**
     * Constructor used locally.
     *
     * This constructor should not be used, even within this class.
     * Use the CommandObjectFactory instead. If you feel you really must
     * use it, justify it and get permission.
     *
     * It's protected so that CommandObjectFactory can use it, but not
     * everybody. That's only partial protection, hence these comments.
     *
     * @param def
     *            the dictionary definition object for this command object
     * 
     * @param appContext App context
     */
    protected FlightCommand(final ApplicationContext appContext, final ICommandDefinition def) {

        if (def == null) {
            throw new IllegalArgumentException(
                    "Command definition may not be null");
        }
        
        this.appContext = appContext;
        this.dictConfig = appContext.getBean(DictionaryProperties.class);

        this.definition = def;

        /*
         * 6/23/14 - MPCS-6304. Since the definition information has been
         * split from the runtime information, we must create actual runtime
         * argument objects to match the dictionary arguments.
         */
        final CommandArgumentFactory argFactory = new CommandArgumentFactory(appContext);
        final List<ICommandArgumentDefinition> definedArguments = def.getArguments();
        this.arguments = new ArrayList<>(
                definedArguments.size());
        for (final ICommandArgumentDefinition argDef : definedArguments) {
            this.arguments.add(argFactory.create(argDef));
        }

        /*
         * 6/23/14 - MPCS-6304. Initialize the local opcode to that in the
         * definition.
         */
        this.enteredOpcode = this.definition.getOpcode();
    }

    @Override
    public ApplicationContext getApplicationContext() {
    	return this.appContext;
    }

    @Override
    public ICommandDefinition getDefinition() {

        return definition;
    }

    @Override
    public void setDefinition(final ICommandDefinition definition) {

        this.definition = definition;
    }

    @Override
    public void clearArgumentValues() {

        for (final ICommandArgument arg : this.arguments) {
            arg.clearArgumentValue();
        }
    }
    
    @Override
    public void clearArgumentValue(final int index) {
    	getArgument(index).clearArgumentValue();
    }
    
    @Override
    public void clearArgumentValue(final int index, final int subIndex) {
    	getRepeatArgument(index).clearArgumentValue(subIndex);
    }

    // MPCS-9596 - 04/12/18 - removed getArguments(), added other getArgument functions
    
    /**
     * Get a single command argument 
     * @param index the index of the argument to be retrieved
     * @return the relevant ICommandArgument
     * 05/14/18 - MPCS-9720 - adjusted scope to protected to allow child classes
     *          , M20ParameterCommand and the like, access. 
     */
    protected final ICommandArgument getArgument(final int index) {
    	if(index >= this.arguments.size()){
    		throw new IllegalArgumentException("Index" + index + " does not exist in this " + this.definition.getStem() + ". Maximum argument index is " + (this.arguments.size()-1));
    	}
    	return this.arguments.get(index);
    }
    
    //gets a command argument as a repeat argument
    /*
     * MPCS-10194 - 09/07/18 - tweaked this to return a repeat on REPEAT or BITMASK.
     *  Bitmask arguments return false on isRepeat, but do implement IRepeatCommandArgument
     */
    private IRepeatCommandArgument getRepeatArgument(final int index) {
    	final ICommandArgument fetched = getArgument(index);
    	
    	if(!(fetched.getDefinition().getType().equals(CommandArgumentType.REPEAT)
    	        || fetched.getDefinition().getType().equals(CommandArgumentType.BITMASK))){
    		throw new IllegalArgumentException("Argument is not a repeated argument");
    	}
    	return (IRepeatCommandArgument) fetched;
    }
    
    @Override
    public int getArgumentCount() {
    	return this.arguments.size();
    }
    
   @Override
    public CommandArgumentType getArgumentType(final int index) {
    	return getArgument(index).getDefinition().getType();
    }
    
    @Override
    public CommandArgumentType getArgumentType(final int index, final int subIndex) {
    	return getRepeatArgument(index).getArgumentDefinition(subIndex).getType();
    }
    
    @Override
    public int getArgumentCount(final int index) {
    	return getRepeatArgument(index).getValuedArgumentCount(false);
    }
    
    @Override
    public ICommandArgumentDefinition getArgumentDefinition(final int index) {
    	return getArgument(index).getDefinition();
    }
    
    @Override
    public ICommandArgumentDefinition getArgumentDefinition(final int index, final int subIndex) {
    	return getRepeatArgument(index).getArgumentDefinition(subIndex);
    }
    
    @Override
    public IRepeatCommandArgumentDefinition getRepeatArgumentDefinition(final int index) {
    	return (IRepeatCommandArgumentDefinition)getRepeatArgument(index).getDefinition();
    }
    
    @Override
    public void setArgumentValue(final int index, final String value){
    	getArgument(index).setArgumentValue(value);
    }
    
    @Override
    public void setArgumentValue(final int index, final int subIndex, final String value){
    	getRepeatArgument(index).setArgumentValue(subIndex, value);
    }

    @Override
    public void setArgumentValues(final int index, final String values) {
        getRepeatArgument(index).setArgumentValues(values);
    }
    
    @Override
    public ICommandEnumerationValue getArgumentEnumValue(final int index) {
    	if (!getArgumentType(index).isEnumeration()) {
    		throw new IllegalArgumentException("Selected command argument is not an enumeration");
    	}
    	return ((IEnumeratedCommandArgument)getArgument(index)).getArgumentEnumValue();
    }
    
    @Override
    public ICommandEnumerationValue getArgumentEnumValue(final int index, final int subIndex) {
    	if(!getArgumentDefinition(index, subIndex).getType().isEnumeration()) {
    		throw new IllegalArgumentException("Selected command argument is not an enumeration");
        }
    	return getRepeatArgument(index).getArgumentEnumValue(subIndex);
    }
    
    @Override
    public boolean isArgumentValueValid(final int index) {
    	return getArgument(index).isValueValid();
    }
    
    @Override
    public boolean isArgumentValueValid(final int index, final int subIndex) {
    	return getRepeatArgument(index).isArgumentValueValid(subIndex);
    }
    
    @Override
    public boolean isArgumentValueTransmittable(final int index) {
    	return getArgument(index).isValueTransmittable();
    }
    
    @Override
    public boolean isArgumentValueTransmittable(final int index, final int subIndex) {
    	return getRepeatArgument(index).isArgumentValueTransmittable(subIndex);
    }
    
    @Override
    public boolean isUserEntered(final int index){
    	return getArgument(index).isUserEntered();
    }
    
    @Override
    public String getArgumentValue(final int index) {
    	return getArgument(index).getArgumentValue();
    }
    
    @Override
    public String getArgumentValue(final int index, final int subIndex) {
    	return getRepeatArgument(index).getArgumentValue(subIndex);
    }
    
    
    @Override
    public String getRepeatArgumentString(final int index) {
    	return getRepeatArgument(index).getArgumentString();
    }
    
    @Override
    public String getArgumentDisplayName(final int index) {
    	return getArgument(index).getDisplayName();
    }
    
    @Override
    public String getArgumentDisplayName(final int index, final int subIndex) {
    	return getRepeatArgument(index).getArgumentDisplayName(subIndex);
    }
    
    @Override
    public void addRepeatArguments(final int index, final int subIndex) {
    	getRepeatArgument(index).addRepeatArgumentSet(subIndex);
    }
    
    
    @Override
    public void removeRepeatArguments(final int index, final int subIndex) {
    	getRepeatArgument(index).removeRepeatArgumentSet(subIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTemplateContext(final Map<String, Object> map) {

        if (map == null) {
            throw new IllegalArgumentException("Null input");
        }

        map.put("stem",
                this.definition.getStem() != null ? this.definition.getStem()
                        : "");
        /* MHT - MPCS-7033 - 10/29/15 - New call to categories. */
        map.put("module",
                this.definition.getCategory(ICommandDefinition.MODULE) != null ? this.definition
                        .getCategory(ICommandDefinition.MODULE) : "");
    }

    @Override
    public String getDatabaseString() {

        return getString(false);
    }

    private String getString(final boolean uplink) {
        final StringBuilder dbString = new StringBuilder(1024);

        dbString.append(this.definition.getStem());
        Iterator<ICommandArgument> argIter = arguments.iterator();
        while(argIter.hasNext()) {
            dbString.append(',');
            final ICommandArgument arg = argIter.next();
            final CommandArgumentType type = arg.getDefinition().getType();
            if (type.isRepeat()) {
                final IRepeatCommandArgument repeatArg = (IRepeatCommandArgument) arg;
                dbString.append(uplink ? repeatArg.getUplinkString() : repeatArg.getArgumentString());
            } else if(type.isTime()) {
                final ITimeArgument timeArg = (ITimeArgument) arg;
                dbString.append(uplink ? timeArg.getUplinkValue() : timeArg.getArgumentValue());
            } else if (type.isNumeric() || type.isFill()) {
                dbString.append(arg.getArgumentValue());
            } else if (type.isString()) {
                dbString.append('"').append(arg.getArgumentValue()).append('"');
            } else if (type.isEnumeration()) {
                IEnumeratedCommandArgument enumArg = (IEnumeratedCommandArgument) arg;
                final String value = enumArg.getArgumentEnumValue() != null ? enumArg.getArgumentEnumValue().getDictionaryValue() : enumArg.getArgumentValue();
                        dbString.append(value);
            } else {
                TraceManager.getTracer(Loggers.UPLINK).warn("Encountered the argument type " + type + " that currently cannot be included in a database string. Ignoring!");
            }
        }

        return (dbString.toString());
    }


    /**
     * Deep copies the command and its arguments, excluding the definition, from
     * the input command object.
     * 
     * @param icmd
     *            the command object to copy from
     */
    private void setSharedValues(final FlightCommand icmd) {

        final FlightCommand cmd = icmd;

        cmd.arguments.clear();
        for (final ICommandArgument arg : this.arguments) {
            cmd.arguments.add(arg.copy());
        }
    }

    /*
     * 1/8/14 - MPCS-5622. Removed methods for the
     * ParsedDictionaryXmlElement interface.
     */

    // MPCS-10473 - 04/05/19 - moved static getCommandObjectFromOpcodeBits to CommandObjectFactory where it belongs

    @Override
    public String toString() {
        return (getString(true));
    }

    @Override
    public FlightCommand copy() {

        final FlightCommand cmd = new FlightCommand(appContext, this
                .getDefinition());
        setSharedValues(cmd);
        return (cmd);
    }

    @Override
    public void setEnteredOpcode(final String val) throws CommandParseException {


        try {
            /* MPCS-7434 - 2/3/16. Add opcode bit length argument */
            this.enteredOpcode = OpcodeUtil.checkAndConvertOpcode(val, ICommandDefinition.NULL_OPCODE_VALUE,
                    dictConfig.getOpcodeBitLength());

        } catch (final IllegalArgumentException e) {
            throw new CommandParseException(e.getMessage(), e);
        }
    }

    @Override
    public String getEnteredOpcode() {

        return this.enteredOpcode.toLowerCase();
    }
    
    @Override
    public String getRepeatArgumentSeparator(final int index) {
        return getRepeatArgument(index).getSeparatorString();
    }
}
