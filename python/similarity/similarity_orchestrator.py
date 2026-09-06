import time
import logging
from typing import List, Tuple, Dict, Any
from .bm25_similarity import BM25Similarity
from .minilm_embeddings import MiniLMSimilarity
from .score_combiner import ScoreCombiner

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class SimilarityOrchestrator:
    # Pattern Singleton pour l'orchestrateur lui-même
    _instance = None

    def __new__(cls, *args, **kwargs):
        if cls._instance is None:
            cls._instance = super(SimilarityOrchestrator, cls).__new__(cls)
            cls._instance.initialized = False
        return cls._instance

    def __init__(self, bm25_weight=0.3, semantic_weight=0.7):
        # On évite la ré-initialisation si déjà fait
        if getattr(self, 'initialized', False):
            return

        self.bm25 = BM25Similarity()
        self.minilm = MiniLMSimilarity()
        self.combiner = ScoreCombiner(bm25_weight, semantic_weight)
        self.is_ready = False # Renommé pour éviter confusion avec __init__
        self.total_documents = 0
        self.initialized = True

    def initialize_from_elasticsearch(self, elastic_client):
        """
        Initialise les modèles. Si déjà initialisé, ne fait rien (GAIN DE TEMPS).
        """
        if self.is_ready:
            logger.info("Modèles déjà chargés en mémoire. Ignorer l'initialisation.")
            return

        logger.info("Initialisation COMPLÈTE des modèles depuis Elasticsearch...")
        start_time = time.time()

        documents = elastic_client.get_all_documents_for_similarity()

        if documents:
            self.total_documents = len(documents)
            self.bm25.build_index(documents)
            self.minilm.compute_embeddings(documents)
            self.is_ready = True
        else:
            logger.warning("Aucun document dans Elasticsearch pour le moment.")
            # On marque comme prêt même si vide, pour permettre les ajouts futurs
            self.is_ready = True

        end_time = time.time()
        logger.info(f"Temps d'initialisation: {end_time - start_time:.2f}s")

    def add_document_dynamic(self, doc_id, text):
        """
        Ajoute un document à l'index en mémoire dynamiquement sans tout recharger
        """
        # Mise à jour BM25 (si supporté par ta classe BM25, sinon faudra rebuilder parfois)
        # Pour l'instant on se concentre sur MiniLM qui est le plus lent
        self.minilm.add_document_embedding(doc_id, text)
        self.total_documents += 1
        logger.info(f"Document {doc_id} ajouté à l'index mémoire dynamique.")

    def find_similar_documents(self, query_text: str, top_k: int = 20) -> Dict[str, Any]:
        if not self.is_ready:
            # Tentative de récupération soft ou erreur
            logger.warning("Modèles non initialisés. Résultats vides.")
            return {'final_results': [], 'statistics': {}}

        start_time = time.time()

        # Calculs
        bm25_results = self.bm25.calculate_similarity(query_text, top_k * 2)
        semantic_results = self.minilm.calculate_similarity(query_text, top_k * 2)
        combined_results = self.combiner.combine_scores(bm25_results, semantic_results)

        final_results = combined_results[:top_k]
        end_time = time.time()

        stats = {
            'processing_time': round(end_time - start_time, 2),
            'final_matches': len(final_results)
        }

        return {
            'final_results': final_results,
            'statistics': stats,
            'bm25_sample': bm25_results[:5],
            'semantic_sample': semantic_results[:5]
        }