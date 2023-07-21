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
package ammos.datagen.generators;

import ammos.datagen.generators.seeds.IFileSeedData;

/**
 * This interface is implemented by all classes that generate discrete values
 * for inclusion in generated packets, whether the generated values are used as
 * EVR arguments, channel values, or data product fields, using a
 * pre-established list of seed values as source. An IFileSeededGenerator is
 * assumed to be driven by an underlying list of values loaded from a file.
 * There is a default file, which can be overridden in the data generator run
 * configuration. This interface supports both sequential (see method getNext())
 * and random (see method getRandom()) return of values from the underlying list
 * of available values.
 * 
 */
public interface IFileSeededGenerator extends ISeededGenerator {

	/**
	 * Token to pass load methods to tell them to load the default seed table
	 * file.
	 */
	public static final String DEFAULT_TABLE_NAME = IFileSeedData.DEFAULT_TABLE_NAME;

	/**
	 * Loads the available values for this generator from the current seed file.
	 * The file is assumed to consist of one value per line, followed by a line
	 * feed. Values which are invalid (cannot be converted to the proper data
	 * type for the current generator) will be discarded, though a warning will
	 * be printed. Blank lines will be skipped except in string generators,
	 * which may preserve them. The seed file path must be set using the
	 * ISeedData object and the setSeedData() method.
	 * 
	 * @return true if the file was loaded, false if not.
	 */
	public boolean load();

	/**
	 * Returns the total number of different values this generator will produce
	 * from getNext() before wrapping.
	 * 
	 * @return count of total potential values
	 */
	public int getCount();

	/**
	 * Returns the path of the last seed file loaded. May be null if no file has
	 * been loaded or the generator does not use a seed file.
	 * 
	 * @return the path to the seed file, or null if none loaded
	 */
	public String getSeedFile();
}
