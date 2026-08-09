package com.aleksandar.customprintservice.config.env;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class DotenvEnvironmentLoader {

    private static final String DOTENV_FILE_NAME = ".env";

    private DotenvEnvironmentLoader() {
    }

    public static void load() {
        findDotenvDirectory()
                .ifPresent(DotenvEnvironmentLoader::loadFromDirectory);
    }

    private static Optional<Path> findDotenvDirectory() {
        Path currentDirectory = Path.of(System.getProperty("user.dir"));
        if (Files.isRegularFile(currentDirectory.resolve(DOTENV_FILE_NAME))) {
            return Optional.of(currentDirectory);
        }

        Path parentDirectory = currentDirectory.getParent();
        if (parentDirectory != null && Files.isRegularFile(parentDirectory.resolve(DOTENV_FILE_NAME))) {
            return Optional.of(parentDirectory);
        }

        return Optional.empty();
    }

    private static void loadFromDirectory(Path directory) {
        Dotenv dotenv = Dotenv.configure()
                .directory(directory.toString())
                .ignoreIfMissing()
                .load();

        for (DotenvEntry entry : dotenv.entries()) {
            if (System.getenv(entry.getKey()) == null && System.getProperty(entry.getKey()) == null) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        }
    }

}
