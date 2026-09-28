@echo off
echo Compiling CraftArena MOBA Java codebase...
if not exist bin mkdir bin
javac -encoding UTF-8 -d bin -sourcepath src src\moba\*.java src\moba\gfx\*.java src\moba\model\*.java src\moba\ui\*.java src\moba\audio\*.java src\moba\world\*.java src\moba\test\*.java
if %ERRORLEVEL% EQU 0 (
    echo Compilation successful!
) else (
    echo Compilation failed!
)
