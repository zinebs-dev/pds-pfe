import numpy as np
from sklearn.preprocessing import MinMaxScaler

class ScoreCombiner:
    def __init__(self, bm25_weight=0.3, semantic_weight=0.7):
        """
        bm25_weight: importance de la similarité lexicale (mots)
        semantic_weight: importance de la similarité sémantique (sens)
        """
        self.bm25_weight = bm25_weight
        self.semantic_weight = semantic_weight
        self.scaler = MinMaxScaler()

    def normalize_scores(self, scores_dict: dict) -> dict:
        """Normalise les scores entre 0 et 1"""
        if not scores_dict:
            return {}

        scores = list(scores_dict.values())
        if len(scores) == 1:
            return {doc_id: 1.0 for doc_id in scores_dict.keys()}

        scores_normalized = self.scaler.fit_transform(np.array(scores).reshape(-1, 1))

        normalized_dict = {}
        for i, doc_id in enumerate(scores_dict.keys()):
            normalized_dict[doc_id] = float(scores_normalized[i][0])

        return normalized_dict

    def combine_scores(self, bm25_scores: list, semantic_scores: list) -> list:
        """
        Combine les scores BM25 et sémantiques
        Returns: liste de (doc_id, score_final) triée
        """
        print(" Combinaison intelligente des scores...")

        # Conversion en dictionnaires
        bm25_dict = {doc_id: score for doc_id, score in bm25_scores}
        semantic_dict = {doc_id: score for doc_id, score in semantic_scores}

        # Normalisation
        bm25_normalized = self.normalize_scores(bm25_dict)
        semantic_normalized = self.normalize_scores(semantic_dict)

        # Combinaison pondérée
        combined_scores = {}
        all_doc_ids = set(bm25_dict.keys()) | set(semantic_dict.keys())

        for doc_id in all_doc_ids:
            bm25_score = bm25_normalized.get(doc_id, 0)
            semantic_score = semantic_normalized.get(doc_id, 0)

            # Score final pondéré
            final_score = (self.bm25_weight * bm25_score +
                           self.semantic_weight * semantic_score)

            combined_scores[doc_id] = final_score

        # Tri par score décroissant
        sorted_scores = sorted(combined_scores.items(), key=lambda x: x[1], reverse=True)

        print(f" Scores combinés pour {len(sorted_scores)} documents")
        return sorted_scores