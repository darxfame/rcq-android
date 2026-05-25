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
public final class UserDao_Impl implements UserDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<UserEntity> __insertionAdapterOfUserEntity;

  private final EntityDeletionOrUpdateAdapter<UserEntity> __deletionAdapterOfUserEntity;

  public UserDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfUserEntity = new EntityInsertionAdapter<UserEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `users` (`id`,`nickname`,`avatarUrl`,`status`,`lastSeen`,`bio`,`isBlocked`,`isFavorite`,`notificationSound`,`customNickname`,`tokens`,`isPremium`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final UserEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getNickname());
        if (entity.getAvatarUrl() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getAvatarUrl());
        }
        statement.bindString(4, entity.getStatus());
        if (entity.getLastSeen() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getLastSeen());
        }
        statement.bindString(6, entity.getBio());
        final int _tmp = entity.isBlocked() ? 1 : 0;
        statement.bindLong(7, _tmp);
        final int _tmp_1 = entity.isFavorite() ? 1 : 0;
        statement.bindLong(8, _tmp_1);
        if (entity.getNotificationSound() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getNotificationSound());
        }
        if (entity.getCustomNickname() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getCustomNickname());
        }
        statement.bindLong(11, entity.getTokens());
        final int _tmp_2 = entity.isPremium() ? 1 : 0;
        statement.bindLong(12, _tmp_2);
      }
    };
    this.__deletionAdapterOfUserEntity = new EntityDeletionOrUpdateAdapter<UserEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `users` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final UserEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
  }

  @Override
  public Object insertUser(final UserEntity user, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfUserEntity.insert(user);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertUsers(final List<UserEntity> users,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfUserEntity.insert(users);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteUser(final UserEntity user, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfUserEntity.handle(user);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object getUser(final long id, final Continuation<? super UserEntity> $completion) {
    final String _sql = "SELECT * FROM users WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<UserEntity>() {
      @Override
      @Nullable
      public UserEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfNickname = CursorUtil.getColumnIndexOrThrow(_cursor, "nickname");
          final int _cursorIndexOfAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "avatarUrl");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfLastSeen = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeen");
          final int _cursorIndexOfBio = CursorUtil.getColumnIndexOrThrow(_cursor, "bio");
          final int _cursorIndexOfIsBlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isBlocked");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfNotificationSound = CursorUtil.getColumnIndexOrThrow(_cursor, "notificationSound");
          final int _cursorIndexOfCustomNickname = CursorUtil.getColumnIndexOrThrow(_cursor, "customNickname");
          final int _cursorIndexOfTokens = CursorUtil.getColumnIndexOrThrow(_cursor, "tokens");
          final int _cursorIndexOfIsPremium = CursorUtil.getColumnIndexOrThrow(_cursor, "isPremium");
          final UserEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpNickname;
            _tmpNickname = _cursor.getString(_cursorIndexOfNickname);
            final String _tmpAvatarUrl;
            if (_cursor.isNull(_cursorIndexOfAvatarUrl)) {
              _tmpAvatarUrl = null;
            } else {
              _tmpAvatarUrl = _cursor.getString(_cursorIndexOfAvatarUrl);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpLastSeen;
            if (_cursor.isNull(_cursorIndexOfLastSeen)) {
              _tmpLastSeen = null;
            } else {
              _tmpLastSeen = _cursor.getString(_cursorIndexOfLastSeen);
            }
            final String _tmpBio;
            _tmpBio = _cursor.getString(_cursorIndexOfBio);
            final boolean _tmpIsBlocked;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsBlocked);
            _tmpIsBlocked = _tmp != 0;
            final boolean _tmpIsFavorite;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp_1 != 0;
            final String _tmpNotificationSound;
            if (_cursor.isNull(_cursorIndexOfNotificationSound)) {
              _tmpNotificationSound = null;
            } else {
              _tmpNotificationSound = _cursor.getString(_cursorIndexOfNotificationSound);
            }
            final String _tmpCustomNickname;
            if (_cursor.isNull(_cursorIndexOfCustomNickname)) {
              _tmpCustomNickname = null;
            } else {
              _tmpCustomNickname = _cursor.getString(_cursorIndexOfCustomNickname);
            }
            final long _tmpTokens;
            _tmpTokens = _cursor.getLong(_cursorIndexOfTokens);
            final boolean _tmpIsPremium;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsPremium);
            _tmpIsPremium = _tmp_2 != 0;
            _result = new UserEntity(_tmpId,_tmpNickname,_tmpAvatarUrl,_tmpStatus,_tmpLastSeen,_tmpBio,_tmpIsBlocked,_tmpIsFavorite,_tmpNotificationSound,_tmpCustomNickname,_tmpTokens,_tmpIsPremium);
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
  public Flow<List<UserEntity>> getAllUsers() {
    final String _sql = "SELECT * FROM users";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"users"}, new Callable<List<UserEntity>>() {
      @Override
      @NonNull
      public List<UserEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfNickname = CursorUtil.getColumnIndexOrThrow(_cursor, "nickname");
          final int _cursorIndexOfAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "avatarUrl");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfLastSeen = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeen");
          final int _cursorIndexOfBio = CursorUtil.getColumnIndexOrThrow(_cursor, "bio");
          final int _cursorIndexOfIsBlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isBlocked");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfNotificationSound = CursorUtil.getColumnIndexOrThrow(_cursor, "notificationSound");
          final int _cursorIndexOfCustomNickname = CursorUtil.getColumnIndexOrThrow(_cursor, "customNickname");
          final int _cursorIndexOfTokens = CursorUtil.getColumnIndexOrThrow(_cursor, "tokens");
          final int _cursorIndexOfIsPremium = CursorUtil.getColumnIndexOrThrow(_cursor, "isPremium");
          final List<UserEntity> _result = new ArrayList<UserEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final UserEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpNickname;
            _tmpNickname = _cursor.getString(_cursorIndexOfNickname);
            final String _tmpAvatarUrl;
            if (_cursor.isNull(_cursorIndexOfAvatarUrl)) {
              _tmpAvatarUrl = null;
            } else {
              _tmpAvatarUrl = _cursor.getString(_cursorIndexOfAvatarUrl);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpLastSeen;
            if (_cursor.isNull(_cursorIndexOfLastSeen)) {
              _tmpLastSeen = null;
            } else {
              _tmpLastSeen = _cursor.getString(_cursorIndexOfLastSeen);
            }
            final String _tmpBio;
            _tmpBio = _cursor.getString(_cursorIndexOfBio);
            final boolean _tmpIsBlocked;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsBlocked);
            _tmpIsBlocked = _tmp != 0;
            final boolean _tmpIsFavorite;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp_1 != 0;
            final String _tmpNotificationSound;
            if (_cursor.isNull(_cursorIndexOfNotificationSound)) {
              _tmpNotificationSound = null;
            } else {
              _tmpNotificationSound = _cursor.getString(_cursorIndexOfNotificationSound);
            }
            final String _tmpCustomNickname;
            if (_cursor.isNull(_cursorIndexOfCustomNickname)) {
              _tmpCustomNickname = null;
            } else {
              _tmpCustomNickname = _cursor.getString(_cursorIndexOfCustomNickname);
            }
            final long _tmpTokens;
            _tmpTokens = _cursor.getLong(_cursorIndexOfTokens);
            final boolean _tmpIsPremium;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsPremium);
            _tmpIsPremium = _tmp_2 != 0;
            _item = new UserEntity(_tmpId,_tmpNickname,_tmpAvatarUrl,_tmpStatus,_tmpLastSeen,_tmpBio,_tmpIsBlocked,_tmpIsFavorite,_tmpNotificationSound,_tmpCustomNickname,_tmpTokens,_tmpIsPremium);
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
