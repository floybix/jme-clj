;; Please start your REPL with `+test` profile
(ns examples.fps.client
  (:require
   [examples.fps.app-states :as app.states]
   [examples.fps.controls :as controls]
   [jme-clj.core :refer :all])
  (:import
   (com.jme3.math ColorRGBA)
   (com.jme3.post FilterPostProcessor)
   (com.jme3.terrain.heightmap HillHeightMap)
   (com.jme3.texture Texture$WrapMode)
   (org.jme.filter ColorScaleFilter)))


(defn- create-player []
  (let [player (character-control (capsule-collision-shape 3 7 1) 0.05)]
    (setc player
          :jump-speed 20
          :fall-speed 30
          :gravity 30
          :physics-location (vec3 -500 0 0))))


(defn- create-material []
  (let [grass (set* (load-texture "Textures/Terrain/splat/grass.jpg") :wrap Texture$WrapMode/Repeat)
        dirt  (set* (load-texture "Textures/Terrain/splat/dirt.jpg") :wrap Texture$WrapMode/Repeat)
        rock  (set* (load-texture "Textures/Terrain/splat/road.jpg") :wrap Texture$WrapMode/Repeat)]
    (-> (material "Common/MatDefs/Terrain/Terrain.j3md")
        (set* :texture "Alpha" (load-texture "Textures/Terrain/splat/alphamap.png"))
        (set* :texture "Tex1" grass)
        (set* :float "Tex1Scale" (float 64))
        (set* :texture "Tex2" dirt)
        (set* :float "Tex2Scale" (float 32))
        (set* :texture "Tex3" rock)
        (set* :float "Tex3Scale" (float 128)))))


(defn- create-terrain [mat]
  (let [_          (set! (HillHeightMap/NORMALIZE_RANGE) 100)
        height-map (hill-height-map 513 1000 50 100 (byte 3))
        _          (call* height-map :load)
        patch-size 65
        terrain    (terrain-quad "my terrain" patch-size 513 (get-height-map height-map))]
    (-> terrain
        (setc :material mat
              :local-translation [0 -100 0]
              :local-scale [2 1 2])
        (add-control (terrain-lod-control terrain (cam))))))


(defn- add-lights []
  (let [sun     (-> (light :directional)
                    (setc :direction (vec3 -0.5 -0.5 -0.5)
                          :color ColorRGBA/White))
        sun-b   (-> (light :directional)
                    (setc :direction (vec3 0.5 0.5 0.5)
                          :color ColorRGBA/White))
        ambient (-> (light :ambient)
                    (set* :color ColorRGBA/White))]
    (add-light-to-root sun)
    (add-light-to-root sun)
    (add-light-to-root sun-b)
    (add-light-to-root sun-b)
    (add-light-to-root ambient)))


(defn init-audio []
  (-> (audio-node "Sound/Effects/Gun.wav" :buffer)
      (setc :positional false
            :looping false)
      (add-to-root)))


(defn- generate-rand-location []
  (let [x (if (= 0 (rand-int 2)) 1 -1)
        z (if (= 0 (rand-int 2)) 1 -1)]
    (vec3 (* (rand 500) x) 0 (* (rand 500) z))))


(defn create-models [node bas]
  (dotimes [i 50]
    (let [player  (setc (character-control (capsule-collision-shape 3 3.5 1) 0.05)
                        :jump-speed 20
                        :fall-speed 30
                        :gravity 30)
          spatial (load-model "Models/Oto/Oto.mesh.xml")]
      (-> spatial
          (set* :user-data "name" (str "model_" i))
          (set* :user-data "hp" 100)
          (add-control player)
          (#(attach-child node %)))
      (-> bas
          (get* :physics-space)
          (call* :add player))
      (set* player :physics-location (generate-rand-location))))
  node)


(defn- create-damage-filter []
  (let [fpp (set* (FilterPostProcessor. (asset-manager)) :num-samples 4)
        cs  (ColorScaleFilter.)]
    (doto fpp (.addFilter cs))))


(defn init []
  (set* (fly-cam) :move-speed 100)
  (let [bas           (attach (bullet-app-state))
        mat           (create-material)
        terrain       (create-terrain mat)
        terrain-shape (create-mesh-shape terrain)
        landscape     (rigid-body-control terrain-shape 0)
        player        (create-player)
        spatial       (load-model "Models/Oto/Oto.mesh.xml")
        shootables    (node "shootables")]
    (add-to-root (create-models shootables bas))
    (add-lights)
    (attach-all (app.states/create-hp)
                (app.states/create-cross-hairs))
    (add-to-root (create-sky "Textures/Sky/Bright/BrightSky.dds" :cube))
    (-> spatial
        (add-control player)
        (add-control (controls/create-user-input player terrain))
        (add-to-root)
        (cull-hint :always))
    (-> terrain
        (add-control landscape)
        (add-to-root))
    (-> bas
        (get* :physics-space)
        (call* :add landscape))
    (-> bas
        (get* :physics-space)
        (call* :add-all spatial))
    {:player-data      {:hp   100
                        :name "ertu"}
     :bullet-app-state bas
     :player           player
     :spatial          spatial
     :terrain          terrain
     :audio            (init-audio)
     :shootables       shootables
     :damage-filter    (create-damage-filter)}))


(defn- simple-update [tpf]
  )


(defsimpleapp app
              :opts {:show-settings?       false
                     :pause-on-lost-focus? false
                     :display-stat-view?   false
                     :settings             {:title          "My JME Game"
                                            :load-defaults? true
                                            :frame-rate     60
                                            :width          1200
                                            :height         800}}
              :init init
              :update simple-update)


(comment
 (start app)

 (run app
      (re-init init))

 ;;after calling unbind-app, we need to re-define the app with defsimpleapp
 (unbind-app #'app)
 )

