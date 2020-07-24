package com.diluv.api.v1.users;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.annotations.Query;
import org.jboss.resteasy.annotations.cache.Cache;

import com.diluv.api.data.DataAuthorizedUser;
import com.diluv.api.data.DataProject;
import com.diluv.api.data.DataUser;
import com.diluv.api.utils.auth.tokens.Token;
import com.diluv.api.utils.error.ErrorMessage;
import com.diluv.api.utils.query.ProjectQuery;
import com.diluv.api.utils.response.ResponseUtil;
import com.diluv.confluencia.database.record.ProjectsEntity;
import com.diluv.confluencia.database.record.UsersEntity;
import com.diluv.confluencia.database.sort.ProjectSort;
import com.diluv.confluencia.database.sort.Sort;

import static com.diluv.api.Main.DATABASE;

@GZIP
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
public class UsersAPI {

    @GET
    @Path("/self")
    public Response getSelf (@HeaderParam("Authorization") Token token) {

        if (token == null) {

            return ErrorMessage.USER_INVALID_TOKEN.respond();
        }

        final UsersEntity userRecord = DATABASE.userDAO.findOneByUserId(token.getUserId());

        if (userRecord == null) {

            return ErrorMessage.NOT_FOUND_USER.respond();
        }

        return ResponseUtil.successResponse(new DataAuthorizedUser(userRecord));
    }

    @Cache(maxAge = 300, mustRevalidate = true)
    @GET
    @Path("/{username}")
    public Response getUser (@HeaderParam("Authorization") Token token, @PathParam("username") String username) {

        final UsersEntity userRecord = DATABASE.userDAO.findOneByUsername(username);

        if (userRecord == null) {

            return ErrorMessage.NOT_FOUND_USER.respond();
        }

        if (token != null && token.getUserId() == userRecord.getId()) {
            return ResponseUtil.successResponse(new DataAuthorizedUser(userRecord));
        }

        return ResponseUtil.successResponse(new DataUser(userRecord));
    }

    @GET
    @Path("/{username}/projects")
    public Response getProjectsByUsername (@HeaderParam("Authorization") Token token, @PathParam("username") String username, @Query ProjectQuery query) {

        long page = query.getPage();
        int limit = query.getLimit();
        Sort sort = query.getSort(ProjectSort.POPULAR);

        UsersEntity userRecord = DATABASE.userDAO.findOneByUsername(username);
        if (userRecord == null) {
            return ErrorMessage.NOT_FOUND_USER.respond();
        }

        boolean authorized = false;

        if (token != null) {
            authorized = userRecord.getUsername().equalsIgnoreCase(username);
        }

        List<ProjectsEntity> projects = DATABASE.projectDAO.findAllByUsername(username, authorized, page, limit, sort);
        return ResponseUtil.successResponse(projects.stream().map(DataProject::new).collect(Collectors.toList()));
    }
}