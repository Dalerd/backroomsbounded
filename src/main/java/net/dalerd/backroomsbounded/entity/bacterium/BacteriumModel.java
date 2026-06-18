package net.dalerd.backroomsbounded.entity.bacterium;

import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class BacteriumModel extends GeoModel<BacteriumEntity> {

    @Override
    public Identifier getModelResource(BacteriumEntity entity) {
        return Identifier.of("backroomsbounded", "geo/entity/bacterium.geo.json");
    }

    @Override
    public Identifier getTextureResource(BacteriumEntity entity) {
        return Identifier.of("backroomsbounded", "textures/entity/bacterium.png");
    }

    @Override
    public Identifier getAnimationResource(BacteriumEntity entity) {
        return Identifier.of("backroomsbounded", "animations/entity/bacterium.animation.json");
    }
}