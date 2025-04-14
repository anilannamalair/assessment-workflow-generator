package com.brillio.app_dependency_discovery_api.utilities;


public class GitLabGroupRequest {

    private String name;
    private String path;
    private String description;
    private String visibility;

    public GitLabGroupRequest(String name, String path, String description, String visibility) {
        this.name = name;
        this.path = path;
        this.description = description;
        this.visibility = visibility;
    }

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }
}
