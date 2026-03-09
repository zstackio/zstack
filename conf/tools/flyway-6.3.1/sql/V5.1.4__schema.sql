CALL ADD_COLUMN('OAuth2ClientVO', 'scope', 'varchar(255)', 1, 'openid');
CALL ADD_COLUMN('OAuth2ClientVO', 'identityProvider', 'varchar(32)', 1, 'default');