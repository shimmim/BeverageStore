package de.group15.assignment1.controller;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.group15.assignment1.model.*;
import de.group15.assignment1.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.LocalDate;
import java.util.*;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest//(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class RegistrationControllerTest {

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private MockMvc mvc;


    private UserDTO userDto;
    private UserDTO blankUser;

    @BeforeEach
    public void initCommonUsedData() {
        Address address = new Address(null, "An der Spinnerei", "13", "96049");
        Address address2 = new Address(null, "An der Test", "15", "96000");

        userDto = new UserDTO("password", LocalDate.of(1996, 8, 2), "mail@mail.de", "Username", Collections.singletonList(address), Collections.singletonList(address2));

        blankUser = new UserDTO();
        blankUser.setBillingAddresses(Collections.singletonList(new Address()));
        blankUser.setDeliveryAddresses(Collections.singletonList(new Address()));
    }


    @Test
    public void getGetRegistrationForm() throws Exception {
        this.mvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attribute("registrationForm",blankUser));
    }

    @Test
    public void postCreateUser_shouldSuccessFirstUser() throws Exception {
        when(this.userRepository.count()).thenReturn(0L);

        this.mvc.perform(post("/register")
                .params(convert(this.userDto))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(this.userRepository, times(1)).count();
    }

    @Test
    public void postCreateUser_shouldFailOnInvalidUser() throws Exception {
        this.userDto.setBirthday(LocalDate.of(1896, 8, 2));
        this.mvc.perform(post("/register")
                .params(convert(this.userDto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attribute("registrationForm",blankUser));
    }

    @Test
    public void postCreateUser_shouldSuccessNotFirstUser() throws Exception {
        when(this.userRepository.count()).thenReturn(1L);

        this.mvc.perform(post("/register")
                .params(convert(this.userDto))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(this.userRepository, times(1)).count();
    }

    private static MultiValueMap<String, String> convert(UserDTO dto) {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();

        for (int i = 0; i< dto.getBillingAddresses().size(); i++) {
            Address a = dto.getBillingAddresses().get(i);
            Map<String, String> maps = new ObjectMapper().convertValue(a, new TypeReference<Map<String, String>>() {});
            Map<String, String> newMap = new HashMap<>();
            for (Map.Entry<String, String> entry : maps.entrySet()) {
                newMap.put("billingAddresses["+i+"]."+ entry.getKey(), maps.get(entry.getKey()));
            }
            parameters.setAll(newMap);
        }

        for (int i = 0; i< dto.getDeliveryAddresses().size(); i++) {
            Address a = dto.getDeliveryAddresses().get(i);
            Map<String, String> maps = new ObjectMapper().convertValue(a, new TypeReference<Map<String, String>>() {});
            Map<String, String> newMap = new HashMap<>();
            for (Map.Entry<String, String> entry : maps.entrySet()) {
                newMap.put("deliveryAddresses["+i+"]."+ entry.getKey(), maps.get(entry.getKey()));
            }
            parameters.setAll(newMap);
        }
        parameters.add("password", dto.getPassword());
        if (dto.getBirthday() != null) {
            parameters.add("birthday", dto.getBirthday().toString());
        }
        parameters.add("email", dto.getEmail());
        parameters.add("username", dto.getUsername());
        return parameters;
    }

}
