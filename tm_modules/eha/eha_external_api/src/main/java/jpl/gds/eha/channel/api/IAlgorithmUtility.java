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

import jpl.gds.dictionary.api.channel.IImmutableChannelDefinition;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.sequence.ISequenceDefinitionProvider;
import jpl.gds.shared.annotation.AmpcsLocked;
import jpl.gds.shared.annotation.CustomerAccessible;
import jpl.gds.shared.annotation.Mutator;
import jpl.gds.shared.log.Tracer;

/**
 * The IAlgorithmUtility interface is to be extended by the interfaces for
 * channel derivation and EU calculation. It provides two kinds of utility
 * functions: those invoked my AMPCS to initialize the algorithm instance, and
 * those used by the customer-supplied classes to perform utility functions and
 * access dictionary information. Customer classes should not extend this
 * interface.
 * <p>
 * <b>THIS IS A MULTI-MISSION CORE ADAPTATION INTERFACE</b>
 * <p>
 * <b>This is a controlled interface. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b>
 * 
 *
 */
@CustomerAccessible(immutable = false)
public interface IAlgorithmUtility {

    /**
     * Sets the latest sample provider for use by the derivation or EU
     * calculation. This allows derivations and EU calculations to access the
     * local LAD in the downlink processor.
     * <p>
     * This is an AMPCS locked method. It cannot be called by customer-supplied
     * derivation classes.
     * 
     * @param provider
     *            the LAD Provider for use by this derivation; may not be null
     * @throws DerivationException
     *             if invoked by a non-authorized class
     */
    @Mutator
    @AmpcsLocked
    public void setLadProvider(ILatestSampleProvider provider) throws DerivationException;

    /**
     * Convenience method to get the most recent realtime value for a channel
     * from the LAD. Note that the interpretation of "latest" is based upon the
     * currently established time system. Parent channel values are placed into
     * the LAD BEFORE a derivation is invoked.
     * 
     * @param id
     *            channel ID of the channel to get the value for
     * @return IChannelValue, or null if no values in the LAD for the specified
     *         channel
     */
    public IChannelValue getMostRecentChannelValue(String id);

    /**
     * Convenience method to get the most recent realtime or recorded value for
     * a channel from the LAD. Note that the interpretation of "latest" is based
     * upon the currently established time system. Parent channel values are
     * placed into the LAD BEFORE a derivation is invoked.
     * 
     * @param id
     *            channel ID of the channel to get the value for
     * @param realtime
     *            true to get the latest realtime value, false for recorded
     * @return IChannelValue, or null if no values in the LAD
     */
    public IChannelValue getMostRecentChannelValue(String id, boolean realtime);

    /**
     * Convenience method to get the most recent realtime or recorded value for
     * a channel from the LAD for a specific station. The only channels kept in
     * the LAD per station are the station monitor channels. Note that the
     * interpretation of "latest" is based upon the currently established time
     * system. Parent channel values are placed into the LAD BEFORE a derivation
     * is invoked.
     * 
     * @param id
     *            channel ID of the channel to get the value for
     * @param realtime
     *            true to get the latest realtime value, false for recorded
     * @param station
     *            the Station ID from which the queried sample came
     * @return IChannelValue, or null if no values in the LAD
     */
    public IChannelValue getMostRecentChannelValue(String id, boolean realtime, int station);

    /**
     * Sets the map of channel definitions, keyed by channel ID, for channels
     * defined in the current telemetry dictionary. This allows derivations and
     * EU calculations to look up dictionary information about channels.
     * <p>
     * This is an AMPCS locked method. It cannot be called by customer-supplied
     * derivation classes
     * 
     * @param defMap
     *            Map of channel ID to channel definition; may not be null
     * @throws DerivationException
     *             if invoked by a non-authorized class
     */
    public void setChannelDefinitionMap(Map<String, ? extends IImmutableChannelDefinition> defMap) throws DerivationException;

    /**
     * Get the definition for the specified Channel ID from the current
     * telemetry dictionary.
     * 
     * 
     * @param chanId
     *            the Channel ID for which the definition is being queried; may
     *            not be null
     * @return the definition for the specified Channel ID, or null if no match
     *         found
     */
    public IImmutableChannelDefinition getChannelDefinition(String chanId);

