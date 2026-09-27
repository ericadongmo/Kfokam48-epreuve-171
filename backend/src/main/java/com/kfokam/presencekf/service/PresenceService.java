package com.kfokam.presencekf.service;

import com.kfokam.presencekf.domain.Presence;
import com.kfokam.presencekf.domain.Session;
import com.kfokam.presencekf.domain.enums.SourcePresence;
import com.kfokam.presencekf.error.ApiException;
import com.kfokam.presencekf.repository.EtudiantRepository;
import com.kfokam.presencekf.repository.PresenceRepository;
import com.kfokam.presencekf.repository.SessionRepository;
import com.kfokam.presencekf.web.dto.PresenceResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class PresenceService {

    private final SessionRepository sessionRepository;
    private final EtudiantRepository etudiantRepository;
    private final PresenceRepository presenceRepository;
    private final RateLimitService rateLimitService;

    public PresenceService(SessionRepository sessionRepository, EtudiantRepository etudiantRepository,
                            PresenceRepository presenceRepository, RateLimitService rateLimitService) {
        this.sessionRepository = sessionRepository;
        this.etudiantRepository = etudiantRepository;
        this.presenceRepository = presenceRepository;
        this.rateLimitService = rateLimitService;
    }

    /** EF2, EF3, EF4, EF17 · RG1, RG13, RG15, RG16 : marquage par l'étudiant lui-même via un code. */
    @Transactional
    public PresenceResponse marquerParCode(String code, Long etudiantId) {
        if (!etudiantRepository.existsById(etudiantId)) {
            throw ApiException.etudiantInconnu();
        }

        rateLimitService.verifierAutorise(etudiantId);

        Session session = resoudreCode(code, etudiantId);

        if (session.isCloturee()) {
            throw ApiException.sessionCloturee();
        }
        if (presenceRepository.existsBySessionIdAndEtudiantId(session.getId(), etudiantId)) {
            throw ApiException.dejaPresent();
        }

        Presence presence = enregistrerOuDejaPresent(session.getId(), etudiantId, SourcePresence.ETUDIANT);
        return toResponse(presence);
    }

    /** RG15 : seules l'inconnaissance et l'expiration du code comptent comme « erreurs de code ». */
    private Session resoudreCode(String code, Long etudiantId) {
        Session session;
        try {
            session = sessionRepository.findByCode(code).orElseThrow(ApiException::codeInconnu);
        } catch (ApiException e) {
            rateLimitService.enregistrerEchec(etudiantId);
            throw e;
        }
        if (Instant.now().isAfter(session.getExpirationAt())) {
            rateLimitService.enregistrerEchec(etudiantId);
            throw ApiException.codeExpire();
        }
        rateLimitService.enregistrerSucces(etudiantId);
        return session;
    }

    /** EF5 · RG11, RG13 : ajout manuel par le formateur, sans passer par le code. */
    @Transactional
    public PresenceResponse ajouterManuellement(Long sessionId, Long etudiantId) {
        Session session = sessionRepository.findById(sessionId).orElseThrow(ApiException::sessionInconnue);
        if (!etudiantRepository.existsById(etudiantId)) {
            throw ApiException.etudiantInconnu();
        }
        if (session.isCloturee()) {
            throw ApiException.sessionCloturee();
        }
        if (presenceRepository.existsBySessionIdAndEtudiantId(sessionId, etudiantId)) {
            throw ApiException.dejaPresent();
        }

        Presence presence = enregistrerOuDejaPresent(sessionId, etudiantId, SourcePresence.FORMATEUR);
        return toResponse(presence);
    }

    @Transactional(readOnly = true)
    public List<Presence> listerPourSession(Long sessionId) {
        return presenceRepository.findBySessionId(sessionId);
    }

    /**
     * Issue #28 : le contrôle {@code existsBySessionIdAndEtudiantId} ci-dessus
     * n'est pas atomique avec cet enregistrement. Sous deux requêtes vraiment
     * concurrentes sur le même (session, étudiant), la seconde peut passer le
     * contrôle avant que la première ne commite ; elle percute alors la
     * contrainte unique {@code uk_presence_session_etudiant} en base. On la
     * traduit ici en 409 DEJA_PRESENT au lieu de laisser fuiter une 500.
     */
    private Presence enregistrerOuDejaPresent(Long sessionId, Long etudiantId, SourcePresence source) {
        Presence presence = new Presence();
        presence.setSessionId(sessionId);
        presence.setEtudiantId(etudiantId);
        presence.setSource(source);
        presence.setMarqueeAt(Instant.now());
        try {
            return presenceRepository.saveAndFlush(presence);
        } catch (DataIntegrityViolationException e) {
            throw ApiException.dejaPresent();
        }
    }

    private PresenceResponse toResponse(Presence presence) {
        return new PresenceResponse(presence.getId(), presence.getSessionId(), presence.getEtudiantId(), presence.getSource());
    }
}
