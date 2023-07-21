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

package jpl.gds.tc.mps.impl.cltu.parsers;

import gov.nasa.jpl.tcsession.TcSession;
import gov.nasa.jpl.uplinkutils.UplinkUtils;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.IBchCodeblock;
import jpl.gds.tc.api.cltu.ICltu;
import jpl.gds.tc.api.ITcTransferFrame;
import jpl.gds.tc.api.cltu.IBchCodeBlockBuilder;
import jpl.gds.tc.api.cltu.ICltuBuilder;
import jpl.gds.tc.api.exception.CltuEndecException;
import jpl.gds.tc.api.exception.FrameWrapUnwrapException;
import jpl.gds.tc.impl.cltu.parsers.BchCodeBlockBuilder;
import jpl.gds.tc.impl.cltu.parsers.CltuBuilder;
import jpl.gds.tc.mps.impl.frame.parsers.MpsTcTransferFrameParser;
import jpl.gds.tc.mps.impl.session.MpsSession;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static jpl.gds.shared.util.BinOctHexUtility.HEX_STRING_PREFIX1;

/**
 * CLTU parser using MPSA's CTS library (UplinkUtils)
 *
 *
 * MPCS-11285 - 09/24/19 - added ability to set default start and tail sequences, consolidated logic of parsing to one function.
 */
public class MpsCltuParser implements IMpsCltuParser {

    private final int scid;

    private final byte[] startSequence;
    private final byte[] tailSequence;

    private int fecfByteLength = 2;

    /**
     * Constructor
     *
     * @param scid spacecraft ID
     */
    public MpsCltuParser(final int scid) {
        this(scid, null, null);
    }

    public MpsCltuParser(final int scid, final byte[] startSequence, final byte[] tailSequence) {
        this.scid = scid;
        this.startSequence = startSequence;
        this.tailSequence = tailSequence;
    }

    /**
     * Set the TC frame FECF byte length
     * @param fecfByteLength
     */
    public void setFecfByteLength(int fecfByteLength) {
        this.fecfByteLength = fecfByteLength;
    }

    /**
     * Reset the TC frame FECF byte length to 2
     */
    public void resetFecfByteLength() {
        this.fecfByteLength = 2;
    }

    @Override
    public ICltu parse(final byte[] cltuBytes) throws CltuEndecException {
        return parse(cltuBytes, true);
    }

    @Override
    public ICltu parse(final byte[] cltuBytes, final boolean validateBch) throws CltuEndecException {
        return parse(cltuBytes, startSequence, tailSequence, validateBch);
    }

    @Override
    public ICltu parse(final byte[] cltuBytes, final byte[] startSequence, final byte[] tailSequence,
                       final boolean validateBch) throws CltuEndecException {
        try (final MpsSession session = new MpsSession(scid)) {
            if(startSequence != null) {
                final boolean startSequenceSet = session
                        .setCltuStartSequence(BinOctHexUtility.toHexFromBytes(startSequence));

                if (!startSequenceSet) {
                    throw new IllegalArgumentException("An error occurred setting the start sequence to " + BinOctHexUtility
                            .toHexFromBytes(startSequence));
                }
            }

            if(tailSequence != null) {
                final boolean tailSequenceSet = session.setCltuTailSequence(BinOctHexUtility.toHexFromBytes(tailSequence));

                if (!tailSequenceSet) {
                    throw new IllegalArgumentException("An error occurred setting the tail sequence to " + BinOctHexUtility
                            .toHexFromBytes(tailSequence));
                }
            }

            final TcSession.cltuitem cltuItem = session.getCltuItem(cltuBytes, validateBch);

            session.restoreCltuStartSequence();
            session.restoreCltuTailSequence();

            return parse(cltuItem);
        }
    }

