package com.abdecd.moebackend.business.service.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggestOption;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.abdecd.moebackend.business.common.util.SpringContextUtil;
import com.abdecd.moebackend.business.dao.entity.SearchVideoGroupEntity;
import com.abdecd.moebackend.business.pojo.vo.videogroup.VideoGroupVO;
import com.abdecd.moebackend.business.pojo.vo.videogroup.VideoGroupWithDataVO;
import com.abdecd.moebackend.business.service.videogroup.VideoGroupServiceBase;
import com.abdecd.moebackend.common.constant.ElasticSearchConstant;
import com.abdecd.moebackend.common.result.PageVO;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@ConditionalOnProperty(prefix = "spring.data.elasticsearch", name = "url")
@Service
public class ElasticSearchService implements SearchService {
    @Autowired
    private ElasticsearchClient esClient;

    @Override
    public void initData(List<VideoGroupVO> videoGroups) throws IOException {
        List<BulkOperation> operations = new ArrayList<>();
        // 插入视频组
        for (var videoGroupVO : videoGroups) {
            if (videoGroupVO == null) continue;
            if (!esClient.exists(g -> g
                    .index(ElasticSearchConstant.INDEX_NAME)
                    .id(videoGroupVO.getId().toString())
            ).value()) {
                var searchNovelEntity = videoGroupVO.toSearchNovelEntity();
                operations.add(BulkOperation.of(o -> o
                        .index(i -> i.index(ElasticSearchConstant.INDEX_NAME)
                                .id(videoGroupVO.getId().toString())
                                .document(searchNovelEntity)
                        )));
            }
        }

        for (int i = 0; i < operations.size(); i += 1000) {
            int finalI = i;
            esClient.bulk(b -> b
                    .index(ElasticSearchConstant.INDEX_NAME)
                    .operations(operations.subList(finalI, Math.min(finalI + 1000, operations.size())))
            );
        }
    }

    @SneakyThrows
    @Override
    public void saveSearchEntity(VideoGroupVO videoGroupVO) {
        esClient.index(u -> u
                .index(ElasticSearchConstant.INDEX_NAME)
                .id(videoGroupVO.getId().toString())
                .document(videoGroupVO.toSearchNovelEntity())
        );
    }

    @SneakyThrows
    @Override
    public void deleteSearchEntity(Long id) {
        esClient.delete(u -> u
                .index(ElasticSearchConstant.INDEX_NAME)
                .id(id.toString())
        );
    }

