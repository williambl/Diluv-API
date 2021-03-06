package com.diluv.api.v1.games;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.validator.GenericValidator;
import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.annotations.Query;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jboss.resteasy.plugins.providers.atom.Content;
import org.jboss.resteasy.plugins.providers.atom.Entry;
import org.jboss.resteasy.plugins.providers.atom.Feed;
import org.jboss.resteasy.plugins.providers.atom.Link;
import org.jboss.resteasy.plugins.providers.atom.Person;

import com.diluv.api.data.*;
import com.diluv.api.provider.ResponseException;
import com.diluv.api.utils.Constants;
import com.diluv.api.utils.ImageUtil;
import com.diluv.api.utils.auth.Validator;
import com.diluv.api.utils.auth.tokens.ErrorToken;
import com.diluv.api.utils.auth.tokens.Token;
import com.diluv.api.utils.error.ErrorMessage;
import com.diluv.api.utils.error.ErrorType;
import com.diluv.api.utils.permissions.ProjectPermissions;
import com.diluv.api.utils.query.GameQuery;
import com.diluv.api.utils.query.ProjectFileQuery;
import com.diluv.api.utils.query.ProjectQuery;
import com.diluv.api.utils.response.ResponseUtil;
import com.diluv.api.v1.utilities.ProjectService;
import com.diluv.confluencia.Confluencia;
import com.diluv.confluencia.database.record.GamesEntity;
import com.diluv.confluencia.database.record.ProjectReviewEntity;
import com.diluv.confluencia.database.record.ProjectFilesEntity;
import com.diluv.confluencia.database.record.ProjectTagsEntity;
import com.diluv.confluencia.database.record.ProjectTypesEntity;
import com.diluv.confluencia.database.record.ProjectsEntity;
import com.diluv.confluencia.database.record.TagsEntity;
import com.diluv.confluencia.database.record.UsersEntity;
import com.diluv.confluencia.database.sort.GameSort;
import com.diluv.confluencia.database.sort.ProjectFileSort;
import com.diluv.confluencia.database.sort.ProjectSort;
import com.diluv.confluencia.database.sort.Sort;
import com.github.slugify.Slugify;

@GZIP
@Path("/games")
@Produces(MediaType.APPLICATION_JSON)
public class GamesAPI {

    public static final List<DataSort> GAME_SORTS = GameSort.LIST.stream().map(DataSort::new).collect(Collectors.toList());
    public static final List<DataSort> PROJECT_SORTS = ProjectSort.LIST.stream().map(DataSort::new).collect(Collectors.toList());

    private final Slugify slugify = new Slugify();

    @GET
    @Path("/")
    public Response getGames (@Query GameQuery query) {

        final long page = query.getPage();
        final int limit = query.getLimit();
        final Sort sort = query.getSort(GameSort.NAME);
        final String search = query.getSearch();

        return Confluencia.getTransaction(session -> {
            final List<DataBaseGame> games = Confluencia.GAME.findAll(session, page, limit, sort, search)
                .stream()
                .map(DataGame::new)
                .collect(Collectors.toList());

            final long gameCount = Confluencia.GAME.countAllBySearch(session, search);

            return ResponseUtil.successResponse(new DataGameList(games, GAME_SORTS, gameCount));
        });
    }

    @GET
    @Path("/{gameSlug}")
    public Response getGame (@PathParam("gameSlug") String gameSlug) {

        return Confluencia.getTransaction(session -> {
            final GamesEntity gameRecord = Confluencia.GAME.findOneBySlug(session, gameSlug);

            if (gameRecord == null) {

                return ErrorMessage.NOT_FOUND_GAME.respond();
            }

            final long projectCount = Confluencia.PROJECT.countAllByGameSlug(session, gameSlug);

            return ResponseUtil.successResponse(new DataGame(gameRecord, PROJECT_SORTS, projectCount));
        });
    }

