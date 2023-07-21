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
package jpl.gds.globallad.data.factory;

import java.util.Collection;
import java.util.Comparator;

import com.google.protobuf.InvalidProtocolBufferException;

import jpl.gds.globallad.data.GlobalLadDataException;
import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.serialization.globallad.data.Proto3GlobalLadTransport;


/**
 * Factory responsible for creating global lad objects from byte arrays or ByteBuffers.
 *
 * Updated the enums so that they can be created by Jersey directly from
 * the query params.
 */
public interface IGlobalLadDataFactory {
	public enum QueryType  {
        /** EVR Query Type enum */
		evr("evr"),
        /** EHA Query Type enum */
		eha("eha"),
        /** Alarm Query Type enum */
		alarm("alarm");

		private final String value;

		/**
		 * @param value
		 */
		private QueryType(final String value) {
			this.value = value;
		}

        /**
         * @return QueryType value
         */
		public String getValue() {
			return value;
		}
	}

	public enum RecordedState {
        /** realtime RecordedState enum */
		realtime("realtime"),
        /** recorded RecordedState enum */
		recorded("recorded"),
        /** BOTH RecordedState enum */
		both("both");

		private final String value;

		/**
		 * @param value
		 */
		private RecordedState(final String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}

	public enum DataSource {
		fsw("fsw"),
		header("header"),
		sse("sse"),
		monitor("monitor"),
		all("all");

		private final String value;

		/**
		 * @param value
		 */
		private DataSource(final String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}

	/**
	 * Comparators were backward and the ring buffers were compensating.  Now everything is
	 * in alignment and these comparators will sort the data in descending order based on a specific time type.
	 */

	/**
	 * Comparator to be used when sorting based on event time.  Compares in such a way to sort in descending order.
	 */
	public static final Comparator<IGlobalLADData> eventComparator = new Comparator<IGlobalLADData>() {

		@Override
		public int compare(final IGlobalLADData c1, final IGlobalLADData c2) {
			return c2.compareInsert(c1);
		}
	};

	/**
	 * Comparator to be used when sorting based on event time.  Compares in such a way to sort in descending order.
	 */
	public static final Comparator<IGlobalLADData> scetComparator = new Comparator<IGlobalLADData>() {

		@Override
		public int compare(final IGlobalLADData c1, final IGlobalLADData c2) {
			return c2.compareScet(c1);
		}
	};

	/**
	 * Comparator to be used when sorting based on event time.  Compares in such a way to sort in descending order.
	 */
	public static final Comparator<IGlobalLADData> ertComparator = new Comparator<IGlobalLADData>() {

		@Override
		public int compare(final IGlobalLADData c1, final IGlobalLADData c2) {
			return c2.compareErt(c1);
		}
	};

	/**
	 * Comparator to be used when sorting based on all time type.
	 * Compares using the primary time type in such a way as to be in descending order.
	 */
	public static final Comparator<IGlobalLADData> allComparator = new Comparator<IGlobalLADData>() {

		@Override
		public int compare(final IGlobalLADData c1, final IGlobalLADData c2) {
			return c2.compareTo(c1);
		}
	};

	/**
	 * Special comparator to be used when the time type is all. This only uses the insert number for comparison.  This needed to
	 * be much more strict on doing the comparisons because we expect all different types be merged together using this comparator.
	 *
	 * We are flattening all of the data maps when using csv and will suffer from the same issues as 8214.
	 * Making flatten comparators for all of the time types.
	 *
	 * More updates. Taking out the insert number check because each of the compare methods already do that.
	 */
	public static final Comparator<IGlobalLADData> flattenAllComparator = new Comparator<IGlobalLADData>() {

		@Override
		public int compare(final IGlobalLADData c1, final IGlobalLADData c2) {
			int cmp = c2.compareInsert(c1);

			if (cmp == 0) {
				cmp = c2.compareErt(c1);
			}

			if (cmp == 0) {
				cmp = c2.compareScet(c1);
			}

			if (cmp == 0) {
				cmp = c2.getIdentifier().toString().compareTo(c1.getIdentifier().toString());
			}

			return cmp;
		}
	};

