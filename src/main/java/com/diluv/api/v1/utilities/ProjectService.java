package com.diluv.api.v1.utilities;

import java.util.List;

import org.hibernate.Session;

import com.diluv.api.data.DataAuthorizedProject;
import com.diluv.api.data.DataProject;
import com.diluv.api.provider.ResponseException;
import com.diluv.api.utils.auth.tokens.Token;
import com.diluv.api.utils.error.ErrorMessage;
import com.diluv.api.utils.permissions.ProjectPermissions;
import com.diluv.confluencia.Confluencia;
import com.diluv.confluencia.database.record.ProjectsEntity;

public class ProjectService {

    public static DataProject getDataProject (Session session, String gameSlug, String projectTypeSlug, String projectSlug, Token token) throws ResponseException {

        final ProjectsEntity project = Confluencia.PROJECT.findOneProject(session, gameSlug, projectTypeSlug, projectSlug);
        if (project == null) {
            if (Confluencia.GAME.findOneBySlug(session, gameSlug) == null) {
                throw new ResponseException(ErrorMessage.NOT_FOUND_GAME.respond());
            }

            if (Confluencia.PROJECT.findOneProjectTypeByGameSlugAndProjectTypeSlug(session, gameSlug, projectTypeSlug) == null) {
                throw new ResponseException(ErrorMessage.NOT_FOUND_PROJECT_TYPE.respond());
            }

            throw new ResponseException(ErrorMessage.NOT_FOUND_PROJECT.respond());
        }

        return getDataProject(project, token);
    }

    public static DataProject getDataProject (Session session, long projectId, Token token) throws ResponseException {

        final ProjectsEntity project = Confluencia.PROJECT.findOneProjectByProjectId(session, projectId);

        if (project == null) {
            throw new ResponseException(ErrorMessage.NOT_FOUND_PROJECT.respond());
        }
        return getDataProject(project, token);
    }

    public static DataProject getDataProject (ProjectsEntity project, Token token) throws ResponseException {

        if (!project.isReleased()) {

            if (token == null)
                throw new ResponseException(ErrorMessage.NOT_FOUND_PROJECT.respond());

            List<String> permissions = ProjectPermissions.getAuthorizedUserPermissions(project, token);

            if (permissions != null) {
                return new DataAuthorizedProject(project, permissions);
            }

            throw new ResponseException(ErrorMessage.NOT_FOUND_PROJECT.respond());
        }

        return new DataProject(project);
    }
}
