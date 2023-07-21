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
package ammos.datagen.pdu.app.client;


import javax.annotation.concurrent.NotThreadSafe;

import ammos.datagen.pdu.PduType;
import ammos.datagen.pdu.PduUtil;
import cfdp.engine.CommLink;
import cfdp.engine.Data;
import cfdp.engine.ID;
import cfdp.engine.PDUType;
import cfdp.engine.TransID;
import jpl.gds.shared.types.Triplet;

/**
 * Implementation of CFDP CommLink interface
 * Intended to be used by a single thread at a time
 * 
 *
 */
@NotThreadSafe
public class CfdpCommLink implements CommLink {
    // whether we have more data
    private boolean moreData    = true;

    // whether current data was read
    private boolean dataRead    = true;

    // whether data was dropped
    private boolean dataDropped = false;

    // holds current PDU for returning
    private Triplet<Data, PduType, TransID> currentPdu;

    @Override
    public boolean open(final ID partnerId) {
        return true;
    }

    /**
     * Do not call directly, called by the Engine when it has a PDU
     * {@inheritDoc}
     */
    @Override
    public boolean ready(final PDUType pduType, final TransID transID, final ID destID) {
        return moreData && dataRead;
    }

    /**
     * Do not call directly, called by the Engine when is ready()
     * {@inheritDoc}
     */
    @Override
    public void send(final TransID transID, final ID partnerID, final Data pdu) {
        final PduType type = PduUtil.getType(pdu);
        currentPdu = new Triplet<>(pdu, type, transID);

        // wait until data is accessed via getCurrentPdu()
        dataRead = false;
    }

    /**
     * Get current PDU
     * 
     * @param dropMeta Drop metadata
     * @param dropData Drop first packet of data
     * @param dropEof Drop EOF
     * 
     * @return PDU as Triplet<Data, PduType, TransID>, or null if we skip the PDU
     */
    public Triplet<Data, PduType, TransID> getCurrentPdu(final boolean dropMeta, final boolean dropData,
                                                         final boolean dropEof) {
        
        final PduType type = currentPdu.getTwo();
        
        // check for EOF PDU
        if (type == PduType.EOF) {
            moreData = false;
        }
        
        // data was accessed
        dataRead = true;

        // error injection
        if (type == PduType.METADATA && dropMeta) {
            return null;
        }
        if (type == PduType.EOF && dropEof) {
            return null;
        }
        if (type == PduType.FILEDATA && dropData && !dataDropped) {
            dataDropped = true;
            return null;
        }

        return currentPdu;
    }

    /**
     * Checks if there if the Engine has more data
     * 
     * @return True if it has more data, false if not
     */
    public boolean hasMoreData() {
        return moreData;
    }
}