    @GET
    @Path("/{gameSlug}/{projectTypeSlug}")
    public Response getProjectType (@PathParam("gameSlug") String gameSlug, @PathParam("projectTypeSlug") String projectTypeSlug) {

        return Confluencia.getTransaction(session -> {
            final ProjectTypesEntity projectTypesRecords = Confluencia.PROJECT.findOneProjectTypeByGameSlugAndProjectTypeSlug(session, gameSlug, projectTypeSlug);

            if (projectTypesRecords == null) {

                if (Confluencia.GAME.findOneBySlug(session, gameSlug) == null) {

                    return ErrorMessage.NOT_FOUND_GAME.respond();
                }

                return ErrorMessage.NOT_FOUND_PROJECT_TYPE.respond();
            }

            final long projectCount = Confluencia.PROJECT.countAllByGameSlugAndProjectTypeSlug(session, gameSlug, projectTypeSlug);
            return ResponseUtil.successResponse(new DataProjectType(projectTypesRecords, projectCount));
        });
    }

    @GET
    @Path("/{gameSlug}/{projectTypeSlug}/projects")
    public Response getProjects (@PathParam("gameSlug") String gameSlug, @PathParam("projectTypeSlug") String projectTypeSlug, @Query ProjectQuery query) {

        final long page = query.getPage();
        final int limit = query.getLimit();
        final Sort sort = query.getSort(ProjectSort.POPULAR);
        final String search = query.getSearch();
        final String versions = query.getVersions();
        final String[] tags = query.getTags();
        final String[] loaders = query.getLoaders();

        return Confluencia.getTransaction(session -> {

            final List<ProjectsEntity> projects = Confluencia.PROJECT.findAllByGameAndProjectType(session, gameSlug, projectTypeSlug, search, page, limit, sort, versions, tags, loaders);

            if (projects.isEmpty()) {

                if (Confluencia.GAME.findOneBySlug(session, gameSlug) == null) {

                    return ErrorMessage.NOT_FOUND_GAME.respond();
                }

                if (Confluencia.PROJECT.findOneProjectTypeByGameSlugAndProjectTypeSlug(session, gameSlug, projectTypeSlug) == null) {

                    return ErrorMessage.NOT_FOUND_PROJECT_TYPE.respond();
                }
            }

            final List<DataBaseProject> dataProjects = projects.stream().map(DataBaseProject::new).collect(Collectors.toList());

            return ResponseUtil.successResponse(dataProjects);
        });
    }

    @GET
    @Path("/{gameSlug}/{projectTypeSlug}/feed.atom")
    @Produces(MediaType.APPLICATION_ATOM_XML)
    public Response getProjectFeed (@PathParam("gameSlug") String gameSlug, @PathParam("projectTypeSlug") String projectTypeSlug) {

        final List<ProjectsEntity> projects = Confluencia.getTransaction(session -> {
            return Confluencia.PROJECT.findAllByGameAndProjectType(session, gameSlug, projectTypeSlug, "", 1, 25, ProjectSort.NEW);
        });

        if (projects.isEmpty()) {
            return Response.status(ErrorType.BAD_REQUEST.getCode()).build();
        }

        final String baseUrl = String.format("%s/games/%s/%s", Constants.WEBSITE_URL, gameSlug, projectTypeSlug);
        Feed feed = new Feed();
        feed.setId(URI.create(baseUrl + "/feed.atom"));
        feed.getLinks().add(new Link("self", baseUrl));
        feed.setUpdated(new Date());
        feed.setTitle("Project Feed");
        for (ProjectsEntity project : projects) {
            Content content = new Content();
            content.setText(project.getSummary());
            Entry entry = new Entry();
            entry.setId(URI.create(baseUrl + "/" + project.getSlug()));
            entry.setTitle(project.getName());
            entry.setUpdated(project.getUpdatedAt());
            entry.setContent(content);

            //TODO Maybe show all authors?
            entry.getAuthors().add(new Person(project.getOwner().getDisplayName()));
            feed.getEntries().add(entry);
        }

        return Response.ok(feed).build();
    }

