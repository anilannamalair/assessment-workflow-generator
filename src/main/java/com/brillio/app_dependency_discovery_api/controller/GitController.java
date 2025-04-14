package com.brillio.app_dependency_discovery_api.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import com.brillio.app_dependency_discovery_api.service.GitLabService;
import com.brillio.app_dependency_discovery_api.service.GitService;
import com.brillio.app_dependency_discovery_api.service.RetainWorkflow;
import com.brillio.app_dependency_discovery_api.service.WorkflowService;

@RestController
@RequestMapping("/api/git")
public class GitController {

	@Value("${github.token}")
    private String githubApiToken;
	
	private static final Logger logger = LoggerFactory.getLogger(GitController.class);
	private RestTemplate restTemplate = new RestTemplate();

	@Autowired
	private RetainWorkflow retainWorkflow;

	@Autowired
	private GitService gitService;

	@Autowired
	private WorkflowService workflowService;
	
	@Autowired
	private  GitLabService gitLabService;

 

	// Clone repository
	@PostMapping("/clone")
	public String cloneRepository(@RequestParam String repoUrl) {
		try {
			gitService.cloneRepository(repoUrl);
			return "Repository cloned successfully";
		} catch (Exception e) {
			return "Error cloning repository: " + e.getMessage();
		}
	}
	
	@GetMapping("/repos")
    public List<Map<String, String>> getOrganizationRepositories(@RequestParam String orgUrl) {

        String orgName = extractOrgName(orgUrl);
        if (orgName == null) {
            throw new IllegalArgumentException("Invalid GitHub organization URL.");
        }

        String url = UriComponentsBuilder.fromHttpUrl("https://api.github.com/orgs/{orgName}/repos")
                .buildAndExpand(orgName)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + githubApiToken);

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(headers),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );

        if (response.getBody() == null) {
            return List.of();
        }

        return response.getBody().stream()
                .map(repo -> {
                    Map<String, String> repoDetails = new java.util.HashMap<>();
                    repoDetails.put("serviceName", (String) repo.get("name"));
                    repoDetails.put("repoUrl", (String) repo.get("html_url"));
                    return repoDetails;
                })
                .collect(Collectors.toList());
    }

    private String extractOrgName(String orgUrl) {
        String regex = "https://github.com/([^/]+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(orgUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
	    

//	// Get dependencies for a specific application
//	@GetMapping("/dependencies")
//	public String getDependencies(@RequestParam String repoUrl, @RequestParam String appName) {
//		try {
//			return gitService.getDependencies(appName, repoUrl);
//		} catch (Exception e) {
//			throw new RuntimeException("Error fetching dependencies", e);
//		}
//	}

	@PostMapping("/assessment")
	public ResponseEntity<Map<String, Object>> uploadExcelFile(@RequestParam("file") MultipartFile file) {
		Map<String, Object> response = new HashMap<>();

		try {
			// Process the CSV file using the GitService method
			List<Map<String, Object>> repoDetailsList = gitService.processCsvFile(file);

			// Populate the response
			response.put("status", "success");
			response.put("message", "File processed successfully!");
			response.put("repoDetails", repoDetailsList); // Include the repo details in the response

			// Return a 200 OK response with the success message and data
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();

			// In case of an error, return a response with an error status
			response.put("status", "error");
			response.put("message", "Error processing file: " + e.getMessage());

			// Return a 500 Internal Server Error response
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	// Endpoint to clone a repository and generate workflow
	@PostMapping("/generate")
	public ResponseEntity<Map<String, Object>> cloneAndGenerateWorkflow(@RequestParam("serviceName") String serviceName,
	        @RequestParam("repoUrl") String repoUrl) {

	    // Call the WorkflowService to clone the repo and generate the workflow
	    String result = workflowService.cloneAndGenerateWorkflow(serviceName, repoUrl);

	    // Create a Map to hold the response data
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "success");
	    response.put("message", "Workflow generated successfully");
	    response.put("data", result); // The result can be any object you want to return

	    // Return the response as a JSON object
	    return new ResponseEntity<>(response, HttpStatus.OK);
	}

	
	@PostMapping("/specific-assessment")
	public ResponseEntity<Map<String, Object>> generateAssessment(String repoUrl) {
		Map<String, Object> response = new HashMap<>();

		try {
			// Process the CSV file using the GitService method
			Map<String, Object> repoDetailsList = gitService.generateAssessment(repoUrl);

			// Populate the response
			response.put("status", "success");
			response.put("message", "r processed successfully!");
			response.put("repoDetails", repoDetailsList); // Include the repo details in the response

			// Return a 200 OK response with the success message and data
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();

			// In case of an error, return a response with an error status
			response.put("status", "error");
			response.put("message", "Error processing file: " + e.getMessage());

			// Return a 500 Internal Server Error response
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}