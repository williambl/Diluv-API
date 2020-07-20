package com.diluv.api.v1.site;

import com.diluv.api.data.*;
import com.diluv.api.data.site.*;
import com.diluv.api.provider.ResponseException;
import com.diluv.api.utils.auth.JWTUtil;
import com.diluv.api.utils.auth.tokens.Token;
import com.diluv.api.utils.error.ErrorMessage;
import com.diluv.api.utils.permissions.ProjectPermissions;
import com.diluv.api.utils.query.AuthorProjectsQuery;
import com.diluv.api.utils.query.GameQuery;
import com.diluv.api.utils.query.ProjectFileQuery;
import com.diluv.api.utils.query.ProjectQuery;
import com.diluv.api.utils.response.ResponseUtil;
import com.diluv.api.v1.games.GamesAPI;
import com.diluv.api.v1.utilities.ProjectService;
import com.diluv.confluencia.database.record.*;
import com.diluv.confluencia.database.sort.GameSort;
import com.diluv.confluencia.database.sort.ProjectFileSort;
import com.diluv.confluencia.database.sort.ProjectSort;
import com.diluv.confluencia.database.sort.Sort;

import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.annotations.Query;
import org.jboss.resteasy.annotations.cache.Cache;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.List;
import java.util.stream.Collectors;

import static com.diluv.api.Main.DATABASE;

@GZIP
@Path("/site")
@Produces(MediaType.APPLICATION_JSON)
public class SiteAPI {

    @Cache(maxAge = 300, mustRevalidate = true)
    @GET
    @Path("/")
    public Response getIndex () {

        final List<FeaturedGamesEntity> gameRecords = DATABASE.gameDAO.findFeaturedGames();
        final List<DataSiteGame> games = gameRecords.stream().map(DataSiteGame::new).collect(Collectors.toList());

        final long projectCount = DATABASE.gameDAO.countAllProjectsBySlug("");
        final long userCount = DATABASE.userDAO.countAll();
        final long gameCount = DATABASE.gameDAO.countAllBySearch("");
        final long projectTypeCount = DATABASE.gameDAO.countAllProjectTypes();
        return ResponseUtil.successResponse(new DataSiteIndex(games, projectCount, userCount, gameCount, projectTypeCount));
    }

    @Cache(maxAge = 300, mustRevalidate = true)
    @GET
    @Path("/games")
    public Response getGames (@Query GameQuery query) {

        final long page = query.getPage();
        final int limit = query.getLimit();
        final Sort sort = query.getSort(GameSort.NAME);
        final String search = query.getSearch();

        final List<GamesEntity> gameRecords = DATABASE.gameDAO.findAll(page, limit, sort, search);

        final long gameCount = DATABASE.gameDAO.countAllBySearch(search);
        final List<DataBaseGame> games = gameRecords.stream().map(DataSiteGame::new).collect(Collectors.toList());
        return ResponseUtil.successResponse(new DataGameList(games, GamesAPI.GAME_SORTS, gameCount));
    }

    @Cache(maxAge = 300, mustRevalidate = true)
    @GET
    @Path("/games/{gameSlug}")
    public Response getGameDefaultType (@PathParam("gameSlug") String gameSlug) {

        final GamesEntity gameRecord = DATABASE.gameDAO.findOneBySlug(gameSlug);
        if (gameRecord == null) {

            return ErrorMessage.NOT_FOUND_GAME.respond();
        }

        return ResponseUtil.successResponse(gameRecord.getDefaultProjectTypeEntity().getSlug());
    }


    @GET
    @Path("/games/{gameSlug}/{projectTypeSlug}/projects")
    public Response getProjects (@PathParam("gameSlug") String gameSlug, @PathParam("projectTypeSlug") String projectTypeSlug, @Query ProjectQuery query) {

        final long page = query.getPage();
        final int limit = query.getLimit();
        final Sort sort = query.getSort(ProjectSort.POPULAR);
        final String search = query.getSearch();
        final String versions = query.getVersions();
        final String[] tags = query.getTags();

        final List<ProjectsEntity> projectRecords = DATABASE.projectDAO.findAllByGameAndProjectType(gameSlug, projectTypeSlug, search, page, limit, sort, versions, tags);

        GamesEntity game = DATABASE.gameDAO.findOneBySlug(gameSlug);
        if (projectRecords.isEmpty()) {

            if (game == null) {

                return ErrorMessage.NOT_FOUND_GAME.respond();
            }

            if (DATABASE.projectDAO.findOneProjectTypeByGameSlugAndProjectTypeSlug(gameSlug, projectTypeSlug) == null) {

                return ErrorMessage.NOT_FOUND_PROJECT_TYPE.respond();
            }
        }
        final List<DataBaseProject> projects = projectRecords.stream().map(DataBaseProject::new).collect(Collectors.toList());

        final List<DataBaseProjectType> types = game.getProjectTypes().stream().map(DataBaseProjectType::new).collect(Collectors.toList());
        final ProjectTypesEntity currentType = DATABASE.projectDAO.findOneProjectTypeByGameSlugAndProjectTypeSlug(gameSlug, projectTypeSlug);

        final long projectCount = DATABASE.projectDAO.countAllByGameSlugAndProjectTypeSlug(gameSlug, projectTypeSlug);

        return ResponseUtil.successResponse(new DataSiteGameProjects(projects, types, new DataProjectType(currentType, projectCount), GamesAPI.PROJECT_SORTS));
    }


