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
package jpl.gds.product.utilities.file;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import jpl.gds.context.api.IGeneralContextInformation;
import jpl.gds.product.api.*;
import jpl.gds.product.api.builder.ProductStorageConstants;
import jpl.gds.product.api.file.IProductMetadata;
import jpl.gds.shared.gdr.GDR;
import org.springframework.context.ApplicationContext;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import jpl.gds.product.api.config.IProductPropertiesProvider;
import jpl.gds.shared.file.FileUtility;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.CoarseFineTime;
import jpl.gds.shared.time.FastDateFormat;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.LocalSolarTimeFactory;
import jpl.gds.shared.time.Sclk;
import jpl.gds.shared.time.SclkFmt.SclkFormatter;
import jpl.gds.shared.time.TimeProperties;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.shared.xml.XmlUtility;
import jpl.gds.shared.xml.parse.SAXParserPool;

/**
 * This class is responsible for the metadata for a generic product. The metadata
 * for all product parts is stored by this class only during product decom.
 * During product generation, part metadata is stored in the ReferenceProductPart.
 * 
 */
public class ReferenceProductMetadata extends AbstractProductMetadata implements IProductMetadata, IReferenceProductMetadataUpdater {
    private static final Calendar      calendar    =
                                           FastDateFormat.getStandardCalendar();

	private long cfdpTransactionId;
	private String filename;
	private int sourceEntityId; // needed to form filenames when MPDU is missing
	private final SclkFormatter sclkFmt;
    /** the data file name */
    protected String dataFilename = "";
//    protected boolean isCompressed;

	/**
	 * Constructor.
	 * 
	 * @param appContext the current application context
	 */
	public ReferenceProductMetadata(final ApplicationContext appContext) {
		super(appContext);
		sclkFmt = TimeProperties.getInstance().getSclkFormatter();
	}

	/**
	 * Loads product metadata from the XML product metadata file. Used during
	 * decom only, not for message parsing.
	 * 
	 * @param filename the full path to the file to load
	 * @throws ProductException if parsing or IO error
	 */
	@Override
	public void loadFile(final String filename) throws ProductException {
		if (filename == null) {
            if (!sseFlag.isApplicationSse()) {
				parseLogger.error("Product EMD path is undefined.");
			}
			throw new ProductException("Product EMD path is undefined.");
		}
		final File path = new File(filename);
		parseLogger.info("Parsing product metadata from " + FileUtility.createFilePathLogMessage(path));
		SAXParser sp = null;
		try {
			final File file = new File(filename);
			sp = SAXParserPool.getInstance().getNonPooledParser();
			sp.parse(file, new MetadataParseHandler());
			setFilename(file.getName());
			if (this.parts.size() == 0) {
				final IProductPartUpdater part = partFactory.createPartUpdater();

				part.setPartNumber(1);
				part.setPartOffset(0L);

				this.parts.add(part);
			}
		} catch (final SAXException e) {
			parseLogger.error(e.getMessage());
			throw new ProductException(e.getMessage(), e);
		} catch (final ParserConfigurationException e) {
			parseLogger.error("Unable to configure sax parser to read product EMD file");
			throw new ProductException(
					"Unable to configure sax parser to product EMD file", e);
		} catch (final Exception e) {
			e.printStackTrace();
			parseLogger.error("Unexpected error parsing or reading product EMD file");
			throw new ProductException(
					"Unexpected error parsing or reading product EMD file", e);
		} 
	}


