package ma.naja7i.chat.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

import ma.naja7i.chat.domain.Conversation;
import ma.naja7i.chat.domain.Message;

/**
 * Ensures the Mongo indexes required by the read paths are created at startup.
 * Spring Data Mongo auto-index creation is disabled by default since Spring Boot 3,
 * so we declare them explicitly here.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class MongoConfig {

    private final MongoTemplate mongoTemplate;

    @PostConstruct
    public void ensureIndexes() {
        IndexOperations conv = mongoTemplate.indexOps(Conversation.class);
        conv.ensureIndex(new Index().on("bookingId", Sort.Direction.ASC).unique());
        conv.ensureIndex(new Index().on("profId", Sort.Direction.ASC));
        conv.ensureIndex(new Index().on("studentId", Sort.Direction.ASC));
        conv.ensureIndex(new Index().on("status", Sort.Direction.ASC));
        conv.ensureIndex(new Index().on("lastMessageAt", Sort.Direction.DESC));

        IndexOperations msg = mongoTemplate.indexOps(Message.class);
        msg.ensureIndex(new Index()
                .on("conversationId", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.DESC));
        msg.ensureIndex(new Index().on("bookingId", Sort.Direction.ASC));

        log.info("Mongo indexes ensured (conversations + messages)");
    }
}
