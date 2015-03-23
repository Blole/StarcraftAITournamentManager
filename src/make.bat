@echo off
call clean.bat
@echo on

javac -cp ".;lib/*" client/ClientMain.java
javac -cp ".;lib/*" server/ServerMain.java

@call:pack client
@call:pack server

@goto:eof



:pack
@mkdir boot
@cd boot
jar -xf ../lib/one-jar-boot-0.95.jar
jar -cfm ../../%~1/%~1.jar boot-manifest.mf com doc
@cd ..
@rmdir /S /Q boot

@mkdir main
jar -cfm main/main.jar config/%~1_manifest.mf server client objects utility
jar -uf ../%~1/%~1.jar main/main.jar lib/snakeyaml-1.10-android.jar
@rmdir /S /Q main

@goto:eof
