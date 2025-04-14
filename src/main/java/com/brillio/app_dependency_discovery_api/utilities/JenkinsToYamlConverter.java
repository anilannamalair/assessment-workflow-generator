package com.brillio.app_dependency_discovery_api.utilities;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class JenkinsToYamlConverter {

	@Value("${github.token}")
	private String gitHubToken;
	Map<String, String> groovyScriptsMap = new HashMap<>();

	public String convertJenkinsfileToYaml(String jenkinsfileContent) throws Exception {
		String sharedLibraryRepoUrl = "https://github.com/anilannamalair/my-shared-library";
		System.out.println(jenkinsfileContent);
		File sharedLibraryDir = cloneSharedLibraryRepository(sharedLibraryRepoUrl, gitHubToken);

		printGroovyScriptContents(sharedLibraryDir);

		Map<String, Object> workflow = new HashMap<>();
		workflow.put("name", "Jenkins to GitHub Actions");
		workflow.put("on", "push");

		Map<String, Object> jobs = new HashMap<>();
		List<Map<String, Object>> jenkinsJobs = new ArrayList<Map<String,Object>>();
		if(jenkinsfileContent.contains("@Library")) {
			jenkinsJobs = parseSharedJenkinsStages(jenkinsfileContent, sharedLibraryDir);
		}else {
			jenkinsJobs = parseJenkinsStages(jenkinsfileContent, sharedLibraryDir);
		}
	
		for (Map<String, Object> jenkinsJob : jenkinsJobs) {
			jobs.putAll(jenkinsJob);
		}

		workflow.put("jobs", jobs);

		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		Yaml yaml = new Yaml(options);
		StringWriter writer = new StringWriter();
		yaml.dump(workflow, writer);

		deleteDirectory(sharedLibraryDir);

		String yamlContent = writer.toString();
		System.out.println("\nGenerated YAML Content:\n" + yamlContent);

		return yamlContent;
	}

	private void printGroovyScriptContents(File sharedLibraryDir) throws IOException {
		File varsDir = new File(sharedLibraryDir, "vars");

		if (varsDir.exists() && varsDir.isDirectory()) {
			File[] groovyFiles = varsDir.listFiles((dir, name) -> name.endsWith(".groovy"));
			if (groovyFiles != null) {
				System.out.println("\nGroovy Script Contents:");
				for (File groovyFile : groovyFiles) {
					String scriptContent = new String(Files.readAllBytes(groovyFile.toPath()));
					groovyScriptsMap.put(groovyFile.getName(), scriptContent);
				}

				for (Map.Entry<String, String> entry : groovyScriptsMap.entrySet()) {
					System.out.println("\n" + entry.getKey() + ":");
					System.out.println(entry.getValue());
				}
			}
		}
	}

	private List<Map<String, Object>> parseSharedJenkinsStages(String jenkinsfileContent, File sharedLibraryDir)
			throws IOException {
		List<Map<String, Object>> jobsList = new ArrayList<>();
		Pattern stagePattern = Pattern.compile(
				"stage\\s*\\(['\"](.*?)['\"]\\)\\s*\\{.*?steps\\s*\\{.*?script\\s*\\{(.*?)\\}", Pattern.DOTALL);
		Matcher stageMatcher = stagePattern.matcher(jenkinsfileContent);

		String previousJobId = null;

		while (stageMatcher.find()) {
			String stageName = stageMatcher.group(1);
			String scriptBlock = stageMatcher.group(2);

			System.out.println("Processing stage: " + stageName);
			System.out.println("Script block for stage " + stageName + ":");
			System.out.println(scriptBlock.trim());

			// Extract function calls
			Pattern functionCallPattern = Pattern.compile("(\\w+)\\(");
			Matcher functionCallMatcher = functionCallPattern.matcher(scriptBlock);

			while (functionCallMatcher.find()) {
				String functionName = functionCallMatcher.group(1);
				String command = mapFunctionCallToGitHubAction(functionName, sharedLibraryDir);

				if (command != null) {
					System.out.println("Adding command from function call: " + command);

					Map<String, Object> job = new HashMap<>();
					job.put("runs-on", "ubuntu-latest");
					job.put("steps", List.of(Map.of("name", "Checkout Source Code", "uses", "actions/checkout@v3"),
							Map.of("name", stageName + ": " + functionName, "run", command)));

					if (previousJobId != null) {
						job.put("needs", previousJobId);
					}

					String jobId = stageName.toLowerCase().replace(" ", "-") + "-" + functionName.hashCode();
					jobsList.add(Map.of(jobId, job));
					previousJobId = jobId;
				}
			}
		}
		return jobsList;
	}

	private String mapFunctionCallToGitHubAction(String functionName, File sharedLibraryDir) throws IOException {
		if ("sh".equals(functionName) || "echo".equals(functionName)) {
			return functionName; // Already handled in parseJenkinsStages
		}

		String scriptName = functionName + ".groovy";
		if (groovyScriptsMap.containsKey(scriptName)) {
			String scriptContent = groovyScriptsMap.get(scriptName);
			return extractAndPrintCommand(scriptContent);
		}
		return null;
	}

	public static String extractAndPrintCommand(String script) {
		String regex = "def process = \"([^\"]+)\"\\.execute\\(\\)";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(script);

		if (matcher.find()) {
			String command = matcher.group(1);
			return command;
		} else {
			Pattern echoPattern = Pattern.compile("echo\\s+\"(.*?)\"");
			Matcher echoMatcher = echoPattern.matcher(script);
			if (echoMatcher.find()) {
				return "echo \"" + echoMatcher.group(1) + "\"";
			}
			return null;
		}
	}

	private File cloneSharedLibraryRepository(String repoUrl, String gitHubToken) throws Exception {
		String uniqueDirName = "sharedLibrary_" + UUID.randomUUID().toString();
		File sharedLibraryDir = new File(uniqueDirName);
		System.out.println("Cloning shared library repository from: " + repoUrl);

		Git.cloneRepository().setURI(repoUrl).setDirectory(sharedLibraryDir)
				.setCredentialsProvider(new UsernamePasswordCredentialsProvider(gitHubToken, "")).call();
		System.out.println("Cloned shared library repository to: " + sharedLibraryDir.getAbsolutePath());
		return sharedLibraryDir;
	}

	private void deleteDirectory(File directory) {
		if (directory.exists()) {
			File[] allContents = directory.listFiles();
			if (allContents != null) {
				for (File file : allContents) {
					deleteDirectory(file);
				}
			}
			directory.delete();
			System.out.println("Deleted cloned directory: " + directory.getAbsolutePath());
		}
	}

    private List<Map<String, Object>> parseJenkinsStages(String jenkinsfileContent, File sharedLibraryDir) throws IOException {
        if(jenkinsfileContent.contains("npm")) {
        	return parseJenkinsStagesforReact(jenkinsfileContent,sharedLibraryDir);
        }
       else {
        	return parseJenkinsStagesforJava(jenkinsfileContent,sharedLibraryDir);
        }
    }

	private List<Map<String, Object>> parseJenkinsStagesforJava(String jenkinsfileContent, File sharedLibraryDir) throws IOException {
		List<Map<String, Object>> jobsList = new ArrayList<>();
		

		Pattern stagePattern = Pattern.compile("stage\\s*\\(['\"](.*?)['\"]\\)\\s*\\{.*?steps\\s*\\{(.*?)\\}",
				Pattern.DOTALL);

		Matcher stageMatcher = stagePattern.matcher(jenkinsfileContent);

		String previousJobId = null;

		while (stageMatcher.find()) {

			String stageName = stageMatcher.group(1);

			String stepsBlock = stageMatcher.group(2);

			System.out.println("Processing stage: " + stageName);

			System.out.println("Steps block for stage " + stageName + ":");

			System.out.println(stepsBlock.trim());

			Pattern shPattern = Pattern.compile("sh\\s*['\"](.*?)['\"]");

			Pattern echoPattern = Pattern.compile("echo\\s*['\"](.*?)['\"]");

			Matcher shMatcher = shPattern.matcher(stepsBlock);

			if (shMatcher.find()) {

				String command = shMatcher.group(1);

				System.out.println("Adding shell command: " + command);

				Map<String, Object> job = new HashMap<>();

				job.put("runs-on", "ubuntu-latest");

				job.put("steps", List.of(

						Map.of("name", "Checkout Source Code", "uses", "actions/checkout@v3"),

						Map.of("name", stageName + ": Run shell command", "run", command)

				));

				if (previousJobId != null) {

					job.put("needs", previousJobId);

				}

				String jobId = stageName.toLowerCase().replace(" ", "-") + "-" + command.hashCode();

				jobsList.add(Map.of(jobId, job));

				previousJobId = jobId;

			}

			Matcher echoMatcher = echoPattern.matcher(stepsBlock);

			if (echoMatcher.find()) {

				String message = echoMatcher.group(1);

				System.out.println("Adding echo command: " + message);

				Map<String, Object> job = new HashMap<>();

				job.put("runs-on", "ubuntu-latest");

				job.put("steps", List.of(

						Map.of("name", "Checkout Source Code", "uses", "actions/checkout@v3"),

						Map.of("name", stageName + ": Echo message", "run", "echo \"" + message + "\"")

				));

				if (previousJobId != null) {

					job.put("needs", previousJobId);

				}

				String jobId = stageName.toLowerCase().replace(" ", "-") + "-echo-" + message.hashCode();

				jobsList.add(Map.of(jobId, job));

				previousJobId = jobId;

			}

		}

		return jobsList;

	}
	

	private List<Map<String, Object>> parseJenkinsStagesforReact(String jenkinsfileContent, File sharedLibraryDir) {
		List<Map<String, Object>> jobsList = new ArrayList<>();

        // Pattern to match stages in the Jenkinsfile (considering npm install and npm run build commands)
        Pattern stagePattern = Pattern.compile("stage\\s*\\(['\"](.*?)['\"]\\)\\s*\\{.*?steps\\s*\\{(.*?)\\}",
                Pattern.DOTALL);

        Matcher stageMatcher = stagePattern.matcher(jenkinsfileContent);

        String previousJobId = null;

        while (stageMatcher.find()) {
            String stageName = stageMatcher.group(1);
            String stepsBlock = stageMatcher.group(2);

            System.out.println("Processing stage: " + stageName);
            System.out.println("Steps block for stage " + stageName + ":");
            System.out.println(stepsBlock.trim());

            // Handling npm install command (will ensure install in each job)
            Pattern npmInstallPattern = Pattern.compile("sh\\s*['\"]npm install['\"]");
            Matcher npmInstallMatcher = npmInstallPattern.matcher(stepsBlock);
            boolean hasNpmInstall = npmInstallMatcher.find();

            // Job for npm install step
            if (hasNpmInstall) {
                Map<String, Object> job = new HashMap<>();
                job.put("runs-on", "ubuntu-latest");
                job.put("steps", List.of(
                        Map.of("name", "Checkout Source Code", "uses", "actions/checkout@v3"),
                        Map.of("name", "Remove existing node_modules and package-lock.json", "run", "rm -rf node_modules package-lock.json"),
                        Map.of("name", "Install Dependencies", "run", "npm install") // Running npm install after clearing the cache
                ));

                if (previousJobId != null) {
                    job.put("needs", previousJobId);
                }

                String jobId = stageName.toLowerCase().replace(" ", "-") + "-npm-install-" + "install".hashCode();
                jobsList.add(Map.of(jobId, job));
                previousJobId = jobId;
            }

            // Handling npm run build command
            Pattern npmRunBuildPattern = Pattern.compile("sh\\s*['\"]npm run build['\"]");
            Matcher npmRunBuildMatcher = npmRunBuildPattern.matcher(stepsBlock);
            boolean hasBuildCommand = npmRunBuildMatcher.find();

            if (hasBuildCommand) {
                Map<String, Object> job = new HashMap<>();
                job.put("runs-on", "ubuntu-latest");
                job.put("steps", List.of(
                        Map.of("name", "Checkout Source Code", "uses", "actions/checkout@v3"),
                        Map.of("name", "Remove existing node_modules and package-lock.json", "run", "rm -rf node_modules package-lock.json"),
                        Map.of("name", "Install Dependencies", "run", "npm install"),
                        Map.of("name", "Build Application", "run", "npm run build")
                ));

                if (previousJobId != null) {
                    job.put("needs", previousJobId);
                }

                String jobId = stageName.toLowerCase().replace(" ", "-") + "-npm-build-" + "build".hashCode();
                jobsList.add(Map.of(jobId, job));
                previousJobId = jobId;
            }
        }

        return jobsList;
	}

}