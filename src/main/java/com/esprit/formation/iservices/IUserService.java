package com.esprit.formation.iservices;

import com.esprit.formation.dto.UserDTO;
import com.esprit.formation.entities.RevokedToken;
import com.esprit.formation.entities.Role;
import com.esprit.formation.entities.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface IUserService {
    Optional<User> getUserByEmail(String email);
    Optional<User> getUserById(Long userId);

    User saveUser(User user);
    User hasGoogleId(User user, String googleId);
    User hasFacebookId(User user, String facebookId);
    void deleteUserByEmail(String mail);
    Boolean validateRole(String role);
    //String validateToken(HttpServletRequest request);

    Set<User> getUserByIds(List<Long> ids);


    boolean isTokenRevoked(String token);
    RevokedToken creatRevokedToken (String token);
    User updateUser(User user);
    void checkAndSaveUser(Authentication authentication);

    List<User> getAllUsers();

    User updateUserPassword(User user, String newPassword);

     List<User> getAllEnabledUsers();
     List<User> getAllDeleteddUsers();
    User updateUserActiveState(String email, boolean isActive);

    List<User> getAllByRole(Role role);

     UserDTO mapToUserDTO(User user);
     User mapToUserEntity(UserDTO userDTO);

    UserDTO mapToUserDtoPost(User user);

    // for logout action
    void performLogout(String token, HttpServletRequest request, HttpServletResponse response);
 List<User> getByAllrole(List<Role> roles);






    //Map<String, Object> processOAuth2User(String email, String providerId, String name, String provider);

}
