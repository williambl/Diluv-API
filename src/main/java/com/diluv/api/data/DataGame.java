package com.diluv.api.data;

import java.util.Collections;
import java.util.List;

import com.diluv.confluencia.database.record.GameRecord;
import com.google.gson.annotations.Expose;

/**
 * Represents the data for a game that we support.
 */
public class DataGame {

    /**
     * The slug for the game. This is a unique string that identifies the game in URLs and API
     * requests.
     */
    @Expose
    private final String slug;

    /**
     * The display name for the game.
     */
    @Expose
    private final String name;

    /**
     * A URL that links to the official home page of the game.
     */
    @Expose
    private final String url;


    /**
     * A URL that links to the banner of the game.
     */
    @Expose
    private final String bannerURL;

    @Expose
    private final List<DataGameVersion> versions;

    public DataGame (GameRecord rs) {

        this(rs, null);
    }

    public DataGame (GameRecord rs, List<DataGameVersion> versions) {

        this.slug = rs.getSlug();
        this.name = rs.getName();
        this.url = rs.getUrl();
        this.bannerURL = rs.getBannerURL();
        this.versions = versions;
    }
}