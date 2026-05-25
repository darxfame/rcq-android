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
import java.util.Arrays;
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

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(6) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `users` (`id` INTEGER NOT NULL, `nickname` TEXT NOT NULL, `avatarUrl` TEXT, `status` TEXT NOT NULL, `lastSeen` TEXT, `bio` TEXT NOT NULL, `isBlocked` INTEGER NOT NULL, `isFavorite` INTEGER NOT NULL, `notificationSound` TEXT, `customNickname` TEXT, `tokens` INTEGER NOT NULL, `isPremium` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `contacts` (`userId` INTEGER NOT NULL, `nickname` TEXT NOT NULL, `avatarUrl` TEXT, `status` TEXT NOT NULL, `lastSeen` TEXT, `isBlocked` INTEGER NOT NULL, `isFavorite` INTEGER NOT NULL, `notificationSound` TEXT, `customNickname` TEXT, `lastMessagePreview` TEXT, `lastMessageTime` INTEGER NOT NULL, PRIMARY KEY(`userId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `chats` (`id` TEXT NOT NULL, `targetId` INTEGER NOT NULL, `targetNickname` TEXT NOT NULL, `targetAvatar` TEXT, `unreadCount` INTEGER NOT NULL, `isPinned` INTEGER NOT NULL, `isMuted` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `messages` (`id` TEXT NOT NULL, `chatId` TEXT NOT NULL, `senderId` INTEGER NOT NULL, `isFromMe` INTEGER NOT NULL, `kind` TEXT NOT NULL, `content` TEXT NOT NULL, `mediaId` TEXT, `timestamp` INTEGER NOT NULL, `status` TEXT NOT NULL, `receivedWhileAway` INTEGER NOT NULL, `deletedForEveryone` INTEGER NOT NULL, `reactions` TEXT, `thumbnailB64` TEXT, `durationSec` REAL NOT NULL, `ttlSeconds` INTEGER, `forwardedFromName` TEXT, `replyToId` TEXT, `replyToContent` TEXT, `replyToAuthorName` TEXT, `editedAt` INTEGER, `premiumPriceTokens` INTEGER, `premiumUnlocked` INTEGER NOT NULL, `albumId` TEXT, `fileName` TEXT, `fileMime` TEXT, `fileSizeBytes` INTEGER, `latitude` REAL, `longitude` REAL, `pollId` TEXT, `isForwarded` INTEGER NOT NULL, `mentionedUserIds` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_chatId` ON `messages` (`chatId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_timestamp` ON `messages` (`timestamp`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `groups` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `avatarUrl` TEXT, `description` TEXT NOT NULL, `ownerId` INTEGER NOT NULL, `memberCount` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `isPinned` INTEGER NOT NULL, `isMuted` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `stories` (`id` TEXT NOT NULL, `userId` INTEGER NOT NULL, `nickname` TEXT NOT NULL, `avatarUrl` TEXT, `viewerCount` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `expiresAt` INTEGER NOT NULL, `isActive` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `story_items` (`id` TEXT NOT NULL, `storyId` TEXT NOT NULL, `type` TEXT NOT NULL, `mediaUrl` TEXT NOT NULL, `thumbnailUrl` TEXT, `caption` TEXT, `backgroundColor` TEXT, `duration` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`storyId`) REFERENCES `stories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_story_items_storyId` ON `story_items` (`storyId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `calls` (`id` TEXT NOT NULL, `type` TEXT NOT NULL, `targetId` INTEGER NOT NULL, `targetNickname` TEXT NOT NULL, `targetAvatar` TEXT, `initiatorId` INTEGER NOT NULL, `status` TEXT NOT NULL, `startedAt` INTEGER, `endedAt` INTEGER, `duration` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `pets` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `rarity` TEXT NOT NULL, `imageUrl` TEXT NOT NULL, `equippedBy` INTEGER, `isForSale` INTEGER NOT NULL, `salePrice` INTEGER, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'a2afddbb2076b578a762a756c1db3c3e')");
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
        db.execSQL("PRAGMA foreign_keys = ON");
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
        final HashMap<String, TableInfo.Column> _columnsUsers = new HashMap<String, TableInfo.Column>(12);
        _columnsUsers.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("nickname", new TableInfo.Column("nickname", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("avatarUrl", new TableInfo.Column("avatarUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("lastSeen", new TableInfo.Column("lastSeen", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("bio", new TableInfo.Column("bio", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("isBlocked", new TableInfo.Column("isBlocked", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("isFavorite", new TableInfo.Column("isFavorite", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("notificationSound", new TableInfo.Column("notificationSound", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("customNickname", new TableInfo.Column("customNickname", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("tokens", new TableInfo.Column("tokens", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("isPremium", new TableInfo.Column("isPremium", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUsers = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesUsers = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoUsers = new TableInfo("users", _columnsUsers, _foreignKeysUsers, _indicesUsers);
        final TableInfo _existingUsers = TableInfo.read(db, "users");
        if (!_infoUsers.equals(_existingUsers)) {
          return new RoomOpenHelper.ValidationResult(false, "users(com.rcq.messenger.data.db.UserEntity).\n"
                  + " Expected:\n" + _infoUsers + "\n"
                  + " Found:\n" + _existingUsers);
        }
        final HashMap<String, TableInfo.Column> _columnsContacts = new HashMap<String, TableInfo.Column>(11);
        _columnsContacts.put("userId", new TableInfo.Column("userId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("nickname", new TableInfo.Column("nickname", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("avatarUrl", new TableInfo.Column("avatarUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("lastSeen", new TableInfo.Column("lastSeen", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("isBlocked", new TableInfo.Column("isBlocked", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("isFavorite", new TableInfo.Column("isFavorite", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("notificationSound", new TableInfo.Column("notificationSound", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("customNickname", new TableInfo.Column("customNickname", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("lastMessagePreview", new TableInfo.Column("lastMessagePreview", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("lastMessageTime", new TableInfo.Column("lastMessageTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysContacts = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesContacts = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoContacts = new TableInfo("contacts", _columnsContacts, _foreignKeysContacts, _indicesContacts);
        final TableInfo _existingContacts = TableInfo.read(db, "contacts");
        if (!_infoContacts.equals(_existingContacts)) {
          return new RoomOpenHelper.ValidationResult(false, "contacts(com.rcq.messenger.data.db.ContactEntity).\n"
                  + " Expected:\n" + _infoContacts + "\n"
                  + " Found:\n" + _existingContacts);
        }
        final HashMap<String, TableInfo.Column> _columnsChats = new HashMap<String, TableInfo.Column>(10);
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
        final HashSet<TableInfo.ForeignKey> _foreignKeysChats = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesChats = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoChats = new TableInfo("chats", _columnsChats, _foreignKeysChats, _indicesChats);
        final TableInfo _existingChats = TableInfo.read(db, "chats");
        if (!_infoChats.equals(_existingChats)) {
          return new RoomOpenHelper.ValidationResult(false, "chats(com.rcq.messenger.data.db.ChatEntity).\n"
                  + " Expected:\n" + _infoChats + "\n"
                  + " Found:\n" + _existingChats);
        }
        final HashMap<String, TableInfo.Column> _columnsMessages = new HashMap<String, TableInfo.Column>(31);
        _columnsMessages.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("chatId", new TableInfo.Column("chatId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("senderId", new TableInfo.Column("senderId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("isFromMe", new TableInfo.Column("isFromMe", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("kind", new TableInfo.Column("kind", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("content", new TableInfo.Column("content", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("mediaId", new TableInfo.Column("mediaId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("receivedWhileAway", new TableInfo.Column("receivedWhileAway", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("deletedForEveryone", new TableInfo.Column("deletedForEveryone", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("reactions", new TableInfo.Column("reactions", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("thumbnailB64", new TableInfo.Column("thumbnailB64", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("durationSec", new TableInfo.Column("durationSec", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("ttlSeconds", new TableInfo.Column("ttlSeconds", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("forwardedFromName", new TableInfo.Column("forwardedFromName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("replyToId", new TableInfo.Column("replyToId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("replyToContent", new TableInfo.Column("replyToContent", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("replyToAuthorName", new TableInfo.Column("replyToAuthorName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("editedAt", new TableInfo.Column("editedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("premiumPriceTokens", new TableInfo.Column("premiumPriceTokens", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("premiumUnlocked", new TableInfo.Column("premiumUnlocked", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("albumId", new TableInfo.Column("albumId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("fileName", new TableInfo.Column("fileName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("fileMime", new TableInfo.Column("fileMime", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("fileSizeBytes", new TableInfo.Column("fileSizeBytes", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("latitude", new TableInfo.Column("latitude", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("longitude", new TableInfo.Column("longitude", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("pollId", new TableInfo.Column("pollId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("isForwarded", new TableInfo.Column("isForwarded", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("mentionedUserIds", new TableInfo.Column("mentionedUserIds", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMessages = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMessages = new HashSet<TableInfo.Index>(2);
        _indicesMessages.add(new TableInfo.Index("index_messages_chatId", false, Arrays.asList("chatId"), Arrays.asList("ASC")));
        _indicesMessages.add(new TableInfo.Index("index_messages_timestamp", false, Arrays.asList("timestamp"), Arrays.asList("ASC")));
        final TableInfo _infoMessages = new TableInfo("messages", _columnsMessages, _foreignKeysMessages, _indicesMessages);
        final TableInfo _existingMessages = TableInfo.read(db, "messages");
        if (!_infoMessages.equals(_existingMessages)) {
          return new RoomOpenHelper.ValidationResult(false, "messages(com.rcq.messenger.data.db.MessageEntity).\n"
                  + " Expected:\n" + _infoMessages + "\n"
                  + " Found:\n" + _existingMessages);
        }
        final HashMap<String, TableInfo.Column> _columnsGroups = new HashMap<String, TableInfo.Column>(9);
        _columnsGroups.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroups.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroups.put("avatarUrl", new TableInfo.Column("avatarUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroups.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroups.put("ownerId", new TableInfo.Column("ownerId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroups.put("memberCount", new TableInfo.Column("memberCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroups.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroups.put("isPinned", new TableInfo.Column("isPinned", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroups.put("isMuted", new TableInfo.Column("isMuted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysGroups = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesGroups = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoGroups = new TableInfo("groups", _columnsGroups, _foreignKeysGroups, _indicesGroups);
        final TableInfo _existingGroups = TableInfo.read(db, "groups");
        if (!_infoGroups.equals(_existingGroups)) {
          return new RoomOpenHelper.ValidationResult(false, "groups(com.rcq.messenger.data.db.GroupEntity).\n"
                  + " Expected:\n" + _infoGroups + "\n"
                  + " Found:\n" + _existingGroups);
        }
        final HashMap<String, TableInfo.Column> _columnsStories = new HashMap<String, TableInfo.Column>(8);
        _columnsStories.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStories.put("userId", new TableInfo.Column("userId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStories.put("nickname", new TableInfo.Column("nickname", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStories.put("avatarUrl", new TableInfo.Column("avatarUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStories.put("viewerCount", new TableInfo.Column("viewerCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStories.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStories.put("expiresAt", new TableInfo.Column("expiresAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStories.put("isActive", new TableInfo.Column("isActive", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysStories = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesStories = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoStories = new TableInfo("stories", _columnsStories, _foreignKeysStories, _indicesStories);
        final TableInfo _existingStories = TableInfo.read(db, "stories");
        if (!_infoStories.equals(_existingStories)) {
          return new RoomOpenHelper.ValidationResult(false, "stories(com.rcq.messenger.data.db.StoryEntity).\n"
                  + " Expected:\n" + _infoStories + "\n"
                  + " Found:\n" + _existingStories);
        }
        final HashMap<String, TableInfo.Column> _columnsStoryItems = new HashMap<String, TableInfo.Column>(9);
        _columnsStoryItems.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStoryItems.put("storyId", new TableInfo.Column("storyId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStoryItems.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStoryItems.put("mediaUrl", new TableInfo.Column("mediaUrl", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStoryItems.put("thumbnailUrl", new TableInfo.Column("thumbnailUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStoryItems.put("caption", new TableInfo.Column("caption", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStoryItems.put("backgroundColor", new TableInfo.Column("backgroundColor", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStoryItems.put("duration", new TableInfo.Column("duration", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStoryItems.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysStoryItems = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysStoryItems.add(new TableInfo.ForeignKey("stories", "CASCADE", "NO ACTION", Arrays.asList("storyId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesStoryItems = new HashSet<TableInfo.Index>(1);
        _indicesStoryItems.add(new TableInfo.Index("index_story_items_storyId", false, Arrays.asList("storyId"), Arrays.asList("ASC")));
        final TableInfo _infoStoryItems = new TableInfo("story_items", _columnsStoryItems, _foreignKeysStoryItems, _indicesStoryItems);
        final TableInfo _existingStoryItems = TableInfo.read(db, "story_items");
        if (!_infoStoryItems.equals(_existingStoryItems)) {
          return new RoomOpenHelper.ValidationResult(false, "story_items(com.rcq.messenger.data.db.StoryItemEntity).\n"
                  + " Expected:\n" + _infoStoryItems + "\n"
                  + " Found:\n" + _existingStoryItems);
        }
        final HashMap<String, TableInfo.Column> _columnsCalls = new HashMap<String, TableInfo.Column>(10);
        _columnsCalls.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalls.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalls.put("targetId", new TableInfo.Column("targetId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalls.put("targetNickname", new TableInfo.Column("targetNickname", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalls.put("targetAvatar", new TableInfo.Column("targetAvatar", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalls.put("initiatorId", new TableInfo.Column("initiatorId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalls.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalls.put("startedAt", new TableInfo.Column("startedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalls.put("endedAt", new TableInfo.Column("endedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalls.put("duration", new TableInfo.Column("duration", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCalls = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCalls = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCalls = new TableInfo("calls", _columnsCalls, _foreignKeysCalls, _indicesCalls);
        final TableInfo _existingCalls = TableInfo.read(db, "calls");
        if (!_infoCalls.equals(_existingCalls)) {
          return new RoomOpenHelper.ValidationResult(false, "calls(com.rcq.messenger.data.db.CallEntity).\n"
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
          return new RoomOpenHelper.ValidationResult(false, "pets(com.rcq.messenger.data.db.PetEntity).\n"
                  + " Expected:\n" + _infoPets + "\n"
                  + " Found:\n" + _existingPets);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "a2afddbb2076b578a762a756c1db3c3e", "bca602601605d04b60d7e1d1b605ed9f");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "users","contacts","chats","messages","groups","stories","story_items","calls","pets");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `users`");
      _db.execSQL("DELETE FROM `contacts`");
      _db.execSQL("DELETE FROM `chats`");
      _db.execSQL("DELETE FROM `messages`");
      _db.execSQL("DELETE FROM `groups`");
      _db.execSQL("DELETE FROM `stories`");
      _db.execSQL("DELETE FROM `story_items`");
      _db.execSQL("DELETE FROM `calls`");
      _db.execSQL("DELETE FROM `pets`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
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
}
