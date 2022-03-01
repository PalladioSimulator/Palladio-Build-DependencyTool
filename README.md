# Palladio-Build-DependencyTool
Tool to analyze the dependencies of [Palladio projects](https://www.palladio-simulator.com/) for the [build process](https://build.palladio-simulator.com/).

## Maven
By means of the instruction `mvn clean package`, the tool can be packed into an [uber jar](https://maven.apache.org/plugins/maven-shade-plugin/), including its dependencies. This jar can be found in at `./target/deploy/dependencytool.jar` after successful compilation.

## CLI Options
* Required options: `-o`, `-at`
* Usage: `java -jar dependencytool.jar <args>`
    * `-o`, `--organization <arg>`, An existing GitHub organization.
    * `-at`, `--authentication-token <arg>`, Valid authentification token for GitHub API.
    * `-help`, `--print-help-message`, Print this message.
    * `-do`, `--dependency-output`, Use dependencies per repo output Print this message
    * `-ii`, `--include-imports`, Consider feature.xml includes while calculating dependencies.
    * `-json`, `--json-output`, Use more informational json output.
    * `-neo4j`, `--create-neo4j-database`, Adding the graph representation to a Neo4j graph database.
    * `-ri`, `--repository-ignore <arg>`, Specify one or more repositories which should be ignored when calculating dependencies. Split by one comma.
    * `-rif`, `--repository-ignore-file <arg>`, Path to file with repositories to ignore. Each repository name must be in a new line.
    * `-ur`, `--use-release`, Use release update site instead of nightly.

## Neo4j
By means of the `-neo4j` flag, the detected dependencies are written into a [Neo4j database](https://neo4j.com/). The root directory of this database is relative to the archive in the `./neo4j` folder. To avoid inconsistencies and unexpected side effects, it is recommended to delete this directory before each tool execution. The command [`docker run -d -p7474:7474 -p7687:7687 -v $PWD/neo4j/data:/data -v $PWD/neo4j/logs:/logs neo4j`](https://neo4j.com/developer/docker/) can be used to mount this directory into a running Neo4j database instance. This running instance can be retrieved via [`localhost:7474`](http://localhost:7474/) and can be accessed with the [native user and default password](https://neo4j.com/docs/operations-manual/current/configuration/set-initial-password/).

## Sample Interaction
The `<access-token>` parameter must be replaced by a [personal access token](https://docs.github.com/en/github/authenticating-to-github/creating-a-personal-access-token), since this tool loads the required data via the [GitHub API](https://docs.github.com/en/rest).

### JSON Interaction
```bash
git clone git@github.com:PalladioSimulator/Palladio-Build-DependencyTool.git
cd ./Palladio-Build-DependencyTool/
mvn clean package
cd ./target/deploy/
java -jar ./dependencytool.jar -ii -json -do -o PalladioSimulator -at <access-token> -ri Palladio-Build-UpdateSite
```

### Neo4j Interaction
```bash
git clone git@github.com:PalladioSimulator/Palladio-Build-DependencyTool.git
cd ./Palladio-Build-DependencyTool/
mvn clean package
cd ./target/deploy/
java -jar ./dependencytool.jar -o PalladioSimulator -at <access-token> -ii -neo4j
docker run -d -p7474:7474 -p7687:7687 -v $PWD/neo4j/data:/data -v $PWD/neo4j/logs:/logs neo4j
firefox localhost:7474
```
