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
package jpl.gds.dictionary.api.command;

import java.util.List;

import jpl.gds.dictionary.api.IAttributesSupport;
import jpl.gds.dictionary.api.ICategorySupport;

/**
 * The ICommandDefinition interface is to be implemented by all command
 * definition classes.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * ICommandDefinition defines methods needed to interact with Command Definition
 * objects as required by the ICommandDictionary interface. It is primarily used
 * by command dictionary file parser implementations in conjunction with the
 * CommandDefinitionFactory, which will be used to create actual Command
 * Definition objects in the parsers. ICommandDictionary objects should interact
 * with Command Definition objects only through the Factory and the
 * ICommandDefinition interface. Interaction with the actual Command Definition
 * implementation classes in an ICommandDictionary implementation is contrary to
 * multi-mission development standards.
 *
 * 
 *
 * @see ICommandDictionary
 * @see CommandDefinitionFactory
 * @see ICommandArgumentDefinition
 */
public interface ICommandDefinition extends IAttributesSupport, ICategorySupport {

    /**
     * String constant that represents an undefined opcode.
     */
    public static final String NULL_OPCODE_VALUE = "NULL";

    /**
     * Gets the command opcode, which is always represented as a hex string and
     * is mandatory for all flight commands. If set to the NULL_OPCODE_VALUE
     * constant, the opcode is undefined.
     * <p>
     * It is very important that this value is used to actually initialize the
     * runtime command object, but thereafter, the opcode should be obtained
     * using the getEnteredOpcode() method on the FswCommand object, because the 
     * opcode is modifiable at runtime.
     * <p>
     * In the multimission command dictionary, the opcode is specified using the
     * "opcode" attribute on the &lt;fsw_command&gt; or &lt;hw_command&gt;
     * elements.
     *
     * @return Returns the opcode.
     */
    public String getOpcode();

    /**
     * Sets the command opcode, which is always represented as a hex string and
     * is mandatory for all flight commands. If set to the NULL_OPCODE_VALUE
     * constant, the opcode is undefined. Input opcode value should always be
     * prefixed with 0x or 0b to properly ensure that it gets properly parsed as
     * hex or binary. 
     * <p>
     * It is very important that this value is used to actually initialize the
     * runtime command object, but thereafter, the opcode should be set using
     * the setEnteredOpcode() method on the FswCommand object, because the
     * opcode is modifiable at runtime.
     * <p>
     * In the multimission command dictionary, the opcode is specified using the
     * "opcode" attribute on the &lt;fsw_command&gt; or &lt;hw_command&gt;
     * elements.
     * 
     * @param val
     *            the new opcode, as a hex or binary string; may not be null
     * @param opcodeBitLen the configured bit length for opcodes in the
     *        current mission/dictionary context
     * 
     */
    public void setOpcode(String val, int opcodeBitLen);

    /**
     * Gets the command stem, also known as the mnemonic, which is a human
     * readable string corresponding to an opcode. It is mandatory for all
     * flight commands.
     * <p>
     * In the multimission command dictionary, the stem is specified using the
     * "stem" attribute on the &lt;fsw_command&gt; or &lt;hw_command&gt;
     * elements.
     * 
     * @return Returns the stem; may not be null
     */
    public String getStem();

    /**
     * Sets the command stem, also known as the mnemonic, which is a human
     * readable string corresponding to an opcode. It is mandatory for all
     * flight commands.
     * <p>
     * In the multimission command dictionary, the stem is specified using the
     * "stem" attribute on the &lt;fsw_command&gt; or &lt;hw_command&gt;
     * elements.
     *
     * @param stem	the stem to set; may not be null
     */
    public void setStem(String stem);


    /**
     * Adds an argument definition to the command definition. Hardware commands
     * do not have arguments.
     * <p>
     * In the multimission command dictionary, command arguments are specified
     * using the &lt;arguments&gt; element under the &lt;fsw_command&gt; element.
     * 
     * @param aca
     *            the argument definition to add
     * 
     */
    public void addArgument(ICommandArgumentDefinition aca);

    /**
     * Gets the list of command argument definitions for this command definition.
     * <p>
     * In the multimission command dictionary, command arguments are specified
     * using the &lt;arguments&gt; element under the &lt;fsw_command&gt; element.
     *
     * @return	list of ICommandArgumentDefinition objects; may be empty, but never null
     * 
     */
    public List<ICommandArgumentDefinition> getArguments();

    /**
     * Clears all argument definitions for this command. After this call,
     * getArguments() will return an empty list.
     */
    public void clearArguments();


    /**
     * Gets the module value associated with the command definition object. The
     * module is an optional means for categorizing or grouping commands by
     * subsystem or flight module.
     * <p>
     * In the multimission command dictionary, the module is specified using the
     * &lt;categories&gt; element on the &lt;fsw_command&gt; or
     * &lt;hw_command&gt; elements.
     * 
     * @return module string; may be null
     * @deprecated  Replaced with ICategorySupport.
     */
    @Deprecated 
    public String getModule();

    /**
     * Sets the module value associated with the command definition object. The
     * module is an optional means for categorizing or grouping commands by
     * subsystem or flight module.
     * <p>
     * In the multimission command dictionary, the module is specified using the
     * &lt;categories&gt; element on the &lt;fsw_command&gt; or
     * &lt;hw_command&gt; elements.
     *
     * @param m module name to set; may be null
     * @deprecated  Replaced with ICategorySupport.
     */
    @Deprecated 
    public void setModule(String m);

    /**
     * Gets the description string for the command definition. Description is
     * optional, may be multi-line, and is unlimited in length.
     * <p>
     * In the multimission command dictionary, the module is specified using the
     * &lt;description&gt; element on the &lt;fsw_command&gt; or
     * &lt;hw_command&gt; elements.
     * 
     * @return description string; may be null
     */
    public String getDescription();

    /**
     * Sets the description string for the command definition. Description is
     * optional, may be multi-line, and is unlimited in length.
     * <p>
     * In the multimission command dictionary, the module is specified using the
     * &lt;description&gt; element on the &lt;fsw_command&gt; or
     * &lt;hw_command&gt; elements.
     * 
     * @param desc description string to set; may be null
     * 
     */
    public void setDescription(String desc);

    /**
     * Accessor for the command definition type, which tells us whether
     * this is a flight, simulation, hardware, or sequence directive command
     * type.
     * 
     * @return the CommandDefinitionType enumeration value.
     * 
     */
    public CommandDefinitionType getType();
    
    /**
     * Gets the command class.  It is optional and can be "FSW" or "SEQ".
     * <p>
     * Different command classes may be encoded using different virtual channels, 
     * or have other encoding differences.
     *
     * @return Returns the command class; 
     */
    public String getCommandClass();

    /**
     * Sets the command class, 
     *
     * @param commandClass Either "FSW" or "SEQ"
     */
    public void setCommandClass(String commandClass);

}
