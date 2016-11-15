
package org.zstack.test.deployer.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for L2NoVlanNetworkConfig complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="L2NoVlanNetworkConfig">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="l3Networks" type="{http://zstack.org/schema/zstack}L3NetworkUnion" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="description" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="physicalInterface" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "L2NoVlanNetworkConfig", propOrder = {
    "l3Networks"
})
@XmlSeeAlso({
    L2VlanNetworkConfig.class
})
public class L2NoVlanNetworkConfig {

    protected L3NetworkUnion l3Networks;
    @XmlAttribute(name = "name", required = true)
    protected String name;
    @XmlAttribute(name = "description")
    protected String description;
    @XmlAttribute(name = "physicalInterface")
    protected String physicalInterface;

    /**
     * Gets the value of the l3Networks property.
     * 
     * @return
     *     possible object is
     *     {@link L3NetworkUnion }
     *     
     */
    public L3NetworkUnion getL3Networks() {
        return l3Networks;
    }

    /**
     * Sets the value of the l3Networks property.
     * 
     * @param value
     *     allowed object is
     *     {@link L3NetworkUnion }
     *     
     */
    public void setL3Networks(L3NetworkUnion value) {
        this.l3Networks = value;
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
     * Gets the value of the physicalInterface property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPhysicalInterface() {
        return physicalInterface;
    }

    /**
     * Sets the value of the physicalInterface property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPhysicalInterface(String value) {
        this.physicalInterface = value;
    }

}
