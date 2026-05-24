package com.winlator.fusion;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.fusion.container.Container;
import com.winlator.fusion.container.ContainerManager;
import com.winlator.fusion.container.Shortcut;
import com.winlator.fusion.contentdialog.ContentDialog;
import com.winlator.fusion.contentdialog.CreateFolderDialog;
import com.winlator.fusion.contentdialog.ShortcutSettingsDialog;
import com.winlator.fusion.core.AppUtils;
import com.winlator.fusion.core.ArrayUtils;
import com.winlator.fusion.core.ExeIconExtractor;
import com.winlator.fusion.core.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

public class ShortcutsFragment extends BaseFileManagerFragment<Shortcut> {
    private static final String STEAMGRID_BASE_URL = "https://www.steamgriddb.com/api/v2/";
    private static String STEAMGRID_API_KEY = "0324c52513634547a7b32d6d323635d0";
    private Shortcut shortcutForIconUpdate;
    private ActivityResultLauncher<String> iconPickerLauncher;
    private final java.util.Set<String> inFlightFetches = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        iconPickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null && shortcutForIconUpdate != null) updateShortcutIcon(uri, shortcutForIconUpdate);
        });
        viewStyle = ViewStyle.valueOf(preferences.getString("shortcuts_view_style", "GRID"));
    }

    @Override
    public void refreshContent() {
        super.refreshContent();

        Shortcut selectedFolder = !folderStack.isEmpty() ? folderStack.peek() : null;
        ArrayList<Shortcut> shortcuts = manager.loadShortcuts(selectedFolder);
        recyclerView.setAdapter(new ShortcutsAdapter(shortcuts));
        emptyTextView.setVisibility(shortcuts.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.shortcuts_menu, menu);
        refreshViewStyleMenuItem(menu.findItem(R.id.menu_item_view_style));
    }

    private void createFolder() {
        clearClipboard();
        if (manager.getContainers().isEmpty()) return;
        CreateFolderDialog createFolderDialog = new CreateFolderDialog(manager);
        createFolderDialog.setOnCreateFolderListener((container, name) -> {
            File userDir = container.getUserDir();
            File desktopDir = userDir != null ? new File(userDir, "Desktop") : null;
            File parent = !folderStack.isEmpty() ? folderStack.peek().file : desktopDir;
            if (parent == null) return;
            File file = new File(parent, name);
            if (file.isDirectory()) {
                AppUtils.showToast(getContext(), R.string.there_already_file_with_that_name);
            }
            else {
                file.mkdir();
                refreshContent();
            }
        });
        createFolderDialog.show();
    }

    @Override
    protected void pasteFiles() {
        if (folderStack.isEmpty()) {
            clearClipboard();
            AppUtils.showToast(getContext(), R.string.you_cannot_paste_files_here);
            return;
        }

        clipboard.targetDir = folderStack.peek().file;
        super.pasteFiles();
    }

    private void instantiateClipboard(Shortcut shortcut, boolean cutMode) {
        clearClipboard();
        File linkFile = shortcut.getLinkFile();
        File[] files = {new File(shortcut.file.getParentFile(), shortcut.file.getName())};
        if (shortcut.file.isFile()) files = ArrayUtils.concat(files, new File[]{new File(linkFile.getParentFile(), linkFile.getName())});

        clipboard = new Clipboard(files, cutMode);
        pasteButton.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.menu_item_view_style) {
            setViewStyle(viewStyle == ViewStyle.GRID ? ViewStyle.LIST : ViewStyle.GRID);
            preferences.edit().putString("shortcuts_view_style", viewStyle.name()).apply();
            refreshViewStyleMenuItem(menuItem);
            return true;
        }
        else if (itemId == R.id.menu_item_new_folder) {
            createFolder();
            return true;
        }
        else return super.onOptionsItemSelected(menuItem);
    }

    @Override
    protected String getHomeTitle() {
        return getString(R.string.shortcuts);
    }

    private File getImagesDir(boolean isCover) {
        File targetDir = new File(Environment.getExternalStorageDirectory(), isCover ? "Winlator/covers" : "Winlator/icons");
        if (!targetDir.exists()) targetDir.mkdirs();
        File nomedia = new File(targetDir, ".nomedia");
        if (!nomedia.exists()) { try { nomedia.createNewFile(); } catch (IOException e) {} }
        return targetDir;
    }

    private void updateShortcutIcon(Uri sourceUri, Shortcut shortcut) {
        try {
            File targetDir = getImagesDir(false);
            String baseName = FileUtils.getBasename(shortcut.file.getPath());
            File destFile = new File(targetDir, baseName + ".user.png");
            try (InputStream is = getContext().getContentResolver().openInputStream(sourceUri);
                 OutputStream os = new FileOutputStream(destFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) > 0) os.write(buffer, 0, length);
            }
            if (recyclerView.getAdapter() != null) recyclerView.getAdapter().notifyDataSetChanged();
        } catch (Exception e) {}
    }

    private File resolveExeFile(Shortcut item) {
        if (item.path == null || item.path.isEmpty()) return null;
        String path = item.path.replace("\\", "/").trim();
        if (path.startsWith("\"") && path.endsWith("\"")) path = path.substring(1, path.length() - 1);
        if (path.startsWith("/")) { File f = new File(path); if (f.exists()) return f; }
        String normalized = path.replaceAll("^[A-Za-z]:/", "");
        File containerRoot = item.container.getRootDir();
        if (containerRoot != null) { File candidate = new File(containerRoot, ".wine/drive_c/" + normalized); if (candidate.exists()) return candidate; }
        return null;
    }

    private String resolveApiKey() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        if (prefs.getBoolean("enable_custom_api_key", false)) {
            String custom = prefs.getString("custom_api_key", "");
            if (custom != null && !custom.isEmpty()) return custom;
        }
        return STEAMGRID_API_KEY;
    }

    private void fetchCoverFromSteamGrid(Shortcut shortcut, File destFile, Runnable onSuccess, Runnable onFail) {
        String fetchKey = shortcut.name;
        if (!inFlightFetches.add(fetchKey)) return;
        String apiKey = resolveApiKey();
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String searchUrl = STEAMGRID_BASE_URL + "search/autocomplete/" + java.net.URLEncoder.encode(shortcut.name, "UTF-8");
                HttpURLConnection conn = (HttpURLConnection) new URL(searchUrl).openConnection();
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.connect();
                String response;
                try (InputStream is = conn.getInputStream()) { response = new String(is.readAllBytes()); }
                conn.disconnect();
                org.json.JSONObject json = new org.json.JSONObject(response);
                org.json.JSONArray data = json.optJSONArray("data");
                if (data == null || data.length() == 0) { if (onFail != null) onFail.run(); return; }
                int gameId = data.getJSONObject(0).getInt("id");
                String gridUrl = STEAMGRID_BASE_URL + "grids/game/" + gameId + "?dimensions=600x900&types=static";
                HttpURLConnection gridConn = (HttpURLConnection) new URL(gridUrl).openConnection();
                gridConn.setRequestProperty("Authorization", "Bearer " + apiKey);
                gridConn.connect();
                String gridResponse;
                try (InputStream is = gridConn.getInputStream()) { gridResponse = new String(is.readAllBytes()); }
                gridConn.disconnect();
                org.json.JSONObject gridJson = new org.json.JSONObject(gridResponse);
                org.json.JSONArray gridData = gridJson.optJSONArray("data");
                if (gridData == null || gridData.length() == 0) { if (onFail != null) onFail.run(); return; }
                String imageUrl = gridData.getJSONObject(0).getString("url");
                HttpURLConnection imgConn = (HttpURLConnection) new URL(imageUrl).openConnection();
                imgConn.connect();
                Bitmap bmp = BitmapFactory.decodeStream(imgConn.getInputStream());
                if (bmp == null) { if (onFail != null) onFail.run(); return; }
                if (destFile.getParentFile() != null) destFile.getParentFile().mkdirs();
                try (FileOutputStream fos = new FileOutputStream(destFile)) { bmp.compress(Bitmap.CompressFormat.PNG, 100, fos); }
                bmp.recycle();
                if (onSuccess != null) onSuccess.run();
                inFlightFetches.remove(fetchKey);
            } catch (Exception e) {
                inFlightFetches.remove(fetchKey);
                if (onFail != null) onFail.run();
            }
        });
    }

    private void addShortcutToScreen(Shortcut shortcut) {
        ShortcutManager shortcutManager = getContext().getSystemService(ShortcutManager.class);
        if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported()) {
            File iconDir = getImagesDir(false);
            String baseName = FileUtils.getBasename(shortcut.file.getPath());
            File imgFile = new File(iconDir, baseName + ".png");
            Bitmap bmp = imgFile.exists() ? BitmapFactory.decodeFile(imgFile.getPath()) : shortcut.icon;
            if (bmp == null) bmp = BitmapFactory.decodeResource(getResources(), R.drawable.icon_wine);
            Intent intent = new Intent(getActivity(), XServerDisplayActivity.class);
            intent.setAction(Intent.ACTION_VIEW);
            intent.putExtra("container_id", shortcut.container.id);
            intent.putExtra("shortcut_path", shortcut.file.getPath());
            ShortcutInfo shortcutInfo = new ShortcutInfo.Builder(getActivity(), shortcut.getExtra("uuid", String.valueOf(shortcut.file.hashCode())))
                    .setShortLabel(shortcut.name)
                    .setLongLabel(shortcut.name)
                    .setIcon(Icon.createWithBitmap(bmp))
                    .setIntent(intent)
                    .build();
            shortcutManager.requestPinShortcut(shortcutInfo, null);
        }
    }

    private void exportShortcut(Shortcut shortcut) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String uriString = sharedPreferences.getString("shortcuts_export_path_uri", null);
        File shortcutsDir;
        if (uriString != null) {
            Uri folderUri = Uri.parse(uriString);
            shortcutsDir = new File(FileUtils.getFilePathFromUri(folderUri));
        } else shortcutsDir = new File(SettingsFragment.DEFAULT_SHORTCUT_EXPORT_PATH);
        if (!shortcutsDir.exists() && !shortcutsDir.mkdirs()) return;
        File exportFile = new File(shortcutsDir, shortcut.file.getName());
        try {
            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(shortcut.file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("container_id:")) lines.add("container_id:" + shortcut.container.id);
                    else lines.add(line);
                }
            }
            boolean hasContainerId = false;
            for (String l : lines) { if (l.startsWith("container_id:")) { hasContainerId = true; break; } }
            if (!hasContainerId) lines.add("container_id:" + shortcut.container.id);
            try (FileWriter writer = new FileWriter(exportFile, false)) {
                for (String line : lines) writer.write(line + "\n");
                writer.flush();
            }
            AppUtils.showToast(getContext(), exportFile.getAbsolutePath());
        } catch (IOException e) {}
    }

    private android.graphics.Bitmap decodeSampledBitmap(String path, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private class ShortcutsAdapter extends RecyclerView.Adapter<ShortcutsAdapter.ViewHolder> {
        private final List<Shortcut> data;

        private class ViewHolder extends RecyclerView.ViewHolder {
            private final ImageView runButton;
            private final ImageView menuButton;
            private final ImageView imageView;
            private final TextView title;
            private final TextView subtitle;

            private ViewHolder(View view) {
                super(view);
                this.imageView = view.findViewById(R.id.ImageView);
                this.title = view.findViewById(R.id.TVTitle);
                this.subtitle = view.findViewById(R.id.TVSubtitle);
                this.runButton = view.findViewById(R.id.BTRun);
                this.menuButton = view.findViewById(R.id.BTMenu);
            }
        }

        public ShortcutsAdapter(List<Shortcut> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int resource = viewStyle == ViewStyle.LIST ? R.layout.file_list_item : R.layout.file_grid_item;
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(resource, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            final Shortcut item = data.get(position);

            if (item.icon == null) {
                int iconResId = item.file.isDirectory() ? R.drawable.container_folder : R.drawable.container_file_link;
                holder.imageView.setImageResource(iconResId);
            }
            else holder.imageView.setImageBitmap(item.icon);

            if (!item.file.isDirectory()) {
                String baseName = FileUtils.getBasename(item.file.getPath());
                File userIcon = new File(getImagesDir(false), baseName + ".user.png");
                File coverFile = new File(getImagesDir(true), baseName + ".png");
                File autoIcon = new File(getImagesDir(false), baseName + ".png");

                if (userIcon.exists()) {
                    holder.imageView.setImageBitmap(decodeSampledBitmap(userIcon.getPath(), 256, 256));
                } else if (coverFile.exists()) {
                    holder.imageView.setImageBitmap(decodeSampledBitmap(coverFile.getPath(), 256, 256));
                } else if (autoIcon.exists()) {
                    holder.imageView.setImageBitmap(decodeSampledBitmap(autoIcon.getPath(), 256, 256));
                } else {
                    int adapterPosition = holder.getAdapterPosition();
                    fetchCoverFromSteamGrid(item, coverFile,
                        () -> { if (getActivity() != null) getActivity().runOnUiThread(() -> {
                            if (holder.getAdapterPosition() != RecyclerView.NO_POSITION && holder.getAdapterPosition() == adapterPosition && coverFile.exists())
                                holder.imageView.setImageBitmap(decodeSampledBitmap(coverFile.getPath(), 256, 256));
                        }); },
                        () -> {
                            File exeFile = resolveExeFile(item);
                            if (exeFile != null) ExeIconExtractor.extractAsync(exeFile, autoIcon, false, () -> {
                                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                                    if (holder.getAdapterPosition() != RecyclerView.NO_POSITION && holder.getAdapterPosition() == adapterPosition && autoIcon.exists())
                                        holder.imageView.setImageBitmap(decodeSampledBitmap(autoIcon.getPath(), 256, 256));
                                });
                            });
                        });
                }
            }

            holder.title.setText(item.name);

            String playtimeStr = item.getExtra("playtime", "0");
            String subtitleText = item.container.getName();
            if (!playtimeStr.equals("0")) {
                long playtimeMs = 0;
                try {
                    playtimeMs = Long.parseLong(playtimeStr);
                } catch (NumberFormatException ignored) {}
                long playtimeMin = playtimeMs / 60000;
                if (playtimeMin > 0) {
                    long hours = playtimeMin / 60;
                    long mins = playtimeMin % 60;
                    if (hours > 0) {
                        subtitleText += " - " + hours + "h " + mins + "m";
                    } else {
                        subtitleText += " - " + mins + "m";
                    }
                }
            }
            holder.subtitle.setText(subtitleText);

            if (item.file.isDirectory()) {
                holder.runButton.setImageResource(R.drawable.icon_open);
            }
            else holder.runButton.setImageResource(R.drawable.icon_run);

            holder.imageView.setOnClickListener((v) -> runFromShortcut(item));
            holder.runButton.setOnClickListener((v) -> runFromShortcut(item));
            holder.menuButton.setOnClickListener((v) -> showListItemMenu(v, item));
        }

        @Override
        public final int getItemCount() {
            return data.size();
        }

        private void showListItemMenu(View anchorView, final Shortcut shortcut) {
            final Context context = getContext();
            PopupMenu listItemMenu = new PopupMenu(context, anchorView);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) listItemMenu.setForceShowIcon(true);

            listItemMenu.inflate(R.menu.file_manager_popup_menu);

            Menu menu = listItemMenu.getMenu();
            menu.findItem(R.id.menu_item_rename).setVisible(false);
            menu.findItem(R.id.menu_item_add_favorite).setVisible(false);
            menu.findItem(R.id.menu_item_info).setVisible(false);
            menu.add(0, R.id.shortcut_change_icon, 0, R.string.shortcut_change_icon);
            menu.add(0, R.id.shortcut_clone_to_container, 0, R.string.shortcut_clone_to_container);
            menu.add(0, R.id.shortcut_add_to_home_screen, 0, R.string.shortcut_add_to_home_screen);
            menu.add(0, R.id.shortcut_export, 0, R.string.shortcut_export);

            listItemMenu.setOnMenuItemClickListener((menuItem) -> {
                int id = menuItem.getItemId();
                if (id == R.id.menu_item_settings) {
                    clearClipboard();
                    (new ShortcutSettingsDialog(ShortcutsFragment.this, shortcut)).show();
                } else if (id == R.id.menu_item_copy || id == R.id.menu_item_cut) {
                    instantiateClipboard(shortcut, id == R.id.menu_item_cut);
                } else if (id == R.id.menu_item_remove) {
                    clearClipboard();
                    ContentDialog.confirm(context, R.string.do_you_want_to_remove_this_file, () -> {
                        shortcut.remove();
                        refreshContent();
                    });
                }
                else if (id == R.id.shortcut_change_icon) {
                    shortcutForIconUpdate = shortcut;
                    iconPickerLauncher.launch("image/*");
                }
                else if (id == R.id.shortcut_clone_to_container) {
                    ContainerManager containerManager = new ContainerManager(context);
                    ArrayList<Container> containers = containerManager.getContainers();
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle(R.string.select_variant);
                    String[] containerNames = new String[containers.size()];
                    for (int i = 0; i < containers.size(); i++) containerNames[i] = containers.get(i).getName();
                    builder.setItems(containerNames, (dialog, which) -> {
                        if (shortcut.cloneToContainer(containers.get(which))) refreshContent();
                    });
                    builder.show();
                }
                else if (id == R.id.shortcut_add_to_home_screen) {
                    addShortcutToScreen(shortcut);
                }
                else if (id == R.id.shortcut_export) {
                    exportShortcut(shortcut);
                }
                return true;
            });
            listItemMenu.show();
        }

        private void runFromShortcut(Shortcut shortcut) {
            AppCompatActivity activity = (AppCompatActivity)getActivity();

            if (shortcut.file.isDirectory()) {
                folderStack.push(shortcut);
                refreshContent();

                ActionBar actionBar = activity.getSupportActionBar();
                actionBar.setHomeAsUpIndicator(R.drawable.icon_action_bar_back);
                actionBar.setTitle(shortcut.name);
            }
            else {
                Intent intent = new Intent(activity, XServerDisplayActivity.class);
                intent.putExtra("container_id", shortcut.container.id);
                intent.putExtra("shortcut_path", shortcut.file.getPath());
                activity.startActivity(intent);
            }
        }
    }
}
