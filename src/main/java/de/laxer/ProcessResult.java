package de.laxer;

public record ProcessResult(int exitCode, String stdout, String stderr) {}

