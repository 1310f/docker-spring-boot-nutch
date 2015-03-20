package crawler.services.tika;

import com.google.gson.Gson;
import crawler.model.Page;
import crawler.repository.MongoPageRepository;
import crawler.services.rabbitmq.RabbitMQService;
import org.apache.tika.Tika;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.IOUtils;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.parser.image.ImageParser;
import org.apache.tika.sax.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;

/**
 * Created by marcel on 08-03-15.
 */
public class TikaServiceImpl implements TikaService {

    private static final Logger log = LoggerFactory.getLogger(TikaServiceImpl.class);


    @Autowired
    Tika tika;


    public String htmlToString(TikaInputStream inputStream, Metadata metadata) throws IOException {
        Reader reader = tika.parse(inputStream, metadata);
        String page = IOUtils.toString(reader);
        reader.close();
        return page;
    }

    @Autowired
    HtmlParser htmlParser;

    @Autowired
    TeeContentHandler teeContentHandler;

    @Autowired
    Metadata metaData;


    @Autowired
    LinkContentHandler linkHandler;

    @Autowired
    ToHTMLContentHandler toHTMLContentHandler;

    @Autowired
    BodyContentHandler bodyContentHandler;

    @Autowired
    Detector detector;

    @Autowired
    Gson gson;

    @Autowired
    MongoTemplate mongoTemplate;

    // @Autowired
    // ElasticsearchPageRepository elasticsearchPageRepository;

    @Autowired
    MongoPageRepository mongoPageRepository;

    public void htmlContentHandler(TikaInputStream tikaInputStream, Metadata metadata) throws TikaException, SAXException, IOException {
        // mongoHtmlPageRepository.findOne(metadata.get(Metadata.RESOURCE_NAME_KEY));
        URL url = new URL(metadata.get(Metadata.RESOURCE_NAME_KEY));
        List<Page> pages = mongoPageRepository.findByUrl(url);
        String html = toHTMLContentHandler.toString();

        if(pages.size()==0) {
            // String text = bodyContentHandler.toString();

            Page page = new Page();
            page.setUrl(url);
            page.setHtml(html);
            page.setInsertDate(new Date());
            mongoPageRepository.save(page);
        } else {
            Page page = pages.get(0);
            page.setIndexedDate(new Date());
            page.setUpdateDate(new Date());
            page.setHtml(html);
            mongoPageRepository.save(page);

            // log.info("url("+url+") already indexed on " + pages.get(0).getUpdateDate().toString());
        }

        htmlParser.parse(tikaInputStream, teeContentHandler, metaData, parseContext);

        for(Link link : linkHandler.getLinks()) {
            String path = null;

            if(link.getUri().startsWith("http://")) {
                path = link.getUri();
            } else if(link.getUri().startsWith("https://")) {
                path = link.getUri();
            } else if(link.getUri().startsWith("#")) {
                // path = link.getUri();

            } else if((!metadata.get(Metadata.RESOURCE_NAME_KEY).contains("#") && !link.getUri().contains("#"))) {
                if(!link.getUri().startsWith("/")) {
                    String parent = metadata.get(Metadata.RESOURCE_NAME_KEY);
                    path = metadata.get(Metadata.RESOURCE_NAME_KEY) + link.getUri();
                } else {
                    String parent = metadata.get(Metadata.RESOURCE_NAME_KEY);
                    URL parentUrl = new URL(parent);
                    path = "http://"+parentUrl.getHost()+link.getUri();
                }
            } else {
                // log.warn("link("+link.getUri()+"), path("+path+"): not supported!");
            }

            if(path!=null) {
                try {
                    URL linkUrl = new URL(path);
                    if (url.getHost().endsWith("onion")) {
                        List<Page> linkPages = mongoPageRepository.findByUrl(linkUrl);
                        if(linkPages.size()==0) {
                            if(linkUrl.getHost().endsWith(".onion")) {
                                log.info("link(" + linkUrl +")");

                                Page page = new Page();
                                page.setUrl(linkUrl);
                                page.setInsertDate(new Date());
                                mongoPageRepository.save(page);
                                rabbitMQService.sendUrlToBroker(path);
                            }
                        }
                    }
                } catch (MalformedURLException e) {
                    log.error("Exception: for link(" + link.getUri() + ") ", e);
                }
            } else {
                // log.warn("path null: link("+link.getUri()+"), path("+path+")");
            }
        }
        // log.info("text:\n" + bodyContentHandler.toString());
        // log.info("html:\n" + toHTMLContentHandler.toString());


    }



    @Autowired
    RabbitMQService rabbitMQService;

    @Autowired
    ImageParser imageParser;
    @Autowired
    ParseContext parseContext;

    @Autowired
    DefaultHandler defaultHandler;



    // meta: X-Parsed-By=org.apache.tika.parser.DefaultParser X-Parsed-By=org.apache.tika.parser.image.ImageParser Dimension HorizontalPixelOffset=0 tiff:ImageLength=62 Compression CompressionTypeName=lzw resourceName=http://xcomics5vvoiary2.onion/Alraune1/default/logo.gif GraphicControlExtension=disposalMethod=none, userInputFlag=false, transparentColorFlag=false, delayTime=0, transparentColorIndex=0 Compression NumProgressiveScans=1 Chroma ColorSpaceType=RGB Chroma BlackIsZero=true Compression Lossless=true width=200 Dimension ImageOrientation=Normal ImageDescriptor=imageLeftPosition=0, imageTopPosition=0, imageWidth=200, imageHeight=62, interlaceFlag=false Dimension VerticalPixelOffset=0 tiff:ImageWidth=200 Chroma NumChannels=3 Data SampleFormat=Index Content-Type=image/gif height=62

    public void imageContentHandler(TikaInputStream tikaInputStream, Metadata metadata) throws IOException, TikaException, SAXException {

        imageParser.parse(tikaInputStream,defaultHandler,metadata,parseContext);


        log.info("meta(" + gson.toJson(metadata)+"), defaultHandler("+gson.toJson(defaultHandler)+"), parseContext("+gson.toJson(parseContext)+")");
    }

    public MediaType detect(TikaInputStream tikaInputStream, Metadata metadata) throws IOException {
        MediaType mediaType = detector.detect(tikaInputStream, metadata);
        return mediaType;
    }



}
