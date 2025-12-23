package com.hendisantika.usermanagement.controller;

import com.hendisantika.usermanagement.entity.Role;
import com.hendisantika.usermanagement.entity.User;
import com.hendisantika.usermanagement.exception.CustomFieldValidationException;
import com.hendisantika.usermanagement.service.UserService;
import com.hendisantika.usermanagement.repository.RoleRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class SignUpTest {

    private MockMvc mockMvc;

    @InjectMocks
    private UserController sut;

    @Mock
    private UserService userService;

    @Mock
    private RoleRepository roleRepository;

    private static Role defaultRole;
    private static User validUser;

    @BeforeAll
    public static void beforeAll() {
        defaultRole = new Role();
        defaultRole.setId(3L);
        defaultRole.setName("USER");
        defaultRole.setDescription("ROLE USER");

        validUser = new User();
        validUser.setId(1L);
        validUser.setFirstName("John");
        validUser.setLastName("Doe");
        validUser.setEmail("john.doe@example.com");
        validUser.setUsername("johndoe");
        validUser.setPassword("password");
        validUser.setConfirmPassword("password");
    }

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(sut)
                .setValidator(validator)
                .build();

        lenient().when(roleRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(roleRepository.findByName(anyString())).thenReturn(defaultRole);
    }


    @Test
    void getSignUpPage_shouldReturnSignupViewAndModel() throws Exception {
        mockMvc.perform(get("/signup"))
                .andExpect(status().isOk())
                .andExpect(view().name("user-form/user-signup"))
                .andExpect(model().attributeExists("signup"))
                .andExpect(model().attributeExists("userForm"))
                .andExpect(model().attributeExists("roles"));

        verify(roleRepository).findAll();
        verify(roleRepository).findByName(anyString());
    }

    @Test
    void postSignUp_validUser_shouldCreateAndReturnIndex() throws Exception {
        when(userService.createUser(any(User.class))).thenReturn(validUser);

        mockMvc.perform(post("/signup").flashAttr("userForm", validUser))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));

        verify(userService).createUser(any(User.class));
    }

    @Test
    void postSignUp_nameBoundaries_minMax_shouldSucceed() throws Exception {
        User boundaryUser = new User();
        boundaryUser.setFirstName("Jo"); // min length 2
        boundaryUser.setLastName("Loooooooooooooo"); // 15 chars
        boundaryUser.setEmail("a@b.com");
        boundaryUser.setUsername("user123");
        boundaryUser.setPassword("pwd");
        boundaryUser.setConfirmPassword("pwd");

        when(userService.createUser(any(User.class))).thenReturn(boundaryUser);

        mockMvc.perform(post("/signup").flashAttr("userForm", boundaryUser))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));

        verify(userService).createUser(any(User.class));
    }

    @Test
    void postSignUp_validationError_shortFirstName_shouldReturnSignupViewAndNotCallService() throws Exception {
        User invalid = new User();
        invalid.setFirstName("J"); // too short
        invalid.setLastName("ValidLast");
        invalid.setEmail("a@b.com");
        invalid.setUsername("user1");
        invalid.setPassword("pwd");
        invalid.setConfirmPassword("pwd");

        mockMvc.perform(post("/signup").flashAttr("userForm", invalid))
                .andExpect(status().isOk())
                .andExpect(view().name("user-form/user-signup"))
                .andExpect(model().attributeExists("userForm"))
                .andExpect(model().attributeExists("signup"));

        verify(userService, never()).createUser(any());
    }

    @Test
    void postSignUp_serviceThrowsCustomFieldValidationException_shouldHandleAndReturnIndex() throws Exception {
        when(userService.createUser(any(User.class)))
                .thenThrow(new CustomFieldValidationException("Username not available", "username"));

        mockMvc.perform(post("/signup").flashAttr("userForm", validUser))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));

        verify(userService).createUser(any(User.class));
    }

    @Test
    void postSignUp_serviceThrowsGenericException_shouldReturnIndexAndSetFormError() throws Exception {
        when(userService.createUser(any(User.class))).thenThrow(new RuntimeException("Boom"));

        mockMvc.perform(post("/signup").flashAttr("userForm", validUser))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));

        verify(userService).createUser(any(User.class));
    }

}
