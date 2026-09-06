"""
Script pour mettre à jour le mapping de l'index similarity_results
Exécuter ce script pour appliquer les nouveaux champs
"""

from elasticsearch import Elasticsearch
import json

# Connexion à Elasticsearch
es = Elasticsearch(['http://localhost:9200'])

INDEX_NAME = 'similarity_results'

# Nouveau mapping avec tous les champs
NEW_MAPPING = {
    "properties": {
        "algorithm_used": {
            "type": "keyword"
        },
        "comparison_date": {
            "type": "date"
        },
        "content_similarity": {
            "type": "float"
        },
        "id": {
            "type": "keyword"
        },
        "matched_keywords": {
            "type": "keyword"
        },
        "metadata_similarity": {
            "type": "float"
        },
        "semantic_similarity": {
            "type": "float"
        },
        "similarity_score": {
            "type": "float"
        },
        "source_document_id": {
            "type": "keyword"
        },
        "target_document_id": {
            "type": "keyword"
        },
        "top_matching_terms": {
            "type": "text"
        },
        "user_id": {
            "type": "keyword"
        },
        "filename": {
            "type": "keyword"
        },
        "overall_similarity": {
            "type": "float"
        },
        "section_similarities": {
            "type": "object",
            "enabled": True
        },
        "total_matches": {
            "type": "integer"
        },
        "top_matches": {
            "type": "nested",
            "properties": {
                "doc_id": {
                    "type": "keyword"
                },
                "title": {
                    "type": "text"
                },
                "similarity_score": {
                    "type": "float"
                },
                "risk_level": {
                    "type": "keyword"
                }
            }
        }
    }
}

def update_mapping():
    """Met à jour le mapping de l'index"""
    try:
        # Vérifier si l'index existe
        if es.indices.exists(index=INDEX_NAME):
            print(f"✅ L'index '{INDEX_NAME}' existe")
            
            # Mettre à jour le mapping (ajoute les nouveaux champs sans supprimer les anciens)
            response = es.indices.put_mapping(
                index=INDEX_NAME,
                body=NEW_MAPPING
            )
            
            if response.get('acknowledged'):
                print(f"✅ Mapping mis à jour avec succès pour l'index '{INDEX_NAME}'")
            else:
                print(f"⚠️  Réponse inattendue: {response}")
        else:
            print(f"❌ L'index '{INDEX_NAME}' n'existe pas")
            print(f"📝 Création de l'index avec le nouveau mapping...")
            
            # Créer l'index avec le mapping
            response = es.indices.create(
                index=INDEX_NAME,
                body={"mappings": NEW_MAPPING}
            )
            
            if response.get('acknowledged'):
                print(f"✅ Index '{INDEX_NAME}' créé avec succès")
            else:
                print(f"⚠️  Réponse inattendue: {response}")
        
        # Afficher le mapping actuel
        current_mapping = es.indices.get_mapping(index=INDEX_NAME)
        print(f"\n📋 Mapping actuel de '{INDEX_NAME}':")
        print(json.dumps(current_mapping, indent=2))
        
    except Exception as e:
        print(f"❌ Erreur lors de la mise à jour du mapping: {e}")

if __name__ == '__main__':
    print("🚀 Mise à jour du mapping de l'index similarity_results")
    print("=" * 60)
    update_mapping()
    print("=" * 60)
    print("✅ Terminé!")

