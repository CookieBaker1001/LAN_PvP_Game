package com.springer.knakobrak.local;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;

public class SkinSelector {

    private final Array<Drawable> skins;
    private int currentIndex = 0;
    private int skinArraySize;

    private Image previewImage;
    private Table root;

    public SkinSelector(Skin uiSkin, Array<Drawable> availableSkins, int type) {
        this.skins = availableSkins;

        previewImage = new Image(skins.first());
        previewImage.setScaling(Scaling.fit);
        skinArraySize = (type == 1) ? availableSkins.size-1 : availableSkins.size;

        TextButton prevSkinBtn = new TextButton("<", uiSkin);
        TextButton nextSkinBtn = new TextButton(">", uiSkin);

        prevSkinBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                previous();
            }
        });

        nextSkinBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                next();
            }
        });

        previewImage.addAction(Actions.sequence(
            Actions.fadeOut(0.1f),
            Actions.run(this::updatePreview),
            Actions.fadeIn(0.1f)
        ));

        root = new Table();
        root.add(new Label((type == 1) ? "Skin" : "Ball" + " select", uiSkin));
        root.row();
        root.add(prevSkinBtn).pad(10);
        root.add(previewImage).size(128, 128).pad(10);
        root.add(nextSkinBtn).pad(10);
    }

    private void previous() {
        currentIndex--;
        if (currentIndex < 0) currentIndex += skinArraySize;
        updatePreview();
    }

    private void next() {
        currentIndex++;
        if (currentIndex >= skinArraySize) currentIndex -= skinArraySize;
        updatePreview();
    }

    private void updatePreview() {
        previewImage.setDrawable(skins.get(currentIndex));
    }

    public Table getRoot() {
        return root;
    }

    public int getSelectedIndex() {
        return currentIndex;
    }

    public Drawable getSelectedSkin() {
        return skins.get(currentIndex);
    }
}
