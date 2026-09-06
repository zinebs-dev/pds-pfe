import sys
import os

# ==========================================
# CORRECTION DU CHEMIN D'IMPORTATION
# ==========================================
current_dir = os.path.dirname(os.path.abspath(__file__))
parent_dir = os.path.dirname(current_dir)
if parent_dir not in sys.path:
    sys.path.insert(0, parent_dir)

# ==========================================
# IMPORTS
# ==========================================
from flask import Flask, request, jsonify
from flask_cors import CORS
import logging
import time
from datetime import datetime
from concurrent.futures import ThreadPoolExecutor, as_completed

# Tes modules personnels
from similarity.similarity_orchestrator import SimilarityOrchestrator
from similarity.section_similarity import SectionSimilarityCalculator
from elastic.elastic_client import ElasticClient
from extraction.pfe_extractor import PFEExtractor

# Pool de threads pour le traitement parallèle
THREAD_POOL = ThreadPoolExecutor(max_workers=4)

# Configuration Logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

app = Flask(__name__)
CORS(app)  # Important pour la communication avec JavaFX

# --- VARIABLES GLOBALES (SINGLETONS) ---
orchestrator = None
es_client = None
extractor = None
section_calculator = None

# --- FONCTIONS UTILITAIRES ---

def _get_risk_level(score: float) -> str:
    """Determine le niveau de risque base sur le score (0.0  1.0)"""
    if score >= 0.8: return "CRITIQUE"
    elif score >= 0.6: return "Elevé"
    elif score >= 0.4: return "Modéré"
    elif score >= 0.2: return "Faible"
    return "Très faible"

def build_full_text_for_analysis(extracted_data):
    """Combine les sections du PDF en un seul texte"""
    text_parts = []
    if extracted_data.get('abstract'):
        text_parts.append(str(extracted_data['abstract']))
    if extracted_data.get('general_introduction'):
        text_parts.append(str(extracted_data['general_introduction']))
    if extracted_data.get('general_conclusion'):
        text_parts.append(str(extracted_data['general_conclusion']))
    if extracted_data.get('chapters'):
        chapters = extracted_data['chapters']
        if isinstance(chapters, dict):
            for content in chapters.values():
                text_parts.append(str(content))
        elif isinstance(chapters, list):
            for chapter in chapters:
                if isinstance(chapter, dict) and 'contenu' in chapter:
                    text_parts.append(str(chapter['contenu']))
    return " ".join(text_parts)

def aggregate_section_scores(detailed_results):
    """
    Calcule les stats globales par section
    """
    if not detailed_results:
        return {}

    stats = {}

    # Collecter tous les scores par section
    section_data = {
        'abstract': [],
        'introduction': [],
        'conclusion': [],
        'chapters': []
    }

    for doc in detailed_results:
        secs = doc.get('section_similarities', {})

        for section_name in section_data.keys():
            section_info = secs.get(section_name)

            if isinstance(section_info, dict):
                # Format: {"max_score": 54.78, "avg_score": 47.27, "risk_level": "Faible"}
                if 'max_score' in section_info:
                    # max_score est déjà en pourcentage (0-100)
                    section_data[section_name].append(section_info['max_score'])
                elif 'score' in section_info:
                    # score est entre 0-1, convertir en pourcentage
                    section_data[section_name].append(section_info['score'] * 100)

            elif isinstance(section_info, (int, float)):
                # Direct score (déjà en pourcentage ou entre 0-1)
                if section_info <= 1.0:
                    # Score entre 0-1, convertir en pourcentage
                    section_data[section_name].append(section_info * 100)
                else:
                    # Déjà en pourcentage
                    section_data[section_name].append(section_info)

    # Calculer les statistiques par section
    for section_name, scores in section_data.items():
        if scores:  # Si on a des scores pour cette section
            max_score = max(scores)
            avg_score = sum(scores) / len(scores)

            # max_score est déjà en pourcentage (0-100)
            stats[section_name] = {
                'max_score': round(max_score, 2),
                'avg_score': round(avg_score, 2),
                'risk_level': _get_risk_level(max_score / 100.0)  # Convertir 0-100 → 0-1
            }

    return stats

def initialize_system():
    """INITIALISATION UNIQUE DU SYSTEME"""
    global orchestrator, es_client, extractor, section_calculator

    try:
        logger.info("Demarrage de l'initialisation du systeme...")

        if es_client is None:
            es_client = ElasticClient()
        if not es_client.check_connection():
            logger.error("Impossible de se connecter a Elasticsearch")
            return False

        if extractor is None:
            extractor = PFEExtractor()

        if section_calculator is None:
            section_calculator = SectionSimilarityCalculator()

        if orchestrator is None:
            orchestrator = SimilarityOrchestrator()
            orchestrator.initialize_from_elasticsearch(es_client)
            logger.info("Orchestrateur charge en memoire")

        logger.info("SYSTEME OPERATIONNEL !")
        return True

    except Exception as e:
        logger.error(f"Erreur critique a l'initialisation: {e}")
        return False

