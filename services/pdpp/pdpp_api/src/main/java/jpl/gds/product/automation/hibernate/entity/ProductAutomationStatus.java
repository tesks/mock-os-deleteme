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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

/**
 * Entity class for product automation status table.
 * 
 * MPCS-8179 - 06/07/16 - Added to AMPCS, updated from original version in MPCS for MSL G9.
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "status")

public class ProductAutomationStatus implements java.io.Serializable, 
	Comparable<ProductAutomationStatus> {

	// Fields
	private static final long serialVersionUID = 8529246451645843795L;
	private Long statusId;
	private String statusName;
	private ProductAutomationProduct product;
	private Long passNumber;
	private Timestamp statusTime;

	// Constructors

	/** default constructor */
	public ProductAutomationStatus() {
	}
	
	/**
	 * short constructor
	 * 
	 * @param statusName
	 *            the name of this status
	 * @param product
	 *            the product this status is referring to
	 * @param passNumber
	 *            the pass number on the product that generated this status
	 */
	public ProductAutomationStatus(String statusName, ProductAutomationProduct product, Long passNumber) {
		this.statusName = statusName;
		this.product = product;
		this.passNumber = passNumber;
	}

	/**
	 * full constructor
	 * 
	 * @param statusName
	 *            the name of this status
	 * @param product
	 *            the product this status is referring to
	 * @param passNumber
	 *            the pass number on the product that generated this status
	 * @param statusTime
	 *            the time this status was generated
	 */
	public ProductAutomationStatus(String statusName, ProductAutomationProduct product, Long passNumber, Timestamp statusTime) {
		this.statusName = statusName;
		this.product = product;
		this.passNumber = passNumber;
		this.statusTime = statusTime;
	}

	// Property accessors
	/**
	 * get the status ID for this object.
	 * 
	 * @return the Long value that uniquely identifies this status
	 */
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name = "statusId", unique = true, nullable = false)
	public Long getStatusId() {
		return this.statusId;
	}

	/**
	 * Set the status ID for this object
	 * 
	 * @param statusId
	 *            a Long value that should uniquely identify this status
	 */
	public void setStatusId(Long statusId) {
		this.statusId = statusId;
	}

	/**
	 * Get the name of this status object.
	 * 
	 * @return the name
	 */
	@Column(name = "statusName", nullable = false, length = 14)
	public String getStatusName() {
		return this.statusName;
	}

	/**
	 * Set the name of this status object.
	 * 
	 * @param statusName
	 *            the new name of this status
	 */
	public void setStatusName(String statusName) {
		this.statusName = statusName;
	}

	/**
	 * Get the product this status refers to.
	 * 
	 * @return the ProductAutomationProduct this status is about
	 */
	@ManyToOne
	@JoinColumn(name = "product")
	public ProductAutomationProduct getProduct() {
		return this.product;
	}

	/**
	 * Set the product this status refers to.
	 * 
	 * @param product
	 *            the ProductAutomationProduct that generated this status
	 */
	public void setProduct(ProductAutomationProduct product) {
		this.product = product;
	}

	/**
	 * Get the pass number.
	 * 
	 * @return the Long pass number value
	 */
	@Column(name = "passNumber", nullable = false)
	public Long getPassNumber() {
		return this.passNumber;
	}

	/**
	 * Set the pass number.
	 * 
	 * @param passNumber
	 *            the Long pass number value
	 */
	public void setPassNumber(Long passNumber) {
		this.passNumber = passNumber;
	}

	/**
	 * Get the time for this status.
	 * 
	 * @return the Timestamp this status was generated
	 */
	@Generated(GenerationTime.INSERT)
	@Column(name = "statusTime", length = 19)
	public Timestamp getStatusTime() {
		return this.statusTime;
	}

	/**
	 * Set the time for this status.
	 * 
	 * @param statusTime
	 *            the new time for this status
	 */
	public void setStatusTime(Timestamp statusTime) {
		this.statusTime = statusTime;
	}

	/**
	 * Creates a copy of the object. The id, time fields and status will be
	 * null.
	 * 
	 * @return a clean copy of this ProductAutomationStatus
	 */
	@Transient
	public ProductAutomationStatus cleanCopy() {
		ProductAutomationStatus copy = new ProductAutomationStatus();
		copy.setPassNumber(getPassNumber());
		copy.setProduct(getProduct());
		
		return copy;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Transient
	@Override
	public String toString() {
		return "MslAutomationStatus [statusId=" + statusId + ", statusName="
				+ statusName + ", product=" + product.getProductPath() + ", passNumber="
				+ passNumber + ", statusTime=" + statusTime + "]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((passNumber == null) ? 0 : passNumber.hashCode());
		result = prime * result
				+ ((statusId == null) ? 0 : statusId.hashCode());
		result = prime * result
				+ ((statusName == null) ? 0 : statusName.hashCode());
		result = prime * result
				+ ((statusTime == null) ? 0 : statusTime.hashCode());
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
		if (!(obj instanceof ProductAutomationStatus)) {
			return false;
		}
		ProductAutomationStatus other = (ProductAutomationStatus) obj;
		if (passNumber == null) {
			if (other.passNumber != null) {
				return false;
			}
		} else if (!passNumber.equals(other.passNumber)) {
			return false;
		}
		if (statusId == null) {
			if (other.statusId != null) {
				return false;
			}
		} else if (!statusId.equals(other.statusId)) {
			return false;
		}
		if (statusName == null) {
			if (other.statusName != null) {
				return false;
			}
		} else if (!statusName.equals(other.statusName)) {
			return false;
		}
		if (statusTime == null) {
			if (other.statusTime != null) {
				return false;
			}
		} else if (!statusTime.equals(other.statusTime)) {
			return false;
		}
		return true;
	}

	@Override
	/**
	 * Mainly used for the gui DAO's.  Does comparisons based on products and then on
	 * the status type...?  Right now only does comparison on the status id, which really
	 * is going to give it in time order.
	 */
	public int compareTo(ProductAutomationStatus o) {
		return this.getStatusId().compareTo(o.getStatusId());
	}

}