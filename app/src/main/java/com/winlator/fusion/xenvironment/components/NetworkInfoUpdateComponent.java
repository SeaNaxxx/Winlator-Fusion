package com.winlator.fusion.xenvironment.components;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

import com.winlator.fusion.core.FileUtils;
import com.winlator.fusion.core.NetworkHelper;
import com.winlator.fusion.xenvironment.FusionFS;
import com.winlator.fusion.xenvironment.EnvironmentComponent;
import com.winlator.fusion.xenvironment.RootFS;

import java.io.File;
import java.util.List;

public class NetworkInfoUpdateComponent extends EnvironmentComponent {
    private BroadcastReceiver broadcastReceiver;

    @Override
    public void start() {
        Context context = environment.getContext();
        final NetworkHelper networkHelper = new NetworkHelper(context);
        updateIFAddrsFile(networkHelper.getIFAddresses());
        updateEtcHostsFile(networkHelper.getIPv4Address());

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateIFAddrsFile(networkHelper.getIFAddresses());
                updateEtcHostsFile(networkHelper.getIPv4Address());
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(broadcastReceiver, filter);
    }

    @Override
    public void stop() {
        if (broadcastReceiver != null) {
            environment.getContext().unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }
    }

    private void updateIFAddrsFile(List<NetworkHelper.IFAddress> ifAddresses) {
        FusionFS fusionFS = FusionFS.find(environment.getContext());
        File bionicTmp = new File(fusionFS.getBionicDir(), "usr/tmp");
        File glibcTmp = new File(fusionFS.getGlibcDir(), "tmp");

        String content = "";
        if (!ifAddresses.isEmpty()) {
            for (NetworkHelper.IFAddress ifAddress : ifAddresses) {
                content += (!content.isEmpty() ? "\n" : "")+ifAddress.toString();
            }
        }
        else content = (new NetworkHelper.IFAddress()).toString();

        if (bionicTmp.isDirectory()) {
            FileUtils.writeString(new File(bionicTmp, "ifaddrs"), content);
        }
        if (glibcTmp.isDirectory()) {
            FileUtils.writeString(new File(glibcTmp, "ifaddrs"), content);
        }
    }

    private void updateEtcHostsFile(String ipAddress) {
        String ip = ipAddress != null ? ipAddress : "127.0.0.1";
        String hostsContent = ip+"\tlocalhost\n";

        FusionFS fusionFS = FusionFS.find(environment.getContext());

        File bionicEtcHosts = new File(fusionFS.getBionicDir(), "usr/etc/hosts");
        File glibcEtcHosts = new File(fusionFS.getGlibcDir(), "etc/hosts");

        FileUtils.writeString(bionicEtcHosts, hostsContent);
        FileUtils.writeString(glibcEtcHosts, hostsContent);
    }
}