    @GET
    @Path("/{gameSlug}/{projectTypeSlug}/{projectSlug}")
    public Response getProject (@HeaderParam("Authorization") Token token, @PathParam("gameSlug") String gameSlug, @PathParam("projectTypeSlug") String projectTypeSlug, @PathParam("projectSlug") String projectSlug) {

        return Confluencia.getTransaction(session -> {
            try {
                final DataProject project = ProjectService.getDataProject(session, gameSlug, projectTypeSlug, projectSlug, token);
                return ResponseUtil.successResponse(project);
            }
            catch (ResponseException e) {
                e.printStackTrace();
                return e.getResponse();
            }
        });
    }

    @PATCH
    @Path("/{gameSlug}/{projectTypeSlug}/{projectSlug}")
    public Response patchProject (@HeaderParam("Authorization") Token token, @PathParam("gameSlug") String gameSlug, @PathParam("projectTypeSlug") String projectTypeSlug, @PathParam("projectSlug") String projectSlug, @MultipartForm ProjectForm form) {

        if (token == null) {
            return ErrorMessage.USER_REQUIRED_TOKEN.respond();
        }

        if (token instanceof ErrorToken) {
            return ((ErrorToken) token).getResponse();
        }

        return Confluencia.getTransaction(session -> {

            if (Confluencia.GAME.findOneBySlug(session, gameSlug) == null) {
                return ErrorMessage.NOT_FOUND_GAME.respond();
            }

            if (Confluencia.PROJECT.findOneProjectTypeByGameSlugAndProjectTypeSlug(session, gameSlug, projectTypeSlug) == null) {
                return ErrorMessage.NOT_FOUND_PROJECT_TYPE.respond();
            }

            ProjectsEntity project = Confluencia.PROJECT.findOneProject(session, gameSlug, projectTypeSlug, projectSlug);
            if (project == null) {
                return ErrorMessage.NOT_FOUND_PROJECT.respond();
            }

            if (!ProjectPermissions.hasPermission(project, token, ProjectPermissions.PROJECT_EDIT)) {

                return ErrorMessage.USER_NOT_AUTHORIZED.respond();
            }

            if (form.data == null) {
                return ErrorMessage.INVALID_DATA.respond();
            }

            if (!GenericValidator.isBlankOrNull(form.data.name)) {
                String name = form.data.name.trim();
                if (!name.equals(project.getName())) {
                    if (!Validator.validateProjectName(name)) {
                        return ErrorMessage.PROJECT_INVALID_NAME.respond();
                    }

                    project.setName(name);
                }
            }

            if (!GenericValidator.isBlankOrNull(form.data.summary)) {
                String summary = form.data.summary.trim();
                if (!summary.equals(project.getSummary())) {
                    if (!Validator.validateProjectSummary(summary)) {
                        return ErrorMessage.PROJECT_INVALID_SUMMARY.respond();
                    }

                    project.setSummary(summary);
                }
            }

            if (!GenericValidator.isBlankOrNull(form.data.description)) {
                String description = form.data.description.trim();
                if (!description.equals(project.getDescription())) {
                    if (!Validator.validateProjectDescription(description)) {
                        return ErrorMessage.PROJECT_INVALID_DESCRIPTION.respond();
                    }

                    project.setDescription(description);
                }
            }

            List<String> tags = form.data.tags;
            if (!tags.isEmpty()) {
                List<TagsEntity> tagRecords = Validator.validateTags(project.getProjectType(), tags);
                if (tagRecords.size() != tags.size()) {
                    return ErrorMessage.PROJECT_INVALID_TAGS.respond();
                }

                project.getTags().clear();

                for (TagsEntity tag : tagRecords) {
                    ProjectTagsEntity tagsEntity = new ProjectTagsEntity();
                    tagsEntity.setProject(project);
                    tagsEntity.setTag(tag);
                    project.getTags().add(tagsEntity);
                }

            }

            for (ProjectReviewEntity review : project.getReviews()) {
                if (!review.isCompleted()) {
                    project.setReview(true);
                    review.setCompleted(true);
                }
            }

            session.update(project);

            if (form.logo != null) {
                final BufferedImage image = ImageUtil.isValidImage(form.logo, 1000000L);

                if (image == null) {
                    return ErrorMessage.INVALID_IMAGE.respond();
                }

                final File file = new File(Constants.CDN_FOLDER, String.format("games/%s/%s/%d/logo.png", gameSlug, projectTypeSlug, project.getId()));
                if (!ImageUtil.savePNG(image, file)) {
                    // return ErrorMessage.ERROR_SAVING_IMAGE.respond();
                    return ErrorMessage.THROWABLE.respond();
                }
            }

            return Response.status(Response.Status.NO_CONTENT).build();
        });
    }

