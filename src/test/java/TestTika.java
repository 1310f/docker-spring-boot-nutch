import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.LinkContentHandler;
import org.apache.tika.sax.TeeContentHandler;
import org.apache.tika.sax.ToHTMLContentHandler;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

/**
 * Created by marcel on 21-03-15.
 */
public class TestTika {
    private static final Logger log = LoggerFactory.getLogger(TestTika.class);


    @Test
    public void testHtml() throws IOException, TikaException, SAXException {
        Gson gson = new Gson();

        URL url = new URL("http://www.nu.nl");

        TikaConfig tikaConfig = new TikaConfig();
        Tika tika = new Tika(tikaConfig);
        Detector detector = tikaConfig.getDetector();

        TikaInputStream tikaInputStream = TikaInputStream.get(IOUtils.toByteArray(url));
        Metadata metadata = new Metadata();
        MediaType mediaType = detector.detect(tikaInputStream, metadata);

        ParseContext parseContext = new ParseContext();
        LinkContentHandler linkContentHandler = new LinkContentHandler();
        BodyContentHandler bodyContentHandler = new BodyContentHandler(10*1024*1024);
        ToHTMLContentHandler toHTMLContentHandler = new ToHTMLContentHandler();
        HtmlParser htmlParser = new HtmlParser();
        DefaultHandler defaultHandler = new DefaultHandler();

        TeeContentHandler teeContentHandler = new TeeContentHandler(linkContentHandler, bodyContentHandler, toHTMLContentHandler);

        AutoDetectParser autoDetectParser = new AutoDetectParser();
        autoDetectParser.parse(tikaInputStream,teeContentHandler,metadata);

        log.info("autodetect: ");

        log.info("defaultHandler(" + gson.toJson(defaultHandler) + ")");
        log.info("metadata(" + gson.toJson(metadata) + ")");
        log.info("parseContext(" + gson.toJson(parseContext) + ")");
        log.info("text(" + gson.toJson(bodyContentHandler.toString()) + ")");
        log.info("html("+gson.toJson(toHTMLContentHandler)+")");
        log.info("link(" + gson.toJson(linkContentHandler.getLinks()) + ")");

    }

    @Test
    public void simpleIndexHtml() throws IOException, TikaException {
        TikaConfig tikaConfig = new TikaConfig();
        Tika tika = new Tika(tikaConfig);
        URL url = new URL("http://www.nu.nl");
        Metadata metadata = new Metadata();

        InputStream inputStream = url.openStream();
        TikaInputStream tikaInputStream = TikaInputStream.get(inputStream);

        Reader reader = tika.parse(tikaInputStream,metadata);
        log.info("parser: " + tika.getParser().getClass());
        log.info("meta(" + metadata + ")");
        log.info("text("+IOUtils.toString(reader));
        log.info("contents("+IOUtils.toString(inputStream));

        // log.info("contents: " + IOUtils.toString(reader));
    }

    @Test
    public void testImage() throws IOException, TikaException, SAXException {
        Gson gson = new Gson();

        URL url = new URL("http://www.exiv2.org/include/img_1771.jpg");

        TikaConfig tikaConfig = new TikaConfig();
        Tika tika = new Tika(tikaConfig);
        Detector detector = tikaConfig.getDetector();

        TikaInputStream tikaInputStream = TikaInputStream.get(IOUtils.toByteArray(url));
        Metadata metadata = new Metadata();
        MediaType mediaType = detector.detect(tikaInputStream, metadata);

        log.info("meta: " + mediaType.toString());
        log.info("contents("+IOUtils.toByteArray(tikaInputStream).length+")");

        AutoDetectParser autoDetectParser = new AutoDetectParser();
        BodyContentHandler bodyContentHandler = new BodyContentHandler();
        autoDetectParser.parse(tikaInputStream,bodyContentHandler,metadata);

        log.info("autodetect: ");
        log.info("metadata(" + gson.toJson(metadata) + ")");
        log.info("body(" + tika.parse(tikaInputStream) + ")");

        log.info("length(" + tikaInputStream.getLength() + ")");
        log.info("contents("+IOUtils.toByteArray(tikaInputStream).length+")");

    }



}
