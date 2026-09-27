# PresenceKF — KF48-171

Présences, dépôt d'exercices et relecture par les pairs pour une promotion
KFOKAM48. Voir `docs/CAHIER_DES_CHARGES.md` pour le contexte complet, les
exigences (EF1-EF17, ENF1-ENF7) et les règles de gestion (RG1-RG16).

**Frontend choisi : Next.js.** Framework React qui apporte nativement le
routage par fichiers (les trois écrans — formateur, étudiant, relecteur —
sont trois dossiers dans `frontend/app/`), une structure de projet déjà
cadrée, et un `next build` vérifiable en une commande (contrainte F1).

## Démarrer depuis un clone vierge (ENF5 — 3 commandes)

Prérequis : Java 17+, Node.js 18+. Aucune base de données à installer : le
backend utilise H2 en mémoire, rechargée avec des données de démo à chaque
démarrage (ENF6).

```bash
# 1. Backend (port 8080)
cd backend && ./mvnw spring-boot:run

# 2. Frontend — dans un second terminal
cd frontend && npm install

# 3.
npm run dev
```

Le frontend est servi sur http://localhost:3000, le backend sur
http://localhost:8080. Copiez `frontend/.env.example` en `frontend/.env.local`
si vous changez le port du backend (`NEXT_PUBLIC_API_URL`).

## Données de démonstration (ENF6)

Au démarrage, Flyway charge une promotion, un formateur, 12 étudiants, une
session ouverte (code `DEMO01`, valable 15 minutes après le démarrage), un
exercice avec une relecture en attente, un exercice déjà relu (note 16/20),
et plusieurs étudiants sans aucune note.

## Structure du dépôt

```
backend/    Spring Boot 3 (Java 17), Maven, Flyway, H2 — voir backend ci-dessous
frontend/   Next.js (App Router), trois écrans (F2)
api/        api/contrat.yaml — contrat OpenAPI, imposé + opérations libres
docs/       Cahier des charges, diagrammes (D1-D4), journal de bord
```

## Backend

```bash
cd backend
./mvnw test              # tests unitaires + intégration (B6)
./mvnw spring-boot:run    # démarrage sur :8080
```

- Séparation contrôleur / service / repository, aucune requête base dans un
  contrôleur, DTO uniquement en sortie des contrôleurs (B3, ENF7).
- Toute erreur passe par un `@RestControllerAdvice` unique et respecte le
  format imposé `{ code, message }`, jamais de stack trace (B4, ENF3).
- Schéma versionné par Flyway (`V1__init.sql`, `V2__donnees_demo.sql`,
  `V3__deux_relecteurs.sql`), `ddl-auto=validate` (B5).
- `api/contrat.yaml` respecté à la lettre pour les 5 opérations imposées,
  plus les opérations libres nécessaires aux trois écrans (clôture, ajout
  manuel de présence, consultation de note, listes promotions/étudiants,
  résolution de session par code, remplacement de lien, relectures d'un
  étudiant).

## Frontend

```bash
cd frontend
npm run dev      # développement, :3000
npm run build     # build de production (F1)
npm run lint
```

- Couche d'appels API dédiée dans `frontend/lib/api.ts` : tous les
  composants passent par là, jamais de `fetch` direct dans un écran (F3).
- États de chargement et d'erreur gérés sur chaque écran ; la moyenne
  affichée sur le tableau formateur vient toujours de l'API, jamais
  recalculée côté frontend (F3).
- Interface responsive, testée sur un viewport 375×667 : saisie et
  validation du code de présence sans zoom (ENF1).

## Limites connues et hors périmètre

Voir `docs/CAHIER_DES_CHARGES.md` section 3 (« Périmètre ») et section 7
(« Zones d'ombre, hypothèses et contradictions ») pour les décisions
documentées : dépôt d'exercice sans présence autorisé, clôture manuelle par
le formateur, comportement sans relecteur disponible, etc.
