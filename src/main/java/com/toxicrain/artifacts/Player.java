package com.toxicrain.artifacts;

import com.toxicrain.core.GameEngine;
import com.toxicrain.core.Logger;
import com.toxicrain.light.LightSystem;
import com.toxicrain.sound.SoundSystem;
import com.toxicrain.texture.TextureInfo;
import com.toxicrain.core.interfaces.IArtifact;
import com.toxicrain.core.json.GameInfoParser;
import com.toxicrain.core.json.KeyInfoParser;
import com.toxicrain.core.json.MapInfoParser;
import com.toxicrain.core.json.SettingsInfoParser;
import com.toxicrain.core.render.BatchRenderer;
import com.toxicrain.factories.GameFactory;
import com.toxicrain.gui.ImguiHandler;
import com.toxicrain.core.Color;
import com.toxicrain.texture.TextureSystem;
import com.toxicrain.util.MathUtils;
import com.toxicrain.util.MouseUtils;
import com.toxicrain.util.WindowUtils;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * The Player class provides information about the player
 */
public class Player implements IArtifact {

    @Getter @Setter
    private float posX;
    @Setter @Getter
    private float posY;
    @Setter @Getter
    private float posZ;
    @Getter @Setter
    private TextureInfo texture;
    private boolean isSprinting;
    public float cameraX, cameraY, cameraZ = 2; // Default camera Z
    public static final float scrollSpeed = 0.2f;
    private float prevCameraX, prevCameraY;
    public float scrollOffset;

    // New stress-related variables
    public float stressLevel;
    private final float maxStressLevel = 100.0f;
    private float baseCameraSpeed = 0.02f; // Base camera speed
    private float currentCameraSpeed; // Speed affected by stress

    private final List<Weapon> weapons;
    private Weapon equippedWeapon;
    private float[] openglMousePos;

    public Player(float posX, float posY, float posZ, TextureInfo texture, boolean isSprinting) {
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.texture = texture;
        this.isSprinting = isSprinting;
        this.weapons = new ArrayList<>();
        this.cameraX = MapInfoParser.playerx;
        this.cameraY = MapInfoParser.playery;
        this.stressLevel = 0; // Initialize stress level
        this.currentCameraSpeed = baseCameraSpeed; // Set initial speed
    }

    public void addWeapon(Weapon weapon) {
        weapons.add(weapon);
    }

    public void equipWeapon(Weapon weapon) {
        if (weapon != null && weapons.contains(weapon)) {
            if (equippedWeapon != null) {
                equippedWeapon.unequip();
            }
            equippedWeapon = weapon;
            equippedWeapon.equip();
            Logger.printLOG("Equipped weapon: " + weapon.getName());
        } else {
            Logger.printLOG("Weapon not found in inventory.");
        }
    }

    public void attack() {
        if (equippedWeapon != null) {
            equippedWeapon.attack();
            increaseStress(10); // Increase stress when attacking
        } else {
            Logger.printLOG("No weapon equipped.");
        }
    }

    private float getAngle(float[] mousePos) {
        float dx = mousePos[0] - posX;
        float dy = mousePos[1] - posY;
        return (float) Math.atan2(dy, dx);
    }

    private void forward(boolean useMouse, int direction) {
        getMouse();
        float angle = getAngle(openglMousePos);
        float angleXS = (float) Math.sin(angle) * -1;
        float angleYS = (float) Math.cos(angle);

        if (useMouse) {
            cameraX += (openglMousePos[0] - posX) * 0.005f * direction;
            cameraY += (openglMousePos[1] - posY) * 0.005f * direction;
        } else {
            cameraX += angleXS * 0.007f * direction;
            cameraY += angleYS * 0.007f * direction;
        }
    }

    public void update(float deltaTime) {
        if (!ImguiHandler.imguiWindowOpen) {
            processInput();
            updatePos(cameraX, cameraY, cameraZ);
            Vector3f center = WindowUtils.getCenter();

            // Manage the player's light dynamically
            manageLight();

            // Update stress level over time (can adjust based on game logic)
            decreaseStress(0.5f * deltaTime); // Gradually decrease stress

            // Adjust camera speed based on stress level
            currentCameraSpeed = baseCameraSpeed * (1 + (stressLevel / maxStressLevel));

            // Calculate velocity based on deltaTime
            float velocityX = (cameraX - prevCameraX) / deltaTime;
            float velocityY = (cameraY - prevCameraY) / deltaTime;

            // Update previous position
            prevCameraX = cameraX;
            prevCameraY = cameraY;
        }
    }

