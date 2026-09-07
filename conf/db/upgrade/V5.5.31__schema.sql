-- SUG-2795: listener-level TCP IPVS data plane and forward mode.
CALL ADD_COLUMN('LoadBalancerListenerVO', 'data_plane', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('LoadBalancerListenerVO', 'forward_mode', 'VARCHAR(32)', 1, NULL);

UPDATE `zstack`.`LoadBalancerListenerVO`
SET data_plane = 'ipvs'
WHERE protocol = 'udp' AND data_plane IS NULL;

UPDATE `zstack`.`LoadBalancerListenerVO`
SET data_plane = 'haproxy'
WHERE data_plane IS NULL;
