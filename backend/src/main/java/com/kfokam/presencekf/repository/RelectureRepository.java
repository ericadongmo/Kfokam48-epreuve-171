package com.kfokam.presencekf.repository;

import com.kfokam.presencekf.domain.Relecture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RelectureRepository extends JpaRepository<Relecture, Long> {

    /** Évolution étape 3 : jusqu'à deux relectures par exercice (RG4/RG5 révisées). */
    List<Relecture> findByExerciceId(Long exerciceId);

    List<Relecture> findByRelecteurIdIn(List<Long> relecteurIds);

    List<Relecture> findByExerciceIdIn(List<Long> exerciceIds);
}
