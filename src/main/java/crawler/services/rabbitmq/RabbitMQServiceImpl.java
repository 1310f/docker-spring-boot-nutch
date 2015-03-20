package crawler.services.rabbitmq;

import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by marcel on 08-03-15.
 */
public class RabbitMQServiceImpl implements RabbitMQService {

    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    Exchange urlExchange;

    public void sendUrlToBroker(String url) {
        rabbitTemplate.convertAndSend(urlExchange.getName(), "*", url);

    }
}
