package com.helpdesk.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.helpdesk.backend.Data_Transfert_Object.UserResponse;
import com.helpdesk.backend.Data_Transfert_Object.UserRoleUpdateRequest;
import com.helpdesk.backend.Data_Transfert_Object.UserUpdateRequest;
import com.helpdesk.backend.exception.ResourceNotFoundException;
import com.helpdesk.backend.model.User;
import com.helpdesk.backend.model.enums.Role;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.helpdesk.backend.repository.UserRepository;
import com.helpdesk.backend.security.JwtService;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @InjectMocks private UserService userService;

    @Test
    void getAllUsers_withNoUsers_throwsResourceNotFoundException() {
        when(userRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> userService.getAllUsers())
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllUsers_returnsListOfUsers() {
        User user = new User();
        user.setId("u1");
        user.setName("Kodjo");
        user.setEmail("test@test.com");
        user.setRole(Role.USER);

        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserResponse> result = userService.getAllUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).email()).isEqualTo("test@test.com");
    }
    
    @Test
    void deleteUser_withExistingId_returnUserResponse(){
        User user = new User();
        user.setId("u1");

        when(userRepository.existsById("u1")).thenReturn(true);

        userService.deleteUser("u1");
        
        verify(userRepository).deleteById("u1");       
    }

    @Test 
    void deleteUser_withUnknowId_throwsResourceNotFoundException(){
        when(userRepository.existsById("bad")).thenReturn(false);
        
        assertThatThrownBy(() -> 
           userService.deleteUser("bad")
        ).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateUser_withExistingId_returnUserResponse() {
        User user = new User();
        user.setId("u1");
        user.setName("Kodjo");
        user.setEmail("test@test.com");
        user.setPassword("encoded");
        user.setRole(Role.USER);
        
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        UserUpdateRequest request = new UserUpdateRequest("Koffi", "test1@test.com", "");
        UserResponse result = userService.updateUser("u1", request);
    
        assertThat(result.name()).isEqualTo("Koffi");
         assertThat(result.email()).isEqualTo("test1@test.com");
    }

    @Test
    void updateUser_withUnknowId_throwsResourceNotFoundException() {
       
        when(userRepository.findById("u2")).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> 
            userService.updateUser("u2", new UserUpdateRequest("Koffi", "test@test.com",""))
        ).isInstanceOf(ResourceNotFoundException.class);
    }
    
    @Test
    void updateRole_withExistingId_returnUserResponse() {
        User user = new User();
        user.setId("u1");
        user.setRole(Role.USER);
        
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        UserRoleUpdateRequest request = new UserRoleUpdateRequest(Role.AGENT);
        UserResponse result = userService.updateRole("u1", request);
    
        assertThat(result.role()).isEqualTo(Role.AGENT);
    }

    @Test
    void updateRole_withUnknowId_throwsResourceNotFoundException() {
       
        when(userRepository.findById("u2")).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> {
            UserRoleUpdateRequest request = new UserRoleUpdateRequest(Role.AGENT);
            userService.updateRole("u2", request);
        }).isInstanceOf(ResourceNotFoundException.class);
    }
}
