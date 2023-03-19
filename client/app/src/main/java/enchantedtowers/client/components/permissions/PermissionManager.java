package enchantedtowers.client.components.permissions;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

public class PermissionManager {
    /**
     * Returns {@code true} if either of
     *      {@link Manifest.permission#ACCESS_FINE_LOCATION} or
     *      {@link Manifest.permission#ACCESS_COARSE_LOCATION}
     *  permissions are granted,
     * otherwise returns {@code false}.
     *
     * @Note Name of function <b>must start</b> with <b><i>'check'</i></b> and <b>end</b> with <b><i>'permission'</i></b>, otherwise linter complains (see: <a href="https://stackoverflow.com/questions/36031218/check-android-permissions-in-a-method">Check Android Permissions in a Method</a>)
     */
    static public boolean checkLocationPermission(@NonNull Context context) {
        boolean accessFineLocationPermissionGranted =
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        boolean accessCoarseLocationPermissionGranted =
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        return accessFineLocationPermissionGranted || accessCoarseLocationPermissionGranted;
    }
}
