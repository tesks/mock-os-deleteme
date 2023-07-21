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
package jpl.gds.evr.impl.service.extractor;

import jpl.gds.dictionary.api.sequence.ISequenceDefinitionProvider;

/**
 * SeqIdReplacement is a type of EVR ParameterReplacement that replaces a number command
 * sequence ID with a sequence category/name. This class takes care of generating a 
 * sequence parameter to be used in an EVR message when a parameter of type SEQID is 
 * encountered.  It contains the seqid dictionary which defines the sequence ID to sequence 
 * category mapping.
 * 
 *
 */
public class SeqidReplacement implements ParameterReplacement {

    private ISequenceDefinitionProvider seqids;

    /**
     * Constructor: set the SEQ ID dictionary for this replacement object
     * @param dictionary dictionary that contains sequence id and category mapping
     */
    public SeqidReplacement(final ISequenceDefinitionProvider dictionary) {
        this.seqids = dictionary;
    }

    /**
     * Sets a SEQ ID dictionary for this replacement object.
     * @param dictionary dictionary that contains sequence id and category mapping
     */
    public void setDictionary(final ISequenceDefinitionProvider dictionary) {
        this.seqids = dictionary;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.evr.impl.service.extractor.ParameterReplacement#replace(java.lang.Object)
     */
    public Object replace(Object original) {

        if(original == null) {
            return original;
        }
        
        try {
            /* Object passed is now byte[] rather than RawEvrData */
            return this.seqids.getSeqNameFromSeqIdBytes((byte[])original);
        }
        catch (NullPointerException e) {
            return original;
        }
    }
}
