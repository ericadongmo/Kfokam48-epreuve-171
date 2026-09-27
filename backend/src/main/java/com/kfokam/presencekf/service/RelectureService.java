package com.kfokam.presencekf.service;

import com.kfokam.presencekf.domain.Exercice;
import com.kfokam.presencekf.domain.Relecture;
import com.kfokam.presencekf.domain.Session;
import com.kfokam.presencekf.domain.enums.StatutExercice;
import com.kfokam.presencekf.domain.enums.StatutRelecture;
import com.kfokam.presencekf.error.ApiException;
import com.kfokam.presencekf.repository.ExerciceRepository;
import com.kfokam.presencekf.repository.RelectureRepository;
import com.kfokam.presencekf.repository.SessionRepository;
import com.kfokam.presencekf.web.dto.NoteExerciceResponse;
import com.kfokam.presencekf.web.dto.RelectureAssigneeResponse;
import com.kfokam.presencekf.web.dto.RelectureResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class RelectureService {

    private final RelectureRepository relectureRepository;
    private final ExerciceRepository exerciceRepository;
    private final SessionRepository sessionRepository;

    public RelectureService(RelectureRepository relectureRepository, ExerciceRepository exerciceRepository,
                             SessionRepository sessionRepository) {
        this.relectureRepository = relectureRepository;
        this.exerciceRepository = exerciceRepository;
        this.sessionRepository = sessionRepository;
    }

    /**
     * EF9, EF10, EF11 · RG2, RG3, RG7, RG12.
     * <p>
     * Résolution de la contradiction section 7 du cahier des charges (Q10 prime
     * sur Q15) : ce même point d'entrée sert au premier envoi et à la correction.
     * {@code RELECTURE_DEJA_RENDUE} est réservé à {@link #commencer(Long)}, seul
     * endroit où le contrat OpenAPI l'attache sans ambiguïté ; ici, le seul gel
     * possible est la clôture de session (RG12).
     */
    @Transactional
    public RelectureResponse rendre(Long relectureId, Integer note, String commentaire, Long relecteurId) {
        if (note == null || note < 0 || note > 20) {
            throw ApiException.noteInvalide();
        }

        Relecture relecture = relectureRepository.findById(relectureId).orElseThrow(ApiException::relectureInconnue);
        Exercice exercice = exerciceRepository.findById(relecture.getExerciceId()).orElseThrow(ApiException::exerciceInconnu);

        if (exercice.getEtudiantId().equals(relecteurId)) {
            throw ApiException.autoRelecture();
        }
        if (!relecture.getRelecteurId().equals(relecteurId)) {
            throw ApiException.accesRefuse();
        }

        Session session = sessionRepository.findById(exercice.getSessionId()).orElseThrow(ApiException::sessionInconnue);
        if (session.isCloturee()) {
            throw ApiException.sessionCloturee();
        }

        relecture.setNote(note);
        relecture.setCommentaire(commentaire);
        relecture.setStatut(StatutRelecture.RENDUE);
        relecture.setRenduAt(Instant.now());
        relectureRepository.save(relecture);

        exercice.setStatut(StatutExercice.RELU);
        exerciceRepository.save(exercice);

        return new RelectureResponse(relecture.getId(), relecture.getStatut());
    }

    /** RG10 : « relecture commencée » = passage à EN_COURS, déclenché à l'ouverture de l'écran relecteur. */
    @Transactional
    public RelectureResponse commencer(Long relectureId) {
        Relecture relecture = relectureRepository.findById(relectureId).orElseThrow(ApiException::relectureInconnue);
        if (relecture.getStatut() == StatutRelecture.RENDUE) {
            throw ApiException.relectureDejaRendue();
        }
        if (relecture.getStatut() == StatutRelecture.EN_ATTENTE) {
            relecture.setStatut(StatutRelecture.EN_COURS);
            relectureRepository.save(relecture);
        }
        return new RelectureResponse(relecture.getId(), relecture.getStatut());
    }

    /**
     * EF14 · RG6 : jamais le nom ni l'identifiant du relecteur.
     * EF9bis (évolution étape 3) : moyenne des relectures rendues, provisoire
     * tant que l'une des relectures assignées n'a pas rendu. Le commentaire
     * affiché est celui de la relecture rendue la plus récente.
     */
    @Transactional(readOnly = true)
    public NoteExerciceResponse noteDeExercice(Long exerciceId) {
        if (!exerciceRepository.existsById(exerciceId)) {
            throw ApiException.exerciceInconnu();
        }

        List<Relecture> relectures = relectureRepository.findByExerciceId(exerciceId);
        List<Relecture> rendues = relectures.stream()
                .filter(r -> r.getStatut() == StatutRelecture.RENDUE)
                .toList();

        if (rendues.isEmpty()) {
            return new NoteExerciceResponse(StatutRelecture.EN_ATTENTE, null, null, false);
        }

        double moyenne = rendues.stream().mapToInt(Relecture::getNote).average().orElseThrow();
        String commentaire = rendues.stream()
                .max(java.util.Comparator.comparing(Relecture::getRenduAt))
                .map(Relecture::getCommentaire)
                .orElse(null);
        boolean provisoire = rendues.size() < relectures.size();
        StatutRelecture statut = provisoire ? StatutRelecture.EN_COURS : StatutRelecture.RENDUE;

        return new NoteExerciceResponse(statut, moyenne, commentaire, provisoire);
    }

    @Transactional(readOnly = true)
    public List<RelectureAssigneeResponse> pourRelecteur(Long relecteurId) {
        return relectureRepository.findByRelecteurIdIn(List.of(relecteurId)).stream()
                .map(r -> {
                    Exercice exercice = exerciceRepository.findById(r.getExerciceId()).orElseThrow(ApiException::exerciceInconnu);
                    return new RelectureAssigneeResponse(r.getId(), exercice.getId(), exercice.getLien(),
                            r.getStatut(), r.getNote(), r.getCommentaire());
                })
                .toList();
    }
}
