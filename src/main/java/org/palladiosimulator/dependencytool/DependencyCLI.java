package org.palladiosimulator.dependencytool;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Command Line Interface for the dependency tool.
 */
public final class DependencyCLI {

    private static final Logger LOGGER = Logger.getLogger(DependencyCLI.class.getName());

    /**
     * Main method for CLI.
     *
     * @param args organization and authentication-token are required arguments.
     * @throws ParseException
     */
    public static void main(String[] args) throws ParseException {
        final Options options = createOptions();
        final CommandLineParser parser = new DefaultParser();
        final CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (final ParseException e) {
            LOGGER.warning(e.getMessage());
            printHelp(options);
            return;
        }

        final String org = cmd.getOptionValue("o");
        final String token = cmd.getOptionValue("at");
        final boolean includeImports = cmd.hasOption("ii");
        final boolean jsonOutput = cmd.hasOption("json");
        final boolean dependencyOutput = cmd.hasOption("do");
        final boolean neo4jOutput = cmd.hasOption("neo4j");
        UpdateSiteTypes updateSiteType = UpdateSiteTypes.NIGHTLY;
        final List<String> reposToIgnore = new ArrayList<>();

        if (cmd.hasOption("help")) {
            printHelp(options);
        }
        if (cmd.hasOption("ur")) {
            updateSiteType = UpdateSiteTypes.RELEASE;
        }
        if (cmd.hasOption("ri")) {
            reposToIgnore.addAll(Arrays.asList(cmd.getOptionValue("ri").split(",")));
        }
        if (cmd.hasOption("rif")) {
            try (BufferedReader in = new BufferedReader(new FileReader(cmd.getOptionValue("rif")))) {
                String line = null;
                while ((line = in.readLine()) != null) {
                    reposToIgnore.add(line);
                }
            } catch (final IOException e) {
                LOGGER.warning("Something went wrong while opening the file, please make sure the path is correct. "
                        + e.getMessage());
            }
        }

        try {
            final GitHubAPIHandler apiHandler = new GitHubAPIHandler(org, token, reposToIgnore);
            final DependencyCalculator dc = new DependencyCalculator(apiHandler, updateSiteType);
            final Set<RepositoryObject> repositories = dc.calculateDependencies(includeImports);
            final GraphicalRepresentation graphRep = new GraphicalRepresentation(repositories);
            graphRep.createTopologyHierarchy();
            final List<Set<RepositoryObject>> topology = graphRep.getTopologyHierachy();

            createJsonOutput(dependencyOutput, jsonOutput, repositories, topology);
            createNeo4jOutput(neo4jOutput, repositories);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            LOGGER.warning("Please make sure you entered the correct organization and authentication token. "
                    + e.getMessage());
        }
    }

    private static Options createOptions() {
        final Options options = new Options();
        options.addRequiredOption("o", "organization", true, "An existing GitHub organization.")
                .addRequiredOption("at", "authentication-token", true, "Valid authentification token for GitHub API.");

        options.addOption("help", "print-help-message", false, "Print this message")
                .addOption("ur", "use-release", false, "Use release update site instead of nightly.")
                .addOption("ii", "include-imports", false, "Consider feature.xml includes while calculating dependencies.")
                .addOption("json", "json-output", false, "Use more informational json output.")
                .addOption("do", "dependency-output", false, "Use dependencies per repo output")
                .addOption("ri", "repository-ignore", true, "Specify one or more repositories which should be ignored when calculating dependencies. Split by an underscore.")
                .addOption("rif", "repository-ignore-file", true, "Path to file with repositories to ignore. Each repository name must be in a new line.")
                .addOption("neo4j", "create-neo4j-database", false, "Adding the graph representation to a Neo4j graph database.");

        return options;
    }

    private static void createJsonOutput(boolean dependencyOutput, boolean jsonOutput,
            Set<RepositoryObject> repositories, List<Set<RepositoryObject>> topology) throws JsonProcessingException {
        if (dependencyOutput && jsonOutput) {
            final ObjectMapper objectMapper = new ObjectMapper();
            for (final RepositoryObject repo : repositories) {
                System.out.println(objectMapper.writerWithDefaultPrettyPrinter().withView(Views.Dependency.class)
                        .writeValueAsString(repo));
            }
        } else if (dependencyOutput) {
            for (final RepositoryObject repo : repositories) {
                final StringBuilder dependencyString = new StringBuilder();
                dependencyString.append("Name: ").append(repo.getRepositoryName()).append("\n");
                dependencyString.append("Adress: ").append(repo.getRepositoryAdress()).append("\n");
                dependencyString.append("UpdateSite: ").append(repo.getUpdateSite()).append("\n");
                dependencyString.append("Dependencies: ").append(repo.getDependencies()).append("\n");
                System.out.println(dependencyString.toString());
            }
        } else if (jsonOutput) {
            final ObjectMapper objectMapper = new ObjectMapper();
            final DefaultPrettyPrinter pp = new DefaultPrettyPrinter();
            final DefaultPrettyPrinter.Indenter indenter = new DefaultIndenter("  ", DefaultIndenter.SYS_LF);
            pp.indentArraysWith(indenter);
            pp.indentObjectsWith(indenter);
            System.out.println(objectMapper.writer(pp).withView(Views.Topology.class).writeValueAsString(topology));
        } else {
            System.out.println(topology.toString().replaceAll("],", "],\n"));
        }
    }

    private static void createNeo4jOutput(boolean neo4jOutput, Set<RepositoryObject> repositories) {
        if (neo4jOutput) {
            try (EmbeddedNeo4j neo4j = new EmbeddedNeo4j()) {
                neo4j.commit(repositories);
            } catch (final Exception e) {
                LOGGER.warning(e.getMessage());
            }
        }
    }

    private static void printHelp(final Options options) {
        new HelpFormatter().printHelp("java -jar dependencytool.jar <args>", options);
    }

    /**
     * Private constructor to avoid object generation.
     */
    private DependencyCLI() {
        throw new IllegalStateException("Utility-class constructor.");
    }

}
