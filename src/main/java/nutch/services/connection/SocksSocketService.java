package nutch.services.connection;

import org.apache.http.client.methods.CloseableHttpResponse;

import java.io.IOException;

/**
 * Created by marcel on 08-03-15.
 */
public interface SocksSocketService {
    public CloseableHttpResponse connect(String url) throws IOException;
}
