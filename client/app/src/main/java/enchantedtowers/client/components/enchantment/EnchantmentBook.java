package enchantedtowers.client.components.enchantment;

import android.content.Context;
import android.graphics.PointF;

import com.google.common.collect.ImmutableList;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EnchantmentBook {
    static private boolean isInstantiated = false;
    static private ArrayList<Enchantment> templates = new ArrayList<>();

    static public void instantiate(ArrayList<ArrayList<PointF>> data) throws RuntimeException {
        if (isInstantiated) {
            throw new RuntimeException("EnchantmentBook singleton is already instantiated");
        }

        isInstantiated = true;
        for (var templatePoints : data) {
            templates.add(
                    new Enchantment(templatePoints)
            );
        }
    }

    static public List<Enchantment> getTemplates() {
        return Collections.unmodifiableList(templates);
    }

    private EnchantmentBook() {}
}