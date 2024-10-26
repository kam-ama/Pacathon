package com.buaisociety.pacman.entity.behavior;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.buaisociety.pacman.maze.Maze;
import com.buaisociety.pacman.maze.Tile;
import com.buaisociety.pacman.maze.TileState;
import com.buaisociety.pacman.sprite.DebugDrawing;
import com.cjcrafter.neat.Client;
import com.cjcrafter.neat.compute.Calculator;
import com.buaisociety.pacman.entity.Direction;
import com.buaisociety.pacman.entity.Entity;
import com.buaisociety.pacman.entity.PacmanEntity;
import com.buaisociety.pacman.Searcher;
import java.util.concurrent.ThreadLocalRandom;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TournamentBehavior implements Behavior {

    private final Calculator calculator;
    private @Nullable PacmanEntity pacman;

    private int previousScore = 0;
    private int framesSinceScoreUpdate = 0;

    public TournamentBehavior(Calculator calculator) {
        this.calculator = calculator;
    }

    /**
     * Returns the desired direction that the entity should move towards.
     *
     * @param entity the entity to get the direction for
     * @return the desired direction for the entity
     */
    @NotNull
    @Override
    public Direction getDirection(@NotNull Entity entity) {
        // --- DO NOT REMOVE ---
        if (pacman == null) {
            pacman = (PacmanEntity) entity;
        }

        int newScore = pacman.getMaze().getLevelManager().getScore();
        if (previousScore != newScore) {
            previousScore = newScore;
            framesSinceScoreUpdate = 0;
        } else {
            framesSinceScoreUpdate++;
        }

        if (framesSinceScoreUpdate > 60 * 40) {
            pacman.kill();
            framesSinceScoreUpdate = 0;
        }
        // --- END OF DO NOT REMOVE ---

        // TODO: Put all your code for info into the neural network here

        // We are going to use these directions a lot for different inputs. Get them all once for clarity and brevity
        Direction forward = pacman.getDirection();
        Direction left = pacman.getDirection().left();
        Direction right = pacman.getDirection().right();
        Direction behind = pacman.getDirection().behind();

        // Input nodes 1, 2, 3, and 4 show if the pacman can move in the forward, left, right, and behind directions
        boolean canMoveForward = pacman.canMove(forward);
        boolean canMoveLeft = pacman.canMove(left);
        boolean canMoveRight = pacman.canMove(right);
        boolean canMoveBehind = pacman.canMove(behind);

        float randomNumber = ThreadLocalRandom.current().nextFloat();

        // Score distances to pellets and power pellets
        Tile currentTile = pacman.getMaze().getTile(pacman.getTilePosition());
        Map<Direction, Searcher.SearchResult> nearestPellets = Searcher.findTileInAllDirections(currentTile, tile -> tile.getState() == TileState.PELLET);
        Map<Direction, Searcher.SearchResult> nearestPower_Pellets = Searcher.findTileInAllDirections(currentTile, tile -> tile.getState() == TileState.POWER_PELLET);

        // ChatGPT - Set scoring factors for pellets and power pellets
        float pelletWeight = 1.0f;
        float powerPelletWeight = 2.0f; // Assing high weight to power pellets
        float distanceDecay = 0.1f; //Decay factor for distance, adjust based on desired influence

        // ChatGPT - Calculate scores based on distances to pellets and power pellets
        Map<Direction, Float> directionScores = new HashMap<>();

        for (Direction dir : Direction.values()) {
            float score = 0.0f; //ChatGPT
            // (original) float pelletScore = nearestPellets.containsKey(dir) ? pelletWeight / (nearestPellets.get(dir).getDistance() + 1) : 0;
            //ChatGPT:
            float pelletScore = (nearestPellets.containsKey(dir) && nearestPellets.get(dir).getDistance() < pacman.getMaze().getDimensions().length()) ?
                pelletWeight / (nearestPellets.get(dir).getDistance() + 1) : 0;
            // (original)float powerPelletScore = nearestPower_Pellets.containsKey(dir) ? powerPelletWeight / (nearestPower_Pellets.get(dir).getDistance() + 1) : 0;
            float powerPelletScore = (nearestPower_Pellets.containsKey(dir) && nearestPower_Pellets.get(dir).getDistance() < pacman.getMaze().getDimensions().length()) ?
                powerPelletWeight / (nearestPower_Pellets.get(dir).getDistance() + 1) : 0;
            directionScores.put(dir, pelletScore + powerPelletScore);
    }

        int maxDistance = -1;
        for (Searcher.SearchResult result : nearestPellets.values()) {
            if (result != null) {
                int distance = result.getDistance();
                //maxDistance = Math.max(maxDistance, result.getDistance());
                //scoreModifier += pelletWeight / (distance + 1) * Math.exp(-distance *distanceDecay); //exponential decay
            }
        }

        float nearestPelletForward = nearestPellets.get(forward) != null ? 1 - (float) nearestPellets.get(forward).getDistance() / maxDistance : 0;
        float nearestPelletLeft = nearestPellets.get(left) != null ? 1 - (float) nearestPellets.get(left).getDistance() / maxDistance : 0;
        float nearestPelletRight = nearestPellets.get(right) != null ? 1 - (float) nearestPellets.get(right).getDistance() / maxDistance : 0;
        float nearestPelletBehind = nearestPellets.get(behind) != null ? 1 - (float) nearestPellets.get(behind).getDistance() / maxDistance : 0;

        int maxPowerDistance = -1;
        for (Map.Entry<Direction, Searcher.SearchResult> result : nearestPower_Pellets.entrySet()) {
            if (result != null) {
                //maxDistance = Math.max(maxPowerDistance, result.getDistance());
                int distance = result.getValue().getDistance();
                //scoreModifier += powerPelletWeight / (distance + 1) * Math.exp(-distance * distanceDecay); //exponential decay
            }

            //directionScores.put(result.getKey(), scoreModifier);
        }

        float nearestPower_PelletForward = nearestPower_Pellets.get(forward) != null ? 1 - (float) nearestPower_Pellets.get(forward).getDistance() / maxPowerDistance : 0;
        float nearestPower_PelletLeft = nearestPower_Pellets.get(left) != null ? 1 - (float) nearestPower_Pellets.get(left).getDistance() / maxPowerDistance : 0;
        float nearestPower_PelletRight = nearestPower_Pellets.get(right) != null ? 1 - (float) nearestPower_Pellets.get(right).getDistance() / maxPowerDistance : 0;
        float nearestPower_PelletBehind = nearestPower_Pellets.get(behind) != null ? 1 - (float) nearestPower_Pellets.get(behind).getDistance() / maxPowerDistance : 0;

        float[] outputs = calculator.calculate(new float[]{
            canMoveForward ? 1f : 0f,
            canMoveLeft ? 1f : 0f,
            canMoveRight ? 1f : 0f,
            canMoveBehind ? 1f : 0f,
            nearestPelletForward,
            nearestPelletLeft,
            nearestPelletRight,
            nearestPelletBehind,
            nearestPower_PelletForward,
            nearestPower_PelletLeft,
            nearestPower_PelletRight,
            nearestPower_PelletBehind,
            randomNumber
        }).join();

        //Direction newDirection = switch (index) { 
        //    case 0 -> pacman.getDirection();
        //    case 1 -> pacman.getDirection().left();
        //    case 2 -> pacman.getDirection().right();
        //    case 3 -> pacman.getDirection().behind();
        //    default -> throw new IllegalStateException("Unexpected value: " + index);
        //};

        // ChatGPT: Find the best direction considering bosth neural network and pellet proximity
        Direction bestDirection = pacman.getDirection();
        float maxScore = -Float.MAX_VALUE;
        
        for (Direction dir : Direction.values()){
            float combinedScore = directionScores.getOrDefault(dir, 0f);
            if (combinedScore > maxScore){
                maxScore = combinedScore;
                bestDirection = dir;
            }
        }
        //for (int i = 0; i < 4; i++) {
            //Direction dir = switch (i) {
                //case 0 -> pacman.getDirection();
                //case 1 -> pacman.getDirection().left();
                //case 2 -> pacman.getDirection().right();
                //case 3 -> pacman.getDirection().behind();
                //default -> throw new IllegalStateException("Unexpected value: " + i);
            //};
            //float combinedScore = outputs[i] + directionScores.getOrDefault(dir, 0f);
            //if (combinedScore > maxScore) {
                //maxScore = combinedScore;
                //bestDirection = dir;
            //}
        

        // Chat GPT - Update the score if necessary and return the best direction
        //client.setScore(pacman.getMaze().getLevelManager().getScore() + scoreModifier);
        return bestDirection;

        //(original) client.setScore(pacman.getMaze().getLevelManager().getScore() + scoreModifier);
        //return newDirection;

    }
}
