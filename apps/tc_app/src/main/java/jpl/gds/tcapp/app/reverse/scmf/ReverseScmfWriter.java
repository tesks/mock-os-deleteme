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

package jpl.gds.tcapp.app.reverse.scmf;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.shared.config.ReleaseProperties;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.time.SclkScetConverter;
import jpl.gds.shared.time.SclkScetUtility;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.ICommandWriteUtility;
import jpl.gds.tc.api.IScmf;
import jpl.gds.tc.api.message.IScmfCommandMessage;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;

/**
 * Reverse SCMF output writer
 *
 * 8/3/19 moved logic from ScmfReverseApp to here
 * 6/29/20 Flush after each section to avoid log messages mixing with output
 */
public class ReverseScmfWriter {

    private static final String DASHED_LINE = "=============================\n";

    private final String               cmdDictPath;
    private final String               dictionaryVersion;
    private final ICommandWriteUtility cmdWriteUtil;

    private IScmf       scmf;
    private PrintWriter pw;
    private Tracer      trace = TraceManager.getDefaultTracer();

    /**
     * @param dictionaryProperties
     * @param cmdWriteUtil         command write utility
     * @throws DictionaryException if a problem occurs loading dictionary properties
     */
    public ReverseScmfWriter(final DictionaryProperties dictionaryProperties,
                             final ICommandWriteUtility cmdWriteUtil) throws
                                                                      DictionaryException {
        this.cmdDictPath = dictionaryProperties.findFileForSystemMission(DictionaryType.COMMAND);
        this.dictionaryVersion = dictionaryProperties.getFswVersion();
        this.cmdWriteUtil = cmdWriteUtil;
    }

    /**
     * Set the log tracer
     *
     * @param tracer
     */
    public void setTracer(final Tracer tracer) {
        this.trace = tracer;
    }

    public void doReverse(final IScmf scmf, final PrintWriter writer) throws DictionaryException {

        this.scmf = scmf;

        this.pw = writer;

        writeGeneralInformation();
        writeScmfHeaders();
        writeCommandMessages();

        cleanUp();
    }

    private void cleanUp() {
        pw.write(ICommandWriteUtility.DOUBLE_LINE);

        pw.flush();
        pw.close();
        this.scmf = null;
        this.pw = null;
    }

    /**
     * Write out top-level information
     *
     * @throws DictionaryException If the command dictionary cannot be parsed
     */
    private void writeGeneralInformation() throws DictionaryException {
        //get the command dictionary
        final File cmdDictFile = new File(cmdDictPath);
        if (!cmdDictFile.exists()) {
            throw new DictionaryException(
                    "The command dictionary " + cmdDictFile.getAbsolutePath() + " does not exist.");
        }

        //get the SCLK/SCET conversion file
        final int               scid            = (int) scmf.getSpacecraftId();
        final SclkScetConverter converter       = SclkScetUtility.getConverterFromSpacecraftId(scid, trace);
        String                  sclkScetFileStr = "No valid conversion file found.";
        if (converter != null) {
            final File sclkScetFile = new File(converter.getFilename());
            if (!sclkScetFile.exists()) {
                sclkScetFileStr = "SCLK/SCET conversion file does not exist.";
            } else {
                sclkScetFileStr = sclkScetFile.getAbsolutePath();
            }
        }

        //display the results
        pw.write(DASHED_LINE);
        pw.write("SCMF Parser Settings\n");
        pw.write(DASHED_LINE + "\n");
        pw.write("MPCS SCMF Reverse Software Version = " + ReleaseProperties.getVersion() + "\n");
        pw.write("SCMF File = " + scmf.getOriginalFile() + "\n");
        pw.write("Command Dictionary = " + cmdDictFile.getAbsolutePath() + "\n");
        pw.write("Command Dictionary Version = " + dictionaryVersion + "\n");
        pw.write("SCLK/SCET File = " + sclkScetFileStr + "\n");
        pw.write(ICommandWriteUtility.DOUBLE_LINE);
        pw.flush();
    }

    /**
     * Write out the various pieces of SCMF high-level metadata
     */
    private void writeScmfHeaders() {
        pw.write(DASHED_LINE);
        pw.write("SFDU Header Information\n");
        pw.write(DASHED_LINE);
        pw.write(scmf.getSfduHeader().getHeaderString());
        pw.write(ICommandWriteUtility.DOUBLE_LINE);

        pw.write(DASHED_LINE);
        pw.write("SCMF Header Information\n");
        pw.write(DASHED_LINE);
        pw.write(scmf.getHeaderString());
        pw.write(ICommandWriteUtility.DOUBLE_LINE);
        pw.flush();
    }

    /**
     * Write out the various command messages (generally CommandMessage = CLTU) in the SCMF
     */
    private void writeCommandMessages() {
        pw.write(DASHED_LINE);
        pw.write("Spacecraft Command Messages\n");
        pw.write(DASHED_LINE + "\n");

        final List<IScmfCommandMessage> messages = scmf.getCommandMessages();
        if (messages.isEmpty()) {
            pw.write("No Spacecraft Command Messages found in SCMF\n\n\n");
        } else {
            pw.write("Total Spacecraft Command Messages (CLTUs) in SCMF = " + messages.size() + "\n\n\n");
        }

        int totalCommandBits = 0;
        //write out each individual command message
        for (int i = 0; i < messages.size(); i++) {
            try {
                totalCommandBits += messages.get(i).getData().length * 8;;
                cmdWriteUtil.writeCltu(pw, messages.get(i).getCltuFromData(), i);
                pw.flush();
            } catch (final Exception cfe) {
                writeBadSpacecraftCommandMessage(messages.get(i).getData(), i);
            }
        }
        pw.write("Total command bit length: " + totalCommandBits);
    }

    /**
     * Write out a command message that could not be interpreted
     *
     * @param bytes      The bytes that were unable to be interpreted as a command message
     * @param cltuNumber The number of the command message in the file
     */
    private void writeBadSpacecraftCommandMessage(final byte[] bytes,
                                                  final int cltuNumber) {
        pw.write(
                "~~~~~~~~~~~~~~~~~~~~~~~Spacecraft Command Message #" + (cltuNumber + 1) + "~~~~~~~~~~~~~~~~~~~~~~~\n");

        pw.write("\nSpacecraft Command Message #" + (cltuNumber + 1) + " could not be interpreted as a CLTU.\n\n");
        pw.write("Spacecraft Command Message Data (Hex) = \n\n" + BinOctHexUtility
                .formatHexString(BinOctHexUtility.toHexFromBytes(bytes),
                        ICommandWriteUtility.HEX_CHARS_PER_LINE) + ICommandWriteUtility.DOUBLE_LINE);

        pw.write("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n\n");
        pw.flush();
    }
}
