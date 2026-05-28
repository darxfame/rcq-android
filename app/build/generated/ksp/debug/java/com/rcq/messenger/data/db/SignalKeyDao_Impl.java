package com.rcq.messenger.data.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.rcq.messenger.domain.model.SignalKeyEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
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

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SignalKeyDao_Impl implements SignalKeyDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SignalKeyEntity> __insertionAdapterOfSignalKeyEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteSession;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllSessions;

  private final SharedSQLiteStatement __preparedStmtOfRemovePreKey;

  private final SharedSQLiteStatement __preparedStmtOfRemoveSignedPreKey;

  private final SharedSQLiteStatement __preparedStmtOfClearAllKeys;

  private final SharedSQLiteStatement __preparedStmtOfCleanupOldKeys;

  public SignalKeyDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSignalKeyEntity = new EntityInsertionAdapter<SignalKeyEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `signal_keys` (`id`,`keyType`,`address`,`keyId`,`keyData`,`timestamp`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SignalKeyEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getKeyType());
        if (entity.getAddress() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getAddress());
        }
        if (entity.getKeyId() == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, entity.getKeyId());
        }
        statement.bindBlob(5, entity.getKeyData());
        statement.bindLong(6, entity.getTimestamp());
      }
    };
    this.__preparedStmtOfDeleteSession = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM signal_keys WHERE keyType = 'session' AND address = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllSessions = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM signal_keys WHERE keyType = 'session' AND address LIKE ? || '%'";
        return _query;
      }
    };
    this.__preparedStmtOfRemovePreKey = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM signal_keys WHERE keyType = 'prekey' AND keyId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfRemoveSignedPreKey = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM signal_keys WHERE keyType = 'signed_prekey' AND keyId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearAllKeys = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM signal_keys";
        return _query;
      }
    };
    this.__preparedStmtOfCleanupOldKeys = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM signal_keys WHERE timestamp < ?";
        return _query;
      }
    };
  }

  @Override
  public Object storeSession(final SignalKeyEntity session,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSignalKeyEntity.insert(session);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object storePreKey(final SignalKeyEntity preKey,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSignalKeyEntity.insert(preKey);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object storeSignedPreKey(final SignalKeyEntity signedPreKey,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSignalKeyEntity.insert(signedPreKey);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object storeIdentityKeyPair(final SignalKeyEntity identityKeyPair,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSignalKeyEntity.insert(identityKeyPair);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object saveIdentity(final SignalKeyEntity identity,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSignalKeyEntity.insert(identity);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object storeLocalRegistrationId(final SignalKeyEntity registrationId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSignalKeyEntity.insert(registrationId);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteSession(final String address, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteSession.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, address);
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
          __preparedStmtOfDeleteSession.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllSessions(final String namePrefix,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllSessions.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, namePrefix);
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
          __preparedStmtOfDeleteAllSessions.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object removePreKey(final int preKeyId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfRemovePreKey.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, preKeyId);
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
          __preparedStmtOfRemovePreKey.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object removeSignedPreKey(final int signedPreKeyId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfRemoveSignedPreKey.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, signedPreKeyId);
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
          __preparedStmtOfRemoveSignedPreKey.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearAllKeys(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearAllKeys.acquire();
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
          __preparedStmtOfClearAllKeys.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object cleanupOldKeys(final long cutoffTime,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfCleanupOldKeys.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, cutoffTime);
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
          __preparedStmtOfCleanupOldKeys.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getSession(final String address,
      final Continuation<? super SignalKeyEntity> $completion) {
    final String _sql = "SELECT * FROM signal_keys WHERE keyType = 'session' AND address = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, address);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SignalKeyEntity>() {
      @Override
      @Nullable
      public SignalKeyEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfKeyType = CursorUtil.getColumnIndexOrThrow(_cursor, "keyType");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfKeyId = CursorUtil.getColumnIndexOrThrow(_cursor, "keyId");
          final int _cursorIndexOfKeyData = CursorUtil.getColumnIndexOrThrow(_cursor, "keyData");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final SignalKeyEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpKeyType;
            _tmpKeyType = _cursor.getString(_cursorIndexOfKeyType);
            final String _tmpAddress;
            if (_cursor.isNull(_cursorIndexOfAddress)) {
              _tmpAddress = null;
            } else {
              _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            }
            final Integer _tmpKeyId;
            if (_cursor.isNull(_cursorIndexOfKeyId)) {
              _tmpKeyId = null;
            } else {
              _tmpKeyId = _cursor.getInt(_cursorIndexOfKeyId);
            }
            final byte[] _tmpKeyData;
            _tmpKeyData = _cursor.getBlob(_cursorIndexOfKeyData);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _result = new SignalKeyEntity(_tmpId,_tmpKeyType,_tmpAddress,_tmpKeyId,_tmpKeyData,_tmpTimestamp);
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

  @Override
  public Object getSubDeviceSessions(final String name,
      final Continuation<? super List<String>> $completion) {
    final String _sql = "SELECT address FROM signal_keys WHERE keyType = 'session' AND address IS NOT NULL AND address LIKE ? || '%'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, name);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<String>>() {
      @Override
      @NonNull
      public List<String> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<String> _result = new ArrayList<String>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final String _item;
            _item = _cursor.getString(0);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object loadPreKey(final int preKeyId,
      final Continuation<? super SignalKeyEntity> $completion) {
    final String _sql = "SELECT * FROM signal_keys WHERE keyType = 'prekey' AND keyId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, preKeyId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SignalKeyEntity>() {
      @Override
      @Nullable
      public SignalKeyEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfKeyType = CursorUtil.getColumnIndexOrThrow(_cursor, "keyType");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfKeyId = CursorUtil.getColumnIndexOrThrow(_cursor, "keyId");
          final int _cursorIndexOfKeyData = CursorUtil.getColumnIndexOrThrow(_cursor, "keyData");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final SignalKeyEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpKeyType;
            _tmpKeyType = _cursor.getString(_cursorIndexOfKeyType);
            final String _tmpAddress;
            if (_cursor.isNull(_cursorIndexOfAddress)) {
              _tmpAddress = null;
            } else {
              _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            }
            final Integer _tmpKeyId;
            if (_cursor.isNull(_cursorIndexOfKeyId)) {
              _tmpKeyId = null;
            } else {
              _tmpKeyId = _cursor.getInt(_cursorIndexOfKeyId);
            }
            final byte[] _tmpKeyData;
            _tmpKeyData = _cursor.getBlob(_cursorIndexOfKeyData);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _result = new SignalKeyEntity(_tmpId,_tmpKeyType,_tmpAddress,_tmpKeyId,_tmpKeyData,_tmpTimestamp);
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

  @Override
  public Object loadPreKeys(final Continuation<? super List<Integer>> $completion) {
    final String _sql = "SELECT keyId FROM signal_keys WHERE keyType = 'prekey' AND keyId IS NOT NULL";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Integer>>() {
      @Override
      @NonNull
      public List<Integer> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<Integer> _result = new ArrayList<Integer>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Integer _item;
            _item = _cursor.getInt(0);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object loadSignedPreKey(final int signedPreKeyId,
      final Continuation<? super SignalKeyEntity> $completion) {
    final String _sql = "SELECT * FROM signal_keys WHERE keyType = 'signed_prekey' AND keyId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, signedPreKeyId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SignalKeyEntity>() {
      @Override
      @Nullable
      public SignalKeyEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfKeyType = CursorUtil.getColumnIndexOrThrow(_cursor, "keyType");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfKeyId = CursorUtil.getColumnIndexOrThrow(_cursor, "keyId");
          final int _cursorIndexOfKeyData = CursorUtil.getColumnIndexOrThrow(_cursor, "keyData");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final SignalKeyEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpKeyType;
            _tmpKeyType = _cursor.getString(_cursorIndexOfKeyType);
            final String _tmpAddress;
            if (_cursor.isNull(_cursorIndexOfAddress)) {
              _tmpAddress = null;
            } else {
              _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            }
            final Integer _tmpKeyId;
            if (_cursor.isNull(_cursorIndexOfKeyId)) {
              _tmpKeyId = null;
            } else {
              _tmpKeyId = _cursor.getInt(_cursorIndexOfKeyId);
            }
            final byte[] _tmpKeyData;
            _tmpKeyData = _cursor.getBlob(_cursorIndexOfKeyData);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _result = new SignalKeyEntity(_tmpId,_tmpKeyType,_tmpAddress,_tmpKeyId,_tmpKeyData,_tmpTimestamp);
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

  @Override
  public Object loadSignedPreKeys(final Continuation<? super List<SignalKeyEntity>> $completion) {
    final String _sql = "SELECT * FROM signal_keys WHERE keyType = 'signed_prekey'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<SignalKeyEntity>>() {
      @Override
      @NonNull
      public List<SignalKeyEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfKeyType = CursorUtil.getColumnIndexOrThrow(_cursor, "keyType");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfKeyId = CursorUtil.getColumnIndexOrThrow(_cursor, "keyId");
          final int _cursorIndexOfKeyData = CursorUtil.getColumnIndexOrThrow(_cursor, "keyData");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final List<SignalKeyEntity> _result = new ArrayList<SignalKeyEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SignalKeyEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpKeyType;
            _tmpKeyType = _cursor.getString(_cursorIndexOfKeyType);
            final String _tmpAddress;
            if (_cursor.isNull(_cursorIndexOfAddress)) {
              _tmpAddress = null;
            } else {
              _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            }
            final Integer _tmpKeyId;
            if (_cursor.isNull(_cursorIndexOfKeyId)) {
              _tmpKeyId = null;
            } else {
              _tmpKeyId = _cursor.getInt(_cursorIndexOfKeyId);
            }
            final byte[] _tmpKeyData;
            _tmpKeyData = _cursor.getBlob(_cursorIndexOfKeyData);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _item = new SignalKeyEntity(_tmpId,_tmpKeyType,_tmpAddress,_tmpKeyId,_tmpKeyData,_tmpTimestamp);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getIdentityKeyPair(final Continuation<? super SignalKeyEntity> $completion) {
    final String _sql = "SELECT * FROM signal_keys WHERE keyType = 'identity_keypair'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SignalKeyEntity>() {
      @Override
      @Nullable
      public SignalKeyEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfKeyType = CursorUtil.getColumnIndexOrThrow(_cursor, "keyType");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfKeyId = CursorUtil.getColumnIndexOrThrow(_cursor, "keyId");
          final int _cursorIndexOfKeyData = CursorUtil.getColumnIndexOrThrow(_cursor, "keyData");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final SignalKeyEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpKeyType;
            _tmpKeyType = _cursor.getString(_cursorIndexOfKeyType);
            final String _tmpAddress;
            if (_cursor.isNull(_cursorIndexOfAddress)) {
              _tmpAddress = null;
            } else {
              _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            }
            final Integer _tmpKeyId;
            if (_cursor.isNull(_cursorIndexOfKeyId)) {
              _tmpKeyId = null;
            } else {
              _tmpKeyId = _cursor.getInt(_cursorIndexOfKeyId);
            }
            final byte[] _tmpKeyData;
            _tmpKeyData = _cursor.getBlob(_cursorIndexOfKeyData);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _result = new SignalKeyEntity(_tmpId,_tmpKeyType,_tmpAddress,_tmpKeyId,_tmpKeyData,_tmpTimestamp);
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

  @Override
  public Object getIdentity(final String address,
      final Continuation<? super SignalKeyEntity> $completion) {
    final String _sql = "SELECT * FROM signal_keys WHERE keyType = 'identity' AND address = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, address);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SignalKeyEntity>() {
      @Override
      @Nullable
      public SignalKeyEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfKeyType = CursorUtil.getColumnIndexOrThrow(_cursor, "keyType");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfKeyId = CursorUtil.getColumnIndexOrThrow(_cursor, "keyId");
          final int _cursorIndexOfKeyData = CursorUtil.getColumnIndexOrThrow(_cursor, "keyData");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final SignalKeyEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpKeyType;
            _tmpKeyType = _cursor.getString(_cursorIndexOfKeyType);
            final String _tmpAddress;
            if (_cursor.isNull(_cursorIndexOfAddress)) {
              _tmpAddress = null;
            } else {
              _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            }
            final Integer _tmpKeyId;
            if (_cursor.isNull(_cursorIndexOfKeyId)) {
              _tmpKeyId = null;
            } else {
              _tmpKeyId = _cursor.getInt(_cursorIndexOfKeyId);
            }
            final byte[] _tmpKeyData;
            _tmpKeyData = _cursor.getBlob(_cursorIndexOfKeyData);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _result = new SignalKeyEntity(_tmpId,_tmpKeyType,_tmpAddress,_tmpKeyId,_tmpKeyData,_tmpTimestamp);
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

  @Override
  public Object getTrustedKeys(final Continuation<? super List<String>> $completion) {
    final String _sql = "SELECT address FROM signal_keys WHERE keyType = 'identity' AND address IS NOT NULL";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<String>>() {
      @Override
      @NonNull
      public List<String> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<String> _result = new ArrayList<String>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final String _item;
            _item = _cursor.getString(0);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getLocalRegistrationId(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT keyId FROM signal_keys WHERE keyType = 'registration_id' LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @Nullable
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            if (_cursor.isNull(0)) {
              _result = null;
            } else {
              _result = _cursor.getInt(0);
            }
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

  @Override
  public Object getKeyCount(final String keyType, final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM signal_keys WHERE keyType = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, keyType);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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
