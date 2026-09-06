"""
Script de diagnostic pour identifier les problèmes du système
Exécuter ce script pour vérifier l'état de tous les composants
"""

import sys
import os
sys.path.append(os.path.dirname(__file__))

def check_elasticsearch():
    """Vérifier la connexion à Elasticsearch"""
    print("\n" + "="*60)
    print("1. VÉRIFICATION ELASTICSEARCH")
    print("="*60)
    
    try:
        from elastic.elastic_client import ElasticClient
        
        es_client = ElasticClient()
        
        if es_client.check_connection():
            print("✅ Elasticsearch est accessible")
            
            # Vérifier l'index documents
            try:
                doc_count = es_client.get_document_count()
                print(f"✅ Index 'documents': {doc_count} documents")
                
                if doc_count == 0:
                    print("⚠️  ATTENTION: Aucun document dans l'index!")
                    print("   Le système ne pourra pas faire de comparaisons.")
                    print("   Importez des documents PFE dans Elasticsearch.")
                    return False
            except Exception as e:
                print(f"❌ Erreur lors de la vérification de l'index 'documents': {e}")
                return False
            
            # Vérifier l'index similarity_results
            try:
                if es_client.es.indices.exists(index='similarity_results'):
                    print("✅ Index 'similarity_results' existe")
                else:
                    print("⚠️  Index 'similarity_results' n'existe pas")
                    print("   Exécutez: python elasticsearch/update_similarity_mapping.py")
            except Exception as e:
                print(f"⚠️  Erreur lors de la vérification de 'similarity_results': {e}")
            
            return True
        else:
            print("❌ Impossible de se connecter à Elasticsearch")
            print("   Vérifiez que Elasticsearch est démarré sur http://localhost:9200")
            return False
            
    except ImportError as e:
        print(f"❌ Erreur d'import: {e}")
        print("   Installez les dépendances: pip install -r requirements.txt")
        return False
    except Exception as e:
        print(f"❌ Erreur: {e}")
        return False

def check_python_dependencies():
    """Vérifier les dépendances Python"""
    print("\n" + "="*60)
    print("2. VÉRIFICATION DES DÉPENDANCES PYTHON")
    print("="*60)
    
    required_packages = [
        'flask',
        'flask_cors',
        'elasticsearch',
        'pdfplumber',
        'sentence_transformers',
        'rank_bm25',
        'sklearn',
        'nltk'
    ]
    
    missing = []
    
    for package in required_packages:
        try:
            __import__(package)
            print(f"✅ {package}")
        except ImportError:
            print(f"❌ {package} - MANQUANT")
            missing.append(package)
    
    if missing:
        print(f"\n⚠️  Packages manquants: {', '.join(missing)}")
        print("   Installez-les avec: pip install -r requirements.txt")
        return False
    else:
        print("\n✅ Toutes les dépendances sont installées")
        return True

def check_api_components():
    """Vérifier les composants de l'API"""
    print("\n" + "="*60)
    print("3. VÉRIFICATION DES COMPOSANTS API")
    print("="*60)
    
    try:
        from similarity.similarity_orchestrator import SimilarityOrchestrator
        print("✅ SimilarityOrchestrator importé")
        
        from similarity.section_similarity import SectionSimilarityCalculator
        print("✅ SectionSimilarityCalculator importé")
        
        from extraction.pfe_extractor import PFEExtractor
        print("✅ PFEExtractor importé")
        
        from elastic.elastic_client import ElasticClient
        print("✅ ElasticClient importé")
        
        return True
        
    except ImportError as e:
        print(f"❌ Erreur d'import: {e}")
        return False
    except Exception as e:
        print(f"❌ Erreur: {e}")
        return False

def check_api_initialization():
    """Vérifier l'initialisation complète de l'API"""
    print("\n" + "="*60)
    print("4. TEST D'INITIALISATION COMPLÈTE")
    print("="*60)
    
    try:
        from elastic.elastic_client import ElasticClient
        from similarity.similarity_orchestrator import SimilarityOrchestrator
        from similarity.section_similarity import SectionSimilarityCalculator
        from extraction.pfe_extractor import PFEExtractor
        
        print("Initialisation des composants...")
        
        # ElasticClient
        es_client = ElasticClient()
        if not es_client.check_connection():
            print("❌ Elasticsearch non accessible")
            return False
        print("✅ ElasticClient initialisé")
        
        # PFEExtractor
        extractor = PFEExtractor()
        print("✅ PFEExtractor initialisé")
        
        # SectionSimilarityCalculator
        section_calc = SectionSimilarityCalculator()
        print("✅ SectionSimilarityCalculator initialisé")
        
        # SimilarityOrchestrator
        print("Initialisation de SimilarityOrchestrator (peut prendre du temps)...")
        orchestrator = SimilarityOrchestrator()
        orchestrator.initialize_from_elasticsearch(es_client)
        
        if orchestrator.is_initialized:
            print(f"✅ SimilarityOrchestrator initialisé avec {orchestrator.total_documents} documents")
            return True
        else:
            print("❌ SimilarityOrchestrator non initialisé")
            return False
            
    except Exception as e:
        print(f"❌ Erreur lors de l'initialisation: {e}")
        import traceback
        traceback.print_exc()
        return False

def main():
    """Exécuter tous les diagnostics"""
    print("\n" + "🔍 DIAGNOSTIC DU SYSTÈME DE DÉTECTION DE SIMILARITÉ" + "\n")
    
    results = []
    
    results.append(("Dépendances Python", check_python_dependencies()))
    results.append(("Elasticsearch", check_elasticsearch()))
    results.append(("Composants API", check_api_components()))
    results.append(("Initialisation complète", check_api_initialization()))
    
    # Résumé
    print("\n" + "="*60)
    print("RÉSUMÉ DU DIAGNOSTIC")
    print("="*60)
    
    passed = sum(1 for _, result in results if result)
    total = len(results)
    
    for test_name, result in results:
        status = "✅ OK" if result else "❌ ÉCHEC"
        print(f"{status} - {test_name}")
    
    print("="*60)
    print(f"Résultat: {passed}/{total} vérifications réussies")
    print("="*60)
    
    if passed == total:
        print("\n🎉 Le système est prêt à fonctionner!")
        print("\nPour démarrer l'API:")
        print("  python python/start_api.py")
    else:
        print(f"\n⚠️  {total - passed} problème(s) détecté(s)")
        print("\nConsultez les messages ci-dessus pour résoudre les problèmes.")

if __name__ == '__main__':
    main()

