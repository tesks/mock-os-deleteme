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

import ammos.datagen.config.GeneralMissionConfiguration;
import ammos.datagen.pdu.PduType;
import ammos.datagen.pdu.config.PduRunConfiguration;
import cfdp.client.DefaultFilestore;
import cfdp.client.MyFilestoreRequestHandler;
import cfdp.client.NodeInfo;
import cfdp.engine.ChecksumAlgorithm;
import cfdp.engine.Client;
import cfdp.engine.Clock;
import cfdp.engine.CommLink;
import cfdp.engine.Data;
import cfdp.engine.Engine;
import cfdp.engine.Filestore;
import cfdp.engine.FilestoreRequestHandler;
import cfdp.engine.ID;
import cfdp.engine.MIB;
import cfdp.engine.Manager;
import cfdp.engine.PutInfo;
import cfdp.engine.Request;
import cfdp.engine.RequestType;
import cfdp.engine.TestSupport;
import cfdp.engine.TransID;
import cfdp.engine.User;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.types.Triplet;
import jpl.gds.shared.types.UnsignedLong;

/**
 * Client for the CFDP library
 * 
 *
 */
public class CfdpClient implements Client {
    private final DefaultFilestore            filestore        = new DefaultFilestore();
    private final GeneralMissionConfiguration missionConfig;
    private final PduRunConfiguration         runConfig;
    private Tracer                            statusLogger;

    private ID                      sourceId;
    private ID                      destId;
    private Manager                 manager;
    private User                    user;
    private FilestoreRequestHandler fsrHandler;
    private CfdpCommLink            pduOutput;
    private CfdpMIB                 mib;
    private String                  restoreDir;
    private UnsignedLong            transactionNumber;

    // error injection
    private boolean                           dropMeta;
    private boolean                           dropData;
    private boolean                           dropEof;

    /**
     * Constructor
     * 
     * @param missionConfig Mission config
     * @param runConfig Run configuration
  
     */
    public CfdpClient(final GeneralMissionConfiguration missionConfig,
            final PduRunConfiguration runConfig, final UnsignedLong transactionNumber) {
        this.missionConfig = missionConfig;
        this.runConfig = runConfig;
        this.transactionNumber = transactionNumber;

        init();
    }
    
    private void init() {
        statusLogger = TraceManager.getTracer(Loggers.DATAGEN);
        // create nodes
        final NodeInfo myNode = new NodeInfo();
        // set properties from config files
        sourceId = ID.create(runConfig.getIntProperty(PduRunConfiguration.SOURCE_ENTITY_ID, 100));
        destId = ID.create(runConfig.getIntProperty(PduRunConfiguration.DEST_ENTITY_ID, 200));
        myNode.id = sourceId;

        // Create objects that implement the Interfaces that are required by the Engine
        mib = new CfdpMIB(myNode.id);
        mib.setGenerateCrc(runConfig.getBooleanProperty(PduRunConfiguration.GENERATE_CRC, false));
        // set properties from config files
        mib.setOutgoingFileChunkSize(runConfig.getIntProperty(PduRunConfiguration.PREF_PDU_LENGTH, 1000));
        mib.setTransSeqNumLength(runConfig.getIntProperty(PduRunConfiguration.TRANS_SEQ_LENGTH, 8));
        mib.setMaxFileChunkLength(missionConfig.getIntProperty(GeneralMissionConfiguration.PACKET_MAX_LEN, 65535));
        fsrHandler = new MyFilestoreRequestHandler(filestore);
        user = new CfdpUser();
        pduOutput = new CfdpCommLink();

        dropMeta = runConfig.getBooleanProperty(PduRunConfiguration.DROP_META, false);
        dropData = runConfig.getBooleanProperty(PduRunConfiguration.DROP_DATA, false);
        dropEof = runConfig.getBooleanProperty(PduRunConfiguration.DROP_EOF, false);

        // create manager
        manager = Engine.createManager(myNode.id, this);

        //set optional transaction number
        TestSupport support = new TestSupport(manager);
        if(transactionNumber != null) {
            support.setTransSeqNum(transactionNumber.intValue());
        }
    }

    @Override
    public CommLink getCommLink() {
        return pduOutput;
    }

    @Override
    public Filestore getFilestore() {
        return filestore;
    }

    @Override
    public MIB getMIB() {
        return mib;
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public String getRestoreDir() {
        return restoreDir;
    }

    @Override
    public boolean restore(final MIB mib) {
        if (!(mib instanceof CfdpMIB)) {
            statusLogger.error("Provided an incompatible MIB");
            return false;
        }
        this.mib = (CfdpMIB) mib;
        return true;
    }

    @Override
    public FilestoreRequestHandler getRequestHandler() {
        return fsrHandler;
    }

    @Override
    public Clock getClock() {
        return null;
    }

    /**
     * Get current PDU
     * 
     * @return PDU as Triplet<Data, PduType, TransID> object, or null if we skip the PDU
     */
    public Triplet<Data, PduType, TransID> getCurrentPdu() {
        //get current PDU from Engine
        final Triplet<Data, PduType, TransID> pdu = pduOutput.getCurrentPdu(dropMeta, dropData, dropEof);
        // tell Engine to produce the next PDU
        manager.cycle();
        return pdu;
    }

    /**
     * Checks if there if the Engine has more data
     * 
     * @return True if it has more data, false if not
     */
    public boolean hasMoreData() {
        return pduOutput.hasMoreData();
    }

    /**
     * Execute PUT request
     * 
     * @param inputFile Input file
     * @param outputFile Output file
     */
    public void executePutRequest(final String inputFile, final String outputFile) {
        // build the request and set parameters from config
        final Request request = new Request();
        request.type = RequestType.REQ_PUT;
        request.transID = TransID.create(sourceId, transactionNumber.longValue());
        // TODO the CFDP Engine does not yet support segmentation control
        final PutInfo put = request.put;
        put.source_file_name = inputFile;
        put.dest_file_name = outputFile;
        put.dest_id = destId;
        put.ack_required = runConfig.getBooleanProperty(PduRunConfiguration.TRANSMISSION_MODE, false);
        put.file_transfer = true;

        // initiate transfer
        manager.giveRequest(request);

        // produce first PDU
        manager.cycle();
    }

    @Override
    public ChecksumAlgorithm getEofPduChecksumAlgorithm() {
        return ChecksumAlgorithm.CFDP;
    }

    @Override
    public boolean getEofPduChecksumValidationEnabled() {
        return false;
    }
}
