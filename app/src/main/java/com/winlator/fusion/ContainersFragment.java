package com.winlator.fusion;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.fusion.container.Container;
import com.winlator.fusion.container.ContainerManager;
import com.winlator.fusion.contentdialog.ContentDialog;
import com.winlator.fusion.contentdialog.StorageInfoDialog;
import com.winlator.fusion.core.PreloaderDialog;
import com.winlator.fusion.core.WineInfo;
import com.winlator.fusion.xenvironment.FusionFS;
import com.winlator.fusion.xenvironment.ImageFs;
import com.winlator.fusion.xenvironment.RootFS;

import java.util.ArrayList;
import java.util.List;

public class ContainersFragment extends Fragment {
    private RecyclerView recyclerView;
    private TextView emptyTextView;
    private ContainerManager manager;
    private PreloaderDialog preloaderDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        preloaderDialog = new PreloaderDialog(getActivity());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        manager = new ContainerManager(getContext());
        loadContainersList();
        ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        if (actionBar != null) actionBar.setTitle(R.string.containers);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FrameLayout frameLayout = (FrameLayout)inflater.inflate(R.layout.containers_fragment, container, false);
        recyclerView = frameLayout.findViewById(R.id.RecyclerView);
        Context context = recyclerView.getContext();
        emptyTextView = frameLayout.findViewById(R.id.TVEmptyText);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        DividerItemDecoration itemDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        itemDecoration.setDrawable(ContextCompat.getDrawable(context, R.drawable.list_item_divider));
        recyclerView.addItemDecoration(itemDecoration);
        return frameLayout;
    }

    private void loadContainersList() {
        ArrayList<Container> containers = manager.getContainers();
        recyclerView.setAdapter(new ContainersAdapter(containers));
        if (containers.isEmpty()) emptyTextView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.containers_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.menu_item_add) {
            FusionFS fusionFS = FusionFS.find(getContext());
            boolean fsAvailable = fusionFS.isValid();
            if (!fsAvailable) return false;
            FragmentManager fragmentManager = getParentFragmentManager();
            fragmentManager.beginTransaction()
                .addToBackStack(null)
                .replace(R.id.FLFragmentContainer, new ContainerDetailFragment())
                .commit();
            return true;
        }
        else return super.onOptionsItemSelected(menuItem);
    }

    private class ContainersAdapter extends RecyclerView.Adapter<ContainersAdapter.ViewHolder> {
        private final List<Container> data;

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

        public ContainersAdapter(List<Container> data) {
            this.data = data;
        }

        @Override
        public final ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.container_list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            final Container item = data.get(position);
            holder.imageView.setImageResource(R.drawable.icon_container);
            holder.title.setText(item.getName());

            // Show variant + wine version as subtitle
            String variant = item.getContainerVariant();
            String wineVersion = item.getWineVersion();
            StringBuilder subtitle = new StringBuilder();
            if (Container.BIONIC.equals(variant)) {
                subtitle.append("Bionic");
            } else {
                subtitle.append("Glibc");
            }
            if (wineVersion != null && !wineVersion.isEmpty()) {
                String wineDisplay = WineInfo.fromIdentifier(holder.itemView.getContext(), wineVersion).toString();
                subtitle.append(" · ").append(wineDisplay);
            }
            if (holder.subtitle != null) {
                holder.subtitle.setText(subtitle.toString());
                holder.subtitle.setVisibility(View.VISIBLE);
            }

            holder.runButton.setOnClickListener((view) -> runContainer(item));
            holder.menuButton.setOnClickListener((view) -> showListItemMenu(view, item));
        }

        @Override
        public final int getItemCount() {
            return data.size();
        }

        private void showListItemMenu(View anchorView, Container container) {
            MainActivity activity = (MainActivity)getActivity();
            PopupMenu listItemMenu = new PopupMenu(activity, anchorView);
            listItemMenu.inflate(R.menu.container_popup_menu);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) listItemMenu.setForceShowIcon(true);

            listItemMenu.setOnMenuItemClickListener((menuItem) -> {
                int id = menuItem.getItemId();
                if (id == R.id.menu_item_file_manager) {
                    activity.showFragment(new ContainerFileManagerFragment(container.id));
                } else if (id == R.id.menu_item_edit) {
                    activity.showFragment(new ContainerDetailFragment(container.id));
                } else if (id == R.id.menu_item_duplicate) {
                    ContentDialog.confirm(getContext(), R.string.do_you_want_to_duplicate_this_container, () -> {
                        preloaderDialog.show(R.string.duplicating_container);
                        manager.duplicateContainerAsync(container, () -> {
                            preloaderDialog.close();
                            loadContainersList();
                        });
                    });
                } else if (id == R.id.menu_item_remove) {
                    ContentDialog.confirm(getContext(), R.string.do_you_want_to_remove_this_container, () -> {
                        preloaderDialog.show(R.string.removing_container);
                        manager.removeContainerAsync(container, () -> {
                            preloaderDialog.close();
                            loadContainersList();
                        });
                    });
                } else if (id == R.id.menu_item_info) {
                    (new StorageInfoDialog(activity, container)).show();
                }
                return true;
            });
            listItemMenu.show();
        }

        private void runContainer(Container container) {
            Activity activity = getActivity();
            Intent intent = new Intent(activity, XServerDisplayActivity.class);
            intent.putExtra("container_id", container.id);
            activity.startActivity(intent);
        }
    }
}
