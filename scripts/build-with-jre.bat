@echo off
REM Build ETF Holdings Tracker with Embedded JRE
REM This script creates a distribution package that includes Java runtime
REM End users don't need to install Java!

setlocal

echo ============================================
echo  ETF Holdings Tracker - Build with JRE
echo ============================================
echo.

REM Get script directory
set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%.."

cd /d "%PROJECT_DIR%"

echo Step 1/3: Building application JAR...
call mvn clean package -DskipTests -B
if errorlevel 1 (
    echo ERROR: Maven build failed!
    pause
    exit /b 1
)
echo.

echo Step 2/3: Downloading and preparing JRE...
echo This may take a few minutes on first run...
call mvn package -Pdist-jre-prepare -DskipTests -B
if errorlevel 1 (
    echo ERROR: JRE preparation failed!
    pause
    exit /b 1
)
echo.

echo Step 3/3: Creating distribution package with JRE...
call mvn package -Pdist-jre -DskipTests -B
if errorlevel 1 (
    echo ERROR: Distribution package creation failed!
    pause
    exit /b 1
)
echo.

echo ============================================
echo  Build Complete!
echo ============================================
echo.
echo Distribution package created at:
echo   target\ETF-Holdings-Tracker-1.0.0-windows-x64.zip
echo.
echo This package includes:
echo   - ETF Holdings Tracker application
echo   - Embedded Java 21 runtime (no installation required)
echo   - Launch scripts (run.bat, ETF-Tracker.cmd)
echo.
echo Users can simply extract the ZIP and run ETF-Tracker.cmd
echo.

pause
endlocal
