/*
 * Copyright 2006-2019. California Institute of Technology.
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
package jpl.gds.tc.mps.impl.ctt;

import gov.nasa.jpl.uplinkutils.CtsReturn;
import gov.nasa.jpl.uplinkutils.SWIGTYPE_p_void;
import gov.nasa.jpl.uplinkutils.UplinkUtils;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.tc.api.ITcTransferFrame;
import jpl.gds.tc.api.config.CommandFrameProperties;
import jpl.gds.tc.api.config.CommandProperties;
import jpl.gds.tc.api.config.VirtualChannelType;
import jpl.gds.tc.api.exception.CommandFileParseException;
import jpl.gds.tc.api.frame.ITcTransferFrameSerializer;
import jpl.gds.tc.mps.impl.ctt.repo.CommandTranslationTableFileRepository;
import jpl.gds.tc.mps.impl.ctt.repo.ICommandTranslationTableRepository;
import org.springframework.context.ApplicationContext;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Representation of the CTS command translation table loaded through JNI. Provides methods for working with the command
 * translation table and telecommand sessions.
 *
 *
 *  MPCS-11285 - 09/24/19 - Added sequence command info, updated returned VCID values to include the execution string
 */
public class CommandTranslationTable implements ICommandTranslationTableMetadataProvider {

    private final int               HDW_COMMAND_VCN;
    private final int               FSW_COMMAND_VCN;
    private final int               SEQ_COMMAND_VCN;
    private final String            commandDictionaryPath;
    private final String            sclkScetPath;
    private final int               scid;
    private final Tracer            tracer;
    // Handle to the in-memory data structure (CTS)
    private final SWIGTYPE_p_void   cttp;
    // Handle to the in-memory data structure for reverse CTT
    private final SWIGTYPE_p_void   rcttp;
    private final CommandProperties commandProperties;
    private final CommandFrameProperties cmdFrameProps;

    private final ICommandTranslationTableRepository          repository;
    // CTT metadata fields
    private       String                                      fswVersion;
    private       String                                      dictionaryVersion;
    private       Map<String, Integer>                        fileLoadTypeMap        = new HashMap<>(9);
    private       List<Integer>                               spacecraftIds          = new ArrayList<>();
    private       Map<String, InternalMpsCommandStemMetadata> commandStemMetadataMap = new HashMap<>();
    private       Map<Integer, String>                        argTypeMap             = new HashMap<>();

    private boolean initialized = false;


    /**
     * Constructor, creates internal Tracer logger with provided application context
     *
     * @param appContext            application context
     * @param commandDictionaryPath command dictionary file path
     * @param sclkScetPath          sclk scet file path
     * @param scid                  spacecraft id
     */
    public CommandTranslationTable(final String commandDictionaryPath, final String sclkScetPath, final int scid,
                                   final ApplicationContext appContext) throws CommandFileParseException {
        this(commandDictionaryPath, sclkScetPath, scid, appContext.getBean(CommandProperties.class),
                appContext.getBean(CommandFrameProperties.class), TraceManager.getTracer(appContext, Loggers.UPLINK));
    }

    /**
     * Constructor, uses basic Uplink Tracer (no Spring app context)
     *
     * @param commandDictionaryPath command dictionary file path
     * @param sclkScetPath          sclk scet file path
     * @param scid                  spacecraft id
     * @param commandProperties     command properties
     * @param cmdFrameProps         command frame properties
     * @param tracer                log tracer
     */
    public CommandTranslationTable(final String commandDictionaryPath, final String sclkScetPath, final int scid,
                                   final CommandProperties commandProperties,
                                   final CommandFrameProperties cmdFrameProps, final Tracer tracer) throws
                                                                                                    CommandFileParseException {
        this.HDW_COMMAND_VCN = cmdFrameProps.getVirtualChannelNumber(VirtualChannelType.HARDWARE_COMMAND);
        this.FSW_COMMAND_VCN = cmdFrameProps.getVirtualChannelNumber(VirtualChannelType.FLIGHT_SOFTWARE_COMMAND);
        this.SEQ_COMMAND_VCN = cmdFrameProps.getVirtualChannelNumber(VirtualChannelType.SEQUENCE_DIRECTIVE);
        this.commandDictionaryPath = commandDictionaryPath;
        this.sclkScetPath = sclkScetPath;
        this.scid = scid;
        this.commandProperties = commandProperties;
        this.cmdFrameProps = cmdFrameProps;
        this.tracer = tracer == null ? TraceManager.getTracer(Loggers.UPLINK) : tracer;
        this.repository = setupRepository();
        this.cttp = loadTranslationDatabase();
        this.rcttp = loadReverseCttp();
        this.initialized = true;
    }

