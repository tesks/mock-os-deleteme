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
package jpl.gds.dictionary.api.evr;

import java.util.List;

import jpl.gds.dictionary.api.IAttributesSupport;
import jpl.gds.dictionary.api.ICategorySupport;
import jpl.gds.serialization.evr.Proto3EvrDefinition;
import jpl.gds.shared.annotation.Jira;

/**
 * The IEvrDefinition interface is to be implemented by all EVR definition
 * classes.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * IEvrDefinition defines methods needed to interact with EVR Definition objects
 * as required by the IEvrDictionary interface. It is primarily used by EVR
 * dictionary file parser implementations in conjunction with the
 * EvrDefinitionFactory, which will be used to create actual EVR Definition
 * objects in the parsers. IEvrDictionary objects should interact with EVR
 * Definition objects only through the Factory and the IEvrDefinition interface.
 * Interaction with the actual EVR Definition implementation classes in an
 * IEvrDictionary implementation is contrary to multi-mission development
 * standards.
 * 
 *
 *
 * @see IEvrDictionary
 */
public interface IEvrDefinition extends IAttributesSupport, ICategorySupport{

	/**
	 * Sets the ID of the EVR. ID is an unsigned integer up to 32 bits in length
	 * and is required on all EVR definitions. It must be unique within an EVR
	 * dictionary.
	 * <p>
	 * The ID attribute of an EVR is specified using the "id" attribute on the
	 * &lt;evr&gt; element in the multimission EVR dictionary schema.
	 * 
	 * @param id
	 *            unique ID of the EVR definition to set
	 */
	public void setId(long id);

	/**
	 * Returns the EVR ID. ID is an unsigned integer up to 32 bits in length
	 * and is required on all EVR definitions. It must be unique within an EVR
	 * dictionary.
	 * <p>
	 * The ID attribute of an EVR is specified using the "id" attribute on the
	 * &lt;evr&gt; element in the multimission EVR dictionary schema.
	 * 
	 * @return unique ID of the EVR Definition
	 */
	public long getId();

	/**
	 * Sets the level of the EVR. Level identifies the type of the EVR: Warning,
	 * Error, Diagnostic etc. The valid level names are mission-specific. Level is
	 * required on all EVR definitions, consists of letters, numbers, underscores,
	 * and dashes, and is limited to 16 characters in length.
	 * <p>
	 * The level attribute of an EVR is specified using the "level" attribute on the
	 * &lt;evr&gt; element in the multimission EVR dictionary schema.
	 * 
	 * @param level
	 *            EVR level to set; may not be null
	 */
	public void setLevel(String level);

	/**
	 * Returns the EVR level. Level identifies the type of the EVR: Warning,
	 * Error, Diagnostic etc. The valid level names are mission-specific. Level is
	 * required on all EVR definitions, consists of letters, numbers, underscores,
	 * and dashes, and is limited to 16 characters in length.
	 * <p>
	 * The level attribute of an EVR is specified using the "level" attribute on the
	 * &lt;evr&gt; element in the multimission EVR dictionary schema.
	 * 
	 * @return level of the EVR; may not be null
	 */
	public String getLevel();

	/**
	 * Sets the format string of the EVR. The format string is message text with
	 * C printf-style formatters used as placeholders for the EVR arguments,
	 * which are inserted at runtime. "Bus %s has current voltage level %f". The
	 * EVR format string is required, may contain any characters, and is limited
	 * to 400 characters in length.
	 * <p>
	 * The format string attribute of an EVR is specified using the
	 * &lt;format_message&gt; element within the &lt;evr&gt; element in the
	 * multimission EVR dictionary schema.
	 * 
	 * @param fmtStr
	 *            EVR's format string to set; may not be null
	 */
	public void setFormatString(String fmtStr);

	/**
	 * Returns the format string of the EVR. The format string is message text with
	 * C printf-style formatters used as placeholders for the EVR arguments,
	 * which are inserted at runtime. "Bus %s has current voltage level %f". The
	 * EVR format string is required, may contain any characters, and is limited
	 * to 400 characters in length.
	 * <p>
	 * The format string attribute of an EVR is specified using the
	 * &lt;format_message&gt; element within the &lt;evr&gt; element in the
	 * multimission EVR dictionary schema.
	 * 
	 * @return EVR's format string; may not be null
	 */
	public String getFormatString();

	/**
	 * Sets the name of the EVR. EVR name is required for all EVR definitions.
	 * EVR name consists only of letters, numbers, underscores, and dashes, and
	 * is limited to 128 characters in length. It must be unique within an EVR
	 * dictionary.
	 * <p>
	 * The name attribute of an EVR is specified using the "name" attribute on
	 * the &lt;evr&gt; element in the multimission EVR dictionary schema.
	 * 
	 * @param name
	 *            EVR's name; may not be null
	 */
	public void setName(String name);

	/**
	 * Returns the name of the EVR. EVR name is required for all EVR definitions.
	 * EVR name consists only of letters, numbers, underscores, and dashes, and
	 * is limited to 128 characters in length. It must be unique within an EVR
	 * dictionary.
	 * <p>
	 * The name attribute of an EVR is specified using the "name" attribute on
	 * the &lt;evr&gt; element in the multimission EVR dictionary schema.
	 * 
	 * @return EVR's name
	 */
	public String getName();

	/**
	 * Sets the module associated with this EVR definition. Module is used by
	 * the author of the dictionary as a means for categorizing EVRs. May be
	 * null.
	 * <p>
	 * The module attribute of an EVR is specified using the &lt;category&gt;
	 * element in the multimission EVR dictionary schema.
	 * 
	 * @param module
	 *            EVR's module; may be null
     * @deprecated use setCategory instead
	 */
    @Deprecated //  Replaced with ICategorySupport.
	public void setModule(String module);

