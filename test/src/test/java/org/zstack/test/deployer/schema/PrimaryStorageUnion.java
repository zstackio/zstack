
package org.zstack.test.deployer.schema;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for PrimaryStorageUnion complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="PrimaryStorageUnion">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="simulatorPrimaryStorage" type="{http://zstack.org/schema/zstack}SimulatorPrimaryStorageConfig" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="nfsPrimaryStorage" type="{http://zstack.org/schema/zstack}NfsPrimaryStorageConfig" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="iscsiBtrfsPrimaryStorage" type="{http://zstack.org/schema/zstack}IscsiFileSystemPrimaryStorageConfig" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="localPrimaryStorage" type="{http://zstack.org/schema/zstack}LocalPrimaryStorageConfig" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="cephPrimaryStorage" type="{http://zstack.org/schema/zstack}CephPrimaryStorageConfig" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="sharedMountPointPrimaryStorage" type="{http://zstack.org/schema/zstack}SharedMountPointPrimaryStorageConfig" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PrimaryStorageUnion", propOrder = {
    "simulatorPrimaryStorage",
    "nfsPrimaryStorage",
    "iscsiBtrfsPrimaryStorage",
    "localPrimaryStorage",
    "cephPrimaryStorage",
    "sharedMountPointPrimaryStorage"
})
public class PrimaryStorageUnion {

    protected List<SimulatorPrimaryStorageConfig> simulatorPrimaryStorage;
    protected List<NfsPrimaryStorageConfig> nfsPrimaryStorage;
    protected List<IscsiFileSystemPrimaryStorageConfig> iscsiBtrfsPrimaryStorage;
    protected List<LocalPrimaryStorageConfig> localPrimaryStorage;
    protected List<CephPrimaryStorageConfig> cephPrimaryStorage;
    protected List<SharedMountPointPrimaryStorageConfig> sharedMountPointPrimaryStorage;

    /**
     * Gets the value of the simulatorPrimaryStorage property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the simulatorPrimaryStorage property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSimulatorPrimaryStorage().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SimulatorPrimaryStorageConfig }
     * 
     * 
     */
    public List<SimulatorPrimaryStorageConfig> getSimulatorPrimaryStorage() {
        if (simulatorPrimaryStorage == null) {
            simulatorPrimaryStorage = new ArrayList<SimulatorPrimaryStorageConfig>();
        }
        return this.simulatorPrimaryStorage;
    }

    /**
     * Gets the value of the nfsPrimaryStorage property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the nfsPrimaryStorage property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getNfsPrimaryStorage().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link NfsPrimaryStorageConfig }
     * 
     * 
     */
    public List<NfsPrimaryStorageConfig> getNfsPrimaryStorage() {
        if (nfsPrimaryStorage == null) {
            nfsPrimaryStorage = new ArrayList<NfsPrimaryStorageConfig>();
        }
        return this.nfsPrimaryStorage;
    }

    /**
     * Gets the value of the iscsiBtrfsPrimaryStorage property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the iscsiBtrfsPrimaryStorage property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getIscsiBtrfsPrimaryStorage().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link IscsiFileSystemPrimaryStorageConfig }
     * 
     * 
     */
    public List<IscsiFileSystemPrimaryStorageConfig> getIscsiBtrfsPrimaryStorage() {
        if (iscsiBtrfsPrimaryStorage == null) {
            iscsiBtrfsPrimaryStorage = new ArrayList<IscsiFileSystemPrimaryStorageConfig>();
        }
        return this.iscsiBtrfsPrimaryStorage;
    }

    /**
     * Gets the value of the localPrimaryStorage property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the localPrimaryStorage property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getLocalPrimaryStorage().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link LocalPrimaryStorageConfig }
     * 
     * 
     */
    public List<LocalPrimaryStorageConfig> getLocalPrimaryStorage() {
        if (localPrimaryStorage == null) {
            localPrimaryStorage = new ArrayList<LocalPrimaryStorageConfig>();
        }
        return this.localPrimaryStorage;
    }

    /**
     * Gets the value of the cephPrimaryStorage property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the cephPrimaryStorage property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCephPrimaryStorage().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CephPrimaryStorageConfig }
     * 
     * 
     */
    public List<CephPrimaryStorageConfig> getCephPrimaryStorage() {
        if (cephPrimaryStorage == null) {
            cephPrimaryStorage = new ArrayList<CephPrimaryStorageConfig>();
        }
        return this.cephPrimaryStorage;
    }

    /**
     * Gets the value of the sharedMountPointPrimaryStorage property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the sharedMountPointPrimaryStorage property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSharedMountPointPrimaryStorage().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SharedMountPointPrimaryStorageConfig }
     * 
     * 
     */
    public List<SharedMountPointPrimaryStorageConfig> getSharedMountPointPrimaryStorage() {
        if (sharedMountPointPrimaryStorage == null) {
            sharedMountPointPrimaryStorage = new ArrayList<SharedMountPointPrimaryStorageConfig>();
        }
        return this.sharedMountPointPrimaryStorage;
    }

}
