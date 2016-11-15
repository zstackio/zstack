
package org.zstack.test.deployer.schema;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="zones" type="{http://zstack.org/schema/zstack}ZoneUnion" minOccurs="0"/>
 *         &lt;element name="securityGroups" type="{http://zstack.org/schema/zstack}SecurityGroupUnion" minOccurs="0"/>
 *         &lt;element name="portForwardings" type="{http://zstack.org/schema/zstack}PortForwardingUnion" minOccurs="0"/>
 *         &lt;element name="eips" type="{http://zstack.org/schema/zstack}EipUnion" minOccurs="0"/>
 *         &lt;element name="lbs" type="{http://zstack.org/schema/zstack}LbUnion" minOccurs="0"/>
 *         &lt;element name="backupStorages" type="{http://zstack.org/schema/zstack}BackupStorageUnion" minOccurs="0"/>
 *         &lt;element name="images" type="{http://zstack.org/schema/zstack}ImageUnion" minOccurs="0"/>
 *         &lt;element name="vm" type="{http://zstack.org/schema/zstack}VmUnion" minOccurs="0"/>
 *         &lt;element name="instanceOfferings" type="{http://zstack.org/schema/zstack}InstanceOfferingUnion" minOccurs="0"/>
 *         &lt;element name="diskOffering" type="{http://zstack.org/schema/zstack}DiskOfferingConfig" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="account" type="{http://zstack.org/schema/zstack}AccountConfig" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="dns" type="{http://zstack.org/schema/zstack}DnsConfig" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "zones",
    "securityGroups",
    "portForwardings",
    "eips",
    "lbs",
    "backupStorages",
    "images",
    "vm",
    "instanceOfferings",
    "diskOffering",
    "account",
    "dns"
})
@XmlRootElement(name = "deployerConfig")
public class DeployerConfig {

    protected ZoneUnion zones;
    protected SecurityGroupUnion securityGroups;
    protected PortForwardingUnion portForwardings;
    protected EipUnion eips;
    protected LbUnion lbs;
    protected BackupStorageUnion backupStorages;
    protected ImageUnion images;
    protected VmUnion vm;
    protected InstanceOfferingUnion instanceOfferings;
    protected List<DiskOfferingConfig> diskOffering;
    protected List<AccountConfig> account;
    protected List<DnsConfig> dns;

    /**
     * Gets the value of the zones property.
     * 
     * @return
     *     possible object is
     *     {@link ZoneUnion }
     *     
     */
    public ZoneUnion getZones() {
        return zones;
    }

    /**
     * Sets the value of the zones property.
     * 
     * @param value
     *     allowed object is
     *     {@link ZoneUnion }
     *     
     */
    public void setZones(ZoneUnion value) {
        this.zones = value;
    }

    /**
     * Gets the value of the securityGroups property.
     * 
     * @return
     *     possible object is
     *     {@link SecurityGroupUnion }
     *     
     */
    public SecurityGroupUnion getSecurityGroups() {
        return securityGroups;
    }

    /**
     * Sets the value of the securityGroups property.
     * 
     * @param value
     *     allowed object is
     *     {@link SecurityGroupUnion }
     *     
     */
    public void setSecurityGroups(SecurityGroupUnion value) {
        this.securityGroups = value;
    }

    /**
     * Gets the value of the portForwardings property.
     * 
     * @return
     *     possible object is
     *     {@link PortForwardingUnion }
     *     
     */
    public PortForwardingUnion getPortForwardings() {
        return portForwardings;
    }

    /**
     * Sets the value of the portForwardings property.
     * 
     * @param value
     *     allowed object is
     *     {@link PortForwardingUnion }
     *     
     */
    public void setPortForwardings(PortForwardingUnion value) {
        this.portForwardings = value;
    }

    /**
     * Gets the value of the eips property.
     * 
     * @return
     *     possible object is
     *     {@link EipUnion }
     *     
     */
    public EipUnion getEips() {
        return eips;
    }

    /**
     * Sets the value of the eips property.
     * 
     * @param value
     *     allowed object is
     *     {@link EipUnion }
     *     
     */
    public void setEips(EipUnion value) {
        this.eips = value;
    }

    /**
     * Gets the value of the lbs property.
     * 
     * @return
     *     possible object is
     *     {@link LbUnion }
     *     
     */
    public LbUnion getLbs() {
        return lbs;
    }

    /**
     * Sets the value of the lbs property.
     * 
     * @param value
     *     allowed object is
     *     {@link LbUnion }
     *     
     */
    public void setLbs(LbUnion value) {
        this.lbs = value;
    }

    /**
     * Gets the value of the backupStorages property.
     * 
     * @return
     *     possible object is
     *     {@link BackupStorageUnion }
     *     
     */
    public BackupStorageUnion getBackupStorages() {
        return backupStorages;
    }

    /**
     * Sets the value of the backupStorages property.
     * 
     * @param value
     *     allowed object is
     *     {@link BackupStorageUnion }
     *     
     */
    public void setBackupStorages(BackupStorageUnion value) {
        this.backupStorages = value;
    }

    /**
     * Gets the value of the images property.
     * 
     * @return
     *     possible object is
     *     {@link ImageUnion }
     *     
     */
    public ImageUnion getImages() {
        return images;
    }

    /**
     * Sets the value of the images property.
     * 
     * @param value
     *     allowed object is
     *     {@link ImageUnion }
     *     
     */
    public void setImages(ImageUnion value) {
        this.images = value;
    }

    /**
     * Gets the value of the vm property.
     * 
     * @return
     *     possible object is
     *     {@link VmUnion }
     *     
     */
    public VmUnion getVm() {
        return vm;
    }

    /**
     * Sets the value of the vm property.
     * 
     * @param value
     *     allowed object is
     *     {@link VmUnion }
     *     
     */
    public void setVm(VmUnion value) {
        this.vm = value;
    }

    /**
     * Gets the value of the instanceOfferings property.
     * 
     * @return
     *     possible object is
     *     {@link InstanceOfferingUnion }
     *     
     */
    public InstanceOfferingUnion getInstanceOfferings() {
        return instanceOfferings;
    }

    /**
     * Sets the value of the instanceOfferings property.
     * 
     * @param value
     *     allowed object is
     *     {@link InstanceOfferingUnion }
     *     
     */
    public void setInstanceOfferings(InstanceOfferingUnion value) {
        this.instanceOfferings = value;
    }

    /**
     * Gets the value of the diskOffering property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the diskOffering property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDiskOffering().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DiskOfferingConfig }
     * 
     * 
     */
    public List<DiskOfferingConfig> getDiskOffering() {
        if (diskOffering == null) {
            diskOffering = new ArrayList<DiskOfferingConfig>();
        }
        return this.diskOffering;
    }

    /**
     * Gets the value of the account property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the account property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAccount().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AccountConfig }
     * 
     * 
     */
    public List<AccountConfig> getAccount() {
        if (account == null) {
            account = new ArrayList<AccountConfig>();
        }
        return this.account;
    }

    /**
     * Gets the value of the dns property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the dns property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDns().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DnsConfig }
     * 
     * 
     */
    public List<DnsConfig> getDns() {
        if (dns == null) {
            dns = new ArrayList<DnsConfig>();
        }
        return this.dns;
    }

}
