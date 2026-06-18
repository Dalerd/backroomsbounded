package net.dalerd.backroomsbounded.world.gen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.dalerd.backroomsbounded.block.ModBlocks;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.biome.source.BiomeAccess;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class BackroomsChunkGenerator extends ChunkGenerator {

    public static final MapCodec<BackroomsChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(BackroomsChunkGenerator::getBiomeSource)
            ).apply(instance, BackroomsChunkGenerator::new)
    );

    private static final int FLOOR_HEIGHT = 8;
    private static final int WALL_HEIGHT = 6;

    private long worldSeed = 0;

    private Map<Long, MoldCluster[]> moldCache = new HashMap<>();

    // Track which grids have bacteria to prevent adjacent generation
    private static final Map<Long, Set<GridPos>> generatedBacteriaGrids = new HashMap<>();

    public BackroomsChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig,
                      BiomeAccess biomeAccess, StructureAccessor structureAccessor,
                      Chunk chunk, GenerationStep.Carver carverStep) {
        this.worldSeed = seed;
    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures,
                             NoiseConfig noiseConfig, Chunk chunk) {
    }

    @Override
    public void populateEntities(ChunkRegion region) {
    }

    @Override
    public int getWorldHeight() {
        return 256;
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Blender blender, NoiseConfig noiseConfig,
                                                  StructureAccessor structureAccessor, Chunk chunk) {
        generateChunk(chunk);
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public int getMinimumY() {
        return 0;
    }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world,
                         NoiseConfig noiseConfig) {
        return 256;
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world,
                                               NoiseConfig noiseConfig) {
        return new VerticalBlockSample(0, new BlockState[0]);
    }

    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
    }

    private void generateChunk(Chunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int chunkStartX = chunkPos.getStartX();
        int chunkStartZ = chunkPos.getStartZ();

        for (int floorStartY = 0; floorStartY < 256; floorStartY += FLOOR_HEIGHT) {
            generateFloor(chunk, chunkStartX, chunkStartZ, floorStartY);
        }
    }

    private void generateFloor(Chunk chunk, int chunkStartX, int chunkStartZ, int floorStartY) {
        int floorY = floorStartY;
        int ceilingY = floorStartY + FLOOR_HEIGHT - 1;
        int lightY = ceilingY - 1;
        int wallBottomY = floorY + 1;
        int wallTopY = ceilingY - 2;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkStartX + x;
                int worldZ = chunkStartZ + z;

                chunk.setBlockState(new BlockPos(x, floorY, z),
                        Blocks.YELLOW_TERRACOTTA.getDefaultState(), false);

                chunk.setBlockState(new BlockPos(x, ceilingY, z),
                        ModBlocks.BACKBOARD_BLOCK.getDefaultState(), false);

                BlockState lightBlock = getLightPatternBlock(x, z, worldX, worldZ);
                chunk.setBlockState(new BlockPos(x, lightY, z), lightBlock, false);

                for (int y = wallBottomY; y <= wallTopY; y++) {
                    chunk.setBlockState(new BlockPos(x, y, z),
                            Blocks.AIR.getDefaultState(), false);
                }
            }
        }

        generateBoundaryWalls(chunk, chunkStartX, chunkStartZ, floorStartY, wallBottomY, wallTopY);
        generateInternalWalls(chunk, chunkStartX, chunkStartZ, floorStartY, wallBottomY, wallTopY);
        generateColumns(chunk, chunkStartX, chunkStartZ, floorStartY, wallBottomY, wallTopY);
        generateProps(chunk, chunkStartX, chunkStartZ, floorStartY, floorY, wallBottomY, wallTopY);
        generateCreepyWallDecor(chunk, chunkStartX, chunkStartZ, floorStartY, wallBottomY, wallTopY);
        generateMossPatch(chunk, chunkStartX, chunkStartZ, floorStartY, floorY, wallBottomY);
        generateHoles(chunk, chunkStartX, chunkStartZ, floorStartY, floorY, ceilingY, lightY);
        generateBacteriaEcosystem(chunk, chunkStartX, chunkStartZ, floorStartY, floorY, wallBottomY, wallTopY);
    }

    // =========================================
    // BACTERIA ECOSYSTEM GENERATION
    // =========================================

    private void generateBacteriaEcosystem(Chunk chunk, int chunkStartX, int chunkStartZ,
                                           int floorStartY, int floorY, int wallBottomY, int wallTopY) {

        long bacteriaSeed = ((long) chunkStartX * 341873128712L +
                (long) chunkStartZ * 132897987541L +
                floorStartY * 555555L) ^ worldSeed;
        ChunkRandom bacteriaRandom = new ChunkRandom(new CheckedRandom(bacteriaSeed));

        // 2% chance per grid
        if (bacteriaRandom.nextFloat() >= 0.02f) return;

        // Check if this grid or adjacent grids already have bacteria
        long floorKey = ((long) chunkStartX << 32) | (chunkStartZ & 0xFFFFFFFFL);
        GridPos thisGrid = new GridPos(chunkStartX, chunkStartZ, floorStartY);

        Set<GridPos> floorGrids = generatedBacteriaGrids.computeIfAbsent(floorKey, k -> new HashSet<>());

        for (GridPos existing : floorGrids) {
            int dx = Math.abs(thisGrid.x - existing.x);
            int dz = Math.abs(thisGrid.z - existing.z);
            if (dx <= 16 && dz <= 16 && existing.y == thisGrid.y) {
                return;
            }
        }

        floorGrids.add(thisGrid);

        int centerX = bacteriaRandom.nextInt(10) + 3;
        int centerZ = bacteriaRandom.nextInt(10) + 3;
        int clusterSize = bacteriaRandom.nextInt(3) + 3; // 3-5 blocks radius (bigger clusters)

        // First pass: place vines on all nearby wallpaper walls (vines are more common)
        for (int dx = -clusterSize; dx <= clusterSize; dx++) {
            for (int dz = -clusterSize; dz <= clusterSize; dz++) {
                int x = centerX + dx;
                int z = centerZ + dz;
                if (x < 0 || x >= 16 || z < 0 || z >= 16) continue;

                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > clusterSize) continue;

                // Vines on walls - 60% chance in outer ring, 40% in inner
                float vineChance = (dist > clusterSize * 0.5f) ? 0.6f : 0.4f;
                if (bacteriaRandom.nextFloat() < vineChance) {
                    placeVinesOnNearbyWalls(chunk, x, z, wallBottomY, wallTopY, bacteriaRandom);
                }
            }
        }

        // Second pass: place a few shrooms in the center
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int x = centerX + dx;
                int z = centerZ + dz;
                if (x < 0 || x >= 16 || z < 0 || z >= 16) continue;

                BlockPos floorPos = new BlockPos(x, floorY, z);
                BlockState floorBlock = chunk.getBlockState(floorPos);

                if (!floorBlock.isSolid() || floorBlock.isAir()) continue;

                // Only 1-3 horizontal shrooms in the center
                if (bacteriaRandom.nextFloat() < 0.5f) {
                    BlockPos shroomPos = new BlockPos(x, wallBottomY, z);
                    if (chunk.getBlockState(shroomPos).isAir()) {
                        chunk.setBlockState(shroomPos,
                                ModBlocks.BACTERIA_SHROOM_HORIZONTAL.getDefaultState()
                                        .with(Properties.HORIZONTAL_FACING,
                                                Direction.Type.HORIZONTAL.random(bacteriaRandom)),
                                false);
                        decayNearbyWallpapers(chunk, x, z, wallBottomY, wallTopY, bacteriaRandom);
                    }
                }
            }
        }

        // Third pass: occasional vertical shrooms on walls in the middle ring
        for (int dx = -clusterSize; dx <= clusterSize; dx++) {
            for (int dz = -clusterSize; dz <= clusterSize; dz++) {
                int x = centerX + dx;
                int z = centerZ + dz;
                if (x < 0 || x >= 16 || z < 0 || z >= 16) continue;

                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist < clusterSize * 0.3f || dist > clusterSize * 0.7f) continue;

                // 15% chance for vertical shrooms in middle ring
                if (bacteriaRandom.nextFloat() < 0.15f) {
                    placeVerticalShroomOnWall(chunk, x, z, wallBottomY, wallTopY, bacteriaRandom);
                }
            }
        }
    }

    private void placeVinesOnNearbyWalls(Chunk chunk, int x, int z, int wallBottomY, int wallTopY, ChunkRandom random) {
        Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

        for (Direction dir : directions) {
            BlockPos wallPos = new BlockPos(x, wallBottomY + random.nextInt(wallTopY - wallBottomY), z).offset(dir);

            if (wallPos.getX() < 0 || wallPos.getX() >= 16 ||
                    wallPos.getZ() < 0 || wallPos.getZ() >= 16) continue;

            BlockState wallState = chunk.getBlockState(wallPos);

            if (isWallpaperBlock(wallState)) {
                // Place bacteria vine on the wall facing back toward the center
                BlockState vineState = ModBlocks.BACTERIA_VINE.getDefaultState();

                switch (dir.getOpposite()) {
                    case NORTH -> vineState = vineState.with(Properties.NORTH, true);
                    case SOUTH -> vineState = vineState.with(Properties.SOUTH, true);
                    case EAST -> vineState = vineState.with(Properties.EAST, true);
                    case WEST -> vineState = vineState.with(Properties.WEST, true);
                    case UP -> vineState = vineState.with(Properties.UP, true);
                    case DOWN -> vineState = vineState.with(Properties.DOWN, true);
                }

                chunk.setBlockState(wallPos, vineState, false);

                // Decay the wallpaper
                decayWallpaperAt(chunk, wallPos, random);
                return;
            }
        }
    }

    private void placeVerticalShroomOnWall(Chunk chunk, int x, int z, int wallBottomY, int wallTopY, ChunkRandom random) {
        Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

        for (Direction dir : directions) {
            int checkY = wallBottomY + random.nextInt(wallTopY - wallBottomY);
            BlockPos wallPos = new BlockPos(x, checkY, z).offset(dir);

            if (wallPos.getX() < 0 || wallPos.getX() >= 16 ||
                    wallPos.getZ() < 0 || wallPos.getZ() >= 16) continue;

            BlockState wallState = chunk.getBlockState(wallPos);

            if (isWallpaperBlock(wallState)) {
                BlockPos shroomPos = new BlockPos(x, checkY, z);
                BlockState currentBlock = chunk.getBlockState(shroomPos);

                if (currentBlock.isAir()) {
                    chunk.setBlockState(shroomPos,
                            ModBlocks.BACTERIA_SHROOM_VERTICAL.getDefaultState()
                                    .with(Properties.HORIZONTAL_FACING, dir),
                            false);

                    decayWallpaperAt(chunk, wallPos, random);
                    return;
                }
            }
        }
    }

    private void decayNearbyWallpapers(Chunk chunk, int centerX, int centerZ, int wallBottomY, int wallTopY, ChunkRandom random) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0) continue;
                int x = centerX + dx;
                int z = centerZ + dz;
                if (x < 0 || x >= 16 || z < 0 || z >= 16) continue;

                for (int y = wallBottomY; y <= wallTopY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = chunk.getBlockState(pos);

                    if (isWallpaperBlock(state) && random.nextFloat() < 0.15f) {
                        decayWallpaperAt(chunk, pos, random);
                    }
                }
            }
        }
    }

    private void decayWallpaperAt(Chunk chunk, BlockPos pos, ChunkRandom random) {
        BlockState currentState = chunk.getBlockState(pos);

        if (!isWallpaperBlock(currentState)) return;

        float roll = random.nextFloat();
        BlockState newState = currentState;

        if (roll < 0.25f) {
            newState = getWetVariant(currentState);
        } else if (roll < 0.45f) {
            newState = getStainedVariant(currentState);
        } else if (roll < 0.60f) {
            newState = getMoldyVariant(currentState);
        } else if (roll < 0.70f) {
            newState = getMoldInfectedVariant(currentState);
        }

        if (newState != currentState) {
            chunk.setBlockState(pos, newState, false);
        }
    }

    private BlockState getWetVariant(BlockState state) {
        Block block = state.getBlock();
        if (block == ModBlocks.WALLPAPER_BLOCK || block == ModBlocks.STAINED_WALLPAPER_BLOCK)
            return ModBlocks.WET_WALLPAPER_BLOCK.getDefaultState();
        if (block == ModBlocks.TORN_WALLPAPER_BLOCK || block == ModBlocks.STAINED_TORN_WALLPAPER_BLOCK)
            return ModBlocks.WET_TORN_WALLPAPER_BLOCK.getDefaultState();
        return state;
    }

    private BlockState getStainedVariant(BlockState state) {
        Block block = state.getBlock();
        if (block == ModBlocks.WALLPAPER_BLOCK)
            return ModBlocks.STAINED_WALLPAPER_BLOCK.getDefaultState();
        if (block == ModBlocks.TORN_WALLPAPER_BLOCK)
            return ModBlocks.STAINED_TORN_WALLPAPER_BLOCK.getDefaultState();
        return state;
    }

    private BlockState getMoldyVariant(BlockState state) {
        Block block = state.getBlock();
        if (isRegularWallpaper(block))
            return ModBlocks.MOLDY_WALLPAPER_BLOCK.getDefaultState();
        if (isTornWallpaper(block))
            return ModBlocks.MOLDY_TORN_WALLPAPER_BLOCK.getDefaultState();
        return state;
    }

    private BlockState getMoldInfectedVariant(BlockState state) {
        Block block = state.getBlock();
        if (isRegularWallpaper(block))
            return ModBlocks.MOLD_INFECTED_WALLPAPER_BLOCK.getDefaultState();
        if (isTornWallpaper(block))
            return ModBlocks.MOLD_INFECTED_TORN_WALLPAPER_BLOCK.getDefaultState();
        return state;
    }

    private boolean isRegularWallpaper(Block block) {
        return block == ModBlocks.WALLPAPER_BLOCK || block == ModBlocks.STAINED_WALLPAPER_BLOCK ||
                block == ModBlocks.WET_WALLPAPER_BLOCK || block == ModBlocks.MOLDY_WALLPAPER_BLOCK ||
                block == ModBlocks.MOLD_INFECTED_WALLPAPER_BLOCK;
    }

    private boolean isTornWallpaper(Block block) {
        return block == ModBlocks.TORN_WALLPAPER_BLOCK || block == ModBlocks.STAINED_TORN_WALLPAPER_BLOCK ||
                block == ModBlocks.WET_TORN_WALLPAPER_BLOCK || block == ModBlocks.MOLDY_TORN_WALLPAPER_BLOCK ||
                block == ModBlocks.MOLD_INFECTED_TORN_WALLPAPER_BLOCK;
    }

    private static class GridPos {
        final int x, z, y;
        GridPos(int x, int z, int y) { this.x = x; this.z = z; this.y = y; }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof GridPos other)) return false;
            return x == other.x && z == other.z && y == other.y;
        }

        @Override
        public int hashCode() {
            return x * 31 * 31 + z * 31 + y;
        }
    }

    // =========================================
    // PROPS
    // =========================================

    private void generateProps(Chunk chunk, int chunkStartX, int chunkStartZ,
                               int floorStartY, int floorY, int wallBottomY, int wallTopY) {

        long propSeed = ((long) chunkStartX * 341873128712L +
                (long) chunkStartZ * 132897987541L +
                floorStartY * 789012345L) ^ worldSeed;
        ChunkRandom propRandom = new ChunkRandom(new CheckedRandom(propSeed));

        List<PropPosition> wallPositions = new ArrayList<>();
        List<PropPosition> openPositions = new ArrayList<>();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkStartX + x;
                int worldZ = chunkStartZ + z;

                BlockState floorBlock = chunk.getBlockState(new BlockPos(x, floorY, z));
                if (floorBlock.isAir()) continue;

                BlockState aboveFloor = chunk.getBlockState(new BlockPos(x, wallBottomY, z));
                if (!aboveFloor.isAir()) continue;

                Direction wallDirection = getAdjacentWallDirection(chunk, x, z, wallBottomY);

                if (wallDirection != null) {
                    wallPositions.add(new PropPosition(x, z, worldX, worldZ, wallDirection));
                } else {
                    openPositions.add(new PropPosition(x, z, worldX, worldZ, null));
                }
            }
        }

        Collections.shuffle(wallPositions, new Random(propRandom.nextLong()));
        Collections.shuffle(openPositions, new Random(propRandom.nextLong()));

        // Generate Lockers (30% chance)
        if (!wallPositions.isEmpty() && propRandom.nextFloat() < 0.3f) {
            int lockerCount = Math.min(propRandom.nextInt(2) + 1, wallPositions.size());
            for (int i = 0; i < lockerCount && i < wallPositions.size(); i++) {
                PropPosition pos = wallPositions.get(i);
                placeLocker(chunk, pos.x, pos.z, wallBottomY, pos.wallDirection);
            }
        }

        // Generate Water Coolers (5% chance)
        if (propRandom.nextFloat() < 0.05f) {
            List<PropPosition> coolerPositions = new ArrayList<>();
            coolerPositions.addAll(openPositions);
            coolerPositions.addAll(wallPositions);
            Collections.shuffle(coolerPositions, new Random(propRandom.nextLong()));

            if (!coolerPositions.isEmpty()) {
                PropPosition pos = coolerPositions.get(0);
                placeWaterCooler(chunk, pos.x, pos.z, wallBottomY, pos.wallDirection);
            }
        }

        // Generate Barrels (5% chance)
        if (propRandom.nextFloat() < 0.05f) {
            List<PropPosition> barrelPositions = new ArrayList<>();
            barrelPositions.addAll(openPositions);
            barrelPositions.addAll(wallPositions);
            Collections.shuffle(barrelPositions, new Random(propRandom.nextLong()));

            int barrelCount = Math.min(propRandom.nextInt(2) + 1, barrelPositions.size());
            for (int i = 0; i < barrelCount && i < barrelPositions.size(); i++) {
                PropPosition pos = barrelPositions.get(i);
                Direction facing = pos.wallDirection != null ?
                        pos.wallDirection : Direction.fromHorizontal(propRandom.nextInt(4));
                placeBarrel(chunk, pos.x, pos.z, wallBottomY, facing);
            }
        }
    }

    private void generateCreepyWallDecor(Chunk chunk, int chunkStartX, int chunkStartZ,
                                         int floorStartY, int wallBottomY, int wallTopY) {

        long decorSeed = ((long) chunkStartX * 341873128712L +
                (long) chunkStartZ * 132897987541L +
                floorStartY * 888888L) ^ worldSeed;
        ChunkRandom decorRandom = new ChunkRandom(new CheckedRandom(decorSeed));

        if (decorRandom.nextFloat() >= 0.03f) return;

        int x = decorRandom.nextInt(14) + 1;
        int z = decorRandom.nextInt(14) + 1;

        BlockPos signPos = new BlockPos(x, wallBottomY, z);

        if (chunk.getBlockState(signPos).isAir()) {
            BlockState signState = Blocks.DARK_OAK_SIGN.getDefaultState()
                    .with(Properties.ROTATION, decorRandom.nextInt(16));

            chunk.setBlockState(signPos, signState, false);

            BlockEntity signEntity = chunk.getBlockEntity(signPos);
            if (signEntity instanceof SignBlockEntity sign) {
                String[] messages = getCreepyMessage(decorRandom);
                SignText signText = sign.getText(false);
                for (int i = 0; i < 4; i++) {
                    signText = signText.withMessage(i, Text.literal(messages[i]));
                }
                signText = signText.withColor(DyeColor.BLACK);
                sign.setText(signText, false);
            }
        }
    }

    private void generateMossPatch(Chunk chunk, int chunkStartX, int chunkStartZ,
                                   int floorStartY, int floorY, int wallBottomY) {

        long mossSeed = ((long) chunkStartX * 341873128712L +
                (long) chunkStartZ * 132897987541L +
                floorStartY * 777777L) ^ worldSeed;
        ChunkRandom mossRandom = new ChunkRandom(new CheckedRandom(mossSeed));

        if (mossRandom.nextFloat() >= 0.03f) return;

        int centerX = mossRandom.nextInt(10) + 3;
        int centerZ = mossRandom.nextInt(10) + 3;

        int patchSize = mossRandom.nextInt(3) + 2;

        for (int dx = -patchSize; dx <= patchSize; dx++) {
            for (int dz = -patchSize; dz <= patchSize; dz++) {
                int x = centerX + dx;
                int z = centerZ + dz;
                if (x < 0 || x >= 16 || z < 0 || z >= 16) continue;

                double noiseOffset = (mossRandom.nextFloat() - 0.5f) * 2.0f;
                double dist = Math.sqrt(dx * dx + dz * dz) + noiseOffset;

                if (dist > patchSize) continue;

                BlockPos floorPos = new BlockPos(x, floorY, z);
                BlockState floorBlock = chunk.getBlockState(floorPos);
                BlockPos abovePos = new BlockPos(x, wallBottomY, z);

                if (dist < patchSize * 0.6f && floorBlock.isOf(Blocks.YELLOW_TERRACOTTA)) {
                    chunk.setBlockState(floorPos, Blocks.MOSS_BLOCK.getDefaultState(), false);

                    float propRoll = mossRandom.nextFloat();
                    if (propRoll < 0.30f) {
                        if (chunk.getBlockState(abovePos).isAir()) {
                            chunk.setBlockState(abovePos, Blocks.MOSS_CARPET.getDefaultState(), false);
                        }
                    } else if (propRoll < 0.55f) {
                        placeLichenOnNearbyWalls(chunk, x, wallBottomY, z, mossRandom);
                    } else if (propRoll < 0.70f) {
                        if (chunk.getBlockState(abovePos).isAir()) {
                            Block mushroomBlock = mossRandom.nextBoolean() ? Blocks.BROWN_MUSHROOM : Blocks.RED_MUSHROOM;
                            chunk.setBlockState(abovePos, mushroomBlock.getDefaultState(), false);
                        }
                    } else if (propRoll < 0.80f) {
                        if (chunk.getBlockState(abovePos).isAir()) {
                            chunk.setBlockState(abovePos, Blocks.AZALEA.getDefaultState(), false);
                        }
                    }
                } else if (dist < patchSize * 0.8f && floorBlock.isOf(Blocks.YELLOW_TERRACOTTA)) {
                    if (mossRandom.nextFloat() < 0.3f && chunk.getBlockState(abovePos).isAir()) {
                        chunk.setBlockState(abovePos, Blocks.MOSS_CARPET.getDefaultState(), false);
                    }
                }
            }
        }

        if (mossRandom.nextFloat() < 0.08f) {
            int skullX = centerX + mossRandom.nextInt(3) - 1;
            int skullZ = centerZ + mossRandom.nextInt(3) - 1;
            if (skullX >= 0 && skullX < 16 && skullZ >= 0 && skullZ < 16) {
                BlockPos skullPos = new BlockPos(skullX, wallBottomY, skullZ);
                BlockState skullBlock = chunk.getBlockState(skullPos);
                if (skullBlock.isAir() || skullBlock.isOf(Blocks.MOSS_CARPET) || skullBlock.isOf(Blocks.AZALEA)) {
                    chunk.setBlockState(skullPos, Blocks.SKELETON_SKULL.getDefaultState()
                            .with(Properties.ROTATION, mossRandom.nextInt(16)), false);
                }
            }
        }
    }

    private void placeLichenOnNearbyWalls(Chunk chunk, int x, int y, int z, ChunkRandom random) {
        Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

        for (Direction dir : directions) {
            for (int dist = 1; dist <= 2; dist++) {
                BlockPos checkPos = new BlockPos(x, y, z).offset(dir, dist);

                if (checkPos.getX() < 0 || checkPos.getX() >= 16 ||
                        checkPos.getZ() < 0 || checkPos.getZ() >= 16) continue;

                BlockState wallState = chunk.getBlockState(checkPos);

                if (wallState.isSolid() && isWallpaperBlock(wallState)) {
                    BlockState lichenState = Blocks.GLOW_LICHEN.getDefaultState();

                    switch (dir.getOpposite()) {
                        case DOWN -> lichenState = lichenState.with(Properties.DOWN, true);
                        case UP -> lichenState = lichenState.with(Properties.UP, true);
                        case NORTH -> lichenState = lichenState.with(Properties.NORTH, true);
                        case SOUTH -> lichenState = lichenState.with(Properties.SOUTH, true);
                        case WEST -> lichenState = lichenState.with(Properties.WEST, true);
                        case EAST -> lichenState = lichenState.with(Properties.EAST, true);
                    }

                    chunk.setBlockState(checkPos, lichenState, false);
                    return;
                }
            }
        }
    }

    private boolean isWallpaperBlock(BlockState state) {
        return state.isOf(ModBlocks.WALLPAPER_BLOCK) ||
                state.isOf(ModBlocks.STAINED_WALLPAPER_BLOCK) ||
                state.isOf(ModBlocks.WET_WALLPAPER_BLOCK) ||
                state.isOf(ModBlocks.MOLDY_WALLPAPER_BLOCK) ||
                state.isOf(ModBlocks.MOLD_INFECTED_WALLPAPER_BLOCK) ||
                state.isOf(ModBlocks.SPONGE_WALLPAPER_BLOCK) ||
                state.isOf(ModBlocks.TORN_WALLPAPER_BLOCK) ||
                state.isOf(ModBlocks.STAINED_TORN_WALLPAPER_BLOCK) ||
                state.isOf(ModBlocks.WET_TORN_WALLPAPER_BLOCK) ||
                state.isOf(ModBlocks.MOLDY_TORN_WALLPAPER_BLOCK) ||
                state.isOf(ModBlocks.MOLD_INFECTED_TORN_WALLPAPER_BLOCK);
    }

    private String[] getCreepyMessage(ChunkRandom random) {
        String[][] messages = {
                {"", "DON'T", "LOOK", ""},
                {"", "HELP", "ME", ""},
                {"", "RUN", "", ""},
                {"", "IT SEES", "YOU", ""},
                {"", "NO", "EXIT", ""},
                {"", "BEHIND", "YOU", ""},
                {"", "→", "", ""},
                {"", "↓↓", "", ""},
                {"", "SHE", "WATCHES", ""},
                {"", "WAKE", "UP", ""},
                {"", "THE END", "IS NEAR", ""},
                {"", "FOLLOW", "THE LIGHT", ""},
                {"", "TRUST", "NO ONE", ""},
                {"", "DON'T", "TURN", "AROUND"},
                {"", "IT'S", "HERE", ""},
                {"", "SAVE", "YOURSELF", ""},
                {"", "DEAD", "END", ""},
                {"", "GO", "BACK", ""}
        };
        return messages[random.nextInt(messages.length)];
    }

    private void generateHoles(Chunk chunk, int chunkStartX, int chunkStartZ, int floorStartY,
                               int floorY, int ceilingY, int lightY) {

        if (floorStartY <= 8 || floorStartY >= 248) return;

        long holeSeed = ((long) chunkStartX * 341873128712L +
                (long) chunkStartZ * 132897987541L +
                floorStartY * 111111111L) ^ worldSeed;
        ChunkRandom holeRandom = new ChunkRandom(new CheckedRandom(holeSeed));

        if (holeRandom.nextFloat() >= 0.01f) return;

        int holeType = holeRandom.nextInt(4);
        int holeX = holeRandom.nextInt(8) + 4;
        int holeZ = holeRandom.nextInt(8) + 4;

        switch (holeType) {
            case 0:
                punchHole(chunk, holeX, holeZ, floorY, ceilingY, lightY);
                break;
            case 1:
                for (int dx = 0; dx < 2; dx++)
                    for (int dz = 0; dz < 2; dz++)
                        punchHole(chunk, holeX + dx, holeZ + dz, floorY, ceilingY, lightY);
                break;
            case 2:
                punchHole(chunk, holeX, holeZ, floorY, ceilingY, lightY);
                punchHole(chunk, holeX + 1, holeZ, floorY, ceilingY, lightY);
                punchHole(chunk, holeX - 1, holeZ, floorY, ceilingY, lightY);
                punchHole(chunk, holeX, holeZ + 1, floorY, ceilingY, lightY);
                punchHole(chunk, holeX, holeZ - 1, floorY, ceilingY, lightY);
                break;
            case 3:
                for (int dx = -3; dx <= 2; dx++)
                    for (int dz = -3; dz <= 2; dz++)
                        punchHole(chunk, holeX + dx, holeZ + dz, floorY, ceilingY, lightY);
                break;
        }
    }

    private void punchHole(Chunk chunk, int x, int z, int floorY, int ceilingY, int lightY) {
        if (x < 0 || x >= 16 || z < 0 || z >= 16) return;

        chunk.setBlockState(new BlockPos(x, floorY, z), Blocks.AIR.getDefaultState(), false);
        chunk.setBlockState(new BlockPos(x, floorY - 1, z), Blocks.AIR.getDefaultState(), false);
        chunk.setBlockState(new BlockPos(x, floorY - 2, z), Blocks.AIR.getDefaultState(), false);

        chunk.setBlockState(new BlockPos(x, lightY, z), Blocks.AIR.getDefaultState(), false);
        chunk.setBlockState(new BlockPos(x, ceilingY, z), Blocks.AIR.getDefaultState(), false);
        chunk.setBlockState(new BlockPos(x, ceilingY + 1, z), Blocks.AIR.getDefaultState(), false);
        chunk.setBlockState(new BlockPos(x, ceilingY + 2, z), Blocks.AIR.getDefaultState(), false);

        chunk.setBlockState(new BlockPos(x, floorY + 8, z), Blocks.AIR.getDefaultState(), false);
    }

    private Direction getAdjacentWallDirection(Chunk chunk, int x, int z, int wallY) {
        if (x > 0 && !chunk.getBlockState(new BlockPos(x - 1, wallY, z)).isAir()) return Direction.EAST;
        if (x < 15 && !chunk.getBlockState(new BlockPos(x + 1, wallY, z)).isAir()) return Direction.WEST;
        if (z > 0 && !chunk.getBlockState(new BlockPos(x, wallY, z - 1)).isAir()) return Direction.SOUTH;
        if (z < 15 && !chunk.getBlockState(new BlockPos(x, wallY, z + 1)).isAir()) return Direction.NORTH;
        return null;
    }

    private void placeLocker(Chunk chunk, int x, int z, int floorY, Direction facing) {
        BlockState lockerState = ModBlocks.LOCKER.getDefaultState()
                .with(Properties.HORIZONTAL_FACING, facing)
                .with(Properties.OPEN, false);

        chunk.setBlockState(new BlockPos(x, floorY, z),
                lockerState.with(Properties.DOUBLE_BLOCK_HALF, net.minecraft.block.enums.DoubleBlockHalf.LOWER), false);
        chunk.setBlockState(new BlockPos(x, floorY + 1, z),
                lockerState.with(Properties.DOUBLE_BLOCK_HALF, net.minecraft.block.enums.DoubleBlockHalf.UPPER), false);
    }

    private void placeWaterCooler(Chunk chunk, int x, int z, int floorY, Direction facing) {
        Direction coolerFacing = facing != null ? facing : Direction.fromHorizontal((int) (worldSeed % 4));
        BlockState coolerState = ModBlocks.WATER_COOLER.getDefaultState()
                .with(Properties.HORIZONTAL_FACING, coolerFacing);
        chunk.setBlockState(new BlockPos(x, floorY, z), coolerState, false);
    }

    private void placeBarrel(Chunk chunk, int x, int z, int floorY, Direction facing) {
        BlockState barrelState = Blocks.BARREL.getDefaultState()
                .with(BarrelBlock.FACING, facing.getOpposite());
        chunk.setBlockState(new BlockPos(x, floorY, z), barrelState, false);
    }

    private static class PropPosition {
        final int x, z, worldX, worldZ;
        final Direction wallDirection;

        PropPosition(int x, int z, int worldX, int worldZ, Direction wallDirection) {
            this.x = x; this.z = z; this.worldX = worldX; this.worldZ = worldZ;
            this.wallDirection = wallDirection;
        }
    }

    private void generateBoundaryWalls(Chunk chunk, int chunkStartX, int chunkStartZ,
                                       int floorStartY, int wallBottomY, int wallTopY) {
        long chunkSeed = ((long) chunkStartX * 341873128712L +
                (long) chunkStartZ * 132897987541L + floorStartY * 456789L) ^ worldSeed;
        ChunkRandom chunkRandom = new ChunkRandom(new CheckedRandom(chunkSeed));

        float wallRoll = chunkRandom.nextFloat();
        int numBoundaryWalls;
        if (wallRoll < 0.4f) numBoundaryWalls = 0;
        else if (wallRoll < 0.7f) numBoundaryWalls = 1;
        else if (wallRoll < 0.9f) numBoundaryWalls = 2;
        else numBoundaryWalls = 3;

        if (numBoundaryWalls == 0) return;

        List<Integer> boundaries = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
        Collections.shuffle(boundaries, new Random(chunkRandom.nextLong()));

        for (int i = 0; i < numBoundaryWalls; i++) {
            int boundary = boundaries.get(i);
            boolean hasOpening = chunkRandom.nextFloat() < 0.7f;
            int openingPos = hasOpening ? chunkRandom.nextInt(14) + 1 : -1;

            switch (boundary) {
                case 0: for (int x = 0; x < 16; x++) { if (x != openingPos && x != openingPos + 1) placeWallBlock(chunk, x, 0, wallBottomY, wallTopY, chunkStartX, chunkStartZ, floorStartY); } break;
                case 1: for (int x = 0; x < 16; x++) { if (x != openingPos && x != openingPos + 1) placeWallBlock(chunk, x, 15, wallBottomY, wallTopY, chunkStartX, chunkStartZ, floorStartY); } break;
                case 2: for (int z = 0; z < 16; z++) { if (z != openingPos && z != openingPos + 1) placeWallBlock(chunk, 0, z, wallBottomY, wallTopY, chunkStartX, chunkStartZ, floorStartY); } break;
                case 3: for (int z = 0; z < 16; z++) { if (z != openingPos && z != openingPos + 1) placeWallBlock(chunk, 15, z, wallBottomY, wallTopY, chunkStartX, chunkStartZ, floorStartY); } break;
            }
        }
    }

    private void generateInternalWalls(Chunk chunk, int chunkStartX, int chunkStartZ,
                                       int floorStartY, int wallBottomY, int wallTopY) {
        long chunkSeed = ((long) chunkStartX * 341873128712L +
                (long) chunkStartZ * 132897987541L + floorStartY * 987654L) ^ worldSeed;
        ChunkRandom chunkRandom = new ChunkRandom(new CheckedRandom(chunkSeed));
        if (chunkRandom.nextFloat() >= 0.4f) return;
        int wallCount = chunkRandom.nextInt(2) + 1;
        for (int i = 0; i < wallCount; i++) {
            generateRandomWallShape(chunk, chunkStartX, chunkStartZ, wallBottomY, wallTopY, chunkRandom, floorStartY);
        }
    }

    private void generateRandomWallShape(Chunk chunk, int chunkStartX, int chunkStartZ,
                                         int wallBottomY, int wallTopY, ChunkRandom random, int floorStartY) {
        int wallType = random.nextInt(6);
        int startX = random.nextInt(14) + 1, startZ = random.nextInt(14) + 1;
        int thickness = random.nextFloat() < 0.3f ? 2 : 1;
        int length = random.nextInt(8) + 3;

        switch (wallType) {
            case 0: for (int dx = 0; dx < length && (startX + dx) < 15; dx++) { placeWallBlock(chunk, startX + dx, startZ, wallBottomY, wallTopY, chunkStartX, chunkStartZ, floorStartY); if (thickness == 2 && startZ + 1 < 15) placeWallBlock(chunk, startX + dx, startZ + 1, wallBottomY, wallTopY, chunkStartX, chunkStartZ, floorStartY); } break;
            case 1: for (int dz = 0; dz < length && (startZ + dz) < 15; dz++) { placeWallBlock(chunk, startX, startZ + dz, wallBottomY, wallTopY, chunkStartX, chunkStartZ, floorStartY); if (thickness == 2 && startX + 1 < 15) placeWallBlock(chunk, startX + 1, startZ + dz, wallBottomY, wallTopY, chunkStartX, chunkStartZ, floorStartY); } break;
            case 2: int legLength = length / 2; for (int dx = 0; dx < legLength && (startX + dx) < 15; dx++) placeWallBlock(chunk, startX + dx, startZ, wallBottomY, wallTopY, chunkStartX, chunkStartZ, floorStartY); for (int dz = 0; dz < legLength && (startZ + dz) < 15; dz++) placeWallBlock(chunk, startX, startZ + dz, wallBottomY, wallTopY, chunkStartX, chunkStartZ, floorStartY); break;
            case 3: int armLength = length / 4; for (int dx = -armLength; dx <= armLength; dx++) { int px = startX + dx; if (px > 0 && px < 15) placeWallBlock(chunk, px, startZ, wallBottomY, wallTopY, chunkStartX, chunkStartZ, floorStartY); } for (int dz = -armLength; dz <= armLength; dz++) { int pz = startZ + dz; if (pz > 0 && pz < 15) placeWallBlock(chunk, startX, pz, wallBottomY, wallTopY, chunkStartX, chunkStartZ, floorStartY); } break;
            case 4: int topLen = length / 3; for (int dx = -topLen; dx <= topLen; dx++) { int px = startX + dx; if (px > 0 && px < 15) placeWallBlock(chunk, px, startZ, wallBottomY, wallTopY, chunkStartX, chunkStartZ, floorStartY); } for (int dz = 0; dz < length - topLen && (startZ + dz) < 15; dz++) placeWallBlock(chunk, startX, startZ + dz, wallBottomY, wallTopY, chunkStartX, chunkStartZ, floorStartY); break;
            case 5: int bracketLen = length / 2; for (int dx = 0; dx < bracketLen && (startX + dx) < 15; dx++) { placeWallBlock(chunk, startX + dx, startZ, wallBottomY, wallTopY, chunkStartX, chunkStartZ, floorStartY); if (startZ + bracketLen < 15) placeWallBlock(chunk, startX + dx, startZ + bracketLen, wallBottomY, wallTopY, chunkStartX, chunkStartZ, floorStartY); } for (int dz = 0; dz < bracketLen && (startZ + dz) < 15; dz++) placeWallBlock(chunk, startX, startZ + dz, wallBottomY, wallTopY, chunkStartX, chunkStartZ, floorStartY); break;
        }
    }

    private void generateColumns(Chunk chunk, int chunkStartX, int chunkStartZ,
                                 int floorStartY, int wallBottomY, int wallTopY) {
        long chunkSeed = ((long) chunkStartX * 341873128712L +
                (long) chunkStartZ * 132897987541L + floorStartY * 123456L) ^ worldSeed;
        ChunkRandom chunkRandom = new ChunkRandom(new CheckedRandom(chunkSeed));
        if (chunkRandom.nextFloat() >= 0.3f) return;
        int columnCount = chunkRandom.nextInt(3) + 1;
        for (int i = 0; i < columnCount; i++) {
            int centerX = chunkRandom.nextInt(12) + 2, centerZ = chunkRandom.nextInt(12) + 2;
            float sizeRoll = chunkRandom.nextFloat();
            int size;
            if (sizeRoll < 0.4f) size = 1;
            else if (sizeRoll < 0.7f) size = 2;
            else if (sizeRoll < 0.9f) size = 3;
            else size = 4;
            for (int dx = -size/2; dx <= size/2; dx++) {
                for (int dz = -size/2; dz <= size/2; dz++) {
                    int px = centerX + dx, pz = centerZ + dz;
                    if (px >= 0 && px < 16 && pz >= 0 && pz < 16) {
                        for (int y = wallBottomY; y <= wallTopY; y++) {
                            BlockState wallBlock = getWallBlockForY(chunkStartX + px, chunkStartZ + pz, floorStartY, y, wallBottomY);
                            chunk.setBlockState(new BlockPos(px, y, pz), wallBlock, false);
                        }
                    }
                }
            }
        }
    }

    private void placeWallBlock(Chunk chunk, int x, int z, int wallBottomY, int wallTopY,
                                int chunkStartX, int chunkStartZ, int floorStartY) {
        for (int y = wallBottomY; y <= wallTopY; y++) {
            BlockState wallBlock = getWallBlockForY(chunkStartX + x, chunkStartZ + z, floorStartY, y, wallBottomY);
            chunk.setBlockState(new BlockPos(x, y, z), wallBlock, false);
        }
    }

    private BlockState getWallBlockForY(int worldX, int worldZ, int floorStartY, int y, int wallBottomY) {
        if (y == wallBottomY) return ModBlocks.BACKBOARD_BLOCK.getDefaultState();
        return getOrganicWallBlock(worldX, worldZ, floorStartY);
    }

    private BlockState getLightPatternBlock(int localX, int localZ, int worldX, int worldZ) {
        if (localZ == 7 || localZ == 8) {
            if (localX == 3 || localX == 11) return getLightBlockWithCorruption(worldX, worldZ);
        }
        return ModBlocks.BACKBOARD_BLOCK.getDefaultState();
    }

    private BlockState getLightBlockWithCorruption(int worldX, int worldZ) {
        long lightSeed = ((long) worldX * 341873128712L + (long) worldZ * 132897987541L) ^ worldSeed;
        ChunkRandom lightRandom = new ChunkRandom(new CheckedRandom(lightSeed));
        double darkZoneNoise = getDarkZoneNoise(worldX, worldZ);
        if (darkZoneNoise > 0.6) {
            return lightRandom.nextFloat() < 0.9f ? Blocks.BONE_BLOCK.getDefaultState() : Blocks.OCHRE_FROGLIGHT.getDefaultState();
        } else {
            return lightRandom.nextFloat() < 0.1f ? Blocks.BONE_BLOCK.getDefaultState() : Blocks.OCHRE_FROGLIGHT.getDefaultState();
        }
    }

    private double getDarkZoneNoise(int worldX, int worldZ) {
        double scale = 0.005, worldSeedOffset = worldSeed * 0.0001;
        double value = Math.sin((worldX + worldSeedOffset) * scale) * Math.cos((worldZ - worldSeedOffset) * scale * 1.3) +
                Math.sin((worldX + worldSeedOffset) * scale * 0.7 + 1.5) * Math.cos((worldZ - worldSeedOffset) * scale * 0.9);
        return (value + 2.0) / 4.0;
    }

    private BlockState getOrganicWallBlock(int worldX, int worldZ, int floorY) {
        long spongeSeed = ((long) worldX * 341873128712L + (long) worldZ * 132897987541L + floorY * 999L) ^ worldSeed;
        ChunkRandom spongeRandom = new ChunkRandom(new CheckedRandom(spongeSeed));
        if (spongeRandom.nextFloat() < 0.1f) return ModBlocks.SPONGE_WALLPAPER_BLOCK.getDefaultState();

        MoldCluster[] clusters = getMoldClustersForRegion(worldX, worldZ);
        double strongestMold = 0;
        for (MoldCluster cluster : clusters) {
            double dx = worldX - cluster.centerX, dz = worldZ - cluster.centerZ;
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance < cluster.radius) {
                double noiseOffset = getMoldNoise(worldX, worldZ, cluster.seed) * 1.5;
                double adjustedDistance = distance + noiseOffset;
                if (adjustedDistance < cluster.radius) {
                    double influence = 1.0 - (adjustedDistance / cluster.radius);
                    strongestMold = Math.max(strongestMold, influence);
                }
            }
        }
        if (strongestMold > 0.85) return ModBlocks.MOLD_INFECTED_WALLPAPER_BLOCK.getDefaultState();
        else if (strongestMold > 0.7) return ModBlocks.MOLDY_WALLPAPER_BLOCK.getDefaultState();
        else if (strongestMold > 0.5) return ModBlocks.WET_WALLPAPER_BLOCK.getDefaultState();
        else if (strongestMold > 0.3) return ModBlocks.STAINED_WALLPAPER_BLOCK.getDefaultState();
        else return ModBlocks.WALLPAPER_BLOCK.getDefaultState();
    }

    private MoldCluster[] getMoldClustersForRegion(int worldX, int worldZ) {
        int regionX = worldX / 64, regionZ = worldZ / 64;
        long regionKey = ((long) regionX << 32) | (regionZ & 0xFFFFFFFFL);
        if (moldCache.containsKey(regionKey)) return moldCache.get(regionKey);

        long regionSeed = ((long) regionX * 341873128712L + (long) regionZ * 132897987541L) ^ worldSeed;
        ChunkRandom regionRandom = new ChunkRandom(new CheckedRandom(regionSeed));
        if (regionRandom.nextFloat() > 0.15f) { MoldCluster[] empty = new MoldCluster[0]; moldCache.put(regionKey, empty); return empty; }

        int numClusters = regionRandom.nextInt(2) + 1;
        MoldCluster[] clusters = new MoldCluster[numClusters];
        for (int i = 0; i < numClusters; i++) {
            int centerX = regionX * 64 + regionRandom.nextInt(64), centerZ = regionZ * 64 + regionRandom.nextInt(64);
            int radius = regionRandom.nextInt(2) + 2;
            long clusterSeed = regionRandom.nextLong();
            clusters[i] = new MoldCluster(centerX, centerZ, radius, clusterSeed);
        }
        moldCache.put(regionKey, clusters);
        return clusters;
    }

    private double getMoldNoise(int worldX, int worldZ, long seed) {
        double scale = 0.3;
        return Math.sin(worldX * scale + seed) * Math.cos(worldZ * scale - seed * 0.7) * 0.5;
    }

    private static class MoldCluster {
        final int centerX, centerZ, radius;
        final long seed;
        MoldCluster(int centerX, int centerZ, int radius, long seed) {
            this.centerX = centerX; this.centerZ = centerZ; this.radius = radius; this.seed = seed;
        }
    }
}