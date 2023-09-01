/*
 * Copyright (c) 2013 Juniper Networks, Inc. All rights reserved.
 */

package org.zstack.sugonSdnController.controller.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Iterator;
import java.util.Random;
import java.lang.reflect.Field;
import java.io.ObjectInputStream;
import java.io.InputStream;
import org.zstack.sugonSdnController.controller.api.types.VirtualMachine;
import org.zstack.sugonSdnController.controller.api.types.VirtualMachineInterface;
import org.zstack.sugonSdnController.controller.api.types.MacAddressesType;
import org.zstack.sugonSdnController.controller.api.types.VirtualNetwork;
import org.zstack.sugonSdnController.controller.api.types.NetworkPolicy;
import org.zstack.sugonSdnController.controller.api.types.Project;
import org.zstack.sugonSdnController.controller.api.types.Domain;
import org.zstack.sugonSdnController.controller.api.types.SecurityGroup;
import org.zstack.sugonSdnController.controller.api.types.InstanceIp;
import org.zstack.sugonSdnController.controller.api.types.FloatingIp;
import org.zstack.sugonSdnController.controller.api.types.FloatingIpPool;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.RandomStringUtils;
import com.google.common.net.InetAddresses;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class ApiConnectorMock implements ApiConnector {
    private static final CLogger s_logger =
            Utils.getLogger(ApiConnector.class);

    private ApiBuilder _apiBuilder;
    private HashMap<String, List<HashMap<String, ApiObjectBase>>> _map;
    private static final HashMap<Class<?extends ApiObjectBase>, Class<?extends ApiObjectBase>> _parentMap;
    static {
        HashMap<Class<?extends ApiObjectBase>, Class<?extends ApiObjectBase>> parentMap  = new HashMap<Class<?extends ApiObjectBase>, Class<?extends ApiObjectBase>>();
        parentMap.put(Domain.class, Domain.class);
        parentMap.put(Project.class, Domain.class);
        parentMap.put(VirtualNetwork.class, Project.class);
        parentMap.put(VirtualMachineInterface.class, VirtualMachine.class);
        parentMap.put(NetworkPolicy.class, Project.class);
        parentMap.put(SecurityGroup.class, Project.class);
        parentMap.put(FloatingIp.class, FloatingIpPool.class);
        parentMap.put(FloatingIpPool.class, VirtualNetwork.class);
        _parentMap = parentMap;
    }

    private static void assignAutoProperty(ApiObjectBase obj) {
        if (obj.getClass() == VirtualMachineInterface.class) {
            if (((VirtualMachineInterface)obj).getMacAddresses() != null) {
                return;
            }
            String addr = RandomStringUtils.random(17, false, true);
            char[] charArray = addr.toCharArray();
            charArray[2] = charArray[5] = charArray[5] = ':';
            charArray[8] = charArray[11] = charArray[14] = ':';
            addr = new String(charArray);

            MacAddressesType macs = new MacAddressesType();
            macs.addMacAddress(addr);
            s_logger.debug("Assigned auto property mac address : " + addr);
            ((VirtualMachineInterface)obj).setMacAddresses(macs);
        } else if (obj.getClass() == InstanceIp.class) {
            if (((InstanceIp)obj).getAddress() != null) {
               return;
            }
            Random random = new Random();
            String ipString = InetAddresses.fromInteger(random.nextInt()).getHostAddress();
            s_logger.debug("Assigned auto property ip address : " + ipString);
            ((InstanceIp)obj).setAddress(ipString);
        }
    }

    private static Class<?extends ApiObjectBase> getVncClass(String clsname) {
        String typename = new String();
        for (int i = 0; i < clsname.length(); i++) {
            char ch = clsname.charAt(i);
            if (i == 0) {
                ch = Character.toUpperCase(ch);
            } else if (ch == '_') {
                ch = clsname.charAt(++i);
                ch = Character.toUpperCase(ch);
            }
            typename += ch;
        }
        try {
            Class<?extends ApiObjectBase> cls = (Class<?extends ApiObjectBase>)Class.forName("net.juniper.contrail.api.types." + typename);
            return cls;
        } catch (Exception e) {
            s_logger.debug("Class not found <net.juniper.contrail.api.types." + typename + ">" + e);
        }
        return null;
    }

    private static HashMap<Class<?extends ApiObjectBase>, ApiObjectBase> _defaultObjectMap;

    ApiConnectorMock() {
        _apiBuilder = new ApiBuilder();
        initConfig();
    }

    public ApiConnectorMock(String hostname, int port) {
    this();
    }

    public void initConfig() {
        _map = new HashMap<String, List<HashMap<String, ApiObjectBase>>>();
        buildDefaultConfig();
        buildDefaultObjectMap();
    }

    void buildDefaultConfig() {
        try {
            InputStream fin = getClass().getResourceAsStream("/default_config");
            ObjectInputStream ois = new ObjectInputStream(fin);
            HashMap<Class<?extends ApiObjectBase>,  List<HashMap<String, ApiObjectBase>>> defaultConfigMap = (HashMap<Class<?extends ApiObjectBase>, List<HashMap<String, ApiObjectBase>>>) ois.readObject();
            Iterator it = defaultConfigMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry)it.next();
                Class<?extends ApiObjectBase> cls = (Class<?extends ApiObjectBase>)pairs.getKey();
                s_logger.debug("buildDefaultConfig: " + _apiBuilder.getTypename(cls));
                _map.put(_apiBuilder.getTypename(cls), (List<HashMap<String, ApiObjectBase>>)pairs.getValue());
            }
        } catch (Exception e) {
                s_logger.debug("buildDefaultConfig: " + e);
        }
    }

    void buildDefaultObjectMap() {
        _defaultObjectMap = new HashMap<Class<?extends ApiObjectBase>, ApiObjectBase>();
        try {
            _defaultObjectMap.put(Domain.class, findByFQN(Domain.class, "default-domain"));
            _defaultObjectMap.put(Project.class, findByFQN(Project.class, "default-domain:default-project"));
        } catch (Exception e) {
            s_logger.debug(e.getMessage());
        }
    }

    List getClassData(Class<?> cls) {
        final String typename = _apiBuilder.getTypename(cls);
        return _map.get(typename);
    }

    void setClassData(Class<?> cls, List clsData) {
        final String typename = _apiBuilder.getTypename(cls);
        _map.put(typename, clsData);
    }

    HashMap<String, ApiObjectBase> getFqnMap(List clsData) {
       if (clsData != null) {
           return (HashMap<String, ApiObjectBase>)clsData.get(1);
       }
       return null;
    }

    HashMap<String, ApiObjectBase> getUuidMap(List clsData) {
       if (clsData != null) {
           return (HashMap<String, ApiObjectBase>)clsData.get(0);
       }
       return null;
    }

    private String getFqnString(List<String> name_list) {
       return StringUtils.join(name_list, ':');
    }

    private boolean validate(ApiObjectBase obj) throws IOException {
        String uuid = obj.getUuid();
        if (uuid == null) {
           uuid = UUID.randomUUID().toString();
           obj.setUuid(uuid);
        }
        String fqn = getFqnString(obj.getQualifiedName());
        List clsData = getClassData(obj.getClass());
        HashMap<String, ApiObjectBase> uuidMap = null;
        HashMap<String, ApiObjectBase> fqnMap = null;
        if (clsData != null) {
            uuidMap = getUuidMap(clsData);
            fqnMap = getFqnMap(clsData);
            if (uuidMap.get(uuid) != null || fqnMap.get(fqn) != null) {
                s_logger.warn("api object: " + obj.getName() + " already exists");
                return false;
            }
        }
        //check for parent child references and backrefs
        ApiObjectBase parent = obj.getParent();
        if (parent == null) {
            parent = getDefaultParent(obj);
        }
        if (parent != null) {
            try {
                s_logger.debug("Verify establish parent(" + _apiBuilder.getTypename(parent.getClass()) + ", " + parent.getName()
                         + ") => child (" + _apiBuilder.getTypename(obj.getClass()) + ", " + obj.getName());
                /* update parent object with new child info */
                updateObjectVerify(parent, obj, getRefname(obj.getClass()) + "s");
                /* update child object back reference to its parent */
                s_logger.debug("Verify Establish child(" + _apiBuilder.getTypename(obj.getClass()) + ", " + obj.getName()
                    + ") => backref to parent(" + _apiBuilder.getTypename(parent.getClass()) + ", " + parent.getName() + ")");
            } catch (Exception e) {
                s_logger.debug("Exception in updateObject : " + e);
                return false;
            }
        } else {
            s_logger.debug("no default parent for : " + obj.getName());
        }
        try {
            /* update object references it has with associated back refs */
            updateRefsVerify(obj);
        } catch (Exception e) {
           return false;
        }
        return true;
    }

    @Override
    public ApiConnector credentials(String username, String password) {
        return this;
    }
    @Override
    public ApiConnector tenantName(String tenant) {
        return this;
    }
    @Override
    public ApiConnector authToken(String token) {
        return this;
    }
    @Override
    public ApiConnector authServer(String type, String url) {
        return this;
    }
    @Override
    public synchronized Status create(ApiObjectBase obj) throws IOException {
        s_logger.debug("create(cls, obj): " + _apiBuilder.getTypename(obj.getClass()) + ", " + obj.getName());
        if (!validate(obj)) {
            s_logger.error("can not create (cls, obj): " + _apiBuilder.getTypename(obj.getClass()) + ", "
                           + obj.getName() + ", validate failed");
            return Status.failure("Validation failed");
        }
        String uuid = obj.getUuid();
        if (uuid == null) {
           uuid = UUID.randomUUID().toString();
           obj.setUuid(uuid);
        }
        String fqn = getFqnString(obj.getQualifiedName());
        List clsData = getClassData(obj.getClass());
        HashMap<String, ApiObjectBase> uuidMap = null;
        HashMap<String, ApiObjectBase> fqnMap = null;
        if (clsData == null) {
            clsData = new ArrayList<HashMap<String, ApiObjectBase>>();
            uuidMap = new HashMap<String, ApiObjectBase>();
            fqnMap = new HashMap<String, ApiObjectBase>();
            clsData.add(uuidMap);
            clsData.add(fqnMap);
            setClassData(obj.getClass(), clsData);
        } else {
            uuidMap = getUuidMap(clsData);
            fqnMap = getFqnMap(clsData);
        }
        if (uuidMap.get(uuid) != null || fqnMap.get(fqn) != null) {
            s_logger.warn("api object: " + obj.getName() + " already exists");
            return Status.failure("Object already exists");
        }
        uuidMap.put(uuid, obj);
        fqnMap.put(fqn, obj);

        //update parent child references and backrefs
        ApiObjectBase parent = obj.getParent();
        if (parent == null) {
            parent = getDefaultParent(obj);
        }
        if (parent != null) {
            try {
                s_logger.debug("Establish parent(" + _apiBuilder.getTypename(parent.getClass()) + ", " + parent.getName()
                         + ") => child (" + _apiBuilder.getTypename(obj.getClass()) + ", " + obj.getName() + ")");
                /* update parent object with new child info */
                updateObject(parent, obj, getRefname(obj.getClass()) + "s");
                obj.setParent(parent);
            } catch (Exception e) {
                s_logger.debug("Exception in updateObject : " + e);
            }
        } else {
            s_logger.debug("no default parent for : " + obj.getName());
        }
        /* update object references it has with associated back refs */
        updateRefs(obj);
        /* assign auto property, if any */
        assignAutoProperty(obj);
        return Status.success();
    }

    ApiObjectBase getDefaultParent(ApiObjectBase obj) {
        if (obj.getClass() == Domain.class) {
             return null;
        }
        if (obj.getName().equals("default-domain")) return null;
        Class<?extends ApiObjectBase> parentCls = _parentMap.get(obj.getClass());
        return _defaultObjectMap.get(parentCls);
    }

    @Override
    public synchronized Status commitDrafts(ApiObjectBase obj) throws IOException {
        s_logger.debug("commit drafts: " + _apiBuilder.getTypename(obj.getClass()) + ", " + obj.getUuid());
        return Status.success();
    }

    @Override
    public synchronized Status discardDrafts(ApiObjectBase obj) throws IOException {
        s_logger.debug("discard drafts: " + _apiBuilder.getTypename(obj.getClass()) + ", " + obj.getUuid());
        return Status.success();
    }

    @Override
    public synchronized Status update(ApiObjectBase obj) throws IOException {
        s_logger.debug("update(cls, obj): " + _apiBuilder.getTypename(obj.getClass()) + ", " + obj.getName());
        String fqn = getFqnString(obj.getQualifiedName());
        if (fqn == null) {
            return Status.failure("Object does not have qualified name.");
        }
        List clsData = getClassData(obj.getClass());
        if (clsData == null) {
            return Status.failure("No class data.");
        }
        HashMap<String, ApiObjectBase> uuidMap =  getUuidMap(clsData);
        HashMap<String, ApiObjectBase> fqnMap =  getFqnMap(clsData);
        ApiObjectBase old = fqnMap.get(fqn);
        String uuid = old.getUuid();
        fqnMap.put(fqn, obj);
        uuidMap.put(uuid, obj);
        return Status.success();
    }

    @Override
    public synchronized Status read(ApiObjectBase obj) throws IOException {
        s_logger.debug("read(cls, obj): " + _apiBuilder.getTypename(obj.getClass()) + ", " + obj.getName());
        String fqn = getFqnString(obj.getQualifiedName());
        if (fqn == null) {
            return Status.failure("Object does not have qualified name.");
        }
        List clsData = getClassData(obj.getClass());
        if (clsData == null) {
            return Status.failure("No class data.");
        }
        HashMap<String, ApiObjectBase> fqnMap =  getFqnMap(clsData);
        if (fqnMap == null || fqnMap.get(fqn) == null) {
            return Status.failure("No qualified name.");
        }
        obj = fqnMap.get(fqn);
        return Status.success();
    }

    @Override
    public Status delete(ApiObjectBase obj) throws IOException {
        s_logger.debug("delete(cls, obj): " + _apiBuilder.getTypename(obj.getClass()) + "," + obj.getName());
        if (isChildrenExists(obj)) {
            s_logger.warn("children exist, can not delete");
            return Status.failure("Object already exists.");
        }
        String uuid = obj.getUuid();
        String fqn = getFqnString(obj.getQualifiedName());
        if (fqn == null || uuid == null) {
            s_logger.debug("can not delete - no uuid/fqn");
            return Status.failure("UUID or FQN not specified.");
        }
        List clsData = getClassData(obj.getClass());
        if (clsData == null) {
            s_logger.debug("can not delete - not exists");
            return Status.success();
        }
        HashMap<String, ApiObjectBase> uuidMap =  getUuidMap(clsData);
        HashMap<String, ApiObjectBase> fqnMap =  getFqnMap(clsData);
        fqnMap.remove(fqn);
        uuidMap.remove(uuid);
        return Status.success();
    }

    @Override
    public synchronized Status delete(Class<? extends ApiObjectBase> cls, String uuid) throws IOException {
        s_logger.debug("delete(cls, uuid): " + _apiBuilder.getTypename(cls) + ", " + uuid);
        List clsData = getClassData(cls);
        if (clsData == null) {
            s_logger.debug("can not delete - not exists");
            return Status.success();
        }
        HashMap<String, ApiObjectBase> uuidMap =  getUuidMap(clsData);
        HashMap<String, ApiObjectBase> fqnMap =  getFqnMap(clsData);

        ApiObjectBase obj = uuidMap.get(uuid);
        if (obj != null && isChildrenExists(obj)) {
            String reason = "Cannot delete object having children.";
            s_logger.warn(reason);
            return Status.failure(reason);
        }
        uuidMap.remove(uuid);
        if (obj != null) {
            fqnMap.remove(getFqnString(obj.getQualifiedName()));
        }
        return Status.success();
    }

    @Override
    public synchronized ApiObjectBase find(Class<? extends ApiObjectBase> cls, ApiObjectBase parent, String name) throws IOException {
        s_logger.debug("find(cls, parent, name) : " +  _apiBuilder.getTypename(cls) + ", " + parent.getName() + ", " + name);
        List clsData = getClassData(cls);
        if (clsData == null) {
            s_logger.debug("not found");
            return null;
        }
        String fqn = getFqnString(parent.getQualifiedName()) + ":" + name;
        HashMap<String, ApiObjectBase> fqnMap =  getFqnMap(clsData);
        return fqnMap.get(fqn);
    }

    @Override
    public ApiObjectBase findByFQN(Class<? extends ApiObjectBase> cls, String fullName) throws IOException {
        s_logger.debug("findFQN(cls, fqn) : " +  _apiBuilder.getTypename(cls) + ", " + fullName);
        List clsData = getClassData(cls);
        if (clsData == null) {
            s_logger.debug("not found");
            return null;
        }
        HashMap<String, ApiObjectBase> fqnMap =  getFqnMap(clsData);
        return fqnMap.get(fullName);
    }

    @Override
    public synchronized ApiObjectBase findById(Class<? extends ApiObjectBase> cls, String uuid) throws IOException {
        s_logger.debug("findById(cls, uuid) : " +  _apiBuilder.getTypename(cls) + ", " + uuid);
        List clsData = getClassData(cls);
        if (clsData == null) {
            s_logger.debug("not found");
            return null;
        }
        HashMap<String, ApiObjectBase> uuidMap =  getUuidMap(clsData);
        return uuidMap.get(uuid);
    }

    @Override
    public String findByName(Class<? extends ApiObjectBase> cls, ApiObjectBase parent, String name) throws IOException {
        s_logger.debug("findByName(cls, parent, name) : " +  _apiBuilder.getTypename(cls) + ", " + name);
        List<String> name_list = new ArrayList<String>();
        if (parent != null) {
            name_list.addAll(parent.getQualifiedName());
        } else {
            try {
                name_list.addAll(cls.newInstance().getDefaultParent());
            } catch (Exception e) {
                s_logger.warn(e.getMessage());
            }
        }
        name_list.add(name);
        return findByName(cls, name_list);
    }

    @Override
    // POST http://hostname:port/fqname-to-id
    // body: {"type": class, "fq_name": [parent..., name]}
    public synchronized String findByName(Class<? extends ApiObjectBase> cls, List<String> name_list) throws IOException {
        String fqn = StringUtils.join(name_list, ':');
        s_logger.debug("findByName(cls, name_list) : " +  _apiBuilder.getTypename(cls) + ", " + fqn);
        List clsData = getClassData(cls);
        if (clsData == null) {
            s_logger.debug("cls not found");
            return null;
        }
        HashMap<String, ApiObjectBase> fqnMap =  getFqnMap(clsData);
        ApiObjectBase obj = fqnMap.get(fqn);
        if (obj != null) {
            return obj.getUuid();
        }
        s_logger.debug("not found");
        return null;
    }

    @Override
    public synchronized List<? extends ApiObjectBase> list(Class<? extends ApiObjectBase> cls, List<String> parent) throws IOException {
        String fqnParent = null;
        if (parent != null) {
            fqnParent = StringUtils.join(parent, ':');
            s_logger.debug("list(cls, parent_name_list) : " +  _apiBuilder.getTypename(cls) + ", " + fqnParent);
        } else {
            s_logger.debug("list(cls, parent_name_list) : " +  _apiBuilder.getTypename(cls) + ", null");
        }
        List clsData = getClassData(cls);
        if (clsData == null) {
            s_logger.debug("cls not found");
            return null;
        }
        HashMap<String, ApiObjectBase> fqnMap =  getFqnMap(clsData);
        ArrayList<ApiObjectBase> arr = new ArrayList<ApiObjectBase>(fqnMap.values());
        List<ApiObjectBase> list = new ArrayList<ApiObjectBase>();
        for (ApiObjectBase obj:arr) {
           if (fqnParent != null && getFqnString(obj.getQualifiedName()).startsWith(fqnParent + ":")) {
              list.add(obj);
           } else {
              list.add(obj);
           }
        }
        return list;
    }

    @Override
    public List<? extends ApiObjectBase> listWithDetail(Class<? extends ApiObjectBase> cls, String fields, String filters) throws IOException {
        return null;
    }

    private boolean isChildrenExists(ApiObjectBase parent) {
        String fqnParent = getFqnString(parent.getQualifiedName());
        ArrayList<List<HashMap<String, ApiObjectBase>>> clsDataList = new ArrayList<List<HashMap<String, ApiObjectBase>>>(_map.values());
        for (List<HashMap<String, ApiObjectBase>> clsData:clsDataList) {
           HashMap<String, ApiObjectBase> fqnMap =  getFqnMap(clsData);
           ArrayList<ApiObjectBase> arr = new ArrayList<ApiObjectBase>(fqnMap.values());
           List<ApiObjectBase> list = new ArrayList<ApiObjectBase>();
           for (ApiObjectBase obj:arr) {
               if (getFqnString(obj.getQualifiedName()).startsWith(fqnParent + ":")) {
                   if (obj.getParent() == parent) {
                       return true;
                   }
               }
           }
        }
        return false;
    }

    @Override
    public <T extends ApiPropertyBase> List<? extends ApiObjectBase>
        getObjects(Class<? extends ApiObjectBase> cls, List<ObjectReference<T>> refList) throws IOException {
        s_logger.debug("getObjects(cls, refList): " + _apiBuilder.getTypename(cls));
        List<ApiObjectBase> list = new ArrayList<ApiObjectBase>();
        for (ObjectReference<T> ref : refList) {
            ApiObjectBase obj = findById(cls, ref.getUuid());
            if (obj == null) {
                s_logger.warn("Unable to find element with uuid: " + ref.getUuid());
                continue;
            }
            list.add(obj);
        }
        return list;
    }

    public String getRefname(Class<?> cls) {
        String clsname = cls.getName();
        int loc = clsname.lastIndexOf('.');
        if (loc > 0) {
            clsname = clsname.substring(loc + 1);
        }
        String typename = new String();
        for (int i = 0; i < clsname.length(); i++) {
            char ch = clsname.charAt(i);
            if (Character.isUpperCase(ch)) {
                if (i > 0) {
                    typename += "_";
                }
                ch = Character.toLowerCase(ch);
            }
            typename += ch;
        }
        return typename;
    }

    private void updateRefsVerify(ApiObjectBase obj) throws IOException {
        Class<?> cls = obj.getClass();
        s_logger.debug("updateRefsVerify: " + obj.getName() + ", class: " + _apiBuilder.getTypename(cls));
        for (Field f : cls.getDeclaredFields()) {
            f.setAccessible(true);
            if (!f.getName().endsWith("_refs") || f.getName().endsWith("_back_refs")) {
                continue;
            }
            List<ObjectReference<ApiPropertyBase>> nv;
            try {
                nv = (List<ObjectReference<ApiPropertyBase>>)f.get(obj);
            } catch (Exception ex) {
                s_logger.warn("Unable to read value for " + f.getName() + ": " + ex.getMessage());
                throw new IOException("Unable to read value for " + f.getName() + ": " + ex.getMessage());
            }

            if (nv == null || nv.isEmpty()) {
                continue;
            }

            String refName = f.getName().substring(0, f.getName().lastIndexOf("_refs"));
            Class<?extends ApiObjectBase> refCls = getVncClass(refName);
            for (ObjectReference<ApiPropertyBase> ref: nv) {
                 String uuid = findByName(refCls, ref.getReferredName());
                 ApiObjectBase refObj = findById(refCls, uuid);
                 if (refObj == null) {
                    s_logger.debug("Can not find obj for class: " + _apiBuilder.getTypename(refCls)
                                          + ", uuid: " + ref.getUuid() +" , href: " + ref.getHRef());
                    throw new IOException("Obj " + obj.getName() + " has a reference of type<" + _apiBuilder.getTypename(refCls) + ", but object does not exist");
                 }
                 s_logger.debug("Verify establish backref on(cls, obj) : " + _apiBuilder.getTypename(refCls) + " => " + refObj.getName() + " with ref(cls, obj): " + _apiBuilder.getTypename(cls) + ", " + obj.getName());
                 updateObjectVerify(refObj, obj, getRefname(obj.getClass()) + "_back_refs");
            }
        }
        return;
    }
    private void updateRefs(ApiObjectBase obj) throws IOException {
        Class<?> cls = obj.getClass();
        s_logger.debug("updateRefs: " + obj.getName() + ", class: " + _apiBuilder.getTypename(cls));
        for (Field f : cls.getDeclaredFields()) {
            f.setAccessible(true);
            if (!f.getName().endsWith("_refs") || f.getName().endsWith("_back_refs")) {
                continue;
            }
            List<ObjectReference<ApiPropertyBase>> nv;
            try {
                nv = (List<ObjectReference<ApiPropertyBase>>)f.get(obj);
            } catch (Exception ex) {
                s_logger.warn("Unable to read value for " + f.getName() + ": " + ex.getMessage());
                continue;
            }

            if (nv == null || nv.isEmpty()) {
                s_logger.debug("no refs of type: " + f.getName());
                continue;
            }

            String refName = f.getName().substring(0, f.getName().lastIndexOf("_refs"));
            s_logger.debug("ref name: " + refName);
            Class<?extends ApiObjectBase> refCls = getVncClass(refName);
            for (ObjectReference<ApiPropertyBase> ref: nv) {
                 String uuid = findByName(refCls, ref.getReferredName());
                 updateField(ref, "uuid", uuid);
                 ApiObjectBase refObj = findById(refCls, uuid);
                 if (refObj == null) {
                    s_logger.debug("Can not find obj for class: " + _apiBuilder.getTypename(refCls)
                                          + ", uuid: " + ref.getUuid() +" , href: " + ref.getHRef());
                    throw new IOException("Obj " + obj.getName() + " has a reference of type<" + _apiBuilder.getTypename(refCls) + ", but object does not exist");
                 }
                 s_logger.debug("Establish backref on(cls, obj) : " + _apiBuilder.getTypename(refCls) + " => " + refObj.getName() + " with ref(cls, obj): " + _apiBuilder.getTypename(cls) + ", " + obj.getName());
                 updateObject(refObj, obj, getRefname(obj.getClass()) + "_back_refs");
            }
        }
        return;
    }

    private void updateField(ObjectReference<ApiPropertyBase> obj, String fieldName, String value)
    {
        Class<?> cls = obj.getClass();

        Field field = null;
        try {
            field = cls.getDeclaredField(fieldName);
            field.setAccessible(true);
        } catch (Exception e) {
            s_logger.debug("no field " + fieldName + ", \n" + e);
            return;
        }
        try {
             field.set(obj, value);
        } catch (Exception ex) {
             s_logger.warn("Unable to set " + field.getName() + ": " + ex.getMessage());
        }
        s_logger.debug("Updated " + fieldName + " to " + value + " \n" );
    }
    private void updateObjectVerify(ApiObjectBase obj, ApiObjectBase other, String fieldName) throws IOException {
        s_logger.debug("updateObjectVerify(cls, obj, other-cls, other, field): " + _apiBuilder.getTypename(obj.getClass()) + ", " + obj.getName() + "," + _apiBuilder.getTypename(other.getClass()) + ", " + other.getName() + "," + fieldName);
        Class<?> cls = obj.getClass();

        Field fRefs = null;
        try {
            fRefs = cls.getDeclaredField(fieldName);
            fRefs.setAccessible(true);
        } catch (Exception e) {
            s_logger.debug("no field " + fieldName + ", \n" + e);
            throw new IOException("no field " + fieldName + ", \n" + e);
        }
        List<ObjectReference<ApiPropertyBase>> nv;
        try {
             nv = (List<ObjectReference<ApiPropertyBase>>)fRefs.get(obj);
        } catch (Exception ex) {
             s_logger.warn("Unable to read new value for " + fRefs.getName() + ": " + ex.getMessage());
             throw new IOException("Unable to read new value for " + fRefs.getName() + ": " + ex.getMessage());
        }
        return;
    }

    private void updateObject(ApiObjectBase obj, ApiObjectBase other, String fieldName) throws IOException {
        s_logger.debug("updateObject(cls, obj, other-cls, other, field): " + _apiBuilder.getTypename(obj.getClass()) + ", " + obj.getName() + "," + _apiBuilder.getTypename(other.getClass()) + ", " + other.getName() + "," + fieldName);
        Class<?> cls = obj.getClass();

        Field fRefs = null;
        try {
            fRefs = cls.getDeclaredField(fieldName);
            fRefs.setAccessible(true);
        } catch (Exception e) {
            s_logger.debug("no field " + fieldName + ", \n" + e);
            return;
        }
        List<ObjectReference<ApiPropertyBase>> nv;
        try {
             nv = (List<ObjectReference<ApiPropertyBase>>)fRefs.get(obj);
        } catch (Exception ex) {
             s_logger.warn("Unable to read new value for " + fRefs.getName() + ": " + ex.getMessage());
             return;
        }

        if (nv == null) {
             nv = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }

        String href = "http://localhost:8082/" + _apiBuilder.getTypename(other.getClass()) + '/' + other.getUuid();
        ObjectReference<ApiPropertyBase> objRef = new ObjectReference<ApiPropertyBase>(other.getQualifiedName(), null, href, other.getUuid());
        nv.add(objRef);
        try {
             fRefs.set(obj, nv);
        } catch (Exception ex) {
             s_logger.warn("Unable to set " + fRefs.getName() + ": " + ex.getMessage());
        }
    }

    public void dumpConfig(Class<?extends ApiObjectBase> cls) throws Exception {
        List<?extends ApiObjectBase> list = list(cls, null);
        for (ApiObjectBase obj:list) {
            s_logger.debug("Class : " + _apiBuilder.getTypename(cls) + ", name: " + obj.getName());
        }
    }

    @Override
    public Status sync(String uri) throws IOException {
        return Status.success();
    }

    @Override
    public void dispose() {
    }
}

