package nutch;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.CrshAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.ManagementSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.FallbackWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.websocket.WebSocketAutoConfiguration;
import org.springframework.boot.cli.compiler.autoconfigure.SpringWebsocketCompilerAutoConfiguration;

@SpringBootApplication(exclude = {
        ActiveMQAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        SpringWebsocketCompilerAutoConfiguration.class,
        WebSocketAutoConfiguration.class,
        CrshAutoConfiguration.AuthenticationManagerAdapterAutoConfiguration.class,
        WebMvcAutoConfiguration.class,
        FallbackWebSecurityAutoConfiguration.class,
        SecurityAutoConfiguration.class,
        ManagementSecurityAutoConfiguration.class,
})
@EnableRabbit
public class NutchStarter {
    public static void main(String[] args) {
        SpringApplication.run(NutchStarter.class, args);
    }
}