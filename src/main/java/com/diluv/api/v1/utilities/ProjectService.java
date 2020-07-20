package com.diluv.api.v1.utilities;

import java.util.List;
import java.util.Optional;

import com.diluv.api.provider.ResponseException;
import com.diluv.api.utils.auth.tokens.Token;
import com.diluv.api.utils.error.ErrorMessage;
import com.diluv.confluencia.database.record.ProjectAuthorsEntity;
import com.diluv.confluencia.database.record.ProjectsEntity;

import static com.diluv.api.Main.DATABASE;

public class ProjectService {

    public static ProjectsEntity getAuthorizedProject (String gameSlug, String projectTypeSlug, String projectSlug, Token token) throws ResponseException {

        final ProjectsEntity projectRecord = DATABASE.projectDAO.findOneProjectByGameSlugAndProjectTypeSlugAndProjectSlug(gameSlug, projectTypeSlug, projectSlug);
        if (projectRecord == null) {
            if (DATABASE.gameDAO.findOneBySlug(gameSlug) == null) {
                throw new ResponseException(ErrorMessage.NOT_FOUND_GAME.respond());
            }

            if (DATABASE.projectDAO.findOneProjectTypeByGameSlugAndProjectTypeSlug(gameSlug, projectTypeSlug) == null) {
                throw new ResponseException(ErrorMessage.NOT_FOUND_PROJECT_TYPE.respond());
            }

            throw new ResponseException(ErrorMessage.NOT_FOUND_PROJECT.respond());
        }

        if (!projectRecord.isReleased()) {
            if (token == null) {
                throw new ResponseException(ErrorMessage.NOT_FOUND_PROJECT.respond());
            }

            if (token.getUserId() == projectRecord.getOwner().getId()) {
                return projectRecord;
            }
            List<ProjectAuthorsEntity> records = projectRecord.getAuthors();
            Optional<ProjectAuthorsEntity> record = records.stream().filter(r -> r.getUser().getId() == token.getUserId()).findAny();

            if (record.isPresent()) {
                return projectRecord;
            }

            throw new ResponseException(ErrorMessage.NOT_FOUND_PROJECT.respond());
        }


        return projectRecord;
    }
}
