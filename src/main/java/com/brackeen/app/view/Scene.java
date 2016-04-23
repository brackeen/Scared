package com.brackeen.app.view;

@SuppressWarnings("unused")
public class Scene extends View {

    private View focusedView = this;

    public View getFocusedView() {
        return focusedView;
    }

    public void setFocusedView(View focusedView) {
        this.focusedView = focusedView;
    }
}
