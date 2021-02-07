package org.palladiosimulator.dependencytool;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.kohsuke.github.GHContent;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;

/**
 * An object holding all relevant information about the according repository.
 */
@JsonPropertyOrder({"name", "adress", "updatesite", "dependencies"})
public class RepositoryObject {

    private final String repositoryName;
    private final String repositoryAdress;
    private final String updateSite;
    private Set<String> requiredBundles;
    private Set<String> requiredFeatures;
    private Set<RepositoryObject> dependencies;
    private final GitHubAPIHandler handler;
    
    @JsonGetter("name")
    public String getRepositoryName() {
        return repositoryName;
    }

    @JsonGetter("adress")
    public String getRepositoryAdress() {
        return repositoryAdress;
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
     * @param repositoryName The name of the repository.
     * @param handler A GitHubAPIHandler to gain access to the information of the existing repository.
     * @param includeImports If true the dependencies of this repository will also contain imported plugins and features.
     * @param includeOptionals If true the dependencies of this repository will also contain optional plugins.
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public RepositoryObject(String repositoryName, GitHubAPIHandler handler, boolean includeImports, boolean includeOptionals) throws IOException, ParserConfigurationException, SAXException {
        this.repositoryName = repositoryName;
        this.updateSite = PalladioConstants.UPDATESITE + repositoryName.toLowerCase() + "/";
        this.handler = handler;
        repositoryAdress = handler.getRepoPath(repositoryName);
        requiredBundles = new HashSet<>();
        requiredFeatures = new HashSet<>();
        dependencies = new HashSet<>();
        calculateRequired(includeImports, includeOptionals);
    }
    
    @Override 
    public String toString() {
        return repositoryName;
    }

    /**
     * Adds dependencies to the repository.
     * 
     * @param dependencies A set of RepositoryObject to add to this repository as dependencies.
     */
    public void setDependencies(Set<RepositoryObject> dependencies) {
        this.dependencies.addAll(dependencies);
    }
    
    private void calculateRequired(Boolean includeImports, boolean includeOptionals) throws IOException, ParserConfigurationException, SAXException {
        // get required bundles from all bundle Manifest.MF
        ManifestHandler mfHandler = new ManifestHandler(repositoryName, handler.getBundles(repositoryName));
        requiredBundles.addAll(mfHandler.getDependencies(includeOptionals));
        
        // get required bundles and features from all Feature.xml
        Set<String> featureXMLs = new HashSet<>();
        for (String feature : handler.getFeatures(repositoryName)) {
            featureXMLs.add("/features/" + feature + "/feature.xml");
        }
        for (String featureXML : featureXMLs) {
            Optional<GHContent> featureContent = handler.getContentfromFile(repositoryName, featureXML);
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
}
