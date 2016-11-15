
package org.zstack.test.deployer.schema;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ClusterConfig complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ClusterConfig">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="hosts" type="{http://zstack.org/schema/zstack}HostUnion" minOccurs="0"/>
 *         &lt;element name="primaryStorageRef" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="l2NetworkRef" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="description" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="hypervisorType" type="{http://www.w3.org/2001/XMLSchema}string" default="Simulator" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ClusterConfig", propOrder = {
    "hosts",
    "primaryStorageRef",
    "l2NetworkRef"
})
public class ClusterConfig {

    protected HostUnion hosts;
    protected List<String> primaryStorageRef;
    protected List<String> l2NetworkRef;
    @XmlAttribute(name = "name", required = true)
    protected String name;
    @XmlAttribute(name = "description")
    protected String description;
    @XmlAttribute(name = "hypervisorType")
    protected String hypervisorType;

    /**
     * Gets the value of the hosts property.
     * 
     * @return
     *     possible object is
     *     {@link HostUnion }
     *     
     */
    public HostUnion getHosts() {
        return hosts;
    }

    /**
     * Sets the value of the hosts property.
     * 
     * @param value
     *     allowed object is
     *     {@link HostUnion }
     *     
     */
    public void setHosts(HostUnion value) {
        this.hosts = value;
    }

    /**
     * Gets the value of the primaryStorageRef property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the primaryStorageRef property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPrimaryStorageRef().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getPrimaryStorageRef() {
        if (primaryStorageRef == null) {
            primaryStorageRef = new ArrayList<String>();
        }
        return this.primaryStorageRef;
    }

    /**
     * Gets the value of the l2NetworkRef property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the l2NetworkRef property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getL2NetworkRef().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getL2NetworkRef() {
        if (l2NetworkRef == null) {
            l2NetworkRef = new ArrayList<String>();
        }
        return this.l2NetworkRef;
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
     * Gets the value of the hypervisorType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getHypervisorType() {
        if (hypervisorType == null) {
            return "Simulator";
        } else {
            return hypervisorType;
        }
    }

    /**
     * Sets the value of the hypervisorType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setHypervisorType(String value) {
        this.hypervisorType = value;
    }

}
