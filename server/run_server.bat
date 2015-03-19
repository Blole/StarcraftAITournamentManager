@echo off
setlocal enableextensions

cd /D "%~dp0"
start javaw -jar server.jar server_settings.ini
exit
