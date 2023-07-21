package jpl.gds.shared.xml;

import org.xml.sax.ContentHandler;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This class extends the SAX default handler to provide a common
 * super class for SAX parsers to be reusable for different schemas
 * that share common elements.
 * 
 * SubHandlers should receive control of XML parsing when the root element returned by
 * {@link #getRootElement()} is first seen, and will return control to the parent when the
 * closing tag for the same element is observed. The SubHandler will call {@link #endElement(String, String, String)}
 * on the parent handler if the parent handler indicates that is necessary.
 * 
 * This handler should always receive the SAX callback pertaining to its root element.
 * This means that if a parent parser intercepts a callback, it should call the corresponding
 * callback on this sub parser.
 * 
 * For example, take the following XML:
 * <subHandlerRoot>foo</subHandlerRoot>
 * 
 * If the parent receives the startElement(String, String, String, Attributes) call from the SAXParser,
 * it needs to call:
 * subHandlerInstance.startElement(String, String, String, Attributes)
 * 
 * However, if the XML being dealt with looks like this:
 * <subHandlerBlockStart>
 * 		<subHandlerRoot>foo</subHandlerRoot>
 * </subHandlerBlockStart>
 * 
 * The parent handler can use the startElement(String, String, String, Attributes) call denoting
 * the subHandlerBlockStart element as an indication of when to pass parsing off to the subHandler.
 * 
 *
 */
public class SubHandler extends DefaultHandler {
	
	protected final String rootElement;
	protected final XMLReader reader;
	protected final ContentHandler parent;
	protected final boolean callLastEventOnParent;
	
	/**
	 * 
	 * @param rootElement The qName of the outermost element this handler parses.
	 * @param reader the reader object that drives parsing, and will be set with the parent parser
	 * 			when the sub parser reaches the end of its root element.
	 * @param parent the parent parser to set as the content handler when the sub parser reaches the end
	 * 			of its root element
	 * @param callLastEventOnParent whether the end element call that will terminate this sub handler's parsing
	 * 			should also be called on the parent handler.
	 */
	protected SubHandler(String rootElement, XMLReader reader, ContentHandler parent, boolean callLastEventOnParent) {
		this.rootElement = rootElement;
		this.reader = reader;
		this.parent = parent;
		this.callLastEventOnParent = callLastEventOnParent;
	}
	
	public final String getRootElement() {
		return rootElement;
	}

}
