Starcraft AI Tournament Manager
===============================

A tournament manager software for bots using [BWAPI](https://github.com/bwapi/bwapi) in StarCraft: Brood War.  
This fork uses YAML files for settings and Java RMI for server-client communications.  
Currently supports BWAPI versions: `4.1`, `4.0`, `3.7`.

#Building from source
##Java
Building the Java part of the project is simple using Maven:
```
git clone https://github.com/blole/StarcraftAITournamentManager.git
cd StarcraftAITournamentManager
mvn install
```
##C++
You will also need to compile the Visual Studio solution under `tournamentmodule/`.
To support bots compiled against different versions of BWAPI, you need to have both the BWAPI version and the corresponding version of Visual Studio installed.
You also need to define an environment variable with the path to the BWAPI directory (e.g. `BWAPI_ROOT_4_1=C:\Program Files (x86)\BWAPI_4.1.2`).

| BWAPI version | Environment variable | Visual Studio version |
|---------------|----------------------|-----------------------|
| 4.1           | BWAPI_ROOT_4_1       | 2013                  |
| 4.0           | BWAPI_ROOT_4_0       | 2010                  |
| 3.7           | BWAPI_ROOT_3_7       | 2008                  |

___NOTE!___
You don't get any specific error message for missing a tournamentmodule.dll yet.
The client will just never recognize that the match has started, timeout and kill StarCraft.

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
- Have two computers (optionally virtual) with StarCraft: Brood War 1.16.1 and Java 8+ installed.
- Copy the files from `build/client/` to the second computer.
- In client.yaml, configure serverUrl and the starcraft directory dependent properties.
- Run both server.bat and client.bat on the first computer.
- Run client.bat on the second computer.

Instructions for using the original software can be found at https://www.youtube.com/watch?v=tl-nansNbsA and things work mostly the same now.
