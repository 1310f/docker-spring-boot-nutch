package crawler.model;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.net.URL;
import java.util.Date;

/**
 * Created by marcel on 20-03-15.
 */
@Document(collection = "page")
public class Page implements Serializable {

    @Id
    ObjectId id;

    Date insertDate;

    public Date getIndexedDate() {
        return indexedDate;
    }

    public void setIndexedDate(Date indexedDate) {
        this.indexedDate = indexedDate;
    }

    Date indexedDate;

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    public Date getInsertDate() {
        return insertDate;
    }

    public void setInsertDate(Date insertDate) {
        this.insertDate = insertDate;
    }

    Date updateDate;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    @Indexed
    URL url;

    String html;
    // String text;

    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}

