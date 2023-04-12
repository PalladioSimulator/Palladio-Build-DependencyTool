package org.palladiosimulator.dependencytool.dependencies;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;

import org.kohsuke.github.GHRepository;
import org.palladiosimulator.dependencytool.github.RepositoryObject;
import org.xml.sax.SAXException;

/**
 *  Handles the assignment of provided objects to required objects.
 */
public class DependencyCalculator {

    private static final Logger LOGGER = Logger.getLogger(DependencyCalculator.class.getName());
    
    private final UpdateSiteTypes type;
    private final String updateSiteUrl;
    private final boolean includeImports;
    private final Set<String> reposToIgnore;
    private final boolean includeArchived;
    
    private Map<String, RepositoryObject> repositories;
    
    private Map<String, List<String>> mappingFeatureRepository;
    private Map<String, List<String>> mappingBundleRepository;

    /**
     * Create a new DependencyCalculator object to handle the assignment of dependencies.
     *
     * @param type The type of the update site to analyze. Can be nightly or release.
     * @throws IOException 
     */
    public DependencyCalculator(String updateSiteUrl, UpdateSiteTypes type, final boolean includeImports, final Set<String> reposToIgnore, final boolean includeArchived) throws IOException {
        this.repositories = new HashMap<>();
        this.updateSiteUrl = updateSiteUrl;
        this.type = type;
        this.includeImports = includeImports;
        this.reposToIgnore = reposToIgnore;
        this.includeArchived = includeArchived;
    }

    public void add(GHRepository repository) {
        addAll(List.of(repository));
    }

    public synchronized void addAll(Collection<GHRepository> repositories) {
        ExecutorService ex = Executors.newFixedThreadPool(128);

        List<Future<RepositoryObject>> futureRepositories = repositories
            .parallelStream()  // Creating RepositoryObjects calls to GitHub, use parallel requests.
            .filter(e -> !reposToIgnore.contains(e.getName()) && !reposToIgnore.contains(e.getFullName()))
            .filter(e -> includeArchived || !e.isArchived())
            .map(e -> ex.submit(() -> {
                try {
                    return new RepositoryObject(e, updateSiteUrl, includeImports);
                } catch (IOException | ParserConfigurationException | SAXException exception) {
                    throw new RuntimeException(exception);
                }
            }))
            .toList();
        ex.shutdown();
        try {
            ex.awaitTermination(15, TimeUnit.MINUTES);
        } catch (InterruptedException e1) {
            LOGGER.warning("Interrupted while waiting for executor termination");
        }

        for (Future<RepositoryObject> future : futureRepositories) {
            try {
                RepositoryObject repo = future.get();
                this.repositories.put(repo.getRepositoryName(), repo);
            } catch (InterruptedException | ExecutionException e1) {
                LOGGER.warning("Exception while waiting for future to complete");
            }
        }
    }
    
    /**
     * Assign dependencies to the repositories.
     * 
     * @param includeImports Considers imports in the feature.xml while calculating dependencies if true.
     * @return A Set of RepositoryObjects with their dependencies set in RepositoryObject.getDependency();
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public synchronized Set<RepositoryObject> calculateDependencies(Boolean includeImports) throws IOException, ParserConfigurationException, SAXException {
        calculateProvided();
        for (Entry<String, RepositoryObject> repo : repositories.entrySet()) {
            repo.getValue().addDependencies(mapRequirementToRepo(repo.getKey()));
        }
        return new HashSet<>(repositories.values());
    }
    
    
    // Get all bundles and features provided by update sites in "UpdateSiteToRepo.txt".
    private void calculateProvided() throws IOException {
        Map<String, Set<String>> providedBundleMap = new HashMap<>();
        Map<String, Set<String>> providedFeatureMap = new HashMap<>();
        
        try (P2RepositoryReader repoReader = new P2RepositoryReader()) {
            List<String> noUpdateSite = new ArrayList<>();
            for (String repo : repositories.keySet()) {
                String updateSite = repositories.get(repo).getUpdateSite() + type.toString() + "/";
                Set<String> tmpBundle = repoReader.readProvidedBundles(updateSite);
                Set<String> tmpFeature = repoReader.readProvidedFeatures(updateSite);
                
                // filter repositories without an update site
                if (tmpBundle.isEmpty() && tmpFeature.isEmpty()) {
                    noUpdateSite.add(repo);
                } else {
                    providedBundleMap.put(repo, tmpBundle);
                    providedFeatureMap.put(repo, tmpFeature);
                }
            }
            // remove repositories without an update site
            noUpdateSite.forEach(repositories::remove);
            
            mappingBundleRepository = verifyUnique(providedBundleMap);
            mappingFeatureRepository = verifyUnique(providedFeatureMap);
        }
    }

    // Adds repositories which contain the required bundles and/or features to the dependency of the requiring repository.
    private Set<RepositoryObject> mapRequirementToRepo(String repoName) {
        Set<RepositoryObject> repositoryDependencies = new HashSet<>();
        repositoryDependencies.addAll(mapRequirement(repoName, mappingBundleRepository, repositories.get(repoName).getRequiredBundles()));
        repositoryDependencies.addAll(mapRequirement(repoName, mappingFeatureRepository, repositories.get(repoName).getRequiredFeatures()));
        return repositoryDependencies;
    }
    
    // Compares provided objects with required objects and returns all matching objects.
    private Set<RepositoryObject> mapRequirement(String repoName, Map<String, List<String>> mapRequiredToRepo, Set<String> required) {
        Set<RepositoryObject> repositoryDependencies = new HashSet<>();
        for (String requirement : required) {
            if (mapRequiredToRepo.containsKey(requirement)) {
                String repo = mapRequiredToRepo.get(requirement).get(0);
                if (!repo.equals(repoName)) {
                    repositoryDependencies.add(repositories.get(mapRequiredToRepo.get(requirement).get(0)));
                }
            }
        }
        return repositoryDependencies;
    }
    
    // Verifies if each element is provided by only one repository.
    private Map<String, List<String>> verifyUnique(Map<String, Set<String>> providedMap) {
        Map<String, List<String>> providedBy = new HashMap<>();
        boolean unique = true;
        // Map repositories to bundles/features.
        for (Entry<String, Set<String>> entry : providedMap.entrySet()) {
            for (String provided : entry.getValue()) {
                List<String> tmp = new ArrayList<>();
                if (providedBy.containsKey(provided)) {
                    tmp = providedBy.get(provided);
                }
                tmp.add(repositories.get(entry.getKey()).getRepositoryName());
                providedBy.put(provided, tmp);
            }
        }
        // Check if more than one repository provides feature or bundle.
        for (Entry<String, List<String>> entry : providedBy.entrySet()) {
            if (entry.getValue().size() != 1) {
                LOGGER.warning(entry.getKey() + " is provided by more than one repository. Please check which of the following repositories is the correct one: " + entry.getValue());
                unique = false;
            }
        }
        //TODO: Wieder einfügen, sobald Problem endlich gelöst wurde.
//        if (!unique) {
//            throw new IllegalArgumentException("One or more items is provided by more than one repository. Please retry when this issue is resolved.");
//        } else {
            return providedBy;
//        }
    }
}

