package com.diluv.api.endpoints.v1.user.domain;

import com.diluv.api.database.record.UserRecord;

public class UserDomain {
    private final String username;
    private final String avatarUrl;

    public UserDomain (UserRecord userRecord) {

        this.username = userRecord.getUsername();
        this.avatarUrl = userRecord.getAvatarUrl();
    }

    public String getUsername () {

        return username;
    }

    public String getAvatarUrl () {

        return avatarUrl;
    }
}