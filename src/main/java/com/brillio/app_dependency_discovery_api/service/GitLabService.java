package com.brillio.app_dependency_discovery_api.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.brillio.app_dependency_discovery_api.utilities.GitLabGroupRequest;

import reactor.core.publisher.Mono;

@Service
public class GitLabService {

//    private final WebClient webClient;
//    
//    @Value("${gitLab.token}")
//    private String gitLabToken;
//    private static final Logger logger = LoggerFactory.getLogger(GitLabService.class);
//    private static final String GITLAB_API_URL = "https://gitlab.com/api/v4/groups";
//
//    public GitLabService(WebClient.Builder webClientBuilder) {
//        this.webClient = webClientBuilder.baseUrl(GITLAB_API_URL).build();
//    }
//
//    public Mono<Map<String, Object>> getGroupProjects(String groupId) {
//        return webClient.get()
//                .uri("/{groupId}/projects", groupId)
//                .header("PRIVATE-TOKEN", gitLabToken)
//                .retrieve()
//                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
//                .map(projects -> {
//                    int totalRepositories = projects.size();
//
//                    // Fetch repository name and URL pairs
//                    List<Map<String, String>> repositoryDetails = projects.stream()
//                            .map(project -> {
//                                String name = (String) project.get("name");
//                                String repoUrl = (String) project.get("http_url_to_repo");
//                                // Return a map with both the name and the URL
//                                return Map.of("name", name, "url", repoUrl);
//                            })
//                            .collect(Collectors.toList());
//
//                    // Print the repo total and repository details
//                    System.out.println("Total repositories: " + totalRepositories);
//                    System.out.println("Repository details: " + repositoryDetails);
//
//                    // Return the result as a Map
//                    return Map.of(
//                            "total_repositories", totalRepositories,
//                            "repository_details", repositoryDetails
//                    );
//                });
//    }
//    public String createGitLabGroup(String name, String path, String description, String visibility) {
//        if (path == null || path.isEmpty()) {
//            throw new IllegalArgumentException("Path is required and cannot be empty.");
//        }
//
//        // Debugging: log input values
//        System.out.println("Creating GitLab group with name: " + name + ", path: " + path + ", description: " + description + ", visibility: " + visibility);
//        logger.debug("Creating GitLab group with name: {}, path: {}, description: {}, visibility: {}", name, path, description, visibility);
//
//        try {
//            // Ensure you are hitting the correct URL
//            String response = this.webClient.post()
//                .uri("/groups")
//                .header("PRIVATE-TOKEN", gitLabToken)
//                .bodyValue(new GitLabGroupRequest(name, path, description, visibility))
//                .retrieve()
//                .bodyToMono(String.class)
//                .block();
//
//            // Debugging: log response from GitLab API
//            System.out.println("GitLab group creation response: " + response);
//            logger.debug("GitLab group creation response: {}", response);
//
//            return response;
//
//        } catch (WebClientResponseException e) {
//            // Log the detailed error response
//            System.err.println("Error while creating GitLab group: Status: " + e.getStatusCode());
//            System.err.println("Response body: " + e.getResponseBodyAsString());
//            logger.error("Error while creating GitLab group: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
//
//            // Additional log for better context
//            if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
//                logger.error("403 Forbidden: You may not have sufficient permissions to create the group. Ensure your token has 'api' scope and check if you have sufficient permissions in the target namespace.");
//            }
//
//            return "Error while creating GitLab group: " + e.getResponseBodyAsString();
//        } catch (Exception e) {
//            // General error handling
//            System.err.println("Unexpected error while creating GitLab group: " + e.getMessage());
//            logger.error("Unexpected error while creating GitLab group: {}", e.getMessage(), e);
//            return "Unexpected error while creating GitLab group: " + e.getMessage();
//        }
//    }
//

}