    @GET
    @Path("/{gameSlug}/{projectTypeSlug}/{projectSlug}/feed.atom")
    @Produces(MediaType.APPLICATION_ATOM_XML)
    public Response getProjectFileFeed (@PathParam("gameSlug") String gameSlug, @PathParam("projectTypeSlug") String projectTypeSlug, @PathParam("projectSlug") String projectSlug) {

        return Confluencia.getTransaction(session -> {
            final ProjectsEntity project = Confluencia.PROJECT.findOneProject(session, gameSlug, projectTypeSlug, projectSlug);
            if (project == null) {
                return Response.status(ErrorType.BAD_REQUEST.getCode()).build();
            }

            final List<ProjectFilesEntity> projectFiles = Confluencia.FILE.findAllByProject(session, project, false, 1, 25, ProjectFileSort.NEW, null);

            final String baseUrl = String.format("%s/games/%s/%s/%s", Constants.WEBSITE_URL, gameSlug, projectTypeSlug, projectSlug);
            Feed feed = new Feed();
            feed.setId(URI.create(baseUrl + "/feed.atom"));
            feed.getLinks().add(new Link("self", baseUrl + "/"));
            feed.setUpdated(new Date());
            feed.setTitle(project.getName() + " File Feed");
            for (ProjectFilesEntity file : projectFiles) {
                Content content = new Content();
                content.setText(file.getChangelog());
                Entry entry = new Entry();
                entry.setId(URI.create(baseUrl + "/files/" + file.getId()));
                entry.setTitle(file.getName());
                entry.setUpdated(file.getUpdatedAt());
                entry.setContent(content);
                entry.getAuthors().add(new Person(file.getUser().getDisplayName()));
                feed.getEntries().add(entry);
            }

            return Response.ok(feed).build();
        });
    }

    @GET
    @Path("/{gameSlug}/{projectTypeSlug}/{projectSlug}/files")
    public Response getProjectFiles (@HeaderParam("Authorization") Token token, @PathParam("gameSlug") String gameSlug, @PathParam("projectTypeSlug") String projectTypeSlug, @PathParam("projectSlug") String projectSlug, @Query ProjectFileQuery query) {

        long page = query.getPage();
        int limit = query.getLimit();
        Sort sort = query.getSort(ProjectFileSort.NEW);
        String gameVersion = query.getGameVersion();

        return Confluencia.getTransaction(session -> {
            final ProjectsEntity project = Confluencia.PROJECT.findOneProject(session, gameSlug, projectTypeSlug, projectSlug);
            if (project == null) {

                if (Confluencia.GAME.findOneBySlug(session, gameSlug) == null) {
                    return ErrorMessage.NOT_FOUND_GAME.respond();
                }

                if (Confluencia.PROJECT.findOneProjectTypeByGameSlugAndProjectTypeSlug(session, gameSlug, projectTypeSlug) == null) {
                    return ErrorMessage.NOT_FOUND_PROJECT_TYPE.respond();
                }

                return ErrorMessage.NOT_FOUND_PROJECT.respond();
            }

            boolean authorized = token != null && ProjectPermissions.hasPermission(project, token, ProjectPermissions.FILE_UPLOAD);
            final List<ProjectFilesEntity> projectFileRecords = Confluencia.FILE.findAllByProject(session, project, authorized, page, limit, sort, gameVersion);

            final List<DataProjectFile> projectFiles = projectFileRecords.stream().map(record -> record.isReleased() ?
                new DataProjectFileAvailable(record, gameSlug, projectTypeSlug, projectSlug) :
                new DataProjectFileInQueue(record, gameSlug, projectTypeSlug, projectSlug)).collect(Collectors.toList());
            return ResponseUtil.successResponse(projectFiles);
        });
    }

