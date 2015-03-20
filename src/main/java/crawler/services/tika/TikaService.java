package crawler.services.tika;

import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Created by marcel on 08-03-15.
 */
public interface TikaService {
    MediaType detect(TikaInputStream tikaInputStream, Metadata metadata) throws IOException;

    public String htmlToString(TikaInputStream inputStream, Metadata metadata) throws IOException;

    public void htmlContentHandler(TikaInputStream inputStream, Metadata metadata) throws TikaException, SAXException, IOException;

    public void imageContentHandler(TikaInputStream tikaInputStream, Metadata metadata) throws IOException, TikaException, SAXException;
}
