/*
 * Copyright (c) 2026 Gibbon
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACTBinding, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
 
package com.gibbon.correctedshipwrecks.client.mixin;

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
