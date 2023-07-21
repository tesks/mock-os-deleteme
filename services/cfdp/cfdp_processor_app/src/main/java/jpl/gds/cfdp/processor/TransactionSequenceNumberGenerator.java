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
package jpl.gds.cfdp.processor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import cfdp.engine.ampcs.ITransactionSequenceNumberGenerator;
import jpl.gds.cfdp.processor.config.ConfigurationManager;
import jpl.gds.cfdp.processor.mib.MibManager;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

@Service
@DependsOn(value = {"configurationManager", "mibManager"})
public class TransactionSequenceNumberGenerator implements ITransactionSequenceNumberGenerator {

    private Tracer log;

    private static long[] maximumNumberTablePerByteLength = new long[]{
            0xFFL,
            0xFFFFL,
            0xFFFFFFL,
            0xFFFFFFFFL,
            0xFFFFFFFFFFL,
            0xFFFFFFFFFFFFL,
            0xFFFFFFFFFFFFFFL,
            0xFFFFFFFFFFFFFFFFL
    };

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    ConfigurationManager configurationManager;

    @Autowired
    MibManager mibManager;

    private long lastUsedSeqNum;

    @PostConstruct
    public void init() throws IOException {
        log = TraceManager.getTracer(appContext, Loggers.CFDP);

        try (Stream<String> stream = Files.lines(Paths.get(configurationManager.getTransactionSequenceNumberFile()))) {
            lastUsedSeqNum = Long.parseUnsignedLong(stream.iterator().next());
            log.info("Last transaction sequence number used: ", Long.toUnsignedString(lastUsedSeqNum));
        } catch (final NoSuchFileException nsfe) {
            lastUsedSeqNum = -1;
            log.warn("Transaction sequence number file ", configurationManager.getTransactionSequenceNumberFile(), " not found: New transaction will start with 0");
        } catch (final IOException ioe) {
            throw ioe;
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see cfdp.engine.ITransactionSequenceNumberGenerator#getNext()
     */
    @Override
    public synchronized long getNext() {

        /* MPCS-10177 - 9/28/2018
         * Check whether or not the next sequence number will violate the TSN field byte limit. If yes, cycle to 0.
         */
        int seqNumMaxBytes = mibManager.genTransSeqNumLength();

        if (seqNumMaxBytes < 1) {
            seqNumMaxBytes = 8;
        }

        long nextSeqNum;

        if (Long.compareUnsigned(lastUsedSeqNum + 1, maximumNumberTablePerByteLength[seqNumMaxBytes - 1]) > 0) {
            nextSeqNum = 0;
            log.info("Transaction sequence number cycling back to 0 because local entity ",
                    mibManager.getLocalEntityId(),
                    " has maximum ",
                    seqNumMaxBytes,
                    " bytes configured for TSNs in MIB.");
        } else {
            nextSeqNum = lastUsedSeqNum + 1;
            log.debug("Next TSN: ", nextSeqNum);
        }

        try {
            Files.write(Paths.get(configurationManager.getTransactionSequenceNumberFile()),
                    Long.toUnsignedString(nextSeqNum).getBytes());
        } catch (final IOException ioe) {
            log.error("Could not write last used transaction sequence number " + nextSeqNum + " to file " +
                    configurationManager.getTransactionSequenceNumberFile() + ": " + ExceptionTools.getMessage(ioe), ioe);
        }

        lastUsedSeqNum = nextSeqNum;
        return nextSeqNum;
    }

}