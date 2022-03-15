UPDATE GlobalConfigVO set value = 'LocalEncryption' where category ='encrypt' and name = 'enable.password.encrypt' and value = 'true';
UPDATE GlobalConfigVO set value = 'None' where category ='encrypt' and name = 'enable.password.encrypt' and value = 'false';
