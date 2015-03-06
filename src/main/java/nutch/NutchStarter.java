package nutch;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.FallbackWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;

@SpringBootApplication(exclude = {
        WebMvcAutoConfiguration.class,
        FallbackWebSecurityAutoConfiguration.class
})
@EnableRabbit
public class NutchStarter {
    public static void main(String[] args) {
        SpringApplication.run(NutchStarter.class, args);
    }
}