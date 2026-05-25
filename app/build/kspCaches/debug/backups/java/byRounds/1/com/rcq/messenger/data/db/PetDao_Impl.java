package com.rcq.messenger.data.db;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
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
public final class PetDao_Impl implements PetDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<PetEntity> __insertionAdapterOfPetEntity;

  private final SharedSQLiteStatement __preparedStmtOfEquipPet;

  private final SharedSQLiteStatement __preparedStmtOfUnequipPet;

  public PetDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPetEntity = new EntityInsertionAdapter<PetEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `pets` (`id`,`name`,`type`,`rarity`,`imageUrl`,`equippedBy`,`isForSale`,`salePrice`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PetEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getType());
        statement.bindString(4, entity.getRarity());
        statement.bindString(5, entity.getImageUrl());
        if (entity.getEquippedBy() == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, entity.getEquippedBy());
        }
        final int _tmp = entity.isForSale() ? 1 : 0;
        statement.bindLong(7, _tmp);
        if (entity.getSalePrice() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getSalePrice());
        }
      }
    };
    this.__preparedStmtOfEquipPet = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE pets SET equippedBy = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUnequipPet = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE pets SET equippedBy = NULL WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertPets(final List<PetEntity> pets,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPetEntity.insert(pets);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object equipPet(final String petId, final long userId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfEquipPet.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, userId);
        _argIndex = 2;
        _stmt.bindString(_argIndex, petId);
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
          __preparedStmtOfEquipPet.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object unequipPet(final String petId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUnequipPet.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, petId);
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
          __preparedStmtOfUnequipPet.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<PetEntity>> getEquippedPets(final long userId) {
    final String _sql = "SELECT * FROM pets WHERE equippedBy = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, userId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"pets"}, new Callable<List<PetEntity>>() {
      @Override
      @NonNull
      public List<PetEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfRarity = CursorUtil.getColumnIndexOrThrow(_cursor, "rarity");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUrl");
          final int _cursorIndexOfEquippedBy = CursorUtil.getColumnIndexOrThrow(_cursor, "equippedBy");
          final int _cursorIndexOfIsForSale = CursorUtil.getColumnIndexOrThrow(_cursor, "isForSale");
          final int _cursorIndexOfSalePrice = CursorUtil.getColumnIndexOrThrow(_cursor, "salePrice");
          final List<PetEntity> _result = new ArrayList<PetEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PetEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpRarity;
            _tmpRarity = _cursor.getString(_cursorIndexOfRarity);
            final String _tmpImageUrl;
            _tmpImageUrl = _cursor.getString(_cursorIndexOfImageUrl);
            final Long _tmpEquippedBy;
            if (_cursor.isNull(_cursorIndexOfEquippedBy)) {
              _tmpEquippedBy = null;
            } else {
              _tmpEquippedBy = _cursor.getLong(_cursorIndexOfEquippedBy);
            }
            final boolean _tmpIsForSale;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsForSale);
            _tmpIsForSale = _tmp != 0;
            final Long _tmpSalePrice;
            if (_cursor.isNull(_cursorIndexOfSalePrice)) {
              _tmpSalePrice = null;
            } else {
              _tmpSalePrice = _cursor.getLong(_cursorIndexOfSalePrice);
            }
            _item = new PetEntity(_tmpId,_tmpName,_tmpType,_tmpRarity,_tmpImageUrl,_tmpEquippedBy,_tmpIsForSale,_tmpSalePrice);
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
  public Flow<List<PetEntity>> getAllPets() {
    final String _sql = "SELECT * FROM pets";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"pets"}, new Callable<List<PetEntity>>() {
      @Override
      @NonNull
      public List<PetEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfRarity = CursorUtil.getColumnIndexOrThrow(_cursor, "rarity");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUrl");
          final int _cursorIndexOfEquippedBy = CursorUtil.getColumnIndexOrThrow(_cursor, "equippedBy");
          final int _cursorIndexOfIsForSale = CursorUtil.getColumnIndexOrThrow(_cursor, "isForSale");
          final int _cursorIndexOfSalePrice = CursorUtil.getColumnIndexOrThrow(_cursor, "salePrice");
          final List<PetEntity> _result = new ArrayList<PetEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PetEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpRarity;
            _tmpRarity = _cursor.getString(_cursorIndexOfRarity);
            final String _tmpImageUrl;
            _tmpImageUrl = _cursor.getString(_cursorIndexOfImageUrl);
            final Long _tmpEquippedBy;
            if (_cursor.isNull(_cursorIndexOfEquippedBy)) {
              _tmpEquippedBy = null;
            } else {
              _tmpEquippedBy = _cursor.getLong(_cursorIndexOfEquippedBy);
            }
            final boolean _tmpIsForSale;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsForSale);
            _tmpIsForSale = _tmp != 0;
            final Long _tmpSalePrice;
            if (_cursor.isNull(_cursorIndexOfSalePrice)) {
              _tmpSalePrice = null;
            } else {
              _tmpSalePrice = _cursor.getLong(_cursorIndexOfSalePrice);
            }
            _item = new PetEntity(_tmpId,_tmpName,_tmpType,_tmpRarity,_tmpImageUrl,_tmpEquippedBy,_tmpIsForSale,_tmpSalePrice);
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
