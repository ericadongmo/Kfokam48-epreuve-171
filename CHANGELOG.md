# Changelog — PresenceKF

Format inspiré de [Keep a Changelog](https://keepachangelog.com/fr/).

## [Non publié] — Étape 3 : enveloppe (correctif + évolution)

### Corrigé

- **Issue #28** — Deux marquages de présence quasi simultanés sur le même
  (session, étudiant) pouvaient produire une `500 ERREUR_INTERNE` au lieu du
  `409 DEJA_PRESENT` attendu : le contrôle d'unicité et l'insertion n'étaient
  pas atomiques, et la violation de contrainte concurrente n'était pas
  interceptée. Reproduit par un test qui échoue (`PresenceConcurrenceIT`)
  avant d'être corrigé dans `PresenceService.enregistrerOuDejaPresent`
  (PR #29, branche `fix/28-course-marquage-presence-concurrent`).

### Ajouté — Évolution (changement de besoin, issue #30)

- **Deux relecteurs par exercice** (RG4/RG5 révisées) : `RelectureAssignmentService`
  tire désormais jusqu'à deux relecteurs distincts, hors auteur, parmi les
  présents.
- **Note retenue = moyenne des relectures rendues** (EF9bis) ; `GET
  /api/exercices/{id}/note` renvoie `note` en `number` (plus `integer`) et un
  nouveau champ `provisoire` (`true` tant que l'une des deux relectures
  assignées n'a pas rendu).
- Migration Flyway additive `V3__deux_relecteurs.sql` : remplace l'unicité
  `relecture.exercice_id` (un seul relecteur) par l'unicité du couple
  `(exercice_id, relecteur_id)`, sans toucher `V1`/`V2`, données existantes
  préservées.
- Écran étudiant : mention « (provisoire) » et bandeau explicatif tant que
  la note n'est pas définitive.
- **Sacrifice de périmètre** (voir `docs/JOURNAL.md`, étape 3) : les
  exercices déjà relus avant cette évolution ne sont pas rétro-migrés vers
  deux relecteurs ; pas de rattrapage automatique si un second candidat
  devient présent après l'assignation initiale.
- Branche et PR séparées de celles du correctif #28, conformément à
  l'enveloppe.

## [0.1.0] — Première version (Must + Should + Could)

### Ajouté

- Backend Spring Boot 3 / Java 17, wrapper `mvnw` commité, base H2 en
  mémoire, migrations Flyway (`V1__init.sql` conforme à D2,
  `V2__donnees_demo.sql` — ENF6).
- API conforme à `api/contrat.yaml` : les 5 opérations imposées, plus les
  opérations libres (clôture de session, ajout manuel de présence,
  consultation de note, résolution de session par code, remplacement de
  lien, listes promotions/étudiants/relectures).
- Frontend Next.js avec les trois écrans imposés (F2) : formateur, étudiant,
  relecteur.
- Toutes les exigences fonctionnelles Must (EF1-EF13, issues #1 à #9),
  Should (EF14-EF16, issues #10 à #12) et Could (EF17, issue #13).
- Tests : unitaires sur RG2/RG4/RG5 (tirage du relecteur) et RG15 (blocage
  après 5 échecs), intégration bout en bout sur le parcours complet et sur
  le format d'erreur imposé (B6).

### Corrigé pendant le développement

- La migration de données de démo insérait des identifiants explicites sans
  faire redémarrer les séquences `IDENTITY` de H2 : la première session
  créée par l'application entrait en collision avec l'id 1 des données de
  démo (`PRIMARY KEY violation`). Corrigé par des `ALTER TABLE ... RESTART
  WITH` à la fin de `V2__donnees_demo.sql`.
- Le blocage après 5 codes invalides (RG15) ne se déclenchait jamais : la
  méthode `@Transactional` qui enregistre l'échec était annulée par le
  rollback provoqué par l'`ApiException` levée juste après. Corrigé en
  isolant l'enregistrement dans sa propre transaction
  (`Propagation.REQUIRES_NEW`), avec un test d'intégration dédié pour éviter
  la régression (un test unitaire à base de mocks ne pouvait pas voir ce
  bug, propre au comportement transactionnel réel).

### Décisions documentées (voir `docs/CAHIER_DES_CHARGES.md` section 7)

- `RELECTURE_DEJA_RENDUE` (409) est réservé à
  `POST /api/relectures/{id}/commencer` ; sur `POST /api/relectures/{id}`,
  la correction d'une note déjà rendue est autorisée tant que la session
  n'est pas clôturée (RG7/RG12, Q10 prime sur Q15).
- `relecteurId` est un champ additionnel (toléré par le schéma OpenAPI) sur
  `POST /api/relectures/{id}` : l'application n'a pas d'authentification,
  c'est le seul moyen pour le serveur de vérifier RG2 (auto-relecture) et
  l'identité de l'appelant.
