# Cahier des charges — PresenceKF

**Auteur :** DONGMO Erica · KF48-171
**Version :** 1 · **Date :** <25/09/2026>
**Frontend choisi :** Next.js, parce que c'est un framework React qui apporte nativement le routage par fichiers (les trois écrans — formateur, étudiant, relecteur — deviennent trois dossiers dans `app/`), le rendu serveur et les Server Components pour les écrans qui lisent des données (le tableau formateur), et une structure de projet déjà cadrée qui évite de perdre du temps à choisir une architecture en pleine épreuve. Le build (`next build`) est vérifiable en une commande, ce qui répond directement à la contrainte F1.

---

## 1. Contexte et objectif

La direction de la formation KFOKAM48 gère aujourd'hui les présences et les exercices de ses promotions à la main : feuilles de présence papier, exercices rendus par mail ou messagerie, relectures attribuées de bouche à oreille. Résultat : des présences contestées, des exercices perdus, des moyennes recalculées à la calculatrice, et aucune traçabilité.

L'application **PresenceKF** répond à ce problème en centralisant le cycle d'une séance de cours : un formateur ouvre une session et obtient un code de présence à durée limitée ; les étudiants marquent leur présence avec ce code ; ils déposent le lien de leur exercice ; le système assigne chaque exercice à un pair relecteur qui rend une note et un commentaire ; le formateur consulte un tableau récapitulatif par promotion.

L'objectif n'est pas de remplacer un ENT complet, mais de couvrir le strict besoin exprimé par la direction : présence fiable, dépôt traçable, relecture par les pairs, tableau de bord formateur.

---

## 2. Acteurs et rôles

| Acteur | Ce qu'il peut faire | Ce qu'il ne peut pas faire |
|---|---|---|
| **Formateur** | Ouvrir une session et obtenir un code de présence (Q2) ; consulter le tableau de sa promotion (Q16) ; ajouter une présence manuellement avec `source = FORMATEUR` (Q14) ; clôturer une session | Marquer une présence à la place d'un étudiant sans que cela soit visible ; relire un exercice ; modifier une note de relecture |
| **Étudiant** | Choisir son nom dans une liste, sans mot de passe (Q1) ; marquer sa présence avec un code (Q2) ; déposer puis remplacer le lien de son exercice (Q12, Q13) ; consulter la note et le commentaire reçus, sans le nom du relecteur (Q8) | Relire son propre exercice (Q5) ; voir le nom de son relecteur (Q8) ; marquer sa présence après la fin de la session (Q3) |
| **Relecteur** | Rendre une note entière de 0 à 20 et un commentaire sur l'exercice qui lui a été assigné (Q6, Q9) ; corriger sa note tant que la session n'est pas clôturée (Q10) | Choisir l'exercice qu'il relit (Q7 : assignation automatique) ; relire un exercice dont il est l'auteur (Q5) ; modifier une note après clôture (Q10) |

> **Décision de modélisation :** le **relecteur n'est pas un acteur distinct**, c'est un **étudiant dans un état particulier** : celui d'avoir été assigné à la relecture d'un exercice par le système. Conséquence sur le modèle de données : pas de table `Relecteur`, mais une entité `Relecture` liant un `Etudiant` (le relecteur), un `Exercice` (relu) et portant `note`, `commentaire`, `statut`, `renduAt`. Un même étudiant peut donc être à la fois auteur d'un exercice et relecteur d'un autre.

---

## 3. Périmètre

**Inclus dans cette version :**

- Ouverture de session par le formateur avec génération d'un code de présence à expiration (15 min, Q2).
- Marquage de présence par code, avec source `ETUDIANT` ou `FORMATEUR` (Q14).
- Blocage temporaire après 5 erreurs de code (Q4).
- Dépôt et remplacement du lien d'exercice tant que la relecture n'a pas commencé (Q12, Q13).
- Assignation automatique d'un relecteur parmi les présents à la session (Q6, Q7).
- Relecture : note entière 0–20 + commentaire, modifiable jusqu'à clôture (Q9, Q10).
- Tableau récapitulatif formateur par promotion (Q16).
- Clôture de session par le formateur (déclenche le gel des notes, Q10/Q15).
- Données de démonstration chargées au démarrage.

