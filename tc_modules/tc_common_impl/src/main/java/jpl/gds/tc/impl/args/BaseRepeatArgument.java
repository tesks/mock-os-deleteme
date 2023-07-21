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
package jpl.gds.tc.impl.args;

import jpl.gds.dictionary.api.command.CommandArgumentType;
import jpl.gds.dictionary.api.command.ICommandArgumentDefinition;
import jpl.gds.dictionary.api.command.ICommandEnumerationValue;
import jpl.gds.dictionary.api.command.IRepeatCommandArgumentDefinition;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.tc.api.command.ICommand;
import jpl.gds.tc.api.command.args.ICommandArgument;
import jpl.gds.tc.api.command.args.IEnumeratedCommandArgument;
import jpl.gds.tc.api.command.args.IRepeatCommandArgument;
import jpl.gds.tc.api.command.args.ITimeArgument;
import jpl.gds.tc.api.command.parser.UplinkInputParser;
import jpl.gds.tc.api.exception.ArgumentParseException;
import jpl.gds.tc.api.exception.CommandParseException;

import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * This class is the base implementation of repeat/variable array arguments.
 * <p>
 * A repeat argument (a.k.a. variable length array argument) is a special type
 * of argument that can contain other arguments that may repeat several times.
 * Repeat arguments are useful for scenarios where distinct sets of information
 * must be sent and varying numbers of these values may be transmitted each
 * time. For example, pretend that a particular command accepts a set of
 * quaternions as its values. The repeat argument itself might contain four
 * floating point arguments (one per coordinate: [x, y, z, w]). If the user
 * entered 5 quaternions, then the repeat argument itself when packaged into
 * binary would have the value 5 to indicate the number of quaternions
 * transmitted and then all 5 quaternions would also be packaged as 4 floating
 * point arguments each.
 * <p>
 * Repeat arguments may not contain other repeat arguments.
 * <p>
 * When repeat arguments are transmitted, the first thing transmitted is an
 * unsigned integer indicating how many times the argument block is repeated. It
 * is important to note that the "argumentValue" member of this class IS this
 * number of repeats. it is for this reason that this class inherits from
 * AbstractNumericArgument, which albeit seems a bit strange.
 * <p>
 * The actual arguments are called "valued arguments" and are attached as a
 * list.
 * 
 *
 * 11/8/13 - MPCS-5521. Some methods and members moved to
 *          superclass. General cleanup, javadoc,static analysis changes.
 * 6/22/14 - MPCS-6304. Dictionary-related members throughout
 *          removed to CommandArgumentDefinition class. This class has been
 *          stripped down to support runtime commanding capabilities only.
 *  07/01/19 MPCS-10745 - removed toBitString, parseFromBitString, and getArgumentBitString added
 *          setArgumentValues
 *  09/08/19 MPCS-11080 - Updated getString(bool) to add ability to return string with
 *          time arg in CTS compatible format
 */
public class BaseRepeatArgument extends AbstractNumericArgument implements IRepeatCommandArgument {
    /**
     * These are the internal arguments of this repeat argument with values
     * attached to them. This list will always have a length equal to (for a
     * repeat count of 1) to the length list of dictionary arguments. For larger
     * repeat counts, the length will be an even multiple of the dictionary
     * argument length.
     */
    private List<ICommandArgument> valuedArguments;

    private final IRepeatCommandArgumentDefinition localDef;
    
    private CommandArgumentFactory argFactory;

