package com.diluv.api.data.site;

import java.util.List;

import com.diluv.api.data.DataBaseProject;
import com.google.gson.annotations.Expose;

public class DataSiteProjectFilesPage {

    @Expose
    private final DataBaseProject project;

    @Expose
    private final List<DataSiteProjectFileDisplay> files;

    public DataSiteProjectFilesPage (DataBaseProject project, List<DataSiteProjectFileDisplay> files) {

        this.project = project;

        this.files = files;
    }
}
