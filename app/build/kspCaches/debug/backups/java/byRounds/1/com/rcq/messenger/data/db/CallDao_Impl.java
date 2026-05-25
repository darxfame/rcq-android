package com.rcq.messenger.data.db;

import android.database.Cursor;
import androidx.annotation.NonNull;
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
import java.lang.Long;
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
public final class CallDao_Impl implements CallDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CallEntity> __insertionAdapterOfCallEntity;

  private final EntityDeletionOrUpdateAdapter<CallEntity> __updateAdapterOfCallEntity;

  public CallDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCallEntity = new EntityInsertionAdapter<CallEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `calls` (`id`,`type`,`targetId`,`targetNickname`,`targetAvatar`,`initiatorId`,`status`,`startedAt`,`endedAt`,`duration`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CallEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getType());
        statement.bindLong(3, entity.getTargetId());
        statement.bindString(4, entity.getTargetNickname());
        if (entity.getTargetAvatar() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getTargetAvatar());
        }
        statement.bindLong(6, entity.getInitiatorId());
        statement.bindString(7, entity.getStatus());
        if (entity.getStartedAt() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getStartedAt());
        }
        if (entity.getEndedAt() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getEndedAt());
        }
        statement.bindLong(10, entity.getDuration());
      }
    };
    this.__updateAdapterOfCallEntity = new EntityDeletionOrUpdateAdapter<CallEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `calls` SET `id` = ?,`type` = ?,`targetId` = ?,`targetNickname` = ?,`targetAvatar` = ?,`initiatorId` = ?,`status` = ?,`startedAt` = ?,`endedAt` = ?,`duration` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CallEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getType());
        statement.bindLong(3, entity.getTargetId());
        statement.bindString(4, entity.getTargetNickname());
        if (entity.getTargetAvatar() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getTargetAvatar());
        }
        statement.bindLong(6, entity.getInitiatorId());
        statement.bindString(7, entity.getStatus());
        if (entity.getStartedAt() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getStartedAt());
        }
        if (entity.getEndedAt() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getEndedAt());
        }
        statement.bindLong(10, entity.getDuration());
        statement.bindString(11, entity.getId());
      }
    };
  }

  @Override
  public Object insertCall(final CallEntity call, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCallEntity.insert(call);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateCall(final CallEntity call, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfCallEntity.handle(call);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<CallEntity>> getCalls(final int limit) {
    final String _sql = "SELECT * FROM calls ORDER BY startedAt DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"calls"}, new Callable<List<CallEntity>>() {
      @Override
      @NonNull
      public List<CallEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfTargetId = CursorUtil.getColumnIndexOrThrow(_cursor, "targetId");
          final int _cursorIndexOfTargetNickname = CursorUtil.getColumnIndexOrThrow(_cursor, "targetNickname");
          final int _cursorIndexOfTargetAvatar = CursorUtil.getColumnIndexOrThrow(_cursor, "targetAvatar");
          final int _cursorIndexOfInitiatorId = CursorUtil.getColumnIndexOrThrow(_cursor, "initiatorId");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfStartedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "startedAt");
          final int _cursorIndexOfEndedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "endedAt");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final List<CallEntity> _result = new ArrayList<CallEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CallEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final long _tmpTargetId;
            _tmpTargetId = _cursor.getLong(_cursorIndexOfTargetId);
            final String _tmpTargetNickname;
            _tmpTargetNickname = _cursor.getString(_cursorIndexOfTargetNickname);
            final String _tmpTargetAvatar;
            if (_cursor.isNull(_cursorIndexOfTargetAvatar)) {
              _tmpTargetAvatar = null;
            } else {
              _tmpTargetAvatar = _cursor.getString(_cursorIndexOfTargetAvatar);
            }
            final long _tmpInitiatorId;
            _tmpInitiatorId = _cursor.getLong(_cursorIndexOfInitiatorId);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final Long _tmpStartedAt;
            if (_cursor.isNull(_cursorIndexOfStartedAt)) {
              _tmpStartedAt = null;
            } else {
              _tmpStartedAt = _cursor.getLong(_cursorIndexOfStartedAt);
            }
            final Long _tmpEndedAt;
            if (_cursor.isNull(_cursorIndexOfEndedAt)) {
              _tmpEndedAt = null;
            } else {
              _tmpEndedAt = _cursor.getLong(_cursorIndexOfEndedAt);
            }
            final long _tmpDuration;
            _tmpDuration = _cursor.getLong(_cursorIndexOfDuration);
            _item = new CallEntity(_tmpId,_tmpType,_tmpTargetId,_tmpTargetNickname,_tmpTargetAvatar,_tmpInitiatorId,_tmpStatus,_tmpStartedAt,_tmpEndedAt,_tmpDuration);
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
  public Flow<List<CallEntity>> getMissedCalls() {
    final String _sql = "SELECT * FROM calls WHERE status = 'MISSED' ORDER BY startedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"calls"}, new Callable<List<CallEntity>>() {
      @Override
      @NonNull
      public List<CallEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfTargetId = CursorUtil.getColumnIndexOrThrow(_cursor, "targetId");
          final int _cursorIndexOfTargetNickname = CursorUtil.getColumnIndexOrThrow(_cursor, "targetNickname");
          final int _cursorIndexOfTargetAvatar = CursorUtil.getColumnIndexOrThrow(_cursor, "targetAvatar");
          final int _cursorIndexOfInitiatorId = CursorUtil.getColumnIndexOrThrow(_cursor, "initiatorId");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfStartedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "startedAt");
          final int _cursorIndexOfEndedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "endedAt");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final List<CallEntity> _result = new ArrayList<CallEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CallEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final long _tmpTargetId;
            _tmpTargetId = _cursor.getLong(_cursorIndexOfTargetId);
            final String _tmpTargetNickname;
            _tmpTargetNickname = _cursor.getString(_cursorIndexOfTargetNickname);
            final String _tmpTargetAvatar;
            if (_cursor.isNull(_cursorIndexOfTargetAvatar)) {
              _tmpTargetAvatar = null;
            } else {
              _tmpTargetAvatar = _cursor.getString(_cursorIndexOfTargetAvatar);
            }
            final long _tmpInitiatorId;
            _tmpInitiatorId = _cursor.getLong(_cursorIndexOfInitiatorId);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final Long _tmpStartedAt;
            if (_cursor.isNull(_cursorIndexOfStartedAt)) {
              _tmpStartedAt = null;
            } else {
              _tmpStartedAt = _cursor.getLong(_cursorIndexOfStartedAt);
            }
            final Long _tmpEndedAt;
            if (_cursor.isNull(_cursorIndexOfEndedAt)) {
              _tmpEndedAt = null;
            } else {
              _tmpEndedAt = _cursor.getLong(_cursorIndexOfEndedAt);
            }
            final long _tmpDuration;
            _tmpDuration = _cursor.getLong(_cursorIndexOfDuration);
            _item = new CallEntity(_tmpId,_tmpType,_tmpTargetId,_tmpTargetNickname,_tmpTargetAvatar,_tmpInitiatorId,_tmpStatus,_tmpStartedAt,_tmpEndedAt,_tmpDuration);
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
