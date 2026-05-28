package com.rcq.messenger.data.db;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class RCQDatabase_Impl extends RCQDatabase {
  private volatile UserDao _userDao;

  private volatile ContactDao _contactDao;

  private volatile ChatDao _chatDao;

  private volatile MessageDao _messageDao;

  private volatile GroupDao _groupDao;

  private volatile StoryDao _storyDao;

  private volatile CallDao _callDao;

  private volatile PetDao _petDao;

  private volatile SignalKeyDao _signalKeyDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(10) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `users` (`id` INTEGER NOT NULL, `username` TEXT NOT NULL, `displayName` TEXT NOT NULL, `avatarUrl` TEXT, `isOnline` INTEGER NOT NULL, `lastSeen` INTEGER NOT NULL, `publicKey` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `contacts` (`userId` INTEGER NOT NULL, `nickname` TEXT NOT NULL, `avatarUrl` TEXT, `status` TEXT NOT NULL, `lastSeen` TEXT, `isBlocked` INTEGER NOT NULL, `isFavorite` INTEGER NOT NULL, `notificationSound` TEXT, `customNickname` TEXT, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`userId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `chats` (`id` TEXT NOT NULL, `targetId` INTEGER NOT NULL, `targetNickname` TEXT NOT NULL, `targetAvatar` TEXT, `unreadCount` INTEGER NOT NULL, `isPinned` INTEGER NOT NULL, `isMuted` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `lastMessageContent` TEXT, `lastMessageTimestamp` INTEGER, `lastMessageKind` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `messages` (`id` TEXT NOT NULL, `chatId` TEXT NOT NULL, `senderId` INTEGER NOT NULL, `content` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `status` TEXT NOT NULL, `replyToId` TEXT, `editedAt` INTEGER, `ciphertext` TEXT, `signalType` INTEGER NOT NULL, `isEncrypted` INTEGER NOT NULL, `isFromMe` INTEGER NOT NULL, `kind` TEXT NOT NULL, `mediaId` TEXT, `receivedWhileAway` INTEGER NOT NULL, `deletedForEveryone` INTEGER NOT NULL, `reactions` TEXT, `thumbnailB64` TEXT, `durationSec` REAL NOT NULL, `ttlSeconds` INTEGER, `forwardedFromName` TEXT, `replyToContent` TEXT, `replyToAuthorName` TEXT, `premiumPriceTokens` INTEGER, `premiumUnlocked` INTEGER NOT NULL, `albumId` TEXT, `fileName` TEXT, `fileMime` TEXT, `fileSizeBytes` INTEGER, `latitude` REAL, `longitude` REAL, `pollId` TEXT, `mentionedUserIds` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `groups` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, `avatarUrl` TEXT, `creatorId` INTEGER NOT NULL, `memberIds` TEXT NOT NULL, `adminIds` TEXT NOT NULL, `isPublic` INTEGER NOT NULL, `inviteLink` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `stories` (`id` TEXT NOT NULL, `userId` INTEGER NOT NULL, `nickname` TEXT, `avatarUrl` TEXT, `type` TEXT NOT NULL, `content` TEXT, `mediaUrl` TEXT, `duration` INTEGER NOT NULL, `viewerIds` TEXT NOT NULL, `viewerCount` INTEGER NOT NULL, `isHighlighted` INTEGER NOT NULL, `isActive` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `expiresAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `story_items` (`id` TEXT NOT NULL, `storyId` TEXT NOT NULL, `type` TEXT NOT NULL, `content` TEXT, `mediaUrl` TEXT, `thumbnailUrl` TEXT, `caption` TEXT, `backgroundColor` TEXT, `duration` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `calls` (`id` TEXT NOT NULL, `type` TEXT NOT NULL, `status` TEXT NOT NULL, `participantIds` TEXT NOT NULL, `initiatorId` INTEGER NOT NULL, `startTime` INTEGER NOT NULL, `endTime` INTEGER, `duration` INTEGER NOT NULL, `isGroupCall` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `pets` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `rarity` TEXT NOT NULL, `imageUrl` TEXT NOT NULL, `equippedBy` INTEGER, `isForSale` INTEGER NOT NULL, `salePrice` INTEGER, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `signal_keys` (`id` TEXT NOT NULL, `keyType` TEXT NOT NULL, `address` TEXT, `keyId` INTEGER, `keyData` BLOB NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '31bf9ae2f6a713a166b37fc290728231')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `users`");
        db.execSQL("DROP TABLE IF EXISTS `contacts`");
        db.execSQL("DROP TABLE IF EXISTS `chats`");
        db.execSQL("DROP TABLE IF EXISTS `messages`");
        db.execSQL("DROP TABLE IF EXISTS `groups`");
        db.execSQL("DROP TABLE IF EXISTS `stories`");
        db.execSQL("DROP TABLE IF EXISTS `story_items`");
        db.execSQL("DROP TABLE IF EXISTS `calls`");
        db.execSQL("DROP TABLE IF EXISTS `pets`");
        db.execSQL("DROP TABLE IF EXISTS `signal_keys`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsUsers = new HashMap<String, TableInfo.Column>(9);
        _columnsUsers.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("username", new TableInfo.Column("username", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("displayName", new TableInfo.Column("displayName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("avatarUrl", new TableInfo.Column("avatarUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("isOnline", new TableInfo.Column("isOnline", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("lastSeen", new TableInfo.Column("lastSeen", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("publicKey", new TableInfo.Column("publicKey", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUsers = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesUsers = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoUsers = new TableInfo("users", _columnsUsers, _foreignKeysUsers, _indicesUsers);
        final TableInfo _existingUsers = TableInfo.read(db, "users");
        if (!_infoUsers.equals(_existingUsers)) {
          return new RoomOpenHelper.ValidationResult(false, "users(com.rcq.messenger.domain.model.UserEntity).\n"
                  + " Expected:\n" + _infoUsers + "\n"
                  + " Found:\n" + _existingUsers);
        }
        final HashMap<String, TableInfo.Column> _columnsContacts = new HashMap<String, TableInfo.Column>(10);
        _columnsContacts.put("userId", new TableInfo.Column("userId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("nickname", new TableInfo.Column("nickname", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("avatarUrl", new TableInfo.Column("avatarUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("lastSeen", new TableInfo.Column("lastSeen", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("isBlocked", new TableInfo.Column("isBlocked", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("isFavorite", new TableInfo.Column("isFavorite", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("notificationSound", new TableInfo.Column("notificationSound", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("customNickname", new TableInfo.Column("customNickname", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("addedAt", new TableInfo.Column("addedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysContacts = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesContacts = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoContacts = new TableInfo("contacts", _columnsContacts, _foreignKeysContacts, _indicesContacts);
        final TableInfo _existingContacts = TableInfo.read(db, "contacts");
        if (!_infoContacts.equals(_existingContacts)) {
          return new RoomOpenHelper.ValidationResult(false, "contacts(com.rcq.messenger.domain.model.ContactEntity).\n"
                  + " Expected:\n" + _infoContacts + "\n"
                  + " Found:\n" + _existingContacts);
        }
        final HashMap<String, TableInfo.Column> _columnsChats = new HashMap<String, TableInfo.Column>(13);
        _columnsChats.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChats.put("targetId", new TableInfo.Column("targetId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChats.put("targetNickname", new TableInfo.Column("targetNickname", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChats.put("targetAvatar", new TableInfo.Column("targetAvatar", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChats.put("unreadCount", new TableInfo.Column("unreadCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChats.put("isPinned", new TableInfo.Column("isPinned", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChats.put("isMuted", new TableInfo.Column("isMuted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChats.put("isArchived", new TableInfo.Column("isArchived", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChats.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChats.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChats.put("lastMessageContent", new TableInfo.Column("lastMessageContent", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChats.put("lastMessageTimestamp", new TableInfo.Column("lastMessageTimestamp", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChats.put("lastMessageKind", new TableInfo.Column("lastMessageKind", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysChats = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesChats = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoChats = new TableInfo("chats", _columnsChats, _foreignKeysChats, _indicesChats);
        final TableInfo _existingChats = TableInfo.read(db, "chats");
        if (!_infoChats.equals(_existingChats)) {
          return new RoomOpenHelper.ValidationResult(false, "chats(com.rcq.messenger.domain.model.ChatEntity).\n"
                  + " Expected:\n" + _infoChats + "\n"
                  + " Found:\n" + _existingChats);
        }
        final HashMap<String, TableInfo.Column> _columnsMessages = new HashMap<String, TableInfo.Column>(33);
        _columnsMessages.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("chatId", new TableInfo.Column("chatId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("senderId", new TableInfo.Column("senderId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("content", new TableInfo.Column("content", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("replyToId", new TableInfo.Column("replyToId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("editedAt", new TableInfo.Column("editedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("ciphertext", new TableInfo.Column("ciphertext", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("signalType", new TableInfo.Column("signalType", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("isEncrypted", new TableInfo.Column("isEncrypted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("isFromMe", new TableInfo.Column("isFromMe", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("kind", new TableInfo.Column("kind", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("mediaId", new TableInfo.Column("mediaId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("receivedWhileAway", new TableInfo.Column("receivedWhileAway", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("deletedForEveryone", new TableInfo.Column("deletedForEveryone", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("reactions", new TableInfo.Column("reactions", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("thumbnailB64", new TableInfo.Column("thumbnailB64", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("durationSec", new TableInfo.Column("durationSec", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("ttlSeconds", new TableInfo.Column("ttlSeconds", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("forwardedFromName", new TableInfo.Column("forwardedFromName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("replyToContent", new TableInfo.Column("replyToContent", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("replyToAuthorName", new TableInfo.Column("replyToAuthorName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("premiumPriceTokens", new TableInfo.Column("premiumPriceTokens", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("premiumUnlocked", new TableInfo.Column("premiumUnlocked", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("albumId", new TableInfo.Column("albumId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("fileName", new TableInfo.Column("fileName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("fileMime", new TableInfo.Column("fileMime", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("fileSizeBytes", new TableInfo.Column("fileSizeBytes", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("latitude", new TableInfo.Column("latitude", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("longitude", new TableInfo.Column("longitude", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("pollId", new TableInfo.Column("pollId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("mentionedUserIds", new TableInfo.Column("mentionedUserIds", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMessages = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMessages = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMessages = new TableInfo("messages", _columnsMessages, _foreignKeysMessages, _indicesMessages);
        final TableInfo _existingMessages = TableInfo.read(db, "messages");
        if (!_infoMessages.equals(_existingMessages)) {
          return new RoomOpenHelper.ValidationResult(false, "messages(com.rcq.messenger.domain.model.MessageEntity).\n"
                  + " Expected:\n" + _infoMessages + "\n"
                  + " Found:\n" + _existingMessages);
        }
        final HashMap<String, TableInfo.Column> _columnsGroups = new HashMap<String, TableInfo.Column>(11);
        _columnsGroups.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroups.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroups.put("description", new TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroups.put("avatarUrl", new TableInfo.Column("avatarUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroups.put("creatorId", new TableInfo.Column("creatorId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroups.put("memberIds", new TableInfo.Column("memberIds", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroups.put("adminIds", new TableInfo.Column("adminIds", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroups.put("isPublic", new TableInfo.Column("isPublic", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroups.put("inviteLink", new TableInfo.Column("inviteLink", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroups.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroups.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysGroups = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesGroups = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoGroups = new TableInfo("groups", _columnsGroups, _foreignKeysGroups, _indicesGroups);
        final TableInfo _existingGroups = TableInfo.read(db, "groups");
        if (!_infoGroups.equals(_existingGroups)) {
          return new RoomOpenHelper.ValidationResult(false, "groups(com.rcq.messenger.domain.model.GroupEntity).\n"
                  + " Expected:\n" + _infoGroups + "\n"
                  + " Found:\n" + _existingGroups);
        }
        final HashMap<String, TableInfo.Column> _columnsStories = new HashMap<String, TableInfo.Column>(15);
        _columnsStories.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStories.put("userId", new TableInfo.Column("userId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStories.put("nickname", new TableInfo.Column("nickname", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStories.put("avatarUrl", new TableInfo.Column("avatarUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStories.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStories.put("content", new TableInfo.Column("content", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStories.put("mediaUrl", new TableInfo.Column("mediaUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStories.put("duration", new TableInfo.Column("duration", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStories.put("viewerIds", new TableInfo.Column("viewerIds", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStories.put("viewerCount", new TableInfo.Column("viewerCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStories.put("isHighlighted", new TableInfo.Column("isHighlighted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStories.put("isActive", new TableInfo.Column("isActive", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStories.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStories.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStories.put("expiresAt", new TableInfo.Column("expiresAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysStories = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesStories = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoStories = new TableInfo("stories", _columnsStories, _foreignKeysStories, _indicesStories);
        final TableInfo _existingStories = TableInfo.read(db, "stories");
        if (!_infoStories.equals(_existingStories)) {
          return new RoomOpenHelper.ValidationResult(false, "stories(com.rcq.messenger.domain.model.StoryEntity).\n"
                  + " Expected:\n" + _infoStories + "\n"
                  + " Found:\n" + _existingStories);
        }
        final HashMap<String, TableInfo.Column> _columnsStoryItems = new HashMap<String, TableInfo.Column>(10);
        _columnsStoryItems.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStoryItems.put("storyId", new TableInfo.Column("storyId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStoryItems.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStoryItems.put("content", new TableInfo.Column("content", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStoryItems.put("mediaUrl", new TableInfo.Column("mediaUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStoryItems.put("thumbnailUrl", new TableInfo.Column("thumbnailUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStoryItems.put("caption", new TableInfo.Column("caption", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStoryItems.put("backgroundColor", new TableInfo.Column("backgroundColor", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStoryItems.put("duration", new TableInfo.Column("duration", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStoryItems.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysStoryItems = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesStoryItems = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoStoryItems = new TableInfo("story_items", _columnsStoryItems, _foreignKeysStoryItems, _indicesStoryItems);
        final TableInfo _existingStoryItems = TableInfo.read(db, "story_items");
        if (!_infoStoryItems.equals(_existingStoryItems)) {
          return new RoomOpenHelper.ValidationResult(false, "story_items(com.rcq.messenger.domain.model.StoryItemEntity).\n"
                  + " Expected:\n" + _infoStoryItems + "\n"
                  + " Found:\n" + _existingStoryItems);
        }
        final HashMap<String, TableInfo.Column> _columnsCalls = new HashMap<String, TableInfo.Column>(9);
        _columnsCalls.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalls.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalls.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalls.put("participantIds", new TableInfo.Column("participantIds", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalls.put("initiatorId", new TableInfo.Column("initiatorId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalls.put("startTime", new TableInfo.Column("startTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalls.put("endTime", new TableInfo.Column("endTime", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalls.put("duration", new TableInfo.Column("duration", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalls.put("isGroupCall", new TableInfo.Column("isGroupCall", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCalls = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCalls = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCalls = new TableInfo("calls", _columnsCalls, _foreignKeysCalls, _indicesCalls);
        final TableInfo _existingCalls = TableInfo.read(db, "calls");
        if (!_infoCalls.equals(_existingCalls)) {
          return new RoomOpenHelper.ValidationResult(false, "calls(com.rcq.messenger.domain.model.CallEntity).\n"
                  + " Expected:\n" + _infoCalls + "\n"
                  + " Found:\n" + _existingCalls);
        }
        final HashMap<String, TableInfo.Column> _columnsPets = new HashMap<String, TableInfo.Column>(8);
        _columnsPets.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPets.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPets.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPets.put("rarity", new TableInfo.Column("rarity", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPets.put("imageUrl", new TableInfo.Column("imageUrl", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPets.put("equippedBy", new TableInfo.Column("equippedBy", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPets.put("isForSale", new TableInfo.Column("isForSale", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPets.put("salePrice", new TableInfo.Column("salePrice", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPets = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPets = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPets = new TableInfo("pets", _columnsPets, _foreignKeysPets, _indicesPets);
        final TableInfo _existingPets = TableInfo.read(db, "pets");
        if (!_infoPets.equals(_existingPets)) {
          return new RoomOpenHelper.ValidationResult(false, "pets(com.rcq.messenger.domain.model.PetEntity).\n"
                  + " Expected:\n" + _infoPets + "\n"
                  + " Found:\n" + _existingPets);
        }
        final HashMap<String, TableInfo.Column> _columnsSignalKeys = new HashMap<String, TableInfo.Column>(6);
        _columnsSignalKeys.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSignalKeys.put("keyType", new TableInfo.Column("keyType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSignalKeys.put("address", new TableInfo.Column("address", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSignalKeys.put("keyId", new TableInfo.Column("keyId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSignalKeys.put("keyData", new TableInfo.Column("keyData", "BLOB", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSignalKeys.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSignalKeys = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSignalKeys = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSignalKeys = new TableInfo("signal_keys", _columnsSignalKeys, _foreignKeysSignalKeys, _indicesSignalKeys);
        final TableInfo _existingSignalKeys = TableInfo.read(db, "signal_keys");
        if (!_infoSignalKeys.equals(_existingSignalKeys)) {
          return new RoomOpenHelper.ValidationResult(false, "signal_keys(com.rcq.messenger.domain.model.SignalKeyEntity).\n"
                  + " Expected:\n" + _infoSignalKeys + "\n"
                  + " Found:\n" + _existingSignalKeys);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "31bf9ae2f6a713a166b37fc290728231", "2b440118cde4418a9287671a631efbdb");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "users","contacts","chats","messages","groups","stories","story_items","calls","pets","signal_keys");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `users`");
      _db.execSQL("DELETE FROM `contacts`");
      _db.execSQL("DELETE FROM `chats`");
      _db.execSQL("DELETE FROM `messages`");
      _db.execSQL("DELETE FROM `groups`");
      _db.execSQL("DELETE FROM `stories`");
      _db.execSQL("DELETE FROM `story_items`");
      _db.execSQL("DELETE FROM `calls`");
      _db.execSQL("DELETE FROM `pets`");
      _db.execSQL("DELETE FROM `signal_keys`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(UserDao.class, UserDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ContactDao.class, ContactDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ChatDao.class, ChatDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(MessageDao.class, MessageDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(GroupDao.class, GroupDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(StoryDao.class, StoryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CallDao.class, CallDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(PetDao.class, PetDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SignalKeyDao.class, SignalKeyDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public UserDao userDao() {
    if (_userDao != null) {
      return _userDao;
    } else {
      synchronized(this) {
        if(_userDao == null) {
          _userDao = new UserDao_Impl(this);
        }
        return _userDao;
      }
    }
  }

  @Override
  public ContactDao contactDao() {
    if (_contactDao != null) {
      return _contactDao;
    } else {
      synchronized(this) {
        if(_contactDao == null) {
          _contactDao = new ContactDao_Impl(this);
        }
        return _contactDao;
      }
    }
  }

  @Override
  public ChatDao chatDao() {
    if (_chatDao != null) {
      return _chatDao;
    } else {
      synchronized(this) {
        if(_chatDao == null) {
          _chatDao = new ChatDao_Impl(this);
        }
        return _chatDao;
      }
    }
  }

  @Override
  public MessageDao messageDao() {
    if (_messageDao != null) {
      return _messageDao;
    } else {
      synchronized(this) {
        if(_messageDao == null) {
          _messageDao = new MessageDao_Impl(this);
        }
        return _messageDao;
      }
    }
  }

  @Override
  public GroupDao groupDao() {
    if (_groupDao != null) {
      return _groupDao;
    } else {
      synchronized(this) {
        if(_groupDao == null) {
          _groupDao = new GroupDao_Impl(this);
        }
        return _groupDao;
      }
    }
  }

  @Override
  public StoryDao storyDao() {
    if (_storyDao != null) {
      return _storyDao;
    } else {
      synchronized(this) {
        if(_storyDao == null) {
          _storyDao = new StoryDao_Impl(this);
        }
        return _storyDao;
      }
    }
  }

  @Override
  public CallDao callDao() {
    if (_callDao != null) {
      return _callDao;
    } else {
      synchronized(this) {
        if(_callDao == null) {
          _callDao = new CallDao_Impl(this);
        }
        return _callDao;
      }
    }
  }

  @Override
  public PetDao petDao() {
    if (_petDao != null) {
      return _petDao;
    } else {
      synchronized(this) {
        if(_petDao == null) {
          _petDao = new PetDao_Impl(this);
        }
        return _petDao;
      }
    }
  }

  @Override
  public SignalKeyDao signalKeyDao() {
    if (_signalKeyDao != null) {
      return _signalKeyDao;
    } else {
      synchronized(this) {
        if(_signalKeyDao == null) {
          _signalKeyDao = new SignalKeyDao_Impl(this);
        }
        return _signalKeyDao;
      }
    }
  }
}
