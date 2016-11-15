
package org.zstack.test.deployer.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for PrimaryStorageConfigBase complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="PrimaryStorageConfigBase">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="description" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="url" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="totalCapacity" type="{http://www.w3.org/2001/XMLSchema}string" default="500G" />
 *       &lt;attribute name="availableCapacity" type="{http://www.w3.org/2001/XMLSchema}string" default="500G" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PrimaryStorageConfigBase")
@XmlSeeAlso({
    SharedMountPointPrimaryStorageConfig.class,
    LocalPrimaryStorageConfig.class,
    IscsiFileSystemPrimaryStorageConfig.class,
    NfsPrimaryStorageConfig.class,
    CephPrimaryStorageConfig.class
})
public class PrimaryStorageConfigBase {

    @XmlAttribute(name = "name", required = true)
    protected String name;
    @XmlAttribute(name = "description")
    protected String description;
    @XmlAttribute(name = "url", required = true)
    protected String url;
    @XmlAttribute(name = "totalCapacity")
    protected String totalCapacity;
    @XmlAttribute(name = "availableCapacity")
    protected String availableCapacity;

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the description property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDescription(String value) {
        this.description = value;
    }

    /**
     * Gets the value of the url property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the value of the url property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUrl(String value) {
        this.url = value;
    }

    /**
     * Gets the value of the totalCapacity property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTotalCapacity() {
        if (totalCapacity == null) {
            return "500G";
        } else {
            return totalCapacity;
        }
    }

    /**
     * Sets the value of the totalCapacity property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTotalCapacity(String value) {
        this.totalCapacity = value;
    }

    /**
     * Gets the value of the availableCapacity property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAvailableCapacity() {
        if (availableCapacity == null) {
            return "500G";
        } else {
            return availableCapacity;
        }
    }

    /**
     * Sets the value of the availableCapacity property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAvailableCapacity(String value) {
        this.availableCapacity = value;
    }

}
