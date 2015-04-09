#include "tournamentmodule/410b/tournamentmodule.hpp"
#include "tournamentmodule/Timer.h"
#include "yaml-cpp/yaml.h"
#include <fstream>
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
string resultsFilename;

int totalFrameLimit;
vector<TimeLimit> timeLimits;
vector<int> frameTimes(100000,0);

void ExampleTournamentAI::onStart()
{
	string starcraftDir = getExeFilename();
	string dllFilename = getDllFilename();
	string yamlFilename = regex_replace(dllFilename, regex("\\..*$"), ".yaml");

	Broodwar << dllFilename << endl;
	Broodwar << yamlFilename << endl;
	
	YAML::Node yaml;
	try
	{
		yaml = YAML::LoadFile(yamlFilename);
	}
	catch (std::exception e)
	{
		Broodwar << "error loading tournament module settings from:" << endl;
		Broodwar << "'" << yamlFilename << "':" << endl;
		Broodwar << e.what() << endl;
		return;
	}

	Broodwar->setLocalSpeed(yaml["localSpeed"].as<int>(0));
	Broodwar->setFrameSkip(yaml["frameSkip"].as<int>(256));
	Broodwar->setCommandOptimizationLevel(yaml["commandOptimization"].as<int>(1));

	cameraMoveTime = yaml["cameraMoveTime"].as<double>(2.0);
	cameraMoveTimeOnUnitCreate = yaml["cameraMoveTimeOnUnitCreate"].as<double>(6.0);
	statusFileUpdateInterval = yaml["statusFileUpdateInterval"].as<double>(1.0);

	statusFilename  = yaml["statusFilename"].as<string>("gamestatus.yaml");
	resultsFilename = yaml["resultsFilename"].as<string>("gameresults.yaml");

	totalFrameLimit = yaml["totalFrameLimit"].as<int>(0);
	for each (auto& item in yaml["timeLimits"].as<map<double, int>>(map<double, int>()))
		timeLimits.emplace_back(item.first, item.second);

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
		writeTo(statusFilename);
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
	for (TimeLimit& timeLimit : timeLimits)
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
	writeTo(resultsFilename);
}


void ExampleTournamentAI::writeTo(const string& filename)
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
		if (draw)
		{
			out << YAML::LocalTag("draw");
			out << YAML::BeginMap;
			out << YAML::Key << "bot" << YAML::Value << YAML::LocalTag("!bot") << Broodwar->self()->getName();
			out << YAML::EndMap;
		}
		else if (timeout)
		{
			out << YAML::LocalTag("timeout");
			out << YAML::Flow;
			out << YAML::BeginMap;
			out << YAML::Key << "bot" << YAML::Value << YAML::LocalTag("bot") << Broodwar->self()->getName();
			out << YAML::EndMap;
		}
		else
		{
			if (Broodwar->self()->isVictorious())
				out << YAML::LocalTag("won");
			else if (Broodwar->self()->isDefeated())
				out << YAML::LocalTag("lost");
			else
				out << YAML::LocalTag("ongoing");
			out << YAML::BeginMap;
			out << YAML::Key << "score" << YAML::Value << selfScore;
			out << YAML::Key << "time" << YAML::Value << (int)gameTimer.getElapsedTimeInMilliSec() << YAML::Comment("ms");
			out << YAML::Key << "frames" << YAML::Value << Broodwar->getFrameCount();
			out << YAML::EndMap;
		}

		outfile << Broodwar->self()->isVictorious() << std::endl;
	}
	outfile.close();
}





void ExampleTournamentAI::moveCamera()
{
	if (Broodwar->getFrameCount() - lastMoved < cameraMoveTime)
		return;

	for each (Unit unit in Broodwar->self()->getUnits())
	{
		if (unit->isUnderAttack() || unit->isAttacking())
		{
			Broodwar->setScreenPosition(unit->getPosition() - Position(320, 240));
			lastMoved = Broodwar->getFrameCount();
		}
	}
}

void ExampleTournamentAI::onUnitCreate(Unit unit)
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


void ExampleTournamentAI::onSendText(string)			{ frameTimes[Broodwar->getFrameCount()] += Broodwar->getLastEventTime(); }
void ExampleTournamentAI::onReceiveText(Player, string)	{ frameTimes[Broodwar->getFrameCount()] += Broodwar->getLastEventTime(); }
void ExampleTournamentAI::onPlayerLeft(Player)			{ frameTimes[Broodwar->getFrameCount()] += Broodwar->getLastEventTime(); }
void ExampleTournamentAI::onPlayerDropped(Player*)		{ frameTimes[Broodwar->getFrameCount()] += Broodwar->getLastEventTime(); }
void ExampleTournamentAI::onNukeDetect(Position)		{ frameTimes[Broodwar->getFrameCount()] += Broodwar->getLastEventTime(); }
void ExampleTournamentAI::onUnitDiscover(Unit)			{ frameTimes[Broodwar->getFrameCount()] += Broodwar->getLastEventTime(); }
void ExampleTournamentAI::onUnitEvade(Unit)				{ frameTimes[Broodwar->getFrameCount()] += Broodwar->getLastEventTime(); }
void ExampleTournamentAI::onUnitShow(Unit)				{ frameTimes[Broodwar->getFrameCount()] += Broodwar->getLastEventTime(); }
void ExampleTournamentAI::onUnitHide(Unit)				{ frameTimes[Broodwar->getFrameCount()] += Broodwar->getLastEventTime(); }
void ExampleTournamentAI::onUnitDestroy(Unit)			{ frameTimes[Broodwar->getFrameCount()] += Broodwar->getLastEventTime(); }
void ExampleTournamentAI::onUnitMorph(Unit)				{ frameTimes[Broodwar->getFrameCount()] += Broodwar->getLastEventTime(); }
void ExampleTournamentAI::onUnitComplete(Unit*)			{ frameTimes[Broodwar->getFrameCount()] += Broodwar->getLastEventTime(); }


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