package nutch.services.connection;

import nutch.services.tika.TikaService;
import org.apache.http.Header;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Created by marcel on 08-03-15.
 */
public class SocksSocketServiceImpl implements SocksSocketService {
    private static final Logger log = LoggerFactory.getLogger(SocksSocketServiceImpl.class);

    @Autowired
    TikaService tikaService;

    @Autowired
    CloseableHttpClient closeableHttpClient;

    @Autowired
    HttpClientContext httpClientContext;

    public CloseableHttpResponse connect(String url) throws IOException {

        HttpGet request = new HttpGet(url);
        CloseableHttpResponse response = closeableHttpClient.execute(request, httpClientContext);
        return response;

    }


}
