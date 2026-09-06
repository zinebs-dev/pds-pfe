import re
import nltk
from nltk.corpus import stopwords
from nltk.tokenize import word_tokenize
from nltk.stem import SnowballStemmer, WordNetLemmatizer
import string

# Téléchargement automatique des données NLTK si manquantes
def download_nltk_resources():
    resources = {
        'punkt': 'tokenizers/punkt',
        'stopwords': 'corpora/stopwords',
        'wordnet': 'corpora/wordnet'
    }

    for resource, path in resources.items():
        try:
            # Vérifier si la ressource existe
            nltk.data.find(path)
        except LookupError:
            print(f" Téléchargement de {resource}...")
            nltk.download(resource, quiet=True)

# Appeler la fonction au chargement du module
download_nltk_resources()

class TextProcessor:
    def __init__(self):
        # S'assurer que les données sont disponibles
        try:
            self.stop_words = set(stopwords.words('french'))
            self.stemmer = SnowballStemmer('french')
            self.lemmatizer = WordNetLemmatizer()
            self.punctuation = string.punctuation + '«»”“…'
        except LookupError:
            print(" Données NLTK manquantes. Téléchargement...")
            download_nltk_resources()
            self.stop_words = set(stopwords.words('french'))
            self.stemmer = SnowballStemmer('french')
            self.lemmatizer = WordNetLemmatizer()
            self.punctuation = string.punctuation + '«»”“…'

    def clean_text(self, text: str) -> str:
        """Nettoie et normalise le texte"""
        if not text:
            return ""

        # Convertir en minuscules
        text = text.lower()

        # Supprimer les caractères spéciaux mais garder les lettres accentuées
        text = re.sub(r'[^a-zA-Zàâäéèêëïîôöùûüÿç\s]', ' ', text)

        # Supprimer les espaces multiples
        text = re.sub(r'\s+', ' ', text).strip()

        return text

    def tokenize(self, text: str) -> list:
        """Tokenise le texte en mots"""
        return word_tokenize(text, language='french')

    def remove_stopwords(self, tokens: list) -> list:
        """Supprime les mots vides"""
        return [token for token in tokens
                if token not in self.stop_words
                and len(token) > 2
                and token not in self.punctuation]

    def stem_tokens(self, tokens: list) -> list:
        """Réduit les mots à leur racine"""
        return [self.stemmer.stem(token) for token in tokens]

    def lemmatize_tokens(self, tokens: list) -> list:
        """Lemmatise les tokens (forme de base)"""
        return [self.lemmatizer.lemmatize(token) for token in tokens]

    def preprocess_for_bm25(self, text: str) -> list:
        """Prétraitement complet pour BM25"""
        cleaned_text = self.clean_text(text)
        tokens = self.tokenize(cleaned_text)
        tokens = self.remove_stopwords(tokens)
        tokens = self.stem_tokens(tokens)
        return tokens

    def preprocess_for_semantic(self, text: str) -> str:
        """Prétraitement pour l'analyse sémantique (plus léger)"""
        cleaned_text = self.clean_text(text)
        return cleaned_text