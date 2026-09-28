@echo off
if not exist bin\moba\Main.class (
    call build.bat
)
echo Launching CraftArena: Block Legends MOBA (60 FPS Hardware-Accelerated Mode)...
java -Dsun.java2d.d3d=true -Dsun.java2d.noddraw=true -Dsun.java2d.transaccel=true -XX:+UseG1GC -cp bin moba.Main
