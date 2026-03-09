package org.zstack.header.volume.block;

import org.zstack.header.volume.VolumeVO_;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * @author shenjin
 * @date 2023/6/13 16:54
 */
@StaticMetamodel(BlockVolumeVO.class)
public class BlockVolumeVO_ extends VolumeVO_ {
    public static volatile SingularAttribute<BlockVolumeVO, String> iscsiPath;
    public static volatile SingularAttribute<BlockVolumeVO, String> vendor;
}
