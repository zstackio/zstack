package org.zstack.ldap.entity;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.log.NoLogging;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = LdapServerVO.class)
@PythonClassInventory
public class LdapServerInventory implements Serializable {
    private String uuid;
    private String name;
    private String type;
    private String description;
    private String url;
    private String base;
    private String username;
    private String serverType;

    @APINoSee
    @NoLogging
    private String password;
    private String encryption;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    private String filter;
    private String usernameProperty;
    private String createAccountStrategy;
    private String deleteAccountStrategy;

    public static LdapServerInventory valueOf(LdapServerVO vo) {
        LdapServerInventory inv = new LdapServerInventory();
        inv.setUuid(vo.getUuid());
        inv.setName(vo.getResourceName());
        inv.setDescription(vo.getDescription());
        inv.setUrl(vo.getUrl());
        inv.setBase(vo.getBase());
        inv.setUsername(vo.getUsername());
        inv.setPassword(vo.getPassword());
        inv.setEncryption(vo.getEncryption());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setType(vo.getType());
        inv.setServerType(vo.getServerType().toString());
        inv.setFilter(vo.getFilter());
        inv.setUsernameProperty(vo.getUsernameProperty());
        inv.setCreateAccountStrategy(vo.getCreateAccountStrategy().toString());
        inv.setDeleteAccountStrategy(vo.getDeleteAccountStrategy().toString());
        return inv;
    }

    public static List<LdapServerInventory> valueOf(Collection<LdapServerVO> vos) {
        List<LdapServerInventory> lst = new ArrayList<>(vos.size());
        for (LdapServerVO vo : vos) {
            lst.add(LdapServerInventory.valueOf(vo));
        }
        return lst;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEncryption() {
        return encryption;
    }

    public void setEncryption(String encryption) {
        this.encryption = encryption;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getServerType() {
        return serverType;
    }

    public void setServerType(String serverType) {
        this.serverType = serverType;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getUsernameProperty() {
        return usernameProperty;
    }

    public void setUsernameProperty(String usernameProperty) {
        this.usernameProperty = usernameProperty;
    }

    public String getCreateAccountStrategy() {
        return createAccountStrategy;
    }

    public void setCreateAccountStrategy(String createAccountStrategy) {
        this.createAccountStrategy = createAccountStrategy;
    }

    public String getDeleteAccountStrategy() {
        return deleteAccountStrategy;
    }

    public void setDeleteAccountStrategy(String deleteAccountStrategy) {
        this.deleteAccountStrategy = deleteAccountStrategy;
    }
}
