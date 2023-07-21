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
package jpl.gds.product.impl.builder;

import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.product.api.*;
import jpl.gds.product.api.builder.*;
import jpl.gds.product.api.config.IProductPropertiesProvider;
import jpl.gds.product.impl.ReferencePduType;
import jpl.gds.shared.log.IPublishableLogMessage;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.template.ProductTemplateManager;
import jpl.gds.shared.template.TemplateException;
import jpl.gds.shared.template.TemplateManager;
import jpl.gds.shared.time.*;
import jpl.gds.shared.time.SclkFmt.SclkFormatter;
import org.apache.velocity.Template;
import org.springframework.context.ApplicationContext;

import java.io.EOFException;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.StringTokenizer;

/**
 * This is the generic TransactionLogStorage class. It is responsible for reading and 
 * writing the product builder's transaction log and the product EMD file.
 * 
 */
public class ReferenceTransactionLogStorage extends AbstractTransactionLogStorage implements ITransactionLogStorage
{
    private ProductTemplateManager templateMgr;
	
	private static final String EMD_VERSION = "1";
	
	private static final long _fineUpperLimit = new DataValidityTime().getFineUpperLimit();
	
	private final SclkFormatter sclkFmt;

	/**
	 * 5/20/2016 Adding parts needs local offset, but the reference version doesn't use it.  Creating a
	 * constant to be used for this.
	 */
	private static final long LOCAL_OFFSET = -1;

