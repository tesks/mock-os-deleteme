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
package jpl.gds.ccsds.impl.cfdp;

import java.util.List;

import jpl.gds.ccsds.api.cfdp.CfdpSegmentationControlType;
import jpl.gds.ccsds.api.cfdp.CfdpTlv;
import jpl.gds.ccsds.api.cfdp.ICfdpMetadataPdu;
import jpl.gds.ccsds.api.cfdp.ICfdpPduHeader;
import jpl.gds.shared.gdr.GDR;

/**
 * CFDP Metadata PDUs are File Directive PDUs that contains information about the file being transmitted
 * 
 *
 */
class CfdpMetadataPdu extends CfdpFileDirectivePdu implements
        ICfdpMetadataPdu {

    // PDU object needs to be corruptible after it's instantiated, so save the valid header data
    private final CfdpSegmentationControlType segmentationControl;
    private final long fileSize;
    private final String sourceFileName;
    private final String destinationFileName;

    /**
     * Constructor that requires an ICfdpPduHeader object and the data
     * 
     * @param header
     *            a valid ICfdpPduHeader object
     * @param pduData
     *            the complete PDU data, including header
     */
    protected CfdpMetadataPdu(final ICfdpPduHeader header, final byte[] pduData) {
        super(header, pduData);
        if (pduData.length < header.getHeaderLength() + 7) {
            throw new IllegalArgumentException("Data field supplied for metadata directive PDU is not long enough");
        }

        // Cache the header data
        int val = GDR.get_u8(data, header.getHeaderLength() + 1) >>> 7;
        if (val == 0) {
            this.segmentationControl = CfdpSegmentationControlType.RECORDS_RESPECTED;
        } else {
            this.segmentationControl = CfdpSegmentationControlType.RECORDS_NOT_RESPECTED;
        }

        this.fileSize = GDR.get_u32(data, header.getHeaderLength() + 2);

        int startOffset = header.getHeaderLength() + 6;
        int len = GDR.get_u8(data,  startOffset);
        if (len == 0) {
            this.sourceFileName = null;
        } else {
            startOffset++;
            if (startOffset + len > data.length) {
                throw new IllegalStateException("Metadata PDU buffer is not large enough to fetch source file name");
            }
            this.sourceFileName = GDR.get_string(data, startOffset, len);
        }

        startOffset = header.getHeaderLength() + 7 + GDR.get_u8(data, (header.getHeaderLength() + 6));
        len = GDR.get_u8(data,  startOffset);
        if (len == 0) {
            this.destinationFileName = null;
        } else {
            if (startOffset + len > data.length) {
                throw new IllegalStateException("Metadata PDU buffer is not large enough to fetch destination file name");
            }
            this.destinationFileName = GDR.get_string(data, startOffset, len);
        }

    }

    @Override
    public CfdpSegmentationControlType getSegmentationControl() {
        return segmentationControl;
    }

    @Override
    public long getFileSize() {
        return fileSize;
    }

    @Override
    public String getSourceFileName() {
        return sourceFileName;
    }

    @Override
    public String getDestinationFileName() {
        return destinationFileName;
    }

    @Override
    public List<CfdpTlv> getOptions() {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public String toString() {
        return "CFDP METADATA PDU: " + super.toString() + ", segmentation control=" + getSegmentationControl() + 
                ", file size=" + getFileSize() + ", source file=" + getSourceFileName() +
                ", destination file=" + getDestinationFileName();
    }

}
