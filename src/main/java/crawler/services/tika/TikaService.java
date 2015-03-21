package crawler.services.tika;

import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.image.ImageParser;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;

/**
 * Created by marcel on 08-03-15.
 */
public interface TikaService {
    MediaType detect(TikaInputStream tikaInputStream, Metadata metadata) throws IOException;

    public String htmlToString(TikaInputStream inputStream, Metadata metadata) throws IOException;

    public void htmlContentHandler(TikaInputStream tikaInputStream, Metadata metadata, ParseContext parseContext) throws TikaException, SAXException, IOException;
    public void imageContentHandler(TikaInputStream tikaInputStream, Metadata metadata, ImageParser imageParser, DefaultHandler defaultHandler, ParseContext parseContext) throws IOException, TikaException, SAXException;

}
