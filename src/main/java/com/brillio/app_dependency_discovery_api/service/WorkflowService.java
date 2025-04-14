package com.brillio.app_dependency_discovery_api.service;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.UUID;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.brillio.app_dependency_discovery_api.utilities.GitHubRepositoryHandler;
import com.brillio.app_dependency_discovery_api.utilities.JenkinsToYamlConverter;

@Service
public class WorkflowService {
	
	@Value("${github.token}")
	private String gitHubAccessToken;

	public String cloneAndGenerateWorkflow(String serviceName, String sourceRepoUrl) {
        try {
            // 1. Extract the repository name from the source repository URL
            String repoName = extractRepoName(sourceRepoUrl);
            String organization = extractOrganization(sourceRepoUrl);
            // 2. Create the destination repository name by appending "-destination" to the repo name
            String destinationRepoName = repoName + "-destination";  // Appending only -destination

            // 3. Check if the destination repository exists
            String ownerName = GitHubUserFetcher.getAuthenticatedUsername(gitHubAccessToken);
//            System.out.println("Owner/Authenticated Username: " + ownerName);
            String destinationRepoUrl = "https://github.com/"+ ownerName+"/" + destinationRepoName + ".git";
            boolean destinationRepoExists = GitHubRepositoryHandler.doesRepositoryExist(destinationRepoName, gitHubAccessToken);

            // If destination repo doesn't exist, create it and push workflow
            if (!destinationRepoExists) {
                createNewGitHubRepository(destinationRepoName, gitHubAccessToken);
                // Create the new workflow and push it to the new repo
                return createNewWorkflowAndPush(serviceName, sourceRepoUrl, destinationRepoUrl, destinationRepoName);
            } else {
                // If the destination repo exists, clone it, delete it, and update with new workflow
                return updateExistingWorkflowAndPush(serviceName, sourceRepoUrl, destinationRepoUrl, destinationRepoName);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
	
	 public static String extractOrganization(String repoUrl) {
	        try {
	            URL url = new URL(repoUrl);
	            String path = url.getPath();

	            if (path == null || path.isEmpty()) {
	                return null; // Or throw an exception, depending on your error handling
	            }

	            String[] parts = path.split("/");

	            if (parts.length >= 2) {
	                return parts[1]; // The organization is the second part of the path
	            } else {
	                return null; // Or throw an exception, depending on your error handling
	            }

	        } catch (MalformedURLException e) {
	            // Handle the exception, e.g., log an error or return null
	            System.err.println("Invalid URL: " + repoUrl);
	            return null;
	        }
	    }


	public static String extractRepoName(String repoUrl) {
        try {
            URL url = new URL(repoUrl);
            String path = url.getPath();
            String[] parts = path.split("/");
            if (parts.length >= 2) {
                return parts[parts.length - 1];
            } else {
                return null; // Or throw an exception, depending on your error handling policy
            }
        } catch (MalformedURLException e) {
            return null; // Or throw an exception, depending on your error handling policy
        }
    }
    private String extractRepoNameFromUrl(String repoUrl) {
        // Extract just the repo name from the URL (removes the base URL and ".git" suffix)
        String repoName = repoUrl.replaceFirst("https://github.com/anilannamalair/", "")
                                 .replaceAll("\\.git$", "");
        return repoName;
    }

 // Create new workflow and push to a new repository
    private String createNewWorkflowAndPush(String serviceName, String sourceRepoUrl, String destinationRepoUrl, String destinationRepoName) throws Exception {
        // Clone the source repo to a temporary directory
        String uniqueDirName = "cloned_repo_" + UUID.randomUUID().toString();
        File clonedRepoDir = new File(uniqueDirName);
        cloneRepository(sourceRepoUrl, clonedRepoDir, gitHubAccessToken);  // *** Clone the source repo (no change here) ***
        
        // Create a new workflow YAML
        String jenkinsFileContent = new String(Files.readAllBytes(findJenkinsFile(serviceName, clonedRepoDir, isCustomRepo(clonedRepoDir)).toPath()));
        String yamlContent = convertJenkinsToYaml(jenkinsFileContent);
        String workflowFolderPath = getWorkflowFolderPath(clonedRepoDir, serviceName);
        createGitHubActionYaml(workflowFolderPath, yamlContent);

        // *** (Here is the change) ***
        // Push the changes to the new destination repo, including source repo content and workflow
        GitHubRepositoryHandler.createAndPushToNewRepo(destinationRepoUrl, clonedRepoDir, gitHubAccessToken);  // This step already included source repo and workflow
        
        return "Successfully pushed to: " + destinationRepoUrl;
    }

    // Update the existing workflow and push to the destination repository
    private String updateExistingWorkflowAndPush(String serviceName, String sourceRepoUrl, String destinationRepoUrl, String destinationRepoName) throws Exception {
        // Clone the existing destination repository
        String uniqueDirName = "cloned_repo_" + UUID.randomUUID().toString();
        File clonedRepoDir = new File(uniqueDirName);
        cloneRepository(destinationRepoUrl, clonedRepoDir, gitHubAccessToken);

        // Clone the source repo to get the requested workflow
        File sourceRepoDir = new File("source_repo_" + UUID.randomUUID().toString());
        cloneRepository(sourceRepoUrl, sourceRepoDir, gitHubAccessToken);

        // Find the Jenkinsfile for the service requested
        String jenkinsFileContent = new String(Files.readAllBytes(findJenkinsFile(serviceName, sourceRepoDir, isCustomRepo(sourceRepoDir)).toPath()));
        String yamlContent = convertJenkinsToYaml(jenkinsFileContent);
        String workflowFolderPath = getWorkflowFolderPath(clonedRepoDir, serviceName);

        // Merge new workflow with the existing workflows for that service
        mergeWorkflows(workflowFolderPath, serviceName, yamlContent);

        // Delete the existing remote repo on GitHub (if necessary)
        GitHubRepositoryHandler.deleteRepository(destinationRepoName, gitHubAccessToken);

        // Recreate the destination repo (to ensure clean slate)
        createNewGitHubRepository(destinationRepoName, gitHubAccessToken);

        // Push the merged workflows back to GitHub
        GitHubRepositoryHandler.createAndPushToNewRepo(destinationRepoUrl, clonedRepoDir, gitHubAccessToken);

        return "Successfully updated and pushed workflows to: " + destinationRepoUrl;
    }

    // Method to merge new workflow with existing workflows
    private void mergeWorkflows(String workflowFolderPath, String serviceName, String newYamlContent) throws Exception {
        File serviceWorkflowFolder = new File(workflowFolderPath, serviceName);
        if (!serviceWorkflowFolder.exists()) {
            serviceWorkflowFolder.mkdirs();
        }

        // Read existing workflows (if any)
        File workflowFile = new File(serviceWorkflowFolder, "jenkins.yml");
        String existingContent = "";
        if (workflowFile.exists()) {
            existingContent = new String(Files.readAllBytes(workflowFile.toPath()));
        }

        // Append new content to existing workflows
        String updatedContent = existingContent + "\n" + newYamlContent;

        // Write the updated workflow content back to the file
        Files.write(workflowFile.toPath(), updatedContent.getBytes());
    }

    // Create GitHub Action YAML
    private void createGitHubActionYaml(String workflowFolderPath, String yamlContent) throws Exception {
        File workflowFolder = new File(workflowFolderPath);
        if (!workflowFolder.exists()) {
            boolean created = workflowFolder.mkdirs();
            System.out.println("Created workflow folder at: " + workflowFolder.getAbsolutePath() + " - " + created);
        }

        File yamlFile = new File(workflowFolder, "jenkins.yml");
        Files.write(yamlFile.toPath(), yamlContent.getBytes());
//        System.out.println("GitHub Action workflow file created at: " + yamlFile.getAbsolutePath());
    }

    // Get the folder path for the workflow based on serviceName
    private String getWorkflowFolderPath(File clonedRepoDir, String serviceName) {
    	
    	if(isCustomRepo(clonedRepoDir)) {
    		return clonedRepoDir.getAbsolutePath() + "/root/services/" + serviceName + "/.github/workflows";
    	}
    	else {
    		
    		return clonedRepoDir.getAbsolutePath() + "/.github/workflows";
    	}
    }

    // Clone the repository
    private void cloneRepository(String repoUrl, File destination, String token) throws Exception {
        Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(destination)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, ""))
                .call();
    }

    // Create a new GitHub repository
    private void createNewGitHubRepository(String serviceName, String token) throws Exception {
        GitHubRepositoryHandler.createNewRepository(serviceName, token);
    }



    private File findJenkinsFile(String serviceName, File clonedRepoDir, boolean isCustomRepo) {
        if (isCustomRepo) {
            return findJenkinsFileForService(serviceName, clonedRepoDir);
        } else {
            return findJenkinsFileForDefaultService(serviceName, clonedRepoDir);
        }
    }

    private File findJenkinsFileForService(String serviceName, File clonedRepoDir) {
        File serviceDir = new File(clonedRepoDir, "root/services/" + serviceName);
        if (serviceDir.exists() && serviceDir.isDirectory()) {
            File jenkinsFile = new File(serviceDir, "Jenkinsfile");
            if (jenkinsFile.exists()) {
//                System.out.println("Found Jenkinsfile for service: " + serviceName);
                return jenkinsFile;
            }
        }
//        System.out.println("Jenkinsfile not found for service: " + serviceName);
        return null;
    }

    private File findJenkinsFileForDefaultService(String serviceName, File clonedRepoDir) {
        File jenkinsFile = new File(clonedRepoDir, "Jenkinsfile");
        if (jenkinsFile.exists()) {
//            System.out.println("Found Jenkinsfile at the root of the repository.");
            return jenkinsFile;
        }
        System.out.println("Jenkinsfile not found at the root.");
        return null;
    }

    public String convertJenkinsToYaml(String jenkinsPipeline) throws Exception {

    	// Create an instance of JenkinsToYamlConverter

    	JenkinsToYamlConverter converter = new JenkinsToYamlConverter();


    	// Call the non-static method using the instance

    	return converter.convertJenkinsfileToYaml(jenkinsPipeline);

    	}

    private boolean isCustomRepo(File clonedRepoDir) {
        File rootDirectory = new File(clonedRepoDir, "root");
        File servicesDirectory = new File(rootDirectory, "services");

        return servicesDirectory.exists() && servicesDirectory.isDirectory();
    }

}