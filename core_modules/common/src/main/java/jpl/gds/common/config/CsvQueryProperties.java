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
package jpl.gds.common.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jpl.gds.shared.annotation.Singleton;
import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.types.Pair;



/**
 * This configuration class loads and provides access to the columns to be used
 * with CSV. (Comma separated values.) This is primarily used by the database
 * query tools.
 *
 *
 * 
 * @TODO - R8 Refactor TODO - Should this singleton be in the application
 *       context?
 */
@Singleton
public class CsvQueryProperties extends GdsHierarchicalProperties
{
    private static final String PROPERTY_FILE       = "csv_query.properties";
    private static final String PROPERTY_PREFIX = "csvQuery.";
    private static final String DESCRIPTION     = "description";
    private static final String HEADER          = "header";
    private static final String DOT             = ".";
    private static final String COMMA           = ",";
    private static final String EMPTY           = "";

    /** Next four must be uppercase and public. */

    /** DatabaseChannelSample must use exact same value */
    public static final String TYPE                = "TYPE";

    /** DatabaseChannelSample must use exact same value */
    public static final String DN                  = "DN";

    /** DatabaseEvr must use exact same value */
    public static final String METADATAKEYWORDLIST = "METADATAKEYWORDLIST";

    /** DatabaseEvr must use exact same value */
    public static final String METADATAVALUESLIST  = "METADATAVALUESLIST";

    private static CsvQueryProperties _instance = null;

    /**
     * Constructor. Construct and load instance.
     *
     */
    public CsvQueryProperties()
    {
        super(PROPERTY_FILE, true);
    }


    /**
     * Get static instance, creating it if required.
     *
     * @return Instance
     * 
     * @TODO R8 Refactor TODO - Does this remain a singleton, or should
     *       it be put into the context, or...?
     */
    public static synchronized CsvQueryProperties instance()
    {
        if (_instance == null)
        {
             _instance = new CsvQueryProperties();
        }

        return _instance;
    }


	/**
	 * Get lists of columns and headers from configuration.
	 *
	 * The application is NOT case-blind.
	 *
	 * @param rawApplication
	 *            Application that needs CSV
	 *
	 * @return Pair of lists of columns and headers
	 *
	 *
	 * @throws CsvQueryPropertiesException
	 *             if there is a problem reading the csv column or header values
	 */
    public Pair<List<String>, List<String>> getCsvLists(final String rawApplication) throws CsvQueryPropertiesException
    {
        final String application = StringUtil.safeTrim(rawApplication);

        if (application.isEmpty())
        {
            return new Pair<>(Collections.emptyList(),
                                                        Collections.emptyList());
        }

        final List<String> columns = getInternalCsvColumnList(application);
        final List<String> headers = getInternalCsvHeaderList(application, columns);

        return new Pair<>(columns, headers);
    }


	/**
	 * Get list of columns from configuration. Note that the list returned must
	 * NOT be uppercased, because it may be used to populate the header. But we
	 * want to do case-blind comparisons of CSV columns.
	 *
	 * The application is NOT case-blind.
	 *
	 * @param application
	 *            Application that needs CSV
	 *
	 * @return List of columns
	 *
	 *
	 * @throws CsvQueryPropertiesException
	 *             if there is a problem with the requested csv column
	 */
    private List<String> getInternalCsvColumnList(final String application) throws CsvQueryPropertiesException
    {
        final String property = StringUtil.safeTrim(getProperty(PROPERTY_PREFIX + application));

        final String[] columns = property.split(COMMA, -1);

        final List<String> list   = new ArrayList<>(columns.length);
        final List<String> uplist = new ArrayList<>(columns.length);

        // If there is no entry or a single empty entry,
        // do not process columns.

        // (No entry or an empty entry will result in an array of length one
        // with an empty element.)

        if ((columns.length > 1) || ! columns[0].trim().isEmpty())
        {
            for (final String column : columns)
            {
                final String cce = column.trim();

                if (cce.isEmpty())
                {

                    throw new CsvQueryPropertiesException(
                                  "CSV column "            +
                                  "in configuration for '" +
                                  application              +
                                  "' in "                  +
                                  PROPERTY_FILE                +
                                  " is empty");
                }

                // Allow duplicates

                list.add(cce);
                uplist.add(cce.toUpperCase());
            }
        }

        // List can be empty if there is no entry or an empty entry,
        // and also if no columns are non-empty.

        if (list.isEmpty())
        {
            throw new CsvQueryPropertiesException(
                          "No valid CSV columns in configuration for '" +
                          application                                   +
                          "' in "                                       +
                          PROPERTY_FILE);
        }

        if (uplist.contains(METADATAKEYWORDLIST) !=
            uplist.contains(METADATAVALUESLIST))
        {
            throw new CsvQueryPropertiesException(METADATAKEYWORDLIST +
                                               " and "             +
                                               METADATAVALUESLIST  +
                                               " in "              +
                                               PROPERTY_FILE           +
                                               " must be specified together");
        }

        // It is not possible to use DN without knowing the type,
        // but you can have the type without DN.

        if (uplist.contains(DN) && ! uplist.contains(TYPE))
        {
            throw new CsvQueryPropertiesException(DN                         +
                                               " in "                     +
                                               PROPERTY_FILE                  +
                                               " must be specified with " +
                                               TYPE);
        }

        return list;
    }


