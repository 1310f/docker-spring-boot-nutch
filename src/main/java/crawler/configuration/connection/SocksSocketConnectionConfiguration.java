package crawler.configuration.connection;

import crawler.services.connection.SocksSocketService;
import crawler.services.connection.SocksSocketServiceImpl;
import org.apache.http.HttpHost;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.*;

/**
 * Created by marcel on 08-03-15.
 */
@Configuration
public class SocksSocketConnectionConfiguration {

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

    @Bean
    HttpClientContext httpClientContext() {
        HttpClientContext httpClientContext = HttpClientContext.create();
        httpClientContext.setAttribute("socks.address", inetSocketAddressSocksProxy());
        return httpClientContext;
    }

    @Bean
    SocksSocketService socksSocketService() {
        SocksSocketService socksSocketService = new SocksSocketServiceImpl();
        return socksSocketService;
    }

}
