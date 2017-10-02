package at.rovo.awsxray.domain;

import at.rovo.awsxray.domain.entities.mongo.CompanyEntity;
import com.mongodb.WriteResult;
import org.mongodb.morphia.query.Query;
import org.springframework.stereotype.Service;

@Service
public class CompanyService extends BaseMongoService<CompanyEntity> {

    public CompanyService() {
        super(CompanyEntity.class);
    }

    public CompanyEntity findCompany(String companyUuid) {
        if ((companyUuid == null) || companyUuid.isEmpty()) {
            return null;
        }

        Query<CompanyEntity> query = mongoDataStore.find(CompanyEntity.class).field("uuid").equal(companyUuid);
        return query.get();
    }

    public CompanyEntity findByGln(String gln) {
        if ((gln == null) || gln.isEmpty()) {
            return null;
        }

        Query<CompanyEntity> query = mongoDataStore.find(CompanyEntity.class).field("gln").equal(gln);
        return query.get();
    }

    public int dropAll() {
        WriteResult result = mongoDataStore.delete(mongoDataStore.find(CompanyEntity.class));
        return result.getN();
    }
}
