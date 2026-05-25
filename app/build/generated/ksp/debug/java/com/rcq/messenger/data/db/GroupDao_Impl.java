package com.rcq.messenger.data.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
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
public final class GroupDao_Impl implements GroupDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<GroupEntity> __insertionAdapterOfGroupEntity;

  private final EntityDeletionOrUpdateAdapter<GroupEntity> __deletionAdapterOfGroupEntity;

  public GroupDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfGroupEntity = new EntityInsertionAdapter<GroupEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `groups` (`id`,`name`,`avatarUrl`,`description`,`ownerId`,`memberCount`,`createdAt`,`isPinned`,`isMuted`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final GroupEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        if (entity.getAvatarUrl() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getAvatarUrl());
        }
        statement.bindString(4, entity.getDescription());
        statement.bindLong(5, entity.getOwnerId());
        statement.bindLong(6, entity.getMemberCount());
        statement.bindLong(7, entity.getCreatedAt());
        final int _tmp = entity.isPinned() ? 1 : 0;
        statement.bindLong(8, _tmp);
        final int _tmp_1 = entity.isMuted() ? 1 : 0;
        statement.bindLong(9, _tmp_1);
      }
    };
    this.__deletionAdapterOfGroupEntity = new EntityDeletionOrUpdateAdapter<GroupEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `groups` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final GroupEntity entity) {
        statement.bindString(1, entity.getId());
      }
    };
  }

  @Override
  public Object insertGroup(final GroupEntity group, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfGroupEntity.insert(group);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertGroups(final List<GroupEntity> groups,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfGroupEntity.insert(groups);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteGroup(final GroupEntity group, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfGroupEntity.handle(group);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<GroupEntity>> getGroups() {
    final String _sql = "SELECT * FROM groups ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"groups"}, new Callable<List<GroupEntity>>() {
      @Override
      @NonNull
      public List<GroupEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "avatarUrl");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfOwnerId = CursorUtil.getColumnIndexOrThrow(_cursor, "ownerId");
          final int _cursorIndexOfMemberCount = CursorUtil.getColumnIndexOrThrow(_cursor, "memberCount");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "isPinned");
          final int _cursorIndexOfIsMuted = CursorUtil.getColumnIndexOrThrow(_cursor, "isMuted");
          final List<GroupEntity> _result = new ArrayList<GroupEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final GroupEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpAvatarUrl;
            if (_cursor.isNull(_cursorIndexOfAvatarUrl)) {
              _tmpAvatarUrl = null;
            } else {
              _tmpAvatarUrl = _cursor.getString(_cursorIndexOfAvatarUrl);
            }
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final long _tmpOwnerId;
            _tmpOwnerId = _cursor.getLong(_cursorIndexOfOwnerId);
            final int _tmpMemberCount;
            _tmpMemberCount = _cursor.getInt(_cursorIndexOfMemberCount);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final boolean _tmpIsPinned;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp != 0;
            final boolean _tmpIsMuted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsMuted);
            _tmpIsMuted = _tmp_1 != 0;
            _item = new GroupEntity(_tmpId,_tmpName,_tmpAvatarUrl,_tmpDescription,_tmpOwnerId,_tmpMemberCount,_tmpCreatedAt,_tmpIsPinned,_tmpIsMuted);
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
  public Object getGroup(final String id, final Continuation<? super GroupEntity> $completion) {
    final String _sql = "SELECT * FROM groups WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<GroupEntity>() {
      @Override
      @Nullable
      public GroupEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "avatarUrl");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfOwnerId = CursorUtil.getColumnIndexOrThrow(_cursor, "ownerId");
          final int _cursorIndexOfMemberCount = CursorUtil.getColumnIndexOrThrow(_cursor, "memberCount");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "isPinned");
          final int _cursorIndexOfIsMuted = CursorUtil.getColumnIndexOrThrow(_cursor, "isMuted");
          final GroupEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpAvatarUrl;
            if (_cursor.isNull(_cursorIndexOfAvatarUrl)) {
              _tmpAvatarUrl = null;
            } else {
              _tmpAvatarUrl = _cursor.getString(_cursorIndexOfAvatarUrl);
            }
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final long _tmpOwnerId;
            _tmpOwnerId = _cursor.getLong(_cursorIndexOfOwnerId);
            final int _tmpMemberCount;
            _tmpMemberCount = _cursor.getInt(_cursorIndexOfMemberCount);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final boolean _tmpIsPinned;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp != 0;
            final boolean _tmpIsMuted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsMuted);
            _tmpIsMuted = _tmp_1 != 0;
            _result = new GroupEntity(_tmpId,_tmpName,_tmpAvatarUrl,_tmpDescription,_tmpOwnerId,_tmpMemberCount,_tmpCreatedAt,_tmpIsPinned,_tmpIsMuted);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
