
package org.zstack.test.deployer.schema;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ImageConfig complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ImageConfig">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="backupStorageRef" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded"/>
 *         &lt;element name="accountRef" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="description" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="size" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="actualSize" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="md5sum" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="bits" type="{http://www.w3.org/2001/XMLSchema}unsignedShort" default="64" />
 *       &lt;attribute name="url" type="{http://www.w3.org/2001/XMLSchema}string" default="http://zstack.org/download/test.qcow2" />
 *       &lt;attribute name="mediaType" type="{http://www.w3.org/2001/XMLSchema}string" default="RootVolumeTemplate" />
 *       &lt;attribute name="platform" type="{http://www.w3.org/2001/XMLSchema}string" default="Linux" />
 *       &lt;attribute name="format" type="{http://www.w3.org/2001/XMLSchema}string" default="qcow2" />
 *       &lt;attribute name="guestOsType" type="{http://www.w3.org/2001/XMLSchema}string" default="centos63" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ImageConfig", propOrder = {
    "backupStorageRef",
    "accountRef"
})
public class ImageConfig {

    @XmlElement(required = true)
    protected List<String> backupStorageRef;
    protected String accountRef;
    @XmlAttribute(name = "name", required = true)
    protected String name;
    @XmlAttribute(name = "description")
    protected String description;
    @XmlAttribute(name = "size")
    protected String size;
    @XmlAttribute(name = "actualSize")
    protected String actualSize;
    @XmlAttribute(name = "md5sum")
    protected String md5Sum;
    @XmlAttribute(name = "bits")
    @XmlSchemaType(name = "unsignedShort")
    protected Integer bits;
    @XmlAttribute(name = "url")
    protected String url;
    @XmlAttribute(name = "mediaType")
    protected String mediaType;
    @XmlAttribute(name = "platform")
    protected String platform;
    @XmlAttribute(name = "format")
    protected String format;
    @XmlAttribute(name = "guestOsType")
    protected String guestOsType;

    /**
     * Gets the value of the backupStorageRef property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the backupStorageRef property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getBackupStorageRef().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getBackupStorageRef() {
        if (backupStorageRef == null) {
            backupStorageRef = new ArrayList<String>();
        }
        return this.backupStorageRef;
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

    /**
     * Gets the value of the size property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSize() {
        return size;
    }

    /**
     * Sets the value of the size property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSize(String value) {
        this.size = value;
    }

    /**
     * Gets the value of the actualSize property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getActualSize() {
        return actualSize;
    }

    /**
     * Sets the value of the actualSize property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setActualSize(String value) {
        this.actualSize = value;
    }

    /**
     * Gets the value of the md5Sum property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMd5Sum() {
        return md5Sum;
    }

    /**
     * Sets the value of the md5Sum property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMd5Sum(String value) {
        this.md5Sum = value;
    }

    /**
     * Gets the value of the bits property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public int getBits() {
        if (bits == null) {
            return  64;
        } else {
            return bits;
        }
    }

    /**
     * Sets the value of the bits property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setBits(Integer value) {
        this.bits = value;
    }

    /**
     * Gets the value of the url property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUrl() {
        if (url == null) {
            return "http://zstack.org/download/test.qcow2";
        } else {
            return url;
        }
    }

    /**
     * Sets the value of the url property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUrl(String value) {
        this.url = value;
    }

    /**
     * Gets the value of the mediaType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMediaType() {
        if (mediaType == null) {
            return "RootVolumeTemplate";
        } else {
            return mediaType;
        }
    }

    /**
     * Sets the value of the mediaType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMediaType(String value) {
        this.mediaType = value;
    }

    /**
     * Gets the value of the platform property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPlatform() {
        if (platform == null) {
            return "Linux";
        } else {
            return platform;
        }
    }

    /**
     * Sets the value of the platform property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPlatform(String value) {
        this.platform = value;
    }

    /**
     * Gets the value of the format property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFormat() {
        if (format == null) {
            return "qcow2";
        } else {
            return format;
        }
    }

    /**
     * Sets the value of the format property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFormat(String value) {
        this.format = value;
    }

    /**
     * Gets the value of the guestOsType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGuestOsType() {
        if (guestOsType == null) {
            return "centos63";
        } else {
            return guestOsType;
        }
    }

    /**
     * Sets the value of the guestOsType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGuestOsType(String value) {
        this.guestOsType = value;
    }

}
