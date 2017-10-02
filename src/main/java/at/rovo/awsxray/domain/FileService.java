package at.rovo.awsxray.domain;

import at.rovo.awsxray.domain.entities.mongo.FileEntity;
import java.util.List;
import org.mongodb.morphia.query.FindOptions;
import org.springframework.stereotype.Service;

@Service
public class FileService extends BaseMongoService<FileEntity> {

    public FileService() {
        super(FileEntity.class);
    }

    public long countFiles() {
        return mongoDataStore.getCount(FileEntity.class);
    }

    public List<FileEntity> listFiles(int limit, int offset) {
        return mongoDataStore.find(FileEntity.class).asList(new FindOptions().skip(offset).limit(limit));
    }
}