# --- ROUTES API ---

@app.route('/api/extract-and-analyze', methods=['POST'])
def extract_and_analyze():
    global extractor, orchestrator, es_client, section_calculator

    start_time = time.time()
    logger.info("=== Nouvelle requete d'analyse ===")

    try:
        # 1. Vérification système
        if not orchestrator or not orchestrator.is_ready:
            if not initialize_system():
                return jsonify({'success': False, 'error': 'Serveur en demarrage...'}), 503

        # 2. Validation fichier
        if 'file' not in request.files:
            return jsonify({'success': False, 'error': 'Aucun fichier recu'}), 400

        file = request.files['file']
        user_id = request.form.get('user_id', '').strip()
        logger.info(f"User ID reçu: {user_id}")
        filename = request.form.get('filename', '').strip()
        # Utiliser le nom du fichier original si filename est vide
        if not filename:
            filename = file.filename

        #AJOUT : Validation obligatoire du user_id
        if not user_id:
            logger.error("User ID manquant dans la requête")
            return jsonify({'success': False, 'error': 'user_id est requis'}), 400

        logger.info(f"=== ANALYSE POUR UTILISATEUR ===")
        logger.info(f"User ID: {user_id}")
        logger.info(f"Filename: {filename}")
        logger.info(f"Fichier reçu: {file.filename}")
        logger.info(f"Headers: {dict(request.headers)}")
        logger.info(f"Form data: {dict(request.form)}")

        # 3. Extraction
        pdf_bytes = file.read()
        extracted_data = extractor.extract_from_bytes(pdf_bytes)

        if 'error' in extracted_data:
            return jsonify({'success': False, 'error': extracted_data['error']}), 400

        full_text = build_full_text_for_analysis(extracted_data)

        # 4. Sauvegarde Elastic
        try:
            new_doc_id = es_client.save_document(extracted_data)
        except Exception as e:
            return jsonify({'success': False, 'error': f"Erreur Elastic: {str(e)}"}), 500

        # 5. Mise à jour Mémoire
        try:
            if hasattr(orchestrator, 'add_document_dynamic'):
                orchestrator.add_document_dynamic(new_doc_id, full_text)
        except Exception as e:
            logger.warning(f"Erreur update dynamique: {e}")

        # 6. Recherche Similarité
        results = orchestrator.find_similar_documents(full_text, top_k=10)

        # 7. Enrichissement et Comparaison fine - VERSION OPTIMISÉE
        detailed_results = []
        query_sections = {
            'introduction': extracted_data.get('general_introduction', ''),
            'conclusion': extracted_data.get('general_conclusion', ''),
            'abstract': extracted_data.get('abstract', ''),
            'chapters': extracted_data.get('chapters', {})
        }

        # Filtre ignorer soi-même et score > 15%
        valid_matches = [
            (did, score) for did, score in results['final_results']
            if did != new_doc_id and score > 0.15
        ]

        # === OPTIMISATION: Récupérer tous les documents en batch ===
        doc_ids_to_fetch = [did for did, _ in valid_matches]
        docs_batch = es_client.get_documents_batch(doc_ids_to_fetch)
        logger.info(f"Documents récupérés en batch: {len(docs_batch)}")

        # === OPTIMISATION: Traitement parallèle des comparaisons de sections ===
        def process_document(doc_id, score, doc_data):
            """Fonction pour traiter un document (exécutée en parallèle)"""
            try:
                doc_details = doc_data['details'].copy()
                doc_details['id'] = doc_id
                doc_details['similarity_score'] = float(score) * 100
                doc_details['risk_level'] = _get_risk_level(score)

                # Comparaison par section
                target_sections = doc_data['sections']
                section_scores = section_calculator.compare_documents_by_sections(
                    query_sections, target_sections
                )
                doc_details['section_similarities'] = section_scores
                return doc_details
            except Exception as e:
                logger.warning(f"Erreur traitement doc {doc_id}: {e}")
                return None

        # Soumettre les tâches au pool de threads
        futures = []
        for doc_id, score in valid_matches:
            if doc_id in docs_batch:
                future = THREAD_POOL.submit(
                    process_document, doc_id, score, docs_batch[doc_id]
                )
                futures.append(future)

        # Collecter les résultats
        for future in as_completed(futures):
            result = future.result()
            if result:
                detailed_results.append(result)

        # 8. Calcul Score Global
        if detailed_results:
            overall_similarity = max([d['similarity_score'] for d in detailed_results])
        else:
            overall_similarity = 0.0

        # --- AJOUT CRITIQUE POUR L'AFFICHAGE ---
        aggregated_sections = aggregate_section_scores(detailed_results)
        # ---------------------------------------

        # 9. Historique
        try:
            es_client.save_similarity_result({
                'user_id': user_id,
                'filename': file.filename,
                'source_document_id': new_doc_id,
                'overall_similarity': overall_similarity,
                'section_similarities': aggregated_sections, # On sauvegarde aussi les stats
                'total_matches': len(detailed_results),
                'top_matches': detailed_results,
                'algorithm_used': 'Hybrid'
            })
        except Exception as e:
            logger.error(f"Erreur historique: {e}")

        # 10. Réponse
        exec_time = time.time() - start_time
        logger.info(f"Termine en {exec_time:.2f}s")

        return jsonify({
            'success': True,
            'extraction': {
                'title': extracted_data.get('titre'),
                'author': extracted_data.get('author')
            },
            'analysis': {
                'overall_similarity': round(overall_similarity, 2),
                'total_matches': len(detailed_results),
                'results': detailed_results,
                'section_similarities': aggregated_sections, # C'est CA qui remplit ton tableau Détails
                'processing_time': round(exec_time, 2)
            }
        })

    except Exception as e:
        logger.error(f"ERREUR SERVEUR: {e}", exc_info=True)
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/api/history/<user_id>', methods=['GET'])
def get_user_history(user_id):
    """
    Récupère l'historique d'un utilisateur spécifique
    """
    global es_client
    try:
        logger.info(f"Récupération historique pour user: {user_id}")

        # Vérifier et réinitialiser si nécessaire
        if es_client is None:
            logger.warning("es_client est None, tentative de réinitialisation...")
            if not initialize_system():
                return jsonify({'success': False, 'error': 'Elasticsearch non initialisé'}), 503
        elif not es_client.check_connection():
            logger.warning("Connexion Elasticsearch perdue, tentative de reconnexion...")
            if not initialize_system(force=True):
                return jsonify({'success': False, 'error': 'Impossible de se connecter à Elasticsearch'}), 503

        history = es_client.get_user_history(user_id, limit=100)
        logger.info(f"{len(history)} résultats trouvés pour {user_id}")

        return jsonify({
            'success': True,
            'data': {
                'history': history,
                'total': len(history)
            }
        }), 200

    except Exception as e:
        logger.error(f"Erreur récupération historique: {e}")
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/api/history/result/<result_id>', methods=['GET'])
def get_history_result(result_id):
    """
    Récupère un résultat spécifique de l'historique avec tous ses détails
    """
    global es_client
    try:
        logger.info(f"Récupération du résultat: {result_id}")

        if es_client is None:
            if not initialize_system():
                return jsonify({'success': False, 'error': 'Elasticsearch non initialisé'}), 503

        # Récupérer le document depuis Elasticsearch
        result = es_client.get_similarity_result(result_id)

        if not result:
            return jsonify({'success': False, 'error': 'Résultat non trouvé'}), 404

        return jsonify({
            'success': True,
            'data': result
        }), 200

    except Exception as e:
        logger.error(f"Erreur récupération résultat: {e}")
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/api/history/result/<result_id>', methods=['DELETE'])
def delete_history_result(result_id):
    """
    Marque un résultat de similarité comme supprimé (soft delete)
    """
    global es_client
    try:
        logger.info(f"Requête DELETE pour le résultat: {result_id}")

        if es_client is None:
            if not initialize_system():
                return jsonify({'success': False, 'error': 'Elasticsearch non initialisé'}), 503

        # Utiliser la méthode delete_similarity_result qui fait un soft delete
        success = es_client.delete_similarity_result(result_id)

        if success:
            logger.info(f"✓ Résultat {result_id} marqué comme supprimé")
            return jsonify({
                'success': True,
                'message': 'Élément supprimé avec succès'
            }), 200
        else:
            logger.error(f"✗ Échec de la suppression du résultat {result_id}")
            return jsonify({
                'success': False,
                'error': 'Impossible de supprimer l\'élément'
            }), 500

    except Exception as e:
        logger.error(f"Erreur lors de la suppression: {e}")
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/api/system/status', methods=['GET'])
def system_status():
    return jsonify({
        'status': 'online',
        'initialized': orchestrator.is_ready if orchestrator else False,
        'documents': orchestrator.total_documents if orchestrator else 0
    })

if __name__ == '__main__':
    logger.info("Demarrage Serveur Flask...")
    try:
        initialize_system()
    except Exception as e:
        logger.error(f"Initialisation échouée au démarrage: {e}")
        logger.info("Le système tentera de s'initialiser à la première requête.")
    app.run(host='0.0.0.0', port=5000, debug=False, threaded=True)