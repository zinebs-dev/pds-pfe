# PDS-PFE — Plateforme Intelligente de Détection de Similarité des Rapports de PFE

<div align="center">

![Java](https://img.shields.io/badge/Java-11+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Python](https://img.shields.io/badge/Python-3.8+-3776AB?style=for-the-badge&logo=python&logoColor=white)
![Elasticsearch](https://img.shields.io/badge/Elasticsearch-7.17-005571?style=for-the-badge&logo=elasticsearch&logoColor=white)
![Flask](https://img.shields.io/badge/Flask-2.x-000000?style=for-the-badge&logo=flask&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-11+-FF6347?style=for-the-badge&logo=java&logoColor=white)

**Détection automatique de plagiat académique — lexical & sémantique**

</div>

## Description

**PDS-PFE** est une plateforme logicielle intelligente dédiée à la **détection automatique de similarités** entre les rapports de Projets de Fin d'Études (PFE).

Face à l'augmentation des productions académiques et aux enjeux d'intégrité intellectuelle, cette solution offre une alternative robuste et automatisée aux vérifications manuelles. Elle combine deux approches complémentaires : une analyse **lexicale** (via BM25) pour repérer les copies mot à mot, et une analyse **sémantique** (via MiniLM) pour détecter les reformulations et paraphrases.

---

## Fonctionnalités

| Fonctionnalité | Description |
|---|---|
| Import PDF | Chargement et extraction automatique du contenu des rapports |
| Plagiat lexical | Détection de copies via l'algorithme **BM25** |
| Plagiat sémantique | Détection de paraphrases via le modèle **MiniLM** |
| Score global | Score de similarité avec détail **par section** (résumé, introduction, conclusion) |
| Niveaux de risque | Classification en 4 niveaux : `FAIBLE` · `MODÉRÉ` · `ÉLEVÉ` · `CRITIQUE` |
| Top Documents | Identification des documents les plus similaires dans la base |
| Recommandations | Conseils automatiques selon le niveau de risque détecté |
| Historique | Consultation et suppression des analyses passées par utilisateur |
| Mode sombre | Interface graphique moderne avec thème clair/sombre |
| Monitoring | Tableau de bord en temps réel via **Kibana** |

---

## Architecture technique

La plateforme repose sur une **architecture multi-tiers** organisée en quatre couches communicant via HTTP/REST :

```
┌─────────────────────────────────────────────────────┐
│              FRONTEND JavaFX (port 8080)            │
│         Interface graphique — DashboardController   │
└──────────────────────┬──────────────────────────────┘
                       │ HTTP/REST (JSON)
┌──────────────────────▼──────────────────────────────┐
│            BACKEND JAVA (port 8080)                 │
│  AuthHandler · FileUploadHandler · SimilarityHandler│
│  DocumentHandler · HistoryHandler · HealthHandler   │
└──────────────────────┬──────────────────────────────┘
                       │ HTTP/REST (JSON)
┌──────────────────────▼──────────────────────────────┐
│          API PYTHON / FLASK (port 5000)             │
│  PFEExtractor · TextProcessor · BM25Similarity      │
│  MiniLMSimilarity · ScoreCombiner                   │
└──────────────────────┬──────────────────────────────┘
                       │ REST API (JSON)
┌──────────────────────▼──────────────────────────────┐
│         ELASTICSEARCH (port 9200)                   │
│  Index: documents · users · similarity_results      │
└─────────────────────────────────────────────────────┘
```

### Flux de traitement

```
Utilisateur
    │
    ▼
[1] Import PDF via JavaFX
    │
    ▼
[2] Backend Java reçoit le fichier et le transmet à Flask
    │
    ▼
[3] Python extrait le texte (pdfplumber), prétraite, calcule BM25 + MiniLM
    │
    ▼
[4] Fusion des scores (30% BM25 + 70% MiniLM) → sauvegarde dans Elasticsearch
    │
    ▼
[5] Score final + recommandations affichés dans l'interface
```

---

## Algorithmes de similarité

### BM25 — Analyse Lexicale `poids : 30%`

Algorithme probabiliste de classement de documents, amélioration de TF-IDF :

- **Saturation de fréquence** `k1 = 1.2` — évite de surévaluer les répétitions de mots-clés
- **Normalisation de longueur** `b = 0.75` — compare équitablement des documents de tailles différentes

> Utilisé pour détecter les **copies mot à mot**.

### MiniLM — Analyse Sémantique `poids : 70%`

Modèle Transformer basé sur `paraphrase-multilingual-MiniLM-L12-v2` (architecture Bi-Encoder) :

- Transforme chaque texte en un **vecteur de 384 dimensions** (embedding)
- Calcule la **similarité cosinus** entre vecteurs
- Supporte nativement le **français**, l'**anglais** et l'**arabe**
- Optimisé par Knowledge Distillation (conserve ~99% des performances de BERT)

> Utilisé pour détecter les **reformulations et paraphrases**.

### Score Hybride Final

```
ScoreFinal = (0.30 × ScoreBM25) + (0.70 × ScoreMiniLM)
```

### Niveaux de risque

| Niveau | Seuil indicatif | Signification |
|---|---|---|
| 🟢 `FAIBLE` | < 20% | Aucun problème apparent |
| 🟡 `MODÉRÉ` | 20% – 40% | Similarités à surveiller |
| 🟠 `ÉLEVÉ` | 40% – 70% | Risque de plagiat significatif |
| 🔴 `CRITIQUE` | > 70% | Plagiat fortement suspecté |

---

## Stack technologique

| Composant | Technologie | Rôle |
|---|---|---|
| Frontend & Backend | Java 11+ / JavaFX / Maven | Interface graphique et logique métier |
| Micro-service IA | Python 3.8+ / Flask | Traitement NLP et calculs de similarité |
| Moteur de recherche | Elasticsearch 7.17 | Stockage, indexation et recherche full-text |
| Monitoring | Kibana 7.17 | Visualisation et supervision |
| Extraction PDF | pdfplumber | Extraction du texte des rapports |
| Analyse lexicale | BM25 (rank_bm25) | Détection de plagiat littéral |
| Analyse sémantique | sentence-transformers (MiniLM) | Détection de paraphrases |
| Calcul vectoriel | scikit-learn / NumPy | Similarité cosinus, normalisation |

---

## Prérequis

Assurez-vous d'avoir installé les éléments suivants avant de continuer :

- **Java 11+** et **Maven 3.6+**
- **Python 3.8+**
- **Elasticsearch 7.17.x** — [Télécharger](https://www.elastic.co/downloads/past-releases/elasticsearch-7-17-0)
- **Kibana 7.17.x** *(optionnel, pour le monitoring)* — [Télécharger](https://www.elastic.co/downloads/past-releases/kibana-7-17-0)

> ⚠️ Elasticsearch et Kibana doivent être de la **même version** (7.17.x).

---

## Installation

### Étape 1 — Cloner le dépôt

```bash
git clone https://github.com/<votre-username>/pds-pfe.git
cd pds-pfe
```

### Étape 2 — Installer les dépendances Python

```bash
cd python
pip install -r requirements.txt
```

Contenu du fichier `requirements.txt` :

```
flask
pdfplumber
sentence-transformers
scikit-learn
numpy
rank-bm25
elasticsearch==7.17.0
bcrypt
```

### Étape 3 — Installer les dépendances Java

```bash
# Backend
cd backend
mvn install

# Frontend
cd ../frontend
mvn install
```

### Étape 4 — Configurer Elasticsearch

Elasticsearch fonctionne sans configuration particulière en local (port `9200` par défaut).

Si nécessaire, éditez `elasticsearch-7.17.0/config/elasticsearch.yml` :

```yaml
network.host: localhost
http.port: 9200
```

### Étape 5 — Créer les index Elasticsearch

Trois index sont nécessaires avant le premier lancement. Ouvrez Kibana sur `http://localhost:5601` → **Menu** → **Dev Tools**, puis exécutez les requêtes ci-dessous.

#### Index `documents`

Avec analyseur français personnalisé et champ vectoriel pour MiniLM :

```json
PUT /documents
{
  "settings": {
    "analysis": {
      "analyzer": {
        "french_analyzer": {
          "tokenizer": "standard",
          "filter": ["lowercase", "french_elision", "light_french_stemmer"]
        }
      },
      "filter": {
        "french_elision": {
          "type": "elision",
          "articles": ["l", "m", "t", "qu", "n", "s", "j", "d"]
        },
        "light_french_stemmer": {
          "type": "stemmer",
          "language": "light_french"
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "title":        { "type": "text", "analyzer": "french_analyzer" },
      "author":       { "type": "text" },
      "specialty":    { "type": "keyword" },
      "abstract":     { "type": "text", "analyzer": "french_analyzer" },
      "introduction": { "type": "text", "analyzer": "french_analyzer" },
      "chapters": {
        "type": "nested",
        "properties": {
          "title":   { "type": "text", "analyzer": "french_analyzer" },
          "content": { "type": "text", "analyzer": "french_analyzer" }
        }
      },
      "embedding": { "type": "dense_vector", "dims": 384 }
    }
  }
}
```

> L'analyseur français enchaîne : tokenisation standard → lowercase → élision (`l'`, `d'`, `qu'`…) → stemming `light_french` (ex : "informatique" et "informaticien" partagent la même racine).

#### Index `users`

```json
PUT /users
{
  "mappings": {
    "properties": {
      "username": { "type": "keyword" },
      "email":    { "type": "keyword" },
      "password": { "type": "keyword" }
    }
  }
}
```

#### Index `similarity_results`

```json
PUT /similarity_results
{
  "mappings": {
    "properties": {
      "user_id":            { "type": "keyword" },
      "filename":           { "type": "keyword" },
      "timestamp":          { "type": "date" },
      "overall_similarity": { "type": "float" },
      "section_scores": {
        "type": "object",
        "properties": {
          "abstract":     { "type": "float" },
          "introduction": { "type": "float" },
          "conclusion":   { "type": "float" }
        }
      },
      "top_documents": {
        "type": "nested",
        "properties": {
          "doc_id":     { "type": "keyword" },
          "title":      { "type": "text" },
          "score":      { "type": "float" },
          "risk_level": { "type": "keyword" }
        }
      },
      "is_deleted": { "type": "boolean" }
    }
  }
}
```

---

## Démarrage

### Ordre de démarrage obligatoire

> ⚠️ Respectez impérativement cet ordre. Le backend Java vérifie la disponibilité d'Elasticsearch au démarrage. L'interface JavaFX dépend du backend.

```
1. Elasticsearch  →  port 9200
2. API Python     →  port 5000
3. Backend Java   →  port 8080
4. Frontend JavaFX
```

### Commandes

```bash
# 1. Démarrer Elasticsearch
cd elasticsearch-7.17.0/bin
./elasticsearch          # Linux/macOS
elasticsearch.bat        # Windows

# 2. Démarrer l'API Python
cd python
python start_api.py

# 3. Démarrer le Backend Java
cd backend
mvn exec:java -Dexec.mainClass="com.pds.pfe.SimpleHttpServer"

# 4. Démarrer le Frontend JavaFX
cd frontend
mvn javafx:run

# (Optionnel) Démarrer Kibana
cd kibana-7.17.0/bin
./kibana                 # Linux/macOS
kibana.bat               # Windows
```

### Vérifier qu'Elasticsearch est bien démarré

```bash
curl http://localhost:9200
```

Une réponse JSON avec `"status" : 200` et le nom du cluster confirme que le service est opérationnel. Si ce n'est pas le cas, attendez 15 à 30 secondes — Elasticsearch peut être long au démarrage.

Kibana sera accessible sur `http://localhost:5601`.

---

## Utilisation

### 1. Créer un compte

Au premier lancement, cliquez sur **"S'inscrire"** et renseignez :
- Nom complet
- Adresse email
- Mot de passe

### 2. Se connecter

Utilisez vos identifiants sur la page de connexion.

### 3. Analyser un rapport

1. Depuis le tableau de bord, cliquez sur **"Importer un fichier"**
2. Sélectionnez un fichier PDF
3. Cliquez sur **"Envoyer"** pour lancer l'analyse
4. Une barre de progression indique l'avancement du traitement

### 4. Consulter les résultats

Les résultats sont présentés en quatre parties :

- **Score global** avec niveau de risque associé
- **Détails par section** : Résumé, Introduction, Conclusion Générale
- **Top Documents Similaires** issus de la base Elasticsearch
- **Recommandations automatiques** selon le niveau de risque détecté

### 5. Gérer l'historique

Les analyses passées sont accessibles depuis la **barre latérale**. Vous pouvez supprimer une analyse via le bouton dédié (une confirmation est requise).

---

## Structure du projet

```
pds-pfe/
│
├── backend/                          # Backend Java
│   ├── src/main/java/com/pds/pfe/
│   │   ├── SimpleHttpServer.java     # Serveur HTTP principal
│   │   ├── AuthHandler.java          # Authentification
│   │   ├── FileUploadHandler.java    # Gestion des uploads PDF
│   │   ├── SimilarityHandler.java    # Proxy vers l'API Python
│   │   ├── HistoryHandler.java       # Historique des analyses
│   │   ├── DocumentHandler.java      # Gestion des documents
│   │   └── HealthHandler.java        # Vérification de l'état du service
│   └── pom.xml
│
├── frontend/                         # Frontend JavaFX
│   ├── src/main/java/com/pds/pfe/
│   │   ├── MainApp.java              # Point d'entrée de l'application
│   │   ├── LoginController.java      # Gestion de la connexion
│   │   ├── DashboardController.java  # Tableau de bord principal
│   │   └── ApiService.java           # Appels HTTP vers le backend
│   └── pom.xml
│
├── python/                           # Micro-service Flask (IA & NLP)
│   ├── start_api.py                  # Point d'entrée Flask
│   ├── pfe_extractor.py              # Extraction PDF (pdfplumber)
│   ├── text_processor.py             # Prétraitement du texte
│   ├── bm25_similarity.py            # Algorithme BM25
│   ├── minilm_similarity.py          # Modèle MiniLM (SBERT)
│   ├── score_combiner.py             # Fusion des scores hybrides
│   ├── elastic_client.py             # Client Elasticsearch
│   └── requirements.txt
│
└── README.md
```

---

---

> Projet de fin de module — **Programmation Java Avancée**
> École Nationale des Sciences Appliquées d'Al Hoceima — Université Abdelmalek Essaadi
> Année universitaire : 2025/2026

**Réalisé par :** AALILOUCH Fatima Zohra · ABAKOUY Asmae · SALIHI Zineb
**Encadré par :** M. BAHRI Abdelkhalak
