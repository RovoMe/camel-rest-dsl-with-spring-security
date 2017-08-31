package at.rovo.awsxray.domain.entities;

import java.util.Date;
import java.util.UUID;
import org.mongodb.morphia.annotations.Entity;

@Entity(value = "file", noClassnameStored = true)
public class FileEntity extends BaseEntity {

    private String name;
    private String charset;
    private long size;
    private Date receivedAt;
    private String blobKey;

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

    @Override
    public String toString() {
        return "File [name=" + this.name
               + "; charset=" + this.charset
               + "; size=" + this.size
               + "; receivedAt=" + this.receivedAt
               + "; blobKey=" + this.blobKey + "]";
    }
}
