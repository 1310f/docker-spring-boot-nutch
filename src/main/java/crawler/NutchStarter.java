package crawler;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.CrshAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.ManagementSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.FallbackWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.websocket.WebSocketAutoConfiguration;
import org.springframework.boot.cli.compiler.autoconfigure.SpringWebsocketCompilerAutoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

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
        CrshAutoConfiguration.class,
        ElasticsearchDataAutoConfiguration.class,
        ElasticsearchRepositoriesAutoConfiguration.class
})
@EnableRabbit
@EnableMongoRepositories
// @EnableElasticsearchRepositories
public class NutchStarter {
    public static void main(String[] args) {
        SpringApplication.run(NutchStarter.class, args);
    }
}