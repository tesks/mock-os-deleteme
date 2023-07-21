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
package jpl.gds.perspective.view;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import jpl.gds.perspective.config.PerspectiveProperties;
import jpl.gds.perspective.config.ViewType;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * ViewReference is used to represent a file pointer to a pre-defined view
 * within a perspective. ViewReferences are saved and loaded from the
 * perspective as references, which are then expanded to the actual view
 * configuration by reading the file they point to.
 * 
 */
public class ViewReference implements Comparable<ViewReference> {
    /**
     * XML view reference element name
     */
    public static final String VIEW_REFERENCE_TAG = "ViewReference";
    
    /**
     * XML reference path attribute name
     */
    public static final String PATH_TAG = "fullPath";
    
    /**
     * XML reference name attribute name
     */
    public static final String NAME_TAG = "name";
    
    /**
     * XML reference type attribute name
     */
    public static final String TYPE_TAG = "type";

    private String name;
    private String fullPath;
    private ViewType type = ViewType.UNKNOWN;
    private boolean foundByName;

    /**
     * Gets the name of this ViewReference, which is essentially the file name
     * portion of the view file less the .xml extension.
     * 
     * @return the view name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the name of this ViewReference, which is essentially the file name
     * portion of the view file less the .xml extension.
     * 
     * @param name the name to set
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Gets the file path for this ViewReference; may be null if the view is
     * found by using the name and traversing the project search path.
     * 
     * @return the file path
     */
    public String getPath() {
        return this.fullPath;
    }

    /**
     * Sets the file path for this ViewReference; may be null if the view is
     * found by using the name and traversing the project view search path.
     * 
     * @param fullPath the file path to set
     */
    public void setPath(final String fullPath) {
        this.fullPath = fullPath;
    }

    /**
     * Gets the type of the view referenced by this ViewReference. Note this is
     * only known for sure after the reference is resolved (loaded).
     * 
     * @return the ViewType
     */
    public ViewType getType() {
        return this.type;
    }

    /**
     * Gets the type of the view referenced by this ViewReference. Note this is
     * only known for sure after the reference is resolved (loaded).
     * 
     * @param type the ViewType of the view referenced by this object
     */
    public void setType(final ViewType type) {
        this.type = type;
    }

    /**
     * Returns an XML representation of this ViewReference.
     * 
     * @return the XML string
     */
    public String toXml() {
        final StringBuilder result = new StringBuilder();
        result.append("<" + VIEW_REFERENCE_TAG + " ");
        result.append(IViewConfiguration.VIEW_VERSION_TAG + "=\""
                + IViewConfiguration.WRITE_VERSION + "\" ");
        if (this.type != null) {
            result.append(TYPE_TAG + "=\"" + this.type + "\" ");
        }
        result.append(NAME_TAG + "=\"" + this.name + "\" ");
        if (this.fullPath != null && !this.foundByName) {
            result.append(PATH_TAG + "=\"" + this.fullPath + "\"");
        }
        result.append("/>\n");
        return result.toString();
    }

    /**
     * Parses the view file referenced by this object and returns the
     * ViewConfiguration resulting from the parse. It is assumed there is only
     * one top-level view in the file. If there are more, only the first is
     * returned. If the file path is null, the name is used to search the
     * project view search path for a view file with the matching name.
     * 
     * @return the parsed ViewConfiguration, or null if none found or the file
     *         could not be parsed
     */
    public IViewConfiguration parse(final ApplicationContext appContext) {
        String path = this.fullPath;

        if (this.fullPath == null && this.name == null) {
            throw new IllegalStateException(
                    "Cannot parse the referenced view; both name and path are null");
        }

        if (this.fullPath == null) {
            path = findReferencedView(appContext.getBean(PerspectiveProperties.class),
                                      appContext.getBean(SseContextFlag.class));
            this.foundByName = true;
        }
        if (path == null) {
            TraceManager.getDefaultTracer(appContext).warn(

                    "View " + this.name
                            + " cannot be located on the project search path");
            return null;
        }
        final List<IViewConfiguration> parsedViews = IViewConfiguration
                .load(appContext, path);
        if (parsedViews == null || parsedViews.isEmpty()) {
            return null;
        }
        final IViewConfiguration vc = parsedViews.get(0);
        this.type = vc.getViewType();
        this.name = ViewScanner.createNameFromPath(path);
        this.fullPath = path;
        vc.setViewReference(this);
        return vc;
    }

    /**
     * Parses the view file referenced by this object and returns the
     * ViewConfiguration resulting from the parse. It is assumed there is only
     * one top-level view in the file. If there are more, only the first is
     * returned. If the file path is null, the name is used to search the
     * project view search path for a view file with the matching name.
     * 
     * @return the parsed ViewConfiguration, or null if none found or the file
     *         could not be parsed
     * @throws IOException if reading the view configuration file results in IO error
     * @throws SAXException if the view configuration XML cannot be parsed
     * @throws ParserConfigurationException  if the XML parser cannot be configured
     */
    public IViewConfiguration parseAndThrow(final ApplicationContext appContext) 
    		throws ParserConfigurationException, SAXException, IOException {
        String path = this.fullPath;

        if (this.fullPath == null && this.name == null) {
            throw new IllegalStateException(
                    "Cannot parse the referenced view; both name and path are null");
        }

        if (this.fullPath == null) {
            path = findReferencedView(appContext.getBean(PerspectiveProperties.class),
                                      appContext.getBean(SseContextFlag.class));
            this.foundByName = true;
        }
        if (path == null) {
            TraceManager.getDefaultTracer().warn(

                    "View " + this.name
                            + " cannot be located on the project search path");
            return null;
        }
        final List<IViewConfiguration> parsedViews = IViewConfiguration
                .loadAndThrow(appContext, path);
        if (parsedViews == null || parsedViews.size() < 1) {
            return null;
        }
        final IViewConfiguration vc = parsedViews.get(0);
        this.type = vc.getViewType();
        this.name = ViewScanner.createNameFromPath(path);
        this.fullPath = path;
        vc.setViewReference(this);
        return vc;
    }

    private String findReferencedView(final PerspectiveProperties perspectiveProperties, final SseContextFlag sseFlag) {
        final List<String> views = ViewScanner.getConfiguredViewPaths(perspectiveProperties, sseFlag);
        if (views == null) {
            return null;
        }
        for (final String view : views) {
            final File viewFile = new File(view);
            final String viewName = viewFile.getName().substring(0,
                    viewFile.getName().length() - 4);
            if (viewName.equalsIgnoreCase(this.name)) {
                return view;
            }
        }
        return null;
    }

    /**
     * Sets the flag indicating whether the view referenced by this object was
     * found by name search rather than file path.
     * 
     * @param found
     *            true to indicate reference is by name
     */
    public void setFoundByName(final boolean found) {
        this.foundByName = found;
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     * 
     * NOTE:Comparison is on the name member.
     */
    @Override
	public int compareTo(final ViewReference vr) {
        if (vr == null) {
            return 1;
        }
        final ViewReference other = vr;
        if (this.name == null && other.name == null) {
            return 0;

        } else if (this.name == null) {
            return -1;
        } else if (other.name == null) {
            return 1;
        }
        final int compare = this.name.compareTo(other.name);

        return (compare);
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object vr) {
        if (!(vr instanceof ViewReference)) {
            return false;
        }
        return compareTo((ViewReference) vr) == 0;
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        if (this.name == null) {
            return 0;
        } else {
            return this.name.hashCode();
        }
    }

}
