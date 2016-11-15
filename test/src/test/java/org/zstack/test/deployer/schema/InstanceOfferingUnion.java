
package org.zstack.test.deployer.schema;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for InstanceOfferingUnion complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="InstanceOfferingUnion">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="instanceOffering" type="{http://zstack.org/schema/zstack}InstanceOfferingConfig" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="virtualRouterOffering" type="{http://zstack.org/schema/zstack}VirtualRouterOfferingConfig" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="convergedOffering" type="{http://zstack.org/schema/zstack}ConvergedOfferingConfig" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "InstanceOfferingUnion", propOrder = {
    "instanceOffering",
    "virtualRouterOffering",
    "convergedOffering"
})
public class InstanceOfferingUnion {

    protected List<InstanceOfferingConfig> instanceOffering;
    protected List<VirtualRouterOfferingConfig> virtualRouterOffering;
    protected List<ConvergedOfferingConfig> convergedOffering;

    /**
     * Gets the value of the instanceOffering property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the instanceOffering property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getInstanceOffering().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link InstanceOfferingConfig }
     * 
     * 
     */
    public List<InstanceOfferingConfig> getInstanceOffering() {
        if (instanceOffering == null) {
            instanceOffering = new ArrayList<InstanceOfferingConfig>();
        }
        return this.instanceOffering;
    }

    /**
     * Gets the value of the virtualRouterOffering property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the virtualRouterOffering property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getVirtualRouterOffering().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link VirtualRouterOfferingConfig }
     * 
     * 
     */
    public List<VirtualRouterOfferingConfig> getVirtualRouterOffering() {
        if (virtualRouterOffering == null) {
            virtualRouterOffering = new ArrayList<VirtualRouterOfferingConfig>();
        }
        return this.virtualRouterOffering;
    }

    /**
     * Gets the value of the convergedOffering property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the convergedOffering property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getConvergedOffering().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ConvergedOfferingConfig }
     * 
     * 
     */
    public List<ConvergedOfferingConfig> getConvergedOffering() {
        if (convergedOffering == null) {
            convergedOffering = new ArrayList<ConvergedOfferingConfig>();
        }
        return this.convergedOffering;
    }

}
