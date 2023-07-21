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
package jpl.gds.shared.metadata.context;

import jpl.gds.shared.metadata.ISerializableMetadata;
import jpl.gds.shared.metadata.MetadataMap;
import jpl.gds.shared.util.HostPortUtility;

import java.util.Map;

/**
 * An object that implements the IContextKey interface, for use as a key in
 * metadata and message objects.
 *
 * @since R8
 */
public class ContextKey implements IContextKey {

    private Long number;
    private Long parentNumber;
    private String host = HostPortUtility.getLocalHostName();
    private Integer hostId;
    private Integer parentHostId;
    private Integer fragment = 1;
    private ISerializableMetadata header;
    private boolean dirty = true;
    private ContextConfigurationType type;

    /**
     * Constructor.
     */
    public ContextKey() {

    }

    @Override
    public Long getNumber() {
        return number;
    }

    @Override
    public Long getParentNumber() {
        return parentNumber;
    }

    @Override
    public void setNumber(final Long number) {
        this.number = number;
        dirty = true;
    }

    @Override
    public void setParentNumber(final Long number) {
        this.parentNumber = number;
        dirty = true;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public void setHost(final String host) {
        this.host = host;
        dirty = true;
    }

    @Override
    public Integer getHostId() {
        return hostId;
    }

    @Override
    public void setHostId(final Integer hostId) {
        this.hostId = hostId;
        dirty = true;
    }

    @Override
    public Integer getParentHostId() {
        return parentHostId;
    }

    @Override
    public void setParentHostId(final Integer parentHostId) {
        this.parentHostId = parentHostId;
        dirty = true;
    }

    @Override
    public Integer getFragment() {
        return fragment;
    }

    @Override
    public void setFragment(final Integer fragment) {
        this.fragment = fragment;
        dirty = true;
    }

    @Override
    public void clearFieldsForNewConfiguration() {
        setNumber(null);
        setHostId(null);
        setParentNumber(null);
        setParentHostId(null);
        setHost(HostPortUtility.getLocalHostName());
        setFragment(1);
    }

    @Override
    public void copyValuesFrom(final IContextKey ck) {
        if (this == ck) {
            return;
        }
        setNumber(ck.getNumber());
        setParentNumber(ck.getParentNumber());
        setParentHostId(ck.getParentHostId());
        setHost(ck.getHost());
        setHostId(ck.getHostId());
        setFragment(ck.getFragment());
        setType(ck.getType());
    }

    @Override
    public String getContextId() {
        return (getNumber() == null ? "0" : getNumber()) +
                ID_SEPARATOR + (getHost() == null ? "unknown" : getHost()) +
                ID_SEPARATOR + (getHostId() == null ? "0" : getHostId()) +
                ID_SEPARATOR + (getFragment() == null ? "1" : getFragment()) +
                ID_SEPARATOR + (getParentNumber() == null ? "0" : getParentNumber()) +
                ID_SEPARATOR + (getParentHostId() == null ? "0" : getParentHostId());
    }

    @Override
    public int getShortContextId() {
        return Integer.valueOf(getContextId().substring(0, getContextId().indexOf(ID_SEPARATOR)));
    }

    @Override
    public ISerializableMetadata getMetadataHeader() {
        if (header == null || dirty) {
            header = new MetadataMap(this);
        }
        dirty = false;
        return header;
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    @Override
    public boolean equals(final Object o) {
        // removed hostId
        if (!(o instanceof IContextKey)) {
            return false;
        }
        final IContextKey other = (IContextKey) o;
        if (!number.equals(other.getNumber())) {
            return false;
        }
        if (!fragment.equals(other.getFragment())) {
            return false;
        }
        if (host != null && other.getHost() != null && host.equals(other.getHost())) {
            return true;
        }
        return (host == null && other.getHost() == null);
    }

    @Override
    public int hashCode() {
        //removed hostId
        int result = 17;
        if (number != null) {
            result = 31 * result + number.hashCode();
        }
        if (fragment != null) {
            result = 31 * result + fragment.hashCode();
        }
        if (host != null) {
            result = 31 * result + host.hashCode();
        }
        return result;
    }

    @Override
    public void setTemplateContext(final Map<String, Object> map) {

        final Long tsi = number != null ? number : 0;

        map.put("sessionId", tsi);
        map.put("testSessionId", tsi); //  deprecated for R8
        map.put("testNumber", tsi); //  deprecated for R8
        map.put("sessionNumber", tsi); //  deprecated for R8
        map.put("sessionKey", tsi); // deprecated for R8

        final Integer frag = fragment != null ? fragment : 1;

        map.put("sessionFragment", frag);

        map.put("sessionHost", host);
        map.put("testSessionHost", host); //  deprecated for R8

        map.put("hostId", hostId);

    }

    @Override
    public ContextConfigurationType getType() {
        return type;
    }

    @Override
    public void setType(ContextConfigurationType type) {
        this.type = type;
    }

    @Override
    public String toString() {

		/*
		Instead of returning getContextId() which contains parent information (which is
		only going to confuse users when they see it), return a simple string of "<number>/<host>/<fragment>"
		 */
        return (getNumber() == null ? "0" : getNumber()) +
                ID_SEPARATOR + (getHost() == null ? "unknown" : getHost()) +
                ID_SEPARATOR + (getFragment() == null ? "1" : getFragment());
    }
}