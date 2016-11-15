
package org.zstack.test.deployer.schema;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for HostUnion complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="HostUnion">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="simulatorHost" type="{http://zstack.org/schema/zstack}SimulatorHostConfig" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="kvmHost" type="{http://zstack.org/schema/zstack}KvmHostConfig" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "HostUnion", propOrder = {
    "simulatorHost",
    "kvmHost"
})
public class HostUnion {

    protected List<SimulatorHostConfig> simulatorHost;
    protected List<KvmHostConfig> kvmHost;

    /**
     * Gets the value of the simulatorHost property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the simulatorHost property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSimulatorHost().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SimulatorHostConfig }
     * 
     * 
     */
    public List<SimulatorHostConfig> getSimulatorHost() {
        if (simulatorHost == null) {
            simulatorHost = new ArrayList<SimulatorHostConfig>();
        }
        return this.simulatorHost;
    }

    /**
     * Gets the value of the kvmHost property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the kvmHost property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getKvmHost().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link KvmHostConfig }
     * 
     * 
     */
    public List<KvmHostConfig> getKvmHost() {
        if (kvmHost == null) {
            kvmHost = new ArrayList<KvmHostConfig>();
        }
        return this.kvmHost;
    }

}
