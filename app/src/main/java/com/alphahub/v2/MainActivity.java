package com.alphahub.v2;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Standard in-app host for Alpha Hub V2.
 *
 * The launcher owns navigation and UI. It deliberately does not start the legacy
 * overlay service or request SYSTEM_ALERT_WINDOW permission.
 */
public class MainActivity extends Activity {
    private static final String PREFS = "alpha_hub_preferences";
    private static final String KEY_SHORTCUTS = "home_shortcuts";
    private static final String HOME = "home";
    private static final String TOOLS = "tools";
    private static final String SETTINGS_HUB = "settings_hub";
    private static final String SHORTCUTS = "shortcuts";

    private static final int BG = Color.rgb(5, 10, 27);
    private static final int PANEL = Color.rgb(12, 23, 49);
    private static final int PANEL_ALT = Color.rgb(17, 32, 65);
    private static final int CYAN = Color.rgb(0, 220, 255);
    private static final int BLUE = Color.rgb(62, 139, 255);
    private static final int WHITE = Color.rgb(245, 249, 255);
    private static final int MUTED = Color.rgb(158, 178, 211);

    private final ArrayDeque<String> backStack = new ArrayDeque<>();
    private String currentScreen = HOME;
    private LinearLayout root;
    private LinearLayout body;

    /** The single catalogue used by the Tools page and shortcut editor. */
    private static final List<Tool> TOOL_CATALOG = Arrays.asList(
            new Tool("Screen Translation", "Translate text using Google Translate", "translate"),
            new Tool("Wi-Fi Settings", "Open Android Wi-Fi controls", "wifi"),
            new Tool("Bluetooth Settings", "Open Android Bluetooth controls", "bluetooth"),
            new Tool("Mobile Network", "Open Android network settings", "mobile"),
            new Tool("App Settings", "Manage Alpha Hub permissions", "app_settings")
    );

    private static final class Tool {
        final String title;
        final String subtitle;
        final String action;

        Tool(String title, String subtitle, String action) {
            this.title = title;
            this.subtitle = subtitle;
            this.action = action;
        }
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        render(HOME, false);
    }

    private void navigate(String destination) {
        if (destination == null || destination.equals(currentScreen)) {
            render(currentScreen, false);
            return;
        }
        backStack.push(currentScreen);
        render(destination, false);
    }

    private void render(String screen, boolean ignored) {
        currentScreen = screen;
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(18), dp(14), dp(18), dp(10));

