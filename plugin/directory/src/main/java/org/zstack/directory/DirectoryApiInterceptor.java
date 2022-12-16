package org.zstack.directory;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIMessage;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.argerr;

/**
 * @author shenjin
 * @date 2022/12/8 11:31
 */
public class DirectoryApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DirectoryFactory factory;

    private static final List<String> NOT_SUPPORTED_SPECIAL_CHARACTER = asList("+", "'", "`", "/");
    //indicates the resource types that the directory allows to bind
    private static final List<String> ALLOW_RESOURCE_TYPES = asList(VmInstanceVO.class.getSimpleName());

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateDirectoryMsg) {
            validate((APICreateDirectoryMsg) msg);
        } else if (msg instanceof APIUpdateDirectoryMsg) {
            validate((APIUpdateDirectoryMsg) msg);
        } else if (msg instanceof APIAddResourcesToDirectoryMsg) {
            validate((APIAddResourcesToDirectoryMsg) msg);
        } else if (msg instanceof APIMoveResourcesToDirectoryMsg) {
            validate((APIMoveResourcesToDirectoryMsg) msg);
        } else if (msg instanceof APIRemoveResourcesFromDirectoryMsg) {
            validate((APIRemoveResourcesFromDirectoryMsg) msg);
        }
        setServiceId(msg);
        return msg;
    }

    private void validate(APIRemoveResourcesFromDirectoryMsg msg) {
        List<String> types = Q.New(ResourceVO.class).select(ResourceVO_.resourceType).in(ResourceVO_.uuid, msg.getResourceUuids()).listValues();
        types = types.stream().distinct().collect(Collectors.toList());
        checkType(types);
        for (String type : types) {
            DirectoryChecker checker = factory.directoryCheckers.get(type);
            ErrorCode result = checker.check(msg.getDirectoryUuid(), msg.getResourceUuids());
            if (result != null) {
                throw new ApiMessageInterceptionException(result);
            }
        }
    }

    private void validate(APIMoveResourcesToDirectoryMsg msg) {
        List<String> types = Q.New(ResourceVO.class).select(ResourceVO_.resourceType).in(ResourceVO_.uuid, msg.getResourceUuids()).listValues();
        types = types.stream().distinct().collect(Collectors.toList());
        checkType(types);
        for (String type : types) {
            DirectoryChecker checker = factory.directoryCheckers.get(type);
            ErrorCode result = checker.check(msg.getDirectoryUuid(), msg.getResourceUuids());
            if (result != null) {
                throw new ApiMessageInterceptionException(result);
            }
        }
    }

    private void setServiceId(APIMessage msg) {
        if (msg instanceof DirectoryMessage) {
            DirectoryMessage dmsg = (DirectoryMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, DirectoryManager.SERVICE_ID, dmsg.getDirectoryUuid());
        }
    }

    private void validate(APIAddResourcesToDirectoryMsg msg) {
        List<String> types = Q.New(ResourceVO.class).select(ResourceVO_.resourceType).in(ResourceVO_.uuid, msg.getResourceUuids()).listValues();
        types = types.stream().distinct().collect(Collectors.toList());
        checkType(types);
        for (String type : types) {
            DirectoryChecker checker = factory.directoryCheckers.get(type);
            ErrorCode result = checker.check(msg.getDirectoryUuid(), msg.getResourceUuids());
            if (result != null) {
                throw new ApiMessageInterceptionException(result);
            }
        }
        List<String> resourceUuids = msg.getResourceUuids();
        List<ResourceDirectoryRefVO> resources = Q.New(ResourceDirectoryRefVO.class).in(ResourceDirectoryRefVO_.resourceUuid,resourceUuids).list();
        if (!resources.isEmpty()) {
            List<String> list = resources.stream().map(ResourceDirectoryRefVO::getResourceUuid).collect(Collectors.toList());
            throw new ApiMessageInterceptionException(argerr("resources %s has already been bound to directory uuid[%s] , multiple paths are not supported", list, msg.getDirectoryUuid()));
        }
    }

    private void checkType(List<String> types) {
        if (!ALLOW_RESOURCE_TYPES.containsAll(types)) {
            List<String> list = types.stream().filter(s -> !ALLOW_RESOURCE_TYPES.contains(s)).collect(Collectors.toList());
            throw new ApiMessageInterceptionException(argerr("resource types %s are not supported by directory, allowed types are %s", list, ALLOW_RESOURCE_TYPES));
        }
    }

    private void validate(APIUpdateDirectoryMsg msg) {
        Optional opt = NOT_SUPPORTED_SPECIAL_CHARACTER.stream().filter(msg.getName()::contains).findAny();
        if (opt.isPresent()) {
            throw new ApiMessageInterceptionException(argerr("name can not contain those characters %s", NOT_SUPPORTED_SPECIAL_CHARACTER));
        }
    }

    private void validate(APICreateDirectoryMsg msg) {
        //judge whether special characters are included
        Optional opt = NOT_SUPPORTED_SPECIAL_CHARACTER.stream().filter(msg.getName()::contains).findAny();
        if (opt.isPresent()) {
            throw new ApiMessageInterceptionException(argerr("name can not contain those characters %s", NOT_SUPPORTED_SPECIAL_CHARACTER));
        }
    }
}
