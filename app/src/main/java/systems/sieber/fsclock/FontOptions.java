package systems.sieber.fsclock;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;

public class FontOptions {
    public final static int DSEG7_CLASSIC  = 0;
    public final static int DSEG14_CLASSIC = 1;
    public final static int CAIRO_REGULAR  = 2;
    public final static int CAIRO_LIGHT    = 3;
    public final static int CAIRO_BOLD     = 4;
    public final static int ROBOTO_THIN    = 5;
    public final static ArrayList<FontOption> FONT_OPTIONS = new ArrayList<>(
            Arrays.asList(
                    new FontOption(DSEG7_CLASSIC, "DSEG7 Classic", R.font.dseg7classic_regular, 0.05f),
                    new FontOption(DSEG14_CLASSIC, "DSEG14 Classic", R.font.dseg14classic_regular, 0.05f),
                    new FontOption(CAIRO_REGULAR, "Cairo Regular", R.font.cairo_regular, 0.05f),
                    new FontOption(CAIRO_LIGHT, "Cairo Light", R.font.cairo_light, 0.05f),
                    new FontOption(CAIRO_BOLD, "Cairo Bold", R.font.cairo_bold, 0.05f),
                    new FontOption(ROBOTO_THIN, "Roboto Thin", R.font.roboto_thin, 0.06f)
            )
    );
    public static FontOption getById(int id) {
        for(FontOption fo : FONT_OPTIONS) {
            if(fo.mId == id) {
                return fo;
            }
        }
        return null;
    }
}

class FontOption {
    int mId;
    String mDisplayName;
    int mResourceId;
    float mXCorr;
    FontOption(int id, String displayName, int resourceId, float xCorr) {
        mId = id;
        mDisplayName = displayName;
        mResourceId = resourceId;
        mXCorr = xCorr;
    }
    @NonNull
    @Override
    public String toString() {
        return mDisplayName;
    }
}