	@Override
	public String toCsv(final List<String> csvColumns)
	{
        final StringBuilder csv  = new StringBuilder(1024);
        final StringBuilder csv2 = new StringBuilder(1024);

		csv.append(CSV_COL_HDR);

        for (final String cce : csvColumns)
        {
            final String upcce = cce.toUpperCase();

            csv.append(CSV_COL_SEP);

            switch (upcce)
            {
                case "SESSIONID":
                    csv.append(sessionId);
                    break;

                case "SESSIONHOST":
                    append(csv, sessionHost);
                    break;

                case "VCID":
                    append(csv, getVcid());
                    break;

                case "APID":
                    csv.append(getApid());
                    break;

                case "PRODUCTTYPE":
                    append(csv, productType);
                    break;

                case "PRODUCTCREATIONTIME":
                    append(csv, getProductCreationTimeStr());
                    break;

                case "SCET":
                    append(csv, getScetStr());
                    break;

                case "LST":
                    if (useSolTime)
                    {
                        append(csv, getSolStr());
                    }
                    break;

                case "ERT":
                    append(csv, getErtStr());
                    break;

                case "SCLK":
                    append(csv, getSclkStr());
                    break;

                case "FULLPATH":
                    append(csv, getAbsoluteDataFile());
                    break;

                case "COMMANDNUMBER":
                    csv.append(getCommandNumber());
                    break;

                case "DVTCOARSE":
                    csv.append(getDvtCoarse());
                    break;

                case "DVTFINE":
                    csv.append(getDvtFine());
                    break;

                case "TOTALPARTS":
                    csv.append(getTotalParts());
                    break;

                case "SEQID":
                    csv.append(getSequenceId());
                    break;

                case "SEQVERSION":
                    csv.append(getSequenceVersion());
                    break;

                case "CFDPTRANSACTIONID":
                    csv.append(Long.toUnsignedString(getCfdpTransactionId()));
                    break;

                case "FILESIZE":
                    csv.append(getFileSize());
                    break;

                case "CHECKSUM":
                    csv.append(getChecksum());
                    break;

                case "GROUNDSTATUS":
                    append(csv, getGroundStatus());
                    break;

                case "VERSION":
                    append(csv, getProductVersion());
                    break;

                case "RCT":
                   if (getRct() != null)
                   {
                       csv.append(FastDateFormat.format(getRct(), calendar, csv2));
                   }
                   break;

                default:
                    if (! csvSkip.contains(upcce))
                    {
                        parseLogger.warn("Column " +
                                           cce       +
                                           " is not supported, skipped");

                        csvSkip.add(upcce);
                    }
                    break;
            }
        }

		csv.append(CSV_COL_TRL);

		return csv.toString();
	}