**Explicitement exclu :**

- Authentification par mot de passe (Q1 : choix dans une liste uniquement).
- Gestion des promotions / comptes (création, édition) — les données sont pré-chargées.
- Upload de fichier : l'étudiant dépose un **lien (URL)** vers son exercice, pas un document.
- Notifications par mail, push ou SMS.
- Export PDF / CSV du tableau.
- Historique des modifications d'une relecture (on garde seulement la dernière version).
- Interface mobile native : seule l'adaptation responsive du web est visée (ENF1).
- Multi-formateurs simultanés sur une même session.
- Suppression d'une session, d'un exercice ou d'une présence.

> Un périmètre sans exclusion n'est pas un périmètre.

---

## 4. Exigences fonctionnelles

| Réf | Exigence | Critère d'acceptation | Priorité |
|---|---|---|---|
| EF1 | Le formateur ouvre une session et obtient un code de présence | Quand je soumets `titre` + `promotionId` valides, alors je reçois `id`, `code`, `ouvertureAt` et `expirationAt` (= ouverture + 15 min) | Must |
| EF2 | L'étudiant marque sa présence avec un code | Quand je saisis un code valide et non expiré, alors ma présence apparaît dans le tableau du formateur avec `source = ETUDIANT` | Must |
| EF3 | Le système refuse un code expiré | Quand je saisis un code dont `expirationAt` est dépassé, alors je reçois `410 { code: "CODE_EXPIRE" }` et aucune présence n'est créée | Must |
| EF4 | Le système refuse une double présence | Quand je soumets un code alors que je suis déjà présent à cette session, alors je reçois `409 { code: "DEJA_PRESENT" }` | Must |
| EF5 | Le formateur peut ajouter une présence manuellement | Quand j'ajoute un étudiant à la main, alors sa présence est enregistrée avec `source = FORMATEUR` et apparaît comme telle dans le tableau | Must |
| EF6 | L'étudiant dépose le lien de son exercice | Quand je soumets `sessionId` + `etudiantId` + `lien` (URL valide) et sans exercice préexistant, alors je reçois `id` et `statut = DEPOSE` | Must |
| EF7 | Le système refuse un second dépôt | Quand je dépose un exercice pour une session où j'en ai déjà un, alors je reçois `409 { code: "EXERCICE_DEJA_DEPOSE" }` | Must |
| EF8 | Le système assigne jusqu'à deux relecteurs distincts *(révisée étape 3, était : « un relecteur unique »)* | Quand un exercice est déposé **et qu'au moins un étudiant présent autre que l'auteur existe**, alors une à deux relectures sont créées avec `statut = EN_ATTENTE`, selon le nombre de candidats disponibles (au plus deux, jamais le même candidat deux fois) | Must |
| EF9 | Chaque relecteur rend sa note indépendamment | Quand je soumets `note` entière 0–20 + `commentaire` sur une relecture qui m'est assignée et non encore rendue, alors je reçois `200` et cette relecture passe `RENDUE` (l'autre relecture du même exercice, s'il y en a une, n'est pas affectée) | Must |
| EF9bis | L'étudiant relu voit une note unique, moyenne des relectures rendues *(ajoutée étape 3)* | Quand je consulte `GET /api/exercices/{id}/note` : si aucune relecture n'est rendue, `statut = EN_ATTENTE` ; si une seule des relectures assignées est rendue, je reçois sa note avec `provisoire = true` ; si toutes les relectures assignées sont rendues, je reçois la moyenne avec `provisoire = false` | Must |
| EF10 | Le relecteur corrige sa note tant que la session est ouverte | Quand je resoumets une note sur une relecture déjà rendue et que la session n'est pas clôturée, alors ma nouvelle note remplace l'ancienne | Must |
| EF11 | Le système interdit l'auto-relecture | Quand je tente de rendre une relecture sur mon propre exercice, alors je reçois `403 { code: "AUTO_RELECTURE" }` | Must |
| EF12 | Le tableau formateur affiche le récapitulatif | Quand je consulte `GET /api/tableau?promotionId=` pour une promotion connue, alors j'obtiens par étudiant : `presences`, `exercicesDeposes`, `moyenne` (nullable), `relecturesEnAttente` | Must |
| EF13 | Le tableau signale les relectures en attente | Quand un relecteur n'a pas rendu, alors `relecturesEnAttente` de l'étudiant concerné est > 0 et visible dans le tableau | Must |
| EF14 | L'étudiant consulte sa note sans voir le relecteur | Quand je consulte la note de mon exercice relu, alors je vois `note` et `commentaire` mais jamais le nom du relecteur | Should |
| EF15 | L'étudiant remplace son lien avant relecture | Quand je resoumets un lien sur un exercice dont la relecture est encore `EN_ATTENTE`, alors le nouveau lien remplace l'ancien | Should |
| EF16 | Le formateur clôture une session | Quand je clôture une session, alors plus aucun dépôt ni marquage n'est accepté et toutes les notes deviennent définitives | Should |
| EF17 | Le système bloque après 5 erreurs de code | Quand un étudiant soumet 5 codes invalides consécutifs, alors toute nouvelle tentative de sa part est refusée pendant 2 minutes | Could |

