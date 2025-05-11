@echo off
REM Ambil nama branch aktif
FOR /F "tokens=*" %%i IN ('git rev-parse --abbrev-ref HEAD') DO set BRANCH=%%i

echo Pushing ke origin (%BRANCH%)...
git push origin %BRANCH%

echo Pushing ke org (%BRANCH%)...
git push org %BRANCH%

echo Selesai push ke origin dan org.
