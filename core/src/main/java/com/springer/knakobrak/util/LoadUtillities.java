package com.springer.knakobrak.util;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.springer.knakobrak.world.Wall;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.springer.knakobrak.util.Constants.BULLET_RADIUS_M;
import static com.springer.knakobrak.util.Constants.PLAYER_RADIUS_M;

public class LoadUtillities {


    public static Body createPlayerBody(World world, float x, float y, int playerId) {
        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.DynamicBody;
        bd.position.set(x, y);

        Body body = world.createBody(bd);

        CircleShape shape = new CircleShape();
        shape.setRadius(PLAYER_RADIUS_M);

        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        fd.density = 1f;
        fd.friction = 0f;
        fd.restitution = 0.4f;

        body.setFixedRotation(true);

        Fixture f = body.createFixture(fd);
        f.setUserData(playerId);

        shape.dispose();

        return body;
    }

    public static Body createProjectile(World world, float x, float y, int projId) {
        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.DynamicBody;
        bd.bullet = true;
        bd.position.set(x, y);

        Body body = world.createBody(bd);

        CircleShape shape = new CircleShape();
        shape.setRadius(BULLET_RADIUS_M);

        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        fd.density = 1f;
        fd.friction = 0f;
        fd.restitution = 1f;

        //fd.isSensor = true; // IMPORTANT for bullets

        Fixture f = body.createFixture(fd);
        f.setUserData(projId);

        //body.setLinearVelocity(vx, vy);

        shape.dispose();
        return body;
    }

    public static Body createWall(World world, float x, float y, int height, int width) {
        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.StaticBody;
        //bd.position.set(x - width/2f, y - height/2f);
        bd.position.set(x, y);

        Body body = world.createBody(bd);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(width / 2f, height / 2f);
        //shape.setAsBox(width, height);

        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        fd.density = 0f;
        fd.friction = 0f;
        fd.restitution = 0f;

        body.createFixture(fd);
        body.setFixedRotation(true);
        shape.dispose();
        return body;
    }

    public static Wall createMergedWall(World world, int x, int y, int w, int h) {

        float centerX = (x + w / 2f);
        float centerY = (y + h / 2f);

        Wall wall = new Wall();
        wall.body = LoadUtillities.createWall(world, centerX, centerY, h, w);
        wall.x = wall.body.getPosition().x;
        wall.y = wall.body.getPosition().y;
        wall.height = h;
        wall.width = w;
        return wall;
    }

    public static Body createTestBox(World world) {
        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.StaticBody;

        // position is CENTER of the box, in meters
        bd.position.set(2f + 0.5f, 2f + 0.5f);

        Body body = world.createBody(bd);

        PolygonShape shape = new PolygonShape();

        // setAsBox takes HALF-width and HALF-height (meters)
        shape.setAsBox(0.5f, 0.5f); // 1m × 1m total

        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        fd.density = 0f;
        fd.friction = 0f;
        fd.restitution = 0f;

        body.createFixture(fd);
        shape.dispose();

        return body;
    }

    public static int[][] loadLevel(String path) throws IOException {
        List<int[]> rows = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                //worldHeight++;
                String[] parts = line.split("\\s+");
                //worldWidth = parts.length;
                int[] row = new int[parts.length];
                //for (int i = parts.length-1; i >= 0; i--) {
                for (int i = 0; i < parts.length; i++) {
                    row[i] = Integer.parseInt(parts[i]);
                }
                rows.add(row);
            }
        }

        int[] tmp;
        for (int i = 0; i < rows.size()/2; i++) {
            tmp = rows.get(i);
            rows.set(i, rows.get(rows.size()-i-1));
            rows.set(rows.size()-i-1, tmp);
        }

        return rows.toArray(new int[0][]);
    }

    public static ArrayList<Wall> generateWallsFromGrid(World world, int[][] grid) {
        int rows = grid.length;
        int cols = grid[0].length;

        boolean[][] used = new boolean[rows][cols];
        ArrayList<Wall> walls = new ArrayList<>();

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {

                if (grid[y][x] != 1 || used[y][x]) continue;
//                if (grid[y][x] == 2) {a
//                    simulation.addPlayerSpawnPoint(new Vector2(x, y));
//                    continue;
//                }

                int width = 1;
                while (x + width < cols &&
                    grid[y][x + width] == 1 &&
                    !used[y][x + width]) {
                    width++;
                }

                // ---- Expand vertically ----
                int height = 1;
                boolean canExpand = true;
                while (y + height < rows && canExpand) {

                    for (int i = 0; i < width; i++) {
                        if (grid[y + height][x + i] == 0 ||
                            used[y + height][x + i]) {
                            canExpand = false;
                            break;
                        }
                    }

                    if (canExpand) height++;
                }

                for (int dy = 0; dy < height; dy++) {
                    for (int dx = 0; dx < width; dx++) {
                        used[y + dy][x + dx] = true;
                    }
                }

                walls.add(LoadUtillities.createMergedWall(world, x, y, width, height));
            }
        }
        return walls;
    }

    public static ArrayList<Vector2> getPlayerSpawnPoints(int[][] grid) {
        ArrayList<Vector2> myList = new ArrayList<>();
        for (int y = 0; y < grid.length; y++) {
            for (int x = 0; x < grid[0].length; x++) {

                if (grid[y][x] == 0) continue;
                if (grid[y][x] == 2) {
                    myList.add(new Vector2(x, y));
                }
            }
        }
        return myList;
    }
}
