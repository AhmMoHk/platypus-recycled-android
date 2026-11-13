// Platypus Reclayed Android port: the time a ship cannot be hurt after taking or losing a power-up. A ship that
// asks for more than 4 seconds (the Purist of "Casinopolis Ships" asks for 8 on every difficulty) gets the game's
// own standard instead, the time the other ships have: after a power-up 2 s (Easy, Regular, Hard) or none (Expert,
// Nasty), after losing one 2 s.
using Platypus.Core;

namespace PlatypusAndroid
{
    public static class ShipRules
    {
        const int Longest = 4;
        static readonly DiffDict<int> StandardPowerUp = new DiffDict<int>(2, 2, 2, 0, 0);
        static readonly DiffDict<int> StandardPowerDown = new DiffDict<int>(2, 2, 2, 2, 2);

        public static float PowerUp(ShipData s) => Pick(s?.invincibilityPowerUp, StandardPowerUp);

        public static float PowerDown(ShipData s) => Pick(s?.invincibilityPowerDown, StandardPowerDown);

        static float Pick(DiffDict<int> own, DiffDict<int> standard)
        {
            GameDifficulty d = Globals.Difficulty;
            int v = own != null ? own.Get(d) : standard.Get(d);
            return v > Longest ? standard.Get(d) : v;
        }
    }
}
