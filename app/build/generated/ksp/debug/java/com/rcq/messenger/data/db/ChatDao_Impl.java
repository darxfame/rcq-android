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
import com.rcq.messenger.domain.model.ChatEntity;
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
public final class ChatDao_Impl implements ChatDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ChatEntity> __insertionAdapterOfChatEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteChat;

  private final SharedSQLiteStatement __preparedStmtOfIncrementUnreadCount;

  private final SharedSQLiteStatement __preparedStmtOfUpdateTimestamp;

  public ChatDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfChatEntity = new EntityInsertionAdapter<ChatEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `chats` (`id`,`targetId`,`targetNickname`,`targetAvatar`,`unreadCount`,`isPinned`,`isMuted`,`isArchived`,`createdAt`,`updatedAt`,`lastMessageContent`,`lastMessageTimestamp`,`lastMessageKind`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ChatEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindLong(2, entity.getTargetId());
        statement.bindString(3, entity.getTargetNickname());
        if (entity.getTargetAvatar() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getTargetAvatar());
        }
        statement.bindLong(5, entity.getUnreadCount());
        final int _tmp = entity.isPinned() ? 1 : 0;
        statement.bindLong(6, _tmp);
        final int _tmp_1 = entity.isMuted() ? 1 : 0;
        statement.bindLong(7, _tmp_1);
        final int _tmp_2 = entity.isArchived() ? 1 : 0;
        statement.bindLong(8, _tmp_2);
        statement.bindLong(9, entity.getCreatedAt());
        statement.bindLong(10, entity.getUpdatedAt());
        if (entity.getLastMessageContent() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getLastMessageContent());
        }
        if (entity.getLastMessageTimestamp() == null) {
          statement.bindNull(12);
        } else {
          statement.bindLong(12, entity.getLastMessageTimestamp());
        }
        if (entity.getLastMessageKind() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getLastMessageKind());
        }
      }
    };
    this.__preparedStmtOfDeleteChat = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM chats WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfIncrementUnreadCount = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE chats SET unreadCount = unreadCount + 1, updatedAt = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateTimestamp = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE chats SET updatedAt = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertChat(final ChatEntity chat, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfChatEntity.insert(chat);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertChats(final List<ChatEntity> chats,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfChatEntity.insert(chats);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteChat(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteChat.acquire();
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
          __preparedStmtOfDeleteChat.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object incrementUnreadCount(final String chatId, final long timestamp,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfIncrementUnreadCount.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, timestamp);
        _argIndex = 2;
        _stmt.bindString(_argIndex, chatId);
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
          __preparedStmtOfIncrementUnreadCount.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateTimestamp(final String chatId, final long timestamp,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateTimestamp.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, timestamp);
        _argIndex = 2;
        _stmt.bindString(_argIndex, chatId);
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
          __preparedStmtOfUpdateTimestamp.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ChatEntity>> getChats() {
    final String _sql = "SELECT * FROM chats WHERE isArchived = 0 ORDER BY updatedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"chats"}, new Callable<List<ChatEntity>>() {
      @Override
      @NonNull
      public List<ChatEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTargetId = CursorUtil.getColumnIndexOrThrow(_cursor, "targetId");
          final int _cursorIndexOfTargetNickname = CursorUtil.getColumnIndexOrThrow(_cursor, "targetNickname");
          final int _cursorIndexOfTargetAvatar = CursorUtil.getColumnIndexOrThrow(_cursor, "targetAvatar");
          final int _cursorIndexOfUnreadCount = CursorUtil.getColumnIndexOrThrow(_cursor, "unreadCount");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "isPinned");
          final int _cursorIndexOfIsMuted = CursorUtil.getColumnIndexOrThrow(_cursor, "isMuted");
          final int _cursorIndexOfIsArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "isArchived");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfLastMessageContent = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMessageContent");
          final int _cursorIndexOfLastMessageTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMessageTimestamp");
          final int _cursorIndexOfLastMessageKind = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMessageKind");
          final List<ChatEntity> _result = new ArrayList<ChatEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ChatEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
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
            final int _tmpUnreadCount;
            _tmpUnreadCount = _cursor.getInt(_cursorIndexOfUnreadCount);
            final boolean _tmpIsPinned;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp != 0;
            final boolean _tmpIsMuted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsMuted);
            _tmpIsMuted = _tmp_1 != 0;
            final boolean _tmpIsArchived;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsArchived);
            _tmpIsArchived = _tmp_2 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final String _tmpLastMessageContent;
            if (_cursor.isNull(_cursorIndexOfLastMessageContent)) {
              _tmpLastMessageContent = null;
            } else {
              _tmpLastMessageContent = _cursor.getString(_cursorIndexOfLastMessageContent);
            }
            final Long _tmpLastMessageTimestamp;
            if (_cursor.isNull(_cursorIndexOfLastMessageTimestamp)) {
              _tmpLastMessageTimestamp = null;
            } else {
              _tmpLastMessageTimestamp = _cursor.getLong(_cursorIndexOfLastMessageTimestamp);
            }
            final String _tmpLastMessageKind;
            if (_cursor.isNull(_cursorIndexOfLastMessageKind)) {
              _tmpLastMessageKind = null;
            } else {
              _tmpLastMessageKind = _cursor.getString(_cursorIndexOfLastMessageKind);
            }
            _item = new ChatEntity(_tmpId,_tmpTargetId,_tmpTargetNickname,_tmpTargetAvatar,_tmpUnreadCount,_tmpIsPinned,_tmpIsMuted,_tmpIsArchived,_tmpCreatedAt,_tmpUpdatedAt,_tmpLastMessageContent,_tmpLastMessageTimestamp,_tmpLastMessageKind);
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
  public Flow<List<ChatEntity>> getArchivedChats() {
    final String _sql = "SELECT * FROM chats WHERE isArchived = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"chats"}, new Callable<List<ChatEntity>>() {
      @Override
      @NonNull
      public List<ChatEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTargetId = CursorUtil.getColumnIndexOrThrow(_cursor, "targetId");
          final int _cursorIndexOfTargetNickname = CursorUtil.getColumnIndexOrThrow(_cursor, "targetNickname");
          final int _cursorIndexOfTargetAvatar = CursorUtil.getColumnIndexOrThrow(_cursor, "targetAvatar");
          final int _cursorIndexOfUnreadCount = CursorUtil.getColumnIndexOrThrow(_cursor, "unreadCount");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "isPinned");
          final int _cursorIndexOfIsMuted = CursorUtil.getColumnIndexOrThrow(_cursor, "isMuted");
          final int _cursorIndexOfIsArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "isArchived");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfLastMessageContent = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMessageContent");
          final int _cursorIndexOfLastMessageTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMessageTimestamp");
          final int _cursorIndexOfLastMessageKind = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMessageKind");
          final List<ChatEntity> _result = new ArrayList<ChatEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ChatEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
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
            final int _tmpUnreadCount;
            _tmpUnreadCount = _cursor.getInt(_cursorIndexOfUnreadCount);
            final boolean _tmpIsPinned;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp != 0;
            final boolean _tmpIsMuted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsMuted);
            _tmpIsMuted = _tmp_1 != 0;
            final boolean _tmpIsArchived;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsArchived);
            _tmpIsArchived = _tmp_2 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final String _tmpLastMessageContent;
            if (_cursor.isNull(_cursorIndexOfLastMessageContent)) {
              _tmpLastMessageContent = null;
            } else {
              _tmpLastMessageContent = _cursor.getString(_cursorIndexOfLastMessageContent);
            }
            final Long _tmpLastMessageTimestamp;
            if (_cursor.isNull(_cursorIndexOfLastMessageTimestamp)) {
              _tmpLastMessageTimestamp = null;
            } else {
              _tmpLastMessageTimestamp = _cursor.getLong(_cursorIndexOfLastMessageTimestamp);
            }
            final String _tmpLastMessageKind;
            if (_cursor.isNull(_cursorIndexOfLastMessageKind)) {
              _tmpLastMessageKind = null;
            } else {
              _tmpLastMessageKind = _cursor.getString(_cursorIndexOfLastMessageKind);
            }
            _item = new ChatEntity(_tmpId,_tmpTargetId,_tmpTargetNickname,_tmpTargetAvatar,_tmpUnreadCount,_tmpIsPinned,_tmpIsMuted,_tmpIsArchived,_tmpCreatedAt,_tmpUpdatedAt,_tmpLastMessageContent,_tmpLastMessageTimestamp,_tmpLastMessageKind);
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
  public Object getChat(final String id, final Continuation<? super ChatEntity> $completion) {
    final String _sql = "SELECT * FROM chats WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ChatEntity>() {
      @Override
      @Nullable
      public ChatEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTargetId = CursorUtil.getColumnIndexOrThrow(_cursor, "targetId");
          final int _cursorIndexOfTargetNickname = CursorUtil.getColumnIndexOrThrow(_cursor, "targetNickname");
          final int _cursorIndexOfTargetAvatar = CursorUtil.getColumnIndexOrThrow(_cursor, "targetAvatar");
          final int _cursorIndexOfUnreadCount = CursorUtil.getColumnIndexOrThrow(_cursor, "unreadCount");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "isPinned");
          final int _cursorIndexOfIsMuted = CursorUtil.getColumnIndexOrThrow(_cursor, "isMuted");
          final int _cursorIndexOfIsArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "isArchived");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfLastMessageContent = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMessageContent");
          final int _cursorIndexOfLastMessageTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMessageTimestamp");
          final int _cursorIndexOfLastMessageKind = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMessageKind");
          final ChatEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
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
            final int _tmpUnreadCount;
            _tmpUnreadCount = _cursor.getInt(_cursorIndexOfUnreadCount);
            final boolean _tmpIsPinned;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp != 0;
            final boolean _tmpIsMuted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsMuted);
            _tmpIsMuted = _tmp_1 != 0;
            final boolean _tmpIsArchived;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsArchived);
            _tmpIsArchived = _tmp_2 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final String _tmpLastMessageContent;
            if (_cursor.isNull(_cursorIndexOfLastMessageContent)) {
              _tmpLastMessageContent = null;
            } else {
              _tmpLastMessageContent = _cursor.getString(_cursorIndexOfLastMessageContent);
            }
            final Long _tmpLastMessageTimestamp;
            if (_cursor.isNull(_cursorIndexOfLastMessageTimestamp)) {
              _tmpLastMessageTimestamp = null;
            } else {
              _tmpLastMessageTimestamp = _cursor.getLong(_cursorIndexOfLastMessageTimestamp);
            }
            final String _tmpLastMessageKind;
            if (_cursor.isNull(_cursorIndexOfLastMessageKind)) {
              _tmpLastMessageKind = null;
            } else {
              _tmpLastMessageKind = _cursor.getString(_cursorIndexOfLastMessageKind);
            }
            _result = new ChatEntity(_tmpId,_tmpTargetId,_tmpTargetNickname,_tmpTargetAvatar,_tmpUnreadCount,_tmpIsPinned,_tmpIsMuted,_tmpIsArchived,_tmpCreatedAt,_tmpUpdatedAt,_tmpLastMessageContent,_tmpLastMessageTimestamp,_tmpLastMessageKind);
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
  public Object getChatByTargetId(final long targetId,
      final Continuation<? super ChatEntity> $completion) {
    final String _sql = "SELECT * FROM chats WHERE targetId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, targetId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ChatEntity>() {
      @Override
      @Nullable
      public ChatEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTargetId = CursorUtil.getColumnIndexOrThrow(_cursor, "targetId");
          final int _cursorIndexOfTargetNickname = CursorUtil.getColumnIndexOrThrow(_cursor, "targetNickname");
          final int _cursorIndexOfTargetAvatar = CursorUtil.getColumnIndexOrThrow(_cursor, "targetAvatar");
          final int _cursorIndexOfUnreadCount = CursorUtil.getColumnIndexOrThrow(_cursor, "unreadCount");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "isPinned");
          final int _cursorIndexOfIsMuted = CursorUtil.getColumnIndexOrThrow(_cursor, "isMuted");
          final int _cursorIndexOfIsArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "isArchived");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfLastMessageContent = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMessageContent");
          final int _cursorIndexOfLastMessageTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMessageTimestamp");
          final int _cursorIndexOfLastMessageKind = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMessageKind");
          final ChatEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
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
            final int _tmpUnreadCount;
            _tmpUnreadCount = _cursor.getInt(_cursorIndexOfUnreadCount);
            final boolean _tmpIsPinned;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp != 0;
            final boolean _tmpIsMuted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsMuted);
            _tmpIsMuted = _tmp_1 != 0;
            final boolean _tmpIsArchived;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsArchived);
            _tmpIsArchived = _tmp_2 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final String _tmpLastMessageContent;
            if (_cursor.isNull(_cursorIndexOfLastMessageContent)) {
              _tmpLastMessageContent = null;
            } else {
              _tmpLastMessageContent = _cursor.getString(_cursorIndexOfLastMessageContent);
            }
            final Long _tmpLastMessageTimestamp;
            if (_cursor.isNull(_cursorIndexOfLastMessageTimestamp)) {
              _tmpLastMessageTimestamp = null;
            } else {
              _tmpLastMessageTimestamp = _cursor.getLong(_cursorIndexOfLastMessageTimestamp);
            }
            final String _tmpLastMessageKind;
            if (_cursor.isNull(_cursorIndexOfLastMessageKind)) {
              _tmpLastMessageKind = null;
            } else {
              _tmpLastMessageKind = _cursor.getString(_cursorIndexOfLastMessageKind);
            }
            _result = new ChatEntity(_tmpId,_tmpTargetId,_tmpTargetNickname,_tmpTargetAvatar,_tmpUnreadCount,_tmpIsPinned,_tmpIsMuted,_tmpIsArchived,_tmpCreatedAt,_tmpUpdatedAt,_tmpLastMessageContent,_tmpLastMessageTimestamp,_tmpLastMessageKind);
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
