package cs.System.Text.RegularExpressions;

public class MatchEvaluator extends cs.System.Delegate {
    public String Invoke(Match m) { if ($list != null) { String r = null; for (cs.System.Delegate d : $list) r = ((MatchEvaluator) d).$call(m); return r; } return $call(m); }
    protected String $call(Match m) { throw new IllegalStateException("unbound delegate"); }
    protected cs.System.Delegate $empty() { return new MatchEvaluator(); }
}
