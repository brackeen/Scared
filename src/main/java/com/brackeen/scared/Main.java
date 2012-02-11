package com.brackeen.scared;

import com.brackeen.app.App;
import com.brackeen.app.view.Scene;

public class Main extends App {
    
    private static final long serialVersionUID = 1L;
    
    @Override
    public Scene createFirstScene() {
        return new TitleScene();
    }

}
