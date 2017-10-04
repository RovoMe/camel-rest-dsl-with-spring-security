package at.rovo.awsxray.domain.entities.mongo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Transient;

@Entity(value = "file", noClassnameStored = true)
public class FileEntity extends BaseEntity {

    private String name;
    private String charset;
    private long size;
    private Date receivedAt;
    private String blobKey;
    @Transient
    private transient byte[] rawContent;

    private Map<String, Integer> terms;
    private List<SearchResults> searchResults;

    public FileEntity() {

    }

    public FileEntity(String name, String charset, long size) {
        this.name = name;
        this.charset = charset;
        this.size = size;
        this.receivedAt = new Date();
        this.setUuid(UUID.randomUUID().toString());
    }

    public String getName() {
        return this.name;
    }

    public String getCharset() {
        return this.charset;
    }

    public long getSize() {
        return this.size;
    }

    public Date getReceivedAt() {
        return this.receivedAt;
    }

    public String getBlobKey() {
        return blobKey;
    }

    public void setBlobKey(String blobKey) {
        this.blobKey = blobKey;
    }

    public byte[] getRawContent() {
        return this.rawContent;
    }

    public void setRawContent(byte[] rawContent) {
        this.rawContent = rawContent;
    }

    public Map<String, Integer> getFileTerms() {
        return terms;
    }

    public void setFileTerms(Map<String, Integer> terms) {
        this.terms = terms;
    }

    public List<SearchResults> getSearchResults() {
        return this.searchResults;
    }

    public void setSearchResults(List<SearchResults> searchResults) {
        this.searchResults = searchResults;
    }

    public void addSearchResult(String url, String urlDescr, String cite, String subhead) {
        if (null == this.searchResults) {
            this.searchResults = new ArrayList<>();
        }
        this.searchResults.add(new SearchResults(url, urlDescr, cite, subhead));
    }

    @Override
    public String toString() {
        return "File [name=" + this.name
               + "; charset=" + this.charset
               + "; size=" + this.size
               + "; receivedAt=" + this.receivedAt
               + "; blobKey=" + this.blobKey + "]";
    }

    @Getter
    @Setter
    public static class SearchResults {

        private String url;
        private String urlDescr;
        private String cite;
        private String subhead;

        // needed for Morphia instantiation
        public SearchResults() {

        }

        public SearchResults(String url, String urlDescr, String cite, String subhead) {
            this. url = url;
            this. urlDescr = urlDescr;
            this. cite = cite;
            this.subhead = subhead;
        }
    }
}
