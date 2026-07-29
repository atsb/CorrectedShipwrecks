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

    // method that fixes the calculations based on WORLD_SURFACE_WG and OCEAN_FLOOR_WG.
    private int filterShipwreckGroundHeight(WorldGenLevel level, Heightmap.Types heightmapType, int x, int z) {
        if (heightmapType == Heightmap.Types.WORLD_SURFACE_WG) {
            int oceanFloorY = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, oceanFloorY, z);
            int targetY = level.getSeaLevel();

            while (pos.getY() > level.getSeaLevel()) {
                BlockState blockState = level.getBlockState(pos);

                if (isSolidGround(blockState)) {
                    targetY = pos.getY();
                    break;
                }
                pos.move(0, -1, 0);
            }

	    // we embed the wrecks into the seabed / floor a little bit
            return Math.max(level.getMinY(), targetY - 3);
        }
        return level.getHeight(heightmapType, x, z);
    }

    // avoid all we can that pertains to wrecks.  This allows generation into seabeds full of life and allows the generation logic to also place seagrass etc onto the wreck.
    private boolean isSolidGround(BlockState blockState) {
        if (blockState.isAir()) {
            return true;
        }

        String blockId = blockState.getBlock().getDescriptionId().toLowerCase();
        boolean isNonSolidBlock = blockId.contains("cloud")
                || blockId.contains("packed_ice")
                || blockId.contains("blue_ice")
                || blockId.contains("ice")
                || blockId.contains("water")
                || blockId.contains("kelp")
                || blockId.contains("seagrass")
                || blockId.contains("pickle");
        return !isNonSolidBlock;
    }
}
