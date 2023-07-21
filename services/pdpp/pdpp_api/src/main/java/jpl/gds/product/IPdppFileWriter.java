/*
 * Copyright 2006-2020. California Institute of Technology.
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
package jpl.gds.product;

import jpl.gds.product.api.ProductException;
import jpl.gds.product.api.file.IProductFilename;

/**
 * Interface for PdppFileWriter, which is a tool to allow PDPP to create new,
 * child products and metadata files after processing
 */
public interface IPdppFileWriter {

    /**
     * Copy a product entry from the source session to the destination session.
     *
     * @param srcPfn the file that is being written
     * @throws ProductException an error is encountered while writing the product
     * @return IProductFilename
     */
    IProductFilename writeFile(IProductFilename srcPfn) throws ProductException;
}
