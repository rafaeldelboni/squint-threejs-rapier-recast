(ns app.core
  (:require
   ["@dimforge/rapier3d-compat" :as rapier]
   ["three/addons/controls/OrbitControls.js" :refer [OrbitControls]]
   [three :refer [BoxGeometry BufferAttribute BufferGeometry LineBasicMaterial
                  LineSegments Mesh MeshBasicMaterial PerspectiveCamera Scene
                  WebGLRenderer]]))

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

(defn debug-render [^js lines ^js world]
  (let [buffers (.debugRender world)]
    (.setAttribute (.-geometry lines) "position" (new BufferAttribute (.-vertices buffers) 3))
    (.setAttribute (.-geometry lines) "color" (new BufferAttribute (.-colors buffers) 4))))

(defn static-cube [^js world ^js scene {:keys [x y z]} {x-size :x y-size :y z-size :z} color]
  (let [^js collider-desc ((-> rapier .-ColliderDesc .-cuboid) x-size y-size z-size)
        ^js collider (.createCollider world (.setTranslation collider-desc x y z))
        ^js geometry (new BoxGeometry x-size y-size z-size)
        ^js material (new MeshBasicMaterial #js {:color color})
        ^js mesh (new Mesh geometry material)]
    (.add scene mesh)
    (sync-mesh-collider mesh collider)
    {:mesh mesh :collider collider}))

(defn rigid-cube [^js world ^js scene {:keys [x y z]} {x-size :x y-size :y z-size :z} color]
  (let [^js rigid-body-desc (.setTranslation (-> rapier .-RigidBodyDesc .dynamic) x y z)
        ^js rigid-body (.createRigidBody world rigid-body-desc)
        ^js collider-desc ((-> rapier .-ColliderDesc .-cuboid) x-size y-size z-size)
        ^js collider (.createCollider world collider-desc rigid-body)
        ^js geometry (new BoxGeometry x-size y-size z-size)
        ^js material (new MeshBasicMaterial #js {:color color})
        ^js mesh (new Mesh geometry material)]
    (.add scene mesh)
    (sync-mesh-collider mesh collider)
    {:mesh mesh :collider collider :rigid-body rigid-body}))

(defn app []
  (let [{:keys [^Scene scene
                ^PerspectiveCamera camera
                ^WebGLRenderer renderer]} system
        gravity #js {:x 0.0 :y -9.81 :z 0.0}
        ^js world (new (.-World rapier) gravity)

        ^js debug-material (new LineBasicMaterial #js {:color 0xffffff :vertexColors true})
        ^js debug-geometry (new BufferGeometry)
        ^js debug-lines (new LineSegments debug-geometry debug-material)

        ^js ground (static-cube world scene {:x 0.0 :y 0.0 :z 0.0} {:x 10.0 :y 0.1 :z 10.0} 0x00ff00)
        ^js rigid-body (rigid-cube world scene {:x 0.0 :y 2.5 :z 0.0} {:x 0.5 :y 0.5 :z 0.5} 0x0000ff)

        objects [ground rigid-body]]
    ;debug lines
    (.add scene debug-lines)
    ; render loop
    (.setAnimationLoop renderer (fn ^js []
                                  (doseq [{:keys [mesh collider]} objects]
                                    (sync-mesh-collider mesh collider))
                                  (debug-render debug-lines world)
                                  (.step world)
                                  (.render renderer scene camera)))))

(-> (.init rapier)
    (.then app))