    @Override
    public PageVO<VideoGroupWithDataVO> search(String keyword, Byte type, Integer page, Integer pageSize) {
        var strlen = keyword.replaceAll("\\s", "").length();
        String minimumShouldMatch;
        if (strlen > 5) minimumShouldMatch = "80%";
        else minimumShouldMatch = "100%";
        try {
            var response = esClient.search(s -> s
                .index(ElasticSearchConstant.INDEX_NAME)
                .query(q -> q
                    .functionScore(f -> f
                        .functions(f1 -> f1.fieldValueFactor(bb -> bb
                            .field("weight")
                            .missing(1.)
                        ))
                        .query(q1 -> q1.bool(b -> {
                            var query = b
                                .should(b1 -> b1.matchPhrase(b2 -> b2.field("title").query(keyword).slop(10).boost(2F)))
                                .should(b1 -> b1.matchPhrasePrefix(b2 -> b2.field("title").query(keyword).boost(1.5F)))
                                .should(b1 -> b1.matchPhrase(b2 -> b2.field("uploaderName").query(keyword).slop(1)))
                                .should(b1 -> b1.term(b2 -> b2.field("tags").value(keyword)))
                                .should(b1 -> b1.match(b2 -> b2.field("tags_text").query(keyword).minimumShouldMatch(minimumShouldMatch)))
                                .should(b1 -> b1.term(b2 -> b2.field("year").value(keyword).boost(10F)));
                            if (strlen > 5) {
                                query.should(b1 -> b1.matchPhrase(b2 -> b2.field("description").query(keyword).slop(1).boost(0.5F)));
                            }
                            if (type != null) {
                                query.filter(b1 -> b1.term(b2 -> b2.field("type").value(type)));
                            }
                            return query;
                        }))
                    )
                )
                .fields(f -> f.field("id"))
                .minScore(0.0001)
                .from(Math.max(0, (page - 1) * pageSize))
                .size(pageSize),
                SearchVideoGroupEntity.class
            );
            if (response.hits().total() == null || response.hits().total().value() == 0)
                return new PageVO<>(0, new ArrayList<>());
            List<SearchVideoGroupEntity> list = response.hits().hits().stream().map(Hit::source).toList();
            var videoGroupService = SpringContextUtil.getBean(VideoGroupServiceBase.class);
            return new PageVO<>(
                    Math.toIntExact(response.hits().total().value()),
                    list.stream().parallel()
                            .map(item -> videoGroupService.getVideoGroupWithData(item.getId())).toList()
            );
        } catch (ElasticsearchException e) {
            throw new RuntimeException(e.response().toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PageVO<VideoGroupWithDataVO> searchRelated(String keyword, Integer page, Integer pageSize) {
        try {
            var response = esClient.search(s -> s
                .index(ElasticSearchConstant.INDEX_NAME)
                .query(q -> q
                    .functionScore(f -> f
                        .functions(f1 -> f1.fieldValueFactor(bb -> bb
                            .field("weight")
                            .missing(1.)
                        ))
                        .query(q1 -> q1.bool(b -> b
                            .should(b1 -> b1.term(b2 -> b2.field("tags").value(keyword)))
                            .should(b1 -> b1.match(b2 -> b2.field("tags_text").query(keyword)))))
                    )
                )
                .fields(f -> f.field("id"))
                .minScore(0.0001)
                .from(Math.max(0, (page - 1) * pageSize))
                .size(pageSize),
                SearchVideoGroupEntity.class
            );
            if (response.hits().total() == null || response.hits().total().value() == 0)
                return new PageVO<>(0, new ArrayList<>());
            List<SearchVideoGroupEntity> list = response.hits().hits().stream().map(Hit::source).toList();
            var videoGroupService = SpringContextUtil.getBean(VideoGroupServiceBase.class);
            return new PageVO<>(
                    Math.toIntExact(response.hits().total().value()),
                    list.stream().parallel()
                            .map(item -> videoGroupService.getVideoGroupWithData(item.getId())).toList()
            );
        } catch (ElasticsearchException e) {
            throw new RuntimeException(e.response().toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    @Override
    public List<String> getSearchSuggestions(String keyword, Integer num) {
        var response = esClient.search(s -> s
                .index(ElasticSearchConstant.INDEX_NAME)
                .suggest(sug -> sug.suggesters("suggestion", sug2 -> sug2
                        .prefix(keyword)
                        .completion(f -> f
                                .field("suggestion")
                                .skipDuplicates(true)
                                .size(num)
                        )
                ))
                .fields(f -> f.field("id")),
                SearchVideoGroupEntity.class
        );
        return response.suggest().get("suggestion").getFirst().completion().options()
                .stream().map(CompletionSuggestOption::text).toList();
    }
}

/*
curl -X PUT "http://localhost:9200/moe-video-group" \
-H 'Content-Type: application/json' \
-d '
{
  "settings": {
    "analysis": {
      "analyzer": {
        "my_py": {
          "tokenizer": "ik_max_word",
          "filter": "py"
        },
        "my_suggestion_py": {
          "tokenizer": "keyword",
          "filter": "suggestion_py"
        }
      },
      "filter": {
        "py": {
          "type": "pinyin",
          "keep_first_letter": false,
          "keep_full_pinyin": false,
          "keep_joined_full_pinyin": true,
          "keep_original": true,
          "limit_first_letter_length": 16,
          "remove_duplicated_term": true,
          "none_chinese_pinyin_tokenize": false,
          "keep_none_chinese_in_joined_full_pinyin": true
        },
        "suggestion_py": {
          "type": "pinyin",
          "keep_full_pinyin": false,
          "keep_joined_full_pinyin": true,
          "keep_original": true,
          "keep_none_chinese": false,
          "limit_first_letter_length": 16,
          "remove_duplicated_term": true,
          "none_chinese_pinyin_tokenize": false,
          "keep_none_chinese_in_joined_full_pinyin": true,
          "ignore_pinyin_offset": false
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "id": {
        "type": "long",
        "index": "false"
      },
      "title": {
        "type": "text",
        "analyzer": "my_py",
        "search_analyzer": "ik_max_word"
      },
      "uploaderName": {
        "type": "text",
        "analyzer": "ik_max_word"
      },
      "description": {
        "type": "text",
        "analyzer": "ik_smart"
      },
      "tags": {
        "type": "keyword",
        "copy_to": "tags_text"
      },
      "tags_text": {
        "type": "text",
        "analyzer": "ik_smart"
      },
      "type": {
        "type": "byte"
      },
      "year": {
        "type": "keyword"
      },
      "weight": {
        "type": "double",
        "index": "false"
      },
      "suggestion": {
        "type": "completion",
        "analyzer": "my_suggestion_py",
        "search_analyzer": "keyword"
      }
    }
  }
}
'

curl -X DELETE "http://localhost:9200/moe-video-group"
 */