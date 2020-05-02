package com.diluv.api.endpoints.v1;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.diluv.api.utils.TestUtil;
import com.diluv.api.utils.error.ErrorMessage;

import java.io.File;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.equalTo;

public class GameTest {

    private static final String URL = "/v1/games";

    @BeforeAll
    public static void setup () {

        TestUtil.start();
    }

    @Test
    public void getGames () {

        given().with().get(URL).then().assertThat().statusCode(200).body(matchesJsonSchemaInClasspath("schema/game-list-schema.json"));
    }

    @Test
    public void getSort () {

        given().with().get(URL + "/sort").then().assertThat().statusCode(200).body(matchesJsonSchemaInClasspath("schema/sort-schema.json"));
    }

    @Test
    public void getGameBySlug () {

        given().with().get(URL + "/invalid").then().assertThat().statusCode(400).body(matchesJsonSchemaInClasspath("schema/error-schema.json")).body("message", equalTo(ErrorMessage.NOT_FOUND_GAME.getMessage()));

        given().with().get(URL + "/minecraft").then().assertThat().statusCode(200).body(matchesJsonSchemaInClasspath("schema/game-schema.json"));
    }

    @Test
    public void getProjectTypesByGameSlug () {

        given().with().get(URL + "/invalid/types").then().assertThat().statusCode(400).body(matchesJsonSchemaInClasspath("schema/error-schema.json")).body("message", equalTo(ErrorMessage.NOT_FOUND_GAME.getMessage()));

        given().with().get(URL + "/minecraft/types").then().assertThat().statusCode(200).body(matchesJsonSchemaInClasspath("schema/project-types-list-schema.json"));
    }

    @Test
    public void getProjectTypesByGameSlugAndProjectType () {

        get(URL + "/invalid/invalid").then().assertThat().statusCode(400).body(matchesJsonSchemaInClasspath("schema/error-schema.json")).body("message", equalTo(ErrorMessage.NOT_FOUND_GAME.getMessage()));

        get(URL + "/minecraft/invalid").then().assertThat().statusCode(400).body(matchesJsonSchemaInClasspath("schema/error-schema.json")).body("message", equalTo(ErrorMessage.NOT_FOUND_PROJECT_TYPE.getMessage()));

        get(URL + "/minecraft/mods").then().assertThat().statusCode(200).body(matchesJsonSchemaInClasspath("schema/project-types-schema.json"));
    }

    @Test
    public void getProjectsByGameSlugAndProjectType () {

        get(URL + "/invalid/invalid/projects").then().assertThat().statusCode(400).body(matchesJsonSchemaInClasspath("schema/error-schema.json")).body("message", equalTo(ErrorMessage.NOT_FOUND_GAME.getMessage()));

        get(URL + "/minecraft/invalid/projects").then().assertThat().statusCode(400).body(matchesJsonSchemaInClasspath("schema/error-schema.json")).body("message", equalTo(ErrorMessage.NOT_FOUND_PROJECT_TYPE.getMessage()));

        get(URL + "/minecraft/mods/projects").then().assertThat().statusCode(200).body(matchesJsonSchemaInClasspath("schema/project-list-schema.json"));
    }

    @Test
    public void getProjectByGameSlugAndProjectTypeAndProjectSlug () {

        get(URL + "/invalid/invalid/test").then().assertThat().statusCode(400).body(matchesJsonSchemaInClasspath("schema/error-schema.json")).body("message", equalTo(ErrorMessage.NOT_FOUND_GAME.getMessage()));

        get(URL + "/minecraft/invalid/test").then().assertThat().statusCode(400).body(matchesJsonSchemaInClasspath("schema/error-schema.json")).body("message", equalTo(ErrorMessage.NOT_FOUND_PROJECT_TYPE.getMessage()));

        get(URL + "/minecraft/mods/test").then().assertThat().statusCode(400).body(matchesJsonSchemaInClasspath("schema/error-schema.json")).body("message", equalTo(ErrorMessage.NOT_FOUND_PROJECT.getMessage()));

        get(URL + "/minecraft/mods/bookshelf").then().assertThat().statusCode(200).body(matchesJsonSchemaInClasspath("schema/project-schema.json"));
    }

    @Test
    public void getProjectFilesByGameSlugAndProjectTypeAndProjectSlug () {

        get(URL + "/invalid/invalid/test/files").then().assertThat().statusCode(400).body(matchesJsonSchemaInClasspath("schema/error-schema.json")).body("message", equalTo(ErrorMessage.NOT_FOUND_GAME.getMessage()));

        get(URL + "/minecraft/invalid/invalidproject/files").then().assertThat().statusCode(400).body(matchesJsonSchemaInClasspath("schema/error-schema.json")).body("message", equalTo(ErrorMessage.NOT_FOUND_PROJECT_TYPE.getMessage()));

        get(URL + "/minecraft/mods/invalidproject/files").then().assertThat().statusCode(400).body(matchesJsonSchemaInClasspath("schema/error-schema.json")).body("message", equalTo(ErrorMessage.NOT_FOUND_PROJECT.getMessage()));

        get(URL + "/minecraft/mods/crafttweaker/files").then().assertThat().statusCode(200).body(matchesJsonSchemaInClasspath("schema/project-files-list-schema.json"));

        get(URL + "/minecraft/mods/bookshelf/files").then().assertThat().statusCode(200).body(matchesJsonSchemaInClasspath("schema/project-files-list-schema.json"));
    }

    @Test
    public void postProjectByGameSlugAndProjectType () {

        final ClassLoader classLoader = this.getClass().getClassLoader();
        final File logo = new File(classLoader.getResource("logo.png").getFile());

        given().header("Authorization", "Bearer " + TestUtil.VALID_TOKEN).multiPart("name", "Bookshelf").multiPart("summary", "Bookshelf summary aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa").multiPart("description", "Bookshelf descriptionaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa").multiPart("logo", logo).with().post(URL + "/invalid/invalid").then().assertThat().statusCode(400).body(matchesJsonSchemaInClasspath("schema/error-schema.json")).body("message", equalTo(ErrorMessage.NOT_FOUND_GAME.getMessage()));
        given().header("Authorization", "Bearer " + TestUtil.VALID_TOKEN).multiPart("name", "Bookshelf").multiPart("summary", "Bookshelf summary aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa").multiPart("description", "Bookshelf descriptionaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa").multiPart("logo", logo).with().post(URL + "/minecraft/invalid").then().assertThat().statusCode(400).body(matchesJsonSchemaInClasspath("schema/error-schema.json")).body("message", equalTo(ErrorMessage.NOT_FOUND_PROJECT_TYPE.getMessage()));
        given().header("Authorization", "Bearer " + TestUtil.VALID_TOKEN).multiPart("name", "Bookshelf").multiPart("summary", "Bookshelf summary aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa").multiPart("description", "Bookshelf descriptionaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa").multiPart("logo", logo).with().post(URL + "/minecraft/mods").then().assertThat().statusCode(400).body(matchesJsonSchemaInClasspath("schema/error-schema.json")).body("message", equalTo(ErrorMessage.PROJECT_TAKEN_SLUG.getMessage()));
        given().header("Authorization", "Bearer " + TestUtil.VALID_TOKEN).multiPart("name", "Bookshelf2").multiPart("summary", "Bookshelf summary aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa").multiPart("description", "Bookshelf descriptionaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa").multiPart("logo", logo).with().post(URL + "/minecraft/mods").then().assertThat().statusCode(200).body(matchesJsonSchemaInClasspath("schema/project-schema.json"));
    }
}