package com.pds.pfe.ui.services; // Ou com.pds.pfe.ui.controllers selon votre structure

import javafx.concurrent.Task;
import java.io.File;
import java.util.Map;

public class SmartAnalysisTask extends Task<Map<String, Object>> {

    private final File fileToAnalyze;

    public SmartAnalysisTask(File file) {
        this.fileToAnalyze = file;
    }

    @Override
    protected Map<String, Object> call() throws Exception {
        // PHASE 1 : Simulation upload (0% -> 30%)
        updateMessage("Envoi du fichier...");
        for (int i = 0; i <= 30; i++) {
            if (isCancelled()) return null;
            updateProgress(i, 100);
            Thread.sleep(10); // Rapide
        }

        // PHASE 2 : Attente analyse Python (30% -> 90%)
        updateMessage("️ Analyse de similarité en cours...");

        // On lance un petit thread parallèle pour faire avancer la barre doucement
        // pendant que le VRAI code attend la réponse du serveur
        Thread animator = new Thread(() -> {
            try {
                for (int i = 31; i <= 90; i++) {
                    if (isCancelled()) break;
                    updateProgress(i, 100);
                    // Plus on avance, plus on ralentit (effet psychologique)
                    Thread.sleep(i < 70 ? 50 : 150);
                }
            } catch (InterruptedException e) { }
        });
        animator.setDaemon(true);
        animator.start();

        // PHASE 3 : L'APPEL RÉEL (Bloquant)
        Map<String, Object> result;
        try {
            // C'est ici qu'on appelle votre ApiService de manière SYNCHRONE
            // Assurez-vous d'avoir la méthode 'analyzeFileSynchronous' dans ApiService (voir plus bas)
            result = ApiService.analyzeFileSynchronous(fileToAnalyze);
        } catch (Exception e) {
            animator.interrupt();
            updateProgress(0, 100);
            updateMessage("Erreur de connexion");
            throw e;
        }

        // PHASE 4 : Finition (100%)
        animator.interrupt();
        updateProgress(100, 100);
        updateMessage("Analyse terminée");

        return result;
    }
}