package cs.Rewired;

public class ReInput$PlayerHelper extends cs.Rewired.Utils.Classes.CodeHelper {
    public int get_playerCount() { return ReInput.players.get_Count(); }
    public int get_allPlayerCount() { return ReInput.allPlayersList.get_Count(); }
    public cs.System.Collections.Generic.IList_1 get_Players() { return ReInput.players; }
    public cs.System.Collections.Generic.IList_1 get_AllPlayers() { return ReInput.allPlayersList; }
    public Player get_SystemPlayer() { return ReInput.system; }
    public Player GetSystemPlayer() { return ReInput.system; }
    /** only player 0 exists (single player); other ids return a player that never receives input */
    public Player GetPlayer(int id) {
        if (id == ReInput.system.id) return ReInput.system;
        if (id == 0) return ReInput.p0;
        return ReInput.ghost(id);
    }
    public Player GetPlayer(String name) { return "SYSTEM".equals(name) ? ReInput.system : ReInput.p0; }
}
