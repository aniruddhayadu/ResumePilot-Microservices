package com.resumepilot.auth.controller;

import com.resumepilot.auth.entity.User;
import com.resumepilot.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private UserRepository userRepository;

    private AdminController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminController();
        ReflectionTestUtils.setField(controller, "userRepository", userRepository);
    }

    @Test
    void getAdminStatsReturnsCounts() {
        when(userRepository.count()).thenReturn(7L);

        ResponseEntity<?> response = controller.getAdminStats();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bodyAsMap(response)).containsEntry("totalUsers", 7L);
        assertThat(bodyAsMap(response)).containsEntry("resumesBuilt", 156);
    }

    @Test
    void getAdminStatsReturnsFallbackWhenRepositoryFails() {
        when(userRepository.count()).thenThrow(new RuntimeException("database down"));

        ResponseEntity<?> response = controller.getAdminStats();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bodyAsMap(response)).containsEntry("totalUsers", 0);
        assertThat(bodyAsMap(response)).containsEntry("error", "database down");
    }

    @Test
    void getAllUsersReturnsRepositoryUsers() {
        User user = new User();
        user.setUserId(15L);
        user.setFullName("Palak");
        user.setEmail("palak@example.com");
        user.setPasswordHash("secret");
        user.setRole("FREE");
        user.setSubscriptionPlan("FREE");
        user.setActive(true);
        user.setVerified(true);
        when(userRepository.findAll()).thenReturn(List.of(user));

        ResponseEntity<?> response = controller.getAllUsers();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bodyAsUserList(response)).hasSize(1);
        assertThat(bodyAsUserList(response).get(0))
                .containsEntry("userId", 15L)
                .containsEntry("fullName", "Palak")
                .containsEntry("email", "palak@example.com")
                .containsEntry("role", "FREE")
                .containsEntry("subscriptionPlan", "FREE")
                .doesNotContainKeys("passwordHash", "otp", "resetToken");
    }

    @Test
    void getAllUsersReturnsServerErrorWhenRepositoryFails() {
        when(userRepository.findAll()).thenThrow(new RuntimeException("database down"));

        ResponseEntity<?> response = controller.getAllUsers();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(bodyAsMap(response)).containsEntry("error", "Error fetching users");
    }

    @Test
    void deleteUserDeletesExistingUser() {
        when(userRepository.existsById(10L)).thenReturn(true);

        ResponseEntity<?> response = controller.deleteUser(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bodyAsMap(response)).containsEntry("message", "User deleted successfully");
        verify(userRepository).deleteById(10L);
    }

    @Test
    void deleteUserReturnsNotFoundForMissingUser() {
        when(userRepository.existsById(10L)).thenReturn(false);

        ResponseEntity<?> response = controller.deleteUser(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(bodyAsMap(response)).containsEntry("error", "User not found");
    }

    @Test
    void deleteUserReturnsServerErrorWhenDeleteFails() {
        when(userRepository.existsById(10L)).thenReturn(true);
        doThrow(new RuntimeException("database down")).when(userRepository).deleteById(10L);

        ResponseEntity<?> response = controller.deleteUser(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(bodyAsMap(response)).containsEntry("error", "Failed to delete user");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> bodyAsMap(ResponseEntity<?> response) {
        return (Map<String, Object>) response.getBody();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> bodyAsUserList(ResponseEntity<?> response) {
        return (List<Map<String, Object>>) response.getBody();
    }
}
