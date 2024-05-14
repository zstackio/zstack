package org.zstack.core.config;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.schema.GuestOsCategory;
import org.zstack.core.config.schema.GuestOsCharacter;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.image.GuestOsCategoryVO;
import org.zstack.header.image.GuestOsCategoryVO_;
import org.zstack.utils.path.PathUtil;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class GuestOsHelper {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;

    private static final GuestOsHelper instance = new GuestOsHelper();

    public static GuestOsHelper getInstance() {
        return instance;
    }

    private static final String GUEST_OS_CATEGORY_FILE = "guestOs/guestOsCategory.xml";
    private static final String GUEST_OS_CHARACTER_FILE = "guestOs/guestOsCharacter.xml";
    private static final Map<String, GuestOsCategory.Config> allGuestOsCategory = new ConcurrentHashMap<>();
    private static final Map<String, GuestOsCharacter.Config> allGuestOsCharacter = new ConcurrentHashMap<>();

    public synchronized void initGuestOsRelatedDb() {
        initGuestOsCategory();
        initGuestOsCharacter();
    }

    public List<GuestOsCharacter.Config> getAllGuestOsCharacter() {
        return new ArrayList<>(allGuestOsCharacter.values());
    }

    public GuestOsCharacter.Config getGuestOsCharacter(String architecture, String platform, String osRelease) {
        return allGuestOsCharacter.get(String.format("%s_%s_%s", architecture, platform, osRelease));
    }

    private void initGuestOsCharacter() {
        GuestOsCharacter configs;
        File guestOsCharacterFile = PathUtil.findFileOnClassPath(GUEST_OS_CHARACTER_FILE);
        try {
            JAXBContext context = JAXBContext.newInstance("org.zstack.core.config.schema");
            Unmarshaller unmarshaller = context.createUnmarshaller();
            configs = (GuestOsCharacter) unmarshaller.unmarshal(guestOsCharacterFile);
        } catch (Exception e){
            throw new CloudRuntimeException(e);
        }
        for (GuestOsCharacter.Config config : configs.getOsInfo()) {
            validateGuestOsCharacter(config);
            allGuestOsCharacter.put(String.format("%s_%s_%s", config.getArchitecture(), config.getPlatform(), config.getOsRelease()), config);
        }
    }

    private void initGuestOsCategory() {
        GuestOsCategory configs;
        File guestOsCategoryFile = PathUtil.findFileOnClassPath(GUEST_OS_CATEGORY_FILE);
        try {
            JAXBContext context = JAXBContext.newInstance("org.zstack.core.config.schema");
            Unmarshaller unmarshaller = context.createUnmarshaller();
            configs = (GuestOsCategory) unmarshaller.unmarshal(guestOsCategoryFile);
        } catch (Exception e){
            throw new CloudRuntimeException(e);
        }
        for (GuestOsCategory.Config config : configs.getOsInfo()) {
            allGuestOsCategory.put(config.getOsRelease(), config);
            if (!Q.New(GuestOsCategoryVO.class).eq(GuestOsCategoryVO_.osRelease, config.getOsRelease()).isExists()) {
                GuestOsCategoryVO vo = new GuestOsCategoryVO();
                vo.setPlatform(config.getPlatform());
                vo.setName(config.getName());
                vo.setVersion(config.getVersion());
                vo.setOsRelease(config.getOsRelease());
                vo.setUuid(Platform.getUuid());
                dbf.persist(vo);
            }
        }

        //delete release not in config
        SQL.New(GuestOsCategoryVO.class).notIn(GuestOsCategoryVO_.osRelease, allGuestOsCategory.keySet()).delete();
    }

    private void validateGuestOsCharacter(GuestOsCharacter.Config config) {
        pluginRgty.getExtensionList(GuestOsExtensionPoint.class).forEach(ext -> ext.validateGuestOsCharacter(config));
    }
}
