
package org.zstack.test.deployer.schema;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for UserVmConfig complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="UserVmConfig">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="accountRef" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="instanceOfferingRef" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="imageRef" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="rootDiskOfferingRef" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="securityGoupRef" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="zoneRef" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="clusterRef" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="hostRef" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="l3NetworkRef" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="hostname" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="defaultL3NetworkRef" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="diskOfferingRef" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="description" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="type" type="{http://www.w3.org/2001/XMLSchema}string" default="UserVm" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "UserVmConfig", propOrder = {
    "accountRef",
    "instanceOfferingRef",
    "imageRef",
    "rootDiskOfferingRef",
    "securityGoupRef",
    "zoneRef",
    "clusterRef",
    "hostRef",
    "l3NetworkRef",
    "hostname",
    "defaultL3NetworkRef",
    "diskOfferingRef"
})
public class UserVmConfig {

    protected String accountRef;
    @XmlElement(required = true)
    protected String instanceOfferingRef;
    @XmlElement(required = true)
    protected String imageRef;
    protected String rootDiskOfferingRef;
    protected String securityGoupRef;
    protected String zoneRef;
    protected String clusterRef;
    protected String hostRef;
    protected List<String> l3NetworkRef;
    protected List<String> hostname;
    protected String defaultL3NetworkRef;
    protected List<String> diskOfferingRef;
    @XmlAttribute(name = "name", required = true)
    protected String name;
    @XmlAttribute(name = "description")
    protected String description;
    @XmlAttribute(name = "type")
    protected String type;

    /**
     * Gets the value of the accountRef property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAccountRef() {
        return accountRef;
    }

    /**
     * Sets the value of the accountRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAccountRef(String value) {
        this.accountRef = value;
    }

    /**
     * Gets the value of the instanceOfferingRef property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getInstanceOfferingRef() {
        return instanceOfferingRef;
    }

    /**
     * Sets the value of the instanceOfferingRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setInstanceOfferingRef(String value) {
        this.instanceOfferingRef = value;
    }

    /**
     * Gets the value of the imageRef property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getImageRef() {
        return imageRef;
    }

    /**
     * Sets the value of the imageRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setImageRef(String value) {
        this.imageRef = value;
    }

    /**
     * Gets the value of the rootDiskOfferingRef property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRootDiskOfferingRef() {
        return rootDiskOfferingRef;
    }

    /**
     * Sets the value of the rootDiskOfferingRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRootDiskOfferingRef(String value) {
        this.rootDiskOfferingRef = value;
    }

    /**
     * Gets the value of the securityGoupRef property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSecurityGoupRef() {
        return securityGoupRef;
    }

    /**
     * Sets the value of the securityGoupRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSecurityGoupRef(String value) {
        this.securityGoupRef = value;
    }

    /**
     * Gets the value of the zoneRef property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getZoneRef() {
        return zoneRef;
    }

    /**
     * Sets the value of the zoneRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setZoneRef(String value) {
        this.zoneRef = value;
    }

    /**
     * Gets the value of the clusterRef property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getClusterRef() {
        return clusterRef;
    }

    /**
     * Sets the value of the clusterRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setClusterRef(String value) {
        this.clusterRef = value;
    }

    /**
     * Gets the value of the hostRef property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getHostRef() {
        return hostRef;
    }

    /**
     * Sets the value of the hostRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setHostRef(String value) {
        this.hostRef = value;
    }

    /**
     * Gets the value of the l3NetworkRef property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the l3NetworkRef property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getL3NetworkRef().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getL3NetworkRef() {
        if (l3NetworkRef == null) {
            l3NetworkRef = new ArrayList<String>();
        }
        return this.l3NetworkRef;
    }

    /**
     * Gets the value of the hostname property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the hostname property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getHostname().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getHostname() {
        if (hostname == null) {
            hostname = new ArrayList<String>();
        }
        return this.hostname;
    }

    /**
     * Gets the value of the defaultL3NetworkRef property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDefaultL3NetworkRef() {
        return defaultL3NetworkRef;
    }

    /**
     * Sets the value of the defaultL3NetworkRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDefaultL3NetworkRef(String value) {
        this.defaultL3NetworkRef = value;
    }

    /**
     * Gets the value of the diskOfferingRef property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the diskOfferingRef property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDiskOfferingRef().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getDiskOfferingRef() {
        if (diskOfferingRef == null) {
            diskOfferingRef = new ArrayList<String>();
        }
        return this.diskOfferingRef;
    }

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
     * Gets the value of the type property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getType() {
        if (type == null) {
            return "UserVm";
        } else {
            return type;
        }
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setType(String value) {
        this.type = value;
    }

}
