package client;

import common.Environment;
import common.utils.WindowsCommandTools;

public class ClientCommands
{
	public static void Client_RunProxyScript(Environment env)
	{
		WindowsCommandTools.RunWindowsCommand(env.get("starcraft") + "bwapi-data/AI/run_proxy.bat", false, false);
	}
	
	// makes edits to windows registry so Chaoslauncher knows where StarCraft is installed
	public static void Client_RegisterStarCraft(Environment env)
	{
		Client.log("registering StarCraft");
		
		// 32-bit machine StarCraft settings
		String sc32KeyName =     "HKEY_LOCAL_MACHINE\\SOFTWARE\\Blizzard Entertainment\\Starcraft";
		String sc32UserKeyName = "HKEY_CURRENT_USER\\SOFTWARE\\Blizzard Entertainment\\Starcraft";
		WindowsCommandTools.RegEdit(sc32KeyName,     "InstallPath", "REG_SZ",    env.get("starcraft") + "\\");
		WindowsCommandTools.RegEdit(sc32KeyName,     "Program",     "REG_SZ",    env.get("starcraft") + "StarCraft.exe");
		WindowsCommandTools.RegEdit(sc32KeyName,     "GamePath",    "REG_SZ",    env.get("starcraft") + "StarCraft.exe");
		WindowsCommandTools.RegEdit(sc32UserKeyName, "introX",      "REG_DWORD", "00000000");
		
		// 64-bit machine StarCraft settings
		String sc64KeyName =     "HKEY_LOCAL_MACHINE\\SOFTWARE\\Wow6432Node\\Blizzard Entertainment\\Starcraft";
		String sc64UserKeyName = "HKEY_CURRENT_USER\\SOFTWARE\\Wow6432Node\\Blizzard Entertainment\\Starcraft";
		WindowsCommandTools.RegEdit(sc64KeyName, "InstallPath", "REG_SZ", env.get("starcraft") + "\\");
		WindowsCommandTools.RegEdit(sc64KeyName, "Program",     "REG_SZ", env.get("starcraft") + "StarCraft.exe");
		WindowsCommandTools.RegEdit(sc64KeyName, "GamePath",    "REG_SZ", env.get("starcraft") + "StarCraft.exe");
		WindowsCommandTools.RegEdit(sc64UserKeyName, "introX",      "REG_DWORD", "00000000");
		
		// Chaoslauncher Settings
		String clKeyName = "HKEY_CURRENT_USER\\Software\\Chaoslauncher\\Launcher";
		WindowsCommandTools.RegEdit(clKeyName,   "GameVersion",     "REG_SZ",    "Starcraft 1.16.1");
		WindowsCommandTools.RegEdit(clKeyName,   "Width",           "REG_DWORD", "00000640");
		WindowsCommandTools.RegEdit(clKeyName,   "Height",          "REG_DWORD", "00000480");
		WindowsCommandTools.RegEdit(clKeyName,   "StartMinimized",  "REG_SZ",    "0");
		WindowsCommandTools.RegEdit(clKeyName,   "MinimizeOnRun",   "REG_SZ",    "1");
		WindowsCommandTools.RegEdit(clKeyName,   "RunScOnStartup",  "REG_SZ",    "1");
		WindowsCommandTools.RegEdit(clKeyName,   "AutoUpdate",      "REG_SZ",    "0");
		WindowsCommandTools.RegEdit(clKeyName,   "WarnNoAdmin",     "REG_SZ",    "0");
		
		// Chaoslauncher plugin settings
		String clpKeyName = "HKEY_CURRENT_USER\\Software\\Chaoslauncher\\PluginsEnabled";
		WindowsCommandTools.RegEdit(clpKeyName,  "BWAPI Injector (1.16.1) RELEASE", "REG_SZ", "1");
		WindowsCommandTools.RegEdit(clpKeyName,  "W-MODE 1.02",                     "REG_SZ", "1");
		WindowsCommandTools.RegEdit(clpKeyName,  "Chaosplugin for 1.16.1",          "REG_SZ", "0");
	}
	
	public static void Client_KillStarcraftAndChaoslauncher()
	{
		Client.log("killing StarCraft and Chaoslauncher");
		
		while (WindowsCommandTools.IsWindowsProcessRunning("StarCraft.exe"))
		{
			System.out.println("Killing Starcraft...  ");
			WindowsCommandTools.RunWindowsCommand("taskkill /F /IM StarCraft.exe", true, false);
			try { Thread.sleep(100); } catch (InterruptedException e) {}
		}
		
		while (WindowsCommandTools.IsWindowsProcessRunning("Chaoslauncher.exe"))
		{
			System.out.println("Killing Chaoslauncher...  ");
			WindowsCommandTools.RunWindowsCommand("taskkill /F /IM Chaoslauncher.exe", true, false);
			try { Thread.sleep(100); } catch (InterruptedException e) {}
		}
		
		while (WindowsCommandTools.IsWindowsProcessRunning("\"Chaoslauncher - MultiInstance.exe\""))
		{
			System.out.println("Killing Chaoslauncher...  ");
			WindowsCommandTools.RunWindowsCommand("taskkill /F /IM \"Chaoslauncher - MultiInstance.exe\"", true, false);
			try { Thread.sleep(100); } catch (InterruptedException e) {}
		}
	}
	
	public static void Client_StartChaoslauncher(Environment env)
	{
		Client.log("      Client_StartChaoslauncher()");
		
		// Launch Chaoslauncher, do not wait for this to finish, exit if it fails (false, true)
		WindowsCommandTools.RunWindowsExeLocal(env.get("chaoslauncher"), "Chaoslauncher.exe", false, true);
	}
}
