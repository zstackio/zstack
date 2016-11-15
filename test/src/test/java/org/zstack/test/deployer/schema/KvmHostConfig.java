
package org.zstack.test.deployer.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for KvmHostConfig complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="KvmHostConfig">
 *   &lt;complexContent>
 *     &lt;extension base="{http://zstack.org/schema/zstack}HostConfigBase">
 *       &lt;attribute name="username" type="{http://www.w3.org/2001/XMLSchema}string" default="root" />
 *       &lt;attribute name="password" type="{http://www.w3.org/2001/XMLSchema}string" default="password" />
 *       &lt;attribute name="memoryCapacity" type="{http://www.w3.org/2001/XMLSchema}string" default="8G" />
 *       &lt;attribute name="cpuNum" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" default="8" />
 *       &lt;attribute name="cpuSpeed" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" default="3000" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "KvmHostConfig")
public class KvmHostConfig
    extends HostConfigBase
{

    @XmlAttribute(name = "username")
    protected String username;
    @XmlAttribute(name = "password")
    protected String password;
    @XmlAttribute(name = "memoryCapacity")
    protected String memoryCapacity;
    @XmlAttribute(name = "cpuNum")
    @XmlSchemaType(name = "unsignedInt")
    protected Long cpuNum;
    @XmlAttribute(name = "cpuSpeed")
    @XmlSchemaType(name = "unsignedInt")
    protected Long cpuSpeed;

    /**
     * Gets the value of the username property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUsername() {
        if (username == null) {
            return "root";
        } else {
            return username;
        }
    }

    /**
     * Sets the value of the username property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUsername(String value) {
        this.username = value;
    }

    /**
     * Gets the value of the password property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPassword() {
        if (password == null) {
            return "password";
        } else {
            return password;
        }
    }

    /**
     * Sets the value of the password property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPassword(String value) {
        this.password = value;
    }

    /**
     * Gets the value of the memoryCapacity property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMemoryCapacity() {
        if (memoryCapacity == null) {
            return "8G";
        } else {
            return memoryCapacity;
        }
    }

    /**
     * Sets the value of the memoryCapacity property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMemoryCapacity(String value) {
        this.memoryCapacity = value;
    }

    /**
     * Gets the value of the cpuNum property.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public long getCpuNum() {
        if (cpuNum == null) {
            return  8L;
        } else {
            return cpuNum;
        }
    }

    /**
     * Sets the value of the cpuNum property.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setCpuNum(Long value) {
        this.cpuNum = value;
    }

    /**
     * Gets the value of the cpuSpeed property.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public long getCpuSpeed() {
        if (cpuSpeed == null) {
            return  3000L;
        } else {
            return cpuSpeed;
        }
    }

    /**
     * Sets the value of the cpuSpeed property.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setCpuSpeed(Long value) {
        this.cpuSpeed = value;
    }

}
