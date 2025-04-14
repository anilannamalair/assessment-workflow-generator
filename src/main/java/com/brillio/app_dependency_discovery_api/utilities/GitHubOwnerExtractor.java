package com.brillio.app_dependency_discovery_api.utilities;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitHubOwnerExtractor {

    // Function to validate if the URL is a valid GitHub repository URL
    public static boolean isValidGitHubURL(String url) {
        String regex = "https://github.com/([a-zA-Z0-9_-]+)/([a-zA-Z0-9_-]+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(url);
        return matcher.matches();
    }

    // Function to extract owner name from GitHub repository URL
    public static String extractOwnerName(String url) {
        try {
            if (isValidGitHubURL(url)) {
                URL gitHubUrl = new URL(url);
                String[] parts = gitHubUrl.getPath().split("/");

                // Owner is the first part after "/"
                if (parts.length > 1) {
                    return parts[1]; // Owner name is always at index 1
                }
            }
            return "Invalid GitHub URL or unable to extract owner name.";
        } catch (Exception e) {
            return "Error extracting owner: " + e.getMessage();
        }
    }

    public static void main(String[] args) {
        // Test the function with a sample GitHub URL
        String url = "https://github.com/jvalentino/example-java-gradle-jenkins-scripted";
        String ownerName = extractOwnerName(url);
        System.out.println("Owner: " + ownerName);

        // Testing with an invalid URL
        String invalidUrl = "https://notgithub.com/invalid-url";
        String invalidOwner = extractOwnerName(invalidUrl);
        System.out.println("Owner from invalid URL: " + invalidOwner);
    }
}
