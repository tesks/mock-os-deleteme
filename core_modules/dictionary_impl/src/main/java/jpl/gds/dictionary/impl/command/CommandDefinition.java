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
package jpl.gds.dictionary.impl.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jpl.gds.dictionary.api.Categories;
import jpl.gds.dictionary.api.KeyValueAttributes;
import jpl.gds.dictionary.api.OpcodeUtil;
import jpl.gds.dictionary.api.command.CommandDefinitionFactory;
import jpl.gds.dictionary.api.command.CommandDefinitionType;
import jpl.gds.dictionary.api.command.ICommandArgumentDefinition;
import jpl.gds.dictionary.api.command.ICommandDefinition;


/**
 * This is the implementation object for the command dictionary
 * ICommandDefinition interface. This class holds all of the dictionary
 * configuration for a command argument.
 * <p>
 * Instances of this class should never be created directly. Use
 * CommandDefinitionFactory.
 * 
 *
 * @see CommandDefinitionFactory
 * 
 *
 */
public class CommandDefinition implements ICommandDefinition {

    /**
     * The type of the command.
     */
    private final CommandDefinitionType commandType;

    /**
     * The command stem (unique)
     */
    private String stem;

    /**
     * The command opcode (unique) (stored as a hex string)
     */
    private String opcode = ICommandDefinition.NULL_OPCODE_VALUE;

    /**
     * The command class 
     */
    private String commandClass;

    /**
     * The FSW module that this command is associated with
     */
    private String module;

    /**
     * A description of this command.
     */
    private String description;
    
    /**
     *  Category map to hold category name and category value for a command.
     *  
     */    
    public Categories categories = new Categories();

    /**
	 *  Key-value attribute map to hold the keyword name and value of any project-
	 *  specific information.
	 *  
	 */	
	private KeyValueAttributes keyValueAttr = new KeyValueAttributes();

    /**
     * The command's argument definitions
     */
    private final List<ICommandArgumentDefinition> arguments = new ArrayList<ICommandArgumentDefinition>(16);

    /**
     * Creates an instance of CommandDefinition for the given command type.
     * 
     * @param type
     *            the CommandDefinitionType for this command object
     * 
     */
    CommandDefinition(final CommandDefinitionType type) {

        if (type == null || type == CommandDefinitionType.UNDEFINED || type == CommandDefinitionType.SSE) {
            throw new IllegalArgumentException("Command type may not be null, UNDEFINED, or SSE");
        }

        this.commandType = type;
    }


    @Override
    public CommandDefinitionType getType() {

        return this.commandType;
    }


    @Override
    public String getStem() {

        return this.stem;
    }


    @Override
    public void setStem(final String stem) {

        if (stem == null) {
            throw new IllegalArgumentException("Null input stem");
        }

        this.stem = stem;
    }


    @Override
    @Deprecated /*  Replaced with ICategorySupport. */
    public String getModule() {

        return this.module;
    }


    @Override
    @Deprecated /*  Replaced with ICategorySupport. */
    public void setModule(final String module) {

        this.module = module;
    }


    @Override
    public String getDescription() {

        return this.description;
    }


    @Override
    public void setDescription(final String description) {

        this.description = description;
    }


    @Override
    public String getOpcode() {

        return this.opcode.toLowerCase();
    }



    @Override
    public void setOpcode(final String val, int opcodeBitLen) {
        this.opcode = OpcodeUtil.checkAndConvertOpcode(
                          val,
                          ICommandDefinition.NULL_OPCODE_VALUE,
                          opcodeBitLen);
    }



    @Override
    public void clearArguments() {

        this.arguments.clear();
    }


    @Override
    public List<ICommandArgumentDefinition> getArguments() {

        return Collections.unmodifiableList(this.arguments);
    }


    @Override
    public void addArgument(final ICommandArgumentDefinition aca) {

        if (aca == null) {
            throw new IllegalArgumentException("Null input command argument");
        }

        this.arguments.add(aca);
    }

	@Override
	
	public void setKeyValueAttribute(String key, String value) {
		keyValueAttr.setKeyValue(key, value);
	}
	


	@Override
	
	public String getKeyValueAttribute(String key) {
		return keyValueAttr.getValueForKey(key);
	}
	

	@Override
	
	public KeyValueAttributes getKeyValueAttributes() {
		return keyValueAttr;
	}
	


	@Override	
	public void setKeyValueAttributes(KeyValueAttributes kvAttr) {
		keyValueAttr.copyFrom(kvAttr);
	}


	@Override
	public void clearKeyValue() {
		keyValueAttr.clearKeyValue();		
	}


    @Override
    public void setCategories(Categories map) {
        categories.copyFrom(map);
    }


    @Override
    public Categories getCategories() {
        return categories;
    }


    @Override
    public void setCategory(String catName, String catValue) {
        categories.setCategory(catName, catValue);        
    }


    @Override
    public String getCategory(String name) {
        return categories.getCategory(name);
    }


    @Override
    public String getCommandClass() {
           return this.commandClass;
    }


    @Override
    public void setCommandClass(String commandClass) {
        this.commandClass = commandClass;
    }

}
