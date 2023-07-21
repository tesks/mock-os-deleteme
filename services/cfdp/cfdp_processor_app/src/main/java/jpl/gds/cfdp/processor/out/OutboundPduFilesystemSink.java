/*
 * Copyright 2006-2019. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */

package jpl.gds.cfdp.processor.out;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.SYNC;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import cfdp.engine.CommLink;
import cfdp.engine.Data;
import cfdp.engine.ID;
import cfdp.engine.PDUType;
import cfdp.engine.TransID;
import jpl.gds.cfdp.processor.config.ConfigurationManager;
import jpl.gds.cfdp.processor.error.DirectoryDoesNotExistException;
import jpl.gds.cfdp.processor.gsfc.util.GsfcUtil;
import jpl.gds.cfdp.processor.stat.StatManager;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

@Service
@DependsOn("configurationManager")
public class OutboundPduFilesystemSink implements CommLink {

    private Tracer log;

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private ConfigurationManager configurationManager;

    @Autowired
    private StatManager statManager;

    @Autowired
    private GsfcUtil gsfcUtil;

    private String directoryPathStr;
    private String filePrefix;
    private String fileExtension;
    private int fileNumberWidth;
    private long lastFileNumber;
    private StringBuilder sb;
    private int sbPrefixLen;

    @Autowired
    private OutboundPduInternalStatManager outboundPduInternalStatManager;

    @PostConstruct
    public void init() throws DirectoryDoesNotExistException {
        log = TraceManager.getTracer(appContext, Loggers.CFDP);
        directoryPathStr = configurationManager.getOutboundPduFilesystemSinkDirectory();

        if (Files.notExists(Paths.get(directoryPathStr))) {
            throw new DirectoryDoesNotExistException(
                    "Outbound PDU filesystem sink directory " + directoryPathStr + " does not exist");
        }

        filePrefix = configurationManager.getOutboundPduFilesystemSinkFilePrefix();
        fileExtension = configurationManager.getOutboundPduFilesystemSinkFileExtension();
        fileNumberWidth = configurationManager.getOutboundPduFilesystemSinkFileNumberWidth();

        // Determine the next file numbering

        final File writeDirectory = new File(directoryPathStr);
        final File[] allFilesInWriteDirectory = writeDirectory.listFiles();
        long largestNumber = -1;
        final Pattern outboundPduWriteFilePattern = Pattern.compile(filePrefix + "([0-9]+)" + fileExtension);

        for (final File f : allFilesInWriteDirectory) {
            final Matcher matcher = outboundPduWriteFilePattern.matcher(f.getName());

            if (matcher.find()) {
                final long newNum = Long.parseLong(matcher.group(1));

                if (newNum > largestNumber) {
                    largestNumber = newNum;
                }

            }

        }

        lastFileNumber = largestNumber;

        sb = new StringBuilder(
                directoryPathStr.length() + filePrefix.length() + fileNumberWidth + fileExtension.length() + 2);
        sb.append(directoryPathStr);

        if (!directoryPathStr.endsWith(File.separator)) {
            sb.append(File.separator);
        }

        sb.append(filePrefix);
        sbPrefixLen = sb.length();
    }

    @Override
    public boolean open(final ID partnerId) {
        log.trace("open " + partnerId);
        return true;
    }

    @Override
    public boolean ready(final PDUType pduType, final TransID transID, final ID partnerID) {

        if (configurationManager.isOutboundPduEnabled() && outboundPduInternalStatManager.getPduSentTimestamp()
                + configurationManager.getOutboundPduFilesystemMinimumWriteIntervalMillis() <= System
                .currentTimeMillis()) {
            log.trace("ready true " + pduType + " " + gsfcUtil.convertEntityId(transID.getSource()) + ":"
                    + Long.toUnsignedString(transID.getNumber()) + " -> " + gsfcUtil.convertEntityId(partnerID));
            return true;
        } else {
            log.trace("ready false " + pduType + " " + gsfcUtil.convertEntityId(transID.getSource()) + ":"
                    + Long.toUnsignedString(transID.getNumber()) + " -> " + gsfcUtil.convertEntityId(partnerID));
            return false;
        }

    }

    @Override
    public void send(final TransID transID, final ID partnerID, final Data pdu) {
        log.trace("send " + gsfcUtil.convertEntityId(transID.getSource()) + ":"
                + Long.toUnsignedString(transID.getNumber()) + " -> "
                + gsfcUtil.convertEntityId(partnerID) + " PDU " + pdu.length + " bytes");

        final String filename = getNextFileStr();

        try {
            Files.write(Paths.get(filename), pdu.get(), WRITE, CREATE, TRUNCATE_EXISTING, SYNC);
            outboundPduInternalStatManager.setPduSentThisCycle(true);
            outboundPduInternalStatManager.setPduSentTimestamp(System.currentTimeMillis());
            statManager.setPduOutOk(true);
        } catch (final IOException ie) {
            statManager.setPduOutOk(false);
            log.error("Could not write PDU to file " + filename + ": " + ExceptionTools.getMessage(ie), ie);
        }

    }

    private String getNextFileStr() {
        sb.setLength(sbPrefixLen);
        sb.append(String.format("%0" + fileNumberWidth + "d", ++lastFileNumber));
        sb.append(fileExtension);
        return sb.toString();
    }

}