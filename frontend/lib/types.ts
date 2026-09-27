export type SourcePresence = "ETUDIANT" | "FORMATEUR";
export type StatutExercice = "DEPOSE" | "EN_ATTENTE" | "RELU";
export type StatutRelecture = "EN_ATTENTE" | "EN_COURS" | "RENDUE";

export interface Promotion {
  id: number;
  nom: string;
}

export interface Etudiant {
  id: number;
  nom: string;
  promotionId: number;
}

export interface SessionCours {
  id: number;
  titre: string;
  promotionId: number;
  code: string;
  ouvertureAt: string;
  expirationAt: string;
  cloturee: boolean;
}

export interface Presence {
  id: number;
  sessionId: number;
  etudiantId: number;
  source: SourcePresence;
}

export interface PresenceAvecNom {
  etudiantId: number;
  nom: string;
  source: SourcePresence;
}

export interface Exercice {
  id: number;
  statut: StatutExercice;
}

export interface Relecture {
  id: number;
  statut: StatutRelecture;
}

export interface NoteExercice {
  statut: StatutRelecture;
  note: number | null;
  commentaire: string | null;
  /** Évolution étape 3 : true tant que l'une des deux relectures assignées n'a pas rendu. */
  provisoire: boolean;
}

export interface RelectureAssignee {
  relectureId: number;
  exerciceId: number;
  lien: string;
  statut: StatutRelecture;
  note: number | null;
  commentaire: string | null;
}

export interface LigneTableau {
  etudiantId: number;
  nom: string;
  presences: number;
  exercicesDeposes: number;
  moyenne: number | null;
  relecturesEnAttente: number;
}

export interface Cloture {
  id: number;
  cloturee: boolean;
  clotureAt: string;
}
