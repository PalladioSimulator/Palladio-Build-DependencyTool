package org.palladiosimulator.dependencytool.github;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;
import org.palladiosimulator.dependencytool.dependencies.FeatureXML;
import org.palladiosimulator.dependencytool.dependencies.ManifestHandler;
import org.palladiosimulator.dependencytool.util.Views;
import org.w3c.dom.Document;
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

    private static final Logger LOGGER = Logger.getLogger(RepositoryObject.class.getName());

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
        this.repositoryName = repository.getName();
        this.updateSite = updateSite + repositoryName.toLowerCase() + "/";
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

    // Returns a set of strings, containing all names of bundles present for the given repository name.
    private Set<String> getBundles() {
        Set<String> bundles = new HashSet<>();
        try {
            for (GHContent bundle : repository.getDirectoryContent("bundles")) {
                if (bundle.isDirectory()) {
                    bundles.add(bundle.getName());
                }
            }
        } catch (IOException e) {
            LOGGER.warning("No bundles page found for " + repositoryName + ".");
        }
        return bundles;
    }

    // Returns a set of strings, containing all names of features present for the given repository name.
    private Set<String> getFeatures() {
        Set<String> features = new HashSet<>();
        try {
            for (GHContent feature : repository.getDirectoryContent("features")) {
                if (feature.isDirectory()) {
                    features.add(feature.getName());
                }
            }
        } catch (IOException e) {
            LOGGER.warning("No features page found for " + repositoryName + ".");
        }
        return features;
    }

    private void calculateRequired(Boolean includeImports) throws IOException, ParserConfigurationException, SAXException {
        // get required bundles from all bundle Manifest.MF
        ManifestHandler mfHandler = new ManifestHandler(repositoryName, getBundles());
        requiredBundles.addAll(mfHandler.getDependencies());

        // get required bundles and features from all Feature.xml
        Set<String> featureXMLs = new HashSet<>();
        for (String feature : getFeatures()) {
            featureXMLs.add("/features/" + feature + "/feature.xml");
        }
        for (String featureXML : featureXMLs) {
            Optional<GHContent> featureContent = getFileContent(featureXML);
            if (featureContent.isPresent()) {
                Document featureDoc = getDocumentFromStream(featureContent.get().read());
                FeatureXML feature = new FeatureXML(featureDoc, includeImports);
                requiredBundles.addAll(feature.getRequiredBundles());
                requiredFeatures.addAll(feature.getRequiredFeatures());
            }
        }
    }
    
    private Document getDocumentFromStream(InputStream content) throws IOException, ParserConfigurationException, SAXException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document document = dBuilder.parse(content);
        document.getDocumentElement().normalize();
        return document;
    }

    // Fetches file content from a given file in a given repository.
    private Optional<GHContent> getFileContent(String filePath) {
        Optional<GHContent> content = Optional.empty();
        try {
            content = Optional.of(repository.getFileContent(filePath));
        } catch (IOException e) {
            LOGGER.warning("No file found for " + filePath + " in " + repository.getFullName() + ".");
        }
        return content;
    }
}