    private SWIGTYPE_p_void loadReverseCttp() throws CommandFileParseException {
        final String reverseTtPath = repository.getReverseTranslationTablePath();

        // return a handle to the in-memory table
        // (this happens in native C code's memory space, please be careful to close it when finished)
        final CtsReturn       ctsReturn = new CtsReturn();
        final SWIGTYPE_p_void localRcttp     = UplinkUtils.cts_new_rctt_w(reverseTtPath, ctsReturn);
        if (localRcttp == null) {
            throw new CommandFileParseException(
                    String.format(
                            "Error loading command translation table for dictionary %s",
                            reverseTtPath));
        }

        return localRcttp;
    }

    private ICommandTranslationTableRepository setupRepository() throws CommandFileParseException {
        // check for a valid file
        final File file = new File(commandDictionaryPath);
        if (!file.exists()) {
            tracer.error("The command dictionary file does not exist.");
            throw new CommandFileParseException("The command dictionary file does not exist.");
        }

        // check to see if we have a compiled version
        return new CommandTranslationTableFileRepository(commandDictionaryPath, commandProperties, tracer);
    }

    /**
     * Load translation table into memory
     */
    private SWIGTYPE_p_void loadTranslationDatabase() throws CommandFileParseException {

        final String cmdDictPath = repository.getForwardTranslationTablePath();

        // return a handle to the in-memory table
        // (this happens in native C code's memory space, please be careful to close it when finished)
        final SWIGTYPE_p_void cttp = UplinkUtils.cts_new_ctt_w(cmdDictPath, sclkScetPath);
        if (cttp == null) {
            throw new CommandFileParseException(
                    String.format(
                            "Error loading command translation table for dictionary %s and SCLK/SCET file %s.",
                            cmdDictPath, sclkScetPath));
        }

        buildMetadata(cttp);

        return cttp;
    }


    /**
     * Indicates translation table initialization
     *
     * @return true if initialized, false if not
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Clean up any references used by the in-memory translation table (JNI)
     */
    public void cleanUp() {
        if (isInitialized()) {
            // need to clean up the memory
            UplinkUtils.ctts_destroy_w(cttp);
            //TODO: This call is crashing the JVM.
//            UplinkUtils.cts_delete_rctt_w(rcttp);
            this.initialized = false;
        }
    }

    /**
     * Return the opaque handle for the internal command translation table
     *
     * @return opaque handle
     */
    public SWIGTYPE_p_void getCommandTranslationPointer() {
        checkInitialized();
        return this.cttp;
    }

    /**
     * Return the opaque handle for the reverse translation table
     *
     * @return opaque handle
     */
    public SWIGTYPE_p_void getReverseTranslationTablePointer() {
        checkInitialized();
        return this.rcttp;
    }

    public String getSclkScetPath() {
        return this.sclkScetPath;
    }

    public int getScid() {
        return this.scid;
    }

    /**
     * Check to see if the class has been initialized
     */
    private void checkInitialized() {
        if (!initialized)
            throw new IllegalStateException("The command translation table has either not been initialized or has " +
                    "already been cleaned.");
    }