    /**
     * Performs the translation of a command OPCODE to its mnemonic stem. Will
     * return null if the mission does not support commanding thru AMPCS and has
     * no command dictionary. Return value must be checked for null.
     * 
     * @param opcode
     *            the OPCODE to translate
     * 
     * @return the stem for the provided opcode, or null if no such opcode
     *         found.
     */
    public String getStemForOpcode(int opcode);

    /**
     * Looks up the command sequence category name given a numeric sequence
     * category ID. This method will function only for missions that have
     * enabled and provided a command sequence dictionary. If that is not the
     * case, this method will return null. The return value should always be
     * checked for null.
     * 
     * @param catId
     *            the category ID to use to lookup the sequence category name
     * @return the sequence category name that is mapped to the provided
     *         category ID; may be null
     */
    public String getSequenceCategoryNameByCategoryId(int catId);

    /**
     * Extracts the command sequence number from the provided numeric sequence
     * ID. This method will function only for missions that have enabled and
     * provided a command sequence dictionary. If that is not the case, this
     * method will return 0.
     * 
     * @param seqid
     *            the sequence ID to use to look up the sequence number
     * @return the sequence number that is mapped to the provided sequence ID
     */
    public int getSequenceNumberFromSeqId(final int seqid);

    /**
     * Extracts the command sequence category ID from the provided numeric
     * sequence ID. This method will function only for missions that have
     * enabled and provided a command sequence dictionary. If that is not the
     * case, this method will return 0.
     * 
     * @param seqid
     *            the sequence ID to use to lookup the category ID
     * @return the category ID that is mapped to the provided sequence ID
     */
    public int getCategoryIdFromSeqId(final int seqid);

    /**
     * Sets the map of command OPCODES to mnemonic stems to use for this
     * derivation. This allows derivations and EU calculations to map channel
     * values that are opcodes to their stems. Used only if the mission supports
     * commanding thru AMPCS and provides a command dictionary.
     * <p>
     * This is an AMPCS locked method. It cannot be called by customer-supplied
     * derivation classes
     * 
     * @param opCodeToStemMap
     *            the map of OPCODES to stems to use this derivation
     * @throws DerivationException
     *             if invoked by a non-authorized class
     */
    @Mutator
    @AmpcsLocked
    public void setOpcodeToStemMap(Map<String, String> opCodeToStemMap) throws DerivationException;

    /**
     * Sets the Dictionary Properties in use at the time this algorithm is
     * employed. This enables various utility functions.
     * <p>
     * This is an AMPCS locked method. It cannot be called by customer-supplied
     * derivation classes
     * 
     * @param dictProperties
     *            the Dictionary Properties object to set
     * @throws DerivationException
     *             if invoked by a non-authorized class
     */
    @Mutator
    @AmpcsLocked
    public void setDictionaryProperties(DictionaryProperties dictProperties) throws DerivationException;

    /**
     * Sets the Sequence Dictionary to use for this algorithm instance, which
     * enables mapping and extraction methods for sequence IDs. This method is
     * used only for missions that have enabled and provided a command sequence
     * dictionary.
     * <p>
     * This is an AMPCS locked method. It cannot be called by customer-supplied
     * derivation classes
     * 
     * @param dict
     *            the Sequence Dictionary to use for this derivation algorithm
     * @throws DerivationException
     *             if invoked by a non-authorized class
     */
    @Mutator
    @AmpcsLocked
    public void setSequenceDictionary(ISequenceDefinitionProvider dict) throws DerivationException;

    /**
     * Sets the trace logger for use by this algorithm instance.
     * <p>
     * This is an AMPCS locked method. It cannot be called by customer-supplied
     * derivation classes
     * 
     * @param log
     *            the Tracer to set as the logger; may not be null
     * 
     * @throws DerivationException
     *             if invoked by a non-authorized class
     */
    public void setLogger(Tracer log) throws DerivationException;

    /**
     * Logs an INFO message on behalf of this algorithm instance.
     * 
     * @param message
     *            message to log
     */
    public void logInfo(String message);

    /**
     * Logs a DEBUG message on behalf of this algorithm instance.
     * 
     * @param message
     *            message to log
     */
    public void logDebug(String message);
    
    /**
     * Logs a WARN message on behalf of this algorithm instance.
     * 
     * @param message
     *            message to log
     */
    public void logWarning(String message);

    /**
     * Logs an ERROR message on behalf of this algorithm instance.
     * 
     * @param message
     *            message to log
     */
    public void logError(String message);

}