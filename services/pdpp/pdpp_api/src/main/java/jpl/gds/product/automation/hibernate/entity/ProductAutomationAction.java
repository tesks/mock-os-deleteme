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
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

/**
 * Entity class for product automation action table.
 * 
 * @version MPCS-8179 - 06/07/16 - Added to AMPCS, updated from original version in MPCS for MSL G9.
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "action", uniqueConstraints = @UniqueConstraint(columnNames = {
		"product", "passNumber", "actionName" }))
		
public class ProductAutomationAction implements java.io.Serializable, Comparable<ProductAutomationAction> {

	// Fields

	/**
	 * MPCS-6544 - Added members and getters for new action table column reassign.
	 */
	private static final long serialVersionUID = 8249820734204607836L;
	private Long actionId;
	private ProductAutomationClassMap actionName;
	private ProductAutomationProcess process;
	private ProductAutomationProduct product;
	private Long passNumber;
	private Long versionId;
	private Timestamp assignedTime;
	private Timestamp acceptedTime;
	private Timestamp completedTime;
	private int reassign = 0;
	
	// Constructors

	/** default constructor */
	public ProductAutomationAction() {
	}
	
	/**
	 * minimal constructor
	 * 
	 * @param actionName
	 *            the ProductAutomationClassMap that relates this action to a
	 *            class
	 * @param process
	 *            the ProductAutomationProcess that handles this action
	 * @param product
	 *            the ProductAutomationProduct that this action will work on
	 * @param passNumber
	 *            the number of times, including this, that the product has been
	 *            processed
	 */
	public ProductAutomationAction(ProductAutomationClassMap actionName, ProductAutomationProcess process, ProductAutomationProduct product, Long passNumber) {
		this.actionName = actionName;
		this.process = process;
		this.product = product;
		this.passNumber = passNumber;
	}

	/**
	 * full constructor
	 * 
	 * @param actionName
	 *            the ProductAutomationClassMap that relates this action to a
	 *            class
	 * @param process
	 *            the ProductAutomationProcess that handles this action
	 * @param product
	 *            the ProductAutomationProduct that this action will work on
	 * @param passNumber
	 *            the number of times, including this, that the product has been
	 *            processed
	 * @param assignedTime
	 *            the time when this action is assigned to a process
	 * @param acceptedTime
	 *            the time when this action is accepted and the process begins
	 *            work
	 * @param completedTime
	 *            the time when this action has been completed
	 * @param reassign
	 *            0 if it is not marked for reassginment, 1 if it is
	 * @param versionId
	 *            the version
	 */
	public ProductAutomationAction(ProductAutomationClassMap actionName, ProductAutomationProcess process, ProductAutomationProduct product,
			Long passNumber, Timestamp assignedTime, Timestamp acceptedTime, Timestamp completedTime, Integer reassign, Long versionId) {
		this.actionName = actionName;
		this.process = process;
		this.product = product;
		this.passNumber = passNumber;
		this.versionId = versionId;
		this.assignedTime = assignedTime;
		this.acceptedTime = acceptedTime;
		this.completedTime = completedTime;
		this.reassign = reassign;
	}

	// Property accessors
	/**
	 * Get the action ID for this object
	 * 
	 * @return the Long value that uniquely identifies this action
	 */
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name = "actionId", unique = true, nullable = false)
	public Long getActionId() {
		return this.actionId;
	}

	/**
	 * Set the action ID for this object
	 * 
	 * @param actionId
	 *            a Long value that will uniquely identifies this action
	 */
	public void setActionId(Long actionId) {
		this.actionId = actionId;
	}
	
	/**
	 * Get the ProductAutomationClassMap for this action. The
	 * classmap shows what class handles performs the action
	 * 
	 * @return the ProductAutomationClassMap for this action
	 */
	@ManyToOne
	@JoinColumn(name = "actionName")
	public ProductAutomationClassMap getActionName() {
		return this.actionName;
	}
	
	/**
	 * Set the ProductAutomationClassMap to be for this action.
	 * 
	 * @param actionName
	 *            a ProductAutomationClassMap with an action mnemonic and class
	 *            name
	 */
	public void setActionName(ProductAutomationClassMap actionName) {
		this.actionName = actionName;
	}
	
	/**
	 * Set the ProductAutomationProduct that is operated on in this action
	 * 
	 * @return the ProductAutomationProduct for this action
	 */
	@ManyToOne
	@JoinColumn(name="product")
	public ProductAutomationProduct getProduct() {
		return this.product;
	}

	/**
	 * Set the ProductAutomationProduct, the product that is operated on during
	 * this action
	 * 
	 * @param product
	 *            a productAutomationProduct to be acted upon
	 */
	public void setProduct(ProductAutomationProduct product) {
		this.product = product;
	}

	/**
	 * Get the ProductAutomationProcess. A process executes an action
	 * 
	 * @return a productAutomationProcess that will execute this action
	 */
	@ManyToOne
	@JoinColumn(name="process")
	public ProductAutomationProcess getProcess() {
		return this.process;
	}
	
	/**
	 * Set the ProductAutomationProcess. A process executes an action.
	 * 
	 * @param process
	 *            a productAutomationProcess that will execute this action
	 */
	public void setProcess(ProductAutomationProcess process) {
		this.process = process;
	}
	
	/**
	 * Get the pass number. This is based upon the number of passes performed on
	 * the product that this action is part of
	 * 
	 * @return a Long value corresponding to the pass number this this action is
	 *         associated with
	 */
	@Column(name = "passNumber", nullable = false)
	public Long getPassNumber() {
		return this.passNumber;
	}

	/**
	 * Set the pass number. This is based upon the number of passes performed on
	 * the product that this action is part of
	 * 
	 * @param passNumber
	 *            a Long value corresponding to the pass number this this action
	 *            is associated with
	 */
	public void setPassNumber(Long passNumber) {
		this.passNumber = passNumber;
	}
	
	/**
	 * Get the time this action was assigned to a process.
	 * 
	 * @return a Timestamp representing the time this action was assigned to a
	 *         process
	 */
	@Generated(GenerationTime.INSERT)
	@Column(name = "assignedTime", length = 19)
	public Timestamp getAssignedTime() {
		return this.assignedTime;
	}

	/**
	 * Get the time this action was assigned to a process.
	 * 
	 * @param assignedTime
	 *            a Timestamp representing the time this action was assigned to
	 *            a process
	 */
	public void setAssignedTime(Timestamp assignedTime) {
		this.assignedTime = assignedTime;
	}
	
	/**
	 * Get the time this action was accepted by the process it was assigned to.
	 * This time will be no earlier than the assigned time
	 * 
	 * @return a Timestamp representing the time this action was accepted by the
	 *         process
	 */
	@Column(name = "acceptedTime", length = 19)
	public Timestamp getAcceptedTime() {
		return this.acceptedTime;
	}

	/**
	 * Set the time this action was accepted by the process it was assigned to.
	 * This time should be no earlier than the assigned time
	 * 
	 * @param acceptedTime
	 *            a Timestamp representing the time this action was accepted by
	 *            the process
	 */
	public void setAcceptedTime(Timestamp acceptedTime) {
		this.acceptedTime = acceptedTime;
	}
	
	/**
	 * Get the time this action was completed. This time will be ater the
	 * accepted time
	 * 
	 * @return a Timestamp representing the time this action was completed by
	 *         the process
	 */
	@Column(name = "completedTime", length = 19)
	public Timestamp getCompletedTime() {
		return this.completedTime;
	}
	
	/**
	 * Set the time this action was completed. This time will be ater the
	 * accepted time
	 * 
	 * @param completedTime
	 *            a Timestamp representing the time this action was completed by
	 *            the process
	 */
	public void setCompletedTime(Timestamp completedTime) {
		this.completedTime = completedTime;
	}
	
	/**
	 * Get the reassignment status value. 0 if it is not marked for reassignment, 1 if it is.
	 * 
	 * @return 0 if this action has not been marked for reassignment, 1 if it has
	 */
	@Column(name = "reassign", nullable = false)
	public Integer getReassign() {
		return reassign;
	}
	
	/**
	 * Set the reassign value. 0 if it is not reassigned, 1 if it is.
	 * 
	 * @param reassign
	 *            an Integer representing the boolean value of the marked for
	 *            reassignment status
	 */
	public void setReassign(Integer reassign) {
		this.reassign = reassign;
	}

	/**
	 * Get the reassign value as a boolean
	 * 
	 * @return true if marked for reassignment, false if not
	 */
	@Transient
	public boolean getReassignBool() {
		return getReassign() == 1;
	}
	
	/**
	 * Get the version ID
	 * 
	 * @return the Long versionId of this action
	 */
	@Version
	@Column(name = "versionId")
	public Long getVersionId() {
		return this.versionId;
	}
	
	/**
	 * Set the version ID
	 * @param versionId the version ID to be set for this action
	 */
	public void setVersionId(Long versionId) {
		this.versionId = versionId;
	}
	
	/**
	 * This is used to copy the object.  All fields except for processId, stage and stagedTime
	 * are set.  You must set stage before you try to persist the object, but the others will be 
	 * handled by hibernate.
	 * 
	 * @return a copy of this ProductAutomationAction with only an action name, pass number, process, and product  
	 */
	@Transient
	public ProductAutomationAction cleanCopy() {
		ProductAutomationAction clone = new ProductAutomationAction();
		clone.setActionName(this.actionName);
		clone.setPassNumber(this.passNumber);
		clone.setProcess(this.process);
		clone.setProduct(this.product);
		
		return clone;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((acceptedTime == null) ? 0 : acceptedTime.hashCode());
		result = prime * result
				+ ((actionName == null) ? 0 : actionName.hashCode());
		result = prime * result
				+ ((assignedTime == null) ? 0 : assignedTime.hashCode());
		result = prime * result
				+ ((completedTime == null) ? 0 : completedTime.hashCode());
		result = prime * result
				+ ((passNumber == null) ? 0 : passNumber.hashCode());
		result = prime * result + ((process == null) ? 0 : process.hashCode());
		result = prime * result + ((product == null) ? 0 : product.hashCode());
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
		if (!(obj instanceof ProductAutomationAction)) {
			return false;
		}
		ProductAutomationAction other = (ProductAutomationAction) obj;
		if (acceptedTime == null) {
			if (other.acceptedTime != null) {
				return false;
			}
		} else if (!acceptedTime.equals(other.acceptedTime)) {
			return false;
		}
		if (actionName == null) {
			if (other.actionName != null) {
				return false;
			}
		} else if (!actionName.equals(other.actionName)) {
			return false;
		}
		if (assignedTime == null) {
			if (other.assignedTime != null) {
				return false;
			}
		} else if (!assignedTime.equals(other.assignedTime)) {
			return false;
		}
		if (completedTime == null) {
			if (other.completedTime != null) {
				return false;
			}
		} else if (!completedTime.equals(other.completedTime)) {
			return false;
		}
		if (passNumber == null) {
			if (other.passNumber != null) {
				return false;
			}
		} else if (!passNumber.equals(other.passNumber)) {
			return false;
		}
		if (process == null) {
			if (other.process != null) {
				return false;
			}
		} else if (!process.equals(other.process)) {
			return false;
		}
		if (product == null) {
			if (other.product != null) {
				return false;
			}
		} else if (!product.equals(other.product)) {
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ProductAutomationAction [actionId=" + actionId + ", actionName="
				+ actionName + ", process=" + process + ", product=" + product
				+ ", passNumber=" + passNumber + ", versionId=" + versionId
				+ ", assignedTime=" + assignedTime + ", acceptedTime="
				+ acceptedTime + ", completedTime=" + completedTime + "]";
	}

	@Override
	public int compareTo(ProductAutomationAction o) {
		return this.getActionId().compareTo(o.getActionId());
	}
	
}