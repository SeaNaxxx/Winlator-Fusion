package com.winlator.fusion;

import android.app.Activity;
import android.app.PictureInPictureParams;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;

import com.google.android.material.navigation.NavigationView;
import com.winlator.fusion.alsaserver.ALSAClient;
import com.winlator.fusion.container.AudioDrivers;
import com.winlator.fusion.container.Container;
import com.winlator.fusion.container.ContainerManager;
import com.winlator.fusion.container.DXWrappers;
import com.winlator.fusion.container.GraphicsDrivers;
import com.winlator.fusion.container.Shortcut;
import com.winlator.fusion.contentdialog.ActiveWindowsDialog;
import com.winlator.fusion.contents.AdrenotoolsManager;
import com.winlator.fusion.contents.ContentsManager;
import com.winlator.fusion.contentdialog.AudioDriverConfigDialog;
import com.winlator.fusion.contentdialog.ContentDialog;
import com.winlator.fusion.contentdialog.DXVKConfigDialog;
import com.winlator.fusion.contentdialog.DebugDialog;
import com.winlator.fusion.contentdialog.ScreenEffectDialog;
import com.winlator.fusion.contentdialog.TurnipConfigDialog;
import com.winlator.fusion.contentdialog.VKD3DConfigDialog;
import com.winlator.fusion.contentdialog.VirGLConfigDialog;
import com.winlator.fusion.contentdialog.WineD3DConfigDialog;
import com.winlator.fusion.contentdialog.ZinkConfigDialog;
import com.winlator.fusion.core.AppUtils;
import com.winlator.fusion.core.DefaultVersion;
import com.winlator.fusion.core.EnvVars;
import com.winlator.fusion.core.EvshimPatcher;
import com.winlator.fusion.core.FileUtils;
import com.winlator.fusion.core.GeneralComponents;
import com.winlator.fusion.core.KeyValueSet;
import com.winlator.fusion.core.LocaleHelper;
import com.winlator.fusion.core.PreloaderDialog;
import com.winlator.fusion.core.ProcessHelper;
import com.winlator.fusion.core.StringUtils;
import com.winlator.fusion.core.TarCompressorUtils;
import com.winlator.fusion.core.Win32AppWorkarounds;
import com.winlator.fusion.core.WineInfo;
import com.winlator.fusion.core.WineInstaller;
import com.winlator.fusion.core.WineRegistryEditor;
import com.winlator.fusion.core.WineStartMenuCreator;
import com.winlator.fusion.core.WineThemeManager;
import com.winlator.fusion.core.WineRequestHandler;
import com.winlator.fusion.core.WineUtils;
import com.winlator.fusion.inputcontrols.ControlsProfile;
import com.winlator.fusion.inputcontrols.ExternalController;
import com.winlator.fusion.inputcontrols.InputControlsManager;
import com.winlator.fusion.math.Mathf;
import com.winlator.fusion.renderer.GLRenderer;
import com.winlator.fusion.renderer.Renderer;
import com.winlator.fusion.renderer.VulkanRenderer;
import cn.sherlock.com.sun.media.sound.SF2Soundbank;
import com.winlator.fusion.midi.MidiHandler;
import com.winlator.fusion.midi.MidiManager;
import com.winlator.fusion.widget.FrameRating;
import com.winlator.fusion.widget.InputControlsView;
import com.winlator.fusion.widget.MagnifierView;
import com.winlator.fusion.widget.TouchpadView;
import com.winlator.fusion.widget.XServerView;
import com.winlator.fusion.winhandler.GamepadHandler;
import com.winlator.fusion.winhandler.TaskManagerDialog;
import com.winlator.fusion.winhandler.WinHandler;
import com.winlator.fusion.xconnector.UnixSocketConfig;
import com.winlator.fusion.xenvironment.FusionFS;
import com.winlator.fusion.xenvironment.FusionFSInstaller;
import com.winlator.fusion.xenvironment.ImageFs;
import com.winlator.fusion.xenvironment.RootFS;
import com.winlator.fusion.xenvironment.XEnvironment;
import com.winlator.fusion.xenvironment.components.ALSAServerComponent;
import com.winlator.fusion.xenvironment.components.BionicProgramLauncherComponent;
import com.winlator.fusion.xenvironment.components.GuestProgramLauncherComponent;
import com.winlator.fusion.xenvironment.components.NetworkInfoUpdateComponent;
import com.winlator.fusion.xenvironment.components.PulseAudioComponent;
import com.winlator.fusion.xenvironment.components.SysVSharedMemoryComponent;
import com.winlator.fusion.xenvironment.components.VirGLRendererComponent;
import com.winlator.fusion.xenvironment.components.VortekRendererComponent;
import com.winlator.fusion.xenvironment.components.XServerComponent;
import com.winlator.fusion.xserver.Atom;
import com.winlator.fusion.xserver.Property;
import com.winlator.fusion.xserver.ScreenInfo;
import com.winlator.fusion.xserver.Window;
import com.winlator.fusion.xserver.WindowManager;
import com.winlator.fusion.xserver.XServer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Executors;

