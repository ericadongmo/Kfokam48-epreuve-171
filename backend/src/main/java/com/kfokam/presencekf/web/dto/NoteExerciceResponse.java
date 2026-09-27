package com.kfokam.presencekf.web.dto;

import com.kfokam.presencekf.domain.enums.StatutRelecture;

/**
 * EF14 · RG6 : jamais le nom ni l'identifiant du relecteur.
 * EF9bis (évolution étape 3) : note = moyenne des relectures rendues,
 * provisoire tant que l'une des relectures assignées n'a pas rendu.
 */
public record NoteExerciceResponse(
        StatutRelecture statut,
        Double note,
        String commentaire,
        boolean provisoire
) {
}
