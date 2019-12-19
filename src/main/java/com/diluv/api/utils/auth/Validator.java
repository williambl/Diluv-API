package com.diluv.api.utils.auth;

import org.apache.commons.validator.GenericValidator;

public class Validator {

    public static boolean validateUsername (String username) {

        if (GenericValidator.isBlankOrNull(username) || username.length() > 50 || username.length() < 3) {
            return false;
        }
        return GenericValidator.matchRegexp(username, "([A-Za-z0-9-_]+)");
    }

    public static boolean validateProjectName (String projectName) {

        if (GenericValidator.isBlankOrNull(projectName) || projectName.length() > 50 || projectName.length() < 3) {
            return false;
        }
        return GenericValidator.matchRegexp(projectName, "([A-Za-z0-9-_:]+)");
    }

    public static boolean validateProjectSummary (String projectSummary) {

        if (GenericValidator.isBlankOrNull(projectSummary) || projectSummary.length() > 250 || projectSummary.length() < 10) {
            return false;
        }
        return GenericValidator.matchRegexp(projectSummary, "([A-Za-z0-9-_:]+)");
    }

    public static boolean validateProjectDescription (String projectDescription) {

        if (GenericValidator.isBlankOrNull(projectDescription) || projectDescription.length() > 1000 || projectDescription.length() < 50) {
            return false;
        }
        return GenericValidator.matchRegexp(projectDescription, "([A-Za-z0-9-_:]+)");
    }
}
