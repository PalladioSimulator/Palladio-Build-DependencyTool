package org.palladiosimulator.dependencytool.github;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.kohsuke.github.GHRepository;
import org.palladiosimulator.dependencytool.dependencies.FeatureXMLHandler;
import org.palladiosimulator.dependencytool.dependencies.ManifestMFDependencyHandler;
import org.palladiosimulator.dependencytool.util.Views;
    
import org.xml.sax.SAXException;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;

/**
 * An object holding all relevant information about the according repository.
 */
@JsonPropertyOrder({"name", "address", "updatesite", "dependencies"})
public class RepositoryObject {

    private final String repositoryName;
    private final String repositoryAddress;
    private final String updateSite;
    private Set<String> requiredBundles;
    private Set<String> requiredFeatures;
    private Set<RepositoryObject> dependencies;
    private GHRepository repository;
    
    @JsonGetter("name")
    public String getRepositoryName() {
        return repositoryName;
    }

    @JsonGetter("address")
    public String getRepositoryAddress() {
        return repositoryAddress;
    }

    @JsonGetter("updatesite")
    public String getUpdateSite() {
        return updateSite;
    }
    
    @JsonIgnore
    public Set<RepositoryObject> getDependency() {
        return dependencies;
    }

    @JsonView(Views.Dependency.class)
    public String getDependencies() {
        return dependencies.toString();
    }
    
    @JsonIgnore
    public Set<String> getRequiredBundles() {
        return requiredBundles;
    }

    @JsonIgnore
    public Set<String> getRequiredFeatures() {
        return requiredFeatures;
    }

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
    public RepositoryObject(GHRepository repository, String updateSite, boolean includeImports) throws IOException, ParserConfigurationException, SAXException {
        this.repository = repository;
        this.repositoryName = repository.getFullName();
        this.updateSite = updateSite + repository.getName().toLowerCase() + "/";
        repositoryAddress = repository.getHtmlUrl().toString();
        requiredBundles = new HashSet<>();
        requiredFeatures = new HashSet<>();
        dependencies = new HashSet<>();

        calculateRequired(includeImports);
    }
    
    @Override 
    public String toString() {
        return repositoryName;
    }

    /**
     * Registers dependencies to this repository.
     * 
     * @param dependencies A set of RepositoryObject to add to this repository as dependencies.
     */
    public void addDependencies(Set<RepositoryObject> dependencies) {
        this.dependencies.addAll(dependencies);
    }

    private void calculateRequired(boolean includeImports) throws IOException, ParserConfigurationException, SAXException {
        // get required bundles from all bundle Manifest.MF
        ManifestMFDependencyHandler manifestMfHandler = new ManifestMFDependencyHandler(repository);
        requiredBundles.addAll(manifestMfHandler.getRequiredBundles());

        FeatureXMLHandler featureXMLHandler = new FeatureXMLHandler(repository, includeImports);
        requiredBundles.addAll(featureXMLHandler.getRequiredBundles());
        requiredFeatures.addAll(featureXMLHandler.getRequiredFeatures());
    }
}
