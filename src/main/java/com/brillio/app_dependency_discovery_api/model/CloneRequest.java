package com.brillio.app_dependency_discovery_api.model;


public class CloneRequest {

    private String sourceRepoUrl;
    private String serviceName;
    private String destinationRepoName;

    // Getters and setters

    public String getSourceRepoUrl() {
        return sourceRepoUrl;
    }

    public void setSourceRepoUrl(String sourceRepoUrl) {
        this.sourceRepoUrl = sourceRepoUrl;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getDestinationRepoName() {
        return destinationRepoName;
    }

    public void setDestinationRepoName(String destinationRepoName) {
        this.destinationRepoName = destinationRepoName;
    }
}