    /**
     * Creates an instance of BaseRepeatArgument.
     * 
     * @param appContext the ApplicationContext that in which this object is being used
     * 
     * @param def
     *            the command argument definition object for this argument.
     * 
     */
    public BaseRepeatArgument(final ApplicationContext appContext, final IRepeatCommandArgumentDefinition def) {

        super(appContext, def);
        
        this.argFactory = new CommandArgumentFactory(appContext);
        
        this.localDef = def;

        this.valuedArguments = new ArrayList<>(8);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overrides the base class implementation, so an
     * IRepeatCommandArgumentDefinition can be returned.
     * 
     * @see jpl.gds.tc.impl.args.AbstractCommandArgument#getDefinition()
     */
    @Override
    public IRepeatCommandArgumentDefinition getDefinition() {

        return this.localDef;
    }

    @Override
    public void clearArgumentValue() {

        super.clearArgumentValue();
        this.valuedArguments.clear();
    }

    /**
     * Mutator for the vector of valued internal arguments
     * 
     * @param valuedArguments
     *            The new vector of valued internal arguments (the length of
     *            this vector should be the length of the dictionary argument
     *            vector multiplied by the number of repeats)
     */
    protected void setValuedArguments(final List<ICommandArgument> valuedArguments) {

        if (valuedArguments == null) {
            throw new IllegalArgumentException("Null input vector");
        }

        this.valuedArguments = valuedArguments;
    }
    
    private void addValuedArgument(final ICommandArgument ca, final int index) {

        if (ca == null) {
            throw new IllegalArgumentException("Null input argument");
        } else if ((ca.getDefinition().getType() == CommandArgumentType.REPEAT)) {
            throw new IllegalArgumentException(
                    "Cannot add other repeat arguments inside a repeat argument");
        }

    	this.valuedArguments.add(index, ca);
    }
    
    private void addValuedArgument(final ICommandArgumentDefinition def, final int index) {
    	addValuedArgument(argFactory.create(def), index);
    }
    
    public void addRepeatArgumentSet(int index){
    	int argsCount = getDefinition().getDictionaryArgumentCount(false);
    	if((index % argsCount) != 0) {
    		throw new IllegalArgumentException("Cannot insert a set of arguments in the middle of another set. Provided:" + index + " - allowedInterval: " + argsCount);
    	}
    	for(int i = 0 ; i < argsCount ; i++) {
    		addValuedArgument(getDefinition().getDictionaryArguments().get(i), index + i);
    	}
    }
    
    protected void removeValuedArgument(final ICommandArgumentDefinition def, final int index) {
    	if (def == null) {
            throw new IllegalArgumentException("Null input argument");
        } else if (index < 0 || index >= valuedArguments.size()) {
            throw new IllegalArgumentException(
                    "index " + index + " is invalid for this argument");
        } else if(!valuedArguments.get(index).getDefinition().getDictionaryName().equals(def.getDictionaryName())) {
        	throw new IllegalArgumentException("Argument to be deleted is not of the expected type. " + def.getDictionaryName() + " / " + valuedArguments.get(index).getDefinition().getDictionaryName());
        }
    	
    	this.valuedArguments.remove(index);
    }
    
    public void removeRepeatArgumentSet(int index) {
    	int argsCount = getDefinition().getDictionaryArgumentCount(false);
    	if((index % argsCount) != 0) {
    		throw new IllegalArgumentException("Cannot remove a set of arguments starting in the middle of a set. Provided:" + index + " - allowedInterval: " + argsCount);
    	}
    	
    	for(int i = 0 ; i < argsCount ; i++) {
    		removeValuedArgument(getDefinition().getDictionaryArguments().get(i), index);
    	}
    }

    public void removeAllRepeatArguments() {
        this.valuedArguments.clear();
    }

    /**
     * Return the number of valued arguments input for this repeat arg. This
     * value is approximately (number of repeats)*getDictionaryArgumentCount.
     * 
     * @param ignoreFillArguments
     *            False if fill arguments should be included in the count, true
     *            otherwise
     * 
     * @return The integer number of valued arguments
     */
    @Override
    public int getValuedArgumentCount(final boolean ignoreFillArguments) {

        if (!ignoreFillArguments) {
            return (this.valuedArguments.size());
        }

        int count = 0;
        for (ICommandArgument arg : valuedArguments) {
            if (!arg.getDefinition().getType().isFill()) {
                count++;
            }
        }

        return (count);
    }

    @Override
    public String getArgumentString() {
        return getString(false);
    }

    @Override
    public String getUplinkString() {
        return getString(true);
    }

    private String getString(final boolean uplink) {
        /*
         * 06/02/2011 MPCS-2250: Command Builder incorrectly builds
         * MOT_EXER_TIMED commands with no mot_spec array If there is nothing in
         * the BaseRepeatArgument, return zero indicating that fact.
         */
        /*
         * 6/17/20 MPCS-11823: Post-CTS integration update
         * "Empty repeat_arg groups are specified to CTS as an empty pair of parentheses `()`" - MPSA
         * Change from return "0" to return "()"
         *
         * 6/30/20 MPCS-11844: Post-bugfix update
         * Add check for uplink boolean to determine if we should return "()" or "0".
         * When true, we're "uplinking" and passing to CTS
         * when false, we're storing in our DB and need to keep legacy format
         * See JIRA for more details
         */
        if (argumentValue == null || this.valuedArguments.isEmpty()) {
            return uplink ? "()" : "0";
        }

        StringBuilder argString = new StringBuilder(1024);


        if(uplink) {
            argString.append('(');
        } else {
            argString.append(getArgumentValue()).append(',');
        }

        Iterator<ICommandArgument> argIter = valuedArguments.iterator();

        while(argIter.hasNext()) {
            ICommandArgument arg = argIter.next();
            /*
             * 1/13/14 - MPCS-5613. Remove support for nested repeat args.
             */
            CommandArgumentType type = arg.getDefinition().getType();
            if (type.isTime()) {
                final ITimeArgument timeArg = (ITimeArgument) arg;
                argString.append(uplink ? timeArg.getUplinkValue() : timeArg.getArgumentValue());
            }else if (type.isNumeric() || type.isFill()) {
                argString.append(arg.getArgumentValue());
            } else if (type.isEnumeration()) {
                BaseEnumeratedArgument enumArg = (BaseEnumeratedArgument) arg;
                String value = enumArg.getArgumentEnumValue() != null ? enumArg
                        .getArgumentEnumValue().getDictionaryValue() : enumArg
                        .getArgumentValue();
                argString.append(value);
            } else if (type.isString()) {
                String value = arg.getArgumentValue();
                argString.append('"').append(value).append('"');
            }

            if(argIter.hasNext()) {
                argString.append(',');
            }
        }
        if(uplink) {
            argString.append(')');
        }

        return (argString.toString());
    }

    @Override
    public String toString() {

        return getArgumentString();
    }

    @Override
    public ICommandArgument copy() {

        final BaseRepeatArgument ra = new BaseRepeatArgument(appContext, this.localDef);
        setSharedValues(ra);
        return (ra);
    }

    /**
     * Set all of the values of this argument onto the input argument
     * 
     * @param ra
     *            The argument whose values should be set to the values of this
     *            argument
     */
    protected void setSharedValues(final BaseRepeatArgument ra) {

        super.setSharedValues(ra);

        ra.valuedArguments.clear();
        for (final ICommandArgument arg : this.valuedArguments) {
            ra.valuedArguments.add(arg.copy());
        }
    }

    @Override
    public Number formatValueAsComparable(final String inputVal)
            throws ArgumentParseException {

        int value;
        try {
            value = GDR.parse_int(inputVal);
        } catch (final Exception e) {
            throw new ArgumentParseException("Could not interpret input value "
                    + inputVal, e);
        }

        if (value < 0) {
            throw new ArgumentParseException("The input string " + inputVal
                    + " is an invalid value for a repeat argument"
                    + " because it is negative.");
        }

        try {
            return value;
        } catch (final Exception e) {
            throw new ArgumentParseException("Could not interpret input value "
                    + inputVal, e);
        }
    }

    @Override
    public boolean isValueTransmittable() {


        if (!super.isValueTransmittable()) {
            return (false);
        }

        if(this.valuedArguments == null || this.valuedArguments.isEmpty()) {
        	return true;
        }

        int repeatSize = getDefinition().getDictionaryArgumentCount(false);
        
        // loop through all the arguments and check each
        for(int i = 0; i < this.valuedArguments.size() ; i++) {
        	if(!(valuedArguments.get(i).isValueTransmittable() && valuedArguments.get(i).getDefinition().equals(getDefinition().getDictionaryArguments().get(i%repeatSize)))) {
        		return false;
            }
        }

        return true;
    }

    @Override
    public boolean isValueValid() {

    	if (!super.isValueValid()) {
            return (false);
        }

    	int repeatSize = getDefinition().getDictionaryArgumentCount(false);

        int repeatCount = Integer.parseInt(argumentValue);
        int argCount = this.getValuedArgumentCount(false); 

        if( (argCount%repeatSize != 0) || (argCount/repeatSize != repeatCount) ){
        	return false;
        }

        if(this.valuedArguments == null || this.valuedArguments.isEmpty()) {
        	return true;
        }

        for(final ICommandArgument subArg : this.valuedArguments) {
        	if(!subArg.isValueValid()) {
        		return false;
        }
        }

        return (true);
    }

    @Override
    public void setArgumentValue(int index, String value) {
        getArgument(index).setArgumentValue(value);
    }

    public void setArgumentValues(String valueString) {
        String[] values;

        try {
            values = UplinkInputParser.splitCommandString(valueString, BaseRepeatArgument.SEPARATOR_STRING);
        } catch (CommandParseException e) {
           return;
        }

        this.clearArgumentValue();
        this.removeAllRepeatArguments();

        this.setArgumentValue(values[0]);

        String[] repeatValues = Arrays.copyOfRange(values, 1, values.length);

        while(this.getValuedArgumentCount(false) < repeatValues.length) {
            this.addRepeatArgumentSet(0);
        }

        for(int i = 0 ; i < repeatValues.length ; i++) {
            this.setArgumentValue(i, repeatValues[i]);
        }

    }

    @Override
    public ICommandArgumentDefinition getArgumentDefinition(int index) {
        return getArgument(index).getDefinition();
    }

    @Override
    public String getArgumentValue(int index) {
        return getArgument(index).getArgumentValue();
    }

    @Override
    public ICommandEnumerationValue getArgumentEnumValue(int index) {
        if(!getArgument(index).getDefinition().getType().isEnumeration()){
            throw new IllegalArgumentException("Selected command repeat argument is not an enumeration");
        }

        return ((IEnumeratedCommandArgument)getArgument(index)).getArgumentEnumValue();
    }

    @Override
    public void clearArgumentValue(int index) {
        getArgument(index).clearArgumentValue();
        }

    @Override
    public boolean isArgumentValueValid(int index) {
        return getArgument(index).isValueValid();
    }

    @Override
    public boolean isArgumentValueTransmittable(int index) {
        return getArgument(index).isValueTransmittable();
    }

    @Override
    public String getArgumentDisplayName(int index) {
        return getArgument(index).getDisplayName();
    }
    
    
    /*protected*/ ICommandArgument getArgument(int index) {
        if(index >= this.valuedArguments.size()){
            throw new IllegalArgumentException("Index " + index + " does not exist in this repeat argument. Currently there are " + this.valuedArguments.size() + " arguments");
        }
        return this.valuedArguments.get(index);
    }

}
