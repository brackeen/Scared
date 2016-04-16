package com.brackeen.scared;

import java.util.concurrent.TimeUnit;

public class Stats {
    public int numDeaths;
    public int numShotsFired;
    public int numShotsFiredHit;
    public int numEnemyShotsFired;
    public int numEnemyShotsFiredHit;
    public boolean cheated;
    public long startTime;
    
    public Stats() {
        startTime = System.nanoTime();
    }
    
    // End-of-level stats
    public int numSecretsFound;
    public int numKills;
    public int totalSecrets;
    public int totalEnemies;
    
    public String getDescription(Map map, int level) {
        final int padWidth = 39;
        String playerAccuracy = (numShotsFired == 0) ? "N/A" : String.format("%.1f%%",
                numShotsFiredHit * 100.0f / numShotsFired);
        String enemyAccuracy = (numEnemyShotsFired == 0) ? "N/A" : String.format("%.1f%%",
                numEnemyShotsFiredHit * 100.0f / numEnemyShotsFired);
        return (padLine("              Level: " + level + " / " + GameScene.NUM_LEVELS, padWidth) + "\n" +
                padLine("        Shots fired: " + numShotsFired, padWidth) + "\n" +
                padLine("      Shot accuracy: " + playerAccuracy, padWidth) + "\n" +
                padLine("     Enemies killed: " + (numKills + map.getPlayer().getKills()) + " / " + (totalEnemies + map.getNumEnemies()), padWidth) + "\n" +
                padLine("  Enemy shots fired: " + numEnemyShotsFired, padWidth) + "\n" +
                padLine("Enemy shot accuracy: " + enemyAccuracy, padWidth) + "\n" +
                padLine("      Secrets found: " + (numSecretsFound + map.getPlayer().getSecrets()) + " / " + (totalSecrets + map.getNumSecrets()), padWidth) + "\n" +
                padLine("             Deaths: " + numDeaths, padWidth) + "\n" +
                padLine("               Time: " + getTimeElapsed(startTime), padWidth) + "\n" +
                padLine("            Cheated: " + (cheated ? "YES" : "NO"), padWidth));
    }
    
    private static String getTimeElapsed(long startTimeNanos) {
        long nanos = System.nanoTime() - startTimeNanos;
        return String.format("%02d:%02d:%02d.%03d",
                TimeUnit.NANOSECONDS.toHours(nanos),
                TimeUnit.NANOSECONDS.toMinutes(nanos) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.NANOSECONDS.toHours(nanos)),
                TimeUnit.NANOSECONDS.toSeconds(nanos) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.NANOSECONDS.toMinutes(nanos)),
                TimeUnit.NANOSECONDS.toMillis(nanos) -
                        TimeUnit.SECONDS.toMillis(TimeUnit.NANOSECONDS.toSeconds(nanos))
        );
    }
    
    private static String padLine(String line, int minLength) {
        // Poor man's center on the colon
        if (line != null && line.length() < minLength) {
            StringBuilder sb = new StringBuilder(line);
            while (sb.length() < minLength) {
                sb.append(' ');
            }
            line = sb.toString();
        }
        return line;
    }
}
