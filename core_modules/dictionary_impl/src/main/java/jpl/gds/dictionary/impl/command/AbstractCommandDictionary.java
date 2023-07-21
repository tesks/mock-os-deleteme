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
package jpl.gds.dictionary.impl.command;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

import jpl.gds.dictionary.api.OpcodeUtil;
import jpl.gds.dictionary.api.command.CommandEnumerationDefinition;
import jpl.gds.dictionary.api.command.ICommandDefinition;
import jpl.gds.dictionary.api.command.ICommandDictionary;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.dictionary.impl.AbstractBaseDictionary;


/**
 * This class is the abstract superclass of each mission-dependent command
 * dictionary parser.  Command dictionary parsing is done by the
 * mission-dependent parsers providing a list of element name to handler method
 * mappings and then those methods being invoked from this superclass.  There
 * are also utility variables such as an element depth stack in this class.
 *
 * The Stack is used to keep track of which object is currently being operated on
 * in the SAX parsing of the XML.  For instance, when parsing a command argument, the Stack
 * might look something like:
 * 
 * --------------------
 * | Command Argument |
 * --------------------
 * --------------------
 * | Command          |
 * --------------------
 * --------------------
 * | Command Dict     |
 * --------------------
 * 
 * Because the "Command Argument" is on top of the Stack, we know that any attributes or elements
 * we operate on belong to the argument.
 *
 * Initialize version to "unknown" rather that null so that the absence of an XML attribute
 * with the expected name does not cause a Null Pointer Exception.
 *

 */
