
package org.zstack.test.deployer.schema;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for L2NetworkUnion complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="L2NetworkUnion">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="l2NoVlanNetwork" type="{http://zstack.org/schema/zstack}L2NoVlanNetworkConfig" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="l2VlanNetwork" type="{http://zstack.org/schema/zstack}L2VlanNetworkConfig" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "L2NetworkUnion", propOrder = {
    "l2NoVlanNetwork",
    "l2VlanNetwork"
})
public class L2NetworkUnion {

    protected List<L2NoVlanNetworkConfig> l2NoVlanNetwork;
    protected List<L2VlanNetworkConfig> l2VlanNetwork;

    /**
     * Gets the value of the l2NoVlanNetwork property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the l2NoVlanNetwork property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getL2NoVlanNetwork().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link L2NoVlanNetworkConfig }
     * 
     * 
     */
    public List<L2NoVlanNetworkConfig> getL2NoVlanNetwork() {
        if (l2NoVlanNetwork == null) {
            l2NoVlanNetwork = new ArrayList<L2NoVlanNetworkConfig>();
        }
        return this.l2NoVlanNetwork;
    }

    /**
     * Gets the value of the l2VlanNetwork property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the l2VlanNetwork property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getL2VlanNetwork().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link L2VlanNetworkConfig }
     * 
     * 
     */
    public List<L2VlanNetworkConfig> getL2VlanNetwork() {
        if (l2VlanNetwork == null) {
            l2VlanNetwork = new ArrayList<L2VlanNetworkConfig>();
        }
        return this.l2VlanNetwork;
    }

}
