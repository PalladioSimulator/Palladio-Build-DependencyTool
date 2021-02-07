package org.palladiosimulator.dependencytool;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Handles parsing for manifest.mf files for all given bundles in the given repository.
 *
 */
public class ManifestHandler {

    private static final Logger LOGGER = Logger.getLogger(ManifestHandler.class.getName());
    
    private final HttpGetReader httpGetReader;
    private final String repoName;
    private final Set<String> bundles;
    
    public ManifestHandler(String repoName, Set<String> bundles) {
        this(repoName, bundles, url -> url.openStream());
    }
    
    public ManifestHandler(String repoName, Set<String> bundles, HttpGetReader httpGetReader) {
        this.repoName = repoName;
        this.bundles = bundles;
        this.httpGetReader = httpGetReader;
    }
    
    /**
     * Parses dependencies for all given bundles.
     * @param includeOptionals True if optional bundles shall be considered.
     * @return A set of all dependencies from manifest files.
     */
    public Set<String> getDependencies(boolean includeOptionals) {
        Set<String> dependencies = new HashSet<>();
        Set<URI> manifestURIs = buildManifestURIs();
        for (URI manifestURI : manifestURIs) {
            Optional<ManifestMF> manifest = Optional.empty();
            try {
                manifest = Optional.of(new ManifestMF(httpGetReader.read(manifestURI.toURL())));
            } catch (IOException e) {
                LOGGER.warning("No Manifest.MF found for " + manifestURI);
            }
            if (manifest.isPresent()) {
                dependencies.addAll(manifest.get().getRequiredBundles(includeOptionals));
            }
        }
        return dependencies;
    }
    
    // Returns a set of URIs to all possible manifest locations.
    private Set<URI> buildManifestURIs() {
        String raw = "https://raw.githubusercontent.com/PalladioSimulator/";
        String manifest = "/META-INF/MANIFEST.MF";
        String master = "/master/bundles/";
        Set<URI> manifestURLS = new HashSet<>();
        for (String bundle : bundles) {
            try {
                manifestURLS.add(new URI(raw + repoName + master + bundle + manifest));
            } catch (URISyntaxException e) {
                LOGGER.warning("Something went wrong while building the manifest URI. Please check if " + repoName + " is a valid repository.");
            }
        }
        return manifestURLS;
    }
}