    @Override
    public ICltu parse(final TcSession.cltuitem cltuItem) throws CltuEndecException {
        final ICltuBuilder builder = new CltuBuilder();

        setSequences(cltuItem, builder);

        final List<IBchCodeblock> codeblocks = getCodeblocks(cltuItem);
        builder.setCodeblocks(codeblocks);

        final byte[] dataBytes = getDataBytes(codeblocks);
        builder.setData(dataBytes);

        try {
            final ITcTransferFrame frame = getTcTransferFrame(cltuItem);
            builder.setFrames(Collections.singletonList(frame));
        } catch (final FrameWrapUnwrapException e) {
            TraceManager.getDefaultTracer().debug("A TC transfer frame could not be found in the following CLTU item. The CLTU will not have a frame object");
            TraceManager.getDefaultTracer().debug(UplinkUtils.bintoasciihex(cltuItem.data, cltuItem.datalen, 1));
        }

        return builder.build();
    }

    private ITcTransferFrame getTcTransferFrame(final TcSession.cltuitem cltuItem) throws FrameWrapUnwrapException {
        try (final MpsSession session = new MpsSession(scid)) {
            final TcSession.frmitem frameItem = session.getFrameItem(cltuItem, fecfByteLength == 2);
            MpsTcTransferFrameParser parser = new MpsTcTransferFrameParser(scid);
            if (fecfByteLength == 2) {
                return parser.parse(frameItem);
            } else {
                parser.setFecLength(fecfByteLength);
                return parser.parse(frameItem,false);
            }
        }
    }

    private void setSequences(final TcSession.cltuitem cltuItem, final ICltuBuilder builder) {
        final String startSeq = cltuItem.ssqAhstr;
        final String tailSeq  = cltuItem.tsqAhstr;
        final String acqSeq   = cltuItem.asqAhstr;

        if (startSeq != null) {
            builder.setStartSequence(fromHexStringToByteArray(startSeq));
        }
        if (tailSeq != null) {
            builder.setTailSequence(fromHexStringToByteArray(tailSeq));
        }
        if (acqSeq != null) {
            builder.setAcquisitionSequence(fromHexStringToByteArray(acqSeq));
        }
    }

    private byte[] getDataBytes(final List<IBchCodeblock> codeblocks) throws CltuEndecException {
        final ByteArrayOutputStream dataBytesOutputStream = new ByteArrayOutputStream();
        for (final IBchCodeblock cb : codeblocks) {
            try {
                dataBytesOutputStream.write(cb.getBytes());
            } catch (final IOException e) {
                throw new CltuEndecException("Error writing bytes to output stream.");
            }
        }
        return dataBytesOutputStream.toByteArray();
    }

    private List<IBchCodeblock> getCodeblocks(final TcSession.cltuitem cltuItem) {
        final List<IBchCodeblock> codeblocks = new ArrayList<>();

        final List<byte[]> byteCodeblocks = cltuItem.cbAhstrAry.stream().map(MpsCltuParser::fromHexStringToByteArray)
                .collect(Collectors.toList());
        final List<byte[]> byteEdacs = cltuItem.edacAhstrAry.stream().map(MpsCltuParser::fromHexStringToByteArray)
                .collect(Collectors.toList());
        for (int i = 0; i < byteCodeblocks.size(); i++) {
            final byte[]        data = byteCodeblocks.get(i);
            final byte[]        edac = byteEdacs.get(i);
            final IBchCodeblock cb   = buildCodeblock(data, edac);
            codeblocks.add(cb);
        }
        return codeblocks;
    }

    private IBchCodeblock buildCodeblock(final byte[] data, final byte[] edac) {
        final IBchCodeBlockBuilder cbBuilder = new BchCodeBlockBuilder();
        cbBuilder.setData(data);
        cbBuilder.setEdac(edac);
        return cbBuilder.build();
    }

    private static byte[] fromHexStringToByteArray(final String hexString) {
        // MPCS-12142: The alled method already has the valid hex check, don't check here
        return BinOctHexUtility.toBytesFromHex(hexString);
    }
}