        root.addView(makeHeader(screen), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(0, dp(10), 0, dp(14));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        if (HOME.equals(screen)) buildHome();
        else if (TOOLS.equals(screen)) buildTools();
        else if (SETTINGS_HUB.equals(screen)) buildSettingsHub();
        else if (SHORTCUTS.equals(screen)) buildShortcutEditor();
        else buildHome();

        root.addView(makeBottomNav(), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56)));
        setContentView(root);
    }

    private View makeHeader(String screen) {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout titleWrap = new LinearLayout(this);
        titleWrap.setOrientation(LinearLayout.VERTICAL);
        TextView eyebrow = text("ALPHA HUB V2", 11, CYAN, true);
        TextView title = text(titleFor(screen), 25, WHITE, true);
        titleWrap.addView(eyebrow);
        titleWrap.addView(title);
        header.addView(titleWrap, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button star = smallButton("★");
        star.setContentDescription("Open Smart Settings Hub");
        star.setOnClickListener(v -> navigate(SETTINGS_HUB));
        header.addView(star, new LinearLayout.LayoutParams(dp(46), dp(46)));
        Button gear = smallButton("⚙");
        gear.setContentDescription("Open Smart Settings Hub");
        gear.setOnClickListener(v -> navigate(SETTINGS_HUB));
        LinearLayout.LayoutParams gearParams = new LinearLayout.LayoutParams(dp(46), dp(46));
        gearParams.leftMargin = dp(8);
        header.addView(gear, gearParams);
        return header;
    }

    private String titleFor(String screen) {
        if (TOOLS.equals(screen)) return "Convenient Tools";
        if (SETTINGS_HUB.equals(screen)) return "Smart Settings Hub";
        if (SHORTCUTS.equals(screen)) return "Manage Shortcuts";
        return "Home";
    }

    private void buildHome() {
        addHero("Your everyday tools, in one place",
                "Open a tool, manage shortcuts, or jump into Smart Settings Hub.");
        addSectionTitle("YOUR SHORTCUTS");
        Set<String> shortcuts = getSavedShortcuts();
        if (shortcuts.isEmpty()) {
            addCard("No shortcuts yet", "Choose tools and pin the ones you use most.",
                    "Manage Shortcuts", () -> navigate(SHORTCUTS));
        } else {
            for (String action : shortcuts) {
                Tool tool = findTool(action);
                if (tool != null) addCard(tool.title, tool.subtitle, "Open", () -> runTool(tool));
            }
        }
        addCard("Convenient Tools", "Translation and quick links to phone settings.",
                "Open Tools", () -> navigate(TOOLS));
        addCard("Smart Settings Hub", "One place for settings and shortcut management.",
                "Open More Features", () -> navigate(SETTINGS_HUB));
        addCard("Manage Shortcuts", "Choose which tools appear on Home.",
                "Edit", () -> navigate(SHORTCUTS));
    }

    private void buildTools() {
        addHero("Useful tools, without floating overlays",
                "These tools open in Alpha Hub or hand off to the relevant Android screen.");
        for (Tool tool : TOOL_CATALOG) {
            addCard(tool.title, tool.subtitle, "Open", () -> runTool(tool));
        }
        addCard("Pin your favorites", "Choose which tools appear on the Home page.",
                "Manage Shortcuts", () -> navigate(SHORTCUTS));
        addCard("More features", "Open the shared Smart Settings Hub.",
                "Open More Features", () -> navigate(SETTINGS_HUB));
    }

    private void buildSettingsHub() {
        addHero("Smart Settings Hub",
                "This is the shared destination for the Home star, gear, and More Features.");
        addCard("Manage Shortcuts", "Select tools to show on Home. Changes are saved only when you tap Done.",
                "Manage", () -> navigate(SHORTCUTS));
        addCard("Convenient Tools", "Browse the shared Tool Catalog.",
                "Browse", () -> navigate(TOOLS));
        addCard("Android App Settings", "Open Alpha Hub's system app settings.",
                "Open Settings", () -> openSystemAction("app_settings"));
        addCard("Screen Translation", "Open the translation tool.",
                "Translate", () -> runTool(findTool("translate")));
    }

    private void buildShortcutEditor() {
        addHero("Choose Home shortcuts",
                "Toggle the tools you want, then tap Done to save. Cancel discards this draft.");
        Set<String> draft = new LinkedHashSet<>(getSavedShortcuts());
        TextView hint = text("Changes are not saved until Done.", 13, MUTED, false);
        hint.setPadding(dp(2), dp(4), dp(2), dp(12));
        body.addView(hint);

        List<Button> toggles = new ArrayList<>();
        for (Tool tool : TOOL_CATALOG) {
            Button toggle = new Button(this);
            updateToggle(toggle, tool, draft.contains(tool.action));
            toggle.setAllCaps(false);
            toggle.setOnClickListener(v -> {
                if (draft.contains(tool.action)) draft.remove(tool.action);
                else draft.add(tool.action);
                updateToggle(toggle, tool, draft.contains(tool.action));
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = dp(8);
            body.addView(toggle, params);
            toggles.add(toggle);
        }

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button cancel = actionButton("Cancel", false);
        cancel.setOnClickListener(v -> goBack());
        Button done = actionButton("Done", true);
        done.setOnClickListener(v -> {
            saveShortcuts(draft);
            Toast.makeText(this, "Shortcuts saved", Toast.LENGTH_SHORT).show();
            goBack();
        });
        actions.addView(cancel, new LinearLayout.LayoutParams(0, dp(50), 1f));
        LinearLayout.LayoutParams doneParams = new LinearLayout.LayoutParams(0, dp(50), 1f);
        doneParams.leftMargin = dp(10);
        actions.addView(done, doneParams);
        body.addView(actions);
    }

    private void updateToggle(Button button, Tool tool, boolean selected) {
        button.setText((selected ? "✓  " : "＋  ") + tool.title + "\n" + tool.subtitle);
        button.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        button.setTextColor(selected ? CYAN : WHITE);
        button.setTextSize(14);
        button.setPadding(dp(14), dp(10), dp(14), dp(10));
        button.setBackground(background(selected ? PANEL_ALT : PANEL,
                selected ? CYAN : Color.rgb(45, 65, 103), 14));
    }

    private void addHero(String heading, String description) {
        LinearLayout card = panel();
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        TextView title = text(heading, 19, WHITE, true);
        TextView detail = text(description, 13, MUTED, false);
        detail.setPadding(0, dp(8), 0, 0);
        card.addView(title);
        card.addView(detail);
        addPanel(card);
    }

    private void addSectionTitle(String value) {
        TextView label = text(value, 12, CYAN, true);
        label.setPadding(dp(3), dp(12), dp(3), dp(8));
        body.addView(label);
    }

    private void addCard(String title, String description, String actionLabel, Runnable action) {
        LinearLayout card = panel();
        card.setPadding(dp(15), dp(14), dp(12), dp(14));
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.addView(text(title, 16, WHITE, true));
        TextView detail = text(description, 12, MUTED, false);
        detail.setPadding(0, dp(5), dp(8), 0);
        copy.addView(detail);
        row.addView(copy, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button actionButton = actionButton(actionLabel, true);
        actionButton.setTextSize(12);
        actionButton.setPadding(dp(10), 0, dp(10), 0);
        actionButton.setOnClickListener(v -> action.run());
        row.addView(actionButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(42)));
        card.addView(row);
        card.setOnClickListener(v -> action.run());
        addPanel(card);
    }

    private LinearLayout makeBottomNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackground(background(PANEL, Color.rgb(33, 54, 93), 18));
        String[] labels = {"Home", "Tools", "Settings"};
        String[] screens = {HOME, TOOLS, SETTINGS_HUB};
        for (int i = 0; i < labels.length; i++) {
            final String destination = screens[i];
            Button item = new Button(this);
            item.setText(labels[i]);
            item.setAllCaps(false);
            item.setTextSize(12);
            item.setTextColor(currentScreen.equals(destination) ? CYAN : MUTED);
            item.setBackgroundColor(Color.TRANSPARENT);
            item.setOnClickListener(v -> navigate(destination));
            nav.addView(item, new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.MATCH_PARENT, 1f));
        }
        return nav;
    }

    private void runTool(Tool tool) {
        if (tool == null) return;
        if ("translate".equals(tool.action)) {
            openUrl("https://translate.google.com/?sl=auto&tl=en&op=translate");
        } else {
            openSystemAction(tool.action);
        }
    }

    private void openSystemAction(String action) {
        Intent intent;
        if ("wifi".equals(action)) {
            intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
        } else if ("bluetooth".equals(action)) {
            intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
        } else if ("mobile".equals(action)) {
            intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        } else if ("app_settings".equals(action)) {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
        } else {
            return;
        }
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "This settings screen is not available on this device.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No browser is available to open this tool.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private Set<String> getSavedShortcuts() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String saved = prefs.getString(KEY_SHORTCUTS, "translate,wifi,bluetooth");
        Set<String> result = new LinkedHashSet<>();
        if (saved != null && !saved.trim().isEmpty()) {
            for (String action : saved.split(",")) {
                if (findTool(action) != null) result.add(action);
            }
        }
        return result;
    }

    private void saveShortcuts(Set<String> actions) {
        StringBuilder value = new StringBuilder();
        for (String action : actions) {
            if (findTool(action) == null) continue;
            if (value.length() > 0) value.append(',');
            value.append(action);
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(KEY_SHORTCUTS, value.toString()).apply();
    }

    private Tool findTool(String action) {
        if (action == null) return null;
        for (Tool tool : TOOL_CATALOG) {
            if (tool.action.equals(action)) return tool;
        }
        return null;
    }

    private void goBack() {
        if (!backStack.isEmpty()) {
            render(backStack.pop(), false);
        } else if (!HOME.equals(currentScreen)) {
            render(HOME, false);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onBackPressed() {
        goBack();
    }

    private LinearLayout panel() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(background(PANEL, Color.rgb(34, 55, 95), 17));
        return card;
    }

    private void addPanel(View panel) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(10);
        body.addView(panel, params);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button smallButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(18);
        button.setTextColor(CYAN);
        button.setAllCaps(false);
        button.setPadding(0, 0, 0, 0);
        button.setBackground(background(PANEL_ALT, Color.rgb(42, 77, 134), 15));
        return button;
    }

    private Button actionButton(String label, boolean primary) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(primary ? Color.rgb(4, 14, 32) : WHITE);
        button.setTextSize(14);
        button.setBackground(background(primary ? CYAN : PANEL_ALT,
                primary ? CYAN : Color.rgb(53, 76, 118), 13));
        return button;
    }

    private GradientDrawable background(int color, int strokeColor, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
