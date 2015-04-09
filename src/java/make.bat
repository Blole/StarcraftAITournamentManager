javac -cp "scaitm;scaitm/lib/commons-io-2.4.jar;scaitm/lib/commons-lang3-3.3.2.jar;scaitm/lib/snakeyaml-1.10-android.jar" scaitm/client/ClientMain.java     -d scaitm/bin
javac -cp "scaitm;scaitm/lib/commons-io-2.4.jar;scaitm/lib/commons-lang3-3.3.2.jar;scaitm/lib/snakeyaml-1.10-android.jar" scaitm/server/ServerMain.java     -d scaitm/bin
javac -cp "scaitm;scaitm/lib/commons-io-2.4.jar;scaitm/lib/commons-lang3-3.3.2.jar;scaitm/lib/snakeyaml-1.10-android.jar" scaitm/generate/GenerateMain.java -d scaitm/bin

@call:pack ../../client/client.jar   client.ClientMain
@call:pack ../../server/server.jar   server.ServerMain
@call:pack ../../server/generate.jar generate.GenerateMain

@goto:eof



:pack
@mkdir boot
@cd boot
jar -xf ../scaitm/lib/one-jar-boot-0.95.jar
jar -cfm ../%~1 boot-manifest.mf com doc
@cd ..
@rmdir /S /Q boot

@mkdir main
echo Main-Class: %~2> manifest.mf
jar -cfm main/main.jar manifest.mf
jar -uf main/main.jar -C scaitm/bin/ .
jar -uf %~1 main/main.jar -C scaitm/ lib/snakeyaml-1.10-android.jar
jar -uf %~1 main/main.jar -C scaitm/ lib/commons-io-2.4.jar
jar -uf %~1 main/main.jar -C scaitm/ lib/commons-lang3-3.3.2.jar
@del manifest.mf
@rmdir /S /Q main

@goto:eof
