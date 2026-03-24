package com.evoting.evoting_backend.repository;

import com.evoting.evoting_backend.model.Voter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface VoterRepository extends JpaRepository<Voter, Long> {
    Optional<Voter> findByMobile(String mobile);
    boolean existsByMobile(String mobile);
    Optional<Voter> findByAadharNumber(String aadharNumber);
    boolean existsByAadharNumber(String aadharNumber);

    @Query("SELECT DISTINCT v.state FROM Voter v WHERE v.state IS NOT NULL ORDER BY v.state")
    List<String> findDistinctStates();

    @Query("SELECT DISTINCT v.city FROM Voter v WHERE v.city IS NOT NULL ORDER BY v.city")
    List<String> findDistinctCities();

    @Query("SELECT DISTINCT v.city FROM Voter v WHERE v.state = :state AND v.city IS NOT NULL ORDER BY v.city")
    List<String> findDistinctCitiesByState(@Param("state") String state);

    @Query("SELECT DISTINCT v.constituency FROM Voter v WHERE v.constituency IS NOT NULL ORDER BY v.constituency")
    List<String> findDistinctConstituencies();

    @Query("SELECT DISTINCT v.constituency FROM Voter v WHERE v.city = :city AND v.constituency IS NOT NULL ORDER BY v.constituency")
    List<String> findDistinctConstituenciesByCity(@Param("city") String city);
    List<Voter> findByStateAndCityAndConstituency(String state, String city, String constituency);
    List<Voter> findByConstituency(String constituency);

    List<Voter> findByState(String state);
    List<Voter> findByStateAndCity(String state, String city);


}