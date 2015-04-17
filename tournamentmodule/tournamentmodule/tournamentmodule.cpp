#include "tournamentmodule/tournamentmodule.hpp"
#include "tournamentmodule/Timer.h"
#include "yaml-cpp/yaml.h"
#include <fstream>
#include <numeric>
#include <regex>

using namespace BWAPI;
using namespace std;

string getDllFilename();
string getExeFilename();



class TimeLimit
{
public:
	double ms;
	int maxOverruns;
	int overruns;

	TimeLimit(double ms, int maxOverruns, int overruns = 0)
		: ms(ms)
		, maxOverruns(maxOverruns)
		, overruns(overruns)
	{}
};



Timer gameTimer;
bool timeout = false;
bool draw = false;

int lastMoved = 0;
double cameraMoveTime;
double cameraMoveTimeOnUnitCreate;
double statusFileUpdateInterval;

string statusFilename;

int totalFrameLimit;
vector<TimeLimit> timeLimits;
vector<int> frameTimes(100000,0);

void ExampleTournamentAI::onStart()
{
	string starcraftDir = getExeFilename();
	string dllFilename = getDllFilename();
	string yamlFilename = dllFilename.substr(0, dllFilename.find_last_of('.')) + ".yaml";

	YAML::Node yaml;
	try
	{
		yaml = YAML::LoadFile(yamlFilename);
	}
	catch (std::exception e)
	{
		Broodwar->printf("error loading tournament module settings from:\n");
		Broodwar->printf("'%s':\n", yamlFilename.c_str());
		Broodwar->printf("%s\n", e.what());
		return;
	}

	Broodwar->setLocalSpeed(yaml["localSpeed"].as<int>(0));
	Broodwar->setFrameSkip(yaml["frameSkip"].as<int>(256));
	Broodwar->setCommandOptimizationLevel(yaml["commandOptimization"].as<int>(1));

	cameraMoveTime = yaml["cameraMoveTime"].as<double>(2.0);
	cameraMoveTimeOnUnitCreate = yaml["cameraMoveTimeOnUnitCreate"].as<double>(6.0);
	statusFileUpdateInterval = yaml["statusFileUpdateInterval"].as<double>(1.0);

	statusFilename  = yaml["statusFilename"].as<string>("gamestatus.yaml");

	totalFrameLimit = yaml["totalFrameLimit"].as<int>(0);
	for each (const pair<double, int>& item in yaml["timeLimits"].as<map<double, int>>(map<double, int>()))
		timeLimits.push_back(TimeLimit(item.first, item.second));

	gameTimer.start();
}


void ExampleTournamentAI::onFrame()
{
	if ((int)frameTimes.size() < Broodwar->getFrameCount() + 10)
		frameTimes.push_back(0);

	moveCamera();
	
	static double lastStatusWriteTime = -numeric_limits<double>::infinity();

	if (lastStatusWriteTime < gameTimer.getElapsedTimeInMilliSec() - statusFileUpdateInterval)
	{
		lastStatusWriteTime = gameTimer.getElapsedTimeInMilliSec();
		writeTo(statusFilename, false);
	}

	if (0 < totalFrameLimit && totalFrameLimit < Broodwar->getFrameCount())
	{
		draw = true;
		Broodwar->sendText("Total frame limit for game (%d frames) reached, leaving", totalFrameLimit);
		Broodwar->leaveGame();
	}
	
	int frame = Broodwar->getFrameCount();

	if (frame < 1)
		return;

	// add the framer times for this frame
	frameTimes[frame] += Broodwar->getLastEventTime();

	// the total time for the last frame
	int timeElapsed = frameTimes[frame-1];

	// check to see if the timer exceeded any frame time limits
	for each (TimeLimit timeLimit in timeLimits)
	{
		if (timeElapsed > timeLimit.ms)
		{
			timeLimit.overruns++;
			if (timeLimit.maxOverruns >= timeLimit.overruns)
			{
				timeout = true;
				Broodwar->sendText("TIMEOUT: ran for over %d ms %d times", timeLimit.ms, timeLimit.overruns);
				Broodwar->leaveGame();
			}
		}
	}
}

void ExampleTournamentAI::onEnd(bool isWinner)
{
	writeTo(statusFilename, true);
}


