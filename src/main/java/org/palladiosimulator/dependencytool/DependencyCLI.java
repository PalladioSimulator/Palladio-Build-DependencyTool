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
public class DependencyCLI {

    private static final Logger LOGGER = Logger.getLogger(DependencyCLI.class.getName());

    /**
     * Main method for CLI.
     *
     * @param args organization and authentication-token are required arguments.
     * @throws ParseException
     */
    public static void main(String[] args) throws ParseException {

        Options options = createOptions();

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        String org = cmd.getOptionValue("o");
        String token = cmd.getOptionValue("at");
        boolean includeImports = cmd.hasOption("ii");
        boolean jsonOutput = cmd.hasOption("json");
        boolean dependencyOutput = cmd.hasOption("do");
        boolean neo4jOutput = cmd.hasOption("neo4j");
        UpdateSiteTypes updateSiteType = UpdateSiteTypes.NIGHTLY;
        List<String> reposToIgnore = new ArrayList<>();

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
            } catch (IOException e) {
                LOGGER.warning("Something went wrong while opening the file, please make sure the path is correct.");
            }
        }

        try {
            GitHubAPIHandler apiHandler = new GitHubAPIHandler(org, token, reposToIgnore);
            DependencyCalculator dc = new DependencyCalculator(apiHandler, updateSiteType);
            Set<RepositoryObject> repositories = dc.calculateDependencies(includeImports);
            GraphicalRepresentation graphRep = new GraphicalRepresentation(repositories);
            graphRep.createTopologyHierarchy();
            List<Set<RepositoryObject>> topology = graphRep.getTopologyHierachy();

            createOutput(dependencyOutput, jsonOutput, repositories, topology);
            createNeo4jOutput(neo4jOutput, repositories);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            LOGGER.warning("Please make sure you entered the correct organization and authentication token.");
        }
    }

    private static void createNeo4jOutput(boolean neo4jOutput, Set<RepositoryObject> repositories) {
        if (neo4jOutput) {
            try (EmbeddedNeo4j neo4j = new EmbeddedNeo4j()) {
                neo4j.commit(repositories);
            } catch (Exception e) {
                LOGGER.warning(e.getMessage());
            }
        }
    }

    private static Options createOptions() {
        Options options = new Options();
        options.addRequiredOption("o", "organization", true, "An existing GitHub organization.")
        .addRequiredOption("at", "authentication-token", true, "Valid authentification token for GitHub API.");
        options.addOption("ur", "use-release", false, "Use release update site instead of nightly.")
        .addOption("ii", "include-imports", false, "Consider feature.xml includes while calculating dependencies.")
        .addOption("json", "json-output", false, "Use more informational json output.")
        .addOption("do", "dependency-output", false, "Use dependencies per repo output")
        .addOption("ri", "repository-ignore", true, "Specify one or more repositories which should be ignored when calculating dependencies. Split by an underscore.")
        .addOption("rif", "repository-ignore-file", true , "Path to file with repositories to ignore. Each repository name must be in a new line.")
        .addOption("neo4j", "create-neo4j-database", false, "Adding the graph representation to a Neo4j graph database.");
        return options;
    }


    private static void createOutput(boolean dependencyOutput, boolean jsonOutput, Set<RepositoryObject> repositories, List<Set<RepositoryObject>> topology) throws JsonProcessingException {
        if (dependencyOutput && jsonOutput) {
            ObjectMapper objectMapper = new ObjectMapper();
            for (RepositoryObject repo : repositories) {
                System.out.println(objectMapper.writerWithDefaultPrettyPrinter().withView(Views.Dependency.class).writeValueAsString(repo));
            }
        } else if (dependencyOutput) {
            for (RepositoryObject repo : repositories) {
                StringBuilder dependencyString = new StringBuilder();
                dependencyString.append("Name: ").append(repo.getRepositoryName()).append("\n");
                dependencyString.append("Adress: ").append(repo.getRepositoryAdress()).append("\n");
                dependencyString.append("UpdateSite: ").append(repo.getUpdateSite()).append("\n");
                dependencyString.append("Dependencies: ").append(repo.getDependencies()).append("\n");
                System.out.println(dependencyString.toString());
            }
        } else if (jsonOutput) {
            ObjectMapper objectMapper = new ObjectMapper();
            DefaultPrettyPrinter pp = new DefaultPrettyPrinter();
            DefaultPrettyPrinter.Indenter indenter = new DefaultIndenter("  ", DefaultIndenter.SYS_LF);
            pp.indentArraysWith(indenter);
            pp.indentObjectsWith(indenter);
            System.out.println(objectMapper.writer(pp).withView(Views.Topology.class).writeValueAsString(topology));
        } else {
            System.out.println(topology.toString().replaceAll("],", "],\n"));
        }
    }
}