    private void buildMetadata(final SWIGTYPE_p_void cttp) throws CommandFileParseException {

        if (cttp == null) {
            throw new IllegalArgumentException("Command translation table can't be null");
        }

        /*
        Pre-build the translation table metadata, such as file types, stem names, etc.
         */

        // First, build the stem info in CTT

        if (UplinkUtils.ctts_build_steminfo_w(cttp) != 0) {
            throw new CommandFileParseException("ctts_build_steminfo_w error");
        }

        tracer.trace("ctts_build_steminfo_w succeeded");

        // Extract the FSW version

        fswVersion = UplinkUtils.ctts_fswversion_w(cttp);
        tracer.debug("ctts_fswversion_w = ", fswVersion);

        // Extract the dictionary version

        dictionaryVersion = UplinkUtils.ctts_dictversion_w(cttp);
        tracer.debug("ctts_dictversion_w = ", dictionaryVersion);

        // Extract the File Load types (names and corresponding number values)

        /*
         * TODO UplinkUtils currently doesn't provide any way to find out what file load type names are defined in the
         * table. When it becomes available, replace our brute-force, hardcoded workaround with call below.
         */
        // fileLoadTypeNames = UplinkUtils.?
        for (String typeName : Arrays.asList(
                "generic",
                "sequence",
                "load_and_go",
                "scfg",
                "eha",
                "sss",
                "mfsk",
                "unknown",
                "mcfsw")) {
            final int typeNum = UplinkUtils.ctts_uplink_filetype_w(cttp, typeName);

            if (typeNum >= 0) {
                fileLoadTypeMap.put(typeName, typeNum);
                tracer.debug("Added File Load type ", typeName, " = ", typeNum);
            } else {
                tracer.trace("ctts_uplink_filetype_w(", typeName, ") = ", typeNum);
            }

        }

        // Extract the spacecraft IDs

        for (int i = 0; ; i++) {
            final int retVal = UplinkUtils.ctts_get_scid_w(cttp, i);

            if (retVal != -1) {
                spacecraftIds.add(retVal);
                tracer.debug("Added SCID = ", retVal);
            } else {
                break;
            }

        }

        // Extract the list of command stem names

        for (int i = 0; ; i++) {
            final String stemName = UplinkUtils.ctts_get_stemname_w(cttp, i);

            if (stemName != null) {
                // Valid stem found
                InternalMpsCommandStemMetadata md = new InternalMpsCommandStemMetadata();
                commandStemMetadataMap.put(stemName, md);
                tracer.debug("Added command stem = ", stemName);

                // Extract stem's metadata
                md.setTitle(UplinkUtils.ctts_get_stem_title_by_stem_name_w(cttp, stemName))
                        .setOpcode(UplinkUtils.ctts_opcode_w(cttp, stemName))
                        .setModuleName(UplinkUtils.ctts_module_name_w(cttp, stemName));

                // Scan for category numbers
                for (int j = 0; ; j++) {
                    final int categoryNum = UplinkUtils.ctts_get_category_w(cttp, stemName, j);

                    if (categoryNum != -1) {
                        md.addCategoryNumber(categoryNum);
                    } else {
                        break;
                    }
                }

                // Scan for class numbers
                for (int j = 0; ; j++) {
                    final int classNum = UplinkUtils.ctts_get_class_w(cttp, stemName, j);

                    if (classNum != -1) {
                        md.addClassNumber(classNum);
                    } else {
                        break;
                    }

                }

                // Now determine arguments
                int numArgs = UplinkUtils.ctts_num_args_w(cttp, stemName);

                for (int j = 0; j < numArgs; j++) {
                    final SWIGTYPE_p_void argHandle = UplinkUtils.ctts_arg_w(cttp, stemName, j);

                    if (argHandle == null) {
                        throw new CommandFileParseException("Could not obtain handle for argument index " + j + "for " +
                                "command stem " + stemName);
                    }

                    final InternalMpsCommandArgumentMetadata argMetadata = new InternalMpsCommandArgumentMetadata();
                    md.addArgumentMetadata(argMetadata);

                    argMetadata.setTypeNum(UplinkUtils.ctts_arg_type_w(argHandle));

                    // TODO Look up name for type among UplinkUtils constants and add to argTypeMap

                    argMetadata.setEntryLength(UplinkUtils.ctts_arg_entry_length_w(argHandle))
                            .setResultLenth(UplinkUtils.ctts_arg_result_length_w(argHandle))
                            .setName(UplinkUtils.ctts_arg_name_w(argHandle))
                            .setTitle(UplinkUtils.ctts_arg_title_w(argHandle))
                            .setFormat(UplinkUtils.ctts_arg_format_w(argHandle))
                            .setDefaultValueExistsFlag(UplinkUtils.ctts_arg_default_flag_w(argHandle) == 1)
                            .setDefaultValue(UplinkUtils.ctts_arg_default_string_w(argHandle))
                            .setRange(UplinkUtils.ctts_arg_range_string_w(argHandle))
                            .setConversionRoutineName(UplinkUtils.ctts_arg_conv_name_w(argHandle))
                            .setMinRepeat(UplinkUtils.ctts_arg_minrep_w(argHandle))
                            .setMaxRepeat(UplinkUtils.ctts_arg_maxrep_w(argHandle));

                    // Extract enumeration keys

                    for (int k = 0; ; k++) {
                        final String enumKey = UplinkUtils.ctts_arg_enum_key_w(argHandle, k);

                        if (enumKey != null) {
                            argMetadata.addEnumKey(enumKey);
                        } else {
                            break;
                        }

                    }

                    argMetadata.setDefaultValue(UplinkUtils.ctts_arg_default_key_w(argHandle));

                }   // command stem argument list loop

            } else {
                // Stop scanning for more stems
                break;

            }   // end if/else command stem is valid

        }   // end command stems list loop

    }

