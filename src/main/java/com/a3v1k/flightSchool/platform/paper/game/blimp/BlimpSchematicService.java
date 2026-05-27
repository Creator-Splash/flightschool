package com.a3v1k.flightSchool.platform.paper.game.blimp;

import com.a3v1k.flightSchool.application.game.GameManager;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@RequiredArgsConstructor
public final class BlimpSchematicService {

    private static final double BLIMP_RADIUS = 750.0;
    private static final double TARGET_SIDE_LENGTH = 153.0;
    private static final double SCALE_VAL = 0.65;

    private static final String[] TEAM_NAMES = {
        "red", "yellow", "green", "blue",
        "dark_violet", "violet", "dark_blue", "orange"
    };

    private static final Object[][] PASTE_MAP_TEAMS = {
        {"Red", BlockTypes.RED_CONCRETE},
        {"Yellow", BlockTypes.YELLOW_CONCRETE},
        {"Green", BlockTypes.LIME_CONCRETE},
        {"Blue", BlockTypes.LIGHT_BLUE_CONCRETE},
        {"Dark Violet", BlockTypes.MAGENTA_CONCRETE},
        {"Violet", BlockTypes.PURPLE_CONCRETE},
        {"Dark Blue", BlockTypes.BLUE_CONCRETE},
        {"Orange", BlockTypes.ORANGE_CONCRETE}
    };

    private final File dataFolder;
    private final Logger logger;
    private final GameManager gameManager;
    private Clipboard blimpClipboard;

    /* Init */

    public void loadSchematic() throws IOException {
        File schemFile = new File(dataFolder, "blimp.schem");
        ClipboardFormat format = ClipboardFormats.findByFile(schemFile);
        if (format == null) {
            throw new IOException("Unknown schematic format for: " + schemFile.getName());
        }

        try (ClipboardReader reader = format.getReader(new FileInputStream(schemFile))) {
            blimpClipboard = reader.read();
        }

        logger.info("Blimp schematic loaded successfully.");
    }

    /* Blimp Data Comp (collision/explosion segments) */

    public void computeBlimpData(World world, Location center) {
        if (blimpClipboard == null) {
            logger.severe("computeBlimpData called before schematic was loaded");
            return;
        }

        for (int i = 0; i < TEAM_NAMES.length; i++) {
            String teamName = TEAM_NAMES[i];

            if (gameManager.getTeam(teamName) == null) {
                logger.warning("computeBlimpData: no team found for: " + teamName);
                continue;
            }

            double angleRad = Math.toRadians(i * 45);
            BlockVector3 pastePos = BlockVector3.at(
                (int) (center.getX() + BLIMP_RADIUS * Math.cos(angleRad)),
                center.getBlockY(),
                (int) (center.getZ() + BLIMP_RADIUS * Math.sin(angleRad))
            );

            AffineTransform transform = new AffineTransform().rotateY(-(i * 45));
            List<Location> solidBlocks = extractSolidBlocks(blimpClipboard, transform, pastePos, world);
            gameManager.registerBlimp(teamName, solidBlocks);
        }
    }

    /* Map Paste */

    public boolean pasteMap(Location location, int teamCount) {
        if (teamCount > PASTE_MAP_TEAMS.length) teamCount = PASTE_MAP_TEAMS.length;
        if (teamCount < 1) return false;

        File schemFile = new File(dataFolder, "blimp.schem");
        if (!schemFile.exists()) {
            logger.warning("blimp.schem not found in plugin data folder");
            return false;
        }

        Clipboard originalClipboard;
        ClipboardFormat format = ClipboardFormats.findByFile(schemFile);

        try (ClipboardReader reader = format.getReader(new FileInputStream(schemFile))) {
            originalClipboard = reader.read();
        } catch (IOException e) {
            logger.severe("Failed to read blimp schematic: " + e.getMessage());
            return false;
        }

        double radius = resolveRadius(teamCount);
        BlockVector3 center = BukkitAdapter.asBlockVector(location);

        try (EditSession editSession = WorldEdit.getInstance()
            .newEditSession(BukkitAdapter.adapt(location.getWorld()))) {

            for (int i = 0; i < teamCount; i++) {
                String teamName = (String) PASTE_MAP_TEAMS[i][0];
                BlockType teamBlock = (BlockType) PASTE_MAP_TEAMS[i][1];

                double angleDeg = i * (360.0 / teamCount);
                double angleRad = Math.toRadians(angleDeg);

                BlockVector3 pastePos = BlockVector3.at(
                    (int) (center.x() + radius * Math.cos(angleRad)),
                    center.y(),
                    (int) (center.z() + radius * Math.sin(angleRad))
                );

                Clipboard teamClipboard = buildTeamClipboard(originalClipboard, teamBlock);
                if (teamClipboard == null) continue;

                ClipboardHolder holder = new ClipboardHolder(teamClipboard);
                holder.setTransform(holder.getTransform()
                    .combine(new AffineTransform().scale(SCALE_VAL, SCALE_VAL, SCALE_VAL))
                    .combine(new AffineTransform().rotateY(-angleDeg)));

                try {
                    Operation operation = holder.createPaste(editSession)
                        .to(pastePos)
                        .ignoreAirBlocks(true)
                        .build();
                    Operations.complete(operation);
                } catch (WorldEditException e) {
                    logger.severe("Failed to paste team " + teamName + ": " + e.getMessage());
                }
            }
        }

        return true;
    }

    /* Helpers */

    private Clipboard buildTeamClipboard(Clipboard original, BlockType teamBlock) {
        Clipboard teamClipboard = new BlockArrayClipboard(original.getRegion());
        teamClipboard.setOrigin(original.getOrigin());

        for (BlockVector3 pt : original.getRegion()) {
            try {
                teamClipboard.setBlock(pt, original.getBlock(pt));
            } catch (WorldEditException e) {
                logger.warning("Failed to copy block at " + pt + ": " + e.getMessage());
            }
        }

        for (BlockVector3 pt : teamClipboard.getRegion()) {
            if (teamClipboard.getBlock(pt).getBlockType() == BlockTypes.DIORITE) {
                try {
                    teamClipboard.setBlock(pt, teamBlock.getDefaultState());
                } catch (WorldEditException e) {
                    logger.warning("Failed to replace block at " + pt + ": " + e.getMessage());
                }
            }
        }

        return teamClipboard;
    }

    private List<Location> extractSolidBlocks(
        Clipboard clipboard,
        Transform transform,
        BlockVector3 pastePos,
        World world
    ) {
        List<Location> result = new ArrayList<>();

        for (BlockVector3 pt : clipboard.getRegion()) {
            if (clipboard.getBlock(pt).getBlockType() == BlockTypes.AIR) continue;

            Vector3 transformed = transform.apply(pt.toVector3());
            result.add(new Location(
                world,
                pastePos.x() + transformed.x(),
                pastePos.y() + transformed.y(),
                pastePos.z() + transformed.z()
            ));
        }

        return result;
    }

    private double resolveRadius(int teamCount) {
        if (teamCount == 1) return 0;
        if (teamCount == 2) return 100;
        return TARGET_SIDE_LENGTH / (2.0 * Math.sin(Math.PI / teamCount));
    }

}
