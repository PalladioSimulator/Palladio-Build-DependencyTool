# Palladio-Build-DependencyTool
Tool to analyze the dependencies of Palladio project for the build process.

## Maven
By means of the instruction `mvn clean package`, the tool can be packed into an uber-jar, including its dependencies. This jar can be found in at `.\target\deploy\dependencytool.jar` after successful compilation.

## CLI Options
* Required options: `o`, `at`
* Usage: `java -jar dependencytool.jar <args>`
    * `-o`,`--organization <arg>`               An existing GitHub organization.
    * `-at`,`--authentication-token <arg>`      Valid authentification token for GitHub API.
    * `-help`,`--print-help-message`            Print this message.
    * `-do`,`--dependency-output`               Use dependencies per repo output Print this message
    * `-ii`,`--include-imports`                 Consider feature.xml includes while calculating dependencies.
    * `-json`,`--json-output`                   Use more informational json output.
    * `-neo4j`,`--create-neo4j-database`        Adding the graph representation to a Neo4j graph database.
    * `-ri`,`--repository-ignore <arg>`         Specify one or more repositories which should be ignored when calculating dependencies. Split by an underscore.
    * `-rif`,`--repository-ignore-file <arg>`   Path to file with repositories to ignore. Each repository name must be in a new line.
    * `-ur`,`--use-release`                     Use release update site instead of nightl

## Neo4j
By means of the `-neo4j` flag, the detected dependencies are written into a Neo4j database. The root directory of this database is relative to the archive in the `.\neo4j` folder. To avoid inconsistencies and unexpected side effects, it is recommended to delete this directory before each execution. The command `docker run --detach --publish=7474:7474 --publish=7687:7687 --volume=$PWD/neo4j/data:/data --volume=$PWD/neo4j/logs:/logs neo4j` can be used to mount this directory into a running Neo4j database instance. This instance can be accessed via `localhost:7474`.

## Sample Interaction

> git clone git@github.com:PalladioSimulator/Palladio-Build-DependencyTool.git
> cd .\Palladio-Build-DependencyTool\
> mvn clean package
> cd .\target\deploy\
> java -jar  .\dependencytool.jar -o PalladioSimulator -at \<authentication-token\> -ii -do -neo4j
> docker run --detach --publish=7474:7474 --publish=7687:7687 --volume=\$PWD/neo4j/data:/data --volume=\$PWD/neo4j/logs:/logs neo4j
> firefox localhost:7474
