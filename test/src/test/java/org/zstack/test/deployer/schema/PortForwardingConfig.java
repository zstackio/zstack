
package org.zstack.test.deployer.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for PortForwardingConfig complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="PortForwardingConfig">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="accountRef" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="description" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="publicL3NetworkRef" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="privateL3NetworkRef" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="vmRef" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="publicPortStart" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *         &lt;element name="publicPortEnd" type="{http://www.w3.org/2001/XMLSchema}unsignedInt"/>
 *         &lt;element name="privatePortStart" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *         &lt;element name="privatePortEnd" type="{http://www.w3.org/2001/XMLSchema}unsignedInt"/>
 *         &lt;element name="allowedCidr" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="protocolType" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PortForwardingConfig", propOrder = {
    "accountRef",
    "name",
    "description",
    "publicL3NetworkRef",
    "privateL3NetworkRef",
    "vmRef",
    "publicPortStart",
    "publicPortEnd",
    "privatePortStart",
    "privatePortEnd",
    "allowedCidr",
    "protocolType"
})
public class PortForwardingConfig {

    protected String accountRef;
    protected String name;
    protected String description;
    protected String publicL3NetworkRef;
    protected String privateL3NetworkRef;
    protected String vmRef;
    @XmlSchemaType(name = "unsignedInt")
    protected Long publicPortStart;
    @XmlSchemaType(name = "unsignedInt")
    protected long publicPortEnd;
    @XmlSchemaType(name = "unsignedInt")
    protected Long privatePortStart;
    @XmlSchemaType(name = "unsignedInt")
    protected long privatePortEnd;
    protected String allowedCidr;
    protected String protocolType;

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

    /**
     * Gets the value of the publicPortStart property.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getPublicPortStart() {
        return publicPortStart;
    }

    /**
     * Sets the value of the publicPortStart property.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setPublicPortStart(Long value) {
        this.publicPortStart = value;
    }

    /**
     * Gets the value of the publicPortEnd property.
     * 
     */
    public long getPublicPortEnd() {
        return publicPortEnd;
    }

    /**
     * Sets the value of the publicPortEnd property.
     * 
     */
    public void setPublicPortEnd(long value) {
        this.publicPortEnd = value;
    }

    /**
     * Gets the value of the privatePortStart property.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getPrivatePortStart() {
        return privatePortStart;
    }

    /**
     * Sets the value of the privatePortStart property.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setPrivatePortStart(Long value) {
        this.privatePortStart = value;
    }

    /**
     * Gets the value of the privatePortEnd property.
     * 
     */
    public long getPrivatePortEnd() {
        return privatePortEnd;
    }

    /**
     * Sets the value of the privatePortEnd property.
     * 
     */
    public void setPrivatePortEnd(long value) {
        this.privatePortEnd = value;
    }

    /**
     * Gets the value of the allowedCidr property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAllowedCidr() {
        return allowedCidr;
    }

    /**
     * Sets the value of the allowedCidr property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAllowedCidr(String value) {
        this.allowedCidr = value;
    }

    /**
     * Gets the value of the protocolType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getProtocolType() {
        return protocolType;
    }

    /**
     * Sets the value of the protocolType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setProtocolType(String value) {
        this.protocolType = value;
    }

}
