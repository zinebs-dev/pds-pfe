"""
Script de test pour vérifier que tous les endpoints de l'API fonctionnent
"""

import requests
import json
import sys

BASE_URL = "http://localhost:5000"

def test_health():
    """Test de l'endpoint /api/health"""
    print("\n" + "="*60)
    print("TEST 1: /api/health")
    print("="*60)
    
    try:
        response = requests.get(f"{BASE_URL}/api/health", timeout=5)
        print(f"Status: {response.status_code}")
        print(f"Response: {response.json()}")
        
        if response.status_code == 200:
            data = response.json()
            if data.get('status') == 'healthy':
                print("✅ API est en bonne santé")
                return True
            else:
                print("❌ API ne répond pas correctement")
                return False
        else:
            print(f"❌ Status code inattendu: {response.status_code}")
            return False
            
    except requests.exceptions.ConnectionError:
        print("❌ Impossible de se connecter à l'API")
        print("   Vérifiez que l'API Python est démarrée sur http://localhost:5000")
        return False
    except Exception as e:
        print(f"❌ Erreur: {e}")
        return False

def test_system_status():
    """Test de l'endpoint /api/system/status"""
    print("\n" + "="*60)
    print("TEST 2: /api/system/status")
    print("="*60)
    
    try:
        response = requests.get(f"{BASE_URL}/api/system/status", timeout=10)
        print(f"Status: {response.status_code}")
        
        if response.status_code == 200:
            data = response.json()
            print(json.dumps(data, indent=2))
            
            # Vérifications
            if data.get('elasticsearch_connected'):
                print("✅ Elasticsearch connecté")
            else:
                print("❌ Elasticsearch non connecté")
                return False
            
            if data.get('orchestrator_initialized'):
                print("✅ Orchestrator initialisé")
            else:
                print("❌ Orchestrator non initialisé")
                return False
            
            total_docs = data.get('total_documents', 0)
            print(f"📊 Total documents: {total_docs}")
            
            if total_docs == 0:
                print("⚠️  ATTENTION: Aucun document dans l'index!")
                print("   Le système ne pourra pas faire de comparaisons.")
                return False
            
            return True
        else:
            print(f"❌ Status code: {response.status_code}")
            print(f"Response: {response.text}")
            return False
            
    except Exception as e:
        print(f"❌ Erreur: {e}")
        return False

def test_history_endpoint():
    """Test de l'endpoint /api/history/<user_id>"""
    print("\n" + "="*60)
    print("TEST 3: /api/history/<user_id>")
    print("="*60)
    
    try:
        test_user_id = "test_user"
        response = requests.get(f"{BASE_URL}/api/history/{test_user_id}", timeout=5)
        print(f"Status: {response.status_code}")
        
        if response.status_code == 200:
            data = response.json()
            print(f"✅ Historique récupéré")
            print(f"   User ID: {data.get('user_id')}")
            print(f"   Total résultats: {data.get('total', 0)}")
            return True
        elif response.status_code == 503:
            print("❌ Service non disponible (503)")
            print("   Elasticsearch n'est probablement pas accessible")
            return False
        else:
            print(f"⚠️  Status code: {response.status_code}")
            return True  # Pas critique si l'utilisateur n'existe pas
            
    except Exception as e:
        print(f"❌ Erreur: {e}")
        return False

def test_extract_analyze_validation():
    """Test de validation de l'endpoint /api/extract-and-analyze"""
    print("\n" + "="*60)
    print("TEST 4: /api/extract-and-analyze (validation)")
    print("="*60)
    
    try:
        # Test sans fichier
        response = requests.post(f"{BASE_URL}/api/extract-and-analyze", timeout=5)
        print(f"Status (sans fichier): {response.status_code}")
        
        if response.status_code == 400:
            print("✅ Validation correcte - fichier requis")
            return True
        else:
            print(f"⚠️  Status code inattendu: {response.status_code}")
            return False
            
    except Exception as e:
        print(f"❌ Erreur: {e}")
        return False

def check_api_running():
    """Vérifier si l'API est en cours d'exécution"""
    try:
        response = requests.get(f"{BASE_URL}/api/health", timeout=2)
        return response.status_code == 200
    except:
        return False

def main():
    """Exécuter tous les tests"""
    print("\n" + "🧪 TESTS DES ENDPOINTS DE L'API" + "\n")
    
    # Vérifier que l'API est démarrée
    if not check_api_running():
        print("❌ L'API n'est pas accessible sur http://localhost:5000")
        print("\nPour démarrer l'API:")
        print("  cd python")
        print("  python start_api.py")
        print("\nOu vérifiez que le port 5000 n'est pas déjà utilisé.")
        sys.exit(1)
    
    results = []
    
    results.append(("Health Check", test_health()))
    results.append(("System Status", test_system_status()))
    results.append(("History Endpoint", test_history_endpoint()))
    results.append(("Extract-Analyze Validation", test_extract_analyze_validation()))
    
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
        print("\n🎉 Tous les tests sont passés!")
        print("\nL'API est prête à recevoir des requêtes.")
    else:
        print(f"\n⚠️  {total - passed} test(s) ont échoué")
        print("\nConsultez le guide de dépannage:")
        print("  DEPANNAGE_ERREUR_503.md")

if __name__ == '__main__':
    main()

