package com.sessiontracks.app.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;

import androidx.annotation.NonNull;

/**
 * Connectivity check used before opening a lecture video. All study data itself is
 * local, so the app stays fully usable offline — only playback needs the network.
 */
public final class NetworkUtils {

    private NetworkUtils() {
    }

    public static boolean isOnline(@NonNull Context context) {
        ConnectivityManager manager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            // Cannot determine: assume online rather than blocking the user.
            return true;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network network = manager.getActiveNetwork();
                if (network == null) {
                    return false;
                }
                NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
                if (capabilities == null) {
                    return false;
                }
                return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                        || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN));
            }
            android.net.NetworkInfo info = manager.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } catch (Exception e) {
            return true;
        }
    }
}