    private void manageLight() {
        if (shouldAddLight()) {
            // Remove light when the player moves
            LightSystem.removeOldestLights();
        } else {
            // Add light if the player is stationary
            LightSystem.addLightSource(this.posX, this.posY, 4);
        }
    }

    private boolean shouldAddLight() {
        // Check if the camera position has changed
        return cameraX != prevCameraX || cameraY != prevCameraY;
    }

    private void getMouse() {
        float[] mousePos = GameFactory.mouseUtils.getMousePosition();
        openglMousePos = MouseUtils.convertToOpenGLCoordinatesOffset(mousePos[0], mousePos[1],
                (int) SettingsInfoParser.windowWidth, (int) SettingsInfoParser.windowHeight, cameraX, cameraY);
    }

    public void render(BatchRenderer batchRenderer) {
        getMouse();
        Vector3f center = WindowUtils.getCenter();
        batchRenderer.addTexturePos(TextureSystem.getTexture("playerTexture"), center.x, center.y, 1.1f, openglMousePos[0],
                openglMousePos[1], 1, 1, Color.toFloatArray(Color.WHITE));
    }

    private void handleCollisions() {
        int collisionType = 0;
        for (int j = 1; j > -2; j--) {
            float k = (float) j * GameInfoParser.playerSize;
            for (int i = Tile.extentTop.size() - 1; i >= 0; i--) {
                if ((cameraY + k <= Tile.extentTop.get(i)) && (cameraY + k >= Tile.extentCenterY.get(i))) {
                    if ((cameraX + k >= Tile.extentLeft.get(i)) && !(cameraX + k >= Tile.extentCenterX.get(i))) {
                        for(int p = MapInfoParser.doCollide.size()-1; p >=0; p--) {
                            if (Tile.mapDataType.get(i) == MapInfoParser.doCollide.get(p)) {
                                cameraY += 0.02f;
                                break;
                            }
                            if(Tile.mapDataType.get(i) == '1'){
                                collisionType = 1;
                                break;
                            }
                        }
                    } else if ((cameraX + k <= Tile.extentRight.get(i)) && !(cameraX + k <= Tile.extentCenterX.get(i))) {
                        for(int p = MapInfoParser.doCollide.size()-1; p >=0; p--) {
                            if (Tile.mapDataType.get(i) == MapInfoParser.doCollide.get(p)) {
                                cameraY += 0.02f;
                                break;
                            }
                            if(Tile.mapDataType.get(i) == '1'){
                                collisionType = 1;
                                break;
                            }
                        }
                    }
                }
                if ((cameraY + k >= Tile.extentBottom.get(i)) && (cameraY + k <= Tile.extentCenterY.get(i))) {
                    if ((cameraX + k >= Tile.extentLeft.get(i)) && !(cameraX + k >= Tile.extentCenterX.get(i))) {
                        for(int p = MapInfoParser.doCollide.size()-1; p >=0; p--) {
                            if (Tile.mapDataType.get(i) == MapInfoParser.doCollide.get(p)) {
                                cameraY -= 0.02f;
                                break;
                            }
                            if(Tile.mapDataType.get(i) == '1'){
                                collisionType = 1;
                                break;
                            }
                        }
                    } else if ((cameraX + k <= Tile.extentRight.get(i)) && !(cameraX + k <= Tile.extentCenterX.get(i))) {
                        for (int p = MapInfoParser.doCollide.size()-1; p >= 0; p--) {
                            if (Tile.mapDataType.get(i) == MapInfoParser.doCollide.get(p)) {
                                cameraY -= 0.02f;
                                break;
                            }
                            if(Tile.mapDataType.get(i) == '1'){
                                collisionType = 1;
                                break;
                            }
                        }
                    }
                }
                if ((cameraX + k <= Tile.extentRight.get(i)) && (cameraX + k >= Tile.extentCenterX.get(i))) {
                    if ((cameraY + k >= Tile.extentBottom.get(i)) && !(cameraY + k > Tile.extentCenterY.get(i))) {
                        for(int p = MapInfoParser.doCollide.size()-1; p >=0; p--) {
                            if (Tile.mapDataType.get(i) == MapInfoParser.doCollide.get(p)) {
                                cameraX += 0.02f;
                                break;
                            }
                            if(Tile.mapDataType.get(i) == '1'){
                                collisionType = 1;
                                break;
                            }
                        }
                    } else if ((cameraY + k <= Tile.extentTop.get(i)) && !(cameraY + k <= Tile.extentCenterY.get(i))) {
                        for (int p = MapInfoParser.doCollide.size()-1; p >= 0; p--) {
                            if (Tile.mapDataType.get(i) == MapInfoParser.doCollide.get(p)) {
                                cameraX += 0.02f;
                                break;
                            }
                            if(Tile.mapDataType.get(i) == '1'){
                                collisionType = 1;
                                break;
                            }
                        }
                    }
                }
                if ((cameraX + k >= Tile.extentLeft.get(i)) && (cameraX + k <= Tile.extentCenterX.get(i))) {
                    if ((cameraY + k >= Tile.extentBottom.get(i)) && !(cameraY + k >= Tile.extentCenterY.get(i))) {
                        for(int p = MapInfoParser.doCollide.size()-1; p >=0; p--) {
                            if (Tile.mapDataType.get(i) == MapInfoParser.doCollide.get(p)) {
                                cameraX -= 0.02f;
                                break;
                            }
                            if(Tile.mapDataType.get(i) == '1'){
                                collisionType = 1;
                                break;
                            }
                        }
                    } else if ((cameraY + k <= Tile.extentTop.get(i)) && !(cameraY + k <= Tile.extentCenterY.get(i))) {
                        for(int p = MapInfoParser.doCollide.size()-1; p >=0; p--) {
                            if (Tile.mapDataType.get(i) == MapInfoParser.doCollide.get(p)) {
                                cameraX -= 0.02f;
                                break;
                            }
                            if(Tile.mapDataType.get(i) == '1'){
                                collisionType = 1;
                                break;
                            }
                        }
                    }
                }
            }
        }
        if (collisionType == 1) {
            currentCameraSpeed = 0.010f; // Slow down when colliding
        }
    }

