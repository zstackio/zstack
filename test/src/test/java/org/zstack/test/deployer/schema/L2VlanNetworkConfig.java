
package org.zstack.test.deployer.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for L2VlanNetworkConfig complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="L2VlanNetworkConfig">
 *   &lt;complexContent>
 *     &lt;extension base="{http://zstack.org/schema/zstack}L2NoVlanNetworkConfig">
 *       &lt;attribute name="vlan" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "L2VlanNetworkConfig")
public class L2VlanNetworkConfig
    extends L2NoVlanNetworkConfig
{

    @XmlAttribute(name = "vlan", required = true)
    @XmlSchemaType(name = "unsignedInt")
    protected long vlan;

    /**
     * Gets the value of the vlan property.
     * 
     */
    public long getVlan() {
        return vlan;
    }

    /**
     * Sets the value of the vlan property.
     * 
     */
    public void setVlan(long value) {
        this.vlan = value;
    }

}
