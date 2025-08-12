DELETE FROM UserTagVO WHERE uuid = 'a4de80903e57422699fb05bd367a3cb4';

CALL ADD_COLUMN('PciDeviceSpecVO', 'allowResourceConfigWithMultipleDevices', 'tinyint(1)', 0, '1');
