#pragma once
#include "tournamentmodule/common.hpp"
#include <BWAPI.h>
#include <windows.h>

#if (BWAPI_VER == 307) //3.7.x
	typedef BWAPI::Player*	player_t;
	typedef BWAPI::Unit*	unit_t;
	#define nullptr	NULL
#else
	typedef BWAPI::Player	player_t;
	typedef BWAPI::Unit		unit_t;
#endif

class ExampleTournamentModule : public BWAPI::TournamentModule
{
  virtual bool onAction(int actionType, void *parameter = nullptr);
};

class ExampleTournamentAI : public BWAPI::AIModule
{
public:
  virtual void onStart();
  virtual void onEnd(bool isWinner);
  virtual void onFrame();
  virtual void onSendText(std::string text);
  virtual void onReceiveText(player_t player, std::string text);
  virtual void onPlayerLeft(player_t player);
  virtual void onNukeDetect(BWAPI::Position target);
  virtual void onUnitDiscover(unit_t unit);
  virtual void onUnitEvade(unit_t unit);
  virtual void onUnitShow(unit_t unit);
  virtual void onUnitHide(unit_t unit);
  virtual void onUnitCreate(unit_t unit);
  virtual void onUnitDestroy(unit_t unit);
  virtual void onUnitMorph(unit_t unit);
  virtual void onUnitComplete(unit_t *unit_t);
  virtual void onPlayerDropped(player_t* player);
  virtual void moveCamera();
  virtual void writeTo(const fs::path& file, bool end);
};