	/**
	 * Returns the module associated with this EVR definition. Module is used by
	 * the author of the dictionary as a means for categorizing EVRs. May be
	 * null.
	 * <p>
	 * The module attribute of an EVR is specified using the &lt;category&gt;
	 * element in the multimission EVR dictionary schema.
	 * 
	 * @return EVR's module; may be null
     * @deprecated use getCategory instead
	 */
    @Deprecated // Replaced with ICategorySupport.
	public String getModule();

	/**
	 * Sets the operational category associated with this EVR definition. Ops
	 * category is used by the author of the dictionary as a means for
	 * categorizing EVRs. May be null.
	 * <p>
	 * The operational category attribute of an EVR is specified using the
	 * &lt;category&gt; element in the multimission EVR dictionary schema.
	 * 
	 * @param opscat
	 *            EVR's operational category; may be null
     * @deprecated use setCategory instead
	 */
    @Deprecated //  Replaced with ICategorySupport.
	public void setOpsCategory(String opscat);

	/**
	 * Returns the operational category associated with this EVR definition. Ops
	 * category is used by the author of the dictionary as a means for
	 * categorizing EVRs. May be null.
	 * <p>
	 * The operational category attribute of an EVR is specified using the
	 * &lt;category&gt; element in the multimission EVR dictionary schema.
	 * 
	 * @return EVR's operational category; may be null
     * @deprecated use getCategory instead
	 */
    @Deprecated // Replaced with ICategorySupport.
	public String getOpsCategory();

	/**
	 * Sets the subsystem name associated with this EVR.  Subsystem is used
	 * by the author of the dictionary as a means for categorizing channels.
	 * May be null.
	 * <p>
	 * The subsystem attribute of an EVR is specified using the
	 * &lt;category&gt; element in the multimission EVR dictionary schema.
	 *
	 * @param subsys
	 *            the subsystem to set; may be null
     * @deprecated use setCategory instead
	 */
    @Deprecated // Replaced with ICategorySupport.
	public abstract void setSubsystem(String subsys);

	/**
	 * Gets the subsystem name associated with this EVR.  Subsystem is used
	 * by the author of the dictionary as a means for categorizing channels.
	 * May be null.
	 * <p>
	 * The subsystem attribute of an EVR is specified using the
	 * &lt;category&gt; element in the multimission EVR dictionary schema.
	 *
	 * @return the subsystem text, or null if none defined
     * @deprecated use setCategory instead
	 */
    @Deprecated //  Replaced with ICategorySupport.
	public abstract String getSubsystem();


	/**
	 * Number of arguments defined for this EVR definition. Returns zero if the
	 * EVR has no arguments or the dictionary implementation does not support
	 * definition of EVR arguments.
	 * <p>
	 * The number of arguments attribute of an EVR is specified using the
	 * &lt;number_of_arguments&gt; element within the &lt;evr&gt; element in the
	 * multimission EVR dictionary schema.
	 * 
	 * @return number of arguments defined; The returned value SHOULD match
	 *         getArgs().size().
	 * @since AMPCS 3.0
	 */
	public int getNargs();

	/**
	 * Set number of arguments defined for this EVR definition. Returns zero if the
	 * EVR has no arguments or the dictionary implementation does not support
	 * definition of EVR arguments.
	 * <p>
	 * The number of arguments attribute of an EVR is specified using the
	 * &lt;number_of_arguments&gt; element within the &lt;evr&gt; element in the
	 * multimission EVR dictionary schema.
	 * 
	 * @param nargs number of arguments to set
	 * 
	 * @since AMPCS 3.0
	 */
	public void setNargs(final int nargs);

	/**
	 * Gets the list of defined EVR arguments.
	 * <p>
	 * EVR arguments are specified using the &lt;args&gt; element within the
	 * &lt;evr&gt; element in the multimission EVR dictionary schema.
	 * 
	 * @return list of argument definitions; returns an empty list if no
	 *         arguments are defined
	 * 
	 * @since AMPCS 3.0
	 * 
	 */
	public List<IEvrArgumentDefinition> getArgs();

	/**
	 * Add argument definitions to this EVR object. Number of arguments should
	 * match getNargs() when object is completely constructed.
	 * <p>
	 * EVR arguments are specified using the &lt;args&gt; element within the
	 * &lt;evr&gt; element in the multimission EVR dictionary schema.
	 * 
	 * @param args
	 *            list of argument definitions
	 * 
	 * @since AMPCS 3.0
	 */
	public void setArgs(List<IEvrArgumentDefinition> args);
	
	/**
	 * Sets the definition type (source) of this EVR definition.
	 * 
	 * @param type the EvrDefinitionType to set
	 * 
	 */
	public void setDefinitionType(EvrDefinitionType type);
	
	
	/**
	 * Gets the definition type (source) of this EVR definition.
	 * 
	 * @return the EvrDefinitionType
	 * 
	 */
	public EvrDefinitionType getDefinitionType();

	/**
	 * Gets the IEvrDefintion as a protobuf message
	 * 
	 * @return the protobuf representation of this IEvrDefinition
	 */
	public Proto3EvrDefinition build();

	/**
	 * Restores the values from the supplied protobuf message to this
	 * IEvrDefinition
	 * 
	 * @param msg
	 *            the protobuf message to be restored as an IEvrDefiition
	 */
	public void load(Proto3EvrDefinition msg);

}
