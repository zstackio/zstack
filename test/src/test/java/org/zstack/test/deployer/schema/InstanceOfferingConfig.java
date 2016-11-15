
package org.zstack.test.deployer.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for InstanceOfferingConfig complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="InstanceOfferingConfig">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="accountRef" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="description" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="memoryCapacity" type="{http://www.w3.org/2001/XMLSchema}string" default="2G" />
 *       &lt;attribute name="cpuNum" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" default="1" />
 *       &lt;attribute name="cpuSpeed" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" default="1024" />
 *       &lt;attribute name="hostTag" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="allocatorStrategy" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "InstanceOfferingConfig", propOrder = {
    "accountRef"
})
@XmlSeeAlso({
    VirtualRouterOfferingConfig.class,
    ConvergedOfferingConfig.class
})
public class InstanceOfferingConfig {

    protected String accountRef;
    @XmlAttribute(name = "name", required = true)
    protected String name;
    @XmlAttribute(name = "description")
    protected String description;
    @XmlAttribute(name = "memoryCapacity")
    protected String memoryCapacity;
    @XmlAttribute(name = "cpuNum")
    @XmlSchemaType(name = "unsignedInt")
    protected Long cpuNum;
    @XmlAttribute(name = "cpuSpeed")
    @XmlSchemaType(name = "unsignedInt")
    protected Long cpuSpeed;
    @XmlAttribute(name = "hostTag")
    protected String hostTag;
    @XmlAttribute(name = "allocatorStrategy")
    protected String allocatorStrategy;

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
     * Gets the value of the memoryCapacity property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMemoryCapacity() {
        if (memoryCapacity == null) {
            return "2G";
        } else {
            return memoryCapacity;
        }
    }

    /**
     * Sets the value of the memoryCapacity property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMemoryCapacity(String value) {
        this.memoryCapacity = value;
    }

    /**
     * Gets the value of the cpuNum property.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public long getCpuNum() {
        if (cpuNum == null) {
            return  1L;
        } else {
            return cpuNum;
        }
    }

    /**
     * Sets the value of the cpuNum property.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setCpuNum(Long value) {
        this.cpuNum = value;
    }

    /**
     * Gets the value of the cpuSpeed property.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public long getCpuSpeed() {
        if (cpuSpeed == null) {
            return  1024L;
        } else {
            return cpuSpeed;
        }
    }

    /**
     * Sets the value of the cpuSpeed property.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setCpuSpeed(Long value) {
        this.cpuSpeed = value;
    }

    /**
     * Gets the value of the hostTag property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getHostTag() {
        return hostTag;
    }

    /**
     * Sets the value of the hostTag property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setHostTag(String value) {
        this.hostTag = value;
    }

    /**
     * Gets the value of the allocatorStrategy property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAllocatorStrategy() {
        return allocatorStrategy;
    }

    /**
     * Sets the value of the allocatorStrategy property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAllocatorStrategy(String value) {
        this.allocatorStrategy = value;
    }

}
