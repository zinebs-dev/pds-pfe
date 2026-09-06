from sentence_transformers import SentenceTransformer
import numpy as np
from sklearn.metrics.pairwise import cosine_similarity
import torch
from preprocessing.text_processor import TextProcessor

# Variable globale pour stocker l'instance unique du modèle
_MODEL_INSTANCE = None

class MiniLMSimilarity:
    def __init__(self, model_name='sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2'):
        global _MODEL_INSTANCE

        self.text_processor = TextProcessor()
        self.embeddings = None
        self.doc_ids = []

        # Singleton: On ne charge le modèle que s'il n'existe pas déjà en mémoire
        if _MODEL_INSTANCE is None:
            print(f"Chargement du modèle {model_name} (une seule fois)...")
            # Utilisation du GPU si disponible, sinon CPU
            device = 'cuda' if torch.cuda.is_available() else 'cpu'
            _MODEL_INSTANCE = SentenceTransformer(model_name, device=device)
            print(f"Modèle MiniLM chargé sur {device}")

        self.model = _MODEL_INSTANCE

    def compute_embeddings(self, documents: list):
        """
        Calcule les embeddings pour une liste de documents
        """
        if not documents:
            print("Aucun document à traiter.")
            return

        print("Calcul des embeddings sémantiques...")

        # Mise à jour des IDs
        self.doc_ids = [doc[0] for doc in documents]

        # Prétraitement
        texts = [self.text_processor.preprocess_for_semantic(text) for _, text in documents]

        # Calcul optimisé
        self.embeddings = self.model.encode(
            texts,
            convert_to_tensor=True,
            show_progress_bar=True,
            batch_size=32, # Augmenté pour aller plus vite
            normalize_embeddings=True
        )

        print(f"Embeddings calculés pour {len(documents)} documents")

    def add_document_embedding(self, doc_id: str, text: str):
        """
        Ajoute un seul document sans tout recalculer (Optimisation Cruciale)
        """
        processed_text = self.text_processor.preprocess_for_semantic(text)
        new_embedding = self.model.encode(
            [processed_text],
            convert_to_tensor=True,
            normalize_embeddings=True
        )

        if self.embeddings is None:
            self.embeddings = new_embedding
            self.doc_ids = [doc_id]
        else:
            # Concaténation avec les embeddings existants
            self.embeddings = torch.cat((self.embeddings, new_embedding), 0)
            self.doc_ids.append(doc_id)

    def calculate_similarity(self, query_text: str, top_k: int = 20) -> list:
        if self.embeddings is None:
            return []

        processed_query = self.text_processor.preprocess_for_semantic(query_text)
        query_embedding = self.model.encode([processed_query], convert_to_tensor=True)

        # Passage sur CPU pour numpy
        similarities = cosine_similarity(
            query_embedding.cpu().numpy(),
            self.embeddings.cpu().numpy()
        )[0]

        top_indices = np.argsort(similarities)[::-1][:top_k]

        results = []
        for idx in top_indices:
            # Sécurité pour éviter les index hors limites
            if idx < len(self.doc_ids):
                results.append((self.doc_ids[idx], float(similarities[idx])))

        return results