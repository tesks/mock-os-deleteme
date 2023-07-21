package jpl.gds.dictionary.impl.decom;

import java.util.LinkedList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import jpl.gds.dictionary.api.decom.IDecomStatement;
import jpl.gds.dictionary.api.decom.IDecomStatementFactory;
import jpl.gds.shared.xml.SubHandler;
import jpl.gds.shared.xml.XmlUtility;

/**
 * Sub-handler /parser that will take care of parsing ground configured variable
 * blocks in decom maps. See {@link SubHandler} class for usage.
 *
 */
public class GroundConfigurationsParser extends SubHandler {
	
	private final IDecomStatementFactory statementFactory;

	private static final String CONFIGURATIONS = "configurations";
	private boolean inConfigurations = false;

    private final StringBuilder text = new StringBuilder(10);
    private String currentVarName;
    
    List<IDecomStatement> varDefs = new LinkedList<>();
	
    /**
     * Create a new parser, which will set the parent handler as the content handler on
     * the provided reader.
     * @param reader the XMLReader to pass content handling back to
     * @param parent the parent ContentHandler that should regain control of parsing
     * @param callLastEventOnParent if true, the final endElement call on this parser
     * 			will also be called on the parent.
     */
	public GroundConfigurationsParser(final XMLReader reader, final DefaultHandler parent, final boolean callLastEventOnParent) {
		super(CONFIGURATIONS, reader, parent, callLastEventOnParent);
		this.statementFactory = IDecomStatementFactory.newInstance();
	}
	
	@Override
	public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) {
	
			if (CONFIGURATIONS.equals(qName)) {
				inConfigurations = true;
			} else if (inConfigurations) {
				/** In configurations, element names are any string */
				currentVarName = qName;
			}
	}
	
	@Override
    public void characters(final char[] ch, final int start, final int length)
            throws SAXException {
        final String newText = new String(ch, start, length);

        if (!newText.equals("\n")) {
            this.text.append(newText);
        }
	}
	
	@Override
	public void endElement(final String uri, final String localName, final String qName)
			throws SAXException {

		final String ntext = XmlUtility.normalizeWhitespace(this.text);
		text.setLength(0);
		if (CONFIGURATIONS.equals(qName)) {
			inConfigurations = false;
			reader.setContentHandler(parent);
			if (callLastEventOnParent) {
				parent.endElement(uri, localName, qName);
			}
		} else if (inConfigurations) {
			// Note that this may throw a NumberFormatException, but that will be caught by the surround catch of ILlegalArgumentException
			final long varValue = Long.parseLong(ntext);
			varDefs.add(statementFactory.createGroundVariableDefinition(currentVarName, varValue));
		}

	}

	/**
	 * Get the parsed variable definitions.
	 * @return all the variable definitions this handler has parsed
	 */
	public List<IDecomStatement> getStatements() {
		return varDefs;
	}
}

