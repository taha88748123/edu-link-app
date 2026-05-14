package ma.naja7i.chat.infrastructure.persistence;

import ma.naja7i.chat.domain.ProcessedEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProcessedEventRepository extends MongoRepository<ProcessedEvent, UUID> {
}
