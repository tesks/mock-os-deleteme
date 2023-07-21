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

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.EnumerationDefinition;
import jpl.gds.dictionary.api.OpcodeUtil;
import jpl.gds.dictionary.api.command.ICommandDefinitionProvider;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.evr.EvrArgumentType;
import jpl.gds.dictionary.api.evr.IEvrArgumentDefinition;
import jpl.gds.dictionary.api.evr.IEvrDefinition;
import jpl.gds.dictionary.api.sequence.ISequenceDefinitionProvider;
import jpl.gds.evr.api.service.extractor.EvrExtractorException;
import jpl.gds.evr.api.service.extractor.IEvrExtractorUtility;
import jpl.gds.evr.api.service.extractor.IRawEvrData;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.string.SprintfUtil;
import jpl.gds.shared.string.SprintfUtilException;
import jpl.gds.shared.types.Triplet;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A utility class that assists with EVR extraction and formatting functions.
 * 
 * @since R8
 */
public class EvrExtractorUtility implements IEvrExtractorUtility {
    
    private final AtomicBoolean noCommandDictFlagged = new AtomicBoolean(false);
    private final AtomicBoolean noSeqidDictFlagged = new AtomicBoolean(false);
    private final ISequenceDefinitionProvider seqDict;
    private final OpcodeUtil opcodeUtil;
    private ICommandDefinitionProvider cmdProvider;
    private final Tracer logger;


    /**
     * Constructor.
     * 
     * @param context
     *            the current application context
     * @throws EvrExtractorException
     *             if there is an issue creating the utility
     */
    public EvrExtractorUtility(final ApplicationContext context) throws EvrExtractorException {

        try {
            this.cmdProvider = context.getBean(ICommandDefinitionProvider.class);
        } catch (final BeanCreationException e) {
            if (!e.contains(DictionaryException.class)) {
                throw new EvrExtractorException("Command dictionary could not be created from the service context", e);
            } else {
                this.cmdProvider = null;
            }
        }
        this.seqDict = context.getBean(ISequenceDefinitionProvider.class);
        this.opcodeUtil = new OpcodeUtil(context.getBean(DictionaryProperties.class));
        /* make logger have the app context */
        logger = TraceManager.getTracer(context, Loggers.TLM_EVR);
    }

    /**
     * Constructor
     * @param cmdProvider The {@link ICommandDefinitionProvider}
     * @param seqDict the {@link ISequenceDefinitionProvider}
     * @param opcodeUtil the {@link OpcodeUtil}
     * @param tracer logger
     */
     EvrExtractorUtility(ICommandDefinitionProvider cmdProvider, ISequenceDefinitionProvider seqDict,
                              OpcodeUtil opcodeUtil, Tracer tracer) {

         this.cmdProvider = cmdProvider;
         this.seqDict = seqDict;
         this.opcodeUtil = opcodeUtil;
         logger = tracer;
     }
    

    /**
     * @{inheritDoc}
     * @see jpl.gds.evr.api.service.extractor.IEvrExtractorUtility#replaceParameters(List, String, IEvrDefinition)
     */
    @Override
    public String replaceParameters(final List<IRawEvrData> parameters,
            final String inFormat, final IEvrDefinition evrDefinition) throws EvrExtractorException {
        String format = inFormat;
        
        /*  Re-arranged logic to fix NPE resulting from using null args */
        if (parameters.isEmpty()) {
            return format;
        }
 
        final List<IEvrArgumentDefinition> args = evrDefinition.getArgs();
        if (args == null) {
            return format;
        }
        final int nargs = args.size();

        if (parameters.size() != nargs) {
            throw new EvrExtractorException("Number of actual parameters " + parameters.size() + 
                    " does not match number of declared EVR arguments " + nargs);
        }
        for (int i = 0; i < nargs; ++i) {
            final IRawEvrData parameter = parameters.get(i);
            final IEvrArgumentDefinition entry = args.get(i);
            /*
             *  Use EnumerationDefinition object rather
             * than ReferenceEnumeratedValue list.
             */
            if (entry.getType().equals(EvrArgumentType.ENUM)) {
                final EnumerationDefinition enumValues = entry.getEnumeration();
                final Long newValue = GDR.getSignedInteger(parameter.getByteArray(), 0, parameter.getSize() * 8);
                final Object replacer = getReplacement(newValue, enumValues);
                parameter.replaceData(replacer.toString());
                parameters.set(i, parameter );
                format = replaceFormat(format, i + 1);
            }
            else if(entry.getType().equals(EvrArgumentType.OPCODE))
            {
                final long opcode = GDR.getUnsignedInteger(parameter.getByteArray(),
                                                           0,
                                                           parameter.getSize() * 8);
                
                /* Code was just completely wrong. It returned the opcode
                 * rather than the format string. Reworked the logic and now return the format string.
                 */
                
                if (cmdProvider == null && !noCommandDictFlagged.get())
                {
                    logger.warn("Could not load command dictionary to do EVR opcode replacement.");
                    setNoCommandDictFlagged(true);
                }
                
                if (noCommandDictFlagged.get())
                {
                    final String op = opcodeUtil.formatOpcode(opcode, true);
                    parameter.replaceData(op);
                    parameters.set(i,parameter);
                    format = replaceFormat(format,i+1);
                } else {

                    final OpcodeReplacement or = new OpcodeReplacement(cmdProvider.getStemByOpcodeMap(), opcodeUtil);
                    final String replacement = or.replace(Long.valueOf(opcode)).toString();
                    parameter.replaceData(replacement);
                    parameters.set(i,parameter);
                    format = replaceFormat(format,i+1);
                }
            }
            else if(entry.getType().equals(EvrArgumentType.SEQID)) {

                //if there is a seqid file...
                if(!noSeqidDictFlagged.get()) {
                   if (seqDict != null) {

                        final SeqidReplacement sr = new SeqidReplacement(seqDict);
                        final String replacement = sr.replace(parameter.getByteArray()).toString();
                        format = insertTextAfterParameter(format, replacement, i+1);
                    } else {
                        setNoSeqidDictFlagged(true);
                    }
                }
            }
        }      
        return format;
    }
    
    
    /**
     * Sets the flag indicating that a missing command dictionary message has
     * been logged.
     * 
     * @param val
     *            true if a missing command dictionary message has been logged,
     *            false if not
     */
    private void setNoCommandDictFlagged(final boolean val) {
        noCommandDictFlagged.set(val);
    }

