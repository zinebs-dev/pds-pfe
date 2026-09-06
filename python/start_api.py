#!/usr/bin/env python3
"""
Script de démarrage de l'API de similarité PFE
Utilise les données existantes d'Elasticsearch
"""
import sys
import os
import logging
import time

# Configuration du logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler('similarity_api.log')
    ]
)

logger = logging.getLogger(__name__)

if __name__ == '__main__':
    try:
        logger.info(" Démarrage de l'API de détection de similarité PFE...")

        # Import et démarrage de l'application Flask
        from api.flask_app import app

        logger.info(" API disponible sur: http://localhost:5000")
        logger.info(" Endpoint santé: http://localhost:5000/api/health")
        logger.info(" Endpoint statut: http://localhost:5000/api/system/status")
        logger.info(" Endpoint similarité: POST http://localhost:5000/api/similarity/check")

        app.run(host='0.0.0.0', port=5000, debug=False)

    except Exception as e:
        logger.error(f" Erreur lors du démarrage: {e}")
        sys.exit(1)