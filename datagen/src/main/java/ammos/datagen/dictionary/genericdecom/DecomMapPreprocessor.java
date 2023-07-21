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
package ammos.datagen.dictionary.genericdecom;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDictionary;
import jpl.gds.dictionary.api.channel.IChannelDictionaryFactory;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.decom.DecomMapDefinitionFactory;
import jpl.gds.dictionary.api.decom.IChannelStatementDefinition;
import jpl.gds.dictionary.api.decom.IDecomMapDefinition;
import jpl.gds.dictionary.api.decom.IDecomStatement;
import jpl.gds.dictionary.api.decom.IOffsetStatementDefinition;
import jpl.gds.dictionary.api.decom.ISkipStatementDefinition;
import jpl.gds.dictionary.api.decom.ISwitchStatementDefinition;
import jpl.gds.dictionary.api.decom.IVariableStatementDefinition;
import jpl.gds.dictionary.api.decom.IWidthStatementDefinition;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.SpringContextFactory;

/**
 * This class will take a decom map file and a channel dictionary and generate a
 * tree of information from the map that details all the various paths through
 * the map, including every variable and channel and their offsets. The intent
 * is to use such information to be able to generate data that matches the map
 * and exhausts all the possible decom paths.
 * <p>
 * The maps read by this class must adhere to the AMPCS generic channel decom
 * dictionary schema, as defined in JPL D-001143 in the MGSS DMS system.
 * 
 *
 * MPCS-6944 - 12/10/14. Added class.
 */
public class DecomMapPreprocessor {

    private final Tracer                                  trace;


    private final String mapFile;
    private final String channelFile;
    private IDecomMapDefinition map;
    private final Map<String, IDecomPreprocessorVariable> variableMap = new HashMap<String, IDecomPreprocessorVariable>();
    private final DecomPreprocessorBranch root = new DecomPreprocessorBranch();
    private int dataOffset = 0;
    private int defaultWidth = 8;
    private DecomPreprocessorBranch currentBranch;
    private Map<String, IChannelDefinition> chanMap;

    /**
     * Constructor for use when the channel dictionary is already loaded into a
     * map.
     * 
     * @param chanMap
     *            a hash map of channel ID to channel definition
     * @param mapFile
     *            the path to the decom map XML file
     */
    public DecomMapPreprocessor(final Map<String, IChannelDefinition> chanMap,
            final String mapFile) {
        if (chanMap == null) {
            throw new IllegalArgumentException("channel map may not be null");
        }
        if (mapFile == null) {
            throw new IllegalArgumentException("map file may not be null");
        }
        this.mapFile = mapFile;
        this.channelFile = null;
        this.chanMap = chanMap;
        this.currentBranch = this.root;
        this.trace = TraceManager.getTracer(Loggers.TLM_EHA);
    }

    /**
     * Constructor for use when the channel dictionary must be loaded.
     * 
     * @param channelFile
     *            the path to the channel dictionary XML file
     * @param mapFile
     *            the path to the decom map XML file
     */
    public DecomMapPreprocessor(final String channelFile, final String mapFile) {

        if (channelFile == null) {
            throw new IllegalArgumentException("channel file may not be null");
        }
        if (mapFile == null) {
            throw new IllegalArgumentException("map file may not be null");
        }
        this.mapFile = mapFile;
        this.channelFile = channelFile;
        this.currentBranch = this.root;
        this.trace = TraceManager.getTracer(Loggers.TLM_EHA);
    }

    /**
     * Initializes the application by loading the channel dictionary and the
     * decom map.
     * @param appContext the current application context
     * 
     * @return true if initialization was successful, false if not
     */
    public synchronized boolean init(final ApplicationContext appContext) {
        trace.setAppContext(appContext);
        try {
            if (this.chanMap == null) {
                final IChannelDictionary dict = appContext.getBean(IChannelDictionaryFactory.class)
                        .getNewInstance(appContext.getBean(DictionaryProperties.class), this.channelFile);
                this.chanMap = new HashMap<>();
                for (final IChannelDefinition def : dict
                        .getChannelDefinitions()) {
                    this.chanMap.put(def.getId(), def);
                }
            }
            /* MHT - MPCS-7768 - update to use the decom map definition factory */
            this.map = DecomMapDefinitionFactory.createDecomMapFromFile(
                    this.mapFile, this.chanMap);

        } catch (final DictionaryException e) {
            trace.error(e.getMessage());
            return false;
        }
        return true;

    }

