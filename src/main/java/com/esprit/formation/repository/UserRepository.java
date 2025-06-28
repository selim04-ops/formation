package com.esprit.formation.repository;

import com.esprit.formation.entities.Role;
import com.esprit.formation.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmail(String email);
  //User deleteUserByEmail(String email);
  User findByGoogleId(String googleId);
  User findByFacebookId(String facebookId);
  List<User> findAllByIsActiveTrue();
  List<User> findAllByIsActiveFalse();
  List<User> findAllByRole(Role role);
  Set<User> findAllByIdInAndRole(List<Long> ids, Role role);
  Set<User> findAllByIdInAndIsActiveTrue(List<Long> ids);
  long countByRole(Role role);
  List<User> findByRoleIn(List<Role> roles);






  //for notification
  //List<User>findByIsActiveTrueAndIsAdminOrIsSuperAdmin(Boolean isAdmin, Boolean isSuperAdmin);
  //List<User> findByIsActiveTrue();
 // Optional<User> findByIdAndIsActiveTrue(String userId);
//  @Query("SELECT u FROM User u JOIN u.participatedEvents e WHERE e.id = :eventId AND u.isActive = true")
//  List<User> findParticipantsByEventId(@Param("eventId") String eventId);



}