    @Override
    public String getFswVersion() {
        checkInitialized();
        return fswVersion;
    }

    @Override
    public String getDictionaryVersion() {
        checkInitialized();
        return dictionaryVersion;
    }

    @Override
    public Map<String, Integer> getFileLoadTypeMap() {
        checkInitialized();
        return fileLoadTypeMap;
    }

    @Override
    public List<Integer> getSpacecraftIds() {
        checkInitialized();
        return spacecraftIds;
    }

    @Override
    public Set<String> getStemNames() {
        checkInitialized();
        return commandStemMetadataMap.keySet();
    }

    private void checkStemName(final String stemName) {

        if (!commandStemMetadataMap.containsKey(stemName)) {
            throw new IllegalStateException("No stem \"" + stemName + "\" found in command translation table");
        }

    }

    @Override
    public String getStemTitle(final String stemName) {
        checkInitialized();
        checkStemName(stemName);
        return commandStemMetadataMap.get(stemName).getTitle();
    }

    @Override
    public int getStemOpCode(final String stemName) {
        checkInitialized();
        checkStemName(stemName);
        return commandStemMetadataMap.get(stemName).getOpcode();
    }

    @Override
    public String getStemModuleName(final String stemName) {
        checkInitialized();
        checkStemName(stemName);
        return commandStemMetadataMap.get(stemName).getModuleName();
    }

    @Override
    public List<Integer> getStemCategoryNumbers(final String stemName) {
        checkInitialized();
        checkStemName(stemName);
        return commandStemMetadataMap.get(stemName).getCategoryNumbers();
    }

    @Override
    public List<Integer> getStemClassNumbers(final String stemName) {
        checkInitialized();
        checkStemName(stemName);
        return commandStemMetadataMap.get(stemName).getClassNumbers();
    }

    @Override
    public int getStemArgumentsCount(final String stemName) {
        checkInitialized();
        checkStemName(stemName);
        return commandStemMetadataMap.get(stemName).getArgumentsMetadata().size();
    }

    private void checkArgumentIndex(final String stemName, final int argumentIndex) {

        if (argumentIndex < 0 ||
                argumentIndex > commandStemMetadataMap.get(stemName).getArgumentsMetadata().size() - 1) {
            throw new IllegalStateException("Argument index " + argumentIndex + " is illegal - stem has "
                    + commandStemMetadataMap.get(stemName).getArgumentsMetadata().size() + " arguments");
        }

    }

    @Override
    public String getStemArgumentName(final String stemName, final int argumentIndex) {
        checkInitialized();
        checkStemName(stemName);
        checkArgumentIndex(stemName, argumentIndex);
        return commandStemMetadataMap.get(stemName).getArgumentsMetadata().get(argumentIndex).getName();
    }

    @Override
    public String getStemArgumentTitle(final String stemName, final int argumentIndex) {
        checkInitialized();
        checkStemName(stemName);
        checkArgumentIndex(stemName, argumentIndex);
        return commandStemMetadataMap.get(stemName).getArgumentsMetadata().get(argumentIndex).getTitle();
    }

    @Override
    public int getStemArgumentTypeNumber(final String stemName, final int argumentIndex) {
        checkInitialized();
        checkStemName(stemName);
        checkArgumentIndex(stemName, argumentIndex);
        return commandStemMetadataMap.get(stemName).getArgumentsMetadata().get(argumentIndex).getTypeNum();
    }

    @Override
    public String getStemArgumentType(final String stemName, final int argumentIndex) {
        return getArgumentTypeMap().get(getStemArgumentTypeNumber(stemName, argumentIndex));
    }

    @Override
    public Map<Integer, String> getArgumentTypeMap() {
        checkInitialized();
        /*
        TODO Need to be able to get full argument type listing and their numeric equivalent from CTS
         */
        throw new NotImplementedException();
    }

