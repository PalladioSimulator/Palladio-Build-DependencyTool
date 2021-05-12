package org.palladiosimulator.dependencytool;

import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.io.fs.FileUtils;

public class EmbeddedNeo4j {
    private enum Relationships implements RelationshipType {
        REPOSITORY("depending on repository"), FEATURE("require the feature"), BUNDLE("require the bundle");

        private final String message;

        Relationships(String message) {
            this.message = message;
        }

        public String getMessage() {
            return this.message;
        }
    }

    private static final Logger LOGGER = Logger.getLogger(EmbeddedNeo4j.class.getName());

    private static void registerShutdownHook(final DatabaseManagementService managementService) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                managementService.shutdown();
            }
        });
    }

    private final Path databaseDirectory;
    private final GraphDatabaseService databaseService;
    private final DatabaseManagementService managementService;

    private final Label repositoryLabel = Label.label("Repository");
    private final Label featureLabel = Label.label("Feature");
    private final Label bundleLabel = Label.label("Bundle");

    private final Map<String, Long> repositoryIds = new HashMap<>();
    private final Map<String, Long> featureIds = new HashMap<>();
    private final Map<String, Long> bundleIds = new HashMap<>();

    public EmbeddedNeo4j() {
        this.databaseDirectory = Path.of(DEFAULT_DATABASE_NAME).toAbsolutePath().normalize();

        try {
            LOGGER.info("Delete Directory: " + this.databaseDirectory);
            FileUtils.deleteDirectory(this.databaseDirectory);
        } catch (final IOException e) {
            LOGGER.warning(e.getMessage());
        }

        this.managementService = new DatabaseManagementServiceBuilder(this.databaseDirectory).build();
        this.databaseService = this.managementService.database(DEFAULT_DATABASE_NAME);
        registerShutdownHook(this.managementService);

    }

    public void commit(Set<RepositoryObject> repositories) {
        for (final RepositoryObject repository : Objects.requireNonNull(repositories,
                "The list of repositories must not be null.")) {

            this.commitFeature(repository);
            this.commitBundle(repository);
            this.commitRepository(repository);

        }
    }

    private void commitBundle(final RepositoryObject repository) {
        for (final String bundle : repository.getRequiredBundles()) {
            try (Transaction tx = this.databaseService.beginTx()) {
                final String repositoryName = repository.getRepositoryName();
                final Node repositoryNode = this.createRepositoryNode(tx, repositoryName);

                final Node bundleNode = this.createBundleNode(tx, bundle);

                this.createRelationship(repositoryNode, bundleNode, Relationships.BUNDLE);

                tx.commit();
                LOGGER.info(repositoryName + " -> " + bundle);
            }
        }
    }

    private void commitFeature(final RepositoryObject repository) {
        for (final String feature : repository.getRequiredFeatures()) {
            try (Transaction tx = this.databaseService.beginTx()) {
                final String repositoryName = repository.getRepositoryName();
                final Node repositoryNode = this.createRepositoryNode(tx, repositoryName);

                final Node featureNode = this.createFeatureNode(tx, feature);

                this.createRelationship(repositoryNode, featureNode, Relationships.FEATURE);

                tx.commit();
                LOGGER.info(repositoryName + " -> " + feature);
            }
        }
    }

    private void commitRepository(final RepositoryObject repository) {
        for (final RepositoryObject dependency : repository.getDependency()) {
            try (Transaction tx = this.databaseService.beginTx()) {
                final String repositoryName = repository.getRepositoryName();
                final Node repositoryNode = this.createRepositoryNode(tx, repositoryName);

                final String dependencyName = dependency.getRepositoryName();
                final Node dependencyNode = this.createRepositoryNode(tx, dependencyName);

                this.createRelationship(repositoryNode, dependencyNode, Relationships.REPOSITORY);

                tx.commit();
                LOGGER.info(repositoryName + " -> " + dependencyName);
            }
        }
    }

    private Node createBundleNode(Transaction tx, String bundle) {
        if (this.bundleIds.containsKey(bundle)) {
            return tx.getNodeById(this.bundleIds.get(bundle));
        }

        final Node node = tx.createNode();
        node.addLabel(this.bundleLabel);
        node.setProperty("name", bundle);
        this.repositoryIds.put(bundle, node.getId());
        return node;
    }

    private Node createFeatureNode(Transaction tx, String feature) {
        if (this.featureIds.containsKey(feature)) {
            return tx.getNodeById(this.featureIds.get(feature));
        }

        final Node node = tx.createNode();
        node.addLabel(this.featureLabel);
        node.setProperty("name", feature);
        this.repositoryIds.put(feature, node.getId());
        return node;
    }

    private void createRelationship(final Node from, final Node to, final Relationships relation) {
        from.createRelationshipTo(to, relation).setProperty("message", relation.getMessage());
    }

    private Node createRepositoryNode(Transaction tx, final String name) {
        if (this.repositoryIds.containsKey(name)) {
            return tx.getNodeById(this.repositoryIds.get(name));
        }

        final Node node = tx.createNode();
        node.addLabel(this.repositoryLabel);
        node.setProperty("name", name);
        this.repositoryIds.put(name, node.getId());
        return node;
    }

    public void shutDown() {
        LOGGER.info("Shut down database");
        this.managementService.shutdown();
    }
}
