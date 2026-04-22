package com.springer.knakobrak.net.gameServerHelpers;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.springer.knakobrak.net.ClientHandler;
import com.springer.knakobrak.net.GameServer;
import com.springer.knakobrak.net.messages.ChatMessage;
import com.springer.knakobrak.net.messages.NetMessage;
import com.springer.knakobrak.net.messages.PlayerInputMessage;
import com.springer.knakobrak.net.messages.SpawnProjectileMessage;
import com.springer.knakobrak.util.LoadUtillities;
import com.springer.knakobrak.world.PhysicsSimulation;
import com.springer.knakobrak.world.ProjectileId;
import com.springer.knakobrak.world.ProjectileState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.springer.knakobrak.util.Constants.*;

public class GameHelper {

    private GameServer gameServer;

    private PhysicsSimulation simulation;
    private Map<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private int nextProjectileId = 0;

    public GameHelper(PhysicsSimulation simulation, Map<Integer, ClientHandler> clients) {
        this.simulation = simulation;
        this.simulation.owner = "Server";
        this.clients = clients;
    }

    public void handleGameMessage(ClientHandler sender, NetMessage msg) {
        if (msg instanceof PlayerInputMessage) {
            PlayerInputMessage pim = (PlayerInputMessage) msg;
            handlePlayerInput(sender, pim);
        } else if (msg instanceof SpawnProjectileMessage) {
            SpawnProjectileMessage spm = (SpawnProjectileMessage) msg;
            handleSpawnProjectile(sender, spm);
        } else if (msg instanceof ChatMessage) {
            ChatMessage cm = (ChatMessage) msg;
            handleChatMessage(cm);
        }
    }

    void handlePlayerInput(ClientHandler sender, PlayerInputMessage pim) {
        int sequence = pim.sequence;
        float dx = pim.dx;
        float dy = pim.dy;
        Body body = sender.playerState.body;
        if (body == null) return;
        Vector2 desiredVelocity = new Vector2(dx, dy)
            .nor()
            .scl(PLAYER_SPEED_MPS);
        body.setLinearVelocity(desiredVelocity);
        sender.lastProcessedInput = sequence;
    }

    void handleSpawnProjectile(ClientHandler sender, SpawnProjectileMessage spm) {
        float dx = spm.dx;
        float dy = spm.dy;
        ProjectileState proj = new ProjectileState();
        proj.clientId = spm.ownerId;
        proj.counter = spm.counter;
        //proj.localPlayerFireSequence = spm.fireSequence;
        Vector2 dir = new Vector2(dx, dy).nor();
        Vector2 spawnPos = sender.playerState.body.getPosition()
            .cpy()
            .add(dir.scl(BULLET_SPAWN_OFFSET_M));
        proj.body = LoadUtillities.createProjectile(
            simulation.getWorld(),
            spawnPos.x,
            spawnPos.y,
            proj.clientId,
            proj.counter
        );
        proj.body.setLinearVelocity(
            dir.scl(BULLET_SPEED_MPS)
        );
        ProjectileId newId = new ProjectileId(proj.clientId, proj.counter);
        simulation.addProjectile(newId, proj);
        //System.out.println("Spawning projectile " + newId);
        //simulation.projectiles.put(proj.id, proj);
    }

    void handleChatMessage(ChatMessage cm) {
        cm.message = "<" + clients.get(cm.playerId).name + ">" + cm.message;
        gameServer.broadcast(cm);
    }
}
