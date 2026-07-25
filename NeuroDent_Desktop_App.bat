@echo off
title NeuroDent AI - Desktop Application
cls
echo =========================================================
echo             NEURODENT AI - DESKTOP APP LAUNCHER
echo =========================================================
echo Launching NeuroDent AI Desktop App for Windows Laptop...
echo URL: https://cozy-semolina-6fc689.netlify.app/
echo =========================================================

:: Check if Microsoft Edge is available (Default on Windows 10/11)
if exist "%ProgramFiles(x86)%\Microsoft\Edge\Application\msedge.exe" (
    start "" "%ProgramFiles(x86)%\Microsoft\Edge\Application\msedge.exe" --app="https://cozy-semolina-6fc689.netlify.app/" --window-size=1280,850 --title="NeuroDent AI"
    exit
)

if exist "%ProgramFiles%\Microsoft\Edge\Application\msedge.exe" (
    start "" "%ProgramFiles%\Microsoft\Edge\Application\msedge.exe" --app="https://cozy-semolina-6fc689.netlify.app/" --window-size=1280,850 --title="NeuroDent AI"
    exit
)

:: Check if Google Chrome is available
if exist "%ProgramFiles%\Google\Chrome\Application\chrome.exe" (
    start "" "%ProgramFiles%\Google\Chrome\Application\chrome.exe" --app="https://cozy-semolina-6fc689.netlify.app/" --window-size=1280,850 --title="NeuroDent AI"
    exit
)

if exist "%ProgramFiles(x86)%\Google\Chrome\Application\chrome.exe" (
    start "" "%ProgramFiles(x86)%\Google\Chrome\Application\chrome.exe" --app="https://cozy-semolina-6fc689.netlify.app/" --window-size=1280,850 --title="NeuroDent AI"
    exit
)

:: Fallback to default browser
start https://cozy-semolina-6fc689.netlify.app/
exit
