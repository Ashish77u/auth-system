package com.codelab.backend.service;


import com.codelab.backend.entity.User;
import com.codelab.backend.enums.Role;
import com.codelab.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        // 1. Let Spring fetch the user info from Google
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 2. Wrap the attributes in our helper
        OAuth2UserInfo userInfo = new OAuth2UserInfo(oAuth2User.getAttributes());

        log.info("OAuth2 login attempt for email: {}", userInfo.getEmail());

        // 3. Find or create the user in our DB
        User user = userRepository.findByEmail(userInfo.getEmail())
                .map(existingUser -> updateExistingUser(existingUser, userInfo))
                .orElseGet(() -> registerNewOAuth2User(userInfo));

        // 4. Return the user — Spring needs this to build the Authentication object

        user.setAttributes(oAuth2User.getAttributes());  // ← add this line   -- after update entity/User


        return user;
    }

    private User registerNewOAuth2User(OAuth2UserInfo userInfo) {
        log.info("Registering new OAuth2 user: {}", userInfo.getEmail());

        // Derive a username from the email (before the @)
        String username = deriveUsername(userInfo.getEmail());

        User newUser = User.builder()
                .username(username)
                .email(userInfo.getEmail())
                .password(null)          // OAuth2 users have no password
                .role(Role.USER)
                .enabled(true)           // Google already verified their email
                .provider("google")
                .providerId(userInfo.getId())
                .build();

        return userRepository.save(newUser);
    }

    private User updateExistingUser(User existingUser, OAuth2UserInfo userInfo) {
        // If they registered locally before, link their Google account
        if (existingUser.getProvider().equals("local")) {
            log.info("Linking Google account to existing local user: {}",
                    existingUser.getEmail());
            existingUser.setProvider("google");
            existingUser.setProviderId(userInfo.getId());
            // Keep their existing username and password
            return userRepository.save(existingUser);
        }
        // Already a Google user — just return them
        return existingUser;
    }

    // Turns "luci.dev@gmail.com" into "luci.dev"
    // Appends a number if that username is already taken
    private String deriveUsername(String email) {
        String base = email.split("@")[0]
                .replaceAll("[^a-zA-Z0-9._]", "");
        String username = base;
        int counter = 1;
        while (userRepository.existsByUsername(username)) {
            username = base + counter++;
        }
        return username;
    }
}