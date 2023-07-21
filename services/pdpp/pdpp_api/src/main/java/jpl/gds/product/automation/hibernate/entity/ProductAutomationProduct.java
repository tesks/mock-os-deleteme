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
package jpl.gds.product.automation.hibernate.entity;

import java.sql.Timestamp;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.SortNatural;

/**
 * Entity class for product automation product table.
 * 
 * MPCS-8179 - 06/07/16 - Added to AMPCS, updated from original version in MPCS for MSL G9.
 * MPCS-8568 - 12/12/16 - Added fswDirectory property. Added get and set methods
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "products", uniqueConstraints = @UniqueConstraint(columnNames = "productPath"))

public class ProductAutomationProduct implements java.io.Serializable, 
	Comparable<ProductAutomationProduct> {

	private static final long serialVersionUID = -8760293388579850634L;
	// Fields

	/**
	 * MPCS-6468  8/2014 - Real time extraction flag is no longer used.  Also adding the
	 * is compressed flag so that all information required for categorizing required product processing
	 * is stored in the product table.  Removing the extraction objects and adding the isCompressed references.
	 */
	private Long productId;
	private String productPath;
	private ProductAutomationProduct parent;
	private Long fswBuildId;
	private String dictVersion;
	private String fswDirectory;
	private Long sessionId;
	private String sessionHost;
	private Integer apid;
	private Integer vcid;
	private Long sclkCoarse;
	private Long sclkFine;
	private int isCompressed;
	private int realTimeExtraction;
	private Timestamp addTime;
	
	private SortedSet<ProductAutomationStatus> statuses;
	private SortedSet<ProductAutomationAction> actions;
	private SortedSet<ProductAutomationLog> logs;

	// Constructors

	/** default constructor */
	public ProductAutomationProduct() {
	}

	/**
	 * minimal constructor
	 * 
	 * @param productPath
	 *            the file path for the product file
	 * @param fswBuildId
	 *            the flight software ID for the dictionary to be used on this
	 *            product
	 * @param dictVersion
	 *            the String of the dictioanry version to be used on this
	 *            product
	 * @param fswDirectory 
	 * @param sessionId
	 *            the session that produced this product
	 * @param sessionHost
	 *            the name of the host machine that produced this product
	 * @param apid
	 *            the application process ID of this product
	 * @param vcid
	 *            the virtual channel ID of this product
	 * @param sclkCoarse
	 *            the coarse spacecraft clock time associated with this product
	 * @param sclkFine
	 *            the fine spacecraft clock time associated with this product
	 * @param isCompressed
	 *            1 if compressed, 0 if not
	 * @param realTimeExtraction
	 *            1 if processed by chill_down, 0 if not
	 */
	public ProductAutomationProduct(String productPath, Long fswBuildId,
			String dictVersion, String fswDirectory, Long sessionId, String sessionHost,
			Integer apid, Integer vcid, Long sclkCoarse, Long sclkFine, int isCompressed, int realTimeExtraction) {
		this.productPath = productPath;
		this.fswBuildId = fswBuildId;
		this.dictVersion = dictVersion;
		this.fswDirectory = fswDirectory;
		this.sessionId = sessionId;
		this.sessionHost = sessionHost;
		this.apid = apid;
		this.vcid = vcid;
		this.sclkCoarse = sclkCoarse;
		this.sclkFine = sclkFine;
		this.isCompressed = isCompressed;
		this.realTimeExtraction = realTimeExtraction;
		this.statuses = new TreeSet<ProductAutomationStatus>();
		this.actions = new TreeSet<ProductAutomationAction>();
		this.logs = new TreeSet<ProductAutomationLog>();
	}
	
	/**
	 * full constructor
	 * 
	 * @param productPath
	 *            the file path for the product file
	 * @param parent
	 *            the ProductAutomationProduct this product came from
	 * @param fswBuildId
	 *            the flight software ID for the dictionary to be used on this
	 *            product
	 * @param dictVersion
	 *            the String of the dictionary version to be used on this
	 *            product
	 * @param fswDirectory
	 *            the directory of the flight software dictionary to be used
	 *            with this process
	 * @param sessionId
	 *            the session that produced this product
	 * @param sessionHost
	 *            the name of the host machine that produced this product
	 * @param apid
	 *            the application process ID of this product
	 * @param vcid
	 *            the virtual channel ID of this product
	 * @param sclkCoarse
	 *            the coarse spacecraft clock time associated with this product
	 * @param sclkFine
	 *            the fine spacecraft clock time associated with this product
	 * @param isCompressed
	 *            1 if compressed, 0 if not
	 * @param realTimeExtraction
	 *            1 if processed by chill_down, 0 if not
	 * @param addTime
	 *            the Timestamp when this product was added
	 * @param statuses
	 *            the SortedSet of ProductAutomationStatus that are and have
	 *            been associated with this product
	 * @param actions
	 *            the SortedSet of ProductAutomationActions that are and have
	 *            been associated with this product
	 * @param logs
	 *            the SortedSet of ProductAutomationLogs that have been
	 *            associated with this product
	 */
	public ProductAutomationProduct(String productPath, ProductAutomationProduct parent,
			Long fswBuildId, String dictVersion, String fswDirectory, Long sessionId,
			String sessionHost, Integer apid, Integer vcid, Long sclkCoarse,
			Long sclkFine, int isCompressed, int realTimeExtraction, Timestamp addTime, 
			SortedSet<ProductAutomationStatus> statuses, SortedSet<ProductAutomationAction> actions,
			SortedSet<ProductAutomationLog> logs) {
		this.productPath = productPath;
		this.parent = parent;
		this.fswBuildId = fswBuildId;
		this.dictVersion = dictVersion;
		this.fswDirectory = fswDirectory;
		this.sessionId = sessionId;
		this.sessionHost = sessionHost;
		this.apid = apid;
		this.vcid = vcid;
		this.sclkCoarse = sclkCoarse;
		this.sclkFine = sclkFine;
		this.realTimeExtraction = realTimeExtraction;
		this.isCompressed = isCompressed;
		this.addTime = addTime;
		this.statuses = statuses;
		this.actions = actions;
		this.logs = logs;
	}

	/**
	 * Run time constructor.
	 * 
	 * @param productPath
	 *            the file path for the product file
	 * @param parent
	 *            the ProductAutomationProduct this product came from
	 * @param fswBuildId
	 *            the flight software ID for the dictionary to be used on this
	 *            product
	 * @param dictVersion
	 *            the String of the dictionary version to be used on this
	 *            product
	 * @param fswDirectory
	 *            the directory of the flight software dictionary to be used
	 *            with this process
	 * @param sessionId
	 *            the session that produced this product
	 * @param sessionHost
	 *            the name of the host machine that produced this product
	 * @param apid
	 *            the application process ID of this product
	 * @param vcid
	 *            the virtual channel ID of this product
	 * @param sclkCoarse
	 *            the coarse spacecraft clock time associated with this product
	 * @param sclkFine
	 *            the fine spacecraft clock time associated with this product
	 * @param isCompressed
	 *            1 if compressed, 0 if not
	 * @param realTimeExtraction
	 *            1 if processed by chill_down, 0 if not
	 */
	public ProductAutomationProduct(String productPath, ProductAutomationProduct parent,
			Long fswBuildId, String dictVersion, String fswDirectory, Long sessionId,
			String sessionHost, Integer apid, Integer vcid, Long sclkCoarse,
			Long sclkFine, int isCompressed, int realTimeExtraction){
		this.productPath = productPath;
		this.parent = parent;
		this.fswBuildId = fswBuildId;
		this.dictVersion = dictVersion;
		this.fswDirectory = fswDirectory;
		this.sessionId = sessionId;
		this.sessionHost = sessionHost;
		this.apid = apid;
		this.vcid = vcid;
		this.sclkCoarse = sclkCoarse;
		this.sclkFine = sclkFine;
		this.isCompressed = isCompressed;
		this.realTimeExtraction = realTimeExtraction;
		this.statuses = new TreeSet<ProductAutomationStatus>();
		this.actions = new TreeSet<ProductAutomationAction>();
		this.logs = new TreeSet<ProductAutomationLog>();
	}

	// Property accessors
	/**
	 * Get the product ID for this object.
	 * 
	 * @return the Long value that uniquely identifies this product
	 */
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name = "productId", unique = true, nullable = false)
	public Long getProductId() {
		return this.productId;
	}

	/**
	 * Set the product ID for this object.
	 * 
	 * @param productId
	 *            a Long value that will uniquely identify this product
	 */
	public void setProductId(Long productId) {
		this.productId = productId;
	}

	/**
	 * Get the file path of the product file.
	 * 
	 * @return a full file path to the product file
	 */
	@Column(name = "productPath", nullable = false, unique = true, length = 1024)
	public String getProductPath() {
		return this.productPath;
	}

	/**
	 * Set the product file path.
	 * 
	 * @param productPath
	 *            the absolute, not relative, file path to the product file
	 */
	public void setProductPath(String productPath) {
		this.productPath = productPath;
	}

	/**
	 * Get the parent product.
	 * 
	 * @return the ProductAutomationProduct that this product was derived
	 */
	@ManyToOne
	@JoinColumn(name = "parent")
	public ProductAutomationProduct getParent() {
		return this.parent;
	}

	/**
	 * Set the parent product.
	 * 
	 * @param parent
	 *            the ProductAutomationProduct that was used to create this
	 *            product
	 */
	public void setParent(ProductAutomationProduct parent) {
		this.parent = parent;
	}

	/**
	 * Get the flight software build ID for the associated dictionary.
	 * 
	 * @return the flight software build ID
	 */
	@Column(name = "fswBuildId", nullable = false)
	public Long getFswBuildId() {
		return this.fswBuildId;
	}

	/**
	 * Set the flight software build ID for the associated dictionary.
	 * 
	 * @param fswBuildId
	 *            the flight software build ID
	 */
	public void setFswBuildId(Long fswBuildId) {
		this.fswBuildId = fswBuildId;
	}

	/**
	 * Get the full dictionary version.
	 * 
	 * @return the String dictionary version
	 */
	@Column(name = "dictVersion", nullable = false, length = 256)
	public String getDictVersion() {
		return this.dictVersion;
	}

	/**
	 * Set the dictionary version. This value should be in the format
	 * "R0_0_0_00000000_00".
	 * 
	 * @param dictVersion
	 *            The String dictionary version
	 */
	public void setDictVersion(String dictVersion) {
		this.dictVersion = dictVersion;
	}
	
	/**
	 * Get the flgiht software directory
	 * 
	 * @return the directory where the flight software dictionary is located
	 */
	@Column(name = "fswDirectory", nullable = false, length = 1024)
	public String getFswDirectory(){
		return this.fswDirectory;
	}
	
	/**
	 * Set the flight software directory
	 * 
	 * @param fswDirectory the directory where the flight software dictionary is located
	 */
	public void setFswDirectory(String fswDirectory){
		this.fswDirectory = fswDirectory;
	}

	/**
	 * Get the session ID that generated this product.
	 * 
	 * @return the session ID
	 */
	@Column(name = "sessionId", nullable = false)
	public Long getSessionId() {
		return this.sessionId;
	}

	/**
	 * Set the session ID that generated this product.
	 * 
	 * @param sessionId
	 *            a valid session ID
	 */
	public void setSessionId(Long sessionId) {
		this.sessionId = sessionId;
	}

	/**
	 * Get the name of the machine that executed the generating session.
	 * 
	 * @return a String host name
	 */
	@Column(name = "sessionHost", nullable = false, length = 1024)
	public String getSessionHost() {
		return this.sessionHost;
	}

	/**
	 * Set the name of the machine that executed the session and generated this
	 * product.
	 * 
	 * @param sessionHost
	 *            a valid host name
	 */
	public void setSessionHost(String sessionHost) {
		this.sessionHost = sessionHost;
	}

	/**
	 * Get the application process ID.
	 * 
	 * @return the application process ID
	 */
	@Column(name = "apid", nullable = false)
	public Integer getApid() {
		return this.apid;
	}

	/**
	 * Set the application process ID.
	 * 
	 * @param apid
	 *            an application process ID
	 */
	public void setApid(Integer apid) {
		this.apid = apid;
	}

	/**
	 * Get the virtual channel ID for this product.
	 * 
	 * @return the virtual channel ID
	 */
	@Column(name = "vcid", nullable = false)
	public Integer getVcid() {
		return this.vcid;
	}

	/**
	 * Set the virtual channel ID for this product.
	 * 
	 * @param vcid
	 *            the valid virtual channel ID
	 */
	public void setVcid(Integer vcid) {
		this.vcid = vcid;
	}

	/**
	 * Get the coarse component of the spacecraft clock
	 * 
	 * @return the coarse spacecraft clock time value
	 */
	@Column(name = "sclkCoarse", nullable = false)
	public Long getSclkCoarse() {
		return this.sclkCoarse;
	}

	/**
	 * Set the coarse component of the spacecraft clock.
	 * 
	 * @param sclkCoarse
	 *            the new coarse spacecraft clock time value
	 */
	public void setSclkCoarse(Long sclkCoarse) {
		this.sclkCoarse = sclkCoarse;
	}

	/**
	 * Get the fine component of the spacecraft clock.
	 * 
	 * @return the fine spacecraft clock time value
	 */
	@Column(name = "sclkFine", nullable = false)
	public Long getSclkFine() {
		return this.sclkFine;
	}

	/**
	 * Set the fine component of the spacecraft clock.
	 * 
	 * @param sclkFine
	 *            the new fine spacecraft clock time value
	 */
	public void setSclkFine(Long sclkFine) {
		this.sclkFine = sclkFine;
	}
	
	/**
	 * Get the compressed state value.
	 * 
	 * @return 1 if compressed, 0, if not
	 */
	@Column(name = "isCompressed", nullable = false)
	public int getIsCompressed() {
		return this.isCompressed;
	}
	
	/**
	 * Set the compressed state value.
	 * 
	 * @param isCompressed
	 *            1 if compressed, 0 if not
	 */
	public void setIsCompressed(int isCompressed) {
		this.isCompressed = isCompressed;
	}
	
	/**
	 * Get the chill_down processing and extraction value.
	 * 
	 * @return 1 if extracted by chill_down, 0 if not
	 */
	@Column(name = "realTimeExtraction", nullable = false)
	public int getRealTimeExtraction() {
		return this.realTimeExtraction;
	}
	
	/**
	 * Set the chill_down processing and extraction value
	 * 
	 * @param realTimeExtraction
	 *            1 if extracted by chill_down, 0 if not
	 */
	public void setRealTimeExtraction(int realTimeExtraction) {
		this.realTimeExtraction = realTimeExtraction;
	}

	/**
	 * Get the add time value.
	 * 
	 * @return the add time Timestamp
	 */
	@Generated(GenerationTime.INSERT)
	@Column(name = "addTime", length = 19)
	public Timestamp getAddTime() {
		return this.addTime;
	}

	/**
	 * Set the add time.
	 * 
	 * @param addTime
	 *            the add time Timestamp
	 */
	public void setAddTime(Timestamp addTime) {
		this.addTime = addTime;
	}

	/**
	 * Get the SortedSet of statuses for this product.
	 * 
	 * @return the set of statuses for this product, sorted by statusId
	 */
	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REFRESH, mappedBy = "product")
	@SortNatural
	public SortedSet<ProductAutomationStatus> getStatuses() {
		return this.statuses;
	}
	
	/**
	 * Set the statuses for this product. If this product currently has status
	 * values they will be overwritten.
	 * 
	 * @param statuses
	 *            the new set of statuses for this product
	 */
	public void setStatuses(SortedSet<ProductAutomationStatus> statuses) {
		this.statuses = statuses;
	}

	/**
	 * Get the SortedSet of actions for this product.
	 * 
	 * @return the set of actions for this product, sorted by actionId
	 */
	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REFRESH, mappedBy = "product")
	@SortNatural
	public SortedSet<ProductAutomationAction> getActions() {
		return this.actions;
	}
	
	/**
	 * Set the actions for this product. If this product currently has action
	 * values they will be overwritten.
	 * 
	 * @param actions
	 *            the new set of actions for this product
	 */
	public void setActions(SortedSet<ProductAutomationAction> actions) {
		this.actions = actions;
	}	

	/**
	 * Get the SortedSet of logs for this product.
	 * 
	 * @return the set of logs for this product, sorted by logId
	 */
	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REFRESH, mappedBy = "product")
	@SortNatural
	public SortedSet<ProductAutomationLog> getLogs() {
		return this.logs;
	}
	
	/**
	 * Set the logs for this product. If this product currently has log
	 * values they will be overwritten.
	 * 
	 * @param logs
	 *            the new set of logs for this product
	 */
	public void setLogs(SortedSet<ProductAutomationLog> logs) {
		this.logs = logs;
	}	
	
	/**
	 * Get the compressed value as a boolean.
	 * 
	 * @return true if compressed, false if not
	 */
	@Transient
	public boolean isCompressedBoolean() {
		return getIsCompressed() == 1;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ProductAutomationProduct [productId=" + productId
				+ ", productPath=" + productPath + ", parent=" + parent
				+ ", fswBuildId=" + fswBuildId + ", dictVersion=" + dictVersion
				+ ", sessionId=" + sessionId + ", sessionHost=" + sessionHost
				+ ", apid=" + apid + ", vcid=" + vcid + ", sclkCoarse="
				+ sclkCoarse + ", sclkFine=" + sclkFine + ", addTime="
				+ addTime + "]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((addTime == null) ? 0 : addTime.hashCode());
		result = prime * result + ((apid == null) ? 0 : apid.hashCode());
		result = prime * result
				+ ((dictVersion == null) ? 0 : dictVersion.hashCode());
		result = prime * result
				+ ((fswBuildId == null) ? 0 : fswBuildId.hashCode());
		result = prime * result
				+ ((parent == null) ? 0 : parent.hashCode());
		result = prime * result
				+ ((productPath == null) ? 0 : productPath.hashCode());
		result = prime * result
				+ ((sclkCoarse == null) ? 0 : sclkCoarse.hashCode());
		result = prime * result
				+ ((sclkFine == null) ? 0 : sclkFine.hashCode());
		result = prime * result
				+ ((sessionHost == null) ? 0 : sessionHost.hashCode());
		result = prime * result
				+ ((sessionId == null) ? 0 : sessionId.hashCode());
		result = prime * result + ((vcid == null) ? 0 : vcid.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof ProductAutomationProduct)) {
			return false;
		}
		ProductAutomationProduct other = (ProductAutomationProduct) obj;
		if (addTime == null) {
			if (other.addTime != null) {
				return false;
			}
		} else if (!addTime.equals(other.addTime)) {
			return false;
		}
		if (apid == null) {
			if (other.apid != null) {
				return false;
			}
		} else if (!apid.equals(other.apid)) {
			return false;
		}
		if (dictVersion == null) {
			if (other.dictVersion != null) {
				return false;
			}
		} else if (!dictVersion.equals(other.dictVersion)) {
			return false;
		}
		if (fswBuildId == null) {
			if (other.fswBuildId != null) {
				return false;
			}
		} else if (!fswBuildId.equals(other.fswBuildId)) {
			return false;
		}
		if (parent == null) {
			if (other.parent != null) {
				return false;
			}
		} else if (!parent.equals(other.parent)) {
			return false;
		}
		if (productPath == null) {
			if (other.productPath != null) {
				return false;
			}
		} else if (!productPath.equals(other.productPath)) {
			return false;
		}
		if (sclkCoarse == null) {
			if (other.sclkCoarse != null) {
				return false;
			}
		} else if (!sclkCoarse.equals(other.sclkCoarse)) {
			return false;
		}
		if (sclkFine == null) {
			if (other.sclkFine != null) {
				return false;
			}
		} else if (!sclkFine.equals(other.sclkFine)) {
			return false;
		}
		if (sessionHost == null) {
			if (other.sessionHost != null) {
				return false;
			}
		} else if (!sessionHost.equals(other.sessionHost)) {
			return false;
		}
		if (sessionId == null) {
			if (other.sessionId != null) {
				return false;
			}
		} else if (!sessionId.equals(other.sessionId)) {
			return false;
		}
		if (vcid == null) {
			if (other.vcid != null) {
				return false;
			}
		} else if (!vcid.equals(other.vcid)) {
			return false;
		}
		return true;
	}

	@Override
	/**
	 * Right now only compares the product id.
	 */
	public int compareTo(ProductAutomationProduct o) {
		return this.getProductId().compareTo(o.getProductId());
	}
}