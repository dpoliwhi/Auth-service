package ru.dpoliwhi.authservice.service;

import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;
import ru.dpoliwhi.authservice.configuration.KeycloakAuthClient;
import ru.dpoliwhi.authservice.dto.RegistrationRequest;
import ru.dpoliwhi.authservice.dto.TokensDto;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakUserService {

    private final RealmResource realmResource;

    private final KeycloakAuthClient keycloakAuthClient;

    private static final String USER_ROLE_NAME = "ROLE_USER";
    private static final String CLIENT_ID = "backend";

    public void registerUser(RegistrationRequest request) {
        log.info("Registering new user: {}", request.getUsername());

        if (isUsernameTaken(request.getUsername())) {
            throw new RuntimeException("Пользователь с таким именем уже существует");
        }

        ensureUserRoleExists();

        String userId = createUser(request);

        assignUserRole(userId);

        log.info("User {} successfully registered with role {}", request.getUsername(), USER_ROLE_NAME);
    }

    public boolean isUsernameTaken(String username) {
        List<UserRepresentation> users = realmResource.users().search(username, -1, -1);
        return users.stream()
                .anyMatch(user -> username.equals(user.getUsername()));
    }

    public TokensDto login(String username, String password) {
        log.info("Login attempt for user: {}", username);

        AccessTokenResponse tokenResponse = keycloakAuthClient.login(username, password);
        if (tokenResponse == null || tokenResponse.getToken() == null) {
            log.error("Login failed for user: {}", username);
            throw new RuntimeException("Неверное имя пользователя или пароль");
        }

        log.info("User {} successfully logged in", username);

        return new TokensDto(
                tokenResponse.getToken(),
                tokenResponse.getRefreshToken(),
                tokenResponse.getExpiresIn()
        );
    }

    public TokensDto refreshTokens(String refreshToken) {
        log.debug("Refreshing access token");

        AccessTokenResponse tokenResponse = keycloakAuthClient.refreshToken(refreshToken);
        if (tokenResponse == null || tokenResponse.getToken() == null) {
            log.error("Token refresh failed");
            throw new RuntimeException("Не удалось обновить токен");
        }

        return new TokensDto(
                tokenResponse.getToken(),
                tokenResponse.getRefreshToken(),
                tokenResponse.getExpiresIn()
        );
    }

    private void ensureUserRoleExists() {
        try {
            RoleResource roleResource = realmResource.clients().get(getClientId()).roles().get(USER_ROLE_NAME);
            roleResource.toRepresentation();
            log.debug("Role {} already exists for client {}", USER_ROLE_NAME, CLIENT_ID);
        } catch (Exception e) {
            log.info("Creating role {} for client {}", USER_ROLE_NAME, CLIENT_ID);
            createRole(USER_ROLE_NAME);
        }
    }

    private void createRole(String roleName) {
        RoleRepresentation role = new RoleRepresentation();
        role.setName(roleName);
        role.setDescription("Standard user role");
        realmResource.clients().get(getClientId()).roles().create(role);
    }

    private String getClientId() {
        return realmResource.clients().findByClientId(CLIENT_ID).get(0).getId();
    }

    private String createUser(RegistrationRequest request) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEnabled(true);
        user.setEmailVerified(false);

        UsersResource usersResource = realmResource.users();
        Response response = usersResource.create(user);

        if (response.getStatus() != 201) {
            Map<String, String> errorInfo = response.readEntity(Map.class);
            String errorMessage = "Ошибка при создании пользователя: " + errorInfo.getOrDefault("errorMessage", "Неизвестная ошибка");
            log.error("Failed to create user: {}", errorMessage);
            throw new RuntimeException(errorMessage);
        }

        String userId = extractUserId(response);
        response.close();

        setPassword(userId, request.getPassword());

        return userId;
    }

    private String extractUserId(Response response) {
        String location = response.getHeaderString("Location");
        if (location == null) {
            throw new RuntimeException("Ошибка при создании пользователя: не получен Location header");
        }
        return location.substring(location.lastIndexOf("/") + 1);
    }

    private void setPassword(String userId, String password) {
        CredentialRepresentation credentials = new CredentialRepresentation();
        credentials.setType(CredentialRepresentation.PASSWORD);
        credentials.setValue(password);
        credentials.setTemporary(false);

        realmResource.users().get(userId).resetPassword(credentials);
        log.debug("Password set for user {}", userId);
    }

    private void assignUserRole(String userId) {
        RoleRepresentation userRole = realmResource.clients().get(getClientId()).roles().get(USER_ROLE_NAME).toRepresentation();
        realmResource.users().get(userId).roles().clientLevel(getClientId()).add(List.of(userRole));
    }
}
