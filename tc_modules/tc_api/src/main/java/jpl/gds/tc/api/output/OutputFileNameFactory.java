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
package jpl.gds.tc.api.output;

import jpl.gds.context.api.IGeneralContextInformation;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.tc.api.command.ICommand;
import jpl.gds.tc.api.command.IFlightCommand;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.util.Date;

/**
 * A utility class for creating names for output files produced by the uplink flow.  User given names are allowed for
 * some of these products, but if the user does not supply the information, we must make a name.
 * <p>
 * Every piece of uplink that is transmitted via chill_up also generates an SCMF by default.  This class assists in
 * generating names for those SCMFs if the user does not supply them.
 *
 *
 * 2019-06-18 - added methods that do not require a spring application context, and updated javadoc
 */
public class OutputFileNameFactory {
    /**
     * The extension given to an output SCMF file.
     */
    public static final String SCMF_EXTENSION = ".scmf";
    /**
     * The extension given to an output raw data file in hex format.
     */
    public static final String HEX_EXTENSION  = ".hex";
    /**
     * The extension given to an output raw data file in binary format.
     */
    public static final String RAW_EXTENSION  = ".raw";

    /**
     * Generate a name for saving an output file to disk. The filename will follow the format:
     *
     * [mission]_[middle]_[datetime].[suffix]
     *
     * @param appContext spring application context
     * @param middle     the middle portion of the filename
     * @param suffix     the end of the filename
     * @param includeDir True if the full directory path should be appended, false otherwise
     * @return A string representing a valid, usable name for a file on the filesystem.
     */
    public static String createName(final ApplicationContext appContext, final String middle, final String suffix,
                                    final boolean includeDir) {
        return createName(appContext.getBean(IGeneralContextInformation.class), middle, suffix, includeDir);
    }

    /**
     * Generate a name for saving an output file to disk. The filename will follow the format:
     *
     * [mission]_[middle]_[datetime].[suffix]
     *
     * @param generalContextInformation general context information object
     * @param middle                    The middle portion of the filename
     * @param suffix                    The end of the filename
     * @param includeDir                True if the full directory path should be appended, false otherwise
     * @return A string representing a valid, usable name for a file on the filesystem.
     */
    public static String createName(final IGeneralContextInformation generalContextInformation, final String middle,
                                    final String suffix, final boolean includeDir) {

        //prefix with the mission name
        String constructedScmfName = GdsSystemProperties
                .getSystemMissionIncludingSse(generalContextInformation.getSseContextFlag().isApplicationSse()) + "_";

        //get the session output directory
        if (includeDir) {
            String scmfDir = null;
            try {
                scmfDir = generalContextInformation.getOutputDir();
            } catch (final IllegalStateException ise) {
                scmfDir = ".";
            }
            final File dir = new File(scmfDir);
            if (!dir.exists()) {
                if (dir.mkdirs()) {
                    constructedScmfName = scmfDir + File.separator + constructedScmfName;
                }
            } else {
                constructedScmfName = scmfDir + File.separator + constructedScmfName;
            }
        }


        constructedScmfName += middle;

        //add a timestamp
        constructedScmfName += "_" + TimeUtility.getFormatter().format(new Date(System.currentTimeMillis())) + suffix;

        return (constructedScmfName);
    }

    /**
     * Generate a name for saving an output file to disk.  Always appends full directory path. The filename will follow
     * the format:
     *
     * [mission]_[middle]_[datetime].[suffix]
     *
     * @param middle The middle portion of the filename
     * @param suffix The end of filename
     * @return A string representing a valid, usable name for a file on the filesystem.
     */
    public static String createName(final ApplicationContext appContext, final String middle, final String suffix) {
        return (createName(appContext, middle, suffix, true));
    }

    /**
     * Generate a name for saving an output file to disk.  Always appends full directory path.
     * The filename will follow the format:
     *
     * [mission]_[middle]_[datetime].[suffix]
     *
     * @param generalContextInformation general context information object
     * @param middle                    The middle portion of the filename
     * @param suffix                    The end of filename
     * @return A string representing a valid, usable name for a file on the filesystem.
     */
    public static String createName(final IGeneralContextInformation generalContextInformation, final String middle,
                                    final String suffix) {
        return (createName(generalContextInformation, middle, suffix, true));
    }

    /**
     * Generate a name for saving an output file to disk.  Always appends full directory path. Suffix will always be an
     * SCMF file extension. The file name will follow the format:
     *
     * [mission]_[middle]_[datetime].scmf
     *
     * @param middle The middle portion of the filename
     * @return A string representing a valid, usable name for a file on the filesystem.
     */
    public static String createName(final ApplicationContext appContext, final String middle) {
        return (createName(appContext, middle, SCMF_EXTENSION));
    }

