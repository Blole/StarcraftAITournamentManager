Starcraft AI Tournament Manager
===============================

A tournament manager software for bots using [BWAPI](https://github.com/bwapi/bwapi) in StarCraft: Brood War.  
This fork uses YAML files for settings and Java RMI for server-client communications.  
Currently supports BWAPI versions: `3.7.4`, `4.0.1-Beta`, `4.1.0-Beta`.

#Building from source
##Java
Building the Java part of the project is simple using Maven:
```
git clone https://github.com/blole/StarcraftAITournamentManager.git
cd StarcraftAITournamentManager
mvn install
```
##C++
However, you will also need to compile the Visual Studio 2013 solution under `tournamentmodule/`, and additionally, to support bots built with different versions of BWAPI, you need have the corresponding versions of Visual Studio installed (2008, 2010 and 2013).
##Output
The built jar's, dll's and stuff will be placed under `build/`:
```
StarcraftAITournamentManager/
└───build/
	├───client/
	│       client.bat
	│       client.yaml
	│       client.jar
	│
	├───server/
	│   │   games.yaml
	│   │   server.bat
	│   │   server.yaml
	│   │   server.jar
	│   └───data/
	│           ...
	│
	└───generate/
			generate.yaml
			generate.jar
```

#Running
- Have two computers (optionally virtual) with StarCraft: Brood War installed.
- Copy the files from `build/client/` to the second computer.
- In client.yaml, configure serverUrl and the starcraft directory dependent properties.
- Run both server.bat and client.bat on the first computer.
- Run client.bat on the second computer.

Instructions for using the original software can be found at https://www.youtube.com/watch?v=tl-nansNbsA and things work mostly the same now.
