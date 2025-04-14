package com.brillio.app_dependency_discovery_api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RetainWorkflow {

//    @Value("${github.token}")
//    private String githubToken;
//
//    @Value("${github.apiUrl}")
//    private String githubApiUrl;
//
//    private RestTemplate restTemplate = new RestTemplate();
//
//    // Fetch Jenkinsfile and create GitHub Actions workflow for the service
//    public void createGitHubActionsWorkflow(String owner, String repoName, String serviceName) throws IOException {
//        // Traverse the repo structure to find the service folder
//        String serviceFolderPath = findServiceFolder(owner, repoName, serviceName);
//        if (serviceFolderPath == null) {
//            throw new IOException("Service folder for '" + serviceName + "' not found in the repository.");
//        }
//
//        // Fetch Jenkinsfile content from the service folder
//        String jenkinsFileContent = fetchJenkinsFileContent(owner, repoName, serviceFolderPath);
//
//        // Convert Jenkinsfile to GitHub Actions YAML format
//        String githubActionsYAML = convertJenkinsFileToGithubActionsYAML(jenkinsFileContent);
//
//        // Save the GitHub Actions workflow
//        saveGitHubActionsWorkflow(owner, repoName, serviceName, githubActionsYAML);
//    }
//
//    // Traverse the repository and search for the service folder
//    private String findServiceFolder(String owner, String repoName, String serviceName) {
//        String url = githubApiUrl + "/repos/" + owner + "/" + repoName + "/contents";
//        HttpHeaders headers = new HttpHeaders();
//        headers.set("Authorization", "token " + githubToken);
//        HttpEntity<String> entity = new HttpEntity<>(headers);
//
//        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
//
//        if (response.getStatusCode() == HttpStatus.OK) {
//            JsonNode repoContents = response.getBody();
//            if (repoContents != null && repoContents.isArray()) {
//                for (JsonNode file : repoContents) {
//                    String path = file.get("path").asText();
//                    if (path.contains(serviceName) && file.get("type").asText().equals("dir")) {
//                        return path; // Service folder path found
//                    }
//                }
//            }
//        }
//        return null; // Service folder not found
//    }
//
//    // Fetch the Jenkinsfile content from the service folder
//    private String fetchJenkinsFileContent(String owner, String repoName, String serviceFolderPath) {
//        String url = githubApiUrl + "/repos/" + owner + "/" + repoName + "/contents/" + serviceFolderPath + "/Jenkinsfile";
//        HttpHeaders headers = new HttpHeaders();
//        headers.set("Authorization", "token " + githubToken);
//
//        HttpEntity<String> entity = new HttpEntity<>(headers);
//        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
//
//        if (response.getStatusCode() == HttpStatus.OK) {
//            JsonNode content = response.getBody();
//            String encodedContent = content.get("content").asText();
//            return decodeFromBase64(encodedContent); // Decode the base64 content to get the Jenkinsfile content
//        }
//
//        return null;
//    }
//
//    // Helper method to decode base64 content
//    private String decodeFromBase64(String encodedContent) {
//        byte[] decodedBytes = Base64.getDecoder().decode(encodedContent);
//        return new String(decodedBytes);
//    }
//
//    // Convert Jenkinsfile content to GitHub Actions workflow YAML
////    private String convertJenkinsFileToGithubActionsYAML(String jenkinsFileContent, String serviceName) {
////        // Here we assume you can map a Jenkins pipeline into a GitHub Actions workflow.
////        // Example mapping from a simplified Jenkinsfile to GitHub Actions YAML
////
////        StringBuilder githubActionsYAML = new StringBuilder();
////        githubActionsYAML.append("name: ").append(serviceName).append(" CI Workflow\n");
////        githubActionsYAML.append("on:\n");
////        githubActionsYAML.append("  push:\n");
////        githubActionsYAML.append("    branches:\n");
////        githubActionsYAML.append("      - main\n");
////        githubActionsYAML.append("jobs:\n");
////        githubActionsYAML.append("  build:\n");
////        githubActionsYAML.append("    runs-on: ubuntu-latest\n");
////        githubActionsYAML.append("    steps:\n");
////
////        // A simplified conversion of Jenkinsfile stages to GitHub Actions steps.
////        // This is highly dependent on your Jenkinsfile content; you may need a parser for full conversion
////        githubActionsYAML.append("      - name: Checkout repository\n");
////        githubActionsYAML.append("        uses: actions/checkout@v2\n");
////
////        githubActionsYAML.append("      - name: Run build\n");
////        githubActionsYAML.append("        run: |\n");
////        githubActionsYAML.append("          echo \"Building application\"\n");
////
////        // You can map more stages or steps from the Jenkinsfile content here
////        githubActionsYAML.append("      - name: Run tests\n");
////        githubActionsYAML.append("        run: |\n");
////        githubActionsYAML.append("          echo \"Running tests\"\n");
////
////        return githubActionsYAML.toString();
////    }
////    
//    
//    public static String convertJenkinsFileToGithubActionsYAML(String jenkinsPipeline) {
//        // YAML structure
//        Map<String, Object> yamlMap = new LinkedHashMap<>();
//        yamlMap.put("name", "CI/CD Pipeline");
//
//        // Trigger conditions
//        Map<String, Object> on = new LinkedHashMap<>();
//        on.put("push", Map.of("branches", List.of("main")));
//        on.put("pull_request", Map.of("branches", List.of("main")));
//        yamlMap.put("on", on);
//
//        // Jobs (Extract from Jenkinsfile)
//        Map<String, Object> jobs = new LinkedHashMap<>();
//
//        // Define stages
//        List<String> stages = List.of("Build", "Test", "SonarQube Scan", "Deploy");
//        List<String> commands = List.of(
//                "mvn clean package", 
//                "mvn test", 
//                "mvn sonar:sonar", 
//                "scp target/artifact-1.0.0.jar user@server:/path/to/deploy"
//        );
//
//        for (int i = 0; i < stages.size(); i++) {
//            Map<String, Object> job = new LinkedHashMap<>();
//            job.put("runs-on", "ubuntu-latest");
//
//            // Steps
//            List<Map<String, Object>> steps = new ArrayList<>();
//            steps.add(Map.of("name", "Checkout Repository", "uses", "actions/checkout@v3"));
//            
//            // Java setup (only for Build stage)
//            if (i == 0) {
//                steps.add(Map.of(
//                        "name", "Set Up JDK 17",
//                        "uses", "actions/setup-java@v3",
//                        "with", Map.of("java-version", "17", "distribution", "temurin")
//                ));
//            }
//
//            // Add Jenkins steps as GitHub Actions steps
//            steps.add(Map.of("name", stages.get(i), "run", commands.get(i)));
//
//            job.put("steps", steps);
//
//            // Define dependencies (needs)
//            if (i > 0) {
//                job.put("needs", List.of(stages.get(i - 1).toLowerCase().replace(" ", "_")));
//            }
//
//            jobs.put(stages.get(i).toLowerCase().replace(" ", "_"), job);
//        }
//
//        yamlMap.put("jobs", jobs);
//
//        // Convert to YAML
//        DumperOptions options = new DumperOptions();
//        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
//        options.setPrettyFlow(true);
//        Yaml yaml = new Yaml(options);
//       System.out.println("**********************"+yaml.dump(yamlMap));
//        return yaml.dump(yamlMap);
//    }
//
//    // Save GitHub Actions workflow file in the repository
//    private void saveGitHubActionsWorkflow(String owner, String repoName, String serviceName, String githubActionsYAML) {
//        String workflowDir = "/.github/workflows/" + "-ci.yml"; // Path to GitHub Actions workflow
//
//        String url = githubApiUrl + "/repos/" + owner + "/" + repoName + "/contents" + workflowDir;
//        HttpHeaders headers = new HttpHeaders();
//        headers.set("Authorization", "token " + githubToken);
//        headers.setContentType(MediaType.APPLICATION_JSON);
//
//        String encodedContent = encodeToBase64(githubActionsYAML); // Encode the YAML content to base64
//        String jsonBody = "{ \"message\": \"Add GitHub Actions workflow for " + serviceName + "\", \"content\": \"" + encodedContent + "\" }";
//        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
//
//        restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
//    }
//
//    // Helper method to encode content to Base64
//    private String encodeToBase64(String content) {
//        return new String(Base64.getEncoder().encode(content.getBytes()));
//    }
}
