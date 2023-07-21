package jpl.gds.tc.impl.args;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.command.CommandArgumentDefinitionFactory;
import jpl.gds.dictionary.api.command.CommandArgumentType;
import jpl.gds.dictionary.api.command.ICommandArgumentDefinition;
import jpl.gds.dictionary.api.command.ICommandEnumerationValue;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.command.args.ICommandArgument;
import jpl.gds.tc.api.command.args.IEnumeratedCommandArgument;
import jpl.gds.tc.api.command.args.IRepeatCommandArgument;
import jpl.gds.tc.api.exception.BlockException;
import jpl.gds.tc.api.exception.UnblockException;

/**
 * This class is the base implementation for all bitmask (conglomerate) arguments.
 * 
 * A bitmask argument is similar to an enumerated argument due to the fact that allowed values are
 * enumeration restricted to a specific list of known values. The number of enumerations that are allowed
 * are not restricted, however, every bit in the value must be specified by one of the enumerations.
 * 
 * One or more enumeration values can be specified. When more than one are specified the values are combined
 * (bitwise) to create a single value for transmission.
 * 
 *
 * MPCS-10745 - 07/01/19 - removed toBitString, parseFromBitString, parseAndSetArgumentValueFromBitString,
 * and getArgumentBitString
 *
 */
public class BaseBitmaskArgument extends AbstractCommandArgument implements IRepeatCommandArgument {

    /** This string is to be used to separate the values when multiple are specified on a single line */
    public static final String SEPARATOR_STRING = "|";
    
    private List<IEnumeratedCommandArgument> compositeValues;
   
    /**
     * Creates an instance of BaseBitmaskArgument
     * 
     * @param def the command argument definition object for this argument.
     */
    public BaseBitmaskArgument(final ApplicationContext appContext, ICommandArgumentDefinition def) {
        super(appContext, def);
        
        compositeValues = new ArrayList<>();
    }

    @Override
    public ICommandArgument copy() {
        BaseBitmaskArgument ba = new BaseBitmaskArgument(this.appContext, this.getDefinition());
        setSharedValues(ba);
        return ba;
    }
    
    protected void setSharedValues(final BaseBitmaskArgument ba) {
        super.setSharedValues(ba);
        
        ba.compositeValues.clear();
        for(IEnumeratedCommandArgument arg : this.compositeValues) {
            ba.compositeValues.add((IEnumeratedCommandArgument) arg.copy());
        }
    }