    /**
     * Sets the flag indicating that a missing sequence ID dictionary message has
     * been logged.
     * 
     * @param val
     *            true if a missing sequence ID dictionary message has been logged,
     *            false if not
     */
    private void setNoSeqidDictFlagged(final boolean val) {
        noSeqidDictFlagged.set(val);
    } 
    
    /**
     * Gets the replacement value for an enumerated EVR parameter (argument).
     * @param parameter the EVR argument value
     * @param enumValues the list of enumeration values for the argument
     * @return the replacement value from the enumeration for the given parameter value
     * 
     */
    private Object getReplacement(final Long parameter, final EnumerationDefinition enumValues) {
        if (enumValues == null) {
            return parameter;
        }
        final Object mappedVal = enumValues.getValue(parameter);
        return mappedVal == null ? parameter : mappedVal;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.evr.api.service.extractor.IEvrExtractorUtility#insertTextAfterParameter(java.lang.String, java.lang.String, int)
     */
    @Override
    public String insertTextAfterParameter(final String format,
            final String textToBeInserted, final int inIndex) {
    
        final int index = inIndex - 1;
    
        List<Triplet<Integer, Integer, String>> formatters = null;
    
        try {
            formatters = SprintfUtil.getFormatLettersAndPositions(format);
        } catch (final SprintfUtilException e) {
            // When this method is used in EVR processing, this error will
            // have been caught
            // earlier and the EVR will not be processed, Therefore, this SHOULD
            // be an unnecessary
            // catch for EVR processing and that's why I do not throw here.
            logger.warn(
                    "EVR insert text found invalid format string: "
                            + e.getMessage(), e);
            return format;
        }
    
        if (index < 0 || index >= formatters.size()) {
            return format;
        }
    
        final Triplet<Integer, Integer, String> position = formatters.get(index);
        final StringBuilder newFormat = new StringBuilder(format.length());
    
        if (position.getTwo() > 0) { // - isn't this always true?
            newFormat.append(format.substring(0, position.getTwo() + 1));
        }
        newFormat.append(" ");
        newFormat.append(textToBeInserted); // insert string here
        newFormat.append(format.substring(position.getTwo() + 1));
    
        return newFormat.toString();
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.evr.api.service.extractor.IEvrExtractorUtility#replaceFormat(java.lang.String, int)
     */
    @Override
    public String replaceFormat(final String format, final int inIndex) {
    
        final int index = inIndex - 1;
    
        try {
            final List<Triplet<Integer, Integer, String>> formatterPositions = SprintfUtil.getFormatLettersAndPositions(format);
            if (index < 0 || index >= formatterPositions.size()) {              
                logger.warn("EVR format replacement attempted to replace formatter that does not exist");
                return format;
            } else {
                final StringBuilder newFormat = new StringBuilder(format);
                final Triplet <Integer, Integer, String> location = formatterPositions.get(index);
                newFormat.replace(location.getOne(), location.getTwo() + 1, "%s");
                final String result = newFormat.toString();
                return result;
            }
        } catch (final SprintfUtilException e) {
    
            // When this method is used in EVR processing, this error will have been caught
            // earlier and the EVR will not be processed, Therefore, this SHOULD be an unnecessary
            // catch for EVR processing and that's why I do not throw here.
            e.printStackTrace();
            logger.warn("EVR format replacement found invalid format string: " + e.getMessage());
            return format;
        }
    }


}