	private boolean checkEmbeddedEpdu;
    /**
     * Creates an instance of ReferenceTransactionLogStorage.
     * @param appContext the current application context
     * @param instanceFactory product builder object instance factory
     */
    public ReferenceTransactionLogStorage(final ApplicationContext appContext, final IProductBuilderObjectFactory instanceFactory) {
    	super(appContext, instanceFactory);
    	sclkFmt = TimeProperties.getInstance().getSclkFormatter();
    	
    	try {
    		templateMgr = appContext.getBean(ProductTemplateManager.class);
    		this.checkEmbeddedEpdu = appContext.getBean(IProductPropertiesProvider.class).checkEmbeddedEpdu();
    	} catch (final Exception e) {
			e.printStackTrace();
			final IPublishableLogMessage log = appContext.getBean(IStatusMessageFactory.class).createPublishableLogMessage(TraceSeverity.FATAL,
                    "Product Builder could not be initialized");
			appContext.getBean(IMessagePublicationBus.class).publish(log);
		}
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeOpenEvent(final Writer writer,
                               final IProductPartProvider part)
        throws IOException, ProductStorageException
    {
    	
        if (part == null) {
            log.error("Internal error: part is null in writeOpenEvent()");
            throw new ProductStorageException("Unable to write open event to product transaction log");
        }
        
        final DateFormat dateFormat = TimeUtility.getFormatterFromPool();
        
        final IReferenceProductMetadataProvider md = (IReferenceProductMetadataProvider)part.getMetadata();
        
        try {
        	// Version of event log format
        	writer.write(EVENT_FORMAT_VERSION + EVENT_FIELD_SEPARATOR);

        	// Type of event (OPEN)
        	writer.write(OPEN_EVENT + EVENT_FIELD_SEPARATOR);

        	// Wall clock
        	writer.write(System.currentTimeMillis() + EVENT_FIELD_SEPARATOR);
        	
        	// Part number
        	writer.write(part.getPartNumber() + EVENT_FIELD_SEPARATOR);

            // VCID
            writer.write(part.getVcid() + EVENT_FIELD_SEPARATOR);
            
        	// Spacecraft ID
        	writer.write(part.getMetadata().getScid() + EVENT_FIELD_SEPARATOR);

        	// Relay spacecraft ID
        	writer.write(part.getRelayScid() + EVENT_FIELD_SEPARATOR);

        	// APID
        	writer.write(part.getApid() + EVENT_FIELD_SEPARATOR);

        	// Product filename (without APID directory)
        	try {
        	    final IProductMissionAdaptor adaptor = appContext.getBean(IProductMissionAdaptor.class);
        	    final String filename = adaptor.getFromFilenameMap(md.getCfdpTransactionId());
        	    writer.write(filename + EVENT_FIELD_SEPARATOR);
        	} catch (final Exception e) {
        	    log.error("Unable to create product mission adaptor in transaction log storage");
                writer.write(Long.toUnsignedString(md.getCfdpTransactionId()) + EVENT_FIELD_SEPARATOR);
        	} 
        	
        	// Filename elements
            writer.write(md.getCommandNumber() + EVENT_FIELD_SEPARATOR);
        	writer.write(md.getSequenceId() + EVENT_FIELD_SEPARATOR);
        	writer.write(md.getSequenceVersion() + EVENT_FIELD_SEPARATOR);
        	writer.write(md.getDvtCoarse() + EVENT_FIELD_SEPARATOR);
        	writer.write(md.getDvtFine() + EVENT_FIELD_SEPARATOR);
        	writer.write(md.getXmlVersion() + EVENT_FIELD_SEPARATOR);

        	// Total parts in product
        	writer.write(md.getTotalParts() + EVENT_FIELD_SEPARATOR);

        	// Other Reference-specific metadata values 
        	writer.write(md.getSourceEntityId() + EVENT_FIELD_SEPARATOR);
        	writer.write(part.getTransactionId() + "\n");
        } finally {
        	if (dateFormat != null) {
        		TimeUtility.releaseFormatterToPool(dateFormat);
        	}
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void writePartEvent(final Writer writer, final IProductPartProvider genericPart,
			final IProductStorageMetadata storageMetadata) throws IOException, ProductStorageException {
    		writePartEvent(writer, genericPart);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void writePartEvent(final Writer writer, final IProductPartProvider part) throws IOException {
    	final DateFormat dateFormat = TimeUtility.getFormatterFromPool();

    	try {
    		// Version of event log format
    		writer.write(EVENT_FORMAT_VERSION + EVENT_FIELD_SEPARATOR);

    		// Type of event (PART)
    		writer.write(PART_EVENT + EVENT_FIELD_SEPARATOR);

    		// Wall clock
    		writer.write(System.currentTimeMillis() + EVENT_FIELD_SEPARATOR);

    		// Part PDU type
    		writer.write(part.getPartPduType() + EVENT_FIELD_SEPARATOR);

    		// Spacecraft ID
    		writer.write(part.getMetadata().getScid() + EVENT_FIELD_SEPARATOR);

    		// Relay spacecraft ID
    		writer.write(part.getRelayScid() + EVENT_FIELD_SEPARATOR);

    		// Part number (starting with 1)
    		writer.write(part.getPartNumber() + EVENT_FIELD_SEPARATOR);

    		// Offset of part in product (starting with 0)
    		writer.write(part.getPartOffset() + EVENT_FIELD_SEPARATOR);

    		// Length of part, in bytes (not including tertiary packet header)
    		writer.write(part.getData().length + EVENT_FIELD_SEPARATOR);

    		// ERT of packet containing part
    		writer.write(part.getMetadata().getErt().getFormattedErt(true) + EVENT_FIELD_SEPARATOR);

    		// SCLK of packet containing part
    		writer.write(part.getMetadata().getSclk() + EVENT_FIELD_SEPARATOR);

    		// SCET of packet (correlated upstream at packet extract)
    		writer.write(part.getMetadata().getScet().getFormattedScet(true) + EVENT_FIELD_SEPARATOR);

    		if (setSolTimes) {
    			// LST of packet (correlated upstream at packet extract)
    			writer.write(part.getMetadata().getSol().getFormattedSol(true) + EVENT_FIELD_SEPARATOR);
    		} else {
    			writer.write(EVENT_FIELD_SEPARATOR);
    		}
    		
    		// Packet sequence number of packet containing part
    		writer.write(part.getPacketSequenceNumber() + EVENT_FIELD_SEPARATOR);

    		// CFDP Transaction ID
    		writer.write(part.getTransactionId() + EVENT_FIELD_SEPARATOR);
    		
    		// If this part had the end of data PDU then write the product
    		// checksum and size also
    		if (part.getPartPduType().isEndOfData() || part.getPartPduType().isEnd()) {
    			writer.write(part.getMetadata().getChecksum() + EVENT_FIELD_SEPARATOR);
    			writer.write(part.getMetadata().getFileSize() + EVENT_FIELD_SEPARATOR);
    		}

    		// Indicate if part starts a new record
    		if (part.getGroupingFlags() == IProductPartProvider.NOT_IN_GROUP) {
    			writer.write(RECORD_NOT);
    		} else if (part.getGroupingFlags() == IProductPartProvider.FIRST_IN_GROUP) {
    			writer.write(RECORD_START);
    		} else if (part.getGroupingFlags() == IProductPartProvider.CONTINUING_GROUP) {
    			writer.write(RECORD_CONTINUED);
    		} else if (part.getGroupingFlags() == IProductPartProvider.LAST_IN_GROUP) {
    			writer.write(RECORD_END);
    		} else {
    			writer.write(RECORD_INVALID);
    		}
    		writer.write("\n");
    	} finally {
    		if (dateFormat != null) {
    			TimeUtility.releaseFormatterToPool(dateFormat);
    		}
    	}
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.product.impl.builder.AbstractTransactionLogStorage#loadTransactionLog(java.lang.String, java.io.LineNumberReader)
     */
    @Override
    public IProductTransactionProvider loadTransactionLog(final String id, final LineNumberReader reader)
        throws ProductStorageException
    {
        final IProductTransactionUpdater tx = this.instanceFactory.convertTransactionToUpdater(this.instanceFactory.createProductTransaction());
        final IReferenceProductMetadataUpdater md = (IReferenceProductMetadataUpdater) tx.getMetadata();
        boolean done = false;

        // for part info
        int relayScid;
        int partNumber;
        long partOffset;
        int partLength;
        String partErtStr;
        String partSclkStr;
        String partScetStr;
        String partSolStr = null;
        int partPacketSequenceNumber;
        int groupingFlags;
        IPduType pduType;

		/* 01/28/2015. Added flag to track latest seen EPDU type. */
		boolean lastEpduWasEmbedded = true;

        final DateFormat dateFormat = TimeUtility.getFormatterFromPool();
        try {
            while (!done) {
                final String line = reader.readLine();
                if (line == null) {
                    done = true;
                    break;
                }
                final StringTokenizer st = new StringTokenizer(line, EVENT_FIELD_SEPARATOR);
                String t = st.nextToken(); // event log format version
                if(!t.equals(EVENT_FORMAT_VERSION)) {
                	throw new ProductStorageException("Could not add product part read from product transaction log"
     			           + " because of a mismatched event format.  Expected " + EVENT_FORMAT_VERSION + " but was " + t);
                }
                t = st.nextToken(); // event type
                if (t.equals(PART_EVENT)) { // check first, it's the most common
                    st.nextToken(); // wall clock
                    t = st.nextToken(); // part PDU type
                    pduType = ReferencePduType.valueOf(t);
                    st.nextToken(); // scid
                    t = st.nextToken(); // relay scid
                    relayScid = parseInt(t);
                    t = st.nextToken(); // part number
                    partNumber = parseInt(t);
                    t = st.nextToken(); // part offset
                    partOffset = parseLong(t);
                    t = st.nextToken(); // part length
                    partLength = parseInt(t);
                    t = st.nextToken(); // ERT
                    partErtStr = t;
                    t = st.nextToken(); // SCLK
                    partSclkStr = t;
                    t = st.nextToken(); // SCET
                    partScetStr = t;
                    if (setSolTimes) {
                    	t = st.nextToken(); // SOL
                    	partSolStr = t;
                    }
                    IAccurateDateTime partErt = null;
                    IAccurateDateTime partScet = null;
                    ILocalSolarTime partSol = null;
                    ISclk partSclk = null;
                    try {
                        partErt = new AccurateDateTime(partErtStr);
                        partScet = new AccurateDateTime(partScetStr);
                        if (setSolTimes) {
                        	partSol = LocalSolarTimeFactory.getNewLst(partSolStr, appContext.getBean(IContextIdentification.class).getSpacecraftId());
                        }
                    } catch (final ParseException pex) {
                        pex.printStackTrace();
                    }
                    try {
                        partSclk = sclkFmt.valueOf(partSclkStr);
                    } catch (final NumberFormatException nex) {
                        log.error("Error parsing SCLK from product transaction log");
                    }
                    t = st.nextToken(); // Packet sequence number
                    partPacketSequenceNumber = parseInt(t);
                    
                    t = st.nextToken(); // transaction ID
                    tx.setId(t);

					/* 01/28/2015. We have to keep track of the last
					 * EPDU type seen, because that indicates how to adjust the total
					 * part count.
					 */
					if (pduType.isEnd()) {
						lastEpduWasEmbedded = false;	
					} else if (pduType.isEndOfData()) {
						lastEpduWasEmbedded = true;
					}

                    // if this part contained the end PDU then load
                    // the file checksum and size
                    if (pduType.isEnd() || pduType.isEndOfData()) {
                        t = st.nextToken(); // product checksum
                        md.setChecksum(parseLong(t));
                        t = st.nextToken(); // product size
                        md.setFileSize(parseLong(t));                        
                    }
                        
                    t = st.nextToken(); // Record start (packet grouping flags)
                    groupingFlags = parseGroupingFlags(t);
                    try {
                    	/*  A stand alone EPDU does not
                    	 * contain product data and should not be included in the 
                    	 * EMD file received packet list, so we skip adding it to
                    	 * the transaction here.
                    	 * 
                    	 * 
                    	 */
                    	if (!pduType.isEnd()) {
                    		tx.addPart(partNumber, partOffset, LOCAL_OFFSET, partLength,
                    				partErt, partSclk, partScet, partSol,
                    				partPacketSequenceNumber, relayScid, groupingFlags, pduType);
                    	}
                    }
                    catch( final Exception addException ) {
                    	
                    	final StringBuilder messageBuilder = 
                    	new StringBuilder( "Could not add product part read from product transaction log"
                    			           + " because of " +
                    			           addException.getClass().getName() );
                    	throw new ProductStorageException( messageBuilder.toString() );
                    }
                }
                else if (t.equals(OPEN_EVENT)) {
                    st.nextToken(); // wall clock
                    t = st.nextToken(); // part number
                    partNumber = (parseInt(t));
                    t = st.nextToken(); // vcid
                    md.setVcid(parseInt(t));
                    t = st.nextToken(); // scid
                    md.setScid(parseInt(t));
                    st.nextToken(); // relay scid
                    t = st.nextToken(); // apid
                    md.setApid(parseInt(t));
                    t = st.nextToken(); // filename
                    tx.setFilename(t);
                    md.setFilename(t);
                    t = st.nextToken(); // command number
                    md.setCommandNumber(parseInt(t));
                    t = st.nextToken(); // sequence id
                    md.setSequenceId(parseInt(t));
                    t = st.nextToken(); // sequence version
                    md.setSequenceVersion(parseInt(t));
                    t = st.nextToken(); // dvt coarse
                    md.setDvtCoarse(parseLong(t));

                    t = st.nextToken(); // dvt fine

                    int dvtfine = parseInt(t);

                    if (dvtfine > _fineUpperLimit)
                    {
                        log.error("ReferenceTransactionLogStorage DVT fine " +
                                  "truncated from "                     +
                                  dvtfine                               +
                                  " to "                                +
                                  _fineUpperLimit);

                        dvtfine = (int)_fineUpperLimit;
                    }

                    md.setDvtFine(dvtfine);

                    t = st.nextToken(); // product definition version
                    md.setXmlVersion(parseInt(t));
                    t = st.nextToken(); // total parts
                    tx.setTotalParts(parseInt(t));
                    t = st.nextToken(); // source entity id
                    md.setSourceEntityId(parseInt(t));
                    t = st.nextToken(); // transaction id
                    final String[] pieces = t.split("/"); // splits into APID & CFDP transaction ID
                    md.setCfdpTransactionId(parseLong(pieces[1]));

                    final DataValidityTime sclk = new DataValidityTime(md.getDvtCoarse(), md.getDvtFine());
                    md.setSclk(sclk);
                    final IAccurateDateTime scet = SclkScetUtility.getScet(sclk, null, md.getScid());
                    if (scet != null) {
                        md.setScet(scet);
                        if (setSolTimes) {
                        	md.setSol(LocalSolarTimeFactory.getNewLst(scet, appContext.getBean(IContextIdentification.class).getSpacecraftId()));
                        }
                    }
					tx.setId(Long.toUnsignedString(md.getCfdpTransactionId()));

					if (tx.getFilename().equals("null")
							|| tx.getFilename().equals("")) {
						final DateFormat df = TimeUtility.getDoyFormatterFromPool();
						final String fname = md.getSourceEntityId() + "-"
								+ Long.toUnsignedString(md.getCfdpTransactionId())
								+ "-" + df.format(new AccurateDateTime());
						TimeUtility.releaseDoyFormatterToPool(df);
						tx.setFilename(fname);
						md.setFilename(tx.getFilename());
					}

					if (partNumber == 0) {
						tx.setReceivedMetadata(true);
					}

				} else if (t.equals(ASSEMBLE_EVENT)) {
					st.nextToken(); // wall clock
					st.nextToken(); // assembly trigger
				}

			}

			/* At this point we should know the total part count and what type of EPDU we have.
			 * If so configured, we must adjust the total part count based upon the type of the EPDU.
			 */
			if (md.getTotalParts() != 0) {
				if (checkEmbeddedEpdu && !lastEpduWasEmbedded) {
					md.setTotalParts(md.getTotalParts() - 1);
                    log.debug("EPDU was SEPARATE for product ", md.getFilename(), ". Total data part count is now ",
                            md.getTotalParts());
				} else {
                    log.debug("Embedded EPDU check is disabled, OR EPDU was EMBEDDED for product ", md.getFilename(),
                            ". Total data part count is ", md.getTotalParts());
				}
			} else {
                log.debug(
                        "No MPDU was received for product ", md.getFilename(), 
						". Total data part count will be 0.");
			}
		}
        catch (final EOFException e) {
            done = true;
        }
        catch (final IOException e) {
            throw new ProductStorageException("Error reading product transaction log", e);
        }
        finally {
            if (reader != null) {
                try { reader.close(); } catch (final IOException e) { /* ok */ }
            }
            if (dateFormat != null) {
            	TimeUtility.releaseFormatterToPool(dateFormat);
            }
        }
        return tx;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void storeTransactionToMetadataFile(final Writer w, final IProductTransactionProvider tx)
        throws ProductStorageException
    {
    	final HashMap<String,Object> map = new HashMap<String,Object>();
    	
		final IProductMetadataUpdater md = this.instanceFactory.convertToMetadataUpdater(getProductMetadata(tx));
		final IContextConfiguration config = appContext.getBean(IContextConfiguration.class);
        
        String xmlText = "";

		try {
			final Template template = templateMgr.getTemplateForType("emd", EMD_VERSION);
			md.setTemplateContext(map);
			if (config != null) {
				config.setTemplateContext(map);
			}
			tx.setTemplateContext(map);
			
			xmlText = TemplateManager.createText(template, map);
		} catch (final TemplateException e) {
			e.printStackTrace();
			throw new ProductStorageException("Unable to access product EMD template");
		}

		try {
			w.write(xmlText);
		} catch (final IOException e) {
			throw new ProductStorageException("I/O Exception storing transaction log: "
					+ e.getMessage(), e);
		}
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IProductMetadataProvider getProductMetadata(final IProductTransactionProvider mtx) {
		final IProductMetadataUpdater result = instanceFactory.convertToMetadataUpdater(mtx.getMetadata());
		IAccurateDateTime ert = null;

		for (int i = mtx.getFirstPartNumber(); i > 0 && i <= mtx.getLastPartNumber(); i++) {
			final IProductStorageMetadata part = mtx.getStorageMetadataForPart(i);
			if (part != null) {
				if (ert == null) {
					ert = part.getErt();
				}
				else {
					if (ert.compareTo(part.getErt()) < 0) {
						ert = part.getErt();
					}
				}
			}
		}
		/*
		 * Set ERT from ReceivedPartsTracker
		 */
		synchronized (tracker) {
			result.setErt(tracker.getEarliestERT(mtx.getMetadata().getVcid(), mtx.getId()));
		}

		result.setProductCreationTime(new AccurateDateTime());
		// Map DVT to Scet and replace last part scet in the product with the
		// mapped value if it can be obtained

		final DataValidityTime sclkt = new DataValidityTime(result.getDvtCoarse(), result.getDvtFine());
		final IAccurateDateTime scet = SclkScetUtility.getScet(sclkt, null, result.getScid());
		result.setSclk(sclkt);
		result.setScet(scet);
		if (scet != null) {
			if (setSolTimes) {
				result.setSol(LocalSolarTimeFactory.getNewLst(scet, result.getScid()));
			}
		}
		return result;
	}
}
