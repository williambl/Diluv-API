package com.diluv.api.database;

import java.util.Collections;
import java.util.List;

import com.diluv.api.utils.FileReader;
import com.diluv.confluencia.database.dao.GameDAO;
import com.diluv.confluencia.database.record.GameRecord;
import com.diluv.confluencia.database.record.GameVersionRecord;
import com.diluv.confluencia.utils.Pagination;

public class GameTestDatabase implements GameDAO {

    private final List<GameRecord> gameRecords;

    public GameTestDatabase () {

        this.gameRecords = FileReader.readJsonFolder("games", GameRecord.class);
    }

    @Override
    public List<GameRecord> findAll (Pagination cursor, int limit) {

        return this.gameRecords;
    }

    @Override
    public List<GameVersionRecord> findAllGameVersionsByGameSlug (String gameSlug) {

        return Collections.emptyList();
    }

    @Override
    public GameRecord findOneBySlug (String slug) {

        for (final GameRecord userRecord : this.gameRecords) {
            if (userRecord.getSlug().equals(slug)) {
                return userRecord;
            }
        }
        return null;
    }
}
