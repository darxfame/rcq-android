package com.rcq.messenger.data.db;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class StoryDao_Impl implements StoryDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<StoryEntity> __insertionAdapterOfStoryEntity;

  private final EntityInsertionAdapter<StoryItemEntity> __insertionAdapterOfStoryItemEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteStory;

  public StoryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfStoryEntity = new EntityInsertionAdapter<StoryEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `stories` (`id`,`userId`,`nickname`,`avatarUrl`,`viewerCount`,`createdAt`,`expiresAt`,`isActive`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final StoryEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindLong(2, entity.getUserId());
        statement.bindString(3, entity.getNickname());
        if (entity.getAvatarUrl() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getAvatarUrl());
        }
        statement.bindLong(5, entity.getViewerCount());
        statement.bindLong(6, entity.getCreatedAt());
        statement.bindLong(7, entity.getExpiresAt());
        final int _tmp = entity.isActive() ? 1 : 0;
        statement.bindLong(8, _tmp);
      }
    };
    this.__insertionAdapterOfStoryItemEntity = new EntityInsertionAdapter<StoryItemEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `story_items` (`id`,`storyId`,`type`,`mediaUrl`,`thumbnailUrl`,`caption`,`backgroundColor`,`duration`,`createdAt`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final StoryItemEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getStoryId());
        statement.bindString(3, entity.getType());
        statement.bindString(4, entity.getMediaUrl());
        if (entity.getThumbnailUrl() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getThumbnailUrl());
        }
        if (entity.getCaption() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getCaption());
        }
        if (entity.getBackgroundColor() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getBackgroundColor());
        }
        statement.bindLong(8, entity.getDuration());
        statement.bindLong(9, entity.getCreatedAt());
      }
    };
    this.__preparedStmtOfDeleteStory = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM stories WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertStory(final StoryEntity story, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfStoryEntity.insert(story);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertStoryItems(final List<StoryItemEntity> items,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfStoryItemEntity.insert(items);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteStory(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteStory.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteStory.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<StoryEntity>> getStories() {
    final String _sql = "SELECT * FROM stories WHERE isActive = 1 ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"stories"}, new Callable<List<StoryEntity>>() {
      @Override
      @NonNull
      public List<StoryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfNickname = CursorUtil.getColumnIndexOrThrow(_cursor, "nickname");
          final int _cursorIndexOfAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "avatarUrl");
          final int _cursorIndexOfViewerCount = CursorUtil.getColumnIndexOrThrow(_cursor, "viewerCount");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfExpiresAt = CursorUtil.getColumnIndexOrThrow(_cursor, "expiresAt");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "isActive");
          final List<StoryEntity> _result = new ArrayList<StoryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final StoryEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final long _tmpUserId;
            _tmpUserId = _cursor.getLong(_cursorIndexOfUserId);
            final String _tmpNickname;
            _tmpNickname = _cursor.getString(_cursorIndexOfNickname);
            final String _tmpAvatarUrl;
            if (_cursor.isNull(_cursorIndexOfAvatarUrl)) {
              _tmpAvatarUrl = null;
            } else {
              _tmpAvatarUrl = _cursor.getString(_cursorIndexOfAvatarUrl);
            }
            final int _tmpViewerCount;
            _tmpViewerCount = _cursor.getInt(_cursorIndexOfViewerCount);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpExpiresAt;
            _tmpExpiresAt = _cursor.getLong(_cursorIndexOfExpiresAt);
            final boolean _tmpIsActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp != 0;
            _item = new StoryEntity(_tmpId,_tmpUserId,_tmpNickname,_tmpAvatarUrl,_tmpViewerCount,_tmpCreatedAt,_tmpExpiresAt,_tmpIsActive);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<StoryItemEntity>> getStoryItems(final String storyId) {
    final String _sql = "SELECT * FROM story_items WHERE storyId = ? ORDER BY createdAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, storyId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"story_items"}, new Callable<List<StoryItemEntity>>() {
      @Override
      @NonNull
      public List<StoryItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "storyId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfMediaUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "mediaUrl");
          final int _cursorIndexOfThumbnailUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbnailUrl");
          final int _cursorIndexOfCaption = CursorUtil.getColumnIndexOrThrow(_cursor, "caption");
          final int _cursorIndexOfBackgroundColor = CursorUtil.getColumnIndexOrThrow(_cursor, "backgroundColor");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<StoryItemEntity> _result = new ArrayList<StoryItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final StoryItemEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpStoryId;
            _tmpStoryId = _cursor.getString(_cursorIndexOfStoryId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpMediaUrl;
            _tmpMediaUrl = _cursor.getString(_cursorIndexOfMediaUrl);
            final String _tmpThumbnailUrl;
            if (_cursor.isNull(_cursorIndexOfThumbnailUrl)) {
              _tmpThumbnailUrl = null;
            } else {
              _tmpThumbnailUrl = _cursor.getString(_cursorIndexOfThumbnailUrl);
            }
            final String _tmpCaption;
            if (_cursor.isNull(_cursorIndexOfCaption)) {
              _tmpCaption = null;
            } else {
              _tmpCaption = _cursor.getString(_cursorIndexOfCaption);
            }
            final String _tmpBackgroundColor;
            if (_cursor.isNull(_cursorIndexOfBackgroundColor)) {
              _tmpBackgroundColor = null;
            } else {
              _tmpBackgroundColor = _cursor.getString(_cursorIndexOfBackgroundColor);
            }
            final int _tmpDuration;
            _tmpDuration = _cursor.getInt(_cursorIndexOfDuration);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new StoryItemEntity(_tmpId,_tmpStoryId,_tmpType,_tmpMediaUrl,_tmpThumbnailUrl,_tmpCaption,_tmpBackgroundColor,_tmpDuration,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
