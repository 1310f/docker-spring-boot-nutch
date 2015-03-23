package crawler.configuration.tika;

import com.google.gson.Gson;

import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.parser.image.ImageParser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.LinkContentHandler;
import org.apache.tika.sax.TeeContentHandler;
import org.apache.tika.sax.ToHTMLContentHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Created by marcel on 07-03-15.
 */
@Configuration
public class TikaConfiguration {

    int MAX_BODY_LENGTH = 10*1024*1024;

    @Bean
    Tika tika() {
        Tika tika = new Tika();
        return tika;
    }

    @Bean
    TikaConfig tikaConfig() {
        TikaConfig tikaConfig = TikaConfig.getDefaultConfig();
        return tikaConfig;
    }

    @Bean
    Detector detector() {
        Detector detector = tikaConfig().getDetector();
        return detector;
    }


    @Bean
    LinkContentHandler linkHandler() {
        LinkContentHandler linkContentHandler = new LinkContentHandler();
        return linkContentHandler;
    }

    @Bean
    BodyContentHandler bodyContentHandler() {
        BodyContentHandler bodyContentHandler = new BodyContentHandler(MAX_BODY_LENGTH);
        return bodyContentHandler;
    }

    @Bean
    ToHTMLContentHandler toHTMLContentHandler() {
        ToHTMLContentHandler toHTMLContentHandler = new ToHTMLContentHandler();
        return toHTMLContentHandler;
    }

    @Bean
    TeeContentHandler teeContentHandler() {
        TeeContentHandler teeContentHandler = new TeeContentHandler(linkHandler(), bodyContentHandler(), toHTMLContentHandler());
        return teeContentHandler;
    }

    @Bean
    Metadata metadata() {
        Metadata metadata = new Metadata();
        return metadata;
    }

    @Bean
    ParseContext parseContext() {
        ParseContext parseContext = new ParseContext();
        return parseContext;
    }

    @Bean
    HtmlParser htmlParser() {
        HtmlParser htmlParser = new HtmlParser();
        return htmlParser;
    }

    @Bean
    ImageParser imageParser() {
        ImageParser imageParser = new ImageParser();
        return imageParser;
    }

    @Bean
    DefaultHandler defaultHandler() {
        DefaultHandler defaultHandler = new DefaultHandler();
        return defaultHandler;
    }
    Gson gson() {
        Gson gson = new Gson();
        return gson;
    }

}
