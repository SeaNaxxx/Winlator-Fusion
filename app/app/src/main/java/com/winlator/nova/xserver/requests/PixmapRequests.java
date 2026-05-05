package com.winlator.nova.xserver.requests;

import com.winlator.nova.xconnector.XInputStream;
import com.winlator.nova.xconnector.XOutputStream;
import com.winlator.nova.xserver.Drawable;
import com.winlator.nova.xserver.Pixmap;
import com.winlator.nova.xserver.XClient;
import com.winlator.nova.xserver.errors.BadDrawable;
import com.winlator.nova.xserver.errors.BadIdChoice;
import com.winlator.nova.xserver.errors.XRequestError;

public abstract class PixmapRequests {
    public static void createPixmap(XClient client, XInputStream inputStream, XOutputStream outputStream) throws XRequestError {
        byte depth = client.getRequestData();
        int pixmapId = inputStream.readInt();
        int drawableId = inputStream.readInt();
        short width = inputStream.readShort();
        short height = inputStream.readShort();

        if (!client.isValidResourceId(pixmapId)) throw new BadIdChoice(pixmapId);

        Drawable drawable = client.xServer.drawableManager.getDrawable(drawableId);
        if (drawable == null) throw new BadDrawable(drawableId);

        Drawable backingStore = client.xServer.drawableManager.createDrawable(pixmapId, width, height, depth);
        if (backingStore == null) throw new BadIdChoice(pixmapId);
        Pixmap pixmap = client.xServer.pixmapManager.createPixmap(backingStore);
        if (pixmap == null) throw new BadIdChoice(pixmapId);
        client.registerAsOwnerOfResource(pixmap);
    }

    public static void freePixmap(XClient client, XInputStream inputStream, XOutputStream outputStream) throws XRequestError {
        client.xServer.pixmapManager.freePixmap(inputStream.readInt());
    }
}
