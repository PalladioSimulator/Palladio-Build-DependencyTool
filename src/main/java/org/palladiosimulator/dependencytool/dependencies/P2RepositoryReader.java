package org.palladiosimulator.dependencytool.dependencies;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.osgi.util.promise.PromiseFactory;

import aQute.bnd.http.HttpClient;
import aQute.p2.api.Artifact;
import aQute.p2.packed.Unpack200;
import aQute.p2.provider.P2Impl;

/**
 * Parser for P2Repositories.
 */
public class P2RepositoryReader implements Closeable {
    
    private final ExecutorService executor;
    private final PromiseFactory promiseFactory;

    public P2RepositoryReader() {
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        promiseFactory = new PromiseFactory(executor);
    }

    /**
     * Returns all features from the update site.
     *  
     * @param path Path to the repository that should be read.
     * @return A set of names of features provided by this repository.
     * @throws IOException
     */
    public Set<String> readProvidedFeatures(String path) throws IOException {
        URI repoURI = URI.create(path);
        Set<String> features = new HashSet<>();
        try (HttpClient client = new HttpClient()) {
            P2Impl p2 = new P2Impl(new Unpack200(), client, repoURI, promiseFactory);
            Collection<Artifact> artifacts = p2.getFeatures();
            artifacts.forEach(a -> features.add(a.id));
        } catch (Exception e) {
            throw new IOException(e);
        }
        return features;
    }
    
    /**
     * Returns all bundles from the update site.
     * 
     * @param path Path to the repository that should be read.
     * @return A set of names of bundles provided by this repository.
     * @throws IOException
     */
    public Set<String> readProvidedBundles(String path) throws IOException {
        URI repoURI = URI.create(path);
        Set<String> bundles = new HashSet<>();
        try (HttpClient client = new HttpClient()) {
            P2Impl p2 = new P2Impl(new Unpack200(), client, repoURI, promiseFactory);
            Collection<Artifact> artifacts = p2.getBundles();
            artifacts.forEach(a -> bundles.add(a.id));
        } catch (Exception e) {
            throw new IOException(e);
        }
        return bundles;
    }
    
    @Override
    public void close() throws IOException {
        executor.shutdown();
    }

}
