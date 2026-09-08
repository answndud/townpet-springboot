package com.townpet.discovery;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Select;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.lang.Nullable;

/** Builds the public feed query shared by the HTTP path and performance diagnostics. */
final class CommunityFeedQuery {
  static final Table<?> ITEMS = DSL.table(DSL.name("townpet_public_feed_item")).as("f");
  static final Field<UUID> SOURCE_ID = DSL.field(DSL.name("f", "source_id"), UUID.class);
  static final Field<String> ITEM_KIND = DSL.field(DSL.name("f", "item_kind"), String.class);
  static final Field<String> ITEM_TYPE = DSL.field(DSL.name("f", "item_type"), String.class);
  static final Field<String> TITLE = DSL.field(DSL.name("f", "title"), String.class);
  static final Field<String> SUMMARY = DSL.field(DSL.name("f", "summary"), String.class);
  static final Field<UUID> AUTHOR_ID = DSL.field(DSL.name("f", "author_member_id"), UUID.class);
  static final Field<UUID> NEIGHBORHOOD_ID =
      DSL.field(DSL.name("f", "neighborhood_id"), UUID.class);
  static final Field<String> ANIMAL_INTEREST_CODE =
      DSL.field(DSL.name("f", "animal_interest_code"), String.class);
  static final Field<String> STATUS = DSL.field(DSL.name("f", "status"), String.class);
  static final Field<OffsetDateTime> CREATED_AT =
      DSL.field(DSL.name("f", "created_at"), OffsetDateTime.class);
  static final Field<OffsetDateTime> UPDATED_AT =
      DSL.field(DSL.name("f", "updated_at"), OffsetDateTime.class);
  static final Field<String> TARGET_PATH = DSL.field(DSL.name("f", "target_path"), String.class);

  private CommunityFeedQuery() {}

  static Select<?> build(DSLContext query, Params params) {
    Condition condition = DSL.trueCondition();
    if (params.from() != null) condition = condition.and(CREATED_AT.ge(utc(params.from())));
    if (params.to() != null) condition = condition.and(CREATED_AT.lt(utc(params.to())));
    if (params.searchQuery() != null && !params.searchQuery().isBlank()) {
      String term = "%" + params.searchQuery().trim().toLowerCase(java.util.Locale.ROOT) + "%";
      condition = condition.and(searchCondition(term, params.searchField()));
    }
    if (params.itemTypes() != null) {
      condition =
          params.itemTypes().isEmpty()
              ? condition.and(DSL.falseCondition())
              : condition.and(ITEM_TYPE.in(params.itemTypes()));
    }
    if (params.animalInterestCodes() != null) {
      condition =
          params.animalInterestCodes().isEmpty()
              ? condition.and(ANIMAL_INTEREST_CODE.isNull())
              : condition.and(
                  ANIMAL_INTEREST_CODE
                      .isNull()
                      .or(ANIMAL_INTEREST_CODE.in(params.animalInterestCodes())));
    }
    if (params.blockedAuthorIds() != null && !params.blockedAuthorIds().isEmpty()) {
      condition = condition.and(AUTHOR_ID.isNull().or(AUTHOR_ID.notIn(params.blockedAuthorIds())));
    }
    if (params.cursor() != null) {
      OffsetDateTime cursorTime = utc(params.cursor().createdAt());
      condition =
          condition.and(
              CREATED_AT
                  .lt(cursorTime)
                  .or(
                      CREATED_AT
                          .eq(cursorTime)
                          .and(
                              ITEM_KIND
                                  .gt(params.cursor().itemKind())
                                  .or(
                                      ITEM_KIND
                                          .eq(params.cursor().itemKind())
                                          .and(SOURCE_ID.lt(params.cursor().sourceId()))))));
    }
    return query
        .select(
            SOURCE_ID,
            ITEM_KIND,
            ITEM_TYPE,
            TITLE,
            SUMMARY,
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
        .limit(params.limit() + 1);
  }

  private static Condition searchCondition(String term, String searchField) {
    if (searchField.equalsIgnoreCase("TITLE")) return TITLE.likeIgnoreCase(term);
    if (searchField.equalsIgnoreCase("BODY")) return SUMMARY.likeIgnoreCase(term);
    return TITLE.likeIgnoreCase(term).or(SUMMARY.likeIgnoreCase(term));
  }

  private static OffsetDateTime utc(Instant value) {
    return value.atOffset(ZoneOffset.UTC);
  }

  record Params(
      @Nullable Instant from,
      @Nullable Instant to,
      @Nullable String searchQuery,
      String searchField,
      @Nullable Set<String> animalInterestCodes,
      @Nullable Set<String> itemTypes,
      @Nullable Set<UUID> blockedAuthorIds,
      @Nullable Cursor cursor,
      int limit) {}

  record Cursor(Instant createdAt, String itemKind, UUID sourceId) {}
}
