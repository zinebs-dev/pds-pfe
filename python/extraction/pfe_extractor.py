import re
import json
import pdfplumber
import os
import io
import unicodedata
from datetime import datetime
from typing import Dict
import logging

logger = logging.getLogger(__name__)

class PFEExtractor:
    """Extracteur de métadonnées depuis PDFs en mémoire - GARDE TOUS TES PATTERNS"""

    def __init__(self):
        self.sections = {}

    def extract_from_bytes(self, pdf_bytes: bytes) -> Dict:
        """Extraire depuis un PDF en mémoire"""
        try:
            if not pdf_bytes or len(pdf_bytes) == 0:
                logger.error("PDF vide ou invalide")
                return {"error": "Le fichier PDF est vide"}

            logger.info(f"Début extraction PDF ({len(pdf_bytes)} bytes)")

            pdf_stream = io.BytesIO(pdf_bytes)

            try:
                pages_text = self.extract_text_from_pdf_stream(pdf_stream)
            except Exception as e:
                logger.error(f"Erreur lors de l'extraction du texte: {e}")
                return {"error": f"Impossible de lire le PDF: {str(e)}"}

            if not pages_text or pages_text.get('total_pages', 0) == 0:
                logger.error("Aucune page extraite du PDF")
                return {"error": "Le PDF ne contient aucune page lisible"}

            # Texte complet
            full_text = "\n\n".join([
                txt for k, txt in pages_text.items()
                if isinstance(k, int)
            ])

            if not full_text or len(full_text.strip()) < 50:
                logger.warning("Texte extrait très court ou vide")
                return {"error": "Le PDF ne contient pas assez de texte exploitable"}

            first_page_text = pages_text.get(1, "")

            logger.info("Extraction des métadonnées...")

            # Extraction complète avec TES patterns
            try:
                data = {
                    "title": self.extract_title(full_text),
                    "author": self.extract_author(full_text),
                    "specialty": self.extract_specialty(first_page_text),
                    "abstract": self.extract_abstract(full_text).get("abstract", ""),
                    "table_of_contents": self.extract_table_of_contents(pages_text),
                    "general_introduction": self.extract_introduction(pages_text),
                    "general_conclusion": self.extract_conclusion(pages_text),
                    "chapters": self.extract_chapters(pages_text),
                    "extraction_date": datetime.now().isoformat()
                }

                logger.info(f"Extraction réussie: {pages_text.get('total_pages', 0)} pages")
                return data

            except Exception as e:
                logger.error(f"Erreur lors de l'extraction des métadonnées: {e}")
                logger.exception(e)
                return {"error": f"Erreur lors de l'extraction des métadonnées: {str(e)}"}

        except Exception as e:
            logger.error(f"Erreur critique extraction PDF: {e}")
            logger.exception(e)
            return {"error": f"Erreur critique: {str(e)}"}

    def extract_text_from_pdf_stream(self, pdf_stream):
        """Extraire texte depuis un stream BytesIO"""
        pages_text = {}
        try:
            with pdfplumber.open(pdf_stream) as pdf:
                total_pages = len(pdf.pages)
                pages_text['total_pages'] = total_pages

                if total_pages == 0:
                    logger.warning("PDF sans pages")
                    return pages_text

                logger.info(f"Extraction de {total_pages} pages...")

                for page_num, page in enumerate(pdf.pages, start=1):
                    try:
                        page_height = page.height
                        header_margin_pts = 2 * 28.3465
                        footer_margin_pts = 3 * 28.3465

                        main_bbox = (
                            0,
                            header_margin_pts,
                            page.width,
                            page_height - footer_margin_pts
                        )

                        main_text = page.within_bbox(main_bbox).extract_text()
                        if main_text:
                            pages_text[page_num] = main_text.strip()
                    except Exception as e:
                        logger.warning(f"Erreur extraction page {page_num}: {e}")
                        continue

            return pages_text
        except Exception as e:
            logger.error(f"Erreur lors de l'ouverture du PDF: {e}")
            raise

    def find_section_page(self, pages_text: dict, section_patterns: list) -> int:
        total_pages = pages_text.get('total_pages', 0)

        for page_num in range(1, total_pages + 1):
            if page_num not in pages_text:
                continue

            page_content = pages_text[page_num]
            page_content_clean = re.sub(r'\s+', ' ', page_content)

            for pattern in section_patterns:
                # Chercher au DÉBUT de la page (premiers 500 caractères)
                search_area = page_content_clean[:500]
                if re.search(pattern, search_area, re.IGNORECASE):
                    return page_num

        return -1

    def extract_title(self, text: str) -> str:
        patterns = [
            r'(?:Sous\s+le\s+thème|Titre)\s*[:\-]?\s*(.+)',
            r'Sous\s*(?:le\s*)?th[eè]me\s*[:\-]?\s*(.+)',
            r'Titre\s*[:\-]?\s*(.+)',
            r'Titled\s*[:\-]?\s*(.+)',
            r'Title\s*[:\-]?\s*(.+)',
            r'(?<=Energétique et Energie renouvelables)([\s\S]*?)(?=Réalisé Par|Préparé Par)',
            r'(?<=Mécanique)([\s\S]*?)(?=Réalisé Par|Préparé Par)',
            r'(?<=Génie Civil)([\s\S]*?)(?=Réalisé Par|Préparé Par)',
            r'(?<=Option Bâtiments, Ponts et Chaussées)([\s\S]*?)(?=Réalisé Par|Préparé Par)',
            r'(?<=Hydraulique)([\s\S]*?)(?=Réalisé Par|Préparé Par)',
            r'(?<=Informatique)([\s\S]*?)(?=Réalisé Par|Préparé Par)',
            r'(?<=Engineering)([\s\S]*?)(?=Réalisé Par|Préparé Par)',
            r'(?<=Data Engineering)([\s\S]*?)(?=Prepared|Submitted)',
            r'(?<=des Données)([\s\S]*?)(?=Réalisé Par|Préparé Par)',
            r'Option\s*\:\s*GénieLogiciel([\s\S]*?)(?=RéaliséPar|Préparé Par)',
            r'Discipline\s*\:\s*Software Engineering([\s\S]*?)(?=Réalisé Par|Préparé Par|Submitted by)',

        ]

        for pattern in patterns:
            match = re.search(pattern, text, re.IGNORECASE | re.DOTALL)
            if match:
                title = match.group(1).strip()
                title = re.split(
                    r'\s*(?:Réalisé|Présenté|Préparé|Submitted|Prepared|Candidat|Encadré)',
                    title, flags=re.IGNORECASE
                )[0]
                title = re.sub(r'\s+', ' ', title)
                title = re.sub(r'^[#\-*\s]+|[#\-*\s]+$', '', title)

                if 5 <= len(title) <= 150:
                    return title.strip()

        return None
    def extract_author(self, text: str) -> str:
        patterns = [
            r'\bFull Name\b[:\s]*([^\n]+)',
            r'Prepared by[:\s]*([^\n]+)',
            r'Réalisé\s*par[:\s]*([^\n]+)',
            r'(?<=Submitted by)[ \t]*([^\n]+?)(?=\s*(?:Defended|Supervised by))',
            r'(?<=Réalisé par)[ \t]*([^\n]+?)(?=\s*(?:Encadré\s*par))',
            r'(?<=Submitted by\s)([^\t\n]+)',
            r'(?<=Réalisé par\s)([^\t\n]+)',
            r'Submitted by[:\s]*([^\n]+)',
            r'Réalisé\s*par[:\s]*([^\n]+)',
            r'Nom[:\s]*([^\n]+)',
            r'Présenté par[:\s]*([^\n]+)',
            r'Préparé par[:\s]*([^\n]+)',
            r'Étudiant[:\s]*([^\n]+)',
            r'Student[:\s]*([^\n]+)',
            r'Par[:\s]*([^\n]+)(?=\s*(?:sous|encadré))',
        ]

        for pattern in patterns:
            match = re.search(pattern, text, re.IGNORECASE)
            if match:
                author = match.group(1) if match.groups() else match.group(0)
                author = re.split(r'\s*(?:sous|encadré|supervised|by)\s*', author, flags=re.IGNORECASE)[0]
                if self.is_valid_name(author):
                    return author

        lines = text.split('\n')
        for i, line in enumerate(lines[:30]):
            line_lower = line.lower()
            if any(keyword in line_lower for keyword in ['prepared by', 'submitted by', 'by:', 'par:', 'réalisé par']):
                for j in range(i+1, min(i+5, len(lines))):
                    potential_author = lines[j].strip()
                    if self.is_valid_name(potential_author):
                        return potential_author

        return None

    def is_valid_name(self, name: str) -> bool:
        name_clean = name.strip()
        if (len(name_clean) < 4 or len(name_clean) > 100 or
                any(keyword in name_clean.lower() for keyword in
                    ['university', 'school', 'faculty', 'abstract', 'résumé'])):
            return False
        if ' ' not in name_clean:
            return False
        letter_ratio = sum(1 for c in name_clean if c.isalpha() or c.isspace()) / len(name_clean)
        return letter_ratio > 0.7

    def extract_specialty(self, text: str) -> str:
        patterns = [
            r"d['’]ingénieur d[’']état\s*(?::|\n)?\s*(?:en\s*)?(.*?)(?=\s*(?:Présenté\s+par|Encadré\s+par|Sous\s+la\s+direction|Réalisé\s+par|$))",
            r"d['’]ingénieur d[’']état\s*(?::|\n)?\s*(?:en\s*)?(.*)",
            r'd’ingénieur d’état\s*((?:|\n)\s*?::en\s*)?(.*)',
            r'd’ingénieur d’état\s*(?::|\n)\s*(.*)',
            r'Filière[:\s]*([^\n]+)',
            r'Field[:\s]*([^\n]+)',
            r'Option[:\s]*([^\n]+)',
            r'Spécialité[:\s]*([^\n]+)',
            r'Departement[:\s]*([^\n]+)',
        ]

        for pattern in patterns:
            match = re.search(pattern, text, re.IGNORECASE)
            if match:
                specialty = match.group(1).strip()
                if 2 < len(specialty) < 100:
                    return specialty
        return None

    def extract_abstract(self, text: str) -> dict:
        patterns = [
            r'(?<=\bRésumé\b)[\s:–\-]*([\s\S]{300,3000}?)(?=\b(Abstract|Sommaire|List\s*of\s*Abbreviations)\b)',
            r'(?<=\bRESUME\b)[\s:–\-]*([\s\S]{300,3000}?)(?=\b(Abstract|Sommaire|List\s*of\s*Abbreviations)\b)',
            r'(?<=\bRÉSUMÉ\b)[\s:–\-]*([\s\S]{300,3000}?)(?=\b(Abstract|Sommaire|List\s*of\s*Abbreviations)\b)',
            r'(^[\s\S]{300,2000}?)(?=\b(Abstract|Sommaire|List\s*of\s*Abbreviations)\b)',
        ]
        for pattern in patterns:
            match = re.search(pattern, text, re.IGNORECASE)
            if match:
                abstract_text = match.group(1).strip()
                abstract_text = re.sub(r'\s+', ' ', abstract_text)
                return {"abstract": abstract_text}
        return {"abstract": None}

    def extract_table_of_contents(self, text_dict) -> dict:
        if isinstance(text_dict, dict):
            text = "\n".join([txt for k, txt in text_dict.items() if isinstance(k, int)])
        else:
            text = text_dict
        chapter_patterns = [
            r'^(?:Chapitre|Chapter)\s+([0-9IVX]+)\s*[:\-–]?\s*(.+)$',
            r'^(\d{1,2})\s+(.+)$'
        ]
        toc = {"chapitres": []}
        seen_chapter_numbers = set()
        current_chapter = None
        for line in text.split('\n'):
            line_clean = line.strip()
            if len(line_clean) < 3:
                continue
            chapter_detected = False
            for pattern in chapter_patterns:
                chapter_match = re.search(pattern, line_clean, re.IGNORECASE)
                if chapter_match:
                    chapter_num = chapter_match.group(1).strip()
                    chapter_title = chapter_match.group(2).strip()
                    if 'chapitre' in pattern.lower():
                        full_chapter_title = f"Chapitre {chapter_num} : {chapter_title}"
                    else:
                        full_chapter_title = f"{chapter_num} {chapter_title}"
                    if chapter_num not in seen_chapter_numbers:
                        seen_chapter_numbers.add(chapter_num)
                        current_chapter = {
                            "titre": full_chapter_title,
                            "numero": chapter_num,
                            "sous_titres": []
                        }
                        toc["chapitres"].append(current_chapter)
                        chapter_detected = True
                    break
            if chapter_detected:
                continue
            if current_chapter:
                ultra_pattern = r'^(\d+(?:\.\d+)*\.?\s+.+)$'
                subtitle_match = re.search(ultra_pattern, line_clean)
                if subtitle_match:
                    subtitle_full = subtitle_match.group(1).strip()
                    is_main_chapter = any(re.search(pattern, line_clean) for pattern in chapter_patterns)
                    if not is_main_chapter:
                        subtitle_full = re.sub(r'\s+', ' ', subtitle_full)
                        if subtitle_full not in current_chapter["sous_titres"]:
                            current_chapter["sous_titres"].append(subtitle_full)

        return toc

    def extract_introduction(self, pages_text: dict) -> str:
        intro_patterns = [
            r'^Introduction\s+[Gg][ée]n[ée]rale',
            r'^INTRODUCTION\s+G[ÉÉ]N[ÉÉ]RALE',
            r'^General\s+Introduction',
            r'^GENERAL\s+INTRODUCTION',
            r'^Introduction\s*$',
            r'^INTRODUCTION\s*$',
            r'^Introduction\b',
            r'^INTRODUCTION\b',
        ]

        # Patterns de fin (début du corps du document)
        end_patterns = [
            # Patterns pour les parties/chapitres principaux
            r'Partie\s*[I1]',
            r'PARTIE\s*[I1]',
            r'Chapitre\s*[I1]',
            r'CHAPITRE\s*[I1]',
            r'Chapter\s*[I1]',
            r'CHAPTER\s*[I1]',

            # Patterns étendus pour les numéros de chapitres/parties
            r'Partie\s+[IVX\d]',
            r'Chapitre\s+[IVX\d]',
            r'Chapter\s+[IVX\d]',
            r'SECTION\s+[IVX\d]',

            # Patterns pour les chapitres avec deux-points
            r'Chapter\s*:',
            r'CHAPTER\s*:',
            r'Chapitre\s*:',
            r'CHAPITRE\s*:',
            r'Partie\s*:',
            r'PARTIE\s*:',

            # Patterns pour les titres de chapitres complets
            r'Chapter\s*:\s*[A-Z][A-Za-z\s]+',
            r'CHAPTER\s*:\s*[A-Z][A-Za-z\s]+',
            r'Chapitre\s*:\s*[A-Z][A-Za-z\s]+',
            r'CHAPITRE\s*:\s*[A-Z][A-Za-z\s]+',

            # Patterns pour la numérotation romaine/arabe
            r'[IVX]+\.\s+[A-Z]',  # "I. ", "II. ", etc.
            r'\d+\.\d+\s+',  # "1.1 ", "2.3 ", etc.
            r'^\s*\d+\s*$',
        ]

        total_pages = pages_text.get('total_pages', 0)
        intro_start_page = self.find_section_page(pages_text, intro_patterns)
        if intro_start_page == -1:
            return None
        intro_end_page = -1
        for page_num in range(intro_start_page + 1, min(intro_start_page + 15, total_pages + 1)):
            if page_num not in pages_text:
                continue
            page_content = pages_text[page_num]
            page_content_clean = re.sub(r'\s+', ' ', page_content)
            search_area = page_content_clean[:300]
            for pattern in end_patterns:
                if re.search(pattern, search_area, re.IGNORECASE):
                    intro_end_page = page_num
                    break
            if intro_end_page != -1:
                break
        if intro_end_page == -1:
            intro_end_page = min(intro_start_page + 10, total_pages)
        intro_content = []
        for page_num in range(intro_start_page, intro_end_page):
            if page_num in pages_text:
                page_text = pages_text[page_num]
                if page_num == intro_start_page:
                    for pattern in intro_patterns:
                        page_text = re.sub(pattern, '', page_text, count=1, flags=re.IGNORECASE)
                intro_content.append(page_text)
        full_intro = "\n\n".join(intro_content).strip()
        if len(full_intro) > 100:
            return self.clean_text(full_intro)
        else:
            return None

    def extract_conclusion(self, pages_text: dict) -> str:
        concl_patterns = [
            r'^Conclusion\s+et\s+[Pp]erspective',
            r'^CONCLUSION\s+ET\s+PERSPECTIVE',
            r'^Conclusion\s+and\s+[Pp]erspective',
            r'^CONCLUSION\s+AND\s+PERSPECTIVE',
            r'^Conclusions?\s+et\s+[Pp]erspectives?',
            r'^CONCLUSIONS?\s+ET\s+PERSPECTIVES?',
            r'^Conclusions?\s+and\s+[Pp]erspectives?',
            r'^CONCLUSIONS?\s+AND\s+PERSPECTIVES?',

            r'^Conclusion\s+[Gg][ée]n[ée]rale\s+et\s+[Pp]erspective',
            r'^CONCLUSION\s+G[EÉ]N[EÉ]RALE\s+ET\s+PERSPECTIVE',
            r'^General\s+[Cc]onclusion\s+and\s+[Pp]erspective',
            r'^GENERAL\s+CONCLUSION\s+AND\s+PERSPECTIVE',

            r'^Conclusion\s+et\s+[Rr]ecommandation',
            r'^CONCLUSION\s+ET\s+RECOMMANDATION',
            r'^Conclusion\s+and\s+[Rr]ecommendation',
            r'^CONCLUSION\s+AND\s+RECOMMENDATION',
            r'^Conclusion\s+[Gg][ée]n[ée]rale',
            r'^CONCLUSION\s+G[EÉ]N[EÉ]RALE',
            r'^General\s+[Cc]onclusion',
            r'^GENERAL\s+CONCLUSION',
            r'^CONCLUSION',
            r'^Conclusion\s*$',
        ]

        # Patterns de fin
        end_patterns = [
            r'R[ée]f[ée]rences',
            r'RÉFÉRENCES',
            r'References',
            r'REFERENCES',
            r'Bibliographie',
            r'BIBLIOGRAPHIE',
            r'Bibliography',
            r'Annexes?',
            r'ANNEXES?',
            r'ANNEX?',
            r'Annex?',
            r'Appendices?',
            r'Webographie',
            r'WEBOGRAPHIE',
        ]
        total_pages = pages_text.get('total_pages', 0)
        start_search = max(1, int(total_pages * 0.7))
        concl_start_page = -1
        for page_num in range(start_search, total_pages + 1):
            if page_num not in pages_text:
                continue
            page_content = pages_text[page_num]
            page_content_clean = re.sub(r'\s+', ' ', page_content)
            search_area = page_content_clean[:500]
            for pattern in concl_patterns:
                if re.search(pattern, search_area, re.IGNORECASE):
                    concl_start_page = page_num
                    break
            if concl_start_page != -1:
                break
        if concl_start_page == -1:
            return None
        concl_end_page = -1
        for page_num in range(concl_start_page + 1, min(concl_start_page + 15, total_pages + 1)):
            if page_num not in pages_text:
                continue
            page_content = pages_text[page_num]
            page_content_clean = re.sub(r'\s+', ' ', page_content)
            search_area = page_content_clean[:300]
            for pattern in end_patterns:
                if re.search(pattern, search_area, re.IGNORECASE):
                    concl_end_page = page_num
                    break
            if concl_end_page != -1:
                break
        if concl_end_page == -1:
            concl_end_page = total_pages + 1
        concl_content = []
        for page_num in range(concl_start_page, concl_end_page):
            if page_num in pages_text:
                page_text = pages_text[page_num]
                if page_num == concl_start_page:
                    for pattern in concl_patterns:
                        page_text = re.sub(pattern, '', page_text, count=1, flags=re.IGNORECASE)
                concl_content.append(page_text)
        full_concl = "\n\n".join(concl_content).strip()
        if len(full_concl) > 100:
            return self.clean_text(full_concl)
        else:
            return None

    def extract_chapters(self, pages_text: dict) -> dict:
        chapter_patterns = [
            r'^Chapitre[\s\n\t]+[1-9]\d*',       # Chapitre 1, Chapitre 2, etc.
            r'^Chapter[\s\n\t]+[1-9]\d*',        # Chapter 1, Chapter 2, etc.
            r'^CHAPITRE[\s\n\t]+[1-9]\d*',       # CHAPITRE 1
            r'^CHAPTER[\s\n\t]+[1-9]\d*',        # CHAPTER 1
            r'^Chapitre[\s\n\t]+[IVX]+',         # Chapitre I, II, III...
            r'^Chapter[\s\n\t]+[IVX]+',          # Chapter I, II, III...
        ]
        end_patterns = [
            r'Conclusion\s+Générale',
            r'Conclusion\s+Générale',
            r'General\s+Conclusion',
            r'CONCLUSION\s+GÉNÉRALE',
            r'CONCLUSION\s+GENERALE',
            r'Conclusion\s+et\s+perspectives',
            r'Conclusion\s+and\s+perspectives'
        ]
        total_pages = pages_text.get('total_pages', 0)
        chapters = {}
        current_chapter = None
        current_content = []
        in_chapter_section = False
        chapter_start_page = -1
        first_chapter_page = -1
        for page_num in range(1, total_pages + 1):
            if page_num not in pages_text:
                continue

            page_content = pages_text[page_num]
            first_lines = "\n".join(page_content.split('\n')[:5])  # Regarder les premières lignes

            for pattern in chapter_patterns:
                if re.search(pattern, first_lines, re.IGNORECASE):
                    first_chapter_page = page_num
                    break

            if first_chapter_page != -1:
                break
        if first_chapter_page == -1:
            return {"Erreur":None}
        conclusion_page = -1
        start_search = max(first_chapter_page, int(total_pages * 0.6))

        for page_num in range(start_search, total_pages + 1):
            if page_num not in pages_text:
                continue

            page_content = pages_text[page_num]
            first_lines = "\n".join(page_content.split('\n')[:10])

            for pattern in end_patterns:
                if re.search(pattern, first_lines, re.IGNORECASE):
                    conclusion_page = page_num
                    break

            if conclusion_page != -1:
                break
        for page_num in range(first_chapter_page, conclusion_page if conclusion_page != -1 else total_pages + 1):
            if page_num not in pages_text:
                continue

            page_content = pages_text[page_num]
            lines = page_content.split('\n')
            new_chapter_detected = False
            chapter_title = None
            for i in range(min(5, len(lines))):
                line = lines[i].strip()
                for pattern in chapter_patterns:
                    match = re.search(pattern, line, re.IGNORECASE)
                    if match and len(line) < 100:
                        new_chapter_detected = True
                        chapter_title = line.strip()
                        break
                if new_chapter_detected:
                    break
            if new_chapter_detected and chapter_title:
                if current_chapter and current_content:
                    full_content = "\n".join(current_content).strip()
                    if len(full_content) > 200:  # Vérifier que le contenu est suffisamment long
                        chapters[current_chapter] = full_content
                current_chapter = chapter_title
                current_content = []
                title_line_index = 0
                for i, line in enumerate(lines):
                    if chapter_title in line:
                        title_line_index = i
                        break
                remaining_content = lines[title_line_index + 1:]
                if remaining_content:
                    current_content.append("\n".join(remaining_content))
            elif current_chapter:
                current_content.append(page_content)
        if current_chapter and current_content:
            full_content = "\n".join(current_content).strip()
            if len(full_content) > 200:
                chapters[current_chapter] = full_content
        cleaned_chapters = {}
        for title, content in chapters.items():
            # Nettoyer le contenu
            cleaned_content = re.sub(r'\.{4,}', '', content)  # Supprimer les séries de points
            cleaned_content = re.sub(r'\s+', ' ', cleaned_content)  # Normaliser les espaces
            cleaned_content = cleaned_content.strip()

            if len(cleaned_content) > 100:
                cleaned_chapters[title] = cleaned_content

        if cleaned_chapters:
            return cleaned_chapters
        else:
            return {"Erreur": None}

    def clean_text(self, text: str, preserve_paragraphs: bool = True) -> str:
        """
        Nettoyage centralisé pour gros blocs de texte (introduction, conclusion, chapitres).
        preserve_paragraphs=True conserve les paragraphes (double newline) au lieu d'écraser tout en espace.
        """
        if not text:
            return ""

        # 1. Normalisation unicode
        text = unicodedata.normalize('NFKC', text)

        # 2. Supprimer caractères de contrôle invisibles (remplacés par espace)
        text = re.sub(r'[\x00-\x1f\x7f]', ' ', text)

        # 3. Supprimer numéros de pages isolés (ligne contenant juste un nombre)
        text = re.sub(r'\n\s*\d+\s*\n', '\n', text)

        # 4. Supprimer motifs "Page 12" ou variantes
        text = re.sub(r'\bPage\s*\d+\b', '', text, flags=re.IGNORECASE)

        # 5. Remplacer longues séries de points :
        #    conserve une ellipse '...' si >=3, réduit '..' en '.' (optionnel)
        text = re.sub(r'\.{3,}', '...', text)
        text = re.sub(r'\.{2}', '.', text)

        # 6. Enlever tirets/puces répétées
        text = re.sub(r'[-_•*]{2,}', ' ', text)

        # 7. Normaliser espaces mais conserver paragraphes si demandé
        if preserve_paragraphs:
            # split paragraphs on 2+ newlines, clean inside each paragraph
            paragraphs = re.split(r'\n{2,}', text)
            cleaned_paragraphs = []
            for p in paragraphs:
                # réduire espaces multiples/newlines internes à un espace, puis trim
                p_clean = re.sub(r'\s+', ' ', p).strip()
                if p_clean:
                    cleaned_paragraphs.append(p_clean)
            text = '\n\n'.join(cleaned_paragraphs)
        else:
            text = re.sub(r'\s+', ' ', text).strip()

        # Final trim
        return text.strip()