package cs.Rewired.Integration.UnityUI;

/** Rewired's UI input module: UGUI's StandaloneInputModule (touch / mouse pointer events) driven by the
 *  Rewired UI actions of player 0 for navigation, submit and cancel. */
public class RewiredStandaloneInputModule extends cs.UnityEngine.EventSystems.StandaloneInputModule {
    public static final Object[] $fmeta = {};
    public static final Object[][] $mmeta = {};
    public void $ctor_RewiredStandaloneInputModule() {
        $ctor_StandaloneInputModule();
        this.m_HorizontalAxis = "UIHorizontal";
        this.m_VerticalAxis = "UIVertical";
        this.m_SubmitButton = "UISubmit";
        this.m_CancelButton = "UICancel";
    }
}