public class XServerDisplayActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private XServerView xServerView;
    private InputControlsView inputControlsView;
    private TouchpadView touchpadView;
    private XEnvironment environment;
    private DrawerLayout drawerLayout;
    protected Container container;
    private XServer xServer;
    private InputControlsManager inputControlsManager;
    private RootFS rootFS;
    private FrameRating frameRating;
    private Runnable editInputControlsCallback;
    private Shortcut shortcut;
    private String[] graphicsDriver = {GraphicsDrivers.DEFAULT_VULKAN_DRIVER, GraphicsDrivers.DEFAULT_OPENGL_DRIVER};
    private String audioDriver = Container.DEFAULT_AUDIO_DRIVER;
    private String dxwrapper = Container.DEFAULT_DXWRAPPER;
    private ScreenInfo screenInfo = new ScreenInfo(Container.DEFAULT_SCREEN_SIZE);
    private KeyValueSet[] dxwrapperConfig;
    private KeyValueSet[] graphicsDriverConfig;
    private KeyValueSet audioDriverConfig;
    private String wincomponents;
    private WineInfo wineInfo;
    private final EnvVars envVars = new EnvVars();
    private EnvVars overrideEnvVars;
    private ClipboardManager clipboardManager;
    private SharedPreferences preferences;
    private final WinHandler winHandler = new WinHandler(this);
    private float globalCursorSpeed = 1.0f;
    private boolean capturePointerOnExternalMouse = true;
    private MagnifierView magnifierView;
    private DebugDialog debugDialog;
    private int frameRatingWindowId = -1;
    private Win32AppWorkarounds win32AppWorkarounds;
    private String screenEffectProfile;
    private boolean isPaused = false;
    private long sessionStartTime = 0;
    private WineRequestHandler wineRequestHandler;
    private MidiHandler midiHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        AppUtils.setActivityTheme(this);
        super.onCreate(savedInstanceState);
        AppUtils.hideSystemUI(this);
        AppUtils.keepScreenOn(this);
        setContentView(R.layout.xserver_display_activity);

        final PreloaderDialog preloaderDialog = new PreloaderDialog(this);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean useAndroidClipboardOnWine = preferences.getBoolean("use_android_clipboard_on_wine", false);
        clipboardManager = useAndroidClipboardOnWine ? (ClipboardManager)getSystemService(CLIPBOARD_SERVICE) : null;

        drawerLayout = findViewById(R.id.DrawerLayout);
        drawerLayout.setOnApplyWindowInsetsListener((view, windowInsets) -> windowInsets.replaceSystemWindowInsets(0, 0, 0, 0));
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        NavigationView navigationView = findViewById(R.id.NavigationView);
        ProcessHelper.removeAllDebugCallbacks();
        boolean enableLogs = preferences.getBoolean("enable_wine_debug", false) || preferences.getInt("box64_logs", 0) >= 1;
        if (enableLogs) ProcessHelper.addDebugCallback(debugDialog = new DebugDialog(this));
        Menu menu = navigationView.getMenu();
        menu.findItem(R.id.menu_item_logs).setVisible(enableLogs);
        navigationView.setNavigationItemSelectedListener(this);

        rootFS = RootFS.find(this);

        if (!isGenerateWineprefix()) {
            ContainerManager containerManager = new ContainerManager(this);
            container = containerManager.getContainerById(getIntent().getIntExtra("container_id", 0));
            if (container == null) { finish(); return; }
            containerManager.activateContainer(container);

            if (container.isBionic()) {
                rootFS = RootFS.fromDir(FusionFS.find(this).getBionicDir());
                if (!FusionFSInstaller.hasProtonInstalled(this)) {
                    AppUtils.showToast(this, R.string.proton_not_installed);
                    finish();
                    return;
                }
            } else {
                if (!rootFS.isValid()) {
                    AppUtils.showToast(this, R.string.unable_to_install_system_files);
                    finish();
                    return;
                }
            }

            boolean wineprefixNeedsUpdate = container.getExtra("wineprefixNeedsUpdate").equals("t");
            if (wineprefixNeedsUpdate) {
                preloaderDialog.show(R.string.updating_system_files);
                WineUtils.updateWineprefix(this, container, (status) -> {
                    if (status == 0) {
                        container.putExtra("wineprefixNeedsUpdate", null);
                        container.putExtra("wincomponents", null);
                        container.saveData();
                        AppUtils.restartActivity(this);
                    }
                    else finish();
                });
                return;
            }

            win32AppWorkarounds = new Win32AppWorkarounds(this);

            String wineVersion = container.getWineVersion();
            wineInfo = WineInfo.fromIdentifier(this, wineVersion);

            if (container.isBionic()) {
                if (wineInfo != WineInfo.MAIN_WINE_INFO && wineInfo.path != null) {
                    rootFS.setWinePath(wineInfo.path);
                } else {
                    rootFS.setWinePath(FusionFS.find(this).getWinePathForVersion(WineInfo.BIONIC_WINE_IDENTIFIER));
                }
            } else if (wineInfo != WineInfo.MAIN_WINE_INFO) {
                rootFS.setWinePath(wineInfo.path);
            }

            String shortcutPath = getIntent().getStringExtra("shortcut_path");
            if (shortcutPath != null && !shortcutPath.isEmpty()) {
                shortcut = new Shortcut(container, new File(shortcutPath));
                sessionStartTime = System.currentTimeMillis();
            }

            String graphicsDriver = container.getGraphicsDriver();
            audioDriver = container.getAudioDriver();
            String dxwrapper = container.getDXWrapper();
            wincomponents = container.getWinComponents();
            String dxwrapperConfig = container.getDXWrapperConfig();
            String graphicsDriverConfig = container.getGraphicsDriverConfig();
            audioDriverConfig = new KeyValueSet(container.getAudioDriverConfig());
            screenInfo = new ScreenInfo(container.getScreenSize());

            int preferredInputApiIdx = preferences.getInt("preferred_input_api", GamepadHandler.PreferredInputApi.AUTO.ordinal());

            if (shortcut != null) {
                graphicsDriver = shortcut.getExtra("graphicsDriver", container.getGraphicsDriver());
                audioDriver = shortcut.getExtra("audioDriver", container.getAudioDriver());
                dxwrapper = shortcut.getExtra("dxwrapper", container.getDXWrapper());
                wincomponents = shortcut.getExtra("wincomponents", container.getWinComponents());
                dxwrapperConfig = shortcut.getExtra("dxwrapperConfig", container.getDXWrapperConfig());
                graphicsDriverConfig = shortcut.getExtra("graphicsDriverConfig", container.getGraphicsDriverConfig());
                audioDriverConfig = new KeyValueSet(shortcut.getExtra("audioDriverConfig", container.getAudioDriverConfig()));
                screenInfo = new ScreenInfo(shortcut.getExtra("screenSize", container.getScreenSize()));

                String dinputMapperType = shortcut.getExtra("dinputMapperType");
                if (!dinputMapperType.isEmpty()) winHandler.gamepadHandler.setDInputMapperType(Byte.parseByte(dinputMapperType));

                String preferredInputApi = shortcut.getExtra("preferredInputApi");
                if (!preferredInputApi.isEmpty()) preferredInputApiIdx = Byte.parseByte(preferredInputApi);

                win32AppWorkarounds.applyStartupWorkarounds(!shortcut.wmClass.isEmpty() ? shortcut.wmClass : shortcut.path);
            }
            else {
                Intent intent = getIntent();
                if (intent.hasExtra("exec_path")) win32AppWorkarounds.applyStartupWorkarounds(FileUtils.getName(intent.getStringExtra("exec_path")));
            }

            this.graphicsDriver = GraphicsDrivers.parseIdentifiers(graphicsDriver);
            this.graphicsDriverConfig = GraphicsDrivers.parseConfigs(graphicsDriver, graphicsDriverConfig);
            this.dxwrapper = DXWrappers.parseIdentifier(dxwrapper);
            this.dxwrapperConfig = DXWrappers.parseConfigs(dxwrapper, dxwrapperConfig);

            winHandler.gamepadHandler.setPreferredInputApi(GamepadHandler.PreferredInputApi.values()[preferredInputApiIdx]);

            if (container != null) {
                winHandler.setInputType((byte) container.getInputType());
            }
            if (shortcut != null) {
                String shortcutInputType = shortcut.getExtra("inputType");
                if (!shortcutInputType.isEmpty()) winHandler.setInputType(Byte.parseByte(shortcutInputType));
            }
        }

        preloaderDialog.showWithCountdown(R.string.starting_up, 15);

        inputControlsManager = new InputControlsManager(this);
        xServer = new XServer(this, screenInfo);
        xServer.setWinHandler(winHandler);
        final boolean[] flags = {false, shortcut != null || getIntent().hasExtra("exec_path")};
        xServer.windowManager.addOnWindowModificationListener(new WindowManager.OnWindowModificationListener() {
            @Override
            public void onUpdateWindowContent(Window window) {
                if (window.id == frameRatingWindowId) frameRating.update();
            }

            @Override
            public void onMapWindow(Window window) {
                if (!flags[0] && window.isRenderable() && !window.getClassName().isEmpty()) {
                    Renderer r = xServer.getRenderer();
                    if (r != null) r.setCursorVisible(true);
                    preloaderDialog.closeOnUiThread();
                    flags[0] = true;
                }

                if (flags[1] && window.attributes.isViewable() && window.isDesktopWindow()) {
                    window.attributes.setViewable(false);
                    if (window.attributes.isEnabled()) window.disableAllDescendants();
                }

                if (win32AppWorkarounds != null) win32AppWorkarounds.applyWindowWorkarounds(window);
                changeFrameRatingVisibility(window, true);
            }

            @Override
            public void onUnmapWindow(Window window) {
                changeFrameRatingVisibility(window, false);
            }
        });

        setupUI();

        Executors.newSingleThreadExecutor().execute(() -> {
            if (!isGenerateWineprefix()) {
                setupWineSystemFiles();
                extractGraphicsDriverFiles();
                changeWineAudioDriver();
            }
            setupXEnvironment();
        });
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.setSystemLocale(newBase));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MainActivity.EDIT_INPUT_CONTROLS_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (editInputControlsCallback != null) {
                editInputControlsCallback.run();
                editInputControlsCallback = null;
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            if (capturePointerOnExternalMouse) touchpadView.requestPointerCapture();

            if (winHandler != null && clipboardManager != null && clipboardManager.hasPrimaryClip()) {
                ClipData primaryClip = clipboardManager.getPrimaryClip();
                if (primaryClip != null && primaryClip.getItemCount() > 0) {
                    CharSequence text = primaryClip.getItemAt(0).getText();
                    if (text != null) winHandler.setClipboardData(text.toString());
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (environment != null) {
            xServerView.onResume();
            environment.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (environment != null && !isInPictureInPictureMode()) {
            environment.onPause();
            xServerView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        if (shortcut != null && sessionStartTime > 0) {
            long playtimeMillis = System.currentTimeMillis() - sessionStartTime;
            long existingPlaytime = Long.parseLong(shortcut.getExtra("playtime", "0"));
            shortcut.putExtra("playtime", String.valueOf(existingPlaytime + playtimeMillis));
            shortcut.saveData();
            sessionStartTime = 0; // Prevent double-counting
        }
        if (wineRequestHandler != null) wineRequestHandler.stop();
        if (midiHandler != null) midiHandler.stop();
        winHandler.stop();
        if (environment != null) environment.stopEnvironmentComponents();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (environment != null) {
            if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
            else drawerLayout.closeDrawers();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_item_keyboard) {
            AppUtils.showKeyboard(this);
            drawerLayout.closeDrawers();
        } else if (id == R.id.menu_item_input_controls) {
            showInputControlsDialog();
            drawerLayout.closeDrawers();
        } else if (id == R.id.menu_item_toggle_fullscreen) {
            Renderer r = xServer.getRenderer();
            if (r != null) r.toggleFullscreen();
            drawerLayout.closeDrawers();
        } else if (id == R.id.menu_item_task_manager) {
            (new TaskManagerDialog(this)).show();
            drawerLayout.closeDrawers();
        } else if (id == R.id.menu_item_active_windows) {
            (new ActiveWindowsDialog(this)).show();
            drawerLayout.closeDrawers();
        } else if (id == R.id.menu_item_magnifier) {
            if (magnifierView == null) {
                final FrameLayout container = findViewById(R.id.FLXServerDisplay);
                magnifierView = new MagnifierView(this);
                magnifierView.setZoomButtonCallback((value) -> {
                    Renderer r = xServer.getRenderer();
                    if (r != null) r.setMagnifierZoom(Mathf.clamp(r.getMagnifierZoom() + value, 1.0f, 3.0f));
                    if (r != null) magnifierView.setZoomValue(r.getMagnifierZoom());
                });
                magnifierView.setZoomValue(xServer.getRenderer() != null ? xServer.getRenderer().getMagnifierZoom() : 1.0f);
                magnifierView.setHideButtonCallback(() -> {
                    container.removeView(magnifierView);
                    magnifierView = null;
                });
                container.addView(magnifierView);
            }
            drawerLayout.closeDrawers();
        } else if (id == R.id.menu_item_screen_effect) {
            (new ScreenEffectDialog(this)).show();
            drawerLayout.closeDrawers();
        } else if (id == R.id.menu_item_pip_mode) {
            PictureInPictureParams pipParams = (new PictureInPictureParams.Builder())
                .setAspectRatio(screenInfo.aspectRatio())
                .build();
            enterPictureInPictureMode(pipParams);
            drawerLayout.closeDrawers();
        } else if (id == R.id.menu_item_logs) {
            debugDialog.show();
            drawerLayout.closeDrawers();
        } else if (id == R.id.menu_item_touchpad_help) {
            showTouchpadHelpDialog();
        } else if (id == R.id.menu_item_pause) {
            if (isPaused) {
                if (environment != null) environment.onResume();
                isPaused = false;
                item.setTitle(R.string.pause_processes);
            } else {
                if (environment != null) environment.onPause();
                isPaused = true;
                item.setTitle(R.string.resume_processes);
            }
            drawerLayout.closeDrawers();
        } else if (id == R.id.menu_item_exit) {
            exit();
        }
        return true;
    }

    public SharedPreferences getPreferences() {
        return preferences;
    }

    private void exit() {
        if (shortcut != null && sessionStartTime > 0) {
            long playtimeMillis = System.currentTimeMillis() - sessionStartTime;
            long existingPlaytime = Long.parseLong(shortcut.getExtra("playtime", "0"));
            shortcut.putExtra("playtime", String.valueOf(existingPlaytime + playtimeMillis));
            shortcut.saveData();
            sessionStartTime = 0; // Prevent double-counting
        }
        if (wineRequestHandler != null) wineRequestHandler.stop();
        if (midiHandler != null) midiHandler.stop();
        winHandler.stop();
        if (environment != null) environment.stopEnvironmentComponents();

        Intent intent = getIntent();
        if (intent.hasExtra("exec_path")) {
            AppUtils.RestartApplicationOptions options = new AppUtils.RestartApplicationOptions();
            options.containerId = container.id;
            options.startPath = FileUtils.getDirname(intent.getStringExtra("exec_path"));
            AppUtils.restartApplication(this, options);
        }
        else AppUtils.restartApplication(this);
    }

    private void setupWineSystemFiles() {
        String appVersion = String.valueOf(AppUtils.getVersionCode(this));
        String rfsVersion = String.valueOf(rootFS.getVersion());
        boolean containerDataChanged = false;

        boolean wineprefixWasUpdated = WineUtils.isWineprefixWasUpdated(container);
        if (!container.getExtra("appVersion").equals(appVersion) || !container.getExtra("rfsVersion").equals(rfsVersion) || wineprefixWasUpdated) {
            applyGeneralPatches(container);
            container.putExtra("appVersion", appVersion);
            container.putExtra("rfsVersion", rfsVersion);
            containerDataChanged = true;
        }

        if (verifyUserRegistry()) containerDataChanged = true;
        if (extractDXWrapperFiles()) containerDataChanged = true;

        if (!wincomponents.equals(container.getExtra("wincomponents"))) {
            extractWinComponentFiles();
            container.putExtra("wincomponents", wincomponents);
            containerDataChanged = true;
        }

        String desktopTheme = container.getDesktopTheme();
        if (!(desktopTheme+","+xServer.screenInfo).equals(container.getExtra("desktopTheme"))) {
            WineThemeManager.apply(this, new WineThemeManager.ThemeInfo(desktopTheme), xServer.screenInfo, FusionFS.find(this).getDirForVariant(container.getContainerVariant()));
            container.putExtra("desktopTheme", desktopTheme+","+xServer.screenInfo);
            containerDataChanged = true;
        }

        WineStartMenuCreator.create(this, container);
        WineUtils.createDosdevicesSymlinks(container, true);

        String startupSelection = String.valueOf(container.getStartupSelection());
        if (!startupSelection.equals(container.getExtra("startupSelection")) || wineprefixWasUpdated) {
            WineUtils.changeServicesStatus(container, container.getStartupSelection() != Container.STARTUP_SELECTION_NORMAL);
            container.putExtra("startupSelection", startupSelection);
            containerDataChanged = true;
        }

        boolean openAndroidBrowserFromWine = preferences.getBoolean("open_android_browser_from_wine", true);
        String openAndroidBrowserFromWineStr = openAndroidBrowserFromWine ? "t" : "f";
        if (!openAndroidBrowserFromWineStr.equals(container.getExtra("openAndroidBrowserFromWine")) || wineprefixWasUpdated) {
            WineUtils.changeBrowsersRegistryKey(container, openAndroidBrowserFromWine);
            container.putExtra("openAndroidBrowserFromWine", openAndroidBrowserFromWineStr);
            containerDataChanged = true;
        }

        String currentInputType = String.valueOf(container.getInputType());
        String currentExclusiveXInput = String.valueOf(container.isExclusiveXInput());
        if (shortcut != null) {
            String shortcutInputType = shortcut.getExtra("inputType", "");
            if (!shortcutInputType.isEmpty()) currentInputType = shortcutInputType;
            String shortcutExclusive = shortcut.getExtra("exclusiveXInput", "");
            if (!shortcutExclusive.isEmpty()) currentExclusiveXInput = shortcutExclusive;
        }
        if (!currentInputType.equals(container.getExtra("inputType")) || !currentExclusiveXInput.equals(container.getExtra("exclusiveXInput")) || wineprefixWasUpdated) {
            int inputType = Integer.parseInt(currentInputType);
            boolean dinputEnabled = (inputType & WinHandler.FLAG_INPUT_TYPE_DINPUT) == WinHandler.FLAG_INPUT_TYPE_DINPUT;
            boolean exclusiveXInput = currentExclusiveXInput.equals("true") || currentExclusiveXInput.equals("1");
            WineUtils.setJoystickRegistryKeys(container, dinputEnabled, exclusiveXInput);
            container.putExtra("inputType", currentInputType);
            container.putExtra("exclusiveXInput", currentExclusiveXInput);
            containerDataChanged = true;
        }

        if (containerDataChanged) container.saveData();
    }

    private void setupXEnvironment() {
        boolean isBionic = container != null && container.isBionic();

        if (isGenerateWineprefix()) {
            WineInfo genWineInfo = getIntent().getParcelableExtra("wine_info");
            if (genWineInfo != null && genWineInfo.isProton()) {
                isBionic = true;
                if (rootFS == RootFS.find(this)) {
                    rootFS = RootFS.fromDir(FusionFS.find(this).getBionicDir());
                }
            }
        }

        com.winlator.fusion.runtime.RuntimeEnvironment runtimeEnv;
        if (isBionic) {
            runtimeEnv = new com.winlator.fusion.runtime.BionicRuntimeEnvironment(this);
        } else {
            runtimeEnv = new com.winlator.fusion.runtime.GlibcRuntimeEnvironment(this);
        }

        com.winlator.fusion.runtime.RuntimeProfile profile = runtimeEnv.getProfile();
        rootFS = runtimeEnv.getRootFSAdapter().getRootFS();
        String rootPath = profile.getRootDir().getPath();

        boolean enableWineDebug = preferences.getBoolean("enable_wine_debug", false);
        String wineDebugChannels = preferences.getString("wine_debug_channels", SettingsFragment.DEFAULT_WINE_DEBUG_CHANNELS);
        envVars.put("WINEDEBUG", enableWineDebug && !wineDebugChannels.isEmpty() ? "+"+wineDebugChannels.replace(",", ",+") : "-all");

        runtimeEnv.prepare();
        runtimeEnv.setupBaseEnvVars(envVars, wineInfo);

        if (container != null) {
            if (container.getHUDMode() == FrameRating.Mode.FULL.ordinal()) envVars.put("X11_WND_GPU_INFO", "1");
            if (container.getStartupSelection() == Container.STARTUP_SELECTION_AGGRESSIVE) winHandler.killProcess("services.exe");

            envVars.putAll(container.getEnvVars());
            if (shortcut != null) envVars.putAll(shortcut.getExtra("envVars"));
            if (!envVars.has("WINEESYNC")) envVars.put("WINEESYNC", "1");

            String lcAll = container.getLC_ALL();
            if (shortcut != null) {
                String shortcutLcAll = shortcut.getExtra("lc_all", "");
                if (!shortcutLcAll.isEmpty()) lcAll = shortcutLcAll;
            }
            if (!lcAll.isEmpty()) envVars.put("LC_ALL", lcAll);

            String vkbasaltConfig = "";
            if (shortcut != null) {
                String sharpnessEffect = shortcut.getExtra("sharpnessEffect", "None");
                if (!sharpnessEffect.equals("None")) {
                    double sharpnessLevel = Double.parseDouble(shortcut.getExtra("sharpnessLevel", "100"));
                    double sharpnessDenoise = Double.parseDouble(shortcut.getExtra("sharpnessDenoise", "100"));
                    vkbasaltConfig = "effects=" + sharpnessEffect.toLowerCase() + ";"
                        + "casSharpness=" + sharpnessLevel / 100 + ";"
                        + "dlsSharpness=" + sharpnessLevel / 100 + ";"
                        + "dlsDenoise=" + sharpnessDenoise / 100 + ";"
                        + "enableOnLaunch=True";
                }
            }
            if (!vkbasaltConfig.isEmpty()) {
                envVars.put("ENABLE_VKBASALT", "1");
                envVars.put("VKBASALT_CONFIG", vkbasaltConfig);
            }
        }

        environment = new XEnvironment(this, rootFS, xServer);
        environment.addComponent(new SysVSharedMemoryComponent(xServer, UnixSocketConfig.create(rootPath, UnixSocketConfig.SYSVSHM_SERVER_PATH)));
        environment.addComponent(new XServerComponent(xServer, UnixSocketConfig.create(rootPath, UnixSocketConfig.XSERVER_PATH)));
        environment.addComponent(new NetworkInfoUpdateComponent());

        runtimeEnv.setupGraphics(environment, envVars, graphicsDriver, graphicsDriverConfig);
        runtimeEnv.setupAudio(environment, envVars, audioDriver, audioDriverConfig);

        if (container != null) {
            String desktopName = shortcut != null || getIntent().hasExtra("exec_path") ? "nogui" : "shell";
            String guestExecutable = "wine explorer /desktop="+desktopName+","+xServer.screenInfo+" "+getWineStartCommand();
            String box64Preset = shortcut != null ? shortcut.getExtra("box64Preset", container.getBox64Preset()) : container.getBox64Preset();
            runtimeEnv.setupLauncher(environment, container, wineInfo, guestExecutable, envVars, box64Preset, (status) -> exit());
        }

        runtimeEnv.postSetup(environment, container);

        if (isGenerateWineprefix()) {
            wineInfo = getIntent().getParcelableExtra("wine_info");
            if (wineInfo != null) WineInstaller.generateWineprefix(wineInfo, environment);
        }
        if (overrideEnvVars != null) {
            envVars.putAll(overrideEnvVars);
            overrideEnvVars = null;
        }
        environment.startEnvironmentComponents();

        winHandler.start();
        wineRequestHandler = new WineRequestHandler(this);
        wineRequestHandler.start();

        String midiSoundFont = container != null ? container.getMIDISoundFont() : "";
        if (shortcut != null) {
            String shortcutMidi = shortcut.getExtra("midiSoundFont", "");
            if (!shortcutMidi.isEmpty()) midiSoundFont = shortcutMidi;
        }
        if (!midiSoundFont.isEmpty()) {
            try {
                midiHandler = new MidiHandler();
                SF2Soundbank soundbank;
                if (midiSoundFont.equals(MidiManager.DEFAULT_SF2_FILE)) {
                    soundbank = new SF2Soundbank(getAssets().open(MidiManager.SF2_ASSETS_DIR + "/" + MidiManager.DEFAULT_SF2_FILE));
                } else {
                    soundbank = new SF2Soundbank(new File(MidiManager.getSoundFontDir(this), midiSoundFont));
                }
                midiHandler.setSoundBank(soundbank);
                midiHandler.start();
            } catch (Exception e) {
                midiHandler = null;
            }
        }

        envVars.clear();
        graphicsDriver = null;
        dxwrapperConfig = null;
        graphicsDriverConfig = null;
        audioDriver = null;
        audioDriverConfig = null;
        wincomponents = null;
    }

    private void setupUI() {
        FrameLayout rootView = findViewById(R.id.FLXServerDisplay);
        boolean useVulkan = !isGenerateWineprefix() && container != null && container.isVulkanRenderer();
        xServerView = new XServerView(this, xServer, useVulkan);

        if (useVulkan) {
            VulkanRenderer vkRenderer = xServerView.getVulkanRenderer();
            vkRenderer.setCursorVisible(false);
            vkRenderer.setVkPresentMode(container.getRendererPresentMode());
            vkRenderer.setFilterMode(container.getRendererFilterMode());
            vkRenderer.setNativeMode(container.isRendererNative());
            if (container.getRendererRefreshRate() > 0) {
                vkRenderer.setRefreshRateLimit(container.getRendererRefreshRate());
            }
            xServer.setRenderer(vkRenderer);
        } else {
            final GLRenderer renderer = xServerView.getRenderer();
            renderer.setCursorVisible(false);
            renderer.setCursorColor(preferences.getInt("cursor_color", 0xffffff));
            renderer.setCursorScale(preferences.getFloat("cursor_scale", 1.0f));
            renderer.setForceWindowsFullscreen(shortcut != null && shortcut.getExtra("forceFullscreen", "0").equals("1"));
            xServer.setRenderer(renderer);
        }

        if (container != null) {
            boolean shouldStretch = container.isFullscreenStretched();
            if (shortcut != null) {
                String shortcutStretched = shortcut.getExtra("fullscreenStretched", "");
                if (!shortcutStretched.isEmpty()) shouldStretch = shortcutStretched.equals("1");
            }
            if (shouldStretch) {
                Renderer r = xServer.getRenderer();
                if (r != null) r.toggleFullscreen();
            }
        }

        rootView.addView(xServerView);

        globalCursorSpeed = preferences.getFloat("cursor_speed", 1.0f);
        capturePointerOnExternalMouse = preferences.getBoolean("capture_pointer_on_external_mouse", true);
        touchpadView = new TouchpadView(this, xServer, capturePointerOnExternalMouse);
        touchpadView.setSensitivity(globalCursorSpeed);
        touchpadView.setMoveCursorToTouchpoint(preferences.getBoolean("move_cursor_to_touchpoint", false));
        touchpadView.setFourFingersTapCallback(() -> {
            if (!drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.openDrawer(GravityCompat.START);
        });
        rootView.addView(touchpadView);

        inputControlsView = new InputControlsView(this);
        inputControlsView.setOverlayOpacity(preferences.getFloat("overlay_opacity", InputControlsView.DEFAULT_OVERLAY_OPACITY));
        inputControlsView.setTouchpadView(touchpadView);
        inputControlsView.setXServer(xServer);
        inputControlsView.setVisibility(View.GONE);
        rootView.addView(inputControlsView);

        if (container != null && container.getHUDMode() != FrameRating.Mode.DISABLED.ordinal()) {
            frameRating = new FrameRating(this);
            frameRating.setMode(FrameRating.Mode.values()[container.getHUDMode()]);
            frameRating.setVisibility(View.GONE);
            rootView.addView(frameRating);
        }

        if (shortcut != null) {
            String controlsProfile = shortcut.getExtra("controlsProfile");
            if (!controlsProfile.isEmpty()) {
                ControlsProfile profile = inputControlsManager.getProfile(Integer.parseInt(controlsProfile));
                if (profile != null) showInputControls(profile);
            }
        }

        if (MainActivity.DEBUG_MODE) rootView.addView(AppUtils.createDebugMsgTextView(this));
        AppUtils.observeSoftKeyboardVisibility(drawerLayout, (visible) -> {
            Renderer r = xServer.getRenderer();
            if (r != null) r.setScreenOffsetYRelativeToCursor(visible);
        });
    }

    private void showInputControlsDialog() {
        final ContentDialog dialog = new ContentDialog(this, R.layout.input_controls_dialog);
        dialog.setTitle(R.string.input_controls);
        dialog.setIcon(R.drawable.icon_input_controls);

        final Spinner sProfile = dialog.findViewById(R.id.SProfile);
        Runnable loadProfileSpinner = () -> {
            ArrayList<ControlsProfile> profiles = inputControlsManager.getProfiles(true);
            ArrayList<String> profileItems = new ArrayList<>();
            int selectedPosition = 0;
            profileItems.add("-- "+getString(R.string.disabled)+" --");
            for (int i = 0; i < profiles.size(); i++) {
                ControlsProfile profile = profiles.get(i);
                if (profile == inputControlsView.getProfile()) selectedPosition = i + 1;
                profileItems.add(profile.getName());
            }

            sProfile.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, profileItems));
            sProfile.setSelection(selectedPosition);
        };
        loadProfileSpinner.run();

        final CheckBox cbRelativeMouseMovement = dialog.findViewById(R.id.CBRelativeMouseMovement);
        cbRelativeMouseMovement.setChecked(xServer.isRelativeMouseMovement());

        final CheckBox cbShowTouchscreenControls = dialog.findViewById(R.id.CBShowTouchscreenControls);
        cbShowTouchscreenControls.setChecked(inputControlsView.isShowTouchscreenControls());

        dialog.findViewById(R.id.BTSettings).setOnClickListener((v) -> {
            int position = sProfile.getSelectedItemPosition();
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("edit_input_controls", true);
            intent.putExtra("selected_profile_id", position > 0 ? inputControlsManager.getProfiles().get(position - 1).id : 0);
            editInputControlsCallback = () -> {
                hideInputControls();
                inputControlsManager.loadProfiles(true);
                loadProfileSpinner.run();
            };
            startActivityForResult(intent, MainActivity.EDIT_INPUT_CONTROLS_REQUEST_CODE);
        });

        dialog.setOnConfirmCallback(() -> {
            xServer.setRelativeMouseMovement(cbRelativeMouseMovement.isChecked());
            inputControlsView.setShowTouchscreenControls(cbShowTouchscreenControls.isChecked());
            int position = sProfile.getSelectedItemPosition();
            if (position > 0) {
                showInputControls(inputControlsManager.getProfiles().get(position - 1));
            }
            else hideInputControls();
        });

        dialog.show();
    }

    private void showInputControls(ControlsProfile profile) {
        inputControlsView.setVisibility(View.VISIBLE);
        inputControlsView.requestFocus();
        inputControlsView.setProfile(profile);

        touchpadView.setSensitivity(profile.getCursorSpeed() * globalCursorSpeed);
        touchpadView.setPointerButtonRightEnabled(false);

        if (profile.isDisableMouseInput()) {
            Renderer r = xServer.getRenderer(); if (r != null) r.setCursorVisible(false);
            touchpadView.setEnabled(false);
        }
        else {
            Renderer r = xServer.getRenderer(); if (r != null) r.setCursorVisible(true);
            touchpadView.setEnabled(true);
        }

        inputControlsView.invalidate();
    }

    private void hideInputControls() {
        inputControlsView.setShowTouchscreenControls(true);
        inputControlsView.setVisibility(View.GONE);
        inputControlsView.setProfile(null);

        touchpadView.setSensitivity(globalCursorSpeed);
        touchpadView.setPointerButtonLeftEnabled(true);
        touchpadView.setPointerButtonRightEnabled(true);

        if (!touchpadView.isEnabled()) {
            touchpadView.setEnabled(true);
            Renderer r = xServer.getRenderer(); if (r != null) r.setCursorVisible(true);
        }

        inputControlsView.invalidate();
    }

    private void extractGraphicsDriverFiles() {
        envVars.put("vblank_mode", "0");

        String cacheId = "";
        if (graphicsDriver[0].equals(GraphicsDrivers.TURNIP)) {
            cacheId += graphicsDriver[0]+"-"+graphicsDriverConfig[0].get("version", DefaultVersion.TURNIP);
        }
        else cacheId += graphicsDriver[0]+"-"+DefaultVersion.valueOf(graphicsDriver[0]);
        cacheId += "-"+graphicsDriver[1]+"-"+DefaultVersion.valueOf(graphicsDriver[1]);

        boolean changed = !cacheId.equals(container.getExtra("graphicsDriver"));
        File rootDir = rootFS.getRootDir();
        File libDir = rootFS.getLibDir();

        if (changed) {
            FileUtils.delete(new File(libDir, "libvulkan_freedreno.so"));
            FileUtils.delete(new File(libDir, "libvulkan_vortek.so"));
            FileUtils.delete(new File(libDir, "libGL.so.1.7.0"));

            File vulkanICDDir = new File(rootDir, "/usr/share/vulkan/icd.d");
            FileUtils.delete(vulkanICDDir);
            vulkanICDDir.mkdirs();

            container.putExtra("graphicsDriver", cacheId);
            container.saveData();
        }

        if (graphicsDriver[0].equals(GraphicsDrivers.TURNIP)) {
            envVars.put("VK_ICD_FILENAMES", rootDir + "/usr/share/vulkan/icd.d/freedreno_icd.aarch64.json");
            envVars.put("MESA_VK_WSI_PRESENT_MODE", "mailbox");
            boolean isBionic = container != null && container.isBionic();
            if (!isBionic) {
                envVars.put("TU_DEBUG", "noconform");
            }
            TurnipConfigDialog.setEnvVars(this, graphicsDriverConfig[0], envVars);

            if (changed) {
                String version = graphicsDriverConfig[0].get("version", DefaultVersion.TURNIP);
                File turnipDest = rootDir; // rootFS already switched to imagefs for Bionic containers
                GeneralComponents.extractFile(GeneralComponents.Type.TURNIP, this, version, DefaultVersion.TURNIP, turnipDest, null);
            }
        }
        else if (graphicsDriver[0].equals(GraphicsDrivers.VORTEK)) {
            envVars.put("VK_ICD_FILENAMES", rootDir + "/usr/share/vulkan/icd.d/vortek_icd.aarch64.json");

            if (changed || MainActivity.DEBUG_MODE) {
                TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "graphics_driver/vortek-" + DefaultVersion.VORTEK + ".tzst", rootDir);
            }
        }
        else if (graphicsDriver[0].equals(GraphicsDrivers.WRAPPER)) {
            envVars.put("VK_ICD_FILENAMES", rootDir + "/usr/share/vulkan/icd.d/wrapper_icd.aarch64.json");
            envVars.put("MESA_VK_WSI_PRESENT_MODE", "mailbox");

            String vkMaxVersion = graphicsDriverConfig[0].get("vkMaxVersion");
            if (vkMaxVersion != null && !vkMaxVersion.isEmpty() && !vkMaxVersion.equals("0")) {
                envVars.put("WRAPPER_VK_VERSION", vkMaxVersion);
            } else {
                envVars.put("WRAPPER_VK_VERSION", "1.3");
            }
            String maxDeviceMemory = graphicsDriverConfig[0].get("maxDeviceMemory");
            if (maxDeviceMemory != null && !maxDeviceMemory.isEmpty() && !maxDeviceMemory.equals("0")) {
                envVars.put("WRAPPER_MAX_DEVICE_MEMORY", maxDeviceMemory);
            }

            if (changed) {
                TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "graphics_driver/wrapper.tzst", rootDir);
            }

            boolean isBionicContainer = container != null && container.isBionic();
            if (isBionicContainer) {
                String adrenotoolsDriverId = graphicsDriverConfig[0].get("adrenotoolsDriver");
                if (adrenotoolsDriverId == null || adrenotoolsDriverId.isEmpty()) {
                    adrenotoolsDriverId = container.getExtra("adrenotoolsDriverId");
                }
                if (adrenotoolsDriverId != null && !adrenotoolsDriverId.isEmpty()) {
                    AdrenotoolsManager adrenotoolsManager = new AdrenotoolsManager(this);
                    adrenotoolsManager.setDriverById(envVars, ImageFs.find(this), adrenotoolsDriverId);
                }
            }
        }

        boolean isBionicContainer = container != null && container.isBionic();

        switch (graphicsDriver[1]) {
            case GraphicsDrivers.ZINK:
                envVars.put("GALLIUM_DRIVER", "zink");
                envVars.put("ZINK_CONTEXT_THREADED", "1");
                if (graphicsDriver[0].equals(GraphicsDrivers.VORTEK)) envVars.put("MESA_GL_VERSION_OVERRIDE", "3.3");
                if (isBionicContainer) envVars.put("MESA_GL_VERSION_OVERRIDE", "4.5");
                ZinkConfigDialog.setEnvVars(graphicsDriverConfig[1], envVars);

                if (changed) {
                    TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "graphics_driver/zink-"+DefaultVersion.ZINK+".tzst", rootDir);
                    if (isBionicContainer) {
                        TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "graphics_driver/zink_dlls.tzst", rootDir);
                    }
                }
                break;
            case GraphicsDrivers.VIRGL:
                envVars.put("GALLIUM_DRIVER", "virpipe");
                envVars.put("VIRGL_NO_READBACK", "true");
                envVars.put("VIRGL_SERVER_PATH", rootDir+UnixSocketConfig.VIRGL_SERVER_PATH);
                VirGLConfigDialog.setEnvVars(graphicsDriverConfig[1], envVars);

                if (changed) TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "graphics_driver/virgl-"+DefaultVersion.VIRGL+".tzst", rootDir);
                break;
            case GraphicsDrivers.GLADIO:
                envVars.put("GLADIO_NO_ERROR", "1");

                if (changed || MainActivity.DEBUG_MODE) TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "graphics_driver/gladio-"+DefaultVersion.GLADIO+".tzst", rootDir);
                break;
        }
    }

    private void showTouchpadHelpDialog() {
        ContentDialog dialog = new ContentDialog(this, R.layout.touchpad_help_dialog);
        dialog.setTitle(R.string.touchpad_help);
        dialog.setIcon(R.drawable.icon_help);
        dialog.findViewById(R.id.BTCancel).setVisibility(View.GONE);
        dialog.show();
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        return !winHandler.onGenericMotionEvent(event) && !touchpadView.onExternalMouseEvent(event) && super.dispatchGenericMotionEvent(event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return (!inputControlsView.onKeyEvent(event) && !winHandler.onKeyEvent(event) && xServer.keyboard.onKeyEvent(event)) ||
               (!ExternalController.isGameController(event.getDevice()) && super.dispatchKeyEvent(event));
    }

    public InputControlsView getInputControlsView() {
        return inputControlsView;
    }

    private boolean extractDXWrapperFiles() {
        String cacheId = "";
        if (dxwrapper.equals(DXWrappers.DXVK)) {
            DXVKConfigDialog.setEnvVars(this, dxwrapperConfig[0], envVars, rootFS.getRootDir());
            cacheId += dxwrapper+"-"+dxwrapperConfig[0].get("version", DefaultVersion.DXVK(graphicsDriver[0]));
        }
        else if (dxwrapper.equals(DXWrappers.WINED3D)) {
            WineD3DConfigDialog.setEnvVars(dxwrapperConfig[0], envVars);
            cacheId += dxwrapper+"-"+dxwrapperConfig[0].get("version", DefaultVersion.WINED3D);
        }

        String ddrawWrapper = dxwrapperConfig[0].get("ddrawWrapper", DXWrappers.WINED3D);
        cacheId += "-"+DXWrappers.VKD3D+"-"+dxwrapperConfig[1].get("version", DefaultVersion.VKD3D)+"-"+ddrawWrapper;
        boolean changed = !cacheId.equals(container.getExtra("dxwrapper"));
        VKD3DConfigDialog.setEnvVars(dxwrapperConfig[1], envVars);

        if (ddrawWrapper.equals(DXWrappers.CNC_DDRAW)) envVars.put("CNC_DDRAW_CONFIG_FILE", "C:\\ProgramData\\cnc-ddraw\\ddraw.ini");

        if (!changed) return false;
        container.putExtra("dxwrapper", cacheId);

        File rootDir = rootFS.getRootDir();
        File windowsDir = new File(rootDir, RootFS.WINEPREFIX+"/drive_c/windows");

        if (dxwrapper.equals(DXWrappers.WINED3D)) {
            String version = dxwrapperConfig[0].get("version", DefaultVersion.WINED3D);
            if (version.equals(WineInfo.MAIN_WINE_VERSION)) {
                final String[] dlls = {"d3d8.dll", "d3d9.dll", "d3d10.dll", "d3d10_1.dll", "d3d10core.dll", "d3d11.dll", "d3d12.dll", "d3d12core.dll", "dxgi.dll", "ddraw.dll", "wined3d.dll"};
                restoreBuiltinDllFiles(dlls);
            }
            else GeneralComponents.extractFile(GeneralComponents.Type.WINED3D, this, version, DefaultVersion.WINED3D, windowsDir, null);
        }
        else if (dxwrapper.equals(DXWrappers.DXVK)) {
            final boolean[] hasD3D8DllFile = {false};
            final boolean[] hasD3D10DllFile = {false};

            boolean isBionicArm64EC = wineInfo != null && wineInfo.isArm64EC() &&
                container != null && container.isBionic();
            if (isBionicArm64EC) {
                String dxvkVersion = dxwrapperConfig[0].get("version", DefaultVersion.MAJOR_DXVK);
                String arm64ecAsset = null;
                if (dxvkVersion.equals("2.4.1") || dxvkVersion.equals("2.3.1")) {
                    arm64ecAsset = "dxwrapper/dxvk-2.3.1-arm64ec-gplasync.tzst";
                } else if (dxvkVersion.equals("1.10.3")) {
                    arm64ecAsset = "dxwrapper/dxvk-1.10.3-arm64ec-async.tzst";
                }
                if (arm64ecAsset != null) {
                    TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, arm64ecAsset, windowsDir, (destination, size) -> {
                        String name = destination.getName();
                        if (name.equals("d3d10.dll")) hasD3D10DllFile[0] = true;
                        else if (name.equals("d3d8.dll")) hasD3D8DllFile[0] = true;
                        return destination;
                    });
                } else {
                    GeneralComponents.extractFile(GeneralComponents.Type.DXVK, this, dxwrapperConfig[0].get("version"), DefaultVersion.DXVK(graphicsDriver[0]), windowsDir, (destination, size) -> {
                        String name = destination.getName();
                        if (name.equals("d3d10.dll")) hasD3D10DllFile[0] = true;
                        else if (name.equals("d3d8.dll")) hasD3D8DllFile[0] = true;
                        return destination;
                    });
                }
            } else {
                GeneralComponents.extractFile(GeneralComponents.Type.DXVK, this, dxwrapperConfig[0].get("version"), DefaultVersion.DXVK(graphicsDriver[0]), windowsDir, (destination, size) -> {
                    String name = destination.getName();
                    if (name.equals("d3d10.dll")) {
                        hasD3D10DllFile[0] = true;
                    }
                    else if (name.equals("d3d8.dll")) {
                        hasD3D8DllFile[0] = true;
                    }
                    return destination;
                });
            }

            if (!hasD3D8DllFile[0]) {
                TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "dxwrapper/d8vk-"+DefaultVersion.D8VK+".tzst", windowsDir);
            }
            if (!hasD3D10DllFile[0]) restoreBuiltinDllFiles("d3d10.dll", "d3d10_1.dll");
        }

        boolean useArm64ecVkd3d = wineInfo != null && wineInfo.isArm64EC() &&
            container != null && container.isBionic();
        if (useArm64ecVkd3d) {
            String vkd3dVersion = dxwrapperConfig[1].get("version", DefaultVersion.VKD3D);
            String arm64ecVkd3d = "dxwrapper/vkd3d-" + vkd3dVersion + "-arm64ec.tzst";
            try {
                getAssets().open(arm64ecVkd3d).close();
                TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, arm64ecVkd3d, windowsDir);
            } catch (Exception e) {
                GeneralComponents.extractFile(GeneralComponents.Type.VKD3D, this, dxwrapperConfig[1].get("version"), DefaultVersion.VKD3D, windowsDir, null);
            }
        } else {
            GeneralComponents.extractFile(GeneralComponents.Type.VKD3D, this, dxwrapperConfig[1].get("version"), DefaultVersion.VKD3D, windowsDir, null);
        }

        if (ddrawWrapper.equals(DXWrappers.CNC_DDRAW)) {
            final String assetDir = "dxwrapper/cnc-ddraw-"+DefaultVersion.CNC_DDRAW;
            File configFile = new File(rootDir, RootFS.WINEPREFIX+"/drive_c/ProgramData/cnc-ddraw/ddraw.ini");
            if (!configFile.isFile()) FileUtils.copy(this, assetDir+"/ddraw.ini", configFile);
            File shadersDir = new File(rootDir, RootFS.WINEPREFIX+"/drive_c/ProgramData/cnc-ddraw/Shaders");
            FileUtils.delete(shadersDir);
            FileUtils.copy(this, assetDir+"/Shaders", shadersDir);
            TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, assetDir+"/ddraw.tzst", windowsDir);
        }
        else restoreBuiltinDllFiles("ddraw.dll");
        return true;
    }

    private void extractWinComponentFiles() {
        File rootDir = rootFS.getRootDir();
        File windowsDir = new File(rootDir, RootFS.WINEPREFIX+"/drive_c/windows");
        File systemRegFile = new File(rootDir, RootFS.WINEPREFIX+"/system.reg");

        try {
            JSONObject wincomponentsJSONObject = new JSONObject(FileUtils.readString(this, "wincomponents/wincomponents.json"));
            Iterator<String[]> oldWinComponentsIter = new KeyValueSet(container.getExtra("wincomponents", Container.FALLBACK_WINCOMPONENTS)).iterator();
            ArrayList<String> builtinDlls = new ArrayList<>();

            for (String[] wincomponent : new KeyValueSet(wincomponents)) {
                if (wincomponent[1].equals(oldWinComponentsIter.next()[1])) continue;
                String identifier = wincomponent[0];
                boolean useNative = wincomponent[1].equals("1");

                if (useNative) {
                    TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "wincomponents/"+identifier+".tzst", windowsDir);
                }
                else {
                    JSONObject wincomponentJSONObject = wincomponentsJSONObject.getJSONObject(identifier);
                    if (wincomponentJSONObject.getBoolean("restoreBuiltinDlls")) {
                        JSONArray dlnames = wincomponentJSONObject.getJSONArray("dlnames");
                        for (int i = 0; i < dlnames.length(); i++) {
                            String dlname = dlnames.getString(i);
                            builtinDlls.add(!dlname.endsWith(".exe") ? dlname+".dll" : dlname);
                        }
                    }
                    else {
                        TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "wincomponents/"+identifier+".tzst", windowsDir, (destination, size) -> {
                            String name = destination.getName();
                            if (name.endsWith(".dll") || name.endsWith(".manifest") || name.endsWith("_deadbeef")) FileUtils.delete(destination);
                            return null;
                        });
                    }
                }

                WineUtils.setWinComponentRegistryKeys(systemRegFile, identifier, useNative);
            }

            if (!builtinDlls.isEmpty()) restoreBuiltinDllFiles(builtinDlls.toArray(new String[0]));
            WineUtils.overrideWinComponentDlls(this, container, wincomponents);
        }
        catch (JSONException e) {}
    }

    private void restoreBuiltinDllFiles(final String... dlls) {
        File rootDir = rootFS.getRootDir();
        File wineDir = new File(rootDir, rootFS.getWinePath());
        
        boolean isArm64EC = wineInfo != null && wineInfo.isArm64EC();
        String system32Arch = isArm64EC ? "aarch64-windows" : "x86_64-windows";
        
        File wineSystem32Dir = new File(wineDir, "/lib/wine/" + system32Arch);
        File wineSysWoW64Dir = new File(wineDir, "/lib/wine/i386-windows");
        File containerSystem32Dir = new File(rootDir, RootFS.WINEPREFIX+"/drive_c/windows/system32");
        File containerSysWoW64Dir = new File(rootDir, RootFS.WINEPREFIX+"/drive_c/windows/syswow64");

        for (String dll : dlls) {
            FileUtils.copy(new File(wineSysWoW64Dir, dll), new File(containerSysWoW64Dir, dll));
            FileUtils.copy(new File(wineSystem32Dir, dll), new File(containerSystem32Dir, dll));
        }
    }

    private boolean isGenerateWineprefix() {
        return getIntent().getBooleanExtra("generate_wineprefix", false);
    }

    private String getWineStartCommand() {
        String cmdArgs = "";
        String execPath = null;
        String execArgs = "";

        if (shortcut != null) {
            execArgs = shortcut.getExtra("execArgs");
            execArgs = !execArgs.isEmpty() ? " "+execArgs : "";

            if (shortcut.path.endsWith(".lnk") || shortcut.path.contains("://")) {
                cmdArgs = "\""+shortcut.path+"\""+execArgs;
            }
            else execPath = shortcut.path;
        }
        else {
            Intent intent = getIntent();
            if (intent.hasExtra("exec_path")) {
                execPath = WineUtils.unixToDOSPath(intent.getStringExtra("exec_path"), container);

                if (execPath.endsWith(".lnk")) {
                    cmdArgs = "\""+execPath+"\"";
                    execPath = null;
                }
            }
        }

        if (execPath != null) {
            String execDir = FileUtils.getDirname(execPath);
            String filename = FileUtils.getName(execPath);
            int dotIndex, spaceIndex;
            if ((dotIndex = filename.lastIndexOf(".")) != -1 && (spaceIndex = filename.indexOf(" ", dotIndex)) != -1) {
                execArgs = filename.substring(spaceIndex+1)+execArgs;
                filename = filename.substring(0, spaceIndex);
            }
            cmdArgs = "/dir "+StringUtils.escapeDOSPath(execDir)+" \""+filename+"\""+execArgs;
        }

        if (cmdArgs.isEmpty()) cmdArgs = "/dir C:\\windows \"wfm.exe\"";

        if (overrideEnvVars != null && overrideEnvVars.has("EXTRA_EXEC_ARGS")) {
            cmdArgs += " "+overrideEnvVars.get("EXTRA_EXEC_ARGS");
            overrideEnvVars.remove("EXTRA_EXEC_ARGS");
        }
        return "C:\\windows\\winhandler.exe "+cmdArgs;
    }

    public XServer getXServer() {
        return xServer;
    }

    public WinHandler getWinHandler() {
        return winHandler;
    }

    public XServerView getXServerView() {
        return xServerView;
    }

    public Container getContainer() {
        return container;
    }

    public RootFS getRootFs() {
        return rootFS;
    }

    public EnvVars getOverrideEnvVars() {
        if (overrideEnvVars == null) overrideEnvVars = new EnvVars();
        return overrideEnvVars;
    }

    public String getDXWrapper() {
        return dxwrapper;
    }

    public void setDXWrapper(String dxwrapper) {
        this.dxwrapper = dxwrapper;
    }

    public ScreenInfo getScreenInfo() {
        return screenInfo;
    }

    public void setScreenInfo(ScreenInfo screenInfo) {
        this.screenInfo = screenInfo;
    }

    public String getWinComponents() {
        return wincomponents;
    }

    public void setWinComponents(String wincomponents) {
        this.wincomponents = wincomponents;
    }

    public DebugDialog getDebugDialog() {
        return debugDialog;
    }

    public String getScreenEffectProfile() {
        return screenEffectProfile;
    }

    public void setScreenEffectProfile(String screenEffectProfile) {
        this.screenEffectProfile = screenEffectProfile;
    }

    private void changeWineAudioDriver() {
        if (!audioDriver.equals(container.getExtra("audioDriver"))) {
            File rootDir = rootFS.getRootDir();
            File userRegFile = new File(rootDir, RootFS.WINEPREFIX+"/user.reg");
            try (WineRegistryEditor registryEditor = new WineRegistryEditor(userRegFile)) {
                if (audioDriver.equals(AudioDrivers.ALSA)) {
                    registryEditor.setStringValue("Software\\Wine\\Drivers", "Audio", "alsa");
                }
                else if (audioDriver.equals(AudioDrivers.PULSEAUDIO)) {
                    registryEditor.setStringValue("Software\\Wine\\Drivers", "Audio", "pulse");
                }
            }
            container.putExtra("audioDriver", audioDriver);
            container.saveData();
        }
    }

    private void applyGeneralPatches(Container container) {
        File rootDir = rootFS.getRootDir();
        FileUtils.delete(new File(rootDir, "/opt/apps"));

        boolean isBionic = container.isBionic();
        if (isBionic) {
            TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "container_pattern_common.tzst", rootDir);
        } else {
            TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, FusionFS.ASSET_FUSIONFS_PATCHES, rootDir);
        }

        String pulseaudioAsset = isBionic ? "pulseaudio-full.tzst" : "pulseaudio.tzst";
        String pulseaudioDir = isBionic ? "pulseaudio/bionic" : "pulseaudio/glibc";
        TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, pulseaudioAsset, new File(getFilesDir(), pulseaudioDir));
        WineUtils.applySystemTweaks(this, wineInfo, rootDir);
        container.putExtra("graphicsDriver", null);
        container.putExtra("dxwrapper", null);
        container.putExtra("desktopTheme", null);
        SettingsFragment.resetBox64Version(this);
        SettingsFragment.resetFEXCoreVersion(this);
    }

    private void changeFrameRatingVisibility(Window window, boolean visible) {
        if (frameRating == null) return;
        if (visible) {
            Window child = window.getChildCount() > 0 ? window.getChildren().get(0) : null;
            boolean viewable = window.attributes.isMapped() && window.getWidth() >= ScreenInfo.MIN_WIDTH && window.getHeight() >= ScreenInfo.MIN_HEIGHT;
            if (viewable && (window.isSurface() || (child != null && child.isSurface()))) {
                Window frameRatingWindow = window.isSurface() ? window : child;
                if (frameRating.getMode() == FrameRating.Mode.FULL) {
                    Property gpuInfo = frameRatingWindow.getProperty(Atom._NET_WM_GPU_INFO);
                    frameRating.setGPUInfo(gpuInfo != null ? new String(gpuInfo.data.array()) : "N/A");
                }
                frameRatingWindowId = frameRatingWindow.id;
                frameRating.reset();
            }
        }
        else if (window.id == frameRatingWindowId) {
            frameRatingWindowId = -1;
            runOnUiThread(() -> frameRating.setVisibility(View.GONE));
        }
    }

    public boolean verifyUserRegistry() {
        File userRegFile = new File(rootFS.getRootDir(), RootFS.WINEPREFIX+"/user.reg");
        String lastModified = String.valueOf(userRegFile.lastModified());

        if (!lastModified.equals(container.getExtra("userRegLastModified"))) {
            try (WineRegistryEditor registryEditor = new WineRegistryEditor(userRegFile)) {
                registryEditor.removeKey("Software\\Wow6432Node\\Wine", true);
            }

            container.putExtra("userRegLastModified", lastModified);
            return true;
        }
        else return false;
    }
}