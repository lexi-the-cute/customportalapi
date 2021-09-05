package net.kyrptonaught.customportalapi.portal;

import net.kyrptonaught.customportalapi.CustomPortalApiRegistry;
import net.kyrptonaught.customportalapi.CustomPortalsMod;
import net.kyrptonaught.customportalapi.portal.frame.PortalFrameTester;
import net.kyrptonaught.customportalapi.util.CustomPortalHelper;
import net.kyrptonaught.customportalapi.util.PortalLink;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockLocating;
import net.minecraft.world.BlockLocating.Rectangle;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;

import java.util.Iterator;
import java.util.Optional;

public class PortalPlacer {
    public static boolean attemptPortalLight(World world, BlockPos portalPos, BlockPos framePos, PortalIgnitionSource ignitionSource) {
        Block foundationBlock = world.getBlockState(framePos).getBlock();
        PortalLink link = CustomPortalApiRegistry.getPortalLinkFromBase(foundationBlock);

        if (link == null || !link.doesIgnitionMatch(ignitionSource) || !link.canLightInDim(world.getRegistryKey().getValue()))
            return false;
        return createPortal(link, world, portalPos, foundationBlock);
    }

    private static boolean createPortal(PortalLink link, World world, BlockPos pos, Block foundationBlock) {
        Optional<PortalFrameTester> optional = link.getFrameTester().createInstanceOfPortalFrameTester().getNewPortal(world, pos, Direction.Axis.X, foundationBlock);
        //is valid frame, and is correct size(if applicable)
        if (optional.isPresent()) {
            if (optional.get().isRequestedSize(link.forcedWidth, link.forcedHeight))
                optional.get().lightPortal(foundationBlock);
            return true;
        }
        return false;
    }

    public static Optional<Rectangle> createDestinationPortal(World world, BlockPos blockPos, BlockState frameBlock, Direction.Axis axis) {
        WorldBorder worldBorder = world.getWorldBorder();
        PortalLink link = CustomPortalApiRegistry.getPortalLinkFromBase(frameBlock.getBlock());
        PortalFrameTester portalFrameTester = link.getFrameTester().createInstanceOfPortalFrameTester();
        for (BlockPos.Mutable mutable : BlockPos.iterateInSquare(blockPos, 16, Direction.WEST, Direction.SOUTH)) {
            BlockPos testingPos = mutable.toImmutable();

            int solidY = Math.min(world.getTopY(), world.getBottomY() + world.getLogicalHeight()) - 5;
            BlockPos pos = null;
            while (solidY >= 3) {
                if (canHoldPortal(world.getBlockState(testingPos.withY(solidY)))) {
                    BlockLocating.Rectangle testRect = portalFrameTester.doesPortalFitAt(world, testingPos.withY(solidY + 1), axis);
                    if (testRect != null) {
                        pos = testRect.lowerLeft;
                        break;
                    }
                }
                solidY--;
            }
            if (pos != null) {
                portalFrameTester.createPortal(world, pos, frameBlock, axis);
                return Optional.of(new Rectangle(pos, 2, 3));
            }
        }
        return Optional.empty();
    }

    private static boolean canHoldPortal(BlockState state) {
        return state.getMaterial().isSolid();
    }
}