	@Override
	public void parseCsv(final String csvStr,
                         final List<String> csvColumns)
    {
		// The following removes the start/end quotes w/ the substring
		// and splits based on ",".  It leaves the trailing empty string in the case that 
		// csvStr ends with "".  The empty strings server as place holders.
        final String[] dataArray = csvStr.substring(1, csvStr.length()-1).split("\",\"",-1);

        if ((csvColumns.size() + 1) != dataArray.length)
        {
            throw new IllegalArgumentException("CSV column length mismatch, received " +
                                               dataArray.length                        +
                                               " but expected "                        +
                                               (csvColumns.size() + 1));
        }

        // Clear everything we might process, in case empty column or not in list

        setSessionId(0L);
        setSessionHost(null);
        setVcid(null);
        setApid(0);
        setProductType(UNKNOWN_PRODUCT_TYPE);
        setProductCreationTime(null);
        setScet(IAccurateDateTime.class.cast(null)); // Ambiguous otherwise
        setSol(null);
        setErt(null);
        setSclk(CoarseFineTime.class.cast(null)); // Ambiguous otherwise
        setFullPath(null);
        setCommandNumber(0);
        setDvtCoarse(0L);
        setDvtFine(0L);
        setTotalParts(0);
        setSequenceId(0);
        setSequenceVersion(0);
        setCfdpTransactionId(0L);
        setFileSize(0L);
        setChecksum(0L);
        setGroundStatus(ProductStatusType.UNKNOWN);
        setProductVersion(null);
        setRct(null);

        int    next  = 1; // Skip recordType
        String token = null;

        for (final String cce : csvColumns)
        {
            token = dataArray[next].trim();

            ++next;

            if (token.isEmpty())
            {
                continue;
            }

            final String upcce = cce.toUpperCase();

            try
            {
                switch (upcce)
                {
                    case "SESSIONID":
                        setSessionId(Long.valueOf(token));
                        break;

                    case "SESSIONHOST":
                        setSessionHost(token);
                        break; 

                    case "VCID":
                        setVcid(Integer.valueOf(token));
                        break;

                    case "APID":
                        setApid(Integer.valueOf(token));
                        break;

                    case "PRODUCTTYPE":
                        setProductType(token);
                        break;

                    case "PRODUCTCREATIONTIME":
                        setProductCreationTime(new AccurateDateTime(token));
                        break;

                    case "SCET":
                        setScet(new AccurateDateTime(token));
                        break;

                    case "LST":
                        if (useSolTime)
                        {
                            setSol(LocalSolarTimeFactory.getNewLst(token, getScid()));
                        }
                        break;

                    case "ERT":
                        setErt(new AccurateDateTime(token));
                        break;

                    case "SCLK":
                        setSclk(token);
                        break;

                    case "FULLPATH":
                        setFullPath(token);
                        break;

                    case "COMMANDNUMBER":
                        setCommandNumber(Integer.valueOf(token));
                        break;

                    case "DVTCOARSE":
                        setDvtCoarse(Long.valueOf(token));
                        break;

                    case "DVTFINE":
                        setDvtFine(Long.valueOf(token));
                        break;

                    case "TOTALPARTS":
                        setTotalParts(Integer.valueOf(token));
                        break;

                    case "SEQID":
                        setSequenceId(Integer.valueOf(token));
                        break;

                    case "SEQVERSION":
                        setSequenceVersion(Integer.valueOf(token));
                        break;

                    case "CFDPTRANSACTIONID":
                        setCfdpTransactionId(Long.parseUnsignedLong(token));
                        break;

                    case "FILESIZE":
                        setFileSize(Long.valueOf(token));
                        break;

                    case "CHECKSUM":
                        setChecksum(Long.valueOf(token));
                        break;

                    case "GROUNDSTATUS":
                        setGroundStatus(ProductStatusType.valueOf(token));
                        break;

                    case "VERSION":
                        setProductVersion(token);
                        break;

                    case "RCT":
                        setRct(new AccurateDateTime(token));
                        break; 

                    default:
                        if (! csvSkip.contains(upcce))
                        {
                            parseLogger.warn("Column " +
                                               cce       +
                                               " is not supported, skipped");

                            csvSkip.add(upcce);
                        }

                        break;
                }
             }
             catch (final RuntimeException re)
             {
                 re.printStackTrace();

                 throw re;
		     }
             catch (final Exception e)
             {
                 e.printStackTrace();
             }
        }
	}