---

## 5. Exigences non fonctionnelles

| Réf | Exigence | Comment on la vérifie |
|---|---|---|
| ENF1 | L'interface de marquage de présence est utilisable sur un téléphone | Ouverture de l'écran étudiant sur un viewport 375×667 (DevTools mobile), saisie du code et validation réussies sans zoom |
| ENF2 | Le tableau du formateur répond en moins de 2 s pour une promotion de 60 étudiants | Jeu de démo avec 60 étudiants × 10 sessions, mesure au `curl -w "%{time_total}"` sur `GET /api/tableau` |
| ENF3 | Toute réponse d'erreur respecte le format imposé `{ code, message }` | Test d'intégration qui parcourt chaque code d'erreur et vérifie le JSON, aucun 500 non géré |
| ENF4 | Le code de présence est non devinable | Génération via `SecureRandom` sur ≥ 6 caractères alphanumériques, jamais séquentiel |
| ENF5 | L'application démarre chez un tiers en 3 commandes maximum | `git clone` + `docker compose up` (ou 3 commandes documentées) testé depuis un dossier vide sur une machine vierge |
| ENF6 | La base contient des données de démonstration exploitables | Au démarrage : 1 promotion, 1 formateur, ≥ 10 étudiants, 1 session de démo, ≥ 1 exercice déposé, ≥ 1 relecture en attente, ≥ 1 étudiant sans note |
| ENF7 | Aucune entité JPA n'est exposée en JSON | Revue du code : chaque contrôleur renvoie un DTO ; test d'intégration qui vérifie l'absence de champs techniques |

---

## 6. Règles de gestion

