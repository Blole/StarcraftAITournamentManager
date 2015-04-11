#define WIN32_LEAN_AND_MEAN	// Exclude rarely-used stuff from Windows headers
#include <windows.h>
#include <BWAPI.h>
#include "tournamentmodule/tournamentmodule.hpp"


#if (BWAPI_VER == 30704) //3.7.4
	namespace BWAPI { Game* Broodwar; }
	extern "C" __declspec(dllexport) BWAPI::AIModule* newTournamentAI(BWAPI::Game* game)
	{
		BWAPI::Broodwar = game;
		return new ExampleTournamentAI();
	}
#else
	extern "C" __declspec(dllexport) void gameInit(BWAPI::Game* game) { BWAPI::BroodwarPtr = game; }
	extern "C" __declspec(dllexport) BWAPI::AIModule* newTournamentAI()
	{
		return new ExampleTournamentAI();
	}
#endif

extern "C" __declspec(dllexport) BWAPI::TournamentModule* newTournamentModule()
{
	return new ExampleTournamentModule();
}

BOOL APIENTRY DllMain(HANDLE, DWORD, LPVOID)
{
	return TRUE;
}
