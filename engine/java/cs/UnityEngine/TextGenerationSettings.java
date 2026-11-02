package cs.UnityEngine;

public class TextGenerationSettings implements cs.rt.Struct {
    public Font font;
    public Color color = new Color();
    public int fontSize, fontStyle, textAnchor, horizontalOverflow, verticalOverflow, resizeTextMinSize, resizeTextMaxSize;
    public float lineSpacing, scaleFactor;
    public boolean richText, alignByGeometry, resizeTextForBestFit, updateBounds, generateOutOfBounds;
    public Vector2 generationExtents = new Vector2(), pivot = new Vector2();
    public TextGenerationSettings() {}
    public TextGenerationSettings $copy() { TextGenerationSettings s = new TextGenerationSettings(); s.$set(this); return s; }
    public void $set(Object o) {
        TextGenerationSettings s = (TextGenerationSettings) o;
        font = s.font; color.$set(s.color); fontSize = s.fontSize; fontStyle = s.fontStyle; textAnchor = s.textAnchor;
        horizontalOverflow = s.horizontalOverflow; verticalOverflow = s.verticalOverflow; resizeTextMinSize = s.resizeTextMinSize;
        resizeTextMaxSize = s.resizeTextMaxSize; lineSpacing = s.lineSpacing; scaleFactor = s.scaleFactor; richText = s.richText;
        alignByGeometry = s.alignByGeometry; resizeTextForBestFit = s.resizeTextForBestFit; updateBounds = s.updateBounds;
        generateOutOfBounds = s.generateOutOfBounds; generationExtents.$set(s.generationExtents); pivot.$set(s.pivot);
    }
    public void $clear() { $set(new TextGenerationSettings()); }
    public static TextGenerationSettings[] $newArray(int n) { TextGenerationSettings[] a = new TextGenerationSettings[n]; for (int i = 0; i < n; i++) a[i] = new TextGenerationSettings(); return a; }
}
