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
package jpl.gds.globallad.data.container;

import jpl.gds.globallad.data.container.search.IGlobalLadContainerSearchAlgorithm;
import jpl.gds.globallad.data.container.search.IGlobalLadDataSearchAlgorithm;

/**
 * A super search algorithm that works with both containers and global lad data.  Used
 * as the input to the queryable interface.
 */
public interface IGlobalLadSearchAlgorithm extends IGlobalLadContainerSearchAlgorithm, IGlobalLadDataSearchAlgorithm {}
