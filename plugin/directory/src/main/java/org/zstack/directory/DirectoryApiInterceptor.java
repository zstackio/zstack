package org.zstack.directory;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
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
    //TODO: fix hardcode
    private static final List<String> ALLOW_RESOURCE_TYPES = asList("VmInstanceVO");

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
        if (!ALLOW_RESOURCE_TYPES.containsAll(types)) {
            throw new ApiMessageInterceptionException(argerr("some resources cannot be bound to the directory"));
        }
        for (String type : types) {
            DirectoryChecker checker = factory.directoryCheckers.get(type);
            boolean result = checker.check(msg.getDirectoryUuid(), msg.getResourceUuids());
            if (!result) {
                // TODO: The error message needs to be concretized
                throw new ApiMessageInterceptionException(argerr("the operation failed, the resource and directory zones are different"));
            }
        }
    }

    private void validate(APIMoveResourcesToDirectoryMsg msg) {
        List<String> types = Q.New(ResourceVO.class).select(ResourceVO_.resourceType).in(ResourceVO_.uuid, msg.getResourceUuids()).listValues();
        types = types.stream().distinct().collect(Collectors.toList());
        if (!ALLOW_RESOURCE_TYPES.containsAll(types)) {
            //TODO：The error message needs to be concretized
            throw new ApiMessageInterceptionException(argerr("some resources cannot be bound to the directory"));
        }
        for (String type : types) {
            DirectoryChecker checker = factory.directoryCheckers.get(type);
            boolean result = checker.check(msg.getDirectoryUuid(), msg.getResourceUuids());
            if (!result) {
                throw new ApiMessageInterceptionException(argerr("the operation failed, the resource and directory zones are different"));
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
        if (!ALLOW_RESOURCE_TYPES.containsAll(types)) {
            throw new ApiMessageInterceptionException(argerr("some resources cannot be bound to the directory"));
        }
        for (String type : types) {
            DirectoryChecker checker = factory.directoryCheckers.get(type);
            boolean result = checker.check(msg.getDirectoryUuid(), msg.getResourceUuids());
            if (!result) {
                throw new ApiMessageInterceptionException(argerr("the operation failed, the resource and directory zones are different"));
            }
        }
        List<String> resourceUuids = msg.getResourceUuids();
        List<ResourceDirectoryRefVO> resources = Q.New(ResourceDirectoryRefVO.class).in(ResourceDirectoryRefVO_.resourceUuid,resourceUuids).list();
        if (!resources.isEmpty()) {
            //TODO:The error message needs to be concretized
            throw new ApiMessageInterceptionException(argerr("one of the resources has already been bound to directory, multiple paths are not supported"));
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
