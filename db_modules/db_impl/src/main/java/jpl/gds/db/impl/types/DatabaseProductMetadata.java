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
package jpl.gds.db.impl.types;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import jpl.gds.db.api.types.IDbProductMetadataUpdater;
import jpl.gds.product.api.IProductMetadataUpdater;
import jpl.gds.product.api.IProductPartProvider;
import jpl.gds.product.api.ProductException;
import jpl.gds.product.api.ProductStatusType;
import jpl.gds.product.api.builder.IProductBuilderObjectFactory;
import jpl.gds.shared.holders.SessionFragmentHolder;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ICoarseFineTime;
import jpl.gds.shared.time.ILocalSolarTime;


public class DatabaseProductMetadata extends AbstractDatabaseItem
        implements IDbProductMetadataUpdater {
    /**
     * The object to wrap and delegate all method calls to
     */
    protected final IProductMetadataUpdater md;

    /**
     * @param appContext
     *            the Spring Application Context
     */
    public DatabaseProductMetadata(final ApplicationContext appContext) {
        this(appContext, appContext.getBean(IProductBuilderObjectFactory.class).createMetadataUpdater());
    }

    /**
     * @param appContext
     *            the Spring Application Context
     * @param md
     *            the IProductMetadataUpdater to wrap and delegate to.
     */
    public DatabaseProductMetadata(final ApplicationContext appContext, final IProductMetadataUpdater md) {
        super(appContext);
        if (null == md) {
            throw new IllegalArgumentException("Cannot initialize a DatabaseProductMetadata object with null.");
        }
        this.md = md;
    }

    /**
     * @param appContext
     *            the Spring Application Context
     * @param md
     *            the IProductMetadataUpdater object to wrap and delegate to.
     * @param contextId
     *            the context ID
     * @param fragment
     *            the Session Fragment
     * @param sessionHost
     *            the Database Host for this session
     */
    public DatabaseProductMetadata(final ApplicationContext appContext, final IProductMetadataUpdater md,
            final Long contextId, final SessionFragmentHolder fragment, final String sessionHost) {
        super(appContext,
              contextId,
              fragment,
              sessionHost);
        if (null == md) {
            throw new IllegalArgumentException("Cannot initialize a DatabaseProductMetadata object with null.");
        }
        this.md = md;
    }

    /**
     * @param appContext
     *            the Spring Application Context
     * @param md
     *            the IProductMetadataUpdater object to wrap and delegate to.
     * @param contextId
     *            the context ID
     * @param sessionHost
     *            the Database Host for this session
     */
    public DatabaseProductMetadata(final ApplicationContext appContext, final IProductMetadataUpdater md,
            final Long contextId, final String sessionHost) {
        super(appContext,
              contextId,
              sessionHost);
        if (null == md) {
            throw new IllegalArgumentException("Cannot initialize a DatabaseProductMetadata object with null.");
        }
        this.md = md;
    }

    @Override // IProductMetadataProvider
    public void setFswVersion(long fswVersion) {
        md.setFswVersion(fswVersion);
    }

    @Override // IProductMetadataProvider
    public long getFswVersion() { return md.getFswVersion();}

    @Override // IProductMetadataProvider
    public void setFswDictionaryDir(String fswDictionaryDir) {
        md.setFswDictionaryDir(fswDictionaryDir);
    }

    @Override // IProductMetadataProvider
    public String getFswDictionaryDir() { return md.getFswDictionaryDir(); }

    @Override // IProductMetadataProvider
    public void setFswDictionaryVersion(String fswDictionaryVersion) {
        md.setFswDictionaryVersion(fswDictionaryVersion);
    }

    @Override // IProductMetadataProvider
    public String getFswDictionaryVersion() { return md.getFswDictionaryVersion(); }

    @Override // IProductMetadataProvider
    public String getCsvHeader(final List<String> csvColumns) {
        return md.getCsvHeader(csvColumns);
    }

    @Override // IProductMetadataProvider
    public String toCsv(final List<String> csvColumns) {
        return md.toCsv(csvColumns);
    }

    @Override // IProductMetadataProvider
    public void parseCsv(final String csvStr, final List<String> csvColumns) {
        md.parseCsv(csvStr, csvColumns);
    }

    @Override // IProductMetadataProvider
    public String toString() {
        return md.toString();
    }

    @Override // IProductMetadataProvider
    public Integer getVcid() {
        return md.getVcid();
    }

    @Override // IProductMetadataProvider
    public int getScid() {
        return md.getScid();
    }

    @Override // IProductMetadataProvider
    public Long getSessionId() {
        return md.getSessionId();
    }

    @Override // IProductMetadataProvider
    public SessionFragmentHolder getSessionFragment() {
        return md.getSessionFragment();
    }

    @Override // IProductMetadataProvider
    public Integer getSessionDssId() {
        return md.getSessionDssId();
    }

    @Override // IProductMetadataProvider
    public Integer getSessionVcid() {
        return md.getSessionVcid();
    }

    @Override // IProductMetadataProvider
    public int getApid() {
        return md.getApid();
    }

    @Override // IProductMetadataProvider
    public IAccurateDateTime getScet() {
        return md.getScet();
    }

    @Override // IProductMetadataProvider
    public ILocalSolarTime getSol() {
        return md.getSol();
    }

    @Override // IProductMetadataProvider
    public long getScetExact() {
        return md.getScetExact();
    }

    @Override // IProductMetadataProvider
    public long getSolExact() {
        return md.getSolExact();
    }

    @Override // IProductMetadataProvider
    public String getScetStr() {
        return md.getScetStr();
    }

    @Override // IProductMetadataProvider
    public String getRctStr() {
        return md.getRctStr();
    }

    @Override // IProductMetadataProvider
    public String getSolStr() {
        return md.getSolStr();
    }

    @Override // IProductMetadataProvider
    public String getProductCreationTimeStr() {
        return md.getProductCreationTimeStr();
    }

    @Override // IProductMetadataProvider
    public String getErtStr() {
        return md.getErtStr();
    }

    @Override // IProductMetadataProvider
    public long getErtExact() {
        return md.getErtExact();
    }

    @Override // IProductMetadataProvider
    public long getErtExactFine() {
        return md.getErtExactFine();
    }

    @Override // IProductMetadataProvider
    public ICoarseFineTime getSclk() {
        return md.getSclk();
    }

    @Override // IProductMetadataProvider
    public long getSclkCoarse() {
        return md.getSclkCoarse();
    }

    @Override // IProductMetadataProvider
    public long getSclkFine() {
        return md.getSclkFine();
    }

    @Override // IProductMetadataProvider
    public long getSclkExact() {
        return md.getSclkExact();
    }

    @Override // IProductMetadataProvider
    public String getSclkStr() {
        return md.getSclkStr();
    }

    @Override // IProductMetadataProvider
    public String getStorageDirectory() {
        return md.getStorageDirectory();
    }

    @Override // IProductMetadataProvider
    public String getFullPath() {
        return md.getFullPath();
    }

    @Override // IProductMetadataProvider
    public Map<String, String> getMissionProperties() {
        return md.getMissionProperties();
    }

    @Override // IProductMetadataProvider
    public String getAbsoluteDataFile() {
        return md.getAbsoluteDataFile();
    }

    @Override // IProductMetadataProvider
    public int getSequenceId() {
        return md.getSequenceId();
    }

    @Override // IProductMetadataProvider
    public int getSequenceVersion() {
        return md.getSequenceVersion();
    }

    @Override // IProductMetadataProvider
    public int getCommandNumber() {
        return md.getCommandNumber();
    }

    @Override // IProductMetadataProvider
    public int getXmlVersion() {
        return md.getXmlVersion();
    }

    @Override // IProductMetadataProvider
    public long getDvtCoarse() {
        return md.getDvtCoarse();
    }

    @Override // IProductMetadataProvider
    public long getDvtFine() {
        return md.getDvtFine();
    }

    @Override // IProductMetadataProvider
    public String getDvtString() {
        return md.getDvtString();
    }

    @Override // IProductMetadataProvider
    public int getTotalParts() {
        return md.getTotalParts();
    }

    @Override // IProductMetadataProvider
    public IProductPartProvider getLastPart() {
        return md.getLastPart();
    }

    @Override // IProductMetadataProvider
    public Iterator<IProductPartProvider> partIterator() {
        return md.partIterator();
    }

    @Override // IProductMetadataProvider
    public List<IProductPartProvider> getPartList() {
        return md.getPartList();
    }

    @Override // IProductMetadataProvider
    public IAccurateDateTime getProductCreationTime() {
        return md.getProductCreationTime();
    }

    @Override // IProductMetadataProvider
    public IAccurateDateTime getRct() {
        return md.getRct();
    }

    @Override // IProductMetadataProvider
    public boolean isPartial() {
        return md.isPartial();
    }

    @Override // IProductMetadataProvider
    public String getSessionHost() {
        return md.getSessionHost();
    }

    @Override // IProductMetadataProvider
    public Integer getSessionHostId() {
        return md.getSessionHostId();
    }

    @Override // IProductMetadataProvider
    public String getFilenameWithPrefix() {
        return md.getFilenameWithPrefix();
    }

    @Override // IProductMetadataProvider
    public String getFilename() {
        return md.getFilename();
    }

    @Override // IProductMetadataProvider
    public String getDirectoryName() {
        return md.getDirectoryName();
    }

    @Override // IProductMetadataProvider
    public long getCommandId() {
        return md.getCommandId();
    }

    @Override // IProductMetadataProvider
    public IAccurateDateTime getErt() {
        return md.getErt();
    }

    @Override // IProductMetadataProvider
    public ProductStatusType getGroundStatus() {
        return md.getGroundStatus();
    }

    @Override // IProductMetadataProvider
    public String getSequenceCategory() {
        return md.getSequenceCategory();
    }

    @Override // IProductMetadataProvider
    public Long getSequenceNumber() {
        return md.getSequenceNumber();
    }

    @Override // IProductMetadataProvider
    public String getProductType() {
        return md.getProductType();
    }

    @Override // IProductMetadataProvider
    public boolean productTypeIsValid() {
        return md.productTypeIsValid();
    }

    @Override // IProductMetadataProvider
    public String getEmdDictionaryDir() {
        return md.getEmdDictionaryDir();
    }

    @Override // IProductMetadataProvider
    public String getEmdDictionaryVersion() {
        return md.getEmdDictionaryVersion();
    }

    @Override // IProductMetadataProvider
    public String toXml() {
        return md.toXml();
    }

    @Override // IProductMetadataProvider
    public Map<String, Object> getAdditionalMissionData() {
        return md.getAdditionalMissionData();
    }

    @Override // IProductMetadataProvider
    public String getProductVersion() {
        return md.getProductVersion();
    }

    @Override // IProductMetadataProvider
    public String getTransformedVcid() {
        return md.getTransformedVcid();
    }

    @Override // IProductMetadataProvider
    public int getTransformedStringId(final String str) {
        return md.getTransformedStringId(str);
    }

    @Override // IProductMetadataProvider
    public long getChecksum() {
        return md.getChecksum();
    }

    @Override // IProductMetadataProvider
    public long getFileSize() {
        return md.getFileSize();
    }

    @Override // IProductMetadataProvider
    public long getActualChecksum() {
        return md.getActualChecksum();
    }

    @Override // IProductMetadataProvider
    public long getActualFileSize() {
        return md.getActualFileSize();
    }

    @Override // IProductMetadataProvider
    public Map<String, String> getFileData(final String NO_DATA) {
        return md.getFileData(NO_DATA);
    }

    @Override // IProductMetadataUpdater
    public void setVcid(final Integer vcid) {
        md.setVcid(vcid);
    }

    @Override // IProductMetadataUpdater
    public void setScid(final int scid) {
        md.setScid(scid);
    }

    @Override // IProductMetadataUpdater
    public void setSessionId(final Long testIdKey) {
        md.setSessionId(testIdKey);
    }

    @Override // IProductMetadataUpdater
    public void setSessionFragment(final SessionFragmentHolder fragment) {
        md.setSessionFragment(fragment);
    }

    @Override // IProductMetadataUpdater
    public void setSessionDssId(final Integer dssId) {
        md.setSessionDssId(dssId);
    }

    @Override // IProductMetadataUpdater
    public void setSessionVcid(final Integer vcid) {
        md.setSessionVcid(vcid);
    }

    @Override // IProductMetadataUpdater
    public void setApid(final int apid) {
        md.setApid(apid);
    }

    @Override // IProductMetadataUpdater
    public void setScet(final IAccurateDateTime scet) {
        md.setScet(scet);
    }

    @Override // IProductMetadataUpdater
    public void setScet(final String scetStr) throws SAXException {
        md.setScet(scetStr);
    }

    @Override // IProductMetadataUpdater
    public void setSol(final ILocalSolarTime sol) {
        md.setSol(sol);
    }

    @Override // IProductMetadataUpdater
    public void setSclk(final ICoarseFineTime sclk) {
        md.setSclk(sclk);
    }

    @Override // IProductMetadataUpdater
    public void setStorageDirectory(final String storageDirectory) {
        md.setStorageDirectory(storageDirectory);
    }

    @Override // IProductMetadataUpdater
    public void setFullPath(final String filenameNoSuffix) {
        md.setFullPath(filenameNoSuffix);
    }

    @Override // IProductMetadataUpdater
    public void setTemplateContext(final Map<String, Object> map) {
        md.setTemplateContext(map);
    }

    @Override // IProductMetadataUpdater
    public boolean parseFromElement(final String elemName, final String text) {
        return md.parseFromElement(elemName, text);
    }

    @Override // IProductMetadataUpdater
    public void setSequenceId(final int seq) {
        md.setSequenceId(seq);
    }

    @Override // IProductMetadataUpdater
    public void setSequenceVersion(final int seqVersion) {
        md.setSequenceVersion(seqVersion);
    }

    @Override // IProductMetadataUpdater
    public void setCommandNumber(final int commandNum) {
        md.setCommandNumber(commandNum);
    }

    @Override // IProductMetadataUpdater
    public void setXmlVersion(final int xmlVersion) {
        md.setXmlVersion(xmlVersion);
    }

    @Override // IProductMetadataUpdater
    public void setDvtCoarse(final long dvtCoarse) {
        md.setDvtCoarse(dvtCoarse);
    }

    @Override // IProductMetadataUpdater
    public void setDvtFine(final long dvtFine) {
        md.setDvtFine(dvtFine);
    }

    @Override // IProductMetadataUpdater
    public void setTotalParts(final int totalParts) {
        md.setTotalParts(totalParts);
    }

    @Override // IProductMetadataUpdater
    public void setProductCreationTime(final IAccurateDateTime productCreationTime) {
        md.setProductCreationTime(productCreationTime);
    }

    @Override // IProductMetadataUpdater
    public void setRct(final IAccurateDateTime rct) {
        md.setRct(rct);
    }

    @Override // IProductMetadataUpdater
    public void setPartial(final boolean partial) {
        md.setPartial(partial);
    }

    @Override // IProductMetadataUpdater
    public void setSessionHost(final String testHost) {
        md.setSessionHost(testHost);
    }

    @Override // IProductMetadataUpdater
    public void setSessionHostId(final Integer hostId) {
        md.setSessionHostId(hostId);
    }

    @Override // IProductMetadataUpdater
    public void addPart(final IProductPartProvider part) {
        md.addPart(part);
    }

    @Override // IProductMetadataUpdater
    public void setErt(final IAccurateDateTime ert) {
        md.setErt(ert);
    }

    @Override // IProductMetadataUpdater
    public void setGroundStatus(final ProductStatusType groundStatus) {
        md.setGroundStatus(groundStatus);
    }

    @Override // IProductMetadataUpdater
    public void setSequenceCategory(final String sc) {
        md.setSequenceCategory(sc);
    }

    @Override // IProductMetadataUpdater
    public void setSequenceNumber(final Long sn) {
        md.setSequenceNumber(sn);
    }

    @Override // IProductMetadataUpdater
    public void setSclk(final String sclkStr) throws SAXException {
        md.setSclk(sclkStr);
    }

    @Override // IProductMetadataUpdater
    public void setProductType(final String productType) {
        md.setProductType(productType);
    }

    @Override // IProductMetadataUpdater
    public void setEmdDictionaryDir(final String emdDictionaryDir) {
        md.setEmdDictionaryDir(emdDictionaryDir);
    }

    @Override // IProductMetadataUpdater
    public void setEmdDictionaryVersion(final String emdDictionaryVersion) {
        md.setEmdDictionaryVersion(emdDictionaryVersion);
    }


    @Override // IProductMetadataUpdater
    public void setProductVersion(final String productVersion) {
        md.setProductVersion(productVersion);
    }

    @Override // IProductMetadataUpdater
    public void setChecksum(final long checksum) {
        md.setChecksum(checksum);
    }

    @Override // IProductMetadataUpdater
    public void setFileSize(final long fileSize) {
        md.setFileSize(fileSize);
    }

    @Override // IProductMetadataUpdater
    public void setActualChecksum(final long checksum) {
        md.setActualChecksum(checksum);
    }

    @Override // IProductMetadataUpdater
    public void setActualFileSize(final long fileSize) {
        md.setActualFileSize(fileSize);
    }

    @Override // IProductMetadataUpdater
    public void setFilename(final String filename) {
        md.setFilename(filename);
    }

    @Override
    public void generateStaxXml(final XMLStreamWriter writer) throws XMLStreamException {
        md.generateStaxXml(writer);
    }

	@Override
	public void loadFile(final String filename) throws ProductException {
		md.loadFile(filename);
	}
}