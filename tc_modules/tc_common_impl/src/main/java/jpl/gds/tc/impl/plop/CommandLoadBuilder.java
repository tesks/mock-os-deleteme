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

package jpl.gds.tc.impl.plop;

import java.util.ArrayList;
import java.util.List;

import jpl.gds.tc.api.cltu.ICltu;
import jpl.gds.tc.api.config.PlopProperties;
import jpl.gds.tc.api.config.PlopType;
import jpl.gds.tc.api.config.SessionLocationType;
import jpl.gds.tc.api.plop.ICommandLoadBuilder;
import jpl.gds.tc.api.session.ISessionBuilder;

/**
 * The job of this class is to take a set of CLTUs that are to be radiated and properly
 * insert all of the acquisition and idle sequences to make a complete command load.
 * 
 * A command load is a set of CLTUs, plus acquisition and idle sequences, that forms a single
 * giant string of bits to be radiated to the spacecraft.
 * 
 *
 * MPCS-9390 - 1/24/18 - Implemented ICommandLoadBuilder interface
 *
 */
public class CommandLoadBuilder implements ICommandLoadBuilder {
	/**
	 * The list of CLTUs used to build the command load
	 */
    private final List<ICltu> cltuList;

	/**
	 * Create a new command load builder with an empty CLTU list
	 */
	public CommandLoadBuilder()
	{
        this.cltuList = new ArrayList<>(ISessionBuilder.MAX_SESSION_SIZE);
	}

	/**
	 * Create a new command load builder with all of the input CLTUs
	 * as part of the command load.
	 * 
	 * @param cltus the Cltus to be added to the current command load being built
	 */
    public CommandLoadBuilder(final List<ICltu> cltus) {
        this.cltuList = new ArrayList<>(ISessionBuilder.MAX_SESSION_SIZE);
		addCltus(cltus);
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void clear() {
		this.cltuList.clear();
	}
	
	/**
     * {@inheritDoc}
     */
    @Override
    public void addCltus(final List<ICltu> cltus) {
		this.cltuList.addAll(cltus);
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public List<ICltu> getPlopCltus(final PlopProperties plopConfig) {
		final List<ICltu> plopCltus = new ArrayList<>(this.cltuList.size());
		
		//identifies acq seq locations
		final SessionLocationType beginCommandLoad = plopConfig.getAcquisitionSequenceLocation();
		
		//identifies idle seq locations
		final SessionLocationType idleSeqLocation = plopConfig.getIdleSequenceLocation();
		
		//really just different terminology for acq seq locations
		final PlopType plopType = plopConfig.getType();

		for(int i=0; i < this.cltuList.size(); i++) {
            final ICltu cltu = this.cltuList.get(i);

			//should this CLTU be preceded by an acq seq?
			switch(beginCommandLoad) {
				case FIRST:
					if(i == 0) {
						cltu.setAcquisitionSequence(plopConfig.getAcquisitionSequence());
					}
					break;

				case ALL:
					cltu.setAcquisitionSequence(plopConfig.getAcquisitionSequence());
					break;

				case NONE:
				default:
					break;
			}

			//handle the plop settings (usually redundant)
			switch(plopType.getValueAsInt()) {
				case PlopType.PLOP_1_TYPE:
					cltu.setAcquisitionSequence(plopConfig.getAcquisitionSequence());
					break;

				case PlopType.PLOP_2_TYPE:
					if(i == 0) {
						cltu.setAcquisitionSequence(plopConfig.getAcquisitionSequence());
					}
					if(i == this.cltuList.size()-1) {
						cltu.setIdleSequence(plopConfig.getIdleSequence());
					}
					break;

				case PlopType.NONE_TYPE:
				default:
					break;
			}

			//should this CLTU be trailed by an idle seq?
			switch(idleSeqLocation) {
				case LAST:
					if(i == this.cltuList.size()-1) {
						cltu.setIdleSequence(plopConfig.getIdleSequence());
					}
					break;

				case ALL:
					cltu.setIdleSequence(plopConfig.getIdleSequence());
					break;

				case NONE:
				default:
					break;
			}

			plopCltus.add(cltu);
		}

		return(plopCltus);
	}
}