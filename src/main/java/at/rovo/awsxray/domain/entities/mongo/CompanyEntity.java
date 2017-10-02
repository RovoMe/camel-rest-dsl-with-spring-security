package at.rovo.awsxray.domain.entities.mongo;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;

@Entity(value = "company", noClassnameStored = true)
@Indexes(@Index(fields = {@Field("gln")}, options = @IndexOptions(unique = true)))
public class CompanyEntity extends BaseEntity {

    private static final long serialVersionUID = 5250718288568350574L;

    private String name;
    private String gln;

    @Embedded
    private Address address;

    public CompanyEntity() {

    }

    public CompanyEntity(String name, String gln) {
        this.name = name;
        this.gln = gln;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGln() {
        return this.gln;
    }

    public void setGln(String gln) {
        this.gln = gln;
    }

    public Address getAddress() {
        return this.address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }
}
