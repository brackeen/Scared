package com.brackeen.scared.action;

import com.brackeen.app.App;
import com.brackeen.app.BufferedAudio;
import com.brackeen.scared.Map;
import com.brackeen.scared.SoundPlayer3D;
import com.brackeen.scared.entity.Player;

public class GeneratorAction implements Action {

    private final Player player;
    private final int sourceTileX;
    private final int sourceTileY;
    private BufferedAudio.Stream stream;
    
    public GeneratorAction(Map map, int x, int y) {
        this.player = map.getPlayer();
        this.sourceTileX = x;
        this.sourceTileY = y;
        BufferedAudio audio = App.getApp().getAudio("/sound/bigfan.wav", 1);
        
        float volume = SoundPlayer3D.getVolume(player, sourceTileX, sourceTileY);
        float pan = SoundPlayer3D.getPan(player, sourceTileX, sourceTileY);
        stream = audio.play(volume, pan, true);
    }
    
    @Override
    public void tick() {
        if (stream != null) {
            stream.setVolume(SoundPlayer3D.getVolume(player, sourceTileX, sourceTileY));
            stream.setPan(SoundPlayer3D.getPan(player, sourceTileX, sourceTileY));
        }
    }

    @Override
    public void unload() {
        if (stream != null) {
            stream.stop();
            stream = null;
        }
    }

    @Override
    public boolean isFinished() {
        return stream == null;
    }
}