    @POST
    @Path("/{gameSlug}/{projectTypeSlug}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response postProject (@HeaderParam("Authorization") Token token, @PathParam("gameSlug") String gameSlug, @PathParam("projectTypeSlug") String projectTypeSlug, @MultipartForm ProjectForm form) {

        if (token == null) {
            return ErrorMessage.USER_REQUIRED_TOKEN.respond();
        }

        if (token instanceof ErrorToken) {
            return ((ErrorToken) token).getResponse();
        }

        return Confluencia.getTransaction(session -> {

            ProjectTypesEntity projectType = Confluencia.PROJECT.findOneProjectTypeByGameSlugAndProjectTypeSlug(session, gameSlug, projectTypeSlug);

            if (projectType == null) {
                if (Confluencia.GAME.findOneBySlug(session, gameSlug) == null) {
                    return ErrorMessage.NOT_FOUND_GAME.respond();
                }
                return ErrorMessage.NOT_FOUND_PROJECT_TYPE.respond();
            }

            if (!Validator.validateProjectName(form.data.name)) {
                return ErrorMessage.PROJECT_INVALID_NAME.respond();
            }

            if (!Validator.validateProjectSummary(form.data.summary)) {
                return ErrorMessage.PROJECT_INVALID_SUMMARY.respond();
            }

            if (!Validator.validateProjectDescription(form.data.description)) {
                return ErrorMessage.PROJECT_INVALID_DESCRIPTION.respond();
            }

            List<String> tags = form.data.tags;
            List<TagsEntity> tagRecords = Validator.validateTags(projectType, tags);
            if (tagRecords.size() != tags.size()) {
                return ErrorMessage.PROJECT_INVALID_TAGS.respond();
            }

            if (form.logo == null) {
                return ErrorMessage.INVALID_IMAGE.respond();
            }

            final BufferedImage image = ImageUtil.isValidImage(form.logo, 1000000L);

            if (image == null) {
                return ErrorMessage.REQUIRES_IMAGE.respond();
            }

            String name = form.data.name.trim();
            String summary = form.data.summary.trim();
            String description = form.data.description.trim();

            final String projectSlug = this.slugify.slugify(name);

            if (Confluencia.PROJECT.findOneProject(session, gameSlug, projectTypeSlug, projectSlug) != null) {
                return ErrorMessage.PROJECT_TAKEN_SLUG.respond();

            }

            ProjectsEntity project = new ProjectsEntity();
            project.setSlug(projectSlug);
            project.setName(name);
            project.setSummary(summary);
            project.setDescription(description);
            project.setOwner(new UsersEntity(token.getUserId()));
            project.setProjectType(projectType);
            if (!tagRecords.isEmpty()) {
                for (TagsEntity tag : tagRecords) {
                    ProjectTagsEntity projectTag = new ProjectTagsEntity();
                    projectTag.setTag(tag);
                    project.addTag(projectTag);
                }
            }

            session.save(project);
            session.flush();
            session.refresh(project);

            final File file = new File(Constants.CDN_FOLDER, String.format("games/%s/%s/%d/logo.png", gameSlug, projectTypeSlug, project.getId()));
            if (!ImageUtil.savePNG(image, file)) {
                // return ErrorMessage.ERROR_SAVING_IMAGE.respond();
                return ErrorMessage.THROWABLE.respond();
            }

            return ResponseUtil.successResponse(new DataProject(project));
        });
    }
}
