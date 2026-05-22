package com.winlator.fusion.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.view.SurfaceControl;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.winlator.fusion.renderer.GLRenderer;
import com.winlator.fusion.renderer.VulkanRenderer;
import com.winlator.fusion.xserver.XServer;

@SuppressLint("ViewConstructor")
public class XServerView extends FrameLayout {
    private GLRenderer glRenderer;
    private VulkanRenderer vulkanRenderer;
    private GLSurfaceView glSurfaceView;
    private TextureView vulkanTextureView;
    private final XServer xServer;
    private final boolean useVulkan;

    public XServerView(Context context, XServer xServer, boolean useVulkan) {
        super(context);
        this.xServer = xServer;
        this.useVulkan = useVulkan;
        setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (useVulkan) {
            setupVulkanRenderer(context);
        } else {
            setupGLRenderer(context);
        }
    }

    public XServerView(Context context, XServer xServer) {
        this(context, xServer, false);
    }

    private void setupGLRenderer(Context context) {
        glSurfaceView = new GLSurfaceView(context);
        glSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        glSurfaceView.setEGLContextClientVersion(3);
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        glSurfaceView.setPreserveEGLContextOnPause(true);
        glRenderer = new GLRenderer(this, xServer);
        glSurfaceView.setRenderer(glRenderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        addView(glSurfaceView);
    }

    private void setupVulkanRenderer(Context context) {
        vulkanTextureView = new TextureView(context);
        vulkanTextureView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        vulkanRenderer = new VulkanRenderer(this, xServer);
        vulkanTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                vulkanRenderer.onSurfaceCreated(new android.view.Surface(surface));
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                vulkanRenderer.onSurfaceChanged(width, height);
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                vulkanRenderer.onSurfaceDestroyed();
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
        addView(vulkanTextureView);
    }

    public GLRenderer getRenderer() {
        return glRenderer;
    }

    public VulkanRenderer getVulkanRenderer() {
        return vulkanRenderer;
    }

    public boolean isVulkan() {
        return useVulkan;
    }

    public void onResume() {
        if (glSurfaceView != null) glSurfaceView.onResume();
    }

    public void onPause() {
        if (glSurfaceView != null) glSurfaceView.onPause();
    }

    public void requestRender() {
        if (glSurfaceView != null) glSurfaceView.requestRender();
    }

    public void queueEvent(Runnable r) {
        if (glSurfaceView != null) glSurfaceView.queueEvent(r);
        else if (vulkanRenderer != null) r.run();
    }

    @android.annotation.SuppressLint("NewApi")
    public SurfaceControl getSurfaceControl() {
        if (Build.VERSION.SDK_INT >= 29) {
            if (glSurfaceView != null) return glSurfaceView.getSurfaceControl();
        }
        return null;
    }
}
