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
package jpl.gds.product.api.builder;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.SortedSet;

import jpl.gds.shared.time.IAccurateDateTime;

/**
 * An interface to be implemented by product builder receives parts tracker classes.
 * 
 *
 * @since R8
 */
public interface IReceivedPartsTracker {

    /** Default version for the tracker file */
    public static final int DEFAULT_VERSION = 1; // the original tracker data file had no version designation.
    // Mupdated the version.  Version 3 contains flags for EPDU and MPDU receipt.
    /** Version for the tracker file with EPU/MPDU receipt flags */
    public static final int MPDU_EPDU_VERSION = 3; // current tracker version.
    /** Current version for the tracker file */
    public static final int CURRENT_VERSION = MPDU_EPDU_VERSION; // current tracker version.
    /** Tracker file name */
    public static final String RECEIVED_PARTS_TRACKER_PERSISTENCE_FILE_NAME = "ReceivedPartsTracker.dat";
    /** Used when constructing tracker log file paths */
    public static final String TEST_LOG_DIR_BASE = "_ReceivedPartsTracker_Temp_";
    /** String value of "was received flag" in the tracker */
    public static final String WAS_RECEIVED = "1";
    /** String value of "was not received flag" in the tracker */
    public static final String WAS_NOT_RECEIVED = "0";

    /**
     * Resets the internal state, which allows the singleton object to be reused from a fresh, clean state.
     */
    public void clear();

    /**
     * Constructs the VCID/TransactionID string from individual VCID and Transaction ID values.
     * 
     * @param vcid
     *            virtual channel ID
     * @param transactionId
     *            product transaction ID
     * @return string that combines <code>vcid</code> and <code>transactionId</code>, separated by a forward slash
     */
    public String getKey(int vcid, String transactionId);

    /**
     * Adds an MPDU to a product.  
     * @param vcid
     *            virtual channel ID
     * @param transactionId
     *            product transaction ID
     * @param ert 
     * 			  ERT of the MPDU.  Will update the earliest ERT table.
     */
    public void addMpdu(int vcid, String transactionId, IAccurateDateTime ert);

    /**
     * Adds an EPDU to a product.
     * @param vcid
     *            virtual channel ID
     * @param transactionId
     *            product transaction ID
     * @param ert 
     * 			  ERT of the EPDU.  Will update the earliest ERT table.
     */
    public void addEpdu(int vcid, String transactionId, IAccurateDateTime ert);

    /**
     * Adds a part number to the table. Will restore tracking information for specified product from persistent disk storage if it
     * does not already exist in the tracker.
     * 
     * @param vcid
     *            virtual channel ID
     * @param transactionId
     *            product transaction ID
     * @param partNumber
     *            number of the part to add to the table
     * @param ert
     * 			the ERT of this part.
     */
    public void addPart(int vcid, String transactionId, int partNumber, IAccurateDateTime ert);

    /**
     * Updates the ERT table for a product with ERT.  This will only update the table if ERT is not null 
     * and it is earlier than the value in the table.
     * 
     *
     * @param vcid VCID of product to update
     * @param transactionId ID of product to update
     * @param ert new ERT  
     */
    public void updateEarliestERT(int vcid, String transactionId, IAccurateDateTime ert);

    /**
     * Sets the total number of parts for a product in the table.  
     * 
     * NOTE:  This will only set the total parts IF AND ONLY IF totalParts is greater than the current
     * total parts value in the totalPartsTable.  To set the value directly use the setTotalPartsDirect method.
     * 
     * @param vcid
     *            virtual channel ID
     * @param transactionId
     *            product transaction ID
     * @param totalParts
     *            total number of parts to set in the table
     */
    public void setTotalParts(int vcid, String transactionId, int totalParts);

    /**
     * Sets the total parts for vcid and transactionId to totalParts regardless of what the current value is.
     *
     * @param vcid
     *            virtual channel ID
     * @param transactionId
     *            product transaction ID
     * @param totalParts
     *            total number of parts to set in the table
     */
    public void setTotalPartsDirect(int vcid, String transactionId, int totalParts);

    /**
     * Check if the total number of parts expected for a product is known (exists in the table).
     * 
     * @param vcid
     *            virtual channel ID
     * @param transactionId
     *            product transaction ID
     * @return true if total number of parts is known, false otherwise
     */
    public boolean isTotalPartsKnown(int vcid, String transactionId);

    /**
     * Return the earliest ERT kept track of by the ReceivedPartsTracker.
     * 
     * @param vcid
     *            virtual channel ID
     * @param transactionId
     *            product transaction ID
     * @return the earliest ERT kept track of by the ReceivedPartsTracker, or null if no parts have been received.
     */
    public IAccurateDateTime getEarliestERT(int vcid, String transactionId);

