package com.example.demo.timeblock.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TimeBlockRepositoryTest {

    @Autowired
    private TimeBlockRepository timeBlockRepository;

    @Test
    void shouldSaveEntity() {
        // TODO: build entity and save
        assertThat(timeBlockRepository.count()).isGreaterThanOrEqualTo(0);
    }
}
