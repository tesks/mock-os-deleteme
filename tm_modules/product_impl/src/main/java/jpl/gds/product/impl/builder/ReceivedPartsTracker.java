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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.context.ApplicationContext;

import jpl.gds.product.api.builder.IProductStorage;
import jpl.gds.product.api.builder.IReceivedPartsTracker;
import jpl.gds.product.api.builder.ProductStorageConstants;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;

/**
 * <code>ReceivedPartsTracker</code> is an internal caching object for the purpose of keeping track of which product parts have been
 * received and how many we are expecting, per VCID and Transaction ID combination. This allows MPCS to push out complete products
 * even when the parts come in out of order.
 * 
 *
 * 11/01/2011 ReceivedPartsTracker now also tracks earliest ERT for a product from all of its parts.
 * 
 * 6/30/2016 - Tracking if a product has received MPDU and EPDU.  Also update the load and
 * store to include entries if any information was added for a given transaction.  Would skip it if there were no parts added
 * but it now is needed to tell if the M or E PDUs were received.
 */
public class ReceivedPartsTracker implements IReceivedPartsTracker {
    private final Tracer               log;

	private static final String			VERSION_HEADER									= ReceivedPartsTracker.class.getSimpleName() + " VERSION: ";

	/** The current application context */
	protected final ApplicationContext appContext;

	/**
	 * Constructor
	 * @param appContext the current application context
	 */
	public ReceivedPartsTracker(final ApplicationContext appContext) {
		this.appContext = appContext;
        this.log = TraceManager.getTracer(appContext, Loggers.TLM_PRODUCT);
	}


	// member variables of interest below
	private final Map<String, SortedSet<Integer>>	receivedPartsTable	= new HashMap<String, SortedSet<Integer>>();
	private final Map<String, Integer>				totalPartsTable		= new HashMap<String, Integer>();
	private final Map<String, IAccurateDateTime>		ertTable			= new HashMap<String, IAccurateDateTime>();
	private final HashSet<String>				    mpduReceivedTable	= new HashSet<String>();
	private final HashSet<String>				    epduReceivedTable	= new HashSet<String>();

	private final StringBuilder						sb					= new StringBuilder(1024);
	
	/*
	 * Keep track of version number of READ file.
	 * (Always write current version).
	 */
	private int trackerReaderVersion = 0;

