package com.townpet.discovery;

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
import org.jooq.impl.DSL;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class CommunityFeed {
  private static final Table<?> ITEMS = DSL.table(DSL.name("townpet_public_feed_item")).as("f");
  private static final Field<UUID> SOURCE_ID = DSL.field(DSL.name("f", "source_id"), UUID.class);
  private static final Field<String> ITEM_KIND =
      DSL.field(DSL.name("f", "item_kind"), String.class);
  private static final Field<String> ITEM_TYPE =
      DSL.field(DSL.name("f", "item_type"), String.class);
  private static final Field<String> TITLE = DSL.field(DSL.name("f", "title"), String.class);
  private static final Field<String> SUMMARY = DSL.field(DSL.name("f", "summary"), String.class);
  private static final Field<String> SCOPE = DSL.field(DSL.name("f", "scope"), String.class);
  private static final Field<UUID> AUTHOR_ID =
      DSL.field(DSL.name("f", "author_member_id"), UUID.class);
  private static final Field<UUID> NEIGHBORHOOD_ID =
      DSL.field(DSL.name("f", "neighborhood_id"), UUID.class);
  private static final Field<String> ANIMAL_INTEREST_CODE =
      DSL.field(DSL.name("f", "animal_interest_code"), String.class);
  private static final Field<String> STATUS = DSL.field(DSL.name("f", "status"), String.class);
  private static final Field<OffsetDateTime> CREATED_AT =
      DSL.field(DSL.name("f", "created_at"), OffsetDateTime.class);
  private static final Field<OffsetDateTime> UPDATED_AT =
      DSL.field(DSL.name("f", "updated_at"), OffsetDateTime.class);
  private static final Field<String> TARGET_PATH =
      DSL.field(DSL.name("f", "target_path"), String.class);

  private final DSLContext query;
  private final MemberDirectory members;
  private final BlockDirectory blocks;

  CommunityFeed(DSLContext query, MemberDirectory members, BlockDirectory blocks) {
    this.query = query;
    this.members = members;
    this.blocks = blocks;
  }

  @Transactional(readOnly = true)
  Page list(
      @Nullable UUID viewerMemberId,
      boolean includeViewerNeighborhood,
      @Nullable String encodedCursor,
      int limit,
      @Nullable String searchQuery,
      @Nullable String scopeFilter,
      @Nullable Instant from,
      @Nullable Instant to,
      @Nullable Set<String> animalInterestCodes,
      @Nullable String type) {
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
    if (viewerNeighborhoodId != null) {
      visible = visible.or(SCOPE.eq("LOCAL").and(NEIGHBORHOOD_ID.eq(viewerNeighborhoodId)));
    }
    if (scopeFilter != null && !scopeFilter.isBlank()) {
      if (scopeFilter.equalsIgnoreCase("LOCAL")) {
        visible = SCOPE.eq("LOCAL").and(NEIGHBORHOOD_ID.eq(viewerNeighborhoodId));
      } else if (scopeFilter.equalsIgnoreCase("GLOBAL")) {
        visible = SCOPE.eq("GLOBAL");
      }
    }
    Condition condition = visible;
    if (from != null) condition = condition.and(CREATED_AT.ge(from.atOffset(ZoneOffset.UTC)));
    if (to != null) condition = condition.and(CREATED_AT.lt(to.atOffset(ZoneOffset.UTC)));
    if (searchQuery != null && !searchQuery.isBlank()) {
      String term = "%" + searchQuery.trim().toLowerCase(Locale.ROOT) + "%";
      condition = condition.and(TITLE.likeIgnoreCase(term).or(SUMMARY.likeIgnoreCase(term)));
    }
    if (type != null && !type.isBlank()) {
      condition = condition.and(ITEM_TYPE.eq(type));
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
      if (!blockedAuthorIds.isEmpty()) {
        condition = condition.and(AUTHOR_ID.isNull().or(AUTHOR_ID.notIn(blockedAuthorIds)));
      }
    }
    if (cursor != null) {
      OffsetDateTime cursorTime = cursor.createdAt().atOffset(ZoneOffset.UTC);
      condition =
          condition.and(
              CREATED_AT
                  .lt(cursorTime)
                  .or(
                      CREATED_AT
                          .eq(cursorTime)
                          .and(
                              ITEM_KIND
                                  .gt(cursor.itemKind())
                                  .or(
                                      ITEM_KIND
                                          .eq(cursor.itemKind())
                                          .and(SOURCE_ID.lt(cursor.sourceId()))))));
    }

    List<Item> fetched =
        query
            .select(
                SOURCE_ID,
                ITEM_KIND,
                ITEM_TYPE,
                TITLE,
                SUMMARY,
                SCOPE,
                AUTHOR_ID,
                NEIGHBORHOOD_ID,
                ANIMAL_INTEREST_CODE,
                STATUS,
                CREATED_AT,
                UPDATED_AT,
                TARGET_PATH)
            .from(ITEMS)
            .where(condition)
            .orderBy(CREATED_AT.desc(), ITEM_KIND.asc(), SOURCE_ID.desc())
            .limit(limit + 1)
            .fetch(CommunityFeed::toItem);

    boolean hasNext = fetched.size() > limit;
    List<Item> items = hasNext ? List.copyOf(fetched.subList(0, limit)) : List.copyOf(fetched);
    String nextCursor =
        hasNext
            ? Cursor.encode(
                items.getLast().createdAt(), items.getLast().itemKind(), items.getLast().sourceId())
            : null;
    return new Page(items, nextCursor, hasNext);
  }

  private static Item toItem(Record record) {
    return new Item(
        record.get(SOURCE_ID),
        record.get(ITEM_KIND),
        record.get(ITEM_TYPE),
        record.get(TITLE),
        record.get(SUMMARY),
        record.get(SCOPE),
        record.get(AUTHOR_ID),
        record.get(NEIGHBORHOOD_ID),
        record.get(ANIMAL_INTEREST_CODE),
        record.get(STATUS),
        record.get(CREATED_AT).toInstant(),
        record.get(UPDATED_AT).toInstant(),
        record.get(TARGET_PATH));
  }

  record Page(List<Item> items, @Nullable String nextCursor, boolean hasNext) {}

  record Item(
      UUID sourceId,
      String itemKind,
      String itemType,
      String title,
      String summary,
      String scope,
      @Nullable UUID authorId,
      @Nullable UUID neighborhoodId,
      @Nullable String animalInterestCode,
      String status,
      Instant createdAt,
      Instant updatedAt,
      String targetPath) {}

  private record Cursor(Instant createdAt, String itemKind, UUID sourceId) {
    static String encode(Instant createdAt, String itemKind, UUID sourceId) {
      String value = createdAt + "|" + itemKind + "|" + sourceId;
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    static Cursor decode(String encoded) {
      try {
        String value = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
        String[] parts = value.split("\\|", -1);
        if (parts.length == 2) {
          return new Cursor(Instant.parse(parts[0]), "PUBLICATION", UUID.fromString(parts[1]));
        }
        if (parts.length != 3 || parts[1].isBlank()) {
          throw new IllegalArgumentException("Invalid feed cursor");
        }
        return new Cursor(Instant.parse(parts[0]), parts[1], UUID.fromString(parts[2]));
      } catch (IllegalArgumentException exception) {
        throw new IllegalArgumentException("Invalid feed cursor", exception);
      }
    }
  }
}
