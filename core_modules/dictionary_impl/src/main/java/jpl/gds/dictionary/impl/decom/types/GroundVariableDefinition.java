package jpl.gds.dictionary.impl.decom.types;

import jpl.gds.dictionary.api.decom.types.IGroundVariableDefinition;

/**
 * Implementation class for ground configured variables in decom maps.
 *
 */
public class GroundVariableDefinition implements IGroundVariableDefinition {

	private final String name;
	private final long value;
	
	/**
	 * Create a new instance.
	 * @param name the variable name
	 * @param value the variable value
	 */
	public GroundVariableDefinition(String name, long value) {
		this.name = name;
		this.value = value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public long getValue() {
		return value;
	}

}
