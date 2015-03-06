package nutch.configuration.rabbitmq;

import jdk.nashorn.internal.runtime.Logging;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.logging.Logger;

/**
 * Created by marcel on 06-03-15.
 */
@Configuration
public class RabbitMQConfiguration implements MessageListener {
    Logger log = Logging.getLogger(this.getClass().getName());

    String queueName = "url";
    @Autowired
    AnnotationConfigApplicationContext context;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Bean
    Queue queue() {
        return new Queue(queueName, false);
    }

    @Bean
    TopicExchange exchange() {
        return new TopicExchange("url");
    }

    @Bean
    Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(queueName);
    }

    @Bean
    SimpleMessageListenerContainer container(ConnectionFactory connectionFactory) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(queueName);
        container.setMessageListener(this);
        return container;
    }

    @Override
    public void onMessage(Message message) {
        log.info("url("+message.getBody()+")");

    }

    @PostConstruct
    String home() throws Exception {
        return (String) rabbitTemplate.convertSendAndReceive(exchange().getName(),"http://www.nu.nl");
    }
}
