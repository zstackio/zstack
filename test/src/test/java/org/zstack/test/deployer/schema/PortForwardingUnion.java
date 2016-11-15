
package org.zstack.test.deployer.schema;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for PortForwardingUnion complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="PortForwardingUnion">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="portForwarding" type="{http://zstack.org/schema/zstack}PortForwardingConfig" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PortForwardingUnion", propOrder = {
    "portForwarding"
})
public class PortForwardingUnion {

    protected List<PortForwardingConfig> portForwarding;

    /**
     * Gets the value of the portForwarding property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the portForwarding property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPortForwarding().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link PortForwardingConfig }
     * 
     * 
     */
    public List<PortForwardingConfig> getPortForwarding() {
        if (portForwarding == null) {
            portForwarding = new ArrayList<PortForwardingConfig>();
        }
        return this.portForwarding;
    }

}
