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
import com.rcq.messenger.domain.model.MessageEntity;
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
public final class MessageDao_Impl implements MessageDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<MessageEntity> __insertionAdapterOfMessageEntity;

  private final EntityDeletionOrUpdateAdapter<MessageEntity> __updateAdapterOfMessageEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteMessage;

  private final SharedSQLiteStatement __preparedStmtOfDeleteChatMessages;

  public MessageDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMessageEntity = new EntityInsertionAdapter<MessageEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `messages` (`id`,`chatId`,`senderId`,`content`,`type`,`timestamp`,`status`,`replyToId`,`attachmentUrl`,`attachmentType`,`attachmentSize`,`isEdited`,`editedAt`,`ciphertext`,`signalType`,`isEncrypted`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MessageEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getChatId());
        statement.bindLong(3, entity.getSenderId());
        statement.bindString(4, entity.getContent());
        statement.bindString(5, entity.getType());
        statement.bindLong(6, entity.getTimestamp());
        statement.bindString(7, entity.getStatus());
        if (entity.getReplyToId() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getReplyToId());
        }
        if (entity.getAttachmentUrl() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getAttachmentUrl());
        }
        if (entity.getAttachmentType() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getAttachmentType());
        }
        if (entity.getAttachmentSize() == null) {
          statement.bindNull(11);
        } else {
          statement.bindLong(11, entity.getAttachmentSize());
        }
        final int _tmp = entity.isEdited() ? 1 : 0;
        statement.bindLong(12, _tmp);
        if (entity.getEditedAt() == null) {
          statement.bindNull(13);
        } else {
          statement.bindLong(13, entity.getEditedAt());
        }
        if (entity.getCiphertext() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getCiphertext());
        }
        statement.bindLong(15, entity.getSignalType());
        final int _tmp_1 = entity.isEncrypted() ? 1 : 0;
        statement.bindLong(16, _tmp_1);
      }
    };
    this.__updateAdapterOfMessageEntity = new EntityDeletionOrUpdateAdapter<MessageEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `messages` SET `id` = ?,`chatId` = ?,`senderId` = ?,`content` = ?,`type` = ?,`timestamp` = ?,`status` = ?,`replyToId` = ?,`attachmentUrl` = ?,`attachmentType` = ?,`attachmentSize` = ?,`isEdited` = ?,`editedAt` = ?,`ciphertext` = ?,`signalType` = ?,`isEncrypted` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MessageEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getChatId());
        statement.bindLong(3, entity.getSenderId());
        statement.bindString(4, entity.getContent());
        statement.bindString(5, entity.getType());
        statement.bindLong(6, entity.getTimestamp());
        statement.bindString(7, entity.getStatus());
        if (entity.getReplyToId() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getReplyToId());
        }
        if (entity.getAttachmentUrl() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getAttachmentUrl());
        }
        if (entity.getAttachmentType() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getAttachmentType());
        }
        if (entity.getAttachmentSize() == null) {
          statement.bindNull(11);
        } else {
          statement.bindLong(11, entity.getAttachmentSize());
        }
        final int _tmp = entity.isEdited() ? 1 : 0;
        statement.bindLong(12, _tmp);
        if (entity.getEditedAt() == null) {
          statement.bindNull(13);
        } else {
          statement.bindLong(13, entity.getEditedAt());
        }
        if (entity.getCiphertext() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getCiphertext());
        }
        statement.bindLong(15, entity.getSignalType());
        final int _tmp_1 = entity.isEncrypted() ? 1 : 0;
        statement.bindLong(16, _tmp_1);
        statement.bindString(17, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteMessage = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM messages WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteChatMessages = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM messages WHERE chatId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertMessage(final MessageEntity message,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMessageEntity.insert(message);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertMessages(final List<MessageEntity> messages,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMessageEntity.insert(messages);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateMessage(final MessageEntity message,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfMessageEntity.handle(message);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteMessage(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteMessage.acquire();
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
          __preparedStmtOfDeleteMessage.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteChatMessages(final String chatId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteChatMessages.acquire();
        int _argIndex = 1;
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
          __preparedStmtOfDeleteChatMessages.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<MessageEntity>> getMessages(final String chatId, final int limit,
      final int offset) {
    final String _sql = "SELECT * FROM messages WHERE chatId = ? ORDER BY timestamp DESC LIMIT ? OFFSET ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, chatId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, limit);
    _argIndex = 3;
    _statement.bindLong(_argIndex, offset);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"messages"}, new Callable<List<MessageEntity>>() {
      @Override
      @NonNull
      public List<MessageEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfChatId = CursorUtil.getColumnIndexOrThrow(_cursor, "chatId");
          final int _cursorIndexOfSenderId = CursorUtil.getColumnIndexOrThrow(_cursor, "senderId");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfReplyToId = CursorUtil.getColumnIndexOrThrow(_cursor, "replyToId");
          final int _cursorIndexOfAttachmentUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "attachmentUrl");
          final int _cursorIndexOfAttachmentType = CursorUtil.getColumnIndexOrThrow(_cursor, "attachmentType");
          final int _cursorIndexOfAttachmentSize = CursorUtil.getColumnIndexOrThrow(_cursor, "attachmentSize");
          final int _cursorIndexOfIsEdited = CursorUtil.getColumnIndexOrThrow(_cursor, "isEdited");
          final int _cursorIndexOfEditedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "editedAt");
          final int _cursorIndexOfCiphertext = CursorUtil.getColumnIndexOrThrow(_cursor, "ciphertext");
          final int _cursorIndexOfSignalType = CursorUtil.getColumnIndexOrThrow(_cursor, "signalType");
          final int _cursorIndexOfIsEncrypted = CursorUtil.getColumnIndexOrThrow(_cursor, "isEncrypted");
          final List<MessageEntity> _result = new ArrayList<MessageEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MessageEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpChatId;
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId);
            final long _tmpSenderId;
            _tmpSenderId = _cursor.getLong(_cursorIndexOfSenderId);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpReplyToId;
            if (_cursor.isNull(_cursorIndexOfReplyToId)) {
              _tmpReplyToId = null;
            } else {
              _tmpReplyToId = _cursor.getString(_cursorIndexOfReplyToId);
            }
            final String _tmpAttachmentUrl;
            if (_cursor.isNull(_cursorIndexOfAttachmentUrl)) {
              _tmpAttachmentUrl = null;
            } else {
              _tmpAttachmentUrl = _cursor.getString(_cursorIndexOfAttachmentUrl);
            }
            final String _tmpAttachmentType;
            if (_cursor.isNull(_cursorIndexOfAttachmentType)) {
              _tmpAttachmentType = null;
            } else {
              _tmpAttachmentType = _cursor.getString(_cursorIndexOfAttachmentType);
            }
            final Long _tmpAttachmentSize;
            if (_cursor.isNull(_cursorIndexOfAttachmentSize)) {
              _tmpAttachmentSize = null;
            } else {
              _tmpAttachmentSize = _cursor.getLong(_cursorIndexOfAttachmentSize);
            }
            final boolean _tmpIsEdited;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsEdited);
            _tmpIsEdited = _tmp != 0;
            final Long _tmpEditedAt;
            if (_cursor.isNull(_cursorIndexOfEditedAt)) {
              _tmpEditedAt = null;
            } else {
              _tmpEditedAt = _cursor.getLong(_cursorIndexOfEditedAt);
            }
            final String _tmpCiphertext;
            if (_cursor.isNull(_cursorIndexOfCiphertext)) {
              _tmpCiphertext = null;
            } else {
              _tmpCiphertext = _cursor.getString(_cursorIndexOfCiphertext);
            }
            final int _tmpSignalType;
            _tmpSignalType = _cursor.getInt(_cursorIndexOfSignalType);
            final boolean _tmpIsEncrypted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsEncrypted);
            _tmpIsEncrypted = _tmp_1 != 0;
            _item = new MessageEntity(_tmpId,_tmpChatId,_tmpSenderId,_tmpContent,_tmpType,_tmpTimestamp,_tmpStatus,_tmpReplyToId,_tmpAttachmentUrl,_tmpAttachmentType,_tmpAttachmentSize,_tmpIsEdited,_tmpEditedAt,_tmpCiphertext,_tmpSignalType,_tmpIsEncrypted);
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
  public Object getMessagesBefore(final String chatId, final long before, final int limit,
      final Continuation<? super List<MessageEntity>> $completion) {
    final String _sql = "SELECT * FROM messages WHERE chatId = ? AND timestamp < ? ORDER BY timestamp DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, chatId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, before);
    _argIndex = 3;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MessageEntity>>() {
      @Override
      @NonNull
      public List<MessageEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfChatId = CursorUtil.getColumnIndexOrThrow(_cursor, "chatId");
          final int _cursorIndexOfSenderId = CursorUtil.getColumnIndexOrThrow(_cursor, "senderId");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfReplyToId = CursorUtil.getColumnIndexOrThrow(_cursor, "replyToId");
          final int _cursorIndexOfAttachmentUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "attachmentUrl");
          final int _cursorIndexOfAttachmentType = CursorUtil.getColumnIndexOrThrow(_cursor, "attachmentType");
          final int _cursorIndexOfAttachmentSize = CursorUtil.getColumnIndexOrThrow(_cursor, "attachmentSize");
          final int _cursorIndexOfIsEdited = CursorUtil.getColumnIndexOrThrow(_cursor, "isEdited");
          final int _cursorIndexOfEditedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "editedAt");
          final int _cursorIndexOfCiphertext = CursorUtil.getColumnIndexOrThrow(_cursor, "ciphertext");
          final int _cursorIndexOfSignalType = CursorUtil.getColumnIndexOrThrow(_cursor, "signalType");
          final int _cursorIndexOfIsEncrypted = CursorUtil.getColumnIndexOrThrow(_cursor, "isEncrypted");
          final List<MessageEntity> _result = new ArrayList<MessageEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MessageEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpChatId;
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId);
            final long _tmpSenderId;
            _tmpSenderId = _cursor.getLong(_cursorIndexOfSenderId);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpReplyToId;
            if (_cursor.isNull(_cursorIndexOfReplyToId)) {
              _tmpReplyToId = null;
            } else {
              _tmpReplyToId = _cursor.getString(_cursorIndexOfReplyToId);
            }
            final String _tmpAttachmentUrl;
            if (_cursor.isNull(_cursorIndexOfAttachmentUrl)) {
              _tmpAttachmentUrl = null;
            } else {
              _tmpAttachmentUrl = _cursor.getString(_cursorIndexOfAttachmentUrl);
            }
            final String _tmpAttachmentType;
            if (_cursor.isNull(_cursorIndexOfAttachmentType)) {
              _tmpAttachmentType = null;
            } else {
              _tmpAttachmentType = _cursor.getString(_cursorIndexOfAttachmentType);
            }
            final Long _tmpAttachmentSize;
            if (_cursor.isNull(_cursorIndexOfAttachmentSize)) {
              _tmpAttachmentSize = null;
            } else {
              _tmpAttachmentSize = _cursor.getLong(_cursorIndexOfAttachmentSize);
            }
            final boolean _tmpIsEdited;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsEdited);
            _tmpIsEdited = _tmp != 0;
            final Long _tmpEditedAt;
            if (_cursor.isNull(_cursorIndexOfEditedAt)) {
              _tmpEditedAt = null;
            } else {
              _tmpEditedAt = _cursor.getLong(_cursorIndexOfEditedAt);
            }
            final String _tmpCiphertext;
            if (_cursor.isNull(_cursorIndexOfCiphertext)) {
              _tmpCiphertext = null;
            } else {
              _tmpCiphertext = _cursor.getString(_cursorIndexOfCiphertext);
            }
            final int _tmpSignalType;
            _tmpSignalType = _cursor.getInt(_cursorIndexOfSignalType);
            final boolean _tmpIsEncrypted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsEncrypted);
            _tmpIsEncrypted = _tmp_1 != 0;
            _item = new MessageEntity(_tmpId,_tmpChatId,_tmpSenderId,_tmpContent,_tmpType,_tmpTimestamp,_tmpStatus,_tmpReplyToId,_tmpAttachmentUrl,_tmpAttachmentType,_tmpAttachmentSize,_tmpIsEdited,_tmpEditedAt,_tmpCiphertext,_tmpSignalType,_tmpIsEncrypted);
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
  public Object getMessage(final String id, final Continuation<? super MessageEntity> $completion) {
    final String _sql = "SELECT * FROM messages WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<MessageEntity>() {
      @Override
      @Nullable
      public MessageEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfChatId = CursorUtil.getColumnIndexOrThrow(_cursor, "chatId");
          final int _cursorIndexOfSenderId = CursorUtil.getColumnIndexOrThrow(_cursor, "senderId");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfReplyToId = CursorUtil.getColumnIndexOrThrow(_cursor, "replyToId");
          final int _cursorIndexOfAttachmentUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "attachmentUrl");
          final int _cursorIndexOfAttachmentType = CursorUtil.getColumnIndexOrThrow(_cursor, "attachmentType");
          final int _cursorIndexOfAttachmentSize = CursorUtil.getColumnIndexOrThrow(_cursor, "attachmentSize");
          final int _cursorIndexOfIsEdited = CursorUtil.getColumnIndexOrThrow(_cursor, "isEdited");
          final int _cursorIndexOfEditedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "editedAt");
          final int _cursorIndexOfCiphertext = CursorUtil.getColumnIndexOrThrow(_cursor, "ciphertext");
          final int _cursorIndexOfSignalType = CursorUtil.getColumnIndexOrThrow(_cursor, "signalType");
          final int _cursorIndexOfIsEncrypted = CursorUtil.getColumnIndexOrThrow(_cursor, "isEncrypted");
          final MessageEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpChatId;
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId);
            final long _tmpSenderId;
            _tmpSenderId = _cursor.getLong(_cursorIndexOfSenderId);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpReplyToId;
            if (_cursor.isNull(_cursorIndexOfReplyToId)) {
              _tmpReplyToId = null;
            } else {
              _tmpReplyToId = _cursor.getString(_cursorIndexOfReplyToId);
            }
            final String _tmpAttachmentUrl;
            if (_cursor.isNull(_cursorIndexOfAttachmentUrl)) {
              _tmpAttachmentUrl = null;
            } else {
              _tmpAttachmentUrl = _cursor.getString(_cursorIndexOfAttachmentUrl);
            }
            final String _tmpAttachmentType;
            if (_cursor.isNull(_cursorIndexOfAttachmentType)) {
              _tmpAttachmentType = null;
            } else {
              _tmpAttachmentType = _cursor.getString(_cursorIndexOfAttachmentType);
            }
            final Long _tmpAttachmentSize;
            if (_cursor.isNull(_cursorIndexOfAttachmentSize)) {
              _tmpAttachmentSize = null;
            } else {
              _tmpAttachmentSize = _cursor.getLong(_cursorIndexOfAttachmentSize);
            }
            final boolean _tmpIsEdited;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsEdited);
            _tmpIsEdited = _tmp != 0;
            final Long _tmpEditedAt;
            if (_cursor.isNull(_cursorIndexOfEditedAt)) {
              _tmpEditedAt = null;
            } else {
              _tmpEditedAt = _cursor.getLong(_cursorIndexOfEditedAt);
            }
            final String _tmpCiphertext;
            if (_cursor.isNull(_cursorIndexOfCiphertext)) {
              _tmpCiphertext = null;
            } else {
              _tmpCiphertext = _cursor.getString(_cursorIndexOfCiphertext);
            }
            final int _tmpSignalType;
            _tmpSignalType = _cursor.getInt(_cursorIndexOfSignalType);
            final boolean _tmpIsEncrypted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsEncrypted);
            _tmpIsEncrypted = _tmp_1 != 0;
            _result = new MessageEntity(_tmpId,_tmpChatId,_tmpSenderId,_tmpContent,_tmpType,_tmpTimestamp,_tmpStatus,_tmpReplyToId,_tmpAttachmentUrl,_tmpAttachmentType,_tmpAttachmentSize,_tmpIsEdited,_tmpEditedAt,_tmpCiphertext,_tmpSignalType,_tmpIsEncrypted);
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
