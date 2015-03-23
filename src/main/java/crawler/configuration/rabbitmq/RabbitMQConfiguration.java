package crawler.configuration.rabbitmq;

import com.google.gson.Gson;
import crawler.model.Page;
import crawler.repository.MongoPageRepository;
import crawler.services.connection.SocksSocketService;
import crawler.services.rabbitmq.RabbitMQService;
import crawler.services.rabbitmq.RabbitMQServiceImpl;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.IOUtils;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.parser.image.ImageParser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.LinkContentHandler;
import org.apache.tika.sax.TeeContentHandler;
import org.apache.tika.sax.ToHTMLContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by marcel on 06-03-15.
 */
@Configuration

@EnableConfigurationProperties(RabbitMQConfiguration.RabbitMQConfigurationProperties.class)
public class RabbitMQConfiguration implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQConfiguration.class);

    static {
        // TODO: application.properties


    }

    int MAX_BODY_LENGTH = 10 * 1024 * 1024;

    @Autowired
    RabbitMQConfigurationProperties rabbitMQConfigurationProperties;
    String htmlQueueName = "text/html";
    String urlExchangeName = "url";
    @Autowired
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory;
    @Autowired
    AnnotationConfigApplicationContext context;
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    AmqpAdmin amqpAdmin;

    @Autowired
    Tika tika;
    @Autowired
    SocksSocketService socksSocketService;
    @Autowired
    Gson gson;
    @Autowired
    MongoTemplate mongoTemplate;
    @Autowired
    MongoPageRepository mongoPageRepository;
    @Autowired
    TikaConfig tikaConfig;

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
        container.setConcurrentConsumers(rabbitMQConfigurationProperties.getConcurrentConsumers());
        // container.setMaxConcurrentConsumers(32);
        container.setPrefetchCount(rabbitMQConfigurationProperties.getPrefetchCount());
        // rabbitListenerContainerFactory.setConcurrentConsumers(16);
        return container;
    }

    @Bean
    RabbitMQService rabbitMQService() {
        RabbitMQService rabbitMQService = new RabbitMQServiceImpl();
        return rabbitMQService;
    }

    public void onMessage(Message message) {
        String url = new String(message.getBody());
        log.info("parse(" + url + ")..");

        CloseableHttpResponse closeableHttpResponse = null;
        try {
            closeableHttpResponse = socksSocketService.connect(url);

            if (closeableHttpResponse.getStatusLine().getStatusCode() == 200) {

                long length = closeableHttpResponse.getEntity().getContentLength();
                Header contentType = closeableHttpResponse.getEntity().getContentType();

                InputStream inputStream = closeableHttpResponse.getEntity().getContent();


                TikaInputStream tikaInputStream = TikaInputStream.get(inputStream);
                Metadata metadata = new Metadata();
                metadata.add(Metadata.RESOURCE_NAME_KEY, url);
                URL u = new URL(metadata.get(Metadata.RESOURCE_NAME_KEY));
                metadata.add(Metadata.CONTENT_TYPE, contentType.getValue());

                Detector detector = tikaConfig.getDetector();
                MediaType mediaType = detector.detect(tikaInputStream, metadata);

                List<Page> pages = mongoPageRepository.findByUrl(u);
                // String text = bodyContentHandler.toString();
                Page page = null;

                if (pages.size() == 0) {
                    // manually inserted page
                    page = new Page();
                    page.setInsertDate(new Date());
                    page.setUrl(u);
                    log.info(" .. manual(" + url + ")");
                } else {
                    if (pages.size() > 1) {
                        log.warn("url(" + url + ") multiple times in db!");
                    }
                    // from html link extractor
                    page = pages.get(0);
                    page.setUpdateDate(new Date());
                }


                ParseContext parseContext = new ParseContext();
                LinkContentHandler linkContentHandler = new LinkContentHandler();
                BodyContentHandler bodyContentHandler = new BodyContentHandler(MAX_BODY_LENGTH);
                ToHTMLContentHandler toHTMLContentHandler = new ToHTMLContentHandler();
                HtmlParser htmlParser = new HtmlParser();
                DefaultHandler defaultHandler = new DefaultHandler();

                TeeContentHandler teeContentHandler = new TeeContentHandler(linkContentHandler, bodyContentHandler, toHTMLContentHandler);

                AutoDetectParser autoDetectParser = new AutoDetectParser();
                autoDetectParser.parse(tikaInputStream,teeContentHandler,metadata);

log.info("autodetect: defaultHandler(" + gson.toJson(defaultHandler) + "), metadata(" + gson.toJson(metadata) + "), mediaType(" + gson.toJson(autoDetectParser.getMediaTypeRegistry()) + "), parseContext(" + gson.toJson(parseContext) + "): content(" + gson.toJson(teeContentHandler) + ")");

                if ("application".equals(mediaType.getType())) {
                    if ("xhtml+xml".equals(mediaType.getSubtype())) {
                        htmlParser.parse(tikaInputStream, teeContentHandler, metadata, parseContext);
                        String html = toHTMLContentHandler.toString();
                        page.setContent(html.toString().getBytes());
                    } else {
                        log.warn("this contentType(" + mediaType.toString() + ") not supported!");
                    }
                } else if("text".equals(mediaType.getType())) {
                    if ("html".equals(mediaType.getSubtype())) {
                        htmlParser.parse(tikaInputStream, teeContentHandler, metadata, parseContext);
                        String html = toHTMLContentHandler.toString();
                        page.setContent(html.toString().getBytes());
                    } else {
                        log.warn("this contentType(" + mediaType.toString() + ") not supported!");
                    }
                }

                page.setContent(IOUtils.toByteArray(tikaInputStream));
                page.setMetadata(metadata);
                mongoPageRepository.save(page);


            } else {
                log.warn("url(" + url + "): statuscode(" + closeableHttpResponse.getStatusLine().getStatusCode() + ")");
            }
            EntityUtils.consume(closeableHttpResponse.getEntity());

        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (TikaException e) {
            e.printStackTrace();
        } finally {
            if (closeableHttpResponse != null) {
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
                //"http://xcomics5vvoiary2.onion/Alraune1/default/logo.gif",
                "http://tigas3l7uusztiqu.onion"

                // link: meta({"metadata":{"resourceName":["http://tigas3l7uusztiqu.onion"],"Content-Type":["text/html"]}}), link({"type":"a","uri":"/colophon/#license","title":"","text":"CC Attribution 3.0. See license.","rel":""})
                // meta({"metadata":{"Dimension HorizontalPixelOffset":["0"],"tiff:ImageLength":["62"],"Compression CompressionTypeName":["lzw"],"resourceName":["http://xcomics5vvoiary2.onion/Alraune1/default/logo.gif"],"GraphicControlExtension":["disposalMethod\u003dnone, userInputFlag\u003dfalse, transparentColorFlag\u003dfalse, delayTime\u003d0, transparentColorIndex\u003d0"],"Compression NumProgressiveScans":["1"],"Chroma ColorSpaceType":["RGB"],"Chroma BlackIsZero":["true"],"Compression Lossless":["true"],"width":["200"],"Dimension ImageOrientation":["Normal"],"ImageDescriptor":["imageLeftPosition\u003d0, imageTopPosition\u003d0, imageWidth\u003d200, imageHeight\u003d62, interlaceFlag\u003dfalse"],"Dimension VerticalPixelOffset":["0"],"tiff:ImageWidth":["200"],"Chroma NumChannels":["3"],"Data SampleFormat":["Index"],"Content-Type":["image/gif"],"height":["62"]}}), defaultHandler({}), parseContext({"context":{}})

        };

        for (String url : rabbitMQConfigurationProperties.getSites()) {
            rabbitMQService().sendUrlToBroker(url);
        }
    }

    @ConfigurationProperties(prefix = "spring.application")
    public static class RabbitMQConfigurationProperties {
        int concurrentConsumers;
        int prefetchCount;
        String[] sites;
        int maxBodyLength;

        public int getMaxBodyLength() {
            return maxBodyLength;
        }

        public void setMaxBodyLength(int maxBodyLength) {
            this.maxBodyLength = maxBodyLength;
        }

        public int getConcurrentConsumers() {
            return concurrentConsumers;
        }

        public void setConcurrentConsumers(int concurrentConsumers) {
            this.concurrentConsumers = concurrentConsumers;
        }

        public int getPrefetchCount() {
            return prefetchCount;
        }

        public void setPrefetchCount(int prefetchCount) {
            this.prefetchCount = prefetchCount;
        }

        public String[] getSites() {
            return sites;
        }

        public void setSites(String[] sites) {
            this.sites = sites;
        }

    }
}
