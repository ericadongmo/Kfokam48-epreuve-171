-- Évolution étape 3 (enveloppe, changement de besoin) : un exercice n'est plus
-- relu par un relecteur unique (contrainte UNIQUE sur relecture.exercice_id,
-- V1__init.sql) mais par jusqu'à DEUX relecteurs distincts. La note retenue
-- devient la moyenne des relectures rendues (calculée applicativement, aucune
-- colonne dédiée ici).
--
-- V1 et V2 ne sont pas modifiées (contrainte C2) : on recrée la table
-- relecture avec la nouvelle contrainte d'unicité, on copie les lignes
-- existantes telles quelles, puis on bascule le nom. Les relectures déjà
-- rendues avant cette migration survivent inchangées (sacrifice de périmètre
-- assumé : elles ne sont pas rétroactivement complétées d'un second
-- relecteur, cf. docs/JOURNAL.md section Étape 3).

CREATE TABLE relecture_v3 (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    exercice_id   BIGINT NOT NULL,
    relecteur_id  BIGINT NOT NULL,
    note          INTEGER,
    commentaire   VARCHAR(4000),
    statut        VARCHAR(20) NOT NULL,
    assignee_at   TIMESTAMP NOT NULL,
    rendu_at      TIMESTAMP,
    CONSTRAINT fk_relecture_v3_exercice FOREIGN KEY (exercice_id) REFERENCES exercice (id),
    CONSTRAINT fk_relecture_v3_relecteur FOREIGN KEY (relecteur_id) REFERENCES etudiant (id),
    CONSTRAINT ck_relecture_v3_note CHECK (note IS NULL OR (note >= 0 AND note <= 20)),
    CONSTRAINT uk_relecture_v3_exercice_relecteur UNIQUE (exercice_id, relecteur_id)
);

-- Pas de FK externe vers relecture.id : on laisse l'identité se régénérer
-- (évite le piège déjà rencontré à l'étape 2 où des id explicites laissaient
-- la séquence IDENTITY en retard, cf. docs/JOURNAL.md étape 2).
INSERT INTO relecture_v3 (exercice_id, relecteur_id, note, commentaire, statut, assignee_at, rendu_at)
SELECT exercice_id, relecteur_id, note, commentaire, statut, assignee_at, rendu_at FROM relecture;

DROP TABLE relecture;
ALTER TABLE relecture_v3 RENAME TO relecture;

CREATE INDEX idx_relecture_relecteur ON relecture (relecteur_id);
CREATE INDEX idx_relecture_exercice ON relecture (exercice_id);