public abstract class AbstractCommandDictionary extends AbstractBaseDictionary
implements ICommandDictionary
{

	/**
	 * Regular expression for various possible excess whitespace characters.
	 */
	protected static final String EXCESS_WHITESPACE_REGEXP = "[ \r\n\t\b\f]{2,}";

	/** A stack of elements as we descend levels in the XML tree */
	protected Stack<Object> currentDepthStack;

	/** True if the parser is inside a repeat argument, false otherwise */
	protected boolean insideRepeat;

	/** The mapping from start element names to handler methods */
	protected Map<String,Method> startElementToMethodMap;

	/** The mapping from end element names to handler methods */
	protected Map<String,Method> endElementToMethodMap;

	/** List of parsed command definitions */
	private final List<ICommandDefinition> parsedDefinitions;

	/*
	 * Added list of parsed enumeration definitions.
	 */
	/** List of parsed enumeration definitions */
	private final Map<String, CommandEnumerationDefinition> parsedEnumerations;

	/** List of parsed uplink command file types */
    private final Map<String, Integer> uplinkFileTypes;

	/* Create & build opcode and stem maps */
	private final  Map<String, String> opcodeForStemMap;
	private final  Map<String, String> stemForOpcodeMap;
	
	/**
	 * Default constructor.  Simply calls <code>super()</code>.
	 * 
	 */
	AbstractCommandDictionary(final String maxSchemaVersion)
	{
		super(DictionaryType.COMMAND, maxSchemaVersion);

		currentDepthStack = new Stack<Object>();
		insideRepeat = false;
		startElementToMethodMap = new HashMap<String,Method>();
		endElementToMethodMap = new HashMap<String,Method>();
		parsedDefinitions = new ArrayList<ICommandDefinition>();
		opcodeForStemMap = new TreeMap<String, String>();
		stemForOpcodeMap = new TreeMap<String, String>();
        uplinkFileTypes = new HashMap<String, Integer>();
		/*
		 * Added initialization of map of parsed enumeration definitions.
		 */
		parsedEnumerations = new TreeMap<String, CommandEnumerationDefinition>();
		
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.IBaseDictionary#clear()
	 */
	@Override
	public void clear() {
		currentDepthStack.clear();
		insideRepeat = false;
		/*
		 * startElementToMethodMap and endElementToMethodMap are "static"
		 * assignments, thus don't require clearing.
		 */
		parsedDefinitions.clear();	
		opcodeForStemMap.clear();
		stemForOpcodeMap.clear();
		uplinkFileTypes.clear();
		super.clear();
	}


	/**
	 * @{inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.command.ICommandDictionary#getCommandDefinitions()
	 */
	@Override
	public List<ICommandDefinition> getCommandDefinitions() {
		return parsedDefinitions;
	}


	/**
	 * Gets the parsed enumeration definitions as a Map (enum name to enum object).
	 * Used only by subclasses.
	 * 
	 * @return Map of CommandEnumerationDefinition, keyed by enum type name
	 * 
	 */
	@Override
	public Map<String, CommandEnumerationDefinition> getArgumentEnumerations() {
		return Collections.unmodifiableMap(this.parsedEnumerations);
	}


	/**
	 * Replaces all excess whitespace characters with a single space, thereby
	 * normalizing them.
	 * 
	 * @param in	<code>String</code> object to apply whitespace normalization
	 * 				on
	 * @return	new <code>String</code> object based on <code>in</code>, with
	 * 			normalized whitespace
	 */
	protected String normalizeWhitespace(final String in)
	{
		return(in.replaceAll(EXCESS_WHITESPACE_REGEXP," "));
	}


    /**
     * Returns a set of uplink file types.
     */
    @Override
    public Set<String> getUplinkFileTypes() {
        final Set<String> fts = uplinkFileTypes.keySet();
        return(fts);
    }
    
	/**
     * Sets uplink file name and id to the parsed definitions
     * list.
     * 
     * @param name name of uplink file
     * @param id id of uplink file
     */
    @Override
    public void setUplinkFileType(final String name, final Integer id) {
        uplinkFileTypes.put(name, id);
    }
    
    /**
     * Retrieves the uplink file id from the uplink file name.
     * @param name name of uplink file
     * @return  id id of uplink file 
     */
    @Override
    public int getUplinkFileIdForType(final String name) {        
        return(uplinkFileTypes.get(name));       
    }
    
    /**
     * Retrieves the uplink file name from the uplink file id.
     * @param  id id of uplink file 
     * @return  name name of uplink file 
     * null or empty if no matching uplink file id
     */
    @Override
    public String getUplinkFileTypeForId(final int id) {
       for (final Entry<String, Integer> entry: uplinkFileTypes.entrySet()) {
           if (Objects.equals(id, entry.getValue())) {
               return entry.getKey();
           }
       }
        return null;    
    }

	/**
	 * Adds an <code>ICommandDefinition</code> object to the parsed definitions
	 * list.
	 * 
	 * @param def <code>ICommandDefinition</code> object to add
	 */
	protected void addDefinition(final ICommandDefinition def) {
		
		if(def == null) {
			final String errorMsg = "Null the input command";
			final IllegalArgumentException e = new IllegalArgumentException(errorMsg);
			tracer.error(errorMsg, e);
			throw e;
		} else if(def.getStem() == null) {
			final String errorMsg = "Stem cannot be null on the input command";
			final IllegalArgumentException e = new IllegalArgumentException(errorMsg);
			tracer.error(errorMsg, e);
			throw e;
		} else if(def.getOpcode() == null) {
			final String errorMsg = "Stem cannot be null on the input command";
			final IllegalArgumentException e = new IllegalArgumentException(errorMsg);
			tracer.error(errorMsg, e);
			throw e;
		}

        parsedDefinitions.add(def);
		final String stem = def.getStem();
		final String opcode = def.getOpcode();
        String cmd = this.opcodeForStemMap.get(stem.toLowerCase());
        
        if (cmd != null) {
            final StringBuilder errorMsg = new StringBuilder();
            errorMsg.append("Found duplicate command dictionary entries for stem ")
                    .append(stem.toUpperCase())
                    .append(" with opcodes ")
                    .append(getDictionaryConfiguration().getHideOpcode() ? "HIDDEN" 
                            : "0x" + cmd.toLowerCase() + " and 0x" + opcode.toLowerCase())
                    .append(".");
            tracer.warn(errorMsg.toString());
        }
        /*  Set opcode and stem in maps */
        opcodeForStemMap.put(stem.toUpperCase(), opcode.toLowerCase());
        cmd = this.stemForOpcodeMap.get(opcode.toLowerCase());
		
        if (cmd != null && !"null".equalsIgnoreCase(opcode)) {
			final StringBuilder errorMsg = new StringBuilder();
			errorMsg.append("Found duplicate command dictionary entries for opcode ")
			        .append(getDictionaryConfiguration().getHideOpcode() ? " HIDDEN" : "0x" + opcode.toLowerCase())
			        .append(" with stems \"" + cmd.toUpperCase() + "\" and \"" + stem.toUpperCase() + "\".");
            tracer.warn(errorMsg.toString());
        }
		
        

        stemForOpcodeMap.put(opcode.toLowerCase(), stem.toUpperCase());
	}


	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.command.ICommandDictionary#getOpcodeByStemMap()
	 */
	@Override
    public Map <String, String> getOpcodeByStemMap() {
		return new HashMap<String, String> (opcodeForStemMap);
	}

	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.command.ICommandDictionary#getStemByOpcodeMap()
	 */
	@Override
    public Map <String, String> getStemByOpcodeMap() {
		return new HashMap<String, String> (stemForOpcodeMap);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.command.ICommandDictionary#getOpcodeForStem(java.lang.String)
	 */
	@Override
    public int getOpcodeForStem(final String stem) {
		final String opcode = opcodeForStemMap.get(stem.toUpperCase());	
		if (opcode == null) {
			return NO_OPCODE;
		}

        return opcodeUtil.parseOpcodeFromHex(opcode);
	}
	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.command.ICommandDictionary#getCommandDefinitionForStem(java.lang.String)
	 */
	@Override
     public ICommandDefinition getCommandDefinitionForStem(final String stem) {
		 for (final ICommandDefinition cd: this.parsedDefinitions)
		 {
			 if (cd.getStem().equals(stem.toUpperCase())) {
				 return cd;
			 }
		 }
		 return null;
	 }
	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.command.ICommandDictionary#getCommandDefinitionForStem(java.lang.String)
	 */
	 @Override
     public ICommandDefinition getCommandDefinitionForOpcode(final String opcode) {
		 final String stem = stemForOpcodeMap.get(OpcodeUtil.stripHexPrefix(opcode.toLowerCase()));
		 if (stem == null) {
			 return null;
		 }
		 return getCommandDefinitionForStem(stem);
	 }
	 
	 /**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.command.ICommandDictionary#getStems()
	 */
	@Override
	public List<String> getStems() {
		 return new ArrayList<String>(opcodeForStemMap.keySet());
		 
	 }

	/**
	 * Retrieves the stem for opcode from the
	 * ICommandDefinition. May be null or empty
     * if no command definition exists.
	 *
     * @param opcode Opcode as int
     *
	 * @return stem
	 */
	@Override
    public String getStemForOpcode(final int opcode)
    {

		return stemForOpcodeMap.get(opcodeUtil.formatOpcode(opcode, false));

	}

	
	/**
	 * Adds a CommandEnumerationDefinition object to the parsed enumerations
	 * list. Will overwrite any enumeration with the same name as an existing enumeration.
	 * 
	 * @param def CommandEnumerationDefinition object to add
	 * 
	 */
	protected void addEnumeration(final CommandEnumerationDefinition def) {
		parsedEnumerations.put(def.getName(), def);
	}
}
