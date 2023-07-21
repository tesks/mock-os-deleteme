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

package jpl.gds.tc.impl.cltu;

import jpl.gds.shared.checksum.BchAlgorithm;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.*;
import jpl.gds.tc.api.cltu.*;
import jpl.gds.tc.api.config.CltuProperties;
import jpl.gds.tc.api.config.PlopProperties;
import jpl.gds.tc.api.exception.CltuEndecException;
import jpl.gds.tc.api.frame.ITcTransferFrameSerializer;
import jpl.gds.tc.api.plop.ICommandLoadBuilder;
import jpl.gds.tc.impl.cltu.parsers.BchCodeBlockBuilder;
import jpl.gds.tc.impl.plop.CommandLoadBuilder;
import org.springframework.context.ApplicationContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A collection of factory methods for creating CLTUs and codeblocks
 * from telecommand frames.
 *
 *
 * MPCS-11285 - 09/24/19 - updated to use configured start and tail sequences in the ITcCltuBuilder
 */
public class CltuFactory implements ICltuFactory {
    protected final ApplicationContext appContext;
    protected final Tracer             trace;
    protected final PlopProperties     plopConfig;
    protected final CltuProperties     cltuConfig;
    protected final ICltuParser        cltuParser;
    protected ITcTransferFrameSerializer frameSerializer;

	/**
	 * @param appContext The current application context
	 */
	public CltuFactory(final ApplicationContext appContext) {
        this.appContext = appContext;
        this.trace = TraceManager.getTracer(appContext, Loggers.UPLINK);
        this.plopConfig = appContext.getBean(PlopProperties.class);
        this.cltuConfig = appContext.getBean(CltuProperties.class);
        this.cltuParser = appContext.getBean(ICltuParser.class);
        this.frameSerializer = appContext.getBean(ITcTransferFrameSerializer.class);
	}
	
	/**
	 * Given a set of telecommand frames for uplink, encode them into CLTUs and return the
	 * list of corresponding CLTU objects.  Generally there is one telecommand frame per CLTU,
	 * but this value can actually be configured on a per-mission basis.
	 * 
	 * @param frames The list of telecommand frames to encode.
	 * 
	 * @return A list of CLTU objects containing the data of the incoming telecommand frames
	 * in an encoded form.
	 */
    @Override
    public List<ICltu> createCltusFromFrames(final List<ITcTransferFrame> frames) throws CltuEndecException {

        ITcCltuBuilder cltuBuilder;
        byte[] start = cltuConfig.getStartSequence();
        byte[] tail = cltuConfig.getTailSequence();
		
		//figure out how many frames go in a CLTU
        final short framesPerCltu = cltuConfig.getFramesPerCltu();
        final List<ICltu> cltus = new ArrayList<>(frames.size() / framesPerCltu);

        trace.debug("Creating CLTU's from frames size=", frames.size(), " framesPerCltu=", framesPerCltu);

		for(ITcTransferFrame frame : frames) {
		    cltuBuilder = appContext.getBean(ITcCltuBuilder.class);

		    // MPCS-11856 - 8/6/2020 - jfwagner: removed code here to populate a cltuitem.
            // When building a CLTU using a frame instead of a cltuitem as parameter,
            // there is no need to set sequences or codeblocks. CTS sets them for you.

            byte[] frameBytes = frameSerializer.getBytes(frame);

            cltuBuilder.setFrameBytes(frameBytes);

			cltus.add(cltuParser.parse(cltuBuilder.build()));
		}

		//needed because the CLTU builder automatically adds the acquisition and idle sequence to EVERY cltu and we don't want that
		cltus.forEach(cltu -> {
		    cltu.setAcquisitionSequence(new byte[0]);
		    cltu.setIdleSequence(new byte[0]);});

        trace.debug("Successfully built ", cltus.size(), " CLTU's");
        return (cltus);
	}

