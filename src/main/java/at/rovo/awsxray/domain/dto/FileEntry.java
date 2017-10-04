package at.rovo.awsxray.domain.dto;

import at.rovo.awsxray.domain.entities.mongo.FileEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import dk.nykredit.jackson.dataformat.hal.HALLink;
import dk.nykredit.jackson.dataformat.hal.annotation.EmbeddedResource;
import dk.nykredit.jackson.dataformat.hal.annotation.Link;
import dk.nykredit.jackson.dataformat.hal.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Resource
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileEntry {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected String name;
    protected String charset;
    protected long size;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    protected Date createdAt;
    @Link
    protected HALLink self;
    protected String content;
    @EmbeddedResource
    protected List<SearchResults> searchResults;

    public FileEntry(FileEntity entity, String contextPath) {
        this.name = entity.getName();
        this.charset = entity.getCharset();
        this.size = entity.getSize();
        this.createdAt = entity.getReceivedAt();
        if (null != entity.getSearchResults()) {
            try {
                searchResults = entity.getSearchResults().stream()
                        .map(sr -> new SearchResults(sr.getUrl(), sr.getUrlDescr(), sr.getCite(), sr.getSubhead()))
                        .collect(Collectors.toList());

            } catch (Exception e) {
                LOG.warn("Could not add search results to response due to " + e.getLocalizedMessage(), e);
            }
        }
        String cp = contextPath;
        if (!contextPath.endsWith(entity.getUuid())) {
            cp = cp + "/" + entity.getUuid();
        }
        this.self = new HALLink.Builder(URI.create(cp)).build();
    }

    public FileEntry(FileEntity entity, String contextPath, byte[] content) {
        this(entity, contextPath);
        this.content = Base64.getEncoder().encodeToString(content);
    }

    @Resource
    @Getter
    public static class SearchResults {

        @Link
        protected HALLink url;
        @Link
        protected HALLink cite;
        protected String subHead;

        public SearchResults(String url, String urlDescr, String cite, String subHead) {
            this.url = new HALLink.Builder(URI.create(url)).title(urlDescr).build();
            try {
                this.cite = new HALLink.Builder(URI.create(cite)).build();
            } catch (Exception e) {
                LOG.warn("Could not create cite link due to " + e.getLocalizedMessage(), e);
            }
            this.subHead = subHead;
        }
    }
}
