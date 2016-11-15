
package org.zstack.test.deployer.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for IscsiFileSystemPrimaryStorageConfig complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="IscsiFileSystemPrimaryStorageConfig">
 *   &lt;complexContent>
 *     &lt;extension base="{http://zstack.org/schema/zstack}PrimaryStorageConfigBase">
 *       &lt;attribute name="chapUsername" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="chapPassword" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="hostname" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="sshUsername" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="sshPassword" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="filesystemType" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "IscsiFileSystemPrimaryStorageConfig")
public class IscsiFileSystemPrimaryStorageConfig
    extends PrimaryStorageConfigBase
{

    @XmlAttribute(name = "chapUsername")
    protected String chapUsername;
    @XmlAttribute(name = "chapPassword")
    protected String chapPassword;
    @XmlAttribute(name = "hostname")
    protected String hostname;
    @XmlAttribute(name = "sshUsername")
    protected String sshUsername;
    @XmlAttribute(name = "sshPassword")
    protected String sshPassword;
    @XmlAttribute(name = "filesystemType")
    protected String filesystemType;

    /**
     * Gets the value of the chapUsername property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getChapUsername() {
        return chapUsername;
    }

    /**
     * Sets the value of the chapUsername property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setChapUsername(String value) {
        this.chapUsername = value;
    }

    /**
     * Gets the value of the chapPassword property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getChapPassword() {
        return chapPassword;
    }

    /**
     * Sets the value of the chapPassword property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setChapPassword(String value) {
        this.chapPassword = value;
    }

    /**
     * Gets the value of the hostname property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * Sets the value of the hostname property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setHostname(String value) {
        this.hostname = value;
    }

    /**
     * Gets the value of the sshUsername property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSshUsername() {
        return sshUsername;
    }

    /**
     * Sets the value of the sshUsername property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSshUsername(String value) {
        this.sshUsername = value;
    }

    /**
     * Gets the value of the sshPassword property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSshPassword() {
        return sshPassword;
    }

    /**
     * Sets the value of the sshPassword property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSshPassword(String value) {
        this.sshPassword = value;
    }

    /**
     * Gets the value of the filesystemType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFilesystemType() {
        return filesystemType;
    }

    /**
     * Sets the value of the filesystemType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFilesystemType(String value) {
        this.filesystemType = value;
    }

}
