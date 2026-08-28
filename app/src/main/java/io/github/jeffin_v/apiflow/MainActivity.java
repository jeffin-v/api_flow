package io.github.jeffin_v.apiflow;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A dependency-free API request workstation. Native platform widgets keep the app lean and make
 * the APK reproducible without downloading third-party artifacts during a build.
 */
public class MainActivity extends Activity {
    private static final int MAX_RESPONSE_BYTES = 1024 * 1024;
    private static final String PREFS = "api_flow";
    private static final String KEY_COLLECTIONS = "collections";
    private static final String KEY_HISTORY = "history";
    private static final String KEY_BASE_URL = "base_url";
    private static final String KEY_THEME = "theme";
    private static final String THEME_SYSTEM = "system";
    private static final String THEME_LIGHT = "light";
    private static final String THEME_DARK = "dark";

    private int INK;
    private int CANVAS;
    private int SURFACE;
    private int LINE;
    private int BLUE;
    private int BLUE_DARK;
    private int MINT;
    private int MUTED;
    private int DANGER;
    private int TOP;
    private int FIELD;
    private int SUBTLE;
    private int CHIP;
    private int BODY_TEXT;
    private int DANGER_SURFACE;
    private int TITLE_MUTED;

    private SharedPreferences prefs;
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private Spinner methodSpinner;
    private EditText urlInput;
    private EditText bodyInput;
    private LinearLayout headersRows;
    private LinearLayout paramsRows;
    private LinearLayout headersPane;
    private LinearLayout paramsPane;
    private LinearLayout bodyPane;
    private TextView responseMeta;
    private TextView responseHeaders;
    private TextView responseBody;
    private View statusSpacer;
    private Button sendButton;
    private Dialog activeSheet;
    private final ArrayList<Button> tabButtons = new ArrayList<>();
    private String activeTab = "Headers";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        applyThemeMode();
        setTitle("API Flow");
        setContentView(createContent());
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        networkExecutor.shutdownNow();
        super.onDestroy();
    }

    /** Applies the saved appearance before any views or dialogs are created. */
    private void applyThemeMode() {
        String preference = prefs.getString(KEY_THEME, THEME_SYSTEM);
        boolean systemDark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        boolean dark = THEME_DARK.equals(preference) || (THEME_SYSTEM.equals(preference) && systemDark);
        setTheme(dark ? R.style.Theme_ApiFlow_Dark : R.style.Theme_ApiFlow);

        if (dark) {
            INK = Color.parseColor("#E8EDF5");
            CANVAS = Color.parseColor("#07111F");
            SURFACE = Color.parseColor("#101C2E");
            LINE = Color.parseColor("#2A3A52");
            BLUE = Color.parseColor("#60A5FA");
            BLUE_DARK = Color.parseColor("#BFDBFE");
            MINT = Color.parseColor("#34D399");
            MUTED = Color.parseColor("#A7B4C7");
            DANGER = Color.parseColor("#FDA4AF");
            TOP = Color.parseColor("#020617");
            FIELD = Color.parseColor("#152238");
            SUBTLE = Color.parseColor("#17243A");
            CHIP = Color.parseColor("#1D365B");
            BODY_TEXT = Color.parseColor("#D8E4F4");
            DANGER_SURFACE = Color.parseColor("#3B1720");
            TITLE_MUTED = Color.parseColor("#B8C6DE");
        } else {
            INK = Color.parseColor("#0B1220");
            CANVAS = Color.parseColor("#F6F8FC");
            SURFACE = Color.WHITE;
            LINE = Color.parseColor("#DCE3EF");
            BLUE = Color.parseColor("#2563EB");
            BLUE_DARK = Color.parseColor("#173EA5");
            MINT = Color.parseColor("#047857");
            MUTED = Color.parseColor("#64748B");
            DANGER = Color.parseColor("#B42318");
            TOP = Color.parseColor("#0B1220");
            FIELD = Color.parseColor("#FBFDFF");
            SUBTLE = Color.parseColor("#F1F5F9");
            CHIP = Color.parseColor("#EFF4FF");
            BODY_TEXT = Color.parseColor("#334155");
            DANGER_SURFACE = Color.parseColor("#FFF1F2");
            TITLE_MUTED = Color.parseColor("#B8C6DE");
        }
        getWindow().setStatusBarColor(TOP);
        getWindow().setNavigationBarColor(TOP);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(0);
        }
    }

    private String currentThemeLabel() {
        String preference = prefs.getString(KEY_THEME, THEME_SYSTEM);
        if (THEME_DARK.equals(preference)) return "Dark";
        if (THEME_LIGHT.equals(preference)) return "Light";
        return "System default";
    }

    private String themeKeyForLabel(String label) {
        if ("Dark".equals(label)) return THEME_DARK;
        if ("Light".equals(label)) return THEME_LIGHT;
        return THEME_SYSTEM;
    }

    private View createContent() {
        LinearLayout root = vertical();
        root.setBackgroundColor(CANVAS);
        statusSpacer = new View(this);
        statusSpacer.setBackgroundColor(TOP);
        root.addView(statusSpacer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8)));
        root.addView(buildTopBar());
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout content = vertical();
        content.setPadding(dp(14), dp(14), dp(14), dp(28));
        content.addView(buildRequestCard());
        content.addView(buildResponseCard(), margins(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 14, 0, 0));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int topInset = Math.max(dp(8), insets.getSystemWindowInsetTop());
            statusSpacer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, topInset));
            return insets;
        });
        root.requestApplyInsets();
        return root;
    }

    private View buildTopBar() {
        LinearLayout bar = horizontal();
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(14), dp(10), dp(10), dp(10));
        bar.setBackgroundColor(TOP);

        TextView mark = text("›_", 24, Color.WHITE, true);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(round(BLUE, dp(10)));
        bar.addView(mark, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout titleBlock = vertical();
        titleBlock.setPadding(dp(10), 0, dp(4), 0);
        TextView title = text("API FLOW", 16, Color.WHITE, true);
        title.setLetterSpacing(.10f);
        titleBlock.addView(title);
        titleBlock.addView(text("A focused mobile API workspace", 11, TITLE_MUTED, false));
        bar.addView(titleBlock, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        ToolbarIconButton collections = topIconButton(ToolbarIconButton.COLLECTIONS, "Collections");
        collections.setOnClickListener(v -> showCollections());
        bar.addView(collections, new LinearLayout.LayoutParams(dp(44), dp(48)));
        ToolbarIconButton history = topIconButton(ToolbarIconButton.HISTORY, "History");
        history.setOnClickListener(v -> showHistory());
        bar.addView(history, new LinearLayout.LayoutParams(dp(44), dp(48)));
        ToolbarIconButton settings = topIconButton(ToolbarIconButton.SETTINGS, "Settings");
        // Optical alignment: this glyph sits slightly high at the same baseline as the others.
        settings.setTranslationY(2f);
        settings.setOnClickListener(v -> showSettings());
        bar.addView(settings, new LinearLayout.LayoutParams(dp(44), dp(48)));
        return bar;
    }

    private ToolbarIconButton topIconButton(int icon, String description) {
        ToolbarIconButton button = new ToolbarIconButton(this, icon);
        button.setContentDescription(description);
        button.setBackground(round(Color.TRANSPARENT, dp(8)));
        button.setClickable(true);
        return button;
    }

    private View buildRequestCard() {
        LinearLayout card = card();
        TextView kicker = text("REQUEST BUILDER", 11, BLUE, true);
        kicker.setLetterSpacing(.10f);
        card.addView(kicker);
        TextView helper = text("Compose, inspect, and save API calls without desktop clutter.", 13, MUTED, false);
        card.addView(helper, margins(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4, 0, 12));

        LinearLayout urlRow = horizontal();
        urlRow.setGravity(Gravity.CENTER_VERTICAL);
        methodSpinner = new Spinner(this);
        String[] methods = {"GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"};
        ArrayAdapter<String> methodAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, methods);
        methodSpinner.setAdapter(methodAdapter);
        methodSpinner.setBackground(round(CHIP, dp(8), BLUE));
        urlRow.addView(methodSpinner, margins(dp(96), dp(50), 0, 0, 8, 0));
        urlInput = input("https://api.example.com/v1/resource", false);
        urlInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        urlInput.setSingleLine(true);
        urlInput.setContentDescription("Request URL");
        urlRow.addView(urlInput, new LinearLayout.LayoutParams(0, dp(50), 1f));
        card.addView(urlRow);

        sendButton = actionButton("Send request", BLUE, Color.WHITE);
        sendButton.setOnClickListener(v -> sendCurrentRequest());
        card.addView(sendButton, margins(ViewGroup.LayoutParams.MATCH_PARENT, dp(50), 0, 12, 0, 0));

        HorizontalScrollView tabScroll = new HorizontalScrollView(this);
        tabScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tabRow = horizontal();
        tabRow.setPadding(0, dp(12), 0, dp(7));
        for (String tab : new String[]{"Headers", "Query", "Body"}) {
            Button button = chipButton(tab);
            button.setOnClickListener(v -> selectTab(tab));
            tabButtons.add(button);
            tabRow.addView(button, margins(ViewGroup.LayoutParams.WRAP_CONTENT, dp(38), 0, 0, 8, 0));
        }
        tabScroll.addView(tabRow);
        card.addView(tabScroll);

        headersPane = vertical();
        headersPane.addView(text("Headers", 13, INK, true));
        headersPane.addView(text("Use one row per header. Empty keys are ignored.", 12, MUTED, false), margins(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 3, 0, 8));
        headersRows = vertical();
        headersPane.addView(headersRows);
        Button addHeader = outlineButton("+ Add header");
        addHeader.setOnClickListener(v -> addKeyValueRow(headersRows, "", "", "Header name", "Value"));
        headersPane.addView(addHeader, margins(ViewGroup.LayoutParams.MATCH_PARENT, dp(42), 0, 8, 0, 0));
        card.addView(headersPane);

        paramsPane = vertical();
        paramsPane.addView(text("Query parameters", 13, INK, true));
        paramsPane.addView(text("Parameters are URL encoded when the request is sent.", 12, MUTED, false), margins(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 3, 0, 8));
        paramsRows = vertical();
        paramsPane.addView(paramsRows);
        Button addParam = outlineButton("+ Add parameter");
        addParam.setOnClickListener(v -> addKeyValueRow(paramsRows, "", "", "Parameter", "Value"));
        paramsPane.addView(addParam, margins(ViewGroup.LayoutParams.MATCH_PARENT, dp(42), 0, 8, 0, 0));
        card.addView(paramsPane);

        bodyPane = vertical();
        LinearLayout bodyTop = horizontal();
        bodyTop.setGravity(Gravity.CENTER_VERTICAL);
        bodyTop.addView(text("Raw request body", 13, INK, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView rawChip = text("UTF-8", 11, BLUE_DARK, true);
        rawChip.setPadding(dp(8), dp(4), dp(8), dp(4));
        rawChip.setBackground(round(CHIP, dp(12)));
        bodyTop.addView(rawChip);
        bodyPane.addView(bodyTop);
        bodyPane.addView(text("Set Content-Type in Headers when required by the API.", 12, MUTED, false), margins(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 3, 0, 8));
        bodyInput = input("{\n  \"example\": true\n}", true);
        bodyInput.setTypeface(Typeface.MONOSPACE);
        bodyInput.setMinLines(7);
        bodyInput.setGravity(Gravity.TOP | Gravity.START);
        bodyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        bodyPane.addView(bodyInput, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(180)));
        card.addView(bodyPane);

        addKeyValueRow(headersRows, "Accept", "application/json", "Header name", "Value");
        selectTab("Headers");
        return card;
    }

    private View buildResponseCard() {
        LinearLayout card = card();
        LinearLayout responseHeading = horizontal();
        responseHeading.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = text("RESPONSE", 11, BLUE, true);
        label.setLetterSpacing(.10f);
        responseHeading.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button clear = outlineButton("Clear");
        clear.setTextSize(11);
        clear.setOnClickListener(v -> clearResponse());
        responseHeading.addView(clear, new LinearLayout.LayoutParams(dp(72), dp(36)));
        card.addView(responseHeading);
        responseMeta = text("Ready when you are", 13, MUTED, true);
        responseMeta.setPadding(0, dp(8), 0, dp(5));
        card.addView(responseMeta);
        responseHeaders = text("Response headers will appear here.", 12, MUTED, false);
        responseHeaders.setTypeface(Typeface.MONOSPACE);
        responseHeaders.setPadding(dp(10), dp(9), dp(10), dp(9));
        responseHeaders.setBackground(round(SUBTLE, dp(8)));
        card.addView(responseHeaders);
        InnerScrollView responseScroll = new InnerScrollView(this);
        responseScroll.setFillViewport(true);
        responseScroll.setVerticalScrollBarEnabled(true);
        responseScroll.setBackground(round(FIELD, dp(8), LINE));
        responseBody = text("Send a request to inspect its response body.", 13, BODY_TEXT, false);
        responseBody.setTextIsSelectable(true);
        responseBody.setTypeface(Typeface.MONOSPACE);
        responseBody.setGravity(Gravity.TOP | Gravity.START);
        responseBody.setPadding(dp(10), dp(12), dp(10), dp(12));
        responseBody.setMinHeight(dp(236));
        responseScroll.addView(responseBody, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        card.addView(responseScroll, margins(ViewGroup.LayoutParams.MATCH_PARENT, dp(260), 0, 10, 0, 0));
        return card;
    }

    private void selectTab(String tab) {
        activeTab = tab;
        headersPane.setVisibility("Headers".equals(tab) ? View.VISIBLE : View.GONE);
        paramsPane.setVisibility("Query".equals(tab) ? View.VISIBLE : View.GONE);
        bodyPane.setVisibility("Body".equals(tab) ? View.VISIBLE : View.GONE);
        for (Button button : tabButtons) {
            boolean selected = tab.equals(button.getText().toString());
            button.setTextColor(selected ? Color.WHITE : MUTED);
            button.setBackground(round(selected ? BLUE : SUBTLE, dp(18)));
        }
    }

    private void addKeyValueRow(LinearLayout container, String key, String value, String keyHint, String valueHint) {
        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER_VERTICAL);
        EditText keyField = compactInput(keyHint);
        keyField.setText(key);
        keyField.setTag("key");
        row.addView(keyField, margins(0, dp(44), 0, 0, 6, 0, 1f));
        EditText valueField = compactInput(valueHint);
        valueField.setText(value);
        valueField.setTag("value");
        row.addView(valueField, margins(0, dp(44), 0, 0, 6, 0, 1.25f));
        Button remove = new Button(this);
        remove.setText("×");
        remove.setTextSize(20);
        remove.setTextColor(DANGER);
        remove.setAllCaps(false);
        remove.setContentDescription("Remove row");
        remove.setPadding(0, 0, 0, 0);
        remove.setBackground(round(DANGER_SURFACE, dp(8)));
        remove.setOnClickListener(v -> container.removeView(row));
        row.addView(remove, new LinearLayout.LayoutParams(dp(42), dp(42)));
        container.addView(row, margins(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, 6));
    }

    private void sendCurrentRequest() {
        final RequestSpec request;
        try {
            request = buildRequest();
        } catch (IllegalArgumentException exception) {
            toast(exception.getMessage());
            return;
        }
        sendButton.setEnabled(false);
        sendButton.setText("Sending…");
        responseMeta.setText("Sending " + request.method + " request…");
        responseMeta.setTextColor(BLUE);
        responseBody.setText("Waiting for a response…");
        networkExecutor.execute(() -> executeRequest(request));
    }

    private RequestSpec buildRequest() {
        String rawUrl = expandVariables(urlInput.getText().toString().trim());
        if (TextUtils.isEmpty(rawUrl)) {
            throw new IllegalArgumentException("Enter an HTTP or HTTPS URL.");
        }
        String normalized = rawUrl.toLowerCase(Locale.US);
        if (!normalized.startsWith("https://") && !normalized.startsWith("http://")) {
            throw new IllegalArgumentException("Only http:// and https:// request URLs are supported.");
        }
        List<KeyValue> params = readRows(paramsRows);
        if (!params.isEmpty()) {
            StringBuilder query = new StringBuilder();
            for (KeyValue param : params) {
                if (query.length() > 0) query.append('&');
                query.append(urlEncode(param.key)).append('=').append(urlEncode(param.value));
            }
            rawUrl += (rawUrl.contains("?") ? "&" : "?") + query;
        }
        try {
            new URL(rawUrl);
        } catch (Exception exception) {
            throw new IllegalArgumentException("The request URL is malformed.");
        }
        return new RequestSpec(methodSpinner.getSelectedItem().toString(), rawUrl, readRows(headersRows), bodyInput.getText().toString());
    }

    private void executeRequest(RequestSpec request) {
        long started = System.nanoTime();
        HttpURLConnection connection = null;
        try {
            URL url = new URL(request.url);
            URLConnection rawConnection = url.openConnection();
            if (!(rawConnection instanceof HttpURLConnection)) {
                throw new IOException("The URL did not resolve to an HTTP connection.");
            }
            connection = (HttpURLConnection) rawConnection;
            connection.setRequestMethod(request.method);
            connection.setConnectTimeout(15_000);
            connection.setReadTimeout(30_000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "API-Flow-Android/1.0.2");
            boolean hasContentType = false;
            for (KeyValue header : request.headers) {
                connection.addRequestProperty(header.key, header.value);
                if ("content-type".equalsIgnoreCase(header.key)) hasContentType = true;
            }
            if (allowsBody(request.method) && !TextUtils.isEmpty(request.body)) {
                connection.setDoOutput(true);
                if (!hasContentType) connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                byte[] data = request.body.getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(data.length);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(data);
                }
            }
            int status = connection.getResponseCode();
            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            ReadBody body = stream == null ? new ReadBody("", false, 0) : readBody(stream);
            long elapsed = (System.nanoTime() - started) / 1_000_000L;
            HttpResult result = new HttpResult(status, elapsed, body.text, body.truncated, body.bytes, connection.getHeaderFields(), null);
            mainHandler.post(() -> renderResult(result, request));
        } catch (Exception exception) {
            long elapsed = (System.nanoTime() - started) / 1_000_000L;
            HttpResult result = new HttpResult(0, elapsed, "", false, 0, Collections.emptyMap(), exception.getMessage());
            mainHandler.post(() -> renderResult(result, request));
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private void renderResult(HttpResult result, RequestSpec request) {
        sendButton.setEnabled(true);
        sendButton.setText("Send request");
        if (result.error != null) {
            responseMeta.setText("Request blocked / failed · " + result.elapsedMs + " ms");
            responseMeta.setTextColor(DANGER);
            responseHeaders.setText("Connection could not be established.");
            responseBody.setText(result.error);
            addHistory(request, result);
            return;
        }
        boolean success = result.status >= 200 && result.status < 400;
        responseMeta.setText("HTTP " + result.status + "  ·  " + result.elapsedMs + " ms  ·  " + humanBytes(result.bytes));
        responseMeta.setTextColor(success ? MINT : DANGER);
        responseHeaders.setText(formatHeaders(result.headers));
        String responseText = result.body;
        if (result.truncated) responseText += "\n\n[Response truncated at 1 MiB for safe mobile viewing.]";
        responseBody.setText(TextUtils.isEmpty(responseText) ? "(No response body)" : responseText);
        addHistory(request, result);
    }

    private void clearResponse() {
        responseMeta.setText("Ready when you are");
        responseMeta.setTextColor(MUTED);
        responseHeaders.setText("Response headers will appear here.");
        responseBody.setText("Send a request to inspect its response body.");
    }

    private void showCollections() {
        LinearLayout content = dialogContent();
        TextView note = text("Saved requests stay on this device.", 13, MUTED, false);
        content.addView(note);
        Button save = actionButton("Save current request", BLUE, Color.WHITE);
        save.setOnClickListener(v -> showSaveRequestDialog());
        content.addView(save, margins(ViewGroup.LayoutParams.MATCH_PARENT, dp(46), 0, 12, 0, 10));
        JSONArray entries = getArray(KEY_COLLECTIONS);
        if (entries.length() == 0) {
            content.addView(emptyState("No saved requests yet", "Build a request, then save it here for reuse."));
        } else {
            for (int i = entries.length() - 1; i >= 0; i--) {
                try {
                    JSONObject item = entries.getJSONObject(i);
                    content.addView(collectionRow(item));
                } catch (JSONException ignored) { }
            }
        }
        showDialog("Collections", content);
    }

    private View collectionRow(final JSONObject item) {
        LinearLayout row = card();
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        TextView name = text(item.optString("name", "Untitled request"), 14, INK, true);
        row.addView(name);
        TextView address = text(item.optString("method", "GET") + "  " + item.optString("url", ""), 12, MUTED, false);
        address.setMaxLines(2);
        row.addView(address, margins(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 3, 0, 8));
        LinearLayout actions = horizontal();
        Button load = outlineButton("Load");
        load.setOnClickListener(v -> loadRequest(item));
        actions.addView(load, new LinearLayout.LayoutParams(0, dp(38), 1f));
        Button remove = outlineButton("Delete");
        remove.setTextColor(DANGER);
        remove.setOnClickListener(v -> deleteCollection(item.optLong("id", -1)));
        actions.addView(remove, margins(0, dp(38), 8, 0, 0, 0, 1f));
        row.addView(actions);
        return row;
    }

    private void showSaveRequestDialog() {
        final EditText name = input("Request name", false);
        name.setText(defaultRequestName());
        LinearLayout content = dialogContent();
        content.addView(text("Headers and request body are saved locally with this request.", 13, MUTED, false));
        content.addView(name, margins(ViewGroup.LayoutParams.MATCH_PARENT, dp(48), 0, 12, 0, 12));
        LinearLayout actions = horizontal();
        Button cancel = outlineButton("Cancel");
        Button save = actionButton("Save request", BLUE, Color.WHITE);
        actions.addView(cancel, new LinearLayout.LayoutParams(0, dp(44), 1f));
        actions.addView(save, margins(0, dp(44), 8, 0, 0, 0, 1.2f));
        content.addView(actions);
        final Dialog[] sheet = new Dialog[1];
        cancel.setOnClickListener(v -> sheet[0].dismiss());
        save.setOnClickListener(v -> {
            saveCurrentRequest(name.getText().toString().trim());
            sheet[0].dismiss();
        });
        sheet[0] = showSheet("Save request", content);
    }

    private void saveCurrentRequest(String name) {
        if (TextUtils.isEmpty(name)) {
            toast("Give the request a name before saving it.");
            return;
        }
        try {
            RequestSpec request = buildRequest();
            JSONArray existing = getArray(KEY_COLLECTIONS);
            JSONArray updated = new JSONArray();
            JSONObject item = new JSONObject();
            item.put("id", System.currentTimeMillis());
            item.put("name", name);
            item.put("method", request.method);
            item.put("url", request.url);
            item.put("body", request.body);
            item.put("headers", rowsToJson(request.headers));
            item.put("params", rowsToJson(readRows(paramsRows)));
            item.put("savedAt", System.currentTimeMillis());
            updated.put(item);
            for (int i = 0; i < existing.length() && i < 49; i++) updated.put(existing.getJSONObject(i));
            putArray(KEY_COLLECTIONS, updated);
            toast("Saved to collections.");
        } catch (Exception exception) {
            toast(exception.getMessage() == null ? "Could not save this request." : exception.getMessage());
        }
    }

    private void loadRequest(JSONObject item) {
        urlInput.setText(item.optString("url", ""));
        setMethod(item.optString("method", "GET"));
        bodyInput.setText(item.optString("body", ""));
        populateRows(headersRows, item.optJSONArray("headers"), "Header name", "Value");
        populateRows(paramsRows, item.optJSONArray("params"), "Parameter", "Value");
        selectTab("Headers");
        toast("Request loaded.");
    }

    private void deleteCollection(long id) {
        JSONArray existing = getArray(KEY_COLLECTIONS);
        JSONArray updated = new JSONArray();
        for (int i = 0; i < existing.length(); i++) {
            JSONObject value = existing.optJSONObject(i);
            if (value != null && value.optLong("id", -2) != id) updated.put(value);
        }
        putArray(KEY_COLLECTIONS, updated);
        toast("Saved request deleted.");
        showCollections();
    }

    private void showHistory() {
        LinearLayout content = dialogContent();
        JSONArray entries = getArray(KEY_HISTORY);
        if (entries.length() == 0) {
            content.addView(emptyState("No requests yet", "Your recent results will appear here."));
        } else {
            for (int i = 0; i < entries.length(); i++) {
                JSONObject item = entries.optJSONObject(i);
                if (item == null) continue;
                LinearLayout row = card();
                row.setPadding(dp(12), dp(10), dp(12), dp(10));
                int status = item.optInt("status", 0);
                TextView statusView = text(status == 0 ? "FAILED" : "HTTP " + status, 13, status >= 200 && status < 400 ? MINT : DANGER, true);
                row.addView(statusView);
                TextView detail = text(item.optString("method", "GET") + "  " + item.optString("url", "") + "\n" + item.optLong("elapsed", 0) + " ms", 12, MUTED, false);
                detail.setMaxLines(3);
                row.addView(detail, margins(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 3, 0, 0));
                content.addView(row, margins(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, 8));
            }
        }
        Button clear = outlineButton("Clear history");
        clear.setTextColor(DANGER);
        clear.setOnClickListener(v -> {
            putArray(KEY_HISTORY, new JSONArray());
            toast("History cleared.");
            showHistory();
        });
        content.addView(clear, margins(ViewGroup.LayoutParams.MATCH_PARENT, dp(42), 0, 6, 0, 0));
        showDialog("History", content);
    }

    private void addHistory(RequestSpec request, HttpResult result) {
        try {
            JSONArray previous = getArray(KEY_HISTORY);
            JSONArray next = new JSONArray();
            JSONObject entry = new JSONObject();
            entry.put("method", request.method);
            entry.put("url", request.url);
            entry.put("status", result.status);
            entry.put("elapsed", result.elapsedMs);
            entry.put("at", System.currentTimeMillis());
            next.put(entry);
            for (int i = 0; i < previous.length() && i < 49; i++) next.put(previous.getJSONObject(i));
            putArray(KEY_HISTORY, next);
        } catch (JSONException ignored) { }
    }

    private void showSettings() {
        LinearLayout content = dialogContent();
        TextView appearanceSection = text("APPEARANCE", 11, BLUE, true);
        appearanceSection.setLetterSpacing(.10f);
        content.addView(appearanceSection);
        content.addView(text("Choose an app-wide appearance. System default follows your device setting.", 12, MUTED, false), margins(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4, 0, 7));
        Spinner appearancePicker = new Spinner(this);
        String[] appearanceOptions = {"System default", "Light", "Dark"};
        ArrayAdapter<String> appearanceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, appearanceOptions);
        appearancePicker.setAdapter(appearanceAdapter);
        appearancePicker.setBackground(round(FIELD, dp(8), LINE));
        for (int i = 0; i < appearanceOptions.length; i++) {
            if (appearanceOptions[i].equals(currentThemeLabel())) {
                appearancePicker.setSelection(i);
                break;
            }
        }
        content.addView(appearancePicker, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        Button applyAppearance = outlineButton("Apply appearance");
        applyAppearance.setOnClickListener(v -> {
            String selected = themeKeyForLabel(appearancePicker.getSelectedItem().toString());
            if (selected.equals(prefs.getString(KEY_THEME, THEME_SYSTEM))) {
                toast("This appearance is already active.");
                return;
            }
            prefs.edit().putString(KEY_THEME, selected).apply();
            recreate();
        });
        content.addView(applyAppearance, margins(ViewGroup.LayoutParams.MATCH_PARENT, dp(42), 0, 8, 0, 20));

        TextView section = text("REQUEST ENVIRONMENT", 11, BLUE, true);
        section.setLetterSpacing(.10f);
        content.addView(section);
        content.addView(text("The {{base_url}} placeholder is expanded before each request.", 12, MUTED, false), margins(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4, 0, 7));
        EditText baseUrl = input("https://api.example.com", false);
        baseUrl.setSingleLine(true);
        baseUrl.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        baseUrl.setText(prefs.getString(KEY_BASE_URL, ""));
        content.addView(baseUrl, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        Button saveEnvironment = outlineButton("Save environment");
        saveEnvironment.setOnClickListener(v -> {
            prefs.edit().putString(KEY_BASE_URL, baseUrl.getText().toString().trim()).apply();
            toast("Base URL saved.");
        });
        content.addView(saveEnvironment, margins(ViewGroup.LayoutParams.MATCH_PARENT, dp(42), 0, 8, 0, 20));

        TextView privacySection = text("PRIVACY & DATA", 11, BLUE, true);
        privacySection.setLetterSpacing(.10f);
        content.addView(privacySection);
        content.addView(text("Review how requests and saved workspaces are handled on this device.", 12, MUTED, false), margins(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4, 0, 7));
        Button privacyPolicy = outlineButton("Privacy policy");
        privacyPolicy.setOnClickListener(v -> showPrivacyPolicy());
        content.addView(privacyPolicy, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42)));

        showDialog("Settings", content);
    }

    private void showPrivacyPolicy() {
        LinearLayout content = dialogContent();
        TextView intro = text("API Flow privacy summary", 15, INK, true);
        content.addView(intro);
        content.addView(text("Effective version: 1.0.2", 12, MUTED, false), margins(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 3, 0, 14));

        content.addView(policyBlock("Stored on this device", "Saved requests, request history, URLs, headers, query parameters, and request bodies are stored in Android app-private storage. Device backups are disabled. Avoid saving long-lived secrets in collections."));
        content.addView(policyBlock("Sent only when you choose", "When you tap Send, API Flow sends the request data you entered directly to the endpoint URL you selected. The app does not operate a relay service."), margins(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 12, 0, 0));
        content.addView(policyBlock("No developer-side collection", "This build has no account system, advertising, analytics, tracking SDK, or developer-operated backend. It does not send your saved workspace to the publisher."), margins(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 12, 0, 0));
        content.addView(policyBlock("Your controls", "Delete saved requests individually from Collections, or clear the complete request history. Clearing Android app storage removes all locally stored API Flow data."), margins(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 12, 0, 0));
        content.addView(text("Publisher: Jeffin Vinod K · Support: jeffin@posteo.net", 12, MUTED, false), margins(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 16, 0, 0));
        showDialog("Privacy policy", content);
    }

    private View policyBlock(String heading, String detail) {
        LinearLayout block = vertical();
        block.setPadding(dp(12), dp(11), dp(12), dp(11));
        block.setBackground(round(SUBTLE, dp(10)));
        block.addView(text(heading, 13, INK, true));
        block.addView(text(detail, 12, MUTED, false), margins(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4, 0, 0));
        return block;
    }

    private List<KeyValue> readRows(LinearLayout container) {
        List<KeyValue> values = new ArrayList<>();
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (!(child instanceof LinearLayout)) continue;
            LinearLayout row = (LinearLayout) child;
            if (row.getChildCount() < 2 || !(row.getChildAt(0) instanceof EditText) || !(row.getChildAt(1) instanceof EditText)) continue;
            String key = ((EditText) row.getChildAt(0)).getText().toString().trim();
            String value = ((EditText) row.getChildAt(1)).getText().toString();
            if (!TextUtils.isEmpty(key)) values.add(new KeyValue(key, value));
        }
        return values;
    }

    private JSONArray rowsToJson(List<KeyValue> values) throws JSONException {
        JSONArray array = new JSONArray();
        for (KeyValue value : values) {
            JSONObject item = new JSONObject();
            item.put("key", value.key);
            item.put("value", value.value);
            array.put(item);
        }
        return array;
    }

    private void populateRows(LinearLayout destination, JSONArray source, String keyHint, String valueHint) {
        destination.removeAllViews();
        if (source != null) {
            for (int i = 0; i < source.length(); i++) {
                JSONObject item = source.optJSONObject(i);
                if (item != null) addKeyValueRow(destination, item.optString("key", ""), item.optString("value", ""), keyHint, valueHint);
            }
        }
        if (destination.getChildCount() == 0 && destination == headersRows) {
            addKeyValueRow(destination, "Accept", "application/json", keyHint, valueHint);
        }
    }

    private JSONArray getArray(String key) {
        try {
            return new JSONArray(prefs.getString(key, "[]"));
        } catch (JSONException exception) {
            return new JSONArray();
        }
    }

    private void putArray(String key, JSONArray value) {
        prefs.edit().putString(key, value.toString()).apply();
    }

    private void setMethod(String method) {
        for (int i = 0; i < methodSpinner.getCount(); i++) {
            if (method.equals(methodSpinner.getItemAtPosition(i).toString())) {
                methodSpinner.setSelection(i);
                return;
            }
        }
        methodSpinner.setSelection(0);
    }

    private String defaultRequestName() {
        String url = urlInput.getText().toString().trim();
        if (TextUtils.isEmpty(url)) return methodSpinner.getSelectedItem().toString() + " request";
        try {
            return methodSpinner.getSelectedItem().toString() + " " + new URL(url).getHost();
        } catch (Exception ignored) {
            return methodSpinner.getSelectedItem().toString() + " request";
        }
    }

    private String expandVariables(String input) {
        String withBase = input.replace("{{base_url}}", prefs.getString(KEY_BASE_URL, ""));

        // If user enters just a path (e.g., "/v1/users"), auto-prepend base_url
        String normalized = withBase.toLowerCase(Locale.US);
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            String baseUrl = prefs.getString(KEY_BASE_URL, "").trim();
            if (!baseUrl.isEmpty()) {
                // Remove trailing slash from base URL to avoid double slashes
                if (baseUrl.endsWith("/")) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                }
                // Ensure path starts with /
                if (!withBase.startsWith("/")) {
                    withBase = "/" + withBase;
                }
                withBase = baseUrl + withBase;
            }
        }
        return withBase;
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (Exception ignored) {
            return value;
        }
    }

    private boolean allowsBody(String method) {
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method) || "DELETE".equals(method);
    }

    private ReadBody readBody(InputStream stream) throws IOException {
        try (InputStream input = stream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int count;
            boolean truncated = false;
            while ((count = input.read(buffer)) != -1) {
                int remaining = MAX_RESPONSE_BYTES - total;
                if (remaining <= 0) {
                    truncated = true;
                    break;
                }
                int take = Math.min(remaining, count);
                output.write(buffer, 0, take);
                total += take;
                if (take < count) {
                    truncated = true;
                    break;
                }
            }
            return new ReadBody(new String(output.toByteArray(), StandardCharsets.UTF_8), truncated, total);
        }
    }

    private String formatHeaders(Map<String, List<String>> headers) {
        if (headers == null || headers.isEmpty()) return "(No response headers)";
        StringBuilder output = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String key = entry.getKey() == null ? "Status" : entry.getKey();
            output.append(key).append(": ");
            output.append(entry.getValue() == null ? "" : TextUtils.join(", ", entry.getValue()));
            output.append('\n');
        }
        return output.toString().trim();
    }

    private String humanBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.US, "%.1f KiB", bytes / 1024f);
        return String.format(Locale.US, "%.1f MiB", bytes / (1024f * 1024f));
    }

    private void showDialog(String title, LinearLayout content) {
        showSheet(title, content);
    }

    /**
     * Uses a custom sheet instead of platform alert-dialog chrome so every secondary screen keeps
     * the same rounded, roomy visual language as the main workspace.
     */
    private Dialog showSheet(String title, LinearLayout content) {
        // A refresh or navigation action replaces the visible sheet instead of stacking another
        // dialog above it. This applies uniformly to history, collections, settings, and save.
        if (activeSheet != null && activeSheet.isShowing()) {
            Dialog previousSheet = activeSheet;
            activeSheet = null;
            previousSheet.dismiss();
        }
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setOnDismissListener(ignored -> {
            if (activeSheet == dialog) activeSheet = null;
        });

        LinearLayout sheet = vertical();
        sheet.setPadding(dp(18), dp(16), dp(18), dp(18));
        sheet.setBackground(round(SURFACE, dp(22), LINE));
        sheet.setElevation(dp(12));

        LinearLayout heading = horizontal();
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView titleView = text(title, 18, INK, true);
        heading.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button close = new Button(this);
        close.setText("×");
        close.setTextSize(22);
        close.setTextColor(MUTED);
        close.setContentDescription("Close " + title);
        close.setAllCaps(false);
        close.setPadding(0, 0, 0, 0);
        close.setBackground(round(SUBTLE, dp(16)));
        close.setOnClickListener(v -> dialog.dismiss());
        heading.addView(close, new LinearLayout.LayoutParams(dp(38), dp(38)));
        sheet.addView(heading);

        View divider = new View(this);
        divider.setBackgroundColor(LINE);
        sheet.addView(divider, margins(ViewGroup.LayoutParams.MATCH_PARENT, dp(1), 0, 12, 0, 12));

        CappedScrollView scroll = new CappedScrollView(this);
        scroll.setMaxHeight((int) (getResources().getDisplayMetrics().heightPixels * .66f));
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(true);
        scroll.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        sheet.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        dialog.setContentView(sheet);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        activeSheet = dialog;
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int maximumWidth = dp(560);
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            window.setLayout(Math.min(maximumWidth, screenWidth - dp(28)), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        return dialog;
    }

    private LinearLayout dialogContent() {
        LinearLayout content = vertical();
        content.setPadding(dp(2), dp(6), dp(2), dp(6));
        return content;
    }

    private View emptyState(String title, String detail) {
        LinearLayout box = vertical();
        box.setPadding(dp(14), dp(16), dp(14), dp(16));
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setBackground(round(SUBTLE, dp(10)));
        box.addView(text(title, 14, INK, true));
        box.addView(text(detail, 12, MUTED, false), margins(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4, 0, 0));
        return box;
    }

    private LinearLayout card() {
        LinearLayout card = vertical();
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackground(round(SURFACE, dp(14), LINE));
        return card;
    }

    private LinearLayout vertical() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout horizontal() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        return layout;
    }

    private EditText input(String hint, boolean multiline) {
        EditText field = new EditText(this);
        field.setTextSize(14);
        field.setTextColor(INK);
        field.setHintTextColor(MUTED);
        field.setHint(hint);
        field.setPadding(dp(11), dp(6), dp(11), dp(6));
        field.setBackground(round(FIELD, dp(8), LINE));
        if (!multiline) field.setSingleLine(true);
        return field;
    }

    private EditText compactInput(String hint) {
        EditText field = input(hint, false);
        field.setTextSize(12);
        field.setPadding(dp(8), 0, dp(8), 0);
        return field;
    }

    private Button actionButton(String label, int background, int foreground) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(foreground);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setBackground(round(background, dp(9)));
        return button;
    }

    private Button outlineButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(BLUE_DARK);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setBackground(round(SURFACE, dp(8), LINE));
        return button;
    }

    private Button chipButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setPadding(dp(14), 0, dp(14), 0);
        return button;
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private GradientDrawable round(int color, int radius) {
        return round(color, radius, Color.TRANSPARENT);
    }

    private GradientDrawable round(int color, int radius, int stroke) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(radius);
        if (stroke != Color.TRANSPARENT) shape.setStroke(dp(1), stroke);
        return shape;
    }

    private LinearLayout.LayoutParams margins(int width, int height, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private LinearLayout.LayoutParams margins(int width, int height, int left, int top, int right, int bottom, float weight) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height, weight);
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Icon glyphs from a font have uneven visual bounds. Drawing each toolbar symbol on the same
     * 20dp geometry gives Collections, History, and Settings a genuinely equal visual weight.
     */
    private static class ToolbarIconButton extends View {
        static final int COLLECTIONS = 0;
        static final int HISTORY = 1;
        static final int SETTINGS = 2;

        private final int icon;
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);

        ToolbarIconButton(Context context, int icon) {
            super(context);
            this.icon = icon;
            stroke.setColor(Color.WHITE);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeCap(Paint.Cap.ROUND);
            stroke.setStrokeJoin(Paint.Join.ROUND);
            fill.setColor(Color.WHITE);
            fill.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float density = getResources().getDisplayMetrics().density;
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            stroke.setStrokeWidth(2f * density);
            if (icon == COLLECTIONS) {
                drawCollections(canvas, cx, cy, density);
            } else if (icon == HISTORY) {
                drawHistory(canvas, cx, cy, density);
            } else {
                drawSettings(canvas, cx, cy, density);
            }
        }

        private void drawCollections(Canvas canvas, float cx, float cy, float d) {
            canvas.drawRoundRect(new RectF(cx - 10f * d, cy - 9f * d, cx + 6f * d, cy + 7f * d), 2f * d, 2f * d, stroke);
            canvas.drawRoundRect(new RectF(cx - 6f * d, cy - 6f * d, cx + 10f * d, cy + 10f * d), 2f * d, 2f * d, stroke);
            canvas.drawLine(cx - 2f * d, cy - 1f * d, cx + 6f * d, cy - 1f * d, stroke);
            canvas.drawLine(cx - 2f * d, cy + 4f * d, cx + 6f * d, cy + 4f * d, stroke);
        }

        private void drawHistory(Canvas canvas, float cx, float cy, float d) {
            float radius = 10f * d;
            canvas.drawCircle(cx, cy, radius, stroke);
            canvas.drawLine(cx, cy, cx, cy - 5.5f * d, stroke);
            canvas.drawLine(cx, cy, cx + 4.5f * d, cy + 3f * d, stroke);
            canvas.drawCircle(cx, cy, 1.15f * d, fill);
        }

        private void drawSettings(Canvas canvas, float cx, float cy, float d) {
            Path gear = new Path();
            for (int point = 0; point < 16; point++) {
                double angle = -Math.PI / 2d + point * Math.PI / 8d;
                float radius = (point % 2 == 0 ? 10f : 8f) * d;
                float x = cx + (float) Math.cos(angle) * radius;
                float y = cy + (float) Math.sin(angle) * radius;
                if (point == 0) gear.moveTo(x, y); else gear.lineTo(x, y);
            }
            gear.close();
            canvas.drawPath(gear, stroke);
            canvas.drawCircle(cx, cy, 3.75f * d, stroke);
        }
    }

    /**
     * Prevents the page-level ScrollView from stealing a drag that began inside a scrollable
     * response. This keeps long API payloads controllable exactly where the user touches them.
     */
    private static class InnerScrollView extends ScrollView {
        InnerScrollView(Context context) {
            super(context);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                requestParentInterception(false);
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                requestParentInterception(true);
            }
            return super.dispatchTouchEvent(event);
        }

        private void requestParentInterception(boolean allow) {
            ViewParent parent = getParent();
            while (parent != null) {
                parent.requestDisallowInterceptTouchEvent(!allow);
                parent = parent.getParent();
            }
        }
    }

    /** Caps modal content so a tall settings or history sheet scrolls within its rounded frame. */
    private static class CappedScrollView extends ScrollView {
        private int maxHeight = Integer.MAX_VALUE;

        CappedScrollView(Context context) {
            super(context);
        }

        void setMaxHeight(int maxHeight) {
            this.maxHeight = maxHeight;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int cappedHeight = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST);
            super.onMeasure(widthMeasureSpec, cappedHeight);
        }
    }

    private static final class KeyValue {
        final String key;
        final String value;
        KeyValue(String key, String value) { this.key = key; this.value = value; }
    }

    private static final class RequestSpec {
        final String method;
        final String url;
        final List<KeyValue> headers;
        final String body;
        RequestSpec(String method, String url, List<KeyValue> headers, String body) {
            this.method = method;
            this.url = url;
            this.headers = headers;
            this.body = body;
        }
    }

    private static final class ReadBody {
        final String text;
        final boolean truncated;
        final int bytes;
        ReadBody(String text, boolean truncated, int bytes) { this.text = text; this.truncated = truncated; this.bytes = bytes; }
    }

    private static final class HttpResult {
        final int status;
        final long elapsedMs;
        final String body;
        final boolean truncated;
        final long bytes;
        final Map<String, List<String>> headers;
        final String error;
        HttpResult(int status, long elapsedMs, String body, boolean truncated, long bytes, Map<String, List<String>> headers, String error) {
            this.status = status;
            this.elapsedMs = elapsedMs;
            this.body = body;
            this.truncated = truncated;
            this.bytes = bytes;
            this.headers = headers == null ? new LinkedHashMap<>() : headers;
            this.error = error;
        }
    }
}
