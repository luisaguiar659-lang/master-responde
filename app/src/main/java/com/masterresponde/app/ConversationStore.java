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
    private static final int DB_VERSION = 2;
    private static final Object LOCK = new Object();

    public ConversationStore(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE conversations (" +
                "id TEXT PRIMARY KEY, kind TEXT NOT NULL, display_name TEXT NOT NULL, " +
                "last_message TEXT NOT NULL, updated_at INTEGER NOT NULL, " +
                "status TEXT NOT NULL DEFAULT 'OPEN', unread_count INTEGER NOT NULL DEFAULT 0, " +
                "labels TEXT NOT NULL DEFAULT '', notes TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE TABLE messages (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, conversation_id TEXT NOT NULL, " +
                "event_id TEXT NOT NULL, body TEXT NOT NULL, direction TEXT NOT NULL, " +
                "created_at INTEGER NOT NULL, UNIQUE(conversation_id,event_id,direction))");
        db.execSQL("CREATE INDEX idx_messages_conversation_time " +
                "ON messages(conversation_id,created_at)");
        db.execSQL("CREATE INDEX idx_conversations_updated ON conversations(updated_at)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Migração aditiva: mantém conversas e mensagens já armazenadas.
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE conversations ADD COLUMN labels TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE conversations ADD COLUMN notes TEXT NOT NULL DEFAULT ''");
        }
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

        try {
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
                        if (incrementUnread) {
                            db.execSQL("UPDATE conversations SET display_name=?,last_message=?,updated_at=?," +
                                    "unread_count=unread_count+1 WHERE id=?",
                                    new Object[]{name, body.trim(), now, id});
                        } else {
                            db.execSQL("UPDATE conversations SET display_name=?,last_message=?,updated_at=? WHERE id=?",
                                    new Object[]{name, body.trim(), now, id});
                        }
                        Cursor exists = db.rawQuery("SELECT 1 FROM conversations WHERE id=?",
                                new String[]{id});
                        boolean found;
                        try { found = exists.moveToFirst(); } finally { exists.close(); }
                        if (!found) {
                            ContentValues first = new ContentValues();
                            first.put("id", id);
                            first.put("kind", kind == null ? "private" : kind);
                            first.put("display_name", name);
                            first.put("last_message", body.trim());
                            first.put("updated_at", now);
                            first.put("status", "OPEN");
                            first.put("unread_count", incrementUnread ? 1 : 0);
                            db.insertOrThrow("conversations", null, first);
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                    helper.close();
                }
            }
        } catch (Exception ignored) {
            // O histórico é complementar: nunca pode interromper uma automação.
        }
    }

    public Cursor conversations(Context context) {
        return conversations(context, "ALL", "");
    }

    public Cursor conversations(Context context, String status, String query) {
        String selectedStatus = status == null ? "ALL" : status;
        String term = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        StringBuilder sql = new StringBuilder(
                "SELECT id,kind,display_name,last_message,updated_at,status,unread_count,labels " +
                "FROM conversations WHERE 1=1");
        java.util.ArrayList<String> args = new java.util.ArrayList<>();
        if (!"ALL".equals(selectedStatus)) {
            sql.append(" AND status=?");
            args.add(selectedStatus);
        }
        if (!term.isEmpty()) {
            sql.append(" AND (LOWER(display_name) LIKE ? OR LOWER(last_message) LIKE ? " +
                    "OR LOWER(labels) LIKE ? OR LOWER(notes) LIKE ?)");
            String like = "%" + term + "%";
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }
        sql.append(" ORDER BY updated_at DESC");
        return getReadableDatabase().rawQuery(sql.toString(), args.toArray(new String[0]));
    }

    public Cursor messages(Context context, String conversationId) {
        return getReadableDatabase().rawQuery(
                "SELECT body,direction,created_at FROM messages " +
                "WHERE conversation_id=? ORDER BY created_at ASC",
                new String[]{conversationId});
    }

    public String labels(String id) {
        return readConversationField(id, "labels");
    }

    public String notes(String id) {
        return readConversationField(id, "notes");
    }

    private String readConversationField(String id, String field) {
        if (!"labels".equals(field) && !"notes".equals(field)) return "";
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT " + field + " FROM conversations WHERE id=?",
                new String[]{id});
        try { return c.moveToFirst() ? c.getString(0) : ""; }
        finally { c.close(); }
    }

    public void updateLabels(String id, String labels) {
        ContentValues values = new ContentValues();
        values.put("labels", labels == null ? "" : labels.trim());
        getWritableDatabase().update("conversations", values, "id=?", new String[]{id});
    }

    public void updateNotes(String id, String notes) {
        ContentValues values = new ContentValues();
        values.put("notes", notes == null ? "" : notes.trim());
        getWritableDatabase().update("conversations", values, "id=?", new String[]{id});
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
