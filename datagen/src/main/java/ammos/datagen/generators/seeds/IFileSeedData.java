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
package ammos.datagen.generators.seeds;

import ammos.datagen.config.TraversalType;

/**
 * This interface is implemented by seed data classes that support data
 * generators that implemented IFileSeededGenerator. These generators rely upon
 * an underlying seed file of data values that feed the generator. Therefore,
 * the seed class provides methods for getting and setting the seed file path.
 * 
 *
 */
public interface IFileSeedData extends ISeedData {
	/**
	 * Token that indicates the default seed file path.
	 */
	public static String DEFAULT_TABLE_NAME = "default";

	/**
	 * Gets the file path to the seed file for this integer generator.
	 * 
	 * @return file path, or the "default" table indicator
	 *         (IFileSeedData.DEFAULT_TABLE_NAME).
	 */
	public String getSeedFile();

	/**
	 * Sets the file path to the seed file for this integer generator.
	 * 
	 * @param tableName
	 *            file path, or the "default" table indicator
	 *            (IFileSeedData.DEFAULT_TABLE_NAME).
	 */
	public void setSeedFile(String tableName);

	/**
	 * Gets the traversal type for value generation: RANDOM or SEQUENTIAL.
	 * 
	 * @return TraversalType enumeration value
	 */
	public TraversalType getTraversalType();
}
