package at.rovo.awsxray.domain.views;

import org.mongodb.morphia.annotations.Entity;

@Entity(value = "compUserView", noClassnameStored = true)
public class CompanyUserViewEntity extends AuthenticationUserViewEntity {

    private String companyUuid;

    public String getCompanyUuid() {
        return companyUuid;
    }

    @Override
    public String toString() {
        return "AuthenticationUserViewEntity [uuid=" + uuid + ", userId=" + userId + ", userKey=" + userKey
               + ", role=" + roles + ", company=" + companyUuid + "]";
    }
}