	/**
	 * Get list of column headers from configuration. Note that the list
	 * returned must NOT be uppercased and duplicates are allowed. Empty or
	 * missing elements are filled from the column list.
	 *
	 * The application is NOT case-blind.
	 *
	 * @param application
	 *            Application that needs CSV
	 * @param columnList
	 *            List of columns
	 *
	 * @return List of column headers
	 *
	 *
	 * @throws CsvQueryPropertiesException
	 *             if the number of headers is greater than the number of
	 *             columns
	 */
    private List<String> getInternalCsvHeaderList(final String       application,
                                                  final List<String> columnList) throws CsvQueryPropertiesException
    {
        final int          size          = columnList.size();
        final List<String> rawHeaderList = getInternalCsvHeaderList(application);
        final int          rawSize       = rawHeaderList.size();

        if (rawSize > size)
        {
            throw new CsvQueryPropertiesException(
                          "In " + PROPERTY_FILE + ", the property "              +
                          application + ".header contains too many values (" +
                          rawSize + "). It cannot be longer than " + application +
                          " (" + size + ").");
        }

        final List<String> headerList = new ArrayList<>(size);

        for (int i = 0; i < size; ++i)
        {
            final String next = ((i < rawSize) ? rawHeaderList.get(i) : EMPTY);

            if (! next.isEmpty())
            {
                headerList.add(next);
            }
            else
            {
                headerList.add(columnList.get(i));
            }
        }

        return headerList;
    }


	/**
	 * Get list of column headers from configuration. Note that the list
	 * returned must NOT be uppercased and duplicates are allowed as well as
	 * empty entries.
	 *
	 * The application is NOT case-blind.
	 *
	 * @param application
	 *            Application that needs CSV
	 *
	 * @return List of column headers
	 *
	 */
	private List<String> getInternalCsvHeaderList(final String application) {
		final String property = StringUtil.safeTrim(getProperty(PROPERTY_PREFIX + application + DOT + HEADER));

		final String[] headers = property.split(COMMA, -1);

		final List<String> list = new ArrayList<>(headers.length);

		// If there is no entry or a single empty entry,
		// do not process headers.

		// (No entry or an empty entry will result in an array of length one
		// with an empty element.)

		if ((headers.length > 1) || !headers[0].trim().isEmpty()) {
			for (final String header : headers) {
				list.add(header.trim());
			}
		}

		// List can be empty if there is no entry or an empty entry.
		// But that's OK.

		return list;
	}


    /**
     * Get description from configuration.
     *
     * The application is NOT case-blind.
     *
     * @param rawApplication Application that needs CSV
     *
     * @return Description
     */
    public String getCsvDescription(final String rawApplication)
    {
        final String application = StringUtil.safeTrim(rawApplication);

        if (application.isEmpty())
        {
            return EMPTY;
        }

        return StringUtil.safeTrim(getProperty(PROPERTY_PREFIX + application + DOT + DESCRIPTION));
    }


    /**
     * This method is part of a proper singleton class. It prevents using
     * cloning as a hack around the singleton.
     *
     * @return It never returns
     *
     * @throws CloneNotSupportedException Always throws this exception
     */
    @Override
    public Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }
    
    @Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    } 
}
