package enchantedtowers.client.components.fs;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class AndroidFileReader {
    public static void writeToFile(Context context, String filename, String data) throws IOException {
        try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(
                context.openFileOutput(filename, Context.MODE_PRIVATE))) {
            outputStreamWriter.write(data);
        }
    }

    public static String readFromFile(Context context, String filename) throws IOException {
        String result = "";

        try (InputStream inputStream = context.openFileInput(filename)) {
            if (inputStream != null) {
                try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                    String receivedString = "";
                    StringBuilder stringBuilder = new StringBuilder();

                    while ( (receivedString = bufferedReader.readLine()) != null ) {
                        stringBuilder.append("\n").append(receivedString);
                    }

                    inputStream.close();
                    result = stringBuilder.toString().trim();
                }
            }
            else {
                throw new IOException("Cannot open stream for file '" + filename + "'");
            }
        }

        return result;
    }
}
