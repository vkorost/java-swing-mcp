@echo off
cd /d "%~dp0.."
call gradlew.bat :demo-app:run