void ExampleTournamentAI::writeTo(const string& filename, bool end)
{
	std::ofstream outfile(filename.c_str(), std::ios::out);
	if (outfile.is_open())
	{
		int selfScore = Broodwar->self()->getKillScore()
			+ Broodwar->self()->getBuildingScore()
			+ Broodwar->self()->getRazingScore()
			+ Broodwar->self()->gatheredMinerals()
			+ Broodwar->self()->gatheredGas();


		YAML::Emitter out(outfile);
		out.SetIndent(4);
		if (timeout)
		{
			out << YAML::LocalTag("timeout") << Broodwar->self()->getName();
			//out << YAML::BeginMap;
			//out << YAML::Key << "bot" << YAML::Value << YAML::LocalTag("!bot") << Broodwar->self()->getName();
			//out << YAML::EndMap;
		}
		else if (end)
		{
			out << YAML::LocalTag("done");
			out << YAML::BeginMap;
			out << YAML::Key << "common" << YAML::Value;
				out << YAML::BeginMap;
					out << YAML::Key << "time" << YAML::Value << (int)gameTimer.getElapsedTimeInMilliSec() << YAML::Comment("ms");
					out << YAML::Key << "frames" << YAML::Value << Broodwar->getFrameCount();
				out << YAML::EndMap;
			out << YAML::Key << "individual" << YAML::Value;
				out << YAML::BeginMap;
					out << YAML::Key << "won" << Broodwar->self()->isVictorious();
					out << YAML::Key << "time" << std::accumulate(frameTimes.begin(), frameTimes.end(), 0);
					out << YAML::Key << "score" << YAML::Value << selfScore;
				out << YAML::EndMap;
			out << YAML::EndMap;
		}
		else
			out << YAML::LocalTag("running");
	}
	outfile.close();
}





void ExampleTournamentAI::moveCamera()
{
	if (Broodwar->getFrameCount() - lastMoved < cameraMoveTime)
		return;

	for each (unit_t unit in Broodwar->self()->getUnits())
	{
		if (unit->isUnderAttack() || unit->isAttacking())
		{
			Broodwar->setScreenPosition(unit->getPosition() - Position(320, 240));
			lastMoved = Broodwar->getFrameCount();
		}
	}
}

void ExampleTournamentAI::onUnitCreate(unit_t unit)
{
	frameTimes[Broodwar->getFrameCount()] += Broodwar->getLastEventTime();

	if (Broodwar->getFrameCount() - lastMoved < cameraMoveTimeOnUnitCreate)
		return;

	Broodwar->setScreenPosition(unit->getPosition() - Position(320, 240));
	lastMoved = Broodwar->getFrameCount();
}

bool ExampleTournamentModule::onAction(int actionType, void *parameter)
{
	switch ( actionType )
	{
		case Tournament::EnableFlag:
			switch ( *(int*)parameter )
			{
				case Flag::CompleteMapInformation:		return false;
				case Flag::UserInput:					return false;
			}
			return false;
		case Tournament::PauseGame:
	//	case Tournament::RestartGame:
		case Tournament::ResumeGame:
		case Tournament::SetFrameSkip:
		case Tournament::SetGUI:
		case Tournament::SetLocalSpeed:
		case Tournament::SetMap:
		case Tournament::LeaveGame:
	//	case Tournament::ChangeRace:
		case Tournament::SetLatCom:
		case Tournament::SetTextSize:
		case Tournament::SendText:
		case Tournament::Printf:
		case Tournament::SetCommandOptimizationLevel:	return false;
							
		default:										break;
	}

	return true;
}


void ExampleTournamentAI::onSendText(string)				{ frameTimes[Broodwar->getFrameCount()] += Broodwar->getLastEventTime(); }
void ExampleTournamentAI::onReceiveText(player_t, string)	{ frameTimes[Broodwar->getFrameCount()] += Broodwar->getLastEventTime(); }
void ExampleTournamentAI::onPlayerLeft(player_t)			{ frameTimes[Broodwar->getFrameCount()] += Broodwar->getLastEventTime(); }
void ExampleTournamentAI::onPlayerDropped(player_t*)		{ frameTimes[Broodwar->getFrameCount()] += Broodwar->getLastEventTime(); }
void ExampleTournamentAI::onNukeDetect(Position)			{ frameTimes[Broodwar->getFrameCount()] += Broodwar->getLastEventTime(); }
void ExampleTournamentAI::onUnitDiscover(unit_t)			{ frameTimes[Broodwar->getFrameCount()] += Broodwar->getLastEventTime(); }
void ExampleTournamentAI::onUnitEvade(unit_t)				{ frameTimes[Broodwar->getFrameCount()] += Broodwar->getLastEventTime(); }
void ExampleTournamentAI::onUnitShow(unit_t)				{ frameTimes[Broodwar->getFrameCount()] += Broodwar->getLastEventTime(); }
void ExampleTournamentAI::onUnitHide(unit_t)				{ frameTimes[Broodwar->getFrameCount()] += Broodwar->getLastEventTime(); }
void ExampleTournamentAI::onUnitDestroy(unit_t)				{ frameTimes[Broodwar->getFrameCount()] += Broodwar->getLastEventTime(); }
void ExampleTournamentAI::onUnitMorph(unit_t)				{ frameTimes[Broodwar->getFrameCount()] += Broodwar->getLastEventTime(); }
void ExampleTournamentAI::onUnitComplete(unit_t*)			{ frameTimes[Broodwar->getFrameCount()] += Broodwar->getLastEventTime(); }


////////////////////////////////////////////////////////////////

EXTERN_C IMAGE_DOS_HEADER __ImageBase;
string getDllFilename()
{
	char dllFilename[MAX_PATH];
	GetModuleFileNameA((HINSTANCE)&__ImageBase, dllFilename, MAX_PATH);
	return dllFilename;
}

string getExeFilename()
{
	char exeFilename[MAX_PATH];
	GetDllDirectoryA(MAX_PATH, exeFilename);
	return exeFilename;
}