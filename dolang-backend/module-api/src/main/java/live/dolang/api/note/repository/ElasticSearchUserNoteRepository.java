package live.dolang.api.note.repository;

import live.dolang.api.note.document.UserNoteDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ElasticSearchUserNoteRepository extends ElasticsearchRepository<UserNoteDocument, String> {

    // 특정 유저의 모든 기록 조회

    Page<UserNoteDocument> findByUserId(Integer userId, Pageable pageable);

    /*
        특정 유저의 기록에서 특정 단어나 문장이 포함된 데이터 검색
        현재는 접두를 기준으로 검색 결과 제공.
     */
    @Query("""
        {
          "bool": {
            "filter": [
              { "term": { "user_id": "?0" } }
            ],
            "must": [
              { "multi_match": {
                "query": "?1",
                "fields": ["native_note", "interest_note"],
                "type": "phrase_prefix"
              }}
            ]
          }
        }
        """)
    Page<UserNoteDocument> searchNotesByUserIdAndKeyword(Integer userId, String keyword, Pageable pageable);

}