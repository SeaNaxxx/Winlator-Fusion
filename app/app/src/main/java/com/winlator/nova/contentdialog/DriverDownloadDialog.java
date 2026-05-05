package com.winlator.nova.contentdialog;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.nova.R;
import com.winlator.nova.contents.AdrenotoolsManager;
import com.winlator.nova.core.HttpUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class DriverDownloadDialog {
    private final Context context;
    private final AdrenotoolsManager adrenotoolsManager;
    private AlertDialog dialog;
    private RecyclerView recyclerView;
    private Runnable onDismissCallback;
    private final String repoUrl;

    public DriverDownloadDialog(Context context, String repoUrl) {
        this.context = context;
        this.adrenotoolsManager = new AdrenotoolsManager(context);
        this.repoUrl = repoUrl;
    }

    public void setOnDismissCallback(Runnable callback) {
        this.onDismissCallback = callback;
    }

    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.available_drivers);

        recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));

        builder.setView(recyclerView);
        builder.setNegativeButton(R.string.back, null);

        dialog = builder.create();
        dialog.show();

        fetchDrivers();
    }

    private void fetchDrivers() {
        final String[] result = {null};
        CountDownLatch latch = new CountDownLatch(1);

        HttpUtils.download(repoUrl, (jsonStr) -> {
            result[0] = jsonStr;
            latch.countDown();
        });

        new Thread(() -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (result[0] == null) {
                runOnUi(() -> Toast.makeText(context, R.string.connection_failed, Toast.LENGTH_SHORT).show());
                return;
            }

            List<ReleaseItem> releases = new ArrayList<>();
            try {
                JSONArray array = new JSONArray(result[0]);

                for (int i = 0; i < array.length(); i++) {
                    JSONObject releaseObj = array.getJSONObject(i);

                    String rawName = releaseObj.optString("name", releaseObj.optString("tag_name", "Unknown Driver"));
                    String cleanName = cleanDriverName(rawName);
                    String description = releaseObj.optString("body", "");

                    List<DriverAsset> assets = new ArrayList<>();
                    if (releaseObj.has("assets")) {
                        JSONArray assetsArr = releaseObj.getJSONArray("assets");
                        for (int j = 0; j < assetsArr.length(); j++) {
                            JSONObject asset = assetsArr.getJSONObject(j);
                            String url = asset.getString("browser_download_url");
                            String filename = asset.optString("name", "driver.zip");

                            if (url.endsWith(".zip") || url.endsWith(".tzst")) {
                                assets.add(new DriverAsset(filename, url));
                            }
                        }
                    }

                    if (!assets.isEmpty()) {
                        releases.add(new ReleaseItem(cleanName, description, assets));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            runOnUi(() -> setupAdapter(releases));
        }).start();
    }

    private String cleanDriverName(String raw) {
        String clean = raw.replace("Mesa Turnip driver ", "")
                          .replace("Mesa Turnip ", "")
                          .replace("Qualcomm Driver ", "");
        return clean.trim();
    }

    private void onDownloadClick(ReleaseItem item) {
        if (item.assets.isEmpty()) return;

        if (item.assets.size() == 1) {
            startDownload(item.assets.get(0));
        } else {
            String[] assetNames = new String[item.assets.size()];
            for (int i = 0; i < item.assets.size(); i++) {
                assetNames[i] = item.assets.get(i).name;
            }

            new AlertDialog.Builder(context)
                .setTitle(R.string.select_variant)
                .setItems(assetNames, (dialogInterface, which) -> {
                    startDownload(item.assets.get(which));
                })
                .show();
        }
    }

    private void startDownload(DriverAsset asset) {
        Toast.makeText(context, context.getString(R.string.downloading) + " " + asset.name + "...", Toast.LENGTH_SHORT).show();

        File tmpFile = new File(context.getCacheDir(), "driver_temp.zip");
        if (tmpFile.exists()) tmpFile.delete();

        AtomicBoolean successRef = new AtomicBoolean(false);

        if (context instanceof Activity) {
            HttpUtils.download((Activity) context, asset.url, tmpFile, (success) -> {
                if (success) {
                    Uri fileUri = Uri.fromFile(tmpFile);
                    String installedName = adrenotoolsManager.installDriver(fileUri);
                    if (!installedName.isEmpty()) {
                        Toast.makeText(context, context.getString(R.string.installed) + ": " + installedName, Toast.LENGTH_SHORT).show();
                        if (onDismissCallback != null) onDismissCallback.run();
                    } else {
                        Toast.makeText(context, R.string.installation_failed, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(context, R.string.download_failed, Toast.LENGTH_SHORT).show();
                }
                tmpFile.delete();
            });
        }
    }

    private void setupAdapter(List<ReleaseItem> releases) {
        if (releases.isEmpty()) {
            Toast.makeText(context, R.string.no_drivers_found, Toast.LENGTH_LONG).show();
            return;
        }
        recyclerView.setAdapter(new DriverAdapter(releases));
    }

    private static class ReleaseItem {
        String name, description;
        List<DriverAsset> assets;
        ReleaseItem(String n, String d, List<DriverAsset> a) { name = n; description = d; assets = a; }
    }

    private static class DriverAsset {
        String name, url;
        DriverAsset(String n, String u) { name = n; url = u; }
    }

    private class DriverAdapter extends RecyclerView.Adapter<DriverAdapter.ViewHolder> {
        private final List<ReleaseItem> list;
        public DriverAdapter(List<ReleaseItem> list) { this.list = list; }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.adrenotools_list_item, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            ReleaseItem item = list.get(position);
            holder.title.setText(item.name);

            if (item.assets.size() > 1) {
                holder.subtitle.setText(item.assets.size() + " variants available");
            } else {
                String shortDesc = item.description.replace("\n", " ").trim();
                if (shortDesc.length() > 50) shortDesc = shortDesc.substring(0, 50) + "...";
                if (shortDesc.isEmpty()) shortDesc = "No description";
                holder.subtitle.setText(shortDesc);
            }

            holder.actionButton.setImageResource(android.R.drawable.stat_sys_download);
            holder.actionButton.setOnClickListener(v -> onDownloadClick(item));
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title, subtitle;
            ImageButton actionButton;
            ViewHolder(View v) {
                super(v);
                title = v.findViewById(R.id.TVName);
                subtitle = v.findViewById(R.id.TVVersion);
                actionButton = v.findViewById(R.id.BTMenu);
            }
        }
    }

    private void runOnUi(Runnable action) {
        if (context instanceof Activity) ((Activity) context).runOnUiThread(action);
    }
}
