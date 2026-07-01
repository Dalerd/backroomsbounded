package net.dalerd.backroomsbounded.entity.mimic;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class MimicRenderer extends MobEntityRenderer<MimicEntity, PlayerEntityModel<MimicEntity>> {

    private static final Identifier STEVE_SKIN = Identifier.of("minecraft", "textures/entity/player/wide/steve.png");
    private static final Identifier ALEX_SKIN = Identifier.of("minecraft", "textures/entity/player/wide/alex.png");
    private static final Identifier CASEOH_TEXTURE = Identifier.of("backroomsbounded", "textures/entity/mimic/caseoh.png");
    private static final Identifier VERITY_TEXTURE = Identifier.of("backroomsbounded", "textures/entity/mimic/verity.png");
    private static final Identifier AVERY_TEXTURE = Identifier.of("backroomsbounded", "textures/entity/mimic/averythemayo.png");

    public MimicRenderer(EntityRendererFactory.Context context) {
        super(context, new PlayerEntityModel<>(context.getPart(
                net.minecraft.client.render.entity.model.EntityModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public void render(MimicEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        matrices.scale(0.97f, 0.97f, 0.97f);
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        matrices.pop();
    }

    @Override
    public Identifier getTexture(MimicEntity entity) {
        String name = entity.getCopiedPlayerName();
        if (name == null || name.isEmpty()) return STEVE_SKIN;

        if (name.equals("Caseoh")) return CASEOH_TEXTURE;
        if (name.equals("Verity")) return VERITY_TEXTURE;
        if (name.equals("Avery")) return AVERY_TEXTURE;

        if (entity.getCopiedPlayerUUID() != null && !name.equals("Steve") && !name.equals("Alex")) {
            return Identifier.of("minecraft", "textures/entity/player/wide/" + name.toLowerCase() + ".png");
        }

        if (name.equals("Alex")) return ALEX_SKIN;
        return STEVE_SKIN;
    }
}