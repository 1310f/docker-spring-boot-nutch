package nutch.configuration.rabbitmq;

import com.google.gson.Gson;
import nutch.services.connection.SocksSocketService;
import nutch.services.rabbitmq.RabbitMQService;
import nutch.services.rabbitmq.RabbitMQServiceImpl;
import nutch.services.tika.TikaService;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.tika.Tika;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.xml.sax.SAXException;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.Reader;
import java.net.*;
import java.util.HashMap;

/**
 * Created by marcel on 06-03-15.
 */
@Configuration
public class RabbitMQConfiguration implements MessageListener {
    private static final Logger log = LoggerFactory.getLogger(RabbitMQConfiguration.class);

    static {
        // TODO: application.properties


    }

    String htmlQueueName = "text/html";
    String urlExchangeName = "url";

    @Autowired
    AnnotationConfigApplicationContext context;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Bean
    Queue htmlQueue() {
        boolean durable = false;
        Queue queue = new Queue(htmlQueueName, durable);
        amqpAdmin.declareQueue(queue);
        return queue;
    }

    @Bean
    Exchange urlExchange() {
        Exchange exchange = new DirectExchange(urlExchangeName);
        amqpAdmin.declareExchange(exchange);
        return exchange;
    }

    @Autowired
    AmqpAdmin amqpAdmin;

    @Bean
    Binding binding() {
        Binding binding = new Binding(
                htmlQueue().getName(),
                Binding.DestinationType.QUEUE,
                urlExchange().getName(),
                "*",
                new HashMap<>());
        amqpAdmin.declareBinding(binding);
        return binding;
    }

    @Bean
    SimpleMessageListenerContainer container(ConnectionFactory connectionFactory) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(htmlQueue().getName());
        container.setMessageListener(this);
        return container;
    }


    @Autowired
    TikaService tikaService;

    @Autowired
    Tika tika;

    @Autowired
    SocksSocketService socksSocketService;

    @Autowired
    Gson gson;

    @Bean
    RabbitMQService rabbitMQService() {
        RabbitMQService rabbitMQService = new RabbitMQServiceImpl();
        return rabbitMQService;
    }

    public void onMessage(Message message) {
        String url = new String(message.getBody());

        CloseableHttpResponse closeableHttpResponse = null;
        try {
            closeableHttpResponse = socksSocketService.connect(url);

            if(closeableHttpResponse.getStatusLine().getStatusCode()==200) {

                long length = closeableHttpResponse.getEntity().getContentLength();
                Header contentType = closeableHttpResponse.getEntity().getContentType();

                TikaInputStream tikaInputStream = TikaInputStream.get(closeableHttpResponse.getEntity().getContent());
                Metadata metadata = new Metadata();
                metadata.add(Metadata.RESOURCE_NAME_KEY, url);
                metadata.add(Metadata.CONTENT_TYPE, contentType.getValue());

                MediaType mediaType = tikaService.detect(tikaInputStream, metadata);

                if( "application".equals(mediaType.getType())) {
                    if("xhtml+xml".equals(mediaType.getSubtype())) {
                        tikaService.htmlContentHandler(tikaInputStream, metadata);
                    } else {
                        log.warn("this contentType("+mediaType.toString()+") not supported!");
                    }
                } else if( "text".equals(mediaType.getType())) {
                    if("html".equals(mediaType.getSubtype())) {
                        tikaService.htmlContentHandler(tikaInputStream, metadata);
                    } else {
                        log.warn("this contentType("+mediaType.toString()+") not supported!");
                    }
                } else if( "image".equals(mediaType.getType())) {
                    tikaService.imageContentHandler(tikaInputStream,metadata);
                } else {
                    log.warn("this contentType("+mediaType.toString()+") not supported!");
                }

            } else {
                log.warn("statuscode("+closeableHttpResponse.getStatusLine().getStatusCode()+")");
            }
            EntityUtils.consume(closeableHttpResponse.getEntity());

        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (TikaException e) {
            e.printStackTrace();
        } finally {
            if(closeableHttpResponse != null) {
                try {
                    closeableHttpResponse.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @PostConstruct
    void sendAFew() throws Exception {

        String[] urls = {
                "http://xcomics5vvoiary2.onion/Alraune1/default/logo.gif",
                "http://tigas3l7uusztiqu.onion"

        // link: meta({"metadata":{"resourceName":["http://tigas3l7uusztiqu.onion"],"Content-Type":["text/html"]}}), link({"type":"a","uri":"/colophon/#license","title":"","text":"CC Attribution 3.0. See license.","rel":""})
        // meta({"metadata":{"Dimension HorizontalPixelOffset":["0"],"tiff:ImageLength":["62"],"Compression CompressionTypeName":["lzw"],"resourceName":["http://xcomics5vvoiary2.onion/Alraune1/default/logo.gif"],"GraphicControlExtension":["disposalMethod\u003dnone, userInputFlag\u003dfalse, transparentColorFlag\u003dfalse, delayTime\u003d0, transparentColorIndex\u003d0"],"Compression NumProgressiveScans":["1"],"Chroma ColorSpaceType":["RGB"],"Chroma BlackIsZero":["true"],"Compression Lossless":["true"],"width":["200"],"Dimension ImageOrientation":["Normal"],"ImageDescriptor":["imageLeftPosition\u003d0, imageTopPosition\u003d0, imageWidth\u003d200, imageHeight\u003d62, interlaceFlag\u003dfalse"],"Dimension VerticalPixelOffset":["0"],"tiff:ImageWidth":["200"],"Chroma NumChannels":["3"],"Data SampleFormat":["Index"],"Content-Type":["image/gif"],"height":["62"]}}), defaultHandler({}), parseContext({"context":{}})

        };

        for(String url : urls) {
            rabbitMQService().sendUrlToBroker(url);
        }
    }
}
