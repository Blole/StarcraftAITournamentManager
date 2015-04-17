@echo off
setlocal enableextensions
cd /d "%~dp0"

for /f %%x in ('"dir /b server*.jar"') do set jarFile=%%x
start javaw -jar %jarFile% server.yaml
