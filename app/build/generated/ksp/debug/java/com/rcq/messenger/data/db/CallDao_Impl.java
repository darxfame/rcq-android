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
import com.rcq.messenger.domain.model.CallEntity;
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

  private final RoomTypeConverters __roomTypeConverters = new RoomTypeConverters();

  private final EntityDeletionOrUpdateAdapter<CallEntity> __deletionAdapterOfCallEntity;

  public CallDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCallEntity = new EntityInsertionAdapter<CallEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `calls` (`id`,`type`,`status`,`participantIds`,`initiatorId`,`startTime`,`endTime`,`duration`,`isGroupCall`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CallEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getType());
        statement.bindString(3, entity.getStatus());
        final String _tmp = __roomTypeConverters.fromListLong(entity.getParticipantIds());
        statement.bindString(4, _tmp);
        statement.bindLong(5, entity.getInitiatorId());
        statement.bindLong(6, entity.getStartTime());
        if (entity.getEndTime() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getEndTime());
        }
        statement.bindLong(8, entity.getDuration());
        final int _tmp_1 = entity.isGroupCall() ? 1 : 0;
        statement.bindLong(9, _tmp_1);
      }
    };
    this.__deletionAdapterOfCallEntity = new EntityDeletionOrUpdateAdapter<CallEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `calls` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CallEntity entity) {
        statement.bindString(1, entity.getId());
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
  public Object insertCalls(final List<CallEntity> calls,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCallEntity.insert(calls);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteCall(final CallEntity call, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfCallEntity.handle(call);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<CallEntity>> getCalls() {
    final String _sql = "SELECT * FROM calls ORDER BY startTime DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"calls"}, new Callable<List<CallEntity>>() {
      @Override
      @NonNull
      public List<CallEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfParticipantIds = CursorUtil.getColumnIndexOrThrow(_cursor, "participantIds");
          final int _cursorIndexOfInitiatorId = CursorUtil.getColumnIndexOrThrow(_cursor, "initiatorId");
          final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "startTime");
          final int _cursorIndexOfEndTime = CursorUtil.getColumnIndexOrThrow(_cursor, "endTime");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfIsGroupCall = CursorUtil.getColumnIndexOrThrow(_cursor, "isGroupCall");
          final List<CallEntity> _result = new ArrayList<CallEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CallEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final List<Long> _tmpParticipantIds;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfParticipantIds);
            _tmpParticipantIds = __roomTypeConverters.toListLong(_tmp);
            final long _tmpInitiatorId;
            _tmpInitiatorId = _cursor.getLong(_cursorIndexOfInitiatorId);
            final long _tmpStartTime;
            _tmpStartTime = _cursor.getLong(_cursorIndexOfStartTime);
            final Long _tmpEndTime;
            if (_cursor.isNull(_cursorIndexOfEndTime)) {
              _tmpEndTime = null;
            } else {
              _tmpEndTime = _cursor.getLong(_cursorIndexOfEndTime);
            }
            final long _tmpDuration;
            _tmpDuration = _cursor.getLong(_cursorIndexOfDuration);
            final boolean _tmpIsGroupCall;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsGroupCall);
            _tmpIsGroupCall = _tmp_1 != 0;
            _item = new CallEntity(_tmpId,_tmpType,_tmpStatus,_tmpParticipantIds,_tmpInitiatorId,_tmpStartTime,_tmpEndTime,_tmpDuration,_tmpIsGroupCall);
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
  public Flow<List<CallEntity>> getCalls(final int limit) {
    final String _sql = "SELECT * FROM calls ORDER BY startTime DESC LIMIT ?";
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
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfParticipantIds = CursorUtil.getColumnIndexOrThrow(_cursor, "participantIds");
          final int _cursorIndexOfInitiatorId = CursorUtil.getColumnIndexOrThrow(_cursor, "initiatorId");
          final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "startTime");
          final int _cursorIndexOfEndTime = CursorUtil.getColumnIndexOrThrow(_cursor, "endTime");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfIsGroupCall = CursorUtil.getColumnIndexOrThrow(_cursor, "isGroupCall");
          final List<CallEntity> _result = new ArrayList<CallEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CallEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final List<Long> _tmpParticipantIds;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfParticipantIds);
            _tmpParticipantIds = __roomTypeConverters.toListLong(_tmp);
            final long _tmpInitiatorId;
            _tmpInitiatorId = _cursor.getLong(_cursorIndexOfInitiatorId);
            final long _tmpStartTime;
            _tmpStartTime = _cursor.getLong(_cursorIndexOfStartTime);
            final Long _tmpEndTime;
            if (_cursor.isNull(_cursorIndexOfEndTime)) {
              _tmpEndTime = null;
            } else {
              _tmpEndTime = _cursor.getLong(_cursorIndexOfEndTime);
            }
            final long _tmpDuration;
            _tmpDuration = _cursor.getLong(_cursorIndexOfDuration);
            final boolean _tmpIsGroupCall;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsGroupCall);
            _tmpIsGroupCall = _tmp_1 != 0;
            _item = new CallEntity(_tmpId,_tmpType,_tmpStatus,_tmpParticipantIds,_tmpInitiatorId,_tmpStartTime,_tmpEndTime,_tmpDuration,_tmpIsGroupCall);
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
    final String _sql = "SELECT * FROM calls WHERE status = 'MISSED' ORDER BY startTime DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"calls"}, new Callable<List<CallEntity>>() {
      @Override
      @NonNull
      public List<CallEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfParticipantIds = CursorUtil.getColumnIndexOrThrow(_cursor, "participantIds");
          final int _cursorIndexOfInitiatorId = CursorUtil.getColumnIndexOrThrow(_cursor, "initiatorId");
          final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "startTime");
          final int _cursorIndexOfEndTime = CursorUtil.getColumnIndexOrThrow(_cursor, "endTime");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfIsGroupCall = CursorUtil.getColumnIndexOrThrow(_cursor, "isGroupCall");
          final List<CallEntity> _result = new ArrayList<CallEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CallEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final List<Long> _tmpParticipantIds;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfParticipantIds);
            _tmpParticipantIds = __roomTypeConverters.toListLong(_tmp);
            final long _tmpInitiatorId;
            _tmpInitiatorId = _cursor.getLong(_cursorIndexOfInitiatorId);
            final long _tmpStartTime;
            _tmpStartTime = _cursor.getLong(_cursorIndexOfStartTime);
            final Long _tmpEndTime;
            if (_cursor.isNull(_cursorIndexOfEndTime)) {
              _tmpEndTime = null;
            } else {
              _tmpEndTime = _cursor.getLong(_cursorIndexOfEndTime);
            }
            final long _tmpDuration;
            _tmpDuration = _cursor.getLong(_cursorIndexOfDuration);
            final boolean _tmpIsGroupCall;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsGroupCall);
            _tmpIsGroupCall = _tmp_1 != 0;
            _item = new CallEntity(_tmpId,_tmpType,_tmpStatus,_tmpParticipantIds,_tmpInitiatorId,_tmpStartTime,_tmpEndTime,_tmpDuration,_tmpIsGroupCall);
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
  public Object getCall(final String id, final Continuation<? super CallEntity> $completion) {
    final String _sql = "SELECT * FROM calls WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<CallEntity>() {
      @Override
      @Nullable
      public CallEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfParticipantIds = CursorUtil.getColumnIndexOrThrow(_cursor, "participantIds");
          final int _cursorIndexOfInitiatorId = CursorUtil.getColumnIndexOrThrow(_cursor, "initiatorId");
          final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "startTime");
          final int _cursorIndexOfEndTime = CursorUtil.getColumnIndexOrThrow(_cursor, "endTime");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfIsGroupCall = CursorUtil.getColumnIndexOrThrow(_cursor, "isGroupCall");
          final CallEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final List<Long> _tmpParticipantIds;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfParticipantIds);
            _tmpParticipantIds = __roomTypeConverters.toListLong(_tmp);
            final long _tmpInitiatorId;
            _tmpInitiatorId = _cursor.getLong(_cursorIndexOfInitiatorId);
            final long _tmpStartTime;
            _tmpStartTime = _cursor.getLong(_cursorIndexOfStartTime);
            final Long _tmpEndTime;
            if (_cursor.isNull(_cursorIndexOfEndTime)) {
              _tmpEndTime = null;
            } else {
              _tmpEndTime = _cursor.getLong(_cursorIndexOfEndTime);
            }
            final long _tmpDuration;
            _tmpDuration = _cursor.getLong(_cursorIndexOfDuration);
            final boolean _tmpIsGroupCall;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsGroupCall);
            _tmpIsGroupCall = _tmp_1 != 0;
            _result = new CallEntity(_tmpId,_tmpType,_tmpStatus,_tmpParticipantIds,_tmpInitiatorId,_tmpStartTime,_tmpEndTime,_tmpDuration,_tmpIsGroupCall);
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
