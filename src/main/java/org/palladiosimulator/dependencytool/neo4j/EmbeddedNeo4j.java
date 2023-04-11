/*
 * Copyright 2021 Yves R. Kirschner
 */
package org.palladiosimulator.dependencytool.neo4j;

import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

import java.io.IOException;
import java.nio.file.Files;
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
import org.palladiosimulator.dependencytool.github.RepositoryObject;

/**
 * The Class EmbeddedNeo4j. An object that hold a Neo4j resource until it is
 * closed.
 */
public class EmbeddedNeo4j implements AutoCloseable {

    /**
     * The Enum Relationships type is mandatory on all relationships and is used to
     * navigate the graph.
     */
    private enum Relationships implements RelationshipType {

        /** The depending on repository relationships. */
        REPOSITORY("depending on repository"),
        /** The require the feature relationships. */
        FEATURE("require the feature"),
        /** The require the bundle relationships. */
        BUNDLE("require the bundle");

        /** The relationship message. */
        private final String message;

        /**
         * Instantiates a new relationships.
         *
         * @param message the message for the relationship
         */
        Relationships(String message) {
            this.message = message;
        }

        /**
         * Gets the relationship message.
         *
         * @return the message
         */
        public String getMessage() {
            return this.message;
        }
    }

    private static final Logger LOGGER = Logger.getLogger(EmbeddedNeo4j.class.getName());

    /**
     * Registers a new virtual-machine shutdown hook for the database management
     * service.
     *
     * @param managementService the management service to shutdown
     */
    private static void addShutdownHook(final DatabaseManagementService managementService) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                managementService.shutdown();
            }
        });
    }

    /**
     * Checks if a directory is not empty.
     *
     * @param path the path to check
     * @return true, if is not empty directory
     */
    private static boolean isNotEmptyDirectory(Path path) {
        try {
            return (Files.isDirectory(path) && !FileUtils.isDirectoryEmpty(path));
        } catch (final IOException e) {
            LOGGER.warning(e.getMessage());
            return false;
        }
    }

    /** The path for the Neo4j database directory. */
    private final Path databaseDirectory;

    /** The Neo4j database service. */
    private final GraphDatabaseService databaseService;

    /** The Neo4j management service. */
    private final DatabaseManagementService managementService;

    /** The label for repository nodes. */
    private final Label repositoryLabel = Label.label("Repository");

    /** The label for feature nodes. */
    private final Label featureLabel = Label.label("Feature");

    /** The label for bundle nodes. */
    private final Label bundleLabel = Label.label("Bundle");

    /** The mapping from repository names to their node ids. */
    private final Map<String, Long> repositoryIds = new HashMap<>();

    /** The mapping from feature names to their node ids. */
    private final Map<String, Long> featureIds = new HashMap<>();

    /** The mapping from bundle names to their node ids. */
    private final Map<String, Long> bundleIds = new HashMap<>();

    /**
     * Instantiates a new embedded Neo4j database instance.
     */
    public EmbeddedNeo4j() {
        this.databaseDirectory = Path.of(DEFAULT_DATABASE_NAME).toAbsolutePath().normalize();

        if (isNotEmptyDirectory(this.databaseDirectory)) {
            LOGGER.warning("Already found a database. New nodes will be added to it. " + this.databaseDirectory);
        }

        this.managementService = new DatabaseManagementServiceBuilder(this.databaseDirectory).build();
        this.databaseService = this.managementService.database(DEFAULT_DATABASE_NAME);
        addShutdownHook(this.managementService);

    }

    @Override
    public void close() throws Exception {
        if (this.managementService != null) {
            LOGGER.info("Shut down database");
            this.managementService.shutdown();
        }
    }

    /**
     * Commit the given repository object to the Neo4j database instance.
     *
     * @param repositories the repositories to commit
     */
    public void commit(Set<RepositoryObject> repositories) {
        for (final RepositoryObject repository : Objects.requireNonNull(repositories,
                "The list of repositories must not be null.")) {

            this.commitFeature(repository);
            this.commitBundle(repository);
            this.commitRepository(repository);
        }
    }

    /**
     * Commit bundle.
     *
     * @param repository the repository
     */
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

    /**
     * Commit feature.
     *
     * @param repository the repository
     */
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

    /**
     * Commit repository.
     *
     * @param repository the repository
     */
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

    /**
     * Creates the bundle node.
     *
     * @param tx     the tx
     * @param bundle the bundle
     * @return the node
     */
    private Node createBundleNode(Transaction tx, String bundle) {
        if (this.bundleIds.containsKey(bundle)) {
            return tx.getNodeById(this.bundleIds.get(bundle));
        }

        final Node node = tx.createNode();
        node.addLabel(this.bundleLabel);
        node.setProperty("name", bundle);
        this.bundleIds.put(bundle, node.getId());
        return node;
    }

    /**
     * Creates the feature node.
     *
     * @param tx      the tx
     * @param feature the feature
     * @return the node
     */
    private Node createFeatureNode(Transaction tx, String feature) {
        if (this.featureIds.containsKey(feature)) {
            return tx.getNodeById(this.featureIds.get(feature));
        }

        final Node node = tx.createNode();
        node.addLabel(this.featureLabel);
        node.setProperty("name", feature);
        this.featureIds.put(feature, node.getId());
        return node;
    }

    /**
     * Creates the relationship.
     *
     * @param from     the from
     * @param to       the to
     * @param relation the relation
     */
    private void createRelationship(final Node from, final Node to, final Relationships relation) {
        from.createRelationshipTo(to, relation).setProperty("message", relation.getMessage());
    }

    /**
     * Creates the repository node.
     *
     * @param tx         the tx
     * @param repository the repository
     * @return the node
     */
    private Node createRepositoryNode(Transaction tx, final String repository) {
        if (this.repositoryIds.containsKey(repository)) {
            return tx.getNodeById(this.repositoryIds.get(repository));
        }

        final Node node = tx.createNode();
        node.addLabel(this.repositoryLabel);
        node.setProperty("name", repository);
        this.repositoryIds.put(repository, node.getId());
        return node;
    }
}
