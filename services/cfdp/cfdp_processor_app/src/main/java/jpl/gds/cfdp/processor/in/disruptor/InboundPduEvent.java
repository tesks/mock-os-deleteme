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

package jpl.gds.cfdp.processor.in.disruptor;

import jpl.gds.cfdp.common.CfdpPduConstants;
import jpl.gds.shared.log.Tracer;

public class InboundPduEvent {

    static Tracer log;

    private byte[] pduBuffer = new byte[CfdpPduConstants.THEORETICAL_MAX_PDU_SIZE_IN_BYTES];
    private int pduLength;
    private String ampcsOutputDirectory;
    private String sclk;
    private String scet;
    private String lst;
    private String ert;
    private int sourcePacketSeqCount;
    // MPCS-9870 Add additional EMD metadata
    private long sessionId;
    private String sessionName;
    private String fswDictionaryDir;
    private String fswVersion;
    private String venueType;
    private String testbedName;
    private String user;
    private String host;
    private int scid;
    private int apid;
    private String productType;
    private int vcid;
    private int sequenceId;
    private int sequenceVersion;
    private int commandNumber;
    private int relayScid;

    public static void setLog(final Tracer log) {
        InboundPduEvent.log = log;
    }

    public static void translate(final InboundPduEvent event, final long sequence, Object... objects) {
        log.trace("translate: " + (byte[]) objects[0] + " " + (int) objects[1] + " " + (String) objects[2]
                + " " + (String) objects[3] + " " + (String) objects[4] + " " + (String) objects[5] + " "
                + (String) objects[6] + " " + (int) objects[7]);
        event.setPdu(
                // pduBuffer
                (byte[]) objects[0],
                // pduLength
                (int) objects[1],
                // ampcsOutputDirectory
                (String) objects[2],
                //sclk
                (String) objects[3],
                // scet
                (String) objects[4],
                // lst
                (String) objects[5],
                // ert
                (String) objects[6],
                // sourcePacketSeqCount
                (int) objects[7],

                // MPCS-9870 Add additional EMD metadata

                // sessionId
                (long) objects[8],
                // sessionName
                (String) objects[9],
                // fswDictionaryDir
                (String) objects[10],
                // fswVersion
                (String) objects[11],
                // venueType
                (String) objects[12],
                // testbedName
                (String) objects[13],
                // user
                (String) objects[14],
                // host
                (String) objects[15],
                // scid
                (int) objects[16],
                // apid
                (int) objects[17],
                // productType
                (String) objects[18],
                // vcid
                (int) objects[19],
                // sequenceId
                (int) objects[20],
                // sequenceVersion
                (int) objects[21],
                // commandNumber
                (int) objects[22],

                // MPCS-9817 Add relay SCID
                // relayScid
                (int) objects[23]
        );

    }

    /**
     * @param pduBuffer the pduBuffer to set
     */
    public void setPdu(byte[] pduBuffer, int pduLength, final String ampcsOutputDirectory, final String sclk, final String scet,
                       final String lst, final String ert, final int sourcePacketSeqCount,
                       // MPCS-9870 Add additional EMD metadata
                       final long sessionId,
                       final String sessionName,
                       final String fswDictionaryDir,
                       final String fswVersion,
                       final String venueType,
                       final String testbedName,
                       final String user,
                       final String host,
                       final int scid,
                       final int apid,
                       final String productType,
                       final int vcid,
                       final int sequenceId,
                       final int sequenceVersion,
                       final int commandNumber,
                       final int relayScid
    ) {
        System.arraycopy(pduBuffer, 0, this.pduBuffer, 0, pduLength);
        this.pduLength = pduLength;
        this.ampcsOutputDirectory = ampcsOutputDirectory;
        this.sclk = sclk;
        this.scet = scet;
        this.lst = lst;
        this.ert = ert;
        this.sourcePacketSeqCount = sourcePacketSeqCount;
        this.sessionId = sessionId;
        this.sessionName = sessionName;
        this.fswDictionaryDir = fswDictionaryDir;
        this.fswVersion = fswVersion;
        this.venueType = venueType;
        this.testbedName = testbedName;
        this.user = user;
        this.host = host;
        this.scid = scid;
        this.apid = apid;
        this.productType = productType;
        this.vcid = vcid;
        this.sequenceId = sequenceId;
        this.sequenceVersion = sequenceVersion;
        this.commandNumber = commandNumber;
        this.relayScid = relayScid;
    }

    /**
     * @return the pduBuffer
     */
    public byte[] getPduBuffer() {
        return pduBuffer;
    }

    /**
     * @return the pduLength
     */
    public int getPduLength() {
        return pduLength;
    }

    /**
     * @return the ampcsOutputDirectory
     */
    public String getAmpcsOutputDirectory() {
        return ampcsOutputDirectory;
    }

    public String getSclk() {
        return sclk;
    }

    public String getScet() {
        return scet;
    }

    public String getLst() {
        return lst;
    }

    public String getErt() {
        return ert;
    }

    public int getSourcePacketSeqCount() {
        return sourcePacketSeqCount;
    }

    public long getSessionId() {
        return sessionId;
    }

    public String getSessionName() {
        return sessionName;
    }

    public String getFswDictionaryDir() {
        return fswDictionaryDir;
    }

    public String getFswVersion() {
        return fswVersion;
    }

    public String getVenueType() {
        return venueType;
    }

    public String getTestbedName() {
        return testbedName;
    }

    public String getUser() {
        return user;
    }

    public String getHost() {
        return host;
    }

    public int getScid() {
        return scid;
    }

    public int getApid() {
        return apid;
    }

    public String getProductType() {
        return productType;
    }

    public int getVcid() {
        return vcid;
    }

    public int getSequenceId() {
        return sequenceId;
    }

    public int getSequenceVersion() {
        return sequenceVersion;
    }

    public int getCommandNumber() {
        return commandNumber;
    }

    public int getRelayScid() {
        return relayScid;
    }

}