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
package jpl.gds.dictionary.api.evr;

import jpl.gds.dictionary.api.EnumerationDefinition;

/**
 * The IEvrArgumentDefinition interface is to be implemented by all EVR argument
 * definition classes.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * An IEvrArgumentDefinition object is the multi-mission representation of an
 * EVR argument specification, which is used to interpret consumed EVR data and
 * to properly format them as required by the mission. IEvrDictionary
 * implementations must parse mission-specific EVR dictionary files and create
 * IEvrDefinition with attached IEvrArgumentDefinition objects for the
 * definitions found therein. In order to isolate the mission adaptation from
 * changes in the multi-mission core, IEvrDictionary implementations should use
 * the EvrArgumentFactory to create multi-mission IEvrArgumentDefinition objects, and
 * define a mission-specific class that implements the interface. All
 * interaction with these objects in mission adaptations should use the
 * IEvrArgumentDefinition interface, rather than directly interacting with the
 * objects themselves.
 * 
 *
 *
 * @see IEvrDictionary
 * @see IEvrDefinition
 */
public interface IEvrArgumentDefinition {

    /**
     * Gets the enumeration table name associated with the EVR argument.
     * Used only for enumerated arguments, but is mandatory in that case.
     * <p>
     * The enumeration table name attribute of an EVR argument is specified
     * using the "enum_name" attribute on the &lt;enum_arg&gt; element in
     * the multimission EVR dictionary.
     * 
     * @return Enumeration typedef/table name, or null if this is not an
     *         enumerated argument.
     * 
     */
    public abstract String getEnumTableName();

    /**
     * Sets the enumeration table name associated with the EVR argument. Used
     * only for enumerated arguments, but is mandatory in that case.
     * <p>
     * The enumeration table name attribute of an EVR argument is specified
     * using the "enum_name" attribute on the &lt;enum_arg&gt; element in the
     * multimission EVR dictionary.
     * 
     * @param enumTableName
     *            Enumeration typedef/table name; may be null only for
     *            non-enumerated arguments
     * 
     */
    public abstract void setEnumTableName(String enumTableName);

    /**
     * Retrieves the argument number. Argument numbers start at 0.
     * <p>
     * There is no explicit specification of EVR argument number in the multimission
     * EVR dictionary. Argument number is implied by the order in which the arguments
     * are defined in the XML.
     * 
     * @return the argument number
     */
    public abstract int getNumber();

    /**
     * Sets the argument number. Argument numbers start at 0.
     * <p>
     * There is no explicit specification of EVR argument number in the multimission
     * EVR dictionary. Argument number is implied by the order in which the arguments
     * are defined in the XML.
     * 
     * @param num the argument number
     */
    public abstract void setNumber(int num);

    /**
     * Gets the argument name. Argument names consist of letters, numbers,
     * underscores, and dashes and are limited to 64 characters in length.
     * Argument names are required by the multimission EVR schema, but not by
     * all EVR schemas, and are not actually required for EVR processing.
     * <p>
     * The name attribute of an EVR argument is specified using the "name"
     * attribute on the specific argument definition element in the multimission
     * EVR dictionary.
     * 
     * @return the name of the EVR argument; may be null
     */
    public abstract String getName();

    /**
     * Sets the argument name. Argument names consist of letters, numbers,
     * underscores, and dashes and are limited to 64 characters in length.
     * Argument names are required by the multimission EVR schema, but not by
     * all EVR schemas, and are not actually required for EVR processing.
     * <p>
     * The name attribute of an EVR argument is specified using the "name"
     * attribute on the specific argument definition element in the multimission
     * EVR dictionary.
     * 
     * @param name the name to set; may be null
     */
    public abstract void setName(String name);

    /**
     * Gets the argument type enumeration value, defining the data type of the
     * EVR argument. The data type is required.
     * <p>
     * The data type attribute of an EVR argument is specified using the "type"
     * attribute on the &lt;numeric_arg&gt; element in the multimission EVR
     * dictionary for numeric arguments, and is derived from the specific
     * argument definition element for other types of arguments.
     * 
     * @return the type enum value; may not be null
     */
    public abstract EvrArgumentType getType();

    /**
     * Sets the argument type enumeration value, defining the data type
     * of the EVR argument. The data type is required.
     * <p>
     * The data type attribute of an EVR argument is specified using the 
     * "type" attribute on the &lt;numeric_arg&gt; element in the multimission
     * EVR dictionary for numeric arguments, and is derived from the specific
     * argument definition element for other types of arguments.
     * 
     * @param type the type enum value; may not be null
     */
    public abstract void setType(EvrArgumentType type);

    /**
     * Gets the argument length in bytes. Argument length is required.
     * <p>
     * The length attribute of an EVR argument is specified using the
     * "byte_length" attribute on the specific argument definition element in
     * the multimission EVR dictionary.
     * 
     * @return the length in bytes
     */
    public abstract int getLength();

    /**
     * Sets the argument length in bytes. Argument length is required.
     * <p>
     * The length attribute of an EVR argument is specified using the
     * "byte_length" attribute on the specific argument definition element in
     * the multimission EVR dictionary.
     * 
     * @param len the length to set
     */
    public abstract void setLength(int len);

    /**
     * Retrieves the list of enumeration values for this argument. Used only for
     * enumerated arguments, but mandatory in that case.
     * <p>
     * The enumeration table for an EVR argument is defined using an
     * &lt;enum_table&gt; element in the multimission EVR schema, and then
     * linking that enumeration definition to the EVR argument using the
     * enumeration table name.
     * 
     * @return the table of enumeration values for this argument; may be null
     *         only for non-enumerated arguments
     */
    public abstract EnumerationDefinition getEnumeration();

    /**
     * Sets the list of enumerated values for this argument. Used only for
     * enumerated arguments, but mandatory in that case.
     * <p>
     * The enumeration table for an EVR argument is defined using an
     * &lt;enum_table&gt; element in the multimission EVR schema, and then
     * linking that enumeration definition to the EVR argument using the
     * enumeration table name.
     * 
     * @param table
     *            table of enumeration values to set; may be null only for
     *            non-enumerated arguments
     */
    public abstract void setEnumeration(EnumerationDefinition table);

}