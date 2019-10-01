package org.palladiosimulator.dependencytool;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

/**
 * Handler to interact with the GitHub API.
 */
public class GitHubAPIHandler {

    private static final Logger LOGGER = Logger.getLogger(GitHubAPIHandler.class.getName());
    private Map<String, GHRepository> repositories = new HashMap<>();
    
    /**
     * Create a new handler object for the given GitHub organization.
     * 
     * @param organization The name of the GitHub organization with which the handler will interact.
     * @param authToken Authentication token to get a higher query limit.
     * @param reposToIgnore A list of repositories of this organization which should be ignored.
     * @throws IOException
     */
    public GitHubAPIHandler(String organization, String authToken, List<String> reposToIgnore) throws IOException {
        GitHub github = GitHub.connectUsingOAuth(authToken);
        GHOrganization org = github.getOrganization(organization);
        Map<String, GHRepository> tmp = org.getRepositories();
        for (Entry<String, GHRepository> repo : tmp.entrySet()) {
            if (!reposToIgnore.contains(repo.getKey()) && !repo.getValue().isArchived()) {
                repositories.put(repo.getKey(), repo.getValue());
            }
        }
    }
    
    // Return names of all associated repositories.
    public Set<String> getRepoNames() {
        Set<String> names = new HashSet<>();
        names.addAll(repositories.keySet());
        return names;
    }
    
    // Returns a set of strings, containing all names of bundles present for the given repository name.
    public Set<String> getBundles(String repoName) {
        Set<String> bundles = new HashSet<>();
        try {
            for (GHContent bundle : repositories.get(repoName).getDirectoryContent("bundles")) {
                if (bundle.isDirectory()) {
                    bundles.add(bundle.getName());
                }
            }
        } catch (IOException e) {
            LOGGER.warning("No bundles page found for " + repoName + ".");
        }
        return bundles;
    }
    
    // Returns a set of strings, containing all names of features present for the given repository name.
    public Set<String> getFeatures(String repoName) {
        Set<String> features = new HashSet<>();
        try {
            for (GHContent feature : repositories.get(repoName).getDirectoryContent("features")) {
                if (feature.isDirectory()) {
                    features.add(feature.getName());
                }
            }
        } catch (IOException e) {
            LOGGER.warning("No features page found for " + repoName + ".");
        }
        return features;
    }
    
    // Returns the URL for a given repository name.
    public String getRepoPath(String repoName) {
        return repositories.get(repoName).getHtmlUrl().toString();
    }
    
    // Fetches file content from a given file in a given repository.
    public Optional<GHContent> getContentfromFile(String repoName, String filePath) {
        Optional<GHContent> content = Optional.empty();
        try {
            content = Optional.of(repositories.get(repoName).getFileContent(filePath));
        } catch (IOException e) {
            LOGGER.warning("No file found for " + filePath + ".");
        }
        return content;
    }
}