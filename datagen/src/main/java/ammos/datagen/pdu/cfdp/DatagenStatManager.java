/*
 * Copyright 2006-2022. California Institute of Technology.
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

package ammos.datagen.pdu.cfdp;

import cfdp.engine.ID;
import cfdp.engine.TransID;
import cfdp.engine.ampcs.IStatManager;

/**
 * StatManager for datagen, that does not depend on other beans
 * only defined for the CFDP processor app
 */
public class DatagenStatManager implements IStatManager {
    @Override
    public void setTimerState(final TransID transID, final boolean b) {

    }

    @Override
    public void setAckPduQueued(final TransID transID, final boolean b) {

    }

    @Override
    public void sentAckPdu(final TransID transID, final int i) {

    }

    @Override
    public void newUplinkTransaction(final TransID transID, final ID id, final boolean b, final long l) {

    }

    @Override
    public void sentFileDataPdu(final TransID transID, final int i, final int i1) {

    }

    @Override
    public void setEofPduQueued(final TransID transID, final boolean b) {

    }

    @Override
    public void sentEofPdu(final TransID transID, final int i) {

    }

    @Override
    public void setMetadataPduQueued(final TransID transID, final boolean b) {

    }

    @Override
    public void sentMetadataPdu(final TransID transID, final int i) {

    }

    @Override
    public void deleteUplinkTransaction(final TransID transID) {

    }

    @Override
    public void receivedPdu(final long l) {

    }

    @Override
    public void newDownlinkTransaction(final TransID transID, final ID id, final boolean b) {

    }

    @Override
    public void setNakPduQueued(final TransID transID, final boolean b) {

    }

    @Override
    public void sentNakPdu(final TransID transID, final int i) {

    }

    @Override
    public void setFinishedPduQueued(final TransID transID, final boolean b) {

    }

    @Override
    public void sentFinishedPdu(final TransID transID, final int i) {

    }

    @Override
    public void determinedFileSize(final TransID transID, final long l) {

    }

    @Override
    public void downlinkedFileData(final TransID transID, final int i) {

    }

    @Override
    public void deleteDownlinkTransaction(final TransID transID) {

    }

    @Override
    public void filestoreAccessOk(final boolean b) {

    }

    @Override
    public void save(final String s) {

    }

    @Override
    public void restore(final String s) {

    }

    @Override
    public void setStateSaveTime(final long l) {

    }
}
