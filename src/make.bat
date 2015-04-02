@call clean.bat

javac -cp ".;lib/*" client/ClientMain.java
javac -cp ".;lib/*" server/ServerMain.java
javac -cp ".;lib/*" generate/GenerateMain.java

@call:pack ../client/client.jar   client.ClientMain
@call:pack ../server/server.jar   server.ServerMain
@call:pack ../server/generate.jar generate.GenerateMain

@goto:eof



:pack
@mkdir boot
@cd boot
jar -xf ../lib/one-jar-boot-0.95.jar
jar -cfm ../%~1 boot-manifest.mf com doc
@cd ..
@rmdir /S /Q boot

@mkdir main
echo Main-Class: %~2> manifest.mf
jar -cfm main/main.jar manifest.mf client server common generate
jar -uf %~1 main/main.jar lib/snakeyaml-1.10-android.jar lib/commons-io-2.4.jar lib/commons-lang3-3.3.2.jar
@del manifest.mf
@rmdir /S /Q main

@goto:eof
