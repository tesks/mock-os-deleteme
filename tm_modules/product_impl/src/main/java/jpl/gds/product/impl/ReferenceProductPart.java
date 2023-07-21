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
package jpl.gds.product.impl;

import java.io.File;
import java.text.DateFormat;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.apid.IApidDefinitionProvider;
import jpl.gds.product.api.IProductMetadataUpdater;
import jpl.gds.product.api.IReferenceProductMetadataProvider;
import jpl.gds.product.api.IReferenceProductMetadataUpdater;
import jpl.gds.product.api.ProductException;
import jpl.gds.product.api.config.IProductPropertiesProvider;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.template.Templatable;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.DataValidityTime;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.shared.types.ByteArraySlice;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.shared.util.ByteOffset;
import jpl.gds.tm.service.api.packet.ITelemetryPacketMessage;

/**
 * This is the generic product part class used by the product builder. Parts are
 * built from packets. Packets used to build generic parts must be formatted
 * according to the MSAP Flight-Ground ICD.
 * 
 */
public class ReferenceProductPart extends AbstractProductPart implements Templatable {
    // flag for determining which secondary header format to use
    // private boolean isStreamingProduct;

    // A reference to the apid reference to for determining if the product is
    // autonomous
    private IApidDefinitionProvider apidReference;

    /*
     * Remove reliance on Packet class below. GROUP
     * constants now in the superclass.
     */

    private final boolean useCommandedProductHeader;

    private final boolean useUpper63TSNBits;

    // 10/17/16 - Added filename DVT marker and separator
    // properties, for more dynamic filename parsing
    private final String filenameDvtMarker;

    private final String filenameDvtSeparator;

    /**
     * Creates an empty instance of ReferenceProductPart.
     * 
     * @param appContext the context
     * @param md  product metadata
     * @param filenameDvtMarker the dvt marker
     * @param filenameDvtSeparator the dvt separator
     * @throws ProductException
     *             if the APID dictionary cannot be loaded
     */
    public ReferenceProductPart(final ApplicationContext appContext, final IProductMetadataUpdater md, 
    		final String filenameDvtMarker, final String filenameDvtSeparator) throws ProductException {
        super(appContext, md);
        this.filenameDvtMarker = filenameDvtMarker;
        this.filenameDvtSeparator = filenameDvtSeparator;
        
        this.useCommandedProductHeader = appContext.getBean(IProductPropertiesProvider.class).isCommandedHeaderUsed();
        this.useUpper63TSNBits = appContext.getBean(IProductPropertiesProvider.class).isUpperTSNBitsUsed();
    }

    /**
     * Creates an instance of ReferenceProductPart for a packet message. It is
     * assumed that the Packet class has already parsed the primary packet
     * header and the DVT times from the secondary header. It requires access to
     * the apid reference in order to determine if the product is autonomous or
     * not, as is required for determining how to parse the secondary header.
     * @param appContext the current application context
     * @param md product metadata
     * @param packet
     *            the Packet message containing the PDU
     * @param filenameDvtMarker the dvt marker
     * @param filenameDvtSeparator the dvt separator
     * @throws ProductException
     *             if the APID dictionary cannot be loaded or part data could
     *             not be created from the packet
     */
    public ReferenceProductPart(final ApplicationContext appContext, final IProductMetadataUpdater md, 
            final ITelemetryPacketMessage packet, final String filenameDvtMarker, final String filenameDvtSeparator) throws ProductException {
        super(appContext, packet, md);

        this.filenameDvtMarker = filenameDvtMarker;
        this.filenameDvtSeparator = filenameDvtSeparator;
        
        this.useCommandedProductHeader = appContext.getBean(IProductPropertiesProvider.class).isCommandedHeaderUsed();
        this.useUpper63TSNBits = appContext.getBean(IProductPropertiesProvider.class).isUpperTSNBitsUsed();

        /*
         * Get VCID from packet info rather than frame info.
         */
        setVcid(packet.getPacketInfo().getVcid());

        try {

            // offset starts after DVT times in secondary packet header
            final ByteArraySlice pktData = getData();
            log.debug("Extracting new product part: pkt data len (i.e. sans pri & sec pkt hdrs) = ", pktData.length);
            load(pktData);
        }
        catch (final Exception loadException) {
            final StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("Failed to load data product for apid " + Integer.valueOf(getApid()).toString() + "--"
                    + loadException.getMessage());
            throw (new ProductException(messageBuilder.toString(), loadException));
        }

        /*
         * Get SCID from packet info rather than frame info below.
         */
        this.metadata.setScid(packet.getPacketInfo().getScid());
    }

