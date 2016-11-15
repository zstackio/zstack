
package org.zstack.test.deployer.schema;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for L3BasicNetworkConfig complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="L3BasicNetworkConfig">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="ipRange" type="{http://zstack.org/schema/zstack}IpRangeConfig" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="dns" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="dnsDomain" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="networkService" type="{http://zstack.org/schema/zstack}NetworkServiceConfig" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="accountRef" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
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
@XmlType(name = "L3BasicNetworkConfig", propOrder = {
    "ipRange",
    "dns",
    "dnsDomain",
    "networkService",
    "accountRef"
})
public class L3BasicNetworkConfig {

    protected List<IpRangeConfig> ipRange;
    protected List<String> dns;
    protected String dnsDomain;
    protected List<NetworkServiceConfig> networkService;
    protected String accountRef;
    @XmlAttribute(name = "name", required = true)
    protected String name;
    @XmlAttribute(name = "description")
    protected String description;

    /**
     * Gets the value of the ipRange property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the ipRange property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getIpRange().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link IpRangeConfig }
     * 
     * 
     */
    public List<IpRangeConfig> getIpRange() {
        if (ipRange == null) {
            ipRange = new ArrayList<IpRangeConfig>();
        }
        return this.ipRange;
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
     * {@link String }
     * 
     * 
     */
    public List<String> getDns() {
        if (dns == null) {
            dns = new ArrayList<String>();
        }
        return this.dns;
    }

    /**
     * Gets the value of the dnsDomain property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDnsDomain() {
        return dnsDomain;
    }

    /**
     * Sets the value of the dnsDomain property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDnsDomain(String value) {
        this.dnsDomain = value;
    }

    /**
     * Gets the value of the networkService property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the networkService property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getNetworkService().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link NetworkServiceConfig }
     * 
     * 
     */
    public List<NetworkServiceConfig> getNetworkService() {
        if (networkService == null) {
            networkService = new ArrayList<NetworkServiceConfig>();
        }
        return this.networkService;
    }

    /**
     * Gets the value of the accountRef property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAccountRef() {
        return accountRef;
    }

    /**
     * Sets the value of the accountRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAccountRef(String value) {
        this.accountRef = value;
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
