UPDATE GlobalConfigVO set defaultValue = 'none' where category ='kvm' and name = 'vm.cacheMode';
UPDATE GlobalConfigVO set value = 'none' where category ='kvm' and name = 'vm.cacheMode' and value = '0';
UPDATE GlobalConfigVO set value = 'writethrough' where category ='kvm' and name = 'vm.cacheMode' and value = '1';
UPDATE GlobalConfigVO set value = 'writeback' where category ='kvm' and name = 'vm.cacheMode' and value = '2';