| Réf | Règle | Source |
|---|---|---|
| RG1 | Un code de présence expire 15 minutes après l'ouverture de la session | Q2 |
| RG2 | Un étudiant ne peut pas relire son propre exercice | Q5 |
| RG3 | Une note est un entier compris entre 0 et 20 (bornes incluses) | Q9 |
| RG4 | ~~Un exercice n'a qu'un seul relecteur~~ **Révisée (évolution étape 3) : un exercice est relu par deux relecteurs distincts** ; la note retenue est la moyenne des notes rendues, affichée comme provisoire tant que l'un des deux n'a pas rendu | Q6, révisée par le changement de besoin étape 3 |
| RG5 | Les deux relecteurs sont choisis au hasard parmi les étudiants présents à la session, hors auteur, et distincts entre eux | Q7, révisée par le changement de besoin étape 3 |
| RG6 | L'étudiant relu voit sa note et le commentaire, mais jamais le nom du relecteur | Q8 |
| RG7 | La note est modifiable par le relecteur tant que la session n'est pas clôturée | Q10 |
| RG8 | Une relecture non rendue laisse l'exercice en attente et cette attente est visible dans le tableau | Q11 |
| RG9 | Un exercice peut être déposé après la fin de la session, tant qu'elle n'est pas clôturée | Q12 |
| RG10 | Un lien d'exercice est remplaçable tant qu'aucune relecture n'a commencé | Q13 |
| RG11 | Une présence ajoutée manuellement est marquée `source = FORMATEUR` | Q14 |
| RG12 | Une note envoyée devient définitive dès la clôture de la session | Q10 (prime sur Q15) |
| RG13 | Un étudiant ne peut avoir qu'une seule présence par session | Déduit de Q3 |
| RG14 | Un étudiant ne peut avoir qu'un seul exercice par session | Déduit de EF7 |
| RG15 | Cinq soumissions de code invalides consécutives bloquent l'étudiant 2 minutes | Q4 |
| RG16 | Le marquage de présence est refusé après la fin de la session ou après clôture | Q3 |

---

## 7. Zones d'ombre, hypothèses et contradictions

### Points que la demande ne tranche pas

| Point | Réponse client (Qx) ou hypothèse | Décision retenue | Conséquence |
|---|---|---|---|
| Clôture d'une session : qui, quand, comment ? | Aucune question ne la couvre | Le formateur clôture manuellement via `POST /api/sessions/{id}/cloture` (opération libre) | Gel des notes (RG12), arrêt des dépôts (RG9) et marquages (RG16) ; nouvelle opération ajoutée au contrat |
| Étudiant non-présent qui dépose un exercice | Q3 interdit la présence après fin, Q12 autorise le dépôt jusqu'à clôture ; **aucune question ne croise les deux** | Le dépôt est autorisé même sans présence (Q12 prime) | Sa présence reste absente du tableau ; il apparaît avec `exercicesDeposes` ≥ 1 mais `presences` inchangé. Il ne peut pas être choisi comme relecteur (Q7). |
| Aucun étudiant présent pour assigner une relecture | Q7 dit « parmi les présents » sans traiter le cas vide | L'exercice reste `DEPOSE` avec `relecture = null`, listé côté formateur comme « à assigner » | EF8 devient conditionnelle ; le champ `relecture` est nullable |
| Blocage après 5 erreurs : par étudiant ou par IP ? | Q4 ne le précise pas | Par `etudiantId` (choix dans une liste, Q1), fenêtre glissante de 2 minutes | Table `TentativeCode(etudiantId, at, succes)` ; pas de dépendance IP |
| Format du code de présence | Q2 parle de durée, pas de format | 6 caractères alphanumériques majuscules, `SecureRandom` | ENF4 |
| « Relecture commencée » (Q13) : c'est quoi ? | Q13 utilise l'expression sans la définir | « Commencée » = relecture passée au statut `EN_COURS` (déclenché à l'ouverture de l'écran relecteur) | Statut `EN_COURS` ajouté sur `Relecture` ; seul `EN_ATTENTE` autorise le remplacement de lien (RG10) |
| Moyenne d'un étudiant sans note reçue | Q16 parle de « moyenne » sans préciser le cas vide | `null` (jamais 0), aligné sur le contrat YAML | Frontend affiche « — » ; ENF6 fournit au moins un étudiant sans note dans la démo |
| Comportement quand une note hors 0–20 non entière est envoyée | Q9 fixe le domaine | Refus `400 { code: "NOTE_INVALIDE" }` | RG3 |

### Contradictions relevées

