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
package jpl.gds.dictionary.api.sequence;

import java.util.Map;

import jpl.gds.dictionary.api.IBaseDictionary;
import jpl.gds.shared.annotation.CustomerAccessible;

/**
 * The ISequenceDictionary interface is to be implemented by all Command
 * Sequence Dictionary adaptation classes.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * The Sequence Dictionary is used in the command and telemetry processing in
 * order to translate numeric sequence IDs to more human-readable equivalents.
 * Primarily it tracks sequence categories, but also has some knowledge about
 * how to parse pure numeric sequence IDs to sequence category and sequence
 * number. Every mission may have a different format for representing the
 * Sequence dictionary. An appropriate dictionary parser must be used in order
 * to create the mission-specific ISequenceDictionary object, which MUST
 * implement this interface. ISequenceDictionary objects should only be created
 * via the SequenceDictionaryFactory. Direct creation of an ISequenceDictionary
 * object is a violation of multi-mission development standards.
 * <p>
 * This interface extends the IBaseDictionary interface, which requires parse()
 * and clear() methods on all dictionary objects. These methods are used to
 * populate and clear the dictionary contents, respectively.
 * 
 *
 * @see ISequenceDictionaryFactory
 * @see jpl.gds.dictionary.api.IBaseDictionary
 */
@CustomerAccessible(immutable = true)
public interface ISequenceDictionary extends IBaseDictionary, ISequenceDefinitionProvider {

    /**
     * Gets the map of sequence category ID to sequence category name.
     * 
     * @return map of sequence id to category name; may be empty but never null
     */
    public Map<Integer,String> getCategoryMap();

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