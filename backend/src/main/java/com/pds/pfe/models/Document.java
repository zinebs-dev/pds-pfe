package com.pds.pfe.models;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class Document {
    private String id;
    private String title;
    private String pdfUrl;
    private String author;
    private String specialty;
    private String abstractText;
    private TableOfContents tableOfContents;
    private String generalIntroduction;
    private String generalConclusion;
    private List<Chapter> chapters;
    private LocalDateTime extractionDate;
    private int sourceLength;

    // Constructeurs
    public Document() {}

    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }

    public String getAbstractText() { return abstractText; }
    public void setAbstractText(String abstractText) { this.abstractText = abstractText; }

    public TableOfContents getTableOfContents() { return tableOfContents; }
    public void setTableOfContents(TableOfContents tableOfContents) { this.tableOfContents = tableOfContents; }

    public String getGeneralIntroduction() { return generalIntroduction; }
    public void setGeneralIntroduction(String generalIntroduction) { this.generalIntroduction = generalIntroduction; }

    public String getGeneralConclusion() { return generalConclusion; }
    public void setGeneralConclusion(String generalConclusion) { this.generalConclusion = generalConclusion; }

    public List<Chapter> getChapters() { return chapters; }
    public void setChapters(List<Chapter> chapters) { this.chapters = chapters; }

    public LocalDateTime getExtractionDate() { return extractionDate; }
    public void setExtractionDate(LocalDateTime extractionDate) { this.extractionDate = extractionDate; }

    public int getSourceLength() { return sourceLength; }
    public void setSourceLength(int sourceLength) { this.sourceLength = sourceLength; }

    // Classes internes
    public static class TableOfContents {
        private List<Chapter> chapitres;

        public List<Chapter> getChapitres() { return chapitres; }
        public void setChapitres(List<Chapter> chapitres) { this.chapitres = chapitres; }
    }

    public static class Chapter {
        private String titre;
        private String numero;
        private String sousTitres;
        private String contenu;

        public String getTitre() { return titre; }
        public void setTitre(String titre) { this.titre = titre; }

        public String getNumero() { return numero; }
        public void setNumero(String numero) { this.numero = numero; }

        public String getSousTitres() { return sousTitres; }
        public void setSousTitres(String sousTitres) { this.sousTitres = sousTitres; }

        public String getContenu() { return contenu; }
        public void setContenu(String contenu) { this.contenu = contenu; }
    }
}