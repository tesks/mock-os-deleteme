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
package jpl.gds.tc.mps.impl.frame.parsers;

import gov.nasa.jpl.tcsession.TcSession;
import gov.nasa.jpl.uplinkutils.UplinkUtils;

import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.ITcTransferFrame;
import jpl.gds.tc.api.frame.ITcTransferFrameBuilder;
import jpl.gds.tc.impl.frame.TcTransferFrameBuilder;

public class MpsTcTransferFrameBuilder extends TcTransferFrameBuilder {

    public static ITcTransferFrame build(TcSession.frmitem frameItem) {
        final ITcTransferFrameBuilder builder = new TcTransferFrameBuilder();
        builder.setVersion(frameItem.frmver)
                .setBypassFlag(frameItem.frmbypass)
                .setCtrlCmdFlag(frameItem.frmcntlcmd)
                .setSpacecraftId(frameItem.frmscid)
                .setFrameLength(frameItem.frmlen)
                .setSequenceNumber(frameItem.frmseqnum)
                .setSpare((byte) 0x0)
                .setVcid((byte) frameItem.frmvc)
                .setData(
                        BinOctHexUtility.toBytesFromHex(
                                UplinkUtils.bintoasciihex(frameItem.data, frameItem.datalen << 3, 0)
                        )
                );

        if (frameItem.frmfecdata != null && frameItem.frmfeclen > 0) {
            builder.setFecf(
                    BinOctHexUtility.toBytesFromHex(
                            UplinkUtils.bintoasciihex(frameItem.frmfecdata, frameItem.frmfeclen << 3, 0)
                    )
            );
        }

        return builder.build();
    }
}
