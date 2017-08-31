package at.rovo.awsxray.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import dk.nykredit.jackson.dataformat.hal.HALLink;
import dk.nykredit.jackson.dataformat.hal.annotation.EmbeddedResource;
import dk.nykredit.jackson.dataformat.hal.annotation.Link;
import java.net.URI;
import java.util.List;
import javax.annotation.Resource;
import lombok.Getter;
import lombok.Setter;

@Resource
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListFilesResponse {
    @EmbeddedResource
    private List<FileEntry> files;

    @Link
    private HALLink self;
    @Link("rel:prev")
    private HALLink prev;
    @Link("rel:next")
    private HALLink next;
    @Link("rel:first")
    private HALLink first;
    @Link("rel:last")
    private HALLink last;

    public ListFilesResponse(List<FileEntry> files, String contextPath, int limit, int offset, long totalItems) {

        long endOffset;
        if (totalItems % limit == 0) {
            endOffset = (totalItems / limit - 1) * limit;
        } else {
            endOffset = (totalItems / limit) * limit;
        }
        this.files = files;

        URI selfUri  = URI.create(contextPath + "?limit=" + limit + "&offset=" + offset);

        this.self = new HALLink.Builder(selfUri).build();
        if (offset > 0) {
            URI firstUri = URI.create(contextPath + "?limit=" + limit + "&offset=0");
            this.first = new HALLink.Builder(firstUri).build();
        } else {
            this.first = null;
        }
        if (offset+limit < totalItems) {
            URI nextUri = URI.create(contextPath + "?limit=" + limit + "&offset=" + (offset + limit));
            this.next = new HALLink.Builder(nextUri).build();
        } else {
            this.next = null;
        }
        if (offset >= limit) {
            URI prevUri = URI.create(contextPath + "?limit=" + limit + "&offset=" + (offset - limit));
            this.prev = new HALLink.Builder(prevUri).build();
        } else {
            this.prev = null;
        }
        if (endOffset > offset) {
            URI lastUri = URI.create(contextPath + "?limit=" + limit + "&offset=" + endOffset);
            this.last = new HALLink.Builder(lastUri).build();
        } else {
            this.last = null;
        }
    }
}
