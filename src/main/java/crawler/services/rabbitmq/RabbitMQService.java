package crawler.services.rabbitmq;

/**
 * Created by marcel on 08-03-15.
 */
public interface RabbitMQService {
    public void sendUrlToBroker(String url);
}
