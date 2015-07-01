import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.storage.primary.*;

/**
 * Created by frank on 6/30/2015.
 */
public class LocalStorageFactory implements PrimaryStorageFactory {
    public static PrimaryStorageType type = new PrimaryStorageType(LocalStorageConstants.LOCAL_STORAGE_TYPE);

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public PrimaryStorageType getPrimaryStorageType() {
        return type;
    }

    @Override
    public PrimaryStorageInventory createPrimaryStorage(PrimaryStorageVO vo, APIAddPrimaryStorageMsg msg) {
        vo = dbf.persistAndRefresh(vo);
        return PrimaryStorageInventory.valueOf(vo);
    }

    @Override
    public PrimaryStorage getPrimaryStorage(PrimaryStorageVO vo) {
        return new LocalStorageBase(vo);
    }

    @Override
    public PrimaryStorageInventory getInventory(String uuid) {
        return PrimaryStorageInventory.valueOf(dbf.findByUuid(uuid, PrimaryStorageVO.class));
    }
}
