@echo off
echo === Démarrage d'Elasticsearch pour le projet PDS-PFE ===
echo.

rem === Définir le chemin du JDK 17 ===
set "ES_JAVA_HOME=C:\Users\Dell\.jdks\temurin-17.0.17"

rem === Chemin vers ton installation Elasticsearch ===
set "ES_HOME=D:\Logeciel_installed\elasticsearch-7.17.0-windows-x86_64\elasticsearch-7.17.0"

rem === Forcer la JVM à utiliser moins de mémoire ===
set "ES_JAVA_OPTS=-Xms1g -Xmx1g"

rem === Vérifier la version Java utilisée ===
"%ES_JAVA_HOME%\bin\java" -version
echo.

rem === Lancer Elasticsearch ===
call "%ES_HOME%\bin\elasticsearch.bat"

pause