	/* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setTemplateContext(java.util.Map)
     */
	@Override
	public void setTemplateContext(final Map<String, Object> map) {
		super.setTemplateContext(map);
		map.put("transactionId", this.cfdpTransactionId);
		map.put("lastPartErt", getErtStr());
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.xml.stax.StaxSerializable#generateStaxXml(javax.xml.stream.XMLStreamWriter)
	 */
	@Override
	public void generateStaxXml(final XMLStreamWriter writer) throws XMLStreamException
	{
		writer.writeStartElement("ReferenceProductMetadata"); // <MsapProductMetadata>
		writer.writeAttribute("class",getClass().getName());

		writer.writeStartElement("GroundCreationTime"); // <GroundCreationTime>
		writer.writeCharacters(getProductCreationTimeStr());
		writer.writeEndElement(); // </GroundCreationTime>

		writer.writeStartElement("TestSessionId"); // <TestSessionId>
		writer.writeCharacters(Long.toString(getSessionId()));
		writer.writeEndElement(); // </TestSessionId>

		writer.writeStartElement("TestSessionHost"); // <TestSessionHost>
		writer.writeCharacters(StringUtil.safeTrim(getSessionHost()));
		writer.writeEndElement(); // </TestSessionHost>

		writer.writeStartElement("Scid"); // <Scid>
		writer.writeCharacters(Long.toString(getScid()));
		writer.writeEndElement(); // </Scid>

		writer.writeStartElement("Apid"); // <Apid>
		writer.writeCharacters(Long.toString(getApid()));
		writer.writeEndElement(); // </Apid>

		writer.writeStartElement("ProductName"); // <ProductName>
		writer.writeCharacters(getProductType());
		writer.writeEndElement(); // </ProductName>

		writer.writeStartElement("Vcid"); // <Vcid>
		if (getVcid() != null) {
			writer.writeCharacters(Long.toString(getVcid()));
		} else {
			writer.writeCharacters("0");
		}
		writer.writeEndElement(); // </Vcid>

		writer.writeStartElement("GroundStatus"); // <GroundStatus>
		writer.writeCharacters(getGroundStatus().toString());
		writer.writeEndElement(); // </GroundStatus>

		writer.writeStartElement("ProductVersion"); // <ProductVersion>
		writer.writeCharacters((getProductVersion() == null) ? "" : getProductVersion());
		writer.writeEndElement(); // </ProductVersion>

		writer.writeStartElement("DataFileName"); // <DataFileName>
		writer.writeCharacters(getAbsoluteDataFile());
		writer.writeEndElement(); // </DataFileName>

		writer.writeStartElement("SequenceId"); // <SequenceId>
		writer.writeCharacters(Long.toString(getSequenceId()));
		writer.writeEndElement(); // </SequenceId>

		writer.writeStartElement("SequenceVersion"); // <SequenceVersion>
		writer.writeCharacters(Long.toString(getSequenceVersion()));
		writer.writeEndElement(); // </SequenceVersion>

		writer.writeStartElement("CommandNumber"); // <CommandNumber>
		writer.writeCharacters(Long.toString(getCommandNumber()));
		writer.writeEndElement(); // </CommandNumber>

		writer.writeStartElement("DvtCoarse"); // <DvtCoarse>
		writer.writeCharacters(Long.toString(getDvtCoarse()));
		writer.writeEndElement(); // </DvtCoarse>

		writer.writeStartElement("DvtFine"); // <DvtFine>
		writer.writeCharacters(Long.toString(getDvtFine()));
		writer.writeEndElement(); // </DvtFine>

		writer.writeStartElement("Sclk"); // <Sclk>
		writer.writeCharacters(getSclkStr());
		writer.writeEndElement(); // </Sclk>

		writer.writeStartElement("Scet"); // <Scet>
		writer.writeCharacters(getScetStr());
		writer.writeEndElement(); // </Scet>

		if (useSolTime && (getSol() != null))
		{
			writer.writeStartElement("Lst"); // <Lst>
			writer.writeCharacters(getSolStr());
			writer.writeEndElement(); // </Lst>
		}

		writer.writeStartElement("LastPartErt"); // <LastPartErt>
		writer.writeCharacters(getErtStr());
		writer.writeEndElement(); // </LastPartErt>

		writer.writeStartElement("TotalParts"); // <TotalParts>
		writer.writeCharacters(Long.toString(getTotalParts()));
		writer.writeEndElement(); // </TotalParts>

		writer.writeStartElement("CfdpTransactionId"); // <CfdpTransactionId>
		writer.writeCharacters(Long.toUnsignedString(getCfdpTransactionId()));
		writer.writeEndElement(); // </CfdpTransactionId>

		writer.writeStartElement("ProductChecksum"); // <ProductChecksum>
		writer.writeCharacters(Long.toString(getChecksum()));
		writer.writeEndElement(); // </ProductChecksum>

		writer.writeStartElement("ProductFileSize"); // <ProductFileSize>
		writer.writeCharacters(Long.toString(getFileSize()));
		writer.writeEndElement(); // </ProductFileSize>

		writer.writeEndElement(); // </ReferenceProductMetadata>
	}

	/**
	 * {@inheritDoc}
	 * @see AbstractProductMetadata#parseFromElement(java.lang.String, java.lang.String)
	 */
	@Override
	public boolean parseFromElement(final String elemName, final String text) {
		if (elemName.equalsIgnoreCase("CfdpTransactionId")) {
			this.cfdpTransactionId = Long.parseUnsignedLong(text);
		} else if (elemName.equalsIgnoreCase("ProductChecksum")) {
			this.checksum = XmlUtility.getLongFromText(text);
		} else if (elemName.equalsIgnoreCase("ProductFileSize")) {
			this.fileSize = XmlUtility.getLongFromText(text);
		} else {
			return super.parseFromElement(elemName, text);
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 * @see AbstractProductMetadata#getCommandId()
	 */
	@Override
	public long getCommandId() {
		return ((long) this.seqId << 32) | ((long) this.seqVersion << 16)
				| (this.commandNum);
	}

	/**
	 * {@inheritDoc}
	 * @see AbstractProductMetadata#getFilename()
	 */
	@Override
	public String getFilename() {
		return this.filename;
	}

    /**
     * {@inheritDoc}
     */
    @Override
	public void setFilename(final String name) {
		this.filename = name;
	}

	/**
	 * {@inheritDoc}
	 * @see AbstractProductMetadata#getFilenameWithPrefix()
	 */
	@Override
	public String getFilenameWithPrefix() {
		return String.format("%04d/%s", Integer.valueOf(getApid()), getFilename());
	}

	/**
	 * {@inheritDoc}
	 * @see AbstractProductMetadata#getDirectoryName()
	 */
	@Override
	public String getDirectoryName() {
		if (appContext.getBean(IProductPropertiesProvider.class).productDirNameUsesApid()) {
			return zeroPad(getApid());
		} else {
			return getProductType();
		}
	}

	/**
	 * Retrieves the CFDP transaction ID, which is what makes
	 * the product unique and links packets together.
	 * 
	 * @return Returns the transaction ID.
	 */
	@Override
	public long getCfdpTransactionId() {
		return this.cfdpTransactionId;
	}

    /**
     * {@inheritDoc}
     */
    @Override
	public void setCfdpTransactionId(final long cfdpTransactionId) {
		this.cfdpTransactionId = cfdpTransactionId;
	}

    /**
	 * This class handles the parsing of the product EMD file for product decommutation.
	 * 
	 *
	 */
	private class MetadataParseHandler extends DefaultHandler {

		private StringBuffer text;
		private boolean inPart = false;
		private boolean inSessionId = false;
        private boolean inVenue = false;
		private IProductPartUpdater part;
		private IProductMetadataUpdater md;

		/**
		 * {@inheritDoc}
		 * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
		 */
		@Override
		public void characters(final char[] chars, final int start, final int length)
				throws SAXException {
			final String newText = new String(chars, start, length);
			if (!newText.equals("\n")) {
				text.append(newText);
			}
		}

		/**
		 * {@inheritDoc}
		 * @see org.xml.sax.helpers.DefaultHandler#error(org.xml.sax.SAXParseException)
		 */
		@Override
		public void error(final SAXParseException e) throws SAXException {
			/*  Correct "apid" reference in log message */
			throw new SAXException("Parse error in product EMD file line "
					+ e.getLineNumber() + " col " + e.getColumnNumber() + ": "
					+ e.getMessage());
		}

		/**
		 * {@inheritDoc}
		 * @see org.xml.sax.helpers.DefaultHandler#fatalError(org.xml.sax.SAXParseException)
		 */
		@Override
		public void fatalError(final SAXParseException e) throws SAXException {
			/* Correct "apid" reference in log message */
			throw new SAXException(
					"Fatal parse error in product EMD file line "
							+ e.getLineNumber() + " col " + e.getColumnNumber()
							+ ": " + e.getMessage());
		}

		/**
		 * {@inheritDoc}
		 * @see org.xml.sax.helpers.DefaultHandler#warning(org.xml.sax.SAXParseException)
		 */
		@Override
		public void warning(final SAXParseException e) {
			/*  Correct "apid" reference in log message */
			parseLogger.warn("Parse warning in product EMD file line "
							+ e.getLineNumber() + " col " + e.getColumnNumber()
							+ ": " + e.getMessage());
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void startElement(final String uri, final String localName, final String qname,
				final Attributes attr) throws SAXException {
			
			text = new StringBuffer();
			if (localName.equalsIgnoreCase("PartList")) {
				try {
					setTotalParts(XmlUtility.getIntFromAttr(attr, "TotalExpected"));
				} catch (final NumberFormatException e) {
					throw new SAXException(
							"total number of parts must be an integer in product EMD file");
				}
			} else if (localName.equalsIgnoreCase("Part")) {
				try {
					this.part = partFactory.createPartUpdater();
			        this.md = partFactory.convertToMetadataUpdater(part.getMetadata());
			       
					this.inPart = true;
					part.setPartNumber(XmlUtility
							.getIntFromAttr(attr, "Number"));
					part.setPartOffset(XmlUtility
							.getIntFromAttr(attr, "Offset"));
					part.setPartLength(XmlUtility
							.getIntFromAttr(attr, "Length"));
				} catch (final NumberFormatException e) {
					throw new SAXException(
							"Part number, length, and offset attributes must all be integers in product EMD file");
				} catch (final ProductException e) {
					throw new SAXException("Failed to create product part through the part factory", e);
				}
				partFactory.convertToMetadataUpdater(part.getMetadata()).setScid(getScid());
			} else if ( localName.equalsIgnoreCase("SessionId")) {
				this.inSessionId = true;
            } else if ( localName.equalsIgnoreCase("Venue")) {
                this.inVenue = true;
            }
		}

		/**
		 * {@inheritDoc}
		 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
		 */
		@Override
		public void endElement(final String uri, final String localname, final String qname)
				throws SAXException {
			if (localname.equalsIgnoreCase("GroundCreationTime")) {
				final DateFormat formatter = TimeUtility.getFormatterFromPool();

				try {
                    setProductCreationTime(new AccurateDateTime(formatter.parse(text.toString().trim())));
				} catch (final ParseException e) {
                    parseLogger
					.error("Error parsing Ground Creation Time " +  text.toString().trim() + " from product EMD file for product "
							+ getFilenameWithPrefix());

				}
				finally
				{
					TimeUtility.releaseFormatterToPool(formatter);
				}
			} else if (localname.equalsIgnoreCase("Scid")) {
				setScid(XmlUtility.getIntFromText(text.toString().trim()));
			} else if (localname.equalsIgnoreCase("Apid")) {
				setApid(XmlUtility.getIntFromText(text.toString().trim()));
			} else if (localname.equalsIgnoreCase("ProductType")) {
				setProductType(text.toString().trim());
			} else if (localname.equalsIgnoreCase("Vcid")) {
				setVcid(XmlUtility.getIntFromText(text.toString().trim()));
			} else if (localname.equalsIgnoreCase("GroundStatus")) {
				setGroundStatus(ProductStatusType.valueOf(text.toString().trim()));
			} else if (localname.equalsIgnoreCase("DataFilePath")) {
				setFullPath(text.toString().trim());
			} else if (localname.equalsIgnoreCase("SequenceId")) {
				setSequenceId(XmlUtility.getIntFromText(text.toString().trim()));
			} else if (localname.equalsIgnoreCase("SequenceVersion")) {
				setSequenceVersion(XmlUtility.getIntFromText(text.toString()
						.trim()));
			} else if (localname.equalsIgnoreCase("CommandNumber")) {
				setCommandNumber(XmlUtility.getIntFromText(text.toString()
						.trim()));
			} else if (localname.equalsIgnoreCase("DvtCoarse")) {
				setDvtCoarse(XmlUtility.getLongFromText(text.toString().trim()));
			} else if (localname.equalsIgnoreCase("DvtFine")) {
				setDvtFine(XmlUtility.getIntFromText(text.toString().trim()));
			} else if (localname.equalsIgnoreCase("FirstPartSclk")) {
				try {
					// For now, the "FirstPartSclk" element
					// holds the DVT in multimission product metadata files.
					sclk = dvtSclkFmt.valueOf(text.toString().trim()); // DVT may have more bits in DvtFine than SCLK
				} catch (final NumberFormatException nex) {
					nex.printStackTrace();
                    parseLogger
					.error("Error parsing SCLK from product EMD file for product "
							+ getFilenameWithPrefix());
				}
			} else if (localname.equalsIgnoreCase("FirstPartScet")) {
				try {
					setScet(new AccurateDateTime(text.toString().trim()));
				} catch (final java.text.ParseException pex) {
					pex.printStackTrace();
                    parseLogger
					.error("Error parsing SCET from product EMD file for product "
							+ getFilenameWithPrefix());
				}
			} else if (useSolTime && localname.equalsIgnoreCase("FirstPartLst")) {
				try {
					setSol(LocalSolarTimeFactory.getNewLst(text.toString().trim(), getScid()));
				} catch (final java.text.ParseException pex) {
					pex.printStackTrace();
                    parseLogger
					.error("Error parsing LST from product EMD file for product "
							+ getFilenameWithPrefix());
				} 
			} else if (localname.equalsIgnoreCase("FirstPartErt")) {
				try {
					setErt(new AccurateDateTime(text.toString().trim()));
				} catch (final java.text.ParseException e) {
					e.printStackTrace();
                    parseLogger
					.error("Error parsing ERT from product EMD file for product "
							+ getFilenameWithPrefix());
				}
			} else if (localname.equalsIgnoreCase("TotalDataParts")) {
				setTotalParts(XmlUtility.getIntFromText(text.toString().trim()));
			} else if (localname.equalsIgnoreCase("ActualProductChecksum")) {
				setActualChecksum(XmlUtility.getLongFromText(text.toString().trim()));
			} else if (localname.equalsIgnoreCase("ExpectedProductChecksum")) {
				setChecksum(XmlUtility.getLongFromText(text.toString().trim()));
			} else if (localname.equalsIgnoreCase("ActualProductFileSize")) {
				try {
					setActualFileSize(XmlUtility.getLongFromText(text.toString().trim()));
				} catch (final NumberFormatException e) {
                    parseLogger
					.error("Error parsing Actual Product File Size from product EMD file for product "
							+ getFilenameWithPrefix());
				}
			} else if (localname.equalsIgnoreCase("ExpectedProductFileSize")) {
				try {
					setFileSize(XmlUtility.getLongFromText(text.toString().trim()));
				} catch (final NumberFormatException e) {
                    parseLogger
					.error("Error parsing Expected Product File Size from product EMD file for product "
							+ getFilenameWithPrefix());
				}
			} else if (localname.equalsIgnoreCase("CfdpTransactionSequenceNumber")) {
				try {
					setCfdpTransactionId(Long.parseUnsignedLong(text.toString().trim()));
				} catch (final NumberFormatException e) {
                    parseLogger
					.error("Error parsing CFDP Transaction Sequence Number from product EMD file for product "
							+ getFilenameWithPrefix());
				}
			} else if (localname.equalsIgnoreCase("Scet") && this.inPart) {
				try {
					md.setScet(new AccurateDateTime(text.toString().trim()));
				} catch (final java.text.ParseException pex) {
					pex.printStackTrace();
                    parseLogger
					.error("Error parsing SCET from product EMD file for product "
							+ getFilenameWithPrefix());
				} 
			} else if (useSolTime && localname.equalsIgnoreCase("Lst") && this.inPart) {
				try {
					md.setSol(LocalSolarTimeFactory.getNewLst(text.toString().trim()));
				} catch (final java.text.ParseException pex) {
					pex.printStackTrace();
                    parseLogger
					.error("Error parsing LST from product EMD file for product "
							+ getFilenameWithPrefix());
				} 
				md.setSol(sol);
			} else if (localname.equalsIgnoreCase("Sclk") && this.inPart) {
                ISclk psclk = new Sclk();
				try {
					psclk = sclkFmt.valueOf(text.toString().trim());
				} catch (final NumberFormatException nex) {
					nex.printStackTrace();
                    parseLogger
					.error("Error parsing SCLK from product EMD file for product "
							+ getFilenameWithPrefix());
				}
				md.setSclk(psclk);
			} else if (localname.equalsIgnoreCase("Ert") && this.inPart) {
				try {
					md.setErt(
							new AccurateDateTime(text.toString().trim()));
				} catch (final java.text.ParseException pex) {
					pex.printStackTrace();
                    parseLogger
					.error("Error parsing ERT from product EMD file for product "
							+ getFilenameWithPrefix());
				} 
			} else if (localname.equalsIgnoreCase("SourcePacketSeqCount") && this.inPart) {
				try {
					part.setPacketSequenceNumber(XmlUtility.getIntFromText(text
							.toString().trim()));
				} catch (final NumberFormatException e) {
					throw new SAXException(
							"source_packet_seq_count in product EMD file must be an integer");
				}
			} else if (localname.equalsIgnoreCase("Part")) {
				parts.add(this.part);
				this.inPart = false;
			} else if (localname.equalsIgnoreCase("Number") && this.inSessionId ) {
				setSessionId(XmlUtility.getLongFromText(text.toString().trim()));
			} else if (localname.equalsIgnoreCase("SessionId")) {
				this.inSessionId = false;
            } else if (localname.equalsIgnoreCase("FswDictionaryDir") && this.inSessionId) {
                setEmdDictionaryDir(text.toString().trim());
            } else if (localname.equalsIgnoreCase("FswDictionaryVersion")  && this.inSessionId) {
                setEmdDictionaryVersion(text.toString().trim());
            } else if (localname.equalsIgnoreCase("Host")  && this.inVenue) {
                setSessionHost(text.toString().trim());
            } else if (localname.equalsIgnoreCase("Venue")) {
                this.inVenue = false;
			} else {
				parseFromElement(localname, text.toString().trim());
			}
		}
	}

	@Override
	public Map<String,String> getFileData(final String NO_DATA) {
		final Map<String, String> map = super.getFileData(NO_DATA);

		final String dataSt = "DVT=" + map.get("dvt") + " TOTAL=" + totalParts
				+ " SIZE=" + fileSize + " " + map.get("ground_status");
		map.put("data", dataSt); // For excel format
		map.put("data_sr_csv", "\"" + dataSt + "\""); // For session report csv
		// format

		return map;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public int getSourceEntityId() {
		return sourceEntityId;
	}

    /**
     * {@inheritDoc}
     */
    @Override
	public void setSourceEntityId(final int sourceEntityId) {
		this.sourceEntityId = sourceEntityId;
	}

    /**
     * constructs the data file name for the data associated with this metadata
     * instance and saves it in the class variable dataFilename
     *
     * @param productIsPartial
     *			true if the product is not yet complete, and false if the
     *			product is complete
     *
     */
    //@Override
    @Override
    public void setDataFileName(final Boolean productIsPartial) {

        if (productIsPartial) {

            dataFilename = getCurrentPartialDataFile();
        } else {
            dataFilename = getCompleteDataFile();
        }
        return;
    }

    /**
     * returns the name of the data file associated with this metadata instance
     *
     * @return data file name
     */
    //@Override
    @Override
    public String getDataFileName() {
        return dataFilename;
    }

    //@Override
    @Override
    public String getCurrentPartialDataFile() {
        File destFile = null;
        final boolean found = false;
        int counter = 1;
        String outputDirectory = appContext.getBean(IGeneralContextInformation.class)
                .getOutputDir();
        outputDirectory += File.separator;
        outputDirectory += "products";
        final String rootFilename = getFilenameWithPrefix() + "_Partial-";
        String previousPartialFileName = rootFilename + counter
                + ProductStorageConstants.PARTIAL_DATA_SUFFIX;
        String filename = "";
        while (!found) {

            filename = rootFilename + counter
                    + ProductStorageConstants.PARTIAL_DATA_SUFFIX;
            destFile = new File(outputDirectory, filename);
            if (!destFile.exists()) {
                break;
            } else {
                previousPartialFileName = filename;
            }
            ++counter;
        }
        return previousPartialFileName;
    }

    /**
     * Constructs the file name for a complete data product with the top-level
     * directory
     *
     * @return the name for the complete file path to the data file
     */
    //@Override
    @Override
    public String getCompleteDataFile() {
        File destFile = null;
        final boolean found = false;
        int counter = 1;
        String outputDirectory = appContext.getBean(IGeneralContextInformation.class)
                .getOutputDir();
        outputDirectory += File.separator;
        outputDirectory += "products";
        final String rootFilename = getFilenameWithPrefix() + "-";
        String previousCompleteFileName = rootFilename + counter
                + ProductStorageConstants.DATA_SUFFIX;
        String filename = "";
        while (!found) {

            filename = rootFilename + counter
                    + ProductStorageConstants.PARTIAL_DATA_SUFFIX;
            destFile = new File(outputDirectory, filename);
            if (!destFile.exists()) {
                break;
            } else {
                previousCompleteFileName = filename;
            }
            ++counter;
        }
        return previousCompleteFileName;
    }

}
