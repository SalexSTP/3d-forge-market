package com.aleksandar.threedforgemarket.security;

import com.aleksandar.threedforgemarket.repository.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MarketplaceUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public MarketplaceUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail)
            throws UsernameNotFoundException {
        return userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .map(MarketplaceUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found."));
    }
}
