package net.dalerd.backroomsbounded.world.gen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.dalerd.backroomsbounded.block.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
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
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.util.Identifier;

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

    // Cache mold data per chunk to ensure consistency
    private Map<Long, MoldCluster[]> moldCache = new HashMap<>();

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

        // Generate floors from Y=0 to Y=256
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

        // Step 1: Generate floor and ceiling for entire chunk
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkStartX + x;
                int worldZ = chunkStartZ + z;

                // Floor - yellow terracotta
                chunk.setBlockState(new BlockPos(x, floorY, z),
                        Blocks.YELLOW_TERRACOTTA.getDefaultState(), false);

                // Top ceiling - backboard block
                chunk.setBlockState(new BlockPos(x, ceilingY, z),
                        ModBlocks.BACKBOARD_BLOCK.getDefaultState(), false);

                // Light layer
                BlockState lightBlock = getLightPatternBlock(x, z, worldX, worldZ);
                chunk.setBlockState(new BlockPos(x, lightY, z), lightBlock, false);

                // Fill with air initially
                for (int y = wallBottomY; y <= wallTopY; y++) {
                    chunk.setBlockState(new BlockPos(x, y, z),
                            Blocks.AIR.getDefaultState(), false);
                }
            }
        }

        // Step 2: Generate walls, columns, and props
        generateBoundaryWalls(chunk, chunkStartX, chunkStartZ, floorStartY, wallBottomY, wallTopY);
        generateInternalWalls(chunk, chunkStartX, chunkStartZ, floorStartY, wallBottomY, wallTopY);
        generateColumns(chunk, chunkStartX, chunkStartZ, floorStartY, wallBottomY, wallTopY);
        generateProps(chunk, chunkStartX, chunkStartZ, floorStartY, floorY, wallBottomY, wallTopY);

        // Step 3: Generate rare holes (1% chance, middle floors only)
        generateHoles(chunk, chunkStartX, chunkStartZ, floorStartY, floorY, ceilingY, lightY);
    }

    private void generateProps(Chunk chunk, int chunkStartX, int chunkStartZ,
                               int floorStartY, int floorY, int wallBottomY, int wallTopY) {

        long propSeed = ((long) chunkStartX * 341873128712L +
                (long) chunkStartZ * 132897987541L +
                floorStartY * 789012345L) ^ worldSeed;
        ChunkRandom propRandom = new ChunkRandom(new CheckedRandom(propSeed));

        // Collect all valid positions for props
        List<PropPosition> wallPositions = new ArrayList<>();
        List<PropPosition> openPositions = new ArrayList<>();

        // Scan the chunk for valid prop positions
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkStartX + x;
                int worldZ = chunkStartZ + z;

                // Check if this floor position is empty (air)
                BlockState floorBlock = chunk.getBlockState(new BlockPos(x, floorY, z));
                if (floorBlock.isAir()) continue;

                BlockState aboveFloor = chunk.getBlockState(new BlockPos(x, wallBottomY, z));
                if (!aboveFloor.isAir()) continue;

                // Check for adjacent walls
                Direction wallDirection = getAdjacentWallDirection(chunk, x, z, wallBottomY);

                if (wallDirection != null) {
                    // Position against a wall
                    wallPositions.add(new PropPosition(x, z, worldX, worldZ, wallDirection));
                } else {
                    // Open position (not against a wall)
                    openPositions.add(new PropPosition(x, z, worldX, worldZ, null));
                }
            }
        }

        // Shuffle positions for randomness
        Collections.shuffle(wallPositions, new Random(propRandom.nextLong()));
        Collections.shuffle(openPositions, new Random(propRandom.nextLong()));

        // Generate Lockers (30% chance) - only against walls, facing outward
        if (!wallPositions.isEmpty() && propRandom.nextFloat() < 0.3f) {
            int lockerCount = Math.min(propRandom.nextInt(2) + 1, wallPositions.size()); // 1-2 lockers

            for (int i = 0; i < lockerCount && i < wallPositions.size(); i++) {
                PropPosition pos = wallPositions.get(i);
                placeLocker(chunk, pos.x, pos.z, wallBottomY, pos.wallDirection);
            }
        }

        // Generate Water Coolers (5% chance) - middle of room or near wall
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

        // Generate Barrels (10% chance) - middle of room or against walls
        if (propRandom.nextFloat() < 0.1f) {
            List<PropPosition> barrelPositions = new ArrayList<>();
            barrelPositions.addAll(openPositions);
            barrelPositions.addAll(wallPositions);
            Collections.shuffle(barrelPositions, new Random(propRandom.nextLong()));

            int barrelCount = Math.min(propRandom.nextInt(2) + 1, barrelPositions.size()); // 1-2 barrels

            for (int i = 0; i < barrelCount && i < barrelPositions.size(); i++) {
                PropPosition pos = barrelPositions.get(i);
                Direction facing = pos.wallDirection != null ?
                        pos.wallDirection : Direction.fromHorizontal(propRandom.nextInt(4));
                placeBarrel(chunk, pos.x, pos.z, wallBottomY, facing);
            }
        }

        // Generate Chests (10% chance) - middle of room or against walls, facing outward from walls
        if (propRandom.nextFloat() < 0.1f) {
            List<PropPosition> chestPositions = new ArrayList<>();

            // Prefer wall positions for chests
            if (!wallPositions.isEmpty()) {
                chestPositions.addAll(wallPositions);
            }
            chestPositions.addAll(openPositions);
            Collections.shuffle(chestPositions, new Random(propRandom.nextLong()));

            int chestCount = Math.min(propRandom.nextInt(1) + 1, chestPositions.size()); // 1 chest only

            for (int i = 0; i < chestCount && i < chestPositions.size(); i++) {
                PropPosition pos = chestPositions.get(i);
                Direction facing = pos.wallDirection != null ?
                        pos.wallDirection : Direction.fromHorizontal(propRandom.nextInt(4));
                placeChest(chunk, pos.x, pos.z, wallBottomY, facing);
            }
        }
    }

    private void generateHoles(Chunk chunk, int chunkStartX, int chunkStartZ, int floorStartY,
                               int floorY, int ceilingY, int lightY) {

        // Only generate holes on floors above Y=8 (not the bottom floor)
        // and below Y=248 (not the top floor)
        if (floorStartY <= 8 || floorStartY >= 248) return;

        long holeSeed = ((long) chunkStartX * 341873128712L +
                (long) chunkStartZ * 132897987541L +
                floorStartY * 111111111L) ^ worldSeed;
        ChunkRandom holeRandom = new ChunkRandom(new CheckedRandom(holeSeed));

        // 1% chance to generate a hole in this chunk on this floor
        if (holeRandom.nextFloat() >= 0.01f) return;

        // Choose hole type: 0 = single block, 1 = 2x2, 2 = plus shape, 3 = 6x3x6 room
        int holeType = holeRandom.nextInt(4);

        // Choose hole position (avoid edges to prevent holes in walls)
        int holeX = holeRandom.nextInt(8) + 4; // 4-11 (more centered for larger holes)
        int holeZ = holeRandom.nextInt(8) + 4; // 4-11

        switch (holeType) {
            case 0: // Single block hole
                punchHole(chunk, holeX, holeZ, floorY, ceilingY, lightY);
                break;
            case 1: // 2x2 hole
                for (int dx = 0; dx < 2; dx++) {
                    for (int dz = 0; dz < 2; dz++) {
                        punchHole(chunk, holeX + dx, holeZ + dz, floorY, ceilingY, lightY);
                    }
                }
                break;
            case 2: // Plus-shaped hole (center + 4 directions)
                punchHole(chunk, holeX, holeZ, floorY, ceilingY, lightY); // center
                punchHole(chunk, holeX + 1, holeZ, floorY, ceilingY, lightY); // east
                punchHole(chunk, holeX - 1, holeZ, floorY, ceilingY, lightY); // west
                punchHole(chunk, holeX, holeZ + 1, floorY, ceilingY, lightY); // north
                punchHole(chunk, holeX, holeZ - 1, floorY, ceilingY, lightY); // south
                break;
            case 3: // 6x3x6 room hole (big open space between floors)
                for (int dx = -3; dx <= 2; dx++) {
                    for (int dz = -3; dz <= 2; dz++) {
                        punchHole(chunk, holeX + dx, holeZ + dz, floorY, ceilingY, lightY);
                    }
                }
                break;
        }
    }

    private void punchHole(Chunk chunk, int x, int z, int floorY, int ceilingY, int lightY) {
        // Make sure we're within chunk bounds
        if (x < 0 || x >= 16 || z < 0 || z >= 16) return;

        // Floor hole: remove floor block AND 2 blocks below (into the room below)
        // This creates a 3-block deep hole in the floor
        chunk.setBlockState(new BlockPos(x, floorY, z),
                Blocks.AIR.getDefaultState(), false);
        chunk.setBlockState(new BlockPos(x, floorY - 1, z),
                Blocks.AIR.getDefaultState(), false);
        chunk.setBlockState(new BlockPos(x, floorY - 2, z),
                Blocks.AIR.getDefaultState(), false);

        // Ceiling hole: remove light layer, top ceiling, AND 2 blocks above (into the room above)
        // This creates a 3-block tall hole in the ceiling
        chunk.setBlockState(new BlockPos(x, lightY, z),
                Blocks.AIR.getDefaultState(), false);
        chunk.setBlockState(new BlockPos(x, ceilingY, z),
                Blocks.AIR.getDefaultState(), false);
        chunk.setBlockState(new BlockPos(x, ceilingY + 1, z),
                Blocks.AIR.getDefaultState(), false);
        chunk.setBlockState(new BlockPos(x, ceilingY + 2, z),
                Blocks.AIR.getDefaultState(), false);
    }

    private Direction getAdjacentWallDirection(Chunk chunk, int x, int z, int wallY) {
        // Check all 4 directions for adjacent walls
        if (x > 0 && !chunk.getBlockState(new BlockPos(x - 1, wallY, z)).isAir()) {
            return Direction.EAST; // Wall is to the west, so prop faces east
        }
        if (x < 15 && !chunk.getBlockState(new BlockPos(x + 1, wallY, z)).isAir()) {
            return Direction.WEST; // Wall is to the east, so prop faces west
        }
        if (z > 0 && !chunk.getBlockState(new BlockPos(x, wallY, z - 1)).isAir()) {
            return Direction.SOUTH; // Wall is to the north, so prop faces south
        }
        if (z < 15 && !chunk.getBlockState(new BlockPos(x, wallY, z + 1)).isAir()) {
            return Direction.NORTH; // Wall is to the south, so prop faces north
        }
        return null; // No adjacent wall
    }

    private void placeLocker(Chunk chunk, int x, int z, int floorY, Direction facing) {
        BlockState lockerState = ModBlocks.LOCKER.getDefaultState()
                .with(Properties.HORIZONTAL_FACING, facing)
                .with(Properties.OPEN, false);

        // Locker is 2 blocks tall (lower and upper half)
        chunk.setBlockState(new BlockPos(x, floorY, z),
                lockerState.with(Properties.DOUBLE_BLOCK_HALF, net.minecraft.block.enums.DoubleBlockHalf.LOWER),
                false);
        chunk.setBlockState(new BlockPos(x, floorY + 1, z),
                lockerState.with(Properties.DOUBLE_BLOCK_HALF, net.minecraft.block.enums.DoubleBlockHalf.UPPER),
                false);
    }

    private void placeWaterCooler(Chunk chunk, int x, int z, int floorY, Direction facing) {
        // Water cooler faces a random direction if not against a wall
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

    private void placeChest(Chunk chunk, int x, int z, int floorY, Direction facing) {
        BlockState chestState = Blocks.CHEST.getDefaultState()
                .with(ChestBlock.FACING, facing)
                .with(Properties.WATERLOGGED, false);
        chunk.setBlockState(new BlockPos(x, floorY, z), chestState, false);
    }

    // Inner class for prop positions
    private static class PropPosition {
        final int x;
        final int z;
        final int worldX;
        final int worldZ;
        final Direction wallDirection; // null if not against a wall

        PropPosition(int x, int z, int worldX, int worldZ, Direction wallDirection) {
            this.x = x;
            this.z = z;
            this.worldX = worldX;
            this.worldZ = worldZ;
            this.wallDirection = wallDirection;
        }
    }

    private void generateBoundaryWalls(Chunk chunk, int chunkStartX, int chunkStartZ,
                                       int floorStartY, int wallBottomY, int wallTopY) {

        long chunkSeed = ((long) chunkStartX * 341873128712L +
                (long) chunkStartZ * 132897987541L +
                floorStartY * 456789L) ^ worldSeed;
        ChunkRandom chunkRandom = new ChunkRandom(new CheckedRandom(chunkSeed));

        float wallRoll = chunkRandom.nextFloat();
        int numBoundaryWalls;

        if (wallRoll < 0.4f) {
            numBoundaryWalls = 0;
        } else if (wallRoll < 0.7f) {
            numBoundaryWalls = 1;
        } else if (wallRoll < 0.9f) {
            numBoundaryWalls = 2;
        } else {
            numBoundaryWalls = 3;
        }

        if (numBoundaryWalls == 0) return;

        List<Integer> boundaries = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
        Collections.shuffle(boundaries, new Random(chunkRandom.nextLong()));

        for (int i = 0; i < numBoundaryWalls; i++) {
            int boundary = boundaries.get(i);
            boolean hasOpening = chunkRandom.nextFloat() < 0.7f;
            int openingPos = hasOpening ? chunkRandom.nextInt(14) + 1 : -1;

            switch (boundary) {
                case 0:
                    for (int x = 0; x < 16; x++) {
                        if (x != openingPos && x != openingPos + 1) {
                            placeWallBlock(chunk, x, 0, wallBottomY, wallTopY, chunkStartX, chunkStartZ, floorStartY);
                        }
                    }
                    break;
                case 1:
                    for (int x = 0; x < 16; x++) {
                        if (x != openingPos && x != openingPos + 1) {
                            placeWallBlock(chunk, x, 15, wallBottomY, wallTopY, chunkStartX, chunkStartZ, floorStartY);
                        }
                    }
                    break;
                case 2:
                    for (int z = 0; z < 16; z++) {
                        if (z != openingPos && z != openingPos + 1) {
                            placeWallBlock(chunk, 0, z, wallBottomY, wallTopY, chunkStartX, chunkStartZ, floorStartY);
                        }
                    }
                    break;
                case 3:
                    for (int z = 0; z < 16; z++) {
                        if (z != openingPos && z != openingPos + 1) {
                            placeWallBlock(chunk, 15, z, wallBottomY, wallTopY, chunkStartX, chunkStartZ, floorStartY);
                        }
                    }
                    break;
            }
        }
    }

    private void generateInternalWalls(Chunk chunk, int chunkStartX, int chunkStartZ,
                                       int floorStartY, int wallBottomY, int wallTopY) {

        long chunkSeed = ((long) chunkStartX * 341873128712L +
                (long) chunkStartZ * 132897987541L +
                floorStartY * 987654L) ^ worldSeed;
        ChunkRandom chunkRandom = new ChunkRandom(new CheckedRandom(chunkSeed));

        if (chunkRandom.nextFloat() >= 0.4f) return;

        int wallCount = chunkRandom.nextInt(2) + 1;

        for (int i = 0; i < wallCount; i++) {
            generateRandomWallShape(chunk, chunkStartX, chunkStartZ, wallBottomY,
                    wallTopY, chunkRandom, floorStartY);
        }
    }

    private void generateRandomWallShape(Chunk chunk, int chunkStartX, int chunkStartZ,
                                         int wallBottomY, int wallTopY, ChunkRandom random, int floorStartY) {

        int wallType = random.nextInt(6);
        int startX = random.nextInt(14) + 1;
        int startZ = random.nextInt(14) + 1;
        int thickness = random.nextFloat() < 0.3f ? 2 : 1;
        int length = random.nextInt(8) + 3;

        switch (wallType) {
            case 0:
                for (int dx = 0; dx < length && (startX + dx) < 15; dx++) {
                    placeWallBlock(chunk, startX + dx, startZ, wallBottomY, wallTopY,
                            chunkStartX, chunkStartZ, floorStartY);
                    if (thickness == 2 && startZ + 1 < 15) {
                        placeWallBlock(chunk, startX + dx, startZ + 1, wallBottomY, wallTopY,
                                chunkStartX, chunkStartZ, floorStartY);
                    }
                }
                break;

            case 1:
                for (int dz = 0; dz < length && (startZ + dz) < 15; dz++) {
                    placeWallBlock(chunk, startX, startZ + dz, wallBottomY, wallTopY,
                            chunkStartX, chunkStartZ, floorStartY);
                    if (thickness == 2 && startX + 1 < 15) {
                        placeWallBlock(chunk, startX + 1, startZ + dz, wallBottomY, wallTopY,
                                chunkStartX, chunkStartZ, floorStartY);
                    }
                }
                break;

            case 2:
                int legLength = length / 2;
                for (int dx = 0; dx < legLength && (startX + dx) < 15; dx++) {
                    placeWallBlock(chunk, startX + dx, startZ, wallBottomY, wallTopY,
                            chunkStartX, chunkStartZ, floorStartY);
                }
                for (int dz = 0; dz < legLength && (startZ + dz) < 15; dz++) {
                    placeWallBlock(chunk, startX, startZ + dz, wallBottomY, wallTopY,
                            chunkStartX, chunkStartZ, floorStartY);
                }
                break;

            case 3:
                int armLength = length / 4;
                for (int dx = -armLength; dx <= armLength; dx++) {
                    int px = startX + dx;
                    if (px > 0 && px < 15) {
                        placeWallBlock(chunk, px, startZ, wallBottomY, wallTopY,
                                chunkStartX, chunkStartZ, floorStartY);
                    }
                }
                for (int dz = -armLength; dz <= armLength; dz++) {
                    int pz = startZ + dz;
                    if (pz > 0 && pz < 15) {
                        placeWallBlock(chunk, startX, pz, wallBottomY, wallTopY,
                                chunkStartX, chunkStartZ, floorStartY);
                    }
                }
                break;

            case 4:
                int topLen = length / 3;
                for (int dx = -topLen; dx <= topLen; dx++) {
                    int px = startX + dx;
                    if (px > 0 && px < 15) {
                        placeWallBlock(chunk, px, startZ, wallBottomY, wallTopY,
                                chunkStartX, chunkStartZ, floorStartY);
                    }
                }
                for (int dz = 0; dz < length - topLen && (startZ + dz) < 15; dz++) {
                    placeWallBlock(chunk, startX, startZ + dz, wallBottomY, wallTopY,
                            chunkStartX, chunkStartZ, floorStartY);
                }
                break;

            case 5:
                int bracketLen = length / 2;
                for (int dx = 0; dx < bracketLen && (startX + dx) < 15; dx++) {
                    placeWallBlock(chunk, startX + dx, startZ, wallBottomY, wallTopY,
                            chunkStartX, chunkStartZ, floorStartY);
                    if (startZ + bracketLen < 15) {
                        placeWallBlock(chunk, startX + dx, startZ + bracketLen, wallBottomY, wallTopY,
                                chunkStartX, chunkStartZ, floorStartY);
                    }
                }
                for (int dz = 0; dz < bracketLen && (startZ + dz) < 15; dz++) {
                    placeWallBlock(chunk, startX, startZ + dz, wallBottomY, wallTopY,
                            chunkStartX, chunkStartZ, floorStartY);
                }
                break;
        }
    }

    private void generateColumns(Chunk chunk, int chunkStartX, int chunkStartZ,
                                 int floorStartY, int wallBottomY, int wallTopY) {

        long chunkSeed = ((long) chunkStartX * 341873128712L +
                (long) chunkStartZ * 132897987541L +
                floorStartY * 123456L) ^ worldSeed;
        ChunkRandom chunkRandom = new ChunkRandom(new CheckedRandom(chunkSeed));

        if (chunkRandom.nextFloat() >= 0.3f) return;

        int columnCount = chunkRandom.nextInt(3) + 1;

        for (int i = 0; i < columnCount; i++) {
            int centerX = chunkRandom.nextInt(12) + 2;
            int centerZ = chunkRandom.nextInt(12) + 2;

            float sizeRoll = chunkRandom.nextFloat();
            int size;
            if (sizeRoll < 0.4f) size = 1;
            else if (sizeRoll < 0.7f) size = 2;
            else if (sizeRoll < 0.9f) size = 3;
            else size = 4;

            for (int dx = -size/2; dx <= size/2; dx++) {
                for (int dz = -size/2; dz <= size/2; dz++) {
                    int px = centerX + dx;
                    int pz = centerZ + dz;
                    if (px >= 0 && px < 16 && pz >= 0 && pz < 16) {
                        for (int y = wallBottomY; y <= wallTopY; y++) {
                            BlockState wallBlock = getWallBlockForY(chunkStartX + px, chunkStartZ + pz,
                                    floorStartY, y, wallBottomY);
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
            BlockState wallBlock = getWallBlockForY(chunkStartX + x, chunkStartZ + z,
                    floorStartY, y, wallBottomY);
            chunk.setBlockState(new BlockPos(x, y, z), wallBlock, false);
        }
    }

    /**
     * Gets the appropriate block for a wall/column at a specific Y level.
     * Bottom block (y == wallBottomY) is backboard base, rest is wallpaper/sponge.
     */
    private BlockState getWallBlockForY(int worldX, int worldZ, int floorStartY, int y, int wallBottomY) {
        // Bottom block of wall/column is backboard base
        if (y == wallBottomY) {
            return ModBlocks.BACKBOARD_BLOCK.getDefaultState();
        }

        // All other blocks are wallpaper with sponge chance
        return getOrganicWallBlock(worldX, worldZ, floorStartY);
    }

    private BlockState getLightPatternBlock(int localX, int localZ, int worldX, int worldZ) {
        if (localZ == 7 || localZ == 8) {
            if (localX == 3 || localX == 11) {
                return getLightBlockWithCorruption(worldX, worldZ);
            }
        }
        return ModBlocks.BACKBOARD_BLOCK.getDefaultState();
    }

    private BlockState getLightBlockWithCorruption(int worldX, int worldZ) {
        long lightSeed = ((long) worldX * 341873128712L + (long) worldZ * 132897987541L) ^ worldSeed;
        ChunkRandom lightRandom = new ChunkRandom(new CheckedRandom(lightSeed));

        double darkZoneNoise = getDarkZoneNoise(worldX, worldZ);

        if (darkZoneNoise > 0.6) {
            return lightRandom.nextFloat() < 0.9f ?
                    Blocks.BONE_BLOCK.getDefaultState() :
                    Blocks.OCHRE_FROGLIGHT.getDefaultState();
        } else {
            return lightRandom.nextFloat() < 0.1f ?
                    Blocks.BONE_BLOCK.getDefaultState() :
                    Blocks.OCHRE_FROGLIGHT.getDefaultState();
        }
    }

    private double getDarkZoneNoise(int worldX, int worldZ) {
        double scale = 0.005;
        double worldSeedOffset = worldSeed * 0.0001;
        double value = Math.sin((worldX + worldSeedOffset) * scale) *
                Math.cos((worldZ - worldSeedOffset) * scale * 1.3) +
                Math.sin((worldX + worldSeedOffset) * scale * 0.7 + 1.5) *
                        Math.cos((worldZ - worldSeedOffset) * scale * 0.9);
        return (value + 2.0) / 4.0;
    }

    private BlockState getOrganicWallBlock(int worldX, int worldZ, int floorY) {
        // Use deterministic random for sponge chance
        long spongeSeed = ((long) worldX * 341873128712L + (long) worldZ * 132897987541L + floorY * 999L) ^ worldSeed;
        ChunkRandom spongeRandom = new ChunkRandom(new CheckedRandom(spongeSeed));

        // 10% chance to be sponge wallpaper instead of regular/mold wallpaper
        if (spongeRandom.nextFloat() < 0.1f) {
            return ModBlocks.SPONGE_WALLPAPER_BLOCK.getDefaultState();
        }

        // Otherwise use mold-based wallpaper
        MoldCluster[] clusters = getMoldClustersForRegion(worldX, worldZ);

        double strongestMold = 0;

        for (MoldCluster cluster : clusters) {
            double dx = worldX - cluster.centerX;
            double dz = worldZ - cluster.centerZ;
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

        if (strongestMold > 0.85) {
            return ModBlocks.MOLD_INFECTED_WALLPAPER_BLOCK.getDefaultState();
        } else if (strongestMold > 0.7) {
            return ModBlocks.MOLDY_WALLPAPER_BLOCK.getDefaultState();
        } else if (strongestMold > 0.5) {
            return ModBlocks.WET_WALLPAPER_BLOCK.getDefaultState();
        } else if (strongestMold > 0.3) {
            return ModBlocks.STAINED_WALLPAPER_BLOCK.getDefaultState();
        } else {
            return ModBlocks.WALLPAPER_BLOCK.getDefaultState();
        }
    }

    private MoldCluster[] getMoldClustersForRegion(int worldX, int worldZ) {
        int regionX = worldX / 64;
        int regionZ = worldZ / 64;
        long regionKey = ((long) regionX << 32) | (regionZ & 0xFFFFFFFFL);

        if (moldCache.containsKey(regionKey)) {
            return moldCache.get(regionKey);
        }

        long regionSeed = ((long) regionX * 341873128712L +
                (long) regionZ * 132897987541L) ^ worldSeed;
        ChunkRandom regionRandom = new ChunkRandom(new CheckedRandom(regionSeed));

        if (regionRandom.nextFloat() > 0.15f) {
            MoldCluster[] empty = new MoldCluster[0];
            moldCache.put(regionKey, empty);
            return empty;
        }

        int numClusters = regionRandom.nextInt(2) + 1;
        MoldCluster[] clusters = new MoldCluster[numClusters];

        for (int i = 0; i < numClusters; i++) {
            int centerX = regionX * 64 + regionRandom.nextInt(64);
            int centerZ = regionZ * 64 + regionRandom.nextInt(64);
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
        final int centerX;
        final int centerZ;
        final int radius;
        final long seed;

        MoldCluster(int centerX, int centerZ, int radius, long seed) {
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.radius = radius;
            this.seed = seed;
        }
    }
}
