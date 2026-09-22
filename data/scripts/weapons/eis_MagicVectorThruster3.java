package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import java.awt.Color;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public class eis_MagicVectorThruster3 implements EveryFrameWeaponEffectPlugin {
    private boolean runOnce = false;
    private boolean accel = false;
    private boolean turn = false;
    private ShipAPI SHIP;
    private ShipEngineAPI thruster;
    private ShipEngineControllerAPI EMGINES;
    private float time = 0.0F;
    private float previousThrust = 0.0F;
    private final float FREQ = 0.05F;
    private final float SMOOTH_THRUSTING = 0.25F;
    private float TURN_RIGHT_ANGLE = 0.0F;
    private float THRUST_TO_TURN = 0.0F;
    private float NEUTRAL_ANGLE = 0.0F;
    private float FRAMES = 0.0F;
    private float OFFSET = 0.0F;
    private Vector2f size = new Vector2f(10.0F, 75.0F);

    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (!this.runOnce) {
            this.runOnce = true;
            this.SHIP = weapon.getShip();
            this.EMGINES = this.SHIP.getEngineController();
            if (weapon.getAnimation() != null) {
                this.FRAMES = weapon.getAnimation().getNumFrames();
            }

            for (ShipEngineAPI e : this.SHIP.getEngineController().getShipEngines()) {
                if (MathUtils.isWithinRange(e.getLocation(), weapon.getLocation(), 2.0F)) {
                    this.thruster = e;
                }
            }

            this.OFFSET = (float)(Math.random() * (float) Math.PI);
            this.NEUTRAL_ANGLE = weapon.getSlot().getAngle();
            this.TURN_RIGHT_ANGLE = MathUtils.clampAngle(VectorUtils.getAngle(this.SHIP.getLocation(), weapon.getLocation()));
            this.TURN_RIGHT_ANGLE = MathUtils.getShortestRotation(this.SHIP.getFacing(), this.TURN_RIGHT_ANGLE) + 90.0F;
            this.THRUST_TO_TURN = this.smooth(MathUtils.getDistance(this.SHIP.getLocation(), weapon.getLocation()) / this.SHIP.getCollisionRadius());
        }

        if (!engine.isPaused() && this.SHIP.getOriginalOwner() != -1) {
            if (this.SHIP.isAlive() && (this.thruster == null || !this.thruster.isDisabled())) {
                this.time += amount;
                if (this.time >= 0.05F) {
                    this.time = 0.0F;
                    float accelerateAngle = this.NEUTRAL_ANGLE;
                    float turnAngle = this.NEUTRAL_ANGLE;
                    float thrust = 0.0F;
                    if (this.EMGINES.getFlameColorShifter().getCurr() != this.EMGINES.getFlameColorShifter().getBase()) {
                        weapon.getSprite().setColor(this.EMGINES.getFlameColorShifter().getCurr());
                    }

                    if (this.EMGINES.isAccelerating()) {
                        accelerateAngle = 180.0F;
                        thrust = 1.5F;
                        this.accel = true;
                    } else if (this.EMGINES.isAcceleratingBackwards()) {
                        accelerateAngle = 0.0F;
                        thrust = 1.5F;
                        this.accel = true;
                    } else if (this.EMGINES.isDecelerating()) {
                        accelerateAngle = this.NEUTRAL_ANGLE;
                        thrust = 0.5F;
                        this.accel = true;
                    } else {
                        this.accel = false;
                    }

                    if (this.EMGINES.isStrafingLeft()) {
                        if (thrust == 0.0F) {
                            accelerateAngle = -90.0F;
                        } else {
                            accelerateAngle += MathUtils.getShortestRotation(accelerateAngle, -90.0F) / 2.0F;
                        }

                        thrust = Math.max(1.0F, thrust);
                        this.accel = true;
                    } else if (this.EMGINES.isStrafingRight()) {
                        if (thrust == 0.0F) {
                            accelerateAngle = 90.0F;
                        } else {
                            accelerateAngle += MathUtils.getShortestRotation(accelerateAngle, 90.0F) / 2.0F;
                        }

                        thrust = Math.max(1.0F, thrust);
                        this.accel = true;
                    }

                    if (this.EMGINES.isTurningRight()) {
                        turnAngle = this.TURN_RIGHT_ANGLE;
                        thrust = Math.max(1.0F, thrust);
                        this.turn = true;
                    } else if (this.EMGINES.isTurningLeft()) {
                        turnAngle = MathUtils.clampAngle(180.0F + this.TURN_RIGHT_ANGLE);
                        thrust = Math.max(1.0F, thrust);
                        this.turn = true;
                    } else {
                        this.turn = false;
                    }

                    if (thrust > 0.0F) {
                        Vector2f offset = new Vector2f(weapon.getLocation().x - this.SHIP.getLocation().x, weapon.getLocation().y - this.SHIP.getLocation().y);
                        VectorUtils.rotate(offset, -this.SHIP.getFacing(), offset);
                        if (!this.turn) {
                            if (this.FRAMES == 0.0F) {
                                this.rotate(weapon, accelerateAngle, thrust * this.SHIP.getMutableStats().getAcceleration().computeMultMod(), 0.25F);
                            } else {
                                this.thrust(weapon, accelerateAngle, thrust * this.SHIP.getMutableStats().getAcceleration().computeMultMod(), 0.25F);
                            }
                        } else if (!this.accel) {
                            if (this.FRAMES == 0.0F) {
                                this.rotate(weapon, turnAngle, thrust * this.SHIP.getMutableStats().getTurnAcceleration().computeMultMod(), 0.25F);
                            } else {
                                this.thrust(weapon, turnAngle, thrust * this.SHIP.getMutableStats().getTurnAcceleration().computeMultMod(), 0.25F);
                            }
                        } else {
                            float clampedThrustToTurn = this.THRUST_TO_TURN * Math.min(1.0F, Math.abs(this.SHIP.getAngularVelocity()) / 10.0F);
                            clampedThrustToTurn = this.smooth(clampedThrustToTurn);
                            float combinedAngle = this.NEUTRAL_ANGLE;
                            combinedAngle = MathUtils.clampAngle(combinedAngle + MathUtils.getShortestRotation(this.NEUTRAL_ANGLE, accelerateAngle));
                            combinedAngle = MathUtils.clampAngle(combinedAngle + clampedThrustToTurn * MathUtils.getShortestRotation(accelerateAngle, turnAngle));
                            float combinedThrust = thrust
                                * (
                                    (this.SHIP.getMutableStats().getTurnAcceleration().computeMultMod() + this.SHIP.getMutableStats().getAcceleration().computeMultMod())
                                        / 2.0F
                                );
                            float offAxis = Math.abs(MathUtils.getShortestRotation(turnAngle, accelerateAngle));
                            offAxis = Math.max(0.0F, offAxis - 90.0F);
                            offAxis /= 45.0F;
                            combinedThrust *= 1.0F - Math.max(0.0F, Math.min(1.0F, offAxis));
                            if (this.FRAMES == 0.0F) {
                                this.rotate(weapon, combinedAngle, combinedThrust, 0.125F);
                            } else {
                                this.thrust(weapon, combinedAngle, combinedThrust, 0.125F);
                            }
                        }
                    } else if (this.FRAMES == 0.0F) {
                        this.rotate(weapon, this.NEUTRAL_ANGLE, 0.0F, 0.25F);
                    } else {
                        this.thrust(weapon, this.NEUTRAL_ANGLE, 0.0F, 0.25F);
                    }
                }
            } else {
                weapon.getAnimation().setFrame(0);
                this.previousThrust = 0.0F;
            }
        }
    }

    private void rotate(WeaponAPI weapon, float angle, float thrust, float smooth) {
        float aim = angle + this.SHIP.getFacing();
        aim = MathUtils.getShortestRotation(weapon.getCurrAngle(), aim);
        aim = (float)(aim + 5.0 * FastTrig.cos(this.SHIP.getFullTimeDeployed() * 5.0F * thrust + this.OFFSET));
        aim *= smooth;
        weapon.setCurrAngle(MathUtils.clampAngle(weapon.getCurrAngle() + aim));
    }

    private void thrust(WeaponAPI weapon, float angle, float thrust, float smooth) {
        int frame = (int)(Math.random() * (this.FRAMES - 1.0F)) + 1;
        if (frame == weapon.getAnimation().getNumFrames()) {
            frame = 1;
        }

        weapon.getAnimation().setFrame(frame);
        SpriteAPI sprite = weapon.getSprite();
        float aim = angle + this.SHIP.getFacing();
        aim = MathUtils.getShortestRotation(weapon.getCurrAngle(), aim);
        float length = thrust * Math.max(0.0F, 1.0F - Math.abs(aim) / 90.0F);
        length -= this.previousThrust;
        length *= smooth;
        length += this.previousThrust;
        this.previousThrust = length;
        aim = (float)(aim + 5.0 * FastTrig.cos(this.SHIP.getFullTimeDeployed() * 5.0F * thrust + this.OFFSET));
        aim *= smooth;
        weapon.setCurrAngle(MathUtils.clampAngle(weapon.getCurrAngle() + aim));
        float width = length * this.size.x / 2.0F + this.size.x / 2.0F;
        float height = length * this.size.y + (float)Math.random() * 3.0F + 3.0F;
        sprite.setSize(width, height);
        sprite.setCenter(width / 2.0F, height / 2.0F);
        length = Math.max(0.0F, Math.min(1.0F, length));
        sprite.setColor(new Color(1.0F, 0.5F + length / 2.0F, 0.75F + length / 4.0F));
    }

    public float smooth(float x) {
        return 0.5F - (float)(Math.cos(x * (float) Math.PI) / 2.0);
    }
}
