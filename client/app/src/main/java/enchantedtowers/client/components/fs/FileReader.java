package enchantedtowers.client.components.fs;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class FileReader {
    private final Context context;

    FileReader(Context context) {
        this.context = context;
    }

    protected String readRawFile(int resourceId) throws IOException {
        InputStream inputStream = context.getResources().openRawResource(resourceId);

        InputStreamReader inputReader = new InputStreamReader(inputStream);
        BufferedReader buffReader = new BufferedReader(inputReader);

        String line;
        StringBuilder text = new StringBuilder();

        while ((line = buffReader.readLine()) != null) {
            text.append(line);
        }

        return text.toString();
    }
}