    /**
     * Looks up and gets the total number of parts for a product.
     * 
     * @param vcid
     *            virtual channel ID
     * @param transactionId
     *            product transaction ID
     * @return if the total number of parts for a product is known, returns the value; -1 if the number is unknown
     */
    public int getTotalParts(int vcid, String transactionId);

    /**
     * Check if all parts have been received for a product. If total number of parts for the product is not yet known, method will
     * always return false.
     * 
     * @param vcid
     *            virtual channel ID
     * @param transactionId
     *            product transaction ID
     * @return true if all parts have been received for the specified product, false otherwise
     */
    public boolean allPartsReceived(int vcid, String transactionId);

    /**
     * Removes the entry for a product from this tracker. Should be called when the product has been completed.
     * 
     * @param vcid
     *            virtual channel ID
     * @param transactionId
     *            product transaction ID
     */
    public void clearProduct(int vcid, String transactionId);

    /**
     * Fetches the <code>SortedSet</code> of the part numbers that have been received (tracked) for a product.
     * 
     * @param vcid
     *            virtual channel ID
     * @param transactionId
     *            transaction ID
     * @return <code>SortedSet</code> of part numbers tracked for the specified product
     */
    public SortedSet<Integer> getParts(int vcid, String transactionId);

    /**
     * Store contents of this object to a single file (the default parts tracker file), 
     * for the specified data product.
     * 
     * File format is CSV:<b>
     * <ul>
     * <li>The first value is is a String used as the KEY for both Maps</li>
     * <li>The second value is the ERT of the first packet received</li>
     * <li>The third value is 1 or 0 indicating if the product MPDU was received.</li>
     * <li>The fourth value is 1 or 0 indicating if the product EPDU was received.</li>
     * <li>The fifth value is the total number of parts for that key</li>
     * <li>The remaining values are the part numbers already contained in the tracker</li>
     * </ul>
     * Blank lines are ignored
     * 
     * <pre>
     * Example:
     * 	 KEY,ERT,MPDU,EPDU,total,list...
     * 	 KEY,ERT,MPDU,EPDU,total,list...
     * 	 KEY,ERT,MPDU,EPDU,total,list...
     * 	 KEY,ERT,MPDU,EPDU,total,list...
     * 	 KEY,ERT,MPDU,EPDU,total,list...
     * 
     * MDPU and EPDU are either 1 for true, 0 for false.  These just indicate if they have been received.
     * </pre>
     * @param vcid VCID of the data product
     * @param transactionId ID of the data product
     */
    public void store(int vcid, String transactionId);

    /**
     * Store contents of this object to a single file, for the specified product,
     * by writing it to the supplied PrintWriter.
     * 
     * File format is CSV:<b>
     * <ul>
     * <li>The first value is is a String used as the KEY for both Maps</li>
     * <li>The second value is the ERT of the first packet received</li>
     * <li>The third value is 1 or 0 indicating if the product MPDU was received.</li>
     * <li>The fourth value is 1 or 0 indicating if the product EPDU was received.</li>
     * <li>The fifth value is the total number of parts for that key</li>
     * <li>The remaining values are the part numbers already contained in the tracker</li>
     * </ul>
     * Blank lines are ignored
     * 
     * <pre>
     * Example:
     * 	 KEY,ERT,MPDU,EPDU,total,list...
     * 	 KEY,ERT,MPDU,EPDU,total,list...
     * 	 KEY,ERT,MPDU,EPDU,total,list...
     * 	 KEY,ERT,MPDU,EPDU,total,list...
     * 	 KEY,ERT,MPDU,EPDU,total,list...
     * 
     * MDPU and EPDU are either 1 for true, 0 for false.  These just indicate if they have been received.
     * Stores the received parts data to trackerDataFile.
     * @param vcid VCID of the data product
     * @param transactionId ID of the data product
     * @param wrtr The print writer.  This will not close the writer once it is finished.
     */
    public void store(int vcid, String transactionId, PrintWriter wrtr);

