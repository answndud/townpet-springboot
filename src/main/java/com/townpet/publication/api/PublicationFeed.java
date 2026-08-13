package com.townpet.publication.api;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import com.townpet.member.api.MemberDirectory;
import com.townpet.relationship.api.BlockDirectory;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PublicationFeed {
  private static final Table<?> PUBLICATION = table(name("publication")).as("p");
  private static final Field<UUID> ID = field(name("p", "id"), UUID.class);
  private static final Field<UUID> AUTHOR_ID = field(name("p", "author_member_id"), UUID.class);
  private static final Field<String> TYPE = field(name("p", "type"), String.class);
  private static final Field<String> SCOPE = field(name("p", "scope"), String.class);
  private static final Field<UUID> NEIGHBORHOOD_ID =
      field(name("p", "neighborhood_id"), UUID.class);
  private static final Field<String> ANIMAL_INTEREST_CODE =
      field(name("p", "animal_interest_code"), String.class);
  private static final Field<String> TITLE = field(name("p", "title"), String.class);
  private static final Field<String> BODY = field(name("p", "body"), String.class);
  private static final Field<String> LIFECYCLE = field(name("p", "lifecycle"), String.class);
  private static final Field<OffsetDateTime> CREATED_AT =
      field(name("p", "created_at"), OffsetDateTime.class);
  private static final Field<OffsetDateTime> UPDATED_AT =
      field(name("p", "updated_at"), OffsetDateTime.class);
  private static final Field<Long> VERSION = field(name("p", "version"), Long.class);

  private final DSLContext query;
  private final MemberDirectory members;
  private final BlockDirectory blocks;

  PublicationFeed(DSLContext query, MemberDirectory members, BlockDirectory blocks) {
    this.query = query;
    this.members = members;
    this.blocks = blocks;
  }

  @Transactional(readOnly = true)
  public Page list(
      @Nullable UUID viewerMemberId,
      boolean includeViewerNeighborhood,
      @Nullable String encodedCursor,
      int limit) {
    return list(viewerMemberId, includeViewerNeighborhood, encodedCursor, limit, null);
  }

  @Transactional(readOnly = true)
  public Page popular(int limit) {
    PopularPage page = popularPage(limit, null, null, "ALL");
    return new Page(
        page.items().stream().map(PopularItem::publication).toList(),
        page.nextCursor(),
        page.hasNext(),
        page.totalPages());
  }

  @Transactional(readOnly = true)
  public List<PopularItem> popularRanked(int limit) {
    return popularRanked(limit, null, "ALL");
  }

  @Transactional(readOnly = true)
  public List<PopularItem> popularRanked(
      int limit, @Nullable String searchQuery, String searchField) {
    return popularPage(limit, null, searchQuery, searchField).items();
  }

  @Transactional(readOnly = true)
  public PopularPage popularPage(
      int limit, @Nullable String encodedCursor, @Nullable String searchQuery, String searchField) {
    if (limit < 1 || limit > 50) throw new IllegalArgumentException("Invalid popular limit");
    if (searchQuery != null && searchQuery.length() > 80) {
      throw new IllegalArgumentException("Invalid feed query");
    }
    if (!searchField.equalsIgnoreCase("ALL")
        && !searchField.equalsIgnoreCase("TITLE")
        && !searchField.equalsIgnoreCase("BODY")) {
      throw new IllegalArgumentException("Invalid feed search field");
    }
    Table<?> REACTION = table(name("engagement_reaction")).as("r");
    Field<UUID> REACTION_PUBLICATION_ID = field(name("r", "publication_id"), UUID.class);
    Field<String> REACTION_TYPE = field(name("r", "type"), String.class);
    Table<?> RECOMMENDATIONS =
        query
            .select(
                REACTION_PUBLICATION_ID.as("publication_id"),
                org.jooq.impl.DSL.count().cast(Long.class).as("recommendation_count"))
            .from(REACTION)
            .where(REACTION_TYPE.eq("LIKE"))
            .groupBy(REACTION_PUBLICATION_ID)
            .asTable("recommendation_count");
    Field<UUID> RECOMMENDATION_PUBLICATION_ID =
        field(name("recommendation_count", "publication_id"), UUID.class);
    Field<Long> RECOMMENDATION_TOTAL =
        field(name("recommendation_count", "recommendation_count"), Long.class);
    PopularCursor cursor = encodedCursor == null ? null : PopularCursor.decode(encodedCursor);
    Condition condition = popularCondition(searchQuery, searchField);
    long totalCount =
        query
            .selectCount()
            .from(PUBLICATION)
            .join(RECOMMENDATIONS)
            .on(RECOMMENDATION_PUBLICATION_ID.eq(ID))
            .where(condition)
            .fetchOne(0, Long.class);
    int totalPages = Math.toIntExact((totalCount + limit - 1) / limit);
    if (cursor != null) {
      OffsetDateTime cursorTime = cursor.createdAt().atOffset(ZoneOffset.UTC);
      condition =
          condition.and(
              RECOMMENDATION_TOTAL
                  .lt(cursor.recommendationCount())
                  .or(
                      RECOMMENDATION_TOTAL
                          .eq(cursor.recommendationCount())
                          .and(
                              CREATED_AT
                                  .lt(cursorTime)
                                  .or(CREATED_AT.eq(cursorTime).and(ID.lt(cursor.id()))))));
    }
    List<PopularItem> fetched =
        query
            .select(
                ID,
                AUTHOR_ID,
                TYPE,
                SCOPE,
                NEIGHBORHOOD_ID,
                ANIMAL_INTEREST_CODE,
                TITLE,
                BODY,
                LIFECYCLE,
                CREATED_AT,
                UPDATED_AT,
                VERSION,
                RECOMMENDATION_TOTAL)
            .from(PUBLICATION)
            .join(RECOMMENDATIONS)
            .on(RECOMMENDATION_PUBLICATION_ID.eq(ID))
            .where(condition)
            .orderBy(RECOMMENDATION_TOTAL.desc(), CREATED_AT.desc(), ID.desc())
            .limit(limit + 1)
            .fetch(
                record ->
                    new PopularItem(toItem(record), valueOrZero(record.get(RECOMMENDATION_TOTAL))));
    boolean hasNext = fetched.size() > limit;
    List<PopularItem> items =
        hasNext ? List.copyOf(fetched.subList(0, limit)) : List.copyOf(fetched);
    String nextCursor =
        hasNext
            ? PopularCursor.encode(
                items.getLast().recommendationCount(),
                items.getLast().publication().createdAt(),
                items.getLast().publication().id())
            : null;
    return new PopularPage(items, nextCursor, hasNext, totalPages);
  }

  private static Condition popularCondition(@Nullable String searchQuery, String searchField) {
    Condition condition = LIFECYCLE.eq("ACTIVE").and(SCOPE.eq("GLOBAL"));
    if (searchQuery != null && !searchQuery.isBlank()) {
      String term = "%" + searchQuery.trim().toLowerCase(Locale.ROOT) + "%";
      if (searchField.equalsIgnoreCase("TITLE"))
        condition = condition.and(TITLE.likeIgnoreCase(term));
      else if (searchField.equalsIgnoreCase("BODY"))
        condition = condition.and(BODY.likeIgnoreCase(term));
      else condition = condition.and(TITLE.likeIgnoreCase(term).or(BODY.likeIgnoreCase(term)));
    }
    return condition;
  }

  private static Item toItem(Record record) {
    return new Item(
        record.get(ID),
        record.get(TYPE),
        record.get(TITLE),
        record.get(BODY),
        record.get(SCOPE),
        record.get(AUTHOR_ID),
        record.get(NEIGHBORHOOD_ID),
        record.get(ANIMAL_INTEREST_CODE),
        record.get(LIFECYCLE),
        record.get(CREATED_AT).toInstant(),
        record.get(UPDATED_AT).toInstant(),
        record.get(VERSION));
  }

  private static long valueOrZero(@Nullable Long value) {
    return value == null ? 0L : value;
  }

  @Transactional(readOnly = true)
  public Page list(
      @Nullable UUID viewerMemberId,
      boolean includeViewerNeighborhood,
      @Nullable String encodedCursor,
      int limit,
      @Nullable String searchQuery) {
    return list(viewerMemberId, includeViewerNeighborhood, encodedCursor, limit, searchQuery, null);
  }

  public record PopularItem(Item publication, long recommendationCount) {}

  public record PopularPage(
      List<PopularItem> items,
      @Nullable String nextCursor,
      boolean hasNext,
      int totalPages) {}

  @Transactional(readOnly = true)
  public Page list(
      @Nullable UUID viewerMemberId,
      boolean includeViewerNeighborhood,
      @Nullable String encodedCursor,
      int limit,
      @Nullable String searchQuery,
      @Nullable String scopeFilter) {
    return list(
        viewerMemberId,
        includeViewerNeighborhood,
        encodedCursor,
        limit,
        searchQuery,
        scopeFilter,
        null,
        null,
        null,
        null);
  }

  @Transactional(readOnly = true)
  public Page list(
      @Nullable UUID viewerMemberId,
      boolean includeViewerNeighborhood,
      @Nullable String encodedCursor,
      int limit,
      @Nullable String searchQuery,
      @Nullable String scopeFilter,
      @Nullable Instant from,
      @Nullable Instant to) {
    return list(
        viewerMemberId,
        includeViewerNeighborhood,
        encodedCursor,
        limit,
        searchQuery,
        scopeFilter,
        from,
        to,
        null,
        null);
  }

  @Transactional(readOnly = true)
  public Page list(
      @Nullable UUID viewerMemberId,
      boolean includeViewerNeighborhood,
      @Nullable String encodedCursor,
      int limit,
      @Nullable String searchQuery,
      @Nullable String scopeFilter,
      @Nullable Instant from,
      @Nullable Instant to,
      @Nullable Set<String> animalInterestCodes,
      @Nullable String publicationType) {
    if (limit < 1 || limit > 50) throw new IllegalArgumentException("Invalid feed limit");
    if (from != null && to != null && !to.isAfter(from)) {
      throw new IllegalArgumentException("Invalid feed date range");
    }
    if (searchQuery != null && searchQuery.length() > 80) {
      throw new IllegalArgumentException("Invalid feed query");
    }
    Cursor cursor = encodedCursor == null ? null : Cursor.decode(encodedCursor);
    UUID viewerNeighborhoodId =
        viewerMemberId == null || !includeViewerNeighborhood
            ? null
            : members
                .findPublicationContext(viewerMemberId)
                .map(MemberDirectory.MemberPublicationContext::neighborhoodId)
                .orElse(null);
    Condition visible = SCOPE.eq("GLOBAL");
    if (viewerNeighborhoodId != null)
      visible = visible.or(SCOPE.eq("LOCAL").and(NEIGHBORHOOD_ID.eq(viewerNeighborhoodId)));
    if (scopeFilter != null && !scopeFilter.isBlank()) {
      if (scopeFilter.equalsIgnoreCase("LOCAL"))
        visible = SCOPE.eq("LOCAL").and(NEIGHBORHOOD_ID.eq(viewerNeighborhoodId));
      else if (scopeFilter.equalsIgnoreCase("GLOBAL")) visible = SCOPE.eq("GLOBAL");
    }
    Condition condition = LIFECYCLE.eq("ACTIVE").and(visible);
    if (from != null) condition = condition.and(CREATED_AT.ge(from.atOffset(ZoneOffset.UTC)));
    if (to != null) condition = condition.and(CREATED_AT.lt(to.atOffset(ZoneOffset.UTC)));
    if (searchQuery != null && !searchQuery.isBlank()) {
      String term = "%" + searchQuery.trim().toLowerCase(Locale.ROOT) + "%";
      condition = condition.and(TITLE.likeIgnoreCase(term).or(BODY.likeIgnoreCase(term)));
    }
    if (publicationType != null && !publicationType.isBlank()) {
      condition = condition.and(TYPE.eq(publicationType));
    }
    if (animalInterestCodes != null) {
      condition =
          animalInterestCodes.isEmpty()
              ? condition.and(ANIMAL_INTEREST_CODE.isNull())
              : condition.and(
                  ANIMAL_INTEREST_CODE.isNull().or(ANIMAL_INTEREST_CODE.in(animalInterestCodes)));
    }
    if (viewerMemberId != null && includeViewerNeighborhood) {
      Set<UUID> blockedAuthorIds = blocks.blockedAuthorIds(viewerMemberId);
      if (!blockedAuthorIds.isEmpty()) condition = condition.and(AUTHOR_ID.notIn(blockedAuthorIds));
    }
    long totalCount = query.selectCount().from(PUBLICATION).where(condition).fetchOne(0, Long.class);
    int totalPages = Math.toIntExact((totalCount + limit - 1) / limit);
    if (cursor != null) {
      OffsetDateTime cursorTime = cursor.createdAt().atOffset(ZoneOffset.UTC);
      condition =
          condition.and(
              CREATED_AT.lt(cursorTime).or(CREATED_AT.eq(cursorTime).and(ID.lt(cursor.id()))));
    }

    List<Item> fetched =
        query
            .select(
                ID,
                AUTHOR_ID,
                TYPE,
                SCOPE,
                NEIGHBORHOOD_ID,
                ANIMAL_INTEREST_CODE,
                TITLE,
                BODY,
                LIFECYCLE,
                CREATED_AT,
                UPDATED_AT,
                VERSION)
            .from(PUBLICATION)
            .where(condition)
            .orderBy(CREATED_AT.desc(), ID.desc())
            .limit(limit + 1)
            .fetch(
                record ->
                    new Item(
                        record.get(ID),
                        record.get(TYPE),
                        record.get(TITLE),
                        record.get(BODY),
                        record.get(SCOPE),
                        record.get(AUTHOR_ID),
                        record.get(NEIGHBORHOOD_ID),
                        record.get(ANIMAL_INTEREST_CODE),
                        record.get(LIFECYCLE),
                        record.get(CREATED_AT).toInstant(),
                        record.get(UPDATED_AT).toInstant(),
                        record.get(VERSION)));

    boolean hasNext = fetched.size() > limit;
    List<Item> items = hasNext ? List.copyOf(fetched.subList(0, limit)) : List.copyOf(fetched);
    String nextCursor =
        hasNext ? Cursor.encode(items.getLast().createdAt(), items.getLast().id()) : null;
    return new Page(items, nextCursor, hasNext, totalPages);
  }

  public record Page(List<Item> items, @Nullable String nextCursor, boolean hasNext, int totalPages) {}

  public record Item(
      UUID id,
      String type,
      String title,
      String body,
      String scope,
      UUID authorId,
      @Nullable UUID neighborhoodId,
      @Nullable String animalInterestCode,
      String lifecycle,
      Instant createdAt,
      Instant updatedAt,
      long version) {}

  private record Cursor(Instant createdAt, UUID id) {
    static String encode(Instant createdAt, UUID id) {
      String value = createdAt + "|" + id;
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    static Cursor decode(String encoded) {
      try {
        String value = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
        int separator = value.indexOf('|');
        if (separator <= 0 || separator != value.lastIndexOf('|')) {
          throw new IllegalArgumentException("Invalid feed cursor");
        }
        return new Cursor(
            Instant.parse(value.substring(0, separator)),
            UUID.fromString(value.substring(separator + 1)));
      } catch (IllegalArgumentException exception) {
        throw new IllegalArgumentException("Invalid feed cursor", exception);
      }
    }
  }

  private record PopularCursor(long recommendationCount, Instant createdAt, UUID id) {
    static String encode(long recommendationCount, Instant createdAt, UUID id) {
      String value = "v1|" + recommendationCount + "|" + createdAt + "|" + id;
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    static PopularCursor decode(String encoded) {
      try {
        String value = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
        String[] parts = value.split("\\|", -1);
        if (parts.length != 4 || !parts[0].equals("v1")) {
          throw new IllegalArgumentException("Invalid popular cursor");
        }
        return new PopularCursor(
            Long.parseLong(parts[1]), Instant.parse(parts[2]), UUID.fromString(parts[3]));
      } catch (IllegalArgumentException exception) {
        throw new IllegalArgumentException("Invalid popular cursor", exception);
      }
    }
  }
}
