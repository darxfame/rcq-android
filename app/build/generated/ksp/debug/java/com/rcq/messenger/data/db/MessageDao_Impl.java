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
import java.lang.Double;
import java.lang.Exception;
import java.lang.Integer;
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

  private final SharedSQLiteStatement __preparedStmtOfClearAll;

  private final SharedSQLiteStatement __preparedStmtOfDeleteMessage;

  private final SharedSQLiteStatement __preparedStmtOfDeleteChatMessages;

  private final SharedSQLiteStatement __preparedStmtOfUpdateMessageStatus;

  public MessageDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMessageEntity = new EntityInsertionAdapter<MessageEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `messages` (`id`,`chatId`,`senderId`,`content`,`timestamp`,`status`,`replyToId`,`editedAt`,`ciphertext`,`signalType`,`isEncrypted`,`isFromMe`,`kind`,`mediaId`,`receivedWhileAway`,`deletedForEveryone`,`reactions`,`thumbnailB64`,`durationSec`,`ttlSeconds`,`forwardedFromName`,`replyToContent`,`replyToAuthorName`,`premiumPriceTokens`,`premiumUnlocked`,`albumId`,`fileName`,`fileMime`,`fileSizeBytes`,`latitude`,`longitude`,`pollId`,`mentionedUserIds`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MessageEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getChatId());
        statement.bindLong(3, entity.getSenderId());
        statement.bindString(4, entity.getContent());
        statement.bindLong(5, entity.getTimestamp());
        statement.bindString(6, entity.getStatus());
        if (entity.getReplyToId() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getReplyToId());
        }
        if (entity.getEditedAt() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getEditedAt());
        }
        if (entity.getCiphertext() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getCiphertext());
        }
        statement.bindLong(10, entity.getSignalType());
        final int _tmp = entity.isEncrypted() ? 1 : 0;
        statement.bindLong(11, _tmp);
        final int _tmp_1 = entity.isFromMe() ? 1 : 0;
        statement.bindLong(12, _tmp_1);
        statement.bindString(13, entity.getKind());
        if (entity.getMediaId() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getMediaId());
        }
        final int _tmp_2 = entity.getReceivedWhileAway() ? 1 : 0;
        statement.bindLong(15, _tmp_2);
        final int _tmp_3 = entity.getDeletedForEveryone() ? 1 : 0;
        statement.bindLong(16, _tmp_3);
        if (entity.getReactions() == null) {
          statement.bindNull(17);
        } else {
          statement.bindString(17, entity.getReactions());
        }
        if (entity.getThumbnailB64() == null) {
          statement.bindNull(18);
        } else {
          statement.bindString(18, entity.getThumbnailB64());
        }
        statement.bindDouble(19, entity.getDurationSec());
        if (entity.getTtlSeconds() == null) {
          statement.bindNull(20);
        } else {
          statement.bindLong(20, entity.getTtlSeconds());
        }
        if (entity.getForwardedFromName() == null) {
          statement.bindNull(21);
        } else {
          statement.bindString(21, entity.getForwardedFromName());
        }
        if (entity.getReplyToContent() == null) {
          statement.bindNull(22);
        } else {
          statement.bindString(22, entity.getReplyToContent());
        }
        if (entity.getReplyToAuthorName() == null) {
          statement.bindNull(23);
        } else {
          statement.bindString(23, entity.getReplyToAuthorName());
        }
        if (entity.getPremiumPriceTokens() == null) {
          statement.bindNull(24);
        } else {
          statement.bindLong(24, entity.getPremiumPriceTokens());
        }
        final int _tmp_4 = entity.getPremiumUnlocked() ? 1 : 0;
        statement.bindLong(25, _tmp_4);
        if (entity.getAlbumId() == null) {
          statement.bindNull(26);
        } else {
          statement.bindString(26, entity.getAlbumId());
        }
        if (entity.getFileName() == null) {
          statement.bindNull(27);
        } else {
          statement.bindString(27, entity.getFileName());
        }
        if (entity.getFileMime() == null) {
          statement.bindNull(28);
        } else {
          statement.bindString(28, entity.getFileMime());
        }
        if (entity.getFileSizeBytes() == null) {
          statement.bindNull(29);
        } else {
          statement.bindLong(29, entity.getFileSizeBytes());
        }
        if (entity.getLatitude() == null) {
          statement.bindNull(30);
        } else {
          statement.bindDouble(30, entity.getLatitude());
        }
        if (entity.getLongitude() == null) {
          statement.bindNull(31);
        } else {
          statement.bindDouble(31, entity.getLongitude());
        }
        if (entity.getPollId() == null) {
          statement.bindNull(32);
        } else {
          statement.bindString(32, entity.getPollId());
        }
        if (entity.getMentionedUserIds() == null) {
          statement.bindNull(33);
        } else {
          statement.bindString(33, entity.getMentionedUserIds());
        }
      }
    };
    this.__updateAdapterOfMessageEntity = new EntityDeletionOrUpdateAdapter<MessageEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `messages` SET `id` = ?,`chatId` = ?,`senderId` = ?,`content` = ?,`timestamp` = ?,`status` = ?,`replyToId` = ?,`editedAt` = ?,`ciphertext` = ?,`signalType` = ?,`isEncrypted` = ?,`isFromMe` = ?,`kind` = ?,`mediaId` = ?,`receivedWhileAway` = ?,`deletedForEveryone` = ?,`reactions` = ?,`thumbnailB64` = ?,`durationSec` = ?,`ttlSeconds` = ?,`forwardedFromName` = ?,`replyToContent` = ?,`replyToAuthorName` = ?,`premiumPriceTokens` = ?,`premiumUnlocked` = ?,`albumId` = ?,`fileName` = ?,`fileMime` = ?,`fileSizeBytes` = ?,`latitude` = ?,`longitude` = ?,`pollId` = ?,`mentionedUserIds` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MessageEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getChatId());
        statement.bindLong(3, entity.getSenderId());
        statement.bindString(4, entity.getContent());
        statement.bindLong(5, entity.getTimestamp());
        statement.bindString(6, entity.getStatus());
        if (entity.getReplyToId() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getReplyToId());
        }
        if (entity.getEditedAt() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getEditedAt());
        }
        if (entity.getCiphertext() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getCiphertext());
        }
        statement.bindLong(10, entity.getSignalType());
        final int _tmp = entity.isEncrypted() ? 1 : 0;
        statement.bindLong(11, _tmp);
        final int _tmp_1 = entity.isFromMe() ? 1 : 0;
        statement.bindLong(12, _tmp_1);
        statement.bindString(13, entity.getKind());
        if (entity.getMediaId() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getMediaId());
        }
        final int _tmp_2 = entity.getReceivedWhileAway() ? 1 : 0;
        statement.bindLong(15, _tmp_2);
        final int _tmp_3 = entity.getDeletedForEveryone() ? 1 : 0;
        statement.bindLong(16, _tmp_3);
        if (entity.getReactions() == null) {
          statement.bindNull(17);
        } else {
          statement.bindString(17, entity.getReactions());
        }
        if (entity.getThumbnailB64() == null) {
          statement.bindNull(18);
        } else {
          statement.bindString(18, entity.getThumbnailB64());
        }
        statement.bindDouble(19, entity.getDurationSec());
        if (entity.getTtlSeconds() == null) {
          statement.bindNull(20);
        } else {
          statement.bindLong(20, entity.getTtlSeconds());
        }
        if (entity.getForwardedFromName() == null) {
          statement.bindNull(21);
        } else {
          statement.bindString(21, entity.getForwardedFromName());
        }
        if (entity.getReplyToContent() == null) {
          statement.bindNull(22);
        } else {
          statement.bindString(22, entity.getReplyToContent());
        }
        if (entity.getReplyToAuthorName() == null) {
          statement.bindNull(23);
        } else {
          statement.bindString(23, entity.getReplyToAuthorName());
        }
        if (entity.getPremiumPriceTokens() == null) {
          statement.bindNull(24);
        } else {
          statement.bindLong(24, entity.getPremiumPriceTokens());
        }
        final int _tmp_4 = entity.getPremiumUnlocked() ? 1 : 0;
        statement.bindLong(25, _tmp_4);
        if (entity.getAlbumId() == null) {
          statement.bindNull(26);
        } else {
          statement.bindString(26, entity.getAlbumId());
        }
        if (entity.getFileName() == null) {
          statement.bindNull(27);
        } else {
          statement.bindString(27, entity.getFileName());
        }
        if (entity.getFileMime() == null) {
          statement.bindNull(28);
        } else {
          statement.bindString(28, entity.getFileMime());
        }
        if (entity.getFileSizeBytes() == null) {
          statement.bindNull(29);
        } else {
          statement.bindLong(29, entity.getFileSizeBytes());
        }
        if (entity.getLatitude() == null) {
          statement.bindNull(30);
        } else {
          statement.bindDouble(30, entity.getLatitude());
        }
        if (entity.getLongitude() == null) {
          statement.bindNull(31);
        } else {
          statement.bindDouble(31, entity.getLongitude());
        }
        if (entity.getPollId() == null) {
          statement.bindNull(32);
        } else {
          statement.bindString(32, entity.getPollId());
        }
        if (entity.getMentionedUserIds() == null) {
          statement.bindNull(33);
        } else {
          statement.bindString(33, entity.getMentionedUserIds());
        }
        statement.bindString(34, entity.getId());
      }
    };
    this.__preparedStmtOfClearAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM messages";
        return _query;
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
    this.__preparedStmtOfUpdateMessageStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE messages SET status = ? WHERE id = ?";
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
  public Object updateMessageStatus(final String id, final String status,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateMessageStatus.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, status);
        _argIndex = 2;
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
          __preparedStmtOfUpdateMessageStatus.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<MessageEntity>> getMessages(final String chatId, final int limit,
      final int offset) {
    final String _sql = "SELECT * FROM messages WHERE chatId = ? ORDER BY timestamp ASC LIMIT ? OFFSET ?";
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
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfReplyToId = CursorUtil.getColumnIndexOrThrow(_cursor, "replyToId");
          final int _cursorIndexOfEditedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "editedAt");
          final int _cursorIndexOfCiphertext = CursorUtil.getColumnIndexOrThrow(_cursor, "ciphertext");
          final int _cursorIndexOfSignalType = CursorUtil.getColumnIndexOrThrow(_cursor, "signalType");
          final int _cursorIndexOfIsEncrypted = CursorUtil.getColumnIndexOrThrow(_cursor, "isEncrypted");
          final int _cursorIndexOfIsFromMe = CursorUtil.getColumnIndexOrThrow(_cursor, "isFromMe");
          final int _cursorIndexOfKind = CursorUtil.getColumnIndexOrThrow(_cursor, "kind");
          final int _cursorIndexOfMediaId = CursorUtil.getColumnIndexOrThrow(_cursor, "mediaId");
          final int _cursorIndexOfReceivedWhileAway = CursorUtil.getColumnIndexOrThrow(_cursor, "receivedWhileAway");
          final int _cursorIndexOfDeletedForEveryone = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedForEveryone");
          final int _cursorIndexOfReactions = CursorUtil.getColumnIndexOrThrow(_cursor, "reactions");
          final int _cursorIndexOfThumbnailB64 = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbnailB64");
          final int _cursorIndexOfDurationSec = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSec");
          final int _cursorIndexOfTtlSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "ttlSeconds");
          final int _cursorIndexOfForwardedFromName = CursorUtil.getColumnIndexOrThrow(_cursor, "forwardedFromName");
          final int _cursorIndexOfReplyToContent = CursorUtil.getColumnIndexOrThrow(_cursor, "replyToContent");
          final int _cursorIndexOfReplyToAuthorName = CursorUtil.getColumnIndexOrThrow(_cursor, "replyToAuthorName");
          final int _cursorIndexOfPremiumPriceTokens = CursorUtil.getColumnIndexOrThrow(_cursor, "premiumPriceTokens");
          final int _cursorIndexOfPremiumUnlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "premiumUnlocked");
          final int _cursorIndexOfAlbumId = CursorUtil.getColumnIndexOrThrow(_cursor, "albumId");
          final int _cursorIndexOfFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "fileName");
          final int _cursorIndexOfFileMime = CursorUtil.getColumnIndexOrThrow(_cursor, "fileMime");
          final int _cursorIndexOfFileSizeBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "fileSizeBytes");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfPollId = CursorUtil.getColumnIndexOrThrow(_cursor, "pollId");
          final int _cursorIndexOfMentionedUserIds = CursorUtil.getColumnIndexOrThrow(_cursor, "mentionedUserIds");
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
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsEncrypted);
            _tmpIsEncrypted = _tmp != 0;
            final boolean _tmpIsFromMe;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsFromMe);
            _tmpIsFromMe = _tmp_1 != 0;
            final String _tmpKind;
            _tmpKind = _cursor.getString(_cursorIndexOfKind);
            final String _tmpMediaId;
            if (_cursor.isNull(_cursorIndexOfMediaId)) {
              _tmpMediaId = null;
            } else {
              _tmpMediaId = _cursor.getString(_cursorIndexOfMediaId);
            }
            final boolean _tmpReceivedWhileAway;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfReceivedWhileAway);
            _tmpReceivedWhileAway = _tmp_2 != 0;
            final boolean _tmpDeletedForEveryone;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfDeletedForEveryone);
            _tmpDeletedForEveryone = _tmp_3 != 0;
            final String _tmpReactions;
            if (_cursor.isNull(_cursorIndexOfReactions)) {
              _tmpReactions = null;
            } else {
              _tmpReactions = _cursor.getString(_cursorIndexOfReactions);
            }
            final String _tmpThumbnailB64;
            if (_cursor.isNull(_cursorIndexOfThumbnailB64)) {
              _tmpThumbnailB64 = null;
            } else {
              _tmpThumbnailB64 = _cursor.getString(_cursorIndexOfThumbnailB64);
            }
            final double _tmpDurationSec;
            _tmpDurationSec = _cursor.getDouble(_cursorIndexOfDurationSec);
            final Integer _tmpTtlSeconds;
            if (_cursor.isNull(_cursorIndexOfTtlSeconds)) {
              _tmpTtlSeconds = null;
            } else {
              _tmpTtlSeconds = _cursor.getInt(_cursorIndexOfTtlSeconds);
            }
            final String _tmpForwardedFromName;
            if (_cursor.isNull(_cursorIndexOfForwardedFromName)) {
              _tmpForwardedFromName = null;
            } else {
              _tmpForwardedFromName = _cursor.getString(_cursorIndexOfForwardedFromName);
            }
            final String _tmpReplyToContent;
            if (_cursor.isNull(_cursorIndexOfReplyToContent)) {
              _tmpReplyToContent = null;
            } else {
              _tmpReplyToContent = _cursor.getString(_cursorIndexOfReplyToContent);
            }
            final String _tmpReplyToAuthorName;
            if (_cursor.isNull(_cursorIndexOfReplyToAuthorName)) {
              _tmpReplyToAuthorName = null;
            } else {
              _tmpReplyToAuthorName = _cursor.getString(_cursorIndexOfReplyToAuthorName);
            }
            final Integer _tmpPremiumPriceTokens;
            if (_cursor.isNull(_cursorIndexOfPremiumPriceTokens)) {
              _tmpPremiumPriceTokens = null;
            } else {
              _tmpPremiumPriceTokens = _cursor.getInt(_cursorIndexOfPremiumPriceTokens);
            }
            final boolean _tmpPremiumUnlocked;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfPremiumUnlocked);
            _tmpPremiumUnlocked = _tmp_4 != 0;
            final String _tmpAlbumId;
            if (_cursor.isNull(_cursorIndexOfAlbumId)) {
              _tmpAlbumId = null;
            } else {
              _tmpAlbumId = _cursor.getString(_cursorIndexOfAlbumId);
            }
            final String _tmpFileName;
            if (_cursor.isNull(_cursorIndexOfFileName)) {
              _tmpFileName = null;
            } else {
              _tmpFileName = _cursor.getString(_cursorIndexOfFileName);
            }
            final String _tmpFileMime;
            if (_cursor.isNull(_cursorIndexOfFileMime)) {
              _tmpFileMime = null;
            } else {
              _tmpFileMime = _cursor.getString(_cursorIndexOfFileMime);
            }
            final Long _tmpFileSizeBytes;
            if (_cursor.isNull(_cursorIndexOfFileSizeBytes)) {
              _tmpFileSizeBytes = null;
            } else {
              _tmpFileSizeBytes = _cursor.getLong(_cursorIndexOfFileSizeBytes);
            }
            final Double _tmpLatitude;
            if (_cursor.isNull(_cursorIndexOfLatitude)) {
              _tmpLatitude = null;
            } else {
              _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            }
            final Double _tmpLongitude;
            if (_cursor.isNull(_cursorIndexOfLongitude)) {
              _tmpLongitude = null;
            } else {
              _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            }
            final String _tmpPollId;
            if (_cursor.isNull(_cursorIndexOfPollId)) {
              _tmpPollId = null;
            } else {
              _tmpPollId = _cursor.getString(_cursorIndexOfPollId);
            }
            final String _tmpMentionedUserIds;
            if (_cursor.isNull(_cursorIndexOfMentionedUserIds)) {
              _tmpMentionedUserIds = null;
            } else {
              _tmpMentionedUserIds = _cursor.getString(_cursorIndexOfMentionedUserIds);
            }
            _item = new MessageEntity(_tmpId,_tmpChatId,_tmpSenderId,_tmpContent,_tmpTimestamp,_tmpStatus,_tmpReplyToId,_tmpEditedAt,_tmpCiphertext,_tmpSignalType,_tmpIsEncrypted,_tmpIsFromMe,_tmpKind,_tmpMediaId,_tmpReceivedWhileAway,_tmpDeletedForEveryone,_tmpReactions,_tmpThumbnailB64,_tmpDurationSec,_tmpTtlSeconds,_tmpForwardedFromName,_tmpReplyToContent,_tmpReplyToAuthorName,_tmpPremiumPriceTokens,_tmpPremiumUnlocked,_tmpAlbumId,_tmpFileName,_tmpFileMime,_tmpFileSizeBytes,_tmpLatitude,_tmpLongitude,_tmpPollId,_tmpMentionedUserIds);
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
    final String _sql = "SELECT * FROM messages WHERE chatId = ? AND timestamp < ? ORDER BY timestamp ASC LIMIT ?";
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
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfReplyToId = CursorUtil.getColumnIndexOrThrow(_cursor, "replyToId");
          final int _cursorIndexOfEditedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "editedAt");
          final int _cursorIndexOfCiphertext = CursorUtil.getColumnIndexOrThrow(_cursor, "ciphertext");
          final int _cursorIndexOfSignalType = CursorUtil.getColumnIndexOrThrow(_cursor, "signalType");
          final int _cursorIndexOfIsEncrypted = CursorUtil.getColumnIndexOrThrow(_cursor, "isEncrypted");
          final int _cursorIndexOfIsFromMe = CursorUtil.getColumnIndexOrThrow(_cursor, "isFromMe");
          final int _cursorIndexOfKind = CursorUtil.getColumnIndexOrThrow(_cursor, "kind");
          final int _cursorIndexOfMediaId = CursorUtil.getColumnIndexOrThrow(_cursor, "mediaId");
          final int _cursorIndexOfReceivedWhileAway = CursorUtil.getColumnIndexOrThrow(_cursor, "receivedWhileAway");
          final int _cursorIndexOfDeletedForEveryone = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedForEveryone");
          final int _cursorIndexOfReactions = CursorUtil.getColumnIndexOrThrow(_cursor, "reactions");
          final int _cursorIndexOfThumbnailB64 = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbnailB64");
          final int _cursorIndexOfDurationSec = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSec");
          final int _cursorIndexOfTtlSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "ttlSeconds");
          final int _cursorIndexOfForwardedFromName = CursorUtil.getColumnIndexOrThrow(_cursor, "forwardedFromName");
          final int _cursorIndexOfReplyToContent = CursorUtil.getColumnIndexOrThrow(_cursor, "replyToContent");
          final int _cursorIndexOfReplyToAuthorName = CursorUtil.getColumnIndexOrThrow(_cursor, "replyToAuthorName");
          final int _cursorIndexOfPremiumPriceTokens = CursorUtil.getColumnIndexOrThrow(_cursor, "premiumPriceTokens");
          final int _cursorIndexOfPremiumUnlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "premiumUnlocked");
          final int _cursorIndexOfAlbumId = CursorUtil.getColumnIndexOrThrow(_cursor, "albumId");
          final int _cursorIndexOfFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "fileName");
          final int _cursorIndexOfFileMime = CursorUtil.getColumnIndexOrThrow(_cursor, "fileMime");
          final int _cursorIndexOfFileSizeBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "fileSizeBytes");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfPollId = CursorUtil.getColumnIndexOrThrow(_cursor, "pollId");
          final int _cursorIndexOfMentionedUserIds = CursorUtil.getColumnIndexOrThrow(_cursor, "mentionedUserIds");
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
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsEncrypted);
            _tmpIsEncrypted = _tmp != 0;
            final boolean _tmpIsFromMe;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsFromMe);
            _tmpIsFromMe = _tmp_1 != 0;
            final String _tmpKind;
            _tmpKind = _cursor.getString(_cursorIndexOfKind);
            final String _tmpMediaId;
            if (_cursor.isNull(_cursorIndexOfMediaId)) {
              _tmpMediaId = null;
            } else {
              _tmpMediaId = _cursor.getString(_cursorIndexOfMediaId);
            }
            final boolean _tmpReceivedWhileAway;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfReceivedWhileAway);
            _tmpReceivedWhileAway = _tmp_2 != 0;
            final boolean _tmpDeletedForEveryone;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfDeletedForEveryone);
            _tmpDeletedForEveryone = _tmp_3 != 0;
            final String _tmpReactions;
            if (_cursor.isNull(_cursorIndexOfReactions)) {
              _tmpReactions = null;
            } else {
              _tmpReactions = _cursor.getString(_cursorIndexOfReactions);
            }
            final String _tmpThumbnailB64;
            if (_cursor.isNull(_cursorIndexOfThumbnailB64)) {
              _tmpThumbnailB64 = null;
            } else {
              _tmpThumbnailB64 = _cursor.getString(_cursorIndexOfThumbnailB64);
            }
            final double _tmpDurationSec;
            _tmpDurationSec = _cursor.getDouble(_cursorIndexOfDurationSec);
            final Integer _tmpTtlSeconds;
            if (_cursor.isNull(_cursorIndexOfTtlSeconds)) {
              _tmpTtlSeconds = null;
            } else {
              _tmpTtlSeconds = _cursor.getInt(_cursorIndexOfTtlSeconds);
            }
            final String _tmpForwardedFromName;
            if (_cursor.isNull(_cursorIndexOfForwardedFromName)) {
              _tmpForwardedFromName = null;
            } else {
              _tmpForwardedFromName = _cursor.getString(_cursorIndexOfForwardedFromName);
            }
            final String _tmpReplyToContent;
            if (_cursor.isNull(_cursorIndexOfReplyToContent)) {
              _tmpReplyToContent = null;
            } else {
              _tmpReplyToContent = _cursor.getString(_cursorIndexOfReplyToContent);
            }
            final String _tmpReplyToAuthorName;
            if (_cursor.isNull(_cursorIndexOfReplyToAuthorName)) {
              _tmpReplyToAuthorName = null;
            } else {
              _tmpReplyToAuthorName = _cursor.getString(_cursorIndexOfReplyToAuthorName);
            }
            final Integer _tmpPremiumPriceTokens;
            if (_cursor.isNull(_cursorIndexOfPremiumPriceTokens)) {
              _tmpPremiumPriceTokens = null;
            } else {
              _tmpPremiumPriceTokens = _cursor.getInt(_cursorIndexOfPremiumPriceTokens);
            }
            final boolean _tmpPremiumUnlocked;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfPremiumUnlocked);
            _tmpPremiumUnlocked = _tmp_4 != 0;
            final String _tmpAlbumId;
            if (_cursor.isNull(_cursorIndexOfAlbumId)) {
              _tmpAlbumId = null;
            } else {
              _tmpAlbumId = _cursor.getString(_cursorIndexOfAlbumId);
            }
            final String _tmpFileName;
            if (_cursor.isNull(_cursorIndexOfFileName)) {
              _tmpFileName = null;
            } else {
              _tmpFileName = _cursor.getString(_cursorIndexOfFileName);
            }
            final String _tmpFileMime;
            if (_cursor.isNull(_cursorIndexOfFileMime)) {
              _tmpFileMime = null;
            } else {
              _tmpFileMime = _cursor.getString(_cursorIndexOfFileMime);
            }
            final Long _tmpFileSizeBytes;
            if (_cursor.isNull(_cursorIndexOfFileSizeBytes)) {
              _tmpFileSizeBytes = null;
            } else {
              _tmpFileSizeBytes = _cursor.getLong(_cursorIndexOfFileSizeBytes);
            }
            final Double _tmpLatitude;
            if (_cursor.isNull(_cursorIndexOfLatitude)) {
              _tmpLatitude = null;
            } else {
              _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            }
            final Double _tmpLongitude;
            if (_cursor.isNull(_cursorIndexOfLongitude)) {
              _tmpLongitude = null;
            } else {
              _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            }
            final String _tmpPollId;
            if (_cursor.isNull(_cursorIndexOfPollId)) {
              _tmpPollId = null;
            } else {
              _tmpPollId = _cursor.getString(_cursorIndexOfPollId);
            }
            final String _tmpMentionedUserIds;
            if (_cursor.isNull(_cursorIndexOfMentionedUserIds)) {
              _tmpMentionedUserIds = null;
            } else {
              _tmpMentionedUserIds = _cursor.getString(_cursorIndexOfMentionedUserIds);
            }
            _item = new MessageEntity(_tmpId,_tmpChatId,_tmpSenderId,_tmpContent,_tmpTimestamp,_tmpStatus,_tmpReplyToId,_tmpEditedAt,_tmpCiphertext,_tmpSignalType,_tmpIsEncrypted,_tmpIsFromMe,_tmpKind,_tmpMediaId,_tmpReceivedWhileAway,_tmpDeletedForEveryone,_tmpReactions,_tmpThumbnailB64,_tmpDurationSec,_tmpTtlSeconds,_tmpForwardedFromName,_tmpReplyToContent,_tmpReplyToAuthorName,_tmpPremiumPriceTokens,_tmpPremiumUnlocked,_tmpAlbumId,_tmpFileName,_tmpFileMime,_tmpFileSizeBytes,_tmpLatitude,_tmpLongitude,_tmpPollId,_tmpMentionedUserIds);
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
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfReplyToId = CursorUtil.getColumnIndexOrThrow(_cursor, "replyToId");
          final int _cursorIndexOfEditedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "editedAt");
          final int _cursorIndexOfCiphertext = CursorUtil.getColumnIndexOrThrow(_cursor, "ciphertext");
          final int _cursorIndexOfSignalType = CursorUtil.getColumnIndexOrThrow(_cursor, "signalType");
          final int _cursorIndexOfIsEncrypted = CursorUtil.getColumnIndexOrThrow(_cursor, "isEncrypted");
          final int _cursorIndexOfIsFromMe = CursorUtil.getColumnIndexOrThrow(_cursor, "isFromMe");
          final int _cursorIndexOfKind = CursorUtil.getColumnIndexOrThrow(_cursor, "kind");
          final int _cursorIndexOfMediaId = CursorUtil.getColumnIndexOrThrow(_cursor, "mediaId");
          final int _cursorIndexOfReceivedWhileAway = CursorUtil.getColumnIndexOrThrow(_cursor, "receivedWhileAway");
          final int _cursorIndexOfDeletedForEveryone = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedForEveryone");
          final int _cursorIndexOfReactions = CursorUtil.getColumnIndexOrThrow(_cursor, "reactions");
          final int _cursorIndexOfThumbnailB64 = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbnailB64");
          final int _cursorIndexOfDurationSec = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSec");
          final int _cursorIndexOfTtlSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "ttlSeconds");
          final int _cursorIndexOfForwardedFromName = CursorUtil.getColumnIndexOrThrow(_cursor, "forwardedFromName");
          final int _cursorIndexOfReplyToContent = CursorUtil.getColumnIndexOrThrow(_cursor, "replyToContent");
          final int _cursorIndexOfReplyToAuthorName = CursorUtil.getColumnIndexOrThrow(_cursor, "replyToAuthorName");
          final int _cursorIndexOfPremiumPriceTokens = CursorUtil.getColumnIndexOrThrow(_cursor, "premiumPriceTokens");
          final int _cursorIndexOfPremiumUnlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "premiumUnlocked");
          final int _cursorIndexOfAlbumId = CursorUtil.getColumnIndexOrThrow(_cursor, "albumId");
          final int _cursorIndexOfFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "fileName");
          final int _cursorIndexOfFileMime = CursorUtil.getColumnIndexOrThrow(_cursor, "fileMime");
          final int _cursorIndexOfFileSizeBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "fileSizeBytes");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfPollId = CursorUtil.getColumnIndexOrThrow(_cursor, "pollId");
          final int _cursorIndexOfMentionedUserIds = CursorUtil.getColumnIndexOrThrow(_cursor, "mentionedUserIds");
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
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsEncrypted);
            _tmpIsEncrypted = _tmp != 0;
            final boolean _tmpIsFromMe;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsFromMe);
            _tmpIsFromMe = _tmp_1 != 0;
            final String _tmpKind;
            _tmpKind = _cursor.getString(_cursorIndexOfKind);
            final String _tmpMediaId;
            if (_cursor.isNull(_cursorIndexOfMediaId)) {
              _tmpMediaId = null;
            } else {
              _tmpMediaId = _cursor.getString(_cursorIndexOfMediaId);
            }
            final boolean _tmpReceivedWhileAway;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfReceivedWhileAway);
            _tmpReceivedWhileAway = _tmp_2 != 0;
            final boolean _tmpDeletedForEveryone;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfDeletedForEveryone);
            _tmpDeletedForEveryone = _tmp_3 != 0;
            final String _tmpReactions;
            if (_cursor.isNull(_cursorIndexOfReactions)) {
              _tmpReactions = null;
            } else {
              _tmpReactions = _cursor.getString(_cursorIndexOfReactions);
            }
            final String _tmpThumbnailB64;
            if (_cursor.isNull(_cursorIndexOfThumbnailB64)) {
              _tmpThumbnailB64 = null;
            } else {
              _tmpThumbnailB64 = _cursor.getString(_cursorIndexOfThumbnailB64);
            }
            final double _tmpDurationSec;
            _tmpDurationSec = _cursor.getDouble(_cursorIndexOfDurationSec);
            final Integer _tmpTtlSeconds;
            if (_cursor.isNull(_cursorIndexOfTtlSeconds)) {
              _tmpTtlSeconds = null;
            } else {
              _tmpTtlSeconds = _cursor.getInt(_cursorIndexOfTtlSeconds);
            }
            final String _tmpForwardedFromName;
            if (_cursor.isNull(_cursorIndexOfForwardedFromName)) {
              _tmpForwardedFromName = null;
            } else {
              _tmpForwardedFromName = _cursor.getString(_cursorIndexOfForwardedFromName);
            }
            final String _tmpReplyToContent;
            if (_cursor.isNull(_cursorIndexOfReplyToContent)) {
              _tmpReplyToContent = null;
            } else {
              _tmpReplyToContent = _cursor.getString(_cursorIndexOfReplyToContent);
            }
            final String _tmpReplyToAuthorName;
            if (_cursor.isNull(_cursorIndexOfReplyToAuthorName)) {
              _tmpReplyToAuthorName = null;
            } else {
              _tmpReplyToAuthorName = _cursor.getString(_cursorIndexOfReplyToAuthorName);
            }
            final Integer _tmpPremiumPriceTokens;
            if (_cursor.isNull(_cursorIndexOfPremiumPriceTokens)) {
              _tmpPremiumPriceTokens = null;
            } else {
              _tmpPremiumPriceTokens = _cursor.getInt(_cursorIndexOfPremiumPriceTokens);
            }
            final boolean _tmpPremiumUnlocked;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfPremiumUnlocked);
            _tmpPremiumUnlocked = _tmp_4 != 0;
            final String _tmpAlbumId;
            if (_cursor.isNull(_cursorIndexOfAlbumId)) {
              _tmpAlbumId = null;
            } else {
              _tmpAlbumId = _cursor.getString(_cursorIndexOfAlbumId);
            }
            final String _tmpFileName;
            if (_cursor.isNull(_cursorIndexOfFileName)) {
              _tmpFileName = null;
            } else {
              _tmpFileName = _cursor.getString(_cursorIndexOfFileName);
            }
            final String _tmpFileMime;
            if (_cursor.isNull(_cursorIndexOfFileMime)) {
              _tmpFileMime = null;
            } else {
              _tmpFileMime = _cursor.getString(_cursorIndexOfFileMime);
            }
            final Long _tmpFileSizeBytes;
            if (_cursor.isNull(_cursorIndexOfFileSizeBytes)) {
              _tmpFileSizeBytes = null;
            } else {
              _tmpFileSizeBytes = _cursor.getLong(_cursorIndexOfFileSizeBytes);
            }
            final Double _tmpLatitude;
            if (_cursor.isNull(_cursorIndexOfLatitude)) {
              _tmpLatitude = null;
            } else {
              _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            }
            final Double _tmpLongitude;
            if (_cursor.isNull(_cursorIndexOfLongitude)) {
              _tmpLongitude = null;
            } else {
              _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            }
            final String _tmpPollId;
            if (_cursor.isNull(_cursorIndexOfPollId)) {
              _tmpPollId = null;
            } else {
              _tmpPollId = _cursor.getString(_cursorIndexOfPollId);
            }
            final String _tmpMentionedUserIds;
            if (_cursor.isNull(_cursorIndexOfMentionedUserIds)) {
              _tmpMentionedUserIds = null;
            } else {
              _tmpMentionedUserIds = _cursor.getString(_cursorIndexOfMentionedUserIds);
            }
            _result = new MessageEntity(_tmpId,_tmpChatId,_tmpSenderId,_tmpContent,_tmpTimestamp,_tmpStatus,_tmpReplyToId,_tmpEditedAt,_tmpCiphertext,_tmpSignalType,_tmpIsEncrypted,_tmpIsFromMe,_tmpKind,_tmpMediaId,_tmpReceivedWhileAway,_tmpDeletedForEveryone,_tmpReactions,_tmpThumbnailB64,_tmpDurationSec,_tmpTtlSeconds,_tmpForwardedFromName,_tmpReplyToContent,_tmpReplyToAuthorName,_tmpPremiumPriceTokens,_tmpPremiumUnlocked,_tmpAlbumId,_tmpFileName,_tmpFileMime,_tmpFileSizeBytes,_tmpLatitude,_tmpLongitude,_tmpPollId,_tmpMentionedUserIds);
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
