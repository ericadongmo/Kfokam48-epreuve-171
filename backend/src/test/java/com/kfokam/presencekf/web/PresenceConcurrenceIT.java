package com.kfokam.presencekf.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Issue #28 : deux marquages de présence quasi simultanés pour le même
 * (session, étudiant) — cas rapporté par le client ("deux étudiants côte à
 * côte, le code tapé presque en même temps, un seul apparaît dans ma liste").
 *
 * Le service vérifie l'unicité (existsBySessionIdAndEtudiantId) puis insère,
 * sans que les deux opérations soient atomiques : sous contention réelle, la
 * requête perdante lève une violation de contrainte unique non interceptée,
 * qui remonte en 500 ERREUR_INTERNE au lieu du 409 DEJA_PRESENT attendu.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PresenceConcurrenceIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void deux_marquages_concurrents_pour_le_meme_etudiant_ne_doivent_jamais_produire_une_erreur_interne() throws Exception {
        String reponseOuverture = mockMvc.perform(post("/api/sessions")
                        .contentType("application/json")
                        .content("""
                                { "titre": "Séance concurrence", "promotionId": 1 }
                                """))
                .andReturn().getResponse().getContentAsString();

        JsonNode session = objectMapper.readTree(reponseOuverture);
        String code = session.get("code").asText();
        String corps = "{ \"code\": \"" + code + "\", \"etudiantId\": 7 }";

        ExecutorService executeur = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> requete = () -> mockMvc.perform(post("/api/presences")
                            .contentType("application/json")
                            .content(corps))
                    .andReturn().getResponse().getStatus();

            List<Future<Integer>> resultats = executeur.invokeAll(List.of(requete, requete));
            executeur.shutdown();
            executeur.awaitTermination(5, TimeUnit.SECONDS);

            List<Integer> statuts = resultats.stream().map(f -> {
                try {
                    return f.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).toList();

            assertThat(statuts).as("statuts HTTP des deux requêtes concurrentes").containsExactlyInAnyOrder(201, 409);
        } finally {
            executeur.shutdownNow();
        }
    }
}