    @Override
    public int getStemArgumentEntryLength(final String stemName, final int argumentIndex) {
        checkInitialized();
        checkStemName(stemName);
        checkArgumentIndex(stemName, argumentIndex);
        return commandStemMetadataMap.get(stemName).getArgumentsMetadata().get(argumentIndex).getEntryLength();
    }

    @Override
    public int getStemArgumentResultLenth(final String stemName, final int argumentIndex) {
        checkInitialized();
        checkStemName(stemName);
        checkArgumentIndex(stemName, argumentIndex);
        return commandStemMetadataMap.get(stemName).getArgumentsMetadata().get(argumentIndex).getResultLenth();
    }

    @Override
    public String getStemArgumentFormat(final String stemName, final int argumentIndex) {
        checkInitialized();
        checkStemName(stemName);
        checkArgumentIndex(stemName, argumentIndex);
        return commandStemMetadataMap.get(stemName).getArgumentsMetadata().get(argumentIndex).getFormat();
    }

    @Override
    public boolean stemArgumentHasDefaultValue(final String stemName, final int argumentIndex) {
        checkInitialized();
        checkStemName(stemName);
        checkArgumentIndex(stemName, argumentIndex);
        return commandStemMetadataMap.get(stemName).getArgumentsMetadata().get(argumentIndex).hasDefaultValue();
    }

    @Override
    public String getStemArgumentDefaultValue(final String stemName, final int argumentIndex) {
        checkInitialized();
        checkStemName(stemName);
        checkArgumentIndex(stemName, argumentIndex);
        return commandStemMetadataMap.get(stemName).getArgumentsMetadata().get(argumentIndex).getDefaultValue();
    }

    @Override
    public String getStemArgumentRange(final String stemName, final int argumentIndex) {
        checkInitialized();
        checkStemName(stemName);
        checkArgumentIndex(stemName, argumentIndex);
        return commandStemMetadataMap.get(stemName).getArgumentsMetadata().get(argumentIndex).getRange();
    }

    @Override
    public String getStemArgumentConversionRoutineName(final String stemName, final int argumentIndex) {
        checkInitialized();
        checkStemName(stemName);
        checkArgumentIndex(stemName, argumentIndex);
        return commandStemMetadataMap.get(stemName).getArgumentsMetadata().get(argumentIndex)
                .getConversionRoutineName();
    }

    @Override
    public int getStemArgumentMinRepeat(final String stemName, final int argumentIndex) {
        checkInitialized();
        checkStemName(stemName);
        checkArgumentIndex(stemName, argumentIndex);
        return commandStemMetadataMap.get(stemName).getArgumentsMetadata().get(argumentIndex).getMinRepeat();

    }

    @Override
    public int getStemArgumentMaxRepeat(final String stemName, final int argumentIndex) {
        checkInitialized();
        checkStemName(stemName);
        checkArgumentIndex(stemName, argumentIndex);
        return commandStemMetadataMap.get(stemName).getArgumentsMetadata().get(argumentIndex).getMaxRepeat();
    }

    @Override
    public List<String> getStemArgumentEnumKeys(final String stemName, final int argumentIndex) {
        checkInitialized();
        checkStemName(stemName);
        checkArgumentIndex(stemName, argumentIndex);
        return commandStemMetadataMap.get(stemName).getArgumentsMetadata().get(argumentIndex).getEnumKeys();
    }

    @Override
    public String getStemArgumentDefaultEnumKey(final String stemName, final int argumentIndex) {
        checkInitialized();
        checkStemName(stemName);
        checkArgumentIndex(stemName, argumentIndex);
        return commandStemMetadataMap.get(stemName).getArgumentsMetadata().get(argumentIndex).getDefaultEnumKey();
    }

    public int getHardwareCommandVcid() {
        return getVcid(HDW_COMMAND_VCN);
    }

    public int getFlightSoftwareCommandVcid() {
        return getVcid(FSW_COMMAND_VCN);
    }

    public int getSequenceCommandVcid() {
        return getVcid(SEQ_COMMAND_VCN);
    }

    private int getVcid(int vcn) {
        int execution = cmdFrameProps.getStringIdVcidValue() & ITcTransferFrame.BITMASK_EXECUTION_STRING;
        int actualVcn = vcn & ITcTransferFrame.BITMASK_VC_NUMBER;
        return  (execution << ITcTransferFrame.EXECUTION_STRING_BIT_OFFSET) + actualVcn;
    }
}