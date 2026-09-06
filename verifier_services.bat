@echo off
echo ========================================
echo Verification des Services
echo ========================================
echo.

echo [1/2] Verification d'Elasticsearch...
curl -s http://localhost:9200 >nul 2>&1
if errorlevel 1 (
    echo ERREUR: Elasticsearch ne repond pas
    echo Demarrez-le avec: cd elasticsearch ^&^& start_elastic.bat
) else (
    echo OK: Elasticsearch est actif sur http://localhost:9200
)
echo.

echo [2/2] Verification de l'API Python...
curl -s http://localhost:5000/api/health >nul 2>&1
if errorlevel 1 (
    echo ERREUR: API Python ne repond pas
    echo Demarrez-la avec: cd python ^&^& python start_api.py
) else (
    echo OK: API Python est active sur http://localhost:5000
)
echo.

echo ========================================
echo Verification terminee
echo ========================================
pause

