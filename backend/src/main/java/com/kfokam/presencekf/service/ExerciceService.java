package com.kfokam.presencekf.service;

import com.kfokam.presencekf.domain.Exercice;
import com.kfokam.presencekf.domain.Relecture;
import com.kfokam.presencekf.domain.Session;
import com.kfokam.presencekf.domain.enums.StatutExercice;
import com.kfokam.presencekf.domain.enums.StatutRelecture;
import com.kfokam.presencekf.error.ApiException;
import com.kfokam.presencekf.repository.EtudiantRepository;
import com.kfokam.presencekf.repository.ExerciceRepository;
import com.kfokam.presencekf.repository.RelectureRepository;
import com.kfokam.presencekf.repository.SessionRepository;
import com.kfokam.presencekf.web.dto.ExerciceResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ExerciceService {

    private final ExerciceRepository exerciceRepository;
    private final SessionRepository sessionRepository;
    private final EtudiantRepository etudiantRepository;
    private final RelectureRepository relectureRepository;
    private final UrlValidator urlValidator;
    private final RelectureAssignmentService relectureAssignmentService;

    public ExerciceService(ExerciceRepository exerciceRepository, SessionRepository sessionRepository,
                            EtudiantRepository etudiantRepository, RelectureRepository relectureRepository,
                            UrlValidator urlValidator, RelectureAssignmentService relectureAssignmentService) {
        this.exerciceRepository = exerciceRepository;
        this.sessionRepository = sessionRepository;
        this.etudiantRepository = etudiantRepository;
        this.relectureRepository = relectureRepository;
        this.urlValidator = urlValidator;
        this.relectureAssignmentService = relectureAssignmentService;
    }

    /** EF6, EF7 · RG9, RG14 : dépôt, autorisé même sans présence (section 7 : Q12 prime), refusé après clôture. */
    @Transactional
    public ExerciceResponse deposer(Long sessionId, Long etudiantId, String lien) {
        urlValidator.valider(lien);

        Session session = sessionRepository.findById(sessionId).orElseThrow(ApiException::sessionInconnue);
        if (!etudiantRepository.existsById(etudiantId)) {
            throw ApiException.etudiantInconnu();
        }
        if (session.isCloturee()) {
            throw ApiException.sessionCloturee();
        }
        if (exerciceRepository.existsBySessionIdAndEtudiantId(sessionId, etudiantId)) {
            throw ApiException.exerciceDejaDepose();
        }

        Exercice exercice = new Exercice();
        exercice.setSessionId(sessionId);
        exercice.setEtudiantId(etudiantId);
        exercice.setLien(lien);
        exercice.setStatut(StatutExercice.DEPOSE);
        exercice.setDeposeAt(Instant.now());
        exercice = exerciceRepository.save(exercice);

        relectureAssignmentService.assigner(exercice);
        exercice = exerciceRepository.save(exercice);

        return new ExerciceResponse(exercice.getId(), exercice.getStatut());
    }

    /** EF15 · RG10 : remplaçable tant qu'aucune relecture n'a commencé (statut EN_ATTENTE ou absente). */
    @Transactional
    public ExerciceResponse remplacerLien(Long exerciceId, Long etudiantId, String lien) {
        urlValidator.valider(lien);

        Exercice exercice = exerciceRepository.findById(exerciceId).orElseThrow(ApiException::exerciceInconnu);
        if (!exercice.getEtudiantId().equals(etudiantId)) {
            throw ApiException.accesRefuse();
        }

        Session session = sessionRepository.findById(exercice.getSessionId()).orElseThrow(ApiException::sessionInconnue);
        if (session.isCloturee()) {
            throw ApiException.sessionCloturee();
        }

        // RG10, étendue étape 3 : bloqué dès qu'UNE des relectures assignées a démarré.
        boolean uneRelectureACommence = relectureRepository.findByExerciceId(exerciceId).stream()
                .anyMatch(r -> r.getStatut() != StatutRelecture.EN_ATTENTE);
        if (uneRelectureACommence) {
            throw ApiException.remplacementImpossible();
        }

        exercice.setLien(lien);
        exercice = exerciceRepository.save(exercice);
        return new ExerciceResponse(exercice.getId(), exercice.getStatut());
    }

    @Transactional(readOnly = true)
    public Exercice trouverPourSessionEtEtudiant(Long sessionId, Long etudiantId) {
        return exerciceRepository.findBySessionIdAndEtudiantId(sessionId, etudiantId)
                .orElseThrow(ApiException::exerciceInconnu);
    }

    @Transactional(readOnly = true)
    public Exercice trouver(Long id) {
        return exerciceRepository.findById(id).orElseThrow(ApiException::exerciceInconnu);
    }
}
