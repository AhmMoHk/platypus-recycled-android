package cs.UnityEngine;

import java.util.ArrayList;

/** RigidbodyType2D: Dynamic 0, Kinematic 1, Static 2. Only kinematic movement is simulated (positions come from transforms). */
public class Rigidbody2D extends Component {
    int bodyType;
    boolean simulated = true, useFullKinematicContacts, useAutoMass;
    float mass = 1, drag, angularDrag = .05f, gravityScale = 1, angularVelocity;
    int collisionDetectionMode, interpolation, sleepMode = 1, constraints;
    final Vector2 velocity = new Vector2();
    Vector2 movePos;
    Float moveRot;
    static final ArrayList<Rigidbody2D> moving = new ArrayList<>();

    public int get_bodyType() { return bodyType; }
    public void set_bodyType(int t) { bodyType = t; }
    public boolean get_isKinematic() { return bodyType == 1; }
    public void set_isKinematic(boolean v) { bodyType = v ? 1 : 0; }
    public boolean get_simulated() { return simulated; }
    public void set_simulated(boolean v) { simulated = v; }
    public boolean get_useFullKinematicContacts() { return useFullKinematicContacts; }
    public void set_useFullKinematicContacts(boolean v) { useFullKinematicContacts = v; }
    public int get_collisionDetectionMode() { return collisionDetectionMode; }
    public void set_collisionDetectionMode(int m) { collisionDetectionMode = m; }
    public int get_interpolation() { return interpolation; }
    public void set_interpolation(int m) { interpolation = m; }
    public int get_sleepMode() { return sleepMode; }
    public void set_sleepMode(int m) { sleepMode = m; }
    public int get_constraints() { return constraints; }
    public void set_constraints(int c) { constraints = c; }
    public float get_mass() { return mass; }
    public void set_mass(float m) { mass = m; }
    public float get_gravityScale() { return gravityScale; }
    public void set_gravityScale(float g) { gravityScale = g; }
    public float get_drag() { return drag; }
    public void set_drag(float d) { drag = d; }
    public Vector2 get_velocity() { return velocity.$copy(); }
    public void set_velocity(Vector2 v) { velocity.$set(v); }
    public float get_angularVelocity() { return angularVelocity; }
    public void set_angularVelocity(float v) { angularVelocity = v; }
    public boolean get_useAutoMass() { return useAutoMass; }
    public void set_useAutoMass(boolean v) { useAutoMass = v; }
    public Vector2 get_position() { Vector3 p = gameObject.transform.get_position(); return new Vector2(p.x, p.y); }
    public void set_position(Vector2 p) { Vector3 c = gameObject.transform.get_position(); gameObject.transform.set_position(new Vector3(p.x, p.y, c.z)); }
    public float get_rotation() { return gameObject.transform.get_eulerAngles().z; }
    public void set_rotation(float r) { gameObject.transform.set_eulerAngles(new Vector3(0, 0, r)); }
    public void MovePosition(Vector2 p) { movePos = p.$copy(); if (!moving.contains(this)) moving.add(this); }
    public void MoveRotation(float r) { moveRot = r; if (!moving.contains(this)) moving.add(this); }
    public void AddForce(Vector2 f) {}
    public void AddForce(Vector2 f, int mode) {}
    public void AddTorque(float t) {}
    public boolean IsAwake() { return true; }
    public boolean IsSleeping() { return false; }
    public void WakeUp() {}
    public void Sleep() {}

    static void applyMoves() {
        for (Rigidbody2D r : moving) {
            if (r.$destroyed || r.gameObject == null) continue;
            if (r.movePos != null) r.set_position(r.movePos);
            if (r.moveRot != null) r.set_rotation(r.moveRot);
            r.movePos = null;
            r.moveRot = null;
        }
        moving.clear();
    }

    @Override public void $load(java.util.HashMap<String, Object> d, Loader.Ctx ctx) {
        bodyType = Loader.i(d, "m_BodyType", 0);
        simulated = Loader.b(d, "m_Simulated", true);
        useFullKinematicContacts = Loader.b(d, "m_UseFullKinematicContacts", false);
        useAutoMass = Loader.b(d, "m_UseAutoMass", false);
        mass = Loader.f(d, "m_Mass", 1);
        drag = Loader.f(d, "m_LinearDrag", 0);
        angularDrag = Loader.f(d, "m_AngularDrag", .05f);
        gravityScale = Loader.f(d, "m_GravityScale", 1);
        interpolation = Loader.i(d, "m_Interpolate", 0);
        sleepMode = Loader.i(d, "m_SleepingMode", 1);
        collisionDetectionMode = Loader.i(d, "m_CollisionDetection", 0);
        constraints = Loader.i(d, "m_Constraints", 0);
    }
    @Override public void $cloneFrom(Component src, Eng.CloneCtx ctx) {
        Rigidbody2D s = (Rigidbody2D) src;
        bodyType = s.bodyType; simulated = s.simulated; useFullKinematicContacts = s.useFullKinematicContacts; useAutoMass = s.useAutoMass;
        mass = s.mass; drag = s.drag; angularDrag = s.angularDrag; gravityScale = s.gravityScale; interpolation = s.interpolation;
        sleepMode = s.sleepMode; collisionDetectionMode = s.collisionDetectionMode; constraints = s.constraints;
    }
}
