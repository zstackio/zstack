
package org.zstack.test.deployer.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for VirtualRouterOfferingConfig complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="VirtualRouterOfferingConfig">
 *   &lt;complexContent>
 *     &lt;extension base="{http://zstack.org/schema/zstack}InstanceOfferingConfig">
 *       &lt;sequence>
 *         &lt;element name="managementL3NetworkRef" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="publicL3NetworkRef" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="imageRef" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="zoneRef" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *       &lt;attribute name="isDefault" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "VirtualRouterOfferingConfig", propOrder = {
    "managementL3NetworkRef",
    "publicL3NetworkRef",
    "imageRef",
    "zoneRef"
})
public class VirtualRouterOfferingConfig
    extends InstanceOfferingConfig
{

    @XmlElement(required = true)
    protected String managementL3NetworkRef;
    protected String publicL3NetworkRef;
    @XmlElement(required = true)
    protected String imageRef;
    @XmlElement(required = true)
    protected String zoneRef;
    @XmlAttribute(name = "isDefault")
    protected Boolean isDefault;

    /**
     * Gets the value of the managementL3NetworkRef property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getManagementL3NetworkRef() {
        return managementL3NetworkRef;
    }

    /**
     * Sets the value of the managementL3NetworkRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setManagementL3NetworkRef(String value) {
        this.managementL3NetworkRef = value;
    }

    /**
     * Gets the value of the publicL3NetworkRef property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPublicL3NetworkRef() {
        return publicL3NetworkRef;
    }

    /**
     * Sets the value of the publicL3NetworkRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPublicL3NetworkRef(String value) {
        this.publicL3NetworkRef = value;
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
     * Gets the value of the isDefault property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isIsDefault() {
        if (isDefault == null) {
            return false;
        } else {
            return isDefault;
        }
    }

    /**
     * Sets the value of the isDefault property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setIsDefault(Boolean value) {
        this.isDefault = value;
    }

}
