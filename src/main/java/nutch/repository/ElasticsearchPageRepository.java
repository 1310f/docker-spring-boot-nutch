package nutch.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
/**
 * Created by marcel on 08-03-15.
 */
public interface ElasticsearchPageRepository extends ElasticsearchRepository<String, String> {

}
