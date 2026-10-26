# ADR 0001 — 참고 샘플 검색의 유사도 알고리즘

- 상태: 채택 (2026-10-26)
- 범위: `good_answer_sample` 벡터 검색. 연습 답변 평가에 쓸 참고 샘플(RAG)을 찾는다.
- 관련 코드
  - `GoodAnswerSampleRepositoryImpl.findNearest`
  - `SimilarityRanker`
  - `GoodAnswerSampleQueryService.findReferenceSamples`
  - `RagProperties`

## 배경

학습자 답변(전사 텍스트)과 비슷한 우수 답변 샘플을 같은 주제 안에서 찾아 평가 프롬프트에 넣는다. 샘플은 **표현 아이디어**를 주는 용도이고, 평가 기준은 별도의 역량 루브릭이다.

이전 구현은 다음과 같았다.

```sql
select * from good_answer_sample
where topic_id = :topicId [and level = :targetLevel] [and tag = :questionType]
order by embedding <-> :embedding   -- L2 거리
limit 3
```

이 구현에는 다음 문제가 있었다.

1. 유사도 하한선이 없다. 관련 없는 샘플도 무조건 3개가 들어간다.
2. 비슷한 샘플이 여러 개 있으면 거의 같은 샘플 3개가 뽑힌다.
3. level/tag를 하드 필터로 건다.
   - 관리자 업로드가 level을 항상 AL로 고정하고, tag는 자유 텍스트다. 그래서 필터는 거의 늘 비어 있고, 필터 없이 다시 검색하는 경로로 떨어졌다.
4. `embedding`이 NULL인 행도 후보에 포함된다.
5. 임베딩 HTTP 호출이 읽기 트랜잭션 안에서 실행돼, 호출하는 동안 DB 커넥션을 붙잡는다.
6. **샘플 저장이 실제로는 실패하는 버그가 있었다.** 컨버터가 반환한 `PGobject`를 Hibernate가 `bytea`로 바인딩해서, Postgres가 `column "embedding" is of type vector but expression is of type bytea`로 거부했다. 컨트롤러 테스트가 서비스를 mock해서 드러나지 않았다.
   - 수정: 컨버터는 `float[]`를 텍스트 `[a,b,...]`로 바꾼다. 엔티티 컬럼은 `@ColumnTransformer(write = "cast(? as vector)")`로, 검색 SQL은 `cast(:embedding as vector)`로 캐스팅한다. 실제 pgvector 위에서 도는 리포지토리 테스트로 보장한다.

임베딩은 OpenAI `text-embedding-3-small`(1536차원)이고 **단위 길이로 정규화된 벡터**를 돌려준다. Postgres에는 pgvector 0.8.7이 설치돼 있다.

## 결정

### 1. 거리 함수: 코사인 거리 `<=>`

| 후보 | 순위(정규화 벡터 기준) | 점수 해석 | 판단 |
|---|---|---|---|
| L2 `<->` | 코사인과 같음 | 0~2, 직관적이지 않음 | 하한선을 정하기 어렵다 |
| 내적 `<#>` | 코사인과 같음 | 음수로 반환(−내적), 정규화를 전제로 함 | 연산 하나를 아끼지만 이 규모에서는 의미가 없다 |
| **코사인 `<=>`** | 기준 | 유사도 = 1 − 거리, 범위 [−1, 1] | **채택** |

- 정규화된 벡터에서는 세 거리의 **순위가 같다**. 그래서 기존 L2 결과가 틀렸던 것은 아니다.
- 코사인을 고른 이유는 세 가지다.
  - (a) 점수가 해석 가능해서 하한선을 둘 수 있다.
  - (b) 정규화되지 않은 임베딩 모델로 바꿔도 의미가 유지된다.
  - (c) 나중에 쓸 인덱스 연산자 클래스 `vector_cosine_ops`와 일치한다.

### 2. 검색 방식: 정확 KNN + topic B-tree 인덱스 (ANN 인덱스 없음)

| 후보 | 재현율 | 필터와의 관계 | 운영 부담 | 판단 |
|---|---|---|---|---|
| **정확 KNN(순차 스캔)** | 100% | topic 필터가 먼저 적용된다 | 없음 | **채택** |
| HNSW | 근사 | 필터를 나중에 적용해 결과가 limit보다 적을 수 있다(0.8의 iterative scan으로 완화) | 메모리, 빌드 시간 | 규모가 커지면 전환 |
| IVFFlat | 근사 | 위와 같다 | 학습 데이터 필요, lists와 probes 튜닝 필요 | 소규모·필터 조건에서 재현율이 낮아 제외 |

