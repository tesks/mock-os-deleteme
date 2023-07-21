/*
 * Copyright 2006-2019. California Institute of Technology.
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
package jpl.gds.tc.api;

import java.io.File;
import java.io.IOException;
import java.util.List;

import jpl.gds.tc.api.cltu.ICltu;
import jpl.gds.tc.api.exception.CltuEndecException;
import jpl.gds.tc.api.exception.ScmfParseException;
import jpl.gds.tc.api.exception.ScmfWrapUnwrapException;

/**
 * The interface for any ScmfFactory class
 *
 */
public interface IScmfFactory {

    /**
     * Deserialize an SCMF file into an SCMF object and return it.
     *
     * @param filename The path to the SCMF file to parse.
     *
     * @return The SCMF object corresponding to the input SCMF file.
     *
     * @throws IOException
     * @throws ScmfWrapUnwrapException
     */
    IScmf parse(String filename) throws IOException, ScmfWrapUnwrapException, ScmfParseException;

    /**
     * Deserialize an SCMF file into an SCMF object and return it.
     *
     * @param scmfFile The pointer to the SCMF file to parse.
     *
     * @return The SCMF object corresponding to the input SCMF file.
     *
     * @throws IOException
     * @throws ScmfWrapUnwrapException
     */
    IScmf parse(File scmfFile) throws IOException, ScmfWrapUnwrapException, ScmfParseException;

    /**
     * Wrap CLTUs in SCMF
     *
     * @param flightCltus The CLTUs
     * @param writeToDisk whether or not to write the SCMF to disk
     *
     * @return The SCMF object built from the input CLTUs
     *
     * @throws IOException
     * @throws ScmfWrapUnwrapException if SCMF exceeds max file size
     */
    IScmf toScmf(List<ICltu> flightCltus, boolean writeToDisk)
            throws IOException, ScmfWrapUnwrapException, CltuEndecException;

    /**
     * Wrap binary data in SCMF
     *
     * @param data The byte array of data
     * @param writeToDisk whether or not to write the SCMF to disk
     *
     * @return The SCMF object built from the input data
     *
     * @throws IOException
     * @throws ScmfWrapUnwrapException if SCMF exceeds max file size
     */
    IScmf toScmf(byte[] data, boolean writeToDisk) throws IOException, ScmfWrapUnwrapException;
    
    /**
     * Wrap binary data in SCMF
     *
     * @param writeToDisk whether or not to write the SCMF to disk
     * @param data The list byte array of data to be put into messages
     *
     * @return The SCMF object built from the input data
     *
     * @throws IOException IO Exception
     * @throws ScmfWrapUnwrapException if SCMF exceeds max file size
	 *
	 * MPCS-10813 - 04/09/19 - added function
     */
    IScmf toScmf(final boolean writeToDisk, final List<Byte[]> data) throws IOException, ScmfWrapUnwrapException;

    /**
     * Wrap raw file with SCMF
     *
     * @param dataFile The data file
     * @param isHexFile True if the file contains ASCII chars, false if it's
     *            pure binary
     * @param writeToDisk whether or not to write the SCMF to disk
     *
     * @return The SCMF object corresponding to the input data
     *
     * @throws IOException
     * @throws ScmfWrapUnwrapException if SCMF exceeds max file size
     */
    IScmf toScmf(File dataFile, boolean isHexFile, boolean writeToDisk)
            throws IOException, ScmfWrapUnwrapException;

}