package enchantedtowers.client.components.permissions;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;


public class PermissionManager {
    private boolean withPermissionsCalled = false;
    private boolean allPermissionsGranted = false;


    public PermissionManager withPermissions(String[] requiredPermissions, Context context, Runnable callback) {
        boolean allPermissionsGranted = true;
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_DENIED) {
                allPermissionsGranted = false;
                break;
            }
        }

        this.withPermissionsCalled = true;
        this.allPermissionsGranted = allPermissionsGranted;

        if (allPermissionsGranted) {
            callback.run();
        }

        return this;
    }

    public void otherwise(Runnable callback) throws IllegalStateException {
        if (!withPermissionsCalled) {
            throw new IllegalStateException("'withPermissions' must be called before 'otherwise'");
        }
        if (!allPermissionsGranted) {
            callback.run();
        }
    }
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
