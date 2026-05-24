package com.winlator.fusion.xserver;

import com.winlator.fusion.XServerDisplayActivity;
import com.winlator.fusion.contentdialog.DebugDialog;
import com.winlator.fusion.core.CursorLocker;
import com.winlator.fusion.renderer.Renderer;
import com.winlator.fusion.winhandler.WinHandler;
import com.winlator.fusion.xserver.extensions.BigReqExtension;
import com.winlator.fusion.xserver.extensions.DRI3Extension;
import com.winlator.fusion.xserver.extensions.Extension;
import com.winlator.fusion.xserver.extensions.GLXExtension;
import com.winlator.fusion.xserver.extensions.MITSHMExtension;
import com.winlator.fusion.xserver.extensions.PresentExtension;
import com.winlator.fusion.xserver.extensions.SyncExtension;
import com.winlator.fusion.xserver.extensions.XComposite;
import com.winlator.fusion.xserver.extensions.XInput2Extension;

import java.nio.charset.Charset;
import java.util.EnumMap;
import java.util.concurrent.locks.ReentrantLock;

public class XServer {
    public enum Lockable {WINDOW_MANAGER, PIXMAP_MANAGER, DRAWABLE_MANAGER, GRAPHIC_CONTEXT_MANAGER, INPUT_DEVICE, CURSOR_MANAGER, SHMSEGMENT_MANAGER}
    public static final short VERSION = 11;
    public static final String VENDOR_NAME = "Elbrus Technologies, LLC";
    public static final Charset LATIN1_CHARSET = Charset.forName("latin1");
    public final XServerDisplayActivity activity;
    private final Extension[] extensions;
    public final ScreenInfo screenInfo;
    public final PixmapManager pixmapManager;
    public final ResourceIDs resourceIDs = new ResourceIDs(128);
    public final GraphicsContextManager graphicsContextManager = new GraphicsContextManager();
    public final SelectionManager selectionManager;
    public final DrawableManager drawableManager;
    public final WindowManager windowManager;
    public final CursorManager cursorManager;
    public final Keyboard keyboard = Keyboard.createKeyboard(this);
    public final Pointer pointer = new Pointer(this);
    public final InputDeviceManager inputDeviceManager;
    public final GrabManager grabManager;
    public final CursorLocker cursorLocker;
    private SHMSegmentManager shmSegmentManager;
    private Renderer renderer;
    private WinHandler winHandler;
    private final EnumMap<Lockable, ReentrantLock> locks = new EnumMap<>(Lockable.class);
    private boolean relativeMouseMovement = false;
    private XInput2Extension xinput2Extension;

    public XServer(XServerDisplayActivity activity, ScreenInfo screenInfo) {
        this.activity = activity;
        this.screenInfo = screenInfo;
        cursorLocker = new CursorLocker(this);
        for (Lockable lockable : Lockable.values()) locks.put(lockable, new ReentrantLock());

        pixmapManager = new PixmapManager();
        drawableManager = new DrawableManager(this);
        cursorManager = new CursorManager(drawableManager);
        windowManager = new WindowManager(screenInfo, drawableManager);
        selectionManager = new SelectionManager(windowManager);
        inputDeviceManager = new InputDeviceManager(this);
        grabManager = new GrabManager(this);

        DesktopHelper.attachTo(this);
        extensions = setupExtensions();
    }

    public boolean isRelativeMouseMovement() {
        return relativeMouseMovement;
    }

    public void setRelativeMouseMovement(boolean relativeMouseMovement) {
        cursorLocker.setEnabled(!relativeMouseMovement);
        this.relativeMouseMovement = relativeMouseMovement;
    }

    public void setRenderingEnabled(boolean enabled) {
        windowManager.setRenderingEnabled(enabled);
    }

    public Renderer getRenderer() {
        return renderer;
    }

    public void setRenderer(Renderer renderer) {
        this.renderer = renderer;
    }

    public WinHandler getWinHandler() {
        return winHandler;
    }

    public void setWinHandler(WinHandler winHandler) {
        this.winHandler = winHandler;
    }

    public SHMSegmentManager getSHMSegmentManager() {
        return shmSegmentManager;
    }

    public void setSHMSegmentManager(SHMSegmentManager shmSegmentManager) {
        this.shmSegmentManager = shmSegmentManager;
    }

    private class SingleXLock implements XLock {
        private final ReentrantLock lock;

        private SingleXLock(Lockable lockable) {
            this.lock = locks.get(lockable);
            lock.lock();
        }

