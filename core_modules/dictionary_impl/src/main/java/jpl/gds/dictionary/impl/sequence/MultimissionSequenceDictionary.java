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
package jpl.gds.dictionary.impl.sequence;

import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;

/**
 * This is the multimission parser for the sequence dictionary. At this time,
 * the MSL schema is the only schema, so the base class implements the parsing,
 * but it is abstract. This empty extension exists so we can create an instance
 * of it. In the future, the implementations may diverge.
 * 
 *
 */
public class MultimissionSequenceDictionary extends AbstractSequenceDictionary {
    
    private static final String MM_SCHEMA_VERSION =
            DictionaryProperties.getMultimissionDictionaryVersion(DictionaryType.SEQUENCE);
    
    /**
     * Package protected constructor.
     * 
     */
    MultimissionSequenceDictionary() {
        super(MM_SCHEMA_VERSION);
    }

}
