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

import static javax.persistence.GenerationType.IDENTITY;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Entity class for product automation classmaps table.
 * 
 * MPCS-8179 - 06/07/16 - Added to AMPCS, updated from original version in MPCS for MSL G9.
 */
@Entity
@Table(name = "classmaps", uniqueConstraints = @UniqueConstraint(columnNames = {
		"mnemonic", "className" }))
public class ProductAutomationClassMap implements java.io.Serializable, Comparable<ProductAutomationClassMap>{

	// Fields
	private static final long serialVersionUID = -860868644166342625L;
	private Long classId;
	private String mnemonic;
	private String className;
	private int enabled;

	// Constructors

	/** default constructor */
	public ProductAutomationClassMap() {
	}

	/**
	 * full constructor
	 * 
	 * @param mnemonic
	 *            the type of action to be performed by the associate class
	 * @param className
	 *            the name of the class that performs the associated action
	 *            mnemonic
	 * @param enabled
	 *            true if this classmap is to be enabled, false if not
	 */
	public ProductAutomationClassMap(String mnemonic, String className, int enabled) {
		this.mnemonic = mnemonic;
		this.className = className;
		this.enabled = enabled;
	}

	// Property accessors
	/**
	 * Get the class ID for this object.
	 * 
	 * @return the Long value that uniquely identifies this classmap
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "classId", unique = true, nullable = false)
	public Long getClassId() {
		return this.classId;
	}

	/**
	 * Set the class ID for this object.
	 * 
	 * @param classId
	 *            a Long value that will uniquely identify this classmap
	 */
	public void setClassId(Long classId) {
		this.classId = classId;
	}

	/**
	 * Get the mnemonic for this classmap. It defines what type of action is
	 * performed by the class in this classmap
	 * 
	 * @return a String representing the type of action done by the class in
	 *         this classmap
	 */
	@Column(name = "mnemonic", nullable = false, length = 8)
	public String getMnemonic() {
		return this.mnemonic;
	}

	/**
	 * Set the mnemonic for this classmap.
	 * 
	 * @param mnemonic
	 *            a String representing the type of action done by the class in
	 *            this classmap
	 */
	public void setMnemonic(String mnemonic) {
		this.mnemonic = mnemonic;
	}

	/**
	 * Get the name of the class in the classmap
	 * 
	 * @return the full class name for this classmap
	 */
	@Column(name = "className", nullable = false, length = 512)
	public String getClassName() {
		return this.className;
	}

	/**
	 * Set the name of the class in this classmap.<br>
	 * <br>
	 * eg: jpl.gds.product.automation.hibernate.entity.
	 * ProductAutomationClassMap
	 * 
	 * @param className
	 */
	public void setClassName(String className) {
		this.className = className;
	}
	
	/**
	 * Get the enabled status
	 * @return 1 if enabled, 0 if not
	 */
	@Column(name = "enabled", nullable = false)
	public int getEnabled() {
		return this.enabled;
	}
	
	/**
	 * @param enabled
	 */
	public void setEnabled(int enabled) {
		this.enabled = enabled;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((classId == null) ? 0 : classId.hashCode());
		result = prime * result
				+ ((className == null) ? 0 : className.hashCode());
		result = prime * result
				+ ((mnemonic == null) ? 0 : mnemonic.hashCode());
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
		if (!(obj instanceof ProductAutomationClassMap)) {
			return false;
		}
		ProductAutomationClassMap other = (ProductAutomationClassMap) obj;
		if (classId == null) {
			if (other.classId != null) {
				return false;
			}
		} else if (!classId.equals(other.classId)) {
			return false;
		}
		if (className == null) {
			if (other.className != null) {
				return false;
			}
		} else if (!className.equals(other.className)) {
			return false;
		}
		if (mnemonic == null) {
			if (other.mnemonic != null) {
				return false;
			}
		} else if (!mnemonic.equals(other.mnemonic)) {
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ProductAutomationClassMap [mnemonic=" + mnemonic + ", className="
				+ className + "]";
	}

	@Override
	/**
	 * Compares the id of the map only.
	 * 
	 * @param o
	 *            the classmap to be compared against this classmap
	 */
	public int compareTo(ProductAutomationClassMap o) {
		return getClassId().compareTo(o.getClassId());
	}

}