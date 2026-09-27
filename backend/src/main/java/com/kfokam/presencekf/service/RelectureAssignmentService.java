package com.kfokam.presencekf.service;

import com.kfokam.presencekf.domain.Exercice;
import com.kfokam.presencekf.domain.Presence;
import com.kfokam.presencekf.domain.Relecture;
import com.kfokam.presencekf.domain.enums.StatutExercice;
import com.kfokam.presencekf.domain.enums.StatutRelecture;
import com.kfokam.presencekf.repository.PresenceRepository;
import com.kfokam.presencekf.repository.RelectureRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * EF8 · RG2, RG4, RG5 (révisées étape 3) : tire jusqu'à deux relecteurs
 * distincts au hasard parmi les étudiants présents à la session, hors
 * auteur, dès qu'un exercice est déposé.
 */
@Component
public class RelectureAssignmentService {

    /** RG4 révisée (évolution étape 3) : deux relecteurs distincts par exercice. */
    static final int NB_RELECTEURS_REQUIS = 2;

    private final PresenceRepository presenceRepository;
    private final RelectureRepository relectureRepository;
    private final SecureRandom random = new SecureRandom();

    public RelectureAssignmentService(PresenceRepository presenceRepository, RelectureRepository relectureRepository) {
        this.presenceRepository = presenceRepository;
        this.relectureRepository = relectureRepository;
    }

    /**
     * @return les relectures nouvellement créées (0, 1 ou 2 selon le nombre de
     * candidats disponibles ; sacrifice de périmètre assumé, cf. section 7 du
     * cahier des charges : pas de rattrapage automatique si un candidat
     * devient présent après cette assignation).
     */
    public List<Relecture> assigner(Exercice exercice) {
        List<Relecture> existantes = relectureRepository.findByExerciceId(exercice.getId());
        int aAssigner = NB_RELECTEURS_REQUIS - existantes.size();
        if (aAssigner <= 0) {
            return List.of();
        }

        List<Long> dejaAssignes = existantes.stream().map(Relecture::getRelecteurId).toList();
        List<Long> candidats = new ArrayList<>(presenceRepository.findBySessionId(exercice.getSessionId()).stream()
                .map(Presence::getEtudiantId)
                .filter(id -> !id.equals(exercice.getEtudiantId()))
                .filter(id -> !dejaAssignes.contains(id))
                .distinct()
                .collect(Collectors.toList()));

        List<Relecture> nouvelles = new ArrayList<>();
        int nombre = Math.min(aAssigner, candidats.size());
        for (int i = 0; i < nombre; i++) {
            Long relecteurId = candidats.remove(random.nextInt(candidats.size()));

            Relecture relecture = new Relecture();
            relecture.setExerciceId(exercice.getId());
            relecture.setRelecteurId(relecteurId);
            relecture.setStatut(StatutRelecture.EN_ATTENTE);
            relecture.setAssigneeAt(Instant.now());
            nouvelles.add(relectureRepository.save(relecture));
        }

        if (!existantes.isEmpty() || !nouvelles.isEmpty()) {
            exercice.setStatut(StatutExercice.EN_ATTENTE);
        }

        return nouvelles;
    }
}