| Réponses en conflit | Ce que j'ai choisi | Pourquoi |
|---|---|---|
| **Q10** : « le relecteur peut corriger tant que le formateur n'a pas clôturé » **vs Q15** : « la note est définitive une fois envoyée, il ne peut plus y revenir » | **Q10 prime** (RG7, RG12) : la note est modifiable jusqu'à la clôture, puis figée | Q10 décrit un cas d'usage réel et daté (avant clôture), Q15 énonce une intention générale sans borne temporelle. La clôture est le seul instant de gel, cohérent avec Q3 et Q12. |
| **Annexe B** : `POST /api/lectures/{id}` **vs `api/contrat.yaml`** : `POST /api/relectures/{id}` | **Le YAML prime** (`/api/relectures/{id}`) | Le sujet qualifie l'annexe B d'« extrait lisible » ; le contrat YAML est la source de vérité technique. |

> Une hypothèse écrite est toujours acceptée. Une hypothèse silencieuse est une faute.

### Évolution étape 3 — deux relecteurs par exercice (changement de besoin)

Le client est revenu après la version 0.1 : un relecteur unique laisse l'étudiant sans note quand ce relecteur ne rend rien. Nouvelle règle (RG4/RG5 révisées ci-dessus) : **deux relecteurs distincts par exercice**, note retenue = moyenne des notes rendues, affichée comme **provisoire** tant que l'un des deux n'a pas rendu.

**Ce que la demande ne tranche pas et que j'ai dû décider :**

| Point | Décision retenue | Conséquence |
|---|---|---|
| Moins de deux candidats présents au moment de l'assignation (0 ou 1) | On assigne autant que possible (0, 1 ou 2), sans attendre | Un exercice peut rester avec un seul relecteur assigné en permanence ; sa note, une fois rendue, est définitive (pas de second relecteur à attendre), voir sacrifice ci-dessous |
| Un étudiant devient présent *après* l'assignation initiale (encore un seul relecteur assigné) | Pas de rattrapage automatique | Sacrifice explicite (périmètre), voir `docs/JOURNAL.md` |
| Exercices déjà relus avant cette évolution (un seul relecteur, note déjà rendue) | Conservés tels quels, non re-migrés vers deux relecteurs | Sacrifice explicite (périmètre), voir `docs/JOURNAL.md` |
| Arrondi de la moyenne des deux notes | Non arrondie côté API (`number`), arrondie à l'affichage frontend uniquement | Cohérent avec ENF7 : la règle de calcul reste unique, côté API |

**Sacrifice de périmètre pour absorber ce Must arrivé tard :** voir `docs/JOURNAL.md`, section Étape 3.

---

## 8. Contraintes techniques

**Contraintes imposées par le sujet :**

