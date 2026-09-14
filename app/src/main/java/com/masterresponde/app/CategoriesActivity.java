package com.masterresponde.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.UUID;

public class CategoriesActivity extends Activity {

    private static final int REQUEST_PHOTO = 4101;

    private SharedPreferences prefs;
    private LinearLayout content;
    private String pendingPhotoCategoryId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("master_responde", MODE_PRIVATE);
        buildScreen();
        render();
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(7, 9, 11));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(14), dp(10), dp(14), dp(10));

        Button back = new Button(this);
        back.setText("←");
        back.setTextColor(Color.WHITE);
        back.setTextSize(24);
        back.setBackgroundColor(Color.TRANSPARENT);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(58), dp(54)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(text("CATEGORIAS", 21, Color.WHITE, true));
        titles.addView(text(
                "Conteúdo exclusivo do menu @infor nos grupos",
                12,
                Color.rgb(150, 160, 170),
                false
        ));
        header.addView(titles, new LinearLayout.LayoutParams(0, dp(58), 1f));

        root.addView(header);

        View line = new View(this);
        line.setBackgroundResource(getResources().getIdentifier(
                "line", "drawable", getPackageName()));
        root.addView(line, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(3)));

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(40));
        scroll.addView(content);

        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }

    private void render() {
        content.removeAllViews();

        Button addCategory = actionButton("+ ADICIONAR CATEGORIA", true);
        addCategory.setOnClickListener(v -> showCategoryDialog());
        content.addView(addCategory);

        gap(10);

        content.addView(text(
                "Estas categorias são usadas SOMENTE pelo menu @infor nos grupos. " +
                "Você pode guardar textos e links para cada opção. " +
                "Exemplo: Banners → Telegram; Tutoriais/Material → Google Drive.",
                12,
                Color.rgb(145, 155, 165),
                false
        ));

        gap(16);

        JSONArray categories = loadCategories();

        if (categories.length() == 0) {
            LinearLayout empty = card();
            empty.addView(text(
                    "Nenhuma categoria cadastrada.",
                    15,
                    Color.rgb(170, 180, 190),
                    false
            ));
            empty.addView(text(
                    "Crie uma categoria e depois adicione textos, links ou fotos.",
                    13,
                    Color.rgb(130, 145, 155),
                    false
            ));
            content.addView(empty);
            return;
        }

        for (int i = 0; i < categories.length(); i++) {
            JSONObject category = categories.optJSONObject(i);
            if (category == null) continue;

            final String categoryId = category.optString("id");
            String name = category.optString("name");
            JSONArray responses = category.optJSONArray("responses");
            if (responses == null) responses = new JSONArray();

            LinearLayout c = card();

            c.addView(text(
                    name,
                    18,
                    Color.WHITE,
                    true
            ));

            c.addView(text(
                    responses.length() + (responses.length() == 1 ? " item" : " itens"),
                    11,
                    Color.rgb(255, 150, 0),
                    true
            ));

            if (responses.length() == 0) {
                TextView none = text(
                        "Ainda sem itens.",
                        12,
                        Color.rgb(135, 145, 155),
                        false
                );
                none.setPadding(0, dp(10), 0, dp(4));
                c.addView(none);
            }

            for (int r = 0; r < responses.length(); r++) {
                JSONObject response = responses.optJSONObject(r);
                if (response == null) continue;

                final String responseId = response.optString("id");
                String type = response.optString("type", "text");

                LinearLayout responseBox = new LinearLayout(this);
                responseBox.setOrientation(LinearLayout.VERTICAL);
                responseBox.setPadding(dp(10), dp(10), dp(10), dp(8));

                if ("photo".equals(type)) {
                    String uriValue = response.optString("uri", "");
                    String fileName = response.optString("name", "Foto");

                    responseBox.addView(text(
                            (r + 1) + ". 📷 FOTO — " + fileName,
                            13,
                            Color.rgb(205, 215, 220),
                            true
                    ));

                    if (!uriValue.isEmpty()) {
                        try {
                            ImageView preview = new ImageView(this);
                            preview.setAdjustViewBounds(true);
                            preview.setScaleType(ImageView.ScaleType.CENTER_CROP);

                            LinearLayout.LayoutParams imageLp =
                                    new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            dp(180)
                                    );

                            imageLp.setMargins(
                                    0,
                                    dp(8),
                                    0,
                                    dp(6)
                            );

                            preview.setLayoutParams(imageLp);
                            preview.setImageURI(Uri.parse(uriValue));
                            responseBox.addView(preview);

                        } catch (Exception ignored) {
                        }
                    }

                    TextView mediaNotice = text(
                            "Foto mantida apenas como arquivo local da categoria. Para atendimento automático, prefira LINKS.",
                            11,
                            Color.rgb(255, 150, 0),
                            false
                    );

                    mediaNotice.setPadding(
                            0,
                            dp(4),
                            0,
                            dp(2)
                    );

                    responseBox.addView(mediaNotice);

                } else if ("link".equals(type)) {
                    final String linkName = response.optString("name", "").trim();
                    final String linkUrl = response.optString("url", "").trim();

                    responseBox.addView(text(
                            (r + 1) + ". 🔗 " +
                                    (linkName.isEmpty() ? "LINK" : linkName),
                            13,
                            Color.rgb(0, 184, 255),
                            true
                    ));

                    responseBox.addView(text(
                            linkUrl,
                            12,
                            Color.rgb(205, 215, 220),
                            false
                    ));

                    Button editLink = smallButton("EDITAR LINK");
                    editLink.setOnClickListener(v ->
                            showLinkDialog(
                                    categoryId,
                                    responseId,
                                    linkName,
                                    linkUrl
                            ));
                    responseBox.addView(editLink);

                } else {
                    String responseText = response.optString("text");

                    responseBox.addView(text(
                            (r + 1) + ". 📝 " + responseText,
                            13,
                            Color.rgb(205, 215, 220),
                            false
                    ));
                }

                Button deleteResponse = smallButton(
                        "photo".equals(type)
                                ? "EXCLUIR FOTO"
                                : ("link".equals(type)
                                ? "EXCLUIR LINK"
                                : "EXCLUIR RESPOSTA")
                );

                deleteResponse.setOnClickListener(v ->
                        deleteResponse(categoryId, responseId));

                responseBox.addView(deleteResponse);
                c.addView(responseBox);
            }

            Button addResponse = actionButton("+ ADICIONAR TEXTO", true);
            addResponse.setOnClickListener(v ->
                    showResponseDialog(categoryId));
            c.addView(addResponse);

            Button addLink = actionButton("+ ADICIONAR LINK", false);
            addLink.setOnClickListener(v ->
                    showLinkDialog(
                            categoryId,
                            "",
                            "",
                            ""
                    ));
            c.addView(addLink);

            Button addPhoto = actionButton("+ ADICIONAR FOTO", false);
            addPhoto.setOnClickListener(v ->
                    choosePhoto(categoryId));
            c.addView(addPhoto);

            Button deleteCategory = actionButton("EXCLUIR CATEGORIA", false);
            deleteCategory.setOnClickListener(v ->
                    confirmDeleteCategory(categoryId));
            c.addView(deleteCategory);

            content.addView(c);
            gap(12);
        }
    }

    private void showCategoryDialog() {
        EditText name = field("Nome da categoria");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Nova categoria")
                .setView(name)
                .setNegativeButton("CANCELAR", null)
                .setPositiveButton("SALVAR", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setOnClickListener(v -> {
                String value = name.getText().toString().trim();

                if (value.isEmpty()) {
                    name.setError("Informe o nome.");
                    return;
                }

                addCategory(value);
                dialog.dismiss();
                render();
            });
        });

        dialog.show();
    }

    private void showResponseDialog(String categoryId) {
        EditText response = field("Texto da resposta");
        response.setMinLines(4);
        response.setGravity(Gravity.TOP);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Nova resposta")
                .setView(response)
                .setNegativeButton("CANCELAR", null)
                .setPositiveButton("SALVAR", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setOnClickListener(v -> {
                String value = response.getText().toString().trim();

                if (value.isEmpty()) {
                    response.setError("Informe o texto.");
                    return;
                }

                addResponse(categoryId, value);
                dialog.dismiss();
                render();
            });
        });

        dialog.show();
    }

    private void addCategory(String name) {
        JSONArray categories = loadCategories();

        JSONObject category = new JSONObject();
        try {
            category.put("id", UUID.randomUUID().toString());
            category.put("name", name);
            category.put("responses", new JSONArray());
            categories.put(category);
            saveCategories(categories);
            Toast.makeText(this, "Categoria criada.", Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {
        }
    }

    private void addResponse(String categoryId, String text) {
        JSONArray categories = loadCategories();

        for (int i = 0; i < categories.length(); i++) {
            JSONObject category = categories.optJSONObject(i);
            if (category == null) continue;
            if (!categoryId.equals(category.optString("id"))) continue;

            JSONArray responses = category.optJSONArray("responses");
            if (responses == null) responses = new JSONArray();

            JSONObject response = new JSONObject();
            try {
                response.put("id", UUID.randomUUID().toString());
                response.put("type", "text");
                response.put("text", text);
                responses.put(response);
                category.put("responses", responses);
            } catch (Exception ignored) {
            }
            break;
        }

        saveCategories(categories);
        Toast.makeText(this, "Resposta adicionada.", Toast.LENGTH_SHORT).show();
    }

    private void showLinkDialog(
            String categoryId,
            String responseId,
            String existingName,
            String existingUrl
    ) {
        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        EditText name =
                field(
                        "Nome do link (ex.: Banners no Telegram)"
                );

        name.setText(
                existingName == null
                        ? ""
                        : existingName
        );

        EditText url =
                field(
                        "Link (ex.: https://t.me/...)"
                );

        url.setText(
                existingUrl == null
                        ? ""
                        : existingUrl
        );

        box.addView(name);
        box.addView(url);

        boolean editing =
                responseId != null
                        && !responseId.trim().isEmpty();

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                editing
                                        ? "Editar link"
                                        : "Novo link"
                        )
                        .setView(box)
                        .setNegativeButton(
                                "CANCELAR",
                                null
                        )
                        .setPositiveButton(
                                "SALVAR",
                                null
                        )
                        .create();

        dialog.setOnShowListener(
                d -> {
                    Button save =
                            dialog.getButton(
                                    AlertDialog.BUTTON_POSITIVE
                            );

                    save.setOnClickListener(
                            v -> {
                                String linkName =
                                        name.getText()
                                                .toString()
                                                .trim();

                                String linkUrl =
                                        normalizeUrl(
                                                url.getText()
                                                        .toString()
                                                        .trim()
                                        );

                                if (linkUrl.isEmpty()) {
                                    url.setError(
                                            "Informe o link."
                                    );
                                    return;
                                }

                                if (!isSupportedUrl(
                                        linkUrl
                                )) {
                                    url.setError(
                                            "Use um link http://, https:// ou tg://"
                                    );
                                    return;
                                }

                                saveLink(
                                        categoryId,
                                        responseId,
                                        linkName,
                                        linkUrl
                                );

                                dialog.dismiss();
                                render();
                            }
                    );
                }
        );

        dialog.show();
    }

    private String normalizeUrl(
            String value
    ) {
        if (value == null) {
            return "";
        }

        String result =
                value.trim();

        if (result.isEmpty()) {
            return "";
        }

        String lower =
                result.toLowerCase(
                        java.util.Locale.ROOT
                );

        if (lower.startsWith("http://")
                || lower.startsWith("https://")
                || lower.startsWith("tg://")) {
            return result;
        }

        if (result.contains(".")) {
            return "https://" + result;
        }

        return result;
    }

    private boolean isSupportedUrl(
            String value
    ) {
        if (value == null) {
            return false;
        }

        String lower =
                value.toLowerCase(
                        java.util.Locale.ROOT
                );

        return lower.startsWith("http://")
                || lower.startsWith("https://")
                || lower.startsWith("tg://");
    }

    private void saveLink(
            String categoryId,
            String responseId,
            String name,
            String url
    ) {
        JSONArray categories =
                loadCategories();

        for (int i = 0;
             i < categories.length();
             i++) {

            JSONObject category =
                    categories.optJSONObject(
                            i
                    );

            if (category == null) continue;

            if (!categoryId.equals(
                    category.optString(
                            "id"
                    )
            )) {
                continue;
            }

            JSONArray responses =
                    category.optJSONArray(
                            "responses"
                    );

            if (responses == null) {
                responses =
                        new JSONArray();
            }

            JSONObject target =
                    null;

            if (responseId != null
                    && !responseId.trim().isEmpty()) {

                for (int r = 0;
                     r < responses.length();
                     r++) {

                    JSONObject current =
                            responses.optJSONObject(
                                    r
                            );

                    if (current == null) continue;

                    if (responseId.equals(
                            current.optString(
                                    "id"
                            )
                    )) {
                        target =
                                current;
                        break;
                    }
                }
            }

            if (target == null) {
                target =
                        new JSONObject();

                try {
                    target.put(
                            "id",
                            UUID.randomUUID()
                                    .toString()
                    );

                    responses.put(
                            target
                    );

                } catch (Exception ignored) {
                }
            }

            try {
                target.put(
                        "type",
                        "link"
                );

                target.put(
                        "name",
                        name == null
                                ? ""
                                : name.trim()
                );

                target.put(
                        "url",
                        url
                );

                category.put(
                        "responses",
                        responses
                );

            } catch (Exception ignored) {
            }

            break;
        }

        saveCategories(
                categories
        );

        Toast.makeText(
                this,
                "Link salvo na categoria.",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void choosePhoto(String categoryId) {
        pendingPhotoCategoryId =
                categoryId == null
                        ? ""
                        : categoryId;

        Intent intent =
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT
                );

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        intent.setType(
                "image/*"
        );

        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        );

        try {
            startActivityForResult(
                    intent,
                    REQUEST_PHOTO
            );

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Não consegui abrir a galeria de fotos.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode != REQUEST_PHOTO
                || resultCode != RESULT_OK
                || data == null
                || data.getData() == null) {
            return;
        }

        if (pendingPhotoCategoryId == null
                || pendingPhotoCategoryId.isEmpty()) {
            return;
        }

        Uri uri =
                data.getData();

        try {
            int flags =
                    data.getFlags()
                            & Intent.FLAG_GRANT_READ_URI_PERMISSION;

            getContentResolver()
                    .takePersistableUriPermission(
                            uri,
                            flags
                    );

        } catch (Exception ignored) {
        }

        String fileName =
                photoName(
                        uri
                );

        addPhoto(
                pendingPhotoCategoryId,
                uri.toString(),
                fileName
        );

        pendingPhotoCategoryId =
                "";

        render();
    }

    private String photoName(Uri uri) {
        String result =
                "Foto selecionada";

        if (uri == null) {
            return result;
        }

        Cursor cursor =
                null;

        try {
            cursor =
                    getContentResolver()
                            .query(
                                    uri,
                                    new String[]{
                                            OpenableColumns.DISPLAY_NAME
                                    },
                                    null,
                                    null,
                                    null
                            );

            if (cursor != null
                    && cursor.moveToFirst()) {

                int index =
                        cursor.getColumnIndex(
                                OpenableColumns.DISPLAY_NAME
                        );

                if (index >= 0) {
                    String value =
                            cursor.getString(
                                    index
                            );

                    if (value != null
                            && !value.trim().isEmpty()) {
                        result =
                                value.trim();
                    }
                }
            }

        } catch (Exception ignored) {

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return result;
    }

    private void addPhoto(
            String categoryId,
            String uri,
            String fileName
    ) {
        JSONArray categories =
                loadCategories();

        for (int i = 0;
             i < categories.length();
             i++) {

            JSONObject category =
                    categories.optJSONObject(
                            i
                    );

            if (category == null) continue;

            if (!categoryId.equals(
                    category.optString(
                            "id"
                    )
            )) {
                continue;
            }

            JSONArray responses =
                    category.optJSONArray(
                            "responses"
                    );

            if (responses == null) {
                responses =
                        new JSONArray();
            }

            JSONObject response =
                    new JSONObject();

            try {
                response.put(
                        "id",
                        UUID.randomUUID()
                                .toString()
                );

                response.put(
                        "type",
                        "photo"
                );

                response.put(
                        "uri",
                        uri
                );

                response.put(
                        "name",
                        fileName
                );

                responses.put(
                        response
                );

                category.put(
                        "responses",
                        responses
                );

            } catch (Exception ignored) {
            }

            break;
        }

        saveCategories(
                categories
        );

        Toast.makeText(
                this,
                "Foto adicionada à categoria.",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void deleteResponse(String categoryId, String responseId) {
        JSONArray categories = loadCategories();

        for (int i = 0; i < categories.length(); i++) {
            JSONObject category = categories.optJSONObject(i);
            if (category == null) continue;
            if (!categoryId.equals(category.optString("id"))) continue;

            JSONArray oldResponses = category.optJSONArray("responses");
            if (oldResponses == null) oldResponses = new JSONArray();

            JSONArray newResponses = new JSONArray();

            for (int r = 0; r < oldResponses.length(); r++) {
                JSONObject response = oldResponses.optJSONObject(r);
                if (response == null) continue;

                if (!responseId.equals(response.optString("id"))) {
                    newResponses.put(response);
                }
            }

            try {
                category.put("responses", newResponses);
            } catch (Exception ignored) {
            }

            break;
        }

        saveCategories(categories);
        render();
    }

    private void confirmDeleteCategory(String categoryId) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir categoria?")
                .setMessage("As respostas dessa categoria também serão excluídas.")
                .setNegativeButton("CANCELAR", null)
                .setPositiveButton("EXCLUIR", (d, w) -> {
                    deleteCategory(categoryId);
                    render();
                })
                .show();
    }

    private void deleteCategory(String categoryId) {
        JSONArray oldCategories = loadCategories();
        JSONArray newCategories = new JSONArray();

        for (int i = 0; i < oldCategories.length(); i++) {
            JSONObject category = oldCategories.optJSONObject(i);
            if (category == null) continue;

            if (!categoryId.equals(category.optString("id"))) {
                newCategories.put(category);
            }
        }

        saveCategories(newCategories);
    }

    private JSONArray loadCategories() {
        try {
            return new JSONArray(
                    prefs.getString("text_categories", "[]")
            );
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private void saveCategories(JSONArray categories) {
        prefs.edit()
                .putString("text_categories", categories.toString())
                .apply();
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(15), dp(15), dp(15), dp(15));
        c.setBackgroundResource(getResources().getIdentifier(
                "card", "drawable", getPackageName()));
        return c;
    }

    private EditText field(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextColor(Color.BLACK);
        e.setHintTextColor(Color.rgb(120, 120, 120));
        e.setTextSize(14);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(dp(18), dp(8), dp(18), dp(8));
        e.setLayoutParams(lp);

        return e;
    }

    private Button actionButton(String label, boolean orange) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(12);
        b.setTypeface(null, 1);
        b.setBackgroundResource(getResources().getIdentifier(
                orange ? "button_orange" : "button_dark",
                "drawable",
                getPackageName()
        ));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(54)
        );
        lp.setMargins(0, dp(4), 0, dp(4));
        b.setLayoutParams(lp);

        return b;
    }

    private Button smallButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.rgb(220, 225, 230));
        b.setTextSize(10);
        b.setBackgroundResource(getResources().getIdentifier(
                "button_dark",
                "drawable",
                getPackageName()
        ));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(44)
        );
        lp.setMargins(0, dp(6), 0, dp(4));
        b.setLayoutParams(lp);

        return b;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, 1);
        return t;
    }

    private void gap(int height) {
        View v = new View(this);
        content.addView(v, new LinearLayout.LayoutParams(1, dp(height)));
    }

    private int dp(int value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density
        );
    }
}
