package com.brillio.app_dependency_discovery_api.utilities;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ComplexityClassifier {

   private static final Pattern LIBRARY_PATTERN = Pattern.compile("^\\s*@Library\\s*\\([\"'].*?[\"']\\)\\s*\\S*.*$", Pattern.MULTILINE);
    private static final Pattern STAGE_PATTERN = Pattern.compile("\\bstage\\s*\\([\"'].*?[\"']\\)", Pattern.MULTILINE);
    private static final Pattern CONDITIONAL_PATTERN = Pattern.compile("\\b(if|when)\\s*\\(", Pattern.MULTILINE);
    private static final Pattern PARALLEL_PATTERN = Pattern.compile("\\bparallel\\s*\\{", Pattern.MULTILINE);
    private static final Pattern LOOP_PATTERN = Pattern.compile("\\b(for|while)\\s*\\(", Pattern.MULTILINE);

    public enum Complexity {
        SIMPLE, MEDIUM, COMPLEX, UNKNOWN
    }

    private String name;

    public ComplexityClassifier() {
        this.name = "classify complexity";
    }

    public static String classifyJenkinsfile(String jenkinsfileContent) {
        System.out.println("Jenkinsfile Content:\n" + jenkinsfileContent);

        System.out.println("Original Content Characters:");
        for (int i = 0; i < jenkinsfileContent.length(); i++) {
            System.out.println("Char " + i + ": " + (int) jenkinsfileContent.charAt(i) + " (" + jenkinsfileContent.charAt(i) + ")");
        }

        if (jenkinsfileContent == null || jenkinsfileContent.trim().isEmpty()) {
            return Complexity.UNKNOWN.toString();
        }

        if (LIBRARY_PATTERN.matcher(jenkinsfileContent).find()) {
            return Complexity.COMPLEX.toString();
        }

        int stageCount = countMatches(STAGE_PATTERN, jenkinsfileContent);
        int conditionalCount = countMatches(CONDITIONAL_PATTERN, jenkinsfileContent);
        int parallelCount = countMatches(PARALLEL_PATTERN, jenkinsfileContent);
        int loopCount = countMatches(LOOP_PATTERN, jenkinsfileContent);

        System.out.println("Stage Count: " + stageCount);
        System.out.println("Conditional Count: " + conditionalCount);
        System.out.println("Parallel Count: " + parallelCount);
        System.out.println("Loop Count: " + loopCount);

        String strippedContent = removeStageContent(jenkinsfileContent);
        System.out.println("strippedContent: \n" + strippedContent);

        System.out.println("Stripped Content Characters:");
        for (int i = 0; i < strippedContent.length(); i++) {
            System.out.println("Char " + i + ": " + (int) strippedContent.charAt(i) + " (" + strippedContent.charAt(i) + ")");
        }

        if (strippedContent.contains("sh")) {
            System.out.println("Medium due to SH in stripped content");
            return Complexity.MEDIUM.toString();
        }

        if (strippedContent.contains("node {") || strippedContent.contains("bat")
                || strippedContent.contains("script {")) {
            System.out.println("Medium due to stripped content");
            return Complexity.MEDIUM.toString();
        }

        if (jenkinsfileContent.contains("pipeline {") && stageCount <= 5 && conditionalCount == 0
                && parallelCount == 0 && loopCount == 0) {
            System.out.println("Simple due to pipeline and counts");
            return Complexity.SIMPLE.toString();
        }

        if (stageCount > 5 || conditionalCount > 0 || parallelCount > 0 || loopCount > 0) {
            System.out.println("Medium due to counts");
            return Complexity.MEDIUM.toString();
        }

        System.out.println("Unknown");
        return Complexity.UNKNOWN.toString();
    }

    private static String removeStageContent(String jenkinsfileContent) {
        String result = jenkinsfileContent;
        int startIndex = result.indexOf("stage(");
        while (startIndex != -1) {
            int openBraceIndex = result.indexOf("{", startIndex);
            if (openBraceIndex == -1) {
                break; // Malformed stage definition
            }
            int closeBraceIndex = findMatchingClosingBrace(result, openBraceIndex);
            if (closeBraceIndex == -1) {
                break; // Malformed stage definition
            }
            result = result.substring(0, startIndex) + result.substring(closeBraceIndex + 1);
            startIndex = result.indexOf("stage(");
        }
        return result;
    }

    private static int findMatchingClosingBrace(String text, int openBraceIndex) {
        int braceCount = 1;
        for (int i = openBraceIndex + 1; i < text.length(); i++) {
            if (text.charAt(i) == '{') {
                braceCount++;
            } else if (text.charAt(i) == '}') {
                braceCount--;
                if (braceCount == 0) {
                    return i;
                }
            }
        }
        return -1; // No matching closing brace found
    }

    private static int countMatches(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    public String getName() {
        return name;
    }


}