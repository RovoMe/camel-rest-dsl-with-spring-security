package at.rovo.awsxray.domain.entities;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Address {
    private String street1;
    private String street2;
    private String zip;
    private String pob;
    private String city;
    private String country;
}
