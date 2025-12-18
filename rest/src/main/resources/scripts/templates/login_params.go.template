// Copyright (c) ZStack.io, Inc.

package param

type LoginByAccountParam struct {
	BaseParam
	LoginByAccount LoginByAccountDetailParam `json:"logInByAccount"`
}

type LoginByAccountDetailParam struct {
	AccountName string                 `json:"accountName"` // Account name
	Password    string                 `json:"password"`    // Password
	AccountType string                 `json:"accountType"` // Account type
	CaptchaUuid string                 `json:"captchaUuid"` // Captcha UUID
	VerifyCode  string                 `json:"verifyCode"`  // Verification code
	ClientInfo  map[string]interface{} `json:"clientInfo"`  // Client information
}

type LogInByUserParam struct {
	BaseParam
	LogInByUser LogInByUserDetailParam `json:"logInByUser"`
}

type LogInByUserDetailParam struct {
	AccountUuid string                 `json:"accountUuid"` // Account UUID
	AccountName string                 `json:"accountName"` // Account name
	UserName    string                 `json:"userName"`    // User name
	Password    string                 `json:"password"`    // Password
	ClientInfo  map[string]interface{} `json:"clientInfo"`  // Client information
}

type LoginIAM2VirtualIDWithLdapParam struct {
	BaseParam
	LoginIAM2VirtualIDWithLdap LoginIAM2VirtualIDWithLdapDetailParam `json:"loginIAM2VirtualIDWithLdap"`
}

type LoginIAM2VirtualIDWithLdapDetailParam struct {
	VirtualIDUuid string `json:"virtualIDUuid"` // Virtual ID UUID
	LdapUid       string `json:"ldapUid"`       // LDAP UID
	Password      string `json:"password"`      // Password
}

type LoginIAM2PlatformParam struct {
	BaseParam
	LoginIAM2Platform LoginIAM2PlatformDetailParam `json:"loginIAM2Platform"`
}

type LoginIAM2PlatformDetailParam struct {
	VirtualIDUuid string `json:"virtualIDUuid"` // Virtual ID UUID
	Password      string `json:"password"`      // Password
}

type ValidateSessionParam struct {
	BaseParam
}
