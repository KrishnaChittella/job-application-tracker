package com.jobtracker.security;

import com.jobtracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads user by email for login, or by id when resolving JWT (see JwtAuthFilter).
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Support lookup by email (login) or by id (JWT filter passes userId as string)
        UserDetails byEmail = loadByEmail(username);
        if (byEmail != null) return byEmail;
        UserDetails byId = loadById(username);
        if (byId != null) return byId;
        throw new UsernameNotFoundException("User not found: " + username);
    }

    private UserDetails loadByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(UserPrincipal::new)
                .orElse(null);
    }

    private UserDetails loadById(String idStr) {
        try {
            Long id = Long.parseLong(idStr);
            return userRepository.findById(id)
                    .map(UserPrincipal::new)
                    .orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
