
package org.zstack.test.deployer.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for LocalPrimaryStorageConfig complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="LocalPrimaryStorageConfig">
 *   &lt;complexContent>
 *     &lt;extension base="{http://zstack.org/schema/zstack}PrimaryStorageConfigBase">
 *       &lt;attribute name="placeHolder" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LocalPrimaryStorageConfig")
public class LocalPrimaryStorageConfig
    extends PrimaryStorageConfigBase
{

    @XmlAttribute(name = "placeHolder")
    protected String placeHolder;

    /**
     * Gets the value of the placeHolder property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPlaceHolder() {
        return placeHolder;
    }

    /**
     * Sets the value of the placeHolder property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPlaceHolder(String value) {
        this.placeHolder = value;
    }

}
