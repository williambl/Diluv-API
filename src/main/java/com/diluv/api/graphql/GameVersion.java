package com.diluv.api.graphql;

import com.diluv.confluencia.database.record.GameVersionsEntity;

public class GameVersion {

    private long id;
    private String version;
    private String type;
    private String releasedAt;
    private GameVersionsEntity entity;

    public GameVersion (GameVersionsEntity entity) {

        this.id = entity.getId();
        this.version = entity.getVersion();
        this.type = entity.getType();
        this.releasedAt = entity.getReleasedAt().toLocalDateTime().toString();
        this.entity = entity;
    }

    public GameVersionsEntity getEntity () {

        return entity;
    }
}
