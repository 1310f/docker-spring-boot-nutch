package crawler.repository;

import crawler.model.Page;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.net.URL;
import java.util.List;

/**
 * Created by marcel on 08-03-15.
 */
public interface MongoPageRepository extends PagingAndSortingRepository<Page, String> {
    public List<Page> findByUrl(URL url);
}
