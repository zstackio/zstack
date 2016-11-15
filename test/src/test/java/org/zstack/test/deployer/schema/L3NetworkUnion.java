
package org.zstack.test.deployer.schema;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for L3NetworkUnion complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="L3NetworkUnion">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="l3BasicNetwork" type="{http://zstack.org/schema/zstack}L3BasicNetworkConfig" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "L3NetworkUnion", propOrder = {
    "l3BasicNetwork"
})
public class L3NetworkUnion {

    protected List<L3BasicNetworkConfig> l3BasicNetwork;

    /**
     * Gets the value of the l3BasicNetwork property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the l3BasicNetwork property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getL3BasicNetwork().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link L3BasicNetworkConfig }
     * 
     * 
     */
    public List<L3BasicNetworkConfig> getL3BasicNetwork() {
        if (l3BasicNetwork == null) {
            l3BasicNetwork = new ArrayList<L3BasicNetworkConfig>();
        }
        return this.l3BasicNetwork;
    }

}
