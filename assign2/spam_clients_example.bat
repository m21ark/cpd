@echo off
setlocal enabledelayedexpansion

rem run this from the g12 folder

rem Compile all .java files in the src folder and its subfolders
for /r src %%f in (*.java) do (
  set "classpath=.;bin"
  for /f "tokens=2 delims==" %%a in ('findstr /B "classpath=" "%%f" 2^>nul') do set "classpath=!classpath!;%%a"
  javac -d bin -cp "!classpath!" "%%f"
  echo "Compiled %%f"
)

rem Start the server
rem start /b cmd /c "java -cp assign2/bin game.server.GameServer"

rem Start 51 clients
for /l %%i in (1,1,51) do (
  start /b cmd /c "java -cp assign2/bin game.client.Client"
)

echo "All clients started"
