package com.diluv.api.database;

import java.util.List;

import com.diluv.api.database.dao.GameDAO;
import com.diluv.api.database.record.GameRecord;
import com.diluv.api.utils.FileReader;
import com.fasterxml.jackson.core.JsonProcessingException;

public class GameTestDatabase implements GameDAO {

    private final List<GameRecord> gameRecords;

    public GameTestDatabase () {

        this.gameRecords = FileReader.readJsonFolder("games", GameRecord.class);
    }

    @Override
    public List<GameRecord> findAll () {

        return this.gameRecords;
    }

    @Override
    public GameRecord findOneBySlug (String slug) {

        for (GameRecord userRecord : this.gameRecords) {
            if (userRecord.getSlug().equals(slug)) {
                return userRecord;
            }
        }
        return null;
    }
}