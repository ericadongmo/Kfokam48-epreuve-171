package com.kfokam.presencekf.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Issue #30 (évolution étape 3) : chaque exercice est relu par deux
 * relecteurs distincts, la note retenue est la moyenne des relectures
 * rendues, provisoire tant que l'une des deux n'a pas rendu.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RelectureDeuxRelecteursIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void note_devient_la_moyenne_des_deux_relectures_et_est_provisoire_tant_qu_une_seule_est_rendue() throws Exception {
        String reponseOuverture = mockMvc.perform(post("/api/sessions")
                        .contentType("application/json")
                        .content("""
                                { "titre": "Séance deux relecteurs", "promotionId": 1 }
                                """))
                .andReturn().getResponse().getContentAsString();
        String code = objectMapper.readTree(reponseOuverture).get("code").asText();

        // Trois étudiants présents : 1 (auteur), 2 et 3 (relecteurs potentiels).
        for (long etudiantId : new long[]{1, 2, 3}) {
            mockMvc.perform(post("/api/presences")
                            .contentType("application/json")
                            .content("{ \"code\": \"" + code + "\", \"etudiantId\": " + etudiantId + " }"))
                    .andExpect(status().isCreated());
        }

        String reponseDepot = mockMvc.perform(post("/api/exercices")
                        .contentType("application/json")
                        .content(depotAvecSession(reponseOuverture)))
                .andReturn().getResponse().getContentAsString();
        long exerciceId = objectMapper.readTree(reponseDepot).get("id").asLong();

        long relecture2 = relectureAssigneeA(2L, exerciceId);
        long relecture3 = relectureAssigneeA(3L, exerciceId);
        assertThat(relecture2).isNotEqualTo(relecture3);

        // Avant toute note rendue.
        mockMvc.perform(get("/api/exercices/" + exerciceId + "/note"))
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.note").doesNotExist())
                .andExpect(jsonPath("$.provisoire").value(false));

        // Un seul des deux relecteurs a rendu : note provisoire.
        mockMvc.perform(post("/api/relectures/" + relecture2)
                        .contentType("application/json")
                        .content("{ \"note\": 10, \"commentaire\": \"Premier avis\", \"relecteurId\": 2 }"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/exercices/" + exerciceId + "/note"))
                .andExpect(jsonPath("$.note").value(10.0))
                .andExpect(jsonPath("$.provisoire").value(true));

        // Le second relecteur rend sa note : la note devient la moyenne, définitive.
        mockMvc.perform(post("/api/relectures/" + relecture3)
                        .contentType("application/json")
                        .content("{ \"note\": 16, \"commentaire\": \"Second avis\", \"relecteurId\": 3 }"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/exercices/" + exerciceId + "/note"))
                .andExpect(jsonPath("$.statut").value("RENDUE"))
                .andExpect(jsonPath("$.note").value(13.0))
                .andExpect(jsonPath("$.provisoire").value(false));
    }

    private String depotAvecSession(String reponseOuverture) throws Exception {
        long sessionId = objectMapper.readTree(reponseOuverture).get("id").asLong();
        return "{ \"sessionId\": " + sessionId + ", \"etudiantId\": 1, \"lien\": \"https://example.com/deux-relecteurs\" }";
    }

    private long relectureAssigneeA(Long relecteurId, long exerciceId) throws Exception {
        String reponse = mockMvc.perform(get("/api/etudiants/" + relecteurId + "/relectures"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode liste = objectMapper.readTree(reponse);
        for (JsonNode relecture : liste) {
            if (relecture.get("exerciceId").asLong() == exerciceId) {
                return relecture.get("relectureId").asLong();
            }
        }
        throw new AssertionError("Aucune relecture assignée à l'étudiant " + relecteurId + " pour l'exercice " + exerciceId);
    }
}
