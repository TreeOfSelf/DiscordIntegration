package de.erdbeerbaerlp.dcintegration.architectury.util;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.LinkManager;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.PlayerLink;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.database.DBInterface;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// Link database one file can back for several servers (databaseClass in Discord-Integration.toml).
// DiscordIntegration-Data/LinkedPlayers.path holds the shared file's path; without it the usual
// DiscordIntegration-Data/LinkedPlayers.json is used. Writes take a file lock, and each server reloads
// its links when the file changes, so linking on one server counts on all of them.
public class SharedLinkStore extends DBInterface {
    private static final Path POINTER = Path.of("DiscordIntegration-Data", "LinkedPlayers.path");
    private static final Path LOCAL = Path.of("DiscordIntegration-Data", "LinkedPlayers.json");
    private static ScheduledExecutorService watcher;
    private Path file;
    private long seenModified;

    @Override
    public void connect() {
        file = LOCAL;
        try {
            if (Files.isRegularFile(POINTER)) file = Path.of(Files.readString(POINTER).strip());
        } catch (IOException e) {
            DiscordIntegration.LOGGER.error("Could not read " + POINTER, e);
        }
        DiscordIntegration.LOGGER.info("Linked players stored in " + file.toAbsolutePath());
    }

    @Override
    public void initialize() {
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            if (!Files.exists(file)) Files.writeString(file, "[]");
            seenModified = Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
            DiscordIntegration.LOGGER.error("Could not create " + file, e);
        }
        if (watcher == null) {
            watcher = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "DiscordIntegration link watcher");
                t.setDaemon(true);
                return t;
            });
            watcher.scheduleWithFixedDelay(this::reloadIfChanged, 5, 5, TimeUnit.SECONDS);
        }
    }

    private void reloadIfChanged() {
        try {
            long modified = Files.getLastModifiedTime(file).toMillis();
            if (modified == seenModified) return;
            seenModified = modified;
            LinkManager.load();
            DiscordIntegration.LOGGER.info("Reloaded " + LinkManager.getAllLinks().size() + " linked players from " + file);
        } catch (Exception e) {
            DiscordIntegration.LOGGER.error("Could not reload " + file, e);
        }
    }

    @Override
    public boolean isConnected() {
        return file != null;
    }

    @Override
    public void addLink(PlayerLink link) {
        update(links -> {
            links.removeIf(l -> l.discordID != null && l.discordID.equals(link.discordID));
            links.add(link);
        });
    }

    @Override
    public void removeLink(String discordID) {
        update(links -> links.removeIf(l -> discordID.equals(l.discordID)));
    }

    @Override
    public PlayerLink[] getAllLinks() {
        return read().toArray(new PlayerLink[0]);
    }

    private List<PlayerLink> read() {
        try {
            if (!Files.isRegularFile(file)) return new ArrayList<>();
            PlayerLink[] links = gson.fromJson(Files.readString(file), PlayerLink[].class);
            return links == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(links));
        } catch (Exception e) {
            DiscordIntegration.LOGGER.error("Could not read " + file, e);
            return new ArrayList<>();
        }
    }

    private void update(java.util.function.Consumer<List<PlayerLink>> change) {
        Path lock = file.resolveSibling(file.getFileName() + ".lock");
        try (FileChannel channel = FileChannel.open(lock, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock ignored = channel.lock()) {
            List<PlayerLink> links = read();
            change.accept(links);
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, gson.toJson(links.toArray(new PlayerLink[0])));
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            seenModified = Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
            DiscordIntegration.LOGGER.error("Could not write " + file, e);
        }
    }
}