	public List<IBchCodeblock> createBchCodeblocksFromFrame(ITcTransferFrame frame) {
        int dataLen = cltuConfig.getCodeBlockDataByteLength();
        byte fill = GDR.parse_byte("0x" + cltuConfig.getFillByteHex());

        byte[] frameBytes = frameSerializer.getBytes(frame);

        ByteArrayInputStream bais = new ByteArrayInputStream(frameBytes);
        byte[] bchBytes;

        IBchCodeBlockBuilder bchBuilder;
        List<IBchCodeblock> bchBlocks = new ArrayList<>();
        bchBuilder = new BchCodeBlockBuilder();


        while(bais.available() > 0) {
            bchBytes = new byte[dataLen];
            Arrays.fill(bchBytes, fill);

            try {
                bais.read(bchBytes);
            } catch (IOException e) {}

            bchBuilder.setData(bchBytes);
            bchBuilder.setEdac(BchAlgorithm.doEncode(bchBytes));

            bchBlocks.add(bchBuilder.build());
        }
        return bchBlocks;
    }

    @Override
    public List<ICltu> createPlopCltusFromFrames(final List<ITcTransferFrame> frames) throws CltuEndecException {
        final ICommandLoadBuilder loadBuilder = new CommandLoadBuilder(createCltusFromFrames(frames));
        trace.debug("Plopifying cltu");
        return loadBuilder.getPlopCltus(plopConfig);
    }

	@Override
	public ICltu parseCltuFromBytes(final byte[] data) throws CltuEndecException {
		return appContext.getBean(ICltuParser.class).parse(data);
	}

    /**
     * MPCS-9532 3/23/18: Initial implementation for extracting multiple CLTU's from a byte array
     * WARNING: Current implementation does not correctly handle splitting the ending CLTU idle sequence and next cltu's
     * start sequence. This should be done based on the plop type configuration.
     * 
     * For the purpose of MPCS-9532: This implementation is sufficient because I just need the data out of the TC frames
     * within the CLTU. The R8 account is being overrun so the remaining work is a TODO for another JIRA.
     * 
     * {@inheritDoc}
     */
    @Override
    public List<ICltu> parseCltusFromBytes(final byte[] data) throws CltuEndecException {
        final String cltuHex = BinOctHexUtility.toHexFromBytes(data);
        final String tailSeqHex = cltuConfig.getTailSequenceHex().toLowerCase();
        final String startSeqHex = cltuConfig.getStartSequenceHex().toLowerCase();
        
        int tailPosition = 0;
        int startSeqIndex = 0;

        trace.trace("Received cltu  ", cltuHex, "\nAttempting to parse cltus from ", data.length,
                    " bytes. Looking for start sequence ", startSeqHex, " and tail sequence ", tailSeqHex);
        
        final List<byte[]> rawCltus = new ArrayList<>();
        do {
            startSeqIndex = cltuHex.indexOf(startSeqHex, tailPosition);
            if (startSeqIndex == -1) {
                if (!rawCltus.isEmpty()) {
                    // At least 1 valid cltu was processed, continue instead of throwing
                    break;
                } else { 
                    throw new CltuEndecException("Could not find the start sequence \"" + startSeqHex + "\" on the input CLTU");
                }
            }
            tailPosition = cltuHex.indexOf(tailSeqHex, startSeqIndex);
            if (tailPosition == -1) { 
                if (!rawCltus.isEmpty()) {
                    // At least 1 valid cltu was processed, continue instead of throwing
                    break;
                } else { 
                    throw new CltuEndecException("Could not find the tail sequence \"" + tailSeqHex + "\" on the input CLTU");
                }
            }
            tailPosition += tailSeqHex.length();

            final String cltuAsHex = cltuHex.substring(startSeqIndex, tailPosition);
            final byte[] aCltu = BinOctHexUtility.toBytesFromHex(cltuAsHex);
            rawCltus.add(aCltu);
            trace.trace("Extracted cltu ", BinOctHexUtility.formatHexString(BinOctHexUtility.toHexFromBytes(aCltu), 40),
                        "\n. Remaining data: ", cltuHex.substring(tailPosition));

        } while ((tailPosition + startSeqHex.length() + tailSeqHex.length()) < cltuHex.length());

        final List<ICltu> cltuList = new ArrayList<>();

        for (final byte[] cltu : rawCltus) {
            cltuList.add((appContext.getBean(ICltuParser.class).parse(cltu)));
        }

        return cltuList;
    }
}