    /**
     * Store contents of the entire object (for all products) to a single file 
     * (the default tracker file).
     * 
     * File format is CSV:<b>
     * <ul>
     * <li>The first value is is a String used as the KEY for both Maps</li>
     * <li>The second value is the ERT of the first packet received</li>
     * <li>The third value is 1 or 0 indicating if the product MPDU was received.</li>
     * <li>The fourth value is 1 or 0 indicating if the product EPDU was received.</li>
     * <li>The fifth value is the total number of parts for that key</li>
     * <li>The remaining values are the part numbers already contained in the tracker</li>
     * </ul>
     * Blank lines are ignored
     * 
     * <pre>
     * Example:
     * 	 KEY,ERT,MPDU,EPDU,total,list...
     * 	 KEY,ERT,MPDU,EPDU,total,list...
     * 	 KEY,ERT,MPDU,EPDU,total,list...
     * 	 KEY,ERT,MPDU,EPDU,total,list...
     * 	 KEY,ERT,MPDU,EPDU,total,list...
     * 
     * MDPU and EPDU are either 1 for true, 0 for false.  These just indicate if they have been received.
     * </pre>
     * 
     * @param file
     *            the output file to write
     * @throws IOException
     *             on error
     */
    public void store(File file) throws IOException;

    /**
     * Store contents of entire object (for all products) to a single Writer.
     * 
     * File format is CSV:<b>
     * <ul>
     * <li>The first value is is a String used as the KEY for both Maps</li>
     * <li>The second value is the ERT of the first packet received</li>
     * <li>The third value is 1 or 0 indicating if the product MPDU was received.</li>
     * <li>The fourth value is 1 or 0 indicating if the product EPDU was received.</li>
     * <li>The fifth value is the total number of parts for that key</li>
     * <li>The remaining values are the part numbers already contained in the tracker</li>
     * </ul>
     * Blank lines are ignored
     * 
     * <pre>
     * Example:
     * 	 KEY,ERT,MPDU,EPDU,total,list...
     * 	 KEY,ERT,MPDU,EPDU,total,list...
     * 	 KEY,ERT,MPDU,EPDU,total,list...
     * 	 KEY,ERT,MPDU,EPDU,total,list...
     * 	 KEY,ERT,MPDU,EPDU,total,list...
     * 
     * MDPU and EPDU are either 1 for true, 0 for false.  These just indicate if they have been received.
     * </pre>
     * 
     * @param out
     *            the Writer to write to
     * @throws IOException
     *             on error
     */
    public void store(Writer out) throws IOException;

    /**
     * Loads the tracker information for the specified data product.
     * 
     * @param vcid the product virtual channel ID
     * @param transactionId the product ID
     */
    public void load(int vcid, String transactionId);

    /**
     * Load contents of entire object from a single file. File format is CSV:<b>
     * <ul>
     * <li>The first value is is a String used as the KEY for both Maps</li>
     * <li>The second value is the ERT of the first packet received</li>
     * <li>The third value is 1 or 0 indicating if the product MPDU was received.</li>
     * <li>The fourth value is 1 or 0 indicating if the product EPDU was received.</li>
     * <li>The fifth value is the total number of parts for that key</li>
     * <li>The remaining values are the part numbers already contained in the tracker</li>
     * </ul>
     * Blank lines are ignored
     * 
     * <pre>
     * Example:
     * 	 KEY,ERT,MPDU,EPDU,total,list...
     * 	 KEY,ERT,MPDU,EPDU,total,list...
     * 	 KEY,ERT,MPDU,EPDU,total,list...
     * 	 KEY,ERT,MPDU,EPDU,total,list...
     * 	 KEY,ERT,MPDU,EPDU,total,list...
     * 
     * MDPU and EPDU are either 1 for true, 0 for false.  These just indicate if they have been received.
     * </pre>
     * 
     * @param file
     *            the input file to read
     * @throws IOException
     *             on error
     */
    public void load(File file) throws IOException;

    /**
     * Load contents of entire object from a single Reader. File format is CSV:<b>
     * <ul>
     * <li>The first value is is a String used as the KEY for both Maps</li>
     * <li>The second value is the ERT of the first packet received</li>
     * <li>The third value is 1 or 0 indicating if the product MPDU was received.</li>
     * <li>The fourth value is 1 or 0 indicating if the product EPDU was received.</li>
     * <li>The fifth value is the total number of parts for that key</li>
     * <li>The remaining values are the part numbers already contained in the tracker</li>
     * </ul>
     * Blank lines are ignored
     * 
     * <pre>
     * Example:
     * 	 KEY,ERT,MPDU,EPDU,total,list...
     * 	 KEY,ERT,MPDU,EPDU,total,list...
     * 	 KEY,ERT,MPDU,EPDU,total,list...
     * 	 KEY,ERT,MPDU,EPDU,total,list...
     * 	 KEY,ERT,MPDU,EPDU,total,list...
     * 
     * MDPU and EPDU are either 1 for true, 0 for false.  These just indicate if they have been received.
     * </pre>
     * 
     * @param in
     *            the Reader to read from
     * @throws IOException
     *             on error
     */
    public void load(Reader in) throws IOException;

}