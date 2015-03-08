package nutch.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by marcel on 08-03-15.
 */
@Repository
public interface MongoHtmlPageRepository extends MongoRepository<String, String> {

}
