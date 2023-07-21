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
import java.util.Map;
import java.util.Set;

import jpl.gds.dictionary.api.IBaseDictionary;
import jpl.gds.shared.annotation.CustomerAccessible;
import jpl.gds.shared.annotation.Mutator;


/**
 * The ICommandDictionary interface is to be implemented by all command
 * dictionary adaptation classes (i.e. parsers). 
 * <p>
 * <p><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <p>
 * <p>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <p>
 * <p>
 * The JPL document corresponding to the multimission command dictionary schema
 * is D-001442, the AMPCS Multimission Command Dictionary SIS, in the JPL MGSS
 * Document Management System (DMS).
 * <p>
 * The Command dictionary is used by uplink applications to formulate and
 * validate spacecraft or SSE commands. A particular command's arguments,
 * format, and requirements are defined in the project's command dictionary.
 * Every mission may have a different format for representing the command
 * dictionary. An appropriate dictionary parser must be used in order to create
 * the mission-specific ICommandDictionary object, which MUST implement this
 * interface. ICommandDictionary objects should only be created via the
 * CommandDictionaryFactory. Direct creation of an ICommandDictionary object is
 * a violation of multi-mission development standards.
 * <p>
 * This interface extends the IBaseDictionary interface, which requires parse()
 * and clear() methods on all dictionary objects. These methods are used to
 * populate and clear the dictionary contents, respectively.
 * <p>
 * The primary job of the ICommandDictionary object is to produce a set of
 * ICommandDefinition objects. Each ICommandDefinition is the multi-mission
 * representation of a defined command in the dictionary.
 * <p>
 *
 *
 * @see ICommandDictionaryFactory
 * @see jpl.gds.dictionary.api.IBaseDictionary
 * @see ICommandDefinition
 */
@CustomerAccessible(immutable = false)
public interface ICommandDictionary extends IBaseDictionary, ICommandDefinitionProvider
{

    /**
     * Retrieves the list of parsed Command Definitions from the
     * ICommandDictionary.
     * 
     * @return	list of ICommandDefinition objects; list may be
     * null or empty if no command definitions exist
     */
    public List<ICommandDefinition> getCommandDefinitions();

    /**
     * Retrieves the map of parsed Command Argument Enumerations from the
     * ICommandDictionary.
     * 
     * @return  Map of CommandEnumerationDefinition objects, keyed by
     * the name of the enumeration; map may be empty if no command enumerations exist
     * 
     */
    public Map<String, CommandEnumerationDefinition> getArgumentEnumerations();


    /**
     * Adds an uplink file type name and ID to the set of uplink
     * file types.
     * 
     * @param name name of uplink file
     * @param id ID of uplink file type
     */
    @Mutator
    public void setUplinkFileType(final String name, final Integer id);
    
    /**
     * Retrieves an uplink file ID given the uplink file type name.
     * @param name type name of uplink file
     * @return ID of uplink file type
     */  
    public int getUplinkFileIdForType(String name);
    
    /**
     * Retrieves the uplink file type name given the uplink file ID.
     * @param  id ID of uplink file 
     * @return type name of uplink file type; null if no matching uplink file ID
     */

    public String getUplinkFileTypeForId(int id);
    
    /**
     * Returns the set of uplink file type names.
     * @return a set of strings
     */
    public Set<String> getUplinkFileTypes(); 

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.dictionary.api.IDefinitionProviderLoadStatus#isLoaded()
     */
    @Override
    default public boolean isLoaded() {
        return true;
    }
}
