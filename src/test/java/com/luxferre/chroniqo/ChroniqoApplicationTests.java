package com.luxferre.chroniqo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.CrossOrigin;

@SpringBootTest
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
class ChroniqoApplicationTests {

    @Test
    void contextLoads() {
    }

}
