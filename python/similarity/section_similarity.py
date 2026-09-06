"""
Module pour calculer la similarité par section (introduction, chapitres, conclusion)
OPTIMISÉ: Utilise un modèle singleton et le traitement par batch
"""
import logging
from typing import Dict, List, Tuple, Any
from .bm25_similarity import BM25Similarity
from .minilm_embeddings import MiniLMSimilarity
from .score_combiner import ScoreCombiner
from sklearn.metrics.pairwise import cosine_similarity
import numpy as np

logger = logging.getLogger(__name__)

# === SINGLETON GLOBAL POUR LE MODÈLE ===
# Évite de recharger le modèle à chaque appel (gain: ~30-60s par appel)
_SECTION_MODEL_INSTANCE = None

def _get_section_model():
    """Retourne l'instance singleton du modèle SentenceTransformer"""
    global _SECTION_MODEL_INSTANCE
    if _SECTION_MODEL_INSTANCE is None:
        from sentence_transformers import SentenceTransformer
        import torch
        logger.info("Chargement du modèle SentenceTransformer pour les sections (une seule fois)...")
        device = 'cuda' if torch.cuda.is_available() else 'cpu'
        _SECTION_MODEL_INSTANCE = SentenceTransformer(
            'sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2',
            device=device
        )
        logger.info(f"Modèle de section chargé sur {device}")
    return _SECTION_MODEL_INSTANCE


class SectionSimilarityCalculator:
    """Calcule la similarité section par section - VERSION OPTIMISÉE"""

    def __init__(self, bm25_weight=0.3, semantic_weight=0.7):
        self.bm25_weight = bm25_weight
        self.semantic_weight = semantic_weight
        self.combiner = ScoreCombiner(bm25_weight, semantic_weight)
        # Lazy loading: le modèle sera chargé au premier appel
        self._model = None

    @property
    def model(self):
        """Lazy loading du modèle - chargé uniquement au premier accès"""
        if self._model is None:
            self._model = _get_section_model()
        return self._model

    def calculate_section_similarity(
        self,
        query_section_text: str,
        target_section_text: str
    ) -> float:
        """
        Calcule la similarité entre deux sections de texte
        """
        if not query_section_text or not target_section_text:
            return 0.0

        # Pour les sections courtes, on utilise une approche simplifiée
        if len(query_section_text) < 100 or len(target_section_text) < 100:
            return self._simple_similarity(query_section_text, target_section_text)

        # Pour les sections plus longues, on utilise les embeddings
        return self._semantic_similarity(query_section_text, target_section_text)

    def _simple_similarity(self, text1: str, text2: str) -> float:
        """Similarité simple basée sur les mots communs (Jaccard)"""
        words1 = set(text1.lower().split())
        words2 = set(text2.lower().split())

        if not words1 or not words2:
            return 0.0

        intersection = words1.intersection(words2)
        union = words1.union(words2)

        return len(intersection) / len(union) if union else 0.0

    def _semantic_similarity(self, text1: str, text2: str) -> float:
        """Similarité sémantique avec MiniLM - OPTIMISÉ avec singleton"""
        try:
            # Utiliser le modèle singleton via property (lazy loading)
            embeddings = self.model.encode([text1, text2], show_progress_bar=False)
            similarity = cosine_similarity([embeddings[0]], [embeddings[1]])[0][0]
            return float(similarity)
        except Exception as e:
            logger.warning(f"Erreur lors du calcul de similarité sémantique: {e}")
            return self._simple_similarity(text1, text2)

    def _batch_semantic_similarity(self, text_pairs: List[Tuple[str, str]]) -> List[float]:
        """
        Calcule la similarité pour plusieurs paires de textes en batch
        OPTIMISATION: Un seul appel encode() pour tous les textes
        """
        if not text_pairs:
            return []

        try:
            # Collecter tous les textes uniques
            all_texts = []
            for t1, t2 in text_pairs:
                all_texts.extend([t1, t2])

            # Encoder tous les textes en une seule fois (lazy loading via property)
            all_embeddings = self.model.encode(all_texts, show_progress_bar=False, batch_size=32)

            # Calculer les similarités par paires
            results = []
            for i in range(0, len(all_embeddings), 2):
                emb1 = all_embeddings[i]
                emb2 = all_embeddings[i + 1]
                sim = cosine_similarity([emb1], [emb2])[0][0]
                results.append(float(sim))

            return results
        except Exception as e:
            logger.warning(f"Erreur batch similarity: {e}")
            return [self._simple_similarity(t1, t2) for t1, t2 in text_pairs]
    
    def compare_documents_by_sections(
        self,
        query_sections: Dict[str, Any],
        target_sections: Dict[str, Any]
    ) -> Dict[str, float]:
        """
        Compare deux documents section par section - VERSION OPTIMISÉE BATCH

        Args:
            query_sections: Sections du document uploadé
            target_sections: Sections du document de référence

        Returns:
            Dictionnaire avec les scores par section
        """
        results = {}

        # === OPTIMISATION: Collecter toutes les paires pour traitement batch ===
        text_pairs = []
        pair_labels = []  # Pour savoir à quelle section appartient chaque paire

        # Sections simples (introduction, abstract, conclusion)
        simple_sections = ['introduction', 'abstract', 'conclusion']
        for section_name in simple_sections:
            q_text = query_sections.get(section_name, '')
            t_text = target_sections.get(section_name, '')
            if q_text and t_text and len(q_text) >= 100 and len(t_text) >= 100:
                text_pairs.append((q_text, t_text))
                pair_labels.append(('simple', section_name))
            elif q_text and t_text:
                # Sections courtes: calcul direct avec Jaccard
                results[section_name] = self._simple_similarity(q_text, t_text)

        # Chapitres
        query_chapters = query_sections.get('chapters', {})
        target_chapters = target_sections.get('chapters', {})
        chapter_pairs_info = []  # (q_title, t_title, index_in_text_pairs)

        if query_chapters and target_chapters:
            for q_title, q_content in query_chapters.items():
                if not q_content or len(str(q_content)) < 50:
                    continue
                for t_title, t_content in target_chapters.items():
                    if not t_content or len(str(t_content)) < 50:
                        continue
                    text_pairs.append((str(q_content), str(t_content)))
                    pair_labels.append(('chapter', (q_title, t_title)))

        # === TRAITEMENT BATCH: Un seul appel encode() pour tout ===
        if text_pairs:
            batch_scores = self._batch_semantic_similarity(text_pairs)

            # Distribuer les scores
            chapter_scores = []
            for i, (label_type, label_info) in enumerate(pair_labels):
                score = batch_scores[i] if i < len(batch_scores) else 0.0

                if label_type == 'simple':
                    results[label_info] = score
                elif label_type == 'chapter':
                    q_title, t_title = label_info
                    chapter_scores.append({
                        'query_chapter': q_title,
                        'target_chapter': t_title,
                        'score': score
                    })

            # Garder les meilleurs scores de chapitres
            if chapter_scores:
                chapter_scores.sort(key=lambda x: x['score'], reverse=True)
                results['chapters'] = chapter_scores[:3]

        return results

