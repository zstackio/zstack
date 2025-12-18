// Copyright (c) ZStack.io, Inc.

package view

import "time"

// SessionInventoryView Session
type SessionInventoryView struct {
	Uuid        string    `json:"uuid"`
	AccountUuid string    `json:"accountUuid,omitempty"`
	UserUuid    string    `json:"userUuid,omitempty"`
	UserType    string    `json:"userType,omitempty"`
	ExpiredDate time.Time `json:"expiredDate,omitempty"`
	CreateDate  time.Time `json:"createDate,omitempty"`
}

// WebUISessionView Web UI Session
type WebUISessionView struct {
	SessionId       string `json:"sessionId"`       // Session ID
	AccountUuid     string `json:"accountUuid"`     // Account UUID
	UserUuid        string `json:"userUuid"`        // User UUID
	UserName        string `json:"username"`        // Username
	LoginType       string `json:"loginType"`       // Login type
	CurrentIdentity string `json:"currentIdentity"` // Current identity
	ZSVersion       string `json:"zsVersion"`       // ZStack Cloud version
}
