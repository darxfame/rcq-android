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
public final class ContactDao_Impl implements ContactDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ContactEntity> __insertionAdapterOfContactEntity;

  private final EntityDeletionOrUpdateAdapter<ContactEntity> __deletionAdapterOfContactEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByUserId;

  private final SharedSQLiteStatement __preparedStmtOfClearAll;

  private final SharedSQLiteStatement __preparedStmtOfBlockContact;

  public ContactDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfContactEntity = new EntityInsertionAdapter<ContactEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `contacts` (`userId`,`nickname`,`avatarUrl`,`status`,`lastSeen`,`isBlocked`,`isFavorite`,`notificationSound`,`customNickname`,`lastMessagePreview`,`lastMessageTime`) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ContactEntity entity) {
        statement.bindLong(1, entity.getUserId());
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
        final int _tmp = entity.isBlocked() ? 1 : 0;
        statement.bindLong(6, _tmp);
        final int _tmp_1 = entity.isFavorite() ? 1 : 0;
        statement.bindLong(7, _tmp_1);
        if (entity.getNotificationSound() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getNotificationSound());
        }
        if (entity.getCustomNickname() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getCustomNickname());
        }
        if (entity.getLastMessagePreview() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getLastMessagePreview());
        }
        statement.bindLong(11, entity.getLastMessageTime());
      }
    };
    this.__deletionAdapterOfContactEntity = new EntityDeletionOrUpdateAdapter<ContactEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `contacts` WHERE `userId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ContactEntity entity) {
        statement.bindLong(1, entity.getUserId());
      }
    };
    this.__preparedStmtOfDeleteByUserId = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM contacts WHERE userId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM contacts";
        return _query;
      }
    };
    this.__preparedStmtOfBlockContact = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE contacts SET isBlocked = 1 WHERE userId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertContact(final ContactEntity contact,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfContactEntity.insert(contact);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAll(final List<ContactEntity> contacts,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfContactEntity.insert(contacts);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteContact(final ContactEntity contact,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfContactEntity.handle(contact);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteByUserId(final long userId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByUserId.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, userId);
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
          __preparedStmtOfDeleteByUserId.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearAll.acquire();
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
          __preparedStmtOfClearAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object blockContact(final long userId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfBlockContact.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, userId);
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
          __preparedStmtOfBlockContact.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ContactEntity>> getContacts() {
    final String _sql = "SELECT * FROM contacts WHERE isBlocked = 0 ORDER BY nickname ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"contacts"}, new Callable<List<ContactEntity>>() {
      @Override
      @NonNull
      public List<ContactEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfNickname = CursorUtil.getColumnIndexOrThrow(_cursor, "nickname");
          final int _cursorIndexOfAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "avatarUrl");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfLastSeen = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeen");
          final int _cursorIndexOfIsBlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isBlocked");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfNotificationSound = CursorUtil.getColumnIndexOrThrow(_cursor, "notificationSound");
          final int _cursorIndexOfCustomNickname = CursorUtil.getColumnIndexOrThrow(_cursor, "customNickname");
          final int _cursorIndexOfLastMessagePreview = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMessagePreview");
          final int _cursorIndexOfLastMessageTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMessageTime");
          final List<ContactEntity> _result = new ArrayList<ContactEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ContactEntity _item;
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
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpLastSeen;
            if (_cursor.isNull(_cursorIndexOfLastSeen)) {
              _tmpLastSeen = null;
            } else {
              _tmpLastSeen = _cursor.getString(_cursorIndexOfLastSeen);
            }
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
            final String _tmpLastMessagePreview;
            if (_cursor.isNull(_cursorIndexOfLastMessagePreview)) {
              _tmpLastMessagePreview = null;
            } else {
              _tmpLastMessagePreview = _cursor.getString(_cursorIndexOfLastMessagePreview);
            }
            final long _tmpLastMessageTime;
            _tmpLastMessageTime = _cursor.getLong(_cursorIndexOfLastMessageTime);
            _item = new ContactEntity(_tmpUserId,_tmpNickname,_tmpAvatarUrl,_tmpStatus,_tmpLastSeen,_tmpIsBlocked,_tmpIsFavorite,_tmpNotificationSound,_tmpCustomNickname,_tmpLastMessagePreview,_tmpLastMessageTime);
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
  public Flow<List<ContactEntity>> getAllContacts() {
    final String _sql = "SELECT * FROM contacts";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"contacts"}, new Callable<List<ContactEntity>>() {
      @Override
      @NonNull
      public List<ContactEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfNickname = CursorUtil.getColumnIndexOrThrow(_cursor, "nickname");
          final int _cursorIndexOfAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "avatarUrl");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfLastSeen = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeen");
          final int _cursorIndexOfIsBlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isBlocked");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfNotificationSound = CursorUtil.getColumnIndexOrThrow(_cursor, "notificationSound");
          final int _cursorIndexOfCustomNickname = CursorUtil.getColumnIndexOrThrow(_cursor, "customNickname");
          final int _cursorIndexOfLastMessagePreview = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMessagePreview");
          final int _cursorIndexOfLastMessageTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMessageTime");
          final List<ContactEntity> _result = new ArrayList<ContactEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ContactEntity _item;
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
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpLastSeen;
            if (_cursor.isNull(_cursorIndexOfLastSeen)) {
              _tmpLastSeen = null;
            } else {
              _tmpLastSeen = _cursor.getString(_cursorIndexOfLastSeen);
            }
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
            final String _tmpLastMessagePreview;
            if (_cursor.isNull(_cursorIndexOfLastMessagePreview)) {
              _tmpLastMessagePreview = null;
            } else {
              _tmpLastMessagePreview = _cursor.getString(_cursorIndexOfLastMessagePreview);
            }
            final long _tmpLastMessageTime;
            _tmpLastMessageTime = _cursor.getLong(_cursorIndexOfLastMessageTime);
            _item = new ContactEntity(_tmpUserId,_tmpNickname,_tmpAvatarUrl,_tmpStatus,_tmpLastSeen,_tmpIsBlocked,_tmpIsFavorite,_tmpNotificationSound,_tmpCustomNickname,_tmpLastMessagePreview,_tmpLastMessageTime);
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
  public Flow<List<ContactEntity>> getBlockedContacts() {
    final String _sql = "SELECT * FROM contacts WHERE isBlocked = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"contacts"}, new Callable<List<ContactEntity>>() {
      @Override
      @NonNull
      public List<ContactEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfNickname = CursorUtil.getColumnIndexOrThrow(_cursor, "nickname");
          final int _cursorIndexOfAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "avatarUrl");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfLastSeen = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeen");
          final int _cursorIndexOfIsBlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isBlocked");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfNotificationSound = CursorUtil.getColumnIndexOrThrow(_cursor, "notificationSound");
          final int _cursorIndexOfCustomNickname = CursorUtil.getColumnIndexOrThrow(_cursor, "customNickname");
          final int _cursorIndexOfLastMessagePreview = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMessagePreview");
          final int _cursorIndexOfLastMessageTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMessageTime");
          final List<ContactEntity> _result = new ArrayList<ContactEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ContactEntity _item;
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
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpLastSeen;
            if (_cursor.isNull(_cursorIndexOfLastSeen)) {
              _tmpLastSeen = null;
            } else {
              _tmpLastSeen = _cursor.getString(_cursorIndexOfLastSeen);
            }
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
            final String _tmpLastMessagePreview;
            if (_cursor.isNull(_cursorIndexOfLastMessagePreview)) {
              _tmpLastMessagePreview = null;
            } else {
              _tmpLastMessagePreview = _cursor.getString(_cursorIndexOfLastMessagePreview);
            }
            final long _tmpLastMessageTime;
            _tmpLastMessageTime = _cursor.getLong(_cursorIndexOfLastMessageTime);
            _item = new ContactEntity(_tmpUserId,_tmpNickname,_tmpAvatarUrl,_tmpStatus,_tmpLastSeen,_tmpIsBlocked,_tmpIsFavorite,_tmpNotificationSound,_tmpCustomNickname,_tmpLastMessagePreview,_tmpLastMessageTime);
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
  public Object getContactByUserId(final long userId,
      final Continuation<? super ContactEntity> $completion) {
    final String _sql = "SELECT * FROM contacts WHERE userId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, userId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ContactEntity>() {
      @Override
      @Nullable
      public ContactEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfNickname = CursorUtil.getColumnIndexOrThrow(_cursor, "nickname");
          final int _cursorIndexOfAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "avatarUrl");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfLastSeen = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeen");
          final int _cursorIndexOfIsBlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isBlocked");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfNotificationSound = CursorUtil.getColumnIndexOrThrow(_cursor, "notificationSound");
          final int _cursorIndexOfCustomNickname = CursorUtil.getColumnIndexOrThrow(_cursor, "customNickname");
          final int _cursorIndexOfLastMessagePreview = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMessagePreview");
          final int _cursorIndexOfLastMessageTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMessageTime");
          final ContactEntity _result;
          if (_cursor.moveToFirst()) {
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
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpLastSeen;
            if (_cursor.isNull(_cursorIndexOfLastSeen)) {
              _tmpLastSeen = null;
            } else {
              _tmpLastSeen = _cursor.getString(_cursorIndexOfLastSeen);
            }
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
            final String _tmpLastMessagePreview;
            if (_cursor.isNull(_cursorIndexOfLastMessagePreview)) {
              _tmpLastMessagePreview = null;
            } else {
              _tmpLastMessagePreview = _cursor.getString(_cursorIndexOfLastMessagePreview);
            }
            final long _tmpLastMessageTime;
            _tmpLastMessageTime = _cursor.getLong(_cursorIndexOfLastMessageTime);
            _result = new ContactEntity(_tmpUserId,_tmpNickname,_tmpAvatarUrl,_tmpStatus,_tmpLastSeen,_tmpIsBlocked,_tmpIsFavorite,_tmpNotificationSound,_tmpCustomNickname,_tmpLastMessagePreview,_tmpLastMessageTime);
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
