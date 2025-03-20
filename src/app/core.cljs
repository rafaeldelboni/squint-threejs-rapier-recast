(ns app.core
  (:require [three :refer [BoxGeometry MeshBasicMaterial Mesh Scene PerspectiveCamera WebGLRenderer]]
            ["three/addons/controls/OrbitControls.js" :refer [OrbitControls]]
            ["@dimforge/rapier3d-compat" :as rapier]))

(defn- prepare-container! [id]
  (let [container (js/document.getElementById id)]
    (set! (.-innerHTML container) "")
    container))

(def system
  (let [container (prepare-container! "app")
        inner-width (.-innerWidth js/window)
        inner-height (.-innerHeight js/window)
        scene (new Scene)
        camera (new PerspectiveCamera 75 (/ inner-width inner-height) 0.1 1000)
        renderer (new WebGLRenderer)]
    (.setSize renderer inner-width inner-height)
    (.appendChild container (.-domElement renderer))
    (.set (-> camera .-position) 0 5 10)
    (new OrbitControls camera (.-domElement renderer))
    {:scene scene
     :camera camera
     :renderer renderer}))

(defn sync-mesh-collider [^js mesh ^js collider]
  (let [^js translation (.translation collider)
        ^js rotation (.rotation collider)]
    (.set (.-position mesh)
          (.-x translation)
          (.-y translation)
          (.-z translation))
    (.set (.-quaternion mesh)
          (.-x rotation)
          (.-y rotation)
          (.-z rotation)
          (.-w rotation))
    (.set (.-scale mesh) 2 2 2)
    (.updateMatrix mesh)))

(defn app []
  (let [{:keys [^Scene scene
                ^PerspectiveCamera camera
                ^WebGLRenderer renderer]} system
        gravity #js {:x 0.0 :y -9.81 :z 0.0}
        ^js world (new (.-World rapier) gravity)

        ^js ground-desc ((-> rapier .-ColliderDesc .-cuboid) 10.0 0.1 10.0)
        ^js ground-collider (.createCollider world ground-desc)
        ^js ground-geometry (new BoxGeometry 10.0 0.1 10.0)
        ^js ground-material (new MeshBasicMaterial #js {:color 0x00ff00})
        ^js ground-mesh (new Mesh ground-geometry ground-material)

        ^js rigid-body-desc (.setTranslation (-> rapier .-RigidBodyDesc .dynamic) 0.0 2.5 0.0)
        ^js rigid-body (.createRigidBody world rigid-body-desc)
        ^js rigid-body-collider-desc ((-> rapier .-ColliderDesc .-cuboid) 0.5 0.5 0.5)
        ^js rigid-body-collider (.createCollider world rigid-body-collider-desc rigid-body)
        ^js rigid-body-geometry (new BoxGeometry 0.5 0.5 0.5)
        ^js rigid-body-material (new MeshBasicMaterial #js {:color 0x0000ff})
        ^js rigid-body-mesh (new Mesh rigid-body-geometry rigid-body-material)]

    ; ground
    (.add scene ground-mesh)
    (sync-mesh-collider ground-mesh ground-collider)

    ; rigid body
    (.add scene rigid-body-mesh)
    (sync-mesh-collider rigid-body-mesh rigid-body-collider)

    (.setAnimationLoop renderer (fn ^js []
                                  (sync-mesh-collider rigid-body-mesh rigid-body-collider)
                                  (.step world)
                                  (.render renderer scene camera)))))

(-> (.init rapier)
    (.then app))
