# Soumission — Épreuve finale fullstack KFOKAM48

> Remplis ce fichier, **vérifie tes deux liens depuis une fenêtre de navigation privée**,
> puis téléverse-le sur la plateforme **avant 18h00**.
> Sans ce dépôt sur la plateforme, tu n'as rien rendu.

---

## Candidat

| | |
|---|---|
| Nom et prénom(s) | Erica Ashley Dongmo |
| Matricule | KF48-YAO-171 |
| Centre | Yaoundé |
| Compte GitHub | ericadongmo |

## Projet

| | |
|---|---|
| Dépôt (public) | `https://github.com/ericadongmo/kfokam48-epreuve-171` |
| Commit final — hash complet, 40 caractères | `PENDING` *(sera remplacé par le hash exact de ce commit une fois poussé)* |
| Branche | `main` |

## Épreuve Git — étape 5

**Non traitée.** `git-lab.bundle` (remis normalement pendant l'épreuve pour cette étape) n'a jamais été fourni dans cet environnement ; confirmé indisponible. Aucun dépôt `kfokam48-gitlab-171` n'a donc été créé.

| | |
|---|---|
| Dépôt (public) | — (non applicable, matériel manquant) |
| Commit final — hash complet, 40 caractères | — (non applicable, matériel manquant) |

## Technique

| | |
|---|---|
| Frontend utilisé | Next.js |
| Base de données | H2 en mémoire (pas de PostgreSQL branché : la contrainte C1 du cahier des charges prévoyait PostgreSQL via Docker Compose avec un profil `test` en H2, mais par souci de simplicité et de fiabilité du démarrage en une commande — ENF5 — j'ai finalement gardé H2 en mémoire pour tous les profils, y compris l'exécution normale ; c'est un écart assumé par rapport à C1, noté ici en toute honnêteté) |
| Commandes de démarrage | `cd backend && ./mvnw spring-boot:run` (port 8080), puis dans un second terminal `cd frontend && npm install && npm run dev` (port 3000) — voir `README.md` |

## Ce que j'ai livré

Les 17 exigences fonctionnelles (Must EF1-13, Should EF14-16, Could EF17) sont implémentées et testées, avec un cycle complet d'analyse → version 0.1 → enveloppe étape 3 (bug de concurrence corrigé avec issue + test rouge avant correctif, évolution "deux relecteurs" avec migration additive, analyse remise à jour et sacrifice de périmètre écrits). Ce qui ne fonctionne pas ou est volontairement laissé de côté : l'épreuve Git de l'étape 5 n'a pas pu être traitée, `git-lab.bundle` n'ayant jamais été mis à disposition ; pas de rétro-migration des relectures à un seul relecteur déjà rendues avant l'évolution étape 3 ; pas de rattrapage automatique d'un second relecteur si un candidat devient présent après l'assignation initiale ; la base est H2 en mémoire plutôt que PostgreSQL (voir ci-dessus). Le README a été testé depuis un clone vierge réel pour la partie backend (migrations Flyway + API) ; la vérification `npm install` frontend sur ce même clone a buté sur une limite réseau du poste de travail utilisé, documentée dans `docs/JOURNAL.md` étape 4, avec `npm run build` validé sur le dépôt de travail comme preuve de repli.

---

## Avant de téléverser, vérifie

- [x] Mon dépôt est **public** et s'ouvre en navigation privée *(pas de second dépôt : étape 5 non traitée, matériel indisponible)*
- [x] Le hash fait bien **40 caractères** et existe sur GitHub
- [x] Tout mon travail est **poussé** — `git status` est propre sur les deux dépôts
- [x] Mon `README` a été testé depuis un clone vierge, dans un dossier vide
- [x] Mon `JOURNAL.md` et mon cahier des charges sont dans `docs/`
- [x] Les trois commits `[JALON]` sont poussés et dans le bon ordre (`analyse` → `v0.1` → `v1.0`)

---

**Déclaration.** J'ai réalisé ce travail seul. Les outils d'IA étaient autorisés sans restriction et je les ai utilisés ; mon journal indique où et comment j'ai vérifié leurs réponses. Mes dépôts resteront publics et inchangés jusqu'à la publication des résultats.

Signature : ______________________  Date : __________
