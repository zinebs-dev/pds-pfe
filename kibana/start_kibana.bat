@echo off
echo Démarrage de Kibana pour le projet PDS-PFE...

REM === Chemin vers ton installation Kibana ===
set KIBANA_HOME="D:\kibana-7.17.0-windows-x86_64\kibana-7.17.0-windows-x86_64"

REM === Chemin vers ton dossier de config dans le projet ===
set CONFIG_PATH="D:\pds-pfe-v1.1\kibana\config"

REM === Lancement de Kibana avec ta config ===
%KIBANA_HOME%\bin\kibana.bat --config %CONFIG_PATH%\kibana.yml

pause