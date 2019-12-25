package org.zstack.header.identity;

import org.zstack.header.message.NeedJsonSchema;

import java.util.Date;

/**
 * Created by xing5 on 2016/3/21.
 */
public class IdentityCanonicalEvents {
    public static final String ACCOUNT_DELETED_PATH = "/account/delete";
    public static final String USER_DELETED_PATH = "/user/delete";
    public static final String ACCOUNT_LOGIN_PATH = "/account/login";
    public static final String SESSION_FORCE_LOGOUT_PATH = "/session/logout";

    @NeedJsonSchema
    public static class SessionForceLogoutData {
        private String sessionUuid;
        private String accountUuid;
        private String userUuid;
        private Date date = new Date();

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public String getSessionUuid() {
            return sessionUuid;
        }

        public void setSessionUuid(String sessionUuid) {
            this.sessionUuid = sessionUuid;
        }

        public String getAccountUuid() {
            return accountUuid;
        }

        public void setAccountUuid(String accountUuid) {
            this.accountUuid = accountUuid;
        }

        public String getUserUuid() {
            return userUuid;
        }

        public void setUserUuid(String userUuid) {
            this.userUuid = userUuid;
        }
    }

    @NeedJsonSchema
    public static class AccountDeletedData {
        private String accountUuid;
        private AccountInventory inventory;
        private Date date = new Date();

        public String getAccountUuid() {
            return accountUuid;
        }

        public void setAccountUuid(String accountUuid) {
            this.accountUuid = accountUuid;
        }

        public AccountInventory getInventory() {
            return inventory;
        }

        public void setInventory(AccountInventory inventory) {
            this.inventory = inventory;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }
    }

    @NeedJsonSchema
    public static class UserDeletedData {
        private String userUuid;
        private UserInventory inventory;
        private Date date = new Date();

        public String getUserUuid() {
            return userUuid;
        }

        public void setUserUuid(String userUuid) {
            this.userUuid = userUuid;
        }

        public UserInventory getInventory() {
            return inventory;
        }

        public void setInventory(UserInventory inventory) {
            this.inventory = inventory;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }
    }

    @NeedJsonSchema
    public static class AccountLoginData {
        private String accountUuid;
        private String userUuid;

        public String getAccountUuid() {
            return accountUuid;
        }

        public void setAccountUuid(String accountUuid) {
            this.accountUuid = accountUuid;
        }

        public String getUserUuid() {
            return userUuid;
        }

        public void setUserUuid(String userUuid) {
            this.userUuid = userUuid;
        }
    }
}
