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
/**
 * 
 */
package jpl.gds.dictionary.impl.decom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.decom.IDecomMapDefinition;
import jpl.gds.dictionary.api.decom.IDecomMapId;
import jpl.gds.dictionary.api.decom.IDecomStatement;

/**
 * Represents the dictionary definition of a single packet decom map for generic
 * decom.
 * 
 *
 */
public class DecomMapDefinition implements IDecomMapDefinition {

    private final static int INITIAL_STATEMENT_LIST_SIZE = 16;

    private final List<IDecomStatement> statements;
    
    private IDecomMapId mapId;

    private int apid = NO_APID;
    private String name;
    private boolean general;
 
    /**
     * Constructor.
     */
    public DecomMapDefinition() {

        this.statements = new ArrayList<IDecomStatement>(INITIAL_STATEMENT_LIST_SIZE);
    }

    /** {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.IDecomMapDefinition#isGeneral()
     */
    @Override
    public boolean isGeneral() {

        return this.general;
    }

    /** {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.IDecomMapDefinition#setGeneral(boolean)
     */
    @Override
    public void setGeneral(final boolean general) {

        this.general = general;
        this.apid = NO_APID;
    }

    /** {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.IDecomMapDefinition#getApid()
     */
    @Override
    public int getApid() {

        return this.apid;
    }

    /** {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.IDecomMapDefinition#setApid(int)
     */
    @Override
    public void setApid(final int apid) {

        this.apid = apid;
    }

    /** {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.IDecomMapDefinition#getName()
     */
    @Override
    public String getName() {

        return this.name;
    }

    /** {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.IDecomMapDefinition#setName(java.lang.String)
     */
    @Override
    public void setName(final String name) {

        this.name = name;
    }

    /** {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.IDecomMapDefinition#addStatement(jpl.gds.dictionary.impl.impl.api.decom.adaptation.IDecomStatement)
     */

   @Override
   public synchronized void addStatement(final IDecomStatement stmt) {

        this.statements.add(stmt);
    }

    /** {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.IDecomMapDefinition#getStatementsToExecute()
     */
    @Override
    public synchronized List<IDecomStatement> getStatementsToExecute() {
        /* Return a non-modifiable version of the list. */
        return Collections.unmodifiableList(this.statements);
    }
    
    /*
     *  Added to support decom map definition factory.
     */
    
    /* Removed parseDecomFile method that takes
     * no channel map. There is no safe default for the channel map.
     */
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.IDecomMapDefinition#parseDecomFile(java.lang.String, java.util.Map)
     */
    @Override
    public synchronized void parseDecomFile(final String decomFile, final Map<String, IChannelDefinition> chanMap) throws DictionaryException {
        /*  Do not create new instance. Clear and populate the current instance. */
        clear();
        final OldDecomMapParser parser = new OldDecomMapParser(this, chanMap);
        parser.parseDecomMapXml(decomFile);
        /*  Removed catch of WrongSchemaException. */
      
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.IDecomMapDefinition#clear()
     */
    @Override
    public void clear() {
        this.general = false;
        this.apid = NO_APID;
        this.name = null;
        this.statements.clear();
    }

    @Override
    public void setId(IDecomMapId id) {
    	mapId = id;
    }

	@Override
	public IDecomMapId getId() {
		return mapId;
	}
    
}