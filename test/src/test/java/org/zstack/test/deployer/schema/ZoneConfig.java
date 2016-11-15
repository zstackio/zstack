
package org.zstack.test.deployer.schema;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ZoneConfig complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ZoneConfig">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="clusters" type="{http://zstack.org/schema/zstack}ClusterUnion" minOccurs="0"/>
 *         &lt;element name="backupStorageRef" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="l2Networks" type="{http://zstack.org/schema/zstack}L2NetworkUnion" minOccurs="0"/>
 *         &lt;element name="primaryStorages" type="{http://zstack.org/schema/zstack}PrimaryStorageUnion" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="description" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ZoneConfig", propOrder = {
    "clusters",
    "backupStorageRef",
    "l2Networks",
    "primaryStorages"
})
public class ZoneConfig {

    protected ClusterUnion clusters;
    protected List<String> backupStorageRef;
    protected L2NetworkUnion l2Networks;
    protected PrimaryStorageUnion primaryStorages;
    @XmlAttribute(name = "name", required = true)
    protected String name;
    @XmlAttribute(name = "description")
    protected String description;

    /**
     * Gets the value of the clusters property.
     * 
     * @return
     *     possible object is
     *     {@link ClusterUnion }
     *     
     */
    public ClusterUnion getClusters() {
        return clusters;
    }

    /**
     * Sets the value of the clusters property.
     * 
     * @param value
     *     allowed object is
     *     {@link ClusterUnion }
     *     
     */
    public void setClusters(ClusterUnion value) {
        this.clusters = value;
    }

    /**
     * Gets the value of the backupStorageRef property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the backupStorageRef property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getBackupStorageRef().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getBackupStorageRef() {
        if (backupStorageRef == null) {
            backupStorageRef = new ArrayList<String>();
        }
        return this.backupStorageRef;
    }

    /**
     * Gets the value of the l2Networks property.
     * 
     * @return
     *     possible object is
     *     {@link L2NetworkUnion }
     *     
     */
    public L2NetworkUnion getL2Networks() {
        return l2Networks;
    }

    /**
     * Sets the value of the l2Networks property.
     * 
     * @param value
     *     allowed object is
     *     {@link L2NetworkUnion }
     *     
     */
    public void setL2Networks(L2NetworkUnion value) {
        this.l2Networks = value;
    }

    /**
     * Gets the value of the primaryStorages property.
     * 
     * @return
     *     possible object is
     *     {@link PrimaryStorageUnion }
     *     
     */
    public PrimaryStorageUnion getPrimaryStorages() {
        return primaryStorages;
    }

    /**
     * Sets the value of the primaryStorages property.
     * 
     * @param value
     *     allowed object is
     *     {@link PrimaryStorageUnion }
     *     
     */
    public void setPrimaryStorages(PrimaryStorageUnion value) {
        this.primaryStorages = value;
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

}