| # | Contrainte |
|---|---|
| B1 | Java 17+, Maven, wrapper `mvnw` commité |
| B2 | `api/contrat.yaml` respecté à la lettre : chemins, verbes, codes de statut, format d'erreur |
| B3 | Séparation contrôleur / service / repository, aucune requête base dans un contrôleur, aucune entité JPA exposée en JSON (DTO) |
| B4 | Validation des entrées + `@RestControllerAdvice`, aucune stack trace renvoyée au client |
| B5 | Schéma versionné par **Flyway**, migrations commitées, `ddl-auto=update` interdit hors tests |
| B6 | Un test unitaire sur une règle métier réelle + un test d'intégration sur un endpoint, exécutables sur un poste vierge |
| F1 | Frontend déclaré et justifié en une ligne dans le README, build qui passe |
| F2 | Trois écrans : formateur, étudiant, relecteur |
| F3 | Couche d'appels API dédiée, états de chargement et d'erreur gérés, aucune règle métier dupliquée (la moyenne vient de l'API) |

**Contraintes que je m'impose :**

| # | Contrainte | Raison |
|---|---|---|
| C1 | Base **PostgreSQL** via Docker Compose, profil `test` avec H2 en mémoire | Reproductibilité et démarrage en une commande |
| C2 | Migrations Flyway nommées `V1__init.sql`, `V2__...` ; toute évolution de l'étape 3 passe par une **nouvelle migration**, jamais par une modification de `V1` | Exigence explicite du sujet (étape 3) |
| C3 | Tests : JUnit 5 + Spring Boot Test ; `@DataJpaTest` interdit pour les règles métier (test unitaire pur sur le service) | B6 |
| C4 | Un `@RestControllerAdvice` unique mappe chaque exception métier vers `{ code, message }` et le code HTTP du contrat | B4 + ENF3 |
| C5 | Aucun secret commité, `.env.example` fourni | Malus −5 |

---

## 9. Livrables

- `docs/CAHIER_DES_CHARGES.md` (ce document) — 10 sections.
- `docs/diagrammes/D1-cas-utilisation.md`, `D2-classes.md`, `D3-sequence-presence.md` (+ bonus `D4-etats-exercice.md`).
- `docs/JOURNAL.md` — une entrée par étape.
- `api/contrat.yaml` — 5 opérations imposées + opérations libres (clôture, ajout manuel de présence, consultation note).
- `backend/` — Spring Boot, Flyway, `mvnw`, tests.
- `frontend/` — **Next.js**, trois écrans (formateur, étudiant, relecteur).
- `CHANGELOG.md`, `README.md` d'installation testé depuis un clone vierge.
- `SOUMISSION.md` téléversé sur la plateforme avant 18h00.
- Dépôt public **`kfokam48-epreuve-171`** + dépôt public **`kfokam48-gitlab-171`**.

---

## 10. Démarche prévue

| Étape | Objectif | Jalon |
|---|---|---|
| 1 — Analyser, spécifier, concevoir | Cahier des charges, diagrammes, backlog, contrat complété — **aucun code** | `[JALON] analyse` |
| 2 — Première version | Stories Must uniquement, branche + PR par ticket | `[JALON] v0.1` |
| 3 — Enveloppe | Bug + changement de besoin → issue, repro, migration, contrat, re-priorisation | commit dédié de mise à jour |
| 4 — Version finale | CHANGELOG, README testé, démo, backlog trié | `[JALON] v1.0` |
| 5 — Épreuve Git | Bundle git-lab, 5 situations, second dépôt public `kfokam48-gitlab-171` | — |
| 6 — Soumettre | `SOUMISSION.md` + hashes sur la plateforme | avant 18h00 |

**Plan de repli en cas de retard :** je sacrifie d'abord `Could` (EF17), puis les `Should` (EF14–EF16), en gardant impérativement les `Must` (EF1–EF13) et la conformité au contrat. Je préfère livrer 9 Must solides avec un historique propre qu'un produit large et un dépôt illisible. Les sacrifices sont notés dans `JOURNAL.md` (étape 3) et dans le `CHANGELOG`.

**Definition of Done — un ticket est terminé quand :**

1. Le comportement décrit par ses critères d'acceptation est vérifiable manuellement ou par un test.
2. Le code est sur une branche dédiée, mergé par PR liée à l'issue (`Closes #N`).
3. Les règles de gestion citées (`RGx`) sont couvertes par au moins un test.
4. La CI locale passe : `./mvnw test` côté backend, `npm run build` côté frontend (Next.js).
5. Le contrat d'API est respecté : chemin, verbe, code de statut, format d'erreur.
6. Le `JOURNAL.md` est mis à jour si l'étape est terminée.

---

## Journal des révisions

| Version | Quand | Ce qui a changé et pourquoi |
|---|---|---|
| 1 | <25/09/2026 — date du jour> | Version initiale après analyse de `CLIENT.md` et `api/contrat.yaml`. Contradiction **Q10/Q15** tranchée en faveur de **Q10** ; divergence **Annexe B / YAML** tranchée en faveur du **YAML** ; trou **Q3/Q12** comblé (dépôt autorisé sans présence) ; trois autres zones d'ombre documentées (clôture, absence de relecteur disponible, définition de « relecture commencée ») ; moyenne nullable alignée sur le contrat. 
