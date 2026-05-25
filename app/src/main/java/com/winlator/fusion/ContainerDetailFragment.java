package com.winlator.fusion;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;

import com.winlator.fusion.box64.Box64Preset;
import com.winlator.fusion.box64.Box64PresetManager;
import com.winlator.fusion.fexcore.FEXCorePreset;
import com.winlator.fusion.fexcore.FEXCorePresetManager;
import com.winlator.fusion.container.Container;
import com.winlator.fusion.container.ContainerManager;
import com.winlator.fusion.container.Drive;
import com.winlator.fusion.container.GraphicsDrivers;
import com.winlator.fusion.contentdialog.AddEnvVarDialog;
import com.winlator.fusion.contentdialog.AudioDriverConfigDialog;
import com.winlator.fusion.contentdialog.ContentDialog;
import com.winlator.fusion.contentdialog.RendererOptionsDialog;
import com.winlator.fusion.contentdialog.VortekConfigDialog;
import com.winlator.fusion.core.AppUtils;
import com.winlator.fusion.core.Callback;
import com.winlator.fusion.container.DXWrapperPicker;
import com.winlator.fusion.core.EnvVars;
import com.winlator.fusion.core.FileUtils;
import com.winlator.fusion.container.GraphicsDriverPicker;
import com.winlator.fusion.core.KeyValueSet;
import com.winlator.fusion.core.PreloaderDialog;
import com.winlator.fusion.core.StringUtils;
import com.winlator.fusion.core.WineInfo;
import com.winlator.fusion.core.WineInstaller;
import com.winlator.fusion.core.WineRegistryEditor;
import com.winlator.fusion.core.WineThemeManager;
import com.winlator.fusion.core.WineUtils;
import com.winlator.fusion.widget.CPUListView;
import com.winlator.fusion.xenvironment.FusionFSInstaller;
import com.winlator.fusion.widget.ColorPickerView;
import com.winlator.fusion.widget.EnvVarsView;
import com.winlator.fusion.widget.FrameRating;
import com.winlator.fusion.widget.ImagePickerView;
import com.winlator.fusion.widget.SeekBar;
import com.winlator.fusion.win32.MSLogFont;
import com.winlator.fusion.win32.WinVersions;
import com.winlator.fusion.winhandler.WinHandler;
import com.winlator.fusion.xserver.XKeycode;
import com.winlator.fusion.midi.MidiManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ContainerDetailFragment extends Fragment {
    private ContainerManager manager;
    private final int containerId;
    private Container container;
    private PreloaderDialog preloaderDialog;
    private Callback<String> openDirectoryCallback;

    public ContainerDetailFragment() {
        this(0);
    }

    public ContainerDetailFragment(int containerId) {
        this.containerId = containerId;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
        preloaderDialog = new PreloaderDialog(getActivity());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == MainActivity.OPEN_DIRECTORY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                String path = FileUtils.getFilePathFromUri(data.getData());
                if (path != null && openDirectoryCallback != null) openDirectoryCallback.call(path);
            }
            openDirectoryCallback = null;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        if (actionBar != null) actionBar.setTitle(isEditMode() ? R.string.edit_container : R.string.new_container);
    }

    public boolean isEditMode() {
        return container != null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup root, @Nullable Bundle savedInstanceState) {
        final Context context = getContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        final View view = inflater.inflate(R.layout.container_detail_fragment, root, false);
        manager = new ContainerManager(context);
        container = containerId > 0 ? manager.getContainerById(containerId) : null;

        final EditText etName = view.findViewById(R.id.ETName);

        if (isEditMode()) {
            etName.setText(container.getName());
        }
        else etName.setText(getString(R.string.container)+"-"+manager.getNextContainerId());

        // Determine current variant for variant-aware driver/wrapper selection
        String currentVariant = isEditMode() ? container.getContainerVariant() : Container.DEFAULT_VARIANT;
        boolean isBionic = currentVariant.equals(Container.BIONIC);

        final ArrayList<WineInfo> wineInfos = WineInstaller.getInstalledWineInfos(context, currentVariant);
        if (wineInfos == null || wineInfos.isEmpty()) {
            if (!Container.BIONIC.equals(currentVariant)) {
                currentVariant = Container.BIONIC;
                isBionic = true;
            }
        }
        final ArrayList<WineInfo>[] wineInfosRef = new ArrayList[]{wineInfos};
        final Spinner sWineVersion = view.findViewById(R.id.SWineVersion);
        if (wineInfosRef[0].size() > 1) loadWineVersionSpinner(view, sWineVersion, wineInfosRef[0]);

        // Container variant selector
        final Spinner sContainerVariant = view.findViewById(R.id.SContainerVariant);
        final View llBionicOptions = view.findViewById(R.id.LLBionicOptions);
        final Spinner sEmulator = view.findViewById(R.id.SEmulator);

        // Define picker refs before variant listener so they are accessible
        final String oldGraphicsDriverConfig = isEditMode() ? container.getGraphicsDriverConfig() : "";
        String selectedGraphicsDriver = isEditMode() ? container.getGraphicsDriver() : GraphicsDrivers.getDefaultDriver(context, isBionic);
        final GraphicsDriverPicker[] graphicsDriverPickerRef = new GraphicsDriverPicker[1];
        graphicsDriverPickerRef[0] = new GraphicsDriverPicker(view.findViewById(R.id.LLGraphicsDriver), selectedGraphicsDriver, oldGraphicsDriverConfig, isBionic);

        String oldDXWrapperConfig = isEditMode() ? container.getDXWrapperConfig() : "";
        String selectedDXWrapper = isEditMode() ? container.getDXWrapper() : Container.DEFAULT_DXWRAPPER;
        final DXWrapperPicker[] dxwrapperPickerRef = new DXWrapperPicker[1];
        dxwrapperPickerRef[0] = new DXWrapperPicker(view.findViewById(R.id.LLDXWrapper), graphicsDriverPickerRef[0], selectedDXWrapper, oldDXWrapperConfig, isBionic);

        view.findViewById(R.id.BTHelpDXWrapper).setOnClickListener((v) -> AppUtils.showHelpBox(context, v, R.string.dxwrapper_help_content));

        final View btRendererOptions = view.findViewById(R.id.BTRendererOptions);
        final TextView tvRendererSummary = view.findViewById(R.id.TVRendererSummary);
        if (isEditMode()) RendererOptionsDialog.loadFromContainer(btRendererOptions, container);
        else {
            btRendererOptions.setTag(R.id.rendererType, Container.RENDERER_GL);
            btRendererOptions.setTag(R.id.rendererNative, false);
            btRendererOptions.setTag(R.id.rendererPresentMode, "fifo");
            btRendererOptions.setTag(R.id.rendererFilterMode, Container.FILTER_MODE_LINEAR);
            btRendererOptions.setTag(R.id.rendererRefreshRate, 0);
        }
        updateRendererSummary(tvRendererSummary, btRendererOptions);
        btRendererOptions.setOnClickListener((v) -> {
            RendererOptionsDialog dialog = new RendererOptionsDialog(btRendererOptions);
            dialog.setAfterConfirmCallback(() -> updateRendererSummary(tvRendererSummary, btRendererOptions));
            dialog.show();
        });

        if (sContainerVariant != null) {
            String variant = isEditMode() ? container.getContainerVariant() : Container.DEFAULT_VARIANT;
            AppUtils.setSpinnerSelectionFromIdentifier(sContainerVariant, variant);
            // Disable variant spinner in edit mode — container filesystem layout cannot be changed
            if (isEditMode()) sContainerVariant.setEnabled(false);
            // Disable Bionic option if imagefs is not installed
            if (!isEditMode() && !FusionFSInstaller.isBionicAvailable(context)) {
                // Force glibc selection — Bionic variant unavailable without imagefs
                if (variant.equals(Container.BIONIC)) {
                    variant = Container.DEFAULT_VARIANT;
                    AppUtils.setSpinnerSelectionFromIdentifier(sContainerVariant, variant);
                }
            }
            llBionicOptions.setVisibility(variant.equals(Container.BIONIC) ? View.VISIBLE : View.GONE);

            final boolean[] variantInitialized = {false};
            sContainerVariant.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                    if (!variantInitialized[0]) {
                        variantInitialized[0] = true;
                        return; // Skip initial trigger — pickers already built
                    }
                    String selected = StringUtils.parseIdentifier(sContainerVariant.getSelectedItem());
                    boolean nowBionic = Container.BIONIC.equals(selected);

                    // Block Bionic selection if imagefs is not installed
                    if (nowBionic && !FusionFSInstaller.isBionicAvailable(context)) {
                        AppUtils.showToast(context, R.string.bionic_not_available);
                        AppUtils.setSpinnerSelectionFromIdentifier(sContainerVariant, Container.DEFAULT_VARIANT);
                        return;
                    }

                    llBionicOptions.setVisibility(nowBionic ? View.VISIBLE : View.GONE);

                    // Rebuild pickers when variant changes so driver/wrapper options match the variant
                    String currentGraphicsDriver = graphicsDriverPickerRef[0].getGraphicsDriver();
                    String currentGraphicsDriverConfig = graphicsDriverPickerRef[0].getGraphicsDriverConfig();
                    String defaultGraphicsDriver = GraphicsDrivers.getDefaultDriver(context, nowBionic);
                    // If current driver is incompatible with new variant (e.g. Vortek in Bionic), reset to default
                    String newGraphicsDriver = currentGraphicsDriver;
                    String[] driverIds = GraphicsDrivers.parseIdentifiers(currentGraphicsDriver);
                    if (nowBionic && driverIds[0].equals(GraphicsDrivers.VORTEK)) {
                        newGraphicsDriver = defaultGraphicsDriver;
                        currentGraphicsDriverConfig = "";
                    }
                    if (!nowBionic && driverIds[0].equals(GraphicsDrivers.WRAPPER)) {
                        newGraphicsDriver = defaultGraphicsDriver;
                        currentGraphicsDriverConfig = "";
                    }
                    graphicsDriverPickerRef[0] = new GraphicsDriverPicker(view.findViewById(R.id.LLGraphicsDriver), newGraphicsDriver, currentGraphicsDriverConfig, nowBionic);

                    String currentDXWrapper = dxwrapperPickerRef[0].getDXWrapper();
                    String currentDXWrapperConfig = dxwrapperPickerRef[0].getDXWrapperConfig();
                    dxwrapperPickerRef[0] = new DXWrapperPicker(view.findViewById(R.id.LLDXWrapper), graphicsDriverPickerRef[0], currentDXWrapper, currentDXWrapperConfig, nowBionic);

                    // Update env vars to variant-specific defaults if user hasn't modified them
                    EnvVarsView envVarsView = view.findViewById(R.id.EnvVarsView);
                    if (envVarsView != null) {
                        String previousVariantDefaults = nowBionic ? Container.DEFAULT_ENV_VARS_GLIBC : Container.DEFAULT_ENV_VARS_BIONIC;
                        String targetVariantDefaults = nowBionic ? Container.DEFAULT_ENV_VARS_BIONIC : Container.DEFAULT_ENV_VARS_GLIBC;
                        String currentEnvVars = envVarsView.getEnvVars();
                        if (currentEnvVars.equals(previousVariantDefaults)) {
                            envVarsView.setEnvVars(new EnvVars(targetVariantDefaults));
                        }
                    }

                    // Hide Box64 preset row for Bionic (it uses its own Box64 in LLBionicOptions)
                    Spinner sBox64Preset = view.findViewById(R.id.SBox64Preset);
                    if (sBox64Preset != null) {
                        sBox64Preset.setVisibility(nowBionic ? View.GONE : View.VISIBLE);
                    }

                    // Re-filter wine versions for the new variant
                    wineInfosRef[0] = WineInstaller.getInstalledWineInfos(context, selected);
                    if (wineInfosRef[0].size() > 1) {
                        loadWineVersionSpinner(view, sWineVersion, wineInfosRef[0]);
                        view.findViewById(R.id.LLWineVersion).setVisibility(View.VISIBLE);
                    } else {
                        view.findViewById(R.id.LLWineVersion).setVisibility(View.GONE);
                    }

                    View llTabXR = view.findViewById(R.id.LLTabXR);
                    if (llTabXR != null) llTabXR.setVisibility(View.GONE);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        if (sEmulator != null && isEditMode()) {
            AppUtils.setSpinnerSelectionFromValue(sEmulator, container.getEmulator());
        }

        // FEXCore configuration
        final Spinner sFEXCoreVersion = view.findViewById(R.id.SFEXCoreVersion);
        final Spinner sFEXCorePreset = view.findViewById(R.id.SFEXCorePreset);
        final Spinner sBox64VersionBionic = view.findViewById(R.id.SBox64VersionBionic);

        // Use FEXCorePresetManager for the preset spinner so custom presets are included
        FEXCorePresetManager.loadSpinner(sFEXCorePreset, isEditMode() ? container.getFEXCorePreset() : FEXCorePreset.DEFAULT);

        if (isEditMode()) {
            AppUtils.setSpinnerSelectionFromValue(sFEXCoreVersion, container.getFEXCoreVersion());
            AppUtils.setSpinnerSelectionFromValue(sBox64VersionBionic, container.getBox64Version());
        }

        loadScreenSizeSpinner(view, isEditMode() ? container.getScreenSize() : Container.DEFAULT_SCREEN_SIZE);

        Spinner sAudioDriver = view.findViewById(R.id.SAudioDriver);
        AppUtils.setSpinnerSelectionFromIdentifier(sAudioDriver, isEditMode() ? container.getAudioDriver() : Container.DEFAULT_AUDIO_DRIVER);

        final View vAudioDriverConfig = view.findViewById(R.id.BTAudioDriverConfig);
        vAudioDriverConfig.setTag(isEditMode() ? container.getAudioDriverConfig() : "");
        vAudioDriverConfig.setOnClickListener((v) -> (new AudioDriverConfigDialog(v)).show());

        final Spinner sMIDISoundFont = view.findViewById(R.id.SMIDISoundFont);
        if (sMIDISoundFont != null) {
            MidiManager.loadSFSpinner(sMIDISoundFont);
            AppUtils.setSpinnerSelectionFromValue(sMIDISoundFont, isEditMode() ? container.getMIDISoundFont() : "");
        }

        final EditText etLC_ALL = view.findViewById(R.id.ETlcall);
        if (etLC_ALL != null) {
            Locale systemLocale = Locale.getDefault();
            etLC_ALL.setText(isEditMode() ? container.getLC_ALL() : systemLocale.getLanguage() + '_' + systemLocale.getCountry() + ".UTF-8");
        }
        final View btShowLCALL = view.findViewById(R.id.BTShowLCALL);
        if (btShowLCALL != null) {
            btShowLCALL.setOnClickListener((v) -> {
                PopupMenu popupMenu = new PopupMenu(context, v);
                String[] lcs = getResources().getStringArray(R.array.some_lc_all);
                for (int i = 0; i < lcs.length; i++)
                    popupMenu.getMenu().add(Menu.NONE, i, Menu.NONE, lcs[i]);
                popupMenu.setOnMenuItemClickListener(item -> {
                    if (etLC_ALL != null) etLC_ALL.setText(item.toString() + ".UTF-8");
                    return true;
                });
                popupMenu.show();
            });
        }

        final CheckBox cbShowFPS = view.findViewById(R.id.CBShowFPS);
        if (cbShowFPS != null) cbShowFPS.setChecked(!isEditMode() || container.isShowFPS());

        final CheckBox cbFullscreenStretched = view.findViewById(R.id.CBFullscreenStretched);
        if (cbFullscreenStretched != null) cbFullscreenStretched.setChecked(isEditMode() && container.isFullscreenStretched());

        final Spinner sHUDMode = view.findViewById(R.id.SHUDMode);
        sHUDMode.setSelection(isEditMode() ? container.getHUDMode() : FrameRating.Mode.DISABLED.ordinal());

        final Spinner sStartupSelection = view.findViewById(R.id.SStartupSelection);
        byte oldStartupSelection = isEditMode() ? container.getStartupSelection() : -1;
        sStartupSelection.setSelection(oldStartupSelection != -1 ? oldStartupSelection : Container.STARTUP_SELECTION_ESSENTIAL);

        final Spinner sWinVersion = view.findViewById(R.id.SWinVersion);
        sWinVersion.setTag((byte)-1);

        final Spinner sBox64Preset = view.findViewById(R.id.SBox64Preset);
        Box64PresetManager.loadSpinner(sBox64Preset, isEditMode() ? container.getBox64Preset() : preferences.getString("box64_preset", Box64Preset.DEFAULT));
        // Hide Box64 preset for Bionic containers (they use their own Box64 in LLBionicOptions)
        if (isBionic && sBox64Preset != null) sBox64Preset.setVisibility(View.GONE);

        final CheckBox cbEnableXInput = view.findViewById(R.id.CBEnableXInput);
        final CheckBox cbEnableDInput = view.findViewById(R.id.CBEnableDInput);
        final CheckBox cbExclusiveXInput = view.findViewById(R.id.CBExclusiveXInput);
        if (cbEnableXInput != null && cbEnableDInput != null && cbExclusiveXInput != null) {
            int inputType = isEditMode() ? container.getInputType() : WinHandler.DEFAULT_INPUT_TYPE;
            cbEnableXInput.setChecked((inputType & WinHandler.FLAG_INPUT_TYPE_XINPUT) == WinHandler.FLAG_INPUT_TYPE_XINPUT);
            cbEnableDInput.setChecked((inputType & WinHandler.FLAG_INPUT_TYPE_DINPUT) == WinHandler.FLAG_INPUT_TYPE_DINPUT);
            cbExclusiveXInput.setChecked(isEditMode() ? container.isExclusiveXInput() : true);

            cbEnableDInput.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (cbExclusiveXInput.isChecked() && isChecked && cbEnableXInput.isChecked()) cbEnableXInput.setChecked(false);
            });
            cbEnableXInput.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (cbExclusiveXInput.isChecked() && isChecked && cbEnableDInput.isChecked()) cbEnableDInput.setChecked(false);
            });
            cbExclusiveXInput.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isChecked) {
                    cbEnableXInput.setChecked(true);
                    cbEnableDInput.setChecked(true);
                    cbEnableXInput.setEnabled(false);
                    cbEnableDInput.setEnabled(false);
                } else {
                    cbEnableXInput.setEnabled(true);
                    cbEnableDInput.setEnabled(true);
                    if (cbEnableXInput.isChecked() && cbEnableDInput.isChecked()) cbEnableDInput.setChecked(false);
                }
            });
            if (!cbExclusiveXInput.isChecked()) {
                cbEnableXInput.setChecked(true);
                cbEnableDInput.setChecked(true);
                cbEnableXInput.setEnabled(false);
                cbEnableDInput.setEnabled(false);
            }
        }
        final View btHelpXInput = view.findViewById(R.id.BTXInputHelp);
        final View btHelpDInput = view.findViewById(R.id.BTDInputHelp);
        final View btHelpExclusiveXInput = view.findViewById(R.id.BTExclusiveXInputHelp);
        if (btHelpXInput != null) btHelpXInput.setOnClickListener((v) -> AppUtils.showHelpBox(context, v, R.string.help_xinput));
        if (btHelpDInput != null) btHelpDInput.setOnClickListener((v) -> AppUtils.showHelpBox(context, v, R.string.help_dinput));
        if (btHelpExclusiveXInput != null) btHelpExclusiveXInput.setOnClickListener((v) -> AppUtils.showHelpBox(context, v, R.string.help_exclusive_xinput));

        final CPUListView cpuListView = view.findViewById(R.id.CPUListView);
        final CPUListView cpuListViewWoW64 = view.findViewById(R.id.CPUListViewWoW64);

        cpuListView.setCheckedCPUList(isEditMode() ? container.getCPUList(true) : Container.getFallbackCPUList());
        cpuListViewWoW64.setCheckedCPUList(isEditMode() ? container.getCPUListWoW64(true) : Container.getFallbackCPUListWoW64());

        final Spinner sPrimaryController = view.findViewById(R.id.SPrimaryController);
        if (sPrimaryController != null) sPrimaryController.setSelection(isEditMode() ? container.getPrimaryController() : 1);
        setupControllerMappingSpinner(view.findViewById(R.id.SButtonA), Container.XrControllerMapping.BUTTON_A, XKeycode.KEY_A.ordinal());
        setupControllerMappingSpinner(view.findViewById(R.id.SButtonB), Container.XrControllerMapping.BUTTON_B, XKeycode.KEY_B.ordinal());
        setupControllerMappingSpinner(view.findViewById(R.id.SButtonX), Container.XrControllerMapping.BUTTON_X, XKeycode.KEY_X.ordinal());
        setupControllerMappingSpinner(view.findViewById(R.id.SButtonY), Container.XrControllerMapping.BUTTON_Y, XKeycode.KEY_Y.ordinal());
        setupControllerMappingSpinner(view.findViewById(R.id.SButtonGrip), Container.XrControllerMapping.BUTTON_GRIP, XKeycode.KEY_SPACE.ordinal());
        setupControllerMappingSpinner(view.findViewById(R.id.SButtonTrigger), Container.XrControllerMapping.BUTTON_TRIGGER, XKeycode.KEY_ENTER.ordinal());
        setupControllerMappingSpinner(view.findViewById(R.id.SThumbstickUp), Container.XrControllerMapping.THUMBSTICK_UP, XKeycode.KEY_UP.ordinal());
        setupControllerMappingSpinner(view.findViewById(R.id.SThumbstickDown), Container.XrControllerMapping.THUMBSTICK_DOWN, XKeycode.KEY_DOWN.ordinal());
        setupControllerMappingSpinner(view.findViewById(R.id.SThumbstickLeft), Container.XrControllerMapping.THUMBSTICK_LEFT, XKeycode.KEY_LEFT.ordinal());
        setupControllerMappingSpinner(view.findViewById(R.id.SThumbstickRight), Container.XrControllerMapping.THUMBSTICK_RIGHT, XKeycode.KEY_RIGHT.ordinal());

        View llTabXR = view.findViewById(R.id.LLTabXR);
        if (llTabXR != null) llTabXR.setVisibility(View.GONE);

        createWineConfigurationTab(view);
        final EnvVarsView envVarsView = createEnvVarsTab(view);
        createWinComponentsTab(view, isEditMode() ? container.getWinComponents() : Container.DEFAULT_WINCOMPONENTS);
        createDrivesTab(view);

        AppUtils.setupTabLayout(view, R.id.TabLayout, (tabResId) -> {
            if (tabResId == R.id.LLTabAdvanced) if ((byte)sWinVersion.getTag() == -1) WinVersions.loadSpinner(container, sWinVersion);
        }, R.id.LLTabWineConfiguration, R.id.LLTabWinComponents, R.id.LLTabEnvVars, R.id.LLTabDrives, R.id.LLTabAdvanced, R.id.LLTabXR);

        view.findViewById(R.id.BTConfirm).setOnClickListener((v) -> {
            try {
                String name = etName.getText().toString();
                String screenSize = getScreenSize(view);
                String envVars = envVarsView.getEnvVars();
                String graphicsDriver = graphicsDriverPickerRef[0].getGraphicsDriver();
                String dxwrapper = dxwrapperPickerRef[0].getDXWrapper();
                String dxwrapperConfig = dxwrapperPickerRef[0].getDXWrapperConfig();
                String graphicsDriverConfig = graphicsDriverPickerRef[0].getGraphicsDriverConfig();
                String audioDriverConfig = vAudioDriverConfig.getTag().toString();
                String audioDriver = StringUtils.parseIdentifier(sAudioDriver.getSelectedItem());
                String wincomponents = getWinComponents(view);
                String drives = getDrives(view);
                byte hudMode = (byte)sHUDMode.getSelectedItemPosition();
                boolean showFPS = cbShowFPS != null && cbShowFPS.isChecked();
                if (showFPS && hudMode == (byte)FrameRating.Mode.DISABLED.ordinal()) {
                    hudMode = (byte)FrameRating.Mode.SIMPLE.ordinal();
                } else if (!showFPS && hudMode != (byte)FrameRating.Mode.DISABLED.ordinal()) {
                    hudMode = (byte)FrameRating.Mode.DISABLED.ordinal();
                }
                String cpuList = cpuListView.getCheckedCPUListAsString();
                String cpuListWoW64 = cpuListViewWoW64.getCheckedCPUListAsString();
                byte startupSelection = (byte)sStartupSelection.getSelectedItemPosition();
                String box64Preset = Box64PresetManager.getSpinnerSelectedId(sBox64Preset);
                String desktopTheme = getDesktopTheme(view);

                String containerVariant = sContainerVariant != null ? StringUtils.parseIdentifier(sContainerVariant.getSelectedItem()) : Container.DEFAULT_VARIANT;
                if (containerVariant == null || containerVariant.isEmpty()) containerVariant = Container.DEFAULT_VARIANT;
                String emulator = sEmulator != null && sEmulator.getSelectedItem() != null ? sEmulator.getSelectedItem().toString() : "FEXCore";

                if (isEditMode()) {
                    // Prevent changing container variant on existing containers — the filesystem
                    // layout (rootfs vs imagefs) is determined at creation time and cannot be
                    // changed retroactively without breaking the container's wineprefix and paths.
                    if (!containerVariant.equals(container.getContainerVariant())) {
                        AppUtils.showToast(getContext(), R.string.cannot_change_container_variant);
                        return;
                    }
                    container.setName(name);
                    container.setScreenSize(screenSize);
                    container.setEnvVars(envVars);
                    container.setCPUList(cpuList);
                    container.setCPUListWoW64(cpuListWoW64);
                    container.setGraphicsDriver(graphicsDriver);
                    container.setDXWrapper(dxwrapper);
                    container.setDXWrapperConfig(dxwrapperConfig);
                    container.setGraphicsDriverConfig(graphicsDriverConfig);
                    container.setAudioDriver(audioDriver);
                    container.setAudioDriverConfig(audioDriverConfig);
                    container.setWinComponents(wincomponents);
                    container.setDrives(drives);
                    container.setHUDMode(hudMode);
                    container.setStartupSelection(startupSelection);
                    container.setBox64Preset(box64Preset);
                    container.setDesktopTheme(desktopTheme);
                    container.setMIDISoundFont(sMIDISoundFont != null && sMIDISoundFont.getSelectedItemPosition() > 0 ? sMIDISoundFont.getSelectedItem().toString() : "");
                    if (etLC_ALL != null) container.setLC_ALL(etLC_ALL.getText().toString());
                    container.setFullscreenStretched(cbFullscreenStretched != null && cbFullscreenStretched.isChecked());
                    container.setShowFPS(cbShowFPS != null && cbShowFPS.isChecked());
                    if (cbExclusiveXInput != null) container.setExclusiveXInput(cbExclusiveXInput.isChecked());
                    int finalInputType = 0;
                    if (cbEnableXInput != null && cbEnableXInput.isChecked()) finalInputType |= WinHandler.FLAG_INPUT_TYPE_XINPUT;
                    if (cbEnableDInput != null && cbEnableDInput.isChecked()) finalInputType |= WinHandler.FLAG_INPUT_TYPE_DINPUT;
                    container.setInputType(finalInputType);
                    if (sPrimaryController != null) container.setPrimaryController(sPrimaryController.getSelectedItemPosition());
                    container.setControllerMapping(getControllerMapping(view));
                    container.setContainerVariant(containerVariant);
                    container.setEmulator(emulator);
                    RendererOptionsDialog.applyToContainer(btRendererOptions, container);
                    if (containerVariant.equals(Container.BIONIC)) {
                        if (sFEXCoreVersion.getSelectedItem() != null)
                            container.setFEXCoreVersion(sFEXCoreVersion.getSelectedItem().toString());
                        container.setFEXCorePreset(FEXCorePresetManager.getSpinnerSelectedId(sFEXCorePreset));
                        if (sBox64VersionBionic.getSelectedItem() != null)
                            container.setBox64Version(sBox64VersionBionic.getSelectedItem().toString());
                    }
                    container.saveData();

                    saveWineRegistryKeys(view);

                    boolean requireRestart = GraphicsDrivers.VORTEK.equals(GraphicsDrivers.parseIdentifiers(graphicsDriver)[0]) && VortekConfigDialog.isRequireRestart(oldGraphicsDriverConfig, graphicsDriverConfig);
                    if (requireRestart) ContentDialog.confirm(context, R.string.the_settings_have_been_changed_do_you_want_to_restart_the_app, () -> AppUtils.restartApplication(context));

                    getActivity().onBackPressed();
                }
                else {
                    JSONObject data = new JSONObject();
                    data.put("name", name);
                    data.put("screenSize", screenSize);
                    data.put("envVars", envVars);
                    data.put("cpuList", cpuList);
                    data.put("cpuListWoW64", cpuListWoW64);
                    data.put("graphicsDriver", graphicsDriver);
                    data.put("dxwrapper", dxwrapper);
                    data.put("dxwrapperConfig", dxwrapperConfig);
                    data.put("graphicsDriverConfig", graphicsDriverConfig);
                    data.put("audioDriver", audioDriver);
                    data.put("audioDriverConfig", audioDriverConfig);
                    data.put("wincomponents", wincomponents);
                    data.put("drives", drives);
                    data.put("hudMode", hudMode);
                    data.put("startupSelection", startupSelection);
                    data.put("box64Preset", box64Preset);
                    data.put("desktopTheme", desktopTheme);
                    data.put("midiSoundFont", sMIDISoundFont != null && sMIDISoundFont.getSelectedItemPosition() > 0 ? sMIDISoundFont.getSelectedItem().toString() : "");
                    if (etLC_ALL != null) data.put("lc_all", etLC_ALL.getText().toString());
                    data.put("fullscreenStretched", cbFullscreenStretched != null && cbFullscreenStretched.isChecked());
                    data.put("exclusiveXInput", cbExclusiveXInput != null && cbExclusiveXInput.isChecked());
                    int finalInputType = 0;
                    if (cbEnableXInput != null && cbEnableXInput.isChecked()) finalInputType |= WinHandler.FLAG_INPUT_TYPE_XINPUT;
                    if (cbEnableDInput != null && cbEnableDInput.isChecked()) finalInputType |= WinHandler.FLAG_INPUT_TYPE_DINPUT;
                    data.put("inputType", finalInputType);
                    if (sPrimaryController != null) data.put("primaryController", sPrimaryController.getSelectedItemPosition());
                    data.put("controllerMapping", getControllerMapping(view));
                    data.put("containerVariant", containerVariant);
                    data.put("emulator", emulator);
                    data.put("rendererType", RendererOptionsDialog.getTagByte(btRendererOptions, R.id.rendererType, Container.RENDERER_GL));
                    data.put("rendererNative", RendererOptionsDialog.getTagBoolean(btRendererOptions, R.id.rendererNative, false));
                    data.put("rendererPresentMode", RendererOptionsDialog.getTagPresentMode(btRendererOptions, R.id.rendererPresentMode, "fifo"));
                    data.put("rendererFilterMode", RendererOptionsDialog.getTagInt(btRendererOptions, R.id.rendererFilterMode, Container.FILTER_MODE_LINEAR));
                    data.put("rendererRefreshRate", RendererOptionsDialog.getTagInt(btRendererOptions, R.id.rendererRefreshRate, 0));
                    if (containerVariant.equals(Container.BIONIC)) {
                        if (sFEXCoreVersion.getSelectedItem() != null)
                            data.put("fexcoreVersion", sFEXCoreVersion.getSelectedItem().toString());
                        data.put("fexcorePreset", FEXCorePresetManager.getSpinnerSelectedId(sFEXCorePreset));
                        if (sBox64VersionBionic.getSelectedItem() != null)
                            data.put("box64Version", sBox64VersionBionic.getSelectedItem().toString());
                    }

                    if (wineInfosRef[0].size() > 1) {
                        data.put("wineVersion", wineInfosRef[0].get(sWineVersion.getSelectedItemPosition()).identifier());
                    } else if (!wineInfosRef[0].isEmpty()) {
                        data.put("wineVersion", wineInfosRef[0].get(0).identifier());
                    } else if (containerVariant.equals(Container.BIONIC)) {
                        if (!FusionFSInstaller.isBionicAvailable(getContext())) {
                            AppUtils.showToast(getContext(), R.string.bionic_not_available);
                            return;
                        }
                        data.put("wineVersion", WineInfo.BIONIC_WINE_IDENTIFIER);
                    } else {
                        AppUtils.showToast(getContext(), R.string.unable_to_install_wine);
                        return;
                    }

                    preloaderDialog.show(R.string.creating_container);
                    manager.createContainerAsync(data, (container) -> {
                        if (container != null) {
                            this.container = container;
                            if (getView() != null) saveWineRegistryKeys(getView());
                        } else {
                            if (getContext() != null) {
                                AppUtils.showToast(getContext(), R.string.unable_to_install_wine);
                            }
                        }
                        if (preloaderDialog != null) preloaderDialog.close();
                        if (getActivity() != null && !getActivity().isFinishing()) getActivity().onBackPressed();
                    });
                }
            }
            catch (JSONException e) {
                android.util.Log.e("ContainerDetail", "Failed to create container data", e);
                if (preloaderDialog != null) preloaderDialog.close();
                AppUtils.showToast(getContext(), R.string.unable_to_install_wine);
            }
        });
        return view;
    }

    private void saveWineRegistryKeys(View view) {
        if (container == null || container.getRootDir() == null) return;
        File userRegFile = new File(container.getRootDir(), ".wine/user.reg");
        if (!userRegFile.isFile()) return;
        try (WineRegistryEditor registryEditor = new WineRegistryEditor(userRegFile)) {
            Spinner sSystemFont = view.findViewById(R.id.SSystemFont);
            if (sSystemFont != null && sSystemFont.getSelectedItem() != null) {
                String faceName = sSystemFont.getSelectedItem().toString();
                WineUtils.setSystemFont(registryEditor, faceName);
                container.setSystemFont(faceName);
            }

            SeekBar sbLogPixels = view.findViewById(R.id.SBLogPixels);
            registryEditor.setDwordValue("Control Panel\\Desktop", "LogPixels", (int)sbLogPixels.getValue());

            Spinner sMouseWarpOverride = view.findViewById(R.id.SMouseWarpOverride);

            final String[] mouseWarpOverrideValues = new String[]{"disable", "enable", "force"};
            registryEditor.setStringValue("Software\\Wine\\DirectInput", "MouseWarpOverride", mouseWarpOverrideValues[sMouseWarpOverride.getSelectedItemPosition()]);

            registryEditor.setStringValue("Software\\Wine\\Direct3D", "shader_backend", "glsl");
            registryEditor.setStringValue("Software\\Wine\\Direct3D", "UseGLSL", "enabled");
        }

        Spinner sWinVersion = view.findViewById(R.id.SWinVersion);
        int oldPosition = (byte)sWinVersion.getTag();
        if (oldPosition != -1) {
            int newPosition = sWinVersion.getSelectedItemPosition();
            if (oldPosition != newPosition) {
                WineUtils.setWinVersion(container, newPosition);
                container.setWinVersion(WinVersions.getWinVersions()[newPosition].version);
            }
        }

        container.saveData();
    }

    private void createWineConfigurationTab(View view) {
        Context context = getContext();

        WineThemeManager.ThemeInfo desktopTheme = new WineThemeManager.ThemeInfo(isEditMode() ? container.getDesktopTheme() : WineThemeManager.DEFAULT_DESKTOP_THEME);
        RadioGroup rgDesktopTheme = view.findViewById(R.id.RGDesktopTheme);
        rgDesktopTheme.check(desktopTheme.theme == WineThemeManager.Theme.LIGHT ? R.id.RBLight : R.id.RBDark);
        final ImagePickerView ipvDesktopBackgroundImage = view.findViewById(R.id.IPVDesktopBackgroundImage);
        ipvDesktopBackgroundImage.setSelectedSource(desktopTheme.wallpaperId);
        final ColorPickerView cpvDesktopBackgroundColor = view.findViewById(R.id.CPVDesktopBackgroundColor);
        cpvDesktopBackgroundColor.setColor(desktopTheme.backgroundColor);

        Spinner sDesktopBackgroundType = view.findViewById(R.id.SDesktopBackgroundType);
        sDesktopBackgroundType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                WineThemeManager.BackgroundType type = WineThemeManager.BackgroundType.values()[position];
                ipvDesktopBackgroundImage.setVisibility(View.GONE);
                cpvDesktopBackgroundColor.setVisibility(View.GONE);

                if (type == WineThemeManager.BackgroundType.IMAGE) {
                    ipvDesktopBackgroundImage.setVisibility(View.VISIBLE);
                }
                else if (type == WineThemeManager.BackgroundType.COLOR) {
                    cpvDesktopBackgroundColor.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        sDesktopBackgroundType.setSelection(desktopTheme.backgroundType.ordinal());

        File containerDir = isEditMode() ? container.getRootDir() : null;
        if (containerDir != null) {
            File userRegFile = new File(containerDir, ".wine/user.reg");
            try (WineRegistryEditor registryEditor = new WineRegistryEditor(userRegFile)) {
                Spinner sSystemFont = view.findViewById(R.id.SSystemFont);
                MSLogFont msLogFont = (new MSLogFont()).fromByteArray(registryEditor.getHexValues("Control Panel\\Desktop\\WindowMetrics", "CaptionFont"));
                AppUtils.setSpinnerSelectionFromValue(sSystemFont, msLogFont.getFaceName());

                SeekBar sbLogPixels = view.findViewById(R.id.SBLogPixels);
                sbLogPixels.setValue(registryEditor.getDwordValue("Control Panel\\Desktop", "LogPixels", 96));

                List<String> mouseWarpOverrideList = Arrays.asList(context.getString(R.string.disable), context.getString(R.string.enable), context.getString(R.string.force));
                Spinner sMouseWarpOverride = view.findViewById(R.id.SMouseWarpOverride);
                sMouseWarpOverride.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, mouseWarpOverrideList));
                AppUtils.setSpinnerSelectionFromValue(sMouseWarpOverride, registryEditor.getStringValue("Software\\Wine\\DirectInput", "MouseWarpOverride", "disable"));
            }
        }
    }

    public static String getScreenSize(View view) {
        Spinner sScreenSize = view.findViewById(R.id.SScreenSize);
        String value = sScreenSize.getSelectedItem().toString();
        if (sScreenSize.getSelectedItemPosition() == 0) {
            value = Container.DEFAULT_SCREEN_SIZE;
            String strWidth = ((EditText)view.findViewById(R.id.ETScreenWidth)).getText().toString().trim();
            String strHeight = ((EditText)view.findViewById(R.id.ETScreenHeight)).getText().toString().trim();
            if (strWidth.matches("[0-9]+") && strHeight.matches("[0-9]+")) {
                int width = Integer.parseInt(strWidth);
                int height = Integer.parseInt(strHeight);
                if ((width % 2) == 0 && (height % 2) == 0) return width+"x"+height;
            }
        }
        return StringUtils.parseIdentifier(value);
    }

    private String getDesktopTheme(View view) {
        Spinner sDesktopBackgroundType = view.findViewById(R.id.SDesktopBackgroundType);
        WineThemeManager.BackgroundType type = WineThemeManager.BackgroundType.values()[sDesktopBackgroundType.getSelectedItemPosition()];
        RadioGroup rgDesktopTheme = view.findViewById(R.id.RGDesktopTheme);
        ImagePickerView ipvDesktopBackgroundImage = view.findViewById(R.id.IPVDesktopBackgroundImage);
        ColorPickerView cpvDesktopBackground = view.findViewById(R.id.CPVDesktopBackgroundColor);
        WineThemeManager.Theme theme = rgDesktopTheme.getCheckedRadioButtonId() == R.id.RBLight ? WineThemeManager.Theme.LIGHT : WineThemeManager.Theme.DARK;

       String desktopTheme = theme+","+type+","+cpvDesktopBackground.getColorAsString();
        if (type == WineThemeManager.BackgroundType.IMAGE) {
            String selectedSource = ipvDesktopBackgroundImage.getSelectedSource();
            String wallpaperId = !selectedSource.equals(WineThemeManager.DEFAULT_WALLPAPER_ID) && selectedSource.startsWith("wallpaper-") ? selectedSource : "0";
            File userWallpaperFile = WineThemeManager.getUserWallpaperFile(getContext());
            desktopTheme += ","+(userWallpaperFile.isFile() && selectedSource.equals("user-wallpaper") ? userWallpaperFile.lastModified() : wallpaperId);
        }
        return desktopTheme;
    }

    public static void loadScreenSizeSpinner(View view, String selectedValue) {
        final Spinner sScreenSize = view.findViewById(R.id.SScreenSize);

        final LinearLayout llCustomScreenSize = view.findViewById(R.id.LLCustomScreenSize);
        sScreenSize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                llCustomScreenSize.setVisibility(sScreenSize.getSelectedItemPosition() == 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        boolean found = AppUtils.setSpinnerSelectionFromIdentifier(sScreenSize, selectedValue);
        if (!found) {
            sScreenSize.setSelection(0);
            String[] screenSize = selectedValue.split("x");
            ((EditText)view.findViewById(R.id.ETScreenWidth)).setText(screenSize[0]);
            ((EditText)view.findViewById(R.id.ETScreenHeight)).setText(screenSize[1]);
        }
    }

    public static String getWinComponents(View view) {
        ViewGroup parent = view.findViewById(R.id.LLTabWinComponents);
        ArrayList<View> views = new ArrayList<>();
        AppUtils.findViewsWithClass(parent, Spinner.class, views);
        String[] wincomponents = new String[views.size()];

        for (int i = 0; i < views.size(); i++) {
            Spinner spinner = (Spinner)views.get(i);
            wincomponents[i] = spinner.getTag()+"="+spinner.getSelectedItemPosition();
        }
        return String.join(",", wincomponents);
    }

    public static void createWinComponentsTab(View view, String wincomponents) {
        Context context = view.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        ViewGroup tabView = view.findViewById(R.id.LLTabWinComponents);
        ViewGroup directxSectionView = tabView.findViewById(R.id.LLWinComponentsDirectX);
        ViewGroup generalSectionView = tabView.findViewById(R.id.LLWinComponentsGeneral);

        for (String[] wincomponent : new KeyValueSet(wincomponents)) {
            final String name = wincomponent[0];
            ViewGroup parent = name.startsWith("direct") || name.startsWith("x") ? directxSectionView : generalSectionView;
            View itemView = inflater.inflate(R.layout.wincomponent_list_item, parent, false);
            ((TextView)itemView.findViewById(R.id.TextView)).setText(StringUtils.getString(context, name));
            Spinner spinner = itemView.findViewById(R.id.Spinner);
            spinner.setSelection(Integer.parseInt(wincomponent[1]), false);
            spinner.setTag(name);
            parent.addView(itemView);
        }
    }

    private EnvVarsView createEnvVarsTab(final View view) {
        final Context context = view.getContext();
        final EnvVarsView envVarsView = view.findViewById(R.id.EnvVarsView);
        String variant = isEditMode() ? container.getContainerVariant() : Container.DEFAULT_VARIANT;
        Spinner spinner = view.getRootView().findViewById(R.id.SContainerVariant);
        if (spinner != null) {
            String selected = StringUtils.parseIdentifier(spinner.getSelectedItem());
            if (selected != null && !selected.isEmpty()) variant = selected;
        }
        String defaultEnvVars = isEditMode() ? container.getEnvVars() :
            (variant.equals(Container.BIONIC) ? Container.DEFAULT_ENV_VARS_BIONIC : Container.DEFAULT_ENV_VARS_GLIBC);
        envVarsView.setEnvVars(new EnvVars(defaultEnvVars));
        view.findViewById(R.id.BTAddEnvVar).setOnClickListener((v) -> (new AddEnvVarDialog(context, envVarsView)).show());
        return envVarsView;
    }

    private String getDrives(View view) {
        LinearLayout parent = view.findViewById(R.id.LLDrives);
        String drives = "";

        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            Spinner spinner = child.findViewById(R.id.Spinner);
            EditText editText = child.findViewById(R.id.EditText);
            String path = editText.getText().toString().replace(":", "").trim();
            if (!path.isEmpty()) drives += spinner.getSelectedItem()+path;
        }
        return drives;
    }

    private void createDrivesTab(View view) {
        final Context context = getContext();

        final LinearLayout parent = view.findViewById(R.id.LLDrives);
        final View emptyTextView = view.findViewById(R.id.TVDrivesEmptyText);
        LayoutInflater inflater = LayoutInflater.from(context);
        final String drives = isEditMode() ? container.getDrives() : Container.DEFAULT_DRIVES;
        final String[] driveLetters = new String[Container.MAX_DRIVE_LETTERS];
        for (int i = 0; i < driveLetters.length; i++) driveLetters[i] = ((char)(i + 68))+":";

        Callback<Drive> addItem = (drive) -> {
            final View itemView = inflater.inflate(R.layout.drive_list_item, parent, false);
            Spinner spinner = itemView.findViewById(R.id.Spinner);
            spinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, driveLetters));
            AppUtils.setSpinnerSelectionFromValue(spinner, drive.letter+":");

            final EditText editText = itemView.findViewById(R.id.EditText);
            editText.setText(drive.path);

            itemView.findViewById(R.id.BTSearch).setOnClickListener((v) -> showDriveSearchPopupMenu(v, drive, editText));
            itemView.findViewById(R.id.BTRemove).setOnClickListener((v) -> {
                parent.removeView(itemView);
                if (parent.getChildCount() == 0) emptyTextView.setVisibility(View.VISIBLE);
            });
            parent.addView(itemView);
        };
        for (Drive drive : Container.drivesIterator(drives)) addItem.call(drive);

        view.findViewById(R.id.BTAddDrive).setOnClickListener((v) -> {
            if (parent.getChildCount() >= Container.MAX_DRIVE_LETTERS) return;
            final String nextDriveLetter = String.valueOf(driveLetters[parent.getChildCount()].charAt(0));
            addItem.call(new Drive(nextDriveLetter, ""));
        });

        if (drives.isEmpty()) emptyTextView.setVisibility(View.VISIBLE);
    }

    private void showDriveSearchPopupMenu(View anchorView, final Drive drive, final EditText editText) {
        final FragmentActivity activity = getActivity();
        final Fragment $this = ContainerDetailFragment.this;

        PopupMenu popupMenu = new PopupMenu(activity, anchorView);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) popupMenu.setForceShowIcon(true);
        popupMenu.inflate(R.menu.drive_search_popup_menu);
        Menu menu = popupMenu.getMenu();
        SubMenu subMenu = menu.findItem(R.id.menu_item_locations).getSubMenu();
        ArrayList<Container> containers = manager.getContainers();
        for (int i = 0; i < containers.size(); i++) {
            Container container = containers.get(i);
            subMenu.add(0, 0, container.id, container.getName()+" (Drive C:)");
        }

        popupMenu.setOnMenuItemClickListener((menuItem) -> {
            int id = menuItem.getItemId();
            if (id == R.id.menu_item_open_directory) {
                openDirectoryCallback = (path) -> {
                    drive.path = path;
                    editText.setText(path);
                };

                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.fromFile(Environment.getExternalStorageDirectory()));
                activity.startActivityFromFragment($this, intent, MainActivity.OPEN_DIRECTORY_REQUEST_CODE);
            } else if (id == R.id.menu_item_downloads) {
                drive.path = AppUtils.DIRECTORY_DOWNLOADS;
                editText.setText(AppUtils.DIRECTORY_DOWNLOADS);
            } else if (id == R.id.menu_item_internal_storage) {
                drive.path = AppUtils.INTERNAL_STORAGE;
                editText.setText(AppUtils.INTERNAL_STORAGE);
            } else {
                Container container = manager.getContainerById(menuItem.getOrder());
                if (container != null) {
                    String path = container.getRootDir()+"/.wine/drive_c";
                    drive.path = path;
                    editText.setText(path);
                }
            }
            return true;
        });

        popupMenu.show();
    }

    private void loadWineVersionSpinner(final View view, Spinner sWineVersion, final ArrayList<WineInfo> wineInfos) {
        final Context context = getContext();
        sWineVersion.setEnabled(!isEditMode());
        view.findViewById(R.id.LLWineVersion).setVisibility(View.VISIBLE);
        sWineVersion.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, wineInfos));
        if (isEditMode()) {
            WineInfo wineInfo = WineInfo.fromIdentifier(context, container.getWineVersion());
            if (wineInfo != null) AppUtils.setSpinnerSelectionFromValue(sWineVersion, wineInfo.toString());
        }
    }

    private void updateRendererSummary(TextView tvRendererSummary, View anchor) {
        Object rt = anchor.getTag(R.id.rendererType);
        byte rendererType = rt != null ? (byte) rt : Container.RENDERER_GL;
        String type = rendererType == Container.RENDERER_VULKAN ? "Vulkan" : "OpenGL";
        Object nm = anchor.getTag(R.id.rendererNative);
        boolean nativeMode = nm != null && (boolean) nm;
        String summary = type;
        if (nativeMode && rendererType == Container.RENDERER_VULKAN) summary += " +Native";
        tvRendererSummary.setText(summary);
    }

    private void setupControllerMappingSpinner(Spinner spinner, Container.XrControllerMapping mapping, int defaultValue) {
        if (spinner == null) return;
        XKeycode[] values = XKeycode.values();
        ArrayList<String> array = new ArrayList<>();
        for (XKeycode value : values) array.add(value.name());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(spinner.getContext(), android.R.layout.simple_spinner_dropdown_item, array);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        byte keycode = isEditMode() ? container.getControllerMapping(mapping) : (byte) defaultValue;
        int index = -1;
        for (int i = 0; i < values.length; i++) {
            if (values[i].id == keycode) { index = i; break; }
        }
        spinner.setSelection(isEditMode() && index != -1 ? index : defaultValue);
    }

    private String getControllerMapping(View view) {
        int[] ids = {
            R.id.SButtonA, R.id.SButtonB, R.id.SButtonX, R.id.SButtonY, R.id.SButtonGrip, R.id.SButtonTrigger,
            R.id.SThumbstickUp, R.id.SThumbstickDown, R.id.SThumbstickLeft, R.id.SThumbstickRight
        };
        byte[] controllerMapping = new byte[ids.length];
        for (int i = 0; i < ids.length; i++) {
            Spinner spinner = view.findViewById(ids[i]);
            int index = spinner != null ? spinner.getSelectedItemPosition() : 0;
            byte value = XKeycode.values()[index].id;
            controllerMapping[i] = value;
        }
        char[] chars = new char[controllerMapping.length];
        for (int i = 0; i < controllerMapping.length; i++) chars[i] = (char)(controllerMapping[i] & 0xFF);
        return new String(chars);
    }
}
