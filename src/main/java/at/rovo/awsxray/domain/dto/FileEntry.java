package at.rovo.awsxray.domain.dto;

import at.rovo.awsxray.domain.entities.mongo.FileEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import dk.nykredit.jackson.dataformat.hal.HALLink;
import dk.nykredit.jackson.dataformat.hal.annotation.Link;
import dk.nykredit.jackson.dataformat.hal.annotation.Resource;
import java.net.URI;
import java.util.Base64;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Resource
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileEntry {

    protected String name;
    protected String charset;
    protected long size;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    protected Date createdAt;
    @Link
    protected HALLink self;
    protected String content;

    public FileEntry(FileEntity entity, String contextPath) {
        this.name = entity.getName();
        this.charset = entity.getCharset();
        this.size = entity.getSize();
        this.createdAt = entity.getReceivedAt();
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
}
