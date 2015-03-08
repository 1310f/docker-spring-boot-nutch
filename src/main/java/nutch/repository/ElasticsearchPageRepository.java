package nutch.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
/**
 * Created by marcel on 08-03-15.
 */
public class ElasticsearchPageRepository implements ElasticsearchRepository<String, String> {

    public Customer findByFirstName(String firstName);

    public List<Customer> findByLastName(String lastName);

}
