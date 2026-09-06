from flask import Flask, request, jsonify
from flask_cors import CORS
import tempfile
import sys
import os
import time
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))
from pfe_extractor import PFEExtractor
from similarity.similarity_orchestrator import SimilarityOrchestrator
from elastic.elastic_client import  ElasticClient
import json

app = Flask(__name__)
CORS(app)

# Initialiser les clients
elastic_client = ElasticClient()
extractor = PFEExtractor()

# Initialiser l'orchestrator de similarité (optionnel, selon vos besoins)
orchestrator = None

@app.route('/api/health', methods=['GET'])
def health_check():
    return jsonify({"status": "ok"}), 200

@app.route('/api/extract', methods=['POST'])
def extract_pdf():
    """Endpoint pour extraire un PDF et le stocker dans Elasticsearch"""
    try:
        if 'file' not in request.files:
            return jsonify({'error': 'Aucun fichier fourni'}), 400

        file = request.files['file']

        if file.filename == '':
            return jsonify({'error': 'Nom de fichier vide'}), 400

        if not file.filename.lower().endswith('.pdf'):
            return jsonify({'error': 'Seuls les fichiers PDF sont acceptés'}), 400

        # Lire le PDF
        pdf_bytes = file.read()

        # Extraire les données
        extracted_data = extractor.extract_from_bytes(pdf_bytes)

        if 'error' in extracted_data:
            return jsonify({'error': extracted_data['error']}), 500

        # Ajouter des métadonnées supplémentaires
        extracted_data.update({
            'pdf_filename': file.filename,
            'upload_timestamp': time.time(),
            'file_size': len(pdf_bytes)
        })

        # Stocker dans Elasticsearch
        # TODO: Implémenter la méthode save_to_elasticsearch
        doc_id = elastic_client.save_document(extracted_data)

        return jsonify({
            'success': True,
            'document_id': doc_id,
            'extracted_data': extracted_data
        }), 200

    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/analyze', methods=['POST'])
def analyze_similarity():
    """Analyse la similarité d'un texte"""
    global orchestrator
    try:
        data = request.json
        text = data.get('text', '')

        if not text or len(text) < 50:
            return jsonify({'error': 'Texte trop court'}), 400

        top_k = data.get('top_k', 10)

        # Initialiser l'orchestrator si nécessaire
        if not orchestrator or not orchestrator.is_initialized:
            orchestrator = SimilarityOrchestrator()
            orchestrator.initialize_from_elasticsearch(elastic_client)

        # Trouver les documents similaires
        results = orchestrator.find_similar_documents(text, top_k)

        # Récupérer les détails des documents
        detailed_results = []
        for doc_id, score in results['final_results']:
            doc_details = elastic_client.get_document_details(doc_id)
            if doc_details:
                doc_details['similarity_score'] = score
                detailed_results.append(doc_details)

        return jsonify({
            'success': True,
            'total_matches': len(detailed_results),
            'results': detailed_results,
            'statistics': results.get('statistics', {})
        }), 200

    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/status', methods=['GET'])
def get_status():
    """Statut du système"""
    global orchestrator
    return jsonify({
        'elasticsearch_connected': elastic_client.check_connection(),
        'total_documents': elastic_client.get_document_count(),
        'similarity_system_ready': orchestrator.is_initialized if orchestrator else False
    }), 200

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)