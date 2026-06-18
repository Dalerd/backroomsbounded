package net.dalerd.backroomsbounded.entity.bacterium;

import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BacteriumRenderer extends GeoEntityRenderer<BacteriumEntity> {

    public BacteriumRenderer(EntityRendererFactory.Context context) {
        super(context, new BacteriumModel());
        this.shadowRadius = 0.5f;
    }

    @Override
    public float getMotionAnimThreshold(BacteriumEntity animatable) {
        return 0.0001f;
    }
}