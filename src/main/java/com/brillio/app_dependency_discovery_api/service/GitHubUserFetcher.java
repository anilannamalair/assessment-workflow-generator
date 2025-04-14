package com.brillio.app_dependency_discovery_api.service;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject; // Requires org.json (or use Gson/Jackson if you prefer)

public class GitHubUserFetcher {

    public static String getAuthenticatedUsername(String accessToken) {
        String apiUrl = "https://api.github.com/user"; // This returns info about the authenticated user
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "token " + accessToken);
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Parse the JSON response
                JSONObject json = new JSONObject(response.toString());
                return json.getString("login"); // This is the GitHub username (owner name)
            } else {
                System.out.println("Failed to fetch user info. Response code: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // Return null if there was an error
    }
}
