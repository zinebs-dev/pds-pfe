"""
Script de test pour vérifier les améliorations du système
"""

import sys
import os
sys.path.append(os.path.dirname(__file__))

from elastic.elastic_client import ElasticClient
from similarity.section_similarity import SectionSimilarityCalculator

def test_elasticsearch_connection():
    """Test de connexion à Elasticsearch"""
    print("\n" + "="*60)
    print("TEST 1: Connexion à Elasticsearch")
    print("="*60)
    
    try:
        es_client = ElasticClient()
        if es_client.check_connection():
            print("✅ Connexion à Elasticsearch réussie")
            
            # Compter les documents
            doc_count = es_client.get_document_count()
            print(f"✅ Nombre de documents dans l'index: {doc_count}")
            
            return True
        else:
            print("❌ Impossible de se connecter à Elasticsearch")
            return False
    except Exception as e:
        print(f"❌ Erreur: {e}")
        return False

def test_similarity_index():
    """Test de l'index similarity_results"""
    print("\n" + "="*60)
    print("TEST 2: Index similarity_results")
    print("="*60)
    
    try:
        es_client = ElasticClient()
        
        # Vérifier si l'index existe
        if es_client.es.indices.exists(index='similarity_results'):
            print("✅ L'index 'similarity_results' existe")
            
            # Récupérer le mapping
            mapping = es_client.es.indices.get_mapping(index='similarity_results')
            properties = mapping['similarity_results']['mappings']['properties']
            
            # Vérifier les nouveaux champs
            required_fields = ['user_id', 'filename', 'overall_similarity', 'section_similarities', 'top_matches']
            missing_fields = []
            
            for field in required_fields:
                if field in properties:
                    print(f"✅ Champ '{field}' présent")
                else:
                    print(f"❌ Champ '{field}' manquant")
                    missing_fields.append(field)
            
            if not missing_fields:
                print("✅ Tous les champs requis sont présents")
                return True
            else:
                print(f"⚠️  Champs manquants: {missing_fields}")
                print("💡 Exécutez: python elasticsearch/update_similarity_mapping.py")
                return False
        else:
            print("❌ L'index 'similarity_results' n'existe pas")
            print("💡 Exécutez: python elasticsearch/update_similarity_mapping.py")
            return False
            
    except Exception as e:
        print(f"❌ Erreur: {e}")
        return False

def test_section_similarity():
    """Test du calculateur de similarité par section"""
    print("\n" + "="*60)
    print("TEST 3: Calculateur de similarité par section")
    print("="*60)
    
    try:
        calculator = SectionSimilarityCalculator()
        print("✅ SectionSimilarityCalculator initialisé")
        
        # Test avec deux textes similaires
        text1 = "L'intelligence artificielle est un domaine de l'informatique qui vise à créer des machines capables de penser."
        text2 = "L'IA est une branche de l'informatique qui cherche à développer des systèmes intelligents."
        
        score = calculator.calculate_section_similarity(text1, text2)
        print(f"✅ Score de similarité calculé: {score:.4f}")
        
        if 0 <= score <= 1:
            print("✅ Score dans la plage valide [0, 1]")
            return True
        else:
            print(f"❌ Score hors de la plage valide: {score}")
            return False
            
    except Exception as e:
        print(f"❌ Erreur: {e}")
        import traceback
        traceback.print_exc()
        return False

def test_save_similarity_result():
    """Test de sauvegarde d'un résultat de similarité"""
    print("\n" + "="*60)
    print("TEST 4: Sauvegarde d'un résultat de similarité")
    print("="*60)
    
    try:
        es_client = ElasticClient()
        
        # Créer un résultat de test
        test_result = {
            'user_id': 'test_user',
            'filename': 'test_rapport.pdf',
            'source_document_id': 'test_doc_123',
            'overall_similarity': 45.5,
            'section_similarities': {
                'introduction': {
                    'max_score': 67.8,
                    'avg_score': 45.2,
                    'risk_level': 'Modéré'
                }
            },
            'total_matches': 3,
            'top_matches': [
                {
                    'doc_id': 'doc1',
                    'title': 'Document 1',
                    'similarity_score': 0.78,
                    'risk_level': 'Élevé'
                }
            ],
            'algorithm_used': 'BM25 + MiniLM (hybrid)'
        }
        
        result_id = es_client.save_similarity_result(test_result)
        print(f"✅ Résultat sauvegardé avec ID: {result_id}")
        
        # Vérifier que le résultat a été sauvegardé
        saved_result = es_client.es.get(index='similarity_results', id=result_id)
        if saved_result['found']:
            print("✅ Résultat récupéré depuis Elasticsearch")
            
            # Nettoyer (supprimer le résultat de test)
            es_client.delete_similarity_result(result_id)
            print("✅ Résultat de test supprimé")
            
            return True
        else:
            print("❌ Résultat non trouvé dans Elasticsearch")
            return False
            
    except Exception as e:
        print(f"❌ Erreur: {e}")
        import traceback
        traceback.print_exc()
        return False

def test_user_history():
    """Test de récupération de l'historique utilisateur"""
    print("\n" + "="*60)
    print("TEST 5: Récupération de l'historique utilisateur")
    print("="*60)
    
    try:
        es_client = ElasticClient()
        
        # Récupérer l'historique d'un utilisateur de test
        history = es_client.get_user_history('test_user', limit=10)
        print(f"✅ Historique récupéré: {len(history)} résultats")
        
        return True
            
    except Exception as e:
        print(f"❌ Erreur: {e}")
        import traceback
        traceback.print_exc()
        return False

def run_all_tests():
    """Exécute tous les tests"""
    print("\n" + "🚀 TESTS DES AMÉLIORATIONS DU SYSTÈME" + "\n")
    
    results = []
    
    results.append(("Connexion Elasticsearch", test_elasticsearch_connection()))
    results.append(("Index similarity_results", test_similarity_index()))
    results.append(("Similarité par section", test_section_similarity()))
    results.append(("Sauvegarde résultat", test_save_similarity_result()))
    results.append(("Historique utilisateur", test_user_history()))
    
    # Résumé
    print("\n" + "="*60)
    print("RÉSUMÉ DES TESTS")
    print("="*60)
    
    passed = sum(1 for _, result in results if result)
    total = len(results)
    
    for test_name, result in results:
        status = "✅ PASS" if result else "❌ FAIL"
        print(f"{status} - {test_name}")
    
    print("="*60)
    print(f"Résultat: {passed}/{total} tests réussis")
    print("="*60)
    
    if passed == total:
        print("\n🎉 Tous les tests sont passés avec succès!")
    else:
        print(f"\n⚠️  {total - passed} test(s) ont échoué")

if __name__ == '__main__':
    run_all_tests()

