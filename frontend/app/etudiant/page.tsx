"use client";

import { useEffect, useState } from "react";
import { api, ApiError } from "@/lib/api";
import type { Etudiant, Exercice, NoteExercice, SessionCours } from "@/lib/types";
import Banniere from "@/components/Banniere";
import PromotionEtudiantPicker from "@/components/PromotionEtudiantPicker";

const LIBELLES_STATUT: Record<string, string> = {
  DEPOSE: "Déposé, en attente d'assignation d'un relecteur",
  EN_ATTENTE: "Déposé, relecture en attente",
  RELU: "Relu",
};

export default function EcranEtudiant() {
  const [etudiant, setEtudiant] = useState<Etudiant | null>(null);

  const [code, setCode] = useState("");
  const [session, setSession] = useState<SessionCours | null>(null);
  const [erreurCode, setErreurCode] = useState<string | null>(null);

  const [erreurPresence, setErreurPresence] = useState<string | null>(null);
  const [succesPresence, setSuccesPresence] = useState<string | null>(null);
  const [enCoursPresence, setEnCoursPresence] = useState(false);

  const [exercice, setExercice] = useState<Exercice | null>(null);
  const [lien, setLien] = useState("");
  const [erreurExercice, setErreurExercice] = useState<string | null>(null);
  const [succesExercice, setSuccesExercice] = useState<string | null>(null);
  const [enCoursExercice, setEnCoursExercice] = useState(false);

  const [note, setNote] = useState<NoteExercice | null>(null);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- réinitialisation liée au changement d'étudiant
    setSession(null);
    setExercice(null);
    setNote(null);
  }, [etudiant]);

  async function validerCode() {
    setErreurCode(null);
    setSession(null);
    setExercice(null);
    setNote(null);
    try {
      setSession(await api.sessions.trouverParCode(code.trim().toUpperCase()));
    } catch (e) {
      if (e instanceof ApiError) setErreurCode(e.message);
    }
  }

  async function marquerPresence() {
    if (!etudiant || !session) return;
    setErreurPresence(null);
    setSuccesPresence(null);
    setEnCoursPresence(true);
    try {
      await api.presences.marquer(session.code, etudiant.id);
      setSuccesPresence("Présence enregistrée.");
    } catch (e) {
      if (e instanceof ApiError) setErreurPresence(e.message);
    } finally {
      setEnCoursPresence(false);
    }
  }

  async function chargerExercice() {
    if (!etudiant || !session) return;
    try {
      const trouve = await api.exercices.trouverPourSessionEtEtudiant(session.id, etudiant.id);
      setExercice(trouve);
      setNote(await api.exercices.note(trouve.id));
    } catch {
      setExercice(null);
      setNote(null);
    }
  }

  useEffect(() => {
    if (etudiant && session) {
      // eslint-disable-next-line react-hooks/set-state-in-effect -- synchronisation avec l'API au changement de session
      chargerExercice();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [etudiant, session]);

  async function deposerOuRemplacer() {
    if (!etudiant || !session || !lien.trim()) return;
    setErreurExercice(null);
    setSuccesExercice(null);
    setEnCoursExercice(true);
    try {
      if (exercice) {
        await api.exercices.remplacerLien(exercice.id, etudiant.id, lien.trim());
        setSuccesExercice("Lien remplacé.");
      } else {
        await api.exercices.deposer(session.id, etudiant.id, lien.trim());
        setSuccesExercice("Exercice déposé.");
      }
      setLien("");
      await chargerExercice();
    } catch (e) {
      if (e instanceof ApiError) setErreurExercice(e.message);
    } finally {
      setEnCoursExercice(false);
    }
  }

  return (
    <div>
      <h2>Écran étudiant</h2>

      <section className="carte">
        <h2>1. Qui suis-je ?</h2>
        <PromotionEtudiantPicker onSelect={setEtudiant} />
      </section>

      {etudiant && (
        <section className="carte">
          <h2>2. Code de la séance</h2>
          <Banniere type="erreur" texte={erreurCode} />
          <div className="champ-groupe">
            <label htmlFor="code">Code donné par le formateur</label>
            <input
              id="code"
              type="text"
              inputMode="text"
              maxLength={6}
              value={code}
              onChange={(e) => setCode(e.target.value.toUpperCase())}
              placeholder="ABC123"
            />
          </div>
          <button onClick={validerCode} disabled={code.trim().length === 0}>
            Valider le code
          </button>
          {session && (
            <p className="banniere banniere--info">
              Séance « {session.titre} »{session.cloturee ? " — clôturée" : ""}
            </p>
          )}
        </section>
      )}

      {etudiant && session && (
        <section className="carte">
          <h2>3. Ma présence</h2>
          <Banniere type="erreur" texte={erreurPresence} />
          <Banniere type="succes" texte={succesPresence} />
          <button onClick={marquerPresence} disabled={enCoursPresence}>
            {enCoursPresence ? "Envoi…" : "Marquer ma présence"}
          </button>
        </section>
      )}

      {etudiant && session && (
        <section className="carte">
          <h2>4. Mon exercice</h2>
          <Banniere type="erreur" texte={erreurExercice} />
          <Banniere type="succes" texte={succesExercice} />

          {exercice && (
            <p>
              Statut actuel : <span className="etiquette etiquette--attente">{LIBELLES_STATUT[exercice.statut]}</span>
            </p>
          )}

          <div className="champ-groupe">
            <label htmlFor="lien">{exercice ? "Remplacer le lien" : "Lien de mon exercice"}</label>
            <input
              id="lien"
              type="url"
              placeholder="https://github.com/…"
              value={lien}
              onChange={(e) => setLien(e.target.value)}
            />
          </div>
          <button onClick={deposerOuRemplacer} disabled={enCoursExercice || lien.trim().length === 0}>
            {exercice ? "Remplacer le lien" : "Déposer l'exercice"}
          </button>

          {note && note.note !== null && (
            <div className="carte" style={{ marginTop: 16, background: "var(--couleur-fond)" }}>
              <h3>Ma note{note.provisoire ? " (provisoire)" : ""}</h3>
              <p>
                <strong>{note.note.toFixed(1)}/20</strong>
              </p>
              <p>{note.commentaire}</p>
              {note.provisoire && (
                <p className="banniere banniere--info">
                  Un seul relecteur a rendu sa note pour l'instant ; cette note est provisoire en attendant le second avis.
                </p>
              )}
            </div>
          )}
          {note && note.note === null && exercice && <p>Relecture pas encore rendue.</p>}
        </section>
      )}
    </div>
  );
}