	/**
	 * More updates. Taking out the insert number check because each of the compare methods already do that.
	 *
	 * Comparator used to flatten query result maps and order based on ERT.
	 */
	public static final Comparator<IGlobalLADData> flattenErtComparator = new Comparator<IGlobalLADData>() {

		@Override
		public int compare(final IGlobalLADData c1, final IGlobalLADData c2) {
			int cmp = c2.compareErt(c1);

			if (cmp == 0) {
				cmp = c2.compareInsert(c1);
			}

			if (cmp == 0) {
				cmp = c2.compareScet(c1);
			}

			if (cmp == 0) {
				cmp = c2.getIdentifier().toString().compareTo(c1.getIdentifier().toString());
			}

			return cmp;
		}
	};

	/**
	 * More updates.  Taking out the insert number check because each of the compare methods already do that.
	 *
	 * Comparator used to flatten query result maps and order based on SCET.
	 */
	public static final Comparator<IGlobalLADData> flattenScetComparator = new Comparator<IGlobalLADData>() {

		@Override
		public int compare(final IGlobalLADData c1, final IGlobalLADData c2) {
			int cmp = c2.compareScet(c1);

			if (cmp == 0) {
				cmp = c2.compareInsert(c1);
			}

			if (cmp == 0) {
				cmp = c2.compareErt(c1);
			}

			if (cmp == 0) {
				cmp = c2.getIdentifier().toString().compareTo(c1.getIdentifier().toString());
			}

			return cmp;
		}
	};

	/**
	 * Looks up the user data type for the input parameters.
	 *
	 * @param queryType
	 * @param dataSource - fsw|header|sse|monitor
	 * @param recordedState - realtime|recorded.
	 * @return collection of user data types mapped to queryType, dataSource and recordedState
	 */
	public Collection<Byte> lookupUserDataTypes(String queryType, String dataSource, String recordedState);

	/**
	 * Figures out the UDT of the given data object.  Special care should be taken to make sure this implemention if very fast because
	 * this will be used in the data creation work flow.
	 *
	 * @param data
	 * @return the user data type of data
	 */
	public byte lookupUserDataType(IGlobalLADData data);

	/**
	 * Looks up the user data type for the input parameters.
	 *
	 * @param queryType
	 * @param dataSource - fsw|header|sse|monitor
	 * @param recordedState - realtime|recorded.
	 *
	 * @return collection of user data types mapped to queryType, dataSource and recordedState
	 */
	public Collection<Byte> lookupUserDataTypes(QueryType queryType, DataSource dataSource, RecordedState recordedState);
	
	/**
	 * Converts the payload of transport to the proper global lad data object.
	 * @param transport
	 * @return glad data 
	 * @throws GlobalLadDataException
	 */
	public IGlobalLADData loadLadData(Proto3GlobalLadTransport transport) throws GlobalLadDataException;
	
	/**
	 * @param userDataType
	 * @return
	 */
	public boolean isRealtime(byte userDataType);
	/**
	 * @param userDataType
	 * @return
	 */
	public boolean isSse(byte userDataType);
	/**
	 * @param userDataType
	 * @return
	 */
	public boolean isHeader(byte userDataType);
	/**
	 * @param userDataType
	 * @return
	 */
	public boolean isMonitor(byte userDataType);
	
	/**
	 * @param userDataType
	 * @return
	 */
	public boolean isFsw(byte userDataType);

	/**
     * Converts export bytes into a Proto3GlobalLadTransport object and calls loadLadData with
     * the resulting object.
     * 
     * @param transportBytes
     *            byte array of data to load into GlobalLAD
     * @return glad data
     * @throws InvalidProtocolBufferException
     *             When an error occurs loading data into the GlobalLAD
     * @throws GlobalLadDataException
     *             When an error occurs loading data into the GlobalLAD
     */
	public default IGlobalLADData loadLadData(final byte[] transportBytes) throws InvalidProtocolBufferException, GlobalLadDataException {
        return loadLadData(Proto3GlobalLadTransport.parseFrom(transportBytes));
	}
}