    /**
     * Sets the DATA offset, for when offsets are specified as DATA in the map.
     * Defaults to 0.
     * 
     * @param offset
     *            the data offset to set
     */
    public synchronized void setStartDataOffset(final int offset) {
        this.dataOffset = offset;
    }

    /**
     * Executes the pre-processing of the decom map.
     */
    public synchronized void buildTree() {

        preProcessStatements(this.map.getStatementsToExecute(), this.dataOffset);
    }

    /**
     * Performs pre-processing on a list of decom statements.
     * 
     * @param statementsToWalk
     *            the decom statements to pre-process
     * @param currentOffset
     *            the current offset within the overall map pre-processing
     */
    private void preProcessStatements(
            final List<IDecomStatement> statementsToWalk, int currentOffset) {

        /* Loop through all the decom statements in the list */
        for (final IDecomStatement s : statementsToWalk) {
            if (s instanceof IWidthStatementDefinition) {

                /*
                 * A width statement just changes the default width of channel
                 * samples that follow.
                 */
                this.defaultWidth = ((IWidthStatementDefinition) s).getWidth();

            } else if (s instanceof IOffsetStatementDefinition) {
                final IOffsetStatementDefinition os = (IOffsetStatementDefinition) s;

                /*
                 * An offset statement sets the current offset to an absolute
                 * value. The "data" offset is special -- it means the offset of
                 * the first data byte.
                 */
                if (os.isDataOffset()) {
                    currentOffset = this.dataOffset;

                } else {

                    switch (os.getOffsetType()) {

                    case ABSOLUTE:
                        currentOffset = os.getOffset();
                        break;

                    case PLUS:
                        currentOffset += os.getOffset();
                        break;

                    case MINUS:
                        currentOffset -= os.getOffset();
                        break;

                    default:
                        // shouldn't happen
                        throw new IllegalStateException(
                                "Encountered unexpected type in offset statement: "
                                        + os.getOffsetType()
                                        + " in decom map for APID "
                                        + this.map.getApid());

                    }

                }

            } else if (s instanceof ISkipStatementDefinition) {

                /*
                 * A skip statement just moves the current offset forward
                 * without extracting anything.
                 */
                currentOffset += ((ISkipStatementDefinition) s)
                        .getNumberOfBitsToSkip();

            } else if (s instanceof IChannelStatementDefinition) {

                final IChannelStatementDefinition cs = (IChannelStatementDefinition) s;

                /*
                 * A channel statement represents a sample. For this we have to
                 * create decom channel object, and then advance the offset.
                 */

                final boolean offsetOverridden = cs.offsetSpecified();
                final int offset = offsetOverridden ? cs.getOffset()
                        : currentOffset;
                final boolean widthOverridden = cs.widthSpecified();
                final int width = widthOverridden ? cs.getWidth()
                        : this.defaultWidth;
                currentOffset = offsetOverridden ? currentOffset
                        : currentOffset + width;

                /* Creating this object adds it to the current decom branch. */
                new DecomPreprocessorChannel(this.currentBranch,
                        cs.getChannelId(), offset / 8, offset % 8, width,
                        cs.getChannelType());

            } else if (s instanceof IVariableStatementDefinition) {
                final IVariableStatementDefinition vs = (IVariableStatementDefinition) s;

                /*
                 * For a variable statement we need to create a variable decom
                 * object. Note that variable statements do not change the
                 * current offset
                 */

                final String name = vs.getVariableName();

                if (vs.isExtractionVariable()) {

                    /*
                     * The variable is one that actually must contain a value in
                     * the decom data.
                     */
                    final int offset = vs.isOffsetSpecified() ? vs
                            .getOffsetToExtract() : currentOffset;
                            final int width = vs.getWidthToExtract();

                            /* Creating this object adds it to the current decom branch. */
                            final DecomPreprocessorVariable varOffset = new DecomPreprocessorVariable(
                                    this.currentBranch, name, offset / 8, offset % 8,
                                    width);
                            this.variableMap.put(name, varOffset);

                } else {
                    /*
                     * The variable just references another variable. Creating
                     * this object adds it to the current decom branch.
                     */
                    final IDecomPreprocessorVariable varOffset = this.variableMap
                            .get(vs.getReferenceVariableName());
                    final DecomPreprocessorVariableReference newVar = new DecomPreprocessorVariableReference(
                            this.currentBranch, name, varOffset);
                    this.variableMap.put(name, newVar);

                }

            } else if (s instanceof ISwitchStatementDefinition) {
                final ISwitchStatementDefinition ss = (ISwitchStatementDefinition) s;

                /*
                 * A switch statement represents a place where the decom map
                 * follows multiple paths. What we want to do is create a new
                 * decom branch for each case in the switch.
                 */
                final String variableName = ss.getVariableToSwitchOn();
                final IDecomPreprocessorVariable variable = this.variableMap
                        .get(variableName);

                /*
                 * This gets all the possible case values in the switch, except
                 * for the default case.
                 */
                final List<Long> validCases = ss.getCaseValues();

                /* Note where we are in the branch structure. */

                final DecomPreprocessorBranch saveBranch = this.currentBranch;

                for (final Long caseVal : validCases) {

                    /*
                     * Do nothing if no decom statements under this case. I
                     * don't know if this can happen or not.
                     */
                    final List<IDecomStatement> statementsUnderCase = ss
                            .getStatementsToExecute(caseVal);
                    if (statementsUnderCase == null
                            || statementsUnderCase.isEmpty()) {
                        continue;
                    }
                    /*
                     * Create a branch for this specific cases. Recursively
                     * pre-process the decom statements under this case,
                     * starting at the current offset.
                     */
                    this.currentBranch = new DecomPreprocessorBranch(
                            saveBranch, variableName, String.valueOf(caseVal));
                    preProcessStatements(statementsUnderCase, currentOffset);

                    /*
                     * Note that the switch variable can take the value
                     * indicated by this case
                     */
                    variable.addPotentialValue(caseVal);
                }

                /*
                 * If there are default statements, create a branch for that and
                 * process those statements in the same way as above.
                 */
                if (ss.getDefaultStatements() != null) {
                    final List<IDecomStatement> statementsUnderCase = ss
                            .getDefaultStatements();
                    this.currentBranch = new DecomPreprocessorBranch(
                            saveBranch, variableName, "DEFAULT");
                    preProcessStatements(statementsUnderCase, currentOffset);
                    variable.setAllowOther(true);
                }

                /*
                 * Pop back out to the branch we were on when we started the
                 * switch. We still remain at the same offset as well.
                 */
                this.currentBranch = saveBranch;

            } else {
                // shouldn't happen
                throw new IllegalStateException(
                        "Encountered unexpected statement type: "
                                + s.getClass().getName()
                                + " in decom map for APID "
                                + this.map.getApid());
            }

        }
    }

    /**
     * Outputs the string representation of the decom pre-processor tree to the
     * given print stream.
     * 
     * @param output
     */
    public synchronized void dumpTreeToStream(final PrintStream output) {

        output.println("# Decom Map Pre-Processor Tree for APID "
                + this.map.getApid() + " (" + this.map.getName() + ")");
        output.print(this.root);
    }

    /**
     * Main method for test purposes only.
     * 
     * @param args
     *            command line arguments
     */
    public static void main(final String[] args) {
        final DecomMapPreprocessor pp = new DecomMapPreprocessor(args[0],
                args[1]);
        pp.init(SpringContextFactory.getSpringContext(true));
        pp.buildTree();
        pp.dumpTreeToStream(System.out);
    }
}