- 검색은 항상 `topic_id = ?`로 거른다. 그래서 비용은 테이블 전체가 아니라 **주제 하나의 샘플 수(수십~수백)**에 비례한다. 이 규모에서는 정확 검색이 ms 단위로 끝나고 결과가 결정적이다.
- `topic_id` B-tree 인덱스(`idx_good_answer_sample_topic`)를 추가해 범위를 좁힌다.
- **HNSW로 바꾸는 조건**: 주제당 샘플 5~10k건 이상, 또는 검색 p95가 50ms를 넘을 때
  - `CREATE INDEX ... USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);`
  - 검색 시 `SET LOCAL hnsw.ef_search = 40; SET LOCAL hnsw.iterative_scan = relaxed_order;`

### 3. 선택 방식: 2단계 (후보 → 하한선 → MMR)

1. **후보**: SQL로 코사인 거리 상위 `candidates`(기본 12)개를 가져온다. `embedding is not null` 조건을 넣고, 동점이면 `id` 순으로 정한다.
2. **하한선**: 코사인 유사도가 `min-similarity`(기본 0.25)보다 낮은 후보는 버린다. 관련 없는 샘플이 프롬프트를 오염시키지 않게 하려는 것이다.
3. **MMR(Maximal Marginal Relevance)**: `top-k`(기본 3)개를 다음 식으로 하나씩 고른다.

   ```
   다음 선택 = argmax_{c ∉ S} [ λ·sim(q, c) − (1 − λ)·max_{s ∈ S} sim(c, s) ]     (λ 기본 0.7)
   ```

   질문과 관련이 높으면서 이미 고른 샘플과 겹치지 않는 샘플을 우선한다. 거의 같은 샘플 3개 대신 서로 다른 표현을 보여 주기 위해서다. 동점은 id로 정해 결과가 결정적이다.

- **하드 필터(level/tag)는 제거했다.** 데이터가 필터를 뒷받침하지 않는다(level은 항상 AL, tag는 자유 텍스트). 관리자 level 입력을 고친 뒤 데이터가 쌓이면, 필터 대신 **재정렬 가중치**로 다시 도입하는 것을 검토한다.
- 질의 텍스트는 학습자 transcript다. 샘플 임베딩(`sampleText`)과 같은 성격(답변 대 답변)이다. `min-query-words`(기본 15)보다 짧은 답변은 비교할 근거가 부족해 검색하지 않는다.
- 임베딩 호출은 트랜잭션 **밖**에서 하고, DB 조회와 DTO 변환만 짧은 읽기 트랜잭션에서 한다.

## 결과

- 장점: 관련 없는 샘플과 중복 샘플이 줄어든다. 점수를 로그로 남겨(`[RAG] ... similarities=[...]`) 하한선을 보정할 수 있다. 결과가 결정적이라 단위 테스트가 가능하다.
- 단점과 주의
  - 하한선 0.25는 초기 추정치다. 실제 로그를 보고 조정한다(`OPICER_RAG_MIN_SIMILARITY`).
  - 샘플이 적은 주제에서는 0~2개만 나올 수 있다. 이때 평가는 루브릭만으로 진행한다.
  - pgvector 확장은 앱이 만들지 않는다. Hibernate DDL이 먼저 실행되기 때문이다. DB마다 한 번 `CREATE EXTENSION IF NOT EXISTS vector;`를 실행해야 한다. 앱은 시작할 때 `VectorExtensionCheck`로 설치 여부만 로그에 남긴다.

## 설정값 (`opicer.rag.*`)

| 키 | 기본값 | 환경 변수 |
|---|---|---|
| candidates | 12 | `OPICER_RAG_CANDIDATES` |
| top-k | 3 | `OPICER_RAG_TOP_K` |
| min-similarity | 0.25 | `OPICER_RAG_MIN_SIMILARITY` |
| mmr-lambda | 0.7 | `OPICER_RAG_MMR_LAMBDA` |
| min-query-words | 15 | `OPICER_RAG_MIN_QUERY_WORDS` |

## 검증

- `SimilarityRankerTest`(순수 단위 테스트)
  - 코사인 값
  - 하한선 제외
  - MMR이 거의 같은 샘플을 밀어내고 다른 샘플을 고르는지
  - 동점 처리, k가 후보보다 큰 경우
- `GoodAnswerSampleRepositoryTest`(실제 pgvector)
  - 합성 단위 벡터로 코사인 순서
  - NULL 임베딩 제외, 다른 주제 제외
- `GoodAnswerSampleQueryServiceTest`(임베딩 mock + 실제 DB)
  - 2단계 선택 결과
  - 짧은 답변이면 임베딩을 호출하지 않는지
