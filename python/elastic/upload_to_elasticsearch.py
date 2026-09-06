"""
Script d'upload Bulk vers Elasticsearch 7.17.0 pour les extractions ENSAH (extraction_pfe_lot.json).

Usage :
  1️ Installer le client compatible :
        pip install elasticsearch==7.17.0
  2️ Définir les variables d'environnement :
        ES_HOST (ex: http://localhost:9200)
        ES_USER et ES_PASS si ton cluster a une authentification
  3️ Vérifier les chemins :
        - mapping_documents.json dans le même dossier
        - vers_elastic/extraction_pfe_lot.json avec tes données
  4️ Lancer :
        python upload_to_elasticsearch.py
"""

import os
import json
import hashlib
from elasticsearch import Elasticsearch, helpers
from datetime import datetime

# === CONFIGURATION ===
INDEX_NAME = "documents"
JSON_PATH = os.path.join("C:\\Users\\Dell\\Project Java\\vers_elastic", "extraction_pfe_lot.json")
MAPPING_FILE = "documents.json"

# === LECTURE DES VARIABLES D’ENVIRONNEMENT ===
ES_HOST = os.environ.get("ES_HOST", "http://localhost:9200")
ES_USER = os.environ.get("ES_USER")
ES_PASS = os.environ.get("ES_PASS")


# === CONNEXION À ELASTICSEARCH ===
def connect_es():
    if ES_USER and ES_PASS:
        es = Elasticsearch([ES_HOST], http_auth=(ES_USER, ES_PASS), verify_certs=False)
    else:
        es = Elasticsearch([ES_HOST], verify_certs=False)
    if not es.ping():
        raise RuntimeError(f"Connexion à Elasticsearch impossible : {ES_HOST}")
    print(f"Connecté à Elasticsearch : {ES_HOST}")
    return es


# === LIRE LE MAPPING ===
def load_mapping():
    if not os.path.exists(MAPPING_FILE):
        raise FileNotFoundError(f"Mapping absent : {MAPPING_FILE}")
    with open(MAPPING_FILE, "r", encoding="utf-8") as f:
        return json.load(f)


# === CRÉER L’INDEX SI NÉCESSAIRE ===
def ensure_index(es, index_name, mapping):
    if es.indices.exists(index=index_name):
        print(f"L'index '{index_name}' existe déjà.")
    else:
        es.indices.create(index=index_name, body=mapping)
        print(f"Index '{index_name}' créé avec le mapping fourni.")


# === GÉNÉRER UN ID STABLE ===
def stable_id(item):
    """Génère un ID stable à partir de pdf_url ou titre+extraction_date."""
    pdf = item.get("pdf_url") or ""
    key = pdf if pdf else (item.get("titre", "") + (item.get("extraction_date", "") or ""))
    return hashlib.md5(key.encode("utf-8")).hexdigest()


# === TRANSFORMER UN DOCUMENT AVANT INDEXATION ===
def transform_item(item):
    """Normalise la structure pour l'indexation (chapters dict -> list)."""
    doc = {
        "titre": item.get("titre", ""),
        "pdf_url": item.get("pdf_url", ""),
        "author": item.get("author", ""),
        "specialty": item.get("specialty", ""),
        "abstract": item.get("abstract", ""),
        "table_of_contents": item.get("table_of_contents", {}),
        "general_introduction": item.get("general_introduction", ""),
        "general_conclusion": item.get("general_conclusion", "")
    }

    # chapters : dict -> liste
    raw_chapters = item.get("chapters") or {}
    chapters_list = []
    if isinstance(raw_chapters, dict):
        for k, v in raw_chapters.items():
            chapters_list.append({"titre": k, "contenu": v})
    elif isinstance(raw_chapters, list):
        chapters_list = raw_chapters
    doc["chapters"] = chapters_list

    # extraction_date
    ed = item.get("extraction_date")
    doc["extraction_date"] = ed or datetime.utcnow().isoformat()

    # longueur source
    doc["_source_len"] = len(json.dumps(doc, ensure_ascii=False))
    return doc


# === BULK INDEXATION ===
def bulk_index(es, index_name, docs, chunk_size=100):
    actions = []
    for item in docs:
        doc = transform_item(item)
        actions.append({
            "_index": index_name,
            "_id": stable_id(item),
            "_source": doc
        })
    helpers.bulk(es, actions, chunk_size=chunk_size)
    print(f" Bulk terminé : {len(actions)} documents indexés.")


# === MAIN ===
def main():
    if not os.path.exists(JSON_PATH):
        raise FileNotFoundError(f" Fichier JSON introuvable : {JSON_PATH}")

    with open(JSON_PATH, "r", encoding="utf-8") as f:
        data = json.load(f)

    if not isinstance(data, list):
        raise ValueError("Le fichier JSON doit contenir une liste de documents à la racine.")

    print(f" {len(data)} documents lus depuis {JSON_PATH}")

    es = connect_es()
    mapping = load_mapping()
    ensure_index(es, INDEX_NAME, mapping)

    print("Démarrage de l'indexation en bulk...")
    bulk_index(es, INDEX_NAME, data)
    print("Indexation terminée avec succès.")


# === POINT D’ENTRÉE ===
if __name__ == "__main__":
    main()
