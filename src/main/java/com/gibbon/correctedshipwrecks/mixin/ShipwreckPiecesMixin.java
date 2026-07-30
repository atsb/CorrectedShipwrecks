package com.gibbon.correctedshipwrecks.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.structures.ShipwreckPieces;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ShipwreckPieces.ShipwreckPiece.class)
public abstract class ShipwreckPiecesMixin extends StructurePiece {

    protected ShipwreckPiecesMixin() {
        super(null, 0, null);
    }

    @Redirect(
            method = "postProcess",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/WorldGenLevel;getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I"
            )
    )
    private int filterShipwreckGroundHeight(WorldGenLevel level, Heightmap.Types heightmapType, int x, int z) {
        int vanillaY = level.getHeight(heightmapType, x, z);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, vanillaY, z);

        // find the actual highest / topmost block
        while (pos.getY() > level.getMinY() && level.getBlockState(pos).isAir()) {
            pos.move(0, -1, 0);
        }
        BlockState topState = level.getBlockState(pos);
        if (isIceOrSnow(topState)) {
            // sink the ship down to the ocean floor
            while (pos.getY() > level.getMinY()) {
                BlockState state = level.getBlockState(pos);
                if (!state.isAir() && !isIceOrSnow(state) && !isWaterOrPlant(state)) {
                    // found the actual seabed
                    return pos.getY();
                }
                pos.move(0, -1, 0);
            }
        }

        // if it's not ice we return the vanilla height.
        return vanillaY;
    }

    private boolean isIceOrSnow(BlockState blockState) {
        String blockId = blockState.getBlock().getDescriptionId().toLowerCase();
        return blockId.contains("packed_ice")
                || blockId.contains("blue_ice")
                || blockId.contains("ice")
                || blockId.contains("snow");
    }

    private boolean isWaterOrPlant(BlockState blockState) {
        String blockId = blockState.getBlock().getDescriptionId().toLowerCase();
        return blockId.contains("water")
                || blockId.contains("kelp")
                || blockId.contains("seagrass")
                || blockId.contains("pickle");
    }
}
