@echo off
chcp 65001 > nul
echo ========================================
echo    LANCEMENT APPLICATION PFE FRONTEND
echo ========================================
echo.
echo 🚀 Lancement de l'application...
call mvnw.cmd javafx:run
echo.
pause

