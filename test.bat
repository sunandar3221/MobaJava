@echo off
call build.bat
if %ERRORLEVEL% EQU 0 (
    echo Running automated test suite...
    java -cp bin moba.test.TestRunner
)
