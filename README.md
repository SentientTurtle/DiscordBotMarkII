# Personal discord bot project

Primarily used as a "sandbox" to experiment with features, architecture and techniques that are unsuitable in more serious projects.

This project is open-sourced primarily for educational uses, while full deployment and use is permitted, instructions may be incomplete and no support will be given.  
Users seeking a Discord bot for their server are recommended to use one of the more popular bots or frameworks instead of this project.

# Deployment

Gradle Shadow is used to build Fat/Uber jars, including all required dependencies and native libraries.

The `shadowJar` task in the root gradle will include all sub-project modules.  
The `deployment` gradle project may be used to produce a customized build.

# Configuration

Configuration is done using Java system properties.

| Property                                         | Option                                            | Default value<br/>(Relative to working directory) |
|--------------------------------------------------|---------------------------------------------------|---------------------------------------------------|
| org.slf4j.simpleLogger.logFile                   | Output file for logs                              | ./log.txt                                         |
| net.sentientturtle.discordbot.datafolderpath     | Folder for data files                             | ./data                                            |
| net.sentientturtle.discordbot.resourcefolderpath | Folder for resource files                         | ./resource                                        |
| net.sentientturtle.discordbot.databasefile       | (Database module)<br/>File location for database  | ./data/database.db                                |
| net.sentientturtle.discordbot.nativelibs         | (Database module)<br/>Folder for native libraries | ./nativelibs                                      |

