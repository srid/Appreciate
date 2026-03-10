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

      perSystem = { pkgs, lib, system, ... }:
        let
          androidSdk = inputs.android-nixpkgs.sdk.${system} (sdkPkgs: with sdkPkgs; [
            build-tools-35-0-0
            cmdline-tools-latest
            platform-tools
            platforms-android-35
          ]);

          isLinux = lib.hasSuffix "-linux" system;

          # Linux app — Python + GTK4
          linuxApp = pkgs.stdenv.mkDerivation {
            pname = "appreciate";
            version = "0.1.0";
            src = lib.sources.sourceByRegex ./. [ "^linux.*" "^common.*" ];

            nativeBuildInputs = [
              pkgs.wrapGAppsHook4
              pkgs.gobject-introspection
            ];

            buildInputs = [
              pkgs.gtk4
              pkgs.gtk3  # for tray subprocess
              pkgs.glib
              pkgs.libayatana-appindicator  # tray icon
              (pkgs.python3.withPackages (ps: [ ps.pygobject3 ps.pycairo ]))
            ];

            installPhase = ''
              mkdir -p $out/bin $out/share/appreciate $out/share/applications
              cp linux/*.py $out/share/appreciate/
              cp linux/*.png $out/share/appreciate/ || true
              cp common/packs.json $out/share/appreciate/packs.json
              cp linux/appreciate.desktop $out/share/applications/ || true

              cat > $out/bin/appreciate <<WRAPPER
              #!/bin/sh
              exec ${pkgs.python3.withPackages (ps: [ ps.pygobject3 ps.pycairo ])}/bin/python3 $out/share/appreciate/appreciate.py "\$@"
              WRAPPER
              chmod +x $out/bin/appreciate
            '';

            meta.mainProgram = "appreciate";
          };
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
        } // lib.optionalAttrs isLinux {
          packages.linux = linuxApp;
          packages.default = linuxApp;
          apps.linux = { type = "app"; program = lib.getExe linuxApp; };
          apps.default = { type = "app"; program = lib.getExe linuxApp; };
        };
    };
}
