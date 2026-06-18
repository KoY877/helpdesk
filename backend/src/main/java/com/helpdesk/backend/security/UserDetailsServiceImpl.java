package com.helpdesk.backend.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.helpdesk.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    
    private final UserRepository userRepository;

    /**
     * Loads a user by email for Spring Security authentication.
     *
     * @param email the email used as the username
     * @return the matching {@link UserDetails}
     * @throws UsernameNotFoundException if no user has that email
     */
    @Override
    public UserDetails loadUserByUsername (String email) throws UsernameNotFoundException {
        // Look up the user by email or fail with a Spring Security exception
        return userRepository.findByEmail(email)
                    .orElseThrow(()-> new UsernameNotFoundException("User not found"));
    }
}
