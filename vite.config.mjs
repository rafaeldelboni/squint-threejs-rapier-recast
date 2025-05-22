import { defineConfig } from 'vite';
import { visualizer } from 'rollup-plugin-visualizer';
import { viteStaticCopy } from 'vite-plugin-static-copy'

export default defineConfig({
  base: './',
  plugins: [
    visualizer({ open: false, filename: 'bundle-visualization.html' }),
    viteStaticCopy({
      targets: [
        {
          src: 'assets/test-scene.glb',
          dest: 'assets/'
        }
      ]
    })
  ]
});
