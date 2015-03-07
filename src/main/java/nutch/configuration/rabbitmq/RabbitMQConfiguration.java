package nutch.configuration.rabbitmq;

import io.netty.channel.ConnectTimeoutException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.protocol.HttpContext;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;

/**
 * Created by marcel on 06-03-15.
 */
@Configuration
public class RabbitMQConfiguration implements MessageListener {
    private static final Logger log = LoggerFactory.getLogger(RabbitMQConfiguration.class);

    static {
        // TODO: application.properties


    }

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
        container.setQueueNames(queue().getName());
        container.setMessageListener(this);
        return container;
    }

    static class MyConnectionSocketFactory extends PlainConnectionSocketFactory {
        @Override
        public Socket createSocket(final HttpContext context) throws IOException {
            InetSocketAddress socksaddr = (InetSocketAddress) context.getAttribute("socks.address");
            Proxy proxy = new Proxy(Proxy.Type.SOCKS, socksaddr);
            return new Socket(proxy);
        }

        @Override
        public Socket connectSocket(int connectTimeout, Socket socket, HttpHost host, InetSocketAddress remoteAddress,
                                    InetSocketAddress localAddress, org.apache.http.protocol.HttpContext context) throws IOException {
            InetSocketAddress unresolvedRemote = InetSocketAddress
                    .createUnresolved(host.getHostName(), remoteAddress.getPort());
            return super.connectSocket(connectTimeout, socket, host, unresolvedRemote, localAddress, context);
        }
    }

    static class MySSLConnectionSocketFactory extends SSLConnectionSocketFactory {

        public MySSLConnectionSocketFactory(final SSLContext sslContext) {
            // You may need this verifier if target site's certificate is not secure
            super(sslContext, ALLOW_ALL_HOSTNAME_VERIFIER);
        }

        @Override
        public Socket createSocket(final HttpContext context) throws IOException {
            InetSocketAddress socksaddr = (InetSocketAddress) context.getAttribute("socks.address");
            Proxy proxy = new Proxy(Proxy.Type.SOCKS, socksaddr);
            return new Socket(proxy);
        }

        @Override
        public Socket connectSocket(int connectTimeout, Socket socket, HttpHost host, InetSocketAddress remoteAddress,
                                    InetSocketAddress localAddress, HttpContext context) throws IOException {
            // Convert address to unresolved
            InetSocketAddress unresolvedRemote = InetSocketAddress
                    .createUnresolved(host.getHostName(), remoteAddress.getPort());
            return super.connectSocket(connectTimeout, socket, host, unresolvedRemote, localAddress, context);
        }
    }

    static class MyConnectionSocketFactory2 implements ConnectionSocketFactory {


        @Override
        public Socket createSocket(org.apache.http.protocol.HttpContext context) throws IOException {
            InetSocketAddress socksaddr = (InetSocketAddress) context.getAttribute("socks.address");
            Proxy proxy = new Proxy(Proxy.Type.SOCKS, socksaddr);
            return new Socket(proxy);        }

        @Override
        public Socket connectSocket(int connectTimeout, Socket socket, org.apache.http.HttpHost host, InetSocketAddress remoteAddress, InetSocketAddress localAddress, org.apache.http.protocol.HttpContext context) throws IOException {
            Socket sock;

                if (socket != null) {
                    sock = socket;
                } else {
                    sock = createSocket(context);
                }
                if (localAddress != null) {
                    sock.bind(localAddress);
                }
                try {
                    sock.connect(remoteAddress, connectTimeout);
                } catch (SocketTimeoutException ex) {
                    throw new org.apache.http.conn.ConnectTimeoutException(ex, host, remoteAddress.getAddress());
                }
                return sock;        }
    }

    static class FakeDnsResolver implements DnsResolver {
        @Override
        public InetAddress[] resolve(String host) throws UnknownHostException {
            // Return some fake DNS record for every request, we won't be using it
            return new InetAddress[] { InetAddress.getByAddress(new byte[] { 1, 1, 1, 1 }) };
        }
    }

    @Bean
    MyConnectionSocketFactory myConnectionSocketFactory() {
        return new MyConnectionSocketFactory();
    }

    @Bean
    MySSLConnectionSocketFactory mySSLConnectionSocketFactory() {
        return new MySSLConnectionSocketFactory(SSLContexts.createSystemDefault());
    }

    @Bean
    Registry<ConnectionSocketFactory> registry() {
        return RegistryBuilder.<ConnectionSocketFactory> create()
                .register("http", myConnectionSocketFactory())
                .register("https", mySSLConnectionSocketFactory())
                .build();
    }

    @Bean
    FakeDnsResolver fakeDnsResolver() {
        return new FakeDnsResolver();
    }

    @Bean
    PoolingHttpClientConnectionManager poolingHttpClientConnectionManager() {
        return new PoolingHttpClientConnectionManager(registry(), new FakeDnsResolver());
    }

    @Bean
    CloseableHttpClient closeableHttpClient() throws IOException {
        return HttpClients.custom().setConnectionManager(poolingHttpClientConnectionManager()).build();
    }

    @Bean
    InetSocketAddress inetSocketAddressSocksProxy() {
        return new InetSocketAddress("docker", 9050);
    }


    public void onMessage(Message message) {
        String url = new String(message.getBody());

        try {

            HttpClientContext context = HttpClientContext.create();
            context.setAttribute("socks.address", inetSocketAddressSocksProxy());
            HttpGet request = new HttpGet(url);

            // HttpHost target = new HttpHost("vj5pbopejlhcbz4n.onion", 80, "http");
            // HttpGet request = new HttpGet("/");

            CloseableHttpResponse response = closeableHttpClient().execute(request, context);

            try {
                if(response.getStatusLine().getStatusCode()==200) {
                    byte[] page = IOUtils.toByteArray(response.getEntity().getContent());
                    log.info("page: " + new String(page));
                } else {
                    log.warn("statuscode("+response.getStatusLine().getStatusCode()+")");
                }
                EntityUtils.consume(response.getEntity());
            } finally {
                response.close();
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @PostConstruct
    void sendOne() throws Exception {
        rabbitTemplate.convertAndSend(exchange().getName(), "http://vj5pbopejlhcbz4n.onion/");
    }
}
