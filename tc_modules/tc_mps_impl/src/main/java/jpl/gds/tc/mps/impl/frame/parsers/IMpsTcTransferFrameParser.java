package jpl.gds.tc.mps.impl.frame.parsers;

import gov.nasa.jpl.tcsession.TcSession;
import jpl.gds.tc.api.ITcTransferFrame;
import jpl.gds.tc.api.frame.ITcTransferFrameParser;

public interface IMpsTcTransferFrameParser extends ITcTransferFrameParser {
    /**
     * Build an MPS TC frame from a CTS frame item
     *
     * @param frameItem TcSession frame item
     * @param validateFecf true to validate FECF, false to skip validation
     * @return MPS TC frame
     */
    ITcTransferFrame parse(TcSession.frmitem frameItem, boolean validateFecf);

    /**
     * Build an MPS TC frame from a CTS frame item, FECF will be validated
     *
     * @param frameItem TcSession frame item
     * @return MPS TC frame
     */
    ITcTransferFrame parse(TcSession.frmitem frameItem);
}
