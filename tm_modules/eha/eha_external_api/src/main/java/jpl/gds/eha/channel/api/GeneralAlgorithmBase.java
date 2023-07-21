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
package jpl.gds.eha.channel.api;

import java.util.Map;

import jpl.gds.dictionary.api.OpcodeUtil;
import jpl.gds.dictionary.api.channel.IImmutableChannelDefinition;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.sequence.ISequenceDefinitionProvider;
import jpl.gds.shared.annotation.AmpcsLocked;
import jpl.gds.shared.annotation.CustomerAccessible;
import jpl.gds.shared.annotation.Mutator;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
 * Base utility class for both custom EU calculations and channel derivations.
 * Provides utility methods that may be needed by algorithms and methods AMPCS
 * uses to setup algorithms. Customer classes should NOT extend this class.
 * They should extend DerivationAlgorithmBase, SimpleEuBase, or ParameterizedEuBase.
 * <p>
 * <b>MULTI-MISSION CORE ADAPTATION CLASS
 * <p>
 * This is a controlled class. It may not be updated without applicable change
 * requests being filed, and approval of project management. A new version tag
 * must be added below with each revision, and both ECR number and author must
 * be included with the version number.</b>
 * <p>
 */
@CustomerAccessible(immutable = false)
public abstract class GeneralAlgorithmBase implements IAlgorithmUtility {
    
    private ILatestSampleProvider ladProvider;
    private Map<String,String> opCodeToStemMap;
    private OpcodeUtil opcodeUtil;
    private ISequenceDefinitionProvider seqDict;
    private Map<String, ? extends IImmutableChannelDefinition> channelDefMap;
    private Tracer                                             logger = TraceManager.getTracer(Loggers.TLM_DERIVATION);
 
	/**
	 * Constructor.
	 */
	protected GeneralAlgorithmBase() {
		super();
	}


    @Override
    @Mutator
    @AmpcsLocked
    public void setLadProvider(final ILatestSampleProvider provider) {
        /** Removed SecurityManager checks for performance improvements */
        this.ladProvider = provider;
	}
	
    
	/**
     * {@inheritDoc}
     */
	@Override
    public IChannelValue getMostRecentChannelValue(final String id) {
		return (getMostRecentChannelValue(id, true));
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public IChannelValue getMostRecentChannelValue(final String id,
			final boolean realtime) {
	    if (ladProvider == null) {
	        throw new IllegalStateException("The latest sample (LAD) provider is null");
	    }
		return (ladProvider.getMostRecentValue(id, realtime, 0));
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public IChannelValue getMostRecentChannelValue(final String id,
			final boolean realtime, final int station) {
	    if (ladProvider == null) {
            throw new IllegalStateException("The latest sample (LAD) provider is null");
        }
		return (ladProvider.getMostRecentValue(id, realtime, station));
	}
	
    /**
     * {@inheritDoc}
     */
	@Override
    public IImmutableChannelDefinition getChannelDefinition(final String chanId) {
	    return channelDefMap.get(chanId);
	}

    @Override
    public String getSequenceCategoryNameByCategoryId(final int catId) {
        if (seqDict == null) {
            return null;
        }
        return seqDict.getCategoryNameByCategoryId(catId);
    }

    @Override
    public int getCategoryIdFromSeqId(final int seqid) {
        if (seqDict == null) {
            return 0;
        }
        return seqDict.getCategoryIdFromSeqId(seqid);
    }

    @Override
    public int getSequenceNumberFromSeqId(final int seqid) {
        if (seqDict == null) {
            return 0;
        }
        return seqDict.getSequenceNumberFromSeqId(seqid);
    }

    @Override
    @Mutator
    @AmpcsLocked
    public void setOpcodeToStemMap(final Map<String, String> opCodeToStemMap) {
        /**  Removed SecurityManager checks for performance improvements */
        this.opCodeToStemMap = opCodeToStemMap;
    }

    @Override
    @Mutator
    @AmpcsLocked
    public void setDictionaryProperties(final DictionaryProperties dictConfig) {
        /**  Removed SecurityManager checks for performance improvements */
    	opcodeUtil = new OpcodeUtil(dictConfig);
    }

    @Override
    @Mutator
    @AmpcsLocked
    public void setSequenceDictionary(final ISequenceDefinitionProvider dict) {
        /** Removed SecurityManager checks for performance improvements */
    	this.seqDict = dict;
    }
    
    @Override
    public String getStemForOpcode(final int opcode) {
        final String op = opCodeToStemMap.get(opcodeUtil.formatOpcode(opcode, false));
        logger.trace("Opcode formatted: ", op);
        return op;
    }

    @Override
    public void setChannelDefinitionMap(final Map<String, ? extends IImmutableChannelDefinition> defMap) {
        this.channelDefMap = defMap;      
    }

    @Override
    public void setLogger(final Tracer log) throws DerivationException {
        if (null == log) {
            return;
        }
        this.logger = log;      
    }

    @Override
    public void logInfo(final String message) {
        logger.info(message);  
    }

    @Override
    public void logDebug(final String message) {
        logger.debug(message);       
    }
    
    @Override
    public void logWarning(final String message) {
        logger.warn(message);       
    }


    @Override
    public void logError(final String message) {
        logger.error(message);      
    }
}