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

import java.util.List;
import java.util.Map;

import jpl.gds.dictionary.api.EnumerationDefinition;
import jpl.gds.dictionary.api.IBaseDictionary;
import jpl.gds.shared.annotation.CustomerAccessible;

/**
 * The IEvrDictionary interface is to be implemented by all EVR dictionary
 * adaptation classes.
 * <p>
 * <p><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <p>
 * <p>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b><p>
 * 
 * The JPL document corresponding to the multimission EVR dictionary schema is
 * D-001140, the AMPCS Multimission EVR Dictionary SIS, in the JPL MGSS Document
 * Management System.
 * <p>
 * The EVR dictionary is used in downlink processing to map EVent Record data to
 * their respective definitions. Each EVR definition carries a human-readable
 * text, with optional parameters whose values are substituted by the processed
 * data. Every mission may have a different format for representing the EVR
 * dictionary. An appropriate dictionary parser must be used in order to create
 * the mission-specific IEvrDictionary object. IEvrDictionary objects should
 * only be created via the EvrDictionaryFactory. Direct creation of an
 * IEvrDictionary object is a violation of multi-mission development standards.
 * <p>
 * This interface extends the IBaseDictionary interface, which requires parse()
 * and clear() methods on all dictionary objects. These methods are used to
 * populate and clear the dictionary contents, respectively.
 * <p>
 * The primary job of the IEvrDictionary object is to produce a set of
 * IEvrDefinition objects. Each IEvrDefinition is the multi-mission
 * representation of a defined EVR in the dictionary.
 * <p>
 * 
 *
 *
 * @see IEvrDictionaryFactory
 * @see IBaseDictionary
 * @see IEvrDefinition
 */
@CustomerAccessible(immutable = true)
public interface IEvrDictionary extends IBaseDictionary, IEvrDefinitionProvider {


    /**
     * Retrieves the list of parsed EVR Definitions from the IEvrDictionary.
     * 
     * @return list of IEvrDefinition objects; list will be empty (not null) if
     *         no EVR definitions exist
     */
    public List<IEvrDefinition> getEvrDefinitions();
    
    /**
     * Retrieves the list of parsed Enumeration Definitions from the
     * IEvrDictionary.
     * 
     * @return Map of EnumerationDefinition objects, keyed by the enumeration
     *         (typedef) name; map will be empty (not null) if no enumeration
     *         definitions exist
     */
    public Map<String, EnumerationDefinition> getEnumDefinitions(); 

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