    @Cache(maxAge = 30, mustRevalidate = true)
    @GET
    @Path("/projects/{gameSlug}/{projectTypeSlug}/{projectSlug}")
    public Response getProject (@HeaderParam("Authorization") Token token, @PathParam("gameSlug") String gameSlug, @PathParam("projectTypeSlug") String projectTypeSlug, @PathParam("projectSlug") String projectSlug) {

        final ProjectsEntity projectRecord = DATABASE.projectDAO.findOneProjectByGameSlugAndProjectTypeSlugAndProjectSlug(gameSlug, projectTypeSlug, projectSlug);
        if (projectRecord == null || !projectRecord.isReleased() && token == null) {
            return ErrorMessage.NOT_FOUND_PROJECT.respond();
        }

        if (token != null) {
            List<String> permissions = ProjectPermissions.getAuthorizedUserPermissions(projectRecord, token);

            if (permissions != null) {
                return ResponseUtil.successResponse(new DataProjectAuthorized(projectRecord, permissions));
            }
        }

        return ResponseUtil.successResponse(new DataProject(projectRecord));
    }

    @GET
    @Path("/games/{gameSlug}/{projectTypeSlug}/{projectSlug}/files")
    public Response getProjectFiles (@HeaderParam("Authorization") Token token, @PathParam("gameSlug") String gameSlug, @PathParam("projectTypeSlug") String projectTypeSlug, @PathParam("projectSlug") String projectSlug, @Query ProjectFileQuery query) throws ResponseException {

        long page = query.getPage();
        int limit = query.getLimit();
        Sort sort = query.getSort(ProjectFileSort.NEW);
        String version = query.getVersions();

        final ProjectsEntity projectRecord = ProjectService.getAuthorizedProject(gameSlug, projectTypeSlug, projectSlug, token);

        boolean authorized = ProjectPermissions.hasPermission(projectRecord, token, ProjectPermissions.FILE_UPLOAD);
        final List<ProjectFilesEntity> projectFileRecords = DATABASE.fileDAO.findAllByProjectId(projectRecord.getId(), authorized, page, limit, sort, version);

        final List<DataSiteProjectFileDisplay> projectFiles = projectFileRecords.stream().map(record -> {
            final List<GameVersionsEntity> gameVersionRecords = record.getGameVersions().stream().map(ProjectFileGameVersionsEntity::getGameVersion).collect(Collectors.toList());
            List<DataGameVersion> gameVersions = gameVersionRecords.stream().map(DataGameVersion::new).collect(Collectors.toList());
            return record.isReleased() ?
                new DataSiteProjectFileDisplay(record, gameVersions, gameSlug, projectTypeSlug, projectSlug) :
                new DataSiteProjectFileDisplay(record, gameVersions, gameSlug, projectTypeSlug, projectSlug);
        }).collect(Collectors.toList());
        return ResponseUtil.successResponse(new DataSiteProjectFilesPage(new DataBaseProject(projectRecord), projectFiles));
    }

    @Cache(maxAge = 300, mustRevalidate = true)
    @GET
    @Path("/author/{username}")
    public Response getUser (@PathParam("username") String username, @HeaderParam("Authorization") String auth, @Query AuthorProjectsQuery query) {

        final UsersEntity userRecord = DATABASE.userDAO.findOneByUsername(username);
        if (userRecord == null) {

            return ErrorMessage.NOT_FOUND_USER.respond();
        }


        boolean authorized = false;
        if (auth != null) {

            final Token token = JWTUtil.getToken(auth);

            if (token != null) {
                final UsersEntity tokenUser = DATABASE.userDAO.findOneByUserId(token.getUserId());

                if (tokenUser.getUsername().equalsIgnoreCase(username)) {
                    authorized = true;

                }
            }
        }
        List<ProjectsEntity> projects = DATABASE.projectDAO.findAllByUsername(username, authorized, query.getPage(), query.getLimit(), query.getSort(ProjectFileSort.NEW));


        // TODO code for if the user is authorized to get an authorized project
        DataUser user;
        List<DataProject> dataProjects;
        if (authorized) {
            dataProjects = projects.stream().map(DataProject::new).collect(Collectors.toList());
            user = new DataAuthorizedUser(userRecord);
        }
        else {
            dataProjects = projects.stream().map(DataProject::new).collect(Collectors.toList());
            user = new DataUser(userRecord);
        }

        long projectCount = DATABASE.projectDAO.countAllByUsername(username, authorized);

        return ResponseUtil.successResponse(new DataSiteAuthorProjects(user, dataProjects, GamesAPI.GAME_SORTS, projectCount));
    }
}
