package cs.UnityEngine;

public class AssetBundleRequest extends ResourceRequest {
    public UObject[] get_allAssets() { return asset == null ? new UObject[0] : new UObject[]{asset}; }
}
