# Journal de bord — <matricule>

> Une entrée **par étape**, écrite **au moment où tu la termines**, pas à la fin de la journée.
> Trois lignes suffisent. Un journal rédigé d'un bloc juste avant de soumettre se repère
> immédiatement dans l'historique Git et ne compte pas.

Chaque entrée répond aux trois mêmes questions :

- **Fait** — ce que tu viens de terminer
- **Bloqué** — ce qui t'a coûté du temps, et combien
- **IA** — ce que tu lui as demandé, et **comment tu as vérifié sa réponse**

---

## Étape 1 — Analyse et conception

**Fait :** cahier des charges (9 exigences fonctionnelles, 12 règles de gestion), les trois diagrammes en Mermaid, 11 issues créées, contrat d'API complété, commit `[JALON] analyse` poussé.

**Bloqué :** 12 min sur la contradiction entre Q10 et Q15. Tranchée en faveur de Q10 : Q11 décrit un usage réel et concret du formateur, Q15 n'est qu'une intention générale. Noté en section 7.

**IA :** m'a proposé un découpage en 18 tickets, j'en ai retenu 11. Les autres étaient des tâches techniques (« créer l'entité », « configurer Flyway »), pas des résultats utilisateur. Vérifié en relisant chaque titre : est-ce que le client le comprendrait ?

---

## Étape 2 — Première version

**Fait :** backend Spring Boot complet (13 issues, Must à Could) conforme à `api/contrat.yaml`, frontend Next.js avec les trois écrans, tests unitaires + intégration verts, `npm run build` et `./mvnw test` passent.

**Bloqué :** ~20 min sur deux bugs trouvés en testant l'API réelle après le build (pas visibles en tests unitaires mockés) : (1) les id explicites de `V2__donnees_demo.sql` ne faisaient pas avancer les séquences IDENTITY de H2, la première session créée entrait en collision avec l'id 1 de démo ; (2) le blocage RG15 après 5 codes invalides ne se déclenchait jamais car le rollback transactionnel de l'`ApiException` annulait l'enregistrement de l'échec lui-même. Corrigés, tests de non-régression ajoutés (voir `CHANGELOG.md`).

**IA :** a généré l'implémentation initiale des 13 issues (backend + frontend) à partir du cahier des charges et du contrat déjà écrits à l'étape 1. Vérifié par : relecture du contrat un endpoint à la fois, exécution de la suite de tests, puis un parcours API complet à la main (`curl`) rejouant chaque critère d'acceptation des 13 issues sur le backend réellement démarré — c'est ce parcours, pas les tests unitaires, qui a révélé les deux bugs ci-dessus. Écran par écran vérifié par rendu serveur (`curl` sur chaque route) et `npm run build`/`lint` ; pas de clic-à-clic dans un vrai navigateur, aucun outil de navigateur n'étant disponible dans cet environnement — à refaire manuellement avant la démo.

---

## Étape 3 — Enveloppe

**Fait :** (1) Bug de concurrence sur le marquage de présence : issue #28 ouverte avec la reproduction avant tout code, test `PresenceConcurrenceIT` rouge (`[201, 500]`) commité seul, puis correctif (`PresenceService.enregistrerOuDejaPresent` intercepte la violation de contrainte unique concurrente et la traduit en `409 DEJA_PRESENT`) dans un second commit, branche `fix/28-...`, PR #29 mergée. (2) Évolution « deux relecteurs » : issue #30, cahier des charges et D2 mis à jour dans un commit dédié *avant* le code, `api/contrat.yaml` mis à jour, migration additive `V3__deux_relecteurs.sql` (V1/V2 non touchées), `RelectureAssignmentService` / `RelectureService.noteDeExercice` / `ExerciceService.remplacerLien` adaptés, écran étudiant affiche la mention provisoire, tests unitaires (`RelectureAssignmentServiceTest`) et d'intégration (`RelectureDeuxRelecteursIT`) verts, branche `evolution/30-...` séparée du correctif.

**Bloqué :** ~15 min à chercher la vraie cause technique derrière le témoignage du client (« deux étudiants tapent le code en même temps, un seul apparaît ») : `PresenceService.marquerParCode` n'a pas de mutable state partagé ni de section critique évidente entre deux étudiants différents ; la traduction retenue est le TOCTOU classique `exists` puis `save` sur la contrainte unique `(session, étudiant)`, démontré par un vrai test à deux threads plutôt que supposé.

**IA :** m'a aidé à explorer le code service par service pour localiser une race condition plausible et cohérente avec le récit client, à écrire le test de concurrence à deux threads (`ExecutorService`, assertions sur les statuts HTTP exacts `[201, 409]`), et à concevoir la migration `V3` en évitant le piège déjà rencontré à l'étape 2 (id explicites qui font retarder la séquence IDENTITY) — vérifié en exécutant réellement Flyway contre H2 (`./mvnw -Dtest=RelectureDeuxRelecteursIT test`) et en lisant les logs de migration (« Successfully applied 3 migrations »), pas en se fiant à la lecture du SQL seule.

**Ce que j'ai sorti du périmètre pour absorber le changement, et pourquoi :** (1) les exercices déjà relus avant cette évolution gardent leur note historique à un seul relecteur, sans rattrapage rétroactif — remigrer l'historique aurait exigé d'inventer un second relecteur a posteriori, ce qui n'a pas de sens métier (la relecture par les pairs présents à l'époque ne peut pas être reconstituée). (2) si moins de deux candidats sont présents au moment de l'assignation, aucun rattrapage automatique n'a lieu si un étudiant devient présent plus tard : construire ce mécanisme (écouter les nouvelles présences, réévaluer les exercices en attente) est un vrai morceau de travail que je préfère documenter comme dette plutôt que bâcler sous la pression du temps de l'épreuve.

---

## Étape 4 — Version finale

**Fait :** Vérifié que le changement de besoin étape 3 (deux relecteurs, moyenne, note provisoire) est bien implémenté backend **et** frontend (déjà fait à l'étape 3, PR #31). `CHANGELOG.md` mis à jour et la section étape 3 étiquetée `[1.0.0]`. Testé le README depuis un **vrai clone vierge** (`git clone` dans `/tmp`, pas le dépôt de travail) : backend démarré avec `./mvnw spring-boot:run`, les 3 migrations Flyway (`V1`, `V2`, `V3`) s'appliquent dans l'ordre sur une base H2 neuve, `GET /api/tableau?promotionId=1` répond `200` avec les données de démo (12 étudiants, une moyenne à 16.0 déjà présente). Jalon `[JALON] v1.0` pointé après ce commit.

**Bloqué :** ~20 min sur `npm install` dans le clone vierge : le registre npm configuré par défaut sur cette machine (`registry.npmmirror.com`) ne répond pas dans ce bac à sable (`npm ping` en timeout), et même en forçant `--registry https://registry.npmjs.org/` (qui répond à un `curl` direct), l'installation reste bloquée sans progrès mesurable — limite réseau/E-S du sandbox de développement, pas un défaut du projet. Contourné en copiant le `node_modules` déjà installé du dépôt de travail vers le clone après avoir vérifié que `package-lock.json` est **strictement identique** (`diff` vide) : cette étape a elle-même échoué deux fois à cause d'un `cp` interrompu par la lenteur d'E/S de `/tmp` sur cette machine (copie partielle avec un `node_modules/node_modules` imbriqué). Faute de temps pour fiabiliser cette copie, je m'appuie sur une preuve déjà solide et antérieure dans cette même session : `npm run build` a réussi sur le dépôt de travail avec ce même `package-lock.json`, juste après les modifications frontend de l'étape 3 (écran étudiant, mention provisoire). Ce n'est pas équivalent à un `npm install` frais vérifié en direct sur le clone, et je le note comme tel plutôt que de prétendre l'avoir fait.

**IA :** m'a aidé à diagnostiquer que le port 8080 était déjà occupé par un service Apache préexistant sur cette machine (rien à voir avec le projet), vérifié avec `ss -ltn` et un `curl` qui a renvoyé une page d'erreur Apache plutôt que du JSON — d'où le choix d'un port alternatif (`--server.port=18080`) uniquement pour ce test, sans toucher au README (le port 8080 par défaut est correct sur une machine réellement vierge).

---

## Étape 5 — Épreuve Git

**Fait :**

**Bloqué :**

**IA :**

---

## Étape 6 — Soumission

**Fait :**

**Ce que je referais autrement avec une journée de plus :**
