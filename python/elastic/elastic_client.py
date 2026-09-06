from elasticsearch import Elasticsearch
import json
import hashlib
from datetime import datetime
from typing import List, Dict, Any, Optional

class ElasticClient:
    def __init__(self, hosts=['http://localhost:9200']):
        self.es = Elasticsearch(hosts)
        self.index_name = "documents"  # Votre index existant
        self.similarity_index = "similarity_results"  # Index pour l'historique

    def get_all_documents_for_similarity(self) -> List[tuple]:
        """
        Récupère tous les documents depuis Elasticsearch pour l'analyse de similarité
        Returns: Liste de (doc_id, texte_complet)
        """
        try:
            print(" Récupération des documents depuis Elasticsearch...")

            # Requête pour récupérer tous les documents avec leurs contenus
            response = self.es.search(
                index=self.index_name,
                body={
                    "query": {"match_all": {}},
                    "size": 10000,  # Ajustez selon le nombre de vos rapports
                    "_source": [
                        "title",
                        "abstract",
                        "general_introduction",
                        "general_conclusion",
                        "chapters.contenu",
                        "author",
                        "specialty"
                    ]
                }
            )

            documents = []
            for hit in response['hits']['hits']:
                doc_id = hit['_id']
                source = hit['_source']

                # Construction du texte complet pour la similarité
                full_text = self._build_full_text(source)
                documents.append((doc_id, full_text))

            print(f" {len(documents)} documents récupérés depuis Elasticsearch")
            return documents

        except Exception as e:
            print(f" Erreur lors de la récupération depuis Elasticsearch: {e}")
            return []

    def _build_full_text(self, source: Dict) -> str:
        """Construit le texte complet à partir des champs Elasticsearch"""
        text_parts = []

        # Titre
        if source.get('title'):
            text_parts.append(str(source['title']))

        # Résumé
        if source.get('abstract'):
            text_parts.append(str(source['abstract']))

        # Introduction générale
        if source.get('general_introduction'):
            text_parts.append(str(source['general_introduction']))

        # Contenu des chapitres
        if source.get('chapters'):
            for chapter in source['chapters']:
                if chapter.get('contenu'):
                    text_parts.append(str(chapter['contenu']))
                if chapter.get('titre'):
                    text_parts.append(str(chapter['titre']))

        # Conclusion générale
        if source.get('general_conclusion'):
            text_parts.append(str(source['general_conclusion']))

        return " ".join(text_parts)

    def get_document_details(self, doc_id: str) -> Dict[str, Any]:
        """Récupère les détails complets d'un document pour l'affichage"""
        try:
            response = self.es.get(index=self.index_name, id=doc_id)
            source = response['_source']

            return {
                'id': doc_id,
                'title': source.get('titre', 'Sans titre'),
                'author': source.get('author', 'Auteur inconnu'),
                'specialty': source.get('specialty', 'Spécialité non spécifiée'),
                'abstract': source.get('abstract', ''),
                'pdf_url': source.get('pdf_url', ''),
                'general_introduction': source.get('general_introduction', ''),
                'general_conclusion': source.get('general_conclusion', ''),
                'chapters': source.get('chapters', [])
            }
        except Exception as e:
            print(f" Erreur lors de la récupération du document {doc_id}: {e}")
            return {}

    def check_connection(self) -> bool:
        """Vérifie la connexion à Elasticsearch"""
        try:
            return self.es.ping()
        except:
            return False
    def save_document(self, document_data):
        """Sauvegarde un document dans Elasticsearch"""
        try:
            # Générer un ID unique
            doc_id = hashlib.md5(
                json.dumps(document_data, sort_keys=True).encode()
            ).hexdigest()

            # Ajouter un timestamp
            document_data['indexed_at'] = datetime.utcnow().isoformat()

            # Indexer le document
            response = self.es.index(
                index=self.index_name,
                id=doc_id,
                body=document_data
            )

            if response['result'] in ['created', 'updated']:
                print(f" Document indexé avec ID: {doc_id}")
                return doc_id
            else:
                raise Exception(f"Échec de l'indexation: {response}")

        except Exception as e:
            print(f" Erreur lors de l'indexation: {e}")
            raise

    def get_document_count(self):
        """Retourne le nombre de documents dans l'index"""
        try:
            response = self.es.count(index=self.index_name)
            return response['count']
        except:
            return 0

    def get_document_sections(self, doc_id: str) -> Dict[str, str]:
        """Récupère les sections d'un document pour comparaison détaillée"""
        try:
            response = self.es.get(index=self.index_name, id=doc_id)
            source = response['_source']

            sections = {
                'introduction': source.get('general_introduction', ''),
                'conclusion': source.get('general_conclusion', ''),
                'abstract': source.get('abstract', ''),
                'chapters': {}
            }

            # Extraire les chapitres individuellement
            if source.get('chapters'):
                for i, chapter in enumerate(source['chapters']):
                    chapter_title = chapter.get('titre', f'Chapitre {i+1}')
                    chapter_content = chapter.get('contenu', '')
                    sections['chapters'][chapter_title] = chapter_content

            return sections
        except Exception as e:
            print(f" Erreur lors de la récupération des sections du document {doc_id}: {e}")
            return {'introduction': '', 'conclusion': '', 'abstract': '', 'chapters': {}}

    def get_documents_batch(self, doc_ids: List[str]) -> Dict[str, Dict[str, Any]]:
        """
        OPTIMISATION: Récupère plusieurs documents en une seule requête mget
        Retourne un dictionnaire {doc_id: {details, sections}}
        """
        if not doc_ids:
            return {}

        try:
            # Utiliser mget pour récupérer tous les documents en une seule requête
            response = self.es.mget(
                index=self.index_name,
                body={'ids': doc_ids}
            )

            results = {}
            for doc in response.get('docs', []):
                if not doc.get('found', False):
                    continue

                doc_id = doc['_id']
                source = doc['_source']

                # Construire les détails
                details = {
                    'id': doc_id,
                    'title': source.get('titre', 'Sans titre'),
                    'author': source.get('author', 'Auteur inconnu'),
                    'specialty': source.get('specialty', 'Spécialité non spécifiée'),
                    'abstract': source.get('abstract', ''),
                    'pdf_url': source.get('pdf_url', ''),
                    'general_introduction': source.get('general_introduction', ''),
                    'general_conclusion': source.get('general_conclusion', ''),
                    'chapters': source.get('chapters', [])
                }

                # Construire les sections
                sections = {
                    'introduction': source.get('general_introduction', ''),
                    'conclusion': source.get('general_conclusion', ''),
                    'abstract': source.get('abstract', ''),
                    'chapters': {}
                }

                if source.get('chapters'):
                    for i, chapter in enumerate(source['chapters']):
                        chapter_title = chapter.get('titre', f'Chapitre {i+1}')
                        chapter_content = chapter.get('contenu', '')
                        sections['chapters'][chapter_title] = chapter_content

                results[doc_id] = {
                    'details': details,
                    'sections': sections
                }

            print(f"✓ {len(results)} documents récupérés en batch")
            return results

        except Exception as e:
            print(f"✗ Erreur lors de la récupération batch: {e}")
            # Fallback: récupérer un par un
            results = {}
            for doc_id in doc_ids:
                details = self.get_document_details(doc_id)
                sections = self.get_document_sections(doc_id)
                if details:
                    results[doc_id] = {'details': details, 'sections': sections}
            return results

    def get_similarity_history(self, limit: int = 50) -> list:
        """Récupère l'historique des résultats de similarité (non supprimés)"""
        try:
            response = self.es.search(
                index=self.similarity_index,
                body={
                    "query": {
                        "bool": {
                            "must_not": [
                                {"term": {"is_deleted": True}}
                            ]
                        }
                    },
                    "sort": [{"timestamp": {"order": "desc"}}],
                    "size": limit
                }
            )

            # Ajouter l'ID à chaque résultat
            history = []
            for hit in response['hits']['hits']:
                result = hit['_source']
                result['id'] = hit['_id']
                history.append(result)

            return history

        except Exception as e:
            print(f"Erreur lors de la récupération de l'historique: {e}")
            return []

    def save_similarity_result(self, result_data: Dict[str, Any]) -> str:
        """Sauvegarde un résultat de similarité dans l'index similarity_results"""
        try:
            # Générer un ID unique pour ce résultat
            result_id = hashlib.md5(
                f"{result_data.get('user_id', '')}_{result_data.get('filename', '')}_{datetime.utcnow().isoformat()}".encode()
            ).hexdigest()

            # Ajouter la date de comparaison
            result_data['comparison_date'] = datetime.utcnow().isoformat()

            # Indexer le résultat
            response = self.es.index(
                index=self.similarity_index,
                id=result_id,
                body=result_data
            )

            if response['result'] in ['created', 'updated']:
                print(f"Résultat de similarité sauvegardé avec ID: {result_id}")
                return result_id
            else:
                raise Exception(f"Échec de la sauvegarde: {response}")

        except Exception as e:
            print(f"Erreur lors de la sauvegarde du résultat: {e}")
            raise

    def get_similarity_result(self, result_id: str) -> Optional[Dict[str, Any]]:
        """Récupère un résultat de similarité spécifique par son ID"""
        try:
            response = self.es.get(
                index=self.similarity_index,
                id=result_id
            )

            result = response['_source']
            result['id'] = result_id

            if result.get('is_deleted', False):
                print(f"✗ Le résultat {result_id} est marqué comme supprimé")
                return None

            print(f"✓ Résultat {result_id} récupéré")
            return result

        except Exception as e:
            print(f"✗ Erreur lors de la récupération du résultat {result_id}: {e}")
            return None

    def get_user_history(self, user_id: str, limit: int = 50) -> List[Dict[str, Any]]:
        """Récupère l'historique des analyses d'un utilisateur (non supprimées)"""
        try:
            response = self.es.search(
                index=self.similarity_index,
                body={
                    "query": {
                        "bool": {
                            "must": [
                                {"term": {"user_id": user_id}}
                            ],
                            "must_not": [
                                {"term": {"is_deleted": True}}  # <-- EXCLURE les supprimés
                            ]
                        }
                    },
                    "sort": [
                        {"comparison_date": {"order": "desc",
                                             "missing": "_last",
                                             "unmapped_type": "date"
                                             }
                         }
                    ],
                    "size": limit
                }
            )

            history = []
            for hit in response['hits']['hits']:
                result = hit['_source']
                result['id'] = hit['_id']
                history.append(result)

            print(f"{len(history)} résultats d'historique récupérés pour l'utilisateur {user_id}")
            return history

        except Exception as e:
            print(f"Erreur lors de la récupération de l'historique: {e}")
            return []

    def delete_similarity_result(self, result_id: str) -> bool:
        """Marque un résultat comme supprimé sans le supprimer physiquement"""
        try:
            response = self.es.update(
                index=self.similarity_index,
                id=result_id,
                body={
                    "doc": {
                        "is_deleted": True,
                        "deleted_at": datetime.now().isoformat()
                    }
                }
            )
            return response['result'] in ['updated', 'noop']
        except Exception as e:
            print(f"Erreur lors du marquage du résultat: {e}")
            return False