    /**
     * Generate a name for saving an output file to disk.  Always appends full directory path. Suffix will always be an
     * SCMF file extension.
     *
     * @param generalContextInformation general context information object
     * @param middle                    The middle portion of the filename
     * @return A string representing a valid, usable name for a file on the filesystem.
     */
    public static String createName(final IGeneralContextInformation generalContextInformation, final String middle) {
        return (createName(generalContextInformation, middle, SCMF_EXTENSION));
    }

    /**
     * Create an SCMF name for a command list file. The output filename will include "CommandListFile", eg:
     * <p>
     * [mission]_CommandListFile_[datatime].scmf
     *
     * @return The auto-generated name for an SCMF built from a command list file.
     */
    public static String createNameForCommandListFile(final ApplicationContext appContext) {
        return (createName(appContext, "CommandListFile"));
    }

    /**
     * Create an SCMF name for a command list file. The output filename will include "CommandListFile", eg:
     * <p>
     * [mission]_CommandListFile_[datatime].scmf
     *
     * @param generalContextInformation general context information object
     * @return The auto-generated name for an SCMF built from a command list file.
     */
    public static String createNameForCommandListFile(final IGeneralContextInformation generalContextInformation) {
        return (createName(generalContextInformation, "CommandListFile"));
    }

    /**
     * Create an SCMF name for a file load. The output filename will include "FileLoad", eg:
     * <p>
     * [mission]_FileLoad_[datatime].scmf
     *
     * @return The auto-generated name for an SCMF built from a file load.
     */
    public static String createNameForFileLoad(final ApplicationContext appContext) {
        return (createName(appContext, "FileLoad"));
    }

    /**
     * Create an SCMF name for a file load. The output filename will include "FileLoad", eg:
     * <p>
     * [mission]_FileLoad_[datatime].scmf
     *
     * @param generalContextInformation general context information object
     * @return The auto-generated name for an SCMF built from a file load.
     */
    public static String createNameForFileLoad(final IGeneralContextInformation generalContextInformation) {
        return (createName(generalContextInformation, "FileLoad"));
    }

    /**
     * Create an SCMF name for a PDU file. The output filename will include "PduFile", eg:
     * <p>
     * [mission]_PduFile_[datatime].scmf
     *
     * @param appContext the current application context
     * @return The auto-generated name for an SCMF built from a PDU file.
     */
    public static String createNameForPduFile(final ApplicationContext appContext) {
        return createName(appContext, "PduFile");
    }

    /**
     * Create an SCMF name for a PDU file. The output filename will include "PduFile", eg:
     * <p>
     * [mission]_PduFile_[datatime].scmf
     *
     * @param generalContextInformation general context information object
     * @return The auto-generated name for an SCMF built from a PDU file.
     */
    public static String createNameForPduFile(final IGeneralContextInformation generalContextInformation) {
        return createName(generalContextInformation, "PduFile");
    }

    /**
     * Create an SCMF name for a raw data file. The output filename will include "RawUplinkData", eg:
     * <p>
     * [mission]_RawUplinkData_[datatime].scmf
     *
     * @return The auto-generated name for an SCMF built from a raw data file.
     */
    public static String createNameForRawUplinkData(final ApplicationContext appContext) {
        return (createName(appContext, "RawUplinkData"));
    }

    /**
     * Create an SCMF name for a raw data file. The output filename will include "RawUplinkData", eg:
     * <p>
     * [mission]_RawUplinkData_[datetime].scmf
     *
     * @param generalContextInformation general context information object
     * @return The auto-generated name for an SCMF built from a raw data file.
     */
    public static String createNameForRawUplinkData(final IGeneralContextInformation generalContextInformation) {
        return (createName(generalContextInformation, "RawUplinkData"));
    }

    /**
     * Create an SCMF name for a single command. The file name will follow the format:
     * <p>
     * [mission]_[optional-command-stem]_[datetime].scmf
     *
     * @return The auto-generated name for an SCMF built from a single command.
     */
    public static String createNameForCommand(final ApplicationContext appContext, final ICommand command) {
        if (command instanceof IFlightCommand) {
            return (createName(appContext, ((IFlightCommand) command).getDefinition().getStem()));
        }

        return (createName(appContext, ""));
    }

    /**
     * Create an SCMF name for a single command. The file name will follow the format:
     * <p>
     * [mission]_[optional-command-stem]_[datetime].scmf
     *
     * @param generalContextInformation general context information object
     * @param command                   command object
     * @return The auto-generated name for an SCMF built from a single command.
     */
    public static String createNameForCommand(final IGeneralContextInformation generalContextInformation,
                                              final ICommand command) {
        if (command instanceof IFlightCommand) {
            return (createName(generalContextInformation, ((IFlightCommand) command).getDefinition().getStem()));
        }

        return (createName(generalContextInformation, ""));
    }
}
