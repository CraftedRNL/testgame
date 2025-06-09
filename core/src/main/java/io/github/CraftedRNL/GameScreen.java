package io.github.CraftedRNL;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;

import com.badlogic.gdx.utils.Timer;

public class GameScreen implements Screen {
    private final Main game;
    private SpriteBatch batch;
    private FitViewport viewport;

    private Texture gearTexture;
    private Texture bulbTexture;
    private Texture litBulbTexture;
    private Texture steampunk;
    private Sprite gearSprite;

    private float angularVelocity = 0;
    private float momentOfInertia = 21.98f;
    private int appliedForce;
    private float kineticEnergy = 0;
    private final float radius = 1f;

    private boolean bulbLit = false;
    private float elapsedTime = 0f;
    private int ePoints;
    private int eMult;
    private int gearCount;
    private Material currentMaterial = Material.WOOD;
    private boolean rotating = false;
    private Timer.Task energyTimerTask;
    private boolean timerRunning;
    private BitmapFont font;

    private String wheel ="woodWheel.png";
    public GameScreen(Main game) {
        this.game = game;
    }

    public GameScreen(Main game, Material changeMaterial) {
        this.game = game;
        this.ePoints = game.getEPoints();
        this.gearCount = game.getGearCount();
        game.setMaterial(changeMaterial);
        this.currentMaterial = game.getMaterial();

        updateMaterialProperties();
        updateMaterial(changeMaterial);
        this.appliedForce = 10 + (gearCount * 10);
    }
    private void updateMaterialProperties() {

        currentMaterial = game.getMaterial();

        switch (currentMaterial) {
            case WOOD:
                eMult = 1;
                wheel = "woodWheel.png";
                break;
            case PLASTIC:
                eMult = 2;
                wheel = "plasticWheel.png";
                break;
            case IRON:
                eMult = 5;
                wheel = "ironWheel.png";
                break;
            case GOLD:
                eMult = 10;
                wheel = "goldWheel.png";
                break;
            case OSMIUM:
                eMult = 15;
                wheel = "osmiumWheel.png";
                break;
        }
    }
    private void goToShopScreen(){
        game.setEPoints(ePoints);
        game.setGearCount(gearCount);
        game.setScreen(new ShopScreen(game));
    }
    public void show(){
        this.gearCount = game.getGearCount();
        appliedForce = 10 + (gearCount * 10);
        batch = new SpriteBatch();
        gearTexture = new Texture(wheel);
        bulbTexture = new Texture("bulb.png");
        viewport = new FitViewport(10,5);
        steampunk = new Texture("steampunk.jpg");
        litBulbTexture = new Texture("litBulb.png");

        gearSprite = new Sprite(gearTexture);
        gearSprite.setSize(2,2);
        gearSprite.setOriginCenter();

        font = new BitmapFont(Gdx.files.internal("MyFont.fnt"));
        font.setUseIntegerPositions(false);
        updateMaterialProperties();
    }


    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }


    @Override
    public void render(float delta) {

        handleInput();
        updatePhysics();
        updateLogic();
        draw();
    }

    public void handleInput() {
        rotating = false;

        if(Gdx.input.isKeyPressed(Input.Keys.RIGHT)){
            applyTorque(false);
            rotating = true;

        }else if(Gdx.input.isKeyPressed(Input.Keys.LEFT)){
            applyTorque(true);
            rotating = true;

        }

        if(Gdx.input.isKeyJustPressed(Input.Keys.M)) goToShopScreen();
        if(Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ADD)) ePoints+=10000;

    }
    private void applyTorque(boolean clockwise){


        float torque = appliedForce * radius;
        float angularAcceleration = torque / momentOfInertia;
        elapsedTime += Gdx.graphics.getDeltaTime();

        if(clockwise){
            angularVelocity = angularAcceleration * elapsedTime;
        }else{
            angularVelocity = -angularAcceleration * elapsedTime;
        }



    }
    public void updatePhysics(){
         kineticEnergy = 0.5f * momentOfInertia * angularVelocity * angularVelocity;


        if(!rotating) {
            angularVelocity *= 0.99f;
            elapsedTime = 0f;
            if(Math.abs(angularVelocity) < 0.05f) {
                angularVelocity = 0f;

            }
        }
    }
    public void updateLogic() {
        float energyThreshold = 60f;
        bulbLit = kineticEnergy >= energyThreshold;


        if(bulbLit && !timerRunning) {
            startTimer();
            timerRunning = true;
        }else if(!bulbLit && timerRunning) {
            stopTimer();
            timerRunning = false;
        }
    }
    public void startTimer() {
        if (energyTimerTask != null) {
            energyTimerTask.cancel();
        }

        energyTimerTask = new Timer.Task() {
            @Override
            public void run() {
                ePoints += (int) (kineticEnergy/100 * eMult);
            }
        };
        Timer.schedule(energyTimerTask, 0, 1);
        timerRunning = true;
    }

    public void stopTimer() {
        if (energyTimerTask != null) {
            energyTimerTask.cancel();
        }
        timerRunning = false;
    }
    private void updateMaterial(Material material){
        float thickness = 0.02f;
        float volume = (float)(Math.PI * radius * radius * thickness);
        float mass = material.density * volume;

        momentOfInertia = 0.5f * mass * radius * radius;
        momentOfInertia = Math.round(momentOfInertia * 100f)/100f;
    }



    public void draw() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        viewport.apply();

        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();

        float worldWidth = viewport.getWorldWidth();
        float worldHeight = viewport.getWorldHeight();
        batch.draw(steampunk, 0, 0, worldWidth, worldHeight);

        gearSprite.setPosition(worldWidth/2 - gearSprite.getWidth()/2, 0);
        gearSprite.rotate(angularVelocity * Gdx.graphics.getDeltaTime() * 180f/(float)Math.PI);
        gearSprite.draw(batch);

        if(bulbLit) {
            batch.draw(litBulbTexture, worldWidth/2 - 0.5f, worldHeight/2 , 1, 1);
        }else {
            batch.draw(bulbTexture, worldWidth/2 - 0.5f, worldHeight/2, 1, 1);
        }

        batch.setColor(Color.WHITE);
        batch.end();


        batch.begin();
        float graphicsHeight = Gdx.graphics.getHeight();
        batch.setProjectionMatrix(batch.getProjectionMatrix()
            .setToOrtho2D(0, 0, Gdx.graphics.getWidth(), graphicsHeight));


        font.setColor(Color.WHITE);
        font.draw(batch, "Material: " + game.getMaterial().name, 25, graphicsHeight/1.5f - 30);
        font.draw(batch, "E-Points: " + ePoints, 25, graphicsHeight/1.5f-60);
        font.draw(batch, "Force: " + appliedForce, 25, graphicsHeight/1.5f-90);
        font.draw(batch, "Inertia: " + momentOfInertia, 25, graphicsHeight/1.5f-120);
        font.draw(batch, "Omega: " + formatFloat(angularVelocity, 2), 25, graphicsHeight/1.5f-180);
        font.draw(batch, "KE: " + formatFloat(kineticEnergy, 2), 25, graphicsHeight/1.5f - 210);
        font.draw(batch, "Time: " + formatFloat(elapsedTime, 2), 25, graphicsHeight/1.5f-240);
        batch.end();
    }
    private String formatFloat(float value, int decimalPlaces) {
        int factor = (int) Math.pow(10, decimalPlaces);
        float rounded = Math.round(value * factor) / (float) factor;
        return String.valueOf(rounded);
    }
    @Override
    public void dispose() {
        if (energyTimerTask != null) {
            energyTimerTask.cancel();
        }
        batch.dispose();
        gearTexture.dispose();
        bulbTexture.dispose();
        steampunk.dispose();
        litBulbTexture.dispose();
    }


    @Override
    public void pause() {

    }
    @Override
    public void resume() {

    }
    @Override
    public void hide() {

    }
}
