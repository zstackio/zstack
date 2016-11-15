
package org.zstack.test.deployer.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for EipConfig complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="EipConfig">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="accountRef" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="description" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="publicL3NetworkRef" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="privateL3NetworkRef" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="vmRef" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EipConfig", propOrder = {
    "accountRef",
    "name",
    "description",
    "publicL3NetworkRef",
    "privateL3NetworkRef",
    "vmRef"
})
public class EipConfig {

    protected String accountRef;
    protected String name;
    protected String description;
    protected String publicL3NetworkRef;
    protected String privateL3NetworkRef;
    protected String vmRef;

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
     * Gets the value of the privateL3NetworkRef property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPrivateL3NetworkRef() {
        return privateL3NetworkRef;
    }

    /**
     * Sets the value of the privateL3NetworkRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPrivateL3NetworkRef(String value) {
        this.privateL3NetworkRef = value;
    }

    /**
     * Gets the value of the vmRef property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVmRef() {
        return vmRef;
    }

    /**
     * Sets the value of the vmRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVmRef(String value) {
        this.vmRef = value;
    }

}