    /**
     * Loads the part metadata from the remaining secondary header and any
     * PDUs that following in the given Packet.
     * @param pktData a ByteArraySlice containing the packet data, with the
     * current offset properly positioned for the remaining parsing 
     * @return the number of bytes consumed from the packet
     * @throws Exception if there is any error loading data from the packet buffer
     */
    protected int load(final ByteArraySlice pktData) throws Exception {
        
        final byte[] buf = pktData.array;
        final int offset = pktData.offset;
        final ByteOffset p = new ByteOffset(offset);
        
        final IReferenceProductMetadataUpdater localMd = (IReferenceProductMetadataUpdater)metadata;
        
        // Parse remainder of secondary packet header for product packet
        metadata.setTotalParts(GDR.get_u16(buf, p.inc(2)) - 1);  // We won't count the metadata part
        setPartNumber(GDR.get_u16(buf, p.inc(2)) - 1); // The metadata part will be part 0 

		/* 
		 * Report total parts + 1 rather than total parts
		 * so we know for sure what flight sent us.
		 */
        log.debug("--> Received product part APID = ", getApid(), ", Total Flight Parts = ",
                  String.valueOf(metadata.getTotalParts() + 1), ", Part Number = ", partNumber);
 
        //If the product is NOT autonomously built then it has the 16 byte header with 
        //sequence id, sequence version, and command number fields, if the project 
        // configuration says commanded product headers are present
        if (useCommandedProductHeader && !this.isStreaming()){
	        final int sequenceId = GDR.get_u16(buf, p.inc(2));
	        final int sequenceVer = GDR.get_u16(buf, p.inc(2));
	        final int commandNum = GDR.get_u16(buf, p.inc(2));
	        metadata.setSequenceId(sequenceId);
	        metadata.setSequenceVersion(sequenceVer);
	        metadata.setCommandNumber(commandNum);
            log.debug("    Non-autonomous part. CommandId = ", metadata.getCommandId(), ", seqId = ", sequenceId,
                      ", seqVer = ", sequenceVer, ", cmdNum = ", commandNum);
        } else {
            log.debug("    Autonomous part.");
        }
        
        // Start Parsing Common PDU header
        checkOffset(p.getOffset(), 5, buf, "product PDU header's first 5 bytes");
        final int pduType = (GDR.get_u8(buf,p.inc(1)) & 0x10) >> 4; // get first PDU type
        final int pduDataLength = GDR.get_u16(buf, p.inc(2));
        log.debug("    Part PDU TYPE = ", pduType, ", Part PDU Data Length = ", pduDataLength);
        
        //length of transaction seq number is variable. get the length
        int transactionSeqLenBytes = (GDR.get_u8(buf, p.inc(1)) & 0x7) + 1;
        localMd.setSourceEntityId(GDR.get_u8(buf, p.inc(1)));
        long transId = 0;
        switch (transactionSeqLenBytes) {
        case 1:
        	checkOffset(p.getOffset(), 1, buf, "product PDU header's transaction sequence number (1 byte)");
        	transId = GDR.get_u8(buf, p.inc(1));
        	break;
        case 2:
        	checkOffset(p.getOffset(), 2, buf, "product PDU header's transaction sequence number (2 bytes)");
        	transId = GDR.get_u16(buf, p.inc(2));
        	break;
        case 4:
        	checkOffset(p.getOffset(), 4, buf, "product PDU header's transaction sequence number (4 bytes)");
        	transId = GDR.get_u32(buf, p.inc(4));
        	break;
        case 8:
        	checkOffset(p.getOffset(), 8, buf, "product PDU header's transaction sequence number (8 bytes)");
        	
			/*
			 * SMAP uses the upper 63 bits, so need to shift right.
			 * For now, base it on a config flag, rather than creating a
			 * separate SMAP adaptation.
			 */
			if (useUpper63TSNBits) {
				final byte[] tsnCopy = new byte[8];
				System.arraycopy(buf, p.inc(8), tsnCopy, 0, 8);
				BinOctHexUtility.shiftRight(tsnCopy);
				transId = GDR.get_u64(tsnCopy, 0);
			} else {
				transId = GDR.get_u64(buf, p.inc(8));
			}

        	break;
        }
        localMd.setCfdpTransactionId(transId);
            
        log.debug("    Transaction Seq Length = ", transactionSeqLenBytes, ", Part transaction ID = ",
                  Long.toUnsignedString(localMd.getCfdpTransactionId()));
        checkOffset(p.getOffset(), 1, buf, "product PDU header's destination entity ID");
        p.inc(1); // Skip entity ID
        // End of common PDU header
    
        String filename = adaptor.getFromFilenameMap(localMd.getCfdpTransactionId());
        
        // moved parsing of DVT from filename to new function
        processFilename(filename);

        // Have metadata PDU
        if (pduType == 0 && getPartNumber() == 0) {
           log.debug("    THIS IS A METADATA PDU");
           setPartPduType(ReferencePduType.METADATA);
           
           checkOffset(p.getOffset(), 6, buf, "file size");
           p.inc(2); // skip file directive and segmentation control flags
           metadata.setFileSize(GDR.get_u32(buf, p.inc(4)));
            log.debug("    File size = ", metadata.getFileSize());

           checkOffset(p.getOffset(), 1, buf, "source filename length");
           final int srcSize = GDR.get_u8(buf, p.inc(1));
            log.debug("    Source filename length = ", srcSize);

           checkOffset(p.getOffset(), srcSize, buf, "source filename (" + srcSize + " bytes)");
           p.inc(srcSize);
           
           checkOffset(p.getOffset(), 1, buf, "destination filename length");
           final int destSize = GDR.get_u8(buf, p.inc(1));
            log.debug("    Destination filename length = ", destSize);

           checkOffset(p.getOffset(), destSize, buf, "destination filename");
           final String destFile = GDR.get_string(buf, p.getOffset(), destSize);
           
           final File temp = new File(destFile);
           filename = temp.getName();
           final int dotIndex = filename.indexOf(".");
           if (dotIndex != -1) {
               filename = filename.substring(0, dotIndex);
           }
            log.debug("    Destination Filename from Metadata PDU: ", filename);
           
           
           // moved parsing of DVT from filename to new function
           final boolean goodFilename = processFilename(filename);
           
           //if the filename was bad, don't put it in the adaptor so it can be fail parsing again
           if(goodFilename){
        	   adaptor.addToFilenameMap(localMd.getCfdpTransactionId(), filename);
           }
        } else if (pduType == 1) { // Have data PDU
            log.debug("    DATA PDU");

            // Parse the data PDU header
            setPartPduType(ReferencePduType.DATA);

            checkOffset(p.getOffset(), 4, buf, "offset field");
            final long pduOffset = GDR.get_u32(buf, p.inc(4));
            checkValidPduOffset(pduOffset);
            setPartOffset(pduOffset);
            setPartLength(Math.max(pduDataLength - 4, 0));
            log.debug("    Offset = ", getPartOffset(), ", Part Length = ", getPartLength(), " (PDU data length = ",
                      pduDataLength, ")");
            
            // Set the part data
            checkOffset(p.getOffset(), getPartLength(), buf, "product part data");
            final ByteArraySlice partData = new ByteArraySlice(
                    pktData.array, p.getOffset(), getPartLength());
            setData(partData); 
            p.inc(getPartLength());
                     
            // check to see if we also have end PDU in this packet
            if (p.getOffset() < buf.length - 1) {
                log.debug("    Part Data PDU contains EOF PDU");
                // Parse the end PDU
                setPartPduType(ReferencePduType.DATA_END);
                checkOffset(p.getOffset(), 5, buf, "EOF PDU header's first 5 bytes");
                p.inc(3); // first, skip common PDU header's first 3 bytes
                
				// length of transaction seq number is variable. get the length,
				// so we can skip proper number of bytes.
                transactionSeqLenBytes = (GDR.get_u8(buf, p.inc(1)) & 0x7) + 1;
				p.inc(1); // skip source entity ID (at this point, we skipped first 5 bytes of common PDU header)
                switch (transactionSeqLenBytes) {
                case 1:
                    checkOffset(p.getOffset(), 1, buf, "EOF PDU header's transaction sequence number (1 byte)");
                	p.inc(1);
                	break;
                case 2:
                	checkOffset(p.getOffset(), 2, buf, "EOF PDU header's transaction sequence number (2 bytes)");
                	p.inc(2);
                	break;
                case 4:
                	checkOffset(p.getOffset(), 4, buf, "EOF PDU header's transaction sequence number (4 bytes)");
                	p.inc(4);
                	break;
                case 8:
                	checkOffset(p.getOffset(), 8, buf, "EOF PDU header's transaction sequence number (8 bytes)");
                	p.inc(8);
                	break;       
                }

            	checkOffset(p.getOffset(), 1, buf, "EOF PDU header's destination entity ID");
                p.inc(1); // Skip destination entity ID
                // Finished skipping piggybacked EPDU's common PDU header

                checkOffset(p.getOffset(), 2, buf, "file directive code, condition code, and spare bits (2 bytes total)");
                p.inc(2); // skip File Directive Code, Condition Code, and Spare Bits
                checkOffset(p.getOffset(), 4, buf, "checskum (from EOF)");
                metadata.setChecksum(GDR.get_u32(buf, p.inc(4)));
                log.debug("    EOF's checksum = ", metadata.getChecksum());
                checkOffset(p.getOffset(), 4, buf, "file size (from EOF)");
                metadata.setFileSize(GDR.get_u32(buf, p.inc(4)));
                log.debug("    EOF's file size = ", metadata.getFileSize());
            } else {
            	log.debug("    Part Data PDU does not have EOF PDU");
            }
        } else { // end of data PDU alone
			log.debug("    Standalone EOF PDU");
            setPartPduType(ReferencePduType.END);
            checkOffset(p.getOffset(), 2, buf, "file directive code, condition code, and spare bits (2 bytes total)");
            p.inc(2); // skip File Directive Code, Condition Code, and Spare Bits 
            checkOffset(p.getOffset(), 4, buf, "checskum (from EOF)");
            metadata.setChecksum(GDR.get_u32(buf, p.inc(4)));
            log.debug("    EOF's checksum = ", metadata.getChecksum());
            checkOffset(p.getOffset(), 4, buf, "file size (from EOF)");
            metadata.setFileSize(GDR.get_u32(buf, p.inc(4)));
            log.debug("    EOF's file size = ", metadata.getFileSize());
        }
        return p.getOffset() - offset;
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate() throws ProductException {
    }

    /**
     * Overrides the default APID setting method and also sets
     * isAutononomoulsyBuilt based on the APID specified if the apidReference is
     * not null.
     * 
     * @param apid
     *            the APID to set
     */
    @Override
    public void setApid(final int apid) {
        super.setApid(apid);
    }

    /**
     * Gets the Is Streaming flag boolean value.
     * 
     * @return true if streaming, false otherwise
     * @throws ProductException
     *             if there is an error while retrieving the streaming flag for
     *             the product part
     */
    public boolean isStreaming() throws ProductException {
        if (this.apidReference == null) {
            try {
                this.apidReference = appContext.getBean(IApidDefinitionProvider.class);
            }
            catch (final Exception e) {
                e.printStackTrace();
                throw new ProductException("Unable to get streaming flag for product part: " + e.toString());
            }
        }
        /*
         * Changed code below to go through the APID
         * definition and to use isCommandedProduct rather than isStreaming.
         * Note the boolean sense of the two calls is opposite, thus the !.
         */
        return !this.apidReference.getApidDefinition(getApid()).isCommandedProduct();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFilename() {
        return this.metadata.getFilename();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDirectoryName() {
        return this.metadata.getDirectoryName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTransactionId() {
        return new StringBuilder().append(getDirectoryName()).append("/")
                .append(Long.toUnsignedString(((IReferenceProductMetadataProvider)metadata).getCfdpTransactionId())).toString();
    }

    /**
     * Parse the DVT coarse and fine values from the filename and set the in the
     * metadata. If the filename is null or cannot be parsed properly, then a
     * placeholder name is used in the metadata
     * 
     * @param filename
     *            the filename of the product being processed
     * @return true if the filename was of the valid format and a valid DVT was
     *         extracted, false otherwise
     */
    public boolean processFilename(final String filename) {

        boolean goodFilename = false;

        if (filename != null) {
            metadata.setFilename(filename);
            log.debug("    Setting Metadata Product Filename: ", filename);
            // Updated parsing out dvtPortion and
            // splitting DVT for more dynamic capabilities

            final int dvtStart = filename.indexOf(filenameDvtMarker);
            final String dvtPortion = filename.substring(dvtStart + 1);
            final String[] portions = dvtPortion.split(filenameDvtSeparator);
            if (dvtStart == -1 || portions.length < 2) {
                log.error("    Product filename " + filename
                        + " does not match the expected format. Cannot parse DVT. Verify product configuration.");
                goodFilename = false;
            }
            else {
                goodFilename = true;
            }
            if (goodFilename) {
                try {
                    metadata.setDvtCoarse(Long.parseLong(portions[0]));
                    metadata.setDvtFine(Long.parseLong(portions[1]));

                    final DataValidityTime metaDvt = new DataValidityTime(metadata.getDvtCoarse(), metadata.getDvtFine());

                    log.debug("    Part DVT Coarse = ", metaDvt.getCoarse(), ", Fine = ", metaDvt.getFine());
                }
                catch (final NumberFormatException e) {
                    log.error("    A full DVT could not be parsed from the filename " + filename
                            + ". Verify product configuration.");
                    goodFilename = false;
                }
                catch (final IllegalArgumentException e) {
                    log.error("    A valid DVT could not be parsed from the filename " + filename
                            + ". Verify time configuration.");
                    goodFilename = false;
                }
            }
        }
        final IReferenceProductMetadataProvider localMd = (IReferenceProductMetadataProvider) metadata;
        
        if (filename == null || !goodFilename) {
            final DateFormat df = TimeUtility.getDoyFormatterFromPool();
            final String fname = (localMd.getSourceEntityId() + "-"
                    + Long.toUnsignedString(localMd.getCfdpTransactionId()) + "-"
                    + df.format(new AccurateDateTime()));
            TimeUtility.releaseDoyFormatterToPool(df);
            metadata.setFilename(fname);
            final StringBuilder logMsg = new StringBuilder("    Setting Metadata Product Filename: " + fname + " (");
            if (filename == null) {
                logMsg.append("filename from map was null");
            }
            else {
                logMsg.append("filename format was invalid");
            }
            logMsg.append("); can't parse DVT");

            log.debug(logMsg);
        }

        return goodFilename;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTemplateContext(final Map<String, Object> map) {
        super.setTemplateContext(map);
        map.put("class", ReferenceProductPart.class.getName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.xml.stax.StaxSerializable#generateStaxXml(javax.xml.stream.XMLStreamWriter)
     */
    @Override
    public void generateStaxXml(final XMLStreamWriter writer) throws XMLStreamException {

        writer.writeStartElement("ProductPart"); // <ProductPart>
        writer.writeAttribute("class", getClass().getName());

        writer.writeStartElement("Vcid"); // <Vcid>
        if (metadata.getVcid() != null) {
            writer.writeCharacters(Long.toString(metadata.getVcid()));
        }
        else {
            writer.writeCharacters("0");
        }
        writer.writeEndElement(); // </Vcid>

        writer.writeStartElement("Apid"); // <Apid>
        writer.writeCharacters(Long.toString(metadata.getApid()));
        writer.writeEndElement(); // </Apid>

        writer.writeStartElement("ProductName"); // <ProductName>
        writer.writeCharacters(metadata.getProductType());
        writer.writeEndElement(); // </ProductName>

        writer.writeStartElement("Scid"); // <Scid>
        writer.writeCharacters(Long.toString(metadata.getScid()));
        writer.writeEndElement(); // </Scid>

        writer.writeStartElement("Sclk"); // <Sclk>
        writer.writeCharacters(metadata.getSclkStr());
        writer.writeEndElement(); // </Sclk>

        writer.writeStartElement("Ert"); // <Ert>
        writer.writeCharacters(metadata.getErtStr());
        writer.writeEndElement(); // </Ert>

        writer.writeStartElement("Scet"); // <Scet>
        writer.writeCharacters(metadata.getScetStr());
        writer.writeEndElement(); // </Scet>

        if (metadata.getSol() != null) {
            writer.writeStartElement("Lst"); // <Lst>
            writer.writeCharacters(metadata.getSolStr());
            writer.writeEndElement(); // </Lst>
        }
        writer.writeStartElement("SourcePacketSequenceCount"); // <SourcePacketSequenceCount>
        writer.writeCharacters(Long.toString(this.packetSequenceNumber));
        writer.writeEndElement(); // </SourcePacketSequenceCount>

        writer.writeStartElement("RelayScid"); // <RelayScid>
        writer.writeCharacters(Long.toString(this.relayScid));
        writer.writeEndElement(); // </RelayScid>

        writer.writeStartElement("TransactionId"); // <TransactionId>
        writer.writeCharacters(getTransactionId() != null ? getTransactionId() : "");
        writer.writeEndElement(); // </TransactionId>

        writer.writeStartElement("DvtCoarse"); // <DvtCoarse>
        writer.writeCharacters(Long.toString(metadata.getDvtCoarse()));
        writer.writeEndElement(); // </DvtCoarse>

        writer.writeStartElement("DvtFine"); // <DvtFine>
        writer.writeCharacters(Long.toString(metadata.getDvtFine()));
        writer.writeEndElement(); // </DvtFine>

        writer.writeStartElement("PartLength"); // <PartLength>
        writer.writeCharacters(Long.toString(this.partLength));
        writer.writeEndElement(); // </PartLength>

        writer.writeStartElement("PartNumber"); // <PartNumber>
        writer.writeCharacters(Long.toString(this.partNumber));
        writer.writeEndElement(); // </PartNumber>

        writer.writeStartElement("PartOffset"); // <PartOffset>
        writer.writeCharacters(Long.toString(this.partOffset));
        writer.writeEndElement(); // </PartOffset>

        writer.writeStartElement("CommandNumber"); // <CommandNumber>
        writer.writeCharacters(Long.toString(metadata.getCommandNumber()));
        writer.writeEndElement(); // </CommandNumber>

        writer.writeStartElement("SequenceId"); // <SequenceId>
        writer.writeCharacters(Long.toString(metadata.getSequenceId()));
        writer.writeEndElement(); // </SequenceId>

        writer.writeStartElement("SequenceVersion"); // <SequenceVersion>
        writer.writeCharacters(Long.toString(metadata.getSequenceVersion()));
        writer.writeEndElement(); // </SequenceVersion>

        writer.writeStartElement("TotalParts"); // <TotalParts>
        writer.writeCharacters(Long.toString(metadata.getTotalParts()));
        writer.writeEndElement(); // </TotalParts>

        writer.writeStartElement("CfdpTransactionId"); // <CfdpTransactionId>
        writer.writeCharacters(Long.toUnsignedString(((IReferenceProductMetadataProvider)metadata).getCfdpTransactionId()));
        writer.writeEndElement(); // </CfdpTransactionId>

        writer.writeEndElement(); // </ProductPart>
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("ReferenceProductPart [partNumber=");
        builder.append(getPartNumber());
        builder.append(", pduType=");
        builder.append(getPartPduType());
        builder.append(", offset=");
        builder.append(getPartOffset());
        builder.append(", sequenceNumber=");
        builder.append(getPacketSequenceNumber());
        builder.append(", partLength=");
        builder.append(getPartLength());
        builder.append("]");
        return builder.toString();
    }
}
