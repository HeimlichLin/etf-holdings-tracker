@echo off
REM ETF Holdings Tracker - GUI Launcher with Embedded JRE (no console window)
REM No Java installation required!

setlocal

REM Get the directory where this script is located
set "APP_HOME=%~dp0"

REM Use embedded JRE (javaw for no console)
set "JAVAW=%APP_HOME%jre\bin\javaw.exe"

REM Check if embedded JRE exists
if not exist "%JAVAW%" (
    echo Error: Embedded JRE not found at %JAVAW%
    echo Please ensure the application was extracted correctly.
    pause
    exit /b 1
)

REM Set the JAR file
set "APP_JAR=%APP_HOME%etf-holdings-tracker.jar"

REM Check if JAR exists
if not exist "%APP_JAR%" (
    echo Error: Application JAR not found at %APP_JAR%
    pause
    exit /b 1
)

REM Launch the application without console window
start "" "%JAVAW%" ^
    -Xms128m ^
    -Xmx512m ^
    --enable-preview ^
    -jar "%APP_JAR%" ^
    %*

endlocal