        @Override
        public void close() {
            lock.unlock();
        }
    }

    private class MultiXLock implements XLock {
        private final Lockable[] lockables;

        private MultiXLock(Lockable[] lockables) {
            this.lockables = lockables;
            for (Lockable lockable : lockables) locks.get(lockable).lock();
        }

        @Override
        public void close() {
            for (int i = lockables.length - 1; i >= 0; i--) {
                locks.get(lockables[i]).unlock();
            }
        }
    }

    public XLock lock(Lockable lockable) {
        return new SingleXLock(lockable);
    }

    public XLock lock(Lockable... lockables) {
        return new MultiXLock(lockables);
    }

    public XLock lockAll() {
        return new MultiXLock(Lockable.values());
    }

    public Extension getExtensionByName(String name) {
        for (Extension extension : extensions) if (extension.getName().equals(name)) return extension;
        return null;
    }

    public void injectPointerMove(int x, int y) {
        try (XLock lock = lock(Lockable.WINDOW_MANAGER, Lockable.INPUT_DEVICE)) {
            pointer.setPosition(x, y);
        }
    }

    public void injectPointerMoveDelta(int dx, int dy) {
        try (XLock lock = lock(Lockable.WINDOW_MANAGER, Lockable.INPUT_DEVICE)) {
            pointer.setPosition(pointer.getX() + dx, pointer.getY() + dy);
            xinput2Extension.emitRawMotion(2, dx, dy);
        }
    }

    public void injectPointerButtonPress(Pointer.Button buttonCode) {
        try (XLock lock = lock(Lockable.WINDOW_MANAGER, Lockable.INPUT_DEVICE)) {
            pointer.setButton(buttonCode, true);
            xinput2Extension.emitRawButton(2, buttonCode.code(), true);
        }
    }

    public void injectPointerButtonRelease(Pointer.Button buttonCode) {
        try (XLock lock = lock(Lockable.WINDOW_MANAGER, Lockable.INPUT_DEVICE)) {
            pointer.setButton(buttonCode, false);
            xinput2Extension.emitRawButton(2, buttonCode.code(), false);
        }
    }

    public void injectKeyPress(XKeycode xKeycode) {
        injectKeyPress(xKeycode, 0);
    }

    public void injectKeyPress(XKeycode xKeycode, int keysym) {
        try (XLock lock = lock(Lockable.WINDOW_MANAGER, Lockable.INPUT_DEVICE)) {
            keyboard.setKeyPress(xKeycode.id, keysym);
        }
    }

    public void injectKeyRelease(XKeycode xKeycode) {
        try (XLock lock = lock(Lockable.WINDOW_MANAGER, Lockable.INPUT_DEVICE)) {
            keyboard.setKeyRelease(xKeycode.id);
        }
    }

    private void registerExtension(Extension ext, int[] nextEventId, int[] nextErrorId) {
        if (ext.getNumEvents() > 0) {
            ext.setFirstEventId((byte) nextEventId[0]);
            nextEventId[0] += ext.getNumEvents();
        }
        if (ext.getNumErrors() > 0) {
            ext.setFirstErrorId((byte) nextErrorId[0]);
            nextErrorId[0] += ext.getNumErrors();
        }
    }

    private Extension[] setupExtensions() {
        byte opcode = Extension.START_MAJOR_OPCODE;
        int[] nextEventId = {64};
        int[] nextErrorId = {128};

        BigReqExtension bigReq = new BigReqExtension(this, opcode--);
        MITSHMExtension mitshm = new MITSHMExtension(this, opcode--);
        DRI3Extension dri3 = new DRI3Extension(this, opcode--);
        PresentExtension present = new PresentExtension(this, opcode--);
        SyncExtension sync = new SyncExtension(this, opcode--);
        XComposite xcomposite = new XComposite(this, opcode--);
        GLXExtension glx = new GLXExtension(this, opcode--);
        xinput2Extension = new XInput2Extension(this, opcode--);

        registerExtension(xinput2Extension, nextEventId, nextErrorId);

        return new Extension[]{bigReq, mitshm, dri3, present, sync, xcomposite, glx, xinput2Extension};
    }

    public <T extends Extension> T getExtension(byte opcode) {
        int index = Extension.START_MAJOR_OPCODE - opcode;
        return (T)extensions[index];
    }

    public void debugPrint(String line) {
        DebugDialog debugDialog = activity.getDebugDialog();
        if (debugDialog != null) debugDialog.call("xserver:"+line);
    }
}
