package net.dalerd.backroomsbounded.world.gen;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

public class StructureHelper {

    public static boolean place(ServerWorld world, String name, BlockPos pos, Random random) {
        StructureTemplateManager manager = world.getStructureTemplateManager();

        Identifier id = Identifier.of("backroomsbounded", name);
        StructureTemplate template = manager.getTemplate(id).orElse(null);

        if (template == null) return false;

        StructurePlacementData data = new StructurePlacementData()
                .setRotation(BlockRotation.random(random))
                .setIgnoreEntities(false);

        template.place(world, pos, pos, data, random, 2);

        return true;
    }
}