    private void processInput() {
        handleSprinting();
        handleCollisions();
        handleMovement();
        handleAttack();

        // Update cameraZ based on the scroll input
        cameraZ = MathUtils.clamp(cameraZ + scrollOffset * scrollSpeed, GameInfoParser.minZoom, GameInfoParser.maxZoom);
        scrollOffset = 0.0f;
    }

    private void handleSprinting() {
        if (GameFactory.mouseUtils.isKeyPressed(KeyInfoParser.getKeyAsGLWFBind("keySprint"))) {
            if (stressLevel < maxStressLevel) {
                currentCameraSpeed = 0.1f; // Sprinting speed
                isSprinting = true;
                increaseStress(5); // Increase stress while sprinting
            } else {
                isSprinting = false; // Prevent sprinting if stressed
            }
        } else {
            isSprinting = false;
            currentCameraSpeed = baseCameraSpeed; // Reset speed
        }
    }

    private void handleMovement() {
        if (!GameEngine.menu) {
            if (GameFactory.mouseUtils.isKeyPressed(KeyInfoParser.getKeyAsGLWFBind("keyWalkLeft"))) forward(false, 1);
            if (GameFactory.mouseUtils.isKeyPressed(KeyInfoParser.getKeyAsGLWFBind("keyWalkRight"))) forward(false, -1);
            if (GameFactory.mouseUtils.isKeyPressed(KeyInfoParser.getKeyAsGLWFBind("keyWalkForward"))) forward(true, 1);
            if (GameFactory.mouseUtils.isKeyPressed(KeyInfoParser.getKeyAsGLWFBind("keyWalkBackward"))) forward(true, -1);
        }
    }

    private void handleAttack() {
        if (GameFactory.mouseUtils.isMouseButtonPressed(1)) {
            GameFactory.soundSystem.play(SoundSystem.getSound("Sample"));
            Logger.printLOG("Player is attacking...");
            attack();
        }
    }

    // Method to update position of the player
    private void updatePos(float x, float y, float z) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
    }

    // New methods to manage stress
    private void increaseStress(float amount) {
        stressLevel = MathUtils.clamp(stressLevel + amount, 0, (int) maxStressLevel);
        Logger.printLOG("Stress Level: " + stressLevel);
    }

    private void decreaseStress(float amount) {
        stressLevel = MathUtils.clamp(stressLevel - amount, 0, (int) maxStressLevel);
        Logger.printLOG("Stress Level: " + stressLevel);
    }
}
