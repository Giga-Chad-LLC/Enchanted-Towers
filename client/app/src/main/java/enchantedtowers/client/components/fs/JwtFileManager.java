package enchantedtowers.client.components.fs;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Optional;
import java.util.logging.Logger;

public class JwtFileManager {
    private static final Logger logger = Logger.getLogger(JwtFileManager.class.getName());
     private static final String filename = "enchanted-towers-jwt-file.txt";
     private final Context context;

     public JwtFileManager(@NonNull Context context) {
        this.context = context;
     }

     public Optional<String> getJwtToken() {
         try {
             String token = AndroidFileReader.readFromFile(context, filename);
             return Optional.of(token);
         }
         catch (IOException err) {
             logger.warning("Error reading jwt token: " + err.getMessage());
             err.printStackTrace();
             return Optional.empty();
         }
     }

     public boolean storeJwtToken(@NonNull String token) {
        try {
            AndroidFileReader.writeToFile(context, filename, token);
            return true;
        }
        catch (IOException err) {
            logger.warning("Error storing jwt token: " + err.getMessage());
            err.printStackTrace();
            return false;
        }
     }
}