	/**
     * {@inheritDoc}
     */
	@Override
    public void clear() {
		receivedPartsTable.clear();
		totalPartsTable.clear();
		ertTable.clear();
		sb.setLength(0);
		
		/**
		 * Clear the new tables.
		 */
		mpduReceivedTable.clear();
		epduReceivedTable.clear();
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public String getKey(final int vcid, final String transactionId) {
		sb.setLength(0);
		sb.append(vcid);
		sb.append("/");
		sb.append(transactionId);
		return sb.toString();
	}

	/**
	 * Adding ways to indicate if the EPDU or MPDU were received.  This
	 * will simplify the way we check if products are complete as well as detecting embedded EPDUs.
	 */
	
	private String buildDebugMessage(final String message, final Integer vcid, final String transactionId, final Integer partNumber) {
		final StringBuilder builder = new StringBuilder(message);
		
		if (partNumber != null) {
			builder.append(" partNumber=")
			.append(partNumber);
		}

		if (vcid != null) {
			builder.append(" VCID=")
			.append(vcid);
		}

		if (transactionId != null) {
			builder.append(" transactionId=")
			.append(transactionId);
		}

		return builder.toString();
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void addMpdu(final int vcid, final String transactionId, final IAccurateDateTime ert) {
		final String key = getKey(vcid, transactionId);

		// Just add it, if it already exists will just replace the old one.
		mpduReceivedTable.add(key);
		updateEarliestERT(vcid, transactionId, ert);
		
		log.debug(buildDebugMessage("Adding MPDU", vcid, transactionId, null));
	}
	
	/**
     * {@inheritDoc}
     */
	@Override
    public void addEpdu(final int vcid, final String transactionId, final IAccurateDateTime ert) {
		final String key = getKey(vcid, transactionId);
		
		// Just add it, if it already exists will just replace the old one.
		epduReceivedTable.add(key);
		
		updateEarliestERT(vcid, transactionId, ert);
		log.debug(buildDebugMessage("Adding EPDU", vcid, transactionId, null));
	}
	
	/**
     * {@inheritDoc}
     */
	@Override
    public void addPart(final int vcid, final String transactionId, final int partNumber, final IAccurateDateTime ert) {
		final String key = getKey(vcid, transactionId);
		SortedSet<Integer> partNumbers = null;

		/*
		 *
		 * Used to be that the tracker was loaded only upon receipt of the 
		 * first part, but caching does not work when multiple processes are
		 * operating in concert. It must be loaded from the file system
		 * every time.
		 */
		load(vcid, transactionId);

		/*
		 * Fixed bug that neglected to set the partNumbers variable after loading it.
		 */
		if (receivedPartsTable.containsKey(key)) {
			partNumbers = receivedPartsTable.get(key);
		}


		if (null == partNumbers) {
			/*
			 * No persistence found. Create a new, empty entry in table. Add a new part numbers SortedSet object to the table
			 */
			partNumbers = new TreeSet<Integer>();
			receivedPartsTable.put(key, partNumbers);
		}
		partNumbers.add(partNumber);

		
		updateEarliestERT(vcid, transactionId, ert);
		
		log.debug(buildDebugMessage("Adding Part", vcid, transactionId, partNumber));
	}
	
	/**
     * {@inheritDoc}
     */
	@Override
    public void updateEarliestERT(final int vcid, final String transactionId, final IAccurateDateTime ert) {
		final String key = getKey(vcid, transactionId);
		/*
		 * Keep track of earliest ERT for product.
		 */
		if (null != ert) {
			if (ertTable.containsKey(key)) {
				final IAccurateDateTime earliestTime = ertTable.get(key);
				if (ert.before(earliestTime)) {
					ertTable.put(key,ert);
				}
			} else {
				ertTable.put(key, ert);
			}
		}	
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setTotalParts(final int vcid, final String transactionId, final int totalParts) {
		final String key = getKey(vcid, transactionId);

		if (totalPartsTable.get(key) == null) {
			totalPartsTable.put(key, totalParts);
		} else {
			final int total = totalPartsTable.get(key);
			if (totalParts > total) {
				totalPartsTable.put(key, totalParts);
			}
		}
	}
	
	/**
     * {@inheritDoc}
     */
	@Override
    public void setTotalPartsDirect(final int vcid, final String transactionId, final int totalParts) {
		final String key = getKey(vcid, transactionId);
		totalPartsTable.put(key, totalParts);
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public boolean isTotalPartsKnown(final int vcid, final String transactionId) {
		return totalPartsTable.get(getKey(vcid, transactionId)) != null ? true : false;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public IAccurateDateTime getEarliestERT(final int vcid, final String transactionId) {
		/*
		 * Do not use Current Time for ERT if there have been no parts received.
		 */
		final SortedSet<Integer> parts = getParts(vcid, transactionId);
		IAccurateDateTime ert = ertTable.get(getKey(vcid, transactionId));
		if ((null == ert) && ((null != parts) && (parts.size() > 0))) {
			ert = new AccurateDateTime();
		}
		return ert;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public int getTotalParts(final int vcid, final String transactionId) {

		if (!isTotalPartsKnown(vcid, transactionId)) {
			return -1;
		}

		return totalPartsTable.get(getKey(vcid, transactionId)).intValue();
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public boolean allPartsReceived(final int vcid, final String transactionId) {
		final String key = getKey(vcid, transactionId);

		if (!mpduReceivedTable.contains(key) || !epduReceivedTable.contains(key)) {
			return false;
		}

		if (!isTotalPartsKnown(vcid, transactionId)) {
			return false;
		} 

		/**
		 * Must handle the case where we only have MPDU and EPDU for a
		 * complete product with no data parts.
		 */
		final int totalParts = totalPartsTable.get(key).intValue();
		
		if (totalParts == 0) {
			return true; 
		} else if (!receivedPartsTable.containsKey(key) || receivedPartsTable.get(key) == null) {
			return false;
		} else {

			final SortedSet<Integer> partNumbers = receivedPartsTable.get(key);

			/**
			 * All data parts will be stored only, we have different collections to store
			 * if the MPDU or EPDU were tracked.  In the case of an embedded EPDU, that will be treated just like a 
			 * normal data part here.  It is important that the maintainer of the parts tracker handle the case where 
			 * the total parts include EPDUs.  
			 */
			return partNumbers.size() == totalParts;
		}
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void clearProduct(final int vcid, final String transactionId) {
		final String key = getKey(vcid, transactionId);
		totalPartsTable.remove(key);
		receivedPartsTable.remove(key);
		ertTable.remove(key);
		
		/**
		 * Clear the new tables.
		 */
		mpduReceivedTable.remove(key);
		epduReceivedTable.remove(key);
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public SortedSet<Integer> getParts(final int vcid, final String transactionId) {
		return receivedPartsTable.get(getKey(vcid, transactionId));
	}

	/*
	 * PERSISTENCE PERSISTENCE PERSISTENCE PERSISTENCE PERSISTENCE PERSISTENCE PERSISTENCE PERSISTENCE PERSISTENCE
	 */
	private File getTrackerDataFile(final int vcid, final String transactionId) {
		return getTrackerDataFile(vcid, transactionId, false);
	}

	private File getTrackerDataFile(final int vcid, final String transactionId, final boolean create) {
		File logDir = null;
		try {
			final IProductStorage dps = appContext.getBean(IProductStorage.class, vcid);
			logDir = new File(dps.getActiveProductDirectory(), transactionId);
		}
		catch (final NullPointerException e) {
			logDir = new File(TEST_LOG_DIR_BASE + File.separator + ProductStorageConstants.ACTIVE_DIR + "-" + vcid + File.separator
					+ transactionId);
		}
		if (create && !logDir.exists()) {
			logDir.mkdirs();
		}
		return new File(logDir, RECEIVED_PARTS_TRACKER_PERSISTENCE_FILE_NAME);
	}
	
	/**
     * {@inheritDoc}
     */
	@Override
    public void store(final int vcid, final String transactionId) {
		final File trackerDataFile = getTrackerDataFile(vcid, transactionId, true);
		trackerDataFile.delete();
		
		try (PrintWriter wrtr = new PrintWriter(new FileWriter(trackerDataFile))) {
			store(vcid, transactionId, wrtr);
		} catch (final IOException e) {
			// ignore -- succeeded already false
		}
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void store(final int vcid, final String transactionId, final PrintWriter wrtr) {
		final String key = getKey(vcid, transactionId);

		/**
		 * With the new version we must write a line if only the
		 * epdu and / or mpdu are received with no data.  
		 */
		final SortedSet<Integer> partList = receivedPartsTable.get(key);
		wrtr.printf("%s%d\n", VERSION_HEADER, CURRENT_VERSION);
		final Integer totalParts = totalPartsTable.containsKey(key) ? totalPartsTable.get(key) : -1;
		final IAccurateDateTime ert = ertTable.containsKey(key) ? ertTable.get(key) : new AccurateDateTime();

		/**
		 * include the mpdu and epdu flags.
		 */
		wrtr.printf("%s,%s,%s,%s,%s", 
				key, // key
				ert.getFormattedErt(false), // ERT
				mpduReceivedTable.contains(key) ? WAS_RECEIVED : WAS_NOT_RECEIVED,
				epduReceivedTable.contains(key) ? WAS_RECEIVED : WAS_NOT_RECEIVED,
				totalParts.toString()); // Total parts

		if (partList != null) {
			for (final Integer i : partList) {
				wrtr.printf(",%s", i.toString());
			}
		}

		wrtr.println();
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void store(final File file) throws IOException {
		store(new FileWriter(file));
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void store(final Writer out) throws IOException {
		PrintWriter wrtr = null;
		try {
			wrtr = new PrintWriter(out);
			wrtr.printf("%s%d\n", VERSION_HEADER, CURRENT_VERSION);
			/**
			 * Handle cases where no data was added but we did get other info
			 * like ERT, total parts or E/M pdu.  
			 */
			final TreeSet<String> allKeys = new TreeSet<String>(receivedPartsTable.keySet());
			
			// Add the keys from the EPDU and MPDU tables and total parts table.  Must make 
			// sure that any transaction that has info in this gets saved.
			allKeys.addAll(mpduReceivedTable);
			allKeys.addAll(epduReceivedTable);
			allKeys.addAll(totalPartsTable.keySet());
			allKeys.addAll(ertTable.keySet());

			for (final String key : allKeys) {
				final SortedSet<Integer> partList = receivedPartsTable.get(key);
				final Integer totalParts = totalPartsTable.get(key);
				final IAccurateDateTime ert = ertTable.containsKey(key) ? ertTable.get(key) : new AccurateDateTime();
				/**
				 * include the mpdu and epdu flags.
				 */
				wrtr.printf("%s,%s,%s,%s,%s", 
						key, // key
						ert.getFormattedErt(false), // ERT
						mpduReceivedTable.contains(key) ? WAS_RECEIVED : WAS_NOT_RECEIVED,
						epduReceivedTable.contains(key) ? WAS_RECEIVED : WAS_NOT_RECEIVED,
						((null != totalParts) ? totalParts.toString() : "-1")); // TOTAL PARTS
				
				if (partList != null) {
					for (final Integer i : partList) {
						wrtr.printf(",%s", i.toString());
					}
				}

				wrtr.println();
			}
		}
		finally {
			if (null != wrtr) {
				try {
					wrtr.flush();
					wrtr.close();
				}
				catch (final Exception e) {
					// ignore
				}
			}
		}
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void load(final int vcid, final String transactionId) {
		final File trackerDataFile = getTrackerDataFile(vcid, transactionId);
		this.clearProduct(vcid, transactionId);
		try {
			load(trackerDataFile);
		}
		catch (final IOException e) {
			// ignore -- succeeded already false
		}
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void load(final File file) throws IOException {
		load(new FileReader(file));
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void load(final Reader in) throws IOException {
        final LineNumberReader rdr = new LineNumberReader(in);
		try {
			boolean firstLine = true;
			String line;
			while (null != (line = rdr.readLine())) {
				if (line.trim().length() == 0) continue; // skip blank lines
				
				/*
				 * Check for version designation at top of file.
				 * NOTE: Version 1 has no version designation (DEFAULT_VERSION).
				 */
				if (firstLine) {
					firstLine = false;
					trackerReaderVersion = DEFAULT_VERSION;
					if (line.startsWith(VERSION_HEADER)) {
						trackerReaderVersion = Integer.parseInt(line.substring(VERSION_HEADER.length()).trim());
						continue;
					}
				}

				int i = 0;
				try {
					final String[] vals = line.split(",");
					final String key = vals[i++].trim();
					
					/*
					 * Only load ERT if not the DEFAULT_VERSION of the file.
					 * (The original (DEFAULT_VERSION) of the file did not contain
					 * an ERT field).
					 */
					if (trackerReaderVersion > DEFAULT_VERSION) {
						IAccurateDateTime ert;
						try {
							ert = new AccurateDateTime(vals[i++].trim());
						}
						catch (final ParseException e1) {
							ert = new AccurateDateTime();
						}
						ertTable.put(key, ert);
					}
					
					/**
					 *  Check if the EPDU and MPDU flags are included.
					 */
					if (trackerReaderVersion >= MPDU_EPDU_VERSION) {
						if (WAS_RECEIVED.equals(vals[i++])) {
							mpduReceivedTable.add(key);
						}
						
						if (WAS_RECEIVED.equals(vals[i++])) {
							epduReceivedTable.add(key);
						}
					}
					
					final String totalPartsString = vals[i++].trim();
					final SortedSet<Integer> partList = new TreeSet<Integer>();
					try {
						final int totalParts = Integer.valueOf(totalPartsString);
						totalPartsTable.put(key, totalParts);
					}
					catch (final Exception e) {
						// ignore -- totalParts is not specified
					}
					while (i < vals.length) {
						try {
							partList.add(Integer.valueOf(vals[i].trim()));
						}
						catch (final NumberFormatException e) {
							throw new IOException("Format error while loading " + getClass().getSimpleName() + " on line #"
									+ rdr.getLineNumber() + ": \"" + line + "\": " + e);
						}
						i++;
					}
					receivedPartsTable.put(key, partList);
				}
				catch (final ArrayIndexOutOfBoundsException e) {
					final int lineno = rdr.getLineNumber();
					rdr.close();
					throw new IOException("Format error while loading " + getClass().getSimpleName() + " on line #"
							+ lineno + ": \"" + line + "\": " + e);
				}
			}
		}
		finally {
			if (null != in) {
				try {
				    rdr.close();
				}
				catch (final Exception e) {
					// ignore
				}
                try {
                    in.close();
                }
                catch (final Exception e) {
                    // ignore
                }
			}
		}
	}
}