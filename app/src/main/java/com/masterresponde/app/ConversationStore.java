package com.masterresponde.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.app.Notification;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/** Armazenamento local e isolado do histórico de atendimentos. */
public final class ConversationStore extends SQLiteOpenHelper {
    private static final String DB_NAME = "master_conversations.db";
    private static final int DB_VERSION = 1;
    private static final Object LOCK = new Object();

    public ConversationStore(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE conversations (" +
                "id TEXT PRIMARY KEY, kind TEXT NOT NULL, display_name TEXT NOT NULL, " +
                "last_message TEXT NOT NULL, updated_at INTEGER NOT NULL, " +
                "status TEXT NOT NULL DEFAULT 'OPEN', unread_count INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE messages (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, conversation_id TEXT NOT NULL, " +
                "event_id TEXT NOT NULL, body TEXT NOT NULL, direction TEXT NOT NULL, " +
                "created_at INTEGER NOT NULL, UNIQUE(conversation_id,event_id,direction))");
        db.execSQL("CREATE INDEX idx_messages_conversation_time " +
                "ON messages(conversation_id,created_at)");
        db.execSQL("CREATE INDEX idx_conversations_updated ON conversations(updated_at)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Migrações futuras serão aditivas para preservar o histórico.
    }

    public static void recordIncoming(Context context, String kind, String sourceKey,
                                       String displayName, String body, String eventId,
                                       boolean group) {
        record(context, kind, sourceKey, displayName, body, eventId, "IN", group);
    }

    public static void recordOutgoing(Context context, Notification notification, String body) {
        if (notification == null) return;
        String shortcut = notification.getShortcutId();
        String title = "";
        String kind = "private";
        if (notification.extras != null) {
            CharSequence group = notification.extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE);
            CharSequence contact = notification.extras.getCharSequence(Notification.EXTRA_TITLE);
            if (group != null && group.length() > 0) {
                title = group.toString();
                kind = "group";
            } else if (contact != null) {
                title = contact.toString();
            }
        }
        String key = shortcut != null && !shortcut.trim().isEmpty()
                ? "shortcut|" + shortcut : kind + "|" + title;
        record(context, kind, key, title, body,
                "out|" + System.currentTimeMillis() + "|" + hash(body), "OUT", false);
    }

    private static void record(Context context, String kind, String sourceKey,
                               String displayName, String body, String eventId,
                               String direction, boolean incrementUnread) {
        if (context == null || body == null || body.trim().isEmpty()) return;
        String id = hash((kind == null ? "private" : kind) + "|" +
                (sourceKey == null ? "" : sourceKey));
        String name = displayName == null || displayName.trim().isEmpty()
                ? ("group".equals(kind) ? "Grupo" : "Contato")
                : displayName.trim();
        String event = eventId == null || eventId.trim().isEmpty()
                ? direction + "|" + System.currentTimeMillis() + "|" + hash(body)
                : eventId.trim();
        long now = System.currentTimeMillis();

        synchronized (LOCK) {
            ConversationStore helper = new ConversationStore(context);
            SQLiteDatabase db = helper.getWritableDatabase();
            db.beginTransaction();
            try {
                ContentValues message = new ContentValues();
                message.put("conversation_id", id);
                message.put("event_id", event);
                message.put("body", body.trim());
                message.put("direction", direction);
                message.put("created_at", now);
                long inserted = db.insertWithOnConflict("messages", null, message,
                        SQLiteDatabase.CONFLICT_IGNORE);
                if (inserted != -1L) {
                    ContentValues conversation = new ContentValues();
                    conversation.put("id", id);
                    conversation.put("kind", kind == null ? "private" : kind);
                    conversation.put("display_name", name);
                    conversation.put("last_message", body.trim());
                    conversation.put("updated_at", now);
                    if (incrementUnread) {
                        db.execSQL("INSERT INTO conversations(id,kind,display_name,last_message,updated_at,status,unread_count) " +
                                "VALUES(?,?,?,?,?,'OPEN',1) ON CONFLICT(id) DO UPDATE SET " +
                                "display_name=excluded.display_name,last_message=excluded.last_message," +
                                "updated_at=excluded.updated_at,unread_count=conversations.unread_count+1",
                                new Object[]{id, kind == null ? "private" : kind, name, body.trim(), now});
                    } else {
                        db.execSQL("INSERT INTO conversations(id,kind,display_name,last_message,updated_at,status,unread_count) " +
                                "VALUES(?,?,?,?,?,'OPEN',0) ON CONFLICT(id) DO UPDATE SET " +
                                "display_name=excluded.display_name,last_message=excluded.last_message," +
                                "updated_at=excluded.updated_at",
                                new Object[]{id, kind == null ? "private" : kind, name, body.trim(), now});
                    }
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
                helper.close();
            }
        }
    }

    public Cursor conversations(Context context) {
        return getReadableDatabase().rawQuery(
                "SELECT id,kind,display_name,last_message,updated_at,status,unread_count " +
                "FROM conversations ORDER BY updated_at DESC", null);
    }

    public Cursor messages(Context context, String conversationId) {
        return getReadableDatabase().rawQuery(
                "SELECT body,direction,created_at FROM messages " +
                "WHERE conversation_id=? ORDER BY created_at ASC",
                new String[]{conversationId});
    }

    public void updateStatus(String id, String status) {
        if (!"OPEN".equals(status) && !"WAITING".equals(status) &&
                !"RESOLVED".equals(status) && !"PAUSED".equals(status)) return;
        ContentValues values = new ContentValues();
        values.put("status", status);
        getWritableDatabase().update("conversations", values, "id=?", new String[]{id});
    }

    public void markRead(String id) {
        ContentValues values = new ContentValues();
        values.put("unread_count", 0);
        getWritableDatabase().update("conversations", values, "id=?", new String[]{id});
    }

    public static String hash(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < 12 && i < bytes.length; i++) {
                out.append(String.format(Locale.ROOT, "%02x", bytes[i] & 0xff));
            }
            return out.toString();
        } catch (Exception ignored) {
            return Integer.toHexString(value == null ? 0 : value.hashCode());
        }
    }

    public static String statusLabel(String status) {
        if ("WAITING".equals(status)) return "Aguardando";
        if ("RESOLVED".equals(status)) return "Resolvido";
        if ("PAUSED".equals(status)) return "Pausado";
        return "Em andamento";
    }
}
