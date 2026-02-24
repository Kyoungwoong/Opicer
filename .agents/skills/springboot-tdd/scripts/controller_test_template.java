package com.example.demo.timeblock.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TimeBlockController.class)
class TimeBlockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private com.example.demo.timeblock.service.TimeBlockService timeBlockService;

    @Test
    @DisplayName("GET /time-blocks returns 200")
    void shouldReturnOk() throws Exception {
        mockMvc.perform(get("/time-blocks")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }
}
