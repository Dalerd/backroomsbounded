package net.dalerd.backroomsbounded.world.gen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.dalerd.backroomsbounded.block.ModBlocks;
import net.dalerd.backroomsbounded.config.BackroomsConfig;
import net.minecraft.block.*;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.StairShape;
import net.minecraft.state.property.Properties;
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
    private static final int CHUNK_SIZE = 16;

    // Ring system: Backrooms 0-3000, Office 3000-6000, repeat every 6000 blocks
    private static final int RING_SIZE = 12000; // Total size of one backrooms+office cycle
    private static final int BACKROOMS_WIDTH = 6000; // Backrooms zone width
    private static final int OFFICE_WIDTH = 6000; // Office zone width

    private long worldSeed = 0;
    private final BackroomsConfig CONFIG = BackroomsConfig.getInstance();

    private Map<Long, MoldCluster[]> moldCache = new HashMap<>();
    private static final Map<Long, Set<GridPos>> generatedBacteriaGrids = new HashMap<>();

    public BackroomsChunkGenerator(BiomeSource biomeSource) { super(biomeSource); }

    @Override protected MapCodec<? extends ChunkGenerator> getCodec() { return CODEC; }
    @Override public void carve(ChunkRegion r, long s, NoiseConfig n, BiomeAccess b, StructureAccessor sa, Chunk c, GenerationStep.Carver carver) { this.worldSeed = s; }
    @Override public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {}
    @Override public void populateEntities(ChunkRegion region) {}
    @Override public int getWorldHeight() { return 256; }
    @Override public CompletableFuture<Chunk> populateNoise(Blender blender, NoiseConfig noiseConfig, StructureAccessor sa, Chunk chunk) {
        generateChunk(chunk);
        return CompletableFuture.completedFuture(chunk);
    }
    @Override public int getSeaLevel() { return 0; }
    @Override public int getMinimumY() { return 0; }
    @Override public int getHeight(int x, int z, Heightmap.Type h, HeightLimitView w, NoiseConfig n) { return 256; }
    @Override public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView w, NoiseConfig n) {
        return new VerticalBlockSample(0, new BlockState[0]);
    }
    @Override public void getDebugHudText(List<String> t, NoiseConfig n, BlockPos p) {}

    // =========================================
    // RING-BASED OFFICE DETECTION
    // =========================================

    /**
     * Simple ring pattern:
     * - Distance 0 to 3000: Backrooms
     * - Distance 3000 to 6000: Office
     * - Distance 6000 to 9000: Backrooms
     * - Distance 9000 to 12000: Office
     * - Repeats every 6000 blocks
     *
     * Ring boundaries are randomized ±500 blocks per world seed
     * to ensure unique generation in different worlds.
     */
    private boolean isOfficeChunk(int chunkX, int chunkZ) {
        // Convert chunk coordinates to block coordinates (center of chunk)
        int blockX = chunkX * CHUNK_SIZE + CHUNK_SIZE / 2;
        int blockZ = chunkZ * CHUNK_SIZE + CHUNK_SIZE / 2;

        // Calculate distance from origin (0,0)
        double distance = Math.sqrt(blockX * blockX + blockZ * blockZ);

        // Get randomized offset for this world
        long offsetSeed = worldSeed;
        ChunkRandom offsetRandom = new ChunkRandom(new CheckedRandom(offsetSeed));
        double worldOffset = offsetRandom.nextFloat() * 1000 - 500; // -500 to +500 blocks

        // Apply offset to distance
        double adjustedDistance = distance + worldOffset;

        // Calculate position within the 6000-block cycle
        double positionInCycle = adjustedDistance % RING_SIZE;
        if (positionInCycle < 0) positionInCycle += RING_SIZE;

        // 0 to BACKROOMS_WIDTH = Backrooms
        // BACKROOMS_WIDTH to RING_SIZE = Office
        return positionInCycle >= BACKROOMS_WIDTH;
    }

    // =========================================
    // CHUNK GENERATION
    // =========================================
    private void generateChunk(Chunk chunk) {
        ChunkPos cp = chunk.getPos();
        int csx = cp.getStartX(), csz = cp.getStartZ();

        boolean isOffice = isOfficeChunk(csx, csz);

        // Generate ALL floors as either office or backrooms
        for (int fsy = 0; fsy < 256; fsy += FLOOR_HEIGHT) {
            if (isOffice) {
                generateOfficeFloor(chunk, csx, csz, fsy);
            } else {
                generateFloor(chunk, csx, csz, fsy);
            }
        }
    }

    // =========================================
    // STANDARD BACKROOMS FLOOR
    // =========================================
    private void generateFloor(Chunk c, int csx, int csz, int fsy) {
        int fy = fsy, cey = fsy + FLOOR_HEIGHT - 1, lty = cey - 1, wby = fy + 1, wty = cey - 2;

        // Standard backrooms floor and ceiling
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                c.setBlockState(new BlockPos(x, fy, z), Blocks.YELLOW_TERRACOTTA.getDefaultState(), false);
                c.setBlockState(new BlockPos(x, cey, z), ModBlocks.BACKBOARD_BLOCK.getDefaultState(), false);
                c.setBlockState(new BlockPos(x, lty, z), getStandardLightBlock(x, z, csx+x, csz+z), false);
                for (int y = wby; y <= wty; y++) {
                    c.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState(), false);
                }
            }
        }

        // Standard backrooms generation
        generateBoundaryWalls(c, csx, csz, fsy, wby, wty, false);
        generateInternalWalls(c, csx, csz, fsy, wby, wty, false);
        generateColumns(c, csx, csz, fsy, wby, wty, false);
        generateProps(c, csx, csz, fsy, fy, wby, wty, false);
        generateMossPatch(c, csx, csz, fsy, fy, wby);
        generateHoles(c, csx, csz, fsy, fy, cey, lty);
        generateBacteriaEcosystem(c, csx, csz, fsy, fy, wby, wty);
    }

    // =========================================
    // OFFICE FLOOR
    // =========================================
    private void generateOfficeFloor(Chunk c, int csx, int csz, int fsy) {
        int fy = fsy, cey = fsy + FLOOR_HEIGHT - 1, lty = cey - 1, wby = fy + 1, wty = cey - 2;

        long seed = ((long) csx * 777888L + (long) csz * 333444L + fsy * 111L) ^ worldSeed;
        ChunkRandom r = new ChunkRandom(new CheckedRandom(seed));

        // OFFICE FLOOR AND CEILING
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                // Polished tuff floor with tuff brick grid pattern
                BlockState floorBlock = ((csx + x + csz + z) % 3 == 0) ?
                        Blocks.TUFF_BRICKS.getDefaultState() : Blocks.POLISHED_TUFF.getDefaultState();
                c.setBlockState(new BlockPos(x, fy, z), floorBlock, false);

                // Smooth quartz ceiling
                c.setBlockState(new BlockPos(x, cey, z), Blocks.SMOOTH_QUARTZ.getDefaultState(), false);

                // Office lighting with same positioning as backrooms
                c.setBlockState(new BlockPos(x, lty, z), getOfficeLightBlock(x, z, csx+x, csz+z), false);

                // Clear space
                for (int y = wby; y <= wty; y++) {
                    c.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState(), false);
                }
            }
        }

        // Generate office walls using SAME algorithms as backrooms but office materials
        generateInternalWalls(c, csx, csz, fsy, wby, wty, true);

        // Office-specific features
        generateOfficeCubicles(c, csx, csz, fsy, fy, wby, r);
        generateOfficeDecorations(c, csx, csz, fsy, fy, wby, wty, r);

        // NO: boundary walls, columns, moss, holes, bacteria - office is clean
    }

    // =========================================
    // LIGHTING
    // =========================================
    private BlockState getStandardLightBlock(int lx, int lz, int wx, int wz) {
        if ((lz == 7 || lz == 8) && (lx == 3 || lx == 11)) return getLightWithCorruption(wx, wz, false);
        return ModBlocks.BACKBOARD_BLOCK.getDefaultState();
    }

    private BlockState getOfficeLightBlock(int lx, int lz, int wx, int wz) {
        if ((lz == 7 || lz == 8) && (lx == 3 || lx == 11)) {
            return getLightWithCorruption(wx, wz, true);
        }
        return Blocks.SMOOTH_QUARTZ.getDefaultState();
    }

    private BlockState getLightWithCorruption(int wx, int wz, boolean office) {
        long ls = ((long) wx * 341873128712L + (long) wz * 132897987541L) ^ worldSeed;
        ChunkRandom lr = new ChunkRandom(new CheckedRandom(ls));
        double dn = getDarkZoneNoise(wx, wz);
        Block froglight = office ? Blocks.PEARLESCENT_FROGLIGHT : Blocks.OCHRE_FROGLIGHT;
        if (dn > 0.6) return lr.nextFloat() < 0.9f ?
                Blocks.BONE_BLOCK.getDefaultState().with(Properties.AXIS, Direction.Axis.Y) : froglight.getDefaultState();
        else return lr.nextFloat() < 0.1f ?
                Blocks.BONE_BLOCK.getDefaultState().with(Properties.AXIS, Direction.Axis.Y) : froglight.getDefaultState();
    }

    // =========================================
    // WALL GENERATION
    // =========================================
    private void generateBoundaryWalls(Chunk c, int csx, int csz, int fsy, int wby, int wty, boolean office) {
        if (office) return;

        long cseed = ((long) csx * 341873128712L + (long) csz * 132897987541L + fsy * 456789L) ^ worldSeed;
        ChunkRandom cr = new ChunkRandom(new CheckedRandom(cseed));
        float wr = cr.nextFloat();
        int nbw = 0;

        if (wr < 0.4f) nbw = 0;
        else if (wr < 0.7f) nbw = 1;
        else if (wr < 0.9f) nbw = 2;
        else nbw = 3;

        if (nbw == 0) return;

        List<Integer> bd = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
        Collections.shuffle(bd, new Random(cr.nextLong()));

        for (int i = 0; i < nbw; i++) {
            int b = bd.get(i);
            boolean ho = cr.nextFloat() < 0.7f;
            int op = ho ? cr.nextInt(14) + 1 : -1;

            switch (b) {
                case 0: for (int x = 0; x < 16; x++) if (x != op && x != op + 1) placeWallBlock(c, x, 0, wby, wty, csx, csz, fsy, office); break;
                case 1: for (int x = 0; x < 16; x++) if (x != op && x != op + 1) placeWallBlock(c, x, 15, wby, wty, csx, csz, fsy, office); break;
                case 2: for (int z = 0; z < 16; z++) if (z != op && z != op + 1) placeWallBlock(c, 0, z, wby, wty, csx, csz, fsy, office); break;
                case 3: for (int z = 0; z < 16; z++) if (z != op && z != op + 1) placeWallBlock(c, 15, z, wby, wty, csx, csz, fsy, office); break;
            }
        }
    }

    private void generateInternalWalls(Chunk c, int csx, int csz, int fsy, int wby, int wty, boolean office) {
        long cseed = ((long) csx * 341873128712L + (long) csz * 132897987541L + fsy * 987654L) ^ worldSeed;
        ChunkRandom cr = new ChunkRandom(new CheckedRandom(cseed));
        float wc = office ? CONFIG.generationInternalWallChance * 2.0f : CONFIG.generationInternalWallChance;
        if (cr.nextFloat() >= wc) return;

        int wcnt = cr.nextInt(4) + 2;
        for (int i = 0; i < wcnt; i++) {
            generateRandomWallShape(c, csx, csz, wby, wty, cr, fsy, office);
        }
    }

    private void generateRandomWallShape(Chunk c, int csx, int csz, int wby, int wty, ChunkRandom r, int fsy, boolean office) {
        int wt = r.nextInt(6), sx = r.nextInt(14) + 1, sz = r.nextInt(14) + 1;
        int th = r.nextFloat() < 0.3f ? 2 : 1;
        int len = r.nextInt(8) + 3;

        switch (wt) {
            case 0: for (int dx = 0; dx < len && (sx + dx) < 15; dx++) {
                placeWallBlock(c, sx + dx, sz, wby, wty, csx, csz, fsy, office);
                if (th == 2 && sz + 1 < 15) placeWallBlock(c, sx + dx, sz + 1, wby, wty, csx, csz, fsy, office);
            } break;
            case 1: for (int dz = 0; dz < len && (sz + dz) < 15; dz++) {
                placeWallBlock(c, sx, sz + dz, wby, wty, csx, csz, fsy, office);
                if (th == 2 && sx + 1 < 15) placeWallBlock(c, sx + 1, sz + dz, wby, wty, csx, csz, fsy, office);
            } break;
            case 2: int leg = len / 2;
                for (int dx = 0; dx < leg && (sx + dx) < 15; dx++) placeWallBlock(c, sx + dx, sz, wby, wty, csx, csz, fsy, office);
                for (int dz = 0; dz < leg && (sz + dz) < 15; dz++) placeWallBlock(c, sx, sz + dz, wby, wty, csx, csz, fsy, office);
                break;
            case 3: int arm = len / 4;
                for (int dx = -arm; dx <= arm; dx++) { int px = sx + dx; if (px > 0 && px < 15) placeWallBlock(c, px, sz, wby, wty, csx, csz, fsy, office); }
                for (int dz = -arm; dz <= arm; dz++) { int pz = sz + dz; if (pz > 0 && pz < 15) placeWallBlock(c, sx, pz, wby, wty, csx, csz, fsy, office); }
                break;
            case 4: int top = len / 3;
                for (int dx = -top; dx <= top; dx++) { int px = sx + dx; if (px > 0 && px < 15) placeWallBlock(c, px, sz, wby, wty, csx, csz, fsy, office); }
                for (int dz = 0; dz < len - top && (sz + dz) < 15; dz++) placeWallBlock(c, sx, sz + dz, wby, wty, csx, csz, fsy, office);
                break;
            case 5: int blen = len / 2;
                for (int dx = 0; dx < blen && (sx + dx) < 15; dx++) {
                    placeWallBlock(c, sx + dx, sz, wby, wty, csx, csz, fsy, office);
                    if (sz + blen < 15) placeWallBlock(c, sx + dx, sz + blen, wby, wty, csx, csz, fsy, office);
                }
                for (int dz = 0; dz < blen && (sz + dz) < 15; dz++) placeWallBlock(c, sx, sz + dz, wby, wty, csx, csz, fsy, office);
                break;
        }
    }

    private void placeWallBlock(Chunk c, int x, int z, int wby, int wty, int csx, int csz, int fsy, boolean office) {
        for (int y = wby; y <= wty; y++) {
            if (x >= 0 && x < 16 && z >= 0 && z < 16) {
                c.setBlockState(new BlockPos(x, y, z), getWallBlockForY(csx + x, csz + z, fsy, y, wby, office), false);
            }
        }
    }

    private BlockState getWallBlockForY(int wx, int wz, int fsy, int y, int wby, boolean office) {
        if (y == wby) {
            return office ? Blocks.SMOOTH_STONE.getDefaultState() : ModBlocks.BACKBOARD_BLOCK.getDefaultState();
        }
        return office ? Blocks.WHITE_CONCRETE.getDefaultState() : getOrganicWallBlock(wx, wz, fsy);
    }

    // =========================================
    // OFFICE CUBICLES
    // =========================================
    private void generateOfficeCubicles(Chunk c, int csx, int csz, int fsy, int fy, int wby, ChunkRandom r) {
        if (r.nextFloat() >= 0.70f) return;

        int cellSize = 5;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int globalX = csx + x;
                int globalZ = csz + z;

                if (globalX % cellSize != 0 || globalZ % cellSize != 0) continue;
                if (x + 3 >= 16 || z + 3 >= 16) continue;

                boolean blocked = false;
                for (int dx = 0; dx < 3; dx++) {
                    for (int dz = 0; dz < 3; dz++) {
                        if (c.getBlockState(new BlockPos(x + dx, wby, z + dz)).isSolid()) {
                            blocked = true;
                            break;
                        }
                    }
                }
                if (blocked) continue;

                long cubicleSeed = ((long) globalX * 341873128712L + (long) globalZ * 132897987541L) ^ worldSeed;
                ChunkRandom cr = new ChunkRandom(new CheckedRandom(cubicleSeed));
                Direction facing = Direction.fromHorizontal(cr.nextInt(4));

                buildSolidCubicle(c, x, z, wby, facing, r);
            }
        }
    }

    private void buildSolidCubicle(Chunk c, int bx, int bz, int wby, Direction facing, ChunkRandom r) {
        Direction right = facing.rotateYClockwise();

        for (int col = 0; col < 3; col++) {
            int px = bx + right.getOffsetX() * col, pz = bz + right.getOffsetZ() * col;
            for (int y = wby; y <= wby + 1; y++) {
                BlockPos bp = new BlockPos(px, y, pz);
                if (bp.getX() < 16 && bp.getZ() < 16) {
                    c.setBlockState(bp, Blocks.DIORITE.getDefaultState(), false);
                }
            }
        }

        int rowX = bx + facing.getOffsetX(), rowZ = bz + facing.getOffsetZ();
        for (int y = wby; y <= wby + 1; y++) {
            BlockPos left = new BlockPos(rowX, y, rowZ);
            if (left.getX() < 16 && left.getZ() < 16) {
                c.setBlockState(left, Blocks.DIORITE.getDefaultState(), false);
            }
        }

        int rightX = rowX + right.getOffsetX() * 2, rightZ = rowZ + right.getOffsetZ() * 2;
        for (int y = wby; y <= wby + 1; y++) {
            BlockPos rw = new BlockPos(rightX, y, rightZ);
            if (rw.getX() < 16 && rw.getZ() < 16) {
                c.setBlockState(rw, Blocks.DIORITE.getDefaultState(), false);
            }
        }

        int deskX = rowX + right.getOffsetX(), deskZ = rowZ + right.getOffsetZ();
        BlockPos desk = new BlockPos(deskX, wby, deskZ);
        if (desk.getX() < 16 && desk.getZ() < 16 && c.getBlockState(desk).isAir()) {
            c.setBlockState(desk, Blocks.OAK_STAIRS.getDefaultState()
                    .with(Properties.HORIZONTAL_FACING, facing.getOpposite())
                    .with(Properties.BLOCK_HALF, BlockHalf.TOP)
                    .with(Properties.STAIR_SHAPE, StairShape.STRAIGHT), false);
        }

        int frontX = bx + facing.getOffsetX() * 2, frontZ = bz + facing.getOffsetZ() * 2;
        for (int y = wby; y <= wby + 1; y++) {
            BlockPos left = new BlockPos(frontX, y, frontZ);
            if (left.getX() < 16 && left.getZ() < 16) {
                c.setBlockState(left, Blocks.DIORITE.getDefaultState(), false);
            }
        }

        int frX = frontX + right.getOffsetX() * 2, frZ = frontZ + right.getOffsetZ() * 2;
        for (int y = wby; y <= wby + 1; y++) {
            BlockPos rw = new BlockPos(frX, y, frZ);
            if (rw.getX() < 16 && rw.getZ() < 16) {
                c.setBlockState(rw, Blocks.DIORITE.getDefaultState(), false);
            }
        }
    }

    // =========================================
    // OFFICE DECORATIONS
    // =========================================
    private void generateOfficeDecorations(Chunk c, int csx, int csz, int fsy, int fy, int wby, int wty, ChunkRandom r) {
        List<BlockPos> wallAdjacent = new ArrayList<>();
        List<BlockPos> openFloor = new ArrayList<>();

        for (int x = 1; x < 15; x++) {
            for (int z = 1; z < 15; z++) {
                if (c.getBlockState(new BlockPos(x, fy, z)).isAir()) continue;
                if (!c.getBlockState(new BlockPos(x, wby, z)).isAir()) continue;

                Direction wd = getAdjacentWallDirection(c, x, z, wby);
                if (wd != null) {
                    wallAdjacent.add(new BlockPos(x, wby, z));
                } else {
                    openFloor.add(new BlockPos(x, wby, z));
                }
            }
        }

        Collections.shuffle(wallAdjacent, new Random(r.nextLong()));
        Collections.shuffle(openFloor, new Random(r.nextLong()));

        if (!wallAdjacent.isEmpty() && r.nextFloat() < 0.30f) {
            BlockPos p = wallAdjacent.get(0);
            Direction wd = getAdjacentWallDirection(c, p.getX(), p.getZ(), wby);
            Direction cf = wd != null ? wd : Direction.fromHorizontal(r.nextInt(4));
            c.setBlockState(p, ModBlocks.WATER_COOLER.getDefaultState().with(Properties.HORIZONTAL_FACING, cf), false);
        }

        if (!wallAdjacent.isEmpty() && r.nextFloat() < 0.50f) {
            int shelfCount = Math.min(r.nextInt(5) + 2, wallAdjacent.size());
            for (int i = 0; i < shelfCount && i < wallAdjacent.size(); i++) {
                BlockPos p = wallAdjacent.get(i);
                Direction wd = getAdjacentWallDirection(c, p.getX(), p.getZ(), wby);
                if (wd != null && c.getBlockState(p.up()).isAir()) {
                    BlockState shelf = r.nextFloat() < 0.4f ?
                            Blocks.CHISELED_BOOKSHELF.getDefaultState().with(Properties.HORIZONTAL_FACING, wd.getOpposite()) :
                            Blocks.BOOKSHELF.getDefaultState();
                    c.setBlockState(p, shelf, false);
                    if (r.nextFloat() < 0.4f && c.getBlockState(p.up(2)).isAir()) {
                        c.setBlockState(p.up(), shelf, false);
                    }
                }
            }
        }

        if (!openFloor.isEmpty() && r.nextFloat() < 0.15f) {
            BlockPos p = openFloor.get(0);
            c.setBlockState(p, Blocks.LECTERN.getDefaultState()
                    .with(Properties.HORIZONTAL_FACING, Direction.fromHorizontal(r.nextInt(4))), false);
        }

        if (!openFloor.isEmpty() && r.nextFloat() < 0.25f) {
            int chairCount = Math.min(r.nextInt(3) + 1, openFloor.size());
            for (int i = 0; i < chairCount && i < openFloor.size(); i++) {
                BlockPos p = openFloor.get(i);
                if (c.getBlockState(p).isAir()) {
                    c.setBlockState(p, Blocks.SPRUCE_STAIRS.getDefaultState()
                            .with(Properties.HORIZONTAL_FACING, Direction.fromHorizontal(r.nextInt(4)))
                            .with(Properties.BLOCK_HALF, BlockHalf.BOTTOM), false);
                }
            }
        }

        if (r.nextFloat() < 0.35f) {
            for (BlockPos p : openFloor) {
                int openSides = 0;
                for (Direction d : Direction.Type.HORIZONTAL) {
                    if (!c.getBlockState(new BlockPos(p.getX() + d.getOffsetX(), wby, p.getZ() + d.getOffsetZ())).isSolid()) {
                        openSides++;
                    }
                }
                if (openSides >= 2 && c.getBlockState(p).isAir()) {
                    c.setBlockState(p, r.nextBoolean() ?
                            Blocks.GRAY_CARPET.getDefaultState() : Blocks.LIGHT_GRAY_CARPET.getDefaultState(), false);
                }
            }
        }

        if (!wallAdjacent.isEmpty() && r.nextFloat() < 0.20f) {
            BlockPos p = wallAdjacent.get(0);
            Direction wd = getAdjacentWallDirection(c, p.getX(), p.getZ(), wby);
            if (wd != null && c.getBlockState(p.up()).isAir()) {
                c.setBlockState(p, Blocks.IRON_BLOCK.getDefaultState(), false);
                if (r.nextFloat() < 0.6f && c.getBlockState(p.up()).isAir()) {
                    c.setBlockState(p.up(), Blocks.IRON_BLOCK.getDefaultState(), false);
                }
            }
        }

        if (!wallAdjacent.isEmpty() && r.nextFloat() < 0.20f) {
            int potCount = Math.min(r.nextInt(3) + 1, wallAdjacent.size());
            for (int i = 0; i < potCount && i < wallAdjacent.size(); i++) {
                BlockPos p = wallAdjacent.get(i);
                Direction wd = getAdjacentWallDirection(c, p.getX(), p.getZ(), wby);
                if (wd != null) {
                    c.setBlockState(p, Blocks.DECORATED_POT.getDefaultState()
                            .with(Properties.HORIZONTAL_FACING, wd.getOpposite()), false);
                }
            }
        }
    }

    // =========================================
    // STANDARD BACKROOMS PROPS
    // =========================================
    private void generateProps(Chunk c, int csx, int csz, int fsy, int fy, int wby, int wty, boolean office) {
        if (office) return;

        long ps = ((long) csx * 341873128712L + (long) csz * 132897987541L + fsy * 789012345L) ^ worldSeed;
        ChunkRandom pr = new ChunkRandom(new CheckedRandom(ps));
        List<PropPosition> wps = new ArrayList<>(), ops = new ArrayList<>();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (c.getBlockState(new BlockPos(x, fy, z)).isAir()) continue;
                if (!c.getBlockState(new BlockPos(x, wby, z)).isAir()) continue;
                Direction wd = getAdjacentWallDirection(c, x, z, wby);
                if (wd != null) wps.add(new PropPosition(x, z, wd));
                else ops.add(new PropPosition(x, z, null));
            }
        }

        Collections.shuffle(wps, new Random(pr.nextLong()));
        Collections.shuffle(ops, new Random(pr.nextLong()));

        if (!wps.isEmpty() && pr.nextFloat() < CONFIG.generationLockerChance) {
            int lc = Math.min(pr.nextInt(2) + 1, wps.size());
            for (int i = 0; i < lc && i < wps.size(); i++) {
                PropPosition pp = wps.get(i);
                placeLocker(c, pp.x, pp.z, wby, pp.wallDirection);
            }
        }

        if (pr.nextFloat() < CONFIG.generationWaterCoolerChance) {
            List<PropPosition> cp = new ArrayList<>();
            cp.addAll(ops);
            cp.addAll(wps);
            Collections.shuffle(cp, new Random(pr.nextLong()));
            if (!cp.isEmpty()) {
                PropPosition pp = cp.get(0);
                placeWaterCooler(c, pp.x, pp.z, wby, pp.wallDirection);
            }
        }

        if (pr.nextFloat() < CONFIG.generationBarrelChance) {
            List<PropPosition> bp = new ArrayList<>();
            bp.addAll(ops);
            bp.addAll(wps);
            Collections.shuffle(bp, new Random(pr.nextLong()));
            int bc = Math.min(pr.nextInt(2) + 1, bp.size());
            for (int i = 0; i < bc && i < bp.size(); i++) {
                PropPosition pp = bp.get(i);
                Direction f = pp.wallDirection != null ? pp.wallDirection : Direction.fromHorizontal(pr.nextInt(4));
                placeBarrel(c, pp.x, pp.z, wby, f);
            }
        }
    }

    // =========================================
    // BACKROOMS FEATURES
    // =========================================
    private void generateMossPatch(Chunk c, int csx, int csz, int fsy, int fy, int wby) {
        long ms = ((long) csx * 341873128712L + (long) csz * 132897987541L + fsy * 777777L) ^ worldSeed;
        ChunkRandom mr = new ChunkRandom(new CheckedRandom(ms));
        if (mr.nextFloat() >= CONFIG.generationMossPatchChance) return;

        int cx = mr.nextInt(10) + 3, cz = mr.nextInt(10) + 3, ps = mr.nextInt(3) + 2;

        for (int dx = -ps; dx <= ps; dx++) {
            for (int dz = -ps; dz <= ps; dz++) {
                int x = cx + dx, z = cz + dz;
                if (x < 0 || x >= 16 || z < 0 || z >= 16) continue;
                double dist = Math.sqrt(dx * dx + dz * dz) + (mr.nextFloat() - 0.5f) * 2.0f;
                if (dist > ps) continue;

                BlockPos fp = new BlockPos(x, fy, z), ap = new BlockPos(x, wby, z);
                if (dist < ps * 0.6f && c.getBlockState(fp).isOf(Blocks.YELLOW_TERRACOTTA)) {
                    c.setBlockState(fp, Blocks.MOSS_BLOCK.getDefaultState(), false);
                    float pr = mr.nextFloat();
                    if (pr < 0.30f && c.getBlockState(ap).isAir()) c.setBlockState(ap, Blocks.MOSS_CARPET.getDefaultState(), false);
                    else if (pr < 0.55f) placeLichenOnNearbyWalls(c, x, wby, z, mr);
                    else if (pr < 0.70f && c.getBlockState(ap).isAir()) c.setBlockState(ap, (mr.nextBoolean() ? Blocks.BROWN_MUSHROOM : Blocks.RED_MUSHROOM).getDefaultState(), false);
                    else if (pr < 0.80f && c.getBlockState(ap).isAir()) c.setBlockState(ap, Blocks.AZALEA.getDefaultState(), false);
                } else if (dist < ps * 0.8f && c.getBlockState(fp).isOf(Blocks.YELLOW_TERRACOTTA) && mr.nextFloat() < 0.3f && c.getBlockState(ap).isAir()) {
                    c.setBlockState(ap, Blocks.MOSS_CARPET.getDefaultState(), false);
                }
            }
        }

        if (mr.nextFloat() < 0.08f) {
            int sx = cx + mr.nextInt(3) - 1, sz = cz + mr.nextInt(3) - 1;
            if (sx >= 0 && sx < 16 && sz >= 0 && sz < 16) {
                BlockPos skp = new BlockPos(sx, wby, sz);
                BlockState skb = c.getBlockState(skp);
                if (skb.isAir() || skb.isOf(Blocks.MOSS_CARPET) || skb.isOf(Blocks.AZALEA)) {
                    c.setBlockState(skp, Blocks.SKELETON_SKULL.getDefaultState().with(Properties.ROTATION, mr.nextInt(16)), false);
                }
            }
        }
    }

    private void placeLichenOnNearbyWalls(Chunk c, int x, int y, int z, ChunkRandom r) {
        for (Direction d : Direction.Type.HORIZONTAL) {
            for (int dist = 1; dist <= 2; dist++) {
                BlockPos cp = new BlockPos(x, y, z).offset(d, dist);
                if (cp.getX() < 0 || cp.getX() >= 16 || cp.getZ() < 0 || cp.getZ() >= 16) continue;
                if (c.getBlockState(cp).isSolid() && isWallpaperBlock(c.getBlockState(cp))) {
                    BlockState ls = Blocks.GLOW_LICHEN.getDefaultState();
                    switch (d.getOpposite()) {
                        case NORTH -> ls = ls.with(Properties.NORTH, true);
                        case SOUTH -> ls = ls.with(Properties.SOUTH, true);
                        case WEST -> ls = ls.with(Properties.WEST, true);
                        case EAST -> ls = ls.with(Properties.EAST, true);
                    }
                    c.setBlockState(cp, ls, false);
                    return;
                }
            }
        }
    }

    private void generateHoles(Chunk c, int csx, int csz, int fsy, int fy, int cey, int lty) {
        if (fsy <= 8 || fsy >= 248) return;
        long hs = ((long) csx * 341873128712L + (long) csz * 132897987541L + fsy * 111111111L) ^ worldSeed;
        ChunkRandom hr = new ChunkRandom(new CheckedRandom(hs));
        if (hr.nextFloat() >= CONFIG.generationHolesChance) return;

        int ht = hr.nextInt(4), hx = hr.nextInt(8) + 4, hz = hr.nextInt(8) + 4;
        switch (ht) {
            case 0: punchHole(c, hx, hz, fy, cey, lty); break;
            case 1: for (int dx = 0; dx < 2; dx++) for (int dz = 0; dz < 2; dz++) punchHole(c, hx + dx, hz + dz, fy, cey, lty); break;
            case 2: punchHole(c, hx, hz, fy, cey, lty); punchHole(c, hx + 1, hz, fy, cey, lty); punchHole(c, hx - 1, hz, fy, cey, lty); punchHole(c, hx, hz + 1, fy, cey, lty); punchHole(c, hx, hz - 1, fy, cey, lty); break;
            case 3: for (int dx = -3; dx <= 2; dx++) for (int dz = -3; dz <= 2; dz++) punchHole(c, hx + dx, hz + dz, fy, cey, lty); break;
        }
    }

    private void punchHole(Chunk c, int x, int z, int fy, int cey, int lty) {
        if (x < 0 || x >= 16 || z < 0 || z >= 16) return;
        c.setBlockState(new BlockPos(x, fy, z), Blocks.AIR.getDefaultState(), false);
        c.setBlockState(new BlockPos(x, fy - 1, z), Blocks.AIR.getDefaultState(), false);
        c.setBlockState(new BlockPos(x, fy - 2, z), Blocks.AIR.getDefaultState(), false);
        c.setBlockState(new BlockPos(x, lty, z), Blocks.AIR.getDefaultState(), false);
        c.setBlockState(new BlockPos(x, cey, z), Blocks.AIR.getDefaultState(), false);
        c.setBlockState(new BlockPos(x, cey + 1, z), Blocks.AIR.getDefaultState(), false);
        c.setBlockState(new BlockPos(x, cey + 2, z), Blocks.AIR.getDefaultState(), false);
        c.setBlockState(new BlockPos(x, fy + 8, z), Blocks.AIR.getDefaultState(), false);
    }

    private void generateBacteriaEcosystem(Chunk c, int csx, int csz, int fsy, int fy, int wby, int wty) {
        long bs = ((long) csx * 341873128712L + (long) csz * 132897987541L + fsy * 555555L) ^ worldSeed;
        ChunkRandom br = new ChunkRandom(new CheckedRandom(bs));
        if (br.nextFloat() >= CONFIG.generationBacteriaClusterChance) return;

        long fk = ((long) csx << 32) | (csz & 0xFFFFFFFFL);
        GridPos tg = new GridPos(csx, csz, fsy);
        Set<GridPos> fgs = generatedBacteriaGrids.computeIfAbsent(fk, k -> new HashSet<>());
        for (GridPos e : fgs) {
            if (Math.abs(tg.x - e.x) <= 16 && Math.abs(tg.z - e.z) <= 16 && tg.y == e.y) return;
        }
        fgs.add(tg);

        int cx = br.nextInt(10) + 3, cz = br.nextInt(10) + 3, cs = br.nextInt(3) + 3;

        for (int dx = -cs; dx <= cs; dx++) {
            for (int dz = -cs; dz <= cs; dz++) {
                int x = cx + dx, z = cz + dz;
                if (x < 0 || x >= 16 || z < 0 || z >= 16) continue;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > cs) continue;
                float vc = (dist > cs * 0.5f) ? 0.6f : 0.4f;
                if (br.nextFloat() < vc) placeVinesOnNearbyWalls(c, x, z, wby, wty, br);
            }
        }

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int x = cx + dx, z = cz + dz;
                if (x < 0 || x >= 16 || z < 0 || z >= 16) continue;
                BlockPos fp = new BlockPos(x, fy, z);
                if (!c.getBlockState(fp).isSolid() || c.getBlockState(fp).isAir()) continue;
                if (br.nextFloat() < 0.5f) {
                    BlockPos sp = new BlockPos(x, wby, z);
                    if (c.getBlockState(sp).isAir()) {
                        c.setBlockState(sp, ModBlocks.BACTERIA_SHROOM_HORIZONTAL.getDefaultState()
                                .with(Properties.HORIZONTAL_FACING, Direction.Type.HORIZONTAL.random(br)), false);
                        decayNearbyWallpapers(c, x, z, wby, wty, br);
                    }
                }
            }
        }

        for (int dx = -cs; dx <= cs; dx++) {
            for (int dz = -cs; dz <= cs; dz++) {
                int x = cx + dx, z = cz + dz;
                if (x < 0 || x >= 16 || z < 0 || z >= 16) continue;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist < cs * 0.3f || dist > cs * 0.7f) continue;
                if (br.nextFloat() < 0.15f) placeVerticalShroomOnWall(c, x, z, wby, wty, br);
            }
        }
    }

    private void placeVinesOnNearbyWalls(Chunk c, int x, int z, int wby, int wty, ChunkRandom r) {
        Direction[] ds = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (Direction d : ds) {
            BlockPos wp = new BlockPos(x, wby + r.nextInt(wty - wby), z).offset(d);
            if (wp.getX() < 0 || wp.getX() >= 16 || wp.getZ() < 0 || wp.getZ() >= 16) continue;
            if (isWallpaperBlock(c.getBlockState(wp)) || c.getBlockState(wp).isOf(Blocks.WHITE_CONCRETE) || c.getBlockState(wp).isOf(Blocks.DIORITE)) {
                BlockState vs = ModBlocks.BACTERIA_VINE.getDefaultState();
                switch (d.getOpposite()) {
                    case NORTH -> vs = vs.with(Properties.NORTH, true);
                    case SOUTH -> vs = vs.with(Properties.SOUTH, true);
                    case EAST -> vs = vs.with(Properties.EAST, true);
                    case WEST -> vs = vs.with(Properties.WEST, true);
                    case UP -> vs = vs.with(Properties.UP, true);
                    case DOWN -> vs = vs.with(Properties.DOWN, true);
                }
                c.setBlockState(wp, vs, false);
                return;
            }
        }
    }

    private void placeVerticalShroomOnWall(Chunk c, int x, int z, int wby, int wty, ChunkRandom r) {
        Direction[] ds = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (Direction d : ds) {
            int cy = wby + r.nextInt(wty - wby);
            BlockPos wp = new BlockPos(x, cy, z).offset(d);
            if (wp.getX() < 0 || wp.getX() >= 16 || wp.getZ() < 0 || wp.getZ() >= 16) continue;
            if (isWallpaperBlock(c.getBlockState(wp)) || c.getBlockState(wp).isOf(Blocks.WHITE_CONCRETE) || c.getBlockState(wp).isOf(Blocks.DIORITE)) {
                BlockPos sp = new BlockPos(x, cy, z);
                if (c.getBlockState(sp).isAir()) {
                    c.setBlockState(sp, ModBlocks.BACTERIA_SHROOM_VERTICAL.getDefaultState()
                            .with(Properties.HORIZONTAL_FACING, d), false);
                    return;
                }
            }
        }
    }

    private void decayNearbyWallpapers(Chunk c, int cx, int cz, int wby, int wty, ChunkRandom r) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0) continue;
                int x = cx + dx, z = cz + dz;
                if (x < 0 || x >= 16 || z < 0 || z >= 16) continue;
                for (int y = wby; y <= wty; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (isWallpaperBlock(c.getBlockState(p)) && r.nextFloat() < 0.15f) {
                        decayWallpaperAt(c, p, r);
                    }
                }
            }
        }
    }

    private void decayWallpaperAt(Chunk c, BlockPos p, ChunkRandom r) {
        BlockState cs = c.getBlockState(p);
        if (!isWallpaperBlock(cs)) return;
        float roll = r.nextFloat();
        BlockState ns = cs;
        if (roll < 0.25f) ns = getWetVariant(cs);
        else if (roll < 0.45f) ns = getStainedVariant(cs);
        else if (roll < 0.60f) ns = getMoldyVariant(cs);
        else if (roll < 0.70f) ns = getMoldInfectedVariant(cs);
        if (ns != cs) c.setBlockState(p, ns, false);
    }

    private BlockState getWetVariant(BlockState s) {
        Block b = s.getBlock();
        if (b == ModBlocks.WALLPAPER_BLOCK || b == ModBlocks.STAINED_WALLPAPER_BLOCK) return ModBlocks.WET_WALLPAPER_BLOCK.getDefaultState();
        if (b == ModBlocks.TORN_WALLPAPER_BLOCK || b == ModBlocks.STAINED_TORN_WALLPAPER_BLOCK) return ModBlocks.WET_TORN_WALLPAPER_BLOCK.getDefaultState();
        return s;
    }

    private BlockState getStainedVariant(BlockState s) {
        Block b = s.getBlock();
        if (b == ModBlocks.WALLPAPER_BLOCK) return ModBlocks.STAINED_WALLPAPER_BLOCK.getDefaultState();
        if (b == ModBlocks.TORN_WALLPAPER_BLOCK) return ModBlocks.STAINED_TORN_WALLPAPER_BLOCK.getDefaultState();
        return s;
    }

    private BlockState getMoldyVariant(BlockState s) {
        Block b = s.getBlock();
        if (isRegularWallpaper(b)) return ModBlocks.MOLDY_WALLPAPER_BLOCK.getDefaultState();
        if (isTornWallpaper(b)) return ModBlocks.MOLDY_TORN_WALLPAPER_BLOCK.getDefaultState();
        return s;
    }

    private BlockState getMoldInfectedVariant(BlockState s) {
        Block b = s.getBlock();
        if (isRegularWallpaper(b)) return ModBlocks.MOLD_INFECTED_WALLPAPER_BLOCK.getDefaultState();
        if (isTornWallpaper(b)) return ModBlocks.MOLD_INFECTED_TORN_WALLPAPER_BLOCK.getDefaultState();
        return s;
    }

    private boolean isRegularWallpaper(Block b) {
        return b == ModBlocks.WALLPAPER_BLOCK || b == ModBlocks.STAINED_WALLPAPER_BLOCK ||
                b == ModBlocks.WET_WALLPAPER_BLOCK || b == ModBlocks.MOLDY_WALLPAPER_BLOCK ||
                b == ModBlocks.MOLD_INFECTED_WALLPAPER_BLOCK;
    }

    private boolean isTornWallpaper(Block b) {
        return b == ModBlocks.TORN_WALLPAPER_BLOCK || b == ModBlocks.STAINED_TORN_WALLPAPER_BLOCK ||
                b == ModBlocks.WET_TORN_WALLPAPER_BLOCK || b == ModBlocks.MOLDY_TORN_WALLPAPER_BLOCK ||
                b == ModBlocks.MOLD_INFECTED_TORN_WALLPAPER_BLOCK;
    }

    private boolean isWallpaperBlock(BlockState s) {
        return s.isOf(ModBlocks.WALLPAPER_BLOCK) || s.isOf(ModBlocks.STAINED_WALLPAPER_BLOCK) ||
                s.isOf(ModBlocks.WET_WALLPAPER_BLOCK) || s.isOf(ModBlocks.MOLDY_WALLPAPER_BLOCK) ||
                s.isOf(ModBlocks.MOLD_INFECTED_WALLPAPER_BLOCK) || s.isOf(ModBlocks.SPONGE_WALLPAPER_BLOCK) ||
                s.isOf(ModBlocks.TORN_WALLPAPER_BLOCK) || s.isOf(ModBlocks.STAINED_TORN_WALLPAPER_BLOCK) ||
                s.isOf(ModBlocks.WET_TORN_WALLPAPER_BLOCK) || s.isOf(ModBlocks.MOLDY_TORN_WALLPAPER_BLOCK) ||
                s.isOf(ModBlocks.MOLD_INFECTED_TORN_WALLPAPER_BLOCK);
    }

    private double getDarkZoneNoise(int wx, int wz) {
        double scale = 0.005, wso = worldSeed * 0.0001;
        double v = Math.sin((wx + wso) * scale) * Math.cos((wz - wso) * scale * 1.3) +
                Math.sin((wx + wso) * scale * 0.7 + 1.5) * Math.cos((wz - wso) * scale * 0.9);
        return (v + 2.0) / 4.0;
    }

    private BlockState getOrganicWallBlock(int wx, int wz, int fy) {
        long ss = ((long) wx * 341873128712L + (long) wz * 132897987541L + fy * 999L) ^ worldSeed;
        ChunkRandom sr = new ChunkRandom(new CheckedRandom(ss));
        if (sr.nextFloat() < CONFIG.generationSpongeWallpaperChance) return ModBlocks.SPONGE_WALLPAPER_BLOCK.getDefaultState();

        MoldCluster[] cl = getMoldClustersForRegion(wx, wz);
        double sm = 0;
        for (MoldCluster c : cl) {
            double dx = wx - c.centerX, dz = wz - c.centerZ;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < c.radius) {
                double ad = dist + getMoldNoise(wx, wz, c.seed) * 1.5;
                if (ad < c.radius) {
                    double inf = 1.0 - (ad / c.radius);
                    sm = Math.max(sm, inf);
                }
            }
        }

        if (sm > 0.85) return ModBlocks.MOLD_INFECTED_WALLPAPER_BLOCK.getDefaultState();
        else if (sm > 0.7) return ModBlocks.MOLDY_WALLPAPER_BLOCK.getDefaultState();
        else if (sm > 0.5) return ModBlocks.WET_WALLPAPER_BLOCK.getDefaultState();
        else if (sm > 0.3) return ModBlocks.STAINED_WALLPAPER_BLOCK.getDefaultState();
        else return ModBlocks.WALLPAPER_BLOCK.getDefaultState();
    }

    private MoldCluster[] getMoldClustersForRegion(int wx, int wz) {
        int rx = wx / 64, rz = wz / 64;
        long rk = ((long) rx << 32) | (rz & 0xFFFFFFFFL);
        if (moldCache.containsKey(rk)) return moldCache.get(rk);

        long rs = ((long) rx * 341873128712L + (long) rz * 132897987541L) ^ worldSeed;
        ChunkRandom rr = new ChunkRandom(new CheckedRandom(rs));
        if (rr.nextFloat() > 0.15f) {
            MoldCluster[] e = new MoldCluster[0];
            moldCache.put(rk, e);
            return e;
        }

        int nc = rr.nextInt(2) + 1;
        MoldCluster[] cl = new MoldCluster[nc];
        for (int i = 0; i < nc; i++) {
            cl[i] = new MoldCluster(rx * 64 + rr.nextInt(64), rz * 64 + rr.nextInt(64), rr.nextInt(2) + 2, rr.nextLong());
        }
        moldCache.put(rk, cl);
        return cl;
    }

    private double getMoldNoise(int wx, int wz, long seed) {
        return Math.sin(wx * 0.3 + seed) * Math.cos(wz * 0.3 - seed * 0.7) * 0.5;
    }

    private Direction getAdjacentWallDirection(Chunk c, int x, int z, int wy) {
        if (x > 0 && !c.getBlockState(new BlockPos(x - 1, wy, z)).isAir()) return Direction.EAST;
        if (x < 15 && !c.getBlockState(new BlockPos(x + 1, wy, z)).isAir()) return Direction.WEST;
        if (z > 0 && !c.getBlockState(new BlockPos(x, wy, z - 1)).isAir()) return Direction.SOUTH;
        if (z < 15 && !c.getBlockState(new BlockPos(x, wy, z + 1)).isAir()) return Direction.NORTH;
        return null;
    }

    private void placeLocker(Chunk c, int x, int z, int fy, Direction f) {
        BlockState ls = ModBlocks.LOCKER.getDefaultState().with(Properties.HORIZONTAL_FACING, f).with(Properties.OPEN, false);
        c.setBlockState(new BlockPos(x, fy, z), ls.with(Properties.DOUBLE_BLOCK_HALF, net.minecraft.block.enums.DoubleBlockHalf.LOWER), false);
        c.setBlockState(new BlockPos(x, fy + 1, z), ls.with(Properties.DOUBLE_BLOCK_HALF, net.minecraft.block.enums.DoubleBlockHalf.UPPER), false);
    }

    private void placeWaterCooler(Chunk c, int x, int z, int fy, Direction f) {
        Direction cf = f != null ? f : Direction.fromHorizontal((int) (worldSeed % 4));
        c.setBlockState(new BlockPos(x, fy, z), ModBlocks.WATER_COOLER.getDefaultState().with(Properties.HORIZONTAL_FACING, cf), false);
    }

    private void placeBarrel(Chunk c, int x, int z, int fy, Direction f) {
        c.setBlockState(new BlockPos(x, fy, z), Blocks.BARREL.getDefaultState().with(BarrelBlock.FACING, f.getOpposite()), false);
    }

    private void generateColumns(Chunk c, int csx, int csz, int fsy, int wby, int wty, boolean office) {
        if (office) return;

        long cseed = ((long) csx * 341873128712L + (long) csz * 132897987541L + fsy * 123456L) ^ worldSeed;
        ChunkRandom cr = new ChunkRandom(new CheckedRandom(cseed));
        float cc = CONFIG.generationColumnChance;
        if (cr.nextFloat() >= cc) return;

        int ccnt = cr.nextInt(3) + 1;
        for (int i = 0; i < ccnt; i++) {
            int cx = cr.nextInt(12) + 2, cz = cr.nextInt(12) + 2;
            float sr = cr.nextFloat();
            int size = sr < 0.4f ? 1 : sr < 0.7f ? 2 : sr < 0.9f ? 3 : 4;

            for (int dx = -size / 2; dx <= size / 2; dx++) {
                for (int dz = -size / 2; dz <= size / 2; dz++) {
                    int px = cx + dx, pz = cz + dz;
                    if (px >= 0 && px < 16 && pz >= 0 && pz < 16) {
                        for (int y = wby; y <= wty; y++) {
                            c.setBlockState(new BlockPos(px, y, pz), getWallBlockForY(csx + px, csz + pz, fsy, y, wby, false), false);
                        }
                    }
                }
            }
        }
    }

    private static class GridPos {
        final int x, z, y;
        GridPos(int x, int z, int y) { this.x = x; this.z = z; this.y = y; }
        @Override public boolean equals(Object o) { if (!(o instanceof GridPos other)) return false; return x == other.x && z == other.z && y == other.y; }
        @Override public int hashCode() { return x * 31 * 31 + z * 31 + y; }
    }

    private static class PropPosition {
        final int x, z;
        final Direction wallDirection;
        PropPosition(int x, int z, Direction wd) { this.x = x; this.z = z; this.wallDirection = wd; }
    }

    private static class MoldCluster {
        final int centerX, centerZ, radius;
        final long seed;
        MoldCluster(int cx, int cz, int r, long s) { this.centerX = cx; this.centerZ = cz; this.radius = r; this.seed = s; }
    }
}