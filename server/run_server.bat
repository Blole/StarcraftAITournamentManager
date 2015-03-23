@echo off
setlocal enableextensions

cd /D "%~dp0"
start javaw -jar server.jar settings.yaml
exit
