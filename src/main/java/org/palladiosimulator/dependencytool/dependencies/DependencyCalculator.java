package org.palladiosimulator.dependencytool.dependencies;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
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
    
    private final UpdateSiteTypes updateSiteType;
    private final String updateSiteUrl;
    private final boolean includeImports;
    private final Set<String> reposToIgnore;
    private final boolean includeArchived;
    private final boolean includeNoUpdateSite;
    
    private Set<RepositoryObject> repositories;

    /**
     * Create a new DependencyCalculator object to handle the assignment of dependencies.
     *
     * @param type The type of the update site to analyze. Can be nightly or release.
     * @throws IOException 
     */
    public DependencyCalculator(final String updateSiteUrl,
                                final UpdateSiteTypes type,
                                final boolean includeImports,
                                final Set<String> reposToIgnore,
                                final boolean includeArchived,
                                final boolean includeNoUpdateSite) throws IOException {
        this.repositories = new HashSet<>();
        this.updateSiteUrl = updateSiteUrl;
        this.updateSiteType = type;
        this.includeImports = includeImports;
        this.reposToIgnore = reposToIgnore;
        this.includeArchived = includeArchived;
        this.includeNoUpdateSite = includeNoUpdateSite;
    }

    public void add(GHRepository repository) {
        addAll(List.of(repository));
    }

    public void addAll(Collection<GHRepository> repositories) {
        ExecutorService ex = Executors.newFixedThreadPool(128);

        this.repositories.addAll(repositories
            .parallelStream()
            .filter(e -> !reposToIgnore.contains(e.getName()) && !reposToIgnore.contains(e.getFullName()))
            .filter(e -> includeArchived || !e.isArchived())
            .sequential()
            .map(e -> ex.submit(() -> {
                try {
                    return new RepositoryObject(e, updateSiteUrl, updateSiteType, includeImports);
                } catch (IOException | ParserConfigurationException | SAXException exception) {
                    throw new RuntimeException(exception);
                }
            }))
            .parallel()
            .map(e -> {
                try {
                    return e.get();
                } catch (InterruptedException | ExecutionException e1) {
                    LOGGER.warning("Exception while waiting for future to complete");
                    return null;
                }
            })
            .filter(e -> e != null)
            .filter(e -> includeNoUpdateSite || e.getUpdateSite() != null)
            .collect(Collectors.toSet()));

        ex.shutdown();
        try {
            ex.awaitTermination(15, TimeUnit.MINUTES);
        } catch (InterruptedException e1) {
            LOGGER.warning("Interrupted while waiting for executor termination");
        }
    }
    
    /**
     * Returns the repository dependencies as map.
     * 
     * The first entry depends on every repository in the second entry.
     *
     * @return     A map representing the dependencies between repositories.
     */
    public Map<RepositoryObject, Set<RepositoryObject>> getDependencies() {
        final Map<String, RepositoryObject> providedBundleToRepo = reverseProvidedMap(RepositoryObject::getProvidedBundles);
        final Map<String, RepositoryObject> providedFeatureToRepo = reverseProvidedMap(RepositoryObject::getProvidedFeatures);

        final Map<RepositoryObject, Set<RepositoryObject>> dependencies = new HashMap<>();
        for (RepositoryObject repo : repositories) {
            final Set<RepositoryObject> repoDependencies = new HashSet<>();
            repoDependencies.addAll(resolveDependencies(repo.getRequiredBundles(), providedBundleToRepo));
            repoDependencies.addAll(resolveDependencies(repo.getRequiredFeatures(), providedFeatureToRepo));

            // loops not allowed by jgrapht
            repoDependencies.remove(repo);
            dependencies.put(repo, repoDependencies);
        }

        return dependencies;
    }

    private Set<RepositoryObject> resolveDependencies(Set<String> required, Map<String, RepositoryObject> providedToRepo) {
        final Set<RepositoryObject> repoDependencies = new HashSet<>();

        for (String dependency : required) {
            if (providedToRepo.containsKey(dependency)) {
                repoDependencies.add(providedToRepo.get(dependency));
            } else {
                LOGGER.warning(dependency + "is not provided by any repository.");
            }
        }

        return repoDependencies;
    }

    private Map<String, RepositoryObject> reverseProvidedMap(Function<RepositoryObject, Set<String>> map) {
        final Map<String, Set<RepositoryObject>> reverseMapWithDuplicates = new HashMap<>();
        final Map<String, RepositoryObject> reverseMap = new HashMap<>();

        for (RepositoryObject repo : repositories) {
            Set<String> reverseKeys = map.apply(repo);
            for (String reverseKey : reverseKeys) {
                reverseMap.put(reverseKey, repo);

                Set<RepositoryObject> duplicates = reverseMapWithDuplicates.getOrDefault(reverseKey, new HashSet<>());
                duplicates.add(repo);
                reverseMapWithDuplicates.put(reverseKey, duplicates);
            }
        }

        // Make sure every bundle/feature/.. is provided by exactly one repository.
        for (Map.Entry<String, Set<RepositoryObject>> entry : reverseMapWithDuplicates.entrySet()) {
            if (entry.getValue().size() > 1) {
                List<String> repoNames = entry.getValue().stream().map(RepositoryObject::getName).toList();
                LOGGER.warning(entry.getKey()  + " is provided by multiple repositories: " + repoNames + " using " + reverseMap.get(entry.getKey()) + ".");
            }
        }

        return reverseMap;
    }
}

