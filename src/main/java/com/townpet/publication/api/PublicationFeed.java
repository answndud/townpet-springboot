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
    if (limit < 1 || limit > 50) throw new IllegalArgumentException("Invalid popular limit");
    Table<?> METRIC = table(name("publication_metric")).as("m");
    Field<UUID> METRIC_ID = field(name("m", "publication_id"), UUID.class);
    Field<Long> VIEWS = field(name("m", "view_count"), Long.class);
    List<Item> items =
        query
            .select(
                ID,
                AUTHOR_ID,
                TYPE,
                SCOPE,
                NEIGHBORHOOD_ID,
                TITLE,
                BODY,
                LIFECYCLE,
                CREATED_AT,
                UPDATED_AT,
                VERSION)
            .from(PUBLICATION)
            .leftJoin(METRIC)
            .on(METRIC_ID.eq(ID))
            .where(LIFECYCLE.eq("ACTIVE").and(SCOPE.eq("GLOBAL")))
            .orderBy(VIEWS.desc().nullsLast(), CREATED_AT.desc(), ID.desc())
            .limit(limit)
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
                        record.get(LIFECYCLE),
                        record.get(CREATED_AT).toInstant(),
                        record.get(UPDATED_AT).toInstant(),
                        record.get(VERSION)));
    return new Page(List.copyOf(items), null, false);
  }

  @Transactional(readOnly = true)
  public Page list(
      @Nullable UUID viewerMemberId,
      boolean includeViewerNeighborhood,
      @Nullable String encodedCursor,
      int limit,
      @Nullable String searchQuery) {
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
    Condition condition = LIFECYCLE.eq("ACTIVE").and(visible);
    if (searchQuery != null && !searchQuery.isBlank()) {
      String term = "%" + searchQuery.trim().toLowerCase(Locale.ROOT) + "%";
      condition = condition.and(TITLE.likeIgnoreCase(term).or(BODY.likeIgnoreCase(term)));
    }
    if (viewerMemberId != null && includeViewerNeighborhood) {
      Set<UUID> blockedAuthorIds = blocks.blockedAuthorIds(viewerMemberId);
      if (!blockedAuthorIds.isEmpty()) condition = condition.and(AUTHOR_ID.notIn(blockedAuthorIds));
    }
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
                        record.get(LIFECYCLE),
                        record.get(CREATED_AT).toInstant(),
                        record.get(UPDATED_AT).toInstant(),
                        record.get(VERSION)));

    boolean hasNext = fetched.size() > limit;
    List<Item> items = hasNext ? List.copyOf(fetched.subList(0, limit)) : List.copyOf(fetched);
    String nextCursor =
        hasNext ? Cursor.encode(items.getLast().createdAt(), items.getLast().id()) : null;
    return new Page(items, nextCursor, hasNext);
  }

  public record Page(List<Item> items, @Nullable String nextCursor, boolean hasNext) {}

  public record Item(
      UUID id,
      String type,
      String title,
      String body,
      String scope,
      UUID authorId,
      @Nullable UUID neighborhoodId,
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
}
