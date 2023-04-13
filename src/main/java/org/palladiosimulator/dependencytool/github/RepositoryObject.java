package org.palladiosimulator.dependencytool.github;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.kohsuke.github.GHRepository;
import org.palladiosimulator.dependencytool.dependencies.FeatureXMLHandler;
import org.palladiosimulator.dependencytool.dependencies.ManifestMFDependencyHandler;
import org.palladiosimulator.dependencytool.dependencies.P2RepositoryReader;
import org.palladiosimulator.dependencytool.dependencies.UpdateSiteTypes;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * An object holding all relevant information about the according repository.
 */
@JsonPropertyOrder({"name", "githubUrl", "updatesiteUrl"})
public class RepositoryObject {

    private static final Logger LOGGER = Logger.getLogger(RepositoryObject.class.getName());

    private final GHRepository repository;
    private String updateSite;

    private Set<String> requiredBundles;
    private Set<String> requiredFeatures;

    private Set<String> providedBundles;
    private Set<String> providedFeatures;

    /**
     * Create a new RepositoryObject from the given repository name.
     * 
     * @param repository The Github repository.
     * @param updateSite The update site to use.
     * @param includeImports If true the dependencies of this repository will also contain imported plugins and features.
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public RepositoryObject(GHRepository repository, String updateSite, UpdateSiteTypes updateSiteType, boolean includeImports) throws IOException, ParserConfigurationException, SAXException {
        this.repository = repository;

        this.requiredBundles = new HashSet<>();
        this.requiredFeatures = new HashSet<>();
        this.providedBundles = new HashSet<>();
        this.providedFeatures = new HashSet<>();

        calculateRequired(includeImports);
        calculateProvided(updateSite, updateSiteType);
    }
    
    /**
     * The full GitHub repository name (including user or organization).
     *
     * @return     The repository name.
     */
    @JsonGetter("name")
    public String getName() {
        return repository.getFullName();
    }

    /**
     * Returns the GitHub repository URL.
     *
     * @return     The the GitHub repository URL.
     */
    @JsonGetter("githubUrl")
    public String getGithubURL() {
        return repository.getHtmlUrl().toString();
    }

    /**
     * Returns the update site URL if one was found using heuristics.
     *
     * @return     The update site or null if none was found.
     */
    @JsonGetter("updatesiteUrl")
    public String getUpdateSite() {
        return updateSite;
    }
    
    @JsonGetter("requiredBundles")
    public Set<String> getRequiredBundles() {
        return requiredBundles;
    }

    @JsonGetter("requiredFeatures")
    public Set<String> getRequiredFeatures() {
        return requiredFeatures;
    }

    @JsonGetter("providedBundles")
    public Set<String> getProvidedBundles() {
        return providedBundles;
    }

    @JsonGetter("providedFeatures")
    public Set<String> getProvidedFeatures() {
        return providedFeatures;
    }
    
    @Override 
    public String toString() {
        return getName();
    }

    private void calculateRequired(boolean includeImports) throws IOException, ParserConfigurationException, SAXException {
        // get required bundles from all bundle Manifest.MF
        ManifestMFDependencyHandler manifestMfHandler = new ManifestMFDependencyHandler(repository);
        requiredBundles.addAll(manifestMfHandler.getRequiredBundles());

        FeatureXMLHandler featureXMLHandler = new FeatureXMLHandler(repository, includeImports);
        requiredBundles.addAll(featureXMLHandler.getRequiredBundles());
        requiredFeatures.addAll(featureXMLHandler.getRequiredFeatures());
    }

    private void calculateProvided(String updateSite, UpdateSiteTypes updateSiteType) throws IOException {
        try (P2RepositoryReader repoReader = new P2RepositoryReader()) {
            String maybeUpdateSiteUrl = updateSite + repository.getName().toLowerCase() + "/" + updateSiteType.toString() + "/";
            providedBundles.addAll(repoReader.readProvidedBundles(maybeUpdateSiteUrl));
            providedFeatures.addAll(repoReader.readProvidedFeatures(maybeUpdateSiteUrl));
            
            if (providedBundles.isEmpty() && providedFeatures.isEmpty()) {
                LOGGER.warning("No update site or provided bundles and features found for "
                               + getName()
                               + " provided bundles and features cannot be determined");
            } else {
                this.updateSite = maybeUpdateSiteUrl;
            }
        }
    }
}
