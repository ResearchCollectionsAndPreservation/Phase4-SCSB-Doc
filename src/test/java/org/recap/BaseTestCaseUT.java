package org.recap;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.TestPropertySource;

@ExtendWith(MockitoExtension.class)
@TestPropertySource("classpath:application.properties")
public class BaseTestCaseUT {

    @Test
    public void loadContexts() {
        System.out.println();
    }

}