    @Override
    public boolean isValueValid() {
        // MPCS-10294 10/08/18 - updated to check that at least one value is selected.
        // Shouldn't be valid if nothing's selected.
        if(compositeValues == null || compositeValues.isEmpty()) {
            return false;
        }
        for(IEnumeratedCommandArgument arg : compositeValues) {
            if(!arg.isValueValid()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isValueTransmittable() {
        for(IEnumeratedCommandArgument arg : compositeValues) {
            if(!arg.isValueTransmittable()) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public String getSeparatorString() {
        return SEPARATOR_STRING;
    }
    
    @Override
    public void setArgumentValue(String value) {
        super.setArgumentValue(value);
        
        compositeValues.clear();
        
        if(value == null || value.trim().isEmpty()) {
            return;
        }
        
        if(value.indexOf(SEPARATOR_STRING) != -1) {
            setArgumentValues(value);
        }
        else { //only a single number
            this.setMasksSingleValue(value);
        }
        
        if(!value.equals(getArgumentString())) {
            super.setArgumentValue(getArgumentString());
        }
        
    }

    @Override
    public void setArgumentValues(String valueString) {
        compositeValues.clear();

        String[] args = valueString.split("\\" + SEPARATOR_STRING);
        for(String val : args) {
            val = val.trim();
            if(val.isEmpty()) {
                continue;
            }
            this.addRepeatArgumentSet(compositeValues.size());
            setArgumentValue(compositeValues.size() - 1, val);
        }

        if(!valueString.equals(getArgumentString())) {
            super.setArgumentValue(getArgumentString());
        }
    }

    @Override
    public void setArgumentValue(int index, String value) {
        compositeValues.get(index).setArgumentValue(value);
        
        //let's change it to the enumeration, so it's easier to read
        ICommandEnumerationValue argVal = this.getArgumentEnumValue(index);
        if(argVal != null) {
            compositeValues.get(index).setArgumentValue(argVal.getDictionaryValue());
        }
    }

    @Override
    public ICommandArgumentDefinition getArgumentDefinition(int index) {
        return compositeValues.get(index).getDefinition();
    }

    @Override
    public String getArgumentValue(int index) {
        return compositeValues.get(index).getArgumentValue();
    }

    @Override
    public ICommandEnumerationValue getArgumentEnumValue(int index) {
        return compositeValues.get(index).getArgumentEnumValue();
    }

    @Override
    public void clearArgumentValue(int index) {
        compositeValues.get(index).clearArgumentValue();
    }

    @Override
    public void addRepeatArgumentSet(int index) {
        IEnumeratedCommandArgument tmp = new BaseEnumeratedArgument(this.appContext, getDefinitionCopy());
        compositeValues.add(index, tmp);
    }

    @Override
    public void removeRepeatArgumentSet(int index) {
        compositeValues.remove(index);
    }

    @Override
    public int getValuedArgumentCount(boolean ignoreFillArguments) {
        return compositeValues.size();
    }

    @Override
    public String getArgumentString() {
        StringBuilder retVal = new StringBuilder();
        if(!compositeValues.isEmpty()) {
            for(IEnumeratedCommandArgument val : compositeValues) {
                if(!val.getArgumentValue().isEmpty()) {
                    retVal.append(val.getArgumentValue())
                    .append(" ")
                    .append(SEPARATOR_STRING)
                    .append(" ");
                }
            }
            if (retVal.toString().endsWith(" " + SEPARATOR_STRING + " ")) {
                return retVal.toString().substring(0, retVal.length() - 3);
            }
        }
        
        return retVal.toString();
    }

    @Override
    public String getUplinkString() {
        return getArgumentString();
    }

    @Override
    public boolean isArgumentValueValid(int index) {
        return compositeValues.get(index).isValueValid();
    }

    @Override
    public boolean isArgumentValueTransmittable(int index) {
        return compositeValues.get(index).isValueTransmittable();
    }

    @Override
    public String getArgumentDisplayName(int index) {
        return compositeValues.get(index).getDisplayName();
    }
    
    private ICommandArgumentDefinition getDefinitionCopy() {
        ICommandArgumentDefinition copy = CommandArgumentDefinitionFactory.create(CommandArgumentType.UNSIGNED_ENUMERATION);
        copy.setBitLength(this.getDefinition().getBitLength());
        copy.setDescription(this.getDefinition().getDescription());
        copy.setDictionaryName(this.getDefinition().getDictionaryName());
        copy.setEnumeration(this.getDefinition().getEnumeration());
        copy.setFswName(this.getDefinition().getFswName());
        copy.setUnits(this.getDefinition().getUnits());
        
        return copy;
    }
    
    private void setMasksSingleValue(String value) {
        compositeValues.clear();
        
        try {
            Long val = Long.valueOf(value);
            for(ICommandEnumerationValue enumVal : getDefinition().getEnumeration().getEnumerationValues()) {
                long enumerationValue = Long.valueOf(enumVal.getBitValue());
                if( (enumerationValue & val) == enumerationValue) {
                    int newValSlot = compositeValues.size();
                    addRepeatArgumentSet(newValSlot);
                    setArgumentValue(newValSlot, enumVal.getBitValue());
                }
            }
        }
        catch (NumberFormatException e) {
            addRepeatArgumentSet(0);
            setArgumentValue(0, value);
        }
    }
}
