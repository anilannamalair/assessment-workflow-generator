package com.brillio.app_dependency_discovery_api.utilities;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

public class GitHubRepositoryHandler {

	private static final String GITHUB_API_URL = "https://api.github.com/repos/";

	public static String createNewRepository(String repoName, String token) throws IOException {
	    // Authenticate with GitHub using the token
	    GitHub github = new GitHubBuilder().withOAuthToken(token).build();
	    
	    try {
	        // Check if the repository already exists
	        GHRepository existingRepo = github.getRepository("anilannamalair/" + repoName);
	        return existingRepo.getHtmlUrl().toString(); // Return the existing repository's URL
	    } catch (GHFileNotFoundException e) {
	        // Repository doesn't exist, so create a new one
	        GHRepository newRepo = github.createRepository(repoName)
	                                    .description("Automatically created repository for workflow.")
	                                    .private_(false)
	                                    .create();
	        return newRepo.getHtmlUrl().toString(); // Return the URL of the newly created repository
	    }
	}


    public static void createAndPushToNewRepo(String destinationRepoUrl, File clonedRepoDir, String token) throws Exception {
        // Initialize the git repository from the cloned repo directory
        Git git = Git.open(clonedRepoDir);
        
        // Add all changes to git
        git.add().addFilepattern(".").call();
        
        // Commit changes
        git.commit().setMessage("Add GitHub Action workflow").call();
        
        // Push the changes to the new GitHub repository
        git.push()
           .setRemote(destinationRepoUrl)
           .setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, ""))
           .call();
        
        System.out.println("Changes have been pushed to: " + destinationRepoUrl);
    }
    public static boolean doesRepositoryExist(String repoName, String token) {
        try {
            // Construct the URL to check the repository's existence
            String urlStr = GITHUB_API_URL + "anilannamalair/" + repoName;
            URL url = new URL(urlStr);
            
            // Open a connection to the GitHub API
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Set the request method to GET
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "token " + token);
            
            // Get the response code
            int responseCode = connection.getResponseCode();
            
            // If the response code is 200, the repository exists
            return responseCode == HttpURLConnection.HTTP_OK;
        } catch (Exception e) {
            // Log the error if the request fails
            e.printStackTrace();
            return false; // Assume repository does not exist if there's an error
        }
    }
    
    // Method to delete a GitHub repository
    public static void deleteRepository(String repoName, String token) throws Exception {
        String urlStr = GITHUB_API_URL + "anilannamalair/" + repoName;
        URL url = new URL(urlStr);

        // Open a connection to the GitHub API
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        // Set the request method to DELETE
        connection.setRequestMethod("DELETE");
        connection.setRequestProperty("Authorization", "token " + token);
        
        // Get the response code to verify the request was successful
        int responseCode = connection.getResponseCode();
        
        if (responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
            System.out.println("Repository " + repoName + " deleted successfully.");
        } else {
            throw new Exception("Failed to delete repository. Response Code: " + responseCode);
        }
    }
}
