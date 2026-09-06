from rank_bm25 import BM25Okapi
import numpy as np
from preprocessing.text_processor import TextProcessor

class BM25Similarity:
    def __init__(self):
        self.text_processor = TextProcessor()
        self.bm25 = None
        self.corpus_tokens = []
        self.doc_ids = []

    def build_index(self, documents: list):
        """
        Construit l'index BM25
        documents: liste de (doc_id, texte)
        """
        print("🏗  Construction de l'index BM25...")

        self.doc_ids = [doc[0] for doc in documents]
        self.corpus_tokens = []

        for doc_id, text in documents:
            tokens = self.text_processor.preprocess_for_bm25(text)
            self.corpus_tokens.append(tokens)

        # Création de l'index BM25
        self.bm25 = BM25Okapi(self.corpus_tokens)
        print(f" Index BM25 construit avec {len(documents)} documents")

    def calculate_similarity(self, query_text: str, top_k: int = 20) -> list:
        """
        Calcule la similarité BM25
        Returns: liste de (doc_id, score)
        """
        if not self.bm25:
            raise ValueError(" Index BM25 non construit. Appelez build_index() d'abord.")

        # Prétraitement de la requête
        query_tokens = self.text_processor.preprocess_for_bm25(query_text)

        # Calcul des scores
        scores = self.bm25.get_scores(query_tokens)

        # Sélection des meilleurs résultats
        top_indices = np.argsort(scores)[::-1][:top_k]

        results = []
        for idx in top_indices:
            if scores[idx] > 0:  # Ignorer les scores nuls
                results.append((self.doc_ids[idx], float(scores[idx])))

        print(f" BM25 a trouvé {len(results)} documents similaires")
        return results