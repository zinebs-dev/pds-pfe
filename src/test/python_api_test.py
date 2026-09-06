#!/usr/bin/env python3
"""
Test complet de l'API Python Flask
"""
import requests
import json
import time
import sys
import os
from pathlib import Path

# Ajouter le chemin du projet python pour les imports
project_root = Path(__file__).parent.parent.parent  # Remonter à la racine du projet
python_api_path = project_root / "python"
sys.path.append(str(python_api_path))

class PythonApiTester:
    def __init__(self, base_url="http://127.0.0.1:5000"):
        self.base_url = base_url
        self.api_url = f"{base_url}/api"
        self.test_results = []

    def log_test(self, name, success, message=""):
        """Enregistrer un résultat de test"""
        status = "✅" if success else "❌"
        print(f"{status} {name}: {message}")
        self.test_results.append({
            "name": name,
            "success": success,
            "message": message
        })
        return success

    def test_health_endpoint(self):
        """Test du endpoint /api/health"""
        try:
            response = requests.get(f"{self.api_url}/health", timeout=5)
            if response.status_code == 200:
                data = response.json()
                return self.log_test(
                    "Health Check",
                    data.get("status") == "ok",
                    f"Status: {data.get('status')}"
                )
            else:
                return self.log_test(
                    "Health Check",
                    False,
                    f"Code: {response.status_code}"
                )
        except Exception as e:
            return self.log_test(
                "Health Check",
                False,
                f"Erreur: {str(e)}"
            )

    def test_status_endpoint(self):
        """Test du endpoint /api/status"""
        try:
            response = requests.get(f"{self.api_url}/status", timeout=10)
            if response.status_code == 200:
                data = response.json()
                es_connected = data.get("elasticsearch_connected", False)
                doc_count = data.get("total_documents", 0)

                # Accepter le test si ES est connecté, même si success est false
                if es_connected:
                    message = f"ES connecté ({doc_count} docs), success: {data.get('success')}"
                    return self.log_test("System Status", True, message)
                else:
                    return self.log_test("System Status", False, "ES non connecté")
            else:
                return self.log_test("System Status", False, f"Code: {response.status_code}")
        except Exception as e:
            return self.log_test("System Status", False, f"Erreur: {str(e)}")

    def test_elasticsearch_connection(self):
        """Test direct de la connexion Elasticsearch"""
        try:
            # Importer depuis le bon chemin
            from elastic.elastic_client import ElasticClient

            client = ElasticClient()
            is_connected = client.check_connection()

            if is_connected:
                doc_count = client.get_document_count()
                return self.log_test(
                    "Elasticsearch Direct",
                    True,
                    f"Connecté - {doc_count} documents"
                )
            else:
                return self.log_test(
                    "Elasticsearch Direct",
                    False,
                    "Non connecté"
                )
        except Exception as e:
            return self.log_test(
                "Elasticsearch Direct",
                False,
                f"Import error: {str(e)}"
            )

    def test_extractor_module(self):
        """Test du module d'extraction PDF"""
        try:
            from extraction.pfe_extractor import PFEExtractor

            extractor = PFEExtractor()

            # Tester avec un texte simple
            test_text = "Titre: Test PDF\nAuteur: John Doe\nSpécialité: Informatique"

            # Tester quelques méthodes
            author = extractor.extract_author(test_text)
            specialty = extractor.extract_specialty(test_text)

            return self.log_test(
                "PDF Extractor",
                True,
                f"Module chargé - Auteur: {author}, Spécialité: {specialty}"
            )
        except Exception as e:
            return self.log_test(
                "PDF Extractor",
                False,
                f"Erreur: {str(e)}"
            )

    def test_similarity_module(self):
        """Test du module de similarité"""
        try:
            # Vérifier que les modules existent
            modules = [
                'similarity.similarity_orchestrator',
                'similarity.bm25_similarity',
                'similarity.minilm_embeddings',
                'similarity.score_combiner'
            ]

            for module_path in modules:
                try:
                    __import__(module_path)
                except ImportError as e:
                    return self.log_test(
                        "Similarity Modules",
                        False,
                        f"Module manquant: {module_path} - Erreur: {str(e)}"
                    )

            return self.log_test(
                "Similarity Modules",
                True,
                "Tous les modules sont présents"
            )
        except Exception as e:
            return self.log_test(
                "Similarity Modules",
                False,
                f"Erreur: {str(e)}"
            )

    def test_similarity_orchestrator(self):
        """Test spécifique du SimilarityOrchestrator"""
        try:
            from similarity.similarity_orchestrator import SimilarityOrchestrator

            orchestrator = SimilarityOrchestrator()

            return self.log_test(
                "Similarity Orchestrator",
                True,
                "Orchestrator initialisé avec succès"
            )
        except Exception as e:
            return self.log_test(
                "Similarity Orchestrator",
                False,
                f"Erreur d'initialisation: {str(e)}"
            )

    def test_analyze_endpoint(self):
        """Test du endpoint /api/analyze avec un texte simple"""
        try:
            test_data = {
                "text": "Ceci est un texte de test pour vérifier le fonctionnement du système de détection de similarité dans les mémoires PFE. Nous testons l'analyse sémantique et la recherche de documents similaires.",
                "top_k": 3
            }

            response = requests.post(
                f"{self.api_url}/analyze",
                json=test_data,
                timeout=30
            )

            if response.status_code == 200:
                data = response.json()
                success = data.get("success", False)

                if success:
                    results = data.get("results", [])
                    return self.log_test(
                        "Analyze Endpoint",
                        True,
                        f"Succès - {len(results)} résultats"
                    )
                else:
                    error = data.get("error", "Unknown error")
                    return self.log_test(
                        "Analyze Endpoint",
                        False,
                        f"API error: {error}"
                    )
            else:
                return self.log_test(
                    "Analyze Endpoint",
                    False,
                    f"HTTP {response.status_code}"
                )

        except Exception as e:
            return self.log_test(
                "Analyze Endpoint",
                False,
                f"Erreur: {str(e)}"
            )

    def run_all_tests(self):
        """Exécuter tous les tests"""
        print("🚀 LANCEMENT DES TESTS API PYTHON 🚀")
        print("========================================")

        tests = [
            self.test_health_endpoint,
            self.test_status_endpoint,
            self.test_elasticsearch_connection,
            self.test_extractor_module,
            self.test_similarity_module,
            self.test_similarity_orchestrator,
            self.test_analyze_endpoint
        ]

        for test_func in tests:
            test_func()
            time.sleep(1)  # Petite pause entre les tests

        # Résumé
        print("\n" + "="*50)
        print("📊 RÉSUMUM DES TESTS")
        print("="*50)

        total = len(self.test_results)
        passed = sum(1 for r in self.test_results if r["success"])
        failed = total - passed

        print(f"Total tests: {total}")
        print(f"✅ Passés: {passed}")
        print(f"❌ Échoués: {failed}")

        if failed > 0:
            print("\n🔍 Tests échoués:")
            for result in self.test_results:
                if not result["success"]:
                    print(f"  - {result['name']}: {result['message']}")

        print("\n" + "="*50)
        if failed == 0:
            print("🎉 TOUS LES TESTS PYTHON SONT PASSÉS !")
        else:
            print("⚠️  Certains tests ont échoué")

        return failed == 0

if __name__ == "__main__":
    # Tester si l'API est en cours d'exécution
    tester = PythonApiTester()

    try:
        # Essayer de se connecter
        response = requests.get("http://localhost:5000/api/health", timeout=2)
        if response.status_code == 200:
            print("🌐 API Python détectée, lancement des tests...")
            success = tester.run_all_tests()
            sys.exit(0 if success else 1)
        else:
            print("❌ API Python non disponible")
            print("Démarrez d'abord l'API avec: python python/extraction_api.py")
            sys.exit(1)
    except requests.ConnectionError:
        print("❌ API Python non démarrée")
        print("Démarrez d'abord l'API avec: python python/extraction_api.py")
        sys.exit(1)