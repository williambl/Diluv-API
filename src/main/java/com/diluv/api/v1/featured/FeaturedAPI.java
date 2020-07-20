package com.diluv.api.v1.featured;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.diluv.confluencia.database.record.FeaturedGamesEntity;

import com.diluv.confluencia.database.record.FeaturedProjectsEntity;

import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.annotations.cache.Cache;

import com.diluv.api.data.DataBaseProject;
import com.diluv.api.data.DataFeatured;
import com.diluv.api.data.DataGame;
import com.diluv.api.data.DataTag;
import com.diluv.api.utils.response.ResponseUtil;
import com.diluv.confluencia.database.record.GamesEntity;
import com.diluv.confluencia.database.record.ProjectsEntity;
import com.diluv.confluencia.database.record.TagsEntity;

import static com.diluv.api.Main.DATABASE;

@GZIP
@Path("/featured")
@Produces(MediaType.APPLICATION_JSON)
public class FeaturedAPI {

    @Cache(maxAge = 300, mustRevalidate = true)
    @GET
    @Path("/")
    public Response getFeatured () {

        final List<FeaturedGamesEntity> gameRecords = DATABASE.gameDAO.findFeaturedGames();
        final List<DataGame> games = gameRecords.stream().map(DataGame::new).collect(Collectors.toList());

        final List<FeaturedProjectsEntity> projectRecords = DATABASE.projectDAO.findFeaturedProjects();

        final List<DataBaseProject> projects = projectRecords.stream().map(DataBaseProject::new).collect(Collectors.toList());

        final long projectCount = DATABASE.gameDAO.countAllProjectsBySlug("");
        final long userCount = DATABASE.userDAO.countAll();

        return ResponseUtil.successResponse(new DataFeatured(games, projects, projectCount, userCount));
    }
}
