{
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";
    flake-parts.url = "github:hercules-ci/flake-parts";
    android-nixpkgs = {
      url = "github:tadfisher/android-nixpkgs";
      inputs.nixpkgs.follows = "nixpkgs";
    };
  };

  outputs = inputs:
    inputs.flake-parts.lib.mkFlake { inherit inputs; } {
      systems = [ "aarch64-darwin" "x86_64-darwin" "aarch64-linux" "x86_64-linux" ];

      perSystem = { pkgs, system, ... }:
        let
          androidSdk = inputs.android-nixpkgs.sdk.${system} (sdkPkgs: with sdkPkgs; [
            build-tools-35-0-0
            cmdline-tools-latest
            platform-tools
            platforms-android-35
          ]);
        in
        {
          devShells.default = pkgs.mkShell {
            buildInputs = [
              androidSdk
              pkgs.jdk17
              pkgs.gradle
              pkgs.just
            ];

            ANDROID_HOME = "${androidSdk}/share/android-sdk";
            JAVA_HOME = "${pkgs.jdk17}";

            shellHook = ''
              echo "🔧 Appreciate dev shell"
              echo "   Java: $(java -version 2>&1 | head -1)"
              echo "   ANDROID_HOME: $ANDROID_HOME"
              echo ""
              echo "   Build:  cd android && ./gradlew assembleDebug"
              echo "   Deploy: adb install android/app/build/outputs/apk/debug/app-debug.apk"
            '';
          };
        };
    };
}
