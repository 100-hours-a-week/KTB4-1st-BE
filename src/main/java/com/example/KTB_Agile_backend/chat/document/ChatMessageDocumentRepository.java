package com.example.KTB_Agile_backend.chat.document;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatMessageDocumentRepository extends MongoRepository<ChatMessageDocument, String> {
	List<ChatMessageDocument> findByChatRoomIdOrderByMessageIdDesc(Long chatRoomId, Pageable pageable);

	List<ChatMessageDocument> findByChatRoomIdAndMessageIdLessThanOrderByMessageIdDesc(
			Long chatRoomId,
			Long messageId,
			Pageable pageable
	);

	@Aggregation(pipeline = {
			"{ '$match': { 'chat_room_id': { '$in': ?0 } } }",
			"{ '$sort': { 'message_id': -1 } }",
			"{ '$group': { '_id': '$chat_room_id', 'message': { '$first': '$$ROOT' } } }",
			"{ '$replaceRoot': { 'newRoot': '$message' } }"
	})
	List<ChatMessageDocument> findLatestByChatRoomIds(List<Long> chatRoomIds);
}
