
package org.zstack.test.deployer.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for CephPrimaryStorageConfig complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="CephPrimaryStorageConfig">
 *   &lt;complexContent>
 *     &lt;extension base="{http://zstack.org/schema/zstack}PrimaryStorageConfigBase">
 *       &lt;attribute name="fsid" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="monUrl" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CephPrimaryStorageConfig")
public class CephPrimaryStorageConfig
    extends PrimaryStorageConfigBase
{

    @XmlAttribute(name = "fsid")
    protected String fsid;
    @XmlAttribute(name = "monUrl")
    protected String monUrl;

    /**
     * Gets the value of the fsid property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFsid() {
        return fsid;
    }

    /**
     * Sets the value of the fsid property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFsid(String value) {
        this.fsid = value;
    }

    /**
     * Gets the value of the monUrl property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMonUrl() {
        return monUrl;
    }

    /**
     * Sets the value of the monUrl property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMonUrl(String value) {
        this.monUrl = value;
    }

}
